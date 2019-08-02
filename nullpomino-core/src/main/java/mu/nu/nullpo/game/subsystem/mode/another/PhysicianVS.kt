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
package mu.nu.nullpo.game.subsystem.mode.another

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.subsystem.mode.AbstractMode
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import java.util.*

/** PHYSICIAN VS-BATTLE mode (beta) */
class PhysicianVS:AbstractMode() {

	/** Has accumulatedojama blockOfcount */
	//private int[] garbage;

	/** Had sentojama blockOfcount */
	//private int[] garbageSent;

	/** Time to display the most recent increase in score */
	private var scgettime:IntArray = IntArray(0)

	/** UseBGM */
	private var bgmno:Int = 0

	/** Sound effectsON/OFF */
	private var enableSE:BooleanArray = BooleanArray(0)

	/** MapUse flag */
	private var useMap:BooleanArray = BooleanArray(0)

	/** UseMapSet number */
	private var mapSet:IntArray = IntArray(0)

	/** Map number(-1Random in) */
	private var mapNumber:IntArray = IntArray(0)

	/** Last preset number used */
	private var presetNumber:IntArray = IntArray(0)

	/** Winner */
	private var winnerID:Int = 0

	/** MapSets ofProperty file */
	private var propMap:Array<CustomProperties?> = emptyArray()

	/** MaximumMap number */
	private var mapMaxNo:IntArray = IntArray(0)

	/** For backupfield (MapUsed to save the replay) */
	private var fldBackup:Array<Field?> = emptyArray()

	/** MapRan for selectioncount */
	private var randMap:Random? = null

	/** Version */
	private var version:Int = 0

	/** Flag for all clear */
	//private boolean[] zenKeshi;

	/** Amount of points earned from most recent clear */
	private var lastscore:IntArray = IntArray(0)

	/** Amount of garbage added in current chain */
	//private int[] garbageAdd;

	/** Score */
	private var score:IntArray = IntArray(0)

	/** Number of initial gem blocks */
	private var hoverBlocks:IntArray = IntArray(0)

	/** Speed mode */
	private var speed:IntArray = IntArray(0)

	/** Number gem blocks cleared in current chain */
	private var gemsClearedChainTotal:IntArray = IntArray(0)

	/** Each player's remaining gem count */
	private var rest:IntArray = IntArray(0)

	/** Each player's garbage block colors to be dropped */
	private var garbageColors:Array<IntArray> = emptyArray()

	/** Flash/normal mode settings */
	private var flash:BooleanArray = BooleanArray(0)

	/* Mode name */
	override val name:String
		get() = "PHYSICIAN VS-BATTLE (RC1)"

	override val isVSMode:Boolean
		get() = true

	/* Number of players */
	override val players:Int
		get() = MAX_PLAYERS

	/* Game style */
	override val gameStyle:Int
		get() = GameEngine.GAMESTYLE_PHYSICIAN

