/*
 * Copyright (c) 2010-2021, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
 *
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
package io.itch.oshisaure

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.net.NetUtil.urlEncode
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.NetDummyMode
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/**
 * MARATHON Mode
 */
class MarathonZone:NetDummyMode() {
	/** Most recent increase in score  */
	private var lastscore = 0
	/** Time to display the most recent increase in score  */
	private var scgettime = 0
	/** Last extension of zone time   */
	private var lastzonegain = 0
	/** Most recent amount of zone lines  */
	private var lastzonelines = 0
	/** Most recent zone bonus  */
	private var lastzonebonus = 0
	/** Time of zone accumulated  */
	private var zoneframes = 0
	/** Time to display most recent zone time extend  */
	private var zonegaintimer = 0
	/** Time to display most recent zone result  */
	private var zonedisplayframes = 0
	/** Zone activation flag  */
	private var inzone:Boolean = false
	/** True if most recent scoring event is a B2B  */
	private var lastb2b:Boolean = false
	/** Combo count for most recent scoring event  */
	private var lastcombo = 0
	/** Current BGM  */
	private var bgmlv = 0
	/** Level at start time  */
	private var startlevel = 0
	/** Game type  */
	private var goaltype = 0
	/** Version  */
	private var version = 0
	/** Current round's ranking rank  */
	private var rankingRank = 0
	/** Rankings' scores  */
	private var rankingScore = emptyArray<IntArray>()
	/** Rankings' line counts  */
	private var rankingLines = emptyArray<IntArray>()
	/** Rankings' times  */
	private var rankingTime = emptyArray<IntArray>()
	/**
	 * Calculate bonus points using a 4th degree polynomial.
	 * @param li lines cleared
	 * @param level current level (multiplier)
	 * @return number of points to award
	 */
	fun getZoneBonus4(li:Int, level:Int):Int = if(li<=0) 0 else level*(25*li*li*li*li-200*li*li*li+575*li*li-100*li)/3
	/**
	 * Calculate bonus points using a 3rd degree polynomial.
	 * @param li lines cleared
	 * @param level current level (multiplier)
	 * @return number of points to award
	 */
	fun getZoneBonus3(li:Int, level:Int):Int = if(li<=0) 0 else level*(50*li*li*li-300*li*li+1150*li-600)/3
	/** Ends zone effect  */
	fun endZone(engine:GameEngine) {
		engine.field.cutLine(engine.field.height-1, lastzonelines)
		engine.nowPieceBottomY += lastzonelines
		zoneframes = 0
		if(lastzonelines>0) {
			zonedisplayframes = 0
			engine.playSE("erase3")
			if(lastzonelines>=10) engine.playSE("bravo")
		}
		lastzonebonus = getZoneBonus3(lastzonelines, engine.statistics.level+1)
		engine.statistics.scoreBonus += lastzonebonus
		inzone = false
		engine.framecolor = GameEngine.FRAME_COLOR_CYAN
		setSpeed(engine)
		engine.playSE("applause${maxOf(lastzonelines/5, 20)}")
		engine.playSE("cool")
		if((engine.statistics.lines>=tableGameClearLines[goaltype])&&(tableGameClearLines[goaltype]>=0)) {
			// Ending
			engine.ending = 1
			engine.gameEnded()
			engine.stat = GameEngine.Status.ENDINGSTART
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/*
		 * Mode name
		 */
	override val name:String = "Zone Journey"

	/*
	 * Initialization
	 */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		scgettime = 0
		lastb2b = false
		lastcombo = 0
		bgmlv = 0
		lastzonegain = 0
		lastzonelines = 0
		lastzonebonus = 0
		zoneframes = 0
		zonedisplayframes = 0
		zonegaintimer = 0
		inzone = false
		rankingRank = -1
		rankingScore = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLines = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingTime = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		netPlayerInit(engine, playerID)
		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.recordProp, engine.ruleOpt.strRuleName)
			version = CURRENT_VERSION
		} else {
			loadSetting(owner.replayProp)
			if(version==0&&owner.replayProp.getProperty("marathonzone.endless", false)) goaltype = 2

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("$playerID.net.netPlayerName", "")
		}
		engine.owner.backgroundStatus.bg = startlevel
		engine.framecolor = GameEngine.FRAME_COLOR_CYAN
	}
	/**
	 * Set the gravity rate
	 * @param engine GameEngine
	 */
	fun setSpeed(engine:GameEngine) {
		var lv = engine.statistics.level
		if(lv<0) lv = 0
		if(lv>=tableGravity.size) lv = tableGravity.size-1
		engine.speed.gravity = tableGravity[lv]
		engine.speed.denominator = tableDenominator[lv]
		engine.speed.are = 8
		engine.speed.areLine = 6
		engine.speed.lineDelay = 12
	}
	/*
	 * Called at settings screen
	 */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode) {
			netOnUpdateNetPlayRanking(engine, goaltype)
		} else if(engine.owner.replayMode==false) {
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
						goaltype += change
						if(goaltype<0) goaltype = GAMETYPE_MAX-1
						if(goaltype>GAMETYPE_MAX-1) goaltype = 0
						if((startlevel>(tableGameClearLines[goaltype]-1)/10)&&(tableGameClearLines[goaltype]>=0)) {
							startlevel = (tableGameClearLines[goaltype]-1)/10
							engine.owner.backgroundStatus.bg = startlevel
						}
					}
				}

				// NET: Signal options change
				if(netIsNetPlay&&(netNumSpectators>0)) {
					netSendOptions(engine)
				}
			}

			// Confirm
			if(engine.ctrl.isPush(mu.nu.nullpo.game.component.Controller.BUTTON_A)&&(engine.statc[3]>=5)) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig)
				owner.saveModeConfig()

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby?.netPlayerClient?.send("start1p\n")
				return false
			}

			// Cancel
			if(engine.ctrl.isPush(mu.nu.nullpo.game.component.Controller.BUTTON_B)&&!netIsNetPlay) {
				engine.quitflag = true
			}

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(mu.nu.nullpo.game.component.Controller.BUTTON_D)&&netIsNetPlay&&(startlevel==0)&&(
					engine.ai==null)) {
				netEnterNetPlayRankingScreen(engine, playerID, goaltype)
			}
			engine.statc[3]++
		} else {
			engine.statc[3]++
			engine.statc[2] = -1
			if(engine.statc[3]>=60) {
				return false
			}
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
			drawMenu(engine, playerID, receiver, 0, COLOR.BLUE, 0,
				"LEVEL" to (startlevel+1),
				"GOAL" to
					if((goaltype==2)) "ENDLESS" else "${tableGameClearLines[goaltype]} LINES")
		}
	}
	/*
	 * Called for initialization during "Ready" screen
	 */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel
		engine.statistics.levelDispAdd = 1
		engine.b2bEnable = true
		inzone = false
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.big = false
		engine.twistAllowKick = true
		engine.twistEnable = true
		engine.useAllSpinBonus = true
