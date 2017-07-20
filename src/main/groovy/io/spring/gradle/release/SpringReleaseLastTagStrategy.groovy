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

import com.github.zafarkhaja.semver.Version
import net.sf.cglib.proxy.Callback
import net.sf.cglib.proxy.Enhancer
import net.sf.cglib.proxy.MethodInterceptor
import net.sf.cglib.proxy.MethodProxy
import org.ajoberstar.gradle.git.release.base.ReleaseVersion
import org.ajoberstar.gradle.git.release.base.TagStrategy
import org.ajoberstar.gradle.git.release.base.VersionStrategy
import org.ajoberstar.gradle.git.release.semver.NearestVersionLocator
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import org.gradle.api.Project
import org.objenesis.ObjenesisHelper

import java.lang.reflect.Method

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
        def locate = new NearestVersionLocator(new SpringReleaseTagStrategy()).locate(grgit)
        return new ReleaseVersion(locate.any.toString(), null, false)
    }
}

class SpringReleaseTagStrategy extends TagStrategy {
    SpringReleaseTagStrategy() {
        TagStrategy delegate = new TagStrategy()

        toTagString = { ReleaseVersion v ->
            delegate.toTagString(v) + '.RELEASE'
        }

        parseTag = { Tag tag ->
            Version version = delegate.parseTag(new Tag(fullName: tag.fullName.replaceAll(/\.RELEASE$/, '')))
            createVersionProxy(new SpringReleaseVersionInterceptor(version: version))
        }
    }

    private static Version createVersionProxy(final MethodInterceptor interceptor) {
        final Enhancer enhancer = new Enhancer()
        enhancer.setUseCache(false) //important
        enhancer.setSuperclass(Version)
        enhancer.setCallbackType(interceptor.getClass())

        final Class<Version> proxyClass = enhancer.createClass()
        Enhancer.registerCallbacks(proxyClass, [interceptor] as Callback[])
        return (Version) ObjenesisHelper.newInstance(proxyClass)
    }
}

/**
 * Override the typical semver version for releases with one ending in .RELEASE
 */
class SpringReleaseVersionInterceptor implements MethodInterceptor {
    Version version

    @Override
    Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        def result = method.invoke(version, args)
        if(method.name == 'toString')
            return result + '.RELEASE'
        return result
    }
}