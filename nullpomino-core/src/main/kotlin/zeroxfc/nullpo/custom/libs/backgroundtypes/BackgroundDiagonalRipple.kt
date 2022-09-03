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

import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.slick.ResourceHolderCustomAssetExtension
import kotlin.math.PI
import kotlin.math.sin

class BackgroundDiagonalRipple:AnimatedBackgroundHook {
	private var chunkGrid:Array<Array<ImageChunk>> = emptyArray()
	private var reverse = false
	private var reverseSlant = false
	private var pulsePhaseMax = 0
	private var currentPulsePhase = 0
	private var pulseBaseScale:Float = BASE_SCALE
	private var pulseScaleVariance:Float = SCALE_VARIANCE

	constructor(bgNumber:Int, cellWidth:Int?, cellHeight:Int?, pulseFrames:Int, pulseBaseScale:Float?, pulseScaleVariance:Float?,
		reverse:Boolean, reverseSlant:Boolean) {
		val num = if(bgNumber in 1..19) bgNumber else 0
		customHolder = ResourceHolderCustomAssetExtension()
		customHolder.loadImage("res/graphics/back$num.png", imageName)
		setup(cellWidth, cellHeight, pulseFrames, pulseBaseScale, pulseScaleVariance, reverse, reverseSlant)
		log.debug("Non-custom diagonal ripple background ($num) created.")
	}

	constructor(filePath:String, cellWidth:Int?, cellHeight:Int?, pulseFrames:Int, pulseBaseScale:Float?,
		pulseScaleVariance:Float?, reverse:Boolean, reverseSlant:Boolean) {
		customHolder = ResourceHolderCustomAssetExtension()
		customHolder.loadImage(filePath, imageName)
		setup(cellWidth, cellHeight, pulseFrames, pulseBaseScale, pulseScaleVariance, reverse, reverseSlant)
		log.debug("Custom diagonal ripple background created (File Path: $filePath).")
	}

	fun modifyValues(pulseFrames:Int, pulseBaseScale:Float?, pulseScaleVariance:Float?, reverse:Boolean, reverseSlant:Boolean) {
		this.reverse = reverse
		this.reverseSlant = reverseSlant
		pulsePhaseMax = pulseFrames
		if(pulseBaseScale!=null) this.pulseBaseScale = pulseBaseScale
		if(pulseScaleVariance!=null) this.pulseScaleVariance = pulseScaleVariance
		if(currentPulsePhase>pulsePhaseMax) currentPulsePhase = pulsePhaseMax
	}

	fun resetPulseScaleValues() {
		pulseBaseScale = BASE_SCALE
		pulseScaleVariance = SCALE_VARIANCE
	}

	private fun setup(cellWidth:Int?, cellHeight:Int?, pulseFrames:Int, pulseBaseScale:Float?, pulseScaleVariance:Float?,
		reverse:Boolean, reverseSlant:Boolean) {
		this.reverse = reverse
		this.reverseSlant = reverseSlant
		pulsePhaseMax = pulseFrames
		currentPulsePhase = pulsePhaseMax
		if(pulseBaseScale==null||pulseScaleVariance==null||cellWidth==null||cellHeight==null) {
			chunkGrid = Array(DEF_GRID_HEIGHT) {y ->
				Array(DEF_GRID_WIDTH) {x ->
					ImageChunk(
						ImageChunk.ANCHOR_POINT_MM, intArrayOf(
							DEF_FIELD_DIM*x+DEF_FIELD_DIM/2, DEF_FIELD_DIM*y+DEF_FIELD_DIM/2
						), intArrayOf(
							DEF_FIELD_DIM*x, DEF_FIELD_DIM*y
						), intArrayOf(DEF_FIELD_DIM, DEF_FIELD_DIM), floatArrayOf(BASE_SCALE, BASE_SCALE)
					)
				}
			}
		} else {
			this.pulseBaseScale = pulseBaseScale
			this.pulseScaleVariance = pulseScaleVariance
			val w:Int = if(640%cellWidth!=0) 8 else 640/cellWidth
			val h:Int = if(480%cellHeight!=0) 8 else 480/cellHeight
			chunkGrid = Array(h) {y ->
				Array(w) {x ->
					ImageChunk(
						ImageChunk.ANCHOR_POINT_MM,
						intArrayOf(cellWidth*x+cellWidth/2, cellHeight*y+cellHeight/2), intArrayOf(
							cellWidth*x, cellHeight*y
						), intArrayOf(cellWidth, cellHeight), floatArrayOf(pulseBaseScale, pulseBaseScale)
					)
				}
			}
		}
	}

