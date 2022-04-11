package edu.cuhk.cse.fyp.tetrisai.lspi

import java.awt.Color
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

open class State {
	var lost = false
		protected set
	var doublePlayer = false
	var label:TLabel? = null
	/**current turn*/
	var turnNumber = 0
		protected set
	var rowsCleared = 0
		protected set
	/**each square in the grid - int means empty - other values mean the turn it was placed*/
	open val field = Array<IntArray?>(ROWS) {IntArray(COLS)}
	//top row+1 of each column
	//0 means empty
	open val top = IntArray(COLS)
	//number of next piece
	var nextPiece:Int = 0
	/** 7-bag generator*/
	protected var bag = intArrayOf(0, 1, 2, 3, 4, 5, 6)
	protected var bagIndex = 7
	var b2b = false
		protected set
	var linesSent = 0
		protected set
	var lastCleared = false
		protected set
	var combo = 0
		protected set
	var linesStack = 0
		protected set
	var totalLinesSent = 0
		protected set
	//initialize legalMoves
	init {
		//for each piece type
		for(i in 0 until N_PIECES) {
			//figure number of legal moves
			var n = 0
			for(j in 0 until pOrients[i]) {
				//number of locations in this orientation
				n += COLS+1-pWidth[i][j]
			}
			//allocate space
			legalMoves[i] = Array(n) {IntArray(2)}
			//for each orientation
			n = 0
			for(j in 0 until pOrients[i]) {
				//for each slot
				for(k in 0 until COLS+1-pWidth[i][j]) {
					legalMoves[i][n][ORIENT] = j
					legalMoves[i][n][SLOT] = k
					n++
				}
			}
		}
	}

	fun addLinesStack(linesSent:Int) {
		linesStack += linesSent
	}

	fun addLineSent(linesSent:Int) {
		this.linesSent = linesSent
		totalLinesSent += linesSent
	}

	fun addLineCleared(linesCleared:Int) {
		rowsCleared += linesCleared
	}
	// random shuffle bag
	private fun shuffleBag() {
		val rnd:Random = ThreadLocalRandom.current()
		for(i in bag.size-1 downTo 1) {
			val index = rnd.nextInt(i+1)
			// Simple swap
			val tmp = bag[index]
			bag[index] = bag[i]
			bag[i] = tmp
		}
	}
	//random integer, returns 0-6
	private fun randomPiece():Int {
		if(bagIndex<0||bagIndex>6) {
			shuffleBag()
			bagIndex = 0
		}
		return bag[bagIndex++]
	}
	// add lines stack to field
	private fun addLines():Boolean {
		// check if game end
		for(c in 0 until COLS) {
			if(top[c]+linesStack>=ROWS) {
				lost = true
				return false
			}
		}
		val rand = Random()
		val hole = rand.nextInt(COLS)
		for(i in ROWS-1 downTo 0) {
			for(j in 0 until COLS) {
				if(i<linesStack) field[i]!![j] = (if(j==hole) 0 else 1) //);
				else field[i]!![j] = field[i-linesStack]!![j]
			}
		}
		for(i in 0 until COLS) top[i] += linesStack
		linesStack = 0
		return true
	}
	// calculate lines sent
	protected fun calLinesSent(rowsCleared:Int) {
		linesSent = 0
		if(rowsCleared==0) {
			combo = 0
			lastCleared = false
		} else {
			// calculate combo
			if(lastCleared) {
				if(combo<2) combo++ else if(combo<4) combo += 2 else if(combo<6) combo += 3 else combo += 4
				linesSent += combo
			} else {
				lastCleared = true
			}

			// calculate lines sent
			if(rowsCleared==4) {
				linesSent += 4
				if(b2b) linesSent += 2 else b2b = true
			} else {
				b2b = false
				if(rowsCleared==2) linesSent++ else if(rowsCleared==3) linesSent += 2
			}

			// perfect clear
			var perfectClear = true
			for(i in 0 until ROWS) {
				for(j in 0 until COLS) {
					if(field[i]!![j]!=0) {
						perfectClear = false
						break
					}
				}
			}
			if(perfectClear) linesSent += 10
			totalLinesSent += linesSent
		}
	}
	// calculate lines sent and return result
	open fun calLinesSentResult(rowsCleared:Int, twist:Boolean):Int {
		var sent = 0
		var totalSent = 0
		if(rowsCleared==0) {
			combo = 0
		} else {
			// calculate combo
			if(lastCleared) {
				if(combo<2) combo++ else if(combo<4) combo += 2 else if(combo<6) combo += 3 else combo += 4
				sent += combo
			}

			// calculate lines sent
			if(twist) {
				sent += rowsCleared*2
				if(b2b) sent += 1 else b2b = true
			} else if(rowsCleared==4) {
					sent += 4
					if(b2b) sent += 1 else b2b = true
				} else {
					b2b = false
					if(rowsCleared==2) sent++ else if(rowsCleared==3) sent += 2
				}

			// perfect clear
			var perfectClear = true
			for(i in 0 until ROWS) {
				for(j in 0 until COLS) {
					if(field[i]!![j]!=0) {
						perfectClear = false
						break
					}
				}
			}
			if(perfectClear) sent += 10
			totalSent += sent
		}
		return sent
	}
	//gives legal moves for
	fun legalMoves():Array<IntArray> = legalMoves[nextPiece]
	//make a move based on the move index - its order in the legalMoves list
	fun makeMove(move:Int) = makeMove(legalMoves[nextPiece][move])
	//make a move based on an array of orient and slot
	fun makeMove(move:IntArray) = makeMove(move[ORIENT], move[SLOT])

