package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece.Shape
import java.util.*

abstract class Randomizer {

	protected var r:Random = Random()
	var pieces:IntArray = Shape.values().map {it.ordinal}.toIntArray()

	protected val isPieceSZOOnly:Boolean
		get() = pieces.all {p -> listOf(Shape.S, Shape.Z, Shape.O).any {it.ordinal==p}}

	constructor()

	constructor(pieceEnable:BooleanArray, seed:Long) {
		setState(pieceEnable, seed)
	}

	init {
	}

	abstract operator fun next():Int

	fun setState(pieceEnable:BooleanArray, seed:Long) {
		setPieceEnable(pieceEnable)
		r = Random(seed)
	}

	fun setPieceEnable(pieceEnable:BooleanArray) {
		pieces = Shape.values().map {it.ordinal}.filter {pieceEnable[it]}.toIntArray()
	}

}
