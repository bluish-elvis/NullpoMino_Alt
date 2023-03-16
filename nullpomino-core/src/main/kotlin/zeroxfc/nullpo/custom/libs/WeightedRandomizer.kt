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

import kotlin.random.Random

class WeightedRandomizer(weightArr:IntArray, seed:Long) {
	private val localRandom:Random
	private var results:IntArray
	private var cumulativeWeightList:IntArray
	private var maxWeight:Int
	fun setWeights(weightArr:IntArray) {
		var numberOfZeroes = 0
		for(i in weightArr) {
			if(i==0) numberOfZeroes++
		}
		cumulativeWeightList = IntArray(weightArr.size-numberOfZeroes)
		results = IntArray(weightArr.size-numberOfZeroes)
		maxWeight = 0
		var ctr = 0
		for(i in weightArr.indices) {
			if(weightArr[i]!=0) {
				maxWeight += weightArr[i]
				cumulativeWeightList[ctr] = maxWeight
				results[ctr] = i
				ctr++
			}
		}
	}

	val max:Int
		get() = results[results.size-1]

	fun nextInt():Int {
		val gVal = localRandom.nextInt(maxWeight)+1
		var result = 0
		for(i in cumulativeWeightList.indices) {
			if(cumulativeWeightList[i]>=gVal) {
				result = i
				break
			}
		}
		return results[result]
	}

	init {
		var numberOfZeroes = 0
		for(i in weightArr) {
			if(i==0) numberOfZeroes++
		}
		cumulativeWeightList = IntArray(weightArr.size-numberOfZeroes)
		results = IntArray(weightArr.size-numberOfZeroes)
		maxWeight = 0
		var ctr = 0
		for(i in weightArr.indices) {
			if(weightArr[i]!=0) {
				maxWeight += weightArr[i]
				cumulativeWeightList[ctr] = maxWeight
				results[ctr] = i
				ctr++
			}
		}
		localRandom = Random(seed)
	}
}
