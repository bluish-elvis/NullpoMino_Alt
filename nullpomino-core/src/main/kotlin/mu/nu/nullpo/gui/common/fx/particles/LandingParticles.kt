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

import mu.nu.nullpo.gui.common.fx.particles.Particle.ParticleShape
import mu.nu.nullpo.gui.common.libs.Vector
import zeroxfc.nullpo.custom.libs.Interpolation
import kotlin.random.Random

class LandingParticles(
	num:Int,
	minX:Int,
	maxX:Int,
	startY:Int,
	yVar:Int,
	red:Int,
	green:Int,
	blue:Int,
	alpha:Int,
	variance:Int,
	maxVel:Float,
	upChance:Float,
	/** Randomizer*/
	randomizer:Random = Random.Default
):ParticleEmitterBase() {
	/**
	 * Adds a number of landing particles.<br></br>
	 * Parameters are min start x, max start x, start y, start y variance,
	 * red, green, blue, alpha, variance (all `int` types),
	 * maximum velocity, chance of upward movement (all `double` types)
	 *
	 * @param num    Number of particles
	 * @param params Parameters to pass onto the particles
	 */
	override val particles:MutableSet<Particle> = (0 until num).map {i ->
		val ured:Int = red+(2*randomizer.nextFloat()*variance-variance).toInt()
		val ugreen:Int = green+(2*randomizer.nextFloat()*variance-variance).toInt()
		val ublue:Int = blue+(2*randomizer.nextFloat()*variance-variance).toInt()
		val ualpha:Int = alpha+(2*randomizer.nextFloat()*variance-variance).toInt()
		val p = Vector(
			Interpolation.lerp(minX, maxX, randomizer.nextFloat()),
			Interpolation.lerp(startY-yVar, startY+yVar, randomizer.nextFloat())
		)
		val v = Vector(
			0f,
			Interpolation.lerp(0f, maxVel, randomizer.nextFloat())*if(randomizer.nextFloat()<upChance) -.5f else 1f
		)
		Particle(
			ParticleShape.Rect, Interpolation.lerp(DEF_MIN_LIFE, DEF_MAX_LIFE, randomizer.nextFloat()),
			p, v, Vector.zero(), .98f, 2, 2, 6f,
			ured, ugreen, ublue, ualpha,
			ured*3/2, ugreen*3/2, ublue*3/2, 64
		)
	}.toMutableSet()

	companion object {
		/** Default max velocity*/
		const val DEF_MAX_VEL = 1f
		/** Default min lifetime*/
		const val DEF_MIN_LIFE = 40
		/** Default max lifetime*/
		const val DEF_MAX_LIFE = 80
	}
}
