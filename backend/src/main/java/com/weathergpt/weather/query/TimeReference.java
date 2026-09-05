package com.weathergpt.weather.query;

/**
 * Deliberately limited set of time references understood by the
 * deterministic query interpreter.
 */
public enum TimeReference {

    /** "right now" / "currently" */
    NOW,

    /** "today" / "tonight" */
    TODAY,

    /** "tomorrow" */
    TOMORROW,

    /** "the next day" — behaves exactly like {@link #TOMORROW} */
    NEXT_DAY,

    /** "this week" */
    THIS_WEEK,

    /** "this weekend" */
    THIS_WEEKEND,

    /** A time range that is not supported yet (e.g. historical queries). */
    UNSUPPORTED
}
