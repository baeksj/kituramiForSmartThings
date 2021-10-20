/**
 *
 *  DTH: Kiturami multiroom boiler for smartthings
 *  AUTHOR: github.com/baeksj, baeksj@gmail.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * Changes
 * 0.1.2021051401 Initial
 * 0.2.2021051501 Add function to operate KRB API directly, and you can choose it in settings
 */

import groovy.json.JsonSlurper
import physicalgraph.device.HubResponse
import java.security.MessageDigest

metadata {
    definition(name: "Kiturami-multiroom-boiler", namespace: "baeksj", author: "baeksj@gmail.com", ocfDeviceType: "oic.d.thermostat") {
        capability "Switch"
        capability "Thermostat Mode"
        capability "Thermostat Heating Setpoint"
        capability "Temperature Measurement"
        capability "Sensor"

        command "refresh"
    }

    preferences {
        input "krbDirectYn", "enum", title: "Direct connnect to KRB API", description: "Yes for direct KRB API, No for internal proxy", options: ["Yes","No"], defaultValue: "Yes", required: true
        input "krbAddress", "text", title: "Internal API Reverse proxy", description: "Internal proxy address as IP:PORT ", required: false
        input "memberId", "text", title: "KRB user id", description: "Enter your kiturami id", required: true
        input "password", "password", title: "KRB password", description: "Enter your kiturami password ", required: true
        input "offMethod", "enum", title: "Mode on switch off", description: "Choose mode off/away on switch off", options: ["off", "away"], defaultValue: "away", required: true
    }

    simulator {
    }
}

// parse events into attributes
def parse(String description) {
    log.debug "Parsing '${description}'"
}

def installed() {
    log.debug "installed()"
    init()
}

def uninstalled() {
    log.debug "uninstalled()"
    unschedule()
}

def updated() {
    log.debug "updated()"
    unschedule()
    init()
}

def init(){
    log.debug "init"
    state.targetTemp = [:]
    sendEvent(name: "switch", value: "off")
    sendEvent(name: "supportedThermostatModes", value: ["off","away","heat","resume"])
    refresh()
    runEvery1Minute(refresh)
}

def refresh() {
    log.debug "refresh()"

    if(memberId && password && (krbAddress || krbDirectYn == "Yes")){

        if(!state.authKey) {
            //in case not yet login
            executeKrbPreprocess()
        } else if(!state.slaveId) {
            //login but no devices, abnormal case
            executeKrbDeviceList()
        } else {
            //refresh device status
            executeKrbDeviceStatus()
        }

        sendEvent(name: "lastCheckin", value: (new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)), displayed: false)
    }
    else
        log.error "Found missing propertie(s), Please setup your device properties on your device details. ${settings}"
}

/* Functions for capability */

def on(dni=null) {
    log.debug "on: ${dni}"

    def slaveId = getSlaveId(dni)

    executeKrbHeat(slaveId)
}

def off(dni=null) {
    log.debug "off: ${dni}"

    def slaveId = getSlaveId(dni)

    if(offMethod == "off")
        executeKrbOff(slaveId)
    else
        executeKrbAway(slaveId)
}

def setHeatingSetpoint(targetTempCelcius, dni=null) {
    log.debug "setHeatingSetpoint: ${targetTempCelcius}, ${dni}"

    def slaveId = getSlaveId(dni)
    executeKrbHeat(slaveId, targetTempCelcius);
}

def heat(dni=null) {
    log.debug "heat ${dni}"

    def slaveId = getSlaveId(dni)
    executeKrbHeat(slaveId);
}

def setThermostatMode(mode, dni=null) {
    log.debug "setThermostatMode ${mode} ${dni}"

    def slaveId = getSlaveId(dni)

    //"off","away","heat","resume"
    switch(mode) {
        case "off":
            if(offMethod == "off")
                executeKrbOff(slaveId)
            else
                executeKrbAway(slaveId)
            break;
        case "away":
            executeKrbAway(slaveId)
            break;
        case "heat":
            executeKrbHeat(slaveId)
            break;
        case "resume":
            executeKrbBath()
            break;
    }
}

/* Unused capability functions */

def auto(){
    log.debug "auto"
}
def cool(){
    log.debug "cool"
}
def emergencyHeat(){
    log.debug "emergencyHeat"
}

/* Functions for KRB API */

