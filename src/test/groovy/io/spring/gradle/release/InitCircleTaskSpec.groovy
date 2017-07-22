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
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Ignore
import spock.lang.IgnoreIf

@Ignore
class InitCircleTaskSpec extends IntegrationTestKitSpec {
    def setup() {
        buildFile << '''
            plugins {
                id 'io.spring.release'
            }
        '''
    }

    @IgnoreIf({ try { "lpass".execute(); false } catch(ignored) { true } })
    def 'generate circle.yml'() {
        when:
        runTasks('initCircle')

        then:
        new File(projectDir, 'circle.yml').exists()
    }

    @IgnoreIf({ try { "lpass".execute(); false } catch(ignored) { true } })
    def 'generate ciBuild.sh'() {
        when:
        runTasks('initCircle')
        def ciBuild = new File(projectDir, 'gradle/ciBuild.sh')

        then:
        ciBuild.exists()
        ciBuild.canExecute()
    }

    @IgnoreIf({ try { "lpass".execute(); false } catch(ignored) { true } })
    def 'unable to encrypt keys if gradle.properties already exists'() {
        setup:
        new File(projectDir, 'gradle.properties') << 'myProp=myValue'

        when:
        runTasks('initCircle')

        then:
        thrown UnexpectedBuildFailure
    }

    @IgnoreIf({ try { "lpass".execute(); false } catch(ignored) { true } })
    def 'generate an encrypted gradle.properties.enc with publishing keys'() {

    }
}
