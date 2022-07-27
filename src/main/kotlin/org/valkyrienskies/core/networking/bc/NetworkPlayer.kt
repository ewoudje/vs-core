package org.valkyrienskies.core.networking.bc

import io.netty.buffer.ByteBuf
import kotlinx.coroutines.CompletableDeferred
import org.valkyrienskies.core.game.IPlayer
import java.net.SocketAddress
import java.util.LinkedList
import java.util.Queue

data class NetworkPlayer(
    val address: SocketAddress
) {
    var player: IPlayer? = null
        private set
    var isConnected: Boolean = true
    val packets: Queue<ByteBuf> = LinkedList()
    var deferred: CompletableDeferred<ByteBuf>? = null
    var transport: PlayerTransport? = null

    fun identify(player: IPlayer) {
        this.player = player
    }
}
