/* Copyright (c) 2010, NullNoname
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of NullNoname nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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
 * POSSIBILITY OF SUCH DAMAGE. */
package mu.nu.nullpo.game.subsystem.ai

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import org.apache.log4j.Logger
import kotlin.math.abs

/** CommonAI */
open class BasicAI:DummyAI(), Runnable {

	/** After that I was groundedX-coordinate */
	var bestXSub:Int = 0

	/** After that I was groundedY-coordinate */
	var bestYSub:Int = 0

	/** After that I was groundedDirection(-1: None) */
	var bestRtSub:Int = 0

	/** The best moveEvaluation score */
	var bestPts:Int = 0

	/** Delay the move for changecount */
	var delay:Int = 0

	/** The GameEngine that owns this AI */
	lateinit var gEngine:GameEngine

	/** The GameManager that owns this AI */
	var gManager:GameManager? = null

	/** When true,To threadThink routineInstructing the execution of the */
	var thinkRequest:Boolean = false

	/** true when thread is executing the think routine. */
	var thinking:Boolean = false

	/** To stop a thread time */
	var thinkDelay:Int = 0

	/** When true,Running thread */
	@Volatile
	var threadRunning:Boolean = false

	/** Thread for executing the think routine */
	var thread:Thread? = null

	/* AIOfName */
	override val name:String
		get() = "BASIC"

	/** MaximumCompromise levelGet the
	 * @return MaximumCompromise level
	 */
	val maxThinkDepth:Int
		get() = 2

	/* Called at initialization */
	override fun init(engine:GameEngine, playerID:Int) {
		delay = 0
		gEngine = engine
		gManager = engine.owner
		thinkRequest = false
		thinking = false
		threadRunning = false

		if(thread?.isAlive!=true&&engine.aiUseThread) {
			thread = Thread(this, "AI_$playerID").apply {
				isDaemon = true
				start()
			}
			thinkDelay = engine.aiThinkDelay
			thinkCurrentPieceNo = 0
			thinkLastPieceNo = 0
		}
	}

	/* End processing */
	override fun shutdown(engine:GameEngine, playerID:Int) {
		if(thread?.isAlive==true) {
			thread?.interrupt()
			threadRunning = false
			thread = null
		}
	}

	/* Called whenever a new piece is spawned */
	override fun newPiece(engine:GameEngine, playerID:Int) {
		if(!engine.aiUseThread)
			thinkBestPosition(engine, playerID)
		else {
			thinkRequest = true
			thinkCurrentPieceNo++
		}
	}

	/* Called at the start of each frame */
	override fun onFirst(engine:GameEngine, playerID:Int) {}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {}

