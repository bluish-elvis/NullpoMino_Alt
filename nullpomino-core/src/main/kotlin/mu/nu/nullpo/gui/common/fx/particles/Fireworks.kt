/*
 * This library class was created by 0xFC963F18DC21 / Shots243
 * It is part of an extension library for the game NullpoMino (copyright 2021)
 *
 * Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 * Herewith shall the term "Game Creator" be given to the original creator of NullpoMino.
 *
 * THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 *
 * Repository: https://github.com/Shots243/ModePile
 *
 * When using this library in a mode / library pack of your own, the following
 * conditions must be satisfied:
 *     - This license must remain visible at the top of the document, unmodified.
 *     - You are allowed to use this library for any modding purpose.
 *         - If this is the case, the Library Creator must be credited somewhere.
 *             - Source comments only are fine, but in a README is recommended.
 *     - Modification of this library is allowed, but only in the condition that a
 *       pull request is made to merge the changes to the repository.
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

package mu.nu.nullpo.gui.common.fx.particles

import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.fx.Effect
import mu.nu.nullpo.gui.common.libs.Vector
import zeroxfc.nullpo.custom.libs.Interpolation.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Add some number of fireworks.
 * `params` are min start X location, max start X location, min start Y location, max start Y location,
 * red, green, blue, alpha, max color variance (all `int` type),
 * max velocity (velocity is a `float`),
 * min lifetime, max lifetime (both `int`) in that order.
 *
 * @param num    Number of particles / particle groups.
 */
class Fireworks @JvmOverloads constructor(
	override var x:Float, override var y:Float, red:Int, green:Int, blue:Int, alpha:Int, variance:Int, gravity:Float = GRAVITY,
	friction:Float = FRICTION, minLifeTime:Int = DEF_MIN_LIFE, maxLifeTime:Int = DEF_MAX_LIFE, maxVelocity:Float = DEF_MAX_VEL,
	/** FireSparks + [num] * random 1x~2x */
	num:IntRange = 64..128,
	/**Randomizer*/
	randomizer:Random = Random.Default
):Effect {
	val particles = List((num.last-num.start).let {if(it>0) randomizer.nextInt(it+1) else 0}+num.start) {

		val ured:Int = red+(2*randomizer.nextFloat()*variance-variance).toInt()
		val ugreen:Int = green+(2*randomizer.nextFloat()*variance-variance).toInt()
		val ublue:Int = blue+(2*randomizer.nextFloat()*variance-variance).toInt()
		val ualpha:Int = alpha+(2*randomizer.nextFloat()*variance-variance).toInt()
		val s = 1+randomizer.nextInt(2)
		val v = Vector(maxVelocity*cos(randomizer.nextFloat()*PI.toFloat()), (randomizer.nextDouble(2*PI)).toFloat(), true)
		val particle = Particle(
			Particle.ParticleShape.ASprite,
			lerp(minLifeTime, maxLifeTime, randomizer.nextFloat()),
			x, y, v, Vector(0f, gravity), friction, s, 1f, ured, ugreen, ublue, ualpha,
			ured*2/3, ugreen*2/3, ublue*2/3, 0
		)
		val v2 = Vector(maxVelocity*sin(randomizer.nextFloat()*PI.toFloat()), (randomizer.nextDouble(2*PI)).toFloat(), true)
		val particle2 = Particle(
			Particle.ParticleShape.ASprite,
			lerp(minLifeTime, maxLifeTime, randomizer.nextFloat()),
			x, y, v2, Vector(0f, gravity), friction, 1, 1f,
			lerp(ured, 255, .9f), lerp(ugreen, 255, .9f), lerp(ublue, 255, .9f),
			lerp(ualpha, 255, .9f), ured*4/5, ugreen*4/5, ublue*4/5, 0
		)
		listOf(particle, particle2)
	}.flatten()

	private var ticks = 0
	private val cColor = Triple(
		lerp(red, 255, .75f)/255f, lerp(green, 255, .75f)/255f, lerp(blue, 255, .75f)/255f
	)
	private val cAlpha = lerp(alpha, 255, .5f)/255f
	override fun draw(i:Int, r:AbstractRenderer) {
		(100-ticks*10).let {s ->
			if(s>0)
				r.drawBlendAdd {
					r.resources.imgFrags[2].draw(
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

		fun colorBy(c:EventReceiver.COLOR):List<Int> = DEF_COLORS[when(c) {
			EventReceiver.COLOR.RED -> 1
			EventReceiver.COLOR.ORANGE -> 2
			EventReceiver.COLOR.YELLOW -> 3
			EventReceiver.COLOR.GREEN -> 4
			EventReceiver.COLOR.CYAN -> 5
			EventReceiver.COLOR.BLUE, EventReceiver.COLOR.COBALT -> 6
			EventReceiver.COLOR.PURPLE, EventReceiver.COLOR.PINK -> 7
			else -> 0
		}]

	}
}
