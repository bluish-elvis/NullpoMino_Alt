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
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.getONorOFF
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import org.apache.log4j.Logger

/** PRACTICE Mode */
class PracticeMode:AbstractMode() {

	private var levelTimer = 0
	private var lastlineTime = 0
	/** Level upRemaining until point */
	private var goal = 0

	/** I got just before point */
	private var lastgoal = 0

	/** Most recent scoring eventInB2BIf it&#39;s the casetrue */
	private var lastb2b = false

	/** Most recent scoring eventInCombocount */
	private var lastcombo = 0

	/** Most recent scoring eventPeace inID */
	private var lastpiece = 0

	/** EndingThe rest of the time */
	private var rolltime = 0

	/** EndingStart flag */
	private var rollstarted = false

	/** Dan back */
	private var secretGrade = 0

	/** BGM number */
	private var bgmno = 0

	/** Flag for types of Twisters allowed (0=none, 1=normal, 2=all spin) */
	private var twistEnableType = 0

	/** Old flag for allowing Twisters */
	private var enableTwist = false

	/** Flag for enabling wallkick Twisters */
	private var enableTwistKick = false

	/** Immobile EZ spin */
	private var twistEnableEZ = false

	/** Flag for enabling B2B */
	private var enableB2B = false

	private var enableSplitB2B = false

	/** ComboType */
	private var comboType = 0

	private val itemBig = BooleanMenuItem("big", "BIG", EventReceiver.COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	/** BigLateral movement of the unit when */
	private var bigmove = false

	/** BigWhenLinescountHalf */
	private var bighalf = false

	/** LevelType */
	private var leveltype = 0

	/** Preset number */
	private var presetNumber = 0

	/** Map number */
	private var mapNumber = 0

	/** Current version */
	private var version = 0

	/** Next Section Of level (This-1At levelStop) */
	private var nextseclv = 0

	/** LevelHas increased flag */
	private var lvupflag = false

	/** Combo bonus */
	private var comboValue = 0

	/** Hard drop bonus */
	private var harddropBonus = 0

	/** levelstop sound */
	private var secAlert = false

	/** Become clear level */
	private var goallv = 0

	/** Limit time (0:No) */
	private var timelimit = 0

	/** Ending time (0:No) */
	private var rolltimelimit = 0

	/** Arrangement of the pieces can appear */
	private var pieceEnable = BooleanArray(0)

	/** MapUse flag */
	private var useMap = false

	/** For backupfield (MapUsed to save the replay) */
	private var fldBackup:Field? = null

	/** Rest time */
	private var timelimitTimer = 0

	/** Level upLimit for each timeReset */
	private var timelimitResetEveryLevel = false

	/** BoneBlockI use */
	private var bone = false

	/** Number of frames before placed blocks disappear (-1:Disable) */
	private var blockHidden = 0

	/** Use alpha-blending for blockHidden */
	private var blockHiddenAnim = false

	/** Outline type */
	private var blockOutlineType = 0

	/** Show outline only flag.
	 * If enabled it does not show actual image of blocks. */
	private var blockShowOutlineOnly = false

	/** Hebo hidden level (0=None) */
	private var heboHiddenLevel = 0

	/** Cascade Style */
	private var cascadeStyle = 0

	/** How to Erasing Block */
	private var eraseStyle = 0

	/* Mode name */
	override val name = "PRACTICE"

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		log.debug("playerInit called")

		super.playerInit(engine, playerID)
		goal = 0
		lastgoal = 0
		lastscore = 0
		scDisp = 0
		lastb2b = false
		lastcombo = 0
		lastpiece = 0
		nextseclv = 100
		lvupflag = false
		comboValue = 0
		harddropBonus = 0
		rolltime = 0
		rollstarted = false
		secretGrade = 0
		pieceEnable = BooleanArray(Piece.PIECE_COUNT)
		fldBackup = null
		timelimitTimer = 0
		engine.framecolor = GameEngine.FRAME_COLOR_BRONZE

		if(!engine.owner.replayMode) {
			version = CURRENT_VERSION
			presetNumber = engine.owner.modeConfig.getProperty("practice.presetNumber", 0)
			mapNumber = engine.owner.modeConfig.getProperty("practice.mapNumber", 0)
			loadPreset(engine, engine.owner.modeConfig, -1)
		} else {
			version = engine.owner.replayProp.getProperty("practice.version", CURRENT_VERSION)
			presetNumber = 0
			mapNumber = 0
			loadPreset(engine, engine.owner.replayProp, -1)
		}
	}

