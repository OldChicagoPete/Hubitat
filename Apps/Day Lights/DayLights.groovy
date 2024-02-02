/*
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
*	Day Lights
*
*	Author:
*		OldChicagoPete
*
*	Documentation:  https://community.hubitat.com/t/release-day-lights-an-interation-of-circadian-daylight/130157
*
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
*
*	Forked from:
*  		Hubitat Circadian Daylight 0.81
*		https://raw.githubusercontent.com/adamkempenich/hubitat/master/Apps/CircadianDaylight.groovy
*	Which was forked from:
*  		SmartThings Circadian Daylight v. 2.6
*		https://github.com/KristopherKubicki/smartapp-circadian-daylight/
*
*
*
*   Custom fade-in/fade-out time:
*       Jeff Byrom (@talz13)
*   Color temperature converter:
*       http://www.tannerhelland.com/4435/convert-temperature-rgb-algorithm-code/
*   RGB to Hue/Saturation/Value:
*       http://www.rapidtables.com/convert/color/rgb-to-hsv.htm
*
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
*
*  Changelog:
*	0.90 (November 2, 2023)
*		- Soft Release
*	0.91 (December 20, 2023)
*		- General Release
*	0.92 (February 2, 2024)
*		- Clarification on device selection headings
*		- When using RGB mode devices with Dynamic Brightness, the calculated level will be used for the Value parameter
*		- RGB mode devices will not be polled to determine if Dynamic Brightness has been overridden
*		- Fix for HSV return values
*
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
*/

definition(
    name: "Day Lights",
    namespace: "daylightsapp",
    author: "OldChicagoPete",
    importURL: "",
    description: "Adjust device color temperature, color, and/or brightness during daylight hours",
    category: "Lighting",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
    page name: "MainPage", install: true, uninstall: true
    page name: "SunriseSunsetOptions"
    page name: "ColorTemperatureOptions"
    page name: "BrightnessOptions"
    page name: "ModeOptions"
    page name: "DisableOptions"

}

def MainPage(){
    dynamicPage(name: "MainPage") {

		section("<h2>Day Lights</h2>"){
            paragraph "This application adjusts the color temperature, color, and/or the brightness of your lights based on the time of day. By default it runs from sunrise to sunset, adjusting the devices selected below. Many additional options are also available."
			label title: "Enter a name for this instance of the application (optional)"
		}
		
        section("<h2>Select Devices</h2>") {
            paragraph "Individual devices should only be selected for one group"
            input "colorTemperatureDevices", "capability.colorTemperature", title: "<b>Color temperature devices (CT mode)</b>", multiple:true
            input "colorDevices", "capability.colorControl", title: "<b>Color changing devices (RGB mode)</b>", multiple:true
            input "dimmableDevices", "capability.switchLevel", title: "<b>Dimmable devices (requires Dynamic Brightness to be enabled)</b>", multiple:true
        }

        section("<h2>Dynamic Brightness</h2>") {
            input "dynamicBrightness","bool", title: "Adjust brightness based on the time of day"
        }

        section("<h2>Advanced Options</h2>") {
            href(name: "toSunriseSunsetOptions",
                 title: "<b>Sunrise/Sunset Options</b>",
                 page: "SunriseSunsetOptions",
                 description: "Set Advanced Sunrise/Sunset Options"
            )
            href(name: "toColorTemperatureOptions",
                 title: "<b>Color Temperature Options</b>",
                 page: "ColorTemperatureOptions",
                 description: "Set Advanced Color Temperature Options"
            )
            href(name: "toBrightnessOptions",
                 title: "<b>Dynamic Brightness Options</b>",
                 page: "BrightnessOptions",
                 description: "Set Brightness Options"
            )
            href(name: "toModeOptions",
                 title: "<b>Mode Options</b>",
                 page: "ModeOptions",
                 description: "Set Mode Options"
            )
            href(name: "toDisableOptions",
                 title: "<b>Disable Options</b>",
                 page: "DisableOptions",
                 description: "Set Disable Options"
            )
        }
	
        section("<h2>Logging</h2>") {
            input(name:"logDescriptionText", 
			      type:"bool", 
				  title: "Enable descriptionText logging",
                  description: "Logs information regarding application activity (Default: On)", 
			      defaultValue: true,
                  required: true, 
				  displayDuringSetup: true
			)
            input(name:"logEnhancedDescriptionText", 
			      type:"bool", 
				  title: "Enable enhanced descriptionText logging",
                  description: "Logs additional information regarding application activity (Default: On)", 
			      defaultValue: false,
                  required: true, 
				  displayDuringSetup: true
			)
            input(name:"logDebug", 
			      type:"bool", 
				  title: "Enable debug logging",
                  description: "Logs detailed data regarding appliction values (Default: Off)", 
				  defaultValue: false,
                  required: true, 
				  displayDuringSetup: true
			)
        }
    }



}

