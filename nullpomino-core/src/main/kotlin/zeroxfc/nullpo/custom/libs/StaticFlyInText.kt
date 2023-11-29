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
package zeroxfc.nullpo.custom.libs

import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.gui.common.libs.Vector
import kotlin.random.Random

/**
 * Create a text object that has its letters fly in from various points on the edges and stays in one spot.
 *
 * @param mainString   String to draw.
 * @param destinationX X-position of pixel of destination.
 * @param destinationY Y-position of pixel of destination.
 * @param flyInTime    Frames to fly in.
 * @param textColor    Text color.
 * @param textScale     Text scale (0.5f, 1.0f, 2.0f).
 * @param seed         Random seed for position.
 */
class StaticFlyInText(private val mainString:String  // String to draw
	, destinationX:Int, destinationY:Int, private val flyInTime:Int   // Timings
	, private var textColor:EventReceiver.COLOR // Color
	, private val textScale:Float// Text scale
	, seed:Long) {
	// Vector array of positions
	private var letterPositions:MutableList<Vector> = mutableListOf()
	// Start location
	private var startLocation:MutableList<Vector> = mutableListOf()
	// Destination vector
	private var destinationLocation:MutableList<Vector> = mutableListOf()
	// Randomizer for start pos
	private val positionRandomizer:Random = Random(seed)
	// Lifetime variable
	private var currentLifetime = 0

	init {
		var sMod = 16
		if(textScale==2.0f) sMod = 32
		if(textScale==0.5f) sMod = 16
		val position:List<Pair<Vector, Vector>> = (mainString.indices).map {i ->
			var startX = 0
			var startY = 0
			var position:Vector = Vector.zero()
			val dec1 = positionRandomizer.nextFloat()
			val dec2 = positionRandomizer.nextFloat()
			if(dec1<.5f) {
				startX = -sMod
				if(dec2<.5f) startX = 41*sMod
				startY = (positionRandomizer.nextFloat()*(32*sMod)).toInt()-sMod
			} else {
				startY = -sMod
				if(dec2<.5f) startY = 31*sMod
				startX = (positionRandomizer.nextFloat()*(42*sMod)).toInt()-sMod
			}
			return@map Vector(startX, startY) to Vector((destinationX+sMod*i), destinationY)
		}
		position.mapTo(letterPositions) {it.first}
		position.mapTo(startLocation) {it.first}
		position.mapTo(destinationLocation) {it.second}
	}
	/** Updates the position and lifetime of this object.*/
	fun update() {
		if(currentLifetime<flyInTime) {
			currentLifetime++
			for(i in letterPositions.indices) {
				letterPositions[i] = Vector(
					Interpolation.lerp(startLocation[i].x, destinationLocation[i].x, currentLifetime.toFloat()/flyInTime),
					Interpolation.lerp(startLocation[i].y, destinationLocation[i].y, currentLifetime.toFloat()/flyInTime)
				)
			}
		}
	}
	/** Draws the text at its current position.*/
	fun draw(receiver:EventReceiver) {
		for(j in letterPositions.indices) {
			receiver.drawDirectFont(
				letterPositions[j].x.toInt(), letterPositions[j].y.toInt(), "${mainString[j]}",
				textColor, textScale
			)
		}
	}

}
