package net.omegaboshi.nullpomino.game.subsystem.randomizer

class BagDoublesRandomizer:BagRandomizer {

	override var baglen:Int = pieces.size*2

	constructor():super()
	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

}
