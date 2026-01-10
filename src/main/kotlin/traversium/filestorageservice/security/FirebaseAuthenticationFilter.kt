package traversium.filestorageservice.security

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserRecord
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import traversium.commonmultitenancy.TenantContext
import traversium.commonmultitenancy.TenantUtils

@Component
class FirebaseAuthenticationFilter(
    private val firebaseAuth: FirebaseAuth,
) : OncePerRequestFilter(){

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val authHeader = request.getHeader("Authorization")
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response)
                return
            }
            val token = authHeader.removePrefix("Bearer ").trim()

            val decodedToken = firebaseAuth.verifyIdToken(token)
            val uid = decodedToken.uid

            SecurityContextHolder.getContext().authentication = TraversiumAuthentication(
                userRecordToPrincipal(firebaseAuth.getUser(uid)),
                null,
                emptyList(),
                token
            )

            filterChain.doFilter(request, response)
        } catch (ex: Exception) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.message)
        }
    }

    private fun userRecordToPrincipal(userRecord: UserRecord): TraversiumPrincipal = TraversiumPrincipal(
        userRecord.uid,
        userRecord.email,
        userRecord.photoUrl
    )

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)

        val exactPaths = setOf(
            "/rest/v1/trips/exists",
            "/swagger-ui.html"
        )
        val prefixPaths = listOf(
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",
            "/actuator/health",
            "/actuator/prometheus",
            "/actuator/",
        )

        return path in exactPaths || prefixPaths.any { path.startsWith(it) }
    }
}