package org.valkyrienskies.core.networking.encryption

import org.bouncycastle.tls.ProtocolVersion
import org.bouncycastle.tls.SecurityParameters
import org.bouncycastle.tls.TlsServerContext
import org.bouncycastle.tls.TlsSession
import org.bouncycastle.tls.crypto.TlsCrypto
import org.bouncycastle.tls.crypto.TlsNonceGenerator

object ServerContext : TlsServerContext {
    override fun getCrypto(): TlsCrypto {
        TODO("Not yet implemented")
    }

    override fun getNonceGenerator(): TlsNonceGenerator {
        TODO("Not yet implemented")
    }

    override fun getSecurityParameters(): SecurityParameters {
        TODO("Not yet implemented")
    }

    override fun getSecurityParametersConnection(): SecurityParameters {
        TODO("Not yet implemented")
    }

    override fun getSecurityParametersHandshake(): SecurityParameters {
        TODO("Not yet implemented")
    }

    override fun isServer(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getClientSupportedVersions(): Array<ProtocolVersion> {
        TODO("Not yet implemented")
    }

    override fun getClientVersion(): ProtocolVersion {
        TODO("Not yet implemented")
    }

    override fun getRSAPreMasterSecretVersion(): ProtocolVersion {
        TODO("Not yet implemented")
    }

    override fun getServerVersion(): ProtocolVersion {
        TODO("Not yet implemented")
    }

    override fun getResumableSession(): TlsSession {
        TODO("Not yet implemented")
    }

    override fun getSession(): TlsSession {
        TODO("Not yet implemented")
    }

    override fun getUserObject(): Any {
        TODO("Not yet implemented")
    }

    override fun setUserObject(userObject: Any?) {
        TODO("Not yet implemented")
    }

    override fun exportChannelBinding(channelBinding: Int): ByteArray {
        TODO("Not yet implemented")
    }

    override fun exportEarlyKeyingMaterial(asciiLabel: String?, context_value: ByteArray?, length: Int): ByteArray {
        TODO("Not yet implemented")
    }

    override fun exportKeyingMaterial(asciiLabel: String?, context_value: ByteArray?, length: Int): ByteArray {
        TODO("Not yet implemented")
    }
}
