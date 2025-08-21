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
package mu.nu.nullpo.game.subsystem.mode.another

import mu.nu.nullpo.game.component.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameEngine.GameStyle
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.play.LineGravity
import mu.nu.nullpo.game.play.LineGravity.CASCADE.canCascade
import mu.nu.nullpo.game.play.clearRule.ColorStraight
import mu.nu.nullpo.game.subsystem.mode.AbstractMode
import mu.nu.nullpo.gui.common.BaseFont.FONT.*
import mu.nu.nullpo.gui.common.GameKeyDummy.Companion.MAX_PLAYERS
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import kotlin.random.Random

/** PHYSICIAN VS-BATTLE mode (beta) */
class PhysicianVS:AbstractMode() {
	/** Has accumulatedojama blockOfcount */
	//private int[] garbage;

	/** Had sentojama blockOfcount */
	//private int[] garbageSent;

	/** Time to display the most recent increase in score */
	private var scgettime = MutableList(MAX_PLAYERS) {0}

	/** UseBGM */
	private var bgmId = 0

	/** Sound effectsON/OFF */
	private var enableSE = MutableList(MAX_PLAYERS) {false}

	/** MapUse flag */
	private var useMap = MutableList(MAX_PLAYERS) {false}

	/** UseMapSet number */
	private var mapSet = MutableList(MAX_PLAYERS) {0}

	/** Map number(-1Random in) */
	private var mapNumber = MutableList(MAX_PLAYERS) {0}

	/** Last preset number used */
	private var presetNumber = MutableList(MAX_PLAYERS) {0}

	/** Winner */
	private var winnerID = 0

	/** MapSets ofProperty file */
	private val propMap:MutableList<CustomProperties?> = MutableList(MAX_PLAYERS) {null}

	/** MaximumMap number */
	private var mapMaxNo = IntArray(0)

	/** For backupfield (MapUsed to save the replay) */
	private val fldBackup:MutableList<Field?> = MutableList(MAX_PLAYERS) {null}

	/** MapRan for selectioncount */
	private var randMap:Random? = null

	/** Version */
	private var version = 0

	/** Flag for all clear */
	//private boolean[] zenKeshi;

	/** Amount of points earned from most recent clear */
	private var lastscores = MutableList(MAX_PLAYERS) {0}

	/** Amount of garbage added in current chain */
	//private int[] garbageAdd;

	/** Score */
	private val score get() = owner.engine.map {it.statistics.score}

	/** Number of initial gem blocks */
	private var hoverBlocks = MutableList(MAX_PLAYERS) {0}

	/** Speed mode */
	private var speed = MutableList(MAX_PLAYERS) {0}

	/** Number gem blocks cleared in current chain */
	private var gemsClearedChainTotal = MutableList(MAX_PLAYERS) {0}

	/** Each player's remaining gem count */
	private var rest = MutableList(MAX_PLAYERS) {0}

	/** Each player's garbage block colors to be dropped */
	private val garbageColors:List<MutableList<Int>> = List(MAX_PLAYERS) {MutableList(0) {0}}

	/** Flash/normal mode settings */
	private val flash = MutableList(MAX_PLAYERS) {false}

	/* Mode name */
	override val name = "PHYSICIAN VS-BATTLE (RC1)"
	override val gameIntensity = 2
	override val isVSMode:Boolean
		get() = true

	/* Number of players */
	override val players:Int
		get() = MAX_PLAYERS

	/* Game style */
	override val gameStyle = GameStyle.PHYSICIAN

