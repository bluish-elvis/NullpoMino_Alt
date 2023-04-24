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

package zeroxfc.nullpo.custom.libs

import kotlin.math.PI
import kotlin.math.sin

object Interpolation {
	/**
	 * Linear interpolation between two `int` values.
	 *
	 * @param x      Start point
	 * @param y      End point
	 * @param lerpVal Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `int`
	 */
	fun lerp(x:Int, y:Int, lerpVal:Float):Int = (x+(y-x)*lerpVal).toInt()
	fun lerp(x:Int, y:Int, lerpVal:Double):Int = (x+(y-x)*lerpVal).toInt()
	/**
	 * Linear interpolation between two `double` values.
	 *
	 * @param x      Start point
	 * @param y      End point
	 * @param lerpVal Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `double`
	 */
	fun lerp(x:Double, y:Double, lerpVal:Double):Double = (x+(y-x)*lerpVal)
	/**
	 * Linear interpolation between two `float` values.
	 *
	 * @param x      Start point
	 * @param y      End point
	 * @param lerpVal Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `float`
	 */
	fun lerp(x:Float, y:Float, lerpVal:Float):Float = (x+(y-x)*lerpVal)
	/**
	 * Linear interpolation between two `long` values.
	 *
	 * @param x      Start point
	 * @param y      End point
	 * @param lerpVal Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `long`
	 */
	fun lerp(x:Long, y:Long, lerpVal:Double):Long = (x+(y-x)*lerpVal).toLong()

	/**
	 * One dimensional Bézier interpolation
	 *
	 * @param points Control points for the interpolation (first = start point, last = end point)
	 * @param t      Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `float`
	 */
	private fun bezier1DInterp(points:FloatArray, t:Float):Float = when {
		points.isEmpty() -> 0f
		points.size==1 -> points[0] // Only one point, return it.
		else -> {
			val np1 = FloatArray(points.size-1) {points[it]} // Get all points except last
			val np2 = FloatArray(points.size-1) {points[it+1]}// Get all points except first
			val ny = bezier1DInterp(np1, t)
			val nv2 = bezier1DInterp(np2, t) // Recursive call
			val nt = 1f-t // Inverse value
			nt*ny+t*nv2
		}
	}
	/**
	 * Two-dimensional Bézier interpolation, requires a value pair within each
	 *
	 * @param points Control points for the interpolation (first = start point, last = end point)
	 * @param t      Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `float[]`
	 */
	private fun bezier2DInterp(points:Array<FloatArray>, t:Float):FloatArray = when {
		points.isEmpty() -> floatArrayOf(0f, 0f)
		points.size==1 -> points[0]

		else -> {
			val np1 = Array(points.size-1) {points[it]}
			val np2 = Array(points.size-1) {points[it+1]}
			val ny = bezier2DInterp(np1, t)
			val nv2 = bezier2DInterp(np2, t)
			val nt = 1f-t
			floatArrayOf(nt*ny[0]+t*nv2[0], nt*ny[1]+t*nv2[1])
		}
	}
	/**
	 * One dimensional Bézier interpolation
	 *
	 * @param points Control points for the interpolation (first = start point, last = end point)
	 * @param t      Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `double`
	 */
	private fun bezier1DInterp(points:DoubleArray, t:Double):Double = when {
		points.isEmpty() -> 0.0
		points.size==1 -> points[0] // Only one point, return it.
		else -> {
			val np1 = DoubleArray(points.size-1) {points[it]} // Get all points except last
			val np2 = DoubleArray(points.size-1) {points[it+1]}// Get all points except first
			val ny = bezier1DInterp(np1, t)
			val nv2 = bezier1DInterp(np2, t) // Recursive call
			val nt = 1.0-t // Inverse value
			nt*ny+t*nv2
		}
	}
	/**
	 * Two-dimensional Bézier interpolation, requires a value pair within each
	 *
	 * @param points Control points for the interpolation (first = start point, last = end point)
	 * @param t      Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `double[]`
	 */
	private fun bezier2DInterp(points:Array<DoubleArray>, t:Double):DoubleArray = when {
		points.isEmpty() -> doubleArrayOf(0.0, 0.0)
		points.size==1 -> points[0]

		else -> {
			val np1 = Array(points.size-1) {points[it]}
			val np2 = Array(points.size-1) {points[it+1]}
			val ny = bezier2DInterp(np1, t)
			val nv2 = bezier2DInterp(np2, t)
			val nt = 1.0-t
			doubleArrayOf(nt*ny[0]+t*nv2[0], nt*ny[1]+t*nv2[1])
		}
	}

