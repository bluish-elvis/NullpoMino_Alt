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
import mu.nu.nullpo.game.play.GameEngine.Companion.COMBO_TYPE_DISABLE
import mu.nu.nullpo.game.play.GameEngine.Companion.COMBO_TYPE_DOUBLE
import mu.nu.nullpo.game.play.GameEngine.Companion.COMBO_TYPE_NORMAL
import mu.nu.nullpo.game.play.GameEngine.Companion.FRAME_SKIN_SG
import mu.nu.nullpo.game.play.clearRule.ClearType.ClearResult
import mu.nu.nullpo.gui.common.fx.PopupCombo.CHAIN
import mu.nu.nullpo.util.GeneralUtil.filterNotNullIndexed
import mu.nu.nullpo.util.GeneralUtil.toInt
import org.apache.logging.log4j.LogManager
import kotlin.math.absoluteValue

/** TETROMINO:clears filled lines */
data object Line:ClearType {
	override fun check(field:Field) = field.checkLines(false)
	override fun flag(engine:GameEngine, field:Field) = field.checkLines().also {check ->
		engine.run {
			val inGame = ending==0||staffrollEnableStatistics
			val li = check.size.let {if(big&&bigHalf) it shr 1 else it}
			lineClearing = li
			split = check.linesSplited
			if(li>0) {
				playSE(
					when {
						split -> "split"
						li>=(if(twist) 2 else if(combo>0) 3 else 4) -> "erase2"
						li>=(if(twist) 1 else 2) -> "erase1"
						else -> "erase0"
					}
				)
				lastLinesY = check.linesYfolded
				lastLineY = check.linesY.maxOrNull()?:0
				if(frameSkin!=FRAME_SKIN_SG)playSE("line${li.coerceIn(1, 4)}")
				if(li>=4) playSE("applause${(2+b2bCount).coerceIn(0, 4)}")
				if(twist) {
					playSE("twister")
					if(li>=3||li>=2&&b2b) playSE("crowd1") else playSE("crowd0")
					if(inGame)
						when(li) {
							1 -> if(twistMini) statistics.totalTwistSingleMini++
							else statistics.totalTwistSingle++
							2 -> if(split) statistics.totalTwistSplitDouble++ else if(twistMini) statistics.totalTwistDoubleMini++
							else statistics.totalTwistDouble++
							3 -> if(split) statistics.totalTwistSplitTriple++ else statistics.totalTwistTriple++
						}
				} else if(inGame)
					when(li) {
						1 -> statistics.totalSingle++
						2 -> if(split) statistics.totalSplitDouble++
						else statistics.totalDouble++
						3 -> if(split) statistics.totalSplitTriple++
						else statistics.totalTriple++
						4 -> statistics.totalQuadruple++
					}
			}
			// B2B bonus
			if(li>=1&&b2bEnable)
				if(li>=4||(split&&splitB2B)||twist||field.isEmpty) {
					b2bCount++
					if(b2bCount>0) {
						playSE("b2b_combo", minOf(1.5f, 1f+(b2bCount)/13f))
						if(inGame) {
							when {
								li==4 -> statistics.totalB2BQuad++
								split -> statistics.totalB2BSplit++
								twist -> statistics.totalB2BTwist++
							}
							if(b2bCount>=statistics.maxB2B) statistics.maxB2B = b2bCount
						}
						owner.receiver.addCombo(this, nowPieceX, nowPieceBottomY-(combo>0).toInt(), b2bCount, CHAIN.B2B)
					} else {
						playSE("b2b_start")
						b2bCount = 0
					}
				} else if(b2bCount>=0&&combo<0) {
					b2bCount = -b2bCount.absoluteValue
					playSE("b2b_end")
				}
			// Combo
			if(comboType!=COMBO_TYPE_DISABLE&&chain==0) {
				if(comboType==COMBO_TYPE_NORMAL||comboType==COMBO_TYPE_DOUBLE&&li>=2) combo++
				if(combo>0) {
					playSE("combo", minOf(2f, 1f+(combo-1)/14f))
					owner.receiver.addCombo(this, nowPieceX, nowPieceBottomY+b2b.toInt(), combo, CHAIN.COMBO)
					if(inGame) if(combo>=statistics.maxCombo) statistics.maxCombo = combo
				}
			}

			lineGravityTotalLines += lineClearing
			statistics.blocks += li*fieldWidth
			if(inGame) statistics.lines += li
		}
	}

	override fun clear(field:Field) = field.clearLines()

	fun Field.checkLines(flag:Boolean = true):ClearResult {
		if(height<=0) return ClearResult()
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
				val fulled = lines.indexOf(i)>=0
				setLineFlag(i, fulled)
				for(it in getRow(i)) it?.setAttribute(fulled, Block.ATTRIBUTE.ERASE)
			}
		}
//		if(lines.isNotEmpty()) log.debug("clearLines {}: {}", flag, lines)
		return ClearResult(lines.size, lines.associateWith {y ->
			getRow(y).filterNotNullIndexed().associate {(x, b) -> x to b}
		})
	}
	/** Linesを消す
	 * @return 消えたLines count
	 */
	fun Field.clearLines():ClearResult {
		val lines = checkLines(false)

//		if(lines.size>0) log.debug("clearLines null: {} {} {} {}", lines.linesY, lines.linesYfolded, lines.linesSplited,lines)
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

	/** Log */
	private val log = LogManager.getLogger()

}
