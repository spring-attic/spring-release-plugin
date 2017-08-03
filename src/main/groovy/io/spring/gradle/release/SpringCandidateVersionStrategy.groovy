package io.spring.gradle.release

import org.ajoberstar.gradle.git.release.base.ReleaseVersion
import org.ajoberstar.gradle.git.release.base.VersionStrategy
import org.ajoberstar.gradle.git.release.opinion.Strategies
import org.ajoberstar.gradle.git.release.semver.ChangeScope
import org.ajoberstar.gradle.git.release.semver.NearestVersionLocator
import org.ajoberstar.gradle.git.release.semver.StrategyUtil
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project

class SpringCandidateVersionStrategy implements VersionStrategy {
    private static final scopes = StrategyUtil.one(Strategies.Normal.USE_SCOPE_PROP,
            Strategies.Normal.ENFORCE_GITFLOW_BRANCH_MAJOR_X, Strategies.Normal.ENFORCE_BRANCH_MAJOR_X,
            Strategies.Normal.ENFORCE_GITFLOW_BRANCH_MAJOR_MINOR_X, Strategies.Normal.ENFORCE_BRANCH_MAJOR_MINOR_X,
            Strategies.Normal.USE_NEAREST_ANY, Strategies.Normal.useScope(ChangeScope.MINOR))

    VersionStrategy delegate = Strategies.PRE_RELEASE.copyWith(normalStrategy: scopes)

    @Override
    String getName() {
        return delegate.name
    }

    @Override
    boolean selector(Project project, Grgit grgit) {
        return delegate.selector(project, grgit)
    }

    @Override
    ReleaseVersion infer(Project project, Grgit grgit) {
        def locator = new NearestVersionLocator(new SpringReleaseTagStrategy())

        // by calling doInfer, we are subverting delegate's normal determination of how to calculate the nearest "release", and
        // stripping the .RELEASE off of it
        return delegate.doInfer(project, grgit, locator)
    }
}
