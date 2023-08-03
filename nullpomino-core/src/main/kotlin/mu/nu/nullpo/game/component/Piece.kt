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

import mu.nu.nullpo.game.component.Piece.Companion.DIRECTION_COUNT
import java.io.Serializable
import kotlin.math.roundToInt

/** Blockピース
 * @param id BlockピースのID */
class Piece(id:Int = 0):Serializable {
	/** ID */
	var id:Int = id
		set(value) {
			field = value
			direction = DIRECTION_UP
			big = false
			offsetApplied = false
			connectBlocks = true
			block = List(maxBlock) {Block()}

			resetOffsetArray()
		}
	val type:Shape get() = Shape.all[id]

	/** Direction */
	var direction = DIRECTION_UP

	/** BigBlock */
	@JvmField var big = false

	/** Connect blocks in this piece? */
	var connectBlocks = true

	/** 相対X位置 (4Direction×nBlock) */
	var dataX = List(DIRECTION_COUNT) {MutableList(maxBlock) {0}}
		private set
	/** 相対Y位置 (4Direction×nBlock) */
	var dataY = List(DIRECTION_COUNT) {MutableList(maxBlock) {0}}
		private set
	/** ピースを構成するBlock (nBlock) */
	var block = List(maxBlock) {Block()}

	/** 相対X位置と相対Y位置がオリジナル stateからずらされているならtrue */
	var offsetApplied = false
	/** 相対X位置のずれ幅 */
	var dataOffsetX = MutableList(DIRECTION_COUNT) {0}
		private set
	/** 相対Y位置のずれ幅 */
	var dataOffsetY = MutableList(DIRECTION_COUNT) {0}
		private set

	/*dataX = List(DIRECTION_COUNT) {IntArray(maxBlock)}
		dataY = List(DIRECTION_COUNT) {IntArray(maxBlock)}
		block = List(maxBlock) {Block()}
		dataOffsetX = IntArray(DIRECTION_COUNT)
		dataOffsetY = IntArray(DIRECTION_COUNT)*/
	/** 1つのピースに含まれるBlockのcountを取得
	 * @return 1つのピースに含まれるBlockのcount
	 */
	val maxBlock get() = DEFAULT_PIECE_DATA_X[id][direction].size

	/** Fetches the colors of the blocks in the piece
	 * @return An int array containing the cint of each block
	 */
	val colors get() = List(block.size) {block[it].cint}

	/** ゲームが始まってから何番目に置かれるBlockか (負countだったら初期配置やgarbage block) */
	var placeNum = -1
		set(value) {
		block.forEach {it.placeNum = value}
		field = value
	}

	/** @return ピース回転軸のX-coordinate */
	val spinCX get() = dataX.flatten().let {(it.maxOrNull() ?: 0)-(it.minOrNull() ?: 0)}/2f
	/** @return ピース回転軸のY-coordinate */
	val spinCY get() = dataY.flatten().let {(it.maxOrNull() ?: 0)-(it.minOrNull() ?: 0)}/2f
	/** @return ピースの幅*/
	val width get() = maximumBlockX-minimumBlockX
	/** @return ピースの高さ*/
	val height get() = maximumBlockY-minimumBlockY
	val centerX get() = (width/2.0).roundToInt()
	val centerY get() = (height/2.0).roundToInt()
	/** @return テトラミノの最も高いBlockのX-coordinate*/
	val minimumBlockX get() = (dataX[direction].minOrNull() ?: 0)*if(big) 2 else 1

	/** @return テトラミノの最も低いBlockのX-coordinate */
	val maximumBlockX get() = (dataX[direction].maxOrNull() ?: 0)*if(big) 2 else 1

	/** @return テトラミノの最も高いBlockのY-coordinate */
	val minimumBlockY get() = (dataY[direction].minOrNull() ?: 0)*if(big) 2 else 1

	/** @return テトラミノの最も低いBlockのY-coordinate */
	val maximumBlockY get() = (dataY[direction].maxOrNull() ?: 0)*if(big) 2 else 1

	init {
		resetOffsetArray()
	}

	/** Copy constructor from [p]
	 */
	constructor(p:Piece):this(p.id) {
		replace(p)
		updateConnectData()
	}

