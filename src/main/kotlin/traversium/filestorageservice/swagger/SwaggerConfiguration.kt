package traversium.filestorageservice.swagger

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * @author Maja Razinger
 */
@Configuration
class SwaggerConfiguration {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(Info().title("Traversium Social Service API").version("v1"))
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
            .components(
                Components().addSecuritySchemes("bearerAuth",
                                                SecurityScheme()
                                                    .type(SecurityScheme.Type.HTTP)
                                                    .scheme("bearer")
                                                    .bearerFormat("JWT")
                )
            )
    }
}