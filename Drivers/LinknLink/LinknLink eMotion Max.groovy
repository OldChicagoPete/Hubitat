/*
 * LinknLink eMotion Max
 * 
 * 
 * MQTT Multi-Zone Motion & Environment Sensor (Parent with Manual-Controlled Child Devices)
 * - Motion and illuminance on parent device
 * - 4 optional child device motion zones
 * - The parent and each child can manually set active/inactive status from the device page
 * - Optional status logging for all devices
 * 
 * Originally posted by "aren" at https://community.hubitat.com/t/linknlink-emotion-mmwave-device-work-with-hubitat/152763/12
 * Modified for the eMotion Max
 * - Removed temperature and humidity
 * - Removed auto-off
 * - Added MotionSensor capability to the parent using Zone 0
 * - Reworked the child driver logic
 * 
 * 
 * Original description
 * 
 * MQTT Multi-Zone Motion & Environment Sensor (Parent with Manual-Controlled Child Devices)
 * - 5 motion zones (each as a child device, enable/disable via preferences)
 * - Temperature, humidity, and illuminance on parent device
 * - Each child can be set active/inactive manually from its device page
 * Author: Perplexity AI, 2025
 */

import groovy.json.JsonSlurper

metadata {
    definition (name: "LinknLink eMotion Max", namespace: "oldchicagopete", author: "OldChicagoPete") {
        capability "IlluminanceMeasurement"
        capability "Initialize"
        capability "MotionSensor"
        command "active"
        command "inactive"
    }
	preferences {
		input name: "MQTTBroker", type: "text", title: "MQTT Broker Address (host or IP):", required: true
		input name: "username", type: "text", title: "MQTT Username (optional):", required: false
		input name: "password", type: "password", title: "MQTT Password (optional):", required: false
		input name: "baseTopic", type: "text", title: "Base Topic (e.g. home/macaddress000000):", required: true
		input name: "zone1Enabled", type: "bool", title: "Enable Zone 1", defaultValue: true
		input name: "zone2Enabled", type: "bool", title: "Enable Zone 2", defaultValue: true
		input name: "zone3Enabled", type: "bool", title: "Enable Zone 3", defaultValue: true
		input name: "zone4Enabled", type: "bool", title: "Enable Zone 4", defaultValue: true
		input name: "motionEnable", type: "bool", title: "Enable motion logging", defaultValue: true
		input name: "illuminationEnable", type: "bool", title: "Enable illuminance logging", defaultValue: true
		input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
	}
}

void installed() {
    log.info "Installed..."
}

void updated() {
    log.info "Updated..."
    initialize()
}

void initialize() {
    createChildDevices()
	
    try {
        if (!interfaces.mqtt.isConnected()) {
			def mqttbroker = "tcp://${settings.MQTTBroker}:1883"
			def clientId = "hubitat_${device.id}"
		
			log.info "Connecting to MQTT broker at ${mqttbroker} as ${clientId}"
			interfaces.mqtt.connect(mqttbroker, clientId, settings.username, settings.password)
			pauseExecution(1000)
		}
        if (interfaces.mqtt.isConnected()) subscribeToTopics()
    } catch(e) {
        log.error "MQTT initialize error: ${e.message}, will retry in 60 seconds"
        runIn(60, initialize)
    }
	
    if (logEnable) runIn(600, logsOff)
}

void createChildDevices() {
    def zoneNames = ["Zone 0", "Zone 1", "Zone 2", "Zone 3", "Zone 4"]
	
    for (int i = 1; i < 5; i++) {
        def enabled = settings?."zone${i}Enabled"
        def dni = "${device.deviceNetworkId}-zone${i}"
		
        if (enabled) {
            if (!getChildDevice(dni)) {
                addChildDevice("LinknLink eMotion Zone", dni, [isComponent:true, name:"eMotion Zone", label:"${device.displayName} ${zoneNames[i]}", ])
                log.info "Created child device for ${zoneNames[i]}"
            }
        } else {
            def child = getChildDevice(dni)
			
            if (child) {
                deleteChildDevice(dni)
                log.info "Deleted child device for ${zoneNames[i]}"
            }
        }
    }
}

