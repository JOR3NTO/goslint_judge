package co.uceva.judge.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.uceva.judge.application.usecase.GetMonitorLimitsUseCase;
import co.uceva.judge.application.usecase.UpdateMonitorLimitsUseCase;
import co.uceva.judge.infrastructure.web.dto.MonitorLimitsDTO;
import co.uceva.judge.infrastructure.web.mapper.MonitorLimitsWebMapper;
import jakarta.validation.Valid;

/**
 * Endpoints para consultar y modificar los límites de los monitores del
 * sandbox. Ambos son exclusivos del administrador: un límite mal puesto
 * cambia el veredicto de todos los envíos.
 */
@RestController
@RequestMapping("/api/v1/judge/monitor-limits")
@PreAuthorize("hasRole('ADMIN')")
public class MonitorLimitsController {

    private final GetMonitorLimitsUseCase getMonitorLimitsUseCase;
    private final UpdateMonitorLimitsUseCase updateMonitorLimitsUseCase;

    /**
     * @param getMonitorLimitsUseCase    Caso de uso de consulta.
     * @param updateMonitorLimitsUseCase Caso de uso de modificación.
     */
    public MonitorLimitsController(GetMonitorLimitsUseCase getMonitorLimitsUseCase,
            UpdateMonitorLimitsUseCase updateMonitorLimitsUseCase) {
        this.getMonitorLimitsUseCase = getMonitorLimitsUseCase;
        this.updateMonitorLimitsUseCase = updateMonitorLimitsUseCase;
    }

    /**
     * @return Los límites vigentes de los monitores.
     */
    @GetMapping
    public ResponseEntity<MonitorLimitsDTO> get() {
        return ResponseEntity.ok(MonitorLimitsWebMapper.toDto(getMonitorLimitsUseCase.execute()));
    }

    /**
     * Reemplaza los límites de los monitores. Solo aplica a las evaluaciones que
     * empiecen después y no sobrevive a un reinicio del servicio.
     *
     * @param request Nuevos valores de los cuatro límites.
     * @return Los límites que quedaron vigentes.
     */
    @PutMapping
    public ResponseEntity<MonitorLimitsDTO> update(@Valid @RequestBody MonitorLimitsDTO request) {
        return ResponseEntity.ok(MonitorLimitsWebMapper.toDto(
                updateMonitorLimitsUseCase.execute(MonitorLimitsWebMapper.toDomain(request))));
    }
}
