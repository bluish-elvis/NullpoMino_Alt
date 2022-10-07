/*
 * Copyright (c) 2022, NullNoname
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

package edu.cuhk.cse.fyp.tetrisai.pyai

import jep.JepConfig
import jep.JepException
import jep.SubInterpreter
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.ai.AIPlayer
import mu.nu.nullpo.game.subsystem.ai.DummyAI
import org.apache.log4j.Logger

/**
 * PyAI - Base class for python AI players
 */
abstract class PyAI(override val name:String = "PyAI", private val scriptPath:String, className:Class<*>?):
	DummyAI(), AIPlayer {
	/** Log  */
	protected var log:Logger = Logger.getLogger(className)
	/** ホールド使用予定  */
	override var bestHold = false
	/** Plan to putX-coordinate  */
	override var bestX = 0
	/** Plan to putY-coordinate  */
	override var bestY = 0
	/** Plan to putDirection  */
	override var bestRt = 0
	/** Current piece number  */
	override var thinkCurrentPieceNo = 0
	/** Peace of thoughts is finished number  */
	override var thinkLastPieceNo = 0
	/** Did the thinking thread finish successfully?  */
	override var thinkComplete = false

	private var jep:SubInterpreter? = null

	init {
		println(jep)
	}

	private fun initJEP() {
		if(jep==null) try {
			jep = SubInterpreter(JepConfig().addSharedModules("numpy")).apply {
				eval("import sys")
				eval("sys.path.append('pyai-scripts')")
				runScript(scriptPath)
				set("AIName", name)
				eval("ai = exportAI(AIName)")
				val v:Any = this.getValue("ai.name")
				log.debug("AI name is $v")
			}
		} catch(j:JepException) {
			log.error("JEP cannot be created!", j)
		}
	}

	override fun init(engine:GameEngine, playerID:Int) {
		try {
			initJEP()
			jep?.run {
				set("engine", engine)
				set("playerID", playerID)
				eval("ai.init(engine, playerID)")
			}
		} catch(j:JepException) {
			log.error("JEP cannot be init!", j)
		}
	}

	override fun newPiece(engine:GameEngine, playerID:Int) {
		try {
			jep?.run {
				set("engine", engine)
				set("playerID", playerID)
				eval("ai.newPiece(engine, playerID)")
			}
		} catch(j:JepException) {
			log.error("Error in newPiece, ", j)
		}
	}

	override fun onFirst(engine:GameEngine, playerID:Int) {
		try {
			jep?.run {
				set("engine", engine)
				set("playerID", playerID)
				eval("ai.onFirst(engine, playerID)")
			}
		} catch(j:JepException) {
			log.error("Error in onFirst, ", j)
		}
	}

	override fun onLast(engine:GameEngine, playerID:Int) {
		try {
			jep?.run {
				set("engine", engine)
				set("playerID", playerID)
				eval("ai.onLast(engine, engine.playerID)")
			}
		} catch(j:JepException) {
			log.error("Error in onLast, ", j)
		}
	}

	override fun renderState(engine:GameEngine, playerID:Int) {
		try {
			jep?.run {
				set("engine", engine)
				set("playerID", playerID)
				eval("ai.renderState(engine, playerID)")
			}
		} catch(j:JepException) {
			log.error("Error in renderState, ", j)
		}
	}

	override fun setControl(engine:GameEngine, playerID:Int, ctrl:Controller):Int =
		try {
			jep?.run {
				set("engine", engine)
				set("playerID", playerID)
				set("ctrl", ctrl)
				eval("ctrlbit = ai.setControl(engine, playerID, ctrl)")
				(this.getValue("ctrlbit") as Int?)
			} ?: 0
		} catch(j:JepException) {
			log.error("Error in setControl, ", j)
			0
		}

	fun shutdown(engine:GameEngine?, playerID:Int) {
		try {
			jep?.run {
				set("engine", engine)
				set("playerID", playerID)
				eval("ai.shutdown(engine, playerID)")
			}
		} catch(j:JepException) {
			log.error("Error in shutdown, ", j)
		}
		try {
			jep?.close()
			jep = null
		} catch(j:JepException) {
			log.error("Error in shutdown, ", j)
		}
	}

	override fun renderHint(engine:GameEngine, playerID:Int) {
		try {
			jep?.run {
				set("engine", engine)
				set("playerID", playerID)
				eval("ai.renderHint(engine, playerID)")
			}
		} catch(j:JepException) {
			log.error("Error in renderHint, ", j)
		}
	}

	operator fun invoke(command:String?) {
		try {
			jep?.eval(command)
		} catch(j:JepException) {
			log.error("Error in invoke, ", j)
		}
	}
}
