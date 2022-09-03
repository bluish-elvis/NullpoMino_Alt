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

class BackgroundHorizontalBars:AnimatedBackgroundHook {
	// private ResourceHolderCustomAssetExtension customHolder;
	private var chunks:Array<ImageChunk> = emptyArray()
	private var pulsePhaseMax = 0
	private var currentPulsePhase = 0
	private var pulseBaseScale:Float? = null
	private var pulseScaleVariance:Float? = null
	private var reverse = false

	constructor(bgNumber:Int, pulseFrames:Int, sliceSize:Int?, pulseBaseScale:Float?, pulseScaleVariance:Float?,
		reverse:Boolean) {
		val bgNumber = if(bgNumber in 1..19) bgNumber else 0
		customHolder = ResourceHolderCustomAssetExtension()
		customHolder.loadImage("res/graphics/back$bgNumber.png", imageName)
		setup(pulseFrames, sliceSize, pulseBaseScale, pulseScaleVariance, reverse)
		log.debug("Non-custom horizontal bars background ($bgNumber) created.")
	}

	constructor(filePath:String, pulseFrames:Int, sliceSize:Int?, pulseBaseScale:Float?, pulseScaleVariance:Float?,
		reverse:Boolean) {
		customHolder = ResourceHolderCustomAssetExtension()
		customHolder.loadImage(filePath, imageName)
		setup(pulseFrames, sliceSize, pulseBaseScale, pulseScaleVariance, reverse)
		log.debug("Custom horizontal bars background created (File Path: $filePath).")
	}

	override fun setBG(bg:Int) {
		customHolder.loadImage("res/graphics/back$bg.png", imageName)
		log.debug("Non-custom horizontal bars background modified (New BG: $bg).")
	}

	override fun setBG(filePath:String) {
		customHolder.loadImage(filePath, imageName)
		log.debug("Custom horizontal bars background modified (New File Path: $filePath).")
	}
	/**
	 * Allows the hot-swapping of preloaded BGs from a storage instance of a `ResourceHolderCustomAssetExtension`.
	 *
	 * @param holder Storage instance
	 * @param name   Image name
	 */
	override fun setBGFromHolder(holder:ResourceHolderCustomAssetExtension, name:String) {
		customHolder.putImageAt(holder.getImageAt(name), imageName)
		log.debug("Custom horizontal bars background modified (New Image Reference: $name).")
	}

	private fun setup(pulseFrames:Int, sliceSize:Int?, pulseBaseScale:Float?, pulseScaleVariance:Float?, reverse:Boolean) {
		if(pulseBaseScale==null||pulseScaleVariance==null||sliceSize==null) {
			chunks = Array(AMT) {i ->
				ImageChunk(
					ImageChunk.ANCHOR_POINT_ML, intArrayOf(0, 480/AMT*i+480/AMT/2),
					intArrayOf(0, 480/AMT*i), intArrayOf(640, 480/AMT), floatArrayOf(1f, BASE_SCALE)
				)
			}
			this.reverse = reverse
			pulsePhaseMax = pulseFrames
			currentPulsePhase = pulsePhaseMax
		} else {
			this.pulseBaseScale = pulseBaseScale
			this.pulseScaleVariance = pulseScaleVariance
			chunks = Array(sliceSize) {i ->
				ImageChunk(
					ImageChunk.ANCHOR_POINT_ML, intArrayOf(0, 480/sliceSize*i+480/sliceSize/2),
					intArrayOf(0, 480/sliceSize*i), intArrayOf(640, 480/sliceSize), floatArrayOf(1f, pulseBaseScale)
				)
			}
			this.reverse = reverse
			pulsePhaseMax = pulseFrames
			currentPulsePhase = pulsePhaseMax
		}
	}

	fun modifyValues(pulseFrames:Int, pulseBaseScale:Float?, pulseScaleVariance:Float?, reverse:Boolean) {
		this.reverse = reverse
		pulsePhaseMax = pulseFrames
		if(pulseBaseScale!=null) this.pulseBaseScale = pulseBaseScale
		if(pulseScaleVariance!=null) this.pulseScaleVariance = pulseScaleVariance
		if(currentPulsePhase>pulsePhaseMax) currentPulsePhase = pulsePhaseMax
	}

	fun resetPulseScaleValues() {
		if(pulseBaseScale!=null) pulseBaseScale = null
		if(pulseScaleVariance!=null) pulseScaleVariance = null
	}

	override fun update() {
		currentPulsePhase = (currentPulsePhase+1)%pulsePhaseMax
		for(i in chunks.indices) {
			var j = i
			if(reverse) j = chunks.size-i-1
			val ppu = (currentPulsePhase+i)%pulsePhaseMax
			val baseScale = if(pulseBaseScale==null) BASE_SCALE else pulseBaseScale!!
			val scaleVariance = if(pulseScaleVariance==null) SCALE_VARIANCE else pulseScaleVariance!!
			var newScale = baseScale+sin(TWO_PI*(ppu.toDouble()/pulsePhaseMax))*scaleVariance
			if(newScale<1.0) newScale = 1.0
			chunks[j].scale = floatArrayOf(1f, newScale.toFloat())
		}
	}

	override fun reset() {
		currentPulsePhase = pulsePhaseMax
		update()
	}

	override fun draw(engine:GameEngine) {
		val priorityList = ArrayList<ImageChunk>()
		priorityList.addAll(chunks)
		priorityList.sortWith {c1:ImageChunk, c2:ImageChunk ->
			c1.scale[1].compareTo(c2.scale[1])
		}
		val baseScale = if(pulseBaseScale==null) BASE_SCALE else pulseBaseScale!!
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

	companion object {
		private const val AMT = 480/3
		private const val TWO_PI = PI*2
		private const val BASE_SCALE = 1f
		private const val SCALE_VARIANCE = 1f
	}

	override val id:Int = ANIMATION_PULSE_HORIZONTAL_BARS

	init {
		imageName = ("localBG")
	}
}
