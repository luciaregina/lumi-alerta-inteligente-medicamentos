package com.lumi.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Entity
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dose: String,
    val times: List<String>, // "HH:MM", ordenados
    val stock: Int,
    val doctor: String = "",
) {
    val dosesPerDay get() = times.size
    val daysLeft get() = if (dosesPerDay == 0) Int.MAX_VALUE else stock / dosesPerDay
    val lowStock get() = daysLeft <= 5
}

/** Uma dose confirmada num dia. */
@Entity(primaryKeys = ["day", "medId", "time"])
data class DoseTaken(val day: String, val medId: Long, val time: String)

/** Vínculo paciente-responsável (no máximo um, id fixo). */
@Entity
data class Caregiver(@PrimaryKey val id: Int = 0, val name: String, val relation: String)

class Converters {
    @TypeConverter fun fromList(v: List<String>) = v.joinToString(",")
    @TypeConverter fun toList(v: String) = if (v.isEmpty()) emptyList() else v.split(",")
}

@Dao
interface LumiDao {
    @Query("SELECT * FROM Medication ORDER BY name")
    fun meds(): Flow<List<Medication>>

    @Query("SELECT * FROM Medication")
    suspend fun allMeds(): List<Medication>

    @Query("SELECT * FROM Medication WHERE id = :id")
    suspend fun med(id: Long): Medication?

    @Insert suspend fun insert(m: Medication): Long
    @Update suspend fun update(m: Medication)

    @Query("DELETE FROM Medication WHERE id = :id")
    suspend fun deleteMed(id: Long)

    @Query("SELECT * FROM DoseTaken WHERE day = :day")
    fun takenOn(day: String): Flow<List<DoseTaken>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaken(d: DoseTaken): Long

    @Query("UPDATE Medication SET stock = stock + :units WHERE id = :id")
    suspend fun addStock(id: Long, units: Int)

    @Query("UPDATE Medication SET stock = MAX(stock - 1, 0) WHERE id = :id")
    suspend fun decrementStock(id: Long)

    /** Marca a dose como tomada e abate 1 unidade do estoque (só na primeira vez). */
    @Transaction
    suspend fun markTaken(medId: Long, time: String, day: LocalDate = LocalDate.now()) {
        if (insertTaken(DoseTaken(day.toString(), medId, time)) != -1L) decrementStock(medId)
    }

    @Query("SELECT * FROM Caregiver WHERE id = 0")
    fun caregiver(): Flow<Caregiver?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCaregiver(c: Caregiver)

    @Query("DELETE FROM Caregiver")
    suspend fun removeCaregiver()
}

@Database(entities = [Medication::class, DoseTaken::class, Caregiver::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LumiDb : RoomDatabase() {
    abstract fun dao(): LumiDao

    companion object {
        @Volatile private var instance: LumiDb? = null

        fun get(context: Context): LumiDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, LumiDb::class.java, "lumi.db")
                .build().also { instance = it }
        }
    }
}
