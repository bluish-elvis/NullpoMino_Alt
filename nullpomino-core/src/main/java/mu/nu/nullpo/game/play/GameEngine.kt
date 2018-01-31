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
package mu.nu.nullpo.game.play

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.subsystem.ai.DummyAI
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick
import mu.nu.nullpo.util.GeneralUtil
import net.omegaboshi.nullpomino.game.subsystem.randomizer.MemorylessRandomizer
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer
import org.apache.log4j.Logger
import java.util.*

/** Each player's Game processing */
class GameEngine
/** Constructor
 * @param owner このゲームエンジンを所有するGameOwnerクラス
 * @param playerID Playerの number
 * @param ruleopt ルール設定
 * @param wallkick Wallkickシステム
 * @param randomizer Blockピースの出現順の生成アルゴリズム
 */
(
	/** GameManager: Owner of this GameEngine */
	val owner:GameManager,
	/** Player ID (0=1P) */
	val playerID:Int,
	/** RuleOptions: Most game settings are here */
	var ruleopt:RuleOptions = RuleOptions(),
	/** Wallkick: The wallkick system */
	var wallkick:Wallkick? = null,
	/** Randomizer: Used by creation of next piece sequence */
	var randomizer:Randomizer? = null
) {

	/** Field: The playfield */
	var field:Field? = null

	/** Controller: You can get player's input from here */
	var ctrl:Controller? = null

	/** Statistics: Various game statistics
	 * such as score, number of lines, etc */
	val statistics:Statistics = Statistics()

	/** SpeedParam: Parameters of game speed
	 * (Gravity, ARE, Line clear delay, etc) */
	val speed:SpeedParam = SpeedParam()

	/** Gravity counter (The piece falls when this reaches
	 * to the value of speed.denominator) */
	var gcount:Int = 0

	/** The first random-seed */
	var randSeed:Long = 0

	/** Random: Used for creating various randomness */
	var random:Random = Random()

	/** ReplayData: Manages input data for replays */
	var replayData:ReplayData? = null

	/** AIPlayer: AI for auto playing */
	var ai:DummyAI? = null

	/** AI move delay */
	var aiMoveDelay:Int = 0

	/** AI think delay (Only when using thread) */
	var aiThinkDelay:Int = 0

	/** Use thread for AI */
	var aiUseThread:Boolean = false

	/** Show Hint with AI */
	var aiShowHint:Boolean = false

	/** Prethink with AI */
	var aiPrethink:Boolean = false

	/** Show internal state of AI */
	var aiShowState:Boolean = false

	/** AI Hint piece (copy of current or hold) */
	var aiHintPiece:Piece? = null
	/** AI Hint X position */
	var aiHintX:Int = 0

	/** AI Hint Y position */
	var aiHintY:Int = 0

	/** AI Hint piece direction */
	var aiHintRt:Int = 0

	/** True if AI Hint is ready */
	var aiHintReady:Boolean = false

	/** Current main game status */
	lateinit var stat:Status

	/** Free status counters */
	var statc:IntArray = IntArray(MAX_STATC)

	/** true if game play, false if menu.
	 * Used for alternate keyboard mappings. */
	var isInGame:Boolean = false

	/** true if the game is active */
	var gameActive:Boolean = false

	/** true if the timer is active */
	var timerActive:Boolean = false

	/** true if the game is started
	 * (It will not change back to false until the game is reset) */
	var gameStarted:Boolean = false

	/** Timer for replay */
	var replayTimer:Int = 0

	/** Time of game start in milliseconds */
	var startTime:Long = 0

	/** Time of game end in milliseconds */
	var endTime:Long = 0

	/** Major version */
	var versionMajor:Float = 0f

	/** Minor version */
	var versionMinor:Int = 0

	/** OLD minor version (Used for 6.9 or earlier replays) */
	var versionMinorOld:Float = 0f

	/** Dev build flag */
	var versionIsDevBuild:Boolean = false

	/** Game quit flag */
	var quitflag:Boolean = false

	/** Piece object of current piece */
	var nowPieceObject:Piece? = null

	/** X coord of current piece */
	var nowPieceX:Int = 0

	/** Y coord of current piece */
	var nowPieceY:Int = 0

	/** Bottommost Y coord of current piece
	 * (Used for ghost piece and harddrop) */
	var nowPieceBottomY:Int = 0

	/** Write anything other than -1 to override whole current piece cint */
	var nowPieceColorOverride:Int = 0

	/** Allow/Disallow certain piece */
	var nextPieceEnable:BooleanArray = BooleanArray(Piece.PIECE_COUNT) {i:Int -> i<Piece.PIECE_STANDARD_COUNT}

	/** Preferred size of next piece array
	 * Might be ignored by certain Randomizer. (Default:1400) */
	var nextPieceArraySize:Int = 0

	/** Array of next piece IDs */
	var nextPieceArrayID:IntArray = IntArray(0)

	/** Array of next piece Objects */
	var nextPieceArrayObject:Array<Piece?> = emptyArray()

	/** Number of pieces put (Used by next piece sequence) */
	var nextPieceCount:Int = 0

	/** Hold piece (null: None) */
	var holdPieceObject:Piece? = null

	/** true if hold is disabled because player used it already */
	var holdDisable:Boolean = false

	/** Number of holds used */
	var holdUsedCount:Int = 0

	/** Number of lines currently clearing */
	var lineClearing:Int = 0
	var garbageClearing:Int = 0

	/** Line gravity type (Native, Cascade, etc) */
	var lineGravityType:LineGravity = LineGravity.NATIVE

	/** Current number of chains */
	var chain:Int = 0

	/** Number of lines cleared for this chains */
	var lineGravityTotalLines:Int = 0

	/** Lock delay counter */
	var lockDelayNow:Int = 0

	/** DAS counter */
	var dasCount:Int = 0

	/** DAS direction (-1:Left 0:None 1:Right) */
	var dasDirection:Int = 0

	/** DAS delay counter */
	var dasSpeedCount:Int = 0

	/** Repeat statMove() for instant DAS */
	var dasRepeat:Boolean = false

	/** In the middle of an instant DAS loop */
	var dasInstant:Boolean = false

	/** Disallow shift while locking key is pressed */
	var shiftLock:Int = 0

	/** IRS direction */
	var initialRotateDirection:Int = 0

	/** Last IRS direction */
	var initialRotateLastDirection:Int = 0

	/** IRS continuous use flag */
	var initialRotateContinuousUse:Boolean = false

	/** IHS */
	var initialHoldFlag:Boolean = false

	/** IHS continuous use flag */
	var initialHoldContinuousUse:Boolean = false

	/** Number of current piece movement */
	var nowPieceMoveCount:Int = 0

	/** Number of current piece rotations */
	var nowPieceRotateCount:Int = 0

	/** Number of current piece failed rotations */
	var nowPieceRotateFailCount:Int = 0

	/** Number of movement while touching to the floor */
	var extendedMoveCount:Int = 0

	/** Number of rotations while touching to the floor */
	var extendedRotateCount:Int = 0

	/** Number of wallkicks used by current piece */
	var nowWallkickCount:Int = 0

	/** Number of upward wallkicks used by current piece */
	var nowUpwardWallkickCount:Int = 0

	/** Number of rows falled by soft drop (Used by soft drop bonuses) */
	var softdropFall:Int = 0

	/** Number of rows falled by hard drop (Used by soft drop bonuses) */
	var harddropFall:Int = 0

	/** fall per frame */
	var fpf:Int = 0

	/** Soft drop continuous use flag */
	var softdropContinuousUse:Boolean = false

	/** Hard drop continuous use flag */
	var harddropContinuousUse:Boolean = false

	/** true if the piece was manually locked by player */
	var manualLock:Boolean = false

	/** Last successful movement */
	lateinit var lastmove:LastMove
	var lastline:Int = 0

	/** Most recent scoring event type */
	var lastevent:Int = 0
	var lasteventpiece:Int = 0

	var lastlines:IntArray = IntArray(0)
	/** ture if last erased line is Splitted */
	var split:Boolean = false

	/** ture if T-Spin */
	var tspin:Boolean = false

	/** true if T-Spin Mini */
	var tspinmini:Boolean = false

	/** EZ T-spin */
	var tspinez:Boolean = false

	/** true if B2B */
	var b2b:Boolean = false

	/** B2B counter */
	var b2bcount:Int = 0
	var b2bbuf:Int = 0

	/** Number of combos */
	var combo:Int = 0
	var combobuf:Int = 0

	/** T-Spin enable flag */
	var tspinEnable:Boolean = false

	/** EZ-T toggle */
	var tspinEnableEZ:Boolean = false

	/** Allow T-Spin with wallkicks */
	var tspinAllowKick:Boolean = false

	/** T-Spin Mini detection type */
	var tspinminiType:Int = 0

	/** Spin detection type */
	var spinCheckType:Int = 0

	/** All Spins flag */
	var useAllSpinBonus:Boolean = false

	/** B2B enable flag */
	var b2bEnable:Boolean = false

	/** Combo type */
	var comboType:Int = 0

	/** Number of frames before placed blocks disappear (-1:Disable) */
	var blockHidden:Int = 0

	/** Use alpha-blending for blockHidden */
	var blockHiddenAnim:Boolean = false

	/** Outline type */
	var blockOutlineType:Int = 0

	/** Show outline only flag.
	 * If enabled it does not show actual image of blocks. */
	var blockShowOutlineOnly:Boolean = false

	/** Hebo-hidden Enable flag */
	var heboHiddenEnable:Boolean = false

	/** Hebo-hidden Timer */
	var heboHiddenTimerNow:Int = 0

	/** Hebo-hidden Timer Max */
	var heboHiddenTimerMax:Int = 0

	/** Hebo-hidden Y coord */
	var heboHiddenYNow:Int = 0

	/** Hebo-hidden Y coord Limit */
	var heboHiddenYLimit:Int = 0

	/** Set when ARE or line delay is canceled */
	var delayCancel:Boolean = false

	/** Piece must move left after canceled delay */
	var delayCancelMoveLeft:Boolean = false

	/** Piece must move right after canceled delay */
	var delayCancelMoveRight:Boolean = false

	/** Use bone blocks [][][][] */
	var bone:Boolean = false

	/** Big blocks */
	var big:Boolean = false

	/** Big movement type (false:1cell true:2cell) */
	var bigmove:Boolean = false

	/** Halves the amount of lines cleared in Big mode */
	var bighalf:Boolean = false

	/** true if wallkick is used */
	var kickused:Boolean = false

	/** Field size (-1:Default) */
	var fieldWidth:Int = 0
	var fieldHeight:Int = 0
	var fieldHiddenHeight:Int = 0

	/** Ending mode (0:During the normal game) */
	var ending:Int = 0

	/** Enable staffroll challenge (Credits) in ending */
	var staffrollEnable:Boolean = false

	/** Disable death in staffroll challenge */
	var staffrollNoDeath:Boolean = false

	/** Update various statistics in staffroll challenge */
	var staffrollEnableStatistics:Boolean = false

	/** Frame cint */
	var framecolor:Int = 0

	/** Duration of Ready->Go */
	var readyStart:Int = 0
	var readyEnd:Int = 0
	var goStart:Int = 0
	var goEnd:Int = 0

	/** true if Ready->Go is already done */
	var readyDone:Boolean = false

	/** Number of lives */
	var lives:Int = 0

	/** Ghost piece flag */
	var ghost:Boolean = false

	/** Amount of meter */
	var meterValue:Int = 0

	/** Color of meter */
	var meterColor:Int = 0

	/** Amount of meter (layer 2) */
	var meterValueSub:Int = 0

	/** Color of meter (layer 2) */
	var meterColorSub:Int = 0

	/** Lag flag (Infinite length of ARE will happen
	 * after placing a piece until this flag is set to false) */
	var lagARE:Boolean = false

	/** Lag flag (Pause the game completely) */
	var lagStop:Boolean = false

	/** Field display size (-1 for mini, 1 for big, 0 for normal) */
	var displaysize:Int = 0

	/** Sound effects enable flag */
	var enableSE:Boolean = false

	/** Stops all other players when this player dies */
	var gameoverAll:Boolean = false

	/** Field visible flag (false for invisible challenge) */
	var isVisible:Boolean = false

	/** Piece preview visible flag */
	var isNextVisible:Boolean = false

	/** Hold piece visible flag */
	var isHoldVisible:Boolean = false

	/** Field edit screen: Cursor coord */
	var fldeditX:Int = 0
	var fldeditY:Int = 0

	/** Field edit screen: Selected cint */
	var fldeditColor:Int = 0

	/** Field edit screen: Previous game status number */
	lateinit var fldeditPreviousStat:Status

	/** Field edit screen: Frame counter */
	var fldeditFrames:Int = 0

	/** Next-skip during Ready->Go */
	var holdButtonNextSkip:Boolean = false

	/** Allow default text rendering
	 * (such as "READY", "GO!", "GAME OVER",etc) */
	var allowTextRenderByReceiver:Boolean = false

	/** RollRoll (Auto rotation) enable flag */
	var itemRollRollEnable:Boolean = false

	/** RollRoll (Auto rotation) interval */
	var itemRollRollInterval:Int = 0

	/** X-RAY enable flag */
	var itemXRayEnable:Boolean = false

	/** X-RAY counter */
	var itemXRayCount:Int = 0

	/** Color-block enable flag */
	var itemColorEnable:Boolean = false

	/** Color-block counter */
	var itemColorCount:Int = 0

	/** Gameplay-interruptable item */
	var interruptItemNumber:Int = 0

	/** Post-status of interruptable item */
	var interruptItemPreviousStat:Status = Status.MOVE

	/** Backup field for Mirror item */
	var interruptItemMirrorField:Field? = null

	/** A button direction -1=Auto(Use rule settings) 0=Left 1=Right */
	var owRotateButtonDefaultRight:Int = 0

	/** Block Skin (-1=Auto -2=Random 0orAbove=Fixed) */
	var owSkin:Int = 0

	/** Min/Max DAS (-1=Auto 0orAbove=Fixed) */
	var owMinDAS:Int = 0
	var owMaxDAS:Int = 0

	/** DAS delay (-1=Auto 0orAbove=Fixed) */
	var owDasDelay:Int = 0

	/** Reverse roles of up/down keys in-game */
	var owReverseUpDown:Boolean = false

	/** Diagonal move (-1=Auto 0=Disable 1=Enable) */
	var owMoveDiagonal:Int = 0

	/** Outline type (-1:Auto 0orAbove:Fixed) */
	var owBlockOutlineType:Int = 0

	/** Show outline only flag
	 * (-1:Auto 0:Always Normal 1:Always Outline Only) */
	var owBlockShowOutlineOnly:Int = 0
	/** Clear mode selection */
	var clearMode:ClearType = ClearType.LINE

	/** Size needed for a cint-group clear */
	var colorClearSize:Int = 0

	/** If true, cint clears will also clear adjacent garbage blocks. */
	var garbageColorClear:Boolean = false

	/** If true, each individual block is a random cint. */
	var randomBlockColor:Boolean = false

	/** If true, block in pieces are connected. */
	var connectBlocks:Boolean = false

	/** List of block colors to use for random block colors. */
	var blockColors:IntArray = BLOCK_COLORS_DEFAULT

	/** Number of colors in blockColors to use. */
	var numColors:Int = 0

	/** If true, line cint clears can be diagonal. */
	var lineColorDiagonals:Boolean = false

	/** If true, gems count as the same cint as
	 * their respectively-colored normal blocks */
	var gemSameColor:Boolean = false

	/** Delay for each step in cascade animations */
	var cascadeDelay:Int = 0

	/** Delay between landing and checking for clears in cascade */
	var cascadeClearDelay:Int = 0

	/** If true, cint clears will ignore hidden rows */
	var ignoreHidden:Boolean = false

	/** Set to true to process rainbow block effects, false to skip. */
	var rainbowAnimate:Boolean = false

	/** If true, the game will execute double rotation to I2 piece
	 * when regular rotation fails twice */
	var dominoQuickTurn:Boolean = false

	/** 0 = default, 1 = link by cint,
	 * 2 = link by cint but ignore links forcascade (Avalanche) */
	var sticky:Int = 0

	/** Hanabi発生間隔 */
	var temphanabi:Int = 0
	var inthanabi:Int = 0

	var explodSize:Array<IntArray> = EXPLOD_SIZE_DEFAULT

	init {
		owRotateButtonDefaultRight = -1
		owSkin = -1
		owMinDAS = -1
		owMaxDAS = -1
		owDasDelay = -1
		owReverseUpDown = false
		owMoveDiagonal = -1
		explodSize
	}

	/** Current AREの値を取得 (ルール設定も考慮）
	 * @return Current ARE
	 */
	val are:Int
		get() = if(speed.are<ruleopt.minARE&&ruleopt.minARE>=0) ruleopt.minARE
		else if(speed.are>ruleopt.maxARE&&ruleopt.maxARE>=0) ruleopt.maxARE else speed.are

	/** Current ARE after line clearの値を取得 (ルール設定も考慮）
	 * @return Current ARE after line clear
	 */
	val areLine:Int
		get() = if(speed.areLine<ruleopt.minARELine&&ruleopt.minARELine>=0) ruleopt.minARELine
		else if(speed.areLine>ruleopt.maxARELine&&ruleopt.maxARELine>=0) ruleopt.maxARELine else speed.areLine

	/** Current Line clear timeの値を取得 (ルール設定も考慮）
	 * @return Current Line clear time
	 */
	val lineDelay:Int
		get() = if(speed.lineDelay<ruleopt.minLineDelay&&ruleopt.minLineDelay>=0) ruleopt.minLineDelay
		else if(speed.lineDelay>ruleopt.maxLineDelay&&ruleopt.maxLineDelay>=0) ruleopt.maxLineDelay else speed.lineDelay

	/** Current 固定 timeの値を取得 (ルール設定も考慮）
	 * @return Current 固定 time
	 */
	val lockDelay:Int
		get() = if(speed.lockDelay<ruleopt.minLockDelay&&ruleopt.minLockDelay>=0) ruleopt.minLockDelay
		else if(speed.lockDelay>ruleopt.maxLockDelay&&ruleopt.maxLockDelay>=0) ruleopt.maxLockDelay else speed.lockDelay

	/** Current DASの値を取得 (ルール設定も考慮）
	 * @return Current DAS
	 */
	val das:Int
		get() = if(speed.das<owMinDAS&&owMinDAS>=0) owMinDAS
		else if(speed.das>owMaxDAS&&owMaxDAS>=0) owMaxDAS
		else if(speed.das<ruleopt.minDAS&&ruleopt.minDAS>=0) ruleopt.minDAS
		else if(speed.das>ruleopt.maxDAS&&ruleopt.maxDAS>=0) ruleopt.maxDAS else speed.das

	/** @return Controller.BUTTON_UP if controls are normal,
	 * Controller.BUTTON_DOWN if up/down are reversed
	 */
	val up:Int
		get() = if(owReverseUpDown) Controller.BUTTON_DOWN else Controller.BUTTON_UP

	/** @return Controller.BUTTON_DOWN if controls are normal,
	 * Controller.BUTTON_UP if up/down are reversed
	 */
	val down:Int
		get() = if(owReverseUpDown) Controller.BUTTON_UP else Controller.BUTTON_DOWN

	/** Current 横移動速度を取得
	 * @return 横移動速度
	 */
	val dasDelay:Int
		get() = if(owDasDelay>=0) owDasDelay else ruleopt.dasDelay

	/** 現在使用中のBlockスキン numberを取得
	 * @return Blockスキン number
	 */
	val skin:Int
		get() = if(owSkin>=0) owSkin else ruleopt.skin

	/** @return A buttonを押したときに左rotationするならfalse, 右rotationするならtrue
	 */
	val isRotateButtonDefaultRight:Boolean
		get() = if(owRotateButtonDefaultRight>=0) owRotateButtonDefaultRight!=0 else ruleopt.rotateButtonDefaultRight

	/** Is diagonal movement enabled?
	 * @return true if diagonal movement is enabled
	 */
	val isDiagonalMoveEnabled:Boolean
		get() = if(owMoveDiagonal>=0) owMoveDiagonal==1 else ruleopt.moveDiagonal

	/** 横移動 input のDirectionを取得
	 * @return -1:左 0:なし 1:右
	 */
	val moveDirection:Int
		get() =
			if(ruleopt.moveLeftAndRightAllow&&ctrl!!.isPress(Controller.BUTTON_LEFT)&&ctrl!!.isPress(Controller.BUTTON_RIGHT)) {
				when {
					ctrl!!.buttonTime[Controller.BUTTON_LEFT]>ctrl!!.buttonTime[Controller.BUTTON_RIGHT] ->
						if(ruleopt.moveLeftAndRightUsePreviousInput) -1 else 1
					ctrl!!.buttonTime[Controller.BUTTON_LEFT]<ctrl!!.buttonTime[Controller.BUTTON_RIGHT] ->
						if(ruleopt.moveLeftAndRightUsePreviousInput) 1 else -1
					else -> 0
				}
			} else if(ctrl!!.isPress(Controller.BUTTON_LEFT)) -1 else if(ctrl!!.isPress(Controller.BUTTON_RIGHT)) 1 else 0

	/** 移動 count制限を超過しているか判定
	 * @return 移動 count制限を超過したらtrue
	 */
	val isMoveCountExceed:Boolean
		get() = if(ruleopt.lockresetLimitShareCount) {
			extendedMoveCount+extendedRotateCount>=ruleopt.lockresetLimitMove&&ruleopt.lockresetLimitMove>=0
		} else ruleopt.lockresetLimitMove in 0..extendedMoveCount

	/** rotation count制限を超過しているか判定
	 * @return rotation count制限を超過したらtrue
	 */
	val isRotateCountExceed:Boolean
		get() = if(ruleopt.lockresetLimitShareCount) {
			extendedMoveCount+extendedRotateCount>=ruleopt.lockresetLimitMove&&ruleopt.lockresetLimitMove>=0
		} else ruleopt.lockresetLimitRotate in 0..extendedRotateCount

	/** ホールド可能かどうか判定
	 * @return ホールド可能ならtrue
	 */
	val isHoldOK:Boolean
		get() = (ruleopt.holdEnable&&!holdDisable&&(holdUsedCount<ruleopt.holdLimit||ruleopt.holdLimit<0)
			&&!initialHoldContinuousUse)

	/** Constants of main game status */
	enum class Status {
		NOTHING, SETTING, READY, MOVE, LOCKFLASH, LINECLEAR, ARE, ENDINGSTART, CUSTOM, EXCELLENT, GAMEOVER, RESULT,
		FIELDEDIT, INTERRUPTITEM
	}

	/** Constants of last successful movements */
	enum class LastMove {
		NONE, FALL_AUTO, FALL_SELF, SLIDE_AIR, SLIDE_GROUND, ROTATE_AIR, ROTATE_GROUND
	}

	/** Line gravity types */
	enum class LineGravity {
		NATIVE, CASCADE, CASCADE_SLOW
	}

	/** Clear mode settings */
	enum class ClearType {
		LINE, COLOR, LINE_COLOR, GEM_COLOR, LINE_GEM_BOMB, LINE_GEM_SPARK
	}

	/** READY前のInitialization */
	fun init() {
		//log.debug("GameEngine init() playerID:" + playerID);

		field = null
		ctrl = Controller()
		statistics.reset()
		speed.reset()
		gcount = 0
		replayData = ReplayData()

		if(!owner.replayMode) {
			versionMajor = GameManager.versionMajor
			versionMinor = GameManager.versionMinor
			versionMinorOld = GameManager.versionMinorOld

			val tempRand = Random()
			randSeed = tempRand.nextLong()
			if(owSkin==-2) owSkin = tempRand.nextInt(owner.receiver.skinMax)

			random = Random(randSeed)
		} else {
			versionMajor = owner.replayProp.getProperty("version.core.major", 0f)
			versionMinor = owner.replayProp.getProperty("version.core.minor", 0)
			versionMinorOld = owner.replayProp.getProperty("version.core.minor", 0f)

			replayData!!.readProperty(owner.replayProp, playerID)

			owRotateButtonDefaultRight =
				owner.replayProp.getProperty(playerID.toString()+".tuning.owRotateButtonDefaultRight", -1)
			owSkin = owner.replayProp.getProperty(playerID.toString()+".tuning.owSkin", -1)
			owMinDAS = owner.replayProp.getProperty(playerID.toString()+".tuning.owMinDAS", -1)
			owMaxDAS = owner.replayProp.getProperty(playerID.toString()+".tuning.owMaxDAS", -1)
			owDasDelay = owner.replayProp.getProperty(playerID.toString()+".tuning.owDasDelay", -1)
			owReverseUpDown = owner.replayProp.getProperty(playerID.toString()+".tuning.owReverseUpDown", false)
			owMoveDiagonal = owner.replayProp.getProperty(playerID.toString()+".tuning.owMoveDiagonal", -1)

			val tempRand = owner.replayProp.getProperty(playerID.toString()+".replay.randSeed", "0")
			randSeed = java.lang.Long.parseLong(tempRand, 16)
			random = Random(randSeed)

		}

		quitflag = false

		stat = Status.SETTING
		statc = IntArray(MAX_STATC)

		lastevent = EVENT_NONE
		lasteventpiece = Piece.PIECE_NONE
		lastlines = IntArray(0)
		lastline = fieldHeight

		isInGame = false
		gameActive = false
		timerActive = false
		gameStarted = false
		replayTimer = 0

		nowPieceObject = null
		nowPieceX = 0
		nowPieceY = 0
		nowPieceBottomY = 0
		nowPieceColorOverride = -1

		nextPieceArraySize = 1400
		nextPieceEnable = BooleanArray(Piece.PIECE_COUNT) {it<Piece.PIECE_STANDARD_COUNT}
		nextPieceArrayID = IntArray(0)
		nextPieceArrayObject = emptyArray()
		nextPieceCount = 0

		holdPieceObject = null
		holdDisable = false
		holdUsedCount = 0

		lineClearing = 0
		garbageClearing = 0
		lineGravityType = LineGravity.NATIVE
		chain = 0
		lineGravityTotalLines = 0

		lockDelayNow = 0

		dasCount = 0
		dasDirection = 0
		dasSpeedCount = dasDelay
		dasRepeat = false
		dasInstant = false
		shiftLock = 0

		initialRotateDirection = 0
		initialRotateLastDirection = 0
		initialHoldFlag = false
		initialRotateContinuousUse = false
		initialHoldContinuousUse = false

		nowPieceMoveCount = 0
		nowPieceRotateCount = 0
		nowPieceRotateFailCount = 0

		extendedMoveCount = 0
		extendedRotateCount = 0

		nowWallkickCount = 0
		nowUpwardWallkickCount = 0

		softdropFall = 0
		harddropFall = 0
		softdropContinuousUse = false
		harddropContinuousUse = false

		manualLock = false

		lastmove = LastMove.NONE
		split = false
		tspin = false
		tspinmini = false
		tspinez = false
		b2b = false
		b2bbuf = 0
		b2bcount = b2bbuf
		combobuf = 0
		combo = combobuf
		inthanabi = 0
		temphanabi = inthanabi

		tspinEnable = false
		tspinEnableEZ = false
		tspinAllowKick = true
		tspinminiType = TSPINMINI_TYPE_ROTATECHECK
		spinCheckType = SPINTYPE_4POINT
		useAllSpinBonus = false
		b2bEnable = false
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

		big = false
		bigmove = true
		bighalf = true

		kickused = false

		fieldWidth = -1
		fieldHeight = -1
		fieldHiddenHeight = -1

		ending = 0
		staffrollEnable = false
		staffrollNoDeath = false
		staffrollEnableStatistics = false

		framecolor = FRAME_COLOR_WHITE

		readyStart = READY_START
		readyEnd = READY_END
		goStart = GO_START
		goEnd = GO_END

		readyDone = false

		lives = 0

		ghost = true

		meterValue = 0
		meterColor = METER_COLOR_RED

		lagARE = false
		lagStop = false
		displaysize = if(playerID>=2) -1 else 0

		enableSE = true
		gameoverAll = true

		isNextVisible = true
		isHoldVisible = true
		isVisible = true

		holdButtonNextSkip = false

		allowTextRenderByReceiver = true

		itemRollRollEnable = false
		itemRollRollInterval = 30

		itemXRayEnable = false
		itemXRayCount = 0

		itemColorEnable = false
		itemColorCount = 0

		interruptItemNumber = INTERRUPTITEM_NONE

		clearMode = ClearType.LINE
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
		owner.mode?.also {
			it.playerInit(this, playerID)
			if(owner.replayMode) it.loadReplay(this, playerID, owner.replayProp)
		}
		owner.receiver.playerInit(this, playerID)
		ai?.also {
			it.shutdown(this, playerID)
			it.init(this, playerID)
		}
	}

	/** 終了処理 */
	fun shutdown() {
		//log.debug("GameEngine shutdown() playerID:" + playerID);

		if(ai!=null) ai!!.shutdown(this, playerID)
		/*owner = null
		ruleopt = null
		wallkick = null
		randomizer = null
		field = null
		ctrl = null
		statistics = null
		speed = null
		random = null
		replayData = null*/
	}

	/** ステータス counterInitialization */
	fun resetStatc() {
		for(i in statc.indices)
			statc[i] = 0
	}

	/** Sound effectsを再生する (enableSEがtrueのときだけ）
	 * @param name Sound effectsのName
	 */
	fun playSE(name:String) {
		if(enableSE) owner.receiver.playSE(name)
	}

	fun playSE(name:String, freq:Float) {
		if(enableSE) owner.receiver.playSE(name, freq)
	}

	fun loopSE(name:String) {
		if(enableSE) owner.receiver.loopSE(name)
	}

	fun stopSE(name:String) {
		owner.receiver.stopSE(name)
	}

	/** NEXTピースのIDを取得
	 * @param c 取得したいNEXTの位置
	 * @return NEXTピースのID
	 */
	fun getNextID(c:Int):Int {
		if(nextPieceArrayObject.isNullOrEmpty()) return Piece.PIECE_NONE
		nextPieceArrayID.let {
			var c2 = c
			while(it.size in 1..c2) c2 -= it.size
			return it[c2]
		}
	}

	/** NEXTピースのオブジェクトを取得
	 * @param c 取得したいNEXTの位置
	 * @return NEXTピースのオブジェクト
	 */
	fun getNextObject(c:Int):Piece? {
		if(nextPieceArrayObject.isNullOrEmpty()) return null
		nextPieceArrayObject.let {
			var c2 = c
			while(it.size in 1..c2)
				c2 -= it.size
			return it[c2]
		}
	}

	/** NEXTピースのオブジェクトのコピーを取得
	 * @param c 取得したいNEXTの位置
	 * @return NEXTピースのオブジェクトのコピー
	 */
	fun getNextObjectCopy(c:Int):Piece? = getNextObject(c)?.let {Piece(it)}

	/** 見え／消えRoll 状態のfieldを通常状態に戻す */
	fun resetFieldVisible() {
		field?.let {f ->
			for(x in 0 until f.width) for(y in 0 until f.height)
				f.getBlock(x, y)?.run {
					if(cint>Block.BLOCK_COLOR_NONE) {
						alpha = 1f
						darkness = 0f
						setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true)
						setAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, true)
					}
				}
		}
	}

	/** ソフト・Hard drop・先行ホールド・先行rotationの使用制限解除 */
	fun checkDropContinuousUse() {
		if(gameActive) {
			if(!ctrl!!.isPress(down)||!ruleopt.softdropLimit) softdropContinuousUse = false
			if(!ctrl!!.isPress(up)||!ruleopt.harddropLimit) harddropContinuousUse = false
			if(!ctrl!!.isPress(Controller.BUTTON_D)||!ruleopt.holdInitialLimit) initialHoldContinuousUse = false
			if(!ruleopt.rotateInitialLimit) initialRotateContinuousUse = false

			if(initialRotateContinuousUse) {
				var dir = 0
				if(ctrl!!.isPress(Controller.BUTTON_A)||ctrl!!.isPress(Controller.BUTTON_C))
					dir = -1
				else if(ctrl!!.isPress(Controller.BUTTON_B)) dir = 1
				else if(ctrl!!.isPress(Controller.BUTTON_E)) dir = 2

				if(initialRotateLastDirection!=dir||dir==0) initialRotateContinuousUse = false
			}
		}
	}

	/** 横溜め処理 */
	fun padRepeat() {
		if(moveDirection!=0) dasCount++
		else if(!ruleopt.dasStoreChargeOnNeutral) dasCount = 0
		dasDirection = moveDirection
	}

	/** Called if delay doesn't allow charging but dasRedirectInDelay == true
	 * Updates dasDirection so player can change direction without dropping
	 * charge on entry. */
	fun dasRedirect() {
		dasDirection = moveDirection
	}

	/** T-Spin routine
	 * @param x X coord
	 * @param y Y coord
	 * @param p Current p object
	 * @param f Field object
	 */
	fun setTSpin(x:Int, y:Int, p:Piece?, f:Field?) {
		if(p==null||f==null||p.id!=Piece.PIECE_T||(!tspinAllowKick&&kickused)) {
			tspin = false
			return
		}

		if(spinCheckType==SPINTYPE_4POINT) {
			if(tspinminiType==TSPINMINI_TYPE_ROTATECHECK) {
				if(nowPieceObject!!.checkCollision(nowPieceX, nowPieceY, getRotateDirection(-1), f)
					&&nowPieceObject!!.checkCollision(nowPieceX, nowPieceY, getRotateDirection(1), f))
					tspinmini = true
			} else if(tspinminiType==TSPINMINI_TYPE_WALLKICKFLAG) tspinmini = kickused

			val tx = IntArray(4)
			val ty = IntArray(4)

			// Setup 4-point coordinates
			if(p.big) {
				tx[0] = 1
				ty[0] = 1
				tx[1] = 4
				ty[1] = 1
				tx[2] = 1
				ty[2] = 4
				tx[3] = 4
				ty[3] = 4
			} else {
				tx[0] = 0
				ty[0] = 0
				tx[1] = 2
				ty[1] = 0
				tx[2] = 0
				ty[2] = 2
				tx[3] = 2
				ty[3] = 2
			}
			for(i in tx.indices)
				if(p.big) {
					tx[i] += ruleopt.pieceOffsetX[p.id][p.direction]*2
					ty[i] += ruleopt.pieceOffsetY[p.id][p.direction]*2
				} else {
					tx[i] += ruleopt.pieceOffsetX[p.id][p.direction]
					ty[i] += ruleopt.pieceOffsetY[p.id][p.direction]
				}

			// Check the corner of the T p
			var count = 0

			for(i in tx.indices)
				if(f.getBlockColor(x+tx[i], y+ty[i])!=Block.BLOCK_COLOR_NONE) count++

			if(count>=3) tspin = true
		} else if(spinCheckType==SPINTYPE_IMMOBILE)
			if(p.checkCollision(x, y-1, f)&&p.checkCollision(x+1, y, f)
				&&p.checkCollision(x-1, y, f)) {
				tspin = true
				val copyField = Field(f)
				p.placeToField(x, y, copyField)
				if(copyField.checkLineNoFlag()==1&&kickused) tspinmini = true
			} else if(tspinEnableEZ&&kickused) {
				tspin = true
				tspinez = true
			}
	}

	/** Spin判定(全スピンルールのとき用)
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param p Current Blockピース
	 * @param f field
	 */
	fun setAllSpin(x:Int, y:Int, p:Piece?, f:Field?) {
		tspin = false
		tspinmini = false
		tspinez = false

		if((!tspinAllowKick&&kickused)||p==null||f==null) return
		if(p.big) return
		if(spinCheckType==SPINTYPE_4POINT) {

			val offsetX = ruleopt.pieceOffsetX[p.id][p.direction]
			val offsetY = ruleopt.pieceOffsetY[p.id][p.direction]

			for(i in 0 until Piece.SPINBONUSDATA_HIGH_X[p.id][p.direction].size/2) {
				var isHighSpot1 = false
				var isHighSpot2 = false
				var isLowSpot1 = false
				var isLowSpot2 = false

				if(!f.getBlockEmptyF(x+Piece.SPINBONUSDATA_HIGH_X[p.id][p.direction][i*2]+offsetX, y
						+Piece.SPINBONUSDATA_HIGH_Y[p.id][p.direction][i*2]+offsetY))
					isHighSpot1 = true
				if(!f.getBlockEmptyF(x+Piece.SPINBONUSDATA_HIGH_X[p.id][p.direction][i*2+1]+offsetX, y
						+Piece.SPINBONUSDATA_HIGH_Y[p.id][p.direction][i*2+1]+offsetY))
					isHighSpot2 = true
				if(!f.getBlockEmptyF(x+Piece.SPINBONUSDATA_LOW_X[p.id][p.direction][i*2]+offsetX, y
						+Piece.SPINBONUSDATA_LOW_Y[p.id][p.direction][i*2]+offsetY))
					isLowSpot1 = true
				if(!f.getBlockEmptyF(x+Piece.SPINBONUSDATA_LOW_X[p.id][p.direction][i*2+1]+offsetX, y
						+Piece.SPINBONUSDATA_LOW_Y[p.id][p.direction][i*2+1]+offsetY))
					isLowSpot2 = true

				//log.debug(isHighSpot1 + "," + isHighSpot2 + "," + isLowSpot1 + "," + isLowSpot2);

				if(isHighSpot1&&isHighSpot2&&(isLowSpot1||isLowSpot2))
					tspin = true
				else if(!tspin&&isLowSpot1&&isLowSpot2&&(isHighSpot1||isHighSpot2)) {
					tspin = true
					tspinmini = true
				}
			}
		} else if(spinCheckType==SPINTYPE_IMMOBILE)
			if(p.checkCollision(x, y-1, f)&&p.checkCollision(x+1, y, f)
				&&p.checkCollision(x-1, y, f)) {
				tspin = true
				val copyField = Field(f)
				p.placeToField(x, y, copyField)
				if(p.height+1!=copyField.checkLineNoFlag()&&kickused) tspinmini = true
				//if((copyField.checkLineNoFlag() == 1) && (kickused == true)) tspinmini = true;
			} else if(tspinEnableEZ&&kickused) {
				tspin = true
				tspinez = true
			}

	}

	/** ピースが出現するX-coordinateを取得
	 * @param fld field
	 * @param piece Piece
	 * @return 出現位置のX-coordinate
	 */
	fun getSpawnPosX(fld:Field?, piece:Piece?):Int {
		var x = 0
		piece?.let {
			x = -1+(fld?.width ?: 0-it.width+1)/2
			if(big&&bigmove&&x%2!=0) x++

			x += if(big) ruleopt.pieceSpawnXBig[it.id][it.direction]
			else ruleopt.pieceSpawnX[it.id][it.direction]

		}
		return x
	}

	/** ピースが出現するY-coordinateを取得
	 * @param piece Piece
	 * @return 出現位置のY-coordinate
	 */
	fun getSpawnPosY(piece:Piece?):Int {
		var y = 0
		piece?.let {
			if(ruleopt.pieceEnterAboveField&&!ruleopt.fieldCeiling) {
				y = -1-it.maximumBlockY
				if(big) y--
			} else y = -it.minimumBlockY

			y += if(big) ruleopt.pieceSpawnYBig[it.id][it.direction]
			else ruleopt.pieceSpawnY[it.id][it.direction]
		}
		return y
	}

	/** rotation buttonを押したあとのピースのDirectionを取得
	 * @param move rotationDirection (-1:左 1:右 2:180度）
	 * @return rotation buttonを押したあとのピースのDirection
	 */
	fun getRotateDirection(move:Int):Int {
		var rt = move
		nowPieceObject?.let {rt = it.direction+move}

		if(move==2) {
			if(rt>3) rt -= 4
			if(rt<0) rt += 4
		} else {
			if(rt>3) rt = 0
			if(rt<0) rt = 3
		}

		return rt
	}

	/** 先行rotationと先行ホールドの処理 */
	fun initialRotate() {
		initialRotateDirection = 0
		initialHoldFlag = false

		if(ruleopt.rotateInitial&&!initialRotateContinuousUse) {
			var dir = 0
			if(ctrl!!.isPress(Controller.BUTTON_A)||ctrl!!.isPress(Controller.BUTTON_C))
				dir = -1
			else if(ctrl!!.isPress(Controller.BUTTON_B)) dir = 1
			else if(ctrl!!.isPress(Controller.BUTTON_E)) dir = 2
			initialRotateDirection = dir
		}

		if(ctrl!!.isPress(Controller.BUTTON_D)&&ruleopt.holdInitial&&isHoldOK) {
			initialHoldFlag = true
			initialHoldContinuousUse = true
			playSE("initialhold")
		}
	}

	/** fieldのBlock stateを更新 */
	private fun fieldUpdate() {
		var outlineOnly = blockShowOutlineOnly // Show outline only flag
		if(owBlockShowOutlineOnly==0) outlineOnly = false
		if(owBlockShowOutlineOnly==1) outlineOnly = true

		field?.let {f ->
			for(i in 0 until f.width)
				for(j in f.hiddenHeight*-1 until f.height) {
					f.getBlock(i, j)?.run {
						if(cint>=Block.BLOCK_COLOR_GRAY) {
							if(elapsedFrames<0) {
								if(!getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE)) darkness = 0f
							} else if(elapsedFrames<ruleopt.lockflash) {
								darkness = -.8f
								if(outlineOnly) {
									setAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, true)
									setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, false)
									setAttribute(Block.BLOCK_ATTRIBUTE_BONE, false)
								}
							} else {
								darkness = 0f
								setAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, true)
								if(outlineOnly) {
									setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, false)
									setAttribute(Block.BLOCK_ATTRIBUTE_BONE, false)
								}
							}

							if(blockHidden!=-1&&elapsedFrames>=blockHidden-10&&gameActive) {
								if(blockHiddenAnim) {
									alpha -= .1f
									if(alpha<0.0f) alpha = 0.0f
								}

								if(elapsedFrames>=blockHidden) {
									alpha = 0.0f
									setAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, false)
									setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, false)
								}
							}

							if(elapsedFrames>=0) elapsedFrames++
						}
					}
				}

			// X-RAY
			if(gameActive) {
				if(itemXRayEnable) {
					for(i in 0 until f.width)
						for(j in f.hiddenHeight*-1 until f.height) {
							f.getBlock(i, j)?.apply {
								if(cint>=Block.BLOCK_COLOR_GRAY) {
									setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, itemXRayCount%36==i)
									setAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, itemXRayCount%36==i)
								}
							}
						}
					itemXRayCount++
				} else itemXRayCount = 0

				// COLOR
				if(itemColorEnable) {
					for(i in 0 until f.width)
						for(j in f.hiddenHeight*-1 until f.height) {
							var bright = j
							if(bright>=5) bright = 9-bright
							bright = 40-((20-i+bright)*4+itemColorCount)%40
							if(bright>=0&&bright<ITEM_COLOR_BRIGHT_TABLE.size) bright = 10-ITEM_COLOR_BRIGHT_TABLE[bright]
							if(bright>10) bright = 10

							f.getBlock(i, j)?.apply {
								alpha = bright*.1f
								setAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, false)
								setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true)
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

		owner.replayProp.setProperty("version.core", versionMajor.toString()+"."+versionMinor)
		owner.replayProp.setProperty("version.core.major", versionMajor)
		owner.replayProp.setProperty("version.core.minor", versionMinor)
		owner.replayProp.setProperty("version.core.dev", versionIsDevBuild)

		owner.replayProp.setProperty(playerID.toString()+".replay.randSeed", java.lang.Long.toString(randSeed, 16))

		replayData!!.writeProperty(owner.replayProp, playerID, replayTimer)
		statistics.writeProperty(owner.replayProp, playerID)
		ruleopt.writeProperty(owner.replayProp, playerID)

		if(playerID==0) {
			owner.mode?.let {owner.replayProp.setProperty("name.mode", it.name)}
			owner.replayProp.setProperty("name.rule", ruleopt.strRuleName)

			// Local timestamp
			val currentTime = Calendar.getInstance()
			val month = currentTime.get(Calendar.MONTH)+1
			val strDate = String.format("%04d/%02d/%02d", currentTime.get(Calendar.YEAR), month, currentTime.get(Calendar.DATE))
			val strTime =
				String.format("%02d:%02d:%02d", currentTime.get(Calendar.HOUR_OF_DAY), currentTime.get(Calendar.MINUTE), currentTime.get(Calendar.SECOND))
			owner.replayProp.setProperty("timestamp.date", strDate)
			owner.replayProp.setProperty("timestamp.time", strTime)

			// GMT timestamp
			owner.replayProp.setProperty("timestamp.gmt", GeneralUtil.exportCalendarString())
		}

		owner.replayProp.setProperty(playerID.toString()+".tuning.owRotateButtonDefaultRight", owRotateButtonDefaultRight)
		owner.replayProp.setProperty(playerID.toString()+".tuning.owSkin", owSkin)
		owner.replayProp.setProperty(playerID.toString()+".tuning.owMinDAS", owMinDAS)
		owner.replayProp.setProperty(playerID.toString()+".tuning.owMaxDAS", owMaxDAS)
		owner.replayProp.setProperty(playerID.toString()+".tuning.owDasDelay", owDasDelay)
		owner.replayProp.setProperty(playerID.toString()+".tuning.owReverseUpDown", owReverseUpDown)
		owner.replayProp.setProperty(playerID.toString()+".tuning.owMoveDiagonal", owMoveDiagonal)

		owner.mode?.also {it.saveReplay(this, playerID, owner.replayProp)}
	}

	/** fieldエディット画面に入る処理 */
	fun enterFieldEdit() {
		fldeditPreviousStat = stat
		stat = Status.FIELDEDIT
		fldeditX = 0
		fldeditY = 0
		fldeditColor = Block.BLOCK_COLOR_GRAY
		fldeditFrames = 0
		owner.menuOnly = false
		createFieldIfNeeded()
	}

	/** fieldをInitialization (まだ存在しない場合） */
	fun createFieldIfNeeded() {
		if(!gameActive&&!owner.replayMode) {
			val tempRand = Random()
			randSeed = tempRand.nextLong()
			random = Random(randSeed)
		}
		if(fieldWidth<0) fieldWidth = ruleopt.fieldWidth
		if(fieldHeight<0) fieldHeight = ruleopt.fieldHeight
		if(fieldHiddenHeight<0) fieldHiddenHeight = ruleopt.fieldHiddenHeight
		if(field==null) field = Field(fieldWidth, fieldHeight, fieldHiddenHeight, ruleopt.fieldCeiling)
	}

	/** Call this if the game has ended */
	fun gameEnded() {
		if(endTime==0L) {
			endTime = System.nanoTime()
			statistics.gamerate = (replayTimer/(0.00000006*(endTime-startTime))).toFloat()
		}
		gameActive = false
		timerActive = false
		isInGame = false
		ai?.also {it.shutdown(this, playerID)}
	}

	/** ゲーム stateの更新 */
	fun update() {
		if(gameActive) {
			// リプレイ関連の処理
			if(!owner.replayMode||owner.replayRerecord) {
				// AIの button処理
				ai?.let {ai ->
					if(!aiShowHint) ai.setControl(this, playerID, ctrl!!)
					else {
						aiHintReady = ai.thinkComplete||ai.thinkCurrentPieceNo>0&&ai.thinkCurrentPieceNo<=ai.thinkLastPieceNo
						if(aiHintReady) {
							aiHintPiece = null
							if(ai.bestHold) {
								if(holdPieceObject!=null) {
									aiHintPiece = Piece(holdPieceObject!!)
								} else {
									aiHintPiece = getNextObjectCopy(nextPieceCount)
									if(!aiHintPiece!!.offsetApplied)
										aiHintPiece!!.applyOffsetArray(ruleopt.pieceOffsetX[aiHintPiece!!.id], ruleopt.pieceOffsetY[aiHintPiece!!.id])
								}
							} else nowPieceObject?.let {aiHintPiece = Piece(it)}
						}
					}
				}
				// input 状態をリプレイに記録
				replayData!!.setInputData(ctrl!!.buttonBit, replayTimer)
			} else // input 状態をリプレイから読み込み
				ctrl!!.buttonBit = replayData!!.getInputData(replayTimer)
			replayTimer++
		}

		//  button input timeの更新
		ctrl!!.updateButtonTime()

		// 最初の処理
		owner.mode?.also {it.onFirst(this, playerID)}
		owner.receiver.onFirst(this, playerID)
		ai?.also {if(!owner.replayMode||owner.replayRerecord) it.onFirst(this, playerID)}
		fpf = 0
		// 各ステータスの処理
		if(!lagStop)
			when(stat) {
				GameEngine.Status.NOTHING -> {
				}
				GameEngine.Status.SETTING -> statSetting()
				GameEngine.Status.READY -> statReady()
				GameEngine.Status.MOVE -> {
					dasRepeat = true
					dasInstant = false
					while(dasRepeat)
						statMove()
				}
				GameEngine.Status.LOCKFLASH -> statLockFlash()
				GameEngine.Status.LINECLEAR -> statLineClear()
				GameEngine.Status.ARE -> statARE()
				GameEngine.Status.ENDINGSTART -> statEndingStart()
				GameEngine.Status.CUSTOM -> statCustom()
				GameEngine.Status.EXCELLENT -> statExcellent()
				GameEngine.Status.GAMEOVER -> statGameOver()
				GameEngine.Status.RESULT -> statResult()
				GameEngine.Status.FIELDEDIT -> statFieldEdit()
				GameEngine.Status.INTERRUPTITEM -> statInterruptItem()
			}

		// fieldのBlock stateや統計情報を更新
		fieldUpdate()
		//if(ending==0||staffrollEnableStatistics) statistics.update()

		// 最後の処理
		if(inthanabi>0) inthanabi--
		owner.mode?.also {it.onLast(this, playerID)}
		owner.receiver.onLast(this, playerID)
		ai?.also {if(!owner.replayMode||owner.replayRerecord) it.onLast(this, playerID)}

		// Timer増加
		if(gameActive&&timerActive) statistics.time++
		if(temphanabi>0&&inthanabi<=0) {
			owner.receiver.shootFireworks(this, playerID)
			temphanabi--
			inthanabi += HANABI_INTERVAL
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
		owner.mode?.also {it.renderFirst(this, playerID)}
		owner.receiver.renderFirst(this, playerID)

		if(rainbowAnimate) Block.updateRainbowPhase(this)

		// 各ステータスの処理
		when(stat) {
			GameEngine.Status.NOTHING -> {
			}
			GameEngine.Status.SETTING -> {
				owner.mode?.also {it.renderSetting(this, playerID)}
				owner.receiver.renderSetting(this, playerID)
			}
			GameEngine.Status.READY -> {
				owner.mode?.also {it.renderReady(this, playerID)}
				owner.receiver.renderReady(this, playerID)
			}
			GameEngine.Status.MOVE -> {
				owner.mode?.also {it.renderMove(this, playerID)}
				owner.receiver.renderMove(this, playerID)
			}
			GameEngine.Status.LOCKFLASH -> {
				owner.mode?.also {it.renderLockFlash(this, playerID)}
				owner.receiver.renderLockFlash(this, playerID)
			}
			GameEngine.Status.LINECLEAR -> {
				owner.mode?.also {it.renderLineClear(this, playerID)}
				owner.receiver.renderLineClear(this, playerID)
			}
			GameEngine.Status.ARE -> {
				owner.mode?.also {it.renderARE(this, playerID)}
				owner.receiver.renderARE(this, playerID)
			}
			GameEngine.Status.ENDINGSTART -> {
				owner.mode?.also {it.renderEndingStart(this, playerID)}
				owner.receiver.renderEndingStart(this, playerID)
			}
			GameEngine.Status.CUSTOM -> {
				owner.mode?.also {it.renderCustom(this, playerID)}
				owner.receiver.renderCustom(this, playerID)
			}
			GameEngine.Status.EXCELLENT -> {
				owner.mode?.also {it.renderExcellent(this, playerID)}
				owner.receiver.renderExcellent(this, playerID)
			}
			GameEngine.Status.GAMEOVER -> {
				owner.mode?.also {it.renderGameOver(this, playerID)}
				owner.receiver.renderGameOver(this, playerID)
			}
			GameEngine.Status.RESULT -> {
				owner.mode?.also {it.renderResult(this, playerID)}
				owner.receiver.renderResult(this, playerID)
			}
			GameEngine.Status.FIELDEDIT -> {
				owner.mode?.also {it.renderFieldEdit(this, playerID)}
				owner.receiver.renderFieldEdit(this, playerID)
			}
			GameEngine.Status.INTERRUPTITEM -> {
			}
		}

		if(owner.showInput) {
			owner.mode?.also {it.renderInput(this, playerID)}
			owner.receiver.renderInput(this, playerID)
		}
		ai?.also {
			if(aiShowState) it.renderState(this, playerID)
			if(aiShowHint) it.renderHint(this, playerID)
		}

		// 最後の処理
		owner.mode?.also {it.renderLast(this, playerID)}
		owner.receiver.renderLast(this, playerID)
	}

	/** 開始前の設定画面のときの処理 */
	private fun statSetting() {
		//  event 発生
		owner.bgmStatus.fadesw = false
		owner.bgmStatus.bgm = BGMStatus.BGM.MENU_3
		owner.mode?.also {if(it.onSetting(this, playerID)) return}
		owner.receiver.onSetting(this, playerID)

		// Mode側が何もしない場合はReady画面へ移動
		stat = Status.READY
		resetStatc()
	}

	/** Ready→Goのときの処理 */
	private fun statReady() {
		//  event 発生
		owner.mode?.also {if(it.onReady(this, playerID)) return}
		owner.receiver.onReady(this, playerID)

		// 横溜め
		if(ruleopt.dasInReady&&gameActive) padRepeat()
		else if(ruleopt.dasRedirectInDelay) dasRedirect()

		// Initialization
		if(statc[0]==0) {

			if(!readyDone&&!owner.bgmStatus.fadesw&&owner.bgmStatus.bgm.id<0&&
				owner.bgmStatus.bgm.id !in BGMStatus.BGM.FINALE_1.id..BGMStatus.BGM.FINALE_3.id)
				owner.bgmStatus.fadesw = true
			// fieldInitialization
			createFieldIfNeeded()
			if(owner.replayMode) {
				val tempRand = owner.replayProp.getProperty(playerID.toString()+".replay.randSeed", "0")
				randSeed = java.lang.Long.parseLong(tempRand, 16)
				random = Random(randSeed)
				nextPieceArrayID = IntArray(0)
				nextPieceArrayObject = emptyArray()
			}
			// NEXTピース作成
			if(nextPieceArrayID.isEmpty()) {
				// 出現可能なピースが1つもない場合は全て出現できるようにする
				if(nextPieceEnable.all {false}) nextPieceEnable = BooleanArray(Piece.PIECE_COUNT) {true}

				// NEXTピースの出現順を作成
				if(randomizer!=null)
					randomizer!!.setState(nextPieceEnable, randSeed)
				else
					randomizer = MemorylessRandomizer(nextPieceEnable, randSeed)

				nextPieceArrayID = IntArray(nextPieceArraySize) {randomizer!!.next()}

			}
			// NEXTピースのオブジェクトを作成
			if(nextPieceArrayObject.isEmpty()) {
				nextPieceArrayObject = Array(nextPieceArrayID.size) {Piece(nextPieceArrayID[it])}

				nextPieceArrayObject.forEach {p ->
					p?.let {
						it.direction = ruleopt.pieceDefaultDirection[it.id]
						if(it.direction>=Piece.DIRECTION_COUNT)
							it.direction = random.nextInt(Piece.DIRECTION_COUNT)
						it.connectBlocks = connectBlocks
						it.setColor(ruleopt.pieceColor[it.id])
						it.setSkin(skin)
						it.updateConnectData()
						it.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true)
						it.setAttribute(Block.BLOCK_ATTRIBUTE_BONE, bone)

						if(randomBlockColor) {
							if(blockColors.size<numColors||numColors<1) numColors = blockColors.size
							val size = it.maxBlock
							val colors = IntArray(size)
							for(j in 0 until size)
								colors[j] = blockColors[random.nextInt(numColors)]
							it.setColor(colors)
							it.updateConnectData()
						}
						if(clearMode==ClearType.LINE_GEM_BOMB||clearMode==ClearType.LINE_GEM_SPARK)
							it.block[random.nextInt(it.maxBlock)].cint += 7
					}
				}
			}

			if(!readyDone) {
				//  button input状態リセット
				ctrl!!.reset()
				// ゲーム中 flagON
				gameActive = true
				gameStarted = true
				isInGame = true
			}
		}

		// READY音
		if(statc[0]==readyStart) playSE("ready")

		// GO音
		if(statc[0]==goStart) playSE("go")

		// NEXTスキップ
		if(statc[0] in 1..(goEnd-1)&&holdButtonNextSkip&&isHoldOK&&ctrl!!.isPush(Controller.BUTTON_D)) {
			playSE("initialhold")
			holdPieceObject = getNextObjectCopy(nextPieceCount)
			holdPieceObject!!.applyOffsetArray(ruleopt.pieceOffsetX[holdPieceObject!!.id], ruleopt.pieceOffsetY[holdPieceObject!!.id])
			nextPieceCount++
			if(nextPieceCount<0) nextPieceCount = 0
		}

		// 開始
		if(statc[0]>=goEnd) {
			owner.mode?.also {it.startGame(this, playerID)}
			owner.receiver.startGame(this, playerID)
			owner.bgmStatus.fadesw = false
			initialRotate()
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
		owner.mode?.also {if(it.onMove(this, playerID)) return}
		owner.receiver.onMove(this, playerID)
		val field = field ?: return
		// 横溜めInitialization
		val moveDirection = moveDirection

		if(statc[0]>0||ruleopt.dasInMoveFirstFrame)
			if(dasDirection!=moveDirection) {
				dasDirection = moveDirection
				if(!(dasDirection==0&&ruleopt.dasStoreChargeOnNeutral)) dasCount = 0
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
						holdPieceObject = getNextObjectCopy(nextPieceCount)
						holdPieceObject!!.applyOffsetArray(ruleopt.pieceOffsetX[holdPieceObject!!.id], ruleopt.pieceOffsetY[holdPieceObject!!.id])
						nextPieceCount++
						if(nextPieceCount<0) nextPieceCount = 0

						if(bone)
							getNextObject(nextPieceCount+ruleopt.nextDisplay-1)!!.setAttribute(Block.BLOCK_ATTRIBUTE_BONE, true)

						nowPieceObject = getNextObjectCopy(nextPieceCount)
						nextPieceCount++
						if(nextPieceCount<0) nextPieceCount = 0
					} else {
						// 2回目以降
						val pieceTemp = holdPieceObject
						holdPieceObject = getNextObjectCopy(nextPieceCount)
						holdPieceObject!!.applyOffsetArray(ruleopt.pieceOffsetX[holdPieceObject!!.id], ruleopt.pieceOffsetY[holdPieceObject!!.id])
						nowPieceObject = pieceTemp
						nextPieceCount++
						if(nextPieceCount<0) nextPieceCount = 0
					}
				} else // 通常ホールド
					if(holdPieceObject==null) {
						// 1回目
						nowPieceObject!!.big = false
						holdPieceObject = nowPieceObject
						nowPieceObject = getNextObjectCopy(nextPieceCount)
						nextPieceCount++
						if(nextPieceCount<0) nextPieceCount = 0
					} else {
						// 2回目以降
						nowPieceObject!!.big = false
						val pieceTemp = holdPieceObject
						holdPieceObject = nowPieceObject
						nowPieceObject = pieceTemp
					}

				// Directionを戻す
				if(ruleopt.holdResetDirection&&ruleopt.pieceDefaultDirection[holdPieceObject!!.id]<Piece.DIRECTION_COUNT) {
					holdPieceObject!!.direction = ruleopt.pieceDefaultDirection[holdPieceObject!!.id]
					holdPieceObject!!.updateConnectData()
				}

				// 使用した count+1
				holdUsedCount++
				statistics.totalHoldUsed++

				// ホールド無効化
				initialHoldFlag = false
				holdDisable = true
			}
			if(framecolor!=FRAME_SKIN_GB)playSE("piece${getNextObject(nextPieceCount)!!.id}")

			if(!nowPieceObject!!.offsetApplied)
				nowPieceObject!!.applyOffsetArray(ruleopt.pieceOffsetX[nowPieceObject!!.id], ruleopt.pieceOffsetY[nowPieceObject!!.id])

			nowPieceObject!!.big = big

			// 出現位置 (横）
			nowPieceX = getSpawnPosX(field, nowPieceObject)

			// 出現位置 (縦）
			nowPieceY = getSpawnPosY(nowPieceObject)

			nowPieceBottomY = nowPieceObject!!.getBottom(nowPieceX, nowPieceY, field)
			nowPieceColorOverride = -1

			if(itemRollRollEnable) nowPieceColorOverride = Block.BLOCK_COLOR_GRAY

			gcount = if(speed.gravity>speed.denominator&&speed.denominator>0)
				speed.gravity%speed.denominator
			else 0

			lockDelayNow = 0
			dasSpeedCount = dasDelay
			dasRepeat = false
			dasInstant = false
			extendedMoveCount = 0
			extendedRotateCount = 0
			softdropFall = 0
			harddropFall = 0
			manualLock = false
			nowPieceMoveCount = 0
			nowPieceRotateCount = 0
			nowPieceRotateFailCount = 0
			nowWallkickCount = 0
			nowUpwardWallkickCount = 0
			lineClearing = 0
			garbageClearing = 0
			lastmove = LastMove.NONE
			kickused = false
			tspin = false
			tspinmini = false
			tspinez = false

			getNextObject(nextPieceCount+ruleopt.nextDisplay-1)!!.setAttribute(Block.BLOCK_ATTRIBUTE_BONE, bone)

			if(ending==0) timerActive = true

			if(ai!=null&&(!owner.replayMode||owner.replayRerecord)) ai!!.newPiece(this, playerID)
		}

		checkDropContinuousUse()

		var softdropUsed = false // この frame にSoft dropを使ったらtrue
		var softdropFallNow = 0 // この frame のSoft dropで落下した段count

		var updown = false // Up下同時押し flag
		if(ctrl!!.isPress(up)&&ctrl!!.isPress(down)) updown = true

		if(!dasInstant) {

			// ホールド
			if(ctrl!!.isPush(Controller.BUTTON_D)||initialHoldFlag)
				if(isHoldOK) {
					statc[0] = 0
					statc[1] = 1
					if(!initialHoldFlag) playSE("hold")
					initialHoldContinuousUse = true
					initialHoldFlag = false
					holdDisable = true
					initialRotate() //Hold swap triggered IRS
					statMove()
					return
				} else if(statc[0]>0&&!initialHoldFlag) playSE("holdfail")

			// rotation
			val onGroundBeforeRotate = nowPieceObject!!.checkCollision(nowPieceX, nowPieceY+1, field)
			var move = 0
			var rotated = false

			if(initialRotateDirection!=0) {
				move = initialRotateDirection
				initialRotateLastDirection = initialRotateDirection
				initialRotateContinuousUse = true
				playSE("initialrotate")
			} else if(statc[0]>0||ruleopt.moveFirstFrame) {
				if(itemRollRollEnable&&replayTimer%itemRollRollInterval==0) move = 1 // Roll Roll

				//  button input
				if(ctrl!!.isPush(Controller.BUTTON_A)||ctrl!!.isPush(Controller.BUTTON_C))
					move = -1
				else if(ctrl!!.isPush(Controller.BUTTON_B))
					move = 1
				else if(ctrl!!.isPush(Controller.BUTTON_E)) move = 2

				if(move!=0) {
					initialRotateLastDirection = move
					initialRotateContinuousUse = true
				}
			}

			if(!ruleopt.rotateButtonAllowDouble&&move==2) move = -1
			if(!ruleopt.rotateButtonAllowReverse&&move==1) move = -1
			if(isRotateButtonDefaultRight&&move!=2) move *= -1

			if(move!=0) {
				// Direction after rotationを決める
				var rt = getRotateDirection(move)

				// rotationできるか判定
				if(!nowPieceObject!!.checkCollision(nowPieceX, nowPieceY, rt, field)) {
					// Wallkickなしでrotationできるとき
					rotated = true
					kickused = false
					nowPieceObject!!.direction = rt
					nowPieceObject!!.updateConnectData()
				} else if(ruleopt.rotateWallkick&&wallkick!=null&&(initialRotateDirection==0||ruleopt.rotateInitialWallkick)
					&&(ruleopt.lockresetLimitOver!=RuleOptions.LOCKRESET_LIMIT_OVER_NOWALLKICK||!isRotateCountExceed)) {
					// Wallkickを試みる
					val allowUpward = ruleopt.rotateMaxUpwardWallkick<0||nowUpwardWallkickCount<ruleopt.rotateMaxUpwardWallkick
					val kick =
						wallkick!!.executeWallkick(nowPieceX, nowPieceY, move, nowPieceObject!!.direction, rt, allowUpward, nowPieceObject!!, field, ctrl)

					if(kick!=null) {
						rotated = true
						kickused = true
						playSE("wallkick")
						nowWallkickCount++
						if(kick.isUpward) nowUpwardWallkickCount++
						nowPieceObject!!.direction = kick.direction
						nowPieceObject!!.updateConnectData()
						nowPieceX += kick.offsetX
						nowPieceY += kick.offsetY

						if(ruleopt.lockresetWallkick&&!isRotateCountExceed) {
							lockDelayNow = 0
							nowPieceObject!!.setDarkness(0f)
						}
					}
				}

				// Domino Quick Turn
				if(!rotated&&dominoQuickTurn&&nowPieceObject!!.id==Piece.PIECE_I2&&nowPieceRotateFailCount>=1) {
					rt = getRotateDirection(2)
					rotated = true
					nowPieceObject!!.direction = rt
					nowPieceObject!!.updateConnectData()
					nowPieceRotateFailCount = 0

					if(nowPieceObject!!.checkCollision(nowPieceX, nowPieceY, rt, field))
						nowPieceY--
					else if(onGroundBeforeRotate) nowPieceY++
				}

				if(rotated) {
					// rotation成功
					nowPieceBottomY = nowPieceObject!!.getBottom(nowPieceX, nowPieceY, field)

					if(ruleopt.lockresetRotate&&!isRotateCountExceed) {
						lockDelayNow = 0
						nowPieceObject!!.setDarkness(0f)
					}

					lastmove = if(onGroundBeforeRotate) {
						extendedRotateCount++
						LastMove.ROTATE_GROUND
					} else
						LastMove.ROTATE_AIR

					if(initialRotateDirection==0) playSE("rotate")

					nowPieceRotateCount++
					if(ending==0||staffrollEnableStatistics) statistics.totalPieceRotate++
				} else {
					// rotation失敗
					playSE("rotfail")
					nowPieceRotateFailCount++
				}
			}
			initialRotateDirection = 0

			// game over check
			if(statc[0]==0&&nowPieceObject!!.checkCollision(nowPieceX, nowPieceY, field)) {
				// Blockの出現位置を上にずらすことができる場合はそうする
				for(i in 0 until ruleopt.pieceEnterMaxDistanceY) {
					if(nowPieceObject!!.big)
						nowPieceY -= 2
					else
						nowPieceY--

					if(!nowPieceObject!!.checkCollision(nowPieceX, nowPieceY, field)) {
						nowPieceBottomY = nowPieceObject!!.getBottom(nowPieceX, nowPieceY, field)
						break
					}
				}

				// 死亡
				if(nowPieceObject!!.checkCollision(nowPieceX, nowPieceY, field)) {
					nowPieceObject!!.placeToField(nowPieceX, nowPieceY, field)
					nowPieceObject = null
					stat = Status.GAMEOVER
					if(ending==2&&staffrollNoDeath) stat = Status.NOTHING
					resetStatc()
					return
				}
			}

		}

		var move = 0
		var sidemoveflag = false // この frame に横移動したらtrue

		if(statc[0]>0||ruleopt.moveFirstFrame) {
			// 横移動
			val onGroundBeforeMove = nowPieceObject!!.checkCollision(nowPieceX, nowPieceY+1, field)

			move = moveDirection

			fpf = 0
			if(statc[0]==0&&delayCancel) {
				if(delayCancelMoveLeft) move = -1
				if(delayCancelMoveRight) move = 1
				dasCount = 0
				// delayCancel = false;
				delayCancelMoveLeft = false
				delayCancelMoveRight = false
			} else if(statc[0]==1&&delayCancel&&dasCount<das) {
				move = 0
				delayCancel = false
			}

			if(move!=0) sidemoveflag = true

			if(big&&bigmove) move *= 2

			if(move!=0&&dasCount==0) shiftLock = 0

			if(move!=0&&(dasCount==0||dasCount>=das)) {
				shiftLock = shiftLock and ctrl!!.buttonBit

				if(shiftLock==0)
					if(dasSpeedCount>=dasDelay||dasCount==0) {
						if(dasCount>0) dasSpeedCount = 1

						if(!nowPieceObject!!.checkCollision(nowPieceX+move, nowPieceY, field)) {
							nowPieceX += move

							if(dasDelay==0&&dasCount>0&&
								!nowPieceObject!!.checkCollision(nowPieceX+move, nowPieceY, field)) {
								if(!dasInstant) playSE("move")
								dasRepeat = true
								dasInstant = true
							}

							//log.debug("Successful movement: move="+move);

							if(ruleopt.lockresetMove&&!isMoveCountExceed) {
								lockDelayNow = 0
								nowPieceObject!!.setDarkness(0f)
							}

							nowPieceMoveCount++
							if(ending==0||staffrollEnableStatistics) statistics.totalPieceMove++
							nowPieceBottomY = nowPieceObject!!.getBottom(nowPieceX, nowPieceY, field)

							lastmove = if(onGroundBeforeMove) {
								extendedMoveCount++
								LastMove.SLIDE_GROUND
							} else LastMove.SLIDE_AIR
							if(!dasInstant) playSE("move")

						} else if(ruleopt.dasChargeOnBlockedMove) {
							dasCount = das
							dasSpeedCount = dasDelay
						}
					} else dasSpeedCount++
			}

			if(!dasRepeat) {
				// Hard drop
				if(ctrl!!.isPress(up)&&!harddropContinuousUse&&ruleopt.harddropEnable
					&&(isDiagonalMoveEnabled||!sidemoveflag)&&(ruleopt.moveUpAndDown||!updown)
					&&nowPieceY<nowPieceBottomY) {
					harddropFall += nowPieceBottomY-nowPieceY
					fpf = nowPieceBottomY-nowPieceY
					if(nowPieceY!=nowPieceBottomY) {
						nowPieceY = nowPieceBottomY
						playSE("harddrop")
					}
					harddropContinuousUse = !ruleopt.harddropLock
					owner.mode?.also {it.afterHardDropFall(this, playerID, harddropFall)}
					owner.receiver.afterHardDropFall(this, playerID, harddropFall)

					lastmove = LastMove.FALL_SELF
					if(ruleopt.lockresetFall) {
						lockDelayNow = 0
						nowPieceObject!!.setDarkness(0f)
						extendedMoveCount = 0
						extendedRotateCount = 0
					}
				}
				val sd = ruleopt.softdropEnable&&ctrl!!.isPress(down)&&!softdropContinuousUse&&
					(isDiagonalMoveEnabled||!sidemoveflag)&&(ruleopt.moveUpAndDown||!updown)
				var sds = ruleopt.softdropSpeed
				sds *= if(ruleopt.softdropMultiplyNativeSpeed||speed.denominator<=0)
					speed.gravity.toFloat()
				else speed.denominator.toFloat()
				if(!onGroundBeforeMove&&!harddropContinuousUse&&sd)
					if(!ruleopt.softdropGravitySpeedLimit||ruleopt.softdropSpeed<1f) {// Old Soft Drop codes
						gcount += sds.toInt()
						softdropUsed = true
					} else // New Soft Drop codes
						if(ruleopt.softdropMultiplyNativeSpeed||speed.gravity<speed.denominator*ruleopt.softdropSpeed) {
							gcount = sds.toInt()
							softdropUsed = true
						} else gcount += speed.gravity// This prevents soft drop from adding to the gravity speed.
			}
			if(ending==0||staffrollEnableStatistics) statistics.totalPieceActiveTime++
		}
		if(!ruleopt.softdropGravitySpeedLimit||ruleopt.softdropSpeed<1f||
			!softdropUsed) gcount += speed.gravity // Part of Old Soft Drop

		while((gcount>=speed.denominator||speed.gravity<0)&&!nowPieceObject!!.checkCollision(nowPieceX, nowPieceY+1, field)) {
			if(speed.gravity>=0) gcount -= speed.denominator
			nowPieceY++
			if(speed.gravity>speed.denominator/2||speed.gravity<0||softdropUsed) fpf++
			if(ruleopt.lockresetFall) {
				lockDelayNow = 0
				nowPieceObject!!.setDarkness(0f)
			}

			if(lastmove!=LastMove.ROTATE_GROUND&&lastmove!=LastMove.SLIDE_GROUND&&lastmove!=LastMove.FALL_SELF) {
				extendedMoveCount = 0
				extendedRotateCount = 0
			}

			if(softdropUsed) {
				lastmove = LastMove.FALL_SELF
				softdropFall++
				softdropFallNow++
			} else lastmove = LastMove.FALL_AUTO
		}

		if(softdropFallNow>0) {
			playSE("softdrop")
			owner.mode?.also {it.afterSoftDropFall(this, playerID, softdropFallNow)}
			owner.receiver.afterSoftDropFall(this, playerID, softdropFallNow)
		}

		// 接地と固定
		if(nowPieceObject!!.checkCollision(nowPieceX, nowPieceY+1, field)&&(statc[0]>0||ruleopt.moveFirstFrame)) {
			if(lockDelayNow==0&&lockDelay>0&&lastmove!=LastMove.SLIDE_GROUND&&lastmove!=LastMove.ROTATE_GROUND) {
				playSE("step")
				if(!ruleopt.softdropLock&&ruleopt.softdropSurfaceLock&&softdropUsed) softdropContinuousUse = true
			}
			if(lockDelayNow<lockDelay) lockDelayNow++

			if(lockDelay>=99&&lockDelayNow>98) lockDelayNow = 98

			if(lockDelayNow<lockDelay)
				if(lockDelayNow>=lockDelay-1) nowPieceObject!!.setDarkness(.5f)
				else nowPieceObject!!.setDarkness(0.35f*lockDelayNow/lockDelay)

			if(lockDelay!=0) gcount = speed.gravity

			// trueになると即固定
			var instantlock = false

			// Hard drop固定
			if(ruleopt.harddropEnable&&!harddropContinuousUse&&
				ctrl!!.isPress(up)&&ruleopt.harddropLock&&
				(isDiagonalMoveEnabled||!sidemoveflag)&&(ruleopt.moveUpAndDown||!updown)) {
				harddropContinuousUse = true
				manualLock = true
				instantlock = true
			}

			// Soft drop固定
			if(ruleopt.softdropEnable&&
				(ruleopt.softdropLock&&ctrl!!.isPress(down)||ctrl!!.isPush(down)&&ruleopt.softdropSurfaceLock&&!softdropUsed)
				&&!softdropContinuousUse&&(isDiagonalMoveEnabled||!sidemoveflag)&&(ruleopt.moveUpAndDown||!updown)) {
				softdropContinuousUse = true
				manualLock = true
				instantlock = true
			}
			if(manualLock&&ruleopt.shiftLockEnable)
			// bit 1 and 2 are button_up and button_down currently
				shiftLock = ctrl!!.buttonBit and 3

			// 移動＆rotationcount制限超過
			if(ruleopt.lockresetLimitOver==RuleOptions.LOCKRESET_LIMIT_OVER_INSTANT&&(isMoveCountExceed||isRotateCountExceed))
				instantlock = true

			// 接地即固定
			if(lockDelay==0&&(gcount>=speed.denominator||speed.gravity<0)) instantlock = true

			// 固定
			if(lockDelay in 1..lockDelayNow||instantlock) {
				if(ruleopt.lockflash>0) nowPieceObject!!.setDarkness(-.8f)

				// T-Spin判定
				if(lastmove==LastMove.ROTATE_GROUND&&tspinEnable)
					if(useAllSpinBonus) setAllSpin(nowPieceX, nowPieceY, nowPieceObject, field)
					else setTSpin(nowPieceX, nowPieceY, nowPieceObject, field)

				nowPieceObject!!.setAttribute(Block.BLOCK_ATTRIBUTE_SELFPLACED, true)

				val partialLockOut = nowPieceObject!!.isPartialLockOut(nowPieceX, nowPieceY, field)
				val put = nowPieceObject!!.placeToField(nowPieceX, nowPieceY, field)

				playSE("lock", 1f-(nowPieceY+nowPieceObject!!.height)*.5f/fieldHeight)

				holdDisable = false

				if(ending==0||staffrollEnableStatistics) statistics.totalPieceLocked++// AREなし
				lineClearing = when(clearMode) {
					ClearType.LINE -> field.checkLineNoFlag()
					ClearType.COLOR -> field.checkColor(colorClearSize, false, garbageColorClear, gemSameColor, ignoreHidden)
					ClearType.LINE_COLOR -> field.checkLineColor(colorClearSize, false, lineColorDiagonals, gemSameColor)
					ClearType.GEM_COLOR -> field.gemColorCheck(colorClearSize, false, garbageColorClear, ignoreHidden)
					ClearType.LINE_GEM_BOMB, ClearType.LINE_GEM_SPARK -> field.checkBombOnLine(true)
				}

				chain = 0
				lineGravityTotalLines = 0
				garbageClearing = field.garbageCleared
				//if(combo==0 && lastevent!=EVENT_NONE)lastevent=EVENT_NONE;
				if(lineClearing==0) {

					if(tspin) {
						playSE("tspin")
						lasteventpiece = nowPieceObject!!.id
						lastevent = if(tspinmini) EVENT_TSPIN_ZERO_MINI else EVENT_TSPIN_ZERO
						if(b2bcount==0) {
							b2bcount = 1
							playSE("b2b_start")
						}
						if(ending==0||staffrollEnableStatistics)
							if(tspinmini)
								statistics.totalTSpinZeroMini++
							else
								statistics.totalTSpinZero++
					} else
						combo = 0

					owner.mode?.also {it.calcScore(this, playerID, lineClearing)}
					owner.receiver.calcScore(this, playerID, lineClearing)
				}

				owner.mode?.also {it.pieceLocked(this, playerID, lineClearing)}
				owner.receiver.pieceLocked(this, playerID, lineClearing)

				dasRepeat = false
				dasInstant = false

				// Next 処理を決める(Mode 側でステータスを弄っている場合は何もしない)
				if(stat==Status.MOVE) {
					resetStatc()

					when {
						ending==1 -> stat = Status.ENDINGSTART // Ending
						!put&&ruleopt.fieldLockoutDeath||partialLockOut&&ruleopt.fieldPartialLockoutDeath -> {
							// 画面外に置いて死亡
							stat = Status.GAMEOVER
							if(ending==2&&staffrollNoDeath) stat = Status.NOTHING
						}
						(lineGravityType==LineGravity.CASCADE||lineGravityType==LineGravity.CASCADE_SLOW)&&!connectBlocks -> {
							stat = Status.LINECLEAR
							statc[0] = lineDelay
							statLineClear()
						}
						lineClearing>0&&(ruleopt.lockflash<=0||!ruleopt.lockflashBeforeLineClear) -> {
							// Line clear
							stat = Status.LINECLEAR
							statLineClear()
						}
						(are>0||lagARE||ruleopt.lockflashBeforeLineClear)&&
							ruleopt.lockflash>0&&ruleopt.lockflashOnlyFrame
							// AREあり (光あり）
						-> stat = Status.LOCKFLASH
						are>0||lagARE -> {
							// AREあり (光なし）
							statc[1] = are
							stat = Status.ARE
						}
						interruptItemNumber!=INTERRUPTITEM_NONE -> {
							// 中断効果のあるアイテム処理
							nowPieceObject = null
							interruptItemPreviousStat = Status.MOVE
							stat = Status.INTERRUPTITEM
						}
						else -> {
							// AREなし
							stat = Status.MOVE
							if(!ruleopt.moveFirstFrame) statMove()
						}
					}
				}
				return
			}
		}

		// 横溜め
		if(statc[0]>0||ruleopt.dasInMoveFirstFrame)
			if(moveDirection!=0&&moveDirection==dasDirection&&(dasCount<das||das<=0))
				dasCount++

		statc[0]++
	}

	/** Block固定直後の光っているときの処理 */
	private fun statLockFlash() {
		//  event 発生
		owner.mode?.also {if(it.onLockFlash(this, playerID)) return}
		owner.receiver.onLockFlash(this, playerID)

		statc[0]++

		checkDropContinuousUse()

		// 横溜め
		if(ruleopt.dasInLockFlash)
			padRepeat()
		else if(ruleopt.dasRedirectInDelay) dasRedirect()

		// Next ステータス
		if(statc[0]>=ruleopt.lockflash) {
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
		if(field==null) return
		//  event 発生
		owner.mode?.also {if(it.onLineClear(this, playerID)) return}
		owner.receiver.onLineClear(this, playerID)

		checkDropContinuousUse()

		// 横溜め
		if(ruleopt.dasInLineClear)
			padRepeat()
		else if(ruleopt.dasRedirectInDelay) dasRedirect()

		// 最初の frame
		if(statc[0]==0) {
			when(clearMode) {
				ClearType.LINE -> lineClearing = field!!.checkLine()// Line clear flagを設定
				ClearType.COLOR ->// Set cint clear flags
					lineClearing = field!!.checkColor(colorClearSize, true, garbageColorClear, gemSameColor, ignoreHidden)
				ClearType.LINE_COLOR ->// Set line cint clear flags
					lineClearing = field!!.checkLineColor(colorClearSize, true, lineColorDiagonals, gemSameColor)
				ClearType.GEM_COLOR -> lineClearing = field!!.gemColorCheck(colorClearSize, true, garbageColorClear, ignoreHidden)
				ClearType.LINE_GEM_BOMB, ClearType.LINE_GEM_SPARK -> {
					lineClearing = field!!.checkBombIgnited()
					if(clearMode==ClearType.LINE_GEM_BOMB) statc[3] = chain
					val force = statc[3]+field!!.checkLineNoFlag()
					if(clearMode==ClearType.LINE_GEM_SPARK) statc[3] = force
					field!!.igniteBomb(explodSize[force][0], explodSize[force][1], explodSize[0][0], explodSize[0][1])

				}
			}
			val ingame = ending==0||staffrollEnableStatistics
			// Linescountを決める
			var li = lineClearing
			if(big&&bighalf) li = li shr 1
			if(clearMode==ClearType.LINE) {
				split = field!!.lastLinesSplited

				if(li>0) {
					playSE("erase")
					lasteventpiece = nowPieceObject!!.id
					lastlines = field!!.lastLinesHeight
					lastline = field!!.lastLinesBottom

					if(li>=4) playSE("erase4") else playSE("erase$li")
					if(tspin) {
						playSE("tspin")
						when(li) {
							1 -> lastevent = if(tspinmini) EVENT_TSPIN_SINGLE_MINI else EVENT_TSPIN_SINGLE
							2 -> lastevent = if(tspinmini) EVENT_TSPIN_DOUBLE_MINI else EVENT_TSPIN_DOUBLE
							3 -> lastevent = EVENT_TSPIN_TRIPLE
						}
						if(ingame)
							when(li) {
								1 -> if(tspinmini) statistics.totalTSpinSingleMini++
								else statistics.totalTSpinSingle++
								2 -> if(tspinmini) statistics.totalTSpinDoubleMini++
								else statistics.totalTSpinDouble++
								3 -> statistics.totalTSpinTriple++
							}
						if(tspinez) lastevent = EVENT_TSPIN_EZ
					} else {
						when {
							li==1 -> lastevent = EVENT_SINGLE
							li==2 -> lastevent = if(split) EVENT_SPLIT_DOUBLE else EVENT_DOUBLE
							li==3 -> lastevent = if(split) EVENT_SPLIT_TRIPLE else EVENT_TRIPLE
							li>=4 -> lastevent = EVENT_QUADRUPLE
						}
						if(ingame)
							when(li) {
								1 -> statistics.totalSingle++
								2 -> if(split) statistics.totalSplitDouble++
								else statistics.totalDouble++
								3 -> if(split) statistics.totalSplitTriple++
								else statistics.totalTriple++
								4 -> statistics.totalQuadruple++
							}
					}
				}
				// B2B bonus

				if((split&&li<=2)||(tspin&&li<=1)) playSE("applause0")
				if(li>=4||(split&&li>2)||(tspin&&li>=2)) playSE("applause1")
				if(b2bEnable)
					if(li>=4||split||tspin) {
						b2bcount++

						if(b2bcount==1) playSE("b2b_start")
						else {
							b2b = true
							playSE("b2b_combo", if(b2bbuf>7) 2f else 1f+(b2bbuf-1)/7f)

							if(ingame)
								when {
									li==4 -> statistics.totalB2BFour++
									split -> statistics.totalB2BSplit++
									tspin -> statistics.totalB2BTSpin++
								}

						}
						b2bbuf = b2bcount
					} else if(b2bcount!=0&&combo<=0) {
						b2b = false
						b2bcount = 0
						playSE("b2b_end")
					}
				// Combo
				if(comboType!=COMBO_TYPE_DISABLE&&chain==0) {
					if(comboType==COMBO_TYPE_NORMAL||comboType==COMBO_TYPE_DOUBLE&&li>=2) combo++
					if(combo>=2) {
						playSE(if(combo==2) "applause2" else "applause3")
						playSE("combo", if(combo>15) 3f else 1f+(combo-2)/7f)
					}
					if(ingame) if(combo>statistics.maxCombo) statistics.maxCombo = combo
					combobuf = combo
				}

				lineGravityTotalLines += lineClearing
				if(ingame) statistics.lines += li

			} else if(clearMode==ClearType.LINE_GEM_BOMB) {

				playSE("erase")
			}
			if(field!!.howManyGemClears>0) playSE("gem")
			// All clear
			if(li>=1&&field!!.isEmpty) {
				owner.receiver.bravo(this, playerID)
				temphanabi += 6
			}
			// Calculate score
			owner.mode?.also {it.calcScore(this, playerID, li)}
			owner.receiver.calcScore(this, playerID, li)

			// Blockを消す演出を出す (まだ実際には消えていない）
			for(i in 0 until field!!.height) {
				if(clearMode==ClearType.LINE&&field!!.getLineFlag(i)) {
					owner.mode?.also {it.lineClear(this, playerID, i)}
					owner.receiver.lineClear(this, playerID, i)
				}
				for(j in 0 until field!!.width) {
					field!!.getBlock(j, i)?.let {b ->
						if(b.getAttribute(Block.BLOCK_ATTRIBUTE_ERASE)) {
							owner.mode?.also {it.blockBreak(this, playerID, j, i, b)}
							owner.receiver.also {r ->
								if(displaysize==1) {
									r.blockBreak(this, playerID, 2*j, 2*i, b)
									r.blockBreak(this, playerID, 2*j+1, 2*i, b)
									r.blockBreak(this, playerID, 2*j, 2*i+1, b)
									r.blockBreak(this, playerID, 2*j+1, 2*i+1, b)
								} else r.blockBreak(this, playerID, j, i, b)
								if(b.isGemBlock&&clearMode==ClearType.LINE_GEM_BOMB)
									r.blockBreak(this, playerID, j, i, b)
							}
						}
					}
				}
			}

			// Blockを消す
			when(clearMode) {
				ClearType.LINE -> field!!.clearLine()
				ClearType.COLOR -> field!!.clearColor(colorClearSize, garbageColorClear, gemSameColor, ignoreHidden)
				ClearType.LINE_COLOR -> field!!.clearProceed()
				ClearType.GEM_COLOR -> lineClearing = field!!.gemClearColor(colorClearSize, garbageColorClear, ignoreHidden)
				ClearType.LINE_GEM_BOMB -> lineClearing = field!!.clearProceed(1)
				ClearType.LINE_GEM_SPARK -> lineClearing = field!!.clearProceed(2)
			}
		}

		// Linesを1段落とす
		if(lineGravityType==LineGravity.NATIVE&&lineDelay>=lineClearing-1&&statc[0]>=lineDelay-(lineClearing-1)
			&&ruleopt.lineFallAnim)
			field!!.downFloatingBlocksSingleLine()

		// Line delay cancel check
		delayCancelMoveLeft = ctrl!!.isPush(Controller.BUTTON_LEFT)
		delayCancelMoveRight = ctrl!!.isPush(Controller.BUTTON_RIGHT)

		val moveCancel = ruleopt.lineCancelMove&&(ctrl!!.isPush(up)||ctrl!!.isPush(down)
			||delayCancelMoveLeft||delayCancelMoveRight)
		val rotateCancel = ruleopt.lineCancelRotate&&(ctrl!!.isPush(Controller.BUTTON_A)||ctrl!!.isPush(Controller.BUTTON_B)
			||ctrl!!.isPush(Controller.BUTTON_C)||ctrl!!.isPush(Controller.BUTTON_E))
		val holdCancel = ruleopt.lineCancelHold&&ctrl!!.isPush(Controller.BUTTON_D)

		delayCancel = moveCancel||rotateCancel||holdCancel

		if(statc[0]<lineDelay&&delayCancel) statc[0] = lineDelay

		// Next ステータス
		if(statc[0]>=lineDelay) {
			if((clearMode==ClearType.LINE_GEM_BOMB||clearMode==ClearType.LINE_GEM_SPARK)&&field!!.checkBombIgnited()>0) {
				statc[0] = 0
				statc[6] = 0
				return
			} else if(lineGravityType==LineGravity.CASCADE||lineGravityType==LineGravity.CASCADE_SLOW) // Cascade
				when {
					statc[6]<cascadeDelay -> {
						statc[6]++
						return
					}
					field!!.doCascadeGravity(lineGravityType) -> {
						statc[6] = 0
						return
					}
					statc[6]<cascadeClearDelay -> {
						statc[6]++
						return
					}
					clearMode==ClearType.LINE&&field!!.checkLineNoFlag()>0||clearMode==ClearType.COLOR&&field!!.checkColor(colorClearSize, false, garbageColorClear, gemSameColor, ignoreHidden)>0
						||clearMode==ClearType.LINE_COLOR&&field!!.checkLineColor(colorClearSize, false, lineColorDiagonals, gemSameColor)>0
						||clearMode==ClearType.GEM_COLOR&&field!!.gemColorCheck(colorClearSize, false, garbageColorClear, ignoreHidden)>0
						||(clearMode==ClearType.LINE_GEM_BOMB||clearMode==ClearType.LINE_GEM_SPARK)&&field!!.checkBombOnLine(true)>0 -> {
						tspin = false
						tspinmini = false
						chain++
						if(chain>statistics.maxChain) statistics.maxChain = chain
						statc[0] = 0
						statc[6] = 0
						combobuf = chain
						return
					}
				}

			var skip = false
			if(owner.mode!=null) skip = owner.mode!!.lineClearEnd(this, playerID)
			owner.receiver.lineClearEnd(this, playerID)
			if(sticky>0) field!!.setBlockLinkByColor()
			if(sticky==2) field!!.setAllAttribute(Block.BLOCK_ATTRIBUTE_IGNORE_BLOCKLINK, true)

			if(!skip) {
				if(lineGravityType==LineGravity.NATIVE) field!!.downFloatingBlocks()
				if(field!!.lastLinesBottom>=field!!.highestBlockY||field!!.lastLinesSplited)
					playSE("linefall")

				field!!.lineColorsCleared = IntArray(0)

				if(stat==Status.LINECLEAR) {
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
						interruptItemNumber!=INTERRUPTITEM_NONE -> {
							// 中断効果のあるアイテム処理
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
			}

			return
		}

		statc[0]++
	}

	/** ARE中の処理 */
	private fun statARE() {
		//  event 発生
		owner.mode?.also {if(it.onARE(this, playerID)) return}

		owner.receiver.onARE(this, playerID)
		if(statc[0]==0) {
			val blocks = field!!.howManyBlocks
			if(blocks>=150) loopSE("danger")
			if(blocks<150) stopSE("danger")
		}
		statc[0]++

		checkDropContinuousUse()

		// ARE cancel check
		delayCancelMoveLeft = ctrl!!.isPush(Controller.BUTTON_LEFT)
		delayCancelMoveRight = ctrl!!.isPush(Controller.BUTTON_RIGHT)

		val moveCancel = ruleopt.areCancelMove&&(ctrl!!.isPush(up)||ctrl!!.isPush(down)
			||delayCancelMoveLeft||delayCancelMoveRight)
		val rotateCancel = ruleopt.areCancelRotate&&(ctrl!!.isPush(Controller.BUTTON_A)||ctrl!!.isPush(Controller.BUTTON_B)
			||ctrl!!.isPush(Controller.BUTTON_C)||ctrl!!.isPush(Controller.BUTTON_E))
		val holdCancel = ruleopt.areCancelHold&&ctrl!!.isPush(Controller.BUTTON_D)

		delayCancel = moveCancel||rotateCancel||holdCancel

		if(statc[0]<statc[1]&&delayCancel) statc[0] = statc[1]

		// 横溜め
		if(ruleopt.dasInARE&&(statc[0]<statc[1]-1||ruleopt.dasInARELastFrame))
			padRepeat()
		else if(ruleopt.dasRedirectInDelay) dasRedirect()

		// Next ステータス
		if(statc[0]>=statc[1]&&!lagARE) {
			nowPieceObject = null
			resetStatc()

			if(interruptItemNumber!=INTERRUPTITEM_NONE) {
				// 中断効果のあるアイテム処理
				interruptItemPreviousStat = Status.MOVE
				stat = Status.INTERRUPTITEM
			} else {
				// Blockピース移動処理
				initialRotate()
				stat = Status.MOVE
			}
		}
	}

	/** Ending突入処理 */
	private fun statEndingStart() {
		//  event 発生
		val animint = 6
		statc[3] = field!!.height*6
		owner.mode?.also {if(it.onEndingStart(this, playerID)) return}
		owner.receiver.onEndingStart(this, playerID)

		checkDropContinuousUse()
		// 横溜め
		if(ruleopt.dasInEndingStart) padRepeat()
		else if(ruleopt.dasRedirectInDelay) dasRedirect()

		if(statc[2]==0) {
			timerActive = false
			owner.bgmStatus.bgm = BGMStatus.BGM.SILENT
			playSE("endingstart")
			statc[2] = 1
		}
		if(statc[0]<lineDelay) statc[0]++
		else if(statc[1]<statc[3]) {
			if(statc[1]%animint==0) {
				val y = field!!.height-statc[1]/animint
				field!!.setLineFlag(y, true)

				for(i in 0 until field!!.width) {
					field!!.getBlock(i, y)?.let {blk ->

						if(blk.cint!=Block.BLOCK_COLOR_NONE) {
							owner.mode?.also {it.blockBreak(this, playerID, i, y, blk)}
							owner.receiver.blockBreak(this, playerID, i, y, blk)
							field!!.setBlockColor(i, y, Block.BLOCK_COLOR_NONE)
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
				field!!.reset()
				nowPieceObject = null
				stat = Status.MOVE
			} else stat = Status.EXCELLENT
		}
	}

	/** 各ゲームMode が自由に使えるステータスの処理 */
	private fun statCustom() {
		//  event 発生
		owner.mode?.also {if(it.onCustom(this, playerID)) return}
		owner.receiver.onCustom(this, playerID)
	}

	/** Ending画面 */
	private fun statExcellent() {
		//  event 発生
		owner.mode?.also {if(it.onExcellent(this, playerID)) return}
		owner.receiver.onExcellent(this, playerID)

		if(statc[0]==0) {
			gameEnded()
			owner.bgmStatus.fadesw = true
			resetFieldVisible()
			temphanabi += 24
			playSE("excellent")
		}

		if(statc[0]>=120&&statc[1]<=0&&ctrl!!.isPush(Controller.BUTTON_A)) statc[0] = 600
		if(statc[0]>=600) {
			resetStatc()
			stat = Status.GAMEOVER
		} else statc[0]++
	}

	/** game overの処理 */
	private fun statGameOver() {
		//  event 発生
		owner.mode?.also {if(it.onGameOver(this, playerID)) return}
		owner.receiver.onGameOver(this, playerID)

		var topout = statc[2]==0
		if(statc[0]==0) {
			topout = gameActive
			if(topout&&ending==2) topout = !staffrollNoDeath
			statc[2] = if(topout) 0 else 1
			stopSE("danger")
			if(topout) playSE("dead")
		}
		if(!topout||lives<=0) {
			// もう復活できないとき
			val animint = 6
			statc[1] = animint*(field!!.height+1)
			if(statc[0]==0) {

				gameEnded()
				blockShowOutlineOnly = false
				if(owner.players<2) owner.bgmStatus.bgm = BGMStatus.BGM.SILENT

				if(field!!.isEmpty) statc[0] = statc[1]
				else {
					resetFieldVisible()
					if(ending==2&&!topout) playSE("end")
					else playSE("shutter")
				}
			}
			when {
				statc[0]<statc[1] -> {
					for(x in 0 until field!!.width)
						if(field!!.getBlockColor(x, field!!.height-statc[0]/animint)!=Block.BLOCK_COLOR_NONE) {
							field!!.getBlock(x, field!!.height-statc[0]/animint)?.apply {
								if(ending==2&&!topout) {
									if(statc[0]%animint==0) {
										setAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, false)
										darkness = -.1f
										elapsedFrames = -1
									}
									alpha = 1f-(1+statc[0]%animint)*1f/animint
								} else if(statc[0]%animint==0) {
									if(!getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE)) {
										cint = Block.BLOCK_COLOR_GRAY
										setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true)
									}
									darkness = .3f
									elapsedFrames = -1
								}
							}
						}
					statc[0]++
				}
				statc[0]==statc[1] -> {
					if(topout) playSE("gameover")
					else if(ending==2&&field!!.isEmpty) playSE("end")
					statc[0]++
				}
				statc[0]<statc[1]+180 -> {
					if(statc[0]>=statc[1]+60&&ctrl!!.isPush(Controller.BUTTON_A)) statc[0] = statc[1]+180
					statc[0]++
				}
				else -> {
					if(!owner.replayMode||owner.replayRerecord) owner.saveReplay()

					for(i in 0 until owner.players)
						if(i==playerID||gameoverAll) {
							if(owner.engine[i].field!=null) owner.engine[i].field!!.reset()
							owner.engine[i].resetStatc()
							owner.engine[i].stat = Status.RESULT
						}
				}
			}
		} else {
			// 復活できるとき
			if(statc[0]==0) {
				//blockShowOutlineOnly=false;
				resetFieldVisible()
				for(i in field!!.hiddenHeight*-1 until field!!.height)
					for(j in 0 until field!!.width)
						if(field!!.getBlockColor(j, i)!=Block.BLOCK_COLOR_NONE) field!!.setBlockColor(j, i, Block.BLOCK_COLOR_GRAY)
				statc[0] = 1
			}
			if(!field!!.isEmpty) {
				val y = field!!.highestBlockY
				for(i in 0 until field!!.width) {
					field!!.getBlock(i, y)?.let {b ->

						if(b.cint!=Block.BLOCK_COLOR_NONE) {
							owner.mode?.also {it.blockBreak(this, playerID, i, y, b)}
							owner.receiver.blockBreak(this, playerID, i, y, b)
							field!!.setBlockColor(i, y, Block.BLOCK_COLOR_NONE)
						}
					}
				}

			} else if(statc[1]<are)
				statc[1]++
			else {
				lives--
				resetStatc()
				stat = Status.MOVE
			}
		}
	}

	/** Results screen */
	private fun statResult() {
		// Event
		owner.bgmStatus.fadesw = false
		when {
			ending==2 -> owner.bgmStatus.bgm = BGMStatus.BGM.CLEARED
			ending!=0 -> owner.bgmStatus.bgm = if(statistics.time<10800) BGMStatus.BGM.RESULT_1 else BGMStatus.BGM.RESULT_2
			else -> owner.bgmStatus.bgm = BGMStatus.BGM.FAILED
		}

		owner.mode?.also {if(it.onResult(this, playerID)) return}
		owner.receiver.onResult(this, playerID)

		// Turn-off in-game flags
		gameActive = false
		timerActive = false
		isInGame = false

		// Cursor movement
		if(ctrl!!.isMenuRepeatKey(Controller.BUTTON_LEFT)||ctrl!!.isMenuRepeatKey(Controller.BUTTON_RIGHT)) {
			statc[0] = if(statc[0]==0) 1 else 0
			playSE("cursor")
		}

		// Confirm
		if(ctrl!!.isPush(Controller.BUTTON_A)) {
			playSE("decide")

			if(statc[0]==0) owner.reset()
			else quitflag = true

		}
	}

	/** fieldエディット画面 */
	private fun statFieldEdit() {
		//  event 発生
		owner.mode?.also {if(it.onFieldEdit(this, playerID)) return}
		owner.receiver.onFieldEdit(this, playerID)

		fldeditFrames++

		// Cursor movement
		if(ctrl!!.isMenuRepeatKey(Controller.BUTTON_LEFT, false)&&!ctrl!!.isPress(Controller.BUTTON_C)) {
			playSE("move")
			fldeditX--
			if(fldeditX<0) fldeditX = fieldWidth-1
		}
		if(ctrl!!.isMenuRepeatKey(Controller.BUTTON_RIGHT, false)&&!ctrl!!.isPress(Controller.BUTTON_C)) {
			playSE("move")
			fldeditX++
			if(fldeditX>fieldWidth-1) fldeditX = 0
		}
		if(ctrl!!.isMenuRepeatKey(up, false)) {
			playSE("move")
			fldeditY--
			if(fldeditY<0) fldeditY = fieldHeight-1
		}
		if(ctrl!!.isMenuRepeatKey(down, false)) {
			playSE("move")
			fldeditY++
			if(fldeditY>fieldHeight-1) fldeditY = 0
		}

		// 色選択
		if(ctrl!!.isMenuRepeatKey(Controller.BUTTON_LEFT, false)&&ctrl!!.isPress(Controller.BUTTON_C)) {
			playSE("cursor")
			fldeditColor--
			if(fldeditColor<Block.BLOCK_COLOR_GRAY) fldeditColor = Block.BLOCK_COLOR_GEM_PURPLE
		}
		if(ctrl!!.isMenuRepeatKey(Controller.BUTTON_RIGHT, false)&&ctrl!!.isPress(Controller.BUTTON_C)) {
			playSE("cursor")
			fldeditColor++
			if(fldeditColor>Block.BLOCK_COLOR_GEM_PURPLE) fldeditColor = Block.BLOCK_COLOR_GRAY
		}

		// 配置
		if(ctrl!!.isPress(Controller.BUTTON_A)&&fldeditFrames>10)
			try {
				if(field!!.getBlockColorE(fldeditX, fldeditY)!=fldeditColor) {
					val blk = Block(fldeditColor, skin, Block.BLOCK_ATTRIBUTE_VISIBLE or Block.BLOCK_ATTRIBUTE_OUTLINE)
					field!!.setBlockE(fldeditX, fldeditY, blk)
					playSE("change")
				}
			} catch(e:Exception) {
			}

		// 消去
		if(ctrl!!.isPress(Controller.BUTTON_D)&&fldeditFrames>10)
			try {
				if(!field!!.getBlockEmptyE(fldeditX, fldeditY)) {
					field!!.setBlockColorE(fldeditX, fldeditY, Block.BLOCK_COLOR_NONE)
					playSE("change")
				}
			} catch(e:Exception) {
			}

		// 終了
		if(ctrl!!.isPush(Controller.BUTTON_B)&&fldeditFrames>10) {
			stat = fldeditPreviousStat
			owner.mode?.also {it.fieldEditExit(this, playerID)}
			owner.receiver.fieldEditExit(this, playerID)
		}
	}

	/** プレイ中断効果のあるアイテム処理 */
	private fun statInterruptItem() {
		var contFlag = false // 続行 flag

		when(interruptItemNumber) {
			INTERRUPTITEM_MIRROR // ミラー
			-> contFlag = interruptItemMirrorProc()
		}

		if(!contFlag) {
			interruptItemNumber = INTERRUPTITEM_NONE
			resetStatc()
			stat = interruptItemPreviousStat
		}
	}

	/** ミラー処理
	 * @return When true,ミラー処理続行
	 */
	fun interruptItemMirrorProc():Boolean {
		when {
			statc[0]==0 -> {
				// fieldをバックアップにコピー
				interruptItemMirrorField = field?.let {Field(it)} ?: Field()
				// fieldのBlockを全部消す
				field!!.reset()
			}
			statc[0]>=21&&statc[0]<21+field!!.width*2&&statc[0]%2==0 -> {
				// 反転
				val x = (statc[0]-20)/2-1

				for(y in field!!.hiddenHeight*-1 until field!!.height)
					field!!.setBlock(field!!.width-x-1, y, interruptItemMirrorField!!.getBlock(x, y))
			}
			statc[0]<21+field!!.width*2+5 -> {
				// 待ち time
			}
			else -> {
				// 終了
				statc[0] = 0
				interruptItemMirrorField = null
				return false
			}
		}

		statc[0]++
		return true
	}

	companion object {
		/** Log (Apache log4j) */
		internal var log = Logger.getLogger(GameEngine::class.java)

		/** Constants of game style
		 * (Currently not directly used by GameEngine, but from game modes) */
		const val GAMESTYLE_TETROMINO = 0
		const val GAMESTYLE_AVALANCHE = 1
		const val GAMESTYLE_PHYSICIAN = 2
		const val GAMESTYLE_SPF = 3

		/** Max number of game style */
		const val MAX_GAMESTYLE = 4

		/** Game style names */
		val GAMESTYLE_NAMES = arrayOf("TETROMINO", "AVALANCHE", "PHYSICIAN", "SPF")

		/** Number of free status counters (used by statc array) */
		const val MAX_STATC = 10

		/** Most recent scoring event type constants */
		const val EVENT_NONE = 0
		const val EVENT_SINGLE = 1
		const val EVENT_DOUBLE = 2
		const val EVENT_TRIPLE = 3
		const val EVENT_QUADRUPLE = 4
		const val EVENT_SPLIT_DOUBLE = 5
		const val EVENT_SPLIT_TRIPLE = 6
		const val EVENT_TSPIN_SINGLE_MINI = 7
		const val EVENT_TSPIN_SINGLE = 8
		const val EVENT_TSPIN_DOUBLE_MINI = 9
		const val EVENT_TSPIN_DOUBLE = 10
		const val EVENT_TSPIN_TRIPLE = 11
		const val EVENT_TSPIN_EZ = 12
		const val EVENT_TSPIN_ZERO_MINI = 13
		const val EVENT_TSPIN_ZERO = 14

		/** Constants of block outline type */
		const val BLOCK_OUTLINE_AUTO = -1
		const val BLOCK_OUTLINE_NONE = 0
		const val BLOCK_OUTLINE_NORMAL = 1
		const val BLOCK_OUTLINE_CONNECT = 2
		const val BLOCK_OUTLINE_SAMECOLOR = 3

		/** Default duration of Ready->Go */
		const val READY_START = 0
		const val READY_END = 49
		const val GO_START = 50
		const val GO_END = 100

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
		const val FRAME_SKIN_GRADE = -1
		const val FRAME_SKIN_GB = -2
		const val FRAME_SKIN_SG = -3
		const val FRAME_SKIN_HEBO = -4

		/** Constants of meter colors */
		const val METER_COLOR_LEVEL = -1
		const val METER_COLOR_LIMIT = -2
		const val METER_COLOR_RED = -0x10000
		const val METER_COLOR_ORANGE = -0x8000
		const val METER_COLOR_YELLOW = -0x100
		const val METER_COLOR_GREEN = -0xff0100
		const val METER_COLOR_DARKGREEN = -0xff8000
		const val METER_COLOR_CYAN = -0xff0001
		const val METER_COLOR_DARKBLUE = -0xffff80
		const val METER_COLOR_BLUE = -0xffff01
		const val METER_COLOR_PURPLE = -0x7fff01
		const val METER_COLOR_PINK = -0xff01

		/** Constants of T-Spin Mini detection type */
		const val TSPINMINI_TYPE_ROTATECHECK = 0
		const val TSPINMINI_TYPE_WALLKICKFLAG = 1

		/** Spin detection type */
		const val SPINTYPE_4POINT = 0
		const val SPINTYPE_IMMOBILE = 1

		/** Constants of combo type */
		const val COMBO_TYPE_DISABLE = 0
		const val COMBO_TYPE_NORMAL = 1
		const val COMBO_TYPE_DOUBLE = 2

		/** Constants of gameplay-interruptable items */
		const val INTERRUPTITEM_NONE = 0
		const val INTERRUPTITEM_MIRROR = 1

		/** Table for cint-block item */
		val ITEM_COLOR_BRIGHT_TABLE =
			intArrayOf(10, 10, 9, 9, 8, 8, 8, 7, 7, 7, 6, 6, 6, 5, 5, 5, 4, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 2, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0)

		/** Default list of block colors to use for random block colors. */
		val BLOCK_COLORS_DEFAULT =
			intArrayOf(Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_ORANGE, Block.BLOCK_COLOR_YELLOW, Block.BLOCK_COLOR_GREEN, Block.BLOCK_COLOR_CYAN, Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_PURPLE)

		const val HANABI_INTERVAL = 10

		val EXPLOD_SIZE_DEFAULT =
			arrayOf(intArrayOf(4, 3), intArrayOf(3, 0), intArrayOf(3, 1), intArrayOf(3, 2), intArrayOf(3, 3), intArrayOf(4, 4), intArrayOf(5, 5), intArrayOf(5, 5), intArrayOf(6, 6), intArrayOf(6, 6), intArrayOf(7, 7))
	}
}
