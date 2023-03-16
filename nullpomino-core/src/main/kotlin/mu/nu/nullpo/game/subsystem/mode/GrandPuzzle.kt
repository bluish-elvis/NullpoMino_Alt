/*
 * Copyright (c) 2010-2023, NullNoname
 * Kotlin converted and modified by Venom=Nhelv.
 * THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
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
package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import org.apache.logging.log4j.LogManager
import org.jetbrains.kotlin.konan.file.File

/** GEM MANIA */
class GrandPuzzle:AbstractMode() {
	/** Level set property file */
	private var propStageSet = mapOf<Int, CustomProperties>().toMutableMap()

	/** 残りプラチナBlockcount */
	private var rest = 0

	/** Current level number */
	private var stage = 0

	/** Last level number */
	private var laststage = 0

	/** Attempted stage count */
	private var tryLevels = 0

	/** Cleared stage count */
	private var doneLevel = 0

	/** Clear rate */
	private var clearRate = 0

	/** Stage clear flag */
	private var clearFlag = false

	/** Stage skip flag */
	private var skipFlag = false

	/** Time limit left */
	private var limittimeNow = 0

	/** Time limit at start */
	private var limittimeStart = 0

	/** Stage time left */
	private var stagetimeNow = 0

	/** Stage 開始後の経過 time */
	private var cleartime = 0

	/** Stage time at start */
	private var stagetimeStart = 0

	/** Stage BGM */
	private var stagebgm = 0

	/** Next section level (levelstop when this is -1) */
	private var nextSecLv = 0

	/** Level */
	private var speedlevel = 0

	/** Levelが増えた flag */
	private var lvupFlag = false

	/** Section Time */
	private val sectionTime = MutableList(MAX_STAGE_TOTAL) {0}

	/** Current time limit extension in seconds */
	private var timeextendSeconds = 0

	/** Number of frames to display current time limit extension */
	private var timeextendDisp = 0

	/** Stage clear time limit extension in seconds */
	private var timeextendStageClearSeconds = 0

	/** Blockを置いた count(1面終了でリセット) */
	private var thisStageTotalPieceLockCount = 0

	/** Skip buttonを押している time */
	private var skipbuttonPressTime = 0

	/** Blockピースを置いた count (NEXTピースの計算用）のバックアップ (コンティニュー時に戻す) */
	private var continueNextPieceCount = 0

	/** Set to true when NO is picked at continue screen */
	private var noContinue = false

	/** All clear flag */
	private var allClear = 0

	/** Best time in training mode */
	private var trainingBestTime = 0

	/** ミラー発動間隔 */
	private var gimmickMirror = 0

	/** Roll Roll 発動間隔 */
	private var gimmickRoll = 0

	/** Big発動間隔 */
	private var gimmickBig = 0

	/** X-RAY発動間隔 */
	private var gimmickXRay = 0

	/** カラー発動間隔 */
	private var gimmickColor = 0

	/** Current edit screen */
	private var editModeScreen = 0

	/** Stage at start */
	private var startStage = 0

	/** Selected stage set */
	private var mapSet = 0

	/** When true, always ghost ON */
	private var alwaysGhost = false

	/** When true, always 20G */
	private var always20g = false

	/** When true, levelstop sound is enabled */
	private var secAlert = false

	/** When true, section time display is enabled */
	private var showST = false

	/** NEXTをランダムにする */
	private var randomQueue = false

	/** Training mode */
	private var trainingType = 0

	/** Block counter at start */
	private var startNextc = 0

