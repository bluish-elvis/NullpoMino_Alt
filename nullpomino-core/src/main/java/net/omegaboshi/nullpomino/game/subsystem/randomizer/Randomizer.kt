package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece
import java.util.*

abstract class Randomizer {

	protected var r:Random = Random()
	var pieces:IntArray = IntArray(Piece.PIECE_COUNT){it}

	protected val isPieceSZOOnly:Boolean
		get() = pieces.all{it==Piece.PIECE_O||it==Piece.PIECE_Z||it==Piece.PIECE_S}

	constructor()

	constructor(pieceEnable:BooleanArray, seed:Long) {
		setState(pieceEnable, seed)
	}

	init {}

	abstract operator fun next():Int

	fun setState(pieceEnable:BooleanArray, seed:Long) {
		setPieceEnable(pieceEnable)
		r = Random(seed)
	}

	fun setPieceEnable(pieceEnable:BooleanArray) {
		pieces = IntArray(pieceEnable.count{it})
		var piece = 0
		for(i in 0 until Piece.PIECE_COUNT)
			if(pieceEnable[i]) {
				pieces[piece] = i
				piece++
			}
	}

}
