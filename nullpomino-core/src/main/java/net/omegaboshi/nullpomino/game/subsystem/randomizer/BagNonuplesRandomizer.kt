package net.omegaboshi.nullpomino.game.subsystem.randomizer

class BagNonuplesRandomizer:BagRandomizer {

	override var baglen:Int = pieces.size*9

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

}
