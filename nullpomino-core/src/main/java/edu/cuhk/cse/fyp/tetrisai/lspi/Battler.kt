package edu.cuhk.cse.fyp.tetrisai.lspi

import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.subsystem.ai.BasicAI
import mu.nu.nullpo.game.subsystem.ai.DummyAI
import mu.nu.nullpo.game.subsystem.mode.GameMode
import mu.nu.nullpo.game.subsystem.mode.VSBattleMode
import mu.nu.nullpo.gui.slick.SlickLog4j
import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.PropertyConfigurator

/**
 * Make a new Simulator object, ready to go.
 * @param mode Game mode Object
 * @param rules Game rules Object
 * @param ai,ai2 AI object to play (MAKE SURE IT SUPPORTS UNTHREADED !!! )
 */
class Battler(mode:GameMode, rules:RuleOptions, ai:DummyAI, ai2:DummyAI) {
	private val gameManager:GameManager = GameManager()
	private val gameEngine:Array<GameEngine>
	private val receiver:EventReceiver? = null
	private var customSeed:String? = null
	/*
	 * Get the current in-game grade from a game of Tetris The Grand Master 3: Terror Instinct.
	 *
	 * @return The TGM3 grade.

	*public int getGM3Grade(){
		VSBattleMode gm3m = (VSBattleMode) gameEngine.owner.mode;
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
		val aitm:VSBattleMode = gameEngine.owner.mode as VSBattleMode
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
	/*
	 * Get the current in-game level.
	 *
	 * @return The level.
	 *
		public int getLevel() {
			return gameEngine.statistics.level;
		}
	*//*
	 * Make a new Simulator object, ready to go.
	 *
	 * @param mode Game mode Object
	 * @param rulePath path string to the rules file
	 * @param ai AI object to play (MAKE SURE IT SUPPORTS UNTHREADED !!! )
	 *
		public Battler(GameMode mode, String rulePath, DummyAI ai)
		{
			this(mode, GeneralUtil.loadRule(rulePath), ai);
		}
	*/
	/**
	 * Make a new Simulator object, ready to go.
	 *
	 * @param mode Game mode Object
	 * @param rulePath path string to the rules file
	 * @param ai,ai2 AI object to play (MAKE SURE IT SUPPORTS UNTHREADED !!! )
	 */
	constructor(mode:GameMode, rulePath:String, ai:DummyAI, ai2:DummyAI):this(mode, GeneralUtil.loadRule(rulePath), ai, ai2)
	/**
	 * Performs a single simulation to completion (STATE == GAMEOVER)
	 */
	fun runSimulation() {
		for(i in 0..1) {
			// Start a new game.
			gameEngine[i].init()

			//		System.out.println(Long.toString(gameEngine.randSeed, 16));
			if(customSeed!=null) {
				gameEngine[i].randSeed = customSeed!!.toLong(16)
				log.debug("Engine seed overwritten with $customSeed")
			}
		}

		// You have to spend at least 5 frames in the menu before you can start the game.
		for(i in 0..9) {
			for(j in 0..1) {
				gameEngine[j].update()
			}
		}

		// Press and release A to start game.
		for(i in 0..1) gameEngine[i].ctrl.setButtonPressed(mu.nu.nullpo.game.component.Controller.BUTTON_A)
		for(i in 0..1) for(j in 0..9) {
			gameEngine[i].update()
		}
		for(i in 0..1) gameEngine[i].ctrl.setButtonUnpressed(mu.nu.nullpo.game.component.Controller.BUTTON_A)
		for(i in 0..1) {
			while(gameEngine[i].stat!==GameEngine.Status.MOVE) gameEngine[i].update()
		}

		// Run the game until Game Over.
		while(gameEngine[0].stat!==GameEngine.Status.GAMEOVER&&gameEngine[1].stat!==GameEngine.Status.GAMEOVER) {
			for(i in 0..1) {
				val cur = gameEngine[i].nowPieceObject
				do {
					gameEngine[i].update()
					if(gameEngine[0].stat===GameEngine.Status.GAMEOVER||gameEngine[1].stat===GameEngine.Status.GAMEOVER) break
				} while(cur==gameEngine[i].nowPieceObject)
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
			log.info(String.format("-------- Simulation %d of %d --------", i, count))
			runSimulation()
		}
		//gameEngine.ai.shutdown(gameEngine, 0);
	}
	/**
	 * Performs multiple sequential simulations to completion (STATE == GAMEOVER),
	 * and records statistics.
	 *
	 * @param count The number of simulations.
	 */
	fun runStatisticsSimulations(esp:Int, step:Int) {
		val totalLines = IntArray(2)
		for(i in 0..1) totalLines[i] = 0
		val count = esp*step
		val winnerCount = IntArray(2)
		for(i in 0..1) winnerCount[i] = 0
		for(i in 0 until count) {
			log.info(String.format("-------- Simulation %d of %d --------", i+1, count))
			runSimulation()
			//if (getGM3Grade() == 32) {
			//	gm++;
			//}
			var winner:Int
			if(gameEngine[0].stat!==GameEngine.Status.GAMEOVER&&gameEngine[1].stat===GameEngine.Status.GAMEOVER) {
				winner = 0
				winnerCount[0] += 1
			} else if(gameEngine[1].stat!==GameEngine.Status.GAMEOVER&&gameEngine[0].stat===GameEngine.Status.GAMEOVER) {
				winner = 1
				winnerCount[1] += 1
			} else winner = -1
			log.info("Winner:\t$winner")
			for(j in 0..1) log.info("win count "+j+":\t"+winnerCount[j])
			for(j in 0..1) log.info("win count rate "+j+":\t"+winnerCount[j].toDouble()/(i+1))
			for(j in 0..1) {
				//int thisLines = getLineCleared(gameEngine[j]);
				log.info("Player $j")
				val thisLines:Int = (gameManager.mode as VSBattleMode).garbageSent[j]
				totalLines[j] += thisLines
				log.info("Line cleared:\t$thisLines")
				log.info("-------- stat till now --------")
				log.info("Total line cleared:\t"+totalLines[j])
				log.info("Line per game:\t"+(totalLines[j].toDouble()/(i+1)).toString())
			}
			//			if (i % step == 0) {
//				PyAI pyai = (PyAI) gameEngine.ai;
//				pyai.invoke("sys.getsizeof(ai.qtable)");
//				pyai.invoke("ai.saveQTable()");
//			}
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
	/**
	 * Sets a custom random seed for use in simulations.
	 *
	 * @param seed The seed.
	 */
	fun setCustomSeed(seed:String?) {
		customSeed = seed
	}
	/*
	 * Make a new Simulator object, ready to go.
	 * @param mode Game mode Object
	 * @param rules Game rules Object
	 * @param ai AI object to play (MAKE SURE IT SUPPORTS UNTHREADED !!! )
	 *
	*
	public Battler(GameMode mode, RuleOptions rules, DummyAI ai)
	{
		// UI
		//receiver = new RendererSlick();

		// Manager setup
		gameManager = new GameManager();

		gameManager.mode = mode;

		gameManager.init();

		gameManager.showInput = false;

		// Engine setup
		gameEngine = gameManager.engine[0];

		// - Rules
		gameEngine.ruleOpt = rules;

		// - Randomizer
		gameEngine.randomizer = GeneralUtil.loadRandomizer(rules.strRandomizer);

		// - Wallkick
		gameEngine.wallkick = GeneralUtil.loadWallkick(rules.strWallkick);

		// - AI
		gameEngine.ai = ai;
		gameEngine.aiMoveDelay = 0;
		gameEngine.aiThinkDelay = 0;
		gameEngine.aiUseThread = false;
		gameEngine.aiShowHint = false;
		gameEngine.aiPrethink = false;
		gameEngine.aiShowState = false;
	}
	*/
	init {
		/*
		 * NOTE(oliver): This code is a domain-specific version of
		 * mu.nu.nullpo.gui.slick.StateInGame.startNewGame(String strRulePath)
		 */

		// UI
		//receiver = new RendererSlick();

		// Manager setup
		gameManager.mode = mode
		gameManager.init()
		gameManager.showInput = false
		gameEngine = Array(2) {i ->
			// Engine setup
			gameManager.engine[i].apply {
				ruleOpt = rules
				randomizer = GeneralUtil.loadRandomizer(rules.strRandomizer)
				wallkick = GeneralUtil.loadWallkick(rules.strWallkick)
				this.ai = if(i==0) ai else ai2
				aiMoveDelay = 0
				aiThinkDelay = 0
				aiUseThread = false
				aiShowHint = false
				aiPrethink = false
				aiShowState = false
			}
		}
	}

	companion object {
		// Run for this amount of round
		private const val RUN_ESP = 1
		// Each round run for this amount of simulation and then save Qtable
		private const val RUN_STEP = 100
		var log = org.apache.log4j.Logger.getLogger(Battler::class.java)
		/**
		 * Log the current game state.
		 */
		//	private void logGameState()
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
		//		log.info(String.format("\tLevel: %3d \tPiece: %s \tState: %s", level, piece, state));
		//	}
		@JvmStatic fun main(args:Array<String>) {

			// Logger initialization.
			PropertyConfigurator.configure("config/etc/log_slick.cfg")
			org.newdawn.slick.util.Log.setLogSystem(SlickLog4j())

			// NOTE(oliver): For other GameModes, look inside src/mu/nu/nullpo/game/subsystem/mode
			val mode:GameMode = VSBattleMode()

			// NOTE(oliver): For other rules, look inside config/rule.
			val rulePath = "config/rule/StandardAITraining.rul"

			// NOTE(oliver): For other AIs, look inside src/mu/nu/nullpo/game/subsystem/ai, or src/dk/itu/ai
			val ai:DummyAI = LSPIAI()
			val ai2:DummyAI = BasicAI()
			//new net.tetrisconcept.poochy.nullpomino.ai.PoochyBot();

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
