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
import kotlin.reflect.full.createInstance
import mu.nu.nullpo.game.component.Block.COLOR as BCOLOR

@Serializable
sealed class Item(val id:Int, val showName:String? = null, val color:BCOLOR = BCOLOR.RED, val toSelf:Boolean = false) {
	open fun statEffect(e:GameEngine):Boolean = false

	/**
	 * Interrupts the game state
	 */
	open fun statInterrupt(e:GameEngine):Boolean = false
	override fun toString():String = this.showName?:this::class.simpleName?:""
	val ordinal get() = entries.indexOf(this)
	/** Flip Opponents Field horizontally 3 times */
	class MIRROR(var maxCount:Int = 3):Item(0, "MIRROR") {
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

	class ROLL_ROLL(var interval:Int = 30):Item(1, "Forced S")
	/** Opponent */
	data object DEATH:Item(2, "BIG BLOCK") {
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

	class XRAY:Item(3) {
		var time = 0
	}

	class COLOR:Item(4) {
		var time = 0
	}

	data object LOCK_SPIN:Item(5)
	data object HIDE_NEXT:Item(6)
	data object MAGNET:Item(7)
	data object FREEZE:Item(8) {
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
	/** Unable Opponents Swap Slots */
	data object LOCK_HOLD:Item(9)
	/** Flip Opponents Field horizontally 3 times */
	data object FLIP_HORIZ:Item(10, "FLIP Horizontal") {
		private var tempField:Field = Field()
		override fun statInterrupt(e:GameEngine):Boolean {
			if(e.statc[0]==0) {
				// fieldをバックアップにコピー
				tempField = Field(e.field).flipHorizontal()
				// fieldのBlockを全部消す
				// e.field.reset()
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

	data object SPEED:Item(11)
	data object ALL_I:Item(12)
	data object FLIP_VERT:Item(13, "FLIP Vertical") {
		private var tempField:Field = Field()
		private var tempFlipped:Field = Field()
		override fun statInterrupt(e:GameEngine):Boolean {
			if(e.statc[0]==0) {
				// fieldをバックアップにコピー
				tempField = Field(e.field).flipVertical()
				// fieldのBlockを全部消す
				// e.field.reset()
			}
			if(e.statc[0] in 91..<91+e.field.height) {
				// 反転
				val y = e.statc[0]-91
				for(x in 0..<e.field.width) e.field.setBlock(
					x, y, tempField.getBlock(x, y)
				)
			}
			if(e.statc[0] in 106..<106+e.field.height) {
				// 反転
				val y = e.statc[0]-21
				for(x in 0..<e.field.width) e.field.setBlock(
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

	data object REMOTE:Item(14)
	data object DARK:Item(15)
	data object DEL_TOP:Item(16, "ERASE UpHALF", BCOLOR.BLUE, true) {
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

	data object DEL_BOTTOM:Item(17, "ERASE DownHALF", BCOLOR.BLUE, true) {
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

	data object DEL_EVEN:Item(18, "ERASE EvEn", BCOLOR.BLUE, true) {
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

	data object TRANSFORM:Item(19, "Mino TRANSFORM")
	data object LASER:Item(20, "Satellite LASER")
	data object NEGA:Item(21, "NEGA Field")
	data object SHOTGUN:Item(22, "SHOTGUN!")
	class EXCHANGE:Item(23, "SWAP Field", BCOLOR.PURPLE) {
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
	data object RANDOM:Item(26, "RANDOMIZER", BCOLOR.RAINBOW, false)
	data object FREE_FALL:Item(27, "Free Fall", BCOLOR.GREEN, true) {
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

	data object MOVE_LEFT:Item(28, "Align Left", BCOLOR.BLUE, true) {
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

	data object MOVE_RIGHT:Item(29, "Align Right", BCOLOR.BLUE, true) {
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

	data object LASER_16T:Item(31, "WIDE LASER 16t")
	object REFLECT:Item(32, "Reflect Shield", BCOLOR.CYAN, true)
	/** Double Sending Garbage Power */
	data object DOUBLE_RISE:Item(33, "Doubled Power", BCOLOR.YELLOW, true)
	data object ALL_CLEAR:Item(34, "All Clean", BCOLOR.CYAN, true)
	data object MISS:Item(35, "Miss", BCOLOR.WHITE)
	data object COPY_FIELD:Item(36, "Field DUPLICATE", BCOLOR.PURPLE, true)
	data object FAKE_NEXT:Item(37, "???")
	data object BONE_BLOCK:Item(38, "[]CUI BONE")
	data object SPOT_LIGHT:Item(39, "SpotLight in dark")
	data object SPIN_FIELD:Item(40, "Spinning Field")

	companion object {
		//operator fun get(index: Int): BGM = if(this._idx)
		val entries:List<Item>
			get() = Item::class.sealedSubclasses.map {
				it.objectInstance?:it.createInstance()
			}.sortedBy {it.id}

		fun values():Array<Item> = entries.toTypedArray()
		fun valueOf(name:String):Item? = entries.find {name==it.showName||name==it::class.simpleName}
	}
}
