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
package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.LineGravity
import mu.nu.nullpo.game.play.clearRule.LineBomb
import mu.nu.nullpo.game.play.clearRule.LineSpark
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.getONorOFF
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import org.apache.logging.log4j.LogManager

/** PRACTICE Mode */
class Practice:AbstractGrand() {
	private var levelTimer = 0
	private var lastlineTime = 0
	/** Level upRemaining until point */
	private var goal = 0

	/** I got just before point */
	private var lastgoal = 0

	/** EndingThe rest of the time */
	private var rollTime = 0

	/** EndingStart flag */
	private var rollStarted = false

	/** Dan back */
	private var secretGrade = 0

	/** BGM number */
	private var bgmId = 0

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

	/** Lateral movement of the unit when Big */
	private var bigmove = false

	/** Lines count Half WhenBig */
	private var bighalf = false

	/** LevelType */
	private var leveltype = 0

	/** Preset number */
	private var presetNumber = 0

	/** Map number */
	private var mapNumber = 0

	/** Current version */
	private var version = 0

	/** Combo bonus */
	private var comboValue = 0

	/** Become clear level */
	private var goallv = 0

	/** Limit time (0:No) */
	private var timelimit = 0

	/** Ending time (0:No) */
	private var rolltimelimit = 0

	/** Arrangement of the pieces can appear */
	private var pieceEnable = MutableList(0) {false}

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

	/** How to erase Block */
	private var eraseStyle = 0

	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		log.debug("playerInit called")

