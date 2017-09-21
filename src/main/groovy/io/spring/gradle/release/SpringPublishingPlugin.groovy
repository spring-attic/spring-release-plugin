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

import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayUploadTask
import nebula.core.ProjectType
import nebula.plugin.bintray.NebulaBintrayPublishingPlugin
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.OpenOp
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.Upload
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin
import org.jfrog.gradle.plugin.artifactory.task.BuildInfoBaseTask

class SpringPublishingPlugin implements Plugin<Project> {
    private Project project

    @Override
    void apply(Project project) {
        this.project = project

        def publishingExtension = project.extensions.create('springPublishing', SpringPublishingExtension)

        def notDryRun = { it.enabled = !project.findProperty('dryRun') }
        def publishingEnabled = { it.enabled = publishingExtension.publishingEnabled }

        project.plugins.apply org.gradle.api.publish.plugins.PublishingPlugin
        project.plugins.apply NebulaBintrayPublishingPlugin
        project.plugins.apply ArtifactoryPlugin
        if(new ProjectType(project).isRootProject) {
            configureArtifactory()
        }

        project.tasks.withType(BintrayUploadTask, notDryRun)
        project.tasks.withType(BintrayUploadTask, publishingEnabled)
        project.tasks.withType(BintrayUploadTask) { Task task ->
            project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
                task.onlyIf {
                    graph.hasTask(':final') || graph.hasTask(':candidate')
                }
            }
        }

        project.tasks.withType(Upload, notDryRun)
        project.tasks.withType(Upload, publishingEnabled)

        project.tasks.withType(BuildInfoBaseTask, notDryRun)
        project.tasks.withType(BuildInfoBaseTask, publishingEnabled)
        project.tasks.withType(BuildInfoBaseTask) { Task task ->
            project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
                task.onlyIf {
                    graph.hasTask(':snapshot') || graph.hasTask(':devSnapshot')
                }
            }
        }

        BintrayExtension bintray = project.extensions.getByType(BintrayExtension)

        String[] githubRemote = findGithubRemote()
        String githubProject, githubOrg
        if(githubRemote)
            (githubOrg, githubProject) = githubRemote

        bintray.pkg.with {
            repo = 'jars'
            userOrg = 'spring'
            websiteUrl = "https://github.com/$githubOrg/$githubProject"
            vcsUrl = "https://github.com/$githubOrg/${githubProject}.git"
            issueTrackerUrl = "https://github.com/$githubOrg/$githubProject/issues"
            version.gpg.sign = false
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