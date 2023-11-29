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

open class BagRandomizer:Randomizer() {
	/** no queue first */
	open val noSZO = false
	/** Number of previous pieces to check for duplicates */
	open val limitPrev = 0

	internal open val bagLen:Int get() = pieces.size
	private val bag = MutableList(0) {0}
	internal open val bagInit get() = List(bagLen) {pieces[it%pieces.size]}

	private var pt:Int = pieces.size
	private var isFirst = true

	override fun init() {
		isFirst = true
		shuffle()
	}

	private fun shuffle() {
		val tmp = bagInit.toMutableList()
		bag.clear()
		pt = 0
		while(tmp.isNotEmpty()) {
			var i:Int
			var c = 0
			do {
				i = tmp.indices.random(r)
				c++
			} while(if(tmp.size==bagLen) !isPieceSZOOnly&&noSZO&&isFirst&&(tmp[i]==Piece.PIECE_S||tmp[i]==Piece.PIECE_Z||tmp[i]==Piece.PIECE_O)
				else c<limitPrev&&bag.takeLast(minOf(4, maxOf(0, tmp.size-1))).contains(tmp[i]))
			isFirst = false
			bag += tmp.removeAt(i)
		}
	}

	override fun next():Int {
		if(pt>=bag.size) shuffle()
		return bag[pt++]
	}

}
