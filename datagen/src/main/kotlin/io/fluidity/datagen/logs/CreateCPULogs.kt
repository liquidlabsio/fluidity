package io.fluidity.datagen.logs

import io.fluidity.datagen.DateUtil
import org.joda.time.format.DateTimeFormat
import java.io.FileOutputStream
import java.util.*

/**
 * Generate CPU log data
 */
fun main() {

    val prefix = "/Volumes/SSD2/logs/fluidity-logs/test-cpu-"
    val fos = FileOutputStream(prefix + Date().time + ".log")

    val dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm.SS")

    var time: Long = System.currentTimeMillis() - DateUtil.DAY * 7
    val minutes = ((System.currentTimeMillis() - time) / DateUtil.MINUTE) as Int

    for (i in 1 until minutes) {
        val cpu = i / minutes.toDouble() * 100.0 + 10 * Math.random()
        fos.write(String.format("%s INFO CPU:%d\n", dateTimeFormatter.print(time), java.lang.Double.valueOf(cpu).toInt()).toByteArray())
        time += DateUtil.MINUTE
    }
    fos.close()
}

