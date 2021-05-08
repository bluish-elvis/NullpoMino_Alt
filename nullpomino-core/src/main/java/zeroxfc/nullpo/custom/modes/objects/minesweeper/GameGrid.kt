package zeroxfc.nullpo.custom.modes.objects.minesweeper

import kotlin.random.Random

class GameGrid @JvmOverloads constructor(val length:Int = 10, val height:Int = 10, minePercent:Float = 0.1f,
	randseed:Long = 0) {
	val squares:Int get() = length*height
	val mines:Int = (minePercent/100f*squares).toInt()
	private val randomizer:Random = Random(randseed)
	var contents:Array<Array<GridSpace>> = Array(height) {Array(length) {GridSpace(false)}}

	fun generateMines(excludeX:Int, excludeY:Int) {
		var i = 0
		while(i<mines) {
			var TestX:Int
			var TestY:Int
			var RollCount = 0
			do {
				TestX = randomizer.nextInt(length)
				TestY = randomizer.nextInt(height)
				RollCount++
			} while(getSurroundingMines(TestX, TestY)>=3&&RollCount<6)
			if(!contents[TestY][TestX].isMine&&!(TestY==excludeY&&TestX==excludeX)) {
				contents[TestY][TestX].isMine = true
			} else {
				i--
			}
			i++
		}
		for(y in 0 until height) {
			for(x in 0 until length) {
				if(!contents[y][x].isMine) {
					contents[y][x].surroundingMines = getSurroundingMines(x, y)
				}
			}
		}
	}

	fun getSurroundingMines(x:Int, y:Int):Int {
		var mine = 0
		val testLocations = arrayOf(intArrayOf(-1, -1), intArrayOf(0, -1), intArrayOf(1, -1), intArrayOf(-1, 0), intArrayOf(1, 0),
			intArrayOf(-1, 1), intArrayOf(0, 1), intArrayOf(1, 1))
		for(loc in testLocations) {
			val px = x+loc[0]
			val py = y+loc[1]
			if(px<0||px>=length) continue
			if(py<0||py>=height) continue
			if(contents[py][px].isMine) mine++
		}
		return mine
	}

	fun getSurroundingFlags(x:Int, y:Int):Int {
		var flag = 0
		val testLocations = arrayOf(intArrayOf(-1, -1), intArrayOf(0, -1), intArrayOf(1, -1), intArrayOf(-1, 0), intArrayOf(1, 0),
			intArrayOf(-1, 1), intArrayOf(0, 1), intArrayOf(1, 1))
		for(loc in testLocations) {
			val px = x+loc[0]
			val py = y+loc[1]
			if(px<0||px>=length) continue
			if(py<0||py>=height) continue
			if(contents[py][px].flagged) flag++
		}
		return flag
	}

	fun getSurroundingCovered(x:Int, y:Int):Int {
		var flag = 0
		val testLocations = arrayOf(intArrayOf(-1, -1), intArrayOf(0, -1), intArrayOf(1, -1), intArrayOf(-1, 0), intArrayOf(1, 0),
			intArrayOf(-1, 1), intArrayOf(0, 1), intArrayOf(1, 1))
		for(loc in testLocations) {
			val px = x+loc[0]
			val py = y+loc[1]
			if(px<0||px>=length) continue
			if(py<0||py>=height) continue
			if(!contents[py][px].uncovered) flag++
		}
		return flag
	}

	fun uncoverAllMines() {
		for(y in 0 until height) {
			for(x in 0 until length) {
				if(contents[y][x].isMine) {
					contents[y][x].uncovered = true
				}
			}
		}
	}

	fun uncoverNonMines() {
		for(y in 0 until height) {
			for(x in 0 until length) {
				if(!contents[y][x].isMine) {
					contents[y][x].uncovered = true
				}
			}
		}
	}

	fun flagAllCovered() {
		for(y in 0 until height) {
			for(x in 0 until length) {
				if(!contents[y][x].uncovered) {
					contents[y][x].flagged = true
					contents[y][x].question = false
				}
			}
		}
	}

	fun uncoverAt(x:Int, y:Int):State {
		if(!contents[y][x].flagged&&!contents[y][x].uncovered&&!contents[y][x].question) {
			contents[y][x].uncovered = true
			return if(contents[y][x].isMine) {
				State.MINE
			} else {
				if(contents[y][x].surroundingMines==0) {
					val testLocations = arrayOf(intArrayOf(-1, -1), intArrayOf(0, -1), intArrayOf(1, -1), intArrayOf(-1, 0),
						intArrayOf(1, 0), intArrayOf(-1, 1), intArrayOf(0, 1), intArrayOf(1, 1))
					for(loc in testLocations) {
						val px = x+loc[0]
						val py = y+loc[1]
						if(px<0||px>=length) continue
						if(py<0||py>=height) continue
						if(!contents[py][px].uncovered) uncoverAt(px, py)
					}
				}
				State.SAFE
			}
		}
		return State.ALREADY_OPEN
	}

	fun cycleState(x:Int, y:Int):Boolean {
		if(!contents[y][x].uncovered) {
			if(!contents[y][x].flagged&&!contents[y][x].question) {
				contents[y][x].flagged = true
				contents[y][x].question = false
				return false
			} else if(contents[y][x].flagged&&!contents[y][x].question) {
				contents[y][x].flagged = false
				contents[y][x].question = true
				return false
			} else if(!contents[y][x].flagged&&contents[y][x].question) {
				contents[y][x].flagged = false
				contents[y][x].question = false
				return false
			}
		}
		return true
	}

	fun getSquareAt(x:Int, y:Int):GridSpace = contents[y][x]

	val coveredSquares:Int get() = contents.sumOf {it -> it.count {!it.uncovered}}
	val flaggedSquares:Int get() = contents.sumOf {it -> it.count {it.flagged}}

	enum class State {
		SAFE, MINE, ALREADY_OPEN
	}

	companion object {

		const val STATE_SAFE = 0
		const val STATE_MINE = 1
		const val STATE_ALREADY_OPEN = 2
	}

}