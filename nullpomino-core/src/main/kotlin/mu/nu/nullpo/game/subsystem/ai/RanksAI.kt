/*
 * Copyright (c) 2010-2022, NullNoname
 * Kotlin converted and modified by Venom=Nhelv.
 * THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NullNoname nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package mu.nu.nullpo.game.subsystem.ai

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.tool.airankstool.AIRanksConstants
import mu.nu.nullpo.tool.airankstool.Ranks
import mu.nu.nullpo.util.CustomProperties
import org.apache.logging.log4j.LogManager
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream
import kotlin.math.abs

open class RanksAI:DummyAI(), Runnable {

	private var ranks:Ranks? = null
	private var skipNextFrame = false

	private var currentHeightMin = 0
	private var currentHeightMax = 0
	private var previewsMax = 2

	private var heights = mutableListOf<Int>()
	private var bestScore:Score = Score()
	/** Tells other classes if the fictitious game is over
	 * @return true if fictitious game is over
	 */
	var isGameOver = false
		private set
	private var plannedToUseIPiece = false
	private var currentRanksFile:String? = ""
	private var allowHold = false
	private var speedLimit = 0

	override val name = "RANKSAI"

	/** Get max think level
	 * @return Max think level (1 in this AI)
	 */
	val maxThinkDepth:Int
		get() = 1
	/** When true,To threadThink routineInstructing the execution of the  */
	protected var thinkRequest = false

	inner class Score {

		var rankStacking = 0f
		var distanceToSet = 0
		var iPieceUsedInTheStack = false

		init {

			rankStacking = 0f
			distanceToSet = (ranks?.stackWidth ?: 0)*20
		}

		override fun toString():String = " Rank Stacking : $rankStacking distance to set :$distanceToSet"

		fun computeScore(heights:List<Int>) {
			val ranks = ranks ?: return
			distanceToSet = 0
			val surface = MutableList(ranks.stackWidth-1) {0}
			val maxJump = ranks.maxJump
			var nbstep = 0
//			val correctedSteepStep = 0
//			val indexSteepStep = 0
//			val isCliff = true

			for(i in 0 until ranks.stackWidth-1) {
				var diff = heights[i+1]-heights[i]

				if(diff>maxJump) {

					nbstep++

					distanceToSet += diff-maxJump
					diff = maxJump

				}
				if(diff<-maxJump) {

					nbstep++
					distanceToSet -= diff+maxJump

					diff = -maxJump

				}
				surface[i] = diff
			}
			log.debug("new surface =${surface.joinToString()}")

			val surfaceNb = ranks.encode(surface)

			rankStacking = ranks.getRankValue(surfaceNb).toFloat()
			if(previewsMax>0&&distanceToSet>0) rankStacking = 0f

		}

		operator fun compareTo(o:Any):Int {
			val otherScore = o as Score

			/* if (this.distanceToSet!= otherScore.distanceToSet){
 * return this.distanceToSet<otherScore.distanceToSet?1:-1;
 * }
 * else { */
			return if(rankStacking!=otherScore.rankStacking) if(rankStacking>otherScore.rankStacking) 1 else -1 else 0

		}
	}

	fun initRanks() {
		delay = 0

		thinkRequest = false
		thinking = false
		threadRunning = false
		val propRanksAI = CustomProperties()
		try {
			val `in` = FileInputStream(AIRanksConstants.RANKSAI_CONFIG_FILE)
			propRanksAI.load(`in`)
			`in`.close()
		} catch(_:IOException) {
		}

		val file = propRanksAI.getProperty("ranksai.file")
		previewsMax = propRanksAI.getProperty("ranksai.numpreviews", 2)
		allowHold = propRanksAI.getProperty("ranksai.allowhold", false)
		speedLimit = propRanksAI.getProperty("ranksai.speedlimit", 0)

		// If no ranks file has been loaded yet, try to load it
		if(ranks==null||currentRanksFile!=file) {
			currentRanksFile = file
			var inputFile = ""
			if(file!=null&&file.trim {it<=' '}.isNotEmpty()) inputFile = AIRanksConstants.RANKSAI_DIR+currentRanksFile!!
			val fis:FileInputStream
			val `in`:ObjectInputStream

			if(inputFile.trim {it<=' '}.isEmpty())
				ranks = Ranks(4, 9)
			else
				try {
					fis = FileInputStream(inputFile)
					`in` = ObjectInputStream(fis)
					ranks = `in`.readObject() as Ranks
					`in`.close()

				} catch(e:FileNotFoundException) {
					ranks = Ranks(4, 9)
				} catch(e:IOException) {
					// TODO Auto-generated catch block
					e.printStackTrace()
				} catch(e:ClassNotFoundException) {
					e.printStackTrace()
				}

		}

		heights = MutableList(ranks!!.stackWidth) {0}
		isGameOver = false
	}

	override fun init(engine:GameEngine, playerID:Int) {
		gEngine = engine
		gManager = engine.owner

		// Inits the ranks
		initRanks()

		//Starts the thread
		if((thread==null||!thread!!.isAlive)&&engine.aiUseThread) {
			thread = Thread(this, "AI_${engine.playerID}")
			thread!!.isDaemon = true
			thread!!.start()
			thinkDelay = engine.aiThinkDelay
			thinkCurrentPieceNo = 0
			thinkLastPieceNo = 0
		}

	}

	final override fun shutdown() {
		ranks = null
		super.shutdown()
	}

	override fun newPiece(engine:GameEngine, playerID:Int) {
		if(!engine.aiUseThread)
			thinkBestPosition(engine)
		else {
			thinkRequest = true
			thinkCurrentPieceNo++
		}
	}

	override fun onFirst(engine:GameEngine, playerID:Int) {}

	override fun onLast(engine:GameEngine, playerID:Int) {}

	override fun setControl(engine:GameEngine, playerID:Int, ctrl:Controller):Int {

		if(engine.nowPieceObject!=null&&engine.stat==GameEngine.Status.MOVE&&delay>=engine.aiMoveDelay&&engine.statc[0]>0&&
			(!engine.aiUseThread||threadRunning&&!thinking&&thinkCurrentPieceNo<=thinkLastPieceNo)
		) {
			val totalPieceLocked = engine.statistics.totalPieceLocked+1
			val tpm = (totalPieceLocked*3600f).toInt()/engine.statistics.time
			if(tpm<=speedLimit||speedLimit<=0) {
				var input = 0
				val pieceNow = engine.nowPieceObject!!
				val nowX = engine.nowPieceX
				val nowY = engine.nowPieceY
				val rt = pieceNow.direction
				val fld = engine.field
				val pieceTouchGround = pieceNow.checkCollision(nowX, nowY+1, fld)

				if(bestHold||forceHold) {
					if(engine.isHoldOK) input = input or Controller.BUTTON_BIT_D
				} else {

					if(rt!=bestRt) {
						val lrot = engine.getSpinDirection(-1)
						val rrot = engine.getSpinDirection(1)

						if(abs(rt-bestRt)==2&&engine.ruleOpt.spinDoubleKey
							&&!ctrl.isPress(Controller.BUTTON_E)
						)
							input = input or Controller.BUTTON_BIT_E
						else if(!ctrl.isPress(Controller.BUTTON_B)&&engine.ruleOpt.spinReverseKey&&
							!engine.spinDirection&&bestRt==rrot
						)
							input = input or Controller.BUTTON_BIT_B
						else if(!ctrl.isPress(Controller.BUTTON_B)&&engine.ruleOpt.spinReverseKey&&
							engine.spinDirection&&bestRt==lrot
						)
							input = input or Controller.BUTTON_BIT_B
						else if(!ctrl.isPress(Controller.BUTTON_A)) input = input or Controller.BUTTON_BIT_A
					}

					val minX = pieceNow.getMostMovableLeft(nowX, nowY, rt, fld)
					val maxX = pieceNow.getMostMovableRight(nowX, nowY, rt, fld)
					if(!skipNextFrame) {
						skipNextFrame = true

						if((bestX<minX-1||bestX>maxX+1||bestY<nowY)&&rt==bestRt)
							thinkRequest = true
						else {

							if(nowX==bestX&&pieceTouchGround&&rt==bestRt) {

								if(bestRtSub!=-1) {
									bestRt = bestRtSub
									bestRtSub = -1
								}

								if(bestX!=bestXSub) {
									bestX = bestXSub
									bestY = bestYSub
								}
							}

							if(nowX>bestX) {

								if(!ctrl.isPress(Controller.BUTTON_LEFT)||engine.aiMoveDelay>=0)
									input = input or Controller.BUTTON_BIT_LEFT
							} else if(nowX<bestX) {

								if(!ctrl.isPress(Controller.BUTTON_RIGHT)||engine.aiMoveDelay>=0)
									input = input or Controller.BUTTON_BIT_RIGHT
							} else if(nowX==bestX&&rt==bestRt)
								if(bestRtSub==-1&&bestX==bestXSub) {
									if(engine.ruleOpt.harddropEnable&&!ctrl.isPress(Controller.BUTTON_UP))
										input = input or Controller.BUTTON_BIT_UP
									else if(engine.ruleOpt.softdropEnable||engine.ruleOpt.softdropLock)
										input = input or Controller.BUTTON_BIT_DOWN
								} else if(engine.ruleOpt.harddropEnable&&!engine.ruleOpt.harddropLock&&!ctrl.isPress(Controller.BUTTON_UP))
									input = input or Controller.BUTTON_BIT_UP
								else if(engine.ruleOpt.softdropEnable&&!engine.ruleOpt.softdropLock)
									input = input or Controller.BUTTON_BIT_DOWN
						}
					} else
						skipNextFrame = false
				}

				delay = 0
				return input
			}
		}
		delay++
		return 0
	}

	/** Plays a fictitious move (ie not rendering it on the screen) for use in
	 * AIRanksTester
	 * It searches for the best possible move and plays it(ie updates the
	 * heights array)
	 * @param heights Heights of the columns
	 * @param pieces Current Piece and Next Pieces
	 */
	fun playFictitiousMove(heights:MutableList<Int>, pieces:MutableList<Int>, holdPiece:MutableList<Int>, holdOK:MutableList<Boolean>) {
		currentHeightMin = 25
		currentHeightMax = 0
		for(i in 0 until ranks!!.stackWidth) {

			if(heights[i]<currentHeightMin) currentHeightMin = heights[i]
			if(heights[i]>currentHeightMax) currentHeightMax = heights[i]
		}

		thinkBestPosition(heights, pieces, holdPiece, holdOK[0])
		if(bestScore.rankStacking==0f)
			isGameOver = true
		else if(bestHold) holdOK[0] = false
		else {
			holdOK[0] = true
			if(bestX==9)
				for(i in heights.indices)
					heights[i] -= 4
			else {
				ranks!!.addToHeights(heights, pieces[0], bestRt, bestX)
				for(height in heights)
					if(height>20) {
						isGameOver = true
						break
					}
			}
		}

	}

	/** Think the best position, for a given engine. It will do the necessary
	 * conversion from engine representation of the field to ranks
	 * representation
	 * of it, and run the main method thinkBestPosition
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun thinkBestPosition(engine:GameEngine) {

		// Current line of the current piece
		val nowY = engine.nowPieceY

		// Currently considered piece
		val pieceNow = engine.nowPieceObject ?: return

		// Initialization of the heights array
		heights = (0 until ranks!!.stackWidth).map {engine.field.height-engine.field.getHighestBlockY(it)}
			.toMutableList()

		// Initialization of the pieces array (contains the current piece and the next pieces)
		val pieces = MutableList(engine.nextPieceArraySize) {
			if(it==0) pieceNow!!.id else engine.getNextObject(engine.nextPieceCount+it-1)!!.id
		}

		val holdPiece = mutableListOf(engine.holdPieceObject?.id ?: -1)

		val holdOK = engine.isHoldOK

		allowHold = allowHold and engine.ruleOpt.holdEnable

		// Call the main method (that actually does the work, on the heights and pieces
		thinkBestPosition(heights, pieces, holdPiece, holdOK)

		// Convert the best chosen move to the engine representation
		bestX -= pieceNow.dataOffsetX[bestRt]+Ranks.PIECES_LEFTMOSTS[pieceNow.id][bestRt]
		bestXSub = bestX

		bestY = pieceNow.getBottom(bestX, nowY, bestRt, engine.field)
		bestYSub = bestY
		bestYSub = bestY

		// If we cant fit the pieces anymore without creating holes, try hold
		//bestHold=false;
		if(bestScore.rankStacking==0f) threadRunning = false
		//bestHold=true;
		thinkLastPieceNo++
		log.debug("nowX : ${engine.nowPieceX} X:$bestX Y:$bestY R:$bestRt H:$bestHold Pts:$bestScore")

	}

	/** Main method that will return the best move for the current piece, by
	 * calling the recursive method thinkmain on each possible move
	 * @param heights Array containing the heights of the columns in the field
	 * @param pieces Array containing the current piece and the next pieces.
	 */

	fun thinkBestPosition(heights:MutableList<Int>, pieces:MutableList<Int>, holdPiece:MutableList<Int>, holdOK:Boolean) {

		// The best* variables contain the chosen best position for the current piece.
		// The best*Sub variables are used in case you want to do a twist or a spin or a slide. they give the final position for the best move
		// We are not using slides or twists so the best*Sub basically equal the best* variables.
		// bestRtSub=-1 means there is no twist.
		bestX = 0
		bestY = 0
		bestRt = 0
		bestXSub = 0
		bestYSub = 0
		bestRtSub = -1
		bestPts = 0

		// We keep track of the best score too to be able to know when there is no possibility to place a piece( when bestScore.rankstacking=0)
		bestScore = Score()

		// Variable to temporarily store the score
		var score:Score
		//int [] piecesCopy= Arrays.copyOf(pieces, pieces.length);
		val piecesCopy = pieces.toMutableList()

		// Current piece
		var pieceNow = piecesCopy[0]

		// Number of previews to consider
		val numPreviews = previewsMax

		// Initialization maximum height and minimum height
		currentHeightMin = 99
		currentHeightMax = 0

		//Compute the maximum/minimum heights
		for(i in 0 until ranks!!.stackWidth) {

			if(heights[i]<currentHeightMin) currentHeightMin = heights[i]
			if(heights[i]>currentHeightMax) currentHeightMax = heights[i]
		}

		// If we are able to score a 4-Line and if the maximum height is dangerously high, then force doing a tetris
		if(pieceNow==Piece.PIECE_I&&currentHeightMin>=THRESHOLD_FORCE_4LINES
			&&currentHeightMin>=4 /* &&!plannedToUseIPiece */) {

			bestHold = false
			//Rightmost column
			bestX = ranks!!.stackWidth

			// Vertical rotation
			bestRt = 1

			bestXSub = bestX
			bestRtSub = -1

			// Dummy score so that the AI doesn't think the game is over (can't fit a piece anymore)
			bestScore.rankStacking = Integer.MAX_VALUE.toFloat()

		} else
		// Try using hold or not
			for(useHold in 0 until if(holdOK&&allowHold) 2 else 1) {
				if(useHold==1)
					if(holdPiece[0]==-1) {
						holdPiece[0] = piecesCopy[0]
						System.arraycopy(piecesCopy, 1, piecesCopy, 0, piecesCopy.size-1)

						pieceNow = piecesCopy[0]
						// numPreviews--;

					} else {
						val tempPiece = piecesCopy[0]
						piecesCopy[0] = holdPiece[0]
						holdPiece[0] = tempPiece
						pieceNow = piecesCopy[0]
					}
				// try all possible rotations{
				for(rt in 0 until Ranks.PIECES_NUM_ROTATIONS[pieceNow]) {

					// Columns go from 0 to 9 in Ranks representation
					val minX = 0
					val maxX = ranks!!.stackWidth-Ranks.PIECES_WIDTHS[pieceNow][rt]
					for(x in minX..maxX) {

						// Run thinkmain on that move to get its score
						score = thinkMain(x, rt, heights, piecesCopy, holdPiece, useHold!=1, numPreviews)
						log.debug("MAIN  id=$pieceNow posX=$x rt=$rt hold :$useHold score:$score")

						//If the score is better than the previous best score, change it, and record the chosen move for further application by setControl
						if(score>bestScore) {
							log.debug("MAIN new best piece !")
							if(pieceNow==Piece.PIECE_I) bestScore.iPieceUsedInTheStack = true
							bestHold = useHold==1
							bestX = x
							bestRt = rt
							bestXSub = x
							bestRtSub = -1
							bestScore = score
						}

					}

					// If we can score a 4-Line, try it
					if(pieceNow==Piece.PIECE_I&&(rt==1||rt==3)&&currentHeightMin>=4) {

						// What are the consequences of scoring a 4-Line ?
						score = thinkMain(maxX+1, rt, heights, piecesCopy, holdPiece, useHold!=1, numPreviews)
						log.debug("MAIN (4 Lines) id=$pieceNow posX=${maxX+1} rt=$rt hold :$useHold score:$score")

						//If the score is better than the previous best score, change it, and record the chosen move for further application by setControl
						if(score>bestScore) {
							log.debug("MAIN (4 Lines) new best piece !")

							bestHold = useHold==1
							bestX = maxX+1
							bestRt = rt
							bestXSub = maxX+1
							bestRtSub = -1
							bestScore = score
						}

					}

				}
			}

		if(numPreviews>0) plannedToUseIPiece = bestScore.iPieceUsedInTheStack

	}

	/** Recursive method that returns the score of a given move for a given
	 * field and given next pieces
	 * @param x Column where the piece has to be put.
	 * @param rt Rotation of the piece.
	 * @param heights Array containing the heights of the field
	 * @param pieces Array containing the Current Piece and Next Pieces
	 * @param numPreviews Number of previews to consider in the thinking process
	 * @return The score for this move (placing the piece in this column, with
	 * this rotation)
	 */
	fun thinkMain(x:Int, rt:Int, heights:List<Int>, pieces:MutableList<Int>, holdPiece:MutableList<Int>, holdOK:Boolean, numPreviews:Int):Score {

		// Initialize the score with zero
		val score = Score()

		// Initialize maximum height and minimum height of the stack with dummy values
		var heightMin = 99
		var heightMax = 0
		//Convert the heights to a surface to be able to check if the piece fits the surface
		val surface = ranks!!.heightsToSurface(heights)

		log.debug("piece id : ${pieces[0]} rot : $rt x :$x surface :${surface.joinToString()}}")

		//Boolean value representing the fact that the current piece is the I-piece, vertical, and in the rightmost column.
		val isVerticalIRightMost = pieces[0]==Piece.PIECE_I&&(rt==1||rt==3)&&x==9

		// Either we are going to score a 4-Line or we have to check that the piece fits the surface
		if(isVerticalIRightMost||ranks!!.surfaceFitsPiece(surface, pieces[0], rt, x)) {

			// Cloning the heights in order to not alter the heights array, that was passed in parameters (and possibly used elsewhere)
			val heightsWork = heights.toMutableList()

			// If we are not going to score a 4-Line, add the piece to the heightsWork array and update the minimum and maximum height.
			if(!isVerticalIRightMost) {

				ranks!!.addToHeights(heightsWork, pieces[0], rt, x)

				for(i in 0 until ranks!!.stackWidth) {
					if(heightsWork[i]>heightMax) heightMax = heights[i]
					if(heightsWork[i]<heightMin) heightMin = heights[i]
				}

			} else {
				for(i in 0 until ranks!!.stackWidth)
					heightsWork[i] -= 4

				heightMin -= 4
				heightMax -= 4

			}// If we are going to score a 4-Line, substract 4 to the heights and to the minimum and maximum heights. They will remain positive because
			// the necessary condition to score a tetris is that the minimal height be greater than 4.

			// If there are still previews left, then we go on recursively to explore the lower branches of the decision tree.
			if(numPreviews>0) {
				// We are going to examine all possible moves for the next piece and return the best score.

				// Initialize the best score to zero.
				var bestScore = Score()

				// We will need a score variable to temporary store the score of the currently considered move.
				var scoreCurrent:Score

				// The piece considered now is the next piece
				var pieceNow = pieces[1]

				// The pieces array that we will pass to the recursive call to thinkMain has to be shifted to the left
				// 2nd piece becomes 1st piece, 3d piece becomes 2d, etc...
				val pieces2 = MutableList(pieces.size-1) {pieces[it+1]}

				val holdPiece2 = mutableListOf(holdPiece[0])

				val numPreviews2 = numPreviews-1
				// If current piece is I-piece,  and minimum height is greater 4 and maximum height is greater than threshold, force the 4-Line
				if(pieceNow==Piece.PIECE_I&&heightMin>=THRESHOLD_FORCE_4LINES&&heightMin>=4 /* &
				 * &
				 * !
				 * plannedToUseIPiece */) {
					val rt2 = 1
					val maxX2 = ranks!!.stackWidth-1
					// Recursive call to thinkMain to examine that move
					scoreCurrent = thinkMain(maxX2+1, rt2, heightsWork, pieces2, holdPiece2, true, numPreviews2)
					log.debug("SUB (4 Lines)$numPreviews id=$pieceNow posX=${maxX2+1} rt=$rt2 score:$scoreCurrent")

					// if the score is better than the previous best score, replace it.
					if(scoreCurrent>bestScore) {
						log.debug("SUB new best piece !")

						bestScore = scoreCurrent
					}
				} else
					for(h2 in 0 until if(holdOK&&allowHold) 2 else 1) {
						if(h2==1)
							if(holdPiece2[0]==-1) {
								holdPiece2[0] = pieces2[0]
								System.arraycopy(pieces2, 1, pieces2, 0, pieces2.size-1)

								pieceNow = pieces2[0]
								// numPreviews2--;

							} else {
								val tempPiece = pieces2[0]
								pieces2[0] = holdPiece2[0]
								holdPiece2[0] = tempPiece
								pieceNow = pieces2[0]
							}

						for(rt2 in 0 until Ranks.PIECES_NUM_ROTATIONS[pieceNow]) {

							// is the piece a vertical I ?
							val isVerticalI2 = pieceNow==Piece.PIECE_I&&(rt2==1||rt2==3)

							// the columns go from 0 to 9 in the representation that Ranks use
							val minX2 = 0
							val maxX2 = minX2+ranks!!.stackWidth-Ranks.PIECES_WIDTHS[pieceNow][rt2]

							for(x2 in minX2..maxX2) {

								// Recursive call to thinkMain to examine that move
								scoreCurrent = thinkMain(x2, rt2, heightsWork, pieces2, holdPiece2, h2!=1, numPreviews2)
								log.debug("SUB $numPreviews id=$pieceNow posX=$x2 rt=$rt2 hold :$h2 score $scoreCurrent")

								// if the score is better than the previous best score, replace it.
								if(scoreCurrent>bestScore) {
									log.debug("SUB new best piece !")
									bestScore = scoreCurrent
								}

							}

							// If the piece considered is vertical I and if the minimum height is greater than 4, try scoring a 4-Line
							if(isVerticalI2&&heightMin>=4) {

								// Recursive call to thinkMain to examine that move
								scoreCurrent = thinkMain(maxX2+1, rt2, heightsWork, pieces2, holdPiece2, h2!=1, numPreviews2)
								log.debug("SUB (4 Lines)$numPreviews id=$pieceNow posX=${maxX2+1} rt=$rt2 hold :$h2 score:$scoreCurrent")
								// if the score is better than the previous best score, replace it.
								if(scoreCurrent>bestScore) {
									log.debug("SUB new best piece !")

									bestScore = scoreCurrent
								}
							}
						}
					}

				// Uncomment to compare all the nodes of the tree between themselves, and not only the end nodes

				/* scoreCurrent=new Score();
 * scoreCurrent.computeScore(heightsWork);
 * if(scoreCurrent.compareTo(bestScore)>0) {
 * bestScore=scoreCurrent;
 * } */

				// Returns the best score
				return bestScore
			}

			score.computeScore(heightsWork)
			if(pieces[0]==Piece.PIECE_I&&x<ranks!!.stackWidth&&numPreviews<previewsMax)
				score.iPieceUsedInTheStack = true
			return score
			// If numPreviews==0, that is, if there are no previews left to consider, just return the score of the surface resulting from the move.
		}
		return score

	}
	/* Thread routine for this AI */
	override fun run() {
		log.info("RanksAI: Thread start")
		threadRunning = true

		while(threadRunning) {
			if(thinkRequest) {
				thinkRequest = false
				thinking = true
				try {
					thinkBestPosition(gEngine)

				} catch(e:Throwable) {
					log.debug("RanksAI: thinkBestPosition Failed", e)
				}

				thinking = false
				skipNextFrame = false
			}

			if(thinkDelay>0)
				try {
					Thread.sleep(thinkDelay.toLong())
				} catch(e:InterruptedException) {
					break
				}

		}

		threadRunning = false
		ranks = null
		log.info("RanksAI: Thread end")
	}

	companion object {

		internal val log = LogManager.getLogger()

		private const val THRESHOLD_FORCE_4LINES = 8
	}
}
