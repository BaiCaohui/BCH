package com.baicaohui.lightweb.util

import com.baicaohui.lightweb.data.db.BookmarkEntity
import com.baicaohui.lightweb.data.db.FolderEntity

object BookmarkHtmlIO {

    data class ImportedBookmark(val title: String, val url: String, val folderName: String?)

    fun export(folders: List<FolderEntity>, bookmarks: List<BookmarkEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("<!DOCTYPE NETSCAPE-Bookmark-file-1>")
        sb.appendLine("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">")
        sb.appendLine("<TITLE>Bookmarks</TITLE>")
        sb.appendLine("<H1>Bookmarks</H1>")
        sb.appendLine("<DL><p>")
        val byFolder = bookmarks.groupBy { it.folderId }
        byFolder[null].orEmpty().sortedBy { it.orderIndex }.forEach { b ->
            sb.appendLine("    <DT><A HREF=\"${escape(b.url)}\">${escape(b.title)}</A>")
        }
        folders.forEach { folder ->
            sb.appendLine("    <DT><H3>${escape(folder.name)}</H3>")
            sb.appendLine("    <DL><p>")
            byFolder[folder.id].orEmpty().sortedBy { it.orderIndex }.forEach { b ->
                sb.appendLine("        <DT><A HREF=\"${escape(b.url)}\">${escape(b.title)}</A>")
            }
            sb.appendLine("    </DL><p>")
        }
        sb.appendLine("</DL><p>")
        return sb.toString()
    }

    fun import(html: String): List<ImportedBookmark> {
        val result = mutableListOf<ImportedBookmark>()
        var currentFolder: String? = null
        html.lineSequence().forEach { line ->
            if (line.contains("</DL>", ignoreCase = true)) {
                currentFolder = null
                return@forEach
            }
            val h3 = Regex("""<H3[^>]*>(.*?)</H3>""", RegexOption.IGNORE_CASE).find(line)
            if (h3 != null) {
                currentFolder = unescape(h3.groupValues[1].trim())
                return@forEach
            }
            val a = Regex("""<A\s+HREF="([^"]*)"[^>]*>(.*?)</A>""", RegexOption.IGNORE_CASE).find(line)
            if (a != null) {
                result += ImportedBookmark(
                    title = unescape(a.groupValues[2].trim()),
                    url = a.groupValues[1],
                    folderName = currentFolder,
                )
            }
        }
        return result
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun unescape(s: String): String = s
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
}
