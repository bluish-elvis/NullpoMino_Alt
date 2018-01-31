package net.omegaboshi.nullpomino.game.subsystem.randomizer

class ExpDistWeightRandomizer:DistanceWeightRandomizer {

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	public override fun getWeight(i:Int):Int = if(weights[i]==0) 0 else 1 shl weights[i]-1

	public override fun isAtDistanceLimit(i:Int):Boolean = weights[i]>25
}
