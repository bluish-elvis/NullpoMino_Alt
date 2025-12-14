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

import mu.nu.nullpo.game.component.Block.ATTRIBUTE
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.LineGravity
import mu.nu.nullpo.game.play.clearRule.ClearType.ClearResult
import mu.nu.nullpo.game.play.clearRule.Line.checkLines
import mu.nu.nullpo.game.play.clearRule.Line.clearLines
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.aNum
import mu.nu.nullpo.util.GeneralUtil.toInt
import org.apache.logging.log4j.LogManager
import kotlin.math.abs
import kotlin.random.Random

/** Game Field */
@kotlinx.serialization.Serializable
class Field {
	/** パラメータ付きConstructor
	 * @param w fieldの幅
	 * @param h Field height
	 * @param hh fieldより上の見えない部分の高さ
	 * @param c 天井の有無
	 */
	@JvmOverloads
	constructor(w:Int = DEFAULT_WIDTH, h:Int = DEFAULT_HEIGHT, hh:Int = DEFAULT_HIDDEN_HEIGHT, c:Boolean = false) {
		width = w
		height = h
		hiddenHeight = hh
		ceiling = c
		reset()
	}

	/** Copy constructor
	 * @param f Copy source
	 */
	constructor(f:Field?) {
		replace(f)
	}

	/** fieldの幅 */
	var width = DEFAULT_WIDTH; private set

	/** Field height */
	var height = DEFAULT_HEIGHT; private set

	/** fieldより上の見えない部分の高さ */
	var hiddenHeight = DEFAULT_HIDDEN_HEIGHT; private set

	/** 天井の有無 */
	var ceiling = false

	/* Oct. 6th, 2010: Changed blockField[][] and blockHidden[][] to
	 * [row][column] format, and updated all relevant functions to match. This
	 * should facilitate referencing rows within the field. It appears the
	 * unfinished flipVertical() was written assuming this was possible, and it
	 * would greatly ease some work I'm currently doing without having
	 * any visible effects outside this function. -Kitaru */

	/** fieldのBlock */
	private var blockField:List<MutableList<Block?>> = List(height) {MutableList(width) {null}}

	/** field上の見えない部分のBlock */
	private var blockHidden:List<MutableList<Block?>> = List(hiddenHeight) {MutableList(width) {null}}

	/** Line clear flag */
	private var lineflagField = MutableList(height) {false}

	/** 見えない部分のLine clear flag */
	private var lineflagHidden = MutableList(hiddenHeight) {false}

	val lineFlags
		get() = (-hiddenHeight..<heightWithoutHurryupFloor).associate {
			it to getLineFlag(it)
		}

	/** HURRY UP地面のcount */
	var hurryUpFloorLines = 0; private set

	/** Number of total blocks exceeds required in last color clear */
	var colorClearExtraCount = 0

	/** Number of different colors in simultaneous color clears */
	var colorsCleared = 0; private set

	/** Number of gems cleared in last color or line color clear */
	var gemsCleared = 0; private set

	/** Number of garbage blocks cleared in last color clear */
	var garbageCleared = 0; private set

	/** List of colors of lines cleared in most recent line color clear */
	var lineColorsCleared = emptyList<Int>()

	/** List of last rows cleared in most recent horizontal line clear. */
	var lastClearResult = ClearResult(0,emptyMap())
	val lastLinesCleared get()=lastClearResult.blocksCleared
	val lastLinesY get() = lastClearResult.linesYfolded
	val lastLinesHeight get() = lastLinesY.map {it.size}

	val lastLinesTop get() = lastClearResult.linesY.minOrNull()?:height
	val lastLinesBottom get() = lastClearResult.linesY.maxOrNull()?:-hiddenHeight

	var lastLinesSplited = false; private set

	var lastExplodedBombs = false; private set

	/** Used for TGM garbage, can later be extended to all types */
	// public ArrayList<Block[]> pendingGarbage;

	/** BombとSparkの効果範囲 */
	var explodWidth = 0
	var explodHeight = 0

	/** Frozen / Line Lock */
	var lockedLines = emptySet<Int>()
	/** 消えるLines countを数える
	 * @return Lines count
	 */
	val lines get() = (-hiddenHeight..<heightWithoutHurryupFloor).count {getLineFlag(it)}

	/** All clearだったらtrue
	 * @return All clearだったらtrue
	 */
	val isEmpty get() = howManyBlocks==0

	/** field内に何個のBlockがあるか調べる
	 * @return field内にあるBlockのcount
	 */
	val howManyBlocks
		get() = (-hiddenHeight..<heightWithoutHurryupFloor).filter {!getLineFlag(it)}
			.sumOf {getRow(it).count {b -> b?.isEmpty==false}}

	/** 左から何個のBlockが並んでいるか調べる
	 * @return 左から並んでいるBlockの総count
	 */
	val howManyBlocksFromLeft
		get() = (-hiddenHeight..<heightWithoutHurryupFloor).filter {!getLineFlag(it)}
			.sumOf {getRow(it).takeWhile {b -> b?.isEmpty==false}.size}

	/** 右から何個のBlockが並んでいるか調べる
	 * @return 右から並んでいるBlockの総count
	 */
	val howManyBlocksFromRight:Int
		get() = (-hiddenHeight..<heightWithoutHurryupFloor).filter {!getLineFlag(it)}
			.sumOf {getRow(it).takeLastWhile {b -> b?.isEmpty==false}.size}

	/** 一番上にあるBlockのY-coordinateを取得
	 * @return 一番上にあるBlockのY-coordinate
	 */
	val highestBlockY:Int
		get() = (-hiddenHeight..<heightWithoutHurryupFloor).filter {!getLineFlag(it)}
			.firstOrNull {getRow(it).filterNotNull().any {b -> !b.isEmpty}}?:height

	/** garbage blockが最初に現れるY-coordinateを取得
	 * @return garbage blockが最初に現れるY-coordinate
	 */
	val highestGarbageBlockY:Int
		get() = (-hiddenHeight..<heightWithoutHurryupFloor).filter {!getLineFlag(it)}.firstOrNull {
			getRow(it).filterNotNull().any {b ->
				b.getAttribute(ATTRIBUTE.GARBAGE)
			}
		}?:height

	val howManyBlocksCovered:Int
		get() = (0..<width).sumOf {j ->
			(getHighestBlockY(j)..<heightWithoutHurryupFloor).filter {!getLineFlag(it)}.count {getBlockEmpty(j, it)}
		}

	val danger get() = (howManyBlocks+howManyLidAboveHoles)*4/(width*heightWithoutHurryupFloor)>=3
	val safety get() = (howManyBlocks+howManyLidAboveHoles)*20/(width*heightWithoutHurryupFloor)>=7

	/** field内の隙間のcountを調べる
	 * @return field内の隙間のcount
	 */
	val howManyHoles:Int
		get() {
			var hole = 0
			var samehole:Boolean

			for(j in 0..<width) {
				samehole = false

				for(i in highestBlockY..<heightWithoutHurryupFloor) if(!getLineFlag(i)) when {
					isHoleBelow(j, i) -> samehole = true
					samehole&&getBlockEmpty(j, i) -> hole++
					else -> samehole = false
				}
			}

			return hole
		}

	/** 隙間の上に何個Blockが積み重なっているか調べる
	 * @return 積み重なっているBlockのcount
	 */
	val howManyLidAboveHoles:Int
		get() {
			var blocks = 0

			for(j in 0..<width) {
				var count = 0

				for(i in highestBlockY..<heightWithoutHurryupFloor-1) if(!getLineFlag(i)) if(isHoleBelow(j, i)) {
					count++
					blocks += count
					count = 0
				} else if(!getBlockEmpty(j, i)) count++
			}

			return blocks
		}

	/** 全ての谷 (■ ■になっている地形）の深さを合計したものを返す (谷が多くて深いほど戻り値も大きくなる）
	 * @return 全ての谷の深さを合計したもの
	 */
	val totalValleyDepth:Int
		get() = (0..<width).sumOf {
			val d = getValleyDepth(it)
			if(d>=2) d else 0
		}

	/** I型が必要な谷 (深さ3以上）のcountを返す
	 * @return I型が必要な谷のcount
	 */
	val totalValleyNeedIPiece:Int
		get() = (0..<width).count {getValleyDepth(it)>=3}

