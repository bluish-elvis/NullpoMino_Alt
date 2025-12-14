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

package mu.nu.nullpo.game.event

import kotlinx.serialization.Serializable
import mu.nu.nullpo.game.component.Statistics

interface Rankable:Comparable<Rankable> {
	val st:Statistics
	val sc get() = st.score
	val lv get() = st.level
	val li get() = st.lines
	val clear get() = st.rollClear
	val lives get() = st.lives
	val ti get() = st.time.let {if(it<0) Int.MAX_VALUE else it}
	val rp:Float get() = st.vs
	override operator fun compareTo(other:Rankable):Int =
		rp.compareTo(other.rp)

	@Serializable
	data class ScoreRow(override val st:Statistics = Statistics()):Rankable {
		override fun compareTo(other:Rankable):Int =
			if(other is ScoreRow)
				compareValuesBy(this, other, {it.clear}, {it.sc}, {it.lives}, {it.lv}, {it.li}, {-it.ti}, {it.rp})
			else super.compareTo(other)

	}
	@Serializable
	data class TimeRow(override val st:Statistics = Statistics()):Rankable {
		override fun compareTo(other:Rankable):Int =
			if(other is TimeRow)
				compareValuesBy(this, other, {it.clear}, {if(it.ti<=0) Int.MIN_VALUE else -it.ti}, {it.lives},
					{it.sc}, {it.lv}, {it.li}, {it.rp})
			else super.compareTo(other)

	}

	@Serializable
	data class GrandRow(val grade:Int, override val st:Statistics, val medals:Medals):Rankable {
		constructor():this(0, Statistics(), Medals())

		override operator fun compareTo(other:Rankable):Int =
			if(other is GrandRow)
				compareValuesBy(this, other, {it.grade}, {it.lv}, {it.clear}, {-it.ti}, {it.sc}, {it.lives}, {it.rp})
			else super.compareTo(other)

		@Suppress("PropertyName")
		@Serializable
		data class Medals(var ST:MutableList<Int> = MutableList(3) {0},
			var SK:Int = 0, var AC:Int = 0, var CO:Int = 0, var RE:Int = 0, var RO:Int = 0) {
			fun reset() {
				ST.fill(0)
				SK = 0
				AC = 0
				CO = 0
				RE = 0
				RO = 0
			}
		}
	}

	companion object {
	}

}
