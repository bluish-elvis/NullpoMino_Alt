package net.omegaboshi.nullpomino.game.subsystem.randomizer

open class BagRandomizer:Randomizer {

	internal open val baglen:Int get() = pieces.size
	internal open var bag:IntArray = IntArray(baglen){pieces[it%pieces.size]}
	internal var pt:Int = baglen

	constructor():super()


	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)



	open fun shuffle() {
		bag = IntArray(baglen){pieces[it%pieces.size]}
		for(i in baglen downTo 2) {
			val j = r.nextInt(i)
			val temp = bag[i-1]
			bag[i-1] = bag[j]
			bag[j] = temp
		}
	}

	override fun next():Int {
		if(pt>=baglen) {
			pt = 0
			this.shuffle()
		}
		return bag[pt++]
	}
}
