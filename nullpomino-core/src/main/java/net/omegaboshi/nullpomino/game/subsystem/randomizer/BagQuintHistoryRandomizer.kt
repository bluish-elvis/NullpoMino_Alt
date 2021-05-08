package net.omegaboshi.nullpomino.game.subsystem.randomizer

class BagQuintHistoryRandomizer:BagRandomizer {
	override val noSZO = true
	override val limitPrev = true

	override val baglen:Int get() = pieces.size*5

	constructor():super()
	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)
}
