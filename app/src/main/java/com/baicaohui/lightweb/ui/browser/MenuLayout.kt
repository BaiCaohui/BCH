package com.baicaohui.lightweb.ui.browser

fun menuPageCount(itemCount: Int, pageSize: Int): Int =
    if (itemCount == 0) 0 else (itemCount + pageSize - 1) / pageSize

fun menuColumnsForWidth(
    usableWidth: Float,
    minItemWidth: Float = 72f,
    minColumns: Int = 3,
    maxColumns: Int = 5,
): Int = (usableWidth / minItemWidth).toInt().coerceIn(minColumns, maxColumns)

fun <T> menuPageItems(items: List<T>, page: Int, pageSize: Int): List<T> =
    items.drop(page * pageSize).take(pageSize)
