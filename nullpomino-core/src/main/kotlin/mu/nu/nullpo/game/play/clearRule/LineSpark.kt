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
import mu.nu.nullpo.game.component.Block.COLOR
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.clearRule.ClearType.ClearResult
import mu.nu.nullpo.game.play.clearRule.ClearType.Companion.clearProceed
import mu.nu.nullpo.game.play.clearRule.LineBomb.checkBombOnLine
import mu.nu.nullpo.game.play.clearRule.LineBomb.igniteBomb
import kotlin.math.absoluteValue

//-------------------------------------------------------------------------------------------------
// /** TETROMINO,SPARKLER: clears orthogonal zone from ignited gems*/
data object LineSpark:ClearType {
	fun power(pow:Int):Int = if(pow<0) 8+pow.absoluteValue*2 else
		maxOf(3, (pow.let {if(it<=4) it else (5+(it-5)/2).coerceAtMost(9)}-1))
	//[3,0],[3,1],[3,2],[3,3],[4,4],[5,5],[5,5],[6,6],[6,6],[7,7]}

	override fun check(field:Field) = field.checkBombOnLine(false)
	override fun flag(engine:GameEngine, field:Field) =
		(if(engine.statc[1]<=0) field.checkBombOnLine(true) else LineBomb.recheck(field)).let {res ->
			val lines = res.linesY
			if(engine.statc[1]<=0) engine.statc[2] = lines.size
			val pow = engine.chain+engine.statc[2]
			engine.run {
				if(statc[1]<=0) lines.let {r ->
					owner.mode?.lineClear(engine, r)
					receiver.lineClear(engine, r)
				}
				res.blocksCleared.let {blkS ->
					receiver.bombExplod(engine,
						blkS.map {(y, r) -> y to r.map {(x, b) -> x to (b to LineBomb.power(pow))}.toMap()}.toMap())
				}
				val cnt = field.igniteBomb(pow)
				playSE("item_laser")
				playSE("erase${(cnt/20).coerceIn(0, 2)}")
			}
			ClearResult(res.size, field.findBlocks {it.getAttribute(ATTRIBUTE.ERASE)}).also {
				engine.chain += it.linesY.size
		}
	}

	override fun clear(field:Field) = field.clearProceed(2)
	override fun recheck(field:Field) =
		field.findBlocks {it.getAttribute(ATTRIBUTE.ERASE)||it.cint==Block.COLOR_GEM_RAINBOW}.let {
			ClearResult(it.values.sumOf {row -> row.size}, it)
		}
	/** 予約済みの爆弾ブロックそれぞれで周囲ブロックの消去予約をさせる
	 * @param w width
	 * @param h height
	 * @param bigw
	 * @param bigh
	 * @return 破壊するブロックの個数
	 */
	fun Field.igniteSpark(pow:Int):Int {
		return filterAttributeBlocks(ATTRIBUTE.ERASE).filter {(b) -> b.isGemBlock}.sumOf {(b, x, y) ->
			setLineFlag(y, false)
			detonateSpark(x, y, if(b.getAttribute(ATTRIBUTE.BIG)) -pow else pow)
		}
	}
	/** 爆弾Blockごとの消去予約をする
	 * @param x x-coordinates
	 * @param y y-coordinates
	 * @return 消えるBlocks
	 */
	private fun Field.detonateSpark(x:Int, y:Int, pow:Int):Int {
		val l = power(pow)
		var blocks = 0
		val r = maxOf(y-l, -hiddenHeight)..minOf(y+l, heightWoFloor) to maxOf(x-l, 0)..minOf(x+l, width)

		fun flag(b:Block) = b.run {
			if(!getAttribute(ATTRIBUTE.ERASE)) {
				blocks++
				setAttribute(true, ATTRIBUTE.ERASE)
				if(isGemBlock) {
					secondaryColor = color?:COLOR.RED
//					color = COLOR.RAINBOW
				}
			}
		}
		getBlock(x, y)?.let {b ->
			b.cint = Block.COLOR_GEM_RAINBOW
			b.setAttribute(true, ATTRIBUTE.TEMP_MARK)
		}
		for(i in r.first)
			getBlock(x, i)?.let {b -> flag(b)}
		for(j in r.second)
			getBlock(j, y)?.let {b -> flag(b)}
		return blocks
	}

}
