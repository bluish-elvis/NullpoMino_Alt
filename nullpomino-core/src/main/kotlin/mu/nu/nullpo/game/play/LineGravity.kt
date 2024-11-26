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

package mu.nu.nullpo.game.play

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Block.ATTRIBUTE
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Field.Companion.COORD_WALL
import mu.nu.nullpo.game.play.LineGravity.CASCADE.cascadeTime
import mu.nu.nullpo.util.GeneralUtil.filterNotNullIndexed
import org.apache.logging.log4j.LogManager

/** Line gravity types */
interface LineGravity {
	/** Check how many/much blocks can be fall
	 * @return pair first: Number of blocks can fall, second: Max Height of blocks that will fall */
	fun check(field:Field):Pair<Int, Int>
	/** Instant fall
	 * @return Number of lines that were cleared
	 */
	fun fallInstant(field:Field):Int
	/** Fall one line
	 * @return Number of lines remaining to fall
	 */
	fun fallSingle(field:Field):Int

	/**Only for ClearType.Line*/
	data object Native:LineGravity {
		/** Check how many/much blocks can be fall
		 * @return pair first: Number of lines can fall, second: Max Height of continuous lines */
		override fun check(field:Field) = field.run {
			(-hiddenHeight..<heightWithoutHurryupFloor).filter {getLineFlag(it)}
		}.let {l ->
			l.size to (l.sorted().fold(mutableListOf<Set<Int>>()) {a, t ->
				if(a.isEmpty()||a.last().maxOrNull()!=t-1) a += setOf(t)
				else a[a.lastIndex] = a[a.lastIndex]+t
				a
			}.maxOfOrNull {it.size}?:0)
		}

		override fun fallInstant(field:Field):Int = field.run {
			val lines = (-hiddenHeight..<heightWithoutHurryupFloor).filter {getLineFlag(it)}.toSet()
//			log.debug("fallInstant: {}", lines.sorted())
//			var bottomY = heightWithoutHurryupFloor-1
//
//			for(i in -hiddenHeight ..<heightWithoutHurryupFloor) if(getLineFlag(bottomY)) {
//				lines++
			lines.sorted().forEach {bottomY ->
				// Blockを1段上からコピー
				for(y in bottomY downTo 1-hiddenHeight) for(x in 0..<width) {
					setBlock(x, y, getBlock(x, y-1)?:Block())
					setLineFlag(y, getLineFlag(y-1))
				}
				// 一番上を空白にする
				for(x in 0..<width) delBlock(x, -hiddenHeight)
				setLineFlag(-hiddenHeight, false)
			}
			return lines.size
		}

		override fun fallSingle(field:Field):Int = field.run {
			val lines = (-hiddenHeight..<heightWithoutHurryupFloor).filter {getLineFlag(it)}
//			log.debug("fallSingle: {}", lines)
			val y = lines.maxOrNull()?:return 0
			// Blockを1段上からコピー
			for(k in y downTo 1-hiddenHeight) for(l in 0..<width) {
				setBlock(l, k, getBlock(l, k-1))
				setLineFlag(k, getLineFlag(k-1))
			}

			// 一番上を空白にする
			for(l in 0..<width) delBlock(l, -hiddenHeight)
			setLineFlag(-hiddenHeight, false)
			lines.size-1
		}
	}

	data object CASCADE:LineGravity {
		fun Field.canCascade():Boolean = cascadeTime().first>0
		/** @return pair first: Number of blocks can fall, second: Max Height of blocks that will */
		fun Field.cascadeTime():Pair<Int, Int> =
			(heightWithoutHurryupFloor-1 downTo -hiddenHeight).flatMap {y ->
				getRow(y).filterNotNullIndexed().filter {(_, it) -> !it.isEmpty&&!it.getAttribute(ATTRIBUTE.ANTIGRAVITY)}
					.mapNotNull {(x, blk) ->
						checkBlockLink(x, y)
						if(blk.getAttribute(ATTRIBUTE.TEMP_MARK)&&!blk.getAttribute(ATTRIBUTE.CASCADE_FALL))
							getHoleBelow(x, y).let {if(it>0) it else null} else null
					}
			}.let {it.size to (it.maxOrNull()?:0)}

		override fun check(field:Field):Pair<Int, Int> = field.cascadeTime()
		override fun fallInstant(field:Field) = field.run {
			setAllAttribute(false, ATTRIBUTE.CASCADE_FALL)
			val check = cascadeTime()
			do {
				val it = cascadeTime()
				repeat(it.second) {cascadeDown(1)}
			} while(it.first>0)
			check.second
		}

		override fun fallSingle(field:Field) = field.run {
			setAllAttribute(false, ATTRIBUTE.CASCADE_FALL)
			cascadeDown(1)
			cascadeTime().second
		}
	}

	data object CASCADE_SLOW:LineGravity {
		override fun check(field:Field):Pair<Int, Int> = CASCADE.check(field)
		override fun fallInstant(field:Field) = field.run {
			setAllAttribute(false, ATTRIBUTE.CASCADE_FALL)
			val check = cascadeTime()
			do {
				val it = cascadeTime()
				repeat(it.second) {cascadeDown(-1)}
			} while(it.first>0)
			check.second
		}

		override fun fallSingle(field:Field) = field.run {
			setAllAttribute(false, ATTRIBUTE.CASCADE_FALL)
			cascadeDown(-1)
			cascadeTime().second
		}
	}

	companion object {
		/** Log */
		internal val log = LogManager.getLogger()

		protected fun Field.cascadeDown(dir:Int) {

//			filterAttributeBlocks(ATTRIBUTE.TEMP_MARK).filter {!it.third.getAttribute(ATTRIBUTE.CASCADE_FALL)}
			for(y in if(dir>=0) hiddenHeight*-1..<heightWithoutHurryupFloor
			else heightWithoutHurryupFloor-1 downTo -hiddenHeight) for(x in 0..<width)
				getBlock(x, y)?.let {bTemp ->
					if(!bTemp.isEmpty&&isHoleBelow(x, y)&&getCoordAttribute(x, y+1)!=COORD_WALL&&
						bTemp.getAttribute(ATTRIBUTE.ANTIGRAVITY)
//						&&bTemp.getAttribute(ATTRIBUTE.TEMP_MARK)||bTemp.getAttribute(ATTRIBUTE.CASCADE_FALL)
					) {
//							bTemp.setAttribute(false, ATTRIBUTE.TEMP_MARK)
						bTemp.setAttribute(true, ATTRIBUTE.LAST_COMMIT)
						bTemp.setAttribute(getBlockEmpty(x, y+2, false), ATTRIBUTE.CASCADE_FALL)
						bTemp.offsetY = 0f
						if(bTemp.getAttribute(ATTRIBUTE.IGNORE_LINK)) bTemp.setAttribute(false, ATTRIBUTE.CONNECT_LEFT,
							ATTRIBUTE.CONNECT_DOWN, ATTRIBUTE.CONNECT_UP, ATTRIBUTE.CONNECT_RIGHT)
						setBlock(x, y+1, bTemp)
						delBlock(x, y)
					}
				}
		}

		fun values():Array<LineGravity> = arrayOf(Native, CASCADE, CASCADE_SLOW)

		fun valueOf(value:String):LineGravity = when(value) {
			"NATIVE" -> Native
			"CASCADE" -> CASCADE
			"CASCADE_SLOW" -> CASCADE_SLOW
			else -> throw IllegalArgumentException("No object mu.nu.nullpo.game.play.LineGravity.$value")

		}
	}
}