	override fun update() {
		currentPulsePhase = (currentPulsePhase+1)%pulsePhaseMax
		for(y in chunkGrid.indices) {
			for(x in 0 until chunkGrid[y].size) {
				var j = currentPulsePhase
				if(reverse) j = pulsePhaseMax-currentPulsePhase-1
				if(reverseSlant) {
					j -= x+y
				} else {
					j += x+y
				}
				var ppu = j%pulsePhaseMax
				if(ppu<0) ppu = pulsePhaseMax-ppu
				val baseScale = pulseBaseScale
				val scaleVariance = pulseScaleVariance
				var newScale = baseScale+sin(TWO_PI*(ppu.toDouble()/pulsePhaseMax))*scaleVariance
				if(newScale<1.0) newScale = 1.0
				chunkGrid[y][x].scale = floatArrayOf(newScale.toFloat(), newScale.toFloat())
			}
		}
	}

	override fun reset() {
		currentPulsePhase = pulsePhaseMax
		update()
	}

	override fun draw(engine:GameEngine) {
		val priorityList = ArrayList<ImageChunk>()
		for(imageChunks in chunkGrid) {
			priorityList.addAll(imageChunks)
		}
		priorityList.sortWith {c1:ImageChunk, c2:ImageChunk ->
			c1.scale[0].compareTo(c2.scale[0])
		}
		val baseScale = pulseBaseScale
		if(almostEqual(baseScale.toDouble(), 1.0, 0.005)) {
			customHolder.drawImage(imageName, 0, 0)
			priorityList.removeAll {imageChunk:ImageChunk ->
				almostEqual(
					imageChunk.scale[0].toDouble(), 1.0, 0.005
				)
			}
		}
		for(i in priorityList) {
			val pos = i.drawLocation
			val ddim = i.drawDimensions
			val sloc = i.sourceLocation
			val sdim = i.sourceDimensions
			customHolder.drawImage(
				imageName, pos[0], pos[1], ddim[0], ddim[1], sloc[0], sloc[1], sdim[0], sdim[1],
				255, 255, 255, 255
			)
		}
	}

	override fun setBG(bg:Int) {
		customHolder.loadImage("res/graphics/back$bg.png", imageName)
		log.debug("Non-custom diagonal ripple background modified (New BG: $bg).")
	}

	override fun setBG(filePath:String) {
		customHolder.loadImage(filePath, imageName)
		log.debug("Custom diagonal ripple background modified (New File Path: $filePath).")
	}
	/**
	 * Allows the hot-swapping of preloaded BGs from a storage instance of a `ResourceHolderCustomAssetExtension`.
	 *
	 * @param holder Storage instance
	 * @param name   Image name
	 */
	override fun setBGFromHolder(holder:ResourceHolderCustomAssetExtension, name:String) {
		customHolder.putImageAt(holder.getImageAt(name), imageName)
		log.debug("Custom diagonal ripple background modified (New Image Reference: $name).")
	}

	companion object {
		private const val TWO_PI = PI*2
		private const val DEF_FIELD_DIM = 8
		private const val DEF_GRID_WIDTH = 640/DEF_FIELD_DIM
		private const val DEF_GRID_HEIGHT = 480/DEF_FIELD_DIM
		private const val BASE_SCALE = 1f
		private const val SCALE_VARIANCE = 1f
	}

	override val id:Int = ANIMATION_DIAGONAL_RIPPLE

	init {
		imageName = ("localBG")
	}
}
