package com.scazzumvivendi.seento

import android.app.Application
import androidx.room.Room
import com.scazzumvivendi.seento.data.local.SeentoDatabase
import com.scazzumvivendi.seento.data.local.MIGRATION_1_2
import com.scazzumvivendi.seento.data.importer.ImportPlaylistUseCase
import com.scazzumvivendi.seento.data.parser.M3uParser
import com.scazzumvivendi.seento.data.repository.RoomPlaylistRepository
import com.scazzumvivendi.seento.domain.repository.PlaylistRepository

class SeentoApplication : Application() {

    val database: SeentoDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            SeentoDatabase::class.java,
            "seento.db"
        ).addMigrations(MIGRATION_1_2).build()
    }

    val playlistRepository: PlaylistRepository by lazy {
        RoomPlaylistRepository(database)
    }

    val importPlaylistUseCase: ImportPlaylistUseCase by lazy {
        ImportPlaylistUseCase(
            parser = M3uParser(),
            repository = playlistRepository
        )
    }
}