	/** @return an ArrayList of rows representing the TGM attack of the last
	 * line
	 * clear action The TGM attack is the lines of the last line clear
	 * flipped vertically and without the blocks that caused it.
	 */
	// Put an empty block if the original block was in the last
	// commit to the field.
	val lastLinesAsTGMAttack
		get() = lastLinesCleared.map {(y,r) ->
			y to r.map {(x, b) -> x to if(b?.getAttribute(ATTRIBUTE.LAST_COMMIT)!=false) null else b}.toMap()
		}.toMap()

	/** 裏段位を取得 (from NullpoMino Unofficial Expansion build 091309)
	 * @return 裏段位
	 */
	val secretGrade:Int
		get() {
			var rows = 0
			var rowCheck:Boolean

			for(i in height-1 downTo 1) {
				val holeLoc = -abs(i-height/2)+height/2-1
				if(getBlockEmpty(holeLoc, i)&&!getBlockEmpty(holeLoc, i-1)) {
					rowCheck = true
					for(j in 0..<width) if(j!=holeLoc&&getBlockEmpty(j, i)) {
						rowCheck = false
						break
					}
					if(rowCheck) rows++
					else break
				} else break
			}

			return rows
		}

	/** 宝石Blockのcountを取得
	 * @return 宝石Blockのcount
	 */
	val howManyGems:Int
		get() = (-hiddenHeight..<heightWithoutHurryupFloor).sumOf {i -> getRow(i).filterNotNull().count {it.isGemBlock}}

	/** 宝石Blockがいくつ消えるか取得
	 * @return 消える宝石Blockのcount
	 */
	val howManyGemClears:Int
		get() = (-hiddenHeight..<heightWithoutHurryupFloor).filter {getLineFlag(it)}
			.sumOf {i -> getRow(i).filterNotNull().count {it.isGemBlock}}

	/** Checks for inum blocks cleared
	 * @return A boolean list with true at each index for which an inum block
	 * of the corresponding ID number was cleared
	 */
	val itemClears
		get() = List(Block.MAX_ITEM+1) {item ->
			(-hiddenHeight..<heightWithoutHurryupFloor).filter {getLineFlag(it)}.any {i -> getRow(i).any {b -> b?.iNum==item}}
		}

	/** Garbageを含むラインがいくつ消えるか取得
	 * @return 消去予定のGarbageを含むライン数
	 */
	// Check the lines we are clearing.
	val howManyGarbageLineClears:Int
		get() = (-hiddenHeight..<heightWithoutHurryupFloor).filter {getLineFlag(it)}.count {
			getRow(it).filterNotNull().any {b -> b.getAttribute(ATTRIBUTE.GARBAGE)}
		}

	/** Checks the lines that are currently being cleared to see how many strips of squares are present in them.
	 * @return +4 for every 1x4 strip of gold (index 0) or silver (index 1)
	 */
	// Check the lines we are clearing.
	// Silver blocks are worth 1, gold are worth 2,
	// but not if they are garbage (avalanche)
	// We have to divide the amount by 4 because it's based on 1x4 strips,
	// not single blocks.
	val howManySquareClears:List<Int>
		get() = findBlocks {
			!it.getAttribute(ATTRIBUTE.GARBAGE)&&(it.isGoldSquareBlock||it.isSilverSquareBlock)
		}.filter {(y, _) ->
			getLineFlag(y)
		}.values.let {y ->
			listOf(y.sumOf {x -> x.values.count {it.isGoldSquareBlock}}, y.sumOf {x -> x.values.count {it.isSilverSquareBlock}})
		}

	/** HURRY UPの地面を除いたField heightを返す
	 * @return HURRY UPの地面を除いたField height
	 */
	val heightWithoutHurryupFloor:Int
		get() = height-hurryUpFloorLines

	/** Called at initialization */
	fun reset() {
		blockField = List(height) {MutableList(width) {null}}
		blockHidden = List(hiddenHeight) {MutableList(width) {null}}
		lineflagField = MutableList(height) {false}
		lineflagHidden = MutableList(hiddenHeight) {false}
		hurryUpFloorLines = 0

		colorClearExtraCount = 0
		colorsCleared = 0
		gemsCleared = 0
		lastClearResult = ClearResult(0,emptyMap())

		lastLinesSplited = false
		explodWidth = 0
		explodHeight = 0
	}

	/** [f]から内容をコピー*/
	fun replace(f:Field?) {
		f?.let {o ->
			width = o.width
			height = o.height
			hiddenHeight = o.hiddenHeight
			ceiling = o.ceiling

			blockField = o.blockField.map {x -> x.map {y -> y?.let {Block(it)}}.toMutableList()}
			blockHidden = o.blockHidden.map {x -> x.map {y -> y?.let {Block(it)}}.toMutableList()}
			lineflagField = o.lineflagField.toMutableList()
			lineflagHidden = o.lineflagField.toMutableList()
			hurryUpFloorLines = o.hurryUpFloorLines

			colorClearExtraCount = o.colorClearExtraCount
			colorsCleared = o.colorsCleared
			o.gemsCleared = 0
			gemsCleared = o.gemsCleared
			lineColorsCleared = o.lineColorsCleared.toList()
			lastLinesSplited = o.lastLinesSplited

			explodHeight = o.explodHeight
			explodWidth = o.explodWidth
		}?:reset()
	}

	/** [x],[y]座標の属性を取得
	 * @return 座標の属性
	 */
	fun getCoordAttribute(x:Int, y:Int):Int = when {
		y<0&&ceiling -> COORD_WALL// 天井
		x<0||x>=width||y>=height -> COORD_WALL// 壁
		y>=0 -> COORD_NORMAL// 通常
		(y*-1-1)<hiddenHeight -> COORD_HIDDEN    // 見えない部分
		else -> COORD_VANISH// 置いたBlockが消える
	}

	/** [x],[y]座標にBlockが置けるかを取得
	 * @return Blockが置ける座標ならtrue
	 */
	fun getCoordVaild(x:Int, y:Int):Boolean = getCoordAttribute(x, y).let {it==COORD_NORMAL||it==COORD_HIDDEN}

	/** @param y height of the row in the field
	 * @return a reference to the row
	 */
	fun getRow(y:Int):List<Block?> = (if(height>0&&y>=0) blockField.getOrNull(y)
	else blockHidden.getOrNull(y*-1-1))?.map {if(it?.isEmpty!=false) null else it}?:List(10) {null}

	/** [x],[y]座標にあるBlockを取得
	 * @return 成功したら指定した座標にあるBlockオブジェクト, 失敗したらnull
	 */
	fun getBlock(x:Int, y:Int):Block? = getRow(y).getOrNull(x).let {if(it?.isEmpty!=false) null else it}

	/** Set block [blk] to [x],[y] location
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk Block
	 * @return true if successful, false if failed
	 */
	fun setBlock(x:Int, y:Int, blk:Block?):Boolean =
		if(blk?.color==null) delBlock(x, y)!=null else if(getCoordVaild(x, y)) try {
			if(y<0) {
				if((getBlock(x, y)?.replace(blk))==null) blockHidden[y*-1-1][x] = Block(blk)
			} else if((getBlock(x, y)?.replace(blk))==null) blockField[y][x] = Block(blk)
			true
		} catch(e:Throwable) {
			log.error("setBlock($x,$y)", e)
			false
		}
		else false

	/** Remove block from [x],[y] location
	 * @return Previous Block if successful, null if failed
	 */
	fun delBlock(x:Int, y:Int):Block? = if(getCoordVaild(x, y)) try {
		(getBlock(x, y))?.let {
			it.reset(true)
			if(y<0) blockHidden[y*-1-1][x] = null
			else blockField[y][x] = null
			it
		}
	} catch(e:Throwable) {
		log.error("delBlock($x,$y)", e)
		null
	}
	else null

	fun findBlocks(range:IntRange, cond:(b:Block)->Boolean):Map<Int, Map<Int, Block>> = (range).associateWith {y ->
		getRow(y).mapIndexedNotNull {x, b ->
			b?.let {if(cond(b)) x to it else null}
		}.associate {it}
	}

	fun findBlocks(inHide:Boolean = true, cond:(b:Block)->Boolean):Map<Int, Map<Int, Block>> =
		findBlocks((if(inHide) -1*hiddenHeight else 0)..<height, cond)
	/** Remove blocks from [pos] locations
	 * @param pos [X,y] coordinate-array
	 * @return Previous Block if successful, null if failed
	 */
	@JvmName("delBlocksi")
	fun delBlocks(pos:Map<Int, Collection<Int>>) =
		pos.map {(y, row) -> y to row.mapNotNull {x -> delBlock(x, y)?.let {x to it}}.associate {it}}.associate {it}

