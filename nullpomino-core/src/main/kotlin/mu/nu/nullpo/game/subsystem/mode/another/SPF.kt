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
import mu.nu.nullpo.game.play.clearRule.ColorGem
import mu.nu.nullpo.game.subsystem.mode.AbstractMode
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.GameKeyDummy.Companion.MAX_PLAYERS
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import org.apache.logging.log4j.LogManager
import kotlin.random.Random

/** SPF VS-BATTLE mode (Beta) */
class SPF:AbstractMode() {
	/** Has accumulatedojama blockOfcount */
	private var ojama = IntArray(0)

	/** Had sentojama blockOfcount */
	private var ojamaSent = IntArray(0)

	/** Time to display the most recent increase in score */
	private var scgettime = IntArray(0)

	/** UseBGM */
	private var bgmId = 0

	/** Big */
	//private boolean[] big;

	/** Sound effectsON/OFF */
	private var enableSE = BooleanArray(0)

	/** MapUse flag */
	private var useMap = BooleanArray(0)

	/** UseMapSet number */
	private var mapSet = IntArray(0)

	/** Map number(-1Random in) */
	private var mapNumber = IntArray(0)

	/** Last preset number used */
	private var presetNumber = IntArray(0)

	/** Winner */
	private var winnerID = 0

	/** MapSets ofProperty file */
	private var propMap:Array<CustomProperties?> = emptyArray()

	/** MaximumMap number */
	private var mapMaxNo = IntArray(0)

	/** For backupfield (MapUsed to save the replay) */
	private var fldBackup:Array<Field?> = emptyArray()

	/** MapRan for selectioncount */
	private var randMap:Random? = null

	/** Version */
	private var version = 0

	/** Amount of points earned from most recent clear */
	private var lastscores = IntArray(0)

	/** Score */
	private var score = IntArray(0)

	/** Settings for starting countdown for ojama blocks */
	private var ojamaCountdown = IntArray(0)

	/** True if it uses bigger field display */
	private var bigDisplay = false

	/** HurryupSeconds before the startcount(0InHurryupNo) */
	private var hurryUpSeconds = IntArray(0)

	/** Time to display "ZENKESHI!" */
	private var zenKeshiDisplay = IntArray(0)

	/** Time to display "TECH BONUS" */
	private var techBonusDisplay = IntArray(0)

	/** Drop patterns */
	private val dropPattern:MutableList<List<List<Int>>> = MutableList(MAX_PLAYERS) {emptyList()}

	/** Drop values set selected */
	private var dropSet = IntArray(0)

	/** Drop values selected */
	private var dropMap = IntArray(0)

	/** Drop multipliers */
	private var attackMultiplier = DoubleArray(0)
	private var defendMultiplier = DoubleArray(0)

	/** Rainbow power settings for each player */
	private var diamondPower = IntArray(0)

	/** Frame when squares were last checked */
	private var lastSquareCheck = IntArray(0)

	/** Flag set when counters have been decremented */
	private var countdownDecremented = BooleanArray(0)

	/* Mode name */
	override val name = "SPF VS-BATTLE (BETA)"
	override val gameIntensity = 2
	override val isVSMode get() = true

	/* Number of players */
	override val players:Int get() = MAX_PLAYERS

