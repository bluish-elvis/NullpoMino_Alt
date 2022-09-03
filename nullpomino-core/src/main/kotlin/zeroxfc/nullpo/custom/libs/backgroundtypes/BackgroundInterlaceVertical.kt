/*
 * Copyright (c) 2021-2022,
 * This library class was created by 0xFC963F18DC21 / Shots243
 * It is part of an extension library for the game NullpoMino (copyright 2021-2022)
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

package zeroxfc.nullpo.custom.libs.backgroundtypes

import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.slick.ResourceHolderCustomAssetExtension
import kotlin.math.PI
import kotlin.math.sin

class BackgroundInterlaceVertical:AnimatedBackgroundHook {
	private var chunks:Array<ImageChunk> = emptyArray()
	private var pulseTimer = 0
	private var pulseTimerMax = 0
	private var columnWidth = 0
	private var baseScale = 0f
	private var scaleVariance = 0f
	private var upOdd = false
	private var reverse = false

	constructor(bgNumber:Int, columnWidth:Int, pulseTimerFrames:Int, pulseBaseScale:Float, pulseScaleVariance:Float,
		upOdd:Boolean, reverse:Boolean) {
		var bgNumber = bgNumber
		if(bgNumber<0||bgNumber>19) bgNumber = 0
		customHolder.loadImage("res/graphics/back$bgNumber.png", imageName)
		setup(columnWidth, pulseTimerFrames, pulseBaseScale, pulseScaleVariance, upOdd, reverse)
		log.debug("Non-custom vertical interlace background ($bgNumber) created.")
	}

	constructor(filePath:String, columnWidth:Int, pulseTimerFrames:Int, pulseBaseScale:Float, pulseScaleVariance:Float,
		upOdd:Boolean, reverse:Boolean) {
		customHolder.loadImage(filePath, imageName)
		setup(columnWidth, pulseTimerFrames, pulseBaseScale, pulseScaleVariance, upOdd, reverse)
		log.debug("Custom vertical interlace background created (File Path: $filePath).")
	}

	private fun setup(columnWidth:Int = DEFAULT_COLUMN_WIDTH, pulseTimerFrames:Int = DEFAULT_TIMER_MAX,
		pulseBaseScale:Float = BASE_SCALE, pulseScaleVariance:Float = SCALE_VARIANCE, upOdd:Boolean = UP_ODD_DEFAULT,
		reverse:Boolean = false) {
		this.upOdd = !upOdd
		pulseTimerMax = pulseTimerFrames
		baseScale = pulseBaseScale
		scaleVariance = pulseScaleVariance
		pulseTimer = pulseTimerFrames
		this.reverse = reverse
		this.columnWidth = if(480%columnWidth!=0) DEFAULT_COLUMN_WIDTH else columnWidth
		chunks = Array(SCREEN_WIDTH/columnWidth) {i ->
			val up = upOdd&&i%2==1
			val anchorType:Int = if(up) ImageChunk.ANCHOR_POINT_LL else ImageChunk.ANCHOR_POINT_TL
			val anchorLocation = intArrayOf(i*columnWidth, if(up) SCREEN_HEIGHT else 0)
			val srcLocation = intArrayOf(i*columnWidth, 0)
			ImageChunk(anchorType, anchorLocation, srcLocation, intArrayOf(columnWidth, 480), floatArrayOf(1f, baseScale))
		}
	}
	/**
	 * Performs an update tick on the background. Advisably used in onLast.
	 */
	override fun update() {
		pulseTimer++
		if(pulseTimer>=pulseTimerMax) {
			pulseTimer = 0
		}
		for(i in chunks.indices) {
			var j = i
			if(reverse) j = chunks.size-1-i
			val ppu = (pulseTimer+i)%pulseTimerMax
			val s = sin(PI*(ppu.toDouble()/pulseTimerMax))
			var scale = baseScale+scaleVariance*s
			if(scale<1.0) scale = 1.0
			chunks[j].scale = floatArrayOf(1f, scale.toFloat())
		}
	}

	fun modifyValues(pulseFrames:Int, pulseBaseScale:Float, pulseScaleVariance:Float, upOdd:Boolean) {
		this.upOdd = upOdd
		pulseTimerMax = pulseFrames
		baseScale = pulseBaseScale
		scaleVariance = pulseScaleVariance
		if(pulseTimer>pulseTimerMax) pulseTimer = pulseTimerMax
	}
	/**
	 * Resets the background to its base state.
	 */
	override fun reset() {
		pulseTimer = pulseTimerMax
		update()
	}

	override fun draw(engine:GameEngine) {
		for(i in chunks) {
			val pos = i.drawLocation
			val ddim = i.drawDimensions
			val sloc = i.sourceLocation
			val sdim = i.sourceDimensions
			customHolder.drawImage(
				imageName, pos[0], pos[1], ddim[0], ddim[1], sloc[0], sloc[1], sdim[0],
				sdim[1], 255, 255, 255, 255
			)
		}
	}
	/**
	 * Change BG to one of the default ones.
	 *
	 * @param bg New BG number
	 */
	override fun setBG(bg:Int) {
		customHolder.loadImage("res/graphics/back$bg.png", imageName)
		log.debug("Non-custom vertical interlace background modified (New BG: $bg).")
	}
	/**
	 * Change BG to a custom BG using its file path.
	 *
	 * @param filePath File path of new background
	 */
	override fun setBG(filePath:String) {
		customHolder.loadImage(filePath, imageName)
		log.debug("Custom vertical interlace background modified (New File Path: $filePath).")
	}
	/**
	 * Allows the hot-swapping of preloaded BGs from a storage instance of a `ResourceHolderCustomAssetExtension`.
	 *
	 * @param holder Storage instance
	 * @param name   Image name
	 */
	override fun setBGFromHolder(holder:ResourceHolderCustomAssetExtension, name:String) {
		customHolder.putImageAt(holder.getImageAt(name), imageName)
		log.debug(
			"Custom vertical interlace background modified (New Image Reference: $name)."
		)
	}
	/**
	 * This last one is important. In the case that any of the child types are used, it allows identification.
	 * The identification can be used to allow casting during operations.
	 *
	 * @return Identification number of child class.
	 */
	override val id = ANIMATION_INTERLACE_VERTICAL

	companion object {
		private const val SCREEN_WIDTH = 640
		private const val SCREEN_HEIGHT = 480
		private const val DEFAULT_COLUMN_WIDTH = 1
		private const val DEFAULT_TIMER_MAX = 30
		private const val UP_ODD_DEFAULT = true
		private const val BASE_SCALE = 1f
		private const val SCALE_VARIANCE = 0.1f
	}

	init {
		customHolder = ResourceHolderCustomAssetExtension()
		imageName = ("localBG")
	}
}
