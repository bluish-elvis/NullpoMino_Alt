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
import zeroxfc.nullpo.custom.libs.AnchorPoint
import kotlin.random.Random

class BackgroundFakeScanlines:AnimatedBackgroundHook {
	// private ResourceHolderCustomAssetExtension customHolder;
	private var colorRandom:Random = Random.Default
	private var chunks:Array<ImageChunk> = emptyArray()
	private var phase = 0

	constructor(bgNumber:Int) {
		var bgNumber = bgNumber
		if(bgNumber<0||bgNumber>19) bgNumber = 0
		customHolder = ResourceHolderCustomAssetExtension()
		customHolder.loadImage("res/graphics/back$bgNumber.png", imageName)
		setup()
		log.debug("Non-custom fake scanline background ($bgNumber) created.")
	}

	constructor(filePath:String) {
		customHolder = ResourceHolderCustomAssetExtension()
		customHolder.loadImage(filePath, imageName)
		setup()
		log.debug("Custom fake scanline background created (File Path: $filePath).")
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

	private fun setup() {
		// Generate chunks
		chunks = Array(AMT) {i ->
			ImageChunk(
				AnchorPoint.TL, listOf(0, 480/AMT*i+480/AMT/2), listOf(0, 480/AMT*i),
				listOf(640, 480/AMT), listOf(1f, 1f)
			)
		}
		phase = 0
	}

	override fun update() {
		chunks.forEach {
			val newScale = (.01f*colorRandom.nextFloat())+.995f
			it.scale = listOf(newScale, 1f)
		}
		phase = (phase+1)%PERIOD
	}

	override fun reset() {
		phase = 0
		update()
	}

	override fun draw(engine:GameEngine) {
		for(id in chunks.indices) {
			var col = 1f-BASE_LUMINANCE_OFFSET
			if(id and 2==0) col -= BASE_LUMINANCE_OFFSET
			if(phase>=PERIOD/2&&(id==phase-PERIOD/2||id==1+phase-PERIOD/2||id==-1+phase-PERIOD/2)) {
				col += BASE_LUMINANCE_OFFSET
			}

			// Randomness offset
			col -= (0.025*colorRandom.nextDouble()).toFloat()
			val color = (255*col).toInt()
			val pos = chunks[id].drawLocation
			val ddim = chunks[id].drawDimensions
			val sloc = chunks[id].sourceLocation
			val sdim = chunks[id].sourceDimensions
			customHolder.drawImage(
				imageName, pos[0], pos[1], ddim[0], ddim[1], sloc[0], sloc[1], sdim[0],
				sdim[1], color, color, color, 255
			)
		}
	}
	/**
	 * This last one is important. In the case that any of the child types are used, it allows identification.
	 * The identification can be used to allow casting during operations.
	 *
	 * @return Identification number of child class.
	 */
	override val id:Int = ANIMATION_FAKE_SCANLINES

	companion object {
		private const val AMT = 480/2
		private const val PERIOD = 480 // Frames
		private const val BASE_LUMINANCE_OFFSET = 0.25f
	}

	init {
		imageName = "localBG"
	}
}
