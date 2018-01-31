package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.play.GameEngine

/** PREVIEW mode - A game mode for Tuning preview */
class PreviewMode:AbstractMode() {
	/* Mode name */
	override val name:String
		get() = "PREVIEW"

	/* Player init */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		engine.allowTextRenderByReceiver = false
		engine.readyStart = -1
		engine.readyEnd = -1
		engine.goStart = -1
		engine.goEnd = 10
	}

	/* Game Over - or is it? */
	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		engine.lives = 1 // Let's give unlimited lives
		return false
	}
}
