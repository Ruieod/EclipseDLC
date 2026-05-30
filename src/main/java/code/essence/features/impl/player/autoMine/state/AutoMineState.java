package code.essence.features.impl.player.autoMine.state;

/**
 * Состояния автомайна.
 *
 * @author nikitavodolaz
 * @since 11.02.2026
 */
public enum AutoMineState {

    /**
     * Ожидание шахты — парсим API, ждём когда время < 12 секунд.
     */
    WAITING_FOR_MINE,

    /**
     * Заходим на анархию — отправляем команду /anXXX.
     */
    JOINING_ANARCHY,

    /**
     * Ждём загрузки анархии после захода.
     */
    WAITING_ANARCHY_LOAD,

    /**
     * Отправляем /warp mine.
     */
    WARPING_TO_MINE,

    /**
     * Ждём телепортации после /warp mine (7 секунд).
     */
    WAITING_WARP,

    /**
     * Бежим к центру региона.
     */
    RUNNING_TO_CENTER,

    /**
     * Активная копка руды.
     */
    MINING,

    /**
     * Шахта закончилась (нет таргетов) — возвращаемся к ожиданию.
     */
    MINE_FINISHED,

    /**
     * Починка кирки: очистка инвентаря, покупка XP, бросок.
     */
    REPAIRING
}