	fun delBlocks(blocks:Map<Int, Map<Int, Block>>) =
		blocks.map {(y, row) -> y to row.mapNotNull {(x, _) -> delBlock(x, y)?.let {x to it}}.associate {it}}.associate {it}
	/** [x],[y]座標にあるBlock colorを取得
	 * @param gemSame If true, a gem block will return the color of the
	 * corresponding normal block.
	 * @return 指定した座標にあるBlock cint (失敗したらBLOCK_COLOR_INVALID）
	 */
	fun getBlockColor(x:Int, y:Int, gemSame:Boolean = false):Int =
		if(getCoordVaild(x, y)) (getBlock(x, y)?.cint?:Block.COLOR_NONE).let {if(gemSame) Block.gemToNormalColor(it) else it}
		else Block.COLOR_INVALID

	fun getBlockColor(x:Int, y:Int):Block.COLOR? = if(getCoordVaild(x, y)) getBlock(x, y)?.color else null

	/** [x],[y]座標にあるBlock colorを[c]に変更
	 * @return true if successful, false if failed
	 */
	fun setBlockColor(x:Int, y:Int, c:Block.COLOR):Boolean =
		if(getCoordVaild(x, y)) if(getBlock(x, y)?.also {it.color = c}==null) setBlock(x, y, Block(c)) else true
		else false

	/** [x],[y]座標にあるBlock colorを[c]に変更
	 * @return true if successful, false if failed
	 */
	fun setBlockColor(x:Int, y:Int, c:Pair<Block.COLOR, Block.TYPE>):Boolean =
		if(getCoordVaild(x, y)) if(getBlock(x, y)?.also {it.color = c.first; it.type = c.second}==null) setBlock(x, y,
			Block(c.first, c.second)) else true
		else false
	/** [x],[y]座標にあるBlock colorを[c]に変更
	 * @return true if successful, false if failed
	 */
	fun setBlockColor(x:Int, y:Int, c:Int):Boolean =
		if(c<0) delBlock(x, y)!=null else if(getCoordVaild(x, y)) if(getBlock(x, y)?.also {it.cint = c}==null&&c>=1) setBlock(
			x, y, Block(c)) else true
		else false

	/*fun howManyColor(c:Block.COLOR):Int =
		blockField.sum{it.count{it.color==c}}

	fun howManyColor(c:Block.COLOR,t:Block.TYPE):Int =
		(blockField+blockHidden).sum{it.count{it.color==c && it.type==t}}
*/
	/** [y]座標のLine clear flagを取得
	 * @param y Y-coordinate
	 * @return 消える列ならtrue, そうでないなら (もしくは座標が範囲外なら）false
	 */
	fun getLineFlag(y:Int):Boolean = (if(y>=0) lineflagField.getOrNull(y)
	else lineflagHidden.getOrNull(y*-1-1))?:false

	/** [x],[y]座標にあるBlockが空白かどうか判定
	 * @oaram ob 指定した座標が範囲外の場合の結果
	 */
	fun getBlockEmpty(x:Int, y:Int, ob:Boolean = true):Boolean = if(getCoordVaild(x, y)) getBlock(x, y)?.isEmpty?:true
	else ob

	/** [x],[y]座標にあるBlockが空白かどうか判定 (指定した座標が範囲外の場合はfalse）
	 */
	@Deprecated("overloaded", ReplaceWith("getBlockEmpty(x,y,false)"))
	fun getBlockEmptyF(x:Int, y:Int):Boolean = getBlockEmpty(x, y, false)

	/** [y]座標のLine clear flagを[flag]に設定
	 * @return true if successful, false if failed
	 */
	fun setLineFlag(y:Int, flag:Boolean):Boolean = try {
		if(y>=0)// field内
			lineflagField[y] = flag
		else lineflagHidden[y*-1-1] = flag
		true
	} catch(e:ArrayIndexOutOfBoundsException) {
		false
	}

	/** Line clear check
	 * @param flag Will mark flag the lines for clear process
	 * @return 消えるLines count
	 */
	fun checkLine(flag:Boolean = true) = checkLines(flag).size

	/** Line clear check (消去 flagの設定とかはしない）
	 * @return 消えるLines count
	 */
	@Deprecated("renamed", ReplaceWith("checkLine(false)"))
	fun checkLineNoFlag():Int = checkLine(false)

	/** Linesを消す
	 * @return 消えたLines count
	 */
	fun clearLine():Int = clearLines().size

	/** 上にあったBlockをすべて下まで下ろす
	 * @return 消えていたLines count
	 */
	fun downFloatingBlocks() = LineGravity.Native.fallInstant(this)

	/** 上にあったBlockを1段だけ下ろす */
	fun downFloatingBlocksSingleLine() = LineGravity.Native.fallSingle(this)

	/** Check if specified line is completely empty
	 * @param y Y coord
	 * @return `true` if the specified line is completely empty,
	 * `false` otherwise.
	 * @see mu.nu.nullpo.game.play.clearRule.isEmptyLine
	 */
	fun isEmptyLine(y:Int):Boolean = (0..<width).all {getBlockEmpty(it, y)}

	@Deprecated("renamed", ReplaceWith("isTwistSpot(x, y, big)"))
	fun isTSpinSpot(x:Int, y:Int, big:Boolean):Boolean = isTwistSpot(x, y, big)
	/** Twisterになる地形だったらtrue
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param big Bigかどうか
	 * @return Twisterになる地形だったらtrue
	 */
	fun isTwistSpot(x:Int, y:Int, big:Boolean):Boolean =
		big.toInt().let {b -> tx[b].indices.count {(getBlock(x+tx[b][it], y+ty[b][it])!=null)}>=3}

	/** Twisterできそうな穴だったらtrue
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param big Bigかどうか
	 * @return Twisterできそうな穴だったらtrue
	 */
	fun isTSlot(x:Int, y:Int, big:Boolean):Boolean {
		// 中央が埋まってると無理
		if(big) {
			if(!getBlockEmpty(x+2, y+2, false)) return false
		} else {
			// □□□※※※□□□□
			// □□□★※□□□□□
			// □□□※※※□□□□
			// □□□○※○□□□□

			if(!getBlockEmpty(x+1, y, false)) return false
			if(!getBlockEmpty(x+1, y+1, false)) return false
			if(!getBlockEmpty(x+1, y+2, false)) return false

			if(!getBlockEmpty(x, y+1, false)) return false
			if(!getBlockEmpty(x+2, y+1, false)) return false

			if(!getBlockEmpty(x+1, y-1, false)) return false
		}

		// 判定用相対座標を設定
		val tx = tx[big.toInt()]
		val ty = ty[big.toInt()]
		// 判定
		return tx.indices.count {(getBlock(x+tx[it], y+ty[it])!=null)}==3
	}

	/** Twisterできそうな穴が何個あるか調べる
	 * @param big Bigだったらtrue
	 * @return Twisterできそうな穴のcount
	 */
	fun getHowManyTSlot(big:Boolean):Int = (0..<width).sumOf {j ->
		(0..<heightWithoutHurryupFloor-2).count {i ->
			!getLineFlag(i)&&isTSlot(j, i, big)
		}
	}

	/** [x],[y]座標でのTwisterで消えるLines countを返す
	 * @param big Bigかどうか(未対応)
	 * @return Twisterで消えるLines count(Twisterじゃない場合などは0)
	 */
	fun getTSlotLineClear(x:Int, y:Int, big:Boolean):Int = if(!isTSlot(x, y, big)) 0 else (0..2).count {i ->
		!(0..<width).any {j ->
			// ■■■★※■■■■■
			// □□□※※※□□□□
			// □□□○※○□□□□
			(j !in x..x+2||(i!=1&&j!=x+1))&&getBlockEmpty(j, y+i, false)
		}
	}

	/** Twisterで消えるLines countを返す(field全体)
	 * @param big Bigかどうか(未対応)
	 * @param minimum 最低Lines count(2にするとTwister Doubleにだけ反応)
	 * @return Twisterで消えるLines count(Twisterじゃない場合やminimumに満たないLinesなどは0)
	 */
	fun getTSlotLineClearAll(big:Boolean, minimum:Int = 0):Int = (0..<width).sumOf {j ->
		(0..<heightWithoutHurryupFloor-2).filter {
			getLineFlag(it)&&getTSlotLineClear(j, it, big)>=minimum
		}.sumOf {getTSlotLineClear(j, it, big)}
	}

