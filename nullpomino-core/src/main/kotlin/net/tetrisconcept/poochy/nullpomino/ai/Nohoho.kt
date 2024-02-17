/*
 Copyright (c) 2010-2024, NullNoname
 All rights reserved.

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis
 THIS IS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
     * Neither the name of NullNoname nor the names of its
       contributors may be used to endorse or promote products derived from
       this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

package net.tetrisconcept.poochy.nullpomino.ai

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.ai.DummyAI
import org.apache.logging.log4j.LogManager
import kotlin.math.abs

/**
Nohoho AI
@author Poochy.EXE
Poochy.Spambucket@gmail.com
 */
class Nohoho:DummyAI(), Runnable {
	/** When true,To threadThink routineInstructing the execution of the */
	private var thinkRequest = ThinkRequestMutex()
	/** Number of frames for which piece has been stuck */
	private var stuckDelay = 0

	/** Status of last frame */
	private var lastInput = 0
	private var lastX = 0
	private var lastY = 0
	private var lastRt = 0
	/** Number of consecutive frames with same piece status */
	private var sameStatusTime = 0
	/** DAS charge status. -1 = left, 0 = none, 1 = right */
	private var setDAS = 0
	/** Last input if done in ARE */
	private var inputARE = 0
	/** Did the thinking thread finish successfully? */
	override var thinkComplete = false
	/** Did the thinking thread find a possible position? */
	private var thinkSuccess = false
	/** Was the game in ARE as of the last frame? */
	private var inARE = false

	/* AI's name */
	override val name = "Avalanche-R V0.01"

	/* Called at initialization */
	override fun init(engine:GameEngine, playerID:Int) {
		delay = 0
		gEngine = engine
		gManager = engine.owner
		thinkRequest = ThinkRequestMutex()
		thinking = false
		threadRunning = false
		setDAS = 0

		stuckDelay = 0
		inputARE = 0
		lastInput = 0
		lastX = -1
		lastY = -1
		lastRt = -1
		sameStatusTime = 0
		thinkComplete = false
		thinkSuccess = false
		inARE = false

		if((thread==null||!thread!!.isAlive)&&engine.aiUseThread) {
			thread = Thread(this, "AI_${engine.playerID}")
			thread!!.isDaemon = true
			thread!!.start()
			thinkDelay = engine.aiThinkDelay
			thinkCurrentPieceNo = 0
			thinkLastPieceNo = 0
		}
	}

	/* Called whenever a new piece is spawned */
	override fun newPiece(engine:GameEngine, playerID:Int) {
		if(!engine.aiUseThread)
			thinkBestPosition(engine)
		else if(!thinking&&!thinkComplete||!engine.aiPreThink||engine.aiShowHint) {
			thinkRequest.newRequest()
			thinkCurrentPieceNo++
		}
	}

	/* Called at the start of each frame */
	override fun onFirst(engine:GameEngine, playerID:Int) {
		if(engine.aiPreThink&&engine.speed.are>0&&engine.speed.areLine>0) {
			inputARE = 0
			val newInARE = engine.stat===GameEngine.Status.ARE||engine.stat===GameEngine.Status.READY
			if(newInARE&&!inARE||!thinking&&!thinkSuccess) {
				if(DEBUG_ALL) log.debug("Begin pre-think of next piece.")
				thinkComplete = false
				thinkRequest.newRequest()
			}
			inARE = newInARE
		}
	}

