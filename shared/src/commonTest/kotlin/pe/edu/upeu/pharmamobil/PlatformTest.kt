package pe.edu.upeu.pharmamobil

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformTest {

    @Test
    fun laPlataformaTieneNombre() {
        assertTrue(getPlatform().name.isNotBlank())
    }
}
