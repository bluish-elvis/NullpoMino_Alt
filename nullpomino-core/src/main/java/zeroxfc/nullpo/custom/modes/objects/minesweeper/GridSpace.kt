package zeroxfc.nullpo.custom.modes.objects.minesweeper

class GridSpace(var isMine:Boolean) {
	var surroundingMines = 0
	var uncovered = false
	var flagged = false
	var question = false
}