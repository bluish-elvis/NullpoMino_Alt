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
import mu.nu.nullpo.game.play.clearRule.ClearType.Companion.clearProceed
import mu.nu.nullpo.game.play.clearRule.ClearType.Companion.setBlkToRes

/** PHYSICIAN:clears connected straight colors */
data class ColorStraight(var colorClearSize:Int = 4, var lineColorDiagonals:Boolean = false,
	var gemSameColor:Boolean = true)
	:ClearType {
	override fun check(field:Field) = field.checkConnectedColor(colorClearSize, false, lineColorDiagonals, gemSameColor)
	override fun flag(engine:GameEngine, field:Field) =
		field.checkConnectedColor(colorClearSize, true, lineColorDiagonals, gemSameColor).also {check ->
			engine.statistics.blocks += check.size
			engine.playSE("erase0")
		}

	override fun clear(field:Field) = field.clearProceed(0)

	companion object {
		/** Check for clears of Color Connected straight.
		 * @param size Minimum length of connect for a clear
		 * @param doErase `true` to set ATTRIBUTE.ERASE on blocks to be cleared.
		 * @param diagonals `true` to check diagonals,
		 * `false` to check only vertical and horizontal
		 * @param gemSame `true` to check gem blocks
		 * @return Total number of blocks that would be cleared.
		 */
		fun Field.checkConnectedColor(size:Int, doErase:Boolean, diagonals:Boolean, gemSame:Boolean):ClearType.ClearResult {
			if(size<1) return ClearType.ClearResult()
			if(doErase) setAllAttribute(false, Block.ATTRIBUTE.ERASE)
			val total = mutableSetOf<Triple<Int, Int, Block>>()
			var blockColor:Int
			for(i in -hiddenHeight..<heightWithoutHurryupFloor)
				for(j in 0..<width) {
					val startColor = getBlockColor(j, i, gemSame)
					if(startColor==Block.COLOR_NONE||startColor==Block.COLOR_INVALID) continue
					for(dir in 0..<if(diagonals) 3 else 2) {
						var x = j
						var y = i
						val count = mutableSetOf<Triple<Int, Int, Block>>()
						do {
							count += Triple(x, y, getBlock(x, y)?:break)
							if(dir!=1) y++
							if(dir!=0) x++
							blockColor = getBlockColor(x, y, gemSame)
						} while(startColor==blockColor)
						if(count.size<size) continue
						total += count
						if(doErase) count.forEach {(x, y, b) ->
							b.apply {
								if(hard>0) hard--
								else setAttribute(true, Block.ATTRIBUTE.ERASE)
							}
						}
					}
				}
			return setBlkToRes(total)
		}
	}

}
