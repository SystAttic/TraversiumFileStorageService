package traversium.filestorageservice.service

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.BlobHttpHeaders
import com.azure.storage.blob.models.BlobStorageException
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import traversium.filestorageservice.dto.PostResponse
import java.util.*
import java.io.IOException
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.core.io.ByteArrayResource
import traversium.filestorageservice.exception.*

data class FileDownload(val resource: Resource, val contentType: String)

@Service
class FileStorageService(
    private val blobContainerClient: BlobContainerClient
): Logging {

    fun postMediaFile(file: MultipartFile): PostResponse {
        val fileExtension = file.originalFilename?.substringAfterLast(".", "")
        val uniqueFilename = "${UUID.randomUUID()}$.$fileExtension"

        val blobClient = blobContainerClient.getBlobClient(uniqueFilename)

        try {
            val headers = BlobHttpHeaders().setContentType(file.contentType)
            blobClient.upload(file.inputStream, file.size, true)
            blobClient.setHttpHeaders(headers)

            val properties = blobClient.properties
            logger.info("Successfully upoaded file: $uniqueFilename with size ${properties.blobSize}")

            return PostResponse(uniqueFilename)
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