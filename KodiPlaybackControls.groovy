/**
 *  KODI Callback Endpoint
 *
 *  Copyright 2015 Thildemar
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
 *  To Use:  Publish Application "For Me", use xbmccallbacks2 plugin to point to individual command URLs for the following commands: play, stop, pause, resume
 *  Install Smartapp with desired switches and light levels.  Edit code below for each command as desired.
 *
 */
definition(
    name: "KODI Callback Endpoint",
    namespace: "Thildemar",
    author: "Thildemar",
    description: "Provides simple lighting control endpoint for KODI (XBMC) callbacks2 plugin.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment1-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment1-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment1-icn@2x.png",
    oauth: true)


preferences {
	

	page(name: "pgSettings", title: "Settings",
          nextPage: "pgURL", uninstall: true) {
        section("Lights to Control") {
            input "switches", "capability.switch", required: true, title: "Which Switches?", multiple: true
        }
        section("Level to set Lights to (101 for last known level):") {
            input "playLevel", "number", required: true, title: "On Playback", defaultValue:"0"
            input "pauseLevel", "number", required: true, title: "On Pause", defaultValue:"40"
            input "resumeLevel", "number", required: true, title: "On Resume", defaultValue:"0"
            input "stopLevel", "number", required: true, title: "On Stop", defaultValue:"101"
        }
        section("Instance Preferences"){
        	label(name: "instanceLabel", title: "Label for this Instance", required: false, multiple: false)
        	icon(title: "Icon for this Instance", required: false)
            	mode(title: "Run only in these modes")
        }
    }
    
    page(name: "pgURL", title: "Instructions", install: true, uninstall: true)
    
}

mappings {

	path("/play") {
		action: [
			GET: "play"
		]
	}
	path("/stop") {
		action: [
			GET: "stop"
		]
	}
	path("/pause") {
		action: [
			GET: "pause"
		]
	}
    path("/resume") {
        action: [
            GET: "resume"
        ]
	}  
    path("/logs") {
        action: [
            GET: "logs"
        ]
	} 
}


//PAGES
///////////////////////////////
def pgURL(){
	if (!state.accessToken) {
    	createAccessToken() 
    }
    def url = apiServerUrl("/api/token/${state.accessToken}/smartapps/installations/${app.id}/")
	dynamicPage(name: "pgURL") {
    	section("Instructions") {
        	paragraph "This app is designed to work with the xbmc.callbacks2 plugin for Kodi. Please download and install callbacks2 and in its settings assign the following URLs for corresponding events:"
            input "playvalue", "text", title:"Web address to copy for play command:", defaultValue:"${url}play"
    		input "stopvalue", "text", title:"Web address to copy for stop command:", defaultValue:"${url}stop"
            input "pausevalue", "text", title:"Web address to copy for pause command:", defaultValue:"${url}pause"
    		input "resumevalue", "text", title:"Web address to copy for resume command:", defaultValue:"${url}resume"
            paragraph "If you have more than one Kodi install, you may install an additional copy of this app for unique addresses specific to each room."
        }
    }
}

//END PAGES
/////////////////////////

def installed() {}

def updated() {}

def uninstalled(){
	//Clean up Access Token
	revokeAccessToken()
	state.accessToken = null 
}



void play() {
	//Code to execute when playback started in KODI
    log.debug "Play command started"
    if (playLevel == 101){
    	log.debug "Restoring Last Known Light Levels"
    	restoreLast(switches)
    }else if (playLevel <= 100){
    	log.debug "Setting lights to ${playLevel} %"
    	SetLight(switches,playLevel)
    }
}
void stop() {
	//Code to execute when playback stopped in KODI
    log.debug "Stop command started"
    if (stopLevel == 101){
    	log.debug "Restoring Last Known Light Levels"
        restoreLast(switches)
    }else if (stopLevel <= 100){
    	log.debug "Setting lights to ${stopLevel} %"
        SetLight(switches,stopLevel)
    }
}
void pause() {
	//Code to execute when playback paused in KODI
    log.debug "Pause command started"
    if (pauseLevel == 101){
    	log.debug "Restoring Last Known Light Levels"
        restoreLast(switches)
    }else if (pauseLevel <= 100){
    	log.debug "Setting lights to ${pauseLevel} %"
        SetLight(switches,pauseLevel)
    }
}
void resume() {
	//Code to execute when playback resumed in KODI
    log.debug "Resume command started"
    if (resumeLevel == 101){
    	log.debug "Restoring Last Known Light Levels"
        restoreLast(switches)
    }else if (resumeLevel <= 100){
    	log.debug "Setting lights to ${resumeLevel} %"
        SetLight(switches,resumeLevel)
    }
}
void logs() {
	//Debug
    DeviceLogs(switches)
}

