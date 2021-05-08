package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece

class StrictHistoryRandomizer:LimitedHistoryRandomizer {
	constructor():super()
	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	init {
		strict = true
		init()
	}

	override fun init() {
		super.init()
		history = intArrayOf(Piece.PIECE_S, Piece.PIECE_Z, Piece.PIECE_O, Piece.PIECE_O)
	}
	/*
	override fun next():Int {
		for(i in pieces.indices) curHist[i] = false
		numDistinctCurHist = 0
		for(i in 0 until histLen)
			if(!curHist[history[i]]) {
				curHist[history[i]] = true
				numDistinctCurHist++
			}
		notHistPos = 0
		for(i in pieces.indices)
			if(!curHist[i]) {
				notHist[notHistPos] = i
				notHistPos++
			}
		id = notHist[r.nextInt(notHistPos)]
		System.arraycopy(history, 0, history, 1, histLen-1)
		history[0] = pieces[id]
		return pieces[id]
	}*/
}