def SunriseSunsetOptions() {
    dynamicPage(name: "SunriseSunsetOptions") {

        section("<h2>Sunrise/Sunset Options</h2>") {
            input "useSunOverrides", "bool", title: "<b>Use Sunrise/Sunset Overrides?</b>"
            input "sunriseOverride", "time", title: "Sunrise Override"
            input "sunsetOverride", "time", title: "Sunset Override"
            paragraph "<br>"
            input "useSunOffsets", "bool", title: "<b>Use Sunset/Sunrise Offsets (+/-)?</b>"
            input "sunriseOffset", "decimal", title: "Sunrise Offset (+/-)"
            input "sunsetOffset", "decimal", title: "Sunset Offset (+/-)"
        }

    }
}

def ColorTemperatureOptions() {
    dynamicPage(name: "ColorTemperatureOptions") {

        section("<h2>Color Temperature Options</h2>"){
            paragraph "The color temperature on your devices will be adjusted from warm at sunrise to cold at midday then back to warm at sunset.<br><br>"
            input "useCTOverrides", "bool", title: "<b>Use Color Temperature Overrides?</b>"
            input "warmCTOverride", "number", title: "Warm White Temperature (default is 2700)"
            input "coldCTOverride", "number", title: "Cold White Temperature (default is 6500)"
        }
    }
}

def BrightnessOptions() {
    dynamicPage(name: "BrightnessOptions") {

        section("<h2>Dynamic Brightness Options</h2>") {
            paragraph "When Dynamic Brightness is enabled the brightness on your devices will be adjusted from low at sunrise to high at midday then back to low at sunset.<br><br>"
		    input name: "brightnessProfile", type: "enum", title: "<b>Brightness Profile</b> (default is Gradual)", required: true, defaultValue: 1, options:["Gradual":"Gradual - Brightening begins at sunrise and reaches maximum at midday","Accelerated":"Accelerated - Brightening begins at sunrise and reaches maximum prior to midday","Delayed":"Delayed - Remains at minimum until accelerated brightening will reach maximum at midday"]
		    paragraph "<br>"
            input "useBrightnessOverrides", "bool", title: "<b>Use Brightness Overrides?</b>"
            input "minBrightnessOverride","number", title: "Low Brightness (default is 1)"
            input "maxBrightnessOverride","number", title: "High Brightness (default is 100)"
		    paragraph "<br>"
            input "usePeriodOverrides", "bool", title: "<b>Use Brightness Period Overrides?</b>"
            paragraph "Times outside of the sunrise/midday and midday/sunset periods will be ignored."
			input "brightenTimeStart", "time", title: "Start Brightening At"
			input "brightenTimeEnd", "time", title: "End Brightening At"
			input "dimTimeStart", "time", title: "Start Dimming At"
			input "dimTimeEnd", "time", title: "End Dimming At"
		}
    }
}

