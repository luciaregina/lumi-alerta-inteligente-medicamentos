package com.lumi

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lumi.data.Caregiver
import com.lumi.data.LumiDb
import com.lumi.data.Medication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DbTest {
    @Test
    fun marcaDoseAbateEstoqueUmaVez() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LumiDb::class.java).build()
        val dao = db.dao()
        val id = dao.insert(Medication(name = "Losartana", dose = "1 comprimido", times = listOf("08:00", "20:00"), stock = 10))

        dao.markTaken(id, "08:00")
        dao.markTaken(id, "08:00") // repetido não abate de novo

        val med = dao.med(id)!!
        assertEquals(9, med.stock)
        assertEquals(listOf("08:00", "20:00"), med.times)
        assertEquals(1, dao.takenOn(java.time.LocalDate.now().toString()).first().size)

        dao.setCaregiver(Caregiver(name = "Ana", relation = "Filha"))
        assertEquals("Ana", dao.caregiver().first()?.name)
        dao.removeCaregiver()
        assertNull(dao.caregiver().first())
        db.close()
    }
}
