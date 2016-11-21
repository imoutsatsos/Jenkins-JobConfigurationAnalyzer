# Jenkins-JobConfigurationAnalyzer
A Jenkins utility job for analyzing and reporting the configuration of other freestyle jobs/projects

##Motivation
The configuration complexity of Jenkins freestyle jobs has increased, especially as we are now introducing dynamic parameter 
behavior (using the [Active Choices](https://wiki.jenkins-ci.org/display/JENKINS/Active+Choices+Plugin) and other similar Jenkins plugins).
There is a need for a quick and concise way to review and access project parameters, builders, publishers, and Groovy code and plugin dependencies.

##What can JENKINS_JOB_ANALYZER do?
This utility job allows you to select one of the jobs on your Jenkins server, analyze its configuration, and **create a concise report of the job's main elements** (properties, scm, builders, publishers, build-wrappers) as well various Groovy code and plugin dependencies.

By examining a JENKINS_JOB_ANALYZER build report you can immediately **visualize and access the target project's**:

1. parameters
2. SCM
3. builders
4. publishers
5. scriptlets, and Groovy code used
6. plugins used 
7. order and sequence of these components

In addition, longitudinal builds of a project can be used as an annotated log/archive of the job's configuration evolution. 
Since **the configuration file of the target project is archived**, it can be used to compare or even revert back to a particular version during development by simply re-deploying it to the project folder on the server.

## Build Form
An example build form is shown below. Note that if the selected project has been the target of a previous list it is listed in the PROJECT_HISTORY list.
The user can easily follow the links to review a previous configuration reports.
![Build Form](./userContent/assets/images/BuildForm.png?raw=true "Build Form")

## Console Report
The build console output is organized using the [Collapsing Console Sections Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Collapsing+Console+Sections+Plugin). 
The console report gives another (perhaps slightly more detailed) view of the project configuration. Each section can be collapsed and expanded for easier inspection of the project components.
![Console Report](./userContent/assets/images/ConsoleReport.png?raw=true "Console Report")

##Build report
The tabular build report uses the [Summary Display Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Summary+Display+Plugin) to display each of the project configuration components in a separate tab.
This is somewhat similar to the Jenkins v2.+ tabular UI used for job configuration, but in a much more compact form.
Groovy code (whether embedded or managed in the scriptler catalog) and used in parameters, builders or publishers can be easily inspected and accessed from report links.
The names and versions of applicable plugins used in parameters, builders and publishers, and build-wrappers are also reported.

### Example Job Configuration Reports
![Example Report](./userContent/assets/images/ExampleReport.png?raw=true "example Report")
And here is the parameter report from a project with several dynamic parameters
![example Complex Param Report](./userContent/assets/images/ExampleReportComplexParam.png?raw=true "example Complex Param Report")

##Build Artifacts
Each build generates a number of archived artifacts as shown in the example below
![Build Artifacts](./userContent/assets/images/BuildArtifacts.png?raw=true "Build Artifacts")

Archived artifacts include:

1. the `config.xml` file of the analyzed project
2. text files corresponding to each project component using an embedded script (Groovy, scriptlet, command)

Script code is identified by a *prefix indicating the component type* where the script originated (parameter, builder, publisher etc.) and it's *serial ID* (the sequence number by which the component is ordered in the configuration files)
##Limitations
It is possible that jobs using configuration components and build steps beyond the ones we have tested may not parse correctly.
We are using this utility primarily with freestyle projects that contain complex interacting parameters. 

Pipeline, multi-configuration and multi-job projects have not been tested.
If you would like to extend the parser, it should be possible to modify the code in [jobConfigParser](./scriptler/scripts/jobConfigParser.groovy) to detect new parameters types and build steps (and these will always be reported in the console).


