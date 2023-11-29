/*
 * Copyright (c) 2010-2022, NullNoname
 * Kotlin converted and modified by Venom=Nhelv.
 * THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
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

package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece

abstract class LimitedHistoryRandomizer:Randomizer() {
	internal val history = mutableListOf<Int>()
	private var historySZ:Int? = null
	private var historyJL:Int? = null
	internal open val numRolls = 0

	private val firstPiece get() = history.isEmpty()

	init {
		history.clear()
	}

	override fun init() {
		history.clear()
	}

	override fun next():Int {
		var p = when {
			firstPiece&&!isPieceSZOOnly -> (pieces-setOf(Piece.PIECE_O, Piece.PIECE_Z, Piece.PIECE_S)).random(r)
			numRolls<0 -> (pieces-history.toSet()).random(r)
			else -> {
				var i = 0
				repeat(1+numRolls) {
					i = pieces.random(r)
					if(history.take(4).none {it==i}) return@repeat
				}
				i
			}
		}
		if(p==Piece.PIECE_S||p==Piece.PIECE_Z) {
			if(p==historySZ) p = if(historySZ==Piece.PIECE_S) Piece.PIECE_Z else Piece.PIECE_S
			historySZ = p
		}
		if(p==Piece.PIECE_J||p==Piece.PIECE_L) {
			if(p==historyJL) p = if(p==Piece.PIECE_J) Piece.PIECE_L else Piece.PIECE_J
			historyJL = p
		}
		history.addFirst(p)
		history.removeAt(4)
		return p
	}
}
