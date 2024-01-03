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

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package wtf.oshisaure.nullpomodshit.wallkicks

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.component.WallkickResult
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick

class DestroyerWallkick:Wallkick {
	override fun executeWallkick(
		x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field, ctrl:Controller?
	):WallkickResult {
		val kick = movePieceInBounds(piece, x, y, rtNew, field)
		destroyBlocksKick(piece, x, y, rtNew, field)
		return kick
	}

	private fun destroyBlocksKick(piece:Piece, x:Int, y:Int, rt:Int, fld:Field) {
		if(piece.big) {
			destroyBlocksKickBig(piece, x, y, rt, fld)
		} else {
			for(i in 0..<piece.maxBlock) {
				val x2 = x+piece.dataX[rt][i]
				val y2 = y+piece.dataY[rt][i]
				fld.setBlock(x2, y2, Block())
			}
		}
	}

	private fun destroyBlocksKickBig(piece:Piece, x:Int, y:Int, rt:Int, fld:Field) {
		for(i in 0..<piece.maxBlock) {
			val x2 = x+piece.dataX[rt][i]*2
			val y2 = y+piece.dataY[rt][i]*2
			for(k in 0..1) {
				for(l in 0..1) {
					val x3 = x2+k
					val y3 = y2+l
					val bl = fld.getBlock(x3, y3)
					if(bl!=null&&!bl.isEmpty) {
						fld.setBlock(x3, y3, Block())
					}
				}
			}
		}
	}

	private fun movePieceInBounds(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):WallkickResult {
		return if(piece.big) {
			movePieceInBoundsBig(piece, x, y, rt, fld)
		} else {
			var kickX = 0
			var kickY = 0
			var kickDir = checkInBoundsKick(piece, x, y, rt, fld)
			while(kickDir!=-1) {
				when(kickDir) {
					0 -> --kickX
					1 -> ++kickX
					2 -> --kickY
					3 -> ++kickY
				}
				kickDir = checkInBoundsKick(piece, x+kickX, y+kickY, rt, fld)
			}
			WallkickResult(kickX, kickY, rt)
		}
	}

	private fun movePieceInBoundsBig(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):WallkickResult {
		var kickX = 0
		var kickY = 0
		var kickDir = checkInBoundsKickBig(piece, x, y, rt, fld)
		while(kickDir!=-1) {
			when(kickDir) {
				0 -> --kickX
				1 -> ++kickX
				2 -> --kickY
				3 -> ++kickY
			}
			kickDir = checkInBoundsKickBig(piece, x+kickX, y+kickY, rt, fld)
		}
		return WallkickResult(kickX, kickY, rt)
	}

	private fun checkInBoundsKick(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):Int {
		return if(piece.big) {
			checkInBoundsKickBig(piece, x, y, rt, fld)
		} else {
			for(i in 0..<piece.maxBlock) {
				val x2 = x+piece.dataX[rt][i]
				val y2 = y+piece.dataY[rt][i]
				if(x2>=fld.width) return 0 else
					if(x2<0) return 1 else
						if(y2>=fld.height) return 2 else
							if(y2<0&&fld.ceiling) return 3
			}
			-1
		}
	}

	private fun checkInBoundsKickBig(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):Int {
		for(i in 0..<piece.maxBlock) {
			val x2 = x+piece.dataX[rt][i]*2
			val y2 = y+piece.dataY[rt][i]*2
			for(k in 0..1) {
				for(l in 0..1) {
					val x3 = x2+k
					val y3 = y2+l
					when {
						x3>=fld.width -> return 0
						x3<0 -> return 1
						y3>=fld.height -> return 2
						y3<0&&fld.ceiling -> return 3
					}
				}
			}
		}
		return -1
	}
}
