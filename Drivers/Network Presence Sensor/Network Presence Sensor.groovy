import groovy.json.*
	
metadata {
	definition (name: "Network Presence Sensor", namespace: "oldchicagopete", author: "oldchicagopete") {
        capability "Initialize"
		capability "Refresh"
		capability "Sensor"
        capability "Presence Sensor"
	}

	preferences {
		section {
			input (
				type: "string",
				name: "ipAddress",
				title: "Device IP Address",
				required: true				
			)
			input (
				type: "number",
				name: "refreshInterval",
				title: "Refresh Interval",
				description: "In minutes",
				required: true,
				defaultValue: 1
			)
			input (
				type: "number",
				name: "awayThreshold",
				title: "Away Status Threshold",
				description: "Number of consecutive unanswered pings to mark as away/offline",
				required: true,
				defaultValue: 3
			)
			input (
				type: "bool",
				name: "enableTextLogging",
				title: "Enable Text Logging?",
				required: true,
				defaultValue: true
			)
			input (
				type: "bool",
				name: "enableDebugLogging",
				title: "Enable Debug Logging?",
				required: true,
				defaultValue: false
			)
		}
	}
}

def installed () {
	log.info "${device.displayName} is installed"
    updated()
}

def updated () {
	log.info "${device.displayName} is updated"
    sendEvent(name: "presence", value: "not present", linkText: deviceName, descriptionText: "Device updated")
    state.ipAddress = ipAddress
    state.refreshInterval = refreshInterval
    state.awayThreshold = awayThreshold
    state.lastPing = "notfound"
    state.notFoundCount = 0
    refresh()
}

def initialize() {
	if(refreshInterval > 0) {
    	unschedule()
    	pingDevice()
	}
}

def refresh() {
    unschedule()
    pingDevice()
}

def logInfo(msg) {
	if (enableTextLogging) {
		log.info(msg)	
	}
}

def logDebug(msg) {
	if (enableDebugLogging) {
		log.debug(msg)	
	}
}

def pingDevice() {
    def currentValue = device.currentValue("presence")

	logDebug "${device.displayName} pingDevice"

    if(!validIP (ipAddress)) {
        log.warn "${device.displayName} IP address value ($ipAddress) is invalid"
    } else {
        hubitat.helper.NetworkUtils.PingData pingData = hubitat.helper.NetworkUtils.ping(ipAddress, 1)
        logDebug "${device.displayName} $pingData"
        if (pingData.packetLoss < 100) {
            state.lastPing = "found"
            logDebug "${device.displayName} IP address $ipAddress is found on the network"
        } else {
            state.lastPing = "notfound"
            logDebug "${device.displayName} IP address $ipAddress is not found on the network"
        }
    }
	if(refreshInterval > 0) runIn(refreshInterval*60, pingDevice)

    if (currentValue == "present" && state.lastPing == "notfound") {
        state.notFoundCount = state.notFoundCount + 1
        if (state.notFoundCount >= awayThreshold.toInteger()) {
            def descriptionText = "${device.displayName} is not found on the network"
            logInfo descriptionText
            sendEvent(name: "presence", value: "not present", linkText: deviceName, descriptionText: descriptionText)
        }
    }

    if (currentValue == "not present" && state.lastPing == "found") {
        def descriptionText = "${device.displayName} is found on the network"
        logInfo descriptionText
        sendEvent(name: "presence", value: "present", linkText: deviceName, descriptionText: descriptionText)
        state.notFoundCount = 0
    }
}
       
def validIP(ipAddress){
    regxPattern =/^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/
    boolean match = ipAddress ==~ regxPattern
    return match
}
