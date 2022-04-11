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

/*
 * Copyright (c) 2010-2021, NullNoname
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

package mu.nu.nullpo.gui.common.particles

import mu.nu.nullpo.gui.common.particles.Particle.ParticleShape
import zeroxfc.nullpo.custom.libs.DoubleVector
import zeroxfc.nullpo.custom.libs.Interpolation
import kotlin.random.Random

/**
 * Parameterless constructor. Uses time as the random seed.
 */
class Fireworks:ParticleEmitterBase {
	/**Randomizer*/
	private val randomizer:Random
	/**
	 * Constructor that uses a fixed random seed.
	 *
	 * @param seed Random seed
	 */
	constructor(seed:Long) {
		randomizer = Random(seed)
	}
	/**
	 * Constructor that uses a fixed random object.
	 *
	 * @param random Random instance
	 */
	@JvmOverloads
	constructor(random:Random = Random.Default) {
		randomizer = random
	}
	/**
	 * Add some number of fireworks.
	 * `params` are min start X location, max start X location, min start Y location, max start Y location,
	 * red, green, blue, alpha, max color variance (all `int` type),
	 * max velocity (velocity is a `double`),
	 * min lifetime, max lifetime (both `int`) in that order.
	 *
	 * @param num    Number of particles / particle groups.
	 * @param params Parameters to pass onto the particles.
	 */
	override fun addNumber(num:Int, params:Array<Any>) {
		val minX:Int
		val maxX:Int
		val minY:Int
		val maxY:Int
		val red:Int
		val green:Int
		val blue:Int
		val alpha:Int
		val variance:Int
		val minLifeTime:Int
		val maxLifeTime:Int
		val maxVelocity:Double
		try {
			minX = params[0] as Int
			maxX = params[1] as Int
			minY = params[2] as Int
			maxY = params[3] as Int
			red = params[4] as Int
			green = params[5] as Int
			blue = params[6] as Int
			alpha = params[7] as Int
			variance = params[8] as Int
			maxVelocity = params[9] as Double
			minLifeTime = params[10] as Int
			maxLifeTime = params[11] as Int
			for(i in 0 until num) {
				val origin = DoubleVector(Interpolation.lerp(minX, maxX, randomizer.nextDouble()).toDouble(),
					Interpolation.lerp(minY, maxY, randomizer.nextDouble())
						.toDouble(), false)
				for(j in 0 until randomizer.nextInt(121)+120) {
					val ured:Int = red+(2*randomizer.nextDouble()*variance-variance).toInt()
					val ugreen:Int = green+(2*randomizer.nextDouble()*variance-variance).toInt()
					val ublue:Int = blue+(2*randomizer.nextDouble()*variance-variance).toInt()
					val ualpha:Int = alpha+(2*randomizer.nextDouble()*variance-variance).toInt()
					val s = 1+randomizer.nextInt(3)
					val v = DoubleVector(2*randomizer.nextDouble()*maxVelocity-maxVelocity, 2*randomizer.nextDouble()*Math.PI, true)
					val particle = Particle(
						ParticleShape.Rectangle,
						Interpolation.lerp(minLifeTime, maxLifeTime, randomizer.nextDouble()),
						origin,
						v,
						DoubleVector(0.0, GRAVITY, false),
						s, s,
						ured, ugreen, ublue, ualpha,
						(ured/1.5).toInt(), (ugreen/1.5).toInt(), (ublue/1.5).toInt(), 64
					)
					particles.add(particle)
					val particle2 = Particle(
						ParticleShape.Rectangle,
						Interpolation.lerp(minLifeTime, maxLifeTime, randomizer.nextDouble()),
						origin,
						v,
						DoubleVector(0.0, GRAVITY, false),
						1, 1,
						Interpolation.lerp(ured, 255, 0.9),
						Interpolation.lerp(ugreen, 255, 0.9),
						Interpolation.lerp(ublue, 255, 0.9),
						Interpolation.lerp(ualpha, 255, 0.9),
						(ured/1.25).toInt(), (ugreen/1.25).toInt(), (ublue/1.25).toInt(), 64
					)
					particles.add(particle2)
				}
			}
		} catch(ce:ClassCastException) {
			log.error("Fireworks.addNumber: Invalid argument in params.", ce)
		} catch(e:Exception) {
			log.error("Fireworks.addNumber: Other exception occurred.", e)
		}
	}

	companion object {
		/**
		 * Default max velocity
		 */
		const val DEF_MAX_VEL = 3.2
		/**
		 * Default min lifetime
		 */
		const val DEF_MIN_LIFE = 60
		/**
		 * Default max lifetime
		 */
		const val DEF_MAX_LIFE = 120
		/**
		 * Gravity
		 */
		private const val GRAVITY = 2.4/30.0
	}
}