	/* Mode initialization */
	override fun modeInit(manager:GameManager) {
		owner = manager
		receiver = owner.receiver

		//garbage = new int[MAX_PLAYERS];
		//garbageSent = new int[MAX_PLAYERS];

		scgettime = IntArray(MAX_PLAYERS)
		bgmno = 0
		enableSE = BooleanArray(MAX_PLAYERS)
		useMap = BooleanArray(MAX_PLAYERS)
		mapSet = IntArray(MAX_PLAYERS)
		mapNumber = IntArray(MAX_PLAYERS)
		presetNumber = IntArray(MAX_PLAYERS)
		propMap = arrayOfNulls(MAX_PLAYERS)
		mapMaxNo = IntArray(MAX_PLAYERS)
		fldBackup = arrayOfNulls(MAX_PLAYERS)
		randMap = Random()

		lastscore = IntArray(MAX_PLAYERS)
		//garbageAdd = new int[MAX_PLAYERS];
		score = IntArray(MAX_PLAYERS)
		hoverBlocks = IntArray(MAX_PLAYERS)
		speed = IntArray(MAX_PLAYERS)
		gemsClearedChainTotal = IntArray(MAX_PLAYERS)
		rest = IntArray(MAX_PLAYERS)
		garbageColors = Array(MAX_PLAYERS) {IntArray(0)}
		flash = BooleanArray(MAX_PLAYERS)

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

	/** Load settings not related to speeds
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 */
	private fun loadOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val playerID = engine.playerID
		bgmno = prop.getProperty("physicianvs.bgmno", 0)
		enableSE[playerID] = prop.getProperty("physicianvs.enableSE.p$playerID", true)
		useMap[playerID] = prop.getProperty("physicianvs.useMap.p$playerID", false)
		mapSet[playerID] = prop.getProperty("physicianvs.mapSet.p$playerID", 0)
		mapNumber[playerID] = prop.getProperty("physicianvs.mapNumber.p$playerID", -1)
		presetNumber[playerID] = prop.getProperty("physicianvs.presetNumber.p$playerID", 0)
		speed[playerID] = prop.getProperty("physicianvs.speed.p$playerID", 1)
		hoverBlocks[playerID] = prop.getProperty("physicianvs.hoverBlocks.p$playerID", 40)
		flash[playerID] = prop.getProperty("physicianvs.flash.p$playerID", false)
	}

	/** Save settings not related to speeds
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val playerID = engine.playerID
		prop.setProperty("physicianvs.bgmno", bgmno)
		prop.setProperty("physicianvs.enableSE.p$playerID", enableSE[playerID])
		prop.setProperty("physicianvs.useMap.p$playerID", useMap[playerID])
		prop.setProperty("physicianvs.mapSet.p$playerID", mapSet[playerID])
		prop.setProperty("physicianvs.mapNumber.p$playerID", mapNumber[playerID])
		prop.setProperty("physicianvs.presetNumber.p$playerID", presetNumber[playerID])
		prop.setProperty("physicianvs.speed.p$playerID", speed[playerID])
		prop.setProperty("physicianvs.hoverBlocks.p$playerID", hoverBlocks[playerID])
		prop.setProperty("physicianvs.flash.p$playerID", flash[playerID])
	}

	/** MapRead
	 * @param field field
	 * @param prop Property file to read from
	 */
	private fun loadMap(field:Field, prop:CustomProperties, id:Int) {
		field.reset()
		//field.readProperty(prop, id);
		field.stringToField(prop.getProperty("values.$id", ""))
		field.setAllAttribute(true, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
		field.setAllAttribute(false, Block.ATTRIBUTE.SELFPLACED)
	}

	/** MapSave
	 * @param field field
	 * @param prop Property file to save to
	 * @param id AnyID
	 */
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
			propMap[playerID] = receiver.loadProperties("config/values/vsbattle/${mapSet[playerID]}.values")
		}

		if(propMap[playerID].isNullOrEmpty())
			engine.field?.reset()
		else propMap[playerID]?.let {
			mapMaxNo[playerID] = it.getProperty("values.maxMapNumber", 0)
			engine.createFieldIfNeeded()
			loadMap(engine.field!!, it, id)
			engine.field!!.setAllSkin(engine.skin)
		}
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		if(playerID==1) {
			engine.randSeed = owner.engine[0].randSeed
			engine.random = Random(owner.engine[0].randSeed)
		}

		engine.framecolor = PLAYER_COLOR_FRAME[playerID]
		engine.clearMode = GameEngine.ClearType.LINE_COLOR
		engine.garbageColorClear = false
		engine.colorClearSize = 4
		engine.lineGravityType = GameEngine.LineGravity.CASCADE
		for(i in 0 until Piece.PIECE_COUNT)
			engine.nextPieceEnable[i] = PIECE_ENABLE[i]==1
		engine.randomBlockColor = true
		engine.blockColors = BLOCK_COLORS
		engine.connectBlocks = true
		engine.cascadeDelay = 18
		engine.gemSameColor = true

		//garbage[playerID] = 0;
		//garbageSent[playerID] = 0;
		score[playerID] = 0
		scgettime[playerID] = 0
		gemsClearedChainTotal[playerID] = 0
		rest[playerID] = 0

