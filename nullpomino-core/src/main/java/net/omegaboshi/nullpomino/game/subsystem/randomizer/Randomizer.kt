package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece.Shape
import kotlin.random.Random

abstract class Randomizer {

	protected var r:Random = Random.Default
	var pieces:IntArray = Shape.values().map {it.ordinal}.toIntArray()
		private set

	protected val isPieceSZOOnly:Boolean
		get() = pieces.all {p -> listOf(Shape.S, Shape.Z, Shape.O).any {it.ordinal==p}}

	constructor()

	constructor(pieceEnable:BooleanArray, seed:Long) {
		setState(pieceEnable, seed)
	}

	abstract operator fun next():Int

	fun setState(pieceEnable:BooleanArray, seed:Long) {
		setPieceEnable(pieceEnable)
		r = Random(seed)
	}

	open fun init() {}
	/**
	 * This is overridden as changing piece count screws with the counter averages.<br></br>
	 * Therefore this calso calls `init()`.
	 *
	 * @param pieceEnable Array of enabled pieces.
	 */
	fun setPieceEnable(pieceEnable:BooleanArray) {
		pieces = Shape.values().map {it.ordinal}.filter {pieceEnable[it]}.toIntArray()
		init()
	}

}
