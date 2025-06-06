/*
 Copyright (c) 2019-2024,
 This library class was created by 0xFC963F18DC21 / Shots243
 It is part of an extension library for the game NullpoMino (copyright 2010-2024)

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis

 Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.

 THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 THIS KOTLIN VERSION WAS NOT MADE IN ASSOCIATION WITH THE LIBRARY CREATOR.

 Original Repository: https://github.com/Shots243/ModePile

 When using this library in a mode / library pack of your own, the following
 conditions must be satisfied:
     - This license must remain visible at the top of the document, unmodified.
     - You are allowed to use this library for any modding purpose.
         - If this is the case, the Library Creator must be credited somewhere.
             - Source comments only are fine, but in a README is recommended.
     - Modification of this library is allowed, but only in the condition that a
       pull request is made to merge the changes to the repository.

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

package zeroxfc.nullpo.custom.modes

import mu.nu.nullpo.game.component.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.AbstractMode
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import zeroxfc.nullpo.custom.libs.ProfileProperties
import zeroxfc.nullpo.custom.modes.objects.minesweeper.GameGrid
import kotlin.random.Random

class Minesweeper:AbstractMode() {
	private var mainGrid:GameGrid = GameGrid()
	private var width = 0
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
	override val name get() = "MineSweeper"

	override fun playerInit(engine:GameEngine) {
		//SoundLoader.loadSoundset(SoundLoader.LOADTYPE_MINESWEEPER)
		width = 0
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
		engine.frameSkin = GameEngine.FRAME_COLOR_BLUE
		engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_SAMECOLOR
		mainGrid = GameGrid()
		blockGrid = emptyArray()
		if(!owner.replayMode) loadSetting(engine, owner.modeConfig)
		else loadSetting(engine, owner.replayProp)

		engine.owner.bgMan.bg = bg
	}

	override fun onSetting(engine:GameEngine):Boolean {
		mainGrid = GameGrid()

		// Menu
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change:Int = updateCursor(engine, 5)
			if(change!=0) {
				engine.playSE("change")
				when(engine.statc[2]) {
					0 -> {
						width += change
						if(width>256) width = 4
						if(width<4) width = 256
					}
					1 -> {
						height += change
						if(height>256) height = 5
						if(height<4) height = 256
					}
					2 -> {
						minePercentage += change*0.1f
						if(minePercentage>99.9f) minePercentage = 0.1f
						if(minePercentage<0.1f||(width*height*(minePercentage/100f)).toInt()==0) minePercentage = 99.9f
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
						engine.owner.bgMan.bg = bg
					}
				}
			}
			engine.owner.bgMan.bg = bg

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&engine.statc[3]>=5) {
				engine.playSE("decide")
				if(engine.playerProp.isLoggedIn) {
					saveSettingPlayer(engine.playerProp)
					engine.playerProp.saveProfileConfig()
				} else {
					saveSetting(engine, owner.modeConfig)
					owner.saveModeConfig()
				}
				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) {
				engine.quitFlag = true
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

	override fun renderSetting(engine:GameEngine) {
		drawMenu(engine, receiver, 0, COLOR.BLUE, 0, "WIDTH" to width, "HEIGHT" to height)
		drawMenu(
			engine, receiver, 4, COLOR.RED, 2, "MINE%" to "%.2f".format(minePercentage),
			"TIME LIMIT" to if(timeLimit>0) timeLimit.toTimeStr else "DISABLED"
		)
		drawMenuBGM(engine, receiver, bgm, COLOR.GREEN)
		drawMenu(engine, receiver, 10, COLOR.GREEN, 5, "BACKGROUND" to bg)
	}

	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		if(engine.quitFlag) {
			engine.playerProp.reset()
		}
	}

	override fun onReady(engine:GameEngine):Boolean {
		// 横溜め
		if(engine.ruleOpt.dasInReady&&engine.gameActive) engine.padRepeat() else if(engine.ruleOpt.dasRedirectInDelay) {
			engine.dasRedirect()
		}

		// Initialization
		if(engine.statc[0]==0) {
			// fieldInitialization
			blockGrid = Array(height) {arrayOfNulls(width)}
			for(y in blockGrid.indices) {
				for(x in 0..<blockGrid[y].size) {
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
			mainGrid = GameGrid(width, height, minePercentage, engine.randSeed)
			engine.ruleOpt.fieldWidth = minOf(width, MAX_DIM)
			engine.ruleOpt.fieldHeight = minOf(height, MAX_DIM)
			engine.ruleOpt.fieldHiddenHeight = 0
			engine.field.reset()
			engine.createFieldIfNeeded()
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
			if(!engine.readyDone) engine.owner.musMan.bgm = BGM.values[bgm]
			engine.owner.mode?.startGame(engine)
			engine.owner.receiver.startGame(engine)
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

	override fun onGameOver(engine:GameEngine):Boolean {
		val field = engine.field
		if(engine.lives<=0)
			when {
				engine.statc[0]==0 -> {
					// もう復活できないとき
					engine.gameEnded()
					engine.blockShowOutlineOnly = false
					if(owner.players<2) owner.musMan.bgm = BGM.Silent
					if(field.isEmpty) engine.statc[0] = field.height+1 else engine.resetFieldVisible()
					engine.statc[0]++
				}
				engine.statc[0]<field.height+1 -> {
					for(i in 0..<field.width)
						field.getBlock(i, field.height-engine.statc[0])?.let {blk ->
							val covered = Block(Block.COLOR.BLUE, engine.blkSkin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
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
					for(i in 0..<owner.players) {
						if(i==engine.playerID||engine.dieAll) {
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
				for(i in field.hiddenHeight*-1..<field.height)
					for(j in 0..<field.width)
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
		if(!engine.gameActive) return
		engine.playSE("bomb")
		receiver.blockBreak(engine, cursorX, cursorY, Block(Block.COLOR.RED, Block.TYPE.GEM))
		mainGrid.uncoverAllMines()
		updateBlockGrid(engine)
		updateEngineGrid(engine)

		receiver.blockBreak(engine, engine.field.findBlocks {it.color==Block.COLOR.RED&&it.isGemBlock})
		/*				for (int y = 0; y < height; y++) {
							for (int x = 0; x < length; x++) {
								if (mainGrid.getSquareAt(x, y).isMine) engine.field.getBlock(x, y).copy(mine);
							}
						}*/
		engine.gameEnded()
		engine.resetStatc()
		engine.stat = GameEngine.Status.GAMEOVER
	}

	override fun onCustom(engine:GameEngine):Boolean {
		if(engine.gameActive) {
			// Block mine = new Block(Block.COLOR.GEM_RED, engine.getSkin(), Block.ATTRIBUTE.VISIBLE);
			// Block open = new Block(Block.COLOR.WHITE, engine.getSkin(), Block.ATTRIBUTE.VISIBLE);
			var changeX = 0
			var changeY = 0
			if(engine.ctrl.isPress(Controller.BUTTON_LEFT)) {
				if(engine.ctrl.isPush(Controller.BUTTON_LEFT)||engine.ctrl.isPress(Controller.BUTTON_E)) {
					cursorX -= 1
					changeX += -1
					if(cursorX<0) cursorX = width-1
					engine.playSE("change")
				}
			}
			if(engine.ctrl.isPress(Controller.BUTTON_RIGHT)) {
				if(engine.ctrl.isPush(Controller.BUTTON_RIGHT)||engine.ctrl.isPress(Controller.BUTTON_E)) {
					cursorX += 1
					changeX += 1
					if(cursorX>=width) cursorX = 0
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
			if(width>20) {
				when {
					cursorX==0 -> {
						cursorScreenX = 0
						offsetX = 0
					}
					cursorX==width-1 -> {
						cursorScreenX = MAX_DIM-1
						offsetX = width-MAX_DIM
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
					val testLocations = arrayOf(
						intArrayOf(-1, -1), intArrayOf(0, -1), intArrayOf(1, -1), intArrayOf(-1, 0),
						intArrayOf(1, 0), intArrayOf(-1, 1), intArrayOf(0, 1), intArrayOf(1, 1)
					)
					for(loc in testLocations) {
						receiver.blockBreak(engine, cursorX, cursorY, Block(Block.COLOR.WHITE))
						val px = cursorX+loc[0]
						val py = cursorY+loc[1]
						if(px<0||px>=width) continue
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
				if(!firstClick&&mainGrid.getSquareAt(cursorX, cursorY).uncovered&&mainGrid.getSurroundingCovered(
						cursorX,
						cursorY
					)==mainGrid.getSurroundingMines(cursorX, cursorY)) {
					val testLocations = arrayOf(
						intArrayOf(-1, -1), intArrayOf(0, -1), intArrayOf(1, -1), intArrayOf(-1, 0),
						intArrayOf(1, 0), intArrayOf(-1, 1), intArrayOf(0, 1), intArrayOf(1, 1)
					)
					for(loc in testLocations) {
						val px = cursorX+loc[0]
						val py = cursorY+loc[1]
						if(px<0||px>=width) continue
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
					"countdown"
				) else if(currentLimit%60==0&&currentLimit<=1800) engine.playSE("countdown")
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

				/*			for (int y = 0; y < height; y++) {
								for (int x = 0; x < length; x++) {
									if (mainGrid.getSquareAt(x, y).uncovered) engine.field.getBlock(x, y).copy(open);
								}
							}*/
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
			val s:Boolean = engine.playerProp.loginScreen.updateScreen(engine)
			if(engine.playerProp.isLoggedIn) {
				loadSetting(engine, engine.playerProp.propProfile)
			}
			if(engine.stat===GameEngine.Status.SETTING) engine.isInGame = false
		}
		return true
	}

	private fun updateBlockGrid(engine:GameEngine) {
		val covered = Block(
			Block.COLOR.BLUE, engine.blkSkin,
			Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE
		)
		val open = Block(Block.COLOR.WHITE, engine.blkSkin, Block.ATTRIBUTE.VISIBLE)
		val mine = Block(Block.COLOR.RED, Block.TYPE.GEM, engine.blkSkin, Block.ATTRIBUTE.VISIBLE)
		for(y in blockGrid.indices) {
			for(x in 0..<blockGrid[y].size) {
				if(mainGrid.getSquareAt(x, y).uncovered) {
					if(!mainGrid.getSquareAt(x, y).isMine)
						blockGrid[y][x]=open
					 else
						blockGrid[y][x]=mine

				} else {
					if(blockGrid[y][x]!!.toChar()!=covered.toChar())
						blockGrid[y][x]=covered

				}
			}
		}
	}

	private fun updateEngineGrid(engine:GameEngine) {
		for(y in 0..<if(height>20) MAX_DIM else height) {
			for(x in 0..<if(width>20) MAX_DIM else width) {
				val px = x+offsetX
				val py = y+offsetY
				if(engine.field.getBlock(x, y)!=blockGrid[py][px]) {
					engine.field.setBlock(x, y,blockGrid[py][px])
				}
			}
		}
	}

	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return
		var ix = (if(width<MAX_DIM) width else MAX_DIM)-10
		if(ix<0) ix = 0
		receiver.drawScoreFont(engine, ix, 0, name, COLOR.BLUE)
		receiver.drawScoreFont(
			engine, ix, 1, "("+(width*height*(minePercentage/100f)).toInt()+" MINES)", COLOR.BLUE
		)
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode) {
			if(engine.playerProp.isLoggedIn) {
				receiver.drawScoreFont(engine, ix, 3, "PLAYER", COLOR.BLUE)
				receiver.drawScoreFont(engine, ix, 4, engine.playerProp.nameDisplay, COLOR.WHITE, 2f)
			} else {
				receiver.drawScoreFont(engine, ix, 3, "LOGIN STATUS", COLOR.BLUE)
				if(!engine.playerProp.isLoggedIn) receiver.drawScoreFont(engine, ix, 4, "(NOT LOGGED IN)\n(E:LOG IN)")
			}
		} else if(engine.stat===GameEngine.Status.CUSTOM&&!engine.gameActive) {
			engine.playerProp.loginScreen.renderScreen(receiver, engine)
		} else {
			receiver.drawScoreFont(engine, ix, 3, "TIME", COLOR.BLUE)
			receiver.drawScoreFont(engine, ix, 4, engine.statistics.time.toTimeStr)
			if(timeLimit>0) {
				receiver.drawScoreFont(engine, ix, 9, "TIME LIMIT", COLOR.RED)
				receiver.drawScoreFont(
					engine, ix, 10, currentLimit.toTimeStr, currentLimit<=1800&&engine.statistics.time/2%2==0
				)
			}
			receiver.drawScoreFont(engine, ix, 6, "FLAGS LEFT", COLOR.BLUE)
			receiver.drawScoreFont(engine, ix, 7, (mainGrid.mines-mainGrid.flaggedSquares).toString())

			if(mainGrid.mines>0) {
				val m:Int = mainGrid.mines-mainGrid.flaggedSquares
				val proportion:Float = m.toFloat()/mainGrid.mines
				engine.meterValue = m*1f/mainGrid.mines
				engine.meterColor = GameEngine.METER_COLOR_RED
				if(proportion<=0.75f) engine.meterColor = GameEngine.METER_COLOR_ORANGE
				if(proportion<=0.5f) engine.meterColor = GameEngine.METER_COLOR_YELLOW
				if(proportion<=0.25f) engine.meterColor = GameEngine.METER_COLOR_GREEN
			}
			// Block covered = new Block(Block.COLOR.BLUE, engine.getSkin(), Block.ATTRIBUTE.VISIBLE | Block.ATTRIBUTE.OUTLINE);
			// Block open = new Block(Block.COLOR.WHITE, engine.getSkin(), Block.ATTRIBUTE.VISIBLE);
			// Block mine = new Block(Block.COLOR.GEM_RED, engine.getSkin(), Block.ATTRIBUTE.VISIBLE);
			if(engine.stat===GameEngine.Status.CUSTOM) {
				if(height<=MAX_DIM&&width<=MAX_DIM) {
					for(y in 0..<height) {
						for(x in 0..<width) {
							if(mainGrid.getSquareAt(x, y).uncovered) {
								if(!mainGrid.getSquareAt(x, y).isMine) {
									val s = mainGrid.getSurroundingMines(x, y)

									if(s>0) receiver.drawMenuFont(
										engine, x, y, "$s", when(s) {
											1 -> COLOR.PINK
											2 -> COLOR.GREEN
											3 -> COLOR.COBALT
											4 -> COLOR.RED
											5 -> COLOR.CYAN
											6 -> COLOR.YELLOW
											7 -> COLOR.PURPLE
											8 -> COLOR.ORANGE
											else -> COLOR.WHITE
										}
									)
								}
							} else {
								if(mainGrid.getSquareAt(x, y).flagged) receiver.drawMenuFont(engine, x, y, BaseFont.CURSOR, COLOR.RED)
								else if(mainGrid.getSquareAt(x, y).question) receiver.drawMenuFont(engine, x, y, "?")
							}
						}
					}
				} else {
					for(y in 0..<minOf(height, MAX_DIM)) {
						for(x in 0..<minOf(width, MAX_DIM)) {
							val px:Int = x+offsetX
							val py:Int = y+offsetY
							if(mainGrid.getSquareAt(px, py).uncovered) {
								if(!mainGrid.getSquareAt(px, py).isMine) {
									val s:Int = mainGrid.getSurroundingMines(px, py)
									if(s>0) receiver.drawMenuFont(
										engine, x, y, "$s", when(s) {
											1 -> COLOR.PINK
											2 -> COLOR.GREEN
											3 -> COLOR.COBALT
											4 -> COLOR.RED
											5 -> COLOR.CYAN
											6 -> COLOR.YELLOW
											7 -> COLOR.PURPLE
											8 -> COLOR.ORANGE
											else -> COLOR.WHITE
										}
									)
								}
							} else {
								if(mainGrid.getSquareAt(px, py).flagged) receiver.drawMenuFont(engine, x, y, BaseFont.CURSOR, COLOR.RED)
								else if(mainGrid.getSquareAt(px, py).question) receiver.drawMenuFont(engine, x, y, "?")
							}
						}
					}
				}
				receiver.drawMenuFont(engine, cursorScreenX, cursorScreenY, BaseFont.CIRCLE_L, COLOR.YELLOW)
				val foffsetX = receiver.fieldX(engine)+4
				val foffsetY = receiver.fieldY(engine)+52
				val size = 16
				if(width>MAX_DIM) {
					if(offsetX!=0)
						receiver.drawDirectFont(foffsetX-2*size, foffsetY+(9.5*size).toInt(), "<", COLOR.YELLOW)
					if(offsetX!=width-MAX_DIM)
						receiver.drawDirectFont(foffsetX+21*size, foffsetY+(9.5*size).toInt(), ">", COLOR.YELLOW)
				}
				if(height>MAX_DIM) {
					if(offsetY!=0)
						receiver.drawDirectFont(foffsetX+(9.5*size).toInt(), foffsetY-2*size, BaseFont.UP_S, COLOR.YELLOW)
					if(offsetY!=width-MAX_DIM)
						receiver.drawDirectFont(foffsetX+(9.5*size).toInt(), foffsetY+21*size, BaseFont.DOWN_S, COLOR.YELLOW)
				}
			}
			if(engine.playerProp.isLoggedIn||engine.playerName.isNotEmpty()) {
				receiver.drawScoreFont(engine, 0, 20, "PLAYER", COLOR.BLUE)
				receiver.drawScoreFont(
					engine, 0, 21, if(owner.replayMode) engine.playerName else engine.playerProp.nameDisplay,
					COLOR.WHITE, 2f
				)
			}
		}
	}
	/*
		 * Render results screen
		 */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 0, "TIME", COLOR.BLUE)
		receiver.drawMenuNum(engine, 0, 1, "%10s".format(engine.statistics.time.toTimeStr))
		receiver.drawMenuFont(engine, 0, 2, "MINES", COLOR.RED)
		receiver.drawMenuNum(engine, 0, 3, "%10s".format(mainGrid.mines))
		receiver.drawMenuFont(engine, 0, 4, "REMAINING", COLOR.GREEN)
		receiver.drawMenuNum(engine, 0, 5, "%10s".format(mainGrid.mines-mainGrid.flaggedSquares))
	}
	/*
		 * Called when saving replay
		 */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		if(!owner.replayMode) {
			saveSetting(engine, prop)
		}
		return false
	}
	/**
	 * Load settings from [prop]
	 *
	 * @param prop Property file
	 */
	override fun loadSetting(engine: GameEngine, prop: CustomProperties, ruleName: String, playerID: Int) {
		width = prop.getProperty("minesweeper.length", 15)
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
	override fun saveSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("minesweeper.length", width)
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
		width = prop.getProperty("minesweeper.length", 15)
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
		prop.setProperty("minesweeper.length", width)
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
