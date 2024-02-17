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

	@Serializable data class ScoreRow(val st:Statistics = Statistics()):Comparable<ScoreRow> {
		val sc get() = st.score
		val lv get() = st.level
		val li get() = st.lines
		val ti get() = st.time.let {if(it<0) Int.MAX_VALUE else it}
		override operator fun compareTo(other:ScoreRow):Int =
			compareValuesBy(this, other, {it.sc}, {it.lv}, {it.li}, {-it.ti})
	}

	@Serializable data class GrandRow(val grade:Int, val st:Statistics, val clear:Int, val medals:Medals):Comparable<GrandRow> {
		constructor():this(0, Statistics(), 0, Medals())

		val level get() = st.level
		val time get() = st.time.let {if(it<0) Int.MAX_VALUE else it}
		override operator fun compareTo(other:GrandRow):Int =
			compareValuesBy(this, other, {it.grade}, {it.level}, {it.clear}, {-it.time})

		@Suppress("PropertyName") @Serializable
		data class Medals(var ST:MutableList<Int> = MutableList(3) {0}, var SK:Int = 0, var AC:Int = 0, var CO:Int = 0, var RE:Int = 0, var RO:Int = 0)
	}

}
