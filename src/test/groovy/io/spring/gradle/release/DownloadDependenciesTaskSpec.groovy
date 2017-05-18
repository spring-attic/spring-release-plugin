package io.spring.gradle.release

import nebula.test.IntegrationTestKitSpec

class DownloadDependenciesTaskSpec extends IntegrationTestKitSpec {
    def setup() {
        buildFile << '''
            plugins {
                id 'io.spring.release'
            }
        '''
    }

    /**
     * Useful for CircleCI to provide for effective caching:
     * https://discuss.circleci.com/t/effective-caching-for-gradle/540/2
     */
    def 'pre-download dependencies'() {
        expect:
        runTasks('downloadDependencies', '-s')
    }
}
