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

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.fx.Effect
import zeroxfc.nullpo.custom.libs.Vector
import zeroxfc.nullpo.custom.libs.Interpolation.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
Add some number of fireworks.
`params` are min start X location, max start X location, min start Y location, max start Y location,
red, green, blue, alpha, max color variance (all `int` type),
max velocity (velocity is a `float`),
min lifetime, max lifetime (both `int`) in that order.

@param num    Number of particles / particle groups.
 */
class Fireworks @JvmOverloads constructor(
	override var x:Float, override var y:Float, red:Int, green:Int, blue:Int, alpha:Int, variance:Int, gravity:Float = GRAVITY,
	friction:Float = FRICTION, minLifeTime:Int = DEF_MIN_LIFE, maxLifeTime:Int = DEF_MAX_LIFE, val maxVelocity:Float = DEF_MAX_VEL,
	/** FireSparks += random *[num]start~end */
	num:IntRange = 64..128,
	/**Randomizer*/
	rand:Random = Random.Default
):Effect {
	val particles = List(maxOf(1, (num.last-num.first).let {if(it>0) rand.nextInt(it+1) else 0}+num.first)) {
		val ured:Int = red+(2*rand.nextFloat()*variance-variance).toInt()
		val ugreen:Int = green+(2*rand.nextFloat()*variance-variance).toInt()
		val ublue:Int = blue+(2*rand.nextFloat()*variance-variance).toInt()
		val ualpha:Int = alpha+(2*rand.nextFloat()*variance-variance).toInt()
		val s = 1+rand.nextInt(2)
		val particle = Particle(
			Particle.ParticleShape.ASprite,
			lerp(minLifeTime, maxLifeTime, rand.nextFloat()),
			x, y, Vector(maxVelocity*cos(rand.nextFloat()*PI.toFloat()), (rand.nextDouble(2*PI)).toFloat(), true),
			Vector(0f, gravity), friction, s, 1f, ured, ugreen, ublue, ualpha,
			ured*2/3, ugreen*2/3, ublue*2/3, 0
		)
		if(num.first>1) {
			val particle2 = Particle(
				Particle.ParticleShape.ASprite,
				lerp(minLifeTime, maxLifeTime, rand.nextFloat()),
				x, y, Vector(maxVelocity*sin(rand.nextFloat()*PI.toFloat()), (rand.nextDouble(2*PI)).toFloat(), true),
				Vector(0f, gravity), friction, 1, 1f,
				lerp(ured, 255, .9f), lerp(ugreen, 255, .9f), lerp(ublue, 255, .9f),
				lerp(ualpha, 255, .9f), ured*4/5, ugreen*4/5, ublue*4/5, 0
			)
			listOf(particle, particle2)
		} else listOf(particle)
	}.flatten()

	private var ticks = 0
	private val cColor = Triple(
		lerp(red, 255, .75f)/255f, lerp(green, 255, .75f)/255f, lerp(blue, 255, .75f)/255f
	)
	private val cAlpha = lerp(alpha, 255, .5f)/255f
	override fun draw(i:Int, r:AbstractRenderer) {
		((10-ticks)*(8+maxVelocity)).let {s ->
			if(s>0)
				r.drawBlendAdd {
					r.resources.imgFrags[0].draw(
						x-s/2f, y-s/2f, x+s/2f, y+s/2f, maxOf(0f, 1f-ticks*.1f)*cAlpha, cColor
					)
				}
		}
	}

	override fun update(r:AbstractRenderer):Boolean {
		++ticks
		return ticks>=10
	}

	companion object {
		/** Default max velocity*/
		const val DEF_MAX_VEL = 5f
		/** Default min lifetime*/
		const val DEF_MIN_LIFE = 45
		/** Default max lifetime*/
		const val DEF_MAX_LIFE = 72
		private const val FRICTION = 0.95f
		private const val GRAVITY = FRICTION/60

		/**
		 * Default color set shared by all emitters.<br></br>
		 * In order: Gray, Red, Orange, Yellow, Green, Cyan, Blue, Purple<br></br>
		 * Parameters: Red, Green, Blue, Alpha, Variance
		 */
		val DEF_COLORS = listOf(
			listOf(240, 240, 240, 235, 20), listOf(240, 30, 0, 235, 20),
			listOf(240, 130, 0, 235, 20), listOf(240, 240, 0, 235, 20), listOf(30, 240, 0, 235, 20),
			listOf(0, 240, 240, 235, 20), listOf(0, 30, 240, 235, 20), listOf(210, 0, 210, 235, 20)
		)

		fun colorBy(c:COLOR):List<Int> = DEF_COLORS[when(c) {
			COLOR.RED -> 1
			COLOR.ORANGE -> 2
			COLOR.YELLOW -> 3
			COLOR.GREEN -> 4
			COLOR.CYAN -> 5
			COLOR.BLUE, COLOR.COBALT -> 6
			COLOR.PURPLE, COLOR.PINK -> 7
			else -> 0
		}]

	}
}
