package net.tetrisconcept.poochy.nullpomino.ai

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.component.Piece.Shape
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.subsystem.ai.DummyAI
import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.Logger

/**
 * PoochyBot AI
 * @author Poochy.EXE
 * Poochy.Spambucket@gmail.com
 */
open class PoochyBot:DummyAI(), Runnable {

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
	private var thinkRequest:ThinkRequestMutex = ThinkRequestMutex()

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
	/** Wait extra frames at low speeds? */
	//protected static final boolean DELAY_DROP_ON = false;
	/** # of extra frames to wait */
	//protected static final int DROP_DELAY = 2;
	/** Number of frames waited */
	//protected int dropDelay;
	/** Did the thinking thread find a possible position? */
	private var thinkSuccess:Boolean = false
	/** Was the game in ARE as of the last frame? */
	private var inARE:Boolean = false

	/* AI's name */
	override val name:String
		get() = "PoochyBot V1.25"

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
		//dropDelay = 0;
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
	override fun shutdown(engine:GameEngine, playerID:Int) {
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
		else if(!thinking&&!thinkComplete||!engine.aiPrethink||engine.aiShowHint
			||engine.speed.are<=0||engine.speed.areLine<=0) {
			thinkComplete = false
			//thinkCurrentPieceNo++;
			thinkRequest.newRequest()
		}
	}

