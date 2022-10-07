/*
 * Copyright (c) 2010-2022, NullNoname
 * Kotlin converted and modified by Venom=Nhelv.
 * THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NullNoname nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package mu.nu.nullpo.game.subsystem.mode.menu

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.util.CustomProperties

open class IntegerMenuItem(name:String, displayName:String, color:COLOR, defaultValue:Int, val range:IntRange,
	compact:Boolean = false, perRule:Boolean = false)
	:AbstractMenuItem<Int>(name, displayName, color, defaultValue, compact, perRule), Comparable<Int> {
	constructor(name:String, displayName:String, color:COLOR, defaultValue:Int, min:Int, max:Int,
		compact:Boolean = false, perRule:Boolean = false):
		this(name, displayName, color, defaultValue, min..max, compact, perRule)

	val min get() = range.first
	val max get() = range.last

	override fun equals(other:Any?):Boolean = if(other is Int) value==other else super.equals(other)
	override operator fun compareTo(other:Int) = value.compareTo(other)
	operator fun plus(y:Int) = value+y
	operator fun Int.plus(y:IntegerMenuItem) = y.plus(value)
	operator fun plusAssign(y:Int) = change(y)
	operator fun minus(y:Int) = value-y
	operator fun minusAssign(y:Int) = change(-y)
	operator fun times(y:Int) = value*y
	operator fun div(y:Int) = value/y
	operator fun rem(y:Int) = value%y
	infix fun until(to:Int):IntRange = value.until(to)
	operator fun rangeTo(to:Int):IntRange = value.rangeTo(to)

	override val valueString:String
		get() = "$value"

	override fun change(dir:Int, fast:Int, cur:Int) {
		value += dir*when(fast) {
			1 -> minOf(5, max/200)
			2 -> minOf(10, max/100)
			else -> 1
		}
		if(value<range.first) value = max
		if(value>range.last) value = min
	}

	override fun load(prop:CustomProperties, propName:String) {
		value = prop.getProperty(propName, DEFAULT_VALUE)
	}

	override fun save(prop:CustomProperties, propName:String) {
		prop.setProperty(propName, value)
	}

	override fun hashCode():Int {
		var result = value
		result = 31*result+min
		result = 31*result+max
		return result
	}

}
