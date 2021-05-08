package zeroxfc.nullpo.custom.wallkick

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
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