package co.uceva.shared.domain;

/**
 * Enumeración que define los lenguajes de programación soportados por el sistema
 * para la evaluación automática de código fuente.
 * <p>
 * Cada valor representa un lenguaje que el motor de juzgamiento puede compilar
 * y ejecutar dentro de un contenedor Docker aislado.
 * </p>
 */
public enum ProgrammingLanguage {
    /** Lenguaje C (estándar C11 o compatible). */
    C,
    /** Lenguaje C++ (estándar C++17 o compatible). */
    CPP,
    /** Lenguaje Java (compatible con OpenJDK 17). */
    JAVA,
    /** Lenguaje Python (compatible con Python 3.11). */
    PYTHON
}
