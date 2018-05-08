package io.spring.gradle.release

import nebula.test.ProjectSpec
import org.ajoberstar.grgit.Grgit

class ArtifactoryUploadTaskSpec extends ProjectSpec {
//    @Rule
//    TemporaryFolder temp

//    def 'upload artifact'() {
//        when:
//        def f = temp.newFile()
//        f << 'test'
//
//        then:
//        new UploadWorker(repoUrl: 'http://localhost:8081/artifactory/libs-snapshot-local', user: 'admin',
//                password: 'admin', path: 'com/test/f/1.0.0-SNAPSHOT/f-1.0.0-SNAPSHOT.jar', artifact: f).run()
//    }

    Grgit repo

    def setup() {
        repo = Grgit.init(dir: projectDir)
//        repo.remote.add(name: 'origin', url: 'git@github.com:spring-gradle-plugins/gradle-release-plugin.git')
//        new File(projectDir, '.gitignore') << 'userHome/'
//
//        repo.add(patterns: repo.status().unstaged.getAllChanges())
//        repo.commit(message: 'initial commit')
    }

    def 'artifact upload order'() {
        when:
        project.with {
            apply plugin: 'java'
            apply plugin: SpringReleasePlugin
            apply plugin: SpringPublishingPlugin

            group = 'io.spring.gradle'
        }

        def uploadTask = project.tasks.findByName('artifactoryUpload') as ArtifactoryUploadTask
        uploadTask.publicationName = 'nebula'

        def artifacts = uploadTask.artifactsToUpload()*.file*.name

        then:
        artifacts.size() == 3
        artifacts[0].endsWith('uncommitted.jar')
        artifacts[1].endsWith('javadoc.jar')
        artifacts[2].endsWith('sources.jar')
    }
}
