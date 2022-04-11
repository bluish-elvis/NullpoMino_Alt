/*
 * Copyright (c) 2010-2021, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
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
import mu.nu.nullpo.util.GeneralUtil.getONorOFF
import mu.nu.nullpo.util.GeneralUtil.getOX

open class BooleanMenuItem(name:String, label:String, color:COLOR, defaultValue:Boolean, compact:Boolean = label.length<8,
	perRule:Boolean = false)
	:AbstractMenuItem<Boolean>(name, label, color, defaultValue, compact, perRule), Comparable<Boolean> {
	override fun compareTo(other:Boolean):Int = value.compareTo(other)

	override fun equals(other:Any?) = if(other is Boolean) value==other else super.equals(other)
	operator fun not() = value.not()

	infix fun and(to:Boolean) = value.and(to)
	infix fun Boolean.and(to:BooleanMenuItem) = this.and(to.value)
	infix fun or(to:Boolean) = value.or(to)
	infix fun Boolean.or(to:BooleanMenuItem) = this.or(to.value)
	infix fun xor(to:Boolean) = value.xor(to)

	override val valueString:String
		get() = if(compact&&label.length>=6) value.getOX else value.getONorOFF()

	override fun change(dir:Int, fast:Int, cur:Int) {
		value = !value
	}

	override fun load(prop:CustomProperties, propName:String) {
		value = prop.getProperty(propName, DEFAULT_VALUE)
	}

	override fun save(prop:CustomProperties, propName:String) {
		prop.setProperty(propName, value)
	}

	override fun hashCode():Int = javaClass.hashCode()
}