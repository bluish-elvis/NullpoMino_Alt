/*
 * Copyright (c) 2010-2022, NullNoname
 * Kotlin converted and modified by Venom=Nhelv.
 * THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
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
package mu.nu.nullpo.game.component

import mu.nu.nullpo.util.CustomProperties
import java.io.Serializable

/** ゲームルールの設定 data */
class RuleOptions:Serializable {

	/** このルールのName */
	var strRuleName = ""

	/** 使用するWallkickアルゴリズムのクラス名 (空文字列ならWallkickしない) */
	var strWallkick = ""

	/** 使用する出現順補正アルゴリズムのクラス名 (空文字列なら完全ランダム) */
	var strRandomizer = ""

	/** Game Style */
	var style = 0

	var pieceOffset = 0
	/** Blockピースの回転パターンのcoordinate補正 (11ピース×4Direction) */
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
	var pieceColor = IntArray(Piece.PIECE_COUNT)

	/** Blockピースの初期Direction */
	var pieceDefaultDirection = IntArray(Piece.PIECE_COUNT)

	/** fieldより上から出現 */
	var pieceEnterAboveField = false

	/** 出現予定地が埋まっているときにY-coordinateを上にずらすMaximum count */
	var pieceEnterMaxDistanceY = 0

	/** fieldの幅 */
	var fieldWidth = 0

	/** Field height */
	var fieldHeight = 0

	/** fieldより上の見えない部分の高さ */
	var fieldHiddenHeight = 0

	/** fieldの天井の有無 */
	var fieldCeiling = false

	/** field枠内に置けなかったら死ぬかどうか */
	var fieldLockoutDeath = false

	/** field枠外に１マスでもはみ出しただけで死ぬかどうか
	 * falseだと１マスでも枠内ならばセーフ */
	var fieldPartialLockoutDeath = false

	/** NEXTのcount */
	var nextDisplay = 0

	/** ホールド使用可否 */
	var holdEnable = false

	/** 先行ホールド */
	var holdInitial = false

	/** 先行ホールド連続使用不可 */
	var holdInitialLimit = false

	/** ホールドを使ったときにBlockピースの向きを初期状態に戻す */
	var holdResetDirection = false

	/** ゲーム毎にホールドできる総数 (-1:無制限) */
	var holdLimit = 0

	/** Hard drop使用可否 */
	var harddropEnable = false

	/** Hard drop即固定 */
	var harddropLock = false

	/** Hard drop連続使用不可 */
	var harddropLimit = false

	/** Soft drop使用可否 */
	var softdropEnable = false

	/** Soft drop即固定 */
	var softdropLock = false

	/** Soft drop連続使用不可 */
	var softdropLimit = false

	/** 接地状態でSoft dropすると即固定 (falseだと20Gのみ即固定) */
	var softdropSurfaceLock = false

	/** Soft drop速度 (1f=1G, .5f=0.5G) */
	var softdropSpeed = 0f

	/** Soft drop速度を通常速度×n倍にする */
	var softdropMultiplyNativeSpeed = false

	/** Soft drop速度を通常速度に影響させない */
	var softdropGravitySpeedLimit = false

	/** 先行回転 */
	var spinInitial = false
	/** 先行回転連続使用不可 */
	var spinInitialLimit = false
	/** Wallkick */
	var spinWallkick = false
	/** 先行回転でもWallkickする */
	var spinInitialWallkick = false
	/** 上DirectionへのWallkickができる count (-1:無限) */
	var spinWallkickMaxRise = 0

	/** TrueにするとA,Cボタンを右回転にする */
	var spinToRight = false
	/** Bボタンでの回転を逆方向にする (falseならA,Cボタンと同じ) */
	var spinReverseKey = false
	/** Eボタンを180 spinにする (falseならA,Cボタンと同じ) */
	var spinDoubleKey = false

	/** 落下で固定猶予リセット */
	var lockResetFall = false
	/** 移動で固定猶予リセット */
	var lockResetMove = false

	/** 回転で固定猶予リセット */
	var lockResetSpin = false

	/** 壁蹴りで固定猶予リセット */
	var lockResetWallkick = false

	/** 横移動による固定猶予リセットの回数制限 (-1:無限) */
	var lockResetMoveLimit = 0

	/** 回転による固定猶予リセットの回数制限 (-1:無限) */
	var lockResetSpinLimit = 0

	/** trueにすると回転の回数制限を横移動と共有する (true: lockResetMoveLimitのみ使用) */
	var lockResetLimitShareCount = false

