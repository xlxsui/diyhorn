package top.xlxs.diyhorn

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ButtonConfigDao {
    @Query("SELECT * FROM ButtonConfig")
    fun getAll(): List<ButtonConfig>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg configs: ButtonConfig)
}
