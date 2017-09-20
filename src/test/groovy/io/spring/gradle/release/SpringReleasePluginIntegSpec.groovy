package io.spring.gradle.release

import nebula.test.IntegrationSpec
import org.ajoberstar.grgit.Grgit

class SpringReleasePluginIntegSpec extends IntegrationSpec {
    Grgit repo

    def setup() {
        repo = Grgit.init(dir: projectDir)
        repo.remote.add(name: 'origin', url: 'git@github.com:spring-gradle-plugins/spring-release-plugin.git')
        new File(projectDir, '.gitignore') << 'userHome/'

        repo.add(patterns: repo.status().unstaged.getAllChanges())
        repo.commit(message: 'initial commit')
    }

    def 'snapshots only attempt to upload to OJO'() {
        when:
        buildFile << """
            buildscript {
                repositories {
                    maven { url 'https://plugins.gradle.org/m2/' }
                }
                dependencies {
                    classpath "gradle.plugin.com.dorongold.plugins:task-tree:1.3"
                }
            }

            apply plugin: 'java'
            apply plugin: ${SpringReleasePlugin.name}
            apply plugin: com.dorongold.gradle.tasktree.TaskTreePlugin
        """

        def out = runTasksSuccessfully('snapshot').standardOutput
        println(out)

        then:
        out.contains('artifactoryPublish')
    }
}
