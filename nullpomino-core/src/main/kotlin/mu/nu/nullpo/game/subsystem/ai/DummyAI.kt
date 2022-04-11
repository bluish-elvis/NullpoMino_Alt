/*
 * Copyright (c) 2010-2021, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NullNoname nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
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
package mu.nu.nullpo.game.subsystem.ai

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.util.GeneralUtil.getOX

/** DummyAI - Base class for AI players */
open class DummyAI:AIPlayer, Runnable {
	override var bestHold = false
	override var bestX = 0
	override var bestY = 0
	override var bestRt = 0

	/** Hold Force */
	var forceHold = false

	override var thinkCurrentPieceNo = 0
	override var thinkLastPieceNo = 0
	override var thinkComplete = false

	override val name = "DummyAI"
	/** After that I was groundedX-coordinate */
	var bestXSub = 0
	/** After that I was groundedY-coordinate */
	var bestYSub = 0
	/** After that I was groundedDirection(-1: None) */
	var bestRtSub = 0
	/** The best moveEvaluation score */
	var bestPts = 0
	/** true when thread is executing the think routine.  */
	var thinking = false
	/** Delay the move for changecount  */
	var delay = 0
	/** The GameEngine that owns this AI  */
	lateinit var gEngine:GameEngine
	/** The GameManager that owns this AI  */
	var gManager:GameManager? = null
	/** To stop a thread time  */
	protected var thinkDelay = 0
	/** When true,Running thread  */
	@Volatile var threadRunning = false
	/** Thread for executing the think routine  */
	var thread:Thread? = null

	override fun init(engine:GameEngine, playerID:Int) {}

	override fun newPiece(engine:GameEngine, playerID:Int) {}

	override fun onFirst(engine:GameEngine, playerID:Int) {}

	override fun onLast(engine:GameEngine, playerID:Int) {}

	override fun renderState(engine:GameEngine, playerID:Int) {
		engine.owner.receiver.run {
			drawMenuFont(engine, playerID, 0, 0, name, EventReceiver.COLOR.GREEN, 0.5f)
			drawMenuFont(engine, playerID, 5, 1, "X", EventReceiver.COLOR.BLUE, 0.5f)
			drawMenuFont(engine, playerID, 8, 1, "Y", EventReceiver.COLOR.BLUE, 0.5f)
			drawMenuFont(engine, playerID, 11, 1, "RT", EventReceiver.COLOR.BLUE, 0.5f)
			drawMenuFont(engine, playerID, 0, 2, "BEST:", EventReceiver.COLOR.BLUE, 0.5f)
			drawMenuFont(engine, playerID, 5, 2, "$bestX", 0.5f)
			drawMenuFont(engine, playerID, 8, 2, "$bestY", 0.5f)
			drawMenuFont(engine, playerID, 11, 2, "$bestRt", 0.5f)
			drawMenuFont(engine, playerID, 0, 3, "SUB:", EventReceiver.COLOR.BLUE, 0.5f)
			drawMenuFont(engine, playerID, 5, 3, "$bestXSub", 0.5f)
			drawMenuFont(engine, playerID, 8, 3, "$bestYSub", 0.5f)
			drawMenuFont(engine, playerID, 11, 3, "$bestRtSub", 0.5f)
			drawMenuFont(engine, playerID, 0, 4, "NOW:", EventReceiver.COLOR.BLUE, 0.5f)
			if(engine.nowPieceObject==null)
				drawMenuFont(engine, playerID, 5, 4, "-- -- --", 0.5f) else {
				drawMenuFont(engine, playerID, 5, 4, "${engine.nowPieceX}", 0.5f)
				drawMenuFont(engine, playerID, 8, 4, "${engine.nowPieceY}", 0.5f)
				drawMenuFont(engine, playerID, 11, 4, "${engine.nowPieceObject?.direction ?: "--"}", 0.5f)
			}
			drawMenuFont(engine, playerID, 0, 5, "MOVE SCORE:", EventReceiver.COLOR.BLUE, 0.5f)
			drawMenuFont(engine, playerID, 13, 5, "$bestPts", bestPts<=0, 0.5f)
			drawMenuFont(engine, playerID, 0, 6, "THINK ACTIVE:", EventReceiver.COLOR.BLUE, 0.5f)
			drawMenuFont(engine, playerID, 15, 6, thinking.getOX, 0.5f)
		}
	}

	override fun setControl(engine:GameEngine, playerID:Int, ctrl:Controller):Int = 0

	override fun shutdown() {
		if(thread?.isAlive==true) {
			thread?.interrupt()
			threadRunning = false
			thread = null
		}
	}

	override fun renderHint(engine:GameEngine, playerID:Int) {

	}

	override fun run() {}
}
