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
package zeroxfc.nullpo.custom.libs

import kotlin.math.*

class DoubleVector @JvmOverloads constructor(v1:Double = 0.0, v2:Double = 0.0, mode:Boolean = false) {
	constructor(v1:Int = 0, v2:Int = 0, mode:Boolean):this(v1.toDouble(), v2.toDouble(), mode)

	// Fields: X, Y, DIR, MAG
	var x = 0.0
		set(x) {
			field = x
			magnitude = sqrt(this.x.pow(2.0)+y.pow(2.0))
			direction = atan2(y, this.x)
			if(direction<0) direction += 2.0*Math.PI
		}
	var y = 0.0
		set(y) {
			field = y
			magnitude = sqrt(x.pow(2.0)+this.y.pow(2.0))
			direction = atan2(this.y, x)
			if(direction<0) direction += 2.0*Math.PI
		}
	var magnitude = 0.0
		set(value) {
			field = abs(value)
			x = magnitude*cos(direction)
			if(almostEqual(x, 0.0, 1E-8)) x = 0.0
			y = magnitude*sin(direction)
			if(almostEqual(y, 0.0, 1E-8)) y = 0.0
		}

	var direction = 0.0
		set(value) {
			field = value
			x = magnitude*cos(direction)
			if(almostEqual(x, 0.0, 1E-8)) x = 0.0
			y = magnitude*sin(direction)
			if(almostEqual(y, 0.0, 1E-8)) y = 0.0
		}
	/*
     * Misc Methods
     */
	/**
	 * Adds DoubleVector e to this vector.
	 */
	operator fun plus(e:DoubleVector) {
		x += e.x
		y += e.y
	}
	/**
	 * Subtracts DoubleVector e from this vector.
	 */
	operator fun minus(e:DoubleVector) {
		x -= e.x
		y -= e.y
	}
	/**
	 * Multiplies the magnitude of this vector by e.
	 */
	operator fun times(e:Double) {
		magnitude *= e
	}
	/**
	 * Divides the magnitude of this vector by e.
	 */
	operator fun div(e:Double) {
		magnitude /= e
	}
	/*
     * Getters / Setters
     */
	/**
	 * Rotates this vector by PI radians.
	 */
	operator fun unaryMinus() {
		x = -x
		y = -y
	}

	companion object {
		/**
		 * Adds two DoubleVector objects; a and b together.
		 */
		fun plus(a:DoubleVector, b:DoubleVector):DoubleVector = DoubleVector(a.x+b.x, a.y+b.y, false)
		/**
		 * Subtracts DoubleVector b from a.
		 */
		fun minus(a:DoubleVector, b:DoubleVector):DoubleVector = DoubleVector(a.x-b.x, a.y-b.y, false)
		/**
		 * Multiples the magnitude of a by b.
		 */
		fun times(a:DoubleVector, b:Double):DoubleVector = DoubleVector(a.magnitude*b, a.direction, true)
		/**
		 * Divides the magnitude of a by b.
		 */
		fun div(a:DoubleVector, b:Double):DoubleVector = DoubleVector(a.magnitude/b, a.direction, true)
		/**
		 * Negates DoubleVector a; inverts its direction by pi radians while preserving magnitude.
		 */
		fun unaryMinus(a:DoubleVector):DoubleVector = DoubleVector(-a.x, -a.y, false)
		/**
		 * Gives a zero vector. For use in blank initialisations.
		 *
		 * @return A zero vector.
		 */
		fun zero():DoubleVector = DoubleVector(0.0, 0.0, false)
		/**
		 * Gives a unit vector in a given direction.
		 *
		 * @param direction Angle of vector.
		 * @return Unit vector at angle 'direction'.
		 */
		fun unitVector(direction:Double):DoubleVector = DoubleVector(1.0, direction, true)
		/**
		 * Gets the distance between two vector tails.
		 *
		 * @param a Vector 1
		 * @param b Vector 2
		 * @return A double denoting distance.
		 */
		fun distanceBetween(a:DoubleVector, b:DoubleVector):Double = sqrt(abs(a.x-b.x).pow(2.0)+abs(a.y-b.y).pow(2.0))
		/**
		 * Returns a vector that represents a vector originating at a and ending at b.
		 * (Starts at a, points to and ends at b.)
		 *
		 * @param a Vector 1.
		 * @param b Vector 2.
		 * @return DoubleVector that treats a as origin and b as end.
		 */
		fun directionBetween(a:DoubleVector, b:DoubleVector):Double = minus(b, a).direction
		// Fuzzy equals.
		private fun almostEqual(a:Double, b:Double, eps:Double):Boolean = abs(a-b)<eps
	}
	/**
	 * Generate a double-container vector for handling directions.
	 *
	 * @param v1   X or Magnitude
	 * @param v2   Y or Direction
	 * @param mode Use (X, Y) if false, use (Mag., Dir.) otherwise.
	 */
	/**
	 * Gives a zero vector. For use in blank initialisations.
	 */
	init {
		if(mode) {
			// MAGNITUDE AND DIRECTION
			magnitude = abs(v1)
			direction = v2
			x = magnitude*cos(direction)
			if(almostEqual(x, 0.0, 1E-8)) x = 0.0
			y = magnitude*sin(direction)
			if(almostEqual(y, 0.0, 1E-8)) y = 0.0
		} else {
			// CARTESIAN
			x = v1
			y = v2
			magnitude = sqrt(x.pow(2.0)+y.pow(2.0))
			direction = atan2(y, x)
			if(direction<0) direction += 2.0*Math.PI
		}
	}
}