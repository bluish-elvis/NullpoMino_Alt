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
package mu.nu.nullpo.game.component

import kotlinx.serialization.Serializable
import mu.nu.nullpo.game.component.Piece.Companion.DIRECTION_COUNT
import kotlin.math.roundToInt

/** Blockピース
@param id BlockピースのShape ID */
@Serializable
class Piece(var shape:Shape) {
	/** Shape ID */
	var id:Int
		get() = shape.ordinal
		set(value) {
			shape = Shape.all[value]
//			direction = DIRECTION_UP
//			big = false
//			connectBlocks = true
//			setColor()
//			block = List(maxBlock) {Block()}
			resetOffsetArray()
		}
	/** Direction */
	var direction = DIRECTION_UP; get() = field%DIRECTION_COUNT
	/** BigBlock */
	var big = false
	/** Connect blocks in this piece? */
	var connectBlocks = true
	/** 1つのピースに含まれるBlockのcountを取得
	 * @return 1つのピースに含まれるBlockのcount
	 */
	val maxBlock get() = shape.pos[direction].size
	/** ピースを構成するBlock (nBlock) */
	var block = List(maxBlock) {Block()}
	/** 相対X位置のずれ幅 */
	val dataOffsetX = MutableList(DIRECTION_COUNT) {0}
	/** 相対Y位置のずれ幅 */
	val dataOffsetY = MutableList(DIRECTION_COUNT) {0}
	/** 相対X位置と相対Y位置がオリジナル stateからずらされているならtrue */
	val offsetApplied get() = dataOffsetX.any {it!=0}||dataOffsetY.any {it!=0}
	/** shape position 相対X位置と相対Y位置の配列 (4Direction×nBlock×<x,y>) */
	private val sp:List<List<Pair<Int, Int>>>
		get() =
			shape.pos.mapIndexed {i, it ->
				it.map {(x, y) ->
					(x+dataOffsetX.getOrElse(i) {0}) to (y+dataOffsetY.getOrElse(i) {0})
				}
			}
	/** 相対X位置と相対Y位置の配列をオーバーライド (4Direction×nBlock×<x,y>) */
	var overridePos:List<MutableList<Pair<Int, Int>>>? = null
	val pos:List<List<Pair<Int, Int>>> get() = overridePos?:sp
	val data:List<List<Triple<Int, Int, Block>>>
		get() = pos.map {d -> d.zip(block).map {(pos, b) -> Triple(pos.first, pos.second, b)}}
	/** 相対X位置 (4Direction×nBlock) */
	val dataX:List<List<Int>> get() = pos.map {b -> b.map {(x) -> x}}
	/** 相対Y位置 (4Direction×nBlock) */
	val dataY:List<List<Int>> get() = pos.map {b -> b.map {(_, y) -> y}}
	/** ピースを構成するBlockの位置とBlockの対応 map (at current Direction: x:<y: Block>) */
	val map
		get() = sp[direction].zip(block)
			.groupBy({(pos, _) -> pos.first}) {(pos, b) -> pos.second to b}
			.mapValues {(_, it) -> it.toMap()}

	/** Fetches the colors of the blocks in the piece
	 * @return An int array containing the color of each block
	 */
	val colors get() = List(block.size) {block[it].cint}

	/** ゲームが始まってから何番目に置かれるBlockか (負countだったら初期配置やgarbage block) */
	var placeNum = -1
		set(value) {
			block.forEach {it.placeNum = value}
			field = value
		}

	/** @return ピース回転軸のX-coordinate */
	val spinCX get() = dataX.flatten().let {(it.maxOrNull()?:0)-(it.minOrNull()?:0)}/2f
	/** @return ピース回転軸のY-coordinate */
	val spinCY get() = dataY.flatten().let {(it.maxOrNull()?:0)-(it.minOrNull()?:0)}/2f
	/** @return ピースの幅*/
	val width get() = maximumBlockX-minimumBlockX
	/** @return ピースの高さ*/
	val height get() = maximumBlockY-minimumBlockY
	val centerX get() = (width/2.0f).roundToInt()
	val centerY get() = (height/2.0f).roundToInt()
	/** @return テトラミノの最も高いBlockのX-coordinate*/
	val minimumBlockX get() = (dataX[direction].minOrNull()?:0)*if(big) 2 else 1

	/** @return テトラミノの最も低いBlockのX-coordinate */
	val maximumBlockX get() = (dataX[direction].maxOrNull()?:0)*if(big) 2 else 1

