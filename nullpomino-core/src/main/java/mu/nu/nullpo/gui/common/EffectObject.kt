/* Copyright (c) 2010, NullNoname
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of NullNoname nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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
 * POSSIBILITY OF SUCH DAMAGE. */
package mu.nu.nullpo.gui.common

import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine

/** 各種エフェクト state */
abstract class EffectObject(
	/** X-coordinate */
	var x:Float = 0f,
	/** Y-coordinate */
	var y:Float = 0f,
	/**	エフェクトサイズ*/
	var scale:Float = 1f,
	/** エフェクト角度*/
	var angle:Float = 1f,
	/** アニメーション velocity */
	var vel:vector = vector()) {
	/** アニメーション counter */
	var anim:Int = 0
		protected set(v) {
			field = v
			if(v<=-1||v>=1000) isExpired = true
		}

	var isExpired:Boolean = false; protected set

	/** その他パラメータ
	 * 0: Color
	 * 1: Skin
	 * 2: bone true to use bone block ([][][][])
	 * 3: darkness Darkness or brightness
	 */
	constructor(x:Float, y:Float):this(x, y, 1f, 1f)
	constructor(x:Int, y:Int):this(x.toFloat(), y.toFloat(), 1f, 1f)

	abstract fun update()
	data class vector(var x:Float = 0f, var y:Float = 0f)
}

class FragAnim(val type:ANIM, x:Int, y:Int, val color:Int, val spd:Int = 0, scale:Float = 1f, vel:vector = vector(0f, 0f))
	:EffectObject(x.toFloat(), y.toFloat(), scale, vel = vel) {
	override fun update() {
		anim += if(type==ANIM.HANABI) 1 else maxOf(1, spd)
		if(anim>=when(type) {
				ANIM.GEM -> 60
				ANIM.HANABI -> 48
				else -> 36
			}) isExpired = true
	}

	enum class ANIM {
		// Normal Block
		BLOCK,

		// Controlled Block
		SPARK,

		// Gem Block
		GEM,

		// Fireworks
		HANABI
	}
}

class BeamH(x:Int, y:Int, val w:Int, val h:Int):EffectObject(x, y) {
	override fun update() {
		anim++
		if(anim>=16) isExpired = true
	}
}

class PopupPoint(x:Int, y:Int, val pts:Int, private val c:Int):EffectObject(x, y) {
	val color get() = if(c==COLOR.RAINBOW.ordinal) EventReceiver.getRainbowColor(anim).ordinal else c
	override fun update() {
		y += vel.y
		vel.y = maxOf(0.2f, 0.5f-anim*0.03f)
		anim++
		if(anim>=72) isExpired = true
	}
}

class PopupCombo(x:Int, y:Int, val pts:Int, val type:CHAIN, val ex:Int=0):EffectObject(x.toFloat(), y.toFloat(), vel = vector(-1f, 0f)) {
	val ox=x
	override fun update() {
		x += vel.x
		vel.x = (ox+ex-x)*0.1f
		anim++
		if(anim>=60) isExpired = true
	}

	enum class CHAIN {
		COMBO, B2B
	}
}

class PopupAward(x:Int, y:Int, val event:GameEngine.ScoreEvent, val movetime:Int, val ex:Int = 0):EffectObject(x, y) {
	val ox=x
	override fun update() {
		anim++
		if(anim>movetime) {
			x += vel.x
			vel.x = (ox+ex-x)*0.1f
		}
		event.piece?.setDarkness(0f)
		if(anim>=66) isExpired = true
	}
}

class PopupBravo(x:Int, y:Int):EffectObject(x, y) {
	override fun update() {
		anim++
		if(anim>=100) isExpired = true
	}
}
