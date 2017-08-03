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