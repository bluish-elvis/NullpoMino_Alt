package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece

class BagBonusBagRandomizer:BagRandomizer {

	private var bonusbag:IntArray = IntArray(0)
	private var bonuspt:Int = pieces.size
	override val baglen:Int get() = pieces.size+1
	override val bagInit:IntArray
		get() = IntArray(baglen) {
			if(bonuspt>=bonusbag.size) {
				bonuspt = 0

				val tmp = IntArray(pieces.size){i->pieces[i]}.toMutableList()
				bonusbag = IntArray(0)
				while(tmp.isNotEmpty()) {
					var i:Int
					do i = r.nextInt(tmp.size)
					while(if(tmp.size==baglen-1) noSZO&&(tmp[i]==Piece.Shape.S.ordinal||tmp[i]==Piece.Shape.Z.ordinal||tmp[i]==Piece.Shape.O.ordinal)
						else limitPrev&&bonusbag.takeLast(minOf(4,maxOf(0,tmp.size-1))).any {it==tmp[i]})
					bonusbag += tmp.removeAt(i)
				}
			}
			if(it==baglen) bonusbag[bonuspt++] else pieces[it%pieces.size]
		}

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

}
