package com.baicaohui.lightweb.util

import com.baicaohui.lightweb.data.db.BookmarkEntity
import com.baicaohui.lightweb.data.db.FolderEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkHtmlIOTest {

    @Test
    fun `export writes folders and links in netscape format`() {
        val html = BookmarkHtmlIO.export(
            folders = listOf(FolderEntity(id = 1, name = "技术")),
            bookmarks = listOf(
                BookmarkEntity(id = 1, folderId = 1, title = "示例", url = "https://example.com"),
                BookmarkEntity(id = 2, title = "首页", url = "https://home.com"),
            ),
        )
        assertTrue(html.contains("<DT><H3>技术</H3>"))
        assertTrue(html.contains("<A HREF=\"https://example.com\">示例</A>"))
        assertTrue(html.contains("<A HREF=\"https://home.com\">首页</A>"))
    }

    @Test
    fun `import parses folders and links with chinese titles`() {
        val html = """
            <!DOCTYPE NETSCAPE-Bookmark-file-1>
            <DL><p>
                <DT><H3>技术</H3>
                <DL><p>
                    <DT><A HREF="https://example.com">示例</A>
                </DL><p>
                <DT><A HREF="https://home.com">首页</A>
            </DL><p>
        """.trimIndent()
        val imported = BookmarkHtmlIO.import(html)
        assertEquals(2, imported.size)
        assertEquals("示例", imported[0].title)
        assertEquals("https://example.com", imported[0].url)
        assertEquals("技术", imported[0].folderName)
        assertEquals("首页", imported[1].title)
        assertEquals(null, imported[1].folderName)
    }

    @Test
    fun `import empty input returns empty list`() {
        assertEquals(emptyList<BookmarkHtmlIO.ImportedBookmark>(), BookmarkHtmlIO.import(""))
    }
}
