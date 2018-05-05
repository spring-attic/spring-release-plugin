package io.spring.gradle.release

import org.junit.Ignore
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

@Ignore
class ArtifactoryUploadTaskSpec extends Specification {
    @Rule
    TemporaryFolder temp

    def 'upload artifact'() {
        when:
        def f = temp.newFile()
        f << 'test'

        then:
        new UploadWorker(repoUrl: 'http://localhost:8081/artifactory/libs-snapshot-local', user: 'admin',
                password: 'admin', path: 'com/test/f/1.0.0-SNAPSHOT/f-1.0.0-SNAPSHOT.jar', artifact: f).run()
    }
}
