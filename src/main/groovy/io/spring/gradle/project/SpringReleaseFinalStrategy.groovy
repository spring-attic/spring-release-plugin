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
package io.spring.gradle.project

import org.ajoberstar.gradle.git.release.base.ReleaseVersion;
import org.ajoberstar.gradle.git.release.base.VersionStrategy;
import org.ajoberstar.gradle.git.release.opinion.Strategies;
import org.ajoberstar.gradle.git.release.semver.ChangeScope;
import org.ajoberstar.gradle.git.release.semver.StrategyUtil
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project;

class SpringReleaseFinalStrategy implements VersionStrategy {
    private static final scopes = StrategyUtil.one(Strategies.Normal.USE_SCOPE_PROP,
        Strategies.Normal.ENFORCE_GITFLOW_BRANCH_MAJOR_X, Strategies.Normal.ENFORCE_BRANCH_MAJOR_X,
        Strategies.Normal.ENFORCE_GITFLOW_BRANCH_MAJOR_MINOR_X, Strategies.Normal.ENFORCE_BRANCH_MAJOR_MINOR_X,
        Strategies.Normal.USE_NEAREST_ANY, Strategies.Normal.useScope(ChangeScope.MINOR))

    private static final semverFinal = Strategies.FINAL.copyWith(normalStrategy: scopes)

    @Override
    String getName() {
        return 'final'
    }

    @Override
    boolean selector(Project project, Grgit grgit) {
        return semverFinal.selector(project, grgit)
    }

    @Override
    ReleaseVersion infer(Project project, Grgit grgit) {
        println("Using spring release final strategy")
        def semverVersion = semverFinal.infer(project, grgit)
        return new ReleaseVersion(semverVersion.version + '.RELEASE', semverVersion.previousVersion,
            semverVersion.createTag)
    }
}
