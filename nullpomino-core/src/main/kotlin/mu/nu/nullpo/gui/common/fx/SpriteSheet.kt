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
package mu.nu.nullpo.gui.common.fx

import mu.nu.nullpo.gui.common.libs.Vector

/** 各種エフェクト state */
abstract class SpriteSheet(
	var pos:Vector = Vector(),
	/** アニメーション velocity */
	var vel:Vector = Vector(),
	/**	エフェクトサイズ*/
	var scale:Float = 1f,
	open var alpha:Float = 1f):Effect {
	/** アニメーション counter */
	var ticks = 0
		protected set
	/** X-coordinate */
	var x
		get() = pos.x
		set(v) {
			pos.x = v
		}
	/** Y-coordinate */
	var y:Float
		get() = pos.y
		set(v) {
			pos.y = v
		}

	/** その他パラメータ
	 * 0: Color
	 * 1: Skin
	 * 2: bone true to use bone block ([][][][])
	 * 3: darkness Darkness or brightness
	 */

	constructor(x:Float, y:Float, scale:Float = 1f, alpha:Float = 1f, vel:Vector = Vector())
		:this(Vector(x, y), vel, scale, alpha)

	constructor(x:Int, y:Int, scale:Float = 1f, alpha:Float = 1f, vel:Vector = Vector())
		:this(x.toFloat(), y.toFloat(), scale, alpha, vel)

	open val dx:Float get() = x
	open val dy:Float get() = y
	open val dx2:Float get() = x
	open val dy2:Float get() = y
	open val srcx:Int get() = 0
	open val srcy:Int get() = 0
	open val srcx2:Int get() = 1
	open val srcy2:Int get() = 1
}