	//returns false if you lose - true otherwise
	open fun makeMove(orient:Int, slot:Int):Boolean {
		turnNumber++
		//height if the first column makes contact
		var height = top[slot]-pBottom[nextPiece][orient][0]
		//for each column beyond the first in the piece
		for(c in 1 until pWidth[nextPiece][orient]) {
			height = maxOf(height, top[slot+c]-pBottom[nextPiece][orient][c])
		}

		//check if game ended
		if(height+pHeight[nextPiece][orient]>=20) {
			lost = true
			return false
		}

		//for each column in the piece - fill in the appropriate blocks
		for(i in 0 until pWidth[nextPiece][orient]) {
			//from bottom to top of brick
			for(h in height+pBottom[nextPiece][orient][i] until height+pTop[nextPiece][orient][i]) {
				field[h]!![i+slot] = 1 //turn;
			}
		}

		//adjust top
		for(c in 0 until pWidth[nextPiece][orient]) {
			top[slot+c] = height+pTop[nextPiece][orient][c]
		}
		var rowsCleared = 0
		//check for full rows - starting at the top
		for(r in height+pHeight[nextPiece][orient]-1 downTo height) {
			//check all columns in the row
			var full = true
			for(c in 0 until COLS) {
				if(field[r]!![c]==0) {
					full = false
					break
				}
			}
			//if the row was full - remove it and slide above stuff down
			if(full) {
				rowsCleared++
				this.rowsCleared++
				//for each column
				for(c in 0 until COLS) {
					//slide down all bricks
					for(i in r until top[c]) {
						if(i+1==23) field[i]!![c] = 0 else field[i]!![c] = field[i+1]!![c]
					}
					//lower the top
					top[c]--
					while(top[c]>=1&&field[top[c]-1]!![c]==0) top[c]--
				}
			}
		}
		var valid = true
		calLinesSent(rowsCleared)
		if(linesSent<linesStack) {
			linesStack -= linesSent
			valid = addLines()
		} else {
			linesSent -= linesStack
			linesStack = 0
		}

		//pick a new piece
		if(!doublePlayer && this !is FutureState) nextPiece = randomPiece()
		return valid
	}

	fun draw() {
		label!!.clear()
		label!!.setPenRadius()
		//outline board
		label!!.line(0.0, 0.0, 0.0, (ROWS+5).toDouble())
		label!!.line(COLS.toDouble(), 0.0, COLS.toDouble(), (ROWS+5).toDouble())
		label!!.line(0.0, 0.0, COLS.toDouble(), 0.0)
		label!!.line(0.0, (ROWS-3).toDouble(), COLS.toDouble(), (ROWS-3).toDouble())

		//show bricks
		for(c in 0 until COLS) {
			for(r in 0 until top[c]) {
				if(field[r]!![c]!=0) {
					drawBrick(c, r)
				}
			}
		}
		for(i in 0 until COLS) {
			label!!.setPenColor(Color.red)
			label!!.line(i.toDouble(), top[i].toDouble(), (i+1).toDouble(), top[i].toDouble())
			label!!.setPenColor()
		}
		label!!.show()
	}
	//constructor
	init {
		nextPiece = randomPiece()
	}

