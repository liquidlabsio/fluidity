/*
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package io.fluidity.datagen.logs

import io.fluidity.datagen.DateUtil
import org.joda.time.format.DateTimeFormat
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * Generate CPU log data
 */
fun main() {

    val prefix = "/Volumes/SSD2/logs/fluidity/cpu-logs/test-cpu-"
    val name = prefix + Date().time + ".log"
    File(name).parentFile.mkdirs()
    val fos = FileOutputStream(name)

    val dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm.SS")

    var time: Long = System.currentTimeMillis() - DateUtil.DAY * 7
    val minutes = ((System.currentTimeMillis() - time) / DateUtil.MINUTE)

    for (i in 1 until minutes) {
        val cpu = i / minutes.toDouble() * 100.0 + 10 * Math.random()
        fos.write(String.format("%s INFO CPU:%d\n", dateTimeFormatter.print(time), java.lang.Double.valueOf(cpu).toInt()).toByteArray())
        time += DateUtil.MINUTE
    }
    fos.close()
}

