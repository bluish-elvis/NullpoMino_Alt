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

import mu.nu.nullpo.game.component.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import kotlin.math.floor
import kotlin.math.ln

/** GARBAGE MANIA Mode */
class GrandM2G:AbstractGrand() {
	/** Roll 経過 time */
	private var rollTime = 0

	/** 裏段位 */
	private var secretGrade = 0

	/** Current BGM */
	private var bgmLv = 0

	/** せり上がりパターン number */
	private var garbagePos = 0

	/** せり上がり usage counter (Linesを消さないと+1) */
	private var garbageCount = 0

	/** せり上がりした count */
	private var garbageTotal = 0

	/** Section Time記録表示中ならtrue */
	private var isShowBestSectionTime = false

	/** せり上がりパターンの種類 (0=RANDOM 1=TGM+ 2=TA SHIRASE) */
	private var goalType = 0

	/** Level at start */
	private var startLevel = 0

	/** When true, always 20G */
	private var always20g = false

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	/** Version */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Rankings' level */
	private val rankingLevel:List<MutableList<Int>> = List(rankingMax) {MutableList(GOALTYPE_MAX) {0}}

	/** Rankings' times */
	private val rankingTime:List<MutableList<Int>> = List(rankingMax) {MutableList(GOALTYPE_MAX) {-1}}

	/** Section Time記録 */
	private val bestSectionTime:List<MutableList<Int>> = List(sectionMax) {MutableList(GOALTYPE_MAX) {DEFAULT_SECTION_TIME}}

	/* Mode name */
	override val name = "Grand Mountain"
	override val gameIntensity = 1
	override val propRank
		get() = rankMapOf(
			rankingLevel.mapIndexed {a, x -> "$a.lines" to x}+
				rankingTime.mapIndexed {a, x -> "$a.time" to x}+
				bestSectionTime.mapIndexed {a, x -> "$a.section.time" to x})

	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		goalType = 0
		rollTime = 0
		secretGrade = 0
		bgmLv = 0
		garbagePos = 0
		garbageCount = 0
		garbageTotal = 0

