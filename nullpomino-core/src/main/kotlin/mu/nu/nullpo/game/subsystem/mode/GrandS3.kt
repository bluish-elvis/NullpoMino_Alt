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
import kotlinx.serialization.serializer
import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.component.Block.ATTRIBUTE
import mu.nu.nullpo.game.event.*
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.*
import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.BaseFont.FONT.*
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.times
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import zeroxfc.nullpo.custom.libs.Interpolation.cosStep

/** SPEED MANIAX Season Mode */
class GrandS3:AbstractGrand() {
	/** 最終結果などに表示される実際の段位 */
	private var grade = 0

	/** 段位表示を光らせる残り frame count */
	private var gradeFlash = 0

	private var water = 0
	/** Combo bonus */
	private var comboValue = 0

	/** Roll 経過 time */
	private var rollTime = 0

	/** Roll started flag */
	private var rollStarted = false

	/** せり上がりカウント */
	private var isFieldFrozen = false

	private var endGame = Triple(-1, -1, GameEngine.UndoState())
	private var endGameLv
		get() = endGame.first
		set(value) {
			endGame = Triple(value, endGame.second, endGame.third)
		}
	private var endGameStep
		get() = endGame.second
		set(value) {
			endGame = Triple(endGame.first, value, endGame.third)
		}
	private var endGameStat
		get() = endGame.third
		set(value) {
			endGame = Triple(endGame.first, endGame.second, value)
		}

	private var endGameTemp = 0
	/** REGRET display time frame count */
	private var regretDispFrame = 0

	/** 裏段位 */
	private var secretGrade = 0

	/** Section Time記録表示中ならtrue */
	private var isShowBestSectionTime = 0

	/** Number of sections */
	override val sectionMax = 26

	override val medalSKQuads = listOf(listOf(10, 20, 30, 40), listOf(1, 2, 4, 6))
	override val medalTSLines = listOf(listOf(32, 64, 96, 128), listOf(1, 2, 4, 6))

	private val itemLevel = LevelGrandMenuItem(COLOR.BLUE, sectionMax-1, false)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private var itemGrade = BooleanMenuItem("showgrade", "SHOW GRADE", COLOR.BLUE, false)
	/** 段位表示 */
	private var gradeDisp:Boolean by DelegateMenuItem(itemGrade)

	private val itemTemp = IntegerMenuItem("temperature", "Temperature", COLOR.CYAN, 0, 0, 50, true)
	private var temperature:Int by DelegateMenuItem(itemTemp)
	private val itemStep = IntegerMenuItem("steps", "Steps", COLOR.CYAN, 0, 0, 10, true)
	private var steps:Int by DelegateMenuItem(itemStep)

	private val itemMode = BooleanMenuItem("strict", "Strict", COLOR.RED, false, true)
	private var strict:Boolean by DelegateMenuItem(itemMode)

	/* Initialization */
	override val menu = MenuList("speedmaniax", itemMode, itemST, itemGrade, itemLevel, itemTemp, itemStep)

	@Serializable
	data class Token(var bravos:Int = 0, var quads:Int = 0, var twists:Int = 0, var frozens:Int = 0, var ices:Int = 0) {
		constructor(it:Statistics):this(it.bravos, it.totalQuadruple, it.totalTwistsLine)

		fun apply(it:Statistics) {
			quads = it.totalQuadruple+it.totalB2BQuad+it.totalB2BSplit+it.totalB2BTwist
			twists = it.totalTwistLinesSum+(it.totalTwistZero+it.totalTwistZeroMini)/2
			bravos = it.bravos
		}

		fun reset() {
			bravos = 0
			quads = 0
			twists = 0
			frozens = 0
			ices = 0
		}

		var temperature = 0
		var steps = 0
		val cold get() = frozens+ices/3
		val lvb:Int get() = (quads/10f+twists/16f+frozens/28f+bravos).toInt()+steps
		val frz:Int get() = 2+quads+twists+(frozens+temperature)*3+ices+bravos*5/2
	}

