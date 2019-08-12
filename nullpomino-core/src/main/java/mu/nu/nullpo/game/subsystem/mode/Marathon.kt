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

/** MARATHON Mode */
class Marathon:NetDummyMode() {

	/** Most recent increase in score */
	private var lastscore:Int = 0

	/** Time to display the most recent increase in score */
	private var scgettime:Int = 0
	private var sc:Int = 0
	private var sum:Int = 0

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

	/** Game type */
	private var goaltype:Int = 0

	/** Big */
	private var big:Boolean = false

	/** Version */
	private var version:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = -1

	/** Rankings' scores */
	private var rankingScore:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Rankings' line counts */
	private var rankingLines:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Rankings' times */
	private var rankingTime:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/* Mode name */
	override val name:String
		get() = "MARATHON"

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		sc = 0
		scgettime = sc
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

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	fun setSpeed(engine:GameEngine) {
		var lv = engine.statistics.level

		if(lv<0) lv = 0
		if(lv>=tableGravity.size) lv = tableGravity.size-1

		engine.speed.gravity = tableGravity[lv]
		engine.speed.denominator = tableDenominator[lv]
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goaltype)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 8, playerID)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						goaltype += change
						if(goaltype<0) goaltype = GAMETYPE_MAX-1
						if(goaltype>GAMETYPE_MAX-1) goaltype = 0

						if(startlevel>(tableGameClearLines[goaltype]-1)/10&&tableGameClearLines[goaltype]>=0) {
							startlevel = (tableGameClearLines[goaltype]-1)/10
							engine.owner.backgroundStatus.bg = startlevel
						}
					}
					1 -> {
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
					2 -> {
						//enableTSpin = !enableTSpin;
						tspinEnableType += change
						if(tspinEnableType<0) tspinEnableType = 2
						if(tspinEnableType>2) tspinEnableType = 0
					}
					3 -> enableTSpinKick = !enableTSpinKick
					4 -> {
						spinCheckType += change
						if(spinCheckType<0) spinCheckType = 1
						if(spinCheckType>1) spinCheckType = 0
					}
					5 -> tspinEnableEZ = !tspinEnableEZ
					6 -> enableB2B = !enableB2B
					7 -> enableCombo = !enableCombo
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
			if(engine.ctrl!!.isPush(Controller.BUTTON_D)&&netIsNetPlay&&startlevel==0&&!big&&
				engine.ai==null)
				netEnterNetPlayRankingScreen(engine, playerID, goaltype)

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
			drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0,
				"GOAL", if(goaltype==2) "ENDLESS" else "${tableGameClearLines[goaltype]} LINES", "LEVEL", (startlevel+1).toString(),
				"SPIN BONUS", if(tspinEnableType==0) "OFF" else if(tspinEnableType==1) "T-ONLY" else "ALL",
				"EZ SPIN", GeneralUtil.getONorOFF(enableTSpinKick),
				"SPIN TYPE", if(spinCheckType==0) "4POINT" else "IMMOBILE", "EZIMMOBILE", GeneralUtil.getONorOFF(tspinEnableEZ))
			drawMenuCompact(engine, playerID, receiver,
				"B2B", GeneralUtil.getONorOFF(enableB2B), "COMBO", GeneralUtil.getONorOFF(enableCombo), "BIG", GeneralUtil.getONorOFF(big))

		}
	}

	/* Called for initialization during "Ready" screen */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel
		engine.statistics.levelDispAdd = 1
		engine.b2bEnable = enableB2B
		if(enableCombo)
			engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		else
			engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		engine.big = big

		if(version>=2) {
			engine.tspinAllowKick = enableTSpinKick
			if(tspinEnableType==0)
				engine.tspinEnable = false
			else if(tspinEnableType==1)
				engine.tspinEnable = true
			else {
				engine.tspinEnable = true
				engine.useAllSpinBonus = true
			}
		} else
			engine.tspinEnable = enableTSpin

		engine.spinCheckType = spinCheckType
		engine.tspinEnableEZ = tspinEnableEZ

		setSpeed(engine)

		if(netIsWatch)
			owner.bgmStatus.bgm = BGM.SILENT
		else
			owner.bgmStatus.bgm = tableBGM[bgmlv]
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return
		if(tableGameClearLines[goaltype]==-1)
			receiver.drawScoreFont(engine, playerID, 0, 0, "ENDLESS MARATHON", EventReceiver.COLOR.GREEN)
		else
			receiver.drawScoreFont(engine, playerID, 0, 0,
				"${tableGameClearLines[goaltype]} LINES MARATHON", EventReceiver.COLOR.GREEN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE  LINE TIME", EventReceiver.COLOR.BLUE, scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, topY+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW, scale)
					receiver.drawScoreNum(engine, playerID, 3, topY+i, "${rankingScore[goaltype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 10, topY+i, "${rankingLines[goaltype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 15, topY+i,
						GeneralUtil.getTime(rankingTime[goaltype][i]), i==rankingRank, scale)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "LINE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 2, engine.statistics.lines.toString(), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 4, "SCORE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 4, "+$lastscore")
			val scget = scgettime<engine.statistics.score
			if(scget) scgettime += ceil((engine.statistics.score-scgettime)/24.0).toInt()
			sc += ceil(((scgettime-sc)/10f).toDouble()).toInt()
			if(scget) renderLineAlert(engine, playerID, receiver)
			receiver.drawScoreNum(engine, playerID, 0, 5, "$sc", scget, 2f)

			receiver.drawScoreFont(engine, playerID, 0, 8, "LEVEL", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 8, String.format("%.1f", engine.statistics.level.toFloat()+
				if(engine.statistics.level>=19&&tableGameClearLines[goaltype]<0) 1f else engine.statistics.lines%10*0.1f+1f), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 9, "TIME", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, GeneralUtil.getTime(engine.statistics.time), 2f)
		}

		super.renderLast(engine, playerID)
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {}

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
				engine.statistics.scoreFromLineClear += get
			else
				engine.statistics.scoreFromOtherBonus += get
			engine.statistics.score += get
			scgettime += spd
		}
		// BGM fade-out effects and BGM changes
		if(tableBGMChange[bgmlv]!=-1) {
			if(engine.statistics.lines>=tableBGMChange[bgmlv]-5) owner.bgmStatus.fadesw = true

			if(engine.statistics.lines>=tableBGMChange[bgmlv]&&(engine.statistics.lines<tableGameClearLines[goaltype]||tableGameClearLines[goaltype]<0)) {
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

		if(engine.statistics.lines>=tableGameClearLines[goaltype]&&tableGameClearLines[goaltype]>=0) {
			// Ending
			engine.ending = 1
			engine.gameEnded()
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

	/* Soft drop */
	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreFromSoftDrop += fall
		engine.statistics.score += fall
		scgettime += fall
	}

	/* Hard drop */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreFromHardDrop += fall*2
		engine.statistics.score += fall*2
		scgettime += fall*2
	}

	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		val b = if(engine.ending==0) BGM.RESULT(1) else BGM.RESULT(2)
		owner.bgmStatus.fadesw = false
		owner.bgmStatus.bgm = b

		return super.onResult(engine, playerID)
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL, Statistic.TIME, Statistic.SPL, Statistic.LPM)
		drawResultRank(engine, playerID, receiver, 12, EventReceiver.COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, playerID, receiver, 14, EventReceiver.COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 16, EventReceiver.COLOR.BLUE, netRankingRank[1])

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
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.time, goaltype)

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
		startlevel = prop.getProperty("marathon.startlevel", 0)
		tspinEnableType = prop.getProperty("marathon.tspinEnableType", 2)
		enableTSpin = prop.getProperty("marathon.enableTSpin", true)
		enableTSpinKick = prop.getProperty("marathon.enableTSpinKick", true)
		spinCheckType = prop.getProperty("marathon.spinCheckType", 1)
		tspinEnableEZ = prop.getProperty("marathon.tspinEnableEZ", true)
		enableB2B = prop.getProperty("marathon.enableB2B", true)
		enableCombo = prop.getProperty("marathon.enableCombo", true)
		goaltype = prop.getProperty("marathon.gametype", 0)
		big = prop.getProperty("marathon.big", false)
		version = prop.getProperty("marathon.version", 0)
	}

	/** Save settings to property file
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("marathon.startlevel", startlevel)
		prop.setProperty("marathon.tspinEnableType", tspinEnableType)
		prop.setProperty("marathon.enableTSpin", enableTSpin)
		prop.setProperty("marathon.enableTSpinKick", enableTSpinKick)
		prop.setProperty("marathon.spinCheckType", spinCheckType)
		prop.setProperty("marathon.tspinEnableEZ", tspinEnableEZ)
		prop.setProperty("marathon.enableB2B", enableB2B)
		prop.setProperty("marathon.enableCombo", enableCombo)
		prop.setProperty("marathon.gametype", goaltype)
		prop.setProperty("marathon.big", big)
		prop.setProperty("marathon.version", version)
	}

	/** Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(j in 0 until GAMETYPE_MAX) {
				rankingScore[j][i] = prop.getProperty("marathon.ranking.$ruleName.$j.score.$i", 0)
				rankingLines[j][i] = prop.getProperty("marathon.ranking.$ruleName.$j.lines.$i", 0)
				rankingTime[j][i] = prop.getProperty("marathon.ranking.$ruleName.$j.time.$i", 0)
			}
	}

	/** Save rankings to property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	private fun saveRanking(prop:CustomProperties?, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(j in 0 until GAMETYPE_MAX) {
				prop!!.setProperty("marathon.ranking.$ruleName.$j.score.$i", rankingScore[j][i])
				prop.setProperty("marathon.ranking.$ruleName.$j.lines.$i", rankingLines[j][i])
				prop.setProperty("marathon.ranking.$ruleName.$j.time.$i", rankingTime[j][i])
			}
	}

	/** Update rankings
	 * @param sc Score
	 * @param li Lines
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

	/** Calculate ranking position
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Int, li:Int, time:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(sc>rankingScore[type][i])
				return i
			else if(sc==rankingScore[type][i]&&li>rankingLines[type][i])
				return i
			else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&time<rankingTime[type][i]) return i

		return -1
	}

	/** NET: Send various in-game stats (as well as goaltype)
	 * @param engine GameEngine
	 */
	override fun netSendStats(engine:GameEngine) {
		val bg =
			if(engine.owner.backgroundStatus.fadesw) engine.owner.backgroundStatus.fadebg else engine.owner.backgroundStatus.bg
		var msg = "game\tstats\t"
		msg += "${engine.statistics.score}\t${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t"
		msg += "${engine.statistics.time}\t${engine.statistics.level}\t"
		msg += "${engine.statistics.lpm}\t${engine.statistics.spl}\t$goaltype\t"
		msg += "${engine.gameActive}\t${engine.timerActive}\t"
		msg += "$lastscore\t$scgettime\t${engine.lastevent}\t${engine.b2bbuf}\t${engine.combobuf}\t${engine.lasteventpiece}\t"
		msg += "$bg"+"\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive various in-game stats (as well as goaltype) */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		engine.statistics.score = Integer.parseInt(message[4])
		engine.statistics.lines = Integer.parseInt(message[5])
		engine.statistics.totalPieceLocked = Integer.parseInt(message[6])
		engine.statistics.time = Integer.parseInt(message[7])
		engine.statistics.level = Integer.parseInt(message[8])
		//engine.statistics.lpm = java.lang.Float.parseFloat(message[9])
		//engine.statistics.spl = java.lang.Double.parseDouble(message[10])
		goaltype = Integer.parseInt(message[11])
		engine.gameActive = java.lang.Boolean.parseBoolean(message[12])
		engine.timerActive = java.lang.Boolean.parseBoolean(message[13])
		lastscore = Integer.parseInt(message[14])
		scgettime = Integer.parseInt(message[15])
		engine.lastevent = Integer.parseInt(message[16])
		engine.b2bbuf = Integer.parseInt(message[17])
		engine.combobuf = Integer.parseInt(message[18])
		engine.lasteventpiece = Integer.parseInt(message[19])
		engine.owner.backgroundStatus.bg = Integer.parseInt(message[20])

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
		subMsg += "LEVEL;${engine.statistics.level+engine.statistics.levelDispAdd}\t"
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
		msg += "$startlevel\t$tspinEnableType\t$enableTSpinKick\t$spinCheckType\t$tspinEnableEZ\t"
		msg += "$enableB2B\t$enableCombo\t$goaltype\t$big\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		startlevel = Integer.parseInt(message[4])
		tspinEnableType = Integer.parseInt(message[5])
		enableTSpinKick = java.lang.Boolean.parseBoolean(message[6])
		spinCheckType = Integer.parseInt(message[7])
		tspinEnableEZ = java.lang.Boolean.parseBoolean(message[8])
		enableB2B = java.lang.Boolean.parseBoolean(message[9])
		enableCombo = java.lang.Boolean.parseBoolean(message[10])
		goaltype = Integer.parseInt(message[11])
		big = java.lang.Boolean.parseBoolean(message[12])
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goaltype

	/** NET: It returns true when the current settings doesn't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = startlevel==0&&!big&&engine.ai==null

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Fall velocity table (numerators) */
		private val tableGravity = intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 4, 5, 7, 10, -1)

		/** Fall velocity table (denominators) */
		private val tableDenominator = intArrayOf(64, 50, 40, 33, 25, 20, 13, 10, 8, 4, 3, 2, 1, 1, 1, 1, 1, 1, 1, 1)

		/** Line counts when BGM changes occur */
		private val tableBGMChange = intArrayOf(60, 80, 140, 170, 200, -1)
		private val tableBGM =
			arrayOf(BGM.GENERIC(0), BGM.GENERIC(1), BGM.GENERIC(2), BGM.GENERIC(3), BGM.GENERIC(4), BGM.GENERIC(5))
		/** Line counts when game ending occurs */
		private val tableGameClearLines = intArrayOf(150, 200, -1)

		/** Number of entries in rankings */
		private const val RANKING_MAX = 10

		/** Number of ranking types */
		private const val RANKING_TYPE = 3

		/** Number of game types */
		private const val GAMETYPE_MAX = 3
	}
}
