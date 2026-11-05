package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.delay
import pe.edu.upeu.pharmamobil.domain.model.PoliticaReintentos
import pe.edu.upeu.pharmamobil.domain.model.PoliticaReintentos.MAX_INTENTOS
import pe.edu.upeu.pharmamobil.domain.model.ResultadoEnvio
import pe.edu.upeu.pharmamobil.domain.model.ResumenSync
import pe.edu.upeu.pharmamobil.domain.port.Telemetria
import pe.edu.upeu.pharmamobil.domain.repository.ColaSincronizacion
import pe.edu.upeu.pharmamobil.domain.repository.EnvioRemoto

/**
 * Sincronizacion refinada (Sesion 13): intentos por fila, espera creciente
 * y distincion entre fallo transitorio y permanente. Sin codigos HTTP.
 */
class SincronizarUseCase(
    private val local: ColaSincronizacion,
    private val remoto: EnvioRemoto,
    private val telemetria: Telemetria,
    /** Inyectable para que las pruebas no esperen de verdad. */
    private val esperar: suspend (Long) -> Unit = { delay(it) }
) {
    suspend operator fun invoke(): ResumenSync {
        var enviadas = 0
        var fallidas = 0
        val confirmados = mutableMapOf<Long, Long>()

        for (fila in local.pendientesPorEnviar(MAX_INTENTOS)) {
            when (val r = remoto.enviar(fila)) {
                is ResultadoEnvio.Exito -> {
                    local.confirmar(fila.id)
                    confirmados[fila.id] = r.idServidor
                    enviadas++
                }

                is ResultadoEnvio.Permanente -> {
                    local.marcarFallida(fila.id, r.motivo, MAX_INTENTOS)
                    fallidas++
                    telemetria.registrarEvento("sync_fila_fallida", mapOf("motivo" to r.motivo))
                }

                is ResultadoEnvio.Transitorio -> {
                    if (r.sinConexion) {
                        // Sin red no tiene sentido seguir ni bajar; la fila no gasta intentos.
                        return ResumenSync(enviadas, fallidas, local.contarPendientes(MAX_INTENTOS),
                            sinConexion = true, confirmados = confirmados)
                    }
                    local.sumarIntento(fila.id, r.motivo)
                    if (fila.intentos + 1 >= MAX_INTENTOS) {
                        fallidas++
                        telemetria.registrarEvento("sync_fila_agoto_intentos", mapOf("motivo" to r.motivo))
                    }
                    esperar(PoliticaReintentos.espera(fila.intentos))
                }
            }
        }

        // Solo despues de subir se baja: invertirlo borraria lo pendiente.
        local.conciliar(remoto.descargar())
        return ResumenSync(enviadas, fallidas, local.contarPendientes(MAX_INTENTOS), confirmados = confirmados)
    }
}
