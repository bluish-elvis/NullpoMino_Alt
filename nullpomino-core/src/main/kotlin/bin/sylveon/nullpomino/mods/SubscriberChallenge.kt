/*
 Copyright (c) 2021-2024,
 This library class was created by 0xFC963F18DC21 / Shots243
 It is part of an extension library for the game NullpoMino (copyright 2010-2024)

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

package bin.sylveon.nullpomino.mods

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameEngine.Status
import mu.nu.nullpo.game.subsystem.mode.NetDummyMode
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.LevelMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.game.subsystem.mode.menu.StringsMenuItem
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import zeroxfc.nullpo.custom.libs.Interpolation
import zeroxfc.nullpo.custom.libs.backgroundtypes.AnimatedBackgroundHook
import kotlin.random.Random

/**
 * SUBSCRIBER CHALLENGE Mode
 * Author ry00001
 *
 * Someone take IDEA away from me.
 */
class SubscriberChallenge:NetDummyMode() {
	/** Most recent scoring event type  */
	private var lastevent:Int = 0
	/** True if most recent scoring event is a B2B  */
	private var lastb2b:Boolean = false
	/** Combo count for most recent scoring event  */
	private var lastcombo:Int = 0
	/** Piece ID for most recent scoring event  */
	private var lastpiece:Int = 0
	/** Current BGM  */
	private var bgmLv:Int = 0
	private val itemLv = LevelMenuItem("startLevel", "Level", EventReceiver.COLOR.BLUE, 0, 0..19)
	/** Level at start time  */
	private var startLevel:Int by DelegateMenuItem(itemLv)

	private val itemMode = StringsMenuItem("goalType", "GOAL", EventReceiver.COLOR.BLUE, 0,
		List(GAMETYPE_MAX) {if(tableGameClearLines[it]<=0) "ENDLESS" else "${tableGameClearLines[it]} LINES"})
	/** Game type  */
	private var goalType:Int by DelegateMenuItem(itemMode)

