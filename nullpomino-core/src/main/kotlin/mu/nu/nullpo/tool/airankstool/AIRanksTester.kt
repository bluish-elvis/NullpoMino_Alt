/*
 * Copyright (c) 2010-2021, NullNoname
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

package mu.nu.nullpo.tool.airankstool

import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.subsystem.ai.RanksAI
import net.omegaboshi.nullpomino.game.subsystem.randomizer.History4RollsRandomizer
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer
import kotlin.random.Random

class AIRanksTester(private val numTries:Int) {
	private var randomizer:Randomizer? = null
	private val ranksAI:RanksAI = RanksAI()
	private var pieces = IntArray(0)
	private var totalPieces = 0

	private fun init() {
		val pieceEnable = BooleanArray(Piece.PIECE_COUNT)
		for(i in 0 until Piece.PIECE_STANDARD_COUNT)
			pieceEnable[i] = true
		val seed = Random.Default.nextLong()

		randomizer = History4RollsRandomizer(pieceEnable, seed).apply {init()}

		pieces = IntArray(6) {randomizer!!.next()}

		ranksAI.initRanks()
	}

	private fun incrementPieces() {

		System.arraycopy(pieces, 1, pieces, 0, 5)
		pieces[5] = randomizer!!.next()

	}

	fun test() {
		for(i in 0 until numTries) {
			val tempTotalPieces = totalPieces
			playGame()
			println("Game : $i Pieces : ${totalPieces-tempTotalPieces} Cumulated average : ${totalPieces/(i+1)}")
		}
	}

	private fun playGame() {
		init()
		/* if ((pieces[0]==Piece.PIECE_S) || (pieces[0]==Piece.PIECE_Z) ||
 * (pieces[0]==Piece.PIECE_O)){
 * //System.out.println("Suchec");
 * } */
		val heights = IntArray(9)
		val holdPiece = intArrayOf(-1)
		val holdOK = booleanArrayOf(true)
		while(!ranksAI.isGameOver) {
			//holdOK[0]=false;
			totalPieces++
			//System.out.println(Arrays.toString(heights));
			ranksAI.playFictitiousMove(heights, pieces, holdPiece, holdOK)
			incrementPieces()

		}
	}

	companion object {
		@JvmStatic
		fun main(args:Array<String>) {

			val tester = AIRanksTester(100)
			tester.test()
		}
	}
}
