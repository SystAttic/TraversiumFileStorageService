package traversium.filestorageservice.service

import com.google.firebase.auth.FirebaseAuth
import org.springframework.stereotype.Service

@Service
class FirebaseService(
    private val firebaseAuth: FirebaseAuth
) {

    fun extractUidFromToken(token: String): String {
        val decodedToken = firebaseAuth.verifyIdToken(token)
        return decodedToken.uid
    }
}