	/** @return テトラミノの最も高いBlockのY-coordinate */
	val minimumBlockY get() = (dataY[direction].minOrNull()?:0)*if(big) 2 else 1

	/** @return テトラミノの最も低いBlockのY-coordinate */
	val maximumBlockY get() = (dataY[direction].maxOrNull()?:0)*if(big) 2 else 1

	init {
		resetOffsetArray()
	}

	/** Copy constructor from [p] */
	constructor(p:Piece):this(p.id) {
		replace(p)
		updateConnectData()
	}

	constructor(id:Int = 0):this(Shape.all[id])
	constructor():this(0)
	/** Blockピースの dataを[p]からコピー
	 * @param keep idを保持
	 */
	fun replace(p:Piece, keep:Boolean = false) {
		if(keep) id = p.id
		direction = p.direction
		big = p.big
		connectBlocks = p.connectBlocks
		block = List(p.maxBlock) {Block(p.block[it])}

		block = p.block.toList()
		p.dataOffsetX.forEachIndexed {i, it -> dataOffsetX[i] = it}
		p.dataOffsetY.forEachIndexed {i, it -> dataOffsetY[i] = it}
	}

	/** すべてのBlock stateを[_blk]と同じに設定
	 */
	fun setBlock(_blk:Block) = block.forEach {it.replace(_blk)}

	/** すべてのBlock colorを[color]に変更*/
	fun setColor(color:Block.COLOR) = block.forEach {it.color = color}

	/** すべてのBlock colorを[color]に変更*/
	fun setColor(color:Int) = block.forEach {it.cint = color}

	/** Changes the colors of the blocks individually; allows one piece to have
	 * blocks of multiple colors
	 * @param color Array with each cell specifying a color of a block
	 */
	fun setColor(color:List<*>) {
		when(color.firstOrNull()) {
			is Block.COLOR -> block.forEachIndexed {i, it -> it.color = color[i%color.size] as Block.COLOR}
			is Int -> block.forEachIndexed {i, it -> it.cint = color[i%color.size] as Int}
		}
	}

	/** Changes the colors of the blocks individually; allows one piece to have
	 * blocks of multiple colors
	 * @param color Array with each cell specifying a color of a block
	 */
	fun setColor(color:IntArray) = block.forEachIndexed {i, it -> it.cint = color[i%color.size]}

	/** Sets all blocks to an item block
	 * @param item ID number of the item
	 */
	fun setItem(item:Int) = block.forEach {it.iNum = item}

	/** Sets the items of the blocks individually; allows one piece to have
	 * different item settings for each block
	 * @param item Array with each element specifying a color of a block
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
	 * @param darkness 暗さまたは明るさ (0.03だったら3%暗く, -0.05だったら5%明るい)
	 */
	fun setDarkness(darkness:Float) = block.forEach {it.darkness = darkness}

	/** すべてのBlockの透明度を変更
	 * @param alpha 透明度 (1fで不透明, 0fで完全に透明)
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
	 * @param offsetX X位置補正量の配列 (int[DIRECTION_COUNT])
	 * @param offsetY Y位置補正量の配列 (int[DIRECTION_COUNT])
	 */
	fun applyOffsetArray(offsetX:List<Int>, offsetY:List<Int>) {
		applyOffsetArrayX(offsetX)
		applyOffsetArrayY(offsetY)
	}

	/** 相対X位置をずらす
	 * @param offsetX X位置補正量の配列 (int[DIRECTION_COUNT])
	 */
	fun applyOffsetArrayX(offsetX:List<Int>) {
		for(i in 0..<DIRECTION_COUNT) dataOffsetX[i] += offsetX[i]
	}

	/** 相対Y位置をずらす
	 * @param offsetY Y位置補正量の配列 (int[DIRECTION_COUNT])
	 */
	fun applyOffsetArrayY(offsetY:List<Int>) {
		for(i in 0..<DIRECTION_COUNT) dataOffsetY[i] += offsetY[i]
	}

	/** 相対X位置と相対Y位置を初期状態に戻す */
	fun resetOffsetArray() {
		dataOffsetX.fill(0)
		dataOffsetY.fill(0)
		updateConnectData()
	}

