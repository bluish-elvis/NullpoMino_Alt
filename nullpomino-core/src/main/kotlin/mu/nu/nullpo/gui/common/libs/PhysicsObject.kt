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
package mu.nu.nullpo.gui.common.libs

import org.apache.logging.log4j.LogManager

/**
 * PhysicsObject
 *
 * Gravity-less square physics objects.
 * Has settable anchor points for more convenient location handling.
 * Has a bounding box collision system.
 */
abstract class PhysicsObject @JvmOverloads constructor(
	var pos:Vector = Vector(0, 0),
	var vel:Vector = Vector(0, 0),
	/** Object "Health". Set to -1 to make indestructible .*/
	protected var collisionsToDestroy:Int = -1,
	protected var width:Int = 1, private var height:Int = 1,
	private var anchorPoint:Int = 0
):Cloneable {
	/**
	 * Property identitifiers. Is set automatically during constructions but can be overridden by external methods.
	 * isStatic is default true if the object cannot move.
	 * canCollide is default true if the object can collide with other objects.
	 * isDestructible is default true if the object can be destroyed.
	 */
	protected var isStatic:Boolean = vel.magnitude==0f
	protected var canCollide:Boolean = !isStatic
	protected var isDestructible:Boolean = collisionsToDestroy>0
	var bounces = 0
		private set
	var ticks = 0
		private set
	/**
	 * Gets the object's physics bounding box. (Top-Left and Bottom-Right coordinate).
	 * Please override this.
	 * @return Collision bounding box.
	 */
	open val boundingBox:Array<FloatArray>
		get() {

			val sizeX = width
			val sizeY = height
			var anchor = floatArrayOf(0f, 0f)
			when(anchorPoint) {
				ANCHOR_POINT_TL -> anchor = floatArrayOf(0f, 0f)
				ANCHOR_POINT_TM -> anchor = floatArrayOf((sizeX/2f), 0f)
				ANCHOR_POINT_TR -> anchor = floatArrayOf((sizeX-1f), 0f)
				ANCHOR_POINT_ML -> anchor = floatArrayOf(0f, (sizeY/2f))
				ANCHOR_POINT_MM -> anchor = floatArrayOf((sizeX/2f), (sizeY/2f))
				ANCHOR_POINT_MR -> anchor = floatArrayOf((sizeX-1f), (sizeY/2f))
				ANCHOR_POINT_LL -> anchor = floatArrayOf(0f, (sizeY-1f))
				ANCHOR_POINT_LM -> anchor = floatArrayOf((sizeX/2f), (sizeY-1f))
				ANCHOR_POINT_LR -> anchor = floatArrayOf((sizeX-1f), (sizeY-1f))
			}
			return arrayOf(
				floatArrayOf(pos.x-anchor[0], pos.y-anchor[1]),
				floatArrayOf(pos.x+sizeX-anchor[0], pos.y+sizeY-anchor[1])
			)
		}
	/** Get x-coordinate of top-left corner of bounding box.*/
	val minX:Float get() = boundingBox[0][0]
	/** Get y-coordinate of top-left corner of bounding box.*/
	val minY:Float get() = boundingBox[0][1]
	/** Get x-coordinate of bottom-right corner of bounding box.*/
	val maxX:Float get() = boundingBox[1][0]
	/** Get y-coordinate of bottom-right corner of bounding box.*/
	val maxY:Float get() = boundingBox[1][1]

	/** Do one movement tick.*/
	fun move() {
		if(isStatic) return
		pos += vel
		ticks++
	}
	/**
	 * Do one movement tick, separated into multiple sub-ticks in order to not merge into / pass through an object.
	 *
	 * @param subTicks  Amount of movement sub-ticks (recommended >= 4)
	 * @param obstacles All obstacles.
	 * @param retract if collide
	 * @return Did the object collide to any obstacles?
	 */
	fun move(subTicks:Int, obstacles:Iterable<PhysicsObject>, retract:Boolean):Boolean {
		ticks++
		if(isStatic) return false
		val v:Vector = vel/subTicks
		for(i in 0 until subTicks) {
			pos += v
			for(obj in obstacles) {
				if(checkCollision(this, obj)) {
					if(retract) pos -= v
					return true
				}
			}
		}
		return false
	}
	/**
	 * Do one movement tick, separated into multiple sub-ticks in order to not merge into / pass through an object.
	 *
	 * @param subTicks  Amount of movement sub-ticks (recommended >= 4)
	 * @param obstacles All obstacles.
	 * @return Did the object collide to any obstacles?
	 */
	fun move(subTicks:Int, obstacles:Array<PhysicsObject>, retract:Boolean):Boolean {
		ticks++
		if(isStatic) return false
		val v:Vector = vel/subTicks
		for(i in 0 until subTicks) {
			pos += v
			for(obj in obstacles) {
				if(checkCollision(this, obj)) {
					if(retract) pos -= v
					return true
				}
			}
		}
		return false
	}
	/**
	 * Do one movement tick with a custom velocity.
	 */
	fun move(velocity:Vector) {
		if(!isStatic) return
		pos += velocity
		ticks++
	}
	/**
	 * Clones the current PhysicsObject instance.
	 * @return A complete clone of the current PhysicsObject instance.
	 */
	public override fun clone():PhysicsObject {
		return try {
			(super.clone() as PhysicsObject).apply {
				replace(this)
			}
		} catch(e:Exception) {
			// Empty, but log error.
			log.error("Failed to create clone.", e)
			this
		}
	}
	/**
	 * Copies another PhysicsObject instance's fields to this instance.
	 * @param obj PhysicsObject to use fields from.
	 */
	fun replace(obj:PhysicsObject) {
		pos = obj.pos
		vel = obj.vel
		collisionsToDestroy = obj.collisionsToDestroy
		width = obj.width
		height = obj.height
		anchorPoint = obj.anchorPoint
		isStatic = obj.isStatic
		isDestructible = obj.isDestructible
		canCollide = obj.canCollide
	}

	companion object {
		/**
		 * Anchor points for position.
		 *
		 * This selects where the position represents on the object.
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
		private val log = LogManager.getLogger()
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
		fun reflectVelocity(vector:Vector, vertical:Boolean) {
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
		fun reflectVelocityWithRestitution(vector:Vector, vertical:Boolean, restitution:Float) {
			if(vertical) vector.y = vector.y*-1
			else vector.x = vector.x*-1

			vector.magnitude = vector.magnitude*restitution
		}
	}
}