	/** Blockピースの dataを[p]からコピー
	 * @param keep idを保持
	 */
	fun replace(p:Piece, keep:Boolean = false) {
		if(keep) id = p.id
		direction = p.direction
		big = p.big
		offsetApplied = p.offsetApplied
		connectBlocks = p.connectBlocks
		block = List(p.maxBlock) {Block(p.block[it])}

		dataX = p.dataX.map {it.toMutableList()}
		dataY = p.dataY.map {it.toMutableList()}
		block = p.block.toList()
		dataOffsetX = p.dataOffsetX.toMutableList()
		dataOffsetY = p.dataOffsetY.toMutableList()
	}

	/** すべてのBlock stateを[block]と同じに設定
	 */
	fun setBlock(block:Block) = this.block.forEach {it.replace(block)}

	/** すべてのBlock colorを[color]に変更*/
	fun setColor(color:Block.COLOR) = block.forEach {it.color = color}

	/** すべてのBlock colorを[color]に変更*/
	fun setColor(color:Int) = block.forEach {it.cint = color}

	/** Changes the colors of the blocks individually; allows one piece to have
	 * blocks of multiple colors
	 * @param color Array with each cell specifying a cint of a block
	 */
	fun setColor(color:List<*>) {
		when(color.firstOrNull()) {
			is Block.COLOR -> block.forEachIndexed {i, it -> it.color = color[i%color.size] as Block.COLOR}
			is Int -> block.forEachIndexed {i, it -> it.cint = color[i%color.size] as Int}
		}
	}

	/** Changes the colors of the blocks individually; allows one piece to have
	 * blocks of multiple colors
	 * @param color Array with each cell specifying a cint of a block
	 */
	fun setColor(color:IntArray) = block.forEachIndexed {i, it -> it.cint = color[i%color.size]}

	/** Sets all blocks to an inum block
	 * @param item ID number of the inum
	 */
	fun setItem(item:Int) = block.forEach {it.iNum = item}

	/** Sets the items of the blocks individually; allows one piece to have
	 * different inum settings for each block
	 * @param item Array with each element specifying a cint of a block
	 */
	fun setItem(item:List<Int>) = block.forEachIndexed {i, it -> it.iNum = item[i%item.size]}

	/** Sets all blocks' hard count to [hard] */
	fun setHard(hard:Int) = block.forEach {it.hard = hard}

	/** Sets the hard counts of the blocks individually; allows one piece to
	 * have
	 * different hard count settings for each block
	 * @param hard Array with each element specifying a hard count of a block
	 */
	fun setHard(hard:List<Int>) = block.forEachIndexed {i, it -> it.hard = hard[i%hard.size]}

	/** すべてのBlockの模様を変更
	 * @param skin 模様
	 */
	fun setSkin(skin:Int) = block.forEach {it.skin = skin}

	/** すべてのBlockの経過 frame を変更
	 * @param elapsedFrames 固定してから経過した frame count
	 */
	fun setElapsedFrames(elapsedFrames:Int) = block.forEach {it.elapsedFrames = elapsedFrames}

	/** すべてのBlockの暗さまたは明るさを変更
	 * @param darkness 暗さまたは明るさ (0.03だったら3%暗く, -0.05だったら5%明るい）
	 */
	fun setDarkness(darkness:Float) = block.forEach {it.darkness = darkness}

	/** すべてのBlockの透明度を変更
	 * @param alpha 透明度 (1fで不透明, 0fで完全に透明）
	 */
	fun setAlpha(alpha:Float) = block.forEach {it.alpha = alpha}

	/** すべてのBlockの属性を設定
	 * @param attrs 変更したい属性
	 * @param status 変更後 state
	 */
	fun setAttribute(status:Boolean, vararg attrs:Block.ATTRIBUTE) = block.forEach {
		it.setAttribute(status, *attrs)
	}

	/** 相対X位置と相対Y位置をずらす
	 * @param offsetX X位置補正量の配列 (int[DIRECTION_COUNT]）
	 * @param offsetY Y位置補正量の配列 (int[DIRECTION_COUNT]）
	 */
	fun applyOffsetArray(offsetX:MutableList<Int>, offsetY:MutableList<Int>) {
		applyOffsetArrayX(offsetX)
		applyOffsetArrayY(offsetY)
	}