	/** Blockの繋がり dataを更新 */
	fun updateConnectData(x:Int = 0, y:Int = 0, fld:Field? = null) {
		block.forEachIndexed {j, b ->

			b.setAttribute(
				false, Block.ATTRIBUTE.CONNECT_UP, Block.ATTRIBUTE.CONNECT_DOWN,
				Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_RIGHT
			)

			if(connectBlocks) {
				// 相対X位置と相対Y位置
				val (bx, by) = sp[direction][j]
				val fx = bx+x
				val fy = by+y
				b.setAttribute(false, Block.ATTRIBUTE.BROKEN)
				// 他の3つのBlockとの繋がりを調べる
				block.forEachIndexed {k, _ ->
					if(k!=j) {
						val (bx2, by2) = sp[direction][k]
						if(bx==bx2&&by-1==by2) b.setAttribute(true, Block.ATTRIBUTE.CONNECT_UP) // Up
						if(bx==bx2&&by+1==by2) b.setAttribute(true, Block.ATTRIBUTE.CONNECT_DOWN) // Down
						if(by==by2&&bx-1==bx2) b.setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT) // 左
						if(by==by2&&bx+1==bx2) b.setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT) // 右
					}
					if(!big) fld?.let {
						if(!it.getBlockEmpty(fx, fy-1)) b.setAttribute(true, Block.ATTRIBUTE.CONNECT_UP)
						if(!it.getBlockEmpty(fx, fy+1)) b.setAttribute(true, Block.ATTRIBUTE.CONNECT_DOWN)
						if(!it.getBlockEmpty(fx-1, fy)) b.setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT)
						if(!it.getBlockEmpty(fx+1, fy)) b.setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT)
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
	fun isPartialLockOut(x:Int, y:Int, rt:Int = direction):Boolean =
		// Bigでは専用処理
		(0..<maxBlock).any {i -> (y+dataY[rt][i]*if(big) 2 else 1)<0}
	/** 1つ以上Blockをfield枠内に置けるかどうか判定(fieldに変更は加えません)
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @return 1つ以上Blockをfield枠内に置けるならtrue, そうでないならfalse
	 */
	fun canPlaceToVisibleField(x:Int, y:Int, rt:Int = direction):Boolean =
		(0..<maxBlock).any {i -> (y+dataY[rt][i].let {if(big) it*2+1 else it})>=0}

	/** fieldにピースを置く
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @param fld field
	 * @return 1つ以上Blockをfield枠内に置けたらtrue, そうでないならfalse
	 */
	fun placeToField(x:Int, y:Int, rt:Int = direction, fld:Field?):Boolean {
		updateConnectData(x, y)

		//On a Big piece, double its size.
		val size = if(big) 2 else 1

		var placed = false

		fld?.setAllAttribute(false, Block.ATTRIBUTE.LAST_COMMIT)
		for(i in 0..<maxBlock) {
			val x2 = x+dataX[rt][i]*size //Multiply co-ordinate offset by piece size.
			val y2 = y+dataY[rt][i]*size

			block[i].setAttribute(true, Block.ATTRIBUTE.LAST_COMMIT)

			/* Loop through width/height of the block, setting cells in the field.
If the piece is normal (size == 1), a standard, 1x1 space is allotted per block.
If the piece is big (size == 2), a 2x2 space is allotted per block. */
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

					val res = fld?.setBlock(x3, y3, blk)==true
					if(y3>=0&&res) placed = true
				}
		}

		return placed
	}

	/** fieldにピースを置く 1つ以上Blockを[fld]枠内に置けたらtrue
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 */
	fun placeToField(x:Int, y:Int, fld:Field?):Boolean = placeToField(x, y, direction, fld)

	/** ピースの当たり判定 [fld]のBlockに重なっているとtrue
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 */
	fun checkCollision(x:Int, y:Int, fld:Field?):Boolean = checkCollision(x, y, direction, fld)

	/** ピースの当たり判定 [fld]のBlockに重なっているとtrue
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 */
	fun checkCollision(x:Int, y:Int, rt:Int = direction, fld:Field?):Boolean =
		fld?.let {
			// Bigでは専用処理
			if(big) checkCollisionBig(x, y, rt, it)
			else (0..<maxBlock).any {i ->
				val x2 = x+dataX[rt][i]
				val y2 = y+dataY[rt][i]
				!it.getCoordVaild(x2, y2, true)
			}
		}?:false

