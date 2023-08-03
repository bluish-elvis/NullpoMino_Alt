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

import mu.nu.nullpo.game.component.Piece.Shape

abstract class LimitedHistoryRandomizer:Randomizer {
	internal var history = IntArray(0)
	private var historySZ:Int? = null
	private var historyJL:Int? = null
	internal var id = 0
	internal var numrolls = 0

	private var firstPiece = true
	internal var strict = false

	constructor():super()

	constructor(pieceEnable:List<Boolean>, seed:Long):super(pieceEnable, seed)

	init {
		firstPiece = true
	}

	override fun init() {
		firstPiece = true
	}

	override fun next():Int {
		if(firstPiece&&!isPieceSZOOnly) {
			pieces.subtract(setOf(Shape.O, Shape.Z, Shape.S).map {it.ordinal}.toSet()).toList().let {id = it[r.nextInt(it.size)]}
			firstPiece = false
		} else if(strict)
			pieces.subtract(history.toSet()).toList().let {id = it[r.nextInt(it.size)]}
		else for(i in 0..<numrolls) {
			id = r.nextInt(pieces.size)
			if(history.take(4).none {it==id}) break
		}
		if(pieces[id]==Shape.S.ordinal||pieces[id]==Shape.Z.ordinal) {
			if(id==historySZ) id = if(historySZ==Shape.S.ordinal) Shape.Z.ordinal else Shape.S.ordinal
			historySZ = id
		}
		if(pieces[id]==Shape.J.ordinal||pieces[id]==Shape.L.ordinal) {
			if(id==historyJL) id = if(id==Shape.J.ordinal) Shape.L.ordinal else Shape.J.ordinal
			historyJL = id
		}
		System.arraycopy(history, 0, history, 1, 3)
		history[0] = pieces[id]
		return pieces[id]
	}
}
