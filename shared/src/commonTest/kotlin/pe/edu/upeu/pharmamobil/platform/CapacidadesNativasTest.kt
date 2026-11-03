package pe.edu.upeu.pharmamobil.platform

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.model.textoParaCompartir
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Se ejecuta sobre el actual de la plataforma de prueba (JVM en
 * testAndroidHostTest, simulador en iosSimulatorArm64Test).
 */
class CapacidadesNativasTest {

    @Test
    fun formatoDeSolesUsaElSimboloPeruano() {
        assertTrue(formatearSoles(15.5).contains("S/"), formatearSoles(15.5))
    }

    @Test
    fun infoDispositivoNoVieneVacia() {
        assertTrue(infoDispositivo().sistema.isNotBlank())
    }

    @Test
    fun textoParaCompartirIncluyeNombrePrecioYStock() {
        val texto = Producto(1, "Paracetamol", 15.5, 0).textoParaCompartir { "S/ $it" }
        assertTrue("Paracetamol" in texto)
        assertTrue("S/ 15.5" in texto)
        assertTrue("Sin stock" in texto)
    }
}
