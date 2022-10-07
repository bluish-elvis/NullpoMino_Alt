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

package mu.nu.nullpo.gui.common.fx.particles

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.libs.Vector
import zeroxfc.nullpo.custom.libs.AnchorPoint
import java.util.Random
import kotlin.math.PI

/**
 * Makes a new field explosion.
 *
 * @param engine      Current GameEngine.
 * @param fieldBlockLocations Block locations.
 * @param clearBlocks Clear the blocks in the field?
 */
class FieldScatter @JvmOverloads constructor(engine:GameEngine, fieldBlockLocations:Iterable<IntArray>? = null, clearBlocks:Boolean) {
	/** Blocks to draw*/
	private val blocks:MutableSet<BlockPhysics> = mutableSetOf()

	init {
		// Direction randomizer
		val rdm = Random(engine.randSeed+engine.statistics.time)
		val receiver = engine.owner.receiver
		fun blkAdd(blk:Block, x:Int, y:Int) {
			if(blk.cint>0) {
				blocks.add(
					BlockPhysics(
						receiver.fieldX(engine)+4+x*16f, receiver.fieldY(engine)+52+y*16f,
						Vector(rdm.nextFloat(8f), rdm.nextFloat((PI*2).toFloat()), true),
						-1, 1, 1, AnchorPoint.TL, blk
					)
				)
				if(clearBlocks) {
					receiver.blockBreak(engine, mapOf(y to mapOf(x to blk)))
					engine.field.delBlock(x, y)
				}
			}
		}
		fieldBlockLocations?.let {
			it.forEach {
				engine.field.getBlock(it[0], it[1])?.let {b -> blkAdd(b, it[0], it[1])}
			}
		} ?: (-1*engine.field.hiddenHeight until engine.field.height).forEach {i ->
			for(j in 0 until engine.field.width)
				engine.field.getBlock(j, i)?.let {b -> blkAdd(b, j, i)}
		}
	}

	companion object {
		/**
		 * Gravity
		 */
		internal val GRAVITY = Vector(0f, 1/6f)
	}
}
