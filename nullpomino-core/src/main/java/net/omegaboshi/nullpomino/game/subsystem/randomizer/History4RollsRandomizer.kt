package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece

class History4RollsRandomizer:LimitedHistoryRandomizer {

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	init {
		history = intArrayOf(Piece.PIECE_Z, Piece.PIECE_Z, Piece.PIECE_Z, Piece.PIECE_Z)
		numrolls = 4
	}
}
