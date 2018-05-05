package io.spring.gradle.release

import okhttp3.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.internal.artifact.DefaultMavenArtifact
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ArtifactoryUploadTask extends DefaultTask {
    @Input
    String user

    @Input
    String password

    @Input
    String repoUrl

    @Input
    String publicationName

    @TaskAction
    void uploadArtifacts() {
        def publication = project.extensions.getByType(PublishingExtension).publications.findByName(publicationName)
        if (!(publication instanceof DefaultMavenPublication)) {
            logger.warn("'$publicationName' does not refer to a maven publication, skipping")
            return
        }

        def pomFile = publication.asNormalisedPublication().pomFile
        if (!pomFile.exists()) {
            throw new GradleException("POM file does not exist: ${pomFile.absolutePath}")
        }

        // the POM must be first, or Artifactory will overwrite javadoc and sources of prior snapshots.
        def artifacts = [new DefaultMavenArtifact(pomFile, "pom", null)] + publication.artifacts

        artifacts.each { artifact ->
            def path = (publication.groupId?.replace('.', '/') ?: "") +
                    "/${publication.artifactId}/${publication.version}/${publication.artifactId}-${publication.version}" +
                    (artifact.classifier?.with { "-$it" } ?: "") +
                    ".${artifact.extension}"

            new UploadWorker(repoUrl: repoUrl, user: user, password: password, path: path, artifact: artifact.file).run()
        }
    }
}

class UploadWorker implements Runnable {
    String repoUrl
    String user
    String password
    String path
    File artifact

    private def md5() {
        MessageDigest.getInstance("MD5").digest(artifact.bytes).encodeHex().toString()
    }

    private def sha1() {
        def digest = MessageDigest.getInstance("SHA1")
        artifact.eachByte(1024 * 1024) { byte[] buf, int bufLen ->
            digest.update(buf, 0, bufLen)
        }
        new BigInteger(1, digest.digest()).toString(16).padLeft(40, '0')
    }

    @Override
    void run() {
        println("Uploading $path")

        // See "Deploy Artifact" at https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API
        Request request = new Request.Builder()
                .header("X-Checksum-Sha1", sha1())
                .header("X-Checksum-Md5", md5())
                .url("$repoUrl/$path")
                .put(RequestBody.create(null, artifact))
                .build()

        try {
            Response response = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(3, TimeUnit.MINUTES)
                    .writeTimeout(6, TimeUnit.MINUTES)
                    .authenticator({ _, response ->
                        def credential = Credentials.basic(user, password)
                        response.request().newBuilder()
                                .header("Authorization", credential)
                                .build()
                    })
                    .build()
                    .newCall(request)
                    .execute()

            try {
                if (!response.isSuccessful()) {
                    throw new GradleException("failed to upload $path: HTTP ${response.code()} / ${response.body()?.string()}")
                }
            } finally {
                response.close()
            }
        } catch (ConnectException e) {
            throw new GradleException("Unable to upload file $path", e)
        }
    }
}