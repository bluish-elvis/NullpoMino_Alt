/*
Copyright (c) 2022-2022, NullNoname
Kotlin converted and modified by Venom=Nhelv.
THIS IS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of NullNoname nor the names of its
      contributors may be used to endorse or promote products derived from
      this software without specific prior written permission.

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

package edu.cuhk.cse.fyp.tetrisai.lspi

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.subsystem.ai.BasicAI
import mu.nu.nullpo.game.subsystem.ai.DummyAI
import mu.nu.nullpo.game.subsystem.mode.GameMode
import mu.nu.nullpo.game.subsystem.mode.VSBattle
import mu.nu.nullpo.gui.slick.SlickLog4j
import org.apache.log4j.Logger
import org.apache.log4j.PropertyConfigurator
import mu.nu.nullpo.util.GeneralUtil as Util

/**
Make a new Simulator object, ready to go.
@param mode Game mode Object
@param rules Game rules Object
@param _ai,_ai2 AI object to play (MAKE SURE IT SUPPORTS UNTHREADED !!! )
 */
class Battler(mode:GameMode, rules:RuleOptions, _ai:DummyAI, _ai2:DummyAI) {
	// NOTE(oliver): For other GameModes, look inside src/mu/nu/nullpo/game/subsystem/mode

	private val manager:GameManager = GameManager(mode = mode)
	private val engine get() = manager.engine
	private val receiver get() = manager.receiver
	private var customSeed:String? = null

	init {
		/*
		 * NOTE(oliver): This code is a domain-specific version of
		 * mu.nu.nullpo.gui.slick.StateInGame.startNewGame(String strRulePath)
		 */
		// UI
//		receiver = new RendererSlick();
		// Manager setup
		manager.showInput = false
		manager.engine.forEachIndexed {i, it ->
			// Engine setup
			it.apply {
				ruleOpt = rules
				randomizer = Util.loadRandomizer(rules.strRandomizer)
				wallkick = Util.loadWallkick(rules.strWallkick)
				ai = if(i==0) _ai else _ai2
				aiMoveDelay = 0
				aiThinkDelay = 0
				aiUseThread = false
				aiShowHint = false
				aiPreThink = false
				aiShowState = false
			}
		}
	}

	/*
	 * Get the current in-game grade from a game of Tetris The Grand Master 3: Terror Instinct.
	 *
	 * @return The TGM3 grade.

	*public int getGM3Grade(){
		VSBattle gm3m = (VSBattle) gameEngine.owner.mode;
		Field field;
		try {
			field = gm3m.getClass().getDeclaredField("grade");
			field.setAccessible(true);
			return field.getInt(gm3m);
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		return -1;
	}*/
	/**
	 * Get the current line cleared.
	 *
	 * @return The line cleared
	 */
	fun getLineCleared(gameEngine:GameEngine):Int {
		val aitm:VSBattle = gameEngine.owner.mode as VSBattle
		try {
			return aitm.garbageSent[0]
			//getDeclaredField("garbageSent");
			//return field.getInt(aitm);
			//return field(aitm);
		} catch(e:NoSuchFieldException) {
			e.printStackTrace()
		} catch(e:SecurityException) {
			e.printStackTrace()
		} catch(e:IllegalArgumentException) {
			e.printStackTrace()
		} catch(e:IllegalAccessException) {
			e.printStackTrace()
		}
		return -1
	}
	/** Get the current in-game level. */
	fun getLevel():Int = engine[0].statistics.level

	/**
	 * Make a new Simulator object, ready to go.
	 *
	 * @param mode Game mode Object
	 * @param rulePath path string to the rules file
	 * @param ai AI object to play (MAKE SURE IT SUPPORTS UNTHREADED !!! )
	 */
	constructor(mode:GameMode, rulePath:String, ai:DummyAI):this(mode, Util.loadRule(rulePath), ai, ai)

