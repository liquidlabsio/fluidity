import java.io.BufferedWriter
import java.io.File
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


fun main(){

//    val output = "datagen/target/lambda-datagen/"
    val output = "/Volumes/SSD2/logs/fluidity/awslambda/"
//    val output = "/work/logs/"

    println("Running from:" + File(".").absolutePath)
    File(output).mkdirs();
    val templateDir = "test-data/awslambda/aws/lambda/";
    val templateSourceDir = "LogscapeDownloaders2/2020/04/22/"
    val templateFile = "downloaders.template"

    val template = File(templateDir + templateSourceDir + templateFile).readText(Charset.defaultCharset());

    val outDir = output + templateSourceDir
    File(outDir).mkdirs()
    val writer = File(outDir + "LogscapeDownloaders.log").bufferedWriter(Charset.defaultCharset());

    var count = 0;
    while (count++ < 10000) {
        writeToFsForAWhile(writer, template)
        Thread.sleep(1000)
    }
}

fun writeToFsForAWhile(writer: BufferedWriter, template: String) {
    var result = template;
    templates.forEach {template -> result = template.evaluate(result) }
    writer.write(result)
    writer.write("\n")
    writer.flush()
}

class TsTemplate() : Template("{{LOG_TIMESTAMP}}") {
    // format as 2020-04-22T16:10:38.119Z
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm.ss.SSS'Z'")
    override fun evaluate(document: String): String {
        return document.replace(matches, formatter.format(LocalDateTime.now()))
    }
}
class UUIDTemplate() : Template("{{UUID}}") {
    override fun evaluate(document: String): String {
        return document.replace(matches, UUID.randomUUID().toString())
    }
}

/**
 * User percentage of time - either sec/minute or minute/hour to shape data values
 */
open class ClockSkewRangeTemplate(matches: String) : RangeTemplate(matches) {
    override fun calcValue(rangeString: String, infuenceFactor: Double): Int {
        return super.calcValue(rangeString, minutePercent())
    }

    private fun minutePercent(): Double {
        val minute = LocalDateTime.now().minute
        return minute / 60.0 ;
    }
}

open class RangeTemplate(matches: String) : Template(matches) {
    override fun evaluate(document: String): String {
        try {
            if (!document.contains(matches)) return document
            // strip out each match and calc value
            // {{DURATION:100-200}}
            val rangeString: String = getTemplateItemVal(document)
            val calcValue = calcValue(rangeString, 0.0)
            var resultDocument = document
            while (resultDocument.contains(matches)) {
                resultDocument = replaceItem(resultDocument, calcValue)
            }
            return resultDocument
        } catch (e: Exception) {
            println("Template:" + matches + " Failed to process document")
            e.printStackTrace()
            return document
        }
    }

    private fun replaceItem(document: String, calcValue: Int): String {
        val from = document.indexOf(matches)
        val to = document.indexOf("}}", from) + 2;
        return document.substring(0, from) + calcValue + document.substring(to)
    }

    open fun calcValue(rangeString: String, infuenceFactor: Double): Int {
        val split = rangeString.split(":")
        val numberString = split[1].split("-")
        var lower = Integer.parseInt(numberString[0])
        val upper = Integer.parseInt(numberString[1])
        lower += ((upper - lower) * infuenceFactor).toInt()

        val range = upper - lower

        val value = range * Math.random();
        
        return (lower + value).toInt()
    }

    private fun getTemplateItemVal(document: String): String {
        val fromIndex = document.indexOf(matches);
        val toindex = document.indexOf("}}", fromIndex);
        return document.substring(fromIndex, toindex);
    }
}
open class Template(val matches: String) {
    open fun evaluate(document: String) : String {
        return document.replace(matches, "matched");
    }
    /*...*/ }

val templates = listOf<Template>(TsTemplate(), UUIDTemplate(), ClockSkewRangeTemplate("{{DURATION"),
        ClockSkewRangeTemplate("{{BILLED_DURATION"), RangeTemplate("{{MEM_SIZE"), RangeTemplate("{{MEM_USED"),
        ClockSkewRangeTemplate("{{INIT_DURATION"))


