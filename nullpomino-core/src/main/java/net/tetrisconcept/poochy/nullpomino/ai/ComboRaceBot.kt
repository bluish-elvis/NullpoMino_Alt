package net.tetrisconcept.poochy.nullpomino.ai

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.subsystem.ai.DummyAI
import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.Logger
import kotlin.math.abs

/**
 * PoochyBot Combo Race AI
 * @author Poochy.EXE
 * Poochy.Spambucket@gmail.com
 */
class ComboRaceBot:DummyAI(), Runnable {

	private val stateScores = intArrayOf(6, 7, 7, 6, 8, 3, 2, 9, 3, 4, 3, 1, 8, 4, 1, 3, 1, 1, 4, 3, 9, 2, 3, 8, 4, 8, 3, 3)

	private val pieceScores = intArrayOf(28, 18, 10, 9, 18, 18, 9)

	private var moves:Array<Array<Transition>>? = null

	private var nextQueueIDs:IntArray = IntArray(0)

	/** After that I was groundedDirection(0: None) */
	private var bestRtSub:Int = 0

	/** Position before twist for hint display */
	private var bestXSub:Int = 0
	private var bestYSub:Int = 0

	/** The best moveEvaluation score */
	private var bestPts:Int = 0

	/** Movement state. 0 = initial, 1 = twist, 2 = post-twist */
	private var movestate:Int = 0

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

	/** Last input if done in ARE */
	private var inputARE:Int = 0
	/** Did the thinking thread finish successfully? */
	override var thinkComplete:Boolean = false
	/** Did the thinking thread find a possible position? */
	private var thinkSuccess:Boolean = false
	/** Was the game in ARE as of the last frame? */
	private var inARE:Boolean = false

	/* AI's name */
	override val name:String
		get() = "Combo Race AI V1.03"

