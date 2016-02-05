/**
 *  KODI Callback Endpoint
 *
 *  Copyright 2015 Thildemar v0.023
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
 *  To Use:  Publish Application "For Me", Make sure KODI Callback Endpoint Light Group App is added (does not need published).  
 *  Use xbmccallbacks2 plugin to point to individual command URLs for the following commands: play, stop, pause, resume
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
	page(name: "pgSettings")
    page(name: "pgURL") 
}
//PAGES
///////////////////////////////
def pgSettings() {
    dynamicPage(name: "pgSettings", title: "Light Control Groups", install: true, uninstall: true) {
        section() {
            app(name: "childApps", appName: "KODI Callback Endpoint Light Group", namespace: "Thildemar", title: "New Light Control Group", multiple: true)
        }
        section("Instance Preferences"){
        	label(name: "instanceLabel", title: "Label for this Instance", required: false, multiple: false)
        	//icon(title: "Icon for this Instance", required: false)
            input "onlyModes", "mode", title: "Only run in these modes", required: false, multiple: true
            input "neverModes", "mode", title: "Never run in these modes", required: false, multiple: true
        }
        section("View URLs"){
        	href( "pgURL", description: "Click here to view URLs", title: "")
        }
    }
}


def pgURL(){
    dynamicPage(name: "pgURL", title: "URLs", uninstall: false, install: false) {
    	if (!state.accessToken) {
        	createAccessToken() 
    	}
    	def url = apiServerUrl("/api/token/${state.accessToken}/smartapps/installations/${app.id}/")
    	section("Instructions") {
            paragraph "This app is designed to work with the Kodi Callbacks plugin for Kodi. Please download and install Kodi Callbacks and in its settings assign the following URLs for corresponding events:"
            input "playvalue", "text", title:"Web address to copy for play command:", required: false, defaultValue:"${url}play"
            input "stopvalue", "text", title:"Web address to copy for stop command:", required: false, defaultValue:"${url}stop"
            input "pausevalue", "text", title:"Web address to copy for pause command:", required: false, defaultValue:"${url}pause"
            input "resumevalue", "text", title:"Web address to copy for resume command:", required: false, defaultValue:"${url}resume"
            input "custom1value", "text", title:"Web address to copy for custom 1 command:", required: false, defaultValue:"${url}custom1"
            input "custom2value", "text", title:"Web address to copy for custom 2 command:", required: false, defaultValue:"${url}custom2"
            paragraph "If you have more than one Kodi install, you may install an additional copy of this app for unique addresses specific to each room."
        }
    }
}

//END PAGES
/////////////////////////
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
    path("/custom1") {
		action: [
			GET: "custom1"
		]
	}
    path("/custom2") {
        action: [
            GET: "custom2"
        ]
	} 
}
def installed() {}

def updated() {}

def uninstalled(){
	//Clean up Access Token
	revokeAccessToken()
	state.accessToken = null 
}



void play() {
	//Code to execute when playback started in KODI
    log.debug "Master Play command started"
    RunCommand(play)
}
void stop() {
	//Code to execute when playback stopped in KODI
    log.debug "Master Stop command started"
    RunCommand(stop)
}
void pause() {
	//Code to execute when playback paused in KODI
    log.debug "Master Pause command started"
    RunCommand(pause)
}
void resume() {
	//Code to execute when playback resumed in KODI
    log.debug "Master Resume command started"
    RunCommand(resume)
}
void custom1() {
	//Code to execute when playback paused in KODI
    log.debug "Master custom1 command started"
    RunCommand(custom1)
}
void custom2() {
	//Code to execute when playback resumed in KODI
    log.debug "Master custom2 command started"
    RunCommand(custom2)
}


def deviceHandler(evt) {}

private void RunCommand(command){
	//Check to see if current mode is in white/black list before we do anything
	if((!onlyModes || location.currentMode in onlyModes) && !(location.currentMode in neverModes)){
    	//Mode is good, go ahead with commands
    	def children = getChildApps()
        log.debug "$children.size() child light groups installed"
        children.each { child ->
            child."${command}"()
        }
    }
}
