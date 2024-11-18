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
package zeroxfc.nullpo.custom.libs

import kotlin.math.abs
import kotlin.math.sqrt

object MathHelper {
	/**
	 * Checks if a coordinate is within a certain radius.
	 *
	 * @param x      X-coordinate of circle's center.
	 * @param y      Y-coordinate of circle's center.
	 * @param xTest  X-coordinate of test square.
	 * @param yTest  Y-coordinate of test square.
	 * @param radius The testing radius
	 * @return The result of the check. true: within. false: not within.
	 */
	fun isCoordWithinRadius(x:Int, y:Int, xTest:Int, yTest:Int, radius:Double):Boolean =
		distanceBetween(x, y, xTest, yTest)<=radius
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

	/**
	 * Modulo operator that functions similarly to Python's % operator.
	 *
	 * @param divisor Divisor
	 * @return Remainder after division
	 */
	@JvmName("pythonModuloInt")
	fun Int.pythonModulo(divisor:Int):Int = (this%divisor).let {
		if(it<0) it+divisor else it
	}
	@Deprecated("Int extended", ReplaceWith("value.pythonModulo(divisor)"))
	fun pythonModulo(value:Int, divisor:Int):Int = value.pythonModulo(divisor)
	/**
	 * Modulo operator that functions similarly to Python's % operator.
	 *
	 * @param divisor Divisor
	 * @return Remainder after division
	 */
	@JvmName("pythonModuloLong")
	fun Long.pythonModulo(divisor:Long) = (this%divisor).let {
		if(it<0) it+divisor else it
	}
	@Deprecated("Long extended", ReplaceWith("value.pythonModulo(divisor)"))
	fun pythonModulo(value:Long, divisor:Long) = value.pythonModulo(divisor)
	/**
	 * Gets the greatest common divisor between [a] and [b].
	 * Recursive function.
	 */
	fun gcd(a:Int, b:Int):Int = if(a==0) b else gcd(b%a, a)
	/**
	 * Gets the lowest common multiple between two integers.<br></br>
	 * Calls `gcd(a, b)`, a recursive function.
	 *
	 * @param a int
	 * @param b int
	 * @return LCM of the two integers
	 */
	fun lcm(a:Int, b:Int):Int = a*b/gcd(a, b)

	@Deprecated("Double extended", ReplaceWith("a.almostEqual(b, eps)"))
	fun almostEqual(a:Double, b:Double, eps:Double):Boolean = a.almostEqual(b, eps)
	/**
	 * Is [this] almost equal to [b]?
	 *
	 * @param eps Exclusive maximum difference
	 * @return Is the difference <= eps?
	 */
	@JvmName("almostEqualDouble")
	fun Double.almostEqual(b:Double, eps:Double):Boolean = abs(this-b)<eps

	@Deprecated("Float extended", ReplaceWith("a.almostEqual(b, eps)"))
	fun almostEqual(a:Float, b:Float, eps:Float):Boolean = a.almostEqual(b, eps)
	/**
	 * Is [this] almost equal to [b]?
	 *
	 * @param eps Exclusive maximum difference
	 * @return Is the difference <= eps?
	 */
	@JvmName("almostEqualFloat")
	fun Float.almostEqual(b:Float, eps:Float):Boolean = abs(this-b)<eps
}
