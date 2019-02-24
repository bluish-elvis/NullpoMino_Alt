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
import org.apache.log4j.Logger
import java.util.*

/** SPF VS-BATTLE mode (Beta) */
class SPF:AbstractMode() {

	/** Has accumulatedojama blockOfcount */
	private var ojama:IntArray = IntArray(0)

	/** Had sentojama blockOfcount */
	private var ojamaSent:IntArray = IntArray(0)

	/** Time to display the most recent increase in score */
	private var scgettime:IntArray = IntArray(0)

	/** UseBGM */
	private var bgmno:Int = 0

	/** Big */
	//private boolean[] big;

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

	/** Amount of points earned from most recent clear */
	private var lastscore:IntArray = IntArray(0)

	/** Score */
	private var score:IntArray = IntArray(0)

	/** Settings for starting countdown for ojama blocks */
	private var ojamaCountdown:IntArray = IntArray(0)

	/** True if use bigger field display */
	private var bigDisplay:Boolean = false

	/** HurryupSeconds before the startcount(0InHurryupNo) */
	private var hurryupSeconds:IntArray = IntArray(0)

	/** Time to display "ZENKESHI!" */
	private var zenKeshiDisplay:IntArray = IntArray(0)

	/** Time to display "TECH BONUS" */
	private var techBonusDisplay:IntArray = IntArray(0)

	/** Drop patterns */
	private var dropPattern:Array<Array<IntArray>> = emptyArray()

	/** Drop values set selected */
	private var dropSet:IntArray = IntArray(0)

	/** Drop values selected */
	private var dropMap:IntArray = IntArray(0)

	/** Drop multipliers */
	private var attackMultiplier:DoubleArray = DoubleArray(0)
	private var defendMultiplier:DoubleArray = DoubleArray(0)

	/** Rainbow power settings for each player */
	private var diamondPower:IntArray = IntArray(0)

	/** Frame when squares were last checked */
	private var lastSquareCheck:IntArray = IntArray(0)

	/** Flag set when counters have been decremented */
	private var countdownDecremented:BooleanArray = BooleanArray(0)

	/* Mode name */
	override val name:String get() = "SPF VS-BATTLE (BETA)"

	override val isVSMode:Boolean get() = true

	/* Number of players */
	override val players:Int get() = MAX_PLAYERS

	/* Game style */
	override val gameStyle:Int get() = GameEngine.GAMESTYLE_SPF

	/* Mode initialization */
	override fun modeInit(manager:GameManager) {
		owner = manager
		receiver = owner.receiver

		ojama = IntArray(MAX_PLAYERS)
		ojamaSent = IntArray(MAX_PLAYERS)

		scgettime = IntArray(MAX_PLAYERS)
		bgmno = 0
		//big = new boolean[MAX_PLAYERS];
		enableSE = BooleanArray(MAX_PLAYERS)
		hurryupSeconds = IntArray(MAX_PLAYERS)
		useMap = BooleanArray(MAX_PLAYERS)
		mapSet = IntArray(MAX_PLAYERS)
		mapNumber = IntArray(MAX_PLAYERS)
		presetNumber = IntArray(MAX_PLAYERS)
		propMap = arrayOfNulls(MAX_PLAYERS)
		mapMaxNo = IntArray(MAX_PLAYERS)
		fldBackup = arrayOfNulls(MAX_PLAYERS)
		randMap = Random()

		lastscore = IntArray(MAX_PLAYERS)
		score = IntArray(MAX_PLAYERS)
		ojamaCountdown = IntArray(MAX_PLAYERS)

		zenKeshiDisplay = IntArray(MAX_PLAYERS)
		techBonusDisplay = IntArray(MAX_PLAYERS)

		dropSet = IntArray(MAX_PLAYERS)
		dropMap = IntArray(MAX_PLAYERS)
		dropPattern = Array(MAX_PLAYERS) {emptyArray<IntArray>()}
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

	/** Load settings not related to speeds
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 */
	private fun loadOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val playerID = engine.playerID
		bgmno = prop.getProperty("spfvs.bgmno", 0)
		//big[playerID] = prop.getProperty("spfvs.big.p" + playerID, false);
		enableSE[playerID] = prop.getProperty("spfvs.enableSE.p$playerID", true)
		hurryupSeconds[playerID] = prop.getProperty("vsbattle.hurryupSeconds.p$playerID", 0)
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

	/** Save settings not related to speeds
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val playerID = engine.playerID
		prop.setProperty("spfvs.bgmno", bgmno)
		//prop.setProperty("spfvs.big.p" + playerID, big[playerID]);
		prop.setProperty("spfvs.enableSE.p$playerID", enableSE[playerID])
		prop.setProperty("vsbattle.hurryupSeconds.p$playerID", hurryupSeconds[playerID])
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
		if(propMap[playerID]==null||forceReload) {
			mapMaxNo[playerID] = 0
			propMap[playerID] = receiver.loadProperties("config/values/spf/"+mapSet[playerID]+".values")
		}
		propMap[playerID]?.let {
			mapMaxNo[playerID] = it.getProperty("values.maxMapNumber", 0)
			engine.createFieldIfNeeded()
			loadMap(engine.field!!, it, id)
			engine.field!!.setAllSkin(engine.skin)
		} ?: engine.field?.reset()
	}

	private fun loadDropMapPreview(engine:GameEngine, playerID:Int, pattern:Array<IntArray>?) {

		pattern?.let {
			log.debug("Loading drop values preview")
			engine.createFieldIfNeeded()
			engine.field!!.reset()
			var patternCol = 0
			val maxHeight = engine.field!!.height-1
			for(x in 0 until engine.field!!.width) {
				if(patternCol>=it.size) patternCol = 0
				for(patternRow in 0 until it[patternCol].size) {
					engine.field!!.setBlockColor(x, maxHeight-patternRow, it[patternCol][patternRow])
					val blk = engine.field!!.getBlock(x, maxHeight-patternRow)
					blk!!.setAttribute(true, Block.ATTRIBUTE.VISIBLE)
					blk.setAttribute(true, Block.ATTRIBUTE.OUTLINE)
				}
				patternCol++
			}
			engine.field!!.setAllSkin(engine.skin)
		} ?: engine.field?.reset()
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		if(playerID==1) {
			engine.randSeed = owner.engine[0].randSeed
			engine.random = Random(owner.engine[0].randSeed)
		}

		engine.framecolor = PLAYER_COLOR_FRAME[playerID]
		engine.clearMode = GameEngine.ClearType.GEM_COLOR
		engine.garbageColorClear = true
		engine.lineGravityType = GameEngine.LineGravity.CASCADE
		for(i in 0 until Piece.PIECE_COUNT)
			engine.nextPieceEnable[i] = PIECE_ENABLE[i]==1
		engine.blockColors = BLOCK_COLORS
		engine.randomBlockColor = true
		engine.connectBlocks = false

		ojama[playerID] = 0
		ojamaSent[playerID] = 0
		score[playerID] = 0
		scgettime[playerID] = 0
		zenKeshiDisplay[playerID] = 0
		techBonusDisplay[playerID] = 0
		lastSquareCheck[playerID] = -1
		countdownDecremented[playerID] = true

		version = if(!engine.owner.replayMode) {
			loadOtherSetting(engine, engine.owner.modeConfig)
			loadPreset(engine, engine.owner.modeConfig, -1-playerID)
			CURRENT_VERSION
		} else {
			loadOtherSetting(engine, engine.owner.replayProp)
			loadPreset(engine, engine.owner.replayProp, -1-playerID)
			owner.replayProp.getProperty("spfvs.version", 0)
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode&&engine.statc[4]==0) {
			// Up
			if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_UP)) {
				menuCursor--
				if(menuCursor<0) {
					menuCursor = 19
					loadDropMapPreview(engine, playerID, DROP_PATTERNS[dropSet[playerID]][dropMap[playerID]])
				} else if(menuCursor==17) engine.field = null
				engine.playSE("cursor")
			}
			// Down
			if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
				menuCursor++
				if(menuCursor>19) {
					menuCursor = 0
					engine.field = null
				} else if(menuCursor==18) loadDropMapPreview(engine, playerID, DROP_PATTERNS[dropSet[playerID]][dropMap[playerID]])
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
						bgmno += change
						if(bgmno<0) bgmno = BGM.count-1
						if(bgmno>=BGM.count) bgmno = 0
					}
					10 -> {
						useMap[playerID] = !useMap[playerID]
						if(!useMap[playerID]) {
							if(engine.field!=null) engine.field!!.reset()
						} else
							loadMapPreview(engine, playerID, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
					}
					11 -> {
						mapSet[playerID] += change
						if(mapSet[playerID]<0) mapSet[playerID] = 99
						if(mapSet[playerID]>99) mapSet[playerID] = 0
						if(useMap[playerID]) {
							mapNumber[playerID] = -1
							loadMapPreview(engine, playerID, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
						}
					}
					12 -> if(useMap[playerID]) {
						mapNumber[playerID] += change
						if(mapNumber[playerID]<-1) mapNumber[playerID] = mapMaxNo[playerID]-1
						if(mapNumber[playerID]>mapMaxNo[playerID]-1) mapNumber[playerID] = -1
						loadMapPreview(engine, playerID, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
					} else
						mapNumber[playerID] = -1
					13 -> enableSE[playerID] = !enableSE[playerID]
					14 -> {
						if(m>10)
							hurryupSeconds[playerID] += change*m/10
						else
							hurryupSeconds[playerID] += change
						if(hurryupSeconds[playerID]<0) hurryupSeconds[playerID] = 300
						if(hurryupSeconds[playerID]>300) hurryupSeconds[playerID] = 0
					}
					15 -> {
						ojamaCountdown[playerID] += change
						if(ojamaCountdown[playerID]<1) ojamaCountdown[playerID] = 9
						if(ojamaCountdown[playerID]>9) ojamaCountdown[playerID] = 1
					}
					16 -> bigDisplay = !bigDisplay
					17 -> {
						diamondPower[playerID] += change
						if(diamondPower[playerID]<0) diamondPower[playerID] = 3
						if(diamondPower[playerID]>3) diamondPower[playerID] = 0
					}
					18 -> {
						dropSet[playerID] += change
						if(dropSet[playerID]<0) dropSet[playerID] = DROP_PATTERNS.size-1
						if(dropSet[playerID]>=DROP_PATTERNS.size) dropSet[playerID] = 0
						if(dropMap[playerID]>=DROP_PATTERNS[dropSet[playerID]].size) dropMap[playerID] = 0
						loadDropMapPreview(engine, playerID, DROP_PATTERNS[dropSet[playerID]][dropMap[playerID]])
					}
					19 -> {
						dropMap[playerID] += change
						if(dropMap[playerID]<0) dropMap[playerID] = DROP_PATTERNS[dropSet[playerID]].size-1
						if(dropMap[playerID]>=DROP_PATTERNS[dropSet[playerID]].size) dropMap[playerID] = 0
						loadDropMapPreview(engine, playerID, DROP_PATTERNS[dropSet[playerID]][dropMap[playerID]])
					}
				}/* case 20:
					 * big[playerID] = !big[playerID];
					 * break; */
			}

