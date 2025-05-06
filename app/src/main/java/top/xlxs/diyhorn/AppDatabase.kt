package top.xlxs.diyhorn

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ButtonConfig::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun buttonConfigDao(): ButtonConfigDao
}
