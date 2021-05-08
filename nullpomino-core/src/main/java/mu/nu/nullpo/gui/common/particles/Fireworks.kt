package mu.nu.nullpo.gui.common.particles

import zeroxfc.nullpo.custom.libs.DoubleVector
import zeroxfc.nullpo.custom.libs.Interpolation
import mu.nu.nullpo.gui.common.particles.Particle.ParticleShape
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