	/** [x]列にて一番上のBlockがあるY座標を取得 */
	fun getHighestBlockY(x:Int):Int =
		(-hiddenHeight..<heightWithoutHurryupFloor).firstOrNull {!getLineFlag(it)&&!getBlockEmpty(x, it)}?:height

	/** [x],[y]座標の下に隙間があるか調べる
	 * @return 指定した座標にブロックがあり、かつ下に隙間があればtrue
	 */
	fun isHoleBelow(x:Int, y:Int) = !getBlockEmpty(x, y)&&getBlockEmpty(x, y+1, false)

	/** [x],[y] 座標の下が何マス空いているか
	 * @return 指定した座標にブロックがある場合、その下に連続した空白の数
	 * 指定座標が空であれば、その空白の高さ
	 */
	fun getHoleBelow(x:Int, y:Int):Int =
		(if(getBlockEmpty(x, y)) ((y downTo -hiddenHeight).firstOrNull {!getBlockEmpty(x, it)}?:y)+1
		else y+1).let {startY ->
			(startY..<heightWithoutHurryupFloor).takeWhile {getBlockEmpty(x, it, false)}.size
		}

	/** 谷 (■ ■になっている地形）の深さを調べる
	 * @param x 調べるX-coordinate
	 * @return 谷の深さ (無かったら0）
	 */
	fun getValleyDepth(x:Int):Int = minOf(getHighestBlockY(x-1), getHighestBlockY(x), getHighestBlockY(x+1)).let {startY ->
		(startY..<heightWithoutHurryupFloor).takeWhile {
			!getBlockEmpty(x-1, it, false)&&getBlockEmpty(x, it, false)&&!getBlockEmpty(x+1, it, false)
		}.size
	}

	/** field全体を上にずらす
	 * @param lines ずらす段count
	 */
	fun pushUp(lines:Int = 1) {
		for(k in 0..<lines) {
			for(i in -hiddenHeight..<heightWithoutHurryupFloor-1)
			// Blockを1段下からコピー
				for(j in 0..<width) {
					setBlock(j, i, getBlock(j, i+1))
					setLineFlag(i, getLineFlag(i+1))
				}

			// 一番下を空白にする
			for(j in 0..<width) {
				val y = heightWithoutHurryupFloor-1
				delBlock(j, y)
				setLineFlag(y, false)
			}
		}
	}

	/** field全体を下にずらす
	 * @param lines ずらす段count
	 */
	fun pushDown(lines:Int = 1) {
		for(k in 0..<lines) {
			for(i in heightWithoutHurryupFloor-1 downTo 1-hiddenHeight)
			// Blockを1段上からコピー
				for(j in 0..<width) {
					setBlock(j, i, getBlock(j, i-1))
					setLineFlag(i, getLineFlag(i+1))
				}

			// 一番上を空白にする
			for(j in 0..<width) {
				delBlock(j, -hiddenHeight)
				setLineFlag(-hiddenHeight, false)
			}
		}
	}

	/** Cut the specified line(s) then push down all things above
	 * @param y Y coord
	 * @param lines Number of lines to cut
	 */
	fun cutLine(y:Int, lines:Int) {
		for(k in 0..<lines) {
			for(i in y downTo 1-hiddenHeight) {
				for(j in 0..<width) setBlock(j, i, getBlock(j, i-1))
				setLineFlag(i, getLineFlag(i+1))
			}

			for(j in 0..<width) {
				delBlock(j, -hiddenHeight)
				setLineFlag(-hiddenHeight, false)
			}
		}
	}

	fun setSingleHoleLine(hole:Int, y:Int, color:Block.COLOR, skin:Int) {
		for(j in 0..<width) if(j!=hole) setBlock(j, y,
			Block(color, skin, ATTRIBUTE.VISIBLE, ATTRIBUTE.OUTLINE, ATTRIBUTE.GARBAGE,
				if(j<hole) ATTRIBUTE.CONNECT_LEFT else ATTRIBUTE.CONNECT_RIGHT))
	}
	/** Add a single hole garbage (Attributes are automatically set)
	 * @param hole Hole position
	 * @param color Color
	 * @param skin Skin
	 * @param lines Number of garbage lines to add
	 */
	fun addSingleHoleGarbage(hole:Int, color:Block.COLOR, skin:Int, lines:Int) {
		for(it in 0..<lines) {
			pushUp(1)
			setSingleHoleLine(hole, heightWithoutHurryupFloor-1, color, skin)
		}
	}
	/** Add a single hole garbage (Attributes are automatically set)
	 * @param hole Hole position before add lines
	 * @param messiness Changing Rate of Hole position
	 * @param color Color
	 * @param skin Skin
	 * @param lines Number of garbage lines to add
	 * @return last hole position
	 */
	fun addRandomHoleGarbage(engine:GameEngine, hole:Int, messiness:Float, color:Block.COLOR, skin:Int, lines:Int):Int {
		var x = hole
		for(i in 0..<lines) {
			if(i>0) {
				val rand = engine.random.nextFloat()
				if(rand<messiness) {
					val newHole = (rand*width).toInt()
					x = newHole+(newHole>=x).toInt()
				}
			}
			pushUp(1)
			setSingleHoleLine(x, heightWithoutHurryupFloor-1, color, skin)
		}
		return x
	}

	/** 一番下のLinesの形をコピーしたgarbage blockを一番下に追加
	 * @param skin garbage blockの絵柄
	 * @param attrs garbage blockの属性
	 * @param lines 追加するgarbage blockのLines count
	 */
	fun addBottomCopyGarbage(skin:Int, lines:Int, vararg attrs:ATTRIBUTE) {
		for(k in 0..<lines) {
			pushUp(1)

			for(j in 0..<width) if(!getBlockEmpty(j, height-2)) {
				setBlock(j, heightWithoutHurryupFloor-1, Block(Block.COLOR.WHITE, skin, *attrs))
			}
		}
	}

	fun filterAttributeMap(vararg attr:ATTRIBUTE):Map<Int, Map<Int, Block>> =
		(-hiddenHeight..<heightWithoutHurryupFloor).associateWith {y ->
			(getRow(y).mapIndexedNotNull {x, it -> if(it?.getAttribute(*attr)==true) x to it else null}.toMap())
		}.filter {it.value.isNotEmpty()}

	fun filterAttributeIList(vararg attr:ATTRIBUTE):Iterable<IndexedValue<List<Block?>>> =
		(-hiddenHeight..<heightWithoutHurryupFloor).map {i ->
			IndexedValue(i, getRow(i).map {if(it?.getAttribute(*attr)==true) it else null})
		}.filter {it.value.isNotEmpty()}

	fun filterAttributeBlocks(vararg attr:ATTRIBUTE):Set<Triple<Int, Int, Block>> =
		(-hiddenHeight..<heightWithoutHurryupFloor).flatMap {y ->
			(0..<width).mapNotNull {x ->
				getBlock(x, y)?.let {if(it.getAttribute(*attr)) Triple(x, y, it) else null}
			}
		}.toSet()

	/** 全てのBlockの属性を変更
	 * @param attr 変更したい属性
	 * @param status 変更後 state
	 */
	fun setAllAttribute(status:Boolean, vararg attr:ATTRIBUTE) {
		for(i in -hiddenHeight..<height) for(j in 0..<width) getBlock(j, i)?.setAttribute(status, *attr)

	}

	/** 全てのBlockの絵柄を変更
	 * @param skin 絵柄
	 */
	fun setAllSkin(skin:Int) {
		for(i in -hiddenHeight..<height) for(j in 0..<width) getBlock(j, i)?.skin = skin
	}

	/** Checks for 2x2 square formations of gem and
	 * converts blocks to big bomb blocks if needed.
	 * @return Number of big bomb formations
	 */
	fun checkForBigBomb():Int {
		// Check for big bomb
		for(i in -hiddenHeight..<heightWithoutHurryupFloor-1) for(j in 0..<width-1) {
			// rootBlk is the upper-left square
			getBlock(j, i)?.let {rootBlk ->
				var squareCheck = false          /* id is the cint of the top-left square: if it is a
					 * monosquare, every block in the 4x4 area will have this
					 * cint. */
				if(rootBlk.isGemBlock) {
					squareCheck = true
					for(k in 1..2) {
						val blk = getBlock(j+1, i)
						if(blk==null||blk.isEmpty||!blk.isGemBlock||blk.cint==Block.COLOR_GEM_RAINBOW) {
							squareCheck = false
							break
						}
					}
				}
				if(squareCheck) {
					// TODO: Gain Big Bomb
				}
			}
		}

		return 0
	}

