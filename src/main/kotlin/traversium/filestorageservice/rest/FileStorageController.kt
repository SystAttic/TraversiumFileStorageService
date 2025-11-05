package traversium.filestorageservice.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.tags.Tag
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import traversium.filestorageservice.dto.PostResponse
import traversium.filestorageservice.exception.*
import traversium.filestorageservice.service.FileStorageService

@RestController
@RequestMapping("/rest/v1/media")
@Tag(name = "File Storage", description = "Endpoints for uploading and managing media")
class FileStorageController(
    private val fileStorageService: FileStorageService
): Logging {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Upload media file",
        description = "Uploads a media file to Azure Blob Storage and returns the unique filename.",
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "File successfully uploaded.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = PostResponse::class)
                )]
            ),
            ApiResponse(responseCode = "400", description = "Bad Request - No file provided or invalid file."),
            ApiResponse(responseCode = "500", description = "Internal Server Error - Failed to upload the file to storage.")
        ]
    )
    fun postMediaFile(@RequestParam("file") file: MultipartFile): ResponseEntity<PostResponse> {
        if (file.isEmpty) {
            logger.warn("Upload attempt with an empty file.")
            return ResponseEntity.badRequest().build()
        }

        return try {
            val responseDto = fileStorageService.postMediaFile(file)
            ResponseEntity.status(HttpStatus.CREATED).body(responseDto)
        } catch (ex: FileUploadException) {
            logger.error("File upload failed for original filename: ${file.originalFilename}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/{filename}")
    @Operation(
        summary = "Get a file by its unique filename.",
        responses = [
            ApiResponse(responseCode = "200",description = "File content successfully retrieved."),
            ApiResponse(responseCode = "404", description = "File not found."),
            ApiResponse(responseCode = "500", description = "Internal Server Error - Failed to download the file.")
        ]
    )
    fun downloadFile(@PathVariable filename: String): ResponseEntity<Any> {
        return try {
            val fileDownload = fileStorageService.getMediaFile(filename)

            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, fileDownload.contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
                .body(fileDownload.resource)
        } catch (ex: FileNotFoundException) {
            ResponseEntity.notFound().build()
        } catch (ex: FileDownloadException) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @DeleteMapping("/{filename}")
    @Operation(
        summary = "Delete a file by its unique filename.",
        responses = [
            ApiResponse(responseCode = "204", description = "File successfully deleted."),
            ApiResponse(responseCode = "500", description = "Internal Server Error - Failed to delete the file.")
        ]
    )
    fun deleteFile(@PathVariable filename: String): ResponseEntity<Void> {
        return try {
            fileStorageService.deleteMediaFile(filename)
            ResponseEntity.noContent().build()
        } catch (ex: FileDeleteException) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}