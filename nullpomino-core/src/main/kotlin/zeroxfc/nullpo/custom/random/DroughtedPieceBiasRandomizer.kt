/*
 * Copyright (c) 2021-2021,
 * This library class was created by 0xFC963F18DC21 / Shots243
 * It is part of an extension library for the game NullpoMino (copyright 2021-2021)
 *
 * Kotlin converted and modified by Venom=Nhelv
 *
 * Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 * Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.
 *
 * THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 *
 * Repository: https://github.com/Shots243/ModePile
 *
 * When using this library in a mode / library pack of your own, the following
 * conditions must be satisfied:
 *     - This license must remain visible at the top of the document, unmodified.
 *     - You are allowed to use this library for any modding purpose.
 *         - If this is the case, the Library Creator must be credited somewhere.
 *             - Source comments only are fine, but in a README is recommended.
 *     - Modification of this library is allowed, but only in the condition that a
 *       pull request is made to merge the changes to the repository.
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

package zeroxfc.nullpo.custom.random

import mu.nu.nullpo.game.component.Piece
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer

class DroughtedPieceBiasRandomizer:Randomizer() {
	var counters = IntArray(pieces.size)
	var history:MutableList<Int> = mutableListOf(Piece.PIECE_O, Piece.PIECE_S, Piece.PIECE_Z, Piece.PIECE_O)

	override fun init() {
		counters = IntArray(pieces.size)
		history = mutableListOf(Piece.PIECE_O, Piece.PIECE_S, Piece.PIECE_Z, Piece.PIECE_O)
	}

	override fun next():Int {
		if(pieces.size==1) return pieces[0]
		var v = -1
		var divisor = 0.0
		val rawChances = DoubleArray(pieces.size)
		val finalChances = DoubleArray(pieces.size)
		var total = 0

		// Get total pieces rolled
		for(i in counters) total += i

		// Get raw chances and the final divisor for the values
		for(i in pieces.indices) {
			rawChances[i] = 1-counters[i].toDouble()/total
			divisor += rawChances[i]
		}

		// Find final values
		for(i in pieces.indices) {
			var chance = rawChances[i]/divisor
			for(j in 0 until i) {
				chance += rawChances[j]/divisor
			}
			finalChances[i] = chance
		}

		// Roll up to three times to get piece that is not in history
		for(i in 0 until if(total<INITIAL_PIECES) ROLLS_INITIAL else ROLLS) {
			val `val` = r.nextDouble()
			var idx = 0
			for(j in 1 until pieces.size) {
				if(`val`<finalChances[j]&&`val`>=finalChances[j-1]) idx = j
			}
			v = pieces[idx]
			if(!history.contains(v)) {
				counters[idx]++
				break
			} else if(i==2) counters[idx]++
		}

		// Add piece to history
		appendHistory(v)
		return v
	}

	private fun appendHistory(i:Int) {
		history.removeAt(0)
		history.add(i)
	}

	companion object {
		private const val ROLLS_INITIAL = 6
		private const val ROLLS = 3
		private const val INITIAL_PIECES = 4
	}
}