	/* Called at initialization */
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
			thinkCurrentPieceNo++
			thinkRequest.newRequest()
		}
		movestate = 0
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
			//input |= calcIRS(nextPiece, engine);
			if(threadRunning&&!thinking&&thinkCurrentPieceNo<=thinkLastPieceNo) {
				val spawnX = engine.getSpawnPosX(engine.field, nextPiece)
				if(bestX-spawnX>1)
				// left
					input = input or Controller.BUTTON_BIT_LEFT
				else if(spawnX-bestX>1)
				// right
					input = input or Controller.BUTTON_BIT_RIGHT
				delay = 0
			}
			if(DEBUG_ALL) log.debug("Currently in ARE. Next piece type = ${nextPiece.id}, IRS = $input")
			//engine.ctrl.setButtonBit(input);
			inputARE = input
		}
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		if(engine.stat===GameEngine.Status.READY&&engine.statc[0]==0) thinkRequest.newCreateTablesRequest()
	}

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
			val nowType = pieceNow.id
			//int width = fld.getWidth();

			var moveDir = 0 //-1 = left,  1 = right
			var rotateDir = 0 //-1 = left,  1 = right
			var drop = 0 //1 = up, -1 = down

			if(bestHold&&thinkComplete&&engine.isHoldOK)
			// Hold
				input = input or Controller.BUTTON_BIT_D
			else {
				if(DEBUG_ALL)
					log.debug("bestX = $bestX, nowX = $nowX, bestY = $bestY, nowY = $nowY, bestRt = $bestRt, rt = $rt, bestRtSub = $bestRtSub")
				printPieceAndDirection(nowType, rt)
				// Rotation
				/* //Rotate iff near destination or stuck
				 * int xDiff = Math.abs(nowX - bestX);
				 * if (bestX < nowX && nowType == Piece.PIECE_I &&
				 * rt == Piece.DIRECTION_DOWN && bestRt != rt)
				 * xDiff--;
				 * if((rt != bestRt && ((xDiff <= 1) ||
				 * (bestX == 0 && nowX == 2 && nowType == Piece.PIECE_I) ||
				 * (((nowX < bestX && pieceNow.checkCollision(nowX+1, nowY, rt,
				 * fld)) ||
				 * (nowX > bestX && pieceNow.checkCollision(nowX-1, nowY, rt,
				 * fld))) &&
				 * !(pieceNow.getMaximumBlockX()+nowX == width-2 && (rt&1) == 1)
				 * &&
				 * !(pieceNow.getMinimumBlockY()+nowY == 2 && pieceTouchGround
				 * && (rt&1) == 0 && nowType != Piece.PIECE_I))))) */
				if(rt!=bestRt) {
					val best180 = abs(rt-bestRt)==2
					//if (DEBUG_ALL) log.debug("Case 1 rotation");

					val lrot = engine.getRotateDirection(-1)
					val rrot = engine.getRotateDirection(1)
					if(DEBUG_ALL) log.debug("lrot = $lrot, rrot = $rrot")

					rotateDir = when {
						best180&&engine.ruleopt.rotateButtonAllowDouble&&!ctrl.isPress(Controller.BUTTON_E) -> 2
						bestRt==rrot -> 1
						bestRt==lrot -> -1
						engine.ruleopt.rotateButtonAllowReverse&&best180&&rt and 1==1 -> if(rrot==Piece.DIRECTION_UP) 1 else -1
						else -> 1
					}
				}

				// Whether reachable position
				val minX = pieceNow.getMostMovableLeft(nowX, nowY, rt, fld)
				val maxX = pieceNow.getMostMovableRight(nowX, nowY, rt, fld)

				if(movestate==0&&rt==bestRt
					&&(bestX<minX-1||bestX>maxX+1||bestY<nowY)) {
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
						if(rt==bestRt)
						// Groundrotation
							if(bestRtSub!=0&&movestate==0) {
								bestRt = pieceNow.getRotateDirection(bestRtSub, bestRt)
								rotateDir = bestRtSub
								bestRtSub = 0
								movestate = 1
							}
					when {
						(nowX==bestX||movestate>0)&&rt==bestRt -> {
							moveDir = 0
							// Funnel
							when {
								bestRtSub==0 -> when {
									pieceTouchGround&&engine.ruleopt.softdropLock -> drop = -1
									engine.ruleopt.harddropEnable -> drop = 1
									engine.ruleopt.softdropEnable||engine.ruleopt.softdropLock -> drop = -1
								}
								engine.ruleopt.harddropEnable&&!engine.ruleopt.harddropLock -> drop = 1
								engine.ruleopt.softdropEnable&&!engine.ruleopt.softdropLock -> drop = -1
							}
						}
						nowX>bestX -> moveDir = -1
						nowX<bestX -> moveDir = 1
					}
				}
			}/* Piece holdPiece = engine.holdPieceObject;
				 * if (holdPiece != null)
				 * input |= calcIRS(holdPiece, engine); */
			//Convert parameters to input
			if(moveDir==-1&&!ctrl.isPress(Controller.BUTTON_LEFT))
				input = input or Controller.BUTTON_BIT_LEFT
			else if(moveDir==1&&!ctrl.isPress(Controller.BUTTON_RIGHT)) input = input or Controller.BUTTON_BIT_RIGHT
			if(drop==1&&!ctrl.isPress(Controller.BUTTON_UP))
				input = input or Controller.BUTTON_BIT_UP
			else if(drop==-1) input = input or Controller.BUTTON_BIT_DOWN

			if(rotateDir!=0) {
				val defaultRotateRight = engine.owRotateButtonDefaultRight==1||engine.owRotateButtonDefaultRight==-1&&engine.ruleopt.rotateButtonDefaultRight

				when {
					engine.ruleopt.rotateButtonAllowDouble&&
						rotateDir==2&&!ctrl.isPress(Controller.BUTTON_E) -> input = input or Controller.BUTTON_BIT_E
					engine.ruleopt.rotateButtonAllowReverse&&
						!defaultRotateRight&&rotateDir==1 -> if(!ctrl.isPress(Controller.BUTTON_B)) input = input or Controller.BUTTON_BIT_B
					engine.ruleopt.rotateButtonAllowReverse&&
						defaultRotateRight&&rotateDir==-1 -> if(!ctrl.isPress(Controller.BUTTON_B)) input = input or Controller.BUTTON_BIT_B
					!ctrl.isPress(Controller.BUTTON_A) -> input = input or Controller.BUTTON_BIT_A
				}
			}

			if(DEBUG_ALL)
				log.debug("Input = $input, moveDir = $moveDir, rotateDir = $rotateDir, drop = $drop")

			delay = 0
			ctrl.buttonBit = input
		} else {
			delay++
			ctrl.buttonBit = inputARE
		}
	}

	private fun printPieceAndDirection(pieceType:Int, rt:Int) {
		var result = "Piece "
		when(pieceType) {
			Piece.PIECE_I -> result += "I"
			Piece.PIECE_L -> result += "L"
			Piece.PIECE_O -> result += "O"
			Piece.PIECE_Z -> result += "Z"
			Piece.PIECE_T -> result += "T"
			Piece.PIECE_J -> result += "J"
			Piece.PIECE_S -> result += "S"
			Piece.PIECE_I1 -> result += "I1"
			Piece.PIECE_I2 -> result += "I2"
			Piece.PIECE_I3 -> result += "I3"
			Piece.PIECE_L3 -> result += "L3"
		}
		result = "$result, direction "

		when(rt) {
			Piece.DIRECTION_LEFT -> result += "left"
			Piece.DIRECTION_DOWN -> result += "down"
			Piece.DIRECTION_UP -> result += "up"
			Piece.DIRECTION_RIGHT -> result += "right"
		}
		if(DEBUG_ALL) log.debug(result)
	}

	/* public int calcIRS(Piece piece, GameEngine engine)
	 * {
	 * piece = checkOffset(piece, engine);
	 * int nextType = piece.id;
	 * Field fld = engine.field;
	 * int spawnX = engine.getSpawnPosX(fld, piece);
	 * SpeedParam speed = engine.speed;
	 * boolean gravityHigh = speed.gravity > speed.denominator;
	 * int width = fld.getWidth();
	 * int midColumnX = (width/2)-1;
	 * if(Math.abs(spawnX - bestX) == 1)
	 * {
	 * if (bestRt == 1)
	 * {
	 * if (engine.ruleopt.rotateButtonDefaultRight)
	 * return Controller.BUTTON_BIT_A;
	 * else
	 * return Controller.BUTTON_BIT_B;
	 * }
	 * else if (bestRt == 3)
	 * {
	 * if (engine.ruleopt.rotateButtonDefaultRight)
	 * return Controller.BUTTON_BIT_B;
	 * else
	 * return Controller.BUTTON_BIT_A;
	 * }
	 * }
	 * else if (nextType == Piece.PIECE_L)
	 * {
	 * if (gravityHigh && fld.getHighestBlockY(midColumnX-1) <
	 * minOf(fld.getHighestBlockY(midColumnX),
	 * fld.getHighestBlockY(midColumnX+1)))
	 * return 0;
	 * else if (engine.ruleopt.rotateButtonDefaultRight)
	 * return Controller.BUTTON_BIT_B;
	 * else
	 * return Controller.BUTTON_BIT_A;
	 * }
	 * else if (nextType == Piece.PIECE_J)
	 * {
	 * if (gravityHigh && fld.getHighestBlockY(midColumnX+1) <
	 * minOf(fld.getHighestBlockY(midColumnX),
	 * fld.getHighestBlockY(midColumnX-1)))
	 * return 0;
	 * if (engine.ruleopt.rotateButtonDefaultRight)
	 * return Controller.BUTTON_BIT_A;
	 * else
	 * return Controller.BUTTON_BIT_B;
	 * }
	 * //else if (nextType == Piece.PIECE_I)
	 * // return Controller.BUTTON_BIT_A;
	 * return 0;
	 * } */

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
		bestRtSub = 0
		bestPts = Integer.MIN_VALUE
		thinkSuccess = false

		val fld:Field = engine.field?.let{Field(it)}
			?:Field(engine.fieldWidth, engine.fieldHeight, engine.fieldHiddenHeight, engine.ruleopt.fieldCeiling)
		var pieceNow = engine.nowPieceObject
		var pieceHold = engine.holdPieceObject
		val holdBoxEmpty = pieceHold==null
		var nextIndex = engine.nextPieceCount
		if(inARE||pieceNow==null) {
			pieceNow = engine.getNextObjectCopy(nextIndex)
			nextIndex++
		}
		pieceHold = if(holdBoxEmpty)
			engine.getNextObjectCopy(nextIndex)
		else engine.getNextObjectCopy(engine.nextPieceCount)
		pieceNow = checkOffset(pieceNow, engine)
		pieceHold = checkOffset(pieceHold, engine)
		val holdOK = engine.isHoldOK
		var holdID = -1
		if(engine.holdPieceObject!=null) holdID = engine.holdPieceObject!!.id

		nextQueueIDs = IntArray(MAX_THINK_DEPTH){engine.getNextID(nextIndex+it)}

		val state = fieldToIndex(fld)
		if(state<0) {
			thinkLastPieceNo++
			return
		}
		var t:Transition? = moves!![state][pieceNow.id]

		while(t!=null) {
			val pts = thinkMain(engine, t.newField, holdID, 0)

			if(pts>bestPts) {
				bestPts = pts
				bestX = t.x+3
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
					bestX = t.x+3
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
				if(pieceTemp.checkCollision(bestX, bestY, newRt, fld)&&engine.wallkick!=null&&
					engine.ruleopt.rotateWallkick) {
					val kick = engine.wallkick!!.executeWallkick(bestX, bestY, -1, bestRt, newRt,
						engine.ruleopt.rotateMaxUpwardWallkick!=0, pieceTemp, fld, null!!)

					if(kick!=null) {
						bestX += kick.offsetX
						bestY = pieceTemp.getBottom(bestX, bestY+kick.offsetY, newRt, fld)
					}
				}
				bestRtSub = bestRt
				bestRt = newRt
			} else
				bestRtSub = bestRt
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
			if(holdID==Piece.PIECE_I)
				result += 1000
			else if(holdID>=0&&holdID<pieceScores.size) result += pieceScores[holdID]*100/28
			return result
		}

		var bestPts = 0
		var t:Transition? = moves!![state][nextQueueIDs[depth]]

		while(t!=null) {
			bestPts = maxOf(bestPts, thinkMain(engine, t.newField, holdID, depth+1)+1000)
			t = t.next
		}

		if(engine.ruleopt.holdEnable)
			if(holdID==-1)
				bestPts = maxOf(bestPts, thinkMain(engine, state, nextQueueIDs[depth], depth+1))
			else {
				t = moves!![state][holdID]
				while(t!=null) {
					bestPts = maxOf(bestPts, thinkMain(engine, t.newField, nextQueueIDs[depth], depth+1)+1000)
					t = t.next
				}
			}

		return bestPts
	}

	/* Processing of the thread */
	override fun run() {
		log.info("ComboRaceBot: Thread start")
		threadRunning = true

		while(threadRunning) {
			try {
				synchronized(thinkRequest) {
					if(!thinkRequest.active&&!thinkRequest.createTablesRequest) thinkRequest.wait()
				}
			} catch(e:InterruptedException) {
				log.debug("ComboRaceBot: InterruptedException waiting for thinkRequest signal")
			}

			if(thinkRequest.active) {
				thinkRequest.active = false
				thinking = true
				try {
					thinkBestPosition(gEngine, gEngine.playerID)
					thinkComplete = true
					//log.debug("ComboRaceBot: thinkBestPosition completed successfully");
				} catch(e:Throwable) {
					log.debug("ComboRaceBot: thinkBestPosition Failed", e)
				}

				thinking = false
			} else if(thinkRequest.createTablesRequest) createTables(gEngine)

			if(thinkDelay>0)
				try {
					Thread.sleep(thinkDelay.toLong())
				} catch(e:InterruptedException) {
					break
				}

		}

		threadRunning = false
		log.info("ComboRaceBot: Thread end")
	}

	/** Constructs the moves table if necessary. */
	private fun createTables(engine:GameEngine) {
		if(moves!=null) return

		moves = Array(FIELDS.size) {emptyArray<Transition>()}

		val fldEmpty = Field(4, Field.DEFAULT_HEIGHT, Field.DEFAULT_HIDDEN_HEIGHT)
		val fldBackup = Field(fldEmpty)
		val fldTemp = Field(fldEmpty)

		val pieces = Array(7){
			checkOffset(Piece(it), engine).apply{setColor(1)}
		}

		var count = 0

		for(i in FIELDS.indices) {
			fldBackup.copy(fldEmpty)
			var code = FIELDS[i]

			for(y in Field.DEFAULT_HEIGHT-1 downTo Field.DEFAULT_HEIGHT-4+1)
				for(x in 3 downTo 0) {
					if(code and 1==1) fldBackup.setBlockColor(x, y, 1)
					code = code shr 1
				}

			for(p in 0..6) {
				val tempX = engine.getSpawnPosX(fldBackup, pieces[p])
				for(rt in 0 until Piece.DIRECTION_COUNT) {
					val minX = pieces[p].getMostMovableLeft(tempX, 0, rt, fldBackup)
					val maxX = pieces[p].getMostMovableRight(tempX, 0, rt, fldBackup)

					for(x in minX..maxX) {
						val y = pieces[p].getBottom(x, 0, rt, fldBackup)
						if(p==Piece.PIECE_L||p==Piece.PIECE_T||p==Piece.PIECE_J||rt<2) {
							fldTemp.copy(fldBackup)
							pieces[p].placeToField(x, y, rt, fldTemp)
							if(fldTemp.checkLine()==1) {
								fldTemp.clearLine()
								fldTemp.downFloatingBlocks()
								val index = fieldToIndex(fldTemp, 0)
								if(index>=0) {
									moves!![i][p] = Transition(x, rt, index, moves!![i][p])
									count++
								}
							}
							if(p==Piece.PIECE_O) continue
						}

						// Left rotation
						if(!engine.ruleopt.rotateButtonDefaultRight||engine.ruleopt.rotateButtonAllowReverse) {
							val rot = pieces[p].getRotateDirection(-1, rt)
							var newX = x
							var newY = y
							fldTemp.copy(fldBackup)

							if(pieces[p].checkCollision(x, y, rot, fldTemp)&&engine.wallkick!=null&&
								engine.ruleopt.rotateWallkick) {
								engine.wallkick!!.executeWallkick(x, y, -1, rt, rot,
									engine.ruleopt.rotateMaxUpwardWallkick!=0, pieces[p], fldTemp, null!!)?.let{kick->
									newX = x+kick.offsetX
									newY = pieces[p].getBottom(newX, y+kick.offsetY, rot, fldTemp)
								}
							}
							if(!pieces[p].checkCollision(newX, newY, rot, fldTemp)&&newY>pieces[p].getBottom(newX, 0, rot, fldTemp)) {
								pieces[p].placeToField(newX, newY, rot, fldTemp)
								if(fldTemp.checkLine()==1) {
									fldTemp.clearLine()
									fldTemp.downFloatingBlocks()
									val index = fieldToIndex(fldTemp, 0)
									if(index>=0) {
										moves!![i][p] = Transition(x, rt, -1, index, moves!![i][p])
										count++
									}
								}
							}
						}

						// Right rotation
						if(engine.ruleopt.rotateButtonDefaultRight||engine.ruleopt.rotateButtonAllowReverse) {
							val rot = pieces[p].getRotateDirection(1, rt)
							var newX = x
							var newY = y
							fldTemp.copy(fldBackup)

							if(pieces[p].checkCollision(x, y, rot, fldTemp)&&engine.wallkick!=null&&
								engine.ruleopt.rotateWallkick) {
								engine.wallkick!!.executeWallkick(x, y, 1, rt, rot,
									engine.ruleopt.rotateMaxUpwardWallkick!=0, pieces[p], fldTemp, null!!)?.let{kick->
									newX = x+kick.offsetX
									newY = pieces[p].getBottom(newX, y+kick.offsetY, rot, fldTemp)
								}
							}
							if(!pieces[p].checkCollision(newX, newY, rot, fldTemp)&&newY>pieces[p].getBottom(newX, 0, rot, fldTemp)) {
								pieces[p].placeToField(newX, newY, rot, fldTemp)
								if(fldTemp.checkLine()==1) {
									fldTemp.clearLine()
									fldTemp.downFloatingBlocks()
									val index = fieldToIndex(fldTemp, 0)
									if(index>=0) {
										moves!![i][p] = Transition(x, rt, 1, index, moves!![i][p])
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

	/**
	 * Called to display internal state
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	override fun renderState(engine:GameEngine, playerID:Int) {
		val r = engine.owner.receiver
		r.drawScoreFont(engine, playerID, 19, 33, name.toUpperCase(), COLOR.GREEN, .5f)
		r.drawScoreFont(engine, playerID, 24, 34, "X", COLOR.BLUE, .5f)
		r.drawScoreFont(engine, playerID, 27, 34, "Y", COLOR.BLUE, .5f)
		r.drawScoreFont(engine, playerID, 30, 34, "RT", COLOR.BLUE, .5f)
		r.drawScoreFont(engine, playerID, 19, 35, "BEST:", COLOR.BLUE, .5f)
		r.drawScoreFont(engine, playerID, 24, 35, "$bestX", !thinkSuccess, .5f)
		r.drawScoreFont(engine, playerID, 27, 35, "$bestY", !thinkSuccess, .5f)
		r.drawScoreFont(engine, playerID, 30, 35, "$bestRt", !thinkSuccess, .5f)
		r.drawScoreFont(engine, playerID, 19, 36, "SUB:", COLOR.BLUE, .5f)
		if(engine.aiShowHint) {
			r.drawScoreFont(engine, playerID, 24, 36, "$bestXSub", !thinkSuccess, .5f)
			r.drawScoreFont(engine, playerID, 27, 36, "$bestYSub", !thinkSuccess, .5f)
		}
		r.drawScoreFont(engine, playerID, 30, 36, "$bestRtSub", !thinkSuccess, .5f)
		r.drawScoreFont(engine, playerID, 19, 37, "NOW:", COLOR.BLUE, .5f)
		engine.nowPieceObject?.let{
			r.drawScoreFont(engine, playerID, 24, 37, engine.nowPieceX.toString(), scale=.5f)
			r.drawScoreFont(engine, playerID, 27, 37, engine.nowPieceY.toString(), scale=.5f)
			r.drawScoreFont(engine, playerID, 30, 37, it.direction.toString(), scale=.5f)
		} ?:
			r.drawScoreFont(engine, playerID, 24, 37, "-- -- --", scale=.5f)
		r.drawScoreFont(engine, playerID, 19, 38, "MOVE SCORE:", COLOR.BLUE, .5f)
		val scoreColor = when {
			bestPts<(MAX_THINK_DEPTH-2)*1000 -> COLOR.RED
			bestPts<(MAX_THINK_DEPTH-1)*1000 -> COLOR.ORANGE
			bestPts<MAX_THINK_DEPTH*1000 -> COLOR.YELLOW
			else -> COLOR.GREEN
		}
		r.drawScoreFont(engine, playerID, 31, 38, "$bestPts", scoreColor, .5f)
		r.drawScoreFont(engine, playerID, 19, 39, "THINK ACTIVE:", COLOR.BLUE, .5f)
		r.drawScoreFont(engine, playerID, 32, 39, GeneralUtil.getOorX(thinking), .5f)
		r.drawScoreFont(engine, playerID, 19, 40, "THINK REQUEST:", COLOR.BLUE, .5f)
		r.drawScoreFont(engine, playerID, 33, 40, GeneralUtil.getOorX(thinkRequest.active), .5f)
		r.drawScoreFont(engine, playerID, 19, 41, "THINK SUCCESS:", if(thinkSuccess)
			COLOR.BLUE else COLOR.RED, .5f)
		r.drawScoreFont(engine, playerID, 33, 41, GeneralUtil.getOorX(thinkSuccess), !thinkSuccess, .5f)
		r.drawScoreFont(engine, playerID, 19, 42, "THINK COMPLETE:", COLOR.BLUE, .5f)
		r.drawScoreFont(engine, playerID, 34, 42, GeneralUtil.getOorX(thinkComplete), .5f)
		r.drawScoreFont(engine, playerID, 19, 43, "IN ARE:", COLOR.BLUE, .5f)
		r.drawScoreFont(engine, playerID, 26, 43, GeneralUtil.getOorX(inARE), .5f)
		r.drawScoreFont(engine, playerID, 19, 44, "QUEUE:", COLOR.BLUE, .5f)
		if(nextQueueIDs==null)
			for(i in 0 until MAX_THINK_DEPTH)
				r.drawScoreFont(engine, playerID, 25+i, 44, "-", .5f)
		else {
			var color = COLOR.GREEN
			for(i in nextQueueIDs.indices) {
				if(i>=bestPts/1000&&color!=COLOR.RED)
					color = if(i<nextQueueIDs.size-1&&thinkComplete) COLOR.RED else COLOR.YELLOW
				r.drawScoreFont(engine, playerID, 25+i, 44, Piece.Shape.names[nextQueueIDs[i]], color, .5f)
			}
		}
		var code = -1
		engine.field?.let{code = fieldToCode(it)}
		r.drawScoreFont(engine, playerID, 19, 45, "STATE:", COLOR.BLUE, .5f)
		r.drawScoreFont(engine, playerID, 25, 45, if(code==-1) "---" else Integer.toHexString(code).toUpperCase(), .5f)

	}

	override fun renderHint(engine:GameEngine, playerID:Int) {
		val r = engine.owner.receiver
		r.drawScoreFont(engine, playerID, 10, 3, "AI HINT MOVE:", COLOR.GREEN)
		if(bestPts>0&&(thinkComplete||thinkCurrentPieceNo in 1..thinkLastPieceNo))
			if(bestHold&&thinkComplete&&engine.isHoldOK)
				r.drawScoreFont(engine, playerID, 10, 4, "HOLD")
			else {
				val pieceNow = engine.nowPieceObject
				val fld = engine.field
				if(fld==null||pieceNow==null) return
				val nowX = engine.nowPieceX
				val nowY = engine.nowPieceY
				val rt = pieceNow.direction
				val pieceTouchGround = pieceNow.checkCollision(nowX, nowY+1, fld)
				if(pieceTouchGround&&nowX==bestX&&nowY==bestY&&rt==bestRt)
					return
				else if(pieceTouchGround&&nowX==bestXSub&&nowY==bestYSub&&rt==bestRtSub) {
					val rotateDir:Int //-1 = left,  1 = right, 2 = 180
					val best180 = abs(rt-bestRt)==2
					//if (DEBUG_ALL) log.debug("Case 1 rotation");

					val lrot = engine.getRotateDirection(-1)
					val rrot = engine.getRotateDirection(1)
					if(DEBUG_ALL) log.debug("lrot = $lrot, rrot = $rrot")

					rotateDir = when {
						best180&&engine.ruleopt.rotateButtonAllowDouble -> 2
						bestRt==rrot -> 1
						bestRt==lrot -> -1
						engine.ruleopt.rotateButtonAllowReverse&&best180&&rt and 1==1 ->
							if(rrot==Piece.DIRECTION_UP) 1 else -1
						else -> 1
					}
					if(rotateDir==-1)
						r.drawScoreFont(engine, playerID, 10, 4, "ROTATE LEFT")
					else if(rotateDir==1)
						r.drawScoreFont(engine, playerID, 10, 4, "ROTATE RIGHT")
					else if(rotateDir==2) r.drawScoreFont(engine, playerID, 10, 4, "ROTATE 180")
					return
				}

				var moveDir = 0 //-1 = left,  1 = right
				var rotateDir = 0 //-1 = left,  1 = right, 2 = 180
				var drop = 0 //1 = up, -1 = down
				var writeY = 4

				if(rt!=bestRtSub) {
					val best180 = abs(rt-bestRtSub)==2
					//if (DEBUG_ALL) log.debug("Case 1 rotation");

					val lrot = engine.getRotateDirection(-1)
					val rrot = engine.getRotateDirection(1)
					if(DEBUG_ALL) log.debug("lrot = $lrot, rrot = $rrot")

					rotateDir = when {
						best180&&engine.ruleopt.rotateButtonAllowDouble -> 2
						bestRtSub==rrot -> 1
						bestRtSub==lrot -> -1
						engine.ruleopt.rotateButtonAllowReverse&&best180&&rt and 1==1 ->
							if(rrot==Piece.DIRECTION_UP) 1 else -1
						else -> 1
					}
					r.drawScoreFont(engine, playerID, 10, writeY, when(rotateDir) {
						-1 -> "ROTATE LEFT"
						1 -> "ROTATE RIGHT"
						else -> "ROTATE 180"
					})
					writeY++
				}

				// Whether reachable position
				val minX = pieceNow.getMostMovableLeft(nowX, nowY, rt, fld)
				val maxX = pieceNow.getMostMovableRight(nowX, nowY, rt, fld)

				if(movestate==0&&rt==bestRtSub
					&&(bestXSub<minX-1||bestXSub>maxX+1||bestYSub<nowY)) {
					// Again because it is thought unreachable
					//thinkBestPosition(engine, playerID);
					thinkComplete = false
					//thinkCurrentPieceNo++;
					//System.out.println("rethink c:" + thinkCurrentPieceNo + " l:" + thinkLastPieceNo);
					if(DEBUG_ALL) log.debug("Needs rethink - cannot reach desired position")
					thinkRequest.newRequest()
				} else if((nowX==bestXSub||movestate>0)&&rt==bestRtSub) {
					moveDir = 0
					// Funnel
					if(bestRtSub==bestRt) {
						if(pieceTouchGround&&engine.ruleopt.softdropLock)
							drop = -1
						else if(engine.ruleopt.harddropEnable)
							drop = 1
						else if(engine.ruleopt.softdropEnable||engine.ruleopt.softdropLock) drop = -1
					} else if(engine.ruleopt.harddropEnable&&!engine.ruleopt.harddropLock)
						drop = 1
					else if(engine.ruleopt.softdropEnable&&!engine.ruleopt.softdropLock) drop = -1
				} else if(nowX>bestXSub)
					moveDir = -1
				else if(nowX<bestXSub) moveDir = 1
				if(moveDir!=0) {
					if(moveDir==-1)
						r.drawScoreFont(engine, playerID, 10, writeY, "MOVE LEFT")
					else if(moveDir==1) r.drawScoreFont(engine, playerID, 10, writeY, "MOVE RIGHT")
					writeY++
				}
				if(drop!=0) {
					if(drop==-1)
						r.drawScoreFont(engine, playerID, 10, writeY, "SOFT DROP")
					else if(drop==1) r.drawScoreFont(engine, playerID, 10, writeY, "HARD DROP")
					writeY++
				}
			}
	}

	private class Transition
	constructor(val x:Int, val rt:Int,val rtSub:Int = 0,val newField:Int,val next:Transition) {
		constructor(bestX:Int, bestRt:Int, newFld:Int, nxt:Transition):this(bestX,bestRt,0,newFld,nxt)
	}

	//Wrapper for think requests
	private class ThinkRequestMutex:java.lang.Object() {
		var active:Boolean = false
		var createTablesRequest:Boolean = false

		init {
			active = false
			createTablesRequest = false
		}

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
		/** Log */
		internal val log = Logger.getLogger(ComboRaceBot::class.java)

		/**
		 * List of field state codes which are possible to sustain a stable
		 * combo
		 */
		private val FIELDS = intArrayOf(0x7, 0xB, 0xD, 0xE, 0x13, 0x15, 0x16, 0x19, 0x1A, 0x1C, 0x23, 0x29, 0x31, 0x32, 0x49, 0x4C, 0x61, 0x68, 0x83, 0x85, 0x86, 0x89, 0x8A, 0x8C, 0xC4, 0xC8, 0x111, 0x888)
		/** Number of pieces to think ahead */
		private const val MAX_THINK_DEPTH = 6
		/** Set to true to print debug information */
		private const val DEBUG_ALL = false

		fun checkOffset(p:Piece?, engine:GameEngine):Piece {
			val result = Piece(p!!)
			result.big = engine.big
			if(!p.offsetApplied)
				result.applyOffsetArray(engine.ruleopt.pieceOffsetX[p.id], engine.ruleopt.pieceOffsetY[p.id])
			return result
		}

		/**
		 * Converts field to field state int code
		 * @param field Field object
		 * @param valleyX Leftmost x-coordinate of 4-block-wide valley to combo in
		 * @return Field state int code.
		 */
		fun fieldToCode(field:Field, valleyX:Int = 3):Int {
			val height = field.height
			var result = 0
			for(y in height-3 until height)
				for(x in 0..3) {
					result = result.shl(1)
					if(!field.getBlockEmptyF(x+valleyX, y)) result++
				}
			return result
		}

		/**
		 * Converts field state int code to FIELDS array index
		 * @param field Field state int code
		 * @return State index if found; -1 if not found.
		 */
		private fun fieldToIndex(field:Int):Int {
			var min = 0
			var max = FIELDS.size-1
			var mid:Int
			while(min<=max) {
				mid = min+max shr 1
				when {
					FIELDS[mid]>field -> max = mid-1
					FIELDS[mid]<field -> min = mid+1
					else -> return mid
				}
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

	}
}
