import java.util.Map

/**
 * Erstellt ein Docker-Image mit eigenem Dockerfile (analog  kd.cloud.openshift.build.gitrepo[.baseimage].* von https://confluence.sbb.ch/display/ESTA/2.+Buildprozess )
 *  Obligatorische Parameter:
 *      targetOsProject      - Name des Openshift Projektes in welches zu deployen ist
 *
 *  Optionale Parameter:
 *    ocApp                  - Name des Openshift Images (Default: artifactId aus ./pom.xml )
 *    ocAppVersion           - Version des Openshift Images (ist auch als $VERSION im Buildpod zugänglich)
 *                               (Default: version aus ./pom.xml )
 *
 *    gitRepoUrl             - Wo liegt das Dockerfile? Url des git Repos  (Default: aktuelles Repo (muss ausgecheckt sein))
 *    gitBranch              -    In welchem Branch?  (Default: aktueller Branch)
 *    dockerDir              -    In welchem Docker Directory (Default: docker)
 *
 *    tag                    - Zusätzlich zu setzendes Tag (Default: ocAppVersion)
 *
 *    cluster                - Auf welchem OSE Cluster soll der Build laufen? (aws, vias, awsdev)  (Default: vias)
 *
 *    baseImageNamespace     - OpenShift Projekt, wo das Basisimage liegt  (Default: kein base image)
 *    baseImageNameAndTag    - Name und Tag vom Basisimage                 (Default: kein base image)
 *
 *    dryRun                 - Erlaubt es, zu schauen was für Defaults übernommen werden/ zum testen (Default: false)
 *
 * Beispiel-Aufruf:
 *     buildDockerImage(targetOsProject: "d", gitRepoUrl: "https://code.sbb.ch/scm/kd_cloud/plattform-cassandra.git", gitBranch: "master", dockerDir: "docker", ocApp: 'greatApp', ocAppVersion: '1', dryRun: true)
 */
def call(Map params) {
    error = ''

    REQUIRED_PARAMS = ['targetOsProject']

    for (String param : REQUIRED_PARAMS) {
        if (!params.containsKey(param)) {
            error += 'missing required param: ' + param + "\n"
        }
    }

    Set ALL_PARAMETERS = ['tag', 'dryRun', 'cluster', 'baseImageNamespace', 'baseImageNameAndTag', 'dockerDir', 'gitRepoUrl', 'gitBranch', 'ocApp', 'ocAppVersion'].plus(REQUIRED_PARAMS)

    for (Object key : params.keySet()) {
        if (!ALL_PARAMETERS.contains(key)) {
            error += 'unknown param: ' + key + "\n"
        }
    }

    boolean dryRun = mapLookup(params, "dryRun", false)
    
    def targetOsProject = params.get("targetOsProject")

    // inlining for lazy evaluation
    def gitRepoUrl = params.containsKey("gitRepoUrl") ? params.get("gitRepoUrl") : getGitUrl(dryRun)

    // inlining for lazy evaluation
    def gitBranch = params.containsKey("gitBranch") ? params.get("gitBranch") : getGitBranch(dryRun)

    def dockerDir = mapLookup(params, "dockerDir", "docker")

    def ocApp = params.get("ocApp")
    def ocAppVersion = params.get("ocAppVersion")
    
    if ((ocApp == null) || (ocAppVersion == null) ) {
        // try to get the param from pom.xml

        def pom = null
        if (!dryRun) {
            try {
                pom = readMavenPom file: 'pom.xml'
            } catch (Exception e){
                error += "ocApp or ocAppVersion unset and problem reading them from pom file " + e
                pom = new DummyPom3(groupId: "<groupId>",
                        artifactId: "<artifactId>",
                        version: "<version>"
                )
            }
        } else {
            pom = new DummyPom3(groupId: "<groupId>",
                    artifactId: "<artifactId>",
                    version: "<version>"
            )
        }

        if (ocApp == null) {
            ocApp = pom.artifactId
        }
        if (ocAppVersion == null){
            ocAppVersion = pom.version
        }
    }


    def baseImageNamespace = mapLookup(params, "baseImageNamespace", "")
    def baseImageNameAndTag = mapLookup(params, "baseImageNameAndTag", "")
    def tag = mapLookup(params, "tag", ocAppVersion)
    def cluster = mapLookup(params, "cluster", "vias")


    Set CLUSTER_VALUES = ['vias', 'aws', 'awsdev']
    if (!CLUSTER_VALUES.contains(cluster)) {
        error += 'unknown value for cluster: ' + cluster + " (allowed: " + CLUSTER_VALUES + ")\n"
    }

    if (((baseImageNamespace != "") && (baseImageNameAndTag == "")) ||
            ((baseImageNamespace == "") && (baseImageNameAndTag != ""))) {
        error += 'must set both baseImageNamespace and baseImageNameAndTag together or not at all'
    }

    println "targetOsProject:" + targetOsProject
    println "gitRepoUrl:" + gitRepoUrl
    println "gitBranch:" + gitBranch
    println "dockerDir:" + dockerDir
    println "ocApp:" + ocApp
    println "ocAppVersion:" + ocAppVersion

    println "baseImageNamespace:" + baseImageNamespace
    println "baseImageNameAndTag:" + baseImageNameAndTag
    println "tag:" + tag
    println "cluster:" + cluster
    println()

    if (!error.equals("")) {
        println("\nERROR:" + error + "\n")
    }

    if (!dryRun) {
        if (!error.equals("")) {
            failTheBuild(error)
        }

        if (baseImageNamespace == "") {
            callJenkinsBuildProject(targetOsProject, gitRepoUrl, gitBranch, dockerDir, ocApp, ocAppVersion, tag, cluster)
        } else {
            callJenkinsBuildProjectBaseimage(targetOsProject, gitRepoUrl, gitBranch, dockerDir, ocApp, ocAppVersion, tag, cluster, baseImageNamespace, baseImageNameAndTag)
        }
    }
}


