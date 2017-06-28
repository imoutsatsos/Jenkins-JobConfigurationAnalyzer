/*** BEGIN META {
  "name" : "jobConfigParser",
  "comment" : "Creates a summary of a Jenkins Project Configuration and writes report data files and groovy code artifacts",
  "parameters" : [ 'vJobName','vBuildNumber','vWorkspace'],
  "core": "1.596",
  "authors" : [
    { name : "Ioannis K. Moutsatsos" }
  ]
} END META**/

/**
The console output displayfrom this script can be further improved using the Collapsible Console Sections plugin
You just need to use the generated Section Start/End keywords in the plugin.
**/
/* parameters from job config */
jobName = vJobName
buildNumber=vBuildNumber
workspace = vWorkspace
scriptWorkspace="$workspace/embeddedScripts"
reportWorkspace="$workspace/buildReportData"

//make destination if not exists
if (!new File(workspace).exists()){
 new File(workspace).mkdirs()  
}

//create required build folders
new File(scriptWorkspace).mkdir() 
new File(reportWorkspace).mkdir() 

env = System.getenv()
JENKINS_HOME = env['JENKINS_HOME']
JENKINS_URL = jenkins.model.Jenkins.instance.getRootUrl()
thisProject=binding.variables['workspace'].replace('\\','/')split('/')[-1]
artifactPath="${JENKINS_URL}job/${thisProject}/${buildNumber}/artifact/"
projectPath ="$JENKINS_HOME/jobs/${jobName}/config.xml"
configList = [] //a list to maintain job element configurations
scriptletRunLink="${JENKINS_URL}scriptler/runScript?id="


// read xml configuration file from server local path
println "\n---------------- REVIEWING: $jobName ----------------\n"
def configFile = new File(projectPath)
assert configFile.exists()

def returnMessage = ''
returnMessage = configFile.getText()
//parse the xml response
def project = new XmlParser().parseText(returnMessage)

//Print Project Elements
//println "\nCONFIGURATION_PROJECT_START"
project.children().each {
    if (it.name() in ['actions', 'description', 'keepDependencies', 'canRoam', 'disabled', 'blockBuildWhenDownstreamBuilding', 'blockBuildWhenUpstreamBuilding', 'triggers', 'concurrentBuild']) {
        if (it.text().contains('=') || it.text().contains('<')) {
            println "\t\t ${it.name()}"//: ${script.text()}"
            it.text().split('\\n').each {
                println "\t\t\t $it"
            }

        } else {
            println "\t ${it.name()}: ${it.text()}"
        }
    }
}// end project builder
//println "\nCONFIGURATION_PROJECT_END"

//Print SCM
println "\nCONFIGURATION_SCM_START"
project.scm[0].children().each {
    scmType = it.name()
    scmVesion = it.attributes()
    //visit(it)

}// end SCM
println "\nCONFIGURATION_SCM_END"

// Print Parameters
println "\nCONFIGURATION_PARAMETER_START"
serialId = 0
project.properties.'hudson.model.ParametersDefinitionProperty'[0].'parameterDefinitions'[0].children().each {
    elementConfiguration = [:] //generic map to maintain element configuration
    serialId++
    paramTypePartsList = it.name().split("\\.")
    def paramType = (paramTypePartsList.size() > 0) ? paramTypePartsList[paramTypePartsList.size() - 1] : paramTypePartsList
    it.children().each {
        if (it.name() in ['name']) {
            println "\nPARAMETER----------------${it.value()} ${paramType}----------------"
            elementConfiguration.put('serialId', serialId)
            elementConfiguration.put('elementType', 'parameter')
            elementConfiguration.put('value', it.value().join(','))
            elementConfiguration.put('type', paramType)
        }

    }
    visit(it, elementConfiguration)

} //end parameters
paramFile = new File("$reportWorkspace/paramProps.csv")
paramFile << 'SERIAL_ID,NAME,TYPE,SCRIPTLET,SCRIPTLET_LINK,CODE_LINK,REFERENCED_PARAMS,CLASS,PLUGIN\n'
configList.each {
    if (it.elementType == 'parameter') {
        reportMap = it.findAll { k, v -> k in ['serialId', 'name', 'type', 'scriptlerScriptId','scriptCode','referencedParameters'] }
        plugin = it.find { k, v -> (v as String).contains('plugin') }
        if (plugin != null) {
            //println plugin
            pluginClass = plugin.getKey()
            //println pluginClass
            reportMap.put('pluginClass', pluginClass)
            pluginVers = plugin.getValue()
            reportMap.put('pluginVers', pluginVers)
        }
        paramFile << "${reportMap.serialId},${reportMap.name},${reportMap.type},${reportMap.scriptlerScriptId!=null?reportMap.scriptlerScriptId:'--'},${reportMap.scriptlerScriptId!=null?scriptletRunLink+reportMap.scriptlerScriptId:'--'},${reportMap.scriptCode!=null?reportMap.scriptCode:'--'},${reportMap.referencedParameters?.replace(',', ':')},${reportMap.pluginClass},${reportMap?.pluginVers?.minus('plugin:')}\n"
    }
}// end check for element type
println "\nCONFIGURATION_PARAMETER_END"
//
//Print Builders
println "\nCONFIGURATION_BUILDER_START"
serialId = 0

