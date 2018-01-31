package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece

class StrictHistoryRandomizer:Randomizer {

	internal var history:IntArray = intArrayOf(Piece.PIECE_S, Piece.PIECE_Z, Piece.PIECE_O, Piece.PIECE_O)
	internal var id:Int = 0

	private var curHist:BooleanArray = BooleanArray(pieces.size){false}
	private var numDistinctCurHist:Int = 0
	private var notHist:IntArray = IntArray(pieces.size)
	private var notHistPos:Int = 0
	private var histLen:Int = minOf(4, pieces.size-1)

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

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
	}
}
