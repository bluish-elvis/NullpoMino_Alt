package mu.nu.nullpo.gui.common.particles

import mu.nu.nullpo.gui.slick.BufferedPrimitiveDrawingHook
import zeroxfc.nullpo.custom.libs.*

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