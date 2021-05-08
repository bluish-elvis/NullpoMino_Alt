package net.omegaboshi.nullpomino.game.subsystem.randomizer

class BagNonuplesRandomizer:BagRandomizer {
	override val baglen:Int get() = pieces.size*9
	constructor():super()
	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)
}
