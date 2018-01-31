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
import mu.nu.nullpo.game.component.Piece.Shape
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import java.util.*

/** VS-BATTLE Mode */
class VSBattleMode:AbstractMode() {

	/** garbage blockType of */
	private var garbageType:IntArray = IntArray(0)

	/** Rate of change of garbage holes */
	private var garbagePercent:IntArray = IntArray(0)

	/** Allow garbage countering */
	private var garbageCounter:BooleanArray = BooleanArray(0)

	/** Allow garbage blocking */
	private var garbageBlocking:BooleanArray = BooleanArray(0)

	/** Has accumulatedgarbage blockOfcount */
	private var garbage:IntArray = IntArray(0)

	/** Had sentgarbage blockOfcount */
	private var garbageSent:IntArray = IntArray(0)

	/** Last garbage hole position */
	private var lastHole:IntArray = IntArray(0)

	/** Time to display the most recent increase in score */
	private var scgettime:IntArray = IntArray(0)

	/** Most recent scoring event type */
	private var lastevent:IntArray = IntArray(0)

	/** Most recent scoring eventInB2BIf it&#39;s the casetrue */
	private var lastb2b:BooleanArray = BooleanArray(0)

	/** Most recent scoring eventInCombocount */
	private var lastcombo:IntArray = IntArray(0)

	/** Most recent scoring eventPeace inID */
	private var lastpiece:IntArray = IntArray(0)

	/** UseBGM */
	private var bgmno:Int = 0

	/** Flag for types of T-Spins allowed (0=none, 1=normal, 2=all spin) */
	private var tspinEnableType:IntArray = IntArray(0)

	/** Old flag for allowing T-Spins */
	private var enableTSpin:BooleanArray = BooleanArray(0)

	/** Flag for enabling wallkick T-Spins */
	private var enableTSpinKick:BooleanArray = BooleanArray(0)

	/** Spin check type (4Point or Immobile) */
	private var spinCheckType:IntArray = IntArray(0)

	/** Immobile EZ spin */
	private var tspinEnableEZ:BooleanArray = BooleanArray(0)

	/** B2B Type (0=OFF 1=ON 2=ON+Separated-garbage) */
	private var b2bType:IntArray = IntArray(0)

	/** Flag for enabling combos */
	private var enableCombo:BooleanArray = BooleanArray(0)

	/** Big */
	private var big:BooleanArray = BooleanArray(0)

	/** Sound effectsON/OFF */
	private var enableSE:BooleanArray = BooleanArray(0)

	/** HurryupSeconds before the startcount(-1InHurryupNo) */
	private var hurryupSeconds:IntArray = IntArray(0)

	/** HurryupTimes afterBlockDo you run up the floor every time you put the */
	private var hurryupInterval:IntArray = IntArray(0)

	/** MapUse flag */
	private var useMap:BooleanArray = BooleanArray(0)

	/** UseMapSet number */
	private var mapSet:IntArray = IntArray(0)

	/** Map number(-1Random in) */
	private var mapNumber:IntArray = IntArray(0)

	/** Last preset number used */
	private var presetNumber:IntArray = IntArray(0)

	/** True if display detailed stats */
	private var showStats:Boolean = false

	/** Winner */
	private var winnerID:Int = 0

	/** I was sent from the enemygarbage blockA list of */
	private var garbageEntries:Array<LinkedList<GarbageEntry>> =  emptyArray()

	/** HurryupAfterBlockI put count */
	private var hurryupCount:IntArray = IntArray(0)

	/** MapSets ofProperty file */
	private var propMap:Array<CustomProperties?> = emptyArray()

	/** MaximumMap number */
	private var mapMaxNo:IntArray = IntArray(0)

	/** For backupfield (MapUsed to save the replay) */
	private var fldBackup:Array<Field?> =  emptyArray()

	/** MapRan for selectioncount */
	private var randMap:Random? = null

	/** Win count for each player */
	private var winCount:IntArray = IntArray(0)

	/** Version */
	private var version:Int = 0

	/* Mode name */
	override val name:String
		get() = "VS-BATTLE"

	override val isVSMode:Boolean
		get() = true

	/* Number of players */
	override val players:Int
		get() = MAX_PLAYERS

