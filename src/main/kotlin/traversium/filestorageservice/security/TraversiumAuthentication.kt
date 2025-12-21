package traversium.filestorageservice.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import java.security.Principal

data class TraversiumAuthentication(
    val principal: Principal,
    private val details: Any?,
    private val authorities: Collection<GrantedAuthority>,
    val token: String?) : AbstractAuthenticationToken(authorities) {

    override fun isAuthenticated(): Boolean = true

    override fun getCredentials(): Any? = token

    override fun getPrincipal(): Any = principal
}