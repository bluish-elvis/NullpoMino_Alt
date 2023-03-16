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
package edu.cuhk.cse.fyp.tetrisai.lspi

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.ai.DummyAI
import mu.nu.nullpo.game.subsystem.mode.VSBattle
import org.apache.log4j.Logger
import kotlin.math.abs

/**
 * CommonAI
 */
open class LSPIAI:DummyAI(), Runnable {
	/** ホールド使用予定  */
	var bestOppHold = false
	/** Plan to putX-coordinate  */
	var bestOppX = 0
	/** Plan to putY-coordinate  */
	var bestOppY = 0
	/** Plan to putDirection  */
	var bestOppRt = 0
	/** Hold Force  */
	var oppforceHold = false
	/** The best moveEvaluation score  */
	var bestPtsD = 0.0
		set(value) {
			bestPts = value.toInt()
			field = value
		}
	/** After that I was groundedX-coordinate  */
	var bestOppXSub = 0
	/** After that I was groundedY-coordinate  */
	var bestOppYSub = 0
	/** After that I was groundedDirection(-1: None)  */
	var bestOppRtSub = 0
	/** The best moveEvaluation score  */
	var bestOppPts = 0.0
	/** When true,To threadThink routineInstructing the execution of the  */
	protected var thinkRequest = false
	protected var player:PlayerSkeleton = PlayerSkeleton().apply {learns = TRAIN}
	var nowState = State()
	var oppNowState = State()
	var futureState = FutureState()
	var oppFutureState = FutureState()
	var nowstateOpp = State()
	var oppnowstateOpp = State()
	var futurestateOpp = FutureState()
	var oppfuturestateOpp = FutureState()
	var move = 0
	var nowid = 0
	var height = 0
	var hiddenHeight = 0
	var width = 0
	var twoPlayerGame = false
	var lastf:DoubleArray? = null
	var savedLastf:DoubleArray? = null
	/*
	 * AIOfName
	 */
	override val name = "LSPI"
	/*
	 * Called at initialization
	 */
	override fun init(engine:GameEngine, playerID:Int) {
		player = PlayerSkeleton().apply {learns = TRAIN}
		nowState = State()
		oppNowState = State()
		move = 0
		nowid = 0
		val playerNum = engine.owner.players
		twoPlayerGame = false
		if(playerNum==2) twoPlayerGame = true
		gEngine = engine
		gManager = engine.owner
		thinkRequest = false
		thinking = false
		threadRunning = false
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

	/*
	 * Called whenever a new piece is spawned
	 */
	override fun newPiece(engine:GameEngine, playerID:Int) {
		if(!engine.aiUseThread) {
			thinkBestPosition(engine, playerID)
		} else {
			thinkRequest = true
			thinkCurrentPieceNo++
		}
	}
	/*
	 * Set button input states
	 */
	override fun setControl(engine:GameEngine, playerID:Int, ctrl:Controller):Int {
		if(engine.nowPieceObject!=null&&engine.stat===GameEngine.Status.MOVE&&delay>=engine.aiMoveDelay
			&&engine.statc[0]>0
			&&(!engine.aiUseThread||threadRunning&&!thinking&&thinkCurrentPieceNo<=thinkLastPieceNo)
		) {
			var input = 0 // button input data
			val pieceNow = engine.nowPieceObject
			val nowX = engine.nowPieceX
			val nowY = engine.nowPieceY
			val rt = pieceNow!!.direction
			val fld = engine.field
			val pieceTouchGround = pieceNow.checkCollision(nowX, nowY+1, fld)
			if((bestHold||forceHold)&&engine.isHoldOK) {
				// Hold
				input = input or Controller.BUTTON_BIT_D
			} else {
				// rotation
				if(rt!=bestRt) {
					val lrot = engine.getSpinDirection(-1)
					val rrot = engine.getSpinDirection(1)
					if(abs(rt-bestRt)==2&&engine.ruleOpt.spinDoubleKey&&!ctrl.isPress(Controller.BUTTON_E)) {
						input = input or Controller.BUTTON_BIT_E
					} else if(!ctrl.isPress(Controller.BUTTON_B)&&engine.ruleOpt.spinReverseKey
						&&!engine.spinDirection&&bestRt==rrot
					) {
						input = input or Controller.BUTTON_BIT_B
					} else if(!ctrl.isPress(Controller.BUTTON_B)&&engine.ruleOpt.spinReverseKey
						&&engine.spinDirection&&bestRt==lrot
					) {
						input = input or Controller.BUTTON_BIT_B
					} else if(!ctrl.isPress(Controller.BUTTON_A)) {
						input = input or Controller.BUTTON_BIT_A
					}
				}

				// Whether reachable position
				val minX = pieceNow.getMostMovableLeft(nowX, nowY, rt, fld)
				val maxX = pieceNow.getMostMovableRight(nowX, nowY, rt, fld)
				if((bestX<minX-1||bestX>maxX+1||bestY<nowY)&&rt==bestRt) {
					// Again because it is thought unreachable
					// thinkBestPosition(engine, playerID);
					thinkRequest = true
					// thinkCurrentPieceNo++;
					// System.out.println("rethink c:" + thinkCurrentPieceNo + " l:" +
					// thinkLastPieceNo);
				} else {
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
						if(!ctrl.isPress(Controller.BUTTON_RIGHT)||engine.aiMoveDelay>=0) input = input or Controller.BUTTON_BIT_RIGHT
					} else if(nowX==bestX&&rt==bestRt) {
						// Funnel
						if(bestRtSub==-1&&bestX==bestXSub) {
							if(engine.ruleOpt.harddropEnable&&!ctrl.isPress(Controller.BUTTON_UP)) input =
								input or Controller.BUTTON_BIT_UP else if(engine.ruleOpt.softdropEnable||engine.ruleOpt.softdropLock) input =
								input or Controller.BUTTON_BIT_DOWN
						} else {
							if(engine.ruleOpt.harddropEnable&&!engine.ruleOpt.harddropLock
								&&!ctrl.isPress(Controller.BUTTON_UP)
							) input =
								input or Controller.BUTTON_BIT_UP else if(engine.ruleOpt.softdropEnable&&!engine.ruleOpt.softdropLock) input =
								input or Controller.BUTTON_BIT_DOWN
						}
					}
				}
			}
			delay = 0
			return input
		}
		delay++
		return 0
	}
	/**
	 * Search for the best choice
	 *
	 * @param engine   The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	open fun thinkBestPosition(engine:GameEngine?, playerID:Int) {
		bestHold = false
		bestX = 0
		bestY = 0
		bestRt = 0
		bestXSub = 0
		bestYSub = 0
		bestRtSub = -1
		bestPtsD = 0.0
		forceHold = false
		height = engine!!.fieldHeight
		hiddenHeight = engine.fieldHiddenHeight
		width = engine.fieldWidth

		/*
		Field fld = engine.field;
        int[][] transformed_fld = new int[height][width];
        for (int c = 0; c < width; c++) {
        	for (int r = 0; r < height; r++) {
        		transformed_fld[r][c] = fld.getBlockEmpty(c, r) ? 0 : 1;
        	}
        }
        int heightest;
        int rr;
        int tmp;
        for (int c = 0; c < State.COLS; c++) {
			heightest = 0;
	        for (int r = 0; r < State.ROWS; r++) {
				try {
					tmp = 0;
					try {
						rr = (State.ROWS - r + 1);
						if (rr > 0)
							tmp = transformed_fld[(State.ROWS - r + 1)][c];
					} catch(Exception e) {
						tmp = 0;
					}
					state.getField()[r][c] = tmp;
					if (tmp == 1)
						heightest = maxOf(r + 1, heightest);
				} catch(Exception e) {
					System.out.println(e);
				}
	        }
			state.top[c] = heightest;
        }

        nowid = engine.nowPieceObject.id;
        state.nextPiece = nullpomino2lspi[nowid];
        if (DEBUG) {
            log.info("NowId:" + nowid);
            log.info("Piece:" + state.nextPiece);
        }
        int[][] tmpLegalMoves = state.legalMoves();
        move = player.pickMove(state,tmpLegalMoves);

        int[] tempNum = tmpLegalMoves[move];
        int orient = tempNum[0];
        int offset = tempNum[1];
        bestX = offset + lspi2X[state.nextPiece][orient];
        bestXSub = bestX;
        bestRt = lspi2rotate[state.nextPiece][orient];
        if (DEBUG) {
        	log.info("Orient LSPI:" + orient);
        	log.info("Offset LSPI:" + offset);
        	log.info("Orient NULL:" + bestRt);
        	log.info("Offset NULL:" + bestX);
        }

        // simulate inside AI
        state.makeMove(move);
        if (TRAIN) {
            //player.getBasisFunctions().computeWeights();
            //log.info(self.player.bs.weight);
        }
        if (DEBUG) {
            // print field
            player.printField(state.getField());
        }

        */
		val pieceNow = engine.nowPieceObject
		val nowX = engine.nowPieceX
		val nowY = engine.nowPieceY
		val holdOK = engine.isHoldOK
		var holdEmpty = false
		var pieceHold = engine.holdPieceObject
		val pieceNext = engine.getNextObject(engine.nextPieceCount)
		val fld = Field(engine.field)
		if(pieceHold==null) {
			holdEmpty = true
		}

