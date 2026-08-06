package com.baicaohui.lightweb.browser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive

/** 控制台功能：构造注入页面的 JS 表达式，并解析 WebView 返回的 JSON 编码结果。 */
object ConsoleCommands {

    private val json = Json { ignoreUnknownKeys = true }

    fun jsString(value: String): String = buildString {
        append('"')
        value.forEach { c ->
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (c < ' ') {
                        append("\\u%04x".format(c.code))
                    } else {
                        append(c)
                    }
                }
            }
        }
        append('"')
    }

    /** 执行任意 JS 命令；字符串原样返回，对象/数组格式化为 JSON，异常返回 Error: ... */
    fun evaluateExpression(command: String): String =
        "(function(){try{var result=eval(${jsString(command)});" +
            "return (result===undefined?'undefined':" +
            "(typeof result==='string'?result:JSON.stringify(result,null,2)));" +
            "}catch(e){return 'Error: '+(e&&e.message?e.message:String(e));}})()"

    fun sourceExpression(): String = "document.documentElement.outerHTML"

    /** evaluateJavascript 的回调值是 JSON 编码字符串，这里解出真实文本。 */
    fun unescapeJsResult(raw: String): String = runCatching {
        json.parseToJsonElement(raw).jsonPrimitive.content
    }.getOrDefault(raw)
}
