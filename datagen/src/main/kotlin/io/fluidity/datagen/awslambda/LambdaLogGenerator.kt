import io.fluidity.datagen.Templates.*
import java.io.BufferedWriter
import java.io.File
import java.nio.charset.Charset

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

//    val output = "datagen/target/lambda-datagen/"
    val output = "/Volumes/SSD2/logs/fluidity/awslambda/"
//    val output = "/work/logs/"

    println("Running from:" + File(".").absolutePath)
    File(output).mkdirs()
    val templateDir = "test-data/awslambda/aws/lambda/"
    val templateSourceDir = "LogscapeDownloaders2/2020/04/22/"
    val templateFile = "downloaders.template"

    val template = File(templateDir + templateSourceDir + templateFile).readText(Charset.defaultCharset())

    val outDir = output + templateSourceDir
    File(outDir).mkdirs()
    val writer = File(outDir + "LogscapeDownloaders.log").bufferedWriter(Charset.defaultCharset())

    var count = 0
    val generator = LambdaLogGenerator()
    while (count++ < 10000) {
        generator.applyTemplates(writer, template)
        Thread.sleep(1000)
    }
}

class LambdaLogGenerator {

    /**
     * apply templates to generate a new record
     */
    fun applyTemplates(writer: BufferedWriter, template: String) {
        var result = template
        templates.forEach { template -> result = template.evaluate(result, System.currentTimeMillis(), mutableMapOf()) }
        writer.write(result)
        writer.write("\n")
        writer.flush()
    }


    private val templates = listOf(TsTemplate(), UUIDTemplate(), ClockSkewRangeTemplate("{{DURATION:"),
            ClockSkewRangeTemplate("{{BILLED_DURATION:"), RangeTemplate("{{MEM_SIZE:"), RangeTemplate("{{MEM_USED:"),
            ClockSkewRangeTemplate("{{INIT_DURATION:"))


}