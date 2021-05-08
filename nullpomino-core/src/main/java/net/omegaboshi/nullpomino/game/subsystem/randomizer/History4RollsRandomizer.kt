package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece

class History4RollsRandomizer:LimitedHistoryRandomizer {
	constructor():super()
	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	init {
		numrolls = 4
		init()
	}

	override fun init() {
		super.init()
		history = intArrayOf(Piece.PIECE_Z, Piece.PIECE_Z, Piece.PIECE_Z, Piece.PIECE_Z)
	}
}
