package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece

class BagNoSZORandomizer:BagRandomizer {

	private var firstBag:Boolean = true

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	override fun shuffle() {
		do super.shuffle()
		while((firstBag&&!isPieceSZOOnly)&&
			(bag[0]==Piece.PIECE_O||bag[0]==Piece.PIECE_Z||bag[0]==Piece.PIECE_S))
		firstBag = false
	}

}