	/* Called at the start of each frame */
	override fun onFirst(engine:GameEngine, playerID:Int) {
		inputARE = 0
		val newInARE = engine.stat===GameEngine.Status.ARE
		if(engine.aiPrethink&&engine.speed.are>0&&engine.speed.areLine>0
			&&(newInARE&&!inARE||!thinking&&!thinkSuccess)) {
			if(DEBUG_ALL) log.debug("Begin pre-think of next piece.")
			thinkComplete = false
			thinkRequest.newRequest()
		}
		inARE = newInARE
		if(inARE&&delay>=engine.aiMoveDelay) {
			var input = 0
			var nextPiece = engine.getNextObject(engine.nextPieceCount)
			if(bestHold&&thinkComplete) {
				input = input or Controller.BUTTON_BIT_D
				nextPiece = if(engine.holdPieceObject==null)
					engine.getNextObject(engine.nextPieceCount+1)
				else
					engine.holdPieceObject
			}
			if(nextPiece==null) return
			nextPiece = checkOffset(nextPiece, engine)
			input = input or calcIRS(nextPiece, engine)
			if(threadRunning&&!thinking&&thinkComplete) {
				val spawnX = engine.getSpawnPosX(engine.field, nextPiece)
				if(bestX-spawnX>1)
				// left
				//setDAS = -1;
					input = input or Controller.BUTTON_BIT_LEFT
				else if(spawnX-bestX>1)
				// right
				//setDAS = 1;
					input = input or Controller.BUTTON_BIT_RIGHT
				else
					setDAS = 0
				delay = 0
			}
			if(DEBUG_ALL)
				log.debug("Currently in ARE. Next piece type = "+
					Shape.names[nextPiece.id]+", IRS = "+input)
			//engine.ctrl.setButtonBit(input);
			inputARE = input
		}
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {}

	/* Set button input states */
	override fun setControl(engine:GameEngine, playerID:Int, ctrl:Controller) {
		if(engine.nowPieceObject!=null&&engine.stat===GameEngine.Status.MOVE&&
			delay>=engine.aiMoveDelay&&engine.statc[0]>0&&
			(!engine.aiUseThread||threadRunning&&!thinking&&thinkComplete)) {
			inputARE = 0
			var input = 0 // Button input data
			val pieceNow = checkOffset(engine.nowPieceObject, engine)
			val nowX = engine.nowPieceX
			val nowY = engine.nowPieceY
			val rt = pieceNow.direction
			val fld = engine.field
			val pieceTouchGround = pieceNow.checkCollision(nowX, nowY+1, fld!!)
			val nowType = pieceNow.id
			val width = fld.width

			var moveDir = 0 //-1 = left,  1 = right
			var rotateDir = 0 //-1 = left,  1 = right
			var drop = 0 //1 = up, -1 = down
			var sync = false //true = delay either rotate or movement for synchro move if needed.

			//SpeedParam speed = engine.speed;
			//boolean lowSpeed = speed.gravity < speed.denominator;
			val canFloorKick =
				engine.nowUpwardWallkickCount<engine.ruleopt.rotateMaxUpwardWallkick||engine.ruleopt.rotateMaxUpwardWallkick<0

			//If stuck, rethink.
			/* if ((nowX < bestX && pieceNow.checkCollision(nowX+1, nowY, rt,
			 * fld)) ||
			 * (nowX > bestX && pieceNow.checkCollision(nowX-1, nowY, rt, fld)))
			 * {
			 * thinkRequest = true;
			 * if (DEBUG_ALL) log.debug("Needs rethink - piece is stuck!");
			 * } */
			/* if (rt == Piece.DIRECTION_DOWN &&
			 * ((nowType == Piece.PIECE_L && bestX > nowX) || (nowType ==
			 * Piece.PIECE_J && bestX < nowX)))
			 * {
			 * if (DEBUG_ALL) log.debug("Checking for stuck L or J piece.");
			 * if (DEBUG_ALL) log.debug("Coordinates of piece: x = " + nowX +
			 * ", y = " + nowY);
			 * if (DEBUG_ALL) log.debug("Coordinates of block to check: x = " +
			 * (pieceNow.getMaximumBlockX()+nowX-1) +
			 * ", y = " + (pieceNow.getMaximumBlockY()+nowY));
			 * for (int xCheck = 0; xCheck < fld.getWidth(); xCheck++)
			 * if (DEBUG_ALL) log.debug("fld.getHighestBlockY(" + xCheck +
			 * ") = " + fld.getHighestBlockY(xCheck));
			 * } */
			if(rt==Piece.DIRECTION_DOWN&&
				(nowType==Piece.PIECE_L&&bestX>nowX||nowType==Piece.PIECE_J&&bestX<nowX)
				&&!fld.getBlockEmpty(pieceNow.maximumBlockX+nowX-1, pieceNow.maximumBlockY+nowY)) {
				thinkComplete = false
				if(DEBUG_ALL) log.debug("Needs rethink - L or J piece is stuck!")
				thinkRequest.newRequest()
			}
			if(nowType==Piece.PIECE_O&&(bestX<nowX&&pieceNow.checkCollision(nowX-1, nowY, rt, fld)||bestX<nowX&&pieceNow.checkCollision(nowX-1, nowY, rt, fld))) {
				thinkComplete = false
				if(DEBUG_ALL) log.debug("Needs rethink - O piece is stuck!")
				thinkRequest.newRequest()
			}
			if(pieceTouchGround&&rt==bestRt&&
				(pieceNow.getMostMovableRight(nowX, nowY, rt, engine.field!!)<bestX||pieceNow.getMostMovableLeft(nowX, nowY, rt, engine.field!!)>bestX))
				stuckDelay++
			else
				stuckDelay = 0
			if(stuckDelay>4) {
				thinkComplete = false
				if(DEBUG_ALL) log.debug("Needs rethink - piece is stuck!")
				thinkRequest.newRequest()
			}
			if(nowX==lastX&&nowY==lastY&&rt==lastRt&&lastInput!=0) {
				sameStatusTime++
				if(sameStatusTime>4) {
					thinkComplete = false
					if(DEBUG_ALL) log.debug("Needs rethink - piece is stuck, last inputs had no effect!")
					thinkRequest.newRequest()
				}
			}
			if(engine.nowPieceRotateCount>=8) {
				thinkComplete = false
				if(DEBUG_ALL) log.debug("Needs rethink - piece is stuck, too many rotations!")
				thinkRequest.newRequest()
			} else
				sameStatusTime = 0
			if(bestHold&&thinkComplete&&engine.isHoldOK) {
				// Hold
				input = input or Controller.BUTTON_BIT_D

				val holdPiece = engine.holdPieceObject
				if(holdPiece!=null) input = input or calcIRS(holdPiece, engine)
			} else {
				if(DEBUG_ALL)
					log.debug("bestX = "+bestX+", nowX = "+nowX+
						", bestY = "+bestY+", nowY = "+nowY+
						", bestRt = "+bestRt+", rt = "+rt+
						", bestXSub = "+bestXSub+", bestYSub = "+bestYSub+", bestRtSub = "+bestRtSub)
				printPieceAndDirection(nowType, rt)
				// Rotation
				//Rotate iff near destination or stuck
				var xDiff = Math.abs(nowX-bestX)
				if(bestX<nowX&&nowType==Piece.PIECE_I&&
					rt==Piece.DIRECTION_DOWN&&bestRt!=rt)
					xDiff--
				val best180 = Math.abs(rt-bestRt)==2
				//Special movements for I piece
				if(nowType==Piece.PIECE_I) {
					var hypRtDir = 1
					var rotateI = false
					if((rt+3)%4==bestRt) hypRtDir = -1
					if(nowX<bestX) {
						moveDir = 1
						if(pieceNow.checkCollision(nowX+1, nowY, fld))
							if(rt and 1==0&&(canFloorKick||!pieceNow.checkCollision(nowX, nowY, (rt+1)%4, fld)))
								rotateI = true
							else if(rt and 1==1&&canFloorKick)
								rotateI = true
							else if(engine.isHoldOK&&!ctrl.isPress(Controller.BUTTON_D)) {
								if(DEBUG_ALL) log.debug("Stuck I piece - use hold")
								input = input or Controller.BUTTON_BIT_D

								val holdPiece = engine.holdPieceObject
								if(holdPiece!=null) input = input or calcIRS(holdPiece, engine)
							}
					} else if(nowX>bestX) {
						moveDir = -1
						if(pieceNow.checkCollision(nowX-1, nowY, fld))
							if(rt and 1==0&&(canFloorKick||!pieceNow.checkCollision(nowX, nowY, (rt+1)%4, fld)))
								rotateI = true
							else if(rt and 1==1&&!pieceNow.checkCollision(nowX-1, nowY, (rt+1)%4, fld)&&
								canFloorKick)
								rotateI = true
							else if(engine.isHoldOK&&!ctrl.isPress(Controller.BUTTON_D)) {
								if(DEBUG_ALL) log.debug("Stuck I piece - use hold")
								input = input or Controller.BUTTON_BIT_D

								val holdPiece = engine.holdPieceObject
								if(holdPiece!=null) input = input or calcIRS(holdPiece, engine)
							}
					} else if(rt!=bestRt)
						if(best180)
							bestRt = (bestRt+2)%4
						else
							rotateI = true
					if(rotateI) rotateDir = hypRtDir
				} else if(rt!=bestRt&&(xDiff<=1||
						bestX==0&&nowX==2&&nowType==Piece.PIECE_I||
						(nowX<bestX&&pieceNow.checkCollision(nowX+1, nowY, rt, fld)||nowX>bestX&&pieceNow.checkCollision(nowX-1, nowY, rt, fld))&&
						!(pieceNow.maximumBlockX+nowX==width-2&&rt and 1==1)&&
						!(pieceNow.minimumBlockY+nowY==2&&pieceTouchGround&&rt and 1==0&&nowType!=Piece.PIECE_I))) {
					//if (DEBUG_ALL) log.debug("Case 1 rotation");

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
				} else if(rt!=Piece.DIRECTION_UP&&xDiff>1&&engine.ruleopt.rotateButtonAllowReverse&&
					(nowType==Piece.PIECE_L||nowType==Piece.PIECE_J
						||nowType==Piece.PIECE_T))
					if(rt==Piece.DIRECTION_DOWN) {
						if(engine.ruleopt.rotateButtonAllowDouble&&!ctrl.isPress(Controller.BUTTON_E))
							input = input or Controller.BUTTON_BIT_E
						else if(nowType==Piece.PIECE_L)
							rotateDir = -1
						else if(nowType==Piece.PIECE_J)
							rotateDir = 1
						else if(nowType==Piece.PIECE_T)
							if(nowX>bestX)
								rotateDir = -1
							else if(nowX<bestX) rotateDir = 1
					} else if(rt==Piece.DIRECTION_RIGHT)
						rotateDir = -1
					else if(rt==Piece.DIRECTION_LEFT) rotateDir = 1//Try to keep flat side down on L, J, or T piece.

				// Whether reachable position
				val minX = pieceNow.getMostMovableLeft(nowX, nowY, rt, fld)
				val maxX = pieceNow.getMostMovableRight(nowX, nowY, rt, fld)

				if((bestX<minX-1||bestX>maxX+1||bestY<nowY)&&rt==bestRt) {
					// Again because it is thought unreachable
					//thinkBestPosition(engine, playerID);
					thinkComplete = false
					//thinkCurrentPieceNo++;
					//System.out.println("rethink c:" + thinkCurrentPieceNo + " l:" + thinkLastPieceNo);
					if(DEBUG_ALL) log.debug("Needs rethink - cannot reach desired position")
					thinkRequest.newRequest()
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
						} else if(nowType==Piece.PIECE_I&&rt and 1==1&&
							nowX+pieceNow.maximumBlockX==width-2&&(fld.highestBlockY<=4||fld.getHighestBlockY(width-2)-fld.getHighestBlockY(width-1)>=4)) {
							bestRt = rt
							bestX += 1
						}
					/* //Move left if need to move left, or if at rightmost
					 * position and can move left.
					 * if (pieceTouchGround && pieceNow.id != Piece.PIECE_I &&
					 * nowX+pieceNow.getMaximumBlockX() == width-1 &&
					 * !pieceNow.checkCollision(nowX-1, nowY, fld))
					 * {
					 * if(!ctrl.isPress(Controller.BUTTON_LEFT) &&
					 * (engine.aiMoveDelay >= 0))
					 * input |= Controller.BUTTON_BIT_LEFT;
					 * bestX = nowX - 1;
					 * } */
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

			val minBlockX = nowX+pieceNow.minimumBlockX
			val maxBlockX = nowX+pieceNow.maximumBlockX
			val minBlockXDepth = fld.getHighestBlockY(minBlockX)
			val maxBlockXDepth = fld.getHighestBlockY(maxBlockX)
			if(nowType==Piece.PIECE_L&&minBlockXDepth<maxBlockXDepth&&pieceTouchGround
				&&rt==Piece.DIRECTION_DOWN&&rotateDir==-1&&maxBlockX<width-1) {
				if(bestX==nowX+1)
					moveDir = 1
				else if(bestX<nowX) {
					if(DEBUG_ALL) log.debug("Delaying rotation on L piece to avoid getting stuck. (Case 1)")
					sync = false
					rotateDir = 0
					moveDir = 1
				} else if(bestX>nowX) {
					/* if (minBlockXDepth == fld.getHighestBlockY(minBlockX-1))
					 * {
					 * if (DEBUG_ALL) log.
					 * debug("Delaying rotation on L piece to avoid getting stuck. (Case 2)"
					 * );
					 * sync = false;
					 * rotateDir = 0;
					 * moveDir = -1;
					 * }
					 * else */
					if(DEBUG_ALL) log.debug("Attempting synchro move on L piece to avoid getting stuck.")
					sync = true
					rotateDir = -1
					moveDir = -1
				}
			} else if(nowType==Piece.PIECE_J&&minBlockXDepth>maxBlockXDepth&&pieceTouchGround
				&&rt==Piece.DIRECTION_DOWN&&rotateDir==1&&minBlockX>0) {
				if(bestX==nowX-1)
					moveDir = -1
				else if(bestX>nowX) {
					if(DEBUG_ALL) log.debug("Delaying rotation on J piece to avoid getting stuck. (Case 1)")
					sync = false
					rotateDir = 0
					moveDir = -1
				} else if(bestX<nowX) {
					/* if (maxBlockXDepth == fld.getHighestBlockY(maxBlockX+1))
					 * {
					 * if (DEBUG_ALL) log.
					 * debug("Delaying rotation on J piece to avoid getting stuck. (Case 2)"
					 * );
					 * sync = false;
					 * rotateDir = 0;
					 * moveDir = 1;
					 * }
					 * else */
					if(DEBUG_ALL) log.debug("Attempting synchro move on J piece to avoid getting stuck.")
					sync = true
					rotateDir = 1
					moveDir = 1
				}
			} else if(rotateDir!=0&&moveDir!=0&&pieceTouchGround&&rt and 1==1
				&&(nowType==Piece.PIECE_J||nowType==Piece.PIECE_L)
				&&!pieceNow.checkCollision(nowX+moveDir, nowY+1, rt, fld)) {
				if(DEBUG_ALL) log.debug("Delaying move on L or J piece to avoid getting stuck.")
				sync = false
				moveDir = 0
			}
			if(engine.nowPieceRotateCount>=5&&rotateDir!=0&&moveDir!=0&&!sync) {
				if(DEBUG_ALL) log.debug("Piece seems to be stuck due to unintentional synchro - trying intentional desync.")
				moveDir = 0
			}
			if(moveDir==-1&&minBlockX==1&&nowType==Piece.PIECE_I&&rt and 1==1
				&&pieceNow.checkCollision(nowX-1, nowY, rt, fld)) {
				val depthNow = fld.getHighestBlockY(minBlockX)
				val depthLeft = fld.getHighestBlockY(minBlockX-1)
				if(depthNow>depthLeft&&depthNow-depthLeft<2)
					if(!pieceNow.checkCollision(nowX+1, nowY, rt, fld))
						moveDir = 1
					else if(engine.isHoldOK&&!ctrl.isPress(Controller.BUTTON_D))
						input = input or Controller.BUTTON_BIT_D
			}
			/* //Catch bug where it fails to rotate J piece
			 * if (moveDir == 0 && rotateDir == 0 & drop == 0)
			 * {
			 * if ((rt+1)%4 == bestRt)
			 * rotateDir = 1;
			 * else if ((rt+3)%4 == bestRt)
			 * rotateDir = -1;
			 * else if ((rt+2)%4 == bestRt)
			 * {
			 * if(engine.ruleopt.rotateButtonAllowDouble)
			 * rotateDir = 2;
			 * else if (rt == 3)
			 * rotateDir = -1;
			 * else
			 * rotateDir = -1;
			 * }
			 * else if (bestX < nowX)
			 * moveDir = -1;
			 * else if (bestX > nowX)
			 * moveDir = 1;
			 * else
			 * if (DEBUG_ALL) log.debug("Movement error: Nothing to do!");
			 * }
			 * if (rotateDir == 0 && Math.abs(rt - bestRt) == 2)
			 * rotateDir = 1; */
			//Convert parameters to input
			val useDAS = engine.dasCount>=engine.speed.das&&moveDir==setDAS
			if(moveDir==-1&&(!ctrl.isPress(Controller.BUTTON_LEFT)||useDAS))
				input = input or Controller.BUTTON_BIT_LEFT
			else if(moveDir==1&&(!ctrl.isPress(Controller.BUTTON_RIGHT)||useDAS))
				input = input or Controller.BUTTON_BIT_RIGHT
			/* if(drop == 1 && !ctrl.isPress(Controller.BUTTON_UP))
			 * {
			 * if (DELAY_DROP_ON && lowSpeed && dropDelay < (DROP_DELAY >> 1))
			 * dropDelay++;
			 * else
			 * input |= Controller.BUTTON_BIT_UP;
			 * }
			 * else if(drop == -1)
			 * {
			 * if (DELAY_DROP_ON && lowSpeed && dropDelay < DROP_DELAY)
			 * dropDelay++;
			 * else
			 * input |= Controller.BUTTON_BIT_DOWN;
			 * } */
			if(drop==1&&!ctrl.isPress(Controller.BUTTON_UP))
				input = input or Controller.BUTTON_BIT_UP
			else if(drop==-1) input = input or Controller.BUTTON_BIT_DOWN

			if(rotateDir!=0) {
				val defaultRotateRight =
					engine.owRotateButtonDefaultRight==1||engine.owRotateButtonDefaultRight==-1&&engine.ruleopt.rotateButtonDefaultRight

				if(engine.ruleopt.rotateButtonAllowDouble&&
					rotateDir==2&&!ctrl.isPress(Controller.BUTTON_E))
					input = input or Controller.BUTTON_BIT_E
				else if(engine.ruleopt.rotateButtonAllowReverse&&
					!defaultRotateRight&&rotateDir==1) {
					if(!ctrl.isPress(Controller.BUTTON_B)) input = input or Controller.BUTTON_BIT_B
				} else if(engine.ruleopt.rotateButtonAllowReverse&&
					defaultRotateRight&&rotateDir==-1) {
					if(!ctrl.isPress(Controller.BUTTON_B)) input = input or Controller.BUTTON_BIT_B
				} else if(!ctrl.isPress(Controller.BUTTON_A)) input = input or Controller.BUTTON_BIT_A
			}
			if(sync) {
				if(DEBUG_ALL) log.debug("Attempting to perform synchro move.")
				val bitsLR = Controller.BUTTON_BIT_LEFT or Controller.BUTTON_BIT_RIGHT
				val bitsAB = Controller.BUTTON_BIT_A or Controller.BUTTON_BIT_B
				if(input and bitsLR==0||input and bitsAB==0) {
					setDAS = 0
					input = input and (bitsLR or bitsAB).inv()
				}
			}
			if(setDAS!=moveDir) setDAS = 0

			lastInput = input
			lastX = nowX
			lastY = nowY
			lastRt = rt

			if(DEBUG_ALL)
				log.debug("Input = "+input+", moveDir = "+moveDir+", rotateDir = "+rotateDir+
					", sync = "+sync+", drop = "+drop+", setDAS = "+setDAS)

			delay = 0
			ctrl.buttonBit = input
		} else {
			//dropDelay = 0;
			delay++
			ctrl.buttonBit = inputARE
		}
	}

	private fun printPieceAndDirection(pieceType:Int, rt:Int) {
		var result = "Piece "+Shape.names[pieceType]+", direction "

		when(rt) {
			Piece.DIRECTION_LEFT -> result += "left"
			Piece.DIRECTION_DOWN -> result += "down"
			Piece.DIRECTION_UP -> result += "up"
			Piece.DIRECTION_RIGHT -> result += "right"
		}
		if(DEBUG_ALL) log.debug(result)
	}

	private fun calcIRS(piece:Piece?, engine:GameEngine):Int {
		val p = checkOffset(piece, engine)
		val nextType = p.id
		val fld = engine.field
		val spawnX = engine.getSpawnPosX(fld, p)
		val speed = engine.speed
		val gravityHigh = speed.gravity>speed.denominator
		val width = fld!!.width
		val midColumnX = width/2-1
		return when {
			Math.abs(spawnX-bestX)==1 ->
				if(bestRt==1) if(engine.ruleopt.rotateButtonDefaultRight) Controller.BUTTON_BIT_A else Controller.BUTTON_BIT_B
			 else if(bestRt==3) if(engine.ruleopt.rotateButtonDefaultRight) Controller.BUTTON_BIT_B else Controller.BUTTON_BIT_A
			else 0

			nextType==Piece.PIECE_L ->
				if(gravityHigh&&fld.getHighestBlockY(midColumnX-1)<minOf(fld.getHighestBlockY(midColumnX), fld.getHighestBlockY(midColumnX+1)))
				0 else if(engine.ruleopt.rotateButtonDefaultRight) Controller.BUTTON_BIT_B else Controller.BUTTON_BIT_A
			nextType==Piece.PIECE_J -> {
				if(gravityHigh&&fld.getHighestBlockY(midColumnX+1)<minOf(fld.getHighestBlockY(midColumnX), fld.getHighestBlockY(midColumnX-1)))
					0 else if(engine.ruleopt.rotateButtonDefaultRight) Controller.BUTTON_BIT_A else Controller.BUTTON_BIT_B
			}
			/* else if (nextType == Piece.PIECE_I)
			 * return Controller.BUTTON_BIT_A; */
			else ->0
		}
		/* else if (nextType == Piece.PIECE_I)
		 * return Controller.BUTTON_BIT_A; */
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

		val fld:Field = if(engine.stat===GameEngine.Status.READY)
			Field(engine.fieldWidth, engine.fieldHeight, engine.fieldHiddenHeight, engine.ruleopt.fieldCeiling)
		else
			Field(engine.field!!)
		var pieceNow = engine.nowPieceObject
		var pieceHold = engine.holdPieceObject
		/* Piece pieceNow = null;
		 * if (engine.nowPieceObject != null)
		 * pieceNow = new Piece(engine.nowPieceObject);
		 * Piece pieceHold = null;
		 * if (engine.holdPieceObject != null)
		 * pieceHold = new Piece(engine.holdPieceObject); */
		val nowX:Int
		val nowY:Int
		val nowRt:Int
		if(inARE||pieceNow==null) {
			pieceNow = engine.getNextObjectCopy(engine.nextPieceCount)
			nowX = engine.getSpawnPosX(fld, pieceNow)
			nowY = engine.getSpawnPosY(pieceNow)
			nowRt = engine.ruleopt.pieceDefaultDirection[pieceNow!!.id]
			if(pieceHold==null) pieceHold = engine.getNextObjectCopy(engine.nextPieceCount+1)
		} else {
			nowX = engine.nowPieceX
			nowY = engine.nowPieceY
			nowRt = pieceNow.direction
			if(pieceHold==null) pieceHold = engine.getNextObjectCopy(engine.nextPieceCount)
		}
		pieceNow = checkOffset(pieceNow, engine)
		pieceHold = checkOffset(pieceHold, engine)
		if(pieceHold.id==pieceNow.id) pieceHold = null
		/* if (!pieceNow.offsetApplied)
		 * pieceNow.applyOffsetArray(engine.ruleopt.pieceOffsetX[pieceNow.id],
		 * engine.ruleopt.pieceOffsetY[pieceNow.id]);
		 * if (!pieceHold.offsetApplied)
		 * pieceHold.applyOffsetArray(engine.ruleopt.pieceOffsetX[pieceHold.id],
		 * engine.ruleopt.pieceOffsetY[pieceHold.id]); */
		val holdOK = engine.isHoldOK

		val canFloorKick =
			engine.nowUpwardWallkickCount<engine.ruleopt.rotateMaxUpwardWallkick||engine.ruleopt.rotateMaxUpwardWallkick<0
		val canFloorKickI = pieceNow.id==Piece.PIECE_I&&nowRt and 1==0&&canFloorKick
		var canFloorKickT = pieceNow.id==Piece.PIECE_T&&nowRt!=Piece.DIRECTION_UP&&canFloorKick
		if(canFloorKickT&&!pieceNow.checkCollision(nowX, nowY, Piece.DIRECTION_UP, fld))
			canFloorKickT = false
		else if(canFloorKickT&&!pieceNow.checkCollision(nowX-1, nowY, Piece.DIRECTION_UP, fld))
			canFloorKickT = false
		else if(canFloorKickT&&!pieceNow.checkCollision(nowX+1, nowY, Piece.DIRECTION_UP, fld))
			canFloorKickT = false

		var move = 1
		if(engine.big) move = 2

		for(depth in 0 until MAX_THINK_DEPTH) {
			/* int dirCount = Piece.DIRECTION_COUNT;
			 * if (pieceNow.id == Piece.PIECE_I || pieceNow.id == Piece.PIECE_S
			 * || pieceNow.id == Piece.PIECE_Z)
			 * dirCount = 2;
			 * else if (pieceNow.id == Piece.PIECE_O)
			 * dirCount = 1; */
			for(rt in 0 until Piece.DIRECTION_COUNT) {
				var tempY = nowY
				if(canFloorKickI&&rt and 1==1)
					tempY -= 2
				else if(canFloorKickT&&rt==Piece.DIRECTION_UP) tempY--

				val minX =
					maxOf(mostMovableX(nowX, tempY, -1, engine, fld, pieceNow, rt), pieceNow.getMostMovableLeft(nowX, tempY, rt, engine.field!!))
				val maxX =
					minOf(mostMovableX(nowX, tempY, 1, engine, fld, pieceNow, rt), pieceNow.getMostMovableRight(nowX, tempY, rt, engine.field!!))
				var spawnOK = true
				if(engine.stat===GameEngine.Status.ARE) {
					val spawnX = engine.getSpawnPosX(fld, pieceNow)
					val spawnY = engine.getSpawnPosY(pieceNow)
					spawnOK = !pieceNow.checkCollision(spawnX, spawnY, fld)
				}
				run {
					var x = minX
					while(x<=maxX&&spawnOK) {
						fld.copy(engine.field!!)
						val y = pieceNow.getBottom(x, tempY, rt, fld)

						if(!pieceNow.checkCollision(x, y, rt, fld)) {
							// As it is
							var pts = thinkMain(x, y, rt, -1, fld, pieceNow, depth)

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
							//Check regardless
							//if((depth > 0) || (bestPts <= 10) || (pieceNow.id == Piece.PIECE_T)) {
							// Left shift
							fld.copy(engine.field!!)
							if(!pieceNow.checkCollision(x-move, y, rt, fld)&&pieceNow.checkCollision(x-move, y-1, rt, fld)) {
								pts = thinkMain(x-move, y, rt, -1, fld, pieceNow, depth)

								if(pts>bestPts) {
									bestHold = false
									bestX = x
									bestY = y
									bestRt = rt
									bestXSub = x-move
									bestYSub = y
									bestRtSub = -1
									bestPts = pts
									if(DEBUG_ALL) logBest(2)
									thinkSuccess = true
								}
							}

							// Right shift
							fld.copy(engine.field!!)
							if(!pieceNow.checkCollision(x+move, y, rt, fld)&&pieceNow.checkCollision(x+1, y-move, rt, fld)) {
								pts = thinkMain(x+move, y, rt, -1, fld, pieceNow, depth)

								if(pts>bestPts) {
									bestHold = false
									bestX = x
									bestY = y
									bestRt = rt
									bestXSub = x+1
									bestYSub = y
									bestRtSub = -1
									bestPts = pts
									if(DEBUG_ALL) logBest(3)
									thinkSuccess = true
								}
							}

							// Left rotation
							if(!engine.ruleopt.rotateButtonDefaultRight||engine.ruleopt.rotateButtonAllowReverse) {
								val rot = pieceNow.getRotateDirection(-1, rt)
								var newX = x
								var newY = y
								fld.copy(engine.field!!)
								pts = Integer.MIN_VALUE

								if(!pieceNow.checkCollision(x, y, rot, fld))
									pts = thinkMain(x, y, rot, rt, fld, pieceNow, depth)
								else if(engine.wallkick!=null&&engine.ruleopt.rotateWallkick) {
									val allowUpward =
										engine.ruleopt.rotateMaxUpwardWallkick<0||engine.nowUpwardWallkickCount<engine.ruleopt.rotateMaxUpwardWallkick
									engine.wallkick!!.executeWallkick(x, y, -1, rt, rot, allowUpward, pieceNow, fld, null)?.let{kick->
										newX = x+kick.offsetX
										newY = y+kick.offsetY
										pts = thinkMain(newX, newY, rot, rt, fld, pieceNow, depth)
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
									if(DEBUG_ALL) logBest(4)
									thinkSuccess = true
								}
							}

							// Right rotation
							if(engine.ruleopt.rotateButtonDefaultRight||engine.ruleopt.rotateButtonAllowReverse) {
								val rot = pieceNow.getRotateDirection(1, rt)
								var newX = x
								var newY = y
								fld.copy(engine.field!!)
								pts = Integer.MIN_VALUE

								if(!pieceNow.checkCollision(x, y, rot, fld))
									pts = thinkMain(x, y, rot, rt, fld, pieceNow, depth)
								else if(engine.wallkick!=null&&engine.ruleopt.rotateWallkick) {
									val allowUpward =
										engine.ruleopt.rotateMaxUpwardWallkick<0||engine.nowUpwardWallkickCount<engine.ruleopt.rotateMaxUpwardWallkick
									engine.wallkick!!.executeWallkick(x, y, 1, rt, rot, allowUpward, pieceNow, fld, null)?.let{kick->
										newX = x+kick.offsetX
										newY = y+kick.offsetY
										pts = thinkMain(newX, newY, rot, rt, fld, pieceNow, depth)
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
									if(DEBUG_ALL) logBest(5)
									thinkSuccess = true
								}
							}

							// 180-degree rotation
							if(engine.ruleopt.rotateButtonAllowDouble) {
								val rot = pieceNow.getRotateDirection(2, rt)
								var newX = x
								var newY = y
								fld.copy(engine.field!!)
								pts = Integer.MIN_VALUE

								if(!pieceNow.checkCollision(x, y, rot, fld))
									pts = thinkMain(x, y, rot, rt, fld, pieceNow, depth)
								else if(engine.wallkick!=null&&engine.ruleopt.rotateWallkick) {
									val allowUpward =
										engine.ruleopt.rotateMaxUpwardWallkick<0||engine.nowUpwardWallkickCount<engine.ruleopt.rotateMaxUpwardWallkick
									engine.wallkick!!.executeWallkick(x, y, 2, rt, rot, allowUpward, pieceNow, fld, null)?.let{kick->
										newX = x+kick.offsetX
										newY = y+kick.offsetY
										pts = thinkMain(newX, newY, rot, rt, fld, pieceNow, depth)
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
									if(DEBUG_ALL) logBest(6)
									thinkSuccess = true
								}
							}
							//}
						}
						x += move
					}
				}

				// Hold piece
				if(holdOK&&pieceHold!=null) {
					val spawnX = engine.getSpawnPosX(engine.field, pieceHold)
					val spawnY = engine.getSpawnPosY(pieceHold)
					val minHoldX =
						maxOf(mostMovableX(spawnX, spawnY, -1, engine, engine.field, pieceHold, rt), pieceHold.getMostMovableLeft(spawnX, spawnY, rt, engine.field!!))
					val maxHoldX =
						minOf(mostMovableX(spawnX, spawnY, 1, engine, engine.field, pieceHold, rt), pieceHold.getMostMovableRight(spawnX, spawnY, rt, engine.field!!))

					//Bonus for holding an I piece, penalty for holding an S or Z.
					val holdType = pieceHold.id
					var holdPts = 0
					if(holdType==Piece.PIECE_I)
						holdPts -= 30
					else if(holdType==Piece.PIECE_S||holdType==Piece.PIECE_Z)
						holdPts += 30
					else if(holdType==Piece.PIECE_O) holdPts += 10
					val nowType = pieceNow.id
					if(nowType==Piece.PIECE_I)
						holdPts += 30
					else if(nowType==Piece.PIECE_S||nowType==Piece.PIECE_Z)
						holdPts -= 30
					else if(nowType==Piece.PIECE_O) holdPts -= 10

					var x = minHoldX
					while(x<=maxHoldX) {
						fld.copy(engine.field!!)
						val y = pieceHold.getBottom(x, spawnY, rt, fld)

						if(!pieceHold.checkCollision(x, y, rt, fld)) {
							// As it is
							var pts = thinkMain(x, y, rt, -1, fld, pieceHold, depth)
							if(pts>Integer.MIN_VALUE+30) pts += holdPts
							if(pts>=bestPts) {
								bestHold = true
								bestX = x
								bestY = y
								bestRt = rt
								bestXSub = x
								bestYSub = y
								bestRtSub = -1
								bestPts = pts
								if(DEBUG_ALL) logBest(7)
								thinkSuccess = true
							}
							//Check regardless
							//if((depth > 0) || (bestPts <= 10) || (pieceHold.id == Piece.PIECE_T)) {
							// Left shift
							fld.copy(engine.field!!)
							if(!pieceHold.checkCollision(x-move, y, rt, fld)&&pieceHold.checkCollision(x-move, y-1, rt, fld)) {
								pts = thinkMain(x-move, y, rt, -1, fld, pieceHold, depth)
								if(pts>Integer.MIN_VALUE+30) pts += holdPts
								if(pts>bestPts) {
									bestHold = true
									bestX = x
									bestY = y
									bestRt = rt
									bestXSub = x-move
									bestYSub = y
									bestRtSub = -1
									bestPts = pts
									if(DEBUG_ALL) logBest(8)
									thinkSuccess = true
								}
							}

							// Right shift
							fld.copy(engine.field!!)
							if(!pieceHold.checkCollision(x+move, y, rt, fld)&&pieceHold.checkCollision(x+move, y-1, rt, fld)) {
								pts = thinkMain(x+move, y, rt, -1, fld, pieceHold, depth)
								if(pts>Integer.MIN_VALUE+30) pts += holdPts
								if(pts>bestPts) {
									bestHold = true
									bestX = x
									bestY = y
									bestRt = rt
									bestXSub = x+move
									bestYSub = y
									bestRtSub = -1
									bestPts = pts
									if(DEBUG_ALL) logBest(9)
									thinkSuccess = true
								}
							}

							// Left rotation
							if(!engine.ruleopt.rotateButtonDefaultRight||engine.ruleopt.rotateButtonAllowReverse) {
								val rot = pieceHold.getRotateDirection(-1, rt)
								var newX = x
								var newY = y
								fld.copy(engine.field!!)
								pts = Integer.MIN_VALUE

								if(!pieceHold.checkCollision(x, y, rot, fld))
									pts = thinkMain(x, y, rot, rt, fld, pieceHold, depth)
								else if(engine.wallkick!=null&&engine.ruleopt.rotateWallkick) {
									val allowUpward =
										engine.ruleopt.rotateMaxUpwardWallkick<0||engine.nowUpwardWallkickCount<engine.ruleopt.rotateMaxUpwardWallkick
									engine.wallkick!!.executeWallkick(x, y, -1, rt, rot, allowUpward, pieceHold, fld, null)?.let{kick->
										newX = x+kick.offsetX
										newY = y+kick.offsetY
										pts = thinkMain(newX, newY, rot, rt, fld, pieceHold, depth)
									}
								}
								if(pts>Integer.MIN_VALUE+30) pts += holdPts
								if(pts>bestPts) {
									bestHold = true
									bestX = x
									bestY = y
									bestRt = rt
									bestXSub = newX
									bestYSub = newY
									bestRtSub = rot
									bestPts = pts
									if(DEBUG_ALL) logBest(10)
									thinkSuccess = true
								}
							}

							// Right rotation
							if(engine.ruleopt.rotateButtonDefaultRight||engine.ruleopt.rotateButtonAllowReverse) {
								val rot = pieceHold.getRotateDirection(1, rt)
								var newX = x
								var newY = y
								fld.copy(engine.field!!)
								pts = Integer.MIN_VALUE

								if(!pieceHold.checkCollision(x, y, rot, fld))
									pts = thinkMain(x, y, rot, rt, fld, pieceHold, depth)
								else if(engine.wallkick!=null&&engine.ruleopt.rotateWallkick) {
									val allowUpward =
										engine.ruleopt.rotateMaxUpwardWallkick<0||engine.nowUpwardWallkickCount<engine.ruleopt.rotateMaxUpwardWallkick
									engine.wallkick!!.executeWallkick(x, y, 1, rt, rot, allowUpward, pieceHold, fld, null)?.let{kick->
										newX = x+kick.offsetX
										newY = y+kick.offsetY
										pts = thinkMain(newX, newY, rot, rt, fld, pieceHold, depth)
									}
								}
								if(pts>Integer.MIN_VALUE+30) pts += holdPts
								if(pts>bestPts) {
									bestHold = true
									bestX = x
									bestY = y
									bestRt = rt
									bestXSub = newX
									bestYSub = newY
									bestRtSub = rot
									bestPts = pts
									if(DEBUG_ALL) logBest(11)
									thinkSuccess = true
								}
							}

							// 180-degree rotation
							if(engine.ruleopt.rotateButtonAllowDouble) {
								val rot = pieceHold.getRotateDirection(2, rt)
								var newX = x
								var newY = y
								fld.copy(engine.field!!)
								pts = Integer.MIN_VALUE

								if(!pieceHold.checkCollision(x, y, rot, fld))
									pts = thinkMain(x, y, rot, rt, fld, pieceHold, depth)
								else if(engine.wallkick!=null&&engine.ruleopt.rotateWallkick) {
									val allowUpward =
										engine.ruleopt.rotateMaxUpwardWallkick<0||engine.nowUpwardWallkickCount<engine.ruleopt.rotateMaxUpwardWallkick
									engine.wallkick!!.executeWallkick(x, y, 2, rt, rot, allowUpward, pieceHold, fld, null)?.let{kick->
										newX = x+kick.offsetX
										newY = y+kick.offsetY
										pts = thinkMain(newX, newY, rot, rt, fld, pieceHold, depth)
									}
								}
								if(pts>Integer.MIN_VALUE+30) pts += holdPts
								if(pts>bestPts) {
									bestHold = true
									bestX = x
									bestY = y
									bestRt = rt
									bestXSub = newX
									bestYSub = newY
									bestRtSub = rot
									bestPts = pts
									if(DEBUG_ALL) logBest(12)
									thinkSuccess = true
								}
							}
						}
						x += move
					}
				}
			}

			if(bestPts>0)
				break
			else
				bestPts = Integer.MIN_VALUE
		}

		if(engine.aiShowHint) {
			bestX = bestXSub
			bestY = bestYSub
			if(bestRtSub!=-1) bestRt = bestRtSub
		}
		//thinkLastPieceNo++;

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
	 * @param depth Compromise level (ranges from 0 through getMaxThinkDepth-1)
	 * @return Evaluation score
	 */
	open fun thinkMain(x:Int, y:Int, rt:Int, rtOld:Int, fld:Field, piece:Piece?, depth:Int):Int {
		var pts = 0

		val big = piece!!.big
		var move = 1
		if(big) move = 2

		// Add points for being adjacent to other blocks
		if(piece.checkCollision(x-1, y, fld)) pts += 1
		if(piece.checkCollision(x+1, y, fld)) pts += 1
		if(piece.checkCollision(x, y-1, fld)) pts += 1000

		val width = fld.width
		val height = fld.height

		val xMin = piece.minimumBlockX+x
		val xMax = piece.maximumBlockX+x

		// Number of holes and valleys needing an I piece (before placement)
		val holeBefore = fld.howManyHoles
		//int lidBefore = fld.getHowManyLidAboveHoles();

		//Check number of holes in rightmost column
		var testY = fld.hiddenHeight
		var holeBeforeRCol = 0
		if(!big) {
			while(fld.getBlockEmpty(width-1, testY)&&testY<height)
				testY++
			while(!fld.getBlockEmpty(width-1, testY)&&testY<height)
				testY++
			while(testY<height) {
				if(fld.getBlockEmpty(width-1, testY)) holeBeforeRCol++
				testY++
			}
		}
		//Fetch depths and find valleys that require an I, J, or L.
		val depthsBefore = getColumnDepths(fld)
		var deepestY = -1
		//int deepestX = -1;
		for(i in 0 until width-1)
			if(depthsBefore[i]>deepestY) deepestY = depthsBefore[i]
		//deepestX = i;
		val valleysBefore = calcValleys(depthsBefore, move)

		// Field height (before placement)
		val heightBefore = fld.highestBlockY
		// T-Spin flag
		var tspin = false
		if(piece.id==Piece.PIECE_T&&rtOld!=-1&&fld.isTSpinSpot(x, y, piece.big)) tspin = true

		//Does move fill in valley with an I piece?
		var valley = 0
		if(piece.id==Piece.PIECE_I)
			if(xMin==xMax&&0<=xMin&&xMin<width) {
				//if (DEBUG_ALL) log.debug("actualX = " + xMin);
				val xDepth = depthsBefore[xMin]
				var sideDepth = -1
				if(xMin>=move) sideDepth = depthsBefore[xMin-move]
				if(xMin<width-move) sideDepth = maxOf(sideDepth, depthsBefore[xMin+move])
				valley = xDepth-sideDepth
				//if (DEBUG_ALL) log.debug("valley = " + valley);
			}

		// Place the piece
		if(!piece.placeToField(x, y, rt, fld)) {
			if(DEBUG_ALL)
				log.debug("End of thinkMain("+x+", "+y+", "+rt+", "+rtOld+
					", fld, piece "+Shape.names[piece.id]+", "+depth+"). pts = 0 (Cannot place piece)")
			return Integer.MIN_VALUE
		}

		// Line clear
		val lines = fld.checkLine()/move
		if(lines>0) {
			fld.clearLine()
			fld.downFloatingBlocks()
		}

		// All clear
		val allclear = fld.isEmpty
		if(allclear) pts += 500000

		// Field height (after clears)
		val heightAfter = fld.highestBlockY

		val depthsAfter = getColumnDepths(fld)

		// Danger flag
		val danger = heightBefore<=4*(move+1)
		//Flag for really dangerously high stacks
		val peril = heightBefore<=2*(move+1)

		// Additional points for lower placements
		pts += if(!danger&&depth==0)
			y*10
		else
			y*20

		val holeAfter = fld.howManyHoles

		val rColPenalty = 1000
		/* if (danger)
		 * rColPenalty = 100; */
		//Apply score penalty if I piece would overflow canyon,
		//unless it would also uncover a hole.
		if(!big&&piece.id==Piece.PIECE_I&&holeBefore<=holeAfter&&xMax==width-1) {
			val rValleyDepth = depthsAfter[width-1-move]-depthsAfter[width-1]
			if(rValleyDepth>0) pts -= (rValleyDepth+1)*rColPenalty
		}
		//Bonus points for filling in valley with an I piece
		var valleyBonus = 0
		if(valley==3&&xMax<width-1)
			valleyBonus = 40000
		else if(valley>=4) valleyBonus = 400000
		if(xMax==0) valleyBonus *= 2
		if(valley>0&&DEBUG_ALL)
			log.debug("I piece xMax = "+xMax+", valley depth = "+valley+
				", valley bonus = "+valleyBonus)
		pts += valleyBonus
		if(lines==1&&!danger&&depth==0&&heightAfter>=16&&holeBefore<3&&
			!tspin&&xMax==width-1) {
			if(DEBUG_ALL)
				log.debug("End of thinkMain("+x+", "+y+", "+rt+", "+rtOld+
					", fld, piece "+Shape.names[piece.id]+", "+depth+"). pts = 0 (Special Condition 3)")
			return Integer.MIN_VALUE
		}
		//Points for line clears
		if(peril) {
			if(lines==1) pts += 500000
			if(lines==2) pts += 1000000
			if(lines==3) pts += 30000000
			if(lines>=4) pts += 100000000
		} else if(!danger&&depth==0) {
			if(lines==1) pts += 10
			if(lines==2) pts += 50
			if(lines==3) pts += 1000
			if(lines>=4) pts += 100000
		} else {
			if(lines==1) pts += 50000
			if(lines==2) pts += 100000
			if(lines==3) pts += 300000
			if(lines>=4) pts += 1000000
		}

		if(lines<4&&!allclear) {
			// Number of holes and valleys needing an I piece (after placement)
			//int lidAfter = fld.getHowManyLidAboveHoles();

			//Find valleys that need an I, J, or L.
			val valleysAfter = calcValleys(depthsAfter, move)

			if(holeAfter>holeBefore) {
				// Demerits for new holes
				if(depth==0) return Integer.MIN_VALUE
				pts -= (holeAfter-holeBefore)*400
			} else if(holeAfter<holeBefore) {
				// Add points for reduction in number of holes
				pts += 10000
				pts += if(!danger)
					(holeBefore-holeAfter)*200
				else
					(holeBefore-holeAfter)*400
			}

			/* if(lidAfter < lidBefore) {
			 * // Add points for reduction in number blocks above holes
			 * pts += (lidAfter - lidBefore) * 500;
			 * } */

			if(tspin&&lines>=1)
			// T-Spin Bonus - retained from Basic AI, but should never actually trigger
				pts += 100000*lines

			testY = fld.hiddenHeight
			var holeAfterRCol = 0
			if(!big) {
				//Check number of holes in rightmost column
				while(fld.getBlockEmpty(width-1, testY)&&testY<height)
					testY++
				while(!fld.getBlockEmpty(width-1, testY)&&testY<height)
					testY++
				while(testY<height) {
					if(fld.getBlockEmpty(width-1, testY)) holeAfterRCol++
					testY++
				}
				//Apply score penalty if non-I piece would plug up canyon
				val deltaRColHoles = holeAfterRCol-holeBeforeRCol
				pts -= deltaRColHoles*rColPenalty
			}

			//Bonuses and penalties for valleys that need I, J, or L.
			var needIValleyDiffScore = 0
			if(valleysBefore[0]>0) needIValleyDiffScore = 1 shl valleysBefore[0]
			if(valleysAfter[0]>0) needIValleyDiffScore -= 1 shl valleysAfter[0]

			var needLJValleyDiffScore = 0
			var needLOrJValleyDiffScore = 0

			if(valleysBefore[1]>3) {
				needLJValleyDiffScore += 1 shl (valleysBefore[1] shr 1)
				needLOrJValleyDiffScore += valleysBefore[1] and 1
			} else
				needLOrJValleyDiffScore += valleysBefore[1] and 3
			if(valleysAfter[1]>3) {
				needLJValleyDiffScore -= 1 shl (valleysAfter[1] shr 1)
				needLOrJValleyDiffScore -= valleysAfter[1] and 1
			} else
				needLOrJValleyDiffScore -= valleysAfter[1] and 3
			if(valleysBefore[2]>3) {
				needLJValleyDiffScore += 1 shl (valleysBefore[2] shr 1)
				needLOrJValleyDiffScore += valleysBefore[2] and 1
			} else
				needLOrJValleyDiffScore += valleysBefore[2] and 3
			if(valleysAfter[2]>3) {
				needLJValleyDiffScore -= 1 shl (valleysAfter[2] shr 1)
				needLOrJValleyDiffScore -= valleysAfter[2] and 1
			} else
				needLOrJValleyDiffScore -= valleysAfter[2] and 3

			if(needIValleyDiffScore<0&&holeAfter>=holeBefore) {
				pts += needIValleyDiffScore*200
				if(depth==0) return Integer.MIN_VALUE
			} else if(needIValleyDiffScore>0)
				pts += if(depth==0&&!danger)
					needIValleyDiffScore*100
				else
					needIValleyDiffScore*200
			if(needLJValleyDiffScore<0&&holeAfter>=holeBefore) {
				pts += needLJValleyDiffScore*40
				if(depth==0) return Integer.MIN_VALUE
			} else if(needLJValleyDiffScore>0)
				pts += if(depth==0&&!danger)
					needLJValleyDiffScore*20
				else
					needLJValleyDiffScore*40

			if(needLOrJValleyDiffScore<0&&holeAfter>=holeBefore)
				pts += needLJValleyDiffScore*40
			else if(needLOrJValleyDiffScore>0)
				pts += if(!danger)
					needLOrJValleyDiffScore*20
				else
					needLOrJValleyDiffScore*40

			if(!big) {
				//Bonus for pyramidal stack
				val mid = width/2-1
				var d:Int
				for(i in 0 until mid-1) {
					d = depthsAfter[i]-depthsAfter[i+1]
					pts += if(d>=0)
						10
					else
						d
				}
				for(i in mid+2 until width) {
					d = depthsAfter[i]-depthsAfter[i-1]
					pts += if(d>=0)
						10
					else
						d
				}
				d = depthsAfter[mid-1]-depthsAfter[mid]
				pts += if(d>=0)
					5
				else
					d
				d = depthsAfter[mid+1]-depthsAfter[mid]
				pts += if(d>=0)
					5
				else
					d
			}

			if(heightBefore<heightAfter) {
				// Add points for reducing the height
				pts += if(depth==0&&!danger)
					(heightAfter-heightBefore)*10
				else
					(heightAfter-heightBefore)*20
			} else if(heightBefore>heightAfter)
			// Demerits for increase in height
				if(depth>0||danger) pts -= (heightBefore-heightAfter)*4

			//Penalty for prematurely filling in canyon
			if(!big&&!danger&&holeAfter>=holeBefore)
				for(i in 0 until width-1)
					if(depthsAfter[i]>depthsAfter[width-1]&&depthsBefore[i]<=depthsBefore[width-1]) {
						pts -= 1000000
						break
					}
			//Penalty for premature clears
			if(!big&&lines>0&&lines<4&&heightAfter>10&&xMax==width-1) {
				var minHi = 0
				for(i in 0 until width-1) {
					val hi = fld.getHighestBlockY(i)
					if(hi>minHi) minHi = hi
				}
				if(minHi>height-4) pts -= 300000
			}
			//Penalty for dangerous placements
			if(heightAfter<2*move)
				if(big) {
					if(heightAfter<0&&heightBefore>=0) return Integer.MIN_VALUE
					val spawnMinX = width/2-3
					val spawnMaxX = width/2+2
					var i = spawnMinX
					while(i<=spawnMaxX) {
						if(depthsAfter[i]<2*move&&depthsAfter[i]<depthsBefore[i])
							pts -= 2000000*(depthsBefore[i]-depthsAfter[i])
						i += move
					}
				} else {
					val spawnMinX = width/2-2
					val spawnMaxX = width/2+1
					for(i in spawnMinX..spawnMaxX)
						if(depthsAfter[i]<2&&depthsAfter[i]<depthsBefore[i]) pts -= 2000000*(depthsBefore[i]-depthsAfter[i])
					if(heightBefore>=2&&depth==0) pts -= 2000000*(heightBefore-heightAfter)
				}
			val r2ColDepth = depthsAfter[width-2]
			if(!big&&danger&&r2ColDepth<depthsAfter[width-1]) {
				//Bonus if edge clear is possible
				var maxLeftDepth = depthsAfter[0]
				for(i in 1 until width-2)
					maxLeftDepth = maxOf(maxLeftDepth, depthsAfter[i])
				if(r2ColDepth>maxLeftDepth) pts += 200
			}
		}
		if(DEBUG_ALL)
			log.debug("End of thinkMain("+x+", "+y+", "+rt+", "+rtOld+
				", fld, piece "+Shape.names[piece.id]+", "+depth+"). pts = "+pts)
		return pts
	}

	/**
	 * Returns the farthest x position the piece can move.
	 * @param x X coord
	 * @param y Y coord
	 * @param dir -1 to move left, 1 to move right.
	 * @param engine GameEngine
	 * @param fld Field
	 * @param piece Piece
	 * @param rt Desired final rotation direction.
	 * @return The farthest x position in the direction that the piece can be
	 * moved to.
	 */
	private fun mostMovableX(x:Int, y:Int, dir:Int, engine:GameEngine, fld:Field?, piece:Piece?, rt:Int):Int {
		if(dir==0) return x
		var shift = 1
		if(piece!!.big) shift = 2
		var testX = x
		var testY = y
		var testRt = Piece.DIRECTION_UP
		val speed = engine.speed
		if(speed.gravity>=0&&speed.gravity<speed.denominator) {
			if(DEBUG_ALL)
				log.debug("mostMovableX not applicable - low gravity (gravity = "+
					speed.gravity+", denominator = "+speed.denominator+")")
			if(dir<0)
				return piece.getMostMovableLeft(testX, testY, rt, fld!!)
			else if(dir>0) return piece.getMostMovableRight(testX, testY, rt, fld!!)
		}
		if(piece.id==Piece.PIECE_I&&dir>0) return piece.getMostMovableRight(testX, testY, rt, fld!!)
		var floorKickOK = false
		if((piece.id==Piece.PIECE_I||piece.id==Piece.PIECE_T)&&(engine.nowUpwardWallkickCount<engine.ruleopt.rotateMaxUpwardWallkick
				||engine.ruleopt.rotateMaxUpwardWallkick<0||
				engine.stat===GameEngine.Status.ARE))
			floorKickOK = true
		testY = piece.getBottom(testX, testY, testRt, fld!!)
		if(piece.id==Piece.PIECE_T&&piece.direction!=Piece.DIRECTION_UP) {
			val testY2 = piece.getBottom(testX, testY, Piece.DIRECTION_DOWN, fld)
			if(testY2>testY) {
				val kickRight = piece.checkCollision(testX+shift, testY2, testRt, fld)
				val kickLeft = piece.checkCollision(testX-shift, testY2, testRt, fld)
				if(kickRight) {
					testY = testY2
					if(rt==Piece.DIRECTION_UP) testX += shift
				} else if(kickLeft) {
					testY = testY2
					if(rt==Piece.DIRECTION_UP) testX -= shift
				} else if(floorKickOK)
					floorKickOK = false
				else
					return testX
			}
		}
		while(true) {
			if(!piece.checkCollision(testX+dir, testY, testRt, fld))
				testX += dir
			else if(testRt!=rt) {
				testRt = rt
				if(floorKickOK&&piece.checkCollision(testX, testY, testRt, fld)) {
					if(piece.id==Piece.PIECE_I) {
						testY -= if(piece.big)
							4
						else
							2
					} else
						testY--
					floorKickOK = false
				}
			} else {
				if(DEBUG_ALL)
					log.debug("mostMovableX("+x+", "+y+", "+dir+
						", piece "+Shape.names[piece.id]+", "+rt+") = "+testX)
				if(piece.id==Piece.PIECE_I&&testX<0&&rt and 1==1) {
					val height1 = fld.getHighestBlockY(1)
					if(height1<fld.getHighestBlockY(2)&&height1<fld.getHighestBlockY(3)+2)
						return 0
					else if(height1>fld.getHighestBlockY(0)) return -1
				}
				return testX
			}
			testY = piece.getBottom(testX, testY, testRt, fld)
		}
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

	/**
	 * Called to display internal state
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	override fun renderState(engine:GameEngine, playerID:Int) {
		engine.owner.receiver.run {
			drawScoreFont(engine, playerID, 19, 39, name.toUpperCase(), COLOR.GREEN, .5f)
			drawScoreFont(engine, playerID, 24, 40, "X", COLOR.BLUE, .5f)
			drawScoreFont(engine, playerID, 27, 40, "Y", COLOR.BLUE, .5f)
			drawScoreFont(engine, playerID, 30, 40, "RT", COLOR.BLUE, .5f)
			drawScoreFont(engine, playerID, 19, 41, "BEST:", COLOR.BLUE, .5f)
			drawScoreFont(engine, playerID, 24, 41, bestX.toString(), .5f)
			drawScoreFont(engine, playerID, 27, 41, bestY.toString(), .5f)
			drawScoreFont(engine, playerID, 30, 41, bestRt.toString(), .5f)
			drawScoreFont(engine, playerID, 19, 42, "SUB:", COLOR.BLUE, .5f)
			drawScoreFont(engine, playerID, 24, 42, bestXSub.toString(), .5f)
			drawScoreFont(engine, playerID, 27, 42, bestYSub.toString(), .5f)
			drawScoreFont(engine, playerID, 30, 42, bestRtSub.toString(), .5f)
			drawScoreFont(engine, playerID, 19, 43, "NOW:", COLOR.BLUE, .5f)

			if(engine.nowPieceObject==null)
				drawScoreFont(engine, playerID, 24, 43, "-- -- --", .5f)
			else {
				drawScoreFont(engine, playerID, 24, 43, engine.nowPieceX.toString(), .5f)
				drawScoreFont(engine, playerID, 27, 43, engine.nowPieceY.toString(), .5f)
				drawScoreFont(engine, playerID, 30, 43, engine.nowPieceObject!!.direction.toString(), .5f)
			}
			drawScoreFont(engine, playerID, 19, 44, "MOVE SCORE:", COLOR.BLUE, .5f)
			drawScoreFont(engine, playerID, 31, 44, bestPts.toString(), bestPts<=0, .5f)
			drawScoreFont(engine, playerID, 19, 45, "THINK ACTIVE:", COLOR.BLUE, .5f)
			drawScoreFont(engine, playerID, 32, 45, GeneralUtil.getOorX(thinking), .5f)
			drawScoreFont(engine, playerID, 19, 46, "IN ARE:", COLOR.BLUE, .5f)
			drawScoreFont(engine, playerID, 26, 46, GeneralUtil.getOorX(inARE), .5f)
		}

	}

	/* Processing of the thread */
	override fun run() {
		log.info("PoochyBot: Thread start")
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
					log.debug("PoochyBot: thinkBestPosition completed successfully")
				} catch(e:Throwable) {
					log.debug("PoochyBot: thinkBestPosition Failed", e)
				}

				thinking = false
			}

			if(thinkDelay>0)
				try {
					Thread.sleep(thinkDelay.toLong())
				} catch(e:InterruptedException) {
					log.debug("PoochyBot: InterruptedException trying to sleep")
				}

		}

		threadRunning = false
		log.info("PoochyBot: Thread end")
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
		internal val log = Logger.getLogger(PoochyBot::class.java)
		/** MaximumCompromise level */
		protected const val MAX_THINK_DEPTH = 2
		/** Set to true to print debug information */
		const val DEBUG_ALL = false

		//private static final int[][] HI_PENALTY = {{6, 2}, {7, 6}, {6, 2}, {1, 0}};
		fun checkOffset(p:Piece?, engine:GameEngine):Piece {
			val result = Piece(p!!)
			result.big = engine.big
			if(!p.offsetApplied)
				result.applyOffsetArray(engine.ruleopt.pieceOffsetX[p.id], engine.ruleopt.pieceOffsetY[p.id])
			return result
		}

		fun calcValleys(depths:IntArray, move:Int):IntArray {
			val result = intArrayOf(0, 0, 0)
			if(depths[0]>depths[move]) result[0] = (depths[0]-depths[move])/3/move
			if(move>=2&&depths[depths.size-1]>depths[depths.size-move-1])
				result[0] = (depths[depths.size-1]-depths[depths.size-move-1])/3/move
			var i = move
			while(i<depths.size-move) {
				val left = depths[i-move]
				val right = depths[i+move]
				val lowerSide = maxOf(left, right)
				val diff = depths[i]-lowerSide
				if(diff>=3) result[0] += diff/3/move
				if(left==right)
					if(left==depths[i]+2*move) {
						result[0]++
						result[1]--
						result[2]--
					} else if(left==depths[i]+move) {
						result[1]++
						result[2]++
					}
				if(diff/move%4==2)
					when {
						left>right -> result[1] += 2
						left<right -> result[2] += 2
						else -> {
							result[2]++
							result[1]++
						}
					}
				i += move
			}
			if((depths[0]-depths[move])/move%4==2) result[2] += 2
			if(move>=2&&(depths[depths.size-1]-depths[depths.size-move-1])/move%4==2) result[1] += 2
			/* if ((depthsBefore[width-2] - depthsBefore[width-3])%4 == 2 &&
		 * (depthsBefore[width-1] - depthsBefore[width-2]) < 2)
		 * valleysBefore[1]++;
		 * valleysBefore[2] >>= 1;
		 * valleysBefore[1] >>= 1; */
			return result
		}

		/**
		 * @param fld Field
		 * @param x X coord
		 * @return Y coord of highest block
		 */
		@Deprecated("Workaround for the bug in Field.getHighestBlockY(int).\n"+
			"\t  The bug has since been fixed as of NullpoMino v6.5, so\n"+
			"\t  fld.getHighestBlockY(x) should be equivalent.")
		fun getColumnDepth(fld:Field, x:Int):Int {
			val maxY = fld.height-1
			var result = fld.getHighestBlockY(x)
			if(result==maxY&&fld.getBlockEmpty(x, maxY)) result++
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
