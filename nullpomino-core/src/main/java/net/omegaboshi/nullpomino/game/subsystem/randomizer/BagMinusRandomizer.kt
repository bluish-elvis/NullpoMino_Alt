package net.omegaboshi.nullpomino.game.subsystem.randomizer

class BagMinusRandomizer:BagRandomizer {
	override val baglen:Int get() = maxOf(0, super.baglen-1)
	override val bagInit:IntArray
		get() {
			val cut = r.nextInt(super.baglen)
			return IntArray(super.baglen) {it%pieces.size}.filterIndexed {i, _ -> i!=cut}.toIntArray()
		}

	constructor():super()
	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)
}
