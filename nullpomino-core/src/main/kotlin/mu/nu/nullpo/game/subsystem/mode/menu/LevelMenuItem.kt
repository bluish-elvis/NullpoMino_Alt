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
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.BaseFont.FONT.*
import mu.nu.nullpo.util.GeneralUtil.toInt

open class LevelMenuItem(name:String, displayName:String, color:COLOR, defaultValue:Int, range:IntRange, compact:Boolean,
	val showG:Boolean, val showD:Boolean):IntegerMenuItem(name, displayName, color, defaultValue, range, compact) {
	override val showHeight = super.showHeight+(if(showG) 2 else 0)+(if(showD) 2 else 0)

	constructor(name:String, displayName:String, color:COLOR, defaultValue:Int, range:IntRange, showG:Boolean = true,
		showD:Boolean = true):
		this(name, displayName, color, defaultValue, range, true, showG, showD)

	constructor(name:String, displayName:String, color:COLOR, defaultValue:Int, range:IntRange, compact:Boolean):
		this(name, displayName, color, defaultValue, range, compact, false, false)

	override fun draw(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, focus:Int) {
		super.draw(engine, playerID, receiver, y, focus)
		receiver.drawMenuSpeed(engine, 5.5f, y+.7f, (value-min)*1f/max, 5f)
		val spd = engine.speed
		if(showG) {
			val z = y+1+(!mini).toInt()
			val g = spd.gravity
			val d = spd.denominator
			receiver.drawMenu(engine, 0, z, "SPEED", BASE, color = COLOR.WHITE)
			receiver.drawMenuNum(engine, 1, z+1, if(g<0||d<0) engine.field.height*1f else g*1f/d, 7 to 4)
			receiver.drawMenu(engine, 2.5f, z+1, "GRAVITY", NANO, color, .5f)
			receiver.drawMenu(engine, 6, z, "%5d".format(g), NUM)
			receiver.drawMenuSpeed(engine, 5.2f, z+.9f, g, d, 5f)
			receiver.drawMenu(engine, 6, z+1, "%5d".format(d), NUM)
		}
		if(showD) {
			val z = y+1+(!mini).toInt()+(showG.toInt())*2

			for(i in 0..1) {
				val show = if(i==0) "ARE" to spd.are else "LINE" to spd.areLine

				receiver.drawMenu(engine, 4+i*3, z, String.format(if(i==0) "%2d/" else "%2d", show.second), NUM)
				receiver.drawMenu(engine, 3+i*2.5f, z+.5f, show.first, NANO, color, .5f)
			}
			for(i in 0..2) {
				val show = when(i) {
					0 -> "LINE" to spd.lineDelay
					1 -> "LOCK" to spd.lockDelay
					else -> " DAS" to spd.das
				}
				receiver.drawMenu(engine, 8-i*3, z+1, String.format(if(i==1) "%2d+" else "%2d", show.second), NUM)
				receiver.drawMenu(engine, 6.5f-i*3, z+1f, show.first, NANO, color, .5f)
			}
			receiver.drawMenu(engine, 0f, z*1f, "DELAYS", NANO, color, .75f)
		}
	}

}
