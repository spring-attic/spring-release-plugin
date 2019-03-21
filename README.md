# Spring Release Plugin

[![Build Status](https://circleci.com/gh/spring-gradle-plugins/spring-release-plugin.svg?style=svg)](https://circleci.com/gh/spring-gradle-plugins/spring-release-plugin)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/spring-gradle-plugins/spring-release-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Apache 2.0](https://img.shields.io/github/license/spring-gradle-plugins/spring-release-plugin.svg)](https://www.apache.org/licenses/LICENSE-2.0)

A Gradle plugin that applies healthy defaults to a Spring project to enable
releases with minimal manual intervention.

## Requirements

 - Gradle 3.x. Officially tested on Gradle 3.5.

## Using the plugin

To apply the plugin, see the instructions on the [Gradle plugin portal](https://plugins.gradle.org/plugin/io.spring.release).

The project plugin applies a set of sensible defaults to Spring projects:

1. Adds a properties section to a generated POM publication with information about the
environment and status of the repository when the artifact was built.
2. Configures a `maven-publish` publication.
3. Substitutes dynamic revisions in the generated POM with fixed versions.
4. Adds SCM information to the POM.
5. Adds exclude information to the POM.
6. Generates a javadoc jar
7. Generates a source jar
8. Applies the Nebula contacts plugin, requiring information about chief committers.
9. Configures license header checks on files. Also places a default license header template
in `gradle/licenseHeader.txt` which is applied to any file needing a license header. Conditions
release builds upon having proper license headers in all source files.
10. Configures Asciidoc compilation and uploading to Github pages.
11. Adds the `nebula.release` plugin.
12. Configures `nebula.bintray-publishing`, automatically filling all the relevant Bintray configuration
based on any git remote attached to your repository that has an organization containing the word `spring`.

## Releasing projects

### From CircleCI

To generate a CircleCI configuration for your project, run `./gradlew initCircle`. Commit the generated files. Enable
your project in CircleCI.

To release your project, create a release in Github with a version like `1.0.0` (semver). Alternatively,
tag your project with a version like `1.0.0` and push the tag. Both have the effect of causing the CircleCI configuration
to realize a release is being built and do the right thing.

### From the command line (not recommended)

To build and upload a release to Bintray, run `./gradlew clean final`. This will build the project, upload it to
Bintray, tag the repository and push the tag to the origin remote. The version number will be a minor version increment
of the latest semver-like tag found in the repo.

To perform a major or patch release, run `./gradlew clean final -Prelease.scope=major` or `./gradlew clean final -Prelease.scope=patch`.

## License header management

To generate license headers for your source files, run `./gradlew licenseFormat`.

## Documentation management

To build docs, run `./gradlew asciidoctor`.

To upload docs to your project's Github pages, run `./gradlew publishGithubPages`.
