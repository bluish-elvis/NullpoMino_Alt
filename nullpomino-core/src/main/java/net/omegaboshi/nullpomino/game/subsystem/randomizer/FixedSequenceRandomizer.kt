package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece
import java.io.*

class FixedSequenceRandomizer:Randomizer {

	private var sequenceTranslated:IntArray = IntArray(0)
	private var id = -1

	constructor():super()
	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	init {
		val sequence= StringBuffer()
		val file = File("sequence.txt")
		val reader = BufferedReader(FileReader(file))
		try {
			// repeat until all lines is read
			reader.readLines().forEach {sequence.append(it)}
		} catch(e:IOException) {
			e.printStackTrace()
		} finally {
			try {
				reader.close()
			} catch(e:IOException) {
				e.printStackTrace()
			}

		}
		sequenceTranslated = IntArray("$sequence".length){pieceCharToId("$sequence"[it])}
		println(sequenceTranslated.contentToString())

	}

	private fun pieceCharToId(c:Char):Int {
		var i = 0
		while(i<Piece.PIECE_STANDARD_COUNT) {
			if(c==Piece.Shape.names[i][0]) break
			i++
		}
		return i
	}

	override fun next():Int = sequenceTranslated[++id%sequenceTranslated.size]

}
