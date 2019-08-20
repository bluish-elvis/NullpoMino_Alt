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
package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import kotlin.math.ceil

/** EXTREME Mode */
class MarathonExtreme:NetDummyMode() {

	/** Most recent increase in score */
	private var lastscore:Int = 0

	/** Time to display the most recent increase in score */
	private var scgettime:Int = 0
	private var sc:Int = 0
	private var sum:Int = 0

	/** Ending time */
	private var rolltime:Int = 0

	/** Current BGM */
	private var bgmlv:Int = 0

	/** Level at start time */
	private var startlevel:Int = 0

	/** Flag for types of T-Spins allowed (0=none, 1=normal, 2=all spin) */
	private var tspinEnableType:Int = 0

	/** Old flag for allowing T-Spins */
	private var enableTSpin:Boolean = false

	/** Flag for enabling wallkick T-Spins */
	private var enableTSpinKick:Boolean = false

	/** Spin check type (4Point or Immobile) */
	private var spinCheckType:Int = 0

	/** Immobile EZ spin */
	private var tspinEnableEZ:Boolean = false

	/** Flag for enabling B2B */
	private var enableB2B:Boolean = false

	/** Flag for enabling combos */
	private var enableCombo:Boolean = false

	/** Endless flag */
	private var endless:Boolean = false

	/** Big */
	private var big:Boolean = false

	/** Version */
	private var version:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = 0

	/** Rankings' scores */
	private var rankingScore:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Rankings' line counts */
	private var rankingLines:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Rankings' times */
	private var rankingTime:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/* Mode name */
	override val name:String
		get() = "MARATHON EXTREME"

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		sc = 0
		scgettime = sc
		bgmlv = 0
		rolltime = 0

		rankingRank = -1
		rankingScore = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLines = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingTime = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

		netPlayerInit(engine, playerID)

		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.recordProp, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else {
			loadSetting(owner.replayProp)

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("$playerID.net.netPlayerName", "")
		}

		engine.staffrollEnable = true
		engine.staffrollNoDeath = true
		engine.staffrollEnableStatistics = true

