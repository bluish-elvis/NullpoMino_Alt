/*
 Copyright (c) 2021-2023,
 This library class was created by 0xFC963F18DC21 / Shots243
 It is part of an extension library for the game NullpoMino (copyright 2010-2023)

 Kotlin converted and modified by Venom=Nhelv

 Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.

 THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

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

package zeroxfc.nullpo.custom.libs.backgroundtypes

import mu.nu.nullpo.game.play.GameEngine

class BackgroundImageSequenceAnim(filePaths:Array<String>, frameTime:Int, pingPong:Boolean):AnimatedBackgroundHook() {
	private val frameTime:Int
	private val frameCount:Int
	private val pingPong:Boolean
	private var currentTick = 0
	private var currentFrame = 0
	private var forward = false
	private fun setup() {
		forward = true
		currentFrame = 0
		currentTick = 0
	}
	/**
	 * Performs an update tick on the background. Advisably used in onLast.
	 */
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
		setup()
	}

	override fun draw(engine:GameEngine) {
		val i = "frame$currentFrame"
		customHolder.drawImage(i, 0, 0, 640, 480, 0, 0, 640, 480, 255, 255, 255, 255)
	}

	override fun setBG(bg:Int) {
		log.warn(
			"Image Sequence animation backgrounds do not support this operation. Please create a new instance."
		)
	}
	/**
	 * Change BG to a custom BG using its file path.
	 *
	 * @param filePath File path of new background
	 */
	override fun setBG(filePath:String) {
		log.warn(
			"Image Sequence animation backgrounds do not support this operation. Please create a new instance."
		)
	}
	/**
	 * Allows the hot-swapping of preloaded BGs from a storage instance of a `ResourceHolderCustomAssetExtension`.
	 *
	 * @param holder Storage instance
	 * @param name   Image name
	 */
	override fun setBGFromHolder(holder:ResourceHolderCustomAssetExtension, name:String) {
		log.warn(
			"Image Sequence animation backgrounds do not support this operation. Please create a new instance."
		)
	}
	/**
	 * This last one is important. In the case that any of the child types are used, it allows identification.
	 * The identification can be used to allow casting during operations.
	 *
	 * @return Identification number of child class.
	 */
	override val id = ANIMATION_IMAGE_SEQUENCE_ANIM

	init {
		customHolder = ResourceHolderCustomAssetExtension()
		for(i in filePaths.indices) {
			customHolder.loadImage(filePaths[i], "frame$i")
			val dim = customHolder.getImageDimensions("frame$i")
			if(dim[0]!=640||dim[1]!=480) log.warn(
				"Image at "+filePaths[i]+" is not 640x480. It may not render correctly."
			)
		}
		this.frameTime = frameTime
		this.pingPong = pingPong
		frameCount = filePaths.size
		setup()
		log.debug("Sequence frame animation background created (Frames: "+filePaths.size+").")
	}
}
