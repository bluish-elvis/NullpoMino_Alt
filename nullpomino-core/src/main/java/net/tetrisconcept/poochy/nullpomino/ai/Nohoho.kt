package net.tetrisconcept.poochy.nullpomino.ai

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.subsystem.ai.DummyAI
import org.apache.log4j.Logger
import kotlin.math.abs

/**
 * Nohoho AI
 * @author Poochy.EXE
 * Poochy.Spambucket@gmail.com
 */
class Nohoho:DummyAI(), Runnable {

	/** After that I was groundedX-coordinate */
	private var bestXSub:Int = 0

	/** After that I was groundedY-coordinate */
	private var bestYSub:Int = 0

	/** After that I was groundedDirection(-1: None) */
	private var bestRtSub:Int = 0

	/** The best moveEvaluation score */
	private var bestPts:Int = 0

	/** Delay the move for changecount */
	var delay:Int = 0

	/** The GameEngine that owns this AI */
	private lateinit var gEngine:GameEngine

	/** The GameManager that owns this AI */
	private var gManager:GameManager? = null

	/** When true,To threadThink routineInstructing the execution of the */
	private lateinit var thinkRequest:ThinkRequestMutex

	/** true when thread is executing the think routine. */
	private var thinking:Boolean = false

	/** To stop a thread time */
	private var thinkDelay:Int = 0

	/** When true,Running thread */
	@Volatile var threadRunning:Boolean = false

	/** Thread for executing the think routine */
	var thread:Thread? = null

	/** Number of frames for which piece has been stuck */
	private var stuckDelay:Int = 0

	/** Status of last frame */
	private var lastInput:Int = 0
	private var lastX:Int = 0
	private var lastY:Int = 0
	private var lastRt:Int = 0
	/** Number of consecutive frames with same piece status */
	private var sameStatusTime:Int = 0
	/** DAS charge status. -1 = left, 0 = none, 1 = right */
	private var setDAS:Int = 0
	/** Last input if done in ARE */
	private var inputARE:Int = 0
	/** Did the thinking thread finish successfully? */
	override var thinkComplete:Boolean = false
	/** Did the thinking thread find a possible position? */
	private var thinkSuccess:Boolean = false
	/** Was the game in ARE as of the last frame? */
	private var inARE:Boolean = false

