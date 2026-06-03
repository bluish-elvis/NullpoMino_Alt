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
import mu.nu.nullpo.game.play.clearRule.Line.checkLines

/** TETROMINO,BOMBER:clears square zone from ignited gems*/
data object LineBomb:ClearType {

	//[3,0],[3,1],[3,2],[3,3],[4,4],[5,5],[5,5],[6,6],[6,6],[7,7]}
	fun power(pow:Int):Pair<Int, Int> = if(pow<0) 4 to 4 else
		(pow.let {if(it<=4) it else (5+(it-5)/2).coerceAtMost(9)}-1).let {maxOf(3, it) to it}

	override fun check(field:Field) = field.checkBombOnLine(false)
	override fun flag(engine:GameEngine, field:Field) =
		(if(engine.statc[1]<=0) field.checkBombOnLine(true) else recheck(field)).let {res ->
			val lines = res.linesY
			if(engine.statc[1]<=0) engine.statc[2] = lines.size
			val pow = engine.chain+engine.statc[2]
		engine.run {
			if(statc[1]<=0) lines.let {r ->
				owner.mode?.lineClear(engine, r)
				receiver.lineClear(engine, r)
			}
			res.blocksCleared.let {blkS ->
				receiver.bombExplod(engine, blkS.map {(y, r) -> y to r.map {(x, b) -> x to (b to power(pow))}.toMap()}.toMap())
			}
			val cnt = field.igniteBomb(pow)
			playSE("bomb")
			playSE("erase${(cnt/20).coerceIn(0, 2)}")
		}
			ClearResult(res.size, field.findBlocks {it.getAttribute(ATTRIBUTE.ERASE)}).also {engine.chain++}
	}
	override fun clear(field:Field) = field.clearProceed(1)
	override fun recheck(field:Field) =
		field.findBlocks {it.getAttribute(ATTRIBUTE.ERASE)||it.cint==Block.COLOR_GEM_RAINBOW}.let {
			ClearResult(it.values.sumOf {row -> row.size}, it)
		}
	/** ライン上の爆弾の取得
	 * @param ignite 爆弾を発火予約する
	 * @return ライン上の爆弾ブロックの個数
	 */
	fun Field.checkBombOnLine(ignite:Boolean):ClearResult = checkLines(false).let {res ->
		if(res.size<=0) return@let ClearResult()
		val tmp = res.blocksCleared.map {(y, row) ->
			y to (row.filter {(_, b) -> b.isGemBlock})
		}.toMap()

		val ret = tmp.values.sumOf {it.size}
		if(ignite) tmp.forEach {(y, row) ->
//			setLineFlag(y, false)
			row.forEach {(x, _) ->
				getBlock(x, y)?.run {
					setAttribute(true, ATTRIBUTE.ERASE)
					secondaryColor = color?:COLOR.RED
//					color = COLOR.RAINBOW
				}
			}
		}

		return ClearResult(ret, tmp)
	}

	/** 予約済みの爆弾ブロックそれぞれで周囲ブロックの消去予約をさせる
	 * @param w width
	 * @param h height
	 * @param bigw
	 * @param bigh
	 * @return 破壊するブロックの個数
	 */
	fun Field.igniteBomb(pow:Int):Int {
		return filterAttributeBlocks(ATTRIBUTE.ERASE).filter {(b) -> b.isGemBlock}.sumOf {(b, x, y) ->
			setLineFlag(y, false)
			detonateBomb(x, y, if(b.getAttribute(ATTRIBUTE.BIG)) -1 else pow)
		}
	}
	/** 爆弾Blockごとの消去予約をする
	 * @param x x-coordinates
	 * @param y y-coordinates
	 * @return 消えるBlocks
	 */
	private fun Field.detonateBomb(x:Int, y:Int, pow:Int):Int {
		val (w, h) = power(pow)
		val my = heightWoFloor
		var blocks = 0
		val r =
			listOf(
				if(y-h>-hiddenHeight) y-h else -hiddenHeight, if(y+h<my) y+h else my, if(x>w) x-w else 0,
				if(x+w<width) x+w else width
			)
		getBlock(x, y)?.let {b ->
			b.cint = Block.COLOR_GEM_RAINBOW
			b.setAttribute(true, ATTRIBUTE.TEMP_MARK)
		}
		for(i in r[0]..r[1])
			for(j in r[2]..r[3])
				getBlock(j, i)?.run {
					if(!getAttribute(ATTRIBUTE.ERASE)) {
						blocks++
						setAttribute(true, ATTRIBUTE.ERASE)
						if(isGemBlock) {
							secondaryColor = color?:COLOR.RED
//							color = COLOR.RAINBOW
						}
					}
				}
		return blocks
	}
}
