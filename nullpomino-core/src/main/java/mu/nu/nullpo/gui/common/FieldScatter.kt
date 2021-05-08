package mu.nu.nullpo.gui.common

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import zeroxfc.nullpo.custom.libs.DoubleVector
import zeroxfc.nullpo.custom.libs.PhysicsObject
import java.util.*
import kotlin.math.abs

class FieldScatter {
	/**
	 * Blocks to draw
	 */
	private val blocks:ArrayList<PhysicsObject>
	/**
	 * Lifetime
	 */
	private var lifeTime:Int
	/**
	 * Makes a new field explosion.
	 *
	 * @param receiver    Renderer to draw with.
	 * @param engine      Current GameEngine.
	 * @param playerID    Current player ID.
	 * @param clearBlocks Clear the blocks in the field?
	 */
	@JvmOverloads
	constructor(receiver:EventReceiver, engine:GameEngine, playerID:Int, clearBlocks:Boolean = true) {
		// Direction randomizer
		val rdm = Random(engine.randSeed+engine.statistics.time)
		lifeTime = 0
		val eBlock = Block(0, engine.skin)
		blocks = ArrayList()
		for(i in -1*engine.field!!.hiddenHeight until engine.field!!.height) {
			for(j in 0 until engine.field!!.width) {
				// Generate object array.
				val blk = engine.field!!.getBlock(j, i)
				if(blk!!.cint>0) {
					blocks.add(PhysicsObject(
						DoubleVector(receiver.fieldX(engine, playerID)+4+j*16, receiver.fieldY(engine, playerID)+52+i*16, false),
						DoubleVector(rdm.nextDouble()*8, rdm.nextDouble()*Math.PI*2, true),
						-1, 1, 1, PhysicsObject.ANCHOR_POINT_TL, blk.drawColor
					))
					if(clearBlocks) receiver.blockBreak(engine, j, i, blk)
					if(clearBlocks) blk.copy(eBlock)
				}
			}
		}
	}
	/**
	 * Makes a new field explosion. Destroys specfic blocks.
	 *
	 * @param receiver            Renderer to draw with.
	 * @param engine              Current GameEngine.
	 * @param playerID            Current player ID.
	 * @param fieldBlockLocations Blocck locations.
	 */
	constructor(receiver:EventReceiver, engine:GameEngine, playerID:Int, fieldBlockLocations:ArrayList<IntArray>) {
		/** Direction randomizer  */
		val rdm = Random(engine.randSeed+engine.statistics.time)
		lifeTime = 0
		val eBlock = Block(0, engine.skin)
		blocks = ArrayList()
		for(loc in fieldBlockLocations) {
			val j = loc[0]
			val i = loc[1]

			// Generate object array.
			val blk = engine.field!!.getBlock(j, i)
			if(blk!!.cint>0) {
				blocks.add(PhysicsObject(
					DoubleVector(
						receiver.fieldX(engine, playerID)+4+j*16,
						receiver.fieldY(engine, playerID)+52+i*16,
						false
					),
					DoubleVector(
						rdm.nextDouble()*8, rdm.nextDouble()*Math.PI*2, true
					),
					-1, 1, 1, PhysicsObject.ANCHOR_POINT_TL, blk.cint
				))
				receiver.blockBreak(engine, j, i, blk)
				blk.copy(eBlock)
			}
		}
	}
	/**
	 * Makes a new field explosion. Destroys specfic blocks.
	 *
	 * @param receiver            Renderer to draw with.
	 * @param engine              Current GameEngine.
	 * @param playerID            Current player ID.
	 * @param fieldBlockLocations Blocck locations.
	 */
	constructor(receiver:EventReceiver, engine:GameEngine, playerID:Int, fieldBlockLocations:Array<IntArray>):this(receiver,
		engine, playerID, listOf<IntArray>(*fieldBlockLocations) as ArrayList<IntArray>)
	/**
	 * Updates the life cycle of the explosion.
	 */
	fun update() {
		if(shouldNull()) return
		for(pho /* NOTE: Delicious */ in blocks) {
//			if ((pho.position.getX() <= -16 || pho.position.getX() > 640)) continue;
//			if ((pho.position.getY() > 460 && Math.abs(pho.velocity.getMagnitude()) < 0.0001)) continue;
			pho.move()
			if(pho.position.y>465) {
				PhysicsObject.reflectVelocityWithRestitution(pho.velocity, true, 0.75)
				while(pho.position.y>465) pho.move()
			}
			pho.velocity = DoubleVector.plus(pho.velocity, GRAVITY)
		}
		blocks.removeIf {block:PhysicsObject -> block.position.x<=-16||block.position.x>640}
		blocks.removeIf {block:PhysicsObject -> block.position.y>460&&abs(block.velocity.magnitude)<0.0001}
		lifeTime++
	}
	/**
	 * Draws the explosion.
	 *
	 * @param receiver Renderer to draw explosion with.
	 * @param engine   Current GameEngine.
	 * @param playerID Current playerID.
	 */
	fun draw(receiver:EventReceiver, engine:GameEngine, playerID:Int) {
		if(shouldNull()) return
		for(pho in blocks) {
//			if ((pho.position.getX() <= -16 || pho.position.getX() > 640)) continue;
//			if ((pho.position.getY() > 460 && Math.abs(pho.velocity.getMagnitude()) < 0.0001)) continue;
			pho.draw(receiver, engine, playerID)
		}
	}
	/**
	 * Checks if the object's lifetime has expired.
	 *
	 * @return Expiry status
	 */
	private fun shouldNull():Boolean = lifeTime>=600

	companion object {
		/**
		 * Gravity
		 */
		private val GRAVITY = DoubleVector(0.0, 1.0/6.0, false)
	}
}