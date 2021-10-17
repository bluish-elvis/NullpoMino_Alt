/*
 * Copyright (c) 2010-2021, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
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
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import kotlin.random.Random

/** VS-BATTLE Mode */
class VSBattleMode:AbstractMode() {

	/** garbage blockType of */
	private var garbageType = IntArray(0)
	private val garbageStyle get() = garbageType.map {GarbageStyle.values()[it]}

	/** Rate of change of garbage holes */
	private var messiness:Array<IntArray> = Array(0) {IntArray(0)}

	/** Allow garbage countering */
	private var garbageCounter = BooleanArray(0)

	/** Allow garbage blocking */
	private var garbageBlocking = BooleanArray(0)

	/** Has accumulated garbage blockOfcount */
	private val garbage:IntArray get() = (0 until players).map {p -> garbageEntries[p].sumOf {it.lines}}.toIntArray()

	/** Had sent garbage blockOfcount */
	private val garbageSent:IntArray get() = owner.engine.map {it.statistics.attacks}.toIntArray()

	/** Had sent garbage per minutes */
	private val attackSpd:FloatArray get() = owner.engine.map {it.statistics.apm}.toFloatArray()

	/** VS Score */
	private val score:FloatArray get() = owner.engine.map {it.statistics.vs}.toFloatArray()

	/** Had guard garbage blockOfcount */
	private var garbageGuard = IntArray(0)

	/** Last garbage hole position */
	private var lastHole = IntArray(0)

	/** Most recent scoring eventInCombocount */
	private var lastcombo = IntArray(0)

	/** Most recent scoring eventPeace inID */
	private var lastpiece = IntArray(0)

	/** UseBGM */
	private var bgmno = 0

	/** Flag for types of Twisters allowed (0=none, 1=normal, 2=all spin) */
	private var twistEnableType = IntArray(0)

	/** Old flag for allowing Twisters */
	private var enableTwist = BooleanArray(0)

	/** Immobile EZ spin */
	private var twistEnableEZ = BooleanArray(0)

	/** B2B Type (0=OFF 1=ON 2=ON+Separated-garbage) */
	private var b2bType = IntArray(0)

	/** Flag for Split chains b2b */
	private var splitb2b = BooleanArray(0)

	/** Flag for enabling combos */
	private var enableCombo = BooleanArray(0)

	/** Big */
	private var big = BooleanArray(0)

	/** Sound effects ON/OFF */
	private var enableSE = BooleanArray(0)

	/** HurryupSeconds before the startcount(-1InHurryupNo) */
	private var hurryupSeconds = IntArray(0)

	/** HurryupTimes afterBlockDo you run up the floor every time you put the */
	private var hurryupInterval = IntArray(0)

	/** MapUse flag */
	private var useMap = BooleanArray(0)

	/** UseMapSet number */
	private var mapSet = IntArray(0)

	/** Map number(-1Random in) */
	private var mapNumber = IntArray(0)

	/** Last preset number used */
	private var presetNumber = IntArray(0)

	/** True if display detailed stats */
	private var showStats = false

	/** Winner */
	private var winnerID = 0

	/** I was sent from the enemygarbage blockA list of */
	private var garbageEntries:Array<MutableList<GarbageEntry>> = emptyArray()

	/** HurryupAfterBlockI put count */
	private var hurryupCount = IntArray(0)

	/** MapSets ofProperty file */
	private var propMap:Array<CustomProperties?> = emptyArray()

	/** MaximumMap number */
	private var mapMaxNo = IntArray(0)

	/** For backupfield (MapUsed to save the replay) */
	private var fldBackup:Array<Field?> = emptyArray()

	/** MapRan for selectioncount */
	private var randMap:Random? = null

	/** Win count for each player */
	private var winCount = IntArray(0)

	/** Version */
	private var version = 0

	/* Mode name */
	override val name = "VS:Face to Face"

	override val isVSMode:Boolean
		get() = true

	/* Number of players */
	override val players:Int
		get() = MAX_PLAYERS

