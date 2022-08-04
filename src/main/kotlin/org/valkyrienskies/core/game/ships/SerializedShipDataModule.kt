package org.valkyrienskies.core.game.ships

import dagger.Module
import dagger.Provides
import org.valkyrienskies.core.game.ChunkAllocator
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.annotation.AnnotationRetention.BINARY

@Retention(BINARY)
@Qualifier
internal annotation class SavedShipData

@Module
class SerializedShipDataModule(
    @get:Provides @Singleton @SavedShipData val queryableShipData: MutableQueryableShipDataServer,
    @get:Provides @Singleton val chunkAllocator: ChunkAllocator
)