	/** 相対X位置をずらす
	 * @param offsetX X位置補正量の配列 (int[DIRECTION_COUNT]）
	 */
	fun applyOffsetArrayX(offsetX:MutableList<Int>) {
		offsetApplied = true

		for(i in 0..<DIRECTION_COUNT) {
			for(j in 0..<maxBlock)
				dataX[i][j] += offsetX[i]
			dataOffsetX[i] = offsetX[i]
		}
	}

	/** 相対Y位置をずらす
	 * @param offsetY Y位置補正量の配列 (int[DIRECTION_COUNT]）
	 */
	fun applyOffsetArrayY(offsetY:MutableList<Int>) {
		offsetApplied = true

		for(i in 0..<DIRECTION_COUNT) {
			for(j in 0..<maxBlock)
				dataY[i][j] += offsetY[i]
			dataOffsetY[i] = offsetY[i]
		}
	}

	/** 相対X位置と相対Y位置を初期状態に戻す */
	fun resetOffsetArray() {
		dataX = DEFAULT_PIECE_DATA_X[id].map {it.toMutableList()}
		dataY = DEFAULT_PIECE_DATA_Y[id].map {it.toMutableList()}
		dataOffsetX.fill(0)
		dataOffsetY.fill(0)
		offsetApplied = false
		updateConnectData()
	}

