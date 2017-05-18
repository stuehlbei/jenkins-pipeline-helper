import java.util.Map

/**
 * Erstellt ein Docker-Image mit eigenem Dockerfile (analog  kd.cloud.openshift.build.gitrepo[.baseimage].* von https://confluence.sbb.ch/display/ESTA/2.+Buildprozess )
 *  Obligatorische Parameter:
 *      targetOsProject      - Name des Openshift Projektes in welches zu deployen ist
 *
 *      ocApp                - Name des Openshift Images
 *      ocAppVersion         - Version des Openshift Images (ist auch als $VERSION im Buildpod zugänglich)
 *
 *  Optionale Parameter:
 *    gitRepoUrl             - Wo liegt das Dockerfile? Url des git Repos  (Default: aktuelles Repo (muss ausgecheckt sein))
 *    gitBranch              -    In welchem Branch?  (Default: aktueller Branch)
 *    dockerDir              -    In welchem Docker Directory (Default: docker)
 *
 *    tag                    - Zusätzlich zu setzendes Tag (Default: ocAppVersion)
 *
 *    cluster                - Auf welchem OSE Cluster soll der Build laufen? (aws, vias, awsdev)  (Default: vias)
 *
 *    baseImageNamespace     - OpenShift Projekt, wo das Basisimage liegt
 *    baseImageNameAndTag    - Name und Tag vom Basisimage
 *
 *    dryRun                 - Erlaubt es, zu schauen was für Defaults übernommen werden/ zum testen (Default: false)
 *
 * Beispiel-Aufruf:
 *     buildDockerImage(targetOsProject: "d", gitRepoUrl: "https://code.sbb.ch/scm/kd_cloud/plattform-cassandra.git", gitBranch: "master", dockerDir: "docker", ocApp: 'greatApp', ocAppVersion: '1', dryRun: true)
 */
def call(Map params) {
    error = ''

    boolean dryRun = mapLookup(params, "dryRun", false)

    // temporarily here 
    println "url: " + getGitUrl(dryRun)
    println "branch: " + getGitBranch(dryRun)


    REQUIRED_PARAMS = ['targetOsProject', 'ocApp', 'ocAppVersion']

    for (String param : REQUIRED_PARAMS) {
        if (!params.containsKey(param)) {
            error += 'missing required param: ' + param + "\n"
        }
    }

    Set ALL_PARAMETERS = ['tag', 'dryRun', 'cluster', 'baseImageNamespace', 'baseImageNameAndTag', 'dockerDir', 'gitRepoUrl', 'gitBranch'].plus(REQUIRED_PARAMS)

    for (Object key : params.keySet()) {
        if (!ALL_PARAMETERS.contains(key)) {
            error += 'unknown param: ' + key + "\n"
        }
    }



    def targetOsProject = params.get("targetOsProject")
    def gitRepoUrl = mapLookup(params, "gitRepoUrl", getGitUrl(dryRun))
    def gitBranch = mapLookup(params, "gitBranch", getGitBranch(dryRun))
    def dockerDir = mapLookup(params, "dockerDir", "docker")
    def ocApp = params.get("ocApp")
    def ocAppVersion = params.get("ocAppVersion")

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
        return
    }

    if (!dryRun) {
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
    return dryRun ? "demoBranch" : sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
}



// some demos

// will fail
call(targetOsProject: "d", dryRun: true)
call(dryRun: true)
call(blabla: "", dryRun: true)
call(targetOsProject: "d", gitRepoUrl: "www.github.com/bla/bla", gitBranch: "master", dockerDir: "docker", ocApp: 'greatApp', ocAppVersion: '1', baseImageNamespace: 'bla', dryRun: true)


call(targetOsProject: "d", gitRepoUrl: "www.github.com/bla/bla", gitBranch: "master", ocApp: 'greatApp', ocAppVersion: '1', dryRun: true)
call(targetOsProject: "d", gitRepoUrl: "www.github.com/bla/bla", gitBranch: "master", dockerDir: "docker2", ocApp: 'greatApp', ocAppVersion: '1',
        baseImageNamespace: 'bla', baseImageNameAndTag:'...', dryRun: true)

call(targetOsProject: "d", ocApp: 'greatApp', ocAppVersion: '1', dryRun: true)

