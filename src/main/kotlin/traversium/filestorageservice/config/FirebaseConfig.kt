package traversium.filestorageservice.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

@Configuration
@RefreshScope
class FirebaseConfig {

    @Bean
    fun initializeFirebase(): FirebaseApp {
        val serviceAccount = FileInputStream("conf/traversium.json")
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        return FirebaseApp.getApps().firstOrNull() ?: FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firebaseAuth(
        initializeFirebase: FirebaseApp,
    ): FirebaseAuth = FirebaseAuth.getInstance(initializeFirebase)
}