	/** Version */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Rankings' stage reached */
	private val rankingStage = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}

	/** Rankings' clear ratio */
	private val rankingRate = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}

	/** Rankings' times */
	private val rankingTime = List(RANKING_TYPE) {MutableList(RANKING_MAX) {-1}}

	/** Rankings' all clear flag */
	private val rankingAllClear = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}

	override val rankMap
		get() = rankMapOf(rankingStage.mapIndexed {a, x -> "$a.stage" to x}+
			rankingRate.mapIndexed {a, x -> "$a.rate" to x}+
			rankingAllClear.mapIndexed {a, x -> "$a.clear" to x}+
			rankingTime.mapIndexed {a, x -> "$a.time" to x})

	private var decoration = 0
	private var decTemp = 0

	/** Mode nameを取得 */
	override val name = "Grand Blossom"
	override val gameIntensity:Int = -1

	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		File(levelDir).listFiles

		loadStageSet(-1)

	}
	/** Initialization */
	override fun playerInit(engine:GameEngine) {
		log.debug("playerInit called")

		super.playerInit(engine)
		rest = 0
		stage = 0
		laststage = MAX_STAGE_NORMAL-1
		tryLevels = 0
		doneLevel = 0
		clearRate = 0
		clearFlag = false
		skipFlag = false
		limittimeNow = 0
		limittimeStart = 0
		stagetimeNow = 0
		stagetimeStart = 0
		cleartime = 0

		nextSecLv = 0
		speedlevel = 0
		lvupFlag = false

		sectionTime.fill(0)

		timeextendSeconds = 0
		timeextendDisp = 0
		timeextendStageClearSeconds = 0

		thisStageTotalPieceLockCount = 0
		skipbuttonPressTime = 0

		continueNextPieceCount = 0
		noContinue = false

		allClear = 0

		trainingBestTime = -1

		gimmickMirror = 0
		gimmickRoll = 0
		gimmickBig = 0
		gimmickXRay = 0
		gimmickColor = 0

		editModeScreen = 0


		rankingRank = -1
		rankingStage.forEach {it.fill(0)}
		rankingRate.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(-1)}
		rankingAllClear.forEach {it.fill(0)}

		engine.twistEnable = false
		engine.b2bEnable = false
		engine.splitB2B = false
		engine.frameColor = GameEngine.FRAME_COLOR_PINK
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		engine.bigHalf = true
		engine.bigMove = false
		engine.staffrollEnable = false
		engine.holdButtonNextSkip = true

		engine.fieldWidth = 10
		engine.fieldHeight = 20
		engine.createFieldIfNeeded()
		version = if(!owner.replayMode) CURRENT_VERSION
		else owner.replayProp.getProperty("gemmania.version", 0)



		if(version<=0) {
			engine.readyStart = 45
			engine.readyEnd = 155
			engine.goStart = 160
			engine.goEnd = 225
		}
	}

	/** stage 開始時の処理
	 * @param engine GameEngine
	 */
	private fun startStage(engine:GameEngine) {
		// スピード levelInitialization
		speedlevel = -1
		nextSecLv = 100
		lvupFlag = false
		setSpeed(engine)
		thisStageTotalPieceLockCount = 0
		continueNextPieceCount = engine.nextPieceCount

		// Background戻す
		if(owner.bgMan.bg!=0) {
			owner.bgMan.nextBg = 0
		}

		// ghost 復活
		engine.ghost = true

		// ホールド消去
		engine.holdDisable = false
		engine.holdPieceObject = null

		clearFlag = false
		skipFlag = false

		//  stage Map読み込み
		engine.createFieldIfNeeded()
		propStageSet[mapSet]?.let {loadMap(engine.field, it, stage)}
		engine.field.setAllSkin(engine.skin)

		//  stage Timeなどを設定
		cleartime = 0
		sectionTime[stage] = 0
		stagetimeNow = stagetimeStart
		rest = engine.field.howManyGems

		if(owner.musMan.bgm.id!=stagebgm) owner.musMan.fadeSW = true
	}

	/** stage セットを読み込み
	 * @param id stage セット number(-1で default )
	 */
	private fun loadStageSet(id:Int) {
		if(propStageSet[id]==null) propStageSet[id] = CustomProperties()
		propStageSet[id]?.load(
			if(id>=0) {
				log.debug("Loading stage set from custom set #$id")
				levelDir+"custom$id.map"
			} else {
				log.debug("Loading stage set from default set")
				levelDir+"default.map"
			}
		)
	}

	/** stage セットを保存
	 * @param id stage セット number(-1で default )
	 */
	private fun saveStageSet(id:Int) {
		if(!owner.replayMode)
			if(id>=0) {
				log.debug("Saving stage set to custom set #$id")
				propStageSet[id]?.save("config/map/gemmania/custom$id.map")
			} else {
				log.debug("Saving stage set to default set")
				propStageSet[id]?.save("config/map/gemmania/default.map")
			}
	}

	/** MapRead into #[id]:[field] from [prop] */
	private fun loadMap(field:Field, prop:CustomProperties, id:Int) {
		field.reset()
		field.readProperty(prop, id)
		field.setAllAttribute(true, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
		field.setAllAttribute(false, Block.ATTRIBUTE.SELF_PLACED)
		limittimeStart = prop.getProperty("$id.gemmania.limittimeStart", 3600*3)
		stagetimeStart = prop.getProperty("$id.gemmania.stagetimeStart", 3600)
		stagebgm = prop.getProperty("$id.gemmania.stagebgm", BGM.Puzzle(0).id)
		gimmickMirror = prop.getProperty("$id.gemmania.gimmickMirror", 0)
		gimmickRoll = prop.getProperty("$id.gemmania.gimmickRoll", 0)
		gimmickBig = prop.getProperty("$id.gemmania.gimmickBig", 0)
		gimmickXRay = prop.getProperty("$id.gemmania.gimmickXRay", 0)
		gimmickColor = prop.getProperty("$id.gemmania.gimmickColor", 0)
	}

	/** MapSave from #[id]:[field] into [prop] */
	private fun saveMap(field:Field, prop:CustomProperties, id:Int) {
		field.writeProperty(prop, id)
		prop.setProperty("$id.gemmania.limittimeStart", limittimeStart)
		prop.setProperty("$id.gemmania.stagetimeStart", stagetimeStart)
		prop.setProperty("$id.gemmania.stagebgm", stagebgm)
		prop.setProperty("$id.gemmania.gimmickMirror", gimmickMirror)
		prop.setProperty("$id.gemmania.gimmickRoll", gimmickRoll)
		prop.setProperty("$id.gemmania.gimmickBig", gimmickBig)
		prop.setProperty("$id.gemmania.gimmickXRay", gimmickXRay)
		prop.setProperty("$id.gemmania.gimmickColor", gimmickColor)
	}

	override fun loadSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
		startStage = prop.getProperty("gemmania.startstage", 0)
		mapSet = prop.getProperty("gemmania.stageset", -1)
		alwaysGhost = prop.getProperty("gemmania.alwaysghost", true)
		always20g = prop.getProperty("gemmania.always20g", false)
		secAlert = prop.getProperty("gemmania.lvstopse", true)
		showST = prop.getProperty("gemmania.showsectiontime", true)
		randomQueue = prop.getProperty("gemmania.randomnext", false)
		trainingType = prop.getProperty("gemmania.trainingType", 0)
		startNextc = prop.getProperty("gemmania.startnextc", 0)
	}

	override fun saveSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("gemmania.startstage", startStage)
		prop.setProperty("gemmania.stageset", mapSet)
		prop.setProperty("gemmania.alwaysghost", alwaysGhost)
		prop.setProperty("gemmania.always20g", always20g)
		prop.setProperty("gemmania.lvstopse", secAlert)
		prop.setProperty("gemmania.showsectiontime", showST)
		prop.setProperty("gemmania.randomnext", randomQueue)
		prop.setProperty("gemmania.trainingType", trainingType)
		prop.setProperty("gemmania.startnextc", startNextc)
	}

	/** Update falling speed
	 * @param engine GameEngine
	 */
	private fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = if(always20g) -1
		else tableGravityValue[tableGravityChangeLevel.indexOfFirst {it>=engine.statistics.level}
			.let {if(it<0) tableGravityChangeLevel.lastIndex else it}]


		engine.speed.are = 23
		engine.speed.areLine = 23
		engine.speed.lockDelay = 31

		if(speedlevel>=300) {
			engine.speed.lineDelay = 25
			engine.speed.das = 15
		} else {
			engine.speed.lineDelay = 40
			engine.speed.das = 9
		}

		engine.ghost = (speedlevel>=100&&!alwaysGhost)
	}

	/** Stage clearや time切れの判定
	 * @param engine GameEngine
	 */
	private fun checkStageEnd(engine:GameEngine) {
		if(clearFlag||stagetimeNow<=0&&stagetimeStart>0&&engine.timerActive) {
			skipFlag = false
			engine.nowPieceObject = null
			engine.timerActive = false
			engine.stat = GameEngine.Status.CUSTOM
			engine.resetStatc()
		} else if(limittimeNow<=0&&engine.timerActive) {
			engine.nowPieceObject = null
			engine.stat = GameEngine.Status.GAMEOVER
			engine.resetStatc()
		}
	}

	/** stage numberをStringで取得
	 * @param stageNumber stage number
	 * @return stage numberの文字列(21面以降はEX扱い)
	 */
	private fun getStageName(stageNumber:Int):String =
		if(stageNumber>=MAX_STAGE_NORMAL) "EX"+(stageNumber+1-MAX_STAGE_NORMAL) else ""+(stageNumber+1)

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// エディットMenu  メイン画面
		if(editModeScreen==1) {
			// Configuration changes
			val change = updateCursor(engine, 4)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
					}
					1, 2 -> {
						startStage += change
						if(startStage<0) startStage = MAX_STAGE_TOTAL-1
						if(startStage>MAX_STAGE_TOTAL-1) startStage = 0
					}
					3, 4 -> {
						mapSet += change
						if(mapSet<0) mapSet = 99
						if(mapSet>99) mapSet = 0
					}
				}
			}

			// 決定
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")

				when(menuCursor) {
					0 -> {
						editModeScreen = 2
						menuCursor = 0
						menuTime = 0
					}
					1 ->
						propStageSet[mapSet]?.let {
							loadMap(engine.field, it, startStage)
							engine.field.setAllSkin(engine.skin)
						}
					2 -> propStageSet[mapSet]?.let {saveMap(engine.field, it, startStage)}
					3 -> loadStageSet(mapSet)
					4 -> saveStageSet(mapSet)
				}
			}

			// Cancel
			if(engine.ctrl.isPress(Controller.BUTTON_D)&&engine.ctrl.isPress(Controller.BUTTON_E)) {
				editModeScreen = 0
				menuCursor = 0
				menuTime = 0
			}
		} else if(editModeScreen==2) {
			// Up
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
				menuCursor--
				if(menuCursor<0) menuCursor = 4
				engine.playSE("cursor")
			}
			// Down
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
				menuCursor++
				if(menuCursor>4) menuCursor = 0
				engine.playSE("cursor")
			}

			// Configuration changes
			var change = 0
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_LEFT)) change = -1
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_RIGHT)) change = 1

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl.isPress(Controller.BUTTON_E)) m = 100
				if(engine.ctrl.isPress(Controller.BUTTON_F)) m = 1000

				when(menuCursor) {
					0 -> {
					}
					1 -> {
						stagetimeStart += change*60*m
						if(stagetimeStart<0) stagetimeStart = 3600*20
						if(stagetimeStart>3600*20) stagetimeStart = 0
					}
					2 -> {
						limittimeStart += change*60*m
						if(limittimeStart<0) limittimeStart = 3600*20
						if(limittimeStart>3600*20) limittimeStart = 0
					}
					3 -> {
						stagebgm += change
						if(stagebgm<0) stagebgm = BGM.count
						if(stagebgm>BGM.count) stagebgm = 0
					}
					4 -> {
						gimmickMirror += change
						if(gimmickMirror<0) gimmickMirror = 99
						if(gimmickMirror>99) gimmickMirror = 0
					}
				}
			}

			// 決定
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")

				if(menuCursor==0) {
					engine.enterFieldEdit()
					return true
				}
				editModeScreen = 1
				menuCursor = 0
				menuTime = 0
			} else if(engine.ctrl.isPush(Controller.BUTTON_B)) {
				// Cancel
				editModeScreen = 1
				menuCursor = 0
				menuTime = 0
			}
		} else if(!engine.owner.replayMode) {
			// Up
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
				menuCursor--
				if(menuCursor<0) menuCursor = 8
				engine.playSE("cursor")
			}
			// Down
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
				menuCursor++
				if(menuCursor>8) menuCursor = 0
				engine.playSE("cursor")
			}

			// Configuration changes
			var change = 0
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_LEFT)) change = -1
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_RIGHT)) change = 1

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						startStage += change
						if(startStage<0) startStage = MAX_STAGE_TOTAL-1
						if(startStage>MAX_STAGE_TOTAL-1) startStage = 0

						propStageSet[mapSet]?.let {loadMap(engine.field, it, startStage)}
						engine.field.setAllSkin(engine.skin)
					}
					1 -> {
						mapSet += change
						if(mapSet<-1) mapSet = 99
						if(mapSet>99) mapSet = -1

						propStageSet[mapSet]?.let {loadMap(engine.field, it, startStage)}
						engine.field.setAllSkin(engine.skin)
					}
					2 -> alwaysGhost = !alwaysGhost
					3 -> always20g = !always20g
					4 -> secAlert = !secAlert
					5 -> showST = !showST
					6 -> randomQueue = !randomQueue
					7 -> {
						trainingType += change
						if(trainingType<0) trainingType = 2
						if(trainingType>2) trainingType = 0
					}
					8 -> {
						startNextc += change
						if(startNextc<0) startNextc = STRING_DEFAULT_NEXT_LIST.length-1
						if(startNextc>STRING_DEFAULT_NEXT_LIST.length-1) startNextc = 0
					}
				}
			}

			// 決定
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")
				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitFlag = true

			// エディット
			if(engine.ctrl.isPush(Controller.BUTTON_D)) {
				if(mapSet<0) mapSet = 0

				loadStageSet(mapSet)
				propStageSet[mapSet]?.let {loadMap(engine.field, it, startStage)}
				engine.field.setAllSkin(engine.skin)

				editModeScreen = 1
				menuCursor = 0
				menuTime = 0
			}
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}// 普通のMenu
		// エディットMenu   stage 画面

		return true
	}

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine) {
		when(editModeScreen) {
			1 -> {
				drawMenu(
					engine, receiver, 0, COLOR.GREEN, 0, "STAGE EDIT" to "[PUSH A]", "LOAD STAGE" to "[${getStageName(startStage)}]",
					"SAVE STAGE" to "[${getStageName(startStage)}]", "LOAD" to "[SET $mapSet]",
					"SAVE" to "[SET $mapSet]"
				)

				receiver.drawMenuFont(engine, 0, 19, "EXIT-> D+E", COLOR.ORANGE)
			}
			// エディットMenu   stage 画面
			2 -> drawMenu(
				engine, receiver, 0, COLOR.GREEN, 0, "MAP EDIT" to "[PUSH A]", "STAGE TIME" to stagetimeStart.toTimeStr,
				"LIMIT TIME" to limittimeStart.toTimeStr, "BGM" to stagebgm,
				"MIRROR" to if(gimmickMirror==0) "OFF" else gimmickMirror
			)
			else -> {
				// 普通のMenu
				if(!engine.owner.replayMode) receiver.drawMenuFont(engine, 0, 19, "D:EDIT", COLOR.ORANGE)

				val strTrainingType = when(trainingType) {
					1 -> "ON"
					2 -> "ON+RESET"
					else -> "OFF"
				}

				drawMenu(
					engine,
					receiver,
					0,
					COLOR.PINK,
					0,
					"STAGE NO." to getStageName(startStage),
					"STAGE SET" to if(mapSet<0) "DEFAULT" else "EDIT $mapSet",
					"FULL GHOST" to alwaysGhost,
					"FULL 20G" to always20g,
					"LVSTOPSE" to secAlert,
					"SHOW STIME" to showST,
					"RANDOM" to randomQueue,
					"TRAINING" to strTrainingType,
					"NEXT COUNT" to startNextc
				)
			}
		}
	}

	/* Ready画面の処理 */
	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			if(!engine.readyDone) {
				loadStageSet(mapSet)
				stage = startStage
				engine.nextPieceCount = startNextc

				if(!randomQueue)
					engine.nextPieceArrayID = GeneralUtil.createNextPieceArrayFromNumberString(STRING_DEFAULT_NEXT_LIST)
			}

			startStage(engine)

			if(!engine.readyDone) limittimeNow = limittimeStart
		}
		return false
	}

	/* Ready画面の描画処理 */
	override fun renderReady(engine:GameEngine) {
		if(engine.statc[0]>=engine.readyStart) {
			// トレーニング
			if(trainingType!=0) receiver.drawMenuFont(engine, 1, 5, "TRAINING", COLOR.GREEN)

			// STAGE XX
			if(stage>=MAX_STAGE_NORMAL) {
				receiver.drawMenuFont(engine, 0, 7, "EX LEVEL ", COLOR.GREEN)
				receiver.drawMenuFont(engine, 9, 7, "${stage+1-MAX_STAGE_NORMAL}")
			} else {
				receiver.drawMenuFont(engine, 1, 7, "LEVEL", COLOR.GREEN)
				val strStage = "%2s".format(getStageName(stage))
				receiver.drawMenuFont(engine, 7, 7, strStage)
			}
		}
	}

	/* Called at game start(2回目以降のReadyも含む) */
	override fun startGame(engine:GameEngine) {
		// X-RAY開始
		if(gimmickXRay>0) engine.itemXRayEnable = true
		// カラー開始
		if(gimmickColor>0) engine.itemColorEnable = true

		// BGM切り替え
		owner.musMan.fadeSW = false
		owner.musMan.bgm = BGM.values[stagebgm]
	}

	/* Render score */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(
			engine, 0, 0, "GRAND BLOSSOM"+if(randomQueue)
				" (RANDOM)"
			else
				"", COLOR.RED
		)

		receiver.drawScoreFont(engine, -1, -4*2, "DECORATION", scale = .5f)
		receiver.drawScoreBadges(engine, 0, -3, 100, decoration)
		receiver.drawScoreBadges(engine, 5, -4, 100, decTemp)
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(startStage==0&&!always20g&&trainingType==0&&startNextc==0&&mapSet<0&&engine.ai==null) {
				val topY = if(receiver.nextDisplayType==2) 5 else 3

				receiver.drawScoreFont(engine, 3, topY-1, "STAGE CLEAR TIME", COLOR.PINK)
				val type = randomQueue.toInt()

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(
						engine, 0, topY+i, "%2d".format(i+1), if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW
					)
					receiver.drawScoreFont(
						engine, 3, topY+i, getStageName(rankingStage[type][i]), when {
							rankingAllClear[type][i]==1 -> COLOR.GREEN
							rankingAllClear[type][i]==2 -> COLOR.ORANGE
							else -> COLOR.WHITE
						}
					)
					receiver.drawScoreNum(engine, 9, topY+i, "${rankingRate[type][i]}%", i==rankingRank)
					receiver.drawScoreNum(engine, 15, topY+i, rankingTime[type][i].toTimeStr, i==rankingRank)
				}
			}
		} else {
			receiver.drawScoreFont(engine, 0, 2, "LEVEL", COLOR.PINK)
			receiver.drawScoreFont(engine, 6, 2, getStageName(stage))

			receiver.drawScoreMedal(engine, 0, 3, "MIRROR", (gimmickMirror>0).toInt())
			receiver.drawScoreMedal(engine, 0, 3, "ROLL ROLL", (gimmickRoll>0).toInt())
			receiver.drawScoreMedal(engine, 0, 3, "DEATH BLOCK", (gimmickBig>0).toInt())
			receiver.drawScoreMedal(engine, 0, 3, "X-RAY", (gimmickXRay>0).toInt())
			receiver.drawScoreMedal(engine, 0, 4, "COLOR", (gimmickColor>0).toInt())

			receiver.drawScoreFont(engine, 0, 5, "REST", COLOR.PINK)
			receiver.drawScoreNum(engine, 0, 6, "$rest")

			if(trainingType==0) {
				receiver.drawScoreFont(engine, 3, 8, "% CLEAR", COLOR.PINK)
				receiver.drawScoreNum(engine, 0, 8, "$clearRate")
			} else {
				receiver.drawScoreFont(engine, 0, 8, "BEST TIME", COLOR.PINK)
				receiver.drawScoreNum(engine, 0, 9, trainingBestTime.toTimeStr)
			}

			//  level
			receiver.drawScoreFont(engine, 0, 11, "Level", COLOR.PINK)
			receiver.drawScoreNum(engine, 1, 12, "%3d".format(maxOf(0, speedlevel)))
			receiver.drawScoreSpeed(engine, 0, 13, if(engine.speed.gravity<0) 40 else engine.speed.gravity/128, 4)
			receiver.drawScoreNum(engine, 1, 14, "%3d".format(nextSecLv))

			//  stage Time
			if(stagetimeStart>0) {
				receiver.drawScoreFont(engine, 0, 16, "STAGE TIME", COLOR.PINK)
				receiver.drawScoreNum(
					engine, 0, 17, stagetimeNow.toTimeStr, engine.timerActive
						&&stagetimeNow<600&&stagetimeNow%4==0, 2f
				)
			}

			// Time limit
			if(limittimeStart>0) {
				if(timeextendDisp>0) {
					receiver.drawScoreNano(engine, 0, 19, "TIME EXTENSION", COLOR.PINK)
					receiver.drawScoreNum(
						engine, 0, 22, "+$timeextendSeconds", engine.timerActive&&limittimeNow<600&&limittimeNow%4==0
					)
				}
				receiver.drawScoreNum(
					engine, 0, 20, limittimeNow.toTimeStr, engine.timerActive&&limittimeNow<600&&limittimeNow%4==0, 2f
				)
			}

			// Section Time
			if(showST&&sectionTime.isNotEmpty()) {
				val y = if(receiver.nextDisplayType==2) 4 else 2
				val x = if(receiver.nextDisplayType==2) 22 else 10

				receiver.drawScoreFont(engine, x, y, "SECTION TIME", COLOR.PINK)

				for(i in sectionTime.indices)
					if(sectionTime[i]!=0) {
						val strSeparator = if(i==stage&&engine.ending==0) BaseFont.CURSOR else " "

						val strSectionTime:String = when {
							sectionTime[i]==-1 -> "%3s%s%s".format(getStageName(i), strSeparator, "FAILED")
							sectionTime[i]==-2 -> "%3s%s%s".format(getStageName(i), strSeparator, "SKIPPED")
							else -> "%3s%s%s".format(getStageName(i), strSeparator, sectionTime[i].toTimeStr)
						}
						val pos = i-maxOf(stage-14, 0)
						if(pos>=0) receiver.drawScoreFont(engine, x, y+1+pos, strSectionTime)
					}

				if(receiver.nextDisplayType==2) {
					receiver.drawScoreFont(engine, 11, 19, "TOTAL", COLOR.PINK)
					receiver.drawScoreNum(engine, 11, 20, engine.statistics.time.toTimeStr, 2f)
				} else {
					receiver.drawScoreFont(engine, 12, 16, "TOTAL TIME", COLOR.PINK)
					receiver.drawScoreNum(engine, 12, 17, engine.statistics.time.toTimeStr, 2f)
				}
			}
		}
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		if(timeextendDisp>0) timeextendDisp--

		if(engine.gameActive&&engine.timerActive&&engine.ctrl.isPress(Controller.BUTTON_F)) {
			skipbuttonPressTime++

			if(skipbuttonPressTime>=60&&(stage<MAX_STAGE_NORMAL-1||trainingType!=0)&&limittimeNow>30*60
				&&!clearFlag
			) {
				skipFlag = true
				engine.nowPieceObject = null
				engine.timerActive = false
				engine.stat = GameEngine.Status.CUSTOM
				engine.resetStatc()
			}
		} else
			skipbuttonPressTime = 0

		// 経過 time
		if(engine.gameActive&&engine.timerActive) {
			cleartime++
			sectionTime[stage] = engine.statistics.time-sectionTime.take(stage).sum()
		}

		// Time limit
		if(engine.gameActive&&engine.timerActive&&limittimeNow>0) {
			limittimeNow--

			// Time meter
			engine.meterValue = minOf(1f, limittimeNow*1f/limittimeStart)
			engine.meterColor = GameEngine.METER_COLOR_LIMIT

			if(limittimeNow>0&&limittimeNow<=10*60&&limittimeNow%60==0)
			// 10秒前からのカウントダウン
				engine.playSE("countdown")
		}

		//  stage Time
		if(engine.gameActive&&engine.timerActive&&stagetimeNow>0) {
			stagetimeNow--

			if(stagetimeNow>0&&stagetimeNow<=10*60&&stagetimeNow%60==0)
			// 10秒前からのカウントダウン
				engine.playSE("countdown")
		}
	}

	/* 移動中の処理 */
	override fun onMove(engine:GameEngine):Boolean {
		// 新規ピース出現時
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!lvupFlag) {
			// Level up
			if(speedlevel<nextSecLv-1) {
				speedlevel++
				if(speedlevel==nextSecLv-1&&secAlert) engine.playSE("levelstop")
			}
			setSpeed(engine)
		}
		if(engine.ending==0&&engine.statc[0]>0) lvupFlag = false

		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable) {
			// Roll Roll
			engine.itemRollRollEnable = gimmickRoll>0&&(thisStageTotalPieceLockCount+1)%gimmickRoll==0
			// Big
			engine.big = gimmickBig>0&&(thisStageTotalPieceLockCount+1)%gimmickBig==0

			// X-RAY
			if(gimmickXRay>0)
				if(thisStageTotalPieceLockCount%gimmickXRay==0)
					engine.itemXRayEnable = true
				else {
					engine.itemXRayEnable = false
					engine.resetFieldVisible()
				}

			// カラー
			if(gimmickColor>0)
				if(thisStageTotalPieceLockCount%gimmickColor==0)
					engine.itemColorEnable = true
				else {
					engine.itemColorEnable = false
					engine.resetFieldVisible()
				}
		}

		return false
	}

	/* ARE中の処理 */
	override fun onARE(engine:GameEngine):Boolean {
		// 最後の frame
		if(engine.ending==0&&engine.statc[0]>=engine.statc[1]-1&&!lvupFlag) {
			if(speedlevel<nextSecLv-1) {
				speedlevel++
				if(speedlevel==nextSecLv-1&&secAlert) engine.playSE("levelstop")
			}
			setSpeed(engine)
			lvupFlag = true
		}

		return false
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// 実際に消えるLinescount(Big時半分にならない)
		val realLines = engine.field.lines

		if(realLines>=1&&engine.ending==0) {
			// 宝石消去
			val gemClears = engine.field.howManyGemClears
			if(gemClears>0) {
				rest -= gemClears
				if(rest<=0) clearFlag = true
				limittimeNow += 60*gemClears
				timeextendSeconds = gemClears
				timeextendDisp = 120
			}

			// Level up
			val levelplus = ev.lines.let {
				when {
					it==3 -> 4
					it>=4 -> 6
					else -> it
				}
			}

			speedlevel += levelplus

			setSpeed(engine)

			if(speedlevel>998)
				speedlevel = 998
			else if(speedlevel>=nextSecLv) {
				// Next Section
				engine.playSE("levelup")

				// Background切り替え
				owner.bgMan.nextBg = nextSecLv/100

				// Update level for next section
				nextSecLv += 100
			} else if(speedlevel==nextSecLv-1&&secAlert) engine.playSE("levelstop")
			return gemClears
		}
		return 0
	}

	/* Line clear処理が終わったときの処理 */
	override fun lineClearEnd(engine:GameEngine):Boolean {
		checkStageEnd(engine)

		return false
	}

	/* Blockピースが固定されたときの処理(calcScoreの直後) */
	override fun pieceLocked(engine:GameEngine, lines:Int) {
		// 固定 count+1
		thisStageTotalPieceLockCount++

		// ミラー
		if(gimmickMirror>0&&thisStageTotalPieceLockCount%gimmickMirror==0)
			engine.interruptItemNumber = Block.ITEM.MIRROR

		//  stage 終了判定
		if(lines<=0) checkStageEnd(engine)
	}

	/** stage 終了画面の描画 */
	override fun onCustom(engine:GameEngine):Boolean {
		// 最初の frame の処理
		if(engine.statc[0]==0) {
			// Sound effects
			engine.playSE(if(clearFlag) "stageclear" else "stagefail")

			// Cleared  stage +1
			if(clearFlag) {
				doneLevel++
				decTemp += 3
				if(stage>=MAX_STAGE_NORMAL) decTemp += 3
			}
			// クリア率計算
			tryLevels++
			clearRate = doneLevel*100/tryLevels

			// Time bonus
			timeextendStageClearSeconds = 0
			if(clearFlag) {
				if(cleartime<10*60)
					timeextendStageClearSeconds = 10
				else if(cleartime<20*60) timeextendStageClearSeconds = 5

				if(stage==MAX_STAGE_NORMAL-1) timeextendStageClearSeconds += 60
			} else if(skipFlag) timeextendStageClearSeconds = 30

			// 最終 stage を決定
			if(stage==MAX_STAGE_NORMAL-1)
				laststage = if(clearRate<90)
					19 // クリア率が90%に満たない場合は stage 20で終了
				else if(clearRate<100)
					22 // クリア率が90～99%はEX3まで
				else if(engine.statistics.time>5*3600)
					24 // クリア率が100%で5分超えている場合はEX5
				else
					MAX_STAGE_TOTAL-1 // クリア率が100%で5分以内ならEX7

			// BGM fadeout
			if((stage==MAX_STAGE_NORMAL-1||stage==laststage)&&trainingType==0) owner.musMan.fadeSW = true

			// ギミック解除
			engine.interruptItemNumber = null
			engine.itemXRayEnable = false
			engine.itemColorEnable = false
			engine.resetFieldVisible()

			// Section Time設定
			sectionTime[stage] = if(clearFlag) cleartime
			else if(!skipFlag) -1 // Out of time
			else -2 // スキップ

			// トレーニングでのベストTime
			if(trainingType!=0&&clearFlag&&(cleartime<trainingBestTime||trainingBestTime<0)) trainingBestTime = cleartime
		}

		// Time limitが増える演出
		if(engine.statc[1]<timeextendStageClearSeconds*60) {
			engine.statc[1] += if(timeextendStageClearSeconds<30)
				4
			else if(timeextendStageClearSeconds<60)
				10 else 30

			// Time meter
			var limittimeTemp = limittimeNow+engine.statc[1]
			if(skipFlag) limittimeTemp = limittimeNow-engine.statc[1]

			engine.meterValue = minOf(1f, limittimeTemp*1f/limittimeStart)

			engine.meterColor = GameEngine.METER_COLOR_LIMIT
		}

		// Next 画面へ
		if(engine.statc[0]>=300||engine.ctrl.isPush(Controller.BUTTON_A)) {
			// Training
			if(trainingType!=0) {
				if(clearFlag) limittimeNow += timeextendStageClearSeconds*60
				if(skipFlag) limittimeNow -= timeextendStageClearSeconds*60
				if(trainingType==2) engine.nextPieceCount = continueNextPieceCount
				engine.stat = GameEngine.Status.READY
				engine.resetStatc()
			} else if(stage>=laststage) {
				allClear = if(stage>=MAX_STAGE_TOTAL-1) 2 else 1
				engine.ending = 1
				engine.gameEnded()
				engine.stat = GameEngine.Status.ENDINGSTART
				engine.resetStatc()
			} else {
				stage++
				if(clearFlag) limittimeNow += timeextendStageClearSeconds*60
				if(skipFlag) limittimeNow -= timeextendStageClearSeconds*60
				engine.stat = GameEngine.Status.READY
				engine.resetStatc()
			}// Next  stage
			// Ending
			return true
		}

		engine.statc[0]++

		return true
	}

	/** stage 終了画面の描画 */
	override fun renderCustom(engine:GameEngine) {
		if(engine.statc[0]<1) return

		// STAGE XX
		receiver.drawMenuFont(engine, 1, 2, "STAGE", COLOR.GREEN)
		val strStage = "%2s".format(getStageName(stage))
		receiver.drawMenuFont(engine, 7, 2, strStage)

		if(clearFlag) {
			// クリア
			receiver.drawMenuFont(
				engine, 2, 4, "CLEAR!", if(engine.statc[0]%2==0)
					COLOR.ORANGE
				else
					COLOR.WHITE
			)

			receiver.drawMenuFont(engine, 0, 7, "LIMIT TIME", COLOR.PINK)
			receiver.drawMenuFont(
				engine,
				1,
				8,
				(limittimeNow+engine.statc[1]).toTimeStr,
				if(engine.statc[0]%2==0&&engine.statc[1]<timeextendStageClearSeconds*60) COLOR.ORANGE else COLOR.WHITE
			)

			receiver.drawMenuFont(engine, 2, 10, "EXTEND", COLOR.PINK)
			receiver.drawMenuFont(engine, 2, 11, "$timeextendStageClearSeconds SEC.")

			receiver.drawMenuFont(engine, 0, 13, "CLEAR TIME", COLOR.PINK)
			receiver.drawMenuFont(engine, 1, 14, cleartime.toTimeStr)

			receiver.drawMenuFont(engine, 0, 16, "TOTAL TIME", COLOR.PINK)
			receiver.drawMenuFont(engine, 1, 17, engine.statistics.time.toTimeStr)
		} else if(skipFlag) {
			// スキップ
			receiver.drawMenuFont(engine, 1, 4, "SKIPPED")
			receiver.drawMenuFont(
				engine, 1, 5, "-30 SEC.", if(engine.statc[0]%2==0&&engine.statc[1]<30*60) COLOR.WHITE else COLOR.RED
			)

			receiver.drawMenuFont(engine, 0, 10, "LIMIT TIME", COLOR.PINK)
			receiver.drawMenuFont(
				engine,
				1,
				11,
				(limittimeNow-engine.statc[1]).toTimeStr,
				if(engine.statc[0]%2==0&&engine.statc[1]<30*60) COLOR.RED else COLOR.WHITE
			)

			if(trainingType==0) {
				receiver.drawMenuFont(engine, 0, 13, "CLEAR PER.", COLOR.PINK)
				receiver.drawMenuFont(engine, 3, 14, "$clearRate%")
			}

			receiver.drawMenuFont(engine, 0, 16, "TOTAL TIME", COLOR.PINK)
			receiver.drawMenuFont(engine, 1, 17, engine.statistics.time.toTimeStr)
		} else if(stagetimeNow<=0&&stagetimeStart>0) {
			// Timeアップ
			receiver.drawMenuFont(engine, 1, 0, "TIME OVER")
			receiver.drawMenuFont(engine, 1, 5, "TRY NEXT")

			receiver.drawMenuFont(engine, 0, 10, "LIMIT TIME", COLOR.PINK)
			receiver.drawMenuFont(engine, 1, 11, limittimeNow.toTimeStr)

			if(trainingType==0) {
				receiver.drawMenuFont(engine, 0, 13, "CLEAR PER.", COLOR.PINK)
				receiver.drawMenuFont(engine, 3, 14, "$clearRate%")
			}

			receiver.drawMenuFont(engine, 0, 16, "TOTAL TIME", COLOR.PINK)
			receiver.drawMenuFont(engine, 1, 17, engine.statistics.time.toTimeStr)
		}
	}

	/* Called at game over(主にコンティニュー画面) */
	override fun onGameOver(engine:GameEngine):Boolean {
		// コンティニュー画面
		if(engine.ending==0&&!noContinue) {
			if(engine.statc[0]==0) {
				engine.playSE("died")
				engine.playSE("shutter")
				owner.musMan.bgm = BGM.Silent

				engine.timerActive = false
				engine.blockShowOutlineOnly = false

				engine.itemXRayEnable = false
				engine.itemColorEnable = false
				engine.interruptItemNumber = null

				engine.resetFieldVisible()

				engine.allowTextRenderByReceiver = false // GAMEOVER表示抑制
			}
			if(engine.statc[0]<engine.field.height+1) {
				// field灰色化
				for(i in 0 until engine.field.width)
					if(!engine.field.getBlockEmpty(i, engine.statc[0])) {
						val blk = engine.field.getBlock(i, engine.statc[0])

						blk?.apply {
							color = Block.COLOR.WHITE
							type = Block.TYPE.BLOCK
							darkness = 0f
							elapsedFrames = -1
						}
					}
				engine.statc[0]++
			} else if(engine.statc[0]<engine.field.height+1+600) {
				// コンティニュー選択
				if(engine.ctrl.isPush(Controller.BUTTON_UP)||engine.ctrl.isPush(Controller.BUTTON_DOWN)) {
					engine.statc[1]++
					if(engine.statc[1]>1) engine.statc[1] = 0
					engine.playSE("cursor")
				}
				// 決定
				if(engine.ctrl.isPush(Controller.BUTTON_A)) {
					if(engine.statc[1]==0) {
						// YES
						limittimeNow = limittimeStart
						engine.nextPieceCount = continueNextPieceCount
						if(trainingType==0) engine.statistics.time += 60*60*2
						engine.allowTextRenderByReceiver = true
						engine.stat = GameEngine.Status.READY
						engine.resetStatc()
						engine.playSE("decide")
					} else
					// NO
						engine.statc[0] = engine.field.height+1+600
				} else
					engine.statc[0]++
			} else if(engine.statc[0]>=engine.field.height+1+600) {
				// ＼(^o^)／ｵﾜﾀ
				noContinue = true
				engine.allowTextRenderByReceiver = true // GAMEOVER表示抑制解除
				engine.resetStatc()
				decoration += decTemp-3
			}

			return true
		}

		return false
	}

	/* game over時の描画処理(主にコンティニュー画面) */
	override fun renderGameOver(engine:GameEngine) {
		if(engine.ending==0&&!noContinue)
			if(engine.statc[0]>=engine.field.height+1&&engine.statc[0]<engine.field.height+1+600) {
				receiver.drawMenuFont(engine, 1, 7, "CONTINUE?", COLOR.PINK)

				receiver.drawMenuFont(engine, 3, 9+engine.statc[1]*2, BaseFont.CURSOR, COLOR.RED)
				receiver.drawMenuFont(engine, 4, 9, "YES", engine.statc[1]==0)
				receiver.drawMenuFont(engine, 4, 11, "NO", engine.statc[1]==1)

				val t = engine.field.height+1+600-engine.statc[0]
				receiver.drawMenuFont(engine, 2, 13, "TIME ${(t-1)/60}", COLOR.GREEN)

				receiver.drawMenuFont(engine, 0, 16, "TOTAL TIME", COLOR.PINK)
				receiver.drawMenuFont(engine, 1, 17, engine.statistics.time.toTimeStr)

				if(trainingType==0) receiver.drawMenuFont(engine, 0, 18, "+2 MINUTES", COLOR.RED)
			}
	}

	/* 結果画面の処理 */
	override fun onResult(engine:GameEngine):Boolean {
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

		return false
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 0, "\u0090\u0093 PAGE${engine.statc[1]+1}/3", COLOR.RED)

		if(engine.statc[1]==0) {
			var gcolor = COLOR.WHITE
			if(allClear==1) gcolor = COLOR.GREEN
			if(allClear==2) gcolor = COLOR.ORANGE

			receiver.drawMenuFont(engine, 0, 2, "STAGE", COLOR.PINK)
			val strStage = "%10s".format(getStageName(stage))
			receiver.drawMenuFont(engine, 0, 3, strStage, gcolor)

			drawResult(engine, receiver, 4, COLOR.PINK, "CLEAR", "%9d%%".format(clearRate))
			drawResultStats(engine, receiver, 6, COLOR.PINK, Statistic.LINES, Statistic.PIECE, Statistic.TIME)
			drawResultRank(engine, receiver, 12, COLOR.PINK, rankingRank)
		} else {
			receiver.drawMenuFont(engine, 0, 2, "SECTION${engine.statc[1]}/2", COLOR.PINK)

			var i = if(engine.statc[1]==1) 0 else 15
			val y = if(engine.statc[1]==1) 3 else -12
			while(i<if(engine.statc[1]==1)
					15
				else
					sectionTime.size
			) {
				if(sectionTime[i]!=0)
					when {
						sectionTime[i]==-1 -> receiver.drawMenuNum(engine, 2, i+y, "FAILED", COLOR.RED)
						sectionTime[i]==-2 -> receiver.drawMenuNum(engine, 2, i+y, "SKIPPED", COLOR.PURPLE)
						else -> receiver.drawMenuNum(engine, 2, i+y, sectionTime[i].toTimeStr)
					}
				i++
			}
		}
	}

	/* リプレイ保存 */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		prop.setProperty("gemmania.version", version)
		prop.setProperty("gemmania.result.stage", stage)
		prop.setProperty("gemmania.result.rate", clearRate)
		prop.setProperty("gemmania.result.clear", allClear)

		engine.statistics.level = stage
		engine.statistics.levelDispAdd = 1
		engine.statistics.scoreBonus = clearRate
		engine.statistics.writeProperty(prop, engine.playerID)
		if(!owner.replayMode) {
			owner.statsProp.setProperty("decoration", decoration)
			owner.statsProp.save(owner.statsFile)
		}
		// Update rankings
		return (!owner.replayMode&&startStage==0&&trainingType==0&&startNextc==0&&mapSet<0&&!always20g&&engine.ai==null&&
			updateRanking(randomQueue.toInt(), stage, clearRate, engine.statistics.time, allClear)!=-1)
	}

	/** Update rankings
	 * @param type Game type
	 * @param stg stage
	 * @param clper クリア率
	 * @param time Time
	 * @param clear 完全クリア flag
	 */
	private fun updateRanking(type:Int, stg:Int, clper:Int, time:Int, clear:Int):Int {
		rankingRank = checkRanking(type, stg, clper, time, clear)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingStage[type][i] = rankingStage[type][i-1]
				rankingRate[type][i] = rankingRate[type][i-1]
				rankingTime[type][i] = rankingTime[type][i-1]
				rankingAllClear[type][i] = rankingAllClear[type][i-1]
			}

			// Add new data
			rankingStage[type][rankingRank] = stg
			rankingRate[type][rankingRank] = clper
			rankingTime[type][rankingRank] = time
			rankingAllClear[type][rankingRank] = clear
		}
		return rankingRank
	}

	/** Calculate ranking position
	 * @param type Game type
	 * @param stg stage
	 * @param clper クリア率
	 * @param time Time
	 * @param clear 完全クリア flag
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(type:Int, stg:Int, clper:Int, time:Int, clear:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(clear>rankingAllClear[type][i]) return i
			else if(clear==rankingAllClear[type][i]&&stg>rankingStage[type][i]) return i
			else if(clear==rankingAllClear[type][i]&&stg==rankingStage[type][i]&&clper>rankingRate[type][i]) return i
			else if(clear==rankingAllClear[type][i]&&stg==rankingStage[type][i]&&clper==rankingRate[type][i]&&
				time<rankingTime[type][i]
			) return i

		return -1
	}

	companion object {
		/** Log */
		internal val log = LogManager.getLogger()

		/** Current version */
		private const val CURRENT_VERSION = 1

		/** level contains directory */
		private const val levelDir = "config/map/gemmania/"
		/** Maximum level count */
		private const val MAX_STAGE_TOTAL = 27

		/** Normal level count */
		private const val MAX_STAGE_NORMAL = 20

		/** NEXT list */
		private const val STRING_DEFAULT_NEXT_LIST =
			"1052463015240653120563402534162340621456034251036420314526014362045136455062150461320365204631546310"+
				"6451324023650143620435621456302513025430312603452013625026345012660132450346213462054360143260534215"+
				"0621543621435624013542130562345123641230462134502613542"

		/** 落下速度 table */
		private val tableGravityValue = listOf(4, 32, 64, 96, 128, 160, 192, 224, 256, 512, 768, 1024, 768, -1)

		/** 落下速度が変わる level */
		private val tableGravityChangeLevel = listOf(20, 30, 33, 36, 39, 43, 47, 51, 100, 130, 160, 250, 300, 10000)

		/** Number of ranking typesのcount */
		private const val RANKING_TYPE = 2

		/** Number of entries in rankings */
		private const val RANKING_MAX = 13
	}
}
