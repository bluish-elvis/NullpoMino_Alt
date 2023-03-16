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
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import kotlin.random.Random

/** VS-BATTLE Mode */
class VSBattle:AbstractMode() {
	/** garbage blockType of */
	private val garbageType = MutableList(MAX_PLAYERS) {0}
	private val garbageStyle get() = garbageType.map {GarbageStyle.all[it]}

	/** Rate of change of garbage holes */
	private val messiness = List(MAX_PLAYERS) {MutableList(2) {0}}

	/** Allow garbage countering */
	private val garbageCounter = MutableList(MAX_PLAYERS) {true}

	/** Allow garbage blocking */
	private val garbageBlocking = MutableList(MAX_PLAYERS) {true}

	/** Has accumulated garbage blockOfcount */
	val garbage get() = (0 until players).map {p -> garbageEntries[p].filter {it.time<=0}.sumOf {it.lines}}

	/** Had sent garbage blockOfcount */
	val garbageSent get() = owner.engine.map {it.statistics.attacks}

	/** Had sent garbage per minutes */
	private val attackSpd get() = owner.engine.map {it.statistics.apm}

	/** VS Score */
	private val score get() = owner.engine.map {it.statistics.vs}

	/** Had guard garbage blockOfcount */
	private val garbageGuard = MutableList(MAX_PLAYERS) {0}

	/** Last garbage hole position */
	private val lastHole = MutableList(MAX_PLAYERS) {0}

	/** Most recent scoring eventInCombocount */
	private val lastcombo = MutableList(MAX_PLAYERS) {0}

	/** UseBGM */
	private var bgmId = 0

	/** Flag for types of Twisters allowed (0=none, 1=normal, 2=all spin) */
	private val twistEnableType = MutableList(MAX_PLAYERS) {0}

	/** Old flag for allowing Twisters */
	private val enableTwist = MutableList(MAX_PLAYERS) {false}

	/** Flag for Split chains b2b */
	private val splitb2b = MutableList(MAX_PLAYERS) {false}

	/** Flag for enabling combos */
	private val enableCombo = MutableList(MAX_PLAYERS) {false}

	/** Big */
	private val big = MutableList(MAX_PLAYERS) {false}

	/** Sound effects ON/OFF */
	private val enableSE = MutableList(MAX_PLAYERS) {false}

	/** Hurry up Seconds before the startcount(-1InHurryupNo) */
	private val hurryUpSeconds = MutableList(MAX_PLAYERS) {0}

	/** Hurry up Times afterBlockDo you run up the floor every time you put the */
	private val hurryUpInterval = MutableList(MAX_PLAYERS) {0}

	/** MapUse flag */
	private val useMap = MutableList(MAX_PLAYERS) {false}

	/** UseMapSet number */
	private val mapSet = MutableList(MAX_PLAYERS) {0}

	/** Map number(-1Random in) */
	private val mapNumber = MutableList(MAX_PLAYERS) {0}

	/** Last preset number used */
	private val presetNumber = MutableList(MAX_PLAYERS) {0}

	/** True if display detailed stats */
	private var showStats = false

	/** Winner */
	private var winnerID = 0

	/** gGrbage blocks sent from the enemy A list of */
	private val garbageEntries = List(MAX_PLAYERS) {emptyList<GarbageEntry>().toMutableList()}

	/** Hurry up After BlockI put count */
	private val hurryUpCount = MutableList(MAX_PLAYERS) {0}

	/** MapSets ofProperty file */
	private var propMap:Array<CustomProperties?> = emptyArray()

	/** MaximumMap number */
	private val mapMaxNo = MutableList(MAX_PLAYERS) {0}

	/** For backup field (MapUsed to save the replay) */
	private var fldBackup:Array<Field?> = emptyArray()

	/** MapRandom for selection count */
	private var randMap:Random? = null

