/*
 Copyright (c) 2021-2023,
 This library class was created by 0xFC963F18DC21 / Shots243
 It is part of an extension library for the game NullpoMino (copyright 2010-2023)

 Kotlin converted and modified by Venom=Nhelv

 Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.

 THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

 Original Repository: https://github.com/Shots243/ModePile

 When using this library in a mode / library pack of your own, the following
 conditions must be satisfied:
     - This license must remain visible at the top of the document, unmodified.
     - You are allowed to use this library for any modding purpose.
         - If this is the case, the Library Creator must be credited somewhere.
             - Source comments only are fine, but in a README is recommended.
     - Modification of this library is allowed, but only in the condition that a
       pull request is made to merge the changes to the repository.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

package zeroxfc.nullpo.custom.modes.objects.minesweeper

import kotlin.random.Random

class GameGrid @JvmOverloads constructor(val length:Int = 10, val height:Int = 10, minePercent:Float = 0.1f,
	randSeed:Long = 0) {
	val squares:Int get() = length*height
	val mines:Int = (minePercent/100f*squares).toInt()
	private val randomizer:Random = Random(randSeed)
	var contents:Array<Array<GridSpace>> = Array(height) {Array(length) {GridSpace(false)}}

	fun generateMines(excludeX:Int, excludeY:Int) {
		var i = 0
		while(i<mines) {
			var testX:Int
			var testY:Int
			var rollCount = 0
			do {
				testX = randomizer.nextInt(length)
				testY = randomizer.nextInt(height)
				rollCount++
			} while(getSurroundingMines(testX, testY)>=3&&rollCount<6)
			if(!contents[testY][testX].isMine&&!(testY==excludeY&&testX==excludeX)) {
				contents[testY][testX].isMine = true
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
		val testLocations = arrayOf(
			intArrayOf(-1, -1), intArrayOf(0, -1), intArrayOf(1, -1), intArrayOf(-1, 0), intArrayOf(1, 0),
			intArrayOf(-1, 1), intArrayOf(0, 1), intArrayOf(1, 1)
		)
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
		val testLocations = arrayOf(
			intArrayOf(-1, -1), intArrayOf(0, -1), intArrayOf(1, -1), intArrayOf(-1, 0), intArrayOf(1, 0),
			intArrayOf(-1, 1), intArrayOf(0, 1), intArrayOf(1, 1)
		)
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
		val testLocations = arrayOf(
			intArrayOf(-1, -1), intArrayOf(0, -1), intArrayOf(1, -1), intArrayOf(-1, 0), intArrayOf(1, 0),
			intArrayOf(-1, 1), intArrayOf(0, 1), intArrayOf(1, 1)
		)
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
		for(y in 0 until height) for(x in 0 until length)
			if(contents[y][x].isMine) contents[y][x].uncovered = true
	}

	fun uncoverNonMines() {
		for(y in 0 until height) for(x in 0 until length)
			if(!contents[y][x].isMine) contents[y][x].uncovered = true
	}

	fun flagAllCovered() {
		for(y in 0 until height) for(x in 0 until length)
			if(!contents[y][x].uncovered) {
				contents[y][x].flagged = true
				contents[y][x].question = false
			}
	}

	fun uncoverAt(x:Int, y:Int):State = if(!contents[y][x].flagged&&!contents[y][x].uncovered&&!contents[y][x].question) {
		contents[y][x].uncovered = true

		if(contents[y][x].isMine) State.MINE else {
			if(contents[y][x].surroundingMines==0) {
				val testLocations = arrayOf(
					intArrayOf(-1, -1), intArrayOf(0, -1), intArrayOf(1, -1), intArrayOf(-1, 0),
					intArrayOf(1, 0), intArrayOf(-1, 1), intArrayOf(0, 1), intArrayOf(1, 1)
				)
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
	} else State.ALREADY_OPEN

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
