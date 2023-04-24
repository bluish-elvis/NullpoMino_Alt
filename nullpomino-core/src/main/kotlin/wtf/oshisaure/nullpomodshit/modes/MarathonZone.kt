/*
 Copyright (c) 2022-2023,
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

package wtf.oshisaure.nullpomodshit.modes

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.NetDummyMode
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.LevelMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.game.subsystem.mode.menu.StringsMenuItem
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/**
 * MARATHON Mode
 */
class MarathonZone:NetDummyMode() {
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
	private var bgmLv = 0

	private val itemLevel = LevelMenuItem("startlevel", "Level", EventReceiver.COLOR.BLUE, 0, 0..19)
	/** Level at start time  */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemMode = StringsMenuItem("goalType", "GOAL", EventReceiver.COLOR.BLUE, 0,
		List(GAMETYPE_MAX) {if(tableGameClearLines[it]<0) "ENDLESS" else "${tableGameClearLines[it]} LINES"})
	/** Game type  */
	private var goalType:Int by DelegateMenuItem(itemMode)
	/** Version  */
	private var version = 0
	/** Current round's ranking position  */
	private var rankingRank = 0
	/** Rankings' scores  */
	private val rankingScore = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0L}}
	/** Rankings' line counts  */
	private val rankingLines = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}
	/** Rankings' times  */
	private val rankingTime = List(RANKING_TYPE) {MutableList(RANKING_MAX) {-1}}

	override val rankMap
		get() = rankMapOf(rankingScore.mapIndexed {a, x -> "$a.score" to x}+
			rankingLines.mapIndexed {a, x -> "$a.lines" to x}+
			rankingTime.mapIndexed {a, x -> "$a.time" to x})

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
		engine.frameColor = GameEngine.FRAME_COLOR_CYAN
		setSpeed(engine)
		engine.playSE("applause${maxOf(lastzonelines/5, 20)}")
		engine.playSE("cool")
		if((engine.statistics.lines>=tableGameClearLines[goalType])&&(tableGameClearLines[goalType]>=0)) {
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

	// Initialization
	override val menu:MenuList
		get() = MenuList("marathonzone")

	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastScore = 0
		lastb2b = false
		lastcombo = 0
		bgmLv = 0
		lastzonegain = 0
		lastzonelines = 0
		lastzonebonus = 0
		zoneframes = 0
		zonedisplayframes = 0
		zonegaintimer = 0
		inzone = false
		rankingRank = -1
		rankingScore.forEach {it.fill(0)}
		rankingLines.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(-1)}
		netPlayerInit(engine)
		if(!owner.replayMode) {
			version = CURRENT_VERSION
		} else {
			if(version==0&&owner.replayProp.getProperty("marathonzone.endless", false)) goalType = 2

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}
		engine.owner.bgMan.bg = startLevel
		engine.frameColor = GameEngine.FRAME_COLOR_CYAN
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
	override fun onSetting(engine:GameEngine):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode) {
			netOnUpdateNetPlayRanking(engine, goalType)
		} else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateMenu(engine)
			if(change!=0) {
				engine.owner.bgMan.bg = startLevel
				engine.playSE("change")

				// NET: Signal options change
				if(netIsNetPlay&&(netNumSpectators>0)) {
					netSendOptions(engine)
				}
			}

			// Confirm
			if(engine.ctrl.isPush(mu.nu.nullpo.game.component.Controller.BUTTON_A)&&(engine.statc[3]>=5)) {
				engine.playSE("decide")

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby?.netPlayerClient?.send("start1p\n")
				return false
			}

			// Cancel
			if(engine.ctrl.isPush(mu.nu.nullpo.game.component.Controller.BUTTON_B)&&!netIsNetPlay) {
				engine.quitFlag = true
			}

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(mu.nu.nullpo.game.component.Controller.BUTTON_D)&&netIsNetPlay&&(startLevel==0)&&(
					engine.ai==null)
			) {
				netEnterNetPlayRankingScreen(goalType)
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
	override fun renderSetting(engine:GameEngine) {
		if(netIsNetRankingDisplayMode) {
			// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, receiver)
		} else {
			drawMenu(
				engine, receiver, 0, EventReceiver.COLOR.BLUE, 0, "LEVEL" to (startLevel+1),
			)
		}
	}
	/*
	 * Called for initialization during "Ready" screen
	 */
	override fun startGame(engine:GameEngine) {
		engine.statistics.level = startLevel
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
			owner.musMan.bgm = BGMStatus.BGM.Silent
		} else
			owner.musMan.bgm = BGMStatus.BGM.Generic(bgmLv)
	}
	/*
	 * Render score
	 */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return
		val titlecolor = if(inzone) EventReceiver.COLOR.RAINBOW else EventReceiver.COLOR.CYAN
		val hudcolor = if(inzone) EventReceiver.COLOR.RAINBOW else EventReceiver.COLOR.BLUE
		receiver.drawScoreFont(engine, 0, 0, name, titlecolor)
		receiver.drawScoreFont(
			engine, 0, 1, if(tableGameClearLines[goalType]==-1) "(Endless run)" else "(${tableGameClearLines[goalType]} Lines run)",
			titlecolor
		)
		if((engine.stat===GameEngine.Status.SETTING)||((engine.stat===GameEngine.Status.RESULT)&&(!owner.replayMode))) {
			if((!owner.replayMode)&&(engine.ai==null)) {
				val scale:Float = if((receiver.nextDisplayType==2)) 0.5f else 1.0f
				val topY = if((receiver.nextDisplayType==2)) 6 else 4
				receiver.drawScoreFont(engine, 2, topY-1, "SCORE    LINE TIME", hudcolor, scale)
				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, -1, topY+i, "%2d".format(i+1), EventReceiver.COLOR.YELLOW, scale)
					receiver.drawScoreNum(engine, 2, topY+i, "${rankingScore[goalType][i]}", (i==rankingRank), scale)
					receiver.drawScoreNum(engine, 11, topY+i, "${rankingLines[goalType][i]}", (i==rankingRank), scale)
					receiver.drawScoreNum(
						engine, 16, topY+i, rankingTime[goalType][i].toTimeStr, (i==rankingRank),
						scale
					)
				}
			}
		} else {
			receiver.drawScoreFont(engine, 0, 3, "SCORE", hudcolor)
			val strScore = "${engine.statistics.score}(+$lastScore)"

			receiver.drawScoreNum(engine, 0, 4, strScore)
			receiver.drawScoreFont(engine, 0, 6, "LINE", hudcolor)
			receiver.drawScoreNum(
				engine, 0, 7, if(engine.statistics.level>=19&&tableGameClearLines[goalType]<0) "${engine.statistics.lines}" else
					"${engine.statistics.lines}/${(engine.statistics.level+1)*24}"
			)
			receiver.drawScoreFont(engine, 0, 9, "LEVEL", hudcolor)
			receiver.drawScoreNum(engine, 0, 10, "${engine.statistics.level+1}")
			receiver.drawScoreFont(engine, 0, 12, "TIME", hudcolor)
			receiver.drawScoreNum(engine, 0, 13, engine.statistics.time.toTimeStr)
			receiver.drawScoreFont(engine, 0, 15, "ZONE", hudcolor)
			val colZone = when {
				inzone -> EventReceiver.COLOR.RAINBOW
				zoneframes<maxzonetime/4 -> EventReceiver.COLOR.RED
				zoneframes>=maxzonetime/2 -> EventReceiver.COLOR.YELLOW
				zoneframes>=maxzonetime -> EventReceiver.COLOR.CYAN
				else -> EventReceiver.COLOR.GREEN
			}

			receiver.drawScoreNum(engine, 0, 16, "$zoneframes", colZone)
			receiver.drawScoreNum(
				engine, 0, 17, zoneframes.toTimeStr, colZone
			)
			if(zonedisplayframes<180&&lastzonelines>0) {
				val linetxt = "%2d".format(lastzonelines)+" LINES!"
				val pointtxt = "+$lastzonebonus PTS."
				receiver.drawMenuFont(engine, 1, engine.field.height/2, linetxt, (zonedisplayframes%2)==0)
				receiver.drawMenuFont(
					engine, 6-(pointtxt.length/2+1), engine.field.height/2+1, pointtxt, (zonedisplayframes%2)==0
				)
			}
		}

		// NET: Number of spectators
		netDrawSpectatorsCount(engine, 0, 18)
		// NET: All number of players
		if(engine.playerID==players-1) {
			netDrawAllPlayersCount()
			netDrawGameRate(engine)
		}
		// NET: Player name (It may also appear in offline replay)
		netDrawPlayerName(engine)
	}

	override fun onFirst(engine:GameEngine) {
		if(engine.ctrl.isPush(mu.nu.nullpo.game.component.Controller.BUTTON_F)&&(zoneframes>maxzonetime/4)&&!inzone) {
			inzone = true
			lastzonelines = 0
			engine.frameColor = GameEngine.FRAME_COLOR_YELLOW
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
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		// Meter
		engine.meterValue = (zoneframes*1f)/maxzonetime
		engine.meterColor = if(inzone) GameEngine.METER_COLOR_YELLOW else GameEngine.METER_COLOR_GREEN
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

	override fun onARE(engine:GameEngine):Boolean {
		if(inzone&&zoneframes<=0) endZone(engine)
		return super.onARE(engine)
	}

	override fun onLineClear(engine:GameEngine):Boolean {
		//return inzone;
		if(inzone) {
			/*
			int newlines = engine.field.checkLine();
			for(int i = 0; i < engine.field.getHeight(); i++) {
				if(engine.field.getLineFlag(i)) {
					for(int j = 0; j < engine.field.getWidth(); j++) {
						Block blk = engine.field.getBlock(j, i);

						if(blk != null) {
							if(owner.mode != null) owner.mode.blockBreak(engine, engine.playerID, j, i, blk);
							owner.receiver.blockBreak(engine, engine.playerID, j, i, blk);
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
						engine.field.setBlock(x, y, Block(Block.COLOR.BLACK))
				if(newlines>10) engine.playSE("combo_pow", minOf(2f, 1f+(newlines-11)/9f))
				else engine.playSE("combo", minOf(2f, 1f+(newlines-1)/10f))
			}
			lastzonelines = newlines
			calcScore(engine, ScoreEvent(null, newlines))
//			engine.owner.receiver.calcScore(engine, ScoreEvent(null, newlines))
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
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Zone timer bonus
		val li = ev.lines
		if(!inzone&&li>0) {
			lastzonegain = li*li*6
			zoneframes = minOf(zoneframes+lastzonegain, maxzonetime) //20s cap
			zonegaintimer = 0
		}

		// Line clear bonus
		var pts = 0
		if(ev.twist) {
			when {
				li==0&&!ev.twistEZ -> pts += if(ev.twistMini) 10 else 40
				ev.twistEZ&&(li>0) -> pts += if(ev.b2b>0) 18 else 12
				li==1 -> pts += if(ev.twistMini) (if(ev.b2b>0) 30 else 20) else (if(ev.b2b>0) 120 else 80)
				li==2 -> pts += if(ev.twistMini&&engine.useAllSpinBonus) (if(ev.b2b>0) 60 else 40) else (if(ev.b2b>0) 180 else 120)
				li>=3 -> pts += if(ev.b2b>0) 240 else 160
			}
		} else {
			when {
				li==1 -> pts += 10
				li==2 -> pts += 30
				li==3 -> pts += 50
				li>=4 -> pts += if(ev.b2b>0) 120 else 80
			}
		}
		lastb2b = ev.b2b>0

		// Combo
		if(ev.combo>0&&li>=1) {
			pts += ((ev.combo)*5)
			lastcombo = ev.combo
		}

		// All clear
		if((li>=1)&&(engine.field.isEmpty)) {
			engine.playSE("bravo")
			pts += 180
		}

		pts *= (engine.statistics.level+10)
		// Add to score
		if(pts>0) {
			lastScore = pts
			if(li>=1) engine.statistics.scoreLine += pts else engine.statistics.scoreBonus += pts
		}

		// BGM fade-out effects and BGM changes
		if(tableBGMChange[bgmLv]!=-1) {
			if(engine.statistics.lines>=tableBGMChange[bgmLv]-5) owner.musMan.fadeSW = true
			if(engine.statistics.lines>=tableBGMChange[bgmLv]&&
				(engine.statistics.lines<tableGameClearLines[goalType]||tableGameClearLines[goalType]<0)
			) {
				bgmLv++
				owner.musMan.bgm = BGMStatus.BGM.Generic(bgmLv)
				owner.musMan.fadeSW = false
			}
		}
		if(engine.statistics.lines>=tableGameClearLines[goalType]&&tableGameClearLines[goalType]>=0) {
			// Ending
			engine.ending = 1
			engine.gameEnded()
		} else if(engine.statistics.lines>=(engine.statistics.level+1)*24&&engine.statistics.level<19) {
			// Level up
			engine.statistics.level++
			owner.bgMan.nextBg = engine.statistics.level
			if(!inzone) setSpeed(engine)
			engine.playSE("levelup")
		}

		return pts
	}

	override fun onGameOver(engine:GameEngine):Boolean {
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
	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreSD += fall
	}
	/*
	 * Hard drop
	 */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreHD += fall*2
	}
	/*
	 * Render results screen
	 */
	override fun renderResult(engine:GameEngine) {
		drawResultStats(
			engine, receiver, 0, EventReceiver.COLOR.BLUE, Statistic.SCORE,
			Statistic.LINES, Statistic.LEVEL, Statistic.TIME, Statistic.SPL, Statistic.LPM
		)
		drawResultRank(engine, receiver, 12, EventReceiver.COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, receiver, 14, EventReceiver.COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, receiver, 16, EventReceiver.COLOR.BLUE, netRankingRank[1])
		if(netIsPB) {
			receiver.drawMenuFont(engine, 2, 21, "NEW PB", EventReceiver.COLOR.ORANGE)
		}
		if(netIsNetPlay&&(netReplaySendStatus==1)) {
			receiver.drawMenuFont(engine, 0, 22, "SENDING...", EventReceiver.COLOR.PINK)
		} else if(netIsNetPlay&&!netIsWatch&&(netReplaySendStatus==2)) {
			receiver.drawMenuFont(engine, 1, 22, "A: RETRY", EventReceiver.COLOR.RED)
		}
	}
	/*
	 * Called when saving replay
	 */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(prop, engine)

		// NET: Save name
		if(!netPlayerName.isNullOrEmpty()) prop.setProperty("${engine.playerID}.net.netPlayerName", netPlayerName)

		// Update rankings
		return (!owner.replayMode&&engine.ai==null&&
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.time, goalType)!=-1)
	}
	/** Load settings from [prop] */
	override fun loadSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
		startLevel = prop.getProperty("marathonzone.startLevel", 0)
		goalType = prop.getProperty("marathonzone.gametype", 0)
		version = prop.getProperty("marathonzone.version", 0)
	}
	/** Save settings to [prop] */
	override fun saveSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("marathonzone.startLevel", startLevel)
		prop.setProperty("marathonzone.gametype", goalType)
		prop.setProperty("marathonzone.version", version)
	}

	/**
	 * Update rankings
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 */
	private fun updateRanking(sc:Long, li:Int, time:Int, type:Int):Int {
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
		return rankingRank
	}
	/**
	 * Calculate ranking position
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Long, li:Int, time:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(sc>rankingScore[type][i]) return i
			else if((sc==rankingScore[type][i])&&(li>rankingLines[type][i])) return i
			else if((sc==rankingScore[type][i])&&(li==rankingLines[type][i])&&(time<rankingTime[type][i])) return i
		return -1
	}
	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(engine.owner.bgMan.fadeSW) engine.owner.bgMan.nextBg else engine.owner.bgMan.bg
		val msg = "game\tstats\t"+
			"${engine.statistics.scoreLine}\t${engine.statistics.scoreSD}\t${engine.statistics.scoreHD}\t${engine.statistics.scoreBonus}\t"+
			"${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t${engine.statistics.time}\t${engine.statistics.level}\t"+
			"$goalType\t${engine.gameActive}\t${engine.timerActive}\t"+
			"$lastScore\t$scDisp\t$lastb2b\t$lastcombo\t"+
			"$bg\n"
		netLobby?.netPlayerClient?.send(msg)
	}
	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:List<String>) {
		engine.statistics.scoreLine = message[4].toInt()
		engine.statistics.scoreSD = message[5].toInt()
		engine.statistics.scoreHD = message[6].toInt()
		engine.statistics.scoreBonus = message[7].toInt()
		engine.statistics.lines = message[8].toInt()
		engine.statistics.totalPieceLocked = message[9].toInt()
		engine.statistics.time = message[10].toInt()
		engine.statistics.level = message[11].toInt()
		goalType = message[12].toInt()
		engine.gameActive = message[13].toBoolean()
		engine.timerActive = message[14].toBoolean()
		lastScore = message[15].toInt()
//		scDisp = message[16].toInt()
		lastb2b = message[17].toBoolean()
		lastcombo = message[18].toInt()
		engine.owner.bgMan.bg = message[19].toInt()

		// Meter
		engine.meterValue = (engine.statistics.lines%10)/9f
		engine.meterColor = GameEngine.METER_COLOR_LEVEL
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
		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby?.netPlayerClient?.send(msg)
	}
	/**
	 * NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t$startLevel\t$goalType\n"
		netLobby?.netPlayerClient?.send(msg)
	}
	/**
	 * NET: Receive game options
	 */
	override fun netRecvOptions(engine:GameEngine, message:List<String>) {
		startLevel = message[4].toInt()
		goalType = message[5].toInt()
	}
	/**
	 * NET: Get goal type
	 */
	override fun netGetGoalType():Int = goalType
	/**
	 * NET: It returns true when the current settings don't prevent leaderboard screen from showing.
	 */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = ((startLevel==0)&&(engine.ai==null))

	companion object {
		/** Current version  */
		private const val CURRENT_VERSION = 2
		/** Fall velocity table (numerators)  */
		private val tableGravity = intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 465, 731, 1280, 1707, -1, -1, -1)
		/** Fall velocity table (denominators)  */
		private val tableDenominator = intArrayOf(63, 50, 39, 30, 22, 16, 12, 8, 6, 4, 3, 2, 1, 256, 256, 256, 256, 256, 256, 256)
		/** Line counts when BGM changes occur  */
		private val tableBGMChange = intArrayOf(5*24, 10*24, 15*24, 20*24, -1)
		/** Line counts when game ending occurs  */
		private val tableGameClearLines = intArrayOf(15*24, 20*24, -1)
		private const val maxzonetime = 1200 // 20 seconds
		/** Number of entries in rankings  */
		private const val RANKING_MAX = 13
		/** Number of game types  */
		private const val GAMETYPE_MAX = 3
		/** Number of ranking types  */
		private const val RANKING_TYPE = GAMETYPE_MAX
	}
}
