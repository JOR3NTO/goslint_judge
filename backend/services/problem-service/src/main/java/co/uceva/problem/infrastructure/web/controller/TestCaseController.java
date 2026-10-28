package co.uceva.problem.infrastructure.web.controller;

import co.uceva.problem.application.usecase.*;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.infrastructure.web.dto.*;
import co.uceva.problem.infrastructure.web.mapper.TestCaseWebMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST que maneja las solicitudes HTTP relacionadas con los Casos de Prueba.
 * Pertenece a la capa de Infraestructura y delega toda la lógica de negocio
 * a los puertos de entrada (casos de uso).
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/problems/test-cases")
public class TestCaseController {

    private final CreateTestCaseUseCase createTestCaseUseCase;
    private final CreateTestCaseBatchUseCase createTestCaseBatchUseCase;
    private final UpdateTestCaseUseCase updateTestCaseUseCase;
    private final ReorderTestCasesUseCase reorderTestCaseUseCase;
    private final DeleteTestCaseUseCase deleteTestCaseUseCase;
    private final DeleteTestCaseBatchUseCase deleteTestCaseBatchUseCase;
    private final GetAllTestCaseByProblemIdUseCase getAllTestCaseByProblemIdUseCase;
    private final GetAllSampleTestCasesByProblemIdUseCase getAllSampleTestCasesByProblemIdUseCase;
    private final GetTestCaseByIdUseCase getTestCaseByIdUseCase;

    /**
     * Inyección de dependencias para obtener los orquestadores de cada caso de uso.
     *
     * @param createTestCaseUseCase                 Caso de uso de creación de caso de prueba.
     * @param createTestCaseBatchUseCase            Caso de uso de creación masiva de casos de prueba.
     * @param updateTestCaseUseCase                 Caso de uso de actualización de caso de prueba.
     * @param reorderTestCaseUseCase                Caso de uso de reordenamiento de casos de prueba.
     * @param deleteTestCaseUseCase                 Caso de uso de eliminación de caso de prueba.
     * @param deleteTestCaseBatchUseCase            Caso de uso de eliminación masiva de casos de prueba.
     * @param getAllTestCaseByProblemIdUseCase      Caso de uso de consulta de casos por problema.
     * @param getAllSampleTestCasesByProblemIdUseCase Caso de uso de consulta de casos de ejemplo por problema.
     * @param getTestCaseByIdUseCase                Caso de uso de consulta de caso por ID.
     */
    public TestCaseController(
            CreateTestCaseUseCase createTestCaseUseCase,
            CreateTestCaseBatchUseCase createTestCaseBatchUseCase,
            UpdateTestCaseUseCase updateTestCaseUseCase,
            ReorderTestCasesUseCase reorderTestCaseUseCase,
            DeleteTestCaseUseCase deleteTestCaseUseCase,
            DeleteTestCaseBatchUseCase deleteTestCaseBatchUseCase,
            GetAllTestCaseByProblemIdUseCase getAllTestCaseByProblemIdUseCase,
            GetAllSampleTestCasesByProblemIdUseCase getAllSampleTestCasesByProblemIdUseCase,
            GetTestCaseByIdUseCase getTestCaseByIdUseCase) {
        this.createTestCaseUseCase = createTestCaseUseCase;
        this.createTestCaseBatchUseCase = createTestCaseBatchUseCase;
        this.updateTestCaseUseCase = updateTestCaseUseCase;
        this.reorderTestCaseUseCase = reorderTestCaseUseCase;
        this.deleteTestCaseUseCase = deleteTestCaseUseCase;
        this.deleteTestCaseBatchUseCase = deleteTestCaseBatchUseCase;
        this.getAllTestCaseByProblemIdUseCase = getAllTestCaseByProblemIdUseCase;
        this.getAllSampleTestCasesByProblemIdUseCase = getAllSampleTestCasesByProblemIdUseCase;
        this.getTestCaseByIdUseCase = getTestCaseByIdUseCase;
    }

    /**
     * Endpoint para crear un caso de prueba asociado a un problema.
     * <p>
     * Requiere rol {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @param problemId Identificador del problema asociado.
     * @param request   DTO con los datos del caso de prueba.
     * @return 201 CREATED con el caso de prueba creado.
     */
    @PostMapping("/{problemId}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<TestCaseResponseDTO> create(
            @PathVariable("problemId") UUID problemId,
            @RequestBody CreateTestCaseRequestDTO request) {
        // Convertir el DTO a comando de aplicación
        var command = TestCaseWebMapper.toCommand(problemId, request);
        // Delegar la creación al caso de uso
        TestCase created = createTestCaseUseCase.execute(command);
        // Construir y retornar la respuesta HTTP
        return ResponseEntity.status(HttpStatus.CREATED).body(TestCaseWebMapper.toResponse(created));
    }

    /**
     * Endpoint para obtener todos los casos de prueba de un problema.
     * <p>
     * Requiere rol {@code ADMIN}, {@code ORGANIZER} o {@code SERVICE}.
     * El rol {@code SERVICE} permite que el {@code judge-service} acceda a los
     * casos de prueba privados necesarios para evaluar envíos.
     * </p>
     *
     * @param problemId Identificador del problema.
     * @return 201 CREATED con la lista de casos de prueba.
     */
    @GetMapping("/{problemId}/all")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER','SERVICE')")
    public ResponseEntity<List<TestCaseResponseDTO>> getAll(@PathVariable("problemId") UUID problemId) {
        List<TestCase> testCases = getAllTestCaseByProblemIdUseCase.execute(problemId);
        List<TestCaseResponseDTO> testCasesResponce = testCases.stream()
                .map(TestCaseWebMapper::toResponse)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(testCasesResponce);
    }

