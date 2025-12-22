package traversium.filestorageservice.kafka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "spring.kafka")
class KafkaProperties @ConstructorBinding constructor(
    val bootstrapServers: String,
    val notificationTopic: String?,
    val auditTopic: String?,
    val clientConfirmationTimeout: Long = 10L
)