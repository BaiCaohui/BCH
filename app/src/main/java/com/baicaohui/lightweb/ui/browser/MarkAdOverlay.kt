package com.baicaohui.lightweb.ui.browser

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.baicaohui.lightweb.R

private const val MIN_SIZE = 24f
private const val HANDLE_HIT = 28f

enum class MarkHandle { MOVE, NW, N, NE, E, SE, S, SW, W }

/**
 * “标记为广告”覆盖层：
 * - 单击广告 → 自动识别该位置的广告元素并把选框吸附到其实际范围；
 * - 拖动 → 框选后同样自动识别并吸附；
 * - 选框支持拖动手柄缩放、整体移动、1px/10px 微调；
 * - 调整面板默认收起为右下角小控件，展开时才显示完整调节区，避免遮挡页面底部广告。
 */
@Composable
fun MarkAdOverlay(
    rect: Rect?,
    moveStep: Int,
    onMoveStepChange: (Int) -> Unit,
    onRectChange: (Rect?) -> Unit,
    onIdentify: (Offset) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    var dragMode by remember { mutableStateOf(MarkHandle.MOVE) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragOrigin by remember { mutableStateOf<Rect?>(null) }
    val currentRect by rememberUpdatedState(rect)
    var expanded by remember { mutableStateOf(false) }

    fun resetDrag() {
        dragMode = MarkHandle.MOVE
        dragStart = null
        dragOrigin = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.06f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { pos -> onIdentify(pos) })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos ->
                        dragStart = pos
                        val r = currentRect
                        if (r == null) {
                            dragMode = MarkHandle.SE
                            dragOrigin = null
                        } else {
                            dragMode = hitTest(r, pos)
                            dragOrigin = r
                        }
                    },
                    onDrag = { change, _ ->
                        val start = dragStart ?: return@detectDragGestures
                        val current = change.position
                        val origin = dragOrigin
                        onRectChange(
                            if (origin == null) {
                                Rect(
                                    left = minOf(start.x, current.x),
                                    top = minOf(start.y, current.y),
                                    right = maxOf(start.x, current.x),
                                    bottom = maxOf(start.y, current.y),
                                ).takeIf { it.width >= MIN_SIZE && it.height >= MIN_SIZE }
                            } else if (dragMode == MarkHandle.MOVE) {
                                origin.translate(current - start)
                            } else {
                                adjustRect(origin, dragMode, current)
                            },
                        )
                        change.consume()
                    },
                    onDragEnd = {
                        resetDrag()
                        currentRect?.let { onIdentify(it.center) }
                    },
                    onDragCancel = { resetDrag() },
                )
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = rect
            if (r != null) {
                drawRect(
                    color = Color(0x3300A0FF),
                    topLeft = r.topLeft,
                    size = r.size,
                )
                drawRect(
                    color = Color(0xFF00A0FF),
                    topLeft = r.topLeft,
                    size = r.size,
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawHandles(r)
            }
        }

        if (rect == null) {
            Text(
                text = stringResource(R.string.mark_ad_hint),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        } else if (expanded) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.mark_ad_move_step),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    FilterChip(
                        selected = moveStep == 1,
                        onClick = { onMoveStepChange(1) },
                        label = { Text("1px") },
                    )
                    FilterChip(
                        selected = moveStep == 10,
                        onClick = { onMoveStepChange(10) },
                        label = { Text("10px") },
                    )
                    IconButton(onClick = { expanded = false }) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.mark_ad_collapse),
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MoveIconButton(Icons.Filled.KeyboardArrowLeft, R.string.mark_ad_move_left) {
                        onRectChange(rect.translate(-moveStep.toFloat(), 0f))
                    }
                    Column {
                        MoveIconButton(Icons.Filled.KeyboardArrowUp, R.string.mark_ad_move_up) {
                            onRectChange(rect.translate(0f, -moveStep.toFloat()))
                        }
                        MoveIconButton(Icons.Filled.KeyboardArrowDown, R.string.mark_ad_move_down) {
                            onRectChange(rect.translate(0f, moveStep.toFloat()))
                        }
                    }
                    MoveIconButton(Icons.Filled.KeyboardArrowRight, R.string.mark_ad_move_right) {
                        onRectChange(rect.translate(moveStep.toFloat(), 0f))
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                ) {
                    TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.mark_ad_mark))
                    }
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
            ) {
                FloatingIconButton(Icons.Filled.Close, R.string.dialog_cancel, onCancel)
                Button(onClick = onConfirm) {
                    Text(stringResource(R.string.mark_ad_mark))
                }
                FloatingIconButton(Icons.Filled.Tune, R.string.mark_ad_expand) {
                    expanded = true
                }
            }
        }
    }
}

@Composable
private fun FloatingIconButton(
    icon: ImageVector,
    @StringRes labelRes: Int,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = stringResource(labelRes))
        }
    }
}

@Composable
private fun MoveIconButton(
    icon: ImageVector,
    @StringRes labelRes: Int,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = stringResource(labelRes))
    }
}

private fun DrawScope.drawHandles(r: Rect) {
    val color = Color(0xFF00A0FF)
    val radius = 8.dp.toPx()
    val points = listOf(
        r.topLeft,
        Offset(r.center.x, r.top),
        r.topRight,
        Offset(r.right, r.center.y),
        r.bottomRight,
        Offset(r.center.x, r.bottom),
        r.bottomLeft,
        Offset(r.left, r.center.y),
    )
    points.forEach { drawCircle(color = color, radius = radius, center = it) }
}

private fun hitTest(r: Rect, pos: Offset): MarkHandle {
    fun near(a: Offset, b: Offset): Boolean = (a - b).getDistance() <= HANDLE_HIT
    return when {
        near(pos, r.topLeft) -> MarkHandle.NW
        near(pos, Offset(r.center.x, r.top)) -> MarkHandle.N
        near(pos, r.topRight) -> MarkHandle.NE
        near(pos, Offset(r.right, r.center.y)) -> MarkHandle.E
        near(pos, r.bottomRight) -> MarkHandle.SE
        near(pos, Offset(r.center.x, r.bottom)) -> MarkHandle.S
        near(pos, r.bottomLeft) -> MarkHandle.SW
        near(pos, Offset(r.left, r.center.y)) -> MarkHandle.W
        else -> MarkHandle.MOVE
    }
}

private fun adjustRect(r: Rect, mode: MarkHandle, pos: Offset): Rect {
    var left = r.left
    var top = r.top
    var right = r.right
    var bottom = r.bottom
    if (mode == MarkHandle.NW || mode == MarkHandle.W || mode == MarkHandle.SW) {
        left = minOf(pos.x, right - MIN_SIZE)
    }
    if (mode == MarkHandle.NE || mode == MarkHandle.E || mode == MarkHandle.SE) {
        right = maxOf(pos.x, left + MIN_SIZE)
    }
    if (mode == MarkHandle.NW || mode == MarkHandle.N || mode == MarkHandle.NE) {
        top = minOf(pos.y, bottom - MIN_SIZE)
    }
    if (mode == MarkHandle.SW || mode == MarkHandle.S || mode == MarkHandle.SE) {
        bottom = maxOf(pos.y, top + MIN_SIZE)
    }
    return Rect(left, top, right, bottom)
}
