package traversium.filestorageservice.dto

import java.time.LocalDateTime
import java.time.OffsetDateTime

data class FileDataDto(
    val filename: String,
    val fileType: String?,
    val fileFormat : String?,

    val size: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationSeconds: Double? = null,

    val geoLocation: GeoLocation? = null,
    val creationTime: OffsetDateTime? = null,

    val uploadTime: OffsetDateTime
)

data class GeoLocation(
    val latitude: Double,
    val longitude: Double
)