	/* Mode initialization */
	override fun modeInit(manager:GameManager) {
		owner = manager

		garbageType = IntArray(MAX_PLAYERS)
		messiness = Array(MAX_PLAYERS) {IntArray(2)}
		garbageCounter = BooleanArray(MAX_PLAYERS)
		garbageBlocking = BooleanArray(MAX_PLAYERS)
		garbageGuard = IntArray(MAX_PLAYERS)
		lastHole = IntArray(MAX_PLAYERS)
		lastcombo = IntArray(MAX_PLAYERS)
		lastpiece = IntArray(MAX_PLAYERS)
		bgmno = 0
		twistEnableType = IntArray(MAX_PLAYERS)
		enableTwist = BooleanArray(MAX_PLAYERS)
		twistEnableEZ = BooleanArray(MAX_PLAYERS)
		b2bType = IntArray(MAX_PLAYERS)
		splitb2b = BooleanArray(MAX_PLAYERS)
		enableCombo = BooleanArray(MAX_PLAYERS)
		big = BooleanArray(MAX_PLAYERS)
		enableSE = BooleanArray(MAX_PLAYERS)
		hurryupSeconds = IntArray(MAX_PLAYERS)
		hurryupInterval = IntArray(MAX_PLAYERS)
		useMap = BooleanArray(MAX_PLAYERS)
		mapSet = IntArray(MAX_PLAYERS)
		mapNumber = IntArray(MAX_PLAYERS)
		presetNumber = IntArray(MAX_PLAYERS)
		garbageEntries = Array(MAX_PLAYERS) {mutableListOf()}
		hurryupCount = IntArray(MAX_PLAYERS)
		propMap = arrayOfNulls(MAX_PLAYERS)
		mapMaxNo = IntArray(MAX_PLAYERS)
		fldBackup = arrayOfNulls(MAX_PLAYERS)
		randMap = Random.Default
		winCount = IntArray(MAX_PLAYERS)
		winnerID = -1
	}

	/** Read speed presets from [prop]
	 * @param engine GameEngine
	 * @param preset Preset number
	 */
	private fun loadPreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		engine.speed.gravity = prop.getProperty("vsbattle.gravity.$preset", 4)
		engine.speed.denominator = prop.getProperty("vsbattle.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("vsbattle.are.$preset", 2)
		engine.speed.areLine = prop.getProperty("vsbattle.areLine.$preset", 3)
		engine.speed.lineDelay = prop.getProperty("vsbattle.lineDelay.$preset", 2)
		engine.speed.lockDelay = prop.getProperty("vsbattle.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("vsbattle.das.$preset", 14)
	}

	/** Save speed presets into [prop]
	 * @param engine GameEngine
	 * @param preset Preset number
	 */
	private fun savePreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		prop.setProperty("vsbattle.gravity.$preset", engine.speed.gravity)
		prop.setProperty("vsbattle.denominator.$preset", engine.speed.denominator)
		prop.setProperty("vsbattle.are.$preset", engine.speed.are)
		prop.setProperty("vsbattle.areLine.$preset", engine.speed.areLine)
		prop.setProperty("vsbattle.lineDelay.$preset", engine.speed.lineDelay)
		prop.setProperty("vsbattle.lockDelay.$preset", engine.speed.lockDelay)
		prop.setProperty("vsbattle.das.$preset", engine.speed.das)
	}

