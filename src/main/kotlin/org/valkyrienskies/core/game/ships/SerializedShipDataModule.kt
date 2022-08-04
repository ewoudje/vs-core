package org.valkyrienskies.core.game.ships

import dagger.Module
import dagger.Provides
import org.valkyrienskies.core.game.ChunkAllocator
import javax.inject.Singleton

@Module
class SerializedShipDataModule(
    @get:Provides @Singleton val queryableShipData: MutableQueryableShipDataServer,
    @get:Provides @Singleton val chunkAllocator: ChunkAllocator
)
