/*
 Copyright (c) 2010-2024, NullNoname
 All rights reserved.

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis
 THIS IS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

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
package mu.nu.nullpo.game.subsystem.mode

import kotlinx.serialization.Serializable
import mu.nu.nullpo.game.component.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Block.ATTRIBUTE
import mu.nu.nullpo.game.component.Block.TYPE
import mu.nu.nullpo.game.component.Statistics
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.Leaderboard
import mu.nu.nullpo.game.event.Rankable
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.*
import mu.nu.nullpo.gui.common.BaseFont.FONT.*
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import org.apache.logging.log4j.LogManager
import mu.nu.nullpo.game.component.Block.COLOR as bCOLOR

/** DIG RACE Mode */
class SprintDig:NetDummyMode() {
	private val itemSpd = object:SpeedPresets(COLOR.BLUE, 0) {
		override fun presetLoad(engine:GameEngine, prop:CustomProperties, ruleName:String, setId:Int) {
			super.presetLoad(engine, prop, ruleName, setId)
			bgmId = prop.getProperty("$ruleName.bgmno.$setId", BGM.values.indexOf(BGM.Rush(3)))
			big = prop.getProperty("$ruleName.big.$setId", false)
			goalType = prop.getProperty("$ruleName.goalType.$setId", 1)
		}

		override fun presetSave(engine:GameEngine, prop:CustomProperties, ruleName:String, setId:Int) {
			super.presetSave(engine, prop, ruleName, setId)
			prop.setProperty("$ruleName.bgmno.$setId", bgmId)
			prop.setProperty("$ruleName.big.$setId", big)
			prop.setProperty("$ruleName.goalType.$setId", goalType)
		}
	}
	/** Last preset number used */
	private var presetNumber:Int by DelegateMenuItem(itemSpd)

	private val itemBGM = BGMMenuItem("bgmno", COLOR.BLUE, BGM.values.indexOf(BGM.Rush(3)))
	/** BGM number */
	private var bgmId:Int by DelegateMenuItem(itemBGM)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	/** Goal type (0=5 garbages, 1=10 garbages, 2=18 garbages) */
	private val itemGoal = StringsMenuItem(
		"goalType", "GOAL", COLOR.BLUE, 0, GOAL_TABLE.map {"$it G.LINES"}
	)

	private var goalType:Int by DelegateMenuItem(itemGoal)

	override val menu = MenuList("digrace", itemGoal, itemSpd, itemBGM, itemBig)

	/** Current version */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	@Serializable
	data class TimeRow(override val st:Statistics = Statistics()):Rankable {
		override fun compareTo(other:Rankable):Int =
			if(other is TimeRow)
				compareValuesBy(this, other, {it.clear}, {-it.ti}, {-it.li}, {-it.st.totalPieceLocked})
			else super.compareTo(other)

	}

	override val ranking = List(GOALTYPE_MAX) {
		Leaderboard(rankingMax, kotlinx.serialization.serializer<List<TimeRow>>()){TimeRow()}}
	/* Mode name */
	override val name = "Digging Sprint"
	override val gameIntensity = 2
	/* Initialization for each player */
	override fun playerInit(engine:GameEngine) {

		bgmId = 0
		big = false
		goalType = 0
		presetNumber = 0
		super.playerInit(engine)

		rankingRank = -1

		engine.frameSkin = GameEngine.FRAME_COLOR_GREEN

		netPlayerInit(engine)

		if(!owner.replayMode) {
			version = CURRENT_VERSION
		} else {
			version = owner.replayProp.getProperty("digrace.version", 0)

			// NET: Load name
			netPlayerName = owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}
	}

	/* Render settings screen */
	override fun renderSetting(engine:GameEngine) {
		if(netIsNetRankingDisplayMode)
		// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, receiver)
		else {
			drawMenuSpeeds(engine, receiver, 0, COLOR.BLUE, 0)
			drawMenuBGM(engine, receiver, bgmId)
			drawMenuCompact(engine, receiver, "GOAL" to GOAL_TABLE[goalType])
			if(!owner.replayMode) {
				menuColor = COLOR.GREEN
				drawMenuCompact(engine, receiver, "LOAD" to presetNumber, "SAVE" to presetNumber)
			}
		}
	}

