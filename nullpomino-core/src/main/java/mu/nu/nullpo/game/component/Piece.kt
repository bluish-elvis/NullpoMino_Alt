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

import java.io.Serializable
import kotlin.math.roundToInt

/** Blockピース
 * @param id BlockピースのID */
class Piece(
	/** ID */
	@JvmField val id:Int = 0):Serializable {
	val type:Shape get() = Shape.values()[id]

	/** Direction */
	var direction:Int = DIRECTION_UP

	/** BigBlock */
	@JvmField var big:Boolean = false

	/** Connect blocks in this piece? */
	var connectBlocks:Boolean = true

	/** 相対X位置 (4Direction×nBlock) */
	var dataX:Array<IntArray> = Array(DIRECTION_COUNT) {IntArray(maxBlock)}

	/** 相対Y位置 (4Direction×nBlock) */
	var dataY:Array<IntArray> = Array(DIRECTION_COUNT) {IntArray(maxBlock)}

	/** ピースを構成するBlock (nBlock) */
	var block:Array<Block> = Array(maxBlock) {Block()}

	/** 相対X位置と相対Y位置がオリジナル stateからずらされているならtrue */
	var offsetApplied:Boolean = false

	/** 相対X位置のずれ幅 */
	var dataOffsetX:IntArray = IntArray(DIRECTION_COUNT)

	/** 相対Y位置のずれ幅 */
	var dataOffsetY:IntArray = IntArray(DIRECTION_COUNT)

/*dataX = Array(DIRECTION_COUNT) {IntArray(maxBlock)}
	dataY = Array(DIRECTION_COUNT) {IntArray(maxBlock)}
	block = Array(maxBlock) {Block()}
	dataOffsetX = IntArray(DIRECTION_COUNT)
	dataOffsetY = IntArray(DIRECTION_COUNT)*/
	/** 1つのピースに含まれるBlockのcountを取得
	 * @return 1つのピースに含まれるBlockのcount
	 */
	val maxBlock:Int
		get() = DEFAULT_PIECE_DATA_X[id][direction].size

	/** Fetches the colors of the blocks in the piece
	 * @return An int array containing the cint of each block
	 */
	val colors:IntArray
		get() = IntArray(block.size) {block[it].cint}

	/** ピースの幅を取得
	 * @return ピースの幅
	 */
	val width:Int
		get() {
			var max = dataX[direction][0]
			var min = dataX[direction][0]

			for(j in 1 until maxBlock) {
				val bx = dataX[direction][j]

				max = maxOf(bx, max)
				min = minOf(bx, min)
			}

			var wide = 1
			if(big) wide = 2

			return (max-min)*wide
		}
	val centerX:Int get() = (width/2f).roundToInt()
	/** ピースの高さを取得
	 * @return ピースの高さ
	 */
	val height:Int
		get() {
			var max = dataY[direction][0]
			var min = dataY[direction][0]

			for(j in 1 until maxBlock) {
				val by = dataY[direction][j]

				max = maxOf(by, max)
				min = minOf(by, min)
			}

			var wide = 1
			if(big) wide = 2

			return (max-min)*wide
		}

	/** テトラミノの最も高いBlockのX-coordinateを取得
	 * @return テトラミノの最も高いBlockのX-coordinate
	 */
	val minimumBlockX:Int
		get() {
			var min = dataX[direction][0]

			for(j in 1 until maxBlock) {
				val by = dataX[direction][j]

				min = minOf(by, min)
			}

			return min*if(big) 2 else 1
		}

	/** テトラミノの最も低いBlockのX-coordinateを取得
	 * @return テトラミノの最も低いBlockのX-coordinate
	 */
	val maximumBlockX:Int
		get() {
			var max = dataX[direction][0]

			for(j in 1 until maxBlock) {
				val by = dataX[direction][j]

				max = maxOf(by, max)
			}

			return max*if(big) 2 else 1
		}

	/** テトラミノの最も高いBlockのY-coordinateを取得
	 * @return テトラミノの最も高いBlockのY-coordinate
	 */
	val minimumBlockY:Int
		get() {
			var min = dataY[direction][0]

			for(j in 1 until maxBlock) {
				val by = dataY[direction][j]

				min = minOf(by, min)
			}

			return min*if(big) 2 else 1
		}

	/** テトラミノの最も低いBlockのY-coordinateを取得
	 * @return テトラミノの最も低いBlockのY-coordinate
	 */
	val maximumBlockY:Int
		get() {
			var max = dataY[direction][0]

			for(j in 1 until maxBlock) {
				val by = dataY[direction][j]

				max = maxOf(by, max)
			}

			return max*if(big) 2 else 1
		}

	init {
		resetOffsetArray()
		updateConnectData()
	}

	/** Copy constructor
	 * @param p Copy source
	 */
	constructor(p:Piece):this(p.id) {
		copy(p)
		updateConnectData()
	}

	/** Blockピースの dataを他のPieceからコピー
	 * @param p Copy source
	 */
	fun copy(p:Piece) {
		direction = p.direction
		big = p.big
		offsetApplied = p.offsetApplied
		connectBlocks = p.connectBlocks

		dataX = p.dataX.clone()
		dataY = p.dataY.clone()
		block = p.block.clone()
		dataOffsetX = p.dataOffsetX.clone()
		dataOffsetY = p.dataOffsetY.clone()
	}

	/** すべてのBlock stateをbと同じに設定
	 * @param b 設定するBlock
	 */
	fun setBlock(b:Block) {
		for(element in block)
			element.copy(b)
	}

	/** すべてのBlock colorを変更
	 * @param color 色
	 */
	fun setColor(color:Int) {
		for(aBlock in block) aBlock.cint = color
	}

	/** Changes the colors of the blocks individually; allows one piece to have
	 * blocks of multiple colors
	 * @param color Array with each cell specifying a cint of a block
	 */
	fun setColor(color:IntArray) {
		val length = minOf(block.size, color.size)
		for(i in 0 until length)
			block[i].cint = color[i]
	}

	/** Sets all blocks to an item block
	 * @param item ID number of the item
	 */
	fun setItem(item:Int) {
		for(aBlock in block) aBlock.item = item
	}

	/** Sets the items of the blocks individually; allows one piece to have
	 * different item settings for each block
	 * @param item Array with each element specifying a cint of a block
	 */
	fun setItem(item:IntArray) {
		val length = minOf(block.size, item.size)
		for(i in 0 until length)
			block[i].item = item[i]
	}

	/** Sets all blocks' hard count
	 * @param hard Hard count
	 */
	fun setHard(hard:Int) {
		for(aBlock in block) aBlock.hard = hard
	}

	/** Sets the hard counts of the blocks individually; allows one piece to
	 * have
	 * different hard count settings for each block
	 * @param hard Array with each element specifying a hard count of a block
	 */
	fun setHard(hard:IntArray) {
		val length = minOf(block.size, hard.size)
		for(i in 0 until length)
			block[i].hard = hard[i]
	}

	/** すべてのBlockの模様を変更
	 * @param skin 模様
	 */
	fun setSkin(skin:Int) {
		for(aBlock in block) aBlock.skin = skin
	}

	/** すべてのBlockの経過 frame を変更
	 * @param elapsedFrames 固定してから経過した frame count
	 */
	fun setElapsedFrames(elapsedFrames:Int) {
		for(aBlock in block) aBlock.elapsedFrames = elapsedFrames
	}

	/** すべてのBlockの暗さまたは明るさを変更
	 * @param darkness 暗さまたは明るさ (0.03だったら3%暗く, -0.05だったら5%明るい）
	 */
	fun setDarkness(darkness:Float) {
		for(aBlock in block) aBlock.darkness = darkness
	}

	/** すべてのBlockの透明度を変更
	 * @param alpha 透明度 (1fで不透明, 0.0fで完全に透明）
	 */
	fun setAlpha(alpha:Float) {
		for(aBlock in block) aBlock.alpha = alpha
	}

	/** すべてのBlockの属性を設定
	 * @param attrs 変更したい属性
	 * @param status 変更後 state
	 */
	fun setAttribute(status:Boolean, vararg attrs:Block.ATTRIBUTE) {
		for(element in block)
			element.setAttribute(status, *attrs)
	}

	/** 相対X位置と相対Y位置をずらす
	 * @param offsetX X位置補正量の配列 (int[DIRECTION_COUNT]）
	 * @param offsetY Y位置補正量の配列 (int[DIRECTION_COUNT]）
	 */
	fun applyOffsetArray(offsetX:IntArray, offsetY:IntArray) {
		applyOffsetArrayX(offsetX)
		applyOffsetArrayY(offsetY)
	}

	/** 相対X位置をずらす
	 * @param offsetX X位置補正量の配列 (int[DIRECTION_COUNT]）
	 */
	fun applyOffsetArrayX(offsetX:IntArray) {
		offsetApplied = true

		for(i in 0 until DIRECTION_COUNT) {
			for(j in 0 until maxBlock)
				dataX[i][j] += offsetX[i]
			dataOffsetX[i] = offsetX[i]
		}
	}

	/** 相対Y位置をずらす
	 * @param offsetY Y位置補正量の配列 (int[DIRECTION_COUNT]）
	 */
	fun applyOffsetArrayY(offsetY:IntArray) {
		offsetApplied = true

		for(i in 0 until DIRECTION_COUNT) {
			for(j in 0 until maxBlock)
				dataY[i][j] += offsetY[i]
			dataOffsetY[i] = offsetY[i]
		}
	}

	/** 相対X位置と相対Y位置を初期状態に戻す */
	fun resetOffsetArray() {
		for(i in 0 until DIRECTION_COUNT) {
			for(j in 0 until maxBlock) {
				dataX[i][j] = DEFAULT_PIECE_DATA_X[id][i][j]
				dataY[i][j] = DEFAULT_PIECE_DATA_Y[id][i][j]
			}
			dataOffsetX[i] = 0
			dataOffsetY[i] = 0
		}
		offsetApplied = false
	}

	/** Blockの繋がり dataを更新 */
	fun updateConnectData() {
		for(j in 0 until maxBlock) {
			// 相対X位置と相対Y位置
			val bx = dataX[direction][j]
			val by = dataY[direction][j]

			block[j].setAttribute(false, Block.ATTRIBUTE.CONNECT_UP)
			block[j].setAttribute(false, Block.ATTRIBUTE.CONNECT_DOWN)
			block[j].setAttribute(false, Block.ATTRIBUTE.CONNECT_LEFT)
			block[j].setAttribute(false, Block.ATTRIBUTE.CONNECT_RIGHT)

			if(connectBlocks) {
				block[j].setAttribute(false, Block.ATTRIBUTE.BROKEN)
				// 他の3つのBlockとの繋がりを調べる
				for(k in 0 until maxBlock)
					if(k!=j) {
						val bx2 = dataX[direction][k]
						val by2 = dataY[direction][k]

						if(bx==bx2&&by-1==by2) block[j].setAttribute(true, Block.ATTRIBUTE.CONNECT_UP) // Up
						if(bx==bx2&&by+1==by2) block[j].setAttribute(true, Block.ATTRIBUTE.CONNECT_DOWN) // Down
						if(by==by2&&bx-1==bx2) block[j].setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT) // 左
						if(by==by2&&bx+1==bx2) block[j].setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT) // 右
					}
			} else
				block[j].setAttribute(true, Block.ATTRIBUTE.BROKEN)
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
		if(big) for(i in 0 until maxBlock) {
			val y2 = y+dataY[rt][i]*2

			// 4Block分置く
			for(k in 0..1)
				for(l in 0..1) {
					val y3 = y2+l
					if(y3<0) placed = true
				}
		} else for(i in 0 until maxBlock) {
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

			for(i in 0 until maxBlock) {
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

		for(i in 0 until maxBlock) {
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
		for(i in 0 until maxBlock) {
			val x2 = x+dataX[rt][i]*size //Multiply co-ordinate offset by piece size.
			val y2 = y+dataY[rt][i]*size

			block[i].setAttribute(true, Block.ATTRIBUTE.LAST_COMMIT)

			/* Loop through width/height of the block, setting cells in the
 * field.
 * If the piece is normal (size == 1), a standard, 1x1 space is
 * allotted per block.
 * If the piece is big (size == 2), a 2x2 space is allotted per
 * block. */
			for(k in 0 until size)
				for(l in 0 until size) {
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
			if(big) return checkCollisionBig(x, y, rt, it)

			for(i in 0 until maxBlock) {
				val x2 = x+dataX[rt][i]
				val y2 = y+dataY[rt][i]

				if((x2>=it.width||y2>=it.height||it.getCoordAttribute(x2, y2)==Field.COORD_WALL)
					||(it.getCoordAttribute(x2, y2)!=Field.COORD_VANISH&&it.getBlockColor(x2, y2)!=Block.BLOCK_COLOR_NONE))
					return true
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
		for(i in 0 until maxBlock) {
			val x2 = x+dataX[rt][i]*2
			val y2 = y+dataY[rt][i]*2

			// 4Block分調べる
			for(k in 0..1)
				for(l in 0..1) {
					val x3 = x2+k
					val y3 = y2+l

					if((x3>=fld.width||y3>=fld.height||fld.getCoordAttribute(x3, y3)==Field.COORD_WALL)
						||(fld.getCoordAttribute(x3, y3)!=Field.COORD_VANISH&&fld.getBlockColor(x3, y3)!=Block.BLOCK_COLOR_NONE))
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
	fun getBottom(x:Int, y:Int, rt:Int, fld:Field?):Int {
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
	fun getBottom(x:Int, y:Int, fld:Field?):Int = getBottom(x, y, direction, fld)

	/** 現在位置からどこまで左に移動できるかを判定
	 * @param nowX 現在X位置
	 * @param nowY 現在Y位置
	 * @param rt ピースのDirection
	 * @param fld field
	 * @return 移動可能なもっとも左の位置
	 */
	fun getMostMovableLeft(nowX:Int, nowY:Int, rt:Int, fld:Field?):Int {
		var x = nowX
		while(fld!=null&&!checkCollision(x-1, nowY, rt, fld))
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
	fun getMostMovableRight(nowX:Int, nowY:Int, rt:Int, fld:Field?):Int {
		var x = nowX
		while(fld!=null&&!checkCollision(x+1, nowY, rt, fld))
			x++
		return x
	}

	/** rotation buttonを押したあとのピースのDirectionを取得
	 * @param move rotationDirection (-1:左 1:右 2:180度）
	 * @return rotation buttonを押したあとのピースのDirection
	 */
	fun getRotateDirection(move:Int):Int {
		var rt = direction+move

		if(move==2) {
			if(rt>3) rt -= 4
			if(rt<0) rt += 4
		} else {
			if(rt>3) rt = 0
			if(rt<0) rt = 3
		}

		return rt
	}

	/** rotation buttonを押したあとのピースのDirectionを取得
	 * @param move rotationDirection (-1:左 1:右 2:180度）
	 * @param dir 元のDirection
	 * @return rotation buttonを押したあとのピースのDirection
	 */
	fun getRotateDirection(move:Int, dir:Int):Int {
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

	companion object {
		/** Serial version ID */
		private const val serialVersionUID = 1204901746632931186L

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

		/** BlockピースのName */
		@Deprecated("This will be enumed", ReplaceWith("Shape.names", "mu.nu.nullpo.game.component.Shape"))
		val PIECE_NAMES = Shape.values().map {it.name}.toTypedArray()

		/** 通常のBlockピースのIDのMaximumcount */
		const val PIECE_STANDARD_COUNT = 7

		/** BlockピースのIDのMaximumcount */
		val PIECE_COUNT get() = Shape.values().size// = 11

		/** default のBlockピースの data (X-coordinate) */
		val DEFAULT_PIECE_DATA_X =
			arrayOf(arrayOf(intArrayOf(0, 1, 2, 3), intArrayOf(2, 2, 2, 2), intArrayOf(3, 2, 1, 0), intArrayOf(1, 1, 1, 1)), // I
				arrayOf(intArrayOf(2, 2, 1, 0), intArrayOf(2, 1, 1, 1), intArrayOf(0, 0, 1, 2), intArrayOf(0, 1, 1, 1)), // L
				arrayOf(intArrayOf(0, 1, 1, 0), intArrayOf(1, 1, 0, 0), intArrayOf(1, 0, 0, 1), intArrayOf(0, 0, 1, 1)), // O
				arrayOf(intArrayOf(0, 1, 1, 2), intArrayOf(2, 2, 1, 1), intArrayOf(2, 1, 1, 0), intArrayOf(0, 0, 1, 1)), // Z
				arrayOf(intArrayOf(1, 0, 1, 2), intArrayOf(2, 1, 1, 1), intArrayOf(1, 2, 1, 0), intArrayOf(0, 1, 1, 1)), // T
				arrayOf(intArrayOf(0, 0, 1, 2), intArrayOf(2, 1, 1, 1), intArrayOf(2, 2, 1, 0), intArrayOf(0, 1, 1, 1)), // J
				arrayOf(intArrayOf(2, 1, 1, 0), intArrayOf(2, 2, 1, 1), intArrayOf(0, 1, 1, 2), intArrayOf(0, 0, 1, 1)), // S
				arrayOf(intArrayOf(0), intArrayOf(0), intArrayOf(0), intArrayOf(0)), // I1
				arrayOf(intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(1, 0), intArrayOf(0, 0)), // I2
				arrayOf(intArrayOf(0, 1, 2), intArrayOf(1, 1, 1), intArrayOf(2, 1, 0), intArrayOf(1, 1, 1)), // I3
				arrayOf(intArrayOf(1, 0, 0), intArrayOf(0, 0, 1), intArrayOf(0, 1, 1), intArrayOf(1, 1, 0)))// L3

		/** default のBlockピースの data (Y-coordinate) */
		val DEFAULT_PIECE_DATA_Y =
			arrayOf(arrayOf(intArrayOf(1, 1, 1, 1), intArrayOf(0, 1, 2, 3), intArrayOf(2, 2, 2, 2), intArrayOf(3, 2, 1, 0)), // I
				arrayOf(intArrayOf(0, 1, 1, 1), intArrayOf(2, 2, 1, 0), intArrayOf(2, 1, 1, 1), intArrayOf(0, 0, 1, 2)), // L
				arrayOf(intArrayOf(0, 0, 1, 1), intArrayOf(0, 1, 1, 0), intArrayOf(1, 1, 0, 0), intArrayOf(1, 0, 0, 1)), // O
				arrayOf(intArrayOf(0, 0, 1, 1), intArrayOf(0, 1, 1, 2), intArrayOf(2, 2, 1, 1), intArrayOf(2, 1, 1, 0)), // Z
				arrayOf(intArrayOf(0, 1, 1, 1), intArrayOf(1, 0, 1, 2), intArrayOf(2, 1, 1, 1), intArrayOf(1, 2, 1, 0)), // T
				arrayOf(intArrayOf(0, 1, 1, 1), intArrayOf(0, 0, 1, 2), intArrayOf(2, 1, 1, 1), intArrayOf(2, 2, 1, 0)), // J
				arrayOf(intArrayOf(0, 0, 1, 1), intArrayOf(2, 1, 1, 0), intArrayOf(2, 2, 1, 1), intArrayOf(0, 1, 1, 2)), // S
				arrayOf(intArrayOf(0), intArrayOf(0), intArrayOf(0), intArrayOf(0)), // I1
				arrayOf(intArrayOf(0, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(1, 0)), // I2
				arrayOf(intArrayOf(1, 1, 1), intArrayOf(0, 1, 2), intArrayOf(1, 1, 1), intArrayOf(2, 1, 0)), // I3
				arrayOf(intArrayOf(1, 1, 0), intArrayOf(1, 0, 0), intArrayOf(0, 0, 1), intArrayOf(0, 1, 1)))// L3

		/** 新スピン bonus用座標 dataA(X-coordinate) */
		val SPINBONUSDATA_HIGH_X =
			arrayOf(arrayOf(intArrayOf(1, 2, 2, 1), intArrayOf(1, 3, 1, 3), intArrayOf(1, 2, 2, 1), intArrayOf(0, 2, 0, 2)), // I
				arrayOf(intArrayOf(1, 0), intArrayOf(2, 2), intArrayOf(1, 2), intArrayOf(0, 0)), // L
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()), // O
				arrayOf(intArrayOf(2, 0), intArrayOf(2, 1), intArrayOf(0, 2), intArrayOf(0, 1)), // Z
				arrayOf(intArrayOf(0, 2), intArrayOf(2, 2), intArrayOf(0, 2), intArrayOf(0, 0)), // T
				arrayOf(intArrayOf(1, 2), intArrayOf(2, 2), intArrayOf(1, 0), intArrayOf(0, 0)), // J
				arrayOf(intArrayOf(0, 2), intArrayOf(1, 2), intArrayOf(2, 0), intArrayOf(1, 0)), // S
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()), // I1
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()), // I2
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()), // I3
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()))// L3

		/** 新スピン bonus用座標 dataA(Y-coordinate) */
		val SPINBONUSDATA_HIGH_Y =
			arrayOf(arrayOf(intArrayOf(0, 2, 0, 2), intArrayOf(1, 2, 2, 1), intArrayOf(1, 3, 1, 3), intArrayOf(1, 2, 2, 1)), // I
				arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(2, 2), intArrayOf(1, 2)), // L
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()), // O
				arrayOf(intArrayOf(0, 1), intArrayOf(2, 0), intArrayOf(2, 1), intArrayOf(0, 2)), // Z
				arrayOf(intArrayOf(0, 0), intArrayOf(0, 2), intArrayOf(2, 2), intArrayOf(0, 2)), // T
				arrayOf(intArrayOf(0, 0), intArrayOf(1, 2), intArrayOf(2, 2), intArrayOf(1, 0)), // J
				arrayOf(intArrayOf(0, 1), intArrayOf(2, 0), intArrayOf(2, 1), intArrayOf(0, 2)), // S
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()), // I1
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()), // I2
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()), // I3
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()))// L3

		/** 新スピン bonus用座標 dataB(X-coordinate) */
		val SPINBONUSDATA_LOW_X =
			arrayOf(arrayOf(intArrayOf(-1, 4, -1, 4), intArrayOf(2, 2, 2, 2), intArrayOf(-1, 4, -1, 4), intArrayOf(1, 1, 1, 1)), // I
				arrayOf(intArrayOf(2, 0), intArrayOf(0, 0), intArrayOf(0, 2), intArrayOf(2, 2)), // L
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()), // O
				arrayOf(intArrayOf(-1, 3), intArrayOf(2, 1), intArrayOf(3, -1), intArrayOf(0, 1)), // Z
				arrayOf(intArrayOf(0, 2), intArrayOf(0, 0), intArrayOf(0, 2), intArrayOf(2, 2)), // T
				arrayOf(intArrayOf(0, 2), intArrayOf(0, 0), intArrayOf(2, 0), intArrayOf(2, 2)), // J
				arrayOf(intArrayOf(3, -1), intArrayOf(1, 2), intArrayOf(-1, 3), intArrayOf(1, 0)), // S
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()), // I1
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()), // I2
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()), // I3
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()))// L3

		/** 新スピン bonus用座標 dataB(Y-coordinate) */
		val SPINBONUSDATA_LOW_Y =
			arrayOf(arrayOf(intArrayOf(1, 1, 1, 1), intArrayOf(-1, 4, -1, 4), intArrayOf(2, 2, 2, 2), intArrayOf(-1, 4, -1, 4)), // I
				arrayOf(intArrayOf(2, 2), intArrayOf(2, 0), intArrayOf(0, 0), intArrayOf(0, 3)), // L
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()), // O
				arrayOf(intArrayOf(0, 1), intArrayOf(-1, 3), intArrayOf(2, 1), intArrayOf(3, -1)), // Z
				arrayOf(intArrayOf(2, 2), intArrayOf(0, 2), intArrayOf(0, 0), intArrayOf(0, 2)), // T
				arrayOf(intArrayOf(2, 2), intArrayOf(0, 2), intArrayOf(0, 0), intArrayOf(2, 0)), // J
				arrayOf(intArrayOf(0, 1), intArrayOf(-1, 3), intArrayOf(2, 1), intArrayOf(3, -1)), // S
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()), // I1
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()), // I2
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()), // I3
				arrayOf(intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf()))// L3

		/** Directionの定数 */
		const val DIRECTION_UP = 0
		const val DIRECTION_RIGHT = 1
		const val DIRECTION_DOWN = 2
		const val DIRECTION_LEFT = 3
		const val DIRECTION_RANDOM = 4

		/** DirectionのMaximumcount */
		const val DIRECTION_COUNT = 4

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
			val names:List<String> get() = values().map {it.name}
			fun name(id:Int):String = if(id>=0&&id<=values().size) values()[id].name else "?"
		}
	}

}