def ModeOptions() {
    dynamicPage(name: "ModeOptions") {

        section("<h2>Mode Options</h2>") {
            input "useModeOverrides", "bool", title: "<b>Use Mode Overrides?</b>"
        }

        section("<b>Override 1</b>") {
            input "mode1Override", "mode", title: "Mode"
            input "mode1OverrideColorTemperature","number", title: "Color temperature"
            input "mode1OverrideValue","number", title: "Brightness"
        }

        section("<b>Override 2</b>") {
            input "mode2Override", "mode", title: "Mode"
            input "mode2OverrideColorTemperature","number", title: "Color temperature"
            input "mode2OverrideValue","number", title: "Brightness"
        }

        section("<b>Override 3</b>") {
            input "mode3Override", "mode", title: "Mode"
            input "mode3OverrideColorTemperature","number", title: "Color temperature"
            input "mode3OverrideValue","number", title: "Brightness"
        }

        section("<b>Override 4</b>") {
            input "mode4Override", "mode", title: "Mode"
            input "mode4OverrideColorTemperature","number", title: "Color temperature"
            input "mode4OverrideValue","number", title: "Brightness"
        }

        section("<b>Override 5</b>") {
            input "mode5Override", "mode", title: "Mode"
            input "mode5OverrideColorTemperature","number", title: "Color temperature"
            input "mode5OverrideValue","number", title: "Brightness"
        }

        section("<b>Override 6</b>") {
            input "mode6Override", "mode", title: "Mode"
            input "mode6OverrideColorTemperature","number", title: "Color temperature"
            input "mode6OverrideValue","number", title: "Brightness"
        }

        section("<b>Override 7</b>") {
            input "mode7Override", "mode", title: "Mode"
            input "mode7OverrideColorTemperature","number", title: "Color temperature"
            input "mode7OverrideValue","number", title: "Brightness"
        }

        section("<b>Override 8</b>") {
            input "mode8Override", "mode", title: "Mode"
            input "mode8OverrideColorTemperature","number", title: "Color temperature"
            input "mode8OverrideValue","number", title: "Brightness"
        }

        section("<b>Override 9</b>") {
            input "mode9Override", "mode", title: "Mode"
            input "mode9OverrideColorTemperature","number", title: "Color temperature"
            input "mode9OverrideValue","number", title: "Brightness"
        }

        section("<b>Override 10</b>") {
            input "mode10Override", "mode", title: "Mode"
            input "mode10OverrideColorTemperature","number", title: "Color temperature"
            input "mode10OverrideValue","number", title: "Brightness"
        }
    }
}

def DisableOptions(){
    dynamicPage(name: "DisableOptions") {

        section("<h2>Disable Day Lights</h2>") {
            input "disablingSwitches","capability.switch", title: "Select switches that will disable Day Lights", multiple:true
            input "disableWhenSwitchOff","bool", title: "Disable when off (normally disables when switch is on)"
        }

        section("<h2>Disable Dynamic Brightness</h2>") {
            input "disableWhenDimmed", "bool", title: "<b>Disable Dynamic Brightness for the day when the brightness on a selected device is manually changed?</b>"
            paragraph "Dynamic Brightness will be re-enabled automatically at the next sunrise"
            input "reenableDimmingTime", "time", title: "Add an additional time to re-enable Dynamic Brightness"
            paragraph "If currently disabled, Dynamic Brightness will be re-enabled by clicking the Done button"
        }
    }
}

def installed() {
    unsubscribe()
    unschedule()
    initialize()
}

def updated() {
    unsubscribe()
    unschedule()
    initialize()
}

def uninstalled() {
    unsubscribe()
    unschedule()
}

private logDescriptionText(debugText) {

    if (settings.logDescriptionText) {
        log.info "${app.name} (${app.getLabel()}): ${debugText}"
    }
}

private logEnhancedDescriptionText(debugText) {

    if (settings.logEnhancedDescriptionText) {
        log.info "${app.name} (${app.getLabel()}): ${debugText}"
    }
}

private logDebug(debugText) {

    if (settings.logDebug) {
        log.debug "${app.name} (${app.getLabel()}): ${debugText}"
    }
}

