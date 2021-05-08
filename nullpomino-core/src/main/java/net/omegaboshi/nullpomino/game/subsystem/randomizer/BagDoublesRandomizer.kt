package net.omegaboshi.nullpomino.game.subsystem.randomizer

class BagDoublesRandomizer:BagRandomizer {
	override val baglen:Int get() = pieces.size*2

	constructor():super()
	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)
}
