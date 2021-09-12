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

import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.gui.slick.BufferedPrimitiveDrawingHook
import org.apache.log4j.Logger

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