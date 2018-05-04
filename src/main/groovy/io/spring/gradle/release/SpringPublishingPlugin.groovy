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
import io.spring.gradle.bintray.task.PublishTask
import nebula.core.ProjectType
import nebula.plugin.contacts.ContactsPlugin
import nebula.plugin.info.InfoPlugin
import nebula.plugin.publishing.maven.MavenPublishPlugin
import nebula.plugin.publishing.maven.license.MavenApacheLicensePlugin
import nebula.plugin.publishing.publications.JavadocJarPlugin
import nebula.plugin.publishing.publications.SourceJarPlugin
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.OpenOp
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.tasks.DefaultTaskDependency
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test

class SpringPublishingPlugin implements Plugin<Project> {
    private Project project

    @Override
    void apply(Project project) {
        this.project = project

        project.plugins.apply PublishingPlugin
        project.plugins.apply MavenPublishPlugin
        project.plugins.apply MavenApacheLicensePlugin
        project.plugins.apply JavadocJarPlugin
        project.plugins.apply SourceJarPlugin
        project.plugins.apply InfoPlugin
        project.plugins.apply ContactsPlugin

        project.tasks.create('downloadDependencies', DownloadDependenciesTask.class)

        project.tasks.withType(Javadoc) {
            failOnError = false
        }

        project.tasks.withType(Test) { Test testTask ->
            testTask.testLogging.exceptionFormat = 'full'
        }

        if(new ProjectType(project).isRootProject) {
            configureArtifactory()
        }

        project.plugins.apply SpringBintrayPlugin

        SpringBintrayExtension bintray = project.extensions.getByType(SpringBintrayExtension)

        String[] githubRemote = findGithubRemote()
        String githubProject, githubOrg
        if(githubRemote)
            (githubOrg, githubProject) = githubRemote

        bintray.with {
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

        def upload = project.tasks.create("artifactoryUpload", ArtifactoryUploadTask)

        project.afterEvaluate {
            upload.user = project.findProperty('springArtifactoryUser')
            upload.password = project.findProperty('springArtifactoryPassword')
            upload.repoUrl = 'https://repo.spring.io/libs-snapshot-local/'
            upload.publicationName = 'nebula'

            def publication = project.extensions.getByType(PublishingExtension).publications.findByName('nebula')
            publication.artifacts.forEach { artifact ->
                upload.dependsOn artifact
            }

            upload.dependsOn("generatePomFileFor${publication.name.capitalize()}Publication")

            project.rootProject.tasks.findByName('snapshot')?.dependsOn(upload)
        }

        // When you want to generate a candidate or final release, sync to JCenter
        project.tasks.withType(PublishTask) { Task task ->
            // the nebula release plugin should only be applied at the root
            project.rootProject.tasks.findByName('candidate')?.dependsOn(task)
            project.rootProject.tasks.findByName('final')?.dependsOn(task)
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