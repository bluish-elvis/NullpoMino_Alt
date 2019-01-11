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

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.Logger

/** GEM MANIA */
class GrandBlossom:AbstractMode() {

	/** Stage set property file */
	private var propStageSet:CustomProperties = CustomProperties()

	/** 残りプラチナBlockcount */
	private var rest:Int = 0

	/** Current stage number */
	private var stage:Int = 0

	/** Last stage number */
	private var laststage:Int = 0

	/** Attempted stage count */
	private var trystage:Int = 0

	/** Cleared stage count */
	private var clearstage:Int = 0

	/** Clear rate */
	private var clearper:Int = 0

	/** Stage clear flag */
	private var clearflag:Boolean = false

	/** Stage skip flag */
	private var skipflag:Boolean = false

	/** Time limit left */
	private var limittimeNow:Int = 0

	/** Time limit at start */
	private var limittimeStart:Int = 0

	/** Stage time left */
	private var stagetimeNow:Int = 0

	/** Stage 開始後の経過 time */
	private var cleartime:Int = 0

	/** Stage time at start */
	private var stagetimeStart:Int = 0

	/** Stage BGM */
	private var stagebgm:Int = 0

	/** Current 落下速度の number (tableGravityChangeLevelの levelに到達するたびに1つ増える) */
	private var gravityindex:Int = 0

	/** Next section level (levelstop when this is -1) */
	private var nextseclv:Int = 0

	/** Level */
	private var speedlevel:Int = 0

	/** Levelが増えた flag */
	private var lvupflag:Boolean = false

	/** Section Time */
	private var sectionTime:IntArray = IntArray(MAX_STAGE_TOTAL)

	/** Current time limit extension in seconds */
	private var timeextendSeconds:Int = 0

	/** Number of frames to display current time limit extension */
	private var timeextendDisp:Int = 0

	/** Stage clear time limit extension in seconds */
	private var timeextendStageClearSeconds:Int = 0

	/** Blockを置いた count(1面終了でリセット) */
	private var thisStageTotalPieceLockCount:Int = 0

	/** Skip buttonを押している time */
	private var skipbuttonPressTime:Int = 0

	/** Blockピースを置いた count (NEXTピースの計算用）のバックアップ (コンティニュー時に戻す) */
	private var continueNextPieceCount:Int = 0

	/** Set to true when NO is picked at continue screen */
	private var noContinue:Boolean = false

	/** All clear flag */
	private var allclear:Int = 0

	/** Best time in training mode */
	private var trainingBestTime:Int = 0

	/** ミラー発動間隔 */
	private var gimmickMirror:Int = 0

	/** Roll Roll 発動間隔 */
	private var gimmickRoll:Int = 0

	/** Big発動間隔 */
	private var gimmickBig:Int = 0

	/** X-RAY発動間隔 */
	private var gimmickXRay:Int = 0

	/** カラー発動間隔 */
	private var gimmickColor:Int = 0

	/** Current edit screen */
	private var editModeScreen:Int = 0

	/** Stage at start */
	private var startstage:Int = 0

	/** Selected stage set */
	private var stageset:Int = 0

	/** When true, always ghost ON */
	private var alwaysghost:Boolean = false

	/** When true, always 20G */
	private var always20g:Boolean = false

	/** When true, levelstop sound is enabled */
	private var lvstopse:Boolean = false

	/** When true, section time display is enabled */
	private var showsectiontime:Boolean = false

	/** NEXTをランダムにする */
	private var randomnext:Boolean = false

	/** Training mode */
	private var trainingType:Int = 0

	/** Block counter at start */
	private var startnextc:Int = 0

	/** Version */
	private var version:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = 0

	/** Rankings' stage reached */
	private var rankingStage:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Rankings' clear ratio */
	private var rankingClearPer:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Rankings' times */
	private var rankingTime:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Rankings' all clear flag */
	private var rankingAllClear:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	private var decoration:Int = 0
	private var dectemp:Int = 0

	/* Mode nameを取得 */
	override val name:String
		get() = "GRAND BLOSSOM"

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		log.debug("playerInit called")

		super.playerInit(engine, playerID)
		menuTime = 0
		rest = 0
		stage = 0
		laststage = MAX_STAGE_NORMAL-1
		trystage = 0
		clearstage = 0
		clearper = 0
		clearflag = false
		skipflag = false
		limittimeNow = 0
		limittimeStart = 0
		stagetimeNow = 0
		stagetimeStart = 0
		cleartime = 0

		gravityindex = 0
		nextseclv = 0
		speedlevel = 0
		lvupflag = false

		sectionTime = IntArray(MAX_STAGE_TOTAL)

		timeextendSeconds = 0
		timeextendDisp = 0
		timeextendStageClearSeconds = 0

		thisStageTotalPieceLockCount = 0
		skipbuttonPressTime = 0

		continueNextPieceCount = 0
		noContinue = false

		allclear = 0

		trainingBestTime = -1

		gimmickMirror = 0
		gimmickRoll = 0
		gimmickBig = 0
		gimmickXRay = 0
		gimmickColor = 0

		editModeScreen = 0

		startstage = 0
		stageset = -1
		alwaysghost = false
		always20g = false
		lvstopse = true
		showsectiontime = false
		randomnext = false
		trainingType = 0
		startnextc = 0

		rankingRank = -1
		rankingStage = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingClearPer = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingTime = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingAllClear = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

		engine.tspinEnable = false
		engine.b2bEnable = false
		engine.framecolor = GameEngine.FRAME_COLOR_PINK
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		engine.bighalf = true
		engine.bigmove = false
		engine.staffrollEnable = false
		engine.holdButtonNextSkip = true

