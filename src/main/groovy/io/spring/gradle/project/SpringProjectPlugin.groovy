package io.spring.gradle.project

import com.jfrog.bintray.gradle.BintrayExtension
import nebula.plugin.bintray.NebulaBintrayPublishingPlugin
import nebula.plugin.contacts.ContactsPlugin
import nebula.plugin.info.InfoPlugin
import nebula.plugin.publishing.maven.MavenPublishPlugin
import nebula.plugin.publishing.publications.JavadocJarPlugin
import nebula.plugin.publishing.publications.SourceJarPlugin
import nebula.plugin.release.ReleaseExtension
import nebula.plugin.release.ReleasePlugin
import nl.javadude.gradle.plugins.license.LicenseExtension
import nl.javadude.gradle.plugins.license.LicensePlugin
import org.ajoberstar.gradle.git.release.base.ReleasePluginExtension
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test

class SpringProjectPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        // Java
        project.plugins.apply(JavaPlugin)

        project.repositories {
            jcenter()
        }

        project.tasks.withType(Javadoc) {
            failOnError = false
        }
        project.tasks.withType(Test) { Test testTask ->
            testTask.testLogging.exceptionFormat = 'full'
        }

        // Publishing
        project.plugins.apply(MavenPublishPlugin)
        project.plugins.apply(JavadocJarPlugin)
        project.plugins.apply(SourceJarPlugin)

        // Info
        project.plugins.apply(InfoPlugin)

        // Contacts
        project.plugins.apply(ContactsPlugin)

        // License
        project.plugins.apply(LicensePlugin)

        def licenseHeader = project.file("gradle/licenseHeader.txt")
        if(!licenseHeader.exists()) {
            licenseHeader.writeText(getClass().getResourceAsStream("licenseHeader.txt").text)
        }

        project.extensions.findByType(LicenseExtension).with {
            header = licenseHeader
            mapping {
                kt = 'JAVADOC_STYLE'
            }
            sourceSets = project.sourceSets
            strictCheck = true
        }

        // Release
        project.plugins.apply(ReleasePlugin)

        project.extensions.findByType(ReleaseExtension).with {
            addReleaseBranchPattern(/v?\d+\.\d+\.\d+\.RELEASE/)
        }

        if(project.isRootProject()) {
            // override nebula's default with a strategy that will add .RELEASE on the end of releases
            project.extensions.findByType(ReleasePluginExtension).versionStrategy(new SpringReleaseLastTagStrategy())
        }

        project.plugins.apply(NebulaBintrayPublishingPlugin)

        Grgit git = Grgit.open()
        git.remote.list()

        project.extensions.findByType(BintrayExtension).with {
            pkg {
                repo = 'jars'
                userOrg = 'spring'
                websiteUrl = 'https://github.com/spring-gradle-plugins/spring-io.spring.gradle.project-plugin'
                vcsUrl = 'https://github.com/spring-gradle-plugins/spring-io.spring.gradle.project-plugin.git'
                issueTrackerUrl = 'https://github.com/spring-gradle-plugins/spring-io.spring.gradle.project-plugin/issues'
                labels = ['gradle', 'spring']
            }
        }
    }
}