	/* AI's name */
	override val name:String = "Avalanche-R V0.01"

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
			thread = Thread(this, "AI_$playerID")
			thread!!.isDaemon = true
			thread!!.start()
			thinkDelay = engine.aiThinkDelay
			thinkCurrentPieceNo = 0
			thinkLastPieceNo = 0
		}
	}

	/* End processing */
	override fun shutdown() {
		if(thread!=null&&thread!!.isAlive) {
			thread!!.interrupt()
			threadRunning = false
			thread = null
		}
	}

	/* Called whenever a new piece is spawned */
	override fun newPiece(engine:GameEngine, playerID:Int) {
		if(!engine.aiUseThread)
			thinkBestPosition(engine, playerID)
		else if(!thinking&&!thinkComplete||!engine.aiPrethink||engine.aiShowHint) {
			thinkRequest.newRequest()
			thinkCurrentPieceNo++
		}
	}

	/* Called at the start of each frame */
	override fun onFirst(engine:GameEngine, playerID:Int) {
		if(engine.aiPrethink&&engine.speed.are>0&&engine.speed.areLine>0) {
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

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {}

	/* Set button input states */
	override fun setControl(engine:GameEngine, playerID:Int, ctrl:Controller) {
		if(engine.nowPieceObject!=null&&engine.stat===GameEngine.Status.MOVE&&
			delay>=engine.aiMoveDelay&&engine.statc[0]>0&&
			(!engine.aiUseThread||threadRunning&&!thinking&&thinkCurrentPieceNo<=thinkLastPieceNo)) {
			inputARE = 0
			var input = 0 // Button input data
			val pieceNow = checkOffset(engine.nowPieceObject, engine)
			val nowX = engine.nowPieceX
			val nowY = engine.nowPieceY
			val rt = pieceNow.direction
			val fld = engine.field
			val pieceTouchGround = pieceNow.checkCollision(nowX, nowY+1, fld!!)

			var moveDir = 0 //-1 = left,  1 = right
			var rotateDir = 0 //-1 = left,  1 = right
			var drop = 0 //1 = up, -1 = down
			val sync = false //true = delay either rotate or movement for synchro move if needed.

			//If stuck, rethink.
			if(pieceTouchGround&&rt==bestRt&&
				(pieceNow.getMostMovableRight(nowX, nowY, rt, engine.field!!)<bestX||pieceNow.getMostMovableLeft(nowX, nowY, rt, engine.field!!)>bestX))
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
			if(engine.nowPieceRotateCount>=8) {
				thinkRequest.newRequest()
				thinkComplete = false
				if(DEBUG_ALL) log.debug("Needs rethink - piece is stuck, too many rotations!")
			} else
				sameStatusTime = 0
			if(bestHold&&thinkComplete&&engine.isHoldOK)
			// Hold
				input = input or Controller.BUTTON_BIT_D
			else {
				if(DEBUG_ALL)
					log.debug("bestX = $bestX, nowX = "+nowX+
						", bestY = $bestY, nowY = "+nowY+
						", bestRt = $bestRt, rt = "+rt+
						", bestXSub = $bestXSub, bestYSub = $bestYSub, bestRtSub = "+bestRtSub)
				// Rotation
				val best180 = abs(rt-bestRt)==2
				if(rt!=bestRt) {
					val lrot = engine.getRotateDirection(-1)
					val rrot = engine.getRotateDirection(1)
					if(DEBUG_ALL) log.debug("lrot = $lrot, rrot = $rrot")

					if(best180&&engine.ruleopt.rotateButtonAllowDouble&&!ctrl.isPress(Controller.BUTTON_E))
						input = input or Controller.BUTTON_BIT_E
					else if(bestRt==rrot)
						rotateDir = 1
					else if(bestRt==lrot)
						rotateDir = -1
					else if(engine.ruleopt.rotateButtonAllowReverse&&best180&&rt and 1==1) {
						rotateDir = if(rrot==Piece.DIRECTION_UP)
							1
						else
							-1
					} else
						rotateDir = 1
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
					if(nowX>bestX)
						moveDir = -1
					else if(nowX<bestX)
						moveDir = 1
					else if(nowX==bestX&&rt==bestRt) {
						moveDir = 0
						setDAS = 0
						// Funnel
						if(bestRtSub==-1&&bestX==bestXSub) {
							if(pieceTouchGround&&engine.ruleopt.softdropLock)
								drop = -1
							else if(engine.ruleopt.harddropEnable)
								drop = 1
							else if(engine.ruleopt.softdropEnable||engine.ruleopt.softdropLock) drop = -1
						} else if(engine.ruleopt.harddropEnable&&!engine.ruleopt.harddropLock)
							drop = 1
						else if(engine.ruleopt.softdropEnable&&!engine.ruleopt.softdropLock) drop = -1
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

			if(rotateDir!=0)
				if(engine.ruleopt.rotateButtonAllowDouble&&
					rotateDir==2&&!ctrl.isPress(Controller.BUTTON_E))
					input = input or Controller.BUTTON_BIT_E
				else if(engine.ruleopt.rotateButtonAllowReverse&&
					!engine.ruleopt.rotateButtonDefaultRight&&rotateDir==1) {
					if(!ctrl.isPress(Controller.BUTTON_B)) input = input or Controller.BUTTON_BIT_B
				} else if(engine.ruleopt.rotateButtonAllowReverse&&
					engine.ruleopt.rotateButtonDefaultRight&&rotateDir==-1) {
					if(!ctrl.isPress(Controller.BUTTON_B)) input = input or Controller.BUTTON_BIT_B
				} else if(!ctrl.isPress(Controller.BUTTON_A)) input = input or Controller.BUTTON_BIT_A
			if(setDAS!=moveDir) setDAS = 0

			lastInput = input
			lastX = nowX
			lastY = nowY
			lastRt = rt

			if(DEBUG_ALL)
				log.debug("Input = $input, moveDir = $moveDir, rotateDir = "+rotateDir+
					", sync = $sync, drop = $drop, setDAS = "+setDAS)

			delay = 0
			ctrl.buttonBit = input
		} else {
			//dropDelay = 0;
			delay++
			ctrl.buttonBit = inputARE
		}
	}

	/**
	 * Search for the best choice
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	private fun thinkBestPosition(engine:GameEngine, playerID:Int) {
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
		val fld = Field(engine.field!!)
		var pieceNow = engine.nowPieceObject
		var pieceHold = engine.holdPieceObject
		val holdOK = engine.isHoldOK
		val nowX:Int
		val nowY:Int
		if(inARE||pieceNow==null) {
			pieceNow = engine.getNextObjectCopy(engine.nextPieceCount)
			nowX = engine.getSpawnPosX(fld, pieceNow)
			nowY = engine.getSpawnPosY(pieceNow)
			if(holdOK&&pieceHold==null) pieceHold = engine.getNextObjectCopy(engine.nextPieceCount+1)
		} else {
			nowX = engine.nowPieceX
			nowY = engine.nowPieceY
			if(holdOK&&pieceHold==null) pieceHold = engine.getNextObjectCopy(engine.nextPieceCount)
		}
		pieceNow = checkOffset(pieceNow, engine)
		if(holdOK&&pieceHold==null) {
			pieceHold = checkOffset(pieceHold, engine)
			if(pieceHold.id==pieceNow.id) pieceHold = null
		}

		var defcon = 5 //Defense condition. 1 = most defensive, 5 = least defensive.
		val depths = getColumnDepths(fld)
		if(depths[2]<=3)
			defcon = 1
		else if(depths[3]<=0) defcon = if(depths[2]<=6) 3 else 4

		if(defcon>=4) {
			var x:Int
			val maxX:Int = if(depths[3]<=0)
				2
			else if(depths[4]<=0)
				3
			else if(depths[5]<=0)
				4
			else
				5
			for(rt in 0 until Piece.DIRECTION_COUNT) {
				x = maxX-pieceNow.maximumBlockX
				fld.copy(engine.field!!)
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
					fld.copy(engine.field!!)
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
			for(rt in 0 until Piece.DIRECTION_COUNT) {
				val minX = pieceNow.getMostMovableLeft(nowX, nowY, rt, engine.field!!)
				val maxX = pieceNow.getMostMovableRight(nowX, nowY, rt, engine.field!!)
				for(x in minX..maxX) {
					fld.copy(engine.field!!)
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
				if(holdOK)pieceHold?.also{
					val spawnX = engine.getSpawnPosX(engine.field, it)
					val spawnY = engine.getSpawnPosY(it)
					val minHoldX = it.getMostMovableLeft(spawnX, spawnY, rt, engine.field!!)
					val maxHoldX = it.getMostMovableRight(spawnX, spawnY, rt, engine.field!!)

					for(x in minHoldX..maxHoldX) {
						fld.copy(engine.field!!)
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
				log.debug("End of thinkMain($x, $y, $rt, "+rtOld+
					", fld, piece ${piece.id}, $defcon). pts = MIN_VALUE (Cannot place piece)")
			return Integer.MIN_VALUE
		}

		fld.freeFall()

		if(defcon>=4) {
			val maxX = piece.maximumBlockX+x
			if(maxX<2) {
				if(DEBUG_ALL)
					log.debug("End of thinkMain($x, $y, $rt, $rtOld, fld, piece "
						+piece.id+", $defcon). pts = MIN_VALUE (Invalid location/defcon combination)")
				return Integer.MIN_VALUE
			}
			val maxY = fld.getHighestBlockY(maxX)
			var clear = fld.clearColor(maxX, maxY, true, true, false, true)
			when {
				clear>=4 -> pts += if(defcon==5) -4 else 4
				clear==3 -> pts += 2
				clear==2 -> pts++
			}

			clear = if(rt and 1==1) {
				pts++
				fld.clearColor(maxX, maxY+1, true, true, false, true)
			} else
				fld.clearColor(maxX-1, fld.getHighestBlockY(maxX-1), true, true, false, true)
			if(clear>=4)
				pts += if(defcon==5) -4 else 4
			else if(clear==3)
				pts += 2
			else if(clear==2) pts++
		}

		// Clear
		var chain = 1
		while(true) {
			val clear = fld.clearColor(4, true, false, true)
			if(clear<=0)
				break
			else if(defcon<=4)
				if(chain==0)
					pts += clear
				else if(chain==2)
					pts += clear shl 3
				else if(chain==3)
					pts += clear shl 4
				else if(chain>=4) pts += clear*32*(chain-3)
			fld.freeFall()
			chain++
		}

		if(defcon<=3) pts += fld.getHighestBlockY(2)

		// All clear
		val allclear = fld.isEmpty
		if(allclear) pts += 1000

		if(DEBUG_ALL)
			log.debug("End of thinkMain($x, $y, $rt, "+rtOld+
				", fld, piece ${piece.id}, $defcon). pts = "+pts)
		return pts
	}

	private fun logBest(caseNum:Int) {
		log.debug("New best position found (Case "+caseNum+
			"): bestHold = "+bestHold+
			", bestX = "+bestX+
			", bestY = "+bestY+
			", bestRt = "+bestRt+
			", bestXSub = "+bestXSub+
			", bestYSub = "+bestYSub+
			", bestRtSub = "+bestRtSub+
			", bestPts = "+bestPts)
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
					thinkBestPosition(gEngine, gEngine.playerID)
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
	private class ThinkRequestMutex:java.lang.Object() {
		var active:Boolean = false

		init {
			active = false
		}

		@Synchronized fun newRequest() {
			active = true
			notifyAll()
		}
	}

	companion object {
		/** Log */
		internal val log = Logger.getLogger(Nohoho::class.java)
		/** MaximumCompromise level */
		private const val MAX_THINK_DEPTH = 2
		/** Set to true to print debug information */
		private const val DEBUG_ALL = true

		//private static final int[][] HI_PENALTY = {{6, 2}, {7, 6}, {6, 2}, {1, 0}};
		fun checkOffset(p:Piece?, engine:GameEngine):Piece {
			val result = Piece(p!!)
			result.big = engine.big
			if(!p.offsetApplied) result.applyOffsetArray(engine.ruleopt.pieceOffsetX[p.id], engine.ruleopt.pieceOffsetY[p.id])
			return result
		}

		fun getColumnDepths(fld:Field):IntArray {
			val width = fld.width
			val result = IntArray(width)
			for(x in 0 until width)
				result[x] = fld.getHighestBlockY(x)
			return result
		}
	}
}
