package io.spring.gradle.project

import nebula.test.IntegrationTestKitSpec
import org.gradle.api.GradleException
import spock.lang.IgnoreIf

class InitCircleTaskSpec extends IntegrationTestKitSpec {
    def setup() {
        buildFile << '''
            plugins {
                id 'io.spring.project'
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
        thrown GradleException
    }

    @IgnoreIf({ try { "lpass".execute(); false } catch(ignored) { true } })
    def 'generate an encrypted gradle.properties.enc with publishing keys'() {

    }
}