	/**
	 * N-dimensional Bézier interpolation, requires a value set within each.<br></br>
	 * Each value set must contain the same number of values.
	 *
	 * @param points Control points for the interpolation (first = start point, last = end point)
	 * @param t      Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `double[]`
	 */
	private fun bezierNDInterp(points:Array<DoubleArray>, t:Double):DoubleArray = when {
		points.isEmpty() -> doubleArrayOf(0.0, 0.0)
		points.size==1 -> points[0]
		else -> {
			val np1 = Array(points.size-1) {points[it]}
			val np2 = Array(points.size-1) {points[it+1]}
			val ny = bezierNDInterp(np1, t)
			val nv2 = bezierNDInterp(np2, t)
			val nt = 1.0-t
			DoubleArray(ny.size) {nt*ny[it]+t*nv2[it]}
		}
	}
	/**
	 * Smooth curve interpolation of two `float` values.
	 *
	 * @param x          Start point
	 * @param y          End point
	 * @param denominator Step ease scale (denominator > 2 where smaller = closer to linear)
	 * @param interpVal   Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `Float`
	 */
	fun smoothStep(x:Float, y:Float, interpVal:Float, denominator:Float = 6f):Float {
		val den = maxOf(1f, denominator)
		val diff = y-x
		val p1 = diff*(1f/den)
		val p2 = diff-p1

		// log.debug(Arrays.toString(new double[] { x, x + p1, x + p2, y }));
		// log.debug(Arrays.toString(new double[] { p1, p2}));
		// log.debug(lerpVal);
		return bezier1DInterp(floatArrayOf(x, x+p1, x+p2, y), interpVal)
	}
	/**
	 * Smooth curve interpolation of two `double` values.
	 *
	 * @param x          Start point
	 * @param y          End point
	 * @param denominator Step ease scale (denominator > 2 where smaller = closer to linear)
	 * @param interpVal   Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `double`
	 */
	fun smoothStep(x:Double, y:Double, interpVal:Double, denominator:Double = 6.0):Double {
		val den = maxOf(1.0, denominator)
		val diff = y-x
		val p1 = diff*(1.0/den)
		val p2 = diff-p1

		// log.debug(Arrays.toString(new double[] { x, x + p1, x + p2, y }));
		// log.debug(Arrays.toString(new double[] { p1, p2}));
		// log.debug(lerpVal);
		return bezier1DInterp(doubleArrayOf(x, x+p1, x+p2, y), interpVal)
	}
	/**
	 * Sine interpolation of two `float` values.
	 *
	 * @param x        Start point
	 * @param y        End point
	 * @param interpVal Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `float`
	 */
	fun sineStep(x:Float, y:Float, interpVal:Float):Float {
		val ofs = (PI/2f).toFloat()
		val t = (sin(-1f*ofs+interpVal*ofs*2)+1f)/2f
		return (1f-t)*x+y*t
	}
	/**
	 * Sine interpolation of two `double` values.
	 *
	 * @param x        Start point
	 * @param y        End point
	 * @param interpVal Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `double`
	 */
	fun sineStep(x:Double, y:Double, interpVal:Double):Double {
		val ofs = PI/2.0
		val t = (sin(-1.0*ofs+interpVal*ofs*2)+1.0)/2.0
		return (1.0-t)*x+y*t
	}
}
