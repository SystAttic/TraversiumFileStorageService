package traversium.filestorageservice.security

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
class TenantFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val tenantId = request.getHeader("X-Tenant-Id")
            val sanitizedTenantId = TenantUtils.sanitizeTenantIdForSchema(tenantId ?: "public")
            TenantContext.setTenant(sanitizedTenantId)

            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }
}
