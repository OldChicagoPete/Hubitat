/*
  Sofabaton X1S (requires minimum Android version 3.4.4)

	2025-03-22 maxwell
		-initial publication in github repo (https://github.com/hubitat/HubitatPublic/blob/master/examples/drivers/sofabatonX1S.groovy)
		-*simple example driver for Sofabaton X1S remote, allows mapping X1S remote buttons to Hubitat button events
	2025-03-28 Gassgs
		-https://community.hubitat.com/t/my-seemingly-never-ending-quest-for-a-universal-av-remote/151722/65
    -Made Button Count a preferences
	2025-04-01 OldChicagoPete
		-Using the JSON header to send the button number as a key/value pair due to the Android Sofabaton app's validity requirements for the Body field
     (This should work for iOS as well)
		-Default the Button Count to 20 to match the Sofabaton X1S hardware
	2025-04-10 OldChicagoPete
		-Added DoubleTap, Hold, and Release capabilities
    -Added an optional key/value pair to support the additional capabilites
	

	*Driver configuration
    -X1S IP - [The Sofabaton's reserved IP address]
    -Button Count - [The number of buttons for this device]

	
	*Mobile app configuration
	-Click add devices (plus sign icon) on the Devices tab
  -Select Wi-Fi
	-Click link at bottom "Create a virtual device for IP control"
	-COMMAND NAME - The command name that will be visible in the Sofabaton app
	-URL - http://[HE hub IP address]:39501/route
	-Request Method - PUT
	-Connect type - application/json
	-Additional Headers
        button:[button number]
        (optional) action:[pushed|doubleTapped|held|released]
*/


metadata {
    definition (name: "Sofabaton X1S", namespace: "oldchicagopete", author: "OldChicagoPete") {
        capability "Actuator"
        capability "PushableButton"
        capability "DoubleTapableButton"
        capability "HoldableButton"
        capability "ReleasableButton"
        preferences {
            input name:"ip", type:"text", title: "X1S IP", required: true
            input name:"numButtons", type: "number",title:"Number of virtual buttons", defaultValue: 20, required: true
            input name:"logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
            input name:"txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        }
    }
}

void updated(){
    log.info "${device.label} updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
    if (ip) {
        device.deviceNetworkId = ipToHex(IP)
        sendEvent(name:"numberOfButtons",value:settings.numButtons)
    }
}

String ipToHex(IP) {
    List<String> quad = ip.split(/\./)
    String hexIP = ""
    quad.each {
        hexIP+= Integer.toHexString(it.toInteger()).padLeft(2,"0").toUpperCase()
    }
    return hexIP
}

void logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

void parse(String description) {
    Map msg = parseLanMessage(description)
    Map header = msg.headers
    if (header.action == null) {
        sendButtonEvent("pushed", header.button, "physical")
    } else {
        sendButtonEvent(header.action, header.button, "physical")
    }
}

void sendButtonEvent(String evt, String bid, String type) {
    if (bid == null || !bid.isNumber()) {
        log.error "Invalid button specified (${bid})"
    } else {
        if (bid.toInteger() < 1 || bid.toInteger() > settings.numButtons) log.warn "Button ${bid} is outside of the device's defined range (1-${settings.numButtons})"
    	String descriptionText = "${device.displayName} button ${bid} was ${evt} [${type}]"
    	if (txtEnable) log.info "${descriptionText}"
    	sendEvent(name: evt, value: bid.toInteger(), descriptionText: descriptionText, isStateChange: true, type: type)
    }
}

void push(BigDecimal button) {
    sendButtonEvent("pushed", button.toString(), "digital")
}

void doubleTap(BigDecimal button) {
    sendButtonEvent("doubleTapped", button.toString(), "digital")
}

void hold(BigDecimal button) {
    sendButtonEvent("held", button.toString(), "digital")
}

void release(BigDecimal button) {
    sendButtonEvent("released", button.toString(), "digital")
}
