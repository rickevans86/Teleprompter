package dev.telepromptor.data

import android.content.Context
import androidx.room.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Entity
data class SavedScript(@PrimaryKey val id: String, val title: String, val text: String)

@Dao
interface Scripts {
    @Query("SELECT * FROM SavedScript ORDER BY title COLLATE NOCASE")
    fun observe(): Flow<List<SavedScript>>

    @Upsert
    suspend fun save(script: SavedScript)

    @Delete
    suspend fun delete(script: SavedScript)
}

@Database(entities = [SavedScript::class], version = 1, exportSchema = false)
abstract class ScriptDatabase : RoomDatabase() {
    abstract fun scripts(): Scripts
}

@Module
@InstallIn(SingletonComponent::class)
object ScriptModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): ScriptDatabase =
        Room.databaseBuilder(context, ScriptDatabase::class.java, "scripts.db").build()

    @Provides
    fun scripts(database: ScriptDatabase): Scripts = database.scripts()
}
