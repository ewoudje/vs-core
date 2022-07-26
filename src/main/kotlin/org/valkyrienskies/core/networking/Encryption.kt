package org.valkyrienskies.core.networking

import org.bouncycastle.tls.crypto.impl.jcajce.JcaTlsCryptoProvider
import java.security.SecureRandom

object Encryption {
    val secureRandom = SecureRandom()
    val crypto = JcaTlsCryptoProvider().create(secureRandom)
    const val packetOverhead = 13
}
