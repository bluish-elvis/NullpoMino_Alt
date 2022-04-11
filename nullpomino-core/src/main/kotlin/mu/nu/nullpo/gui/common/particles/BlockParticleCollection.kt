/*
 * This library class was created by 0xFC963F18DC21 / Shots243
 * It is part of an extension library for the game NullpoMino (copyright 2010)
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
package mu.nu.nullpo.gui.common.particles

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import zeroxfc.nullpo.custom.libs.DoubleVector
import kotlin.random.Random

/**
 * Creates a new collection of particles.
 *
 * @param length Maximum particle count.
 */
class BlockParticleCollection(length:Int, animType:Int) {
	// Block particles in collection
	private val collectionBlockParticles:Array<BlockParticle?> = arrayOfNulls(length)
	// Animation type
	private val animationType:Int = animType
	fun update() {
		collectionBlockParticles.filterNotNull()
			.forEachIndexed {id, it ->
				it.update(animationType)
				if(it.currentLifetime>=it.maxLifetime) collectionBlockParticles[id] = null
			}
	}

	fun drawAll(engine:GameEngine, receiver:EventReceiver?, playerID:Int) {
		for(collectionBlockParticle in collectionBlockParticles) {
			collectionBlockParticle?.draw(engine, receiver, playerID, animationType)
		}
	}

	fun addBlock(engine:GameEngine, receiver:EventReceiver, playerID:Int, block:Block?, x:Int, y:Int, maxVelocity:Double,
		timeToLive:Int, isFlashing:Boolean, randomizer:Random) {
		var timeToLive = timeToLive
		var i = 0
		try {
			while(collectionBlockParticles[i]!=null) i++
		} catch(e:IndexOutOfBoundsException) {
			return  // Do not add block if full.
		}
		val k = Block()
		k.copy(block)
		val v1:Int = receiver.fieldX(engine,
			playerID)+(if(engine.displaysize==0) 4 else 8)+x*if(engine.displaysize==0) 16 else 32
		val v2:Int = receiver.fieldY(engine,
			playerID)+(if(engine.displaysize==0) 52 else 104)+y*if(engine.displaysize==0) 16 else 32
		val position = DoubleVector(v1.toDouble(), v2.toDouble(), false)
		timeToLive += randomizer.nextInt(5)-2
		collectionBlockParticles[i] = BlockParticle(k, position, maxVelocity, timeToLive, isFlashing, randomizer)
	}

	fun addBlock(engine:GameEngine, receiver:EventReceiver, playerID:Int, block:Block?, x:Int, y:Int, maxX:Int, yMod:Int,
		maxYMod:Int, timeToLive:Int) {
		var i = 0
		try {
			while(collectionBlockParticles[i]!=null) i++
		} catch(e:IndexOutOfBoundsException) {
			return  // Do not add block if full.
		}
		val k = Block()
		k.copy(block)
		val v1:Int = receiver.fieldX(engine,
			playerID)+(if(engine.displaysize==0) 4 else 8)+x*if(engine.displaysize==0) 16 else 32
		val v2:Int = receiver.fieldY(engine,
			playerID)+(if(engine.displaysize==0) 52 else 104)+y*if(engine.displaysize==0) 16 else 32
		val position = DoubleVector(v1.toDouble(), v2.toDouble(), false)
		var xU = x-maxX/2.0
		if(maxX%2==0) xU += 0.5
		val mod = 1.0/3.0*xU
		val velocity = DoubleVector(mod*1.1, -4.8*(0.5+0.5*((maxYMod-yMod).toDouble()/maxYMod)), false)
		collectionBlockParticles[i] = BlockParticle(k, position, velocity, timeToLive)
	}

	val count:Int
		get() {
			var g = 0
			for(collectionBlockParticle in collectionBlockParticles) {
				if(collectionBlockParticle!=null) g++
			}
			return g
		}

	companion object {
		// Animation types:
		const val ANIMATION_DTET = 0
		const val ANIMATION_TGM = 1
		// Number of anim types
		const val ANIMATION_TYPES = 2
	}
}