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
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** SPEED MANIAX Season Mode */
class GrandS3:AbstractGrand() {
	/** 最終結果などに表示される実際の段位 */
	private var grade = 0

	/** 段位表示を光らせる残り frame count */
	private var gradeFlash = 0

	/** Combo bonus */
	private var comboValue = 0

	/** Roll 経過 time */
	private var rollTime = 0

	/** Roll started flag */
	private var rollStarted = false

	/** せり上がりカウント */
	private var isFieldFrozen = false

	private var endGame = -1
	/** REGRET display time frame count */
	private var regretDispFrame = 0

	/** 裏段位 */
	private var secretGrade = 0

	/** Section Time記録表示中ならtrue */
	private var isShowBestSectionTime = false

	/** Number of sections */
	override val sectionMax = 26

	override val medalSKQuads = listOf(listOf(10, 20, 30, 40), listOf(1, 2, 4, 6))
	override val medalTSLines = listOf(listOf(32, 64, 96, 128), listOf(1, 2, 4, 6))

	private val itemLevel = LevelGrandMenuItem(COLOR.BLUE, sectionMax, false)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private var itemGrade = BooleanMenuItem("showgrade", "SHOW GRADE", COLOR.BLUE, false)
	/** 段位表示 */
	private var gradeDisp:Boolean by DelegateMenuItem(itemGrade)

	private val itemTemp = IntegerMenuItem("temperature", "Temperature", COLOR.CYAN, 0, 0, 50, true)
	/** Level at start */
	private var temperature:Int by DelegateMenuItem(itemTemp)
	private val itemStep = IntegerMenuItem("temperature", "Steps", COLOR.CYAN, 0, 0, 70, true)
	/** Level at start */
	private var steps:Int by DelegateMenuItem(itemStep)

	/* Initialization */
	override val menu = MenuList("speedmaniax", itemAlert, itemST, itemGrade, itemLevel, itemTemp, itemStep)

	@Serializable
	data class Token(var bravos:Int = 0, var quads:Int = 0, var twists:Int = 0, var frozens:Int = 0) {
		constructor(it:Statistics):this(it.bravos, it.totalQuadruple, it.totalTwistsLine, 0)

		fun apply(it:Statistics) {
			quads = it.totalQuadruple
			twists = it.totalTwistsLine/2
			bravos = it.bravos
		}

		fun reset() {
			bravos = 0
			quads = 0
			twists = 0
			frozens = 0
		}

