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

package zeroxfc.nullpo.custom.random

import mu.nu.nullpo.game.component.Piece
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer

class TerrorInstinctRandomizer:Randomizer() {
	private var piecePool:MutableList<Int> = mutableListOf()
	private var history:MutableList<Int> = mutableListOf()
	private var count = 0
	override fun init() {
		piecePool = MutableList(pieces.size*5) {pieces[it/5]}
		history = MutableList(4) {if(it%2==0) Piece.PIECE_S else Piece.PIECE_Z}
		count = 0
		for(i in pieces) for(j in 0..4) piecePool.add(i)
	}

	override fun next():Int {
		var idx:Int
		var id:Int
		var rolls = 0
		do {
			idx = r.nextInt(piecePool.size)
			id = piecePool[idx]
			rolls++
		} while(history.contains(id)&&rolls<MAX_ROLLS||count==0&&id==Piece.PIECE_O)
		count++
		appendHistory(id, idx)
		return id
	}

	private fun appendHistory(id:Int, idx:Int) {
		val temp = history.removeAt(0)
		history.add(id)
		piecePool.removeAt(idx)
		if(count>4) piecePool.add(temp)
	}

	companion object {
		private const val MAX_ROLLS = 6
	}
}
