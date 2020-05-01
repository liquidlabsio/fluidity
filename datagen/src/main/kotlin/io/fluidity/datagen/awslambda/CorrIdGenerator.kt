import io.fluidity.datagen.Templates
import io.fluidity.datagen.Templates.*
import java.io.BufferedWriter
import java.io.File
import java.nio.charset.Charset
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
    val outputPath = "/single-path-correlation/logs/"

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

    var timestamp = System.currentTimeMillis() - Templates.DAY
    while (count++ < 10) {
        val constMap = mutableMapOf("CORR_ID" to randomUUID().toString(), "FROM_USER" to generator.getUser(generator.fromUserList), "TO_USER" to generator.getUser(generator.toUserList))

        val writer = File(outDir + "correlation.log").bufferedWriter(Charset.defaultCharset())

        // the set of templates represents a dataflow bound together by the correlationId
        templates.forEach { template -> generator.applyTemplates(writer, template, timestamp, constMap) }
        timestamp += 1000
    }
}


/**
 * Builds logic specific to building the single-dataflow-trace entities
 */
class CorrelationTraceGenerator {
    val templates = listOf(TsTemplate(), UUIDTemplate(), ConstTemplate(), TsWithOffset(),
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