	/* Set button input states */
	override fun setControl(engine:GameEngine, playerID:Int, ctrl:Controller):Int {
		if(engine.nowPieceObject!=null&&engine.stat===GameEngine.Status.MOVE&&
			delay>=engine.aiMoveDelay&&engine.statc[0]>0&&
			(!engine.aiUseThread||threadRunning&&!thinking&&thinkCurrentPieceNo<=thinkLastPieceNo)
		) {
			inputARE = 0
			var input = 0 // Button input data
			val pieceNow = checkOffset(engine.nowPieceObject!!, engine)
			val nowX = engine.nowPieceX
			val nowY = engine.nowPieceY
			val rt = pieceNow.direction
			val fld = engine.field
			val pieceTouchGround = pieceNow.checkCollision(nowX, nowY+1, fld)

			var moveDir = 0 //-1 = left,  1 = right
			var spinDir = 0 //-1 = left,  1 = right
			var drop = 0 //1 = up, -1 = down
			val sync = false //true = delay either spin or movement for synchro move if needed.

			//If stuck, rethink.
			if(pieceTouchGround&&rt==bestRt&&
				(pieceNow.getMostMovableRight(nowX, nowY, rt, engine.field)<bestX||pieceNow.getMostMovableLeft(
					nowX, nowY, rt,
					engine.field
				)>bestX)
			)
				stuckDelay++
			else
				stuckDelay = 0
			if(stuckDelay>4) {
				thinkRequest.newRequest()
				thinkComplete = false
				if(DEBUG_ALL) log.debug("Needs rethink - piece is stuck!")
			}
			if(nowX==lastX&&nowY==lastY&&rt==lastRt&&lastInput!=0) {
				sameStatusTime++
				if(sameStatusTime>4) {
					thinkRequest.newRequest()
					thinkComplete = false
					if(DEBUG_ALL) log.debug("Needs rethink - piece is stuck, last inputs had no effect!")
				}
			}
			if(engine.nowPieceSpinCount>=8) {
				thinkRequest.newRequest()
				thinkComplete = false
				if(DEBUG_ALL) log.debug("Needs rethink - piece is stuck, too many rotations!")
			} else
				sameStatusTime = 0
			if(bestHold&&thinkComplete&&engine.isHoldOK)
			// Hold
				input =  Controller.BUTTON_BIT_D
			else {
				if(DEBUG_ALL)
					log.debug(
						"bestX = $bestX, nowX = "+nowX+
							", bestY = $bestY, nowY = "+nowY+
							", bestRt = $bestRt, rt = "+rt+
							", bestXSub = $bestXSub, bestYSub = $bestYSub, bestRtSub = "+bestRtSub
					)
				// Rotation
				val best180 = abs(rt-bestRt)==2
				if(rt!=bestRt) {
					val spL = engine.getSpinDirection(-1)
					val spR = engine.getSpinDirection(1)
					if(DEBUG_ALL) log.debug("spL = $spL, spR = $spR")

					if(best180&&engine.ruleOpt.spinDoubleKey&&!ctrl.isPress(Controller.BUTTON_E))
						input =  Controller.BUTTON_BIT_E
					else if(bestRt==spR) spinDir = 1
					else if(bestRt==spL) spinDir = -1
					else if(engine.ruleOpt.spinReverseKey&&best180&&rt and 1>0) {
						spinDir = if(spR==Piece.DIRECTION_UP) 1 else -1
					} else spinDir = 1
				}

				// Whether reachable position
				val minX = pieceNow.getMostMovableLeft(nowX, nowY, rt, fld)
				val maxX = pieceNow.getMostMovableRight(nowX, nowY, rt, fld)

				if((bestX<minX-1||bestX>maxX+1||bestY<nowY)&&rt==bestRt) {
					// Again because it is thought unreachable
					//thinkBestPosition(engine, playerID);
					thinkRequest.newRequest()
					thinkComplete = false
					//thinkCurrentPieceNo++;
					//System.out.println("rethink c:" + thinkCurrentPieceNo + " l:" + thinkLastPieceNo);
					if(DEBUG_ALL) log.debug("Needs rethink - cannot reach desired position")
				} else {
					// If you are able to reach
					if(nowX==bestX&&pieceTouchGround)
						if(rt==bestRt) {
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
					if(nowX>bestX) moveDir = -1
					else if(nowX<bestX) moveDir = 1
					else if(nowX==bestX&&rt==bestRt) {
						moveDir = 0
						setDAS = 0
						// Funnel
						if(bestRtSub==-1&&bestX==bestXSub) {
							if(pieceTouchGround&&engine.ruleOpt.softdropLock) drop = -1
							else if(engine.ruleOpt.harddropEnable) drop = 1
							else if(engine.ruleOpt.softdropEnable||engine.ruleOpt.softdropLock) drop = -1
						} else if(engine.ruleOpt.harddropEnable&&!engine.ruleOpt.harddropLock)
							drop = 1
						else if(engine.ruleOpt.softdropEnable&&!engine.ruleOpt.softdropLock) drop = -1
					}
				}
			}

			//Convert parameters to input
			val useDAS = engine.dasCount>=engine.speed.das&&moveDir==setDAS
			if(moveDir==-1&&(!ctrl.isPress(Controller.BUTTON_LEFT)||useDAS))
				input = input or Controller.BUTTON_BIT_LEFT
			else if(moveDir==1&&(!ctrl.isPress(Controller.BUTTON_RIGHT)||useDAS)) input = input or Controller.BUTTON_BIT_RIGHT
			if(drop==1&&!ctrl.isPress(Controller.BUTTON_UP))
				input = input or Controller.BUTTON_BIT_UP
			else if(drop==-1) input = input or Controller.BUTTON_BIT_DOWN

			if(spinDir!=0)
				if(engine.ruleOpt.spinDoubleKey&&spinDir==2&&!ctrl.isPress(Controller.BUTTON_E))
					input = input or Controller.BUTTON_BIT_E
				else if(engine.ruleOpt.spinReverseKey&&!engine.ruleOpt.spinToRight&&spinDir==1) {
					if(!ctrl.isPress(Controller.BUTTON_B)) input = input or Controller.BUTTON_BIT_B
				} else if(engine.ruleOpt.spinReverseKey&&
					engine.ruleOpt.spinToRight&&spinDir==-1
				) {
					if(!ctrl.isPress(Controller.BUTTON_B)) input = input or Controller.BUTTON_BIT_B
				} else if(!ctrl.isPress(Controller.BUTTON_A)) input = input or Controller.BUTTON_BIT_A
			if(setDAS!=moveDir) setDAS = 0

			lastInput = input
			lastX = nowX
			lastY = nowY
			lastRt = rt

			if(DEBUG_ALL)
				log.debug("Input = $input, moveDir = $moveDir, spinDir = $spinDir, sync = $sync, drop = $drop, setDAS = $setDAS")

			delay = 0
			return input
		}
		//dropDelay = 0;
		delay++
		return inputARE
	}

	/**
	 * Search for the best choice
	 * @param engine The GameEngine that owns this AI
	 */
	private fun thinkBestPosition(engine:GameEngine) {
		if(DEBUG_ALL) log.debug("thinkBestPosition called, inARE = $inARE, piece: ")
		bestHold = false
		bestX = 0
		bestY = 0
		bestRt = 0
		bestXSub = 0
		bestYSub = 0
		bestRtSub = -1
		bestPts = 0
		thinkSuccess = false

		engine.createFieldIfNeeded()
		val fld = Field(engine.field)
		var pieceNow = engine.nowPieceObject
		var pieceHold = engine.holdPieceObject
		val holdOK = engine.isHoldOK
		val nowX:Int
		val nowY:Int
		if(inARE||pieceNow==null) {
			pieceNow = engine.getNextObjectCopy(engine.nextPieceCount) ?: return
			nowX = engine.getSpawnPosX(pieceNow, fld)
			nowY = engine.getSpawnPosY(pieceNow)
			if(holdOK&&pieceHold==null) pieceHold = engine.getNextObjectCopy(engine.nextPieceCount+1)
		} else {
			nowX = engine.nowPieceX
			nowY = engine.nowPieceY
			if(holdOK&&pieceHold==null) pieceHold = engine.getNextObjectCopy(engine.nextPieceCount)
		}
		pieceNow = checkOffset(pieceNow, engine)
		if(holdOK&&pieceHold==null) {
			pieceHold = checkOffset(pieceNow, engine)
			if(pieceHold.id==pieceNow.id) pieceHold = null
		}

		var defcon = 5 //Defense condition. 1 = most defensive, 5 = least defensive.
		val depths = getColumnDepths(fld)
		if(depths[2]<=3)
			defcon = 1
		else if(depths[3]<=0) defcon = if(depths[2]<=6) 3 else 4

		if(defcon>=4) {
			var x:Int
			val maxX:Int = when {
				depths[3]<=0 -> 2
				depths[4]<=0 -> 3
				depths[5]<=0 -> 4
				else -> 5
			}
			for(rt in 0..<Piece.DIRECTION_COUNT) {
				x = maxX-pieceNow.maximumBlockX
				fld.replace(engine.field)
				var y = pieceNow.getBottom(x, nowY, rt, fld)

				if(!pieceNow.checkCollision(x, y, rt, fld)) {
					val pts = thinkMain(x, y, rt, -1, fld, pieceNow, defcon)

					if(pts>=bestPts) {
						bestHold = false
						bestX = x
						bestY = y
						bestRt = rt
						bestXSub = x
						bestYSub = y
						bestRtSub = -1
						bestPts = pts
						if(DEBUG_ALL) logBest(1)
						thinkSuccess = true
					}
				}
				if(holdOK&&pieceHold!=null) {
					x = maxX-pieceHold.maximumBlockX
					fld.replace(engine.field)
					y = pieceHold.getBottom(x, nowY, rt, fld)

					if(!pieceHold.checkCollision(x, y, rt, fld)) {
						val pts = thinkMain(x, y, rt, -1, fld, pieceHold, defcon)

						if(pts>=bestPts) {
							bestHold = false
							bestX = x
							bestY = y
							bestRt = rt
							bestXSub = x
							bestYSub = y
							bestRtSub = -1
							bestPts = pts
							if(DEBUG_ALL) logBest(2)
							thinkSuccess = true
						}
					}
				}
			}
		} else
			for(rt in 0..<Piece.DIRECTION_COUNT) {
				val minX = pieceNow.getMostMovableLeft(nowX, nowY, rt, engine.field)
				val maxX = pieceNow.getMostMovableRight(nowX, nowY, rt, engine.field)
				for(x in minX..maxX) {
					fld.replace(engine.field)
					val y = pieceNow.getBottom(x, nowY, rt, fld)

					if(!pieceNow.checkCollision(x, y, rt, fld)) {
						// As it is
						val pts = thinkMain(x, y, rt, -1, fld, pieceNow, defcon)

						if(pts>=bestPts) {
							bestHold = false
							bestX = x
							bestY = y
							bestRt = rt
							bestXSub = x
							bestYSub = y
							bestRtSub = -1
							bestPts = pts
							if(DEBUG_ALL) logBest(3)
							thinkSuccess = true
						}
					}
				}

				// Hold piece
				if(holdOK) pieceHold?.also {
					val spawnX = engine.getSpawnPosX(it, engine.field)
					val spawnY = engine.getSpawnPosY(it)
					val minHoldX = it.getMostMovableLeft(spawnX, spawnY, rt, engine.field)
					val maxHoldX = it.getMostMovableRight(spawnX, spawnY, rt, engine.field)

					for(x in minHoldX..maxHoldX) {
						fld.replace(engine.field)
						val y = it.getBottom(x, spawnY, rt, fld)

						if(!it.checkCollision(x, y, rt, fld)) {
							// As it is
							val pts = thinkMain(x, y, rt, -1, fld, it, defcon)
							if(pts>=bestPts) {
								bestHold = true
								bestX = x
								bestY = y
								bestRt = rt
								bestXSub = x
								bestYSub = y
								bestRtSub = -1
								bestPts = pts
								if(DEBUG_ALL) logBest(4)
								thinkSuccess = true
							}
						}
					}
				}
			}

		thinkLastPieceNo++

		//System.out.println("X:" + bestX + " Y:" + bestY + " R:" + bestRt + " H:" + bestHold + " Pts:" + bestPts);
	}

	/**
	 * Think routine
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @param rtOld Direction before rotation (-1: None)
	 * @param fld Field (Can be modified without problems)
	 * @param piece Piece
	 * @param defcon Defense level (the lower, the more defensive)
	 * @return Evaluation score
	 */
	private fun thinkMain(x:Int, y:Int, rt:Int, rtOld:Int, fld:Field, piece:Piece, defcon:Int):Int {
		var pts = 0

		if(defcon<=3) pts -= fld.getHighestBlockY(2)

		// Place the piece
		if(!piece.placeToField(x, y, rt, fld)) {
			if(DEBUG_ALL)
				log.debug(
					"End of thinkMain($x, $y, $rt, $rtOld, fld, piece ${piece.type.name}, $defcon). pts = MIN_VALUE (Cannot place piece)"
				)
			return Integer.MIN_VALUE
		}

		fld.freeFall()

		if(defcon>=4) {
			val maxX = piece.maximumBlockX+x
			if(maxX<2) {
				if(DEBUG_ALL)
					log.debug(
						"End of thinkMain($x, $y, $rt, $rtOld, fld, piece ${piece.type.name}, $defcon). pts = MIN_VALUE (Invalid location/defcon combination)"
					)
				return Integer.MIN_VALUE
			}
			val maxY = fld.getHighestBlockY(maxX)
			var clear = fld.clearColor(maxX, maxY, true, true, false, true)
			when {
				clear>=4 -> pts += if(defcon==5) -4 else 4
				clear==3 -> pts += 2
				clear==2 -> pts++
			}

			clear = if(rt and 1>0) {
				pts++
				fld.clearColor(maxX, maxY+1, true, true, false, true)
			} else fld.clearColor(maxX-1, fld.getHighestBlockY(maxX-1), true, true, false, true)
			if(clear>=4) pts += if(defcon==5) -4 else 4
			else if(clear==3) pts += 2
			else if(clear==2) pts++
		}

		// Clear
		var chain = 1
		while(true) {
			val clear = fld.clearColor(4, true, false, true)
			if(clear<=0)
				break
			else if(defcon<=4)
				if(chain==0) pts += clear
				else if(chain==2) pts += clear shl 3
				else if(chain==3) pts += clear shl 4
				else if(chain>=4) pts += clear*32*(chain-3)
			fld.freeFall()
			chain++
		}

		if(defcon<=3) pts += fld.getHighestBlockY(2)

		// All clear
		val allclear = fld.isEmpty
		if(allclear) pts += 1000

		if(DEBUG_ALL)
			log.debug(
				"End of thinkMain($x, $y, $rt, "+rtOld+
					", fld, piece ${piece.id}, $defcon). pts = "+pts
			)
		return pts
	}

	private fun logBest(caseNum:Int) {
		log.debug(
			"New best position found (Case $caseNum): bestHold = $bestHold, bestPos = ($bestX, $bestY, $bestRt), bestPosSub = ($bestXSub, $bestYSub, $bestRtSub), bestPts = $bestPts"
		)
	}

	/* Processing of the thread */
	override fun run() {
		log.info("Nohoho: Thread start")
		threadRunning = true

		while(threadRunning) {
			try {
				synchronized(thinkRequest) {
					if(!thinkRequest.active) thinkRequest.wait()
				}
			} catch(e:InterruptedException) {
				log.debug("PoochyBot: InterruptedException waiting for thinkRequest signal")
			}

			if(thinkRequest.active) {
				thinkRequest.active = false
				thinking = true
				try {
					thinkBestPosition(gEngine)
					thinkComplete = true
					log.debug("Nohoho: thinkBestPosition completed successfully")
				} catch(e:Throwable) {
					log.debug("Nohoho: thinkBestPosition Failed", e)
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
		log.info("Nohoho: Thread end")
	}

	//Wrapper for think requests
	private class ThinkRequestMutex:Object() {
		var active = false

		@Synchronized fun newRequest() {
			active = true
			notifyAll()
		}
	}

	companion object {
		/** Log */
		internal val log = LogManager.getLogger()
		/** MaximumCompromise level */
		private const val MAX_THINK_DEPTH = 2
		/** Set to true to print debug information */
		private const val DEBUG_ALL = true

		//private static final int[][] HI_PENALTY = {{6, 2}, {7, 6}, {6, 2}, {1, 0}};
		fun checkOffset(p:Piece, engine:GameEngine):Piece {
			val result = Piece(p)
			result.big = engine.big
			if(!p.offsetApplied) result.applyOffsetArray(engine.ruleOpt.pieceOffsetX[p.id], engine.ruleOpt.pieceOffsetY[p.id])
			return result
		}

		fun getColumnDepths(fld:Field):IntArray {
			val width = fld.width
			val result = IntArray(width)
			for(x in 0..<width)
				result[x] = fld.getHighestBlockY(x)
			return result
		}
	}
}