	/** Checks for 4x4 square formations and converts blocks to square blocks if
	 * needed.
	 * @return Number of square formations (index 0 is gold, index 1 is
	 * silver)
	 */
	fun checkForSquares():IntArray {
		val squares = intArrayOf(0, 0)

		// Check for gold squares
		for(i in -hiddenHeight..<heightWithoutHurryupFloor-3) for(j in 0..<width-3) {
			// rootBlk is the upper-left square
			val rootBlk = getBlock(j, i)
			var squareCheck = false

			/* id is the cint of the top-left square:
			 if it is a monosquare, every block in the 4x4 area will have this color. */
			var id = Block.COLOR_NONE
			if(!(rootBlk==null||rootBlk.isEmpty)) id = rootBlk.cint

			// This can't be a square if rootBlk doesn't exist or is part of
			// another square.
			if(!(rootBlk==null||rootBlk.isEmpty||rootBlk.isGoldSquareBlock||rootBlk.isSilverSquareBlock)) {
				// A square is innocent until proven guilty.
				squareCheck = true
				for(k in 0..3) {
					for(l in 0..3) {
						// blk is the current block
						val blk = getBlock(j+l, i+k)
						if(blk==null||blk.isEmpty||blk.isGoldSquareBlock||blk.isSilverSquareBlock||
							blk.getAttribute(ATTRIBUTE.BROKEN)||blk.getAttribute(ATTRIBUTE.GARBAGE)||blk.cint!=id||
							l==0&&blk.getAttribute(ATTRIBUTE.CONNECT_LEFT)||l==3&&blk.getAttribute(ATTRIBUTE.CONNECT_RIGHT)||
							k==0&&blk.getAttribute(ATTRIBUTE.CONNECT_UP)||k==3&&blk.getAttribute(ATTRIBUTE.CONNECT_DOWN)) {
							/* Reasons why the entire area would not be a monosquare:
							 this block does not exist, it is part of another square,
							 it has been broken by line clears, is a garbage block,
								is not the same color as id, or has connections outside the area. */
							squareCheck = false
							break
						}
					}
					if(!squareCheck) break
				}
			}
			// We found a square! Set all the blocks equal to gold blocks.
			if(squareCheck) {
				squares[0]++
				for(k in 0..3) for(l in 0..3) getBlock(j+l, i+k)?.apply {
					setAttribute(true, ATTRIBUTE.SQUARE_GOLD)
					// For stylistic concerns, we attach all blocks in the square together.
					if(k>0) setAttribute(true, ATTRIBUTE.CONNECT_UP)
					if(k<3) setAttribute(true, ATTRIBUTE.CONNECT_DOWN)
					if(l>0) setAttribute(true, ATTRIBUTE.CONNECT_LEFT)
					if(l<3) setAttribute(true, ATTRIBUTE.CONNECT_RIGHT)
				}
			}
		}
		// Check for silver squares
		for(i in -hiddenHeight..<heightWithoutHurryupFloor-3) for(j in 0..<width-3) {
			val rootBlk = getBlock(j, i)
			var squareCheck = false
			// We don't have to check colors because this loop checks for
			// multisquares.
			if(!(rootBlk==null||rootBlk.isEmpty||rootBlk.isGoldSquareBlock||rootBlk.isSilverSquareBlock)) {
				// A square is innocent until proven guilty
				squareCheck = true
				for(k in 0..3) {
					for(l in 0..3) {
						val blk = getBlock(j+l, i+k)
						// See above, but without the color checking.
						if(blk==null||blk.isEmpty||blk.isGoldSquareBlock||blk.isSilverSquareBlock||
							blk.getAttribute(ATTRIBUTE.BROKEN)||blk.getAttribute(ATTRIBUTE.GARBAGE)||
							l==0&&blk.getAttribute(ATTRIBUTE.CONNECT_LEFT)||l==3&&blk.getAttribute(ATTRIBUTE.CONNECT_RIGHT)||
							k==0&&blk.getAttribute(ATTRIBUTE.CONNECT_UP)||k==3&&blk.getAttribute(ATTRIBUTE.CONNECT_DOWN)) {
							squareCheck = false
							break
						}
					}
					if(!squareCheck) break
				}
			}
			// We found a square! Set all the blocks equal to silver blocks.
			if(squareCheck) {
				squares[1]++
				for(k in 0..3) for(l in 0..3) getBlock(j+l, i+k)?.apply {
					setAttribute(true, ATTRIBUTE.SQUARE_SILVER)
					if(k>0) setAttribute(true, ATTRIBUTE.CONNECT_UP)
					if(k<3) setAttribute(true, ATTRIBUTE.CONNECT_DOWN)
					if(l>0) setAttribute(true, ATTRIBUTE.CONNECT_LEFT)
					if(l<3) setAttribute(true, ATTRIBUTE.CONNECT_RIGHT)
				}
			}
		}

		return squares
	}
//-----------------------------------

	/** Clears all blocks of the same color
	 * @param targetColor The color to clear
	 * @param flag `true` to set Block.ATTRIBUTE.ERASE on cleared blocks,
	 * `false` to delete the block immediately
	 * @param gemSame `true` to check gem blocks or only normal block.
	 * @return The number of blocks cleared.
	 */
	fun allClearColor(targetColor:Block.COLOR, flag:Boolean, gemSame:Boolean):Int {
		if(targetColor==Block.COLOR.RAINBOW) return 0
		var total = 0
		for(y in -1*hiddenHeight..<height) for(x in 0..<width) getBlock(x, y)?.let {
			if(it.color==targetColor&&gemSame||it.type==Block.TYPE.BLOCK) {
				total++
				if(flag) it.setAttribute(true, ATTRIBUTE.ERASE)
				else delBlock(x, y)
			}
		}
		return total
	}

	fun doCascadeGravity(type:LineGravity):Boolean {
		setAllAttribute(false, ATTRIBUTE.LAST_COMMIT)
		return when(type) {
			LineGravity.CASCADE_SLOW -> doCascadeSlow()
			LineGravity.CASCADE -> doCascadeGravity()
			else -> false
		}
	}

	/**
	 * Main routine for cascade gravity.
	 * @return `true` if something falls. `false` if nothing falls.
	 */
	private fun doCascadeGravity():Boolean = LineGravity.CASCADE.fallInstant(this)>0
	/**
	 * Routine for cascade gravity which checks from the top down for a slower fall animation.
	 * @return `true` if something falls. `false` if nothing falls.
	 */
	private fun doCascadeSlow():Boolean = LineGravity.CASCADE.fallSingle(this)>0

	/** Checks the connection of blocks and set "mark" to each block.
	 * @param x X coord
	 * @param y Y coord
	 */
	internal fun checkBlockLink(x:Int, y:Int) {
		setAllAttribute(false, ATTRIBUTE.TEMP_MARK)
		fun checkBlockLinkSub(x:Int, y:Int) {
			getBlock(x, y)?.also {
				if(!it.isEmpty&&!it.getAttribute(ATTRIBUTE.TEMP_MARK)) {
					it.setAttribute(true, ATTRIBUTE.TEMP_MARK)

					if(!it.getAttribute(ATTRIBUTE.IGNORE_LINK)) {
						if(it.getAttribute(ATTRIBUTE.CONNECT_UP)) checkBlockLinkSub(x, y-1)
						if(it.getAttribute(ATTRIBUTE.CONNECT_DOWN)) checkBlockLinkSub(x, y+1)
						if(it.getAttribute(ATTRIBUTE.CONNECT_LEFT)) checkBlockLinkSub(x-1, y)
						if(it.getAttribute(ATTRIBUTE.CONNECT_RIGHT)) checkBlockLinkSub(x+1, y)
					}
				}
			}
		}
		checkBlockLinkSub(x, y)
	}