		// Convert to now State
		// My now state
		nowState = createState(fld, engine)
		if(twoPlayerGame) {
			val oppEngine:GameEngine = engine.owner.engine[1-engine.playerID]
			val oppFld = Field(oppEngine.field)
			// Opp now state
			oppNowState = createState(oppFld, oppEngine)
		}
		for(depth in 0 until maxThinkDepth) {
			for(rt in 0 until Piece.DIRECTION_COUNT) {
				// Peace for now
				val minX = pieceNow!!.getMostMovableLeft(nowX, nowY, rt, engine.field)
				val maxX = pieceNow.getMostMovableRight(nowX, nowY, rt, engine.field)
				for(x in minX..maxX) {
					fld.replace(engine.field)
					val y = pieceNow.getBottom(x, nowY, rt, fld)
					if(!pieceNow.checkCollision(x, y, rt, fld)) {
						// As it is
						var pts = thinkMain(engine, x, y, rt, -1, fld, pieceNow, pieceNext, pieceHold, depth)
						if(pts>=bestPtsD) {
							bestHold = false
							bestX = x
							bestY = y
							bestRt = rt
							bestXSub = x
							bestYSub = y
							bestRtSub = -1
							bestPtsD = pts
							savedLastf = lastf
						}
						if(depth>0||pieceNow.id==Piece.PIECE_T) {
							//if ((depth > 0) || (bestPts <= 10) || (pieceNow.id == Piece.PIECE_T)) {
							// Left shift
							fld.replace(engine.field)
							if(!pieceNow.checkCollision(x-1, y, rt, fld)
								&&pieceNow.checkCollision(x-1, y-1, rt, fld)
							) {
								pts = thinkMain(engine, x-1, y, rt, -1, fld, pieceNow, pieceNext, pieceHold, depth)
								if(pts>bestPtsD) {
									bestHold = false
									bestX = x
									bestY = y
									bestRt = rt
									bestXSub = x-1
									bestYSub = y
									bestRtSub = -1
									bestPtsD = pts
									savedLastf = lastf
								}
							}

							// Right shift
							fld.replace(engine.field)
							if(!pieceNow.checkCollision(x+1, y, rt, fld)
								&&pieceNow.checkCollision(x+1, y-1, rt, fld)
							) {
								pts = thinkMain(engine, x+1, y, rt, -1, fld, pieceNow, pieceNext, pieceHold, depth)
								if(pts>bestPtsD) {
									bestHold = false
									bestX = x
									bestY = y
									bestRt = rt
									bestXSub = x+1
									bestYSub = y
									bestRtSub = -1
									bestPtsD = pts
									savedLastf = lastf
								}
							}

							// Leftrotation
							if(!engine.spinDirection||engine.ruleOpt.spinReverseKey) {
								val rot = pieceNow.getSpinDirection(-1, rt)
								var newX = x
								var newY = y
								fld.replace(engine.field)
								pts = 0.0
								if(!pieceNow.checkCollision(x, y, rot, fld)) {
									pts = thinkMain(engine, x, y, rot, rt, fld, pieceNow, pieceNext, pieceHold, depth)
								} else if(engine.wallkick!=null&&engine.ruleOpt.spinWallkick) {
									val allowUpward = (engine.ruleOpt.spinWallkickMaxRise<0
										||engine.nowWallkickRiseCount<engine.ruleOpt.spinWallkickMaxRise)
									val kick = engine.wallkick!!.executeWallkick(
										x, y, -1, rt, rot,
										allowUpward, pieceNow, fld, null
									)
									if(kick!=null) {
										newX = x+kick.offsetX
										newY = y+kick.offsetY
										pts = thinkMain(
											engine, newX, newY, rot, rt, fld, pieceNow, pieceNext,
											pieceHold, depth
										)
									}
								}
								if(pts>bestPtsD) {
									bestHold = false
									bestX = x
									bestY = y
									bestRt = rt
									bestXSub = newX
									bestYSub = newY
									bestRtSub = rot
									bestPtsD = pts
									savedLastf = lastf
								}
							}

							// Rightrotation
							if(engine.spinDirection||engine.ruleOpt.spinReverseKey) {
								val rot = pieceNow.getSpinDirection(1, rt)
								var newX = x
								var newY = y
								fld.replace(engine.field)
								pts = 0.0
								if(!pieceNow.checkCollision(x, y, rot, fld)) {
									pts = thinkMain(engine, x, y, rot, rt, fld, pieceNow, pieceNext, pieceHold, depth)
								} else if(engine.wallkick!=null&&engine.ruleOpt.spinWallkick) {
									val allowUpward = (engine.ruleOpt.spinWallkickMaxRise<0
										||engine.nowWallkickRiseCount<engine.ruleOpt.spinWallkickMaxRise)
									val kick = engine.wallkick!!.executeWallkick(
										x, y, 1, rt, rot, allowUpward,
										pieceNow, fld, null
									)
									if(kick!=null) {
										newX = x+kick.offsetX
										newY = y+kick.offsetY
										pts = thinkMain(
											engine, newX, newY, rot, rt, fld, pieceNow, pieceNext,
											pieceHold, depth
										)
									}
								}
								if(pts>bestPtsD) {
									bestHold = false
									bestX = x
									bestY = y
									bestRt = rt
									bestXSub = newX
									bestYSub = newY
									bestRtSub = rot
									bestPtsD = pts
									savedLastf = lastf
								}
							}

							// 180-degree rotation
							if(engine.ruleOpt.spinDoubleKey) {
								val rot = pieceNow.getSpinDirection(2, rt)
								var newX = x
								var newY = y
								fld.replace(engine.field)
								pts = 0.0
								if(!pieceNow.checkCollision(x, y, rot, fld)) {
									pts = thinkMain(engine, x, y, rot, rt, fld, pieceNow, pieceNext, pieceHold, depth)
								} else if(engine.wallkick!=null&&engine.ruleOpt.spinWallkick) {
									val allowUpward = (engine.ruleOpt.spinWallkickMaxRise<0
										||engine.nowWallkickRiseCount<engine.ruleOpt.spinWallkickMaxRise)
									val kick = engine.wallkick!!.executeWallkick(
										x, y, 2, rt, rot, allowUpward,
										pieceNow, fld, null
									)
									if(kick!=null) {
										newX = x+kick.offsetX
										newY = y+kick.offsetY
										pts = thinkMain(
											engine, newX, newY, rot, rt, fld, pieceNow, pieceNext,
											pieceHold, depth
										)
									}
								}
								if(pts>bestPtsD) {
									bestHold = false
									bestX = x
									bestY = y
									bestRt = rt
									bestXSub = newX
									bestYSub = newY
									bestRtSub = rot
									bestPtsD = pts
									savedLastf = lastf
								}
							}
						}
					}
				}
				if(pieceHold==null) {
					pieceHold = engine.getNextObject(engine.nextPieceCount)
				}
				// Hold Peace
				if(holdOK&&pieceHold!=null&&depth==0) {
					val spawnX = engine.getSpawnPosX(engine.field, pieceHold)
					val spawnY = engine.getSpawnPosY(pieceHold)
					val minHoldX = pieceHold.getMostMovableLeft(spawnX, spawnY, rt, engine.field)
					val maxHoldX = pieceHold.getMostMovableRight(spawnX, spawnY, rt, engine.field)
					for(x in minHoldX..maxHoldX) {
						fld.replace(engine.field)
						val y = pieceHold.getBottom(x, spawnY, rt, fld)
						if(!pieceHold.checkCollision(x, y, rt, fld)) {
							var pieceNext2 = engine.getNextObject(engine.nextPieceCount)
							if(holdEmpty) pieceNext2 = engine.getNextObject(engine.nextPieceCount+1)
							val pts = thinkMain(engine, x, y, rt, -1, fld, pieceHold, pieceNext2, null, depth)
							if(pts>bestPtsD) {
								bestHold = true
								bestX = x
								bestY = y
								bestRt = rt
								bestRtSub = -1
								bestPtsD = pts
								savedLastf = lastf
							}
						}
					}
				}
			}
			if(bestPtsD>0) break
		}
		thinkLastPieceNo++

