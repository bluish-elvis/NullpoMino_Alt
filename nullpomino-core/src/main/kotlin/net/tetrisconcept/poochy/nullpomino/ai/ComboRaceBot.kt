/*
 Copyright (c) 2022-2024, NullNoname
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
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.ai.DummyAI
import mu.nu.nullpo.util.GeneralUtil.getOX
import org.apache.log4j.Logger
import kotlin.math.abs

/**
PoochyBot Combo Race AI
@author Poochy.EXE
Poochy.Spambucket@gmail.com
 */
class ComboRaceBot:DummyAI(), Runnable {
	private var moves:Array<Array<Transition?>>? = null
	private var nextQueueIDs:IntArray = IntArray(MAX_THINK_DEPTH)

	/** Movement state. 0 = initial, 1 = twist, 2 = post-twist  */
	private var movestate = 0
	/** When true,To threadThink routineInstructing the execution of the  */
	private var thinkRequest:ThinkRequestMutex? = null
	/** Last input if done in ARE  */
	private var inputARE = 0
	/** Did the thinking thread find a possible position?  */
	private var thinkSuccess = false
	/** Was the game in ARE as of the last frame?  */
	private var inARE = false
	/** AI's name */
	override val name:String
		get() = "Combo Race AI V1.03"
	/** Called at initialization */
	override fun init(engine:GameEngine, playerID:Int) {
		delay = 0
		gEngine = engine
		gManager = engine.owner
		thinkRequest = ThinkRequestMutex()
		thinking = false
		threadRunning = false
		inputARE = 0
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
	/** Called whenever a new piece is spawned */
	override fun newPiece(engine:GameEngine, playerID:Int) {
		if(!engine.aiUseThread) {
			thinkBestPosition(engine)
		} else if(!thinking&&!thinkComplete||!engine.aiPreThink||engine.aiShowHint
			||engine.are<=0||engine.areLine<=0
		) {
			thinkCurrentPieceNo++
			thinkRequest!!.newRequest()
		}
		movestate = 0
	}
	/** Called at the start of each frame */
	override fun onFirst(engine:GameEngine, playerID:Int) {
		inputARE = 0
		val newInARE = engine.stat===GameEngine.Status.ARE
		if(engine.aiPreThink&&engine.are>0&&engine.areLine>0
			&&(newInARE&&!inARE||!thinking&&!thinkSuccess)
		) {
			if(DEBUG_ALL) log.debug("Begin pre-think of next piece.")
			thinkComplete = false
			thinkRequest!!.newRequest()
		}
		inARE = newInARE
		if(inARE&&delay>=engine.aiMoveDelay) {
			var input = 0
			var nextPiece = engine.getNextObject(engine.nextPieceCount)
			if(bestHold&&thinkComplete) {
				input = input or Controller.BUTTON_BIT_D
				nextPiece = if(engine.holdPieceObject==null) engine.getNextObject(engine.nextPieceCount+1) else engine.holdPieceObject
			}
			if(nextPiece==null) return
			nextPiece = checkOffset(nextPiece, engine)
			//input |= calcIRS(nextPiece, engine);
			if(threadRunning&&!thinking&&thinkCurrentPieceNo<=thinkLastPieceNo) {
				val spawnX = engine.getSpawnPosX(nextPiece, engine.field)
				if(bestX-spawnX>1) {
					// left
					input = input or Controller.BUTTON_BIT_LEFT
				} else if(spawnX-bestX>1) {
					// right
					input = input or Controller.BUTTON_BIT_RIGHT
				}
				delay = 0
			}
			if(DEBUG_ALL) log.debug("Currently in ARE. Next piece type = "+nextPiece.id+", IRS = "+input)
			//engine.ctrl.setButtonBit(input);
			inputARE = input
		}
	}
	/** Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		if(engine.stat===GameEngine.Status.READY&&engine.statc[0]==0) thinkRequest!!.newCreateTablesRequest()
	}
	/** Set button input states */
	override fun setControl(engine:GameEngine, playerID:Int, ctrl:Controller):Int {
		if(engine.nowPieceObject!=null&&engine.stat===GameEngine.Status.MOVE&&
			delay>=engine.aiMoveDelay&&engine.statc[0]>0&&
			(!engine.aiUseThread||threadRunning&&!thinking&&thinkCurrentPieceNo<=thinkLastPieceNo)
		) {
			inputARE = 0
			var input = 0 // Button input data
			val pieceNow = checkOffset(engine.nowPieceObject, engine)
			val nowX = engine.nowPieceX
			val nowY = engine.nowPieceY
			val rt = pieceNow.direction
			val fld = engine.field
			val pieceTouchGround = pieceNow.checkCollision(nowX, nowY+1, fld)
			val nowType = pieceNow.id
			//int width = fld.getWidth();
			var moveDir = 0 //-1 = left,  1 = right
			var spinDir = 0 //-1 = left,  1 = right
			var drop = 0 //1 = up, -1 = down
			if(bestHold&&thinkComplete&&engine.isHoldOK) {
				// Hold
				input = Controller.BUTTON_BIT_D
				/*
				Piece holdPiece = engine.holdPieceObject;
				if (holdPiece != null)
					input |= calcIRS(holdPiece, engine);
				*/
			} else {
				if(DEBUG_ALL) log.debug(
					"bestX = "+bestX+", nowX = "+nowX+
						", bestY = "+bestY+", nowY = "+nowY+
						", bestRt = "+bestRt+", rt = "+rt+
						", bestRtSub = "+bestRtSub
				)
				printPieceAndDirection(nowType, rt)
				// Rotation
				/*
				//Spin iff near destination or stuck
				int xDiff = Math.abs(nowX - bestX);
				if (bestX < nowX && nowType == Piece.PIECE_I &&
						rt == Piece.DIRECTION_DOWN && bestRt != rt)
					xDiff--;
				if((rt != bestRt && ((xDiff <= 1) ||
						(bestX == 0 && nowX == 2 && nowType == Piece.PIECE_I) ||
						(((nowX < bestX && pieceNow.checkCollision(nowX+1, nowY, rt, fld)) ||
						(nowX > bestX && pieceNow.checkCollision(nowX-1, nowY, rt, fld))) &&
						!(pieceNow.getMaximumBlockX()+nowX == width-2 && (rt&1) == 1) &&
						!(pieceNow.getMinimumBlockY()+nowY == 2 && pieceTouchGround && (rt&1) == 0 && nowType != Piece.PIECE_I)))))
				*/if(rt!=bestRt) {
					val best180 = abs(rt-bestRt)==2
					//if (DEBUG_ALL) log.debug("Case 1 rotation");
					val spL = engine.getSpinDirection(-1)
					val spR = engine.getSpinDirection(1)
					if(DEBUG_ALL) log.debug("spL = $spL, spR = $spR")
					spinDir =
						when {
							best180&&(engine.ruleOpt.spinDoubleKey)&&!ctrl.isPress(Controller.BUTTON_E) -> 2
							bestRt==spR -> 1
							bestRt==spL -> -1
							engine.ruleOpt.spinReverseKey&&best180&&((rt and 1)==1) -> {
								if(spR==Piece.DIRECTION_UP) 1 else -1
							}
							else -> 1
						}
				}

				// Whether reachable position
				val minX = pieceNow.getMostMovableLeft(nowX, nowY, rt, fld)
				val maxX = pieceNow.getMostMovableRight(nowX, nowY, rt, fld)
				if(((movestate==0)&&(rt==bestRt)
						&&((bestX<minX-1)||(bestX>maxX+1)||(bestY<nowY)))
				) {
					// Again because it is thought unreachable
					//thinkBestPosition(engine, playerID);
					thinkComplete = false
					//thinkCurrentPieceNo++;
					//System.out.println("rethink c:" + thinkCurrentPieceNo + " l:" + thinkLastPieceNo);
					if(DEBUG_ALL) log.debug("Needs rethink - cannot reach desired position")
					thinkRequest!!.newRequest()
				} else {
					// If you are able to reach
					if((nowX==bestX)&&(pieceTouchGround)) {
						if(rt==bestRt) {
							// Groundrotation
							if(bestRtSub!=0&&movestate==0) {
								bestRt = pieceNow.getSpinDirection(bestRtSub, bestRt)
								spinDir = bestRtSub
								bestRtSub = 0
								movestate = 1
							}
						}
					}
					if((nowX==bestX||movestate>0)&&(rt==bestRt)) {
						moveDir = 0
						// Funnel
						if(bestRtSub==0) {
							if(pieceTouchGround&&engine.ruleOpt.softdropLock) drop = -1
							else if(engine.ruleOpt.harddropEnable) drop = 1
							else if(engine.ruleOpt.softdropEnable||engine.ruleOpt.softdropLock) drop = -1
						} else {
							if(engine.ruleOpt.harddropEnable&&!engine.ruleOpt.harddropLock) drop = 1
							else if(engine.ruleOpt.softdropEnable&&!engine.ruleOpt.softdropLock) drop = -1
						}
					} else if(nowX>bestX) moveDir = -1 else if(nowX<bestX) moveDir = 1
				}
			}
			//Convert parameters to input
			if(moveDir==-1&&!ctrl.isPress(Controller.BUTTON_LEFT)) input = Controller.BUTTON_BIT_LEFT
			else if(moveDir==1&&!ctrl.isPress(Controller.BUTTON_RIGHT)) input = Controller.BUTTON_BIT_RIGHT
			if(drop==1&&!ctrl.isPress(Controller.BUTTON_UP)) input = Controller.BUTTON_BIT_UP
			else if(drop==-1) input = Controller.BUTTON_BIT_DOWN
			if(spinDir!=0) {
				val spinRight = (engine.owSpinDir==1||(engine.owSpinDir==-1&&engine.ruleOpt.spinToRight))
				if(engine.ruleOpt.spinDoubleKey&&(spinDir==2)&&!ctrl.isPress(Controller.BUTTON_E))
					input = input or Controller.BUTTON_BIT_E
				else if((engine.ruleOpt.spinReverseKey&&!spinRight&&(spinDir==1))) {
					if(!ctrl.isPress(Controller.BUTTON_B)) input = input or Controller.BUTTON_BIT_B
				} else if((engine.ruleOpt.spinReverseKey&&spinRight&&(spinDir==-1))) {
					if(!ctrl.isPress(Controller.BUTTON_B)) input = input or Controller.BUTTON_BIT_B
				} else if(!ctrl.isPress(Controller.BUTTON_A)) input = input or Controller.BUTTON_BIT_A
			}
			if(DEBUG_ALL) log.debug("Input = $input, moveDir = $moveDir, spinDir = $spinDir, drop = $drop")
			delay = 0
			return (input)
		}
		delay++
		return (inputARE)
	}

	private fun printPieceAndDirection(pieceType:Int, rt:Int) {
		val result = "Piece ${pieceType}, direction ${
			when(rt) {
				Piece.DIRECTION_LEFT -> "left"
				Piece.DIRECTION_DOWN -> "down"
				Piece.DIRECTION_UP -> "up"
				Piece.DIRECTION_RIGHT -> "right"
				else -> ""
			}
		}"

		if(DEBUG_ALL) log.debug(result)
	}
	/*
	public int calcIRS(Piece piece, GameEngine engine)
	{
		piece = checkOffset(piece, engine);
		int nextType = piece.id;
		Field fld = engine.field;
		int spawnX = engine.getSpawnPosX(fld, piece);
		SpeedParam speed = engine.speed;
		boolean gravityHigh = speed.gravity > speed.denominator;
		int width = fld.getWidth();
		int midColumnX = (width/2)-1;
		if(Math.abs(spawnX - bestX) == 1)
		{
			if (bestRt == 1)
			{
				if (engine.ruleOpt.spinButtonDefaultRight)
					return Controller.BUTTON_BIT_A;
				else
					return Controller.BUTTON_BIT_B;
			}
			else if (bestRt == 3)
			{
				if (engine.ruleOpt.spinButtonDefaultRight)
					return Controller.BUTTON_BIT_B;
				else
					return Controller.BUTTON_BIT_A;
			}
		}
		else if (nextType == Piece.PIECE_L)
		{
			if (gravityHigh && fld.getHighestBlockY(midColumnX-1) <
					minOf(fld.getHighestBlockY(midColumnX), fld.getHighestBlockY(midColumnX+1)))
				return 0;
			else if (engine.ruleOpt.spinButtonDefaultRight)
				return Controller.BUTTON_BIT_B;
			else
				return Controller.BUTTON_BIT_A;
		}
		else if (nextType == Piece.PIECE_J)
		{
			if (gravityHigh && fld.getHighestBlockY(midColumnX+1) <
					minOf(fld.getHighestBlockY(midColumnX), fld.getHighestBlockY(midColumnX-1)))
				return 0;
			if (engine.ruleOpt.spinButtonDefaultRight)
				return Controller.BUTTON_BIT_A;
			else
				return Controller.BUTTON_BIT_B;
		}
		//else if (nextType == Piece.PIECE_I)
		//	return Controller.BUTTON_BIT_A;
		return 0;
	}
	*/
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
		bestRtSub = 0
		bestPts = Int.MIN_VALUE
		thinkSuccess = false
		val fld = Field(engine.field)
		val fx = maxOf(0, fld.width-4)/2
		var pieceNow = engine.nowPieceObject
		var pieceHold = engine.holdPieceObject
		val holdBoxEmpty = (pieceHold==null)
		var nextIndex = engine.nextPieceCount
		if(inARE||pieceNow==null) {
			pieceNow = engine.getNextObjectCopy(nextIndex)
			nextIndex++
		}
		if(holdBoxEmpty) pieceHold = engine.getNextObjectCopy(nextIndex)
		else if(holdBoxEmpty) pieceHold = engine.getNextObjectCopy(engine.nextPieceCount)
		pieceNow = checkOffset(pieceNow, engine)
		pieceHold = checkOffset(pieceHold, engine)
		val holdOK = engine.isHoldOK
		var holdID = -1
		if(engine.holdPieceObject!=null) holdID = engine.holdPieceObject!!.id
		nextQueueIDs = IntArray(MAX_THINK_DEPTH) {engine.getNextID(nextIndex+it)}
		val state = fieldToIndex(fld)
		if(state<0) {
			thinkLastPieceNo++
			return
		}
		var t = moves!![state][pieceNow.id]
		while(t!=null) {
			val pts = thinkMain(engine, t.newField, holdID, 0)
			if(pts>bestPts) {
				bestPts = pts
				bestX = t.x+fx
				bestRt = t.rt
				bestY = pieceNow.getBottom(bestX, 0, bestRt, fld)
				bestRtSub = t.rtSub
				bestHold = false
				thinkSuccess = true
			}
			t = t.next
		}
		if(pieceHold.id!=pieceNow.id&&holdOK) {
			t = moves!![state][pieceHold.id]
			while(t!=null) {
				val pts = thinkMain(engine, t.newField, pieceNow.id, if(holdBoxEmpty) 1 else 0)
				if(pts>bestPts) {
					bestPts = pts
					bestX = t.x+fx
					bestRt = t.rt
					bestY = pieceNow.getBottom(bestX, 0, bestRt, fld)
					bestRtSub = t.rtSub
					bestHold = true
					thinkSuccess = true
				}
				t = t.next
			}
		}
		thinkLastPieceNo++
		if(engine.aiShowHint) {
			val pieceTemp = if(bestHold) pieceHold else pieceNow
			bestY = pieceTemp.getBottom(bestX, bestY, bestRt, fld)
			bestXSub = bestX
			bestYSub = bestY
			if(bestRtSub!=0) {
				val newRt = (bestRt+bestRtSub+Piece.DIRECTION_COUNT)%Piece.DIRECTION_COUNT
				if((pieceTemp.checkCollision(bestX, bestY, newRt, fld)&&(engine.wallkick!=null)&&
						(engine.ruleOpt.spinWallkick))
				) {
					val kick = engine.wallkick!!.executeWallkick(
						bestX, bestY, -1, bestRt, newRt,
						(engine.ruleOpt.spinWallkickMaxRise!=0), (pieceTemp), fld, null
					)
					if(kick!=null) {
						bestX += kick.offsetX
						bestY = pieceTemp.getBottom(bestX, bestY+kick.offsetY, newRt, fld)
					}
				}
				bestRtSub = bestRt
				bestRt = newRt
			} else bestRtSub = bestRt
		}

