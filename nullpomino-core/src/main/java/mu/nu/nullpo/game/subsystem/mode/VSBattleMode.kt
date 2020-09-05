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
import mu.nu.nullpo.game.event.EventReceiver
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
	private val garbageStyle get() = garbageType.map {GARBAGE_STYLE.values()[it]}

	/** Rate of change of garbage holes */
	private var garbagePercent:IntArray = IntArray(0)

	/** Allow garbage countering */
	private var garbageCounter:BooleanArray = BooleanArray(0)

	/** Allow garbage blocking */
	private var garbageBlocking:BooleanArray = BooleanArray(0)

	/** Has accumulatedgarbage blockOfcount */
	private val garbage:IntArray get() = (0 until players).map {getTotalGarbageLines(it)}.toIntArray()

	/** Had sent garbage blockOfcount */
	private var garbageSent:IntArray = IntArray(0)

	/** Had guard garbage blockOfcount */
	private var garbageGuard:IntArray = IntArray(0)

	/** Last garbage hole position */
	private var lastHole:IntArray = IntArray(0)

	/** Most recent scoring eventInB2BIf it&#39;s the casetrue */
	private var lastb2b:BooleanArray = BooleanArray(0)

	/** Most recent scoring eventInCombocount */
	private var lastcombo:IntArray = IntArray(0)

	/** Most recent scoring eventPeace inID */
	private var lastpiece:IntArray = IntArray(0)

	/** UseBGM */
	private var bgmno:Int = 0

	/** Flag for types of Twisters allowed (0=none, 1=normal, 2=all spin) */
	private var twistEnableType:IntArray = IntArray(0)

	/** Old flag for allowing Twisters */
	private var enableTwist:BooleanArray = BooleanArray(0)

	/** Flag for enabling wallkick Twisters */
	private var enableTwistKick:BooleanArray = BooleanArray(0)

	/** Immobile EZ spin */
	private var twistEnableEZ:BooleanArray = BooleanArray(0)

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
	private var garbageEntries:Array<LinkedList<GarbageEntry>> = emptyArray()

	/** HurryupAfterBlockI put count */
	private var hurryupCount:IntArray = IntArray(0)

	/** MapSets ofProperty file */
	private var propMap:Array<CustomProperties?> = emptyArray()

	/** MaximumMap number */
	private var mapMaxNo:IntArray = IntArray(0)

	/** For backupfield (MapUsed to save the replay) */
	private var fldBackup:Array<Field?> = emptyArray()

	/** MapRan for selectioncount */
	private var randMap:Random? = null

	/** Win count for each player */
	private var winCount:IntArray = IntArray(0)

	/** Version */
	private var version:Int = 0

	/* Mode name */
	override val name:String
		get() = "VS:Face to Face"

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
		garbageSent = IntArray(MAX_PLAYERS)
		garbageGuard = IntArray(MAX_PLAYERS)
		lastHole = IntArray(MAX_PLAYERS)
		lastb2b = BooleanArray(MAX_PLAYERS)
		lastcombo = IntArray(MAX_PLAYERS)
		lastpiece = IntArray(MAX_PLAYERS)
		bgmno = 0
		twistEnableType = IntArray(MAX_PLAYERS)
		enableTwist = BooleanArray(MAX_PLAYERS)
		enableTwistKick = BooleanArray(MAX_PLAYERS)
		twistEnableEZ = BooleanArray(MAX_PLAYERS)
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
		garbageEntries = Array(MAX_PLAYERS) {LinkedList<GarbageEntry>()}
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
			garbageType[playerID] = prop.getProperty("vsbattle.garbageType.p$playerID", 0)
		else
			garbageType[playerID] = prop.getProperty("vsbattle.garbageType", 0)
		garbagePercent[playerID] = prop.getProperty("vsbattle.garbagePercent.p$playerID", 100)
		garbageCounter[playerID] = prop.getProperty("vsbattle.garbageCounter.p$playerID", true)
		garbageBlocking[playerID] = prop.getProperty("vsbattle.garbageBlocking.p$playerID", true)
		twistEnableType[playerID] = prop.getProperty("vsbattle.twistEnableType.p$playerID", 1)
		enableTwist[playerID] = prop.getProperty("vsbattle.enableTwist.p$playerID", true)
		enableTwistKick[playerID] = prop.getProperty("vsbattle.enableTwistKick.p$playerID", true)
		twistEnableEZ[playerID] = prop.getProperty("vsbattle.twistEnableEZ.p$playerID", false)
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
		prop.setProperty("vsbattle.twistEnableType.p$playerID", twistEnableType[playerID])
		prop.setProperty("vsbattle.enableTwist.p$playerID", enableTwist[playerID])
		prop.setProperty("vsbattle.enableTwistKick.p$playerID", enableTwistKick[playerID])
		prop.setProperty("vsbattle.twistEnableEZ.p$playerID", twistEnableEZ[playerID])
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

	/** I have now accumulatedgarbage blockOfcountReturns
	 * @param playerID Player ID
	 * @return I have now accumulatedgarbage blockOfcount
	 */
	private fun getTotalGarbageLines(playerID:Int):Int = garbageEntries[playerID].sumBy {it.lines}

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
			loadMap(engine.field!!, it, id)
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
		engine.ruleopt.lockresetLimitMove = engine.ruleopt.lockresetLimitMove.let {if(it<0) 30 else minOf(it, 30)}
		engine.ruleopt.lockresetLimitRotate = engine.ruleopt.lockresetLimitRotate.let {if(it<0) 20 else minOf(it, 20)}
		garbageSent[playerID] = 0
		lastHole[playerID] = -1
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
			val change = updateCursor(engine, 26, playerID)

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl.isPress(Controller.BUTTON_E)) m = 100
				if(engine.ctrl.isPress(Controller.BUTTON_F)) m = 1000

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
						if(garbageType[playerID]<0) garbageType[playerID] = GARBAGE_STYLE.values().size-1
						if(garbageType[playerID]>=GARBAGE_STYLE.values().size) garbageType[playerID] = 0
					}
					10 -> {
						garbagePercent[playerID] += change
						if(garbagePercent[playerID]<0) garbagePercent[playerID] = 100
						if(garbagePercent[playerID]>100) garbagePercent[playerID] = 0
					}
					11 -> garbageCounter[playerID] = !garbageCounter[playerID]
					12 -> garbageBlocking[playerID] = !garbageBlocking[playerID]
					13 -> {
						//enableTwist[playerID] = !enableTwist[playerID];
						twistEnableType[playerID] += change
						if(twistEnableType[playerID]<0) twistEnableType[playerID] = 2
						if(twistEnableType[playerID]>2) twistEnableType[playerID] = 0
					}
					14 -> enableTwistKick[playerID] = !enableTwistKick[playerID]
					15 -> twistEnableEZ[playerID] = !twistEnableEZ[playerID]
					16 -> {
						//enableB2B[playerID] = !enableB2B[playerID];
						b2bType[playerID] += change
						if(b2bType[playerID]<0) b2bType[playerID] = 2
						if(b2bType[playerID]>2) b2bType[playerID] = 0
					}
					17 -> enableCombo[playerID] = !enableCombo[playerID]
					18 -> big[playerID] = !big[playerID]
					19 -> enableSE[playerID] = !enableSE[playerID]
					20 -> {
						hurryupSeconds[playerID] += change
						if(hurryupSeconds[playerID]<-1) hurryupSeconds[playerID] = 300
						if(hurryupSeconds[playerID]>300) hurryupSeconds[playerID] = -1
					}
					21 -> {
						hurryupInterval[playerID] += change
						if(hurryupInterval[playerID]<1) hurryupInterval[playerID] = 99
						if(hurryupInterval[playerID]>99) hurryupInterval[playerID] = 1
					}
					22 -> {
						bgmno += change
						if(bgmno<0) bgmno = BGM.count-1
						if(bgmno>=BGM.count) bgmno = 0
					}
					23 -> showStats = !showStats
					24 -> {
						useMap[playerID] = !useMap[playerID]
						if(!useMap[playerID]) {
							if(engine.field!=null) engine.field!!.reset()
						} else
							loadMapPreview(engine, playerID, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
					}
					25 -> {
						mapSet[playerID] += change
						if(mapSet[playerID]<0) mapSet[playerID] = 99
						if(mapSet[playerID]>99) mapSet[playerID] = 0
						if(useMap[playerID]) {
							mapNumber[playerID] = -1
							loadMapPreview(engine, playerID, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
						}
					}
					26 -> if(useMap[playerID]) {
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
			if(owner.engine[0].statc[4]==1&&((owner.engine[1].statc[4]==1||owner.engine[1].ai!=null)&&(playerID==1||engine.ctrl.isPush(Controller.BUTTON_A)))) {
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
					drawMenu(engine, playerID, receiver, 0, COLOR.ORANGE, 0, "GRAVITY", engine.speed.gravity.toString(), "G-MAX", engine.speed.denominator.toString(), "ARE", engine.speed.are.toString(), "ARE LINE", engine.speed.areLine.toString(), "LINE DELAY", engine.speed.lineDelay.toString(), "LOCK DELAY", engine.speed.lockDelay.toString(), "DAS", engine.speed.das.toString())
					drawMenu(engine, playerID, receiver, 14, COLOR.GREEN, 7, "LOAD", "${presetNumber[playerID]}", "SAVE", "${presetNumber[playerID]}")
				}
				menuCursor<18 -> {
					val strTWISTEnable = when {
						twistEnableType[playerID]==0 -> "OFF"
						twistEnableType[playerID]==1 -> "T-ONLY"
						twistEnableType[playerID]==2 -> "ALL"
						else -> ""
					}

					val strB2BType = when(b2bType[playerID]) {
						0 -> "OFF"
						1 -> "ON"
						2 -> "SEPARATE"
						else -> ""
					}
					drawMenu(engine, playerID, receiver, 0, COLOR.CYAN, 9, "GARBAGE", garbageStyle[playerID].name,
						"CHANGERATE", "${garbagePercent[playerID]}%", "COUNTERING", GeneralUtil.getONorOFF(garbageCounter[playerID]),
						"BLOCKING", GeneralUtil.getONorOFF(garbageBlocking[playerID]), "SPIN BONUS", strTWISTEnable,
						"KICK SPIN", GeneralUtil.getONorOFF(enableTwistKick[playerID]),
						"EZIMMOBILE", GeneralUtil.getONorOFF(twistEnableEZ[playerID]), "B2B", strB2BType,
						"COMBO", GeneralUtil.getONorOFF(enableCombo[playerID]))
				}
				else -> {
					drawMenu(engine, playerID, receiver, 0, COLOR.CYAN, 18, "BIG", GeneralUtil.getONorOFF(big[playerID]),
						"SE", GeneralUtil.getONorOFF(enableSE[playerID]),
						"HURRYUP",
						if(hurryupSeconds[playerID]==-1) "NONE" else "${hurryupSeconds[playerID]}SEC",
						"INTERVAL", "${hurryupInterval[playerID]}")
					drawMenu(engine, playerID, receiver, 8, COLOR.PINK, 22, "BGM", "${BGM.values[bgmno]}",
						"SHOW STATS", GeneralUtil.getONorOFF(showStats))
					drawMenu(engine, playerID, receiver, 12, COLOR.CYAN, 24, "USE MAP", GeneralUtil.getONorOFF(useMap[playerID]),
						"MAP SET", "${mapSet[playerID]}",
						"MAP NO.", if(mapNumber[playerID]<0) "RANDOM" else "${mapNumber[playerID]}/${mapMaxNo[playerID]-1}")
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
						loadMap(engine.field!!, owner.replayProp, playerID)
						engine.field!!.setAllSkin(engine.skin)
					} else {
						if(propMap[playerID]==null)
							propMap[playerID] = receiver.loadProperties("config/map/vsbattle/${mapSet[playerID]}.map")

						propMap[playerID]?.let {
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
		if(playerID==1) owner.bgmStatus.bgm = BGM.values[bgmno]

		engine.twistAllowKick = enableTwistKick[playerID]
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
		// Status display
		if(playerID==0) {
			receiver.drawDirectNum(232, 16, GeneralUtil.getTime(engine.statistics.time), scale = 2f)

			if(hurryupSeconds[playerID]>=0&&engine.timerActive&&
				engine.statistics.time>=hurryupSeconds[playerID]*60&&engine.statistics.time<(hurryupSeconds[playerID]+5)*60)
				receiver.drawDirectFont(256, 32, "DANGER", if(engine.statistics.time%2==0) COLOR.RED else COLOR.YELLOW)

			if(owner.receiver.nextDisplayType!=2&&showStats) {
				receiver.drawScoreFont(engine, playerID, 0, 0, "VS-BATTLE", COLOR.ORANGE)

				receiver.drawScoreFont(engine, playerID, 0, 2, "SPIKES", COLOR.PINK)
				receiver.drawScoreNum(engine, playerID, 0, 3, "${garbageSent[0]}", EventReceiver.getPlayerColor(0))
				receiver.drawScoreNum(engine, playerID, 8, 3, "${garbageSent[1]}", EventReceiver.getPlayerColor(1))

				if(!owner.replayMode) {
					receiver.drawScoreFont(engine, playerID, 0, 5, "VICTORIES", COLOR.CYAN)
					receiver.drawScoreNum(engine, playerID, 0, 6, "${winCount[0]}", EventReceiver.getPlayerColor(0))
					receiver.drawScoreNum(engine, playerID, 8, 6, "${winCount[1]}", EventReceiver.getPlayerColor(1))
				}
			}
		}
		if(showStats) {
			val x = receiver.fieldX(engine)
			val y = receiver.fieldY(engine)

			val strTempGarbage = String.format("%5d", garbage[playerID])
			garbageEntries[playerID].forEachIndexed {i, g ->
				receiver.drawDirectNum(x+176, y+372-i*16, "${g.lines}", when {
					g.lines>=1 -> COLOR.YELLOW
					g.lines>=3 -> COLOR.ORANGE
					g.lines>=4 -> COLOR.RED
					else -> COLOR.WHITE
				})
			}

//			if(owner.receiver.nextDisplayType==2) {
			val fontColor = EventReceiver.getPlayerColor(playerID)

			receiver.drawDirectFont(x-40, y+120, "TOTAL", fontColor, .5f)
			receiver.drawDirectFont(x-40, y+128, "SPIKE", fontColor, .5f)
			receiver.drawDirectNum(x-36, y+142, "${garbageSent[playerID]}")

			receiver.drawDirectFont(x-44, y+190, "WINS", fontColor, .5f)
			receiver.drawDirectNum(x-36, y+204, "${winCount[playerID]}")
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
			lastb2b[playerID] = engine.b2b
			if(engine.b2b) {
				if(b2bType[playerID]==2)
					ptsB2B += if(!engine.useAllSpinBonus) 2 else 1
			}

			// gem block attack
			pts += engine.field!!.howManyGemClears

			lastpiece[playerID] = engine.nowPieceObject!!.id

			// Offset
			garbage[playerID] = getTotalGarbageLines(playerID)
			var ofs = 0
			if(pts>0&&garbage[playerID]>0&&garbageCounter[playerID])
				while(!garbageEntries[playerID].isEmpty()&&(pts-ofs)>0) {
					val entry = garbageEntries[playerID].first
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
						val l = minOf(it.lines, when(garbageStyle[playerID]) {
							GARBAGE_STYLE.FiveLines -> 5-gct
							GARBAGE_STYLE.Primitive -> 1
							else -> engine.fieldHeight
						})
						var hole = lastHole[playerID]
						if(hole==-1) hole = engine.random.nextInt(engine.field!!.width)
						else if(engine.random.nextInt(100)<garbagePercent[playerID]||hole==-1) {
							var newHole = engine.random.nextInt(engine.field!!.width-1)
							if(newHole>=hole) newHole++
							hole = newHole
						}

						engine.field?.addSingleHoleGarbage(hole, garbageColor, engine.skin, l)
						gct += l
						it.lines -= l
						lastHole[playerID] = hole
					}
				}
				garbageEntries[playerID].removeAll {it.lines<=0}
			} while(when(garbageStyle[playerID]) {
					GARBAGE_STYLE.STACK -> false
					GARBAGE_STYLE.FiveLines -> gct<5
					else -> true
				}&&!garbageEntries[playerID].isEmpty())
		}

		// HURRY UP!
		if(hurryupSeconds[playerID]>=0&&engine.timerActive)
			if(engine.statistics.time>=hurryupSeconds[playerID]*60) {
				hurryupCount[playerID]++

				if(hurryupCount[playerID]%hurryupInterval[playerID]==0) engine.field!!.addHurryupFloor(1, engine.skin)
			} else hurryupCount[playerID] = hurryupInterval[playerID]-1

		return pts
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
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
				owner.bgmStatus.bgm = BGM.SILENT
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
				owner.bgmStatus.bgm = BGM.SILENT
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
				owner.bgmStatus.bgm = BGM.SILENT
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

		val apm = (garbageSent[playerID]*3600).toFloat()/engine.statistics.time.toFloat()
		var apl = 0f
		if(engine.statistics.lines>0) apl = garbageSent[playerID].toFloat()/engine.statistics.lines.toFloat()

		drawResult(engine, playerID, receiver, 2, COLOR.ORANGE, "ATTACK", garbageSent[playerID])
		drawResultStats(engine, playerID, receiver, 4, COLOR.ORANGE, Statistic.LINES, Statistic.PIECE)
		drawResult(engine, playerID, receiver, 8, COLOR.ORANGE, "Spikes/LINE", apl)
		drawResult(engine, playerID, receiver, 10, COLOR.ORANGE, "Spikes/MIN", apm)
		drawResultStats(engine, playerID, receiver, 12, COLOR.ORANGE, Statistic.PPS, Statistic.MAXCOMBO, Statistic.MAXB2B)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveOtherSetting(engine, owner.replayProp)
		savePreset(engine, owner.replayProp, -1-playerID)

		if(useMap[playerID]) fldBackup[playerID]?.let {saveMap(it, owner.replayProp, playerID)}

		owner.replayProp.setProperty("vsbattle.version", version)
	}

	/** sent from the enemygarbage blockOf data
	 * @param lines garbage blockcount
	 * @param playerID Sender players ID
	 */
	private data class GarbageEntry(var lines:Int, val playerID:Int)

	companion object {
		/** Combo attack table */
		private val COMBO_ATTACK_TABLE = intArrayOf(0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 4, 5)

		/** garbage blockChanges to the position of the holes in the normally
		 * random */
		private enum class GARBAGE_STYLE {
			STACK // One Attack will One garbage-Group
			,
			FiveLines // Each One turn will draws max 5 lines
			,
			OnceAll // All garbages will put-out on Once placing
			,
			Primitive // All garbages will put-out and all garbage groups will scattered
		}

		/** Each player's garbage block cint */
		private val PLAYER_COLOR_BLOCK = intArrayOf(Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE)

		/** Each player's frame cint */
		private val PLAYER_COLOR_FRAME = intArrayOf(GameEngine.FRAME_COLOR_GREEN, GameEngine.FRAME_COLOR_BLUE)

		/** Current version */
		private const val CURRENT_VERSION = 5

		/** Number of players */
		private const val MAX_PLAYERS = 2

	}
}
