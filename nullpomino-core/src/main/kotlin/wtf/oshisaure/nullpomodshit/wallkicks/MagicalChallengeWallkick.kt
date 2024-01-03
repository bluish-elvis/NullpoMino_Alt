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

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.component.WallkickResult
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick

class MagicalChallengeWallkick:Wallkick {
	override fun executeWallkick(
		x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field, ctrl:Controller?
	):WallkickResult? = when(piece.id) {
		0 -> when(rtNew) {
			0, 2 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -2, 0, rtNew, field)
			else if(checkCell(x+3, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
			else if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
			else null
			1, 3 -> if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, -2, rtNew, field)
			else if(checkCell(x+1, y+3, field)) testWallkick(piece, x, y, 0, -1, rtNew, field)
			else if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field)
			else null
			else -> null
		}
		1 -> when(rtDir) {
			1 -> when(rtNew) {
				0 -> if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, -1, rtNew, field)
				else if(checkCell(x, y+2, field)) testWallkick(piece, x, y, 1, 0, rtNew, field) else null
				1 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else if(checkCell(x, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				2 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field)
				else if(checkCell(x+2, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				3 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else if(checkCell(x+2, y+2, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				else -> null
			}
			-1 -> when(rtNew) {
				0 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else if(checkCell(x+2, y+2, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				1 -> if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, -1, rtNew, field) else null
				2 -> if(checkCell(x, y, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field) else null
				3 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field) else null
				else -> null
			}
			else -> null
		}
		5 -> when(rtDir) {
			-1 -> when(rtNew) {
				0 -> if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, -1, rtNew, field)
				else if(checkCell(x+2, y+2, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				1 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else if(checkCell(x, y+2, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				2 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field)
				else if(checkCell(x, y, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				3 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else if(checkCell(x+2, y, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				else -> null
			}
			1 -> when(rtNew) {
				0 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else if(checkCell(x, y+2, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				1 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, -1, rtNew, field)
				else null
				2 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else if(checkCell(x+1, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				3 -> if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, 1, rtNew, field)
				else null
				else -> null
			}
			else -> null
		}
		6 -> when(rtDir) {
			1 -> when(rtNew) {
				0 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field)
				else if(checkCell(x+2, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				1 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else if(checkCell(x+2, y+2, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				2 -> if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, -1, rtNew, field)
				else if(checkCell(x+2, y+2, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				3 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else if(checkCell(x, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				else -> null
			}
			-1 -> when(rtNew) {
				0 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				1 -> if(checkCell(x+2, y+2, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field)
				else null
				2 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				3 -> if(checkCell(x, y, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, -1, rtNew, field)
				else null
				else -> null
			}
			else -> null
		}
		3 -> when(rtDir) {
			-1 -> when(rtNew) {
				0 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field)
				else if(checkCell(x, y, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				1 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else if(checkCell(x+2, y+2, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				2 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, -1, rtNew, field)
				else if(checkCell(x+2, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				3 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else if(checkCell(x, y+2, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				else -> null
			}
			1 -> when(rtNew) {
				0 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field) else null
				1 -> if(checkCell(x+2, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else if(checkCell(x, y+2, field)) testWallkick(piece, x, y, 0, -1, rtNew, field) else null
				2 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field) else null
				3 -> if(checkCell(x, y+2, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field) else null
				else -> null
			}
			else -> null
		}
		4 -> when(rtOld) {
			0 -> if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, -1, rtNew, field) else null
			1 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field) else null
			2 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field) else null
			3 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field) else null
			else -> null
		}
		9 -> when(rtNew) {
			0, 2 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
			else if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field) else null
			1, 3 -> if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, -1, rtNew, field)
			else if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field) else null
			else -> null
		}
		8 -> when(rtNew) {
			0, 2 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field) else null
			1, 3 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 0, -1, rtNew, field) else null
			else -> null
		}
		10 -> when(rtDir) {
			1 -> when(rtNew) {
				0 -> if(checkCell(x, y, field)) testWallkick(piece, x, y, 1, 0, rtNew, field) else null
				1 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field) else null
				2 -> if(checkCell(x+1, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field) else null
				3 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 0, -1, rtNew, field) else null
				else -> null
			}
			-1 -> when(rtNew) {
				0 -> if(checkCell(x+1, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field) else null
				1 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 0, -1, rtNew, field) else null
				2 -> null
				3 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field) else null
				else -> null
			}
			else -> null
		}
		else -> null
	}

	private fun testWallkick(piece:Piece, x:Int, y:Int, i:Int, j:Int, rtNew:Int, field:Field):WallkickResult? =
		if(piece.checkCollision(x+i, y+j, rtNew, field)) null else WallkickResult(i, j, rtNew)

	private fun checkCell(x:Int, y:Int, fld:Field):Boolean =
		x>=fld.width||y>=fld.height||fld.getCoordAttribute(x, y)==3||fld.getCoordAttribute(x, y)!=2&&fld.getBlockColor(x, y)!=null
}
