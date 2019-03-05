package net.omegaboshi.nullpomino.game.subsystem.randomizer

class BagMinusTwoRandomizer:BagRandomizer {

	override val baglen:Int get() = maxOf(1, pieces.size-2)

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

}