		engine.fieldWidth = 10
		engine.fieldHeight = 20
		engine.createFieldIfNeeded()
		dectemp = 0
		decoration = dectemp
		version = if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.modeConfig, engine.ruleopt.strRuleName)
			CURRENT_VERSION
		} else {
			loadSetting(owner.replayProp)
			owner.replayProp.getProperty("gemmania.version", 0)
		}

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
		gravityindex = 0
		nextseclv = 100
		lvupflag = false
		setSpeed(engine)
		thisStageTotalPieceLockCount = 0
		continueNextPieceCount = engine.nextPieceCount

		// Background戻す
		if(owner.backgroundStatus.bg!=0) {
			owner.backgroundStatus.fadesw = true
			owner.backgroundStatus.fadecount = 0
			owner.backgroundStatus.fadebg = 0
		}

		// ghost 復活
		engine.ghost = true

		// ホールド消去
		engine.holdDisable = false
		engine.holdPieceObject = null

		clearflag = false
		skipflag = false

		//  stage Map読み込み
		engine.createFieldIfNeeded()
		loadMap(engine.field!!, propStageSet, stage)
		engine.field!!.setAllSkin(engine.skin)

		//  stage Timeなどを設定
		cleartime = 0
		sectionTime[stage] = 0
		stagetimeNow = stagetimeStart
		rest = engine.field!!.howManyGems

		if(owner.bgmStatus.bgm.id!=stagebgm) owner.bgmStatus.fadesw = true
	}

	/** stage セットを読み込み
	 * @param id stage セット number(-1で default )
	 */
	private fun loadStageSet(id:Int) {
		propStageSet = if(id>=0) {
			log.debug("Loading stage set from custom set #$id")
			receiver.loadProperties("config/values/gemmania/custom$id.values")
		} else {
			log.debug("Loading stage set from default set")
			receiver.loadProperties("config/values/gemmania/default.values")
		}?:CustomProperties()
	}

	/** stage セットを保存
	 * @param id stage セット number(-1で default )
	 */
	private fun saveStageSet(id:Int) {
		if(!owner.replayMode)
			if(id>=0) {
				log.debug("Saving stage set to custom set #$id")
				receiver.saveProperties("config/values/gemmania/custom$id.values", propStageSet)
			} else {
				log.debug("Saving stage set to default set")
				receiver.saveProperties("config/values/gemmania/default.values", propStageSet)
			}
	}

	/** Map読み込み
	 * @param field field
	 * @param prop Property file to read from
	 */
	private fun loadMap(field:Field, prop:CustomProperties, id:Int) {
		field.reset()
		field.readProperty(prop, id)
		field.setAllAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true)
		field.setAllAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, true)
		field.setAllAttribute(Block.BLOCK_ATTRIBUTE_SELFPLACED, false)
		limittimeStart = prop.getProperty(id.toString()+".gemmania.limittimeStart", 3600*3)
		stagetimeStart = prop.getProperty(id.toString()+".gemmania.stagetimeStart", 3600)
		stagebgm = prop.getProperty(id.toString()+".gemmania.stagebgm", BGM.PUZZLE_1.id)
		gimmickMirror = prop.getProperty(id.toString()+".gemmania.gimmickMirror", 0)
		gimmickRoll = prop.getProperty(id.toString()+".gemmania.gimmickRoll", 0)
		gimmickBig = prop.getProperty(id.toString()+".gemmania.gimmickBig", 0)
		gimmickXRay = prop.getProperty(id.toString()+".gemmania.gimmickXRay", 0)
		gimmickColor = prop.getProperty(id.toString()+".gemmania.gimmickColor", 0)
	}

	/** Map保存
	 * @param field field
	 * @param prop Property file to save to
	 * @param id 任意のID
	 */
	private fun saveMap(field:Field, prop:CustomProperties, id:Int) {
		field.writeProperty(prop, id)
		prop.setProperty(id.toString()+".gemmania.limittimeStart", limittimeStart)
		prop.setProperty(id.toString()+".gemmania.stagetimeStart", stagetimeStart)
		prop.setProperty(id.toString()+".gemmania.stagebgm", stagebgm)
		prop.setProperty(id.toString()+".gemmania.gimmickMirror", gimmickMirror)
		prop.setProperty(id.toString()+".gemmania.gimmickRoll", gimmickRoll)
		prop.setProperty(id.toString()+".gemmania.gimmickBig", gimmickBig)
		prop.setProperty(id.toString()+".gemmania.gimmickXRay", gimmickXRay)
		prop.setProperty(id.toString()+".gemmania.gimmickColor", gimmickColor)
	}

	/** Load settings from property file
	 * @param prop Property file
	 */
	override fun loadSetting(prop:CustomProperties) {
		startstage = prop.getProperty("gemmania.startstage", 0)
		stageset = prop.getProperty("gemmania.stageset", -1)
		alwaysghost = prop.getProperty("gemmania.alwaysghost", true)
		always20g = prop.getProperty("gemmania.always20g", false)
		lvstopse = prop.getProperty("gemmania.lvstopse", true)
		showsectiontime = prop.getProperty("gemmania.showsectiontime", true)
		randomnext = prop.getProperty("gemmania.randomnext", false)
		trainingType = prop.getProperty("gemmania.trainingType", 0)
		startnextc = prop.getProperty("gemmania.startnextc", 0)
	}

	/** Save settings to property file
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("gemmania.startstage", startstage)
		prop.setProperty("gemmania.stageset", stageset)
		prop.setProperty("gemmania.alwaysghost", alwaysghost)
		prop.setProperty("gemmania.always20g", always20g)
		prop.setProperty("gemmania.lvstopse", lvstopse)
		prop.setProperty("gemmania.showsectiontime", showsectiontime)
		prop.setProperty("gemmania.randomnext", randomnext)
		prop.setProperty("gemmania.trainingType", trainingType)
		prop.setProperty("gemmania.startnextc", startnextc)
	}

	/** Update falling speed
	 * @param engine GameEngine
	 */
	private fun setSpeed(engine:GameEngine) {
		if(always20g)
			engine.speed.gravity = -1
		else {
			while(speedlevel>=tableGravityChangeLevel[gravityindex])
				gravityindex++
			engine.speed.gravity = tableGravityValue[gravityindex]
		}

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

		if(speedlevel>=100&&!alwaysghost) engine.ghost = false
	}

	/** Stage clearや time切れの判定
	 * @param engine GameEngine
	 */
	private fun checkStageEnd(engine:GameEngine) {
		if(clearflag||stagetimeNow<=0&&stagetimeStart>0&&engine.timerActive) {
			skipflag = false
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
	private fun getStageName(stageNumber:Int):String = if(stageNumber>=MAX_STAGE_NORMAL) "EX"+(stageNumber+1-MAX_STAGE_NORMAL) else ""+(stageNumber+1)

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
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
						startstage += change
						if(startstage<0) startstage = MAX_STAGE_TOTAL-1
						if(startstage>MAX_STAGE_TOTAL-1) startstage = 0
					}
					3, 4 -> {
						stageset += change
						if(stageset<0) stageset = 99
						if(stageset>99) stageset = 0
					}
				}
			}

			// 決定
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				when(menuCursor) {
					0 -> {
						editModeScreen = 2
						menuCursor = 0
						menuTime = 0
					}
					1 -> if(propStageSet!=null&&engine.field!=null) {
						loadMap(engine.field!!, propStageSet, startstage)
						engine.field!!.setAllSkin(engine.skin)
					}
					2 -> if(propStageSet!=null&&engine.field!=null) saveMap(engine.field!!, propStageSet, startstage)
					3 -> loadStageSet(stageset)
					4 -> saveStageSet(stageset)
				}
			}

			// Cancel
			if(engine.ctrl!!.isPress(Controller.BUTTON_D)&&engine.ctrl!!.isPress(Controller.BUTTON_E)) {
				editModeScreen = 0
				menuCursor = 0
				menuTime = 0
			}

			menuTime++
		} else if(editModeScreen==2) {
			// Up
			if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_UP)) {
				menuCursor--
				if(menuCursor<0) menuCursor = 4
				engine.playSE("cursor")
			}
			// Down
			if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
				menuCursor++
				if(menuCursor>4) menuCursor = 0
				engine.playSE("cursor")
			}

			// Configuration changes
			var change = 0
			if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_LEFT)) change = -1
			if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_RIGHT)) change = 1

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl!!.isPress(Controller.BUTTON_E)) m = 100
				if(engine.ctrl!!.isPress(Controller.BUTTON_F)) m = 1000

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
						if(stagebgm<0) stagebgm =BGM.count
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
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				if(menuCursor==0) {
					engine.enterFieldEdit()
					return true
				} else {
					editModeScreen = 1
					menuCursor = 0
					menuTime = 0
				}
			}

			// Cancel
			if(engine.ctrl!!.isPush(Controller.BUTTON_B)&&menuTime>=5) {
				editModeScreen = 1
				menuCursor = 0
				menuTime = 0
			}

			menuTime++
		} else if(!engine.owner.replayMode) {
			// Up
			if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_UP)) {
				menuCursor--
				if(menuCursor<0) menuCursor = 8
				engine.playSE("cursor")
			}
			// Down
			if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
				menuCursor++
				if(menuCursor>8) menuCursor = 0
				engine.playSE("cursor")
			}

			// Configuration changes
			var change = 0
			if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_LEFT)) change = -1
			if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_RIGHT)) change = 1

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						startstage += change
						if(startstage<0) startstage = MAX_STAGE_TOTAL-1
						if(startstage>MAX_STAGE_TOTAL-1) startstage = 0

						if(propStageSet==null) loadStageSet(stageset)
						loadMap(engine.field!!, propStageSet, startstage)
						engine.field!!.setAllSkin(engine.skin)
					}
					1 -> {
						stageset += change
						if(stageset<-1) stageset = 99
						if(stageset>99) stageset = -1

						loadStageSet(stageset)
						loadMap(engine.field!!, propStageSet, startstage)
						engine.field!!.setAllSkin(engine.skin)
					}
					2 -> alwaysghost = !alwaysghost
					3 -> always20g = !always20g
					4 -> lvstopse = !lvstopse
					5 -> showsectiontime = !showsectiontime
					6 -> randomnext = !randomnext
					7 -> {
						trainingType += change
						if(trainingType<0) trainingType = 2
						if(trainingType>2) trainingType = 0
					}
					8 -> {
						startnextc += change
						if(startnextc<0) startnextc = STRING_DEFAULT_NEXT_LIST.length-1
						if(startnextc>STRING_DEFAULT_NEXT_LIST.length-1) startnextc = 0
					}
				}
			}

			// 決定
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig)
				receiver.saveModeConfig(owner.modeConfig)
				return false
			}

			// Cancel
			if(engine.ctrl!!.isPush(Controller.BUTTON_B)) engine.quitflag = true

			// エディット
			if(engine.ctrl!!.isPush(Controller.BUTTON_D)) {
				if(stageset<0) stageset = 0

				loadStageSet(stageset)
				loadMap(engine.field!!, propStageSet, startstage)
				engine.field!!.setAllSkin(engine.skin)

				editModeScreen = 1
				menuCursor = 0
				menuTime = 0
			}

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60

		}// 普通のMenu
		// エディットMenu   stage 画面

		return true
	}

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(editModeScreen==1) {
			drawMenu(engine, playerID, receiver, 0, COLOR.GREEN, 0, "STAGE EDIT", "[PUSH A]", "LOAD STAGE", "["
				+getStageName(startstage)+"]", "SAVE STAGE", "["+getStageName(startstage)+"]", "LOAD", "[SET "+stageset
				+"]", "SAVE", "[SET $stageset]")

			receiver.drawMenuFont(engine, playerID, 0, 19, "EXIT-> D+E", COLOR.ORANGE)
		} else if(editModeScreen==2)
		// エディットMenu   stage 画面
			drawMenu(engine, playerID, receiver, 0, COLOR.GREEN, 0, "MAP EDIT", "[PUSH A]", "STAGE TIME", GeneralUtil.getTime(stagetimeStart.toFloat()), "LIMIT TIME", GeneralUtil.getTime(limittimeStart.toFloat()), "BGM", stagebgm.toString(), "MIRROR",
				if(gimmickMirror==0)
					"OFF"
				else
					gimmickMirror.toString())
		else {
			// 普通のMenu
			if(!engine.owner.replayMode) receiver.drawMenuFont(engine, playerID, 0, 19, "D:EDIT", COLOR.ORANGE)

			var strTrainingType = "OFF"
			if(trainingType==1) strTrainingType = "ON"
			if(trainingType==2) strTrainingType = "ON+RESET"
			drawMenu(engine, playerID, receiver, 0, COLOR.PINK, 0, "STAGE NO.", getStageName(startstage), "STAGE SET",
				if(stageset<0)
					"DEFAULT"
				else
					"EDIT $stageset", "FULL GHOST", GeneralUtil.getONorOFF(alwaysghost), "20G MODE", GeneralUtil.getONorOFF(always20g), "LVSTOPSE", GeneralUtil.getONorOFF(lvstopse), "SHOW STIME", GeneralUtil.getONorOFF(showsectiontime), "RANDOM", GeneralUtil.getONorOFF(randomnext), "TRAINING", strTrainingType, "NEXT COUNT", startnextc.toString())
		}
	}

	/* Ready画面の処理 */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) {
			if(!engine.readyDone) {
				loadStageSet(stageset)
				stage = startstage
				engine.nextPieceCount = startnextc

				if(!randomnext)
					engine.nextPieceArrayID = GeneralUtil.createNextPieceArrayFromNumberString(STRING_DEFAULT_NEXT_LIST)
			}

			startStage(engine)

			if(!engine.readyDone) limittimeNow = limittimeStart

		}
		return false
	}

	/* Ready画面の描画処理 */
	override fun renderReady(engine:GameEngine, playerID:Int) {
		if(engine.statc[0]>=engine.readyStart) {
			// トレーニング
			if(trainingType!=0) receiver.drawMenuFont(engine, playerID, 1, 5, "TRAINING", COLOR.GREEN)

			// STAGE XX
			if(stage>=MAX_STAGE_NORMAL) {
				receiver.drawMenuFont(engine, playerID, 0, 7, "EX STAGE ", COLOR.GREEN)
				receiver.drawMenuFont(engine, playerID, 9, 7, ""+(stage+1-MAX_STAGE_NORMAL))
			} else {
				receiver.drawMenuFont(engine, playerID, 1, 7, "STAGE", COLOR.GREEN)
				val strStage = String.format("%2s", getStageName(stage))
				receiver.drawMenuFont(engine, playerID, 7, 7, strStage)
			}
		}
	}

	/* Called at game start(2回目以降のReadyも含む) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		// X-RAY開始
		if(gimmickXRay>0) engine.itemXRayEnable = true
		// カラー開始
		if(gimmickColor>0) engine.itemColorEnable = true

		// BGM切り替え
		owner.bgmStatus.fadesw = false
		owner.bgmStatus.bgm = BGM.values[stagebgm]
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "GRAND BLOSSOM"+if(randomnext)
			" (RANDOM)"
		else
			"", COLOR.RED)

		receiver.drawScoreFont(engine, playerID, -1, -4*2, "DECORATION", scale = .5f)
		receiver.drawScoreDecorations(engine, playerID, 0, -3, 100, decoration)
		receiver.drawScoreDecorations(engine, playerID, 5, -4, 100, dectemp)
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(startstage==0&&!always20g&&trainingType==0&&startnextc==0&&stageset<0&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 5 else 3

				receiver.drawScoreFont(engine, playerID, 3, topY-1, "STAGE CLEAR TIME", COLOR.PINK, scale)
				val type = if(randomnext) 1 else 0

				for(i in 0 until RANKING_MAX) {
					var gcolor = COLOR.WHITE
					if(rankingAllClear[type][i]==1) gcolor = COLOR.GREEN
					if(rankingAllClear[type][i]==2) gcolor = COLOR.ORANGE

					receiver.drawScoreFont(engine, playerID, 0, topY+i, String.format("%2d", i+1), COLOR.YELLOW, scale)
					receiver.drawScoreFont(engine, playerID, 3, topY+i, getStageName(rankingStage[type][i]), gcolor, scale)
					receiver.drawScoreNum(engine, playerID, 9, topY+i, rankingClearPer[type][i].toString()+"%", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 15, topY+i, GeneralUtil.getTime(rankingTime[type][i].toFloat()), i==rankingRank, scale)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 2, "STAGE", COLOR.PINK)
			receiver.drawScoreFont(engine, playerID, 0, 3, getStageName(stage))
			if(gimmickMirror>0)
				receiver.drawScoreFont(engine, playerID, 0, 4, "MIRROR", COLOR.RED)
			else if(gimmickRoll>0)
				receiver.drawScoreFont(engine, playerID, 0, 4, "ROLL ROLL", COLOR.RED)
			else if(gimmickBig>0)
				receiver.drawScoreFont(engine, playerID, 0, 4, "DEATH BLOCK", COLOR.RED)
			else if(gimmickXRay>0)
				receiver.drawScoreFont(engine, playerID, 0, 4, "X-RAY", COLOR.RED)
			else if(gimmickColor>0) receiver.drawScoreFont(engine, playerID, 0, 4, "COLOR", COLOR.RED)

			receiver.drawScoreFont(engine, playerID, 0, 5, "REST", COLOR.PINK)
			receiver.drawScoreNum(engine, playerID, 0, 6, ""+rest)

			if(trainingType==0) {
				receiver.drawScoreFont(engine, playerID, 0, 8, "CLEAR", COLOR.PINK)
				receiver.drawScoreNum(engine, playerID, 0, 9, clearper.toString()+"%")
			} else {
				receiver.drawScoreFont(engine, playerID, 0, 8, "BEST TIME", COLOR.PINK)
				receiver.drawScoreNum(engine, playerID, 0, 9, GeneralUtil.getTime(trainingBestTime.toFloat()))
			}

			//  level
			receiver.drawScoreFont(engine, playerID, 0, 11, "LEVEL", COLOR.PINK)
			var tempLevel = speedlevel
			if(tempLevel<0) tempLevel = 0
			val strLevel = String.format("%3d", tempLevel)
			receiver.drawScoreNum(engine, playerID, 0, 12, strLevel)

			var speed = engine.speed.gravity/128
			if(engine.speed.gravity<0) speed = 40
			receiver.drawSpeedMeter(engine, playerID, 0, 13, speed)

			receiver.drawScoreNum(engine, playerID, 0, 14, String.format("%3d", nextseclv))

			//  stage Time
			if(stagetimeStart>0) {
				receiver.drawScoreFont(engine, playerID, 0, 16, "STAGE TIME", COLOR.PINK)
				receiver.drawScoreNum(engine, playerID, 0, 17, GeneralUtil.getTime(stagetimeNow.toFloat()), engine.timerActive
					&&stagetimeNow<600&&stagetimeNow%4==0, 2f)
			}

			// Time limit
			if(limittimeStart>0) {
				receiver.drawScoreFont(engine, playerID, 0, 19, "LIMIT TIME", COLOR.PINK)
				if(timeextendDisp>0)
					receiver.drawScoreNum(engine, playerID, 0, 22, "+$timeextendSeconds", engine.timerActive&&limittimeNow<600
						&&limittimeNow%4==0)

				receiver.drawScoreNum(engine, playerID, 0, 20, GeneralUtil.getTime(limittimeNow.toFloat()), engine.timerActive
					&&limittimeNow<600
					&&limittimeNow%4==0, 2f)
			}

			// Section Time
			if(showsectiontime&&sectionTime.isNotEmpty()) {
				val y = if(receiver.nextDisplayType==2) 4 else 2
				val x = if(receiver.nextDisplayType==2) 22 else 12
				val scale = if(receiver.nextDisplayType==2) .5f else 1f

				receiver.drawScoreFont(engine, playerID, x, y, "SECTION TIME", COLOR.PINK, scale)

				for(i in sectionTime.indices)
					if(sectionTime[i]!=0) {
						val strSeparator = if(i==stage&&engine.ending==0) "b" else " "

						val strSectionTime:String = when {
							sectionTime[i]==-1 -> String.format("%3s%s%s", getStageName(i), strSeparator, "FAILED")
							sectionTime[i]==-2 -> String.format("%3s%s%s", getStageName(i), strSeparator, "SKIPPED")
							else -> String.format("%3s%s%s", getStageName(i), strSeparator, GeneralUtil.getTime(sectionTime[i].toFloat()))
						}

						val pos = i-maxOf(stage-14, 0)

						if(pos>=0) receiver.drawScoreFont(engine, playerID, x, y+1+pos, strSectionTime, scale=scale)
					}

				if(receiver.nextDisplayType==2) {
					receiver.drawScoreFont(engine, playerID, 11, 19, "TOTAL", COLOR.PINK)
					receiver.drawScoreNum(engine, playerID, 11, 20, GeneralUtil.getTime(engine.statistics.time.toFloat()), 2f)
				} else {
					receiver.drawScoreFont(engine, playerID, 12, 19, "TOTAL TIME", COLOR.PINK)
					receiver.drawScoreNum(engine, playerID, 12, 20, GeneralUtil.getTime(engine.statistics.time.toFloat()), 2f)
				}
			}
		}
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		if(timeextendDisp>0) timeextendDisp--

		if(engine.gameActive&&engine.timerActive&&engine.ctrl!!.isPress(Controller.BUTTON_F)) {
			skipbuttonPressTime++

			if(skipbuttonPressTime>=60&&(stage<MAX_STAGE_NORMAL-1||trainingType!=0)&&limittimeNow>30*60
				&&!clearflag) {
				skipflag = true
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
			sectionTime[stage]++
		}

		// Time limit
		if(engine.gameActive&&engine.timerActive&&limittimeNow>0) {
			limittimeNow--

			// Time meter
			if(limittimeNow>=limittimeStart)
				engine.meterValue = receiver.getMeterMax(engine)
			else
				engine.meterValue = limittimeNow*receiver.getMeterMax(engine)/limittimeStart
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
	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		// 新規ピース出現時
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!lvupflag) {
			// Level up
			if(speedlevel<nextseclv-1) {
				speedlevel++
				if(speedlevel==nextseclv-1&&lvstopse) engine.playSE("levelstop")
			}
			setSpeed(engine)
		}
		if(engine.ending==0&&engine.statc[0]>0) lvupflag = false

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
	override fun onARE(engine:GameEngine, playerID:Int):Boolean {
		// 最後の frame
		if(engine.ending==0&&engine.statc[0]>=engine.statc[1]-1&&!lvupflag) {
			if(speedlevel<nextseclv-1) {
				speedlevel++
				if(speedlevel==nextseclv-1&&lvstopse) engine.playSE("levelstop")
			}
			setSpeed(engine)
			lvupflag = true
		}

		return false
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {
		// 実際に消えるLinescount(Big時半分にならない)
		val realLines = engine.field!!.lines

		if(realLines>=1&&engine.ending==0) {
			// 宝石消去
			val gemClears = engine.field!!.howManyGemClears
			if(gemClears>0) {
				rest -= gemClears
				if(rest<=0) clearflag = true
				limittimeNow += 60*gemClears
				timeextendSeconds = gemClears
				timeextendDisp = 120
			}

			// Level up
			var levelplus = lines
			if(lines==3) levelplus = 4
			if(lines>=4) levelplus = 6

			speedlevel += levelplus

			setSpeed(engine)

			if(speedlevel>998)
				speedlevel = 998
			else if(speedlevel>=nextseclv) {
				// Next Section
				engine.playSE("levelup")

				// Background切り替え
				owner.backgroundStatus.fadesw = true
				owner.backgroundStatus.fadecount = 0
				owner.backgroundStatus.fadebg = nextseclv/100

				// Update level for next section
				nextseclv += 100
			} else if(speedlevel==nextseclv-1&&lvstopse) engine.playSE("levelstop")

		}
	}

	/* Line clear処理が終わったときの処理 */
	override fun lineClearEnd(engine:GameEngine, playerID:Int):Boolean {
		checkStageEnd(engine)

		return false
	}

	/* Blockピースが固定されたときの処理(calcScoreの直後) */
	override fun pieceLocked(engine:GameEngine, playerID:Int, lines:Int) {
		// 固定 count+1
		thisStageTotalPieceLockCount++

		// ミラー
		if(gimmickMirror>0&&thisStageTotalPieceLockCount%gimmickMirror==0)
			engine.interruptItemNumber = GameEngine.INTERRUPTITEM_MIRROR

		//  stage 終了判定
		if(lines<=0) checkStageEnd(engine)
	}

	/** stage 終了画面の描画 */
	override fun onCustom(engine:GameEngine, playerID:Int):Boolean {
		// 最初の frame の処理
		if(engine.statc[0]==0) {
			// Sound effects
			if(clearflag)
				engine.playSE("stageclear")
			else
				engine.playSE("stagefail")

			// Cleared  stage +1
			if(clearflag) {
				clearstage++
				dectemp += 3
				if(stage>=MAX_STAGE_NORMAL) dectemp += 3
			}
			// クリア率計算
			trystage++
			clearper = clearstage*100/trystage

			// Time bonus
			timeextendStageClearSeconds = 0
			if(clearflag) {
				if(cleartime<10*60)
					timeextendStageClearSeconds = 10
				else if(cleartime<20*60) timeextendStageClearSeconds = 5

				if(stage==MAX_STAGE_NORMAL-1) timeextendStageClearSeconds += 60
			} else if(skipflag) timeextendStageClearSeconds = 30

			// 最終 stage を決定
			if(stage==MAX_STAGE_NORMAL-1)
				laststage = if(clearper<90)
					19 // クリア率が90%に満たない場合は stage 20で終了
				else if(clearper<100)
					22 // クリア率が90～99%はEX3まで
				else if(engine.statistics.time>5*3600)
					24 // クリア率が100%で5分超えている場合はEX5
				else
					MAX_STAGE_TOTAL-1 // クリア率が100%で5分以内ならEX7

			// BGM fadeout
			if((stage==MAX_STAGE_NORMAL-1||stage==laststage)&&trainingType==0) owner.bgmStatus.fadesw = true

			// ギミック解除
			engine.interruptItemNumber = GameEngine.INTERRUPTITEM_NONE
			engine.itemXRayEnable = false
			engine.itemColorEnable = false
			engine.resetFieldVisible()

			// Section Time設定
			if(!clearflag) {
				if(!skipflag)
					sectionTime[stage] = -1 // Out of time
				else
					sectionTime[stage] = -2 // スキップ
			} else
				sectionTime[stage] = cleartime

			// トレーニングでのベストTime
			if(trainingType!=0&&clearflag&&(cleartime<trainingBestTime||trainingBestTime<0)) trainingBestTime = cleartime

		}

		// Time limitが増える演出
		if(engine.statc[1]<timeextendStageClearSeconds*60) {
			if(timeextendStageClearSeconds<30)
				engine.statc[1] += 4
			else if(timeextendStageClearSeconds<60)
				engine.statc[1] += 10
			else
				engine.statc[1] += 30

			// Time meter
			var limittimeTemp = limittimeNow+engine.statc[1]
			if(skipflag) limittimeTemp = limittimeNow-engine.statc[1]

			if(limittimeTemp>=limittimeStart)
				engine.meterValue = receiver.getMeterMax(engine)
			else
				engine.meterValue = limittimeTemp*receiver.getMeterMax(engine)/limittimeStart

			engine.meterColor = GameEngine.METER_COLOR_LIMIT
		}

		// Next 画面へ
		if(engine.statc[0]>=300||engine.ctrl!!.isPush(Controller.BUTTON_A)) {
			// Training
			if(trainingType!=0) {
				if(clearflag) limittimeNow += timeextendStageClearSeconds*60
				if(skipflag) limittimeNow -= timeextendStageClearSeconds*60
				if(trainingType==2) engine.nextPieceCount = continueNextPieceCount
				engine.stat = GameEngine.Status.READY
				engine.resetStatc()
			} else if(stage>=laststage) {
				allclear = if(stage>=MAX_STAGE_TOTAL-1) 2 else 1
				engine.ending = 1
				engine.gameEnded()
				engine.stat = GameEngine.Status.ENDINGSTART
				engine.resetStatc()
			} else {
				stage++
				if(clearflag) limittimeNow += timeextendStageClearSeconds*60
				if(skipflag) limittimeNow -= timeextendStageClearSeconds*60
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
	override fun renderCustom(engine:GameEngine, playerID:Int) {
		if(engine.statc[0]<1) return

		// STAGE XX
		receiver.drawMenuFont(engine, playerID, 1, 2, "STAGE", COLOR.GREEN)
		val strStage = String.format("%2s", getStageName(stage))
		receiver.drawMenuFont(engine, playerID, 7, 2, strStage)

		if(clearflag) {
			// クリア
			receiver.drawMenuFont(engine, playerID, 2, 4, "CLEAR!", if(engine.statc[0]%2==0)
				COLOR.ORANGE
			else
				COLOR.WHITE)

			receiver.drawMenuFont(engine, playerID, 0, 7, "LIMIT TIME", COLOR.PINK)
			receiver.drawMenuFont(engine, playerID, 1, 8, GeneralUtil.getTime((limittimeNow+engine.statc[1]).toFloat()), if(engine.statc[0]%2==0&&engine.statc[1]<timeextendStageClearSeconds*60)
				COLOR.ORANGE
			else
				COLOR.WHITE)

			receiver.drawMenuFont(engine, playerID, 2, 10, "EXTEND", COLOR.PINK)
			receiver.drawMenuFont(engine, playerID, 2, 11, timeextendStageClearSeconds.toString()+" SEC.")

			receiver.drawMenuFont(engine, playerID, 0, 13, "CLEAR TIME", COLOR.PINK)
			receiver.drawMenuFont(engine, playerID, 1, 14, GeneralUtil.getTime(cleartime.toFloat()))

			receiver.drawMenuFont(engine, playerID, 0, 16, "TOTAL TIME", COLOR.PINK)
			receiver.drawMenuFont(engine, playerID, 1, 17, GeneralUtil.getTime(engine.statistics.time.toFloat()))
		} else if(skipflag) {
			// スキップ
			receiver.drawMenuFont(engine, playerID, 1, 4, "SKIPPED")
			receiver.drawMenuFont(engine, playerID, 1, 5, "-30 SEC.")

			receiver.drawMenuFont(engine, playerID, 0, 10, "LIMIT TIME", COLOR.PINK)
			receiver.drawMenuFont(engine, playerID, 1, 11, GeneralUtil.getTime((limittimeNow-engine.statc[1]).toFloat()), if(engine.statc[0]%2==0&&engine.statc[1]<30*60)
				COLOR.RED
			else
				COLOR.WHITE)

			if(trainingType==0) {
				receiver.drawMenuFont(engine, playerID, 0, 13, "CLEAR PER.", COLOR.PINK)
				receiver.drawMenuFont(engine, playerID, 3, 14, clearper.toString()+"%")
			}

			receiver.drawMenuFont(engine, playerID, 0, 16, "TOTAL TIME", COLOR.PINK)
			receiver.drawMenuFont(engine, playerID, 1, 17, GeneralUtil.getTime(engine.statistics.time.toFloat()))
		} else if(stagetimeNow<=0&&stagetimeStart>0) {
			// Timeアップ
			receiver.drawMenuFont(engine, playerID, 1, 4, "TIME UP!")
			receiver.drawMenuFont(engine, playerID, 1, 5, "TRY NEXT")

			receiver.drawMenuFont(engine, playerID, 0, 10, "LIMIT TIME", COLOR.PINK)
			receiver.drawMenuFont(engine, playerID, 1, 11, GeneralUtil.getTime(limittimeNow.toFloat()))

			if(trainingType==0) {
				receiver.drawMenuFont(engine, playerID, 0, 13, "CLEAR PER.", COLOR.PINK)
				receiver.drawMenuFont(engine, playerID, 3, 14, clearper.toString()+"%")
			}

			receiver.drawMenuFont(engine, playerID, 0, 16, "TOTAL TIME", COLOR.PINK)
			receiver.drawMenuFont(engine, playerID, 1, 17, GeneralUtil.getTime(engine.statistics.time.toFloat()))
		}
	}

	/* Called at game over(主にコンティニュー画面) */
	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		// コンティニュー画面
		if(engine.ending==0&&!noContinue) {
			if(engine.statc[0]==0) {
				engine.playSE("died")
				engine.playSE("shutter")
				owner.bgmStatus.bgm = BGM.SILENT

				engine.timerActive = false
				engine.blockShowOutlineOnly = false

				engine.itemXRayEnable = false
				engine.itemColorEnable = false
				engine.interruptItemNumber = GameEngine.INTERRUPTITEM_NONE

				engine.resetFieldVisible()

				engine.allowTextRenderByReceiver = false // GAMEOVER表示抑制
			}
			if(engine.statc[0]<engine.field!!.height+1) {
				// field灰色化
				for(i in 0 until engine.field!!.width)
					if(engine.field!!.getBlockColor(i, engine.statc[0])!=Block.BLOCK_COLOR_NONE) {
						val blk = engine.field!!.getBlock(i, engine.statc[0])

						if(blk!=null) {
							blk.cint = Block.BLOCK_COLOR_GRAY
							blk.darkness = 0f
							blk.elapsedFrames = -1
						}
					}
				engine.statc[0]++
			} else if(engine.statc[0]<engine.field!!.height+1+600) {
				// コンティニュー選択
				if(engine.ctrl!!.isPush(Controller.BUTTON_UP)||engine.ctrl!!.isPush(Controller.BUTTON_DOWN)) {
					engine.statc[1]++
					if(engine.statc[1]>1) engine.statc[1] = 0
					engine.playSE("cursor")
				}
				// 決定
				if(engine.ctrl!!.isPush(Controller.BUTTON_A)) {
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
						engine.statc[0] = engine.field!!.height+1+600

				} else
					engine.statc[0]++

			} else if(engine.statc[0]>=engine.field!!.height+1+600) {
				// ＼(^o^)／ｵﾜﾀ
				noContinue = true
				engine.allowTextRenderByReceiver = true // GAMEOVER表示抑制解除
				engine.resetStatc()
				decoration += dectemp-3
			}

			return true
		}

		return false
	}

	/* game over時の描画処理(主にコンティニュー画面) */
	override fun renderGameOver(engine:GameEngine, playerID:Int) {
		if(engine.ending==0&&!noContinue)
			if(engine.statc[0]>=engine.field!!.height+1&&engine.statc[0]<engine.field!!.height+1+600) {
				receiver.drawMenuFont(engine, playerID, 1, 7, "CONTINUE?", COLOR.PINK)

				receiver.drawMenuFont(engine, playerID, 3, 9+engine.statc[1]*2, "b", COLOR.RED)
				receiver.drawMenuFont(engine, playerID, 4, 9, "YES", engine.statc[1]==0)
				receiver.drawMenuFont(engine, playerID, 4, 11, "NO", engine.statc[1]==1)

				val t = engine.field!!.height+1+600-engine.statc[0]
				receiver.drawMenuFont(engine, playerID, 2, 13, "TIME "+(t-1)/60, COLOR.GREEN)

				receiver.drawMenuFont(engine, playerID, 0, 16, "TOTAL TIME", COLOR.PINK)
				receiver.drawMenuFont(engine, playerID, 1, 17, GeneralUtil.getTime(engine.statistics.time.toFloat()))

				if(trainingType==0) receiver.drawMenuFont(engine, playerID, 0, 18, "+2 MINUTES", COLOR.RED)
			}

	}

	/* 結果画面の処理 */
	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_UP)) {
			engine.statc[1]--
			if(engine.statc[1]<0) engine.statc[1] = 2
			receiver.playSE("change")
		}
		if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
			engine.statc[1]++
			if(engine.statc[1]>2) engine.statc[1] = 0
			receiver.playSE("change")
		}

		return false
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 0, "kn PAGE"+(engine.statc[1]+1)+"/3", COLOR.RED)

		if(engine.statc[1]==0) {
			var gcolor = COLOR.WHITE
			if(allclear==1) gcolor = COLOR.GREEN
			if(allclear==2) gcolor = COLOR.ORANGE

			receiver.drawMenuFont(engine, playerID, 0, 2, "STAGE", COLOR.PINK)
			val strStage = String.format("%10s", getStageName(stage))
			receiver.drawMenuFont(engine, playerID, 0, 3, strStage, gcolor)

			drawResult(engine, playerID, receiver, 4, COLOR.PINK, "CLEAR", String.format("%9d%%", clearper))
			drawResultStats(engine, playerID, receiver, 6, COLOR.PINK, AbstractMode.Statistic.LINES, AbstractMode.Statistic.PIECE, AbstractMode.Statistic.TIME)
			drawResultRank(engine, playerID, receiver, 12, COLOR.PINK, rankingRank)
		} else {
			receiver.drawMenuFont(engine, playerID, 0, 2, "SECTION"+engine.statc[1]+"/2", COLOR.PINK)

			var i = if(engine.statc[1]==1) 0 else 15
			val y = if(engine.statc[1]==1) 3 else -12
			while(i<if(engine.statc[1]==1)
					15
				else
					sectionTime.size) {
				if(sectionTime[i]!=0)
					if(sectionTime[i]==-1)
						receiver.drawMenuNum(engine, playerID, 2, i+y, "FAILED", COLOR.RED)
					else if(sectionTime[i]==-2)
						receiver.drawMenuNum(engine, playerID, 2, i+y, "SKIPPED", COLOR.PURPLE)
					else
						receiver.drawMenuNum(engine, playerID, 2, i+y, GeneralUtil.getTime(sectionTime[i].toFloat()))
				i++
			}
		}
	}

	/* リプレイ保存 */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)
		prop.setProperty("gemmania.version", version)
		prop.setProperty("gemmania.result.stage", stage)
		prop.setProperty("gemmania.result.clearper", clearper)
		prop.setProperty("gemmania.result.allclear", allclear)

		engine.statistics.level = stage
		engine.statistics.levelDispAdd = 1
		engine.statistics.score = clearper
		engine.statistics.writeProperty(prop, playerID)

		// Update rankings
		if(!owner.replayMode&&startstage==0&&trainingType==0&&
			startnextc==0&&stageset<0&&!always20g&&engine.ai==null) {
			updateRanking(if(randomnext) 1 else 0, stage, clearper, engine.statistics.time, allclear)

			if(rankingRank!=-1) {
				saveRanking(owner.modeConfig, engine.ruleopt.strRuleName)
				receiver.saveModeConfig(owner.modeConfig)
			}
		}
	}

	/** Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	private fun loadRanking(prop:CustomProperties?, ruleName:String) {
		for(type in 0 until RANKING_TYPE)
			for(i in 0 until RANKING_MAX) {
				rankingStage[type][i] = prop!!.getProperty("gemmania.ranking.$ruleName.$type.stage.$i", 0)
				rankingClearPer[type][i] = prop.getProperty("gemmania.ranking.$ruleName.$type.clearper.$i", 0)
				rankingTime[type][i] = prop.getProperty("gemmania.ranking.$ruleName.$type.time.$i", 0)
				rankingAllClear[type][i] = prop.getProperty("gemmania.ranking.$ruleName.$type.allclear.$i", 0)
			}
		decoration = prop!!.getProperty("decoration", 0)
	}

	/** Save rankings to property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	private fun saveRanking(prop:CustomProperties?, ruleName:String) {
		for(type in 0 until RANKING_TYPE)
			for(i in 0 until RANKING_MAX) {
				prop!!.setProperty("gemmania.ranking.$ruleName.$type.stage.$i", rankingStage[type][i])
				prop.setProperty("gemmania.ranking.$ruleName.$type.clearper.$i", rankingClearPer[type][i])
				prop.setProperty("gemmania.ranking.$ruleName.$type.time.$i", rankingTime[type][i])
				prop.setProperty("gemmania.ranking.$ruleName.$type.allclear.$i", rankingAllClear[type][i])
			}
		prop!!.setProperty("decoration", decoration)
	}

	/** Update rankings
	 * @param type Game type
	 * @param stg stage
	 * @param clper クリア率
	 * @param time Time
	 * @param clear 完全クリア flag
	 */
	private fun updateRanking(type:Int, stg:Int, clper:Int, time:Int, clear:Int) {
		rankingRank = checkRanking(type, stg, clper, time, clear)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingStage[type][i] = rankingStage[type][i-1]
				rankingClearPer[type][i] = rankingClearPer[type][i-1]
				rankingTime[type][i] = rankingTime[type][i-1]
				rankingAllClear[type][i] = rankingAllClear[type][i-1]
			}

			// Add new data
			rankingStage[type][rankingRank] = stg
			rankingClearPer[type][rankingRank] = clper
			rankingTime[type][rankingRank] = time
			rankingAllClear[type][rankingRank] = clear
		}
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
			if(clear>rankingAllClear[type][i])
				return i
			else if(clear==rankingAllClear[type][i]&&stg>rankingStage[type][i])
				return i
			else if(clear==rankingAllClear[type][i]&&stg==rankingStage[type][i]&&clper>rankingClearPer[type][i])
				return i
			else if(clear==rankingAllClear[type][i]&&stg==rankingStage[type][i]&&clper==rankingClearPer[type][i]&&
				time<rankingTime[type][i])
				return i

		return -1
	}

	companion object {
		/** Log */
		internal val log = Logger.getLogger(GrandBlossom::class.java)

		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Maximum stage count */
		private const val MAX_STAGE_TOTAL = 27

		/** Normal stage count */
		private const val MAX_STAGE_NORMAL = 20

		/** NEXT list */
		private const val STRING_DEFAULT_NEXT_LIST = "1052463015240653120563402534162340621456034251036420314526014362045136455062150461320365204631546310"+
			"6451324023650143620435621456302513025430312603452013625026345012660132450346213462054360143260534215"+
			"0621543621435624013542130562345123641230462134502613542"

		/** 落下速度 table */
		private val tableGravityValue = intArrayOf(4, 32, 64, 96, 128, 160, 192, 224, 256, 512, 768, 1024, 768, -1)

		/** 落下速度が変わる level */
		private val tableGravityChangeLevel = intArrayOf(20, 30, 33, 36, 39, 43, 47, 51, 100, 130, 160, 250, 300, 10000)

		/** Number of ranking typesのcount */
		private const val RANKING_TYPE = 2

		/** Number of entries in rankings */
		private const val RANKING_MAX = 10
	}
}
