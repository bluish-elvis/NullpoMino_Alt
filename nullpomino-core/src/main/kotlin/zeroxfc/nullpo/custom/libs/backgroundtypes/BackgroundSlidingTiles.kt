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
package zeroxfc.nullpo.custom.libs.backgroundtypes

import mu.nu.nullpo.game.component.Block.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.slick.ResourceHolderCustomAssetExtension
import kotlin.math.abs
import kotlin.random.Random

class BackgroundSlidingTiles:AnimatedBackgroundHook {
	private var gridChunks:Array<Array<ImageChunk>> = emptyArray()
	private var colors:Array<Array<COLOR>> = emptyArray()
	private var skin:Int? = null
	private var size = 0
	private var color:COLOR? = null
	private var darkness:Float? = null
	private var custom:Boolean
	private var directionRandomizer:Random
	private var direction = 0
	private var horizontal = false
	private var currentMovement = 0
	private var width = 0
	private var height = 0
	private var move = false

	constructor(skin:Int, directionRandomizer:Random, color:Int?, size:Int, darkness:Float) {
		custom = false
		this.skin = if(skin in 1 until ResourceHolderCustomAssetExtension.numberLoadedBlockSkins) skin else 0
		this.color = COLOR.all[color ?: 0]
		this.size = size
		this.darkness = darkness
		this.directionRandomizer = directionRandomizer
		setup()
		log.debug("Non-custom sliding tiles background created (Skin: $skin).")
	}

	constructor(skin:Int, seed:Long, color:Int?, size:Int, darkness:Float) {
		custom = false
		this.skin = if(skin in 1 until ResourceHolderCustomAssetExtension.numberLoadedBlockSkins) skin else 0
		this.color = COLOR.all[color ?: 0]
		this.size = size
		this.darkness = darkness
		directionRandomizer = Random(seed)
		setup()
		log.debug("Non-custom sliding tiles background created (Skin: $skin).")
	}

	constructor(filePath:String, directionRandomizer:Random) {
		custom = true
		this.directionRandomizer = directionRandomizer
		customHolder = ResourceHolderCustomAssetExtension()
		customHolder.loadImage(filePath, imageName)
		setup()
		log.debug("Custom sliding tiles background created (File Path: $filePath).")
	}

	private fun setup() {
		customHolder.loadImage("res/graphics/blank_black_24b.png", "blackBG")
		direction = directionRandomizer.nextInt(DIRECTIONS)
		if(custom) {
			val dim = customHolder.getImageDimensions(imageName)
			width = dim[0]
			height = dim[1]
			var sw = 640/dim[0]
			if(sw*dim[0]<640) sw++
			sw += 2
			var sh = 480/dim[1]
			if(sh*dim[1]<480) sh++
			sh += 2
			gridChunks = Array(sh) {y ->
				Array(sw) {x ->
					ImageChunk(
						ImageChunk.ANCHOR_POINT_TL, intArrayOf((x-1)*dim[0], (y-1)*dim[1]),
						intArrayOf(0, 0), intArrayOf(
							dim[0], dim[1]
						), floatArrayOf(1f, 1f)
					)
				}
			}
		} else {
			var s = 16
			if(size<0) s = 8
			if(size>0) s = 32
			width = s
			height = s
			gridChunks = Array(480/s+2) {y ->
				Array(640/s+2) {x ->
					ImageChunk(
						ImageChunk.ANCHOR_POINT_TL, intArrayOf((x-1)*s, (y-1)*s), intArrayOf(0, 0),
						intArrayOf(s, s), floatArrayOf(1f, 1f)
					)
				}
			}
			colors = Array(480/s+2) {y ->
				Array(640/s+2) {x ->
					color ?: (COLOR.all[directionRandomizer.nextInt(8)+1])
				}
			}
		}
	}

