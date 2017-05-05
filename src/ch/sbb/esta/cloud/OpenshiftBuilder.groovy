// some helper methods for deploying to openshift

def buildDockerImageSelfRunningJar(Map params) {
    // will be much extended

    def targetProject = params.get("targetOsProject")

    def pom = readMavenPom file: 'pom.xml'
    build job: 'kd.cloud.openshift.build.springboot.vias', parameters: [[$class: 'StringParameterValue', name: 'POM_GROUP_ID', value: "$pom.groupId"],
                                                                        [$class: 'StringParameterValue', name: 'POM_ARTIFACT_ID', value: "$pom.artifactId"],
                                                                        [$class: 'StringParameterValue', name: 'POM_VERSION', value: "$pom.version"],
                                                                        [$class: 'StringParameterValue', name: 'OC_PROJECT', value: "$targetProject"],
                                                                        [$class: 'StringParameterValue', name: 'OC_APP', value: "plattform-chaos-monkey"],
                                                                        [$class: 'StringParameterValue', name: 'OC_APP_VERSION', value: "1.0.1"],
                                                                        [$class: 'StringParameterValue', name: 'APPLICATION_PORT', value: "8080"],
                                                                        [$class: 'StringParameterValue', name: 'TAG', value: "1.0.1"]
    ]

}