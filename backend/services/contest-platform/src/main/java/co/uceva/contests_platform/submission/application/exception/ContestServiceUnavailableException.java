package co.uceva.contests_platform.submission.application.exception;

/**
 * Excepción lanzada cuando no se pudo obtener información de los datos de {@code contest}
 * por un fallo inesperado al consultarlos.
 * <p>
 * Es distinta de un 404: un recurso inexistente es una respuesta válida, mientras
 * que esta excepción significa que no se pudo decidir si el envío es elegible. Se
 * traduce en un 503 para que el cliente pueda reintentar más tarde.
 * </p>
 */
public class ContestServiceUnavailableException extends RuntimeException {

    /**
     * @param reason Descripción del fallo.
     * @param cause  Excepción original.
     */
    public ContestServiceUnavailableException(String reason, Throwable cause) {
        super("No se pudo consultar los datos del concurso: " + reason, cause);
    }
}
