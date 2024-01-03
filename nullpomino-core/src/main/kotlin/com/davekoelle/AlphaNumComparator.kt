/*
 found at: https://stackoverflow.com/a/104709/1353930
 downloaded from: http://www.davekoelle.com/files/AlphanumComparator.java
 local modifications by Daniel Alder:
 - static alphaNumOrder() / instance
 - package declaration
 - fixed sorting of leading zeroes
 */
/*
 The Alphanum Algorithm is an improved sorting algorithm for strings
 containing numbers.  Instead of sorting numbers in ASCII order like
 a standard nameSort, this algorithm sorts numbers in numeric order.

 The Alphanum Algorithm is discussed at http://www.DaveKoelle.com

 Released under the MIT License - https://opensource.org/licenses/MIT

 Copyright 2007-2017 David Koelle

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the "Software"),
 to deal in the Software without restriction, including without limitation
 the rights to use, copy, modify, merge, publish, distribute, sublicense,
 and/or sell copies of the Software, and to permit persons to whom the
 Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included
 in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.davekoelle

import java.util.stream.Collectors

/**
This is an updated version with enhancements made by Daniel Migowski,
Andre Bogus, and David Koelle. Updated by David Koelle in 2017.

To use this class:
Use the static "nameSort" method from the java.util.Collections class:
Collections.nameSort(your list, new AlphaNumComparator());
 */
class AlphaNumComparator:Comparator<String?> {
	private fun isDigit(ch:Char):Boolean = ch.code in 48..57
	/** Length of string is passed in for improved efficiency (only need to calculate it once)  */
	private fun getChunk(s:String, slength:Int, marker:Int):String {
		var mk = marker
		val chunk = StringBuilder()
		var c = s[mk]
		chunk.append(c)
		mk++
		if(isDigit(c)) {
			while(mk<slength) {
				c = s[mk]
				if(!isDigit(c)) break
				chunk.append(c)
				mk++
			}
		} else {
			while(mk<slength) {
				c = s[mk]
				if(isDigit(c)) break
				chunk.append(c)
				mk++
			}
		}
		return "$chunk"
	}

	override fun compare(s1:String?, s2:String?):Int {
		if(s1==null||s2==null) return 0
		var thisMarker = 0
		var thatMarker = 0
		val s1Length = s1.length
		val s2Length = s2.length
		while(thisMarker<s1Length&&thatMarker<s2Length) {
			val thisChunk = getChunk(s1, s1Length, thisMarker)
			thisMarker += thisChunk.length
			val thatChunk = getChunk(s2, s2Length, thatMarker)
			thatMarker += thatChunk.length

			// If both chunks contain numeric characters, nameSort them numerically
			var result = 0
			if(isDigit(thisChunk[0])&&isDigit(thatChunk[0])) {
				val thisLen = thisChunk.length
				val thatLen = thatChunk.length
				val bothLen = maxOf(thisLen, thatLen)
				var thisPos = thisLen-bothLen
				var thatPos = thatLen-bothLen
				while(thisPos<thisLen) {
					val thisChar = if(thisPos<0) '0' else thisChunk[thisPos]
					val thatChar = if(thatPos<0) '0' else thatChunk[thatPos]
					result = thisChar-thatChar
					if(result!=0) {
						return result
					}
					thisPos++
					thatPos++
				}
			} else {
				result = thisChunk.compareTo(thatChunk)
			}
			if(result!=0) return result
		}
		val result = s1Length-s2Length
		return if(result!=0) result else s1.compareTo(s2)
	}

	companion object {
		val instance:Comparator<String?> = AlphaNumComparator()
		/**
		 * Shows an example of how the comparator works.
		 * Feel free to delete this in your own code!
		 */
		@JvmStatic fun main(args:Array<String>) {
			val values = listOf("dazzle2", "dazzle10", "dazzle1", "dazzle2.7", "dazzle2.10", "2", "10", "1", "EctoMorph6",
				"EctoMorph62", "EctoMorph7")
			println(values.stream().sorted(AlphaNumComparator()).collect(Collectors.joining(" ")))
		}
	}
}