	/* Mode initialization */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)

		//garbage = new int[MAX_PLAYERS];
		//garbageSent = new int[MAX_PLAYERS];

		scgettime.fill(0)
		bgmId = 0
		enableSE.fill(false)
		useMap.fill(false)
		mapSet.fill(0)
		mapNumber.fill(0)
		presetNumber.fill(0)
		propMap.fill(null)
		mapMaxNo.fill(0)
		fldBackup.fill(null)
		randMap = Random.Default

		lastscores.fill(0)
		//garbageAdd = new int[MAX_PLAYERS];
		hoverBlocks.fill(0)
		speed.fill(0)
		gemsClearedChainTotal.fill(0)
		rest.fill(0)
		garbageColors.forEach {it.clear()}
		flash.fill(false)

		winnerID = -1
	}

	/** Read speed presets
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 * @param preset Preset number
	 */
	private fun loadPreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		engine.speed.gravity = prop.getProperty("physicianvs.gravity.$preset", 4)
		engine.speed.denominator = prop.getProperty("physicianvs.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("physicianvs.are.$preset", 24)
		engine.speed.areLine = prop.getProperty("physicianvs.areLine.$preset", 24)
		engine.speed.lineDelay = prop.getProperty("physicianvs.lineDelay.$preset", 10)
		engine.speed.lockDelay = prop.getProperty("physicianvs.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("physicianvs.das.$preset", 14)
	}

	/** Save speed presets
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 * @param preset Preset number
	 */
	private fun savePreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		prop.setProperty("physicianvs.gravity.$preset", engine.speed.gravity)
		prop.setProperty("physicianvs.denominator.$preset", engine.speed.denominator)
		prop.setProperty("physicianvs.are.$preset", engine.speed.are)
		prop.setProperty("physicianvs.areLine.$preset", engine.speed.areLine)
		prop.setProperty("physicianvs.lineDelay.$preset", engine.speed.lineDelay)
		prop.setProperty("physicianvs.lockDelay.$preset", engine.speed.lockDelay)
		prop.setProperty("physicianvs.das.$preset", engine.speed.das)
	}

	/** Load settings into [engine] from [prop] not related to speeds */
	private fun loadOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val pid = engine.playerID
		bgmId = prop.getProperty("physicianvs.bgmno", 0)
		enableSE[pid] = prop.getProperty("physicianvs.enableSE.p$pid", true)
		useMap[pid] = prop.getProperty("physicianvs.useMap.p$pid", false)
		mapSet[pid] = prop.getProperty("physicianvs.mapSet.p$pid", 0)
		mapNumber[pid] = prop.getProperty("physicianvs.mapNumber.p$pid", -1)
		presetNumber[pid] = prop.getProperty("physicianvs.presetNumber.p$pid", 0)
		speed[pid] = prop.getProperty("physicianvs.speed.p$pid", 1)
		hoverBlocks[pid] = prop.getProperty("physicianvs.hoverBlocks.p$pid", 40)
		flash[pid] = prop.getProperty("physicianvs.flash.p$pid", false)
	}

	/** Save settings from [engine] into [prop] not related to speeds */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val pid = engine.playerID
		prop.setProperty("physicianvs.bgmno", bgmId)
		prop.setProperty("physicianvs.enableSE.p$pid", enableSE[pid])
		prop.setProperty("physicianvs.useMap.p$pid", useMap[pid])
		prop.setProperty("physicianvs.mapSet.p$pid", mapSet[pid])
		prop.setProperty("physicianvs.mapNumber.p$pid", mapNumber[pid])
		prop.setProperty("physicianvs.presetNumber.p$pid", presetNumber[pid])
		prop.setProperty("physicianvs.speed.p$pid", speed[pid])
		prop.setProperty("physicianvs.hoverBlocks.p$pid", hoverBlocks[pid])
		prop.setProperty("physicianvs.flash.p$pid", flash[pid])
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
		if(propMap[playerID].isNullOrEmpty()||forceReload) {
			mapMaxNo[playerID] = 0
			propMap[playerID] = receiver.loadProperties("config/map/vsbattle/${mapSet[playerID]}.map")
		}

		if(propMap[playerID].isNullOrEmpty())
			engine.field.reset()
		else propMap[playerID]?.let {
			mapMaxNo[playerID] = it.getProperty("values.maxMapNumber", 0)
			engine.createFieldIfNeeded()
			loadMap(engine.field, it, id)
			engine.field.setAllSkin(engine.blkSkin)
		}
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine) {
		val pid = engine.playerID
		if(pid==1) {
			engine.randSeed = owner.engine[0].randSeed
			engine.random = Random(owner.engine[0].randSeed)
		}

		engine.frameSkin = PLAYER_COLOR_FRAME[pid]
		engine.clearMode = ColorStraight(4, false, true)
		engine.garbageColorClear = false
		engine.colorClearSize = 4
		engine.lineGravityType = LineGravity.CASCADE
		engine.nextPieceEnable = PIECE_ENABLE.map {it==1}
		engine.randomBlockColor = true
		engine.blockColors = BLOCK_COLORS
		engine.connectBlocks = true
		engine.cascadeDelay = 18
		engine.gemSameColor = true

		//garbage[playerID] = 0;
		//garbageSent[playerID] = 0;
		scgettime[pid] = 0
		gemsClearedChainTotal[pid] = 0
		rest[pid] = 0

		version = if(!engine.owner.replayMode) {
			loadOtherSetting(engine, engine.owner.modeConfig)
			loadPreset(engine, engine.owner.modeConfig, -1-pid)
			CURRENT_VERSION
		} else {
			loadOtherSetting(engine, engine.owner.replayProp)
			loadPreset(engine, engine.owner.replayProp, -1-pid)
			owner.replayProp.getProperty("physicianvs.version", 0)
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		val pid = engine.playerID
		if(!engine.owner.replayMode&&engine.statc[4]==0) {
			// Configuration changes
			val change = updateCursor(engine, 16)

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
					9 -> {
						speed[pid] += change
						if(speed[pid]<0) speed[pid] = 2
						if(speed[pid]>2) speed[pid] = 0
					}
					10 -> {
						if(m>=10)
							hoverBlocks[pid] += change*10
						else
							hoverBlocks[pid] += change
						if(hoverBlocks[pid]<1) hoverBlocks[pid] = 99
						if(hoverBlocks[pid]>99) hoverBlocks[pid] = 1
					}
					11 -> flash[pid] = !flash[pid]
					12 -> enableSE[pid] = !enableSE[pid]
					13 -> bgmId = rangeCursor(bgmId+change, 0, BGM.count-1)
					14 -> {
						useMap[pid] = !useMap[pid]
						if(!useMap[pid]) {
							engine.field.reset()
						} else
							loadMapPreview(engine, pid, if(mapNumber[pid]<0) 0 else mapNumber[pid], true)
					}
					15 -> {
						mapSet[pid] += change
						if(mapSet[pid]<0) mapSet[pid] = 99
						if(mapSet[pid]>99) mapSet[pid] = 0
						if(useMap[pid]) {
							mapNumber[pid] = -1
							loadMapPreview(engine, pid, if(mapNumber[pid]<0) 0 else mapNumber[pid], true)
						}
					}
					16 -> if(useMap[pid]) {
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
				loadMapPreview(
					engine, pid, if(mapNumber[pid]<0) 0 else mapNumber[pid], true
				)

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

			if(menuTime>=120)
				engine.statc[4] = 1
			else if(menuTime>=60) menuCursor = 9
		} else // Start
			if(owner.engine[0].statc[4]==1&&owner.engine[1].statc[4]==1&&pid==1) {
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
			if(menuCursor<9) {
				drawMenuSpeeds(engine, receiver, 0, COLOR.ORANGE, 0)
				drawMenu(engine, receiver, COLOR.GREEN, "LOAD" to presetNumber[pid], "SAVE" to presetNumber[pid])
			} else {
				drawMenu(
					engine, receiver, 0, COLOR.CYAN, 9, "SPEED" to SPEED_NAME[speed[pid]], "VIRUS" to hoverBlocks[pid],
					"MODE" to if(flash[pid]) "FLASH" else "NORMAL"
				)
				drawMenu(engine, receiver, COLOR.PINK, "SE" to enableSE[pid], "BGM" to BGM.values[bgmId])
				drawMenu(
					engine, receiver, COLOR.CYAN,
					"USE MAP" to useMap[pid],
					"MAP SET" to "${mapSet[pid]}",
					"MAP NO." to if(mapNumber[pid]<0) "RANDOM" else "${mapNumber[pid]}"+"/"+(mapMaxNo[pid]-1)
				)
			}
		} else
			receiver.drawMenu(engine, 3, 10, "WAIT", BASE, COLOR.YELLOW)
	}

	/* Called for initialization during Ready (before initialization) */
	override fun onReady(engine:GameEngine):Boolean {
		val pid = engine.playerID
		if(engine.statc[0]==0) {
			// MapFor storing backup Replay read
			if(useMap[pid]) {
				if(owner.replayMode) {
					engine.createFieldIfNeeded()
					loadMap(engine.field, owner.replayProp, pid)
					engine.field.setAllSkin(engine.blkSkin)
				} else {
					if(propMap[pid]==null) {
						propMap[pid] = receiver.loadProperties("config/map/vsbattle/${mapSet[pid]}.map")
					} else propMap[pid]?.let {
						engine.createFieldIfNeeded()

						if(mapNumber[pid]<0) {
							if(pid==1&&useMap[0]&&mapNumber[0]<0) engine.field.replace(owner.engine[0].field)
							else {
								val no = if(mapMaxNo[pid]<1) 0 else randMap!!.nextInt(mapMaxNo[pid])
								loadMap(engine.field, it, no)
							}
						} else loadMap(engine.field, it, mapNumber[pid])

						engine.field.setAllSkin(engine.blkSkin)
						fldBackup[pid] = Field(engine.field)
					}
				}
			} else engine.field.reset()
			if(hoverBlocks[pid]>0) {
				engine.createFieldIfNeeded()
				var minY = 6
				when {
					hoverBlocks[pid]>=80 -> minY = 3
					hoverBlocks[pid]>=72 -> minY = 4
					hoverBlocks[pid]>=64 -> minY = 5
				}
				if(flash[pid]) {
					engine.field.addRandomHoverBlocks(engine, hoverBlocks[pid], FLASH_BLOCK_COLORS, minY, true, true)
					engine.field.setAllSkin(12)
				} else engine.field.addRandomHoverBlocks(engine, hoverBlocks[pid], HOVER_BLOCK_COLORS, minY, true)
			}
		}

		return false
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		engine.b2bEnable = false
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_CONNECT
		engine.enableSE = enableSE[engine.playerID]
		if(engine.playerID==1) owner.musMan.bgm = BGM.values[bgmId]

		engine.twistAllowKick = false
		engine.twistEnable = false
		engine.useAllSpinBonus = false
	}

	/* Render score */
	override fun renderLast(engine:GameEngine) {
		val fldPosX = receiver.fieldX(engine)
		val fldPosY = receiver.fieldY(engine)
		val pid = engine.playerID
		val playerColor = EventReceiver.getPlayerColor(pid)
		val tempX:Int

		// Timer
		if(pid==0) receiver.drawFont(256, 16, engine.statistics.time.toTimeStr, BASE)

		if(engine.gameStarted) {
			// Rest
			receiver.drawFont(fldPosX+160, fldPosY+241, "Target", BASE, playerColor, .5f)
			tempX = if(rest[pid]<10) 8 else 0
			receiver.drawFont(
				fldPosX+160+tempX, fldPosY+257, "${rest[pid]}",
				BASE,
				if(rest[pid]<=if(flash[pid]) 1 else 3) COLOR.RED else COLOR.WHITE
			)

			// Speed
			receiver.drawFont(fldPosX+156, fldPosY+280, "Speed", BASE, playerColor, .5f)
			receiver.drawFont(fldPosX+152, fldPosY+296, SPEED_NAME[speed[pid]], BASE, SPEED_COLOR[speed[pid]])
		}

		/* if(playerID == 0) {
 receiver.drawScore(engine, playerID, -1, 0, "PHYSICIAN VS",
 EventReceiver.COLOR.GREEN);
 receiver.drawScore(engine, playerID, -1, 2, "REST",
 EventReceiver.COLOR.PURPLE);
 receiver.drawScore(engine, playerID, -1, 3, "1P:",
 EventReceiver.COLOR.RED);
 receiver.drawScore(engine, playerID, 3, 3,
 String.valueOf(rest[0]), (rest[0] <= (flash[playerID] ? 1 : 3)));
 receiver.drawScore(engine, playerID, -1, 4, "2P:",
 EventReceiver.COLOR.BLUE);
 receiver.drawScore(engine, playerID, 3, 4,
 String.valueOf(rest[1]), (rest[1] <= (flash[playerID] ? 1 : 3)));
 receiver.drawScore(engine, playerID, -1, 6, "SPEED",
 EventReceiver.COLOR.GREEN);
 receiver.drawScore(engine, playerID, -1, 7, "1P:",
 EventReceiver.COLOR.RED);
 receiver.drawScore(engine, playerID, 3, 7, SPEED_NAME[speed[0]],
 SPEED_COLOR[speed[0]]);
 receiver.drawScore(engine, playerID, -1, 8, "2P:",
 EventReceiver.COLOR.BLUE);
 receiver.drawScore(engine, playerID, 3, 8, SPEED_NAME[speed[1]],
 SPEED_COLOR[speed[1]]);
 receiver.drawScore(engine, playerID, -1, 10, "Score",
 EventReceiver.COLOR.PURPLE);
 receiver.drawScore(engine, playerID, -1, 11, "1P: " +
 String.valueOf(score[0]), EventReceiver.COLOR.RED);
 receiver.drawScore(engine, playerID, -1, 12, "2P: " +
 String.valueOf(score[1]), EventReceiver.COLOR.BLUE);
 receiver.drawScore(engine, playerID, -1, 14, "Time",
 EventReceiver.COLOR.GREEN);
 receiver.drawScore(engine, playerID, -1, 15,
 engine.statistics.time.toTimeStr);
 } */
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
//		if(engine.field==null) return 0
		var gemsCleared = engine.field.gemsCleared
		val pid = engine.playerID
		val blkc = ev.lines
		if(gemsCleared>0&&blkc>0) {
			var pts = 0
			while(gemsCleared>0&&gemsClearedChainTotal[pid]<5) {
				pts += 1 shl gemsClearedChainTotal[pid]
				gemsClearedChainTotal[pid]++
				gemsCleared--
			}
			if(gemsClearedChainTotal[pid]>=5) pts += gemsCleared shl 5
			pts *= (speed[pid]+1)*100
			gemsClearedChainTotal[pid] += gemsCleared
			lastscores[pid] = pts
			scgettime[pid] = 120
			engine.statistics.scoreLine += pts
			engine.playSE("gem")
			setSpeed(engine)
			return pts
		} else if(blkc==0&&!engine.field.canCascade())
			if(garbageCheck(engine)) {
				engine.stat = GameEngine.Status.LINECLEAR
				engine.statc[0] = engine.lineDelay
			}
		return 0
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
		engine.speed.gravity =
			listOf(6, 8, 10)[speed[engine.playerID]]*(10+(engine.statistics.totalPieceLocked/10))
		engine.speed.denominator = 3600
	}

	override fun lineClearEnd(engine:GameEngine):Boolean {
		val enemyID = if(engine.playerID==0) 1 else 0

		engine.field.lineColorsCleared.let {cleared ->
			if(cleared.size>1)
				if(garbageColors[enemyID].isEmpty()) garbageColors[enemyID].apply {
					clear()
					addAll(cleared)
				}
				else {
					val s = garbageColors.size
					cleared.forEachIndexed {i, it -> garbageColors[enemyID][s+i] = it}
				}
		}
		engine.field.lineColorsCleared = emptyList()
		return garbageCheck(engine)
	}

	private fun garbageCheck(engine:GameEngine):Boolean {
		val pid = engine.playerID
		if(garbageColors[pid].isEmpty()) return false
		val size = garbageColors[pid].size
		if(size<2) return false
		garbageColors[pid].apply {
			clear()
			addAll(garbageColors[pid].shuffled(engine.random))
		}
		val colors = IntArray(4)
		when {
			size>=4 -> for(x in 0..3)
				colors[x] = garbageColors[pid][x]

			size==3 -> {
				val skipSlot = engine.random.nextInt(4)
				colors[skipSlot] = -1
				var i:Int
				for(x in 0..2) {
					i = x
					if(x>=skipSlot) i++
					colors[i] = garbageColors[pid][x]
				}
			}

			else -> {
				val firstSlot = engine.random.nextInt(4)
				colors[firstSlot] = garbageColors[pid][0]
				var secondSlot = firstSlot+2
				if(secondSlot>3) secondSlot -= 4
				colors[secondSlot] = garbageColors[pid][1]
			}
		}
		val shift = engine.random.nextInt(2)
		val y = -1*engine.field.hiddenHeight
		for(x in 0..3)
			if(colors[x]!=-1) {
				engine.field.garbageDropPlace(2*x+shift, y, false, 0, colors[x])
				engine.field.getBlock(2*x+shift, y)!!.skin = engine.blkSkin
			}
		garbageColors[pid].clear()
		return true
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		val pid = engine.playerID
		scgettime[pid]++

		val rest = engine.field.howManyGems
		if(flash[pid]) {
			engine.meterValue = rest/3f
			when(rest) {
				1 -> engine.meterColor = GameEngine.METER_COLOR_GREEN
				2 -> engine.meterColor = GameEngine.METER_COLOR_YELLOW
				else -> engine.meterColor = GameEngine.METER_COLOR_RED
			}
		} else {
			engine.meterValue = rest*1f/hoverBlocks[pid]
			engine.meterColor = when {
				rest<=3 -> GameEngine.METER_COLOR_GREEN
				rest<hoverBlocks[pid] shr 2 -> GameEngine.METER_COLOR_YELLOW
				rest<hoverBlocks[pid] shr 1 -> GameEngine.METER_COLOR_ORANGE
				else -> GameEngine.METER_COLOR_RED
			}
		}

		// Settlement
		if(pid==1&&owner.engine[0].gameActive) {
			val p1Lose = owner.engine[0].stat==GameEngine.Status.GAMEOVER||owner.engine[1].field.howManyGems==0
			val p2Lose = owner.engine[1].stat==GameEngine.Status.GAMEOVER||owner.engine[0].field.howManyGems==0
			if(p1Lose&&p2Lose) {
				// Draw
				winnerID = -1
				owner.engine[0].stat = GameEngine.Status.GAMEOVER
				owner.engine[1].stat = GameEngine.Status.GAMEOVER
			} else if(p2Lose) {
				// 1P win
				winnerID = 0
				owner.engine[0].stat = GameEngine.Status.EXCELLENT
				owner.engine[1].stat = GameEngine.Status.GAMEOVER
			} else if(p1Lose) {
				// 2P win
				winnerID = 1
				owner.engine[0].stat = GameEngine.Status.GAMEOVER
				owner.engine[1].stat = GameEngine.Status.EXCELLENT
			}
			if(p1Lose||p2Lose) {
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.engine[0].resetStatc()
				owner.engine[1].resetStatc()
				owner.engine[0].statc[1] = 1
				owner.engine[1].statc[1] = 1
				owner.musMan.bgm = BGM.Silent
			}
		}
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenu(engine, 0, 1, "RESULT", BASE, COLOR.ORANGE)
		when(winnerID) {
			-1 -> receiver.drawMenu(engine, 6, 2, "DRAW", BASE, COLOR.GREEN)
			engine.playerID -> receiver.drawMenu(engine, 6, 2, "WIN!", BASE, COLOR.YELLOW)
			else -> receiver.drawMenu(engine, 6, 2, "LOSE", BASE, COLOR.WHITE)
		}

		drawResultStats(
			engine, receiver, 3, COLOR.ORANGE, Statistic.LINES, Statistic.PIECE, Statistic.LPM, Statistic.PPS, Statistic.TIME
		)
		/* float apm = (float)(garbageSent[playerID] * 3600) /
 (float)(engine.statistics.time);
 drawResult(engine, playerID, receiver, 3, EventReceiver.COLOR.ORANGE,
 "ATTACK", "%10d".format(garbageSent[playerID]),
 "ATTACK/MIN", "%10g".format(apm)); */
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveOtherSetting(engine, owner.replayProp)
		val pid = engine.playerID
		savePreset(engine, owner.replayProp, -1-pid)

		if(useMap[pid]) fldBackup[pid]?.let {saveMap(it, owner.replayProp, pid)}

		owner.replayProp.setProperty("physicianvs.version", version)
		return false
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0

		/** Enabled piece types */
		private val PIECE_ENABLE = listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0)

		/** Block colors */
		private val BLOCK_COLORS = listOf(Block.COLOR.RED, Block.COLOR.BLUE, Block.COLOR.YELLOW)
		private val FLASH_BLOCK_COLORS = BLOCK_COLORS.map {it to Block.TYPE.BLOCK}
		/** Hovering block colors */
		private val HOVER_BLOCK_COLORS = BLOCK_COLORS.map {it to Block.TYPE.GEM}
		//private static final int[] BASE_SPEEDS = {10, 20, 25};

		/** Names of speed settings */
		private val SPEED_NAME = listOf("LOW", "MED", "HI")

		/** Colors for speed settings */
		private val SPEED_COLOR = listOf(COLOR.BLUE, COLOR.YELLOW, COLOR.RED)

		/** Each player's frame cint */
		private val PLAYER_COLOR_FRAME = listOf(GameEngine.FRAME_COLOR_RED, GameEngine.FRAME_COLOR_BLUE)
	}
}
