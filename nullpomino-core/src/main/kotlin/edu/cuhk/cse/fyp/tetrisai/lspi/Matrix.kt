package edu.cuhk.cse.fyp.tetrisai.lspi

import kotlin.jvm.JvmStatic
import java.text.DecimalFormat
import java.util.Arrays

object Matrix {
	private val formatter = DecimalFormat("#0.0")
	//preprocessing
	fun arrayToCol(w:DoubleArray, colW:Array<DoubleArray>) {
		for(i in w.indices) colW[i][0] = w[i]
	}

	fun arrayToRow(w:DoubleArray, rowW:Array<DoubleArray>) {
		for(i in w.indices) rowW[0][i] = w[i]
	}

	fun colToArray(colW:Array<DoubleArray>, w:DoubleArray) {
		for(i in w.indices) w[i] = colW[i][0]
	}

	fun copy(a:Array<DoubleArray>, b:Array<DoubleArray>):Array<DoubleArray> {
		for(i in a.indices) System.arraycopy(a[i], 0, b[i], 0, a[i].size)
		return b
	}

	private fun findFirstNonZero(m:Array<DoubleArray>, col:Int):Int {
		for(i in col until m.size) if(m[i][col]!=0.0) return i
		return -1
	}

	private fun swapRow(m:Array<DoubleArray>, r1:Int, r2:Int) {
		if(r1==r2) return
		val t = m[r1]
		m[r1] = m[r2]
		m[r2] = t
	}

	private fun multiplyRow(m:Array<DoubleArray>, r:Int, factor:Double) {
		for(i in m[r].indices) m[r][i] = factor*m[r][i]
	}

	private fun addMultiple(m:Array<DoubleArray>, r1:Int, r2:Int, factor:Double) {
		val arr1 = m[r1]
		val arr2 = m[r2]
		for(i in arr1.indices) arr1[i] = arr1[i]+factor*arr2[i]
	}

	private fun identity(r:Array<DoubleArray>):Array<DoubleArray> {
		for(i in r.indices) {
			Arrays.fill(r[i], 0.0)
			r[i][i] = 1.0
		}
		return r
	}

	@JvmStatic fun main(args:Array<String>) {
		val A = arrayOf(doubleArrayOf(1.0, 2.0, 3.0), doubleArrayOf(2.0, 5.0, 3.0), doubleArrayOf(1.0, 0.0, 8.0))
		val I = Array(3) {DoubleArray(3)}
		val result = Array(3) {DoubleArray(3)}
		val tmp = Array(3) {DoubleArray(3)}
		identity(I)
		premultiplyInverse(A, I, result, tmp)
		printField(result)
	}

	fun multiply(f:Double, m:Array<DoubleArray>):Array<DoubleArray> {
		for(i in m.indices) for(j in m[0].indices) m[i][j] = m[i][j]*f
		return m
	}

	fun sum(a:Array<DoubleArray>, b:Array<DoubleArray>):Array<DoubleArray> {
		for(i in b.indices) for(j in b[0].indices) a[i][j] = a[i][j]+b[i][j]
		return a
	}

	fun premultiplyInverse(l:Array<DoubleArray>, r:Array<DoubleArray>, result:Array<DoubleArray>, tmp:Array<DoubleArray>):Array<DoubleArray>? {
		var l = l
		var result = result
		l = copy(l, tmp)
		result = copy(r, result)
		for(col in l.indices) {
			val nonZero = findFirstNonZero(l, col)
			if(nonZero<0) {
				println("$col FULL EMPTY COLUMN")
				return null
			}
			swapRow(l, col, nonZero)
			swapRow(result, col, nonZero)
			val f = 1.0f/l[col][col]
			multiplyRow(l, col, f)
			multiplyRow(result, col, f)
			for(row in col+1 until l.size) {
				if(l[row][col]!=0.0) {
					val e = -1.0f*l[row][col]
					addMultiple(l, row, col, e)
					addMultiple(result, row, col, e)
				}
			}
		}
		for(col in l.indices.reversed()) {
			for(row in col-1 downTo 0) {
				if(l[row][col]!=0.0) {
					val e = -1.0f*l[row][col]
					addMultiple(l, row, col, e)
					addMultiple(result, row, col, e)
				}
			}
		}
		return result
	}

	fun product(a:Array<DoubleArray>, b:Array<DoubleArray>, result:Array<DoubleArray>):Array<DoubleArray>? {
		if(a[0].size!=b.size) return null else if(!(result.size==a.size&&result[0].size==b[0].size)) return null
		for(i in result.indices) {
			for(j in result[0].indices) {
				result[i][j] = 0.0
				for(k in a[0].indices) {
					result[i][j] += a[i][k]*b[k][j]
				}
			}
		}
		return result
	}

	private fun printField(field:Array<DoubleArray>) {
		for(i in field.indices) {
			for(j in field[0].indices) {
				print(" "+field[i][j])
			}
			println()
		}
	}
}
