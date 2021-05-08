package mu.nu.nullpo.gui.common.particles

import mu.nu.nullpo.game.event.EventReceiver
import org.apache.log4j.Logger
import mu.nu.nullpo.gui.slick.BufferedPrimitiveDrawingHook

abstract class ParticleEmitterBase {
	/**
	 * Particle container
	 */
	protected var particles = ArrayList<Particle>()
	/**
	 * Drawing buffer
	 */
	private var drawingQueue = BufferedPrimitiveDrawingHook()
	/**
	 * Update method. Used to update all partcles.
	 */
	fun update() {
		if(particles.size<=0) return
		for(i in particles.indices.reversed()) {
			val res = particles[i].update()
			if(res) {
				particles.removeAt(i)
			}
		}
	}
	/**
	 * Draw the particles to the current renderer.
	 *
	 * @param receiver Renderer to use
	 */
	fun draw(receiver:EventReceiver?) {
		if(particles.size<=0) return
		for(p in particles) {
			if(p.position.x<0||p.position.x>640) continue
			if(p.position.y<0||p.position.y>480) continue
			p.draw(drawingQueue)
		}
		drawingQueue.renderAll(receiver)
	}
	/**
	 * Add particles directly to the collection.
	 *
	 * @param particle Particle to add
	 */
	fun addSpecific(particle:Particle) {
		particles.add(particle)
	}
	/**
	 * Add some number of particles or particle groups.
	 * Varies upon child class.
	 *
	 * @param num    Number of particles / particle groups.
	 * @param params Parameters to pass onto the particles.
	 */
	abstract fun addNumber(num:Int, params:Array<Any>)

	companion object {
		/**
		 * Default color set shared by all emitters.<br></br>
		 * In order: Gray, Red, Orange, Yellow, Green, Cyan, Blue, Purple<br></br>
		 * Parameters: Red, Green, Blue, Alpha, Variance
		 */
		val DEF_COLORS = arrayOf(intArrayOf(240, 240, 240, 235, 20), intArrayOf(240, 30, 0, 235, 20),
			intArrayOf(240, 130, 0, 235, 20), intArrayOf(240, 240, 0, 235, 20), intArrayOf(30, 240, 0, 235, 20),
			intArrayOf(0, 240, 240, 235, 20), intArrayOf(0, 30, 240, 235, 20), intArrayOf(210, 0, 210, 235, 20))
		/**
		 * Debug logger
		 */
		internal val log:Logger = Logger.getLogger(ParticleEmitterBase::class.java)
	}
}