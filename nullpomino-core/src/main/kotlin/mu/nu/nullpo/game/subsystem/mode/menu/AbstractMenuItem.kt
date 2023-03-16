/*
 * Copyright (c) 2010-2023, NullNoname
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

import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.BaseFont.FONT
import mu.nu.nullpo.util.CustomProperties

abstract class AbstractMenuItem<T>(
	val name:String, val label:String, val color:COLOR, val defaultValue:T,
	val compact:Boolean = false, val perRule:Boolean = false
) {
	var value:T = defaultValue
	open val valueString:String get() = "$value"
	private val mini get() = compact&&(label+valueString).length<=9
	open val colMax:Int = 1
	open val showHeight:Int get() = if(mini) 1 else 2
	/** Change the value.
	 * @param dir Direction pressed: -1 = left, 1 = right.
	 * If 0, update without changing any settings.
	 * @param fast 0 by default, +1 if C(Alt.R.Spin) held, +2 if D(Swap) held.
	 */
	abstract fun change(dir:Int, fast:Int = 0, cur:Int = 0)

	fun propName(propName:String, ruleName:String, playerID:Int) =
		"$propName.$name${if(perRule&&ruleName.isNotEmpty()) ".$ruleName" else if(playerID>=0) ".p$playerID" else ""}"

	abstract fun load(prop:CustomProperties, propName:String)
	abstract fun save(prop:CustomProperties, propName:String)

	/**
	 * Draw Menu
	 * @param engine GameEngine
	 * @param playerID Int
	 * @param receiver EventReceiver
	 * @param y Int
	 * @param focus if Cursor selected 0, false=recommend -1
	 * @return Int
	 */
	open fun draw(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, focus:Int = -1) {
		if(mini) {
			receiver.drawMenuFont(engine, 1, y, "${label}:", color = color)
			if(focus==0) receiver.drawMenuFont(engine, 0, y, "\u0082", true)
			receiver.drawMenu(
				engine, label.length+2, y, valueString,
				if(valueString.all {it.isDigit()}) FONT.NUM else FONT.NORMAL,
				if(focus==0) COLOR.RAINBOW else COLOR.WHITE
			)
		} else {
			receiver.drawMenuFont(engine, 0, y, label, color = color)
			if(focus==0) receiver.drawMenuFont(engine, 0, y+1, "\u0082", true)
			receiver.drawMenu(
				engine, 1, y+1, valueString,
				if(valueString.all {it.isDigit()}) FONT.NUM else FONT.NORMAL,
				if(focus==0) COLOR.RAINBOW else COLOR.WHITE
			)
		}
	}

	fun draw(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, focus:Boolean) =
		draw(engine, playerID, receiver, y, if(focus) 0 else -1)

}
