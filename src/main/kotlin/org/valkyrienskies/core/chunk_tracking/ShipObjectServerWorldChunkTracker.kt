package org.valkyrienskies.core.chunk_tracking

import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.game.DimensionId
import org.valkyrienskies.core.game.IPlayer
import org.valkyrienskies.core.game.ships.ShipData
import org.valkyrienskies.core.game.ships.ShipObjectServerWorld
import java.util.SortedSet
import java.util.TreeSet
import java.util.function.LongFunction
import kotlin.math.min

class ShipObjectServerWorldChunkTracker(
    private val parent: ShipObjectServerWorld,
    var chunkWatchDistance: Double,
    var chunkUnwatchDistance: Double,
) {

    private val chunkToPlayersWatchingMap: Long2ObjectMap<MutableSet<IPlayer>> = Long2ObjectOpenHashMap()

    /**
     * Player -> Ship Id -> Number of chunks on that ship the player is tracking
     */
    internal val playersToShipsWatchingMap = HashMap<IPlayer, Object2IntMap<ShipData>>()

    /**
     * Ship Id -> Players watching
     */
    internal val shipsToPlayersWatchingMap = Long2ObjectOpenHashMap<MutableSet<IPlayer>>()

    /**
     * Players -> ships that they weren't watching before
     *
     * This gets cleared by [ShipObjectServerWorld] every tick
     */
    internal val playersToShipsNewlyWatchingMap = HashMap<IPlayer, MutableSet<ShipData>>()

    /**
     * Players -> ships that they are no longer watching
     *
     * This gets cleared by [ShipObjectServerWorld] every tick
     */
    internal val playersToShipsNoLongerWatchingMap = HashMap<IPlayer, MutableSet<ShipData>>()

    internal val shipsToLoad = HashSet<ShipData>()

    internal val shipsToUnload = HashSet<ShipData>()

    var chunkWatchTasks: SortedSet<ChunkWatchTask> = sortedSetOf()
        private set
    var chunkUnwatchTasks: SortedSet<ChunkUnwatchTask> = sortedSetOf()
        private set

    fun cleanDeletedShips() {
        for (ship in parent.deletedShipObjects) {
            playersToShipsWatchingMap.values.forEach { it.removeInt(ship) }
            shipsToPlayersWatchingMap.remove(ship.id)
            shipsToUnload.add(ship)
        }
    }

    fun updateTracking(players: Iterable<IPlayer>) {
        cleanDeletedShips()

        val newChunkWatchTasks = TreeSet<ChunkWatchTask>()
        val newChunkUnwatchTasks = TreeSet<ChunkUnwatchTask>()

        // Reuse these vector objects across iterations
        val tempVector0 = Vector3d()
        val tempVector1 = Vector3d()
        parent.queryableShipData.forEach { shipData ->
            val shipTransform = shipData.shipTransform

            shipData.shipActiveChunksSet.iterateChunkPos { chunkX, chunkZ ->
                val chunkPosInWorldCoordinates: Vector3dc = shipTransform.shipToWorldMatrix.transformPosition(
                    tempVector0.set(
                        ((chunkX shl 4) + 8).toDouble(),
                        127.0,
                        ((chunkZ shl 4) + 8).toDouble()
                    )
                )

                val newPlayersWatching: MutableList<IPlayer> = ArrayList()
                val newPlayersUnwatching: MutableList<IPlayer> = ArrayList()

                var minWatchingDistanceSq = Double.MAX_VALUE
                var minUnwatchingDistanceSq = Double.MAX_VALUE

                val playersWatchingChunk = getPlayersWatchingChunk(chunkX, chunkZ, shipData.chunkClaimDimension)

                for (player in players) {

                    val playerPositionInWorldCoordinates: Vector3dc = player.getPosition(tempVector1)
                    val displacementDistanceSq =
                        chunkPosInWorldCoordinates.distanceSquared(playerPositionInWorldCoordinates)

                    val isPlayerWatchingThisChunk = playersWatchingChunk.contains(player)

                    if (shipData.chunkClaimDimension != player.dimension) {
                        // if the chunk dimension is different from the player dimension, lets just ignore it for now
                        continue
                    }

                    if (displacementDistanceSq < chunkWatchDistance * chunkWatchDistance) {
                        if (!isPlayerWatchingThisChunk) {
                            // Watch this chunk
                            newPlayersWatching.add(player)
                            // Update [minWatchingDistanceSq]
                            minWatchingDistanceSq = min(minWatchingDistanceSq, displacementDistanceSq)
                        }
                    } else if (displacementDistanceSq > chunkUnwatchDistance * chunkUnwatchDistance) {
                        if (isPlayerWatchingThisChunk) {
                            // Unwatch this chunk
                            newPlayersUnwatching.add(player)
                            // Update [minUnwatchingDistanceSq]
                            minUnwatchingDistanceSq = min(minUnwatchingDistanceSq, displacementDistanceSq)
                        }
                    }
                }

                val chunkPosAsLong = IShipActiveChunksSet.chunkPosToLong(chunkX, chunkZ)
                // TODO distanceSqToClosestPlayer is for the watch and unwatch tasks incorrect
                // ( doesn't matter as long as we still do all of the watching in one tick though )
                if (newPlayersWatching.isNotEmpty()) {
                    val newChunkWatchTask = ChunkWatchTask(
                        chunkPosAsLong, shipData.chunkClaimDimension, newPlayersWatching, minWatchingDistanceSq
                    ) {
                        addWatchersToChunk(shipData, chunkPosAsLong, newPlayersWatching)
                    }
                    newChunkWatchTasks.add(newChunkWatchTask)
                }
                if (newPlayersUnwatching.isNotEmpty()) {
                    // If the all the currently watching players have unwatched, we should unload this chunk
                    val shouldUnloadChunk = playersWatchingChunk.size == newPlayersUnwatching.size
                    val newChunkUnwatchTask = ChunkUnwatchTask(
                        chunkPosAsLong, shipData.chunkClaimDimension,
                        newPlayersUnwatching, shouldUnloadChunk, minUnwatchingDistanceSq
                    ) {
                        removeWatchersFromChunk(shipData, chunkPosAsLong, newPlayersUnwatching)
                    }
                    newChunkUnwatchTasks.add(newChunkUnwatchTask)
                }
            }
        }

        chunkWatchTasks = newChunkWatchTasks
        chunkUnwatchTasks = newChunkUnwatchTasks
    }

    private fun isPlayerWatchingChunk(chunkX: Int, chunkZ: Int, dimensionId: DimensionId, player: IPlayer): Boolean {
        return getPlayersWatchingChunk(chunkX, chunkZ, dimensionId).contains(player)
    }

    // note dimensionId intentionally ignored for now
    fun getPlayersWatchingChunk(chunkX: Int, chunkZ: Int, dimensionId: DimensionId): Collection<IPlayer> {
        val chunkPosAsLong = IShipActiveChunksSet.chunkPosToLong(chunkX, chunkZ)
        return chunkToPlayersWatchingMap[chunkPosAsLong] ?: listOf()
    }

    fun getShipsPlayerIsWatching(player: IPlayer): Iterable<ShipData> {
        return (playersToShipsWatchingMap[player] ?: emptyMap()).keys
    }

    private fun addWatchersToChunk(shipData: ShipData, chunkPos: Long, newWatchingPlayers: Iterable<IPlayer>) {
        chunkToPlayersWatchingMap
            .computeIfAbsent(chunkPos) { HashSet() }
            .addAll(newWatchingPlayers)

        newWatchingPlayers.forEach { player ->
            playersToShipsWatchingMap
                .computeIfAbsent(player) { Object2IntOpenHashMap() }
                .compute(shipData) { _, count ->
                    if (count == null) {
                        // This ship was not already tracked by this [player]
                        playersToShipsNewlyWatchingMap
                            .computeIfAbsent(player) { HashSet() }
                            .add(shipData)

                        val playersWatchingShip = shipsToPlayersWatchingMap
                            .computeIfAbsent(shipData.id, LongFunction { HashSet() })

                        // If no players were watching this ship before, load it
                        if (playersWatchingShip.isEmpty()) {
                            shipsToLoad.add(shipData)
                        }

                        playersWatchingShip.add(player)

                        // return 1 (the player is now watching one chunk on this ship)
                        1
                    } else {
                        // other chunks on this ship were already tracked by this [player], increase the count by one
                        count + 1
                    }
                }
        }
    }

    private fun removeWatchersFromChunk(
        shipData: ShipData, chunkPos: Long, removedWatchingPlayers: Iterable<IPlayer>
    ) {
        // If there are players watching this chunk, remove the removedWatchingPlayers
        chunkToPlayersWatchingMap.computeIfPresent(chunkPos) { _, playersWatchingChunk ->
            playersWatchingChunk.removeAll(removedWatchingPlayers)
            // delete the entry in the chunkToPlayersWatchingMap if playersWatchingChunk is now empty
            playersWatchingChunk.ifEmpty { null }
        }

        removedWatchingPlayers.forEach { player ->
            playersToShipsWatchingMap[player]?.computeIfPresent(shipData) { _, count ->
                if (count == 1) {
                    // This was the last chunk on the ship that [player] was tracking
                    playersToShipsNoLongerWatchingMap
                        .computeIfAbsent(player) { HashSet() }
                        .add(shipData)

                    val playersWatchingShip = shipsToPlayersWatchingMap.get(shipData.id)!!
                    playersWatchingShip.remove(player)

                    // If no players are watching this ship anymore, unload it
                    if (playersWatchingShip.isEmpty()) {
                        shipsToUnload.add(shipData)
                    }

                    null // return null to remove the hashmap entry for this [shipId]
                } else {
                    // There are remaining chunks on the ship that [player] is tracking
                    count - 1
                }
            }
        }
    }
}