project.builders[0].children().each {
    elementConfiguration = [:] //generic map to maintain element configuration
    serialId++
    builderTypePartsList = it.name().split("\\.")
    def builderType = (builderTypePartsList.size() > 0) ? builderTypePartsList[builderTypePartsList.size() - 1] : builderTypePartsList
    builderVesion = it.attributes()
    println "\nBUILDER----------------[${builderType}]----------------"
    elementConfiguration.put('serialId', serialId)
    elementConfiguration.put('elementType', 'builder')
    elementConfiguration.put('type', builderType)
    visit(it, elementConfiguration)
}// end project builder
buildFile = new File("$reportWorkspace/builderProps.csv")
buildFile << 'SERIAL_ID,TYPE,SCRIPT_ID,CODE_LINK,CLASS,PLUGIN\n'
configList.each {
//    println it
    if (it.elementType=='builder'){
    reportMap = it.findAll { k, v -> k in ['serialId','type','scriptId','scriptSource','scriptFile','scriptCode'] }
      // println reportMap
    plugin = it.find { k, v -> (v as String).contains('plugin') }
    if (plugin != null) {
        //println plugin
        pluginClass = plugin.getKey()
        //println pluginClass
        reportMap.put('pluginClass', pluginClass)
        pluginVers = plugin.getValue()
        reportMap.put('pluginVers', pluginVers)
    }
    if (reportMap.type=='R'){
    println reportMap
    reportMap.scriptId='R-script'
    reportMap.pluginClass='org.biouno.r.R'
    }
        buildFile <<  "${reportMap.serialId},${reportMap.type},${reportMap.scriptId!=null?reportMap.scriptId:'scriptFile'},${reportMap.scriptId!=null&&reportMap.scriptId!='R-script'?scriptletRunLink+reportMap.scriptId: reportMap.scriptFile!=null?reportMap.scriptFile:reportMap.scriptCode},${reportMap.pluginClass},${reportMap?.pluginVers?.minus('plugin:')}\n"
}
} //end check for element type

println "\nCONFIGURATION_BUILDER_END"

//Print Publishers
println "\nCONFIGURATION_PUBLISHER_START"
serialId = 0
project.publishers[0].children().each{
    elementConfiguration = [:] //generic map to maintain element configuration
    serialId++
    publisherTypePartsList=it.name().split("\\.")
    def publisherType=(publisherTypePartsList.size()>0)?publisherTypePartsList[publisherTypePartsList.size()-1]:publisherTypePartsList
    println "\nPUBLISHER----------------[${publisherType}]----------------"
    elementConfiguration.put('serialId', serialId)
    elementConfiguration.put('elementType', 'publisher')
    elementConfiguration.put('type', publisherType)
    visit(it, elementConfiguration)
}// end project builder
pubFile = new File("$reportWorkspace/publisherProps.csv")
pubFile << 'SERIAL_ID,TYPE,CLASS,PLUGIN\n'
configList.each {
//    println it
    if (it.elementType=='publisher'){
        reportMap = it.findAll { k, v -> k in ['serialId', 'type'] }
        // println reportMap
        plugin = it.find { k, v -> (v as String).contains('plugin') }
        if (plugin != null) {
            //println plugin
            pluginClass = plugin.getKey()
            //println pluginClass
            reportMap.put('pluginClass', pluginClass)
            pluginVers = plugin.getValue()
            reportMap.put('pluginVers', pluginVers)
        }
        pubFile << "${reportMap.serialId},${reportMap.type},${reportMap.pluginClass},${reportMap?.pluginVers?.minus('plugin:')}\n"
    }
} //end check for element type

