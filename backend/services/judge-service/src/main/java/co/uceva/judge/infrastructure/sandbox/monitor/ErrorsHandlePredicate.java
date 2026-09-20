package co.uceva.judge.infrastructure.sandbox.monitor;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Colección de expresiones regulares y del predicado agregado utilizados para
 * detectar, a partir de la salida de error de un proceso, si este falló por un
 * error en tiempo de ejecución (excepciones, trazas de pila, fallos del
 * sistema, etc.) en los distintos lenguajes soportados por el judge.
 */
public final class ErrorsHandlePredicate {

    private ErrorsHandlePredicate() {}

    /**
     * Excepciones de Java (stack traces), por ejemplo:
     * Exception in thread "main" java.lang...
     * java.lang.NullPointerException: ...
     */
    private static final Pattern JAVA_EXCEPTION = Pattern.compile(
        "(?m)^\\s*(?:Exception in thread\\s+\"[^\"]+\"\\s+)?"
        + "(?:[\\w$]+\\.)*[\\w$]+"
        + "(?:Exception|Error|Throwable)"
        + "(?::|\\s|$)"
    );

    /**
     * Encadenamiento de excepciones de Java, por ejemplo:
     * Caused by: ...
     */
    private static final Pattern JAVA_CAUSED_BY = Pattern.compile(
        "(?m)^\\s*Caused by:\\s*(?:[\\w$]+\\.)*[\\w$]+"
        + "(?:Exception|Error|Throwable)\\b"
    );

    /**
     * Inicio de una traza de Python, por ejemplo:
     * Traceback (most recent call last):
     */
    private static final Pattern PYTHON_TRACEBACK = Pattern.compile(
        "(?m)^\\s*Traceback \\(most recent call last\\):\\s*$"
    );

    /**
     * Excepciones de Python, por ejemplo:
     * ValueError: ...
     */
    private static final Pattern PYTHON_EXCEPTION = Pattern.compile(
        "(?m)^\\s*(?:[\\w.]+\\.)?"
        + "(?:AssertionError|AttributeError|EOFError|"
        + "ImportError|IndexError|KeyError|NameError|"
        + "NotImplementedError|OSError|OverflowError|"
        + "RuntimeError|StopIteration|SyntaxError|"
        + "SystemError|TypeError|ValueError|ZeroDivisionError)"
        + "(?::|\\s|$)"
    );

    /**
     * Fallos de aserción y excepciones no capturadas en C / C++, por ejemplo:
     * Assertion `...' failed.
     * Assertion failed: ...
     * terminate called after throwing an instance of ...
     * what(): ...
     * uncaught exception ...
     */
    private static final Pattern CPP_FAILURE = Pattern.compile(
        "(?im)^\\s*(?:"
        + ".*\\bassertion\\b.*\\bfailed\\b.*"
        + "|.*\\bterminate called after throwing\\b.*"
        + "|.*\\buncaught exception\\b.*"
        + "|.*\\bwhat\\(\\):\\s*.+"
        + ")\\s*$"
    );

    /**
     * Diagnósticos de AddressSanitizer / LeakSanitizer / ThreadSanitizer, por ejemplo:
     * ERROR: AddressSanitizer: heap-buffer-overflow
     * ERROR: LeakSanitizer: detected memory leaks
     * WARNING: ThreadSanitizer: data race
     */
    private static final Pattern SANITIZER_FAILURE = Pattern.compile(
        "(?im)^\\s*(?:"
        + "==\\d+==\\s*)?"
        + "(?:ERROR|WARNING):\\s*"
        + "(?:AddressSanitizer|LeakSanitizer|"
        + "ThreadSanitizer|MemorySanitizer):"
        + ".*$"
    );

    /**
     * Diagnósticos de UndefinedBehaviorSanitizer y errores de runtime de C/C++, por ejemplo:
     * runtime error: signed integer overflow
     * runtime error: division by zero
     */
    private static final Pattern C_CPP_RUNTIME_ERROR = Pattern.compile(
        "(?im)^\\s*(?:"
        + ".*\\bruntime error:\\s*.+"
        + "|.*\\bUndefinedBehaviorSanitizer\\b.*"
        + "|.*\\bubsan:\\s*.+"
        + ")\\s*$"
    );

    /**
     * Fallos explícitos del sistema/runtime.
     * Se buscan mensajes completos o prefijos reconocibles,
     * no palabras aisladas como "fatal", "abort" o "killed".
     */
    private static final Pattern SYSTEM_FAILURE = Pattern.compile(
        "(?im)^\\s*(?:"
        + "segmentation fault(?:\\s*\\(core dumped\\))?"
        + "|segfault(?:\\s*\\(core dumped\\))?"
        + "|bus error(?:\\s*\\(core dumped\\))?"
        + "|illegal instruction(?:\\s*\\(core dumped\\))?"
        + "|floating point exception(?:\\s*\\(core dumped\\))?"
        + "|core dumped"
        + "|double free or corruption.*"
        + "|malloc\\(\\):.*"
        + "|free\\(\\):.*"
        + "|stack smashing detected.*"
        + "|stack overflow.*"
        + "|out of memory.*"
        + ")\\s*$"
    );

    /**
     * Predicado agregado que evalúa una línea de la salida de error contra
     * todos los patrones definidos (Java, Python, C/C++ y fallos del sistema)
     * y determina si dicha línea corresponde a un error de tiempo de ejecución.
     *
     * @param text Línea de la salida de error a evaluar.
     * @return {@code true} si la línea coincide con algún patrón de error en
     *         tiempo de ejecución; {@code false} en caso contrario o si el
     *         texto es nulo o está en blanco.
     */
    public static final Predicate<String> ERROR_RUNTIME = text -> {
        if (text == null || text.isBlank()) {
            return false;
        }

        return JAVA_EXCEPTION.matcher(text).find()
            || JAVA_CAUSED_BY.matcher(text).find()
            || PYTHON_TRACEBACK.matcher(text).find()
            || PYTHON_EXCEPTION.matcher(text).find()
            || CPP_FAILURE.matcher(text).find()
            || SANITIZER_FAILURE.matcher(text).find()
            || C_CPP_RUNTIME_ERROR.matcher(text).find()
            || SYSTEM_FAILURE.matcher(text).find();
    };
}