	/* Game style */
	override val gameStyle = GameStyle.SPF
	/* Mode initialization */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)

		ojama = IntArray(MAX_PLAYERS)
		ojamaSent = IntArray(MAX_PLAYERS)

		scgettime = IntArray(MAX_PLAYERS)
		bgmId = 0
		//big = new boolean[MAX_PLAYERS];
		enableSE = BooleanArray(MAX_PLAYERS)
		hurryUpSeconds = IntArray(MAX_PLAYERS)
		useMap = BooleanArray(MAX_PLAYERS)
		mapSet = IntArray(MAX_PLAYERS)
		mapNumber = IntArray(MAX_PLAYERS)
		presetNumber = IntArray(MAX_PLAYERS)
		propMap = arrayOfNulls(MAX_PLAYERS)
		mapMaxNo = IntArray(MAX_PLAYERS)
		fldBackup = arrayOfNulls(MAX_PLAYERS)
		randMap = Random.Default

		lastscores = IntArray(MAX_PLAYERS)
		score = IntArray(MAX_PLAYERS)
		ojamaCountdown = IntArray(MAX_PLAYERS)

		zenKeshiDisplay = IntArray(MAX_PLAYERS)
		techBonusDisplay = IntArray(MAX_PLAYERS)

		dropSet = IntArray(MAX_PLAYERS)
		dropMap = IntArray(MAX_PLAYERS)
		dropPattern.fill(emptyList())
		attackMultiplier = DoubleArray(MAX_PLAYERS)
		defendMultiplier = DoubleArray(MAX_PLAYERS)
		diamondPower = IntArray(MAX_PLAYERS)
		lastSquareCheck = IntArray(MAX_PLAYERS)
		countdownDecremented = BooleanArray(MAX_PLAYERS)

		winnerID = -1
	}

	/** Read speed presets
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 * @param preset Preset number
	 */
	private fun loadPreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		engine.speed.gravity = prop.getProperty("spfvs.gravity.$preset", 4)
		engine.speed.denominator = prop.getProperty("spfvs.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("spfvs.are.$preset", 24)
		engine.speed.areLine = prop.getProperty("spfvs.areLine.$preset", 24)
		engine.speed.lineDelay = prop.getProperty("spfvs.lineDelay.$preset", 10)
		engine.speed.lockDelay = prop.getProperty("spfvs.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("spfvs.das.$preset", 14)
	}

	/** Save speed presets
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 * @param preset Preset number
	 */
	private fun savePreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		prop.setProperty("spfvs.gravity.$preset", engine.speed.gravity)
		prop.setProperty("spfvs.denominator.$preset", engine.speed.denominator)
		prop.setProperty("spfvs.are.$preset", engine.speed.are)
		prop.setProperty("spfvs.areLine.$preset", engine.speed.areLine)
		prop.setProperty("spfvs.lineDelay.$preset", engine.speed.lineDelay)
		prop.setProperty("spfvs.lockDelay.$preset", engine.speed.lockDelay)
		prop.setProperty("spfvs.das.$preset", engine.speed.das)
	}

	/** Load settings into [engine] from [prop] not related to speeds */
	private fun loadOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val playerID = engine.playerID
		bgmId = prop.getProperty("spfvs.bgmno", 0)
		//big[playerID] = prop.getProperty("spfvs.big.p" + playerID, false);
		enableSE[playerID] = prop.getProperty("spfvs.enableSE.p$playerID", true)
		hurryUpSeconds[playerID] = prop.getProperty("vsbattle.hurryupSeconds.p$playerID", 0)
		useMap[playerID] = prop.getProperty("spfvs.useMap.p$playerID", false)
		mapSet[playerID] = prop.getProperty("spfvs.mapSet.p$playerID", 0)
		mapNumber[playerID] = prop.getProperty("spfvs.mapNumber.p$playerID", -1)
		presetNumber[playerID] = prop.getProperty("spfvs.presetNumber.p$playerID", 0)
		ojamaCountdown[playerID] = prop.getProperty("spfvs.ojamaHard.p$playerID", 5)
		bigDisplay = prop.getProperty("spfvs.bigDisplay", false)
		dropSet[playerID] = prop.getProperty("spfvs.dropSet.p$playerID", 0)
		dropMap[playerID] = prop.getProperty("spfvs.dropMap.p$playerID", 0)
		diamondPower[playerID] = prop.getProperty("spfvs.rainbowPower.p$playerID", 2)
	}

	/** Save settings from [engine] into [prop] not related to speeds */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val playerID = engine.playerID
		prop.setProperty("spfvs.bgmno", bgmId)
		//prop.setProperty("spfvs.big.p" + playerID, big[playerID]);
		prop.setProperty("spfvs.enableSE.p$playerID", enableSE[playerID])
		prop.setProperty("vsbattle.hurryupSeconds.p$playerID", hurryUpSeconds[playerID])
		prop.setProperty("spfvs.useMap.p$playerID", useMap[playerID])
		prop.setProperty("spfvs.mapSet.p$playerID", mapSet[playerID])
		prop.setProperty("spfvs.mapNumber.p$playerID", mapNumber[playerID])
		prop.setProperty("spfvs.presetNumber.p$playerID", presetNumber[playerID])
		prop.setProperty("spfvs.ojamaHard.p$playerID", ojamaCountdown[playerID])
		prop.setProperty("spfvs.bigDisplay", bigDisplay)
		prop.setProperty("spfvs.dropSet.p$playerID", dropSet[playerID])
		prop.setProperty("spfvs.dropMap.p$playerID", dropMap[playerID])
		prop.setProperty("spfvs.rainbowPower.p$playerID", diamondPower[playerID])
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
			propMap[playerID] = receiver.loadProperties("config/map/spf/${mapSet[playerID]}.map")
		}
		propMap[playerID]?.let {
			mapMaxNo[playerID] = it.getProperty("values.maxMapNumber", 0)
			engine.createFieldIfNeeded()
			loadMap(engine.field, it, id)
			engine.field.setAllSkin(engine.skin)
		} ?: engine.field.reset()
	}

	private fun loadDropMapPreview(engine:GameEngine, pattern:List<List<Int>>?) {
		pattern?.let {
			log.debug("Loading drop values preview")
			engine.createFieldIfNeeded().run {
				reset()
				var patternCol = 0
				val maxHeight = height-1
				for(x in 0..<width) {
					if(patternCol>=it.size) patternCol = 0
					for(patternRow in it[patternCol].indices) {
						setBlockColor(x, maxHeight-patternRow, it[patternCol][patternRow])
						getBlock(x, maxHeight-patternRow)?.run {
							setAttribute(true, Block.ATTRIBUTE.VISIBLE)
							setAttribute(true, Block.ATTRIBUTE.OUTLINE)
						}
					}
					patternCol++
				}
				setAllSkin(engine.skin)
			}
		} ?: engine.field.reset()
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine) {
		val pid = engine.playerID
		if(pid==1) {
			engine.randSeed = owner.engine[0].randSeed
			engine.random = Random(owner.engine[0].randSeed)
		}

		engine.frameColor = PLAYER_COLOR_FRAME[pid]
		engine.clearMode = ColorGem(2,true,false)
		engine.colorClearSize = 2
		engine.ignoreHidden = false
		engine.garbageColorClear = true
		engine.lineGravityType = LineGravity.CASCADE
		engine.nextPieceEnable = PIECE_ENABLE.map {it==1}
		engine.blockColors = BLOCK_COLORS
		engine.gemRate = 0.2f
		engine.randomBlockColor = true
		engine.connectBlocks = false

		ojama[pid] = 0
		ojamaSent[pid] = 0
		score[pid] = 0
		scgettime[pid] = 0
		zenKeshiDisplay[pid] = 0
		techBonusDisplay[pid] = 0
		lastSquareCheck[pid] = -1
		countdownDecremented[pid] = true

		version = if(!engine.owner.replayMode) {
			loadOtherSetting(engine, engine.owner.modeConfig)
			loadPreset(engine, engine.owner.modeConfig, -1-pid)
			CURRENT_VERSION
		} else {
			loadOtherSetting(engine, engine.owner.replayProp)
			loadPreset(engine, engine.owner.replayProp, -1-pid)
			owner.replayProp.getProperty("spfvs.version", 0)
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		val pid = engine.playerID
		if(!engine.owner.replayMode&&engine.statc[4]==0) {
			// Up
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
				menuCursor--
				if(menuCursor<0) {
					menuCursor = 19
					loadDropMapPreview(engine, DROP_PATTERNS[dropSet[pid]][dropMap[pid]])
				} else if(menuCursor==17) engine.field.reset()
				engine.playSE("cursor")
			}
			// Down
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
				menuCursor++
				if(menuCursor>19) {
					menuCursor = 0
					engine.field.reset()
				} else if(menuCursor==18) loadDropMapPreview(engine, DROP_PATTERNS[dropSet[pid]][dropMap[pid]])
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
					0 -> engine.speed.gravity = rangeCursor(engine.speed.gravity+change*m, -1, 99999)
					1 -> engine.speed.denominator = rangeCursor(change*m, -1, 99999)
					2 -> engine.speed.are = rangeCursor(engine.speed.are+change, 0, 99)
					3 -> engine.speed.areLine = rangeCursor(engine.speed.areLine+change, 0, 99)
					4 -> engine.speed.lineDelay = rangeCursor(engine.speed.lineDelay+change, 0, 99)
					5 -> engine.speed.lockDelay = rangeCursor(engine.speed.lockDelay+change, 0, 99)
					6 -> engine.speed.das = rangeCursor(engine.speed.das+change, 0, 99)
					7, 8 -> presetNumber[pid] = rangeCursor(presetNumber[pid]+change, 0, 99)
					9 -> bgmId = rangeCursor(bgmId+change, 0, BGM.count-1)
					10 -> {
						useMap[pid] = !useMap[pid]
						if(!useMap[pid]) engine.field.reset() else
							loadMapPreview(engine, pid, if(mapNumber[pid]<0) 0 else mapNumber[pid], true)
					}
					11 -> {
						mapSet[pid] += change
						if(mapSet[pid]<0) mapSet[pid] = 99
						if(mapSet[pid]>99) mapSet[pid] = 0
						if(useMap[pid]) {
							mapNumber[pid] = -1
							loadMapPreview(engine, pid, if(mapNumber[pid]<0) 0 else mapNumber[pid], true)
						}
					}
					12 -> if(useMap[pid]) {
						mapNumber[pid] += change
						if(mapNumber[pid]<-1) mapNumber[pid] = mapMaxNo[pid]-1
						if(mapNumber[pid]>mapMaxNo[pid]-1) mapNumber[pid] = -1
						loadMapPreview(engine, pid, if(mapNumber[pid]<0) 0 else mapNumber[pid], true)
					} else
						mapNumber[pid] = -1
					13 -> enableSE[pid] = !enableSE[pid]
					14 -> {
						if(m>10)
							hurryUpSeconds[pid] += change*m/10
						else
							hurryUpSeconds[pid] += change
						if(hurryUpSeconds[pid]<0) hurryUpSeconds[pid] = 300
						if(hurryUpSeconds[pid]>300) hurryUpSeconds[pid] = 0
					}
					15 -> {
						ojamaCountdown[pid] += change
						if(ojamaCountdown[pid]<1) ojamaCountdown[pid] = 9
						if(ojamaCountdown[pid]>9) ojamaCountdown[pid] = 1
					}
					16 -> bigDisplay = !bigDisplay
					17 -> {
						diamondPower[pid] += change
						if(diamondPower[pid]<0) diamondPower[pid] = 3
						if(diamondPower[pid]>3) diamondPower[pid] = 0
					}
					18 -> {
						dropSet[pid] += change
						if(dropSet[pid]<0) dropSet[pid] = DROP_PATTERNS.size-1
						if(dropSet[pid]>=DROP_PATTERNS.size) dropSet[pid] = 0
						if(dropMap[pid]>=DROP_PATTERNS[dropSet[pid]].size) dropMap[pid] = 0
						loadDropMapPreview(engine, DROP_PATTERNS[dropSet[pid]][dropMap[pid]])
					}
					19 -> {
						dropMap[pid] += change
						if(dropMap[pid]<0) dropMap[pid] = DROP_PATTERNS[dropSet[pid]].size-1
						if(dropMap[pid]>=DROP_PATTERNS[dropSet[pid]].size) dropMap[pid] = 0
						loadDropMapPreview(engine, DROP_PATTERNS[dropSet[pid]][dropMap[pid]])
					}
				}/* case 20:
					 * big[pid] = !big[pid];
					 * break; */
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

			if(menuTime>=180)
				engine.statc[4] = 1
			else if(menuTime>120)
				menuCursor = 18
			else if(menuTime==120) {
				menuCursor = 18
				loadDropMapPreview(engine, DROP_PATTERNS[dropSet[pid]][dropMap[pid]])
			} else if(menuTime>=60) menuCursor = 9
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
				receiver.drawMenuFont(engine, 0, 19, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE 1/3", COLOR.YELLOW)
			} else if(menuCursor<18) {
				drawMenu(engine, receiver, 0, COLOR.PINK, 9, "BGM" to BGM.values[bgmId])

				drawMenu(
					engine,
					receiver,
					COLOR.CYAN,
					"USE MAP" to useMap[pid],
					"MAP SET" to mapSet[pid],
					"MAP NO." to if(mapNumber[pid]<0) "RANDOM" else "${mapNumber[pid]}/${mapMaxNo[pid]-1}",
					"SE" to enableSE[pid],
					"HURRYUP" to if(hurryUpSeconds[pid]==0) "NONE" else "${hurryUpSeconds[pid]}SEC",
					"COUNTDOWN" to ojamaCountdown[pid]
				)
				drawMenu(engine, receiver, COLOR.PINK, "BIG DISP" to bigDisplay)
				drawMenu(engine, receiver, COLOR.CYAN, "RAINBOW" to "")
				drawMenu(engine, receiver, "GEM POWER" to RAINBOW_POWER_NAMES[diamondPower[pid]])

				receiver.drawMenuFont(engine, 0, 19, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE 2/3", COLOR.YELLOW)
			} else {
				receiver.drawMenuFont(engine, 0, 0, "ATTACK", COLOR.CYAN)
				var multiplier = (100*getAttackMultiplier(dropSet[pid], dropMap[pid])).toInt()
				if(multiplier>=100)
					receiver.drawMenuFont(
						engine, 2, 1, "$multiplier%", if(multiplier==100) COLOR.YELLOW else COLOR.GREEN
					)
				else
					receiver.drawMenuFont(engine, 3, 1, "$multiplier%", COLOR.RED)
				receiver.drawMenuFont(engine, 0, 2, "DEFEND", COLOR.CYAN)
				multiplier = (100*getDefendMultiplier(dropSet[pid], dropMap[pid])).toInt()
				if(multiplier>=100)
					receiver.drawMenuFont(
						engine, 2, 3, "$multiplier%", if(multiplier==100) COLOR.YELLOW else COLOR.RED
					)
				else
					receiver.drawMenuFont(engine, 3, 3, "$multiplier%", COLOR.GREEN)

				drawMenu(
					engine,
					receiver,
					14,
					COLOR.CYAN,
					18,
					"DROP SET" to DROP_SET_NAMES[dropSet[pid]],
					"DROP MAP" to "%2d".format(dropMap[pid]+1)+"/"+String.format(
						"%2d",
						DROP_PATTERNS[dropSet[pid]].size
					)
				)

				receiver.drawMenuFont(engine, 0, 19, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE 3/3", COLOR.YELLOW)
			}
		} else
			receiver.drawMenuFont(engine, 3, 10, "WAIT", COLOR.YELLOW)
	}

	/* Called for initialization during Ready (before initialization) */
	override fun onReady(engine:GameEngine):Boolean {
		val pid = engine.playerID
		if(engine.statc[0]==0) {
			engine.numColors = BLOCK_COLORS.size
			engine.rainbowAnimate = pid==0
			engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_CONNECT
			engine.displaySize = if(bigDisplay) 1 else 0

			dropPattern[pid] = DROP_PATTERNS[dropSet[pid]][dropMap[pid]]
			attackMultiplier[pid] = getAttackMultiplier(dropSet[pid], dropMap[pid])
			defendMultiplier[pid] = getDefendMultiplier(dropSet[pid], dropMap[pid])

			// MapFor storing backup Replay read
			if(useMap[pid]) {
				if(owner.replayMode) {
					engine.createFieldIfNeeded()
					loadMap(engine.field, owner.replayProp, pid)
					engine.field.setAllSkin(engine.skin)
				} else {
					if(propMap[pid]==null)
						propMap[pid] = receiver.loadProperties("config/map/spf/${mapSet[pid]}.map")
					else propMap[pid]?.let {
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
		} else if(engine.statc[0]==1&&diamondPower[pid]>0) {
			var x = 24
			while(x<engine.nextPieceArraySize) {
				engine.nextPieceArrayObject[x].block[1].run {
					color = Block.COLOR.RAINBOW
					type = Block.TYPE.GEM
				}
				x += 25
			}
		}
		return false
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		engine.b2bEnable = false
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		//engine.big = big[engine.playerID];
		engine.enableSE = enableSE[engine.playerID]
		if(engine.playerID==1) owner.musMan.bgm = BGM.values[bgmId]
		//engine.colorClearSize = big[engine.playerID] ? 8 : 2;

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

		// Timer
		if(pid==0) receiver.drawDirectFont(224, 0, engine.statistics.time.toTimeStr)

		// Ojama Counter
		val fontColor = when {
			ojama[pid]>=1 -> COLOR.YELLOW
			ojama[pid]>=6 -> COLOR.ORANGE
			ojama[pid]>=12 -> COLOR.RED
			else -> COLOR.WHITE
		}
		if(ojama[pid]!=0) receiver.drawDirectFont(fldPosX+4, fldPosY+32, "${ojama[pid]}", fontColor)

		// Score
		if(engine.displaySize==1)
			receiver.drawDirectFont(fldPosX+4, fldPosY+472, "%12d".format(score[pid]), playerColor)
		else if(engine.gameStarted)
			receiver.drawDirectFont(fldPosX-28, fldPosY+264, "%8d".format(score[pid]), playerColor)

		// Countdown Blocks
		if(engine.gameActive)
			for(x in 0..<engine.field.width)
				for(y in 0..<engine.field.height)
					engine.field.getBlock(x, y)?.let {b ->
						if(b.countdown>0) {
							val textColor = when(b.secondaryColor) {
								Block.COLOR.BLUE -> COLOR.BLUE
								Block.COLOR.GREEN -> COLOR.GREEN
								Block.COLOR.RED -> COLOR.RED
								Block.COLOR.YELLOW -> COLOR.YELLOW
								else -> COLOR.WHITE
							}
							if(engine.displaySize==1)
								receiver.drawMenuFont(
									engine, x*2, y*2,
									b.countdown.toString(), textColor, 2f
								)
							else
								receiver.drawMenuFont(engine, x, y, b.countdown.toString(), textColor)
						}
					}

		// On-screen Texts

		val textHeight = if(engine.displaySize==1) 11 else engine.field.height+3
		val baseX = if(engine.displaySize==1) 1 else -2

		if(techBonusDisplay[pid]>0)
			receiver.drawMenuFont(engine, baseX, textHeight, "TECH BONUS", COLOR.YELLOW)
		if(zenKeshiDisplay[pid]>0)
			receiver.drawMenuFont(engine, baseX+1, textHeight+1, "PERFECT!", COLOR.YELLOW)
	}

	override fun onMove(engine:GameEngine):Boolean {
		countdownDecremented[engine.playerID] = false
		return false
	}

	override fun pieceLocked(engine:GameEngine, lines:Int, finesse:Boolean) {
		checkAll(engine)
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		val field = engine.field
		if(field.canCascade()) return 0
		checkAll(engine)

		val pid = engine.playerID
		val enemyID = if(pid==0) 1 else 0

		val width = field.width
		val height = field.height
		val hiddenHeight = field.hiddenHeight

		var diamondBreakColor:Block.COLOR? = null
		if(diamondPower[pid]>0) {
			val all = field.findBlocks(true) {
				it.color==Block.COLOR.RAINBOW&&it.type==Block.TYPE.GEM
			}
			if(all.isNotEmpty()) {
				if(all.keys.any {y -> y+1>=height}) {
					techBonusDisplay[pid] = 120
					engine.statistics.scoreLine += 10000
					score[pid] += 10000
				}
				diamondBreakColor = all.entries.maxBy {it.key}.let {(y, value) ->
					val x = value.minBy {it.key}.key
					field.getBlockColor(x, y+1)
				}
			}
			field.delBlocks(all).let {receiver.blockBreak(engine, it)}
		}

		var pts = 0.0
		//Clear blocks from diamond
		diamondBreakColor?.let {dbc ->
			field.allClearColor(dbc, true, true)
			val all = field.findBlocks(true) {it.color==dbc}
			if(all.isNotEmpty()) {
				pts += all.entries.sumOf {(y, it) -> getRowValue(y)*7*it.size}
				field.delBlocks(all).let {receiver.blockBreak(engine, it)}
			}
		}
		if(diamondPower[pid]==1) pts *= 0.5
		else if(diamondPower[pid]==2) pts *= 0.8
		//TODO: Add diamond glitch

		//Clear blocks
		val all = field.findBlocks(true) {it.getAttribute(Block.ATTRIBUTE.ERASE)}
		pts += all.entries.sumOf {(y, it) ->
			it.values.sumOf {
				getRowValue(y)*7*maxOf(1, it.bonusValue)/if(it.getAttribute(Block.ATTRIBUTE.GARBAGE)) 2 else 1
			}
		}
		receiver.blockBreak(engine, all)
		all.forEach {(y, r) -> r.forEach {(x, _) -> field.delBlock(x, y)}}

		if(engine.chain>1) pts += (engine.chain-1)*20.0

		if(engine.chain>=1) engine.playSE("combo", minOf(2f, 1f+(engine.chain-2)/7f))

		var pow = (pts*attackMultiplier[pid]/7.0).toInt().toDouble()

		if(field.isEmpty) {
			zenKeshiDisplay[pid] = 120
			pow += 12.0
			engine.statistics.scoreBonus += 1000
			score[pid] += 1000
		}

		lastscores[pid] = pts.toInt()*10
		scgettime[pid] = 120
		score[pid] += lastscores[pid]

		if(hurryUpSeconds[pid]>0&&engine.statistics.time>hurryUpSeconds[pid])
			pow *= (1 shl engine.statistics.time/(hurryUpSeconds[pid]*60)).toDouble()
		var ojamaSend = pow
		if(ojama[pid]>0&&ojamaSend>0.0) {
			val delta = minOf(ojama[pid] shl 1, ojamaSend.toInt())
			ojama[pid] -= delta shr 1
			ojamaSend -= delta.toDouble()
		}
		ojama[enemyID] += maxOf(0.0, (ojamaSend*defendMultiplier[enemyID])).toInt()
		return pow.toInt()
	}

	fun checkAll(engine:GameEngine) {
		val recheck = checkCountdown(engine)
		if(recheck) log.debug("Converted garbage blocks to regular blocks. Rechecking squares.")
		checkSquares(engine, recheck)
	}

	fun checkCountdown(engine:GameEngine):Boolean {
		if(countdownDecremented[engine.playerID]) return false
		countdownDecremented[engine.playerID] = true
		var result = false
		for(y in engine.field.hiddenHeight*-1..<engine.field.height)
			for(x in 0..<engine.field.width) {
				val b = engine.field.getBlock(x, y) ?: continue
				if(b.countdown>1)
					b.countdown--
				else if(b.countdown==1) {
					b.countdown = 0
					b.setAttribute(false, Block.ATTRIBUTE.GARBAGE)
					b.color = b.secondaryColor
					result = true
				}
			}
		return result
	}

	fun checkSquares(engine:GameEngine, forceRecheck:Boolean) {
		if(engine.field.isEmpty) return
		if(engine.statistics.time==lastSquareCheck[engine.playerID]&&!forceRecheck) return
		lastSquareCheck[engine.playerID] = engine.statistics.time

		//log.debug("Checking squares.");

		val width = engine.field.width
		val height = engine.field.height
		val hiddenHeight = engine.field.hiddenHeight

		for(x in 0..<width)
			for(y in -1*hiddenHeight..<height) {
				val b = engine.field.getBlock(x, y) ?: continue
				val color = b.color ?: continue
				if(!color.color||b.type!=Block.TYPE.GEM) continue
				var minX = x
				var minY = y
				var maxX = x
				var maxY = y
				var expanded = false
				if(!b.getAttribute(Block.ATTRIBUTE.BROKEN)&&
					b.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)&&b.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)&&
					!b.getAttribute(Block.ATTRIBUTE.CONNECT_UP)&&!b.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) {
					//Find boundaries of existing gem block
					maxX++
					maxY++
					var test:Block?
					while(maxX<width) {
						test = engine.field.getBlock(maxX, y)
						if(test==null) {
							maxX--
							break
						}
						if(test!=b) {
							maxX--
							break
						}
						if(!test.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) break
						maxX++
					}
					while(maxY<height) {
						test = engine.field.getBlock(x, maxY)
						if(test==null) {
							maxY--
							break
						}
						if(test!=b) {
							maxY--
							break
						}
						if(!test.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) break
						maxY++
					}
					log.debug(
						"Pre-existing square found: ($minX, $minY) to ("+
							maxX+", $maxY)"
					)
				} else if(b.getAttribute(Block.ATTRIBUTE.BROKEN)&&
					color==engine.field.getBlockColor(x+1, y)&&
					color==engine.field.getBlockColor(x, y+1)&&
					color==engine.field.getBlockColor(x+1, y+1)) {
					val bR = engine.field.getBlock(x+1, y)
					val bD = engine.field.getBlock(x, y+1)
					val bDR = engine.field.getBlock(x+1, y+1)
					if(bR!!.getAttribute(Block.ATTRIBUTE.BROKEN)&&
						bD!!.getAttribute(Block.ATTRIBUTE.BROKEN)&&
						bDR!!.getAttribute(Block.ATTRIBUTE.BROKEN)) {
						//Form new gem block
						maxX = x+1
						maxY = y+1
						b.setAttribute(false, Block.ATTRIBUTE.BROKEN)
						bR.setAttribute(false, Block.ATTRIBUTE.BROKEN)
						bD.setAttribute(false, Block.ATTRIBUTE.BROKEN)
						bDR.setAttribute(false, Block.ATTRIBUTE.BROKEN)
						expanded = true
					}
					log.debug(
						"New square formed: ($minX, $minY) to ("+
							maxX+", $maxY)"
					)
				}
				if(maxX<=minX||maxY<=minY) continue //No gem block, skip to next block
				var expandHere:Boolean
				var done:Boolean
				var testX:Int
				var testY:Int
				var bTest:Block?
				log.debug(
					"Testing square for expansion. Coordinates before: ($minX, $minY) to ("+
						maxX+", $maxY)"
				)
				//Expand up
				testY = minY-1
				done = false
				while(testY>=-1*hiddenHeight&&!done) {
					log.debug("Testing to expand up. testY = $testY")
					if(color!=engine.field.getBlockColor(minX, testY)||color!=engine.field.getBlockColor(maxX, testY))
						break
					if(engine.field.getBlock(minX, testY)!!
							.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)||engine.field.getBlock(maxX, testY)!!
							.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT))
						break
					expandHere = true
					testX = minX
					while(testX<=maxX&&!done) {
						if(engine.field.getBlockColor(testX, testY)!=color) {
							done = true
							expandHere = false
						} else if(engine.field.getBlock(testX, testY)!!.getAttribute(Block.ATTRIBUTE.CONNECT_UP))
							expandHere = false
						testX++
					}
					if(expandHere) {
						minY = testY
						expanded = true
					}
					testY--
				}
				//Expand left
				testX = minX-1
				done = false
				while(testX>=0&&!done) {
					if(color!=engine.field.getBlockColor(testX, minY)||color!=engine.field.getBlockColor(testX, maxY))
						break
					if(engine.field.getBlock(testX, minY)!!
							.getAttribute(Block.ATTRIBUTE.CONNECT_UP)||engine.field.getBlock(testX, maxY)!!
							.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN))
						break
					expandHere = true
					testY = minY
					while(testY<=maxY&&!done) {
						if(engine.field.getBlockColor(testX, testY)!=color) {
							done = true
							expandHere = false
						} else if(engine.field.getBlock(testX, testY)!!.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT))
							expandHere = false
						testY++
					}
					if(expandHere) {
						minX = testX
						expanded = true
					}
					testX--
				}
				//Expand right
				testX = maxX+1
				done = false
				while(testX<width&&!done) {
					if(color!=engine.field.getBlockColor(testX, minY)||color!=engine.field.getBlockColor(testX, maxY))
						break
					if(engine.field.getBlock(testX, minY)!!
							.getAttribute(Block.ATTRIBUTE.CONNECT_UP)||engine.field.getBlock(testX, maxY)!!
							.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN))
						break
					expandHere = true
					testY = minY
					while(testY<=maxY&&!done) {
						if(engine.field.getBlockColor(testX, testY)!=color) {
							done = true
							expandHere = false
						} else if(engine.field.getBlock(testX, testY)!!.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT))
							expandHere = false
						testY++
					}
					if(expandHere) {
						maxX = testX
						expanded = true
					}
					testX++
				}
				//Expand down
				testY = maxY+1
				done = false
				while(testY<height&&!done) {
					if(color!=engine.field.getBlockColor(minX, testY)||color!=engine.field.getBlockColor(maxX, testY))
						break
					if(engine.field.getBlock(minX, testY)!!
							.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)||engine.field.getBlock(maxX, testY)!!
							.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT))
						break
					expandHere = true
					testX = minX
					while(testX<=maxX&&!done) {
						if(engine.field.getBlockColor(testX, testY)!=color) {
							done = true
							expandHere = false
						} else if(engine.field.getBlock(testX, testY)!!.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN))
							expandHere = false
						testX++
					}
					if(expandHere) {
						maxY = testY
						expanded = true
					}
					testY++
				}
				log.debug("expanded = $expanded")
				if(expanded) {
					log.debug(
						"Expanding square. Coordinates after: ($minX, $minY) to ("+
							maxX+", $maxY)"
					)
					val size = minOf(maxX-minX+1, maxY-minY+1)
					testX = minX
					while(testX<=maxX) {
						testY = minY
						while(testY<=maxY) {
							bTest = engine.field.getBlock(testX, testY)
							bTest!!.setAttribute(false, Block.ATTRIBUTE.BROKEN)
							bTest.setAttribute(testX!=minX, Block.ATTRIBUTE.CONNECT_LEFT)
							bTest.setAttribute(testY!=maxY, Block.ATTRIBUTE.CONNECT_DOWN)
							bTest.setAttribute(testY!=minY, Block.ATTRIBUTE.CONNECT_UP)
							bTest.setAttribute(testX!=maxX, Block.ATTRIBUTE.CONNECT_RIGHT)
							bTest.bonusValue = size
							testY++
						}
						testX++
					}
				}
			}
	}

	override fun lineClearEnd(engine:GameEngine):Boolean {
		if(engine.field.isEmpty) return false

		val width = engine.field.width
		val height = engine.field.height
		val hiddenHeight = engine.field.hiddenHeight

		for(y in -1*hiddenHeight..<height)
			for(x in 0..<width)
				if(engine.field.getBlockColor(x, y)==Block.COLOR.RAINBOW) {
					calcScore(engine, ScoreEvent())
					return true
				}

		checkAll(engine)

		//Drop garbage if needed.
		val pid = engine.playerID
		if(ojama[pid]>0) {
			val enemyID = if(pid==0) 1 else 0

			val dropRows = minOf((ojama[pid]+width-1)/width, engine.field.getHighestBlockY(3))
			if(dropRows<=0) return false
			val drop = minOf(ojama[pid], width*dropRows)
			ojama[pid] -= drop
			//engine.field.garbageDrop(engine, drop, big[pid], ojamaHard[pid], 3);
			engine.field.garbageDrop(engine, drop, false, 0, ojamaCountdown[pid], 3)
			engine.field.setAllSkin(engine.skin)
			var patternCol = 0
			for(x in 0..<engine.field.width) {
				if(patternCol>=dropPattern[enemyID].size) patternCol = 0
				var patternRow = 0
				for(y in dropRows-hiddenHeight downTo -1*hiddenHeight) {
					engine.field.getBlock(x, y)?.let {b ->
						if(b.getAttribute(Block.ATTRIBUTE.GARBAGE)&&!b.secondaryColor.color) {
							if(patternRow>=dropPattern[enemyID][patternCol].size) patternRow = 0
							Block.intToColor(dropPattern[enemyID][patternCol][patternRow]).first?.let {
								b.secondaryColor = it
							}
							patternRow++
						}
					}
				}
				patternCol++
			}
			return true
		}
		return false
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		val pid = engine.playerID
		scgettime[pid]++
		if(zenKeshiDisplay[pid]>0) zenKeshiDisplay[pid]--
		val width = engine.field.width
		val blockHeight = engine.blockSize
		// Rising auctionMeter
		if(ojama[pid]*blockHeight/width>engine.meterValue)
			engine.meterValue++
		else if(ojama[pid]*blockHeight/width<engine.meterValue) engine.meterValue--
		engine.meterColor = when {
			ojama[pid]>30 -> GameEngine.METER_COLOR_RED
			ojama[pid]>10 -> GameEngine.METER_COLOR_YELLOW
			else -> GameEngine.METER_COLOR_GREEN
		}

		// Settlement
		if(pid==1&&owner.engine[0].gameActive)
			if(owner.engine[0].stat==GameEngine.Status.GAMEOVER&&owner.engine[1].stat==GameEngine.Status.GAMEOVER) {
				// Draw
				winnerID = -1
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.musMan.bgm = BGM.Silent
			} else if(owner.engine[0].stat!=GameEngine.Status.GAMEOVER&&owner.engine[1].stat==GameEngine.Status.GAMEOVER) {
				// 1P win
				winnerID = 0
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.engine[0].stat = GameEngine.Status.EXCELLENT
				owner.engine[0].resetStatc()
				owner.engine[0].statc[1] = 1
				owner.musMan.bgm = BGM.Silent
			} else if(owner.engine[0].stat==GameEngine.Status.GAMEOVER&&owner.engine[1].stat!=GameEngine.Status.GAMEOVER) {
				// 2P win
				winnerID = 1
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.engine[1].stat = GameEngine.Status.EXCELLENT
				owner.engine[1].resetStatc()
				owner.engine[1].statc[1] = 1
				owner.musMan.bgm = BGM.Silent
			}
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 1, "RESULT", COLOR.ORANGE)
		val pid = engine.playerID
		when(winnerID) {
			-1 -> receiver.drawMenuFont(engine, 6, 2, "DRAW", COLOR.GREEN)
			pid -> receiver.drawMenuFont(engine, 6, 2, "WIN!", COLOR.YELLOW)
			else -> receiver.drawMenuFont(engine, 6, 2, "LOSE", COLOR.WHITE)
		}

		val apm = (ojamaSent[pid]*3600).toFloat()/engine.statistics.time.toFloat()
		drawResult(engine, receiver, 3, COLOR.ORANGE, "ATTACK", "%10d".format(ojamaSent[pid]))
		drawResultStats(engine, receiver, 5, COLOR.ORANGE, Statistic.LINES, Statistic.PIECE)
		drawResult(engine, receiver, 9, COLOR.ORANGE, "ATTACK/MIN", "%10g".format(apm))
		drawResultStats(
			engine,
			receiver,
			11,
			COLOR.ORANGE,
			Statistic.LPM,
			Statistic.PPS,
			Statistic.TIME
		)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveOtherSetting(engine, owner.replayProp)
		val pid = engine.playerID
		savePreset(engine, owner.replayProp, -1-pid)

		if(useMap[pid]) fldBackup[pid]?.let {saveMap(it, owner.replayProp, pid)}

		owner.replayProp.setProperty("spfvs.version", version)
		return false
	}

	companion object {
		/** Log (Apache log4j) */
		internal val log = LogManager.getLogger()

		/** Current version */
		private const val CURRENT_VERSION = 0

		/** Enabled piece types */
		private val PIECE_ENABLE = listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0)

		/** Block colors */
		private val BLOCK_COLORS = listOf(Block.COLOR.RED, Block.COLOR.GREEN, Block.COLOR.BLUE, Block.COLOR.YELLOW)
		/*arrayOf(Block.COLOR.RED, Block.COLOR.RED, Block.COLOR.RED, Block.COLOR.RED, Block.COLOR.GEM_RED,
			Block.COLOR.GREEN, Block.COLOR.GREEN, Block.COLOR.GREEN, Block.COLOR.GREEN, Block.COLOR.GEM_GREEN,
			Block.COLOR.BLUE, Block.COLOR.BLUE, Block.COLOR.BLUE, Block.COLOR.BLUE, Block.COLOR.GEM_BLUE,
			Block.COLOR.YELLOW, Block.COLOR.YELLOW, Block.COLOR.YELLOW, Block.COLOR.YELLOW, Block.COLOR.GEM_YELLOW)*/

		private val ROW_VALUES = doubleArrayOf(2.3, 2.2, 2.1, 2.0, 1.9, 1.8, 1.7, 1.6, 1.5, 1.4, 1.3, 1.2, 1.1, 1.0)

		/** Names of drop values sets */
		private val DROP_SET_NAMES = listOf("CLASSIC", "REMIX", "SWORD", "S-MIRROR", "AVALANCHE", "A-MIRROR")

		private val DROP_PATTERNS =
			listOf(
				listOf(
					listOf(listOf(2, 2, 2, 2), listOf(5, 5, 5, 5), listOf(7, 7, 7, 7), listOf(4, 4, 4, 4)),
					listOf(
						listOf(2, 2, 4, 4), listOf(2, 2, 4, 4), listOf(5, 5, 2, 2), listOf(5, 5, 2, 2),
						listOf(7, 7, 5, 5), listOf(7, 7, 5, 5)
					),
					listOf(
						listOf(5, 5, 5, 5), listOf(2, 7, 2, 7), listOf(2, 7, 2, 7), listOf(2, 7, 2, 7),
						listOf(2, 7, 2, 7), listOf(4, 4, 4, 4)
					), listOf(listOf(2, 5, 7, 4)),
					listOf(
						listOf(7, 7, 4, 4), listOf(4, 4, 7, 7), listOf(2, 2, 5, 5), listOf(2, 2, 5, 5),
						listOf(4, 4, 7, 7), listOf(7, 7, 4, 4)
					),
					listOf(
						listOf(4, 7, 7, 5), listOf(7, 7, 5, 5), listOf(7, 5, 5, 2), listOf(5, 5, 2, 2),
						listOf(5, 2, 2, 4), listOf(2, 2, 4, 4)
					),
					listOf(
						listOf(2, 2, 5, 5), listOf(4, 4, 5, 5), listOf(2, 2, 5, 5), listOf(4, 4, 7, 7),
						listOf(2, 2, 7, 7), listOf(4, 4, 7, 7)
					),
					listOf(
						listOf(5, 5, 5, 5), listOf(2, 2, 7, 7), listOf(2, 2, 7, 7), listOf(7, 7, 2, 2),
						listOf(7, 7, 2, 2), listOf(4, 4, 4, 4)
					),
					listOf(listOf(5, 7, 4, 2), listOf(2, 5, 7, 4), listOf(4, 2, 5, 7), listOf(7, 4, 2, 5)),
					listOf(listOf(2, 5, 7, 4), listOf(5, 7, 4, 2), listOf(7, 4, 2, 5), listOf(4, 2, 5, 7)),
					listOf(listOf(2, 2, 2, 2))
				),
				listOf(
					listOf(listOf(2, 2, 7, 2), listOf(5, 5, 4, 5), listOf(7, 7, 5, 7), listOf(4, 4, 2, 4)),
					listOf(
						listOf(2, 2, 4, 4), listOf(2, 2, 4, 4), listOf(5, 5, 2, 2), listOf(5, 5, 2, 2),
						listOf(7, 7, 5, 5), listOf(7, 7, 5, 5)
					),
					listOf(
						listOf(5, 5, 4, 4), listOf(2, 7, 2, 7), listOf(2, 7, 2, 7), listOf(2, 7, 2, 7),
						listOf(2, 7, 2, 7), listOf(4, 4, 5, 5)
					), listOf(listOf(2, 5, 7, 4)),
					listOf(
						listOf(7, 7, 4, 4), listOf(4, 4, 7, 7), listOf(2, 5, 5, 5), listOf(2, 2, 2, 5),
						listOf(4, 4, 7, 7), listOf(7, 7, 4, 4)
					),
					listOf(
						listOf(7, 7, 7, 7), listOf(5, 7, 4, 2), listOf(7, 4, 2, 5), listOf(4, 2, 5, 7),
						listOf(2, 5, 7, 4), listOf(5, 5, 5, 5)
					),
					listOf(
						listOf(2, 2, 5, 5), listOf(4, 4, 5, 5), listOf(2, 2, 5, 5), listOf(4, 4, 7, 7),
						listOf(2, 2, 7, 7), listOf(4, 4, 7, 7)
					),
					listOf(
						listOf(5, 4, 5, 4), listOf(2, 2, 2, 7), listOf(2, 7, 7, 7), listOf(7, 2, 2, 2),
						listOf(7, 7, 7, 2), listOf(4, 5, 4, 5)
					),
					listOf(listOf(5, 7, 4, 2), listOf(2, 5, 7, 4), listOf(4, 2, 5, 7), listOf(7, 4, 2, 5)),
					listOf(listOf(2, 5, 7, 4), listOf(5, 7, 4, 2), listOf(7, 4, 2, 5), listOf(4, 2, 5, 7)),
					listOf(listOf(2, 2, 2, 2))
				), listOf(
					listOf(
						listOf(2, 5, 5, 5), listOf(5, 2, 2, 5), listOf(5, 5, 2, 2), listOf(4, 4, 7, 7),
						listOf(4, 7, 7, 4), listOf(7, 4, 4, 4)
					),
					listOf(
						listOf(2, 2, 2, 5, 5, 5), listOf(5, 3, 7, 5, 4, 5), listOf(5, 5, 7, 7, 4, 4),
						listOf(4, 4, 2, 4, 4, 7), listOf(4, 2, 4, 4, 7, 4), listOf(2, 4, 4, 7, 4, 4)
					),
					listOf(
						listOf(4, 4, 5, 5, 7, 2), listOf(4, 4, 5, 5, 7, 2), listOf(5, 5, 7, 7, 7, 5),
						listOf(5, 7, 7, 7, 4, 5), listOf(7, 7, 2, 2, 5, 4), listOf(7, 2, 2, 2, 5, 4)
					),
					listOf(
						listOf(2, 2, 5, 4, 2, 7), listOf(2, 7, 4, 5, 7, 2), listOf(2, 7, 4, 4, 7, 7),
						listOf(2, 7, 5, 5, 2, 2), listOf(2, 7, 5, 4, 2, 7), listOf(7, 7, 4, 5, 7, 2)
					),
					listOf(
						listOf(2, 7, 7, 7, 7), listOf(2, 7, 5, 7, 7), listOf(2, 2, 5, 5, 5), listOf(2, 2, 2, 5, 5),
						listOf(2, 4, 2, 4, 4), listOf(4, 4, 4, 4, 4)
					),
					listOf(
						listOf(2, 2, 5, 5), listOf(2, 7, 7, 5), listOf(5, 7, 4, 4), listOf(5, 5, 2, 4),
						listOf(4, 2, 2, 7), listOf(4, 4, 7, 7)
					),
					listOf(
						listOf(2, 2, 5, 5), listOf(2, 2, 5, 5), listOf(5, 5, 7, 7), listOf(5, 5, 7, 7),
						listOf(7, 7, 4, 4), listOf(7, 7, 4, 4)
					),
					listOf(
						listOf(2, 2, 5, 4, 2, 7), listOf(2, 2, 4, 5, 7, 2), listOf(7, 7, 4, 5, 7, 2),
						listOf(7, 7, 5, 4, 2, 7), listOf(2, 2, 5, 4, 2, 7), listOf(2, 2, 4, 5, 7, 2)
					),
					listOf(
						listOf(7, 7, 4, 4, 7, 7), listOf(7, 7, 7, 7, 5, 7), listOf(2, 5, 2, 2, 5, 2),
						listOf(2, 5, 2, 2, 5, 2), listOf(4, 4, 4, 4, 5, 4), listOf(4, 4, 7, 7, 4, 4)
					),
					listOf(
						listOf(2, 5, 5, 5, 5, 4), listOf(5, 2, 5, 5, 4, 4), listOf(2, 2, 2, 2, 2, 2),
						listOf(7, 7, 7, 7, 7, 7), listOf(4, 7, 4, 4, 5, 5), listOf(7, 4, 4, 4, 4, 5)
					),
					listOf(
						listOf(2, 2, 5, 2, 2, 4), listOf(2, 5, 5, 2, 5, 5), listOf(5, 5, 5, 7, 7, 2),
						listOf(7, 7, 7, 5, 5, 4), listOf(4, 7, 7, 4, 7, 7), listOf(4, 4, 7, 4, 4, 2)
					),
					listOf(
						listOf(7, 7, 5, 5, 5, 5), listOf(7, 2, 2, 5, 5, 7), listOf(7, 2, 2, 4, 4, 7),
						listOf(2, 7, 7, 4, 4, 2), listOf(2, 7, 7, 5, 5, 2), listOf(7, 7, 5, 5, 5, 5)
					),
					listOf(
						listOf(7, 7, 5, 5), listOf(7, 2, 5, 2), listOf(5, 5, 5, 2), listOf(4, 4, 4, 2),
						listOf(7, 2, 4, 2), listOf(7, 7, 4, 4)
					),
					listOf(
						listOf(2, 2, 5, 5), listOf(2, 7, 5, 5), listOf(5, 5, 7, 7), listOf(5, 5, 7, 7),
						listOf(4, 7, 4, 4), listOf(7, 7, 4, 4)
					),
					listOf(
						listOf(7, 7, 5, 5, 5), listOf(4, 7, 7, 7, 5), listOf(5, 4, 4, 4, 4), listOf(5, 2, 2, 2, 2),
						listOf(2, 7, 7, 7, 5), listOf(7, 7, 5, 5, 5)
					),
					listOf(
						listOf(2, 2, 4), listOf(2, 2, 2), listOf(7, 7, 7), listOf(7, 7, 7), listOf(5, 5, 5),
						listOf(5, 5, 4)
					),
					listOf(
						listOf(7, 7, 7, 7), listOf(7, 2, 2, 7), listOf(2, 7, 5, 4), listOf(4, 5, 7, 2),
						listOf(5, 4, 4, 5), listOf(5, 5, 5, 5)
					)
				), listOf(
					listOf(
						listOf(7, 4, 4, 4), listOf(4, 7, 7, 4), listOf(4, 4, 7, 7), listOf(5, 5, 2, 2),
						listOf(5, 2, 2, 5), listOf(2, 5, 5, 5)
					),
					listOf(
						listOf(2, 4, 4, 7, 4, 4), listOf(4, 2, 4, 4, 7, 4), listOf(4, 4, 2, 4, 4, 7),
						listOf(5, 5, 7, 7, 4, 4), listOf(5, 3, 7, 5, 4, 5), listOf(2, 2, 2, 5, 5, 5)
					),
					listOf(
						listOf(7, 2, 2, 2, 5, 4), listOf(7, 7, 2, 2, 5, 4), listOf(5, 7, 7, 7, 4, 5),
						listOf(5, 5, 7, 7, 7, 5), listOf(4, 4, 5, 5, 7, 2), listOf(4, 4, 5, 5, 7, 2)
					),
					listOf(
						listOf(7, 7, 4, 5, 7, 2), listOf(2, 7, 5, 4, 2, 7), listOf(2, 7, 5, 5, 2, 2),
						listOf(2, 7, 4, 4, 7, 7), listOf(2, 7, 4, 5, 7, 2), listOf(2, 2, 5, 4, 2, 7)
					),
					listOf(
						listOf(4, 4, 4, 4, 4), listOf(2, 4, 2, 4, 4), listOf(2, 2, 2, 5, 5), listOf(2, 2, 5, 5, 5),
						listOf(2, 7, 5, 7, 7), listOf(2, 7, 7, 7, 7)
					),
					listOf(
						listOf(4, 4, 7, 7), listOf(4, 2, 2, 7), listOf(5, 5, 2, 4), listOf(5, 7, 4, 4),
						listOf(2, 7, 7, 5), listOf(2, 2, 5, 5)
					),
					listOf(
						listOf(7, 7, 4, 4), listOf(7, 7, 4, 4), listOf(5, 5, 7, 7), listOf(5, 5, 7, 7),
						listOf(2, 2, 5, 5), listOf(2, 2, 5, 5)
					),
					listOf(
						listOf(2, 2, 4, 5, 7, 2), listOf(2, 2, 5, 4, 2, 7), listOf(7, 7, 5, 4, 2, 7),
						listOf(7, 7, 4, 5, 7, 2), listOf(2, 2, 4, 5, 7, 2), listOf(2, 2, 5, 4, 2, 7)
					),
					listOf(
						listOf(4, 4, 7, 7, 4, 4), listOf(4, 4, 4, 4, 5, 4), listOf(2, 5, 2, 2, 5, 2),
						listOf(2, 5, 2, 2, 5, 2), listOf(7, 7, 7, 7, 5, 7), listOf(7, 7, 4, 4, 7, 7)
					),
					listOf(
						listOf(7, 4, 4, 4, 4, 5), listOf(4, 7, 4, 4, 5, 5), listOf(7, 7, 7, 7, 7, 7),
						listOf(2, 2, 2, 2, 2, 2), listOf(5, 2, 5, 5, 4, 4), listOf(2, 5, 5, 5, 5, 4)
					),
					listOf(
						listOf(4, 4, 7, 4, 4, 2), listOf(4, 7, 7, 4, 7, 7), listOf(7, 7, 7, 5, 5, 4),
						listOf(5, 5, 5, 7, 7, 2), listOf(2, 5, 5, 2, 5, 5), listOf(2, 2, 5, 2, 2, 4)
					),
					listOf(
						listOf(7, 7, 5, 5, 5, 5), listOf(2, 7, 7, 5, 5, 2), listOf(2, 7, 7, 4, 4, 2),
						listOf(7, 2, 2, 4, 4, 7), listOf(7, 2, 2, 5, 5, 7), listOf(7, 7, 5, 5, 5, 5)
					),
					listOf(
						listOf(7, 7, 4, 4), listOf(7, 2, 4, 2), listOf(4, 4, 4, 2), listOf(5, 5, 5, 2),
						listOf(7, 2, 5, 2), listOf(7, 7, 5, 5)
					),
					listOf(
						listOf(7, 7, 4, 4), listOf(4, 7, 4, 4), listOf(5, 5, 7, 7), listOf(5, 5, 7, 7),
						listOf(2, 7, 5, 5), listOf(2, 2, 5, 5)
					),
					listOf(
						listOf(7, 7, 5, 5, 5), listOf(2, 7, 7, 7, 5), listOf(5, 2, 2, 2, 2), listOf(5, 4, 4, 4, 4),
						listOf(4, 7, 7, 7, 5), listOf(7, 7, 5, 5, 5)
					),
					listOf(
						listOf(5, 5, 4), listOf(5, 5, 5), listOf(7, 7, 7), listOf(7, 7, 7), listOf(2, 2, 2),
						listOf(2, 2, 4)
					),
					listOf(
						listOf(5, 5, 5, 5), listOf(5, 4, 4, 5), listOf(4, 5, 7, 2), listOf(2, 7, 5, 4),
						listOf(7, 2, 2, 7), listOf(7, 7, 7, 7)
					)
				), listOf(
					listOf(
						listOf(5, 4, 4, 5, 5), listOf(2, 5, 5, 2, 2), listOf(4, 2, 2, 4, 4), listOf(7, 4, 4, 7, 7),
						listOf(5, 7, 7, 5, 5), listOf(2, 5, 5, 2, 2)
					),
					listOf(
						listOf(2, 7, 7, 7, 2), listOf(5, 2, 2, 2, 5), listOf(5, 4, 4, 4, 5), listOf(4, 5, 5, 5, 4),
						listOf(4, 7, 7, 7, 4), listOf(7, 2, 2, 2, 7)
					),
					listOf(
						listOf(2, 2, 5, 5, 5), listOf(5, 7, 7, 2, 2), listOf(7, 7, 2, 2, 5), listOf(5, 4, 4, 7, 7),
						listOf(4, 4, 7, 7, 5), listOf(5, 5, 5, 4, 4)
					),
					listOf(
						listOf(7, 2, 2, 5, 5), listOf(4, 4, 5, 5, 2), listOf(4, 7, 7, 2, 2), listOf(7, 7, 4, 4, 5),
						listOf(5, 4, 4, 7, 7), listOf(2, 2, 7, 7, 4)
					),
					listOf(
						listOf(7, 2, 7, 2, 2), listOf(7, 4, 7, 7, 2), listOf(5, 4, 4, 7, 4), listOf(5, 5, 4, 5, 4),
						listOf(2, 5, 2, 5, 5), listOf(2, 7, 2, 2, 4)
					),
					listOf(
						listOf(5, 5, 4, 2, 2), listOf(5, 4, 4, 2, 7), listOf(4, 2, 2, 7, 7), listOf(4, 2, 7, 5, 5),
						listOf(2, 7, 7, 5, 4), listOf(7, 5, 5, 4, 4)
					),
					listOf(listOf(7, 7, 4, 7, 7), listOf(5, 5, 7, 5, 5), listOf(2, 2, 5, 2, 2), listOf(4, 4, 2, 4, 4)),
					listOf(listOf(4, 4, 2, 2, 5), listOf(2, 2, 5, 5, 7), listOf(5, 5, 7, 7, 4), listOf(7, 7, 4, 4, 2)),
					listOf(listOf(5, 5, 5, 2, 4), listOf(7, 7, 7, 5, 2), listOf(4, 4, 4, 7, 5), listOf(2, 2, 2, 4, 7)),
					listOf(listOf(4, 4, 4, 5, 7), listOf(2, 2, 2, 7, 4), listOf(5, 5, 5, 4, 2), listOf(7, 7, 7, 2, 5)),
					listOf(listOf(4, 2, 5, 5, 5), listOf(7, 4, 2, 2, 2), listOf(5, 7, 4, 4, 4), listOf(2, 5, 7, 7, 7))
				),
				listOf(
					listOf(
						listOf(2, 5, 5, 2, 2), listOf(5, 7, 7, 5, 5), listOf(7, 4, 4, 7, 7), listOf(4, 2, 2, 4, 4),
						listOf(2, 5, 5, 2, 2), listOf(5, 4, 4, 5, 5)
					),
					listOf(
						listOf(7, 2, 2, 2, 7), listOf(4, 7, 7, 7, 4), listOf(4, 5, 5, 5, 4), listOf(5, 4, 4, 4, 5),
						listOf(5, 2, 2, 2, 5), listOf(2, 7, 7, 7, 2)
					),
					listOf(
						listOf(5, 5, 5, 4, 4), listOf(4, 4, 7, 7, 5), listOf(5, 4, 4, 7, 7), listOf(7, 7, 2, 2, 5),
						listOf(5, 7, 7, 2, 2), listOf(2, 2, 5, 5, 5)
					),
					listOf(
						listOf(2, 2, 7, 7, 4), listOf(5, 4, 4, 7, 7), listOf(7, 7, 4, 4, 5), listOf(4, 7, 7, 2, 2),
						listOf(4, 4, 5, 5, 2), listOf(7, 2, 2, 5, 5)
					),
					listOf(
						listOf(2, 7, 2, 2, 4), listOf(2, 5, 2, 5, 5), listOf(5, 5, 4, 5, 4), listOf(5, 4, 4, 7, 4),
						listOf(7, 4, 7, 7, 2), listOf(7, 2, 7, 2, 2)
					),
					listOf(
						listOf(7, 5, 5, 4, 4), listOf(2, 7, 7, 5, 4), listOf(4, 2, 7, 5, 5), listOf(4, 2, 2, 7, 7),
						listOf(5, 4, 4, 2, 7), listOf(5, 5, 4, 2, 2)
					),
					listOf(listOf(5, 5, 7, 5, 5), listOf(7, 7, 4, 7, 7), listOf(4, 4, 2, 4, 4), listOf(2, 2, 5, 2, 2)),
					listOf(listOf(2, 2, 5, 5, 7), listOf(4, 4, 2, 2, 5), listOf(7, 7, 4, 4, 2), listOf(5, 5, 7, 7, 4)),
					listOf(listOf(7, 7, 7, 5, 2), listOf(5, 5, 5, 2, 4), listOf(2, 2, 2, 4, 7), listOf(4, 4, 4, 7, 5)),
					listOf(listOf(2, 2, 2, 7, 4), listOf(4, 4, 4, 5, 7), listOf(7, 7, 7, 2, 5), listOf(5, 5, 5, 4, 2)),
					listOf(listOf(7, 4, 2, 2, 2), listOf(4, 2, 5, 5, 5), listOf(2, 5, 7, 7, 7), listOf(5, 7, 4, 4, 4))
				)
			)
		private val DROP_PATTERNS_ATTACK_MULTIPLIERS =
			listOf(
				doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.7, 0.7, 1.0),
				doubleArrayOf(1.0, 1.2, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.85, 1.0)
			)
		private val DROP_PATTERNS_DEFEND_MULTIPLIERS =
			listOf(
				doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
				doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.2, 1.0, 1.0)
			)

		/** Names of rainbow power settings */
		private val RAINBOW_POWER_NAMES = listOf("NONE", "50%", "80%", "100%", "50/100%")

		/** Each player's frame cint */
		private val PLAYER_COLOR_FRAME = listOf(GameEngine.FRAME_COLOR_RED, GameEngine.FRAME_COLOR_BLUE)

		fun getAttackMultiplier(set:Int, map:Int):Double {
			return try {
				DROP_PATTERNS_ATTACK_MULTIPLIERS[set][map]
			} catch(e:ArrayIndexOutOfBoundsException) {
				1.0
			}
		}

		fun getDefendMultiplier(set:Int, map:Int):Double {
			return try {
				DROP_PATTERNS_DEFEND_MULTIPLIERS[set][map]
			} catch(e:ArrayIndexOutOfBoundsException) {
				1.0
			}
		}

		fun getRowValue(row:Int):Double = ROW_VALUES[row.coerceIn(0, ROW_VALUES.size-1)]
	}
}