println "\nCONFIGURATION_PUBLISHER_END"

//Print BuildWrappers
println "\nCONFIGURATION_BUILD_WRAPPERS_START"
serialId = 0
project.buildWrappers[0].children().each{
    elementConfiguration = [:] //generic map to maintain element configuration
    serialId++
    wrapperTypePartsList=it.name().split("\\.")
    def wrapperType=(wrapperTypePartsList.size()>0)?wrapperTypePartsList[wrapperTypePartsList.size()-1]:wrapperTypePartsList
    println "\nBUILD_WRAPPER----------------[${wrapperType}]----------------"
    elementConfiguration.put('serialId', serialId)
    elementConfiguration.put('elementType', 'wrapper')
    elementConfiguration.put('type', wrapperType)
    visit(it, elementConfiguration)
}// end project builder
wrapFile = new File("$reportWorkspace/wrapperProps.csv")
wrapFile << 'SERIAL_ID,TYPE,CLASS,PLUGIN\n'
configList.each {
//    println it
    if (it.elementType=='wrapper'){
        reportMap = it.findAll { k, v -> k in ['serialId', 'type'] }
        // println reportMap
        plugin = it.find { k, v -> (v as String).contains('plugin') }
        if (plugin != null) {
            //println plugin
            pluginClass = plugin.getKey()
            //println pluginClass
            reportMap.put('pluginClass', pluginClass)
            pluginVers = plugin.getValue()
            reportMap.put('pluginVers', pluginVers)
        }
        wrapFile << "${reportMap.serialId},${reportMap.type},${reportMap.pluginClass},${reportMap?.pluginVers?.minus('plugin:')}\n"
    }
} //end check for element type

println "\nCONFIGURATION_BUILD_WRAPPERS_END"


def visit(Node node, HashMap elementConfiguration) {
    if (node.children().size() >= 1) { //check that this is not a text node
        node.depthFirst().each {
            if (it instanceof Node) {
                if (it.depthFirst().size() <= 2) {
                    if (it.text().contains('=') || it.text().contains('<')) {
                        println "\t\t ${it.name()}"//usually scripts, commands and descriptions
                        if (it.name() in ['script','command']){
                            scriptFileName="${elementConfiguration.elementType}_${elementConfiguration.serialId}_${elementConfiguration.name}_${it.name()}.txt"
                          elementConfiguration.put('scriptCode',"${artifactPath}embeddedScripts/$scriptFileName/*view*/")
                            scriptFile = new File("$scriptWorkspace/${scriptFileName}")
                            scriptFile << it.text()
                        }


                        it.text().split('\\n').each {
                             println "\t\t\t $it"

                        }

                    } else {
                        println "\t ${it.name()}: ${it.text()}"
                        /* check for empty param values
                        it.name()!=''?it.name():"UNNAMED_$elementConfiguration.serialId
                         */
                        elementConfiguration.put(it.name() as String, it.text() != '' ? it.text() : "UNNAMED_$elementConfiguration.serialId" as String)

                    }

                } else {
                    println "\t ${it.name()}: ${it.attributes()}"
                    elementConfiguration.put(it.name() as String, it.attributes() as String)
                }
            } //end NOT instance of String

        }//end node.depth


    }//end node.children.size>1 (not a text node)
    else {
        println "\t ${node.name()}: ${node.text()}"
        elementConfiguration.put(it.name() as String, it.text() as String)
    }
    /*note global configList */
//    println elementConfiguration
    configList.add(elementConfiguration)

}


return 'SUCCESS'