	/** Checks the connection of blocks and set the "broken" flag to each block.
	 * It only affects to normal blocks. (ex. not square or gems)
	 * @param x X coord
	 * @param y Y coord
	 */
	fun setBlockLinkBroken(x:Int, y:Int) {
		setAllAttribute(false, ATTRIBUTE.TEMP_MARK)
		fun setBlockLinkBrokenSub(x:Int, y:Int) {
			getBlock(x, y)?.run {
				if(!isEmpty&&!getAttribute(ATTRIBUTE.TEMP_MARK)&&isNormalBlock) {
					setAttribute(true, ATTRIBUTE.TEMP_MARK)
					setAttribute(true, ATTRIBUTE.BROKEN)
					if(getAttribute(ATTRIBUTE.CONNECT_UP)) setBlockLinkBrokenSub(x, y-1)
					if(getAttribute(ATTRIBUTE.CONNECT_DOWN)) setBlockLinkBrokenSub(x, y+1)
					if(getAttribute(ATTRIBUTE.CONNECT_LEFT)) setBlockLinkBrokenSub(x-1, y)
					if(getAttribute(ATTRIBUTE.CONNECT_RIGHT)) setBlockLinkBrokenSub(x+1, y)
				}
			}
		}

		setBlockLinkBrokenSub(x, y)
	}

	/** Checks the color of all blocks in this Field and set the connection flags to each block. */
	fun setBlockLinkByColor() {
		setAllAttribute(false, ATTRIBUTE.TEMP_MARK)
		for(i in -hiddenHeight..<heightWithoutHurryupFloor)
			for(j in (0..<width).filter {getBlock(it, i)?.getAttribute(ATTRIBUTE.TEMP_MARK)==false})
			// まだTEMP_MARKがついていないBlockに対して
			// 色を見て接続フラグを設定
				setBlockLinkByColor(j, i)
	}

	/** Checks the color of block on [x],[y] position and set the connection flags to each block. */
	fun setBlockLinkByColor(x:Int, y:Int) {
		fun setBlockLinkByColorSub(x:Int, y:Int) {
			getBlock(x, y)?.run {
				if(!isEmpty&&!getAttribute(ATTRIBUTE.TEMP_MARK)&&!getAttribute(ATTRIBUTE.GARBAGE)&&isNormalBlock) {
					setAttribute(true, ATTRIBUTE.TEMP_MARK)
					if(getBlockColor(x, y-1)==color) {
						setAttribute(true, ATTRIBUTE.CONNECT_UP)
						setBlockLinkByColorSub(x, y-1)
					} else setAttribute(false, ATTRIBUTE.CONNECT_UP)
					if(getBlockColor(x, y+1)==color) {
						setAttribute(true, ATTRIBUTE.CONNECT_DOWN)
						setBlockLinkByColorSub(x, y+1)
					} else setAttribute(false, ATTRIBUTE.CONNECT_DOWN)
					if(getBlockColor(x-1, y)==color) {
						setAttribute(true, ATTRIBUTE.CONNECT_LEFT)
						setBlockLinkByColorSub(x-1, y)
					} else setAttribute(false, ATTRIBUTE.CONNECT_LEFT)
					if(getBlockColor(x+1, y)==color) {
						setAttribute(true, ATTRIBUTE.CONNECT_RIGHT)
						setBlockLinkByColorSub(x+1, y)
					} else setAttribute(false, ATTRIBUTE.CONNECT_RIGHT)
				}
			}
		}

		setBlockLinkByColorSub(x, y)
	}

	/** HURRY UPの地面を一番下に追加
	 * @param lines 上げるLines count
	 * @param skin 地面の絵柄
	 */
	fun addHurryUpFloor(lines:Int, skin:Int) {
		if(lines>0) for(k in 0..<lines) {
			pushUp(1)

			for(j in 0..<width) {
				val blk = Block(Block.COLOR.BLACK, Block.TYPE.BLOCK, skin, ATTRIBUTE.WALL, ATTRIBUTE.GARBAGE, ATTRIBUTE.VISIBLE)
				setBlock(j, heightWithoutHurryupFloor-1, blk)
			}

			hurryUpFloorLines++
		}
		else if(lines<0) {
			val l = minOf(lines, hurryUpFloorLines)
			hurryUpFloorLines -= l
			cutLine(height-1, l)
		}
	}

	/** プロパティセット[p]:[id].field.mapに保存 マップはcsvとして格納される
	 */
	fun writeProperty(p:CustomProperties, id:Int) {
		for(i in 0..<height) {
			val mapStr = StringBuilder()

			for(j in 0..<width) {
				mapStr.append("${getBlock(j, i)?.toChar()?:'_'}")
				if(j<width-1) mapStr.append(",")
			}

			p.setProperty("$id.field.map.$i", "$mapStr")
		}
	}

	/** プロパティセット[p]:[id].field.mapからcsvマップを読み込み*/
	fun readProperty(p:CustomProperties, id:Int) {
		for(i in 0..<height) {
			val mapStr = p.getProperty("$id.field.map.$i", p.getProperty("$id.field.values.$i", ""))
			val mapArray = mapStr.split(Regex(",")).dropLastWhile {it.isEmpty()}

			for(j in mapArray.indices) {
				val blkColor = try {
					mapArray[j].toInt()
				} catch(_:NumberFormatException) {
					mapArray[j][0].aNum
				}
				setBlock(j, i, Block(blkColor).apply {elapsedFrames = -1})
			}
			setBlockLinkByColor()
		}
	}

	/** @param row Row of blocks
	 * @return a String representing the row
	 */
	fun rowToString(row:List<Block?>?):String {
		val strResult = StringBuilder()

		row?.forEach {strResult.append(it?:" ")}

		return "$strResult"
	}

	/** fieldを文字列に変換
	 * @return 文字列に変換されたfield
	 */
	fun fieldToString():String {
		var strResult = StringBuilder()

		for(i in height-1 downTo maxOf(-1, highestBlockY)) strResult.append(rowToString(getRow(i)))

		// 終わりの0を取り除く
		while("$strResult".endsWith("0")) strResult = StringBuilder(strResult.substring(0, strResult.length-1))

		return "$strResult"
	}

	/** @param str String representing field state
	 * @param skin Block skin being used in this field
	 * @param isGarbage Row is garbage row
	 * @param isWall Row is a wall (i.e. hurry-up rows)
	 * @return The row array
	 */
	@JvmOverloads
	fun stringToRow(str:String, skin:Int = 0, isGarbage:Boolean = false, isWall:Boolean = false):Array<Block?> =
		(0..<minOf(width, str.length)).map {j ->
			/* NullNoname's original approach from the old stringToField:
				 If a character outside the row string is referenced, default to an empty block by ignoring the exception. */
			try {
				Block(str[j].aNum, skin).apply {
					elapsedFrames = -1
					setAttribute(true, ATTRIBUTE.VISIBLE)
					setAttribute(true, ATTRIBUTE.OUTLINE)

					if(isGarbage)
					// not only sport one hole (i.e. TGM garbage)
						setAttribute(true, ATTRIBUTE.GARBAGE)
					if(isWall) setAttribute(true, ATTRIBUTE.WALL)
				}
			} catch(e:Exception) {
				log.warn(e)
				null
			}
		}.toTypedArray()

	/** 文字列を元にfieldを変更
	 * @param str 文字列
	 * @param skin Blockの絵柄
	 * @param highestGarbageY 最も高いgarbage blockの位置
	 * @param highestWallY 最も高いHurryupBlockの位置
	 */
	@JvmOverloads
	fun stringToField(str:String, skin:Int = 0, highestGarbageY:Int = Integer.MAX_VALUE,
		highestWallY:Int = Integer.MAX_VALUE) {
		for(i in -1..<height) {
			val index = (height-1-i)*width
			/* Much like NullNoname's try/catch from the old stringToField that is now in stringToRow,
			 we need to skip over substrings referenced outside the field string -- empty rows. */
			try {
				val substr = str.substring(index, minOf(str.length, index+width))
//				log.debug("Field Row $i: $substr ${substr.map {it.aNum}.joinToString()}")
				val row = stringToRow(substr, skin, i>=highestGarbageY, i>=highestWallY)
				for(j in 0..<width) setBlock(j, i, row[j])
			} catch(e:Exception) {
				for(j in 0..<width) delBlock(j, i)
			}
		}
	}

	/** @param row Row of blocks
	 * @return a String representing the [row] with attributes
	 */
	fun attrRowToString(row:List<Block?>?):String {
		val strResult = StringBuilder()

		row?.forEach {
			strResult.append((it?.cint?:0).toString(16)).append("/")
			strResult.append((it?.aint?:0).toString(16)).append(";")
		}

		return "$strResult"
	}

	/** Convert this field to a String with attributes
	 * @return a String representing the field with attributes
	 */
	fun attrFieldToString():String {
		var strResult = StringBuilder()

		for(i in height-1 downTo maxOf(-1, highestBlockY)) strResult.append(attrRowToString(getRow(i)))
		while("$strResult".endsWith("0/0;")) strResult = StringBuilder(strResult.substring(0, strResult.length-4))

		return "$strResult"
	}