	private fun drawBrick(c:Int, r:Int) {
		label!!.filledRectangleLL(c.toDouble(), r.toDouble(), 1.0, 1.0, brickCol)
		label!!.rectangleLL(c.toDouble(), r.toDouble(), 1.0, 1.0)
	}

	fun drawNext(slot:Int, orient:Int) {
		for(i in 0 until pWidth[nextPiece][orient]) {
			for(j in pBottom[nextPiece][orient][i] until pTop[nextPiece][orient][i]) {
				drawBrick(i+slot, j+ROWS+1)
			}
		}
		label!!.show()
	}
	//visualization
	//clears the area where the next piece is shown (top)
	fun clearNext() {
		label!!.filledRectangleLL(0.0, ROWS+.9, COLS.toDouble(), 4.2, TLabel.DEFAULT_CLEAR_COLOR)
		label!!.line(0.0, 0.0, 0.0, (ROWS+5).toDouble())
		label!!.line(COLS.toDouble(), 0.0, COLS.toDouble(), (ROWS+5).toDouble())
	}

	companion object {
		const val COLS = 10
		const val ROWS = 23
		const val N_PIECES = 7
		//all legal moves - first index is piece type - then a list of 2-length arrays
		@JvmStatic
		protected var legalMoves:Array<Array<IntArray>> = Array(N_PIECES) {emptyArray()}
		//indices for legalMoves
		const val ORIENT = 0
		const val SLOT = 1
		//possible orientations for a given piece type
		var pOrients = intArrayOf(1, 2, 4, 4, 4, 2, 2)
		//the next several arrays define the piece vocabulary in detail
		//width of the pieces [piece ID][orientation]
		var pWidth =
			arrayOf(
				intArrayOf(2),
				intArrayOf(1, 4),
				intArrayOf(2, 3, 2, 3),
				intArrayOf(2, 3, 2, 3),
				intArrayOf(2, 3, 2, 3),
				intArrayOf(3, 2),
				intArrayOf(3, 2)
			)
		//height of the pieces [piece ID][orientation]
		@JvmStatic
		protected val pHeight =
			arrayOf(
				intArrayOf(2),
				intArrayOf(4, 1),
				intArrayOf(3, 2, 3, 2),
				intArrayOf(3, 2, 3, 2),
				intArrayOf(3, 2, 3, 2),
				intArrayOf(2, 3),
				intArrayOf(2, 3)
			)
		@JvmStatic
		protected val pBottom =
			arrayOf(
				arrayOf(intArrayOf(0, 0)),
				arrayOf(intArrayOf(0), intArrayOf(0, 0, 0, 0)),
				arrayOf(intArrayOf(0, 0), intArrayOf(0, 1, 1), intArrayOf(2, 0), intArrayOf(0, 0, 0)),
				arrayOf(intArrayOf(0, 0), intArrayOf(0, 0, 0), intArrayOf(0, 2), intArrayOf(1, 1, 0)),
				arrayOf(intArrayOf(0, 1), intArrayOf(1, 0, 1), intArrayOf(1, 0), intArrayOf(0, 0, 0)),
				arrayOf(intArrayOf(0, 0, 1), intArrayOf(1, 0)),
				arrayOf(intArrayOf(1, 0, 0), intArrayOf(0, 1))
			)
		@JvmStatic
		protected val pTop =
			arrayOf(
				arrayOf(intArrayOf(2, 2)),
				arrayOf(intArrayOf(4), intArrayOf(1, 1, 1, 1)),
				arrayOf(intArrayOf(3, 1), intArrayOf(2, 2, 2), intArrayOf(3, 3), intArrayOf(1, 1, 2)),
				arrayOf(intArrayOf(1, 3), intArrayOf(2, 1, 1), intArrayOf(3, 3), intArrayOf(2, 2, 2)),
				arrayOf(intArrayOf(3, 2), intArrayOf(2, 2, 2), intArrayOf(2, 3), intArrayOf(1, 2, 1)),
				arrayOf(intArrayOf(1, 2, 2), intArrayOf(3, 2)),
				arrayOf(intArrayOf(2, 2, 1), intArrayOf(2, 3))
			)

		val brickCol = Color.gray
	}
}
