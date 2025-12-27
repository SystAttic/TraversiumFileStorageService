package traversium.filestorageservice.service

import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.models.BlobProperties
import com.azure.storage.blob.models.BlobStorageException
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Metadata
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationEventPublisher
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import traversium.audit.kafka.AuditStreamData
import traversium.commonmultitenancy.TenantContext
import traversium.filestorageservice.exception.FileDeleteException
import traversium.filestorageservice.exception.FileDownloadException
import traversium.filestorageservice.exception.FileNotFoundException
import traversium.filestorageservice.exception.FileUploadException
import traversium.filestorageservice.exception.UnauthorizedMediaAcessException
import traversium.filestorageservice.restclient.TripServiceClient
import traversium.filestorageservice.security.TraversiumAuthentication
import traversium.filestorageservice.security.TraversiumPrincipal
import java.io.IOException
import java.io.InputStream

@ExtendWith(MockKExtension::class)
class FileStorageServiceTest {

    @MockK
    lateinit var blobServiceClient: BlobServiceClient

    @MockK
    lateinit var blobContainerClient: BlobContainerClient

    @MockK
    lateinit var blobClient: BlobClient

    @MockK
    lateinit var eventPublisher: ApplicationEventPublisher

    @MockK
    lateinit var tripServiceClient: TripServiceClient

    @InjectMockKs
    lateinit var fileStorageService: FileStorageService

    @BeforeEach
    fun setUp() {
        mockkStatic(ImageMetadataReader::class)
        mockkStatic(SecurityContextHolder::class)

        TenantContext.setTenant("test-tenant")

        every { blobServiceClient.getBlobContainerClient(any()) } returns blobContainerClient
        every { blobContainerClient.getBlobClient(any()) } returns blobClient

        every { blobContainerClient.exists() } returns true
        every { blobContainerClient.create() } just runs

        val mockContext = mockk<SecurityContext>()
        val mockPrincipal = TraversiumPrincipal(uid = "test-firebase-id", email = "test@test.com", photoUrl = null)
        val mockAuth = TraversiumAuthentication(
            principal = mockPrincipal,
            details = null,
            authorities = emptyList(),
            token = "fake-token"
        )

        every { SecurityContextHolder.getContext() } returns mockContext
        every { mockContext.authentication } returns mockAuth

        every { eventPublisher.publishEvent(any<AuditStreamData>()) } just runs

        every { tripServiceClient.checkViewPermission(any(), any()) } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(ImageMetadataReader::class)
        TenantContext.clear()
    }

    // --- UPLOAD TESTS ---

    @Test
    fun `postMediaFile - Success Uploading Image`() {
        val file = MockMultipartFile("file", "vacation.jpg", "image/jpeg", "fake-data".toByteArray())

        every { blobClient.upload(any<InputStream>(), any(), true) } just runs
        every { blobClient.setHttpHeaders(any()) } just runs

        val properties = mockk<BlobProperties>()
        every { properties.blobSize } returns file.size
        every { blobClient.properties } returns properties

        // Mock metadata extraction to return empty metadata (prevents actual parsing)
        every { ImageMetadataReader.readMetadata(any<InputStream>()) } returns Metadata()

        val result = fileStorageService.postMediaFile(file)

        assertNotNull(result)
        assertEquals("Image", result.fileType)
        assertTrue(result.filename.endsWith(".jpg"))
        verify { blobClient.upload(any(), file.size, true) }

        //verify that the event was published
        verify { eventPublisher.publishEvent(any<AuditStreamData>()) }
    }

    @Test
    fun `postMediaFile - Success Uploading Video`() {
        val file = MockMultipartFile("file", "clip.mp4", "video/mp4", "video-data".toByteArray())

        every { blobClient.upload(any<InputStream>(), any(), true) } just runs
        every { blobClient.setHttpHeaders(any()) } just runs
        every { blobClient.properties.blobSize } returns file.size
        every { ImageMetadataReader.readMetadata(any<InputStream>()) } returns Metadata()

        val result = fileStorageService.postMediaFile(file)

        assertEquals("Video", result.fileType)
        assertEquals("mp4", result.fileFormat)

        //verify that the event was published
        verify { eventPublisher.publishEvent(any<AuditStreamData>()) }
    }

