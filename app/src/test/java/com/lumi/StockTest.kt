package com.lumi

import com.lumi.stock.stockStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class StockTest {
    @Test
    fun seloDeStatus() {
        assertEquals("Acabando!", stockStatus(0))
        assertEquals("Acabando!", stockStatus(3))
        assertEquals("Atenção", stockStatus(4))
        assertEquals("Atenção", stockStatus(7))
        assertEquals("Tudo certo", stockStatus(8))
    }

    @Test
    fun alertaDeEstoqueBaixoComCincoDiasOuMenos() {
        fun med(stock: Int) = com.lumi.data.Medication(name = "X", dose = "1", times = listOf("08:00", "20:00"), stock = stock)
        assertEquals(true, med(11).lowStock)   // 5 dias
        assertEquals(false, med(12).lowStock)  // 6 dias
    }
}