	/** Win count for each player */
	private val winCount = MutableList(MAX_PLAYERS) {0}

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
		super.modeInit(manager)
		garbageType.fill(0)
		messiness.forEach {it.fill(0)}
		garbageCounter.fill(true)
		garbageBlocking.fill(true)
		garbageGuard.fill(0)
		lastHole.fill(0)
		lastcombo.fill(0)
		bgmId = 0
		twistEnableType.fill(0)
		enableTwist.fill(true)
		splitb2b.fill(true)
		enableCombo.fill(true)
		big.fill(false)
		enableSE.fill(true)
		hurryUpSeconds.fill(-1)
		hurryUpInterval.fill(5)
		useMap.fill(false)
		mapSet.fill(0)
		mapNumber.fill(-1)
		presetNumber.fill(0)
		garbageEntries.forEach {it.clear()}
		hurryUpCount.fill(0)
		propMap = arrayOfNulls(MAX_PLAYERS)
		mapMaxNo.fill(0)
		fldBackup = arrayOfNulls(MAX_PLAYERS)
		randMap = Random.Default
		winCount.fill(0)
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
		val pid = engine.playerID
		bgmId = prop.getProperty("vsbattle.bgmno", 0)
		garbageType[pid] = prop.getProperty("vsbattle.garbageType.p$pid", 0)
		prop.getProperties("vsbattle.messiness.p$pid", listOf(90, 30)).forEachIndexed {i, it ->
			messiness[pid][i] = it
		}
		garbageCounter[pid] = prop.getProperty("vsbattle.garbageCounter.p$pid", true)

