/*
 Copyright (c) 2010-2024, NullNoname
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

package mu.nu.nullpo.game.subsystem.mode.menu

import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.util.CustomProperties
import kotlin.enums.EnumEntries

class EnumMenuItem<T:Enum<T>>(name:String, displayName:String, color:EventReceiver.COLOR, default:T,
	val all:EnumEntries<T>, compact:Boolean = false, perRule:Boolean = false)
	:AbstractMenuItem<T>(name, displayName, color, default, compact, perRule) {
	var ordinal = defaultValue.ordinal
		get() = value.ordinal
		set(value) {
			field = value
			this.value = all.getOrElse(value.mod(all.size)+min) {defaultValue}
		}

	override val valueString:String
		get() = "$value"

	val min get() = all.indices.first
	val max get() = all.indices.last
	/** Change the value.
	 * @param dir Direction pressed: -1 = left, 1 = right.
	 * If 0, update without changing any settings.
	 * @param fast 0 by default, +1 if C(Alt.R.Spin) held, +2 if D(Swap) held.
	 */
	override fun change(dir:Int, fast:Int, cur:Int) {
		ordinal += dir*when(fast) {
			1 -> minOf(5, max/200)
			2 -> minOf(10, max/100)
			else -> 1
		}
		if(ordinal<min) ordinal = max
		else if(ordinal>max) ordinal = min
	}

	override fun load(prop:CustomProperties, propName:String):T = prop.getProperty(propName, defaultValue.ordinal).let {
		ordinal = it.coerceIn(min, max)
		all[it]
	}

	override fun save(prop:CustomProperties, propName:String) {
		prop.setProperty(propName, ordinal)
	}

}