	/** 固定猶予リセットの回数を使い切った場合の処理
	 * LOCKRESET_LIMIT_OVER_NORESET = 0 : 固定猶予をリセットしないようにする
	 * LOCKRESET_LIMIT_OVER_INSTANT = 1 : 即固定する
	 * LOCKRESET_LIMIT_OVER_NOWALLKICK = 2 : Wallkickしないようにする */
	var lockResetLimitOver = 0

	/** 固定した瞬間光る frame count */
	var lockFlash = 0

	/** Blockが光る専用 frame を入れる */
	var lockFlashOnlyFrame = false

	/** Line clear前にBlockが光る frame を入れる */
	var lockFlashBeforeLineClear = false

	/** ARE cancel on move */
	var areCancelMove = false

	/** ARE cancel on spin */
	var areCancelSpin = false

	/** ARE cancel on hold */
	var areCancelHold = false

	/** 最小/MaximumARE (-1:指定なし) */
	var minARE = 0
	var maxARE = 0

	/** 最小/MaximumARE after line clear (-1:指定なし) */
	var minARELine = 0
	var maxARELine = 0

	/** 最小/MaximumLine clear time (-1:指定なし) */
	var minLineDelay = 0
	var maxLineDelay = 0

	/** 最小/Maximum固定 time (-1:指定なし) */
	var minLockDelay = 0
	var maxLockDelay = 0

	/** 最小/Maximum横溜め time (-1:指定なし) */
	var minDAS = 0
	var maxDAS = 0

	/** 横移動間隔 */
	var dasARR = 0

	var shiftLockEnable = false

	/** Ready画面で横溜め可能 */
	var dasInReady = false

	/** 最初の frame で横溜め可能 */
	var dasInMoveFirstFrame = false

	/** Blockが光った瞬間に横溜め可能 */
	var dasInLockFlash = false

	/** Line clear中に横溜め可能 */
	var dasInLineClear = false

	/** ARE中に横溜め可能 */
	var dasInARE = false

	/** AREの最後の frame で横溜め可能 */
	var dasInARELastFrame = false

	/** Ending突入画面で横溜め可能 */
	var dasInEndingStart = false

	/** Charge DAS on blocked move */
	var dasChargeOnBlockedMove = false

	/** Leave DAS charge alone when left/right are not held -- useful with
	 * dasRedirectInDelay
	 */
	var dasStoreChargeOnNeutral = false

	/** Allow direction changes during ARE delay without zeroing DAS charge */
	var dasRedirectInDelay = false

	/** 最初の frame で移動可能 */
	var moveFirstFrame = false

	/** 斜め移動 */
	var moveDiagonal = false

	/** 上下同時押し許可 */
	var moveUpAndDown = false

	/** 左右同時押し許可 */
	var moveLeftAndRightAllow = false

	/** 左右同時押ししたときに前の frame の input Directionを優先する (左を押しながら右を押すと右を無視して左を優先) */
	var moveLeftAndRightUsePreviousInput = false

	/** Line clear後に上のBlockが1段ずつ落ちるアニメーションを表示 */
	var lineFallAnim = false

	/** Line delay cancel on move */
	var lineCancelMove = false

	/** Line delay cancel on spin */
	var lineCancelSpin = false

	/** Line delay cancel on hold */
	var lineCancelHold = false

	/** Blockの絵柄 */
	var skin = 0

	/** ghost の有無 (falseならMode 側でghost を is enabledにしていても非表示) */
	var ghost = false

	/** Constructor */
	constructor() {
		reset()
	}

