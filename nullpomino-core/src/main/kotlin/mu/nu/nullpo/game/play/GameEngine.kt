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
package mu.nu.nullpo.game.play

import kotlinx.serialization.encodeToString
import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.component.SpeedParam.Companion.SDS_FIXED
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.ReplayData
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.event.ScoreEvent.Twister
import mu.nu.nullpo.game.play.clearRule.*
import mu.nu.nullpo.game.subsystem.ai.AIPlayer
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick
import mu.nu.nullpo.gui.common.ConfigGlobal.AIConf
import mu.nu.nullpo.gui.common.ConfigGlobal.TuneConf
import mu.nu.nullpo.util.GeneralUtil
import mu.nu.nullpo.util.GeneralUtil.Json
import mu.nu.nullpo.util.GeneralUtil.toInt
import net.omegaboshi.nullpomino.game.subsystem.randomizer.MemorylessRandomizer
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer
import org.apache.logging.log4j.LogManager
import zeroxfc.nullpo.custom.libs.MathHelper.pythonModulo
import zeroxfc.nullpo.custom.libs.ProfileProperties
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.random.Random
import mu.nu.nullpo.game.event.EventReceiver.Companion as ReceiverC

/** Each player's Game processing*/
class GameEngine(
	/** owner このゲームエンジンを所有するGameOwnerクラス */
	val owner:GameManager,
	/**Playerの number */
	val playerID:Int,
	/** ルール設定 */
	var ruleOpt:RuleOptions = RuleOptions(),
	/** wallkick判定アルゴリズム */
	var wallkick:Wallkick? = null,
	/** Blockピースの出現順の生成アルゴリズム */
	var randomizer:Randomizer = MemorylessRandomizer()
) {
	var field = Field(); private set
	/** Controller: You can get player's input from here */
	var ctrl = Controller(); private set
	/** Statistics: Various game statistics
	 * such as score, number of lines, etc */
	val statistics = Statistics().apply {rule = ruleOpt.strRuleName}
	/** SpeedParam: Parameters of game speed
	 * (Gravity, ARE, Line clear delay, etc.) */
	val speed = SpeedParam()

	/** Gravity counter (The piece falls when this reaches
	 * to the value of speed denominator) */
	var gCount = 0; private set

	/** The first random-seed */
	var randSeed = 0L
	/** Random: Used for creating various randomness */
	var random = Random(0L)
	/** ReplayData: Manages input data for replays */
	private var replayData = ReplayData()

	var frameX = 0f
	var frameY = 0f

	/** AIPlayer: AI for autoplaying */
	var ai:AIPlayer? = null
	var aiConf:AIConf = AIConf()
	/** AIの移動間隔 */
	var aiMoveDelay
		get() = aiConf.moveDelay
		set(value) {
			aiConf.moveDelay = value
		}
	/** AI think delay (Only when using thread) */
	var aiThinkDelay
		get() = aiConf.thinkDelay
		set(value) {
			aiConf.thinkDelay = value
		}
	/** AIでスレッドを使う */
	var aiUseThread
		get() = aiConf.useThread
		set(value) {
			aiConf.useThread = value
		}
	var aiShowHint
		get() = aiConf.showHint
		set(value) {
			aiConf.showHint = value
		}
	/** Pre-think with AI */
	var aiPreThink
		get() = aiConf.preThink
		set(value) {
			aiConf.preThink = value
		}
	/** Show internal state of AI */
	var aiShowState
		get() = aiConf.showState
		set(value) {
			aiConf.showState = value
		}
	/** AI Hint piece (copy of current or hold) */
	var aiHintPiece:Piece? = null
	/** AI Hint X position */
	var aiHintX = 0
	/** AI Hint Y position */
	var aiHintY = 0
	/** AI Hint piece direction */
	var aiHintRt = 0
	/** True if AI Hint is ready */
	var aiHintReady = false; private set

	/** Current main game status */
	var stat:Status = Status.NOTHING
	/** Free status counters */
	val statc = MutableList(MAX_STATC) {0}

	/** True if game play, false if menu.
	 * Used for alternate keyboard mappings. */
	var isInGame = false

	/** True if the game is active */
	var gameActive = false
	/** True if the timer is active */
	var timerActive = false
	/** True if the game is started
	 * (It will not change back to false until the game is reset) */
	var gameStarted = false
	/** Timer for replay */
	var replayTimer = 0; private set
	/** Time of game start in milliseconds */
	var startTime = 0L
	/** Time of game end in milliseconds */
	var endTime = 0L

	private var versionMajor = 0f
	private var versionMinor = 0
	/** OLD minor version (Used for 6.9 or earlier replays) */
	private var versionMinorOld = 0f
	/** Dev build flag */
	private val versionIsDevBuild = false
	var quitFlag = false

	/** Piece object of current piece */
	var nowPieceObject:Piece? = null
	/** X coord. of current piece */
	var nowPieceX = 0
	/** Y coord. of current piece */
	var nowPieceY = 0
	/** Bottommost Y coord of current piece
	 * (Used for ghost piece and harddrop) */
	var nowPieceBottomY = 0
	/** Write anything other than -1 to override whole current piece cint */
	var nowPieceColorOverride:Block.COLOR? = null; private set

	/** Allow/Disallow certain piece */
	var nextPieceEnable = List(Piece.PIECE_COUNT) {it<Piece.PIECE_STANDARD_COUNT}
	/** Preferred size of next piece array
	 * Might be ignored by certain Randomizer. (Default:1400) */
	var nextPieceArraySize = 0
	/** Array of next piece IDs */
	var nextPieceArrayID = List(0) {0}
	/** Array of next piece Objects */
	var nextPieceArrayObject = emptyList<Piece>()
	/** Number of pieces spawned (Used by next piece sequence) */
	var nextPieceCount = 0
	/** Hold piece (null: None) */
	var holdPieceObject:Piece? = null
	/** True if hold is disabled because player used it already */
	var holdDisable = false
	/** Number of holds used */
	var holdUsedCount = 0

	/** Number of lines currently clearing
	 * (if [clearMode] isn't [ClearType.LINE], this shows Number of cleared blocks)  */
	var lineClearing = 0; internal set
	var garbageClearing = 0; private set
	/** Line gravity type (Native, Cascade, etc) */
	var lineGravityType:LineGravity = LineGravity.Native
	/** Current number of chains */
	var chain = 0; internal set

	/** Number of lines cleared for this chains */
	var lineGravityTotalLines = 0

	/** Lock delay counter */
	var lockDelayNow = 0//; private set

	/** DAS counter */
	var dasCount = 0; private set
	/** DAS direction (-1:Left 0:None 1:Right) */
	var dasDirection = 0; private set
	/** DAS delay counter */
	var dasSpeedCount = 0; private set
	/** Repeat statMove() for instant DAS */
	var dasRepeat = false; private set
	/** In the middle of an instant DAS loop */
	var dasInstant = false; private set
	/** Wall pushing DAS for preventing move-fail sounds */
	var dasWall = false; private set

	/** Disallow shift while locking key is pressed */
	var shiftLock = 0; private set

	/** IRS direction */
	var initialSpinDirection = 0; private set
	/** Last IRS direction */
	var initialSpinLastDirection = 0; private set
	/** IRS continuous use flag */
	var initialSpinContinuousUse = false; private set
	/** IHS */
	var initialHoldFlag = false; private set
	/** IHS continuous use flag */
	var initialHoldContinuousUse = false; private set

	/** Number of current piece movement */
	var nowPieceMoveCount = 0; private set
	/** Number of current piece rotations */
	var nowPieceSpinCount = 0; private set
	/** Number of current piece failed rotations */
	var nowPieceSpinFailCount = 0; private set
	/** Number of movement while touching to the floor */
	var extendedMoveCount = 0; private set
	/** Number of rotations while touching to the floor */
	var extendedSpinCount = 0; private set
	/** Number of wall kicks used by current piece */
	var nowWallkickCount = 0; private set
	/** Number of upward wall kicks used by current piece */
	var nowWallkickRiseCount = 0; private set
	/** Number of rows fell by soft drop (Used by soft drop bonuses) */
	var softdropFall = 0; private set
	/** Number of rows fell by hard drop (Used by soft drop bonuses) */
	var harddropFall = 0; private set
	/** fall per frame */
	var fpf = 0; private set
	/** Soft drop continuous use flag */
	var softdropContinuousUse = false; private set
	/** Hard drop continuous use flag */
	var harddropContinuousUse = false; private set
	/** True if the piece was manually locked by player */
	var manualLock = false; private set

	/** Last successful movement */
	var lastMove = LastMove.NONE; private set
	/** Last Erased Line's bottom Y-coordinates*/
	var lastLineY = 0; internal set

	/** True if last placement is Finesse */
	var finesse = false; private set
	var finesseCombo = 0; private set
	/** Number of current piece moves */
	var nowPieceSteps = 0; private set

	/** Most recent scoring event type */
	var lastEvent:ScoreEvent? = null

	var lastLinesY = emptySet<Set<Int>>(); internal set

	/** True if last erased line is Split */
	var split = false; internal set
	/** Last Twister */
	var twistType:Twister? = null; private set
	/** True if Twister */
	val twist get() = twistType!=null
	/** True if Twister Mini */
	val twistMini get() = twistType?.mini==true
	/** EZ Twister */
	val twistEZ get() = twistType?.ez==true

	/** True if last erasing line is B2B */
	val b2b get() = b2bCount>0
	/** B2B counter */
	var b2bCount = -1; internal set

	/** Number of combos */
	var combo = -1; internal set

	/** Twister enable flag */
	var twistEnable = false
	/** EZ-T toggle */
	var twistEnableEZ = false
	/** Allow Twister with wall kicks */
	var twistAllowKick = false
	/** All Spins flag */
	var useAllSpinBonus = false

	/** B2B enable flag */
	var b2bEnable = false
	/** Does Split trigger B2B flag */
	var splitB2B = false

	/** Combo counts Line border
	 * (0:Disable, 1: Normal Ren, 2: Except Single, 4:Only Quad) */
	var comboType = 0

	/** Number of frames before placed blocks disappear (-1:Disable) */
	var blockHidden = 0
	/** Use alpha-blending for blockHidden */
	var blockHiddenAnim = false

	/** Outline type */
	var blockOutlineType = 0

	/** Show outline only flag.
	 * If enabled it does not show actual image of blocks. */
	var blockShowOutlineOnly = false

	/** Hebo-hidden Enable flag */
	var heboHiddenEnable = false
	/** Hebo-hidden Timer */
	var heboHiddenTimerNow = 0
	/** Hebo-hidden Timer Max */
	var heboHiddenTimerMax = 0
	/** Hebo-hidden Y coord */
	var heboHiddenYNow = 0
	/** Hebo-hidden Y coord Limit */
	var heboHiddenYLimit = 0

	/** Set when ARE or line delay is canceled */
	var delayCancel = false; private set
	/** Piece must move left after canceled delay */
	var delayCancelMoveLeft = false; private set
	/** Piece must move right after canceled delay */
	var delayCancelMoveRight = false; private set

	/** Use bone blocks [][][][] */
	var bone = false
	/** gem Block Rate per blocks */
	var gemRate = 0f

	/** Big blocks mode */
	var big = false
	/** Big movement type (false:1cell true:2cell) */
	var bigMove = false
	/** Halves the amount of lines cleared in Big mode */
	var bigHalf = false

	/** True if rotate is used */
	var spun = false; private set
	/** True if wallkick is used */
	var kicked = false; private set
	/** Field size (-1:Default) */
	var fieldWidth = -1
	var fieldHeight = -1
	var fieldHiddenHeight = -1

	/** Ending mode (0:During the normal game) */
	var ending = 0

	/** Enable Credits phase (Staffroll) after ending = 1 */
	var staffrollEnable = false

	/** Disable GameOver in Credits phase */
	var staffrollNoDeath = false

	/** Update various statistics in Credits phase */
	var staffrollEnableStatistics = false

	/** Field's Frame color-int */
	var frameSkin = 0

	val isRetroSkin get() = frameSkin==FRAME_SKIN_GB||frameSkin==FRAME_SKIN_SG

	/** Duration of Ready->Go */
	var readyStart = 0
	var readyEnd = 0
	var goStart = 0
	var goEnd = 0

	/** True if Ready->Go is already done */
	var readyDone = false

	/** Number of Revives */
	var lives = 0

	/** 150個以上Blockがあるとtrue, 70個まで減らすとfalseになる */
	var recoveryFlag = false

	/** Ghost piece flag */
	var ghost = false

	/** Amount of meter  */
	var meterValue = 0f

	/** Color of meter */
	var meterColor = 0

	/** Amount of meter 0-100 */
	var meterValueSub = 0f

	/** Color of meter (under layer) */
	var meterColorSub = 0

	/** Lag flag (Infinite length of ARE will happen
	 * after placing a piece until this flag is set to false) */
	var lagARE = false; private set

	/** Lag flag (Pause the game completely) */
	var lagStop = false; private set

	/** Field display size (-1 for mini, 1 for big, 0 for normal) */
	var displaySize = 0

	/** @return Width&Height of block image*/
	val blockSize get() = ReceiverC.getBlockSize(displaySize)

	/** Sound effects enable flag */
	var enableSE = false

	/** Stops all other players when this player dies */
	var dieAll = false

	/** Field visible flag (false for invisible challenge) */
	var isVisible = false

	/** Piece preview visible flag */
	var isNextVisible = false
	/** Hold piece visible flag */
	var isHoldVisible = false

	/** Field edit screen: Cursor coord */
	var mapEditX = 0; private set
	/** Field edit screen: Cursor coord */
	var mapEditY = 0; private set
	/** Field edit screen: Selected cint */
	var mapEditColor = 0; private set
	/** Field edit screen: Previous game status number */
	var mapEditPreviousStat = Status.NOTHING; private set
	/** Field edit screen: Frame counter */
	var mapEditFrames = 0; private set

	/** Next-skip during Ready->Go */
	var holdButtonNextSkip = false

	/** Allow default text rendering
	 * ( such as "READY", "GO!", "GAME OVER",etc. ) */
	var allowTextRenderByReceiver = true

	var itemQueue:MutableList<Item> = mutableListOf(); private set
	/** Item enable flag */
	var itemEnable:Item?
		get() = itemQueue.firstOrNull()
		set(value) {
			if(value!=null) itemQueue.addFirst(value)
		}
	/** RollRoll (Auto rotation) enable flag */
	var itemRollRollEnable
		get() = itemEnable is Item.ROLL_ROLL
		set(value) {
			itemEnable = if(value) Item.ROLL_ROLL() else null
		}
	/** RollRoll (Auto rotation) interval */
	var itemRollRollInterval
		get() = (itemEnable as? Item.ROLL_ROLL)?.interval?:0
		set(value) {
			if(itemEnable is Item.ROLL_ROLL) (itemEnable as? Item.ROLL_ROLL)?.interval = value
		}//; private set

	/** X-RAY enable flag */
	var itemXRayEnable
		get() = itemEnable is Item.XRAY
		set(value) {
			itemEnable = if(value) Item.XRAY() else null
		}
	/** X-RAY time counter */
	private var itemXRayCount
		get() = (itemEnable as? Item.XRAY)?.time?:0
		set(value) {
			if(itemEnable is Item.XRAY) (itemEnable as? Item.XRAY)?.time = value
		}//; private set
	/** Color-block enable flag */
	var itemColorEnable
		get() = itemEnable is Item.COLOR
		set(value) {
			itemEnable = if(value) Item.COLOR() else null
		}
	/** Color-block counter */
	private var itemColorCount
		get() = (itemEnable as? Item.COLOR)?.time?:0
		set(value) {
			if(itemEnable is Item.COLOR) (itemEnable as? Item.COLOR)?.time = value
		}//; private set

	/** Gameplay-interruptable inum */
	var interruptItem:Item? = null

	/** Post-status of interruptable inum */
	private var interruptItemPreviousStat:Status = Status.MOVE//; private set

	/**Overwriting Rule settings(should not change from gamemode)*/
	var owTune = TuneConf()
	/** A button direction -1=Auto(Use rule settings) 0=Left 1=Right */
	var owSpinDir
		get() = owTune.spinDir
		set(value) {
			owTune.spinDir = value
		}
	/** Block Skin (-1=Auto -2=Random 0orAbove=Fixed) */
	var owSkin
		get() = owTune.skin
		set(value) {
			owTune.skin = value
		}
	/** Min DAS (-1=Auto 0orAbove=Fixed) */
	var owMinDAS
		get() = owTune.minDAS
		set(value) {
			owTune.minDAS = value
		}
	/** Max DAS (-1=Auto 0orAbove=Fixed) */
	var owMaxDAS
		get() = owTune.maxDAS
		set(value) {
			owTune.maxDAS = value
		}
	/** ARR (-1=Auto 0orAbove=Fixed) */
	var owARR
		get() = owTune.owARR
		set(value) {
			owTune.owARR = value
		}
	/** SoftDrop Speed -1(Below 0)=Auto
	 * 0-6=Always Fixed 0.5/1/2/3/4/5/20G
	 * 7-22 =Always Multiply *5-*20  */
	var owSDSpd
		get() = owTune.owSDSpd
		set(value) {
			owTune.owSDSpd = value
		}
	/** Reverse roles of up/down keys in-game */
	var owReverseUpDown
		get() = owTune.reverseUpDown xor itemQueue.any {it is Item.REV_CTRL_V}
		set(value) {
			owTune.reverseUpDown = value
		}
	/** Diagonal move (-1=Auto 0=Disable 1=Enable) */
	var owMoveDiagonal
		get() = owTune.moveDiagonal
		set(value) {
			owTune.moveDiagonal = value
		}
	/** Outline type (-1:Auto 0orAbove:Fixed) */
	var owBlockOutlineType
		get() = owTune.blockOutlineType
		set(value) {
			owTune.blockOutlineType = value
		}
	/** Show outline only flag
	 * (-1:Auto 0:Always Normal 1:Always Outline Only) */
	var owBlockShowOutlineOnly
		get() = owTune.blockShowOutlineOnly
		set(value) {
			owTune.blockShowOutlineOnly = value
		}
	/** ARE Canceling
	 * (-1:Rule 1:Move 2:Spin 4:Hold)*/
	var owDelayCancel
		get() = owTune.delayCancel
		set(value) {
			owTune.delayCancel = value
		}

	/** Clear mode selection */
	var clearMode:ClearType = Line
	/** Size needed for a cint-group clear */
	var colorClearSize = 0
		set(value) {
			when(clearMode) {
				is Color -> (clearMode as Color).colorClearSize = value
				is ColorStraight -> (clearMode as ColorStraight).colorClearSize = value
				is ColorGem -> (clearMode as ColorGem).colorClearSize = value
			}
			field = value
		}
	/** If true, cint clears will also clear adjacent garbage blocks. */
	var garbageColorClear = false
		set(value) {
			when(clearMode) {
				is Color -> (clearMode as Color).garbageColorClear = value
				is ColorGem -> (clearMode as ColorGem).garbageColorClear = value
			}
			field = value
		}
	/** If true, each individual block is a random cint. */
	var randomBlockColor = false
	/** If true, block in pieces are connected. */
	var connectBlocks = false
	/** List of block colors to use for random block colors. */
	var blockColors = BLOCK_COLORS_DEFAULT
	/** Number of colors in blockColors to use. */
	var numColors = 0
	/** If true, line cint clears can be diagonal. */
	var lineColorDiagonals = false
		set(value) {
			if(clearMode is ColorStraight) (clearMode as ColorStraight).lineColorDiagonals = value
			field = value
		}

	/** If true, gems count as the same cint as
	 * their respectively-colored normal blocks */
	var gemSameColor = false
		set(value) {
			if(clearMode is Color) (clearMode as Color).gemSameColor = value
			else if(clearMode is ColorStraight) (clearMode as ColorStraight).gemSameColor = value
			field = value
		}

	/** Delay for each step in cascade animations */
	var cascadeDelay = 0
	/** Delay between landing and checking for clears in cascade */
	var cascadeClearDelay = 0
	/** If true, cint clears will ignore hidden rows */
	var ignoreHidden = false
		set(value) {
			when(clearMode) {
				is Color -> (clearMode as Color).ignoreHidden = value
				is ColorGem -> (clearMode as ColorGem).ignoreHidden = value
			}
			field = value
		}
	/** Set to true to process rainbow block effects, false to skip. */
	var rainbowAnimate = false
	/** If true, the game will execute double rotation to I2 piece
	 * when regular rotation fails twice */
	var dominoQuickTurn = false

	/** 0 = default, 1 = link by cint,
	 * 2 = link by cint but ignore links for cascade (Avalanche) */
	var sticky = 0

	/** Hanabi発生間隔 */
	var tempHanabi = 0
	var intHanabi = 0; private set

	var explodSize = EXPLOD_SIZE_DEFAULT

	internal val playerProp = ProfileProperties(
		when(owner.mode?.gameIntensity) {
			-1 -> EventReceiver.COLOR.PINK
			1 -> EventReceiver.COLOR.GREEN
			2 -> EventReceiver.COLOR.YELLOW
			3 -> EventReceiver.COLOR.RED
			else -> EventReceiver.COLOR.BLUE
		}
	)

	internal var playerName = ""

	/** Current AREの値を取得 (ルール設定も考慮）*/
	val are
		get() = if(speed.are<ruleOpt.minARE&&ruleOpt.minARE>=0) ruleOpt.minARE
		else if(speed.are>ruleOpt.maxARE&&ruleOpt.maxARE>=0) ruleOpt.maxARE else speed.are
	/** Current ARE after line clearの値を取得 (ルール設定も考慮）*/
	val areLine
		get() = if(speed.areLine<ruleOpt.minARELine&&ruleOpt.minARELine>=0) ruleOpt.minARELine
		else if(speed.areLine>ruleOpt.maxARELine&&ruleOpt.maxARELine>=0) ruleOpt.maxARELine else speed.areLine
	/** Current Line clear timeの値を取得 (ルール設定も考慮）*/
	val lineDelay
		get() = if(speed.lineDelay<ruleOpt.minLineDelay&&ruleOpt.minLineDelay>=0) ruleOpt.minLineDelay
		else if(speed.lineDelay>ruleOpt.maxLineDelay&&ruleOpt.maxLineDelay>=0) ruleOpt.maxLineDelay else speed.lineDelay
	/** Current 固定 timeの値を取得 (ルール設定も考慮）*/
	val lockDelay
		get() = if(speed.lockDelay<ruleOpt.minLockDelay&&ruleOpt.minLockDelay>=0) ruleOpt.minLockDelay
		else if(speed.lockDelay>ruleOpt.maxLockDelay&&ruleOpt.maxLockDelay>=0) ruleOpt.maxLockDelay else speed.lockDelay

	/** Current DASの値を取得 (ルール設定も考慮）*/
	val das
		get() = if(speed.das<owMinDAS&&owMinDAS>=0) owMinDAS
		else if(speed.das>owMaxDAS&&owMaxDAS>=0) owMaxDAS
		else if(speed.das<ruleOpt.minDAS&&ruleOpt.minDAS>=0) ruleOpt.minDAS
		else if(speed.das>ruleOpt.maxDAS&&ruleOpt.maxDAS>=0) ruleOpt.maxDAS else speed.das

	/** Current SoftDropの形式を取得 (ルール設定も考慮）
	 * @return false:固定値 true:倍率
	 */
	val sdMul get() = if(owSDSpd<0) ruleOpt.softdropMultiplyNativeSpeed else owSDSpd>=SDS_FIXED.size

	/** Current SoftDrop速度を取得 (ルール設定も考慮）*/
	val softDropSpd
		get() = when {
			owSDSpd<0 -> ruleOpt.softdropSpeed
			else -> (if(owSDSpd<SDS_FIXED.size) SDS_FIXED[owSDSpd] else owSDSpd-SDS_FIXED.size+5f)
		}*(if(sdMul||speed.denominator<=0) speed.gravity else speed.denominator).toFloat()

	/** Controller.BUTTON_UP if controls are normal,
	 * Controller.BUTTON_DOWN if up/down are reversed
	 */
	val up get() = if(owReverseUpDown) Controller.BUTTON_DOWN else Controller.BUTTON_UP
	/** Controller.BUTTON_DOWN if controls are normal,
	 * Controller.BUTTON_UP if up/down are reversed
	 */
	val down get() = if(owReverseUpDown) Controller.BUTTON_UP else Controller.BUTTON_DOWN

	/** @return Current 横移動速度を取得*/
	val dasDelay get() = if(owARR>=0) owARR else ruleOpt.dasARR
	/** 現在使用中のBlockスキン numberを取得*/
	val blkSkin get() = if(owSkin>=0) owSkin else ruleOpt.skin

	/** @return A buttonを押したときに左rotationするならfalse, 右rotationするならtrue*/
	val spinDirection get() = if(owSpinDir>=0) owSpinDir!=0 else ruleOpt.spinToRight

	/** Is diagonal movement enabled?*/
	val isDiagonalMoveEnabled get() = if(owMoveDiagonal>=0) owMoveDiagonal==1 else ruleOpt.moveDiagonal

	/** 横移動 input のDirectionを取得
	 * @return -1:左 0:なし 1:右
	 */
	val moveDirection
		get() = (if(itemQueue.any {it is Item.REV_CTRL_H}) -1 else 1)*

			if(ruleOpt.moveLeftAndRightAllow&&ctrl.isPress(Controller.BUTTON_LEFT)&&ctrl.isPress(Controller.BUTTON_RIGHT)) {
				when {
					ctrl.buttonTime[Controller.BUTTON_LEFT]>ctrl.buttonTime[Controller.BUTTON_RIGHT] ->
						if(ruleOpt.moveLeftAndRightUsePreviousInput) -1 else 1
					ctrl.buttonTime[Controller.BUTTON_LEFT]<ctrl.buttonTime[Controller.BUTTON_RIGHT] ->
						if(ruleOpt.moveLeftAndRightUsePreviousInput) 1 else -1
					else -> 0
				}
			} else if(ctrl.isPress(Controller.BUTTON_LEFT)) -1 else if(ctrl.isPress(Controller.BUTTON_RIGHT)) 1 else 0

	/** @return 移動 count制限を超過したらtrue*/
	val isMoveCountExceed
		get() = if(ruleOpt.lockResetLimitShareCount) {
			extendedMoveCount+extendedSpinCount>=ruleOpt.lockResetMoveLimit&&ruleOpt.lockResetMoveLimit>=0
		} else ruleOpt.lockResetMoveLimit in 0..extendedMoveCount

	/** @return 回転 count制限を超過したらtrue*/
	val isSpinCountExceed
		get() = if(ruleOpt.lockResetLimitShareCount) {
			extendedMoveCount+extendedSpinCount>=ruleOpt.lockResetMoveLimit&&ruleOpt.lockResetMoveLimit>=0
		} else ruleOpt.lockResetSpinLimit in 0..extendedSpinCount

	/** @return ホールド可能ならtrue */
	val isHoldOK
		get() = (ruleOpt.holdEnable&&!holdDisable&&(holdUsedCount<ruleOpt.holdLimit||ruleOpt.holdLimit<0)
			&&!initialHoldContinuousUse)

	val canARECancelMove get() = if(owDelayCancel>=0) (owDelayCancel and 1)>0 else ruleOpt.areCancelMove
	val canLineCancelMove get() = if(owDelayCancel>=0) (owDelayCancel and 2)>0 else ruleOpt.lineCancelMove
	val canARECancelSpin get() = if(owDelayCancel>=0) (owDelayCancel and 4)>0 else ruleOpt.areCancelSpin
	val canLineCancelSpin get() = if(owDelayCancel>=0) (owDelayCancel and 8)>0 else ruleOpt.lineCancelSpin
	val canARECancelHold get() = if(owDelayCancel>=0) (owDelayCancel and 16)>0 else ruleOpt.areCancelHold
	val canLineCancelHold get() = if(owDelayCancel>=0) (owDelayCancel and 32)>0 else ruleOpt.lineCancelMove

	/** READY前のInitialization */
	fun init() {
		log.debug("GameEngine init playerID:${playerID}")
		field.reset()
		statistics.reset()
		speed.reset()
		playerProp.reset()
		gCount = 0
		owner.recordProp.load(owner.recorder(ruleOpt.strRuleName))
		replayData = ReplayData()

		if(!owner.replayMode) {
			versionMajor = GameManager.versionMajor
			versionMinor = GameManager.versionMinor
			versionMinorOld = GameManager.versionMinorOld

			val tempRand = Random.Default
			randSeed = tempRand.nextLong()
			random = Random(randSeed)
			if(owSkin<=-2) owSkin = tempRand.nextInt(owner.receiver.skinMax)
		} else {
			versionMajor = owner.replayProp.getProperty("version.core.major", 0f)
			versionMinor = owner.replayProp.getProperty("version.core.minor", 0)
			versionMinorOld = owner.replayProp.getProperty("version.core.minor", 0f)

			replayData.readProperty(owner.replayProp, playerID)

			owTune = try {
				Json.decodeFromString(owner.replayProp.getProperty("$playerID.tuning", "{}"))
			} catch(e:Exception) {
				log.warn(e)
				TuneConf(
					owner.replayProp.getProperty("$playerID.tuning.owRotateButtonDefaultRight", -1),
					owner.replayProp.getProperty("$playerID.tuning.owSkin", -1),
					owner.replayProp.getProperty("$playerID.tuning.owMinDAS", -1),
					owner.replayProp.getProperty("$playerID.tuning.owMaxDAS", -1),
					owner.replayProp.getProperty("$playerID.tuning.owDelayCancel", -1),
					owner.replayProp.getProperty("$playerID.tuning.owDasDelay", -1),
					owner.replayProp.getProperty("$playerID.tuning.owSDSpd", -1),
					owner.replayProp.getProperty("$playerID.tuning.owReverseUpDown", false),
					owner.replayProp.getProperty("$playerID.tuning.owMoveDiagonal", -1),
					owner.replayProp.getProperty("$playerID.tuning.owBlockOutlineType", -1),
					owner.replayProp.getProperty("$playerID.tuning.owBlockShowOutlineOnly", -1)
				)
			}

			randSeed = owner.replayProp.getProperty("$playerID.replay.randSeed", 16L)
			random = Random(randSeed)
		}
		quitFlag = false

		owner.musMan.bgm = BGM.Menu(4+(owner.mode?.gameIntensity?:0))
		stat = Status.SETTING
		statc.fill(0)

		lastEvent = null
		lastLinesY = emptySet()
		lastLineY = fieldHeight

		isInGame = false
		gameActive = false
		timerActive = false
		gameStarted = false
		replayTimer = 0

		nowPieceObject = null
		nowPieceX = 0
		nowPieceY = 0
		nowPieceBottomY = 0
		nowPieceColorOverride = null

		nextPieceArraySize = 1400
		nextPieceEnable = List(Piece.PIECE_COUNT) {it<Piece.PIECE_STANDARD_COUNT}
		nextPieceArrayID = emptyList()
		nextPieceArrayObject = emptyList()
		nextPieceCount = 0

		holdPieceObject = null
		holdDisable = false
		holdUsedCount = 0

		lineClearing = 0
		garbageClearing = 0
		lineGravityType = LineGravity.Native
		chain = 0
		lineGravityTotalLines = 0

		lockDelayNow = 0

		dasCount = 0
		dasDirection = 0
		dasSpeedCount = dasDelay
		dasRepeat = false
		dasInstant = false
		shiftLock = 0

		initialSpinDirection = 0
		initialSpinLastDirection = 0
		initialHoldFlag = false
		initialSpinContinuousUse = false
		initialHoldContinuousUse = false

		finesseCombo = 0
		nowPieceSteps = 0
		nowPieceMoveCount = 0
		nowPieceSpinCount = 0
		nowPieceSpinFailCount = 0

		extendedMoveCount = 0
		extendedSpinCount = 0

		nowWallkickCount = 0
		nowWallkickRiseCount = 0

		softdropFall = 0
		harddropFall = 0
		softdropContinuousUse = false
		harddropContinuousUse = false

		manualLock = false

		lastMove = LastMove.NONE
		split = false
		twistType = null
		b2bCount = -1
		combo = -1
		intHanabi = 0
		tempHanabi = 0

		twistEnable = false
		twistEnableEZ = false
		twistAllowKick = true
		useAllSpinBonus = false
		b2bEnable = false
		splitB2B = false
		comboType = COMBO_TYPE_DISABLE

		blockHidden = -1
		blockHiddenAnim = true
		blockOutlineType = BLOCK_OUTLINE_NORMAL
		blockShowOutlineOnly = false

		heboHiddenEnable = false
		heboHiddenTimerNow = 0
		heboHiddenTimerMax = 0
		heboHiddenYNow = 0
		heboHiddenYLimit = 0

		delayCancel = false
		delayCancelMoveLeft = false
		delayCancelMoveRight = false

		bone = false
		gemRate = 0f

		big = false
		bigMove = true
		bigHalf = true

		kicked = false

		fieldWidth = -1
		fieldHeight = -1
		fieldHiddenHeight = -1

		ending = 0
		staffrollEnable = false
		staffrollNoDeath = false
		staffrollEnableStatistics = false

		frameSkin = FRAME_COLOR_WHITE

		readyStart = 0
		readyEnd = READY_GO_TIME.first-1
		goStart = READY_GO_TIME.first
		goEnd = READY_GO_TIME.second

		readyDone = false

		lives = 0
		recoveryFlag = false

		ghost = true

		meterValue = 0f
		meterColor = METER_COLOR_LEVEL

		lagARE = false
		lagStop = false
		displaySize = if(playerID>=2) -1 else 0

		enableSE = true
		dieAll = true

		isNextVisible = true
		isHoldVisible = true
		isVisible = true

		holdButtonNextSkip = false

		allowTextRenderByReceiver = true

		itemQueue.clear()
		itemRollRollInterval = 30

		itemXRayCount = 0

		itemColorCount = 0

		interruptItem = null

		clearMode = Line
		colorClearSize = -1
		garbageColorClear = false
		ignoreHidden = false
		connectBlocks = true
		lineColorDiagonals = false
		blockColors = BLOCK_COLORS_DEFAULT
		cascadeDelay = 0
		cascadeClearDelay = 0

		rainbowAnimate = false

		startTime = 0
		endTime = 0

		//  event 発生
		owner.mode?.let {
			it.playerInit(this)
			if(owner.replayMode) it.loadReplay(this, owner.replayProp)
			else {
				it.loadRanking(owner.recordProp)
				it.ranking.load(owner.recorder(ruleOpt.strRuleName)+".lb")
				if(playerProp.isLoggedIn) it.loadRankingPlayer(playerProp)
			}
		}
		playerName = if(owner.replayMode) owner.replayProp.getProperty("$playerID.playerName", "") else ""
		owner.receiver.playerInit(this)
		ai?.shutdown()
		ai?.init(this, playerID)
		stopSE("danger")
	}

	/** 終了処理 */
	fun shutdown() {
		ai?.shutdown()
	}
	/** ステータス counterInitialization */
	fun resetStatc() {
		for(i in statc.indices) statc[i] = 0
	}

	/** Sound effect [name]を再生する (enableSEがtrueのときだけ）
	 * @param name Sound effectsのName
	 */
	fun playSE(name:String, freq:Float = 1f, vol:Float = 1f) {
		if(enableSE) owner.receiver.playSE(name, freq, vol)
	}

	fun loopSE(name:String, freq:Float = 1f, vol:Float = 1f) {
		if(enableSE) owner.receiver.loopSE(name, freq, vol)
	}

	fun stopSE(name:String) = owner.receiver.stopSE(name)

	/** @return NEXTピース[c]番目のID*/
	fun getNextID(c:Int):Int =
		if(nextPieceArrayObject.isEmpty()) Piece.PIECE_NONE else nextPieceArrayID[c%nextPieceArrayID.size]

	/** @return NEXTピース[c]番目のオブジェクト */
	fun getNextObject(c:Int):Piece? = try {
		if(nextPieceArrayObject.isEmpty()) null else nextPieceArrayObject[c%nextPieceArrayObject.size]
	} catch(_:Exception) {
		null
	}
	/** NEXTピース[c]番目のオブジェクトコピーを取得*/
	fun getNextObjectCopy(c:Int):Piece? = getNextObject(c)?.let {Piece(it)}

	/** 見え／消えRoll 状態のfieldを通常状態に戻す */
	fun resetFieldVisible() {
		field.let {f ->
			for(x in 0..<f.width) for(y in 0..<f.height)
				f.getBlock(x, y)?.run {
					if(color!=null) {
						alpha = 1f
						darkness = 0f
						setAttribute(true, Block.ATTRIBUTE.VISIBLE)
						setAttribute(true, Block.ATTRIBUTE.OUTLINE)
					}
				}
		}
	}

	/** ソフト・Hard drop・先行ホールド・同方向への先行rotationの使用制限解除 */
	private fun checkDropContinuousUse() {
		if(gameActive) {
			if(!ctrl.isPress(down)||ruleOpt.softdropLimit<0) softdropContinuousUse = false
			if(!ctrl.isPress(up)||ruleOpt.harddropLimit<0) harddropContinuousUse = false
			if(!ctrl.isPress(Controller.BUTTON_D)||!ruleOpt.holdInitialLimit) initialHoldContinuousUse = false
			if(initialSpinContinuousUse) {
				val dir = getSpinOperation()
				if(!ruleOpt.spinInitialLimit||initialSpinLastDirection!=dir||dir==0) initialSpinContinuousUse = false
			}
		}
	}

	/** 横溜め処理 */
	fun padRepeat() {
		if(moveDirection!=0) dasCount++
		else {
			dasWall = false
			if(!ruleOpt.dasStoreChargeOnNeutral) dasCount = 0
		}
		dasDirection = moveDirection
	}

	/** Called if delay doesn't allow charging but dasRedirectInDelay == true
	 * Updates dasDirection so player can change direction without dropping
	 * charge on entry. */
	fun dasRedirect() {
		dasDirection = moveDirection
	}

	/** Twister routine
	 * @param x X coord
	 * @param y Y coord
	 * @param p Current p object
	 * @param f Field object
	 * @param all when false, T-Piece only
	 */
	private fun checkTwisted(x:Int, y:Int, p:Piece?, f:Field?, all:Boolean = true):Twister? {
		p?:return null
		f?:return null
		if(!all&&p.type!=Piece.Shape.T) return null
		if(!twistAllowKick&&kicked) return null
		val m = if(p.big) 2 else 1
		var res:Twister? = null
		if(p.checkCollision(x, y-m, f)&&p.checkCollision(x+m, y, f)
			&&p.checkCollision(x-m, y, f)
		) {
			res = Twister.IMMOBILE
			val copyField = Field().apply {replace(f)}
			p.placeToField(x, y, copyField)
			if(p.height+1!=copyField.checkLine(false)&&kicked) res = Twister.IMMOBILE_MINI
			if(copyField.checkLine(false)==1&&kicked) res = Twister.IMMOBILE_MINI
		} else if(twistEnableEZ&&kicked&&p.checkCollision(x, y+m, f))
			res = Twister.IMMOBILE_EZ

		if(p.type==Piece.Shape.T) {
			// Setup 4-point coordinates
			val tx = (if(p.big) listOf(1, 4, 1, 4) else listOf(0, 2, 0, 2)).map {
				it+ruleOpt.pieceOffsetX[p.id][p.direction]*(if(p.big) 2 else 1)
			}
			val ty = (if(p.big) listOf(1, 1, 4, 4) else listOf(0, 0, 2, 2)).map {
				it+ruleOpt.pieceOffsetY[p.id][p.direction]*(if(p.big) 2 else 1)
			}
			// Check the corner of the T p
			if(tx.indices.count {!f.getBlockEmpty(x+tx[it], y+ty[it])}>=3) res = Twister.POINT
			else if(p.checkCollision(x, y, getSpinDirection(-1), f)
				&&p.checkCollision(x, y, getSpinDirection(1), f)
			) res = Twister.POINT_MINI
		} else {
			val offsetX = ruleOpt.pieceOffsetX[p.id][p.direction]
			val offsetY = ruleOpt.pieceOffsetY[p.id][p.direction]
			if(!p.big)
				for(i in 0..<Piece.SPINBONUSDATA_HIGH_X[p.id][p.direction].size/2) {
					val isHighSpot1 = (!f.getBlockEmpty(
						x+Piece.SPINBONUSDATA_HIGH_X[p.id][p.direction][i*2]+offsetX,
						y+Piece.SPINBONUSDATA_HIGH_Y[p.id][p.direction][i*2]+offsetY, false
					))
					val isHighSpot2 = (!f.getBlockEmpty(
						x+Piece.SPINBONUSDATA_HIGH_X[p.id][p.direction][i*2+1]+offsetX,
						y+Piece.SPINBONUSDATA_HIGH_Y[p.id][p.direction][i*2+1]+offsetY, false
					))
					val isLowSpot1 = (!f.getBlockEmpty(
						x+Piece.SPINBONUSDATA_LOW_X[p.id][p.direction][i*2]+offsetX,
						y+Piece.SPINBONUSDATA_LOW_Y[p.id][p.direction][i*2]+offsetY, false
					))
					val isLowSpot2 = (!f.getBlockEmpty(
						x+Piece.SPINBONUSDATA_LOW_X[p.id][p.direction][i*2+1]+offsetX,
						y+Piece.SPINBONUSDATA_LOW_Y[p.id][p.direction][i*2+1]+offsetY, false
					))

					//log.debug(isHighSpot1 + "," + isHighSpot2 + "," + isLowSpot1 + "," + isLowSpot2);

					if(isHighSpot1&&isHighSpot2&&(isLowSpot1||isLowSpot2))
						res = Twister.POINT
					else if(isLowSpot1&&isLowSpot2&&(isHighSpot1||isHighSpot2))
						res = Twister.POINT_MINI
				}
		}
		return res
	}

	/** @return [piece]が[field]に出現するX-coordinate */
	fun getSpawnPosX(piece:Piece?, fld:Field = field):Int =
		((fld.width-1-(piece?.width?:0))/2).let {x -> x+(big&&bigMove&&x%2!=0).toInt()}+(piece?.let {p ->
			if(big) ruleOpt.pieceSpawnXBig[p.id][p.direction] else ruleOpt.pieceSpawnX[p.id][p.direction]
		}?:0)

	/** @return [piece]が出現するX-coordinate */
	fun getSpawnPosX(piece:Piece?):Int = getSpawnPosX(piece, field)

	/** @return [piece]が[field]に出現するY-coordinate */
	fun getSpawnPosY(piece:Piece?, fld:Field? = field):Int {
		val y = getSpawnPosY(piece)
		if (fld==null) return y
		var p = 0
		while(piece?.checkCollision(getSpawnPosX(piece, fld), y-p, fld)==true&&p<ruleOpt.pieceEnterMaxDistanceY) {
			p++
		}
		return y-p
	}

	/** @return [piece]が出現するX-coordinate */
	fun getSpawnPosY(piece:Piece?):Int {
		var y = 0
		piece?.let {
			if(ruleOpt.pieceEnterAboveField&&!ruleOpt.fieldCeiling) {
				y = -1-it.maximumBlockY
				if(big) y--
			} else y = -it.minimumBlockY

			y += if(big) ruleOpt.pieceSpawnYBig[it.id][it.direction]
			else ruleOpt.pieceSpawnY[it.id][it.direction]
		}
		return y
	}

	/** @return rotation buttonを押したあとのピースのDirection
	 * @param move rotationDirection (-1:左 1:右 2:180度）
	 */
	fun getSpinDirection(move:Int):Int = ((nowPieceObject?.direction?:0)+move).pythonModulo(4)

	/** @return rotation buttonによる回転方向
	 */
	fun getSpinOperation(it:Controller = ctrl):Int =
		(if(ruleOpt.spinReverseKey) -1 else 1).let {rev ->
			(if(owSpinDir==1||owSpinDir<0&&ruleOpt.spinToRight) 1 else -1)*if(ruleOpt.spinDoubleKey) {
				(it.isPress(Controller.BUTTON_E).toInt()*2)+
					(it.isPressCount(Controller.BUTTON_B, Controller.BUTTON_F)*rev)+
					it.isPressCount(Controller.BUTTON_A, Controller.BUTTON_C)
			} else if(it.isPressAny(Controller.BUTTON_A, Controller.BUTTON_C, Controller.BUTTON_E)) 1
			else if(it.isPressAny(Controller.BUTTON_B, Controller.BUTTON_F)) rev
			else 0
		}

	/** 先行rotationと先行ホールドの処理 */
	private fun initialSpin():Int =
		ctrl.let {
			if(it.isPress(Controller.BUTTON_D)&&ruleOpt.holdInitial&&isHoldOK) {
				initialHoldFlag = true
				initialHoldContinuousUse = true
				if(frameSkin!=FRAME_SKIN_SG) playSE("initialhold")
			}
			if(ruleOpt.spinInitial&&(!ruleOpt.spinInitialLimit||!initialSpinContinuousUse)) {
				val dir = getSpinOperation(it)
				if(dir!=0) spun = false
				initialSpinDirection = dir
				dir
			} else 0
		}

	/** fieldのBlock stateを更新 */
	private fun fieldUpdate() {
		val outlineOnly =  // Show outline only flag
			if(owBlockShowOutlineOnly>=0) owBlockShowOutlineOnly>0 else blockShowOutlineOnly

		field.let {f ->
			for(i in 0..<f.width)
				for(j in f.hiddenHeight*-1..<f.height) {
					f.getBlock(i, j)?.run {
						if(elapsedFrames<0) {
//							if(!getAttribute(Block.ATTRIBUTE.GARBAGE)) darkness = 0f
						} else if(elapsedFrames<ruleOpt.lockFlash) {
							darkness = -.8f
							if(outlineOnly) {
								setAttribute(true, Block.ATTRIBUTE.OUTLINE)
								setAttribute(false, Block.ATTRIBUTE.VISIBLE)
								setAttribute(false, Block.ATTRIBUTE.BONE)
							}
						} else {
							//after lockflash
							darkness = .18f
							setAttribute(true, Block.ATTRIBUTE.OUTLINE)
							if(outlineOnly) {
								setAttribute(false, Block.ATTRIBUTE.VISIBLE)
								setAttribute(false, Block.ATTRIBUTE.BONE)
							}
						}

						if(blockHidden!=-1&&elapsedFrames>=blockHidden-10&&gameActive) {
							if(blockHiddenAnim) {
								alpha -= .1f
								if(alpha<0f) alpha = 0f
							}

							if(elapsedFrames>=blockHidden) {
								alpha = 0f
								setAttribute(false, Block.ATTRIBUTE.OUTLINE)
								setAttribute(false, Block.ATTRIBUTE.VISIBLE)
							}
						}

						if(elapsedFrames>=0) elapsedFrames++
					}
				}

			// X-RAY
			if(gameActive) {
				if(itemXRayEnable) {
					for(i in 0..<f.width)
						for(j in f.hiddenHeight*-1..<f.height) {
							f.getBlock(i, j)?.apply {
								setAttribute(itemXRayCount%36==i, Block.ATTRIBUTE.VISIBLE)
								setAttribute(itemXRayCount%36==i, Block.ATTRIBUTE.OUTLINE)
							}
						}
					itemXRayCount++
				} else itemXRayCount = 0

				// COLOR
				if(itemColorEnable) {
					for(i in 0..<f.width)
						for(j in f.hiddenHeight*-1..<f.height) {
							var bright = minOf(j, 10)
							if(bright>=5) bright = 9-bright
							bright = 40-((20-i+bright)*4+itemColorCount)%40
							if(bright in ITEM_COLOR_BRIGHT_TABLE.indices) bright = 10-ITEM_COLOR_BRIGHT_TABLE[bright]

							f.getBlock(i, j)?.apply {
								alpha = bright*.1f
								setAttribute(false, Block.ATTRIBUTE.OUTLINE)
								setAttribute(true, Block.ATTRIBUTE.VISIBLE)
							}
						}
					itemColorCount++
				} else itemColorCount = 0
			}

			// ヘボHIDDEN
			if(heboHiddenEnable) {
				heboHiddenTimerNow++
				if(heboHiddenTimerNow>heboHiddenTimerMax) {
					heboHiddenTimerNow = 0
					heboHiddenYNow++
					if(heboHiddenYNow>heboHiddenYLimit) heboHiddenYNow = heboHiddenYLimit
				}
			}
		}
	}

	/** Called when saving replay */
	fun saveReplay() {
		if(owner.replayMode&&!owner.replayRerecord) return

		owner.replayProp.setProperty("version.core", "$versionMajor.$versionMinor")
		owner.replayProp.setProperty("version.core.major", versionMajor)
		owner.replayProp.setProperty("version.core.minor", versionMinor)
		owner.replayProp.setProperty("version.core.dev", versionIsDevBuild)

		owner.replayProp.setProperty("$playerID.replay.randSeed", randSeed)
		statistics.rule = ruleOpt.strRuleName
		replayData.writeProperty(owner.replayProp, playerID, replayTimer)
		statistics.writeProperty(owner.replayProp, playerID)
		owner.replayProp.setProperty("$playerID.rule", Json.encodeToString(ruleOpt))
		//ruleOpt.writeProperty(owner.replayProp, playerID)

		if(playerID==0) {
			owner.mode?.let {owner.replayProp.setProperty("name.mode", it.id)}
			owner.replayProp.setProperty("name.rule", ruleOpt.strRuleName)

			// Local timestamp
			val time = Calendar.getInstance()
			val month = time.get(Calendar.MONTH)+1
			val strDate = "%04d/%02d/%02d".format(time.get(Calendar.YEAR), month, time.get(Calendar.DATE))
			val strTime =
				"%02d:%02d:%02d".format(time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), time.get(Calendar.SECOND))
			owner.replayProp.setProperty("timestamp.date", strDate)
			owner.replayProp.setProperty("timestamp.time", strTime)

			// GMT timestamp
			owner.replayProp.setProperty("timestamp.gmt", GeneralUtil.nowGMT)
		}
		if(playerProp.isLoggedIn)
			owner.replayProp.setProperty("$playerID.playerName", playerProp.nameDisplay)

		owner.replayProp.setProperty("$playerID.tuning.owRotateButtonDefaultRight", owSpinDir)
		owner.replayProp.setProperty("$playerID.tuning.owSkin", owSkin)
		owner.replayProp.setProperty("$playerID.tuning.owMinDAS", owMinDAS)
		owner.replayProp.setProperty("$playerID.tuning.owMaxDAS", owMaxDAS)
		owner.replayProp.setProperty("$playerID.tuning.owDasDelay", owARR)
		owner.replayProp.setProperty("$playerID.tuning.owSDSpd", owSDSpd)
		owner.replayProp.setProperty("$playerID.tuning.owReverseUpDown", owReverseUpDown)
		owner.replayProp.setProperty("$playerID.tuning.owMoveDiagonal", owMoveDiagonal)
		owner.replayProp.setProperty("$playerID.tuning.owDelayCancel", owDelayCancel)

		owner.replayProp.setProperty("$playerID.tuning.owBlockOutlineType", owBlockOutlineType)
		owner.replayProp.setProperty("$playerID.tuning.owBlockShowOutlineOnly", owBlockShowOutlineOnly)

		owner.mode?.let {
			it.saveSetting(this, owner.replayProp)
			owner.saveModeConfig()
			if(it.saveReplay(this, owner.replayProp)) {
				it.saveRanking()
				it.ranking.save()
				owner.recordProp.save()
				if(playerProp.isLoggedIn) {
					it.saveRankingPlayer(playerProp)
					playerProp.saveProfileConfig()
				}
			}
		}
	}

	/** fieldエディット画面に入る処理 */
	fun enterFieldEdit() {
		mapEditPreviousStat = stat
		stat = Status.FIELDEDIT
		mapEditX = 0
		mapEditY = 0
		mapEditColor = Block.COLOR_WHITE
		mapEditFrames = 0
		owner.menuOnly = false
		createFieldIfNeeded()
	}

	/** fieldをInitialization (まだ存在しない場合） */
	fun createFieldIfNeeded():Field {
		if(fieldWidth<0) fieldWidth = ruleOpt.fieldWidth
		if(fieldHeight<0) fieldHeight = ruleOpt.fieldHeight
		if(fieldHiddenHeight<0) fieldHiddenHeight = ruleOpt.fieldHiddenHeight
		if(field.width==fieldWidth&&field.height==fieldHeight&&field.hiddenHeight==fieldHiddenHeight&&
			field.ceiling==ruleOpt.fieldCeiling
		) return field

		/*if(!gameActive&&!owner.replayMode) {
			val tempRand = Random.Default
			randSeed = tempRand.nextLong()
			random = Random(randSeed)
		}*/
		val new = Field(fieldWidth, fieldHeight, fieldHiddenHeight, ruleOpt.fieldCeiling)
		field = new
		return new
	}

	/** Call this if the game has ended */
	fun gameEnded() {
		if(endTime==0L) {
			endTime = System.nanoTime()
			statistics.gameRate = (replayTimer/(0.00000006*(endTime-startTime))).toFloat()
		}
		gameActive = false
		timerActive = false
		isInGame = false
		ai?.also {it.shutdown()}
	}

	/** ゲーム stateの更新 */
	fun update() {
		if(gameActive) {
			// リプレイ関連の処理
			if(!owner.replayMode||owner.replayRerecord) {
				// AIの button処理
				ai?.let {a ->
					if(!aiShowHint) ctrl.setButtonBit(a.setControl(this, playerID, ctrl))
					else {
						aiHintReady = a.thinkComplete||a.thinkCurrentPieceNo>0&&a.thinkCurrentPieceNo<=a.thinkLastPieceNo
						if(aiHintReady) {
							aiHintPiece = null
							if(a.bestHold) {
								holdPieceObject?.also {aiHintPiece = Piece(it)}?:run {
									getNextObjectCopy(nextPieceCount)?.also {
										if(!it.offsetApplied) it.applyOffsetArray(ruleOpt.pieceOffsetX[it.id], ruleOpt.pieceOffsetY[it.id])
									}
								}
							} else nowPieceObject?.let {aiHintPiece = Piece(it)}
						}
					}
				}
				replayData.setInputData(ctrl.buttonBit, replayTimer) // input 状態をリプレイに記録
			} else {
				ctrl.setButtonBit(replayData.getInputData(replayTimer)) // input 状態をリプレイから読み込み
			}
			replayTimer++
		}

		//  button input timeの更新
		ctrl.updateButtonTime()
		// 最初の処理
		owner.mode?.onFirst(this)
		owner.receiver.onFirst(this)

		if(stat!=Status.SETTING&&stat!=Status.PROFILE&&stat!=Status.NOTHING&&
			(!owner.replayMode||owner.replayRerecord)
		) ai?.onFirst(this, playerID)
		fpf = 0
		// 各ステータスの処理
		if(!lagStop)
			when(stat) {
				Status.SETTING -> statSetting()
				Status.PROFILE -> statProfile()
				Status.READY -> statReady()
				Status.MOVE -> {
					dasRepeat = true
					dasInstant = false
					while(dasRepeat) statMove()
				}
				Status.LOCKFLASH -> statLockFlash()
				Status.LINECLEAR -> statLineClear()
				Status.ARE -> statARE()
				Status.ENDINGSTART -> statEndingStart()
				Status.CUSTOM -> statCustom()
				Status.EXCELLENT -> statExcellent()
				Status.GAMEOVER -> statGameOver()
				Status.RESULT -> statResult()
				Status.FIELDEDIT -> statFieldEdit()
				Status.INTERRUPTITEM -> statInterruptItem()
				Status.NOTHING -> {
				}
			}

		// fieldのBlock stateや統計情報を更新
		fieldUpdate()
		//if(ending==0||staffrollEnableStatistics) statistics.update()

		// 最後の処理
		if(intHanabi>0) intHanabi--
		owner.mode?.onLast(this)
		owner.receiver.onLast(this)
		ai?.also {if(!owner.replayMode||owner.replayRerecord) it.onLast(this, playerID)}

		// Timer増加
		if(gameActive&&timerActive) statistics.time++
		if(tempHanabi>0&&intHanabi<=0) {
			owner.receiver.shootFireworks(this)
			tempHanabi--
			intHanabi += HANABI_INTERVAL
		}

		/* if(startTime > 0 && endTime == 0) {
		 * statistics.gamerate = (float)(replayTimer /
		 * (0.00000006*(System.nanoTime() - startTime)));
		 * } */
	}

	/** Draw the screen
	 * (各Mode や event 処理クラスの event を呼び出すだけで, それ以外にGameEngine自身は何もしません） */
	fun render() {
		// 最初の処理
		if(!owner.receiver.doesGraphicsExist) return
		owner.receiver.renderFirst(this) {owner.mode?.renderFirst(this)}

		if(rainbowAnimate) Block.updateRainbowPhase(this)

		// 各ステータスの処理
		when(stat) {
			Status.NOTHING -> {
			}
			Status.SETTING -> {
				owner.receiver.renderSetting(this)
				owner.mode?.renderSetting(this)
			}
			Status.PROFILE -> {
				owner.receiver.renderProfile(this)
				owner.mode?.renderProfile(this)
			}
			Status.READY -> {
				owner.receiver.renderReady(this)
				owner.mode?.renderReady(this)
			}
			Status.MOVE -> {
				owner.receiver.renderMove(this)
				owner.mode?.renderMove(this)
			}
			Status.LOCKFLASH -> {
				owner.receiver.renderLockFlash(this)
				owner.mode?.renderLockFlash(this)
			}
			Status.LINECLEAR -> {
				owner.receiver.renderLineClear(this)
				owner.mode?.renderLineClear(this)
			}
			Status.ARE -> {
				owner.receiver.renderARE(this)
				owner.mode?.renderARE(this)
			}
			Status.ENDINGSTART -> {
				owner.receiver.renderEndingStart(this)
				owner.mode?.renderEndingStart(this)
			}
			Status.CUSTOM -> {
				owner.receiver.renderCustom(this)
				owner.mode?.renderCustom(this)
			}
			Status.EXCELLENT -> {
				owner.receiver.renderExcellent(this)
				owner.mode?.renderExcellent(this)
			}
			Status.GAMEOVER -> {
				owner.receiver.renderGameOver(this)
				owner.mode?.renderGameOver(this)
			}
			Status.RESULT -> {
				owner.receiver.renderResult(this)
				owner.mode?.renderResult(this)
			}
			Status.FIELDEDIT -> {
				owner.receiver.renderFieldEdit(this)
				owner.mode?.renderFieldEdit(this)
			}
			Status.INTERRUPTITEM -> {
			}
		}

		if(owner.showInput) {
			owner.receiver.renderInput(this)
			owner.mode?.renderInput(this)
		}
		ai?.also {
			if(gameActive&&aiShowState) it.renderState(this, playerID)
			if(aiShowState||aiShowHint) it.renderHint(this, playerID)
		}

		// 最後の処理
		owner.mode?.renderLast(this)
		owner.receiver.renderLast(this)
	}

	/** 開始前の設定画面のときの処理 */
	private fun statSetting() {
		//  event 発生
		owner.musMan.fadeSW = false
		if(owner.mode?.onSetting(this)==true) return
		owner.receiver.onSetting(this)

		// Mode側が何もしない・決定したことでfalseを返した場合はReady画面へ移動
		stat = Status.READY
		if(playerProp.isLoggedIn) {
			owner.mode?.saveSetting(this, playerProp.propProfile)
			playerProp.saveProfileConfig()
		}
		owner.mode?.saveSetting(this, owner.modeConfig)
		owner.saveModeConfig()

		resetStatc()
	}

	/** 開始前の名前入力画面のときの処理 */
	private fun statProfile() {
		//  event 発生
		if(owner.mode?.onProfile(this)==true) return
		owner.receiver.onProfile(this)

		if(playerProp.loginScreen.updateScreen(this)) return
		// Mode側が何もしない場合は設定画面へ戻る
		stat = Status.SETTING
		resetStatc()
	}

	/** Ready→Goのときの処理 */
	private fun statReady() {
		//  event 発生
		if(owner.mode?.onReady(this)==true) return
		owner.receiver.onReady(this)

		// 横溜め
		if(ruleOpt.dasInReady&&gameActive) padRepeat()
		else if(ruleOpt.dasRedirectInDelay) dasRedirect()

		// Initialization
		if(statc[0]==0) {
			if(!readyDone&&!owner.musMan.fadeSW&&owner.musMan.bgm.id<0&&
				owner.musMan.bgm.id !in BGM.Finale(0).id..BGM.Finale(2).id
			)
				owner.musMan.fadeSW = true
			// fieldInitialization
			createFieldIfNeeded()
			// NEXTピース作成
			if(nextPieceArrayID.isEmpty()) {
				if(owner.replayMode) {
					randSeed = owner.replayProp.getProperty("$playerID.replay.randSeed", 16L)
					nextPieceArrayID = emptyList()
					nextPieceArrayObject = emptyList()
				}
				// 出現可能なピースが1つもない場合は全て出現できるようにする
				if(nextPieceEnable.all {!it}) nextPieceEnable = List(Piece.PIECE_COUNT) {true}

				nextPieceCount = 0
				// NEXTピースの出現順を作成
				random = Random(randSeed)
				randomizer.setState(nextPieceEnable, randSeed)

				nextPieceArrayID = List(nextPieceArraySize) {randomizer.next()}
				statistics.randSeed = randSeed
			}
			// NEXTピースのオブジェクトを作成
			if(nextPieceArrayObject.isEmpty()) {
				nextPieceArrayObject = List(nextPieceArrayID.size) {
					Piece(nextPieceArrayID[it]).also {p ->
						p.direction = ruleOpt.pieceDefaultDirection[p.id]
						if(p.direction>=Piece.DIRECTION_COUNT)
							p.direction = random.nextInt(Piece.DIRECTION_COUNT)
						p.connectBlocks = connectBlocks
						p.setColor(ruleOpt.pieceColor[p.id])
						p.setDarkness(0f)
						p.setSkin(blkSkin)
						p.setAttribute(true, Block.ATTRIBUTE.VISIBLE)
						p.setAttribute(bone, Block.ATTRIBUTE.BONE)
						p.placeNum = it

						if(randomBlockColor) {
							if(blockColors.size<numColors||numColors<1) numColors = blockColors.size
							val size = p.maxBlock
							p.setColor(List(size) {blockColors[random.nextInt(numColors)]})
							if(clearMode is ColorGem) p.block.forEach {b ->
								if(random.nextFloat()<=gemRate) b.type = Block.TYPE.GEM
							}
						}
						if(clearMode is LineBomb||clearMode is LineSpark)
							if(random.nextFloat()>=0.1f)
								p.block[random.nextInt(p.maxBlock)].type = Block.TYPE.GEM

						p.updateConnectData()
					}
				}
			}

			if(!readyDone) {
				//  button input状態リセット
				ctrl.reset()
				// ゲーム中 flagON
				gameActive = true
				gameStarted = true
				isInGame = true
			}
		}

		// READY音
		if(statc[0]==readyStart) playSE("start0")

		// GO音
		if(statc[0]==goStart) playSE("start1")

		// NEXTスキップ
		if(statc[0] in 1..<goEnd&&holdButtonNextSkip&&isHoldOK&&ctrl.isPush(Controller.BUTTON_D)) {
			if(frameSkin!=FRAME_SKIN_SG) playSE("initialhold")
			holdPieceObject = getNextObjectCopy(nextPieceCount)?.also {
				it.applyOffsetArray(ruleOpt.pieceOffsetX[it.id], ruleOpt.pieceOffsetY[it.id])
			}
			nextPieceCount++
			if(nextPieceCount<0) nextPieceCount = 0
		}

		// 開始
		if(statc[0]>=goEnd) {
			owner.mode?.startGame(this)
			owner.receiver.startGame(this)
			owner.musMan.fadeSW = false
			initialSpin()
			stat = Status.MOVE
			resetStatc()
			if(!readyDone) startTime = System.nanoTime()
			//startTime = System.nanoTime()/1000000L;
			readyDone = true
			return
		}

		statc[0]++
	}

	/** Blockピースの移動処理 */
	private fun statMove() {
		dasRepeat = false

		//  event 発生
		if(owner.mode?.onMove(this)==true) return
		owner.receiver.onMove(this)
//		val field = field ?: return
		// 横溜めInitialization
		val moveDirection = moveDirection

		if(statc[0]>0||ruleOpt.dasInMoveFirstFrame)
			if(dasDirection!=moveDirection) {
				dasDirection = moveDirection
				if(!(dasDirection==0&&ruleOpt.dasStoreChargeOnNeutral)) {
					dasWall = false
					dasCount = 0
				}
			}

		// 出現時の処理
		if(statc[0]==0) {
			if(statc[1]==0&&!initialHoldFlag) {
				// 通常出現
				nowPieceObject = getNextObjectCopy(nextPieceCount)
				nextPieceCount++
				if(nextPieceCount<0) nextPieceCount = 0
				holdDisable = false
			} else {
				// ホールド出現
				if(initialHoldFlag) {
					// 先行ホールド
					if(holdPieceObject==null) {
						// 1回目
						holdPieceObject = getNextObjectCopy(nextPieceCount)?.also {
							it.applyOffsetArray(ruleOpt.pieceOffsetX[it.id], ruleOpt.pieceOffsetY[it.id])
						}
						nextPieceCount++
						if(nextPieceCount<0) nextPieceCount = 0

						getNextObject(nextPieceCount+ruleOpt.nextDisplay-1)?.setAttribute(bone, Block.ATTRIBUTE.BONE)
						nowPieceObject = getNextObjectCopy(nextPieceCount)
						nextPieceCount++
						if(nextPieceCount<0) nextPieceCount = 0
					} else {
						// 2回目以降
						holdPieceObject.let {pieceTemp ->
							holdPieceObject = getNextObjectCopy(nextPieceCount)?.also {
								it.applyOffsetArray(ruleOpt.pieceOffsetX[it.id], ruleOpt.pieceOffsetY[it.id])
							}
							nowPieceObject = pieceTemp
						}
						nextPieceCount++
						if(nextPieceCount<0) nextPieceCount = 0
					}
				} else // 通常ホールド
					if(holdPieceObject==null) {
						// 1回目
						holdPieceObject = nowPieceObject
						nowPieceObject = getNextObjectCopy(nextPieceCount)

						nextPieceCount++
						if(nextPieceCount<0) nextPieceCount = 0
					} else {
						// 2回目以降
						holdPieceObject.let {pieceTemp ->
							holdPieceObject = nowPieceObject
							nowPieceObject = pieceTemp
						}
					}

				holdPieceObject?.let {
					// Directionを戻す
					if(ruleOpt.holdResetDirection&&ruleOpt.pieceDefaultDirection[it.id]<Piece.DIRECTION_COUNT) {
						it.direction = ruleOpt.pieceDefaultDirection[it.id]
						it.updateConnectData()
					}
					nowPieceObject?.placeNum?.let {nNum ->
						nowPieceObject?.placeNum = it.placeNum
						it.placeNum = nNum
					}
				}

				// 使用した count+1
				holdUsedCount++
				if(ending==0||staffrollEnableStatistics) statistics.totalHoldUsed++
				initialSpin() //Hold swap triggered IRS
				// ホールド無効化
				initialHoldFlag = false
				holdDisable = true
			}
			if(!isRetroSkin) getNextObject(nextPieceCount)?.let {
				playSE(
					"piece_${it.type.name.lowercase(Locale.getDefault())}", 1f,
					if((owner.mode?.players?:1)>1) 0.3f else 1f
				)
			}
			nowPieceObject?.let {
				if(!it.offsetApplied)
					it.applyOffsetArray(ruleOpt.pieceOffsetX[it.id], ruleOpt.pieceOffsetY[it.id])
				it.big = big
				// 出現位置 (横）
				nowPieceX = getSpawnPosX(it, field)
				nowPieceY = getSpawnPosY(it)
				nowPieceBottomY = it.getBottom(nowPieceX, nowPieceY, field)
			}
			nowPieceColorOverride = if(itemRollRollEnable/* || itemEnable==ITEM.SPIN_LOCK*/) Block.COLOR.WHITE else null

			gCount = if(speed.gravity>speed.denominator&&speed.denominator>0)
				speed.gravity%speed.denominator
			else 0

			lockDelayNow = 0
			dasSpeedCount = dasDelay
			dasRepeat = false
			dasInstant = false
			extendedMoveCount = 0
			extendedSpinCount = 0
			softdropFall = 0
			harddropFall = 0
			manualLock = false
			finesse = false
			nowPieceSteps = 0
			nowPieceMoveCount = 0
			nowPieceSpinCount = 0
			nowPieceSpinFailCount = 0
			nowWallkickCount = 0
			nowWallkickRiseCount = 0
			lineClearing = 0
			garbageClearing = 0
			lastMove = LastMove.NONE
			kicked = false
			twistType = null

			getNextObject(nextPieceCount+ruleOpt.nextDisplay-1)?.run {
				setDarkness(0f)
				setAttribute(bone, Block.ATTRIBUTE.BONE)
				updateConnectData()
			}

			if(ending==0) timerActive = true
			if(!owner.replayMode||owner.replayRerecord) ai?.newPiece(this, playerID)
		}
		checkDropContinuousUse()

		var softDropUsed = false // この frame にSoft dropを使ったらtrue
		var softDropFallNow = 0 // この frame のSoft dropで落下した段count

		var updown = false // Up下同時押し flag
		if(ctrl.isPressAll(up, down)) updown = true

		nowPieceObject?.let {
			if(!dasInstant) {
				// ホールド
				if(ctrl.isPush(Controller.BUTTON_D)||initialHoldFlag)
					if(isHoldOK) {
						statc[0] = 0
						statc[1] = 1
						if(!initialHoldFlag) playSE("hold")
						initialHoldContinuousUse = true
						initialHoldFlag = false
						holdDisable = true
						return@statMove statMove()
					} else if(statc[0]>0&&!initialHoldFlag) playSE("holdfail")

				// spin
				val onGroundBeforeSpin = it.checkCollision(nowPieceX, nowPieceY+1, field)
				var spin = 0

				if(initialSpinDirection!=0) {
					spin = initialSpinDirection
					initialSpinLastDirection = initialSpinDirection
					initialSpinContinuousUse = true
					if(nowPieceSpinFailCount>0) nowPieceSpinFailCount--
					else playSE(if(frameSkin==FRAME_SKIN_GB) "initialrotateold" else "initialrotate")
				} else if(statc[0]>0||ruleOpt.moveFirstFrame) {
					if(itemRollRollEnable&&replayTimer%itemRollRollInterval==0) spin = 1 // Roll Roll

					//  button input
					spin = getSpinOperation(ctrl)
					if(spin==0) spun = false



					if(spin!=0) {
						initialSpinLastDirection = spin
						initialSpinContinuousUse = true
					}
				}

				if(spin!=0&&!spun) {
					// Direction after rotationを決める
					var rt = getSpinDirection(spin)

					// rotationできるか判定
					if(!it.checkCollision(nowPieceX, nowPieceY, rt, field)) {
						// Wallkickなしでrotationできるとき
						spun = true
						kicked = false
						it.direction = rt
						it.updateConnectData(nowPieceX, nowPieceY)
					} else if(ruleOpt.spinWallkick&&wallkick!=null&&(initialSpinDirection==0||ruleOpt.spinInitialWallkick)
						&&(ruleOpt.lockResetLimitOver!=RuleOptions.LOCKRESET_LIMIT_OVER_NO_KICK||!isSpinCountExceed)
					) {
						// Wallkickを試みる
						val allowUpward = ruleOpt.spinWallkickMaxRise<0||nowWallkickRiseCount<ruleOpt.spinWallkickMaxRise

						wallkick?.executeWallkick(nowPieceX, nowPieceY, spin, it.direction, rt, allowUpward, it, field, ctrl)
							?.let {kick ->
								spun = true
								kicked = true
								if(!isRetroSkin) playSE("wallkick")
								nowWallkickCount++
								if(kick.isUpward) nowWallkickRiseCount++
								it.direction = kick.direction
								it.updateConnectData(nowPieceX, nowPieceY)
								nowPieceX += kick.offsetX
								nowPieceY += kick.offsetY

								if(ruleOpt.lockResetWallkick&&!isSpinCountExceed) {
									lockDelayNow = 0
									it.setDarkness(0f)
								}
							}
					} else if(dominoQuickTurn&&it.type==Piece.Shape.I2&&nowPieceSpinFailCount>=1) {
						// Domino Quick Turn
						rt = getSpinDirection(2)
						spun = true
						it.direction = rt
						it.updateConnectData(nowPieceX, nowPieceY)
						nowPieceSpinFailCount = 0

						if(it.checkCollision(nowPieceX, nowPieceY, rt, field)) nowPieceY--
						else if(onGroundBeforeSpin) nowPieceY++
					}

					if(spun) {
						// rotation成功
						nowPieceBottomY = it.getBottom(nowPieceX, nowPieceY, field)

						if(ruleOpt.lockResetSpin&&!isSpinCountExceed) {
							lockDelayNow = 0
							it.setDarkness(0f)
						}

						lastMove = if(onGroundBeforeSpin) {
							extendedSpinCount++
							LastMove.SPIN_GROUND
						} else LastMove.SPIN_AIR

						if(frameSkin!=FRAME_SKIN_SG) playSE(if(frameSkin==FRAME_SKIN_GB) "rotateold" else "rotate")
						val twisting = checkTwisted(nowPieceX, nowPieceY, it, field)
						if(!isRetroSkin&&twisting!=null) playSE("twist")
						nowPieceSpinCount += spin.absoluteValue
						if(nowPieceObject?.type!=Piece.Shape.O) nowPieceSteps += spin.absoluteValue
						if(ending==0||staffrollEnableStatistics) statistics.totalPieceSpin += spin.absoluteValue
					} else if(ctrl.isPush(Controller.BUTTON_A)||ctrl.isPush(Controller.BUTTON_C)||
						ctrl.isPush(Controller.BUTTON_B)||ctrl.isPush(Controller.BUTTON_E)) {
						// rotation失敗
						/*if(!isRetroSkin)*/playSE("rotfail")
						nowPieceSpinFailCount++
						if(ruleOpt.spinHoldBuffer) initialSpin()
					}
				}

				initialSpinDirection = 0

				// game over check
				if(statc[0]==0&&it.checkCollision(nowPieceX, nowPieceY, field)) {
					val mass = if(it.big) 2 else 1
					while(nowPieceX+it.maximumBlockX*mass>field.width) nowPieceX--
					while(nowPieceX-it.minimumBlockX*mass<0) nowPieceX++
					while((nowPieceBottomY-it.height)<-field.hiddenHeight) {
						nowPieceY++
						nowPieceBottomY = it.getBottom(nowPieceX, nowPieceY, field)
					}
					// Blockの出現位置を上にずらせる場合はそうする
					for(i in 0..<ruleOpt.pieceEnterMaxDistanceY) {
						nowPieceY--

						if(!it.checkCollision(nowPieceX, nowPieceY, field)) {
							nowPieceBottomY = it.getBottom(nowPieceX, nowPieceY, field)
							break
						}
					}

					// 死亡
					if(it.checkCollision(nowPieceX, nowPieceY, field)) {
						it.placeToField(nowPieceX, nowPieceY, field)
						nowPieceObject = null
						stat = Status.GAMEOVER
						stopSE("danger")
						if(ending==2&&staffrollNoDeath) stat = Status.NOTHING
						resetStatc()
						return@statMove
					}
				}
			}

			var sideMoved = false // この frame に横移動したらtrue

			if(statc[0]>0||ruleOpt.moveFirstFrame) {
				// 横移動
				val onGroundBeforeMove = it.checkCollision(nowPieceX, nowPieceY+1, field)
				var move = moveDirection

				fpf = 0
				if(statc[0]==0&&delayCancel) {
					if(delayCancelMoveLeft) move = -1
					if(delayCancelMoveRight) move = 1
					dasCount = das/2
					// delayCancel = false;
					delayCancelMoveLeft = false
					delayCancelMoveRight = false
				} else if(statc[0]==1&&delayCancel&&dasCount<das) delayCancel = false

				if(move!=0) {
					sideMoved = true
					if(big&&bigMove) move *= 2
					if(dasCount==0||dasCount>=das) {
						if(dasCount==0||statc[0]==(!ruleOpt.moveFirstFrame).toInt()) shiftLock = 0
						shiftLock = shiftLock and ctrl.buttonBit
						if(shiftLock==0)
							if(dasSpeedCount>=dasDelay||dasCount==0) {
								if(dasCount>0) dasSpeedCount = 1
								if(!it.checkCollision(nowPieceX+move, nowPieceY, field)) {
									nowPieceX += move

									if(dasDelay==0&&dasCount>0&&
										!it.checkCollision(nowPieceX+move, nowPieceY, field)
									) {
										if(!dasInstant&&frameSkin!=FRAME_SKIN_SG)
											playSE(if(frameSkin==FRAME_SKIN_GB||frameSkin==FRAME_SKIN_HEBO) "moveold" else "move")
										dasRepeat = true
										dasInstant = true
									}

									//log.debug("Successful movement: move="+move);

									if(ruleOpt.lockResetMove&&!isMoveCountExceed) {
										lockDelayNow = 0
										it.setDarkness(0f)
									}

									nowPieceMoveCount++
									if(ending==0||staffrollEnableStatistics) statistics.totalPieceMove++
									if(dasCount==0||statc[0]==(!ruleOpt.moveFirstFrame).toInt()) nowPieceSteps++
									nowPieceBottomY = it.getBottom(nowPieceX, nowPieceY, field)

									lastMove = if(onGroundBeforeMove) {
										//it.updateConnectData(nowPieceX, nowPieceY, field)
										owner.receiver.pieceFlicked(this, nowPieceX, nowPieceY, it, true)
										extendedMoveCount++
										LastMove.SLIDE_GROUND
									} else LastMove.SLIDE_AIR
									if(!dasInstant&&frameSkin!=FRAME_SKIN_SG)
										playSE(if(frameSkin==FRAME_SKIN_GB||frameSkin==FRAME_SKIN_HEBO) "moveold" else "move")
								} else {
									if(!dasWall) {
										if(!isRetroSkin) playSE("movefail")
										dasWall = true
									}
									if(ruleOpt.dasChargeOnBlockedMove) {
										dasCount = das
										dasSpeedCount = dasDelay
									}
									frameX += 5f.let {f -> (f*move.sign).let {i -> (frameX+i).coerceIn(-f, f)-frameX}}
								}

							} else dasSpeedCount++
					}

				}
				if(!dasRepeat) {
					// Hard drop
					if(ctrl.isPress(up)&&!harddropContinuousUse&&ruleOpt.harddropEnable&&
						(isDiagonalMoveEnabled||!sideMoved)&&(ruleOpt.moveUpAndDown||!updown)&&nowPieceY<nowPieceBottomY
					) {
						harddropFall += nowPieceBottomY-nowPieceY
						fpf = nowPieceBottomY-nowPieceY
						nowPieceY = nowPieceBottomY
						if(!isRetroSkin) playSE("harddrop", 1f, (fpf*2f/field.height).coerceIn(.75f, 1f))
						harddropContinuousUse = !ruleOpt.harddropLock
						owner.mode?.afterHardDropFall(this, harddropFall)
						owner.receiver.afterHardDropFall(this, harddropFall)
						frameY += (3+fpf/4).let {i -> minOf((frameY+i), 8f)-frameY}

						lastMove = LastMove.FALL_SELF
						if(ruleOpt.lockResetFall) {
							lockDelayNow = 0
							it.setDarkness(0f)
							extendedMoveCount = 0
							extendedSpinCount = 0
						}
					}
					if(ruleOpt.softdropEnable&&ctrl.isPress(down)&&
						!softdropContinuousUse&&(isDiagonalMoveEnabled||!sideMoved)&&
						(ruleOpt.moveUpAndDown||!updown)&&
						(!onGroundBeforeMove&&!harddropContinuousUse)
					) {
						// This prevents soft drop from adding to the gravity speed.
						if(!ruleOpt.softdropGravitySpeedLimit) gCount = 0
						if(!ruleOpt.softdropGravitySpeedLimit||(softDropSpd<speed.denominator)) {// Old Soft Drop codes
							gCount += softDropSpd.toInt()
							softDropUsed = true
						} else {// New Soft Drop codes
							gCount = softDropSpd.toInt()
							softDropUsed = true
						}
					}
				}
				if(ending==0||staffrollEnableStatistics) statistics.totalPieceActiveTime++
			}
			if(!ruleOpt.softdropGravitySpeedLimit||softDropSpd<1f||!softDropUsed) gCount += speed.gravity // Part of Old Soft Drop

			while((gCount>=speed.denominator||speed.gravity<0)&&!it.checkCollision(nowPieceX, nowPieceY+1, field)) {
				if(speed.denominator!=0) {
					if(speed.gravity>=0) gCount -= speed.denominator
					if(ruleOpt.softdropGravitySpeedLimit) gCount -= gCount%speed.denominator
				}
				nowPieceY++
				/*if(speed.gravity>speed.denominator/2||speed.gravity<0||softDropUsed)*/ fpf++
				if(ruleOpt.lockResetFall) {
					lockDelayNow = 0
					it.setDarkness(0f)
				}

				if(lastMove!=LastMove.SPIN_GROUND&&lastMove!=LastMove.SLIDE_GROUND&&lastMove!=LastMove.FALL_SELF) {
					extendedMoveCount = 0
					extendedSpinCount = 0
				}

				if(softDropUsed) {
					lastMove = LastMove.FALL_SELF
					softdropFall++
					softDropFallNow++
				} else lastMove = LastMove.FALL_AUTO
			}
			if(fpf>0) owner.receiver.afterPieceFall(this, fpf)
			if(softDropFallNow>0) {
				if(!isRetroSkin) playSE("softdrop")
				owner.mode?.afterSoftDropFall(this, softDropFallNow)
				owner.receiver.afterSoftDropFall(this, softDropFallNow)
			}

			// 接地と固定
			if(it.checkCollision(nowPieceX, nowPieceY+1, field)&&(statc[0]>0||ruleOpt.moveFirstFrame)) {
				if(lockDelayNow==0&&lockDelay>0&&lastMove!=LastMove.SLIDE_GROUND&&lastMove!=LastMove.SPIN_GROUND) {
					//it.updateConnectData(nowPieceX, nowPieceY, field)
					owner.receiver.pieceFlicked(this, nowPieceX, nowPieceY, it, false)
					playSE(if(frameSkin==FRAME_SKIN_GB) "stepold" else "step")
					if(!ruleOpt.softdropLock&&ruleOpt.softdropSurfaceLock&&softDropUsed) softdropContinuousUse = true
				}
				if(lockDelayNow<lockDelay) lockDelayNow++

				if(lockDelay>=99&&lockDelayNow>98) lockDelayNow = 98

				it.setDarkness(minOf(.75f*lockDelayNow/lockDelay, .75f))

				if(lockDelay!=0) gCount = speed.gravity

				// trueになると即固定
				var instantLock = false

				// Hard drop固定
				if(ruleOpt.harddropEnable&&!harddropContinuousUse&&
					ctrl.isPress(up)&&ruleOpt.harddropLock&&
					(isDiagonalMoveEnabled||!sideMoved)&&(ruleOpt.moveUpAndDown||!updown)
				) {
					harddropContinuousUse = true
					manualLock = true
					instantLock = true
				}

				// Soft drop固定
				if(ruleOpt.softdropEnable&&
					(ruleOpt.softdropLock&&ctrl.isPress(down)||ctrl.isPush(down)&&
						(ruleOpt.softdropSurfaceLock||speed.gravity<0)&&!softDropUsed)
					&&!softdropContinuousUse&&(isDiagonalMoveEnabled||!sideMoved)&&(ruleOpt.moveUpAndDown||!updown)
				) {
					softdropContinuousUse = true
					manualLock = true
					instantLock = true
				}
				if(manualLock&&ruleOpt.shiftLockEnable)
				// bit 1 and 2 are button_up and button_down currently
					shiftLock = ctrl.buttonBit and 3

				// 移動＆rotationcount制限超過
				if(ruleOpt.lockResetLimitOver==RuleOptions.LOCKRESET_LIMIT_OVER_INSTANT&&(isMoveCountExceed||isSpinCountExceed))
					instantLock = true

				// 接地即固定
				if(lockDelay==0&&(gCount>=speed.denominator||speed.gravity<0)) instantLock = true

				// 固定
				if(lockDelay in 1..lockDelayNow||instantLock) {
					// Twister判定
					if(lastMove==LastMove.SPIN_GROUND&&twistEnable)
						twistType = checkTwisted(nowPieceX, nowPieceY, it, field, useAllSpinBonus)

					it.setAttribute(true, Block.ATTRIBUTE.SELF_PLACED)

					val partialLockOut = it.isPartialLockOut(nowPieceX, nowPieceY)
					it.setDarkness(.5f)
					val underRoof = nowPieceObject?.isUnderRoof(nowPieceX, nowPieceY, field)?:false
					val put = it.placeToField(nowPieceX, nowPieceY, field)

					if(ruleOpt.lockFlash>0) it.setDarkness(-.8f)

					playSE("lock", 1.25f-(nowPieceY+it.height)*.25f/fieldHeight)
					holdDisable = false


					if(ending==0||staffrollEnableStatistics)
						statistics.totalPieceLocked++// AREなし
					if(fieldWidth==10&&owner.mode?.gameStyle==GameStyle.TETROMINO) {
						val momentum = nowPieceSteps-(2*(twistType!=null).toInt())-(2*underRoof.toInt())
						val d = momentum-it.finesseLimit(nowPieceX)
						val finePts = /*if(nowPieceBottomY>=fieldHeight-1||underRoof) 5 else*/ if(d<=0) 5 else maxOf(3-d, 0)

						finesse = d<=0
						if(ending==0||staffrollEnableStatistics) {
							statistics.finessePts += finePts
							if(finesse) {
								statistics.finesse++
								if(++finesseCombo>statistics.maxFinesseCombo) statistics.maxFinesseCombo = finesseCombo
							} else {
								statistics.finesseFault += d
								finesseCombo = 0
							}
						}
					}
					val clearNow = clearMode.check(field)
					lineClearing = clearNow.size

					chain = 0
					lineGravityTotalLines = 0
					garbageClearing = clearNow.garbageCleared
					//if(combo==0 && lastevent!=EVENT_NONE)lastevent=EVENT_NONE;
					if(lineClearing==0) {
						val ev = ScoreEvent(it, twistType = twistType)
						if(twist) {
							playSE("twister")
							lastEvent = ev
							if(b2bCount<0) {
								b2bCount = 0
								playSE("b2b_start")
							}
							if(ending==0||staffrollEnableStatistics)
								if(twistMini) statistics.totalTwistZeroMini++
								else statistics.totalTwistZero++
							owner.receiver.calcScore(this, lastEvent)
						} else combo = -1

						owner.mode?.calcScore(this, ev)?.let {sc ->
							if(sc>0)
								owner.receiver.addScore(this, nowPieceX, nowPieceBottomY, sc, ReceiverC.getPlayerColor(playerID))
						}
					}

					owner.mode?.pieceLocked(this, lineClearing, finesse)
					owner.receiver.pieceLocked(this, nowPieceX, nowPieceY, it, lineClearing, finesse)

					dasRepeat = false
					dasInstant = false

					// Next 処理を決める(Mode 側でステータスを弄っている場合は何もしない)
					if(stat==Status.MOVE) {
						resetStatc()

						when {
							ending==1 -> stat = Status.ENDINGSTART // Ending
							!put&&ruleOpt.fieldLockoutDeath||partialLockOut&&ruleOpt.fieldPartialLockoutDeath -> {
								// 画面外に置いて死亡
								stat = Status.GAMEOVER
								stopSE("danger")
								if(ending==2&&staffrollNoDeath) stat = Status.NOTHING
							}
							(lineGravityType==LineGravity.CASCADE||lineGravityType==LineGravity.CASCADE_SLOW)&&!connectBlocks -> {
								stat = Status.LINECLEAR
								statc[0] = lineDelay
								statLineClear()
							}
							clearNow.size>0&&(ruleOpt.lockFlash<=0||!ruleOpt.lockFlashBeforeLineClear) -> {
								// Line clear
								stat = Status.LINECLEAR
								statLineClear()
							}
							(are>0||lagARE||ruleOpt.lockFlashBeforeLineClear)&&
								ruleOpt.lockFlash>1||(ruleOpt.lockFlash==1&&ruleOpt.lockFlashOnlyFrame)
								-> {
								// AREあり (光あり）
								stat = Status.LOCKFLASH
								statc[0] = if(ruleOpt.lockFlashOnlyFrame) 0 else 1
							}
							are>0||lagARE -> {
								// AREあり (光なし）
								statc[1] = are
								stat = Status.ARE
							}
							interruptItem!=null -> {
								// 中断効果のあるアイテム処理
								nowPieceObject?.let {b ->
									owner.receiver.blockBreak(this,
										b.map.mapValues {(_, col) -> col.mapKeys {(x, _) -> x+nowPieceX}}.mapKeys {(row, _) -> row+nowPieceY})
								}
								nowPieceObject = null
								interruptItemPreviousStat = Status.MOVE
								stat = Status.INTERRUPTITEM
							}
							else -> {
								// AREなし
								stat = Status.MOVE
								if(!ruleOpt.moveFirstFrame) statMove()
							}
						}
					}
					return@statMove
				}
			}

			// 横溜め
			if(statc[0]>0||ruleOpt.dasInMoveFirstFrame) if(moveDirection!=0&&moveDirection==dasDirection&&(dasCount<das||das<=0))
				dasCount++

			statc[0]++
		}?:run {statc[0] = 0}
	}

	/** Block固定直後の光っているときの処理 */
	private fun statLockFlash() {
		//  event 発生
		if(owner.mode?.onLockFlash(this)==true) return
		owner.receiver.onLockFlash(this)
		statc[0]++
		checkDropContinuousUse()

		// 横溜め
		if(ruleOpt.dasInLockFlash)
			padRepeat()
		else if(ruleOpt.dasRedirectInDelay) dasRedirect()

		// Next ステータス
		if(statc[0]>=ruleOpt.lockFlash) {
			resetStatc()

			if(lineClearing>0) {
				// Line clear
				stat = Status.LINECLEAR
				statLineClear()
			} else {
				// ARE
				statc[1] = are
				stat = Status.ARE
			}
			return
		}
	}

	/** Line clear処理 */
	private fun statLineClear() {
		//  event 発生
		if(owner.mode?.onLineClear(this)==true) return
		owner.receiver.onLineClear(this)
		checkDropContinuousUse()
		// 横溜め
		if(ruleOpt.dasInLineClear) padRepeat()
		else if(ruleOpt.dasRedirectInDelay) dasRedirect()

		// 最初の frame
		if(statc[0]==0) {
			val check = clearMode.flag(this, field)
			val li = check.size
			//lineClearing = li
			if(check.gemCleared>0) playSE("gem")
			val ev = ScoreEvent(nowPieceObject, li, b2bCount, combo, twistType, split)
			lastEvent = ev
			// All clear
			if(li>=1&&field.isEmpty) {
				statistics.bravos++
				owner.receiver.bravo(this)
				tempHanabi += 6
			}
			// Calculate score
			owner.mode?.calcScore(this, ev)?.let {
				if(it>0) owner.receiver.addScore(this, nowPieceX, lastLinesY.maxBy{it.size}.average().toInt(), it)
			}
			if(li>0) owner.receiver.calcScore(this, ev)

			if(b2bCount<-1) b2bCount = -1

			// Blockを消す演出を出す (まだ実際には消えていない）
			if(clearMode===Line) (0..<field.height).filter {field.getLineFlag(it)}.let {
				owner.mode?.lineClear(this, it)
				owner.receiver.lineClear(this, it)
			}
			field.filterAttributeMap(Block.ATTRIBUTE.ERASE).let {
				if(owner.mode?.blockBreak(this, it)!=true)
					owner.receiver.blockBreak(this, it)
			}
			// Blockを消す
			clearMode.clear(field)
		}

		val fallCheck = lineGravityType.check(field)
		statc[7] = fallCheck.first
		statc[8] = fallCheck.second
// Linesを1段落とす
		if(lineGravityType==LineGravity.Native&&ruleOpt.lineFallAnim&&statc[0]>=lineDelay-(fallCheck.first-1).coerceAtLeast
				(0))
			lineGravityType.fallSingle(field)//field.downFloatingBlocksSingleLine()

// Line delay cancel check
		delayCancelMoveLeft = ctrl.isPush(Controller.BUTTON_LEFT)
		delayCancelMoveRight = ctrl.isPush(Controller.BUTTON_RIGHT)

		val moveCancel = canLineCancelMove&&(ctrl.isPush(up)||ctrl.isPush(down)
			||delayCancelMoveLeft||delayCancelMoveRight)
		val spinCancel = canLineCancelSpin&&(ctrl.isPush(Controller.BUTTON_A)||ctrl.isPush(Controller.BUTTON_B)
			||ctrl.isPush(Controller.BUTTON_C)||ctrl.isPush(Controller.BUTTON_E)||ctrl.isPush(Controller.BUTTON_F))
		val holdCancel = canLineCancelHold&&ctrl.isPush(Controller.BUTTON_D)

		delayCancel = moveCancel||spinCancel||holdCancel

		if(statc[0]<lineDelay&&delayCancel) statc[0] = lineDelay

// Next ステータス
		if(statc[0]>=lineDelay) {
			if(lineGravityType==LineGravity.CASCADE||lineGravityType==LineGravity.CASCADE_SLOW) // Cascade
				if(statc[6]<cascadeDelay) {
					statc[6]++
					field.filterAttributeBlocks(Block.ATTRIBUTE.CASCADE_FALL).forEach {(_, _, b) ->
						b.offsetY = statc[6]/cascadeDelay.toFloat()
					}
					return
				} else if(fallCheck.first>0) {
					statc[9] = lineGravityType.fallSingle(field)
					statc[6] = 0
					if(!isRetroSkin) playSE("softdrop")
					return
				} else if(statc[6]<cascadeClearDelay) {
					statc[6]++
					return
				} else if(clearMode.check(field).size>0) {
					twistType = null
					chain++
					if(chain>statistics.maxChain) statistics.maxChain = chain
					statc[0] = 0
					statc[6] = 0
					return
				}

			val skip = owner.mode?.lineClearEnd(this)?:false
			owner.receiver.lineClearEnd(this)
			if(sticky>0) field.setBlockLinkByColor()
			if(sticky==2) field.setAllAttribute(true, Block.ATTRIBUTE.IGNORE_LINK)

			if(!skip) {
				if(lineGravityType==LineGravity.Native) lineGravityType.fallInstant(field)
				lastLinesY.filter {it.max()>=field.highestBlockY}.distinctBy {it.size>=3}.forEach {
					playSE(
						when {
							it.size>=4 -> "linefall1"
							it.size<=1 -> "linefall"
							else -> "linefall0"
						},
						maxOf(0.8f, 1.2f-it.max()/3f/fieldHeight),
						minOf(1f, 0.4f+speed.lineDelay*0.1f)
					)
				}
//				field.lineColorsCleared = emptyList()

				resetStatc()
				when {
					ending==1 -> stat = Status.ENDINGSTART// Ending
					areLine>0||lagARE -> {
						// AREあり
						statc[0] = 0
						statc[1] = areLine
						statc[2] = 1
						stat = Status.ARE
					}
					interruptItem!=null -> {
						// AREなし:中断効果のあるアイテム処理
						nowPieceObject = null
						interruptItemPreviousStat = Status.MOVE
						stat = Status.INTERRUPTITEM
					}
					else -> {
						// AREなし
						nowPieceObject = null
						stat = Status.MOVE
					}
				}
			}
		} else statc[0]++
	}

	/** ARE中の処理 */
	private fun statARE() {
		//  event 発生
		if(owner.mode?.onARE(this)==true) return

		owner.receiver.onARE(this)
		if(statc[0]==0)
			if(field.danger) {
				loopSE("danger")
				recoveryFlag = true
			} else {
				stopSE("danger")
				if(field.safety) recoveryFlag = false
			}

		statc[0]++

		checkDropContinuousUse()

		// ARE cancel check
		delayCancelMoveLeft = ctrl.isPush(Controller.BUTTON_LEFT)
		delayCancelMoveRight = ctrl.isPush(Controller.BUTTON_RIGHT)

		val moveCancel = canARECancelMove&&(ctrl.isPush(up)||ctrl.isPush(down)||delayCancelMoveLeft||delayCancelMoveRight)
		val spinCancel = canARECancelSpin&&(ctrl.isPush(Controller.BUTTON_A)||ctrl.isPush(Controller.BUTTON_B)
			||ctrl.isPush(Controller.BUTTON_C)||ctrl.isPush(Controller.BUTTON_E)||ctrl.isPush(Controller.BUTTON_F))
		val holdCancel = canARECancelHold&&ctrl.isPush(Controller.BUTTON_D)

		delayCancel = moveCancel||spinCancel||holdCancel

		if(statc[0]<statc[1]&&delayCancel) statc[0] = statc[1]

		// 横溜め
		if(ruleOpt.dasInARE&&(statc[0]<statc[1]-1||ruleOpt.dasInARELastFrame)) {
			padRepeat()
			if(ruleOpt.dasChargeOnBlockedMove) {
				dasCount = das
				dasSpeedCount = dasDelay
			}
		} else if(ruleOpt.dasRedirectInDelay) dasRedirect()

		// Next ステータス
		if(statc[0]>=statc[1]&&!lagARE) {
			nowPieceObject = null
			resetStatc()
			lockDelayNow = 0

			if(interruptItem!=null) {
				// 中断効果のあるアイテム処理
				interruptItemPreviousStat = Status.MOVE
				stat = Status.INTERRUPTITEM
			} else {
				// Blockピース移動処理
				initialSpin()
				stat = Status.MOVE
			}
		}
	}

	/** Ending突入処理 */
	private fun statEndingStart() {
		//  event 発生
		val animInt = 6
		statc[3] = field.height*6
		if(owner.mode?.onEndingStart(this)==true) return
		owner.receiver.onEndingStart(this)

		checkDropContinuousUse()
		// 横溜め
		if(ruleOpt.dasInEndingStart) padRepeat()
		else if(ruleOpt.dasRedirectInDelay) dasRedirect()

		if(statc[2]==0) {
			timerActive = false
			owner.musMan.bgm = BGM.Silent
			playSE("endingstart")
			statc[2] = 1
		}
		if(statc[0]<lineDelay) statc[0]++
		else if(statc[1]<statc[3]) {
			field.let {field ->
				if(statc[1]%animInt==0) {
					val y = field.height-statc[1]/animInt
					field.getRow(y).mapIndexedNotNull {i, b ->
						b?.let {if(it.getAttribute(Block.ATTRIBUTE.ERASE)) i to it else null}
					}.associate {it}.let {
						field.delBlocks(mapOf(y to it)).let {b ->
							if(owner.mode?.blockBreak(this, b)!=true)
								owner.receiver.blockBreak(this, b)
						}
					}
				}
			}

			statc[1]++
		} else if(statc[0]<lineDelay+2) statc[0]++
		else {
			ending = 2
			resetStatc()

			if(staffrollEnable&&gameActive&&isInGame) {
				field.reset()
				nowPieceObject = null
				stat = Status.MOVE
			} else stat = Status.EXCELLENT
		}
	}

	/** 各ゲームMode が自由に使えるステータスの処理 */
	private fun statCustom() {
		//  event 発生
		if(owner.mode?.onCustom(this)==true) return
		owner.receiver.onCustom(this)
	}

	/** Ending画面 */
	private fun statExcellent() {
		//  event 発生
		if(owner.mode?.onExcellent(this)==true) return
		owner.receiver.onExcellent(this)

		if(statc[0]==0) {
			stopSE("danger")
			gameEnded()
			owner.musMan.fadeSW = true
			resetFieldVisible()
			tempHanabi += 24
			playSE("excellent")
		}

		if(statc[0]>=120&&statc[1]<=0&&ctrl.isPush(Controller.BUTTON_A)) statc[0] = 600
		if(statc[0]>=600) {
			resetStatc()
			stat = Status.GAMEOVER
		} else statc[0]++
	}

	/** game overの処理 */
	private fun statGameOver() {
		//  event 発生
		if(owner.mode?.onGameOver(this)==true) return
		owner.receiver.onGameOver(this)
		if(statc[0]==0) {
			//死亡時はgameActive中にStatus.GAMEOVERになる
			statc[2] = if(gameActive&&!staffrollNoDeath) 0 else 1
			stopSE("danger")
		}
		val topOut = statc[2]==0
		field.let {field ->
			if(!topOut||lives<=0) {
				// もう復活できないとき
				val animInt = 6
				statc[1] = animInt*(field.height+1)
				if(statc[0]==0) {
					if(topOut) playSE("dead_last")
					gameEnded()
					blockShowOutlineOnly = false
					if(!owner.isGameActive) owner.musMan.fadeSW = true

					if(field.isEmpty) statc[0] = statc[1]
					else {
						resetFieldVisible()
						if(ending==2&&!topOut) playSE("gamewon")
						else playSE("shutter")
					}
				}
				when {
					statc[0]<statc[1] -> {
						for(x in 0..<field.width)
							field.getBlock(x, field.height-statc[0]/animInt)?.apply {
								if(ending==2&&!topOut) {
									if(statc[0]%animInt==0) {
										setAttribute(false, Block.ATTRIBUTE.OUTLINE)
										darkness = -.1f
										elapsedFrames = -1
									}
									alpha = 1f-(1+statc[0]%animInt)*1f/animInt
								} else if(statc[0]%animInt==0) {
									if(!getAttribute(Block.ATTRIBUTE.GARBAGE)) {
										cint = Block.COLOR_WHITE
										setAttribute(true, Block.ATTRIBUTE.GARBAGE)
									}
									darkness = .3f
									elapsedFrames = -1
								}
							}

						statc[0]++
					}
					statc[0]==statc[1] -> {
						if(ending==2&&field.isEmpty) playSE("gamewon") else
							playSE("gamelost")
						statc[0]++
					}
					statc[0]<statc[1]+180 -> {
						if(statc[0]>=statc[1]+60&&ctrl.isPush(Controller.BUTTON_A)) statc[0] = statc[1]+180
						statc[0]++
					}
					else -> {
						if(!owner.replayMode||owner.replayRerecord) owner.saveReplay()

						for(i in 0..<owner.players)
							if(i==playerID||dieAll) {
								owner.engine[i].field.reset()
								owner.engine[i].resetStatc()
								owner.engine[i].stat = Status.RESULT
							}
					}
				}
			} else {
				// 復活できるとき
				if(statc[0]==0) {
					/*if(topOut) */playSE("dead")
					//blockShowOutlineOnly=false
					resetFieldVisible()
					for(i in field.hiddenHeight*-1..<field.height)
						for(j in 0..<field.width)
							field.getBlock(j, i)?.apply {color = Block.COLOR.BLACK}
					statc[0] = 1
				}
				if(!field.isEmpty) {
					val y = field.highestBlockY
					for(i in 0..<field.width) {
						field.getRow(y).mapIndexedNotNull {my, b ->
							b?.let {if(it.getAttribute(Block.ATTRIBUTE.ERASE)) my to it else null}
						}.associate {it}.let {
							field.delBlocks(mapOf(y to it)).let {b ->
								if(owner.mode?.blockBreak(this, b)!=true) owner.receiver.blockBreak(this, b)
							}
						}
					}
				} else if(statc[1]<are) statc[1]++
				else {
					lives--
					resetStatc()
					stat = Status.MOVE
				}
			}
		}
	}

	/** Results screen */
	private fun statResult() {
		// Event
		owner.musMan.fadeSW = false
		owner.musMan.bgm = BGM.Result(
			when {
				ending==2 -> if(owner.mode?.gameIntensity in 1..2) (if(statistics.time<10800) 1 else 2) else 3
				ending!=0 -> if(statistics.time<10800) 1 else 2
				else -> 0
			}
		)

		if(owner.mode?.onResult(this)==true) return
		owner.receiver.onResult(this)

		// Turn-off in-game flags
		gameActive = false
		timerActive = false
		isInGame = false

		// Cursor movement
		if(ctrl.isMenuRepeatKey(Controller.BUTTON_LEFT)||ctrl.isMenuRepeatKey(Controller.BUTTON_RIGHT)) {
			statc[0] = (statc[0]==0).toInt()
			playSE("cursor")
		}

		// Confirm
		if(ctrl.isPush(Controller.BUTTON_A)) {
			playSE("decide")

			if(statc[0]==0) owner.reset()
			else quitFlag = true
		}
	}

	/** fieldエディット画面 */
	private fun statFieldEdit() {
		//  event 発生
		if(owner.mode?.onFieldEdit(this)==true) return
		owner.receiver.onFieldEdit(this)

		mapEditFrames++

		// Cursor movement
		if(ctrl.isMenuRepeatKey(Controller.BUTTON_LEFT, false)&&!ctrl.isPress(Controller.BUTTON_C)) {
			playSE("move")
			mapEditX--
			if(mapEditX<0) mapEditX = fieldWidth-1
		}
		if(ctrl.isMenuRepeatKey(Controller.BUTTON_RIGHT, false)&&!ctrl.isPress(Controller.BUTTON_C)) {
			playSE("move")
			mapEditX++
			if(mapEditX>fieldWidth-1) mapEditX = 0
		}
		if(ctrl.isMenuRepeatKey(up, false)) {
			playSE("move")
			mapEditY--
			if(mapEditY<0) mapEditY = fieldHeight-1
		}
		if(ctrl.isMenuRepeatKey(down, false)) {
			playSE("move")
			mapEditY++
			if(mapEditY>fieldHeight-1) mapEditY = 0
		}

		// 色選択
		if(ctrl.isMenuRepeatKey(Controller.BUTTON_LEFT, false)&&ctrl.isPress(Controller.BUTTON_C)) {
			playSE("cursor")
			mapEditColor--
			if(mapEditColor<Block.COLOR_WHITE) mapEditColor = Block.COLOR_GEM_PURPLE
		}
		if(ctrl.isMenuRepeatKey(Controller.BUTTON_RIGHT, false)&&ctrl.isPress(Controller.BUTTON_C)) {
			playSE("cursor")
			mapEditColor++
			if(mapEditColor>Block.COLOR_GEM_PURPLE) mapEditColor = Block.COLOR_WHITE
		}

		field.let {field ->
			// 配置
			if(ctrl.isPress(Controller.BUTTON_A)&&mapEditFrames>10)
				try {
					if(field.getBlock(mapEditX, mapEditY)?.cint!=mapEditColor) {
						field.setBlock(
							mapEditX, mapEditY,
							Block(mapEditColor, blkSkin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
						)
						playSE("change")
					}
				} catch(_:Exception) {
				}

			// 消去
			if(ctrl.isPress(Controller.BUTTON_D)&&mapEditFrames>10)
				try {
					if(field.delBlock(mapEditX, mapEditY)!=null)
						playSE("change")
				} catch(_:Exception) {
				}
		}
		// 終了
		if(ctrl.isPush(Controller.BUTTON_B)&&mapEditFrames>10) {
			stat = mapEditPreviousStat
			owner.mode?.fieldEditExit(this)
			owner.receiver.fieldEditExit(this)
		}
	}

	/** プレイ中断効果のあるアイテム処理 */
	private fun statInterruptItem() {
		// 続行 flag
		val contFlag = interruptItem?.statInterrupt(this)?:false

		if(!contFlag) {
			interruptItem = null
			resetStatc()
			stat = interruptItemPreviousStat
		}
	}

	sealed class FrameSkin {
		class COLOR(cid:Int):FrameSkin() {
			val color = cid%FRAME_COLOR_ALL
		}

		data object GB:FrameSkin()
		data object SG:FrameSkin()
		data object HEBO:FrameSkin()
		data object GRADE:FrameSkin()
		data object METAL:FrameSkin()
		companion object {
			fun values():Array<FrameSkin> = arrayOf(COLOR(0), GB, SG, HEBO, GRADE, METAL)

			fun valueOf(value:Int):FrameSkin {
				return when(value) {
					FRAME_SKIN_GB -> GB
					FRAME_SKIN_SG -> SG
					FRAME_SKIN_HEBO -> HEBO
					FRAME_SKIN_GRADE -> GRADE
					FRAME_SKIN_METAL -> METAL
					else -> COLOR(value)
				}
			}
		}
	}

	/** Constants of main game status */
	enum class Status {
		NOTHING, SETTING, PROFILE, READY, MOVE, LOCKFLASH, LINECLEAR, ARE, ENDINGSTART, CUSTOM, EXCELLENT, GAMEOVER, RESULT,
		FIELDEDIT, INTERRUPTITEM
	}
	/** Constants of last successful movements */
	enum class LastMove {
		NONE, FALL_AUTO, FALL_SELF, SLIDE_AIR, SLIDE_GROUND, SPIN_AIR, SPIN_GROUND
	}
	/** Game style names */
	enum class GameStyle {
		TETROMINO, AVALANCHE, PHYSICIAN, SPF
	}

	enum class MeterColor(val color:Int) {
		LEVEL(-1), LIMIT(-2),
		RED(0xFF0000), ORANGE(0xFF8000), YELLOW(0xFFFF00), GREEN(0x00ff00), DARKGREEN(0x008000),
		CYAN(0x00FFFF), DARKBLUE(0x0000FF), BLUE(0x0080FF), PURPLE(0x8000FF), PINK(0xff0080)
	}

	companion object {
		/** Log (Apache log4j) */
		private var log = LogManager.getLogger()

		/** Max number of game style */
		val MAX_GAMESTYLE get() = GameStyle.entries.size
		val GAMESTYLE_NAMES = GameStyle.entries.map {it.name}
		/** Number of free status counters (used by statc array) */
		const val MAX_STATC = 10

		/** Constants of block outline type */
		const val BLOCK_OUTLINE_AUTO = -1
		const val BLOCK_OUTLINE_NONE = 0
		const val BLOCK_OUTLINE_NORMAL = 1
		const val BLOCK_OUTLINE_CONNECT = 2
		const val BLOCK_OUTLINE_SAMECOLOR = 3

		/** Default duration of Ready->Go */
		val READY_GO_TIME = 50 to 100

		/** Constants of frame colors */
		const val FRAME_COLOR_WHITE = 0
		const val FRAME_COLOR_GREEN = 1
		const val FRAME_COLOR_SILVER = 2
		const val FRAME_COLOR_RED = 3
		const val FRAME_COLOR_PINK = 4
		const val FRAME_COLOR_CYAN = 5
		const val FRAME_COLOR_BRONZE = 6
		const val FRAME_COLOR_PURPLE = 7
		const val FRAME_COLOR_BLUE = 8
		const val FRAME_COLOR_GRAY = 9
		const val FRAME_COLOR_YELLOW = 11
		const val FRAME_COLOR_ALL = 15
		const val FRAME_SKIN_GB = -1
		const val FRAME_SKIN_SG = -2
		const val FRAME_SKIN_HEBO = -3
		const val FRAME_SKIN_GRADE = -4
		const val FRAME_SKIN_METAL = -5

		/** Constants of meter colors */
		val METER_COLOR_LEVEL = MeterColor.LEVEL.color
		val METER_COLOR_LIMIT = MeterColor.LIMIT.color
		val METER_COLOR_RED = MeterColor.RED.color
		val METER_COLOR_ORANGE = MeterColor.ORANGE.color
		val METER_COLOR_YELLOW = MeterColor.YELLOW.color
		val METER_COLOR_GREEN = MeterColor.GREEN.color
		val METER_COLOR_DARKGREEN = MeterColor.DARKGREEN.color
		val METER_COLOR_CYAN = MeterColor.CYAN.color
		val METER_COLOR_DARKBLUE = MeterColor.DARKBLUE.color
		val METER_COLOR_BLUE = MeterColor.BLUE.color
		val METER_COLOR_PURPLE = MeterColor.PURPLE.color
		val METER_COLOR_PINK = MeterColor.PINK.color

		/** Constants of combo type */
		const val COMBO_TYPE_DISABLE = 0
		const val COMBO_TYPE_NORMAL = 1
		const val COMBO_TYPE_DOUBLE = 2

		/** Table for cint-block inum */
		val ITEM_COLOR_BRIGHT_TABLE =
			listOf(
				10, 10, 9, 9, 8, 8, 8, 7, 7, 7, 6, 6, 6, 5, 5, 5, 4, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 2, 1, 1, 1, 1, 0, 0, 0,
				0, 0, 0, 0, 0
			)

		/** Default list of block colors to use for random block colors. */
		val BLOCK_COLORS_DEFAULT =
			listOf(
				Block.COLOR.RED, Block.COLOR.ORANGE, Block.COLOR.YELLOW, Block.COLOR.GREEN,
				Block.COLOR.CYAN, Block.COLOR.BLUE, Block.COLOR.PURPLE
			)

		const val HANABI_INTERVAL = 10

		val EXPLOD_SIZE_DEFAULT =
			listOf(
				listOf(4, 3), listOf(3, 0), listOf(3, 1), listOf(3, 2), listOf(3, 3), listOf(4, 4),
				listOf(5, 5), listOf(5, 5), listOf(6, 6), listOf(6, 6), listOf(7, 7)
			)
	}
}
