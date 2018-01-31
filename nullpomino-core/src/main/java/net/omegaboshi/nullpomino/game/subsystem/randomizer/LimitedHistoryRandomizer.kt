package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece

abstract class LimitedHistoryRandomizer:Randomizer {

	internal var history:IntArray = IntArray(0)
	internal var id:Int = 0
	internal var numrolls:Int = 0

	internal var firstPiece:Boolean = false

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	init {
		firstPiece = true
	}

	override fun next():Int {
		if(firstPiece&&!isPieceSZOOnly) {
			do
				id = r.nextInt(pieces.size)
			while(pieces[id]==Piece.PIECE_O||pieces[id]==Piece.PIECE_Z||pieces[id]==Piece.PIECE_S)
			firstPiece = false
		} else
			for(i in 0 until numrolls) {
				id = r.nextInt(pieces.size)
				if(!(pieces[id]==history[0]||pieces[id]==history[1]||pieces[id]==history[2]||pieces[id]==history[3])) break
			}
		System.arraycopy(history, 0, history, 1, 3)
		history[0] = pieces[id]
		return pieces[id]
	}
}
