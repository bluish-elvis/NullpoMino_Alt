package mu.nu.nullpo.gui.common.particles

import zeroxfc.nullpo.custom.libs.DoubleVector
import zeroxfc.nullpo.custom.libs.Interpolation
import mu.nu.nullpo.gui.common.particles.Particle.ParticleShape
import kotlin.random.Random

/**
 * Parameterless constructor. Uses time as the random seed.
 */
class LandingParticles:ParticleEmitterBase {
	/** Randomizer*/
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
	 * Adds a number of landing particles.<br></br>
	 * Parameters are min start x, max start x, start y, start y variance,
	 * red, green, blue, alpha, variance (all `int` types),
	 * maximum velocity, chance of upward movement (all `double` types)
	 *
	 * @param num    Number of particles
	 * @param params Parameters to pass onto the particles
	 */
	override fun addNumber(num:Int, params:Array<Any>) {
		val minX:Int
		val maxX:Int
		val startY:Int
		val yVar:Int
		val red:Int
		val green:Int
		val blue:Int
		val alpha:Int
		val variance:Int
		val maxVel:Double
		val upChance:Double
		try {
			minX = params[0] as Int
			maxX = params[1] as Int
			startY = params[2] as Int
			yVar = params[3] as Int
			red = params[4] as Int
			green = params[5] as Int
			blue = params[6] as Int
			alpha = params[7] as Int
			variance = params[8] as Int
			maxVel = params[9] as Double
			upChance = params[10] as Double
			for(i in 0 until num) {
				val ured:Int = red+(2*randomizer.nextDouble()*variance-variance).toInt()
				val ugreen:Int = green+(2*randomizer.nextDouble()*variance-variance).toInt()
				val ublue:Int = blue+(2*randomizer.nextDouble()*variance-variance).toInt()
				val ualpha:Int = alpha+(2*randomizer.nextDouble()*variance-variance).toInt()
				val p = DoubleVector(
					Interpolation.lerp(minX, maxX, randomizer.nextDouble()).toDouble(),
					Interpolation.lerp(startY-yVar, startY+yVar, randomizer.nextDouble()).toDouble(),
					false
				)
				val v = DoubleVector(
					0.0,
					Interpolation.lerp(0.0, maxVel, randomizer.nextDouble())*if(randomizer.nextDouble()<upChance) -0.5 else 1.0,
					false
				)
				val particle = Particle(ParticleShape.Rectangle,
					Interpolation.lerp(DEF_MIN_LIFE, DEF_MAX_LIFE, randomizer.nextDouble()), p,
					v,
					DoubleVector.zero(),
					2, 2,
					ured, ugreen, ublue, ualpha,
					(ured/1.5).toInt(), (ugreen/1.5).toInt(), (ublue/1.5).toInt(), 64
				)
				particles.add(particle)
			}
		} catch(ce:ClassCastException) {
			log.error("LandingParticles.addNumber: Invalid argument in params.", ce)
		} catch(e:Exception) {
			log.error("LandingParticles.addNumber: Other exception occurred.", e)
		}
	}

	companion object {
		/**
		 * Default max velocity
		 */
		const val DEF_MAX_VEL = 1.0
		/**
		 * Default min lifetime
		 */
		const val DEF_MIN_LIFE = 40
		/**
		 * Default max lifetime
		 */
		const val DEF_MAX_LIFE = 80
	}
}