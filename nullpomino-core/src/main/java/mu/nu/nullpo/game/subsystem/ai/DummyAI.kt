/* Copyright (c) 2010, NullNoname
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of NullNoname nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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
 * POSSIBILITY OF SUCH DAMAGE. */
package mu.nu.nullpo.game.subsystem.ai

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.play.GameEngine

/** DummyAI - Base class for AI players */
open class DummyAI:AIPlayer {
	/** ホールド使用予定 */
	var bestHold:Boolean = false

	/** Plan to putX-coordinate */
	var bestX:Int = 0

	/** Plan to putY-coordinate */
	var bestY:Int = 0

	/** Plan to putDirection */
	var bestRt:Int = 0

	/** Hold Force */
	var forceHold:Boolean = false

	/** Current piece number */
	var thinkCurrentPieceNo:Int = 0

	/** Peace of thoughts is finished number */
	var thinkLastPieceNo:Int = 0

	/** Did the thinking thread finish successfully? */
	open var thinkComplete:Boolean = false

	override val name:String = "DummyAI"

	override fun init(engine:GameEngine, playerID:Int) {}

	override fun newPiece(engine:GameEngine, playerID:Int) {}

	override fun onFirst(engine:GameEngine, playerID:Int) {}

	override fun onLast(engine:GameEngine, playerID:Int) {}

	override fun renderState(engine:GameEngine, playerID:Int) {}

	override fun setControl(engine:GameEngine, playerID:Int, ctrl:Controller) {}

	override fun shutdown() {}

	override fun renderHint(engine:GameEngine, playerID:Int) {}
}
