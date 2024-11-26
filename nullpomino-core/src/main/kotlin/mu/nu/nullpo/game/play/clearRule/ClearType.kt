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
import mu.nu.nullpo.game.component.Block.ATTRIBUTE
import mu.nu.nullpo.game.component.Field

/** Clearing block from Field method */
interface ClearType {

	fun check(field:Field):ClearResult
	fun flag(field:Field):ClearResult
	fun clear(field:Field):ClearResult

	/** Result of  the result
	 * @param size Line: number of lines cleared, others: number of blocks cleared
	 * @param blocksCleared  List of row of cleared blocks with Y-coordinate index
	 * */
	data class ClearResult(val size:Int = 0, val blocksCleared:Iterable<IndexedValue<List<Block?>>> = emptyList()) {

		val linesY = blocksCleared.map {it.index}.toSet()
		val linesSplited = linesY.sorted().zipWithNext().any {(a, b) -> b==a+1}
		val gemCleared = blocksCleared.count {it.value.any {b -> b?.isGemBlock==true}}
		val garbageCleared = blocksCleared.count {it.value.any {b -> b?.getAttribute(ATTRIBUTE.GARBAGE)==true}}
		val colorContains = blocksCleared.count {it.value.any {b -> b?.color?.color==true}}
		val linesYfolded = linesY.sorted().fold(mutableListOf<Set<Int>>()) {a, t ->
			if(a.isEmpty()||a.last().maxOrNull()!=t-1) a += setOf(t)
			else a[a.lastIndex] = a[a.lastIndex]+t
			a
		}.toSet()
	}
	//-------------------------------------------------------------------------------------------------

	companion object {
		/** Clear blocks got Erase Frag.
		 * @param gemType 1 = Bomb,2 = Spark
		 * @return Total number of blocks cleared.
		 */
		fun Field.clearProceed(gemType:Int = 0):ClearResult {
			val total = (-hiddenHeight..<heightWithoutHurryupFloor).map {y ->
				IndexedValue(y, getRow(y).mapIndexed {x, b ->
					if(b?.getAttribute(ATTRIBUTE.ERASE)==true) {
						if(b.hard>0) {
							b.hard--
							return@mapIndexed null
						}
						if(b.getAttribute(ATTRIBUTE.CONNECT_DOWN)) getBlock(x, y+1)?.run {
							setAttribute(false, ATTRIBUTE.CONNECT_UP)
							setAttribute(true, ATTRIBUTE.BROKEN)
						}
						if(b.getAttribute(ATTRIBUTE.CONNECT_UP)) getBlock(x, y-1)?.run {
							setAttribute(false, ATTRIBUTE.CONNECT_DOWN)
							setAttribute(true, ATTRIBUTE.BROKEN)
						}
						if(b.getAttribute(ATTRIBUTE.CONNECT_LEFT)) getBlock(x-1, y)?.run {
							setAttribute(false, ATTRIBUTE.CONNECT_RIGHT)
							setAttribute(true, ATTRIBUTE.BROKEN)
						}
						if(b.getAttribute(ATTRIBUTE.CONNECT_RIGHT)) getBlock(x+1, y)?.run {
							setAttribute(false, ATTRIBUTE.CONNECT_LEFT)
							setAttribute(true, ATTRIBUTE.BROKEN)
						}

						if(gemType!=0&&b.isGemBlock&&b.cint!=Block.COLOR_GEM_RAINBOW)
							setBlockColor(x, y, Block.COLOR_GEM_RAINBOW) else
							delBlock(x, y)
						b
					} else null
				})
			}
			return ClearResult(total.sumOf {it.value.count {b -> b!=null}}, total)
		}

		internal fun Field.setBlkToRes(total:MutableSet<Triple<Int, Int, Block>>) =
			ClearResult(total.size, total.groupBy {it.second}.map {
				IndexedValue(it.key, (0..<width).map {x -> it.value.find {(bx) -> bx==x}?.third})
			})
	}

}
