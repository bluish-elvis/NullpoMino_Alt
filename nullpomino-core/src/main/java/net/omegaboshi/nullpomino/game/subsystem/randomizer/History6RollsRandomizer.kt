package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece

class History6RollsRandomizer:LimitedHistoryRandomizer {

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	init {
		history = intArrayOf(Piece.PIECE_S, Piece.PIECE_Z, Piece.PIECE_S, Piece.PIECE_Z)
		numrolls = 6
	}
}
