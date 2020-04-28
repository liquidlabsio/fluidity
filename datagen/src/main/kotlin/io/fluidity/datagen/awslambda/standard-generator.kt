import java.io.BufferedWriter
import java.io.File
import java.lang.RuntimeException
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

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
        applyTemplates(writer, template)
        Thread.sleep(1000)
    }
}

/**
 * apply templates to generate a new record
 */
fun applyTemplates(writer: BufferedWriter, template: String) {
    var result = template;
    templates.forEach {template -> result = template.evaluate(result) }
    writer.write(result)
    writer.write("\n")
    writer.flush()
}

/**
 * Timestamp template
 */
class TsTemplate : Template("{{LOG_TIMESTAMP}}") {
    // format as 2020-04-22T16:10:38.119Z
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm.ss.SSS'Z'")
    override fun evaluate(document: String): String {
        return document.replace(matches, formatter.format(LocalDateTime.now()))
    }
}

/**
 * UUID generator template
 */
class UUIDTemplate : Template("{{UUID}}") {
    override fun evaluate(document: String): String {
        return document.replace(matches, UUID.randomUUID().toString())
    }
}

/**
 * Similar to RangeTemmplate bue applies a time based skew using the minute of the hour as a percent factor
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

/**
 * Range template that generates values between those defined in the template argument
 * i.e. {{SOMETHING:100-1000}}
 */
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

/**
 * Base class
 */
open class Template(val matches: String) {
    open fun evaluate(document: String) : String {
        return document.replace(matches, "matched");
    }
    /*...*/ }

val templates = listOf(TsTemplate(), UUIDTemplate(), ClockSkewRangeTemplate("{{DURATION"),
        ClockSkewRangeTemplate("{{BILLED_DURATION"), RangeTemplate("{{MEM_SIZE"), RangeTemplate("{{MEM_USED"),
        ClockSkewRangeTemplate("{{INIT_DURATION"))


