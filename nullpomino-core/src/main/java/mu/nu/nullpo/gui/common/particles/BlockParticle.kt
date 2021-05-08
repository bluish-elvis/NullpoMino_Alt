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

class BlockParticle @JvmOverloads constructor(
	block:Block?,
	/** Position */
	private var position:DoubleVector,
	/** Velocity */
	private var velocity:DoubleVector,
	/** Lifetime */
	val maxLifetime:Int,
	// Flash
	private var isFlashing:Boolean = false
) {
	// Block for use in texture.
	private val objectTexture:Block = Block().apply {copy(block)}
	// Size
	private var size:Float = 1f
	// Current time alive
	var currentLifetime:Int = 0
		private set

	/**
	 * Creates a block particle.
	 *
	 * @param block       Block to import parameters from.
	 * @param position    Starting location.
	 * @param maxVelocity Highest absolute speed.
	 * @param timeToLive  Frames to live.
	 * @param randomizer  Randomizer used for setting speed and direction.
	 */
	constructor(block:Block?, position:DoubleVector, maxVelocity:Double, timeToLive:Int, flash:Boolean, randomizer:Random)
		:this(block, position, DoubleVector(randomizer.nextDouble()*maxVelocity, randomizer.nextDouble()*Math.PI*2, true),
		timeToLive, flash)

	/**
	 * Update position and lifetime data.
	 */
	fun update(animType:Int) {
		position = DoubleVector.plus(position, velocity)
		when(animType) {
			BlockParticleCollection.ANIMATION_DTET -> if(currentLifetime<maxLifetime*0.75) {
				velocity.direction = velocity.direction+Math.PI/maxLifetime
			}
			BlockParticleCollection.ANIMATION_TGM -> {
				velocity = DoubleVector.plus(velocity, DoubleVector(0.0, 0.980665/2.25, false))
				size += 1f/60f
			}
		}
		currentLifetime++
	}
	/**
	 * Draw the block particle.
	 *
	 * @param receiver EventReceiver doing the drawing.
	 */
	fun draw(engine:GameEngine, receiver:EventReceiver?, playerID:Int, animType:Int) {
		if(engine.displaysize!=-1) {
			if(animType==BlockParticleCollection.ANIMATION_TGM) {
				receiver?.drawBlock(position.x
					.toInt()+((if(engine.displaysize==0) 2 else 4)*size).toInt(), position.y.toInt()+if(engine.displaysize==0) 2 else 4,
					objectTexture.drawColor, objectTexture.skin,
					objectTexture.getAttribute(Block.ATTRIBUTE.BONE), 0.5f,
					1f, (if(engine.displaysize==0) 1f else 2f)*size)
				//				receiver.drawSingleBlock(engine, playerID,
//		                (int)position.getX() + ((engine.displaysize == 0) ? 2 : 4), (int)position.getY() + ((engine.displaysize == 0) ? 2 : 4),
//		                objectTexture.color, objectTexture.skin,
//		                objectTexture.getAttribute(Block.ATTRIBUTE.BONE), 0.5f, 1f,
//		                (engine.displaysize == 0) ? 1f : 2f);
			}
			receiver?.drawBlock(position.x.toInt(), position.y.toInt(), objectTexture.drawColor, objectTexture.skin,
				objectTexture.getAttribute(Block.ATTRIBUTE.BONE), if(isFlashing&&currentLifetime/2%2==0) -0.8f else 0f,
				if(animType==BlockParticleCollection.ANIMATION_DTET) 0.667f else 1f, (if(engine.displaysize==0) 1f else 2f)*size)
			//			receiver.drawSingleBlock(engine, playerID,
//	                (int)position.getX(), (int)position.getY(),
//	                objectTexture.color, objectTexture.skin,
//	                objectTexture.getAttribute(Block.ATTRIBUTE.BONE), (isFlashing && ((currentLifetime / 2) % 2 == 0)) ? -0.8f : 0f, (animType == BlockParticleCollection.ANIMATION_DTET) ? 0.667f : 1f,
//	                (engine.displaysize == 0) ? 1f : 2f);
		}
	}
}