private def initialize() {
    state.scheduleActive = false
    state.bypassManualOverrideCheck = true
    state.disabledFromDimmer = false
    state.lastAssignedBrightness = 0
	
    logDebug("initialize() with settings: ${settings}")

    subscribe(location, "sunriseTime", scheduleNextWakeup)
    subscribe(app, eventApplication)
	
    if (colorTemperatureDevices) {
        subscribe(colorTemperatureDevices, "switch.on", eventDeviceOn)
    }
    if (colorDevices) {
        subscribe(colorDevices, "switch.on", eventDeviceOn)
    }
    if (dimmableDevices) {
        subscribe(dimmableDevices, "switch.on", eventDeviceOn)
    }
	if (settings.useModeOverrides) {
        subscribe(location, "mode", eventMode)
	}
    if (disablingSwitches) { 
        subscribe(disablingSwitches, "switch", eventSwitch) 
    }
	scheduleNextWakeup()
	eventHandler("Initialize")
}

def scheduleNextWakeup(evt) {
    def sunriseTime
	def events
	
	if (settings.useSunOverrides && settings.sunriseOverride != null && settings.sunriseOverride != "") {
        sunriseTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", settings.sunriseOverride)
		if (sunriseTime < new Date()) {
		    sunriseTime = sunriseTime + 1
		}
        logDebug("Sunrise Override is ${sunriseTime}")
	}
	else {
	    if (evt != null) {
		    sunriseTime = toDateTime(evt.value)
		}
		else {
	        events = getLocationEventsSince("sunriseTime", new Date() - 2, [max: 1])
            sunriseTime = toDateTime(events[0].value)
	    }
        logDebug("Next system sunrise time is ${sunriseTime}")
	}
	if (settings.useSunOffsets && settings.sunriseOffset != null && settings.sunriseOffset != "") {
		sunriseTime = sunriseTime.plusMinutes(settings.sunriseOffset)
        logDebug("Sunrise offset to ${sunriseTime}")
	}
	
    schedule(sunriseTime, eventWakeup)
}

def eventApplication(evt) {

    eventHandler("Application ${evt.name}(${evt.value})")
}

def eventDeviceOn(evt) {

    state.bypassManualOverrideCheck = true
    eventHandler("Device On")
}

def eventMode(evt) {

    eventHandler("${location.mode} Mode")
}
def eventSwitch(evt) {

    eventHandler("Disable Switch")
}

def eventWakeup(evt) {

	if (!state.scheduleActive)  {
        state.disabledFromDimmer = false
        state.bypassManualOverrideCheck = true
        eventHandler("Wakeup")
    }
}

def disableDimmerOverride(evt) {
	
    state.disabledFromDimmer = false
    unschedule(disableDimmerOverride)
    state.bypassManualOverrideCheck = true
    eventHandler("Reset Brighness")
}

