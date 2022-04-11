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

class BackgroundFrameAnim(filePath:String, type:Int, frameTime:Int, pingPong:Boolean):AnimatedBackgroundHook() {
	private val type:Int
	private var chunkSequence:Array<ImageChunk> = emptyArray()
	// private ResourceHolderCustomAssetExtension customHolder;
	private var frameTime:Int
	private var currentTick = 0
	private var frameCount = 0
	private var currentFrame = 0
	private var pingPong:Boolean
	private var forward = false
	private fun setup() {
		forward = true
		currentFrame = 0
		currentTick = 0
		when(type) {
			SEQUENCE_LINEAR_HORIZONTAL -> {
				val hDim = customHolder.getImageDimensions(imageName)
				val hAmount = hDim[0]/640
				chunkSequence = Array(hAmount) {i ->
					ImageChunk(ImageChunk.ANCHOR_POINT_TL, intArrayOf(0, 0), intArrayOf(i*640, 0),
						intArrayOf(640, 480), floatArrayOf(1f, 1f))

				}
				frameCount = hAmount
			}
			SEQUENCE_LINEAR_VERTICAL -> {
				val vDim = customHolder.getImageDimensions(imageName)
				val vAmount = vDim[1]/480
				chunkSequence = Array(vAmount) {i ->
					ImageChunk(ImageChunk.ANCHOR_POINT_TL, intArrayOf(0, 0), intArrayOf(0, i*480),
						intArrayOf(640, 480), floatArrayOf(1f, 1f))

				}
				frameCount = vAmount
			}
			SEQUENCE_GRID_HFTV -> {
				val gDim1 = customHolder.getImageDimensions(imageName)
				val hCells1 = gDim1[0]/640
				val vCells1 = gDim1[1]/480
				chunkSequence = Array(vCells1*hCells1) {i ->
					val x = i%hCells1
					ImageChunk(ImageChunk.ANCHOR_POINT_TL, intArrayOf(0, 0), intArrayOf(640*x, 480*x),
						intArrayOf(640, 480), floatArrayOf(1f, 1f))
				}
				frameCount = hCells1*vCells1
			}
			SEQUENCE_GRID_VFTH -> {
				val gDim2 = customHolder.getImageDimensions(imageName)
				val hCells2 = gDim2[0]/640
				val vCells2 = gDim2[1]/480
				chunkSequence = Array(vCells2*hCells2) {i ->
					val x = i%vCells2
					ImageChunk(ImageChunk.ANCHOR_POINT_TL, intArrayOf(0, 0), intArrayOf(640*x, 480*x),
						intArrayOf(640, 480), floatArrayOf(1f, 1f))
				}
				frameCount = hCells2*vCells2
			}
			else -> {
			}
		}
	}

	fun setSpeed(frameTime:Int) {
		this.frameTime = frameTime
		reset()
	}

	fun setPingPong(pingPong:Boolean) {
		this.pingPong = pingPong
		reset()
	}

	override fun update() {
		currentTick++
		if(currentTick>=frameTime) {
			currentTick = 0
			if(pingPong) {
				if(forward) currentFrame++ else currentFrame--
				if(currentFrame>=frameCount) {
					currentFrame -= 2
					forward = false
				} else if(currentFrame<0) {
					currentFrame++
					forward = true
				}
			} else {
				currentFrame = (currentFrame+1)%frameCount
			}
		}
	}

	override fun reset() {
		forward = true
		currentFrame = 0
		currentTick = 0
	}

	override fun draw(engine:GameEngine, playerID:Int) {
		val i = chunkSequence[currentFrame]
		val pos = i.drawLocation
		val ddim = i.drawDimensions
		val sloc = i.sourceLocation
		val sdim = i.sourceDimensions
		customHolder.drawImage(imageName, pos[0], pos[1], ddim[0], ddim[1], sloc[0], sloc[1], sdim[0],
			sdim[1], 255, 255, 255, 255)
	}

	override fun setBG(bg:Int) {
		log.warn("Frame animation backgrounds do not support in-game backgrounds.")
	}

	override fun setBG(filePath:String) {
		customHolder.loadImage(filePath, imageName)
		log.debug("Custom frame animation background modified (New File Path: $filePath).")
		setup()
	}
	/**
	 * Allows the hot-swapping of pre-loaded BGs from a storage instance of a `ResourceHolderCustomAssetExtension`.
	 *
	 * @param holder Storage instance
	 * @param name   Image name
	 */
	override fun setBGFromHolder(holder:ResourceHolderCustomAssetExtension, name:String) {
		customHolder.putImageAt(holder.getImageAt(name), imageName)
		log.debug("Custom frame animation background modified (New Image Reference: $name).")
		setup()
	}
	/**
	 * This last one is important. In the case that any of the child types are used, it allows identification.
	 * The identification can be used to allow casting during operations.
	 *
	 * @return Identification number of child class.
	 */
	override val id = ANIMATION_FRAME_ANIM

	companion object {
		const val SEQUENCE_LINEAR_HORIZONTAL = 0
		const val SEQUENCE_LINEAR_VERTICAL = 1
		const val SEQUENCE_GRID_HFTV = 2
		const val SEQUENCE_GRID_VFTH = 3
	}

	init {
		imageName = ("localBG")
	}

	init {
		customHolder = ResourceHolderCustomAssetExtension()
		customHolder.loadImage(filePath, imageName)
		this.type = type
		this.frameTime = frameTime
		this.pingPong = pingPong
		setup()
		log.debug("Type $type frame animation background created (File Path: $filePath).")
	}
}