/*
 Copyright (c) 2024, NullNoname
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
import mu.nu.nullpo.game.play.GameEngine
import zeroxfc.nullpo.custom.libs.shotgunField
import kotlin.reflect.full.createInstance
import mu.nu.nullpo.game.component.Block.COLOR as BCOLOR

@Suppress("ClassName")
@Serializable
sealed class Item(val id:Int, val showName:String? = null,
	/** the item persists duration
	 * - [duration]=3 ,[isSec]:false = 3 placements
	 * - [duration]=3 ,[isSec]:true = 5 seconds
	 */
	val duration:Int = 0,
	/** if this is true, [duration] as frame */
	val isSec:Boolean = false,
	val type:Type = Type.INTERRUPT,
	val color:BCOLOR = if(type==Type.SELF) BCOLOR.BLUE else BCOLOR.RED) {

	constructor(id:Int, showName:String):this(id, showName, type = Type.SELF)

	var lifetime = duration
	open fun statEffect(e:GameEngine):Boolean = false

	/**
	 * Interrupts the game state
	 */
	open fun statInterrupt(e:GameEngine):Boolean = false
	override fun toString():String = this.showName?:this::class.simpleName?:""
	val ordinal get() = entries.indexOf(this)

	/** Flip Opponents Field horizontally 3 times */
	class MIRROR(var maxCount:Int = 3):Item(0, "FLIP MIRROR", 3) {
		private var count = 0
		private var tempField:Field = Field()
		override fun statInterrupt(e:GameEngine):Boolean {
			if(e.statc[0]==0) {
				// fieldをバックアップにコピー
				tempField = Field(e.field).flipHorizontal()
				// fieldのBlockを全部消す
				e.field.reset()
				count++
			}
			if(e.statc[0] in 21..<21+e.field.width) {
				// 反転
				val x = e.statc[0]-21
				for(y in e.field.hiddenHeight*-1..<e.field.height) e.field.setBlock(
					x, y, tempField.getBlock(x, y)
				)
			}
			return if(e.statc[0]>=46+e.field.width) {
				// 終了
				e.statc[0] = 0

				false
			} else {
				e.statc[0]++
				true
			}
		}
	}

	class ROLL_ROLL(var interval:Int = 30):Item(1, "ROLLING dizzy", 3)
	/** Opponent's a next Piece becomes BIG */
	data object DEATH:Item(2, "DEATH BIG BLOCK", 0) {
		override fun statInterrupt(e:GameEngine):Boolean {
			if(e.statc[0]==15) {
				e.getNextObject(e.nextPieceCount)?.big = true
			}
			return if(e.statc[0]>=30) {
				// 終了
				e.statc[0] = 0
				false
			} else {
				e.statc[0]++
				true
			}
		}
	}
	/** Opponent's field becomes barely visible for 3 seconds */
	class XRAY:Item(3, "X-RAY", 4) {
		var time = 0
	}

	class COLOR:Item(4, "Color Illumination", 3) {
		var time = 0
	}
	/** disable opponent's spinning piece */
	data object LOCK_SPIN:Item(5, "LOCK SPIN SHOCK")
	data object HIDE_NEXT:Item(6, "HIDDEN QUEUE")
	/** Changes opponent's lockDelay to 0 */
	data object MAGNET:Item(7, "MAGNA LOCK")
	/** Freezes opponent's play for 3 seconds */
	data object FREEZE:Item(8, "CHRONOS FREEZE") {
		override fun statInterrupt(e:GameEngine):Boolean {
			return if(e.statc[0]>=200) {
				// 終了
				e.statc[0] = 0
				false
			} else {
				e.statc[0]++
				true
			}
		}
	}
	/** Unable opponents HoldSwap Slots */
	data object LOCK_HOLD:Item(9, "LOCK SWAP ZONE", 6, false, Type.DIRECT)
	/** Flip opponents control horizontally  */
	data object REV_CTRL_H:Item(10, "Horiz. REV.CTRL", duration = 4, true, Type.DIRECT) {
	}
	/** Increase Oopponents Speed to 20G for 7 seconds */
	data object SPEED:Item(11, "BOOST FIRE", 7, true, Type.DIRECT)
	/**  piece becomes I shape and hard drops for 10 seconds */
	data object ALL_I:Item(12, "I FEVER!!", 10, true, Type.SELF, BCOLOR.RAINBOW)
	data object REV_CTRL_V:Item(13, "FLIP 180 Vertical", 4, true, Type.DIRECT)
	data object REMOTE:Item(14, "REMOTE CONTROL", 1) {
	}
	/** Opponent's field becomes totally invisible for 3 seconds */
	data object DARK:Item(15, "INVISIBLE FIELD")
	/** Erase upper half of owner's field*/
	data object DEL_TOP:Item(16, "ERASE Top HALF") {
		override fun statInterrupt(e:GameEngine):Boolean {
			val lines = e.field.delUpperRange
			if(e.statc[0]==40) {
				// 待ち time
				e.playSE("linefall1")
				e.field.delUpper()
			}
			return if(e.statc[0]>=100) {
				// 終了
				e.statc[0] = 0
				false
			} else {
				e.statc[0]++
				true
			}
		}
	}
	/** Erase lower half of owner's field*/
	data object DEL_BOTTOM:Item(17, "ERASE Bot.HALF") {
		override fun statInterrupt(e:GameEngine):Boolean {
			val lines = e.field.delLowerRange
			if(e.statc[0]==40) {
				e.playSE("linefall1")
				e.field.delLower()
				// 待ち time
			}
			return if(e.statc[0]>=100) {
				// 終了
				e.statc[0] = 0
				false
			} else {
				e.statc[0]++
				true
			}
		}
	}
	/** Erase even lines of owner's field*/
	data object DEL_EVEN:Item(18, "ERASE EvEn") {
		override fun statInterrupt(e:GameEngine):Boolean {
			val lines = e.field.delEvenRange
			if(e.statc[0] in 20..<20+lines.size*8&&e.statc[0]%8==0) {
				val y = lines[(e.statc[0]-20)/8]

			} else if(e.statc[0]>=45+lines.size*8) {
				e.field.delEven()
				e.playSE("linefall0")
			}
			return if(e.statc[0]>=60+lines.size*8) {
				// 終了
				e.statc[0] = 0
				false
			} else {
				e.statc[0]++
				true
			}
		}
	}

	data object TRANSFORM:Item(19, "Piece TRANSFORM", 1)
	/** Fires a laser vertically that erases blocks in the opponent's field. */
	data object LASER:Item(20, "Satellite LASER", 0) {
		override fun statInterrupt(e:GameEngine):Boolean {
			if(e.statc[0]==0) {
				e.playSE("laser")
//				e.field.laser()
			}
			return if(e.statc[0]>=30) {
				// 終了
				e.statc[0] = 0
				false
			} else {
				e.statc[0]++
				true
			}
		}
	}

	data object NEGA:Item(21, "NEGA Field", 0)
	data object SHOTGUN:Item(22, "SHOTGUN!", 0) {
		override fun statInterrupt(e:GameEngine):Boolean {
			if(e.statc[0]==0) {
				e.playSE("bomb")
				e.field.shotgunField(e.random, e.owner.receiver)
			}
			return if(e.statc[0]>=30) {
				// 終了
				e.statc[0] = 0
				false
			} else {
				e.statc[0]++
				true
			}
		}
	}
	/** Swap the field with opponent's one. */
	class EXCHANGE:Item(23, "SWAP Field", color = BCOLOR.PURPLE) {
		private var opp = 0
		private var tempField:Field = Field()
		override fun statInterrupt(e:GameEngine):Boolean {
			if(e.statc[0]==0) {
				if(e.owner.players>1) {
					opp = if(e.owner.players>2) e.random.nextInt(e.owner.players).let {
						if(it==e.playerID) (it+1)%e.owner.players else it
					} else if(e.playerID==0) 1 else 0
					tempField = Field(e.owner.engine[opp].field)
					// fieldのBlockを全部消す
					// e.field.reset()
				}
			} else {
				return false
			}

			if(e.statc[0] in 21..<21+e.field.width) {
				// 反転
				val x = e.statc[0]-21
				for(y in e.field.hiddenHeight*-1..<e.field.height) e.field.setBlock(
					x, y, tempField.getBlock(x, y)
				)
			}
			return if(e.statc[0]>=46+e.field.width) {
				// 終了
				e.statc[0] = 0
				false
			} else {
				e.statc[0]++
				true
			}
		}
	}
	/** Opponent's next piece will remain when once lines-cleared with it, and it leaves whole spaces that lines. */
	data object HARD_MINO:Item(24) {
		override fun statInterrupt(e:GameEngine):Boolean {
			if(e.statc[0]==15) {
				e.getNextObject(e.nextPieceCount)?.setHard(1)
			}
			return if(e.statc[0]>=30) {
				// 終了
				e.statc[0] = 0
				false
			} else {
				e.statc[0]++
				true
			}
		}
	}

	data object SHUFFLE:Item(25)
	/**
	 * */
	data object RANDOM:Item(26, "RANDOMIZER", color = BCOLOR.RAINBOW)
	data object FREE_FALL:Item(27, "Free Fall", type = Type.SELF, color = BCOLOR.GREEN) {
		override fun statInterrupt(e:GameEngine):Boolean {
			if(e.statc[0]==40) {
				e.playSE("linefall1")
				e.field.freeFall()
				// 待ち time
			}
			return if(e.statc[0]>=100) {
				// 終了
				e.statc[0] = 0
				false
			} else {
				e.statc[0]++
				true
			}
		}
	}
	/** Move the blocks of owner's field to left side, and open the right with empty spaces. */
	data object MOVE_LEFT:Item(28, "Align Left") {
		override fun statInterrupt(e:GameEngine):Boolean {
			if(e.statc[0]==40) {
				e.playSE("linefall1")
				e.field.moveLeft()
				// 待ち time
			}
			return if(e.statc[0]>=75) {
				// 終了
				e.statc[0] = 0
				false
			} else {
				e.statc[0]++
				true
			}
		}
	}
	/** Move the blocks of owner's field to right side, and open the left with empty spaces. */
	data object MOVE_RIGHT:Item(29, "Align Right") {
		override fun statInterrupt(e:GameEngine):Boolean {
			if(e.statc[0] in 10..<40) {
				e.frameX -= 2
			}
			if(e.statc[0]==50) {
				e.playSE("linefall1")
				e.field.moveRight()
				e.frameX += 80
				// 待ち time
			}
			return if(e.statc[0]>=75) {
				// 終了
				e.statc[0] = 0
				false
			} else {
				e.statc[0]++
				true
			}
		}
	}
	/** Flip the opponent's field vertically( and random horizontally). */
	class FLIP_180:Item(30, "FLIP 180") {
		private var tempField:Field = Field()
		override fun statInterrupt(e:GameEngine):Boolean {
			if(e.statc[0]==0) {
				// fieldをバックアップにコピー
				tempField = Field(e.field).flipVertical().flipHorizontal()
				// fieldのBlockを全部消す
				e.field.reset()
			}
			if(e.statc[0] in 21..<21+e.field.width) {
				// 反転
				val x = e.statc[0]-21
				for(y in e.field.hiddenHeight*-1..<e.field.height) e.field.setBlock(
					x, y, tempField.getBlock(x, y)
				)
			}
			return if(e.statc[0]>=46+e.field.width) {
				// 終了
				e.statc[0] = 0
				false
			} else {
				e.statc[0]++
				true
			}
		}
	}
	/** Fires a wide laser vertically that erases blocks in the opponent's field. */
	data object LASER_16T:Item(31, "WIDE LASER 16t")
	/** Reflects opponent's next attack for 10 seconds, including any Item. */
	data object REFLECT:Item(32, "Reflect Shield", 10, true, Type.SELF)
	/** Double Sending Garbage Power */
	data object DOUBLE_RISE:Item(33, "Doubled Power", 10, true, Type.SELF, BCOLOR.YELLOW)
	data object ALL_CLEAR:Item(34, "All Clean") {

	}

	data object MISS:Item(35, "Miss", 20, false, Type.SELF, color = BCOLOR.WHITE)
	data object COPY_FIELD:Item(36, "Field DUPLICATE", color = BCOLOR.PURPLE)
	data object FAKE_NEXT:Item(37, "???")
	/** All blocks in opponent's field change temporally to bone block that can't be seen their colors.
	 * When next piece */
	data object BONE_BLOCK:Item(38, "[]CUI BONE")
	data object SPOT_LIGHT:Item(39, "SpotLight in dark")
	data object SPIN_FIELD:Item(40, "Spinning Field")

	companion object {
		enum class Type {
			INTERRUPT, DIRECT, SELF
		}
		//operator fun get(index: Int): BGM = if(this._idx)
		val entries:List<Item>
			get() = Item::class.sealedSubclasses.mapNotNull {
				it.objectInstance?:runCatching {it.createInstance()}.getOrNull()
			}.sortedBy {it.id}/* = listOf(
				MIRROR(),
				ROLL_ROLL(),
				DEATH,
				XRAY(),
				COLOR(),
				LOCK_SPIN,
				HIDE_NEXT,
				MAGNET,
				FREEZE,
				LOCK_HOLD,
				REV_CTRL_H,
				SPEED,
				ALL_I,
				REV_CTRL_V,
				REMOTE,
				DARK,
				DEL_TOP,
				DEL_BOTTOM,
				DEL_EVEN,
				TRANSFORM,
				LASER,
				NEGA,
				SHOTGUN,
				EXCHANGE(),
				HARD_MINO,
				SHUFFLE,
				RANDOM,
				FREE_FALL,
				MOVE_LEFT,
				MOVE_RIGHT,
				FLIP_180(),
				LASER_16T,
				REFLECT,
				DOUBLE_RISE,
				ALL_CLEAR,
				MISS,
				COPY_FIELD,
				FAKE_NEXT,
				BONE_BLOCK,
				SPOT_LIGHT,
				SPIN_FIELD
			)*/

		fun values() = entries.toTypedArray()
		fun valueOf(name:String):Item? = entries.find {entry -> name==entry.showName||name==entry::class.simpleName}
	}
}
