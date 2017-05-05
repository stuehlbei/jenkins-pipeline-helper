#!/usr/bin/groovy
package ch.sbb.esta.cloud;

import java.util.Map;

// some helper methods for deploying to openshift

REQUIRED_PARAMS = ['targetOsProject']

// ALL_PARAMETERS = ['targetOsProject','pomGroupId', 'pomArtifactId', 'pomVersion', 'ocApp', 'ocAppVersion', 'port', 'tag', 'dryRun']
def buildDockerImageSelfRunningJar(Map params) {

    error = ''

    REQUIRED_PARAMS.each({ it ->
        if (!params.containsKey(it)) {
            error += 'missing required param: ' + it + "\n"
        }
    })

    Set ALL_PARAMETERS = ['targetOsProject','pomGroupId', 'pomArtifactId', 'pomVersion', 'ocApp', 'ocAppVersion', 'port', 'tag', 'dryRun']

    params.keySet().each({ it ->
       if (!ALL_PARAMETERS.contains(it)) {
           error += 'unknown param: ' + it + "\n"
       }
    });

    if (!error.equals("")) {
        println("\nERROR:"+error+"\n");
        return
    }

    boolean dryRun = mapLookup(params, "dryRun", false)

    def pom = null;
    if (! dryRun) {
        pom = readMavenPom file: 'pom.xml'
    } else {
        println "param map:" + params + "\n"
        pom = new DummyPom(groupId: "<groupId>",
                artifactId:"<artifactId>",
                version: "<version>"
        )
    }


    def targetProject = params.get("targetOsProject")
    def pomGroupId = mapLookup(params, "pomGroupId", pom.groupId)
    def pomArtifactId = mapLookup(params, "pomArtifactId", pom.artifactId)
    def pomVersion = mapLookup(params, "pomVersion", pom.version)

    def ocApp = mapLookup(params, "ocApp", pomArtifactId)
    def ocAppVersion = mapLookup(params, "ocAppVersion", pomVersion)
    def port = mapLookup(params, "port", "8080")
    def tag = mapLookup(params, "tag", pomVersion)

    println "pomGroupId: " + pomGroupId
    println "pomArtifactId:" + pomArtifactId
    println "pomVersion:" + pomVersion
    println "targetProject:" + targetProject
    println "ocApp:" + ocApp
    println "ocAppVersion:" + ocAppVersion
    println "port:" + port
    println "tag:" + tag

    if (! dryRun) {
        callJenkinsBuildProject(pomGroupId, pomArtifactId, pomVersion, targetProject, ocApp, ocAppVersion, port, tag)
    }
}

private void callJenkinsBuildProject(pomGroupId, pomArtifactId, pomVersion, targetProject, ocApp, ocAppVersion, port, tag) {
    build job: 'kd.cloud.openshift.build.springboot.vias', parameters: [[$class: 'StringParameterValue', name: 'POM_GROUP_ID', value: "$pomGroupId"],
                                                                        [$class: 'StringParameterValue', name: 'POM_ARTIFACT_ID', value: "$pomArtifactId"],
                                                                        [$class: 'StringParameterValue', name: 'POM_VERSION', value: "$pomVersion"],
                                                                        [$class: 'StringParameterValue', name: 'OC_PROJECT', value: "$targetProject"],
                                                                        [$class: 'StringParameterValue', name: 'OC_APP', value: "$ocApp"],
                                                                        [$class: 'StringParameterValue', name: 'OC_APP_VERSION', value: "$ocAppVersion"],
                                                                        [$class: 'StringParameterValue', name: 'APPLICATION_PORT', value: "$port"],
                                                                        [$class: 'StringParameterValue', name: 'TAG', value: "$tag"]
    ]
}

def Object mapLookup(Map map, String key, Object defaultValue){
    return map.containsKey(key) ? map.get(key): defaultValue
}

class DummyPom {
    public String groupId, artifactId, version
}


buildDockerImageSelfRunningJar(targetOsProject:"d", dryRun:true)
buildDockerImageSelfRunningJar(dryRun:true)
buildDockerImageSelfRunningJar(dryRun:true, targetOsProject:"d", notExisting:"")
buildDockerImageSelfRunningJar(targetOsProject:"d", port:"2222", dryRun:true, pomArtifactId:"bla")