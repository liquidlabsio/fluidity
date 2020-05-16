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

import io.fluidity.datagen.DateUtil
import io.fluidity.datagen.Templates.*
import java.io.BufferedWriter
import java.io.File
import java.nio.charset.Charset
import java.util.*
import java.util.UUID.randomUUID

/**
 * Configure:
 * 1. the output path
 * 2. source template location and attributes
 * 3. duration
 * 4. interval
 * 5. template parameters
 *
 * Then run to generate data.
 */
fun main() {

//    val outputRoot = "datagen/target/lambda-datagen/"
    val outputRoot = "/Volumes/SSD2/logs/fluidity/"
    val outputPath = "/correlation-model-1/logs/"

    println("Running from:" + File(".").absolutePath)
    val templateDir = File("test-data/dataflow-models/single-event-stream/")

    val templateFiles = templateDir.listFiles()
    val templates = templateFiles.map { template ->
        template.readText(Charset.defaultCharset())
    }

    val outDir = outputRoot + outputPath
    File(outDir).mkdirs()
    var count = 0
    val generator = CorrelationTraceGenerator()

    var timestamp = System.currentTimeMillis() - (DateUtil.HOUR * 6)
    while (count++ < 1000) {
        val constMap = mutableMapOf("CORR_ID" to randomUUID().toString(), "FROM_USER" to generator.getUser(generator.fromUserList), "TO_USER" to generator.getUser(generator.toUserList))

        var currentTimeStamp = timestamp
        var outfileNumber = 0

        // the set of templates represents a dataflow bound together by the correlationId
        templates.forEach { template ->
            run {
                val fl = File(outDir + "corr-" + count + "-" + outfileNumber++ + ".log")
                fl.bufferedWriter(Charset.defaultCharset()).use { writer ->
                    generator.applyTemplates(writer, template, currentTimeStamp, constMap)
                    // add latency between each step
                    currentTimeStamp += 100

                    // slow transaction every now and then
                    if (count % 1000 == 0) {
                        currentTimeStamp += 5000
                    }
                }
                fl.setLastModified(currentTimeStamp)
            }
        }
        timestamp += 20000

        if (count % 1000 == 0) {
            println("Count:" + count + " Time:" + Date(timestamp))
        }
    }
}


/**
 * Builds logic specific to building the single-dataflow-trace entities
 */
class CorrelationTraceGenerator {
    val templates = listOf(TsTemplate(), UUIDTemplate(), ConstTemplate(), TsWithOffset(), OptionalTemplate(),
            // Lambda metrics
            ClockSkewRangeTemplate("{{DURATION"), ClockSkewRangeTemplate("{{BILLED_DURATION"), RangeTemplate("{{MEM_SIZE"),
            RangeTemplate("{{MEM_USED"), ClockSkewRangeTemplate("{{INIT_DURATION"))

    val fromUserList = mutableListOf("JohnBarnes")
    val toUserList = mutableListOf("AliceBarnes")


    /**
     * apply templates to generate a new record
     */
    fun applyTemplates(writer: BufferedWriter, template: String, timestamp: Long, constMap: MutableMap<String, String>) {
        var result = template
        templates.forEach { template -> result = template.evaluate(result, timestamp, constMap) }
        writer.write(result)
        writer.write("\n")
        writer.flush()
    }

    /**
     * Populate distinct sets of users
     */
    init {
        // generate lots of usersnames
        val firstNames = listOf("Alistair", "Alice", "Barry", "Briana", "Charles", "Chelsea", "Darren", "Delia", "Edgebert", "Ellie", "Frank", "Fiona", "Gary", "Gerty", "Harry", "Hali",
                "Ian", "Isla", "John", "Jenny", "Kalid", "Karen", "Larry", "Lisa", "Matt", "Mona", "Neil", "Nena", "Ollie", "Olga", "Peter", "Poppie", "Quitin", "Qara", "Robert", "Rosie",
                "Steve", "Selma", "Trev", "Theresa", "Ulur", "Urlsa", "Viktor", "Vivian", "Wayna", "Wendy", "Xian", "Xienne", "Yaan", "Yeva", "Zoran", "Zelda")
        val surnameNames1 = listOf("Alba", "Alli", "Bab", "Bibbi", "Caca", "Cord", "Deed", "Deb", "Effi", "Ell", "Gaag", "Gip", "Hoop", "Hope", "Igh", "Illi", "Johns", "Jeffry", "Keels", "Kelo",
                "Looper", "Lad", "Madden", "Moon", "Nall", "Nova", "Opo", "Olla", "Pillik", "Peterson", "Quora", "Quord", "Roberts", "Robbins", "Stevens", "Stella", "Trevors", "Till", "Ulster",
                "Uggo", "Vaan", "Vivo", "Wayne", "Woob", "Xoon", "Xoom", "Yaan", "Yeen", "Zobo", "Zorg")
        val surnameNames2 = listOf("Albo", "Agg", "Boon", "Been", "Chee", "Cho", "Dho", "Dee", "Ho", "Hi", "Jen", "Jop", "Kip", "Kop", "Liw", "Lee", "Mon", "Min", "Nob", "Nib", "Opp", "Ooo",
                "Qor", "Qbb", "Ruu", "Rii", "Sii", "Sil", "Tiu", "Tam", "Uuu", "Uli", "Vaa", "Vbb", "Wow", "Wee", "Xaa", "Xbb", "Yaa", "Ybb", "Zaa", "Zbb")

        firstNames.forEach { name -> surnameNames1.forEach { surname -> fromUserList.add(name + surname) } }
        firstNames.forEach { name -> surnameNames2.forEach { surname -> toUserList.add(name + surname) } }
    }


    /**
     * Get random user from the list
     */
    fun getUser(userList: List<String>): String {
        return userList[(userList.size * Math.random()).toInt()]
    }
}