	/** Copy constructor
	 * @param r Copy source
	 */
	constructor(r:RuleOptions?) {
		replaace(r)
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
		pieceColor[Piece.PIECE_I] = Block.COLOR_WHITE
		pieceColor[Piece.PIECE_L] = Block.COLOR_WHITE
		pieceColor[Piece.PIECE_O] = Block.COLOR_WHITE
		pieceColor[Piece.PIECE_Z] = Block.COLOR_WHITE
		pieceColor[Piece.PIECE_T] = Block.COLOR_WHITE
		pieceColor[Piece.PIECE_J] = Block.COLOR_WHITE
		pieceColor[Piece.PIECE_S] = Block.COLOR_WHITE
		pieceColor[Piece.PIECE_I1] = Block.COLOR_PURPLE
		pieceColor[Piece.PIECE_I2] = Block.COLOR_BLUE
		pieceColor[Piece.PIECE_I3] = Block.COLOR_GREEN
		pieceColor[Piece.PIECE_L3] = Block.COLOR_ORANGE

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

		spinInitial = true
		spinInitialLimit = false
		spinWallkick = true
		spinInitialWallkick = true
		spinWallkickMaxRise = -1
		spinToRight = true
		spinReverseKey = true
		spinDoubleKey = true

		lockResetFall = true
		lockResetMove = true
		lockResetSpin = true
		lockResetWallkick = false
		lockResetMoveLimit = 15
		lockResetSpinLimit = 15
		lockResetLimitShareCount = true
		lockResetLimitOver = LOCKRESET_LIMIT_OVER_INSTANT

		lockFlash = 2
		lockFlashOnlyFrame = true
		lockFlashBeforeLineClear = false
		areCancelMove = false
		areCancelSpin = false
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

		dasARR = 0

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
		lineCancelSpin = false
		lineCancelHold = false

		skin = 0
		ghost = true
	}

	/** 設定を[r]からコピー */
	fun replaace(r:RuleOptions?) {
		r?.let {o ->
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

			spinInitial = o.spinInitial
			spinInitialLimit = o.spinInitialLimit
			spinWallkick = o.spinWallkick
			spinInitialWallkick = o.spinInitialWallkick
			spinWallkickMaxRise = o.spinWallkickMaxRise
			spinToRight = o.spinToRight
			spinReverseKey = o.spinReverseKey
			spinDoubleKey = o.spinDoubleKey

			lockResetFall = o.lockResetFall
			lockResetMove = o.lockResetMove
			lockResetSpin = o.lockResetSpin
			lockResetWallkick = o.lockResetWallkick
			lockResetMoveLimit = o.lockResetMoveLimit
			lockResetSpinLimit = o.lockResetSpinLimit
			lockResetLimitShareCount = o.lockResetLimitShareCount
			lockResetLimitOver = o.lockResetLimitOver

			lockFlash = o.lockFlash
			lockFlashOnlyFrame = o.lockFlashOnlyFrame
			lockFlashBeforeLineClear = o.lockFlashBeforeLineClear
			areCancelMove = o.areCancelMove
			areCancelSpin = o.areCancelSpin
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

			dasARR = o.dasARR

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
			lineCancelSpin = o.lineCancelSpin
			lineCancelHold = o.lineCancelHold

			skin = o.skin
			ghost = o.ghost
		} ?: reset()
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

		if(spinInitial!=r.spinInitial) return false
		if(spinInitialLimit!=r.spinInitialLimit) return false
		if(spinWallkick!=r.spinWallkick) return false
		if(spinInitialWallkick!=r.spinInitialWallkick) return false
		if(spinWallkickMaxRise!=r.spinWallkickMaxRise) return false
		if(spinToRight!=r.spinToRight) return false
		if(spinReverseKey!=r.spinReverseKey) return false
		if(spinDoubleKey!=r.spinDoubleKey) return false

		if(lockResetFall!=r.lockResetFall) return false
		if(lockResetMove!=r.lockResetMove) return false
		if(lockResetSpin!=r.lockResetSpin) return false
		if(lockResetWallkick!=r.lockResetWallkick) return false
		if(lockResetMoveLimit!=r.lockResetMoveLimit) return false
		if(lockResetSpinLimit!=r.lockResetSpinLimit) return false
		if(lockResetLimitShareCount!=r.lockResetLimitShareCount) return false
		if(lockResetLimitOver!=r.lockResetLimitOver) return false

		if(lockFlash!=r.lockFlash) return false
		if(lockFlashOnlyFrame!=r.lockFlashOnlyFrame) return false
		if(lockFlashBeforeLineClear!=r.lockFlashBeforeLineClear) return false
		if(areCancelMove!=r.areCancelMove) return false
		if(areCancelSpin!=r.areCancelSpin) return false
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

		if(dasARR!=r.dasARR) return false

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
		if(lineCancelSpin!=r.lineCancelSpin) return false
		if(lineCancelHold!=r.lineCancelHold) return false

		return if(ignoreGraphicsSetting&&skin!=r.skin) false else ghost==r.ghost
	}

