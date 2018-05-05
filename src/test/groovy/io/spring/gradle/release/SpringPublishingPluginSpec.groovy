package io.spring.gradle.release

import nebula.test.ProjectSpec
import org.ajoberstar.grgit.Grgit

class SpringPublishingPluginSpec extends ProjectSpec {
    Grgit repo

    def setup() {
        repo = Grgit.init(dir: projectDir)
        repo.remote.add(name: 'origin', url: 'git@github.com:spring-gradle-plugins/gradle-release-plugin.git')
        new File(projectDir, '.gitignore') << 'userHome/'

        repo.add(patterns: repo.status().unstaged.getAllChanges())
        repo.commit(message: 'initial commit')
    }

    def 'bintray configuration'() {
        when:
        project.plugins.apply(SpringReleasePlugin)
        project.plugins.apply(SpringPublishingPlugin)

        then:
        project.bintray.websiteUrl == 'https://github.com/spring-gradle-plugins/gradle-release-plugin'
    }

    def 'bintray configuration for project not in a spring org'() {
        setup:
        new File(projectDir, '.git').deleteDir()
        repo = Grgit.init(dir: projectDir)
        repo.remote.add(name: 'origin', url: 'git@github.com:micrometer-metrics/micrometer.git')

        repo.add(patterns: repo.status().unstaged.getAllChanges())
        repo.commit(message: 'initial commit')

        when:
        project.plugins.apply(SpringReleasePlugin)
        project.plugins.apply(SpringPublishingPlugin)

        then:
        project.bintray.websiteUrl == 'https://github.com/micrometer-metrics/micrometer'
    }
}
