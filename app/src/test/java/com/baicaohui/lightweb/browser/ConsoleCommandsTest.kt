package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class ConsoleCommandsTest {

    @Test
    fun `jsString escapes quotes and backslashes`() {
        assertEquals("\"a\\\"b\\\\c\"", ConsoleCommands.jsString("a\"b\\c"))
    }

    @Test
    fun `jsString escapes newline and control chars`() {
        assertEquals("\"a\\nb\\tc\"", ConsoleCommands.jsString("a\nb\tc"))
    }

    @Test
    fun `evaluateExpression embeds escaped command`() {
        val expression = ConsoleCommands.evaluateExpression("1 + 1")
        assertEquals(true, expression.contains("eval(\"1 + 1\")"))
    }

    @Test
    fun `evaluateExpression returns stringified object result`() {
        val expression = ConsoleCommands.evaluateExpression("({a:1})")
        assertEquals(true, expression.contains("JSON.stringify(result,null,2)"))
        assertEquals(true, expression.contains("Error:"))
    }

    @Test
    fun `sourceExpression reads outer html`() {
        assertEquals("document.documentElement.outerHTML", ConsoleCommands.sourceExpression())
    }

    @Test
    fun `unescapeJsResult decodes json string`() {
        assertEquals("<html></html>", ConsoleCommands.unescapeJsResult("\"<html></html>\""))
    }

    @Test
    fun `unescapeJsResult falls back to raw when not json`() {
        assertEquals("plain", ConsoleCommands.unescapeJsResult("plain"))
    }
}