	/** PresetRead
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 * @param preset Preset number
	 */
	private fun loadPreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		engine.speed.gravity = prop.getProperty("practice.gravity.$preset", 4)
		engine.speed.denominator = prop.getProperty("practice.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("practice.are.$preset", 10)
		engine.speed.areLine = prop.getProperty("practice.areLine.$preset", 5)
		engine.speed.lineDelay = prop.getProperty("practice. lineDelay.$preset", 20)
		engine.speed.lockDelay = prop.getProperty("practice.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("practice.das.$preset", 14)
		bgmno = prop.getProperty("practice.bgmno.$preset", 0)
		twistEnableType = prop.getProperty("practice.twistEnableType.$preset", 1)
		enableTwist = prop.getProperty("practice.enableTwist.$preset", true)
		enableTwistKick = prop.getProperty("practice.enableTwistKick.$preset", true)
		twistEnableEZ = prop.getProperty("practice.twistEnableEZ.$preset", false)
		enableB2B = prop.getProperty("practice.enableB2B.$preset", true)
		comboType = prop.getProperty("practice.comboType.$preset", GameEngine.COMBO_TYPE_NORMAL)
		big = prop.getProperty("practice.big.$preset", false)
		bigmove = prop.getProperty("practice.bigmove.$preset", true)
		bighalf = prop.getProperty("practice.bighalf.$preset", true)
		leveltype = prop.getProperty("practice.leveltype.$preset", LEVELTYPE_NONE)
		secAlert = prop.getProperty("practice.lvstopse.$preset", true)
		goallv = prop.getProperty("practice.goallv.$preset", -1)
		timelimit = prop.getProperty("practice.timelimit.$preset", 0)
		rolltimelimit = prop.getProperty("practice.rolltimelimit.$preset", 0)
		for(i in 0 until Piece.PIECE_COUNT)
			pieceEnable[i] = prop.getProperty("practice.pieceEnable.$i.$preset", i<Piece.PIECE_STANDARD_COUNT)
		useMap = prop.getProperty("practice.useMap.$preset", false)
		timelimitResetEveryLevel = prop.getProperty("practice.timelimitResetEveryLevel.$preset", false)
		bone = prop.getProperty("practice.bone.$preset", false)
		blockHidden = prop.getProperty("practice.blockHidden.$preset", -1)
		blockHiddenAnim = prop.getProperty("practice.blockHiddenAnim.$preset", true)
		blockOutlineType = prop.getProperty("practice.blockOutlineType.$preset", GameEngine.BLOCK_OUTLINE_NORMAL)
		blockShowOutlineOnly = prop.getProperty("practice.blockShowOutlineOnly.$preset", false)
		heboHiddenLevel = prop.getProperty("practice.heboHiddenLevel.$preset", 0)
		cascadeStyle = prop.getProperty("practice.cascadeStyle", 0)
		eraseStyle = prop.getProperty("practice.eraseStyle", 0)
	}

	/** PresetSave the
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 * @param preset Preset number
	 */
	private fun savePreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		prop.setProperty("practice.gravity.$preset", engine.speed.gravity)
		prop.setProperty("practice.denominator.$preset", engine.speed.denominator)
		prop.setProperty("practice.are.$preset", engine.speed.are)
		prop.setProperty("practice.areLine.$preset", engine.speed.areLine)
		prop.setProperty("practice.lineDelay.$preset", engine.speed.lineDelay)
		prop.setProperty("practice.lockDelay.$preset", engine.speed.lockDelay)
		prop.setProperty("practice.das.$preset", engine.speed.das)
		prop.setProperty("practice.bgmno.$preset", bgmno)
		prop.setProperty("practice.twistEnableType.$preset", twistEnableType)
		prop.setProperty("practice.enableTwist.$preset", enableTwist)
		prop.setProperty("practice.enableTwistKick.$preset", enableTwistKick)
		prop.setProperty("practice.twistEnableEZ.$preset", twistEnableEZ)
		prop.setProperty("practice.enableB2B.$preset", enableB2B)
		prop.setProperty("practice.comboType.$preset", comboType)
		prop.setProperty("practice.big.$preset", big)
		prop.setProperty("practice.bigmove.$preset", bigmove)
		prop.setProperty("practice.bighalf.$preset", bighalf)
		prop.setProperty("practice.leveltype.$preset", leveltype)
		prop.setProperty("practice.lvstopse.$preset", secAlert)
		prop.setProperty("practice.goallv.$preset", goallv)
		prop.setProperty("practice.timelimit.$preset", timelimit)
		prop.setProperty("practice.rolltimelimit.$preset", rolltimelimit)
		for(i in 0 until Piece.PIECE_COUNT)
			prop.setProperty("practice.pieceEnable.$i.$preset", pieceEnable[i])
		prop.setProperty("practice.useMap.$preset", useMap)
		prop.setProperty("practice.timelimitResetEveryLevel.$preset", timelimitResetEveryLevel)
		prop.setProperty("practice.bone.$preset", bone)
		prop.setProperty("practice.blockHidden.$preset", blockHidden)
		prop.setProperty("practice.blockHiddenAnim.$preset", blockHiddenAnim)
		prop.setProperty("practice.blockOutlineType.$preset", blockOutlineType)
		prop.setProperty("practice.blockShowOutlineOnly.$preset", blockShowOutlineOnly)
		prop.setProperty("practice.heboHiddenLevel.$preset", heboHiddenLevel)
		prop.setProperty("practice.cascadeStyle.$preset", cascadeStyle)
		prop.setProperty("practice.eraseStyle.$preset", eraseStyle)
	}

	/** MapRead into #[id]:[field] from [prop] */
	private fun loadMap(field:Field, prop:CustomProperties, id:Int) {
		field.reset()
		field.readProperty(prop, id)
		field.setAllAttribute(true, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
		field.setAllAttribute(false, Block.ATTRIBUTE.SELF_PLACED)
	}

	/** MapSave from #[id]:[field] into [prop] */
	private fun saveMap(field:Field, prop:CustomProperties, id:Int) {
		field.writeProperty(prop, id)
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			owner.menuOnly = true

			// Configuration changes
			val change = updateCursor(engine, 47)

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
					7 -> bgmno = rangeCursor(bgmno+change, 0, BGM.count-1)
					8 -> big = !big
					9 -> {
						leveltype += change
						if(leveltype<0) leveltype = LEVELTYPE_MAX-1
						if(leveltype>LEVELTYPE_MAX-1) leveltype = 0
					}
					10 -> {
						//enableTwist = !enableTwist;
						twistEnableType += change
						if(twistEnableType<0) twistEnableType = 2
						if(twistEnableType>2) twistEnableType = 0
					}
					11 -> enableTwistKick = !enableTwistKick
					13 -> twistEnableEZ = !twistEnableEZ
					14 -> enableB2B = !enableB2B
					15 -> {
						comboType += change
						if(comboType<0) comboType = 2
						if(comboType>2) comboType = 0
					}
					16 -> secAlert = !secAlert
					17 -> bigmove = !bigmove
					18 -> bighalf = !bighalf
					19 -> {
						goallv += change*m
						if(goallv<-1) goallv = 9999
						if(goallv>9999) goallv = -1
					}
					20 -> {
						timelimit += change*60*m
						if(timelimit<0) timelimit = 3600*20
						if(timelimit>3600*20) timelimit = 0
					}
					21 -> {
						rolltimelimit += change*60*m
						if(rolltimelimit<0) rolltimelimit = 3600*20
						if(rolltimelimit>3600*20) rolltimelimit = 0
					}
					22 -> timelimitResetEveryLevel = !timelimitResetEveryLevel
					23 -> bone = !bone
					24 -> {
						blockHidden += change*m
						if(blockHidden<-2) blockHidden = 9999
						if(blockHidden>9999) blockHidden = -2
					}
					25 -> blockHiddenAnim = !blockHiddenAnim
					26 -> {
						blockOutlineType += change
						if(blockOutlineType<0) blockOutlineType = 3
						if(blockOutlineType>3) blockOutlineType = 0
					}
					27 -> blockShowOutlineOnly = !blockShowOutlineOnly
					28 -> {
						heboHiddenLevel += change
						if(heboHiddenLevel<0) heboHiddenLevel = 7
						if(heboHiddenLevel>7) heboHiddenLevel = 0
					}
					29 -> pieceEnable[0] = !pieceEnable[0]
					30 -> pieceEnable[1] = !pieceEnable[1]
					31 -> pieceEnable[2] = !pieceEnable[2]
					32 -> pieceEnable[3] = !pieceEnable[3]
					33 -> pieceEnable[4] = !pieceEnable[4]
					34 -> pieceEnable[5] = !pieceEnable[5]
					35 -> pieceEnable[6] = !pieceEnable[6]
					36 -> pieceEnable[7] = !pieceEnable[7]
					37 -> pieceEnable[8] = !pieceEnable[8]
					38 -> pieceEnable[9] = !pieceEnable[9]
					39 -> pieceEnable[10] = !pieceEnable[10]
					40 -> useMap = !useMap
					41, 42, 43 -> {
						mapNumber += change
						if(mapNumber<0) mapNumber = 99
						if(mapNumber>99) mapNumber = 0
					}
					44, 45 -> presetNumber = rangeCursor(presetNumber+change, 0, 99)
					46 -> {
						cascadeStyle += change
						if(cascadeStyle<0) cascadeStyle = BLOCK_CASCADE_TYPE_STRING.size-1
						if(cascadeStyle>=BLOCK_CASCADE_TYPE_STRING.size) cascadeStyle = 0
						if(cascadeStyle==0&&eraseStyle!=0) eraseStyle = 0
					}
					47 -> {
						eraseStyle += change
						if(eraseStyle<0) eraseStyle = BLOCK_ERASE_TYPE_STRING.size-1
						if(eraseStyle>=BLOCK_CASCADE_TYPE_STRING.size) eraseStyle = 0
						if(eraseStyle!=0&&cascadeStyle==0) cascadeStyle = 1
					}
				}
			}