	override fun update() {
		if(move) {
			move = false
			if(horizontal) {
				for(y in gridChunks.indices) {
					for(x in 0 until gridChunks[y].size) {
						val locOld = gridChunks[y][x].anchorLocation
						val yMod = abs(locOld[1]/width)
						val dir = (direction+yMod)%DIRECTIONS
						var xNew:Int
						when(dir) {
							DIRECTION_LEFT -> {
								xNew = locOld[0]-1
								if(xNew<=width*-2) xNew = (gridChunks[0].size-2)*width
								gridChunks[y][x].anchorLocation = intArrayOf(xNew, locOld[1])
							}
							DIRECTION_RIGHT -> {
								xNew = locOld[0]+1
								if(xNew>=(gridChunks[0].size-1)*width) xNew = width*-1
								gridChunks[y][x].anchorLocation = intArrayOf(xNew, locOld[1])
							}
							else -> {
							}
						}
					}
				}
				currentMovement++
			} else {
				for(x in 0 until gridChunks[0].size) {
					for(y in gridChunks.indices) {
						val locOld = gridChunks[y][x].anchorLocation
						val xMod = abs(locOld[0]/width)
						val dir2 = (direction+xMod)%DIRECTIONS
						var yNew:Int
						when(dir2) {
							DIRECTION_UP -> {
								yNew = locOld[1]-1
								if(yNew<=height*-2) yNew = (gridChunks.size-2)*height
								gridChunks[y][x].anchorLocation = intArrayOf(locOld[0], yNew)
							}
							DIRECTION_DOWN -> {
								yNew = locOld[1]+1
								if(yNew>=(gridChunks.size-1)*height) yNew = height*-1
								gridChunks[y][x].anchorLocation = intArrayOf(locOld[0], yNew)
							}
							else -> {
							}
						}
					}
				}
				currentMovement++
			}
			if(horizontal) {
				if(currentMovement>=width) {
					currentMovement = 0
					direction = directionRandomizer.nextInt(DIRECTIONS)
					horizontal = false
				}
			} else {
				if(currentMovement>=height) {
					currentMovement = 0
					direction = directionRandomizer.nextInt(DIRECTIONS)
					horizontal = true
				}
			}
		} else {
			move = true
		}
	}

	private fun swap(x:Int, y:Int, x2:Int, y2:Int) {
		val ic = gridChunks[y][x]
		gridChunks[y][x] = gridChunks[y2][x2]
		gridChunks[y][x] = ic
	}

	override fun reset() {
		setup()
		horizontal = true
		move = false
		currentMovement = 0
	}

	override fun draw(engine:GameEngine) {
		customHolder.drawImage("blackBG", 0, 0)
		for(y in gridChunks.indices) {
			for(x in 0 until gridChunks[y].size) {
				val i = gridChunks[y][x]
				val pos = i.drawLocation
				val ddim = i.drawDimensions
				val sloc = i.sourceLocation
				val sdim = i.sourceDimensions
				if(custom) {
					customHolder.drawImage(
						imageName, pos[0], pos[1], ddim[0], ddim[1], sloc[0], sloc[1], sdim[0],
						sdim[1], 255, 255, 255, 255
					)
				} else {
					var s = 1f
					if(size<0) s = 0.5f
					if(size>0) s = 2f
					engine.owner.receiver.drawBlock(
						pos[0], pos[1], colors[y][x].ordinal, skin!!, false,
						darkness!!, 1f, s
					)
				}
			}
		}
	}

	private fun modifyValues(color:Int?, size:Int?, darkness:Float?) {
		if(custom) return
		if(color!=null) this.color = COLOR.all[color]
		if(size!=null) this.size = size
		if(darkness!=null) this.darkness = darkness
		if(color!=null||size!=null) reset()
	}

	fun setSeed(seed:Long) {
		directionRandomizer = Random(seed)
		reset()
	}
	/**
	 * In this case, BG means block skin.
	 *
	 * @param bg Block skin to use.
	 */
	override fun setBG(bg:Int) {
		custom = false
		skin = bg
		color = null
		size = 0
		darkness = 0f
		modifyValues(null, null, null)
		log.debug("Non-custom sliding tiles background modified (New Skin: $bg).")
		log.warn("Please set up new values using modifyValues(...)")
	}

	override fun setBG(filePath:String) {
		custom = true
		customHolder.loadImage(filePath, imageName)
		reset()
		log.debug("Custom sliding tiles background modified (New File Path: $filePath).")
	}
	/**
	 * Allows the hot-swapping of preloaded BGs from a storage instance of a `ResourceHolderCustomAssetExtension`.
	 *
	 * @param holder Storage instance
	 * @param name   Image name
	 */
	override fun setBGFromHolder(holder:ResourceHolderCustomAssetExtension, name:String) {
		custom = true
		customHolder.putImageAt(holder.getImageAt(name), imageName)
		reset()
		log.debug("Custom sliding tiles background modified (New Image Reference: $name).")
	}
	/**
	 * This last one is important. In the case that any of the child types are used, it allows identification.
	 * The identification can be used to allow casting during operations.
	 *
	 * @return Identification number of child class.
	 */
	override val id = ANIMATION_SLIDING_TILES

	companion object {
		private const val DIRECTION_UP = 0
		private const val DIRECTION_RIGHT = 1
		private const val DIRECTION_DOWN = 1
		private const val DIRECTION_LEFT = 0
		private const val DIRECTIONS = 2
	}

	init {
		imageName = "localSkin"
		horizontal = true
		move = false
		currentMovement = 0
	}
}