	/* Ready */
	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0)
			if(!netIsNetPlay||!netIsWatch) {
				engine.createFieldIfNeeded()
				fillGarbage(engine, goalType)

				// Update meter
				engine.meterValue = GOAL_TABLE[goalType]*1f
				engine.meterColor = GameEngine.METER_COLOR_GREEN

				// NET: Send field
				if(netNumSpectators>0) netSendField(engine)
			}
		return false
	}

	/* This function will be called before the game actually begins (after
 Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine) {
		if(version<=0) engine.big = big

		owner.musMan.bgm = if(netIsWatch) BGM.Silent else BGM.values[bgmId]
	}

	/** Fill the playfield with garbage
	 * @param engine GameEngine
	 * @param height Garbage height level number
	 */
	private fun fillGarbage(engine:GameEngine, height:Int) {
		val w:Int = engine.field.width
		val h:Int = engine.field.height
		var hole:Int = -1

		for(y:Int in h-1 downTo h-GOAL_TABLE[height]) {
			var newhole:Int
			do newhole = engine.random.nextInt(w)
			while(newhole==hole)
			hole = newhole

			var prevColor:bCOLOR? = null
			for(x:Int in 0..<w)
				if(x!=hole) {
					var color:bCOLOR = bCOLOR.WHITE
					if(y==h-1) {
						do
							color = bCOLOR.all[1+engine.random.nextInt(7)]
						while(color==prevColor)
						prevColor = color
					}
					engine.field.setBlock(
						x, y, Block(
							color, if(y==h-1) TYPE.BLOCK else TYPE.GEM,
							engine.blkSkin, ATTRIBUTE.VISIBLE, ATTRIBUTE.GARBAGE
						)
					)
				}

			// Set connections
			if(receiver.isStickySkin(engine)&&y!=h-1)
				for(x:Int in 0..<w)
					if(x!=hole) {
						val blk = engine.field.getBlock(x, y)
						if(blk!=null) {
							if(!engine.field.getBlockEmpty(x-1, y)) blk.setAttribute(true, ATTRIBUTE.CONNECT_LEFT)
							if(!engine.field.getBlockEmpty(x+1, y))
								blk.setAttribute(true, ATTRIBUTE.CONNECT_RIGHT)
						}
					}
		}
	}

	private fun getRemainGarbageLines(engine:GameEngine?, height:Int):Int {
		if(engine?.field==null) return -1

		val w = engine.field.width
		val h = engine.field.height
		var lines = 0

		for(y in h-1 downTo h-GOAL_TABLE[height])
			if(!engine.field.getLineFlag(y))
				for(x in 0..<w) {
					val blk = engine.field.getBlock(x, y)

					if(blk!=null&&blk.getAttribute(ATTRIBUTE.GARBAGE)) {
						lines++
						break
					}
				}

		return lines
	}

	/* Score display */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return

		receiver.drawScore(engine, 0, 0, name, BASE, COLOR.GREEN)
		receiver.drawScore(
			engine, 0, 1, "("+GOAL_TABLE[goalType]
				+" garbages run)", BASE, COLOR.GREEN
		)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&engine.ai==null&&!netIsWatch) {
				receiver.drawScore(engine, 3, 3, "TIME   LINE Piece", BASE, COLOR.BLUE)

				ranking[goalType].forEachIndexed { i,it->
					receiver.drawScore(
						engine,
						0,
						4+i,
						"%2d".format(i+1),
						GRADE,
						if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW
					)
					receiver.drawScore(engine, 3, 4+i, it.ti.toTimeStr, NUM, rankingRank==i)
					receiver.drawScore(engine, 10, 4+i, "${it.li}", NUM, rankingRank==i)
					receiver.drawScore(engine, 14, 4+i, "${it.st.totalPieceLocked}", NUM, rankingRank==i)
				}
			}
		} else {
			val remainLines = getRemainGarbageLines(engine, goalType)
			val fontColor = when(remainLines) {
				in 1..4 -> COLOR.RED
				in 1..8 -> COLOR.ORANGE
				in 1..14 -> COLOR.YELLOW
				else -> COLOR.WHITE
			}
			"${maxOf(0, remainLines)}".let {
				receiver.drawMenu(engine, -30, 1, it, NUM, fontColor, 4f, .5f)
				receiver.drawScore(engine, 0, 3, it, NUM, fontColor, 2f)
			}
			receiver.drawScore(engine, 4, 3, "LINES\nTO GO", NANO, COLOR.BLUE)

			receiver.drawScore(engine, 0, 5, "${engine.statistics.totalPieceLocked}", NUM, 2f)
			receiver.drawScore(engine, 0, 7, "Piece\n/sec", BASE, COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 8, engine.statistics.pps, scale = 2f)
			receiver.drawScore(engine, 6, 7, "Finesse", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 5, 5, "${engine.statistics.finesse}", NUM, 2f)
			receiver.drawScoreNum(engine, 0, 10, engine.statistics.lpm, scale = 2f)
			receiver.drawScore(engine, 0, 12, "Lines/MIN", BASE, COLOR.BLUE)


			receiver.drawScore(engine, 0, 15, "Time", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 0, 16, engine.statistics.time.toTimeStr, NUM_T)
		}

		super.renderLast(engine)
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Update meter
		val remainLines = getRemainGarbageLines(engine, goalType)
		engine.meterValue = remainLines*1f/engine.fieldHeight
		engine.meterColor = GameEngine.METER_COLOR_LEVEL

		// Game is completed when there is no gem blocks
		if(ev.lines>0&&remainLines==0) {
			engine.ending = 1
			engine.gameEnded()
		}
		return 0
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		drawResultStats(
			engine, receiver, 1, COLOR.BLUE, Statistic.LINES, Statistic.PIECE, Statistic.TIME, Statistic.LPM,
			Statistic.PPS
		)
		drawResultRank(engine, receiver, 11, COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, receiver, 13, COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, receiver, 15, COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenu(engine, 2, 18, "NEW PB", BASE, COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenu(engine, 0, 19, "SENDING...", BASE, COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2)
			receiver.drawMenu(engine, 1, 19, "A: RETRY", BASE, COLOR.RED)
	}

	/* Save replay file */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		owner.replayProp.setProperty("digrace.version", version)
		itemSpd.presetSave(engine, prop, menu.propName, -1)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty(
			"${engine.playerID}.net.netPlayerName",
			netPlayerName
		)

		// Update rankings
		if(!owner.replayMode&&getRemainGarbageLines(engine, goalType)==0&&engine.ending!=0&&engine.ai==null&&!netIsWatch) {
			rankingRank=ranking[goalType].add(TimeRow(engine.statistics))
			if(rankingRank!=-1) return true
		}

		return false
	}


	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val msg = "game\tstats\t"+
			"${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t"+
			"${engine.statistics.time}\t${engine.statistics.lpm}\t"+
			"${engine.statistics.pps}\t$goalType\t"+
			"${engine.gameActive}\t${engine.timerActive}\t"+
			"${engine.meterColor}\t${engine.meterValue}"+
			"\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:List<String>) {
		engine.statistics.lines = message[4].toInt()
		engine.statistics.totalPieceLocked = message[5].toInt()
		engine.statistics.time = message[6].toInt()
		//engine.statistics.lpm = message[7].toFloat()
		//engine.statistics.pps = message[8].toFloat()
		goalType = message[9].toInt()
		engine.gameActive = message[10].toBoolean()
		engine.timerActive = message[11].toBoolean()
		engine.meterColor = message[12].toInt()
		engine.meterValue = message[13].toFloat()
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		val subMsg =
			"GARBAGE;${(GOAL_TABLE[goalType]-getRemainGarbageLines(engine, goalType))}/${GOAL_TABLE[goalType]}\t"+
				"LINE;${engine.statistics.lines}\t"+
				"PIECE;${engine.statistics.totalPieceLocked}\t"+
				"TIME;${engine.statistics.time.toTimeStr}\t"+
				"LINE/MIN;${engine.statistics.lpm}\t"+
				"PIECE/SEC;${engine.statistics.pps}\t"

		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t"+
			"${engine.speed.gravity}\t${engine.speed.denominator}\t${engine.speed.are}\t"+
			"${engine.speed.areLine}\t${engine.speed.lineDelay}\t${engine.speed.lockDelay}\t"+
			"${engine.speed.das}\t$bgmId\t$goalType\t$presetNumber"+
			"\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:List<String>) {
		engine.speed.gravity = message[4].toInt()
		engine.speed.denominator = message[5].toInt()
		engine.speed.are = message[6].toInt()
		engine.speed.areLine = message[7].toInt()
		engine.speed.lineDelay = message[8].toInt()
		engine.speed.lockDelay = message[9].toInt()
		engine.speed.das = message[10].toInt()
		bgmId = message[11].toInt()
		goalType = message[12].toInt()
		presetNumber = message[13].toInt()
	}

	/** NET: Get goal type */
	override val netGetGoalType get() = goalType

	/** NET: It returns true when the current settings don't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = engine.ai==null

	/** NET: It returns true when the current settings don't prevent replay
	 * data from sending. */
	override fun netIsNetRankingSendOK(engine:GameEngine):Boolean =
		netIsNetRankingViewOK(engine)&&getRemainGarbageLines(engine, goalType)==0&&engine.ending!=0

	companion object {
		/* ----- Main variables ----- */
		/** Logger */
		internal var log = LogManager.getLogger()

		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Number of goal type */
		private const val GOALTYPE_MAX = 3

		/** Table of garbage lines */
		private val GOAL_TABLE = listOf(5, 10, 18)
	}
}
