/*
 * Copyright (c) 2010-2023, NullNoname
 * Kotlin converted and modified by Venom=Nhelv.
 * THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
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
package mu.nu.nullpo.game.subsystem.mode.another

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR.BLUE
import mu.nu.nullpo.game.event.EventReceiver.COLOR.COBALT
import mu.nu.nullpo.game.event.EventReceiver.COLOR.GREEN
import mu.nu.nullpo.game.event.EventReceiver.COLOR.ORANGE
import mu.nu.nullpo.game.event.EventReceiver.COLOR.RED
import mu.nu.nullpo.game.event.EventReceiver.COLOR.WHITE
import mu.nu.nullpo.game.event.EventReceiver.COLOR.YELLOW
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import kotlin.random.Random

/** AVALANCHE FEVER MARATHON mode (Release Candidate 2) */
class Avalanche1PFever:Avalanche1PDummyMode() {
	/** Selected game type */
	private var mapSet = 0

	/** Version number */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Rankings' line counts */
	private val rankingScore = List(3) {List(FEVER_MAPS.size) {MutableList(RANKING_MAX) {0L}}}

	/** Rankings' times */
	private val rankingTime = List(3) {List(FEVER_MAPS.size) {MutableList(RANKING_MAX) {-1}}}

	/** Flag for all clear */
	private var zenKeshiDisplay = 0

	/** Time limit left */
	private var timeLimit = 0

	/** Time added to limit */
	private var timeLimitAdd = 0

	/** Time to display added time */
	private var timeLimitAddDisplay = 0

	/** Fever values CustomProperties */
	private var propFeverMap = CustomProperties()

	/** Chain levels for Fever Mode */
	private var feverChain = 0

	/** Chain level boundaries for Fever Mode */
	private var feverChainMin = 0
	private var feverChainMax = 0

	/** Flag set to true when last piece caused a clear */
	private var cleared = false

	/** List of subsets in selected values */
	private var mapSubsets = List(0) {""}

	/** Fever chain count when last chain hit occurred */
	private var feverChainDisplay = 0

	/** Type of chain display */
	private var chainDisplayType = 0

	/** Number of boards played */
	private var boardsPlayed = 0

	/** Level at start of chain */
	private var chainLevelMultiplier = 0

	/** Fast-forward settings for debug use */
	private var fastenable = 0

	/** Flag set when fast-forward is enabled */
	private var fastinuse = false

	/** Indices for values previews */
	private var previewChain = 0
	private var previewSubset = 0

	/** ??? */
	private var xyzzy = 0

	override val rankMap
		get() = rankMapOf(rankingScore.flatMapIndexed {a, x ->
			x.mapIndexed {b, y -> "$a.$b.score" to y}
		}
			+rankingTime.flatMapIndexed {a, x ->
			x.mapIndexed {b, y -> "$a.$b.time" to y}
		})
	/* Mode name */
	override val name = "AVALANCHE 1P FEVER MARATHON (RC2)"
	override val gameIntensity = 1
	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		val pid = engine.playerID
		super.playerInit(engine)

		cleared = false
		boardsPlayed = 0

		timeLimit = TIME_LIMIT
		timeLimitAdd = 0
		timeLimitAddDisplay = 0

		feverChainDisplay = 0
		chainDisplayType = 0

		feverChain = 5

		rankingRank = -1
		rankingScore.forEach {it.forEach {p -> p.fill(0)}}
		rankingTime.forEach {it.forEach {p -> p.fill(0)}}

		xyzzy = 0
		fastenable = 0
		fastinuse = false
		previewChain = 5
		previewSubset = 0

