package net.omegaboshi.nullpomino.game.subsystem.randomizer

class BagMinusRandomizer:BagRandomizer {

	override var baglen:Int = maxOf(1, pieces.size-1)

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

}
