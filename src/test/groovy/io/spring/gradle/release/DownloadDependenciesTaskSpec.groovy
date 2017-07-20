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
