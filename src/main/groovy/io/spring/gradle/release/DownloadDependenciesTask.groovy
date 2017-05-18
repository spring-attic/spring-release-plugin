package io.spring.gradle.release

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class DownloadDependenciesTask extends DefaultTask {
    DownloadDependenciesTask() {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    protected void downloadDependencies() {
        project.configurations.findAll { it.canBeResolved }*.files
    }
}
