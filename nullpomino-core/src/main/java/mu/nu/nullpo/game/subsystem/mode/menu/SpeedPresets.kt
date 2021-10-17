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

import mu.nu.nullpo.game.component.SpeedParam
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties

class SpeedPresets(color:COLOR, defaultPreset:Int, val showG:Boolean = true, showD:Boolean = true)
	:IntegerMenuItem("presetNumber", "Speeds", color, defaultPreset, 0..99), PresetItem {
	val spd = SpeedParam()
	val showD:Boolean = showD||!(showG&&showD)
	override val showHeight:Int
		get() = 1+(if(showG) 2 else 0)+(if(showD) 2 else 0)
	override val colMax = 2+if(showG) 2 else 0+if(showD) 5 else 0
	override fun draw(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, focus:Int) {
		val spd = engine.speed
		if(showG) {
			val g = spd.gravity
			val d = spd.denominator
			receiver.drawMenuFont(engine, playerID, 0, y, "SPEED", color = color)
			receiver.drawSpeedMeter(engine, playerID, -13, y+1, g, d, 5)
			receiver.drawMenuNum(engine, playerID, 6, y, String.format("%5d", g))
			receiver.drawMenuNum(engine, playerID, 6, y+1, String.format("%5d", d))

		}
		if(showD) {
			val y = y+if(showG) 2 else 0

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

				receiver.drawMenuNum(engine, playerID, 8-i*3, y,
					String.format(if(i==1) "%2d+" else "%2d", show.second))
				receiver.drawMenuNano(engine, playerID, 14-i*6, y*2+1, show.first, color, .5f)
			}
			receiver.drawMenuNano(engine, playerID, 0, y*2-2, "DELAYS", color, .5f)
		}
	}

	override fun change(dir:Int, fast:Int, cur:Int) {
		fun Int.proc(dir:Int, fast:Int):Int = (this+dir).let {
			when {
				it<min -> max
				it>max -> min
				else -> it
			}
		}
		if(showG) when(cur) {
			2 -> spd.gravity = spd.gravity.proc(dir, fast)
			3 -> spd.denominator = spd.denominator.proc(dir, fast)
			4 -> spd.are = spd.are.proc(dir, fast)
			5 -> spd.areLine = spd.areLine.proc(dir, fast)
			6 -> spd.lineDelay = spd.lineDelay.proc(dir, fast)
			7 -> spd.lockDelay = spd.lockDelay.proc(dir, fast)
			8 -> spd.das = spd.das.proc(dir, fast)
			else -> value
		} else when(cur) {
			2 -> spd.are = spd.are.proc(dir, fast)
			3 -> spd.areLine = spd.areLine.proc(dir, fast)
			4 -> spd.lineDelay = spd.lineDelay.proc(dir, fast)
			5 -> spd.lockDelay = spd.lockDelay.proc(dir, fast)
			6 -> spd.das = spd.das.proc(dir, fast)
			else -> value = value.proc(dir, fast)
		}
	}

	override fun presetSave(prop:CustomProperties, modeName:String) {
		if(showG) {
			spd.gravity = prop.getProperty("$modeName.gravity.$value", spd.gravity)
			spd.denominator = prop.getProperty("$modeName.denominator.$value", spd.denominator)
		}
		if(showD) {
			spd.are = prop.getProperty("$modeName.are.$value", spd.are)
			spd.areLine = prop.getProperty("$modeName.areLine.$value", spd.areLine)
			spd.lineDelay = prop.getProperty("$modeName.lineDelay.$value", spd.lineDelay)
			spd.lockDelay = prop.getProperty("$modeName.lockDelay.$value", spd.lockDelay)
			spd.das = prop.getProperty("$modeName.das.$value", spd.das)
		}
	}

	override fun presetLoad(prop:CustomProperties, modeName:String) {
		if(showG) {
			prop.getProperty("$modeName.gravity.$value", 1)
			prop.setProperty("$modeName.denominator.$value", spd.denominator)
		}
		if(showD) {
			prop.setProperty("$modeName.are.$value", spd.are)
			prop.setProperty("$modeName.areLine.$value", spd.areLine)
			prop.setProperty("$modeName.lineDelay.$value", spd.lineDelay)
			prop.setProperty("$modeName.lockDelay.$value", spd.lockDelay)
			prop.setProperty("$modeName.das.$value", spd.das)
		}
	}

}