		version = if(!engine.owner.replayMode) {
			loadOtherSetting(engine, engine.owner.modeConfig)
			loadPreset(engine, engine.owner.modeConfig, -1-playerID)
			CURRENT_VERSION
		} else {
			loadOtherSetting(engine, engine.owner.replayProp)
			loadPreset(engine, engine.owner.replayProp, -1-playerID)
			owner.replayProp.getProperty("physicianvs.version", 0)
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode&&engine.statc[4]==0) {
			// Configuration changes
			val change = updateCursor(engine, 16)

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl!!.isPress(Controller.BUTTON_E)) m = 100
				if(engine.ctrl!!.isPress(Controller.BUTTON_F)) m = 1000

				when(menuCursor) {
					0 -> {
						engine.speed.gravity += change*m
						if(engine.speed.gravity<-1) engine.speed.gravity = 99999
						if(engine.speed.gravity>99999) engine.speed.gravity = -1
					}
					1 -> {
						engine.speed.denominator += change*m
						if(engine.speed.denominator<-1) engine.speed.denominator = 99999
						if(engine.speed.denominator>99999) engine.speed.denominator = -1
					}
					2 -> {
						engine.speed.are += change
						if(engine.speed.are<0) engine.speed.are = 99
						if(engine.speed.are>99) engine.speed.are = 0
					}
					3 -> {
						engine.speed.areLine += change
						if(engine.speed.areLine<0) engine.speed.areLine = 99
						if(engine.speed.areLine>99) engine.speed.areLine = 0
					}
					4 -> {
						engine.speed.lineDelay += change
						if(engine.speed.lineDelay<0) engine.speed.lineDelay = 99
						if(engine.speed.lineDelay>99) engine.speed.lineDelay = 0
					}
					5 -> {
						engine.speed.lockDelay += change
						if(engine.speed.lockDelay<0) engine.speed.lockDelay = 99
						if(engine.speed.lockDelay>99) engine.speed.lockDelay = 0
					}
					6 -> {
						engine.speed.das += change
						if(engine.speed.das<0) engine.speed.das = 99
						if(engine.speed.das>99) engine.speed.das = 0
					}
					7, 8 -> {
						presetNumber[playerID] += change
						if(presetNumber[playerID]<0) presetNumber[playerID] = 99
						if(presetNumber[playerID]>99) presetNumber[playerID] = 0
					}
					9 -> {
						speed[playerID] += change
						if(speed[playerID]<0) speed[playerID] = 2
						if(speed[playerID]>2) speed[playerID] = 0
					}
					10 -> {
						if(m>=10)
							hoverBlocks[playerID] += change*10
						else
							hoverBlocks[playerID] += change
						if(hoverBlocks[playerID]<1) hoverBlocks[playerID] = 99
						if(hoverBlocks[playerID]>99) hoverBlocks[playerID] = 1
					}
					11 -> flash[playerID] = !flash[playerID]
					12 -> enableSE[playerID] = !enableSE[playerID]
					13 -> {
						bgmno += change
						if(bgmno<0) bgmno = BGM.count-1
						if(bgmno>=BGM.count) bgmno = 0
					}
					14 -> {
						useMap[playerID] = !useMap[playerID]
						if(!useMap[playerID]) {
							if(engine.field!=null) engine.field!!.reset()
						} else
							loadMapPreview(engine, playerID, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
					}
					15 -> {
						mapSet[playerID] += change
						if(mapSet[playerID]<0) mapSet[playerID] = 99
						if(mapSet[playerID]>99) mapSet[playerID] = 0
						if(useMap[playerID]) {
							mapNumber[playerID] = -1
							loadMapPreview(engine, playerID, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
						}
					}
					16 -> if(useMap[playerID]) {
						mapNumber[playerID] += change
						if(mapNumber[playerID]<-1) mapNumber[playerID] = mapMaxNo[playerID]-1
						if(mapNumber[playerID]>mapMaxNo[playerID]-1) mapNumber[playerID] = -1
						loadMapPreview(engine, playerID, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
					} else
						mapNumber[playerID] = -1
				}
			}

			// 決定
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				when(menuCursor) {
					7 -> loadPreset(engine, owner.modeConfig, presetNumber[playerID])
					8 -> {
						savePreset(engine, owner.modeConfig, presetNumber[playerID])
						receiver.saveModeConfig(owner.modeConfig)
					}
					else -> {
						saveOtherSetting(engine, owner.modeConfig)
						savePreset(engine, owner.modeConfig, -1-playerID)
						receiver.saveModeConfig(owner.modeConfig)
						engine.statc[4] = 1
					}
				}
			}

			// Cancel
			if(engine.ctrl!!.isPush(Controller.BUTTON_B)) engine.quitflag = true

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

			if(menuTime>=120)
				engine.statc[4] = 1
			else if(menuTime>=60) menuCursor = 9
		} else // Start
			if(owner.engine[0].statc[4]==1&&owner.engine[1].statc[4]==1&&playerID==1) {
				owner.engine[0].stat = GameEngine.Status.READY
				owner.engine[1].stat = GameEngine.Status.READY
				owner.engine[0].resetStatc()
				owner.engine[1].resetStatc()
			} else if(engine.ctrl!!.isPush(Controller.BUTTON_B)) engine.statc[4] = 0// Cancel

