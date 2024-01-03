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
import kotlin.random.Random

abstract class Randomizer {
	protected var seed:Long = 0L; private set//Random.Default.nextLong()
	protected var r:Random = Random(seed); private set
	/**enabled pieces*/
	var pieces = Shape.all.map {it.ordinal}
		private set

	protected val isPieceSZOOnly:Boolean
		get() = pieces.all {p -> listOf(Shape.S, Shape.Z, Shape.O).any {it.ordinal==p}}

	abstract operator fun next():Int
	abstract fun init()

	/**
	 * This is overridden as changing piece count screws with the counter averages.<br></br>
	 * Therefore this calls `init()`.
	 *
	 * @param setPieces Array of enabled pieces.
	 */
	fun setState(setPieces:Collection<Int>, seed:Long = this.seed) {
		this.seed = seed
		r = Random(seed)
		pieces = setPieces.filter {i -> Shape.all.map {it.ordinal}.contains(i)}
		init()
	}
	@JvmOverloads @JvmName("setStateBooleans")
	fun setState(pieceEnable:List<Boolean>, seed:Long = this.seed) =
		setState(Shape.all.map {it.ordinal}.filter {pieceEnable[it]}, seed)
	@JvmOverloads @JvmName("setStateShape")
	fun setState(pieceEnable:Collection<Shape>, seed:Long = this.seed) = setState(pieceEnable.map {it.ordinal}, seed)
}