private void callJenkinsBuildProject(targetOsProject, gitRepoUrl, gitBranch, dockerDir, ocApp, ocAppVersion, tag, cluster) {
    build job: "kd.cloud.openshift.build.gitrepo.$cluster", parameters: [[$class: 'StringParameterValue', name: 'GIT_REPO_PARAM', value: "$gitRepoUrl"],
                                                                         [$class: 'StringParameterValue', name: 'GIT_BRANCH_PARAM', value: "$gitBranch"],
                                                                         [$class: 'StringParameterValue', name: 'DOCKER_DIR', value: "$dockerDir"],
                                                                         [$class: 'StringParameterValue', name: 'OC_PROJECT', value: "$targetOsProject"],
                                                                         [$class: 'StringParameterValue', name: 'OC_APP', value: "$ocApp"],
                                                                         [$class: 'StringParameterValue', name: 'OC_APP_VERSION', value: "$ocAppVersion"],
                                                                         [$class: 'StringParameterValue', name: 'TAG', value: "$tag"]
    ]
}

private void callJenkinsBuildProjectBaseimage(targetOsProject, gitRepoUrl, gitBranch, dockerDir, ocApp, ocAppVersion, tag, cluster, baseImageNamespace, baseImageNameAndTag) {
    build job: "kd.cloud.openshift.build.gitrepo.baseimage.$cluster", parameters: [[$class: 'StringParameterValue', name: 'GIT_REPO_PARAM', value: "$gitRepoUrl"],
                                                                                   [$class: 'StringParameterValue', name: 'GIT_BRANCH_PARAM', value: "$gitBranch"],
                                                                                   [$class: 'StringParameterValue', name: 'DOCKER_DIR', value: "$dockerDir"],
                                                                                   [$class: 'StringParameterValue', name: 'OC_PROJECT', value: "$targetOsProject"],
                                                                                   [$class: 'StringParameterValue', name: 'OC_APP', value: "$ocApp"],
                                                                                   [$class: 'StringParameterValue', name: 'OC_APP_VERSION', value: "$ocAppVersion"],
                                                                                   [$class: 'StringParameterValue', name: 'TAG', value: "$tag"],
                                                                                   [$class: 'StringParameterValue', name: 'BASEIMAGE_NAMESPACE', value: "$baseImageNamespace"],
                                                                                   [$class: 'StringParameterValue', name: 'BASEIMAGE_NAME_TAG', value: "$baseImageNameAndTag"]
    ]
}

Object mapLookup(Map map, String key, Object defaultValue) {
    return map.containsKey(key) ? map.get(key) : defaultValue
}

String getGitUrl(boolean dryRun) {
    return dryRun ? "demoUrl" : sh(returnStdout: true, script: 'git config remote.origin.url').trim()
}

String getGitBranch(boolean dryRun) {
    return dryRun ? "demoBranch" : sh(returnStdout: true, script: 'echo $BRANCH_NAME').trim()
}

class DummyPom3 {
    public String groupId, artifactId, version
}

def failTheBuild(String message) {
    def messageColor = "\u001B[32m"
    def messageColorReset = "\u001B[0m"

    currentBuild.result = "FAILURE"
    echo messageColor + message + messageColorReset
    error(message)
}


// some demos

println "will fail:"

call(dryRun: true)
call(blabla: "", dryRun: true)
call(targetOsProject: "d", gitRepoUrl: "www.github.com/bla/bla", gitBranch: "master", dockerDir: "docker", ocApp: 'greatApp', ocAppVersion: '1', baseImageNamespace: 'bla', dryRun: true)

println "\n\n\nshould work:"

call(targetOsProject: "e", ocApp: 'greatApp', dryRun: true)
call(targetOsProject: "f", gitRepoUrl: "www.github.com/bla/bla", gitBranch: "master", ocAppVersion: '1', dryRun: true)
call(targetOsProject: "g", gitRepoUrl: "www.github.com/bla/bla", gitBranch: "master", dockerDir: "docker2", ocApp: 'greatApp', ocAppVersion: '1',
        baseImageNamespace: 'bla', baseImageNameAndTag:'...', dryRun: true)

call(targetOsProject: "h", ocApp: 'greatApp', ocAppVersion: '1', dryRun: true)


// try out escaping
x = 'git for-each-ref --format=\'%(objectname) %(refname:short)\' refs/heads | awk "/^$(git rev-parse HEAD)/ {print \\$2}"'

print "string is: $x"