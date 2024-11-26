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
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.clearRule.ClearType.ClearResult
import mu.nu.nullpo.game.play.clearRule.ClearType.Companion.clearProceed
import mu.nu.nullpo.game.play.clearRule.Line.checkLines
import mu.nu.nullpo.game.play.clearRule.LineBomb.checkBombOnLine

//-------------------------------------------------------------------------------------------------/** TETROMINO,SPARKLER: clears orthogonal zone from ignited gems*/
data object LineSpark:ClearType {
	override fun check(field:Field) = ClearResult(field.checkBombOnLine(false))
	override fun flag(engine:GameEngine, field:Field) = field.checkLines(false).size.let {
		if(it<=0) return@let ClearResult()
		field.checkBombOnLine(true)
		ClearResult(it, field.igniteSpark(engine.chain+field.checkLines(false).size).let {
			emptyMap()
		})
	}.also {
		engine.run {
			playSE("bomb")
			playSE("erase0")
			chain += it.linesY.size
		}
	}

	override fun clear(field:Field) = field.clearProceed(2)

	/** 予約済みの爆弾ブロックそれぞれで周囲ブロックの消去予約をさせる
	 * @param w width
	 * @param h height
	 * @param bigw
	 * @param bigh
	 * @return 破壊するブロックの個数
	 */
	fun Field.igniteSpark(pow:Int):Int {
		return filterAttributeBlocks(ATTRIBUTE.ERASE).filter {it.third.isGemBlock}.sumOf {(x, y, b) ->
			setLineFlag(y, false)
			detonateSpark(x, y, if(b.getAttribute(ATTRIBUTE.BIG)) -1 else pow)
		}
	}
	/** 爆弾Blockごとの消去予約をする
	 * @param x x-coordinates
	 * @param y y-coordinates
	 * @return 消えるBlocks
	 */
	private fun Field.detonateSpark(x:Int, y:Int, pow:Int):Int {
		val (w, h) = if(pow<0) 4 to 4 else
			(pow.let {if(it<=4) it else (5+(it-5)/2).coerceAtMost(9)}-1).let {maxOf(3, it) to it}
		//[3,0],[3,1],[3,2],[3,3],[4,4],[5,5],[5,5],[6,6],[6,6],[7,7]}
		val my = heightWithoutHurryupFloor
		var blocks = 0
		val r =
			listOf(
				if(y-h>-hiddenHeight) y-h else -hiddenHeight, if(y+h<my) y+h else my, if(x>w) x-w else 0,
				if(x+w<width) x+w else width
			)

		fun flag(b:Block) {
			if(!b.getAttribute(ATTRIBUTE.ERASE)) {
				blocks++
				b.setAttribute(true, ATTRIBUTE.ERASE)
			}
		}
		for(i in r[0]..r[1])
			getBlock(x, i)?.let {b -> flag(b)}
		for(j in r[2]..r[3])
			getBlock(j, y)?.let {b -> flag(b)}
		return blocks
	}

}
