/**
 *  KODI Playback Controls
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
 *  To Use:  Publish Application "For Me", open App properties and copy OAuth Client ID and secret under oAuth tab.
 *  Create command URLs:  https://graph.api.smartthings.com/api/smartapps/installations/<CLIENTID>/<COMMAND>?access_token=<SECRET>
 *  Use xbmccallbacks2 plugin to point to individual command URLs for the following commands: play, stop, pause, resume
 *  Install Smartapp with desired dimmer switches and light levels.  Edit code below for each command as desired.
 *
 */
definition(
    name: "KODI Playback Controls",
    namespace: "Thildemar",
    author: "Thildemar",
    description: "Provide simple endpoint template for KODI (XBMC) callbacks2 plugin.  Plan to execute automation code within SmartApp instead of relying on 3rd party scripts to trigger multiple endpoints.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment1-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment1-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment1-icn@2x.png",
    oauth: true)


preferences {
	section("Lights to Control") {
		input "switches", "capability.switchLevel", required: true, title: "Which Switches?", multiple: true
	}
    section("Level to set Lights to (-1 for last known level):") {
		input "playLevel", "number", required: true, title: "On Playback"
        input "pauseLevel", "number", required: true, title: "On Pause"
        input "resumeLevel", "number", required: true, title: "On Resume"
        input "stopLevel", "number", required: true, title: "On Stop"
	}
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

def installed() {}

def updated() {}



void play() {
	//Code to execute when playback started in KODI
    log.debug "Play command started"
    if (playLevel == -1){
    	log.debug "Restoring Last Known Light Levels"
    	restoreLast(switches)
    }else{
    	log.debug "Setting lights to ${playLevel} %"
    	switches.setLevel(playLevel)
    }
}
void stop() {
	//Code to execute when playback stopped in KODI
    log.debug "Stop command started"
    if (stopLevel == -1){
    	log.debug "Restoring Last Known Light Levels"
        restoreLast(switches)
    }else{
    	log.debug "Setting lights to ${stopLevel} %"
        switches.setLevel(stopLevel)
    }
}
void pause() {
	//Code to execute when playback paused in KODI
    log.debug "Pause command started"
    if (pauseLevel == -1){
    	log.debug "Restoring Last Known Light Levels"
        restoreLast(switches)
    }else{
    	log.debug "Setting lights to ${pauseLevel} %"
        switches.setLevel(pauseLevel)
    }
}
void resume() {
	//Code to execute when playback resumed in KODI
    log.debug "Resume command started"
    if (resumeLevel == -1){
    	log.debug "Restoring Last Known Light Levels"
        restoreLast(switches)
    }else{
    	log.debug "Setting lights to ${resumeLevel} %"
        switches.setLevel(resumeLevel)
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
    	//Get events for this switch in the last day
    	def swEvents = sw.eventsSince(new Date() - 1, [max: 1000])
		log.debug "Found ${swEvents?.size() ?: 0} Switch events for ${sw.displayName}"
        //Find Last Event Where switch was turned on/off/level, but not changed by this app
        //Oddly not all events properyly contain the "installedSmartAppId", particularly ones that actual contain useful values
        //In order to filter out events created by this app we have to find the set of evennts for the app control and the actual action
        //the first 8 char of the event ID seem to be unique, but the rest seems to be the same for any grouping of events, so match on that (substring)
		def lastState = swEvents.find {
        (it.name == "level" || it.name == "switch") && (swEvents.find{it2 -> it2.id.toString().substring(8) == it.id.toString().substring(8) && it2.installedSmartAppId == app.id} == null)
        }
        //If we found one restore to that
        if (lastState){       
            log.debug "Last External Event - Event ID: ${lastState.id} | AppID: ${lastState.installedSmartAppId} | Description: ${lastState.descriptionText} | Name: ${lastState.displayName} (${lastState.name}) | App: ${lastState.installedSmartApp} | Value: ${lastState.stringValue} | Source: ${lastState.source} | Desc: ${lastState.description}"
            if (lastState.stringValue == "on"){
            	sw.setLevel(100)	
            }else if (lastState.name == "level"){
                try {
                    sw.setLevel(lastState.integerValue)
                }catch (e) { //This should not fire as level events should have an integer value
                    log.debug "Trying to get the integerValue for ${lastState.name} threw an exception: $e"
                }
            }else{ //If no event or last action was "off" assume we wanted that light off
            	sw.setLevel(0)
            }
    	}	
    }
}

private void DeviceLogs(devices){
	devices.each{ device ->
    	def devEvents = device.eventsSince(new Date() - 1, [max: 1000])
    	devEvents.each{ev ->
        	log.debug "Event Date: ${ev.date} | ID: ${ev.id} | AppID: ${ev.installedSmartAppId} | Description: ${ev.descriptionText} | Name: ${ev.displayName} (${ev.name}) | App: ${ev.installedSmartApp} | Value: ${ev.stringValue} | Source: ${ev.source} | Desc: ${ev.description}"
        }
    }

}