def eventHandler(evt) {
	
    for (disableSwitch in disablingSwitches) {
        if ((disableSwitch.currentSwitch == "on" && !settings.disableWhenSwitchOff) || (disableSwitch.currentSwitch == "off" && settings.disableWhenSwitchOff)) {
		    logDescriptionText("Currently disabled by a switch")
            return
        }
    }

    if (state.disabledFromDimmer) {
        logDescriptionText("Processing(${evt}) (Dynamic Brightness disabled)")
	}
	else {
        logDescriptionText("Processing(${evt})")
	}

    if (state.bypassManualOverrideCheck) {
        state.bypassManualOverrideCheck = false
    }
    else if (settings.disableWhenDimmed && !state.disabledFromDimmer) {
		for (device in colorTemperatureDevices) {
			if (device.currentValue("switch") == "on" && device.currentValue("level") != state.lastAssignedBrightness) {
				state.disabledFromDimmer = true
			}
		}
        //Some color devices don't precisely apply the HSV values, so they will not be considered for disabling Dynamic Brightness
		//for (device in colorDevices) {
		//	if (device.currentValue("switch") == "on" && device.currentValue("level") != state.lastAssignedBrightness) {
		//		state.disabledFromDimmer = true
		//	}
		//}
		for (device in dimmableDevices) {
			if (device.currentValue("switch") == "on" && device.currentValue("level") != state.lastAssignedBrightness) {
				state.disabledFromDimmer = true
			}
		}
		if (state.disabledFromDimmer) {
            if (settings.reenableDimmingTime != null && settings.reenableDimmingTime != "") {
                def scheduleTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", settings.reenableDimmingTime)
		        if (scheduleTime > new Date()) {
		            schedule(scheduleTime, disableDimmerOverride)
		        }
            }
		    logDescriptionText("Dynamic Brightness disabled until reset")
		}
    }

    def nv = getNewValues()
	def ct = nv.colorTemp
	def bright = nv.brightness
	def rgb = ctToRGB(ct)
	def hex = rgbToHex(rgb).toUpperCase()
	def hsv = rgbToHSV(rgb)
    if (settings.dynamicBrightness && !state.disabledFromDimmer) {
        hsv.v = bright
    }
    def color = [hex: hex, hue: hsv.h, saturation: hsv.s, level: hsv.v]
    state.lastAssignedBrightness = bright
    
    logEnhancedDescriptionText("CT=${ct}K, Level=${bright}%, Color=${hex}, Hue=${hsv.h}, Saturation=${hsv.s}, Value=${hsv.v}")
    logDebug("Color Temperature: ${ct}")
    logDebug("Brightness: ${bright}")
	logDebug("Color: ${color}")

    for (device in colorTemperatureDevices) {
        if (device.currentValue("switch") == "on") {
            if (device.currentValue("colorTemperature") != ct) {
                device.setColorTemperature(ct)
            }
            if (settings.dynamicBrightness && !state.disabledFromDimmer && device.currentValue("level") != bright) {
                device.setLevel(bright)
            }
        }
    }

    for (device in colorDevices) {
        if (device.currentValue("switch") == "on") {
            if (device.currentValue("color") != hex || (settings.dynamicBrightness && !state.disabledFromDimmer && device.currentValue("level") != bright)) {
                device.setColor(color)
            }
        }
    }
		
    for (device in dimmableDevices) {
        if (device.currentValue("switch") == "on") {
            if (settings.dynamicBrightness && !state.disabledFromDimmer && device.currentValue("level") != bright) {
                device.setLevel(bright)
            }
        }
    }
}