    @Test
    fun `postMediaFile - Throws FileUploadException on Azure Error`() {
        val file = MockMultipartFile("file", "test.txt", "text/plain", "data".toByteArray())

        // Simulate Azure throwing an exception
        every { blobClient.upload(any<InputStream>(), any(), true) } throws mockk<BlobStorageException>()

        assertThrows<FileUploadException> {
            fileStorageService.postMediaFile(file)
        }
    }

    @Test
    fun `postMediaFile - Throws FileUploadException on IO Error`() {
        val file = mockk<MockMultipartFile>()
        every { file.originalFilename } returns "test.jpg"
        every { file.contentType } returns "image/jpeg"
        every { file.inputStream } throws IOException("Disk full")

        assertThrows<FileUploadException> {
            fileStorageService.postMediaFile(file)
        }
    }

    // --- DOWNLOAD TESTS ---

    @Test
    fun `getMediaFile - Success`() {
        val filename = "existing-file.png"
        val fakeContent = "content".toByteArray()

        every { blobClient.exists() } returns true
        val mockBinaryData = mockk<BinaryData>()
        every { blobClient.downloadContent() } returns mockBinaryData
        every { mockBinaryData.toBytes() } returns fakeContent

        val properties = mockk<BlobProperties>()
        every { properties.contentType } returns "image/png"
        every { blobClient.properties } returns properties

        val result = fileStorageService.getMediaFile(filename)

        assertNotNull(result)
        assertEquals("image/png", result.contentType)
        assertArrayEquals(fakeContent, result.resource.inputStream.readAllBytes())
    }

    @Test
    fun `getMediaFile - Throws FileNotFoundException when blob does not exist`() {
        every { blobClient.exists() } returns false

        val exception = assertThrows<FileNotFoundException> {
            fileStorageService.getMediaFile("missing.jpg")
        }
        assertTrue(exception.message!!.contains("not found"))
    }

    @Test
    fun `getMediaFile - Throws FileDownloadException on Azure error`() {
        every { blobClient.exists() } returns true
        every { blobClient.downloadContent() } throws mockk<BlobStorageException>()

        assertThrows<FileDownloadException> {
            fileStorageService.getMediaFile("error.jpg")
        }
    }

    @Test
    fun `getMediaFile - Should throw UnauthorizedMediaAcessException when permission denied`() {
        val filename = "private-file.jpg"

        every { tripServiceClient.checkViewPermission(filename, any()) } returns false

        assertThrows<UnauthorizedMediaAcessException> {
            fileStorageService.getMediaFile(filename)
        }

        verify(exactly = 0) { blobClient.downloadContent() }
    }


    // --- DELETE TESTS ---

    @Test
    fun `deleteMediaFile - Success`() {
        every { blobClient.deleteIfExists() } returns true

        fileStorageService.deleteMediaFile("delete-me.jpg")

        verify { blobClient.deleteIfExists() }

        //verify that the event was published
        verify { eventPublisher.publishEvent(any<AuditStreamData>()) }
    }

    @Test
    fun `deleteMediaFile - Handles non-existent file gracefully`() {
        every { blobClient.deleteIfExists() } returns false

        fileStorageService.deleteMediaFile("non-existent.jpg")

        verify { blobClient.deleteIfExists() }
    }

    @Test
    fun `deleteMediaFile - Throws FileDeleteException on Azure Error`() {
        every { blobClient.deleteIfExists() } throws mockk<BlobStorageException>()

        assertThrows<FileDeleteException> {
            fileStorageService.deleteMediaFile("fail.jpg")
        }
    }

}