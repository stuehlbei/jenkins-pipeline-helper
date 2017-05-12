#!/usr/bin/groovy
package ch.sbb.esta.cloud

// some helper methods for deploying to openshift
//  trying to share some utilities among "variables" (the scripts in /vars)
final class OpenshiftBuilderHelper {


    public void callJenkinsBuildProject(pomGroupId, pomArtifactId, pomVersion, targetProject, ocApp, ocAppVersion, port, tag) {
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

    public static Object mapLookup(Map map, String key, Object defaultValue) {
        return map.containsKey(key) ? map.get(key) : defaultValue
    }


    public static class DummyPom {
        public String groupId, artifactId, version
    }
}

