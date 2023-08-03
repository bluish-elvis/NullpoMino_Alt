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

class FlyInOutText(  // String to draw
	private val mainString:String, destinationX:Int, destinationY:Int,   // Timings
	private val flyInTime:Int, private val persistTime:Int,
	private val flyOutTime:Int,   // Colors: idx 0 - main, any other - shadows
	private val textColors:Array<EventReceiver.COLOR>, scale:Float, seed:Long, flashOnLand:Boolean) {
	// Vector array of positions
	private val letterPositions:Array<Array<Vector>>
	// Start location
	private val startLocation:Array<Array<Vector>>
	// Vector array of velocities
	// private FloatVector[] letterVelocities;
	// Destination vector
	private val destinationLocation:Array<Array<Vector>>
	// Shadow count
	private val shadowCount:Int = textColors.size
	// Randomizer for start pos
	private val positionRandomizer:Random = Random(seed)
	// Text scale
	private val textScale:Float = scale
	// Should it flash?
	private val flash:Boolean = flashOnLand
	// Lifetime variable
	private var currentLifetime = 0
	fun draw(receiver:EventReceiver) {
		for(i in letterPositions.indices.reversed()) {
			for(j in 0..<letterPositions[i].size) {
				receiver.drawDirectFont(
					letterPositions[i][j].x.toInt(), letterPositions[i][j].y.toInt(), "${mainString[j]}",
					if((currentLifetime-i)/4%2==0&&flash) EventReceiver.COLOR.WHITE else textColors[i], textScale
				)
			}
		}
	}

	fun update() {
		for(i in letterPositions.indices) {
			if(currentLifetime-i in 0..<flyInTime) {
				for(j in 0..<letterPositions[i].size) {
					val v1 = Interpolation.lerp(
						startLocation[i][j].x, destinationLocation[i][j].x,
						(currentLifetime-i).toFloat()/flyInTime
					).toInt()
					val v2 = Interpolation.lerp(
						startLocation[i][j].y, destinationLocation[i][j].y,
						(currentLifetime-i).toFloat()/flyInTime
					).toInt()
					letterPositions[i][j] = Vector(v1.toFloat(), v2.toFloat(), false)
				}
			} else if(currentLifetime-i>=flyInTime+persistTime) {
				for(j in 0..<letterPositions[i].size) {
					val v1 = Interpolation.lerp(
						destinationLocation[i][j].x, startLocation[i][j].x,
						(currentLifetime-i-flyInTime-persistTime).toFloat()/flyOutTime
					).toInt()
					val v2 = Interpolation.lerp(
						destinationLocation[i][j].y, startLocation[i][j].y,
						(currentLifetime-i-flyInTime-persistTime).toFloat()/flyOutTime
					).toInt()
					letterPositions[i][j] = Vector(v1.toFloat(), v2.toFloat(), false)
				}
			} else if(currentLifetime-i==flyInTime) {
				for(j in 0..<letterPositions[i].size) {
					letterPositions[i][j] = Vector(
						destinationLocation[i][j].x, destinationLocation[i][j].y, false
					)
				}
			}
		}
		currentLifetime++
	}
	/**
	 * Tells a parent method whether to null this object.
	 *
	 * @return A boolean that tells parent method to purge or not.
	 */
	fun shouldPurge():Boolean = currentLifetime>=flyInTime+persistTime+flyOutTime+shadowCount+1

	init {
		// Independent vars.
		// destinationLocation = new FloatVector(destinationX, destinationY, false);

		// Dependent vars.

		// letterVelocities = new FloatVector[mainString.length()];
		var sMod = 16
		if(scale==2.0f) sMod = 32
		if(scale==0.5f) sMod = 16
		val position:List<Pair<Vector, Vector>> = mainString.indices.map {i ->
			var startX = 0
			var startY = 0
			var position:Vector = Vector.zero()
			// float distanceX = 0, distanceY = 0;
			// FloatVector velocity = FloatVector.zero();
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
			return@map Vector(startX.toFloat(), startY.toFloat(), false) to Vector(
				(destinationX+sMod*i).toFloat(),
				destinationY.toFloat(),
				false
			)
			// distanceX = (destinationLocation.getX() + (i * sMod)) - position.getX();
			// distanceY = destinationLocation.getY() - position.getY();
			// velocity = new FloatVector(distanceX / flyInTime, distanceY / flyInTime, false);
			// letterVelocities[i] = velocity;
		}
		letterPositions = Array(shadowCount) {j -> Array(mainString.length) {i -> position[i].first}}
		startLocation = letterPositions.clone()
		destinationLocation = Array(shadowCount) {Array(mainString.length) {i -> position[i].second}}
	}
}
