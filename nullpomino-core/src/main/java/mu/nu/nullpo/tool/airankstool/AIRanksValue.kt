/*
 * Copyright (c) 2010-2021, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NullNoname nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
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

package mu.nu.nullpo.tool.airankstool

import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream

object AIRanksValue {

	/** @param args arguments
	 */
	@JvmStatic
	fun main(args:Array<String>) {
		val fis:FileInputStream
		val `in`:ObjectInputStream
		var ranks:Ranks
		val inputFile = "${AIRanksConstants.RANKSAI_DIR}ranks20"

		if(inputFile.trim {it<=' '}.isEmpty())
			ranks = Ranks(4, 9)
		else
			try {
				fis = FileInputStream(inputFile)
				`in` = ObjectInputStream(fis)
				ranks = `in`.readObject() as Ranks
				`in`.close()
				val surface1 = intArrayOf(0, 1, 1, -1, -1, 1, -3, -2)
				val surface2 = intArrayOf(0, 1, 1, -1, -1, 4, -4, 2)

				val rank1 = ranks.getRankValue(ranks.encode(surface1))
				val rank2 = ranks.getRankValue(ranks.encode(surface2))
				println(rank1)
				println(rank2)
			} catch(e:FileNotFoundException) {
				ranks = Ranks(4, 9)
			} catch(e:IOException) {
				// TODO Auto-generated catch block
				e.printStackTrace()
			} catch(e:ClassNotFoundException) {
				e.printStackTrace()
			}

	}

}
