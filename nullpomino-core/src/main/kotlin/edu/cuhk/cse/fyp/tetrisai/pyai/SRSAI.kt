package edu.cuhk.cse.fyp.tetrisai.pyai

import org.apache.log4j.Logger


/**
 * SRSAI - PvP AI Class
 */
class SRSAI:PyAI("SRSAI", scriptPath, SRSAI::class.java) {

	companion object {
		/** Log  */
		var log:Logger = Logger.getLogger(SRSAI::class.java)
		private var scriptPath = "pyai-scripts/PvPAI.py"
	}
}
