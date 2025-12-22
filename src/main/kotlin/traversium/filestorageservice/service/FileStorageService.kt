package traversium.filestorageservice.service

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.BlobHttpHeaders
import com.azure.storage.blob.models.BlobStorageException
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import traversium.filestorageservice.dto.FileDataDto
import java.util.*
import java.io.IOException
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.core.io.ByteArrayResource
import traversium.audit.kafka.ActivityType
import traversium.audit.kafka.AuditStreamData
import traversium.audit.kafka.EntityType
import traversium.filestorageservice.dto.GeoLocation
import traversium.filestorageservice.exception.*
import java.io.BufferedInputStream
import java.time.OffsetDateTime
import java.time.ZoneOffset

data class FileDownload(val resource: Resource, val contentType: String)

fun Date.toOffsetDateTimeUTC(): OffsetDateTime {
    return this.toInstant().atOffset(ZoneOffset.UTC)
}

private fun extractMediaDetails(
    file: MultipartFile,
    filename: String
): FileDataDto {
    val contentType = file.contentType ?: "application/octet-stream"
    val fileType = when {
        contentType.startsWith("image/") -> "Image"
        contentType.startsWith("video/") -> "Video"
        else -> "File"
    }
    val initialFormat = contentType.substringAfter('/')

    val inputStream = BufferedInputStream(file.inputStream)
    inputStream.mark(file.size.toInt() + 1)
    val metadata = ImageMetadataReader.readMetadata(inputStream)

    val finalformat = metadata.getFirstDirectoryOfType(com.drew.metadata.jpeg.JpegDirectory::class.java)?.name
        ?: metadata.getFirstDirectoryOfType(com.drew.metadata.png.PngDirectory::class.java)?.name
        ?: metadata.getFirstDirectoryOfType(com.drew.metadata.heif.HeifDirectory::class.java)?.name
        ?: metadata.getFirstDirectoryOfType(com.drew.metadata.gif.GifHeaderDirectory::class.java)?.name
        ?: metadata.getFirstDirectoryOfType(com.drew.metadata.webp.WebpDirectory::class.java)?.name
        ?: metadata.getFirstDirectoryOfType(com.drew.metadata.bmp.BmpHeaderDirectory::class.java)?.name
        ?: initialFormat

    val width = metadata.getFirstDirectoryOfType(com.drew.metadata.jpeg.JpegDirectory::class.java)?.imageWidth
        ?: metadata.getFirstDirectoryOfType(com.drew.metadata.png.PngDirectory::class.java)?.getInt(com.drew.metadata.png.PngDirectory.TAG_IMAGE_WIDTH)
        ?: metadata.getFirstDirectoryOfType(com.drew.metadata.gif.GifHeaderDirectory::class.java)?.getInt(com.drew.metadata.gif.GifHeaderDirectory.TAG_IMAGE_WIDTH)

    val height = metadata.getFirstDirectoryOfType(com.drew.metadata.jpeg.JpegDirectory::class.java)?.imageHeight
        ?: metadata.getFirstDirectoryOfType(com.drew.metadata.png.PngDirectory::class.java)?.getInt(com.drew.metadata.png.PngDirectory.TAG_IMAGE_HEIGHT)
        ?: metadata.getFirstDirectoryOfType(com.drew.metadata.gif.GifHeaderDirectory::class.java)?.getInt(com.drew.metadata.gif.GifHeaderDirectory.TAG_IMAGE_HEIGHT)

    val exifDir = metadata.getFirstDirectoryOfType(com.drew.metadata.exif.ExifSubIFDDirectory::class.java)
    val creationTime = exifDir?.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, TimeZone.getDefault())
        ?.toOffsetDateTimeUTC()
    val gpsDir = metadata.getFirstDirectoryOfType(com.drew.metadata.exif.GpsDirectory::class.java)
    val geoLocation = gpsDir?.geoLocation?.let { GeoLocation(it.latitude, it.longitude) }

    return FileDataDto(
        filename = filename,
        fileType = fileType,
        fileFormat = finalformat,
        size = file.size,
        width = width,
        height = height,
        geoLocation = geoLocation,
        creationTime = creationTime,
        uploadTime = OffsetDateTime.now()
    )
}

@Service
class FileStorageService(
    private val blobContainerClient: BlobContainerClient,
    private val eventPublisher: ApplicationEventPublisher,
): Logging {

    fun postMediaFile(file: MultipartFile): FileDataDto {
        val fileExtension = file.originalFilename?.substringAfterLast(".", "")
        val uniqueFilename = "${UUID.randomUUID()}$.$fileExtension"

        val blobClient = blobContainerClient.getBlobClient(uniqueFilename)

        val contentType = file.contentType ?: "application/octet-stream"
        val fileType = when {
            contentType.startsWith("image") -> "image"
            contentType.startsWith("video") -> "video"
            else -> "file"
        }
        val fileFormat = contentType.substringAfter('/')

        try {
            val headers = BlobHttpHeaders().setContentType(file.contentType)
            blobClient.upload(file.inputStream, file.size, true)
            blobClient.setHttpHeaders(headers)

            val properties = blobClient.properties
            logger.info("Successfully upoaded file: $uniqueFilename with size ${properties.blobSize}")

            return extractMediaDetails(file, uniqueFilename)
        } catch (e: BlobStorageException) {
            logger.error("Azure Storage Error during upload for blob: $uniqueFilename", e)
            throw FileUploadException("Failed to upload file to Azure due to a storage error.", e)
        } catch (e: IOException) {
            logger.error("Network/IO Error during upload for blob: $uniqueFilename", e)
            throw FileUploadException("Failed to upload file due to a network or IO error.", e)
        }

    }

    fun getMediaFile(filename: String): FileDownload {
        val blobClient = blobContainerClient.getBlobClient(filename)

        try {
            if(!blobClient.exists()){
                logger.warn("File not found: Download requested for non-existent blob '$filename'")
                throw FileNotFoundException("File not found with name: $filename")
            }

            val data = blobClient.downloadContent().toBytes()
            val contentType = blobClient.properties.contentType

            //logger.info("Successfully downloaded file '$filename' with content type '$contentType'")

            return FileDownload(resource = ByteArrayResource(data), contentType = contentType)
        } catch (e: BlobStorageException) {
            logger.error("Azure Storage Error during download for blob: $filename", e)
            throw FileDownloadException("Failed to download file '$filename' due to a storage error.", e)
        }
    }

    fun deleteMediaFile(filename: String) {
        val blobClient = blobContainerClient.getBlobClient(filename)

        try {
            val deleted = blobClient.deleteIfExists()

            if(deleted){
                logger.info("Successfully deleted file: $filename")
            } else {
                logger.warn("Delete requested for non-existent file '$filename'")
            }
        } catch (e: BlobStorageException) {
            logger.error("Azure Storage Error during deletion for blob: $filename", e)
            throw FileDeleteException("Failed to delete file '$filename' due to a storage error.", e)
        }
    }
}