		if(!owner.replayMode) {
			version = CURRENT_VERSION
		}
	}

	override fun readyInit(engine:GameEngine):Boolean {
		cascadeSlow = true
		super.readyInit(engine)
		loadMapSetFever(mapSet, true)
		loadFeverMap(engine, feverChain)
		timeLimit = TIME_LIMIT
		timeLimitAdd = 0
		timeLimitAddDisplay = 0
		chainLevelMultiplier = level
		return false
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
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
							loadMapSetFever(mapSet, true)
							if(previewChain<feverChainMin) previewChain = feverChainMax
							if(previewChain>feverChainMax) previewChain = feverChainMin
							if(previewSubset>=mapSubsets.size) previewSubset = 0
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
						if(previewSubset<0) previewSubset = mapSubsets.size-1
						if(previewSubset>=mapSubsets.size) previewSubset = 0
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
					loadMapSetFever(mapSet, true)
					loadFeverMap(engine, Random.Default, previewChain, previewSubset)
				} else if(xyzzy==9) {
					engine.playSE("levelup")
					xyzzy = 573
					loadMapSetFever(mapSet, true)
				} else if(menuTime>=5) {
					// 決定
					engine.playSE("decide")
					return false
				}

			if(engine.ctrl.isPush(Controller.BUTTON_B))
				if(xyzzy==8)
					xyzzy++
				else
				// Cancel
					engine.quitFlag = true
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}

		return true
	}

	/* When the piece is movable */
	override fun renderMove(engine:GameEngine) {
		if(engine.gameStarted) drawX(engine)
	}

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine) {
		if(menuCursor<=5) {
			val strOutline = when(outlinetype) {
				1 -> "COLOR"
				2 -> "NONE"
				else -> "NORMAL"
			}

			drawMenu(
				engine, receiver, 0, BLUE, 0, "MAP SET" to FEVER_MAPS[mapSet].uppercase(), "OUTLINE" to strOutline,
				"COLORS" to numColors, "SHOW CHAIN" to CHAIN_DISPLAY_NAMES[chainDisplayType],
				"BIG DISP" to bigDisplay
			)
			if(xyzzy==573) drawMenu(engine, receiver, "FAST" to FAST_NAMES[fastenable])
		} else {
			receiver.drawMenuFont(engine, 0, 13, "MAP PREVIEW", YELLOW)
			receiver.drawMenuFont(engine, 0, 14, "A:DISPLAY", GREEN)
			drawMenu(
				engine,
				receiver,
				15,
				BLUE,
				6,
				"MAP SET" to FEVER_MAPS[mapSet].uppercase(),
				"SUBSET" to mapSubsets[previewSubset].uppercase(),
				"CHAIN" to previewChain
			)
		}
	}

	/* Render score */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 0, "AVALANCHE FEVER MARATHON", COBALT)
		receiver.drawScoreFont(
			engine, 0, 1, "(${FEVER_MAPS[mapSet].uppercase()} $numColors COLORS)", COBALT
		)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&engine.ai==null) {
				val topY = if(receiver.nextDisplayType==2) 6 else 4

				receiver.drawScoreFont(engine, 3, topY-1, "SCORE      TIME", BLUE)

				for(i in 0..<RANKING_MAX) {
					receiver.drawScoreGrade(engine, 0, topY+i, "%2d".format(i+1), YELLOW)
					receiver.drawScoreFont(engine, 3, topY+i, "${rankingScore[numColors-3][mapSet][i]}", i==rankingRank)
					receiver.drawScoreNum(engine, 14, topY+i, rankingTime[numColors-3][mapSet][i].toTimeStr, i==rankingRank)
				}
			}
		} else {
			receiver.drawScoreFont(engine, 0, 3, "Score", BLUE)
			val strScore = "${engine.statistics.score}(+${lastScore}X$lastmultiplier)"
			receiver.drawScoreNum(engine, 0, 4, strScore, 2f)

			receiver.drawScoreFont(engine, 0, 6, "Level", BLUE)
			receiver.drawScoreNum(engine, 0, 7, "$level")

			receiver.drawScoreFont(engine, 0, 9, "Time Limit", BLUE)
			receiver.drawScoreNum(engine, 0, 10, timeLimit.toTimeStr, 2f)
			if(timeLimitAddDisplay>0) receiver.drawScoreFont(engine, 0, 14, "(+${timeLimitAdd/60} SEC.)")

			receiver.drawScoreFont(engine, 0, 12, "Played", BLUE)
			receiver.drawScoreNum(engine, 0, 13, engine.statistics.time.toTimeStr)

			receiver.drawScoreFont(engine, 11, 6, "Boards #", BLUE)
			receiver.drawScoreNum(engine, 11, 7, "$boardsPlayed")

			receiver.drawScoreFont(engine, 11, 9, "Cleaned", BLUE)
			receiver.drawScoreNum(engine, 11, 10, "$zenKeshiCount")

			receiver.drawScoreFont(engine, 11, 12, "Longest Chain", BLUE)
			receiver.drawScoreNum(engine, 11, 13, engine.statistics.maxChain.toString())

			receiver.drawScoreFont(engine, 11, 15, "Total Power", BLUE)
			var strSent = "$garbageSent"
			if(garbageAdd>0) strSent = "$strSent(+$garbageAdd)"
			receiver.drawScoreFont(engine, 11, 16, strSent)

			receiver.drawScoreFont(engine, 11, 18, "Erased", BLUE)
			receiver.drawScoreNum(engine, 11, 19, "$blocksCleared")

			if(engine.gameStarted&&engine.stat!=GameEngine.Status.MOVE
				&&engine.stat!=GameEngine.Status.RESULT)
				drawX(engine)

			if(!engine.gameActive) return

			val textHeight = if(engine.displaySize==1) 11 else engine.field.height+1

			val baseX = if(engine.displaySize==1) 1 else 0
			if(engine.chain>0&&chainDisplay>0&&chainDisplayType!=0) {
				var color = YELLOW
				if(chainDisplayType==2)
					when {
						engine.chain>=feverChainDisplay -> color = GREEN
						engine.chain==feverChainDisplay-2 -> color = ORANGE
						engine.chain<feverChainDisplay-2 -> color = RED
					}
				receiver.drawMenuFont(engine, baseX+if(engine.chain>9) 0 else 1, textHeight, "${engine.chain} CHAIN!", color)
			}
			if(zenKeshiDisplay>0)
				receiver.drawMenuFont(engine, baseX, textHeight+1, "ZENKESHI!", YELLOW)
		}
	}

	/** Draw fever timer on death columns*/
	override fun drawX(engine:GameEngine) {
		val strFeverTimer = "%02d".format((timeLimit+59)/60)

		for(i in 0..1)
			if(engine.field.getBlockEmpty(2+i, 0))
				if(engine.displaySize==1)
					receiver.drawMenuFont(engine, 4+i*2, 0, "${strFeverTimer[i]}", if(timeLimit<360) RED else WHITE, 2f)
				else
					receiver.drawMenuFont(engine, 2+i, 0, "${strFeverTimer[i]}", if(timeLimit<360) RED else WHITE)
	}

	override fun onMove(engine:GameEngine):Boolean {
		cleared = false
		zenKeshi = false
		return false
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)

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
		engine.meterValue = timeLimit*1f/TIME_LIMIT
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

	override fun lineClearEnd(engine:GameEngine):Boolean {
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
		} else if(!engine.field.getBlockEmpty(2, 0)||!engine.field.getBlockEmpty(3, 0)) {
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
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 1, "PLAY DATA", ORANGE)

		receiver.drawMenuFont(engine, 0, 3, "Score", BLUE)
		val strScoreBefore = "%10d".format(scoreBeforeBonus(engine.statistics))
		receiver.drawMenuNum(engine, 0, 4, strScoreBefore, GREEN)

		receiver.drawMenuFont(engine, 0, 5, "Clean Bonus", BLUE)
		val strZenKeshi = "%10d".format(zenKeshiCount)
		receiver.drawMenuNum(engine, 0, 6, strZenKeshi)
		val strZenKeshiBonus = "+$zenKeshiBonus"
		receiver.drawMenuFont(engine, 10-strZenKeshiBonus.length, 7, strZenKeshiBonus, GREEN)

		receiver.drawMenuFont(engine, 0, 8, "Chain Bonus", BLUE)
		val strMaxChain = "%10d".format(engine.statistics.maxChain)
		receiver.drawMenuFont(engine, 0, 9, strMaxChain)
		val strMaxChainBonus = "+$maxChainBonus"
		receiver.drawMenuFont(engine, 10-strMaxChainBonus.length, 10, strMaxChainBonus, GREEN)

		receiver.drawMenuFont(engine, 0, 11, "TOTAL", BLUE)
		val strScore = "%10d".format(engine.statistics.score)
		receiver.drawMenuFont(engine, 0, 12, strScore, RED)

		receiver.drawMenuFont(engine, 0, 13, "Time", BLUE)
		val strTime = "%10s".format(engine.statistics.time.toTimeStr)
		receiver.drawMenuNum(engine, 0, 14, strTime)

		if(rankingRank!=-1) {
			receiver.drawMenuFont(engine, 0, 15, "RANK", BLUE)
			val strRank = "%10d".format(rankingRank+1)
			receiver.drawMenuNum(engine, 0, 16, strRank)
		}
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean = (!owner.replayMode&&engine.ai==null
		&&updateRanking(engine.statistics.score, engine.statistics.time, mapSet, numColors)!=-1)

	override fun loadSetting(engine: GameEngine, prop: CustomProperties, ruleName: String, playerID: Int) {
		mapSet = prop.getProperty("avalanchefever.gametype", 0)
		outlinetype = prop.getProperty("avalanchefever.outlinetype", 0)
		numColors = prop.getProperty("avalanchefever.numcolors", 4)
		version = prop.getProperty("avalanchefever.version", 0)
		chainDisplayType = prop.getProperty("avalanchefever.chainDisplayType", 1)
		bigDisplay = prop.getProperty("avalanchefever.bigDisplay", false)
	}

	override fun saveSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("avalanchefever.gametype", mapSet)
		prop.setProperty("avalanchefever.outlinetype", outlinetype)
		prop.setProperty("avalanchefever.numcolors", numColors)
		prop.setProperty("avalanchefever.version", version)
		prop.setProperty("avalanchefever.chainDisplayType", chainDisplayType)
		prop.setProperty("avalanchefever.bigDisplay", bigDisplay)
	}

	private fun loadMapSetFever(id:Int, forceReload:Boolean) {
		if(forceReload) {
			propFeverMap.load("config/map/avalanche/${FEVER_MAPS[id]}Endless.map")
			feverChainMin = propFeverMap.getProperty("minChain", 3)
			feverChainMax = propFeverMap.getProperty("maxChain", 15)
			val subsets = propFeverMap.getProperty("sets")
			mapSubsets = subsets.split(Regex(",")).dropLastWhile {it.isEmpty()}
		}
	}

	private fun loadFeverMap(engine:GameEngine, chain:Int) {
		loadFeverMap(engine, engine.random, chain, engine.random.nextInt(mapSubsets.size))
	}

	private fun loadFeverMap(engine:GameEngine, rand:Random, chain:Int, subset:Int) {
		engine.createFieldIfNeeded()
		engine.field.reset()
		engine.field.stringToField(propFeverMap.getProperty("${mapSubsets[subset]}.${numColors}colors.${chain}chain"))
		engine.field.setBlockLinkByColor()
		engine.field.setAllAttribute(false, Block.ATTRIBUTE.GARBAGE, Block.ATTRIBUTE.ANTIGRAVITY)
		engine.field.setAllSkin(engine.skin)
		engine.field.shuffleColors(BLOCK_COLORS, numColors, rand)
	}

	/** Update rankings
	 * @param sc Score
	 * @param time Time
	 */
	private fun updateRanking(sc:Long, time:Int, type:Int, colors:Int):Int {
		rankingRank = checkRanking(sc, time, type, colors)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[colors-3][type][i] = rankingScore[colors-3][type][i-1]
				rankingTime[colors-3][type][i] = rankingTime[colors-3][type][i-1]
			}

			// Add new data
			rankingScore[colors-3][type][rankingRank] = sc
			rankingTime[colors-3][type][rankingRank] = time
		}
		return rankingRank
	}

	/** Calculate ranking position
	 * @param sc Score
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Long, time:Int, type:Int, colors:Int):Int {
		for(i in 0..<RANKING_MAX)
			if(sc>rankingScore[colors-3][type][i])
				return i
			else if(sc==rankingScore[colors-3][type][i]&&time<rankingTime[colors-3][type][i]) return i

		return -1
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0

		private val CHAIN_POWERS = listOf(
			4, 10, 18, 22, 30, 48, 80, 120, 160, 240, 280, 288, 342, 400, 440, 480, 520, 560, 600,
			640, 680, 720, 760, 800 //Amitie
		)

		/** Names of chain display settings */
		private val CHAIN_DISPLAY_NAMES = listOf("OFF", "YELLOW", "SIZE")

		/** Number of ranking records */
		private const val RANKING_MAX = 13

		/** Time limit */
		private const val TIME_LIMIT = 3600

		/** Names of fast-forward settings */
		private val FAST_NAMES = listOf("OFF", "CLEAR", "ALL")
	}
}
