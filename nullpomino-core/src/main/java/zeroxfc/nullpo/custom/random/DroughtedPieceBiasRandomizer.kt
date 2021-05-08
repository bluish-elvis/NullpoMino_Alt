package zeroxfc.nullpo.custom.random

import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer
import mu.nu.nullpo.game.component.Piece

class DroughtedPieceBiasRandomizer:Randomizer() {
	var counters:IntArray = IntArray(pieces.size)
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