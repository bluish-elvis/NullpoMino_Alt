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

package mu.nu.nullpo.game.play.clearRule

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.play.clearRule.ClearType.Companion.setBlkToRes

/** AVALANCHE:clears connected colors and fills empty spaces */
data class Color(var colorClearSize:Int = 4, var garbageColorClear:Boolean = true, var gemSameColor:Boolean = true,
	var ignoreHidden:Boolean = true):
	ClearType {
	override fun check(field:Field) =
		field.checkColor(colorClearSize, false, garbageColorClear, gemSameColor, ignoreHidden)

	override fun flag(field:Field) = field.checkColor(colorClearSize, true, garbageColorClear, gemSameColor, ignoreHidden)
	override fun clear(field:Field) = field.clearAll(colorClearSize, garbageColorClear, gemSameColor, ignoreHidden)

	companion object {
		/** check connected blocks with [targetColor]<br/>
		 *  Note: This method is private because calling it with a targetColor is BLOCK_COLOR_NONE or BLOCK_COLOR_INVALID
		 *  may cause an infinite loop and crash the game.<br/>
		 *  This check is handled by the above public method to avoid redundant checks.
		 * @param flag `true` to flag ATTRIBUTE.ERASE them, `false` to remove them, 'null' to flag ATTRIBUTE.TEMP_MARK.
		 * @param garbageClear `true` to clear garbage blocks adjacent to cleared
		 * */
		private fun Field.checkColor(
			x:Int, y:Int, targetColor:Int, flag:Boolean?, garbageClear:Boolean, gemSame:Boolean,
			ignoreHidden:Boolean
		):Set<Triple<Int, Int, Block>> {
			if(ignoreHidden&&y<0) return emptySet()
			val blockColor = getBlockColor(x, y, gemSame)
			if(blockColor==Block.COLOR_INVALID||blockColor==Block.COLOR_NONE) return emptySet()

			return getBlock(x, y)?.let {b ->
				val f = if(flag==true) Block.ATTRIBUTE.ERASE else Block.ATTRIBUTE.TEMP_MARK
				if(flag!=false&&b.getAttribute(Block.ATTRIBUTE.ERASE)&&b.getAttribute(
						Block.ATTRIBUTE.TEMP_MARK)) return@let emptySet()
				val blk = if(flag==false) Block(b) else b
				if(garbageClear&&b.getAttribute(Block.ATTRIBUTE.GARBAGE)
					&&!b.getAttribute(Block.ATTRIBUTE.WALL)
				)
					if(flag==false) if(b.hard>0) b.hard-- else delBlock(x, y)
					else b.setAttribute(true, f)
				if(blockColor!=targetColor) return@let emptySet()
				if(flag==false) if(b.hard>0) b.hard-- else delBlock(x, y)
				else b.setAttribute(true, f)
				val ret = setOf(Triple(x, y, blk))+checkColor(x+1, y, targetColor, flag, garbageClear, gemSame, ignoreHidden)+
					checkColor(x-1, y, targetColor, flag, garbageClear, gemSame, ignoreHidden)+
					checkColor(x, y+1, targetColor, flag, garbageClear, gemSame, ignoreHidden)+
					checkColor(x, y-1, targetColor, flag, garbageClear, gemSame, ignoreHidden)
				if(flag==null) setAllAttribute(false, Block.ATTRIBUTE.TEMP_MARK)
				ret
			}?:emptySet()
		}

		/** Performs clearing all connected with color  of sufficient size.
		 * @param size Minimum size of cluster for a clear
		 * @param garbageClear `true` to clear garbage blocks adjacent to cleared
		 * clusters
		 * @param gemSame `true` to check gem blocks
		 * @return Total number of blocks cleared.
		 */
		fun Field.clearAll(size:Int, garbageClear:Boolean, gemSame:Boolean,
			ignoreHidden:Boolean = false):ClearType.ClearResult {
			val total = mutableSetOf<Triple<Int, Int, Block>>()
			for(i in (if(ignoreHidden) 0 else -hiddenHeight)..<heightWithoutHurryupFloor)
				for(j in 0..<width) {
					val clear = clearColor(j, i, true, garbageClear, gemSame, ignoreHidden)
					if(clear.size>=size) {
						total += clear
						clearColor(j, i, false, garbageClear, gemSame, ignoreHidden)
					}
				}
			return ClearType.ClearResult(total.size, total.groupBy {it.second}.map {
				IndexedValue(it.key, (0..<width).map {x -> it.value.find {(bx) -> bx==x}?.third})
			})
		}
		/** Clears the block at the [x][y] position as well as all adjacent blocks of the same color,
		 * @param flag true to flag ATTRIBUTE.ERASE them, false to execute clear them. null to do nothing
		 * @param garbageClear `true` to clear garbage blocks adjacent to cleared clusters
		 * @param gemSame `true` to check gem blocks
		 * @return The number of blocks cleared.
		 */
		fun Field.clearColor(x:Int, y:Int, flag:Boolean?, garbageClear:Boolean, gemSame:Boolean,
			ignoreHidden:Boolean):Set<Triple<Int, Int, Block>> {
			val blockColor = getBlockColor(x, y, gemSame)
			if(blockColor==Block.COLOR_NONE||blockColor==Block.COLOR_INVALID) return emptySet()
			return if(getBlock(x, y)?.getAttribute(Block.ATTRIBUTE.GARBAGE)!=false) emptySet()
			else checkColor(x, y, blockColor, flag, garbageClear, gemSame, ignoreHidden)
		}
		/**
		 * @param size required number of connected blocks
		 * @param doErase execute block clearing
		 * @param garbageClear including garbage blocks for checking
		 * */
		fun Field.checkColor(size:Int, doErase:Boolean, garbageClear:Boolean, gemSame:Boolean,
			ignoreHidden:Boolean):ClearType.ClearResult {
			val total = mutableSetOf<Triple<Int, Int, Block>>()
			if(doErase) {
				setAllAttribute(false, Block.ATTRIBUTE.ERASE)
				colorClearExtraCount = 0
				//				colorsCleared = 0
			}

			for(i in -hiddenHeight..<heightWithoutHurryupFloor)
				for(j in 0..<width) {
					val clear = clearColor(j, i, null, garbageClear, gemSame, ignoreHidden)
					if(clear.size>=size) {
						total += clear
						if(doErase) {
							clearColor(j, i, true, garbageClear, gemSame, ignoreHidden)
							colorClearExtraCount += (clear.size-size).coerceAtLeast(0)
						}
					}
				}
			return setBlkToRes(total)
		}
	}

}
