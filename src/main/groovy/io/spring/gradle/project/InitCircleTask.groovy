package io.spring.gradle.project

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class InitCircleTask extends DefaultTask {
    @TaskAction
    void initCircle() {
        File ciBuild = project.file('gradle/ciBuild.sh')

        if (!ciBuild.exists()) {
            ciBuild.parentFile.mkdirs()
            ciBuild << getClass().getResourceAsStream("/ciBuild.sh").text
            ciBuild.setExecutable(true)
        }

        File circleYml = project.file('circle.yml')

        if(!circleYml.exists()) {
            circleYml << getClass().getResourceAsStream('/circle.yml').text
        }

        File gradleProperties = project.file('gradle.properties')
        if(gradleProperties.exists()) {
            throw new GradleException('Cannot encrypt keys into gradle.properties because gradle.properties is already present.')
        }

        def sonatype = 'lpass show oss.sonatype.org'.execute()
        if(sonatype.waitFor() != 0) {
            throw new GradleException('Unable to generate gradle.properties.enc with keys. Please install the laspass CLI and run' +
                    ' `lpass login` with your Pivotal email (https://github.com/lastpass/lastpass-cli).')
        }

        def (sonatypeToken, sonatypePassword) = sonatype.text.readLines().findAll { it.startsWith('User token') }.collect { it.substring(it.indexOf(': ')) }

        gradleProperties << "sonatypeUsername=$sonatypeToken\n"
        gradleProperties << "sonatypePassword=$sonatypePassword\n"

        def bintray = 'lpass show bintray.com'.execute()
        if(bintray.waitFor() != 0) {
            throw new GradleException('Unable to generate gradle.properties.enc with keys. Please install the laspass CLI and run' +
                    ' `lpass login` with your Pivotal email (https://github.com/lastpass/lastpass-cli).')
        }

        def bintrayText = bintray.text
        def bintrayUser = bintrayText.readLines().find { it.startsWith('Username: ') }.substring('Username: '.length())
        def bintrayKey = bintrayText.readLines().find { it.startsWith('Notes: ') }
        bintrayKey = bintrayKey.substring(bintrayKey.indexOf('API key '))

        gradleProperties << "bintrayUser=$bintrayUser\n"
        gradleProperties << "bintrayKey=$bintrayKey\n"
    }
}