def getNewValues() {
    def sunriseTime = getSunriseTime()
    def sunsetTime = getSunsetTime()
    def midTime = sunriseTime.time + ((sunsetTime.time - sunriseTime.time) / 2)
    def currentTime = now()
	
    def workTime
	def brightenStart = sunriseTime.time
	def brightenEnd = midTime
	def dimStart = midTime
	def dimEnd = sunsetTime.time
	if (settings.usePeriodOverrides) {
	    if (settings.brightenTimeStart != null && settings.brightenTimeStart != "") {
            workTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", settings.brightenTimeStart)
            if (workTime > sunriseTime) {
	            brightenStart = workTime.time
            }
        }
  	    if (settings.brightenTimeEnd != null && settings.brightenTimeEnd != "") {
            workTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", settings.brightenTimeEnd)
            if (workTime.time < midTime) {
	            brightenEnd = workTime.time
            }
        }
        if (settings.dimTimeStart != null && settings.dimTimeStart != "") {
            workTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", settings.dimTimeStart)
            if (workTime.time > midTime) {
	            dimStart = workTime.time
            }
        }
	    if (settings.dimTimeEnd != null && settings.dimTimeEnd != "") {
            workTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", settings.dimTimeEnd)
            if (workTime < sunsetTime) {
	            dimEnd = workTime.time
            }
        }
	}

    logDebug("sunriseTime: ${sunriseTime}")
    logDebug("sunsetTime: ${sunsetTime}")
    logDebug("midTime: ${midTime}")
    logDebug("currentTime: ${currentTime}")
	logDebug("brightenStart: ${brightenStart}")
    logDebug("brightenEnd: ${brightenEnd}")
    logDebug("dimStart: ${dimStart}")
	logDebug("dimEnd: ${dimEnd}")

    def int warmCT = 2700
	def int coldCT = 6500
	if (settings.useCTOverrides) {
	    if (settings.warmCTOverride != null && settings.warmCTOverride != "") {
		    warmCT = settings.warmCTOverride
        }
	    if (settings.coldCTOverride != null && settings.coldCTOverride != "") {
		    coldCT = settings.coldCTOverride
        }
	}
	def int rangeCT = coldCT - warmCT
	
    def int minBrightness = 1
    def int maxBrightness = 100
    if (settings.useBrightnessOverrides) {
	    if (settings.minBrightnessOverride != null && settings.minBrightnessOverride > 1) {
		    minBrightness = settings.minBrightnessOverride
	    }
	    if (settings.maxBrightnessOverride != null && settings.maxBrightnessOverride < 100) {
		    maxBrightness = settings.maxBrightnessOverride
	    }
    }
	
    def int baseBrightness
	def int rangeBrightness
	switch (settings.brightnessProfile) {
	    case "Gradual":
		    baseBrightness = minBrightness
			rangeBrightness = maxBrightness - minBrightness
		    break
		case "Accelerated":
		    baseBrightness = minBrightness
			rangeBrightness = 99
		    break
		case "Delayed":
		    baseBrightness = 1
			rangeBrightness = 99
		    break
        default :
		    baseBrightness = minBrightness
			rangeBrightness = maxBrightness - minBrightness
		    break
	}
	
    if (currentTime >= sunriseTime.time && currentTime <= sunsetTime.time) {
        if (currentTime < midTime) {
            colorTemp = warmCT + ((currentTime - sunriseTime.time) / (midTime - sunriseTime.time) * rangeCT)
			
			if (currentTime < brightenStart) {
				brightness = minBrightness	
			}
			else if (currentTime < brightenEnd) {
				brightness = baseBrightness + ((currentTime - brightenStart) / (brightenEnd - brightenStart) * rangeBrightness)
				if (brightness < minBrightness) {
				    brightness = minBrightness
				}
				if (brightness > maxBrightness) {
				    brightness = maxBrightness
				}
			}
			else {
				brightness = maxBrightness
			}
        }
        else {
            colorTemp = warmCT + ((sunsetTime.time - currentTime) / (sunsetTime.time - midTime) * rangeCT)
			
            if (currentTime < dimStart) {
				brightness = maxBrightness
			}
			else if (currentTime < dimEnd) {
			    brightness = baseBrightness + ((dimEnd - currentTime) / (dimEnd - dimStart) * rangeBrightness)
				if (brightness < minBrightness) {
				    brightness = minBrightness
				}
				if (brightness > maxBrightness) {
				    brightness = maxBrightness
				}
			}
			else {
				brightness = minBrightness	
			}
        }
		if (!state.scheduleActive)  {
            schedule("0 */5 * * * ?", eventHandler, [data: "Update Lights"])
		    state.scheduleActive = true
		}
	}
	else {
	    if (state.scheduleActive) {
		    unschedule(eventHandler)
	        state.scheduleActive = false
		}
	    colorTemp = warmCT
		brightness = minBrightness
    }

	if (settings.useModeOverrides) {
        switch (location.mode) {
            case mode1Override:
    	        if (settings.mode1OverrideValue != null && settings.mode1OverrideValue > 0 && settings.mode1OverrideValue <= 100) {
    		        brightness = settings.mode1OverrideValue
    	        }
    			if (settings.mode1OverrideColorTemperature != null && settings.mode1OverrideColorTemperature != "") {
    			    colorTemp = settings.mode1OverrideColorTemperature
    			}
                logDebug("Mode1 Override for ${location.mode} mode")
                break
            case mode2Override:
    	        if (settings.mode2OverrideValue != null && settings.mode2OverrideValue > 0 && settings.mode2OverrideValue <= 100) {
    		        brightness = settings.mode2OverrideValue
    	        }
    			if (settings.mode2OverrideColorTemperature != null && settings.mode2OverrideColorTemperature != "") {
    			    colorTemp = settings.mode2OverrideColorTemperature
    			}
                logDebug("Mode2 Override for ${location.mode} mode")
                break
            case mode3Override:
	            if (settings.mode3OverrideValue != null && settings.mode3OverrideValue > 0 && settings.mode3OverrideValue <= 100) {
		            brightness = settings.mode3OverrideValue
	            }
    			if (settings.mode3OverrideColorTemperature != null && settings.mode3OverrideColorTemperature != "") {
	    		    colorTemp = settings.mode3OverrideColorTemperature
		    	}
                logDebug("Mode3 Override for ${location.mode} mode")
                break
            case mode4Override:
	            if (settings.mode4OverrideValue != null && settings.mode4OverrideValue > 0 && settings.mode4OverrideValue <= 100) {
		            brightness = settings.mode4OverrideValue
    	        }
	    		if (settings.mode4OverrideColorTemperature != null && settings.mode4OverrideColorTemperature != "") {
		    	    colorTemp = settings.mode4OverrideColorTemperature
			    }
                logDebug("Mode4 Override for ${location.mode} mode")
                break
            case mode5Override:
	            if (settings.mode5OverrideValue != null && settings.mode5OverrideValue > 0 && settings.mode5OverrideValue <= 100) {
    		        brightness = settings.mode5OverrideValue
	            }
		    	if (settings.mode5OverrideColorTemperature != null && settings.mode5OverrideColorTemperature != "") {
			        colorTemp = settings.mode5OverrideColorTemperature
    			}
                logDebug("Mode5 Override for ${location.mode} mode")
                break
            case mode6Override:
	            if (settings.mode6OverrideValue != null && settings.mode6OverrideValue > 0 && settings.mode6OverrideValue <= 100) {
		            brightness = settings.mode6OverrideValue
	            }
    			if (settings.mode6OverrideColorTemperature != null && settings.mode6OverrideColorTemperature != "") {
	    		    colorTemp = settings.mode6OverrideColorTemperature
		    	}
                logDebug("Mode6 Override for ${location.mode} mode")
                break
            case mode7Override:
	            if (settings.mode7OverrideValue != null && settings.mode7OverrideValue > 0 && settings.mode7OverrideValue <= 100) {
		            brightness = settings.mode7OverrideValue
    	        }
	    		if (settings.mode7OverrideColorTemperature != null && settings.mode7OverrideColorTemperature != "") {
		    	    colorTemp = settings.mode7OverrideColorTemperature
			    }
                logDebug("Mode7 Override for ${location.mode} mode")
                break
            case mode8Override:
	            if (settings.mode8OverrideValue != null && settings.mode8OverrideValue > 0 && settings.mode8OverrideValue <= 100) {
    		        brightness = settings.mode8OverrideValue
	            }
		    	if (settings.mode8OverrideColorTemperature != null && settings.mode8OverrideColorTemperature != "") {
			        colorTemp = settings.mode8OverrideColorTemperature
    			}
                logDebug("Mode8 Override for ${location.mode} mode")
                break
            case mode9Override:
	            if (settings.mode9OverrideValue != null && settings.mode9OverrideValue > 0 && settings.mode9OverrideValue <= 100) {
		            brightness = settings.mode9OverrideValue
	            }
    			if (settings.mode9OverrideColorTemperature != null && settings.mode9OverrideColorTemperature != "") {
	    		    colorTemp = settings.mode9OverrideColorTemperature
		    	}
                logDebug("Mode9 Override for ${location.mode} mode")
                break
            case mode10Override:
	            if (settings.mode10OverrideValue != null && settings.mode10OverrideValue > 0 && settings.mode10OverrideValue <= 100) {
		            brightness = settings.mode10OverrideValue
    	        }
	    		if (settings.mode10OverrideColorTemperature != null && settings.mode10OverrideColorTemperature != "") {
		    	    colorTemp = settings.mode10OverrideColorTemperature
			    }
                logDebug("Mode10 Override for ${location.mode} mode")
                break
            default :
                logDebug("No override for ${location.mode} mode")
                break
        }
	}
	else {
	    logDebug("Mode overrides are not active")
	}

    return [colorTemp: Math.round(colorTemp), brightness: Math.round(brightness)]
}

