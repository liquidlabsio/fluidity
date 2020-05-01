package io.fluidity.datagen

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class Templates {
    companion object {
        // MILLIS_CALCULATIONS
        val SECOND = 1000
        val MINUTE = 60 * 1000.toLong()
        val TEN_MINS = 10 * MINUTE
        val HOUR = MINUTE * 60
        val DAY = HOUR * 24
        val WEEK = 7 * DAY
    }

    /**
     * Timestamp template
     * Default format as 2020-04-22T16:10:38.119Z
     */
    class TsTemplate : Template("{{LOG_TIMESTAMP}}") {

        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm.ss.SSS'Z'")

        override fun evaluate(document: String, timestamp: Long, inputConstant: MutableMap<String, String>): String {
            if (!document.contains(matches)) return document
            val time = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
            return document.replace(matches, formatter.format(time))
        }
    }

    /**
     * Increments the given timestamp by the value specified
     * i.e. {{LOG_TIMESTAMP:1000}}
     */
    class TsWithOffset : Template("{{LOG_TIMESTAMP:") {
        // format as 2020-04-22T16:10:38.119Z
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm.ss.SSS'Z'")

        override fun evaluate(document: String, timestamp: Long, inputConstant: MutableMap<String, String>): String {
            if (!document.contains(matches)) return document
            val templateItemVal = getTemplatedValue(document)
            val timeOffset = calcValue(templateItemVal)
            val newTime = timestamp + timeOffset
            val time = Instant.ofEpochMilli(newTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
            return document.replace("$templateItemVal}}", formatter.format(time))
        }

        private fun calcValue(templateItemVal: String): Int {
            val split = templateItemVal.split(":")
            return Integer.parseInt(split[1])
        }
    }


    /**
     * Optional values,  defined in the document
     * i.e. {{OPTIONAL:A|B|C}}
     */
    class OptionalTemplate : Template("{{OPTIONAL:") {
        override fun evaluate(document: String, timestamp: Long, inputConstant: MutableMap<String, String>): String {
            if (!document.contains(matches)) return document
            val templateItemVal = getTemplatedValue(document)
            val calcValue = calcValue(templateItemVal)
            var resultDocument = document
            while (resultDocument.contains(matches)) {
                resultDocument = replaceItem(resultDocument, calcValue)
            }
            return resultDocument
        }

        private fun calcValue(templateItemVal: String): String {
            val split = templateItemVal.split(":")
            val optionalValues = split[1].substring(0, split[1].length)
            val split1 = optionalValues.split("|")
            return split1[(split1.size * Math.random()).toInt()]
        }
    }

    /**
     * UUID generator template
     * i.e. {{UUID}}
     */
    class UUIDTemplate : Template("{{UUID}}") {
        override fun evaluate(document: String, timestamp: Long, inputConstant: MutableMap<String, String>): String {
            if (!document.contains(matches)) return document
            return document.replace(matches, UUID.randomUUID().toString())
        }
    }

    /**
     * Maps to provided constant input values. i.e. user_name provided by the script
     * i.e. {{CONST:USER}}
     */
    class ConstTemplate : Template("{{CONST:") {
        override fun evaluate(document: String, timestamp: Long, inputConstant: MutableMap<String, String>): String {

            var replaceDoc = document
            while (replaceDoc.contains(matches)) {
                val templateValue: String = getTemplatedValue(replaceDoc)
                val split = templateValue.split(":")
                val mappedReplacement: String = inputConstant[split[1]].toString()

                replaceDoc = replaceDoc.replace("$templateValue}}", mappedReplacement)
            }
            return replaceDoc
        }
    }


    /**
     * Similar to RangeTemplate but applies a time-based skew using the minute of the hour as a percent based weighting
     * i.e. {{SOMETHING:100-1000}}
     */
    open class ClockSkewRangeTemplate(matches: String) : RangeTemplate(matches) {
        override fun calcValue(rangeString: String, infuenceFactor: Double): Int {
            return super.calcValue(rangeString, minutePercent())
        }

        private fun minutePercent(): Double {
            val minute = LocalDateTime.now().minute
            return minute / 60.0
        }
    }

    /**
     * Range template generates values between those defined in the template document and NOT the config here
     * i.e. {{SOMETHING:100-1000}}
     */
    open class RangeTemplate(matches: String) : Template(matches) {

        override fun evaluate(document: String, timestamp: Long, inputConstant: MutableMap<String, String>): String {
            try {
                if (!document.contains(matches)) return document
                // strip out each match and calc value
                // {{DURATION:100-200}}
                val templatedValue: String = getTemplatedValue(document)
                val calcValue = calcValue(templatedValue, 0.0)
                var resultDocument = document
                while (resultDocument.contains(matches)) {
                    resultDocument = replaceItem(resultDocument, calcValue.toString())
                }
                return resultDocument
            } catch (e: Exception) {
                println("Template:$matches Failed to process document")
                e.printStackTrace()
                return document
            }
        }

        /**
         * Calculate range value. parse :100-200 part of the template and apply influence and random scaling
         */
        open fun calcValue(rangeString: String, infuenceFactor: Double): Int {
            val split = rangeString.split(":")
            val numberString = split[1].split("-")
            var lower = Integer.parseInt(numberString[0])
            val upper = Integer.parseInt(numberString[1])
            lower += ((upper - lower) * infuenceFactor).toInt()
            val range = upper - lower
            val value = range * Math.random()
            return (lower + value).toInt()
        }
    }

    /**
     * Base class
     */
    open class Template(val matches: String) {
        // support optional pre-processing
        init {
            preprocess()
        }

        /**
         * Preprocess arguments
         */
        open fun preprocess() {
        }

        /**
         * Extract template and paramters
         */
        open fun getTemplatedValue(document: String): String {
            val fromIndex = document.indexOf(matches)
            val toindex = document.indexOf("}}", fromIndex)
            return document.substring(fromIndex, toindex)
        }

        open fun replaceItem(document: String, templatedValue: String): String {
            val from = document.indexOf(matches)
            val to = document.indexOf("}}", from) + 2
            return document.substring(0, from) + templatedValue + document.substring(to)
        }

        /**
         * Apply template expansion
         */
        open fun evaluate(document: String, timestamp: Long, inputConstant: MutableMap<String, String>): String {
            return document.replace(matches, "matched")
        }
        /*...*/
    }
}