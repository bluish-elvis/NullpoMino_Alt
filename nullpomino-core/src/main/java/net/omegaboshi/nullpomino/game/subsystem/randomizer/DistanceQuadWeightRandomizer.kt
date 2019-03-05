package net.omegaboshi.nullpomino.game.subsystem.randomizer

class DistanceQuadWeightRandomizer:DistanceWeightRandomizer {

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	public override fun getWeight(i:Int):Int = weights[i]*weights[i]

	public override fun isAtDistanceLimit(i:Int):Boolean = false

}