		val lvb:Int get() = (quads/10f+twists/16f+frozens/28f+bravos/6f).toInt()//quads/10+twists/16+frozens/28+bravos/6
		val frz:Int get() = 2+quads+twists+frozens*3+bravos*5/2
		val pt:Float get() = lvb+frz*.01f
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
		grade = 0
		gradeFlash = 0
		comboValue = 0
		lastScore = 0
		rollTime = 0
		rollStarted = false
		regretDispFrame = 0
		secretGrade = 0
		token.reset()
		endGame = -1
		rankingRank = -1
		ranking.forEach {it.fill(Rankable.GrandRow())}
		owner.receiver.conf.showCenter = showCenterOrig
		bestSectionTime.fill(3599)
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
		stMedalCheck(engine, section, sectionTime[section], bestSectionTime[section])

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		if(!owner.replayMode) {
			// section time display切替
			if(engine.ctrl.isPush(Controller.BUTTON_F)) {
				engine.playSE("change")
				isShowBestSectionTime = !isShowBestSectionTime
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
		token.frozens = temperature
		token.quads = steps
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
			}; p.updateConnectData()
		}
	}
	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		owner.musMan.bgm = BGM.GrandR(calcBgmLv(startLevel*100))
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
				if(!isShowBestSectionTime) {
					// Rankings
					val topY = if(receiver.bigSideNext) 5 else 3
					receiver.drawScore(engine, 0, topY-1, "GRADE LV TIME", BASE, COLOR.RED)

					ranking[0].forEachIndexed {i, it ->
						receiver.drawScore(engine, 0, topY+i, "%02d".format(i+1), GRADE, COLOR.YELLOW)
						receiver.drawScore(engine, 5, topY+i, "%03d".format(it.lv), NUM, i==rankingRank)
						receiver.drawScore(
							engine, 2, topY+i, tableGradeName[it.grade], GRADE,
							when(it.clear) {
								1 -> COLOR.GREEN; 2 -> COLOR.ORANGE; else -> COLOR.WHITE; }
						)
						receiver.drawScore(engine, 8, topY+i, it.ti.toTimeStr, NUM, i==rankingRank)
					}

					receiver.drawScore(engine, 0, 20, "F:VIEW SECTION TIME", BASE, COLOR.GREEN)
				} else {
					// Section Time
					receiver.drawScore(engine, 0, 2, "SECTION TIME", BASE, COLOR.RED)

					val totalTime = (0..<sectionMax).fold(0) {tt, i ->
						val slv = i*100
						receiver.drawScore(
							engine, 0, 3+i, "%4d-%4d %s".format(slv, slv+99, bestSectionTime[i].toTimeStr),
							NUM,
							sectionIsNewRecord[i]
						)
						tt+bestSectionTime[i]
					}

					receiver.drawScore(engine, 0, 17, "TOTAL", BASE, COLOR.RED)
					receiver.drawScore(engine, 0, 18, totalTime.toTimeStr, NUM_T)
					receiver.drawScore(engine, 9, 17, "AVERAGE", BASE, COLOR.RED)
					receiver.drawScore(engine, 9, 18, (totalTime/sectionMax).toTimeStr, NUM_T)

					receiver.drawScore(engine, 0, 20, "F:VIEW RANKING", BASE, COLOR.GREEN)
				}
		} else {
			if(gradeDisp) {
				// 段位
				if(grade>=0&&grade<tableGradeName.size)
					receiver.drawScore(engine, 0, 1, tableGradeName[grade], GRADE, gradeFlash>0&&gradeFlash%4==0, 2f)

				// Score
				receiver.drawScore(engine, 0, 3, "Score", BASE, COLOR.RED)
				receiver.drawScore(engine, 5, 3, "+$lastScore", NUM)
				receiver.drawScore(engine, 0, 4, "$scDisp", NUM, 2f)
				receiver.drawScoreNum(engine, 6, 9, token.pt, 6 to 3, scale = 2f)
			}

			// medal
			receiver.drawScoreMedal(engine, 0, 6, "SK", medalSK)
			receiver.drawScore(engine, 2, 6, "%3d".format(token.quads), NUM)
			receiver.drawScoreMedal(engine, 0, 7, "TS", medalTS)
			receiver.drawScore(engine, 2, 7, "%3d".format(token.twists), NUM)
			receiver.drawScoreMedal(engine, 5, 6, "AC", medalAC)
			receiver.drawScore(engine, 7, 6, "%3d".format(token.bravos), NUM)
			receiver.drawScoreMedal(engine, 5, 7, "FR", token.frozens*4/42)
			receiver.drawScore(engine, 7, 7, "%3d".format(token.frozens), NUM)

			receiver.drawScoreMedal(engine, 5, 20, "ST", medalST)
			receiver.drawScore(engine, 7, 20, medalsST.joinToString("."), NUM)
			receiver.drawScoreMedal(engine, 5, 21, "CO", medalCO)
			receiver.drawScore(engine, 7, 21, "%3d".format(engine.statistics.maxCombo), NUM)
			// level
			receiver.drawScore(engine, 0, 9, "Level", BASE, COLOR.RED)
			receiver.drawScore(engine, 1, 10, "%3d".format(maxOf(engine.statistics.level, 0)), NUM)
			receiver.drawScoreSpeed(engine, 0, 11, if(engine.speed.gravity<0) 40 else engine.speed.gravity/128, 4)
			if(endGame<0) receiver.drawScore(engine, 1, 12, "%3d".format(nextSecLv), NUM)

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
		if(engine.ending==2&&endGame>0&&!rollStarted) rollStarted = true
		return false
	}

	override fun onCustom(engine:GameEngine):Boolean {
		if(engine.ending==0||endGame<=0) {
			engine.gameEnded()
			engine.staffrollEnable = false
			engine.ending = 1
			engine.stat = GameEngine.Status.ENDINGSTART
			engine.timerActive = false
			secretGrade = engine.field.secretGrade
			return true
		}

		if(engine.tempHanabi<=0&&engine.statc[1]<=0) {
			engine.statc[0]++
			if(engine.statc[0]==240) owner.musMan.bgm = BGM.Ending(3)
			if(engine.statc[0]>=400) {
				engine.statc[0] = 0
				engine.statc[1] = 1
			}
		} else if(engine.statc[1]==1) {
			if(engine.statc[0]>=1200) {
				engine.field.filterBlocks {b, _, _ -> b.hard!=0||b.getAttribute(ATTRIBUTE.IGNORE_LINK)}.forEach {(b) ->
					b.setAttribute(false, ATTRIBUTE.IGNORE_LINK)
					b.hard = 0
				}
				engine.resetStatc()
				engine.stat = GameEngine.Status.READY
				engine.statc[0] = engine.readyStart
				rollStarted = true
				return true
			}

		}
		return false
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

		if(li>=1) if(engine.ending==0||(engine.ending==2&&endGame>0)) {
			token.apply(engine.statistics)
			if(isFieldFrozen) token.frozens += engine.lastLinesY.flatten().intersect(engine.field.lockedLines).size
			// Level up
			val levelb = engine.statistics.level
			val section = levelb/100
			if(section>=13&&li>=4) engine.field.delLine(engine.field.bottomY)

			levelUp(engine, li.let {it+maxOf(0, it-2)+token.lvb})
			if(engine.statistics.level>=sectionMax*100) {
				// Ending
				engine.playSE("endingstart")
				engine.owner.musMan.bgm = BGM.Silent
				engine.statistics.level = sectionMax*100
				if(engine.ending==0) {
					engine.statistics.rollClear = 1
					if(engine.statistics.time>=ENDGAME_QUOTA||true) {
						//GM Torikan
						engine.gameEnded()
						engine.staffrollEnable = false
						engine.ending = 1
						engine.timerActive = false
						secretGrade = engine.field.secretGrade
					} else {
						engine.ending = 2
						engine.timerActive = false
						engine.stat = GameEngine.Status.CUSTOM
						endGame = levelb
						nextSecLv = sectionMax*100
						//TODO: Rewind History in Engine.Field
					}
					engine.tempHanabi += 10
					sectionsDone++
					decTemp++
					// ST medal
					stMedalCheck(engine, section)

					// 段位上昇
					grade = 2
					gradeFlash = 180
				} else {
					// ST medal
					stMedalCheck(engine, sectionMax)
					engine.statistics.rollClear = 2
					engine.gameEnded()
					engine.resetStatc()
					engine.ending = 1
					engine.timerActive = false
					engine.playSE("grade4")
					grade = 3
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

		// Section Time増加
		if(engine.gameActive&&engine.timerActive&&engine.stat!=GameEngine.Status.CUSTOM) {
			val section = engine.statistics.level/100

			if(section>=0&&section<sectionTime.size) sectionTime[section] =
				engine.statistics.time-sectionTime.take(section).sum()

			if(section>=13) {
				engine.field.filterBlocks {it, _, _ ->
					it.getAttribute(ATTRIBUTE.IGNORE_LINK)&&!it.getAttribute(ATTRIBUTE.ERASE)
						&&it.elapsedFrames>=token.frz&&it.hard!=-1
				}.forEach {(b) ->
					b.hard = -1
					b.darkness = -.5f
					b.color?.let {b.secondaryColor = it}
				}
			}
		}

		// Ending
		if(engine.gameActive&&engine.stat!=GameEngine.Status.CUSTOM&&rollStarted) {
			rollTime++

			// Time meter
			val remainRollTime = ROLLTIMELIMIT-rollTime
			engine.meterValue = remainRollTime*1f/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_LIMIT

			// Roll 終了
			if(rollTime>=ROLLTIMELIMIT) {
				secretGrade = engine.field.secretGrade
				engine.statistics.rollClear = 1
				engine.lives = -1
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.GAMEOVER
			}
		} else if(engine.statistics.level==nextSecLv-1)
			engine.meterColor = if(engine.meterColor==-0x1) -0x10000 else -0x1
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
				receiver.drawMenu(engine, 0, 1.66f, tableGradeName[grade], GRADE, gcolor, 2f)

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

				for(i in sectionTime.indices)
					if(sectionTime[i]>0)
						receiver.drawMenu(engine, 2, 3+i, sectionTime[i].toTimeStr, BASE, sectionIsNewRecord[i])

				if(sectionAvgTime>0) {
					receiver.drawMenu(engine, 0, 16, "AVERAGE", BASE, COLOR.RED)
					receiver.drawMenu(engine, 2, 17, sectionAvgTime.toTimeStr, BASE)
				}
			}
			2 -> {

				receiver.drawMenuMedal(engine, 8, 1, "AC", medalAC)
				receiver.drawMenuMedal(engine, 2, 2, "SK", medalSK)
				receiver.drawMenuMedal(engine, 5, 2, "TS", medalTS)
				receiver.drawMenuMedal(engine, 2, 2, "FR", token.frozens*4/42)
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
			engine.statc[1]--
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
			isShowBestSectionTime = !isShowBestSectionTime
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

		/** Default torikan time for ending */
		private const val ENDGAME_QUOTA = 30000
		/** Ending time limit */
		private const val ROLLTIMELIMIT = 4000

		private val tableFrozenSection = listOf(3, 5, 7, 8, 9)

		private val tableLevel =
			LevelData(
				listOf(12, 12, 12, 11, 10, 9, 8, 7, 7, 6, 12),
				listOf(+8, +7, +6, +6, +6, 5, 5, 5, 5, 5, 10),
				listOf(+6, +5, +4, +4, +4, 3),
				listOf(30, 28, 26, 25, 25, 25, 25, 24, 23, 22,
					//10,11,12, 13-16
					28, 27, 26, 25, 22),
				listOf(12, 12, 11, 10, 10, 9, 9, 9, 8, 8,
					8, 8, 8, 8, 4))

		/** BGM change levels */
		private val tableBGMChange = listOf(300, 700, 1000, 1300)
		/** BGM fadeout levels */
		private val tableBGMFadeout = tableBGMChange.map {it-15}
		/** 段位のName */
		private val tableGradeName = listOf("", "Master", "GrandMaster", "GrandMaster")

		/** 裏段位のName */
		private val tableSecretGradeName =
			listOf("S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9",
				"GM")

	}
}
