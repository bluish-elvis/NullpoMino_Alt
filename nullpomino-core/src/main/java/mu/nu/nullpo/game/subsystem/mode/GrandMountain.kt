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

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import kotlin.math.*

/** GARBAGE MANIA Mode */
class GrandMountain:AbstractMode() {

	/** Current 落下速度の number (tableGravityChangeLevelの levelに到達するたびに1つ増える) */
	private var gravityindex:Int = 0

	/** Next Section の level (これ-1のときに levelストップする) */
	private var nextseclv:Int = 0

	/** Levelが増えた flag */
	private var lvupflag:Boolean = false

	/** Hard dropした段count */
	private var harddropBonus:Int = 0

	/** Combo bonus */
	private var comboValue:Int = 0

	/** Most recent increase in score */
	private var lastscore:Int = 0

	/** 獲得Render scoreがされる残り time */
	private var scgettime:Int = 0

	/** Roll 経過 time */
	private var rolltime:Int = 0

	/** 裏段位 */
	private var secretGrade:Int = 0

	/** Current BGM */
	private var bgmlv:Int = 0

	/** Section Time */
	private var sectionTime:IntArray = IntArray(SECTION_MAX)

	/** 新記録が出たSection はtrue */
	private var sectionIsNewRecord:BooleanArray = BooleanArray(SECTION_MAX)

	/** どこかのSection で新記録を出すとtrue */
	private var sectionAnyNewRecord:Boolean = false

	/** Cleared Section count */
	private var sectionscomp:Int = 0

	/** Average Section Time */
	private var sectionavgtime:Int = 0

	/** せり上がりパターン number */
	private var garbagePos:Int = 0

	/** せり上がり usage counter (Linesを消さないと+1) */
	private var garbageCount:Int = 0

	/** せり上がりした count */
	private var garbageTotal:Int = 0

	/** Section Time記録表示中ならtrue */
	private var isShowBestSectionTime:Boolean = false

	/** せり上がりパターンの種類 (0=RANDOM 1=TGM+ 2=TA SHIRASE) */
	private var goaltype:Int = 0

	/** Level at start */
	private var startlevel:Int = 0

	/** When true, always ghost ON */
	private var alwaysghost:Boolean = false

	/** When true, always 20G */
	private var always20g:Boolean = false

	/** When true, levelstop sound is enabled */
	private var lvstopse:Boolean = false

	/** BigMode */
	private var big:Boolean = false

	/** When true, section time display is enabled */
	private var showsectiontime:Boolean = false

	/** Version */
	private var version:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = 0

	/** Rankings' level */
	private var rankingLevel:Array<IntArray> = Array(RANKING_MAX) {IntArray(GOALTYPE_MAX)}

	/** Rankings' times */
	private var rankingTime:Array<IntArray> = Array(RANKING_MAX) {IntArray(GOALTYPE_MAX)}

	/** Section Time記録 */
	private var bestSectionTime:Array<IntArray> = Array(SECTION_MAX) {IntArray(GOALTYPE_MAX)}

	private val decoration:Int = 0
	private val dectemp:Int = 0

	/* Mode name */
	override val name:String
		get() = "GRAND MOUNTAIN"

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		goaltype = 0
		gravityindex = 0
		nextseclv = 0
		lvupflag = true
		harddropBonus = 0
		comboValue = 0
		lastscore = 0
		scgettime = 0
		rolltime = 0
		secretGrade = 0
		bgmlv = 0
		sectionTime = IntArray(SECTION_MAX)
		sectionIsNewRecord = BooleanArray(SECTION_MAX)
		sectionAnyNewRecord = false
		sectionscomp = 0
		sectionavgtime = 0
		garbagePos = 0
		garbageCount = 0
		garbageTotal = 0
		isShowBestSectionTime = false
		startlevel = 0
		alwaysghost = false
		always20g = false
		lvstopse = false
		big = false

