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
import mu.nu.nullpo.game.play.GameEngine

/** AIInterface */
interface AIPlayer {
	/** NameGet the
	 * @return AIOfName
	 */
	val name:String

	/** Called at initialization
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	fun init(engine:GameEngine, playerID:Int)

	/** End processing */
	fun shutdown()

	/** Set button input states
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 * @param ctrl Button inputState management class
	 */
	fun setControl(engine:GameEngine, playerID:Int, ctrl:Controller)

	/** Called at the start of each frame
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	fun onFirst(engine:GameEngine, playerID:Int)

	/** Called after every frame
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	fun onLast(engine:GameEngine, playerID:Int)

	/** Called to display internal state
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	fun renderState(engine:GameEngine, playerID:Int)

	/** Happens when a new piece appeared
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	fun newPiece(engine:GameEngine, playerID:Int)

	/** Called to display additional hint information
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	fun renderHint(engine:GameEngine, playerID:Int)
}