	private val itemBig = BooleanMenuItem("big", "BIG", EventReceiver.COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	/** BRO! YOU JUST POSTED T-SPIN! YOU ARE GOING TO GAIN SUBSCRIBER!*/
	private var subscriber:Int = 0
	private var subscriberRNG:Random = Random.Default

	private var lastValue:Int = 0
	/** Version  */
	private var version:Int = 0
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
	/*
	 * Mode name
	 */
	override val name:String = "SUBSCRIBER CHALLENGE"

	// Initialization
	override val menu = MenuList("subscriberchallenge", itemMode, itemLv, itemBig)
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastScore = 0
		lastb2b = false
		lastcombo = 0
		lastpiece = 0
		bgmLv = 0
		lastValue = 0
		subscriber = 0
		lastValue = 0
		rankingRank = -1
		rankingScore.forEach {it.fill(0)}
		rankingLines.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(-1)}
		netPlayerInit(engine)
		if(!owner.replayMode) {
			version = CURRENT_VERSION
		} else {
			if((version==0)&&(owner.replayProp.getProperty("subscriberchallenge.endless", false))) goalType = 2

			// NET: Load name
			netPlayerName = owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}
		owner.bgMan.bg = startLevel
		engine.frameColor = GameEngine.FRAME_COLOR_GREEN
	}
	/**
	 * Set the gravity rate
	 * @param engine GameEngine
	 */
	fun setSpeed(engine:GameEngine) {
		var lv:Int = engine.statistics.level
		if(lv<0) lv = 0
		if(lv>=tableGravity.size) lv = tableGravity.size-1
		engine.speed.gravity = tableGravity[lv]
		engine.speed.denominator = tableDenominator[lv]
	}
	/*
	 * Called at settings screen
	 */
	override fun onSetting(engine:GameEngine):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode) {
			netOnUpdateNetPlayRanking(engine, goalType)
		} else if(!owner.replayMode) {
			// Configuration changes
			val change:Int = updateCursor(engine, 8)
			if(change!=0) {
				engine.playSE("change")
				when(engine.statc[2]) {
					0 -> {
						startLevel += change
						if(tableGameClearLines[goalType]>=0) {
							if(startLevel<0) startLevel = (tableGameClearLines[goalType]-1)/10
							if(startLevel>(tableGameClearLines[goalType]-1)/10) startLevel = 0
						} else {
							if(startLevel<0) startLevel = 19
							if(startLevel>19) startLevel = 0
						}
						owner.bgMan.bg = startLevel
					}
					1 -> {
						goalType += change
						if(goalType<0) goalType = GAMETYPE_MAX-1
						if(goalType>GAMETYPE_MAX-1) goalType = 0
						if((startLevel>(tableGameClearLines[goalType]-1)/10)&&(tableGameClearLines[goalType]>=0)) {
							startLevel = (tableGameClearLines[goalType]-1)/10
							owner.bgMan.bg = startLevel
						}
					}
					2 -> big = !big
				}

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
			if(engine.ctrl.isPush(mu.nu.nullpo.game.component.Controller.BUTTON_D)&&netIsNetPlay&&(startLevel==0)&&!big&&(
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
				engine, receiver, 0, EventReceiver.COLOR.BLUE, 0,
			)
			drawMenuCompact(engine, receiver, "Level" to startLevel+1)
			drawMenuSpeeds(engine, receiver, 4, EventReceiver.COLOR.WHITE, 10)
			drawMenuCompact(engine, receiver, 9, EventReceiver.COLOR.BLUE, 2, "BIG" to big)
		}
	}
	/*
	 * Called for initialization during "Ready" screen
	 */
	override fun startGame(engine:GameEngine) {
		engine.statistics.level = startLevel
		lastValue = 0
		engine.statistics.levelDispAdd = 1
		engine.b2bEnable = true
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.big = big
		engine.twistEnable = true
		engine.useAllSpinBonus = true
		engine.twistEnableEZ = true
		setSpeed(engine)
		owner.musMan.bgm = if(netIsWatch) BGMStatus.BGM.Silent else BGMStatus.BGM.Generic(bgmLv)
	}
	/*
	 * Render score
	 */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return
		receiver.drawScoreFont(engine, 0, 0, name, EventReceiver.COLOR.GREEN)
		receiver.drawScoreFont(
			engine, 0, 1, if(tableGameClearLines[goalType]==-1) "(Endless Run)" else "(${tableGameClearLines[goalType]} Lines run)",
			EventReceiver.COLOR.GREEN
		)
		if((engine.stat==Status.SETTING)||((engine.stat==Status.RESULT)&&!owner.replayMode)) {
			if(!owner.replayMode&&!big&&(engine.ai==null)) {
				val scale:Float = if((receiver.nextDisplayType==2)) 0.5f else 1.0f
				val topY:Int = if((receiver.nextDisplayType==2)) 6 else 4
				receiver.drawScoreFont(engine, 3, topY-1, "SCORE  LINE TIME", EventReceiver.COLOR.BLUE, scale)
				for(i in 0..<RANKING_MAX) {
					receiver.drawScoreGrade(engine, 0, topY+i, "%2d".format(i+1), EventReceiver.COLOR.YELLOW, scale)
					receiver.drawScoreFont(engine, 3, topY+i, "${rankingScore[goalType][i]}", (i==rankingRank), scale)
					receiver.drawScoreFont(engine, 10, topY+i, "${rankingLines[goalType][i]}", (i==rankingRank), scale)
					receiver.drawScoreFont(engine, 15, topY+i, rankingTime[goalType][i].toTimeStr, i==rankingRank, scale)
				}
			}
		} else {
			receiver.drawScoreFont(engine, 0, 3, "SUBSCRIBER", EventReceiver.COLOR.BLUE)
			val a = "${
				if(subscriber!=lastValue) Interpolation.lerp(lastValue, subscriber, scDisp/120.0) else subscriber
			}${if(subscriber-lastValue!=0) "(${if(subscriber-lastValue>0) "+" else ""}${subscriber-lastValue})" else ""}"

			receiver.drawScoreFont(engine, 0, 4, a, ((subscriber-lastValue)>0))

			receiver.drawScoreFont(engine, 0, 6, "LINE", EventReceiver.COLOR.BLUE)
			if((engine.statistics.level>=19)&&(tableGameClearLines[goalType]<0)) receiver.drawScoreFont(
				engine, 0, 7, "${engine.statistics.lines}"
			) else receiver.drawScoreFont(
				engine, 0, 7, "${engine.statistics.lines}/${(engine.statistics.level+1)*10}"
			)
			receiver.drawScoreFont(engine, 0, 9, "LEVEL", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 10, "${engine.statistics.level+1}")
			receiver.drawScoreFont(engine, 0, 12, "TIME", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 13, engine.statistics.time.toTimeStr)
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
	/*
	 * Called after every frame
	 */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		if(engine.gameStarted&&(engine.stat==Status.ARE||engine.stat==Status.LINECLEAR))
			for(i in 0..<engine.field.width) for(j in engine.field.hiddenHeight*-1..<engine.field.height)
				engine.field.getBlock(i, j)?.let {
					it.skin = (++it.skin)%skinCount
				}
	}

	val skinCount:Int
		get() {
			return when(AnimatedBackgroundHook.resourceHook) {
				AnimatedBackgroundHook.HOLDER_SLICK -> mu.nu.nullpo.gui.slick.ResourceHolder.imgNormalBlockList.size
				//AnimatedBackgroundHook.HOLDER_SWING -> return ResourceHolderSwing.imgNormalBlockList.size()
				//AnimatedBackgroundHook.HOLDER_SDL -> return ResourceHolderSDL.imgNormalBlockList.size()
				else -> 0
			}
		}
	/*
	 * Calculate score
	 */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Line clear bonus
		var sub = 0
		val li = ev.lines
		if(li>0) lastValue = subscriber
		val pts = calcScoreBase(engine, ev)
		val cmb = if(ev.combo>0&&li>=1) engine.combo else 0
		// Combo
		val spd = maxOf(0, engine.lockDelay-engine.lockDelayNow)+engine.manualLock.toInt()
		// Add to score
		if(pts+cmb+spd>0) {
			val get = calcScoreCombo(pts, cmb, engine.statistics.level, spd)

			if(pts>0) lastScore = get
			if(li>=1) engine.statistics.scoreLine += get
			else engine.statistics.scoreBonus += get
		}
		// T-Spin 0 lines
		sub += if(engine.twist) when {
			(li==0)&&(!engine.twistEZ) -> subscriberRNG.nextInt(100)
			engine.twistEZ&&(li>0) -> subscriberRNG.nextInt(100*li)+100*li
			li==1 -> (if(engine.twistMini) subscriberRNG.nextInt(200)+3000 else subscriberRNG.nextInt(700)+6000)
			li==2 -> subscriberRNG.nextInt(2300)+6000
			li>=3 -> subscriberRNG.nextInt(5000)+9000
			else -> 0
		} else when {
			li==1 -> subscriberRNG.nextInt(600)-300
			li==2 -> subscriberRNG.nextInt(600)-150
			li==3 -> subscriberRNG.nextInt(600)
			li>=4 -> subscriberRNG.nextInt(2300)+6000
			else -> 0
		}
		lastb2b = engine.b2b

		// Combo
		if(ev.combo>0&&ev.lines>=1) {
			sub += subscriberRNG.nextInt(1000*engine.combo)+100*engine.combo-100
			lastcombo = engine.combo
		}

		// All clear
		if(li>=1&&engine.field.isEmpty) {
			engine.playSE("bravo")
			sub += subscriberRNG.nextInt(500)+10000
		}

		// Add to score
		if(pts>0) {
			lastScore = pts
			lastpiece = engine.nowPieceObject?.id ?: 0
			if(li>=1) engine.statistics.scoreLine += pts else engine.statistics.scoreBonus += pts
		}
		subscriber += sub
		// BGM fade-out effects and BGM changes
		if(tableBGMChange[bgmLv]!=-1) {
			if(engine.statistics.lines>=tableBGMChange[bgmLv]-5) owner.musMan.fadeSW = true
			if((engine.statistics.lines>=tableBGMChange[bgmLv])&&
				((engine.statistics.lines<tableGameClearLines[goalType])||(tableGameClearLines[goalType]<0))
			) {
				bgmLv++
				owner.musMan.bgm = BGMStatus.BGM.Generic(bgmLv)
				owner.musMan.fadeSW = false
			}
		}

		// Meter
		engine.meterValue = (engine.statistics.lines%10)/9f
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(engine.statistics.lines%10>=4) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(engine.statistics.lines%10>=6) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(engine.statistics.lines%10>=8) engine.meterColor = GameEngine.METER_COLOR_RED
		if((engine.statistics.lines>=tableGameClearLines[goalType])&&(tableGameClearLines[goalType]>=0)) {
			// Ending
			engine.ending = 1
			engine.gameEnded()
		} else if((engine.statistics.lines>=(engine.statistics.level+1)*10)&&(engine.statistics.level<19)) {
			// Level up
			engine.statistics.level++
			owner.bgMan.nextBg = engine.statistics.level
			setSpeed(engine)
			engine.playSE("levelup")
		}
		return sub
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

	override fun onGameOver(engine:GameEngine):Boolean {
		subscriber -= subscriberRNG.nextInt(maxOf(1, tableGameClearLines[goalType]-engine.statistics.lines))
		return super.onGameOver(engine)
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
		// NET: Save name
		if(!netPlayerName.isNullOrEmpty()) prop.setProperty("${engine.playerID}.net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			if(updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.time, goalType)!=-1) return true
		}
		return false
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
		for(i in 0..<RANKING_MAX) {
			if(sc>rankingScore[type][i]) return i
			else if((sc==rankingScore[type][i])&&(li>rankingLines[type][i])) return i
			else if((sc==rankingScore[type][i])&&(li==rankingLines[type][i])&&(time<rankingTime[type][i])) return i
		}
		return -1
	}
	/**
	 * NET: Send various in-game stats (as well as goalType)
	 * @param engine GameEngine
	 */
	override fun netSendStats(engine:GameEngine) {
		val bg:Int =
			if(owner.bgMan.fadeSW) engine.owner.bgMan.nextBg else engine.owner.bgMan.bg
		val msg = "game\tstats\t"+
			"${engine.statistics.scoreLine}\t${engine.statistics.scoreSD}\t${engine.statistics.scoreHD}\t${engine.statistics.scoreBonus}\t"+
			"${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t"+
			"${engine.statistics.time}\t${engine.statistics.level}\t${engine.statistics.lpm}\t${engine.statistics.spl}\t"+
			"$goalType\t${engine.gameActive}\t${engine.timerActive}\t$lastScore\t$scDisp\t"+
			"$subscriber\t$lastb2b\t$lastcombo\t$lastpiece\t$bg\n"
		netLobby?.netPlayerClient?.send(msg)
	}
	/**
	 * NET: Receive various in-game stats (as well as goalType)
	 */
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
		subscriber = message[15].toInt()
