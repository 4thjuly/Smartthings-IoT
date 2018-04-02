/**
 *  IoT Device
 *
 *  Copyright 2018 4thjuly
 *
 */
 
import groovy.json.JsonSlurper

metadata {
	definition (name: "IoT Device", namespace: "4thjuly", author: "4thjuly") {
		capability "Sensor"
        capability "Switch"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	// attribute "battery", "NUMBER"
    // attribute "lightLevel", "NUMBER"
    // attribute "relay", "ENUM"

    tiles {      
        valueTile("BatteryTile", "device.battery", height: 1, width: 1) {
            state "battery", label:'${currentValue}v'
        }
        
        valueTile("LightLevelTile", "device.lightLevel", height: 1, width: 1) {
            state "lightLevel", label:'${currentValue}'
        }
        
        standardTile("SwitchTile", "device.relay", width: 1, height: 1) {
            state "off", label: 'off', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff"
            state "on", label: 'on', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00a0dc"
        }
        
    }
	main("SwitchTile")
}

/*
def installed() {
    initialize()
}

def updated() {
    // unsubscribe()
    initialize()
}

def initialize() {
    // state.deviceIP = 0 // Don't reset IP
}
*/

def parse(description) {
    //log.debug "Parse: ${description}"
    def events = []
    def descMap = parseDescriptionAsMap(description)
    def headers = new String(descMap["headers"].decodeBase64())
    def body = new String(descMap["body"].decodeBase64())
    def slurper = new JsonSlurper()
    def result = slurper.parseText(body)
    // log.debug "Parse Headers: ${headers}"
    log.debug "Parse Body: ${body}"
    // log.debug "Parse result: ${result}"

	if (result.containsKey("IP")) {
    	state.deviceIP = result.IP;
    }
    if (result.containsKey("A1")) {
    	def lightLevel = result.A1;
        lightLevel = lightLevel.setScale(3, BigDecimal.ROUND_HALF_UP)
    	events << createEvent(name:"lightLevel", value:lightLevel, isStateChange:true)
    }
    if (result.containsKey("A0")) {
    	def volts = result.A0 * 33.0;
		volts = volts.setScale(2, BigDecimal.ROUND_HALF_UP)
    	events << createEvent(name:"battery", value:volts, isStateChange:true)
    }
    if (result.containsKey("B15")) {
    	def relayState = result.B15 ? "on" : "off";
    	events << createEvent(name:"relay", value:relayState, isStateChange:true)
    }

    log.debug "Parse events: ${events}"
    // runIn(5, postParse);

    return events
}

/*
def postParse() {
    log.debug "PostParse: ${state.relay}"
	sendEvent(name:"relay", value:state.relay, displayed:true, isStateChange:true)
}
*/

def parseDescriptionAsMap(description) {
	description.split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
        
        if (nameAndValue.length == 2) map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
        else map += [(nameAndValue[0].trim()):""]
	}
}

// handle commands
def on() {
    def deviceIP = state.deviceIP;
    if (!deviceIP) { 
	    log.debug "Ignoring 'on', no device IP";
    	return 
    };
	
    def headers = [:] 
    headers.put("HOST", "${deviceIP}:80");
    headers.put("Content-Type", "application/json")
    def hubAction = new physicalgraph.device.HubAction(
        method: "POST",
        path: "/relay",
        body: '{"on":"true"}',
        headers: headers
    )  
    sendHubCommand(hubAction);
    return true;
}

def off() {
    def deviceIP = state.deviceIP;
    log.debug "Executing 'off' for ${deviceIP}";
    if (!deviceIP) { 
	    log.debug "Ignoring 'off', no device IP";
    	return 
    };
	
    def headers = [:] 
    headers.put("HOST", "${deviceIP}:80");
    headers.put("Content-Type", "application/json")
    def hubAction = new physicalgraph.device.HubAction(
        method: "POST",
        path: "/relay",
        body: '{"on":"false"}',
        headers: headers
    )  
    sendHubCommand(hubAction);
    return true;
}

// gets the address of the Hub
def getHubAddress() {
  return device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}

