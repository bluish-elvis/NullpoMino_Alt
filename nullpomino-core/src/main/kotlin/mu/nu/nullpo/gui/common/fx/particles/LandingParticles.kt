/*
 Copyright (c) 2019-2024,
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

package mu.nu.nullpo.gui.common.fx.particles

import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.fx.Effect
import mu.nu.nullpo.gui.common.fx.particles.Particle.ParticleShape
import zeroxfc.nullpo.custom.libs.Interpolation.lerp
import zeroxfc.nullpo.custom.libs.Vector
import kotlin.random.Random

class LandingParticles(
	num:Int,
	minX:Float,
	maxX:Float,
	startY:Float,
	yVar:Float,
	red:Int,
	green:Int,
	blue:Int,
	alpha:Int,
	variance:Int,
	maxVel:Float,
	upChance:Float,
	/** Randomizer*/
	randomizer:Random = Random
):Effect {
	/**
	 * Adds a number of landing particles.<br></br>
	 * Parameters are min start x, max start x, start y, start y variance,
	 * red, green, blue, alpha, variance (all `int` types),
	 * maximum velocity, chance of upward movement (all `double` types)
	 */
	val particles = (0..<num).map {i ->
		val ured:Int = red+(2*randomizer.nextFloat()*variance-variance).toInt()
		val ugreen:Int = green+(2*randomizer.nextFloat()*variance-variance).toInt()
		val ublue:Int = blue+(2*randomizer.nextFloat()*variance-variance).toInt()
		val ualpha:Int = alpha+(2*randomizer.nextFloat()*variance-variance).toInt()
		val p = Vector(
			lerp(minX, maxX, randomizer.nextFloat()),
			lerp(startY-yVar, startY+yVar, randomizer.nextFloat())
		)
		val v = Vector(
			0f,
			lerp(0f, maxVel, randomizer.nextFloat())*if(randomizer.nextFloat()<upChance) -.5f else 1f
		)
		Particle(
			ParticleShape.Rect, lerp(DEF_MIN_LIFE, DEF_MAX_LIFE, randomizer.nextFloat()),
			p.x, p.y, v, Vector.zero(), .98f, 2, 6f,
			ured, ugreen, ublue, ualpha,
			ured*2/3, ugreen*2/3, ublue*2/3, 64
		)
	}
	val width = maxX-minX
	/** X-coordinate */
	override var x:Float = lerp(minX, maxX, .5f)
	/** Y-coordinate */
	override var y:Float  = startY


	private var ticks = 0
	/** @return true if it's expired */
	override fun update(r:AbstractRenderer):Boolean = ++ticks>=10

	private val cColor = Triple(
		lerp(red, 255, .75f)/255f, lerp(green, 255, .75f)/255f, lerp(blue, 255, .75f)/255f
	)
	private val cAlpha = lerp(alpha, 255, .5f)/255f
	/**
	 * Draw the particles to the current renderer.
	 * @param i
	 * @param r Renderer to use
	 */
	override fun draw(i:Int, r:AbstractRenderer) {
		((10-ticks)*width/7f).let {s ->
			if(s>0)
				r.drawBlendAdd {
					r.resources.imgFrags[1].draw(
						x-10-s/5f, y-s/3f, x+10+s/5f, y+s/3f, maxOf(0f, 1f-ticks*.1f)*cAlpha, cColor
					)
				}
		}
	}

	companion object {
		/** Default max velocity*/
		const val DEF_MAX_VEL = 1f
		/** Default min lifetime*/
		const val DEF_MIN_LIFE = 40
		/** Default max lifetime*/
		const val DEF_MAX_LIFE = 80
	}
}
