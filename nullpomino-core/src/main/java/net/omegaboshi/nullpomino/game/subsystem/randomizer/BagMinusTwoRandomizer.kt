package net.omegaboshi.nullpomino.game.subsystem.randomizer

class BagMinusTwoRandomizer:BagRandomizer {

	override var baglen:Int = maxOf(1, pieces.size-2)

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

}
