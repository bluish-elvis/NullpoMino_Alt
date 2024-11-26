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
import mu.nu.nullpo.util.GeneralUtil.filterNotNullIndexed

/** TETROMINO:clears filled lines */
data object Line:ClearType {
	override fun check(field:Field) = field.checkLines(false)
	override fun flag(field:Field) = field.checkLines()
	override fun clear(field:Field) = field.clearLines()

	fun Field.checkLines(flag:Boolean = true):ClearType.ClearResult {
		if(height<=0) return ClearType.ClearResult()
		/*val lines1 = (-hiddenHeight..<heightWithoutHurryupFloor).filter {
				getRow(it).all {b -> b?.isEmpty==false&&!b.getAttribute(ATTRIBUTE.WALL)}
			}
		val res = lines1.sorted().fold<Int, MutableList<Set<Int>>>(mutableListOf()) {a, t ->
			if(a.isEmpty()||a.last().maxOrNull()!=t-1) a += setOf(t)
			else a[a.lastIndex] = a[a.lastIndex]+t
			a
		}.toSet()*/

		val lines = (-hiddenHeight..<heightWithoutHurryupFloor).filter {
			getRow(it).all {b -> b?.isEmpty==false&&!b.getAttribute(Block.ATTRIBUTE.WALL)}
		}.toSet()
		if(flag) {
			for(i in -hiddenHeight..<heightWithoutHurryupFloor) {
				val fulled = lines.any {it==i}
				setLineFlag(i, fulled)
				if(fulled) {
					for(it in getRow(i)) it?.setAttribute(true, Block.ATTRIBUTE.ERASE)
				}
			}
		}
		return ClearType.ClearResult(lines.size, lines.map {IndexedValue(it, getRow(it))})
	}

	/** Linesを消す
	 * @return 消えたLinescount
	 */
	fun Field.clearLines():ClearType.ClearResult {
		val lines = checkLines(false)
		// field内
		lines.linesY.forEach {y ->
			getRow(y).filterNotNullIndexed().forEach {(x, b) ->
				if(b.hard>0) {
					b.hard--
					setLineFlag(y, false)
				} else delBlock(x, y)
			}
			// 消えたLinesの上下のBlockの結合を解除
			getRow(y+1).filterNotNullIndexed().forEach {(j, blk) ->
				if(blk.getAttribute(Block.ATTRIBUTE.CONNECT_UP)) {
					blk.setAttribute(false, Block.ATTRIBUTE.CONNECT_UP)
					setBlockLinkBroken(j, y)
				}
			}
			getRow(y-1).filterNotNullIndexed().forEach {(j, blk) ->
				if(blk.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) {
					blk.setAttribute(false, Block.ATTRIBUTE.CONNECT_DOWN)
					setBlockLinkBroken(j, y)
				}
			}
		}
		return lines
	}
}
