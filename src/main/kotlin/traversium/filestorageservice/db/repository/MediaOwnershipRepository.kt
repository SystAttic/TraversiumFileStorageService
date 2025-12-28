package traversium.filestorageservice.db.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import traversium.filestorageservice.db.model.MediaOwnership
import java.util.*

@Repository
interface MediaOwnershipRepository : JpaRepository<MediaOwnership, UUID> {
    fun findByFilename(filename: String): MediaOwnership?
}