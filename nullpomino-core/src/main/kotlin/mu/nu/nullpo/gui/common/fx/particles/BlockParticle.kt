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
import mu.nu.nullpo.game.event.EventReceiver.Companion.BS
import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.libs.Vector

class BlockParticle @JvmOverloads constructor(
	block:Block?, position:Vector, velocity:Vector, accelerate:Vector = Vector(0f, 0.980665f/2.25f), val animType:Type,
	val blockSize:Int = 16, private val isFlashing:Boolean = false)
	:Particle(null, if(animType==Type.TGM) 100 else 250, position, velocity, accelerate, 0.980665f, blockSize, blockSize) {
	// Block for use in texture.
	private val objectTexture:Block = Block(block)

	override fun update(r:AbstractRenderer):Boolean {
		pos += vel
		vel *= friction
		vel += acc
		return ++ticks>maxLifetime||ua<=0||
			pos.x<-blockSize/2&&vel.x<0||pos.x>640+blockSize/2&&vel.x>0||
			pos.y<-blockSize/2&&vel.y<0||pos.y>480+blockSize/2&&vel.y<0
	}

	override fun draw(i:Int, r:AbstractRenderer) {
		val size = if(animType==Type.TGM) 1+ticks/100f else 1f
		if(animType==Type.TGM)
			r.drawBlock(
				pos.x+blockSize/4, pos.y+blockSize/4, objectTexture,
				if(isFlashing&&ticks/2%2==0) 0f else 0.5f,
				1f, size*blockSize/BS
			)
		r.drawBlock(
			pos.x, pos.y, objectTexture, if(isFlashing&&ticks/2%2==0) -0.8f else 0f,
			if(animType==Type.TGM) 2f/3 else 1f, size*blockSize/BS
		)
	}

	companion object {
		enum class Type {
			DTET, TGM
		}
	}
}
