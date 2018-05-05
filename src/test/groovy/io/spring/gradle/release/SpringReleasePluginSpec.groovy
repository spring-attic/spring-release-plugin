/*
 * Copyright 2017 Pivotal Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.spring.gradle.release

import nebula.test.ProjectSpec
import org.ajoberstar.grgit.Grgit

class SpringReleasePluginSpec extends ProjectSpec {
    Grgit repo

    def setup() {
        repo = Grgit.init(dir: projectDir)
        repo.remote.add(name: 'origin', url: 'git@github.com:spring-gradle-plugins/gradle-release-plugin.git')
        new File(projectDir, '.gitignore') << 'userHome/'

        repo.add(patterns: repo.status().unstaged.getAllChanges())
        repo.commit(message: 'initial commit')
    }

    def 'dev snapshot task accounts for prior releases with .RELEASE suffix'() {
        setup:
        repo.tag.add(name: 'v0.2.0', force: true)

        when:
        project.gradle.startParameter.taskNames = ['devSnapshot']
        project.plugins.apply(SpringReleasePlugin)

        then:
        project.version.toString().startsWith('0.3.0-dev.0+')
    }

    def 'snapshot task accounts for prior releases with .RELEASE suffix'() {
        setup:
        repo.tag.add(name: 'v0.2.0', force: true)

        when:
        project.gradle.startParameter.taskNames = ['snapshot']
        project.plugins.apply(SpringReleasePlugin)

        then:
        project.version.toString().startsWith('0.3.0-SNAPSHOT')
    }

    def 'snapshot versioning applies when publishing to maven local'() {
        setup:
        repo.tag.add(name: 'v0.2.0', force: true)

        when:
        project.gradle.startParameter.taskNames = ['pTML']
        project.plugins.apply(SpringReleasePlugin)

        then:
        project.version.toString().startsWith('0.3.0-SNAPSHOT')
    }

    def 'candidate task generates releases .rc suffix'() {
        setup:
        repo.tag.add(name: 'v1.0.0', force: true)

        when:
        project.gradle.startParameter.taskNames = ['candidate']
        project.plugins.apply(SpringReleasePlugin)

        then:
        project.version.toString() == '1.1.0-rc.1'
    }

    def '`useLastTag` generates releases off of last tag'() {
        setup:
        repo.tag.add(name: 'v0.2.0', force: true)

        when:
        project.ext.set('release.useLastTag', 'true')
        project.gradle.startParameter.taskNames = ['final']
        project.plugins.apply(SpringReleasePlugin)

        then:
        project.version.toString() == '0.2.0'
    }

    def 'release.version with custom `release.version`'() {
        when:
        project.ext.set('release.version', '0.2.0')
        project.gradle.startParameter.taskNames = ['final']
        project.plugins.apply(SpringReleasePlugin)

        then:
        project.version.toString() == '0.2.0'
    }

    def 'final task with release scope'() {
        setup:
        repo.tag.add(name: 'v0.2.0')

        when:
        project.ext.set('release.scope', 'patch')
        project.gradle.startParameter.taskNames = ['final']
        project.plugins.apply(SpringReleasePlugin)

        then:
        project.version.toString() == '0.2.1'
    }

    def 'snapshot after a candidate'() {
        setup:
        repo.tag.add(name: 'v1.0.0-rc.1')

        when:
        project.gradle.startParameter.taskNames = ['devSnapshot']
        project.plugins.apply(SpringReleasePlugin)

        then:
        project.version.toString().startsWith('1.0.0-rc.1.dev.0+')
    }

    def 'candidate after a candidate'() {
        setup:
        repo.tag.add(name: 'v1.0.0-rc.1')

        when:
        project.gradle.startParameter.taskNames = ['candidate']
        project.plugins.apply(SpringReleasePlugin)

        then:
        project.version.toString().startsWith('1.0.0-rc.2')
    }

    def 'final after a candidate'() {
        setup:
        repo.tag.add(name: 'v1.0.0-rc.1')

        when:
        project.gradle.startParameter.taskNames = ['final']
        project.plugins.apply(SpringReleasePlugin)

        then:
        project.version.toString().startsWith('1.0.0')
    }

    def 'useLastTag does not favor old releases with .RELEASE'() {
        setup:
        repo.tag.add(name: 'v0.1.0.RELEASE')
        repo.tag.add(name: 'v1.0.0-rc.1')
        repo.tag.add(name: 'v1.0.0')

        when:
        project.ext.set('release.useLastTag', 'true')
        project.gradle.startParameter.taskNames = ['final']
        project.plugins.apply(SpringReleasePlugin)

        then:
        project.version.toString() == '1.0.0'
    }
}
