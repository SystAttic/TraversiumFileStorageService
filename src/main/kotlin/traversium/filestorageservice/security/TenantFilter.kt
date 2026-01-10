package traversium.filestorageservice.security

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import traversium.commonmultitenancy.TenantContext
import traversium.commonmultitenancy.TenantUtils

/**
 * @author Maja Razinger
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TenantFilter(
    private val firebaseAuth: FirebaseAuth
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val tenantId = request.getHeader("X-Tenant-Id")

            if (tenantId != "public" && tenantId != null) {
                val tenantExists = checkIfTenantExistsInFirebase(tenantId)
                if (!tenantExists) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tenant does not exist")
                    return
                }
            }
            val sanitizedTenantId = TenantUtils.sanitizeTenantIdForSchema(tenantId ?: "public")
            TenantContext.setTenant(sanitizedTenantId)

            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }

    private fun checkIfTenantExistsInFirebase(tenantId: String):  Boolean {
        return try {
            firebaseAuth.tenantManager.getAuthForTenant(tenantId)
            true
        } catch (e: FirebaseAuthException) {
            false
        } catch (e: Exception) {
            logger.warn("Error checking if tenant exists: ${e.message}")
            false
        }
    }
}
