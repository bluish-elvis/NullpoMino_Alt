package zeroxfc.nullpo.custom.random

import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer
import mu.nu.nullpo.game.component.Piece

class TerrorInstinctRandomizer:Randomizer() {
	private var piecePool:MutableList<Int> = mutableListOf()
	private var history:MutableList<Int> = mutableListOf()
	private var count = 0
	override fun init() {
		piecePool = MutableList(pieces.size*5) {pieces[it/5]}
		history = MutableList(4) {if(it%2==0) Piece.PIECE_S else Piece.PIECE_Z}
		count = 0
		for(i in pieces) {
			for(j in 0..4) {
				piecePool.add(i)
			}
		}
	}

	override fun next():Int {
		var idx:Int
		var id:Int
		var rolls = 0
		do {
			idx = r.nextInt(piecePool.size)
			id = piecePool[idx]
			rolls++
		} while(history.contains(id)&&rolls<MAX_ROLLS||count==0&&id==Piece.PIECE_O)
		count++
		appendHistory(id, idx)
		return id
	}

	private fun appendHistory(id:Int, idx:Int) {
		val temp = history.removeAt(0)
		history.add(id)
		piecePool.removeAt(idx)
		if(count>4) {
			piecePool.add(temp)
		}
	}

	companion object {
		private const val MAX_ROLLS = 6
	}
}