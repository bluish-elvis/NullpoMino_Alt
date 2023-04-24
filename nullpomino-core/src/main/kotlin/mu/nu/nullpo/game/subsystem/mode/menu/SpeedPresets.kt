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

import mu.nu.nullpo.game.component.SpeedParam
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toInt

open class SpeedPresets(color:COLOR, defaultPreset:Int = 0,
	/** true to Enable Gravity/denominator*/
	val showG:Boolean = true,
	/** true to Enable ARE,Delay,DAS adjust*/
	showD:Boolean = true):IntegerMenuItem("presetNumber", "Speeds", color, defaultPreset, 0..99), PresetItem {
	val spd = SpeedParam()
	val showD:Boolean = showD||!showG
	override val showHeight:Int
		get() = 1+(if(showG) 2 else 0)+(if(showD) 2 else 0)
	override val colMax = 2+showG.toInt()*2+showD.toInt()*5
	override fun draw(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, focus:Int) {

		receiver.drawMenuFont(
			engine, 0, y,
			when(focus) {
				0 -> "LOAD"
				1 -> "SAVE"
				else -> "PRESET"
			},
			COLOR.GREEN,
		)

		if(focus in 0..1)
			receiver.drawMenuFont(engine, 6, y, "\u0082", true)
		receiver.drawMenuNum(engine, 7, y, valueString)

		if(showG) {
			val g = spd.gravity
			val d = spd.denominator
			receiver.drawMenuFont(engine, 0, y+1, "SPEED", color = color)
			receiver.drawScoreSpeed(engine, 0f, y+1.5f, g, d, 5f)
			receiver.drawMenuNum(engine, 6, y+1, "%5d".format(g), focus==2)
			receiver.drawMenuNum(engine, 6, y+2, "%5d".format(d), focus==3)
			if(focus in 2..3)
				receiver.drawMenuFont(engine, 5, y+focus-1, "\u0082", true)
		}
		if(showD) {
			val y = y+if(showG) 3 else 1

			for(i in 0..1) {
				val show = if(i==0) "ARE" to spd.are else "LINE" to spd.areLine
				val pos = 2+showG.toInt()*2+i
				if(focus==pos)
					receiver.drawMenuFont(engine, 3+i*3, y, "\u0082", true)

				receiver.drawMenuNum(engine, 4+i*3, y, String.format(if(i==0) "%2d/" else "%2d", show.second), focus==pos)
				receiver.drawMenuNano(engine, 6+i*5, y*2+1, show.first, color, .5f)
			}
			for(i in 0..2) {
				val show = when(i) {
					0 -> "LINE" to spd.lineDelay
					1 -> "LOCK" to spd.lockDelay
					else -> "DAS" to spd.das
				}
				val pos = 4+showG.toInt()*2+i
				if(focus==pos)
					receiver.drawMenuFont(engine, 7-i*3, y+1, "\u0082", true)

				receiver.drawMenuNum(engine, 8-i*3, y+1, String.format(if(i==1) "%2d+" else "%2d", show.second), focus==pos)
				receiver.drawMenuNano(engine, 14-i*6, y*2+3, show.first, color, .5f)
			}
			receiver.drawMenuNano(engine, 0, y*2-2, "DELAYS", color, .5f)

		}
	}

	override fun change(dir:Int, fast:Int, cur:Int) {
		fun Int.proc(dir:Int, fast:Int, mi:Int = min, ma:Int = max):Int = (this+dir*when(fast) {
			1 -> minOf(5, max/200)
			2 -> minOf(10, max/100)
			else -> 1
		}).let {
			when {
				it<mi -> ma
				it>ma -> mi
				else -> it
			}
		}
		if(showG) when(cur) {
			2 -> spd.gravity = spd.gravity.proc(dir, fast, -1, 99999)
			3 -> spd.denominator = spd.denominator.proc(dir, fast, -1, 99999)
			4 -> spd.are = spd.are.proc(dir, fast, 0, 99)
			5 -> spd.areLine = spd.areLine.proc(dir, fast, 0, 99)
			6 -> spd.lineDelay = spd.lineDelay.proc(dir, fast, 0, 99)
			7 -> spd.lockDelay = spd.lockDelay.proc(dir, fast, 0, 99)
			8 -> spd.das = spd.das.proc(dir, fast, 0, 99)
			else -> value = value.proc(dir, fast)
		} else if(showD) when(cur) {
			2 -> spd.are = spd.are.proc(dir, fast, 0, 99)
			3 -> spd.areLine = spd.areLine.proc(dir, fast, 0, 99)
			4 -> spd.lineDelay = spd.lineDelay.proc(dir, fast, 0, 99)
			5 -> spd.lockDelay = spd.lockDelay.proc(dir, fast, 0, 99)
			6 -> spd.das = spd.das.proc(dir, fast, 0, 99)
			else -> value = value.proc(dir, fast)
		}
	}

	override fun presetSave(engine:GameEngine, prop:CustomProperties, modeName:String, setId:Int) {
		if(showG) {
			prop.setProperty("$modeName.gravity.$setId", 1)
			prop.setProperty("$modeName.denominator.$setId", spd.denominator)
		}
		if(showD) {
			prop.setProperty("$modeName.are.$setId", spd.are)
			prop.setProperty("$modeName.areLine.$setId", spd.areLine)
			prop.setProperty("$modeName.lineDelay.$setId", spd.lineDelay)
			prop.setProperty("$modeName.lockDelay.$setId", spd.lockDelay)
			prop.setProperty("$modeName.das.$setId", spd.das)
		}
		engine.speed.replace(spd)
	}

	override fun presetLoad(engine:GameEngine, prop:CustomProperties, modeName:String, setId:Int) {
		if(showG) {
			spd.gravity = prop.getProperty("$modeName.gravity.$setId", spd.gravity)
			spd.denominator = prop.getProperty("$modeName.denominator.$setId", spd.denominator)
		}
		if(showD) {
			spd.are = prop.getProperty("$modeName.are.$setId", spd.are)
			spd.areLine = prop.getProperty("$modeName.areLine.$setId", spd.areLine)
			spd.lineDelay = prop.getProperty("$modeName.lineDelay.$setId", spd.lineDelay)
			spd.lockDelay = prop.getProperty("$modeName.lockDelay.$setId", spd.lockDelay)
			spd.das = prop.getProperty("$modeName.das.$setId", spd.das)
		}
		engine.speed.replace(spd)
	}

}
