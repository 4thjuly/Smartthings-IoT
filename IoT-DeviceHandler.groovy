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


    tiles {      
        valueTile("battery", "device.battery", decoration: "flat", height: 1, width: 1, inactiveLabel: false, canChangeIcon: false) {
            state "battery", label:'${currentValue}v'
        }
        
        valueTile("lightLevel", "device.lightLevel", decoration: "flat", height: 1, width: 1, inactiveLabel: false, canChangeIcon: false) {
            state "lightLevel", label:'${currentValue}'
        }
        
        standardTile("relay", "device.relay", width: 1, height: 1, canChangeIcon: true) {
            state "off", label: '${currentValue}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff"
            state "on", label: '${currentValue}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00a0dc"
        }
        
    }
	main("battery")
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    state.deviceIP = 0
}

def parse(description) {
    //log.debug "Parse: ${description}"
    def events = []
    def descMap = parseDescriptionAsMap(description)
    def headers = new String(descMap["headers"].decodeBase64())
    def body = new String(descMap["body"].decodeBase64())
    def slurper = new JsonSlurper()
    def result = slurper.parseText(body)
    //log.debug "Headers: ${headers}"
    log.debug "Parse result: ${result}"

	if (result.containsKey("IP")) {
    	state.deviceIP = result.IP;
    }
    if (result.containsKey("A1")) {
    	def lightLevel = result.A1;
        lightLevel = lightLevel.setScale(3, BigDecimal.ROUND_HALF_UP)
    	events << createEvent(name:"lightLevel", value:lightLevel, data:result)
    }
    if (result.containsKey("A0")) {
    	def volts = result.A0 * 33.0;
		volts = volts.setScale(2, BigDecimal.ROUND_HALF_UP)
    	events << createEvent(name:"battery", value:volts, data:result)
    }

    log.debug "Parse events: ${events}"

    return events
}

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
    log.debug "Executing 'on' for ${deviceIP}";
    if (!deviceIP) return;
	
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
	log.debug "Executing 'off' "
    def deviceIP = state.deviceIP;
    log.debug "Executing 'on' for ${deviceIP}";
    if (!deviceIP) return;
	
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
