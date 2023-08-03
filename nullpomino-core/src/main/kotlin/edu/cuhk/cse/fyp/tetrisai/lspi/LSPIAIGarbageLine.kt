/*
    Copyright (c) 2010, NullNoname
    All rights reserved.

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

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.play.GameEngine

/**
 * CommonAI
 */
class LSPIAIGarbageLine:LSPIAI() {
	override val name = "LSPI garbageLine"

	/**
	 * Think routine
	 *
	 * @param engine    GameEngine
	 * @param x         X-coordinate
	 * @param y         Y-coordinate
	 * @param rt        Direction
	 * @param rtOld     Direction before rotation (-1: None)
	 * @param fld       Field (Can be modified without problems)
	 * @param piece     Piece
	 * @param nextpiece NEXTPeace
	 * @param holdpiece HOLDPeace(nullMay be)
	 * @param depth     Compromise level (ranges from 0 through getMaxThinkDepth-1)
	 * @return Evaluation score
	 */
	override fun thinkMain(
		engine:GameEngine?, x:Int, y:Int, rt:Int, rtOld:Int, fld:Field, piece:Piece?, nextpiece:Piece?,
		holdpiece:Piece?, depth:Int
	):Double {
		var pts = 0.0
		var oppNowState = this.oppNowState

		// T-Spin flag
		val tspin = (piece!!.type==Piece.Shape.T&&rtOld!=-1&&fld.isTwistSpot(x, y, piece.big))

		// Place the piece
		if(!piece.placeToField(x, y, rt, fld)) return 0.0

		// Line clear
		val lines = fld.checkLine()
		if(lines>0) {
			fld.clearLine()
			fld.downFloatingBlocks()
		}

		// All clear
		val allclear = fld.isEmpty
		if(allclear) pts += 500000.0
		val lineSent = nowState.calLinesSentResult(lines, tspin)

		// Convert to future State
		oppFutureState = FutureState()
		if(twoPlayerGame) {
			val oppEngine:GameEngine = engine!!.owner.engine[1-engine.playerID]
			val oppFld = Field()
			val oppNow = Piece()
//			val oppLines = 0
			val saveNowPiece = oppEngine.nowPieceObject
			if(saveNowPiece!=null) {
				oppFld.replace(oppEngine.field)
				oppNow.replace(saveNowPiece)
				//thinkbestOppPosition(oppEngine, 1 - engine.playerID);
				oppFld.addSingleHoleGarbage((Math.random()*width).toInt(), Block.COLOR.BLACK, 0, lineSent)
			}
			oppNowState = createState(oppFld, oppEngine)
			val oppLineSent = oppNowState.calLinesSentResult(lines, tspin)
			oppFutureState = createFutureState(oppNowState, oppFld, oppEngine, 0, 0, 0)

			// My future state
			futureState = createFutureState(nowState, fld, engine, 0, lineSent, lines)
		} else {
			// My future state
			futureState = createFutureState(nowState, fld, engine, 0, lineSent, lines)
		}
		val f:DoubleArray?
		if(twoPlayerGame) {
			f = player.twoPlayerBasisFunctions.getFutureArray(nowState, futureState, oppNowState, oppFutureState)
			for(i in 0..<TwoPlayerBasisFunction.FEATURE_COUNT) {
				pts += f[i]*player.twoPlayerBasisFunctions.weight[i]
				//log.error(i + ":" + f[i]);
			}
		} else {
			f = player.basisFunctions.getFeatureArray(nowState, futureState)
			for(i in 0..<BasisFunction.FUTURE_COUNT) {
				pts += f[i]*player.basisFunctions.weight[i]
				//log.error(i + ":" + f[i]);
			}
		}
		lastf = f

		//log.error(pts);
		return pts
	}

}
