/*
 * Copyright (c) 2021-2021,
 * This library class was created by 0xFC963F18DC21 / Shots243
 * It is part of an extension library for the game NullpoMino (copyright 2021-2021)
 *
 * Kotlin converted and modified by Venom=Nhelv
 *
 * Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 * Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.
 *
 * THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 *
 * Repository: https://github.com/Shots243/ModePile
 *
 * When using this library in a mode / library pack of your own, the following
 * conditions must be satisfied:
 *     - This license must remain visible at the top of the document, unmodified.
 *     - You are allowed to use this library for any modding purpose.
 *         - If this is the case, the Library Creator must be credited somewhere.
 *             - Source comments only are fine, but in a README is recommended.
 *     - Modification of this library is allowed, but only in the condition that a
 *       pull request is made to merge the changes to the repository.
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

package zeroxfc.nullpo.custom.modes

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.AbstractMode
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import zeroxfc.nullpo.custom.libs.ProfileProperties
import zeroxfc.nullpo.custom.modes.objects.minesweeper.GameGrid
import kotlin.math.min
import kotlin.random.Random

class Minesweeper:AbstractMode() {
	private var mainGrid:GameGrid = GameGrid()
	private var length = 0
	private var height = 0
	private var cursorX = 0
	private var cursorY = 0
	private var cursorScreenX = 0
	private var cursorScreenY = 0
	private var offsetX = 0
	private var offsetY = 0
	private var blockGrid:Array<Array<Block?>> = emptyArray()
	private var minePercentage = 0f
	private var firstClick = false
	private var localRand:Random? = null
	private var bgm = 0
	private var bg = 0
	private var timeLimit = 0
	private var currentLimit = 0
	private var numberOfCover = 0
	override val name:String get() = "MineSweeper"

	override fun playerInit(engine:GameEngine, playerID:Int) {
		//SoundLoader.loadSoundset(SoundLoader.LOADTYPE_MINESWEEPER)
		owner = engine.owner
		length = 0
		height = 0
		cursorX = 0
		cursorY = 0
		offsetX = 0
		offsetY = 0
		cursorScreenX = 0
		cursorScreenY = 0
		firstClick = true
		minePercentage = 0f
		bgm = 0
		bg = 0
		timeLimit = 0
		currentLimit = 0
		numberOfCover = 0
		engine.framecolor = GameEngine.FRAME_COLOR_BLUE
		engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_SAMECOLOR
		mainGrid = GameGrid()
		blockGrid = emptyArray()
		if(!owner.replayMode) loadSetting(owner.modeConfig, engine)
		else loadSetting(owner.replayProp, engine)

		engine.owner.backgroundStatus.bg = bg
	}

	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		mainGrid = GameGrid()

		// Menu
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change:Int = updateCursor(engine, 5, playerID)
			if(change!=0) {
				engine.playSE("change")
				when(engine.statc[2]) {
					0 -> {
						length += change
						if(length>256) length = 4
						if(length<4) length = 256
					}
					1 -> {
						height += change
						if(height>256) height = 5
						if(height<4) height = 256
					}
					2 -> {
						minePercentage += change*0.1f
						if(minePercentage>99.9f) minePercentage = 0.1f
						if(minePercentage<0.1f||(length*height*(minePercentage/100f)).toInt()==0) minePercentage = 99.9f
					}
					3 -> {
						timeLimit += 60*change
						if(timeLimit>36000) timeLimit = 0
						if(timeLimit<0) timeLimit = 36000
					}
					4 -> {
						bgm += change
						if(bgm>15) bgm = 0
						if(bgm<0) bgm = 15
					}
					5 -> {
						bg += change
						if(bg>19) bg = 0
						if(bg<0) bg = 19
						engine.owner.backgroundStatus.bg = bg
					}
				}
			}
			engine.owner.backgroundStatus.bg = bg

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&engine.statc[3]>=5) {
				engine.playSE("decide")
				if(engine.playerProp.isLoggedIn) {
					saveSettingPlayer(engine.playerProp)
					engine.playerProp.saveProfileConfig()
				} else {
					saveSetting(owner.modeConfig, engine)
					owner.saveModeConfig()
				}
				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) {
				engine.quitflag = true
			}

			// New acc
			if(engine.ctrl.isPush(Controller.BUTTON_E)&&engine.ai==null) {
				engine.playSE("decide")
				engine.stat = GameEngine.Status.CUSTOM
				engine.resetStatc()
				return true
			}
			engine.statc[3]++
		} else {
			engine.statc[3]++
			engine.statc[2] = -1
			return engine.statc[3]<60
		}
		return true
	}

	override fun renderSetting(engine:GameEngine, playerID:Int) {
		drawMenu(engine, playerID, receiver, 0, COLOR.BLUE, 0, "WIDTH" to length, "HEIGHT" to height)
		drawMenu(engine, playerID, receiver, 4, COLOR.RED, 2,
			"MINE%" to String.format("%.2f", minePercentage),
			"TIME LIMIT" to if(timeLimit>0) timeLimit.toTimeStr else "DISABLED")
		drawMenuBGM(engine, playerID, receiver, bgm, COLOR.GREEN)
		drawMenu(engine, playerID, receiver, 10, COLOR.GREEN, 5, "BACKGROUND" to bg)
	}

	override fun onLast(engine:GameEngine, playerID:Int) {
		super.onLast(engine, playerID)
		if(engine.quitflag) {
			engine.playerProp.reset()
		}
	}

	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		// 横溜め
		if(engine.ruleOpt.dasInReady&&engine.gameActive) engine.padRepeat() else if(engine.ruleOpt.dasRedirectInDelay) {
			engine.dasRedirect()
		}

		// Initialization
		if(engine.statc[0]==0) {
			// fieldInitialization
			blockGrid = Array(height) {arrayOfNulls(length)}
			for(y in blockGrid.indices) {
				for(x in 0 until blockGrid[y].size) {
					blockGrid[y][x] = Block()
				}
			}
			cursorX = 0
			cursorY = 0
			offsetX = 0
			offsetY = 0
			cursorScreenX = 0
			cursorScreenY = 0
			currentLimit = timeLimit
			firstClick = true
			localRand = Random(engine.randSeed)
			mainGrid = GameGrid(length, height, minePercentage, engine.randSeed)
			engine.ruleOpt.fieldWidth = min(length, MAX_DIM)
			engine.ruleOpt.fieldHeight = min(height, MAX_DIM)
			engine.ruleOpt.fieldHiddenHeight = 0
			engine.fieldWidth = engine.ruleOpt.fieldWidth
			engine.fieldHeight = engine.ruleOpt.fieldHeight
			engine.fieldHiddenHeight = engine.ruleOpt.fieldHiddenHeight
			engine.field = Field(engine.fieldWidth, engine.fieldHeight, engine.fieldHiddenHeight, engine.ruleOpt.fieldCeiling)
			if(!engine.readyDone) {
				//  button input状態リセット
				engine.ctrl.reset()
				// ゲーム中 flagON
				engine.gameActive = true
				engine.gameStarted = true
				engine.isInGame = true
			}
		}

		// READY音
		if(engine.statc[0]==engine.readyStart) engine.playSE("ready")

		// GO音
		if(engine.statc[0]==engine.goStart) engine.playSE("go")

		// 開始
		if(engine.statc[0]>=engine.goEnd) {
			if(!engine.readyDone) engine.owner.bgmStatus.bgm = BGMStatus.BGM.values[bgm]
			engine.owner.mode?.startGame(engine, playerID)
			engine.owner.receiver.startGame(engine, playerID)
			engine.stat = GameEngine.Status.CUSTOM
			engine.resetStatc()
			if(!engine.readyDone) {
				engine.startTime = System.nanoTime()
				//startTime = System.nanoTime()/1000000L;
			}
			engine.readyDone = true
			return true
		}
		engine.statc[0]++
		return true
	}

	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		val field = engine.field
		if(engine.lives<=0)
			when {
				engine.statc[0]==0 -> {
					// もう復活できないとき
					engine.gameEnded()
					engine.blockShowOutlineOnly = false
					if(owner.players<2) owner.bgmStatus.bgm = BGMStatus.BGM.Silent
					if(field.isEmpty) engine.statc[0] = field.height+1 else engine.resetFieldVisible()
				}
				engine.statc[0]<field.height+1 -> {
					for(i in 0 until field.width)
						field.getBlock(i, field.height-engine.statc[0])?.let {blk ->
							val covered = Block(Block.COLOR.BLUE, engine.skin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
							if(blk.toChar()==covered.toChar()) {
								if(!blk.getAttribute(Block.ATTRIBUTE.GARBAGE)) {
									blk.color = Block.COLOR.WHITE
									blk.setAttribute(true, Block.ATTRIBUTE.GARBAGE)
								}
								blk.darkness = 0.9f
								blk.elapsedFrames = -1
							}
						}
					engine.statc[0]++
				}
				engine.statc[0]==field.height+1 -> {
					engine.playSE("gameover")
					engine.statc[0]++
				}
				engine.statc[0]<field.height+1+180 -> {
					if(engine.statc[0]>=field.height+1+60&&engine.ctrl.isPush(Controller.BUTTON_A)) {
						engine.statc[0] = field.height+1+180
					}
					engine.statc[0]++
				}
				else -> {
					if(!owner.replayMode||owner.replayRerecord) owner.saveReplay()
					for(i in 0 until owner.players) {
						if(i==playerID||engine.gameoverAll) {
							owner.engine[i].field.reset()
							owner.engine[i].resetStatc()
							owner.engine[i].stat = GameEngine.Status.RESULT
						}
					}
				}
			} else {
			// 復活できるとき
			if(engine.statc[0]==0) {
				engine.blockShowOutlineOnly = false
				engine.playSE("died")
				engine.resetFieldVisible()
				for(i in field.hiddenHeight*-1 until field.height)
					for(j in 0 until field.width)
						field.getBlock(j, i)?.apply {color = Block.COLOR.BLACK}

				engine.statc[0] = 1
			}
			if(!field.isEmpty) {
				field.pushDown()
			} else if(engine.statc[1]<engine.are) {
				engine.statc[1]++
			} else {
				engine.lives--
				engine.resetStatc()
				engine.stat = GameEngine.Status.CUSTOM
			}
		}
		return true
	}

	private fun caughtMine(engine:GameEngine) {
		val dummyBlock = Block(Block.COLOR.RED, Block.TYPE.GEM)
		if(!engine.gameActive) return
		engine.playSE("bomb")
		receiver.blockBreak(engine, cursorX, cursorY, dummyBlock)
		mainGrid.uncoverAllMines()
		updateBlockGrid(engine)
		updateEngineGrid(engine)
		for(y in 0 until minOf(MAX_DIM, height))
			for(x in 0 until minOf(MAX_DIM, length)) {
				val blk = engine.field.getBlock(x, y)
				if(blk?.color==Block.COLOR.RED&&blk.isGemBlock)
					receiver.blockBreak(engine, x, y, dummyBlock)
			}
		//				for (int y = 0; y < height; y++) {
		//					for (int x = 0; x < length; x++) {
		//						if (mainGrid.getSquareAt(x, y).isMine) engine.field.getBlock(x, y).copy(mine);
		//					}
		//				}
		engine.gameEnded()
		engine.resetStatc()
		engine.stat = GameEngine.Status.GAMEOVER
	}

	override fun onCustom(engine:GameEngine, playerID:Int):Boolean {
		if(engine.gameActive) {
			// Block mine = new Block(Block.COLOR.GEM_RED, engine.getSkin(), Block.ATTRIBUTE.VISIBLE);
			// Block open = new Block(Block.COLOR.WHITE, engine.getSkin(), Block.ATTRIBUTE.VISIBLE);
			val dummyBlock = Block(Block.COLOR.RED, Block.TYPE.GEM)
			var changeX = 0
			var changeY = 0
			if(engine.ctrl.isPress(Controller.BUTTON_LEFT)) {
				if(engine.ctrl.isPush(Controller.BUTTON_LEFT)||engine.ctrl.isPress(Controller.BUTTON_E)) {
					cursorX -= 1
					changeX += -1
					if(cursorX<0) cursorX = length-1
					engine.playSE("change")
				}
			}
			if(engine.ctrl.isPress(Controller.BUTTON_RIGHT)) {
				if(engine.ctrl.isPush(Controller.BUTTON_RIGHT)||engine.ctrl.isPress(Controller.BUTTON_E)) {
					cursorX += 1
					changeX += 1
					if(cursorX>=length) cursorX = 0
					engine.playSE("change")
				}
			}
			if(engine.ctrl.isPress(Controller.BUTTON_UP)) {
				if(engine.ctrl.isPush(Controller.BUTTON_UP)||engine.ctrl.isPress(Controller.BUTTON_E)) {
					cursorY -= 1
					changeY += -1
					if(cursorY<0) cursorY = height-1
					engine.playSE("change")
				}
			}
			if(engine.ctrl.isPress(Controller.BUTTON_DOWN)) {
				if(engine.ctrl.isPush(Controller.BUTTON_DOWN)||engine.ctrl.isPress(Controller.BUTTON_E)) {
					cursorY += 1
					changeY += 1
					if(cursorY>=height) cursorY = 0
					engine.playSE("change")
				}
			}
			cursorScreenX += changeX
			cursorScreenY += changeY
			if(length>20) {
				when {
					cursorX==0 -> {
						cursorScreenX = 0
						offsetX = 0
					}
					cursorX==length-1 -> {
						cursorScreenX = MAX_DIM-1
						offsetX = length-MAX_DIM
					}
					cursorScreenX<0 -> {
						cursorScreenX = 0
						offsetX--
					}
					cursorScreenX>=MAX_DIM -> {
						cursorScreenX = MAX_DIM-1
						offsetX++
					}
				}
			} else {
				cursorScreenX = cursorX
			}
			if(height>20) {
				when {
					cursorY==0 -> {
						cursorScreenY = 0
						offsetY = 0
					}
					cursorY==height-1 -> {
						cursorScreenY = MAX_DIM-1
						offsetY = height-MAX_DIM
					}
					cursorScreenY<0 -> {
						cursorScreenY = 0
						offsetY--
					}
					cursorScreenY>=MAX_DIM -> {
						cursorScreenY = MAX_DIM-1
						offsetY++
					}
				}
			} else {
				cursorScreenY = cursorY
			}
			// IN ORDER: cycle state, uncover, chord uncover, chord flag
			if(engine.ctrl.isPush(Controller.BUTTON_B)) {
				engine.playSE(if(mainGrid.cycleState(cursorX, cursorY)) "hold" else "holdfail")
			} else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				if(firstClick) {
					firstClick = false
					mainGrid.generateMines(cursorX, cursorY)
				}
				when(mainGrid.uncoverAt(cursorX, cursorY)) {
					GameGrid.State.MINE -> {
						caughtMine(engine)
						return true
					}
					GameGrid.State.SAFE -> {
						receiver.blockBreak(engine, cursorX, cursorY, Block(Block.COLOR.WHITE))
						if(numberOfCover>mainGrid.coveredSquares&&timeLimit>0) {
							val bonusCount:Int = numberOfCover-mainGrid.coveredSquares
							currentLimit += TIMEBONUS_SQUARE*bonusCount
							numberOfCover = mainGrid.coveredSquares
						}
						engine.playSE("erase0")
					}
					else -> engine.playSE("holdfail")
				}
			} else if(engine.ctrl.isPush(Controller.BUTTON_C)) {
				if(!firstClick&&mainGrid.getSurroundingMines(cursorX, cursorY)>0&&
					mainGrid.getSquareAt(cursorX, cursorY).uncovered&&
					mainGrid.getSurroundingFlags(cursorX, cursorY)==mainGrid.getSurroundingMines(cursorX, cursorY)) {
					val testLocations = arrayOf(intArrayOf(-1, -1), intArrayOf(0, -1), intArrayOf(1, -1), intArrayOf(-1, 0),
						intArrayOf(1, 0), intArrayOf(-1, 1), intArrayOf(0, 1), intArrayOf(1, 1))
					for(loc in testLocations) {
						receiver.blockBreak(engine, cursorX, cursorY, Block(Block.COLOR.WHITE))
						val px = cursorX+loc[0]
						val py = cursorY+loc[1]
						if(px<0||px>=length) continue
						if(py<0||py>=height) continue
						if(mainGrid.uncoverAt(px, py)==GameGrid.State.MINE) {
							caughtMine(engine)
							return true
						}
					}
					if(numberOfCover>mainGrid.coveredSquares&&timeLimit>0) {
						val bonusCount:Int = numberOfCover-mainGrid.coveredSquares
						currentLimit += TIMEBONUS_SQUARE*bonusCount
						numberOfCover = mainGrid.coveredSquares
					}
					engine.playSE("lock")
				} else {
					engine.playSE("holdfail")
				}
			} else if(engine.ctrl.isPush(Controller.BUTTON_D)) {
				if(!firstClick&&mainGrid.getSquareAt(cursorX, cursorY).uncovered&&mainGrid.getSurroundingCovered(cursorX,
						cursorY)==mainGrid.getSurroundingMines(cursorX, cursorY)) {
					val testLocations = arrayOf(intArrayOf(-1, -1), intArrayOf(0, -1), intArrayOf(1, -1), intArrayOf(-1, 0),
						intArrayOf(1, 0), intArrayOf(-1, 1), intArrayOf(0, 1), intArrayOf(1, 1))
					for(loc in testLocations) {
						val px = cursorX+loc[0]
						val py = cursorY+loc[1]
						if(px<0||px>=length) continue
						if(py<0||py>=height) continue
						if(!mainGrid.getSquareAt(px, py).uncovered) {
							mainGrid.getSquareAt(px, py).flagged = true
							mainGrid.getSquareAt(px, py).question = false
						}
					}
					engine.playSE("hold")
				} else {
					engine.playSE("holdfail")
				}
			}
			updateBlockGrid(engine)
			updateEngineGrid(engine)
			numberOfCover = mainGrid.coveredSquares
			if(currentLimit>0&&timeLimit>0) {
				currentLimit--
				if(currentLimit%15==0&&currentLimit<=600) engine.playSE(
					"countdown") else if(currentLimit%60==0&&currentLimit<=1800) engine.playSE("countdown")
			} else if(timeLimit>0) {
				engine.lives = 0
				caughtMine(engine)
				return true
			}
			if(mainGrid.coveredSquares==mainGrid.mines&&mainGrid.mines>0) {
				mainGrid.uncoverNonMines()
				mainGrid.flagAllCovered()
				updateBlockGrid(engine)
				updateEngineGrid(engine)

				//			for (int y = 0; y < height; y++) {
				//				for (int x = 0; x < length; x++) {
				//					if (mainGrid.getSquareAt(x, y).uncovered) engine.field.getBlock(x, y).copy(open);
				//				}
				//			}
				engine.gameEnded()
				engine.stat = GameEngine.Status.EXCELLENT
				engine.ending = 1
				engine.resetStatc()
				return true
			}
			if(engine.ending==0) engine.timerActive = true
			engine.statc[0]++
		} else {
			engine.isInGame = true
			val s:Boolean = engine.playerProp.loginScreen.updateScreen(engine, playerID)
			if(engine.playerProp.isLoggedIn) {
				loadSetting(engine.playerProp.propProfile)
			}
			if(engine.stat===GameEngine.Status.SETTING) engine.isInGame = false
		}
		return true
	}

	private fun updateBlockGrid(engine:GameEngine) {
		val covered = Block(Block.COLOR.BLUE, engine.skin,
			Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
		val open = Block(Block.COLOR.WHITE, engine.skin, Block.ATTRIBUTE.VISIBLE)
		val mine = Block(Block.COLOR.RED, Block.TYPE.GEM, engine.skin, Block.ATTRIBUTE.VISIBLE)
		for(y in blockGrid.indices) {
			for(x in 0 until blockGrid[y].size) {
				if(mainGrid.getSquareAt(x, y).uncovered) {
					if(!mainGrid.getSquareAt(x, y).isMine) {
						if(blockGrid[y][x]!!.toChar()!=open.toChar()) blockGrid[y][x]!!.copy(open)
					} else {
						if(blockGrid[y][x]!!.toChar()!=mine.toChar()) blockGrid[y][x]!!.copy(mine)
					}
				} else {
					if(blockGrid[y][x]!!.toChar()!=covered.toChar()) {
						blockGrid[y][x]!!.copy(covered)
					}
				}
			}
		}
	}

	private fun updateEngineGrid(engine:GameEngine) {
		for(y in 0 until if(height>20) MAX_DIM else height) {
			for(x in 0 until if(length>20) MAX_DIM else length) {
				val px = x+offsetX
				val py = y+offsetY
				if(engine.field.getBlock(x, y)?.toChar()!=blockGrid[py][px]!!.toChar()) {
					engine.field.getBlock(x, y)?.copy(blockGrid[py][px])
				}
			}
		}
	}

	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return
		var ix = (if(length<MAX_DIM) length else MAX_DIM)-10
		if(ix<0) ix = 0
		receiver.drawScoreFont(engine, playerID, ix, 0, name, COLOR.BLUE)
		receiver.drawScoreFont(engine, playerID, ix, 1, "("+(length*height*(minePercentage/100f)).toInt()+" MINES)",
			COLOR.BLUE)
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode) {
			if(engine.playerProp.isLoggedIn) {
				receiver.drawScoreFont(engine, playerID, ix, 3, "PLAYER", COLOR.BLUE)
				receiver.drawScoreFont(engine, playerID, ix, 4, engine.playerProp.nameDisplay, COLOR.WHITE, 2f)
			} else {
				receiver.drawScoreFont(engine, playerID, ix, 3, "LOGIN STATUS", COLOR.BLUE)
				if(!engine.playerProp.isLoggedIn) receiver.drawScoreFont(engine, playerID, ix, 4, "(NOT LOGGED IN)\n(E:LOG IN)")
			}
		} else if(engine.stat===GameEngine.Status.CUSTOM&&!engine.gameActive) {
			engine.playerProp.loginScreen.renderScreen(receiver, engine, playerID)
		} else {
			receiver.drawScoreFont(engine, playerID, ix, 3, "TIME", COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, ix, 4, engine.statistics.time.toTimeStr)
			if(timeLimit>0) {
				receiver.drawScoreFont(engine, playerID, ix, 9, "TIME LIMIT", COLOR.RED)
				receiver.drawScoreFont(engine, playerID, ix, 10, currentLimit.toTimeStr,
					currentLimit<=1800&&engine.statistics.time/2%2==0)
			}
			receiver.drawScoreFont(engine, playerID, ix, 6, "FLAGS LEFT", COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, ix, 7, (mainGrid.mines-mainGrid.flaggedSquares).toString())

			if(mainGrid.mines>0) {
				val m:Int = mainGrid.mines-mainGrid.flaggedSquares
				val proportion:Float = m.toFloat()/mainGrid.mines
				engine.meterValue = m*receiver.getMeterMax(engine)/mainGrid.mines
				engine.meterColor = GameEngine.METER_COLOR_RED
				if(proportion<=0.75f) engine.meterColor = GameEngine.METER_COLOR_ORANGE
				if(proportion<=0.5f) engine.meterColor = GameEngine.METER_COLOR_YELLOW
				if(proportion<=0.25f) engine.meterColor = GameEngine.METER_COLOR_GREEN
			}
			// Block covered = new Block(Block.COLOR.BLUE, engine.getSkin(), Block.ATTRIBUTE.VISIBLE | Block.ATTRIBUTE.OUTLINE);
			// Block open = new Block(Block.COLOR.WHITE, engine.getSkin(), Block.ATTRIBUTE.VISIBLE);
			// Block mine = new Block(Block.COLOR.GEM_RED, engine.getSkin(), Block.ATTRIBUTE.VISIBLE);
			if(engine.stat===GameEngine.Status.CUSTOM) {
				if(height<=MAX_DIM&&length<=MAX_DIM) {
					for(y in 0 until height) {
						for(x in 0 until length) {
							if(mainGrid.getSquareAt(x, y).uncovered) {
								if(!mainGrid.getSquareAt(x, y).isMine) {
									val s = mainGrid.getSurroundingMines(x, y)
									if(s>0) {
										var c = COLOR.PINK
										if(s==2) c = COLOR.GREEN else if(s==3) c = COLOR.COBALT else if(s==4) c = COLOR.RED else if(s==5) c = COLOR.CYAN else if(s==6) c = COLOR.YELLOW else if(s==7) c = COLOR.PURPLE else if(s==8) c = COLOR.ORANGE
										receiver.drawMenuFont(engine, playerID, x, y, "$s", c)
									}
								}
							} else {
								if(mainGrid.getSquareAt(x, y).flagged) {
									receiver.drawMenuFont(engine, playerID, x, y, "b", COLOR.RED)
								} else if(mainGrid.getSquareAt(x, y).question) {
									receiver.drawMenuFont(engine, playerID, x, y, "?")
								}
							}
						}
					}
				} else {
					for(y in 0 until if(height<=MAX_DIM) height else MAX_DIM) {
						for(x in 0 until if(length<=MAX_DIM) length else MAX_DIM) {
							val px:Int = x+offsetX
							val py:Int = y+offsetY
							if(mainGrid.getSquareAt(px, py).uncovered) {
								if(!mainGrid.getSquareAt(px, py).isMine) {
									val s:Int = mainGrid.getSurroundingMines(px, py)
									if(s>0) {
										var c = COLOR.PINK
										if(s==2) c = COLOR.GREEN else if(s==3) c = COLOR.COBALT else if(s==4) c = COLOR.RED else if(s==5) c = COLOR.CYAN else if(s==6) c = COLOR.YELLOW else if(s==7) c = COLOR.PURPLE else if(s==8) c = COLOR.ORANGE
										receiver.drawMenuFont(engine, playerID, x, y, "$s", c)
									}
								}
							} else {
								if(mainGrid.getSquareAt(px, py).flagged) {
									receiver.drawMenuFont(engine, playerID, x, y, "b", COLOR.RED)
								} else if(mainGrid.getSquareAt(px, py).question) {
									receiver.drawMenuFont(engine, playerID, x, y, "?")
								}
							}
						}
					}
				}
				receiver.drawMenuFont(engine, playerID, cursorScreenX, cursorScreenY, "f", COLOR.YELLOW)
				val foffsetX:Int = receiver.fieldX(engine, playerID)+4
				val foffsetY:Int = receiver.fieldY(engine, playerID)+52
				val size = 16
				if(length>MAX_DIM) {
					if(offsetX!=0) receiver.drawDirectFont(foffsetX-2*size, foffsetY+(9.5*size).toInt(), "<",
						COLOR.YELLOW)
					if(offsetX!=length-MAX_DIM) receiver.drawDirectFont(foffsetX+21*size, foffsetY+(9.5*size).toInt(),
						">", COLOR.YELLOW)
				}
				if(height>MAX_DIM) {
					if(offsetY!=0) receiver.drawDirectFont(foffsetX+(9.5*size).toInt(), foffsetY-2*size, "k",
						COLOR.YELLOW)
					if(offsetY!=length-MAX_DIM) receiver.drawDirectFont(foffsetX+(9.5*size).toInt(), foffsetY+21*size,
						"n", COLOR.YELLOW)
				}
			}
			if(engine.playerProp.isLoggedIn||engine.playerName.isNotEmpty()) {
				receiver.drawScoreFont(engine, playerID, 0, 20, "PLAYER", COLOR.BLUE)
				receiver.drawScoreFont(engine, playerID, 0, 21,
					if(owner.replayMode) engine.playerName else engine.playerProp.nameDisplay,
					COLOR.WHITE, 2f)
			}
		}
	}
	/*
		 * Render results screen
		 */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 0, "TIME", COLOR.BLUE)
		receiver.drawMenuFont(engine, playerID, 0, 1, String.format("%10s", engine.statistics.time.toTimeStr))
		receiver.drawMenuFont(engine, playerID, 0, 2, "MINES", COLOR.RED)
		receiver.drawMenuFont(engine, playerID, 0, 3, String.format("%10s", mainGrid.mines))
		receiver.drawMenuFont(engine, playerID, 0, 4, "REMAINING", COLOR.GREEN)
		receiver.drawMenuFont(engine, playerID, 0, 5, String.format("%10s", mainGrid.mines-mainGrid.flaggedSquares))
	}
	/*
		 * Called when saving replay
		 */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties):Boolean {
		if(!owner.replayMode) {
			saveSetting(prop, engine)

		}
		return false
	}
	/**
	 * Load settings from [prop]
	 *
	 * @param prop Property file
	 */
	override fun loadSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
		length = prop.getProperty("minesweeper.length", 15)
		height = prop.getProperty("minesweeper.height", 15)
		minePercentage = prop.getProperty("minesweeper.minePercentage", 15f)
		bgm = prop.getProperty("minesweeper.bgm", 0)
		bg = prop.getProperty("minesweeper.bg", 0)
		timeLimit = prop.getProperty("minesweeper.timeLimit", 0)
	}
	/**
	 * Save settings to [prop]
	 *
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("minesweeper.length", length)
		prop.setProperty("minesweeper.height", height)
		prop.setProperty("minesweeper.minePercentage", minePercentage)
		prop.setProperty("minesweeper.bgm", bgm)
		prop.setProperty("minesweeper.bg", bg)
		prop.setProperty("minesweeper.timeLimit", timeLimit)
	}
	/**
	 * Load settings from [prop]
	 *
	 * @param prop Property file
	 */
	private fun loadSettingPlayer(prop:ProfileProperties) {
		length = prop.getProperty("minesweeper.length", 15)
		height = prop.getProperty("minesweeper.height", 15)
		minePercentage = prop.getProperty("minesweeper.minePercentage", 15f)
		bgm = prop.getProperty("minesweeper.bgm", 0)
		bg = prop.getProperty("minesweeper.bg", 0)
		timeLimit = prop.getProperty("minesweeper.timeLimit", 0)
	}
	/**
	 * Save settings to [prop]
	 *
	 * @param prop Property file
	 */
	private fun saveSettingPlayer(prop:ProfileProperties) {
		prop.setProperty("minesweeper.length", length)
		prop.setProperty("minesweeper.height", height)
		prop.setProperty("minesweeper.minePercentage", minePercentage)
		prop.setProperty("minesweeper.bgm", bgm)
		prop.setProperty("minesweeper.bg", bg)
		prop.setProperty("minesweeper.timeLimit", timeLimit)
	}

	companion object {
		private const val MAX_DIM = 20
		private const val TIMEBONUS_SQUARE = 9
		private val headerColor = COLOR.YELLOW
	}
}
