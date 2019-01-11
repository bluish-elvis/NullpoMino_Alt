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
import org.apache.log4j.Logger

/** SCORE RACE Mode */
class SprintScore:NetDummyMode() {

	/** Most recent increase in score */
	private var lastscore:Int = 0
	private var sum:Int = 0

	/** Time to display the most recent increase in score */
	private var scgettime:Int = 0

	/** Most recent scoring event b2b */
	private var lastb2b:Boolean = false

	/** Most recent scoring event combo count */
	private var lastcombo:Int = 0

	/** Most recent scoring event piece ID */
	private var lastpiece:Int = 0

	/** BGM number */
	private var bgmno:Int = 0

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

	/** Big */
	private var big:Boolean = false

	/** Goal score type */
	private var goaltype:Int = 0

	/** Last preset number used */
	private var presetNumber:Int = 0

	/** Version */
	private var version:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = 0

	/** Rankings' times */
	private var rankingTime:Array<IntArray> = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}

	/** Rankings' line counts */
	private var rankingLines:Array<IntArray> = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}

	/** Rankings' score/line */
	private var rankingSPL:Array<DoubleArray> = Array(GOALTYPE_MAX) {DoubleArray(RANKING_MAX)}

	/* Mode name */
	override val name:String
		get() = "SCORE RACE"

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		scgettime = 0
		lastb2b = false
		lastcombo = 0
		lastpiece = 0
		bgmno = 0

		rankingRank = -1
		rankingTime = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}
		rankingLines = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}
		rankingSPL = Array(GOALTYPE_MAX) {DoubleArray(RANKING_MAX)}

		engine.framecolor = GameEngine.FRAME_COLOR_BRONZE

		netPlayerInit(engine, playerID)

		if(!engine.owner.replayMode) {
			presetNumber = engine.owner.modeConfig.getProperty("scorerace.presetNumber", 0)
			loadPreset(engine, engine.owner.modeConfig, -1)
			loadRanking(owner.modeConfig, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else {
			presetNumber = 0
			loadPreset(engine, engine.owner.replayProp, -1)
			version = engine.owner.replayProp.getProperty("scorerace.version", 0)
			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty(playerID.toString()+".net.netPlayerName", "")
		}
	}

	/** Load options from a preset
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 * @param preset Preset number
	 */
	private fun loadPreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		engine.speed.gravity = prop.getProperty("scorerace.gravity.$preset", 4)
		engine.speed.denominator = prop.getProperty("scorerace.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("scorerace.are.$preset", 10)
		engine.speed.areLine = prop.getProperty("scorerace.areLine.$preset", 5)
		engine.speed.lineDelay = prop.getProperty("scorerace. lineDelay.$preset", 20)
		engine.speed.lockDelay = prop.getProperty("scorerace.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("scorerace.das.$preset", 14)
		bgmno = prop.getProperty("scorerace.bgmno.$preset", 0)
		tspinEnableType = prop.getProperty("scorerace.tspinEnableType.$preset", 1)
		enableTSpin = prop.getProperty("scorerace.enableTSpin.$preset", true)
		enableTSpinKick = prop.getProperty("scorerace.enableTSpinKick.$preset", true)
		spinCheckType = prop.getProperty("scorerace.spinCheckType.$preset", 0)
		tspinEnableEZ = prop.getProperty("scorerace.tspinEnableEZ.$preset", false)
		enableB2B = prop.getProperty("scorerace.enableB2B.$preset", true)
		enableCombo = prop.getProperty("scorerace.enableCombo.$preset", true)
		big = prop.getProperty("scorerace.big.$preset", false)
		goaltype = prop.getProperty("scorerace.goaltype.$preset", 1)
	}

	/** Save options to a preset
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 * @param preset Preset number
	 */
	private fun savePreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		prop.setProperty("scorerace.gravity.$preset", engine.speed.gravity)
		prop.setProperty("scorerace.denominator.$preset", engine.speed.denominator)
		prop.setProperty("scorerace.are.$preset", engine.speed.are)
		prop.setProperty("scorerace.areLine.$preset", engine.speed.areLine)
		prop.setProperty("scorerace.lineDelay.$preset", engine.speed.lineDelay)
		prop.setProperty("scorerace.lockDelay.$preset", engine.speed.lockDelay)
		prop.setProperty("scorerace.das.$preset", engine.speed.das)
		prop.setProperty("scorerace.bgmno.$preset", bgmno)
		prop.setProperty("scorerace.tspinEnableType.$preset", tspinEnableType)
		prop.setProperty("scorerace.enableTSpin.$preset", enableTSpin)
		prop.setProperty("scorerace.enableTSpinKick.$preset", enableTSpinKick)
		prop.setProperty("scorerace.spinCheckType.$preset", spinCheckType)
		prop.setProperty("scorerace.tspinEnableEZ.$preset", tspinEnableEZ)
		prop.setProperty("scorerace.enableB2B.$preset", enableB2B)
		prop.setProperty("scorerace.enableCombo.$preset", enableCombo)
		prop.setProperty("scorerace.big.$preset", big)
		prop.setProperty("scorerace.goaltype.$preset", goaltype)
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goaltype)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 17, playerID)

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl!!.isPress(Controller.BUTTON_E)) m = 100
				if(engine.ctrl!!.isPress(Controller.BUTTON_F)) m = 1000

				when(menuCursor) {
					0 -> {
						engine.speed.gravity += change*m
						if(engine.speed.gravity<-1) engine.speed.gravity = 99999
						if(engine.speed.gravity>99999) engine.speed.gravity = -1
					}
					1 -> {
						engine.speed.denominator += change*m
						if(engine.speed.denominator<-1) engine.speed.denominator = 99999
						if(engine.speed.denominator>99999) engine.speed.denominator = -1
					}
					2 -> {
						engine.speed.are += change
						if(engine.speed.are<0) engine.speed.are = 99
						if(engine.speed.are>99) engine.speed.are = 0
					}
					3 -> {
						engine.speed.areLine += change
						if(engine.speed.areLine<0) engine.speed.areLine = 99
						if(engine.speed.areLine>99) engine.speed.areLine = 0
					}
					4 -> {
						engine.speed.lineDelay += change
						if(engine.speed.lineDelay<0) engine.speed.lineDelay = 99
						if(engine.speed.lineDelay>99) engine.speed.lineDelay = 0
					}
					5 -> {
						engine.speed.lockDelay += change
						if(engine.speed.lockDelay<0) engine.speed.lockDelay = 99
						if(engine.speed.lockDelay>99) engine.speed.lockDelay = 0
					}
					6 -> {
						engine.speed.das += change
						if(engine.speed.das<0) engine.speed.das = 99
						if(engine.speed.das>99) engine.speed.das = 0
					}
					7 -> {
						bgmno += change
						if(bgmno<0) bgmno =BGM.count
						if(bgmno>BGM.count) bgmno = 0
					}
					8 -> big = !big
					9 -> {
						goaltype += change
						if(goaltype<0) goaltype = GOALTYPE_MAX-1
						if(goaltype>GOALTYPE_MAX-1) goaltype = 0
					}
					10 -> {
						//enableTSpin = !enableTSpin;
						tspinEnableType += change
						if(tspinEnableType<0) tspinEnableType = 2
						if(tspinEnableType>2) tspinEnableType = 0
					}
					11 -> enableTSpinKick = !enableTSpinKick
					12 -> {
						spinCheckType += change
						if(spinCheckType<0) spinCheckType = 1
						if(spinCheckType>1) spinCheckType = 0
					}
					13 -> tspinEnableEZ = !tspinEnableEZ
					14 -> enableB2B = !enableB2B
					15 -> enableCombo = !enableCombo
					16, 17 -> {
						presetNumber += change
						if(presetNumber<0) presetNumber = 99
						if(presetNumber>99) presetNumber = 0
					}
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				if(menuCursor==16) {
					// Load preset
					loadPreset(engine, owner.modeConfig, presetNumber)

					// NET: Signal options change
					if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
				} else if(menuCursor==17) {
					// Save preset
					savePreset(engine, owner.modeConfig, presetNumber)
					receiver.saveModeConfig(owner.modeConfig)
				} else {
					// Save settings
					owner.modeConfig.setProperty("scorerace.presetNumber", presetNumber)
					savePreset(engine, owner.modeConfig, -1)
					receiver.saveModeConfig(owner.modeConfig)

					// NET: Signal start of the game
					if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")

					return false
				}
			}

			// Cancel
			if(engine.ctrl!!.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitflag = true

			// NET: Netplay Ranking
			if(engine.ctrl!!.isPush(Controller.BUTTON_D)&&netIsNetPlay&&!netIsWatch&&!big
				&&engine.ai==null)
				netEnterNetPlayRankingScreen(engine, playerID, goaltype)

			menuTime++
		} else {
			menuTime++
			menuCursor = 0

			if(menuTime>=60) menuCursor = 10
			return menuTime<120
		}// Replay

		return true
	}

	/* Render settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(netIsNetRankingDisplayMode)
		// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, playerID, receiver)
		else {
			drawMenuSpeeds(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0, engine.speed.gravity, engine.speed.denominator, engine.speed.are, engine.speed.areLine, engine.speed.lineDelay, engine.speed.lockDelay, engine.speed.das)
			drawMenuBGM(engine, playerID, receiver, bgmno)
			drawMenuCompact(engine, playerID, receiver, "BIG", GeneralUtil.getOorX(big), "GOAL", String.format("%3dK",
				GOAL_TABLE[goaltype]/1000))

			drawMenu(engine, playerID, receiver, "SPIN BONUS", if(tspinEnableType==0) "OFF" else if(tspinEnableType==1) "T-ONLY" else "ALL",
				"EZ SPIN", GeneralUtil.getONorOFF(enableTSpinKick), "SPIN TYPE", if(spinCheckType==0) "4POINT" else "IMMOBILE",
				"EZIMMOBILE", GeneralUtil.getONorOFF(tspinEnableEZ))
			drawMenuCompact(engine, playerID, receiver,
				"B2B", GeneralUtil.getONorOFF(enableB2B), "COMBO", GeneralUtil.getONorOFF(enableCombo))
			if(!engine.owner.replayMode) {
				menuColor = EventReceiver.COLOR.GREEN
				drawMenuCompact(engine, playerID, receiver, "LOAD", presetNumber.toString(), "SAVE", presetNumber.toString())
			}
		}
	}

	/* This function will be called before the game actually begins (after
 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.big = big
		engine.b2bEnable = enableB2B
		if(enableCombo)
			engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		else
			engine.comboType = GameEngine.COMBO_TYPE_DISABLE

		if(netIsWatch)
			owner.bgmStatus.bgm = BGM.SILENT
		else
			owner.bgmStatus.bgm = BGM.values[bgmno]

		if(version>=1) {
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
	}

	/* Score display */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, playerID, 0, 0, "SCORE RACE", EventReceiver.COLOR.RED)
		receiver.drawScoreFont(engine, playerID, 0, 1, "("+GOAL_TABLE[goaltype]+" PTS GAME)", EventReceiver.COLOR.RED)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null&&!netIsWatch) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "TIME   LINE SCR/LINE", EventReceiver.COLOR.BLUE, scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, topY+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW, scale)
					receiver.drawScoreNum(engine, playerID, 3,
						topY+i, GeneralUtil.getTime(rankingTime[goaltype][i].toFloat()), rankingRank==i, scale)
					receiver.drawScoreNum(engine, playerID, 12, topY+i,
						rankingLines[goaltype][i].toString(), rankingRank==i, scale)
					receiver.drawScoreNum(engine, playerID, 17, topY+i,
						String.format("%.6g", rankingSPL[goaltype][i]), rankingRank==i, scale)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "SCORE", EventReceiver.COLOR.BLUE)
			var sc = GOAL_TABLE[goaltype]-engine.statistics.score
			if(sc<0) sc = 0
			var fontcolor = EventReceiver.COLOR.WHITE
			if(sc in 1..9600) fontcolor = EventReceiver.COLOR.YELLOW
			if(sc in 1..4800) fontcolor = EventReceiver.COLOR.ORANGE
			if(sc in 1..2400) fontcolor = EventReceiver.COLOR.RED
			receiver.drawScoreNum(engine, playerID, 5, 6, "+$lastscore")
			receiver.drawScoreNum(engine, playerID, 0, 7, scgettime.toString(), fontcolor, 2f)
			if(scgettime<engine.statistics.score) scgettime += Math.ceil(((engine.statistics.score-scgettime)/10f).toDouble()).toInt()

			receiver.drawScoreFont(engine, playerID, 0, 6, "LINE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 7, engine.statistics.lines.toString(), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 9, "SCORE/MIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, String.format("%-10g", engine.statistics.spm), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 12, "LINE/MIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 13, engine.statistics.lpm.toString(), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 15, "SCORE/LINE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 16, String.format("%-10g", engine.statistics.spl), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 18, "TIME", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 19, GeneralUtil.getTime(engine.statistics.time.toFloat()), 2f)

			renderLineAlert(engine, playerID, receiver)
		}

		super.renderLast(engine, playerID)
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
				engine.statistics.scoreFromLineClear += get
			else
				engine.statistics.scoreFromOtherBonus += get
			engine.statistics.score += get
			scgettime += spd
		}
	}

	/* Soft drop */
	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreFromSoftDrop += fall
		engine.statistics.score += fall
	}

	/* Hard drop */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreFromHardDrop += fall*2
		engine.statistics.score += fall*2
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		// Update meter
		var remainScore = GOAL_TABLE[goaltype]-engine.statistics.score
		if(!engine.timerActive) remainScore = 0
		engine.meterValue = remainScore*receiver.getMeterMax(engine)/GOAL_TABLE[goaltype]
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(remainScore<=9600) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(remainScore<=4800) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(remainScore<=2400) engine.meterColor = GameEngine.METER_COLOR_RED

		// Goal reached
		if(engine.statistics.score>=GOAL_TABLE[goaltype]&&engine.timerActive) {
			engine.gameEnded()
			engine.resetStatc()
			engine.stat = GameEngine.Status.ENDINGSTART
		}

		// BGM fadeout
		if(remainScore<=1000&&engine.timerActive) owner.bgmStatus.fadesw = true

	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 0, "kn PAGE"+(engine.statc[1]+1)+"/2", EventReceiver.COLOR.RED)

		if(engine.statc[1]==0) {
			drawResultStats(engine, playerID, receiver, 2, EventReceiver.COLOR.BLUE, AbstractMode.Statistic.SCORE, AbstractMode.Statistic.LINES, AbstractMode.Statistic.TIME, AbstractMode.Statistic.PIECE)
			drawResultRank(engine, playerID, receiver, 10, EventReceiver.COLOR.BLUE, rankingRank)
			drawResultNetRank(engine, playerID, receiver, 12, EventReceiver.COLOR.BLUE, netRankingRank[0])
			drawResultNetRankDaily(engine, playerID, receiver, 14, EventReceiver.COLOR.BLUE, netRankingRank[1])
		} else if(engine.statc[1]==1)
			drawResultStats(engine, playerID, receiver, 2, EventReceiver.COLOR.BLUE, AbstractMode.Statistic.SPL, AbstractMode.Statistic.SPM, AbstractMode.Statistic.LPM, AbstractMode.Statistic.PPS)

		if(netIsPB) receiver.drawMenuFont(engine, playerID, 2, 21, "NEW PB", EventReceiver.COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, playerID, 0, 22, "SENDING...", EventReceiver.COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, playerID, 1, 22, "A: RETRY", EventReceiver.COLOR.RED)
	}

	/* Results screen */
	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		// Page change
		if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_UP)) {
			engine.statc[1]--
			if(engine.statc[1]<0) engine.statc[1] = 1
			engine.playSE("change")
		}
		if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
			engine.statc[1]++
			if(engine.statc[1]>1) engine.statc[1] = 0
			engine.playSE("change")
		}

		return super.onResult(engine, playerID)
	}

	/* Save replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		savePreset(engine, engine.owner.replayProp, -1)
		engine.owner.replayProp.setProperty("scorerace.version", version)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty(playerID.toString()+".net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&engine.statistics.score>=GOAL_TABLE[goaltype]&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.time, engine.statistics.lines, engine.statistics.spl)

			if(rankingRank!=-1) {
				saveRanking(owner.modeConfig, engine.ruleopt.strRuleName)
				receiver.saveModeConfig(owner.modeConfig)
			}
		}
	}

	/** Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until GOALTYPE_MAX)
			for(j in 0 until RANKING_MAX) {
				rankingTime[i][j] = prop.getProperty("scorerace.ranking.$ruleName.$i.time.$j", -1)
				rankingLines[i][j] = prop.getProperty("scorerace.ranking.$ruleName.$i.lines.$j", 0)

				if(rankingLines[i][j]>0) {
					val defaultSPL = GOAL_TABLE[i].toDouble()/rankingLines[i][j].toDouble()
					rankingSPL[i][j] = prop.getProperty("scorerace.ranking.$ruleName.$i.spl.$j", defaultSPL)
				} else
					rankingSPL[i][j] = 0.0
			}
	}

	/** Save rankings to property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	private fun saveRanking(prop:CustomProperties?, ruleName:String) {
		for(i in 0 until GOALTYPE_MAX)
			for(j in 0 until RANKING_MAX) {
				prop!!.setProperty("scorerace.ranking.$ruleName.$i.time.$j", rankingTime[i][j])
				prop.setProperty("scorerace.ranking.$ruleName.$i.lines.$j", rankingLines[i][j])
				prop.setProperty("scorerace.ranking.$ruleName.$i.spl.$j", rankingSPL[i][j])
			}
	}

	/** Update rankings
	 * @param time Time
	 * @param lines Lines
	 * @param spl Score/Line
	 */
	private fun updateRanking(time:Int, lines:Int, spl:Double) {
		rankingRank = checkRanking(time, lines, spl)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingTime[goaltype][i] = rankingTime[goaltype][i-1]
				rankingLines[goaltype][i] = rankingLines[goaltype][i-1]
				rankingSPL[goaltype][i] = rankingSPL[goaltype][i-1]
			}

			// Add new data
			rankingTime[goaltype][rankingRank] = time
			rankingLines[goaltype][rankingRank] = lines
			rankingSPL[goaltype][rankingRank] = spl
		}
	}

	/** Calculate ranking position
	 * @param time Time
	 * @param lines Lines
	 * @param spl Score/Line
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(time:Int, lines:Int, spl:Double):Int {
		for(i in 0 until RANKING_MAX)
			if(time<rankingTime[goaltype][i]||rankingTime[goaltype][i]<0)
				return i
			else if(time==rankingTime[goaltype][i]&&(lines<rankingLines[goaltype][i]||rankingLines[goaltype][i]==0))
				return i
			else if(time==rankingTime[goaltype][i]&&lines==rankingLines[goaltype][i]
				&&spl>rankingSPL[goaltype][i])
				return i

		return -1
	}

	/** NET: Send various in-game stats (as well as goaltype)
	 * @param engine GameEngine
	 */
	override fun netSendStats(engine:GameEngine) {
		var msg = "game\tstats\t"
		msg += engine.statistics.score.toString()+"\t"+engine.statistics.lines+"\t"+engine.statistics.totalPieceLocked+"\t"
		msg += engine.statistics.time.toString()+"\t"+engine.statistics.spm+"\t"
		msg += engine.statistics.lpm.toString()+"\t"+engine.statistics.spl+"\t"+goaltype+"\t"
		msg += engine.gameActive.toString()+"\t"+engine.timerActive+"\t"
		msg += lastscore.toString()+"\t"+scgettime+"\t"+lastb2b+"\t"+lastcombo+"\t"+lastpiece
		msg += "\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive various in-game stats (as well as goaltype) */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		engine.statistics.score = Integer.parseInt(message[4])
		engine.statistics.lines = Integer.parseInt(message[5])
		engine.statistics.totalPieceLocked = Integer.parseInt(message[6])
		engine.statistics.time = Integer.parseInt(message[7])
		//engine.statistics.spm =  java.lang.Double.parseDouble(message[8])
		//engine.statistics.lpm = java.lang.Float.parseFloat(message[9])
		//engine.statistics.spl = java.lang.Double.parseDouble(message[10])
		goaltype = Integer.parseInt(message[11])
		engine.gameActive = java.lang.Boolean.parseBoolean(message[12])
		engine.timerActive = java.lang.Boolean.parseBoolean(message[13])
		lastscore = Integer.parseInt(message[14])
		scgettime = Integer.parseInt(message[15])
		lastb2b = java.lang.Boolean.parseBoolean(message[16])
		lastcombo = Integer.parseInt(message[17])
		lastpiece = Integer.parseInt(message[18])
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		var subMsg = ""
		subMsg += "SCORE;"+engine.statistics.score+"/"+GOAL_TABLE[goaltype]+"\t"
		subMsg += "LINE;"+engine.statistics.lines+"\t"
		subMsg += "TIME;"+GeneralUtil.getTime(engine.statistics.time.toFloat())+"\t"
		subMsg += "PIECE;"+engine.statistics.totalPieceLocked+"\t"
		subMsg += "SCORE/LINE;"+engine.statistics.spl+"\t"
		subMsg += "SCORE/MIN;"+engine.statistics.spm+"\t"
		subMsg += "LINE/MIN;"+engine.statistics.lpm+"\t"
		subMsg += "PIECE/SEC;"+engine.statistics.pps+"\t"

		val msg = "gstat1p\t"+NetUtil.urlEncode(subMsg)+"\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		var msg = "game\toption\t"
		msg += engine.speed.gravity.toString()+"\t"+engine.speed.denominator+"\t"+engine.speed.are+"\t"
		msg += engine.speed.areLine.toString()+"\t"+engine.speed.lineDelay+"\t"+engine.speed.lockDelay+"\t"
		msg += engine.speed.das.toString()+"\t"+bgmno+"\t"+big+"\t"+goaltype+"\t"+tspinEnableType+"\t"
		msg += enableTSpinKick.toString()+"\t"+enableB2B+"\t"+enableCombo+"\t"+presetNumber+"\t"
		msg += spinCheckType.toString()+"\t"+tspinEnableEZ+"\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		engine.speed.gravity = Integer.parseInt(message[4])
		engine.speed.denominator = Integer.parseInt(message[5])
		engine.speed.are = Integer.parseInt(message[6])
		engine.speed.areLine = Integer.parseInt(message[7])
		engine.speed.lineDelay = Integer.parseInt(message[8])
		engine.speed.lockDelay = Integer.parseInt(message[9])
		engine.speed.das = Integer.parseInt(message[10])
		bgmno = Integer.parseInt(message[11])
		big = java.lang.Boolean.parseBoolean(message[12])
		goaltype = Integer.parseInt(message[13])
		tspinEnableType = Integer.parseInt(message[14])
		enableTSpinKick = java.lang.Boolean.parseBoolean(message[15])
		enableB2B = java.lang.Boolean.parseBoolean(message[16])
		enableCombo = java.lang.Boolean.parseBoolean(message[17])
		presetNumber = Integer.parseInt(message[18])
		spinCheckType = Integer.parseInt(message[19])
		tspinEnableEZ = java.lang.Boolean.parseBoolean(message[20])
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goaltype

	/** NET: It returns true when the current settings doesn't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = !big&&engine.ai==null

	/** NET: It returns true when the current settings doesn't prevent replay
	 * data from sending. */
	override fun netIsNetRankingSendOK(engine:GameEngine):Boolean = netIsNetRankingViewOK(engine)&&engine.statistics.score>=GOAL_TABLE[goaltype]

	companion object {
		/* ----- Main constants ----- */
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Number of entries in rankings */
		private const val RANKING_MAX = 10

		/** Goal score type */
		private const val GOALTYPE_MAX = 3

		/** Goal score constants */
		private val GOAL_TABLE = intArrayOf(10000, 25000, 30000)

		/* ----- Main variables ----- */
		/** Log */
		internal var log = Logger.getLogger(SprintScore::class.java)
	}
}