		rankingRank = -1
		rankingLevel.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(-1)}
		bestSectionTime.forEach {it.fill(-1)}

		engine.speed.are = 23
		engine.speed.areLine = 23
		engine.speed.lineDelay = 40
		engine.speed.lockDelay = 31
		engine.speed.das = 15

		engine.twistEnable = false
		engine.b2bEnable = false
		engine.splitB2B = false
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.bigHalf = true
		engine.bigMove = true
		engine.staffrollEnable = true
		engine.staffrollNoDeath = true

		version = if(!owner.replayMode) CURRENT_VERSION
		else owner.replayProp.getProperty("garbagemania.version", 0)

		owner.bgMan.bg = startLevel
		setSpeed(engine)
	}

	override fun loadSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		goalType = prop.getProperty("garbagemania.goalType", GOALTYPE_PATTERN)
		startLevel = prop.getProperty("garbagemania.startLevel", 0)
		alwaysGhost = prop.getProperty("garbagemania.alwaysghost", true)
		always20g = prop.getProperty("garbagemania.always20g", false)
		secAlert = prop.getProperty("garbagemania.lvstopse", true)
		showST = prop.getProperty("garbagemania.showsectiontime", true)
		big = prop.getProperty("garbagemania.big", false)
	}

	override fun saveSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("garbagemania.goalType", goalType)
		prop.setProperty("garbagemania.startLevel", startLevel)
		prop.setProperty("garbagemania.alwaysghost", alwaysGhost)
		prop.setProperty("garbagemania.always20g", always20g)
		prop.setProperty("garbagemania.lvstopse", secAlert)
		prop.setProperty("garbagemania.showsectiontime", showST)
		prop.setProperty("garbagemania.big", big)
	}

	/** Set BGM at start of game
	 * @param engine GameEngine
	 */
	private fun setStartBgmlv(engine:GameEngine) {
		bgmLv = 0
		while(tableBGMChange[bgmLv]!=-1&&engine.statistics.level>=tableBGMChange[bgmLv])
			bgmLv++
	}

	/** Update falling speed
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = if(always20g) -1
		else tableGravityValue[tableGravityChangeLevel.indexOfFirst {engine.statistics.level<it}
			.let {if(it<0) tableGravityChangeLevel.size-1 else it}]
	}

	/** Section Time更新処理
	 * @param section Section number
	 */
	private fun stNewRecordCheck(engine:GameEngine, section:Int, goalType:Int) =
		stMedalCheck(engine, section, sectionTime[section], bestSectionTime[section][goalType])

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		if(!owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 6)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						goalType += change
						if(goalType<0) goalType = GOALTYPE_MAX-1
						if(goalType>GOALTYPE_MAX-1) goalType = 0
					}
					1 -> {
						startLevel += change
						if(startLevel<0) startLevel = 9
						if(startLevel>9) startLevel = 0
						owner.bgMan.bg = startLevel
					}
					2 -> alwaysGhost = !alwaysGhost
					3 -> always20g = !always20g
					4 -> secAlert = !secAlert
					5 -> showST = !showST
					6 -> big = !big
				}
			}

			//  section time display切替
			if(engine.ctrl.isPush(Controller.BUTTON_F)) {
				engine.playSE("change")
				isShowBestSectionTime = !isShowBestSectionTime
			}

			// 決定
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")
				isShowBestSectionTime = false
				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitFlag = true
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}

		return true
	}

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine) {
		drawMenu(
			engine, receiver, 0, COLOR.BLUE, 0,
			"PATTERN" to when(goalType) {
				GOALTYPE_RANDOM -> "RANDOM"
				GOALTYPE_PATTERN -> "PATTERN"
				else -> "COPY"
			},
			"Level" to (startLevel*100),
			"FULL GHOST" to alwaysGhost,
			"FULL 20G" to always20g, "LVSTOPSE" to secAlert, "SHOW STIME" to showST, "BIG" to big,
		)
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		engine.statistics.level = startLevel*100
		nextSecLv = (engine.statistics.level+100).coerceIn(100, 999)
		owner.bgMan.bg = engine.statistics.level/100
		garbageCount = 13-engine.statistics.level/100
		engine.big = big
		if(goalType==GOALTYPE_RANDOM)
			garbagePos = if(big)
				engine.random.nextInt(engine.field.width/2)
			else
				engine.random.nextInt(engine.field.width)
		setSpeed(engine)
		setStartBgmlv(engine)
		owner.musMan.bgm = BGM.values[bgmLv]
	}

	override fun renderFirst(engine:GameEngine) {
		if(engine.gameActive&&engine.ending==2) receiver.drawStaffRoll(engine, rollTime*1f/ROLLTIMELIMIT)
	}
	/* Render score */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.CYAN)
		receiver.drawScoreFont(engine, -1, -4*2, "DECORATION", scale = .5f)
		receiver.drawScoreBadges(engine, 0, -3, 100, decoration)
		receiver.drawScoreBadges(engine, 5, -4, 100, decTemp)
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startLevel==0&&!big&&!always20g
				&&engine.ai==null
			)
				if(!isShowBestSectionTime) {
					// Rankings
					receiver.drawScoreFont(engine, 3, 2, "LEVEL TIME", COLOR.BLUE)

					for(i in 0..<rankingMax) {
						receiver.drawScoreGrade(
							engine, 0, 3+i, "%2d".format(i+1),
							if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW
						)
						receiver.drawScoreNum(engine, 3, 3+i, "${rankingLevel[i][goalType]}", i==rankingRank)
						receiver.drawScoreNum(engine, 9, 3+i, rankingTime[i][goalType].toTimeStr, i==rankingRank)
					}

					receiver.drawScoreFont(engine, 0, 17, "F:VIEW SECTION TIME", COLOR.GREEN)
				} else {
					// Section Time
					receiver.drawScoreFont(engine, 0, 2, "SECTION TIME", COLOR.BLUE)

					val totalTime = (0..<sectionMax).fold(0) {tt, i ->
						val slv = minOf(i*100, 999)
						receiver.drawScoreNum(
							engine, 0, 3+i, "%3d-%3d %s".format(slv, slv+99, bestSectionTime[i][goalType].toTimeStr),
							sectionIsNewRecord[i]
						)
						tt+bestSectionTime[i][goalType]
					}

					receiver.drawScoreFont(engine, 0, 14, "TOTAL", COLOR.BLUE)
					receiver.drawScoreNum(engine, 0, 15, totalTime.toTimeStr)
					receiver.drawScoreFont(engine, 9, 14, "AVERAGE", COLOR.BLUE)
					receiver.drawScoreNum(engine, 9, 15, (totalTime/sectionMax).toTimeStr)

					receiver.drawScoreFont(engine, 0, 17, "F:VIEW RANKING", COLOR.GREEN)
				}
		} else {
			val g20 = engine.speed.gravity<0
			receiver.drawScoreFont(
				engine, 0, 2, "GARBAGE\n${
					when(goalType) {
						GOALTYPE_COPY -> "";GOALTYPE_PATTERN -> "#$garbagePos";else -> "NEXT: $garbagePos"
					}
				}", COLOR.BLUE
			)
			receiver.drawScoreFont(engine, 0, 4, "LEFT:$garbageCount")

			// Score
			val headCol = if(g20) COLOR.CYAN else COLOR.BLUE
			receiver.drawScoreFont(engine, 0, 6, "Score", headCol)
			receiver.drawScoreNum(engine, 5, 6, "+$lastScore", g20)
			receiver.drawScoreNum(engine, 0, 7, "$scDisp", g20, 2f)

			// level
			receiver.drawScoreFont(engine, 0, 9, "Level", headCol)

			receiver.drawScoreNum(engine, 1, 10, "%3d".format(maxOf(engine.statistics.level, 0)), g20)
			receiver.drawScoreSpeed(
				engine, 0, 11, if(g20) 40 else floor(ln(engine.speed.gravity.toDouble())).toInt()*4,
				4
			)
			receiver.drawScoreNum(engine, 1, 12, "%3d".format(nextSecLv), g20)

			// Time
			receiver.drawScoreFont(engine, 0, 14, "Time", headCol)
			if(engine.ending!=2||rollTime/20%2==0)
				receiver.drawScoreNum(engine, 0, 15, engine.statistics.time.toTimeStr, g20, 2f)

			// Roll 残り time
			if(engine.gameActive&&engine.ending==2) {
				val time = maxOf(0, ROLLTIMELIMIT-rollTime)
				receiver.drawScoreFont(engine, 0, 17, "ROLL TIME", COLOR.BLUE)
				receiver.drawScoreNum(engine, 0, 18, time.toTimeStr, time>0&&time<10*60, 2f)
			}

			// Section Time
			if(showST&&sectionTime.isNotEmpty()) {
				val x = if(receiver.nextDisplayType==2) 8 else 12
				val x2 = if(receiver.nextDisplayType==2) 9 else 12

				receiver.drawScoreFont(engine, x, 2, "SECTION TIME", COLOR.BLUE)
				val section = engine.statistics.level/100
				sectionTime.forEachIndexed {i, it ->
					if(it>0) {
						receiver.drawScoreNum(
							engine, x, 3+i, "%3d%s%s".format(
								minOf(i*100, 999), if(i==section&&engine.ending==0) "+" else "-", it.toTimeStr
							),
							sectionIsNewRecord[i]
						)
					}
				}

				if(sectionAvgTime>0) {
					receiver.drawScoreFont(engine, x2, 14, "AVERAGE", COLOR.BLUE)
					receiver.drawScoreNum(engine, x2, 15, sectionAvgTime.toTimeStr, 2f)
				}
			}
		}
	}

	/** levelが上がったときの共通処理 */
	override fun levelUp(engine:GameEngine, lu:Int) {
		super.levelUp(engine, lu)
		// BGM fadeout
		if(lu>0&&tableBGMFadeout[bgmLv]!=-1&&engine.statistics.level>=tableBGMFadeout[bgmLv]) owner.musMan.fadeSW = true
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Combo
		val li = ev.lines
		// Calculate score
		val pts = super.calcScore(
			engine,
			ScoreEvent(ev.piece, ev.lines+engine.field.howManyGarbageLineClears, ev.b2b, ev.combo, ev.twistType, ev.split)
		)
		if(li==0) {
			// せり上がり
			garbageCount--

			if(garbageCount<=0) {
				engine.playSE("garbage0")

				val field = engine.field
				val w = field.width
				val h = field.height
				if(big&&version>=3) {
					when(goalType) {
						GOALTYPE_RANDOM -> {
							field.pushUp(2)
							for(x in 0..<w/2)
								if(x!=garbagePos)
									for(j in 0..1)
										for(k in 0..1)
											field.setBlock(
												x*2+k, h-1-j,
												Block(Block.COLOR.WHITE, engine.blkSkin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE)
											)

							//int prevHole=garbagePos;do
							garbagePos = engine.random.nextInt(w/2)
						}
						GOALTYPE_PATTERN -> {
							field.pushUp(2)
							for(i in tableGarbagePatternBig[garbagePos].indices)
								if(tableGarbagePatternBig[garbagePos][i]!=0)
									for(j in 0..1)
										for(k in 0..1)
											field.setBlock(
												i*2+k, h-1-j,
												Block(Block.COLOR.WHITE, engine.blkSkin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE)
											)
							garbagePos++
							field.addBottomCopyGarbage(
								engine.blkSkin, 2, Block.ATTRIBUTE.GARBAGE, Block.ATTRIBUTE.VISIBLE,
								Block.ATTRIBUTE.OUTLINE
							)
						}
						GOALTYPE_COPY -> field.addBottomCopyGarbage(
							engine.blkSkin, 2, Block.ATTRIBUTE.GARBAGE,
							Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE
						)
					}//					while(garbagePos==prevHole);
					// Set connections
					if(receiver.isStickySkin(engine))
						for(y in 1..1)
							for(x in 0..<w)
								if(x!=garbagePos) field.getBlock(x, h-y)?.run {
									if(!field.getBlockEmpty(x-1, h-y))
										setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT)
									if(!field.getBlockEmpty(x+1, h-y))
										setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT)
								}
				} else {
					when(goalType) {
						GOALTYPE_RANDOM -> {
							field.pushUp()
							for(x in 0..<w)
								if(x!=garbagePos)
									field.setBlock(
										x, h-1,
										Block(Block.COLOR.WHITE, engine.blkSkin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE)
									)
							// Set connections
							if(receiver.isStickySkin(engine))
								for(x in 0..<w)
									if(x!=garbagePos) field.getBlock(x, h-1)?.run {
										if(!field.getBlockEmpty(x-1, h-1))
											setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT)
										if(!field.getBlockEmpty(x+1, h-1))
											setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT)
									}
							//int prevHole=garbagePos;do
							garbagePos = engine.random.nextInt(w)
						}
						GOALTYPE_PATTERN -> {
							field.pushUp()
							for(i in tableGarbagePattern[garbagePos].indices)
								if(tableGarbagePattern[garbagePos][i]!=0)
									field.setBlock(
										i, h-1,
										Block(Block.COLOR.WHITE, engine.blkSkin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE)
									)
							garbagePos++
						}
						GOALTYPE_COPY -> field.addBottomCopyGarbage(
							engine.blkSkin, 1,
							Block.ATTRIBUTE.GARBAGE, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE
						)
					}//while(garbagePos==prevHole);
					if(receiver.isStickySkin(engine))
						for(x in 0..<w)
							if(x!=garbagePos) field.getBlock(x, h-1)?.run {
								if(!field.getBlockEmpty(x-1, h-1))
									setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT)
								if(!field.getBlockEmpty(x+1, h-1))
									setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT)
							}
				}

				garbageTotal++

				if(garbagePos>tableGarbagePatternBig.size-1&&big&&version>=3)
					garbagePos = 0
				else if(garbagePos>tableGarbagePattern.size-1) garbagePos = 0

				garbageCount = 13-engine.statistics.level/100
			}
		}
		if(li>=1&&engine.ending==0) {
			// Level up
			var ls = li
			ls += engine.field.howManyGarbageLineClears
			levelUp(engine, ls)

			if(engine.statistics.level>=999) {
				// Ending
				engine.playSE("endingstart")
				engine.statistics.level = 999
				engine.timerActive = false
				engine.ending = 2

				sectionsDone++
				stNewRecordCheck(engine, sectionsDone-1, goalType)
			} else if(engine.statistics.level>=nextSecLv) {
				// Next Section
				sectionsDone++
				stNewRecordCheck(engine, sectionsDone-1, goalType)

				// Background切り替え
				owner.bgMan.nextBg = nextSecLv/100

				// BGM切り替え
				if(tableBGMChange[bgmLv]!=-1&&engine.statistics.level>=tableBGMChange[bgmLv]) {
					bgmLv++
					owner.musMan.fadeSW = false
					owner.musMan.bgm = BGM.values[bgmLv]
					engine.playSE("levelup_section")
				}
				engine.playSE("levelup")

				// Update level for next section
				nextSecLv += 100
				if(nextSecLv>999) nextSecLv = 999
			}
			lastScore = pts
			engine.statistics.scoreLine += lastScore
			return lastScore
		}
		return 0
	}

	/* 各 frame の終わりの処理 */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)

		// Section Time増加
		if(engine.timerActive&&engine.ending==0) {
			val section = engine.statistics.level/100

			if(section>=0&&section<sectionTime.size) sectionTime[section] = engine.statistics.time-sectionTime.take(section).sum()
		}

		// Ending
		if(engine.gameActive&&engine.ending==2) {
			rollTime += if(version>=1&&engine.ctrl.isPress(Controller.BUTTON_F))
				5
			else
				1

			// Time meter
			val remainRollTime = ROLLTIMELIMIT-rollTime
			engine.meterValue = remainRollTime*1f/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_LEVEL

			// Roll 終了
			if(rollTime>=ROLLTIMELIMIT) {
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		}
	}

	/* Called at game over */
	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) secretGrade = engine.field.secretGrade

		return false
	}

	/* 結果画面 */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(
			engine,
			0,
			0,
			"${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE${(engine.statc[1]+1)}/3",
			COLOR.RED
		)

		when(engine.statc[1]) {
			0 -> {
				drawResultStats(
					engine, receiver, 2, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_MANIA,
					Statistic.TIME
				)
				drawResult(engine, receiver, 10, COLOR.BLUE, "GARBAGE", "%10d".format(garbageTotal))
				drawResultRank(engine, receiver, 12, COLOR.BLUE, rankingRank)
				if(secretGrade>4)
					drawResult(
						engine, receiver, 14, COLOR.BLUE, "S. GRADE",
						"%10s".format(tableSecretGradeName[secretGrade-1])
					)
			}
			1 -> {
				receiver.drawMenuFont(engine, 0, 2, "SECTION", COLOR.BLUE)

				for(i in sectionTime.indices)
					if(sectionTime[i]>0)
						receiver.drawMenuFont(engine, 2, 3+i, sectionTime[i].toTimeStr, sectionIsNewRecord[i])

				if(sectionAvgTime>0) {
					receiver.drawMenuFont(engine, 0, 14, "AVERAGE", COLOR.BLUE)
					receiver.drawMenuFont(engine, 2, 15, sectionAvgTime.toTimeStr)
				}
			}
			2 -> drawResultStats(
				engine, receiver, 1, COLOR.BLUE, Statistic.LPM, Statistic.SPM, Statistic.PIECE,
				Statistic.PPS
			)
		}
	}

	/* 結果画面の処理 */
	override fun onResult(engine:GameEngine):Boolean {
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
		//  section time display切替
		if(engine.ctrl.isPush(Controller.BUTTON_F)) {
			engine.playSE("change")
			isShowBestSectionTime = !isShowBestSectionTime
		}

		return false
	}

	/* リプレイ保存 */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(engine, owner.replayProp)
		owner.replayProp.setProperty("garbagemania.version", version)

		// Update rankings
		if(!owner.replayMode&&startLevel==0&&!always20g&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.level, engine.statistics.time, goalType)
			if(sectionAnyNewRecord) updateBestSectionTime(goalType)

			if(rankingRank!=-1||sectionAnyNewRecord) return true
		}
		return false
	}

	/** Update rankings
	 * @param lv level
	 * @param time Time
	 */
	private fun updateRanking(lv:Int, time:Int, goalType:Int) {
		rankingRank = checkRanking(lv, time, goalType)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in rankingMax-1 downTo rankingRank+1) {
				rankingLevel[i][goalType] = rankingLevel[i-1][goalType]
				rankingTime[i][goalType] = rankingTime[i-1][goalType]
			}

			// Add new data
			rankingLevel[rankingRank][goalType] = lv
			rankingTime[rankingRank][goalType] = time
		}
	}

	/** Calculate ranking position
	 * @param lv level
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(lv:Int, time:Int, goalType:Int):Int {
		for(i in 0..<rankingMax)
			if(lv>rankingLevel[i][goalType]) return i
			else if(lv==rankingLevel[i][goalType]&&time<rankingTime[i][goalType]) return i

		return -1
	}

	/** Update best section time records */
	private fun updateBestSectionTime(goalType:Int) {
		for(i in 0..<sectionMax)
			if(sectionIsNewRecord[i]) bestSectionTime[i][goalType] = sectionTime[i]
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 3

		/** Number of goal type */
		private const val GOALTYPE_MAX = 3

		/** Goal type constants */
		private const val GOALTYPE_RANDOM = 0
		private const val GOALTYPE_PATTERN = 1
		private const val GOALTYPE_COPY = 2

		/** 落下速度 table */
		private val tableGravityValue = listOf(
			4, 6, 8, 10, 12, 16, 32, 48, 64, 80, 96, 112, 128, 144, 4, 32, 64, 96, 128, 160,
			192, 224, 256, 512, 768, 1024, 1280, 1024, 768, -1
		)

		/** 落下速度が変わる level */
		private val tableGravityChangeLevel = listOf(
			30, 35, 40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 170, 200, 220, 230,
			233, 236, 239, 243, 247, 251, 300, 330, 360, 400, 420, 450, 500, 10000
		)

		/** BGM fadeout levels */
		private val tableBGMFadeout = listOf(495, 695, 880, -1)

		/** BGM change levels */
		private val tableBGMChange = listOf(500, 700, 900, -1)

		/** 裏段位のName */
		private val tableSecretGradeName = listOf(
			"9", "8", "7", "6", "5", "4", "3", "2", "1", //  0～ 8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", //  9～17
			"GM" // 18
		)

		/** LV999 roll time */
		private const val ROLLTIMELIMIT = 2024

		/** Number of sections */
		private const val SECTION_MAX = 10

		/** Default section time */
		private const val DEFAULT_SECTION_TIME = 5400

		/** せり上がりパターン */
		private val tableGarbagePattern = listOf(
			listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1),
			listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1), listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1),
			listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1), listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0),
			listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0), listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0),
			listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0), listOf(0, 0, 1, 1, 1, 1, 1, 1, 1, 1),
			listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1), listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1),
			listOf(1, 1, 1, 1, 1, 1, 1, 1, 0, 0), listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0),
			listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0), listOf(1, 1, 0, 1, 1, 1, 1, 1, 1, 1),
			listOf(1, 0, 0, 1, 1, 1, 1, 1, 1, 1), listOf(1, 0, 1, 1, 1, 1, 1, 1, 1, 1),
			listOf(1, 1, 1, 1, 1, 1, 1, 0, 1, 1), listOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1),
			listOf(1, 1, 1, 1, 1, 1, 1, 1, 0, 1), listOf(1, 1, 1, 1, 0, 0, 1, 1, 1, 1),
			listOf(1, 1, 1, 1, 0, 0, 1, 1, 1, 1), listOf(1, 1, 1, 1, 0, 1, 1, 1, 1, 1),
			listOf(1, 1, 1, 0, 0, 0, 1, 1, 1, 1)
		)

		/** BIG用せり上がりパターン */
		private val tableGarbagePatternBig = listOf(
			listOf(0, 1, 1, 1, 1), listOf(0, 1, 1, 1, 1),
			listOf(0, 1, 1, 1, 1), listOf(0, 1, 1, 1, 1), listOf(1, 1, 1, 1, 0), listOf(1, 1, 1, 1, 0),
			listOf(1, 1, 1, 1, 0), listOf(1, 1, 1, 1, 0), listOf(0, 0, 1, 1, 1), listOf(0, 1, 1, 1, 1),
			listOf(0, 1, 1, 1, 1), listOf(1, 1, 1, 0, 0), listOf(1, 1, 1, 1, 0), listOf(1, 1, 1, 1, 0),
			listOf(1, 1, 0, 1, 1), listOf(1, 0, 0, 1, 1), listOf(1, 0, 1, 1, 1), listOf(1, 1, 0, 1, 1),
			listOf(1, 1, 0, 0, 1), listOf(1, 1, 1, 0, 1), listOf(1, 0, 0, 1, 1), listOf(1, 0, 0, 1, 1),
			listOf(1, 1, 0, 1, 1), listOf(1, 0, 0, 0, 1)
		)
	}
}
