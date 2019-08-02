package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece.Shape

abstract class LimitedHistoryRandomizer:Randomizer {

	internal var history:IntArray = IntArray(0)
	private var historySZ:Int? = null
	private var historyJL:Int? = null
	internal var id:Int = 0
	internal var numrolls:Int = 0

	internal var firstPiece:Boolean = true
	internal var strict:Boolean = false

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	init {
		firstPiece = true
	}

	override fun next():Int {
		if(firstPiece&&!isPieceSZOOnly) {
			pieces.subtract(listOf(Shape.O, Shape.Z, Shape.S).map{it.ordinal}).toList().let {id = it[r.nextInt(it.size)]}
			firstPiece = false
		} else if(strict)
			pieces.subtract(history.toList()).toList().let {id = it[r.nextInt(it.size)]}
		else for(i in 0 until numrolls) {
			id = r.nextInt(pieces.size)
			if(history.take(4).none {it==id}) break
		}
		if(pieces[id]==Shape.S.ordinal||pieces[id]==Shape.Z.ordinal) {
			if(id==historySZ)id = if(historySZ==Shape.S.ordinal) Shape.Z.ordinal else Shape.S.ordinal
			historySZ = id
		}
		if(pieces[id]==Shape.J.ordinal||pieces[id]==Shape.L.ordinal) {
			if(id==historyJL) id = if(id==Shape.J.ordinal) Shape.L.ordinal else Shape.J.ordinal
			historyJL = id
		}
		System.arraycopy(history, 0, history, 1, 3)
		history[0] = pieces[id]
		return pieces[id]
	}
}
