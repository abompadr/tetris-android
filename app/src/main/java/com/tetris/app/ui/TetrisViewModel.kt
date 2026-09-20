package com.tetris.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TetrisViewModel : ViewModel() {

    private val _state = MutableStateFlow(TetrisState())
    val state: StateFlow<TetrisState> = _state

    private var dropJob: Job? = null

    init {
        spawnPiece()
        startDropLoop()
    }

    // ── Public actions ────────────────────────────────────────────────────────

    fun moveLeft() = transformActive { it.copy(col = it.col - 1) }
    fun moveRight() = transformActive { it.copy(col = it.col + 1) }
    fun rotate() = transformActive { it.copy(rotation = (it.rotation + 1) % 4) }

    fun softDrop() {
        val s = _state.value
        if (s.gameState != GameState.PLAYING) return
        val moved = s.active?.copy(row = s.active.row + 1) ?: return
        if (isValid(moved, s.board)) {
            _state.value = s.copy(active = moved, score = s.score + 1)
        } else {
            lockAndSpawn()
        }
    }

    fun hardDrop() {
        val s = _state.value
        if (s.gameState != GameState.PLAYING) return
        var piece = s.active ?: return
        var dropped = 0
        while (true) {
            val next = piece.copy(row = piece.row + 1)
            if (isValid(next, s.board)) { piece = next; dropped++ } else break
        }
        _state.value = s.copy(active = piece, score = s.score + dropped * 2)
        lockAndSpawn()
    }

    fun togglePause() {
        val s = _state.value
        if (s.gameState == GameState.GAME_OVER) return
        if (s.gameState == GameState.PLAYING) {
            _state.value = s.copy(gameState = GameState.PAUSED)
            dropJob?.cancel()
        } else {
            _state.value = s.copy(gameState = GameState.PLAYING)
            startDropLoop()
        }
    }

    fun restart() {
        dropJob?.cancel()
        _state.value = TetrisState()
        spawnPiece()
        startDropLoop()
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun startDropLoop() {
        dropJob?.cancel()
        dropJob = viewModelScope.launch {
            while (true) {
                delay(_state.value.dropInterval())
                val s = _state.value
                if (s.gameState != GameState.PLAYING) break
                val moved = s.active?.copy(row = s.active.row + 1) ?: break
                if (isValid(moved, s.board)) {
                    _state.value = s.copy(active = moved)
                } else {
                    lockAndSpawn()
                    if (_state.value.gameState == GameState.GAME_OVER) break
                }
            }
        }
    }

    private fun spawnPiece() {
        val s = _state.value
        val piece = ActivePiece(s.next, 0, 0, COLS / 2 - 2)
        val nextType = TETROMINOES.random()
        if (!isValid(piece, s.board)) {
            _state.value = s.copy(gameState = GameState.GAME_OVER, active = null)
            return
        }
        _state.value = s.copy(active = piece, next = nextType)
    }

    private fun lockAndSpawn() {
        val s = _state.value
        val piece = s.active ?: return

        // Paint cells onto board
        val newBoard = s.board.map { it.clone() }.toTypedArray()
        for ((r, c) in piece.cells()) {
            if (r in 0 until ROWS && c in 0 until COLS)
                newBoard[r][c] = piece.type.colorIndex
        }

        // Clear complete lines
        val cleared = newBoard.filter { row -> row.all { it >= 0 } }
        val kept = newBoard.filter { row -> row.any { it < 0 } }
        val linesCleared = cleared.size
        val filledBoard = Array(ROWS) { i ->
            if (i < ROWS - kept.size) IntArray(COLS) { -1 } else kept[i - (ROWS - kept.size)]
        }

        val lineScores = listOf(0, 100, 300, 500, 800)
        val addScore = (lineScores.getOrElse(linesCleared) { 800 }) * s.level
        val newLines = s.lines + linesCleared
        val newLevel = newLines / 10 + 1

        _state.value = s.copy(
            board = filledBoard,
            active = null,
            score = s.score + addScore,
            lines = newLines,
            level = newLevel
        )
        spawnPiece()
        if (_state.value.gameState == GameState.PLAYING) startDropLoop()
    }

    private fun transformActive(transform: (ActivePiece) -> ActivePiece) {
        val s = _state.value
        if (s.gameState != GameState.PLAYING) return
        val piece = s.active ?: return
        val moved = transform(piece)
        if (isValid(moved, s.board)) _state.value = s.copy(active = moved)
    }

    private fun isValid(piece: ActivePiece, board: Array<IntArray>): Boolean {
        for ((r, c) in piece.cells()) {
            if (c < 0 || c >= COLS || r >= ROWS) return false
            if (r >= 0 && board[r][c] >= 0) return false
        }
        return true
    }
}
