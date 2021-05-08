/* Copyright (c) 2010, NullNoname
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of NullNoname nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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
 * POSSIBILITY OF SUCH DAMAGE. */
package mu.nu.nullpo.game.subsystem.mode.another

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import kotlin.random.Random

/** AVALANCHE FEVER MARATHON mode (Release Candidate 2) */
class Avalanche1PFever:Avalanche1PDummyMode() {

	/** Selected game type */
	private var mapSet:Int = 0

	/** Version number */
	private var version:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = 0

	/** Rankings' line counts */
	private var rankingScore:Array<Array<IntArray>>? = null

	/** Rankings' times */
	private var rankingTime:Array<Array<IntArray>>? = null

	/** Flag for all clear */
	private var zenKeshiDisplay:Int = 0

	/** Time limit left */
	private var timeLimit:Int = 0

	/** Time added to limit */
	private var timeLimitAdd:Int = 0

	/** Time to display added time */
	private var timeLimitAddDisplay:Int = 0

	/** Fever values CustomProperties */
	private var propFeverMap:CustomProperties = CustomProperties()

	/** Chain levels for Fever Mode */
	private var feverChain:Int = 0

	/** Chain level boundaries for Fever Mode */
	private var feverChainMin:Int = 0
	private var feverChainMax:Int = 0

	/** Flag set to true when last piece caused a clear */
	private var cleared:Boolean = false

	/** List of subsets in selected values */
	private var mapSubsets:Array<String>? = null

	/** Fever chain count when last chain hit occurred */
	private var feverChainDisplay:Int = 0

	/** Type of chain display */
	private var chainDisplayType:Int = 0

	/** Number of boards played */
	private var boardsPlayed:Int = 0

	/** Level at start of chain */
	private var chainLevelMultiplier:Int = 0

	/** Fast-forward settings for debug use */
	private var fastenable:Int = 0

	/** Flag set when fast-forward is enabled */
	private var fastinuse:Boolean = false

	/** Indices for values previews */
	private var previewChain:Int = 0
	private var previewSubset:Int = 0

	/** ??? */
	private var xyzzy:Int = 0