def deviceHandler(evt) {}

private void restoreLast(switchList){
	//This will look for the last external (not from this app) setting applied to each switch and set the switch back to that
	//Look at each switch passed
	switchList.each{sw ->
    	def lastState = LastState(sw) //get Last State
        if (lastState){   //As long as we found one, set it    
            SetLight(sw,lastState)
    	}else{ //Otherwise assume it was off
        	SetLight(sw, "off") 
        }
    }
}

private def LastState(device){
	//Get events for this device in the last day
	def devEvents = device.eventsSince(new Date() - 1, [max: 1000])
    //Find Last Event Where switch was turned on/off/level, but not changed by this app
    //Oddly not all events properly contain the "installedSmartAppId", particularly ones that actual contain useful values
    //In order to filter out events created by this app we have to find the set of events for the app control and the actual action
    //the first 8 char of the event ID seem to be unique much of the time, but the rest seems to be the same for any grouping of events, so match on that (substring)
    //In case the substring fails we will also check for events with similar timestamp (within 8 sec)
    def last = devEvents.find {
        (it.name == "level" || it.name == "switch") && (devEvents.find{it2 -> it2.installedSmartAppId == app.id && (it2.id.toString().substring(8) == it.id.toString().substring(8) || Math.sqrt((it2.date.getTime() - it.date.getTime())**2) < 6000 )} == null)
        }
    //If we found one return the stringValue
    if (last){
    	log.debug "Last External Event - Date: ${last.date} | Event ID: ${last.id} | AppID: ${last.installedSmartAppId} | Description: ${last.descriptionText} | Name: ${last.displayName} (${last.name}) | App: ${last.installedSmartApp} | Value: ${last.stringValue} | Source: ${last.source} | Desc: ${last.description}"
    	//if event is "on" find last externally set level as it could be in an older event
        if(last.stringValue == "on"){
            def lastLevel = devEvents.find {
            (it.name == "level") && (devEvents.find{it2 -> it2.installedSmartAppId == app.id && (it2.id.toString().substring(8) == it.id.toString().substring(8) || Math.sqrt((it2.date.getTime() - it.date.getTime())**2) < 6000 )} == null)
            }
        	if(lastLevel){
            	return lastLevel.stringValue 
            }
        }
		return last.stringValue
    }else{
    	return null
    }
}

private void SetLight(switches,value){
	//Set value for one or more lights, translates dimmer values to on/off for basic switches

	//Fix any odd values that could be passed
	if(value.toString().isInteger()){
    	if(value.toInteger() < 0){value = 0}
        if(value.toInteger() > 100){value = 100}
    }else if(value.toString() != "on" && value.toString() != "off"){
    	return //ABORT! Lights do not support commands like "Hamster"
    }
	switches.each{sw ->
    	log.debug "${sw.name} |  ${value}"
    	if(value.toString() == "off" || value.toString() == "0"){ //0 and off are the same here, turn the light off
        	sw.off()
        }else if(value.toString() == "on"  || value.toString() == "100"){ //As stored light level is not really predictable, on should mean 100% for now
        	if(sw.hasCommand("setLevel")){ //setlevel for dimmers, on for basic
            	sw.setLevel(100)
            }else{
            	sw.on()
            }
        }else{ //Otherwise we should have a % value here after cleanup above, use ir or just turn a basic switch on
        	if(sw.hasCommand("setLevel")){//setlevel for dimmers, on for basic
            	sw.setLevel(value.toInteger())
            }else{
            	sw.on()
            }
        }
    }
}

private void DeviceLogs(devices){
	//Output event logs from last day for all devices passed
	devices.each{ device ->
    	def devEvents = device.eventsSince(new Date() - 1, [max: 1000])
    	devEvents.each{ev ->
        	log.debug "Event Date: ${ev.date} | ${ev.date.getTime()} | ID: ${ev.id} | AppID: ${ev.installedSmartAppId} | Description: ${ev.descriptionText} | Name: ${ev.displayName} (${ev.name}) | App: ${ev.installedSmartApp} | Value: ${ev.stringValue} | Source: ${ev.source} | Desc: ${ev.description}"
        }
    }

}
