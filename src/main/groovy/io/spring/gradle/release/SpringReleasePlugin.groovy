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
import nebula.plugin.release.NetflixOssStrategies
import nebula.plugin.release.ReleasePlugin
import org.ajoberstar.gradle.git.release.base.ReleasePluginExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph

/**
 * Plugin applied only to the root project that controls version calculation and adds `snapshot`, `candidate`, and `final` tasks.
 */
class SpringReleasePlugin implements Plugin<Project> {
    private Project project
    private ProjectType type

    @Override
    void apply(Project project) {
        this.project = project
        this.type = new ProjectType(project)

        if (!type.isRootProject) {
            throw new GradleException("io.spring.release should only applied to the root project")
        }

        project.plugins.apply ReleasePlugin
        project.extensions.findByType(ReleasePluginExtension).with {
            defaultVersionStrategy = NetflixOssStrategies.SNAPSHOT
        }

        project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
            if (graph.hasTask(':devSnapshot')) {
                throw new GradleException('You cannot use the devSnapshot task from the release plugin. Please use the snapshot task.')
            }
        }

        // CircleCI
        project.tasks.create('initCircle', InitCircleTask)
    }
}