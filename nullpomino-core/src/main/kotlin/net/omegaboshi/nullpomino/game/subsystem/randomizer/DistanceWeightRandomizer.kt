/*
 Copyright (c) 2010-2024, NullNoname
 All rights reserved.

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis
 THIS IS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
     * Neither the name of NullNoname nor the names of its
       contributors may be used to endorse or promote products derived from
       this software without specific prior written permission.

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

package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece.Shape

abstract class DistanceWeightRandomizer:Randomizer() {
	private val initWeights = intArrayOf(3, 3, 0, 0, 3, 3, 0, 2, 2, 2, 2)
	internal var weights = IntArray(pieces.size)
	private var cumulative = IntArray(pieces.size)
	internal var sum = 0
	internal var id = 0

	private var firstPiece = true

	init {
		init()
	}

	final override fun init() {
		for(i in pieces.indices)
			weights[i] = initWeights[pieces[i]]
	}

	override fun next():Int {
		sum = 0
		for(i in pieces.indices) {
			sum += getWeight(i)
			cumulative[i] = sum
		}
		id = r.nextInt(sum)
		for(i in pieces.indices)
			if(id<cumulative[i]) {
				id = i
				break
			}
		weights[id] = 0
		for(i in pieces.indices)
			if(firstPiece&&pieces[i]==Shape.O.ordinal)
				weights[i] = 3
			else if(!isAtDistanceLimit(i)) weights[i]++
		firstPiece = false
		return id
	}

	protected abstract fun getWeight(i:Int):Int

	protected abstract fun isAtDistanceLimit(i:Int):Boolean

}
