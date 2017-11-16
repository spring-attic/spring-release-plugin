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

import nebula.core.ProjectType
import nebula.plugin.contacts.ContactsPlugin
import nebula.plugin.info.InfoPlugin
import nebula.plugin.publishing.maven.MavenPublishPlugin
import nebula.plugin.publishing.maven.license.MavenApacheLicensePlugin
import nebula.plugin.publishing.publications.JavadocJarPlugin
import nebula.plugin.publishing.publications.SourceJarPlugin
import nebula.plugin.release.NetflixOssStrategies
import nebula.plugin.release.ReleasePlugin
import nl.javadude.gradle.plugins.license.License
import nl.javadude.gradle.plugins.license.LicenseExtension
import nl.javadude.gradle.plugins.license.LicensePlugin
import org.ajoberstar.gradle.git.ghpages.GithubPagesPlugin
import org.ajoberstar.gradle.git.release.base.ReleasePluginExtension
import org.asciidoctor.gradle.AsciidoctorPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test

class SpringReleasePlugin implements Plugin<Project> {
    private Project project
    private ProjectType type
    private String githubOrg
    private String githubProject

    @Override
    void apply(Project project) {
        this.project = project
        this.type = new ProjectType(project)

        if (type.isLeafProject || type.isRootProject) {
            project.plugins.apply ReleasePlugin
            if (type.isRootProject) {
                ReleasePluginExtension releaseExtension = project.extensions.findByType(ReleasePluginExtension)
                releaseExtension?.with {
                    defaultVersionStrategy = NetflixOssStrategies.SNAPSHOT
                }
            }
            if (type.isLeafProject) {
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

                // License
                configureLicenseChecks()

                // Docs
                configureDocs()
            }


            project.plugins.apply SpringPublishingPlugin
        }

        if (type.isRootProject) {
            project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
                if (graph.hasTask(':devSnapshot')) {
                    throw new GradleException('You cannot use the devSnapshot task from the release plugin. Please use the snapshot task.')
                }
            }
        }

        // CircleCI
        project.tasks.create('initCircle', InitCircleTask)
    }

    private void configureLicenseChecks() {
        project.plugins.withType(JavaPlugin) {
            def licenseHeader = project.rootProject.file("gradle/licenseHeader.txt")

            def prepareLicenseHeaderTask = project.tasks.create('prepareLicenseHeader') {
                doLast {
                    if (!licenseHeader.exists()) {
                        licenseHeader.parentFile.mkdirs()
                        licenseHeader << getClass().getResourceAsStream("/licenseHeader.txt").text
                    }
                }
            }

            project.with {
                apply plugin: LicensePlugin

                extensions.findByType(LicenseExtension)?.with {
                    header = licenseHeader
                    mapping {
                        kt = 'JAVADOC_STYLE'
                    }
                    sourceSets = project.sourceSets
                    strictCheck = true
                }
            }

            project.tasks.withType(License) { it.dependsOn prepareLicenseHeaderTask }
        }
    }

    private void configureDocs() {
        File asciidocRoot = project.file('src/docs/asciidoc')
        if (asciidocRoot.exists() && project == project.rootProject) {
            project.with {
                apply plugin: AsciidoctorPlugin

                asciidoctorj {
                    version = '1.5.4'
                }

                asciidoctor {
                    attributes 'build-gradle': buildFile,
                            'source-highlighter':
                                    'coderay',
                            'imagesdir': 'images',
                            'toc': 'left',
                            'icons': 'font',
                            'setanchors': 'true',
                            'idprefix': '',
                            'idseparator': '-',
                            'docinfo1': 'true'
                }

                apply plugin: GithubPagesPlugin
                publishGhPages.dependsOn asciidoctor

                githubPages {
                    repoUri = "https://github.com/$githubOrg/${githubProject}.git"
                    credentials {
                        username = project.hasProperty('githubToken') ? project.githubToken : ''
                        password = ''
                    }

                    pages {
                        from file(asciidoctor.outputDir.path + '/html5')
                    }
                }
            }
        }
    }
}