/*
 Copyright (c) 2019-2024,
 This library class was created by 0xFC963F18DC21 / Shots243
 It is part of an extension library for the game NullpoMino (copyright 2010-2024)

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis

 Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.

 THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 THIS KOTLIN VERSION WAS NOT MADE IN ASSOCIATION WITH THE LIBRARY CREATOR.

 Original Repository: https://github.com/Shots243/ModePile

 When using this library in a mode / library pack of your own, the following
 conditions must be satisfied:
     - This license must remain visible at the top of the document, unmodified.
     - You are allowed to use this library for any modding purpose.
         - If this is the case, the Library Creator must be credited somewhere.
             - Source comments only are fine, but in a README is recommended.
     - Modification of this library is allowed, but only in the condition that a
       pull request is made to merge the changes to the repository.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

package mu.nu.nullpo.gui.common.fx.particles

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.event.EventReceiver.Companion.BS
import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.fx.Effect
import zeroxfc.nullpo.custom.libs.PhysicsObject
import zeroxfc.nullpo.custom.libs.Vector
import zeroxfc.nullpo.custom.libs.AnchorPoint
import kotlin.math.abs

class BlockPhysics(x:Float = 0f, y:Float = 0f, velocity:Vector = Vector(0, 0),
	collisionsToDestroy:Int = -1, val blockSizeX:Int = 1, val blockSizeY:Int = 1, anchorPoint:AnchorPoint = AnchorPoint.TL,
	val block:Block = Block(1)):
	PhysicsObject(x, y, velocity, collisionsToDestroy, blockSizeX*BS, blockSizeY*BS, anchorPoint), Effect {
	override fun update(r:AbstractRenderer):Boolean {
		move()
		if(y>465) {
			reflectVelocityWithRestitution(vel, true, .75f)
			while(y>465) move()
		}
		vel += FieldScatter.GRAVITY
		return x<=-BS/2||x>640+BS/2||(y>460&&abs(vel.magnitude)<0.0001)
	}

	override fun draw(i:Int, r:AbstractRenderer) {
		for(y in 0..<blockSizeY) {
			for(x in 0..<blockSizeX) {
				r.drawBlock(minX+x*BS, minY+y*BS, block)
			}
		}
	}
}
