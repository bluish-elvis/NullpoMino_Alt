package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.component.Piece.Shape
import java.io.*
import java.util.*

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
		sequenceTranslated = IntArray(sequence.toString().length){pieceCharToId(sequence.toString()[it])}
		println(Arrays.toString(sequenceTranslated))

	}

	private fun pieceCharToId(c:Char):Int {
		var i = 0
		while(i<Piece.PIECE_STANDARD_COUNT) {
			if(c==Shape.names[i][0]) break
			i++
		}
		return i
	}

	override fun next():Int = sequenceTranslated[++id%sequenceTranslated.size]

}