	/** プロパティセットに保存
	 * @param p プロパティセット
	 * @param id Player IDまたはPresetID
	 */
	fun writeProperty(p:CustomProperties, id:Int) {
		p.setProperty("$id.ruleOpt.strRuleName", strRuleName)
		p.setProperty("$id.ruleOpt.strWallkick", strWallkick)
		p.setProperty("$id.ruleOpt.strRandomizer", strRandomizer)

		p.setProperty("$id.ruleOpt.style", style)
		p.setProperty("$id.ruleOpt.pieceOffset", pieceOffset)

		for(i in 0 until Piece.PIECE_COUNT) {
			if(pieceOffset==PIECEOFFSET_ASSIGN)
				for(j in 0 until Piece.DIRECTION_COUNT) {
					p.setProperty("$id.ruleOpt.pieceOffsetX.$i.$j", pieceOffsetX[i][j])
					p.setProperty("$id.ruleOpt.pieceOffsetY.$i.$j", pieceOffsetY[i][j])
					p.setProperty("$id.ruleOpt.pieceSpawnX.$i.$j", pieceSpawnX[i][j])
					p.setProperty("$id.ruleOpt.pieceSpawnY.$i.$j", pieceSpawnY[i][j])
					p.setProperty("$id.ruleOpt.pieceSpawnXBig.$i.$j", pieceSpawnXBig[i][j])
					p.setProperty("$id.ruleOpt.pieceSpawnYBig.$i.$j", pieceSpawnYBig[i][j])
				}
			p.setProperty("$id.ruleOpt.pieceColor.$i", pieceColor[i])
			p.setProperty("$id.ruleOpt.pieceDefaultDirection.$i", pieceDefaultDirection[i])
		}
		p.setProperty("$id.ruleOpt.pieceEnterAboveField", pieceEnterAboveField)
		p.setProperty("$id.ruleOpt.pieceEnterMaxDistanceY", pieceEnterMaxDistanceY)

		p.setProperty("$id.ruleOpt.fieldWidth", fieldWidth)
		p.setProperty("$id.ruleOpt.fieldHeight", fieldHeight)
		p.setProperty("$id.ruleOpt.fieldHiddenHeight", fieldHiddenHeight)
		p.setProperty("$id.ruleOpt.fieldCeiling", fieldCeiling)
		p.setProperty("$id.ruleOpt.fieldLockoutDeath", fieldLockoutDeath)
		p.setProperty("$id.ruleOpt.fieldPartialLockoutDeath", fieldPartialLockoutDeath)

		p.setProperty("$id.ruleOpt.nextDisplay", nextDisplay)

		p.setProperty("$id.ruleOpt.holdEnable", holdEnable)
		p.setProperty("$id.ruleOpt.holdInitial", holdInitial)
		p.setProperty("$id.ruleOpt.holdInitialLimit", holdInitialLimit)
		p.setProperty("$id.ruleOpt.holdResetDirection", holdResetDirection)
		p.setProperty("$id.ruleOpt.holdLimit", holdLimit)

		p.setProperty("$id.ruleOpt.harddropEnable", harddropEnable)
		p.setProperty("$id.ruleOpt.harddropLock", harddropLock)
		p.setProperty("$id.ruleOpt.harddropLimit", harddropLimit)

		p.setProperty("$id.ruleOpt.softdropEnable", softdropEnable)
		p.setProperty("$id.ruleOpt.softdropLock", softdropLock)
		p.setProperty("$id.ruleOpt.softdropLimit", softdropLimit)
		p.setProperty("$id.ruleOpt.softdropSurfaceLock", softdropSurfaceLock)
		p.setProperty("$id.ruleOpt.softdropSpeed", softdropSpeed)
		p.setProperty("$id.ruleOpt.softdropMultiplyNativeSpeed", softdropMultiplyNativeSpeed)
		p.setProperty("$id.ruleOpt.softdropGravitySpeedLimit", softdropGravitySpeedLimit)

		p.setProperty("$id.ruleOpt.rotateInitial", spinInitial)
		p.setProperty("$id.ruleOpt.rotateInitialLimit", spinInitialLimit)
		p.setProperty("$id.ruleOpt.rotateWallkick", spinWallkick)
		p.setProperty("$id.ruleOpt.rotateInitialWallkick", spinInitialWallkick)
		p.setProperty("$id.ruleOpt.rotateMaxUpwardWallkick", spinWallkickMaxRise)
		p.setProperty("$id.ruleOpt.rotateButtonDefaultRight", spinToRight)
		p.setProperty("$id.ruleOpt.rotateButtonAllowReverse", spinReverseKey)
		p.setProperty("$id.ruleOpt.rotateButtonAllowDouble", spinDoubleKey)

		p.setProperty("$id.ruleOpt.lockresetFall", lockResetFall)
		p.setProperty("$id.ruleOpt.lockresetMove", lockResetMove)
		p.setProperty("$id.ruleOpt.lockresetRotate", lockResetSpin)
		p.setProperty("$id.ruleOpt.lockresetWallkick", lockResetWallkick)
		p.setProperty("$id.ruleOpt.lockresetLimitMove", lockResetMoveLimit)
		p.setProperty("$id.ruleOpt.lockresetLimitRotate", lockResetSpinLimit)
		p.setProperty("$id.ruleOpt.lockresetLimitShareCount", lockResetLimitShareCount)
		p.setProperty("$id.ruleOpt.lockresetLimitOver", lockResetLimitOver)

		p.setProperty("$id.ruleOpt.lockflash", lockFlash)
		p.setProperty("$id.ruleOpt.lockflashOnlyFrame", lockFlashOnlyFrame)
		p.setProperty("$id.ruleOpt.lockflashBeforeLineClear", lockFlashBeforeLineClear)
		p.setProperty("$id.ruleOpt.areCancelMove", areCancelMove)
		p.setProperty("$id.ruleOpt.areCancelRotate", areCancelSpin)
		p.setProperty("$id.ruleOpt.areCancelHold", areCancelHold)

		p.setProperty("$id.ruleOpt.minARE", minARE)
		p.setProperty("$id.ruleOpt.maxARE", maxARE)
		p.setProperty("$id.ruleOpt.minARELine", minARELine)
		p.setProperty("$id.ruleOpt.maxARELine", maxARELine)
		p.setProperty("$id.ruleOpt.minLineDelay", minLineDelay)
		p.setProperty("$id.ruleOpt.maxLineDelay", maxLineDelay)
		p.setProperty("$id.ruleOpt.minLockDelay", minLockDelay)
		p.setProperty("$id.ruleOpt.maxLockDelay", maxLockDelay)
		p.setProperty("$id.ruleOpt.minDAS", minDAS)
		p.setProperty("$id.ruleOpt.maxDAS", maxDAS)

		p.setProperty("$id.ruleOpt.dasDelay", dasARR)

		p.setProperty("$id.ruleOpt.shiftLockEnable", shiftLockEnable)

		p.setProperty("$id.ruleOpt.dasInReady", dasInReady)
		p.setProperty("$id.ruleOpt.dasInMoveFirstFrame", dasInMoveFirstFrame)
		p.setProperty("$id.ruleOpt.dasInLockFlash", dasInLockFlash)
		p.setProperty("$id.ruleOpt.dasInLineClear", dasInLineClear)
		p.setProperty("$id.ruleOpt.dasInARE", dasInARE)
		p.setProperty("$id.ruleOpt.dasInARELastFrame", dasInARELastFrame)
		p.setProperty("$id.ruleOpt.dasInEndingStart", dasInEndingStart)
		p.setProperty("$id.ruleOpt.dasOnBlockedMove", dasChargeOnBlockedMove)
		p.setProperty("$id.ruleOpt.dasStoreChargeOnNeutral", dasStoreChargeOnNeutral)
		p.setProperty("$id.ruleOpt.dasRedirectInDelay", dasRedirectInDelay)

		p.setProperty("$id.ruleOpt.moveFirstFrame", moveFirstFrame)
		p.setProperty("$id.ruleOpt.moveDiagonal", moveDiagonal)
		p.setProperty("$id.ruleOpt.moveUpAndDown", moveUpAndDown)
		p.setProperty("$id.ruleOpt.moveLeftAndRightAllow", moveLeftAndRightAllow)
		p.setProperty("$id.ruleOpt.moveLeftAndRightUsePreviousInput", moveLeftAndRightUsePreviousInput)

		p.setProperty("$id.ruleOpt.lineFallAnim", lineFallAnim)
		p.setProperty("$id.ruleOpt.lineCancelMove", lineCancelMove)
		p.setProperty("$id.ruleOpt.lineCancelRotate", lineCancelSpin)
		p.setProperty("$id.ruleOpt.lineCancelHold", lineCancelHold)

		p.setProperty("$id.ruleOpt.skin", skin)
		p.setProperty("$id.ruleOpt.ghost", ghost)
	}