		garbageBlocking[pid] = prop.getProperty("vsbattle.garbageBlocking.p$pid", true)
		twistEnableType[pid] = prop.getProperty("vsbattle.twistEnableType.p$pid", 1)
		enableTwist[pid] = prop.getProperty("vsbattle.enableTwist.p$pid", true)
		splitb2b[pid] = prop.getProperty("vsbattle.splitb2b.p$pid", true)
		enableCombo[pid] = prop.getProperty("vsbattle.enableCombo.p$pid", true)
		big[pid] = prop.getProperty("vsbattle.big.p$pid", false)
		enableSE[pid] = prop.getProperty("vsbattle.enableSE.p$pid", true)
		hurryUpSeconds[pid] = prop.getProperty("vsbattle.hurryupSeconds.p$pid", -1)
		hurryUpInterval[pid] = prop.getProperty("vsbattle.hurryupInterval.p$pid", 5)
		useMap[pid] = prop.getProperty("vsbattle.useMap.p$pid", false)
		mapSet[pid] = prop.getProperty("vsbattle.mapSet.p$pid", 0)
		mapNumber[pid] = prop.getProperty("vsbattle.mapNumber.p$pid", -1)
		presetNumber[pid] = prop.getProperty("vsbattle.presetNumber.p$pid", 0)
		showStats = prop.getProperty("vsbattle.showStats", true)
	}

	/** Save settings from [engine] into [prop] not related to speeds */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val pid = engine.playerID
		prop.setProperty("vsbattle.bgmno", bgmId)
		prop.setProperty("vsbattle.garbageType.p$pid", garbageType[pid])
		prop.setProperty("vsbattle.messiness.p$pid", messiness.getOrElse(pid) {emptyList()})
		prop.setProperty("vsbattle.garbageCounter.p$pid", garbageCounter[pid])
		prop.setProperty("vsbattle.garbageBlocking.p$pid", garbageBlocking[pid])
		prop.setProperty("vsbattle.twistEnableType.p$pid", twistEnableType[pid])
		prop.setProperty("vsbattle.enableTwist.p$pid", enableTwist[pid])
		prop.setProperty("vsbattle.splitb2b.p$pid", splitb2b[pid])
		prop.setProperty("vsbattle.enableCombo.p$pid", enableCombo[pid])
		prop.setProperty("vsbattle.big.p$pid", big[pid])
		prop.setProperty("vsbattle.enableSE.p$pid", enableSE[pid])
		prop.setProperty("vsbattle.hurryupSeconds.p$pid", hurryUpSeconds[pid])
		prop.setProperty("vsbattle.hurryupInterval.p$pid", hurryUpInterval[pid])
		prop.setProperty("vsbattle.useMap.p$pid", useMap[pid])
		prop.setProperty("vsbattle.mapSet.p$pid", mapSet[pid])
		prop.setProperty("vsbattle.mapNumber.p$pid", mapNumber[pid])
		prop.setProperty("vsbattle.presetNumber.p$pid", presetNumber[pid])
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
	override fun playerInit(engine:GameEngine) {
		val playerID = engine.playerID
		if(playerID==1) {
			engine.randSeed = owner.engine[0].randSeed
			engine.random = Random(owner.engine[0].randSeed)
		}

		engine.frameColor = PLAYER_COLOR_FRAME[playerID]
		engine.ruleOpt.lockResetMoveLimit = engine.ruleOpt.lockResetMoveLimit.let {if(it<0) 30 else minOf(it, 30)}
		engine.ruleOpt.lockResetSpinLimit = engine.ruleOpt.lockResetSpinLimit.let {if(it<0) 20 else minOf(it, 20)}
		lastHole[playerID] = -1
		lastcombo[playerID] = 0

		garbageEntries[playerID].clear()

		hurryUpCount[playerID] = 0

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
	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		val pid = engine.playerID
		if(!engine.owner.replayMode&&engine.statc[4]==0) {
			// Configuration changes
			val change = updateCursor(engine, 27)

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
					7, 8 -> presetNumber[pid] = rangeCursor(presetNumber[pid]+change, 0, 99)
					9 -> garbageType[pid] = rangeCursor(garbageType[pid]+change, 0, GarbageStyle.all.size-1)
					10 -> messiness[pid][0] = rangeCursor(messiness[pid][0]+change, 0, 100)
					11 -> messiness[pid][1] = rangeCursor(messiness[pid][1]+change, 0, 100)
					12 -> garbageCounter[pid] = !garbageCounter[pid]
					13 -> garbageBlocking[pid] = !garbageBlocking[pid]
					14 -> {
						twistEnableType[pid] += change
						if(twistEnableType[pid]<0) twistEnableType[pid] = 2
						if(twistEnableType[pid]>2) twistEnableType[pid] = 0
					}
					15 -> splitb2b[pid] = !splitb2b[pid]
					16 -> enableCombo[pid] = !enableCombo[pid]
					17 -> big[pid] = !big[pid]
					18 -> enableSE[pid] = !enableSE[pid]
					19 -> {
						hurryUpSeconds[pid] += change
						if(hurryUpSeconds[pid]<-1) hurryUpSeconds[pid] = 300
						if(hurryUpSeconds[pid]>300) hurryUpSeconds[pid] = -1
					}
					20 -> {
						hurryUpInterval[pid] += change
						if(hurryUpInterval[pid]<1) hurryUpInterval[pid] = 99
						if(hurryUpInterval[pid]>99) hurryUpInterval[pid] = 1
					}
					21 -> bgmId = rangeCursor(bgmId+change, 0, BGM.count-1)
					22 -> showStats = !showStats
					23 -> {
						useMap[pid] = !useMap[pid]
						if(!useMap[pid]) engine.field.reset() else
							loadMapPreview(engine, pid, if(mapNumber[pid]<0) 0 else mapNumber[pid], true)
					}
					24 -> {
						mapSet[pid] += change
						if(mapSet[pid]<0) mapSet[pid] = 99
						if(mapSet[pid]>99) mapSet[pid] = 0
						if(useMap[pid]) {
							mapNumber[pid] = -1
							loadMapPreview(engine, pid, if(mapNumber[pid]<0) 0 else mapNumber[pid], true)
						}
					}
					25 -> if(useMap[pid]) {
						mapNumber[pid] += change
						if(mapNumber[pid]<-1) mapNumber[pid] = mapMaxNo[pid]-1
						if(mapNumber[pid]>mapMaxNo[pid]-1) mapNumber[pid] = -1
						loadMapPreview(engine, pid, if(mapNumber[pid]<0) 0 else mapNumber[pid], true)
					} else
						mapNumber[pid] = -1
				}
			}

			// 決定
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")

				when(menuCursor) {
					7 -> loadPreset(engine, owner.modeConfig, presetNumber[pid])
					8 -> {
						savePreset(engine, owner.modeConfig, presetNumber[pid])
						owner.saveModeConfig()
					}
					else -> {
						saveOtherSetting(engine, owner.modeConfig)
						savePreset(engine, owner.modeConfig, -1-pid)
						owner.saveModeConfig()
						engine.statc[4] = 1
					}
				}
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitFlag = true

			// プレビュー用Map読み込み
			if(useMap[pid]&&menuTime==0)
				loadMapPreview(engine, pid, if(mapNumber[pid]<0) 0 else mapNumber[pid], true)

			// Random values preview
			if(useMap[pid]&&propMap[pid]!=null&&mapNumber[pid]<0)
				if(menuTime%30==0) {
					engine.statc[5]++
					if(engine.statc[5]>=mapMaxNo[pid]) engine.statc[5] = 0
					loadMapPreview(engine, pid, engine.statc[5], false)
				}
		} else if(engine.statc[4]==0) {
			menuTime++
			menuCursor = 0

			if(menuTime>=60) menuCursor = 9
			if(menuTime>=120) engine.statc[4] = 1
		} else // Start
			if(owner.engine[0].statc[4]==1&&((owner.engine[1].statc[4]==1||owner.engine[1].ai!=null)&&
					(pid==1||engine.ctrl.isPush(Controller.BUTTON_A)))
			) {
				owner.engine[0].stat = GameEngine.Status.READY
				owner.engine[1].stat = GameEngine.Status.READY
				owner.engine[0].resetStatc()
				owner.engine[1].resetStatc()
			} else if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.statc[4] = 0// Cancel

		return true
	}

	/* Setting screen drawing */
	override fun renderSetting(engine:GameEngine) {
		if(engine.statc[4]==0) {
			val pid = engine.playerID
			when {
				menuCursor<9 -> {
					drawMenuSpeeds(engine, receiver, 0, COLOR.ORANGE, 0)
					drawMenu(
						engine, receiver, 5, COLOR.GREEN, 7, "LOAD" to presetNumber[pid], "SAVE" to presetNumber[pid]
					)
				}
				menuCursor<19 -> {
					val strTWISTEnable = listOf("OFF", "T-ONLY", "ALL")[twistEnableType[pid]]

					drawMenu(engine, receiver, 0, COLOR.CYAN, 17, "GARBAGE" to garbageStyle[pid].name)
					receiver.drawMenuFont(engine, 0, 2, "Messiness", COLOR.YELLOW)
					messiness[pid].map {"$it%"}.forEachIndexed {i, it ->
						val f = menuCursor==(10+i)&&!engine.owner.replayMode
						receiver.drawMenuFont(engine, 5*i+if(f) 1 else 0, 3, "\u0082$it", f)
					}
					drawMenu(
						engine, receiver, 4, COLOR.CYAN, 12,
						"COUNTERING" to garbageCounter[pid], "BLOCKING" to garbageBlocking[pid],
						"SPIN BONUS" to strTWISTEnable, "COMBO" to enableCombo[pid]
					)
				}
				else -> {
					drawMenu(
						engine, receiver, 0, COLOR.CYAN, 17, "BIG" to big[pid], "SE" to enableSE[pid],
						"HURRYUP" to if(hurryUpSeconds[pid]==-1) "NONE" else "${hurryUpSeconds[pid]}SEC",
						"INTERVAL" to hurryUpInterval[pid]
					)
					drawMenu(engine, receiver, 8, COLOR.PINK, 22, "BGM" to BGM.values[bgmId], "SHOW STATS" to showStats)
					drawMenu(
						engine, receiver, 12, COLOR.CYAN, 24, "USE MAP" to useMap[pid], "MAP SET" to mapSet[pid],
						"MAP NO." to if(mapNumber[pid]<0) "RANDOM" else "${mapNumber[pid]}/${mapMaxNo[pid]-1}"
					)
				}
			}
		} else
			receiver.drawMenuFont(engine, 3, 10, "WAIT", COLOR.YELLOW)
	}

	/* Called for initialization during Ready (before initialization) */
	override fun onReady(engine:GameEngine):Boolean {
		val pid = engine.playerID
		if(engine.statc[0]==0)
		// MapFor storing backup Replay read
			if(version>=3)
				if(useMap[pid]) {
					if(owner.replayMode) {
						engine.createFieldIfNeeded()
						loadMap(engine.field, owner.replayProp, pid)
						engine.field.setAllSkin(engine.skin)
					} else {
						if(propMap[pid]==null)
							propMap[pid] = receiver.loadProperties("config/map/vsbattle/${mapSet[pid]}.map")

						propMap[pid]?.let {
							engine.createFieldIfNeeded()

							if(mapNumber[pid]<0) {
								if(pid==1&&useMap[0]&&mapNumber[0]<0)
									engine.field.replace(owner.engine[0].field)
								else {
									val no = if(mapMaxNo[pid]<1) 0 else randMap!!.nextInt(mapMaxNo[pid])
									loadMap(engine.field, it, no)
								}
							} else
								loadMap(engine.field, it, mapNumber[pid])

							engine.field.setAllSkin(engine.skin)
							fldBackup[pid] = Field(engine.field)
						}
					}
				} else engine.field.reset()

		return false
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		val playerID = engine.playerID
		engine.b2bEnable = true
		engine.splitB2B = version>=5&&splitb2b[playerID]
		engine.comboType = if(enableCombo[playerID]) GameEngine.COMBO_TYPE_NORMAL else GameEngine.COMBO_TYPE_DISABLE
		engine.big = big[playerID]
		engine.enableSE = enableSE[playerID]
		if(playerID==1) owner.musMan.bgm = BGM.values[bgmId]

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
		engine.twistEnableEZ = engine.twistEnable
	}

	/* Render score */
	override fun renderLast(engine:GameEngine) {
		fun col(it:Int) = EventReceiver.getPlayerColor(it)
		// Status display
		val pid = engine.playerID
		if(pid==0) {
			receiver.drawDirectNum(232, 16, engine.statistics.time.toTimeStr, scale = 2f)

			if(hurryUpSeconds[pid]>=0&&engine.timerActive&&
				engine.statistics.time>=hurryUpSeconds[pid]*60&&engine.statistics.time<(hurryUpSeconds[pid]+5)*60
			)
				receiver.drawDirectFont(256, 32, "DANGER", if(engine.statistics.time%2==0) COLOR.RED else COLOR.YELLOW)

			if(owner.receiver.nextDisplayType!=2&&showStats) {
				receiver.drawScoreFont(engine, 0, 0, "VS-BATTLE", COLOR.ORANGE)

				if(!owner.replayMode) {
					receiver.drawScoreFont(engine, 3, 3, "WINS", COLOR.CYAN)
					receiver.drawScoreNum(engine, 0, 1, String.format("%2d", winCount[0]), col(0), 2f)
					receiver.drawScoreNum(engine, 6, 1, String.format("%2d", winCount[1]), col(1), 2f)
				}
				receiver.drawScoreFont(engine, 2, 5, "SPIKES", COLOR.PINK)
				receiver.drawScoreNum(engine, 1, 6, String.format("%3d", garbageSent[0]), col(0))
				receiver.drawScoreNum(engine, 6, 6, String.format("%3d", garbageSent[1]), col(1))
			}
		}
		if(showStats) {
			receiver.drawScoreSpeed(engine, 0, 7, attackSpd[0]/50, 5f)
			receiver.drawScoreSpeed(engine, 5, 7, attackSpd[1]/50, 5f)
			receiver.drawScoreNum(engine, 1, 8, String.format("%5.2f", attackSpd[0]), col(0))
			receiver.drawScoreNum(engine, 6, 8, String.format("%5.2f", attackSpd[1]), col(1))
			receiver.drawScoreFont(engine, 0, 10, "RANK POINT", COLOR.PURPLE)
			receiver.drawScoreNum(engine, 0, 11, String.format("%6.2f", score[0]), col(0))
			receiver.drawScoreNum(engine, 5, 11, String.format("%6.2f", score[1]), col(1))
			receiver.drawScoreSpeed(engine, 0, 12, score[0]/50, 5f)
			receiver.drawScoreSpeed(engine, 5, 12, score[1]/50, 5f)
			val x = receiver.fieldX(engine)
			val y = receiver.fieldY(engine)+(engine.fieldHeight+2)*EventReceiver.BS+1


			garbageEntries[pid].forEachIndexed {i, (lines, playerID, time) ->
				receiver.drawDirectNum(
					x+engine.fieldWidth*EventReceiver.BS, y-i*16, "${lines}", when {
						time>0&&(time/2)%2==1 -> EventReceiver.getPlayerColor(playerID)
						lines>=5 -> COLOR.RED
						lines>=3 -> COLOR.ORANGE
						lines>=1 -> COLOR.YELLOW
						else -> COLOR.WHITE
					}, 1f-time*0.01f
				)
			}

//			if(owner.receiver.nextDisplayType==2) {
			val myCol = col(pid)

			receiver.drawDirectFont(x-32, y+24, "WINS", myCol, .5f)
			receiver.drawDirectNum(x-24, y+8, String.format("%2d", winCount[pid]))
			receiver.drawDirectNano(x-32, y-8, "RP", myCol, .5f)
			receiver.drawDirectNum(x-32, y-24, String.format("%3d", score[pid].toInt()), myCol)
			receiver.drawDirectNano(x-14, y-8, String.format("%3d", (score[pid]*1000%1000).toInt()), myCol, 0.5f)

			receiver.drawDirectNano(x-32, y-36, "SENT", myCol, .5f)
			receiver.drawDirectNum(x-32, y-52, String.format("%3d", garbageSent[pid]))

//			}
		}
	}

	private fun sendGarbage(pts:Int, playerID:Int) {
		val pid = playerID
		val enemyID = if(pid==0) 1 else 0
		// Offset
		var ofs = 0
		if(pts>0&&garbage[pid]>0&&garbageCounter[pid])
			while(garbageEntries[pid].any {it.time<=0}&&(pts-ofs)>0) {
				val entry = garbageEntries[pid].firstOrNull {it.time<=0} ?: break
				val gl = entry.lines
				if(gl<=(pts-ofs)) {
					ofs += gl
					garbageEntries[pid].removeFirst()
				} else {
					entry.lines -= maxOf(0, pts-ofs)
					ofs = pts
				}
			}

		//  Attack
		if((pts-ofs)>0) {
			garbageEntries[enemyID].add(
				GarbageEntry(pts-ofs, pid, maxOf(10, minOf(24, 30-owner.engine[enemyID].statistics.time/600)))
			)

			if(owner.engine[enemyID].ai==null&&garbage[enemyID]>=4) owner.engine[enemyID].playSE("levelstop")
		}
	}
	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		var pts = 0
		//  Attack
		val pid = engine.playerID
		if(ev.lines>0) {
			val pow = super.calcPower(engine, ev, true)
			// gem block attack
			val gems = engine.field.howManyGemClears
			engine.statistics.attacksBonus += gems
			pts += pow+gems

			sendGarbage(pts, pid)
		}

		// Rising auction
		if((ev.lines==0||!garbageBlocking[pid])&&garbage[pid]>0) {
			engine.playSE("linefall${if(garbage[pid]>3) "1" else if(garbage[pid]>1) "0" else ""}")
			var gct = 0
			do {
				garbageEntries[pid].filter {it.time<=0}.first {it.lines>0}.let {
					if(it.lines>0) {
						val garbageColor = PLAYER_COLOR_BLOCK[it.playerID]
						val l = minOf(
							it.lines,
							if(garbageStyle[pid]==GarbageStyle.FiveLines) 5-gct else engine.field.height/2
						)
						val w = engine.field.width
						var hole = lastHole[pid]
						if(hole==-1||messiness[pid][0]==10) hole = engine.random.nextInt(w)
						else {
							val rand = engine.random.nextFloat()
							if(rand<(messiness[pid][0]/100f)) {
								val newHole = (rand*w).toInt()
								hole = newHole+if(newHole>=hole) 1 else 0
							}
						}

						engine.field.addRandomHoleGarbage(engine, hole, messiness[pid][1]/100f, garbageColor, engine.skin, l)
						gct += l
						it.lines -= l
						lastHole[pid] = hole
					}
				}
				garbageEntries[pid].removeAll {it.lines<=0}
			} while(when(garbageStyle[pid]) {
					GarbageStyle.STACK -> false
					GarbageStyle.FiveLines -> gct<5
					else -> true
				}&&garbageEntries[pid].isNotEmpty()
			)
		}

		// HURRY UP!
		if(hurryUpSeconds[pid]>=0&&engine.timerActive)
			if(engine.statistics.time>=hurryUpSeconds[pid]*60) {
				hurryUpCount[pid]++

				if(hurryUpCount[pid]%hurryUpInterval[pid]==0) engine.field.addHurryupFloor(1, engine.skin)
			} else hurryUpCount[pid] = hurryUpInterval[pid]-1

		return pts
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		val pid = engine.playerID
		super.onLast(engine)
		// HURRY UP!
		if(pid==0&&engine.timerActive&&hurryUpSeconds[pid]>=0&&engine.statistics.time==hurryUpSeconds[pid]*60)
			engine.playSE("hurryup")
		if(garbageEntries[pid].any {it.time>0}) {
			if(garbageEntries[pid].any {it.time==1})
				engine.playSE(
					"garbage${
						(garbageEntries[pid].filter {it.time==1}.maxOf {it.lines}>=4).toInt()+
							((engine.field.highestBlockY-garbage[pid])<engine.fieldHeight*4/7).toInt()
					}"
				)
			garbageEntries[pid].filter {it.time>0}.forEach {it.time--}
		}
		// Rising auctionMeter
		if(garbage[pid]*engine.blockSize>engine.meterValue)
			engine.meterValue += engine.blockSize/2
		else if(garbage[pid]*engine.blockSize<engine.meterValue) engine.meterValue--
		engine.meterColor = GameEngine.METER_COLOR_RED

		// Settlement
		if(pid==1&&owner.engine[0].gameActive)
			if(owner.engine[0].stat==GameEngine.Status.GAMEOVER&&owner.engine[1].stat==GameEngine.Status.GAMEOVER) {
				// Draw
				winnerID = -1
				owner.engine.forEach {
					it.gameEnded()
					it.stopSE("danger")
				}
				owner.musMan.bgm = BGM.Silent
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
				owner.musMan.bgm = BGM.Silent
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
				owner.musMan.bgm = BGM.Silent
				if(!owner.replayMode) winCount[1]++
			}
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		when(winnerID) {
			-1 -> receiver.drawMenuFont(engine, 0, 0, "Double KO.", COLOR.YELLOW)
			engine.playerID -> receiver.drawMenuFont(engine, 1, 0, "You Won!", COLOR.GREEN)
			else -> receiver.drawMenuFont(engine, 1, 0, "You Lost", COLOR.RED)
		}
		drawResultStats(engine, receiver, 2, COLOR.ORANGE, Statistic.VS, Statistic.LINES)
		drawResultStats(engine, receiver, 6, COLOR.PINK, Statistic.ATTACKS, Statistic.APM, Statistic.APL)
		drawResultStats(engine, receiver, 12, COLOR.ORANGE, Statistic.PPS, Statistic.MAXCOMBO, Statistic.MAXB2B)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveOtherSetting(engine, owner.replayProp)
		val pid = engine.playerID
		savePreset(engine, owner.replayProp, -1-pid)

		if(useMap[pid]) fldBackup[pid]?.let {saveMap(it, owner.replayProp, pid)}

		owner.replayProp.setProperty("vsbattle.version", version)
		return false
	}

	/** sent from the enemygarbage blockOf data
	 * @param lines garbage blockcount
	 * @param playerID Sender players ID
	 * @param time Remaining Travelling frames
	 */
	private data class GarbageEntry(var lines:Int, val playerID:Int, var time:Int)
	companion object {
		/** garbage blockChanges to the position of the holes in the normally random */
		private enum class GarbageStyle {
			/** One Attack will be One garbage-Group*/
			STACK,
			/** Each One turn will draw max 5 lines*/
			FiveLines,
			/** All garbages will put-out on Once placing**/
			OnceAll;

			companion object {
				val all = values()
			}
		}

		/** Each player's garbage block cint */
		private val PLAYER_COLOR_BLOCK = listOf(Block.COLOR.RED, Block.COLOR.BLUE)

		/** Each player's frame cint */
		private val PLAYER_COLOR_FRAME = listOf(GameEngine.FRAME_COLOR_GREEN, GameEngine.FRAME_COLOR_BLUE)

		/** Current version */
		private const val CURRENT_VERSION = 5

		/** Number of players */
		private const val MAX_PLAYERS = 2
	}
}
