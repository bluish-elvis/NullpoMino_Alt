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

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil

/** ULTRA Mode */
class SprintUltra:NetDummyMode() {

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

	/** Time limit type */
	private var goaltype:Int = 0

	/** Last preset number used */
	private var presetNumber:Int = 0

	/** Version */
	private var version:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:IntArray = IntArray(0)

	/** Rankings' scores */
	private var rankingScore:Array<Array<IntArray>>? = null

	/** Rankings' line counts */
	private var rankingLines:Array<Array<IntArray>>? = null

	/* Mode name */
	override val name:String
		get() = "ULTRA"

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		scgettime = 0
		lastb2b = false
		lastcombo = 0
		lastpiece = 0
		bgmno = 0

		rankingRank = IntArray(RANKING_TYPE)
		rankingRank[0] = -1
		rankingRank[1] = -1

		rankingScore = Array(GOALTYPE_MAX) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}
		rankingLines = Array(GOALTYPE_MAX) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}

		engine.framecolor = GameEngine.FRAME_COLOR_BLUE

		netPlayerInit(engine, playerID)

		if(!engine.owner.replayMode) {
			presetNumber = engine.owner.modeConfig.getProperty("ultra.presetNumber", 0)
			loadPreset(engine, engine.owner.modeConfig, -1)
			loadRanking(owner.modeConfig, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else {
			presetNumber = 0
			loadPreset(engine, engine.owner.replayProp, -1)
			version = engine.owner.replayProp.getProperty("ultra.version", 0)
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
		engine.speed.gravity = prop.getProperty("ultra.gravity.$preset", 4)
		engine.speed.denominator = prop.getProperty("ultra.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("ultra.are.$preset", 10)
		engine.speed.areLine = prop.getProperty("ultra.areLine.$preset", 5)
		engine.speed.lineDelay = prop.getProperty("ultra. lineDelay.$preset", 20)
		engine.speed.lockDelay = prop.getProperty("ultra.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("ultra.das.$preset", 14)
		bgmno = prop.getProperty("ultra.bgmno.$preset", 0)
		tspinEnableType = prop.getProperty("ultra.tspinEnableType.$preset", 1)
		enableTSpin = prop.getProperty("ultra.enableTSpin.$preset", true)
		enableTSpinKick = prop.getProperty("ultra.enableTSpinKick.$preset", true)
		spinCheckType = prop.getProperty("ultra.spinCheckType.$preset", 0)
		tspinEnableEZ = prop.getProperty("ultra.tspinEnableEZ.$preset", false)
		enableB2B = prop.getProperty("ultra.enableB2B.$preset", true)
		enableCombo = prop.getProperty("ultra.enableCombo.$preset", true)
		big = prop.getProperty("ultra.big.$preset", false)
		goaltype = prop.getProperty("ultra.goaltype.$preset", 2)
	}

	/** Save options to a preset
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 * @param preset Preset number
	 */
	private fun savePreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		prop.setProperty("ultra.gravity.$preset", engine.speed.gravity)
		prop.setProperty("ultra.denominator.$preset", engine.speed.denominator)
		prop.setProperty("ultra.are.$preset", engine.speed.are)
		prop.setProperty("ultra.areLine.$preset", engine.speed.areLine)
		prop.setProperty("ultra.lineDelay.$preset", engine.speed.lineDelay)
		prop.setProperty("ultra.lockDelay.$preset", engine.speed.lockDelay)
		prop.setProperty("ultra.das.$preset", engine.speed.das)
		prop.setProperty("ultra.bgmno.$preset", bgmno)
		prop.setProperty("ultra.tspinEnableType.$preset", tspinEnableType)
		prop.setProperty("ultra.enableTSpin.$preset", enableTSpin)
		prop.setProperty("ultra.enableTSpinKick.$preset", enableTSpinKick)
		prop.setProperty("ultra.spinCheckType.$preset", spinCheckType)
		prop.setProperty("ultra.tspinEnableEZ.$preset", tspinEnableEZ)
		prop.setProperty("ultra.enableB2B.$preset", enableB2B)
		prop.setProperty("ultra.enableCombo.$preset", enableCombo)
		prop.setProperty("ultra.big.$preset", big)
		prop.setProperty("ultra.goaltype.$preset", goaltype)
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goaltype)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 17)

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
						if(bgmno<0) bgmno = BGM.count-1
						if(bgmno>=BGM.count) bgmno = 0
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
					loadPreset(engine, owner.modeConfig, presetNumber)

					// NET: Signal options change
					if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
				} else if(menuCursor==17) {
					savePreset(engine, owner.modeConfig, presetNumber)
					receiver.saveModeConfig(owner.modeConfig)
				} else {
					owner.modeConfig.setProperty("ultra.presetNumber", presetNumber)
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
			if(engine.ctrl!!.isPush(Controller.BUTTON_D)&&netIsNetPlay&&!netIsWatch
				&&netIsNetRankingViewOK(engine))
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

	/* Setting screen drawing */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(netIsNetRankingDisplayMode)
		// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, playerID, receiver)
		else {
			drawMenuSpeeds(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0, engine.speed.gravity, engine.speed.denominator, engine.speed.are, engine.speed.areLine, engine.speed.lineDelay, engine.speed.lockDelay, engine.speed.das)
			drawMenuBGM(engine, playerID, receiver, bgmno)
			drawMenuCompact(engine, playerID, receiver, "BIG", GeneralUtil.getONorOFF(big), "GOAL", (goaltype+1).toString()+"MIN")
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
		engine.meterValue = 320
		engine.meterColor = GameEngine.METER_COLOR_GREEN

		if(netIsWatch)
			owner.bgmStatus.bgm = BGM.SILENT
		else
			owner.bgmStatus.bgm = BGM.values[bgmno]

		engine.tspinAllowKick = enableTSpinKick
		if(tspinEnableType==0)
			engine.tspinEnable = false
		else if(tspinEnableType==1)
			engine.tspinEnable = true
		else {
			engine.tspinEnable = true
			engine.useAllSpinBonus = true
		}

		engine.spinCheckType = spinCheckType
		engine.tspinEnableEZ = tspinEnableEZ
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, playerID, 0, 0, "ULTRA SCORE ATTACK", EventReceiver.COLOR.CYAN)
		receiver.drawScoreFont(engine, playerID, 0, 1, "("+(goaltype+1)+" MINUTES SPRINT)", EventReceiver.COLOR.CYAN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				receiver.drawScoreFont(engine, playerID, 0, 3, "SCORE RANKING", EventReceiver.COLOR.GREEN)
				receiver.drawScoreFont(engine, playerID, 3, 4, "SCORE  LINE", EventReceiver.COLOR.BLUE)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, 5+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW)
					receiver.drawScoreNum(engine, playerID, 3, 5+i, rankingScore!![goaltype][0][i].toString(), i==rankingRank[0])
					receiver.drawScoreNum(engine, playerID, 10, 5+i, rankingLines!![goaltype][0][i].toString(), i==rankingRank[0])
				}

				receiver.drawScoreFont(engine, playerID, 0, 11, "LINE RANKING", EventReceiver.COLOR.GREEN)
				receiver.drawScoreFont(engine, playerID, 3, 12, "LINE SCORE", EventReceiver.COLOR.BLUE)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, 13+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW)
					receiver.drawScoreNum(engine, playerID, 3, 13+i, rankingLines!![goaltype][1][i].toString(), i==rankingRank[1])
					receiver.drawScoreNum(engine, playerID, 8, 13+i, rankingScore!![goaltype][1][i].toString(), i==rankingRank[1])
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "SCORE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 3, "+$lastscore")
			receiver.drawScoreNum(engine, playerID, 0, 4, scgettime.toString(), 2f)
			if(scgettime<engine.statistics.score) scgettime += Math.ceil(((engine.statistics.score-scgettime)/10f).toDouble()).toInt()

			receiver.drawScoreFont(engine, playerID, 0, 6, "LINE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 7, engine.statistics.lines.toString(), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 9, "SCORE/MIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, String.format("%-10g", engine.statistics.spm), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 12, "LINE/MIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 13, engine.statistics.lpm.toString(), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 15, "TIME", EventReceiver.COLOR.BLUE)
			var time = (goaltype+1)*3600-engine.statistics.time
			if(time<0) time = 0
			var fontcolor = EventReceiver.COLOR.WHITE
			if(time<30*60&&time>0) fontcolor = EventReceiver.COLOR.YELLOW
			if(time<20*60&&time>0) fontcolor = EventReceiver.COLOR.ORANGE
			if(time<10*60&&time>0) fontcolor = EventReceiver.COLOR.RED
			receiver.drawScoreNum(engine, playerID, 0, 16, GeneralUtil.getTime(time.toFloat()), fontcolor, 2f)
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

	/* Each frame Processing at the end of */
	override fun onLast(engine:GameEngine, playerID:Int) {
		if(engine.gameActive&&engine.timerActive) {
			val limitTime = (goaltype+1)*3600
			val remainTime = (goaltype+1)*3600-engine.statistics.time

			// Time meter
			engine.meterValue = remainTime*receiver.getMeterMax(engine)/limitTime
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainTime<=20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED

			if(!netIsWatch) {
				// Out of time
				if(engine.statistics.time>=limitTime) {
					engine.gameEnded()
					engine.resetStatc()
					engine.stat = GameEngine.Status.ENDINGSTART
					return
				}

				// 10Seconds before the countdown
				if(engine.statistics.time>=limitTime-10*60&&engine.statistics.time%60==0) engine.playSE("countdown")

				// 5Of seconds beforeBGM fadeout
				if(engine.statistics.time>=limitTime-5*60) owner.bgmStatus.fadesw = true

				// 1Per-minuteBackgroundSwitching
				if(engine.statistics.time>0&&engine.statistics.time%3600==0) {
					engine.playSE("levelup")
					owner.backgroundStatus.fadesw = true
					owner.backgroundStatus.fadebg = owner.backgroundStatus.bg+1
				}
			}
		}
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, AbstractMode.Statistic.SCORE)
		if(rankingRank[0]!=-1) {
			val strRank = String.format("RANK %d", rankingRank[0]+1)
			receiver.drawMenuFont(engine, playerID, 4, 2, strRank, EventReceiver.COLOR.ORANGE)
		}

		drawResultStats(engine, playerID, receiver, 3, EventReceiver.COLOR.BLUE, AbstractMode.Statistic.LINES)
		if(rankingRank[1]!=-1) {
			val strRank = String.format("RANK %d", rankingRank[1]+1)
			receiver.drawMenuFont(engine, playerID, 4, 5, strRank, EventReceiver.COLOR.ORANGE)
		}

		drawResultStats(engine, playerID, receiver, 6, EventReceiver.COLOR.BLUE, AbstractMode.Statistic.PIECE, AbstractMode.Statistic.SPL, AbstractMode.Statistic.SPM, AbstractMode.Statistic.LPM, AbstractMode.Statistic.PPS)

		drawResultNetRank(engine, playerID, receiver, 16, EventReceiver.COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 18, EventReceiver.COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenuFont(engine, playerID, 2, 21, "NEW PB", EventReceiver.COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, playerID, 0, 22, "SENDING...", EventReceiver.COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, playerID, 1, 22, "A: RETRY", EventReceiver.COLOR.RED)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		savePreset(engine, engine.owner.replayProp, -1)
		engine.owner.replayProp.setProperty("ultra.version", version)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty(playerID.toString()+".net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.score, engine.statistics.lines)

			if(rankingRank[0]!=-1||rankingRank[1]!=-1) {
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
			for(j in 0 until RANKING_TYPE)
				for(k in 0 until RANKING_MAX) {
					rankingScore!![i][j][k] = prop.getProperty("ultra.ranking.$ruleName.$i.$j.score.$k", 0)
					rankingLines!![i][j][k] = prop.getProperty("ultra.ranking.$ruleName.$i.$j.lines.$k", 0)
				}
	}

	/** Save rankings to property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	private fun saveRanking(prop:CustomProperties?, ruleName:String) {
		for(i in 0 until GOALTYPE_MAX)
			for(j in 0 until RANKING_TYPE)
				for(k in 0 until RANKING_MAX) {
					prop!!.setProperty("ultra.ranking.$ruleName.$i.$j.score.$k", rankingScore!![i][j][k])
					prop.setProperty("ultra.ranking.$ruleName.$i.$j.lines.$k", rankingLines!![i][j][k])
				}
	}

	/** Update rankings
	 * @param sc Score
	 * @param li Lines
	 */
	private fun updateRanking(sc:Int, li:Int) {
		for(i in 0 until RANKING_TYPE) {
			rankingRank[i] = checkRanking(sc, li, i)

			if(rankingRank[i]!=-1) {
				// Shift down ranking entries
				for(j in RANKING_MAX-1 downTo rankingRank[i]+1) {
					rankingScore!![goaltype][i][j] = rankingScore!![goaltype][i][j-1]
					rankingLines!![goaltype][i][j] = rankingLines!![goaltype][i][j-1]
				}

				// Add new data
				rankingScore!![goaltype][i][rankingRank[i]] = sc
				rankingLines!![goaltype][i][rankingRank[i]] = li
			}
		}
	}

	/** Calculate ranking position
	 * @param sc Score
	 * @param li Lines
	 * @param rankingtype Number of ranking types
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Int, li:Int, rankingtype:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(rankingtype==0) {
				if(sc>rankingScore!![goaltype][rankingtype][i])
					return i
				else if(sc==rankingScore!![goaltype][rankingtype][i]&&li>rankingLines!![goaltype][rankingtype][i]) return i
			} else if(li>rankingLines!![goaltype][rankingtype][i])
				return i
			else if(li==rankingLines!![goaltype][rankingtype][i]&&sc>rankingScore!![goaltype][rankingtype][i]) return i

		return -1
	}

	/** NET: Send various in-game stats (as well as goaltype)
	 * @param engine GameEngine
	 */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(owner.backgroundStatus.fadesw) owner.backgroundStatus.fadebg else owner.backgroundStatus.bg
		var msg = "game\tstats\t"
		msg += engine.statistics.score.toString()+"\t"+engine.statistics.lines+"\t"+engine.statistics.totalPieceLocked+"\t"
		msg += engine.statistics.time.toString()+"\t"+engine.statistics.spm+"\t"
		msg += engine.statistics.lpm.toString()+"\t"+engine.statistics.spl+"\t"+goaltype+"\t"
		msg += engine.gameActive.toString()+"\t"+engine.timerActive+"\t"
		msg += lastscore.toString()+"\t"+scgettime+"\t"+engine.lastevent+"\t"+lastb2b+"\t"+lastcombo+"\t"+lastpiece+"\t"
		msg += bg.toString()+"\n"
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
		engine.lastevent = Integer.parseInt(message[16])
		lastb2b = java.lang.Boolean.parseBoolean(message[17])
		lastcombo = Integer.parseInt(message[18])
		lastpiece = Integer.parseInt(message[19])
		owner.backgroundStatus.bg = Integer.parseInt(message[20])

		// Time meter
		val limitTime = (goaltype+1)*3600
		val remainTime = (goaltype+1)*3600-engine.statistics.time
		engine.meterValue = remainTime*receiver.getMeterMax(engine)/limitTime
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(remainTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(remainTime<=20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(remainTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		var subMsg = ""
		subMsg += "SCORE;"+engine.statistics.score+"\t"
		subMsg += "LINE;"+engine.statistics.lines+"\t"
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

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Number of entries in rankings */
		private const val RANKING_MAX = 5

		/** Number of ranking types */
		private const val RANKING_TYPE = 2

		/** Time limit type */
		private const val GOALTYPE_MAX = 5
	}
}
