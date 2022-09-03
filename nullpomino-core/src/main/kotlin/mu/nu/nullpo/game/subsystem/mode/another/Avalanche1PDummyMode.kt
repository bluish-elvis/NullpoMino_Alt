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
package mu.nu.nullpo.game.subsystem.mode.another

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.component.Statistics
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameStyle
import mu.nu.nullpo.game.subsystem.mode.AbstractMode
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** AVALANCHE DUMMY Mode */
abstract class Avalanche1PDummyMode:AbstractMode() {

	val tableSpeedChangeLevel = intArrayOf(80, 90, 96, 97, Integer.MAX_VALUE)

	val tableSpeedValue = intArrayOf(30, 45, 120, 480, -1)

	protected var lastmultiplier = 0

	/** Outline type */
	protected var outlinetype = 0

	/** Flag for all clear */
	protected var zenKeshi = false

	/** Amount of garbage sent */
	protected var garbageSent = 0
	protected var garbageAdd = 0

	/** Number of colors to use */
	protected var numColors = 0

	/** Time to display last chain */
	protected var chainDisplay = 0

	/** Number of all clears */
	protected var zenKeshiCount = 0

	/** Score before adding zenkeshi bonus and max chain bonus */
	protected fun scoreBeforeBonus(st:Statistics):Int = st.scoreLine+st.scoreSD+st.scoreHD

	/** Zenkeshi bonus and max chain bonus amounts */
	protected var zenKeshiBonus = 0
	protected var maxChainBonus = 0

	/** Blocks cleared */
	protected var blocksCleared = 0

	/** Current level */
	protected var level = 0

	/** Maximum level */
	protected val maxLevel = 99

	/** Blocks cleared needed to reach next level */
	protected var toNextLevel = 0

	/** Blocks cleared needed to reach next level */
	protected val blocksPerLevel = 15

	/** True to use slower falling animations, false to use faster */
	protected var cascadeSlow = false

	/** True to use big field display */
	protected var bigDisplay = false

	/** 1 ojama is generated per this many points. */
	protected val ojamaRate = 120

	/** Index of current speed value in table */
	protected var speedIndex = 0

	/* Mode name */
	override val name = "AVALANCHE DUMMY"

	/* Game style */
	override val gameStyle = GameStyle.AVALANCHE

	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastscore = 0
		lastmultiplier = 0

		speedIndex = 0
		outlinetype = 0

		zenKeshi = false
		garbageSent = 0
		garbageAdd = 0
		//firstExtra = false;

		zenKeshiCount = 0
		engine.statistics.maxChain = 0
		zenKeshiBonus = 0
		maxChainBonus = 0
		blocksCleared = 0

		chainDisplay = 0
		level = 5
		toNextLevel = blocksPerLevel

		engine.frameColor = GameEngine.FRAME_COLOR_PURPLE
		engine.clearMode = GameEngine.ClearType.COLOR
		engine.garbageColorClear = true
		engine.colorClearSize = 4
		engine.ignoreHidden = true
		for(i in 0 until Piece.PIECE_COUNT)
			engine.nextPieceEnable[i] = PIECE_ENABLE[i]==1
		engine.randomBlockColor = true
		engine.blockColors = BLOCK_COLORS
		engine.connectBlocks = false
		engine.cascadeDelay = 1
		engine.cascadeClearDelay = 10
		engine.dominoQuickTurn = true
		/* engine.fieldWidth = 6;
 * engine.fieldHeight = 12;
 * engine.fieldHiddenHeight = 2; */
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	open fun setSpeed(engine:GameEngine) {
		if(level<=40) {
			engine.speed.gravity = 1
			engine.speed.denominator = maxOf(43-level-(level%10 shl 1), 2)
		} else {
			engine.speed.denominator = 60
			while(level>=tableSpeedChangeLevel[speedIndex])
				speedIndex++
			engine.speed.gravity = tableSpeedValue[speedIndex]
		}
	}

	override fun onReady(engine:GameEngine):Boolean =
		if(engine.statc[0]==0) readyInit(engine) else false

	protected open fun readyInit(engine:GameEngine):Boolean {
		engine.numColors = numColors
		engine.lineGravityType = if(cascadeSlow) GameEngine.LineGravity.CASCADE_SLOW else GameEngine.LineGravity.CASCADE
		engine.displaySize = if(bigDisplay) 1 else 0

		when(outlinetype) {
			0 -> engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
			1 -> engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_SAMECOLOR
			2 -> engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE
		}

		when(numColors) {
			3 -> level = 1
			4 -> level = 5
			5 -> level = 10
		}
		toNextLevel = blocksPerLevel

		zenKeshiCount = 0
		engine.statistics.maxChain = 0
		zenKeshiBonus = 0
		maxChainBonus = 0
		blocksCleared = 0
		engine.sticky = 2

		return false
	}

	/* Called for initialization during "Ready" screen */
	override fun startGame(engine:GameEngine) {
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE

		engine.twistEnable = false
		engine.useAllSpinBonus = false
		engine.twistAllowKick = false

		engine.speed.are = 30
		engine.speed.areLine = 30
		engine.speed.das = DAS
		engine.speed.lockDelay = 60

		setSpeed(engine)
	}