	fun attrStringToRow(str:String, skin:Int):List<Block> =
		attrStringToRow(str.split(Regex(";")).dropLastWhile {it.isEmpty()}, skin)

	fun attrStringToRow(strArray:List<String>, skin:Int):List<Block> = List(width) {j ->
		var blkColor:Int = Block.COLOR_NONE
		var attr = 0

		try {
			val strSubArray = strArray[j].split(Regex("/")).dropLastWhile {it.isEmpty()}
			if(strSubArray.isNotEmpty()) blkColor = strSubArray[0].toInt(16)
			if(strSubArray.size>1) attr = strSubArray[1].toInt(16)
		} catch(_:Exception) {
		}

		Block(blkColor, skin).apply {
			elapsedFrames = -1
			aint = attr
			setAttribute(true, ATTRIBUTE.VISIBLE, ATTRIBUTE.OUTLINE)
		}
	}

	fun attrStringToField(str:String, skin:Int) {
		val strArray = str.split(Regex(";")).dropLastWhile {it.isEmpty()}

		for(i in -1..<height) {
			val index = (height-1-i)*width

			try {
				//String strTemp="";
				val strArray2 = List(width) {
					if(index+it<strArray.size) strArray[index+it] else ""
				}
				val row = attrStringToRow(strArray2, skin)
				for(j in 0..<width) setBlock(j, i, row[j])
			} catch(e:Exception) {
				for(j in 0..<width) setBlock(j, i, null)
			}
		}
	}

	/** fieldの文字列表現を取得 */
	override fun toString():String {
		val str = StringBuilder("${javaClass.name}@${Integer.toHexString(hashCode())}\n")

		for(i in -hiddenHeight..<height) {
			str.append("%3d:".format(i))

			for(j in 0..<width) {
				val blk = getBlock(j, i)

				str.append(when(blk?.color) {
					null -> "."
					else -> "${blk.cint}"
				})
			}

			str.append("\n")
		}

		return "$str"
	}

	@JvmOverloads
	fun garbageDrop(engine:GameEngine, drop:Int, big:Boolean, hard:Int = 0, countdown:Int = 0, avoidColumn:Int = -1,
		color:Int = Block.COLOR_WHITE) {
		var d = drop
		var y = -1*hiddenHeight
		var actualWidth = width
		if(big) actualWidth = actualWidth shr 1
		val bigMove = if(big) 2 else 1
		while(d>=actualWidth) {
			d -= actualWidth
			var x = 0
			while(x<actualWidth) {
				garbageDropPlace(x, y, big, hard, color, countdown)
				x += bigMove
			}
			y += bigMove
		}
		if(d==0) return
		val placeBlock = BooleanArray(actualWidth)
		var j:Int
		if(d>actualWidth shr 1) {
			for(x in 0..<actualWidth) placeBlock[x] = true
			var start = actualWidth
			if(avoidColumn in 0..<actualWidth) {
				start--
				placeBlock[avoidColumn] = false
			}
			for(i in start downTo d+1) {
				do j = engine.random.nextInt(actualWidth)
				while(!placeBlock[j])
				placeBlock[j] = false
			}
		} else {
			for(x in 0..<actualWidth) placeBlock[x] = false
			for(i in 0..<d) {
				do j = engine.random.nextInt(actualWidth)
				while(placeBlock[j]&&j!=avoidColumn)
				placeBlock[j] = true
			}
		}

		for(x in 0..<actualWidth) if(placeBlock[x]) garbageDropPlace(x*bigMove, y, big, hard, color, countdown)
	}

	@JvmOverloads
	fun garbageDropPlace(x:Int, y:Int, big:Boolean, hard:Int, color:Int = Block.COLOR_WHITE, countdown:Int = 0):Boolean {
		val b = getBlock(x, y)?:return false
		if(big) {
			garbageDropPlace(x+1, y, false, hard)
			garbageDropPlace(x, y+1, false, hard)
			garbageDropPlace(x+1, y+1, false, hard)
		}
		if(getBlockEmpty(x, y, false)) {
			setBlockColor(x, y, color)
			b.run {
				setAttribute(false, ATTRIBUTE.ANTIGRAVITY)
				setAttribute(true, ATTRIBUTE.GARBAGE)
				setAttribute(true, ATTRIBUTE.BROKEN)
				setAttribute(true, ATTRIBUTE.VISIBLE)
				setAttribute(false, ATTRIBUTE.CONNECT_UP)
				setAttribute(false, ATTRIBUTE.CONNECT_DOWN)
				setAttribute(false, ATTRIBUTE.CONNECT_LEFT)
				setAttribute(false, ATTRIBUTE.CONNECT_RIGHT)
				secondaryColor = Block.COLOR.BLACK
			}
			b.hard = hard
			b.countdown = countdown
			return true
		}
		return false
	}

	/** mainly for Physician, generate random maps */
	fun addRandomHoverBlocks(engine:GameEngine, count:Int, colors:List<Pair<Block.COLOR, Block.TYPE>>, minY:Int,
		avoidLines:Boolean, flashMode:Boolean = false) {
		val posRand = Random(engine.random.nextLong())
		val colorRand = Random(engine.random.nextLong())
		val placeHeight = height-minY
		val placeSize = placeHeight*width
		val colorCounts = MutableList(colors.size) {0}

		val placeBlock = List(width) {MutableList(placeHeight) {count>=(placeSize shr 1)}}
		if(count<placeSize shr 1) {
			var i = 0
			while(i<count) {
				val x = posRand.nextInt(width)
				val y = posRand.nextInt(placeHeight)
				if(!getBlockEmpty(x, y+minY)) i--
				else placeBlock[x][y] = true

				i++
			}
		} else {
			var i = placeSize
			while(i>count) {
				val x = posRand.nextInt(width)
				val y = posRand.nextInt(placeHeight)
				if(placeBlock[x][y]) placeBlock[x][y] = false
				else i++
				i--
			}
		}

		placeBlock.forEachIndexed {x, a ->
			a.forEachIndexed {y, b ->
				if(b) {
					val blockColor = (x+y+colorRand.nextInt(colors.size))%colors.size
					colorCounts[blockColor]++
					addHoverBlock(x, y+minY, colors[blockColor])
				}
			}
		}

		if(!avoidLines||colors.size==1) return
		var colorUp:Block.COLOR?
		var colorLeft:Block.COLOR?
		var cIndex:Int
		for(y in minY..<height) for(x in 0..<width) if(placeBlock[x][y-minY]) {
			colorUp = getBlockColor(x, y-2)
			colorLeft = getBlockColor(x-2, y)
			val blockColor = getBlockColor(x, y)
			if(blockColor!=colorUp&&blockColor!=colorLeft) continue

			cIndex = colors.indexOfFirst {it.first==blockColor}

			if(cIndex!=-1) {
				val color = colors.map {it.first}
				if(color.size==2) {
					if(color[0]==colorUp&&color[1]!=colorLeft||color[0]==colorLeft&&color[1]!=colorUp) {
						colorCounts[1]++
						colorCounts[cIndex]--
						setBlockColor(x, y, color[1])
					} else if(color[1]==colorUp&&color[0]!=colorLeft||color[1]==colorLeft&&color[0]!=colorUp) {
						colorCounts[0]++
						colorCounts[cIndex]--
						setBlockColor(x, y, color[0])
					}
				} else {
					var newColor:Int
					do newColor = colorRand.nextInt(color.size)
					while(color[newColor]==colorUp||color[newColor]==colorLeft)
					colorCounts[cIndex]--
					colorCounts[newColor]++
					setBlockColor(x, y, color[newColor])
				}
			}
		}
		val minCount = count/colors.size
		val maxCount = (count+colors.size-1)/colors.size
		var done = true
		for(colorCount in colorCounts) if(colorCount>maxCount) {
			done = false
			break
		}
		var bestSwitch:Int
		var bestSwitchCount:Int
		var excess = 0
		var fill:Boolean
		while(!done) {
			done = true
			for(y in minY..<height) for(x in 0..<width) {
				val blockColor = getBlockColor(x, y)
				fill = getBlock(x, y)==null
				cIndex = -1
				if(!fill) {
					if(!placeBlock[x][y-minY]) continue
					cIndex = colors.indexOfFirst {it.first==blockColor}
					if(cIndex==-1) continue
					if(colorCounts[cIndex]<=maxCount) continue
				}

				val canSwitch = colorCounts.map {
					it<maxCount&&!listOf(getBlockColor(x, y-2), getBlockColor(x, y+2), getBlockColor(x-2, y),
						getBlockColor(x+2, y)).any {c -> !colors.any {(first) -> first==c}}
				}
				bestSwitch = -1
				bestSwitchCount = Integer.MAX_VALUE
				for(i in colorCounts.indices) if(canSwitch[i]&&colorCounts[i]<bestSwitchCount) {
					bestSwitch = i
					bestSwitchCount = colorCounts[i]
				}
				if(bestSwitch!=-1) {
					if(fill) {
						excess++
						addHoverBlock(x, y, colors[bestSwitch])
						placeBlock[x][y-minY] = true
					} else {
						colorCounts[cIndex]--
						setBlockColor(x, y, colors[bestSwitch])
					}
					colorCounts[bestSwitch]++
					done = false
				}
			}
			while(excess>0) {
				val x = posRand.nextInt(width)
				val y = posRand.nextInt(placeHeight)+minY
				if(!placeBlock[x][y-minY]) continue
				val blockColor = getBlockColor(x, y)
				for(i in colors.indices) if(colors[i].first==blockColor) {
					if(colorCounts[i]>minCount) {
						delBlock(x, y)
						colorCounts[i]--
						excess--
						placeBlock[x][y-minY] = false
					}
					break
				}
			}
			var balanced = true
			for(colorCount in colorCounts) if(colorCount>maxCount) {
				balanced = false
				break
			}
			if(balanced) done = true
		}
		if(!flashMode) return
		val gemNeeded = colors.mapIndexed {i, (first) -> first.color&&colorCounts[i]>0}.toMutableList()
		done = !gemNeeded.any()
		while(!done) {
			val x = posRand.nextInt(width)
			val y = posRand.nextInt(placeHeight)+minY
			if(!placeBlock[x][y-minY]) continue
			val blockColor = getBlockColor(x, y)
			for(i in colors.indices) if(colors[i].first==blockColor) {
				if(gemNeeded[i]) {
					getBlock(x, y)?.type = Block.TYPE.GEM
					gemNeeded[i] = false
				}
				break
			}
			done = true
			for(i in colors.indices) if(gemNeeded[i]) done = false
		}
	}

