# Valkyrien Skies Networking System

Valkyrien Skies has two main networking systems - the basic networking `Packet`
that
uses and operates with raw Netty `ByteBuf`s, and the `SimplePacket` system
implemented on top of that basic system that makes it easier to use.

## `SimplePacket`

To use `SimplePacket`, first create your own class implementing it:

```kt
data class MyPacket(
    val field1: String,
    val field2: Int
) : SimplePacket
```

Then register it in `Packets` (or anywhere, just make sure all packets are
registered in the same order on the client and server)

```kt
init {
    MyPacket::class.register()
}
```

You can also directly implement handlers in this class if you wish:

```kt
data class MyPacket(
    val field1: String,
    val field2: Int
) : SimplePacket {
    override fun receivedByClient() {}

    override fun receivedByServer(player: IPlayer) {}
}
```

To register a handler, simply do:

```kt
MyPacket::class.registerServerHandler { packet, player ->
    // do stuff
}
```

> Remember: the handler doesn't run on the game thread.








