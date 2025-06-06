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

import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.GeneralUtil.aNum
import mu.nu.nullpo.util.GeneralUtil.toAlphaNum

/** Block */
@kotlinx.serialization.Serializable
class Block @JvmOverloads constructor(
	/** Block color */
	var color:COLOR? = null,
	/** Block type */
	var type:TYPE = TYPE.BLOCK,
	/** Blockの絵柄 */
	var skin:Int = 0,
	/** Blockの属性 */
	var aint:Int = 3
) {
	var offsetY = 0f
	/** Block color integer for processing */
	var cint:Int
		get() = colorNumber(color, type, getAttribute(ATTRIBUTE.BONE), item)
		set(v) {
			intToColor(v).let {
				color = it.first
				type = it.second
			}
		}

	/** Block color integer for Rendering */
	val drawColor:Int
		get() = when(cint) {
			COLOR_GEM_RAINBOW -> COLOR_GEM_RED+rainbowPhase/3
			COLOR_RAINBOW -> COLOR_RED+rainbowPhase/3
			else -> cint
		}

	constructor(color:COLOR?, type:TYPE, skin:Int = 0, vararg attrs:ATTRIBUTE):
		this(color, type, skin, attrs.fold(0) {x, y -> x or y.bit})

	constructor(color:COLOR?, skin:Int, vararg attrs:ATTRIBUTE):this(color, TYPE.BLOCK, skin, *attrs)
	constructor(mode:Pair<COLOR?, TYPE>, skin:Int = 0, aint:Int):this(mode.first, mode.second, skin, aint)
	constructor(mode:Pair<COLOR?, TYPE>, skin:Int = 0, vararg attrs:ATTRIBUTE):this(mode.first, mode.second, skin, *attrs)
	constructor(cint:Int = 0, skin:Int = 0, aint:Int):this(intToColor(cint), skin, aint)
	constructor(cint:Int = 0, skin:Int = 0, vararg attrs:ATTRIBUTE):this(intToColor(cint), skin, *attrs)
	constructor(char:Char, skin:Int = 0, vararg attrs:ATTRIBUTE):this(char.aNum, skin, *attrs)

	override operator fun equals(other:Any?):Boolean = other is Block?&&color==other?.color&&type==other?.type

	//val aset:Set<ATTRIBUTE> get() = aint.values{it.bit}.sum()

	/** 固定してから経過した frame count */
	var elapsedFrames = 0

	/** Blockの暗さ, または明るさ (0.03だったら3%暗く, -0.05だったら5%明るい) */
	var darkness = 0f

	/** 透明度 (1fで不透明, 0fで完全に透明) */
	var alpha:Float = 1f

	/** ゲームが始まってから何番目に置いたBlockか (負countだったら初期配置やgarbage block) */
	var placeNum:Int = -1

	/** アイテム enum */
	var item:Item? = null
	/** アイテム number */
	var iNum
		get() = item?.let {it.ordinal+1}?:0
		set(value) {
			item = if(value in 1..items.size) items[value-1] else null
		}

	/** Number of extra clears required before block is erased */
	var hard = 0

	/** Counter for blocks that count down before some effect occurs */
	var countdown = 0

	/** Color to turn into when garbage block turns into a regular block */
	var secondaryColor = COLOR.BLACK

	/** Bonus value awarded when cleared */
	var bonusValue = 0

	/** このBlockが空白かどうか判定
	 * @return このBlockが空白だったらtrue
	 */
	val isEmpty get() = color==null

	/** このBlockが宝石Blockかどうか判定
	 * @return このBlockが宝石Blockだったらtrue
	 */
	val isGemBlock get() = type===TYPE.GEM

	/** Checks to see if `this` is a gold square block
	 * @return `true` if the block is a gold square block
	 */
	val isGoldSquareBlock get() = type===TYPE.SQUARE_GOLD

	/** Checks to see if `this` is a silver square block
	 * @return `true` if the block is a silver square block
	 */
	val isSilverSquareBlock get() = type===TYPE.SQUARE_SILVER

	/** Checks to see if `this` is a normal block (gray to purple)
	 * @return `true` if the block is a normal block
	 */
	val isNormalBlock get() = type===TYPE.BLOCK

	/** 設定をReset to defaults */
	fun reset(del:Boolean = false) {
		if(del) color = null
		type = TYPE.BLOCK
		skin = 0
		aint = 3
		elapsedFrames = 0
		darkness = 0f
		alpha = 1f
		placeNum = -1
		iNum = 0
		hard = 0
		countdown = 0
		secondaryColor = COLOR.BLACK
		bonusValue = 0
	}

	/** Copy constructor
	 * @param b Copy source
	 */
	constructor(b:Block?):this(b?.color, b?.type?:TYPE.BLOCK, b?.skin?:0) {
		replace(b)
	}

	/** 設定を[b]からコピー */
	fun replace(b:Block?) {
		b?.let {
			color = it.color
			type = it.type
			skin = it.skin
			aint = it.aint
			elapsedFrames = it.elapsedFrames
			darkness = it.darkness
			alpha = it.alpha
			placeNum = it.placeNum
			iNum = it.iNum
			hard = it.hard
			countdown = it.countdown
			secondaryColor = it.secondaryColor
			bonusValue = it.bonusValue
		}?:reset(true)
	}

	/** 指定した属性 stateを調べる
	 * @param attr 調べたい属性
	 * @return 指定した属性がすべてセットされている場合はtrue
	 */
	fun getAttribute(vararg attr:ATTRIBUTE):Boolean = (aint and attr.fold(0) {x, y -> x or y.bit})>0

	/** 属性を変更する
	 * @param attrs 変更したい属性
	 * @param status 変更後 state
	 */
	fun setAttribute(status:Boolean, vararg attrs:ATTRIBUTE) =
		setAttribute(status, attrs.fold(0) {x, y -> x or y.bit})

	fun setAttribute(status:Boolean, attr:Int) {
		aint = if(status) aint or attr else aint and attr.inv()
	}

	@Deprecated("Argument Swapped", ReplaceWith("setAttribute(status, attr)"))
	fun setAttribute(attr:Int, status:Boolean) = setAttribute(status, attr)

	/** @return the character representing the color of this block
	 */
	fun toChar():Char = cint.toAlphaNum

	override fun toString():String = "${toChar()}"
	override fun hashCode():Int = (color?.hashCode()?:0)
		.let {31*it+type.hashCode()}
		.let {31*it+skin}
		.let {31*it+aint}
		.let {31*it+elapsedFrames}
		.let {31*it+darkness.hashCode()}
		.let {31*it+alpha.hashCode()}
		.let {31*it+placeNum}
		.let {31*it+(item?.hashCode()?:0)}
		.let {31*it+hard}
		.let {31*it+countdown}
		.let {31*it+secondaryColor.hashCode()}
		.let {31*it+bonusValue}

	enum class TYPE(val pos:Byte = 0) { BLOCK, GEM, SQUARE_GOLD, SQUARE_SILVER, ITEM }
	enum class COLOR(val color:Boolean = true) {
		BLACK(false), WHITE(false), RED, ORANGE, YELLOW, GREEN, CYAN, BLUE, PURPLE, RAINBOW(false);

		companion object {
			val all = entries
			/** 通常のBlock colorのMaximum count */
			val COUNT = all.size
			/** 宝石になりうるBlock colorのMaximum count */
			val COLOR_NUM = all.count {it.color}
			val ALL_COLOR_NUM = COUNT+COLOR_NUM
			fun colors() = all.filter {it.color}
		}
	}

	enum class ATTRIBUTE {
		/** Block表示あり */
		VISIBLE,
		/** 枠線表示あり */
		OUTLINE,
		/** 骨Block */
		BONE,
		/** 上のBlockと繋がっている */
		CONNECT_UP,
		/** 下のBlockと繋がっている */
		CONNECT_DOWN,
		/** 左のBlockと繋がっている */
		CONNECT_LEFT,
		/** 右のBlockと繋がっている */
		CONNECT_RIGHT,
		/** 自分で置いたBlock */
		SELF_PLACED,
		/** 壊れたピースの一部分 */
		BROKEN,
		/** Ojama block */
		GARBAGE,
		/** 壁 */
		WALL,
		/** 消える予定のBlock */
		ERASE,
		/** Temporary mark for block linking check algorithm */
		TEMP_MARK,
		/** "Block has fallen" flag for cascade gravity */
		CASCADE_FALL,
		/** Antigravity flag (The block will not fall by gravity) */
		ANTIGRAVITY,
		/** Last commit flag -- block was part of last placement or cascade */
		LAST_COMMIT,
		/** Ignore block connections (for Avalanche modes) */
		IGNORE_LINK,
		/** Placed with Big Piece, or Combined as Big Bomb*/
		BIG;

		val bit:Int get() = 1 shl ordinal
	}

	companion object {
		private val items = Item.entries
		/** Block colorの定数 */
		const val COLOR_INVALID = -2
		const val COLOR_NONE = -1
		const val COLOR_BLACK = 0
		const val COLOR_WHITE = 1
		const val COLOR_RED = 2
		const val COLOR_ORANGE = 3
		const val COLOR_YELLOW = 4
		const val COLOR_GREEN = 5
		const val COLOR_CYAN = 6
		const val COLOR_BLUE = 7
		const val COLOR_PURPLE = 8
		const val COLOR_GEM_RED = 9
		const val COLOR_GEM_ORANGE = 10
		const val COLOR_GEM_YELLOW = 11
		const val COLOR_GEM_GREEN = 12
		const val COLOR_GEM_CYAN = 13
		const val COLOR_GEM_BLUE = 14
		const val COLOR_GEM_PURPLE = 15
		const val COLOR_SQUARE_GOLD_1 = 16
		const val COLOR_SQUARE_GOLD_2 = 17
		const val COLOR_SQUARE_GOLD_3 = 18
		const val COLOR_SQUARE_GOLD_4 = 19
		const val COLOR_SQUARE_GOLD_5 = 20
		const val COLOR_SQUARE_GOLD_6 = 21
		const val COLOR_SQUARE_GOLD_7 = 22
		const val COLOR_SQUARE_GOLD_8 = 23
		const val COLOR_SQUARE_GOLD_9 = 24
		const val COLOR_SQUARE_SILVER_1 = 25
		const val COLOR_SQUARE_SILVER_2 = 26
		const val COLOR_SQUARE_SILVER_3 = 27
		const val COLOR_SQUARE_SILVER_4 = 28
		const val COLOR_SQUARE_SILVER_5 = 29
		const val COLOR_SQUARE_SILVER_6 = 30
		const val COLOR_SQUARE_SILVER_7 = 31
		const val COLOR_SQUARE_SILVER_8 = 32
		const val COLOR_SQUARE_SILVER_9 = 33
		const val COLOR_RAINBOW = 34
		const val COLOR_GEM_RAINBOW = 35

		val MAX_ITEM get() = items.size

		@Deprecated("moved", ReplaceWith("Block.COLOR.COUNT"))
		val COLOR_COUNT
			get() = COLOR.COUNT

		@Deprecated("moved", ReplaceWith("Block.COLOR.ALL_COLOR_NUM"))
		val COLOR_EXT_COUNT
			get() = COLOR.ALL_COLOR_NUM

		/** Color-shift phase for rainbow blocks */
		var rainbowPhase = 0

		fun colorNumber(color:COLOR?, type:TYPE, isBone:Boolean = false, item:Item? = null):Int =
			if(color==COLOR.RAINBOW) {
				if(type==TYPE.GEM) COLOR_GEM_RAINBOW
				else COLOR_RAINBOW
			} else {
				val ci:Int = (color?.ordinal?:0)
				when(type) {
					TYPE.BLOCK -> if(color==COLOR.BLACK&&isBone) COLOR_WHITE
					else ci
					TYPE.GEM -> if(color?.color!=true) {
						COLOR_GEM_RAINBOW
					} else ci+COLOR_GEM_RED-COLOR_RED
					TYPE.SQUARE_SILVER -> ci+COLOR_SQUARE_SILVER_1
					TYPE.SQUARE_GOLD -> ci+COLOR_SQUARE_GOLD_1
					TYPE.ITEM -> item?.color?.ordinal?:0
				}
			}

		fun intToColor(v:Int):Pair<COLOR?, TYPE> = when(v) {
			in COLOR_WHITE..COLOR_PURPLE -> {
				COLOR.all[v] to TYPE.BLOCK
			}
			in COLOR_GEM_RED..COLOR_GEM_PURPLE -> {
				COLOR.all[v-COLOR_PURPLE] to TYPE.GEM
			}
			COLOR_RAINBOW -> {
				COLOR.RAINBOW to TYPE.BLOCK
			}
			COLOR_GEM_RAINBOW -> {
				COLOR.RAINBOW to TYPE.GEM
			}
			in COLOR_SQUARE_SILVER_1..COLOR_SQUARE_SILVER_9 -> {
				COLOR.all[v-COLOR_SQUARE_SILVER_1] to TYPE.SQUARE_SILVER
			}
			in COLOR_SQUARE_GOLD_1..COLOR_SQUARE_GOLD_9 -> {
				COLOR.all[v-COLOR_SQUARE_GOLD_1] to TYPE.SQUARE_GOLD
			}
			else -> null to TYPE.BLOCK
		}

		fun charToColorNum(c:Char):Int = c.aNum

		fun updateRainbowPhase(time:Int) {
			rainbowPhase = time%21
		}

		fun updateRainbowPhase(engine:GameEngine) {
			if(engine.timerActive) updateRainbowPhase(engine.statistics.time)
			else if(++rainbowPhase>=21) rainbowPhase = 0
		}

		//@Deprecated("enumed : COLOR.ungem",level=DeprecationLevel.WARNING)
		fun gemToNormalColor(color:Int):Int = when(color) {
			in COLOR_GEM_RED..COLOR_GEM_PURPLE -> color-7
			COLOR_GEM_RAINBOW -> COLOR_RAINBOW
			else -> color
		}
	}
}