	/* Mode name */
	override val name:String = "AVALANCHE 1P FEVER MARATHON (RC2)"
	override val gameIntensity:Int = 1
	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)

		cleared = false
		boardsPlayed = 0

		timeLimit = TIME_LIMIT
		timeLimitAdd = 0
		timeLimitAddDisplay = 0

		feverChainDisplay = 0
		chainDisplayType = 0

		feverChain = 5

		rankingRank = -1
		rankingScore = Array(3) {Array(FEVER_MAPS.size) {IntArray(RANKING_MAX)}}
		rankingTime = Array(3) {Array(FEVER_MAPS.size) {IntArray(RANKING_MAX)}}

		xyzzy = 0
		fastenable = 0
		fastinuse = false
		previewChain = 5
		previewSubset = 0

		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.recordProp, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else
			loadSetting(owner.replayProp)
	}

	public override fun readyInit(engine:GameEngine, playerID:Int):Boolean {
		cascadeSlow = true
		super.readyInit(engine, playerID)
		loadMapSetFever(engine, playerID, mapSet, true)
		loadFeverMap(engine, feverChain)
		timeLimit = TIME_LIMIT
		timeLimitAdd = 0
		timeLimitAddDisplay = 0
		chainLevelMultiplier = level
		return false
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, if(xyzzy==573) 8 else 4)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {

					0, 6 -> {
						mapSet += change
						if(mapSet<0) mapSet = FEVER_MAPS.size-1
						if(mapSet>FEVER_MAPS.size-1) mapSet = 0
						if(xyzzy==573) {
							loadMapSetFever(engine, playerID, mapSet, true)
							if(previewChain<feverChainMin) previewChain = feverChainMax
							if(previewChain>feverChainMax) previewChain = feverChainMin
							if(previewSubset>=mapSubsets!!.size) previewSubset = 0
						}
					}
					1 -> {
						outlinetype += change
						if(outlinetype<0) outlinetype = 2
						if(outlinetype>2) outlinetype = 0
					}
					2 -> {
						numColors += change
						if(numColors<3) numColors = 5
						if(numColors>5) numColors = 3
					}
					3 -> {
						chainDisplayType += change
						if(chainDisplayType<0) chainDisplayType = 2
						if(chainDisplayType>2) chainDisplayType = 0
					}
					4 -> bigDisplay = !bigDisplay
					5 -> {
						fastenable += change
						if(fastenable<0) fastenable = 2
						if(fastenable>2) fastenable = 0
					}
					7 -> {
						previewSubset += change
						if(previewSubset<0) previewSubset = mapSubsets!!.size-1
						if(previewSubset>=mapSubsets!!.size) previewSubset = 0
					}
					8 -> {
						previewChain += change
						if(previewChain<feverChainMin) previewChain = feverChainMax
						if(previewChain>feverChainMax) previewChain = feverChainMin
					}
				}
				if(mapSet==4) numColors = 3
			}

			if(xyzzy!=573) {
				if(engine.ctrl.isPush(Controller.BUTTON_UP))
					if(xyzzy==1)
						xyzzy++
					else if(xyzzy!=2) xyzzy = 1
				if(engine.ctrl.isPush(Controller.BUTTON_DOWN))
					if(xyzzy==2||xyzzy==3)
						xyzzy++
					else
						xyzzy = 0
				if(engine.ctrl.isPush(Controller.BUTTON_LEFT))
					if(xyzzy==4||xyzzy==6)
						xyzzy++
					else
						xyzzy = 0
				if(engine.ctrl.isPush(Controller.BUTTON_RIGHT))
					if(xyzzy==5||xyzzy==7)
						xyzzy++
					else
						xyzzy = 0
			}

			if(engine.ctrl.isPush(Controller.BUTTON_A))
				if(xyzzy==573&&menuCursor>5) {
					loadMapSetFever(engine, playerID, mapSet, true)
					loadFeverMap(engine, Random.Default, previewChain, previewSubset)
				} else if(xyzzy==9) {
					engine.playSE("levelup")
					xyzzy = 573
					loadMapSetFever(engine, playerID, mapSet, true)
				} else if(menuTime>=5) {
					// 決定
					engine.playSE("decide")
					saveSetting(owner.modeConfig)
					owner.saveModeConfig()
					return false
				}

			if(engine.ctrl.isPush(Controller.BUTTON_B))
				if(xyzzy==8)
					xyzzy++
				else
				// Cancel
					engine.quitflag = true

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}

		return true
	}

	/* When the piece is movable */
	override fun renderMove(engine:GameEngine, playerID:Int) {
		if(engine.gameStarted) drawXorTimer(engine, playerID)
	}

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(menuCursor<=5) {
			var strOutline = ""
			if(outlinetype==0) strOutline = "NORMAL"
			if(outlinetype==1) strOutline = "COLOR"
			if(outlinetype==2) strOutline = "NONE"

			initMenu(0, EventReceiver.COLOR.BLUE, 0)
			drawMenu(engine, playerID, receiver, "MAP SET", FEVER_MAPS[mapSet].uppercase(), "OUTLINE", strOutline, "COLORS",
				"$numColors", "SHOW CHAIN", CHAIN_DISPLAY_NAMES[chainDisplayType], "BIG DISP", GeneralUtil.getONorOFF(bigDisplay))
			if(xyzzy==573) drawMenu(engine, playerID, receiver, "FAST", FAST_NAMES[fastenable])
		} else {
			receiver.drawMenuFont(engine, playerID, 0, 13, "MAP PREVIEW", EventReceiver.COLOR.YELLOW)
			receiver.drawMenuFont(engine, playerID, 0, 14, "A:DISPLAY", EventReceiver.COLOR.GREEN)
			drawMenu(engine, playerID, receiver, 15, EventReceiver.COLOR.BLUE, 6, "MAP SET", FEVER_MAPS[mapSet].uppercase(), "SUBSET",
				mapSubsets!![previewSubset].uppercase(), "CHAIN", "$previewChain")
		}
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "AVALANCHE FEVER MARATHON", EventReceiver.COLOR.COBALT)
		receiver.drawScoreFont(engine, playerID, 0, 1, "(${FEVER_MAPS[mapSet].uppercase()} $numColors COLORS)",
			EventReceiver.COLOR.COBALT)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4

				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE      TIME", EventReceiver.COLOR.BLUE, scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, topY+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW, scale)
					receiver.drawScoreFont(engine, playerID, 3, topY+i, "${rankingScore!![numColors-3][mapSet][i]}", i==rankingRank,
						scale)
					receiver.drawScoreNum(engine, playerID, 14, topY+i, GeneralUtil.getTime(rankingTime!![numColors-3][mapSet][i]),
						i==rankingRank, scale)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "Score", EventReceiver.COLOR.BLUE)
			val strScore:String = if(lastscore==0||lastmultiplier==0||scgettime<=0)
				"${engine.statistics.score}"
			else "${engine.statistics.score}(+${lastscore}X$lastmultiplier)"
			receiver.drawScoreNum(engine, playerID, 0, 4, strScore, 2f)

			receiver.drawScoreFont(engine, playerID, 0, 6, "Level", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 7, "$level")

			receiver.drawScoreFont(engine, playerID, 0, 9, "Time Limit", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, GeneralUtil.getTime(timeLimit), 2f)
			if(timeLimitAddDisplay>0) receiver.drawScoreFont(engine, playerID, 0, 14, "(+${timeLimitAdd/60} SEC.)")

			receiver.drawScoreFont(engine, playerID, 0, 12, "Played", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 13, GeneralUtil.getTime(engine.statistics.time))

			receiver.drawScoreFont(engine, playerID, 11, 6, "Boards #", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 11, 7, "$boardsPlayed")

			receiver.drawScoreFont(engine, playerID, 11, 9, "Cleaned", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 11, 10, "$zenKeshiCount")

			receiver.drawScoreFont(engine, playerID, 11, 12, "Longest Chain", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 11, 13, engine.statistics.maxChain.toString())

			receiver.drawScoreFont(engine, playerID, 11, 15, "Total Power", EventReceiver.COLOR.BLUE)
			var strSent = "$garbageSent"
			if(garbageAdd>0) strSent = "$strSent(+$garbageAdd)"
			receiver.drawScoreFont(engine, playerID, 11, 16, strSent)

			receiver.drawScoreFont(engine, playerID, 11, 18, "Erased", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 11, 19, "$blocksCleared")

			if(engine.gameStarted&&engine.stat!=GameEngine.Status.MOVE
				&&engine.stat!=GameEngine.Status.RESULT)
				drawXorTimer(engine, playerID)

			if(!engine.gameActive) return

			val textHeight = if(engine.displaysize==1) 11 else (engine.field?.height ?: 12)+1

			val baseX = if(engine.displaysize==1) 1 else 0
			if(engine.chain>0&&chainDisplay>0&&chainDisplayType!=0) {
				var color = EventReceiver.COLOR.YELLOW
				if(chainDisplayType==2)
					when {
						engine.chain>=feverChainDisplay -> color = EventReceiver.COLOR.GREEN
						engine.chain==feverChainDisplay-2 -> color = EventReceiver.COLOR.ORANGE
						engine.chain<feverChainDisplay-2 -> color = EventReceiver.COLOR.RED
					}
				receiver.drawMenuFont(engine, playerID, baseX+if(engine.chain>9) 0 else 1, textHeight, "${engine.chain} CHAIN!", color)
			}
			if(zenKeshiDisplay>0)
				receiver.drawMenuFont(engine, playerID, baseX, textHeight+1, "ZENKESHI!", EventReceiver.COLOR.YELLOW)
		}
	}

	/** Draw fever timer on death columns
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	private fun drawXorTimer(engine:GameEngine, playerID:Int) {
		val strFeverTimer = String.format("%02d", (timeLimit+59)/60)

		for(i in 0..1)
			if(engine.field==null||engine.field!!.getBlockEmpty(2+i, 0))
				if(engine.displaysize==1)
					receiver.drawMenuFont(engine, playerID, 4+i*2, 0, "${strFeverTimer[i]}",
						if(timeLimit<360) EventReceiver.COLOR.RED else EventReceiver.COLOR.WHITE, 2f)
				else
					receiver.drawMenuFont(engine, playerID, 2+i, 0, "${strFeverTimer[i]}",
						if(timeLimit<360) EventReceiver.COLOR.RED else EventReceiver.COLOR.WHITE)
	}

	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		cleared = false
		zenKeshi = false
		return false
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		if(scgettime>0) scgettime--

		if(engine.timerActive) {
			if(chainDisplay>0) chainDisplay--
			if(zenKeshiDisplay>0) zenKeshiDisplay--
			if(timeLimit>0) {
				timeLimit--
				if(timeLimit in 1..360&&timeLimit%60==0)
					engine.playSE("countdown")
				else if(timeLimit==0) engine.playSE("levelstop")
			}
		}
		if(timeLimitAddDisplay>0) timeLimitAddDisplay--

		// Time meter
		engine.meterValue = timeLimit*receiver.getMeterMax(engine)/TIME_LIMIT
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(timeLimit<=1800) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(timeLimit<=900) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(timeLimit<=300) engine.meterColor = GameEngine.METER_COLOR_RED

		if(!fastinuse&&engine.ctrl.isPress(Controller.BUTTON_F)&&
			(fastenable==2||engine.stat==GameEngine.Status.LINECLEAR&&fastenable==1)) {
			fastinuse = true
			for(i in 0..3)
				engine.owner.updateAll()
			fastinuse = false
		}
	}

	override fun calcOjama(score:Int, avalanche:Int, pts:Int, multiplier:Int):Int =
		(avalanche*10*multiplier+ojamaRate-1)/ojamaRate

	override fun calcPts(avalanche:Int):Int = avalanche*chainLevelMultiplier*10

	override fun calcChainMultiplier(chain:Int):Int {
		return if(chain>CHAIN_POWERS.size)
			CHAIN_POWERS[CHAIN_POWERS.size-1]
		else
			CHAIN_POWERS[chain-1]
	}

	override fun onClear(engine:GameEngine, playerID:Int) {
		chainDisplay = 60
		cleared = true
		feverChainDisplay = feverChain
		if(engine.chain==1) chainLevelMultiplier = level
	}

	override fun lineClearEnd(engine:GameEngine, playerID:Int):Boolean {
		if(garbageAdd>0) {
			garbageSent += garbageAdd
			garbageAdd = 0
		}

		if(cleared) {
			boardsPlayed++
			timeLimitAdd = 0
			var newFeverChain = maxOf(engine.chain+1, feverChain-2)
			if(zenKeshi) {
				timeLimitAdd += 180
				zenKeshiDisplay = 120
				newFeverChain += 2
			}
			if(newFeverChain<feverChainMin) newFeverChain = feverChainMin
			if(newFeverChain>feverChainMax) newFeverChain = feverChainMax
			if(newFeverChain>feverChain)
				engine.playSE("cool")
			else if(newFeverChain<feverChain) engine.playSE("regret")
			feverChain = newFeverChain
			if(timeLimit>0) {
				timeLimitAdd += maxOf(0, (engine.chain-2)*60)
				if(timeLimitAdd>0) {
					timeLimit += timeLimitAdd
					timeLimitAddDisplay = 120
				}
				loadFeverMap(engine, feverChain)
			}
		} else if(engine.field!=null)
			if(!engine.field!!.getBlockEmpty(2, 0)||!engine.field!!.getBlockEmpty(3, 0)) {
				engine.stat = GameEngine.Status.GAMEOVER
				engine.gameEnded()
				engine.resetStatc()
				engine.statc[1] = 1
			}

		// Out of time
		if(timeLimit<=0&&engine.timerActive) {
			engine.gameEnded()
			engine.resetStatc()
			engine.stat = GameEngine.Status.ENDINGSTART
		}
		return false
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 1, "PLAY DATA", EventReceiver.COLOR.ORANGE)

		receiver.drawMenuFont(engine, playerID, 0, 3, "Score", EventReceiver.COLOR.BLUE)
		val strScoreBefore = String.format("%10d", scoreBeforeBonus)
		receiver.drawMenuNum(engine, playerID, 0, 4, strScoreBefore, EventReceiver.COLOR.GREEN)

		receiver.drawMenuFont(engine, playerID, 0, 5, "Clean Bonus", EventReceiver.COLOR.BLUE)
		val strZenKeshi = String.format("%10d", zenKeshiCount)
		receiver.drawMenuNum(engine, playerID, 0, 6, strZenKeshi)
		val strZenKeshiBonus = "+$zenKeshiBonus"
		receiver.drawMenuFont(engine, playerID, 10-strZenKeshiBonus.length, 7, strZenKeshiBonus, EventReceiver.COLOR.GREEN)

		receiver.drawMenuFont(engine, playerID, 0, 8, "Chain Bonus", EventReceiver.COLOR.BLUE)
		val strMaxChain = String.format("%10d", engine.statistics.maxChain)
		receiver.drawMenuFont(engine, playerID, 0, 9, strMaxChain)
		val strMaxChainBonus = "+$maxChainBonus"
		receiver.drawMenuFont(engine, playerID, 10-strMaxChainBonus.length, 10, strMaxChainBonus, EventReceiver.COLOR.GREEN)

		receiver.drawMenuFont(engine, playerID, 0, 11, "TOTAL", EventReceiver.COLOR.BLUE)
		val strScore = String.format("%10d", engine.statistics.score)
		receiver.drawMenuFont(engine, playerID, 0, 12, strScore, EventReceiver.COLOR.RED)

		receiver.drawMenuFont(engine, playerID, 0, 13, "Time", EventReceiver.COLOR.BLUE)
		val strTime = String.format("%10s", GeneralUtil.getTime(engine.statistics.time))
		receiver.drawMenuNum(engine, playerID, 0, 14, strTime)

		if(rankingRank!=-1) {
			receiver.drawMenuFont(engine, playerID, 0, 15, "RANK", EventReceiver.COLOR.BLUE)
			val strRank = String.format("%10d", rankingRank+1)
			receiver.drawMenuNum(engine, playerID, 0, 16, strRank)
		}
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)

		// Update rankings
		if(!owner.replayMode&&engine.ai==null) {
			updateRanking(engine.statistics.score, engine.statistics.time, mapSet, numColors)

			if(rankingRank!=-1) {
				saveRanking(owner.recordProp, engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
		}
	}

	/** Load settings from property file
	 * @param prop Property file
	 */
	override fun loadSetting(prop:CustomProperties) {
		mapSet = prop.getProperty("avalanchefever.gametype", 0)
		outlinetype = prop.getProperty("avalanchefever.outlinetype", 0)
		numColors = prop.getProperty("avalanchefever.numcolors", 4)
		version = prop.getProperty("avalanchefever.version", 0)
		chainDisplayType = prop.getProperty("avalanchefever.chainDisplayType", 1)
		bigDisplay = prop.getProperty("avalanchefever.bigDisplay", false)
	}

	/** Save settings to property file
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("avalanchefever.gametype", mapSet)
		prop.setProperty("avalanchefever.outlinetype", outlinetype)
		prop.setProperty("avalanchefever.numcolors", numColors)
		prop.setProperty("avalanchefever.version", version)
		prop.setProperty("avalanchefever.chainDisplayType", chainDisplayType)
		prop.setProperty("avalanchefever.bigDisplay", bigDisplay)
	}

	private fun loadMapSetFever(engine:GameEngine, playerID:Int, id:Int, forceReload:Boolean) {
		if(propFeverMap==null||forceReload) {
			receiver.loadProperties("config/map/avalanche/${FEVER_MAPS[id]}Endless.map")?.let {propFeverMap = it}
			feverChainMin = propFeverMap.getProperty("minChain", 3)
			feverChainMax = propFeverMap.getProperty("maxChain", 15)
			val subsets = propFeverMap.getProperty("sets")
			mapSubsets = subsets.split(",".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()
		}
	}

	private fun loadFeverMap(engine:GameEngine, chain:Int) {
		loadFeverMap(engine, engine.random, chain, engine.random.nextInt(mapSubsets!!.size))
	}

	private fun loadFeverMap(engine:GameEngine, rand:Random, chain:Int, subset:Int) {
		engine.createFieldIfNeeded()
		engine.field!!.reset()
		engine.field!!.stringToField(propFeverMap.getProperty("${mapSubsets!![subset]}.${numColors}colors.${chain}chain"))
		engine.field!!.setBlockLinkByColor()
		engine.field!!.setAllAttribute(false, Block.ATTRIBUTE.GARBAGE, Block.ATTRIBUTE.ANTIGRAVITY)
		engine.field!!.setAllSkin(engine.skin)
		engine.field!!.shuffleColors(BLOCK_COLORS, numColors, rand)
	}

	/** Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(j in FEVER_MAPS.indices)
				for(colors in 3..5) {
					rankingScore!![colors-3][j][i] = prop.getProperty(
						"avalanchefever.ranking.$ruleName.${colors}colors.${FEVER_MAPS[j]}.score.$i", 0)
					rankingTime!![colors-3][j][i] = prop.getProperty(
						"avalanchefever.ranking.$ruleName.${colors}colors.${FEVER_MAPS[j]}.time.$i", -1)
				}
	}

	/** Save rankings to property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	fun saveRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(j in FEVER_MAPS.indices)
				for(colors in 3..5) {
					prop.setProperty("avalanchefever.ranking.$ruleName.${colors}colors.${FEVER_MAPS[j]}.score.$i",
						rankingScore!![colors-3][j][i])
					prop.setProperty("avalanchefever.ranking.$ruleName.${colors}colors.${FEVER_MAPS[j]}.time.$i",
						rankingTime!![colors-3][j][i])
				}
	}

	/** Update rankings
	 * @param sc Score
	 * @param time Time
	 */
	private fun updateRanking(sc:Int, time:Int, type:Int, colors:Int) {
		rankingRank = checkRanking(sc, time, type, colors)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore!![colors-3][type][i] = rankingScore!![colors-3][type][i-1]
				rankingTime!![colors-3][type][i] = rankingTime!![colors-3][type][i-1]
			}

			// Add new data
			rankingScore!![colors-3][type][rankingRank] = sc
			rankingTime!![colors-3][type][rankingRank] = time
		}
	}

	/** Calculate ranking position
	 * @param sc Score
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Int, time:Int, type:Int, colors:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(sc>rankingScore!![colors-3][type][i])
				return i
			else if(sc==rankingScore!![colors-3][type][i]&&time<rankingTime!![colors-3][type][i]) return i

		return -1
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0

		private val CHAIN_POWERS = intArrayOf(4, 10, 18, 22, 30, 48, 80, 120, 160, 240, 280, 288, 342, 400, 440, 480, 520, 560, 600,
			640, 680, 720, 760, 800 //Amitie
		)

		/** Names of chain display settings */
		private val CHAIN_DISPLAY_NAMES = arrayOf("OFF", "YELLOW", "SIZE")

		/** Number of ranking records */
		private const val RANKING_MAX = 10

		/** Time limit */
		private const val TIME_LIMIT = 3600

		/** Names of fast-forward settings */
		private val FAST_NAMES = arrayOf("OFF", "CLEAR", "ALL")
	}
}