	/** ピースの当たり判定 (Big用)
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @param fld field
	 * @return Blockに重なっていたらtrue, 重なっていないならfalse
	 */
	private fun checkCollisionBig(x:Int, y:Int, rt:Int, fld:Field):Boolean {
		return (0..<maxBlock).any {i ->
			val x2 = x+dataX[rt][i]*2
			val y2 = y+dataY[rt][i]*2

			// 4Block分調べる
			mapOf(0 to 0, 0 to 1, 1 to 0, 1 to 1).any {(k, l) ->
				val x3 = x2+k
				val y3 = y2+l
				!fld.getCoordVaild(x3, y3, true)
			}
		}
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
		while(!checkCollision(x-1, nowY, rt, fld)) x--
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
		while(!checkCollision(x+1, nowY, rt, fld)) x++
		return x
	}

	/** spin buttonを押したあとのピースのDirectionを取得
	 * @param move spinDirection (-1:左 1:右 2:180度)
	 * @param dir 元のDirection
	 * @return spin buttonを押したあとのピースのDirection
	 */
	fun getSpinDirection(move:Int, dir:Int = direction):Int = (dir+move%DIRECTION_COUNT).let {
		if(it<0) it+DIRECTION_COUNT
		else if(it>=DIRECTION_COUNT) it-DIRECTION_COUNT
		else it
	}

	/** fieldに置いたPieceが屋根になるかを判定
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param fld field
	 * @return 置いたピースの下に空白があるとtrue
	 */
	fun canMakeRoof(x:Int, y:Int, fld:Field?):Boolean = fld?.let {
		val rt = direction
		sp[rt].groupBy({it.first}) {it.second}.map {(x, y) -> x to (y.maxOrNull()?:0)}
			.any {(px, py) ->
				val x2 = x+px
				val y2 = y+py
				it.getCoordVaild(x2, y2)&&!it.getBlockEmpty(x2, y2)&&
					(it.getCoordVaild(x2, y2+1)||it.getCoordAttribute(x2, y2+1)!=Field.Coord.VANISH)
					&&it.getBlockEmpty(x2, y2+1, false)
			}
	}?:false

	/** Pieceの上にブロックがあるかを判定
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param fld field
	 * @return 置いたピースの下に空白があるとtrue
	 */
	fun isUnderRoof(x:Int, y:Int, fld:Field?):Boolean = fld?.let {
		val rt = direction
		sp[rt].groupBy({it.first}) {it.second}.map {(x, y) -> x to (y.minOrNull()?:0)}
			.any {(px, py) ->
				val x2 = x+px
				val y2 = y+py
				(y2 downTo -it.hiddenHeight).indexOfFirst {yc -> !it.getBlockEmpty(x2, yc, true)}>=0
			}
	}?:false

	fun finesseLimit(nowPieceX:Int):Int =
		shape.finesse.let {it[direction%(it.size)]}.let {it[(nowPieceX+minimumBlockX).coerceIn(it.indices)]}

	companion object {

		const val PIECE_NONE = -1
		val PIECE_I = Shape.I.ordinal
		val PIECE_L = Shape.L.ordinal
		val PIECE_O = Shape.O.ordinal
		val PIECE_Z = Shape.Z.ordinal
		val PIECE_T = Shape.T.ordinal
		val PIECE_J = Shape.J.ordinal
		val PIECE_S = Shape.S.ordinal
		val PIECE_I1 = Shape.I1.ordinal
		val PIECE_I2 = Shape.I2.ordinal
		val PIECE_I3 = Shape.I3.ordinal
		val PIECE_L3 = Shape.L3.ordinal

		/** 通常のBlockピースのIDのMaximumcount */
		@Deprecated("This will be moved", ReplaceWith("Piece.Shape.numTetras"))
		val PIECE_STANDARD_COUNT = Shape.numTetras

		/** BlockピースのIDのMaximumcount */
		@Deprecated("This will be moved", ReplaceWith("Piece.Shape.num"))
		val PIECE_COUNT get() = Shape.num

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
		@Deprecated("This will be enumed", ReplaceWith("Piece.Shape.name"))
		fun getPieceName(id:Int):String = if(id>=0&&id<Shape.names.size) Shape.names[id] else "?"

		/** Returns true if enabled piece types are S,Z,O only.
		 * @param pieceEnable Piece enable flags
		 * @return `true` if enabled piece types are S,Z,O only.
		 */
		fun isPieceSZOOnly(pieceEnable:List<Boolean>?):Boolean =
			pieceEnable?.let {it[PIECE_S]&&it[PIECE_Z]&&it[PIECE_O]}?:false

		/** Create piece ID array from a String
		 * @param strSrc String
		 * @return Piece ID array
		 */
		fun createQueueFromIntStr(strSrc:String):List<Int> {
			val len = strSrc.length
			return if(len<1) emptyList() else List(len) {
				var pieceID = PIECE_I

				try {
					pieceID = maxOf(0, Character.getNumericValue(strSrc[it])%Shape.numTetras)
				} catch(_:NumberFormatException) {
				}
				pieceID
			}
		}

	}

