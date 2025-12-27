package traversium.filestorageservice

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClient
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.ActiveProfiles
import traversium.filestorageservice.restclient.TripServiceClient

@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",

    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.cloud.azure.storage.blob.account-key=dummy-key",
    "AZURE_STORAGE_ACCOUNT_KEY=dummy-key"
])
@ActiveProfiles("test")
class FileStorageServiceApplicationTests {

    @MockkBean(relaxed = true)
    lateinit var blobServiceClient: BlobServiceClient

    @MockkBean(relaxed = true)
    lateinit var firebaseAuth: FirebaseAuth

    @MockkBean(relaxed = true)
    lateinit var firebaseApp: FirebaseApp

    @MockkBean(relaxed = true)
    lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @MockkBean(relaxed = true)
    lateinit var tripServiceClient: TripServiceClient

    @Test
    fun contextLoads() {
    }
}