			// 決定
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				if(menuCursor==41) {
					// fieldエディット
					engine.enterFieldEdit()
					return true
				} else if(menuCursor==42) {
					// Map読み込み
					engine.createFieldIfNeeded()
					engine.field.reset()

					val prop = receiver.loadProperties("config/map/practice/$mapNumber.map")
					if(prop!=null) {
						loadMap(engine.field, prop, 0)
						engine.field.setAllSkin(engine.skin)
					}
				} else if(menuCursor==43) {
					// Map保存
					val prop = CustomProperties("config/map/practice/$mapNumber.map")
					saveMap(engine.field, prop, 0)
					prop.save()
				} else if(menuCursor==44)
				// Preset読み込み
					loadPreset(engine, owner.modeConfig, presetNumber)
				else if(menuCursor==45) {
					// Preset保存
					savePreset(engine, owner.modeConfig, presetNumber)
					owner.saveModeConfig()
				} else {
					// Start game
					owner.modeConfig.setProperty("practice.presetNumber", presetNumber)
					owner.modeConfig.setProperty("practice.mapNumber", mapNumber)
					savePreset(engine, owner.modeConfig, -1)
					owner.saveModeConfig()

					if(useMap&&(engine.field==null||engine.field.isEmpty)) {
						val prop = receiver.loadProperties("config/map/practice/$mapNumber.map")
						if(prop!=null) {
							engine.createFieldIfNeeded()
							loadMap(engine.field, prop, 0)
							engine.field.setAllSkin(engine.skin)
						} else
							useMap = false
					}

					owner.menuOnly = false
					return false
				}
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitflag = true

