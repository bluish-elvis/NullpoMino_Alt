/*
 Copyright (c) 2021-2023,
 This library class was created by 0xFC963F18DC21 / Shots243
 It is part of an extension library for the game NullpoMino (copyright 2010-2023)

 Kotlin converted and modified by Venom=Nhelv

 Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.

 THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

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

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.AbstractMode
import mu.nu.nullpo.gui.slick.MouseInput
import mu.nu.nullpo.gui.slick.NullpoMinoSlick
import zeroxfc.nullpo.custom.libs.backgroundtypes.ResourceHolderCustomAssetExtension
import mu.nu.nullpo.gui.slick.img.RenderStaffRoll.scale
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.getONorOFF
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import zeroxfc.nullpo.custom.libs.SideWaveText
import zeroxfc.nullpo.custom.libs.WeightedRandomizer
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class Collapse:AbstractMode() {
	private var enableBombs = false
	private val rankingScore = List(MAX_DIFFICULTIES) {MutableList(MAX_RANKING) {0L}}
	private val rankingLevel = List(MAX_DIFFICULTIES) {MutableList(MAX_RANKING) {0}}
	private var bScore = 0
	private var rankingRank = 0
	private var difficulty = 0
	private var linesLeft = 0
	private var bgm = 0
	private var cursorX = 0
	private var cursorY = 0
	private var fieldX = 0
	private var fieldY = 0
	private var holderType = 0
	private var spawnTimer = 0
	private var spawnTimerLimit = 0
	private var wRandomEngine:WeightedRandomizer = WeightedRandomizer(intArrayOf(), 0L)
	private var wRandomEngineBomb:WeightedRandomizer = WeightedRandomizer(intArrayOf(), 0L)
	private var sTextArr:MutableList<SideWaveText> = mutableListOf()
	private var nextBlocks:MutableList<Block> = mutableListOf()
	private var localState = 0
	private var localRandom:Random = Random.Default
	private var force = false
	private var lineSpawn = 0
	private var multiplier = 0.0
	private var acTime = 0
	private val rankingScorePlayer = List(MAX_DIFFICULTIES) {MutableList(MAX_RANKING) {0L}}
	private val rankingLevelPlayer = List(MAX_DIFFICULTIES) {MutableList(MAX_RANKING) {0}}
	private var rankingRankPlayer = 0
	override val rankMap
		get() = rankMapOf(rankingScore.mapIndexed {a, x -> "$a.score" to x}+
			rankingLevel.mapIndexed {a, x -> "$a.level" to x})

	override val rankPersMap
		get() = rankMapOf(rankingScorePlayer.mapIndexed {a, x -> "$a.score" to x}+
			rankingLevelPlayer.mapIndexed {a, x -> "$a.level" to x})

	/*
     * ------ MAIN METHODS ------
     */
	override val name:String = "COLLAPSE"

	override fun playerInit(engine:GameEngine) {
		rankingScore.forEach {it.fill(0)}
		rankingLevel.forEach {it.fill(0)}
		rankingScorePlayer.forEach {it.fill(0)}
		rankingLevelPlayer.forEach {it.fill(0)}
		rankingRankPlayer = -1
		enableBombs = false
		rankingRank = -1
		difficulty = 0
		linesLeft = 0
		bgm = 0
		cursorX = 0
		cursorY = 0
		fieldX = -1
		fieldY = -1
		wRandomEngine = WeightedRandomizer(intArrayOf(), 0L)
		wRandomEngineBomb = WeightedRandomizer(intArrayOf(), 0L)
		localRandom = Random.Default
		localState = -1
		force = false
		bScore = 0
		lineSpawn = 0
		multiplier = 1.0
		acTime = -1
		resetSTextArr()
		spawnTimer = 0
		spawnTimerLimit = 0
		nextBlocks.clear()
		resetBlockArray()
		val mainClass:String = ResourceHolderCustomAssetExtension.mainClassName
		holderType = when {
			mainClass.contains("Slick") -> HOLDER_SLICK
			else -> -1
		}
		engine.frameColor = GameEngine.FRAME_COLOR_BRONZE
	}

	private fun resetSTextArr() {
		sTextArr.clear()
	}

	private fun updateSTextArr() {
		sTextArr.removeAll {it.lifeTime>=SideWaveText.MaxLifeTime}
		sTextArr.forEach {it.update()}
	}

	private fun addSText(score:Int, big:Boolean, largeClear:Boolean) {
		val str = "$score"
		val offsetX = -1*(str.length*if(big) 32 else 16/2)
		var offsetY = -8
		if(big) {
			offsetY *= 2
		}
		sTextArr.add(
			SideWaveText(
				cursorX+offsetX, cursorY+offsetY, 1.5f,
				if(!largeClear) 0f else if(big) 24f else 16f, str, big, largeClear
			)
		)
	}

	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change:Int = updateCursor(engine, 2)
			if(change!=0) {
				engine.playSE("change")
				when(engine.statc[2]) {
					0 -> {
						difficulty += change
						if(difficulty<0) difficulty = MAX_DIFFICULTIES-1
						if(difficulty>=MAX_DIFFICULTIES) difficulty = 0
					}
					1 -> enableBombs = !enableBombs
					2 -> {
						bgm += change
						if(bgm<0) bgm = 15
						if(bgm>15) bgm = 0
					}
				}
			}

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&engine.statc[3]>=5) {
				engine.playSE("decide")
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
		drawMenu(
			engine, receiver, 0, COLOR.RED, 0, "DIFFICULTY" to DIFFICULTY_NAMES[difficulty]
		)
		drawMenu(
			engine, receiver, 2, COLOR.BLUE, 1, "BOMBS" to enableBombs.getONorOFF(),
			"BGM" to "$bgm"
		)
	}

	override fun onReady(engine:GameEngine):Boolean {
		// 横溜め
		if(engine.ruleOpt.dasInReady&&engine.gameActive) engine.padRepeat() else if(engine.ruleOpt.dasRedirectInDelay) {
			engine.dasRedirect()
		}

		// Initialization
		if(engine.statc[0]==0) {
			engine.ruleOpt.fieldWidth = 12
			engine.ruleOpt.fieldHeight = 16
			engine.ruleOpt.fieldHiddenHeight = 1
			engine.statistics.level = 0
			resetSTextArr()
			cursorX = 0
			cursorY = 0
			fieldX = -1
			fieldY = -1
			localState = -1
			force = false
			localRandom = Random(engine.randSeed-1L)
			wRandomEngine = WeightedRandomizer(tableColorWeights[0], engine.randSeed)
			wRandomEngineBomb = WeightedRandomizer(tableBombColorWeights[0], engine.randSeed+1)
			levelUp(engine, true)
			engine.field.reset()
			engine.createFieldIfNeeded()
			engine.field.setAllAttribute(true, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
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
			if(!engine.readyDone) engine.owner.musMan.bgm = BGMStatus.BGM.Silent
			startGame(engine)
			engine.owner.receiver.startGame(engine)
			engine.stat = GameEngine.Status.CUSTOM
			for(i in 0..<lineSpawn) {
				while(nextEmpty<engine.fieldWidth) {
					val temp = if(linesLeft<=1) Block.COLOR.WHITE else tableColors[wRandomEngine.nextInt()]
					nextBlocks.add(Block(temp, engine.skin).apply {
						setAttribute(true, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
					})
				}
				incrementField(engine)
				resetBlockArray()
			}
			engine.playSE("garbage")
			localState = LOCALSTATE_INGAME
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

	override fun startGame(engine:GameEngine) {
		engine.owner.musMan.bgm = BGMStatus.BGM.values[bgm]
	}

	override fun onCustom(engine:GameEngine):Boolean {
		/*		if (engine.ctrl.isPush(Controller.BUTTON_D)) {
		//			engine.resetStatc();
		//			engine.gameEnded();
		//
		//			engine.stat = GameEngine.Status.EXCELLENT;
		//			engine.ending = 1;
		//			engine.rainbowAnimate = false;
		//			return false;
				}  // DEBUG CODE.*/
		return if(engine.gameActive) {
			parseMouse(engine)
			if(!engine.rainbowAnimate) engine.rainbowAnimate = true
			var incrementTime = false
			when(localState) {
				LOCALSTATE_INGAME -> {
					if(!engine.timerActive) engine.timerActive = true
					incrementTime = stateInGame(engine)
				}
				LOCALSTATE_TRANSITION -> {
					if(engine.timerActive) engine.timerActive = false
					incrementTime = stateTransition(engine)
				}
				else -> {
				}
			}
			if(engine.ending!=0) {
				engine.timerActive = false
			}
			if(incrementTime) engine.statc[0]++
			true
		} else {
			showPlayerStats = false
			engine.isInGame = true
			engine.playerProp.loginScreen.updateScreen(engine)
			if(engine.playerProp.isLoggedIn) {
				loadRankingPlayer(engine.playerProp)
				loadSetting(engine, engine.playerProp.propProfile)
			}
			if(engine.stat===GameEngine.Status.SETTING) engine.isInGame = false
			true
		}
	}
	// DONE: Make the rest of the gamemode work.
	private fun clearSquares(engine:GameEngine) {
		var score = 0
		var squares = 0
		var fromBomb = false
		engine.field.getBlock(fieldX, fieldY)?.let {b ->
			if(b.color?.color==true) {
				squares = getSquares(engine, fieldX, fieldY)
				if(squares>=3) {
					score = getClearScore(engine, squares)
					for(y in 0..<engine.field.height) for(x in 0..<engine.field.width)
						engine.field.getBlock(x, y)?.let {
							if(it.getAttribute(Block.ATTRIBUTE.TEMP_MARK)) it.setAttribute(true, Block.ATTRIBUTE.ERASE)
						}
					engine.playSE("erase${if(squares>=12) 1 else 0}")
				} else {
					engine.playSE("rotfail")
					for(y in 0..<engine.field.height) for(x in 0..<engine.field.width)
						engine.field.getBlock(x, y)?.setAttribute(false, Block.ATTRIBUTE.TEMP_MARK)
				}
			} else if(b.type==Block.TYPE.GEM) {
				fromBomb = true
				if(b.color?.color==true) {
					val c = b.color
					for(y in 0..<engine.field.height) for(x in 0..<engine.field.width)
						engine.field.getBlock(x, y)?.let {
							if(it.color==c&&it.type==Block.TYPE.BLOCK) {
								it.setAttribute(true, Block.ATTRIBUTE.ERASE)
								squares++
							}
						}
					score = (getClearScore(engine, squares)*0.75).toInt()
				} else {
					squares = 0
					for(y in 0..<engine.field.height) for(x in 0..<engine.field.width)
						engine.field.getBlock(x, y)?.let {
							if(isCoordWithinRadius(fieldX, fieldY, x, y, 4.5)) {
								it.setAttribute(true, Block.ATTRIBUTE.ERASE)
								squares++
							}
						}
					score = (getClearScore(engine, squares)*0.5).toInt()
				}
				explode(engine)
				engine.playSE("erase2")
			} else {
				engine.playSE("rotfail")
			}
		}
		if(score>0) {
			score *= multiplier.toInt()
			if(squares>=6) addSText(score, fromBomb, squares>=12)
			setNewLowerScore(engine)
			if(engine.field.isEmpty) {
				acTime = 0
				engine.playSE("bravo")
				engine.statistics.scoreBonus += 100000*1.025.pow(engine.statistics.level.toDouble()).toInt()
			}
			engine.statistics.scoreLine += score
		}
	}

	private fun stateInGame(engine:GameEngine):Boolean {
		if(fieldX!=-1)
			if(!engine.field.getBlockEmpty(fieldX, fieldY))
				clearSquares(engine)



		if(bScore>0) bScore = 0
		val all = engine.field.findBlocks(false) {it.getAttribute(Block.ATTRIBUTE.ERASE)}
		engine.field.delBlocks(all).let {receiver.blockBreak(engine, it)}
		val brk = all.values.sumOf {it.size}

		if(brk>0) {
			if(engine.field.freeFall()) engine.playSE("step") else engine.playSE("lock")
			for(i in 0..5) bringColumnsCloser(engine)
		}
		spawnTimer++
		if(spawnTimer>=spawnTimerLimit) {
			spawnTimer = 0 // -= spawnTimerLimit
			val index = nextEmpty
			if(index!=-1) {
				val coeff = localRandom.nextDouble()
				var temp = -1
				if(coeff<=bombChance*(if(engine.field.highestBlockY<4) 3.0 else 1.0)&&enableBombs&&linesLeft!=1) {
					temp = wRandomEngineBomb.nextInt()
					nextBlocks.add(Block(tableColors[temp], Block.TYPE.GEM, engine.skin).apply {
						setAttribute(true, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
					})
				} else {
					temp = wRandomEngine.nextInt()
					var bone = false
					if(temp==wRandomEngine.max&&wRandomEngine.max==5) bone = true
					if(linesLeft==1) {
						temp = 5
					}
					nextBlocks.add(Block(tableColors[temp], engine.skin).apply {
						setAttribute(true, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
						setAttribute(bone, Block.ATTRIBUTE.BONE)
					})
				}
			} else {
				if(linesLeft>0) linesLeft--
				val proportion = linesLeft.toDouble()/tableLevelLine[engine.statistics.level]
				engine.meterColor = GameEngine.METER_COLOR_LEVEL
				engine.meterValue =
					if(linesLeft>=0) proportion.toFloat() else 1f
				if(linesLeft!=0) {
					engine.playSE("garbage")
					incrementField(engine)
				}
				resetBlockArray()
				if(linesLeft==3) {
					engine.playSE("levelstop")
				}
				multiplier = midgameSpeedSet(engine)
				if(spawnTimer==0&&engine.field.highestBlockY<=2&&engine.field.highestBlockY>=0) {
					engine.playSE("danger")
				}
			}
		} else if(force&&!nextFull) {
			spawnTimer = 0
			force = false
			while(!nextFull) {
				val index = nextEmpty
				val coeff = localRandom.nextDouble()
				var temp:Int
				if(coeff<=bombChance*(if(engine.field.highestBlockY<4) 3.0 else 1.0)&&enableBombs&&linesLeft!=1) {
					temp = wRandomEngineBomb.nextInt()
					if(linesLeft==1) {
						temp = 5
					}
					nextBlocks.add(Block(tableColors[temp], Block.TYPE.GEM, engine.skin).apply {
						setAttribute(true, Block.ATTRIBUTE.OUTLINE, Block.ATTRIBUTE.VISIBLE)
					})
				} else {
					temp = wRandomEngine.nextInt()
					var bone = false
					if(temp==wRandomEngine.max&&wRandomEngine.max==5) bone = true
					if(linesLeft==1) {
						temp = 5
					}
					nextBlocks.add(Block(tableColors[temp], engine.skin).apply {
						setAttribute(true, Block.ATTRIBUTE.OUTLINE, Block.ATTRIBUTE.VISIBLE)
						setAttribute(bone, Block.ATTRIBUTE.BONE)
					})
				}
			}
		}
		if(linesLeft==0) {
			engine.resetStatc()
			localState = LOCALSTATE_TRANSITION
			return false
		}
		if(engine.field.highestBlockY<0) {
			if(engine.statistics.level<19) {
				engine.stat = GameEngine.Status.GAMEOVER
				engine.resetStatc()
				engine.gameEnded()
				engine.rainbowAnimate = false
			} else {
				engine.stat = GameEngine.Status.EXCELLENT
				engine.resetStatc()
				engine.gameEnded()
				engine.ending = 1
				engine.rainbowAnimate = false
			}
		}
		return false
	}

	private fun stateTransition(engine:GameEngine):Boolean {
		if(engine.statc[0]>180+16*3) {
			levelUp(engine, false)
			resetBlockArray()
			engine.resetStatc()
			for(i in 0..<lineSpawn) {
				while(!nextFull) {
					val index = nextEmpty
					val temp = wRandomEngine.nextInt()
					val bone = (temp==wRandomEngine.max&&wRandomEngine.max==5)

					nextBlocks.add(Block(if(linesLeft>1) tableColors[temp] else Block.COLOR.WHITE, engine.skin).apply {
						setAttribute(true, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
						setAttribute(bone, Block.ATTRIBUTE.BONE)
					})
				}
				incrementField(engine)
				resetBlockArray()
			}
			engine.playSE("go")
			engine.playSE("rise")
			localState = LOCALSTATE_INGAME
			return false
		} else if(engine.statc[0]>0) {
			val f:Int = engine.statc[0]-60
			if(f%3==0&&f>=0&&15-f/3>=-1) {
				val y = 15-f/3
				for(x in 0..<engine.field.width)
					engine.field.delBlock(x, y)?.let {receiver.blockBreak(engine, x, y, it)}
			}
			if(engine.statc[0]==120+16*3) engine.playSE("ready")
		} else {
			engine.playSE("stageclear")
			bScore = getLevelClearBonus(engine)
			setNewLowerScore(engine)
			engine.statistics.scoreBonus += bScore
		}
		return true
	}

	private val nextFull:Boolean get() = nextBlocks.size>=FIELD_WIDTH
	private val nextEmpty:Int
		get() = if(nextFull) -1 else nextBlocks.size

	private fun getSquares(engine:GameEngine, x:Int, y:Int):Int =
		engine.field.getBlock(x, y)?.color?.let {flagSquares(engine, 0, x, y, it)} ?: 0

	private fun flagSquares(engine:GameEngine, counter:Int, x:Int, y:Int, color:Block.COLOR):Int {
		if(x in 0..11&&y in 0..15) {
			engine.field.getBlock(x, y)?.let {
				if(it.color==color&&it.type==Block.TYPE.BLOCK) {
					it.setAttribute(true, Block.ATTRIBUTE.TEMP_MARK)
					return@flagSquares counter+1+flagSquares(engine, counter, x+1, y, color)+
						flagSquares(engine, counter, x-1, y, color)+
						flagSquares(engine, counter, x, y+1, color)+
						flagSquares(engine, counter, x, y-1, color)
				}
			}
		}
		return 0
	}

	private fun resetBlockArray() {
		nextBlocks.clear()
	}

	private fun incrementField(engine:GameEngine) {
		for(y in -1..<engine.field.height-1) {
			for(x in 0..<engine.field.width) {
				engine.field.getBlock(x, y)?.replace(engine.field.getBlock(x, y+1))
			}
		}
		for(x in 0..<engine.field.width) {
			engine.field.getBlock(x, engine.field.height-1)?.replace(nextBlocks[x])
		}
	}

	private fun bringColumnsCloser(engine:GameEngine) {
		for(x in 5 downTo 1) if((0..<engine.field.height).all {y -> engine.field.getBlockEmpty(x, y)}) {
			for(x2 in x downTo 1)
				for(y in 0..<engine.field.height) engine.field.getBlock(x2, y)?.replace(engine.field.getBlock(x2-1, y))
			for(y in 0..<engine.field.height) engine.field.delBlock(0, y)
		}
		for(x in 6..10) if((0..<engine.field.height).all {y -> engine.field.getBlockEmpty(x, y)}) {
			for(x2 in x..10) for(y in 0..<engine.field.height) engine.field.getBlock(x2, y)
				?.replace(engine.field.getBlock(x2+1, y))
			for(y in 0..<engine.field.height) engine.field.delBlock(11, y)
		}
	}

	private fun levelUp(engine:GameEngine, beginning:Boolean) {
		if(!beginning) engine.statistics.level++
		owner.bgMan.bg = engine.statistics.level%20
		val effectiveLevel:Int = engine.statistics.level
		spawnTimer = 0
		resetBlockArray()
		lineSpawn = tableLevelStartLines[effectiveLevel]
		linesLeft = tableLevelLine[effectiveLevel]
		force = false
		var result = 0
		var `is` = 0
		while(true) {
			if(tableLevelWeightShift[`is`+1]>=effectiveLevel) {
				result = `is`
				break
			}
			`is`++
		}
		multiplier = 0.75
		wRandomEngine.setWeights(tableColorWeights[result])
		wRandomEngineBomb.setWeights(tableBombColorWeights[result])
		when(difficulty) {
			0 -> spawnTimerLimit = (tableSpawnSpeedEasy[effectiveLevel]*1.5).toInt()
			1 -> spawnTimerLimit = (tableSpawnSpeedNormal[effectiveLevel]*1.5).toInt()
			2 -> spawnTimerLimit = (tableSpawnSpeedHard[effectiveLevel]*1.5).toInt()
		}
	}

	private fun midgameSpeedSet(engine:GameEngine):Double {
		return if(linesLeft>=maxSpeedLine) {
			val effectiveLevel:Int = engine.statistics.level
			val fraction = 0.75+0.75*((linesLeft-maxSpeedLine).toDouble()/(tableLevelLine[effectiveLevel]-maxSpeedLine))
			var rawSpeed = 1.0
			when(difficulty) {
				0 -> rawSpeed = fraction*tableSpawnSpeedEasy[effectiveLevel]
				1 -> rawSpeed = fraction*tableSpawnSpeedNormal[effectiveLevel]
				2 -> rawSpeed = fraction*tableSpawnSpeedHard[effectiveLevel]
			}
			val mod = rawSpeed%1
			spawnTimerLimit = if(mod>0) (rawSpeed+1).toInt() else rawSpeed.toInt()
			0.75-(fraction-1.5)
		} else if(linesLeft>=0) {
			1.5
		} else {
			val effectiveLevel:Int = engine.statistics.level
			var rawSpeed = 1.0
			when(difficulty) {
				0 -> rawSpeed = 0.75*tableSpawnSpeedEasy[effectiveLevel]
				1 -> rawSpeed = 0.75*tableSpawnSpeedNormal[effectiveLevel]
				2 -> rawSpeed = 0.75*tableSpawnSpeedHard[effectiveLevel]
			}
			val mod = rawSpeed%1
			spawnTimerLimit = if(mod>0) (rawSpeed+1).toInt() else rawSpeed.toInt()
			1.5
		}
	}

	private fun parseMouse(engine:GameEngine) {
		// XXX: SWING DOES NOT SUPPORT MOUSE INPUT. FALL BACK TO KEYBOARD INPUT.
		var changeX = 0
		var changeY = 0
		if(engine.ctrl.isPush(Controller.BUTTON_LEFT)) changeX--
		if(engine.ctrl.isPush(Controller.BUTTON_RIGHT)) changeX++
		if(engine.ctrl.isPush(Controller.BUTTON_UP)) changeY--
		if(engine.ctrl.isPush(Controller.BUTTON_DOWN)) changeY++
		fieldX += changeX
		fieldY += changeY
		if(changeX!=0||changeY!=0) engine.playSE("change")
		if(fieldX<0) fieldX = 11
		if(fieldX>11) fieldX = 0
		if(fieldY<0) fieldY = 15
		if(fieldY>15) fieldY = 0
		if(engine.ctrl.isPush(Controller.BUTTON_B)&&localState==LOCALSTATE_INGAME) force = true
		when(holderType) {
			HOLDER_SLICK -> {
				MouseInput.update(NullpoMinoSlick.appGameContainer.input)
				cursorX = MouseInput.mouseX
				cursorY = MouseInput.mouseY
				if(MouseInput.isMouseClicked) {
					fieldX = (cursorX-4-(receiver.fieldX(engine)-engine.frameX).toInt())/16
					fieldY = (cursorY-52-(receiver.fieldY(engine)-engine.frameY).toInt())/16
				} else {
					fieldX = -1
					fieldY = -1
				}
			}
			/*HOLDER_SDL -> {
				try {
					MouseInputSDL.update()
				} catch(e:java.lang.Exception) {
					// DO NOTHING
				}
				cursorX = MouseInputSDL.mouseX
				cursorY = MouseInputSDL.mouseY
				if(MouseInputSDL.isMouseClicked) {
					fieldX = (cursorX-4-receiver.fieldX(engine, playerID))/16
					fieldY = (cursorY-52-receiver.fieldY(engine, playerID))/16
				} else {
					fieldX = -1
					fieldY = -1
				}
			}
			else -> { }*/
		}
		if(fieldX<0||fieldX>11||fieldY<0||fieldY>15) {
			if(fieldY==17&&localState==LOCALSTATE_INGAME) force = true
			fieldX = -1
			fieldY = -1
		} else {
			force = false
		}
	}

	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.lives<=0) {
			// もう復活できないとき
			if(engine.statc[0]==0) {
				engine.gameEnded()
				engine.blockShowOutlineOnly = false
				if(owner.players<2) owner.musMan.bgm = BGMStatus.BGM.Silent
				if(engine.field.isEmpty) {
					engine.statc[0] = engine.field.height+1
				} else {
					engine.resetFieldVisible()
				}
			}
			if(engine.statc[0]<engine.field.height+1) {
				for(i in 0..<engine.field.width) {
					engine.field.getBlock(i, engine.field.height-engine.statc[0])?.let {blk ->
						if(!blk.getAttribute(Block.ATTRIBUTE.GARBAGE)) {
							blk.color = Block.COLOR.BLACK
							blk.setAttribute(true, Block.ATTRIBUTE.GARBAGE)
						}
						blk.darkness = 0.3f
						blk.elapsedFrames = -1
					}
				}
				engine.statc[0]++
			} else if(engine.statc[0]==engine.field.height+1) {
				engine.playSE("gameover")
				engine.statc[0]++
			} else if(engine.statc[0]<engine.field.height+1+180) {
				if(engine.statc[0]>=engine.field.height+1+60&&engine.ctrl.isPush(Controller.BUTTON_A)) {
					engine.statc[0] = engine.field.height+1+180
				}
				engine.statc[0]++
			} else {
				if(enableBombs) updateRanking(
					engine.statistics.score, difficulty, engine.statistics.level+1,
					engine.playerProp.isLoggedIn
				)
				if(rankingRank!=-1) saveRanking()
				if(rankingRankPlayer!=-1) saveRankingPlayer(engine.playerProp)

				owner.saveModeConfig()
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
				for(i in engine.field.hiddenHeight*-1..<engine.field.height)
					for(j in 0..<engine.field.width)
						engine.field.getBlock(j, i)?.let {it.color = Block.COLOR.BLACK}


				engine.statc[0] = 1
			}
			if(!engine.field.isEmpty) {
				engine.field.pushDown()
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

	private fun setNewLowerScore(engine:GameEngine) {
	}

	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		if(!engine.lagStop) {
			updateSTextArr()
			if(acTime in 0..119) acTime++ else acTime = -1
		}
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode||engine.stat===GameEngine.Status.CUSTOM) {
			// Show rank
			if(engine.ctrl.isPush(
					Controller.BUTTON_F
				)&&engine.playerProp.isLoggedIn&&engine.stat!==GameEngine.Status.CUSTOM
			) {
				showPlayerStats = !showPlayerStats
				engine.playSE("change")
			}
		}
	}

	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.ORANGE)
		receiver.drawScoreFont(
			engine, 0, 1, "("+DIFFICULTY_NAMES[difficulty]+" DIFFICULTY)", COLOR.ORANGE
		)
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&enableBombs&&engine.ai==null) {
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 3, topY-1, "SCORE    LEVEL", COLOR.BLUE)
				if(showPlayerStats) {
					for(i in 0..<MAX_RANKING) {
						receiver.drawScoreFont(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW)
						receiver.drawScoreFont(engine, 3, topY+i, "${rankingScorePlayer[difficulty][i]}", i==rankingRankPlayer)
						receiver.drawScoreFont(engine, 12, topY+i, "${rankingLevelPlayer[difficulty][i]}", i==rankingRankPlayer, scale)
					}
					receiver.drawScoreFont(engine, 0, topY+MAX_RANKING+1, "PLAYER SCORES", COLOR.BLUE)
					receiver.drawScoreFont(engine, 0, topY+MAX_RANKING+2, engine.playerProp.nameDisplay, COLOR.WHITE, 2f)
					receiver.drawScoreFont(engine, 0, topY+MAX_RANKING+5, "F:SWITCH RANK SCREEN", COLOR.GREEN)
				} else {
					for(i in 0..<MAX_RANKING) {
						receiver.drawScoreFont(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW)
						receiver.drawScoreFont(engine, 3, topY+i, "${rankingScore[difficulty][i]}", i==rankingRank)
						receiver.drawScoreFont(engine, 12, topY+i, "${rankingLevel[difficulty][i]}", i==rankingRank)
					}
					receiver.drawScoreFont(engine, 0, topY+MAX_RANKING+1, "LOCAL SCORES", COLOR.BLUE)
					if(!engine.playerProp.isLoggedIn)
						receiver.drawScoreFont(engine, 0, topY+MAX_RANKING+2, "(NOT LOGGED IN)\n(E:LOG IN)")
					if(engine.playerProp.isLoggedIn)
						receiver.drawScoreFont(engine, 0, topY+MAX_RANKING+5, "F:SWITCH RANK SCREEN", COLOR.GREEN)
				}
			}
		} else if(!engine.gameActive&&engine.stat===GameEngine.Status.CUSTOM) {
			engine.playerProp.loginScreen.renderScreen(receiver, engine)
		} else {
			receiver.drawScoreFont(engine, 0, 3, "SCORE", COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 4, "$scDisp")
			receiver.drawScoreFont(engine, 0, 6, "LEVEL", COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 7, (engine.statistics.level+1).toString())
			if(linesLeft>=0) {
				receiver.drawScoreFont(engine, 0, 9, "LINES LEFT", COLOR.BLUE)
				receiver.drawScoreFont(engine, 0, 10, "$linesLeft")
			}
			receiver.drawScoreFont(engine, 0, 12, "TIME", COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 13, engine.statistics.time.toTimeStr)
			if(engine.playerProp.isLoggedIn) {
				receiver.drawScoreFont(engine, 0, 15, "PLAYER", COLOR.BLUE)
				receiver.drawScoreFont(engine, 0, 16, engine.playerProp.nameDisplay, COLOR.WHITE, 2f)
			}
			sTextArr.forEach {
				val x = it.location[0]
				val y = it.location[1]
				val scale:Float
				val rs:Float
				val baseScale:Float = if(it.big) 2f else 1f
				val baseDim = if(it.big) 32.0 else 16.0
				if(it.lifeTime<24) {
					scale = baseScale
					rs = 1f
				} else {
					scale = baseScale-baseScale*((it.lifeTime-24).toFloat()/96)
					rs = 1-(it.lifeTime-24).toFloat()/96
				}
				var nOffX = 0
				var nOffY = 0
				if(rs<1) {
					nOffX = ((baseDim*it.text.length-baseDim*it.text.length*rs)/2).toInt()
					nOffY = ((baseDim-baseDim*rs)/2).toInt()
				}
				if(it.lifeTime/2%2==0&&it.largeClear) receiver.drawDirectFont(
					x+nOffX,
					y+nOffY,
					it.text,
					COLOR.YELLOW,
					scale
				) else receiver.drawDirectFont(x+nOffX, y+nOffY, it.text, COLOR.ORANGE, scale)
			}
			receiver.drawMenuFont(engine, fieldX, fieldY, "f", COLOR.YELLOW)

			if(localState==LOCALSTATE_TRANSITION) {
				val s = "$bScore"
				val l = s.length
				val offset = (12-l)/2
				receiver.drawMenuFont(
					engine, 2, 6, "LEVEL UP", if(engine.statc[0]/2%2==0) COLOR.YELLOW else COLOR.ORANGE
				)
				receiver.drawMenuFont(engine, 0, 8, "BONUS POINTS", COLOR.YELLOW)
				receiver.drawMenuFont(engine, offset, 9, s)
			}

//			receiver.drawScoreFont(engine, playerID, 0, 15, "MOUSE COORDS", EventReceiver.COLOR.BLUE);
//			receiver.drawScoreFont(engine, playerID, 0, 16, "($cursorX, $cursorY");
//
//			receiver.drawScoreFont(engine, playerID, 0, 18, "FIELD CELL CLICKED", EventReceiver.COLOR.BLUE);
//			receiver.drawScoreFont(engine, playerID, 0, 19, "($fieldX, $fieldY)");
			if(localState==LOCALSTATE_INGAME) {
				val fx = receiver.fieldX(engine)+4
				val fy = receiver.fieldY(engine)+52+17*16f
				nextBlocks.forEachIndexed {i, it ->
					receiver.drawBlock(fx+i*16f, fy, it)
				}
				val s = "${(100000*1.025.pow(engine.statistics.level)).toInt()}"
				val l = s.length
				val offset = (12-l)/2
				if(acTime in 0..119) {
					receiver.drawMenuFont(
						engine,
						1,
						6,
						"ALL CLEAR!",
						if(engine.statistics.time/2%2==0) COLOR.YELLOW else COLOR.ORANGE
					)
					receiver.drawMenuFont(engine, 0, 8, "BONUS POINTS", COLOR.YELLOW)
					receiver.drawMenuFont(engine, offset, 9, s)
				}
			}
		}
	}

	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 0, "SCORE", COLOR.BLUE)
		receiver.drawMenuFont(engine, 0, 1, "%12s".format(engine.statistics.score))
		receiver.drawMenuFont(engine, 0, 2, "LEVEL", COLOR.BLUE)
		receiver.drawMenuFont(engine, 0, 3, "%12s".format(engine.statistics.level+1))
		receiver.drawMenuFont(engine, 0, 4, "TIME", COLOR.BLUE)
		receiver.drawMenuFont(engine, 0, 5, "%12s".format(engine.statistics.time.toTimeStr))
	}
	/**
	 * Load settings from [prop]
	 *
	 * @param prop Property file
	 */
	override fun loadSetting(engine: GameEngine, prop: CustomProperties, ruleName: String, playerID: Int) {
		enableBombs = prop.getProperty("collapse.enableBombs", true)
		difficulty = prop.getProperty("collapse.difficulty", 1)
		bgm = prop.getProperty("collapse.bgm", 0)
	}
	/**
	 * Save settings to [prop]
	 *
	 * @param prop Property file
	 */
	override fun saveSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("collapse.enableBombs", enableBombs)
		prop.setProperty("collapse.difficulty", difficulty)
		prop.setProperty("collapse.bgm", bgm)
	}
	/*
		 * why do I even make stuff like this
		 * people just keep asking "why?" or just straight up calling it useless (even if they don't explicitly say it)
		 * nobody's going to look at this and think "wow this code is cool", they usually only see the surface level end product
		 * eh, whatever. might as well carry on to satiate my own wants for these modes and libraries
		 * screw everyone else
		 * I'll just make this for myself first, others later
		 * this isn't even original anyway, just piggybacking off an existing engine and other games for concepts
		 * you got people saying "your workflow is shit, your computer's OS is shit and you should be ashamed, you got people saying the sites you use are shit and you should feel bad" like
		 * what the hell do I do
		 * I just want to use my computer and develop shit for mostly fun
		 * nothing I do in my own time when I'm not interacting with you is hurting you
		 * ...fucking elitists...
		 * why the absolute FUCK do you care so much about what I do?
		 * where I source information about a game which has no official source (a game's speed difficulty ffs) does not hurt you in any way.
		 * okay then mr. "I'm-right-you're-wrong-fuck-you-and-your-choices".
		 * I'll go fuck off now, so you don't have to deal with my shit.
		 * poison the well before I even get to make a point will you.
		 * fuck off
		 */

	/**
	 * Update rankings
	 *
	 * @param sc Score
	 */
	private fun updateRanking(sc:Long, type:Int, lv:Int, isLoggedIn:Boolean) {
		rankingRank = checkRanking(sc, lv, type)
		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in MAX_RANKING-1 downTo rankingRank+1) {
				rankingScore[type][i] = rankingScore[type][i-1]
				rankingLevel[type][i] = rankingLevel[type][i-1]
			}

			// Add new data
			rankingScore[type][rankingRank] = sc
			rankingLevel[type][rankingRank] = lv
		}
		if(isLoggedIn) {
			rankingRankPlayer = checkRankingPlayer(sc, lv, type)
			if(rankingRankPlayer!=-1) {
				// Shift down ranking entries
				for(i in MAX_RANKING-1 downTo rankingRankPlayer+1) {
					rankingScorePlayer[type][i] = rankingScorePlayer[type][i-1]
					rankingLevelPlayer[type][i] = rankingLevelPlayer[type][i-1]
				}

				// Add new data
				rankingScorePlayer[type][rankingRankPlayer] = sc
				rankingLevelPlayer[type][rankingRankPlayer] = lv
			}
		} else rankingRankPlayer = -1
	}
	/**
	 * Calculate ranking position
	 *
	 * @param sc Score
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Long, lv:Int, type:Int):Int {
		for(i in 0..<MAX_RANKING) {
			if(sc>rankingScore[type][i]) {
				return i
			} else if(sc==rankingScore[type][i]&&lv<rankingLevel[type][i]) {
				return i
			}
		}
		return -1
	}
	/**
	 * Calculate ranking position
	 *
	 * @param sc Score
	 * @return Position (-1 if unranked)
	 */
	private fun checkRankingPlayer(sc:Long, lv:Int, type:Int):Int {
		for(i in 0..<MAX_RANKING) {
			if(sc>rankingScorePlayer[type][i]) return i
			else if(sc==rankingScorePlayer[type][i]&&lv<rankingLevelPlayer[type][i]) return i
		}
		return -1
	}
	/*
		 * ------ MODE-SPECIFIC PRIVATE METHODS ------
		 */
	private fun getClearScore(engine:GameEngine, squares:Int):Int =
		(18*squares*(squares/4.0)*((engine.statistics.level+1)/3.0)*
			1.015.pow(squares.toDouble())).toInt()

	private fun getLevelClearBonus(engine:GameEngine):Int {
		val h:Int = engine.field.height
		val w:Int = engine.field.width
		val s = maxOf(0, h*w-engine.field.howManyBlocks)

		return getClearScore(engine, s)/48
	}
	/**
	 * Checks if a coordinate is within a certain radius.
	 *
	 * @param x      X-coordinate of circle's center.
	 * @param y      Y-coordinate of circle's center.
	 * @param xTest  X-coordinate of test square.
	 * @param yTest  Y-coordinate of test square.
	 * @param radius The testing radius
	 * @return The result of the check. true: within. false: not within.
	 */
	private fun isCoordWithinRadius(
		x:Int, y:Int, xTest:Int, yTest:Int,
		radius:Double
	):Boolean {
		val dX = xTest-x
		val dY = yTest-y
		val distance = sqrt((dX*dX+dY*dY).toDouble())
		return distance<=radius
	}

	private fun explode(engine:GameEngine) {
		engine.playSE("bomb")
	}

	companion object {
		// Hey, have any of you played any of the Super Collapse games?
		// Field Dimensions: 12 x 16; 1 Hidden Height
		//
		// DONE: Create a weighted Randomizer for the blocks.
		// DONE: Do the center-direction column gravity thing... somehow.
		// DONE: Implement mouse control if you can.
		// DONE: Implement flying score popups.
		private val tableColors = arrayOf(
			Block.COLOR.RED,
			Block.COLOR.BLUE,
			Block.COLOR.YELLOW,
			Block.COLOR.GREEN,
			Block.COLOR.ORANGE,
			Block.COLOR.WHITE // Set as Silver blocks for silver blocks, movable but only break by super bombs
			// .// Use Gray Bombs for super bombs, as will be shown as rainbow gems.
		)
		private val tableColorWeights = arrayOf(
			intArrayOf(1, 1, 1, 0, 0, 0), intArrayOf(160, 160, 160, 0, 0, 1),
			intArrayOf(160, 160, 160, 80, 0, 2), intArrayOf(160, 160, 160, 120, 0, 3), intArrayOf(160, 160, 160, 160, 0, 4),
			intArrayOf(192, 192, 192, 192, 96, 11), intArrayOf(204, 204, 204, 204, 204, 18)
		)
		private val tableBombColorWeights = arrayOf(
			intArrayOf(0, 0, 0, 0, 0, 1), intArrayOf(5, 5, 5, 0, 0, 15),
			intArrayOf(4, 4, 4, 4, 0, 16), intArrayOf(4, 4, 4, 4, 0, 16), intArrayOf(4, 4, 4, 4, 0, 16),
			intArrayOf(8, 8, 8, 8, 8, 40), intArrayOf(8, 8, 8, 8, 8, 40)
		)
		private val tableLevelWeightShift = intArrayOf(
			0, 3, 6, 9, 12, 15, 18, 10000
		)
		private val tableLevelStartLines = intArrayOf(
			3, 4, 4, 5, 5, 5,
			6, 6, 6, 7, 7, 7,
			8, 8, 8, 9, 9, 9,
			10, 10, 10
		) // MAX: 2.5 row/s
		private val tableSpawnSpeedNormal = intArrayOf(
			15, 15, 15, 14, 14, 13,
			13, 12, 12, 11, 11, 10,
			10, 9, 8, 7, 6, 5,
			4, 3, 2
		) // MAX: 2.5 row/s
		private val tableSpawnSpeedEasy = intArrayOf(
			20, 19, 18, 17, 16, 15,
			14, 13, 12, 12, 11, 11,
			10, 10, 9, 9, 8, 8,
			7, 6, 5
		) // MAX: 1 row/s
		private val tableSpawnSpeedHard = intArrayOf(
			12, 12, 12, 12, 11, 11,
			11, 10, 10, 9, 8, 7,
			6, 5, 4, 4, 3, 3,
			2, 2, 1
		) // MAX: 5 row/s
		private val tableLevelLine = intArrayOf(
			20, 30, 40, 50, 60, 70,
			80, 90, 100, 120, 140, 160,
			180, 200, 240, 280, 320, 360,
			400, -1
		)

		private const val FIELD_WIDTH = 12
		private const val FIELD_HEIGHT = 16
		private const val FIELD_HIDDEN_HEIGHT = 1
		private const val bombChance = 0.01
		private const val MAX_RANKING = 10
		private const val MAX_DIFFICULTIES = 3
		private val DIFFICULTY_NAMES = arrayOf("EASY", "NORMAL", "HARD")
		private const val maxSpeedLine = 8
		private const val HOLDER_SLICK = 0
		private const val LOCALSTATE_INGAME = 0
		private const val LOCALSTATE_TRANSITION = 1
	}
}