		engine.owner.backgroundStatus.bg = startlevel
		engine.framecolor = GameEngine.FRAME_COLOR_RED
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	fun setSpeed(engine:GameEngine) {
		var lv = engine.statistics.level

		if(lv<0) lv = 0
		if(lv>=tableARE.size) lv = tableARE.size-1

		engine.speed.gravity = -1
		engine.speed.are = tableARE[lv]
		engine.speed.areLine = tableARELine[lv]
		engine.speed.lineDelay = tableLineDelay[lv]
		engine.speed.lockDelay = tableLockDelay[lv]
		engine.speed.das = tableDAS[lv]
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, netGetGoalType())
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 8)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						startlevel += change
						if(startlevel<0) startlevel = 19
						if(startlevel>19) startlevel = 0
						engine.owner.backgroundStatus.bg = startlevel
					}
					1 -> {
						//enableTSpin = !enableTSpin;
						tspinEnableType += change
						if(tspinEnableType<0) tspinEnableType = 2
						if(tspinEnableType>2) tspinEnableType = 0
					}
					2 -> enableTSpinKick = !enableTSpinKick
					3 -> {
						spinCheckType += change
						if(spinCheckType<0) spinCheckType = 1
						if(spinCheckType>1) spinCheckType = 0
					}
					4 -> tspinEnableEZ = !tspinEnableEZ
					5 -> enableB2B = !enableB2B
					6 -> enableCombo = !enableCombo
					7 -> endless = !endless
					8 -> big = !big
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig)
				owner.saveModeConfig()

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")

				return false
			}

			// Cancel
			if(engine.ctrl!!.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitflag = true

			// NET: Netplay Ranking
			if(engine.ctrl!!.isPush(Controller.BUTTON_D)&&netIsNetPlay&&startlevel==0&&!big
				&&engine.ai==null)
				netEnterNetPlayRankingScreen(engine, playerID, netGetGoalType())

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}// Replay

		return true
	}

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(netIsNetRankingDisplayMode)
		// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, playerID, receiver)
		else {
			drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR.RED, 0,
				"LEVEL", (startlevel+1).toString(), "SPIN BONUS", if(tspinEnableType==0) "OFF" else if(tspinEnableType==1) "T-ONLY" else "ALL",
				"EZ SPIN", GeneralUtil.getONorOFF(enableTSpinKick), "SPIN TYPE", if(spinCheckType==0) "4POINT" else "IMMOBILE",
				"EZIMMOBILE", GeneralUtil.getONorOFF(tspinEnableEZ))

			drawMenuCompact(engine, playerID, receiver,
				"B2B", GeneralUtil.getONorOFF(enableB2B), "COMBO", GeneralUtil.getONorOFF(enableCombo), "BIG", GeneralUtil.getONorOFF(big))
		}
	}

	/* Called for initialization during "Ready" screen */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel
		engine.statistics.levelDispAdd = 1
		engine.b2bEnable = enableB2B
		engine.comboType = if(enableCombo)
			GameEngine.COMBO_TYPE_NORMAL
		else GameEngine.COMBO_TYPE_DISABLE
		engine.big = big

		owner.bgmStatus.bgm = if(netIsWatch) BGM.SILENT
		else tableBGM[bgmlv]

		if(version>=1) {
			engine.tspinAllowKick = enableTSpinKick
			when(tspinEnableType) {
				0 -> engine.tspinEnable = false
				1 -> engine.tspinEnable = true
				else -> {
					engine.tspinEnable = true
					engine.useAllSpinBonus = true
				}
			}
		} else
			engine.tspinEnable = enableTSpin

		engine.spinCheckType = spinCheckType
		engine.tspinEnableEZ = tspinEnableEZ

		setSpeed(engine)
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, playerID, 0, 0, "EXTREME MARATHON!", EventReceiver.COLOR.RED)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE  LINE TIME", EventReceiver.COLOR.RED, scale)

				for(i in 0 until RANKING_MAX) {
					var endlessIndex = 0
					if(endless) endlessIndex = 1

					receiver.drawScoreNum(engine, playerID, 0, topY+i, String.format("%02d", i+1), EventReceiver.COLOR.RED, scale)
					receiver.drawScoreNum(engine, playerID, 3, topY+i, "${rankingScore[endlessIndex][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 10, topY+i, "${rankingLines[endlessIndex][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 15, topY+i, GeneralUtil.getTime(rankingTime[endlessIndex][i]), i==rankingRank, scale)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "LINE", EventReceiver.COLOR.RED)
			receiver.drawScoreNum(engine, playerID, 5, 2, engine.statistics.lines.toString(), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 4, "SCORE", EventReceiver.COLOR.RED)
			receiver.drawScoreNum(engine, playerID, 5, 4, "+$lastscore")
			val scget = scgettime<engine.statistics.score
			if(scget) scgettime += ceil((engine.statistics.score-scgettime)/22.0).toInt()
			sc += ceil(((scgettime-sc)/10f).toDouble()).toInt()
			receiver.drawScoreNum(engine, playerID, 0, 5, "$sc", scget, 2f)
			if(engine.gameActive&&engine.ending==2) {
				val remainRollTime = maxOf(0, ROLLTIMELIMIT-rolltime)

				receiver.drawScoreFont(engine, playerID, 0, 7, "ROLL TIME", EventReceiver.COLOR.RED)
				receiver.drawScoreNum(engine, playerID, 5, 7, GeneralUtil.getTime(remainRollTime), remainRollTime>0&&remainRollTime<10*60, 2f)
			} else {

				receiver.drawScoreFont(engine, playerID, 0, 7, "LEVEL", EventReceiver.COLOR.RED)
				receiver.drawScoreNum(engine, playerID, 5, 7, String.format("%.1f", engine.statistics.level.toDouble()+1.0
					+engine.statistics.lines%10*0.1), 2f)
			}
			receiver.drawScoreFont(engine, playerID, 0, 8, "TIME", EventReceiver.COLOR.RED)
			receiver.drawScoreNum(engine, playerID, 0, 9, GeneralUtil.getTime(engine.statistics.time), 2f)

			if(scget) renderLineAlert(engine, playerID, receiver)
		}

		super.renderLast(engine, playerID)
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		// Ending
		if(engine.gameActive&&engine.ending==2) {
			rolltime++

			// Time meter
			var remainRollTime = ROLLTIMELIMIT-rolltime
			if(remainRollTime<0) remainRollTime = 0
			engine.meterValue = remainRollTime*receiver.getMeterMax(engine)/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainRollTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainRollTime<=20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainRollTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED

			// Finished
			if(rolltime>=ROLLTIMELIMIT) {
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		}

	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {
		// Line clear bonus
		val pts = calcScore(engine, lines)
		var cmb = 0
		// Combo
		if(enableCombo&&engine.combo>=1&&lines>=1) cmb = engine.combo-1

		val spd = maxOf(0, engine.lockDelay-engine.lockDelayNow)+if(engine.manualLock) 1 else 0
		// Add to score
		if(pts+cmb+spd>0) {
			var get = pts*(10+engine.statistics.level)/10+spd
			if(cmb>=1) {
				var b = sum*(1+cmb)/2
				sum += get
				b = sum*(2+cmb)/2-b
				get = b
			} else
				sum = get
			if(pts>0) lastscore = get
			if(lines>=1)
				engine.statistics.scoreLine += get
			else
				engine.statistics.scoreBonus += get
			scgettime += spd
		}

		if(engine.ending==0) {
			// BGM fade-out effects and BGM changes
			if(tableBGMChange[bgmlv]!=-1) {
				if(engine.statistics.lines>=tableBGMChange[bgmlv]-5) owner.bgmStatus.fadesw = true

				if(engine.statistics.lines>=tableBGMChange[bgmlv]) {
					bgmlv++
					owner.bgmStatus.bgm = tableBGM[bgmlv]
					owner.bgmStatus.fadesw = false
				}
			}

			// Meter
			engine.meterValue = engine.statistics.lines%10*receiver.getMeterMax(engine)/9
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(engine.statistics.lines%10>=4) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(engine.statistics.lines%10>=6) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(engine.statistics.lines%10>=8) engine.meterColor = GameEngine.METER_COLOR_RED

			if(engine.statistics.lines>=200&&!endless) {
				// Ending
				engine.playSE("levelup")
				engine.playSE("endingstart")
				owner.bgmStatus.fadesw = false
				owner.bgmStatus.bgm = BGM.ENDING(2)
				engine.bone = true
				engine.ending = 2
				engine.timerActive = false
			} else if(engine.statistics.lines>=(engine.statistics.level+1)*10&&engine.statistics.level<19) {
				// Level up
				engine.statistics.level++

				owner.backgroundStatus.fadesw = true
				owner.backgroundStatus.fadecount = 0
				owner.backgroundStatus.fadebg = engine.statistics.level

				setSpeed(engine)
				engine.playSE("levelup")
			}
		}
	}

	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		val b = if(engine.ending==0) BGM.RESULT(0) else BGM.RESULT(3)
		owner.bgmStatus.fadesw = false
		owner.bgmStatus.bgm = b

		return super.onResult(engine, playerID)
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(engine, playerID, receiver, 0, EventReceiver.COLOR.RED, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL, Statistic.TIME, Statistic.SPL, Statistic.LPM)
		drawResultRank(engine, playerID, receiver, 12, EventReceiver.COLOR.RED, rankingRank)
		drawResultNetRank(engine, playerID, receiver, 14, EventReceiver.COLOR.RED, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 16, EventReceiver.COLOR.RED, netRankingRank[1])

		if(netIsPB) receiver.drawMenuFont(engine, playerID, 2, 21, "NEW PB", EventReceiver.COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, playerID, 0, 22, "SENDING...", EventReceiver.COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, playerID, 1, 22, "A: RETRY", EventReceiver.COLOR.RED)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty("$playerID.net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.time, endless)

			if(rankingRank!=-1) {
				saveRanking(owner.modeConfig, engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
		}
	}

	/** Load settings from property file
	 * @param prop Property file
	 */
	override fun loadSetting(prop:CustomProperties) {
		startlevel = prop.getProperty("extreme.startlevel", 0)
		tspinEnableType = prop.getProperty("extreme.tspinEnableType", 2)
		enableTSpin = prop.getProperty("extreme.enableTSpin", true)
		enableTSpinKick = prop.getProperty("extreme.enableTSpinKick", true)
		spinCheckType = prop.getProperty("extreme.spinCheckType", 1)
		tspinEnableEZ = prop.getProperty("extreme.tspinEnableEZ", true)
		enableB2B = prop.getProperty("extreme.enableB2B", true)
		enableCombo = prop.getProperty("extreme.enableCombo", true)
		endless = prop.getProperty("extreme.endless", false)
		big = prop.getProperty("extreme.big", false)
		version = prop.getProperty("extreme.version", 0)
	}

	/** Save settings to property file
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("extreme.startlevel", startlevel)
		prop.setProperty("extreme.tspinEnableType", tspinEnableType)
		prop.setProperty("extreme.enableTSpin", enableTSpin)
		prop.setProperty("extreme.enableTSpinKick", enableTSpinKick)
		prop.setProperty("extreme.spinCheckType", spinCheckType)
		prop.setProperty("extreme.tspinEnableEZ", tspinEnableEZ)
		prop.setProperty("extreme.enableB2B", enableB2B)
		prop.setProperty("extreme.enableCombo", enableCombo)
		prop.setProperty("extreme.endless", endless)
		prop.setProperty("extreme.big", big)
		prop.setProperty("extreme.version", version)
	}

	/** Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(endlessIndex in 0..1) {
				rankingScore[endlessIndex][i] = prop.getProperty("extreme.ranking.$ruleName.$endlessIndex.score.$i", 0)
				rankingLines[endlessIndex][i] = prop.getProperty("extreme.ranking.$ruleName.$endlessIndex.lines.$i", 0)
				rankingTime[endlessIndex][i] = prop.getProperty("extreme.ranking.$ruleName.$endlessIndex.time.$i", 0)
			}
	}

	/** Save rankings to property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	fun saveRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(endlessIndex in 0..1) {
				prop.setProperty("extreme.ranking.$ruleName.$endlessIndex.score.$i", rankingScore[endlessIndex][i])
				prop.setProperty("extreme.ranking.$ruleName.$endlessIndex.lines.$i", rankingLines[endlessIndex][i])
				prop.setProperty("extreme.ranking.$ruleName.$endlessIndex.time.$i", rankingTime[endlessIndex][i])
			}
	}

	/** Update rankings
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 */
	private fun updateRanking(sc:Int, li:Int, time:Int, endlessMode:Boolean) {
		rankingRank = checkRanking(sc, li, time, endlessMode)

		if(rankingRank!=-1) {
			var endlessIndex = 0
			if(endlessMode) endlessIndex = 1

			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[endlessIndex][i] = rankingScore[endlessIndex][i-1]
				rankingLines[endlessIndex][i] = rankingLines[endlessIndex][i-1]
				rankingTime[endlessIndex][i] = rankingTime[endlessIndex][i-1]
			}

			// Add new data
			rankingScore[endlessIndex][rankingRank] = sc
			rankingLines[endlessIndex][rankingRank] = li
			rankingTime[endlessIndex][rankingRank] = time
		}
	}

	/** Calculate ranking position
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Int, li:Int, time:Int, endlessMode:Boolean):Int {
		var endlessIndex = 0
		if(endlessMode) endlessIndex = 1

		for(i in 0 until RANKING_MAX)
			if(sc>rankingScore[endlessIndex][i])
				return i
			else if(sc==rankingScore[endlessIndex][i]&&li>rankingLines[endlessIndex][i])
				return i
			else if(sc==rankingScore[endlessIndex][i]&&li==rankingLines[endlessIndex][i]
				&&time<rankingTime[endlessIndex][i])
				return i

		return -1
	}

	/** NET: Send various in-game stats (as well as goaltype)
	 * @param engine GameEngine
	 */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(engine.owner.backgroundStatus.fadesw) engine.owner.backgroundStatus.fadebg else engine.owner.backgroundStatus.bg
		var msg = "game\tstats\t"
		msg += "${engine.statistics.scoreLine}\t${engine.statistics.scoreSD}\t${engine.statistics.scoreHD}\t${engine.statistics.scoreBonus}\t"
		msg += "${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t"
		msg += "${engine.statistics.time}\t${engine.statistics.level}\t$endless\t"
		msg += "${engine.gameActive}\t${engine.timerActive}\t"
		msg += "$lastscore\t$scgettime\t${engine.lastevent}\t${engine.b2bbuf}\t${engine.combobuf}\t${engine.lasteventpiece}\t"
		msg += "$bg\t$rolltime\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive various in-game stats (as well as goaltype) */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		listOf<(String)->Unit>({},{},{},{},
			{engine.statistics.scoreLine = Integer.parseInt(it)},
			{engine.statistics.scoreSD = Integer.parseInt(it)},
			{engine.statistics.scoreHD = Integer.parseInt(it)},
			{engine.statistics.scoreBonus = Integer.parseInt(it)},
			{engine.statistics.lines = Integer.parseInt(it)},
			{engine.statistics.totalPieceLocked = Integer.parseInt(it)},
			{engine.statistics.time = Integer.parseInt(it)},
			{engine.statistics.level = Integer.parseInt(it)},
			{endless = java.lang.Boolean.parseBoolean(it)},
			{engine.gameActive = java.lang.Boolean.parseBoolean(it)},
			{engine.timerActive = java.lang.Boolean.parseBoolean(it)},
			{lastscore = Integer.parseInt(it)},
			{scgettime = Integer.parseInt(it)},
			{engine.lastevent = Integer.parseInt(it)},
			{engine.b2bbuf = Integer.parseInt(it)},
			{engine.combobuf = Integer.parseInt(it)},
			{engine.lasteventpiece = Integer.parseInt(it)},
			{engine.owner.backgroundStatus.bg = Integer.parseInt(it)},
			{rolltime = Integer.parseInt(it)}).zip(message).forEach{
			(x,y)->x(y)
		}
		// Meter
		engine.meterValue = engine.statistics.lines%10*receiver.getMeterMax(engine)/9
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(engine.statistics.lines%10>=4) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(engine.statistics.lines%10>=6) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(engine.statistics.lines%10>=8) engine.meterColor = GameEngine.METER_COLOR_RED
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		var subMsg = ""
		subMsg += "SCORE;${engine.statistics.score}\t"
		subMsg += "LINE;${engine.statistics.lines}\t"
		subMsg += "LEVEL;${(engine.statistics.level+engine.statistics.levelDispAdd)}\t"
		subMsg += "TIME;${GeneralUtil.getTime(engine.statistics.time)}\t"
		subMsg += "SCORE/LINE;${engine.statistics.spl}\t"
		subMsg += "LINE/MIN;${engine.statistics.lpm}\t"

		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		var msg = "game\toption\t"
		msg += "$startlevel\t$tspinEnableType\t$enableTSpinKick\t$enableB2B\t"
		msg += "$enableCombo\t$endless\t$big\t$spinCheckType\t+$tspinEnableEZ\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		startlevel = Integer.parseInt(message[4])
		tspinEnableType = Integer.parseInt(message[5])
		enableTSpinKick = java.lang.Boolean.parseBoolean(message[6])
		enableB2B = java.lang.Boolean.parseBoolean(message[7])
		enableCombo = java.lang.Boolean.parseBoolean(message[8])
		endless = java.lang.Boolean.parseBoolean(message[9])
		big = java.lang.Boolean.parseBoolean(message[10])
		spinCheckType = Integer.parseInt(message[11])
		tspinEnableEZ = java.lang.Boolean.parseBoolean(message[12])
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = if(endless) 1 else 0

	/** NET: It returns true when the current settings doesn't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = startlevel==0&&!big&&engine.ai==null

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Ending time */
		private const val ROLLTIMELIMIT = 3238

		/** ARE table */
		private val tableARE = intArrayOf(25, 24, 23, 22, 21, 20, 18, 16, 14, 12, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)

		/** ARE after line clear table */
		private val tableARELine = intArrayOf(20, 18, 16, 14, 12, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 1, 1, 1, 1, 1)

		/** Line clear time table */
		private val tableLineDelay = intArrayOf(30, 25, 20, 17, 14, 11, 9, 8, 7, 6, 5, 5, 5, 5, 5, 4, 3, 2, 1, 0)
		//   50,43,36,31,26,21,18,16,14,12,10, 9, 8, 7, 6, 5, 4, 3, 2, 1
		/** Lock delay table */
		private val tableLockDelay = intArrayOf(30, 29, 28, 27, 27, 26, 26, 25, 25, 24, 24, 23, 23, 22, 22, 22, 21, 21, 21, 20)

		/** DAS table */
		private val tableDAS = intArrayOf(10, 10, 10, 9, 9, 9, 8, 8, 8, 7, 7, 7, 6, 6, 6, 5, 5, 5, 4, 4)

		/** Line counts when BGM changes occur */
		private val tableBGMChange = intArrayOf(66, 133, -1)
		private val tableBGM = arrayOf(BGM.RUSH(0), BGM.RUSH(1), BGM.RUSH(2))
		/** Number of entries in rankings */
		private const val RANKING_MAX = 10

		/** Number of ranking types */
		private const val RANKING_TYPE = 2
	}
}