def getOperation(map = null) {

    def operation = [
            login: [
                    command: "/api/member/login",
                    body: """{"memberId": "${memberId}","password": "${getSHA256EncodeString(password)}"}"""
            ],
            deviceInfo: [
                    command: "/api/member/getMemberDeviceList",
                    body: """{"parentId": "1"}"""
            ],
            deviceList: [
                    command: "/api/device/getDeviceInfo",
                    body: """{"nodeId": "${state.nodeId}","parentId": "1"}"""
            ],
            deviceModeInfo: [
                    command: "/api/device/getDeviceModeInfo",
                    body: """{"nodeId": "${state.nodeId}", "actionId":"0102", "parentId":"1", "slaveId": "${map?.slaveId}"}"""
            ],
            deviceControl: [
                    command: "/api/device/deviceControl",
                    body: """{"nodeIds": ["${state.nodeId}"], "messageId": "${map?.messageId}", "messageBody": "${map?.messageBody}"}"""
            ],
            isAliveNormal: [
                    command: "/api/device/isAliveNormal",
                    body: """{"nodeId": "${state.nodeId}","parentId": "1"}"""
            ]
    ]

    operation
}

def executeKrbOn(slaveId) {
    def controlMessage = [messageId: "0101", messageBody: "${slaveId}0000000001"]
    log.debug "turnOn: ${controlMessage}"
    executeAPICommand(getOperation(controlMessage).deviceControl, controlCallback)
}

def executeKrbOff(slaveId) {
    def controlMessage = [messageId: "0101", messageBody: "${slaveId}0000000002"]
    log.debug "turnOff: ${controlMessage}"
    executeAPICommand(getOperation(controlMessage).deviceControl, controlCallback)
}

def executeKrbHeat(slaveId,targetTempCelcius=null) {
    if(!targetTempCelcius || targetTempCelcius < 10 || targetTempCelcius > 40) {
        targetTempCelcius = state.targetTemp[slaveId]
    }
    def controlMessage = [
            messageId: "0102",
            messageBody: "${slaveId}000000${Integer.toHexString(targetTempCelcius)}00"
    ]
    log.debug "Heat: ${controlMessage}"
    executeAPICommand(getOperation(controlMessage).deviceControl, controlCallback)
}

def executeKrbBath() {
    def controlMessage = [
            messageId: "0105",
            messageBody: "00000000${Integer.toHexString(state.targetTemp["01"])}00"
    ]
    log.debug "Bath: ${controlMessage}"
    executeAPICommand(getOperation(controlMessage).deviceControl, controlCallback)
}

def executeKrbAway(slaveId) {
    def controlMessage = [messageId: "0106", messageBody: "${slaveId}0200000000"]
    log.debug "Away: ${controlMessage}"
    executeAPICommand(getOperation(controlMessage).deviceControl, controlCallback)
}

def executeKrbPreprocess() {
    executeAPICommand(operation.login, loginCallback)
}

def executeKrbDeviceList() {
    executeAPICommand(operation.deviceList, deviceListCallback)
}

def executeKrbDeviceStatus() {
    //executeAPICommand(operation.isAliveNormal, executeKrbRealDeviceStatus)
    log.debug "wait for run executeKrbRealDeviceStatus"
//    runIn(2, executeKrbRealDeviceStatus)
    executeKrbRealDeviceStatus()
}

//def executeKrbRealDeviceStatus(hubResponse, response=null) {
//    def jsonObj = getJsonResponse(hubResponse, response)
//    log.debug "isAlive: ${jsonObj}"
def executeKrbRealDeviceStatus() {

    log.debug "executeKrbRealDeviceStatus"

    //master device
    executeAPICommand(getOperation([slaveId: state.slaveId]).deviceModeInfo, deviceStatusCallback)
    //child devices
    childDevices?.each {
        executeAPICommand(getOperation([slaveId: getSlaveId(it.deviceNetworkId)]).deviceModeInfo, deviceStatusCallback)
    }
}

def controlCallback(hubResponse, response=null) {
    //wait for a sec for KRB device status sync
    def jsonObj = getJsonResponse(hubResponse, response)
    log.debug "Control result: ${jsonObj}"
    if(jsonObj.successFlag) executeKrbDeviceStatus()
}