		rankingRank = -1
		rankingLevel = Array(RANKING_MAX) {IntArray(GOALTYPE_MAX)}
		rankingTime = Array(RANKING_MAX) {IntArray(GOALTYPE_MAX)}
		bestSectionTime = Array(SECTION_MAX) {IntArray(GOALTYPE_MAX)}

		engine.speed.are = 23
		engine.speed.areLine = 23
		engine.speed.lineDelay = 40
		engine.speed.lockDelay = 31
		engine.speed.das = 15

		engine.tspinEnable = false
		engine.b2bEnable = false
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.bighalf = true
		engine.bigmove = true
		engine.staffrollEnable = true
		engine.staffrollNoDeath = true

		version = if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.recordProp, engine.ruleopt.strRuleName)
			CURRENT_VERSION
		} else {
			loadSetting(owner.replayProp)
			owner.replayProp.getProperty("garbagemania.version", 0)
		}

		owner.backgroundStatus.bg = startlevel
	}

	/** Load settings from property file
	 * @param prop Property file
	 */
	override fun loadSetting(prop:CustomProperties) {
		goaltype = prop.getProperty("garbagemania.goaltype", GOALTYPE_PATTERN)
		startlevel = prop.getProperty("garbagemania.startlevel", 0)
		alwaysghost = prop.getProperty("garbagemania.alwaysghost", true)
		always20g = prop.getProperty("garbagemania.always20g", false)
		lvstopse = prop.getProperty("garbagemania.lvstopse", true)
		showsectiontime = prop.getProperty("garbagemania.showsectiontime", true)
		big = prop.getProperty("garbagemania.big", false)
	}

	/** Save settings to property file
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("garbagemania.goaltype", goaltype)
		prop.setProperty("garbagemania.startlevel", startlevel)
		prop.setProperty("garbagemania.alwaysghost", alwaysghost)
		prop.setProperty("garbagemania.always20g", always20g)
		prop.setProperty("garbagemania.lvstopse", lvstopse)
		prop.setProperty("garbagemania.showsectiontime", showsectiontime)
		prop.setProperty("garbagemania.big", big)
	}

	/** Set BGM at start of game
	 * @param engine GameEngine
	 */
	private fun setStartBgmlv(engine:GameEngine) {
		bgmlv = 0
		while(tableBGMChange[bgmlv]!=-1&&engine.statistics.level>=tableBGMChange[bgmlv])
			bgmlv++
	}

	/** Update falling speed
	 * @param engine GameEngine
	 */
	private fun setSpeed(engine:GameEngine) {
		if(always20g)
			engine.speed.gravity = -1
		else {
			while(engine.statistics.level>=tableGravityChangeLevel[gravityindex])
				gravityindex++
			engine.speed.gravity = tableGravityValue[gravityindex]
		}
	}

	/** Update average section time */
	private fun setAverageSectionTime() {
		if(sectionscomp>0) {
			var temp = 0
			for(i in startlevel until startlevel+sectionscomp)
				if(i>=0&&i<sectionTime.size) temp += sectionTime[i]

			sectionavgtime = temp/sectionscomp
		} else
			sectionavgtime = 0

	}

	/** Section Time更新処理
	 * @param sectionNumber Section number
	 */
	private fun stNewRecordCheck(sectionNumber:Int, goaltype:Int) {
		if(sectionTime[sectionNumber]<bestSectionTime[sectionNumber][goaltype]&&!owner.replayMode) {
			sectionIsNewRecord[sectionNumber] = true
			sectionAnyNewRecord = true
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 6)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {

					0 -> {
						goaltype += change
						if(goaltype<0) goaltype = GOALTYPE_MAX-1
						if(goaltype>GOALTYPE_MAX-1) goaltype = 0
					}
					1 -> {
						startlevel += change
						if(startlevel<0) startlevel = 9
						if(startlevel>9) startlevel = 0
						owner.backgroundStatus.bg = startlevel
					}
					2 -> alwaysghost = !alwaysghost
					3 -> always20g = !always20g
					4 -> lvstopse = !lvstopse
					5 -> showsectiontime = !showsectiontime
					6 -> big = !big
				}
			}

			//  section time display切替
			if(engine.ctrl!!.isPush(Controller.BUTTON_F)&&menuTime>=5) {
				engine.playSE("change")
				isShowBestSectionTime = !isShowBestSectionTime
			}

			// 決定
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig)
				owner.saveModeConfig()
				isShowBestSectionTime = false
				sectionscomp = 0
				return false
			}

			// Cancel
			if(engine.ctrl!!.isPush(Controller.BUTTON_B)) engine.quitflag = true

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}

		return true
	}

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0, "PATTERN", if(goaltype==GOALTYPE_RANDOM)
			"RANDOM"
		else if(goaltype==GOALTYPE_PATTERN) "PATTERN" else "COPY", "LEVEL", (startlevel*100).toString(), "FULL GHOST", GeneralUtil.getONorOFF(alwaysghost), "20G MODE", GeneralUtil.getONorOFF(always20g), "LVSTOPSE", GeneralUtil.getONorOFF(lvstopse), "SHOW STIME", GeneralUtil.getONorOFF(showsectiontime), "BIG", GeneralUtil.getONorOFF(big))
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel*100

		nextseclv = engine.statistics.level+100
		if(engine.statistics.level<0) nextseclv = 100
		if(engine.statistics.level>=900) nextseclv = 999

		owner.backgroundStatus.bg = engine.statistics.level/100

		garbageCount = 13-engine.statistics.level/100
		engine.big = big
		if(goaltype==GOALTYPE_RANDOM)
			garbagePos = if(big)
				engine.random.nextInt(engine.field!!.width/2)
			else
				engine.random.nextInt(engine.field!!.width)
		setSpeed(engine)
		setStartBgmlv(engine)
		owner.bgmStatus.bgm = BGM.values[bgmlv]
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "GARBAGE MANIA", EventReceiver.COLOR.CYAN)
		receiver.drawScoreFont(engine, playerID, -1, -4*2, "DECORATION", scale = .5f)
		receiver.drawScoreBadges(engine, playerID, 0, -3, 100, decoration)
		receiver.drawScoreBadges(engine, playerID, 5, -4, 100, dectemp)
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startlevel==0&&!big&&!always20g
				&&engine.ai==null)
				if(!isShowBestSectionTime) {
					// Rankings
					receiver.drawScoreFont(engine, playerID, 3, 2, "LEVEL TIME", EventReceiver.COLOR.BLUE)

					for(i in 0 until RANKING_MAX) {
						receiver.drawScoreGrade(engine, playerID, 0, 3+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW)
						receiver.drawScoreNum(engine, playerID, 3, 3+i, "${rankingLevel[i][goaltype]}", i==rankingRank)
						receiver.drawScoreNum(engine, playerID, 9, 3+i, GeneralUtil.getTime(rankingTime[i][goaltype]), i==rankingRank)
					}

					receiver.drawScoreFont(engine, playerID, 0, 17, "F:VIEW SECTION TIME", EventReceiver.COLOR.GREEN)
				} else {
					// Section Time
					receiver.drawScoreFont(engine, playerID, 0, 2, "SECTION TIME", EventReceiver.COLOR.BLUE)

					var totalTime = 0
					for(i in 0 until SECTION_MAX) {
						val temp = minOf(i*100, 999)
						val temp2 = minOf((i+1)*100-1, 999)

						val strSectionTime:String
						strSectionTime = String.format("%3d-%3d %s", temp, temp2, GeneralUtil.getTime(bestSectionTime[i][goaltype]))

						receiver.drawScoreNum(engine, playerID, 0, 3+i, strSectionTime, sectionIsNewRecord[i])

						totalTime += bestSectionTime[i][goaltype]
					}

					receiver.drawScoreFont(engine, playerID, 0, 14, "TOTAL", EventReceiver.COLOR.BLUE)
					receiver.drawScoreNum(engine, playerID, 0, 15, GeneralUtil.getTime(totalTime))
					receiver.drawScoreFont(engine, playerID, 9, 14, "AVERAGE", EventReceiver.COLOR.BLUE)
					receiver.drawScoreNum(engine, playerID, 9, 15, GeneralUtil.getTime((totalTime/SECTION_MAX)))

					receiver.drawScoreFont(engine, playerID, 0, 17, "F:VIEW RANKING", EventReceiver.COLOR.GREEN)
				}
		} else {
			val g20 = engine.speed.gravity<0
			val strGarbage:String = if(goaltype==GOALTYPE_COPY) "" else if(goaltype==GOALTYPE_PATTERN) "#$garbagePos" else "NEXT: $garbagePos"
			receiver.drawScoreFont(engine, playerID, 0, 2, "GARBAGE\n$strGarbage", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 4, "LEFT:$garbageCount")

			// Score
			receiver.drawScoreFont(engine, playerID, 0, 6, "SCORE", if(g20)
				EventReceiver.COLOR.CYAN
			else
				EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 6, "+$lastscore", g20)
			receiver.drawScoreNum(engine, playerID, 0, 7, "$scgettime", g20, 2f)
			if(scgettime<engine.statistics.score) scgettime += ceil(((engine.statistics.score-scgettime)/10f).toDouble())
				.toInt()

			// level
			receiver.drawScoreFont(engine, playerID, 0, 9, "LEVEL", if(g20)
				EventReceiver.COLOR.CYAN
			else
				EventReceiver.COLOR.BLUE)

			receiver.drawScoreNum(engine, playerID, 0, 10, String.format("%3d", maxOf(engine.statistics.level, 0)), g20)
			receiver.drawSpeedMeter(engine, playerID, 0, 11, if(g20) 40 else floor(ln(engine.speed.gravity.toDouble())).toInt()*4)
			receiver.drawScoreNum(engine, playerID, 0, 12, String.format("%3d", nextseclv), g20)

			// Time
			receiver.drawScoreFont(engine, playerID, 0, 14, "TIME", if(g20)
				EventReceiver.COLOR.CYAN
			else
				EventReceiver.COLOR.BLUE)
			if(engine.ending!=2||rolltime/20%2==0)
				receiver.drawScoreNum(engine, playerID, 0, 15, GeneralUtil.getTime(engine.statistics.time), g20, 2f)

			// Roll 残り time
			if(engine.gameActive&&engine.ending==2) {
				var time = ROLLTIMELIMIT-rolltime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, playerID, 0, 17, "ROLL TIME", EventReceiver.COLOR.BLUE)
				receiver.drawScoreNum(engine, playerID, 0, 18, GeneralUtil.getTime(time), time>0&&time<10*60, 2f)
			}

			// Section Time
			if(showsectiontime&&sectionTime.isNotEmpty()) {
				val x = if(receiver.nextDisplayType==2) 8 else 12
				val x2 = if(receiver.nextDisplayType==2) 9 else 12

				receiver.drawScoreFont(engine, playerID, x, 2, "SECTION TIME", EventReceiver.COLOR.BLUE)
				for(i in sectionTime.indices)
					if(sectionTime[i]>0) {
						var temp = i*100
						if(temp>999) temp = 999

						val section = engine.statistics.level/100
						var strSeparator = "-"
						if(i==section&&engine.ending==0) strSeparator = "+"

						val strSectionTime:String
						strSectionTime = String.format("%3d%s%s", temp, strSeparator, GeneralUtil.getTime(sectionTime[i]))

						receiver.drawScoreNum(engine, playerID, x, 3+i, strSectionTime, sectionIsNewRecord[i])
					}

				if(sectionavgtime>0) {
					receiver.drawScoreFont(engine, playerID, x2, 14, "AVERAGE", EventReceiver.COLOR.BLUE)
					receiver.drawScoreNum(engine, playerID, x2, 15, GeneralUtil.getTime(sectionavgtime), 2f)
				}
			}
		}
	}

	/* 移動中の処理 */
	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		// 新規ピース出現時
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!lvupflag) {
			// Level up
			if(engine.statistics.level<nextseclv-1) {
				engine.statistics.level++
				if(engine.statistics.level==nextseclv-1&&lvstopse) engine.playSE("levelstop")
			}
			levelUp(engine)

			// Hard drop bonusInitialization
			harddropBonus = 0
		}
		if(engine.ending==0&&engine.statc[0]>0&&(version>=2||!engine.holdDisable)) lvupflag = false

		return false
	}

	/* ARE中の処理 */
	override fun onARE(engine:GameEngine, playerID:Int):Boolean {
		// 最後の frame
		if(engine.ending==0&&engine.statc[0]>=engine.statc[1]-1&&!lvupflag) {
			if(engine.statistics.level<nextseclv-1) {
				engine.statistics.level++
				if(engine.statistics.level==nextseclv-1&&lvstopse) engine.playSE("levelstop")
			}
			levelUp(engine)
			lvupflag = true
		}

		return false
	}

	/** levelが上がったときの共通処理 */
	private fun levelUp(engine:GameEngine) {
		// Meter
		engine.meterValue = engine.statistics.level%100*receiver.getMeterMax(engine)/99
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(engine.statistics.level%100>=50) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(engine.statistics.level%100>=80) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(engine.statistics.level==nextseclv-1) engine.meterColor = GameEngine.METER_COLOR_RED

		// 速度変更
		setSpeed(engine)

		// LV100到達でghost を消す
		if(engine.statistics.level>=100&&!alwaysghost) engine.ghost = false

		// BGM fadeout
		if(tableBGMFadeout[bgmlv]!=-1&&engine.statistics.level>=tableBGMFadeout[bgmlv]) owner.bgmStatus.fadesw = true
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {
		// Combo
		comboValue = if(lines==0) 1
		else maxOf(1,comboValue+2*lines-2)

		if(lines==0) {
			// せり上がり
			garbageCount--

			if(garbageCount<=0) {
				engine.playSE("garbage")

				val field = engine.field
				val w = field!!.width
				val h = field.height
				if(big&&version>=3) {
					when(goaltype) {
						GOALTYPE_RANDOM -> {
							field.pushUp(2)
							for(x in 0 until w/2)
								if(x!=garbagePos)
									for(j in 0..1)
										for(k in 0..1)
											field.setBlock(x*2+k, h-1-j,
												Block(Block.COLOR.WHITE, engine.skin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE))

							//int prevHole=garbagePos;do
							garbagePos = engine.random.nextInt(w/2)
						}
						GOALTYPE_PATTERN -> {
							field.pushUp(2)
							for(i in 0 until tableGarbagePatternBig[garbagePos].size)
								if(tableGarbagePatternBig[garbagePos][i]!=0)
									for(j in 0..1)
										for(k in 0..1)
											field.setBlock(i*2+k, h-1-j,
												Block(Block.COLOR.WHITE, engine.skin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE))
							garbagePos++
							field.addBottomCopyGarbage(engine.skin, 2, Block.ATTRIBUTE.GARBAGE, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
						}
						GOALTYPE_COPY -> field.addBottomCopyGarbage(engine.skin, 2, Block.ATTRIBUTE.GARBAGE,
							Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
					}//					while(garbagePos==prevHole);
					// Set connections
					if(receiver.isStickySkin(engine))
						for(y in 1..1)
							for(x in 0 until w)
								if(x!=garbagePos) field.getBlock(x, h-y)?.run {
									if(!field.getBlockEmpty(x-1, h-y))
										setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT)
									if(!field.getBlockEmpty(x+1, h-y))
										setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT)
								}

				} else {

					when(goaltype) {
						GOALTYPE_RANDOM -> {
							field.pushUp()
							for(x in 0 until w)
								if(x!=garbagePos)
									field.setBlock(x, h-1, Block(Block.COLOR.WHITE, engine.skin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE))
							// Set connections
							if(receiver.isStickySkin(engine))
								for(x in 0 until w)
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
							for(i in 0 until tableGarbagePattern[garbagePos].size)
								if(tableGarbagePattern[garbagePos][i]!=0)
									field.setBlock(i, h-1, Block(Block.COLOR.WHITE, engine.skin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE))
							garbagePos++
						}
						GOALTYPE_COPY -> field.addBottomCopyGarbage(engine.skin, 1,
							Block.ATTRIBUTE.GARBAGE, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
					}//while(garbagePos==prevHole);
					if(receiver.isStickySkin(engine))
						for(x in 0 until w)
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
		if(lines>=1&&engine.ending==0) {
			// Level up
			val levelb = engine.statistics.level
			var ls = lines
			ls += engine.field!!.howManyGarbageLineClears
			engine.statistics.level += ls
			levelUp(engine)

			if(engine.statistics.level>=999) {
				// Ending
				engine.playSE("endingstart")
				engine.statistics.level = 999
				engine.timerActive = false
				engine.ending = 2

				sectionscomp++
				setAverageSectionTime()
				stNewRecordCheck(sectionscomp-1, goaltype)
			} else if(engine.statistics.level>=nextseclv) {
				// Next Section

				sectionscomp++
				setAverageSectionTime()
				stNewRecordCheck(sectionscomp-1, goaltype)

				// Background切り替え
				owner.backgroundStatus.fadesw = true
				owner.backgroundStatus.fadecount = 0
				owner.backgroundStatus.fadebg = nextseclv/100

				// BGM切り替え
				if(tableBGMChange[bgmlv]!=-1&&engine.statistics.level>=tableBGMChange[bgmlv]) {
					bgmlv++
					owner.bgmStatus.fadesw = false
					owner.bgmStatus.bgm = BGM.values[bgmlv]
					engine.playSE("levelup_section")
				}else engine.playSE("levelup")

				// Update level for next section
				nextseclv += 100
				if(nextseclv>999) nextseclv = 999
			} else if(engine.statistics.level==nextseclv-1&&lvstopse) engine.playSE("levelstop")

			// Calculate score
			var bravo = 1
			if(engine.field!!.isEmpty) bravo = 4

			lastscore = ((levelb+ls)/4+engine.softdropFall+(if(engine.manualLock) 1 else 0)+harddropBonus)*ls*comboValue*bravo+
				engine.statistics.level/2+maxOf(0, engine.lockDelay-engine.lockDelayNow)*7
			engine.statistics.scoreLine += lastscore

		}
	}

	/* Called when hard drop used */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		if(fall*2>harddropBonus) harddropBonus = fall*2
	}

	/* 各 frame の終わりの処理 */
	override fun onLast(engine:GameEngine, playerID:Int) {
		// 獲得Render score
		if(scgettime>0) scgettime--

		// Section Time増加
		if(engine.timerActive&&engine.ending==0) {
			val section = engine.statistics.level/100

			if(section>=0&&section<sectionTime.size) sectionTime[section]++

		}

		// Ending
		if(engine.gameActive&&engine.ending==2) {
			rolltime += if(version>=1&&engine.ctrl!!.isPress(Controller.BUTTON_F))
				5
			else
				1

			// Time meter
			val remainRollTime = ROLLTIMELIMIT-rolltime
			engine.meterValue = remainRollTime*receiver.getMeterMax(engine)/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainRollTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainRollTime<=20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainRollTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED

			// Roll 終了
			if(rolltime>=ROLLTIMELIMIT) {
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		}
	}

	/* Called at game over */
	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) secretGrade = engine.field!!.secretGrade

		return false
	}

	/* 結果画面 */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 0, "kn PAGE${(engine.statc[1]+1)}/3", EventReceiver.COLOR.RED)

		when(engine.statc[1]) {
			0 -> {
				drawResultStats(engine, playerID, receiver, 2, EventReceiver.COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_MANIA, Statistic.TIME)
				drawResult(engine, playerID, receiver, 10, EventReceiver.COLOR.BLUE, "GARBAGE", String.format("%10d", garbageTotal))
				drawResultRank(engine, playerID, receiver, 12, EventReceiver.COLOR.BLUE, rankingRank)
				if(secretGrade>4)
					drawResult(engine, playerID, receiver, 14, EventReceiver.COLOR.BLUE, "S. GRADE", String.format("%10s", tableSecretGradeName[secretGrade-1]))
			}
			1 -> {
				receiver.drawMenuFont(engine, playerID, 0, 2, "SECTION", EventReceiver.COLOR.BLUE)

				for(i in sectionTime.indices)
					if(sectionTime[i]>0)
						receiver.drawMenuFont(engine, playerID, 2, 3+i, GeneralUtil.getTime(sectionTime[i]), sectionIsNewRecord[i])

				if(sectionavgtime>0) {
					receiver.drawMenuFont(engine, playerID, 0, 14, "AVERAGE", EventReceiver.COLOR.BLUE)
					receiver.drawMenuFont(engine, playerID, 2, 15, GeneralUtil.getTime(sectionavgtime))
				}
			}
			2 -> drawResultStats(engine, playerID, receiver, 1, EventReceiver.COLOR.BLUE, Statistic.LPM, Statistic.SPM, Statistic.PIECE, Statistic.PPS)
		}

	}

	/* 結果画面の処理 */
	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		// ページ切り替え
		if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_UP)) {
			engine.statc[1]--
			if(engine.statc[1]<0) engine.statc[1] = 2
			engine.playSE("change")
		}
		if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
			engine.statc[1]++
			if(engine.statc[1]>2) engine.statc[1] = 0
			engine.playSE("change")
		}
		//  section time display切替
		if(engine.ctrl!!.isPush(Controller.BUTTON_F)) {
			engine.playSE("change")
			isShowBestSectionTime = !isShowBestSectionTime
		}

		return false
	}

	/* リプレイ保存 */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(owner.replayProp)
		owner.replayProp.setProperty("garbagemania.version", version)

		// Update rankings
		if(!owner.replayMode&&startlevel==0&&!always20g&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.level, engine.statistics.time, goaltype)
			if(sectionAnyNewRecord) updateBestSectionTime(goaltype)

			if(rankingRank!=-1||sectionAnyNewRecord) {
				saveRanking(owner.modeConfig, engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
		}
	}

	/** Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(j in 0 until GOALTYPE_MAX) {
			for(i in 0 until RANKING_MAX) {
				rankingLevel[i][j] = prop.getProperty("garbagemania.ranking.$ruleName.$j.level.$i", 0)
				rankingTime[i][j] = prop.getProperty("garbagemania.ranking.$ruleName.$j.time.$i", 0)
			}
			for(i in 0 until SECTION_MAX)
				bestSectionTime[i][j] = prop.getProperty("garbagemania.bestSectionTime.$ruleName.$j."
					+i, DEFAULT_SECTION_TIME)
		}
	}

	/** Save rankings to property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	fun saveRanking(prop:CustomProperties, ruleName:String) {
		for(j in 0 until GOALTYPE_MAX) {
			for(i in 0 until RANKING_MAX) {
				prop.setProperty("garbagemania.ranking.$ruleName.$j.level.$i", rankingLevel[i][j])
				prop.setProperty("garbagemania.ranking.$ruleName.$j.time.$i", rankingTime[i][j])
			}
			for(i in 0 until SECTION_MAX)
				prop.setProperty("garbagemania.bestSectionTime.$ruleName.$j.$i", bestSectionTime[i][j])
		}
	}

	/** Update rankings
	 * @param lv level
	 * @param time Time
	 */
	private fun updateRanking(lv:Int, time:Int, goaltype:Int) {
		rankingRank = checkRanking(lv, time, goaltype)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingLevel[i][goaltype] = rankingLevel[i-1][goaltype]
				rankingTime[i][goaltype] = rankingTime[i-1][goaltype]
			}

			// Add new data
			rankingLevel[rankingRank][goaltype] = lv
			rankingTime[rankingRank][goaltype] = time
		}
	}

	/** Calculate ranking position
	 * @param lv level
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(lv:Int, time:Int, goaltype:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(lv>rankingLevel[i][goaltype])
				return i
			else if(lv==rankingLevel[i][goaltype]&&time<rankingTime[i][goaltype]) return i

		return -1
	}

	/** Update best section time records */
	private fun updateBestSectionTime(goaltype:Int) {
		for(i in 0 until SECTION_MAX)
			if(sectionIsNewRecord[i]) bestSectionTime[i][goaltype] = sectionTime[i]
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
		private val tableGravityValue = intArrayOf(4, 6, 8, 10, 12, 16, 32, 48, 64, 80, 96, 112, 128, 144, 4, 32, 64, 96, 128, 160, 192, 224, 256, 512, 768, 1024, 1280, 1024, 768, -1)

		/** 落下速度が変わる level */
		private val tableGravityChangeLevel = intArrayOf(30, 35, 40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 170, 200, 220, 230, 233, 236, 239, 243, 247, 251, 300, 330, 360, 400, 420, 450, 500, 10000)

		/** BGM fadeout levels */
		private val tableBGMFadeout = intArrayOf(495, 695, 880, -1)

		/** BGM change levels */
		private val tableBGMChange = intArrayOf(500, 700, 900, -1)

		/** 裏段位のName */
		private val tableSecretGradeName = arrayOf("9", "8", "7", "6", "5", "4", "3", "2", "1", //  0～ 8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", //  9～17
			"GM" // 18
		)

		/** LV999 roll time */
		private const val ROLLTIMELIMIT = 2024

		/** Number of entries in rankings */
		private const val RANKING_MAX = 10

		/** Number of sections */
		private const val SECTION_MAX = 10

		/** Default section time */
		private const val DEFAULT_SECTION_TIME = 5400

		/** せり上がりパターン */
		private val tableGarbagePattern = arrayOf(intArrayOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1), intArrayOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1), intArrayOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1), intArrayOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1), intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0), intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0), intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0), intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0), intArrayOf(0, 0, 1, 1, 1, 1, 1, 1, 1, 1), intArrayOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1), intArrayOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1), intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 0, 0), intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0), intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0), intArrayOf(1, 1, 0, 1, 1, 1, 1, 1, 1, 1), intArrayOf(1, 0, 0, 1, 1, 1, 1, 1, 1, 1), intArrayOf(1, 0, 1, 1, 1, 1, 1, 1, 1, 1), intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 1, 1), intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1), intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 0, 1), intArrayOf(1, 1, 1, 1, 0, 0, 1, 1, 1, 1), intArrayOf(1, 1, 1, 1, 0, 0, 1, 1, 1, 1), intArrayOf(1, 1, 1, 1, 0, 1, 1, 1, 1, 1), intArrayOf(1, 1, 1, 0, 0, 0, 1, 1, 1, 1))

		/** BIG用せり上がりパターン */
		private val tableGarbagePatternBig = arrayOf(intArrayOf(0, 1, 1, 1, 1), intArrayOf(0, 1, 1, 1, 1), intArrayOf(0, 1, 1, 1, 1), intArrayOf(0, 1, 1, 1, 1), intArrayOf(1, 1, 1, 1, 0), intArrayOf(1, 1, 1, 1, 0), intArrayOf(1, 1, 1, 1, 0), intArrayOf(1, 1, 1, 1, 0), intArrayOf(0, 0, 1, 1, 1), intArrayOf(0, 1, 1, 1, 1), intArrayOf(0, 1, 1, 1, 1), intArrayOf(1, 1, 1, 0, 0), intArrayOf(1, 1, 1, 1, 0), intArrayOf(1, 1, 1, 1, 0), intArrayOf(1, 1, 0, 1, 1), intArrayOf(1, 0, 0, 1, 1), intArrayOf(1, 0, 1, 1, 1), intArrayOf(1, 1, 0, 1, 1), intArrayOf(1, 1, 0, 0, 1), intArrayOf(1, 1, 1, 0, 1), intArrayOf(1, 0, 0, 1, 1), intArrayOf(1, 0, 0, 1, 1), intArrayOf(1, 1, 0, 1, 1), intArrayOf(1, 0, 0, 0, 1))
	}
}
