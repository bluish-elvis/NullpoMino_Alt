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
package mu.nu.nullpo.gui.common.fx.particles

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.fx.particles.BlockParticle.Companion.Type
import mu.nu.nullpo.gui.common.libs.Vector
import kotlin.math.PI
import kotlin.math.abs
import kotlin.random.Random

/**
 * Creates a new collection of particles.
 *
 * @param length Maximum particle count.
 */
class BlockParticleCollection:ParticleEmitterBase {
	override val particles:MutableSet<Particle>
	/**DTET scattering to horizontal */
	constructor(engine:GameEngine, receiver:EventReceiver, blocks:Map<Int, Map<Int, Block>>,
		maxVelocity:Float, isFlashing:Boolean, randomizer:Random) {
		particles = blocks.flatMap {(y, row) ->
			val colA = blocks.keys.sorted()
			val rowA = row.keys.sorted()
			row.map {(x, b) ->
				val bs = EventReceiver.getBlockSize(engine)
				val py = colA.let {a -> a.indexOf(y)-a.size/2}.let {if(it==0) 2*rowA.indexOf(x)%2-1 else it}

				BlockParticle(
					Block(b), Vector(receiver.fieldX(engine)/*+(bs/4)*/+x*bs, receiver.fieldY(engine)/*+(bs*13/4)*/+y*bs),
					Vector(randomizer.nextFloat()*2-1f, py.toFloat()).apply {magnitude = maxVelocity*(.5f+randomizer.nextFloat()/2)},
					Vector(randomizer.nextFloat()*maxVelocity, randomizer.nextDouble(PI*2).toFloat(), true),
					Type.DTET,
					bs, isFlashing
				)
			}
		}.toMutableSet()
	}
	/**TGM falling */
	constructor(
		engine:GameEngine, receiver:EventReceiver, blocks:Map<Int, Map<Int, Block>>, velY:Float = 4.8f, velYMod:Float, isFlashing:Boolean = false,
	) {
		val colA = blocks.keys.sorted()
		particles = blocks.flatMap {(y, row) ->
			row.map {(x, b) ->
				val bs = EventReceiver.getBlockSize(engine)
				val width = engine.field.width
				val py = colA.indexOf(y)
				var xU = x-width/2f
				if(width%2==0) xU += .5f
				val mod = 1f/3f*xU
				val velocity = Vector(mod*1.1f, -abs(velY)*1.5f-abs(velYMod)*py.toFloat()+abs(xU)*velY/width)
				BlockParticle(
					Block(b), Vector(receiver.fieldX(engine)+x*bs, receiver.fieldY(engine)+y*bs), velocity,
					animType = Type.TGM, blockSize = bs, isFlashing = isFlashing
				)
			}
		}.toMutableSet()
	}

	companion object {
		// Number of anim types
		val ANIMATION_TYPES = Type.values().size
	}
}
