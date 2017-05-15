package io.spring.gradle.project

import org.ajoberstar.gradle.git.release.base.ReleasePluginExtension
import org.ajoberstar.gradle.git.release.base.ReleaseVersion
import org.ajoberstar.gradle.git.release.base.VersionStrategy
import org.ajoberstar.gradle.git.release.semver.NearestVersionLocator
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project;

class SpringReleaseLastTagStrategy implements VersionStrategy {
    @Override
    String getName() {
        return 'use-last-tag'
    }

    @Override
    boolean selector(Project project, Grgit grgit) {
        if (project.findProperty('release.useLastTag')?.toBoolean()) {
            project.tasks.release.deleteAllActions() // remove tagging op since already tagged
            return true
        }
        return false
    }

    @Override
    ReleaseVersion infer(Project project, Grgit grgit) {
        def tagStrategy = project.extensions.getByType(ReleasePluginExtension).tagStrategy
        def locate = new NearestVersionLocator(tagStrategy).locate(grgit)
        return new ReleaseVersion(locate.any.toString(), null, false)
    }
}