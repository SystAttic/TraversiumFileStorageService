package traversium.filestorageservice.kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer
import java.util.concurrent.TimeUnit

@Configuration
@EnableKafka
@EnableConfigurationProperties(KafkaProperties::class)
@ConditionalOnProperty(prefix = "spring.kafka", name = ["bootstrap-servers"])
class KafkaConfig {

    @Bean
    fun producerFactory(
        kafkaProperties: KafkaProperties,
        environment: Environment,
    ): ProducerFactory<String, Any> {
        val producerConfiguration: MutableMap<String, Any> = HashMap()
        producerConfiguration[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaProperties.bootstrapServers
        producerConfiguration[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        producerConfiguration[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java.name
        KAFKA_PROPERTIES.forEach { kafkaPropertyName ->
            environment.getProperty(KAFKA_PROPERTY_PREFIX + kafkaPropertyName)?.also {
                producerConfiguration[kafkaPropertyName] = it
            }
        }

        return DefaultKafkaProducerFactory(producerConfiguration)
    }

    @Bean
    fun kafkaTemplate(
        producerFactory: ProducerFactory<String, Any>,
        @Value("\${${KAFKA_PROPERTY_PREFIX}health.indicator.timeout.ms:1000}")
        kafkaTimeoutForHealthEndpoint: Long
    ): KafkaTemplate<String, Any> {
        val kafkaTemplate = KafkaTemplate(producerFactory)
        kafkaTemplate.send("kafka-health-indicator", "❥")[kafkaTimeoutForHealthEndpoint, TimeUnit.MILLISECONDS]
        return kafkaTemplate
    }

    companion object {
        private const val KAFKA_PROPERTY_PREFIX = "spring.kafka."
        private val KAFKA_PROPERTIES = arrayOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            ProducerConfig.ACKS_CONFIG,
            ProducerConfig.BUFFER_MEMORY_CONFIG,
            ProducerConfig.COMPRESSION_TYPE_CONFIG,
            ProducerConfig.RETRIES_CONFIG,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG,
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG,
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
            ProducerConfig.BATCH_SIZE_CONFIG,
            ProducerConfig.CLIENT_ID_CONFIG,
            ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG,
            ProducerConfig.LINGER_MS_CONFIG,
            ProducerConfig.MAX_BLOCK_MS_CONFIG,
            ProducerConfig.MAX_REQUEST_SIZE_CONFIG,
            ProducerConfig.PARTITIONER_CLASS_CONFIG,
            ProducerConfig.RECEIVE_BUFFER_CONFIG,
            ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
            SaslConfigs.SASL_JAAS_CONFIG,
            SaslConfigs.SASL_KERBEROS_SERVICE_NAME,
            SaslConfigs.SASL_MECHANISM,
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
            ProducerConfig.SEND_BUFFER_CONFIG,
            SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG,
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG,
            SslConfigs.SSL_PROTOCOL_CONFIG,
            SslConfigs.SSL_PROVIDER_CONFIG,
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
            ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
            ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
            ProducerConfig.METADATA_MAX_AGE_CONFIG,
            ProducerConfig.METRIC_REPORTER_CLASSES_CONFIG,
            ProducerConfig.METRICS_NUM_SAMPLES_CONFIG,
            ProducerConfig.METRICS_RECORDING_LEVEL_CONFIG,
            ProducerConfig.METRICS_SAMPLE_WINDOW_MS_CONFIG,
            ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG,
            ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG,
            ProducerConfig.RETRY_BACKOFF_MS_CONFIG,
            SaslConfigs.SASL_KERBEROS_KINIT_CMD,
            SaslConfigs.SASL_KERBEROS_MIN_TIME_BEFORE_RELOGIN,
            SaslConfigs.SASL_KERBEROS_TICKET_RENEW_JITTER,
            SaslConfigs.SASL_KERBEROS_TICKET_RENEW_WINDOW_FACTOR,
            SslConfigs.SSL_CIPHER_SUITES_CONFIG,
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
            SslConfigs.SSL_KEYMANAGER_ALGORITHM_CONFIG,
            SslConfigs.SSL_SECURE_RANDOM_IMPLEMENTATION_CONFIG,
            SslConfigs.SSL_TRUSTMANAGER_ALGORITHM_CONFIG,
            ProducerConfig.TRANSACTION_TIMEOUT_CONFIG,
            ProducerConfig.TRANSACTIONAL_ID_CONFIG
        )
    }
}