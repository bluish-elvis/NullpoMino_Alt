/*
 Copyright (c) 2023,
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

import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.ResourceImage
import mu.nu.nullpo.gui.common.bg.AbstractBG
import zeroxfc.nullpo.custom.libs.AnchorPoint

class BackgroundFrameAnim<T>(img:ResourceImage<T>, private val type:Int, frameTime:Int, pingPong:Boolean):AbstractBG<T>(img) {
	private var chunkSequence:Array<ImageChunk> = emptyArray()
	// private ResourceHolderCustomAssetExtension customHolder;
	var frameTime:Int = frameTime
		set(value) {
			field = value
			reset()
		}
	private var currentTick = 0
	private var frameCount = 0
	private var currentFrame = 0
	var pingPong:Boolean = pingPong
		set(value) {
			field = value
			reset()
		}
	private var forward = false

	init {
		setup()
	}

	private fun setup() {
		forward = true
		currentFrame = 0
		currentTick = 0
		val dim = listOf(img.width, img.height)
		when(type) {
			SEQUENCE_LINEAR_HORIZONTAL -> {
				val hAmount = dim[0]/640
				chunkSequence = Array(hAmount) {i ->
					ImageChunk(
						AnchorPoint.TL, listOf(0, 0), listOf(i*640, 0),
						listOf(640, 480), listOf(1f, 1f)
					)
				}
				frameCount = hAmount
			}
			SEQUENCE_LINEAR_VERTICAL -> {
				val vAmount = dim[1]/480
				chunkSequence = Array(vAmount) {i ->
					ImageChunk(
						AnchorPoint.TL, listOf(0, 0), listOf(0, i*480),
						listOf(640, 480), listOf(1f, 1f)
					)
				}
				frameCount = vAmount
			}
			SEQUENCE_GRID_HFTV -> {
				val hCells1 = dim[0]/640
				val vCells1 = dim[1]/480
				chunkSequence = Array(vCells1*hCells1) {i ->
					val x = i%hCells1
					ImageChunk(
						AnchorPoint.TL, listOf(0, 0), listOf(640*x, 480*x),
						listOf(640, 480), listOf(1f, 1f)
					)
				}
				frameCount = hCells1*vCells1
			}
			SEQUENCE_GRID_VFTH -> {
				val hCells2 = dim[0]/640
				val vCells2 = dim[1]/480
				chunkSequence = Array(vCells2*hCells2) {i ->
					val x = i%vCells2
					ImageChunk(
						AnchorPoint.TL, listOf(0, 0), listOf(640*x, 480*x),
						listOf(640, 480), listOf(1f, 1f)
					)
				}
				frameCount = hCells2*vCells2
			}
			else -> {
			}
		}
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

	override fun draw(render:AbstractRenderer) {
		val i = chunkSequence[currentFrame]
		val pos = i.drawLocation
		val ddim = i.drawDimensions
		val sloc = i.sourceLocation
		val sdim = i.sourceDimensions
		img.draw(pos[0], pos[1], ddim[0], ddim[1], sloc[0], sloc[1], sdim[0], sdim[1])
	}

	companion object {
		const val SEQUENCE_LINEAR_HORIZONTAL = 0
		const val SEQUENCE_LINEAR_VERTICAL = 1
		const val SEQUENCE_GRID_HFTV = 2
		const val SEQUENCE_GRID_VFTH = 3
	}

}
