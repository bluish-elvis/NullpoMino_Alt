package net.omegaboshi.nullpomino.game.subsystem.randomizer

open class BagRandomizer:Randomizer {

	internal open var baglen:Int = pieces.size
	internal open var bag:IntArray = IntArray(baglen){pieces[it%pieces.size]}
	internal var pt:Int = 0

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	init {
		shuffle()
	}

	open fun shuffle() {
		for(i in baglen downTo 2) {
			val j = r.nextInt(i)
			val temp = bag[i-1]
			bag[i-1] = bag[j]
			bag[j] = temp
		}
	}

	override fun next():Int {
		val id = bag[pt]
		pt++
		if(pt==baglen) {
			pt = 0
			shuffle()
		}
		return id
	}
}
