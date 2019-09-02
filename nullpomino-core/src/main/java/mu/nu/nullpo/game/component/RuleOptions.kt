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
package mu.nu.nullpo.game.component

import mu.nu.nullpo.util.CustomProperties
import java.io.Serializable

/** ゲームルールの設定 data */
class RuleOptions:Serializable {

	/** このルールのName */
	var strRuleName:String = ""

	/** 使用するWallkickアルゴリズムのクラス名 (空文字列ならWallkickしない) */
	var strWallkick:String = ""

	/** 使用する出現順補正アルゴリズムのクラス名 (空文字列なら完全ランダム) */
	var strRandomizer:String = ""

	/** Game Style */
	var style:Int = 0

	var pieceOffset:Int = 0
	/** Blockピースのrotationパターンのcoordinate補正 (11ピース×4Direction) */
	var pieceOffsetX:Array<IntArray> = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}
	var pieceOffsetY:Array<IntArray> = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}

	/** Blockピースの出現X-coordinate補正 (11ピース×4Direction) */
	var pieceSpawnX:Array<IntArray> = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}

	/** Blockピースの出現Y-coordinate補正 (11ピース×4Direction) */
	var pieceSpawnY:Array<IntArray> = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}

	/** BlockピースのBig時の出現X-coordinate補正 (11ピース×4Direction) */
	var pieceSpawnXBig:Array<IntArray> = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}

	/** BlockピースのBig時の出現Y-coordinate補正 (11ピース×4Direction) */
	var pieceSpawnYBig:Array<IntArray> = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}

	/** Blockピース cint */
	var pieceColor:IntArray = IntArray(Piece.PIECE_COUNT)

	/** Blockピースの初期Direction */
	var pieceDefaultDirection:IntArray = IntArray(Piece.PIECE_COUNT)

	/** fieldより上から出現 */
	var pieceEnterAboveField:Boolean = false

	/** 出現予定地が埋まっているときにY-coordinateを上にずらすMaximum count */
	var pieceEnterMaxDistanceY:Int = 0

	/** fieldの幅 */
	var fieldWidth:Int = 0

	/** Field height */
	var fieldHeight:Int = 0

	/** fieldより上の見えない部分の高さ */
	var fieldHiddenHeight:Int = 0

	/** fieldの天井の有無 */
	var fieldCeiling:Boolean = false

	/** field枠内に置けなかったら死ぬかどうか */
	var fieldLockoutDeath:Boolean = false

	/** field枠外にはみ出しただけで死ぬかどうか */
	var fieldPartialLockoutDeath:Boolean = false

	/** NEXTのcount */
	var nextDisplay:Int = 0

	/** ホールド使用可否 */
	var holdEnable:Boolean = false

	/** 先行ホールド */
	var holdInitial:Boolean = false

	/** 先行ホールド連続使用不可 */
	var holdInitialLimit:Boolean = false

	/** ホールドを使ったときにBlockピースの向きを初期状態に戻す */
	var holdResetDirection:Boolean = false

	/** ホールドできる count (-1:無制限) */
	var holdLimit:Int = 0

	/** Hard drop使用可否 */
	var harddropEnable:Boolean = false

	/** Hard drop即固定 */
	var harddropLock:Boolean = false

	/** Hard drop連続使用不可 */
	var harddropLimit:Boolean = false

	/** Soft drop使用可否 */
	var softdropEnable:Boolean = false

	/** Soft drop即固定 */
	var softdropLock:Boolean = false

	/** Soft drop連続使用不可 */
	var softdropLimit:Boolean = false

	/** 接地状態でSoft dropすると即固定 */
	var softdropSurfaceLock:Boolean = false

	/** Soft drop速度 (1f=1G, .5f=0.5G) */
	var softdropSpeed:Float = 0f

	/** Soft drop速度を通常速度×n倍にする */
	var softdropMultiplyNativeSpeed:Boolean = false

	/** Soft drop速度を通常速度に影響させない */
	var softdropGravitySpeedLimit:Boolean = false

	/** 先行rotation */
	var rotateInitial:Boolean = false

	/** 先行rotation連続使用不可 */
	var rotateInitialLimit:Boolean = false

	/** Wallkick */
	var rotateWallkick:Boolean = false

	/** 先行rotationでもWallkickする */
	var rotateInitialWallkick:Boolean = false

	/** 上DirectionへのWallkickができる count (-1:無限) */
	var rotateMaxUpwardWallkick:Int = 0

	/** falseなら左が正rotation, When true,右が正rotation */
	var rotateButtonDefaultRight:Boolean = false

	/** 逆rotationを許可 (falseなら正rotationと同じ) */
	var rotateButtonAllowReverse:Boolean = false

	/** 180-degree rotationを許可 (falseなら正rotationと同じ) */
	var rotateButtonAllowDouble:Boolean = false

	/** 落下で固定 timeリセット */
	var lockresetFall:Boolean = false

	/** 移動で固定 timeリセット */
	var lockresetMove:Boolean = false

	/** rotationで固定 timeリセット */
	var lockresetRotate:Boolean = false

	/** Lock delay reset on wallkick */
	var lockresetWallkick:Boolean = false

	/** 横移動 count制限 (-1:無限) */
	var lockresetLimitMove:Int = 0

	/** rotation count制限 (-1:無限) */
	var lockresetLimitRotate:Int = 0

	/** 横移動 counterとrotation counterを共有 (横移動 counterだけ使う) */
	var lockresetLimitShareCount:Boolean = false

	/** 横移動 counterかrotation counterが超過したときの処理
	 * (LOCKRESET_LIMIT_OVER_で始まる定数を使う) */
	var lockresetLimitOver:Int = 0

	/** 固定した瞬間光る frame count */
	var lockflash:Int = 0

	/** Blockが光る専用 frame を入れる */
	var lockflashOnlyFrame:Boolean = false

	/** Line clear前にBlockが光る frame を入れる */
	var lockflashBeforeLineClear:Boolean = false

	/** ARE cancel on move */
	var areCancelMove:Boolean = false

	/** ARE cancel on rotate */
	var areCancelRotate:Boolean = false

	/** ARE cancel on hold */
	var areCancelHold:Boolean = false

	/** 最小/MaximumARE (-1:指定なし) */
	var minARE:Int = 0
	var maxARE:Int = 0

	/** 最小/MaximumARE after line clear (-1:指定なし) */
	var minARELine:Int = 0
	var maxARELine:Int = 0

	/** 最小/MaximumLine clear time (-1:指定なし) */
	var minLineDelay:Int = 0
	var maxLineDelay:Int = 0

	/** 最小/Maximum固定 time (-1:指定なし) */
	var minLockDelay:Int = 0
	var maxLockDelay:Int = 0

	/** 最小/Maximum横溜め time (-1:指定なし) */
	var minDAS:Int = 0
	var maxDAS:Int = 0

	/** 横移動間隔 */
	var dasDelay:Int = 0

	var shiftLockEnable:Boolean = false

	/** Ready画面で横溜め可能 */
	var dasInReady:Boolean = false

	/** 最初の frame で横溜め可能 */
	var dasInMoveFirstFrame:Boolean = false

	/** Blockが光った瞬間に横溜め可能 */
	var dasInLockFlash:Boolean = false

	/** Line clear中に横溜め可能 */
	var dasInLineClear:Boolean = false

	/** ARE中に横溜め可能 */
	var dasInARE:Boolean = false

	/** AREの最後の frame で横溜め可能 */
	var dasInARELastFrame:Boolean = false

	/** Ending突入画面で横溜め可能 */
	var dasInEndingStart:Boolean = false

	/** Charge DAS on blocked move */
	var dasChargeOnBlockedMove:Boolean = false

	/** Leave DAS charge alone when left/right are not held -- useful with
	 * dasRedirectInDelay
	 */
	var dasStoreChargeOnNeutral:Boolean = false

	/** Allow direction changes during delays without zeroing DAS charge */
	var dasRedirectInDelay:Boolean = false

	/** 最初の frame で移動可能 */
	var moveFirstFrame:Boolean = false

	/** 斜め移動 */
	var moveDiagonal:Boolean = false

	/** 上下同時押し許可 */
	var moveUpAndDown:Boolean = false

	/** 左右同時押し許可 */
	var moveLeftAndRightAllow:Boolean = false

	/** 左右同時押ししたときに前の frame の input Directionを優先する (左を押しながら右を押すと右を無視して左を優先) */
	var moveLeftAndRightUsePreviousInput:Boolean = false

	/** Line clear後に上のBlockが1段ずつ落ちるアニメーションを表示 */
	var lineFallAnim:Boolean = false

	/** Line delay cancel on move */
	var lineCancelMove:Boolean = false

	/** Line delay cancel on rotate */
	var lineCancelRotate:Boolean = false

	/** Line delay cancel on hold */
	var lineCancelHold:Boolean = false

	/** Blockの絵柄 */
	var skin:Int = 0

	/** ghost の有無 (falseならMode 側でghost を is enabledにしていても非表示) */
	var ghost:Boolean = false

	/** Constructor */
	constructor() {
		reset()
	}

	/** Copy constructor
	 * @param r Copy source
	 */
	constructor(r:RuleOptions?) {
		copy(r)
	}

	/** Initialization */
	fun reset() {
		strRuleName = ""
		strWallkick = ""
		strRandomizer = ""

		style = 0
		pieceOffset = PIECEOFFSET_NONE
		pieceOffsetX = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}
		pieceOffsetY = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}
		pieceSpawnX = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}
		pieceSpawnY = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}
		pieceSpawnXBig = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}
		pieceSpawnYBig = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}

		pieceColor = IntArray(Piece.PIECE_COUNT)
		pieceColor[Piece.PIECE_I] = Block.BLOCK_COLOR_GRAY
		pieceColor[Piece.PIECE_L] = Block.BLOCK_COLOR_GRAY
		pieceColor[Piece.PIECE_O] = Block.BLOCK_COLOR_GRAY
		pieceColor[Piece.PIECE_Z] = Block.BLOCK_COLOR_GRAY
		pieceColor[Piece.PIECE_T] = Block.BLOCK_COLOR_GRAY
		pieceColor[Piece.PIECE_J] = Block.BLOCK_COLOR_GRAY
		pieceColor[Piece.PIECE_S] = Block.BLOCK_COLOR_GRAY
		pieceColor[Piece.PIECE_I1] = Block.BLOCK_COLOR_PURPLE
		pieceColor[Piece.PIECE_I2] = Block.BLOCK_COLOR_BLUE
		pieceColor[Piece.PIECE_I3] = Block.BLOCK_COLOR_GREEN
		pieceColor[Piece.PIECE_L3] = Block.BLOCK_COLOR_ORANGE

		pieceDefaultDirection = IntArray(Piece.PIECE_COUNT)
		pieceEnterAboveField = true
		pieceEnterMaxDistanceY = 0

		fieldWidth = Field.DEFAULT_WIDTH
		fieldHeight = Field.DEFAULT_HEIGHT
		fieldHiddenHeight = Field.DEFAULT_HIDDEN_HEIGHT
		fieldCeiling = false
		fieldLockoutDeath = true
		fieldPartialLockoutDeath = false

		nextDisplay = 3

		holdEnable = true
		holdInitial = true
		holdInitialLimit = false
		holdResetDirection = true
		holdLimit = -1

		harddropEnable = true
		harddropLock = true
		harddropLimit = true

		softdropEnable = true
		softdropLock = false
		softdropLimit = false
		softdropSurfaceLock = false
		softdropSpeed = .5f
		softdropMultiplyNativeSpeed = false
		softdropGravitySpeedLimit = false

		rotateInitial = true
		rotateInitialLimit = false
		rotateWallkick = true
		rotateInitialWallkick = true
		rotateMaxUpwardWallkick = -1
		rotateButtonDefaultRight = true
		rotateButtonAllowReverse = true
		rotateButtonAllowDouble = true

		lockresetFall = true
		lockresetMove = true
		lockresetRotate = true
		lockresetWallkick = false
		lockresetLimitMove = 15
		lockresetLimitRotate = 15
		lockresetLimitShareCount = true
		lockresetLimitOver = LOCKRESET_LIMIT_OVER_INSTANT

		lockflash = 2
		lockflashOnlyFrame = true
		lockflashBeforeLineClear = false
		areCancelMove = false
		areCancelRotate = false
		areCancelHold = false

		minARE = -1
		maxARE = -1
		minARELine = -1
		maxARELine = -1
		minLineDelay = -1
		maxLineDelay = -1
		minLockDelay = -1
		maxLockDelay = -1
		minDAS = -1
		maxDAS = -1

		dasDelay = 0

		shiftLockEnable = false

		dasInReady = true
		dasInMoveFirstFrame = true
		dasInLockFlash = true
		dasInLineClear = true
		dasInARE = true
		dasInARELastFrame = true
		dasInEndingStart = true
		dasChargeOnBlockedMove = false
		dasStoreChargeOnNeutral = false
		dasRedirectInDelay = false

		moveFirstFrame = true
		moveDiagonal = true
		moveUpAndDown = true
		moveLeftAndRightAllow = true
		moveLeftAndRightUsePreviousInput = false

		lineFallAnim = true
		lineCancelMove = false
		lineCancelRotate = false
		lineCancelHold = false

		skin = 0
		ghost = true
	}

	/** 他のRuleParamの内容をコピー
	 * @param r Copy sourceのRuleParam
	 */
	fun copy(r:RuleOptions?) {
		r?.let{o->
		strRuleName = o.strRuleName
		strWallkick = o.strWallkick
		strRandomizer = o.strRandomizer

		style = o.style
		pieceOffset = o.pieceOffset
		pieceOffsetX = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}
		pieceOffsetY = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}
		pieceSpawnX = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}
		pieceSpawnY = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}
		pieceSpawnXBig = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}
		pieceSpawnYBig = Array(Piece.PIECE_COUNT) {IntArray(Piece.DIRECTION_COUNT)}
		pieceColor = IntArray(Piece.PIECE_COUNT)
		pieceDefaultDirection = IntArray(Piece.PIECE_COUNT)
		for(i in 0 until Piece.PIECE_COUNT) {
			for(j in 0 until Piece.DIRECTION_COUNT) {
				pieceOffsetX[i][j] = o.pieceOffsetX[i][j]
				pieceOffsetY[i][j] = o.pieceOffsetY[i][j]
				pieceSpawnX[i][j] = o.pieceSpawnX[i][j]
				pieceSpawnY[i][j] = o.pieceSpawnY[i][j]
				pieceSpawnXBig[i][j] = o.pieceSpawnXBig[i][j]
				pieceSpawnYBig[i][j] = o.pieceSpawnYBig[i][j]
			}
			pieceColor[i] = o.pieceColor[i]
			pieceDefaultDirection[i] = o.pieceDefaultDirection[i]
		}
		pieceEnterAboveField = o.pieceEnterAboveField
		pieceEnterMaxDistanceY = o.pieceEnterMaxDistanceY

		fieldWidth = o.fieldWidth
		fieldHeight = o.fieldHeight
		fieldHiddenHeight = o.fieldHiddenHeight
		fieldCeiling = o.fieldCeiling
		fieldLockoutDeath = o.fieldLockoutDeath
		fieldPartialLockoutDeath = o.fieldPartialLockoutDeath

		nextDisplay = o.nextDisplay

		holdEnable = o.holdEnable
		holdInitial = o.holdInitial
		holdInitialLimit = o.holdInitialLimit
		holdResetDirection = o.holdResetDirection
		holdLimit = o.holdLimit

		harddropEnable = o.harddropEnable
		harddropLock = o.harddropLock
		harddropLimit = o.harddropLimit

		softdropEnable = o.softdropEnable
		softdropLock = o.softdropLock
		softdropLimit = o.softdropLimit
		softdropSurfaceLock = o.softdropSurfaceLock
		softdropSpeed = o.softdropSpeed
		softdropMultiplyNativeSpeed = o.softdropMultiplyNativeSpeed
		softdropGravitySpeedLimit = o.softdropGravitySpeedLimit

		rotateInitial = o.rotateInitial
		rotateInitialLimit = o.rotateInitialLimit
		rotateWallkick = o.rotateWallkick
		rotateInitialWallkick = o.rotateInitialWallkick
		rotateMaxUpwardWallkick = o.rotateMaxUpwardWallkick
		rotateButtonDefaultRight = o.rotateButtonDefaultRight
		rotateButtonAllowReverse = o.rotateButtonAllowReverse
		rotateButtonAllowDouble = o.rotateButtonAllowDouble

		lockresetFall = o.lockresetFall
		lockresetMove = o.lockresetMove
		lockresetRotate = o.lockresetRotate
		lockresetWallkick = o.lockresetWallkick
		lockresetLimitMove = o.lockresetLimitMove
		lockresetLimitRotate = o.lockresetLimitRotate
		lockresetLimitShareCount = o.lockresetLimitShareCount
		lockresetLimitOver = o.lockresetLimitOver

		lockflash = o.lockflash
		lockflashOnlyFrame = o.lockflashOnlyFrame
		lockflashBeforeLineClear = o.lockflashBeforeLineClear
		areCancelMove = o.areCancelMove
		areCancelRotate = o.areCancelRotate
		areCancelHold = o.areCancelHold

		minARE = o.minARE
		maxARE = o.maxARE
		minARELine = o.minARELine
		maxARELine = o.maxARELine
		minLineDelay = o.minLineDelay
		maxLineDelay = o.maxLineDelay
		minLockDelay = o.minLockDelay
		maxLockDelay = o.maxLockDelay
		minDAS = o.minDAS
		maxDAS = o.maxDAS

		dasDelay = o.dasDelay

		shiftLockEnable = o.shiftLockEnable

		dasInReady = o.dasInReady
		dasInMoveFirstFrame = o.dasInMoveFirstFrame
		dasInLockFlash = o.dasInLockFlash
		dasInLineClear = o.dasInLineClear
		dasInARE = o.dasInARE
		dasInARELastFrame = o.dasInARELastFrame
		dasInEndingStart = o.dasInEndingStart
		dasChargeOnBlockedMove = o.dasChargeOnBlockedMove
		dasStoreChargeOnNeutral = o.dasStoreChargeOnNeutral
		dasRedirectInDelay = o.dasRedirectInDelay

		moveFirstFrame = o.moveFirstFrame
		moveDiagonal = o.moveDiagonal
		moveUpAndDown = o.moveUpAndDown
		moveLeftAndRightAllow = o.moveLeftAndRightAllow
		moveLeftAndRightUsePreviousInput = o.moveLeftAndRightUsePreviousInput

		lineFallAnim = o.lineFallAnim
		lineCancelMove = o.lineCancelMove
		lineCancelRotate = o.lineCancelRotate
		lineCancelHold = o.lineCancelHold

		skin = o.skin
		ghost = o.ghost
		}?:reset()
	}

	/** 他のルールと比較し, 同じならtrueを返す
	 * @param r 比較するルール
	 * @param ignoreGraphicsSetting trueにするとゲーム自体に影響しない設定を無視
	 * @return 比較したルールと同じならtrue
	 */
	fun compare(r:RuleOptions, ignoreGraphicsSetting:Boolean):Boolean {
		if(!ignoreGraphicsSetting&&strRuleName!=r.strRuleName) return false
		if(strWallkick!=r.strWallkick) return false
		if(strRandomizer!=r.strRandomizer) return false

		if(style!=r.style) return false
		if(pieceOffset!=r.pieceOffset) return false
		for(i in 0 until Piece.PIECE_COUNT) {
			if(pieceOffset==PIECEOFFSET_ASSIGN)
				for(j in 0 until Piece.DIRECTION_COUNT) {
					if(pieceOffsetX[i][j]!=r.pieceOffsetX[i][j]) return false
					if(pieceOffsetY[i][j]!=r.pieceOffsetY[i][j]) return false
					if(pieceSpawnX[i][j]!=r.pieceSpawnX[i][j]) return false
					if(pieceSpawnY[i][j]!=r.pieceSpawnY[i][j]) return false
					if(pieceSpawnXBig[i][j]!=r.pieceSpawnXBig[i][j]) return false
					if(pieceSpawnYBig[i][j]!=r.pieceSpawnYBig[i][j]) return false
				}
			if(!ignoreGraphicsSetting&&pieceColor[i]!=r.pieceColor[i]) return false
			if(pieceDefaultDirection[i]!=r.pieceDefaultDirection[i]) return false
		}

		if(pieceEnterAboveField!=r.pieceEnterAboveField) return false
		if(pieceEnterMaxDistanceY!=r.pieceEnterMaxDistanceY) return false

		if(fieldWidth!=r.fieldWidth) return false
		if(fieldHeight!=r.fieldHeight) return false
		if(fieldHiddenHeight!=r.fieldHiddenHeight) return false
		if(fieldCeiling!=r.fieldCeiling) return false
		if(fieldLockoutDeath!=r.fieldLockoutDeath) return false
		if(fieldPartialLockoutDeath!=r.fieldPartialLockoutDeath) return false

		if(nextDisplay!=r.nextDisplay) return false

		if(holdEnable!=r.holdEnable) return false
		if(holdInitial!=r.holdInitial) return false
		if(holdInitialLimit!=r.holdInitialLimit) return false
		if(holdResetDirection!=r.holdResetDirection) return false
		if(holdLimit!=r.holdLimit) return false

		if(harddropEnable!=r.harddropEnable) return false
		if(harddropLock!=r.harddropLock) return false
		if(harddropLimit!=r.harddropLimit) return false

		if(softdropEnable!=r.softdropEnable) return false
		if(softdropLock!=r.softdropLock) return false
		if(softdropLimit!=r.softdropLimit) return false
		if(softdropSurfaceLock!=r.softdropSurfaceLock) return false
		if(softdropSpeed!=r.softdropSpeed) return false
		if(softdropMultiplyNativeSpeed!=r.softdropMultiplyNativeSpeed) return false
		if(softdropGravitySpeedLimit!=r.softdropGravitySpeedLimit) return false

		if(rotateInitial!=r.rotateInitial) return false
		if(rotateInitialLimit!=r.rotateInitialLimit) return false
		if(rotateWallkick!=r.rotateWallkick) return false
		if(rotateInitialWallkick!=r.rotateInitialWallkick) return false
		if(rotateMaxUpwardWallkick!=r.rotateMaxUpwardWallkick) return false
		if(rotateButtonDefaultRight!=r.rotateButtonDefaultRight) return false
		if(rotateButtonAllowReverse!=r.rotateButtonAllowReverse) return false
		if(rotateButtonAllowDouble!=r.rotateButtonAllowDouble) return false

		if(lockresetFall!=r.lockresetFall) return false
		if(lockresetMove!=r.lockresetMove) return false
		if(lockresetRotate!=r.lockresetRotate) return false
		if(lockresetWallkick!=r.lockresetWallkick) return false
		if(lockresetLimitMove!=r.lockresetLimitMove) return false
		if(lockresetLimitRotate!=r.lockresetLimitRotate) return false
		if(lockresetLimitShareCount!=r.lockresetLimitShareCount) return false
		if(lockresetLimitOver!=r.lockresetLimitOver) return false

		if(lockflash!=r.lockflash) return false
		if(lockflashOnlyFrame!=r.lockflashOnlyFrame) return false
		if(lockflashBeforeLineClear!=r.lockflashBeforeLineClear) return false
		if(areCancelMove!=r.areCancelMove) return false
		if(areCancelRotate!=r.areCancelRotate) return false
		if(areCancelHold!=r.areCancelHold) return false

		if(minARE!=r.minARE) return false
		if(maxARE!=r.maxARE) return false
		if(minARELine!=r.minARELine) return false
		if(maxARELine!=r.maxARELine) return false
		if(minLineDelay!=r.minLineDelay) return false
		if(maxLineDelay!=r.maxLineDelay) return false
		if(minLockDelay!=r.minLockDelay) return false
		if(maxLockDelay!=r.maxLockDelay) return false
		if(minDAS!=r.minDAS) return false
		if(maxDAS!=r.maxDAS) return false

		if(dasDelay!=r.dasDelay) return false

		if(shiftLockEnable!=r.shiftLockEnable) return false

		if(dasInReady!=r.dasInReady) return false
		if(dasInMoveFirstFrame!=r.dasInMoveFirstFrame) return false
		if(dasInLockFlash!=r.dasInLockFlash) return false
		if(dasInLineClear!=r.dasInLineClear) return false
		if(dasInARE!=r.dasInARE) return false
		if(dasInARELastFrame!=r.dasInARELastFrame) return false
		if(dasInEndingStart!=r.dasInEndingStart) return false
		if(dasChargeOnBlockedMove!=r.dasChargeOnBlockedMove) return false
		if(dasStoreChargeOnNeutral!=r.dasStoreChargeOnNeutral) return false
		if(dasRedirectInDelay!=r.dasRedirectInDelay) return false

		if(moveFirstFrame!=r.moveFirstFrame) return false
		if(moveDiagonal!=r.moveDiagonal) return false
		if(moveUpAndDown!=r.moveUpAndDown) return false
		if(moveLeftAndRightAllow!=r.moveLeftAndRightAllow) return false
		if(moveLeftAndRightUsePreviousInput!=r.moveLeftAndRightUsePreviousInput) return false

		if(ignoreGraphicsSetting&&lineFallAnim!=r.lineFallAnim) return false
		if(lineCancelMove!=r.lineCancelMove) return false
		if(lineCancelRotate!=r.lineCancelRotate) return false
		if(lineCancelHold!=r.lineCancelHold) return false

		return if(ignoreGraphicsSetting&&skin!=r.skin) false else ghost==r.ghost
	}

	/** プロパティセットに保存
	 * @param p プロパティセット
	 * @param id Player IDまたはPresetID
	 */
	fun writeProperty(p:CustomProperties, id:Int) {
		p.setProperty("$id.ruleopt.strRuleName", strRuleName)
		p.setProperty("$id.ruleopt.strWallkick", strWallkick)
		p.setProperty("$id.ruleopt.strRandomizer", strRandomizer)

		p.setProperty("$id.ruleopt.style", style)
		p.setProperty("$id.ruleopt.pieceOffset", pieceOffset)

		for(i in 0 until Piece.PIECE_COUNT) {
			if(pieceOffset==PIECEOFFSET_ASSIGN)
				for(j in 0 until Piece.DIRECTION_COUNT) {
					p.setProperty("$id.ruleopt.pieceOffsetX.$i.$j", pieceOffsetX[i][j])
					p.setProperty("$id.ruleopt.pieceOffsetY.$i.$j", pieceOffsetY[i][j])
					p.setProperty("$id.ruleopt.pieceSpawnX.$i.$j", pieceSpawnX[i][j])
					p.setProperty("$id.ruleopt.pieceSpawnY.$i.$j", pieceSpawnY[i][j])
					p.setProperty("$id.ruleopt.pieceSpawnXBig.$i.$j", pieceSpawnXBig[i][j])
					p.setProperty("$id.ruleopt.pieceSpawnYBig.$i.$j", pieceSpawnYBig[i][j])
				}
			p.setProperty("$id.ruleopt.pieceColor.$i", pieceColor[i])
			p.setProperty("$id.ruleopt.pieceDefaultDirection.$i", pieceDefaultDirection[i])
		}
		p.setProperty("$id.ruleopt.pieceEnterAboveField", pieceEnterAboveField)
		p.setProperty("$id.ruleopt.pieceEnterMaxDistanceY", pieceEnterMaxDistanceY)

		p.setProperty("$id.ruleopt.fieldWidth", fieldWidth)
		p.setProperty("$id.ruleopt.fieldHeight", fieldHeight)
		p.setProperty("$id.ruleopt.fieldHiddenHeight", fieldHiddenHeight)
		p.setProperty("$id.ruleopt.fieldCeiling", fieldCeiling)
		p.setProperty("$id.ruleopt.fieldLockoutDeath", fieldLockoutDeath)
		p.setProperty("$id.ruleopt.fieldPartialLockoutDeath", fieldPartialLockoutDeath)

		p.setProperty("$id.ruleopt.nextDisplay", nextDisplay)

		p.setProperty("$id.ruleopt.holdEnable", holdEnable)
		p.setProperty("$id.ruleopt.holdInitial", holdInitial)
		p.setProperty("$id.ruleopt.holdInitialLimit", holdInitialLimit)
		p.setProperty("$id.ruleopt.holdResetDirection", holdResetDirection)
		p.setProperty("$id.ruleopt.holdLimit", holdLimit)

		p.setProperty("$id.ruleopt.harddropEnable", harddropEnable)
		p.setProperty("$id.ruleopt.harddropLock", harddropLock)
		p.setProperty("$id.ruleopt.harddropLimit", harddropLimit)

		p.setProperty("$id.ruleopt.softdropEnable", softdropEnable)
		p.setProperty("$id.ruleopt.softdropLock", softdropLock)
		p.setProperty("$id.ruleopt.softdropLimit", softdropLimit)
		p.setProperty("$id.ruleopt.softdropSurfaceLock", softdropSurfaceLock)
		p.setProperty("$id.ruleopt.softdropSpeed", softdropSpeed)
		p.setProperty("$id.ruleopt.softdropMultiplyNativeSpeed", softdropMultiplyNativeSpeed)
		p.setProperty("$id.ruleopt.softdropGravitySpeedLimit", softdropGravitySpeedLimit)

		p.setProperty("$id.ruleopt.rotateInitial", rotateInitial)
		p.setProperty("$id.ruleopt.rotateInitialLimit", rotateInitialLimit)
		p.setProperty("$id.ruleopt.rotateWallkick", rotateWallkick)
		p.setProperty("$id.ruleopt.rotateInitialWallkick", rotateInitialWallkick)
		p.setProperty("$id.ruleopt.rotateMaxUpwardWallkick", rotateMaxUpwardWallkick)
		p.setProperty("$id.ruleopt.rotateButtonDefaultRight", rotateButtonDefaultRight)
		p.setProperty("$id.ruleopt.rotateButtonAllowReverse", rotateButtonAllowReverse)
		p.setProperty("$id.ruleopt.rotateButtonAllowDouble", rotateButtonAllowDouble)

		p.setProperty("$id.ruleopt.lockresetFall", lockresetFall)
		p.setProperty("$id.ruleopt.lockresetMove", lockresetMove)
		p.setProperty("$id.ruleopt.lockresetRotate", lockresetRotate)
		p.setProperty("$id.ruleopt.lockresetWallkick", lockresetWallkick)
		p.setProperty("$id.ruleopt.lockresetLimitMove", lockresetLimitMove)
		p.setProperty("$id.ruleopt.lockresetLimitRotate", lockresetLimitRotate)
		p.setProperty("$id.ruleopt.lockresetLimitShareCount", lockresetLimitShareCount)
		p.setProperty("$id.ruleopt.lockresetLimitOver", lockresetLimitOver)

		p.setProperty("$id.ruleopt.lockflash", lockflash)
		p.setProperty("$id.ruleopt.lockflashOnlyFrame", lockflashOnlyFrame)
		p.setProperty("$id.ruleopt.lockflashBeforeLineClear", lockflashBeforeLineClear)
		p.setProperty("$id.ruleopt.areCancelMove", areCancelMove)
		p.setProperty("$id.ruleopt.areCancelRotate", areCancelRotate)
		p.setProperty("$id.ruleopt.areCancelHold", areCancelHold)

		p.setProperty("$id.ruleopt.minARE", minARE)
		p.setProperty("$id.ruleopt.maxARE", maxARE)
		p.setProperty("$id.ruleopt.minARELine", minARELine)
		p.setProperty("$id.ruleopt.maxARELine", maxARELine)
		p.setProperty("$id.ruleopt.minLineDelay", minLineDelay)
		p.setProperty("$id.ruleopt.maxLineDelay", maxLineDelay)
		p.setProperty("$id.ruleopt.minLockDelay", minLockDelay)
		p.setProperty("$id.ruleopt.maxLockDelay", maxLockDelay)
		p.setProperty("$id.ruleopt.minDAS", minDAS)
		p.setProperty("$id.ruleopt.maxDAS", maxDAS)

		p.setProperty("$id.ruleopt.dasDelay", dasDelay)

		p.setProperty("$id.ruleopt.shiftLockEnable", shiftLockEnable)

		p.setProperty("$id.ruleopt.dasInReady", dasInReady)
		p.setProperty("$id.ruleopt.dasInMoveFirstFrame", dasInMoveFirstFrame)
		p.setProperty("$id.ruleopt.dasInLockFlash", dasInLockFlash)
		p.setProperty("$id.ruleopt.dasInLineClear", dasInLineClear)
		p.setProperty("$id.ruleopt.dasInARE", dasInARE)
		p.setProperty("$id.ruleopt.dasInARELastFrame", dasInARELastFrame)
		p.setProperty("$id.ruleopt.dasInEndingStart", dasInEndingStart)
		p.setProperty("$id.ruleopt.dasOnBlockedMove", dasChargeOnBlockedMove)
		p.setProperty("$id.ruleopt.dasStoreChargeOnNeutral", dasStoreChargeOnNeutral)
		p.setProperty("$id.ruleopt.dasRedirectInARE", dasRedirectInDelay)

		p.setProperty("$id.ruleopt.moveFirstFrame", moveFirstFrame)
		p.setProperty("$id.ruleopt.moveDiagonal", moveDiagonal)
		p.setProperty("$id.ruleopt.moveUpAndDown", moveUpAndDown)
		p.setProperty("$id.ruleopt.moveLeftAndRightAllow", moveLeftAndRightAllow)
		p.setProperty("$id.ruleopt.moveLeftAndRightUsePreviousInput", moveLeftAndRightUsePreviousInput)

		p.setProperty("$id.ruleopt.lineFallAnim", lineFallAnim)
		p.setProperty("$id.ruleopt.lineCancelMove", lineCancelMove)
		p.setProperty("$id.ruleopt.lineCancelRotate", lineCancelRotate)
		p.setProperty("$id.ruleopt.lineCancelHold", lineCancelHold)

		p.setProperty("$id.ruleopt.skin", skin)
		p.setProperty("$id.ruleopt.ghost", ghost)
	}

	@JvmOverloads
	fun readProperty(p:CustomProperties, id:Int, offset:Boolean = false) {
		strRuleName = p.getProperty("$id.ruleopt.strRuleName", strRuleName)
		strWallkick = p.getProperty("$id.ruleopt.strWallkick", strWallkick)
		strRandomizer = p.getProperty("$id.ruleopt.strRandomizer", strRandomizer)

		style = p.getProperty("$id.ruleopt.style", 0)
		pieceOffset = p.getProperty("$id.ruleopt.pieceOffset", PIECEOFFSET_NONE)
		for(i in 0 until Piece.PIECE_COUNT) {
			for(j in 0 until Piece.DIRECTION_COUNT)
				when(if(offset) PIECEOFFSET_ASSIGN else pieceOffset) {
					PIECEOFFSET_NONE -> {
						pieceSpawnYBig[i][j] = 0
						pieceSpawnXBig[i][j] = 0
						pieceSpawnY[i][j] = 0
						pieceSpawnX[i][j] = 0
						pieceOffsetY[i][j] = 0
						pieceOffsetX[i][j] = 0
					}
					PIECEOFFSET_BIASED -> {
						pieceOffsetX[i][j] = PIECEOFFSET_ARSPRESET[0][i][j]
						pieceOffsetY[i][j] = PIECEOFFSET_ARSPRESET[1][i][j]
						pieceSpawnYBig[i][j] = 0
						pieceSpawnXBig[i][j] = pieceSpawnYBig[i][j]
						pieceSpawnY[i][j] = pieceSpawnXBig[i][j]
						pieceSpawnX[i][j] = pieceSpawnY[i][j]
					}
					PIECEOFFSET_BOTTOM -> {
						pieceOffsetY[i][j] = PIECEOFFSET_ARSPRESET[1][i][j]
						pieceSpawnYBig[i][j] = 0
						pieceSpawnXBig[i][j] = pieceSpawnYBig[i][j]
						pieceSpawnY[i][j] = pieceSpawnXBig[i][j]
						pieceSpawnX[i][j] = pieceSpawnY[i][j]
					}
					PIECEOFFSET_ASSIGN -> {
						pieceOffsetX[i][j] = p.getProperty("$id.ruleopt.pieceOffsetX.$i.$j", pieceOffsetX[i][j])
						pieceOffsetY[i][j] = p.getProperty("$id.ruleopt.pieceOffsetY.$i.$j", pieceOffsetY[i][j])
						pieceSpawnX[i][j] = p.getProperty("$id.ruleopt.pieceSpawnX.$i.$j", pieceSpawnX[i][j])
						pieceSpawnY[i][j] = p.getProperty("$id.ruleopt.pieceSpawnY.$i.$j", pieceSpawnY[i][j])
						pieceSpawnXBig[i][j] = p.getProperty("$id.ruleopt.pieceSpawnXBig.$i.$j", pieceSpawnXBig[i][j])
						pieceSpawnYBig[i][j] = p.getProperty("$id.ruleopt.pieceSpawnYBig.$i.$j", pieceSpawnYBig[i][j])
					}
				}

			pieceColor[i] = p.getProperty("$id.ruleopt.pieceColor.$i", pieceColor[i])
			pieceDefaultDirection[i] = p.getProperty("$id.ruleopt.pieceDefaultDirection.$i", pieceDefaultDirection[i])
		}
		pieceEnterAboveField = p.getProperty("$id.ruleopt.pieceEnterAboveField", pieceEnterAboveField)
		pieceEnterMaxDistanceY = p.getProperty("$id.ruleopt.pieceEnterMaxDistanceY", pieceEnterMaxDistanceY)

		fieldWidth = p.getProperty("$id.ruleopt.fieldWidth", fieldWidth)
		fieldHeight = p.getProperty("$id.ruleopt.fieldHeight", fieldHeight)
		fieldHiddenHeight = p.getProperty("$id.ruleopt.fieldHiddenHeight", fieldHiddenHeight)
		fieldCeiling = p.getProperty("$id.ruleopt.fieldCeiling", fieldCeiling)
		fieldLockoutDeath = p.getProperty("$id.ruleopt.fieldLockoutDeath", fieldLockoutDeath)
		fieldPartialLockoutDeath = p.getProperty("$id.ruleopt.fieldPartialLockoutDeath", fieldPartialLockoutDeath)

		nextDisplay = p.getProperty("$id.ruleopt.nextDisplay", nextDisplay)

		holdEnable = p.getProperty("$id.ruleopt.holdEnable", holdEnable)
		holdInitial = p.getProperty("$id.ruleopt.holdInitial", holdInitial)
		holdInitialLimit = p.getProperty("$id.ruleopt.holdInitialLimit", holdInitialLimit)
		holdResetDirection = p.getProperty("$id.ruleopt.holdResetDirection", holdResetDirection)
		holdLimit = p.getProperty("$id.ruleopt.holdLimit", holdLimit)

		harddropEnable = p.getProperty("$id.ruleopt.harddropEnable", harddropEnable)
		harddropLock = p.getProperty("$id.ruleopt.harddropLock", harddropLock)
		harddropLimit = p.getProperty("$id.ruleopt.harddropLimit", harddropLimit)

		softdropEnable = p.getProperty("$id.ruleopt.softdropEnable", softdropEnable)
		softdropLock = p.getProperty("$id.ruleopt.softdropLock", softdropLock)
		softdropLimit = p.getProperty("$id.ruleopt.softdropLimit", softdropLimit)
		softdropSurfaceLock = p.getProperty("$id.ruleopt.softdropSurfaceLock", softdropSurfaceLock)
		softdropSpeed = p.getProperty("$id.ruleopt.softdropSpeed", softdropSpeed)
		softdropMultiplyNativeSpeed = p.getProperty("$id.ruleopt.softdropMultiplyNativeSpeed", softdropMultiplyNativeSpeed)
		softdropGravitySpeedLimit = p.getProperty("$id.ruleopt.softdropGravitySpeedLimit", softdropGravitySpeedLimit)

		rotateInitial = p.getProperty("$id.ruleopt.rotateInitial", rotateInitial)
		rotateInitialLimit = p.getProperty("$id.ruleopt.rotateInitialLimit", rotateInitialLimit)
		rotateWallkick = p.getProperty("$id.ruleopt.rotateWallkick", rotateWallkick)
		rotateInitialWallkick = p.getProperty("$id.ruleopt.rotateInitialWallkick", rotateInitialWallkick)
		rotateMaxUpwardWallkick = p.getProperty("$id.ruleopt.rotateMaxUpwardWallkick", rotateMaxUpwardWallkick)
		rotateButtonDefaultRight = p.getProperty("$id.ruleopt.rotateButtonDefaultRight", rotateButtonDefaultRight)
		rotateButtonAllowReverse = p.getProperty("$id.ruleopt.rotateButtonAllowReverse", rotateButtonAllowReverse)
		rotateButtonAllowDouble = p.getProperty("$id.ruleopt.rotateButtonAllowDouble", rotateButtonAllowDouble)

		lockresetFall = p.getProperty("$id.ruleopt.lockresetFall", lockresetFall)
		lockresetMove = p.getProperty("$id.ruleopt.lockresetMove", lockresetMove)
		lockresetRotate = p.getProperty("$id.ruleopt.lockresetRotate", lockresetRotate)
		lockresetWallkick = p.getProperty("$id.ruleopt.lockresetWallkick", lockresetWallkick)
		lockresetLimitMove = p.getProperty("$id.ruleopt.lockresetLimitMove", lockresetLimitMove)
		lockresetLimitRotate = p.getProperty("$id.ruleopt.lockresetLimitRotate", lockresetLimitRotate)
		lockresetLimitShareCount = p.getProperty("$id.ruleopt.lockresetLimitShareCount", lockresetLimitShareCount)
		lockresetLimitOver = p.getProperty("$id.ruleopt.lockresetLimitOver", lockresetLimitOver)

		lockflash = p.getProperty("$id.ruleopt.lockflash", lockflash)
		lockflashOnlyFrame = p.getProperty("$id.ruleopt.lockflashOnlyFrame", lockflashOnlyFrame)
		lockflashBeforeLineClear = p.getProperty("$id.ruleopt.lockflashBeforeLineClear", lockflashBeforeLineClear)
		areCancelMove = p.getProperty("$id.ruleopt.areCancelMove", areCancelMove)
		areCancelRotate = p.getProperty("$id.ruleopt.areCancelRotate", areCancelRotate)
		areCancelHold = p.getProperty("$id.ruleopt.areCancelHold", areCancelHold)

		minARE = p.getProperty("$id.ruleopt.minARE", minARE)
		maxARE = p.getProperty("$id.ruleopt.maxARE", maxARE)
		minARELine = p.getProperty("$id.ruleopt.minARELine", minARELine)
		maxARELine = p.getProperty("$id.ruleopt.maxARELine", maxARELine)
		minLineDelay = p.getProperty("$id.ruleopt.minLineDelay", minLineDelay)
		maxLineDelay = p.getProperty("$id.ruleopt.maxLineDelay", maxLineDelay)
		minLockDelay = p.getProperty("$id.ruleopt.minLockDelay", minLockDelay)
		maxLockDelay = p.getProperty("$id.ruleopt.maxLockDelay", maxLockDelay)
		minDAS = p.getProperty("$id.ruleopt.minDAS", minDAS)
		maxDAS = p.getProperty("$id.ruleopt.maxDAS", maxDAS)

		dasDelay = p.getProperty("$id.ruleopt.dasDelay", dasDelay)
		shiftLockEnable = p.getProperty("$id.ruleopt.shiftLockEnable", shiftLockEnable)

		dasInReady = p.getProperty("$id.ruleopt.dasInReady", dasInReady)
		dasInMoveFirstFrame = p.getProperty("$id.ruleopt.dasInMoveFirstFrame", dasInMoveFirstFrame)
		dasInLockFlash = p.getProperty("$id.ruleopt.dasInLockFlash", dasInLockFlash)
		dasInLineClear = p.getProperty("$id.ruleopt.dasInLineClear", dasInLineClear)
		dasInARE = p.getProperty("$id.ruleopt.dasInARE", dasInARE)
		dasInARELastFrame = p.getProperty("$id.ruleopt.dasInARELastFrame", dasInARELastFrame)
		dasInEndingStart = p.getProperty("$id.ruleopt.dasInEndingStart", dasInEndingStart)
		dasChargeOnBlockedMove = p.getProperty("$id.ruleopt.dasOnBlockedMove", dasChargeOnBlockedMove)
		dasStoreChargeOnNeutral = p.getProperty("$id.ruleopt.dasStoreChargeOnNeutral", dasStoreChargeOnNeutral)
		dasRedirectInDelay = p.getProperty("$id.ruleopt.dasRedirectInARE", dasRedirectInDelay)

		moveFirstFrame = p.getProperty("$id.ruleopt.moveFirstFrame", moveFirstFrame)
		moveDiagonal = p.getProperty("$id.ruleopt.moveDiagonal", moveDiagonal)
		moveUpAndDown = p.getProperty("$id.ruleopt.moveUpAndDown", moveUpAndDown)
		moveLeftAndRightAllow = p.getProperty("$id.ruleopt.moveLeftAndRightAllow", moveLeftAndRightAllow)
		moveLeftAndRightUsePreviousInput = p.getProperty("$id.ruleopt.moveLeftAndRightUsePreviousInput", moveLeftAndRightUsePreviousInput)

		lineFallAnim = p.getProperty("$id.ruleopt.lineFallAnim", lineFallAnim)
		lineCancelMove = p.getProperty("$id.ruleopt.lineCancelMove", lineCancelMove)
		lineCancelRotate = p.getProperty("$id.ruleopt.lineCancelRotate", lineCancelRotate)
		lineCancelHold = p.getProperty("$id.ruleopt.lineCancelHold", lineCancelHold)

		skin = p.getProperty("$id.ruleopt.skin", skin)
		ghost = p.getProperty("$id.ruleopt.ghost", ghost)
	}

	companion object {
		/** Serial version ID */
		private const val serialVersionUID = 5781310758989780350L

		/** 横移動 counterかrotation counterが超過したら固定 timeリセットを無効にする */
		const val LOCKRESET_LIMIT_OVER_NORESET = 0

		/** 横移動 counterかrotation counterが超過したら即座に固定する */
		const val LOCKRESET_LIMIT_OVER_INSTANT = 1

		/** 横移動 counterかrotation counterが超過したらWallkick無効にする */
		const val LOCKRESET_LIMIT_OVER_NOWALLKICK = 2

		/** Blockピースのcolorパターン */
		enum class PieceColor(val array:IntArray) {
			ARS(intArrayOf(1, 2, 3, 4, 5, 6, 7, 5, 4, 0, 0)), SRS(intArrayOf(5, 2, 3, 1, 7, 6, 4, 1, 4, 0, 0));
		}

		const val PIECECOLOR_ARS = 0
		const val PIECECOLOR_SRS = 1
		val PIECECOLOR_PRESET = arrayOf(PieceColor.ARS.array, PieceColor.SRS.array)
		/** Blockピースのrotationパターン */
		const val PIECEOFFSET_NONE = 0
		const val PIECEOFFSET_BOTTOM = 1
		const val PIECEOFFSET_BIASED = 2
		const val PIECEOFFSET_ASSIGN = 3
		val PIECEOFFSET_NAME = arrayOf("SRS CENTER", "BOTTOM Aligned", "ARS BIASED", "Customized")
		val PIECEOFFSET_ARSPRESET = arrayOf(//[x/y][piece][direction]
			arrayOf(//x
				intArrayOf(0, 0, 0, 1), intArrayOf(0, 0, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(0, 0, 0, 1), intArrayOf(0, 0, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(0, -1, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(0, 0, 0, 0)), arrayOf(//y
			intArrayOf(0, 0, -1, 0), intArrayOf(1, 0, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(1, 0, 0, 0), intArrayOf(1, 0, 0, 0), intArrayOf(1, 0, 0, 0), intArrayOf(1, 0, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(0, 0, 0, 0)))

		val PIECEDIRECTION_ARSPRESET = intArrayOf(0, 2, 0, 0, 2, 2, 0, 0, 0, 0, 0)
	}
}
/** プロパティセットから読み込み
 * @param p プロパティセット
 * @param id Player IDまたはPresetID
 */
