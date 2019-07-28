package mu.nu.nullpo.tool.airankstool

import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.subsystem.ai.RanksAI
import net.omegaboshi.nullpomino.game.subsystem.randomizer.History4RollsRandomizer
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer
import java.util.*

class AIRanksTester(private val numTries:Int) {
	private var randomizer:Randomizer? = null
	private val ranksAI:RanksAI = RanksAI()
	private var pieces:IntArray = IntArray(0)
	private var totalPieces:Int = 0

	private fun init() {
		val pieceEnable = BooleanArray(Piece.PIECE_COUNT)
		for(i in 0 until Piece.PIECE_STANDARD_COUNT)
			pieceEnable[i] = true
		val seed = Random().nextLong()

		randomizer = History4RollsRandomizer(pieceEnable, seed).apply {init()}

		pieces = IntArray(6) {randomizer!!.next()}

		ranksAI.initRanks()
	}

	private fun incrementPieces() {

		System.arraycopy(pieces, 1, pieces, 0, 5)
		pieces[5] = randomizer!!.next()

	}

	fun test() {
		for(i in 0 until numTries) {
			val tempTotalPieces = totalPieces
			playGame()
			println("Game : $i Pieces : ${totalPieces-tempTotalPieces} Cumulated average : ${totalPieces/(i+1)}")
		}
	}

	private fun playGame() {
		init()
		/* if ((pieces[0]==Piece.PIECE_S) || (pieces[0]==Piece.PIECE_Z) ||
 * (pieces[0]==Piece.PIECE_O)){
 * //System.out.println("Suchec !!");
 * } */
		val heights = IntArray(9)
		val holdPiece = intArrayOf(-1)
		val holdOK = booleanArrayOf(true)
		while(!ranksAI.isGameOver) {
			//holdOK[0]=false;
			totalPieces++
			//System.out.println(Arrays.toString(heights));
			ranksAI.playFictitiousMove(heights, pieces, holdPiece, holdOK)
			incrementPieces()

		}
	}

	companion object {
		@JvmStatic
		fun main(args:Array<String>) {

			val tester = AIRanksTester(100)
			tester.test()
		}
	}
}