	/** Load settings into [engine] from [prop] not related to speeds */
	private fun loadOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val playerID = engine.playerID
		bgmno = prop.getProperty("vsbattle.bgmno", 0)
		garbageType[playerID] = prop.getProperty("vsbattle.garbageType.p$playerID", 0)
		messiness[playerID] = prop.getProperty("vsbattle.messiness.p$playerID", intArrayOf(90, 30))
		garbageCounter[playerID] = prop.getProperty("vsbattle.garbageCounter.p$playerID", true)
		garbageBlocking[playerID] = prop.getProperty("vsbattle.garbageBlocking.p$playerID", true)
		twistEnableType[playerID] = prop.getProperty("vsbattle.twistEnableType.p$playerID", 1)
		enableTwist[playerID] = prop.getProperty("vsbattle.enableTwist.p$playerID", true)
		twistEnableEZ[playerID] = prop.getProperty("vsbattle.twistEnableEZ.p$playerID", false)
		b2bType[playerID] = prop.getProperty("vsbattle.b2bType.p$playerID", 1)
		splitb2b[playerID] = prop.getProperty("vsbattle.splitb2b.p$playerID", true)
		enableCombo[playerID] = prop.getProperty("vsbattle.enableCombo.p$playerID", true)
		big[playerID] = prop.getProperty("vsbattle.big.p$playerID", false)
		enableSE[playerID] = prop.getProperty("vsbattle.enableSE.p$playerID", true)
		hurryupSeconds[playerID] = prop.getProperty("vsbattle.hurryupSeconds.p$playerID", -1)
		hurryupInterval[playerID] = prop.getProperty("vsbattle.hurryupInterval.p$playerID", 5)
		useMap[playerID] = prop.getProperty("vsbattle.useMap.p$playerID", false)
		mapSet[playerID] = prop.getProperty("vsbattle.mapSet.p$playerID", 0)
		mapNumber[playerID] = prop.getProperty("vsbattle.mapNumber.p$playerID", -1)
		presetNumber[playerID] = prop.getProperty("vsbattle.presetNumber.p$playerID", 0)
		showStats = prop.getProperty("vsbattle.showStats", true)
	}

	/** Save settings from [engine] into [prop] not related to speeds */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val playerID = engine.playerID
		prop.setProperty("vsbattle.bgmno", bgmno)
		prop.setProperty("vsbattle.garbageType.p$playerID", garbageType[playerID])
		prop.setProperty("vsbattle.messiness.p$playerID", messiness[playerID])
		prop.setProperty("vsbattle.garbageCounter.p$playerID", garbageCounter[playerID])
		prop.setProperty("vsbattle.garbageBlocking.p$playerID", garbageBlocking[playerID])
		prop.setProperty("vsbattle.twistEnableType.p$playerID", twistEnableType[playerID])
		prop.setProperty("vsbattle.enableTwist.p$playerID", enableTwist[playerID])
		prop.setProperty("vsbattle.twistEnableEZ.p$playerID", twistEnableEZ[playerID])
		prop.setProperty("vsbattle.b2bType.p$playerID", b2bType[playerID])
		prop.setProperty("vsbattle.splitb2b.p$playerID", splitb2b[playerID])
		prop.setProperty("vsbattle.enableCombo.p$playerID", enableCombo[playerID])
		prop.setProperty("vsbattle.big.p$playerID", big[playerID])
		prop.setProperty("vsbattle.enableSE.p$playerID", enableSE[playerID])
		prop.setProperty("vsbattle.hurryupSeconds.p$playerID", hurryupSeconds[playerID])
		prop.setProperty("vsbattle.hurryupInterval.p$playerID", hurryupInterval[playerID])
		prop.setProperty("vsbattle.useMap.p$playerID", useMap[playerID])
		prop.setProperty("vsbattle.mapSet.p$playerID", mapSet[playerID])
		prop.setProperty("vsbattle.mapNumber.p$playerID", mapNumber[playerID])
		prop.setProperty("vsbattle.presetNumber.p$playerID", presetNumber[playerID])
		prop.setProperty("vsbattle.showStats", showStats)
	}

	/** MapRead into #[id]:[field] from [prop] */
	private fun loadMap(field:Field, prop:CustomProperties, id:Int) {
		field.reset()
		//field.readProperty(prop, id);
		field.stringToField(prop.getProperty("values.$id", ""))
		field.setAllAttribute(true, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
		field.setAllAttribute(false, Block.ATTRIBUTE.SELF_PLACED)
	}

	/** MapSave from #[id]:[field] into [prop] */
	private fun saveMap(field:Field, prop:CustomProperties, id:Int) {
		//field.writeProperty(prop, id);
		prop.setProperty("values.$id", field.fieldToString())
	}

	/** For previewMapRead
	 * @param engine GameEngine
	 * @param playerID Player number
	 * @param id MapID
	 * @param forceReload trueWhen youMapForce Reload the file
	 */
	private fun loadMapPreview(engine:GameEngine, playerID:Int, id:Int, forceReload:Boolean) {
		if(propMap[playerID]==null||forceReload) {
			mapMaxNo[playerID] = 0
			propMap[playerID] = receiver.loadProperties("config/map/vsbattle/${mapSet[playerID]}.map")
		}

		propMap[playerID]?.let {
			mapMaxNo[playerID] = it.getProperty("values.maxMapNumber", 0)
			engine.createFieldIfNeeded()
			loadMap(engine.field, it, id)
			engine.field.setAllSkin(engine.skin)
		} ?: engine.field.reset()
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		if(playerID==1) {
			engine.randSeed = owner.engine[0].randSeed
			engine.random = Random(owner.engine[0].randSeed)
		}

		engine.framecolor = PLAYER_COLOR_FRAME[playerID]
		engine.ruleOpt.lockresetLimitMove = engine.ruleOpt.lockresetLimitMove.let {if(it<0) 30 else minOf(it, 30)}
		engine.ruleOpt.lockresetLimitRotate = engine.ruleOpt.lockresetLimitRotate.let {if(it<0) 20 else minOf(it, 20)}
		garbageSent[playerID] = 0
		lastHole[playerID] = -1
		lastcombo[playerID] = 0

		garbageEntries[playerID].clear()

		hurryupCount[playerID] = 0

		if(!engine.owner.replayMode) {
			version = CURRENT_VERSION
			loadOtherSetting(engine, engine.owner.modeConfig)
			loadPreset(engine, engine.owner.modeConfig, -1-playerID)
		} else {
			version = owner.replayProp.getProperty("vsbattle.version", 0)
			loadOtherSetting(engine, engine.owner.replayProp)
			loadPreset(engine, engine.owner.replayProp, -1-playerID)
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode&&engine.statc[4]==0) {
			// Configuration changes
			val change = updateCursor(engine, 27, playerID)

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl.isPress(Controller.BUTTON_E)) m = 100
				if(engine.ctrl.isPress(Controller.BUTTON_F)) m = 1000

				when(menuCursor) {
					0 -> engine.speed.gravity = rangeCursor(engine.speed.gravity+change*m, -1, 99999)
					1 -> engine.speed.denominator = rangeCursor(change*m, -1, 99999)
					2 -> engine.speed.are = rangeCursor(engine.speed.are+change, 0, 99)
					3 -> engine.speed.areLine = rangeCursor(engine.speed.areLine+change, 0, 99)
					4 -> engine.speed.lineDelay = rangeCursor(engine.speed.lineDelay+change, 0, 99)
					5 -> engine.speed.lockDelay = rangeCursor(engine.speed.lockDelay+change, 0, 99)
					6 -> engine.speed.das = rangeCursor(engine.speed.das+change, 0, 99)
					7, 8 -> presetNumber[playerID] = rangeCursor(presetNumber[playerID]+change, 0, 99)
					9 -> garbageType[playerID] = rangeCursor(garbageType[playerID]+change, 0, GarbageStyle.values().size-1)
					10 -> messiness[playerID][0] = rangeCursor(messiness[playerID][0]+change, 0, 100)
					11 -> messiness[playerID][1] = rangeCursor(messiness[playerID][1]+change, 0, 100)
					12 -> garbageCounter[playerID] = !garbageCounter[playerID]
					13 -> garbageBlocking[playerID] = !garbageBlocking[playerID]
					14 -> {
						twistEnableType[playerID] += change
						if(twistEnableType[playerID]<0) twistEnableType[playerID] = 2
						if(twistEnableType[playerID]>2) twistEnableType[playerID] = 0
					}
					15 -> twistEnableEZ[playerID] = !twistEnableEZ[playerID]
					16 -> {
						b2bType[playerID] += change
						if(b2bType[playerID]<0) b2bType[playerID] = 2
						if(b2bType[playerID]>2) b2bType[playerID] = 0
					}
					17 -> splitb2b[playerID] = !splitb2b[playerID]
					18 -> enableCombo[playerID] = !enableCombo[playerID]
					19 -> big[playerID] = !big[playerID]
					20 -> enableSE[playerID] = !enableSE[playerID]
					21 -> {
						hurryupSeconds[playerID] += change
						if(hurryupSeconds[playerID]<-1) hurryupSeconds[playerID] = 300
						if(hurryupSeconds[playerID]>300) hurryupSeconds[playerID] = -1
					}
					22 -> {
						hurryupInterval[playerID] += change
						if(hurryupInterval[playerID]<1) hurryupInterval[playerID] = 99
						if(hurryupInterval[playerID]>99) hurryupInterval[playerID] = 1
					}
					23 -> bgmno = rangeCursor(bgmno+change, 0, BGM.count-1)
					24 -> showStats = !showStats
					25 -> {
						useMap[playerID] = !useMap[playerID]
						if(!useMap[playerID]) {
							if(engine.field!=null) engine.field.reset()
						} else
							loadMapPreview(engine, playerID, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
					}
					26 -> {
						mapSet[playerID] += change
						if(mapSet[playerID]<0) mapSet[playerID] = 99
						if(mapSet[playerID]>99) mapSet[playerID] = 0
						if(useMap[playerID]) {
							mapNumber[playerID] = -1
							loadMapPreview(engine, playerID, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
						}
					}
					27 -> if(useMap[playerID]) {
						mapNumber[playerID] += change
						if(mapNumber[playerID]<-1) mapNumber[playerID] = mapMaxNo[playerID]-1
						if(mapNumber[playerID]>mapMaxNo[playerID]-1) mapNumber[playerID] = -1
						loadMapPreview(engine, playerID, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
					} else
						mapNumber[playerID] = -1
				}
			}

			// 決定
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				when(menuCursor) {
					7 -> loadPreset(engine, owner.modeConfig, presetNumber[playerID])
					8 -> {
						savePreset(engine, owner.modeConfig, presetNumber[playerID])
						owner.saveModeConfig()
					}
					else -> {
						saveOtherSetting(engine, owner.modeConfig)
						savePreset(engine, owner.modeConfig, -1-playerID)
						owner.saveModeConfig()
						engine.statc[4] = 1
					}
				}
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitflag = true

			// プレビュー用Map読み込み
			if(useMap[playerID]&&menuTime==0)
				loadMapPreview(engine, playerID, if(mapNumber[playerID]<0)
					0
				else
					mapNumber[playerID], true)

			// Random values preview
			if(useMap[playerID]&&propMap[playerID]!=null&&mapNumber[playerID]<0)
				if(menuTime%30==0) {
					engine.statc[5]++
					if(engine.statc[5]>=mapMaxNo[playerID]) engine.statc[5] = 0
					loadMapPreview(engine, playerID, engine.statc[5], false)
				}

			menuTime++
		} else if(engine.statc[4]==0) {
			menuTime++
			menuCursor = 0

			if(menuTime>=60) menuCursor = 9
			if(menuTime>=120) engine.statc[4] = 1
		} else // Start
			if(owner.engine[0].statc[4]==1&&((owner.engine[1].statc[4]==1||owner.engine[1].ai!=null)&&
					(playerID==1||engine.ctrl.isPush(Controller.BUTTON_A)))) {
				owner.engine[0].stat = GameEngine.Status.READY
				owner.engine[1].stat = GameEngine.Status.READY
				owner.engine[0].resetStatc()
				owner.engine[1].resetStatc()
			} else if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.statc[4] = 0// Cancel

		return true
	}

	/* Setting screen drawing */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(engine.statc[4]==0) {
			when {
				menuCursor<9 -> {
					drawMenuSpeeds(engine, playerID, receiver, 0, COLOR.ORANGE, 0)
					drawMenu(engine, playerID, receiver, 5, COLOR.GREEN, 7, "LOAD" to presetNumber[playerID],
						"SAVE" to presetNumber[playerID])
				}
				menuCursor<18 -> {
					val strTWISTEnable = listOf("OFF", "T-ONLY", "ALL")[twistEnableType[playerID]]
					val strB2BType = listOf("OFF", "ON", "SEPARATE")[b2bType[playerID]]

					drawMenu(engine, playerID, receiver, 0, COLOR.CYAN, 9, "GARBAGE" to garbageStyle[playerID].name)
					receiver.drawMenuFont(engine, playerID, 0, 2, "Messiness", COLOR.YELLOW)
					messiness[playerID].map {"$it%"}.forEachIndexed {i, it ->
						val f = menuCursor==(10+i)&&!engine.owner.replayMode
						receiver.drawMenuFont(engine, playerID, 5*i+if(f) 1 else 0, 3, "\u0082$it", f)
					}
					drawMenu(engine, playerID, receiver, 4, COLOR.CYAN, 11, "COUNTERING" to garbageCounter[playerID],
						"BLOCKING" to garbageBlocking[playerID], "SPIN BONUS" to strTWISTEnable, "EZIMMOBILE" to twistEnableEZ[playerID],
						"B2B" to strB2BType, "B2B SPLIT" to enableCombo[playerID], "COMBO" to enableCombo[playerID])
				}
				else -> {
					drawMenu(engine, playerID, receiver, 0, COLOR.CYAN, 18, "BIG" to big[playerID],
						"SE" to enableSE[playerID],
						"HURRYUP" to if(hurryupSeconds[playerID]==-1) "NONE" else "${hurryupSeconds[playerID]}SEC",
						"INTERVAL" to hurryupInterval[playerID])
					drawMenu(engine, playerID, receiver, 8, COLOR.PINK, 22, "BGM" to BGM.values[bgmno], "SHOW STATS" to showStats)
					drawMenu(engine, playerID, receiver, 12, COLOR.CYAN, 24, "USE MAP" to useMap[playerID],
						"MAP SET" to mapSet[playerID],
						"MAP NO." to if(mapNumber[playerID]<0) "RANDOM" else "${mapNumber[playerID]}/${mapMaxNo[playerID]-1}")
				}
			}
		} else
			receiver.drawMenuFont(engine, playerID, 3, 10, "WAIT", COLOR.YELLOW)
	}

	/* Called for initialization during Ready (before initialization) */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0)
		// MapFor storing backup Replay read
			if(version>=3)
				if(useMap[playerID]) {
					if(owner.replayMode) {
						engine.createFieldIfNeeded()
						loadMap(engine.field, owner.replayProp, playerID)
						engine.field.setAllSkin(engine.skin)
					} else {
						if(propMap[playerID]==null)
							propMap[playerID] = receiver.loadProperties("config/map/vsbattle/${mapSet[playerID]}.map")

						propMap[playerID]?.let {
							engine.createFieldIfNeeded()

							if(mapNumber[playerID]<0) {
								if(playerID==1&&useMap[0]&&mapNumber[0]<0)
									engine.field.copy(owner.engine[0].field)
								else {
									val no = if(mapMaxNo[playerID]<1) 0 else randMap!!.nextInt(mapMaxNo[playerID])
									loadMap(engine.field, it, no)
								}
							} else
								loadMap(engine.field, it, mapNumber[playerID])

							engine.field.setAllSkin(engine.skin)
							fldBackup[playerID] = Field(engine.field)
						}
					}
				} else engine.field.reset()

		return false
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.b2bEnable = b2bType[playerID]>=1
		engine.splitb2b = version>=5&&splitb2b[playerID]
		engine.comboType = if(enableCombo[playerID]) GameEngine.COMBO_TYPE_NORMAL else GameEngine.COMBO_TYPE_DISABLE
		engine.big = big[playerID]
		engine.enableSE = enableSE[playerID]
		if(playerID==1) owner.bgmStatus.bgm = BGM.values[bgmno]

		engine.twistAllowKick = true
		if(version>=4) {
			when {
				twistEnableType[playerID]==0 -> {
					engine.twistEnable = false
					engine.useAllSpinBonus = false
				}
				twistEnableType[playerID]==1 -> {
					engine.twistEnable = true
					engine.useAllSpinBonus = false
				}
				twistEnableType[playerID]==2 -> {
					engine.twistEnable = true
					engine.useAllSpinBonus = true
				}
			}
		} else engine.twistEnable = enableTwist[playerID]
		engine.twistEnableEZ = twistEnableEZ[playerID]

	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		fun col(it:Int) = EventReceiver.getPlayerColor(it)
		// Status display
		if(playerID==0) {
			receiver.drawDirectNum(232, 16, engine.statistics.time.toTimeStr, scale = 2f)

			if(hurryupSeconds[playerID]>=0&&engine.timerActive&&
				engine.statistics.time>=hurryupSeconds[playerID]*60&&engine.statistics.time<(hurryupSeconds[playerID]+5)*60)
				receiver.drawDirectFont(256, 32, "DANGER", if(engine.statistics.time%2==0) COLOR.RED else COLOR.YELLOW)

			if(owner.receiver.nextDisplayType!=2&&showStats) {
				receiver.drawScoreFont(engine, playerID, 0, 0, "VS-BATTLE", COLOR.ORANGE)

				if(!owner.replayMode) {
					receiver.drawScoreFont(engine, playerID, 3, 3, "WINS", COLOR.CYAN)
					receiver.drawScoreNum(engine, playerID, 0, 1, String.format("%2d", winCount[0]), col(0), 2f)
					receiver.drawScoreNum(engine, playerID, 6, 1, String.format("%2d", winCount[1]), col(1), 2f)
				}
				receiver.drawScoreFont(engine, playerID, 2, 5, "SPIKES", COLOR.PINK)
				receiver.drawScoreNum(engine, playerID, 1, 6, String.format("%3d", garbageSent[0]), col(0))
				receiver.drawScoreNum(engine, playerID, 6, 6, String.format("%3d", garbageSent[1]), col(1))

			}
		}
		if(showStats) {
			receiver.drawSpeedMeter(engine, playerID, 0, 7, attackSpd[0]/50, 5f)
			receiver.drawSpeedMeter(engine, playerID, 5, 7, attackSpd[1]/50, 5f)
			receiver.drawScoreNum(engine, playerID, 1, 8, String.format("%5.2f", attackSpd[0]), col(0))
			receiver.drawScoreNum(engine, playerID, 6, 8, String.format("%5.2f", attackSpd[1]), col(1))
			receiver.drawScoreFont(engine, playerID, 0, 10, "RANK POINT", COLOR.PURPLE)
			receiver.drawScoreNum(engine, playerID, 0, 11, String.format("%6.2f", score[0]), col(0))
			receiver.drawScoreNum(engine, playerID, 5, 11, String.format("%6.2f", score[1]), col(1))
			receiver.drawSpeedMeter(engine, playerID, 0, 12, score[0]/50, 5f)
			receiver.drawSpeedMeter(engine, playerID, 5, 12, score[1]/50, 5f)
			val x = receiver.fieldX(engine)
			val y = receiver.fieldY(engine)+24*EventReceiver.BS+1


			garbageEntries[playerID].forEachIndexed {i, g ->
				receiver.drawDirectNum(x+engine.fieldWidth*EventReceiver.BS, y-i*16, "${g.lines}", when {
					g.lines>=1 -> COLOR.YELLOW
					g.lines>=3 -> COLOR.ORANGE
					g.lines>=4 -> COLOR.RED
					else -> COLOR.WHITE
				})
			}

//			if(owner.receiver.nextDisplayType==2) {
			val mycol = col(playerID)

			receiver.drawDirectFont(x-32, y-8, "WINS", mycol, .5f)
			receiver.drawDirectNum(x-24, y-24, String.format("%2d", winCount[playerID]))

			receiver.drawDirectNano(x+36, y, "SENT", mycol, .5f)
			receiver.drawDirectNum(x, y, String.format("%3d", garbageSent[playerID]))

			receiver.drawDirectNano(x+68, y-8, "RP", mycol, .5f)
			receiver.drawDirectNum(x+84, y-8, String.format("%6.2f", score[playerID]), mycol)

//			}
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		var enemyID = 0
		if(playerID==0) enemyID = 1
		var pts = super.calcPower(engine, lines)
		//  Attack
		if(lines>0) {
			var ptsB2B = 0

			// B2B
			if(engine.b2b) {
				if(b2bType[playerID]==2)
					ptsB2B += if(!engine.useAllSpinBonus) 2 else 1
			}

			// gem block attack
			pts += engine.field.howManyGemClears

			engine.statistics.attacks += pts
			lastpiece[playerID] = engine.nowPieceObject!!.id

			// Offset
			var ofs = 0
			if(pts>0&&garbage[playerID]>0&&garbageCounter[playerID])
				while(garbageEntries[playerID].isNotEmpty()&&(pts-ofs)>0) {
					val entry = garbageEntries[playerID].first()
					val gl = entry.lines
					if(gl<=(pts-ofs)) {
						ofs += gl
						garbageEntries[playerID].removeFirst()
					} else {
						entry.lines -= maxOf(0, pts-ofs)
						ofs = pts
					}
				}

			//  Attack
			if((pts-ofs)>0) {
				garbageSent[playerID] += pts-ofs
				garbageEntries[enemyID].add(GarbageEntry(pts-ofs-if(b2bType[playerID]==2) ptsB2B else 0, playerID))
				// Separated B2B
				if(b2bType[playerID]==2&&ptsB2B>0) garbageEntries[enemyID].add(GarbageEntry(ptsB2B, playerID))

				if(owner.engine[enemyID].ai==null&&garbage[enemyID]>=4) owner.engine[enemyID].playSE("levelstop")
			}
		}

		// Rising auction
		if((lines==0||!garbageBlocking[playerID])&&garbage[playerID]>0) {
			engine.playSE("garbage${if(garbage[playerID]>=3) 1 else 0}")
			var gct = 0
			do {
				garbageEntries[playerID].first {it.lines>0}.let {

					if(it.lines>0) {
						val garbageColor = PLAYER_COLOR_BLOCK[it.playerID]
						val l = minOf(it.lines,
							if(garbageStyle[playerID]==GarbageStyle.FiveLines) 5-gct else engine.field.height/2)
						val w = engine.field.width
						var hole = lastHole[playerID]
						if(hole==-1||messiness[playerID][0]==10) hole = engine.random.nextInt(w)
						else {
							val rand = engine.random.nextFloat()
							if(rand<(messiness[playerID][0]/100f)) {
								val newHole = (rand*w).toInt()
								hole = newHole+if(newHole>=hole) 1 else 0
							}
						}

						engine.field.addRandomHoleGarbage(engine, hole, messiness[playerID][1]/100f, garbageColor, engine.skin, l)
						gct += l
						it.lines -= l
						lastHole[playerID] = hole
					}
				}
				garbageEntries[playerID].removeAll {it.lines<=0}
			} while(when(garbageStyle[playerID]) {
					GarbageStyle.STACK -> false
					GarbageStyle.FiveLines -> gct<5
					else -> true
				}&&garbageEntries[playerID].isNotEmpty())
		}

		// HURRY UP!
		if(hurryupSeconds[playerID]>=0&&engine.timerActive)
			if(engine.statistics.time>=hurryupSeconds[playerID]*60) {
				hurryupCount[playerID]++

				if(hurryupCount[playerID]%hurryupInterval[playerID]==0) engine.field.addHurryupFloor(1, engine.skin)
			} else hurryupCount[playerID] = hurryupInterval[playerID]-1

		return pts
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		super.onLast(engine, playerID)
		// HURRY UP!
		if(playerID==0&&engine.timerActive&&hurryupSeconds[playerID]>=0&&engine.statistics.time==hurryupSeconds[playerID]*60)
			engine.playSE("hurryup")

		// Rising auctionMeter
		if(garbage[playerID]*receiver.getBlockSize(engine)>engine.meterValue)
			engine.meterValue += receiver.getBlockSize(engine)/2
		else if(garbage[playerID]*receiver.getBlockSize(engine)<engine.meterValue) engine.meterValue--
		engine.meterColor = GameEngine.METER_COLOR_RED

		// Settlement
		if(playerID==1&&owner.engine[0].gameActive)
			if(owner.engine[0].stat==GameEngine.Status.GAMEOVER&&owner.engine[1].stat==GameEngine.Status.GAMEOVER) {
				// Draw
				winnerID = -1
				owner.engine.forEach {
					it.gameEnded()
					it.stopSE("danger")
				}
				owner.bgmStatus.bgm = BGM.Silent
			} else if(owner.engine[0].stat!=GameEngine.Status.GAMEOVER&&owner.engine[1].stat==GameEngine.Status.GAMEOVER) {
				// 1P win
				winnerID = 0
				owner.engine.forEach {
					it.gameEnded()
					it.stopSE("danger")
				}
				owner.engine[0].stat = GameEngine.Status.EXCELLENT
				owner.engine[0].resetStatc()
				owner.engine[0].statc[1] = 1
				owner.bgmStatus.bgm = BGM.Silent
				if(!owner.replayMode) winCount[0]++
			} else if(owner.engine[0].stat==GameEngine.Status.GAMEOVER&&owner.engine[1].stat!=GameEngine.Status.GAMEOVER) {
				// 2P win
				winnerID = 1
				owner.engine.forEach {
					it.gameEnded()
					it.stopSE("danger")
				}
				owner.engine[1].stat = GameEngine.Status.EXCELLENT
				owner.engine[1].resetStatc()
				owner.engine[1].statc[1] = 1
				owner.bgmStatus.bgm = BGM.Silent
				if(!owner.replayMode) winCount[1]++
			}
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		when(winnerID) {
			-1 -> receiver.drawMenuFont(engine, playerID, 0, 0, "Double KO.", COLOR.YELLOW)
			playerID -> receiver.drawMenuFont(engine, playerID, 1, 0, "You Won!", COLOR.GREEN)
			else -> receiver.drawMenuFont(engine, playerID, 1, 0, "You Lost", COLOR.RED)
		}
		drawResultStats(engine, playerID, receiver, 2, COLOR.ORANGE, Statistic.VS, Statistic.LINES)
		drawResultStats(engine, playerID, receiver, 6, COLOR.PINK, Statistic.ATTACKS, Statistic.APM, Statistic.APL)
		drawResultStats(engine, playerID, receiver, 12, COLOR.ORANGE, Statistic.PPS, Statistic.MAXCOMBO, Statistic.MAXB2B)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties):Boolean {
		saveOtherSetting(engine, owner.replayProp)
		savePreset(engine, owner.replayProp, -1-playerID)

		if(useMap[playerID]) fldBackup[playerID]?.let {saveMap(it, owner.replayProp, playerID)}

		owner.replayProp.setProperty("vsbattle.version", version)
		return false
	}

	/** sent from the enemygarbage blockOf data
	 * @param lines garbage blockcount
	 * @param playerID Sender players ID
	 */
	private data class GarbageEntry(var lines:Int, val playerID:Int)

	companion object {

		/** garbage blockChanges to the position of the holes in the normally random */
		private enum class GarbageStyle {
			/** One Attack will be One garbage-Group*/
			STACK,
			/** Each One turn will draws max 5 lines*/
			FiveLines,
			/** All garbages will put-out on Once placing**/
			OnceAll,
		}

		/** Each player's garbage block cint */
		private val PLAYER_COLOR_BLOCK = arrayOf(Block.COLOR.RED, Block.COLOR.BLUE)

		/** Each player's frame cint */
		private val PLAYER_COLOR_FRAME = intArrayOf(GameEngine.FRAME_COLOR_GREEN, GameEngine.FRAME_COLOR_BLUE)

		/** Current version */
		private const val CURRENT_VERSION = 5

		/** Number of players */
		private const val MAX_PLAYERS = 2

	}
}
