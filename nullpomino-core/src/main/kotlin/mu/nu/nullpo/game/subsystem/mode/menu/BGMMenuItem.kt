/*
 * Copyright (c) 2024, NullNoname
 * Kotlin converted and modified by Venom=Nhelv.
 *
 * THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
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

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.BaseFont

class BGMMenuItem(name:String, color:EventReceiver.COLOR, defaultValue:Int = 0):
	StringsMenuItem(name, "BGM", color, defaultValue, BGMStatus.BGM.values.map {it.drawName}, false, false) {
	override val valueString get() = choiceNames[value]
	override val colMax = 1
	override val showHeight = 2
	override fun draw(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, focus:Int) {
		val cur = focus==0
		receiver.drawMenuFont(engine, 0, y, "BGM", color)
		receiver.drawMenuFont(engine, 0, y+1, valueString, cur)
		receiver.drawMenuNum(engine, 8, y, "%2d".format(value), cur)
		if(cur) receiver.drawMenuFont(engine, 7, y, BaseFont.CURSOR, true)
	}
}
