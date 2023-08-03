/*
 Copyright (c) 2023,
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

package zeroxfc.nullpo.custom.modes

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetUtil.urlEncode
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.NetDummyMode
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.IntegerMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.LevelMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.game.subsystem.mode.menu.StringsMenuItem
import mu.nu.nullpo.game.subsystem.mode.rankMapType
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/**
 * MARATHON Mode
 */
open class MarathonModeBase:NetDummyMode() {
	/** Current BGM*/
	@JvmField var bgmLv = 0

	val itemLevel = LevelMenuItem("startlevel", "LEVEL", COLOR.BLUE, 0, 0..19)
	/** Level at start time */
	var startLevel:Int by DelegateMenuItem(itemLevel)

	open val itemMode:IntegerMenuItem = StringsMenuItem("goalType", "GOAL", COLOR.BLUE, 0,
		List(GAMETYPE_MAX) {if(tableGameClearLines[it]<0) "ENDLESS" else "${tableGameClearLines[it]} LINES"})
	/** Game type */
	var goalType:Int by DelegateMenuItem(itemMode)

	val itemBig = BooleanMenuItem("big", "BIG MODE", COLOR.WHITE, false, true)
	/** Big*/
	var big:Boolean by DelegateMenuItem(itemBig)
	/** Version*/
	@JvmField var version = 0
	/** Current round's ranking position */
	@JvmField var rankingRank = 0
	/** Rankings' scores */
	private val rankingScore = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0L}}
	/** Rankings' line counts*/
	private val rankingLines = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}
	/** Rankings' times */
	private val rankingTime = List(RANKING_TYPE) {MutableList(RANKING_MAX) {-1}}
	override val rankMap
		get() = rankMapOf(rankingScore.mapIndexed {a, x -> "$a.score" to x}+
			rankingLines.mapIndexed {a, x -> "$a.lines" to x}+
			rankingTime.mapIndexed {a, x -> "$a.time" to x})
	override val rankPersMap:rankMapType get() = emptyMap()

	override val name:String
		get() = "marathonBase"
	override val menu:MenuList by lazy {MenuList(id, itemMode, itemLevel, itemBig)}

	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastScore = 0
		bgmLv = 0
		rankingRank = -1
		rankingScore.forEach {it.fill(0)}
		rankingLines.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(-1)}
		netPlayerInit(engine)
		/*if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.modeConfig, engine.ruleOpt.strRuleName)
			version = CURRENT_VERSION
		} else {
			loadSetting(owner.replayProp)
			if(version==0&&owner.replayProp.getProperty("marathon.endless", false)) goalType = 2

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}
		engine.owner.backgroundStatus.bg = startLevel
		engine.framecolor = GameEngine.FRAME_COLOR_GREEN*/
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
		engine.speed.are = 10
		engine.speed.areLine = 6
		engine.speed.lineDelay = 10
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
			val change = updateCursor(engine, 7)
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
						engine.owner.bgMan.bg = startLevel
					}
					6 -> {
						goalType += change
						if(goalType<0) goalType = GAMETYPE_MAX-1
						if(goalType>GAMETYPE_MAX-1) goalType = 0
						if(startLevel>(tableGameClearLines[goalType]-1)/10&&tableGameClearLines[goalType]>=0) {
							startLevel = (tableGameClearLines[goalType]-1)/10
							engine.owner.bgMan.bg = startLevel
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

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")
				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) {
				engine.quitFlag = true
			}

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&startLevel==0&&!big&&engine.ai==null) {
				netEnterNetPlayRankingScreen(goalType)
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
     * Called for initialization during "Ready" screen
     */
	override fun startGame(engine:GameEngine) {
		engine.statistics.level = startLevel
		engine.statistics.levelDispAdd = 1
		engine.b2bEnable = true
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.big = big
		engine.twistAllowKick = true
		engine.twistEnable = true
		engine.useAllSpinBonus = true
		engine.twistEnableEZ = true
		setSpeed(engine)
		if(netIsWatch) {
			owner.musMan.bgm = BGMStatus.BGM.Silent
		}
	}
	/*
     * Render score
     */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.GREEN)

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
     * Calculate score
     */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int = 0
	/*
     * Soft drop
     */
	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreSD += fall
		scDisp += fall
	}
	/*
     * Hard drop
     */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreHD += fall*2
		scDisp += fall*2
	}
	/*
     * Render results screen
     */
	override fun renderResult(engine:GameEngine) {
		drawResultStats(
			engine, receiver, 0, COLOR.BLUE, Statistic.SCORE,
			Statistic.LINES, Statistic.LEVEL, Statistic.TIME, Statistic.SPL, Statistic.LPM
		)
		drawResultRank(engine, receiver, 12, COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, receiver, 14, COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, receiver, 16, COLOR.BLUE, netRankingRank[1])
		if(netIsPB) receiver.drawMenuFont(engine, 2, 21, "NEW PB", COLOR.RAINBOW)
		if(netIsNetPlay&&netReplaySendStatus==1) receiver.drawMenuFont(engine, 0, 22, "SENDING...", COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, 1, 22, "A: RETRY", COLOR.RED)
	}
	/*
     * Called when saving replay
     */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) {
			prop.setProperty("${engine.playerID}.net.netPlayerName", netPlayerName)
		}

		// Update rankings
		return !owner.replayMode&&!big&&engine.ai==null&&
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.time, goalType)!=-1
	}
	/**
	 * Update rankings
	 *
	 * @param sc   Score
	 * @param li   Lines
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
	 *
	 * @param sc   Score
	 * @param li   Lines
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Long, li:Int, time:Int, type:Int):Int {
		for(i in 0..<RANKING_MAX) {
			if(sc>rankingScore[type][i]) return i
			else if(sc==rankingScore[type][i]&&li>rankingLines[type][i]) return i
			else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&time<rankingTime[type][i]) return i
		}
		return -1
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(engine.owner.bgMan.fadeSW) engine.owner.bgMan.nextBg else engine.owner.bgMan.bg
		val msg = "game\tstats\t"+engine.run {
			statistics.run {
				"${scoreLine}\t${scoreSD}\t${scoreHD}\t${scoreBonus}\t"+
					"${lines}\t${totalPieceLocked}\t"+
					"${time}\t${level}\t"
			}+"$goalType\t${engine.gameActive}\t${timerActive}\t$lastScore\t$scDisp\t${lastEvent}\t$bg\n"
		}
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:List<String>) {
		listOf<(String)->Unit>({}, {}, {}, {},
			{engine.statistics.scoreLine = it.toInt()},
			{engine.statistics.scoreSD = it.toInt()},
			{engine.statistics.scoreHD = it.toInt()},
			{engine.statistics.scoreBonus = it.toInt()},
			{engine.statistics.lines = it.toInt()},
			{engine.statistics.totalPieceLocked = it.toInt()},
			{engine.statistics.time = it.toInt()},
			{engine.statistics.level = it.toInt()},
			{goalType = it.toInt()},
			{engine.gameActive = it.toBoolean()},
			{engine.timerActive = it.toBoolean()},
			{lastScore = it.toInt()},
			{/*scDisp = it.toInt()*/},
			{engine.lastEvent = ScoreEvent.parseStr(it)},
			{engine.owner.bgMan.bg = it.toInt()}).zip(message).forEach {(x, y) ->
			x(y)
		}

		// Meter
		engine.meterValue = engine.statistics.lines%10/9f
		engine.meterColor = GameEngine.METER_COLOR_LEVEL
	}
	/**
	 * NET: Send end-of-game stats
	 *
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		val subMsg = engine.statistics.run {
			"SCORE;${score}\tLINE;${lines}\tLEVEL;${level+levelDispAdd}\tTIME;${time.toTimeStr}\tSCORE/LINE;${spl}\tLINE/MIN;${lpm}\t"
		}
		val msg = "gstat1p\t${urlEncode(subMsg)}\n"
		netLobby?.netPlayerClient?.send(msg)
	}
	/**
	 * NET: Send game options to all spectators
	 *
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
		big = java.lang.Boolean.parseBoolean(message[6])
	}
	/**
	 * NET: Get goal type
	 */
	override val netGetGoalType get() = goalType
	/**
	 * NET: It returns true when the current settings don't prevent leaderboard screen from showing.
	 */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = startLevel==0&&!big&&engine.ai==null

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
		const val RANKING_MAX = 13
		/**
		 * Number of ranking types
		 */
		const val RANKING_TYPE = 3
		/**
		 * Number of game types
		 */
		const val GAMETYPE_MAX = 3
	}
}
