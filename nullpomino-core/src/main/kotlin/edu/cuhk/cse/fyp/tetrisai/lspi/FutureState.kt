package edu.cuhk.cse.fyp.tetrisai.lspi

import java.util.Arrays
import java.util.Random

class FutureState:State() {
	override val field = Array<IntArray?>(ROWS) {IntArray(COLS)}
	override val top = IntArray(COLS)

	fun resetToCurrentState(s:State) {
		val field = s.field
		this.nextPiece = s.nextPiece
		lost = s.lost
		rowsCleared = s.rowsCleared
		turnNumber = s.turnNumber
		this.b2b = s.b2b
		linesSent = s.linesSent
		this.lastCleared = s.lastCleared
		this.combo = s.combo
		this.linesStack = s.linesStack
		totalLinesSent = s.totalLinesSent
		Arrays.fill(this.top, 0)
		for(i in field.indices.reversed()) {
			this.field[i] = field[i]?.clone()
			for(j in top.indices) if(top[j]==0&&field[i]!![j]>0) top[j] = i+1
		}
	}
	// add lines stack to field
	fun addLines(linesStack:Int):Boolean {
		// check if game end
		var linesStack = linesStack
		for(i in ROWS-1 downTo ROWS-1-linesStack+1) {
			for(j in 0 until COLS) {
				if(field[i]!![j]!=0) {
					lost = true
					return false
				}
			}
		}
		val rand = Random()
		val hole = rand.nextInt(COLS)
		for(i in ROWS-1 downTo 0) {
			for(j in 0 until COLS) {
				if(i<linesStack) field[i]!![j] = (if(j==hole) 0 else 1) //turn);
				else field[i]!![j] = field[i-linesStack]!![j]
			}
		}
		for(i in 0 until COLS) top[i] += linesStack
		linesStack = 0
		return true
	}
	// add lines stack to field
	private fun addLines():Boolean {
		// check if game end
		for(i in ROWS-1 downTo ROWS-1-linesStack+1) {
			for(j in 0 until COLS) {
				if(field[i]!![j]!=0) {
					lost = true
					return false
				}
			}
		}
		val rand = Random()
		val hole = rand.nextInt(COLS)
		for(i in ROWS-1 downTo 0) {
			for(j in 0 until COLS) {
				if(i<linesStack) field[i]!![j] = (if(j==hole) 0 else 1) //turn);
				else field[i]!![j] = field[i-linesStack]!![j]
			}
		}
		for(i in 0 until COLS) top[i] += linesStack
		linesStack = 0
		return true
	}

}
