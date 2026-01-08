package traversium.filestorageservice.restclient

import org.apache.logging.log4j.kotlin.Logging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import traversium.commonmultitenancy.TenantContext
import traversium.commonmultitenancy.TenantUtils

@Component
class TripServiceClient(
    private val tripServiceWebClient: WebClient
): Logging {

    fun checkViewPermission(pathUrl: String, token: String?): Boolean {
        return tripServiceWebClient.get()
            .uri("/rest/v1/media/path/{pathUrl}", pathUrl)
            .header(HttpHeaders.AUTHORIZATION, token)
            .header("X-Tenant-Id", TenantUtils.desanitizeTenantIdFromSchema(TenantContext.getTenant()))
            .exchangeToMono { response ->
                when (response.statusCode()) {
                    HttpStatus.OK, HttpStatus.NOT_FOUND -> Mono.just(true)
                    else -> {
                        logger.warn("Permission check failed for path $pathUrl. Status: ${response.statusCode()}")
                        Mono.just(false)
                    }
                }
            }
            .onErrorResume { e ->
                // Handle network timeouts or connection refused
                logger.error("Network error during permission check: ${e.message}")
                Mono.just(false)
            }
            .block() ?: false
    }
}