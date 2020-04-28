import com.fasterxml.jackson.databind.ObjectMapper
import net.jpountz.lz4.LZ4FrameOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.WebTarget
import javax.ws.rs.sse.InboundSseEvent
import javax.ws.rs.sse.SseEventSource


fun main(){

//    val output = "datagen/target/wikimedia"
//    val output = "/Volumes/SSD2/logs/fluidity/wikimedia/"
    val output = "/work/logs/"

    File(output).mkdirs();

    while (true) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");
        val fileName = output + "edit-stream-" + formatter.format(LocalDateTime.now()) + ".log";

        val wikiMediaUrl = "https://stream.wikimedia.org/v2/stream/recentchange"
        val client = ClientBuilder.newClient()
        val target = client.target(wikiMediaUrl)
        applyTemplates(fileName, target, client)
    }

}

private fun applyTemplates(fileName: String, target: WebTarget?, client: Client) {
    println("Writing to " + File(fileName).absolutePath)

    var count = 0
    val objectMapper = ObjectMapper()

    val out = LZ4FrameOutputStream(FileOutputStream(File(fileName + ".lz4")), LZ4FrameOutputStream.BLOCKSIZE.SIZE_1MB)

    out.bufferedWriter().use { out ->
        try {
            val eventSource: SseEventSource = SseEventSource.target(target).build()
            eventSource.use { eventSource: SseEventSource ->
                eventSource.register { inboundSseEvent: InboundSseEvent ->
                    val readData = inboundSseEvent.readData()
                    if (readData.isNotEmpty()) {
                        val readValue = objectMapper.readValue(readData, HashMap::class.java)
                        out.write(readData)
                        out.newLine()
                    }
                    print("," + count++)
                    if (count % 100 == 0) println()
                }
                eventSource.open()

                //Consume events for one minute
                Thread.sleep(5 * 60 * 1000.toLong())
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
    client.close()
}
    