	/* Set button input states */
	override fun setControl(engine:GameEngine, playerID:Int, ctrl:Controller) {
		if(engine.nowPieceObject!=null&&engine.stat==GameEngine.Status.MOVE&&delay>=engine.aiMoveDelay&&engine.statc[0]>0&&
			(!engine.aiUseThread||threadRunning&&!thinking&&thinkCurrentPieceNo<=thinkLastPieceNo)) {
			var input = 0 //  button input data
			val pieceNow = engine.nowPieceObject
			val nowX = engine.nowPieceX
			val nowY = engine.nowPieceY
			val rt = pieceNow!!.direction
			if(engine.field==null)return
			val fld = engine.field!!
			val pieceTouchGround = pieceNow.checkCollision(nowX, nowY+1, fld)

			if((bestHold||forceHold)&&engine.isHoldOK)
			// Hold
				input = input or Controller.BUTTON_BIT_D
			else {
				// rotation
				if(rt!=bestRt) {
					val lrot = engine.getRotateDirection(-1)
					val rrot = engine.getRotateDirection(1)

					if(abs(rt-bestRt)==2&&engine.ruleopt.rotateButtonAllowDouble
						&&!ctrl.isPress(Controller.BUTTON_E))
						input = input or Controller.BUTTON_BIT_E
					else if(!ctrl.isPress(Controller.BUTTON_B)&&engine.ruleopt.rotateButtonAllowReverse&&
						!engine.isRotateButtonDefaultRight&&bestRt==rrot)
						input = input or Controller.BUTTON_BIT_B
					else if(!ctrl.isPress(Controller.BUTTON_B)&&engine.ruleopt.rotateButtonAllowReverse&&
						engine.isRotateButtonDefaultRight&&bestRt==lrot)
						input = input or Controller.BUTTON_BIT_B
					else if(!ctrl.isPress(Controller.BUTTON_A)) input = input or Controller.BUTTON_BIT_A
				}

				// Whether reachable position
				val minX = pieceNow.getMostMovableLeft(nowX, nowY, rt, fld)
				val maxX = pieceNow.getMostMovableRight(nowX, nowY, rt, fld)

				if((bestX<minX-1||bestX>maxX+1||bestY<nowY)&&rt==bestRt)
				// Again because it is thought unreachable
				//thinkBestPosition(engine, playerID);
					thinkRequest = true
				else {
					// If you are able to reach
					if(nowX==bestX&&pieceTouchGround&&rt==bestRt) {
						// Groundrotation
						if(bestRtSub!=-1) {
							bestRt = bestRtSub
							bestRtSub = -1
						}
						// Shift move
						if(bestX!=bestXSub) {
							bestX = bestXSub
							bestY = bestYSub
						}
					}

					if(nowX>bestX) {
						// Left
						if(!ctrl.isPress(Controller.BUTTON_LEFT)||engine.aiMoveDelay>=0) input = input or Controller.BUTTON_BIT_LEFT
					} else if(nowX<bestX) {
						// Right
						if(!ctrl.isPress(Controller.BUTTON_RIGHT)||engine.aiMoveDelay>=0)
							input = input or Controller.BUTTON_BIT_RIGHT
					} else if(nowX==bestX&&rt==bestRt)
					// Funnel
						if(bestRtSub==-1&&bestX==bestXSub) {
							if(engine.ruleopt.harddropEnable&&!ctrl.isPress(Controller.BUTTON_UP))
								input = input or Controller.BUTTON_BIT_UP
							else if(engine.ruleopt.softdropEnable||engine.ruleopt.softdropLock) input = input or Controller.BUTTON_BIT_DOWN
						} else if(engine.ruleopt.harddropEnable&&!engine.ruleopt.harddropLock&&!ctrl.isPress(Controller.BUTTON_UP))
							input = input or Controller.BUTTON_BIT_UP
						else if(engine.ruleopt.softdropEnable&&!engine.ruleopt.softdropLock) input = input or Controller.BUTTON_BIT_DOWN
				}//thinkCurrentPieceNo++;
				//System.out.println("rethink c:" + thinkCurrentPieceNo + " l:" + thinkLastPieceNo);
			}

			delay = 0
			ctrl.buttonBit = input
		} else {
			delay++
			ctrl.buttonBit = 0
		}
	}

