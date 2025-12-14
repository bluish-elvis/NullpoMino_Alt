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

/** COMBO RACE Mode */
class SprintCombo:NetDummyMode() {
	/** EventReceiver object (This receives many game events, can also be used
	 * for drawing the fonts.) */

	/** Elapsed time from last line clear */
	private var scgettime = 0

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	/** Hindrance Lines type (0=5,1=10,2=18) */
	private val itemGoal = StringsMenuItem(
		"goalType", "GOAL", COLOR.BLUE, 0, GOAL_TABLE.map {"$it LINES"}
	)

	/** Time limit type */
	private var goalType:Int by DelegateMenuItem(itemGoal)

	/** Current version */
	private var version = 0

	private val itemSpd = object:SpeedPresets(COLOR.BLUE, 0) {
		override fun presetLoad(engine:GameEngine, prop:CustomProperties, ruleName:String, setId:Int) {
			super.presetLoad(engine, prop, ruleName, setId)
			bgmId = prop.getProperty("$ruleName.bgmno.$setId", BGM.values.indexOf(BGM.Rush(0)))
			big = prop.getProperty("$ruleName.big.$setId", false)
			goalType = prop.getProperty("$ruleName.goalType.$setId", 1)
			shapetype = prop.getProperty("$ruleName.shapetype.$setId", 1)
			comboWidth = prop.getProperty("$ruleName.comboWidth.$setId", 4)
			ceilingAdjust = prop.getProperty("$ruleName.ceilingAdjust.$setId", -2)
			spawnAboveField = prop.getProperty("$ruleName.spawnAboveField.$setId", true)
		}

		override fun presetSave(engine:GameEngine, prop:CustomProperties, ruleName:String, setId:Int) {
			super.presetSave(engine, prop, ruleName, setId)
			prop.setProperty("$ruleName.bgmno.$setId", bgmId)
			prop.setProperty("$ruleName.big.$setId", big)
			prop.setProperty("$ruleName.goalType.$setId", goalType)
			prop.setProperty("$ruleName.shapetype.$setId", shapetype)
			prop.setProperty("$ruleName.comboWidth.$setId", comboWidth)
			prop.setProperty("$ruleName.ceilingAdjust.$setId", ceilingAdjust)
			prop.setProperty("$ruleName.spawnAboveField.$setId", spawnAboveField)
		}
	}
	/** Last preset number used */
	private var presetNumber:Int by DelegateMenuItem(itemSpd)

	private val itemBGM = BGMMenuItem("bgmno", COLOR.BLUE, BGM.values.indexOf(BGM.Rush(3)))
	/** BGM number */
	private var bgmId:Int by DelegateMenuItem(itemBGM)

	/** Current round's ranking position */
	private var rankingRank = 0

	override val ranking = List(GAMETYPE_MAX*GOAL_TABLE.size) {
		Leaderboard<ComboRow>(rankingMax, kotlinx.serialization.serializer<List<ComboRow>>())
	}
	@Serializable
	data class ComboRow(override val st:Statistics = Statistics()):Rankable {
		override fun compareTo(other:Rankable):Int =
			if(other is ComboRow)
				compareValuesBy(this, other, {it.clear}, {it.st.maxCombo}, {-it.ti}, {it.rp})
			else super.compareTo(other)

	}

	/**  */
	private val itemShape = StringsMenuItem(
		"shapetype", "StartShape", COLOR.BLUE, 1, SHAPE_NAME_TABLE, false, true
	)
	/** Shape type */
	private var shapetype:Int by DelegateMenuItem(itemShape)

	/** Stack color */
	private var stackColor = 0

	private val itemWidth = IntegerMenuItem("comboWidth", "WIDTH", COLOR.BLUE, 5, 2..10, true, true)
	/** Width of combo well */
	private var comboWidth:Int by DelegateMenuItem(itemWidth)

	/** */
	private val gameType:Int
		get() = when {
			comboWidth<4 -> 0
			comboWidth==4 -> 1
			else -> 2
		}

	private val itemCeil = IntegerMenuItem("ceilingAdjust", "CEILING", COLOR.BLUE, -2, -10..10, true, true)
	/** Height difference between ceiling and stack (negative number lowers the
	 * stack height) */
	private var ceilingAdjust:Int by DelegateMenuItem(itemCeil)

	private var itemAbove = BooleanMenuItem("spawnAboveField", "SpawnAbove", COLOR.BLUE, true, false, true)
	/** Piece spawns above field if true */
	private var spawnAboveField:Boolean by DelegateMenuItem(itemAbove)

	/** Number of remaining stack lines that need to be added when lines are
	 * cleared */
	private var remainStack = 0

