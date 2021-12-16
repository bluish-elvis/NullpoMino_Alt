package edu.cuhk.cse.fyp.tetrisai.lspi

import kotlin.jvm.JvmStatic
import java.io.IOException
import kotlin.Throws
import java.io.PrintWriter
import java.io.File
import java.io.Writer
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

object TrainerGarbageLine {
	private const val ROUNDS = 30
	private const val SPIN_STEP_DELAY = 2500
	private var out:Writer? = null
	@Throws(IOException::class) @JvmStatic fun main(args:Array<String>) {
		if(out==null) out = PrintWriter(File("weight_lambda_garbage.log"))
		val p1 = PlayerSkeleton()
		p1.learns = true
		val p2 = PlayerSkeleton()
		//p2.learns = true;
		var s1:State
		var s2:State
		var frame1:TFrame? = null
		var frame2:TFrame? = null
		val bf1 = p1.twoPlayerBasisFunctions
		val bf2 = p2.twoPlayerBasisFunctions
		var score = ""
		var consecutive_all_wins = 0
		var cnt = 0
		var tp1wins = 0
		var tp2wins = 0

		// keep on training!
		while(true) {
			var p1wins = 0
			var draw = 0
			var p2wins = 0
			var prevLength = 0
			println("Training for $ROUNDS rounds...")
			for(i in 0 until ROUNDS) {
				s1 = State()
				s2 = State()
				s1.doublePlayer = true
				s2.doublePlayer = true
				if(frame1==null) frame1 = TFrame(s1, "Player 1") else frame1.bindState(s1)
				if(frame2==null) frame2 = TFrame(s2, "Player 2") else frame2.bindState(s2)
				playGame(s1, p1, s2, p2, score, i)
				if(s1.lost&&!s2.lost) p2wins++ else if(!s1.lost&&s2.lost) p1wins++ else draw++
				tp1wins += p1wins
				tp2wins += p2wins
				val sent = s1.totalLinesSent.toDouble()/s1.turnNumber
				score = "$sent"
				print("\r  ")
				print(score)
				for(j in 0..prevLength-score.length) print(' ')
				prevLength = score.length
			}
			print("\r\nP1 wins: ")
			println(p1wins)
			print("P2 wins: ")
			println(p2wins)
			print("Draw: ")
			println(draw)
			print("P1 win rate: ")
			println(tp1wins.toDouble()/(tp1wins+tp2wins))
			if(p1wins>ROUNDS*8/10) consecutive_all_wins++ else consecutive_all_wins = 0
			if(consecutive_all_wins==3) {
				println("COPY WEIGHT")
				tp1wins = 0
				tp2wins = 0
				// copy weight
				for(i in bf2.weight.indices) {
					println("${bf1.weight[i]}"+",")
					bf2.weight[i] = bf1.weight[i]
				}
				consecutive_all_wins = 0
			}
			bf1.computeWeights()
			//for(int i=0;i<weights.length;i++) {
			//	weights[i] = 0.1 * weights[i] + 0.9 * defWeights[i];
			//	weights[i] = 0.001 * (weights[i]*(0.5 - Math.random()));
			//}
			cnt++
			if(cnt==100) {
				println("Write weight to log file")
				out!!.write("Weight:\n")
				for(i in bf1.weight.indices) {
					out!!.write(bf1.weight[i].toString())
					out!!.write('\n'.code)
				}
				out!!.write('\n'.code)
				out!!.flush()
				cnt = 0
			}
		}
	}

	private fun doNewWeightActions(weights:DoubleArray) {
		for(i in weights.indices) {
			if(i!=0) println(',')
			print('\t')
			print(weights[i])
		}
		println()
	}

	private val rotating = charArrayOf('-', '\\', '|', '/')
	private fun playGame(s1:State, p1:PlayerSkeleton, s2:State, p2:PlayerSkeleton, prevScore:String, round:Int) {
		var i = 0
		var spin = 0
		val bag = intArrayOf(0, 1, 2, 3, 4, 5, 6)
		var bag_index = 7
		while(!s1.lost&&!s2.lost) {
			if(bag_index<0||bag_index>6) {
				val rnd:Random = ThreadLocalRandom.current()
				for(k in bag.size-1 downTo 1) {
					val index = rnd.nextInt(k+1)
					// Simple swap
					val tmp = bag[index]
					bag[index] = bag[k]
					bag[k] = tmp
				}
				bag_index = 0
			}
			val nextPiece = bag[bag_index++]
			s1.nextPiece=nextPiece
			s2.nextPiece=nextPiece
			s1.makeMove(p1.pickMove(s1, s2, s1.legalMoves()))
			s2.addLinesStack(s1.linesSent)
			//s1.draw();
			//s1.drawNext(0,0);
			//s2.draw();
			//s2.drawNext(0,0);
			//String input1 = System.console().readLine();
			if(!s2.lost) {
				s2.makeMove(p2.pickMove(s2, s1, s2.legalMoves()))
				s1.addLinesStack(s2.linesSent)
				//s1.draw();
				//s1.drawNext(0,0);
				//s2.draw();
				//s2.drawNext(0,0);
				//String input2 = System.console().readLine();
			}
			if(i==SPIN_STEP_DELAY) {
				print("\r")
				print(rotating[spin])
				print(" Round ")
				print(round)
				print(": ")
				print(prevScore)
				spin = (spin+1)%rotating.size
				i = 0
			}
			i++
		}
		s1.draw()
		s1.drawNext(0, 0)
		s2.draw()
		s2.drawNext(0, 0)
	}

	private fun drawBoard(s:State, t:TFrame) {
		t.bindState(s)
		s.draw()
		s.drawNext(0, 0)
	}
}
