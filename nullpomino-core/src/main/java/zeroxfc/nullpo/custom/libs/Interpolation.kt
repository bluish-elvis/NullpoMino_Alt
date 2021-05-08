package zeroxfc.nullpo.custom.libs

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
	fun lerp(x:Int, y:Int, lerpVal:Double):Int = ((1.0-lerpVal)*x).toInt()+(lerpVal*y).toInt()
	/**
	 * Linear interpolation between two `double` values.
	 *
	 * @param x      Start point
	 * @param y      End point
	 * @param lerpVal Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `double`
	 */
	fun lerp(x:Double, y:Double, lerpVal:Double):Double = (1.0-lerpVal)*x+lerpVal*y
	/**
	 * Linear interpolation between two `long` values.
	 *
	 * @param x      Start point
	 * @param y      End point
	 * @param lerpVal Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `long`
	 */
	fun lerp(x:Long, y:Long, lerpVal:Double):Long = ((1.0-lerpVal)*x).toLong()+(lerpVal*y).toLong()
	/**
	 * Linear interpolation between two `float` values.
	 *
	 * @param x      Start point
	 * @param y      End point
	 * @param lerpVal Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `float`
	 */
	fun lerp(x:Float, y:Float, lerpVal:Double):Float = ((1.0-lerpVal)*x).toFloat()+(lerpVal*y).toFloat()
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
	 * Two dimensional Bézier interpolation, requires a value pair within each
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
	 * Smooth curve interpolation of two `double` values.
	 *
	 * @param x          Start point
	 * @param y          End point
	 * @param denominator Step ease scale (denominator > 2 where smaller = closer to linear)
	 * @param interpVal   Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `double`
	 */
	fun smoothStep(x:Double, y:Double, denominator:Double = 6.0, interpVal:Double):Double {
		val den = if(denominator<=2) 6.0 else denominator
		val diff = y-x
		val p1 = diff*(1.0/den)
		val p2 = diff-p1

		// log.debug(Arrays.toString(new double[] { x, x + p1, x + p2, y }));
		// log.debug(Arrays.toString(new double[] { p1, p2}));
		// log.debug(lerpVal);
		return bezier1DInterp(doubleArrayOf(x, x+p1, x+p2, y), interpVal)
	}
	/**
	 * Smooth curve interpolation of two `double` values.
	 *
	 * @param x        Start point
	 * @param y        End point
	 * @param interpVal Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `double`
	 */
	fun smoothStep(x:Double, y:Double, interpVal:Double):Double = smoothStep(x, y, 6.0, interpVal)
	/**
	 * Sine interpolation of two `double` values.
	 *
	 * @param x        Start point
	 * @param y        End point
	 * @param interpVal Proportion of point travelled (0 = start, 1 = end)
	 * @return Interpolated value as `double`
	 */
	fun sineStep(x:Double, y:Double, interpVal:Double):Double {
		val ofs = Math.PI/2.0
		val t = (sin(-1.0*ofs+interpVal*ofs*2)+1.0)/2.0
		return (1.0-t)*x+y*t
	}
}