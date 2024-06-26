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
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.ai.DummyAI
import mu.nu.nullpo.util.GeneralUtil.getOX
import org.apache.logging.log4j.LogManager
import kotlin.math.abs

/**
PoochyBot AI
@author Poochy.EXE
Poochy.Spambucket@gmail.com
 */
open class PoochyBot:DummyAI(), Runnable {
	/** When true,To threadThink routineInstructing the execution of the */
	private var thinkRequest:ThinkRequestMutex = ThinkRequestMutex()

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
	/** Wait extra frames at low speeds? */
	//protected static final boolean DELAY_DROP_ON = false;
	/** # of extra frames to wait */
	//protected static final int DROP_DELAY = 2;
	/** Number of frames waited */
	//protected int dropDelay;
	/** Did the thinking thread find a possible position? */
	private var thinkSuccess = false
	/** Was the game in ARE as of the last frame? */
	private var inARE = false

	/* AI's name */
	override val name = "PoochyBot V1.25"

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

		if(thread?.isAlive!=true&&engine.aiUseThread) {
			thread = Thread(this, "AI_${engine.playerID}").apply {
				isDaemon = true
				start()
			}
			thinkDelay = engine.aiThinkDelay
			thinkCurrentPieceNo = 0
			thinkLastPieceNo = 0
		}
	}

	/* Called whenever a new piece is spawned */
	override fun newPiece(engine:GameEngine, playerID:Int) {
		if(!engine.aiUseThread) thinkBestPosition(engine)
		else if(!thinking&&!thinkComplete||!engine.aiPreThink||engine.aiShowHint||engine.speed.are<=0||engine.speed.areLine<=0) {
			thinkComplete = false
			//thinkCurrentPieceNo++;
			thinkRequest.newRequest()
		}
	}

	/* Called at the start of each frame */
	override fun onFirst(engine:GameEngine, playerID:Int) {
		inputARE = 0
		val newInARE = engine.stat===GameEngine.Status.ARE
		if(engine.aiPreThink&&engine.speed.are>0&&engine.speed.areLine>0&&(newInARE&&!inARE||!thinking&&!thinkSuccess)) {
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
				nextPiece = if(engine.holdPieceObject==null) engine.getNextObject(engine.nextPieceCount+1)
				else engine.holdPieceObject
			}
			if(nextPiece==null) return
			nextPiece = checkOffset(nextPiece, engine)
			input = input or calcIRS(nextPiece, engine)
			if(threadRunning&&!thinking&&thinkComplete) {
				val spawnX = engine.getSpawnPosX(nextPiece, engine.field)
				if(bestX-spawnX>1)
				// left
				//setDAS = -1;
					input = input or Controller.BUTTON_BIT_LEFT
				else if(spawnX-bestX>1)
				// right
				//setDAS = 1;
					input = input or Controller.BUTTON_BIT_RIGHT
				else setDAS = 0
				delay = 0
			}
			if(DEBUG_ALL) log.debug(
				"Currently in ARE. Next piece type = ${nextPiece.type.name}, IRS = $input"
			)
			//engine.ctrl.setButtonBit(input);
			inputARE = input
		}
	}

	/* Set button input states */
	override fun setControl(engine:GameEngine, playerID:Int, ctrl:Controller):Int {
		if(engine.nowPieceObject!=null&&engine.stat===GameEngine.Status.MOVE&&delay>=engine.aiMoveDelay&&engine.statc[0]>0&&(!engine.aiUseThread||threadRunning&&!thinking&&thinkComplete)) {
			inputARE = 0
			var input = 0 // Button input data
			val pieceNow = checkOffset(engine.nowPieceObject, engine)
			val nowX = engine.nowPieceX
			val nowY = engine.nowPieceY
			val rt = pieceNow.direction
			val fld = engine.field
			val pieceTouchGround = pieceNow.checkCollision(nowX, nowY+1, fld)
			val nowType = pieceNow.type
			val width = fld.width

			var moveDir = 0 //-1 = left,  1 = right
			var spinDir = 0 //-1 = left,  1 = right
			var drop = 0 //1 = up, -1 = down
			var sync = false //true = delay either spin or movement for synchro move if needed.

			//SpeedParam speed = engine.speed;
			//boolean lowSpeed = speed.gravity < speed.denominator;
			val canFloorKick =
				engine.nowWallkickRiseCount<engine.ruleOpt.spinWallkickMaxRise||engine.ruleOpt.spinWallkickMaxRise<0

			//If stuck, rethink.
			/* if ((nowX < bestX && pieceNow.checkCollision(nowX+1, nowY, rt,
			 * fld)) ||
			 * (nowX > bestX && pieceNow.checkCollision(nowX-1, nowY, rt, fld)))
			 * {
			 * thinkRequest = true;
			 * if (DEBUG_ALL) log.debug("Needs rethink - piece is stuck!");
			 * } */
			/* if (rt == Piece.DIRECTION_DOWN &&
			 * ((nowType == Piece.Shape.L && bestX > nowX) || (nowType ==
			 * Piece.Shape.J && bestX < nowX)))
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
			if(rt==Piece.DIRECTION_DOWN&&(nowType==Piece.Shape.L&&bestX>nowX||nowType==Piece.Shape.J&&bestX<nowX)&&
				!fld.getBlockEmpty(pieceNow.maximumBlockX+nowX-1, pieceNow.maximumBlockY+nowY)
			) {
				thinkComplete = false
				if(DEBUG_ALL) log.debug("Needs rethink - L or J piece is stuck!")
				thinkRequest.newRequest()
			}
			if(nowType==Piece.Shape.O&&(bestX<nowX&&pieceNow.checkCollision(nowX-1, nowY, rt, fld)||
					bestX<nowX&&pieceNow.checkCollision(nowX-1, nowY, rt, fld))
			) {
				thinkComplete = false
				if(DEBUG_ALL) log.debug("Needs rethink - O piece is stuck!")
				thinkRequest.newRequest()
			}
			if(pieceTouchGround&&rt==bestRt&&
				(pieceNow.getMostMovableRight(nowX, nowY, rt, fld)<bestX||pieceNow.getMostMovableLeft(nowX, nowY, rt, fld)>bestX)
			)
				stuckDelay++ else stuckDelay = 0
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
			if(engine.nowPieceSpinCount>=8) {
				thinkComplete = false
				if(DEBUG_ALL) log.debug("Needs rethink - piece is stuck, too many rotations!")
				thinkRequest.newRequest()
			} else sameStatusTime = 0
			if(bestHold&&thinkComplete&&engine.isHoldOK) {
				// Hold
				input = Controller.BUTTON_BIT_D

				val holdPiece = engine.holdPieceObject
				if(holdPiece!=null) input = calcIRS(holdPiece, engine)
			} else {
				if(DEBUG_ALL) log.debug(
					"bestX = $bestX, nowX = $nowX, bestY = $bestY, nowY = $nowY, bestRt = $bestRt, rt = $rt, bestXSub = $bestXSub, bestYSub = $bestYSub, bestRtSub = $bestRtSub"
				)
				printPieceAndDirection(nowType, rt)
				//Spin if near destination or stuck
				var xDiff = abs(nowX-bestX)
				if(bestX<nowX&&nowType==Piece.Shape.I&&rt==Piece.DIRECTION_DOWN&&bestRt!=rt) xDiff--
				val best180 = abs(rt-bestRt)==2
				//Special movements for I-piece
				if(nowType==Piece.Shape.I) {
					var hypRtDir = 1
					var spinI = false
					if((rt+3)%4==bestRt) hypRtDir = -1
					if(nowX<bestX) {
						moveDir = 1
						if(pieceNow.checkCollision(nowX+1, nowY, fld))
							if(rt and 1==0&&(canFloorKick||!pieceNow.checkCollision(nowX, nowY, (rt+1)%4, fld))) spinI = true
							else if(rt and 1>0&&canFloorKick) spinI = true
							else if(engine.isHoldOK&&!ctrl.isPress(Controller.BUTTON_D)) {
								if(DEBUG_ALL) log.debug("[<-]Stuck I piece - use hold")
								input = Controller.BUTTON_BIT_D

								val holdPiece = engine.holdPieceObject
								if(holdPiece!=null) input = input or calcIRS(holdPiece, engine)
							}
					} else if(nowX>bestX) {
						moveDir = -1
						if(pieceNow.checkCollision(nowX-1, nowY, fld))
							if(rt and 1==0&&(canFloorKick||!pieceNow.checkCollision(nowX, nowY, (rt+1)%4, fld))) spinI = true
							else if(rt and 1>0&&!pieceNow.checkCollision(nowX-1, nowY, (rt+1)%4, fld)&&canFloorKick) spinI = true
							else if(engine.isHoldOK&&!ctrl.isPress(Controller.BUTTON_D)) {
								if(DEBUG_ALL) log.debug("[->]Stuck I piece - use hold")
								input = Controller.BUTTON_BIT_D

								val holdPiece = engine.holdPieceObject
								if(holdPiece!=null) input = input or calcIRS(holdPiece, engine)
							}
					} else if(rt!=bestRt) if(best180) bestRt = (bestRt+2)%4
					else spinI = true
					if(spinI) spinDir = hypRtDir
				} else if(rt!=bestRt&&(xDiff<=1||bestX==0&&nowX==2&&nowType==Piece.Shape.I||
						(nowX<bestX&&pieceNow.checkCollision(nowX+1, nowY, rt, fld)||
							nowX>bestX&&pieceNow.checkCollision(nowX-1, nowY, rt, fld))&&
						!(pieceNow.maximumBlockX+nowX==width-2&&rt and 1>0)&&
						!(pieceNow.minimumBlockY+nowY==2&&pieceTouchGround&&rt and 1==0&&nowType!=Piece.Shape.I))
				) {
					//if (DEBUG_ALL) log.debug("Case 1 rotation");

					val spL = engine.getSpinDirection(-1)
					val spR = engine.getSpinDirection(1)
					if(DEBUG_ALL) log.debug("spL = $spL, spR = $spR")

					if(best180&&engine.ruleOpt.spinDoubleKey&&!ctrl.isPress(Controller.BUTTON_E))
						input = Controller.BUTTON_BIT_E
					else if(bestRt==spR) spinDir = 1
					else if(bestRt==spL) spinDir = -1
					else if(engine.ruleOpt.spinReverseKey&&best180&&rt and 1>0) {
						spinDir = if(spR==Piece.DIRECTION_UP) 1
						else -1
					} else spinDir = 1
				} else if(rt!=Piece.DIRECTION_UP&&xDiff>1&&engine.ruleOpt.spinReverseKey
					&&(nowType==Piece.Shape.L||nowType==Piece.Shape.J||nowType==Piece.Shape.T)
				) when(rt) {
					Piece.DIRECTION_DOWN -> {
						when {
							engine.ruleOpt.spinDoubleKey&&!ctrl.isPress(Controller.BUTTON_E) -> input = Controller.BUTTON_BIT_E
							nowType==Piece.Shape.L -> spinDir = -1
							nowType==Piece.Shape.J -> spinDir = 1
							nowType==Piece.Shape.T -> if(nowX>bestX) spinDir = -1
							else if(nowX<bestX) spinDir = 1
						}
					}
					Piece.DIRECTION_RIGHT -> spinDir = -1
					Piece.DIRECTION_LEFT -> spinDir = 1
				}//Try to keep flat side down on L, J, or T piece.

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
					if(nowX==bestX&&pieceTouchGround) if(rt==bestRt) {
						// Ground rotation
						if(bestRtSub!=-1) {
							bestRt = bestRtSub
							bestRtSub = -1
						}
						// Shift move
						if(bestX!=bestXSub) {
							bestX = bestXSub
							bestY = bestYSub
						}
					} else if(nowType==Piece.Shape.I&&rt and 1>0&&nowX+pieceNow.maximumBlockX==width-2&&(fld.highestBlockY<=4||fld.getHighestBlockY(
							width-2
						)-fld.getHighestBlockY(
							width-1
						)>=4)
					) {
						bestRt = rt
						bestX += 1
					}
					/* //Move left if they need to move left, or if at rightmost position and can move left.
					 * if (pieceTouchGround && pieceNow.id != Piece.Shape.I &&
					 * nowX+pieceNow.getMaximumBlockX() == width-1 &&
					 * !pieceNow.checkCollision(nowX-1, nowY, fld))
					 * {
					 * if(!ctrl.isPress(Controller.BUTTON_LEFT) &&
					 * (engine.aiMoveDelay >= 0))
					 * input |= Controller.BUTTON_BIT_LEFT;
					 * bestX = nowX - 1;
					 * } */
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
						} else if(engine.ruleOpt.harddropEnable&&!engine.ruleOpt.harddropLock) drop = 1
						else if(engine.ruleOpt.softdropEnable&&!engine.ruleOpt.softdropLock) drop = -1
					}
				}
			}

			val minBlockX = nowX+pieceNow.minimumBlockX
			val maxBlockX = nowX+pieceNow.maximumBlockX
			val minBlockXDepth = fld.getHighestBlockY(minBlockX)
			val maxBlockXDepth = fld.getHighestBlockY(maxBlockX)
			if(nowType==Piece.Shape.L&&minBlockXDepth<maxBlockXDepth&&pieceTouchGround&&rt==Piece.DIRECTION_DOWN&&spinDir==-1&&maxBlockX<width-1) {
				if(bestX==nowX+1) moveDir = 1
				else if(bestX<nowX) {
					if(DEBUG_ALL) log.debug("Delaying rotation on L piece to avoid getting stuck. (Case 1)")
					sync = false
					spinDir = 0
					moveDir = 1
				} else if(bestX>nowX) {
					/* if (minBlockXDepth == fld.getHighestBlockY(minBlockX-1))
					  {
					  if (DEBUG_ALL) log.
					  debug("Delaying rotation on L piece to avoid getting stuck. (Case 2)"
					  );
					  sync = false;
					  rotateDir = 0;
					  moveDir = -1;
					  }
					  else */
					if(DEBUG_ALL) log.debug("Attempting synchro move on L piece to avoid getting stuck.")
					sync = true
					spinDir = -1
					moveDir = -1
				}
			} else if(nowType==Piece.Shape.J&&minBlockXDepth>maxBlockXDepth&&pieceTouchGround&&rt==Piece.DIRECTION_DOWN&&spinDir==1&&minBlockX>0) {
				if(bestX==nowX-1) moveDir = -1
				else if(bestX>nowX) {
					if(DEBUG_ALL) log.debug("Delaying rotation on J piece to avoid getting stuck. (Case 1)")
					sync = false
					spinDir = 0
					moveDir = -1
				} else if(bestX<nowX) {
					/* if (maxBlockXDepth == fld.getHighestBlockY(maxBlockX+1))
					  {
					  if (DEBUG_ALL) log.
					  debug("Delaying rotation on J piece to avoid getting stuck. (Case 2)"
					  );
					  sync = false;
					  rotateDir = 0;
					  moveDir = 1;
					  }
					  else */
					if(DEBUG_ALL) log.debug("Attempting synchro move on J piece to avoid getting stuck.")
					sync = true
					spinDir = 1
					moveDir = 1
				}
			} else if(spinDir!=0&&moveDir!=0&&pieceTouchGround&&rt and 1>0&&(nowType==Piece.Shape.J||nowType==Piece.Shape.L)
				&&!pieceNow.checkCollision(nowX+moveDir, nowY+1, rt, fld)
			) {
				if(DEBUG_ALL) log.debug("Delaying move on L or J piece to avoid getting stuck.")
				sync = false
				moveDir = 0
			}
			if(engine.nowPieceSpinCount>=5&&spinDir!=0&&moveDir!=0&&!sync) {
				if(DEBUG_ALL) log.debug("Piece seems to be stuck due to unintentional synchro - trying intentional desync.")
				moveDir = 0
			}
			if(moveDir==-1&&minBlockX==1&&nowType==Piece.Shape.I&&rt and 1>0&&pieceNow.checkCollision(nowX-1, nowY, rt, fld)) {
				val depthNow = fld.getHighestBlockY(minBlockX)
				val depthLeft = fld.getHighestBlockY(minBlockX-1)
				if(depthNow>depthLeft&&depthNow-depthLeft<2) if(!pieceNow.checkCollision(nowX+1, nowY, rt, fld)) moveDir = 1
				else if(engine.isHoldOK&&!ctrl.isPress(Controller.BUTTON_D)) input = input or Controller.BUTTON_BIT_D
			}
			/* //Catch bug where it fails to spin J piece
			 * if (moveDir == 0 && rotateDir == 0 & drop == 0)
			 * {
			 * if ((rt+1)%4 == bestRt)
			 * rotateDir = 1;
			 * else if ((rt+3)%4 == bestRt)
			 * rotateDir = -1;
			 * else if ((rt+2)%4 == bestRt)
			 * {
			 * if(engine.ruleOpt.spinButtonAllowDouble)
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
			if(moveDir==-1&&(!ctrl.isPress(Controller.BUTTON_LEFT)||useDAS)) input = input or Controller.BUTTON_BIT_LEFT
			else if(moveDir==1&&(!ctrl.isPress(Controller.BUTTON_RIGHT)||useDAS)) input = input or Controller.BUTTON_BIT_RIGHT
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
			if(drop==1&&!ctrl.isPress(Controller.BUTTON_UP)) input = input or Controller.BUTTON_BIT_UP
			else if(drop==-1) input = input or Controller.BUTTON_BIT_DOWN

			if(spinDir!=0) {
				val spinRight = engine.owSpinDir==1||engine.owSpinDir==-1&&engine.ruleOpt.spinToRight

				if(engine.ruleOpt.spinDoubleKey&&spinDir==2&&!ctrl.isPress(Controller.BUTTON_E))
					input = input or Controller.BUTTON_BIT_E
				else if(engine.ruleOpt.spinReverseKey&&!spinRight&&spinDir==1) {
					if(!ctrl.isPress(Controller.BUTTON_B)) input = input or Controller.BUTTON_BIT_B
				} else if(engine.ruleOpt.spinReverseKey&&spinRight&&spinDir==-1) {
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
				log.debug("Input = $input, moveDir = $moveDir, spinDir = $spinDir, sync = $sync, drop = $drop, setDAS = $setDAS")

			delay = 0
			return input
		}
		//dropDelay = 0;
		delay++
		return inputARE
	}

	private fun printPieceAndDirection(pieceType:Piece.Shape, rt:Int) {
		var result = "Piece ${pieceType.name}, direction "

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
		val nextType = p.type
		val fld = engine.field
		val spawnX = engine.getSpawnPosX(p, fld)
		val speed = engine.speed
		val gravityHigh = speed.gravity>speed.denominator
		val width = fld.width
		val midColumnX = width/2-1
		return when {
			abs(spawnX-bestX)==1 ->
				if(bestRt==1) if(engine.ruleOpt.spinToRight) Controller.BUTTON_BIT_A else Controller.BUTTON_BIT_B
				else if(bestRt==3) if(engine.ruleOpt.spinToRight) Controller.BUTTON_BIT_B else Controller.BUTTON_BIT_A
				else 0

			nextType==Piece.Shape.L ->
				if(gravityHigh&&fld.getHighestBlockY(midColumnX-1)<
					minOf(fld.getHighestBlockY(midColumnX), fld.getHighestBlockY(midColumnX+1))) 0
				else if(engine.ruleOpt.spinToRight) Controller.BUTTON_BIT_B else Controller.BUTTON_BIT_A
			nextType==Piece.Shape.J -> {
				if(gravityHigh&&fld.getHighestBlockY(midColumnX+1)<
					minOf(fld.getHighestBlockY(midColumnX), fld.getHighestBlockY(midColumnX-1))) 0
				else if(engine.ruleOpt.spinToRight) Controller.BUTTON_BIT_A else Controller.BUTTON_BIT_B
			}
			/* else if (nextType == Piece.Shape.I)
			 * return Controller.BUTTON_BIT_A; */
			else -> 0
		}
		/* else if (nextType == Piece.Shape.I)
		 * return Controller.BUTTON_BIT_A; */
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

		val fld:Field = if(engine.stat===GameEngine.Status.READY) Field(
			engine.fieldWidth,
			engine.fieldHeight,
			engine.fieldHiddenHeight,
			engine.ruleOpt.fieldCeiling
		)
		else Field(engine.field)
		var pieceNow = if(inARE||engine.nowPieceObject==null) engine.getNextObjectCopy(engine.nextPieceCount)
		else engine.nowPieceObject!!
		var pieceHold = engine.holdPieceObject
		/* Piece pieceNow = null;
		 * if (engine.nowPieceObject != null)
		 * pieceNow = new Piece(engine.nowPieceObject);
		 * Piece pieceHold = null;
		 * if (engine.holdPieceObject != null)
		 * pieceHold = new Piece(engine.holdPieceObject); */
		val nowX = if(inARE) engine.getSpawnPosX(pieceNow, fld) else engine.nowPieceX
		val nowY = if(inARE) engine.getSpawnPosY(pieceNow) else engine.nowPieceY
		val nowRt = if(inARE) engine.ruleOpt.pieceDefaultDirection[pieceNow?.id ?: 0] else pieceNow?.direction ?: 0
		if(pieceHold==null) pieceHold = (if(inARE||pieceNow==null) engine.getNextObjectCopy(engine.nextPieceCount+1)
		else engine.getNextObjectCopy(engine.nextPieceCount))
		pieceNow = checkOffset(pieceNow, engine)
		pieceHold = checkOffset(pieceHold, engine)
		if(pieceHold.type==pieceNow.type) pieceHold = null
		/* if (!pieceNow.offsetApplied)
		 * pieceNow.applyOffsetArray(engine.ruleOpt.pieceOffsetX[pieceNow.id],
		 * engine.ruleOpt.pieceOffsetY[pieceNow.id]);
		 * if (!pieceHold.offsetApplied)
		 * pieceHold.applyOffsetArray(engine.ruleOpt.pieceOffsetX[pieceHold.id],
		 * engine.ruleOpt.pieceOffsetY[pieceHold.id]); */
		val holdOK = engine.isHoldOK

		val canFloorKick =
			engine.nowWallkickRiseCount<engine.ruleOpt.spinWallkickMaxRise||engine.ruleOpt.spinWallkickMaxRise<0
		val canFloorKickI = pieceNow.type==Piece.Shape.I&&nowRt and 1==0&&canFloorKick
		var canFloorKickT = pieceNow.type==Piece.Shape.T&&nowRt!=Piece.DIRECTION_UP&&canFloorKick
		if(canFloorKickT&&!pieceNow.checkCollision(nowX, nowY, Piece.DIRECTION_UP, fld)) canFloorKickT = false
		else if(canFloorKickT&&!pieceNow.checkCollision(nowX-1, nowY, Piece.DIRECTION_UP, fld)) canFloorKickT = false
		else if(canFloorKickT&&!pieceNow.checkCollision(nowX+1, nowY, Piece.DIRECTION_UP, fld)) canFloorKickT = false

		var move = 1
		if(engine.big) move = 2

		for(depth in 0..<MAX_THINK_DEPTH) {
			/* int dirCount = Piece.DIRECTION_COUNT;
			 * if (pieceNow.id == Piece.Shape.I || pieceNow.id == Piece.Shape.S
			 * || pieceNow.id == Piece.Shape.Z)
			 * dirCount = 2;
			 * else if (pieceNow.id == Piece.Shape.O)
			 * dirCount = 1; */
			for(rt in 0..<Piece.DIRECTION_COUNT) {
				var tempY = nowY
				if(canFloorKickI&&rt and 1>0) tempY -= 2
				else if(canFloorKickT&&rt==Piece.DIRECTION_UP) tempY--

				val minX = maxOf(
					mostMovableX(nowX, tempY, -1, engine, fld, pieceNow, rt),
					pieceNow.getMostMovableLeft(nowX, tempY, rt, engine.field)
				)
				val maxX = minOf(
					mostMovableX(nowX, tempY, 1, engine, fld, pieceNow, rt),
					pieceNow.getMostMovableRight(nowX, tempY, rt, engine.field)
				)
				var spawnOK = true
				if(engine.stat===GameEngine.Status.ARE) {
					val spawnX = engine.getSpawnPosX(pieceNow, fld)
					val spawnY = engine.getSpawnPosY(pieceNow)
					spawnOK = !pieceNow.checkCollision(spawnX, spawnY, fld)
				}
				run {
					var x = minX
					while(x<=maxX&&spawnOK) {
						fld.replace(engine.field)
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
							//if((depth > 0) || (bestPts <= 10) || (pieceNow.id == Piece.Shape.T)) {
							// Left shift
							fld.replace(engine.field)
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
							fld.replace(engine.field)
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
							if(!engine.ruleOpt.spinToRight||engine.ruleOpt.spinReverseKey) {
								val rot = pieceNow.getSpinDirection(-1, rt)
								var newX = x
								var newY = y
								fld.replace(engine.field)
								pts = Integer.MIN_VALUE

								if(!pieceNow.checkCollision(x, y, rot, fld)) pts = thinkMain(x, y, rot, rt, fld, pieceNow, depth)
								else if(engine.wallkick!=null&&engine.ruleOpt.spinWallkick) {
									val allowUpward =
										engine.ruleOpt.spinWallkickMaxRise<0||engine.nowWallkickRiseCount<engine.ruleOpt.spinWallkickMaxRise
									engine.wallkick!!.executeWallkick(
										x,
										y,
										-1,
										rt,
										rot,
										allowUpward,
										pieceNow,
										fld,
										null
									)?.let {(offsetX, offsetY) ->
										newX = x+offsetX
										newY = y+offsetY
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
							if(engine.ruleOpt.spinToRight||engine.ruleOpt.spinReverseKey) {
								val rot = pieceNow.getSpinDirection(1, rt)
								var newX = x
								var newY = y
								fld.replace(engine.field)
								pts = Integer.MIN_VALUE

								if(!pieceNow.checkCollision(x, y, rot, fld)) pts = thinkMain(x, y, rot, rt, fld, pieceNow, depth)
								else if(engine.wallkick!=null&&engine.ruleOpt.spinWallkick) {
									val allowUpward =
										engine.ruleOpt.spinWallkickMaxRise<0||engine.nowWallkickRiseCount<engine.ruleOpt.spinWallkickMaxRise
									engine.wallkick!!.executeWallkick(
										x,
										y,
										1,
										rt,
										rot,
										allowUpward,
										pieceNow,
										fld,
										null
									)?.let {(offsetX, offsetY) ->
										newX = x+offsetX
										newY = y+offsetY
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
							if(engine.ruleOpt.spinDoubleKey) {
								val rot = pieceNow.getSpinDirection(2, rt)
								var newX = x
								var newY = y
								fld.replace(engine.field)
								pts = Integer.MIN_VALUE

								if(!pieceNow.checkCollision(x, y, rot, fld)) pts = thinkMain(x, y, rot, rt, fld, pieceNow, depth)
								else if(engine.wallkick!=null&&engine.ruleOpt.spinWallkick) {
									val allowUpward =
										engine.ruleOpt.spinWallkickMaxRise<0||engine.nowWallkickRiseCount<engine.ruleOpt.spinWallkickMaxRise
									engine.wallkick!!.executeWallkick(
										x,
										y,
										2,
										rt,
										rot,
										allowUpward,
										pieceNow,
										fld,
										null
									)?.let {(offsetX, offsetY) ->
										newX = x+offsetX
										newY = y+offsetY
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
					val spawnX = engine.getSpawnPosX(pieceHold, engine.field)
					val spawnY = engine.getSpawnPosY(pieceHold)
					val minHoldX = maxOf(
						mostMovableX(spawnX, spawnY, -1, engine, engine.field, pieceHold, rt),
						pieceHold.getMostMovableLeft(spawnX, spawnY, rt, engine.field)
					)
					val maxHoldX = minOf(
						mostMovableX(spawnX, spawnY, 1, engine, engine.field, pieceHold, rt),
						pieceHold.getMostMovableRight(spawnX, spawnY, rt, engine.field)
					)

					//Bonus for holding an I-piece, penalty for holding an S or Z.
					val holdPts = when(pieceHold.type) {
						Piece.Shape.I -> -30
						Piece.Shape.S, Piece.Shape.Z -> 30
						Piece.Shape.O -> 10
						else -> 0
					}+when(pieceNow.type) {
						Piece.Shape.I -> 30
						Piece.Shape.S, Piece.Shape.Z -> -30
						Piece.Shape.O -> -10
						else -> 0
					}

					var x = minHoldX
					while(x<=maxHoldX) {
						fld.replace(engine.field)
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
							//if((depth > 0) || (bestPts <= 10) || (pieceHold.id == Piece.Shape.T)) {
							// Left shift
							fld.replace(engine.field)
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
							fld.replace(engine.field)
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
							if(!engine.ruleOpt.spinToRight||engine.ruleOpt.spinReverseKey) {
								val rot = pieceHold.getSpinDirection(-1, rt)
								var newX = x
								var newY = y
								fld.replace(engine.field)
								pts = Integer.MIN_VALUE

								if(!pieceHold.checkCollision(x, y, rot, fld)) pts = thinkMain(x, y, rot, rt, fld, pieceHold, depth)
								else if(engine.wallkick!=null&&engine.ruleOpt.spinWallkick) {
									val allowUpward =
										engine.ruleOpt.spinWallkickMaxRise<0||engine.nowWallkickRiseCount<engine.ruleOpt.spinWallkickMaxRise
									engine.wallkick!!.executeWallkick(
										x,
										y,
										-1,
										rt,
										rot,
										allowUpward,
										pieceHold,
										fld,
										null
									)?.let {(offsetX, offsetY) ->
										newX = x+offsetX
										newY = y+offsetY
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
							if(engine.ruleOpt.spinToRight||engine.ruleOpt.spinReverseKey) {
								val rot = pieceHold.getSpinDirection(1, rt)
								var newX = x
								var newY = y
								fld.replace(engine.field)
								pts = Integer.MIN_VALUE

								if(!pieceHold.checkCollision(x, y, rot, fld)) pts = thinkMain(x, y, rot, rt, fld, pieceHold, depth)
								else if(engine.wallkick!=null&&engine.ruleOpt.spinWallkick) {
									val allowUpward =
										engine.ruleOpt.spinWallkickMaxRise<0||engine.nowWallkickRiseCount<engine.ruleOpt.spinWallkickMaxRise
									engine.wallkick!!.executeWallkick(
										x,
										y,
										1,
										rt,
										rot,
										allowUpward,
										pieceHold,
										fld,
										null
									)?.let {(offsetX, offsetY) ->
										newX = x+offsetX
										newY = y+offsetY
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
							if(engine.ruleOpt.spinDoubleKey) {
								val rot = pieceHold.getSpinDirection(2, rt)
								var newX = x
								var newY = y
								fld.replace(engine.field)
								pts = Integer.MIN_VALUE

								if(!pieceHold.checkCollision(x, y, rot, fld)) pts = thinkMain(x, y, rot, rt, fld, pieceHold, depth)
								else if(engine.wallkick!=null&&engine.ruleOpt.spinWallkick) {
									val allowUpward =
										engine.ruleOpt.spinWallkickMaxRise<0||engine.nowWallkickRiseCount<engine.ruleOpt.spinWallkickMaxRise
									engine.wallkick!!.executeWallkick(
										x,
										y,
										2,
										rt,
										rot,
										allowUpward,
										pieceHold,
										fld,
										null
									)?.let {(offsetX, offsetY) ->
										newX = x+offsetX
										newY = y+offsetY
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

			if(bestPts>0) break
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

		// Number of holes and valleys needing an I-piece (before placement)
		val holeBefore = fld.howManyHoles
		//int lidBefore = fld.getHowManyLidAboveHoles();

		//Check number of holes in rightmost column
		var testY = fld.hiddenHeight
		var holeBeforeRCol = 0
		if(!big) {
			while(fld.getBlockEmpty(width-1, testY)&&testY<height) testY++
			while(!fld.getBlockEmpty(width-1, testY)&&testY<height) testY++
			while(testY<height) {
				if(fld.getBlockEmpty(width-1, testY)) holeBeforeRCol++
				testY++
			}
		}
		//Fetch depths and find valleys that require an [I], [J], or [L].
		val depthsBefore = getColumnDepths(fld)
		var deepestY = -1
		//int deepestX = -1;
		for(i in 0..<width-1) if(depthsBefore[i]>deepestY) deepestY = depthsBefore[i]
		//deepestX = i;
		val valleysBefore = calcValleys(depthsBefore, move)

		// Field height (before placement)
		val heightBefore = fld.highestBlockY
		// Twister flag
		var twist = false
		if(piece.type==Piece.Shape.T&&rtOld!=-1&&fld.isTwistSpot(x, y, piece.big)) twist = true

		//Does move fill in valley with an I-piece?
		var valley = 0
		if(piece.type==Piece.Shape.I) if(xMin==xMax&&0<=xMin&&xMin<width) {
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
			if(DEBUG_ALL) log.debug(
				"End of thinkMain($x, $y, $rt, $rtOld, fld, piece ${piece.type.name}, $depth). pts = 0 (Cannot place piece)"
			)
			return Integer.MIN_VALUE
		}

		// Line clear
		val lines = fld.checkLine()/move
		if(lines>0) {
			fld.clearLine()
			fld.downFloatingBlocks()
		}

		// All clear
		val allClear = fld.isEmpty
		if(allClear) pts += 500000

		// Field height (after clears)
		val heightAfter = fld.highestBlockY

		val depthsAfter = getColumnDepths(fld)

		// Danger flag
		val danger = heightBefore<=4*(move+1)
		//Flag for really dangerously high stacks
		val peril = heightBefore<=2*(move+1)

		// Additional points for lower placements
		pts += if(!danger&&depth==0) y*10
		else y*20

		val holeAfter = fld.howManyHoles

		val rColPenalty = 1000
		/* if (danger)
		 * rColPenalty = 100; */
		//Apply score penalty if I-piece would overflow canyon,
		//unless it would also uncover a hole.
		if(!big&&piece.type==Piece.Shape.I&&holeBefore<=holeAfter&&xMax==width-1) {
			val rValleyDepth = depthsAfter[width-1-move]-depthsAfter[width-1]
			if(rValleyDepth>0) pts -= (rValleyDepth+1)*rColPenalty
		}
		//Bonus points for filling in valley with an I-piece
		var valleyBonus = 0
		if(valley==3&&xMax<width-1) valleyBonus = 40000
		else if(valley>=4) valleyBonus = 400000
		if(xMax==0) valleyBonus *= 2
		if(valley>0&&DEBUG_ALL) log.debug("I piece xMax = $xMax, valley depth = $valley, valley bonus = $valleyBonus")
		pts += valleyBonus
		if(lines==1&&!danger&&depth==0&&heightAfter>=16&&holeBefore<3&&!twist&&xMax==width-1) {
			if(DEBUG_ALL) log.debug(
				"End of thinkMain($x, $y, $rt, "+rtOld+", fld, piece ${piece.type.name}, 0). pts = 0 (Special Condition 3)"
			)
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

		if(lines<4&&!allClear) {
			// Number of holes and valleys needing an I-piece (after placement)
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
				pts += if(!danger) (holeBefore-holeAfter)*200
				else (holeBefore-holeAfter)*400
			}

			/* if(lidAfter < lidBefore) {
			 * // Add points for reduction in number blocks above holes
			 * pts += (lidAfter - lidBefore) * 500;
			 * } */

			if(twist&&lines>=1)
			// Twister Bonus - retained from Basic AI, but should never actually trigger
				pts += 100000*lines

			testY = fld.hiddenHeight
			var holeAfterRCol = 0
			if(!big) {
				//Check number of holes in rightmost column
				while(fld.getBlockEmpty(width-1, testY)&&testY<height) testY++
				while(!fld.getBlockEmpty(width-1, testY)&&testY<height) testY++
				while(testY<height) {
					if(fld.getBlockEmpty(width-1, testY)) holeAfterRCol++
					testY++
				}
				//Apply score penalty if non-I-piece would plug up canyon
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
			} else needLOrJValleyDiffScore += valleysBefore[1] and 3
			if(valleysAfter[1]>3) {
				needLJValleyDiffScore -= 1 shl (valleysAfter[1] shr 1)
				needLOrJValleyDiffScore -= valleysAfter[1] and 1
			} else needLOrJValleyDiffScore -= valleysAfter[1] and 3
			if(valleysBefore[2]>3) {
				needLJValleyDiffScore += 1 shl (valleysBefore[2] shr 1)
				needLOrJValleyDiffScore += valleysBefore[2] and 1
			} else needLOrJValleyDiffScore += valleysBefore[2] and 3
			if(valleysAfter[2]>3) {
				needLJValleyDiffScore -= 1 shl (valleysAfter[2] shr 1)
				needLOrJValleyDiffScore -= valleysAfter[2] and 1
			} else needLOrJValleyDiffScore -= valleysAfter[2] and 3

			if(needIValleyDiffScore<0&&holeAfter>=holeBefore) {
				pts += needIValleyDiffScore*200
				if(depth==0) return Integer.MIN_VALUE
			} else if(needIValleyDiffScore>0) pts += if(depth==0&&!danger) needIValleyDiffScore*100
			else needIValleyDiffScore*200
			if(needLJValleyDiffScore<0&&holeAfter>=holeBefore) {
				pts += needLJValleyDiffScore*40
				if(depth==0) return Integer.MIN_VALUE
			} else if(needLJValleyDiffScore>0) pts += if(depth==0&&!danger) needLJValleyDiffScore*20
			else needLJValleyDiffScore*40

			if(needLOrJValleyDiffScore<0&&holeAfter>=holeBefore) pts += needLJValleyDiffScore*40
			else if(needLOrJValleyDiffScore>0) pts += if(!danger) needLOrJValleyDiffScore*20
			else needLOrJValleyDiffScore*40

			if(!big) {
				//Bonus for pyramidal stack
				val mid = width/2-1
				var d:Int
				for(i in 0..<mid-1) {
					d = depthsAfter[i]-depthsAfter[i+1]
					pts += if(d>=0) 10
					else d
				}
				for(i in mid+2..<width) {
					d = depthsAfter[i]-depthsAfter[i-1]
					pts += if(d>=0) 10
					else d
				}
				d = depthsAfter[mid-1]-depthsAfter[mid]
				pts += if(d>=0) 5
				else d
				d = depthsAfter[mid+1]-depthsAfter[mid]
				pts += if(d>=0) 5
				else d
			}

			if(heightBefore<heightAfter) {
				// Add points for reducing the height
				pts += if(depth==0&&!danger) (heightAfter-heightBefore)*10
				else (heightAfter-heightBefore)*20
			} else if(heightBefore>heightAfter)
			// Demerits for increase in height
				if(depth>0||danger) pts -= (heightBefore-heightAfter)*4

			//Penalty for prematurely filling in canyon
			if(!big&&!danger&&holeAfter>=holeBefore) for(i in 0..<width-1) if(depthsAfter[i]>depthsAfter[width-1]&&depthsBefore[i]<=depthsBefore[width-1]) {
				pts -= 1000000
				break
			}
			//Penalty for premature clears
			if(!big&&lines>0&&lines<4&&heightAfter>10&&xMax==width-1) {
				var minHi = 0
				for(i in 0..<width-1) {
					val hi = fld.getHighestBlockY(i)
					if(hi>minHi) minHi = hi
				}
				if(minHi>height-4) pts -= 300000
			}
			//Penalty for dangerous placements
			if(heightAfter<2*move) if(big) {
				if(heightAfter<0&&heightBefore>=0) return Integer.MIN_VALUE
				val spawnMinX = width/2-3
				val spawnMaxX = width/2+2
				var i = spawnMinX
				while(i<=spawnMaxX) {
					if(depthsAfter[i]<2*move&&depthsAfter[i]<depthsBefore[i]) pts -= 2000000*(depthsBefore[i]-depthsAfter[i])
					i += move
				}
			} else {
				val spawnMinX = width/2-2
				val spawnMaxX = width/2+1
				for(i in spawnMinX..spawnMaxX) if(depthsAfter[i]<2&&depthsAfter[i]<depthsBefore[i]) pts -= 2000000*(depthsBefore[i]-depthsAfter[i])
				if(heightBefore>=2&&depth==0) pts -= 2000000*(heightBefore-heightAfter)
			}
			val r2ColDepth = depthsAfter[width-2]
			if(!big&&danger&&r2ColDepth<depthsAfter[width-1]) {
				//Bonus if edge clear is possible
				var maxLeftDepth = depthsAfter[0]
				for(i in 1..<width-2) maxLeftDepth = maxOf(maxLeftDepth, depthsAfter[i])
				if(r2ColDepth>maxLeftDepth) pts += 200
			}
		}
		if(DEBUG_ALL) log.debug(
			"End of thinkMain($x, $y, $rt, "+rtOld+", fld, piece ${piece.type.name}, $depth). pts = "+pts
		)
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
			if(DEBUG_ALL) log.debug(
				"mostMovableX not applicable - low gravity (gravity = "+speed.gravity+", denominator = ${speed.denominator})"
			)
			if(dir<0) return piece.getMostMovableLeft(testX, testY, rt, fld!!)
			else if(dir>0) return piece.getMostMovableRight(testX, testY, rt, fld!!)
		}
		if(piece.type==Piece.Shape.I&&dir>0) return piece.getMostMovableRight(testX, testY, rt, fld!!)
		var floorKickOK = false
		if((piece.type==Piece.Shape.I||piece.type==Piece.Shape.T)&&
			(engine.nowWallkickRiseCount<engine.ruleOpt.spinWallkickMaxRise||
				engine.ruleOpt.spinWallkickMaxRise<0||engine.stat===GameEngine.Status.ARE)
		)
			floorKickOK = true
		testY = piece.getBottom(testX, testY, testRt, fld!!)
		if(piece.type==Piece.Shape.T&&piece.direction!=Piece.DIRECTION_UP) {
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
				} else if(floorKickOK) floorKickOK = false
				else return testX
			}
		}
		while(true) {
			if(!piece.checkCollision(testX+dir, testY, testRt, fld)) testX += dir
			else if(testRt!=rt) {
				testRt = rt
				if(floorKickOK&&piece.checkCollision(testX, testY, testRt, fld)) {
					if(piece.type==Piece.Shape.I) {
						testY -= if(piece.big) 4
						else 2
					} else testY--
					floorKickOK = false
				}
			} else {
				if(DEBUG_ALL) log.debug(
					"mostMovableX($x, $y, $dir, piece ${piece.type.name}, $rt) = $testX"
				)
				if(piece.type==Piece.Shape.I&&testX<0&&rt and 1>0) {
					val height1 = fld.getHighestBlockY(1)
					if(height1<fld.getHighestBlockY(2)&&height1<fld.getHighestBlockY(3)+2) return 0
					else if(height1>fld.getHighestBlockY(0)) return -1
				}
				return testX
			}
			testY = piece.getBottom(testX, testY, testRt, fld)
		}
	}

	private fun logBest(caseNum:Int) {
		log.debug(
			"New best position found (Case $caseNum): bestHold = $bestHold, bestX = $bestX, bestY = $bestY, bestRt = $bestRt, bestXSub = $bestXSub, bestYSub = $bestYSub, bestRtSub = $bestRtSub, bestPts = $bestPts"
		)
	}

	/**
	 * Called to display internal state
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	override fun renderState(engine:GameEngine, playerID:Int) {
		super.renderState(engine, playerID)
		engine.owner.receiver.run {
			drawMenuFont(engine, 0, 7, "IN ARE:", COLOR.BLUE, .5f)
			drawMenuFont(engine, 7, 7, inARE.getOX, .5f)
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
					thinkBestPosition(gEngine)
					thinkComplete = true
					if(DEBUG_ALL) log.debug("PoochyBot: thinkBestPosition completed successfully")
				} catch(e:Throwable) {
					log.debug("PoochyBot: thinkBestPosition Failed", e)
				}

				thinking = false
			}

			if(thinkDelay>0) try {
				Thread.sleep(thinkDelay.toLong())
			} catch(e:InterruptedException) {
				log.debug("PoochyBot: InterruptedException trying to sleep")
			}
		}

		threadRunning = false
		log.info("PoochyBot: Thread end")
	}

	//Wrapper for think requests
	@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
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
		protected const val MAX_THINK_DEPTH = 2
		/** Set to true to print debug information */
		const val DEBUG_ALL = false

		//private static final int[][] HI_PENALTY = {{6, 2}, {7, 6}, {6, 2}, {1, 0}};
		fun checkOffset(p:Piece?, engine:GameEngine):Piece {
			val result = Piece(p!!)
			result.big = engine.big
			if(!p.offsetApplied) result.applyOffsetArray(engine.ruleOpt.pieceOffsetX[p.id], engine.ruleOpt.pieceOffsetY[p.id])
			return result
		}

		fun calcValleys(depths:IntArray, move:Int):IntArray {
			val result = intArrayOf(0, 0, 0)
			if(depths[0]>depths[move]) result[0] = (depths[0]-depths[move])/3/move
			if(move>=2&&depths[depths.size-1]>depths[depths.size-move-1]) result[0] =
				(depths[depths.size-1]-depths[depths.size-move-1])/3/move
			var i = move
			while(i<depths.size-move) {
				val left = depths[i-move]
				val right = depths[i+move]
				val lowerSide = maxOf(left, right)
				val diff = depths[i]-lowerSide
				if(diff>=3) result[0] += diff/3/move
				if(left==right) if(left==depths[i]+2*move) {
					result[0]++
					result[1]--
					result[2]--
				} else if(left==depths[i]+move) {
					result[1]++
					result[2]++
				}
				if(diff/move%4==2) when {
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
		 * @return Y coord of the highest block
		 */
		@Deprecated(
			"Workaround for the bug in Field.getHighestBlockY(int).\n"+"\t  The bug has since been fixed as of NullpoMino v6.5, so\n"+"\t  fld.getHighestBlockY(x) should be equivalent."
		) fun getColumnDepth(fld:Field, x:Int):Int {
			val maxY = fld.height-1
			var result = fld.getHighestBlockY(x)
			if(result==maxY&&fld.getBlockEmpty(x, maxY)) result++
			return result
		}

		fun getColumnDepths(fld:Field):IntArray {
			val width = fld.width
			val result = IntArray(width)
			for(x in 0..<width) result[x] = fld.getHighestBlockY(x)
			return result
		}
	}
}
