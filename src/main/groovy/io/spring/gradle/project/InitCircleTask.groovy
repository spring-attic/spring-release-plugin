package io.spring.gradle.project

import org.gradle.api.DefaultTask
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

        // TODO generate gradle.properties.enc
    }
}
