package co.uceva.contests_platform.contest.domain.model;

/**
 * Estado de la tabla de posiciones pública de un concurso.
 * <ul>
 *   <li>{@link #LIVE}: muestra los resultados al momento.</li>
 *   <li>{@link #FROZEN}: muestra los resultados al momento del congelamiento; lo activa una tarea
 *       programada a la hora de congelar.</li>
 *   <li>{@link #UNFROZEN}: el organizador liberó los resultados reales; no se vuelve a congelar.</li>
 * </ul>
 */
public enum ScoreboardStatus {
    LIVE,
    FROZEN,
    UNFROZEN
}