		//System.out.println("X:" + bestX + " Y:" + bestY + " R:" + bestRt + " H:" + bestHold + " Pts:" + bestPts);
	}
	/**
	 * Think routine
	 * @param engine GameEngine
	 * @param state Think state
	 * @param holdID Hold piece ID
	 * @param depth Search depth
	 * @return Evaluation score
	 */
	private fun thinkMain(engine:GameEngine, state:Int, holdID:Int, depth:Int):Int {
		if(state==-1) return 0
		if(depth==nextQueueIDs.size) {
			var result = stateScores[state]*100
			if(holdID==Piece.PIECE_I) result += 1000 else if(holdID>=0&&holdID<pieceScores.size) result += pieceScores[holdID]*100/28
			return result
		}
		var bestPts = 0
		var t = moves!![state][nextQueueIDs[depth]]
		while(t!=null) {
			bestPts = maxOf(bestPts, thinkMain(engine, t.newField, holdID, depth+1)+1000)
			t = t.next
		}
		if(engine.ruleOpt.holdEnable) {
			if(holdID==-1) bestPts = maxOf(bestPts, thinkMain(engine, state, nextQueueIDs[depth], depth+1)) else {
				t = moves!![state][holdID]
				while(t!=null) {
					bestPts = maxOf(bestPts, thinkMain(engine, t.newField, nextQueueIDs[depth], depth+1)+1000)
					t = t.next
				}
			}
		}
		return bestPts
	}
	/*
	 * Processing of the thread
	 */
	override fun run() {
		log.info("ComboRaceBot: Thread start")
		threadRunning = true
		while(threadRunning&&thinkRequest!=null) {
			try {
				synchronized(thinkRequest!!) {if(!thinkRequest!!.active&&!thinkRequest!!.createTablesRequest) thinkRequest?.wait()}
			} catch(e:InterruptedException) {
				log.debug("ComboRaceBot: InterruptedException waiting for thinkRequest signal")
			}
			if(thinkRequest!!.active) {
				thinkRequest!!.active = false
				thinking = true
				try {
					thinkBestPosition(gEngine)
					thinkComplete = true
					//log.debug("ComboRaceBot: thinkBestPosition completed successfully");
				} catch(e:Throwable) {
					log.debug("ComboRaceBot: thinkBestPosition Failed", e)
				}
				thinking = false
			} else if(thinkRequest!!.createTablesRequest) createTables(gEngine)
			if(thinkDelay>0) {
				try {
					Thread.sleep(thinkDelay.toLong())
				} catch(e:InterruptedException) {
					break
				}
			}
		}
		threadRunning = false
		log.info("ComboRaceBot: Thread end")
	}
	/**
	 * Constructs the moves table if necessary.
	 */
	fun createTables(engine:GameEngine?) {
		if(moves!=null) return
		moves = Array(FIELDS.size) {arrayOfNulls(7)}
		val fldEmpty = Field(4, Field.DEFAULT_HEIGHT, Field.DEFAULT_HIDDEN_HEIGHT)
		val fldBackup = Field(fldEmpty)
		val fldTemp = Field(fldEmpty)
		val pieces = Array(7) {p -> checkOffset(Piece(p), engine).apply {setColor(1)}}
		var count = 0
		for(i in FIELDS.indices) {
			fldBackup.replace(fldEmpty)
			var code = FIELDS[i].toInt()
			for(y in Field.DEFAULT_HEIGHT-1 downTo (Field.DEFAULT_HEIGHT-4)+1) for(x in 3 downTo 0) {
				if((code and 1)==1) fldBackup.setBlockColor(x, y, 1)
				code = code shr 1
			}
			for(p in 0..6) {
				val tempX = engine!!.getSpawnPosX(pieces[p], fldBackup)
				for(rt in 0..<Piece.DIRECTION_COUNT) {
					val minX = pieces[p].getMostMovableLeft(tempX, 0, rt, fldBackup)
					val maxX = pieces[p].getMostMovableRight(tempX, 0, rt, fldBackup)
					for(x in minX..maxX) {
						val y = pieces[p].getBottom(x, 0, rt, fldBackup)
						if((p==Piece.PIECE_L)||(p==Piece.PIECE_T)||(p==Piece.PIECE_J)||(rt<2)) {
							fldTemp.replace(fldBackup)
							pieces[p].placeToField(x, y, rt, fldTemp)
							if(fldTemp.checkLine()==1) {
								fldTemp.clearLine()
								fldTemp.downFloatingBlocks()
								val index = fieldToIndex(fldTemp, 0)
								if(index>=0) {
									moves!![i][p] = Transition(
										x, rt, index,
										moves!![i][p]
									)
									count++
								}
							}
							if(p==Piece.PIECE_O) continue
						}

						// Left rotation
						if(!engine.ruleOpt.spinToRight||engine.ruleOpt.spinReverseKey) {
							val rot = pieces[p].getSpinDirection(-1, rt)
							var newX = x
							var newY = y
							fldTemp.replace(fldBackup)
							if((pieces[p].checkCollision(x, y, rot, fldTemp)&&(engine.wallkick!=null)&&
									(engine.ruleOpt.spinWallkick))
							) {
								val kick = engine.wallkick!!.executeWallkick(
									x, y, -1, rt, rot,
									(engine.ruleOpt.spinWallkickMaxRise!=0), (pieces[p]), fldTemp, null
								)
								if(kick!=null) {
									newX = x+kick.offsetX
									newY = pieces[p].getBottom(newX, y+kick.offsetY, rot, fldTemp)
								}
							}
							if((!pieces[p].checkCollision(newX, newY, rot, fldTemp)
									&&newY>pieces[p].getBottom(newX, 0, rot, fldTemp))
							) {
								pieces[p].placeToField(newX, newY, rot, fldTemp)
								if(fldTemp.checkLine()==1) {
									fldTemp.clearLine()
									fldTemp.downFloatingBlocks()
									val index = fieldToIndex(fldTemp, 0)
									if(index>=0) {
										moves!![i][p] = Transition(
											x, rt, -1, index,
											moves!![i][p]
										)
										count++
									}
								}
							}
						}

						// Right rotation
						if(engine.ruleOpt.spinToRight||engine.ruleOpt.spinReverseKey) {
							val rot = pieces[p].getSpinDirection(1, rt)
							var newX = x
							var newY = y
							fldTemp.replace(fldBackup)
							if((pieces[p].checkCollision(x, y, rot, fldTemp)&&(engine.wallkick!=null)&&
									(engine.ruleOpt.spinWallkick))
							) {
								val kick = engine.wallkick!!.executeWallkick(
									x, y, 1, rt, rot,
									(engine.ruleOpt.spinWallkickMaxRise!=0), (pieces[p]), fldTemp, null
								)
								if(kick!=null) {
									newX = x+kick.offsetX
									newY = pieces[p].getBottom(newX, y+kick.offsetY, rot, fldTemp)
								}
							}
							if((!pieces[p].checkCollision(newX, newY, rot, fldTemp)
									&&newY>pieces[p].getBottom(newX, 0, rot, fldTemp))
							) {
								pieces[p].placeToField(newX, newY, rot, fldTemp)
								if(fldTemp.checkLine()==1) {
									fldTemp.clearLine()
									fldTemp.downFloatingBlocks()
									val index = fieldToIndex(fldTemp, 0)
									if(index>=0) {
										moves!![i][p] = Transition(
											x, rt, 1, index,
											moves!![i][p]
										)
										count++
									}
								}
							}
						}
					}
					if(pieces[p].id==Piece.PIECE_O) break
				}
			}
		}
		log.debug("Transition table created. Total entries: $count")
		//TODO: PageRank scores for each state
	}

	override fun renderState(engine:GameEngine, playerID:Int) {
		super.renderState(engine, playerID)
		engine.owner.receiver.run {
			drawMenuFont(engine, 0, 7, "THINK REQUEST:", EventReceiver.COLOR.BLUE, .5f)
			drawMenuFont(engine, 14, 7, (thinkRequest?.active ?: false).getOX, .5f)
			drawMenuFont(
				engine, 0, 8, "THINK SUCCESS:", if(thinkSuccess) EventReceiver.COLOR.BLUE else EventReceiver.COLOR.RED,
				.5f
			)
			drawMenuFont(engine, 14, 8, thinkSuccess.getOX, !thinkSuccess, .5f)
			drawMenuFont(engine, 0, 9, "THINK COMPLETE:", EventReceiver.COLOR.BLUE, .5f)
			drawMenuFont(engine, 15, 9, thinkComplete.getOX, .5f)
			drawMenuFont(engine, 0, 10, "IN ARE:", EventReceiver.COLOR.BLUE, .5f)
			drawMenuFont(engine, 7, 10, inARE.getOX, .5f)
			drawMenuFont(engine, 0, 11, "QUEUE:", EventReceiver.COLOR.BLUE, .5f)
			var color = EventReceiver.COLOR.GREEN
			for(i in nextQueueIDs.indices) {
				if(i>=bestPts/1000&&color!=EventReceiver.COLOR.RED) color =
					if(i<nextQueueIDs.size-1&&thinkComplete) EventReceiver.COLOR.RED else EventReceiver.COLOR.YELLOW
				drawMenuFont(engine, 6+i, 11, Piece.Shape.names[nextQueueIDs[i]], color, .5f)
			}
			val code = fieldToCode(engine.field)
			drawMenuFont(engine, 0, 12, "STATE:", EventReceiver.COLOR.BLUE, .5f)
			drawMenuFont(
				engine, 6, 12, if(code.toInt()==-1) "---" else
					"#${fieldToIndex(engine.field)}:${Integer.toHexString(code.toInt()).uppercase()}", .5f
			)
		}
	}

	override fun renderHint(engine:GameEngine, playerID:Int) {
		val r = engine.owner.receiver
		r.drawScoreFont(engine, 10, 3, "AI HINT MOVE:", EventReceiver.COLOR.GREEN)
		if(bestPts>0&&(thinkComplete||(((thinkCurrentPieceNo>0)
				&&(thinkCurrentPieceNo<=thinkLastPieceNo))))
		) {
			if(bestHold&&thinkComplete&&engine.isHoldOK) r.drawScoreFont(engine, 10, 4, "HOLD") else {
				val pieceNow = engine.nowPieceObject
				val fld:Field = engine.field
				if(pieceNow==null) return
				val nowX = engine.nowPieceX
				val nowY = engine.nowPieceY
				val rt = pieceNow.direction
				val pieceTouchGround = pieceNow.checkCollision(nowX, nowY+1, fld)
				if(pieceTouchGround&&(nowX==bestX)&&(nowY==bestY)&&(rt==bestRt)) return else if(pieceTouchGround&&(nowX==bestXSub)&&(nowY==bestYSub)&&(rt==bestRtSub)) {
					val best180 = abs(rt-bestRt)==2
					//if (DEBUG_ALL) log.debug("Case 1 rotation");
					val spL = engine.getSpinDirection(-1)
					val spR = engine.getSpinDirection(1)
					if(DEBUG_ALL) log.debug("spL = $spL, spR = $spR")
					val spinDir =
						if(best180&&(engine.ruleOpt.spinDoubleKey)) 2 else if(bestRt==spR) 1 else if(bestRt==spL) -1 else if(engine.ruleOpt.spinReverseKey&&best180&&((rt and 1)==1)) {
							if(spR==Piece.DIRECTION_UP) 1 else -1
						} else 1
					when(spinDir) {
						-1 -> r.drawScoreFont(engine, 10, 4, "SPIN LEFT")
						1 -> r.drawScoreFont(engine, 10, 4, "SPIN RIGHT")
						2 -> r.drawScoreFont(engine, 10, 4, "SPIN 180")
					}
					return
				}
				var moveDir = 0 //-1 = left,  1 = right
				var drop = 0 //1 = up, -1 = down
				var writeY = 4
				if(rt!=bestRtSub) {
					val best180 = abs(rt-bestRtSub)==2
					//if (DEBUG_ALL) log.debug("Case 1 rotation");
					val spL = engine.getSpinDirection(-1)
					val spR = engine.getSpinDirection(1)
					if(DEBUG_ALL) log.debug("spL = $spL, spR = $spR")
					val spinDir =//-1 = left,  1 = right, 2 = 180
						if(best180&&(engine.ruleOpt.spinDoubleKey)) 2 else if(bestRtSub==spR) 1 else if(bestRtSub==spL) -1
						else if(engine.ruleOpt.spinReverseKey&&best180&&((rt and 1)==1)) {
							if(spR==Piece.DIRECTION_UP) 1 else -1
						} else 1
					when(spinDir) {
						-1 -> r.drawScoreFont(engine, 10, writeY, "SPIN LEFT")
						1 -> r.drawScoreFont(engine, 10, writeY, "SPIN RIGHT")
						2 -> r.drawScoreFont(engine, 10, writeY, "SPIN 180")
					}
					writeY++
				}

				// Whether reachable position
				val minX = pieceNow.getMostMovableLeft(nowX, nowY, rt, fld)
				val maxX = pieceNow.getMostMovableRight(nowX, nowY, rt, fld)
				if(((movestate==0)&&(rt==bestRtSub)
						&&((bestXSub<minX-1)||(bestXSub>maxX+1)||(bestYSub<nowY)))
				) {
					// Again because it is thought unreachable
					//thinkBestPosition(engine, playerID);
					thinkComplete = false
					//thinkCurrentPieceNo++;
					//System.out.println("rethink c:" + thinkCurrentPieceNo + " l:" + thinkLastPieceNo);
					if(DEBUG_ALL) log.debug("Needs rethink - cannot reach desired position")
					thinkRequest!!.newRequest()
				} else {
					if((nowX==bestXSub||movestate>0)&&(rt==bestRtSub)) {
						moveDir = 0
						// Funnel
						if(bestRtSub==bestRt) {
							if(pieceTouchGround&&engine.ruleOpt.softdropLock) drop = -1
							else if(engine.ruleOpt.harddropEnable) drop = 1
							else if(engine.ruleOpt.softdropEnable||engine.ruleOpt.softdropLock) drop = -1
						} else {
							if(engine.ruleOpt.harddropEnable&&!engine.ruleOpt.harddropLock) drop = 1
							else if(engine.ruleOpt.softdropEnable&&!engine.ruleOpt.softdropLock) drop = -1
						}
					} else if(nowX>bestXSub) moveDir = -1 else if(nowX<bestXSub) moveDir = 1
				}
				if(moveDir!=0) {
					r.drawScoreFont(engine, 10, writeY, if(moveDir==-1) "MOVE LEFT" else "MOVE RIGHT")
					writeY++
				}
				if(drop!=0) {
					r.drawScoreFont(engine, 10, writeY, if(drop==-1) "SOFT DROP" else "HARD DROP")
					writeY++
				}
			}
		}
	}

	private class Transition(var x:Int, var rt:Int, var rtSub:Int = 0, var newField:Int, var next:Transition? = null) {
		constructor(bestX:Int, bestRt:Int, newFld:Int):this(bestX, bestRt, 0, newFld, null)
		constructor(bestX:Int, bestRt:Int, bestRtSub:Int, newFld:Int):this(bestX, bestRt, bestRtSub, newFld, null)
		constructor(bestX:Int, bestRt:Int, newFld:Int, nxt:Transition?):this(bestX, bestRt, 0, newFld, nxt)
	}
	//Wrapper for think requests
	class ThinkRequestMutex:Object() {
		var active = false
		var createTablesRequest = false
		@Synchronized fun newRequest() {
			active = true
			notifyAll()
		}

		@Synchronized fun newCreateTablesRequest() {
			createTablesRequest = true
			notifyAll()
		}
	}

	companion object {
		/** Log  */
		var log:Logger = Logger.getLogger(ComboRaceBot::class.java)
		/** List of field state codes which are possible to sustain a stable combo  */
		private val FIELDS = shortArrayOf(
			0x7, 0xB, 0xD, 0xE,
			0x13, 0x15, 0x16, 0x19, 0x1A, 0x1C,
			0x23, 0x29,
			0x31, 0x32,
			0x49, 0x4C,
			0x61, 0x68,
			0x83, 0x85, 0x86, 0x89, 0x8A, 0x8C,
			0xC4, 0xC8,
			0x111, 0x888
		)
		/** Number of pieces to think ahead  */
		private const val MAX_THINK_DEPTH = 6
		/** Set to true to print debug information  */
		private const val DEBUG_ALL = false
		fun checkOffset(p:Piece?, engine:GameEngine?):Piece {
			val result = Piece((p)!!)
			result.big = engine!!.big
			if(!p.offsetApplied) result.applyOffsetArray(engine.ruleOpt.pieceOffsetX[p.id], engine.ruleOpt.pieceOffsetY[p.id])
			return result
		}
		/**
		 * Converts field to field state int code
		 * @param field Field object
		 * @param valleyX Leftmost x-coordinate of 4-block-wide valley to combo in
		 * @return Field state int code.
		 */
		@JvmOverloads fun fieldToCode(field:Field, valleyX:Int = maxOf(field.width-4, 0)/2):Short {
			val height = field.height
			var result:Short = 0
			for(y in height-3..<height) for(x in 0..3) {
				result = (result.toInt() shl 1).toShort()
				if(!field.getBlockEmpty(x+valleyX, y, false)) result++
			}
			return result
		}
		/**
		 * Converts field state int code to FIELDS array index
		 * @param field Field state int code
		 * @return State index if found; -1 if not found.
		 */
		private fun fieldToIndex(field:Short):Int {
			var min = 0
			var max = FIELDS.size-1
			var mid:Int
			while(min<=max) {
				mid = (min+max) shr 1
				if(FIELDS[mid]>field) max = mid-1 else if(FIELDS[mid]<field) min = mid+1 else return mid
			}
			return -1
		}
		/**
		 * Converts field object to FIELDS array index
		 * @param field Field object
		 * @param valleyX Leftmost x-coordinate of 4-block-wide valley to combo in
		 * @return State index if found; -1 if not found.
		 */
		fun fieldToIndex(field:Field, valleyX:Int):Int = fieldToIndex(fieldToCode(field, valleyX))

		fun fieldToIndex(field:Field):Int = fieldToIndex(fieldToCode(field))

		private var stateScores = intArrayOf(6, 7, 7, 6, 8, 3, 2, 9, 3, 4, 3, 1, 8, 4, 1, 3, 1, 1, 4, 3, 9, 2, 3, 8, 4, 8, 3, 3)
		private var pieceScores = intArrayOf(28, 18, 10, 9, 18, 18, 9)
	}
}
