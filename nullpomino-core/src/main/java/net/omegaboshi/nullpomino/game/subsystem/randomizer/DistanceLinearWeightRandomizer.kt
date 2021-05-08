package net.omegaboshi.nullpomino.game.subsystem.randomizer

class DistanceLinearWeightRandomizer:DistanceWeightRandomizer {
	constructor():super()
	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	public override fun getWeight(i:Int):Int = weights[i]
	public override fun isAtDistanceLimit(i:Int):Boolean = false
}
