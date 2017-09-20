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

import com.jfrog.bintray.gradle.BintrayUploadTask
import nebula.core.ProjectType
import nebula.plugin.bintray.BintrayPlugin
import nebula.plugin.bintray.NebulaOJOPublishingPlugin
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
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.OpenOp
import org.asciidoctor.gradle.AsciidoctorPlugin
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.jfrog.gradle.plugin.artifactory.task.BuildInfoBaseTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SpringReleasePlugin implements Plugin<Project> {
    private static final Logger logger = LoggerFactory.getLogger(SpringReleasePlugin)
    private Project project
    private ProjectType type
    private String githubOrg
    private String githubProject

    @Override
    void apply(Project project) {
        this.project = project
        findGithubRemote()
        this.type = new ProjectType(project)


        if(project == project.rootProject) {
            ReleasePluginExtension releaseExtension = project.extensions.findByType(ReleasePluginExtension)
            releaseExtension?.with {
                defaultVersionStrategy = NetflixOssStrategies.SNAPSHOT
            }

            project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
                if (graph.hasTask(':devSnapshot')) {
                    // otherwise, we may accidentally get a semver-style snapshot into JCenter and Maven Central!
                    throw new GradleException('You cannot use the devSnapshot task from the release plugin. Please use the snapshot task.')
                }
            }
        }

        if (project.subprojects.isEmpty() || project != project.rootProject) {
            println("Project $project.name is being configured as a Java project")

            project.with {
                // Java
                apply plugin: 'java'

                repositories {
                    jcenter()
                }

                // Publishing
                apply plugin: MavenPublishPlugin
                apply plugin: MavenApacheLicensePlugin
                apply plugin: JavadocJarPlugin
                apply plugin: SourceJarPlugin

                // Info
                apply plugin: InfoPlugin

                // Contacts
                apply plugin: ContactsPlugin
            }

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

        // Release
        configureRelease()

        // CircleCI
        project.tasks.create('initCircle', InitCircleTask)
    }

    private void findGithubRemote() {
        try {
            Grgit git = new OpenOp(dir: project.rootProject.rootDir).call()

            // Remote URLs will be formatted like one of these:
            //  https://github.com/spring-gradle-plugins/spring-project-plugin.git
            //  git@github.com:spring-gradle-plugins/spring-release-plugin.git
            def repoParts = git.remote.list().collect { it.url =~ /github\.com[\/:]([^\/]+)\/(.+)\.git/ }
                    .find { it.count == 1 }

            if (repoParts == null) {
                // no remote configured yet, do nothing
                return
            }

            (githubOrg, githubProject) = repoParts[0].drop(1)
        } catch (RepositoryNotFoundException ignored) {
            // do nothing
        }
    }

    private void configureRelease() {
        if (githubOrg == null) {
            logger.warn('No git remote configured, not enabling release related tasks')
            return
        }

        project.with {
            apply plugin: ReleasePlugin
            apply plugin: BintrayPlugin

            bintray.pkg {
                repo = 'jars'
                userOrg = 'spring'
                websiteUrl = "https://github.com/$githubOrg/$githubProject"
                vcsUrl = "https://github.com/$githubOrg/${githubProject}.git"
                issueTrackerUrl = "https://github.com/$githubOrg/$githubProject/issues"

                version {
                    gpg {
                        sign = false
                    }
                }
            }

            BintrayUploadTask bintrayUpload = (BintrayUploadTask) project.tasks.find {
                it instanceof BintrayUploadTask
            }

            bintrayUpload.doFirst {
                bintrayUpload.packageName = githubProject
                bintrayUpload.packageWebsiteUrl = "https://github.com/$githubOrg/$githubProject"
                bintrayUpload.packageIssueTrackerUrl = "https://github.com/$githubOrg/$githubProject/issues"
                bintrayUpload.packageVcsUrl = "https://github.com/$githubOrg/${githubProject}.git"
            }

            boolean dryRun = project.hasProperty('dryRun') && project.property('dryRun') as Boolean
            def disable = {
                it.enabled = !dryRun
            }

            project.tasks.withType(BintrayUploadTask, disable)
            project.tasks.withType(BintrayUploadTask) { Task task ->
                project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
                    task.onlyIf {
                        graph.hasTask(':final') || graph.hasTask(':candidate')
                    }
                }
            }

            project.tasks.withType(Upload, disable)
            project.tasks.withType(BuildInfoBaseTask, disable)
            project.tasks.withType(BuildInfoBaseTask) { Task task ->
                project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
                    task.onlyIf {
                        graph.hasTask(':snapshot') || graph.hasTask(':devSnapshot')
                    }
                }
            }

            if (project.subprojects.isEmpty() || project != project.rootProject) {
                println("Project $project.name is being configured to be published")



            }
        }

        if (project.rootProject != project) {
            project.plugins.withType(JavaPlugin) {
                project.rootProject.tasks.release.dependsOn project.tasks.build
            }
        }
    }

    static GIT_PATTERN = /((git|ssh|https?):(\/\/))?(\w+@)?([\w\.]+)([\:\\/])([\w\.@\:\/\-~]+)(\/)?/

    /**
     * Convert git syntax of git@github.com:reactivex/rxjava-core.git to https://github.com/reactivex/rxjava-core
     * @param origin
     */
    static String calculateUrlFromOrigin(String origin) {
        def m = origin =~ GIT_PATTERN
        return "https://${m[0][5]}/" + (m[0][7] - '.git')
    }

    static String calculateRepoFromOrigin(String origin) {
        def m = origin =~ GIT_PATTERN
        String path = m[0][7] - '.git'
        path.tokenize('/').last()
    }

    private void configureLicenseChecks() {
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