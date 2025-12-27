package traversium.filestorageservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@RefreshScope
class WebClientConfig(
    @param:Value("\${trip-service.url:http://localhost:8080}")
    private val tripServiceUrl: String
) {

    @Bean
    fun tripServiceWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(tripServiceUrl)
            .build()
    }
}