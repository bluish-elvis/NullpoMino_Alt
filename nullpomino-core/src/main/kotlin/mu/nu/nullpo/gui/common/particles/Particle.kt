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

import mu.nu.nullpo.gui.slick.BufferedPrimitiveDrawingHook
import zeroxfc.nullpo.custom.libs.DoubleVector
import zeroxfc.nullpo.custom.libs.Interpolation

/**
 * Create an instance of a particle.
 *
 * @param shape        The shape of the particle. Warning: SDL cannot draw circular particles.
 * @param particleMaxLifetime  The maximum frame lifetime of the particle.
 * @param position     Vector position of the particle.
 * @param velocity     Vector velocity of the particle.
 * @param acceleration Vector acceleration of the particle.
 * @param sizeX        Horizontal size of the particle.
 * @param sizeY        Vertical size of the particle.
 * @param red          Red component of color.
 * @param green        Green component of color.
 * @param blue         Blue component of color.
 * @param alpha        Alpha component of color.
 * @param redEnd       Red component of color at particle death.
 * @param greenEnd     Green component of color at particle death.
 * @param blueEnd      Blue component of color at particle death.
 * @param alphaEnd     Alpha component of color at particle death.
 */
class Particle @JvmOverloads constructor(
	/** Particle shape*/
	private val shape:ParticleShape?,
	/** Lifetime */
	private val particleMaxLifetime:Int,
	/** Position vector */
	var position:DoubleVector,
	/** Velocity vector */
	var velocity:DoubleVector,
	/** Acceleration vector */
	private var acceleration:DoubleVector,
	/** X size */
	val sizeX:Int,
	/** Y size */
	val sizeY:Int,
	/** Red color component */
	val red:Int = DEFAULT_COLOR,
	/*** Green color component */
	val green:Int = DEFAULT_COLOR,
	/** Blue color component */
	val blue:Int = DEFAULT_COLOR,
	/** Alpha component */
	val alpha:Int = DEFAULT_COLOR,
	/** Red color component at end */
	val redEnd:Int = DEFAULT_COLOR,
	/** Green color component at end */
	val greenEnd:Int = DEFAULT_COLOR,
	/** Blue color component at end */
	val blueEnd:Int = DEFAULT_COLOR,
	/** Alpha component at end */
	val alphaEnd:Int = DEFAULT_COLOR) {

	/*
     * Color variables.
     * Please use <code>0 <= value <= 255</code>
     */
	/**
	 * Used colors
	 */
	private var ur = 0
	private var ug = 0
	private var ub = 0
	private var ua = 0
	/**
	 * Current life
	 */
	private var particleLifetime = 0
	/**
	 * Initialise a stationary particle.
	 *
	 * @param shape       The shape of the particle. Warning: SDL cannot draw circular particles.
	 * @param maxLifeTime The maximum frame lifetime of the particle.
	 * @param sizeX       Horizontal size of the particle.
	 * @param sizeY       Vertical size of the particle.
	 * @param red         Red component of color.
	 * @param green       Green component of color.
	 * @param blue        Blue component of color.
	 * @param alpha       Alpha component of color.
	 * @param redEnd      Red component of color at particle death.
	 * @param greenEnd    Green component of color at particle death.
	 * @param blueEnd     Blue component of color at particle death.
	 * @param alphaEnd    Alpha component of color at particle death.
	 */
	constructor(shape:ParticleShape?, maxLifeTime:Int, sizeX:Int, sizeY:Int,
		red:Int, green:Int, blue:Int, alpha:Int,
		redEnd:Int, greenEnd:Int, blueEnd:Int, alphaEnd:Int):this(shape, maxLifeTime, DoubleVector.zero(),
		DoubleVector.zero(), DoubleVector.zero(), sizeX, sizeY,
		red, green, blue, alpha, redEnd, greenEnd, blueEnd, alphaEnd)
	/**
	 * Initialise a default color, stationary particle.
	 *
	 * @param shape       The shape of the particle. Warning: SDL cannot draw circular particles.
	 * @param maxLifeTime The maximum frame lifetime of the particle.
	 * @param sizeX       Horizontal size of the particle.
	 * @param sizeY       Vertical size of the particle.
	 */
	constructor(shape:ParticleShape?, maxLifeTime:Int, sizeX:Int, sizeY:Int):this(shape, maxLifeTime,
		DoubleVector.zero(), DoubleVector.zero(), DoubleVector.zero(), sizeX, sizeY)
	/**
	 * Draw the particle.
	 *
	 * @param buffer Drawing buffer to use
	 */
	fun draw(buffer:BufferedPrimitiveDrawingHook) {
		if(particleLifetime>particleMaxLifetime) return
		when(shape) {
			ParticleShape.Rectangle -> buffer.drawRectangle(position.x.toInt()-sizeX/2, position.y.toInt()-sizeY/2, sizeX, sizeY,
				ur, ug, ub, ua, true)
			ParticleShape.Circle -> buffer.drawOval(position.x.toInt()-sizeX/2, position.y.toInt()-sizeY/2, sizeX, sizeY, ur, ug,
				ub, ua, true)
			else -> {
			}
		}
	}
	/**
	 * Update's the particle's position, color and lifetime.
	 *
	 * @return `true` if the particle needs to be destroyed, else `false`.
	 */
	fun update():Boolean {
		velocity = DoubleVector.plus(velocity, acceleration)
		position = DoubleVector.plus(position, velocity)
		ur = Interpolation.lerp(red, redEnd, particleLifetime.toDouble()/particleMaxLifetime)
		ug = Interpolation.lerp(green, greenEnd, particleLifetime.toDouble()/particleMaxLifetime)
		ub = Interpolation.lerp(blue, blueEnd, particleLifetime.toDouble()/particleMaxLifetime)
		ua = Interpolation.lerp(alpha, alphaEnd, particleLifetime.toDouble()/particleMaxLifetime)
		return ++particleLifetime>particleMaxLifetime
	}
	/**
	 * Particle Shapes
	 * Warning: you cannot use circular particles with SDL.
	 */
	enum class ParticleShape {
		Rectangle, Circle
	}

	companion object {
		/**
		 * Default color
		 */
		private const val DEFAULT_COLOR = 255
	}

}