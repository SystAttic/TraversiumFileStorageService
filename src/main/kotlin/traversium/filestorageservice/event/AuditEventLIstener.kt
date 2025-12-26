package traversium.filestorageservice.event

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.boot.actuate.audit.AuditEvent
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.event.EventListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import traversium.audit.kafka.AuditStreamData
import traversium.commonmultitenancy.TenantContext
import traversium.filestorageservice.kafka.KafkaProperties

@Component
@ConditionalOnProperty(name = ["spring.kafka.audit-topic"])
class AuditEventListener(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val kafkaProperties: KafkaProperties
): Logging {

    @EventListener
    fun sendAuditDataToKafka(event: AuditStreamData) {
        val tenantId = TenantContext.getTenant()

        logger.info("Publish event to kafka")

        val record = ProducerRecord<String, Any>(kafkaProperties.auditTopic!!, event)
        tenantId.let {
            record.headers().add(RecordHeader("tenantId", it.toByteArray()))
        }

        kafkaTemplate.send(record)
    }
}