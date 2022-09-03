/*
 * Copyright (c) 2022-2022,
 * This library class was created by 0xFC963F18DC21 / Shots243
 * It is part of an extension library for the game NullpoMino (copyright 2022-2022)
 *
 * Kotlin converted and modified by Venom=Nhelv
 *
 * Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 * Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.
 *
 * THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 *
 * Original Repository: https://github.com/Shots243/ModePile
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
import mu.nu.nullpo.gui.common.fx.Effect
import mu.nu.nullpo.gui.common.libs.PhysicsObject
import mu.nu.nullpo.gui.common.libs.Vector
import kotlin.math.abs

class BlockPhysics(position:Vector = Vector(0, 0), velocity:Vector = Vector(0, 0),
	collisionsToDestroy:Int = -1, val blockSizeX:Int = 1, val blockSizeY:Int = 1, anchorPoint:Int = 0, val block:Block = Block(1)):
	PhysicsObject(position, velocity, collisionsToDestroy, blockSizeX*BS, blockSizeY*BS, anchorPoint), Effect {
	override fun update(r:AbstractRenderer):Boolean {
		move()
		if(pos.y>465) {
			reflectVelocityWithRestitution(vel, true, .75f)
			while(pos.y>465) move()
		}
		vel += FieldScatter.GRAVITY
		return pos.x<=-BS/2||pos.x>640+BS/2||(pos.y>460&&abs(vel.magnitude)<0.0001)
	}

	override fun draw(i:Int, r:AbstractRenderer) {
		/**
		 * Draw instance blocks to engine.
		 *
		 * @param receiver Block renderer.
		 * @param engine   Current GameEngine.
		 * @param playerID Current player ID.
		 */
		for(y in 0 until blockSizeY) {
			for(x in 0 until blockSizeX) {
				r.drawBlock(minX+x*BS, minY+y*BS, block, 0f, 1f, 1f)
			}
		}
	}
}
