package net.omegaboshi.nullpomino.game.subsystem.randomizer

class BagMinusTwoRandomizer:BagRandomizer {
	override val baglen:Int get() = maxOf(0, super.baglen-2)
	override val bagInit:IntArray
		get() {
			val cut = MutableList(super.baglen){it}.shuffled().toIntArray().take(2)
			return IntArray(super.baglen) {it%pieces.size}.filterIndexed {i, _ -> cut.all {it != i}}.toIntArray()
		}
	constructor():super()
	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)
}
