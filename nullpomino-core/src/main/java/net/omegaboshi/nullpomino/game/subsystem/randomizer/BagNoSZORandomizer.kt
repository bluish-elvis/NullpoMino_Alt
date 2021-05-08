package net.omegaboshi.nullpomino.game.subsystem.randomizer

class BagNoSZORandomizer:BagRandomizer {
	override val noSZO = true
	constructor():super()
	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)
}
