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
import kotlin.math.absoluteValue

//-------------------------------------------------------------------------------------------------
// /** TETROMINO,SPARKLER: clears orthogonal zone from ignited gems*/
data object LineSpark:ClearType {
	fun power(pow:Int):Int = if(pow<0) 8+pow.absoluteValue*2 else
		maxOf(2+pow).coerceAtMost(9)
	//3,4,5,6,7,8,9

	override fun check(field:Field) = field.checkBombOnLine(false).first
	/**複数ステップに分けて消去フラグを立てる

	1. ブロックが揃ったライン上の爆弾(GEM)を発火予約
	2. 発火予約の爆弾を発火し、上下・左右数ブロック直線を消去予約
	消去対象に非発火の爆弾があった場合は発火予約し、次のステップで2xを繰り返す
	消去範囲の長さは1で保持したライン数に応じて増える
	3. 最後に、消去予約ブロックの領域で囲われたブロックを消去予約。消去準備完了
	 */
	override fun flag(engine:GameEngine, field:Field) =
		if(engine.statc[1]<=0) field.checkBombOnLine(true).let {(x, y) ->
			engine.run {
				val lines = x.linesY
				statc[2] = field.lineFlags.count {it.value}
				lines.let {r ->
					owner.mode?.lineClear(this, r)
					receiver.lineClear(this, r)
				}
				playSE("lines${lines.size.coerceIn(1, 4)}")
				if(x.gemClearedNum>0) playSE("gem")
				statc[3] = x.gemClearedNum
			}
			lastSparked.clear()
			x
		} else recheck(engine, field).let {res ->
			engine.run {
				val gemRemain =
					res.gemCleared.entries.associate {(y, r) -> y to r.filterValues {!it.getAttribute(ATTRIBUTE.TEMP_MARK)}}
				if(gemRemain.values.sumOf {it.size}>0) {
					val lines = engine.statc[2]
					val pow = engine.chain+lines
					gemRemain.let {blkS ->
						receiver.sparkExplod(engine,
							blkS.map {(y, r) -> y to r.map {(x, b) -> x to (b to power(pow))}.toMap()}.toMap())
					}
					field.igniteSpark(pow)
					playSE("item_laser", (1.5f-(pow-1)*.15f).coerceIn(.8f, 1.5f))
					if(field.filterBlocks {it, _, _ ->
							it.getAttribute(ATTRIBUTE.ERASE)&&it.isGemBlock&&it.cint!=Block.COLOR_GEM_RAINBOW&&
								!it.getAttribute(ATTRIBUTE.TEMP_MARK)
						}.isNotEmpty()) playSE("gem")
					statc[3] = res.size/*field.findBlocks {
						it.cint==Block.COLOR_GEM_RAINBOW&&
							!it.getAttribute(ATTRIBUTE.TEMP_MARK)
					}.values.sumOf { row -> row.size }*/
					res
				} else {
					field.delSparkRect()
					playSE("erase${(field.filterAttributeBlocks(ATTRIBUTE.ERASE).size/20).coerceIn(0, 2)}")
					statc[3] = -1
					ClearResult(res.size, field.findBlocks {it.getAttribute(ATTRIBUTE.ERASE)}).also {engine.statc[2]}
				}
			}
		}

	/**[flag]のステップ3が完了している場合は消去予約ブロックを消去する。そうでない場合は何もしない*/
	override fun clear(engine:GameEngine, field:Field) = if(engine.statc[3]<=0) field.clearProceed(2) else ClearResult()

	/** @return 残存している発火予定の爆弾 */
	override fun recheck(engine:GameEngine, field:Field) =
		field.findBlocks {
			it.getAttribute(ATTRIBUTE.ERASE)||it.cint==Block.COLOR_GEM_RAINBOW&&
				!it.getAttribute(ATTRIBUTE.TEMP_MARK)
		}.let {
			ClearResult(it.values.sumOf {row -> row.size}, it)
		}

