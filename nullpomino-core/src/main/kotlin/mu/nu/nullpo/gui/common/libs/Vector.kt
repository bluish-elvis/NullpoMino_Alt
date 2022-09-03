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
package mu.nu.nullpo.gui.common.libs

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Generate a vector for handling directions.
 * @param sx   X or Magnitude
 * @param sy   Y or Direction
 * @param isDir Use (Mag., Dir) if true, use (X, Y) otherwise.
 */
class Vector @JvmOverloads constructor(sx:Float = 0f, sy:Float = 0f, isDir:Boolean = false) {
	constructor(sx:Int, sy:Int = 0, isDir:Boolean = false):this(sx.toFloat(), sy.toFloat(), isDir)

	/** X-coordinate */
	var x = 0f

	/** Y-coordinate */
	var y = 0f

	var magnitude:Float
		get() = sqrt(x.pow(2f)+y.pow(2f))
		set(value) {
			x = abs(value)*cos(direction)
			if(almostEqual(x, 0f, Float.MIN_VALUE)) x = 0f
			y = abs(value)*sin(direction)
			if(almostEqual(y, 0f, Float.MIN_VALUE)) y = 0f
		}
	var direction:Float
		get() = atan2(y, x).let {if(it<0) it+(2f*PI).toFloat() else it}
		set(value) {
			x = magnitude*cos(value)
			if(almostEqual(x, 0f, Float.MIN_VALUE)) x = 0f
			y = magnitude*sin(value)
			if(almostEqual(y, 0f, Float.MIN_VALUE)) y = 0f
		}

	init {
		if(isDir) {
			// MAGNITUDE AND DIRECTION
			x = abs(sx)*cos(sy)
			if(almostEqual(x, 0f, Float.MIN_VALUE)) x = 0f
			y = abs(sx)*sin(sy)
			if(almostEqual(y, 0f, Float.MIN_VALUE)) y = 0f
		} else {
			// CARTESIAN
			x = sx
			y = sy
			/*magnitude = sqrt(x.pow(2f)+y.pow(2f))
			direction = atan2(y, x)
			if(direction<0) direction += (2f*PI).toFloat()*/
		}
	}

	/** Adds [e] to this vector. */
	operator fun plus(e:Vector):Vector = Vector(x+e.x, y+e.y)
	/** Subtracts [e] from this vector.*/
	operator fun minus(e:Vector):Vector = Vector(x-e.x, y-e.y)
	/** Multiplies the magnitude of this vector by [e].*/
	operator fun times(e:Int):Vector = Vector(magnitude*e, direction, true)
	/** Multiplies the magnitude of this vector by [e].*/
	operator fun times(e:Float):Vector = Vector(magnitude*e, direction, true)
	/** Divides the magnitude of this vector by [e].*/
	operator fun div(e:Int):Vector = Vector(magnitude/e, direction, true)
	/** Divides the magnitude of this vector by [e].*/
	operator fun div(e:Float):Vector = Vector(magnitude/e, direction, true)
	/** Spins this vector by PI radians.*/
	operator fun unaryMinus():Vector = Vector(-x, -y)

	companion object {
		/** Gives a zero vector. For use in blank initialisations.*/
		fun zero():Vector = Vector(0f, 0f)
		/**
		 * Gives a unit vector in a given [direction].
		 *
		 * @param direction Angle of vector.
		 * @return Unit vector at angle 'direction'.
		 */
		fun unitVector(direction:Float):Vector = Vector(1f, direction, true)
		/**
		 * Gets the distance between two vector tails.
		 *
		 * @param a Vector 1
		 * @param b Vector 2
		 * @return A double denoting distance.
		 */
		fun distanceBetween(a:Vector, b:Vector):Float = sqrt(abs(a.x-b.x).pow(2f)+abs(a.y-b.y).pow(2f))
		/**
		 * Returns a vector that represents a vector originating at [a] and ending at [b].
		 * (Starts at [a], points to and ends at [b].)
		 *
		 * @param a Vector 1.
		 * @param b Vector 2.
		 * @return DoubleVector that treats a as origin and b as end.
		 */
		fun directionBetween(a:Vector, b:Vector):Float = (b-a).direction
		// Fuzzy equals.
		private fun almostEqual(a:Float, b:Float, eps:Float):Boolean = abs(a-b)<eps
	}

	override fun toString():String = "($x, $y)"
}
