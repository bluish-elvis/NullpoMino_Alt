package net.omegaboshi.nullpomino.game.subsystem.randomizer

class MemorylessRandomizer:Randomizer {
	constructor():super()
	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)
	override fun next():Int = pieces[r.nextInt(pieces.size)]
}