	private val token = Token()

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Section Time記録 */
	private val bestSectionTime = MutableList(sectionMax+1) {3599}
	private val sectionPts = MutableList(sectionMax+1) {0f}
	private val bestSectionPts = MutableList(sectionMax+1) {0f}
	override val ranking = listOf(Leaderboard(rankingMax, serializer<List<Rankable.GrandRow>>()))
	override val propRank
		get() = rankMapOf(
			"section.time" to bestSectionTime,
			"section.pts" to bestSectionPts
		)
	/*override val rankPersMap:Map<String, IntArray>
		get() = rankMap("grade" to rankingGrade, "level" to rankingLevel, "time" to rankingTime, "rollClear" to rankingRollClear,
			"section.time" to bestSectionTime, "exam.history" to gradeHistory, "exam.record" to examRecord)*/

	/* Mode name */
	override val name = "Grand Seasons"
	override val gameIntensity = 3

	private val showCenterOrig by lazy {owner.receiver.conf.showCenter}
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		secAlert = true
		grade = 0
		gradeFlash = 0
		comboValue = 0
		water = 0
		lastScore = 0
		rollTime = 0
		rollStarted = false
		regretDispFrame = 0
		secretGrade = 0
		token.reset()
		endGame = Triple(-1, -1, GameEngine.UndoState())
		endGameTemp = 0
		rankingRank = -1
		ranking.forEach {it.fill(Rankable.GrandRow())}
		owner.receiver.conf.showCenter = showCenterOrig
		bestSectionTime.fill(3599)
		if(sectionTime.size<=sectionMax) sectionTime.addLast(0)
		if(sectionIsNewRecord.size<=sectionMax) sectionIsNewRecord.addLast(false)
		engine.receiver.setBGSpd(engine.owner, 2f)
		engine.twistEnable = true
		engine.b2bEnable = true
		engine.splitB2B = true
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.frame = GameEngine.Frame.RED
		engine.bigHalf = true
		engine.bigMove = true
		engine.staffrollEnable = true
		engine.staffrollNoDeath = false

	}
	/** Set BGM at start of game
	 */
	private fun calcBgmLv(lv:Int) = tableBGMChange.count {lv>=it}

	private fun freezeRange(engine:GameEngine) = (engine.field.heightWoFloor/2..engine.field.height).toSet()

	/** Update falling speed
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = -1

		val section = engine.statistics.level/100
		engine.speed.replace(tableLevel[minOf(section, tableLevel.size-1)])

		isFieldFrozen = tableFrozenSection.contains(section)
	}

	/** ST medal check
	 * @param engine GameEngine
	 * @param section Section number
	 */
	private fun stMedalCheck(engine:GameEngine, section:Int) =
		stMedalCheck(engine, section, sectionTime.getOrElse(section) {rollTime},
			bestSectionTime.let {it[section.coerceIn(it.indices)]})

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		if(!owner.replayMode) {
			// section time display切替
			if(engine.ctrl.isPush(Controller.BUTTON_F)) {
				engine.playSE("change")
				isShowBestSectionTime = (isShowBestSectionTime+1).mod(3)
			}
		}
		return super.onSetting(engine)
	}

	override fun onSettingChanged(engine:GameEngine) {
		super.onSettingChanged(engine)
		val lv = startLevel*100
		engine.statistics.level = lv

		nextSecLv = (lv+100).coerceIn(100, 2600)
		owner.bgMan.bg = -minOf(1+startLevel, 15)
		token.temperature = temperature
		token.steps = steps
		setSpeed(engine)
	}

	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==1) {
			engine.field.lockedLines = if(tableFrozenSection.contains(startLevel)) freezeRange(engine) else emptySet()
			if(startLevel>=10) dirShuffle(engine)
		}
		return super.onReady(engine)
	}

	private fun dirShuffle(engine:GameEngine) {
		engine.nextPieceArrayObject.forEach {p ->
			engine.random.nextInt(Piece.DIRECTION_COUNT*3).let {
				if(it<Piece.DIRECTION_COUNT) p.direction = it
			}
		}
	}
	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		if(endGameLv<=0) owner.musMan.bgm = BGM.GrandR(calcBgmLv(startLevel*100))
		token.temperature = temperature
		token.steps = steps
	}

	override fun renderFirst(engine:GameEngine) {
		if(engine.gameActive&&engine.ending==2) receiver.drawStaffRoll(engine, rollTime*1f/ROLLTIMELIMIT)
	}
	/* Render score */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScore(engine, 0, 0, name, BASE, COLOR.RED)

		receiver.drawScore(engine, -1, -4*2, "DECORATION", BASE, scale = .5f)
		receiver.drawScoreBadges(engine, 0, -3, 100, owner.stats.decoration)
		receiver.drawScoreBadges(engine, 5, -4, 100, decTemp)
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startLevel==0&&temperature<=0&&steps<=0&&engine.ai==null)
				if(isShowBestSectionTime<=0) {
					// Rankings
					val topY = if(receiver.bigSideNext) 5 else 3
					receiver.drawScore(engine, 0, topY-1, "GRADE LV TIME", BASE, COLOR.RED)

					ranking[0].forEachIndexed {i, it ->
						receiver.drawScore(engine, 0, topY+i, "%02d".format(i+1), GRADE, COLOR.YELLOW)
						receiver.drawScore(engine, 5, topY+i, "%03d".format(it.lv), NUM, i==rankingRank)
						receiver.drawScore(
							engine, 2, topY+i, getShortGradeName(it.grade), GRADE,
							when(it.clear) {
								1 -> COLOR.GREEN; 2 -> COLOR.ORANGE; else -> COLOR.WHITE; }
						)
						receiver.drawScore(engine, 8, topY+i, it.ti.toTimeStr, NUM, i==rankingRank)
					}

					receiver.drawScore(engine, 0, 20, "F:VIEW SECTION TIME", BASE, COLOR.GREEN)
				} else {
					// Section Time
					receiver.drawScore(engine, 0, 2, "SECTION TIME", BASE, COLOR.RED)
					val stt = (isShowBestSectionTime-1)*13
					(stt..<(stt+14).coerceIn(bestSectionTime.indices)).forEachIndexed {y, i ->
						val slv = i*100
						receiver.drawScore(
							engine, 0, 3+y, "%4d-%4d %s".format(slv, slv+99, bestSectionTime[i].toTimeStr), NUM, sectionIsNewRecord[i]
						)
					}
					val totalTime = bestSectionTime.filter {it<3599}.sum()

					receiver.drawScore(engine, 0, 18, "TOTAL", BASE, COLOR.RED)
					receiver.drawScore(engine, 0, 19, totalTime.toTimeStr, NUM_T)
					receiver.drawScore(engine, 9, 18, "AVERAGE", BASE, COLOR.RED)
					receiver.drawScore(engine, 9, 19, (totalTime/sectionMax).toTimeStr, NUM_T)

					receiver.drawScore(engine, 0, 21, "F:VIEW RANKING", BASE, COLOR.GREEN)
				}
		} else {
			if(gradeDisp) {
				// 段位
				if(grade>=0)
					receiver.drawScore(engine, 0, 1, getGradeName(grade), GRADE, gradeFlash>0&&gradeFlash%4==0, 2f)

				// Score
				receiver.drawScore(engine, 0, 3, "Score", BASE, COLOR.RED)
				receiver.drawScore(engine, 5, 3, "+$lastScore", NUM)
				receiver.drawScore(engine, 0, 4, "$scDisp", NUM, 2f)
				receiver.drawScore(engine, 4, 10f, "+${token.lvb}", NUM_T)
				receiver.drawScore(engine, 6, 9f, "+${token.frz}", NUM)
			}

			// medal
			receiver.drawScoreMedal(engine, 0, 6, "SK", medalSK)
			receiver.drawScore(engine, 2, 6, "%3d".format(token.quads), NUM)
			receiver.drawScoreMedal(engine, 0, 7, "TS", medalTS)
			receiver.drawScore(engine, 2, 7, "%3d".format(token.twists), NUM)
			receiver.drawScoreMedal(engine, 5, 6, "AC", medalAC)
			receiver.drawScore(engine, 7, 6, "%3d".format(token.bravos), NUM)
			receiver.drawScoreMedal(engine, 5, 7, "FR", token.cold*4/42)
			receiver.drawScore(engine, 7, 7, "%3d".format(token.cold), NUM)

			receiver.drawScoreMedal(engine, 5, 20, "ST", medalST)
			receiver.drawScore(engine, 7, 20, medalsST.joinToString("."), NUM)
			receiver.drawScoreMedal(engine, 5, 21, "CO", medalCO)
			receiver.drawScore(engine, 7, 21, "%3d".format(engine.statistics.maxCombo), NUM)
			// level
			receiver.drawScore(engine, 0, 9, "Level", BASE, COLOR.RED)
			receiver.drawScore(engine, 1, 10, "%4d".format(maxOf(engine.statistics.level, 0)), NUM)
			receiver.drawScoreSpeed(engine, 0, 11, if(engine.speed.gravity<0) 40 else engine.speed.gravity/128, 4)
			receiver.drawScore(engine, 1, 12, "%4d".format(if(endGameLv<0) nextSecLv else endGameLv), NUM)

			// Time
			receiver.drawScore(engine, 0, 14, "Time", BASE, COLOR.RED)
			if(engine.ending!=2||rollTime/10%2==0||!engine.gameActive)
				receiver.drawScore(engine, 0, 15, engine.statistics.time.toTimeStr, NUM_T)

			// Roll 残り time
			if(engine.gameActive&&engine.ending==2) {
				var time = ROLLTIMELIMIT-rollTime
				if(time<0) time = 0
				receiver.drawScore(engine, 0, 17, "ROLL TIME", BASE, COLOR.RED)
				receiver.drawScore(engine, 0, 18, time.toTimeStr, NUM, time>0&&time<10*60, 2f)
			}

			if(isFieldFrozen) (receiver as AbstractRenderer).run {
				drawBlendAdd {
					val bs = engine.blockSize
					val range = freezeRange(engine)
					drawRect(fieldX(engine, 0), fieldY(engine, range.first()),
						bs*engine.field.width, bs*(range.last()-range.first()), 0x99, 1f, 1, 0x9999)
				}
			}
			// Section Time
			if(showST&&sectionTime.isNotEmpty()) {
				val y = if(receiver.bigSideNext) 4 else 2
				val x = if(receiver.bigSideNext) 20 else 12
				val x2 = if(receiver.bigSideNext) 9 else 12

				receiver.drawScore(engine, x, y, "SECTION TIME", BASE, COLOR.RED)
				val section = engine.statistics.level/100

				sectionTime.forEachIndexed {i, it ->
					if(it>0) receiver.drawScore(
						engine, x-1, y+1+i,
						"%4d%s%s".format(i*100, if(i==section&&engine.ending==0) "+" else "-", it.toTimeStr),
						NUM,
						sectionIsNewRecord[i]
					)
				}
				receiver.drawScore(engine, x2, 17, "AVERAGE", BASE, COLOR.RED)
				receiver.drawScore(engine, x2, 18, sectionTime.filter {it>0}.average().toTimeStr, NUM_T)
			}
		}
	}

	/* 移動中の処理 */
	override fun onMove(engine:GameEngine):Boolean {
		super.onMove(engine)
		if(engine.statc[0]==0&&engine.statistics.level>=1300) {
			engine.getNextObject()?.setAttribute(true, ATTRIBUTE.IGNORE_LINK)
		}
		/* 移動中の処理 */
		if(engine.ending==2&&endGameLv>0&&!rollStarted) rollStarted = true
		return false
	}

	override fun onCustom(engine:GameEngine):Boolean {
		if(engine.ending==0||endGameLv<=0) {
			engine.gameEnded()
			engine.staffrollEnable = false
			engine.ending = 1
			engine.stat = GameEngine.Status.ENDINGSTART
			engine.timerActive = false
			secretGrade = engine.field.secretGrade
			return true
		}
		if(engine.tempHanabi>0) {
			engine.resetStatc(); return true
		}
		engine.statc[0]++
		if(engine.statc[1]<=0) {
			if(engine.statc[0]==120) engine.playSE("excellent")
			if(engine.statc[0]==240) owner.musMan.bgm = BGM.Ending(3)
			if(engine.statc[0]>=400) {
				engine.statc[0] = 0
				engine.statc[1] = 1
			}
		} else if(engine.statc[1]==1) {
			val step = if(engine.statc[0]==1200) endGameStep-engine.statc[2]
			else cosStep(endGameStep.toFloat(), 0f, (1200-engine.statc[0])/1200f).toInt()
			if(step-engine.statc[2]>0&&engine.statc[0]<=1200) {
				repeat(step-engine.statc[2]) {
					engine.undo("GrandS3")
					engine.statc[2]++
				}
			}
			if(engine.statc[0]>=1200) {
				engine.field.replace(endGameStat.field)
				engine.statistics.level = endGameLv
				engine.field.filterBlocks {b, _, _ -> b.hard!=0||b.getAttribute(ATTRIBUTE.IGNORE_LINK)}.forEach {(b) ->
					b.setAttribute(false, ATTRIBUTE.IGNORE_LINK)
					b.hard = 0
				}
				engine.resetStatc()
				engine.stat = GameEngine.Status.READY
				return true
			}

		}
		return false
	}

	override fun renderCustom(engine:GameEngine) {
		val col = COLOR.WHITE

		val cY = (engine.fieldHeight-1)

		if(engine.statc[2]<=0) {
			if(engine.statc[1]>0||engine.statc[0]>=120) {
				receiver.drawMenu(engine, 0f, cY/3f, "EXCELLENT!", BASE, COLOR.ORANGE, 1f)
				receiver.drawMenu(engine, 2f, 8f, "BUT...", BASE, col)
				receiver.drawMenu(engine, -.25f, 9f, "THIS IS NOT", BASE, col)
				receiver.drawMenu(engine, .5f, 10f, "OVER YET!", BASE, col)
			}
		}
		super.renderCustom(engine)
	}
	/** levelが上がったときの共通処理 */
	override fun levelUp(engine:GameEngine, lu:Int) {
		val lb = engine.statistics.level

		super.levelUp(engine, lu)
		val lA = engine.statistics.level

		if(lu<=0) return
		// BGM fadeout
		if(tableBGMFadeout.any {it in lb..lA}) owner.musMan.fadeSW = true
		// BGM切り替え
		if(tableBGMChange.any {it in lb..lA}) {
			owner.musMan.fadeSW = false
			owner.musMan.bgm = BGM.GrandR(calcBgmLv(lA))
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Combo
		val li = ev.lines
		// Calculate score
		val pts = super.calcScore(engine, ev)

		if(li>=1) if(engine.ending==0||(engine.ending==2&&endGameLv>0)) {
			token.apply(engine.statistics)
			if(isFieldFrozen) token.frozens += engine.lastLinesY.flatten().intersect(engine.field.lockedLines).size
			// Level up
			val levelb = engine.statistics.level
			val section = levelb/100
			if(section>=13) {
				if(!strict) engine.field.lastLinesCleared.values.let {llc ->
					val fli = llc.count {it.any {(_, b) -> b?.hard!=0}}
					val fbc = llc.sumOf {it.values.filter {b -> b?.hard!=0}.size}
					water += (5+fli*5+minOf(
						maxOf(fbc/2, minOf(fli*10, token.frz/12-8, 20-(engine.field.highestBlockY*2-10).coerceIn(0, 20))), 20)
						+ev.combo+ev.twist*10+ev.split*10+token.lvb)
				}

				var dl = 0
				if(li>=4) dl++
				while(!strict&&water>=30) {
					dl++; water -= 30
				}
				repeat(dl) {
					engine.field.delLine(engine.field.bottomY-it)
					token.ices++
				}

			}

			levelUp(engine, li.let {it+maxOf(0, it-2)+token.lvb})
			if(engine.statistics.level>=sectionMax*100) {
				// Ending
				engine.playSE("endingstart")
				engine.owner.musMan.bgm = BGM.Silent
				engine.statistics.level = sectionMax*100
				if(engine.ending==0) {
					engine.statistics.rollClear = 1
					if(engine.statistics.time>=ENDGAME_QUOTA) {
						//GM Torikan
						engine.gameEnded()
						engine.staffrollEnable = false
						engine.ending = 1
						engine.timerActive = false
						secretGrade = engine.field.secretGrade
						endGameLv = -1
					} else {
						engine.ending = 1
						engine.timerActive = false
						endGame = engine.undoStack.withIndex().let {
							it.filter {(_, it) -> it.time>=engine.statistics.time-ROLLTIMELIMIT}
								.minByOrNull {(_, it) -> it.time}?:it.last()
						}.let {(i, it) -> Triple(it.statistics.level, engine.undoStack.size-i, it)}
						nextSecLv = sectionMax*100
					}
					engine.tempHanabi += 10
					sectionsDone++
					decTemp++
					// ST medal
					stMedalCheck(engine, section)

					// 段位上昇
					grade = 2
					gradeFlash = 180
					endGameTemp = engine.field.filterBlocks {it, _, _ -> it.hard!=0}.size
				} else {
					// ST medal
					stMedalCheck(engine, sectionMax)
					engine.statistics.rollClear = 2
					engine.gameEnded()
					engine.resetStatc()
					engine.ending = 1
					engine.timerActive = false
					engine.playSE("grade4")
					engine.playSE("applause5")
					engine.tempHanabi += 36
					grade = (when {
						endGameLv<=1300 -> 17//GMSM
						endGameLv<=1350 -> 16//GMS13
						endGameLv<=1400 -> 15//GMS12
						endGameLv<=1450 -> 14//GMS11
						endGameLv<=1500 -> 13//GMS10
						endGameLv<=1550 -> 12
						endGameLv<=1600 -> 11
						endGameLv<=1650 -> 10//GMS7
						endGameLv<=1700 -> 9//GMS6
						endGameLv<=1800 -> 8//GMS5
						endGameLv<=1900 -> 7
						endGameLv<=2000 -> 6
						endGameLv<=2100 -> 5//GMS2
						endGameLv<=2300 -> 4//GMS1
						else -> 3//GMS
					}+maxOf(30-endGameTemp, (90-endGameTemp)/10, 0)).coerceIn(0, 17)
					gradeFlash = 180
				}

			} else if(engine.statistics.level>=nextSecLv) {
				// Next Section
				engine.playSE("levelup")

				// Background切り替え
				val nextSec = nextSecLv/100
				owner.bgMan.nextBg = -minOf(1+nextSec, 15)

				sectionsDone++
				// ST medal
				stMedalCheck(engine, section)
				engine.field.lockedLines = emptySet()
				if(nextSec>=10&&grade<1&&startLevel<10) {
					grade = 1
					owner.receiver.conf.showCenter = true
					dirShuffle(engine)
				}

				// Update level for next section
				nextSecLv += 100

				// 段位上昇
				//if(grade<tableSectionMax) grade++
				//gradeFlash = 180

			}
			lastScore = pts
			engine.statistics.scoreLine += pts
			return pts
		}

		return 0
	}

	override fun onEndingStart(engine:GameEngine):Boolean {
		if(engine.staffrollEnable&&endGameLv>0&&!rollStarted) {

			engine.stat = GameEngine.Status.CUSTOM
			engine.ending = 2
		return true
	}
		return false
	}

	override fun onARE(engine:GameEngine):Boolean {
		if(engine.statc[0]==0&&isFieldFrozen) engine.field.lockedLines = freezeRange(engine)
		return super.onARE(engine)
	}
	/* 各 frame の終わりの処理 */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		// 段位上昇時のフラッシュ
		if(gradeFlash>0) gradeFlash--

		// REGRET表示
		if(regretDispFrame>0) regretDispFrame--

		if(engine.gameActive&&engine.stat!=GameEngine.Status.READY&&engine.stat!=GameEngine.Status.CUSTOM) {
			val section = engine.statistics.level/100
			// Section Time増加
			if(engine.timerActive&&section in sectionTime.indices) sectionTime[section] =
				engine.statistics.time-sectionTime.take(section).sum()

			if(section>=13) engine.field.filterBlocks {it, _, _ ->
				it.getAttribute(ATTRIBUTE.IGNORE_LINK)&&!it.getAttribute(ATTRIBUTE.ERASE)&&it.hard!=-1
			}.forEach {(b) ->
				if(b.getAttribute(ATTRIBUTE.LAST_COMMIT))
					b.elapsedFrames = minOf(b.elapsedFrames, engine.ruleOpt.lockFlash, token.frz-1)
				else if(b.elapsedFrames>=token.frz) {
					b.hard = -1
					b.darkness = -.5f
					b.color?.let {b.secondaryColor = it}
				}
			}

			// Ending
			if(rollStarted) {
				rollTime++

				// Time meter
				val remainRollTime = ROLLTIMELIMIT-rollTime
				engine.meterValue = remainRollTime*1f/ROLLTIMELIMIT
				engine.meterColor = GameEngine.METER_COLOR_LIMIT

				// Roll 終了
				if(rollTime>=ROLLTIMELIMIT) {
					secretGrade = engine.field.secretGrade
					engine.statistics.rollClear = 1
					engine.ending = 0
					engine.lives = -1
					engine.gameEnded()
					engine.resetStatc()
					engine.stat = GameEngine.Status.GAMEOVER
				}
			} else if(engine.statistics.level==nextSecLv-1)
				engine.meterColor = if(engine.meterColor==-0x1) -0x10000 else -0x1
		}
	}

	/* Called at game over */
	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.statc[0]==0&&engine.gameActive) {
			secretGrade = engine.field.secretGrade

			val time = engine.statistics.time
			if(time<6000)
				decTemp -= 3
			else {
				decTemp++
				if(time%3600 !in 61..<3540) decTemp++
			}

			if(sectionsDone==0) decTemp -= 4
			// Blockの表示を元に戻す
			engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
			// 裏段位
			secretGrade = engine.field.secretGrade
			owner.stats.decoration += decTemp+secretGrade
		}
		return false
	}

	/* 結果画面 */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenu(engine, 0, 0, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE${engine.statc[1]+1}/3", BASE, COLOR.RED)

		when(engine.statc[1]) {
			0 -> {
				val gcolor = when(engine.statistics.rollClear) {
					1, 3 -> COLOR.GREEN
					2, 4 -> COLOR.ORANGE
					else -> COLOR.WHITE
				}
				receiver.drawMenu(engine, 0, 2, "GRADE", BASE, COLOR.RED)
				receiver.drawMenu(engine, 0, 1.66f, getShortGradeName(grade), GRADE, gcolor, 2f)

				drawResultStats(
					engine, receiver, 4, COLOR.RED, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_MANIA, Statistic.TIME
				)
				drawResultRank(engine, receiver, 12, COLOR.RED, rankingRank)
				if(secretGrade>4) {
					receiver.drawMenu(engine, 0, 15, "SECRET GRADE", NANO, COLOR.BLUE, .75f)
					receiver.drawMenu(engine, 6, 15, tableSecretGradeName[secretGrade-1], GRADE, 2f)
				}
			}
			1 -> {
				receiver.drawMenu(engine, 0, 2, "SECTION", BASE, COLOR.RED)

				val stt = (isShowBestSectionTime-1)*13
				(stt..<(stt+10).coerceIn(bestSectionTime.indices)).takeWhile {sectionTime[it]>0}.forEachIndexed {y, i ->
					receiver.drawMenu(engine, 2, 3+y, sectionTime[i].toTimeStr, NUM, sectionIsNewRecord[i])
				}
				if(sectionAvgTime>0) {
					receiver.drawMenu(engine, 0, 16, "AVERAGE", BASE, COLOR.RED)
					receiver.drawMenu(engine, 2, 17, sectionAvgTime.toTimeStr, NUM)
				}
			}
			2 -> {

				receiver.drawMenuMedal(engine, 8, 1, "AC", medalAC)
				receiver.drawMenuMedal(engine, 2, 2, "SK", medalSK)
				receiver.drawMenuMedal(engine, 5, 2, "TS", medalTS)
				receiver.drawMenuMedal(engine, 2, 2, "FR", token.cold*4/42)
				receiver.drawMenu(engine, 0, 4, "MEDAL", NANO, COLOR.RED, .5f)
				receiver.drawMenuMedal(engine, 5, 4, "ST", medalST)
				receiver.drawMenuMedal(engine, 1, 5, "RE", medalRE)
				receiver.drawMenuMedal(engine, 4, 5, "RO", medalRO)
				receiver.drawMenuMedal(engine, 7, 5, "CO", medalCO)

				drawResultStats(engine, receiver, 4, COLOR.RED, Statistic.LPM, Statistic.SPM, Statistic.PIECE, Statistic.PPS)

				receiver.drawMenu(engine, 0, 15, "DECORATION", BASE, COLOR.RED)
				receiver.drawMenu(engine, 0, 16, "%10d".format(decTemp), BASE, COLOR.WHITE)
			}
		}
	}

	/* 結果画面の処理 */
	override fun onResult(engine:GameEngine):Boolean {
		owner.musMan.fadeSW = false
		owner.musMan.bgm = when(engine.ending) {
			0 -> BGM.Result(0)
			2 if engine.statistics.rollClear>0 -> BGM.Result(3)
			else -> BGM.Result(2)
		}
		// ページ切り替え
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
			engine.statc[1] = engine.statc[1]
			if(engine.statc[1]<0) engine.statc[1] = 2
			engine.playSE("change")
		}
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
			engine.statc[1]++
			if(engine.statc[1]>2) engine.statc[1] = 0
			engine.playSE("change")
		}
		// section time display切替
		if(engine.ctrl.isPush(Controller.BUTTON_F)) {
			engine.playSE("change")
			isShowBestSectionTime = (isShowBestSectionTime+1).mod(3)
		}

		return false
	}

	/* リプレイ保存 */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		// Update rankings
		if(!owner.replayMode&&startLevel==0&&temperature<=0&&steps<=0&&engine.ai==null) {
			owner.statsProp.setProperty("decoration", owner.stats.decoration)
			rankingRank = ranking[0].add(Rankable.GrandRow(grade, engine.statistics, medals.copy()))
			if(medalST==3) updateBestSectionTime()

			if(rankingRank!=-1||medalST==3) return true
		}
		return false
	}

	/** Update best section time records */
	private fun updateBestSectionTime() {
		for(i in 0..<sectionMax)
			if(sectionIsNewRecord[i]) {
				bestSectionTime[i] = sectionTime[i]
				bestSectionPts[i] = sectionPts[i]
			}
	}

	companion object {

		/** Default torikan time for ending 87031 */
		private const val ENDGAME_QUOTA = 50000 // 13:53.33
		//30000 = 8:20
		/** Ending time limit */
		private const val ROLLTIMELIMIT = 4000

		private val tableFrozenSection = listOf(3, 5, 7, 8, 9)

		private val tableLevel =
			LevelData(
				listOf(12, 12, 12, 11, 10, 9, 8, 7, 7, 6, 12),
				listOf(+8, +7, +6, +6, +6, 5, 5, 5, 5, 5, 10),
				listOf(+6, +5, +4, +4, +4, 3),
				listOf(30, 28, 26, 25, 25, 25, 25, 24, 23, 22,
					//10,11,12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
					28, 26, 24, 32, 31, 30, 29, 28, 27, 26, 25),
				listOf(12, 12, 11, 10, 10, 9, 9, 9, 8, 8,
					8, 8, 8, 8, 4))

		/** BGM change levels */
		private val tableBGMChange = listOf(300, 700, 1000, 1300)
		/** BGM fadeout levels */
		private val tableBGMFadeout = tableBGMChange.map {it-15}
		/** 段位のName */
		private fun getGradeName(i:Int) = listOf("", "Master", "GrandMaster").let {
			it[i.coerceIn(it.indices)]+when(i) {
				in 3..16 -> "S${i-3}"; 17 -> "SMaster"; else -> ""
			}
		}

		private fun getShortGradeName(i:Int) = listOf("", "M", "GM").let {
			it[i.coerceIn(it.indices)]+when(i) {
				in 3..16 -> "S${i-3}"; 17 -> "SM"; else -> ""
			}
		}

		/** 裏段位のName */
		private val tableSecretGradeName =
			listOf("S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9",
				"GM")

	}
}