def deviceStatusCallback(hubResponse, response=null) {
    try {
        def jsonObj = getJsonResponse(hubResponse, response)

        if(jsonObj) {

            log.debug "deviceStatus: slaveId: ${jsonObj.slaveId}, slaveAlias: ${jsonObj.slaveAlias}, deviceMode: ${jsonObj.deviceMode}, value: ${Integer.parseInt(jsonObj.value, 16)}"
            //current temperature
            state.targetTemp.put(jsonObj.slaveId, Integer.parseInt(jsonObj.value, 16))

            sendDeviceEvent([name: "temperature", value: Integer.parseInt(jsonObj.currentTemp, 16), unit: getTemperatureScale(), displayed: false], jsonObj.slaveId)
            sendDeviceEvent([name: "heatingSetpoint", value: state.targetTemp[jsonObj.slaveId], unit: getTemperatureScale(),displayed: false], jsonObj.slaveId)

            switch (jsonObj.deviceMode) {
                case "0101":
                    sendDeviceEvent([name: "switch", value: "off", displayed: false], jsonObj.slaveId)
                    sendDeviceEvent([name: "thermostatMode", value: "off", displayed: false], jsonObj.slaveId)
                    break
                case "0102":
                    sendDeviceEvent([name: "switch", value: "on", displayed: false], jsonObj.slaveId)
                    sendDeviceEvent([name: "thermostatMode", value: "heat", displayed: false], jsonObj.slaveId)
                    break
                case "0105":
                    sendDeviceEvent([name: "switch", value: "on", displayed: false], jsonObj.slaveId)
                    sendDeviceEvent([name: "thermostatMode", value: "resume", displayed: false], jsonObj.slaveId)
                    break
                case "0106":
                    sendDeviceEvent([name: "switch", value: "off", displayed: false], jsonObj.slaveId)
                    sendDeviceEvent([name: "thermostatMode", value: "away", displayed: false], jsonObj.slaveId)
                    break
            }
        }
    } catch(e) {
        log.error e
    }
}

def loginCallback(hubResponse, response=null) {
    state.authKey = getJsonResponse(hubResponse, response).authKey
    if(state.authKey) executeAPICommand(operation.deviceInfo, deviceInfoCallback);
}

def deviceInfoCallback(hubResponse, response=null) {
    state.nodeId = getJsonResponse(hubResponse, response).memberDeviceList[0]?.nodeId
    if(!state.slaveId) executeKrbDeviceList()
}

def deviceListCallback(hubResponse, response=null) {
    try {
        def jsonObj = getJsonResponse(hubResponse, response)

        for(slave in jsonObj.deviceSlaveInfo) {
            //01 is master
            if(slave.slaveId != "01") {
                addChildDevice("Kiturami-child-boiler", "${device.deviceNetworkId}:${slave.slaveId}", device.hubId,
                        [completedSetup: true, label: "${device.displayName}-${slave.alias}", isComponent: false])

                log.debug "createChildDevice dni: ${device.deviceNetworkId}:${slave.slaveId}, label: ${device.displayName}-${slave.alias}"
            }
        }

        state.slaveId = "01"

        //Update device status, after 5 secs child device init time
        runIn(5, executeKrbDeviceStatus)

    } catch(e) {
        log.error e
    }
}

/* Utility Functions */

def getSlaveId(dni) {
    def slaveId = state.slaveId
    if(dni) slaveId = dni.split(":")[1]

    slaveId
}

def sendDeviceEvent(eventMap, slaveId=null) {
    if(slaveId == "01") {
        sendEvent(eventMap)
    } else {
        def childDevice = childDevices.find {
            it.deviceNetworkId.endsWith(slaveId)
        }
        childDevice.sendEvent(eventMap)
    }
}

def getSHA256EncodeString(str) {

    def mac = MessageDigest.getInstance("SHA-256")
    def signatureBytes = mac.digest(str.getBytes())

    def hexString = ""

    for (int j=0; j < signatureBytes.length; j++) {
        def hex = Integer.toHexString(0xff & signatureBytes[j]);
        if(hex.length()==1) hexString += '0'

        hexString += hex;
    }

    hexString
}

def getJsonResponse(hubResponse, response) {
    response ? response.data : new JsonSlurper().parseText(parseLanMessage(hubResponse.description).body)
}

def executeAPICommand(operation, _callback=null) {
    if(krbDirectYn == null || krbDirectYn == "Yes")
        executeAPIDirect(operation, _callback)
    else
        executeAPIViaProxy(operation, _callback)
}

def executeAPIViaProxy(operation, _callback = null) {
    try {
        def options = [
                "method": "POST",
                "path": operation.command,
                "headers": [
                        "HOST": "${krbAddress}",
                        "Content-Type": "application/json; charset=UTF-8",
                        "AUTH-KEY": state.authKey
                ],
                "body": operation.body
        ]

        def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
        sendHubCommand(myhubAction)

    } catch(e) {
        log.error e
    }
}

def executeAPIDirect(operation, _callback = null) {
    def options = [
            uri: "https://igis.krb.co.kr",
            path: operation.command,
            contentType: "application/json; charset=UTF-8",
            headers: [ "AUTH-KEY": state.authKey ],
            body: operation.body
    ]

    httpPostJson(options) {
        if(_callback) "${_callback}"(null, it)
    }
}
