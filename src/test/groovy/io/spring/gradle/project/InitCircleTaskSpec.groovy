package io.spring.gradle.project

import nebula.test.IntegrationTestKitSpec
import org.gradle.api.GradleException

class InitCircleTaskSpec extends IntegrationTestKitSpec {
    def setup() {
        buildFile << '''
            plugins {
                id 'io.spring.project'
            }
        '''
    }

    def 'generate circle.yml'() {
        when:
        runTasks('initCircle')

        then:
        new File(projectDir, 'circle.yml').exists()
    }

    def 'generate ciBuild.sh'() {
        when:
        runTasks('initCircle')
        def ciBuild = new File(projectDir, 'gradle/ciBuild.sh')

        then:
        ciBuild.exists()
        ciBuild.canExecute()
    }

    def 'unable to encrypt keys if gradle.properties already exists'() {
        setup:
        new File(projectDir, 'gradle.properties') << 'myProp=myValue'

        when:
        runTasks('initCircle')

        then:
        thrown GradleException
    }

    def 'generate an encrypted gradle.properties.enc with publishing keys'() {

    }
}