		return true
	}

	/* Setting screen drawing */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(engine.statc[4]==0) {
			if(menuCursor<9) {
				initMenu(COLOR.ORANGE, 0)
				drawMenu(engine, playerID, receiver, "GRAVITY", engine.speed.gravity.toString(), "G-MAX", engine.speed.denominator.toString(), "ARE", engine.speed.are.toString(), "ARE LINE", engine.speed.areLine.toString(), "LINE DELAY", engine.speed.lineDelay.toString(), "LOCK DELAY", engine.speed.lockDelay.toString(), "DAS", engine.speed.das.toString())
				menuColor = COLOR.GREEN
				drawMenu(engine, playerID, receiver, "LOAD", "${presetNumber[playerID]}", "SAVE", "${presetNumber[playerID]}")
			} else {
				initMenu(COLOR.CYAN, 9)
				drawMenu(engine, playerID, receiver, "SPEED", SPEED_NAME[speed[playerID]], "VIRUS", "${hoverBlocks[playerID]}", "MODE",
					if(flash[playerID])
						"FLASH"
					else
						"NORMAL")
				menuColor = COLOR.PINK
				drawMenu(engine, playerID, receiver, "SE", GeneralUtil.getONorOFF(enableSE[playerID]), "BGM", "${BGM.values[bgmno]}")
				menuColor = COLOR.CYAN
				drawMenu(engine, playerID, receiver, "USE MAP", GeneralUtil.getONorOFF(useMap[playerID]), "MAP SET", "${mapSet[playerID]}", "MAP NO.",
					if(mapNumber[playerID]<0)
						"RANDOM"
					else
						"${mapNumber[playerID]}"+"/"+(mapMaxNo[playerID]-1))
			}
		} else
			receiver.drawMenuFont(engine, playerID, 3, 10, "WAIT", COLOR.YELLOW)
	}

	/* Called for initialization during Ready (before initialization) */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) {
			// MapFor storing backup Replay read
			if(useMap[playerID]) {
				if(owner.replayMode) {
					engine.createFieldIfNeeded()
					loadMap(engine.field!!, owner.replayProp, playerID)
					engine.field!!.setAllSkin(engine.skin)
				} else {
					if(propMap[playerID]==null) {
						propMap[playerID] = receiver.loadProperties("config/values/vsbattle/"
							+mapSet[playerID]+".values")
					} else propMap[playerID]?.let {
						engine.createFieldIfNeeded()

						if(mapNumber[playerID]<0) {
							if(playerID==1&&useMap[0]&&mapNumber[0]<0) engine.field!!.copy(owner.engine[0].field)
							else {
								val no = if(mapMaxNo[playerID]<1) 0 else randMap!!.nextInt(mapMaxNo[playerID])
								loadMap(engine.field!!, it, no)
							}
						} else loadMap(engine.field!!, it, mapNumber[playerID])

						engine.field!!.setAllSkin(engine.skin)
						fldBackup[playerID] = Field(engine.field)
					}
				}
			} else engine.field?.reset()
			if(hoverBlocks[playerID]>0) {
				engine.createFieldIfNeeded()
				var minY = 6
				when {
					hoverBlocks[playerID]>=80 -> minY = 3
					hoverBlocks[playerID]>=72 -> minY = 4
					hoverBlocks[playerID]>=64 -> minY = 5
				}
				if(flash[playerID]) {
					engine.field?.addRandomHoverBlocks(engine, hoverBlocks[playerID], BLOCK_COLORS, minY, true, true)
					engine.field?.setAllSkin(12)
				} else engine.field?.addRandomHoverBlocks(engine, hoverBlocks[playerID], HOVER_BLOCK_COLORS, minY, true)
			}
		}

		return false
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.b2bEnable = false
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_CONNECT
		engine.enableSE = enableSE[playerID]
		if(playerID==1) owner.bgmStatus.bgm = BGM.values[bgmno]

		engine.tspinAllowKick = false
		engine.tspinEnable = false
		engine.useAllSpinBonus = false
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		val fldPosX = receiver.fieldX(engine, playerID)
		val fldPosY = receiver.fieldY(engine, playerID)
		val playerColor = if(playerID==0) COLOR.RED else COLOR.BLUE
		val tempX:Int

		// Timer
		if(playerID==0) receiver.drawDirectFont(256, 16, GeneralUtil.getTime(engine.statistics.time))

		if(engine.gameStarted) {
			// Rest
			receiver.drawDirectFont(fldPosX+160, fldPosY+241, "REST", playerColor, .5f)
			tempX = if(rest[playerID]<10) 8 else 0
			receiver.drawDirectFont(fldPosX+160+tempX, fldPosY+257, "${rest[playerID]}",
				if(rest[playerID]<=if(flash[playerID]) 1 else 3) COLOR.RED else COLOR.WHITE)

			// Speed
			receiver.drawDirectFont(fldPosX+156, fldPosY+280, "SPEED", playerColor, .5f)
			receiver.drawDirectFont(fldPosX+152, fldPosY+296, SPEED_NAME[speed[playerID]], SPEED_COLOR[speed[playerID]])
		}

		/* if(playerID == 0) {
 * receiver.drawScore(engine, playerID, -1, 0, "PHYSICIAN VS",
 * EventReceiver.COLOR.GREEN);
 * receiver.drawScore(engine, playerID, -1, 2, "REST",
 * EventReceiver.COLOR.PURPLE);
 * receiver.drawScore(engine, playerID, -1, 3, "1P:",
 * EventReceiver.COLOR.RED);
 * receiver.drawScore(engine, playerID, 3, 3,
 * String.valueOf(rest[0]), (rest[0] <= (flash[playerID] ? 1 : 3)));
 * receiver.drawScore(engine, playerID, -1, 4, "2P:",
 * EventReceiver.COLOR.BLUE);
 * receiver.drawScore(engine, playerID, 3, 4,
 * String.valueOf(rest[1]), (rest[1] <= (flash[playerID] ? 1 : 3)));
 * receiver.drawScore(engine, playerID, -1, 6, "SPEED",
 * EventReceiver.COLOR.GREEN);
 * receiver.drawScore(engine, playerID, -1, 7, "1P:",
 * EventReceiver.COLOR.RED);
 * receiver.drawScore(engine, playerID, 3, 7, SPEED_NAME[speed[0]],
 * SPEED_COLOR[speed[0]]);
 * receiver.drawScore(engine, playerID, -1, 8, "2P:",
 * EventReceiver.COLOR.BLUE);
 * receiver.drawScore(engine, playerID, 3, 8, SPEED_NAME[speed[1]],
 * SPEED_COLOR[speed[1]]);
 * receiver.drawScore(engine, playerID, -1, 10, "SCORE",
 * EventReceiver.COLOR.PURPLE);
 * receiver.drawScore(engine, playerID, -1, 11, "1P: " +
 * String.valueOf(score[0]), EventReceiver.COLOR.RED);
 * receiver.drawScore(engine, playerID, -1, 12, "2P: " +
 * String.valueOf(score[1]), EventReceiver.COLOR.BLUE);
 * receiver.drawScore(engine, playerID, -1, 14, "TIME",
 * EventReceiver.COLOR.GREEN);
 * receiver.drawScore(engine, playerID, -1, 15,
 * GeneralUtil.getTime(engine.statistics.time));
 * } */
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {
		if(engine.field==null) return
		var gemsCleared = engine.field!!.gemsCleared
		if(gemsCleared>0&&lines>0) {
			var pts = 0
			while(gemsCleared>0&&gemsClearedChainTotal[playerID]<5) {
				pts += 1 shl gemsClearedChainTotal[playerID]
				gemsClearedChainTotal[playerID]++
				gemsCleared--
			}
			if(gemsClearedChainTotal[playerID]>=5) pts += gemsCleared shl 5
			pts *= (speed[playerID]+1)*100
			gemsClearedChainTotal[playerID] += gemsCleared
			lastscore[playerID] = pts
			scgettime[playerID] = 120
			engine.statistics.scoreFromLineClear += pts
			engine.statistics.score += pts
			score[playerID] += pts
			engine.playSE("gem")
			setSpeed(engine)
		} else if(lines==0&&!engine.field!!.canCascade())
			if(garbageCheck(engine, playerID)) {
				engine.stat = GameEngine.Status.LINECLEAR
				engine.statc[0] = engine.lineDelay
			}
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	fun setSpeed(engine:GameEngine) {
		engine.speed.gravity =
			intArrayOf(6,8,10)[speed[engine.playerID]]*(10+(engine.statistics.totalPieceLocked/10))
		engine.speed.denominator = 3600
	}

	override fun lineClearEnd(engine:GameEngine, playerID:Int):Boolean {
		engine.field ?: return false

		var enemyID = 0
		if(playerID==0) enemyID = 1


		engine.field!!.lineColorsCleared.let {cleared ->
			if(cleared.size>1)
				if(garbageColors[enemyID].isEmpty()) garbageColors[enemyID] = cleared.clone()
				else {
					val s = garbageColors.size
					cleared.forEachIndexed {i, it -> garbageColors[enemyID][s+i] = it}
				}
		}
		engine.field!!.lineColorsCleared = IntArray(0)
		return garbageCheck(engine, playerID)
	}

	private fun garbageCheck(engine:GameEngine, playerID:Int):Boolean {
		if(garbageColors[playerID].isEmpty()) return false
		val size = garbageColors[playerID].size
		if(size<2) return false
		garbageColors[playerID] = garbageColors[playerID].toMutableList().shuffled(engine.random).toIntArray()
		val colors = IntArray(4)
		when {
			size>=4 -> for(x in 0..3)
				colors[x] = garbageColors[playerID][x]
			size==3 -> {
				val skipSlot = engine.random.nextInt(4)
				colors[skipSlot] = -1
				var i:Int
				for(x in 0..2) {
					i = x
					if(x>=skipSlot) i++
					colors[i] = garbageColors[playerID][x]
				}
			}
			else -> {
				val firstSlot = engine.random.nextInt(4)
				colors[firstSlot] = garbageColors[playerID][0]
				var secondSlot = firstSlot+2
				if(secondSlot>3) secondSlot -= 4
				colors[secondSlot] = garbageColors[playerID][1]
			}
		}
		val shift = engine.random.nextInt(2)
		val y = -1*engine.field!!.hiddenHeight
		for(x in 0..3)
			if(colors[x]!=-1) {
				engine.field!!.garbageDropPlace(2*x+shift, y, false, 0, colors[x])
				engine.field!!.getBlock(2*x+shift, y)!!.skin = engine.skin
			}
		garbageColors[playerID] = IntArray(0)
		return true
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		scgettime[playerID]++

		if(engine.field!=null) {
			val rest = engine.field!!.howManyGems
			if(flash[playerID]) {
				engine.meterValue = rest*receiver.getMeterMax(engine)/3
				when(rest) {
					1 -> engine.meterColor = GameEngine.METER_COLOR_GREEN
					2 -> engine.meterColor = GameEngine.METER_COLOR_YELLOW
					else -> engine.meterColor = GameEngine.METER_COLOR_RED
				}
			} else {
				engine.meterValue = rest*receiver.getMeterMax(engine)/hoverBlocks[playerID]
				engine.meterColor = when {
					rest<=3 -> GameEngine.METER_COLOR_GREEN
					rest<hoverBlocks[playerID] shr 2 -> GameEngine.METER_COLOR_YELLOW
					rest<hoverBlocks[playerID] shr 1 -> GameEngine.METER_COLOR_ORANGE
					else -> GameEngine.METER_COLOR_RED
				}
			}
		}

		// Settlement
		if(playerID==1&&owner.engine[0].gameActive) {
			var p1Lose = owner.engine[0].stat==GameEngine.Status.GAMEOVER
			if(!p1Lose&&owner.engine[1].field!=null) {
				rest[1] = owner.engine[1].field!!.howManyGems
				p1Lose = rest[1]==0
			}
			var p2Lose = owner.engine[1].stat==GameEngine.Status.GAMEOVER
			if(!p2Lose&&owner.engine[0].field!=null) {
				rest[0] = owner.engine[0].field!!.howManyGems
				p2Lose = rest[0]==0
			}
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
				owner.bgmStatus.bgm = BGM.SILENT
			}
		}
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 1, "RESULT", COLOR.ORANGE)
		when(winnerID) {
			-1 -> receiver.drawMenuFont(engine, playerID, 6, 2, "DRAW", COLOR.GREEN)
			playerID -> receiver.drawMenuFont(engine, playerID, 6, 2, "WIN!", COLOR.YELLOW)
			else -> receiver.drawMenuFont(engine, playerID, 6, 2, "LOSE", COLOR.WHITE)
		}

		drawResultStats(engine, playerID, receiver, 3, COLOR.ORANGE, Statistic.LINES, Statistic.PIECE, Statistic.LPM, Statistic.PPS, Statistic.TIME)
		/* float apm = (float)(garbageSent[playerID] * 3600) /
 * (float)(engine.statistics.time);
 * drawResult(engine, playerID, receiver, 3, EventReceiver.COLOR.ORANGE,
 * "ATTACK", String.format("%10d", garbageSent[playerID]),
 * "ATTACK/MIN", String.format("%10g", apm)); */
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveOtherSetting(engine, owner.replayProp)
		savePreset(engine, owner.replayProp, -1-playerID)

		if(useMap[playerID]) fldBackup[playerID]?.let {saveMap(it, owner.replayProp, playerID)}

		owner.replayProp.setProperty("physicianvs.version", version)
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0

		/** Enabled piece types */
		private val PIECE_ENABLE = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0)

		/** Block colors */
		private val BLOCK_COLORS = intArrayOf(Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_YELLOW)
		/** Hovering block colors */
		private val HOVER_BLOCK_COLORS =
			intArrayOf(Block.BLOCK_COLOR_GEM_RED, Block.BLOCK_COLOR_GEM_BLUE, Block.BLOCK_COLOR_GEM_YELLOW)
		//private static final int[] BASE_SPEEDS = {10, 20, 25};

		/** Names of speed settings */
		private val SPEED_NAME = arrayOf("LOW", "MED", "HI")

		/** Colors for speed settings */
		private val SPEED_COLOR = arrayOf(COLOR.BLUE, COLOR.YELLOW, COLOR.RED)

		/** Number of players */
		private const val MAX_PLAYERS = 2
		/** Each player's frame cint */
		private val PLAYER_COLOR_FRAME = intArrayOf(GameEngine.FRAME_COLOR_RED, GameEngine.FRAME_COLOR_BLUE)
	}
}
