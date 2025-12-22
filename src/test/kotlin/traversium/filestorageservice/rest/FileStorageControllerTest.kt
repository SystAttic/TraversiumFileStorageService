package traversium.filestorageservice.rest

import com.google.firebase.auth.FirebaseAuth
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import traversium.filestorageservice.dto.FileDataDto
import traversium.filestorageservice.exception.*
import traversium.filestorageservice.service.FileDownload
import traversium.filestorageservice.service.FileStorageService
import java.time.OffsetDateTime

@WebMvcTest(FileStorageController::class)
@AutoConfigureMockMvc(addFilters = false) // Skip the actual Firebase filter logic for unit tests
class FileStorageControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var fileStorageService: FileStorageService

    @MockkBean
    lateinit var firebaseAuth: FirebaseAuth

    private val baseUrl = "/rest/v1/media"

    // --- UPLOAD TESTS ---

    @Test
    @WithMockUser
    fun `postMediaFile - Should return 201 and JSON on success`() {
        val file = MockMultipartFile("file", "test.jpg", "image/jpeg", "content".toByteArray())
        val mockResponse = FileDataDto(
            filename = "unique-id$.jpg",
            fileType = "Image",
            fileFormat = "jpeg",
            uploadTime = OffsetDateTime.now()
        )

        every { fileStorageService.postMediaFile(any()) } returns mockResponse

        mockMvc.perform(
            multipart(baseUrl)
                .file(file)
                .with(csrf())
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.filename").value("unique-id$.jpg"))
            .andExpect(jsonPath("$.fileType").value("Image"))
    }

    @Test
    @WithMockUser
    fun `postMediaFile - Should return 400 when file is empty`() {
        val emptyFile = MockMultipartFile("file", "empty.jpg", "image/jpeg", ByteArray(0))

        mockMvc.perform(
            multipart(baseUrl)
                .file(emptyFile)
                .with(csrf())
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser
    fun `postMediaFile - Should return 500 when service fails`() {
        val file = MockMultipartFile("file", "test.jpg", "image/jpeg", "content".toByteArray())

        every { fileStorageService.postMediaFile(any()) } throws FileUploadException("Azure error")

        mockMvc.perform(
            multipart(baseUrl)
                .file(file)
                .with(csrf())
        )
            .andExpect(status().isInternalServerError)
    }

    // --- DOWNLOAD TESTS ---

    @Test
    @WithMockUser
    fun `downloadFile - Should return 200 and file content with headers`() {
        val filename = "test.jpg"
        val content = "fake-binary-data".toByteArray()
        val fileDownload = FileDownload(ByteArrayResource(content), "image/jpeg")

        every { fileStorageService.getMediaFile(filename) } returns fileDownload

        mockMvc.perform(get("$baseUrl/$filename"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/jpeg"))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\""))
            .andExpect(content().bytes(content))
    }

    @Test
    @WithMockUser
    fun `downloadFile - Should return 404 when file not found`() {
        every { fileStorageService.getMediaFile(any()) } throws FileNotFoundException("Not found")

        mockMvc.perform(get("$baseUrl/missing.jpg"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser
    fun `downloadFile - Should return 500 when download fails`() {
        every { fileStorageService.getMediaFile(any()) } throws FileDownloadException("IO error")

        mockMvc.perform(get("$baseUrl/error.jpg"))
            .andExpect(status().isInternalServerError)
    }

    // --- DELETE TESTS ---

    @Test
    @WithMockUser
    fun `deleteFile - Should return 204 on success`() {
        val filename = "delete-me.jpg"
        every { fileStorageService.deleteMediaFile(filename) } just runs

        mockMvc.perform(
            delete("$baseUrl/$filename")
                .with(csrf())
        )
            .andExpect(status().isNoContent)

        verify { fileStorageService.deleteMediaFile(filename) }
    }

    @Test
    @WithMockUser
    fun `deleteFile - Should return 500 on deletion failure`() {
        every { fileStorageService.deleteMediaFile(any()) } throws FileDeleteException("Failed")

        mockMvc.perform(
            delete("$baseUrl/fail.jpg")
                .with(csrf())
        )
            .andExpect(status().isInternalServerError)
    }
}