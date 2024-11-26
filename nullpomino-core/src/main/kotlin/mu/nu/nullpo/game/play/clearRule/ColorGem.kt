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
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.clearRule.ClearType.ClearResult
import mu.nu.nullpo.game.play.clearRule.ClearType.Companion.setBlkToRes
import mu.nu.nullpo.game.play.clearRule.Color.Companion.clearColor

/** SPF:clears connected colors, but ignited by gems */
data class ColorGem(var colorClearSize:Int = 2, var garbageColorClear:Boolean = true, var ignoreHidden:Boolean = false)
	:ClearType {
	override fun check(field:Field) = field.gemColorCheck(colorClearSize, false, garbageColorClear, ignoreHidden)
	override fun flag(engine:GameEngine, field:Field) =
		field.gemColorCheck(colorClearSize, true, garbageColorClear, ignoreHidden).also {check ->
			engine.statistics.blocks += check.size
			engine.playSE("erase0")
		}

	override fun clear(field:Field) = field.gemClearColor(colorClearSize, garbageColorClear, ignoreHidden)

	companion object {
		fun Field.gemColorCheck(size:Int, flag:Boolean, garbageClear:Boolean, ignoreHidden:Boolean):ClearResult {
			if(flag) setAllAttribute(false, Block.ATTRIBUTE.ERASE)

			val total = mutableSetOf<Triple<Int, Int, Block>>()

			for(i in -hiddenHeight..<heightWithoutHurryupFloor) for(j in 0..<width) {
				getBlock(j, i)?.let {b ->
					if(!b.isGemBlock) return@let
					val clear = clearColor(j, i, null, garbageClear, true, ignoreHidden)
					if(clear.size>=size) {
						total += clear
						if(flag) clearColor(j, i, true, garbageClear, true, ignoreHidden)
					}
				}
			}
			return setBlkToRes(total)
		}

		/** Performs all cint clears of sufficient size containing at least one gem
		 * block.
		 * @param size Minimum size of cluster for a clear
		 * @param garbageClear `true` to clear garbage blocks adjacent to cleared
		 * clusters
		 * @return Total number of blocks cleared.
		 */
		fun Field.gemClearColor(size:Int, garbageClear:Boolean, ignoreHidden:Boolean = false):ClearResult {
			val total = mutableSetOf<Triple<Int, Int, Block>>()

			for(i in -hiddenHeight..<heightWithoutHurryupFloor)
				for(j in 0..<width) {
					getBlock(j, i)?.also {b ->
						if(!b.isGemBlock) return@also
						val clear = clearColor(j, i, null, garbageClear, true, ignoreHidden)
						if(clear.size>=size) {
							total += clear
							clearColor(j, i, false, garbageClear, true, ignoreHidden)
						}
					}
				}
			return setBlkToRes(total)
		}
	}
}