    /**
     * Endpoint para obtener un caso de prueba por su identificador.
     * <p>
     * Requiere rol {@code ADMIN}, {@code ORGANIZER} o {@code SERVICE}.
     * El rol {@code SERVICE} permite que el {@code judge-service} consulte un
     * caso de prueba específico durante la evaluación de un envío.
     * </p>
     *
     * @param id Identificador del caso de prueba.
     * @return 201 CREATED con el caso de prueba encontrado.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER','SERVICE')")
    public ResponseEntity<TestCaseResponseDTO> getById(@PathVariable("id") UUID id) {
        TestCase testCase = getTestCaseByIdUseCase.execute(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(TestCaseWebMapper.toResponse(testCase));
    }

    /**
     * Endpoint para actualizar un caso de prueba.
     * <p>
     * Requiere rol {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @param problemId  Identificador del problema asociado.
     * @param testCaseId Identificador del caso de prueba a actualizar.
     * @param request    DTO con los nuevos datos.
     * @return 200 OK con el caso de prueba actualizado.
     */
    @PutMapping("/{problemId}/{testCaseId}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<TestCaseResponseDTO> update(
            @PathVariable("problemId") UUID problemId,
            @PathVariable("testCaseId") UUID testCaseId,
            @RequestBody UpdateTestCaseRequestDTO request) {
        // Convertir el DTO a comando de aplicación
        var command = TestCaseWebMapper.toCommand(testCaseId, request);
        // Delegar la actualización al caso de uso
        TestCase updated = updateTestCaseUseCase.execute(command);
        // Construir y retornar la respuesta HTTP
        return ResponseEntity.ok(TestCaseWebMapper.toResponse(updated));
    }

    /**
     * Endpoint para reordenar los casos de prueba de un problema.
     * <p>
     * Requiere rol {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @param problemId Identificador del problema.
     * @param request   DTO con la lista ordenada de identificadores.
     * @return 204 NO CONTENT.
     */
    @PutMapping("/{problemId}/reorder")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<Void> reorder(
            @PathVariable("problemId") UUID problemId,
            @RequestBody ReorderTestCasesRequestDTO request) {
        reorderTestCaseUseCase.execute(problemId, request.testCaseIdsInOrder());
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint para eliminar un caso de prueba.
     * <p>
     * Requiere rol {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @param problemId  Identificador del problema asociado.
     * @param testCaseId Identificador del caso de prueba a eliminar.
     * @return 204 NO CONTENT.
     */
    @DeleteMapping("/{problemId}/{testCaseId}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<Void> delete(
            @PathVariable("problemId") UUID problemId,
            @PathVariable("testCaseId") UUID testCaseId) {
        deleteTestCaseUseCase.execute(testCaseId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint para crear varios casos de prueba de forma masiva.
     * <p>
     * Requiere rol {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @param problemId Identificador del problema asociado.
     * @param request   DTO con la lista de casos de prueba.
     * @return 201 CREATED con la lista de casos de prueba creados.
     */
    @PostMapping("/{problemId}/batch")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<List<TestCaseResponseDTO>> createBatch(
            @PathVariable("problemId") UUID problemId,
            @RequestBody CreateTestCaseBatchRequestDTO request) {
        // Convertir el DTO a comando de aplicación
        var command = TestCaseWebMapper.toCreateBatchCommand(problemId, request);
        // Delegar la creación masiva al caso de uso
        List<TestCase> created = createTestCaseBatchUseCase.execute(command);
        // Construir y retornar la respuesta HTTP
        List<TestCaseResponseDTO> response = created.stream()
                .map(TestCaseWebMapper::toResponse)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint para eliminar varios casos de prueba de forma masiva.
     * <p>
     * Requiere rol {@code ADMIN} u {@code ORGANIZER}.
     * </p>
     *
     * @param request DTO con la lista de identificadores a eliminar.
     * @return 204 NO CONTENT.
     */
    @PostMapping("/batch-delete")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<Void> deleteBatch(@RequestBody DeleteTestCaseBatchRequestDTO request) {
        deleteTestCaseBatchUseCase.execute(request.testCaseIds());
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint público para obtener los casos de prueba de ejemplo
     * ({@code isSample = true}) de un problema.
     * <p>
     * Acceso público. No requiere autenticación.
     * </p>
     *
     * @param problemId Identificador del problema.
     * @return 200 OK con la lista de casos de prueba de ejemplo.
     */
    @GetMapping("/{problemId}/samples")
    public ResponseEntity<List<TestCaseResponseDTO>> getAllSamples(@PathVariable("problemId") UUID problemId) {
        List<TestCase> testCases = getAllSampleTestCasesByProblemIdUseCase.execute(problemId);
        List<TestCaseResponseDTO> testCasesResponse = testCases.stream()
                .map(TestCaseWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(testCasesResponse);
    }
}