	/** Search for the best choice
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	fun thinkBestPosition(engine:GameEngine, playerID:Int) {
		bestHold = false
		bestX = 0
		bestY = 0
		bestRt = 0
		bestXSub = 0
		bestYSub = 0
		bestRtSub = -1
		bestPts = 0
		forceHold = false

		val pieceNow = engine.nowPieceObject
		val nowX = engine.nowPieceX
		val nowY = engine.nowPieceY
		val holdOK = engine.isHoldOK
		var holdEmpty = false
		var pieceHold = engine.holdPieceObject
		val pieceNext = engine.getNextObject(engine.nextPieceCount)
		if(pieceHold==null) holdEmpty = true
		if(engine.field==null)return
		val fld = Field(engine.field!!)

		for(depth in 0 until maxThinkDepth) {
			for(rt in 0 until Piece.DIRECTION_COUNT) {
				// Peace for now
				val minX = pieceNow!!.getMostMovableLeft(nowX, nowY, rt, engine.field!!)
				val maxX = pieceNow.getMostMovableRight(nowX, nowY, rt, engine.field!!)

				for(x in minX..maxX) {
					fld.copy(engine.field)
					val y = pieceNow.getBottom(x, nowY, rt, fld)

					if(!pieceNow.checkCollision(x, y, rt, fld)) {
						// As it is
						var pts = thinkMain(engine, x, y, rt, -1, fld, pieceNow, pieceNext, pieceHold, depth)

						if(pts>=bestPts) {
							bestHold = false
							bestX = x
							bestY = y
							bestRt = rt
							bestXSub = x
							bestYSub = y
							bestRtSub = -1
							bestPts = pts
						}

						if(depth>0||bestPts<=10||pieceNow.id==Piece.PIECE_T) {
							// Left shift
							fld.copy(engine.field)
							if(!pieceNow.checkCollision(x-1, y, rt, fld)&&pieceNow.checkCollision(x-1, y-1, rt, fld)) {
								pts = thinkMain(engine, x-1, y, rt, -1, fld, pieceNow, pieceNext, pieceHold, depth)

								if(pts>bestPts) {
									bestHold = false
									bestX = x
									bestY = y
									bestRt = rt
									bestXSub = x-1
									bestYSub = y
									bestRtSub = -1
									bestPts = pts
								}
							}

							// Right shift
							fld.copy(engine.field)
							if(!pieceNow.checkCollision(x+1, y, rt, fld)&&pieceNow.checkCollision(x+1, y-1, rt, fld)) {
								pts = thinkMain(engine, x+1, y, rt, -1, fld, pieceNow, pieceNext, pieceHold, depth)

								if(pts>bestPts) {
									bestHold = false
									bestX = x
									bestY = y
									bestRt = rt
									bestXSub = x+1
									bestYSub = y
									bestRtSub = -1
									bestPts = pts
								}
							}

							// Leftrotation
							if(!engine.isRotateButtonDefaultRight||engine.ruleopt.rotateButtonAllowReverse) {
								val rot = pieceNow.getRotateDirection(-1, rt)
								var newX = x
								var newY = y
								fld.copy(engine.field)
								pts = 0

								if(!pieceNow.checkCollision(x, y, rot, fld))
									pts = thinkMain(engine, x, y, rot, rt, fld, pieceNow, pieceNext, pieceHold, depth)
								else if(engine.wallkick!=null&&engine.ruleopt.rotateWallkick) {
									val allowUpward = engine.ruleopt.rotateMaxUpwardWallkick<0||engine.nowUpwardWallkickCount<engine.ruleopt.rotateMaxUpwardWallkick
									val kick = engine.wallkick!!.executeWallkick(x, y, -1, rt, rot, allowUpward, pieceNow, fld, null)

									if(kick!=null) {
										newX = x+kick.offsetX
										newY = y+kick.offsetY
										pts = thinkMain(engine, newX, newY, rot, rt, fld, pieceNow, pieceNext, pieceHold, depth)
									}
								}

								if(pts>bestPts) {
									bestHold = false
									bestX = x
									bestY = y
									bestRt = rt
									bestXSub = newX
									bestYSub = newY
									bestRtSub = rot
									bestPts = pts
								}
							}

							// Rightrotation
							if(engine.isRotateButtonDefaultRight||engine.ruleopt.rotateButtonAllowReverse) {
								val rot = pieceNow.getRotateDirection(1, rt)
								var newX = x
								var newY = y
								fld.copy(engine.field)
								pts = 0

								if(!pieceNow.checkCollision(x, y, rot, fld))
									pts = thinkMain(engine, x, y, rot, rt, fld, pieceNow, pieceNext, pieceHold, depth)
								else if(engine.wallkick!=null&&engine.ruleopt.rotateWallkick) {
									val allowUpward = engine.ruleopt.rotateMaxUpwardWallkick<0||engine.nowUpwardWallkickCount<engine.ruleopt.rotateMaxUpwardWallkick
									val kick = engine.wallkick!!.executeWallkick(x, y, 1, rt, rot, allowUpward, pieceNow, fld, null)

									if(kick!=null) {
										newX = x+kick.offsetX
										newY = y+kick.offsetY
										pts = thinkMain(engine, newX, newY, rot, rt, fld, pieceNow, pieceNext, pieceHold, depth)
									}
								}

								if(pts>bestPts) {
									bestHold = false
									bestX = x
									bestY = y
									bestRt = rt
									bestXSub = newX
									bestYSub = newY
									bestRtSub = rot
									bestPts = pts
								}
							}

							// 180-degree rotation
							if(engine.ruleopt.rotateButtonAllowDouble) {
								val rot = pieceNow.getRotateDirection(2, rt)
								var newX = x
								var newY = y
								fld.copy(engine.field)
								pts = 0

								if(!pieceNow.checkCollision(x, y, rot, fld))
									pts = thinkMain(engine, x, y, rot, rt, fld, pieceNow, pieceNext, pieceHold, depth)
								else if(engine.wallkick!=null&&engine.ruleopt.rotateWallkick) {
									val allowUpward = engine.ruleopt.rotateMaxUpwardWallkick<0||engine.nowUpwardWallkickCount<engine.ruleopt.rotateMaxUpwardWallkick
									val kick = engine.wallkick!!.executeWallkick(x, y, 2, rt, rot, allowUpward, pieceNow, fld, null)

									if(kick!=null) {
										newX = x+kick.offsetX
										newY = y+kick.offsetY
										pts = thinkMain(engine, newX, newY, rot, rt, fld, pieceNow, pieceNext, pieceHold, depth)
									}
								}

								if(pts>bestPts) {
									bestHold = false
									bestX = x
									bestY = y
									bestRt = rt
									bestXSub = newX
									bestYSub = newY
									bestRtSub = rot
									bestPts = pts
								}
							}
						}
					}
				}

				if(pieceHold==null) pieceHold = engine.getNextObject(engine.nextPieceCount)
				// Hold Peace
				if(holdOK&&pieceHold!=null&&depth==0) {
					val spawnX = engine.getSpawnPosX(engine.field, pieceHold)
					val spawnY = engine.getSpawnPosY(pieceHold)
					val minHoldX = pieceHold.getMostMovableLeft(spawnX, spawnY, rt, engine.field)
					val maxHoldX = pieceHold.getMostMovableRight(spawnX, spawnY, rt, engine.field)

					for(x in minHoldX..maxHoldX) {
						fld.copy(engine.field)
						val y = pieceHold.getBottom(x, spawnY, rt, fld)

						if(!pieceHold.checkCollision(x, y, rt, fld)) {
							var pieceNext2 = engine.getNextObject(engine.nextPieceCount)
							if(holdEmpty) pieceNext2 = engine.getNextObject(engine.nextPieceCount+1)

							val pts = thinkMain(engine, x, y, rt, -1, fld, pieceHold, pieceNext2, null, depth)

							if(pts>bestPts) {
								bestHold = true
								bestX = x
								bestY = y
								bestRt = rt
								bestRtSub = -1
								bestPts = pts
							}
						}
					}
				}
			}

			if(bestPts>0) break
		}

		thinkLastPieceNo++

		//System.out.println("X:" + bestX + " Y:" + bestY + " R:" + bestRt + " H:" + bestHold + " Pts:" + bestPts);
	}

	/** Think routine
	 * @param engine GameEngine
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @param rtOld Direction before rotation (-1: None)
	 * @param fld Field (Can be modified without problems)
	 * @param piece Piece
	 * @param nextpiece NEXTPeace
	 * @param holdpiece HOLDPeace(nullMay be)
	 * @param depth Compromise level (ranges from 0 through getMaxThinkDepth-1)
	 * @return Evaluation score
	 */
	open fun thinkMain(engine:GameEngine, x:Int, y:Int, rt:Int, rtOld:Int, fld:Field, piece:Piece, nextpiece:Piece?,
		holdpiece:Piece?, depth:Int):Int {
		var pts = 0

		// Add points for being adjacent to other blocks
		if(piece.checkCollision(x-1, y, fld)) pts += 1
		if(piece.checkCollision(x+1, y, fld)) pts += 1
		if(piece.checkCollision(x, y-1, fld)) pts += 100

		// Number of holes and valleys needing an I piece (before placement)
		val holeBefore = fld.howManyHoles
		val lidBefore = fld.howManyLidAboveHoles
		val needIValleyBefore = fld.totalValleyNeedIPiece
		// Field height (before placement)
		val heightBefore = fld.highestBlockY
		// Twister flag
		var twist = false
		if(piece.id==Piece.PIECE_T&&rtOld!=-1&&fld.isTwistSpot(x, y, piece.big)) twist = true

		// Place the piece
		if(!piece.placeToField(x, y, rt, fld)) return 0

		// Line clear
		val lines = fld.checkLine()
		if(lines>0) {
			fld.clearLine()
			fld.downFloatingBlocks()
		}

		// All clear
		val allclear = fld.isEmpty
		if(allclear) pts += 500000

		// Field height (after clears)
		val heightAfter = fld.highestBlockY

		// Danger flag
		val danger = heightAfter<=12

		// Additional points for lower placements
		pts += if(!danger&&depth==0)
			y*10
		else
			y*20

		// LinescountAdditional points in
		if(lines==1&&!danger&&depth==0&&heightAfter>=16&&holeBefore<3&&!twist&&engine.combo<1) return 0
		if(!danger&&depth==0) {
			if(lines==1) pts += 10
			if(lines==2) pts += 50
			if(lines==3) pts += 100
			if(lines>=4) pts += 100000
		} else {
			if(lines==1) pts += 5000
			if(lines==2) pts += 10000
			if(lines==3) pts += 30000
			if(lines>=4) pts += 100000
		}

		if(lines<4&&!allclear) {
			// Number of holes and valleys needing an I piece (after placement)
			val holeAfter = fld.howManyHoles
			val lidAfter = fld.howManyLidAboveHoles
			val needIValleyAfter = fld.totalValleyNeedIPiece

			if(holeAfter>holeBefore) {
				// Demerits for new holes
				pts -= (holeAfter-holeBefore)*10
				if(depth==0) return 0
			} else if(holeAfter<holeBefore)
			// Add points for reduction in number of holes
				pts += if(!danger)
					(holeBefore-holeAfter)*5
				else
					(holeBefore-holeAfter)*10

			if(lidAfter>lidBefore) {
				// Is riding on top of the holeBlockIncreasing the deduction
				pts -= if(!danger)
					(lidAfter-lidBefore)*10
				else
					(lidAfter-lidBefore)*20
			} else if(lidAfter<lidBefore)
			// Add points for reduction in number blocks above holes
				pts += if(!danger)
					(lidBefore-lidAfter)*10
				else
					(lidBefore-lidAfter)*20

			if(twist&&lines>=1)
			// Twister bonus
				pts += 100000*lines

			if(needIValleyAfter>needIValleyBefore&&needIValleyAfter>=2) {
				// 2One or moreIDeduction and make a hole type is required
				pts -= (needIValleyAfter-needIValleyBefore)*10
				if(depth==0) return 0
			} else if(needIValleyAfter<needIValleyBefore)
			// Add points for reduction in number of holes
				pts += if(depth==0&&!danger)
					(needIValleyBefore-needIValleyAfter)*10
				else
					(needIValleyBefore-needIValleyAfter)*20

			if(heightBefore<heightAfter) {
				// Add points for reducing the height
				pts += if(depth==0&&!danger)
					(heightAfter-heightBefore)*10
				else
					(heightAfter-heightBefore)*20
			} else if(heightBefore>heightAfter)
			// Demerits for increase in height
				if(depth>0||danger) pts -= (heightBefore-heightAfter)*4

			// Combo bonus
			if(lines>=1&&engine.comboType!=GameEngine.COMBO_TYPE_DISABLE) pts += lines*engine.combo*100
		}

		return pts
	}

	/* Processing of the thread */
	override fun run() {
		log.info("BasicAI: Thread start")
		threadRunning = true

		while(threadRunning) {
			if(thinkRequest) {
				thinkRequest = false
				thinking = true
				try {
					thinkBestPosition(gEngine, gEngine.playerID)
				} catch(e:Throwable) {
					log.debug("BasicAI: thinkBestPosition Failed", e)
				}

				thinking = false
			}

			if(thinkDelay>0)
				try {
					Thread.sleep(thinkDelay.toLong())
				} catch(e:InterruptedException) {
					break
				}

		}

		threadRunning = false
		log.info("BasicAI: Thread end")
	}

	companion object {
		/** Log */
		internal val log = Logger.getLogger(BasicAI::class.java)
	}
}
