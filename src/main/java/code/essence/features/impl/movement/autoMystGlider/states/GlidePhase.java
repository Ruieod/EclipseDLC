package code.essence.features.impl.movement.autoMystGlider.states;

/**
 * Фазы цикла AutoMystGlider:
 * FLYING → WAITING_REOPEN → LOOTING → HUB_TO_HOME → AT_HOME → HUB_TO_NEXT → WAITING_COORDS → FLYING ...
 */
public enum GlidePhase {
    FLYING,           // Летим к координатам + ноклип
    WAITING_REOPEN,   // В хабе, ждём открытия для лута
    LOOTING,          // На анархии, лутаем сундуки
    HUB_TO_HOME,      // В хабе, заходим на домашнюю анархию
    AT_HOME,          // На домашней анархии, скидываем ресы
    HUB_TO_NEXT,      // В хабе, ждём следующий ивент из API
    WAITING_COORDS    // На следующей анархии, ждём координаты в чате
}
