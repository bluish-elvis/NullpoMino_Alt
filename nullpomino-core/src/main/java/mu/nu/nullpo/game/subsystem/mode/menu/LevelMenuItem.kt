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

import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine

open class LevelMenuItem(name:String, displayName:String, color:COLOR, defaultValue:Int, range:IntRange, compact:Boolean,
	val showG:Boolean, val showD:Boolean):IntegerMenuItem(name, displayName, color, defaultValue, range, compact) {
	override val showHeight = super.showHeight+(if(showG) 2 else 0)+(if(showD) 2 else 0)+if(showG||showD) 1 else 0

	constructor(name:String, displayName:String, color:COLOR, defaultValue:Int, range:IntRange, showG:Boolean = true,
		showD:Boolean = true):
		this(name, displayName, color, defaultValue, range, false, showG, showD)

	constructor(name:String, displayName:String, color:COLOR, defaultValue:Int, range:IntRange, compact:Boolean):
		this(name, displayName, color, defaultValue, range, compact, false, false)

	override fun draw(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, focus:Int) {
		super.draw(engine, playerID, receiver, y, focus)
		if(!compact) receiver.drawSpeedMeter(engine, playerID, -8, y, (value-min)*1f/max, 5f)
		val spd = engine.speed
		if(showG) {
			val g = spd.gravity
			val d = spd.denominator
			receiver.drawMenuFont(engine, playerID, 0, y+2, "SPEED", color = COLOR.WHITE)
			receiver.drawSpeedMeter(engine, playerID, -13, y+3, g, d, 5)
			receiver.drawMenuNum(engine, playerID, 6, y+2, String.format("%5d", g))
			receiver.drawMenuNum(engine, playerID, 6, y+3, String.format("%5d", d))
		}
		if(showD) {
			val y = y+if(showG) 4 else 2

			for(i in 0..1) {
				val show = if(i==0) "ARE" to spd.are else "LINE" to spd.areLine

				receiver.drawMenuNum(engine, playerID, 4+i*3, y, String.format(if(i==0) "%2d/" else "%2d", show.second))
				receiver.drawMenuNano(engine, playerID, 6+i*5, y*2+1, show.first, color, .5f)
			}
			for(i in 0..2) {
				val show = when(i) {
					0 -> "LINE" to spd.lineDelay
					1 -> "LOCK" to spd.lockDelay
					else -> "DAS" to spd.das
				}
				receiver.drawMenuNum(engine, playerID, 8-i*3, y+1, String.format(if(i==1) "%2d+" else "%2d", show.second))
				receiver.drawMenuNano(engine, playerID, 14-i*6, y*2+2, show.first, color, .5f)
			}
			receiver.drawMenuNano(engine, playerID, 0, y*2, "DELAYS", color, .5f)
		}
	}

}