void subscribeToTopics() {
    for (int i = 0; i < 5; i++) {
        if (i == 0 || settings?."zone${i}Enabled") {
            def topic = "${settings.baseTopic}_${i}/status"
			
            interfaces.mqtt.subscribe(topic)
            if (logEnable) log.debug "Subscribed to presence topic: ${topic}"
        }
    }
    interfaces.mqtt.subscribe("${settings.baseTopic}_l/status")
    if (logEnable) log.debug "Subscribed to illuminance topic"
}

void mqttClientStatus(String status) {
    if (status.startsWith("Error")) {
        log.warn "MQTT error: ${status}"
        if (!interfaces.mqtt.isConnected()) {
            log.warn "MQTT not connected, will retry in 60 seconds"
            runIn(60, initialize)
        }
    } else {
		if (logEnable) log.debug "MQTT Status: ${status}"
    }
}

void uninstalled() {
    log.info "Uninstalled. Disconnecting MQTT and removing any child devices"
    interfaces.mqtt.disconnect()
    getChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
}

void logsOff() {
    if (logEnable) {
    	log.warn "Debug logging disabled."
    	device.updateSetting("logEnable", [value: "false", type: "bool"])
    }
}

void active() {
    if (motionEnable) log.info "${device.displayName} is active [digital]"
    sendEvent(name:"motion", value:"active", descriptionText:"${device.displayName} is active [digital]")
}

void inactive() {
    if (motionEnable) log.info "${device.displayName} is inactive [digital]"
    sendEvent(name:"motion", value:"inactive", descriptionText:"${device.displayName} is inactive [digital]")
}

void parse(String description) {
    if (logEnable) log.debug "parse() called with: ${description}"
    def msg = interfaces.mqtt.parseMessage(description)
    def topic = msg.topic
    def payload = msg.payload?.replaceFirst(/^=\s*/, '')

    if (logEnable) log.info "MQTT message on '${topic}': ${payload}"
    try {
        def json = new JsonSlurper().parseText(payload)
		
        if (topic.endsWith("_l/status") && json.illuminance != null) {
            handleIlluminance(json.illuminance)
        } else {
            def presenceZone = topic =~ /_(\d)\/status$/
			
            if (presenceZone && json.pir_detected != null) {
                def zoneNum = presenceZone[0][1] as Integer
				
                if (zoneNum == 0 || settings?."zone${zoneNum}Enabled") {
                    handlePresence(zoneNum, json.pir_detected)
                }
            }
        }
    } catch (Exception e) {
        log.warn "JSON parse error: ${e.message}"
    }
}

void handleIlluminance(illuminance) {
    if (illuminance as Integer != device.currentValue("illuminance")) {
    	if (illuminationEnable) log.info "${device.displayName} illuminance is ${illuminance} lux"
    	sendEvent(name: "illuminance", value: illuminance as Integer, unit: "lux")
    }
}

void handlePresence(Integer zone, pirDetected) {
    def motionState = (pirDetected == 1) ? "active" : "inactive"
	
    if (zone == 0) {
        if (motionState != device.currentValue("motion")) {
    		if (motionEnable) log.info "${device.displayName} is ${motionState}"
            sendEvent([name:"motion", value:motionState, descriptionText:"${device.displayName} is ${motionState} [physical]"])
        }
    } else {
    	def dni = "${device.deviceNetworkId}-zone${zone}"
    	def child = getChildDevice(dni)
	
    	if (child) {
        	if (motionState != child.currentValue("motion")) {
				child.parse([name:"motion", value:motionState, descriptionText:"${child.displayName} is ${motionState} [physical]"])
        	}
    	}
    }
}
