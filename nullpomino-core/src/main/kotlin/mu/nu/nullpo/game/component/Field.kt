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

import mu.nu.nullpo.game.component.Block.ATTRIBUTE
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameEngine.LineGravity
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.filterNotNullIndexed
import mu.nu.nullpo.util.GeneralUtil.toInt
import org.apache.logging.log4j.LogManager
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.random.Random

/** Gamefield */
@kotlinx.serialization.Serializable class Field {

	/** パラメータ付きConstructor
	 * @param w fieldの幅
	 * @param h Field height
	 * @param hh fieldより上の見えない部分の高さ
	 * @param c 天井の有無
	 */
	@JvmOverloads constructor(w:Int = DEFAULT_WIDTH, h:Int = DEFAULT_HEIGHT, hh:Int = DEFAULT_HIDDEN_HEIGHT, c:Boolean = false) {
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

	/** HURRY UP地面のcount */
	var hurryupFloorLines = 0; private set

	/** Number of total blocks above minimum required in cint clears */
	var colorClearExtraCount = 0

	/** Number of different colors in simultaneous cint clears */
	var colorsCleared = 0; private set

	/** Number of gems cleared in last cint or line cint clear */
	var gemsCleared = 0; private set

	/** Number of garbage blocks cleared in last cint clear */
	var garbageCleared = 0; private set

	/** List of colors of lines cleared in most recent line cint clear */
	var lineColorsCleared = emptyList<Int>()

	/** List of last rows cleared in most recent horizontal line clear. */
	var lastLinesCleared:List<List<Block?>> = emptyList(); private set
	var lastLinesHeight = emptyList<Int>(); private set

	var lastLinesSplited = false; private set

	var lastexplodedbombs = false; private set

	/** Used for TGM garbage, can later be extended to all types */
	// public ArrayList<Block[]> pendingGarbage;

	/** BombとSparkの効果範囲 */
	private var explodWidth = 0
	private var explodHeight = 0
	private var explodWidthBig = 0
	private var explodHeightBig = 0

	val lastLinesTop:Int get() = lastLinesHeight.minOrNull() ?: height
	val lastLinesBottom:Int get() = lastLinesHeight.maxOrNull() ?: 0

	/** 消えるLinescountを数える
	 * @return Linescount
	 */
	val lines:Int get() = (-hiddenHeight until heightWithoutHurryupFloor).count {getLineFlag(it)}

	/** All clearだったらtrue
	 * @return All clearだったらtrue
	 */
	val isEmpty:Boolean
		get() = howManyBlocks==0

	/** field内に何個のBlockがあるか調べる
	 * @return field内にあるBlockのcount
	 */
	val howManyBlocks:Int
		get() = (-hiddenHeight until heightWithoutHurryupFloor)
			.filter {!getLineFlag(it)}
			.sumOf {getRow(it).count {b -> b?.isEmpty==false}}

	/** 左から何個のBlockが並んでいるか調べる
	 * @return 左から並んでいるBlockの総count
	 */
	val howManyBlocksFromLeft:Int
		get() = (-hiddenHeight until heightWithoutHurryupFloor)
			.filter {!getLineFlag(it)}
			.sumOf {getRow(it).takeWhile {b -> b?.isEmpty==false}.size}

	/** 右から何個のBlockが並んでいるか調べる
	 * @return 右から並んでいるBlockの総count
	 */
	val howManyBlocksFromRight:Int
		get() = (-hiddenHeight until heightWithoutHurryupFloor)
			.filter {!getLineFlag(it)}
			.sumOf {getRow(it).takeLastWhile {b -> b?.isEmpty==false}.size}

	/** 一番上にあるBlockのY-coordinateを取得
	 * @return 一番上にあるBlockのY-coordinate
	 */
	val highestBlockY:Int
		get() =
			(-hiddenHeight until heightWithoutHurryupFloor)
				.filter {!getLineFlag(it)}
				.firstOrNull {getRow(it).filterNotNull().any {b -> !b.isEmpty}} ?: height

	/** garbage blockが最初に現れるY-coordinateを取得
	 * @return garbage blockが最初に現れるY-coordinate
	 */
	val highestGarbageBlockY:Int
		get() =
			(-hiddenHeight until heightWithoutHurryupFloor)
				.filter {!getLineFlag(it)}
				.firstOrNull {getRow(it).filterNotNull().any {b -> b.getAttribute(ATTRIBUTE.GARBAGE)}} ?: height

	val howManyBlocksCovered:Int
		get() = (0 until width).sumOf {j ->
			(getHighestBlockY(j) until heightWithoutHurryupFloor)
				.filter {!getLineFlag(it)}
				.count {getBlockEmpty(j, it)}
		}

	val danger:Boolean get() = (howManyBlocks+howManyLidAboveHoles)*5/(width*heightWithoutHurryupFloor)>=4

	/** field内の隙間のcountを調べる
	 * @return field内の隙間のcount
	 */
	val howManyHoles:Int
		get() {
			var hole = 0
			var samehole:Boolean

			for(j in 0 until width) {
				samehole = false

				for(i in highestBlockY until heightWithoutHurryupFloor)
					if(!getLineFlag(i))
						when {
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

			for(j in 0 until width) {
				var count = 0

				for(i in highestBlockY until heightWithoutHurryupFloor-1)
					if(!getLineFlag(i))
						if(isHoleBelow(j, i)) {
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
		get() = (0 until width).sumOf {
			val d = getValleyDepth(it)
			if(d>=2) d else 0
		}

	/** I型が必要な谷 (深さ3以上）のcountを返す
	 * @return I型が必要な谷のcount
	 */
	val totalValleyNeedIPiece:Int
		get() = (0 until width).count {getValleyDepth(it)>=3}

	/** @return an ArrayList of rows representing the TGM attack of the last
	 * line
	 * clear action The TGM attack is the lines of the last line clear
	 * flipped vertically and without the blocks that caused it.
	 */
	// Put an empty block if the original block was in the last
	// commit to the field.
	val lastLinesAsTGMAttack:List<List<Block?>>
		get() = lastLinesCleared.map {y ->
			y.map {it?.takeUnless {it.getAttribute(ATTRIBUTE.LAST_COMMIT)}}
		}

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
					for(j in 0 until width)
						if(j!=holeLoc&&getBlockEmpty(j, i)) {
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
		get() = (-hiddenHeight until heightWithoutHurryupFloor)
			.sumOf {i -> getRow(i).filterNotNull().count {it.isGemBlock}}

	/** 宝石Blockがいくつ消えるか取得
	 * @return 消える宝石Blockのcount
	 */
	val howManyGemClears:Int
		get() = (-hiddenHeight until heightWithoutHurryupFloor)
			.filter {getLineFlag(it)}
			.sumOf {i -> getRow(i).filterNotNull().count {it.isGemBlock}}

	/** Checks for inum blocks cleared
	 * @return A boolean list with true at each index for which an inum block
	 * of the corresponding ID number was cleared
	 */
	val itemClears
		get() = List(Block.MAX_ITEM+1) {item ->
			(-hiddenHeight until heightWithoutHurryupFloor)
				.filter {getLineFlag(it)}.any {i -> getRow(i).any {b -> b?.inum==item}}
		}

	/** Garbageを含むラインがいくつ消えるか取得
	 * @return 消去予定のGarbageを含むライン数
	 */
	// Check the lines we are clearing.
	val howManyGarbageLineClears:Int
		get() = (-hiddenHeight until heightWithoutHurryupFloor)
			.filter {getLineFlag(it)}.count {
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
		get() = findBlocks {!it.getAttribute(ATTRIBUTE.GARBAGE)&&(it.isGoldSquareBlock||it.isSilverSquareBlock)}
			.filter {(y, _) -> getLineFlag(y)}.values.let {
				listOf(it.sumOf {it.values.count {it.isGoldSquareBlock}},
					it.sumOf {it.values.count {it.isSilverSquareBlock}}
				)
			}

	/** HURRY UPの地面を除いたField heightを返す
	 * @return HURRY UPの地面を除いたField height
	 */
	val heightWithoutHurryupFloor:Int
		get() = height-hurryupFloorLines

	/** Called at initialization */
	fun reset() {
		blockField = List(height) {MutableList(width) {null}}
		blockHidden = List(hiddenHeight) {MutableList(width) {null}}
		lineflagField = MutableList(height) {false}
		lineflagHidden = MutableList(hiddenHeight) {false}
		hurryupFloorLines = 0

		colorClearExtraCount = 0
		colorsCleared = 0
		gemsCleared = 0
		lineColorsCleared = emptyList()
		lastLinesCleared = emptyList()
		lastLinesHeight = emptyList()

		lastLinesSplited = false
		garbageCleared = 0
		explodWidth = 0
		explodHeight = 0
		explodWidthBig = 0
		explodHeightBig = 0

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
			hurryupFloorLines = o.hurryupFloorLines

			colorClearExtraCount = o.colorClearExtraCount
			colorsCleared = o.colorsCleared
			o.gemsCleared = 0
			gemsCleared = o.gemsCleared
			lineColorsCleared = o.lineColorsCleared.toList()
			lastLinesCleared = o.lastLinesCleared.toList()
			garbageCleared = o.garbageCleared
			lastLinesHeight = o.lastLinesHeight.toList()
			lastLinesSplited = o.lastLinesSplited

			explodHeight = o.explodHeight
			explodWidth = o.explodWidth
			explodHeightBig = o.explodHeightBig
			explodWidthBig = o.explodWidthBig
		} ?: reset()
	}

	/** プロパティセットに保存
	 * @param p プロパティセット
	 * @param id 適当なID
	 */
	fun writeProperty(p:CustomProperties, id:Int) {
		for(i in 0 until height) {
			val mapStr = StringBuilder()

			for(j in 0 until width) {
				mapStr.append("${getBlock(j, i)?.toChar() ?: '_'}")
				if(j<width-1) mapStr.append(",")
			}

			p.setProperty("$id.field.values.$i", "$mapStr")
		}
	}

	/** プロパティセットから読み込み
	 * @param p プロパティセット
	 * @param id 適当なID
	 */
	fun readProperty(p:CustomProperties, id:Int) {
		for(i in 0 until height) {
			val mapStr = p.getProperty("$id.field.values.$i", "")
			val mapArray = mapStr.split(Regex(",")).dropLastWhile {it.isEmpty()}

			for(j in mapArray.indices) {
				val blkColor = try {
					mapArray[j].toInt()
				} catch(e:NumberFormatException) {
					-1
				}

				setBlockColor(j, i, blkColor)

				getBlock(j, i)?.apply {elapsedFrames = -1}
			}
		}
	}

	/** 指定された座標の属性を取得
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @return 座標の属性
	 */
	fun getCoordAttribute(x:Int, y:Int):Int = when {
		y<0&&ceiling -> COORD_WALL// 天井
		x<0||x>=width||y>=height -> COORD_WALL// 壁
		y>=0 -> COORD_NORMAL// 通常
		(y*-1-1)<hiddenHeight -> COORD_HIDDEN    // 見えない部分
		else -> COORD_VANISH// 置いたBlockが消える
	}

	/** 指定された座標にBlockが置けるかを取得
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @return Blockが置ける座標ならtrue
	 */
	fun getCoordVaild(x:Int, y:Int):Boolean = getCoordAttribute(x, y).let {it==COORD_NORMAL||it==COORD_HIDDEN}

	/** @param y height of the row in the field
	 * @return a reference to the row
	 */
	fun getRow(y:Int):List<Block?> = (if(height>0&&y>=0) blockField.getOrNull(y)
	else blockHidden.getOrNull(y*-1-1))?.map {if(it?.isEmpty!=false) null else it} ?: List(10) {null}

	/** 指定した座標にあるBlockを取得
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @return 成功したら指定した座標にあるBlockオブジェクト, 失敗したらnull
	 */
	fun getBlock(x:Int, y:Int):Block? = getRow(y).getOrNull(x).let {if(it?.isEmpty!=false) null else it}

	/** Set block to specific location
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk Block
	 * @return true if successful, false if failed
	 */
	fun setBlock(x:Int, y:Int, blk:Block?):Boolean = if(blk?.color==null) delBlock(x, y)!=null else
		if(getCoordVaild(x, y)) try {
			if(y<0) {
				if((getBlock(x, y)?.replace(blk))==null) blockHidden[y*-1-1][x] = Block(blk)
			} else if((getBlock(x, y)?.replace(blk))==null) blockField[y][x] = Block(blk)
			true
		} catch(e:Throwable) {
			log.error("setBlock($x,$y)", e)
			false
		}
		else false

	/** Remove block from specific location
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @return Previous Block if successful, null if failed
	 */
	fun delBlock(x:Int, y:Int):Block? = if(getCoordVaild(x, y)) try {
		(getBlock(x, y))?.let {
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
		findBlocks((if(inHide) -1*hiddenHeight else 0) until height, cond)
	/** Remove blocks from specific location
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @return Previous Block if successful, null if failed
	 */
	@JvmName("delBlocksi") fun delBlocks(pos:Map<Int, Collection<Int>>) =
		pos.map {(y, row) -> y to row.mapNotNull {x -> delBlock(x, y)?.let {x to it}}.associate {it}}.associate {it}

	fun delBlocks(blocks:Map<Int, Map<Int, Block>>) =
		blocks.map {(y, row) -> y to row.mapNotNull {(x, _) -> delBlock(x, y)?.let {x to it}}.associate {it}}.associate {it}
	/** 指定した座標にあるBlock colorを取得
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param gemSame If true, a gem block will return the cint of the
	 * corresponding normal block.
	 * @return 指定した座標にあるBlock cint (失敗したらBLOCK_COLOR_INVALID）
	 */
	fun getBlockColor(x:Int, y:Int, gemSame:Boolean = false):Int =
		if(getCoordVaild(x, y)) (getBlock(x, y)?.cint ?: Block.COLOR_NONE)
			.let {if(gemSame) Block.gemToNormalColor(it) else it}
		else Block.COLOR_INVALID

	fun getBlockColor(x:Int, y:Int):Block.COLOR? =
		if(getCoordVaild(x, y)) getBlock(x, y)?.color else null

	/** 指定した座標にあるBlock colorを変更
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param c 色
	 * @return true if successful, false if failed
	 */
	fun setBlockColor(x:Int, y:Int, c:Block.COLOR):Boolean =
		if(getCoordVaild(x, y))
			if(getBlock(x, y)?.also {it.color = c}==null) setBlock(x, y, Block(c)) else true
		else false

	/** 指定した座標にあるBlock colorを変更
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param c 色 to タイプ
	 * @return true if successful, false if failed
	 */
	fun setBlockColor(x:Int, y:Int, c:Pair<Block.COLOR, Block.TYPE>):Boolean =
		if(getCoordVaild(x, y))
			if(getBlock(x, y)?.also {it.color = c.first;it.type = c.second}==null) setBlock(
				x,
				y,
				Block(c.first, c.second)
			) else true
		else false
	/** 指定した座標にあるBlock colorを変更
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param c 色タイプ
	 * @return true if successful, false if failed
	 */
	fun setBlockColor(x:Int, y:Int, c:Int):Boolean =
		if(c<0) delBlock(x, y)!=null else if(getCoordVaild(x, y))
			if(getBlock(x, y)?.also {it.cint = c}==null&&c>=1) setBlock(x, y, Block(c)) else true
		else false

	/*fun howManyColor(c:Block.COLOR):Int =
		blockField.sum{it.count{it.color==c}}

	fun howManyColor(c:Block.COLOR,t:Block.TYPE):Int =
		(blockField+blockHidden).sum{it.count{it.color==c && it.type==t}}
*/
	/** Line clear flagを取得
	 * @param y Y-coordinate
	 * @return 消える列ならtrue, そうでないなら (もしくは座標が範囲外なら）false
	 */
	fun getLineFlag(y:Int):Boolean =
		(if(y>=0) lineflagField.getOrNull(y)
		else lineflagHidden.getOrNull(y*-1-1)) ?: false

	/** 指定した座標にあるBlockが空白かどうか判定
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @return 指定した座標にあるBlockが空白ならtrue (指定した座標が範囲外の場合もtrue）
	 */
	fun getBlockEmpty(x:Int, y:Int, ob:Boolean = true):Boolean = if(getCoordVaild(x, y)) getBlock(x, y)?.isEmpty ?: true
	else ob

	/** 指定した座標にあるBlockが空白かどうか判定 (指定した座標が範囲外の場合はfalse）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @return 指定した座標にあるBlockが空白ならtrue (指定した座標が範囲外の場合はfalse）
	 */
	@Deprecated("overloaded", ReplaceWith("getBlockEmpty(x,y,false)"))
	fun getBlockEmptyF(x:Int, y:Int):Boolean = getBlockEmpty(x, y, false)

	/** Line clear flagを設定
	 * @param y Y-coordinate
	 * @param flag 設定するLine clear flag
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
	 * @return 消えるLinescount
	 */
	fun checkLine():Int {
		val lines = checkLinesNoFlag()
		if(height<=0||lines.isEmpty()) return 0
		lastLinesCleared = lines.map {getRow(it)}
		lastLinesHeight = lines

		lastLinesSplited = false
		var inv = false
		for(i in -hiddenHeight until heightWithoutHurryupFloor) {
			val flag = lines.any {it==i}
			setLineFlag(i, flag)
			if(flag) {
				if(inv) lastLinesSplited = true
				for(it in getRow(i)) {
					it?.setAttribute(true, ATTRIBUTE.ERASE)
				}
			} else if(i>=lines.sorted().first()) inv = true
		}
		return lines.size
	}

	/** Line clear check (消去 flagの設定とかはしない）
	 * @return 消えるLinescount
	 */
	fun checkLineNoFlag():Int = checkLinesNoFlag().size

	fun checkLinesNoFlag():List<Int> {
		val lines = (-hiddenHeight until heightWithoutHurryupFloor)
			.filter {
				getRow(it).all {b -> b?.isEmpty==false&&!b.getAttribute(ATTRIBUTE.WALL)}
			}
		garbageCleared = lines.count {
			getRow(it).any {b -> b?.getAttribute(ATTRIBUTE.GARBAGE)==true}
		}
		return lines
	}

	/** Linesを消す
	 * @return 消えたLinescount
	 */
	fun clearLine():Int {
		val lines = checkLinesNoFlag()
		// field内
		lines.forEach {y ->
			getRow(y).filterNotNullIndexed().forEach {(x, b) ->
				if(b.hard>0) {
					b.hard--
					setLineFlag(y, false)
				} else delBlock(x, y)
			}
			// 消えたLinesの上下のBlockの結合を解除
			getRow(y+1).filterNotNullIndexed().forEach {(j, blk) ->
				if(blk.getAttribute(ATTRIBUTE.CONNECT_UP)) {
					blk.setAttribute(false, ATTRIBUTE.CONNECT_UP)
					setBlockLinkBroken(j, y)
				}
			}
			getRow(y-1).filterNotNullIndexed().forEach {(j, blk) ->
				if(blk.getAttribute(ATTRIBUTE.CONNECT_DOWN)) {
					blk.setAttribute(false, ATTRIBUTE.CONNECT_DOWN)
					setBlockLinkBroken(j, y)
				}
			}
		}
		return lines.size
	}

	/** 上にあったBlockをすべて下まで下ろす
	 * @return 消えていたLinescount
	 */
	fun downFloatingBlocks():Int {
		var lines = 0
		var y = heightWithoutHurryupFloor-1

		for(i in -hiddenHeight until heightWithoutHurryupFloor)
			if(getLineFlag(y)) {
				lines++

				// Blockを1段上からコピー
				for(k in y downTo 1-hiddenHeight)
					for(l in 0 until width) {
						setBlock(l, k, getBlock(l, k-1) ?: Block())
						setLineFlag(k, getLineFlag(k-1))
					}

				// 一番上を空白にする
				for(l in 0 until width)
					setBlock(l, -hiddenHeight, Block())

				setLineFlag(-hiddenHeight, false)
			} else y--

		return lines
	}

	/** 上にあったBlockを1段だけ下ろす */
	fun downFloatingBlocksSingleLine() {
		val y = heightWithoutHurryupFloor-1

		for(i in -hiddenHeight until heightWithoutHurryupFloor)
			if(getLineFlag(y)) {
				// Blockを1段上からコピー
				for(k in y downTo 1-hiddenHeight)
					for(l in 0 until width) {
						setBlock(l, k, getBlock(l, k-1))
						setLineFlag(k, getLineFlag(k-1))
					}

				// 一番上を空白にする
				for(l in 0 until width) delBlock(l, -hiddenHeight)
				setLineFlag(-hiddenHeight, false)
				break
			}
	}

	/** Check if specified line is completely empty
	 * @param y Y coord
	 * @return `true` if the specified line is completely empty,
	 * `false` otherwise.
	 */
	fun isEmptyLine(y:Int):Boolean = (0 until width).all {getBlockEmpty(it, y)}

	@Deprecated("renamed", ReplaceWith("isTwistSpot(x, y, big)"))
	fun isTSpinSpot(x:Int, y:Int, big:Boolean):Boolean = isTwistSpot(x, y, big)
	/** Twisterになる地形だったらtrue
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param big Bigかどうか
	 * @return Twisterになる地形だったらtrue
	 */
	fun isTwistSpot(x:Int, y:Int, big:Boolean):Boolean {
		// 判定用相対座標を設定
		val tx = IntArray(4)
		val ty = IntArray(4)

		if(big) {
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

		// 判定
		var count = 0

		for(i in tx.indices)
			if(getBlock(x+tx[i], y+ty[i])!=null) count++

		return count>=3

	}

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
		val tx = IntArray(4)
		val ty = IntArray(4)

		if(big) {
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

		// 判定
		var count = 0

		for(i in tx.indices)
			if(getBlock(x+tx[i], y+ty[i])!=null) count++

		return count==3

	}

	/** Twisterできそうな穴が何個あるか調べる
	 * @param big Bigだったらtrue
	 * @return Twisterできそうな穴のcount
	 */
	fun getHowManyTSlot(big:Boolean):Int {
		var result = 0

		for(j in 0 until width)
			for(i in 0 until heightWithoutHurryupFloor-2)
				if(!getLineFlag(i)) if(isTSlot(j, i, big)) result++

		return result
	}

	/** Twisterで消えるLinescountを返す
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param big Bigかどうか(未対応)
	 * @return Twisterで消えるLinescount(Twisterじゃない場合などは0)
	 */
	fun getTSlotLineClear(x:Int, y:Int, big:Boolean):Int {
		if(!isTSlot(x, y, big)) return 0

		val lineflag = (0..1).map {i ->
			!(0 until width).any {j ->
				// ■■■★※■■■■■
				// □□□※※※□□□□
				// □□□○※○□□□□
				(j<x||j>=x+3)&&getBlockEmpty(j, y+1+i, false)
			}
		}
		var lines = 0
		for(element in lineflag)
			if(element) lines++

		return lines
	}

	/** Twisterで消えるLinescountを返す(field全体)
	 * @param big Bigかどうか(未対応)
	 * @param minimum 最低Linescount(2にするとTwister Doubleにだけ反応)
	 * @return Twisterで消えるLinescount(Twisterじゃない場合やminimumに満たないLinesなどは0)
	 */
	fun getTSlotLineClearAll(big:Boolean, minimum:Int = 0):Int = (0 until width)
		.sumOf {j ->
			(0 until heightWithoutHurryupFloor-2)
				.filter {getLineFlag(it)&&getTSlotLineClear(j, it, big)>=minimum}
				.sumOf {getTSlotLineClear(j, it, big)}
		}

	/** 一番上にあるBlockのY-coordinateを取得 (X-coordinateを指定できるVersion）
	 * @param x X-coordinate
	 * @return 一番上にあるBlockのY-coordinate
	 */
	fun getHighestBlockY(x:Int):Int =
		(-hiddenHeight until heightWithoutHurryupFloor)
			.firstOrNull {!getLineFlag(it)&&!getBlockEmpty(x, it)}
			?: height

	/** 指定した座標の下に隙間があるか調べる
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @return 指定した座標の下に隙間があればtrue
	 */
	fun isHoleBelow(x:Int, y:Int):Boolean = !getBlockEmpty(x, y)&&getBlockEmpty(x, y+1)

	/** 谷 (■ ■になっている地形）の深さを調べる
	 * @param x 調べるX-coordinate
	 * @return 谷の深さ (無かったら0）
	 */
	fun getValleyDepth(x:Int):Int {
		var depth = 0

		var highest = getHighestBlockY(x-1)
		highest = minOf(highest, getHighestBlockY(x))
		highest = minOf(highest, getHighestBlockY(x+1))

		for(i in highest until heightWithoutHurryupFloor)
			if(!getLineFlag(i))
				if((!getBlockEmpty(x-1, i, false)||x<=0)&&getBlockEmpty(x, i, false)
					&&(!getBlockEmpty(x+1, i, false)||x>=width-1)
				)
					depth++

		return depth
	}

	/** field全体を上にずらす
	 * @param lines ずらす段count
	 */
	fun pushUp(lines:Int = 1) {
		for(k in 0 until lines) {
			for(i in -hiddenHeight until heightWithoutHurryupFloor-1)
			// Blockを1段下からコピー
				for(j in 0 until width) {
					setBlock(j, i, getBlock(j, i+1))
					setLineFlag(i, getLineFlag(i+1))
				}

			// 一番下を空白にする
			for(j in 0 until width) {
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
		for(k in 0 until lines) {
			for(i in heightWithoutHurryupFloor-1 downTo 1-hiddenHeight)
			// Blockを1段上からコピー
				for(j in 0 until width) {
					var blk = getBlock(j, i-1)
					if(blk==null) blk = Block()
					setBlock(j, i, blk)
					setLineFlag(i, getLineFlag(i+1))
				}

			// 一番上を空白にする
			for(j in 0 until width) {
				setBlock(j, -hiddenHeight, Block())
				setLineFlag(-hiddenHeight, false)
			}
		}
	}

	/** Cut the specified line(s) then push down all things above
	 * @param y Y coord
	 * @param lines Number of lines to cut
	 */
	fun cutLine(y:Int, lines:Int) {
		for(k in 0 until lines) {
			for(i in y downTo 1-hiddenHeight) {
				for(j in 0 until width) setBlock(j, i, getBlock(j, i-1))
				setLineFlag(i, getLineFlag(i+1))
			}

			for(j in 0 until width) {
				delBlock(j, -hiddenHeight)
				setLineFlag(-hiddenHeight, false)
			}
		}
	}

	fun setSingleHoleLine(hole:Int, y:Int, color:Block.COLOR, skin:Int) {
		for(j in 0 until width)
			if(j!=hole)
				setBlock(
					j, y, Block(
						color, skin,
						ATTRIBUTE.VISIBLE, ATTRIBUTE.OUTLINE, ATTRIBUTE.GARBAGE,
						if(j<hole) ATTRIBUTE.CONNECT_LEFT else if(j>hole) ATTRIBUTE.CONNECT_RIGHT else ATTRIBUTE.ERASE
					)
				)

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
		for(i in 0 until lines) {
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

	/** Add a single hole garbage (Attributes are automatically set)
	 * @param hole Hole position
	 * @param color Color
	 * @param skin Skin
	 * @param lines Number of garbage lines to add
	 */
	fun addSingleHoleGarbage(hole:Int, color:Block.COLOR, skin:Int, lines:Int) {
		for(it in 0 until lines) {
			pushUp(1)
			setSingleHoleLine(hole, heightWithoutHurryupFloor-1, color, skin)
		}
	}
	/** 一番下のLinesの形をコピーしたgarbage blockを一番下に追加
	 * @param skin garbage blockの絵柄
	 * @param attrs garbage blockの属性
	 * @param lines 追加するgarbage blockのLinescount
	 */
	fun addBottomCopyGarbage(skin:Int, lines:Int, vararg attrs:ATTRIBUTE) {
		for(k in 0 until lines) {
			pushUp(1)

			for(j in 0 until width) if(!getBlockEmpty(j, height-2)) {
				setBlock(j, heightWithoutHurryupFloor-1, Block(Block.COLOR.WHITE, skin, *attrs))
			}
		}
	}

	/** 全てのBlockの属性を変更
	 * @param attr 変更したい属性
	 * @param status 変更後 state
	 */
	fun setAllAttribute(status:Boolean, vararg attr:ATTRIBUTE) {
		for(i in -hiddenHeight until height)
			for(j in 0 until width) {
				getBlock(j, i)?.setAttribute(status, *attr)
			}
	}

	/** 全てのBlockの絵柄を変更
	 * @param skin 絵柄
	 */
	fun setAllSkin(skin:Int) {
		for(i in -hiddenHeight until height)
			for(j in 0 until width)
				getBlock(j, i)?.skin = skin

	}

	/** Checks for 2x2 square formations of gem and
	 * converts blocks to big bomb blocks if needed.
	 * @return Number of big bomb formations
	 */
	fun checkForBigBomb():Int {

		// Check for big bomb
		for(i in -hiddenHeight until heightWithoutHurryupFloor-1)
			for(j in 0 until width-1) {
				// rootBlk is the upper-left square
				getBlock(j, i)?.let {rootBlk ->
					var squareCheck = false
					/* id is the cint of the top-left square: if it is a
					 * monosquare, every block in the 4x4 area will have this
					 * cint. */
					if(rootBlk.isGemBlock) {
						squareCheck = true
						for(k in 1..4) {
							val blk = getBlock(j+1, i)
							if(blk==null||blk.isEmpty||!blk.isGemBlock||
								blk.cint==Block.COLOR_GEM_RAINBOW
							) {
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
		for(i in -hiddenHeight until heightWithoutHurryupFloor-3)
			for(j in 0 until width-3) {
				// rootBlk is the upper-left square
				val rootBlk = getBlock(j, i)
				var squareCheck = false

				/* id is the cint of the top-left square: if it is a
				 * monosquare, every block in the 4x4 area will have this
				 * cint. */
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
							/* Reasons why the entire area would not be a
							 * monosquare: this block does not exist, it is
							 * part
							 * of another square, it has been broken by line
							 * clears, is a garbage block, is not the same cint
							 * as id, or has connections outside the area. */
							if(blk==null||blk.isEmpty||blk.isGoldSquareBlock||blk.isSilverSquareBlock
								||blk.getAttribute(ATTRIBUTE.BROKEN)
								||blk.getAttribute(ATTRIBUTE.GARBAGE)||blk.cint!=id
								||l==0&&blk.getAttribute(ATTRIBUTE.CONNECT_LEFT)
								||l==3&&blk.getAttribute(ATTRIBUTE.CONNECT_RIGHT)
								||k==0&&blk.getAttribute(ATTRIBUTE.CONNECT_UP)
								||k==3&&blk.getAttribute(ATTRIBUTE.CONNECT_DOWN)
							) {
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
					val squareX = listOf(0, 1, 1, 2)
					val squareY = listOf(0, 3, 3, 6)
					for(k in 0..3)
						for(l in 0..3) getBlock(j+l, i+k)?.apply {
							cint = Block.COLOR_SQUARE_GOLD_1+squareX[l]+squareY[k]
							// For stylistic concerns, we attach all blocks in the square together.
							if(k>0) setAttribute(true, ATTRIBUTE.CONNECT_UP)
							if(k<3) setAttribute(true, ATTRIBUTE.CONNECT_DOWN)
							if(l>0) setAttribute(true, ATTRIBUTE.CONNECT_LEFT)
							if(l<3) setAttribute(true, ATTRIBUTE.CONNECT_RIGHT)
						}
				}
			}
		// Check for silver squares
		for(i in -hiddenHeight until heightWithoutHurryupFloor-3)
			for(j in 0 until width-3) {
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
							// See above, but without the cint checking.
							if(blk==null||blk.isEmpty||blk.isGoldSquareBlock||blk.isSilverSquareBlock
								||blk.getAttribute(ATTRIBUTE.BROKEN)
								||blk.getAttribute(ATTRIBUTE.GARBAGE)
								||l==0&&blk.getAttribute(ATTRIBUTE.CONNECT_LEFT)
								||l==3&&blk.getAttribute(ATTRIBUTE.CONNECT_RIGHT)
								||k==0&&blk.getAttribute(ATTRIBUTE.CONNECT_UP)
								||k==3&&blk.getAttribute(ATTRIBUTE.CONNECT_DOWN)
							) {
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
					val squareX = listOf(0, 1, 1, 2)
					val squareY = listOf(0, 3, 3, 6)
					for(k in 0..3)
						for(l in 0..3) getBlock(j+l, i+k)?.apply {
							cint = Block.COLOR_SQUARE_SILVER_1+squareX[l]+squareY[k]
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
	/** ライン上の爆弾の取得
	 * @param ignite 爆弾を発火予約する
	 * @return ライン上の爆弾ブロックの個数
	 */
	fun checkBombOnLine(ignite:Boolean):Int {
		var ret = 0
		for(i in -hiddenHeight until heightWithoutHurryupFloor) {
			val bil = ArrayList<Int>()
			for(j in 0 until width) {
				val b = getBlock(j, i)
				if(b?.isEmpty!=false) {
					bil.clear()
					break
				}
				if(b.isGemBlock) bil.add(j)
			}
			setLineFlag(i, bil.isNotEmpty())
			if(bil.isNotEmpty()) {
				ret += bil.size
				if(ignite)
					for(j in bil)
						getBlock(j, i)?.run {
							cint = Block.COLOR_GEM_RAINBOW
							setAttribute(true, ATTRIBUTE.ERASE)
						}

			}
		}
		return ret
	}

	/** 点火した爆弾ブロックの個数
	 * @return 点火した爆弾ブロックの個数
	 */
	fun checkBombIgnited():Int {
		var ret = 0
		for(i in -hiddenHeight until heightWithoutHurryupFloor)
			for(j in 0 until width) {
				val b = getBlock(j, i) ?: continue
				if(b.cint==Block.COLOR_GEM_RAINBOW&&b.getAttribute(ATTRIBUTE.ERASE))
					ret++
			}
		return ret
	}

	/** 予約済みの爆弾を爆発させる
	 * @param w width
	 * @param h height
	 * @param bigw
	 * @param bigh
	 * @return 破壊するブロックの個数
	 */
	@JvmOverloads
	fun igniteBomb(w:Int = explodWidth, h:Int = explodHeight, bigw:Int = explodWidth, bigh:Int = explodHeight):Int {
		var ret = 0
		explodWidth = w
		explodHeight = h
		explodHeightBig = bigh
		explodWidthBig = bigw
		for(i in -hiddenHeight until heightWithoutHurryupFloor)
			for(j in 0 until width) {
				val b = getBlock(j, i) ?: continue
				if(b.cint==Block.COLOR_GEM_RAINBOW&&b.getAttribute(ATTRIBUTE.ERASE)) {
					val big = b.getAttribute(ATTRIBUTE.IGNORE_LINK)
					ret += detonateBomb(j, i, if(big) bigw else w, if(big) bigh else h)
					setLineFlag(i, false)
				}
			}

		return ret
	}

	/** 爆弾Blockごとの消去予約をする
	 * @param x x-coordinates
	 * @param y y-coordinates
	 * @return 消えるBlocks
	 */
	private fun detonateBomb(x:Int, y:Int, w:Int, h:Int):Int {
		//range={[4,3],
		//[3,0],[3,1],[3,2],[3,3],[4,4],[5,5],[5,5],[6,6],[6,6],[7,7]}
		val my = heightWithoutHurryupFloor
		var blocks = 0
		val r =
			listOf(
				if(y-h>-hiddenHeight) y-h else -hiddenHeight, if(y+h<my) y+h else my, if(x>w) x-w else 0,
				if(x+w<width) x+w else width
			)
		for(i in r[0]..r[1])
			for(j in r[2]..r[3]) {
				val b = getBlock(j, i) ?: continue
				if(!b.getAttribute(ATTRIBUTE.ERASE)) {
					blocks++
					b.setAttribute(true, ATTRIBUTE.ERASE)
				}
			}
		return blocks
	}

	/** Clear blocks got Erase Frag.
	 * @param gemType 1 = Bomb,2 = Spark
	 * @return Total number of blocks cleared.
	 */
	fun clearProceed(gemType:Int = 0):Int {
		var total = 0
		var b:Block?
		for(i in -hiddenHeight until heightWithoutHurryupFloor)
			for(j in 0 until width) {
				b = getBlock(j, i)
				if(b==null) continue
				if(b.hard>0) {
					b.hard--
					continue
				}
				if(b.getAttribute(ATTRIBUTE.ERASE)) {
					total++
					if(b.getAttribute(ATTRIBUTE.CONNECT_DOWN)) {
						getBlock(j, i+1)?.run {
							setAttribute(false, ATTRIBUTE.CONNECT_UP)
							setAttribute(true, ATTRIBUTE.BROKEN)
						}
					}
					if(b.getAttribute(ATTRIBUTE.CONNECT_UP)) {
						getBlock(j, i-1)?.run {
							setAttribute(false, ATTRIBUTE.CONNECT_DOWN)
							setAttribute(true, ATTRIBUTE.BROKEN)
						}
					}
					if(b.getAttribute(ATTRIBUTE.CONNECT_LEFT)) {
						getBlock(j-1, i)?.run {
							setAttribute(false, ATTRIBUTE.CONNECT_RIGHT)
							setAttribute(true, ATTRIBUTE.BROKEN)
						}
					}
					if(b.getAttribute(ATTRIBUTE.CONNECT_RIGHT)) {
						getBlock(j+1, i)?.run {
							setAttribute(false, ATTRIBUTE.CONNECT_LEFT)
							setAttribute(true, ATTRIBUTE.BROKEN)
						}
					}

					if(gemType!=0&&b.isGemBlock&&b.cint!=Block.COLOR_GEM_RAINBOW)
						setBlockColor(j, i, Block.COLOR_GEM_RAINBOW)
					else delBlock(j, i)
				}
			}
		return total
	}

	/** Check for clears of Color Connected straight line.
	 * @param size Minimum length of connect for a clear
	 * @param flag `true` to set Block.ATTRIBUTE.ERASE on blocks to be cleared.
	 * @param diagonals `true` to check diagonals,
	 * `false` to check only vertical and horizontal
	 * @param gemSame `true` to check gem blocks
	 * @return Total number of blocks that would be cleared.
	 */
	fun checkConnectLine(size:Int, flag:Boolean, diagonals:Boolean, gemSame:Boolean):Int {
		if(size<1) return 0
		if(flag) {
			setAllAttribute(false, ATTRIBUTE.ERASE)
			lineColorsCleared = emptyList()
			gemsCleared = 0
		}
		val colored = MutableList(0) {0}
		var total = 0
		var x:Int
		var y:Int
		var count:Int
		var blockColor:Int
		var lineColor:Int
		for(i in -hiddenHeight until heightWithoutHurryupFloor)
			for(j in 0 until width) {
				lineColor = getBlockColor(j, i, gemSame)
				if(lineColor==Block.COLOR_NONE||lineColor==Block.COLOR_INVALID) continue
				for(dir in 0 until if(diagonals) 3 else 2) {
					blockColor = lineColor
					x = j
					y = i
					count = 0
					while(lineColor==blockColor) {
						count++
						if(dir!=1) y++
						if(dir!=0) x++
						blockColor = getBlockColor(x, y, gemSame)
					}
					if(count<size) continue
					total += count
					if(!flag) continue
					if(count==size) colored.add(lineColor)
					x = j
					y = i
					blockColor = lineColor
					while(lineColor==blockColor) {
						getBlock(x, y)?.apply {
							if(hard>0) hard--
							else if(!getAttribute(ATTRIBUTE.ERASE)) {
								if(isGemBlock) gemsCleared++
								setAttribute(true, ATTRIBUTE.ERASE)
							}
						}
						if(dir!=1) y++
						if(dir!=0) x++
						blockColor = getBlockColor(x, y, gemSame)
					}
				}
			}
		lineColorsCleared = colored.toList()
		return total
	}

	/** Performs all cint clears of sufficient size containing at least one gem
	 * block.
	 * @param size Minimum size of cluster for a clear
	 * @param garbageClear `true` to clear garbage blocks adjacent to cleared
	 * clusters
	 * @return Total number of blocks cleared.
	 */
	fun gemClearColor(size:Int, garbageClear:Boolean, ignoreHidden:Boolean = false):Int {
		val temp = Field(this)
		var total = 0
		var b:Block?

		for(i in -hiddenHeight until heightWithoutHurryupFloor)
			for(j in 0 until width) {
				b = getBlock(j, i)
				if(b==null) continue
				if(!b.isGemBlock) continue
				val clear = temp.clearColor(j, i, false, garbageClear, true, ignoreHidden)
				if(clear>=size) {
					total += clear
					clearColor(j, i, false, garbageClear, true, ignoreHidden)
				}
			}
		return total
	}

	/** Performs all cint clears of sufficient size.
	 * @param size Minimum size of cluster for a clear
	 * @param garbageClear `true` to clear garbage blocks adjacent to cleared
	 * clusters
	 * @param gemSame `true` to check gem blocks
	 * @return Total number of blocks cleared.
	 */
	fun clearColor(size:Int, garbageClear:Boolean, gemSame:Boolean, ignoreHidden:Boolean = false):Int {
		val temp = Field(this)
		var total = 0
		for(i in (if(ignoreHidden) 0 else -hiddenHeight) until heightWithoutHurryupFloor)
			for(j in 0 until width) {
				val clear = temp.clearColor(j, i, false, garbageClear, gemSame, ignoreHidden)
				if(clear>=size) {
					total += clear
					clearColor(j, i, false, garbageClear, gemSame, ignoreHidden)
				}
			}
		return total
	}

	/** Clears the block at the given position as well as all adjacent blocks of
	 * the same cint, and any garbage blocks adjacent to the group if
	 * garbageClear is true.
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param flag `true` to set Block.ATTRIBUTE.ERASE to true on
	 * cleared blocks.
	 * @param garbageClear `true` to clear garbage blocks adjacent to
	 * cleared clusters
	 * @param gemSame `true` to check gem blocks
	 * @return The number of blocks cleared.
	 */
	fun clearColor(x:Int, y:Int, flag:Boolean, garbageClear:Boolean, gemSame:Boolean, ignoreHidden:Boolean):Int {
		val blockColor = getBlockColor(x, y, gemSame)
		if(blockColor==Block.COLOR_NONE||blockColor==Block.COLOR_INVALID) return 0
		return if(getBlock(x, y)?.getAttribute(ATTRIBUTE.GARBAGE)!=false) 0
		else clearColor(x, y, blockColor, flag, garbageClear, gemSame, ignoreHidden)
	}

	/** Note: This method is private because calling it with a targetColor
	 * parameter of BLOCK_COLOR_NONE or BLOCK_COLOR_INVALID may cause an
	 * infinite loop and crash the game. This check is handled by the above
	 * public method to avoid redundant checks. */
	private fun clearColor(
		x:Int, y:Int, targetColor:Int, flag:Boolean, garbageClear:Boolean, gemSame:Boolean,
		ignoreHidden:Boolean
	):Int {
		if(ignoreHidden&&y<0) return 0
		val blockColor = getBlockColor(x, y, gemSame)
		if(blockColor==Block.COLOR_INVALID||blockColor==Block.COLOR_NONE) return 0
		getBlock(x, y)?.also {b ->
			if(flag&&b.getAttribute(ATTRIBUTE.ERASE)) return@clearColor 0
			if(garbageClear&&b.getAttribute(ATTRIBUTE.GARBAGE)
				&&!b.getAttribute(ATTRIBUTE.WALL)
			)
				when {
					flag -> {
						b.setAttribute(true, ATTRIBUTE.ERASE)
						garbageCleared++
					}
					b.hard>0 -> b.hard--
					else -> delBlock(x, y)
				}
			if(blockColor!=targetColor) return@clearColor 0
			when {
				flag -> b.setAttribute(true, ATTRIBUTE.ERASE)
				b.hard>0 -> b.hard--
				else -> delBlock(x, y)
			}
		}
		return (1+clearColor(x+1, y, targetColor, flag, garbageClear, gemSame, ignoreHidden)
			+clearColor(x-1, y, targetColor, flag, garbageClear, gemSame, ignoreHidden)
			+clearColor(x, y+1, targetColor, flag, garbageClear, gemSame, ignoreHidden)
			+clearColor(x, y-1, targetColor, flag, garbageClear, gemSame, ignoreHidden))
	}

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
		for(y in -1*hiddenHeight until height)
			for(x in 0 until width)
				getBlock(x, y)?.let {
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
		return if(type==LineGravity.CASCADE_SLOW)
			doCascadeSlow()
		else doCascadeGravity()
	}

	/** Main routine for cascade gravity.
	 * @return `true` if something falls. `false` if
	 * nothing falls.
	 */
	fun doCascadeGravity():Boolean {
		var result = false

		setAllAttribute(false, ATTRIBUTE.CASCADE_FALL)

		for(i in heightWithoutHurryupFloor-1 downTo -hiddenHeight)
			for(j in 0 until width) getBlock(j, i)?.let {blk ->
				if(!blk.isEmpty&&!blk.getAttribute(ATTRIBUTE.ANTIGRAVITY)) {
					var fall = true
					checkBlockLink(j, i)

					for(k in heightWithoutHurryupFloor-1 downTo -hiddenHeight)
						for(l in 0 until width) {
							getBlock(l, k)?.let {
								if(!it.isEmpty&&it.getAttribute(ATTRIBUTE.TEMP_MARK)
									&&!it.getAttribute(ATTRIBUTE.CASCADE_FALL)
								) {
									val bBelow = getBlock(l, k+1)

									if(getCoordAttribute(l, k+1)==COORD_WALL||(bBelow!=null&&!bBelow.isEmpty
											&&!bBelow.getAttribute(ATTRIBUTE.TEMP_MARK))
									)
										fall = false

								}
							}
						}

					if(fall) {
						result = true
						for(k in heightWithoutHurryupFloor-1 downTo -hiddenHeight)
							for(l in 0 until width)
								getBlock(l, k)?.let {bTemp ->
									getBlock(l, k+1)?.let {bBelow ->
										if(getCoordAttribute(l, k+1)!=COORD_WALL&&!bTemp.isEmpty&&bBelow.isEmpty&&bTemp.getAttribute(
												ATTRIBUTE.TEMP_MARK
											)&&!bTemp.getAttribute(ATTRIBUTE.CASCADE_FALL)
										) {
											bTemp.setAttribute(false, ATTRIBUTE.TEMP_MARK)
											bTemp.setAttribute(true, ATTRIBUTE.CASCADE_FALL)
											bTemp.setAttribute(true, ATTRIBUTE.LAST_COMMIT)
											setBlock(l, k+1, bTemp)
											setBlock(l, k, Block())
										}
									}
								}
					}
				}
			}

		setAllAttribute(false, ATTRIBUTE.TEMP_MARK, ATTRIBUTE.CASCADE_FALL)

		/* for(int i = (hidden_height * -1); i < getHeightWithoutHurryupFloor();
		 * i++) { setLineFlag(i, false); } */

		return result
	}

	/** Routine for cascade gravity which checks from the top down for a slower
	 * fall animation.
	 * @return `true` if something falls. `false` if
	 * nothing falls.
	 */
	fun doCascadeSlow():Boolean {
		var result = false

		setAllAttribute(false, ATTRIBUTE.CASCADE_FALL)

		for(i in -hiddenHeight until heightWithoutHurryupFloor)
			for(j in 0 until width) {
				getBlock(j, i)?.let {blk ->
					if(!blk.getAttribute(ATTRIBUTE.ANTIGRAVITY)) {
						var fall = true
						checkBlockLink(j, i)

						for(k in heightWithoutHurryupFloor-1 downTo -hiddenHeight)
							for(l in 0 until width) getBlock(l, k)?.let {bTemp ->
								if(bTemp.getAttribute(ATTRIBUTE.TEMP_MARK)&&!bTemp.getAttribute(ATTRIBUTE.CASCADE_FALL)) {
									getBlock(l, k+1)?.let {bBelow ->
										if(getCoordAttribute(l, k+1)==COORD_WALL||!bBelow.getAttribute(ATTRIBUTE.TEMP_MARK))
											fall = false
									}
								}
							}

						if(fall) {
							result = true
							for(k in heightWithoutHurryupFloor-1 downTo -hiddenHeight)
								for(l in 0 until width) {
									val bTemp = getBlock(l, k)
									val bBelow = getBlock(l, k+1)

									if(getCoordAttribute(l, k+1)!=COORD_WALL&&bTemp!=null&&!bTemp.isEmpty&&bBelow!=null
										&&bBelow.isEmpty
										&&bTemp.getAttribute(ATTRIBUTE.TEMP_MARK)
										&&!bTemp.getAttribute(ATTRIBUTE.CASCADE_FALL)
									) {
										bTemp.setAttribute(false, ATTRIBUTE.TEMP_MARK)
										bTemp.setAttribute(true, ATTRIBUTE.CASCADE_FALL, ATTRIBUTE.LAST_COMMIT)
										setBlock(l, k+1, bTemp)
										setBlock(l, k, Block())
									}
								}
						}
					}
				}
			}

		setAllAttribute(false, ATTRIBUTE.TEMP_MARK, ATTRIBUTE.CASCADE_FALL)

		return result
	}

	/** Checks the connection of blocks and set "mark" to each block.
	 * @param x X coord
	 * @param y Y coord
	 */
	fun checkBlockLink(x:Int, y:Int) {
		setAllAttribute(false, ATTRIBUTE.TEMP_MARK)
		checkBlockLinkSub(x, y)
	}

	/** Subroutine for checkBlockLink.
	 * @param x X coord
	 * @param y Y coord
	 */
	private fun checkBlockLinkSub(x:Int, y:Int) {
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

	/** Checks the connection of blocks and set the "broken" flag to each block.
	 * It only affects to normal blocks. (ex. not square or gems)
	 * @param x X coord
	 * @param y Y coord
	 */
	fun setBlockLinkBroken(x:Int, y:Int) {
		setAllAttribute(false, ATTRIBUTE.TEMP_MARK)
		setBlockLinkBrokenSub(x, y)
	}

	/** Subroutine for setBlockLinkBrokenSub.
	 * @param x X coord
	 * @param y Y coord
	 */
	private fun setBlockLinkBrokenSub(x:Int, y:Int) {
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

	/** Checks the cint of blocks and set the connection flags to each
	 * block. */
	fun setBlockLinkByColor() {
		for(i in -hiddenHeight until heightWithoutHurryupFloor)
			for(j in 0 until width)
				setBlockLinkByColor(j, i)
	}

	/** Checks the cint of blocks and set the connection flags to each block.
	 * @param x X coord
	 * @param y Y coord
	 */
	fun setBlockLinkByColor(x:Int, y:Int) {
		setAllAttribute(false, ATTRIBUTE.TEMP_MARK)
		setBlockLinkByColorSub(x, y)
	}

	/** Subroutine for setBlockLinkByColorSub.
	 * @param x X coord
	 * @param y Y coord
	 */
	private fun setBlockLinkByColorSub(x:Int, y:Int) {
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

	/** HURRY UPの地面を一番下に追加
	 * @param lines 上げるLinescount
	 * @param skin 地面の絵柄
	 */
	fun addHurryupFloor(lines:Int, skin:Int) {
		if(lines>0)
			for(k in 0 until lines) {
				pushUp(1)

				for(j in 0 until width) {
					val blk = Block(
						Block.COLOR.BLACK, Block.TYPE.BLOCK, skin,
						ATTRIBUTE.WALL, ATTRIBUTE.GARBAGE, ATTRIBUTE.VISIBLE
					)
					setBlock(j, heightWithoutHurryupFloor-1, blk)
				}

				hurryupFloorLines++
			}
		else if(lines<0) {
			val l = minOf(lines, hurryupFloorLines)
			hurryupFloorLines -= l
			cutLine(height-1, l)
		}
	}

	/** @param row Row of blocks
	 * @return a String representing the row
	 */
	fun rowToString(row:List<Block?>?):String {
		val strResult = StringBuilder()

		row?.forEach {strResult.append(it ?: " ")}

		return "$strResult"
	}

	/** fieldを文字列に変換
	 * @return 文字列に変換されたfield
	 */
	fun fieldToString():String {
		var strResult = StringBuilder()

		for(i in height-1 downTo maxOf(-1, highestBlockY))
			strResult.append(rowToString(getRow(i)))

		// 終わりの0を取り除く
		while("$strResult".endsWith("0"))
			strResult = StringBuilder(strResult.substring(0, strResult.length-1))

		return "$strResult"
	}

	/** @param str String representing field state
	 * @param skin Block skin being used in this field
	 * @param isGarbage Row is garbage row
	 * @param isWall Row is a wall (i.e. hurry-up rows)
	 * @return The row array
	 */
	@JvmOverloads
	fun stringToRow(str:String, skin:Int = 0, isGarbage:Boolean = false, isWall:Boolean = false):Array<Block?> {
		val row = arrayOfNulls<Block>(width)
		for(j in 0 until width) {

			/* NullNoname's original approach from the old stringToField: If a
			 * character outside the row string is referenced, default to an
			 * empty block by ignoring the exception. */
			try {
				row[j] = Block(str[j], skin).apply {
					elapsedFrames = -1
					setAttribute(true, ATTRIBUTE.VISIBLE)
					setAttribute(true, ATTRIBUTE.OUTLINE)

					if(isGarbage)
					// not only sport one hole (i.e. TGM garbage)
						setAttribute(true, ATTRIBUTE.GARBAGE)
					if(isWall) setAttribute(true, ATTRIBUTE.WALL)
				}
			} catch(_:Exception) {
			}
		}

		return row
	}

	/** 文字列を元にfieldを変更
	 * @param str 文字列
	 * @param skin Blockの絵柄
	 * @param highestGarbageY 最も高いgarbage blockの位置
	 * @param highestWallY 最も高いHurryupBlockの位置
	 */
	@JvmOverloads
	fun stringToField(
		str:String, skin:Int = 0, highestGarbageY:Int = Integer.MAX_VALUE,
		highestWallY:Int = Integer.MAX_VALUE
	) {
		for(i in -1 until height) {
			val index = (height-1-i)*width
			/* Much like NullNoname's try/catch from the old stringToField that
			 * is now in stringToRow, we need to skip over substrings
			 * referenced
			 * outside the field string -- empty rows. */
			try {
				val substr = str.substring(index, minOf(str.length, index+width))
				val row = stringToRow(substr, skin, i>=highestGarbageY, i>=highestWallY)
				for(j in 0 until width)
					setBlock(j, i, row[j])
			} catch(e:Exception) {
				for(j in 0 until width)
					setBlock(j, i, Block())
			}

		}
	}

	/** @param row Row of blocks
	 * @return a String representing the [row] with attributes
	 */
	fun attrRowToString(row:List<Block?>?):String {
		val strResult = StringBuilder()

		row?.forEach {
			strResult.append((it?.cint ?: 0).toString(16)).append("/")
			strResult.append((it?.aint ?: 0).toString(16)).append(";")
		}

		return "$strResult"
	}

	/** Convert this field to a String with attributes
	 * @return a String representing the field with attributes
	 */
	fun attrFieldToString():String {
		var strResult = StringBuilder()

		for(i in height-1 downTo maxOf(-1, highestBlockY))
			strResult.append(attrRowToString(getRow(i)))
		while("$strResult".endsWith("0/0;"))
			strResult = StringBuilder(strResult.substring(0, strResult.length-4))

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

		for(i in -1 until height) {
			val index = (height-1-i)*width

			try {
				//String strTemp="";
				val strArray2 = List(width) {
					if(index+it<strArray.size) strArray[index+it] else ""
				}
				val row = attrStringToRow(strArray2, skin)
				for(j in 0 until width)
					setBlock(j, i, row[j])
			} catch(e:Exception) {
				for(j in 0 until width)
					setBlock(j, i, null)
			}

		}
	}

	/** fieldの文字列表現を取得 */
	override fun toString():String {
		val str = StringBuilder("${javaClass.name}@${Integer.toHexString(hashCode())}\n")

		for(i in -hiddenHeight until height) {
			str.append(String.format("%3d:", i))

			for(j in 0 until width) {
				val blk = getBlock(j, i)

				str.append(
					when(blk?.color) {
						null -> "."
						else -> "${blk.cint}"
					}
				)
			}

			str.append("\n")
		}

		return "$str"
	}

	fun checkColor(size:Int, flag:Boolean, garbageClear:Boolean, gemSame:Boolean, ignoreHidden:Boolean):Int {
		val temp = Field(this)
		var total = 0
		val colorsClearedArray = BooleanArray(7)
		if(flag) {
			setAllAttribute(false, ATTRIBUTE.ERASE)
			garbageCleared = 0
			colorClearExtraCount = 0
			colorsCleared = 0
			for(i in 0..6)
				colorsClearedArray[i] = false
		}

		for(i in -hiddenHeight until heightWithoutHurryupFloor)
			for(j in 0 until width) {
				val clear = temp.clearColor(j, i, false, garbageClear, gemSame, ignoreHidden)
				if(clear>=size) {
					total += clear
					if(flag) {
						val blockColor = getBlockColor(j, i, gemSame)
						clearColor(j, i, true, garbageClear, gemSame, ignoreHidden)
						colorClearExtraCount += clear-size
						if(blockColor in 2..8) colorsClearedArray[blockColor-2] = true
					}
				}
			}
		if(flag)
			for(i in 0..6)
				if(colorsClearedArray[i]) colorsCleared++
		return total
	}

	@JvmOverloads
	fun garbageDrop(
		engine:GameEngine, drop:Int, big:Boolean, hard:Int = 0, countdown:Int = 0, avoidColumn:Int = -1,
		color:Int = Block.COLOR_WHITE
	) {
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
			for(x in 0 until actualWidth)
				placeBlock[x] = true
			var start = actualWidth
			if(avoidColumn in 0 until actualWidth) {
				start--
				placeBlock[avoidColumn] = false
			}
			for(i in start downTo d+1) {
				do
					j = engine.random.nextInt(actualWidth)
				while(!placeBlock[j])
				placeBlock[j] = false
			}
		} else {
			for(x in 0 until actualWidth)
				placeBlock[x] = false
			for(i in 0 until d) {
				do
					j = engine.random.nextInt(actualWidth)
				while(placeBlock[j]&&j!=avoidColumn)
				placeBlock[j] = true
			}
		}

		for(x in 0 until actualWidth)
			if(placeBlock[x]) garbageDropPlace(x*bigMove, y, big, hard, color, countdown)
	}

	@JvmOverloads
	fun garbageDropPlace(
		x:Int, y:Int, big:Boolean, hard:Int, color:Int = Block.COLOR_WHITE,
		countdown:Int = 0
	):Boolean {
		val b = getBlock(x, y) ?: return false
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

	fun canCascade():Boolean {
		for(i in heightWithoutHurryupFloor-1 downTo -hiddenHeight)
			for(j in 0 until width) {
				val blk = getBlock(j, i)

				if(blk!=null&&!blk.isEmpty&&!blk.getAttribute(ATTRIBUTE.ANTIGRAVITY)) {
					var fall = true
					checkBlockLink(j, i)

					for(k in heightWithoutHurryupFloor-1 downTo -hiddenHeight)
						for(l in 0 until width) {
							val bTemp = getBlock(l, k)

							if(bTemp!=null&&!bTemp.isEmpty&&bTemp.getAttribute(ATTRIBUTE.TEMP_MARK)
								&&!bTemp.getAttribute(ATTRIBUTE.CASCADE_FALL)
							) {
								val bBelow = getBlock(l, k+1)

								if(getCoordAttribute(l, k+1)==COORD_WALL||(bBelow!=null&&!bBelow.isEmpty
										&&!bBelow.getAttribute(ATTRIBUTE.TEMP_MARK))
								)
									fall = false
							}
						}

					if(fall) return true
				}
			}
		return false
	}

	/** mainly for Physician, generate random maps */
	fun addRandomHoverBlocks(
		engine:GameEngine, count:Int, colors:List<Pair<Block.COLOR, Block.TYPE>>,
		minY:Int, avoidLines:Boolean, flashMode:Boolean = false
	) {
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
		for(y in minY until height)
			for(x in 0 until width)
				if(placeBlock[x][y-minY]) {
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
							do
								newColor = colorRand.nextInt(color.size)
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
		for(colorCount in colorCounts)
			if(colorCount>maxCount) {
				done = false
				break
			}
		var bestSwitch:Int
		var bestSwitchCount:Int
		var excess = 0
		var fill:Boolean
		while(!done) {
			done = true
			for(y in minY until height)
				for(x in 0 until width) {
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
						it<maxCount&&!listOf(
							getBlockColor(x, y-2), getBlockColor(x, y+2),
							getBlockColor(x-2, y), getBlockColor(x+2, y)
						).any {c -> !colors.any {p -> p.first==c}}
					}
					bestSwitch = -1
					bestSwitchCount = Integer.MAX_VALUE
					for(i in colorCounts.indices)
						if(canSwitch[i]&&colorCounts[i]<bestSwitchCount) {
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
				for(i in colors.indices)
					if(colors[i].first==blockColor) {
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
			for(colorCount in colorCounts)
				if(colorCount>maxCount) {
					balanced = false
					break
				}
			if(balanced) done = true
		}
		if(!flashMode) return
		val gemNeeded = colors.mapIndexed {i, it -> it.first.color&&colorCounts[i]>0}
			.toMutableList()
		done = !gemNeeded.any()
		while(!done) {
			val x = posRand.nextInt(width)
			val y = posRand.nextInt(placeHeight)+minY
			if(!placeBlock[x][y-minY]) continue
			val blockColor = getBlockColor(x, y)
			for(i in colors.indices)
				if(colors[i].first==blockColor) {
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

	fun addHoverBlock(x:Int, y:Int, mode:Pair<Block.COLOR, Block.TYPE>, skin:Int = 0):Boolean = setBlock(x, y,
		Block(mode, skin, ATTRIBUTE.ANTIGRAVITY, ATTRIBUTE.BROKEN, ATTRIBUTE.VISIBLE).apply {
			setAttribute(
				false, ATTRIBUTE.GARBAGE, ATTRIBUTE.ERASE,
				ATTRIBUTE.CONNECT_UP, ATTRIBUTE.CONNECT_DOWN, ATTRIBUTE.CONNECT_LEFT, ATTRIBUTE.CONNECT_RIGHT
			)
		})

	fun shuffleColors(blockColors:List<Block.COLOR>, numColors:Int, rand:Random) {
		var bC = blockColors.toMutableList()
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
		for(x in 0 until width)
			for(y in 0 until height) {
				var temp = getBlockColor(x, y, false)-1
				if(numColors==3&&temp>=3) temp--
				if(temp in 0 until maxX) setBlockColor(x, y, bC[temp])
			}
	}

	fun gemColorCheck(size:Int, flag:Boolean, garbageClear:Boolean, ignoreHidden:Boolean):Int {
		if(flag) setAllAttribute(false, ATTRIBUTE.ERASE)

		val temp = Field(this)
		var total = 0
		var b:Block?

		for(i in -hiddenHeight until heightWithoutHurryupFloor)
			for(j in 0 until width) {
				b = getBlock(j, i)
				if(b?.isGemBlock!=true) continue
				val clear = temp.clearColor(j, i, false, garbageClear, true, ignoreHidden)
				if(clear>=size) {
					total += clear
					if(flag) clearColor(j, i, true, garbageClear, true, ignoreHidden)
				}
			}
		return total
	}

	/** Instant avalanche, skips intermediate (cascade falling animation) steps.
	 * @return true if it affected the field at all, false otherwise.
	 */
	fun freeFall():Boolean {
		var y1:Int
		var y2:Int
		var result = false
		for(x in 0 until width) {
			y1 = height-1
			while(!getBlockEmpty(x, y1)&&y1>=-1*hiddenHeight)
				y1--
			y2 = y1
			while(getBlockEmpty(x, y2)&&y2>=-1*hiddenHeight)
				y2--
			while(y2>=-1*hiddenHeight) {
				setBlock(x, y1, getBlock(x, y2))
				setBlock(x, y2, Block())
				y1--
				y2--
				result = true
				while(getBlockEmpty(x, y2)&&y2>=-1*hiddenHeight)
					y2--
			}
		}
		return result
	}

	fun delEven() {
		for(y in highestBlockY until height)
			if(y and 1==0) delLine(y)
	}

	fun delLower() {
		val rows = height-highestBlockY+1 shr 1
		for(i in 1..rows)
			delLine(height-i)
	}

	fun delUpper() {
		val rows = ((height-highestBlockY)/2.0).roundToLong().toInt()
		// I think this rounds up.
		val g = highestBlockY
		for(y in 0 until rows) delLine(g+y)

	}

	fun delLine(y:Int) {
		for(x in 0 until width) {
			getBlock(x, y)?.hard = 0
		}
		setLineFlag(y, true)
	}

	fun moveLeft() {
		var x1:Int
		var x2:Int
		for(y in highestBlockY until height) {
			x1 = 0
			while(!getBlockEmpty(x1, y))
				x1++
			x2 = x1
			while(x2<width) {
				while(getBlockEmpty(x2, y)&&x2<width)
					x2++
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
		for(y in highestBlockY until height) {
			x1 = width-1
			while(!getBlockEmpty(x1, y))
				x1--
			x2 = x1
			while(x2>=0) {
				while(getBlockEmpty(x2, y)&&x2>=0)
					x2--
				setBlock(x1, y, getBlock(x2, y))
				setBlock(x2, y, Block())
				x1--
				x2--
			}
		}
	}

	fun negaField() {
		for(y in highestBlockY until height) for(x in 0 until width) {
			if(getBlockEmpty(x, y)) garbageDropPlace(x, y, false, 0) // TODO: Set color
			else setBlock(x, y, null)
		}
	}

	fun flipVertical() {
		val field2 = Field(this)
		var yMin = highestBlockY-hiddenHeight
		var yMax = hiddenHeight+height-1
		while(yMin<yMax) {
			for(x in 0 until width) {
				setBlock(x, yMin, field2.getBlock(x, yMin))
				setBlock(x, yMax, field2.getBlock(x, yMax))
			}
			yMin++
			yMax--
		}
	}

	fun mirror() {
		var temp:Block?
		var y = highestBlockY
		while(y<height) {
			var xMin = 0
			var xMax = width-1
			while(xMin<xMax) {
				temp = getBlock(xMin, y)
				setBlock(xMin, y, getBlock(xMax, y))
				setBlock(xMax, y, temp)
				xMin++
				xMax--
			}
			y--
		}
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
	}
}
