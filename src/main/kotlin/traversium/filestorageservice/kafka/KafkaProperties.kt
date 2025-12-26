package traversium.filestorageservice.kafka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import org.springframework.cloud.context.config.annotation.RefreshScope

@ConfigurationProperties(prefix = "spring.kafka")
@RefreshScope
class KafkaProperties @ConstructorBinding constructor(
    val bootstrapServers: String,
    val notificationTopic: String?,
    val auditTopic: String?,
    val clientConfirmationTimeout: Long = 10L
)