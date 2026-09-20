package com.tetris.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.abs

// ── Piece colours ─────────────────────────────────────────────────────────────
private val PIECE_COLORS = listOf(
    Color(0xFF00BFFF), // I – cyan
    Color(0xFFFFD700), // O – yellow
    Color(0xFF9B59B6), // T – purple
    Color(0xFF2ECC71), // S – green
    Color(0xFFE74C3C), // Z – red
    Color(0xFF3498DB), // J – blue
    Color(0xFFFF8C00)  // L – orange
)
private val GHOST_COLOR = Color(0x44FFFFFF)
private val GRID_COLOR  = Color(0xFF1A1A2E)
private val BORDER_COLOR = Color(0xFF444466)

@Composable
fun TetrisScreen(vm: TetrisViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScoreBox("SCORE", state.score.toString())
            ScoreBox("LEVEL", state.level.toString())
            ScoreBox("LINES", state.lines.toString())
        }

        // ── Game board + side panel ───────────────────────────────────────────
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TetrisBoard(state, vm)

            Spacer(Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NEXT", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                NextPiecePreview(state.next)
            }
        }

        // ── Touch controls ────────────────────────────────────────────────────
        ControlPad(vm, state)
    }

    // ── Overlays ──────────────────────────────────────────────────────────────
    if (state.gameState == GameState.GAME_OVER) {
        GameOverOverlay(score = state.score, onRestart = { vm.restart() })
    }
    if (state.gameState == GameState.PAUSED) {
        PausedOverlay(onResume = { vm.togglePause() })
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Board Canvas
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TetrisBoard(state: TetrisState, vm: TetrisViewModel) {
    // Ghost piece position
    val ghost = computeGhost(state)

    // Gesture thresholds
    val swipeThreshold = 40f
    var dragAccX by remember { mutableFloatStateOf(0f) }
    var dragAccY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .aspectRatio(COLS.toFloat() / ROWS.toFloat())
            .fillMaxHeight(0.85f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { vm.rotate() },
                    onDoubleTap = { vm.hardDrop() }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccX = 0f },
                    onHorizontalDrag = { _, amount ->
                        dragAccX += amount
                        while (dragAccX > swipeThreshold) { vm.moveRight(); dragAccX -= swipeThreshold }
                        while (dragAccX < -swipeThreshold) { vm.moveLeft(); dragAccX += swipeThreshold }
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccY = 0f },
                    onVerticalDrag = { _, amount ->
                        dragAccY += amount
                        while (dragAccY > swipeThreshold) { vm.softDrop(); dragAccY -= swipeThreshold }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellW = size.width / COLS
            val cellH = size.height / ROWS

            // Background grid
            drawRect(GRID_COLOR, size = size)
            for (r in 0..ROWS) drawLine(BORDER_COLOR, Offset(0f, r * cellH), Offset(size.width, r * cellH), 0.5f)
            for (c in 0..COLS) drawLine(BORDER_COLOR, Offset(c * cellW, 0f), Offset(c * cellW, size.height), 0.5f)

            // Locked cells
            for (r in 0 until ROWS)
                for (c in 0 until COLS) {
                    val idx = state.board[r][c]
                    if (idx >= 0) drawCell(r, c, PIECE_COLORS[idx], cellW, cellH)
                }

            // Ghost
            ghost?.cells()?.forEach { (r, c) -> drawGhostCell(r, c, cellW, cellH) }

            // Active piece
            state.active?.cells()?.forEach { (r, c) ->
                if (r >= 0) drawCell(r, c, PIECE_COLORS[state.active.type.colorIndex], cellW, cellH)
            }
        }
    }
}

private fun DrawScope.drawCell(r: Int, c: Int, color: Color, cellW: Float, cellH: Float) {
    val pad = 1.5f
    drawRect(color, Offset(c * cellW + pad, r * cellH + pad), Size(cellW - pad * 2, cellH - pad * 2))
}

private fun DrawScope.drawGhostCell(r: Int, c: Int, cellW: Float, cellH: Float) {
    val pad = 1.5f
    drawRect(GHOST_COLOR, Offset(c * cellW + pad, r * cellH + pad), Size(cellW - pad * 2, cellH - pad * 2))
}

private fun computeGhost(state: TetrisState): ActivePiece? {
    var piece = state.active ?: return null
    while (true) {
        val next = piece.copy(row = piece.row + 1)
        val valid = next.cells().all { (r, c) ->
            c in 0 until COLS && r < ROWS && (r < 0 || state.board[r][c] < 0)
        }
        if (valid) piece = next else break
    }
    return if (piece.row != state.active?.row) piece else null
}

// ─────────────────────────────────────────────────────────────────────────────
// Next-piece preview
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NextPiecePreview(piece: Tetromino) {
    Canvas(
        modifier = Modifier
            .size(72.dp)
            .background(GRID_COLOR, RoundedCornerShape(4.dp))
    ) {
        val cellSize = size.width / 4
        val cells = piece.cells[0]
        val minR = cells.minOf { it.first }
        val minC = cells.minOf { it.second }
        val spanR = cells.maxOf { it.first } - minR + 1
        val spanC = cells.maxOf { it.second } - minC + 1
        val offsetR = (4 - spanR) / 2f
        val offsetC = (4 - spanC) / 2f
        cells.forEach { (dr, dc) ->
            val r = dr - minR + offsetR
            val c = dc - minC + offsetC
            val pad = 1.5f
            drawRect(
                PIECE_COLORS[piece.colorIndex],
                Offset(c * cellSize + pad, r * cellSize + pad),
                Size(cellSize - pad * 2, cellSize - pad * 2)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Touch control pad
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ControlPad(vm: TetrisViewModel, state: TetrisState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top row: rotate + hard drop
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CtrlButton("Rotate", Modifier.weight(1f)) { vm.rotate() }
            CtrlButton("Hard Drop", Modifier.weight(1f)) { vm.hardDrop() }
        }
        // Bottom row: left + soft drop + right + pause
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CtrlButton("◀", Modifier.weight(1f)) { vm.moveLeft() }
            CtrlButton("▼", Modifier.weight(1f)) { vm.softDrop() }
            CtrlButton("▶", Modifier.weight(1f)) { vm.moveRight() }
            CtrlButton(if (state.gameState == GameState.PAUSED) "▶" else "⏸", Modifier.weight(1f)) { vm.togglePause() }
        }
    }
}

@Composable
private fun CtrlButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A4A))
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScoreBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF8888AA), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GameOverOverlay(score: Int, onRestart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GAME OVER", color = Color(0xFFE74C3C), fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Score: $score", color = Color.White, fontSize = 20.sp)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRestart) { Text("Play Again") }
        }
    }
}

@Composable
private fun PausedOverlay(onResume: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PAUSED", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onResume) { Text("Resume") }
        }
    }
}