			// 決定
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				if(menuCursor==7)
					loadPreset(engine, owner.modeConfig, presetNumber[playerID])
				else if(menuCursor==8) {
					savePreset(engine, owner.modeConfig, presetNumber[playerID])
					receiver.saveModeConfig(owner.modeConfig)
				} else {
					saveOtherSetting(engine, owner.modeConfig)
					savePreset(engine, owner.modeConfig, -1-playerID)
					receiver.saveModeConfig(owner.modeConfig)
					engine.statc[4] = 1
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

			if(menuTime>=180)
				engine.statc[4] = 1
			else if(menuTime>120)
				menuCursor = 18
			else if(menuTime==120) {
				menuCursor = 18
				loadDropMapPreview(engine, playerID, DROP_PATTERNS[dropSet[playerID]][dropMap[playerID]])
			} else if(menuTime>=60) menuCursor = 9
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
				drawMenu(engine, playerID, receiver, "LOAD", presetNumber[playerID].toString(), "SAVE", presetNumber[playerID].toString())
				receiver.drawMenuFont(engine, playerID, 0, 19, "PAGE 1/3", COLOR.YELLOW)
			} else if(menuCursor<18) {
				initMenu(COLOR.PINK, 9)
				drawMenu(engine, playerID, receiver, "BGM", BGM.values[bgmno].toString())
				menuColor = COLOR.CYAN
				drawMenu(engine, playerID, receiver, "USE MAP", GeneralUtil.getONorOFF(useMap[playerID]), "MAP SET", mapSet[playerID].toString(), "MAP NO.",
					if(mapNumber[playerID]<0)
						"RANDOM"
					else
						mapNumber[playerID].toString()+"/"+(mapMaxNo[playerID]-1), "SE", GeneralUtil.getONorOFF(enableSE[playerID]), "HURRYUP", if(hurryupSeconds[playerID]==0)
					"NONE"
				else
					hurryupSeconds[playerID].toString()+"SEC", "COUNTDOWN", ojamaCountdown[playerID].toString())
				menuColor = COLOR.PINK
				drawMenu(engine, playerID, receiver, "BIG DISP", GeneralUtil.getONorOFF(bigDisplay))
				menuColor = COLOR.CYAN
				drawMenu(engine, playerID, receiver, "RAINBOW")
				drawMenu(engine, playerID, receiver, "GEM POWER", RAINBOW_POWER_NAMES[diamondPower[playerID]])

				receiver.drawMenuFont(engine, playerID, 0, 19, "PAGE 2/3", COLOR.YELLOW)
			} else {
				receiver.drawMenuFont(engine, playerID, 0, 0, "ATTACK", COLOR.CYAN)
				var multiplier = (100*getAttackMultiplier(dropSet[playerID], dropMap[playerID])).toInt()
				if(multiplier>=100)
					receiver.drawMenuFont(engine, playerID, 2, 1, multiplier.toString()+"%", if(multiplier==100)
						COLOR.YELLOW
					else
						COLOR.GREEN)
				else
					receiver.drawMenuFont(engine, playerID, 3, 1, multiplier.toString()+"%", COLOR.RED)
				receiver.drawMenuFont(engine, playerID, 0, 2, "DEFEND", COLOR.CYAN)
				multiplier = (100*getDefendMultiplier(dropSet[playerID], dropMap[playerID])).toInt()
				if(multiplier>=100)
					receiver.drawMenuFont(engine, playerID, 2, 3, multiplier.toString()+"%", if(multiplier==100)
						COLOR.YELLOW
					else
						COLOR.RED)
				else
					receiver.drawMenuFont(engine, playerID, 3, 3, multiplier.toString()+"%", COLOR.GREEN)

				drawMenu(engine, playerID, receiver, 14, COLOR.CYAN, 18, "DROP SET", DROP_SET_NAMES[dropSet[playerID]], "DROP MAP",
					String.format("%2d", dropMap[playerID]+1)+"/"+
						String.format("%2d", DROP_PATTERNS[dropSet[playerID]].size))

				receiver.drawMenuFont(engine, playerID, 0, 19, "PAGE 3/3", COLOR.YELLOW)
			}
		} else
			receiver.drawMenuFont(engine, playerID, 3, 10, "WAIT", COLOR.YELLOW)
	}

	/* Called for initialization during Ready (before initialization) */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) {
			engine.numColors = BLOCK_COLORS.size
			engine.rainbowAnimate = playerID==0
			engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_CONNECT
			engine.displaysize = if(bigDisplay) 1 else 0

			dropPattern[playerID] = DROP_PATTERNS[dropSet[playerID]][dropMap[playerID]]
			attackMultiplier[playerID] = getAttackMultiplier(dropSet[playerID], dropMap[playerID])
			defendMultiplier[playerID] = getDefendMultiplier(dropSet[playerID], dropMap[playerID])

			// MapFor storing backup Replay read
			if(useMap[playerID]) {
				if(owner.replayMode) {
					engine.createFieldIfNeeded()
					loadMap(engine.field!!, owner.replayProp, playerID)
					engine.field!!.setAllSkin(engine.skin)
				} else {
					if(propMap[playerID]==null)
						propMap[playerID] = receiver.loadProperties("config/values/spf/"
							+mapSet[playerID]+".values")
					else propMap[playerID]?.let {
						engine.createFieldIfNeeded()

						if(mapNumber[playerID]<0) {
							if(playerID==1&&useMap[0]&&mapNumber[0]<0)
								engine.field!!.copy(owner.engine[0].field)
							else {
								val no = if(mapMaxNo[playerID]<1) 0 else randMap!!.nextInt(mapMaxNo[playerID])
								loadMap(engine.field!!, it, no)
							}
						} else
							loadMap(engine.field!!, it, mapNumber[playerID])

						engine.field!!.setAllSkin(engine.skin)
						fldBackup[playerID] = Field(engine.field)
					}
				}
			} else if(engine.field!=null) engine.field!!.reset()
		} else if(engine.statc[0]==1&&diamondPower[playerID]>0) {
			var x = 24
			while(x<engine.nextPieceArraySize) {
				engine.nextPieceArrayObject[x]!!.block[1].cint = DIAMOND_COLOR
				x += 25
			}
		}
		return false
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.b2bEnable = false
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		//engine.big = big[playerID];
		engine.enableSE = enableSE[playerID]
		if(playerID==1) owner.bgmStatus.bgm = BGM.values[bgmno]
		//engine.colorClearSize = big[playerID] ? 8 : 2;
		engine.colorClearSize = 2
		engine.ignoreHidden = false

		engine.tspinAllowKick = false
		engine.tspinEnable = false
		engine.useAllSpinBonus = false
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		val fldPosX = receiver.getFieldDisplayPositionX(engine, playerID)
		val fldPosY = receiver.getFieldDisplayPositionY(engine, playerID)
		val playerColor = if(playerID==0) COLOR.RED else COLOR.BLUE

		// Timer
		if(playerID==0) receiver.drawDirectFont(224, 0, GeneralUtil.getTime(engine.statistics.time.toFloat()))

		// Ojama Counter
		val fontColor = when {
			ojama[playerID]>=1 -> COLOR.YELLOW
			ojama[playerID]>=6 -> COLOR.ORANGE
			ojama[playerID]>=12 -> COLOR.RED
			else -> COLOR.WHITE
		}
		val strOjama = ojama[playerID].toString()
		if(strOjama!="0") receiver.drawDirectFont(fldPosX+4, fldPosY+32, strOjama, fontColor)

		// Score
		if(engine.displaysize==1)
			receiver.drawDirectFont(fldPosX+4, fldPosY+472, String.format("%12d", score[playerID]), playerColor)
		else if(engine.gameStarted)
			receiver.drawDirectFont(fldPosX-28, fldPosY+264, String.format("%8d", score[playerID]), playerColor)

		// Countdown Blocks
		var b:Block?
		var blockColor:Int

		if(engine.field!=null&&engine.gameActive)
			for(x in 0 until engine.field!!.width)
				for(y in 0 until engine.field!!.height) {
					b = engine.field!!.getBlock(x, y)
					if(!b!!.isEmpty&&b.countdown>0) {
						blockColor = b.secondaryColor

						val textColor = when(blockColor) {
							Block.BLOCK_COLOR_BLUE -> COLOR.BLUE
							Block.BLOCK_COLOR_GREEN -> COLOR.GREEN
							Block.BLOCK_COLOR_RED -> COLOR.RED
							Block.BLOCK_COLOR_YELLOW -> COLOR.YELLOW
							else -> COLOR.WHITE
						}
						if(engine.displaysize==1)
							receiver.drawMenuFont(engine, playerID, x*2,
								y*2, b.countdown.toString(), textColor, 2f)
						else
							receiver.drawMenuFont(engine, playerID, x, y, b.countdown.toString(), textColor)
					}
				}

		// On-screen Texts
		var textHeight = 13
		if(engine.field!=null) {
			textHeight = engine.field!!.height
			textHeight += 3
		}
		if(engine.displaysize==1) textHeight = 11
		val baseX = if(engine.displaysize==1) 1 else -2

		if(techBonusDisplay[playerID]>0)
			receiver.drawMenuFont(engine, playerID, baseX, textHeight, "TECH BONUS", COLOR.YELLOW)
		if(zenKeshiDisplay[playerID]>0)
			receiver.drawMenuFont(engine, playerID, baseX+1, textHeight+1, "ZENKESHI!", COLOR.YELLOW)
	}

	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		countdownDecremented[playerID] = false
		return false
	}

	override fun pieceLocked(engine:GameEngine, playerID:Int, avalanche:Int) {
		if(engine.field==null) return
		checkAll(engine, playerID)
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, avalanche:Int) {
		if(engine.field==null) return

		checkAll(engine, playerID)

		if(engine.field!!.canCascade()) return

		var enemyID = 0
		if(playerID==0) enemyID = 1

		val width = engine.field!!.width
		val height = engine.field!!.height
		val hiddenHeight = engine.field!!.hiddenHeight

		var diamondBreakColor = Block.BLOCK_COLOR_INVALID
		if(diamondPower[playerID]>0) {
			var y = -1*hiddenHeight
			while(y<height&&diamondBreakColor==Block.BLOCK_COLOR_INVALID) {
				{
					var x = 0
					while(x<width&&diamondBreakColor==Block.BLOCK_COLOR_INVALID) {
						if(engine.field!!.getBlockColor(x, y)==DIAMOND_COLOR) {
							if(engine.displaysize==1) {
								receiver.blockBreak(engine, playerID, 2*x, 2*y, engine.field!!.getBlock(x, y)!!)
								receiver.blockBreak(engine, playerID, 2*x+1, 2*y, engine.field!!.getBlock(x, y)!!)
								receiver.blockBreak(engine, playerID, 2*x, 2*y+1, engine.field!!.getBlock(x, y)!!)
								receiver.blockBreak(engine, playerID, 2*x+1, 2*y+1, engine.field!!.getBlock(x, y)!!)
							} else
								receiver.blockBreak(engine, playerID, x, y, engine.field!!.getBlock(x, y)!!)

							engine.field!!.setBlockColor(x, y, Block.BLOCK_COLOR_NONE)
							if(y+1>=height) {
								techBonusDisplay[playerID] = 120
								engine.statistics.score += 10000
								score[playerID] += 10000
							} else
								diamondBreakColor = engine.field!!.getBlockColor(x, y+1, true)
						}
						x++
					}
				}
				y++
			}
		}
		var pts = 0.0
		var add:Double
		var multiplier:Double
		var b:Block?
		//Clear blocks from diamond
		if(diamondBreakColor>Block.BLOCK_COLOR_NONE) {
			engine.field!!.allClearColor(diamondBreakColor, true, true)
			for(y in -1*hiddenHeight until height) {
				multiplier = getRowValue(y)
				for(x in 0 until width)
					if(engine.field!!.getBlockColor(x, y, true)==diamondBreakColor) {
						pts += multiplier*7
						if(engine.displaysize==1) {
							receiver.blockBreak(engine, playerID, 2*x, 2*y, engine.field!!.getBlock(x, y)!!)
							receiver.blockBreak(engine, playerID, 2*x+1, 2*y, engine.field!!.getBlock(x, y)!!)
							receiver.blockBreak(engine, playerID, 2*x, 2*y+1, engine.field!!.getBlock(x, y)!!)
							receiver.blockBreak(engine, playerID, 2*x+1, 2*y+1, engine.field!!.getBlock(x, y)!!)
						} else
							receiver.blockBreak(engine, playerID, x, y, engine.field!!.getBlock(x, y)!!)
						engine.field!!.setBlockColor(x, y, Block.BLOCK_COLOR_NONE)
					}
			}
		}
		if(diamondPower[playerID]==1)
			pts *= 0.5
		else if(diamondPower[playerID]==2) pts *= 0.8
		//TODO: Add diamond glitch
		//Clear blocks
		//engine.field.gemColorCheck(engine.colorClearSize, true, engine.garbageColorClear, engine.ignoreHidden);
		for(y in -1*hiddenHeight until height) {
			multiplier = getRowValue(y)
			for(x in 0 until width) {
				b = engine.field!!.getBlock(x, y)
				if(b==null) continue
				if(!b.getAttribute(Block.ATTRIBUTE.ERASE)||b.isEmpty) continue
				add = multiplier*7
				if(b.bonusValue>1) add *= b.bonusValue.toDouble()
				if(b.getAttribute(Block.ATTRIBUTE.GARBAGE)) {
					add /= 2.0
					b.secondaryColor = 0
				}
				if(engine.displaysize==1) {
					receiver.blockBreak(engine, playerID, 2*x, 2*y, b)
					receiver.blockBreak(engine, playerID, 2*x+1, 2*y, b)
					receiver.blockBreak(engine, playerID, 2*x, 2*y+1, b)
					receiver.blockBreak(engine, playerID, 2*x+1, 2*y+1, b)
				} else
					receiver.blockBreak(engine, playerID, x, y, b)
				engine.field!!.setBlockColor(x, y, Block.BLOCK_COLOR_NONE)
				pts += add
			}
		}
		if(engine.chain>1) pts += (engine.chain-1)*20.0

		if(engine.chain>=1) engine.playSE("combo"+minOf(engine.chain, 20))

		var ojamaNew = (pts*attackMultiplier[playerID]/7.0).toInt().toDouble()

		if(engine.field!!.isEmpty) {
			zenKeshiDisplay[playerID] = 120
			ojamaNew += 12.0
			engine.statistics.score += 1000
			score[playerID] += 1000
		}

		lastscore[playerID] = pts.toInt()*10
		scgettime[playerID] = 120
		score[playerID] += lastscore[playerID]

		if(hurryupSeconds[playerID]>0&&engine.statistics.time>hurryupSeconds[playerID])
			ojamaNew *= (1 shl engine.statistics.time/(hurryupSeconds[playerID]*60)).toDouble()

		if(ojama[playerID]>0&&ojamaNew>0.0) {
			val delta = minOf(ojama[playerID] shl 1, ojamaNew.toInt())
			ojama[playerID] -= delta shr 1
			ojamaNew -= delta.toDouble()
		}
		val ojamaSend = (ojamaNew*defendMultiplier[enemyID]).toInt()
		if(ojamaSend>0) ojama[enemyID] += ojamaSend
	}

	fun checkAll(engine:GameEngine, playerID:Int) {
		val recheck = checkCountdown(engine, playerID)
		if(recheck) log.debug("Converted garbage blocks to regular blocks. Rechecking squares.")
		checkSquares(engine, playerID, recheck)
	}

	fun checkCountdown(engine:GameEngine, playerID:Int):Boolean {
		if(countdownDecremented[playerID]) return false
		countdownDecremented[playerID] = true
		var result = false
		for(y in engine.field!!.hiddenHeight*-1 until engine.field!!.height)
			for(x in 0 until engine.field!!.width) {
				val b = engine.field!!.getBlock(x, y) ?: continue
				if(b.countdown>1)
					b.countdown--
				else if(b.countdown==1) {
					b.countdown = 0
					b.setAttribute(false, Block.ATTRIBUTE.GARBAGE)
					b.cint = b.secondaryColor
					result = true
				}
			}
		return result
	}

	fun checkSquares(engine:GameEngine, playerID:Int, forceRecheck:Boolean) {
		if(engine.field==null) return
		if(engine.statistics.time==lastSquareCheck[playerID]&&!forceRecheck) return
		lastSquareCheck[playerID] = engine.statistics.time

		//log.debug("Checking squares.");

		val width = engine.field!!.width
		val height = engine.field!!.height
		val hiddenHeight = engine.field!!.hiddenHeight

		var color:Int
		var b:Block?
		var minX:Int
		var minY:Int
		var maxX:Int
		var maxY:Int
		for(x in 0 until width)
			for(y in -1*hiddenHeight until height) {
				color = engine.field!!.getBlockColor(x, y)
				if(color<Block.BLOCK_COLOR_RED||color>Block.BLOCK_COLOR_PURPLE) continue
				minX = x
				minY = y
				maxX = x
				maxY = y
				var expanded = false
				b = engine.field!!.getBlock(x, y)
				if(!b!!.getAttribute(Block.ATTRIBUTE.BROKEN)&&
					b.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)&&
					b.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)&&
					!b.getAttribute(Block.ATTRIBUTE.CONNECT_UP)&&
					!b.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) {
					//Find boundaries of existing gem block
					maxX++
					maxY++
					var test:Block?
					while(maxX<width) {
						test = engine.field!!.getBlock(maxX, y)
						if(test==null) {
							maxX--
							break
						}
						if(test.cint!=color) {
							maxX--
							break
						}
						if(!test.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) break
						maxX++
					}
					while(maxY<height) {
						test = engine.field!!.getBlock(x, maxY)
						if(test==null) {
							maxY--
							break
						}
						if(test.cint!=color) {
							maxY--
							break
						}
						if(!test.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) break
						maxY++
					}
					log.debug("Pre-existing square found: ("+minX+", "+minY+") to ("+
						maxX+", "+maxY+")")
				} else if(b.getAttribute(Block.ATTRIBUTE.BROKEN)&&
					color==engine.field!!.getBlockColor(x+1, y)&&
					color==engine.field!!.getBlockColor(x, y+1)&&
					color==engine.field!!.getBlockColor(x+1, y+1)) {
					val bR = engine.field!!.getBlock(x+1, y)
					val bD = engine.field!!.getBlock(x, y+1)
					val bDR = engine.field!!.getBlock(x+1, y+1)
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
					log.debug("New square formed: ("+minX+", "+minY+") to ("+
						maxX+", "+maxY+")")
				}
				if(maxX<=minX||maxY<=minY) continue //No gem block, skip to next block
				var expandHere:Boolean
				var done:Boolean
				var testX:Int
				var testY:Int
				var bTest:Block?
				log.debug("Testing square for expansion. Coordinates before: ("+minX+", "+minY+") to ("+
					maxX+", "+maxY+")")
				//Expand up
				testY = minY-1
				done = false
				while(testY>=-1*hiddenHeight&&!done) {
					log.debug("Testing to expand up. testY = $testY")
					if(color!=engine.field!!.getBlockColor(minX, testY)||color!=engine.field!!.getBlockColor(maxX, testY))
						break
					if(engine.field!!.getBlock(minX, testY)!!.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)||engine.field!!.getBlock(maxX, testY)!!.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT))
						break
					expandHere = true
					testX = minX
					while(testX<=maxX&&!done) {
						if(engine.field!!.getBlockColor(testX, testY)!=color) {
							done = true
							expandHere = false
						} else if(engine.field!!.getBlock(testX, testY)!!.getAttribute(Block.ATTRIBUTE.CONNECT_UP))
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
					if(color!=engine.field!!.getBlockColor(testX, minY)||color!=engine.field!!.getBlockColor(testX, maxY))
						break
					if(engine.field!!.getBlock(testX, minY)!!.getAttribute(Block.ATTRIBUTE.CONNECT_UP)||engine.field!!.getBlock(testX, maxY)!!.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN))
						break
					expandHere = true
					testY = minY
					while(testY<=maxY&&!done) {
						if(engine.field!!.getBlockColor(testX, testY)!=color) {
							done = true
							expandHere = false
						} else if(engine.field!!.getBlock(testX, testY)!!.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT))
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
					if(color!=engine.field!!.getBlockColor(testX, minY)||color!=engine.field!!.getBlockColor(testX, maxY))
						break
					if(engine.field!!.getBlock(testX, minY)!!.getAttribute(Block.ATTRIBUTE.CONNECT_UP)||engine.field!!.getBlock(testX, maxY)!!.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN))
						break
					expandHere = true
					testY = minY
					while(testY<=maxY&&!done) {
						if(engine.field!!.getBlockColor(testX, testY)!=color) {
							done = true
							expandHere = false
						} else if(engine.field!!.getBlock(testX, testY)!!.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT))
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
					if(color!=engine.field!!.getBlockColor(minX, testY)||color!=engine.field!!.getBlockColor(maxX, testY))
						break
					if(engine.field!!.getBlock(minX, testY)!!.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)||engine.field!!.getBlock(maxX, testY)!!.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT))
						break
					expandHere = true
					testX = minX
					while(testX<=maxX&&!done) {
						if(engine.field!!.getBlockColor(testX, testY)!=color) {
							done = true
							expandHere = false
						} else if(engine.field!!.getBlock(testX, testY)!!.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN))
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

					log.debug("Expanding square. Coordinates after: ("+minX+", "+minY+") to ("+
						maxX+", "+maxY+")")
					val size = minOf(maxX-minX+1, maxY-minY+1)
					testX = minX
					while(testX<=maxX) {
						{
							testY = minY
							while(testY<=maxY) {
								bTest = engine.field!!.getBlock(testX, testY)
								bTest!!.setAttribute(false, Block.ATTRIBUTE.BROKEN)
								bTest!!.setAttribute(testX!=minX, Block.ATTRIBUTE.CONNECT_LEFT)
								bTest!!.setAttribute(testY!=maxY, Block.ATTRIBUTE.CONNECT_DOWN)
								bTest!!.setAttribute(testY!=minY, Block.ATTRIBUTE.CONNECT_UP)
								bTest!!.setAttribute(testX!=maxX, Block.ATTRIBUTE.CONNECT_RIGHT)
								bTest!!.bonusValue = size
								testY++
							}
						}
						testX++
					}
				}
			}
	}

	override fun lineClearEnd(engine:GameEngine, playerID:Int):Boolean {
		if(engine.field==null) return false

		val width = engine.field!!.width
		val height = engine.field!!.height
		val hiddenHeight = engine.field!!.hiddenHeight

		for(y in -1*hiddenHeight until height)
			for(x in 0 until width)
				if(engine.field!!.getBlockColor(x, y)==DIAMOND_COLOR) {
					calcScore(engine, playerID, 0)
					return true
				}

		checkAll(engine, playerID)

		//Drop garbage if needed.
		if(ojama[playerID]>0) {
			var enemyID = 0
			if(playerID==0) enemyID = 1

			val dropRows = minOf((ojama[playerID]+width-1)/width, engine.field!!.getHighestBlockY(3))
			if(dropRows<=0) return false
			val drop = minOf(ojama[playerID], width*dropRows)
			ojama[playerID] -= drop
			//engine.field.garbageDrop(engine, drop, big[playerID], ojamaHard[playerID], 3);
			engine.field!!.garbageDrop(engine, drop, false, 0, ojamaCountdown[playerID], 3)
			engine.field!!.setAllSkin(engine.skin)
			var patternCol = 0
			for(x in 0 until engine.field!!.width) {
				if(patternCol>=dropPattern[enemyID].size) patternCol = 0
				var patternRow = 0
				for(y in dropRows-hiddenHeight downTo -1*hiddenHeight) {
					val b = engine.field!!.getBlock(x, y)
					if(b!!.getAttribute(Block.ATTRIBUTE.GARBAGE)&&b.secondaryColor==0) {
						if(patternRow>=dropPattern[enemyID][patternCol].size) patternRow = 0
						b.secondaryColor = dropPattern[enemyID][patternCol][patternRow]
						patternRow++
					}
				}
				patternCol++
			}
			return true
		}
		return false
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		scgettime[playerID]++
		if(zenKeshiDisplay[playerID]>0) zenKeshiDisplay[playerID]--
		var width = 1
		if(engine.field!=null) width = engine.field!!.width
		val blockHeight = receiver.getBlockGraphicsHeight(engine)
		// Rising auctionMeter
		if(ojama[playerID]*blockHeight/width>engine.meterValue)
			engine.meterValue++
		else if(ojama[playerID]*blockHeight/width<engine.meterValue) engine.meterValue--
		if(ojama[playerID]>30)
			engine.meterColor = GameEngine.METER_COLOR_RED
		else if(ojama[playerID]>10)
			engine.meterColor = GameEngine.METER_COLOR_YELLOW
		else
			engine.meterColor = GameEngine.METER_COLOR_GREEN

		// Settlement
		if(playerID==1&&owner.engine[0].gameActive)
			if(owner.engine[0].stat==GameEngine.Status.GAMEOVER&&owner.engine[1].stat==GameEngine.Status.GAMEOVER) {
				// Draw
				winnerID = -1
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.bgmStatus.bgm = BGM.SILENT
			} else if(owner.engine[0].stat!=GameEngine.Status.GAMEOVER&&owner.engine[1].stat==GameEngine.Status.GAMEOVER) {
				// 1P win
				winnerID = 0
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.engine[0].stat = GameEngine.Status.EXCELLENT
				owner.engine[0].resetStatc()
				owner.engine[0].statc[1] = 1
				owner.bgmStatus.bgm = BGM.SILENT
			} else if(owner.engine[0].stat==GameEngine.Status.GAMEOVER&&owner.engine[1].stat!=GameEngine.Status.GAMEOVER) {
				// 2P win
				winnerID = 1
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.engine[1].stat = GameEngine.Status.EXCELLENT
				owner.engine[1].resetStatc()
				owner.engine[1].statc[1] = 1
				owner.bgmStatus.bgm = BGM.SILENT
			}
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 1, "RESULT", COLOR.ORANGE)
		if(winnerID==-1)
			receiver.drawMenuFont(engine, playerID, 6, 2, "DRAW", COLOR.GREEN)
		else if(winnerID==playerID)
			receiver.drawMenuFont(engine, playerID, 6, 2, "WIN!", COLOR.YELLOW)
		else
			receiver.drawMenuFont(engine, playerID, 6, 2, "LOSE", COLOR.WHITE)

		val apm = (ojamaSent[playerID]*3600).toFloat()/engine.statistics.time.toFloat()
		drawResult(engine, playerID, receiver, 3, COLOR.ORANGE, "ATTACK", String.format("%10d", ojamaSent[playerID]))
		drawResultStats(engine, playerID, receiver, 5, COLOR.ORANGE, AbstractMode.Statistic.LINES, AbstractMode.Statistic.PIECE)
		drawResult(engine, playerID, receiver, 9, COLOR.ORANGE, "ATTACK/MIN", String.format("%10g", apm))
		drawResultStats(engine, playerID, receiver, 11, COLOR.ORANGE, AbstractMode.Statistic.LPM, AbstractMode.Statistic.PPS, AbstractMode.Statistic.TIME)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveOtherSetting(engine, owner.replayProp)
		savePreset(engine, owner.replayProp, -1-playerID)

		if(useMap[playerID]) fldBackup[playerID]?.let {saveMap(it, owner.replayProp, playerID)}

		owner.replayProp.setProperty("spfvs.version", version)
	}

	companion object {
		/** Log (Apache log4j) */
		internal val log = Logger.getLogger(SPF::class.java)

		/** Current version */
		private const val CURRENT_VERSION = 0

		/** Enabled piece types */
		private val PIECE_ENABLE = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0)

		/** Block colors */
		private val BLOCK_COLORS =
			intArrayOf(Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_GEM_RED, Block.BLOCK_COLOR_GREEN, Block.BLOCK_COLOR_GREEN, Block.BLOCK_COLOR_GREEN, Block.BLOCK_COLOR_GREEN, Block.BLOCK_COLOR_GEM_GREEN, Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_GEM_BLUE, Block.BLOCK_COLOR_YELLOW, Block.BLOCK_COLOR_YELLOW, Block.BLOCK_COLOR_YELLOW, Block.BLOCK_COLOR_YELLOW, Block.BLOCK_COLOR_GEM_YELLOW)

		private val ROW_VALUES = doubleArrayOf(2.3, 2.2, 2.1, 2.0, 1.9, 1.8, 1.7, 1.6, 1.5, 1.4, 1.3, 1.2, 1.1, 1.0)

		private const val DIAMOND_COLOR = Block.BLOCK_COLOR_GEM_RAINBOW

		/** Number of players */
		private const val MAX_PLAYERS = 2

		/** Names of drop values sets */
		private val DROP_SET_NAMES = arrayOf("CLASSIC", "REMIX", "SWORD", "S-MIRROR", "AVALANCHE", "A-MIRROR")

		private val DROP_PATTERNS =
			arrayOf(arrayOf(arrayOf(intArrayOf(2, 2, 2, 2), intArrayOf(5, 5, 5, 5), intArrayOf(7, 7, 7, 7), intArrayOf(4, 4, 4, 4)), arrayOf(intArrayOf(2, 2, 4, 4), intArrayOf(2, 2, 4, 4), intArrayOf(5, 5, 2, 2), intArrayOf(5, 5, 2, 2), intArrayOf(7, 7, 5, 5), intArrayOf(7, 7, 5, 5)), arrayOf(intArrayOf(5, 5, 5, 5), intArrayOf(2, 7, 2, 7), intArrayOf(2, 7, 2, 7), intArrayOf(2, 7, 2, 7), intArrayOf(2, 7, 2, 7), intArrayOf(4, 4, 4, 4)), arrayOf(intArrayOf(2, 5, 7, 4)), arrayOf(intArrayOf(7, 7, 4, 4), intArrayOf(4, 4, 7, 7), intArrayOf(2, 2, 5, 5), intArrayOf(2, 2, 5, 5), intArrayOf(4, 4, 7, 7), intArrayOf(7, 7, 4, 4)), arrayOf(intArrayOf(4, 7, 7, 5), intArrayOf(7, 7, 5, 5), intArrayOf(7, 5, 5, 2), intArrayOf(5, 5, 2, 2), intArrayOf(5, 2, 2, 4), intArrayOf(2, 2, 4, 4)), arrayOf(intArrayOf(2, 2, 5, 5), intArrayOf(4, 4, 5, 5), intArrayOf(2, 2, 5, 5), intArrayOf(4, 4, 7, 7), intArrayOf(2, 2, 7, 7), intArrayOf(4, 4, 7, 7)), arrayOf(intArrayOf(5, 5, 5, 5), intArrayOf(2, 2, 7, 7), intArrayOf(2, 2, 7, 7), intArrayOf(7, 7, 2, 2), intArrayOf(7, 7, 2, 2), intArrayOf(4, 4, 4, 4)), arrayOf(intArrayOf(5, 7, 4, 2), intArrayOf(2, 5, 7, 4), intArrayOf(4, 2, 5, 7), intArrayOf(7, 4, 2, 5)), arrayOf(intArrayOf(2, 5, 7, 4), intArrayOf(5, 7, 4, 2), intArrayOf(7, 4, 2, 5), intArrayOf(4, 2, 5, 7)), arrayOf(intArrayOf(2, 2, 2, 2))), arrayOf(arrayOf(intArrayOf(2, 2, 7, 2), intArrayOf(5, 5, 4, 5), intArrayOf(7, 7, 5, 7), intArrayOf(4, 4, 2, 4)), arrayOf(intArrayOf(2, 2, 4, 4), intArrayOf(2, 2, 4, 4), intArrayOf(5, 5, 2, 2), intArrayOf(5, 5, 2, 2), intArrayOf(7, 7, 5, 5), intArrayOf(7, 7, 5, 5)), arrayOf(intArrayOf(5, 5, 4, 4), intArrayOf(2, 7, 2, 7), intArrayOf(2, 7, 2, 7), intArrayOf(2, 7, 2, 7), intArrayOf(2, 7, 2, 7), intArrayOf(4, 4, 5, 5)), arrayOf(intArrayOf(2, 5, 7, 4)), arrayOf(intArrayOf(7, 7, 4, 4), intArrayOf(4, 4, 7, 7), intArrayOf(2, 5, 5, 5), intArrayOf(2, 2, 2, 5), intArrayOf(4, 4, 7, 7), intArrayOf(7, 7, 4, 4)), arrayOf(intArrayOf(7, 7, 7, 7), intArrayOf(5, 7, 4, 2), intArrayOf(7, 4, 2, 5), intArrayOf(4, 2, 5, 7), intArrayOf(2, 5, 7, 4), intArrayOf(5, 5, 5, 5)), arrayOf(intArrayOf(2, 2, 5, 5), intArrayOf(4, 4, 5, 5), intArrayOf(2, 2, 5, 5), intArrayOf(4, 4, 7, 7), intArrayOf(2, 2, 7, 7), intArrayOf(4, 4, 7, 7)), arrayOf(intArrayOf(5, 4, 5, 4), intArrayOf(2, 2, 2, 7), intArrayOf(2, 7, 7, 7), intArrayOf(7, 2, 2, 2), intArrayOf(7, 7, 7, 2), intArrayOf(4, 5, 4, 5)), arrayOf(intArrayOf(5, 7, 4, 2), intArrayOf(2, 5, 7, 4), intArrayOf(4, 2, 5, 7), intArrayOf(7, 4, 2, 5)), arrayOf(intArrayOf(2, 5, 7, 4), intArrayOf(5, 7, 4, 2), intArrayOf(7, 4, 2, 5), intArrayOf(4, 2, 5, 7)), arrayOf(intArrayOf(2, 2, 2, 2))), arrayOf(arrayOf(intArrayOf(2, 5, 5, 5), intArrayOf(5, 2, 2, 5), intArrayOf(5, 5, 2, 2), intArrayOf(4, 4, 7, 7), intArrayOf(4, 7, 7, 4), intArrayOf(7, 4, 4, 4)), arrayOf(intArrayOf(2, 2, 2, 5, 5, 5), intArrayOf(5, 3, 7, 5, 4, 5), intArrayOf(5, 5, 7, 7, 4, 4), intArrayOf(4, 4, 2, 4, 4, 7), intArrayOf(4, 2, 4, 4, 7, 4), intArrayOf(2, 4, 4, 7, 4, 4)), arrayOf(intArrayOf(4, 4, 5, 5, 7, 2), intArrayOf(4, 4, 5, 5, 7, 2), intArrayOf(5, 5, 7, 7, 7, 5), intArrayOf(5, 7, 7, 7, 4, 5), intArrayOf(7, 7, 2, 2, 5, 4), intArrayOf(7, 2, 2, 2, 5, 4)), arrayOf(intArrayOf(2, 2, 5, 4, 2, 7), intArrayOf(2, 7, 4, 5, 7, 2), intArrayOf(2, 7, 4, 4, 7, 7), intArrayOf(2, 7, 5, 5, 2, 2), intArrayOf(2, 7, 5, 4, 2, 7), intArrayOf(7, 7, 4, 5, 7, 2)), arrayOf(intArrayOf(2, 7, 7, 7, 7), intArrayOf(2, 7, 5, 7, 7), intArrayOf(2, 2, 5, 5, 5), intArrayOf(2, 2, 2, 5, 5), intArrayOf(2, 4, 2, 4, 4), intArrayOf(4, 4, 4, 4, 4)), arrayOf(intArrayOf(2, 2, 5, 5), intArrayOf(2, 7, 7, 5), intArrayOf(5, 7, 4, 4), intArrayOf(5, 5, 2, 4), intArrayOf(4, 2, 2, 7), intArrayOf(4, 4, 7, 7)), arrayOf(intArrayOf(2, 2, 5, 5), intArrayOf(2, 2, 5, 5), intArrayOf(5, 5, 7, 7), intArrayOf(5, 5, 7, 7), intArrayOf(7, 7, 4, 4), intArrayOf(7, 7, 4, 4)), arrayOf(intArrayOf(2, 2, 5, 4, 2, 7), intArrayOf(2, 2, 4, 5, 7, 2), intArrayOf(7, 7, 4, 5, 7, 2), intArrayOf(7, 7, 5, 4, 2, 7), intArrayOf(2, 2, 5, 4, 2, 7), intArrayOf(2, 2, 4, 5, 7, 2)), arrayOf(intArrayOf(7, 7, 4, 4, 7, 7), intArrayOf(7, 7, 7, 7, 5, 7), intArrayOf(2, 5, 2, 2, 5, 2), intArrayOf(2, 5, 2, 2, 5, 2), intArrayOf(4, 4, 4, 4, 5, 4), intArrayOf(4, 4, 7, 7, 4, 4)), arrayOf(intArrayOf(2, 5, 5, 5, 5, 4), intArrayOf(5, 2, 5, 5, 4, 4), intArrayOf(2, 2, 2, 2, 2, 2), intArrayOf(7, 7, 7, 7, 7, 7), intArrayOf(4, 7, 4, 4, 5, 5), intArrayOf(7, 4, 4, 4, 4, 5)), arrayOf(intArrayOf(2, 2, 5, 2, 2, 4), intArrayOf(2, 5, 5, 2, 5, 5), intArrayOf(5, 5, 5, 7, 7, 2), intArrayOf(7, 7, 7, 5, 5, 4), intArrayOf(4, 7, 7, 4, 7, 7), intArrayOf(4, 4, 7, 4, 4, 2)), arrayOf(intArrayOf(7, 7, 5, 5, 5, 5), intArrayOf(7, 2, 2, 5, 5, 7), intArrayOf(7, 2, 2, 4, 4, 7), intArrayOf(2, 7, 7, 4, 4, 2), intArrayOf(2, 7, 7, 5, 5, 2), intArrayOf(7, 7, 5, 5, 5, 5)), arrayOf(intArrayOf(7, 7, 5, 5), intArrayOf(7, 2, 5, 2), intArrayOf(5, 5, 5, 2), intArrayOf(4, 4, 4, 2), intArrayOf(7, 2, 4, 2), intArrayOf(7, 7, 4, 4)), arrayOf(intArrayOf(2, 2, 5, 5), intArrayOf(2, 7, 5, 5), intArrayOf(5, 5, 7, 7), intArrayOf(5, 5, 7, 7), intArrayOf(4, 7, 4, 4), intArrayOf(7, 7, 4, 4)), arrayOf(intArrayOf(7, 7, 5, 5, 5), intArrayOf(4, 7, 7, 7, 5), intArrayOf(5, 4, 4, 4, 4), intArrayOf(5, 2, 2, 2, 2), intArrayOf(2, 7, 7, 7, 5), intArrayOf(7, 7, 5, 5, 5)), arrayOf(intArrayOf(2, 2, 4), intArrayOf(2, 2, 2), intArrayOf(7, 7, 7), intArrayOf(7, 7, 7), intArrayOf(5, 5, 5), intArrayOf(5, 5, 4)), arrayOf(intArrayOf(7, 7, 7, 7), intArrayOf(7, 2, 2, 7), intArrayOf(2, 7, 5, 4), intArrayOf(4, 5, 7, 2), intArrayOf(5, 4, 4, 5), intArrayOf(5, 5, 5, 5))), arrayOf(arrayOf(intArrayOf(7, 4, 4, 4), intArrayOf(4, 7, 7, 4), intArrayOf(4, 4, 7, 7), intArrayOf(5, 5, 2, 2), intArrayOf(5, 2, 2, 5), intArrayOf(2, 5, 5, 5)), arrayOf(intArrayOf(2, 4, 4, 7, 4, 4), intArrayOf(4, 2, 4, 4, 7, 4), intArrayOf(4, 4, 2, 4, 4, 7), intArrayOf(5, 5, 7, 7, 4, 4), intArrayOf(5, 3, 7, 5, 4, 5), intArrayOf(2, 2, 2, 5, 5, 5)), arrayOf(intArrayOf(7, 2, 2, 2, 5, 4), intArrayOf(7, 7, 2, 2, 5, 4), intArrayOf(5, 7, 7, 7, 4, 5), intArrayOf(5, 5, 7, 7, 7, 5), intArrayOf(4, 4, 5, 5, 7, 2), intArrayOf(4, 4, 5, 5, 7, 2)), arrayOf(intArrayOf(7, 7, 4, 5, 7, 2), intArrayOf(2, 7, 5, 4, 2, 7), intArrayOf(2, 7, 5, 5, 2, 2), intArrayOf(2, 7, 4, 4, 7, 7), intArrayOf(2, 7, 4, 5, 7, 2), intArrayOf(2, 2, 5, 4, 2, 7)), arrayOf(intArrayOf(4, 4, 4, 4, 4), intArrayOf(2, 4, 2, 4, 4), intArrayOf(2, 2, 2, 5, 5), intArrayOf(2, 2, 5, 5, 5), intArrayOf(2, 7, 5, 7, 7), intArrayOf(2, 7, 7, 7, 7)), arrayOf(intArrayOf(4, 4, 7, 7), intArrayOf(4, 2, 2, 7), intArrayOf(5, 5, 2, 4), intArrayOf(5, 7, 4, 4), intArrayOf(2, 7, 7, 5), intArrayOf(2, 2, 5, 5)), arrayOf(intArrayOf(7, 7, 4, 4), intArrayOf(7, 7, 4, 4), intArrayOf(5, 5, 7, 7), intArrayOf(5, 5, 7, 7), intArrayOf(2, 2, 5, 5), intArrayOf(2, 2, 5, 5)), arrayOf(intArrayOf(2, 2, 4, 5, 7, 2), intArrayOf(2, 2, 5, 4, 2, 7), intArrayOf(7, 7, 5, 4, 2, 7), intArrayOf(7, 7, 4, 5, 7, 2), intArrayOf(2, 2, 4, 5, 7, 2), intArrayOf(2, 2, 5, 4, 2, 7)), arrayOf(intArrayOf(4, 4, 7, 7, 4, 4), intArrayOf(4, 4, 4, 4, 5, 4), intArrayOf(2, 5, 2, 2, 5, 2), intArrayOf(2, 5, 2, 2, 5, 2), intArrayOf(7, 7, 7, 7, 5, 7), intArrayOf(7, 7, 4, 4, 7, 7)), arrayOf(intArrayOf(7, 4, 4, 4, 4, 5), intArrayOf(4, 7, 4, 4, 5, 5), intArrayOf(7, 7, 7, 7, 7, 7), intArrayOf(2, 2, 2, 2, 2, 2), intArrayOf(5, 2, 5, 5, 4, 4), intArrayOf(2, 5, 5, 5, 5, 4)), arrayOf(intArrayOf(4, 4, 7, 4, 4, 2), intArrayOf(4, 7, 7, 4, 7, 7), intArrayOf(7, 7, 7, 5, 5, 4), intArrayOf(5, 5, 5, 7, 7, 2), intArrayOf(2, 5, 5, 2, 5, 5), intArrayOf(2, 2, 5, 2, 2, 4)), arrayOf(intArrayOf(7, 7, 5, 5, 5, 5), intArrayOf(2, 7, 7, 5, 5, 2), intArrayOf(2, 7, 7, 4, 4, 2), intArrayOf(7, 2, 2, 4, 4, 7), intArrayOf(7, 2, 2, 5, 5, 7), intArrayOf(7, 7, 5, 5, 5, 5)), arrayOf(intArrayOf(7, 7, 4, 4), intArrayOf(7, 2, 4, 2), intArrayOf(4, 4, 4, 2), intArrayOf(5, 5, 5, 2), intArrayOf(7, 2, 5, 2), intArrayOf(7, 7, 5, 5)), arrayOf(intArrayOf(7, 7, 4, 4), intArrayOf(4, 7, 4, 4), intArrayOf(5, 5, 7, 7), intArrayOf(5, 5, 7, 7), intArrayOf(2, 7, 5, 5), intArrayOf(2, 2, 5, 5)), arrayOf(intArrayOf(7, 7, 5, 5, 5), intArrayOf(2, 7, 7, 7, 5), intArrayOf(5, 2, 2, 2, 2), intArrayOf(5, 4, 4, 4, 4), intArrayOf(4, 7, 7, 7, 5), intArrayOf(7, 7, 5, 5, 5)), arrayOf(intArrayOf(5, 5, 4), intArrayOf(5, 5, 5), intArrayOf(7, 7, 7), intArrayOf(7, 7, 7), intArrayOf(2, 2, 2), intArrayOf(2, 2, 4)), arrayOf(intArrayOf(5, 5, 5, 5), intArrayOf(5, 4, 4, 5), intArrayOf(4, 5, 7, 2), intArrayOf(2, 7, 5, 4), intArrayOf(7, 2, 2, 7), intArrayOf(7, 7, 7, 7))), arrayOf(arrayOf(intArrayOf(5, 4, 4, 5, 5), intArrayOf(2, 5, 5, 2, 2), intArrayOf(4, 2, 2, 4, 4), intArrayOf(7, 4, 4, 7, 7), intArrayOf(5, 7, 7, 5, 5), intArrayOf(2, 5, 5, 2, 2)), arrayOf(intArrayOf(2, 7, 7, 7, 2), intArrayOf(5, 2, 2, 2, 5), intArrayOf(5, 4, 4, 4, 5), intArrayOf(4, 5, 5, 5, 4), intArrayOf(4, 7, 7, 7, 4), intArrayOf(7, 2, 2, 2, 7)), arrayOf(intArrayOf(2, 2, 5, 5, 5), intArrayOf(5, 7, 7, 2, 2), intArrayOf(7, 7, 2, 2, 5), intArrayOf(5, 4, 4, 7, 7), intArrayOf(4, 4, 7, 7, 5), intArrayOf(5, 5, 5, 4, 4)), arrayOf(intArrayOf(7, 2, 2, 5, 5), intArrayOf(4, 4, 5, 5, 2), intArrayOf(4, 7, 7, 2, 2), intArrayOf(7, 7, 4, 4, 5), intArrayOf(5, 4, 4, 7, 7), intArrayOf(2, 2, 7, 7, 4)), arrayOf(intArrayOf(7, 2, 7, 2, 2), intArrayOf(7, 4, 7, 7, 2), intArrayOf(5, 4, 4, 7, 4), intArrayOf(5, 5, 4, 5, 4), intArrayOf(2, 5, 2, 5, 5), intArrayOf(2, 7, 2, 2, 4)), arrayOf(intArrayOf(5, 5, 4, 2, 2), intArrayOf(5, 4, 4, 2, 7), intArrayOf(4, 2, 2, 7, 7), intArrayOf(4, 2, 7, 5, 5), intArrayOf(2, 7, 7, 5, 4), intArrayOf(7, 5, 5, 4, 4)), arrayOf(intArrayOf(7, 7, 4, 7, 7), intArrayOf(5, 5, 7, 5, 5), intArrayOf(2, 2, 5, 2, 2), intArrayOf(4, 4, 2, 4, 4)), arrayOf(intArrayOf(4, 4, 2, 2, 5), intArrayOf(2, 2, 5, 5, 7), intArrayOf(5, 5, 7, 7, 4), intArrayOf(7, 7, 4, 4, 2)), arrayOf(intArrayOf(5, 5, 5, 2, 4), intArrayOf(7, 7, 7, 5, 2), intArrayOf(4, 4, 4, 7, 5), intArrayOf(2, 2, 2, 4, 7)), arrayOf(intArrayOf(4, 4, 4, 5, 7), intArrayOf(2, 2, 2, 7, 4), intArrayOf(5, 5, 5, 4, 2), intArrayOf(7, 7, 7, 2, 5)), arrayOf(intArrayOf(4, 2, 5, 5, 5), intArrayOf(7, 4, 2, 2, 2), intArrayOf(5, 7, 4, 4, 4), intArrayOf(2, 5, 7, 7, 7))), arrayOf(arrayOf(intArrayOf(2, 5, 5, 2, 2), intArrayOf(5, 7, 7, 5, 5), intArrayOf(7, 4, 4, 7, 7), intArrayOf(4, 2, 2, 4, 4), intArrayOf(2, 5, 5, 2, 2), intArrayOf(5, 4, 4, 5, 5)), arrayOf(intArrayOf(7, 2, 2, 2, 7), intArrayOf(4, 7, 7, 7, 4), intArrayOf(4, 5, 5, 5, 4), intArrayOf(5, 4, 4, 4, 5), intArrayOf(5, 2, 2, 2, 5), intArrayOf(2, 7, 7, 7, 2)), arrayOf(intArrayOf(5, 5, 5, 4, 4), intArrayOf(4, 4, 7, 7, 5), intArrayOf(5, 4, 4, 7, 7), intArrayOf(7, 7, 2, 2, 5), intArrayOf(5, 7, 7, 2, 2), intArrayOf(2, 2, 5, 5, 5)), arrayOf(intArrayOf(2, 2, 7, 7, 4), intArrayOf(5, 4, 4, 7, 7), intArrayOf(7, 7, 4, 4, 5), intArrayOf(4, 7, 7, 2, 2), intArrayOf(4, 4, 5, 5, 2), intArrayOf(7, 2, 2, 5, 5)), arrayOf(intArrayOf(2, 7, 2, 2, 4), intArrayOf(2, 5, 2, 5, 5), intArrayOf(5, 5, 4, 5, 4), intArrayOf(5, 4, 4, 7, 4), intArrayOf(7, 4, 7, 7, 2), intArrayOf(7, 2, 7, 2, 2)), arrayOf(intArrayOf(7, 5, 5, 4, 4), intArrayOf(2, 7, 7, 5, 4), intArrayOf(4, 2, 7, 5, 5), intArrayOf(4, 2, 2, 7, 7), intArrayOf(5, 4, 4, 2, 7), intArrayOf(5, 5, 4, 2, 2)), arrayOf(intArrayOf(5, 5, 7, 5, 5), intArrayOf(7, 7, 4, 7, 7), intArrayOf(4, 4, 2, 4, 4), intArrayOf(2, 2, 5, 2, 2)), arrayOf(intArrayOf(2, 2, 5, 5, 7), intArrayOf(4, 4, 2, 2, 5), intArrayOf(7, 7, 4, 4, 2), intArrayOf(5, 5, 7, 7, 4)), arrayOf(intArrayOf(7, 7, 7, 5, 2), intArrayOf(5, 5, 5, 2, 4), intArrayOf(2, 2, 2, 4, 7), intArrayOf(4, 4, 4, 7, 5)), arrayOf(intArrayOf(2, 2, 2, 7, 4), intArrayOf(4, 4, 4, 5, 7), intArrayOf(7, 7, 7, 2, 5), intArrayOf(5, 5, 5, 4, 2)), arrayOf(intArrayOf(7, 4, 2, 2, 2), intArrayOf(4, 2, 5, 5, 5), intArrayOf(2, 5, 7, 7, 7), intArrayOf(5, 7, 4, 4, 4))))
		private val DROP_PATTERNS_ATTACK_MULTIPLIERS =
			arrayOf(doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.7, 0.7, 1.0), doubleArrayOf(1.0, 1.2, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.85, 1.0))
		private val DROP_PATTERNS_DEFEND_MULTIPLIERS =
			arrayOf(doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0), doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.2, 1.0, 1.0))

		/** Names of rainbow power settings */
		private val RAINBOW_POWER_NAMES = arrayOf("NONE", "50%", "80%", "100%", "50/100%")

		/** Each player's frame cint */
		private val PLAYER_COLOR_FRAME = intArrayOf(GameEngine.FRAME_COLOR_RED, GameEngine.FRAME_COLOR_BLUE)

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

		fun getRowValue(row:Int):Double = ROW_VALUES[minOf(maxOf(row, 0), ROW_VALUES.size-1)]
	}
}
