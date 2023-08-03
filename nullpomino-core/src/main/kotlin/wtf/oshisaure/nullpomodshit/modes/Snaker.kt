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

package wtf.oshisaure.nullpomodshit.modes

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.AbstractMode

class Snaker:AbstractMode() {
	private var orientation = 0
	private var lastmove = 0
	private var movetimer = 0
	private var movedelay = 0
	private var snakepositionsX:IntArray = IntArray(5)
	private var snakepositionsY:IntArray = IntArray(5)
	private var snakelength = 0
	private var bonusY = 0
	private var bonusX = 0
	override val name = "Sneaker"

	override fun playerInit(engine:GameEngine) {
		snakelength = 5
		snakepositionsX = IntArray(5)
		snakepositionsY = IntArray(5)
		for(i in 0..4) {
			snakepositionsX[i] = 7+i
			snakepositionsY[i] = 9
		}
		movetimer = 0
		movedelay = 15
		lastmove = 1
		orientation = 1
		engine.blockOutlineType = 0
		engine.rainbowAnimate = true
	}

	override fun onReady(engine:GameEngine):Boolean {
		return if(engine.statc[0]==0) {
			engine.fieldWidth = 19
			engine.fieldHeight = 19
			engine.createFieldIfNeeded()
			engine.statc[0]++
			engine.playSE("ready")
			val startx = 9-snakelength/2
			for(i in 0..<snakelength) {
				engine.field.setBlock(startx+i, 9, Block(5, engine.skin, 1 or (if(i!=0) 32 else 0) or if(i!=snakelength-1) 64 else 0))
			}
			bonusX = 9
			bonusY = 9
			moveBonus(engine)
			true
		} else if(engine.statc[0]>=engine.goEnd) {
			engine.stat = GameEngine.Status.MOVE
			if(!engine.readyDone) {
				engine.startTime = System.nanoTime()
			}
			engine.readyDone = true
			true
		} else {
			false
		}
	}

	override fun onMove(engine:GameEngine):Boolean {
		if(engine.ctrl.isPress(0)&&lastmove!=2) orientation = 0
		if(engine.ctrl.isPress(3)&&lastmove!=3) orientation = 1
		if(engine.ctrl.isPress(1)&&lastmove!=0) orientation = 2
		if(engine.ctrl.isPress(2)&&lastmove!=1) orientation = 3
		try {
			if(movetimer>=movedelay) {
				if(checkBonusAhead()) {
					moveForwardAndCatch(engine)
				} else {
					moveForward(engine)
				}
				lastmove = orientation
				movetimer = 0
			}
		} catch(var4:NullPointerException) {
			engine.stat = GameEngine.Status.GAMEOVER
			engine.gameEnded()
		}
		if(blockInSnake(snakepositionsX[snakelength-1], snakepositionsY[snakelength-1])!=snakelength-1) {
			engine.stat = GameEngine.Status.GAMEOVER
			engine.gameEnded()
		}
		++movetimer
		return true
	}

	private fun checkBonusAhead():Boolean {
		val x = snakepositionsX[snakelength-1]
		val y = snakepositionsY[snakelength-1]
		return when(orientation) {
			0 -> bonusX==x&&bonusY==y-1
			1 -> bonusX==x+1&&bonusY==y
			2 -> bonusX==x&&bonusY==y+1
			3 -> bonusX==x-1&&bonusY==y
			else -> false
		}
	}

	private fun moveForward(engine:GameEngine) {
		val oldtailX = snakepositionsX[0]
		val oldtailY = snakepositionsY[0]
		val newtailX = snakepositionsX[1]
		val newtailY = snakepositionsY[1]
		val newtail = engine.field.getBlock(newtailX, newtailY)
		val oldhead = engine.field.getBlock(snakepositionsX[snakelength-1], snakepositionsY[snakelength-1])
		var headattr = 1
		engine.field.setBlock(oldtailX, oldtailY, Block())
		var i = 1
		while(i<snakelength) {
			snakepositionsX[i-1] = snakepositionsX[i]
			++i
		}
		i = 1
		while(i<snakelength) {
			snakepositionsY[i-1] = snakepositionsY[i]
			++i
		}
		when(orientation) {
			0 -> {
				snakepositionsY[snakelength-1]--
				headattr = headattr or 16
				oldhead?.setAttribute(true, 8)
			}
			1 -> {
				snakepositionsX[snakelength-1]++
				headattr = headattr or 32
				oldhead?.setAttribute(true, 64)
			}
			2 -> {
				snakepositionsY[snakelength-1]++
				headattr = headattr or 8
				oldhead?.setAttribute(true, 16)
			}
			3 -> {
				snakepositionsX[snakelength-1]--
				headattr = headattr or 64
				oldhead?.setAttribute(true, 32)
			}
		}
		engine.field.setBlock(
			snakepositionsX[snakelength-1],
			snakepositionsY[snakelength-1], Block(5, engine.skin, headattr)
		)
		if(oldtailX>newtailX) newtail?.setAttribute(false, 64)
		if(oldtailX<newtailX) newtail?.setAttribute(false, 32)
		if(oldtailY>newtailY) newtail?.setAttribute(false, 16)
		if(oldtailY<newtailY) newtail?.setAttribute(false, 8)
	}

	private fun moveForwardAndCatch(engine:GameEngine) {
		val newpositionsX = IntArray(snakelength+1)
		val newpositionsY = IntArray(snakelength+1)
		var oldheadX = 0
		while(oldheadX<snakelength) {
			newpositionsX[oldheadX] = snakepositionsX[oldheadX]
			newpositionsY[oldheadX] = snakepositionsY[oldheadX]
			++oldheadX
		}
		newpositionsX[snakelength] = bonusX
		newpositionsY[snakelength] = bonusY
		oldheadX = newpositionsX[snakelength-1]
		val oldheadY = newpositionsY[snakelength-1]
		val newheadX = newpositionsX[snakelength]
		val newheadY = newpositionsY[snakelength]
		val oldhead = engine.field.getBlock(oldheadX, oldheadY)
		val newhead = Block(5, engine.skin, 1)
		when(orientation) {
			0 -> {
				newhead.setAttribute(true, 16)
				oldhead?.setAttribute(true, 8)
			}
			1 -> {
				newhead.setAttribute(true, 32)
				oldhead?.setAttribute(true, 64)
			}
			2 -> {
				newhead.setAttribute(true, 8)
				oldhead?.setAttribute(true, 16)
			}
			3 -> {
				newhead.setAttribute(true, 64)
				oldhead?.setAttribute(true, 32)
			}
		}
		engine.field.setBlock(newheadX, newheadY, newhead)
		++snakelength
		snakepositionsX = newpositionsX
		snakepositionsY = newpositionsY
		moveBonus(engine)
	}

	private fun blockInSnake(x:Int, y:Int):Int {
		for(i in 0..<snakelength) if(snakepositionsX[i]==x&&snakepositionsY[i]==y) return i
		return -1
	}

	private fun moveBonus(engine:GameEngine) {
		while(blockInSnake(bonusX, bonusY)!=-1) {
			bonusX = engine.random.nextInt(engine.field.width)
			bonusY = engine.random.nextInt(engine.field.height)
		}
		engine.field.setBlock(bonusX, bonusY, Block(35, engine.skin, Block.ATTRIBUTE.VISIBLE))
	}

	companion object {
		private const val ORIENTATION_UP = 0
		private const val ORIENTATION_RIGHT = 1
		private const val ORIENTATION_DOWN = 2
		private const val ORIENTATION_LEFT = 3
	}
}
