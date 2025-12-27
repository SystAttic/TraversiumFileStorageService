package traversium.filestorageservice.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Thrown when user is not allowed to view the media file
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
class UnauthorizedMediaAcessException(message: String) : RuntimeException(message)