		//System.out.println("X:" + bestX + " Y:" + bestY + " R:" + bestRt + " H:" +
		//bestHold + " Pts:" + bestPts);
//		for (int i = 0; i < TwoPlayerBasisFunction.FEATURE_COUNT; i++)
//			System.out.println(lastf[i]);
		//System.out.println(savedLastf[3]);
	}
	/**
	 * Search for the bestOpp choice
	 *
	 * @param engine   The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	open fun thinkbestOppPosition(engine:GameEngine, playerID:Int) {
		bestOppHold = false
		bestOppX = 0
		bestOppY = 0
		bestOppRt = 0
		bestOppXSub = 0
		bestOppYSub = 0
		bestOppRtSub = -1
		bestOppPts = 0.0
		oppforceHold = false
		height = engine.fieldHeight
		hiddenHeight = engine.fieldHiddenHeight
		width = engine.fieldWidth
		val pieceNow = engine.nowPieceObject
		val nowX = engine.nowPieceX
		val nowY = engine.nowPieceY
		val holdOK = engine.isHoldOK
		var holdEmpty = false
		var pieceHold = engine.holdPieceObject
		val pieceNext = engine.getNextObject(engine.nextPieceCount)
		val fld = Field(engine.field)
		if(pieceHold==null) {
			holdEmpty = true
		}

		// Convert to now State
		// My now state
		nowstateOpp = createState(fld, engine)
		if(twoPlayerGame) {
			val oppEngine:GameEngine = engine.owner.engine[1-engine.playerID]
			val oppFld = Field(oppEngine.field)
			// Opp now state
			oppnowstateOpp = createState(oppFld, oppEngine)
		}
		for(depth in 0 until maxThinkDepth) {
			for(rt in 0 until Piece.DIRECTION_COUNT) {
				// Peace for now
				val minX = pieceNow!!.getMostMovableLeft(nowX, nowY, rt, engine.field)
				val maxX = pieceNow.getMostMovableRight(nowX, nowY, rt, engine.field)
				for(x in minX..maxX) {
					fld.replace(engine.field)
					val y = pieceNow.getBottom(x, nowY, rt, fld)
					if(!pieceNow.checkCollision(x, y, rt, fld)) {
						// As it is
						var pts = thinkMainByLineStack(engine, x, y, rt, -1, fld, pieceNow, pieceNext, pieceHold, depth)
						if(pts>=bestOppPts) {
							bestOppHold = false
							bestOppX = x
							bestOppY = y
							bestOppRt = rt
							bestOppXSub = x
							bestOppYSub = y
							bestOppRtSub = -1
							bestOppPts = pts
							savedLastf = lastf
						}
						if(depth>0||pieceNow.id==Piece.PIECE_T) {
							//if ((depth > 0) || (bestOppPts <= 10) || (pieceNow.id == Piece.PIECE_T)) {
							// Left shift
							fld.replace(engine.field)
							if(!pieceNow.checkCollision(x-1, y, rt, fld)
								&&pieceNow.checkCollision(x-1, y-1, rt, fld)
							) {
								pts = thinkMainByLineStack(engine, x-1, y, rt, -1, fld, pieceNow, pieceNext, pieceHold, depth)
								if(pts>bestOppPts) {
									bestOppHold = false
									bestOppX = x
									bestOppY = y
									bestOppRt = rt
									bestOppXSub = x-1
									bestOppYSub = y
									bestOppRtSub = -1
									bestOppPts = pts
									savedLastf = lastf
								}
							}

							// Right shift
							fld.replace(engine.field)
							if(!pieceNow.checkCollision(x+1, y, rt, fld)
								&&pieceNow.checkCollision(x+1, y-1, rt, fld)
							) {
								pts = thinkMainByLineStack(engine, x+1, y, rt, -1, fld, pieceNow, pieceNext, pieceHold, depth)
								if(pts>bestOppPts) {
									bestOppHold = false
									bestOppX = x
									bestOppY = y
									bestOppRt = rt
									bestOppXSub = x+1
									bestOppYSub = y
									bestOppRtSub = -1
									bestOppPts = pts
									savedLastf = lastf
								}
							}

							// Leftrotation
							if(!engine.spinDirection||engine.ruleOpt.spinReverseKey) {
								val rot = pieceNow.getSpinDirection(-1, rt)
								var newX = x
								var newY = y
								fld.replace(engine.field)
								pts = 0.0
								if(!pieceNow.checkCollision(x, y, rot, fld)) {
									pts = thinkMainByLineStack(engine, x, y, rot, rt, fld, pieceNow, pieceNext, pieceHold, depth)
								} else if(engine.wallkick!=null&&engine.ruleOpt.spinWallkick) {
									val allowUpward = (engine.ruleOpt.spinWallkickMaxRise<0
										||engine.nowWallkickRiseCount<engine.ruleOpt.spinWallkickMaxRise)
									val kick = engine.wallkick!!.executeWallkick(
										x, y, -1, rt, rot,
										allowUpward, pieceNow, fld, null
									)
									if(kick!=null) {
										newX = x+kick.offsetX
										newY = y+kick.offsetY
										pts = thinkMainByLineStack(
											engine, newX, newY, rot, rt, fld, pieceNow, pieceNext,
											pieceHold, depth
										)
									}
								}
								if(pts>bestOppPts) {
									bestOppHold = false
									bestOppX = x
									bestOppY = y
									bestOppRt = rt
									bestOppXSub = newX
									bestOppYSub = newY
									bestOppRtSub = rot
									bestOppPts = pts
									savedLastf = lastf
								}
							}

							// Rightrotation
							if(engine.spinDirection||engine.ruleOpt.spinReverseKey) {
								val rot = pieceNow.getSpinDirection(1, rt)
								var newX = x
								var newY = y
								fld.replace(engine.field)
								pts = 0.0
								if(!pieceNow.checkCollision(x, y, rot, fld)) {
									pts = thinkMainByLineStack(engine, x, y, rot, rt, fld, pieceNow, pieceNext, pieceHold, depth)
								} else if(engine.wallkick!=null&&engine.ruleOpt.spinWallkick) {
									val allowUpward = (engine.ruleOpt.spinWallkickMaxRise<0
										||engine.nowWallkickRiseCount<engine.ruleOpt.spinWallkickMaxRise)
									val kick = engine.wallkick!!.executeWallkick(
										x, y, 1, rt, rot, allowUpward,
										pieceNow, fld, null
									)
									if(kick!=null) {
										newX = x+kick.offsetX
										newY = y+kick.offsetY
										pts = thinkMainByLineStack(
											engine, newX, newY, rot, rt, fld, pieceNow, pieceNext,
											pieceHold, depth
										)
									}
								}
								if(pts>bestOppPts) {
									bestOppHold = false
									bestOppX = x
									bestOppY = y
									bestOppRt = rt
									bestOppXSub = newX
									bestOppYSub = newY
									bestOppRtSub = rot
									bestOppPts = pts
									savedLastf = lastf
								}
							}

							// 180-degree rotation
							if(engine.ruleOpt.spinDoubleKey) {
								val rot = pieceNow.getSpinDirection(2, rt)
								var newX = x
								var newY = y
								fld.replace(engine.field)
								pts = 0.0
								if(!pieceNow.checkCollision(x, y, rot, fld)) {
									pts = thinkMainByLineStack(engine, x, y, rot, rt, fld, pieceNow, pieceNext, pieceHold, depth)
								} else if(engine.wallkick!=null&&engine.ruleOpt.spinWallkick) {
									val allowUpward = (engine.ruleOpt.spinWallkickMaxRise<0
										||engine.nowWallkickRiseCount<engine.ruleOpt.spinWallkickMaxRise)
									val kick = engine.wallkick!!.executeWallkick(
										x, y, 2, rt, rot, allowUpward,
										pieceNow, fld, null
									)
									if(kick!=null) {
										newX = x+kick.offsetX
										newY = y+kick.offsetY
										pts = thinkMainByLineStack(
											engine, newX, newY, rot, rt, fld, pieceNow, pieceNext,
											pieceHold, depth
										)
									}
								}
								if(pts>bestOppPts) {
									bestOppHold = false
									bestOppX = x
									bestOppY = y
									bestOppRt = rt
									bestOppXSub = newX
									bestOppYSub = newY
									bestOppRtSub = rot
									bestOppPts = pts
									savedLastf = lastf
								}
							}
						}
					}
				}
				if(pieceHold==null) {
					pieceHold = engine.getNextObject(engine.nextPieceCount)
				}
				// Hold Peace
				if(holdOK&&pieceHold!=null&&depth==0) {
					val spawnX = engine.getSpawnPosX(engine.field, pieceHold)
					val spawnY = engine.getSpawnPosY(pieceHold)
					val minHoldX = pieceHold.getMostMovableLeft(spawnX, spawnY, rt, engine.field)
					val maxHoldX = pieceHold.getMostMovableRight(spawnX, spawnY, rt, engine.field)
					for(x in minHoldX..maxHoldX) {
						fld.replace(engine.field)
						val y = pieceHold.getBottom(x, spawnY, rt, fld)
						if(!pieceHold.checkCollision(x, y, rt, fld)) {
							var pieceNext2 = engine.getNextObject(engine.nextPieceCount)
							if(holdEmpty) pieceNext2 = engine.getNextObject(engine.nextPieceCount+1)
							val pts = thinkMainByLineStack(engine, x, y, rt, -1, fld, pieceHold, pieceNext2, null, depth)
							if(pts>bestOppPts) {
								bestOppHold = true
								bestOppX = x
								bestOppY = y
								bestOppRt = rt
								bestOppRtSub = -1
								bestOppPts = pts
								savedLastf = lastf
							}
						}
					}
				}
			}
			if(bestOppPts>0) break
		}

		//System.out.println("X:" + bestOppX + " Y:" + bestOppY + " R:" + bestOppRt + " H:" +
		//bestOppHold + " Pts:" + bestOppPts);
//		for (int i = 0; i < TwoPlayerBasisFunction.FEATURE_COUNT; i++)
//			System.out.println(lastf[i]);
		//System.out.println(savedLastf[3]);
	}
	/**
	 * Think routine
	 *
	 * @param engine    GameEngine
	 * @param x         X-coordinate
	 * @param y         Y-coordinate
	 * @param rt        Direction
	 * @param rtOld     Direction before rotation (-1: None)
	 * @param fld       Field (Can be modified without problems)
	 * @param piece     Piece
	 * @param nextpiece NEXTPeace
	 * @param holdpiece HOLDPeace(nullMay be)
	 * @param depth     Compromise level (ranges from 0 through getMaxThinkDepth-1)
	 * @return Evaluation score
	 */
	open fun thinkMainByLineStack(
		engine:GameEngine, x:Int, y:Int, rt:Int, rtOld:Int, fld:Field, piece:Piece?, nextpiece:Piece?,
		holdpiece:Piece?, depth:Int
	):Double {
		var pts = 0.0

		// T-Spin flag
		val tspin = (piece!!.type==Piece.Shape.T&&rtOld!=-1&&fld.isTwistSpot(x, y, piece.big))

		// Place the piece
		if(!piece.placeToField(x, y, rt, fld)) return 0.0

		// Line clear
		val lines = fld.checkLine()
		if(lines>0) {
			fld.clearLine()
			fld.downFloatingBlocks()
		}

		// All clear
		val allclear = fld.isEmpty
		if(allclear) pts += 500000.0
		val lineSent = nowstateOpp.calLinesSentResult(lines, tspin)

		// Convert to future State
		oppfuturestateOpp = FutureState()

		// My future state
		futurestateOpp = createFutureState(nowstateOpp, fld, engine, 0, lineSent, lines)
		if(twoPlayerGame) {
			val oppEngine:GameEngine = engine.owner.engine[1-engine.playerID]
			val oppFld:Field = oppEngine.field
			// Opp future state
			oppfuturestateOpp = createFutureState(oppnowstateOpp, oppFld, oppEngine, lineSent, 0, 0)
		}
		val f:DoubleArray?
		if(twoPlayerGame) {
			f = player.twoPlayerBasisFunctions.getFutureArray(nowstateOpp, futurestateOpp, oppnowstateOpp, oppfuturestateOpp)
			for(i in 0 until TwoPlayerBasisFunction.FEATURE_COUNT) {
				pts += f[i]*player.twoPlayerBasisFunctions.weight[i]
				//log.error(i + ":" + f[i]);
			}
		} else {
			f = player.basisFunctions.getFeatureArray(nowstateOpp, futurestateOpp)
			for(i in 0 until BasisFunction.FUTURE_COUNT) {
				pts += f[i]*player.basisFunctions.weight[i]
				//log.error(i + ":" + f[i]);
			}
		}
		lastf = f
		return pts
	}
	/**
	 * Think routine
	 *
	 * @param engine    GameEngine
	 * @param x         X-coordinate
	 * @param y         Y-coordinate
	 * @param rt        Direction
	 * @param rtOld     Direction before rotation (-1: None)
	 * @param fld       Field (Can be modified without problems)
	 * @param piece     Piece
	 * @param nextpiece NEXTPeace
	 * @param holdpiece HOLDPeace(nullMay be)
	 * @param depth     Compromise level (ranges from 0 through getMaxThinkDepth-1)
	 * @return Evaluation score
	 */
	open fun thinkMain(
		engine:GameEngine?, x:Int, y:Int, rt:Int, rtOld:Int, fld:Field, piece:Piece?, nextpiece:Piece?,
		holdpiece:Piece?, depth:Int
	):Double {
		var pts = 0.0
		var oppNowState = oppNowState

		// T-Spin flag
		val tspin = (piece!!.type==Piece.Shape.T&&rtOld!=-1&&fld.isTwistSpot(x, y, piece.big))

		// Place the piece
		if(!piece.placeToField(x, y, rt, fld)) return 0.0

		// Line clear
		val lines = fld.checkLine()
		if(lines>0) {
			fld.clearLine()
			fld.downFloatingBlocks()
		}

		// All clear
		val allclear = fld.isEmpty
		if(allclear) pts += 500000.0
		val lineSent = nowState.calLinesSentResult(lines, tspin)

		// Convert to future State
		oppFutureState = FutureState()
		if(twoPlayerGame) {
			val oppEngine:GameEngine = engine!!.owner.engine[1-engine.playerID]
			val oppFld = Field()
			val oppNow = Piece()
			var oppLines = 0
			if(oppEngine.nowPieceObject!=null) {
				oppFld.replace(oppEngine.field)
				oppNow.replace(oppEngine.nowPieceObject!!)
				thinkbestOppPosition(oppEngine, 1-engine.playerID)
				oppNow.placeToField(bestOppX, bestOppY, oppFld)
				// Line clear
				oppLines = oppFld.checkLine()
				if(oppLines>0) {
					oppFld.clearLine()
					oppFld.downFloatingBlocks()
				}
			}
			oppNowState = createState(oppFld, oppEngine)
			val oppLineSent = oppNowState.calLinesSentResult(lines, tspin)
			oppFutureState = createFutureState(oppNowState, oppFld, oppEngine, 0, oppLineSent, oppLines)

			// My future state
			futureState = createFutureState(nowState, fld, engine, oppLineSent, lineSent, lines)
		} else {
			// My future state
			futureState = createFutureState(nowState, fld, engine, 0, lineSent, lines)
		}
		val f:DoubleArray?
		if(twoPlayerGame) {
			f = player.twoPlayerBasisFunctions.getFutureArray(nowState, futureState, oppNowState, oppFutureState)
			for(i in 0 until TwoPlayerBasisFunction.FEATURE_COUNT) {
				pts += f[i]*player.twoPlayerBasisFunctions.weight[i]
				//log.error(i + ":" + f[i]);
			}
		} else {
			f = player.basisFunctions.getFeatureArray(nowState, futureState)
			for(i in 0 until BasisFunction.FUTURE_COUNT) {
				pts += f[i]*player.basisFunctions.weight[i]
				//log.error(i + ":" + f[i]);
			}
		}
		lastf = f

		//log.error(pts);
		return pts
	}

	protected fun createState(fld:Field, engine:GameEngine?):State {
		val newState = State()
		val transformedFld = Array(fld.fullHeight) {IntArray(width)}
		for(c in 0 until width) {
			for(r in -hiddenHeight until height) {
				transformedFld[r+hiddenHeight][c] = if(fld.getBlockEmpty(c, r)) 0 else 1
			}
		}
		var heightest:Int
		var rr:Int
		var tmp:Int
		for(c in 0 until State.COLS) {
			heightest = 0
			for(r in 0 until State.ROWS) {
				try {
					tmp = 0
					try {
						rr = State.ROWS-r-1
						if(rr>0) tmp = transformedFld[State.ROWS-r-1][c]
					} catch(e:Exception) {
						tmp = 0
					}
					newState.field[r]?.set(c, tmp)
					if(tmp==1) heightest = maxOf(r+1, heightest)
				} catch(e:Exception) {
					println(e)
				}
			}
			newState.top[c] = heightest
		}
		if(engine!=null) {
			if(engine.owner.mode is VSBattle) {
				(engine.owner.mode as VSBattle).let {mode ->
					newState.addLinesStack(mode.garbage[engine.playerID])
					newState.addLineSent(mode.garbageSent[engine.playerID])
				}
			} else {
				newState.addLineSent(engine.lineClearing)
			}
			newState.addLineCleared(engine.lineClearing)
		}
		return newState
	}

	protected fun createFutureState(
		oldState:State, fld:Field, engine:GameEngine?, deltaMeter:Int, deltaLineSent:Int, deltaLineCleared:Int
	):FutureState {
		val newState = FutureState()
		newState.resetToCurrentState(oldState)
		val transformedFld = Array(fld.fullHeight) {IntArray(width)}
		for(c in 0 until width) {
			for(r in -hiddenHeight until height) {
				transformedFld[r+hiddenHeight][c] = if(fld.getBlockEmpty(c, r)) 0 else 1
			}
		}
		var heightest:Int
		var rr:Int
		var tmp:Int
		for(c in 0 until State.COLS) {
			heightest = 0
			for(r in 0 until State.ROWS) {
				try {
					tmp = 0
					try {
						rr = State.ROWS-r-1
						if(rr>0) tmp = transformedFld[State.ROWS-r-1][c]
					} catch(e:Exception) {
						tmp = 0
					}
					newState.field[r]?.set(c, tmp)
					if(tmp==1) heightest = maxOf(r+1, heightest)
				} catch(e:Exception) {
					println(e)
				}
			}
			newState.top[c] = heightest
		}
		newState.addLinesStack(deltaMeter)
		if(engine!!.owner.mode is VSBattle) {
			val mode = engine.owner.mode as VSBattle?
			newState.addLineSent(deltaLineSent)
		} else {
			newState.addLineSent(deltaLineCleared)
		}
		newState.addLineCleared(deltaLineCleared)
		return newState
	}
	/** MaximumCompromise level*/
	open val maxThinkDepth:Int
		get() = 2
	/*
	 * Processing of the thread
	 */
	override fun run() {
		log.info("LSPIAI: Thread start")
		threadRunning = true
		while(threadRunning) {
			if(thinkRequest) {
				thinkRequest = false
				thinking = true
				try {
					thinkBestPosition(gEngine, gEngine.playerID)
				} catch(e:Throwable) {
					log.debug("LSPIAI: thinkBestPosition Failed", e)
				}
				thinking = false
			}
			if(thinkDelay>0) {
				try {
					Thread.sleep(thinkDelay.toLong())
				} catch(e:InterruptedException) {
					break
				}
			}
		}
		threadRunning = false
		log.info("LSPIAI: Thread end")
	}

	companion object {
		/** Log  */
		var log:Logger = Logger.getLogger(LSPIAI::class.java)
		/**
		 * NEW THINGS IN ESTR LSPI AI
		 *
		 *
		 */
		const val TRAIN = false
		const val DEBUG = false
		/** Map LSPI AI piece to nullpomino piece  */
		val lspi2nullpomino = intArrayOf(2, 0, 1, 5, 4, 6, 3)
		/** Map nullpomino piece to LSPI AI piece  */
		val nullpomino2lspi = intArrayOf(1, 2, 0, 6, 4, 3, 5)
		/** Map LSPI AI spin to nullpomino spin according piece  */
		val lspi2rotate =
			arrayOf(
				intArrayOf(0),
				intArrayOf(3, 0),
				intArrayOf(1, 2, 3, 0),
				intArrayOf(3, 0, 1, 2),
				intArrayOf(1, 2, 3, 0),
				intArrayOf(0, 1),
				intArrayOf(0, 1)
			)
		/** Map LSPI AI X to nullpomino X according piece  */
		val lspi2X =
			arrayOf(
				intArrayOf(0),
				intArrayOf(-1, 0),
				intArrayOf(-1, 0, 0, 0),
				intArrayOf(0, 0, -1, 0),
				intArrayOf(-1, 0, 0, 0),
				intArrayOf(0, -1),
				intArrayOf(0, -1)
			)
	}
}