	/** プロパティセットから読み込み
	 * @param p プロパティセット
	 * @param id Player IDまたはPresetID
	 */
	@JvmOverloads
	fun readProperty(p:CustomProperties, id:Int, offset:Boolean = false) {
		strRuleName = p.getProperty("$id.ruleOpt.strRuleName", strRuleName)
		strWallkick = p.getProperty("$id.ruleOpt.strWallkick", strWallkick)
		strRandomizer = p.getProperty("$id.ruleOpt.strRandomizer", strRandomizer)

		style = p.getProperty("$id.ruleOpt.style", 0)
		pieceOffset = p.getProperty("$id.ruleOpt.pieceOffset", PIECEOFFSET_NONE)
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
						pieceOffsetX[i][j] = p.getProperty("$id.ruleOpt.pieceOffsetX.$i.$j", pieceOffsetX[i][j])
						pieceOffsetY[i][j] = p.getProperty("$id.ruleOpt.pieceOffsetY.$i.$j", pieceOffsetY[i][j])
						pieceSpawnX[i][j] = p.getProperty("$id.ruleOpt.pieceSpawnX.$i.$j", pieceSpawnX[i][j])
						pieceSpawnY[i][j] = p.getProperty("$id.ruleOpt.pieceSpawnY.$i.$j", pieceSpawnY[i][j])
						pieceSpawnXBig[i][j] = p.getProperty("$id.ruleOpt.pieceSpawnXBig.$i.$j", pieceSpawnXBig[i][j])
						pieceSpawnYBig[i][j] = p.getProperty("$id.ruleOpt.pieceSpawnYBig.$i.$j", pieceSpawnYBig[i][j])
					}
				}

			pieceColor[i] = p.getProperty("$id.ruleOpt.pieceColor.$i", pieceColor[i])
			pieceDefaultDirection[i] = p.getProperty("$id.ruleOpt.pieceDefaultDirection.$i", pieceDefaultDirection[i])
		}
		pieceEnterAboveField = p.getProperty("$id.ruleOpt.pieceEnterAboveField", pieceEnterAboveField)
		pieceEnterMaxDistanceY = p.getProperty("$id.ruleOpt.pieceEnterMaxDistanceY", pieceEnterMaxDistanceY)

		fieldWidth = p.getProperty("$id.ruleOpt.fieldWidth", fieldWidth)
		fieldHeight = p.getProperty("$id.ruleOpt.fieldHeight", fieldHeight)
		fieldHiddenHeight = p.getProperty("$id.ruleOpt.fieldHiddenHeight", fieldHiddenHeight)
		fieldCeiling = p.getProperty("$id.ruleOpt.fieldCeiling", fieldCeiling)
		fieldLockoutDeath = p.getProperty("$id.ruleOpt.fieldLockoutDeath", fieldLockoutDeath)
		fieldPartialLockoutDeath = p.getProperty("$id.ruleOpt.fieldPartialLockoutDeath", fieldPartialLockoutDeath)

		nextDisplay = p.getProperty("$id.ruleOpt.nextDisplay", nextDisplay)

		holdEnable = p.getProperty("$id.ruleOpt.holdEnable", holdEnable)
		holdInitial = p.getProperty("$id.ruleOpt.holdInitial", holdInitial)
		holdInitialLimit = p.getProperty("$id.ruleOpt.holdInitialLimit", holdInitialLimit)
		holdResetDirection = p.getProperty("$id.ruleOpt.holdResetDirection", holdResetDirection)
		holdLimit = p.getProperty("$id.ruleOpt.holdLimit", holdLimit)

		harddropEnable = p.getProperty("$id.ruleOpt.harddropEnable", harddropEnable)
		harddropLock = p.getProperty("$id.ruleOpt.harddropLock", harddropLock)
		harddropLimit = p.getProperty("$id.ruleOpt.harddropLimit", harddropLimit)

		softdropEnable = p.getProperty("$id.ruleOpt.softdropEnable", softdropEnable)
		softdropLock = p.getProperty("$id.ruleOpt.softdropLock", softdropLock)
		softdropLimit = p.getProperty("$id.ruleOpt.softdropLimit", softdropLimit)
		softdropSurfaceLock = p.getProperty("$id.ruleOpt.softdropSurfaceLock", softdropSurfaceLock)
		softdropSpeed = p.getProperty("$id.ruleOpt.softdropSpeed", softdropSpeed)
		softdropMultiplyNativeSpeed = p.getProperty("$id.ruleOpt.softdropMultiplyNativeSpeed", softdropMultiplyNativeSpeed)
		softdropGravitySpeedLimit = p.getProperty("$id.ruleOpt.softdropGravitySpeedLimit", softdropGravitySpeedLimit)

		spinInitial = p.getProperty("$id.ruleOpt.rotateInitial", spinInitial)
		spinInitialLimit = p.getProperty("$id.ruleOpt.rotateInitialLimit", spinInitialLimit)
		spinWallkick = p.getProperty("$id.ruleOpt.rotateWallkick", spinWallkick)
		spinInitialWallkick = p.getProperty("$id.ruleOpt.rotateInitialWallkick", spinInitialWallkick)
		spinWallkickMaxRise = p.getProperty("$id.ruleOpt.rotateMaxUpwardWallkick", spinWallkickMaxRise)
		spinToRight = p.getProperty("$id.ruleOpt.rotateButtonDefaultRight", spinToRight)
		spinReverseKey = p.getProperty("$id.ruleOpt.rotateButtonAllowReverse", spinReverseKey)
		spinDoubleKey = p.getProperty("$id.ruleOpt.rotateButtonAllowDouble", spinDoubleKey)

		lockResetFall = p.getProperty("$id.ruleOpt.lockresetFall", lockResetFall)
		lockResetMove = p.getProperty("$id.ruleOpt.lockresetMove", lockResetMove)
		lockResetSpin = p.getProperty("$id.ruleOpt.lockresetRotate", lockResetSpin)
		lockResetWallkick = p.getProperty("$id.ruleOpt.lockresetWallkick", lockResetWallkick)
		lockResetMoveLimit = p.getProperty("$id.ruleOpt.lockresetLimitMove", lockResetMoveLimit)
		lockResetSpinLimit = p.getProperty("$id.ruleOpt.lockresetLimitRotate", lockResetSpinLimit)
		lockResetLimitShareCount = p.getProperty("$id.ruleOpt.lockresetLimitShareCount", lockResetLimitShareCount)
		lockResetLimitOver = p.getProperty("$id.ruleOpt.lockresetLimitOver", lockResetLimitOver)

		lockFlash = p.getProperty("$id.ruleOpt.lockflash", lockFlash)
		lockFlashOnlyFrame = p.getProperty("$id.ruleOpt.lockflashOnlyFrame", lockFlashOnlyFrame)
		lockFlashBeforeLineClear = p.getProperty("$id.ruleOpt.lockflashBeforeLineClear", lockFlashBeforeLineClear)
		areCancelMove = p.getProperty("$id.ruleOpt.areCancelMove", areCancelMove)
		areCancelSpin = p.getProperty("$id.ruleOpt.areCancelRotate", areCancelSpin)
		areCancelHold = p.getProperty("$id.ruleOpt.areCancelHold", areCancelHold)

		minARE = p.getProperty("$id.ruleOpt.minARE", minARE)
		maxARE = p.getProperty("$id.ruleOpt.maxARE", maxARE)
		minARELine = p.getProperty("$id.ruleOpt.minARELine", minARELine)
		maxARELine = p.getProperty("$id.ruleOpt.maxARELine", maxARELine)
		minLineDelay = p.getProperty("$id.ruleOpt.minLineDelay", minLineDelay)
		maxLineDelay = p.getProperty("$id.ruleOpt.maxLineDelay", maxLineDelay)
		minLockDelay = p.getProperty("$id.ruleOpt.minLockDelay", minLockDelay)
		maxLockDelay = p.getProperty("$id.ruleOpt.maxLockDelay", maxLockDelay)
		minDAS = p.getProperty("$id.ruleOpt.minDAS", minDAS)
		maxDAS = p.getProperty("$id.ruleOpt.maxDAS", maxDAS)

		dasARR = p.getProperty("$id.ruleOpt.dasDelay", dasARR)
		shiftLockEnable = p.getProperty("$id.ruleOpt.shiftLockEnable", shiftLockEnable)

		dasInReady = p.getProperty("$id.ruleOpt.dasInReady", dasInReady)
		dasInMoveFirstFrame = p.getProperty("$id.ruleOpt.dasInMoveFirstFrame", dasInMoveFirstFrame)
		dasInLockFlash = p.getProperty("$id.ruleOpt.dasInLockFlash", dasInLockFlash)
		dasInLineClear = p.getProperty("$id.ruleOpt.dasInLineClear", dasInLineClear)
		dasInARE = p.getProperty("$id.ruleOpt.dasInARE", dasInARE)
		dasInARELastFrame = p.getProperty("$id.ruleOpt.dasInARELastFrame", dasInARELastFrame)
		dasInEndingStart = p.getProperty("$id.ruleOpt.dasInEndingStart", dasInEndingStart)
		dasChargeOnBlockedMove = p.getProperty("$id.ruleOpt.dasOnBlockedMove", dasChargeOnBlockedMove)
		dasStoreChargeOnNeutral = p.getProperty("$id.ruleOpt.dasStoreChargeOnNeutral", dasStoreChargeOnNeutral)
		dasRedirectInDelay = p.getProperty("$id.ruleOpt.dasRedirectInDelay", dasRedirectInDelay)

		moveFirstFrame = p.getProperty("$id.ruleOpt.moveFirstFrame", moveFirstFrame)
		moveDiagonal = p.getProperty("$id.ruleOpt.moveDiagonal", moveDiagonal)
		moveUpAndDown = p.getProperty("$id.ruleOpt.moveUpAndDown", moveUpAndDown)
		moveLeftAndRightAllow = p.getProperty("$id.ruleOpt.moveLeftAndRightAllow", moveLeftAndRightAllow)
		moveLeftAndRightUsePreviousInput =
			p.getProperty("$id.ruleOpt.moveLeftAndRightUsePreviousInput", moveLeftAndRightUsePreviousInput)

		lineFallAnim = p.getProperty("$id.ruleOpt.lineFallAnim", lineFallAnim)
		lineCancelMove = p.getProperty("$id.ruleOpt.lineCancelMove", lineCancelMove)
		lineCancelSpin = p.getProperty("$id.ruleOpt.lineCancelRotate", lineCancelSpin)
		lineCancelHold = p.getProperty("$id.ruleOpt.lineCancelHold", lineCancelHold)

		skin = p.getProperty("$id.ruleOpt.skin", skin)
		ghost = p.getProperty("$id.ruleOpt.ghost", ghost)
	}

	companion object {
		/** Serial version ID */
		private const val serialVersionUID = 5781310758989780350L

		/** 横移動 counterかspin counterが超過したら固定 timeリセットを無効にする */
		const val LOCKRESET_LIMIT_OVER_NORESET = 0

		/** 横移動 counterかspin counterが超過したら即座に固定する */
		const val LOCKRESET_LIMIT_OVER_INSTANT = 1

		/** 横移動 counterかspin counterが超過したらWallkick無効にする */
		const val LOCKRESET_LIMIT_OVER_NOWALLKICK = 2

		/** Blockピースのcolorパターン */
		enum class PieceColor(val array:List<Int>) {
			ARS(listOf(1, 2, 3, 4, 5, 6, 7, 5, 4, 0, 0)), SRS(listOf(5, 2, 3, 1, 7, 6, 4, 1, 4, 0, 0));
		}

		const val PIECECOLOR_ARS = 0
		const val PIECECOLOR_SRS = 1
		val PIECECOLOR_PRESET = listOf(PieceColor.ARS.array, PieceColor.SRS.array)
		/** Blockピースのspinパターン */
		const val PIECEOFFSET_NONE = 0
		const val PIECEOFFSET_BOTTOM = 1
		const val PIECEOFFSET_BIASED = 2
		const val PIECEOFFSET_ASSIGN = 3
		val PIECEOFFSET_NAME = listOf("SRS CENTER", "BOTTOM Aligned", "ARS BIASED", "Customized")
		val PIECEOFFSET_ARSPRESET = listOf(//[x/y][piece][direction]
			listOf(//x
				listOf(0, 0, 0, 1),
				listOf(0, 0, 0, 0),
				listOf(0, 0, 0, 0),
				listOf(0, 0, 0, 1),
				listOf(0, 0, 0, 0),
				listOf(0, 0, 0, 0),
				listOf(0, -1, 0, 0),
				listOf(0, 0, 0, 0),
				listOf(0, 0, 0, 0),
				listOf(0, 0, 0, 0),
				listOf(0, 0, 0, 0)
			), listOf(//y
				listOf(0, 0, -1, 0),
				listOf(1, 0, 0, 0),
				listOf(0, 0, 0, 0),
				listOf(1, 0, 0, 0),
				listOf(1, 0, 0, 0),
				listOf(1, 0, 0, 0),
				listOf(1, 0, 0, 0),
				listOf(0, 0, 0, 0),
				listOf(0, 0, 0, 0),
				listOf(0, 0, 0, 0),
				listOf(0, 0, 0, 0)
			)
		)

		val PIECEDIRECTION_ARSPRESET = listOf(0, 2, 0, 0, 2, 2, 0, 0, 0, 0, 0)
	}
}
