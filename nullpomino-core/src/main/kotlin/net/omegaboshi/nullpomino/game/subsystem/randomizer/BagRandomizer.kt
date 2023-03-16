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

open class BagRandomizer:Randomizer {
	open val noSZO = false
	open val limitPrev = false

	internal open val baglen:Int get() = pieces.size
	private var bag = MutableList(0) {0}
	internal open val bagInit get() = List(baglen) {pieces[it%pieces.size]}

	private var pt:Int = pieces.size
	private var isFirst = true

	constructor():super()
	constructor(pieceEnable:List<Boolean>, seed:Long):super(pieceEnable, seed)

	override fun init() {
		isFirst = true
		bag = bagInit.toMutableList()
		shuffle()
	}

	open fun shuffle() {
		val tmp = bagInit.toMutableList()
		bag.clear()
		pt = 0
		while(tmp.isNotEmpty()) {
			var i = 0
			do i = r.nextInt(tmp.size)
			while(if(tmp.size==baglen) !isPieceSZOOnly&&noSZO&&isFirst&&(tmp[i]==Piece.Shape.S.ordinal||tmp[i]==Piece.Shape.Z.ordinal||tmp[i]==Piece.Shape.O.ordinal)
				else limitPrev&&bag.takeLast(minOf(4, maxOf(0, tmp.size-1))).any {it==tmp[i]})
			isFirst = false
			bag += tmp.removeAt(i)
		}
	}

	override fun next():Int {
		if(pt>=bag.size) {
			pt = 0
			shuffle()
		}
		return bag[pt++]
	}

}