	/** Next section lines */
	private var nextseclines = 0

	override val menu = MenuList("comborace", itemGoal, itemWidth, itemShape, itemSpd, itemCeil, itemAbove, itemBGM, itemBig)

	/** Returns the name of this mode */
	override val name = "REN Sprint"
	override val gameIntensity = 2
	/** This function will be called when the game enters the main game
	 * screen. */
	override fun playerInit(engine:GameEngine) {

		scgettime = 0
		bgmId = 0
		big = false
		goalType = 0
		shapetype = 1
		presetNumber = 0
		remainStack = 0
		stackColor = 0
		nextseclines = 10

		super.playerInit(engine)
		rankingRank = -1

		engine.frameSkin = GameEngine.FRAME_COLOR_RED

		netPlayerInit(engine)

		if(!engine.owner.replayMode) version = CURRENT_VERSION else {
			version = engine.owner.replayProp.getProperty("comborace.version", 0)
			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}
	}

	/** Ready */
	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			engine.fieldWidth = maxOf(4, comboWidth)
			engine.createFieldIfNeeded()
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			engine.meterValue = if(GOAL_TABLE[goalType]==-1) 0f else 1f

			if(!netIsWatch) {
				fillStack(engine, goalType)

				// NET: Send field
				if(netNumSpectators>0) netSendField(engine)
			}
		}
		return false
	}

	/** This function will be called before the game actually begins (after
	 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine) {
		if(version<=0) engine.big = big
		owner.musMan.bgm = if(netIsWatch) BGM.Silent else BGM.values[bgmId]
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.twistEnable = true
		engine.twistAllowKick = true
		engine.ruleOpt.pieceEnterAboveField = spawnAboveField
	}

	/** Fill the playfield with stack
	 * @param engine GameEngine
	 * @param height Stack height level number
	 */
	private fun fillStack(engine:GameEngine, height:Int) {
		val w = engine.field.width
		val h = engine.field.height
		val stackHeight:Int
		val cx = w-comboWidth/2-1
		/* set initial stack height and remaining stack lines
 depending on the goal lines and ceiling height adjustment */
		if(GOAL_TABLE[height]>h+ceilingAdjust||GOAL_TABLE[height]==-1) {
			stackHeight = h+ceilingAdjust
			remainStack = GOAL_TABLE[height]-h-ceilingAdjust
		} else {
			stackHeight = GOAL_TABLE[height]
			remainStack = 0
		}

		// fill stack from the bottom to the top
		if(w>comboWidth)
			for(y in h-1 downTo h-stackHeight) {
				for(x in 0..<w)
					if(x<cx-1||x>cx-2+comboWidth)
						engine.field.setBlock(
							x, y,
							Block(
								STACK_COLOR_TABLE[stackColor%STACK_COLOR_TABLE.size], engine.blkSkin, Block.ATTRIBUTE.VISIBLE,
								Block.ATTRIBUTE.GARBAGE
							)
						)
				stackColor++
			}

		// insert starting shape
		if(comboWidth==4)
			for(i in 0..11)
				if(SHAPE_TABLE[shapetype][i]==1)
					engine.field.setBlock(
						i%4, h-1-i/4,
						Block(SHAPE_COLOR_TABLE[shapetype], engine.blkSkin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE)
					)
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return

		receiver.drawScore(engine, 0, 0, name, BASE, COLOR.RED)

		receiver.drawScore(
			engine, 0, 1, if(GOAL_TABLE[goalType]==-1) "(Endless run)" else
				"(${GOAL_TABLE[goalType]-1}CHAIN Challenge)", BASE, COLOR.WHITE
		)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				receiver.drawScore(engine, 3, 3, "RECORD", BASE, COLOR.BLUE)

				ranking[typeSerial(gameType,goalType)].forEachIndexed { i, it ->
					receiver.drawScore(
						engine, 0, 4+i, "%2d".format(i+1),
						GRADE, if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW
					)
					if(it.st.maxCombo==GOAL_TABLE[goalType]-1)
						receiver.drawScore(engine, 2, 4+i, "PERFECT", BASE, true)
					else receiver.drawScore(engine, 3, 4+i, "${it.st.maxCombo}", NUM, rankingRank==i)
					receiver.drawScore(engine, 9, 4+i, it.ti.toTimeStr, NUM, rankingRank==i)
				}
			}
		} else {
			receiver.drawScore(engine, 0, 3, "Longest Chain", BASE, COLOR.BLUE)
			receiver.drawScore(
				engine, 0, 4, "${engine.statistics.maxCombo}",
				NUM,
				engine.statistics.maxCombo>0&&engine.combo==engine.statistics.maxCombo,
				2f
			)

			receiver.drawScore(engine, 0, 6, "Lines", BASE, COLOR.BLUE)
			receiver.drawScore(
				engine, 0, 7, "${engine.statistics.lines}", NUM,
				engine.statistics.lines==engine.statistics.totalPieceLocked,
				2f
			)

			receiver.drawScore(engine, 0, 9, "PIECE", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 0, 10, "${engine.statistics.totalPieceLocked}", NUM, 2f)


			receiver.drawScore(engine, 0, 15, "Time", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 0, 16, scgettime.toTimeStr, NUM_T)
			receiver.drawScore(engine, 0, 17, engine.statistics.time.toTimeStr, NANO)
		}

		super.renderLast(engine)
	}

	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		//  Attack
		if(ev.lines>0) {
			scgettime = engine.statistics.time

			// add any remaining stack lines
			val w = engine.field.width
			if(w>comboWidth) {
				if(GOAL_TABLE[goalType]==-1) remainStack = Integer.MAX_VALUE
				val cx = w-comboWidth/2-1
				var tmplines = 1
				while(tmplines<=ev.lines&&remainStack>0) {
					for(x in 0..<w)
						if(x<cx-1||x>cx-2+comboWidth)
							engine.field.setBlock(
								x, -ceilingAdjust-tmplines,
								Block(
									STACK_COLOR_TABLE[stackColor%STACK_COLOR_TABLE.size], engine.blkSkin, Block.ATTRIBUTE.VISIBLE,
									Block.ATTRIBUTE.GARBAGE
								)
							)
					stackColor++
					tmplines++
					remainStack--
				}
			}
			if(GOAL_TABLE[goalType]==-1) {
				val meterMax = 1
				val colorIndex = (engine.statistics.maxCombo-1)/meterMax
				engine.meterValue = (engine.statistics.maxCombo-1f)%meterMax
				engine.meterColor = METER_COLOR_TABLE[colorIndex%METER_COLOR_TABLE.size]
				engine.meterValueSub = if(colorIndex>0) 1f else 0f
				engine.meterColorSub = METER_COLOR_TABLE[maxOf(colorIndex-1, 0)%METER_COLOR_TABLE.size]
			} else {
				val remainLines = GOAL_TABLE[goalType]-engine.statistics.lines
				engine.meterValue = remainLines*1f/GOAL_TABLE[goalType]

				if(remainLines<=30) engine.meterColor = GameEngine.METER_COLOR_YELLOW
				if(remainLines<=20) engine.meterColor = GameEngine.METER_COLOR_ORANGE
				if(remainLines<=10) engine.meterColor = GameEngine.METER_COLOR_RED

				// Goal
				when {
					engine.statistics.lines>=GOAL_TABLE[goalType] -> {
						engine.ending = 1
						engine.statistics.rollClear = 1
						engine.gameEnded()
					}
					engine.statistics.lines>=GOAL_TABLE[goalType]-5 -> owner.musMan.fadeSW = true
					engine.statistics.lines>=nextseclines -> {
						owner.bgMan.nextBg = nextseclines/10
						nextseclines += 10
					}
				}
			}
		} else if(engine.statistics.maxCombo>=(if(GOAL_TABLE[goalType]==-1) 2 else GOAL_TABLE[goalType]-engine.statistics.lines)) {
			engine.ending = 1
			engine.gameEnded()
			engine.resetStatc()
			engine.stat = if(engine.statistics.maxCombo>=if(GOAL_TABLE[goalType]==-1) 40 else GOAL_TABLE[goalType]-1)
				GameEngine.Status.EXCELLENT else GameEngine.Status.GAMEOVER
			engine.statistics.time = scgettime
		}
		return 0
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine) {
		drawResultStats(engine, receiver, 0, COLOR.CYAN, Statistic.MAXCOMBO, Statistic.TIME)
		drawResultStats(
			engine, receiver, 4, COLOR.BLUE, Statistic.LINES, Statistic.PIECE, Statistic.LPM, Statistic.PPS
		)
		drawResultRank(engine, receiver, 12, COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, receiver, 14, COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, receiver, 16, COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenu(engine, 2, 18, "NEW PB", BASE, COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenu(engine, 0, 19, "SENDING...", BASE, COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2
		)
			receiver.drawMenu(engine, 1, 19, "A: RETRY", BASE, COLOR.RED)
	}

	/** This function will be called when the replay data is going to be saved */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		engine.owner.replayProp.setProperty("comborace.version", version)
		itemSpd.presetSave(engine, prop, menu.propName, -1)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty(
			"${engine.playerID}.net.netPlayerName",
			netPlayerName
		)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			rankingRank = ranking[goalType*GAMETYPE_MAX+gameType].add(ComboRow(engine.statistics))
			if(rankingRank!=-1) return true
		}
		return false
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(owner.bgMan.fadeSW) owner.bgMan.nextBg else owner.bgMan.bg
		val msg = "game\tstats\t"+
			engine.run {
				statistics.run {"${maxCombo}\t${lines}\t${totalPieceLocked}\t${time}\t${lpm}\t${pps}\t"}+
					"$goalType\t${gameActive}\t${timerActive}\t${meterColor}\t${meterValue}\t"
			}+"$bg\t$scgettime\t\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:List<String>) {
		listOf<(String)->Unit>(
			{}, {}, {}, {},
			{engine.statistics.maxCombo = it.toInt()},
			{engine.statistics.lines = it.toInt()},
			{engine.statistics.totalPieceLocked = it.toInt()},
			{engine.statistics.time = it.toInt()},
			{goalType = it.toInt()},
			{engine.gameActive = it.toBoolean()},
			{engine.timerActive = it.toBoolean()},
			{engine.meterColor = it.toInt()},
			{engine.meterValue = it.toFloat()},
			{owner.bgMan.bg = it.toInt()},
			{scgettime = it.toInt()}
		).zip(message).forEach {(x, y) ->
			x(y)
		}
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		val subMsg = engine.statistics.run {
			"MAX COMBO;${(maxCombo-1)}\tTIME;${time.toTimeStr}\tLINE;${lines}\tPIECE;${totalPieceLocked}\tLINE/MIN;${lpm}\tPIECE/SEC;${pps}\t"
		}
		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t"+engine.speed.run {
			"${gravity}\t${denominator}\t${are}\t${areLine}\t${lineDelay}\t${lockDelay}\t${das}\t"
		}+"$bgmId\t$goalType\t$presetNumber\t$shapetype${"\t\t$comboWidth\t$ceilingAdjust\t"+spawnAboveField}\n"
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
		shapetype = message[14].toInt()
		comboWidth = message[16].toInt()
		ceilingAdjust = message[17].toInt()
		spawnAboveField = message[18].toBoolean()
	}

	/** NET: Get goal type */
	override val netGetGoalType get() = goalType

	/** NET: It returns true when the current settings don't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = !big&&engine.ai==null

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Number of ranking types */
		private const val GAMETYPE_MAX = 3

		/** Hindrance Lines Constant */
		private val GOAL_TABLE = listOf(21, 41, 101, -1)

		private fun typeSerial(gameType:Int, goalType:Int) = goalType*GAMETYPE_MAX+gameType
		/** Names of starting shapes */
		private val SHAPE_NAME_TABLE = listOf(
			"NONE", "LEFT I", "RIGHT I", "LEFT Z", "RIGHT S", "LEFT S", "RIGHT Z", "LEFT J",
			"RIGHT L"
		)

		/** Number of starting shapes */
		private val SHAPETYPE_MAX = SHAPE_NAME_TABLE.size

		/** Starting shape table */
		private val SHAPE_TABLE = listOf(
			listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
			listOf(1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0), listOf(0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0),
			listOf(1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0), listOf(0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0),
			listOf(1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0), listOf(0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0),
			listOf(1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0), listOf(0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1)
		)

		/** Starting shape color */
		private val SHAPE_COLOR_TABLE = listOf(
			null, Block.COLOR.CYAN, Block.COLOR.CYAN, Block.COLOR.RED,
			Block.COLOR.GREEN, Block.COLOR.GREEN, Block.COLOR.RED, Block.COLOR.BLUE, Block.COLOR.ORANGE
		)

		/** Stack color order */
		private val STACK_COLOR_TABLE = listOf(
			Block.COLOR.RED, Block.COLOR.ORANGE, Block.COLOR.YELLOW,
			Block.COLOR.GREEN, Block.COLOR.CYAN, Block.COLOR.BLUE, Block.COLOR.PURPLE
		)

		/** Meter colors for really high combos in Endless */
		private val METER_COLOR_TABLE = listOf(
			GameEngine.METER_COLOR_GREEN, GameEngine.METER_COLOR_YELLOW,
			GameEngine.METER_COLOR_ORANGE, GameEngine.METER_COLOR_RED, GameEngine.METER_COLOR_PINK,
			GameEngine.METER_COLOR_PURPLE,
			GameEngine.METER_COLOR_DARKBLUE, GameEngine.METER_COLOR_BLUE, GameEngine.METER_COLOR_CYAN,
			GameEngine.METER_COLOR_DARKGREEN
		)
	}
}
