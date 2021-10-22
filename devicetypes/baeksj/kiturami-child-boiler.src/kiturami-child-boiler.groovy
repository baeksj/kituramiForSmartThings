/**
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
 */

metadata {
    definition(name: "Kiturami-child-boiler", namespace: "baeksj", author: "baeksj@gmail.com", ocfDeviceType: "oic.d.thermostat") {
        capability "Switch"
        capability "Thermostat Mode"
        capability "Thermostat Heating Setpoint"
        capability "Temperature Measurement"
        capability "Sensor"
    }

    simulator {
    }
}

// parse events into attributes
def parse(String description) {
    log.debug "Parsing child '${description}' ${device.deviceNetworkId}"
}

def installed() {
    log.debug "installed() child ${device.deviceNetworkId}"
    sendEvent(name: "supportedThermostatModes", value: ["off","away","heat","resume","schedule"])
}

def uninstalled() {
    log.debug "uninstalled() child ${device.deviceNetworkId}"
    parent.delete()
}

def updated() {
    log.debug "updated() child ${device.deviceNetworkId}"
}

def on() {
    parent.onForDevice(device.deviceNetworkId)
}

def off() {
    parent.offForDevice(device.deviceNetworkId)
}

def setHeatingSetpoint(targetTempCelcius) {
    parent.setHeatingSetpointForDevice(targetTempCelcius, device.deviceNetworkId);
}

def heat() {
    parent.heatForDevice(device.deviceNetworkId)
}

def setThermostatMode(mode) {
    parent.setThermostatModeForDevice(mode, device.deviceNetworkId)
}

/* Unused capability functions */

def auto(){
    log.debug "auto child ${device.deviceNetworkId}"
}
def cool(){
    log.debug "cool child ${device.deviceNetworkId}"
}
def emergencyHeat(){
    log.debug "emergencyHeat child ${device.deviceNetworkId}"
}