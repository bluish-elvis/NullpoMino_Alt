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

import kotlin.math.abs
import kotlin.math.sqrt

class MathHelper {
	/**
	 * Checks if a coordinate is within a certain radius.
	 *
	 * @param x      X-coordinate of circle's centre.
	 * @param y      Y-coordinate of circle's centre.
	 * @param xTest  X-coordinate of test square.
	 * @param yTest  Y-coordinate of test square.
	 * @param radius The testing radius
	 * @return The result of the check. true: within. false: not within.
	 */
	fun isCoordWithinRadius(x:Int, y:Int, xTest:Int, yTest:Int, radius:Double):Boolean {
		val dX = xTest-x
		val dY = yTest-y
		val distance = sqrt((dX*dX+dY*dY).toDouble())
		return distance<=radius
	}
	/**
	 * Gets the direct distance between two coordinate points.
	 *
	 * @param x0 X-coordinate of the first point
	 * @param y0 Y-coordinate of the first point
	 * @param x1 X-coordinate of the second point
	 * @param y1 Y-coordinate of the second point
	 * @return Direct distance between the two points.
	 */
	fun distanceBetween(x0:Int, y0:Int, x1:Int, y1:Int):Double {
		val dX:Int = x1-x0
		val dY:Int = y1-y0
		return sqrt((dX*dX+dY*dY).toDouble())
	}

	companion object {
		/**
		 * Modulo operator that functions similarly to Python's % operator.
		 *
		 * @param value   Number
		 * @param divisor Divisor
		 * @return Remainder after division
		 */
		fun pythonModulo(value:Int, divisor:Int):Int {
			var dividend = value%divisor
			if(dividend<0) dividend += divisor
			return dividend
		}
		/**
		 * Modulo operator that functions similarly to Python's % operator.
		 *
		 * @param value   Number
		 * @param divisor Divisor
		 * @return Remainder after division
		 */
		fun pythonModulo(value:Long, divisor:Long):Long {
			var dividend = value%divisor
			if(dividend<0) dividend += divisor
			return dividend
		}
		/**
		 * Clamps a value to within a range.
		 *
		 * @param value Value to clamp
		 * @param min   Min value
		 * @param max   Max value
		 * @return Clamped value
		 */
		fun clamp(value:Int, min:Int, max:Int):Int {
			return when {
				value in min..max -> value
				value<min -> min
				else -> max
			}
		}
		/**
		 * Clamps a value to within a range.
		 *
		 * @param value Value to clamp
		 * @param min   Min value
		 * @param max   Max value
		 * @return Clamped value
		 */
		fun clamp(value:Long, min:Long, max:Long):Long {
			return when {
				value in min..max -> value
				value<min -> min
				else -> max
			}
		}
		/**
		 * Clamps a value to within a range.
		 *
		 * @param value Value to clamp
		 * @param min   Min value
		 * @param max   Max value
		 * @return Clamped value
		 */
		fun clamp(value:Float, min:Float, max:Float):Float {
			return when {
				value in min..max -> value
				value<min -> min
				else -> max
			}
		}
		/**
		 * Clamps a value to within a range.
		 *
		 * @param value Value to clamp
		 * @param min   Min value
		 * @param max   Max value
		 * @return Clamped value
		 */
		fun clamp(value:Double, min:Double, max:Double):Double {
			return when {
				value in min..max -> value
				value<min -> min
				else -> max
			}
		}
		/**
		 * Gets the greatest common divisor between two integers.<br></br>
		 * Recursive function.
		 *
		 * @param a int
		 * @param b int
		 * @return GCD of the two integers
		 */
		private fun gcd(a:Int, b:Int):Int = if(a==0) b else gcd(b%a, a)
		/**
		 * Gets the lowest common multiple between two integers.<br></br>
		 * Calls `gcd(a, b)`, a recursive function.
		 *
		 * @param a int
		 * @param b int
		 * @return LCM of the two integers
		 */
		fun lcm(a:Int, b:Int):Int = a*b/gcd(a, b)
		/**
		 * Is almost equal to.
		 *
		 * @param a   Value
		 * @param b   Value
		 * @param eps Exclusive maximum difference
		 * @return Is the difference <= eps?
		 */
		fun almostEqual(a:Double, b:Double, eps:Double):Boolean = abs(a-b)<eps
	}
}