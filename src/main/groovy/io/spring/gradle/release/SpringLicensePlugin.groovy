package io.spring.gradle.release

import nl.javadude.gradle.plugins.license.License
import nl.javadude.gradle.plugins.license.LicenseExtension
import nl.javadude.gradle.plugins.license.LicensePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class SpringLicensePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
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
}
