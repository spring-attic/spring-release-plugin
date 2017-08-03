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

import org.ajoberstar.gradle.git.release.base.ReleaseVersion
import org.ajoberstar.gradle.git.release.base.TagStrategy
import org.ajoberstar.grgit.Tag

class SpringReleaseTagStrategy extends TagStrategy {
    SpringReleaseTagStrategy() {
        TagStrategy delegate = new TagStrategy()

        toTagString = { ReleaseVersion v ->
            delegate.toTagString(v) + '.RELEASE'
        }

        parseTag = { Tag tag ->
            delegate.parseTag(new Tag(fullName: tag.fullName.replaceAll(/\.RELEASE$/, '')))
        }
    }
}