	/** 予約済みの爆弾ブロックそれぞれで周囲ブロックの消去予約をさせる
	 * @return 破壊するブロックの個数
	 */
	fun Field.igniteSpark(pow:Int):Int {
		val detonated = filterBlocks {b, _, _ ->
			b.isGemBlock&&b.getAttribute(ATTRIBUTE.ERASE)&&!b.getAttribute(ATTRIBUTE.TEMP_MARK)
		}.sumOf {(b, x, y) ->
			setLineFlag(y, false)
			b.setAttribute(true, ATTRIBUTE.TEMP_MARK)
			b.secondaryColor = b.color?:COLOR.RED
			b.cint = Block.COLOR_GEM_RAINBOW
			detonateSpark(x, y, if(b.getAttribute(ATTRIBUTE.BIG)) -pow else pow)
		}
		return detonated
	}

	val lastSparked = mutableSetOf<Pair<Int, Int>>()
	/** 爆弾Blockごとの消去予約をする
	 * @param x x-coordinates
	 * @param y y-coordinates
	 * @return 消えるBlocks
	 */
	private fun Field.detonateSpark(x:Int, y:Int, pow:Int):Int {
		val l = power(pow)
		var blocks = 0
		val sparkedPositions = mutableSetOf<Pair<Int, Int>>()
		val v = listOf(y downTo maxOf(y-l, -hiddenHeight), y..minOf(y+l, heightWoFloor-1))
		val h = listOf(x downTo maxOf(x-l, 0), x..minOf(x+l, width-1))

		fun flag(b:Block, posX:Int, posY:Int) = b.run {
			if(!getAttribute(ATTRIBUTE.ERASE)) {
				blocks++
				sparkedPositions += (posX to posY)
				setAttribute(true, ATTRIBUTE.ERASE)
				alpha = .8f
				if(isGemBlock) {
					if(getAttribute(ATTRIBUTE.TEMP_MARK)) {
					secondaryColor = color?:COLOR.RED
					color = COLOR.RAINBOW
						darkness = -.5f
					}
				} else darkness = .5f

			}
		}
		for(c in v) for(i in c)
			getBlock(x, i)?.let {b ->
				setLineFlag(i, false)
				flag(b, x, i)
				if(b.hard>0) break
			}
		for(r in h) for(j in r)
			getBlock(j, y)?.let {b ->
				flag(b, j, y)
				if(b.hard>0) break
			}
		lastSparked += sparkedPositions
		return blocks
	}

	/**消去フラグつきBlockに囲まれたブロックに消去予約をする
	 * @return 消えるBlocks*/
	private fun Field.delSparkRect():Int {
		/*Sparklissでは、爆弾ブロックの上下左右直線上の数ブロックを消去するだけでなく、消去領域で囲まれた長方形範囲内のブロックも消去する仕様です。
		 この関数はその長方形領域内のブロックを消去する処理を行います。
		 なお、長方形の領域内の爆弾は発火せずに消滅します。また耐久(hard)が1以上あるブロックも、即座に消滅するよう耐久を0にして消去フラグを立てます。
		 */
		if(width<=0||heightWoFloor+hiddenHeight<=0) return 0

		var blocks = 0
		val yRange = -hiddenHeight..<heightWoFloor
		val xRange = 0..<width

		fun isErase(x:Int, y:Int):Boolean = lastSparked.contains(x to y)||getBlock(x, y)?.getAttribute(ATTRIBUTE.ERASE)==true
		fun flag(x:Int, y:Int, b:Block) = b.run {
			if(!getAttribute(ATTRIBUTE.ERASE)) {
				blocks++
				if(hard>0) hard = 0
				setAttribute(true, ATTRIBUTE.ERASE)
				setLineFlag(y, false)
				if(isGemBlock) setAttribute(true, ATTRIBUTE.TEMP_MARK)
				darkness = .5f
			}
		}

		for(top in yRange)
			for(bottom in (top+2)..<yRange.last+1)
				for(left in xRange)
					for(right in (left+2)..<xRange.last+1) {
						val horizontal = (left..right).all {xx -> isErase(xx, top)&&isErase(xx, bottom)}
						if(!horizontal) continue

						val vertical = (top..bottom).all {yy -> isErase(left, yy)&&isErase(right, yy)}
						if(!vertical) continue

						for(yy in (top+1)..<bottom)
							for(xx in (left+1)..<right)
								getBlock(xx, yy)?.let {b ->
									flag(xx, yy, b)
								}
					}

		return blocks
	}

}