	/**
	 * Make a new Simulator object, ready to go.
	 *
	 * @param mode Game mode Object
	 * @param rulePath path string to the rules file
	 * @param ai,_ai2 AI object to play (MAKE SURE IT SUPPORTS UNTHREADED !!! )
	 */
	constructor(mode:GameMode, rulePath:String, ai:DummyAI, ai2:DummyAI):this(mode, Util.loadRule(rulePath), ai, ai2)
	/**
	 * Performs a single simulation to completion (STATE == GAMEOVER)
	 */
	private fun runSimulation() {
		engine.forEach {
			// Start a new game.
			it.init()

			//		System.out.println(Long.toString(gameEngine.randSeed, 16));
			customSeed?.let {s ->
				it.randSeed = s.toLong(16)
				log.debug("Engine seed overwritten with $")
			}
			// You have to spend at least 5 frames in the menu before you can start the game.
			for(i in 0..9) it.init()

			// Press and release A to start game.
			it.ctrl.setButtonPressed(Controller.BUTTON_A)
			for(j in 0..9) it.update()

			it.ctrl.setButtonUnpressed(Controller.BUTTON_A)
			while(it.stat!==GameEngine.Status.MOVE) it.update()
		}

		// Run the game until Game Over.
		while(engine.all {it.stat!==GameEngine.Status.GAMEOVER}) {
			for(i in 0..1) {
				val cur = engine[i].nowPieceObject
				do {
					engine[i].update()
					if(engine.any {it.stat===GameEngine.Status.GAMEOVER}) break
				} while(cur==engine[i].nowPieceObject)
				val a = 1
			}
			//			logGameState();
		}

//		log.info("Game is over!");
//		log.info("Final Level: " + gameEngine.statistics.level);
	}
	/**
	 * Performs multiple sequential simulations to completion (STATE == GAMEOVER).
	 *
	 * @param count The number of simulations.
	 */
	fun runSimulations(count:Int) {
		for(i in 1..count) {
			log.info("-------- Simulation %d of %d --------".format(i, count))
			runSimulation()
		}
		//gameEngine._ai.shutdown(gameEngine, 0);
	}
	/**
	 * Performs multiple sequential simulations to completion (STATE == GAMEOVER),
	 * and records statistics.
	 *
	 * @param esp The number of simulations.
	 */
	fun runStatisticsSimulations(esp:Int, step:Int) {
		val totalLines = IntArray(engine.size)
		for(i in totalLines.indices) totalLines[i] = 0
		val count = esp*step
		val winnerCount = IntArray(engine.size)
		for(i in winnerCount.indices) winnerCount[i] = 0
		for(i in 0..<count) {
			log.info("-------- Simulation %d of %d --------".format(i+1, count))
			runSimulation()
			//if (getGM3Grade() == 32) gm++
			val winner:Int = engine.indexOfFirst {it.stat!==GameEngine.Status.GAMEOVER}
			if(winner>=0) winnerCount[winner]++
			log.info("Winner:\t$winner")
			for(j in winnerCount.indices) log.info("win count $j:\t"+winnerCount[j])
			for(j in winnerCount.indices) log.info("win count rate $j:\t"+winnerCount[j].toDouble()/(i+1))
			for(j in engine.indices) {
				//int thisLines = getLineCleared(gameEngine[j]);
				log.info("Player $j")
				val thisLines:Int = (manager.mode as VSBattle).garbageSent[j]
				totalLines[j] += thisLines
				log.info("Line cleared:\t$thisLines")
				log.info("-------- stat till now --------")
				log.info("Total line cleared:\t"+totalLines[j])
				log.info("Line per game:\t"+totalLines[j].toDouble()/(i+1))
			}
			/*			if (i % step == 0) {
//				PyAI pyai = (PyAI) gameEngine._ai;
//				pyai.invoke("sys.getsizeof(_ai.qtable)");
//				pyai.invoke("_ai.saveQTable()");
			}*/
		}
		log.info("-------- COMPLETE --------")
		log.info("Total:\t$count")
		log.info("Total line cleared:\t$totalLines")
		//log.info("Line per game:\t" + String.valueOf(totalLines / count));

		//double prop = (double) totalLines / (double) count;

		//log.info("Sample Proportion:\t" + prop);
		val confidenceMultiplier = 1.96

		//double interval = confidenceMultiplier * Math.sqrt(prop * (1 - prop)/ count );

		//log.info("Confidence interval:\t" + interval);
	}

	companion object {
		// Run for this amount of round
		private const val RUN_ESP = 1
		// Each round run for this amount of simulation and then save Qtable
		private const val RUN_STEP = 100
		var log:Logger = Logger.getLogger(Battler::class.java)
		/**
		 * Log the current game state.
		 */
		/*	private void logGameState()
		//	{
		//		int level = gameEngine.statistics.level;
		//
		//		String piece =
		//			gameEngine.nowPieceObject == null
		//				? " "
		//				: Piece.PIECE_NAMES[gameEngine.nowPieceObject.id];
		//
		//		String state = gameEngine.stat.toString();
		//
		//		log.info("\tLevel: %3d \tPiece: %s \tState: %s".format(level, piece, state));
			}*/
		@JvmStatic fun main(args:Array<String>) {
			// Logger initialization.
			PropertyConfigurator.configure("config/etc/log_slick.cfg")
			org.newdawn.slick.util.Log.setLogSystem(SlickLog4j())
			val mode = VSBattle()
			// NOTE(oliver): For other rules, look inside config/rule.
			val rulePath = "config/rule/StandardAITraining.rul"

			// NOTE(oliver): For other AIs, look inside src/mu/nu/nullpo/game/subsystem/_ai, or src/dk/itu/_ai
			val ai:DummyAI = LSPIAI()
			val ai2:DummyAI = BasicAI()
			//net.tetrisconcept.poochy.nullpomino._ai.PoochyBot()

			// Actual simulation.
			val battler = Battler(mode, rulePath, ai, ai2)

			// Custom seeding.

//		String customSeed =
//			"-2fac0ecd9c988463"
//			"15478945"
//			"897494638"
//			"4697358"
//			;

//		simulator.setCustomSeed(customSeed);

//		simulator.runSimulation();
			battler.runStatisticsSimulations(RUN_ESP, RUN_STEP)
		}
	}
}