	/* Mode initialization */
	override fun modeInit(manager:GameManager) {
		owner = manager
		receiver = owner.receiver

		garbageType = IntArray(MAX_PLAYERS)
		garbagePercent = IntArray(MAX_PLAYERS)
		garbageCounter = BooleanArray(MAX_PLAYERS)
		garbageBlocking = BooleanArray(MAX_PLAYERS)
		garbage = IntArray(MAX_PLAYERS)
		garbageSent = IntArray(MAX_PLAYERS)
		lastHole = IntArray(MAX_PLAYERS)
		scgettime = IntArray(MAX_PLAYERS)
		lastevent = IntArray(MAX_PLAYERS)
		lastb2b = BooleanArray(MAX_PLAYERS)
		lastcombo = IntArray(MAX_PLAYERS)
		lastpiece = IntArray(MAX_PLAYERS)
		bgmno = 0
		tspinEnableType = IntArray(MAX_PLAYERS)
		enableTSpin = BooleanArray(MAX_PLAYERS)
		enableTSpinKick = BooleanArray(MAX_PLAYERS)
		spinCheckType = IntArray(MAX_PLAYERS)
		tspinEnableEZ = BooleanArray(MAX_PLAYERS)
		b2bType = IntArray(MAX_PLAYERS)
		enableCombo = BooleanArray(MAX_PLAYERS)
		big = BooleanArray(MAX_PLAYERS)
		enableSE = BooleanArray(MAX_PLAYERS)
		hurryupSeconds = IntArray(MAX_PLAYERS)
		hurryupInterval = IntArray(MAX_PLAYERS)
		useMap = BooleanArray(MAX_PLAYERS)
		mapSet = IntArray(MAX_PLAYERS)
		mapNumber = IntArray(MAX_PLAYERS)
		presetNumber = IntArray(MAX_PLAYERS)
		garbageEntries = Array(MAX_PLAYERS){LinkedList<GarbageEntry>()}
		hurryupCount = IntArray(MAX_PLAYERS)
		propMap = arrayOfNulls(MAX_PLAYERS)
		mapMaxNo = IntArray(MAX_PLAYERS)
		fldBackup = arrayOfNulls(MAX_PLAYERS)
		randMap = Random()
		winCount = IntArray(MAX_PLAYERS)
		winnerID = -1
	}

