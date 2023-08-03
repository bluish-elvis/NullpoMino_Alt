/*
 Copyright (c) 2022-2023,
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

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package wtf.oshisaure.nullpomodshit.wallkicks

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.component.WallkickResult
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick

class ClassicOSpinWallkick:Wallkick {
	var trySeq = intArrayOf(4, 0, 1, 5, 3, 6, 2)
	override fun executeWallkick(
		x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field, ctrl:Controller?
	):WallkickResult? {
		var check = 0
		if(piece.big) check = 1
		if(checkCollisionKick(piece, x, y, rtNew, field)||piece.id==8||piece.id==10) {
			val oldpiece = Piece(piece)
			var temp:Int
			if(piece.id==2) {
				temp = 0
				while(temp<trySeq.size) {
					piece.replace(Piece(trySeq[temp]))
					piece.setBlock(oldpiece.block[0])
//					piece.id = oldpiece.id
					piece.setColor(oldpiece.colors)
					for(yi in 3 downTo 0) {
						for(xi in 0..10) {
							if(!piece.checkCollision(x+xi*(1+check), y+yi, rtNew, field)) return WallkickResult(xi, yi, rtNew)
							if(!piece.checkCollision(x-xi*(1+check), y+yi, rtNew, field)) return WallkickResult(-xi, yi, rtNew)
						}
					}
					++temp
				}
				piece.replace(oldpiece)
			} else if(piece.id!=0&&(checkCollisionKick(piece, x, y, rtNew, field)||piece.id==8||piece.id==10)) {
				temp = 0
				if(!piece.checkCollision(x-1-check, y, rtNew, field)) temp = -1-check
				if(!piece.checkCollision(x+1+check, y, rtNew, field)) temp = 1+check
				if(temp!=0) return WallkickResult(temp, 0, rtNew)
			}
		}
		return null
	}

	private fun checkCollisionKick(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):Boolean {
		return if(piece.big) {
			checkCollisionKickBig(piece, x, y, rt, fld)
		} else {
			for(i in 0..<piece.maxBlock) {
				if(piece.dataX[rt][i]!=1+piece.dataOffsetX[rt]) {
					val x2 = x+piece.dataX[rt][i]
					val y2 = y+piece.dataY[rt][i]
					if(x2>=fld.width||y2>=fld.height||fld.getCoordAttribute(x2, y2)==3||
						fld.getCoordAttribute(x2, y2)!=2&&fld.getBlockColor(x2, y2)!=null
					) return true
				}
			}
			false
		}
	}

	private fun checkCollisionKickBig(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):Boolean {
		for(i in 0..<piece.maxBlock) {
			if(piece.dataX[rt][i]!=1+piece.dataOffsetX[rt]) {
				val x2 = x+piece.dataX[rt][i]*2
				val y2 = y+piece.dataY[rt][i]*2
				for(k in 0..1) {
					for(l in 0..1) {
						val x3 = x2+k
						val y3 = y2+l
						if(x3>=fld.width||y3>=fld.height||fld.getCoordAttribute(x3, y3)==3
							||fld.getCoordAttribute(x3, y3)!=2&&fld.getBlockColor(x3, y3)!=null
						) return true
					}
				}
			}
		}
		return false
	}
}