//		scDisp = message[16].toInt()
		lastevent = message[17].toInt()
		lastb2b = message[18].toBoolean()
		lastcombo = message[19].toInt()
		lastpiece = message[20].toInt()
		engine.owner.bgMan.bg = message[21].toInt()

		// Meter
		engine.meterValue = (engine.statistics.lines%10)/9f
		engine.meterColor = GameEngine.METER_COLOR_LEVEL
	}
	/**
	 * NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		val subMsg:String = "SCORE;${engine.statistics.score}\tLINE;${engine.statistics.lines}\t"+
			"LEVEL;${engine.statistics.level+engine.statistics.levelDispAdd}\t"+
			"TIME;${engine.statistics.time.toTimeStr}\t"+
			"SCORE/LINE;${engine.statistics.spl}\tLINE/MIN;${engine.statistics.lpm}"
		netLobby?.netPlayerClient?.send("gstat1p\t${NetUtil.urlEncode(subMsg)}\n")
	}
	/**
	 * NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t$startLevel\t$goalType\t$big\n"
		netLobby?.netPlayerClient?.send(msg)
	}
	/**
	 * NET: Receive game options
	 */
	override fun netRecvOptions(engine:GameEngine, message:List<String>) {
		startLevel = message[4].toInt()
		goalType = message[5].toInt()
		big = message[6].toBoolean()
	}
	/**
	 * NET: Get goal type
	 */
	override val netGetGoalType get() = goalType
	/**
	 * NET: It returns true when the current settings don't prevent leaderboard screen from showing.
	 */
	override fun netIsNetRankingViewOK(engine:GameEngine) = startLevel==0&&!big&&engine.ai==null

	companion object {
		/** Current version  */
		const val CURRENT_VERSION = 2
		/** Fall velocity table (numerators)  */
		val tableGravity = listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 465, 731, 1280, 1707, -1, -1, -1)
		/** Fall velocity table (denominators)  */
		val tableDenominator = listOf(63, 50, 39, 30, 22, 16, 12, 8, 6, 4, 3, 2, 1, 256, 256, 256, 256, 256, 256, 256)
		/** Line counts when BGM changes occur  */
		val tableBGMChange = listOf(50, 100, 150, 200, -1)
		/** Line counts when game ending occurs  */
		val tableGameClearLines = listOf(150, 200, -1)
		/** Number of entries in rankings  */
		const val RANKING_MAX = 10
		/** Number of ranking types  */
		const val RANKING_TYPE = 3
		/** Number of game types  */
		const val GAMETYPE_MAX = 3
	}
}