	override fun onARE(engine:GameEngine):Boolean {
		if(engine.dasCount in 1 until DAS) engine.dasCount = DAS
		return false
	}
	/** Draw X or fever timer on death columns*/
	abstract fun drawX(engine:GameEngine)
	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		if(chainDisplay>0) chainDisplay--
	}

	/* game over */
	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) addBonus(engine)
		return false
	}

	protected open fun addBonus(engine:GameEngine) {
		zenKeshiBonus = when {
			numColors>=5 -> zenKeshiCount*zenKeshiCount*1000
			numColors==4 -> zenKeshiCount*(zenKeshiCount+1)*500
			else -> zenKeshiCount*(zenKeshiCount+3)*250
		}
		maxChainBonus = engine.statistics.maxChain*engine.statistics.maxChain*2000
		engine.statistics.scoreBonus += zenKeshiBonus+maxChainBonus
	}

	/* Called when hard drop used */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreHD += fall
	}

	/* Called when soft drop used */
	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreSD += fall
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, lines:Int):Int {
		if(lines>0) {
			if(zenKeshi) garbageAdd += 30
			if(engine.field.isEmpty) {
				zenKeshi = true
				zenKeshiCount++
				//engine.statistics.score += 2100;
			} else
				zenKeshi = false

			onClear(engine, engine.playerID)
			engine.playSE("combo${minOf(engine.chain, 20)}")

			val pts = calcPts(lines)

			var multiplier = engine.field.colorClearExtraCount
			if(engine.field.colorsCleared>1) multiplier += (engine.field.colorsCleared-1)*2

			multiplier += calcChainMultiplier(engine.chain)

			if(multiplier>999) multiplier = 999
			if(multiplier<1) multiplier = 1

			blocksCleared += lines
			toNextLevel -= lines
			if(toNextLevel<=0&&level<maxLevel) {
				toNextLevel = blocksPerLevel
				level++
			}

			lastscore = pts
			lastmultiplier = multiplier
			val score = pts*multiplier
			engine.statistics.scoreLine += score

			garbageAdd += calcOjama(score, lines, pts, multiplier)

			setSpeed(engine)
			return pts
		}
		return 0
	}

	protected open fun calcOjama(score:Int, avalanche:Int, pts:Int, multiplier:Int):Int = (score+ojamaRate-1)/ojamaRate

	protected open fun calcPts(avalanche:Int):Int = avalanche*10

	protected open fun calcChainMultiplier(chain:Int):Int {
		return when {
			chain==2 -> 8
			chain==3 -> 16
			chain>=4 -> 32*(chain-3)
			else -> 0
		}
	}

	protected open fun onClear(engine:GameEngine, playerID:Int) {
		chainDisplay = 60
	}

	override fun lineClearEnd(engine:GameEngine):Boolean {
		if(garbageAdd>0) {
			garbageSent += garbageAdd
			garbageAdd = 0
		}
		return false
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 1, "PLAY DATA", EventReceiver.COLOR.ORANGE)

		receiver.drawMenuFont(engine, 0, 3, "Score", EventReceiver.COLOR.BLUE)
		val strScoreBefore = String.format("%10d", scoreBeforeBonus(engine.statistics))
		receiver.drawMenuFont(engine, 0, 4, strScoreBefore, EventReceiver.COLOR.GREEN)

		receiver.drawMenuFont(engine, 0, 5, "ZENKESHI", EventReceiver.COLOR.BLUE)
		receiver.drawMenuFont(engine, 0, 6, String.format("%10d", zenKeshiCount))
		val strZenKeshiBonus = "+$zenKeshiBonus"
		receiver.drawMenuFont(engine, 10-strZenKeshiBonus.length, 7, strZenKeshiBonus, EventReceiver.COLOR.GREEN)

		receiver.drawMenuFont(engine, 0, 8, "MAX CHAIN", EventReceiver.COLOR.BLUE)
		receiver.drawMenuFont(engine, 0, 9, String.format("%10d", engine.statistics.maxChain))
		val strMaxChainBonus = "+$maxChainBonus"
		receiver.drawMenuFont(engine, 10-strMaxChainBonus.length, 10, strMaxChainBonus, EventReceiver.COLOR.GREEN)

		receiver.drawMenuFont(engine, 0, 11, "TOTAL", EventReceiver.COLOR.BLUE)
		val strScore = String.format("%10d", engine.statistics.score)
		receiver.drawMenuFont(engine, 0, 12, strScore, EventReceiver.COLOR.RED)

		receiver.drawMenuFont(engine, 0, 13, "Time", EventReceiver.COLOR.BLUE)
		val strTime = String.format("%10s", engine.statistics.time.toTimeStr)
		receiver.drawMenuFont(engine, 0, 14, strTime)
	}

	companion object {
		/** Enabled piece types */
		val PIECE_ENABLE = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0)

		/** Enabled piece types */
		val CHAIN_POWERS_FEVERTYPE = intArrayOf(4, 12, 24, 32, 48, 96, 160, 240, 320, 400, 500, 600, 700, 800, 900, 999)

		/** Block colors */
		val BLOCK_COLORS = arrayOf(
			Block.COLOR.RED, Block.COLOR.GREEN, Block.COLOR.BLUE,
			Block.COLOR.YELLOW, Block.COLOR.PURPLE
		)

		/** Fever values files list */
		val FEVER_MAPS = arrayOf("Fever", "15th", "15thDS", "7", "Poochy7")

		const val DAS = 10
	}
}
