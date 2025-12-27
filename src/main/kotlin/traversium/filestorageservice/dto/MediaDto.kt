package traversium.filestorageservice.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

data class MediaDto(
    val mediaId: Long?,
    val pathUrl: String? = null,
    val uploader: String?,
    val fileType: String? = null,
    val fileFormat: String? = null,
    val fileSize: Long? = null,
    val geoLocation: String? = null,
    val createdAt : OffsetDateTime? = null,

    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val uploadedAt: OffsetDateTime? = null,
)