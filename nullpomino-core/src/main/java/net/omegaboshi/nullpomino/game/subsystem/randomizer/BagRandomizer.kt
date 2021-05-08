package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece

open class BagRandomizer:Randomizer {
	open val noSZO = false
	open val limitPrev = false

	internal open val baglen:Int get() = pieces.size
	private var bag:IntArray = IntArray(0)
	internal open val bagInit:IntArray get() = IntArray(baglen) {pieces[it%pieces.size]}

	private var pt:Int = pieces.size
	private var isfirst = true

	constructor():super()
	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	override fun init() {
		isfirst = true
		shuffle()
	}

	open fun shuffle() {
		val tmp = bagInit.toMutableList()
		bag = IntArray(0)
		pt = 0
		while(tmp.isNotEmpty()) {
			var i = 0
			do i = r.nextInt(tmp.size)
			while(if(tmp.size==baglen) noSZO&&isfirst&&(tmp[i]==Piece.Shape.S.ordinal||tmp[i]==Piece.Shape.Z.ordinal||tmp[i]==Piece.Shape.O.ordinal)
				else limitPrev&&bag.takeLast(minOf(4, maxOf(0, tmp.size-1))).any {it==tmp[i]})
			isfirst = false
			bag += tmp.removeAt(i)
		}
	}

	override fun next():Int {
		if(pt>=bag.size) {
			pt = 0
			shuffle()
		}
		return bag[pt++]
	}

}
