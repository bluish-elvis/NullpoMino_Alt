/*
 Copyright (c) 2019-2024,
 This library class was created by 0xFC963F18DC21 / Shots243
 It is part of an extension library for the game NullpoMino (copyright 2010-2024)

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis

 Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.

 THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 THIS KOTLIN VERSION WAS NOT MADE IN ASSOCIATION WITH THE LIBRARY CREATOR.

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
import zeroxfc.nullpo.custom.libs.MathHelper.almostEqual
import kotlin.math.PI
import kotlin.math.sin

class BackgroundDiagonalRipple<T>(img:ResourceImage<T>, cellWidth:Int? = DEF_GRID_WIDTH, cellHeight:Int? = DEF_GRID_HEIGHT,
	pulseFrames:Int = 200, var pulseBaseScale:Float = BASE_SCALE, var pulseScaleVariance:Float = SCALE_VARIANCE,
	reverse:Boolean = false, reverseSlant:Boolean = false):AbstractBG<T>(img) {
	private var chunkGrid:List<List<ImageChunk>> = emptyList()
	var reverse = false
	var reverseSlant = false
	var pulseFrame = 0
	private var currentPulsePhase = 0

	init {
		setup(cellWidth, cellHeight, pulseFrames, pulseBaseScale, pulseScaleVariance, reverse, reverseSlant)
	}

	fun modifyValues(pulseFrames:Int, pulseBaseScale:Float?, pulseScaleVariance:Float?, reverse:Boolean, reverseSlant:Boolean) {
		this.reverse = reverse
		this.reverseSlant = reverseSlant
		pulseFrame = pulseFrames
		if(pulseBaseScale!=null) this.pulseBaseScale = pulseBaseScale
		if(pulseScaleVariance!=null) this.pulseScaleVariance = pulseScaleVariance
		if(currentPulsePhase>pulseFrame) currentPulsePhase = pulseFrame
	}

	fun resetPulseScaleValues() {
		pulseBaseScale = BASE_SCALE
		pulseScaleVariance = SCALE_VARIANCE
	}

	private fun setup(cellWidth:Int?, cellHeight:Int?, pulseFrames:Int, pulseBaseScale:Float?, pulseScaleVariance:Float?,
		reverse:Boolean, reverseSlant:Boolean) {
		this.reverse = reverse
		this.reverseSlant = reverseSlant
		pulseFrame = pulseFrames
		currentPulsePhase = pulseFrame
		if(pulseBaseScale==null||pulseScaleVariance==null||cellWidth==null||cellHeight==null) {
			chunkGrid = List(DEF_GRID_HEIGHT) {y ->
				List(DEF_GRID_WIDTH) {x ->
					ImageChunk(
						AnchorPoint.MM, listOf(
							DEF_FIELD_DIM*x+DEF_FIELD_DIM/2, DEF_FIELD_DIM*y+DEF_FIELD_DIM/2
						), listOf(
							DEF_FIELD_DIM*x, DEF_FIELD_DIM*y
						), listOf(DEF_FIELD_DIM, DEF_FIELD_DIM), listOf(BASE_SCALE, BASE_SCALE)
					)
				}
			}
		} else {
			this.pulseBaseScale = pulseBaseScale
			this.pulseScaleVariance = pulseScaleVariance
			val w:Int = if(640%cellWidth!=0) 8 else 640/cellWidth
			val h:Int = if(480%cellHeight!=0) 8 else 480/cellHeight
			chunkGrid = List(h) {y ->
				List(w) {x ->
					ImageChunk(
						AnchorPoint.MM,
						listOf(cellWidth*x+cellWidth/2, cellHeight*y+cellHeight/2),
						listOf(cellWidth*x, cellHeight*y), listOf(cellWidth, cellHeight), listOf(pulseBaseScale, pulseBaseScale)
					)
				}
			}
		}
	}

	override fun update() {
		currentPulsePhase = (currentPulsePhase+1)%pulseFrame
		for(y in chunkGrid.indices) {
			for(x in 0..<chunkGrid[y].size) {
				var j = currentPulsePhase
				if(reverse) j = pulseFrame-currentPulsePhase-1
				if(reverseSlant) j -= x+y else j += x+y
				var ppu = j%pulseFrame
				if(ppu<0) ppu = pulseFrame-ppu
				val baseScale = pulseBaseScale
				val scaleVariance = pulseScaleVariance
				val newScale = minOf(1.0, baseScale+sin(TWO_PI*ppu.toDouble()/pulseFrame)*scaleVariance)
				chunkGrid[y][x].scale = listOf(newScale.toFloat(), newScale.toFloat())
			}
		}
	}

	override fun reset() {
		currentPulsePhase = pulseFrame
		update()
	}

	override fun draw(render:AbstractRenderer) {
		val priorityList = chunkGrid.flatten().sortedBy {it.scale[0]}.toMutableList()
		val baseScale = pulseBaseScale
		if(almostEqual(baseScale.toDouble(), 1.0, 0.005)) {
			img.draw()
			priorityList.removeAll {almostEqual(it.scale[0].toDouble(), 1.0, 0.005)}
		}
		priorityList.forEach {i ->
			val pos = i.drawLocation
			val ddim = i.drawDimensions
			val sloc = i.sourceLocation
			val sdim = i.sourceDimensions
			img.draw(pos[0], pos[1], ddim[0], ddim[1], sloc[0], sloc[1], sdim[0], sdim[1])
		}
	}

	companion object {
		private const val TWO_PI = PI*2
		private const val DEF_FIELD_DIM = 8
		private const val DEF_GRID_WIDTH = 640/DEF_FIELD_DIM
		private const val DEF_GRID_HEIGHT = 480/DEF_FIELD_DIM
		private const val BASE_SCALE = 1f
		private const val SCALE_VARIANCE = 1f
	}

}
