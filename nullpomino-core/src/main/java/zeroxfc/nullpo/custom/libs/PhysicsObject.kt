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
package zeroxfc.nullpo.custom.libs

import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import org.apache.log4j.Logger

/**
 * PhysicsObject
 *
 *
 * Gravity-less square physics objects.
 * Has settable anchor points for more convenient location handling.
 * Has a bounding box collision system.
 */
class PhysicsObject @JvmOverloads constructor(var position:DoubleVector = DoubleVector(0, 0, false),
	var velocity:DoubleVector = DoubleVector(0.0, 0.0, false),
	/**
	 * Object "Health". Set to -1 to make indestructible.
	 */
	private var collisionsToDestroy:Int = -1, private var blockSizeX:Int = 1, private var blockSizeY:Int = 1,
	private var anchorPoint:Int = 0,
	var color:Int = 1):Cloneable {
	/**
	 * Property identitifiers. Is set automatically during constructions but can be overridden by external methods.
	 *
	 *
	 * isStatic is default true if the object cannot move.
	 * canCollide is default true if the object can collide with other objects.
	 * isDestructible is default true if the object can be destroyed.
	 */
	private var isStatic:Boolean
	private var canCollide:Boolean
	private var isDestructible:Boolean
	var bounces = 0
	/**
	 * Gets the object's physics bounding box. (Top-Left and Bottom-Right coordinate).
	 * Please override this.
	 *
	 * @return Collision bounding box.
	 */
	val boundingBox:Array<DoubleArray>
		get() {
			val baseSize = 16
			val sizeX = blockSizeX*baseSize
			val sizeY = blockSizeY*baseSize
			var anchor = doubleArrayOf(0.0, 0.0)
			when(anchorPoint) {
				ANCHOR_POINT_TL -> anchor = doubleArrayOf(0.0, 0.0)
				ANCHOR_POINT_TM -> anchor = doubleArrayOf((sizeX/2).toDouble(), 0.0)
				ANCHOR_POINT_TR -> anchor = doubleArrayOf((sizeX-1).toDouble(), 0.0)
				ANCHOR_POINT_ML -> anchor = doubleArrayOf(0.0, (sizeY/2).toDouble())
				ANCHOR_POINT_MM -> anchor = doubleArrayOf((sizeX/2).toDouble(), (sizeY/2).toDouble())
				ANCHOR_POINT_MR -> anchor = doubleArrayOf((sizeX-1).toDouble(), (sizeY/2).toDouble())
				ANCHOR_POINT_LL -> anchor = doubleArrayOf(0.0, (sizeY-1).toDouble())
				ANCHOR_POINT_LM -> anchor = doubleArrayOf((sizeX/2).toDouble(), (sizeY-1).toDouble())
				ANCHOR_POINT_LR -> anchor = doubleArrayOf((sizeX-1).toDouble(), (sizeY-1).toDouble())
			}
			return arrayOf(doubleArrayOf(position.x-anchor[0], position.y-anchor[1]), doubleArrayOf(
				position.x+sizeX-anchor[0], position.y+sizeY-anchor[1]))
		}
	/**
	 * Get x-coordinate of top-left corner of bounding box.
	 *
	 * @return x-coordinate
	 */
	val minX:Double get() = boundingBox[0][0]
	/**
	 * Get y-coordinate of top-left corner of bounding box.
	 *
	 * @return y-coordinate
	 */
	private val minY:Double get() = boundingBox[0][1]
	/**
	 * Get x-coordinate of bottom-right corner of bounding box.
	 *
	 * @return x-coordinate
	 */
	val maxX:Double get() = boundingBox[1][0]
	/**
	 * Get y-coordinate of bottom-right corner of bounding box.
	 *
	 * @return y-coordinate
	 */
	val maxY:Double get() = boundingBox[1][1]
	/**
	 * Draw instance blocks to engine.
	 *
	 * @param receiver Block renderer.
	 * @param engine   Current GameEngine.
	 * @param playerID Current player ID.
	 */
	fun draw(receiver:EventReceiver, engine:GameEngine, playerID:Int) {
		val size = 16
		for(y in 0 until blockSizeY) {
			for(x in 0 until blockSizeX) {
				receiver.drawBlock(minX.toInt()+x*size, minY.toInt()+y*size, color, engine.skin, false, 0f, 1f, 1f)
			}
		}
	}
	/**
	 * Do one movement tick.
	 */
	fun move() {
		if(!isStatic) position = DoubleVector.plus(position, velocity)
	}
	/**
	 * Do one movement tick, separated into multiple subticks in order to not merge into / pass through an object.
	 *
	 * @param subticks  Amount of movement subticks (recommended >= 4)
	 * @param obstacles All obstacles.
	 * @return Did the object collide at all?
	 */
	fun move(subticks:Int, obstacles:ArrayList<PhysicsObject>, retract:Boolean):Boolean {
		if(isStatic) return false
		val v:DoubleVector = DoubleVector.div(velocity, subticks.toDouble())
		for(i in 0 until subticks) {
			if(retract) position = DoubleVector.plus(position, v)
			for(obj in obstacles) {
				if(checkCollision(this, obj)) {
					position = DoubleVector.minus(position, v)
					return true
				}
			}
		}
		return false
	}
	/**
	 * Do one movement tick, separated into multiple subticks in order to not merge into / pass through an object.
	 *
	 * @param subticks  Amount of movement subticks (recommended >= 4)
	 * @param obstacles All obstacles.
	 * @return Did the object collide at all?
	 */
	fun move(subticks:Int, obstacles:Array<PhysicsObject>, retract:Boolean):Boolean {
		if(isStatic) return false
		val v:DoubleVector = DoubleVector.div(velocity, subticks.toDouble())
		for(i in 0 until subticks) {
			position = DoubleVector.plus(position, v)
			for(obj in obstacles) {
				if(checkCollision(this, obj)) {
					if(retract) position = DoubleVector.minus(position, v)
					return true
				}
			}
		}
		return false
	}
	/**
	 * Do one movement tick with a custom velocity.
	 */
	fun move(velocity:DoubleVector) {
		if(!isStatic) position = DoubleVector.plus(position, velocity)
	}
	/**
	 * Clones the current PhysicsObject instance.
	 *
	 * @return A complete clone of the current PhysicsObject instance.
	 */
	public override fun clone():PhysicsObject {
		val clone:PhysicsObject
		return try {
			clone = super.clone() as PhysicsObject
			clone.position = position
			clone.velocity = velocity
			clone.collisionsToDestroy = collisionsToDestroy
			clone.blockSizeX = blockSizeX
			clone.blockSizeY = blockSizeY
			clone.anchorPoint = anchorPoint
			clone.color = color
			clone.isStatic = isStatic
			clone.isDestructible = isDestructible
			clone.canCollide = canCollide
			clone
		} catch(e:Exception) {
			// Empty, but log error.
			log.error("Failed to create clone.", e)
			PhysicsObject()
		}
	}
	/**
	 * Copies another PhysicsObject instance's fields to this instance.
	 *
	 * @param obj PhysicsObject to use fields from.
	 */
	fun copy(obj:PhysicsObject) {
		position = obj.position
		velocity = obj.velocity
		collisionsToDestroy = obj.collisionsToDestroy
		blockSizeX = obj.blockSizeX
		blockSizeY = obj.blockSizeY
		anchorPoint = obj.anchorPoint
		color = obj.color
		isStatic = obj.isStatic
		isDestructible = obj.isDestructible
		canCollide = obj.canCollide
	}

	companion object {
		/**
		 * Anchor points for position.
		 *
		 *
		 * This selects where the position represents on the object.
		 *
		 *
		 * TL = top-left corner.
		 * TM = top-middle.
		 * TR = top-right corner.
		 * ML = middle-left.
		 * MM = centre.
		 * MR = middle-right.
		 * LL = bottom-left corner.
		 * LM = bottom-middle.
		 * LR = bottom-right corner.
		 */
		const val ANCHOR_POINT_TL = 0
		const val ANCHOR_POINT_TM = 1
		const val ANCHOR_POINT_TR = 2
		const val ANCHOR_POINT_ML = 3
		const val ANCHOR_POINT_MM = 4
		const val ANCHOR_POINT_MR = 5
		const val ANCHOR_POINT_LL = 6
		const val ANCHOR_POINT_LM = 7
		const val ANCHOR_POINT_LR = 8
		private val log = Logger.getLogger(PhysicsObject::class.java)
		/**
		 * Checks if two PhysicsObject instances are intersecting each other.
		 * If either instance has collisions disabled,
		 *
		 * @param a First PhysicsObject instance
		 * @param b Sirst PhysicsObject instance
		 * @return Boolean that says if the instances are intersecting.
		 */
		fun checkCollision(a:PhysicsObject, b:PhysicsObject):Boolean {
			if(!a.canCollide) return false
			if(!b.canCollide) return false
			val bboxA = a.boundingBox
			val aMinX = bboxA[0][0]
			val aMinY = bboxA[0][1]
			val aMaxX = bboxA[1][0]
			val aMaxY = bboxA[1][1]
			val bboxB = b.boundingBox
			val bMinX = bboxB[0][0]
			val bMinY = bboxB[0][1]
			val bMaxX = bboxB[1][0]
			val bMaxY = bboxB[1][1]
			if((aMaxX in bMinX..bMaxX&&aMaxY in bMinY..bMaxY)||(aMinX in bMinX..bMaxX&&aMinY in bMinY..bMaxY)||
				(bMaxX in aMinX..aMaxX&&bMaxY in aMinY..aMaxY)||(bMinX in aMinX..aMaxX&&bMinY in aMinY..aMaxY)) {
				return true
			}
			return false
		}
		/**
		 * Conducts a flat-surface reflection.
		 *
		 * @param vector   Velocity vector to modify.
		 * @param vertical `true` if using a vertical mirror line. `false` if using a horizontal mirror line.
		 */
		fun reflectVelocity(vector:DoubleVector, vertical:Boolean) {
			if(vertical) vector.y = vector.y*-1
			else vector.x = vector.x*-1

		}
		/**
		 * Conducts a flat-surface reflection.
		 *
		 * @param vector      Velocity vector to modify.
		 * @param vertical    `true` if using a vertical mirror line. `false` if using a horizontal mirror line.
		 * @param restitution The amount of "bounce". Use a value between 0 and 1.
		 */
		fun reflectVelocityWithRestitution(vector:DoubleVector, vertical:Boolean, restitution:Double) {
			if(vertical) vector.y = vector.y*-1
			else vector.x = vector.x*-1

			vector.magnitude = vector.magnitude*restitution
		}
	}

	init {
		isStatic = velocity.magnitude==0.0
		isDestructible = collisionsToDestroy>0
		canCollide = true
	}
}