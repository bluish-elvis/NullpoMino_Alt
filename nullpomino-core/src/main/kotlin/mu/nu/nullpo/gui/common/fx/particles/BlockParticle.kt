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
import mu.nu.nullpo.game.event.EventReceiver.Companion.BS
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.libs.Vector
import mu.nu.nullpo.util.GeneralUtil.toInt
import kotlin.math.abs
import kotlin.random.Random

class BlockParticle(block:Block?, x:Float, y:Float, velocity:Vector, accelerate:Vector, val animType:Type,
	val blockSize:Int = 16, private val isFlashing:Boolean = false)
	:Particle(null, if(animType==Type.TGM) 100 else 250, x, y, velocity, accelerate, 0.980665f, blockSize) {
	// Block for use in texture.
	private val tex:Block = Block(block)

	override fun update(r:AbstractRenderer):Boolean {
		x += vel.x
		y += vel.y
		vel *= friction
		vel += acc
		return ++ticks>maxLifetime||ua<=0||
			x<-blockSize/2&&vel.x<0||x>640+blockSize/2&&vel.x>0||
			y<-blockSize/2&&vel.y<0||y>480+blockSize/2&&vel.y<0
	}

	override fun draw(i:Int, r:AbstractRenderer) {
		val size = if(animType==Type.TGM) 1+ticks/100f else 1f
		tex.alpha = when {
			animType==Type.TGM -> 1f
			ticks<20 -> 1-ticks*.02f
			ticks>maxLifetime-60 -> (maxLifetime-ticks)*.01f
			else -> .6f
		}
		tex.darkness = if(isFlashing&&ticks/2%2==0) -0.8f*tex.alpha else (1-tex.alpha)*-.24f

		if(animType==Type.TGM)
			r.drawBlock(x+blockSize/4, y+blockSize/4, tex, .6f, 1f, size*blockSize/BS)
		r.drawBlock(x, y, tex, 0f, 1f, size*blockSize/BS)
	}

	enum class Type { DTET, TGM }
	companion object {
		// Number of anim types
		val ANIMATION_TYPES = Type.entries.size
	}
	/**
	 * Creates a new collection of particles.
	 * [type] : Type.DTET | Type.TGM
	 */
	class Mapper(engine:GameEngine, receiver:EventReceiver, blocks:Map<Int, Map<Int, Block>>, type:Type,
		isFlashing:Boolean, maxVelocity:Float = 4f, rand:Random = Random.Default) {
		val particles:MutableSet<Particle> = blocks.flatMap {(y, row) ->
			val bs = engine.blockSize
			val width = engine.field.width
			val colA = blocks.keys.sorted()
			val rowA = row.keys.sorted()
			row.filterValues {!it.getAttribute(Block.ATTRIBUTE.BONE)}.map {(x, b) ->
				val cy = colA.let {(1+it.indexOf(y))*2-(it.size+1)}
					.let {if(it==0&&type==Type.DTET) 1-(rowA.indexOf(x)%2)*2f else it/2f}
				when(type) {
					Type.TGM -> {
						val xU = x-width/2f+((width%2==0).toInt()*.5f)
						val spd = abs(maxVelocity*.2f)
						BlockParticle(
							Block(b), receiver.fieldX(engine)+x*bs, receiver.fieldY(engine)+y*bs,
							Vector((1f/3f*xU)*1.35f*spd, (-5f+cy+abs(xU)/width)*1.5f*spd), Vector(0f, .72f*spd),
							Type.TGM, bs, isFlashing
						)
					}
					Type.DTET -> {
						BlockParticle(
							Block(b), receiver.fieldX(engine)+x*bs, receiver.fieldY(engine)+y*bs,
							Vector((x-width/2f)*5f/row.size, cy*2).apply {magnitude = maxVelocity*(.5f+rand.nextFloat()/2)},
							Vector((.04f+rand.nextFloat()*.21f)*(1-rand.nextInt(2)*2), rand.nextFloat()*.2f-.09f),
							type,
							bs, isFlashing
						)
					}
				}
			}
		}.toMutableSet()
	}
}
