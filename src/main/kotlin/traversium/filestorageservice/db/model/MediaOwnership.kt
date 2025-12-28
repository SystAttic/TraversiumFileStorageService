package traversium.filestorageservice.db.model

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "media_ownership")
class MediaOwnership(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    val filename: String,

    @Column(nullable = false)
    val ownerId: String, // Firebase UID of the uploader

    @Column(nullable = false)
    val uploadTime: OffsetDateTime = OffsetDateTime.now()

)