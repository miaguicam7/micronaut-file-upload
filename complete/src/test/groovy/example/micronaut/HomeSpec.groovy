package example.micronaut

import geb.spock.GebSpec
import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.AutoCleanup
import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

class HomeSpec extends GebSpec {

    @Shared
    @AutoCleanup
    EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer)

    @IgnoreIf({ !env['AWS_S3_REGION'] || !env['AWS_SECRET_KEY'] || !env['AWS_S3_BUCKET'] || !env['AWS_ACCESS_KEY_ID']})
    @Unroll
    def "upload image to S3 and delete"() {
        given:
        final String imagename = 'blacklogo.png'
        browser.baseUrl = "http://localhost:${embeddedServer.port}"
        File f = new File("src/test/resources/$imagename")

        expect:
        f.exists()

        when:
        embeddedServer.applicationContext.getBean(AwsCredentialsConfigurationProperties)

        then:
        noExceptionThrown()

        when:
        FileRepository fileRepository = embeddedServer.applicationContext.getBean(S3FileRepository)

        then:
        noExceptionThrown()

        when:
        to HomePage

        then:
        at HomePage
        !hasImage()

        when:
        uploadFile(imagename, f.absolutePath)

        then:
        at HomePage
        hasImage()

        when:
        delete()

        then:
        at HomePage
        !hasImage()

        when:
        uploadStreamingFile(imagename, f.absolutePath)

        PollingConditions conditions = new PollingConditions(timeout: 5)

        then:
        conditions.eventually {
            fileRepository.doesObjectExists(imagename)
        }
    }
}
