package com.tetris.app.ui

// ── Board dimensions ──────────────────────────────────────────────────────────
const val COLS = 10
const val ROWS = 20

// ── Tetromino definitions (each piece = list of 4-rotation arrays of (row,col)) ─
data class Tetromino(val cells: Array<Array<Pair<Int, Int>>>, val colorIndex: Int)

// Rotation arrays: each element is a list of (row, col) offsets from origin (0,0)
val TETROMINOES = listOf(
    // I
    Tetromino(
        arrayOf(
            arrayOf(0 to 0, 0 to 1, 0 to 2, 0 to 3),
            arrayOf(0 to 2, 1 to 2, 2 to 2, 3 to 2),
            arrayOf(3 to 0, 3 to 1, 3 to 2, 3 to 3),
            arrayOf(0 to 1, 1 to 1, 2 to 1, 3 to 1)
        ), 0
    ),
    // O
    Tetromino(
        arrayOf(
            arrayOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),
            arrayOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),
            arrayOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),
            arrayOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)
        ), 1
    ),
    // T
    Tetromino(
        arrayOf(
            arrayOf(0 to 1, 1 to 0, 1 to 1, 1 to 2),
            arrayOf(0 to 0, 1 to 0, 1 to 1, 2 to 0),
            arrayOf(1 to 0, 1 to 1, 1 to 2, 2 to 1),
            arrayOf(0 to 1, 1 to 0, 1 to 1, 2 to 1)
        ), 2
    ),
    // S
    Tetromino(
        arrayOf(
            arrayOf(0 to 1, 0 to 2, 1 to 0, 1 to 1),
            arrayOf(0 to 0, 1 to 0, 1 to 1, 2 to 1),
            arrayOf(0 to 1, 0 to 2, 1 to 0, 1 to 1),
            arrayOf(0 to 0, 1 to 0, 1 to 1, 2 to 1)
        ), 3
    ),
    // Z
    Tetromino(
        arrayOf(
            arrayOf(0 to 0, 0 to 1, 1 to 1, 1 to 2),
            arrayOf(0 to 1, 1 to 0, 1 to 1, 2 to 0),
            arrayOf(0 to 0, 0 to 1, 1 to 1, 1 to 2),
            arrayOf(0 to 1, 1 to 0, 1 to 1, 2 to 0)
        ), 4
    ),
    // J
    Tetromino(
        arrayOf(
            arrayOf(0 to 0, 1 to 0, 1 to 1, 1 to 2),
            arrayOf(0 to 0, 0 to 1, 1 to 0, 2 to 0),
            arrayOf(1 to 0, 1 to 1, 1 to 2, 2 to 2),
            arrayOf(0 to 1, 1 to 1, 2 to 0, 2 to 1)
        ), 5
    ),
    // L
    Tetromino(
        arrayOf(
            arrayOf(0 to 2, 1 to 0, 1 to 1, 1 to 2),
            arrayOf(0 to 0, 1 to 0, 2 to 0, 2 to 1),
            arrayOf(1 to 0, 1 to 1, 1 to 2, 2 to 0),
            arrayOf(0 to 0, 0 to 1, 1 to 1, 2 to 1)
        ), 6
    )
)

data class ActivePiece(
    val type: Tetromino,
    val rotation: Int,
    val row: Int,
    val col: Int
) {
    fun cells(): List<Pair<Int, Int>> =
        type.cells[rotation].map { (dr, dc) -> (row + dr) to (col + dc) }
}

enum class GameState { PLAYING, PAUSED, GAME_OVER }

data class TetrisState(
    val board: Array<IntArray> = Array(ROWS) { IntArray(COLS) { -1 } },
    val active: ActivePiece? = null,
    val next: Tetromino = TETROMINOES.random(),
    val score: Int = 0,
    val lines: Int = 0,
    val level: Int = 1,
    val gameState: GameState = GameState.PLAYING
) {
    fun dropInterval(): Long = maxOf(100L, 800L - (level - 1) * 70L)
}
