package net.omegaboshi.nullpomino.game.subsystem.randomizer

open class BagBonusRandomizer:BagRandomizer {
	override val baglen:Int get() = super.baglen+1
	override val bagInit:IntArray
		get() = IntArray(baglen) {pieces[(if(it==baglen-1) r.nextInt(pieces.size) else it)%pieces.size]}

	constructor():super()
	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)
}
