/*
 * Copyright (c) 2022, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NullNoname nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

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
	fun addLines(lines:Int):Boolean {
		// check if game end
		var stk = lines
		for(i in ROWS-1 downTo ROWS-1-stk+1) {
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
				if(i<stk) field[i]!![j] = (if(j==hole) 0 else 1) //turn);
				else field[i]!![j] = field[i-stk]!![j]
			}
		}
		for(i in 0 until COLS) top[i] += stk
		stk = 0
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
