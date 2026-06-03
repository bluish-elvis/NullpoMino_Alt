/*
 Copyright (c) 2026, NullNoname
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

package mu.nu.nullpo.game.play.fallRule

import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.play.GameEngine

/**Only for ClearType.Line*/
data object Native:LineGravity {
	/** Check how many/much blocks can be fall
	 * @return pair first: Number of lines can fall, second: Max Height of continuous lines */
	override fun check(field:Field) = field.run {
		(allSpaceRows-lockedLines).filter {getLineFlag(it)}
	}.let {l ->
		l.size to (l.sorted().fold(mutableListOf<Set<Int>>()) {a, t ->
			if(a.isEmpty()||a.last().maxOrNull()!=t-1) a += setOf(t)
			else a[a.lastIndex] = a[a.lastIndex]+t
			a
		}.maxOfOrNull {it.size}?:0)
	}

	override fun fallInstant(field:Field):Int = field.run {
		val lines = (allSpaceRows-lockedLines).filter {getLineFlag(it)}.toSet()
//			log.debug("fallInstant: {}", lines.sorted())
//			var bottomY = heightWithoutHurryupFloor-1
//
//			for(i in -hiddenHeight ..<heightWithoutHurryupFloor) if(getLineFlag(bottomY)) {
//				lines++
		lines.sorted().forEach {bottomY ->
			// Blockを1段上からコピー
			for(y in bottomY downTo 1-hiddenHeight) for(x in 0..<width) {
				setBlock(x, y, getBlock(x, y-1))
				setLineFlag(y, getLineFlag(y-1))
			}
			// 一番上を空白にする
			for(x in 0..<width) delBlock(x, -hiddenHeight)
			setLineFlag(-hiddenHeight, false)
		}
		return lines.size
	}

	override fun fallSingle(field:Field):Int = field.run {
		val lines = (allSpaceRows-lockedLines).filter {getLineFlag(it)}
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

	override fun statLineClear(engine:GameEngine):Boolean = engine.run {
// Linesを1段落とす
		val (fc1, fc2) = check(field)
		statc[7] = fc1
		statc[8] = fc2
		if(ruleOpt.lineFallAnim&&stime>=lineDelay-(fc1-1).coerceAtLeast(0)) {
			lineGravityType.fallSingle(field)//field.downFloatingBlocksSingleLine()
			depth++
		}
		return stime>=lineDelay
	}

	override fun lineClearEnd(engine:GameEngine) = engine.run {
		fallInstant(field).also {depth += it}
		lastLinesY.filter {it.max()>=field.highestBlockY}.distinctBy {it.size>=3}.forEach {
			playSE(
				when {
					frame==GameEngine.Frame.GB -> "linefallold"
					it.size>=4 -> "linefall1"
					it.size<=1 -> "linefall"
					else -> "linefall0"
				},
				maxOf(0.8f, 1.2f-it.max()/3f/fieldHeight),
				minOf(1f, 0.4f+speed.lineDelay*0.1f)
			)
		}
	}
}