			menuTime++
		} else {
			owner.menuOnly = true

			menuTime++
			menuCursor = 0

			if(menuTime>=60) menuCursor = 22
			if(menuTime>=120||engine.ctrl.isPush(Controller.BUTTON_F)) {
				owner.menuOnly = false
				return false
			}
		}

		return true
	}

	/* Setting screen drawing */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 1, 1, "PRACTICE MODE SETTINGS", EventReceiver.COLOR.ORANGE)

		if(!engine.owner.replayMode)
			receiver.drawMenuFont(engine, playerID, 1, 27, "A:START B:EXIT C+<>:FAST CHANGE", EventReceiver.COLOR.CYAN)
		else
			receiver.drawMenuFont(engine, playerID, 1, 27, "F:SKIP", EventReceiver.COLOR.RED)

		var cx = 1
		var cy = menuCursor
		if(menuCursor<23) {
			cx = when(menuCursor) {
				0 -> 10
				1 -> 15
				2 -> 6
				3 -> 9
				4 -> 11
				5 -> 8
				6 -> 19
				else -> cx
			}
			cy = if(menuCursor<=1) 3 else if(menuCursor<=3) 4 else if(menuCursor<=6) 5 else cy

			receiver.drawMenuFont(engine, playerID, 2, 3, "GRAVITY:", EventReceiver.COLOR.BLUE)
			receiver.drawMenuNum(engine, playerID, 11, 3, String.format("%5d/", engine.speed.gravity), menuCursor==0)
			receiver.drawMenuNum(engine, playerID, 16, 3, String.format("%5d", engine.speed.denominator), menuCursor==1)
			receiver.drawSpeedMeter(engine, playerID, 13, 4, engine.speed.gravity, engine.speed.denominator, 3)
			receiver.drawMenuFont(engine, playerID, 2, 4, "ARE:", EventReceiver.COLOR.BLUE)
			receiver.drawMenuNum(engine, playerID, 7, 4, String.format("%2d/", engine.speed.are), menuCursor==2)
			receiver.drawMenuNum(engine, playerID, 11, 4, String.format("%2d", engine.speed.areLine), menuCursor==3)
			receiver.drawMenuFont(engine, playerID, 2, 5, "DELAY:", EventReceiver.COLOR.BLUE)
			receiver.drawMenuNum(engine, playerID, 9, 5, String.format("%2d+", engine.speed.lockDelay), menuCursor==5)
			receiver.drawMenuNum(engine, playerID, 12, 5, String.format("%2d", engine.speed.lineDelay), menuCursor==4)
			receiver.drawMenuFont(engine, playerID, 15, 5, "DAS:", EventReceiver.COLOR.BLUE)
			receiver.drawMenuNum(engine, playerID, 20, 5, String.format("%2d", engine.speed.das), menuCursor==6)
			receiver.drawMenuFont(
				engine, playerID, 2, 7,
				String.format("BGM:%2d %s", bgmno, "${BGM.values[bgmno]}".uppercase()), menuCursor==7
			)
			receiver.drawMenuFont(engine, playerID, 2, 8, "BIG:${big.getONorOFF()}", menuCursor==8)
			receiver.drawMenuFont(engine, playerID, 2, 9, "LEVEL TYPE:${LEVELTYPE_STRING[leveltype]}", menuCursor==9)
			receiver.drawMenuFont(
				engine, playerID, 2, 10,
				"SPIN BONUS:${if(twistEnableType==0) "OFF" else if(twistEnableType==1) "T-ONLY" else "ALL"}", menuCursor==10
			)
			receiver.drawMenuFont(engine, playerID, 2, 11, "EZ SPIN:${enableTwistKick.getONorOFF()}", menuCursor==11)
			receiver.drawMenuFont(
				engine, playerID, 2, 13, "EZ IMMOBILE:${twistEnableEZ.getONorOFF()}",
				menuCursor==13
			)
			receiver.drawMenuFont(engine, playerID, 2, 14, "B2B:${enableB2B.getONorOFF()}", menuCursor==14)
			receiver.drawMenuFont(engine, playerID, 2, 15, "COMBO:${COMBOTYPE_STRING[comboType]}", menuCursor==15)
			receiver.drawMenuFont(engine, playerID, 2, 16, "LEVEL STOP SE:${secAlert.getONorOFF()}", menuCursor==16)
			receiver.drawMenuFont(engine, playerID, 2, 17, "BIG MOVE:${if(bigmove) "2 CELLS" else "1 CELL"}", menuCursor==17)
			receiver.drawMenuFont(engine, playerID, 2, 18, "BIG HALF:${bighalf.getONorOFF()}", menuCursor==18)
			var strGoalLv = "ENDLESS"
			if(goallv>=0)
				strGoalLv = when(leveltype) {
					LEVELTYPE_MANIA, LEVELTYPE_MANIAPLUS -> "${((goallv+1)*100)} LEVELS"
					LEVELTYPE_NONE -> "${(goallv+1)} LINES"
					else -> "${(goallv+1)} LEVELS"
				}
			receiver.drawMenuFont(engine, playerID, 2, 19, "GOAL LEVEL:$strGoalLv", menuCursor==19)
			receiver.drawMenuFont(
				engine, playerID, 2, 20,
				"TIME LIMIT:${if(timelimit==0) "NONE" else timelimit.toTimeStr}", menuCursor==20
			)
			receiver.drawMenuFont(
				engine, playerID, 2, 21,
				"ROLL LIMIT:${if(rolltimelimit==0) "NONE" else rolltimelimit.toTimeStr}", menuCursor==21
			)
			receiver.drawMenuFont(
				engine, playerID, 2, 22,
				"TIME LIMIT PER LEVEL:${timelimitResetEveryLevel.getONorOFF()}", menuCursor==22
			)
		} else {
			cx = if(menuCursor in 40..45) 15 else if(menuCursor in 46..49) 16 else cx
			cy -= if(menuCursor<29) 20 else if(menuCursor<40) 19 else if(menuCursor<=45) 30 else 29

			receiver.drawMenuFont(engine, playerID, 2, 3, "USE BONE BLOCKS:${bone.getONorOFF()}", menuCursor==23)
			var strHiddenFrames = "NONE"
			if(blockHidden==-2) strHiddenFrames = "LOCK FLASH (${engine.ruleOpt.lockflash}F)"
			if(blockHidden>=0) strHiddenFrames = String.format("%d (%.2f SEC.)", blockHidden, blockHidden/60f)
			receiver.drawMenuFont(
				engine, playerID, 2, 4,
				"BLOCK HIDDEN FRAMES:$strHiddenFrames", menuCursor==24
			)
			receiver.drawMenuFont(
				engine, playerID, 2, 5,
				"BLOCK HIDDEN ANIM:${blockHiddenAnim.getONorOFF()}", menuCursor==25
			)
			receiver.drawMenuFont(
				engine, playerID, 2, 6,
				"BLOCK OUTLINE TYPE:${BLOCK_OUTLINE_TYPE_STRING[blockOutlineType]}", menuCursor==26
			)
			receiver.drawMenuFont(
				engine, playerID, 2, 7,
				"BLOCK OUTLINE ONLY:${blockShowOutlineOnly.getONorOFF()}", menuCursor==27
			)
			receiver.drawMenuFont(
				engine, playerID, 2, 8,
				"HEBO HIDDEN:${if(heboHiddenLevel==0) "NONE" else "LV$heboHiddenLevel"}", menuCursor==28
			)
			receiver.drawMenuFont(engine, playerID, 2, 10, "PIECE I:${pieceEnable[0].getONorOFF()}", menuCursor==29)
			receiver.drawMenuFont(engine, playerID, 2, 11, "PIECE L:${pieceEnable[1].getONorOFF()}", menuCursor==30)
			receiver.drawMenuFont(engine, playerID, 2, 12, "PIECE O:${pieceEnable[2].getONorOFF()}", menuCursor==31)
			receiver.drawMenuFont(engine, playerID, 2, 13, "PIECE Z:${pieceEnable[3].getONorOFF()}", menuCursor==32)
			receiver.drawMenuFont(engine, playerID, 2, 14, "PIECE T:${pieceEnable[4].getONorOFF()}", menuCursor==33)
			receiver.drawMenuFont(engine, playerID, 2, 15, "PIECE J:${pieceEnable[5].getONorOFF()}", menuCursor==34)
			receiver.drawMenuFont(engine, playerID, 2, 16, "PIECE S:${pieceEnable[6].getONorOFF()}", menuCursor==35)
			receiver.drawMenuFont(engine, playerID, 2, 17, "PIECE I1:${pieceEnable[7].getONorOFF()}", menuCursor==36)
			receiver.drawMenuFont(engine, playerID, 2, 18, "PIECE I2:${pieceEnable[8].getONorOFF()}", menuCursor==37)
			receiver.drawMenuFont(engine, playerID, 2, 19, "PIECE I3:${pieceEnable[9].getONorOFF()}", menuCursor==38)
			receiver.drawMenuFont(engine, playerID, 2, 20, "PIECE L3:${pieceEnable[10].getONorOFF()}", menuCursor==39)
			receiver.drawMenuFont(engine, playerID, 16, 10, "USE MAP:${useMap.getONorOFF()}", menuCursor==40)
			receiver.drawMenuFont(engine, playerID, 16, 11, "[EDIT FIELD MAP]", menuCursor==41)
			receiver.drawMenuFont(engine, playerID, 16, 12, "[LOAD FIELD MAP]:$mapNumber", menuCursor==42)
			receiver.drawMenuFont(engine, playerID, 16, 13, "[SAVE FIELD MAP]:$mapNumber", menuCursor==43)
			receiver.drawMenuFont(engine, playerID, 16, 14, "[LOAD PRESET]:$presetNumber", menuCursor==44)
			receiver.drawMenuFont(engine, playerID, 16, 15, "[SAVE PRESET]:$presetNumber", menuCursor==45)

			receiver.drawMenuFont(
				engine, playerID, 17, 17, "BLOCK FALL:${BLOCK_CASCADE_TYPE_STRING[cascadeStyle]}",
				menuCursor==46
			)
			receiver.drawMenuFont(engine, playerID, 17, 18, "BLOCK ERASE:${BLOCK_ERASE_TYPE_STRING[eraseStyle]}", menuCursor==47)
		}
		if(!owner.replayMode) receiver.drawMenuFont(engine, playerID, cx, cy, "\u0082", EventReceiver.COLOR.RED)
	}

	/* Called for initialization during Ready (before initialization) */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) {
			//  timeLimit setting
			if(timelimit>0) timelimitTimer = timelimit

			// BoneBlock
			engine.bone = bone

			// Set the piece that can appear
			if(version>=1) System.arraycopy(pieceEnable, 0, engine.nextPieceEnable, 0, Piece.PIECE_COUNT)

			// MapFor storing backup Replay read
			if(version>=2)
				if(useMap) {
					if(owner.replayMode) {
						log.debug("Loading values data from replay data")
						engine.createFieldIfNeeded()
						loadMap(engine.field, owner.replayProp, 0)
						engine.field.setAllSkin(engine.skin)
					} else {
						log.debug("Backup values data")
						fldBackup = Field(engine.field)
					}
				} else if(engine.field!=null) {
					log.debug("Use no values, reseting field")
					engine.field.reset()
				} else
					log.debug("Use no values")
		}

		// Another Rule
		if(cascadeStyle==0)
			engine.lineGravityType = GameEngine.LineGravity.NATIVE
		else {
			engine.lineGravityType = GameEngine.LineGravity.CASCADE
			if(eraseStyle==1) engine.clearMode = GameEngine.ClearType.LINE_GEM_BOMB
			if(eraseStyle==2) engine.clearMode = GameEngine.ClearType.LINE_GEM_SPARK
		}

		return false
	}

	/* ReadyAt the time ofCalled at initialization (Start gameJust before) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.big = big
		engine.bigmove = bigmove
		engine.bighalf = bighalf

		if(leveltype!=LEVELTYPE_MANIA&&leveltype!=LEVELTYPE_MANIAPLUS) {
			engine.b2bEnable = enableB2B
			engine.splitb2b = enableSplitB2B

			engine.comboType = comboType
			engine.statistics.levelDispAdd = 1

			engine.twistAllowKick = enableTwistKick
			when(twistEnableType) {
				0 -> engine.twistEnable = false
				1 -> engine.twistEnable = true
				else -> {
					engine.twistEnable = true
					engine.useAllSpinBonus = true
				}
			}

			engine.twistEnableEZ = twistEnableEZ
		} else {
			engine.twistEnable = false
			engine.twistAllowKick = false
			engine.b2bEnable = false
			engine.splitb2b = false
			engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
			engine.statistics.levelDispAdd = 0
		}

		// Hidden
		if(blockHidden==-2)
			engine.blockHidden = engine.ruleOpt.lockflash
		else
			engine.blockHidden = blockHidden
		engine.blockHiddenAnim = blockHiddenAnim
		engine.blockOutlineType = blockOutlineType
		engine.blockShowOutlineOnly = blockShowOutlineOnly

		// Hebo Hidden
		setHeboHidden(engine)
		owner.bgmStatus.bgm = BGM.values[bgmno]

		goal = 5*(engine.statistics.level+1)

		engine.meterValue = 0
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		setMeter(engine)
	}

	/** Set Hebo Hidden params
	 * @param engine GameEngine
	 */
	private fun setHeboHidden(engine:GameEngine) {
		if(heboHiddenLevel>=1) {
			engine.heboHiddenEnable = true

			when(heboHiddenLevel) {
				1 -> {
					engine.heboHiddenYLimit = 15
					engine.heboHiddenTimerMax = (engine.heboHiddenYNow+2)*120
				}
				2 -> {
					engine.heboHiddenYLimit = 17
					engine.heboHiddenTimerMax = (engine.heboHiddenYNow+1)*100
				}
				3 -> {
					engine.heboHiddenYLimit = 19
					engine.heboHiddenTimerMax = engine.heboHiddenYNow*60+60
				}
				4 -> {
					engine.heboHiddenYLimit = 19
					engine.heboHiddenTimerMax = engine.heboHiddenYNow*30+45
				}
				5 -> {
					engine.heboHiddenYLimit = 19
					engine.heboHiddenTimerMax = engine.heboHiddenYNow*30+30
				}
				6 -> {
					engine.heboHiddenYLimit = 19
					engine.heboHiddenTimerMax = engine.heboHiddenYNow*2+15
				}
				7 -> {
					engine.heboHiddenYLimit = 20
					engine.heboHiddenTimerMax = engine.heboHiddenYNow+15
				}
			}
		} else
			engine.heboHiddenEnable = false
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "PRACTICE", EventReceiver.COLOR.YELLOW)

		// fieldエディットのとき
		if(engine.stat==GameEngine.Status.FIELDEDIT) {

			// 座標
			receiver.drawScoreFont(engine, playerID, 0, 2, "X POS", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 3, "${engine.fldeditX}")
			receiver.drawScoreFont(engine, playerID, 0, 4, "Y POS", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 5, "${engine.fldeditY}")

			// Put your field-checking algorithm test codes here
			/* if(engine.field != null) {
			 * receiver.drawScore(engine, playerID, 0, 7,
			 * "T-SLOT+LINECLEAR", EventReceiver.COLOR.BLUE);
			 * receiver.drawScore(engine, playerID, 0, 8, "" +
			 * engine.field.getTSlotLineClearAll(false));
			 * receiver.drawScore(engine, playerID, 0, 9, "HOLE",
			 * EventReceiver.COLOR.BLUE);
			 * receiver.drawScore(engine, playerID, 0, 10, "" +
			 * engine.field.getHowManyHoles());
			 * } */

		} else {
			// Score
			receiver.drawScoreFont(engine, playerID, 0, 5, "Score", EventReceiver.COLOR.BLUE)

			receiver.drawScoreNum(engine, playerID, 6, 5, "+$lastscore")
			receiver.drawScoreNum(engine, playerID, 0, 6, "${engine.statistics.score}", 2f)

			// Time
			receiver.drawScoreFont(engine, playerID, 0, 17, "Time", EventReceiver.COLOR.BLUE)
			var time = engine.statistics.time
			if(timelimit>0) time = timelimitTimer
			if(time<0) time = 0
			receiver.drawScoreNum(
				engine, playerID, 0, 18, time.toTimeStr,
				if(timelimit>0) getTimeFontColor(time) else EventReceiver.COLOR.WHITE, 2f
			)

			// Roll Rest time
			if(engine.gameActive&&engine.ending==2) {
				var remainTime = rolltimelimit-rolltime
				if(remainTime<0) remainTime = 0
				receiver.drawScoreFont(engine, playerID, 0, 20, "ROLL TIME", EventReceiver.COLOR.BLUE)
				receiver.drawScoreFont(
					engine, playerID, 0, 21, remainTime.toTimeStr, remainTime>0&&remainTime<10*60,
					2f
				)
			}
			// 1分間あたり score
			receiver.drawScoreFont(engine, playerID, 0, 11, "SCORE/MIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 12, String.format("%10f", engine.statistics.spm))

			// 1分間あたりのLines
			receiver.drawScoreFont(engine, playerID, 0, 14, "LINE/MIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 15, "${engine.statistics.lpm}")

			if(leveltype==LEVELTYPE_MANIA||leveltype==LEVELTYPE_MANIAPLUS) {
				//  GrandLevel
				receiver.drawScoreFont(engine, playerID, 0, 9, "Level", EventReceiver.COLOR.BLUE)
				receiver.drawScoreNum(engine, playerID, 1, 10, String.format("%3d", maxOf(0, engine.statistics.level)))
				receiver.drawSpeedMeter(engine, playerID, 0, 11, if(engine.speed.gravity<0) 40 else engine.speed.gravity/128, 4)
				receiver.drawScoreNum(engine, playerID, 1, 12, String.format("%3d", nextseclv))

				// Roll Rest time
				if(engine.gameActive&&engine.ending==2) {
					var remainTime = rolltimelimit-rolltime
					if(remainTime<0) remainTime = 0
					receiver.drawScoreFont(engine, playerID, 0, 17, "ROLL TIME", EventReceiver.COLOR.BLUE)
					receiver.drawScoreNum(
						engine, playerID, 0, 18, remainTime.toTimeStr, remainTime>0&&remainTime<10*60,
						2f
					)
				}
			} else if(leveltype!=LEVELTYPE_NONE) {
				when(leveltype) {
					LEVELTYPE_POINTS -> {
						// ゴール
						receiver.drawScoreFont(engine, playerID, 0, 5, "GOAL", EventReceiver.COLOR.BLUE)
						var strGoal = "$goal"
						if(lastgoal!=0&&engine.ending==0) strGoal += "(-$lastgoal)"
						receiver.drawScoreFont(engine, playerID, 0, 6, strGoal)
					}
					LEVELTYPE_10LINES -> {
						// Lines( levelタイプが10LINESのとき)
						receiver.drawScoreFont(engine, playerID, 0, 5, "LINE", EventReceiver.COLOR.BLUE)
						receiver.drawScoreNum(engine, playerID, 0, 6, "${engine.statistics.lines}/${(engine.statistics.level+1)*10}")
					}
					else -> {
						// Lines( levelタイプがNONEのとき)
						receiver.drawScoreFont(engine, playerID, 0, 5, "LINE", EventReceiver.COLOR.BLUE)
						receiver.drawScoreNum(engine, playerID, 0, 6, engine.statistics.lines.toString())
					}
				}

				receiver.drawScoreFont(engine, playerID, 0, 8, "Level", EventReceiver.COLOR.BLUE)
				receiver.drawScoreNum(engine, playerID, 0, 9, (engine.statistics.level+1).toString())
			}

		}
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		super.onLast(engine, playerID)

		if(engine.gameActive&&engine.timerActive)
		// Hebo Hidden
			setHeboHidden(engine)

		if(engine.gameActive&&engine.ending==2) {
			// EndingMedium
			rolltime++

			// Roll End
			if(rolltime>=rolltimelimit) {
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		} else {
			if(timelimitTimer>0&&engine.timerActive) timelimitTimer--

			// Out of time
			if(timelimit>0&&timelimitTimer<=0&&engine.timerActive) {
				engine.gameEnded()
				engine.timerActive = false
				engine.resetStatc()
				if(goallv==-1)
					engine.stat = GameEngine.Status.ENDINGSTART
				else
					engine.stat = GameEngine.Status.GAMEOVER
			}

			// 10Seconds before the countdown
			if(timelimit>0&&timelimitTimer<=10*60&&timelimitTimer%60==0
				&&engine.timerActive
			)
				engine.playSE("countdown")

			// 5Of seconds beforeBGM fadeout
			if(timelimit>0&&timelimitTimer<=5*60&&!timelimitResetEveryLevel
				&&engine.timerActive
			)
				owner.bgmStatus.fadesw = true
		}

		// Update meter
		setMeter(engine)
	}

	/* Called at game over */
	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0&&engine.gameActive) secretGrade = engine.field.secretGrade
		return false
	}

	/* Processing on the move */
	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		// Occurrence new piece
		if(leveltype==LEVELTYPE_MANIA||leveltype==LEVELTYPE_MANIAPLUS) {
			if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!lvupflag) {
				// Level up
				if(engine.statistics.level<nextseclv-1) {
					engine.statistics.level++
					if(engine.statistics.level==nextseclv-1&&secAlert) engine.playSE("levelstop")
					setMeter(engine)
				}

				// Hard drop bonusInitialization
				harddropBonus = 0
			}
			if(engine.ending==0&&engine.statc[0]>0&&(version>=1||!engine.holdDisable)) lvupflag = false
		}

		// EndingStart
		if(engine.ending==2&&!rollstarted) {
			rollstarted = true

			if(leveltype==LEVELTYPE_MANIA||leveltype==LEVELTYPE_MANIAPLUS) {
				engine.blockHidden = 300
				engine.blockHiddenAnim = true

				if(leveltype==LEVELTYPE_MANIA) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE
			}

			owner.bgmStatus.bgm = BGM.Ending(0)
		}

		return false
	}

	/* AREProcessing during */
	override fun onARE(engine:GameEngine, playerID:Int):Boolean {
		// Last frame
		if(leveltype==LEVELTYPE_MANIA||leveltype==LEVELTYPE_MANIAPLUS)
			if(engine.ending==0
				&&engine.statc[0]>=engine.statc[1]-1&&!lvupflag
			) {
				if(engine.statistics.level<nextseclv-1) {
					engine.statistics.level++
					if(engine.statistics.level==nextseclv-1&&secAlert) engine.playSE("levelstop")
					setMeter(engine)
				}
				lvupflag = true
			}

		return false
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Decrease Hebo Hidden
		if(engine.heboHiddenEnable&&lines>0) {
			engine.heboHiddenTimerNow = 0
			engine.heboHiddenYNow -= lines
			if(engine.heboHiddenYNow<0) engine.heboHiddenYNow = 0
		}

		return if(leveltype==LEVELTYPE_MANIA||leveltype==LEVELTYPE_MANIAPLUS)
			calcScoreMania(engine, lines)
		else
			calcScoreNormal(engine, lines)
	}

	/** levelTypesMANIAAt the time ofCalculate score */
	private fun calcScoreMania(engine:GameEngine, lines:Int):Int {
		// Combo
		comboValue = if(lines==0) 1
		else maxOf(1, comboValue+2*lines-2)

		if(lines>=1&&engine.ending==0) {
			// Level up
			val levelb = engine.statistics.level

			if(leveltype==LEVELTYPE_MANIA)
				engine.statistics.level += lines
			else {
				var levelplus = lines
				if(lines==3) levelplus = 4
				if(lines>=4) levelplus = 6
				engine.statistics.level += levelplus
			}

			if(engine.statistics.level>=(goallv+1)*100&&goallv!=-1) {
				// Ending
				engine.statistics.level = (goallv+1)*100
				engine.ending = 1
				engine.timerActive = false
				if(rolltimelimit==0) {
					engine.gameEnded()
					secretGrade = engine.field.secretGrade
				} else {
					engine.staffrollEnable = true
					engine.staffrollEnableStatistics = false
					engine.staffrollNoDeath = false
				}
			} else if(engine.statistics.level>=nextseclv) {
				// Next Section
				engine.playSE("levelup")

				// BackgroundSwitching
				if(owner.backgroundStatus.bg<19) {
					owner.backgroundStatus.fadesw = true
					owner.backgroundStatus.fadecount = 0
					owner.backgroundStatus.fadebg = owner.backgroundStatus.bg+1
				}

				// Update level for next section
				nextseclv += 100

				// Limit timeReset
				if(timelimitResetEveryLevel&&timelimit>0) timelimitTimer = timelimit
			} else if(engine.statistics.level==nextseclv-1&&secAlert) engine.playSE("levelstop")

			// Calculate score
			if(leveltype==LEVELTYPE_MANIA) {
				var manuallock = 0
				if(engine.manualLock) manuallock = 1

				var bravo = 1
				if(engine.field.isEmpty) {
					bravo = 4
				}

				var speedBonus = engine.lockDelay-engine.statc[0]
				if(speedBonus<0) speedBonus = 0

				lastscore = ((levelb+lines)/4+engine.softdropFall+manuallock+harddropBonus)*lines*comboValue*bravo+
					engine.statistics.level/2+speedBonus*7

			} else {
				var manuallock = 0
				if(engine.manualLock) manuallock = 1

				var bravo = 1
				if(engine.field.isEmpty) bravo = 2

				var speedBonus = engine.lockDelay-engine.statc[0]
				if(speedBonus<0) speedBonus = 0

				lastscore = (((levelb+lines)/4+engine.softdropFall+manuallock+harddropBonus)*lines*comboValue+speedBonus+
					engine.statistics.level/2)*bravo

			}
			if(engine.clearMode==GameEngine.ClearType.LINE_GEM_BOMB||engine.clearMode==GameEngine.ClearType.LINE_GEM_SPARK) {
				lastscore /= 7+3*engine.chain
			}
			engine.statistics.scoreLine += lastscore

			setMeter(engine)
		}
		return if(lines>=1) lastscore else 0
	}

	override fun calcScore(engine:GameEngine, lines:Int):Int = calcPower(engine, lines)

	/** levelTypesMANIAWhen a non-systemCalculate score */
	private fun calcScoreNormal(engine:GameEngine, lines:Int):Int {
		// Line clear bonus
		val pts = super.calcScore(engine, lines)
		val spd = maxOf(0, engine.lockDelay-engine.lockDelayNow)+if(engine.manualLock) 1 else 0
		// Combo
		val cmb = if(engine.combo>=1&&lines>=1) engine.combo-1 else 0
		// Add to score
		if(pts+cmb+spd>0) {
			val get = calcScoreCombo(pts, cmb, engine.statistics.level, spd)
			if(pts>0) lastscore = get
			if(lines>=1) engine.statistics.scoreLine += get
			else engine.statistics.scoreBonus += get
			scDisp += spd

			var cmbindex = engine.combo-1
			if(cmbindex<0) cmbindex = 0
			if(cmbindex>=COMBO_GOAL_TABLE.size) cmbindex = COMBO_GOAL_TABLE.size-1
			lastgoal = calcPoint(engine, lines)+COMBO_GOAL_TABLE[cmbindex]
			goal -= lastgoal
			lastlineTime = levelTimer
			if(goal<=0) goal = 0
		}

		var endingFlag = false // EndingIf the inrushtrue

		if(leveltype==LEVELTYPE_10LINES&&engine.statistics.lines>=(engine.statistics.level+1)*10||leveltype==LEVELTYPE_POINTS&&goal<=0)
			if(engine.statistics.level>=goallv&&goallv!=-1)
			// Ending
				endingFlag = true
			else {
				// Level up
				engine.statistics.level++

				if(owner.backgroundStatus.bg<19) {
					owner.backgroundStatus.fadesw = true
					owner.backgroundStatus.fadecount = 0
					owner.backgroundStatus.fadebg = owner.backgroundStatus.bg+1
				}

				goal = 5*(engine.statistics.level+1)

				// Limit timeReset
				if(timelimitResetEveryLevel&&timelimit>0) timelimitTimer = timelimit

				engine.playSE("levelup")
			}

		// Ending ( levelTypeNONE)
		if(version>=2&&leveltype==LEVELTYPE_NONE&&engine.statistics.lines>=goallv+1&&(goallv!=-1||version<=2))
			endingFlag = true

		// EndingRush processing
		if(endingFlag) {
			engine.timerActive = false

			if(rolltimelimit==0) {
				engine.ending = 1
				engine.gameEnded()
				secretGrade = engine.field.secretGrade
			} else {
				engine.ending = 2
				engine.staffrollEnable = true
				engine.staffrollEnableStatistics = true
				engine.staffrollNoDeath = true
			}
		}

		setMeter(engine)
		return if(pts>0) lastscore else 0
	}

	/** MeterUpdate the amount of
	 * @param engine GameEngine
	 * */
	private fun setMeter(engine:GameEngine) {
		if(engine.gameActive&&engine.ending==2) {
			val remainRollTime = rolltimelimit-rolltime
			engine.meterValue = remainRollTime*receiver.getMeterMax(engine)/rolltimelimit
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainRollTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainRollTime<=20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainRollTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED
		} else if(timelimit>0) {
			val remainTime = timelimitTimer
			engine.meterValue = remainTime*receiver.getMeterMax(engine)/timelimit
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainTime<=20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED
		} else if(leveltype==LEVELTYPE_10LINES) {
			engine.meterValue = engine.statistics.lines%10*receiver.getMeterMax(engine)/9
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(engine.statistics.lines%10>=4) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(engine.statistics.lines%10>=6) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(engine.statistics.lines%10>=8) engine.meterColor = GameEngine.METER_COLOR_RED
		} else if(leveltype==LEVELTYPE_POINTS) {
			engine.meterValue = goal*receiver.getMeterMax(engine)/(5*(engine.statistics.level+1))
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(engine.meterValue<=receiver.getMeterMax(engine)/2) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(engine.meterValue<=receiver.getMeterMax(engine)/3) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(engine.meterValue<=receiver.getMeterMax(engine)/4) engine.meterColor = GameEngine.METER_COLOR_RED
		} else if(leveltype==LEVELTYPE_MANIA||leveltype==LEVELTYPE_MANIAPLUS) {
			engine.meterValue = engine.statistics.level%100*receiver.getMeterMax(engine)/99
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(engine.statistics.level%100>=50) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(engine.statistics.level%100>=80) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(engine.statistics.level==nextseclv-1) engine.meterColor = GameEngine.METER_COLOR_RED
		} else if(leveltype==LEVELTYPE_NONE&&goallv!=-1) {
			engine.meterValue = engine.statistics.lines*receiver.getMeterMax(engine)/(goallv+1)
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(engine.meterValue>=receiver.getMeterMax(engine)/10) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(engine.meterValue>=receiver.getMeterMax(engine)/5) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(engine.meterValue>=receiver.getMeterMax(engine)/2) engine.meterColor = GameEngine.METER_COLOR_RED
		}

		if(engine.meterValue<0) engine.meterValue = 0
		if(engine.meterValue>receiver.getMeterMax(engine)) engine.meterValue = receiver.getMeterMax(engine)
	}

	/* Soft drop */
	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		if(leveltype!=LEVELTYPE_MANIA&&leveltype!=LEVELTYPE_MANIAPLUS) {
			engine.statistics.scoreSD += fall
		}
	}

	/* Hard drop */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		if(leveltype==LEVELTYPE_MANIA||leveltype==LEVELTYPE_MANIAPLUS) {
			if(fall*2>harddropBonus) harddropBonus = fall*2
		} else {
			engine.statistics.scoreHD += fall*2
		}
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(
			engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, Statistic.SCORE, Statistic.LINES,
			Statistic.LEVEL_ADD_DISP, Statistic.TIME, Statistic.SPL, Statistic.SPM, Statistic.LPM
		)
		if(secretGrade>0)
			drawResult(
				engine, playerID, receiver, 14, EventReceiver.COLOR.BLUE, "S. GRADE",
				String.format("%10s", tableSecretGradeName[secretGrade-1])
			)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties):Boolean {
		engine.owner.replayProp.setProperty("practice.version", version)
		if(useMap&&fldBackup!=null) saveMap(fldBackup!!, prop, 0)
		savePreset(engine, engine.owner.replayProp, -1)
		return false
	}

	companion object {
		/** Log */
		internal val log = Logger.getLogger(PracticeMode::class.java)

		/** Current version */
		private const val CURRENT_VERSION = 5

		/** ComboGet in point */
		private val COMBO_GOAL_TABLE = intArrayOf(0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 4, 5)

		/** LevelConstant of typecount */
		private const val LEVELTYPE_NONE = 0
		private const val LEVELTYPE_10LINES = 1
		private const val LEVELTYPE_POINTS = 2
		private const val LEVELTYPE_MANIA = 3
		private const val LEVELTYPE_MANIAPLUS = 4
		private const val LEVELTYPE_MAX = 5

		/** Dan&#39;s backName */
		private val tableSecretGradeName = arrayOf(
			"9", "8", "7", "6", "5", "4", "3", "2", "1", //  0~ 8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", //  9~17
			"GM" // 18
		)

		/** LevelThe display name of the type */
		private val LEVELTYPE_STRING = arrayOf("NONE", "10LINES", "POINTS", "MANIA", "MANIA+")

		/** ComboThe display name of the type */
		private val COMBOTYPE_STRING = arrayOf("DISABLE", "NORMAL", "DOUBLE")

		/** Outline type names */
		private val BLOCK_OUTLINE_TYPE_STRING = arrayOf("NONE", "NORMAL", "CONNECT", "SAMECOLOR")

		/** Cascade type names */
		private val BLOCK_CASCADE_TYPE_STRING = arrayOf("NONE", "CONNECT", "SAMECOLOR")
		/** Erase type names */
		private val BLOCK_ERASE_TYPE_STRING = arrayOf("LINE", "BOMB", "CROSS")
	}
}
