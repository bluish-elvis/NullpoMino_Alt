package net.omegaboshi.nullpomino.game.subsystem.randomizer

open class BagBonusRandomizer:BagRandomizer {

	override val baglen:Int get() = pieces.size+1
	internal open var bonus:Int = pieces.size

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	override fun shuffle() {
		bonus = pieces.size
		bag[bonus] = r.nextInt(pieces.size)
		for(i in baglen downTo 2) {
			val j = r.nextInt(i)
			val temp = bag[i-1]
			bag[i-1] = bag[j]
			bag[j] = temp
			if(bonus==i-1) bonus = j
			else if(bonus==j) bonus = i-1
		}
	}
}
