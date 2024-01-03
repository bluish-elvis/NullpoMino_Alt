/*
 Copyright (c) 2024,
 This library class was created by 0xFC963F18DC21 / Shots243
 It is part of an extension library for the game NullpoMino (copyright 2010-2024)

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis

 Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.

 THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 THIS KOTLIN VERSION WAS NOT MADE IN ASSOCIATION WITH THE LIBRARY CREATOR.

 Original Repository: https://github.com/Shots243/ModePile

 When using this library in a mode / library pack of your own, the following
 conditions must be satisfied:
     - This license must remain visible at the top of the document, unmodified.
     - You are allowed to use this library for any modding purpose.
         - If this is the case, the Library Creator must be credited somewhere.
             - Source comments only are fine, but in a README is recommended.
     - Modification of this library is allowed, but only in the condition that a
       pull request is made to merge the changes to the repository.

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

/*
 Copyright (c) 2023-2024, NullNoname
 All rights reserved.

 Converted to Kotlin and modified by Venom_Nhelv
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

package mu.nu.nullpo.gui.common.fx.particles

import mu.nu.nullpo.gui.common.AbstractRenderer
import zeroxfc.nullpo.custom.libs.Vector
import zeroxfc.nullpo.custom.libs.Interpolation

class PolyFrag(
  /** Lifetime */
	maxLifetime:Int,
  /** X-coordinate */
	x:Float,
  /** Y-coordinate */
	y:Float,
  /** Velocity vector */
	vel: Vector = Vector.zero(),
  /** Acceleration vector */
	acc: Vector = Vector.zero(),
  /** Velocity decrease float */
	friction:Float = 1f,
  /** X size */
	size:Int,
  sizeEase:Float = 6f,
  /** Red color component 0-255 */
	red:Int = DEFAULT_COLOR,
  /*** Green color component 0-255 */
	green:Int = DEFAULT_COLOR,
  /** Blue color component 0-255 */
	blue:Int = DEFAULT_COLOR,
  /** brightness anim variance -50~+50*/
	val vv:Int = 0,
  /** Alpha component 0-255 */
	alphaI:Int = DEFAULT_COLOR,
):Particle(null, maxLifetime, x, y, vel, acc, friction, size, sizeEase, red, green, blue, alphaI) {

	override fun draw(i:Int, r:AbstractRenderer) {

	}

	override fun update(r:AbstractRenderer):Boolean {

		x += vel.x
		y += vel.y
		vel *= friction
		vel += acc
		ur = Interpolation.lerp(red, redEnd, ticks.toDouble()/maxLifetime)
		ug = Interpolation.lerp(green, greenEnd, ticks.toDouble()/maxLifetime)
		ub = Interpolation.lerp(blue, blueEnd, ticks.toDouble()/maxLifetime)
		ua = Interpolation.lerp(alphaI, alphaEnd, ticks.toDouble()/maxLifetime)
		if(sizeEase>=1)
			us = Interpolation.smoothStep(size.toFloat(), 0f, ticks.toFloat()/maxLifetime, sizeEase)

		return ++ticks>maxLifetime||ua<=0||
			x<-us/2&&vel.x<0||x>640+us/2&&vel.x>0||
			y<-us/2&&vel.y<0||y>480+us/2&&vel.y<0
	}
}
