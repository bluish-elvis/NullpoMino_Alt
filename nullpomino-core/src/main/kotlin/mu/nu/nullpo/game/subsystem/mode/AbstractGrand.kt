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

import mu.nu.nullpo.game.event.Rankable
import mu.nu.nullpo.game.play.GameEngine

abstract class AbstractGrand:AbstractMode() {
	open val SECTION_MAX = 10
	/** Next Section の level (これ-1のときに levelストップする) */
	protected var nextSecLv = 0
	/** Levelが増えた flag */
	protected var lvupFlag = false
	/** medal 状態 */
	protected val medals = Rankable.GrandRow.Medals()
	protected var medalAC get() = medals.AC; set(v) {;medals.AC = v}
	protected var medalsST get() = medals.ST; set(v) {;medals.ST = v}
	protected val medalST get() = medalsST.indexOfFirst {it>0}.let {;if(it>=0) medalsST.size-it else 0;}
	protected var medalSK get() = medals.SK; set(v) {;medals.SK = v}

	protected var medalRE get() = medals.RE; set(v) {;medals.RE = v}
	/** 150個以上Blockがあるとtrue, 70個まで減らすとfalseになる */
	protected var recoveryFlag = false

	protected var medalRO get() = medals.RO; set(v) {;medals.RO = v}
	/** rotationした合計 count (Maximum4個ずつ増える) */
	protected var spinCount = 0
	/** rotationした合計 count (大セクションごとの合計rotation count to 大セクションごとのPiece配置数) */
	private var sectionSpins = MutableList(3) {0 to 0}


	protected var medalCO get() = medals.CO; set(v) {;medals.CO = v}

	/** Section Time in Current run */
	protected val sectionTime = MutableList(SECTION_MAX) {0}
	/** 新記録が出たSection はtrue */
	protected val sectionIsNewRecord = MutableList(SECTION_MAX) {false}
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
		val lv = when {
			nextSecLv==300 -> 0
			nextSecLv==700 -> 1
			nextSecLv==999 -> 2
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

}