	fun addHoverBlock(x:Int, y:Int, mode:Pair<Block.COLOR, Block.TYPE>, skin:Int = 0):Boolean =
		setBlock(x, y, Block(mode, skin, ATTRIBUTE.ANTIGRAVITY, ATTRIBUTE.BROKEN, ATTRIBUTE.VISIBLE).apply {
			setAttribute(false, ATTRIBUTE.GARBAGE, ATTRIBUTE.ERASE, ATTRIBUTE.CONNECT_UP, ATTRIBUTE.CONNECT_DOWN,
				ATTRIBUTE.CONNECT_LEFT, ATTRIBUTE.CONNECT_RIGHT)
		})

	fun shuffleColors(blockColors:List<Block.COLOR>, numColors:Int, rand:Random) {
		val bC = blockColors.toMutableList()
		val maxX = minOf(bC.size, numColors)
		var j:Int
		var i = maxX
		while(i>1) {
			j = rand.nextInt(i)
			i--
			if(j!=i) {
				val temp = bC[i]
				bC[i] = bC[j]
				bC[j] = temp
			}
		}
		for(x in 0..<width) for(y in 0..<height) {
			var temp = getBlockColor(x, y, false)-1
			if(numColors==3&&temp>=3) temp--
			if(temp in 0..<maxX) setBlockColor(x, y, bC[temp])
		}
	}
	/** Instant avalanche, skips intermediate (cascade falling animation) steps.
	 * @return true if it affected the field at all, false otherwise.
	 */
	fun freeFall():Boolean {
		var y1:Int
		var y2:Int
		var result = false
		for(x in 0..<width) {
			y1 = height-1
			while(!getBlockEmpty(x, y1)&&y1>=-1*hiddenHeight) y1--
			y2 = y1
			while(getBlockEmpty(x, y2)&&y2>=-1*hiddenHeight) y2--
			while(y2>=-1*hiddenHeight) {
				setBlock(x, y1, getBlock(x, y2))
				setBlock(x, y2, Block())
				y1--
				y2--
				result = true
				while(getBlockEmpty(x, y2)&&y2>=-1*hiddenHeight) y2--
			}
		}
		return result
	}

	val delEvenRange get() = (highestBlockY..<height).filter {it and 1==0}
	fun delEven() {
		for(y in delEvenRange) delLine(y)
	}

	val delLowerRange get() = height-(height-highestBlockY+1 shr 1)..<height

	fun delLower() {
//		for(i in 1..(height-highestBlockY+1 shr 1)) delLine(height-i)
		for(y in delLowerRange) delLine(y)
	}

	// I think this rounds up.
	val delUpperRange get() = highestBlockY..<highestBlockY+((height-highestBlockY)/2)

	fun delUpper() {
//		for(y in 0..<(((height-highestBlockY)/2.0).roundToLong().toInt()))) delLine(highestBlockY+y)
		for(y in delUpperRange) delLine(y)
	}

	fun delLine(y:Int) {
		for(x in 0..<width) {
			getBlock(x, y)?.hard = 0
		}
		setLineFlag(y, true)
	}

	fun moveLeft() {
		var x1:Int
		var x2:Int
		for(y in highestBlockY..<height) {
			x1 = 0
			while(!getBlockEmpty(x1, y)) x1++
			x2 = x1
			while(x2<width) {
				while(getBlockEmpty(x2, y)&&x2<width) x2++
				setBlock(x1, y, getBlock(x2, y))
				setBlock(x2, y, Block())
				x1++
				x2++
			}
		}
	}

	fun moveRight() {
		var x1:Int
		var x2:Int
		for(y in highestBlockY..<height) {
			x1 = width-1
			while(!getBlockEmpty(x1, y)) x1--
			x2 = x1
			while(x2>=0) {
				while(getBlockEmpty(x2, y)&&x2>=0) x2--
				setBlock(x1, y, getBlock(x2, y))
				setBlock(x2, y, Block())
				x1--
				x2--
			}
		}
	}

	fun negaField():Field {
		for(y in highestBlockY..<height) for(x in 0..<width) {
			if(getBlockEmpty(x, y)) garbageDropPlace(x, y, false, 0) // TODO: Set color
			else setBlock(x, y, null)
		}
		return this
	}

	/**
	 * Gets the full height of a field, including hidden height.
	 *
	 * @return int; Full height
	 */
	val fullHeight:Int get() = hiddenHeight+height

	/** filp this field vertically */
	fun flipVertical():Field {
		var temp:Block?
		var yMin = highestBlockY-hiddenHeight
		var yMax = fullHeight-1
		while(yMin<yMax) {
			for(x in 0..<width) {
				temp = getBlock(x, yMin)
				setBlock(x, yMin, getBlock(x, yMax))
				setBlock(x, yMax, temp)
			}
			yMin++
			yMax--
		}
		return this
	}

	/** filp this field horizontally */
	fun flipHorizontal():Field {
		var temp:Block?
		var xMin = 0
		var xMax = width-1
		while(xMin<xMax) {
			for(y in highestBlockY..<height) {
				temp = getBlock(xMin, y)
				setBlock(xMin, y, getBlock(xMax, y))
				setBlock(xMax, y, temp)
				xMin++
				xMax--
			}
		}
		return this
	}

	/** filp this field 180 */
	fun mirror():Field {
		flipVertical()
		flipHorizontal()
		return this
	}

	companion object {
		/** Log */
		internal var log = LogManager.getLogger()

		/** default の幅 */
		const val DEFAULT_WIDTH = 10

		/** default の高さ */
		const val DEFAULT_HEIGHT = 20

		/** default の見えない部分の高さ */
		const val DEFAULT_HIDDEN_HEIGHT = 3

		/** 座標の属性 (通常) */
		const val COORD_NORMAL = 0

		/** 座標の属性 (見えない部分) */
		const val COORD_HIDDEN = 1

		/** 座標の属性 (置いたBlockが消える) */
		const val COORD_VANISH = 2

		/** 座標の属性 (壁) */
		const val COORD_WALL = 3

		/** Twister判定用相対x座標*/
		private val tx = listOf(listOf(0, 2, 0, 2), listOf(1, 4, 1, 4))
		/** Twister判定用相対y座標*/
		private val ty = listOf(listOf(0, 0, 2, 2), listOf(1, 1, 4, 4))
	}
}
