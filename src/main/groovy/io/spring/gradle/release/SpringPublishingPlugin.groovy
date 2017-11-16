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

import io.spring.gradle.bintray.SpringBintrayExtension
import io.spring.gradle.bintray.SpringBintrayPlugin
import io.spring.gradle.bintray.task.UploadTask
import nebula.core.ProjectType
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.OpenOp
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin
import org.jfrog.gradle.plugin.artifactory.task.BuildInfoBaseTask

class SpringPublishingPlugin implements Plugin<Project> {
    private Project project

    @Override
    void apply(Project project) {
        this.project = project

        project.plugins.apply org.gradle.api.publish.plugins.PublishingPlugin

        project.plugins.apply ArtifactoryPlugin
        if(new ProjectType(project).isRootProject) {
            configureArtifactory()
        }

        project.plugins.apply SpringBintrayPlugin

        project.rootProject.subprojects.each { p ->
            def check = p.tasks.findByName('check')
            if(check) {
                project.tasks.findByName('bintrayCreatePackage')?.dependsOn(check)
            }
        }

        project.tasks.withType(UploadTask) { Task task ->
            project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
                task.onlyIf {
                    graph.hasTask(':final') || graph.hasTask(':candidate')
                }
            }
        }

        project.tasks.withType(BuildInfoBaseTask) { Task task ->
            project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
                task.onlyIf {
                    graph.hasTask(':snapshot') || graph.hasTask(':devSnapshot')
                }
            }
        }

        SpringBintrayExtension bintray = project.extensions.getByType(SpringBintrayExtension)

        String[] githubRemote = findGithubRemote()
        String githubProject, githubOrg
        if(githubRemote)
            (githubOrg, githubProject) = githubRemote

        bintray.with {
            bintrayUser = project.findProperty('bintrayUser')
            bintrayKey = project.findProperty('bintrayKey')

            repo = 'jars'
            org = 'spring'

            publication = 'nebula'

            websiteUrl = "https://github.com/$githubOrg/$githubProject"
            vcsUrl = "https://github.com/$githubOrg/${githubProject}.git"
            issueTrackerUrl = "https://github.com/$githubOrg/$githubProject/issues"

            ossrhUser = project.findProperty('sonatypeUsername')
            ossrhPassword = project.findProperty('sonatypePassword')

            gpgPassphrase = project.findProperty('gpgPassphrase')

            licenses = ['Apache-2.0']
        }
    }

    private String[] findGithubRemote() {
        try {
            Grgit git = new OpenOp(dir: project.rootProject.rootDir).call()

            // Remote URLs will be formatted like one of these:
            //  https://github.com/spring-gradle-plugins/spring-project-plugin.git
            //  git@github.com:spring-gradle-plugins/spring-release-plugin.git
            def repoParts = git.remote.list().collect { it.url =~ /github\.com[\/:]([^\/]+)\/(.+)\.git/ }
                    .find { it.count == 1 }

            if (repoParts == null) {
                // no remote configured yet, do nothing
                return null
            }

            return repoParts[0].drop(1)
        } catch (RepositoryNotFoundException ignored) {
            // do nothing
        }
    }

    private void configureArtifactory() {
        def artifactoryConvention = project.convention.plugins.artifactory

        artifactoryConvention.contextUrl = 'https://repo.spring.io'
        artifactoryConvention.publish {
            repository {
                repoKey = 'libs-snapshot-local' //The Artifactory repository key to publish to
                // For local build we expect them to be found in ~/.gradle/gradle.properties, otherwise to be set in CI

                // Conditionalize for the users who don't have credentials setup
                if (project.hasProperty('springArtifactoryUser')) {
                    username = project.property('springArtifactoryUser')
                    password = project.property('springArtifactoryPassword')
                }
            }
            defaults {
                publications 'nebula'
                publishIvy false
            }
        }
    }
}