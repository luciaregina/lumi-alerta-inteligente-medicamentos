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
}