//		engine.spinCheckType = spinCheckType
		engine.twistEnableEZ = true
		setSpeed(engine)
		if(netIsWatch) {
			owner.bgmStatus.bgm = BGMStatus.BGM.Silent
		} else
			owner.bgmStatus.bgm = BGMStatus.BGM.Generic(bgmlv)
	}
	/*
	 * Render score
	 */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return
		val titlecolor = if(inzone) COLOR.RAINBOW else COLOR.CYAN
		val hudcolor = if(inzone) COLOR.RAINBOW else COLOR.BLUE
		receiver.drawScoreFont(engine, playerID, 0, 0, "ZONE MARATHON", titlecolor)
		receiver.drawScoreFont(engine, playerID, 0, 1,
			if(tableGameClearLines[goaltype]==-1) "(Endless run)" else "(${tableGameClearLines[goaltype]} Lines run)", titlecolor)
		if((engine.stat===GameEngine.Status.SETTING)||((engine.stat===GameEngine.Status.RESULT)&&(!owner.replayMode))) {
			if((!owner.replayMode)&&(engine.ai==null)) {
				val scale:Float = if((receiver.nextDisplayType==2)) 0.5f else 1.0f
				val topY = if((receiver.nextDisplayType==2)) 6 else 4
				receiver.drawScoreFont(engine, playerID, 2, topY-1, "SCORE    LINE TIME", hudcolor, scale)
				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, -1, topY+i, String.format("%2d", i+1), COLOR.YELLOW,
						scale)
					receiver.drawScoreNum(engine, playerID, 2, topY+i, "${rankingScore[goaltype][i]}", (i==rankingRank),
						scale)
					receiver.drawScoreNum(engine, playerID, 11, topY+i, "${rankingLines[goaltype][i]}", (i==rankingRank),
						scale)
					receiver.drawScoreNum(engine, playerID, 16, topY+i, rankingTime[goaltype][i].toTimeStr,
						(i==rankingRank), scale)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "SCORE", hudcolor)
			val strScore:String = if(lastscore==0||scgettime>=120) "${engine.statistics.score}"
			else engine.statistics.score.toString()+"(+$lastscore)"

			receiver.drawScoreNum(engine, playerID, 0, 4, strScore)
			receiver.drawScoreFont(engine, playerID, 0, 6, "LINE", hudcolor)
			receiver.drawScoreNum(engine, playerID, 0, 7,
				if(engine.statistics.level>=19&&tableGameClearLines[goaltype]<0) "${engine.statistics.lines}" else
					"${engine.statistics.lines}/${(engine.statistics.level+1)*24}")
			receiver.drawScoreFont(engine, playerID, 0, 9, "LEVEL", hudcolor)
			receiver.drawScoreNum(engine, playerID, 0, 10, "${engine.statistics.level+1}")
			receiver.drawScoreFont(engine, playerID, 0, 12, "TIME", hudcolor)
			receiver.drawScoreNum(engine, playerID, 0, 13, engine.statistics.time.toTimeStr)
			receiver.drawScoreFont(engine, playerID, 0, 15, "ZONE", hudcolor)
			val colZone = when {
				inzone -> COLOR.RAINBOW
				zoneframes<maxzonetime/4 -> COLOR.RED
				zoneframes>=maxzonetime/2 -> COLOR.YELLOW
				zoneframes>=maxzonetime -> COLOR.CYAN
				else -> COLOR.GREEN
			}

			receiver.drawScoreNum(engine, playerID, 0, 16, "$zoneframes", colZone)
			receiver.drawScoreNum(
				engine, playerID, 0, 17, zoneframes.toTimeStr,colZone
				)
			if(zonedisplayframes<180&&lastzonelines>0) {
				val linetxt = String.format("%2d", lastzonelines)+" LINES!"
				val pointtxt = "+$lastzonebonus PTS."
				receiver.drawMenuFont(engine, playerID, 1, engine.field.height/2, linetxt, (zonedisplayframes%2)==0)
				receiver.drawMenuFont(engine, playerID, 6-(pointtxt.length/2+1), engine.field.height/2+1, pointtxt,
					(zonedisplayframes%2)==0)
			}
		}

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

	override fun onFirst(engine:GameEngine, playerID:Int) {
		if(engine.ctrl.isPush(mu.nu.nullpo.game.component.Controller.BUTTON_F)&&(zoneframes>maxzonetime/4)&&!inzone) {
			inzone = true
			lastzonelines = 0
			engine.framecolor = GameEngine.FRAME_COLOR_YELLOW
			engine.playSE("medal")
		}
		if(inzone) {
			engine.speed.gravity = if(engine.ctrl.isPress(engine.down)) tableGravity[0] else 0
			engine.speed.denominator = tableDenominator[0]
		}
	}
	/*
	 * Called after every frame
	 */
	override fun onLast(engine:GameEngine, playerID:Int) {
		// Meter
		engine.meterValue = (zoneframes*receiver.getMeterMax(engine))/maxzonetime
		engine.meterColor = if(inzone) GameEngine.METER_COLOR_YELLOW else GameEngine.METER_COLOR_GREEN
		scgettime++
		zonedisplayframes++
		zonegaintimer++
		if(inzone) {
			for(y in engine.field.height-1 downTo engine.field.height-lastzonelines) {
				for(x in 0 until engine.field.width)
					engine.field.getBlock(x, y)?.color = Block.COLOR.colors()[(x+y+(zoneframes/4))%Block.COLOR.COLOR_NUM]

			}
			if(zoneframes>0) zoneframes--
			//else endZone(engine)

		}
	}

	override fun onARE(engine:GameEngine, playerID:Int):Boolean {
		if(inzone&&zoneframes<=0) endZone(engine)
		return super.onARE(engine, playerID)
	}

	override fun onLineClear(engine:GameEngine, playerID:Int):Boolean {
		//return inzone;
		if(inzone) {
			/*
			int newlines = engine.field.checkLine();
			for(int i = 0; i < engine.field.getHeight(); i++) {
				if(engine.field.getLineFlag(i)) {
					for(int j = 0; j < engine.field.getWidth(); j++) {
						Block blk = engine.field.getBlock(j, i);

						if(blk != null) {
							if(owner.mode != null) owner.mode.blockBreak(engine, playerID, j, i, blk);
							owner.receiver.blockBreak(engine, playerID, j, i, blk);
						}
					}
				}
			}
			engine.field.clearLine();
			engine.field.pushUp(newlines);
			*/
			val newlines = engine.field.checkLine()
			for(y in engine.field.height-1 downTo engine.field.height-lastzonelines) {
				engine.field.setLineFlag(y, false)
			}
			for(y in -engine.field.hiddenHeight until engine.field.height) {
				if(engine.field.getLineFlag(y)) engine.field.cutLine(y, 1)
			}
			val nextLines = newlines-lastzonelines
			if(nextLines>0) {
				engine.field.pushUp(nextLines)
				for(y in engine.field.height-1 downTo engine.field.height-nextLines)
					for(x in 0 until engine.field.width)
						engine.field.setBlock(x,y,Block(Block.COLOR.BLACK))
				if(newlines>10) engine.playSE("combo_pow", minOf(2f, 1f+(newlines-11)/9f))
				else engine.playSE("combo", minOf(2f, 1f+(newlines-1)/10f))
			}
			lastzonelines = newlines
			engine.owner.mode?.calcScore(engine, playerID, newlines)
			engine.owner.receiver.calcScore(engine, null)
			engine.statc[0] = 0
			engine.statc[1] = engine.are
			engine.statc[2] = 1
			engine.stat = GameEngine.Status.ARE
			return true
		}
		return false
	}
	/*
	 * Calculate score
	 */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Zone timer bonus
		if(!inzone&&lines>0) {
			lastzonegain = lines*lines*6
			zoneframes = minOf(zoneframes+lastzonegain, maxzonetime) //20s cap
			zonegaintimer = 0
		}

		// Line clear bonus
		var pts = 0
		if(engine.twist) {
			when {
				lines==0&&!engine.twistez -> pts += if(engine.twistmini) 10 else 40
				engine.twistez&&(lines>0) -> pts += if(engine.b2b) 18 else 12
				lines==1 -> pts += if(engine.twistmini) (if(engine.b2b) 30 else 20) else (if(engine.b2b) 120 else 80)
				lines==2 -> pts += if(engine.twistmini&&engine.useAllSpinBonus) (if(engine.b2b) 60 else 40) else (if(engine.b2b) 180 else 120)
				lines>=3 -> pts += if(engine.b2b) 240 else 160
			}
		} else {
			when {
				lines==1 -> pts += 10
				lines==2 -> pts += 30
				lines==3 -> pts += 50
				lines>=4 -> pts += if(engine.b2b) 120 else 80
			}
		}
		lastb2b = engine.b2b

		// Combo
		if(engine.combo>=1&&lines>=1) {
			pts += ((engine.combo-1)*5)
			lastcombo = engine.combo
		}

		// All clear
		if((lines>=1)&&(engine.field.isEmpty)) {
			engine.playSE("bravo")
			pts += 180
		}

		pts *= (engine.statistics.level+10)
		// Add to score
		if(pts>0) {
			lastscore = pts
			scgettime = 0
			if(lines>=1) engine.statistics.scoreLine += pts else engine.statistics.scoreBonus += pts
		}

		// BGM fade-out effects and BGM changes
		if(tableBGMChange[bgmlv]!=-1) {
			if(engine.statistics.lines>=tableBGMChange[bgmlv]-5) owner.bgmStatus.fadesw = true
			if(engine.statistics.lines>=tableBGMChange[bgmlv]&&
				(engine.statistics.lines<tableGameClearLines[goaltype]||tableGameClearLines[goaltype]<0)) {
				bgmlv++
				owner.bgmStatus.bgm = BGMStatus.BGM.Generic(bgmlv)
				owner.bgmStatus.fadesw = false
			}
		}
		if(engine.statistics.lines>=tableGameClearLines[goaltype]&&tableGameClearLines[goaltype]>=0) {
			// Ending
			engine.ending = 1
			engine.gameEnded()
		} else if(engine.statistics.lines>=(engine.statistics.level+1)*24&&engine.statistics.level<19) {
			// Level up
			engine.statistics.level++
			owner.backgroundStatus.fadesw = true
			owner.backgroundStatus.fadecount = 0
			owner.backgroundStatus.fadebg = engine.statistics.level
			if(!inzone) setSpeed(engine)
			engine.playSE("levelup")
		}

		return pts
	}

	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		if(inzone) {
			endZone(engine)
			engine.resetStatc()
			if(engine.ending==0) engine.stat = GameEngine.Status.MOVE
			return true
		}
		return false
	}
	/*
	 * Soft drop
	 */
	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreSD += fall
	}
	/*
	 * Hard drop
	 */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreHD += fall*2
	}
	/*
	 * Render results screen
	 */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(engine, playerID, receiver, 0, COLOR.BLUE,
			Statistic.SCORE, Statistic.LINES, Statistic.LEVEL, Statistic.TIME, Statistic.SPL, Statistic.LPM)
		drawResultRank(engine, playerID, receiver, 12, COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, playerID, receiver, 14, COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 16, COLOR.BLUE, netRankingRank[1])
		if(netIsPB) {
			receiver.drawMenuFont(engine, playerID, 2, 21, "NEW PB", COLOR.ORANGE)
		}
		if(netIsNetPlay&&(netReplaySendStatus==1)) {
			receiver.drawMenuFont(engine, playerID, 0, 22, "SENDING...", COLOR.PINK)
		} else if(netIsNetPlay&&!netIsWatch&&(netReplaySendStatus==2)) {
			receiver.drawMenuFont(engine, playerID, 1, 22, "A: RETRY", COLOR.RED)
		}
	}
	/*
	 * Called when saving replay
	 */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)

		// NET: Save name
		if(!netPlayerName.isNullOrEmpty()) prop.setProperty("$playerID.net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&engine.ai==null) {
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.time, goaltype)
			if(rankingRank!=-1) {
				saveRanking(engine.ruleOpt.strRuleName)
				owner.saveModeConfig()
			}
		}
	}
	/**
	 * Load settings from property file
	 * @param prop Property file
	 */
	override fun loadSetting(prop:CustomProperties) {
		startlevel = prop.getProperty("marathonzone.startlevel", 0)
		goaltype = prop.getProperty("marathonzone.gametype", 0)
		version = prop.getProperty("marathonzone.version", 0)
	}
	/**
	 * Save settings to property file
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("marathonzone.startlevel", startlevel)
		prop.setProperty("marathonzone.gametype", goaltype)
		prop.setProperty("marathonzone.version", version)
	}
	/**
	 * Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX) for(j in 0 until GAMETYPE_MAX) {
			rankingScore[j][i] = prop.getProperty("$ruleName.$j.score.$i", 0)
			rankingLines[j][i] = prop.getProperty("$ruleName.$j.lines.$i", 0)
			rankingTime[j][i] = prop.getProperty("$ruleName.$j.time.$i", 0)
		}
	}
	/**
	 * Save rankings to property file
	 * @param ruleName Rule name
	 */
	private fun saveRanking(ruleName:String) =
		super.saveRanking(ruleName, (0 until GAMETYPE_MAX).flatMap {j ->
			(0 until RANKING_MAX).flatMap {i ->
				listOf(
					"$ruleName.$j.score.$i" to rankingScore[j][i],
					"$ruleName.$j.lines.$i" to rankingLines[j][i],
					"$ruleName.$j.time.$i" to rankingTime[j][i])
			}
		})

	/**
	 * Update rankings
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
	/**
	 * Calculate ranking position
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Int, li:Int, time:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(sc>rankingScore[type][i]) return i
			else if((sc==rankingScore[type][i])&&(li>rankingLines[type][i])) return i
			else if((sc==rankingScore[type][i])&&(li==rankingLines[type][i])&&(time<rankingTime[type][i])) return i
		return -1
	}
	/**
	 * NET: Send various in-game stats (as well as goaltype)
	 * @param engine GameEngine
	 */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(engine.owner.backgroundStatus.fadesw) engine.owner.backgroundStatus.fadebg else engine.owner.backgroundStatus.bg
		val msg = "game\tstats\t"+
			"${engine.statistics.scoreLine}\t${engine.statistics.scoreSD}\t${engine.statistics.scoreHD}\t${engine.statistics.scoreBonus}\t"+
			"${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t${engine.statistics.time}\t${engine.statistics.level}\t"+
			"$goaltype\t${engine.gameActive}\t${engine.timerActive}\t"+
			"$lastscore\t$scgettime\t$lastb2b\t$lastcombo\t"+
			"$bg\n"
		netLobby?.netPlayerClient?.send(msg)
	}
	/**
	 * NET: Receive various in-game stats (as well as goaltype)
	 */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		engine.statistics.scoreLine = message[4].toInt()
		engine.statistics.scoreSD = message[5].toInt()
		engine.statistics.scoreHD = message[6].toInt()
		engine.statistics.scoreBonus = message[7].toInt()
		engine.statistics.lines = message[8].toInt()
		engine.statistics.totalPieceLocked = message[9].toInt()
		engine.statistics.time = message[10].toInt()
		engine.statistics.level = message[11].toInt()
		goaltype = message[12].toInt()
		engine.gameActive = message[13].toBoolean()
		engine.timerActive = message[14].toBoolean()
		lastscore = message[15].toInt()
		scgettime = message[16].toInt()
		lastb2b = message[17].toBoolean()
		lastcombo = message[18].toInt()
		engine.owner.backgroundStatus.bg = message[19].toInt()

		// Meter
		engine.meterValue = ((engine.statistics.lines%10)*receiver.getMeterMax(engine))/9
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(engine.statistics.lines%10>=4) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(engine.statistics.lines%10>=6) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(engine.statistics.lines%10>=8) engine.meterColor = GameEngine.METER_COLOR_RED
	}
	/**
	 * NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		val subMsg:String =
			"SCORE;${engine.statistics.score}\t"+
				"LINE;${engine.statistics.lines}\t"+
				"LEVEL;${engine.statistics.level+engine.statistics.levelDispAdd}\t"+
				"TIME;${engine.statistics.time.toTimeStr}\t"+
				"SCORE/LINE;${engine.statistics.spl}\t"+
				"LINE/MIN;${engine.statistics.lpm}\t"
		val msg = "gstat1p\t${urlEncode(subMsg)}\n"
		netLobby?.netPlayerClient?.send(msg)
	}
	/**
	 * NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t$startlevel\t$goaltype\n"
		netLobby?.netPlayerClient?.send(msg)
	}
	/**
	 * NET: Receive game options
	 */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		startlevel = message[4].toInt()
		goaltype = message[5].toInt()
	}
	/**
	 * NET: Get goal type
	 */
	override fun netGetGoalType():Int = goaltype
	/**
	 * NET: It returns true when the current settings doesn't prevent leaderboard screen from showing.
	 */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = ((startlevel==0)&&(engine.ai==null))

	companion object {
		/** Current version  */
		private val CURRENT_VERSION = 2
		/** Fall velocity table (numerators)  */
		private val tableGravity = intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 465, 731, 1280, 1707, -1, -1, -1)
		/** Fall velocity table (denominators)  */
		private val tableDenominator = intArrayOf(63, 50, 39, 30, 22, 16, 12, 8, 6, 4, 3, 2, 1, 256, 256, 256, 256, 256, 256, 256)
		/** Line counts when BGM changes occur  */
		private val tableBGMChange = intArrayOf(5*24, 10*24, 15*24, 20*24, -1)
		/** Line counts when game ending occurs  */
		private val tableGameClearLines = intArrayOf(15*24, 20*24, -1)
		private val maxzonetime = 1200 // 20 seconds
		/** Number of entries in rankings  */
		private val RANKING_MAX = 13
		/** Number of game types  */
		private val GAMETYPE_MAX = 3
		/** Number of ranking types  */
		private val RANKING_TYPE = GAMETYPE_MAX
	}
}