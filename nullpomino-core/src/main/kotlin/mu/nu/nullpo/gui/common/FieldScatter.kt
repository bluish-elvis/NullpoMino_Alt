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

package mu.nu.nullpo.gui.common

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import zeroxfc.nullpo.custom.libs.DoubleVector
import zeroxfc.nullpo.custom.libs.PhysicsObject
import java.util.Random
import kotlin.math.abs

class FieldScatter {
	/**
	 * Blocks to draw
	 */
	private val blocks:ArrayList<PhysicsObject>
	/**
	 * Lifetime
	 */
	private var lifeTime:Int
	/**
	 * Makes a new field explosion.
	 *
	 * @param receiver    Renderer to draw with.
	 * @param engine      Current GameEngine.
	 * @param playerID    Current player ID.
	 * @param clearBlocks Clear the blocks in the field?
	 */
	@JvmOverloads
	constructor(receiver:EventReceiver, engine:GameEngine, playerID:Int, clearBlocks:Boolean = true) {
		// Direction randomizer
		val rdm = Random(engine.randSeed+engine.statistics.time)
		lifeTime = 0
		val eBlock = Block(0, engine.skin)
		blocks = ArrayList()
		for(i in -1*engine.field.hiddenHeight until engine.field.height) {
			for(j in 0 until engine.field.width) {
				// Generate object array.
				val blk = engine.field.getBlock(j, i)
				if(blk!!.cint>0) {
					blocks.add(PhysicsObject(
						DoubleVector(receiver.fieldX(engine, playerID)+4+j*16, receiver.fieldY(engine, playerID)+52+i*16, false),
						DoubleVector(rdm.nextDouble()*8, rdm.nextDouble()*Math.PI*2, true),
						-1, 1, 1, PhysicsObject.ANCHOR_POINT_TL, blk.drawColor
					))
					if(clearBlocks) receiver.blockBreak(engine, j, i, blk)
					if(clearBlocks) blk.copy(eBlock)
				}
			}
		}
	}
	/**
	 * Makes a new field explosion. Destroys specfic blocks.
	 *
	 * @param receiver            Renderer to draw with.
	 * @param engine              Current GameEngine.
	 * @param playerID            Current player ID.
	 * @param fieldBlockLocations Blocck locations.
	 */
	constructor(receiver:EventReceiver, engine:GameEngine, playerID:Int, fieldBlockLocations:ArrayList<IntArray>) {
		/** Direction randomizer  */
		val rdm = Random(engine.randSeed+engine.statistics.time)
		lifeTime = 0
		val eBlock = Block(0, engine.skin)
		blocks = ArrayList()
		for(loc in fieldBlockLocations) {
			val j = loc[0]
			val i = loc[1]

			// Generate object array.
			val blk = engine.field.getBlock(j, i)
			if(blk!!.cint>0) {
				blocks.add(PhysicsObject(
					DoubleVector(
						receiver.fieldX(engine, playerID)+4+j*16,
						receiver.fieldY(engine, playerID)+52+i*16,
						false
					),
					DoubleVector(
						rdm.nextDouble()*8, rdm.nextDouble()*Math.PI*2, true
					),
					-1, 1, 1, PhysicsObject.ANCHOR_POINT_TL, blk.cint
				))
				receiver.blockBreak(engine, j, i, blk)
				blk.copy(eBlock)
			}
		}
	}
	/**
	 * Makes a new field explosion. Destroys specfic blocks.
	 *
	 * @param receiver            Renderer to draw with.
	 * @param engine              Current GameEngine.
	 * @param playerID            Current player ID.
	 * @param fieldBlockLocations Blocck locations.
	 */
	constructor(receiver:EventReceiver, engine:GameEngine, playerID:Int, fieldBlockLocations:Array<IntArray>):this(receiver,
		engine, playerID, listOf<IntArray>(*fieldBlockLocations) as ArrayList<IntArray>)
	/**
	 * Updates the life cycle of the explosion.
	 */
	fun update() {
		if(shouldNull()) return
		for(pho /* NOTE: Delicious */ in blocks) {
//			if ((pho.position.getX() <= -16 || pho.position.getX() > 640)) continue;
//			if ((pho.position.getY() > 460 && Math.abs(pho.velocity.getMagnitude()) < 0.0001)) continue;
			pho.move()
			if(pho.position.y>465) {
				PhysicsObject.reflectVelocityWithRestitution(pho.velocity, true, 0.75)
				while(pho.position.y>465) pho.move()
			}
			pho.velocity = DoubleVector.plus(pho.velocity, GRAVITY)
		}
		blocks.removeIf {block:PhysicsObject -> block.position.x<=-16||block.position.x>640}
		blocks.removeIf {block:PhysicsObject -> block.position.y>460&&abs(block.velocity.magnitude)<0.0001}
		lifeTime++
	}
	/**
	 * Draws the explosion.
	 *
	 * @param receiver Renderer to draw explosion with.
	 * @param engine   Current GameEngine.
	 * @param playerID Current playerID.
	 */
	fun draw(receiver:EventReceiver, engine:GameEngine, playerID:Int) {
		if(shouldNull()) return
		for(pho in blocks) {
//			if ((pho.position.getX() <= -16 || pho.position.getX() > 640)) continue;
//			if ((pho.position.getY() > 460 && Math.abs(pho.velocity.getMagnitude()) < 0.0001)) continue;
			pho.draw(receiver, engine, playerID)
		}
	}
	/**
	 * Checks if the object's lifetime has expired.
	 *
	 * @return Expiry status
	 */
	private fun shouldNull():Boolean = lifeTime>=600

	companion object {
		/**
		 * Gravity
		 */
		private val GRAVITY = DoubleVector(0.0, 1.0/6.0, false)
	}
}