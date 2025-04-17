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
import mu.nu.nullpo.gui.common.fx.SpriteSheet
import mu.nu.nullpo.gui.common.fx.particles.Particle.ParticleShape.*
import zeroxfc.nullpo.custom.libs.Interpolation.lerp
import zeroxfc.nullpo.custom.libs.Interpolation.smoothStep
import zeroxfc.nullpo.custom.libs.Vector

/**
Create an instance of a particle.

@param shape        The shape of the particle. Warning: SDL cannot draw circular particles.
@param maxLifetime  The maximum frame lifetime of the particle.
@param x     Horizontal position of the particle.
@param y     Vertical position of the particle.
@param vel     Vector velocity of the particle.
@param acc Vector acceleration of the particle.
@param friction Vector deceleration of the particle
@param size of the particle.
@param red component of color.
@param green component of color.
@param blue component of color.
@param alphaI        Alpha component of color.
@param redEnd       Red component of color at particle death.
@param greenEnd     Green component of color at particle death.
@param blueEnd      Blue component of color at particle death.
@param alphaEnd     Alpha component of color at particle death.
 */
open class Particle @JvmOverloads constructor(
	/** Particle shape*/
	val shape:ParticleShape?,
	/** Lifetime */
	protected val maxLifetime:Int,
	/** X-coordinate */
	x:Float,
	/** Y-coordinate */
	y:Float,
	/** Velocity vector */
	vel:Vector = Vector.zero(),
	/** Acceleration vector */
	var acc:Vector = Vector.zero(),
	/** Velocity decrease float */
	val friction:Float = 1f,
	/** X size */
	val size:Int,
	val sizeEase:Float = 6f,
	/** Red color component 0-255 */
	val red:Int = DEFAULT_COLOR,
	/*** Green color component 0-255 */
	val green:Int = DEFAULT_COLOR,
	/** Blue color component 0-255 */
	val blue:Int = DEFAULT_COLOR,
	/** Alpha component 0-255 */
	val alphaI:Int = DEFAULT_COLOR,
	/** Red color component at end 0-255 */
	val redEnd:Int = red,
	/** Green color component at end 0-255 */
	val greenEnd:Int = green,
	/** Blue color component at end 0-255 */
	val blueEnd:Int = blue,
	/** Alpha component at end 0-255 */
	val alphaEnd:Int = alphaI):SpriteSheet(x, y, vel) {
	/** Used colors 0-255*/
	var ur = red
		protected set
	/** Used colors 0-255*/
	var ug = green
		protected set
	/** Used colors 0-255*/
	var ub = blue
		protected set
	/** Used colors 0-255*/
	var ua = alphaI
		protected set
	/** used scale 0-1*/
	var us = size.toFloat()
		protected set
	/** Draw the particle.*/
	override fun draw(i:Int, r:AbstractRenderer) {
		fun line(dim:Boolean) = r.drawLineSpecific(
			x, y, x+vel.x*us, y+vel.y*us,
			if(dim)(ur*us).toInt()*0x10000+(ug*us).toInt()*0x100+(ub*us).toInt()
				else ur*0x10000+ug*0x100+ub, if(dim)ua*us/255f else ua/255f, 1f
		)
		fun rect() = r.drawRect(
			x-us/2, y-us/2, us, us,
			ur*0x10000+ug*0x100+ub, ua/255f, 0f
		)

		fun oval() = r.drawOval(
			x-us/2, y-us/2, us, us,
			ur*0x10000+ug*0x100+ub, ua/255f, 0f
		)

		fun sprite() {
			val sx = 16*us
			val sy = 16*us
			r.resources.imgFrags.let {
				it[2].draw(
					x-sx/2, y-sy/2, x+sx/2, y+sy/2,
					ua/255f, Triple(ur/255f, ug/255f, ub/255f)
				)
			}
		}
		when(shape) {
			Rect -> {rect();line(false)}
			ARect -> r.drawBlendAdd {line(false);rect()}
			Oval -> {oval();line(false)}
			AOval -> r.drawBlendAdd {line(true);oval()}
			ASprite -> r.drawBlendAdd {line(true);oval();sprite()}
			ASpark -> r.drawBlendAdd {line(false);oval();sprite()}
			else -> ticks=maxLifetime
		}
	}
	/**
	 * Update's the particle's position, color and lifetime.
	 * @return `true` if the particle needs to be destroyed, else `false`.
	 */
	override fun update(r:AbstractRenderer):Boolean {
		x += vel.x
		y += vel.y
		vel *= friction
		vel += acc
		ur = lerp(red, redEnd, ticks.toDouble()/maxLifetime)
		ug = lerp(green, greenEnd, ticks.toDouble()/maxLifetime)
		ub = lerp(blue, blueEnd, ticks.toDouble()/maxLifetime)
		ua = lerp(alphaI, alphaEnd, ticks.toDouble()/maxLifetime)
		if(sizeEase>=1)
			us = smoothStep(size.toFloat(), 0f, ticks.toFloat()/maxLifetime, sizeEase)

		return ++ticks>maxLifetime||ua<=0||
			x<-us/2&&vel.x<0||x>640+us/2&&vel.x>0||
			y<-us/2&&vel.y<0||y>480+us/2&&vel.y<0
	}
	/**
	 * Particle Shapes
	 * Warning: you cannot use circular & Light particles with SDL.
	 */
	enum class ParticleShape {
		Rect, Oval, ARect, AOval,
		ASprite, ASpark
	}
	enum class ParticleMovement {
		Frag
	}

	companion object {
		/** Default color*/
		internal const val DEFAULT_COLOR = 255
	}

}