	/** Read speed presets
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 * @param preset Preset number
	 */
	private fun loadPreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		engine.speed.gravity = prop.getProperty("vsbattle.gravity.$preset", 4)
		engine.speed.denominator = prop.getProperty("vsbattle.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("vsbattle.are.$preset", 10)
		engine.speed.areLine = prop.getProperty("vsbattle.areLine.$preset", 5)
		engine.speed.lineDelay = prop.getProperty("vsbattle.lineDelay.$preset", 20)
		engine.speed.lockDelay = prop.getProperty("vsbattle.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("vsbattle.das.$preset", 14)
	}

	/** Save speed presets
	 * @param engine GameEngine
	 * @param prop Property file to save to
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

	/** Load settings not related to speeds
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 */
	private fun loadOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val playerID = engine.playerID
		bgmno = prop.getProperty("vsbattle.bgmno", 0)
		if(version>=5)
			garbageType[playerID] = prop.getProperty("vsbattle.garbageType.p$playerID", GARBAGE_TYPE_NOCHANGE_ONE_ATTACK)
		else
			garbageType[playerID] = prop.getProperty("vsbattle.garbageType", GARBAGE_TYPE_NOCHANGE_ONE_ATTACK)
		garbagePercent[playerID] = prop.getProperty("vsbattle.garbagePercent.p$playerID", 100)
		garbageCounter[playerID] = prop.getProperty("vsbattle.garbageCounter.p$playerID", true)
		garbageBlocking[playerID] = prop.getProperty("vsbattle.garbageBlocking.p$playerID", true)
		tspinEnableType[playerID] = prop.getProperty("vsbattle.tspinEnableType.p$playerID", 1)
		enableTSpin[playerID] = prop.getProperty("vsbattle.enableTSpin.p$playerID", true)
		enableTSpinKick[playerID] = prop.getProperty("vsbattle.enableTSpinKick.p$playerID", true)
		spinCheckType[playerID] = prop.getProperty("vsbattle.spinCheckType.p$playerID", 0)
		tspinEnableEZ[playerID] = prop.getProperty("vsbattle.tspinEnableEZ.p$playerID", false)
		if(version>=5)
			b2bType[playerID] = prop.getProperty("vsbattle.b2bType.p$playerID", 1)
		else {
			val b = prop.getProperty("vsbattle.enableB2B.p$playerID", true)
			b2bType[playerID] = if(b) 1 else 0
		}
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

	/** Save settings not related to speeds
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val playerID = engine.playerID
		prop.setProperty("vsbattle.bgmno", bgmno)
		prop.setProperty("vsbattle.garbageType.p$playerID", garbageType[playerID])
		prop.setProperty("vsbattle.garbagePercent.p$playerID", garbagePercent[playerID])
		prop.setProperty("vsbattle.garbageCounter.p$playerID", garbageCounter[playerID])
		prop.setProperty("vsbattle.garbageBlocking.p$playerID", garbageBlocking[playerID])
		prop.setProperty("vsbattle.tspinEnableType.p$playerID", tspinEnableType[playerID])
		prop.setProperty("vsbattle.enableTSpin.p$playerID", enableTSpin[playerID])
		prop.setProperty("vsbattle.enableTSpinKick.p$playerID", enableTSpinKick[playerID])
		prop.setProperty("vsbattle.spinCheckType.p$playerID", spinCheckType[playerID])
		prop.setProperty("vsbattle.tspinEnableEZ.p$playerID", tspinEnableEZ[playerID])
		prop.setProperty("vsbattle.b2bType.p$playerID", b2bType[playerID])
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

	/** MapRead
	 * @param field field
	 * @param prop Property file to read from
	 */
	private fun loadMap(field:Field, prop:CustomProperties, id:Int) {
		field.reset()
		//field.readProperty(prop, id);
		field.stringToField(prop.getProperty("map.$id", ""))
		field.setAllAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true)
		field.setAllAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, true)
		field.setAllAttribute(Block.BLOCK_ATTRIBUTE_SELFPLACED, false)
	}

	/** MapSave
	 * @param field field
	 * @param prop Property file to save to
	 * @param id AnyID
	 */
	private fun saveMap(field:Field, prop:CustomProperties, id:Int) {
		//field.writeProperty(prop, id);
		prop.setProperty("map.$id", field.fieldToString())
	}

	/** I have now accumulatedgarbage blockOfcountReturns
	 * @param playerID Player ID
	 * @return I have now accumulatedgarbage blockOfcount
	 */
	private fun getTotalGarbageLines(playerID:Int):Int {
		var count = 0
		garbageEntries[playerID]?.forEach {garbageEntry ->
			count += garbageEntry.lines
		}
		return count
	}

	/** For previewMapRead
	 * @param engine GameEngine
	 * @param playerID Player number
	 * @param id MapID
	 * @param forceReload trueWhen youMapForce Reload the file
	 */
	private fun loadMapPreview(engine:GameEngine, playerID:Int, id:Int, forceReload:Boolean) {
		if(propMap!![playerID]==null||forceReload) {
			mapMaxNo[playerID] = 0
			propMap[playerID] = receiver.loadProperties("config/map/vsbattle/"+mapSet[playerID]+".map")
		}

		propMap!![playerID]?.let{
			mapMaxNo[playerID] = it.getProperty("map.maxMapNumber", 0)
			engine.createFieldIfNeeded()
			loadMap(engine.field!!, it, id)
			engine.field!!.setAllSkin(engine.skin)
		}?:engine.field?.reset()
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		if(playerID==1) {
			engine.randSeed = owner.engine[0].randSeed
			engine.random = Random(owner.engine[0].randSeed)
		}

		engine.framecolor = PLAYER_COLOR_FRAME[playerID]

		garbage[playerID] = 0
		garbageSent[playerID] = 0
		lastHole[playerID] = -1
		scgettime[playerID] = 0
		lastevent[playerID] = EVENT_NONE
		lastb2b[playerID] = false
		lastcombo[playerID] = 0

		garbageEntries[playerID] = LinkedList()

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
						garbageType[playerID] += change
						if(garbageType[playerID]<0) garbageType[playerID] = 2
						if(garbageType[playerID]>2) garbageType[playerID] = 0
					}
					10 -> {
						garbagePercent[playerID] += change
						if(garbagePercent[playerID]<0) garbagePercent[playerID] = 100
						if(garbagePercent[playerID]>100) garbagePercent[playerID] = 0
					}
					11 -> garbageCounter[playerID] = !garbageCounter[playerID]
					12 -> garbageBlocking[playerID] = !garbageBlocking[playerID]
					13 -> {
						//enableTSpin[playerID] = !enableTSpin[playerID];
						tspinEnableType[playerID] += change
						if(tspinEnableType[playerID]<0) tspinEnableType[playerID] = 2
						if(tspinEnableType[playerID]>2) tspinEnableType[playerID] = 0
					}
					14 -> enableTSpinKick[playerID] = !enableTSpinKick[playerID]
					15 -> {
						spinCheckType[playerID] += change
						if(spinCheckType[playerID]<0) spinCheckType[playerID] = 1
						if(spinCheckType[playerID]>1) spinCheckType[playerID] = 0
					}
					16 -> tspinEnableEZ[playerID] = !tspinEnableEZ[playerID]
					17 -> {
						//enableB2B[playerID] = !enableB2B[playerID];
						b2bType[playerID] += change
						if(b2bType[playerID]<0) b2bType[playerID] = 2
						if(b2bType[playerID]>2) b2bType[playerID] = 0
					}
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
					23 -> {
						bgmno += change
						if(bgmno<0) bgmno = BGMStatus.count
						if(bgmno>BGMStatus.count) bgmno = 0
					}
					24 -> showStats = !showStats
					25 -> {
						useMap[playerID] = !useMap[playerID]
						if(!useMap[playerID]) {
							if(engine.field!=null) engine.field!!.reset()
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

			// Random map preview
			if(useMap[playerID]&&propMap!![playerID]!=null&&mapNumber[playerID]<0)
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
				drawMenu(engine, playerID, receiver, 0, COLOR.ORANGE, 0, "GRAVITY", engine.speed.gravity.toString(), "G-MAX", engine.speed.denominator.toString(), "ARE", engine.speed.are.toString(), "ARE LINE", engine.speed.areLine.toString(), "LINE DELAY", engine.speed.lineDelay.toString(), "LOCK DELAY", engine.speed.lockDelay.toString(), "DAS", engine.speed.das.toString())
				drawMenu(engine, playerID, receiver, 14, COLOR.GREEN, 7, "LOAD", presetNumber[playerID].toString(), "SAVE", presetNumber[playerID].toString())
			} else if(menuCursor<19) {
				var strTSpinEnable = ""
				if(version>=4) {
					if(tspinEnableType[playerID]==0) strTSpinEnable = "OFF"
					if(tspinEnableType[playerID]==1) strTSpinEnable = "T-ONLY"
					if(tspinEnableType[playerID]==2) strTSpinEnable = "ALL"
				} else
					strTSpinEnable = GeneralUtil.getONorOFF(enableTSpin[playerID])
				var strB2BType = ""
				if(b2bType[playerID]==0) strB2BType = "OFF"
				if(b2bType[playerID]==1) strB2BType = "ON"
				if(b2bType[playerID]==2) strB2BType = "SEPARATE"
				drawMenu(engine, playerID, receiver, 0, COLOR.CYAN, 9, "GARBAGE", GARBAGE_TYPE_STRING[garbageType[playerID]], "CHANGERATE",
					garbagePercent[playerID].toString()+"%", "COUNTERING", GeneralUtil.getONorOFF(garbageCounter[playerID]), "BLOCKING", GeneralUtil.getONorOFF(garbageBlocking[playerID]), "SPIN BONUS", strTSpinEnable, "KICK SPIN", GeneralUtil.getONorOFF(enableTSpinKick[playerID]), "SPIN TYPE",
					if(spinCheckType[playerID]==0)
						"4POINT"
					else
						"IMMOBILE", "EZIMMOBILE", GeneralUtil.getONorOFF(tspinEnableEZ[playerID]), "B2B", strB2BType, "COMBO", GeneralUtil.getONorOFF(enableCombo[playerID]))
			} else {
				drawMenu(engine, playerID, receiver, 0, COLOR.CYAN, 19, "BIG", GeneralUtil.getONorOFF(big[playerID]), "SE", GeneralUtil.getONorOFF(enableSE[playerID]), "HURRYUP",
					if(hurryupSeconds[playerID]==-1)
						"NONE"
					else
						hurryupSeconds[playerID].toString()+"SEC", "INTERVAL", hurryupInterval[playerID].toString())
				drawMenu(engine, playerID, receiver, 8, COLOR.PINK, 23, "BGM", BGMStatus[bgmno].toString(), "SHOW STATS", GeneralUtil.getONorOFF(showStats))
				drawMenu(engine, playerID, receiver, 12, COLOR.CYAN, 25, "USE MAP", GeneralUtil.getONorOFF(useMap[playerID]), "MAP SET", mapSet[playerID].toString(), "MAP NO.",
					if(mapNumber[playerID]<0)
						"RANDOM"
					else
						mapNumber[playerID].toString()+"/"+(mapMaxNo[playerID]-1))
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
						loadMap(engine.field!!, owner.replayProp, playerID)
						engine.field!!.setAllSkin(engine.skin)
					} else {
						if(propMap!![playerID]==null)
							propMap[playerID] = receiver.loadProperties("config/map/vsbattle/"+mapSet[playerID]+".map")

						propMap!![playerID]?.let {
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
				} else engine.field?.reset()

		return false
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.b2bEnable = b2bType[playerID]>=1
		engine.comboType = if(enableCombo[playerID]) GameEngine.COMBO_TYPE_NORMAL else GameEngine.COMBO_TYPE_DISABLE
		engine.big = big[playerID]
		engine.enableSE = enableSE[playerID]
		if(playerID==1) owner.bgmStatus.bgm = BGMStatus[bgmno]

		engine.tspinAllowKick = enableTSpinKick[playerID]
		if(version>=4) {
			when {
				tspinEnableType[playerID]==0 -> {
					engine.tspinEnable = false
					engine.useAllSpinBonus = false
				}
				tspinEnableType[playerID]==1 -> {
					engine.tspinEnable = true
					engine.useAllSpinBonus = false
				}
				tspinEnableType[playerID]==2 -> {
					engine.tspinEnable = true
					engine.useAllSpinBonus = true
				}
			}
		} else engine.tspinEnable = enableTSpin[playerID]

		if(version>=5) {
			engine.spinCheckType = spinCheckType[playerID]
			engine.tspinEnableEZ = tspinEnableEZ[playerID]
		}
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		// Status display
		if(playerID==0) {
			receiver.drawDirectFont(256, 16, GeneralUtil.getTime(engine.statistics.time.toFloat()))

			if(hurryupSeconds[playerID]>=0&&engine.timerActive&&
				engine.statistics.time>=hurryupSeconds[playerID]*60&&engine.statistics.time<(hurryupSeconds[playerID]+5)*60)
				receiver.drawDirectFont(playerID, 256-8, 32, "HURRY UP!", engine.statistics.time%2==0)
		}

		if(playerID==0&&owner.receiver.nextDisplayType!=2&&showStats) {
			receiver.drawScoreFont(engine, playerID, 0, 0, "VS-BATTLE", COLOR.ORANGE)

			receiver.drawScoreFont(engine, playerID, 0, 2, "1P ATTACK", COLOR.RED)
			receiver.drawScoreFont(engine, playerID, 0, 3, garbageSent[0].toString())

			receiver.drawScoreFont(engine, playerID, 0, 5, "2P ATTACK", COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 6, garbageSent[1].toString())

			if(!owner.replayMode) {
				receiver.drawScoreFont(engine, playerID, 0, 8, "1P WINS", COLOR.RED)
				receiver.drawScoreFont(engine, playerID, 0, 9, winCount[0].toString())

				receiver.drawScoreFont(engine, playerID, 0, 11, "2P WINS", COLOR.BLUE)
				receiver.drawScoreFont(engine, playerID, 0, 12, winCount[1].toString())
			}
		}

		if(showStats) {
			val x = receiver.getFieldDisplayPositionX(engine, playerID)
			val y = receiver.getFieldDisplayPositionY(engine, playerID)
			var fontColor = COLOR.WHITE

			if(garbage[playerID]>0) {
				if(garbage[playerID]>=1) fontColor = COLOR.YELLOW
				if(garbage[playerID]>=3) fontColor = COLOR.ORANGE
				if(garbage[playerID]>=4) fontColor = COLOR.RED

				val strTempGarbage = String.format("%5d", garbage[playerID])
				receiver.drawDirectFont(x+96, y+372, strTempGarbage, fontColor)
			}

			if(owner.receiver.nextDisplayType==2) {
				fontColor = if(playerID==0) COLOR.RED else COLOR.BLUE

				receiver.drawDirectFont(x-48, y+120, "TOTAL", fontColor, .5f)
				receiver.drawDirectFont(x-52, y+128, "ATTACK", fontColor, .5f)
				if(garbageSent[playerID]>=10)
					receiver.drawDirectFont(x-44, y+142, garbageSent[playerID].toString())
				else
					receiver.drawDirectFont(x-36, y+142, garbageSent[playerID].toString())

				receiver.drawDirectFont(x-44, y+190, "WINS", fontColor, .5f)
				if(winCount[playerID]>=10)
					receiver.drawDirectFont(x-44, y+204, winCount[playerID].toString())
				else
					receiver.drawDirectFont(x-36, y+204, winCount[playerID].toString())
			}
		}

		// Line clear event Display
		if(lastevent[playerID]!=EVENT_NONE&&scgettime[playerID]<120) {
			val strPieceName = Shape.names[lastpiece[playerID]]


				when(lastevent[playerID]) {
				EVENT_SINGLE -> receiver.drawMenuFont(engine, playerID, 2, 21, "SINGLE", COLOR.COBALT)
				EVENT_DOUBLE -> receiver.drawMenuFont(engine, playerID, 2, 21, "DOUBLE", COLOR.BLUE)
				EVENT_TRIPLE -> receiver.drawMenuFont(engine, playerID, 2, 21, "TRIPLE", COLOR.GREEN)
				EVENT_FOUR -> if(lastb2b[playerID])
					receiver.drawMenuFont(engine, playerID, 3, 21, "FOUR", COLOR.RED)
				else
					receiver.drawMenuFont(engine, playerID, 3, 21, "FOUR", COLOR.ORANGE)
				EVENT_TSPIN_SINGLE_MINI -> if(lastb2b[playerID])
					receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-MINI-S", COLOR.RED)
				else
					receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-MINI-S", COLOR.ORANGE)
				EVENT_TSPIN_SINGLE -> if(lastb2b[playerID])
					receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-SINGLE", COLOR.RED)
				else
					receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-SINGLE", COLOR.ORANGE)
				EVENT_TSPIN_DOUBLE_MINI -> if(lastb2b[playerID])
					receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-MINI-D", COLOR.RED)
				else
					receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-MINI-D", COLOR.ORANGE)
				EVENT_TSPIN_DOUBLE -> if(lastb2b[playerID])
					receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-DOUBLE", COLOR.RED)
				else
					receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-DOUBLE", COLOR.ORANGE)
				EVENT_TSPIN_TRIPLE -> if(lastb2b[playerID])
					receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-TRIPLE", COLOR.RED)
				else
					receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-TRIPLE", COLOR.ORANGE)
				EVENT_TSPIN_EZ -> if(lastb2b[playerID])
					receiver.drawMenuFont(engine, playerID, 3, 21, "EZ-$strPieceName", COLOR.RED)
				else
					receiver.drawMenuFont(engine, playerID, 3, 21, "EZ-$strPieceName", COLOR.ORANGE)
			}

			if(lastcombo[playerID]>=2)
				receiver.drawMenuFont(engine, playerID, 2, 22, (lastcombo[playerID]-1).toString()+"COMBO", COLOR.CYAN)
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {
		var enemyID = 0
		if(playerID==0) enemyID = 1

		//  Attack
		if(lines>0) {
			var pts = 0
			var ptsB2B = 0
			scgettime[playerID] = 0

			if(engine.tspin) {
				// Immobile EZ Spin
				if(engine.tspinez) {
					if(engine.useAllSpinBonus) {
						//pts += 0;
					} else
						pts += 1
					lastevent[playerID] = EVENT_TSPIN_EZ
				} else if(lines==1) {
					if(engine.tspinmini) {
						if(engine.useAllSpinBonus) {
							//pts += 0;
						} else
							pts += 1
						lastevent[playerID] = EVENT_TSPIN_SINGLE_MINI
					} else {
						pts += 2
						lastevent[playerID] = EVENT_TSPIN_SINGLE
					}
				} else if(lines==2) {
					if(engine.tspinmini&&engine.useAllSpinBonus) {
						pts += 3
						lastevent[playerID] = EVENT_TSPIN_DOUBLE_MINI
					} else {
						pts += 4
						lastevent[playerID] = EVENT_TSPIN_DOUBLE
					}
				} else if(lines>=3) {
					pts += 6
					lastevent[playerID] = EVENT_TSPIN_TRIPLE
				}// T-Spin 3 lines
				// T-Spin 2 lines
				// T-Spin 1 line
			} else if(lines==1)
			// 1Column
				lastevent[playerID] = EVENT_SINGLE
			else if(lines==2) {
				pts += 1 // 2Column
				lastevent[playerID] = EVENT_DOUBLE
			} else if(lines==3) {
				pts += 2 // 3Column
				lastevent[playerID] = EVENT_TRIPLE
			} else if(lines>=4) {
				pts += 4 // 4 lines
				lastevent[playerID] = EVENT_FOUR
			}

			// B2B
			if(engine.b2b) {
				lastb2b[playerID] = true

				if(pts>0) {
					ptsB2B += if(version>=1&&lastevent[playerID]==EVENT_TSPIN_TRIPLE&&!engine.useAllSpinBonus)
						2
					else
						1

					if(b2bType[playerID]==1) pts += ptsB2B // Non-separated B2B
				}
			} else
				lastb2b[playerID] = false

			// Combo
			if(engine.comboType!=GameEngine.COMBO_TYPE_DISABLE) {
				var cmbindex = engine.combo-1
				if(cmbindex<0) cmbindex = 0
				if(cmbindex>=COMBO_ATTACK_TABLE.size) cmbindex = COMBO_ATTACK_TABLE.size-1
				pts += COMBO_ATTACK_TABLE[cmbindex]
				lastcombo[playerID] = engine.combo
			}

			// All clear
			if(lines>=1&&engine.field!!.isEmpty) pts += 6

			// gem block attack
			pts += engine.field!!.howManyGemClears

			lastpiece[playerID] = engine.nowPieceObject!!.id

			/* if(pts > 0) {
 * garbageSent[playerID] += pts;
 * if(garbage[playerID] > 0) {
 * // Offset
 * garbage[playerID] -= pts;
 * if(garbage[playerID] < 0) {
 * // Ojama return
 * garbage[enemyID] += Math.abs(garbage[playerID]);
 * garbage[playerID] = 0;
 * }
 * } else {
 * // Attack
 * garbage[enemyID] += pts;
 * }
 * } */

			// Attack lines count
			garbageSent[playerID] += pts
			if(b2bType[playerID]==2) garbageSent[playerID] += ptsB2B

			// Offset
			garbage[playerID] = getTotalGarbageLines(playerID)
			if(pts>0&&garbage[playerID]>0&&garbageCounter[playerID])
				while(!garbageEntries!![playerID].isEmpty()&&pts>0) {
					val garbageEntry = garbageEntries!![playerID].first
					garbageEntry.lines -= pts

					if(garbageEntry.lines<=0) {
						pts = Math.abs(garbageEntry.lines)
						garbageEntries!![playerID].removeFirst()
					} else
						pts = 0
				}

			//  Attack
			if(pts>0) {
				garbageEntries!![enemyID].add(GarbageEntry(pts, playerID))

				// Separated B2B
				if(b2bType[playerID]==2&&ptsB2B>0) garbageEntries!![enemyID].add(GarbageEntry(ptsB2B, playerID))

				garbage[enemyID] = getTotalGarbageLines(enemyID)

				if(owner.engine[enemyID].ai==null&&garbage[enemyID]>=4) owner.engine[enemyID].playSE("danger")
			}
		}

		// Rising auction
		garbage[playerID] = getTotalGarbageLines(playerID)
		if((lines==0||!garbageBlocking[playerID])&&garbage[playerID]>0) {
			engine.playSE("garbage")

			while(!garbageEntries!![playerID].isEmpty()) {
				val garbageEntry = garbageEntries!![playerID].poll()
				val garbageColor = PLAYER_COLOR_BLOCK[garbageEntry.playerID]

				if(garbageEntry.lines>0) {
					var hole = lastHole[playerID]
					if(hole==-1||version<=4) hole = engine.random.nextInt(engine.field!!.width)

					if(garbageType[playerID]==GARBAGE_TYPE_NORMAL)
					// Change the normal hole position
						while(garbageEntry.lines>0) {
							engine.field!!.addSingleHoleGarbage(hole, garbageColor, engine.skin, 1)

							if(version>=5) {
								if(engine.random.nextInt(100)<garbagePercent[playerID])
									hole = engine.random.nextInt(engine.field!!.width)
							} else if(engine.random.nextInt(10)>=7) hole = engine.random.nextInt(engine.field!!.width)

							garbageEntry.lines--
						}
					else if(garbageType[playerID]==GARBAGE_TYPE_NOCHANGE_ONE_RISE) {
						// 1Hole position does not change at the rising times of auction
						if(version>=5) {
							if(engine.random.nextInt(100)<garbagePercent[playerID]) {
								var newHole = engine.random.nextInt(engine.field!!.width-1)
								if(newHole>=hole) newHole++
								hole = newHole
							}
						} else
							hole = engine.random.nextInt(engine.field!!.width)

						engine.field!!.addSingleHoleGarbage(hole, garbageColor, engine.skin, garbage[playerID])
						garbageEntries!![playerID].clear()
						break
					} else if(garbageType[playerID]==GARBAGE_TYPE_NOCHANGE_ONE_ATTACK) {
						// garbage blockThe position of the holes in the1Of times Attack I will not change(2If you change more than once)
						if(version>=5) {
							if(engine.random.nextInt(100)<garbagePercent[playerID]) {
								var newHole = engine.random.nextInt(engine.field!!.width-1)
								if(newHole>=hole) newHole++
								hole = newHole
							}
						} else
							hole = engine.random.nextInt(engine.field!!.width)

						engine.field!!.addSingleHoleGarbage(hole, garbageColor, engine.skin, garbageEntry.lines)
					}

					lastHole[playerID] = hole
				}
			}

			garbage[playerID] = 0
		}

		// HURRY UP!
		if(version>=2) {
			if(hurryupSeconds[playerID]>=0&&engine.timerActive)
				if(engine.statistics.time>=hurryupSeconds[playerID]*60) {
					hurryupCount[playerID]++

					if(hurryupCount[playerID]%hurryupInterval[playerID]==0) engine.field!!.addHurryupFloor(1, engine.skin)
				} else
					hurryupCount[playerID] = hurryupInterval[playerID]-1
		} else if(hurryupSeconds[playerID]>=0&&engine.timerActive&&engine.statistics.time>=hurryupSeconds[playerID]*60) {
			hurryupCount[playerID]++

			if(hurryupCount[playerID]%hurryupInterval[playerID]==0) engine.field!!.addHurryupFloor(1, engine.skin)
		}
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		scgettime[playerID]++

		// HURRY UP!
		if(playerID==0&&engine.timerActive&&hurryupSeconds[playerID]>=0&&engine.statistics.time==hurryupSeconds[playerID]*60)
			owner.receiver.playSE("hurryup")

		// Rising auctionMeter
		if(garbage[playerID]*receiver.getBlockGraphicsHeight(engine)>engine.meterValue)
			engine.meterValue += receiver.getBlockGraphicsHeight(engine)/2
		else if(garbage[playerID]*receiver.getBlockGraphicsHeight(engine)<engine.meterValue) engine.meterValue--
		when {
			garbage[playerID]>=4 -> engine.meterColor = GameEngine.METER_COLOR_RED
			garbage[playerID]>=3 -> engine.meterColor = GameEngine.METER_COLOR_ORANGE
			garbage[playerID]>=1 -> engine.meterColor = GameEngine.METER_COLOR_YELLOW
			else -> engine.meterColor = GameEngine.METER_COLOR_GREEN
		}

		// Settlement
		if(playerID==1&&owner.engine[0].gameActive)
			if(owner.engine[0].stat==GameEngine.Status.GAMEOVER&&owner.engine[1].stat==GameEngine.Status.GAMEOVER) {
				// Draw
				winnerID = -1
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.bgmStatus.bgm = BGMStatus.BGM.SILENT
			} else if(owner.engine[0].stat!=GameEngine.Status.GAMEOVER&&owner.engine[1].stat==GameEngine.Status.GAMEOVER) {
				// 1P win
				winnerID = 0
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.engine[0].stat = GameEngine.Status.EXCELLENT
				owner.engine[0].resetStatc()
				owner.engine[0].statc[1] = 1
				owner.bgmStatus.bgm = BGMStatus.BGM.SILENT
				if(!owner.replayMode) winCount[0]++
			} else if(owner.engine[0].stat==GameEngine.Status.GAMEOVER&&owner.engine[1].stat!=GameEngine.Status.GAMEOVER) {
				// 2P win
				winnerID = 1
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.engine[1].stat = GameEngine.Status.EXCELLENT
				owner.engine[1].resetStatc()
				owner.engine[1].statc[1] = 1
				owner.bgmStatus.bgm = BGMStatus.BGM.SILENT
				if(!owner.replayMode) winCount[1]++
			}
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 0, "RESULT", COLOR.ORANGE)
		when(winnerID) {
			-1 -> receiver.drawMenuFont(engine, playerID, 6, 1, "DRAW", COLOR.GREEN)
			playerID -> receiver.drawMenuFont(engine, playerID, 6, 1, "WIN!", COLOR.YELLOW)
			else -> receiver.drawMenuFont(engine, playerID, 6, 1, "LOSE", COLOR.WHITE)
		}

		val apm = (garbageSent[playerID]*3600).toFloat()/engine.statistics.time.toFloat()
		var apl = 0f
		if(engine.statistics.lines>0) apl = garbageSent[playerID].toFloat()/engine.statistics.lines.toFloat()

		drawResult(engine, playerID, receiver, 2, COLOR.ORANGE, "ATTACK", String.format("%10d", garbageSent[playerID]))
		drawResultStats(engine, playerID, receiver, 4, COLOR.ORANGE, AbstractMode.Statistic.LINES, AbstractMode.Statistic.PIECE)
		drawResult(engine, playerID, receiver, 8, COLOR.ORANGE, "ATK/LINE", String.format("%10g", apl))
		drawResult(engine, playerID, receiver, 10, COLOR.ORANGE, "ATTACK/MIN", String.format("%10g", apm))
		drawResultStats(engine, playerID, receiver, 12, COLOR.ORANGE, AbstractMode.Statistic.LPM, AbstractMode.Statistic.PPS, AbstractMode.Statistic.TIME)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveOtherSetting(engine, owner.replayProp)
		savePreset(engine, owner.replayProp, -1-playerID)

		if(useMap[playerID])fldBackup[playerID]?.let{saveMap(it, owner.replayProp, playerID)}

		owner.replayProp.setProperty("vsbattle.version", version)
	}

	/** I was sent from the enemygarbage blockOf data */
	private inner class GarbageEntry
	/** With parametersConstructor
	 * @param g garbage blockcount
	 * @param p Source
	 */
	(g:Int, p:Int) {
		/** garbage blockcount */
		var lines = 0

		/** Source */
		var playerID = 0

		init {
			lines = g
			playerID = p
		}
	}

	companion object {
		/** Combo attack table */
		private val COMBO_ATTACK_TABLE = intArrayOf(0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 4, 5)

		/** garbage blockChanges to the position of the holes in the normally
		 * random */
		private const val GARBAGE_TYPE_NORMAL = 0

		/** garbage blockThe position of the holes in the1I would not change my time
		 * at the rising auction */
		private const val GARBAGE_TYPE_NOCHANGE_ONE_RISE = 1

		/** garbage blockThe position of the holes in the1Of times Attack I will not
		 * change(2If you change more than once) */
		private const val GARBAGE_TYPE_NOCHANGE_ONE_ATTACK = 2

		/** garbage blockThe display name of the type */
		private val GARBAGE_TYPE_STRING = arrayOf("NORMAL", "ONE RISE", "1-ATTACK")

		/** Each player's garbage block cint */
		private val PLAYER_COLOR_BLOCK = intArrayOf(Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE)

		/** Each player's frame cint */
		private val PLAYER_COLOR_FRAME = intArrayOf(GameEngine.FRAME_COLOR_RED, GameEngine.FRAME_COLOR_BLUE)

		/** Current version */
		private const val CURRENT_VERSION = 5

		/** Number of players */
		private const val MAX_PLAYERS = 2

		/** Most recent scoring event type constants */
		private const val EVENT_NONE = 0
		private const val EVENT_SINGLE = 1
		private const val EVENT_DOUBLE = 2
		private const val EVENT_TRIPLE = 3
		private const val EVENT_FOUR = 4
		private const val EVENT_TSPIN_SINGLE_MINI = 5
		private const val EVENT_TSPIN_SINGLE = 6
		private const val EVENT_TSPIN_DOUBLE = 7
		private const val EVENT_TSPIN_TRIPLE = 8
		private const val EVENT_TSPIN_DOUBLE_MINI = 9
		private const val EVENT_TSPIN_EZ = 10
	}
}
