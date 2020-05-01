package io.fluidity.datagen

import io.fluidity.datagen.Templates.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TemplatesTest {

    @Test
    fun timeStampWithOffsetWorks() {
        val template = TsWithOffset()
        val evaluate = template.evaluate("value {{LOG_TIMESTAMP:100}}", System.currentTimeMillis(), mutableMapOf())
        assert(!evaluate.contains("LOG_TIMESTAMP"))
    }

    @Test
    fun multiConstWorks() {
        val template = ConstTemplate()
        val map = mutableMapOf("YOYO" to "yoyoValue", "BOBO" to "boboValue")
        val evaluate = template.evaluate("value {{CONST:YOYO}} {{CONST:BOBO}}", System.currentTimeMillis(), map)
        assert(evaluate.contains("yoyoValue"))
        assert(evaluate.contains("boboValue"))
    }

    @Test
    fun constWorks() {
        val template = ConstTemplate()
        val evaluate = template.evaluate("value {{CONST:YOYO}}", System.currentTimeMillis(), mutableMapOf("YOYO" to "newValue"))
        assert(evaluate.contains("newValue"))
    }


    @Test
    fun rangeWorks() {
        val template = RangeTemplate("{{SOMETHING:")
        val evaluate = template.evaluate("value {{SOMETHING:50-52}}", System.currentTimeMillis(), mutableMapOf())
        assert(evaluate.contains("50") || evaluate.contains("51") || evaluate.contains("52"))
    }

    @Test
    fun optionalWorks() {
        val template = OptionalTemplate()
        val evaluate = template.evaluate("this is {{OPTIONAL:true|false}}", System.currentTimeMillis(), mutableMapOf())
        assert(evaluate.contains("true") || evaluate.contains("false"))
    }

}