		super.playerInit(engine)
		goal = 0
		lastgoal = 0
		lastScore = 0
		nextSecLv = 100
		lvupFlag = false
		comboValue = 0
		rollTime = 0
		rollStarted = false
		secretGrade = 0
		pieceEnable = MutableList(Piece.PIECE_COUNT) {false}
		fldBackup = null
		timelimitTimer = 0
		engine.frameSkin = GameEngine.FRAME_COLOR_BRONZE

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
		bgmId = prop.getProperty("practice.bgmno.$preset", 0)
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
		for(i in 0..<Piece.PIECE_COUNT)
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
		prop.setProperty("practice.bgmno.$preset", bgmId)
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
		for(i in 0..<Piece.PIECE_COUNT)
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
	override fun onSetting(engine:GameEngine):Boolean {
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
					7 -> bgmId = rangeCursor(bgmId+change, 0, BGM.count-1)
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
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
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
						engine.field.setAllSkin(engine.blkSkin)
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

					if(useMap&&engine.field.isEmpty) {
						val prop = receiver.loadProperties("config/map/practice/$mapNumber.map")
						if(prop!=null) {
							engine.createFieldIfNeeded()
							loadMap(engine.field, prop, 0)
							engine.field.setAllSkin(engine.blkSkin)
						} else
							useMap = false
					}

					owner.menuOnly = false
					return false
				}
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitFlag = true
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
	override fun renderSetting(engine:GameEngine) {
		receiver.drawMenuFont(engine, 1, 1, "PRACTICE MODE SETTINGS", EventReceiver.COLOR.ORANGE)

		if(!engine.owner.replayMode)
			receiver.drawMenuFont(engine, 1, 27, "A:START B:EXIT C+<>:FAST CHANGE", EventReceiver.COLOR.CYAN)
		else
			receiver.drawMenuFont(engine, 1, 27, "F:SKIP", EventReceiver.COLOR.RED)

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

			receiver.drawMenuFont(engine, 2, 3, "GRAVITY:", EventReceiver.COLOR.BLUE)
			receiver.drawMenuNum(engine, 11, 3, "%5d/".format(engine.speed.gravity), menuCursor==0)
			receiver.drawMenuNum(engine, 16, 3, "%5d".format(engine.speed.denominator), menuCursor==1)
			receiver.drawScoreSpeed(engine, 13, 4, engine.speed.rank, 3f)
			receiver.drawMenuFont(engine, 2, 4, "ARE:", EventReceiver.COLOR.BLUE)
			receiver.drawMenuNum(engine, 7, 4, "%2d/".format(engine.speed.are), menuCursor==2)
			receiver.drawMenuNum(engine, 11, 4, "%2d".format(engine.speed.areLine), menuCursor==3)
			receiver.drawMenuFont(engine, 2, 5, "DELAY:", EventReceiver.COLOR.BLUE)
			receiver.drawMenuNum(engine, 9, 5, "%2d+".format(engine.speed.lockDelay), menuCursor==5)
			receiver.drawMenuNum(engine, 12, 5, "%2d".format(engine.speed.lineDelay), menuCursor==4)
			receiver.drawMenuFont(engine, 15, 5, "DAS:", EventReceiver.COLOR.BLUE)
			receiver.drawMenuNum(engine, 20, 5, "%2d".format(engine.speed.das), menuCursor==6)
			receiver.drawMenuFont(
				engine, 2, 7, "BGM:%2d %s".format(bgmId, "${BGM.values[bgmId]}".uppercase()),
				menuCursor==7
			)
			receiver.drawMenuFont(engine, 2, 8, "BIG:${big.getONorOFF()}", menuCursor==8)
			receiver.drawMenuFont(engine, 2, 9, "LEVEL TYPE:${LEVELTYPE_STRING[leveltype]}", menuCursor==9)
			receiver.drawMenuFont(
				engine, 2, 10, "SPIN BONUS:${if(twistEnableType==0) "OFF" else if(twistEnableType==1) "T-ONLY" else "ALL"}",
				menuCursor==10
			)
			receiver.drawMenuFont(engine, 2, 11, "EZ SPIN:${enableTwistKick.getONorOFF()}", menuCursor==11)
			receiver.drawMenuFont(
				engine, 2, 13, "EZ IMMOBILE:${twistEnableEZ.getONorOFF()}", menuCursor==13
			)
			receiver.drawMenuFont(engine, 2, 14, "B2B:${enableB2B.getONorOFF()}", menuCursor==14)
			receiver.drawMenuFont(engine, 2, 15, "COMBO:${COMBOTYPE_STRING[comboType]}", menuCursor==15)
			receiver.drawMenuFont(engine, 2, 16, "LEVEL STOP SE:${secAlert.getONorOFF()}", menuCursor==16)
			receiver.drawMenuFont(engine, 2, 17, "BIG MOVE:${if(bigmove) "2 CELLS" else "1 CELL"}", menuCursor==17)
			receiver.drawMenuFont(engine, 2, 18, "BIG HALF:${bighalf.getONorOFF()}", menuCursor==18)
			var strGoalLv = "ENDLESS"
			if(goallv>=0)
				strGoalLv = when(leveltype) {
					LEVELTYPE_MANIA, LEVELTYPE_MANIAPLUS -> "${((goallv+1)*100)} LEVELS"
					LEVELTYPE_NONE -> "${(goallv+1)} LINES"
					else -> "${(goallv+1)} LEVELS"
				}
			receiver.drawMenuFont(engine, 2, 19, "GOAL LEVEL:$strGoalLv", menuCursor==19)
			receiver.drawMenuFont(
				engine, 2, 20, "TIME LIMIT:${if(timelimit==0) "NONE" else timelimit.toTimeStr}",
				menuCursor==20
			)
			receiver.drawMenuFont(
				engine, 2, 21, "ROLL LIMIT:${if(rolltimelimit==0) "NONE" else rolltimelimit.toTimeStr}",
				menuCursor==21
			)
			receiver.drawMenuFont(
				engine, 2, 22, "TIME LIMIT PER LEVEL:${timelimitResetEveryLevel.getONorOFF()}",
				menuCursor==22
			)
		} else {
			cx = if(menuCursor in 40..45) 15 else if(menuCursor in 46..49) 16 else cx
			cy -= if(menuCursor<29) 20 else if(menuCursor<40) 19 else if(menuCursor<=45) 30 else 29

			receiver.drawMenuFont(engine, 2, 3, "USE BONE BLOCKS:${bone.getONorOFF()}", menuCursor==23)
			var strHiddenFrames = "NONE"
			if(blockHidden==-2) strHiddenFrames = "LOCK FLASH (${engine.ruleOpt.lockFlash}F)"
			if(blockHidden>=0) strHiddenFrames = "%d (%.2f SEC.)".format(blockHidden, blockHidden/60f)
			receiver.drawMenuFont(
				engine, 2, 4, "BLOCK HIDDEN FRAMES:$strHiddenFrames",
				menuCursor==24
			)
			receiver.drawMenuFont(
				engine, 2, 5, "BLOCK HIDDEN ANIM:${blockHiddenAnim.getONorOFF()}",
				menuCursor==25
			)
			receiver.drawMenuFont(
				engine, 2, 6, "BLOCK OUTLINE TYPE:${BLOCK_OUTLINE_TYPE_STRING[blockOutlineType]}",
				menuCursor==26
			)
			receiver.drawMenuFont(
				engine, 2, 7, "BLOCK OUTLINE ONLY:${blockShowOutlineOnly.getONorOFF()}",
				menuCursor==27
			)
			receiver.drawMenuFont(
				engine, 2, 8, "HEBO HIDDEN:${if(heboHiddenLevel==0) "NONE" else "LV$heboHiddenLevel"}",
				menuCursor==28
			)
			receiver.drawMenuFont(engine, 2, 10, "PIECE I:${pieceEnable[0].getONorOFF()}", menuCursor==29)
			receiver.drawMenuFont(engine, 2, 11, "PIECE L:${pieceEnable[1].getONorOFF()}", menuCursor==30)
			receiver.drawMenuFont(engine, 2, 12, "PIECE O:${pieceEnable[2].getONorOFF()}", menuCursor==31)
			receiver.drawMenuFont(engine, 2, 13, "PIECE Z:${pieceEnable[3].getONorOFF()}", menuCursor==32)
			receiver.drawMenuFont(engine, 2, 14, "PIECE T:${pieceEnable[4].getONorOFF()}", menuCursor==33)
			receiver.drawMenuFont(engine, 2, 15, "PIECE J:${pieceEnable[5].getONorOFF()}", menuCursor==34)
			receiver.drawMenuFont(engine, 2, 16, "PIECE S:${pieceEnable[6].getONorOFF()}", menuCursor==35)
			receiver.drawMenuFont(engine, 2, 17, "PIECE I1:${pieceEnable[7].getONorOFF()}", menuCursor==36)
			receiver.drawMenuFont(engine, 2, 18, "PIECE I2:${pieceEnable[8].getONorOFF()}", menuCursor==37)
			receiver.drawMenuFont(engine, 2, 19, "PIECE I3:${pieceEnable[9].getONorOFF()}", menuCursor==38)
			receiver.drawMenuFont(engine, 2, 20, "PIECE L3:${pieceEnable[10].getONorOFF()}", menuCursor==39)
			receiver.drawMenuFont(engine, 16, 10, "USE MAP:${useMap.getONorOFF()}", menuCursor==40)
			receiver.drawMenuFont(engine, 16, 11, "[EDIT FIELD MAP]", menuCursor==41)
			receiver.drawMenuFont(engine, 16, 12, "[LOAD FIELD MAP]:$mapNumber", menuCursor==42)
			receiver.drawMenuFont(engine, 16, 13, "[SAVE FIELD MAP]:$mapNumber", menuCursor==43)
			receiver.drawMenuFont(engine, 16, 14, "[LOAD PRESET]:$presetNumber", menuCursor==44)
			receiver.drawMenuFont(engine, 16, 15, "[SAVE PRESET]:$presetNumber", menuCursor==45)

			receiver.drawMenuFont(
				engine, 17, 17, "BLOCK FALL:${BLOCK_CASCADE_TYPE_STRING[cascadeStyle]}", menuCursor==46
			)
			receiver.drawMenuFont(engine, 17, 18, "BLOCK ERASE:${BLOCK_ERASE_TYPE_STRING[eraseStyle]}", menuCursor==47)
		}
		if(!owner.replayMode) receiver.drawMenuFont(engine, cx, cy, BaseFont.CURSOR, EventReceiver.COLOR.RED)
	}

	/* Called for initialization during Ready (before initialization) */
	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			//  timeLimit setting
			if(timelimit>0) timelimitTimer = timelimit

			// BoneBlock
			engine.bone = bone

			// Set the piece that can appear
			if(version>=1) engine.nextPieceEnable = pieceEnable

			// MapFor storing backup Replay read
			if(version>=2)
				if(useMap) {
					if(owner.replayMode) {
						log.debug("Loading values data from replay data")
						engine.createFieldIfNeeded()
						loadMap(engine.field, owner.replayProp, 0)
						engine.field.setAllSkin(engine.blkSkin)
					} else {
						log.debug("Backup values data")
						fldBackup = Field(engine.field)
					}
				} else {
					log.debug("Use no values, reseting field")
					engine.field.reset()
				}
		}

		// Another Rule
		if(cascadeStyle==0)
			engine.lineGravityType = LineGravity.Native
		else {
			engine.lineGravityType = LineGravity.CASCADE
			if(eraseStyle==1) engine.clearMode = LineBomb
			if(eraseStyle==2) engine.clearMode = LineSpark
		}

		return false
	}

	/* ReadyAt the time ofCalled at initialization (Start gameJust before) */
	override fun startGame(engine:GameEngine) {
		engine.big = big
		engine.bigMove = bigmove
		engine.bigHalf = bighalf

		if(leveltype!=LEVELTYPE_MANIA&&leveltype!=LEVELTYPE_MANIAPLUS) {
			engine.b2bEnable = enableB2B
			engine.splitB2B = enableSplitB2B

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
			engine.splitB2B = false
			engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
			engine.statistics.levelDispAdd = 0
		}

		// Hidden
		engine.blockHidden = if(blockHidden==-2) engine.ruleOpt.lockFlash else blockHidden
		engine.blockHiddenAnim = blockHiddenAnim
		engine.blockOutlineType = blockOutlineType
		engine.blockShowOutlineOnly = blockShowOutlineOnly

		// Hebo Hidden
		setHeboHidden(engine)
		owner.musMan.bgm = BGM.values[bgmId]

		goal = 5*(engine.statistics.level+1)

		engine.meterValue = 0f
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
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 0, name, EventReceiver.COLOR.YELLOW)

		// fieldエディットのとき
		if(engine.stat==GameEngine.Status.FIELDEDIT) {
			// 座標
			receiver.drawScoreFont(engine, 0, 2, "X POS", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 3, "${engine.mapEditX}")
			receiver.drawScoreFont(engine, 0, 4, "Y POS", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 5, "${engine.mapEditY}")

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
			receiver.drawScoreFont(engine, 0, 2, "Score", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 2, "+${"%6d".format(lastScore)}")
			receiver.drawScoreNum(engine, 0, 3, "%7d".format(scDisp), scDisp<engine.statistics.score, 2f)

			receiver.drawScoreFont(engine, 10, 2, "/min", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(
				engine, 10.5f, 3f, engine.statistics.spm, 7 to null, scale = 1.5f
			)

			receiver.drawScoreFont(engine, 0, 5, "Spike", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 6, "%5d".format(engine.statistics.attacks), 2f)

			receiver.drawScoreFont(engine, 3, 8, "Lines", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, 8, 7, "${engine.statistics.lines}", 2f)

			receiver.drawScoreFont(engine, 4, 9, "/min", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, 8.5f, 9f, engine.statistics.lpm, null to null, scale = 1.5f)

			// Time
			receiver.drawScoreFont(engine, 0, 18, "Time", EventReceiver.COLOR.BLUE)
			val time = if(timelimit>0) timelimitTimer else maxOf(0, engine.statistics.time)
			receiver.drawScoreNum(
				engine, 0, 19, time.toTimeStr, if(timelimit>0) getTimeFontColor(time) else EventReceiver.COLOR.WHITE,
				2f
			)

			// Roll Rest time
			if(engine.gameActive&&engine.ending==2) {
				val remainTime = maxOf(0, rolltimelimit-rollTime)
				receiver.drawScoreFont(engine, 14, 18, "ROLL TIME", EventReceiver.COLOR.BLUE)
				receiver.drawScoreFont(
					engine, 14, 19, remainTime.toTimeStr, remainTime>0&&remainTime<10*60, 2f
				)
			}
			if(leveltype!=LEVELTYPE_NONE) when(leveltype) {
				LEVELTYPE_MANIA, LEVELTYPE_MANIAPLUS -> {
					//  GrandLevel
					receiver.drawScoreFont(engine, 0, 12, "Level", EventReceiver.COLOR.BLUE)
					receiver.drawScoreNum(engine, 1, 13, "%3d".format(maxOf(0, engine.statistics.level)))
					receiver.drawScoreSpeed(engine, 0, 14, if(engine.speed.gravity<0) 40 else engine.speed.gravity/128, 4)
					receiver.drawScoreNum(engine, 1, 15, "%3d".format(nextSecLv))
				}
				LEVELTYPE_POINTS -> {
					receiver.drawScoreFont(engine, 0, 11, "Level", EventReceiver.COLOR.BLUE)
					receiver.drawScoreNum(engine, 0, 12, (engine.statistics.level+1).toString())
					// ゴール
					receiver.drawScoreFont(engine, 0, 13, "GOAL", EventReceiver.COLOR.BLUE)
					var strGoal = "$goal"
					if(lastgoal!=0&&engine.ending==0) strGoal += "(-$lastgoal)"
					receiver.drawScoreFont(engine, 0, 14, strGoal)
				}
				LEVELTYPE_10LINES -> {
					receiver.drawScoreFont(engine, 0, 11, "Level", EventReceiver.COLOR.BLUE)
					receiver.drawScoreNum(engine, 0, 12, (engine.statistics.level+1).toString())

				}
			}
		}
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)

		if(engine.gameActive&&engine.timerActive)
		// Hebo Hidden
			setHeboHidden(engine)

		if(engine.gameActive&&engine.ending==2) {
			// EndingMedium
			rollTime++

			// Roll End
			if(rollTime>=rolltimelimit) {
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
				engine.stat = if(goallv==-1) GameEngine.Status.ENDINGSTART else GameEngine.Status.GAMEOVER
			}

			// 10Seconds before the countdown
			if(timelimit>0&&timelimitTimer<=10*60&&timelimitTimer%60==0&&engine.timerActive)
				engine.playSE("countdown")

			// 5Of seconds beforeBGM fadeout
			if(timelimit>0&&timelimitTimer<=5*60&&!timelimitResetEveryLevel&&engine.timerActive)
				owner.musMan.fadeSW = true
		}

		// Update meter
		setMeter(engine)
	}

	/* Called at game over */
	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.statc[0]==0&&engine.gameActive) secretGrade = engine.field.secretGrade
		return false
	}

	/* Processing on the move */
	override fun onMove(engine:GameEngine):Boolean {
		// Occurrence new piece
		if(leveltype==LEVELTYPE_MANIA||leveltype==LEVELTYPE_MANIAPLUS) {
			super.onMove(engine)
		}

		// EndingStart
		if(engine.ending==2&&!rollStarted) {
			rollStarted = true

			if(leveltype==LEVELTYPE_MANIA||leveltype==LEVELTYPE_MANIAPLUS) {
				engine.blockHidden = 300
				engine.blockHiddenAnim = true

				if(leveltype==LEVELTYPE_MANIA) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE
			}

			owner.musMan.bgm = BGM.Ending(0)
		}

		return false
	}

	/* AREProcessing during */
	override fun onARE(engine:GameEngine):Boolean {
		// Last frame
		if(leveltype==LEVELTYPE_MANIA||leveltype==LEVELTYPE_MANIAPLUS)
			return super.onARE(engine)

		return false
	}

	override fun levelUp(engine:GameEngine, lu:Int) {
		super.levelUp(engine, lu)
		setMeter(engine)
	}
	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Decrease Hebo Hidden
		if(engine.heboHiddenEnable&&ev.lines>0) {
			engine.heboHiddenTimerNow = 0
			engine.heboHiddenYNow -= ev.lines
			if(engine.heboHiddenYNow<0) engine.heboHiddenYNow = 0
		}
		calcPower(engine, ev, true)
		return if(leveltype==LEVELTYPE_MANIA||leveltype==LEVELTYPE_MANIAPLUS)
			calcScoreMania(engine, ev) else calcScoreNormal(engine, ev)
	}

	/** levelTypesMANIA At the time ofCalculate score */
	private fun calcScoreMania(engine:GameEngine, ev:ScoreEvent):Int {
		super.calcScoreGrand(engine, ev)
		val lines = ev.lines
		if(lines>=1&&engine.ending==0) {

			levelUp(
				engine,
				if(leveltype==LEVELTYPE_MANIAPLUS&&lines>=3) lines+lines-2 else lines
			)

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
			} else if(engine.statistics.level>=nextSecLv) {
				// Next Section
				engine.playSE("levelup")

				// BackgroundSwitching
				if(owner.bgMan.bg<19) owner.bgMan.nextBg = owner.bgMan.bg+1

				// Update level for next section
				nextSecLv += 100

				// Limit timeReset
				if(timelimitResetEveryLevel&&timelimit>0) timelimitTimer = timelimit
			} else if(engine.statistics.level==nextSecLv-1&&secAlert) engine.playSE("levelstop")

			if(engine.clearMode is LineBomb||engine.clearMode is LineSpark) {
				lastScore /= 7+3*engine.chain
			}
			engine.statistics.scoreLine += lastScore

			setMeter(engine)
		}
		return if(lines>=1) lastScore else 0
	}

	/** levelTypesMANIAWhen a non-systemCalculate score */
	private fun calcScoreNormal(engine:GameEngine, ev:ScoreEvent):Int {
		// Line clear bonus
		val pts = super.calcScore(engine, ev)
		// Add to score
		if(leveltype==LEVELTYPE_POINTS) {
			lastgoal = calcPoint(engine, ev)
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

				if(owner.bgMan.bg<19) owner.bgMan.nextBg = owner.bgMan.bg+1

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
		return if(pts>0) lastScore else 0
	}

	/** MeterUpdate the amount of
	 * @param engine GameEngine
	 * */
	private fun setMeter(engine:GameEngine) {
		if(engine.gameActive&&engine.ending==2) {
			val remainRollTime = rolltimelimit-rollTime
			engine.meterValue = remainRollTime*1f/rolltimelimit
			engine.meterColor = GameEngine.METER_COLOR_LEVEL
		} else if(timelimit>0) {
			val remainTime = timelimitTimer
			engine.meterValue = remainTime*1f/timelimit
			engine.meterColor = GameEngine.METER_COLOR_LIMIT
		} else if(leveltype==LEVELTYPE_10LINES) {
			engine.meterValue = engine.statistics.lines%10*1f/9f
			engine.meterColor = GameEngine.METER_COLOR_LEVEL
		} else if(leveltype==LEVELTYPE_POINTS) {
			engine.meterValue = goal/(5f*(engine.statistics.level+1))
			engine.meterColor = GameEngine.METER_COLOR_LEVEL
		} else if(leveltype==LEVELTYPE_MANIA||leveltype==LEVELTYPE_MANIAPLUS) {
			engine.meterValue = engine.statistics.level%100/99f
			engine.meterColor = GameEngine.METER_COLOR_LEVEL
		} else if(leveltype==LEVELTYPE_NONE&&goallv!=-1) {
			engine.meterValue = engine.statistics.lines/(goallv+1f)
			engine.meterColor = GameEngine.METER_COLOR_LEVEL
		}

		if(engine.meterValue<0) engine.meterValue = 0f
		if(engine.meterValue>1f) engine.meterValue = 1f
	}

	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		if(leveltype!=LEVELTYPE_MANIA&&leveltype!=LEVELTYPE_MANIAPLUS)
			engine.statistics.scoreSD += fall
	}

	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		if(leveltype!=LEVELTYPE_MANIA&&leveltype!=LEVELTYPE_MANIAPLUS)
			engine.statistics.scoreHD += fall*2
	}

	override fun renderResult(engine:GameEngine) {
		drawResultStats(
			engine, receiver, 0, EventReceiver.COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_ADD_DISP,
			Statistic.TIME, Statistic.SPL, Statistic.SPM, Statistic.LPM
		)
		if(secretGrade>0)
			drawResult(
				engine, receiver, 14, EventReceiver.COLOR.BLUE, "S. GRADE",
				"%10s".format(tableSecretGradeName[secretGrade-1])
			)
	}

	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		engine.owner.replayProp.setProperty("practice.version", version)
		if(useMap&&fldBackup!=null) saveMap(fldBackup!!, prop, 0)
		savePreset(engine, engine.owner.replayProp, -1)
		return false
	}

	companion object {
		/** Log */
		internal val log = LogManager.getLogger()

		/** Current version */
		private const val CURRENT_VERSION = 5

		/** LevelConstant of typecount */
		private const val LEVELTYPE_NONE = 0
		private const val LEVELTYPE_10LINES = 1
		private const val LEVELTYPE_POINTS = 2
		private const val LEVELTYPE_MANIA = 3
		private const val LEVELTYPE_MANIAPLUS = 4
		private const val LEVELTYPE_MAX = 5

		/** Dan&#39;s backName */
		private val tableSecretGradeName = listOf(
			"9", "8", "7", "6", "5", "4", "3", "2", "1", //  0~ 8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", //  9~17
			"GM" // 18
		)

		/** LevelThe display name of the type */
		private val LEVELTYPE_STRING = listOf("NONE", "10LINES", "POINTS", "MANIA", "MANIA+")

		/** ComboThe display name of the type */
		private val COMBOTYPE_STRING = listOf("DISABLE", "NORMAL", "DOUBLE")

		/** Outline type names */
		private val BLOCK_OUTLINE_TYPE_STRING = listOf("NONE", "NORMAL", "CONNECT", "SAMECOLOR")

		/** Cascade type names */
		private val BLOCK_CASCADE_TYPE_STRING = listOf("NONE", "CONNECT", "SAMECOLOR")
		/** Erase type names */
		private val BLOCK_ERASE_TYPE_STRING = listOf("LINE", "BOMB", "CROSS")
	}
}
