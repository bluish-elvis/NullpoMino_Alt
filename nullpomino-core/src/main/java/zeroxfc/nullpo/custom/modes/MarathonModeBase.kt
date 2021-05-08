/*
    Copyright (c) 2010, NullNoname
    All rights reserved.

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
package zeroxfc.nullpo.custom.modes

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.net.NetUtil.urlEncode
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.NetDummyMode
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.getONorOFF
import mu.nu.nullpo.util.GeneralUtil.getTime

/**
 * MARATHON Mode
 */
open class MarathonModeBase:NetDummyMode() {
	/**
	 * Most recent increase in score
	 */
	@JvmField var lastscore = 0
	/**
	 * Time to display the most recent increase in score
	 */
	@JvmField var scgettime = 0

	protected var sc:Int = 0
	protected var sum:Int = 0
	/**
	 * Current BGM
	 */
	@JvmField var bgmlv = 0
	/**
	 * Level at start time
	 */
	@JvmField var startlevel = 0
	/**
	 * Flag for types of T-Spins allowed (0=none, 1=normal, 2=all spin)
	 */
	@JvmField var twistEnableType = 0
	/**
	 * Old flag for allowing T-Spins
	 */
	@JvmField var enableTSpin = false
	/**
	 * Flag for enabling wallkick T-Spins
	 */
	@JvmField var enableTSpinKick = false
	/**
	 * Immobile EZ spin
	 */
	@JvmField var twistEnableEZ = false
	/**
	 * Flag for enabling B2B
	 */
	@JvmField var enableB2B = false
	/**
	 * Flag for enabling combos
	 */
	@JvmField var enableCombo = false
	/**
	 * Game type
	 */
	@JvmField var goaltype = 0
	/**
	 * Big
	 */
	@JvmField var big = false
	/**
	 * Version
	 */
	@JvmField var version = 0
	/**
	 * Current round's ranking rank
	 */
	@JvmField var rankingRank = 0
	/** Rankings' scores */
	private var rankingScore:Array<IntArray> = emptyArray()
	/** Rankings' line counts*/
	private var rankingLines:Array<IntArray> = emptyArray()
	/** Rankings' times */
	private var rankingTime:Array<IntArray> = emptyArray()
	/*
     * Mode name
     */
	override val name:String
		get() = "marathonBase"
	/*
     * Initialization
     */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		owner = engine.owner
		lastscore = 0
		scgettime = 0
		bgmlv = 0
		rankingRank = -1
		rankingScore = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLines = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingTime = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		netPlayerInit(engine, playerID)
		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.modeConfig, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else {
			loadSetting(owner.replayProp)
			if(version==0&&owner.replayProp.getProperty("marathon.endless", false)) goaltype = 2

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("$playerID.net.netPlayerName", "")
		}
		engine.owner.backgroundStatus.bg = startlevel
		engine.framecolor = GameEngine.FRAME_COLOR_GREEN
	}
	/**
	 * Set the gravity rate
	 *
	 * @param engine GameEngine
	 */
	open fun setSpeed(engine:GameEngine) {
		var lv = engine.statistics.level
		if(lv<0) lv = 0
		if(lv>=tableGravity.size) lv = tableGravity.size-1
		engine.speed.gravity = tableGravity[lv]
		engine.speed.denominator = tableDenominator[lv]
	}
	/*
     * Called at settings screen
     */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode) {
			netOnUpdateNetPlayRanking(engine, goaltype)
		} else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 7, playerID)
			if(change!=0) {
				engine.playSE("change")
				when(engine.statc[2]) {
					0 -> {
						startlevel += change
						if(tableGameClearLines[goaltype]>=0) {
							if(startlevel<0) startlevel = (tableGameClearLines[goaltype]-1)/10
							if(startlevel>(tableGameClearLines[goaltype]-1)/10) startlevel = 0
						} else {
							if(startlevel<0) startlevel = 19
							if(startlevel>19) startlevel = 0
						}
						engine.owner.backgroundStatus.bg = startlevel
					}
					1 -> {
						//enableTSpin = !enableTSpin;
						twistEnableType += change
						if(twistEnableType<0) twistEnableType = 2
						if(twistEnableType>2) twistEnableType = 0
					}
					2 -> enableTSpinKick = !enableTSpinKick
					3 -> twistEnableEZ = !twistEnableEZ
					4 -> enableB2B = !enableB2B
					5 -> enableCombo = !enableCombo
					6 -> {
						goaltype += change
						if(goaltype<0) goaltype = GAMETYPE_MAX-1
						if(goaltype>GAMETYPE_MAX-1) goaltype = 0
						if(startlevel>(tableGameClearLines[goaltype]-1)/10&&tableGameClearLines[goaltype]>=0) {
							startlevel = (tableGameClearLines[goaltype]-1)/10
							engine.owner.backgroundStatus.bg = startlevel
						}
					}
					7 -> big = !big
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) {
					netSendOptions(engine)
				}
			}

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&engine.statc[3]>=5) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig)
				owner.saveModeConfig()

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")
				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) {
				engine.quitflag = true
			}

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&startlevel==0&&!big&&engine.ai==null) {
				netEnterNetPlayRankingScreen(engine, playerID, goaltype)
			}
			engine.statc[3]++
		} else {
			engine.statc[3]++
			engine.statc[2] = -1
			return engine.statc[3]<60
		}
		return true
	}
	/*
     * Render the settings screen
     */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(netIsNetRankingDisplayMode) {
			// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, playerID, receiver)
		} else {
			var strtwistEnable = ""
			if(version>=2) {
				if(twistEnableType==0) strtwistEnable = "OFF"
				if(twistEnableType==1) strtwistEnable = "T-ONLY"
				if(twistEnableType==2) strtwistEnable = "ALL"
			} else {
				strtwistEnable = getONorOFF(enableTSpin)
			}
			drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0,
				"LEVEL", "${startlevel+1}",
				"SPIN BONUS", strtwistEnable,
				"EZ SPIN", getONorOFF(enableTSpinKick),
				"EZIMMOBILE", getONorOFF(twistEnableEZ),
				"B2B", getONorOFF(enableB2B),
				"COMBO", getONorOFF(enableCombo),
				"GOAL", if(goaltype==2) "ENDLESS" else "${tableGameClearLines[goaltype]}"+" LINES",
				"BIG", getONorOFF(big))
		}
	}
	/*
     * Called for initialization during "Ready" screen
     */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel
		engine.statistics.levelDispAdd = 1
		engine.b2bEnable = enableB2B
		engine.comboType = if(enableCombo) GameEngine.COMBO_TYPE_NORMAL else GameEngine.COMBO_TYPE_DISABLE
		engine.big = big
		engine.twistAllowKick = enableTSpinKick
		when(twistEnableType) {
			0 -> engine.twistEnable = false
			1 -> engine.twistEnable = true
			else -> {
				engine.twistEnable = true
				engine.useAllSpinBonus = true
			}
		}
		engine.twistEnableEZ = twistEnableEZ
		setSpeed(engine)
		if(netIsWatch) {
			owner.bgmStatus.bgm = BGMStatus.BGM.Silent
		}
	}
	/*
     * Render score
     */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return
		receiver.drawScoreFont(engine, playerID, 0, 0, name, EventReceiver.COLOR.GREEN)

		// NET: Number of spectators
		netDrawSpectatorsCount(engine, 0, 18)
		// NET: All number of players
		if(playerID==players-1) {
			netDrawAllPlayersCount()
			netDrawGameRate(engine)
		}
		// NET: Player name (It may also appear in offline replay)
		netDrawPlayerName(engine)
	}
	/*
     * Called after every frame
     */
	override fun onLast(engine:GameEngine, playerID:Int) {
		scgettime++
	}
	/*
     * Calculate score
     */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int = 0
	/*
     * Soft drop
     */
	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreSD += fall
		scgettime += fall
	}
	/*
     * Hard drop
     */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreHD += fall*2
		scgettime += fall*2
	}
	/*
     * Render results screen
     */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE,
			Statistic.SCORE, Statistic.LINES, Statistic.LEVEL, Statistic.TIME, Statistic.SPL, Statistic.LPM)
		drawResultRank(engine, playerID, receiver, 12, EventReceiver.COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, playerID, receiver, 14, EventReceiver.COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 16, EventReceiver.COLOR.BLUE, netRankingRank[1])
		if(netIsPB) {
			receiver.drawMenuFont(engine, playerID, 2, 21, "NEW PB", EventReceiver.COLOR.ORANGE)
		}
		if(netIsNetPlay&&netReplaySendStatus==1) {
			receiver.drawMenuFont(engine, playerID, 0, 22, "SENDING...", EventReceiver.COLOR.PINK)
		} else if(netIsNetPlay&&!netIsWatch&&netReplaySendStatus==2) {
			receiver.drawMenuFont(engine, playerID, 1, 22, "A: RETRY", EventReceiver.COLOR.RED)
		}
	}
	/*
     * Called when saving replay
     */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) {
			prop.setProperty("$playerID.net.netPlayerName", netPlayerName)
		}

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.time, goaltype)
			if(rankingRank!=-1) {
				saveRanking(owner.modeConfig, engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
		}
	}
	/**
	 * Load settings from property file
	 *
	 * @param prop Property file
	 */
	override fun loadSetting(prop:CustomProperties) {
		startlevel = prop.getProperty("marathon.startlevel", 0)
		twistEnableType = prop.getProperty("marathon.twistEnableType", 1)
		enableTSpin = prop.getProperty("marathon.enableTSpin", true)
		enableTSpinKick = prop.getProperty("marathon.enableTSpinKick", true)
		twistEnableEZ = prop.getProperty("marathon.twistEnableEZ", false)
		enableB2B = prop.getProperty("marathon.enableB2B", true)
		enableCombo = prop.getProperty("marathon.enableCombo", true)
		goaltype = prop.getProperty("marathon.gametype", 0)
		big = prop.getProperty("marathon.big", false)
		version = prop.getProperty("marathon.version", 0)
	}
	/**
	 * Save settings to property file
	 *
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("marathon.startlevel", startlevel)
		prop.setProperty("marathon.twistEnableType", twistEnableType)
		prop.setProperty("marathon.enableTSpin", enableTSpin)
		prop.setProperty("marathon.enableTSpinKick", enableTSpinKick)
		prop.setProperty("marathon.twistEnableEZ", twistEnableEZ)
		prop.setProperty("marathon.enableB2B", enableB2B)
		prop.setProperty("marathon.enableCombo", enableCombo)
		prop.setProperty("marathon.gametype", goaltype)
		prop.setProperty("marathon.big", big)
		prop.setProperty("marathon.version", version)
	}
	/**
	 * Read rankings from property file
	 *
	 * @param prop     Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX) {
			for(j in 0 until GAMETYPE_MAX) {
				rankingScore[j][i] = prop.getProperty("marathon.ranking.$ruleName.$j.score.$i", 0)
				rankingLines[j][i] = prop.getProperty("marathon.ranking.$ruleName.$j.lines.$i", 0)
				rankingTime[j][i] = prop.getProperty("marathon.ranking.$ruleName.$j.time.$i", 0)
			}
		}
	}
	/**
	 * Save rankings to property file
	 *
	 * @param prop     Property file
	 * @param ruleName Rule name
	 */
	private fun saveRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX) {
			for(j in 0 until GAMETYPE_MAX) {
				prop.setProperty("marathon.ranking.$ruleName.$j.score.$i", rankingScore[j][i])
				prop.setProperty("marathon.ranking.$ruleName.$j.lines.$i", rankingLines[j][i])
				prop.setProperty("marathon.ranking.$ruleName.$j.time.$i", rankingTime[j][i])
			}
		}
	}
	/**
	 * Update rankings
	 *
	 * @param sc   Score
	 * @param li   Lines
	 * @param time Time
	 */
	private fun updateRanking(sc:Int, li:Int, time:Int, type:Int) {
		rankingRank = checkRanking(sc, li, time, type)
		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[type][i] = rankingScore[type][i-1]
				rankingLines[type][i] = rankingLines[type][i-1]
				rankingTime[type][i] = rankingTime[type][i-1]
			}

			// Add new data
			rankingScore[type][rankingRank] = sc
			rankingLines[type][rankingRank] = li
			rankingTime[type][rankingRank] = time
		}
	}
	/**
	 * Calculate ranking position
	 *
	 * @param sc   Score
	 * @param li   Lines
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Int, li:Int, time:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX) {
			if(sc>rankingScore[type][i]) {
				return i
			} else if(sc==rankingScore[type][i]&&li>rankingLines[type][i]) {
				return i
			} else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&time<rankingTime[type][i]) {
				return i
			}
		}
		return -1
	}

	/** NET: Send various in-game stats (as well as goaltype)
	 * @param engine GameEngine
	 */
	override fun netSendStats(engine:GameEngine) {
		val bg =
			if(engine.owner.backgroundStatus.fadesw) engine.owner.backgroundStatus.fadebg else engine.owner.backgroundStatus.bg
		var msg = "game\tstats\t"
		msg += "${engine.statistics.scoreLine}\t${engine.statistics.scoreSD}\t${engine.statistics.scoreHD}\t${engine.statistics.scoreBonus}\t"
		msg += "${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t"
		msg += "${engine.statistics.time}\t${engine.statistics.level}\t"
		msg += "$goaltype\t${engine.gameActive}\t${engine.timerActive}\t"
		msg += "$lastscore\t$scgettime\t${engine.lastevent}\t${engine.b2bbuf}\t${engine.combobuf}\t$bg\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive various in-game stats (as well as goaltype) */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		listOf<(String)->Unit>({}, {}, {}, {},
			{engine.statistics.scoreLine = it.toInt()},
			{engine.statistics.scoreSD = it.toInt()},
			{engine.statistics.scoreHD = it.toInt()},
			{engine.statistics.scoreBonus = it.toInt()},
			{engine.statistics.lines = it.toInt()},
			{engine.statistics.totalPieceLocked = it.toInt()},
			{engine.statistics.time = it.toInt()},
			{engine.statistics.level = it.toInt()},
			{goaltype = it.toInt()},
			{engine.gameActive = it.toBoolean()},
			{engine.timerActive = it.toBoolean()},
			{lastscore = it.toInt()},
			{scgettime = it.toInt()},
			{engine.lastevent = GameEngine.ScoreEvent.parseInt(it)},
			{engine.b2bbuf = it.toInt()},
			{engine.combobuf = it.toInt()},
			{engine.owner.backgroundStatus.bg = it.toInt()}).zip(message).forEach {(x, y) ->
			x(y)
		}

		// Meter
		engine.meterValue = engine.statistics.lines%10*receiver.getMeterMax(engine)/9
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(engine.statistics.lines%10>=4) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(engine.statistics.lines%10>=6) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(engine.statistics.lines%10>=8) engine.meterColor = GameEngine.METER_COLOR_RED
	}
	/**
	 * NET: Send end-of-game stats
	 *
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		var subMsg = ""
		subMsg += "SCORE;"+engine.statistics.score.toString()+"\t"
		subMsg += "LINE;"+engine.statistics.lines+"\t"
		subMsg += "LEVEL;"+(engine.statistics.level+engine.statistics.levelDispAdd)+"\t"
		subMsg += "TIME;"+getTime(engine.statistics.time)+"\t"
		subMsg += "SCORE/LINE;"+engine.statistics.spl.toString()+"\t"
		subMsg += "LINE/MIN;"+engine.statistics.lpm.toString()+"\t"
		val msg = """
			 	gstat1p	${urlEncode(subMsg)}
			 	
			 	""".trimIndent()
		netLobby!!.netPlayerClient!!.send(msg)
	}
	/**
	 * NET: Send game options to all spectators
	 *
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		var msg = "game\toption\t"
		msg += "$startlevel"+"\t"+twistEnableType+"\t"+enableTSpinKick+"\t\t"+twistEnableEZ+"\t"
		msg += """$enableB2B	$enableCombo	$goaltype	$big
"""
		netLobby!!.netPlayerClient!!.send(msg)
	}
	/**
	 * NET: Receive game options
	 */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		startlevel = message[4].toInt()
		twistEnableType = message[5].toInt()
		enableTSpinKick = java.lang.Boolean.parseBoolean(message[6])
		twistEnableEZ = java.lang.Boolean.parseBoolean(message[8])
		enableB2B = java.lang.Boolean.parseBoolean(message[9])
		enableCombo = java.lang.Boolean.parseBoolean(message[10])
		goaltype = message[11].toInt()
		big = java.lang.Boolean.parseBoolean(message[12])
	}
	/**
	 * NET: Get goal type
	 */
	override fun netGetGoalType():Int = goaltype
	/**
	 * NET: It returns true when the current settings doesn't prevent leaderboard screen from showing.
	 */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = startlevel==0&&!big&&engine.ai==null

	companion object {
		/**
		 * Current version
		 */
		const val CURRENT_VERSION = 2
		/**
		 * Fall velocity table (numerators)
		 */
		@JvmField val tableGravity = intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 465, 731, 1280, 1707, -1, -1, -1)
		/**
		 * Fall velocity table (denominators)
		 */
		@JvmField val tableDenominator = intArrayOf(63, 50, 39, 30, 22, 16, 12, 8, 6, 4, 3, 2, 1, 256, 256, 256, 256, 256, 256, 256)
		/**
		 * Line counts when BGM changes occur
		 */
		@JvmField val tableBGMChange = intArrayOf(50, 100, 150, 200, -1)
		/**
		 * Line counts when game ending occurs
		 */
		@JvmField val tableGameClearLines = intArrayOf(150, 200, -1)
		/**
		 * Number of entries in rankings
		 */
		const val RANKING_MAX = 10
		/**
		 * Number of ranking types
		 */
		const val RANKING_TYPE = 3
		/**
		 * Number of game types
		 */
		const val GAMETYPE_MAX = 3
		/**
		 * Most recent scoring event type constants
		 */
		const val EVENT_NONE = 0
		const val EVENT_SINGLE = 1
		const val EVENT_DOUBLE = 2
		const val EVENT_TRIPLE = 3
		const val EVENT_FOUR = 4
		const val EVENT_TSPIN_ZERO_MINI = 5
		const val EVENT_TSPIN_ZERO = 6
		const val EVENT_TSPIN_SINGLE_MINI = 7
		const val EVENT_TSPIN_SINGLE = 8
		const val EVENT_TSPIN_DOUBLE_MINI = 9
		const val EVENT_TSPIN_DOUBLE = 10
		const val EVENT_TSPIN_TRIPLE = 11
		const val EVENT_TSPIN_EZ = 12
	}
}