	/** Blockの繋がり dataを更新 */
	fun updateConnectData() {
		block.forEachIndexed {j, b ->

			b.setAttribute(
				false, Block.ATTRIBUTE.CONNECT_UP, Block.ATTRIBUTE.CONNECT_DOWN,
				Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_RIGHT
			)

			if(connectBlocks) {
				// 相対X位置と相対Y位置
				val bx = dataX[direction][j]
				val by = dataY[direction][j]
				b.setAttribute(false, Block.ATTRIBUTE.BROKEN)
				// 他の3つのBlockとの繋がりを調べる
				block.forEachIndexed {k, _ ->
					if(k!=j) {
						val bx2 = dataX[direction][k]
						val by2 = dataY[direction][k]

						if(bx==bx2&&by-1==by2) b.setAttribute(true, Block.ATTRIBUTE.CONNECT_UP) // Up
						if(bx==bx2&&by+1==by2) b.setAttribute(true, Block.ATTRIBUTE.CONNECT_DOWN) // Down
						if(by==by2&&bx-1==bx2) b.setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT) // 左
						if(by==by2&&bx+1==bx2) b.setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT) // 右
					}
				}
			} else b.setAttribute(true, Block.ATTRIBUTE.BROKEN)
		}
	}

	/** 1つ以上Blockがfield枠外に置かれるかどうか判定
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @return 1つ以上Blockがfield枠外に置かれるならtrue, そうでないならfalse
	 */
	fun isPartialLockOut(x:Int, y:Int, rt:Int = direction):Boolean {
		var placed = false
		// Bigでは専用処理
		if(big) for(i in 0..<maxBlock) {
			val y2 = y+dataY[rt][i]*2

			// 4Block分置く
			for(k in 0..1)
				for(l in 0..1) {
					val y3 = y2+l
					if(y3<0) placed = true
				}
		} else for(i in 0..<maxBlock) {
			val y2 = y+dataY[rt][i]
			if(y2<0) placed = true
		}
		return placed
	}
	/** 1つ以上Blockをfield枠内に置けるかどうか判定(fieldに変更は加えません)
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @return 1つ以上Blockをfield枠内に置けるならtrue, そうでないならfalse
	 */
	fun canPlaceToVisibleField(x:Int, y:Int, rt:Int = direction):Boolean {
		var placed = false
		// Bigでは専用処理
		if(big) {
			for(i in 0..<maxBlock) {
				val y2 = y+dataY[rt][i]*2

				// 4Block分置く
				for(k in 0..1)
					for(l in 0..1) {
						val y3 = y2+l
						if(y3>=0) placed = true
					}
			}

			return placed
		}

		for(i in 0..<maxBlock) {
			val y2 = y+dataY[rt][i]
			if(y2>=0) placed = true
		}

		return placed
	}

	/** fieldにピースを置く
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @param fld field
	 * @return 1つ以上Blockをfield枠内に置けたらtrue, そうでないならfalse
	 */
	fun placeToField(x:Int, y:Int, rt:Int = direction, fld:Field?):Boolean {
		updateConnectData()

		//On a Big piece, double its size.
		val size = if(big) 2 else 1

		var placed = false

		fld?.setAllAttribute(false, Block.ATTRIBUTE.LAST_COMMIT)
		for(i in 0..<maxBlock) {
			val x2 = x+dataX[rt][i]*size //Multiply co-ordinate offset by piece size.
			val y2 = y+dataY[rt][i]*size

			block[i].setAttribute(true, Block.ATTRIBUTE.LAST_COMMIT)

			/* Loop through width/height of the block, setting cells in the field.
	* If the piece is normal (size == 1), a standard, 1x1 space is allotted per block.
	* If the piece is big (size == 2), a 2x2 space is allotted per block. */
			for(k in 0..<size)
				for(l in 0..<size) {
					val x3 = x2+k
					val y3 = y2+l
					val blk = Block(block[i])

					// Set Big block connections
					if(big) {
						if(block[i].getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)&&block[i].getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) {
							// Top
							if(l==0) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_DOWN)
							// Bottom
							if(l==1) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_UP)
						} else if(block[i].getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) {
							// Top
							if(l==0) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_DOWN)
							// Bottom
							if(l==1) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_UP)
							// Left
							if(k==0) {
								blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT)
								blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT)
							}
							// Right
							if(k==1) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT)
						} else if(block[i].getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) {
							// Top
							if(l==0) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_DOWN)
							// Bottom
							if(l==1) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_UP)
							// Left
							if(k==0) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT)
							// Right
							if(k==1) {
								blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT)
								blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT)
							}
						}

						if(block[i].getAttribute(Block.ATTRIBUTE.CONNECT_UP)&&block[i].getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) {
							// Left
							if(k==0) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT)
							// Right
							if(k==1) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT)
						} else if(block[i].getAttribute(Block.ATTRIBUTE.CONNECT_UP)) {
							// Left
							if(k==0) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT)
							// Right
							if(k==1) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT)
							// Top
							if(l==0) {
								blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_UP)
								blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_DOWN)
							}
							// Bottom
							if(l==1) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_UP)
						} else if(block[i].getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) {
							// Left
							if(k==0) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT)
							// Right
							if(k==1) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT)
							// Top
							if(l==0) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_DOWN)
							// Bottom
							if(l==1) {
								blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_UP)
								blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_DOWN)
							}
						}
					}

					fld?.setBlock(x3, y3, blk)
					if(y3>=0) placed = true
				}
		}

		return placed
	}

	/** fieldにピースを置く
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param fld field
	 * @return 1つ以上Blockをfield枠内に置けたらtrue, そうでないならfalse
	 */
	fun placeToField(x:Int, y:Int, fld:Field?):Boolean = placeToField(x, y, direction, fld)

	/** ピースの当たり判定
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param fld field
	 * @return Blockに重なっていたらtrue, 重なっていないならfalse
	 */
	fun checkCollision(x:Int, y:Int, fld:Field?):Boolean = checkCollision(x, y, direction, fld)

	/** ピースの当たり判定
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @param fld field
	 * @return Blockに重なっていたらtrue, 重なっていないならfalse
	 */
	fun checkCollision(x:Int, y:Int, rt:Int = direction, fld:Field?):Boolean {
		fld?.let {
			// Bigでは専用処理
			if(big) return@checkCollision checkCollisionBig(x, y, rt, it)

			for(i in 0..<maxBlock) {
				val x2 = x+dataX[rt][i]
				val y2 = y+dataY[rt][i]

				if((x2>=it.width||y2>=it.height||it.getCoordAttribute(x2, y2)==Field.COORD_WALL)
					||(it.getCoordAttribute(x2, y2)!=Field.COORD_VANISH&&!it.getBlockEmpty(x2, y2))
				)
					return@checkCollision true
			}
		}
		return false
	}

	/** ピースの当たり判定 (Big用）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @param fld field
	 * @return Blockに重なっていたらtrue, 重なっていないならfalse
	 */
	private fun checkCollisionBig(x:Int, y:Int, rt:Int, fld:Field):Boolean {
		for(i in 0..<maxBlock) {
			val x2 = x+dataX[rt][i]*2
			val y2 = y+dataY[rt][i]*2

			// 4Block分調べる
			for(k in 0..1)
				for(l in 0..1) {
					val x3 = x2+k
					val y3 = y2+l

					if((x3>=fld.width||y3>=fld.height||fld.getCoordAttribute(x3, y3)==Field.COORD_WALL)
						||(fld.getCoordAttribute(x3, y3)!=Field.COORD_VANISH&&fld.getBlockEmpty(x3, y3))
					)
						return true
				}
		}

		return false
	}

	/** ピースをそのまま落とした場合のY-coordinateを取得
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @param fld field
	 * @return ピースをそのまま落とした場合のY-coordinate
	 */
	fun getBottom(x:Int, y:Int, rt:Int, fld:Field):Int {
		var y2 = y
		while(!checkCollision(x, y2, rt, fld)) y2++
		return y2-1
	}

	/** ピースをそのまま落とした場合のY-coordinateを取得
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param fld field
	 * @return ピースをそのまま落とした場合のY-coordinate
	 */
	fun getBottom(x:Int, y:Int, fld:Field):Int = getBottom(x, y, direction, fld)

	/** 現在位置からどこまで左に移動できるかを判定
	 * @param nowX 現在X位置
	 * @param nowY 現在Y位置
	 * @param rt ピースのDirection
	 * @param fld field
	 * @return 移動可能なもっとも左の位置
	 */
	fun getMostMovableLeft(nowX:Int, nowY:Int, rt:Int, fld:Field):Int {
		var x = nowX
		while(!checkCollision(x-1, nowY, rt, fld))
			x--
		return x
	}

	/** 現在位置からどこまで右に移動できるかを判定
	 * @param nowX 現在X位置
	 * @param nowY 現在Y位置
	 * @param rt ピースのDirection
	 * @param fld field
	 * @return 移動可能なもっとも右の位置
	 */
	fun getMostMovableRight(nowX:Int, nowY:Int, rt:Int, fld:Field):Int {
		var x = nowX
		while(!checkCollision(x+1, nowY, rt, fld))
			x++
		return x
	}

	/** spin buttonを押したあとのピースのDirectionを取得
	 * @param move spinDirection (-1:左 1:右 2:180度）
	 * @param dir 元のDirection
	 * @return spin buttonを押したあとのピースのDirection
	 */
	fun getSpinDirection(move:Int, dir:Int = direction):Int {
		var rt = dir+move

		if(move==2) {
			if(rt>3) rt -= 4
			if(rt<0) rt += 4
		} else {
			if(rt>3) rt = 0
			if(rt<0) rt = 3
		}

		return rt
	}

	/** fieldに置いたPieceが屋根になるかを判定
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param fld field
	 * @return 置いたピースの下に空白があるとtrue
	 */
	fun canMakeRoof(x:Int, y:Int, fld:Field?):Boolean {
		val rt:Int = direction
		fld?.let {
			dataX[rt].groupBy({x -> x}, {i -> dataY[rt][i]})
				.forEach {(px, c) ->
					val x2 = x+px
					val y2 = y+c.max()

					if(!it.getCoordVaild(x2, y2)||!it.getBlockEmpty(x2, y2)) return@canMakeRoof false
					if((it.getCoordVaild(x2, y2+1)||it.getCoordAttribute(x2, y2+1)!=Field.COORD_VANISH)
						&&it.getBlockEmpty(x2, y2+1, false))
						return@canMakeRoof true
				}
		}
		return false
	}

	/** Pieceの上にブロックがあるかを判定
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param fld field
	 * @return 置いたピースの下に空白があるとtrue
	 */
	fun isUnderRoof(x:Int, y:Int, fld:Field?):Boolean {
		val rt:Int = direction
		fld?.let {
			dataX[rt].groupBy({x -> x}, {i -> dataY[rt][i]})
				.forEach {(px, c) ->
					val x2 = x+px
					val y2 = y+c.min()
					for(yc in (-it.hiddenHeight..<y2).reversed())
						if(!it.getBlockEmpty(x2, yc, true))
							return@isUnderRoof true
				}
		}
		return false
	}

	fun finesseLimit(nowPieceX:Int):Int =
		FINESSE_LIST.getOrNull(id)?.let {it[direction%(it.size)]}
			?.let {it[maxOf(0, minOf(nowPieceX+minimumBlockX, it.size-1))]} ?: 0

	companion object {

		const val PIECE_NONE = -1
		const val PIECE_I = 0
		const val PIECE_L = 1
		const val PIECE_O = 2
		const val PIECE_Z = 3
		const val PIECE_T = 4
		const val PIECE_J = 5
		const val PIECE_S = 6
		const val PIECE_I1 = 7
		const val PIECE_I2 = 8
		const val PIECE_I3 = 9
		const val PIECE_L3 = 10

		/** 通常のBlockピースのIDのMaximumcount */
		const val PIECE_STANDARD_COUNT = 7

		/** BlockピースのIDのMaximumcount */
		val PIECE_COUNT get() = Shape.all.size// = 11

		/** default のBlockピースの data (X-coordinate) */
		val DEFAULT_PIECE_DATA_X =
			listOf(
				listOf(listOf(0, 1, 2, 3), listOf(2, 2, 2, 2), listOf(3, 2, 1, 0), listOf(1, 1, 1, 1)), // I
				listOf(listOf(2, 2, 1, 0), listOf(2, 1, 1, 1), listOf(0, 0, 1, 2), listOf(0, 1, 1, 1)), // L
				listOf(listOf(0, 1, 1, 0), listOf(1, 1, 0, 0), listOf(1, 0, 0, 1), listOf(0, 0, 1, 1)), // O
				listOf(listOf(0, 1, 1, 2), listOf(2, 2, 1, 1), listOf(2, 1, 1, 0), listOf(0, 0, 1, 1)), // Z
				listOf(listOf(1, 0, 1, 2), listOf(2, 1, 1, 1), listOf(1, 2, 1, 0), listOf(0, 1, 1, 1)), // T
				listOf(listOf(0, 0, 1, 2), listOf(2, 1, 1, 1), listOf(2, 2, 1, 0), listOf(0, 1, 1, 1)), // J
				listOf(listOf(2, 1, 1, 0), listOf(2, 2, 1, 1), listOf(0, 1, 1, 2), listOf(0, 0, 1, 1)), // S
				listOf(listOf(0), listOf(0), listOf(0), listOf(0)), // I1
				listOf(listOf(0, 1), listOf(1, 1), listOf(1, 0), listOf(0, 0)), // I2
				listOf(listOf(0, 1, 2), listOf(1, 1, 1), listOf(2, 1, 0), listOf(1, 1, 1)), // I3
				listOf(listOf(1, 0, 0), listOf(0, 0, 1), listOf(0, 1, 1), listOf(1, 1, 0))// L3
			)

		/** default のBlockピースの data (Y-coordinate) */
		val DEFAULT_PIECE_DATA_Y =
			listOf(
				listOf(listOf(1, 1, 1, 1), listOf(0, 1, 2, 3), listOf(2, 2, 2, 2), listOf(3, 2, 1, 0)), // I
				listOf(listOf(0, 1, 1, 1), listOf(2, 2, 1, 0), listOf(2, 1, 1, 1), listOf(0, 0, 1, 2)), // L
				listOf(listOf(0, 0, 1, 1), listOf(0, 1, 1, 0), listOf(1, 1, 0, 0), listOf(1, 0, 0, 1)), // O
				listOf(listOf(0, 0, 1, 1), listOf(0, 1, 1, 2), listOf(2, 2, 1, 1), listOf(2, 1, 1, 0)), // Z
				listOf(listOf(0, 1, 1, 1), listOf(1, 0, 1, 2), listOf(2, 1, 1, 1), listOf(1, 2, 1, 0)), // T
				listOf(listOf(0, 1, 1, 1), listOf(0, 0, 1, 2), listOf(2, 1, 1, 1), listOf(2, 2, 1, 0)), // J
				listOf(listOf(0, 0, 1, 1), listOf(2, 1, 1, 0), listOf(2, 2, 1, 1), listOf(0, 1, 1, 2)), // S
				listOf(listOf(0), listOf(0), listOf(0), listOf(0)), // I1
				listOf(listOf(0, 0), listOf(0, 1), listOf(1, 1), listOf(1, 0)), // I2
				listOf(listOf(1, 1, 1), listOf(0, 1, 2), listOf(1, 1, 1), listOf(2, 1, 0)), // I3
				listOf(listOf(1, 1, 0), listOf(1, 0, 0), listOf(0, 0, 1), listOf(0, 1, 1))// L3
			)

		/** 新スピン bonus用座標 dataA(X-coordinate) */
		val SPINBONUSDATA_HIGH_X =
			listOf(
				listOf(listOf(1, 2, 2, 1), listOf(1, 3, 1, 3), listOf(1, 2, 2, 1), listOf(0, 2, 0, 2)), // I
				listOf(listOf(1, 0), listOf(2, 2), listOf(1, 2), listOf(0, 0)), // L
				listOf(listOf(), listOf(), listOf(), listOf()), // O
				listOf(listOf(2, 0), listOf(2, 1), listOf(0, 2), listOf(0, 1)), // Z
				listOf(listOf(0, 2), listOf(2, 2), listOf(0, 2), listOf(0, 0)), // T
				listOf(listOf(1, 2), listOf(2, 2), listOf(1, 0), listOf(0, 0)), // J
				listOf(listOf(0, 2), listOf(1, 2), listOf(2, 0), listOf(1, 0)), // S
				listOf(listOf(), listOf(), listOf(), listOf()), // I1
				listOf(listOf(), listOf(), listOf(), listOf()), // I2
				listOf(listOf(), listOf(), listOf(), listOf()), // I3
				listOf(listOf(), listOf(), listOf(), listOf())
			)// L3

		/** 新スピン bonus用座標 dataA(Y-coordinate) */
		val SPINBONUSDATA_HIGH_Y =
			listOf(
				listOf(listOf(0, 2, 0, 2), listOf(1, 2, 2, 1), listOf(1, 3, 1, 3), listOf(1, 2, 2, 1)), // I
				listOf(listOf(0, 0), listOf(1, 0), listOf(2, 2), listOf(1, 2)), // L
				listOf(listOf(), listOf(), listOf(), listOf()), // O
				listOf(listOf(0, 1), listOf(2, 0), listOf(2, 1), listOf(0, 2)), // Z
				listOf(listOf(0, 0), listOf(0, 2), listOf(2, 2), listOf(0, 2)), // T
				listOf(listOf(0, 0), listOf(1, 2), listOf(2, 2), listOf(1, 0)), // J
				listOf(listOf(0, 1), listOf(2, 0), listOf(2, 1), listOf(0, 2)), // S
				listOf(listOf(), listOf(), listOf(), listOf()), // I1
				listOf(listOf(), listOf(), listOf(), listOf()), // I2
				listOf(listOf(), listOf(), listOf(), listOf()), // I3
				listOf(listOf(), listOf(), listOf(), listOf())
			)// L3

		/** 新スピン bonus用座標 dataB(X-coordinate) */
		val SPINBONUSDATA_LOW_X =
			listOf(
				listOf(listOf(-1, 4, -1, 4), listOf(2, 2, 2, 2), listOf(-1, 4, -1, 4), listOf(1, 1, 1, 1)), // I
				listOf(listOf(2, 0), listOf(0, 0), listOf(0, 2), listOf(2, 2)), // L
				listOf(listOf(), listOf(), listOf(), listOf()), // O
				listOf(listOf(-1, 3), listOf(2, 1), listOf(3, -1), listOf(0, 1)), // Z
				listOf(listOf(0, 2), listOf(0, 0), listOf(0, 2), listOf(2, 2)), // T
				listOf(listOf(0, 2), listOf(0, 0), listOf(2, 0), listOf(2, 2)), // J
				listOf(listOf(3, -1), listOf(1, 2), listOf(-1, 3), listOf(1, 0)), // S
				listOf(listOf(), listOf(), listOf(), listOf()), // I1
				listOf(listOf(), listOf(), listOf(), listOf()), // I2
				listOf(listOf(), listOf(), listOf(), listOf()), // I3
				listOf(listOf(), listOf(), listOf(), listOf())
			)// L3

		/** 新スピン bonus用座標 dataB(Y-coordinate) */
		val SPINBONUSDATA_LOW_Y =
			listOf(
				listOf(listOf(1, 1, 1, 1), listOf(-1, 4, -1, 4), listOf(2, 2, 2, 2), listOf(-1, 4, -1, 4)), // I
				listOf(listOf(2, 2), listOf(2, 0), listOf(0, 0), listOf(0, 3)), // L
				listOf(listOf(), listOf(), listOf(), listOf()), // O
				listOf(listOf(0, 1), listOf(-1, 3), listOf(2, 1), listOf(3, -1)), // Z
				listOf(listOf(2, 2), listOf(0, 2), listOf(0, 0), listOf(0, 2)), // T
				listOf(listOf(2, 2), listOf(0, 2), listOf(0, 0), listOf(2, 0)), // J
				listOf(listOf(0, 1), listOf(-1, 3), listOf(2, 1), listOf(3, -1)), // S
				listOf(listOf(), listOf(), listOf(), listOf()), // I1
				listOf(listOf(), listOf(), listOf(), listOf()), // I2
				listOf(listOf(), listOf(), listOf(), listOf()), // I3
				listOf(listOf(), listOf(), listOf(), listOf())
			)// L3

		/** Directionの定数 */
		const val DIRECTION_UP = 0
		const val DIRECTION_RIGHT = 1
		const val DIRECTION_DOWN = 2
		const val DIRECTION_LEFT = 3
		const val DIRECTION_RANDOM = 4

		/** DirectionのMaximumcount */
		const val DIRECTION_COUNT = 4

		val FINESSE_LIST = listOf(
			listOf(
				listOf(1, 2, 1, 0, 1, 2, 1),
				listOf(2, 2, 2, 2, 1, 1, 2, 2, 2, 2),
			), // I
			listOf(
				listOf(1, 2, 1, 0, 1, 2, 2, 1),
				listOf(2, 2, 3, 2, 1, 2, 3, 3, 2),
				listOf(3, 4, 3, 2, 3, 4, 4, 3),
				listOf(2, 3, 2, 1, 2, 3, 3, 2, 2),
			), // L
			listOf(
				listOf(1, 2, 2, 1, 0, 1, 2, 2, 1),
			), // O
			listOf(
				listOf(1, 2, 1, 0, 1, 2, 2, 1),
				listOf(2, 2, 2, 1, 1, 2, 3, 2, 2),
			), // Z
			listOf(
				listOf(1, 2, 1, 0, 1, 2, 2, 1),
				listOf(2, 2, 3, 2, 1, 2, 3, 3, 2),
				listOf(3, 4, 3, 2, 3, 4, 4, 3),
				listOf(2, 3, 2, 1, 2, 3, 3, 2, 2),
			), // T
			listOf(
				listOf(1, 2, 1, 0, 1, 2, 2, 1),
				listOf(2, 2, 3, 2, 1, 2, 3, 3, 2),
				listOf(3, 4, 3, 2, 3, 4, 4, 3),
				listOf(2, 3, 2, 1, 2, 3, 3, 2, 2),
			), // J
			listOf(
				listOf(1, 2, 1, 0, 1, 2, 2, 1),
				listOf(2, 2, 2, 1, 1, 2, 3, 2, 2),
			), // S

		)
		/** ピース名を取得
		 * @param id ピースID
		 * @return ピース名(不正な場合は ? を返す)
		 */
		@Deprecated("This will be enumed", ReplaceWith("Shape.name", "mu.nu.nullpo.game.component.Shape"))
		fun getPieceName(id:Int):String = if(id>=0&&id<Shape.names.size) Shape.names[id] else "?"
	}

	/** BlockピースのIDの定数 */
	enum class Shape {
		I, L, O, Z, T, J, S, I1, I2, I3, L3;

		companion object {
			val all = entries
			val names:List<String> get() = all.map {it.name}
			fun name(id:Int):String = all.getOrNull(id)?.name ?: "?"
		}
	}

}
