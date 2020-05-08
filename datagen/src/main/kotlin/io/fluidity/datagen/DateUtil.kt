package io.fluidity.datagen

class DateUtil {
    companion object {
        // MILLIS_CALCULATIONS
        val SECOND = 1000
        val MINUTE = 60 * 1000.toLong()
        val TEN_MINS = 10 * MINUTE
        val HOUR = MINUTE * 60
        val DAY = HOUR * 24
        val WEEK = 7 * DAY
    }
}