private def getSunriseTime() {
    def sunRiseSet
    def sunriseTime
	
	if (settings.useSunOverrides && settings.sunriseOverride != null && settings.sunriseOverride != "") {
        sunriseTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", settings.sunriseOverride)
        logDebug("Sunrise Override is ${sunriseTime}")
	}
	else {
        sunRiseSet = getSunriseAndSunset()
        sunriseTime = sunRiseSet.sunrise
        logDebug("System Sunrise time is ${sunriseTime}")
	}
	if (settings.useSunOffsets && settings.sunriseOffset != null && settings.sunriseOffset != "") {
		sunriseTime = sunriseTime.plusMinutes(settings.sunriseOffset)
        logDebug("Sunrise offset to ${sunriseTime}")
	}
	
    return sunriseTime
}

private def getSunsetTime(){
    def sunRiseSet
    def sunsetTime

	if (settings.useSunOverrides && settings.sunsetOverride != null && settings.sunsetOverride != "") {
        sunsetTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", settings.sunsetOverride)
        logDebug("Sunset Override is ${sunsetTime}")
	}
	else {
        sunRiseSet = getSunriseAndSunset()
        sunsetTime = sunRiseSet.sunset
        logDebug("System Sunset time is ${sunsetTime}")
	}
	if (settings.useSunOffsets && settings.sunsetOffset != null && settings.sunsetOffset != "") {
		sunsetTime = sunsetTime.plusMinutes(settings.sunsetOffset)
        logDebug("Sunset offset to ${sunsetTime}")
	}
	
    return sunsetTime
}