	/** BlockピースのIDの定数 */
	enum class Shape(
		/** ピースの相対X位置と相対Y位置の配列 (4Direction×nBlock×<x,y>) */
		val pos:List<List<Pair<Int, Int>>>,
		/** ピースのfinesse limit (4Direction×width) */
		val finesse:List<List<Int>> = emptyList(),
		/** spin bonus判定位置 (4Direction×nBlock×<highX,highY> to <lowX, lowY>) */
		val spinBonus:List<Pair<List<Pair<Int, Int>>, List<Pair<Int, Int>>>> =
			List(DIRECTION_COUNT) {emptyList<Pair<Int, Int>>() to emptyList()}) {
		I(listOf(
			listOf(0 to 1, 1 to 1, 2 to 1, 3 to 1), listOf(2 to 0, 2 to 1, 2 to 2, 2 to 3),
			listOf(3 to 2, 2 to 2, 1 to 2, 0 to 2), listOf(1 to 3, 1 to 2, 1 to 1, 1 to 0)
		), listOf(listOf(1, 2, 1, 0, 1, 2, 1), listOf(2, 2, 2, 2, 1, 1, 2, 2, 2, 2)), listOf(
			listOf(1 to 0, 2 to 2, 2 to 0, 1 to 2) to listOf(-1 to 1, 4 to 1, -1 to 1, 4 to 1),
			listOf(1 to 1, 3 to 2, 1 to 2, 3 to 1) to listOf(2 to -1, 2 to 4, 2 to -1, 2 to 4),
			listOf(1 to 1, 2 to 3, 2 to 1, 1 to 3) to listOf(-1 to 2, 4 to 2, -1 to 2, 4 to 2),
			listOf(0 to 1, 2 to 2, 0 to 1, 2 to 2) to listOf(1 to -1, 1 to 4, 1 to -1, 1 to 4))),
		L(listOf(
			listOf(2 to 0, 2 to 1, 1 to 1, 0 to 1), listOf(2 to 2, 1 to 2, 1 to 1, 1 to 0),
			listOf(0 to 2, 0 to 1, 1 to 1, 2 to 1), listOf(0 to 0, 1 to 0, 1 to 1, 1 to 2)
		), listOf(
			listOf(1, 2, 1, 0, 1, 2, 2, 1), listOf(2, 2, 3, 2, 1, 2, 3, 3, 2),
			listOf(3, 4, 3, 2, 3, 4, 4, 3), listOf(2, 3, 2, 1, 2, 3, 3, 2, 2)
		), listOf(
			listOf(1 to 0, 0 to 0) to listOf(2 to 2, 0 to 2),
			listOf(1 to 0, 0 to 0) to listOf(2 to 0, 0 to 2),
			listOf(1 to 2, 2 to 2) to listOf(0 to 0, 2 to 0),
			listOf(0 to 0, 0 to 2) to listOf(2 to 3, 2 to 3))),
		O(listOf(
			listOf(0 to 0, 1 to 0, 1 to 1, 0 to 1), listOf(1 to 0, 1 to 1, 0 to 1, 0 to 0),
			listOf(1 to 1, 0 to 1, 0 to 0, 1 to 0), listOf(0 to 1, 0 to 0, 1 to 0, 1 to 1)
		), listOf(listOf(1, 2, 2, 1, 0, 1, 2, 2, 1))),
		Z(listOf(
			listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1), listOf(2 to 0, 2 to 1, 1 to 1, 1 to 2),
			listOf(2 to 2, 1 to 2, 1 to 1, 0 to 1), listOf(0 to 2, 0 to 1, 1 to 1, 1 to 0)
		), listOf(listOf(1, 2, 1, 0, 1, 2, 2, 1), listOf(2, 2, 2, 1, 1, 2, 3, 2, 2)), listOf(
			listOf(2 to 0, 0 to 1) to listOf(-1 to 0, 3 to 1),
			listOf(2 to 1, 1 to 2) to listOf(2 to -1, 1 to 3),
			listOf(0 to 2, 2 to 1) to listOf(3 to 2, -1 to 1),
			listOf(0 to 1, 1 to 0) to listOf(0 to 3, 1 to -1))),
		T(listOf(
			listOf(1 to 0, 0 to 1, 1 to 1, 2 to 1), listOf(2 to 1, 1 to 0, 1 to 1, 1 to 2),
			listOf(1 to 2, 2 to 1, 1 to 1, 0 to 1), listOf(0 to 1, 1 to 2, 1 to 1, 1 to 0)
		), listOf(
			listOf(1, 2, 1, 0, 1, 2, 2, 1), listOf(2, 2, 3, 2, 1, 2, 3, 3, 2),
			listOf(3, 4, 3, 2, 3, 4, 4, 3), listOf(2, 3, 2, 1, 2, 3, 3, 2, 2)
		), listOf(
			listOf(0 to 0, 2 to 2) to listOf(2 to 2, 0 to 2),
			listOf(2 to 2, 2 to 2) to listOf(0 to 2, 0 to 2),
			listOf(0 to 2, 2 to 2) to listOf(0 to 0, 0 to 2),
			listOf(0 to 0, 0 to 2) to listOf(2 to 2, 2 to 2))),
		J(listOf(
			listOf(0 to 0, 0 to 1, 1 to 1, 2 to 1), listOf(2 to 0, 1 to 0, 1 to 1, 1 to 2),
			listOf(2 to 2, 2 to 1, 1 to 1, 0 to 1), listOf(0 to 2, 1 to 2, 1 to 1, 1 to 0)
		), listOf(
			listOf(1, 2, 1, 0, 1, 2, 2, 1), listOf(2, 2, 3, 2, 1, 2, 3, 3, 2),
			listOf(3, 4, 3, 2, 3, 4, 4, 3), listOf(2, 3, 2, 1, 2, 3, 3, 2, 2)
		), listOf(
			listOf(1 to 2, 2 to 2) to listOf(0 to 2, 0 to 2),
			listOf(2 to 2, 2 to 2) to listOf(0 to 2, 0 to 2),
			listOf(1 to 0, 0 to 0) to listOf(2 to 0, 2 to 0),
			listOf(0 to 0, 0 to 0) to listOf(2 to 2, 2 to 2))),
		S(listOf(
			listOf(2 to 0, 1 to 0, 1 to 1, 0 to 1), listOf(2 to 2, 2 to 1, 1 to 1, 1 to 0),
			listOf(0 to 2, 1 to 2, 1 to 1, 2 to 1), listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2)
		), listOf(listOf(1, 2, 1, 0, 1, 2, 2, 1), listOf(2, 2, 2, 1, 1, 2, 3, 2, 2)), listOf(
			listOf(0 to 2, 1 to 2) to listOf(3 to 0, -1 to 1),
			listOf(1 to 2, 2 to 0) to listOf(1 to -1, 2 to 3),
			listOf(2 to 0, 1 to 0) to listOf(-1 to 2, 3 to 1),
			listOf(1 to 0, 0 to 0) to listOf(1 to 3, -1 to 0))),
		I1(listOf(listOf(0 to 0), listOf(0 to 0), listOf(0 to 0), listOf(0 to 0))),
		I2(listOf(listOf(0 to 0, 1 to 0), listOf(1 to 0, 1 to 1), listOf(1 to 1, 0 to 1), listOf(0 to 1, 0 to 0))),
		I3(listOf(
			listOf(0 to 1, 1 to 1, 2 to 1), listOf(1 to 0, 1 to 1, 1 to 2),
			listOf(2 to 1, 1 to 1, 0 to 1), listOf(1 to 2, 1 to 1, 1 to 0)
		)),
		L3(listOf(
			listOf(1 to 1, 0 to 1, 0 to 0), listOf(0 to 1, 0 to 0, 1 to 0),
			listOf(0 to 0, 1 to 0, 1 to 1), listOf(1 to 0, 1 to 1, 0 to 1)
		));

		companion object {
			val all get() = entries
			val num get() = all.size
			val Tetras
				get() = entries.filter {
					it.pos.all {a ->
						a.size==4&&a.any {(ax, ay) ->
							a.any {(bx, by) -> ax==bx&&(ay==by-1||ay==by+1)||ay==by&&(ax==bx-1||ax==bx+1)}
						}
					}
				}
			val numTetras get() = Tetras.size
			val names:List<String> get() = all.map {it.name}
			fun name(id:Int):String = all.getOrNull(id)?.name?:"?"
		}
	}
}
