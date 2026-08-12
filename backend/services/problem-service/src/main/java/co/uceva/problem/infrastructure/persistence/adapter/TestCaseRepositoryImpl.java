package co.uceva.problem.infrastructure.persistence.adapter;

import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.TestCaseRepository;
import co.uceva.problem.infrastructure.mapper.TestCaseEntityMapper;
import co.uceva.problem.infrastructure.persistence.entity.TestCaseEntity;
import co.uceva.problem.infrastructure.persistence.repository.SpringDataTestCaseRepository;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptador de infraestructura que implementa el puerto de salida {@link TestCaseRepository}.
 * Traduce entre entidades de dominio y entidades JPA, delegando la persistencia a Spring Data.
 */
@Component
public class TestCaseRepositoryImpl implements TestCaseRepository {

    private final SpringDataTestCaseRepository springDataRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param springDataRepository Repositorio de Spring Data para casos de prueba.
     */
    public TestCaseRepositoryImpl(SpringDataTestCaseRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    /**
     * Guarda un caso de prueba convirtiéndolo a entidad JPA y retornando el dominio persistido.
     *
     * @param testCase Entidad de dominio a guardar.
     * @return Caso de prueba persistido.
     */
    @Override
    public TestCase save(TestCase testCase) {
        TestCaseEntity entity = TestCaseEntityMapper.toEntity(testCase);
        TestCaseEntity saved = springDataRepository.save(entity);
        return TestCaseEntityMapper.toDomain(saved);
    }

    /**
     * Guarda varios casos de prueba de forma masiva.
     *
     * @param testCases Lista de entidades de dominio a guardar.
     * @return Lista de casos de prueba persistidos.
     */
    @Override
    public List<TestCase> saveAll(List<TestCase> testCases) {
        List<TestCaseEntity> entities = testCases.stream()
                .map(TestCaseEntityMapper::toEntity)
                .collect(Collectors.toList());

        List<TestCaseEntity> savedEntities = springDataRepository.saveAll(entities);

        return savedEntities.stream()
                .map(TestCaseEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public Optional<TestCase> findById(UUID testCaseId) {
        return springDataRepository.findById(testCaseId)
                .map(TestCaseEntityMapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public List<TestCase> findAllByProblemId(UUID problemId) {
        return springDataRepository.findByProblemIdOrderByOrderIndexAsc(problemId).stream()
                .map(TestCaseEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * Recupera únicamente los casos de prueba de ejemplo asociados a un problema,
     * ordenados por su índice de orden ascendente.
     */
    @Override
    public List<TestCase> findAllSampleByProblemId(UUID problemId) {
        return springDataRepository.findByProblemIdAndIsSampleTrueOrderByOrderIndexAsc(problemId).stream()
                .map(TestCaseEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public void deleteById(UUID testCaseId) {
        springDataRepository.deleteById(testCaseId);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteAllById(List<UUID> testCaseIds) {
        if (testCaseIds != null && !testCaseIds.isEmpty()) {
            springDataRepository.deleteAllById(testCaseIds);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleteByProblemId(UUID problemId) {
        springDataRepository.deleteByProblemId(problemId);
    }

    /**
     * Actualiza los índices de orden de una lista de casos de prueba.
     *
     * @param newOrders Mapa con los nuevos índices de orden indexados por identificador.
     */
    @Override
    @Transactional
    public void updateOrderIndexes(Map<UUID, Integer> newOrders) {
        if (newOrders == null || newOrders.isEmpty()) {
            return;
        }
        // Recuperar las entidades a modificar
        List<TestCaseEntity> entities = springDataRepository.findAllById(newOrders.keySet());
        // Actualizar el índice de orden de cada entidad
        for (TestCaseEntity entity : entities) {
            Integer newOrderIndex = newOrders.get(entity.getId());
            if (newOrderIndex != null) {
                entity.setOrderIndex(newOrderIndex);
            }
        }
        // Persistir los cambios
        springDataRepository.saveAll(entities);
    }
}