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

import mu.nu.nullpo.game.component.Block.TYPE.*
import mu.nu.nullpo.game.play.GameEngine
import java.io.Serializable
import kotlin.math.pow
import kotlin.math.roundToInt

/** Block */
class Block(
	/** Block color */
	var color:COLOR? = null,
	/** Block type */
	var type:TYPE = BLOCK,
	/** Blockの絵柄 */
	var skin:Int = 0,
	/** Blockの属性 */
	vararg attrs:ATTRIBUTE):Serializable {
	/** Blockの属性 */
	var aint:Int = attrs.fold(0) {x, y -> x or y.bit}

	/** Block color integer */
	var cint:Int
		get() = if(color==COLOR.RAINBOW) {
			if(type==GEM) BLOCK_COLOR_GEM_RAINBOW
			else BLOCK_COLOR_RAINBOW
		} else {
			val ci:Int = (color?.ordinal ?: 0)
			when(type) {
				BLOCK -> if(color==COLOR.BLACK&&getAttribute(ATTRIBUTE.BONE)) BLOCK_COLOR_GRAY
				else ci
				GEM -> if(color==COLOR.BLACK||color==COLOR.WHITE) {
					BLOCK_COLOR_GEM_RAINBOW
				} else ci+BLOCK_COLOR_GEM_RED-BLOCK_COLOR_RED
				SQUARE_SILVER -> ci+BLOCK_COLOR_SQUARE_SILVER_1
				SQUARE_GOLD -> ci+BLOCK_COLOR_SQUARE_GOLD_1
			}
		}
		set(v) {
			color = when(v) {
				in BLOCK_COLOR_GRAY..BLOCK_COLOR_PURPLE -> {
					type = BLOCK;COLOR.values()[v]
				}
				BLOCK_COLOR_RAINBOW -> {
					type = BLOCK;COLOR.RAINBOW
				}
				in BLOCK_COLOR_GEM_RED..BLOCK_COLOR_GEM_PURPLE -> {
					type = GEM
					COLOR.values()[v-BLOCK_COLOR_GEM_RED]
				}
				BLOCK_COLOR_GEM_RAINBOW -> {
					type = GEM;COLOR.RAINBOW
				}
				in BLOCK_COLOR_SQUARE_SILVER_1..BLOCK_COLOR_SQUARE_SILVER_9 -> {
					type = SQUARE_SILVER
					COLOR.values()[v-BLOCK_COLOR_SQUARE_SILVER_1]
				}
				in BLOCK_COLOR_SQUARE_GOLD_1..BLOCK_COLOR_SQUARE_GOLD_9 -> {
					type = SQUARE_GOLD
					COLOR.values()[v-BLOCK_COLOR_SQUARE_GOLD_1]
				}
				else -> null
			}
		}

	constructor():this(null, BLOCK, 0)
	constructor(color:COLOR?, skin:Int, vararg attrs:ATTRIBUTE):this(color, BLOCK, skin, *attrs)
	constructor(cint:Int = 0, skin:Int = 0, vararg attrs:ATTRIBUTE):this(null, BLOCK, skin, *attrs) {
		this.cint = cint
	}
	//val aset:Set<ATTRIBUTE> get() = aint.values{it.bit}.sum()

	/** 固定してから経過した frame count */
	var elapsedFrames:Int = 0

	/** Blockの暗さ, または明るさ (0.03だったら3%暗く, -0.05だったら5%明るい) */
	var darkness:Float = 0f

	/** 透明度 (1fで不透明, 0.0fで完全に透明) */
	var alpha:Float = 1f

	/** ゲームが始まってから何番目に置いたBlockか (負countだったら初期配置やgarbage block) */
	var pieceNum:Int = -1; protected set

	/** アイテム number */
	var item:Int = 0

	/** Number of extra clears required before block is erased */
	var hard:Int = 0

	/** Counter for blocks that count down before some effect occurs */
	var countdown:Int = 0

	/** Color to turn into when garbage block turns into a regular block */
	var secondaryColor:Int = 0

	/** Bonus value awarded when cleared */
	var bonusValue:Int = 0

	/** このBlockが空白かどうか判定
	 * @return このBlockが空白だったらtrue
	 */
	val isEmpty:Boolean get() = color==null

	/** このBlockが宝石Blockかどうか判定
	 * @return このBlockが宝石Blockだったらtrue
	 */
	val isGemBlock:Boolean get() = type===GEM

	/** Checks to see if `this` is a gold square block
	 * @return `true` if the block is a gold square block
	 */
	val isGoldSquareBlock:Boolean get() = type===SQUARE_GOLD

	/** Checks to see if `this` is a silver square block
	 * @return `true` if the block is a silver square block
	 */
	val isSilverSquareBlock:Boolean get() = type===SQUARE_SILVER

	/** Checks to see if `this` is a normal block (gray to purple)
	 * @return `true` if the block is a normal block
	 */
	val isNormalBlock:Boolean get() = type===BLOCK

	val drawColor:Int
		get() = when(cint) {
			BLOCK_COLOR_GEM_RAINBOW -> BLOCK_COLOR_GEM_RED+rainbowPhase/3
			BLOCK_COLOR_RAINBOW -> BLOCK_COLOR_RED+rainbowPhase/3
			else -> cint
		}

	/** 設定をReset to defaults */
	fun reset(del:Boolean = false) {
		if(del) color = null
		type = BLOCK
		skin = 0
		aint = 0
		elapsedFrames = 0
		darkness = 0f
		alpha = 1f
		pieceNum = -1
		item = 0
		hard = 0
		countdown = 0
		secondaryColor = 0
		bonusValue = 0
	}

	/** Copy constructor
	 * @param b Copy source
	 */
	constructor(b:Block?):this(b?.color, b?.type ?: BLOCK, b?.skin ?: 0) {
		copy(b)
	}

	/** 設定を他のBlockからコピー
	 * @param b Copy source
	 */
	fun copy(b:Block?) {
		b?.let {
			color = b.color
			type = b.type
			skin = b.skin
			aint = b.aint
			elapsedFrames = b.elapsedFrames
			darkness = b.darkness
			alpha = b.alpha
			pieceNum = b.pieceNum
			item = b.item
			hard = b.hard
			countdown = b.countdown
			secondaryColor = b.secondaryColor
			bonusValue = b.bonusValue
		} ?: reset(true)
	}

	/** 指定した属性 stateを調べる
	 * @param attr 調べたい属性
	 * @return 指定した属性がすべてセットされている場合はtrue
	 */
	fun getAttribute(vararg attr:ATTRIBUTE):Boolean = (aint and attr.fold(0) {x, y -> x or y.bit})!=0

	/** 属性を変更する
	 * @param attrs 変更したい属性
	 * @param status 変更後 state
	 */
	fun setAttribute(status:Boolean, vararg attrs:ATTRIBUTE) {
		val attr=attrs.fold(0) {x, y -> x or y.bit}
		aint = if(status) aint or attr else aint and attr.inv()
	}

	fun setAttribute(status:Boolean, attr:Int) {
		aint = if(status) aint or attr else aint and attr.inv()
	}

	/** @return the character representing the color of this block
	 */
	fun blockToChar():Char =//'0'-'9','A'-'Z' represent colors 0-35.
	//Colors beyond that would follow the ASCII table starting at '['.
		if(cint>=10) ('A'.toInt()+(cint-10)).toChar() else ('0'.toInt()+maxOf(0, cint)).toChar()

	override fun toString():String = ""+blockToChar()

	enum class TYPE { BLOCK, GEM, SQUARE_GOLD, SQUARE_SILVER }
	enum class COLOR {
		BLACK, WHITE, RED, ORANGE, YELLOW, GREEN, CYAN, BLUE, PURPLE, RAINBOW;

		val type:TYPE = when {
			name.contains("GEM_") -> GEM
			name.contains("SQUARE_SILVER") -> SQUARE_SILVER
			name.contains("SQUARE_GOLD") -> SQUARE_GOLD
			else -> BLOCK
		}
	}

	enum class ITEM { RANDOM }
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
		SELFPLACED,
		/** 壊れたピースの一部分 */
		BROKEN,
		/** ojama block */
		GARBAGE,
		/** 壁 */
		WALL,
		/** 消える予定のBlock */
		ERASE,
		/** Temporary mark for block linking check algorithm */
		TEMP_MARK,
		/** "Block has fallen" flag for cascade gravity */
		CASCADE_FALL,
		/** Anti-gravity flag (The block will not fall by gravity) */
		ANTIGRAVITY,
		/** Last commit flag -- block was part of last placement or cascade */
		LAST_COMMIT,
		/** Ignore block connections (for Avalanche modes) */
		IGNORE_BLOCKLINK;

		val bit:Int get() = 2.0.pow(ordinal).roundToInt()

	}

	companion object {
		/** Serial version ID */
		private const val serialVersionUID = -7126899262733374545L

		/** Block colorの定数 */
		const val BLOCK_COLOR_INVALID = -1
		const val BLOCK_COLOR_NONE = 0
		const val BLOCK_COLOR_GRAY = 1
		const val BLOCK_COLOR_RED = 2
		const val BLOCK_COLOR_ORANGE = 3
		const val BLOCK_COLOR_YELLOW = 4
		const val BLOCK_COLOR_GREEN = 5
		const val BLOCK_COLOR_CYAN = 6
		const val BLOCK_COLOR_BLUE = 7
		const val BLOCK_COLOR_PURPLE = 8
		const val BLOCK_COLOR_GEM_RED = 9
		const val BLOCK_COLOR_GEM_ORANGE = 10
		const val BLOCK_COLOR_GEM_YELLOW = 11
		const val BLOCK_COLOR_GEM_GREEN = 12
		const val BLOCK_COLOR_GEM_CYAN = 13
		const val BLOCK_COLOR_GEM_BLUE = 14
		const val BLOCK_COLOR_GEM_PURPLE = 15
		const val BLOCK_COLOR_SQUARE_GOLD_1 = 16
		const val BLOCK_COLOR_SQUARE_GOLD_2 = 17
		const val BLOCK_COLOR_SQUARE_GOLD_3 = 18
		const val BLOCK_COLOR_SQUARE_GOLD_4 = 19
		const val BLOCK_COLOR_SQUARE_GOLD_5 = 20
		const val BLOCK_COLOR_SQUARE_GOLD_6 = 21
		const val BLOCK_COLOR_SQUARE_GOLD_7 = 22
		const val BLOCK_COLOR_SQUARE_GOLD_8 = 23
		const val BLOCK_COLOR_SQUARE_GOLD_9 = 24
		const val BLOCK_COLOR_SQUARE_SILVER_1 = 25
		const val BLOCK_COLOR_SQUARE_SILVER_2 = 26
		const val BLOCK_COLOR_SQUARE_SILVER_3 = 27
		const val BLOCK_COLOR_SQUARE_SILVER_4 = 28
		const val BLOCK_COLOR_SQUARE_SILVER_5 = 29
		const val BLOCK_COLOR_SQUARE_SILVER_6 = 30
		const val BLOCK_COLOR_SQUARE_SILVER_7 = 31
		const val BLOCK_COLOR_SQUARE_SILVER_8 = 32
		const val BLOCK_COLOR_SQUARE_SILVER_9 = 33
		const val BLOCK_COLOR_RAINBOW = 34
		const val BLOCK_COLOR_GEM_RAINBOW = 35

		val MAX_ITEM get() = ITEM.values().size

		/** 通常のBlock colorのMaximumcount */
		val BLOCK_COLOR_COUNT get() = COLOR.values().count {it.type==BLOCK}

		/** 通常＋宝石Block colorのMaximumcount */
		val BLOCK_COLOR_EXT_COUNT get() = COLOR.values().count {it.type==BLOCK||it.type==GEM}

		/** Block表示あり */
		const val BLOCK_ATTRIBUTE_VISIBLE = 1

		/** 枠線表示あり */
		const val BLOCK_ATTRIBUTE_OUTLINE = 2

		/** 骨Block */
		const val BLOCK_ATTRIBUTE_BONE = 4

		/** 上のBlockと繋がっている */
		const val BLOCK_ATTRIBUTE_CONNECT_UP = 8

		/** 下のBlockと繋がっている */
		const val BLOCK_ATTRIBUTE_CONNECT_DOWN = 16

		/** 左のBlockと繋がっている */
		const val BLOCK_ATTRIBUTE_CONNECT_LEFT = 32

		/** 右のBlockと繋がっている */
		const val BLOCK_ATTRIBUTE_CONNECT_RIGHT = 64

		/** 自分で置いたBlock */
		const val BLOCK_ATTRIBUTE_SELFPLACED = 128

		/** 壊れたピースの一部分 */
		const val BLOCK_ATTRIBUTE_BROKEN = 256

		/** ojama block */
		const val BLOCK_ATTRIBUTE_GARBAGE = 512

		/** 壁 */
		const val BLOCK_ATTRIBUTE_WALL = 1024

		/** 消える予定のBlock */
		const val BLOCK_ATTRIBUTE_ERASE = 2048

		/** Temporary mark for block linking check algorithm */
		const val BLOCK_ATTRIBUTE_TEMP_MARK = 4096

		/** "Block has fallen" flag for cascade gravity */
		const val BLOCK_ATTRIBUTE_CASCADE_FALL = 8192

		/** Anti-gravity flag (The block will not fall by gravity) */
		const val BLOCK_ATTRIBUTE_ANTIGRAVITY = 16384

		/** Last commit flag -- block was part of last placement or cascade */
		const val BLOCK_ATTRIBUTE_LAST_COMMIT = 32768

		/** Ignore block connections (for Avalanche modes) */
		const val BLOCK_ATTRIBUTE_IGNORE_BLOCKLINK = 65536

		/** Color-shift phase for rainbow blocks */
		var rainbowPhase = 0

		/** @param c A character representing a block
		 * @return The int representing the block's color
		 */
		fun charToBlockColor(c:Char):Int {
			var blkColor:Int = Character.digit(c, 36)

			//With a radix of 36, the digits encompass '0'-'9','A'-'Z'.
			//With a radix higher than 36, we can also have characters 'a'-'z' represent digits.

			//Given the current implementation of other functions, I assumed that
			//if we needed additional BLOCK_COLOR values, it would follow from 'Z'->'['
			//in the ASCII chart.
			if(blkColor==-1) blkColor = c-'['+36
			return blkColor
		}

		fun updateRainbowPhase(time:Int) {
			rainbowPhase = time%21
		}

		fun updateRainbowPhase(engine:GameEngine) {
			if(engine.timerActive) updateRainbowPhase(engine.statistics.time)
			else if(++rainbowPhase>=21) rainbowPhase = 0
		}

		//@Deprecated("enumed : COLOR.ungem",level=DeprecationLevel.WARNING)
		fun gemToNormalColor(color:Int):Int = when(color) {
			in BLOCK_COLOR_GEM_RED..BLOCK_COLOR_GEM_PURPLE -> color-7
			BLOCK_COLOR_GEM_RAINBOW -> BLOCK_COLOR_RAINBOW
			else -> color
		}
	}
}
