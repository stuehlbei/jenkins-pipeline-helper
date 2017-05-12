import java.util.Map

/**
 * Erstellt ein Docker-Image für ein self-running jar (analog  kd.cloud.openshift.build.springboot.* von https://confluence.sbb.ch/display/ESTA/2.+Buildprozess )
 *  Obligatorische Parameter:
 *      targetOsProject      - Name des Openshift Projektes in welches zu deployen ist
 *
 *  Optionale Parameter:
 *    pomGroupId, pomArtifactId, pomVersion - definieren, von welchem Maven Projekt das jar-file genommen werden soll
 *                                                 (Default: das jar aus dem pom-File unter /pom.xml)
 *
 *    ocApp                    - Name des Images (Default: <pomArtifactId>)
 *    ocAppVersion             - Version des Images (Default: <pomVersion>)
 *
 *    tag                     - Zusätzlich zu setzendes Tag (Default: <pomVersion>
 *    port                    - Alternativer Port (Default: 8080)
 *
 *    dryRun                  - Erlaubt es, zu schauen was für Defaults übernommen werden/ zum testen (Default: false)
 *
 *
 * Beispiel-Aufruf:
 *     buildDockerImageSelfRunningJar(targetOsProject:"d", port:"2222", dryRun:true, pomArtifactId:"bla")</code>
 */
def call(Map params) {

    error = ''

    REQUIRED_PARAMS = ['targetOsProject']

    for (String param: REQUIRED_PARAMS) {
        if (!params.containsKey(param)) {
            error += 'missing required param: ' + param + "\n"
        }
    }

    Set ALL_PARAMETERS = ['targetOsProject','pomGroupId', 'pomArtifactId', 'pomVersion', 'ocApp', 'ocAppVersion', 'port', 'tag', 'dryRun', 'cluster']

    for (Object key : params.keySet()){
        if (!ALL_PARAMETERS.contains(key)) {
            error += 'unknown param: ' + key + "\n"
        }
    }

    if (!error.equals("")) {
        println("\nERROR:" + error + "\n")
        return
    }

    boolean dryRun = mapLookup(params, "dryRun", false)

    def pom = null
    if (!dryRun) {
        pom = readMavenPom file: 'pom.xml'
    } else {
        println "param map:" + params + "\n"
        pom = new DummyPom2(groupId: "<groupId>",
                artifactId: "<artifactId>",
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

    if (!dryRun) {
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

static Object mapLookup(Map map, String key, Object defaultValue){
    return map.containsKey(key) ? map.get(key): defaultValue
}

class DummyPom2 {
    public String groupId, artifactId, version
}

// some demos

buildDockerImageSelfRunningJar(targetOsProject:"d", dryRun:true)
buildDockerImageSelfRunningJar(dryRun:true)
buildDockerImageSelfRunningJar(dryRun:true, targetOsProject:"d", notExisting:"")
buildDockerImageSelfRunningJar(targetOsProject:"d", port:"2222", dryRun:true, pomArtifactId:"bla")