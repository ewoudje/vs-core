package org.valkyrienskies.core.networking

import org.bouncycastle.tls.CertificateRequest
import org.bouncycastle.tls.TlsAuthentication
import org.bouncycastle.tls.TlsCredentials
import org.bouncycastle.tls.TlsServerCertificate
import org.bouncycastle.tls.crypto.impl.jcajce.JcaTlsCryptoProvider
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object Encryption {
    const val packetOverhead = 13

    val secureRandom = SecureRandom()
    val crypto = JcaTlsCryptoProvider().create(secureRandom)

    val clientAuth = object : TlsAuthentication {
        lateinit var cert: TlsServerCertificate

        override fun notifyServerCertificate(serverCertificate: TlsServerCertificate) {
            cert = serverCertificate
        }

        override fun getClientCredentials(certificateRequest: CertificateRequest): TlsCredentials? {
            return null
        }
    }

    fun generateAES128Key(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(128)
        return keyGenerator.generateKey()
    }
}
