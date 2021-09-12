/*
 * Copyright (c) 2021-2021,
 * This library class was created by 0xFC963F18DC21 / Shots243
 * It is part of an extension library for the game NullpoMino (copyright 2021-2021)
 *
 * Kotlin converted and modified by Venom=Nhelv
 *
 * Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 * Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.
 *
 * THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 *
 * Repository: https://github.com/Shots243/ModePile
 *
 * When using this library in a mode / library pack of your own, the following
 * conditions must be satisfied:
 *     - This license must remain visible at the top of the document, unmodified.
 *     - You are allowed to use this library for any modding purpose.
 *         - If this is the case, the Library Creator must be credited somewhere.
 *             - Source comments only are fine, but in a README is recommended.
 *     - Modification of this library is allowed, but only in the condition that a
 *       pull request is made to merge the changes to the repository.
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

package zeroxfc.nullpo.custom.wallkick

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.component.WallkickResult
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick
import kotlin.math.pow

class SomeRandomShittyRotationSystemWallkick:Wallkick {
	// So basically, it's a symmetric spiral checker.
	// The bigger the field width, the more checks it performs.
	override fun executeWallkick(x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean,
		piece:Piece, field:Field, ctrl:Controller?):WallkickResult? {
		var x2 = 0
		var y2 = 0
		var offsetRadius = field.width+1
		if(piece.big) offsetRadius = field.width/2+1
		var dx = 1
		var dy = 1
		var m = 1 // not sure what this is for...
		var iter = 0
		while(iter<offsetRadius.toDouble().pow(2.0)) {
			while(2*x2*dx<m) {
				// STH
				if(iter>=offsetRadius.toDouble().pow(2.0)) break
				var x3 = x2
				x3 *= if(rtDir==1) 1 else -1
				if(piece.big) {
					x3 *= 2
					y2 *= 2
				}
				if(!piece.checkCollision(x+x3, y+y2, rtNew, field)) {
					if(!(x3==0&&y2==0)) {
						return WallkickResult(x3, y2, rtNew)
					}
				}
				iter++
				x2 += dx
			}
			while(2*y2*dy<m) {
				// STH
				if(iter>=offsetRadius.toDouble().pow(2.0)) break
				var x3 = x2
				x3 *= if(rtDir==1) 1 else -1
				if(piece.big) {
					x3 *= 2
					y2 *= 2
				}
				if(!piece.checkCollision(x+x3, y+y2, rtNew, field)) {
					if(!(x3==0&&y2==0)) {
						return WallkickResult(x3, y2, rtNew)
					}
				}
				iter++
				y2 += dy
			}
			dx *= -1
			dy *= -1
			m++
		}

		/*
		for (int i = 0; i < Math.pow(argX, 2); i++) {
			// Spirals are swine.
			// They are a son of a mother-less goat.
			
			I really have no goddamn clue. 
			 
			if ((-argX/2 < x2) &&
				(x2 <= argX/2) &&
				(-argY/2 < y2) &&
				(y2 <= argY/2)) {
				x3 = x2;
				x3 *= (rtDir == 1) ? -1 : 1;
				
				if (piece.big)
				{
					x3 *= 2; y2 *= 2;
				}
				
				if ((piece.checkCollision(x + x3, y + y2, rtNew, field) == false)) {
					if (!(x3 == 0 && y2 == 0)) {
						return new WallkickResult(x3, y2, rtNew);
					}
				}
			}
			
			if ((x2 == y2) ||
				(x2 < 0 && x2 == -y2) ||
				(x2 > 0 && x2 == (1-y2))) {
				int temp = dx;
				dx = -dy;
				dy = temp;
			}
					
			x2 += dx;
			y2 += dy;
			
		}
		*/return null
	}
}