/*
 Copyright (c) 2024, NullNoname
 All rights reserved.

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis
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

package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.Rankable
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.util.GeneralUtil.toInt

abstract class AbstractGrand:AbstractMode() {
	/** Number of sections */
	open val sectionMax = 10
	/** Next Section の level (これ-1のときに levelストップする) */
	protected var nextSecLv = 0
	/** Levelが増えた flag */
	protected var lvupFlag = false
	/** Combo bonus */
	private var comboValue = 0

	protected val itemGhost = BooleanMenuItem("alwaysghost", "FULL GHOST", COLOR.BLUE, false)
	/** When true, always ghost ON */
	protected var alwaysGhost:Boolean by DelegateMenuItem(itemGhost)

	protected val itemAlert = BooleanMenuItem("lvstopse", "Sect.ALERT", COLOR.BLUE, true)
	/** When true, levelstop sound is enabled */
	protected var secAlert:Boolean by DelegateMenuItem(itemAlert)

	protected val itemST = BooleanMenuItem("showsectiontime", "SHOW STIME", COLOR.BLUE, true)
	/** When true, section time display is enabled */
	protected var showST:Boolean by DelegateMenuItem(itemST)

	/** medal 状態 */
	protected val medals = Rankable.GrandRow.Medals()
	protected var medalAC get() = medals.AC; set(v) {;medals.AC = v}
	protected var medalsST get() = medals.ST; set(v) {;medals.ST = v}
	protected val medalST get() = medalsST.indexOfFirst {it>0}.let {;if(it>=0) medalsST.size-it else 0;}
	protected var medalSK get() = medals.SK; set(v) {;medals.SK = v}
	/** SK medal conditions: Required Quads */
	open val medalSKQuads = listOf(listOf(10, 20, 30), listOf(1, 2, 4))
	protected var medalRE get() = medals.RE; set(v) {;medals.RE = v}
	/** 150個以上Blockがあるとtrue, 70個まで減らすとfalseになる */
	protected var recoveryFlag = false

	protected var medalRO get() = medals.RO; set(v) {;medals.RO = v}
	/** rotationした合計 count (Maximum4個ずつ増える) */
	protected var spinCount = 0
	/** rotationした合計 count (大セクションごとの合計rotation count to 大セクションごとのPiece配置数) */
	private var sectionSpins = MutableList(3) {0 to 0}

	protected var medalCO get() = medals.CO; set(v) {;medals.CO = v}
	/** CO medal conditions: Required Chains */
	private val medalCOChain = listOf(listOf(3, 4, 5), listOf(2, 3, 4))

	/** Section Time in Current run */
	protected val sectionTime = MutableList(sectionMax) {0}
	/** 新記録が出たSection はtrue */
	protected val sectionIsNewRecord = MutableList(sectionMax) {false}
	/** どこかのSection で新記録を出すとtrue */
	protected val sectionAnyNewRecord get() = sectionIsNewRecord.any()
	/** Cleared Section count */
	protected var sectionsDone = 0
	/** Average Section Time */
	protected val sectionAvgTime
		get() = sectionTime.filter {it>0}.average().toFloat()

	protected var decoration = 0
	protected var decTemp = 0

	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		nextSecLv = 0
		lvupFlag = false

		comboValue = 0

		sectionTime.fill(0)
		sectionIsNewRecord.fill(false)
		sectionsDone = 0
		medals.reset()
		recoveryFlag = false
		spinCount = 0
		sectionSpins.fill(0 to 0)
	}
	/** ST medal check
	 * @param engine GameEngine
	 * @param section Section number
	 */
	protected fun stMedalCheck(engine:GameEngine, section:Int = engine.statistics.level/100, lastTime:Int, best:Int) {
//		val best = bestSectionTime[goalType][section]

		if(lastTime<best||best<=0) {
			engine.playSE("medal3")
			if(medalST<1) decTemp += 3
			if(medalST<2) decTemp += 6
			decTemp += 6
			medalsST[0]++
			if(!owner.replayMode) {
				decTemp++
				sectionIsNewRecord[section] = true
			}
		} else if(lastTime<best+300) {
			engine.playSE("medal2")
			if(medalST<1) decTemp += 3
			medalsST[1]++
			decTemp += 6
		} else if(lastTime<best+600) {
			engine.playSE("medal1")
			medalsST[2]++
			decTemp += 3
		}
	}
	/** RO medal check */
	protected fun roMedalCheck(engine:GameEngine, nextSecLv:Int) {
		val lv = when (nextSecLv) {
			300 -> 0
			700 -> 1
			999 -> 2
			else -> return
		}
		val e = spinCount to engine.statistics.totalPieceLocked-sectionSpins.sumOf {it.second}
		sectionSpins[lv] = e
		spinCount = 0
		(sectionSpins.indexOfLast {(it.first.toFloat()/it.second)>=1.2f}+1).let {
			if(it>medalRO) {
				engine.playSE("medal$it")
				if(medalRO<1) decTemp += 3
				if(medalRO<2) decTemp += 6
				decTemp += 3
				medalRO = lv+1
			}
		}
	}

	/** levelが上がったときの共通処理 */
	open fun levelUp(engine:GameEngine, lu:Int = 0) {
		if(lu<=0) return
		engine.statistics.level += lu
		// Meter
		engine.meterValue = engine.statistics.level%100/99f
		engine.meterColor = GameEngine.METER_COLOR_LEVEL

		if(lu>0&&engine.statistics.level==nextSecLv-1&&secAlert) engine.playSE("levelstop")
		// 速度変更
		setSpeed(engine)

		// RE medal
		if(engine.timerActive&&medalRE<3) {
			val blocks = engine.field.howManyBlocks

			if(!recoveryFlag) {
				if(blocks>=150) recoveryFlag = true
			} else if(blocks<=70) {
				recoveryFlag = false
				decTemp += 5+medalRE// 5 11 18
				engine.playSE("medal${++medalRE}")
			}
		}
		// LV100到達でghost を消す
		engine.ghost = (engine.speed.rank<1f)&&(engine.statistics.level<100||alwaysGhost)
	}

	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			sectionsDone = 0
			decTemp = 0
		}
		return super.onReady(engine)
	}

	override fun onMove(engine:GameEngine):Boolean {
		// 新規ピース出現時
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!lvupFlag) {
			levelUp(engine, (engine.statistics.level<nextSecLv-1).toInt())
		}
		if(engine.ending==0&&engine.statc[0]>0&&!engine.holdDisable) lvupFlag = false

		return false
	}

	override fun onARE(engine:GameEngine):Boolean {
		// 最後の frame
		if(engine.ending==0&&engine.statc[0]>=engine.statc[1]-1&&!lvupFlag) {
			levelUp(engine, (engine.statistics.level<nextSecLv-1).toInt())
			lvupFlag = true
		}

		return false
	}
	/** Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		if(engine.ending!=0) return 0
		// RO medal 用カウント
		spinCount += minOf(4, engine.nowPieceSpinCount)
		return calcScoreGrand(engine, ev)
	}

	fun calcScoreGrand(engine:GameEngine, ev:ScoreEvent):Int {
		val li = ev.lines
		// Combo
		comboValue = if(li==0) 1
		else maxOf(1, comboValue+2*li-2)

		// RO medal 用カウント
		spinCount += minOf(4, engine.nowPieceSpinCount)
		return if(li>=1) {
			// SK medal
			if(li>=4) medalSKQuads[engine.big.toInt()].getOrNull(medalSK)?.let {qua ->
				if(engine.statistics.totalQuadruple>=qua) {
					decTemp += 3+medalSK*2// 3 8 15
					engine.playSE("medal${++medalSK}")
				}
			}
			// Calculate score
			val bravo = if(engine.field.isEmpty) {
				decTemp += li*25
				if(li==3) decTemp += 25
				if(li==4) decTemp += 150
				if(medalAC<3) {
					decTemp += 3+medalAC*4// 3 10 21
					engine.playSE("medal${++medalAC}")
				}
				4
			} else 1

			// CO medal
			if(engine.comboType>0)
				medalCOChain[engine.big.toInt()].getOrNull(medalCO)?.let {qua ->
					if(engine.combo>=qua) {
						decTemp += 3+medalCO// 3 7 12
						engine.playSE("medal${++medalCO}")
					}
				}

			// Level up
			val levelb = engine.statistics.level+li
			val levela = engine.statistics.level+if(li>2) li*2-2 else li
			val twist = if(ev.twist) 2 to 3 else 1 to 2
			((levelb/(4-(ev.b2b>0).toInt())+engine.softdropFall+engine.manualLock.toInt()+engine.harddropFall*2)
				*li*comboValue)*bravo+maxOf(0, engine.lockDelay-engine.lockDelayNow)*7+levela*twist.first/twist.second
		} else 0
	}
}
