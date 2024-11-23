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

import mu.nu.nullpo.game.component.Block.COLOR
import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.ResourceImage
import mu.nu.nullpo.gui.common.bg.AbstractBG
import zeroxfc.nullpo.custom.libs.AnchorPoint
import kotlin.math.abs
import kotlin.random.Random

class BackgroundSlidingTiles(private val custom:ResourceImage<*>?, directionRandomizer:Random = Random.Default):
	AbstractBG<Nothing?>(ResourceImage.ResourceImageBlank) {
	private var darkness:Float = 0f
	private var directionRandomizer:Random = directionRandomizer
		set(value) {
			field = value
			reset()
		}

	private var gridChunks:Array<Array<ImageChunk>> = emptyArray()
	private var colors:Array<Array<COLOR>> = emptyArray()

	private var skin:Int = 0
	private var size = 0
	private var color:COLOR? = null

	private var direction = 0
	private var horizontal = true
	private var currentMovement = 0
	private var width = 0
	private var height = 0
	private var move = false

	constructor(_skin:Int, directionRandomizer:Random = Random, _color:Int = 0, _size:Int = 1, _darkness:Float = 0f):this(
		null,
		directionRandomizer
	) {
		skin = _skin
		color = COLOR.all[_color]
		size = _size
		darkness = _darkness
	}

	init {
		setup()
	}

	private fun setup() {
//		customHolder.loadImage("res/graphics/blank_black_24b.png", "blackBG")
		direction = directionRandomizer.nextInt(DIRECTIONS)
		if(custom!=null) {
			val dim = listOf(custom.width, custom.height)
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
						AnchorPoint.TL, listOf((x-1)*dim[0], (y-1)*dim[1]),
						listOf(0, 0), listOf(dim[0], dim[1]), listOf(1f, 1f)
					)
				}
			}
		} else {
			val s = if(size<0) 8 else if(size>0) 32 else 16
			width = s
			height = s
			gridChunks = Array(480/s+2) {y ->
				Array(640/s+2) {x ->
					ImageChunk(
						AnchorPoint.TL, listOf((x-1)*s, (y-1)*s), listOf(0, 0),
						listOf(s, s), listOf(1f, 1f)
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
					for(x in 0..<gridChunks[y].size) {
						val locOld = gridChunks[y][x].anchorLocation
						val yMod = abs(locOld[1]/width)
						val dir = (direction+yMod)%DIRECTIONS
						var xNew:Int
						when(dir) {
							DIRECTION_LEFT -> {
								xNew = locOld[0]-1
								if(xNew<=width*-2) xNew = (gridChunks[0].size-2)*width
								gridChunks[y][x].anchorLocation = listOf(xNew, locOld[1])
							}
							DIRECTION_RIGHT -> {
								xNew = locOld[0]+1
								if(xNew>=(gridChunks[0].size-1)*width) xNew = width*-1
								gridChunks[y][x].anchorLocation = listOf(xNew, locOld[1])
							}
							else -> {
							}
						}
					}
				}
				currentMovement++
			} else {
				for(x in 0..<gridChunks[0].size) {
					for(y in gridChunks.indices) {
						val locOld = gridChunks[y][x].anchorLocation
						val xMod = abs(locOld[0]/width)
						val dir2 = (direction+xMod)%DIRECTIONS
						var yNew:Int
						when(dir2) {
							DIRECTION_UP -> {
								yNew = locOld[1]-1
								if(yNew<=height*-2) yNew = (gridChunks.size-2)*height
								gridChunks[y][x].anchorLocation = listOf(locOld[0], yNew)
							}
							DIRECTION_DOWN -> {
								yNew = locOld[1]+1
								if(yNew>=(gridChunks.size-1)*height) yNew = height*-1
								gridChunks[y][x].anchorLocation = listOf(locOld[0], yNew)
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

	override fun draw(render:AbstractRenderer) {
		for(y in gridChunks.indices) for(x in 0..<gridChunks[y].size) {
			val i = gridChunks[y][x]
			val pos = i.drawLocation
			val ddim = i.drawDimensions
			val sloc = i.sourceLocation
			val sdim = i.sourceDimensions
			if(custom!=null) custom.draw(pos[0], pos[1], ddim[0], ddim[1], sloc[0], sloc[1], sdim[0], sdim[1]) else {
				var s = 1f
				if(size<0) s = 0.5f
				if(size>0) s = 2f
				render.drawBlock(pos[0], pos[1], colors[y][x].ordinal, skin, false, darkness, 1f, s)
			}
		}
	}

	private fun modifyValues(_color:Int?, _size:Int?, _darkness:Float?) {
		if(custom!=null) return
		if(_color!=null) color = COLOR.all[_color]
		if(_size!=null) size = _size
		if(_darkness!=null) darkness = _darkness
		if(_color!=null||_size!=null) reset()
	}

	companion object {
		private const val DIRECTION_UP = 0
		private const val DIRECTION_RIGHT = 1
		private const val DIRECTION_DOWN = 1
		private const val DIRECTION_LEFT = 0
		private const val DIRECTIONS = 2
	}

}
