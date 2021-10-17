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

	override fun playerInit(engine:GameEngine, playerID:Int) {
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

	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		return if(engine.statc[0]==0) {
			engine.fieldWidth = 19
			engine.fieldHeight = 19
			engine.createFieldIfNeeded()
			val var10002 = engine.statc[0]++
			engine.playSE("ready")
			val startx = 9-snakelength/2
			for(i in 0 until snakelength) {
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

	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
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
		var i:Int = 1
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
				oldhead?.setAttribute(8, true)
			}
			1 -> {
				snakepositionsX[snakelength-1]++
				headattr = headattr or 32
				oldhead?.setAttribute(64, true)
			}
			2 -> {
				snakepositionsY[snakelength-1]++
				headattr = headattr or 8
				oldhead?.setAttribute(16, true)
			}
			3 -> {
				snakepositionsX[snakelength-1]--
				headattr = headattr or 64
				oldhead?.setAttribute(32, true)
			}
		}
		engine.field.setBlock(snakepositionsX[snakelength-1],
			snakepositionsY[snakelength-1], Block(5, engine.skin, headattr))
		if(oldtailX>newtailX) newtail?.setAttribute(64, false)
		if(oldtailX<newtailX) newtail?.setAttribute(32, false)
		if(oldtailY>newtailY) newtail?.setAttribute(16, false)
		if(oldtailY<newtailY) newtail?.setAttribute(8, false)
	}

	private fun moveForwardAndCatch(engine:GameEngine) {
		val newpositionsX = IntArray(snakelength+1)
		val newpositionsY = IntArray(snakelength+1)
		var oldheadX:Int = 0
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
		val newhead:Block = Block(5, engine.skin, 1)
		when(orientation) {
			0 -> {
				newhead.setAttribute(16, true)
				oldhead?.setAttribute(8, true)
			}
			1 -> {
				newhead.setAttribute(32, true)
				oldhead?.setAttribute(64, true)
			}
			2 -> {
				newhead.setAttribute(8, true)
				oldhead?.setAttribute(16, true)
			}
			3 -> {
				newhead.setAttribute(64, true)
				oldhead?.setAttribute(32, true)
			}
		}
		engine.field.setBlock(newheadX, newheadY, newhead)
		++snakelength
		snakepositionsX = newpositionsX
		snakepositionsY = newpositionsY
		moveBonus(engine)
	}

	private fun blockInSnake(x:Int, y:Int):Int {
		for(i in 0 until snakelength) if(snakepositionsX[i]==x&&snakepositionsY[i]==y) return i
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