def ctToRGB(ct) {

    if(ct < 1000) { ct = 1000 }
    if(ct > 40000) { ct = 40000 }

    ct = ct / 100

    //red
    def r
    if(ct <= 66) { r = 255 }
    else { r = 329.698727446 * ((ct - 60) ** -0.1332047592) }
    if(r < 0) { r = 0 }
    if(r > 255) { r = 255 }

    //green
    def g
    if (ct <= 66) { g = 99.4708025861 * Math.log(ct) - 161.1195681661 }
    else { g = 288.1221695283 * ((ct - 60) ** -0.0755148492) }
    if(g < 0) { g = 0 }
    if(g > 255) { g = 255 }

    //blue
    def b
    if(ct >= 66) { b = 255 }
    else if(ct <= 19) { b = 0 }
    else { b = 138.5177312231 * Math.log(ct - 10) - 305.0447927307 }
    if(b < 0) { b = 0 }
    if(b > 255) { b = 255 }

    def rgb = [:]
    rgb = [r: r as Integer, g: g as Integer, b: b as Integer]
    rgb
}

def rgbToHex(rgb) {
    return "#" + Integer.toHexString(rgb.r).padLeft(2,'0') + Integer.toHexString(rgb.g).padLeft(2,'0') + Integer.toHexString(rgb.b).padLeft(2,'0')
}

def rgbToHSV(rgb) {
    def h, s, v

    def r = rgb.r / 255
    def g = rgb.g / 255
    def b = rgb.b / 255

    def max = [r, g, b].max()
    def min = [r, g, b].min()

    def delta = max - min

    //hue
    if(delta == 0) { h = 0}
    else if(max == r) {
        double dub = (g - b) / delta
        h = 60 * (dub % 6)
    }
    else if(max == g) { h = 60 * (((b - r) / delta) + 2) }
    else if(max == b) { h = 60 * (((r - g) / delta) + 4) }

    //saturation
    if(max == 0) { s = 0 }
    else { s = (delta / max) * 100 }

    //value
    v = max * 100

    logDebug("r=${rgb.r}, g=${rgb.g}, b=${rgb.b}, max=${max}, min=${min}, delta=${delta}, h=${Math.round(h)}, s=${Math.round(s)}, v=${Math.round(v)}")
    return [h: Math.round(h / 3.6), s: Math.round(s), v: Math.round(v)]
}
