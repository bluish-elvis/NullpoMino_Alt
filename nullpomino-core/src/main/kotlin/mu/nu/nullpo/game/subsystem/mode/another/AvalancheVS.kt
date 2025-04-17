/*
 Copyright (c) 2010-2024, NullNoname
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
package mu.nu.nullpo.game.subsystem.mode.another

import mu.nu.nullpo.game.component.BGM
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.GameKeyDummy.Companion.MAX_PLAYERS
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import kotlin.random.Random

/** AVALANCHE VS-BATTLE mode (Release Candidate 1) */
class AvalancheVS:AvalancheVSDummyMode() {
	/** Version */
	private var version = 0

	/** Fever points needed to enter Fever Mode */
	private var feverThreshold = IntArray(0)

	/** Fever points */
	private var feverPoints = IntArray(0)

	/** Fever time */
	private var feverTime = IntArray(0)

	/** Minimum and maximum fever time */
	private var feverTimeMin = IntArray(0)
	private var feverTimeMax = IntArray(0)

	/** Flag set to true when player is in Fever Mode */
	private var inFever = BooleanArray(0)

	/** Backup fields for Fever Mode */
	private var feverBackupField:Array<Field> = emptyArray()

	/** Time added to limit */
	private var feverTimeLimitAdd = IntArray(0)

	/** Time to display added time */
	private var feverTimeLimitAddDisplay = IntArray(0)

	/** Second ojama counter for Fever Mode */
	private var ojamaFever = IntArray(0)

	/** Set to true when opponent starts chain while in Fever Mode */
	private var ojamaAddToFever = BooleanArray(0)

	/** Chain levels for Fever Mode */
	private var feverChain = IntArray(0)

	/** Criteria to add a fever point */
	private var feverPointCriteria = IntArray(0)

	/** Criteria to add 1 second of fever time */
	private var feverTimeCriteria = IntArray(0)

	/** Fever power multiplier */
	private var feverPower = IntArray(0)

	/** Initial fever chain */
	private var feverChainStart = IntArray(0)

	/** True to show fever points as meter, false to show numerical counts */
	private var feverShowMeter = BooleanArray(0)

	/** True to show ojama on meter, false to show fever points */
	private var ojamaMeter = BooleanArray(0)

	/** Zenkeshi preset chain size */
	private var zenKeshiChain = IntArray(0)

	/** Zenkeshi ojama bonus */
	private var zenKeshiOjama = IntArray(0)

	/** Indices for values previews */
	private var previewChain = IntArray(0)
	private var previewSubset = IntArray(0)

	/** ??? */
	private var xyzzy = 0

	/* Mode name */
	override val name = "AVALANCHE VS-BATTLE (RC1)"

	/* Mode initialization */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		feverThreshold = IntArray(MAX_PLAYERS)
		feverPoints = IntArray(MAX_PLAYERS)
		feverTime = IntArray(MAX_PLAYERS)
		feverTimeMin = IntArray(MAX_PLAYERS)
		feverTimeMax = IntArray(MAX_PLAYERS)
		inFever = BooleanArray(MAX_PLAYERS)
		feverBackupField = Array(MAX_PLAYERS) {manager.engine[it].field}
		feverTimeLimitAdd = IntArray(MAX_PLAYERS)
		feverTimeLimitAddDisplay = IntArray(MAX_PLAYERS)
		ojamaFever = IntArray(MAX_PLAYERS)
		ojamaAddToFever = BooleanArray(MAX_PLAYERS)
		feverChain = IntArray(MAX_PLAYERS)
		feverShowMeter = BooleanArray(MAX_PLAYERS)
		ojamaMeter = BooleanArray(MAX_PLAYERS)
		feverPointCriteria = IntArray(MAX_PLAYERS)
		feverTimeCriteria = IntArray(MAX_PLAYERS)
		feverPower = IntArray(MAX_PLAYERS)
		feverChainStart = IntArray(MAX_PLAYERS)
		zenKeshiChain = IntArray(MAX_PLAYERS)
		zenKeshiOjama = IntArray(MAX_PLAYERS)
		previewChain = IntArray(MAX_PLAYERS)
		previewSubset = IntArray(MAX_PLAYERS)
		for(i in 0..<MAX_PLAYERS) {
			previewChain[i] = 5
			previewSubset[i] = 0
		}
		xyzzy = 0
	}

	/** Load settings into [engine] from [prop] not related to speeds */
	private fun loadOtherSetting(engine:GameEngine, prop:CustomProperties) {
		super.loadOtherSetting(engine, prop, "")
		val playerID = engine.playerID
		ojamaRate[playerID] = prop.getProperty("avalanchevs.ojamaRate.p$playerID", 120)
		ojamaHard[playerID] = prop.getProperty("avalanchevs.ojamaHard.p$playerID", 0)
		feverThreshold[playerID] = prop.getProperty("avalanchevs.feverThreshold.p$playerID", 0)
		feverTimeMin[playerID] = prop.getProperty("avalanchevs.feverTimeMin.p$playerID", 15)
		feverTimeMax[playerID] = prop.getProperty("avalanchevs.feverTimeMax.p$playerID", 30)
		feverShowMeter[playerID] = prop.getProperty("avalanchevs.feverShowMeter.p$playerID", true)
		ojamaMeter[playerID] = prop.getProperty("avalanchevs.ojamaMeter.p$playerID", true)
		feverPointCriteria[playerID] = prop.getProperty("avalanchevs.feverPointCriteria.p$playerID", 0)
		feverTimeCriteria[playerID] = prop.getProperty("avalanchevs.feverTimeCriteria.p$playerID", 0)
		feverPower[playerID] = prop.getProperty("avalanchevs.feverPower.p$playerID", 10)
		feverChainStart[playerID] = prop.getProperty("avalanchevs.feverChainStart.p$playerID", 5)
		zenKeshiChain[playerID] = prop.getProperty("avalanchevs.zenKeshiChain.p$playerID", 4)
		zenKeshiOjama[playerID] = prop.getProperty("avalanchevs.zenKeshiOjama.p$playerID", 30)
		if(owner.replayMode&&prop.getProperty("avalanchevs.debugcheatenable", false)) xyzzy = 573
	}

	/** Save settings from [engine] into [prop] not related to speeds */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		super.saveOtherSetting(engine, prop, "")
		val playerID = engine.playerID
		prop.setProperty("avalanchevs.feverThreshold.p$playerID", feverThreshold[playerID])
		prop.setProperty("avalanchevs.feverTimeMin.p$playerID", feverTimeMin[playerID])
		prop.setProperty("avalanchevs.feverTimeMax.p$playerID", feverTimeMax[playerID])
		prop.setProperty("avalanchevs.feverShowMeter.p$playerID", feverShowMeter[playerID])
		prop.setProperty("avalanchevs.ojamaMeter.p$playerID", ojamaMeter[playerID])
		prop.setProperty("avalanchevs.feverPointCriteria.p$playerID", feverPointCriteria[playerID])
		prop.setProperty("avalanchevs.feverTimeCriteria.p$playerID", feverTimeCriteria[playerID])
		prop.setProperty("avalanchevs.feverPower.p$playerID", feverPower[playerID])
		prop.setProperty("avalanchevs.feverChainStart.p$playerID", feverChainStart[playerID])
		prop.setProperty("avalanchevs.zenKeshiChain.p$playerID", zenKeshiChain[playerID])
		prop.setProperty("avalanchevs.zenKeshiOjama.p$playerID", zenKeshiOjama[playerID])
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		val pid = engine.playerID
		ojamaFever[pid] = 0
		feverPoints[pid] = 0
		feverTime[pid] = feverTimeMin[pid]*60
		feverTimeLimitAdd[pid] = 0
		feverTimeLimitAddDisplay[pid] = 0
		inFever[pid] = false
		feverBackupField[pid].reset()

		version = if(!engine.owner.replayMode) {
			loadOtherSetting(engine, engine.owner.modeConfig)
			loadPreset(engine, engine.owner.modeConfig, -1-pid, "")
			CURRENT_VERSION
		} else {
			loadOtherSetting(engine, engine.owner.replayProp)
			loadPreset(engine, engine.owner.replayProp, -1-pid, "")
			owner.replayProp.getProperty("avalanchevs.version", 0)
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		val playerID = engine.playerID
		// Menu
		if(!engine.owner.replayMode&&engine.statc[4]==0) {
			// Configuration changes
			val change = updateCursor(engine, if(xyzzy==573) 46 else 43)

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl.isPress(Controller.BUTTON_E)) m = 100
				if(engine.ctrl.isPress(Controller.BUTTON_F)) m = 1000

				when(menuCursor) {
					0 -> engine.speed.gravity = rangeCursor(engine.speed.gravity+change*m, -1, 99999)
					1 -> engine.speed.denominator = rangeCursor(change*m, -1, 99999)
					2 -> engine.speed.are = rangeCursor(engine.speed.are+change, 0, 99)
					3 -> engine.speed.areLine = rangeCursor(engine.speed.areLine+change, 0, 99)
					4 -> engine.speed.lineDelay = rangeCursor(engine.speed.lineDelay+change, 0, 99)
					5 -> engine.speed.lockDelay = rangeCursor(engine.speed.lockDelay+change*minOf(m, 10), 0, 999)
					6 -> engine.speed.das = rangeCursor(engine.speed.das+change, 0, 99)
					7 -> {
						engine.cascadeDelay += change
						if(engine.cascadeDelay<0) engine.cascadeDelay = 20
						if(engine.cascadeDelay>20) engine.cascadeDelay = 0
					}
					8 -> {
						engine.cascadeClearDelay += change
						if(engine.cascadeClearDelay<0) engine.cascadeClearDelay = 99
						if(engine.cascadeClearDelay>99) engine.cascadeClearDelay = 0
					}
					9 -> {
						ojamaCounterMode[playerID] += change
						if(ojamaCounterMode[playerID]<0) ojamaCounterMode[playerID] = 2
						if(ojamaCounterMode[playerID]>2) ojamaCounterMode[playerID] = 0
					}
					10 -> {
						if(m>=10)
							maxAttack[playerID] += change*10
						else
							maxAttack[playerID] += change
						if(maxAttack[playerID]<0) maxAttack[playerID] = 99
						if(maxAttack[playerID]>99) maxAttack[playerID] = 0
					}
					11 -> {
						numColors[playerID] += change
						if(numColors[playerID]<3) numColors[playerID] = 5
						if(numColors[playerID]>5) numColors[playerID] = 3
					}
					12 -> {
						rensaShibari[playerID] += change
						if(rensaShibari[playerID]<1) rensaShibari[playerID] = 20
						if(rensaShibari[playerID]>20) rensaShibari[playerID] = 1
					}
					13 -> {
						engine.colorClearSize += change
						if(engine.colorClearSize<2) engine.colorClearSize = 36
						if(engine.colorClearSize>36) engine.colorClearSize = 2
					}
					14 -> {
						if(m>=10)
							ojamaRate[playerID] += change*100
						else
							ojamaRate[playerID] += change*10
						if(ojamaRate[playerID]<10) ojamaRate[playerID] = 1000
						if(ojamaRate[playerID]>1000) ojamaRate[playerID] = 10
					}
					15 -> {
						if(m>10)
							hurryUpSeconds[playerID] += change*m/10
						else
							hurryUpSeconds[playerID] += change
						if(hurryUpSeconds[playerID]<0) hurryUpSeconds[playerID] = 300
						if(hurryUpSeconds[playerID]>300) hurryUpSeconds[playerID] = 0
					}
					16 -> newChainPower[playerID] = !newChainPower[playerID]
					17 -> {
						outlineType[playerID] += change
						if(outlineType[playerID]<0) outlineType[playerID] = 2
						if(outlineType[playerID]>2) outlineType[playerID] = 0
					}
					18 -> {
						chainDisplayType[playerID] += change
						if(chainDisplayType[playerID]<0) chainDisplayType[playerID] = 3
						if(chainDisplayType[playerID]>3) chainDisplayType[playerID] = 0
					}
					19 -> cascadeSlow[playerID] = !cascadeSlow[playerID]
					20 -> big[playerID] = !big[playerID]
					21 -> {
						ojamaHard[playerID] += change
						if(ojamaHard[playerID]<0) ojamaHard[playerID] = 9
						if(ojamaHard[playerID]>9) ojamaHard[playerID] = 0
					}
					22 -> dangerColumnDouble[playerID] = !dangerColumnDouble[playerID]
					23 -> dangerColumnShowX[playerID] = !dangerColumnShowX[playerID]
					24 -> {
						zenKeshiType[playerID] += change
						if(zenKeshiType[playerID]<0) zenKeshiType[playerID] = 2
						if(zenKeshiType[playerID]>2) zenKeshiType[playerID] = 0
					}
					25 -> if(zenKeshiType[playerID]==ZENKESHI_MODE_FEVER) {
						zenKeshiChain[playerID] += change
						if(zenKeshiChain[playerID]<feverChainMin[playerID])
							zenKeshiChain[playerID] = feverChainMax[playerID]
						if(zenKeshiChain[playerID]>feverChainMax[playerID])
							zenKeshiChain[playerID] = feverChainMin[playerID]
					} else {
						if(m>=10)
							zenKeshiOjama[playerID] += change*10
						else
							zenKeshiOjama[playerID] += change
						if(zenKeshiOjama[playerID]<1) zenKeshiOjama[playerID] = 99
						if(zenKeshiOjama[playerID]>99) zenKeshiOjama[playerID] = 1
					}
					26 -> {
						feverThreshold[playerID] += change
						if(feverThreshold[playerID]<0) feverThreshold[playerID] = 9
						if(feverThreshold[playerID]>9) feverThreshold[playerID] = 0
					}
					27, 44 -> {
						feverMapSet[playerID] += change
						if(feverMapSet[playerID]<0) feverMapSet[playerID] = FEVER_MAPS.size-1
						if(feverMapSet[playerID]>=FEVER_MAPS.size) feverMapSet[playerID] = 0
						loadMapSetFever(engine, feverMapSet[playerID], true)
						if(zenKeshiChain[playerID]<feverChainMin[playerID])
							zenKeshiChain[playerID] = feverChainMax[playerID]
						if(zenKeshiChain[playerID]>feverChainMax[playerID])
							zenKeshiChain[playerID] = feverChainMin[playerID]
						if(feverChainStart[playerID]<feverChainMin[playerID])
							feverChainStart[playerID] = feverChainMax[playerID]
						if(feverChainStart[playerID]>feverChainMax[playerID])
							feverChainStart[playerID] = feverChainMin[playerID]
						if(previewChain[playerID]<feverChainMin[playerID]) previewChain[playerID] = feverChainMax[playerID]
						if(previewChain[playerID]>feverChainMax[playerID]) previewChain[playerID] = feverChainMin[playerID]
						if(previewSubset[playerID]>=feverMapSubsets[playerID].size) previewSubset[playerID] = 0
					}
					28 -> {
						if(m>=10)
							feverTimeMin[playerID] += change*10
						else
							feverTimeMin[playerID] += change
						if(feverTimeMin[playerID]<1) feverTimeMin[playerID] = feverTimeMax[playerID]
						if(feverTimeMin[playerID]>feverTimeMax[playerID]) feverTimeMin[playerID] = 1
					}
					29 -> {
						if(m>=10)
							feverTimeMax[playerID] += change*10
						else
							feverTimeMax[playerID] += change
						if(feverTimeMax[playerID]<feverTimeMin[playerID]) feverTimeMax[playerID] = 99
						if(feverTimeMax[playerID]>99) feverTimeMax[playerID] = feverTimeMin[playerID]
					}
					30 -> feverShowMeter[playerID] = !feverShowMeter[playerID]
					31 -> {
						feverPointCriteria[playerID] += change
						if(feverPointCriteria[playerID]<0) feverPointCriteria[playerID] = 2
						if(feverPointCriteria[playerID]>2) feverPointCriteria[playerID] = 0
					}
					32 -> {
						feverTimeCriteria[playerID] += change
						if(feverTimeCriteria[playerID]<0) feverTimeCriteria[playerID] = 1
						if(feverTimeCriteria[playerID]>1) feverTimeCriteria[playerID] = 0
					}
					33 -> {
						feverPower[playerID] += change
						if(feverPower[playerID]<0) feverPower[playerID] = 20
						if(feverPower[playerID]>20) feverPower[playerID] = 0
					}
					34 -> {
						feverChainStart[playerID] += change
						if(feverChainStart[playerID]<feverChainMin[playerID])
							feverChainStart[playerID] = feverChainMax[playerID]
						if(feverChainStart[playerID]>feverChainMax[playerID])
							feverChainStart[playerID] = feverChainMin[playerID]
					}
					35 -> ojamaMeter[playerID] = feverThreshold[playerID]>0||!ojamaMeter[playerID]
					36 -> {
						useMap[playerID] = !useMap[playerID]
						if(!useMap[playerID]) engine.field.reset() else
							loadMapPreview(engine, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
					}
					37 -> {
						mapSet[playerID] += change
						if(mapSet[playerID]<0) mapSet[playerID] = 99
						if(mapSet[playerID]>99) mapSet[playerID] = 0
						if(useMap[playerID]) {
							mapNumber[playerID] = -1
							loadMapPreview(engine, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
						}
					}
					38 -> if(useMap[playerID]) {
						mapNumber[playerID] += change
						if(mapNumber[playerID]<-1) mapNumber[playerID] = mapMaxNo[playerID]-1
						if(mapNumber[playerID]>mapMaxNo[playerID]-1) mapNumber[playerID] = -1
						loadMapPreview(engine, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
					} else
						mapNumber[playerID] = -1
					39 -> bgmId = rangeCursor(bgmId+change, 0, BGM.count-1)
					40 -> enableSE[playerID] = !enableSE[playerID]
					41 -> bigDisplay = !bigDisplay
					42, 43 -> presetNumber[playerID] = rangeCursor(presetNumber[playerID]+change, 0, 99)
					45 -> {
						previewSubset[playerID] += change
						if(previewSubset[playerID]<0) previewSubset[playerID] = feverMapSubsets[playerID].size-1
						if(previewSubset[playerID]>=feverMapSubsets[playerID].size) previewSubset[playerID] = 0
					}
					46 -> {
						previewChain[playerID] += change
						if(previewChain[playerID]<feverChainMin[playerID]) previewChain[playerID] = feverChainMax[playerID]
						if(previewChain[playerID]>feverChainMax[playerID]) previewChain[playerID] = feverChainMin[playerID]
					}
				}
			}

			if(xyzzy!=573&&playerID==0) {
				if(engine.ctrl.isPush(Controller.BUTTON_UP))
					if(xyzzy==1)
						xyzzy++
					else if(xyzzy!=2) xyzzy = 1
				if(engine.ctrl.isPush(Controller.BUTTON_DOWN))
					if(xyzzy==2||xyzzy==3)
						xyzzy++
					else
						xyzzy = 0
				if(engine.ctrl.isPush(Controller.BUTTON_LEFT))
					if(xyzzy==4||xyzzy==6)
						xyzzy++
					else
						xyzzy = 0
				if(engine.ctrl.isPush(Controller.BUTTON_RIGHT))
					if(xyzzy==5||xyzzy==7)
						xyzzy++
					else
						xyzzy = 0
			}

			// 決定
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")

				if(xyzzy==573&&menuCursor>43)
					loadFeverMap(engine, Random.Default, previewChain[playerID], previewSubset[playerID])
				else if(xyzzy==9&&playerID==0) {
					engine.playSE("levelup")
					xyzzy = 573
				} else if(menuCursor==42)
					loadPreset(engine, owner.modeConfig, presetNumber[playerID], "")
				else if(menuCursor==43) {
					savePreset(engine, owner.modeConfig, presetNumber[playerID], "")
					owner.saveModeConfig()
				} else {
					saveOtherSetting(engine, owner.modeConfig)
					savePreset(engine, owner.modeConfig, -1-playerID, "")
					owner.saveModeConfig()
					engine.statc[4] = 1
				}
			}

			if(engine.ctrl.isPush(Controller.BUTTON_B))
				if(xyzzy==8&&playerID==0)
					xyzzy++
				else
				// Cancel
					engine.quitFlag = true

			// プレビュー用Map読み込み
			if(useMap[playerID]&&menuTime==0)
				loadMapPreview(engine, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)

			// Random values preview
			if(useMap[playerID]&&propMap[playerID]!=null&&mapNumber[playerID]<0)
				if(menuTime%30==0) {
					engine.statc[5]++
					if(engine.statc[5]>=mapMaxNo[playerID]) engine.statc[5] = 0
					loadMapPreview(engine, engine.statc[5], false)
				}
		} else if(engine.statc[4]==0) {
			menuTime++
			menuCursor = 0

			when {
				menuTime>=300 -> engine.statc[4] = 1
				menuTime>=240 -> menuCursor = 36
				menuTime>=180 -> menuCursor = 26
				menuTime>=120 -> menuCursor = 17
				menuTime>=60 -> menuCursor = 9
			}
		} else // Start
			if(owner.engine[0].statc[4]==1&&owner.engine[1].statc[4]==1&&playerID==1) {
				owner.engine[0].stat = GameEngine.Status.READY
				owner.engine[1].stat = GameEngine.Status.READY
				owner.engine[0].resetStatc()
				owner.engine[1].resetStatc()
			} else if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.statc[4] = 0// Cancel

		return true
	}

	/* Setting screen drawing */
	override fun renderSetting(engine:GameEngine) {
		if(engine.statc[4]==0) {
			val pid = engine.playerID
			when {
				menuCursor<9 -> {
					drawMenuSpeeds(engine, receiver, 0, COLOR.ORANGE, 0)
					drawMenu(engine, receiver, "FALL DELAY" to engine.cascadeDelay, "CLEAR DELAY" to engine.cascadeClearDelay)

					receiver.drawMenuFont(engine, 0, 21, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE 1/5", COLOR.YELLOW)
				}
				menuCursor<17 -> {
					drawMenu(
						engine, receiver, 0, COLOR.CYAN, 9,
						"COUNTER" to OJAMA_COUNTER_STRING[ojamaCounterMode[pid]],
						"MAX ATTACK" to maxAttack[pid],
						"COLORS" to numColors[pid],
						"MIN CHAIN" to rensaShibari[pid],
						"CLEAR SIZE" to engine.colorClearSize,
						"OJAMA RATE" to ojamaRate[pid],
						"HURRYUP" to if(hurryUpSeconds[pid]==0) "NONE" else "${hurryUpSeconds[pid]}SEC",
						"CHAINPOWER" to if(newChainPower[pid]) "FEVER" else "CLASSIC"
					)

					receiver.drawMenuFont(engine, 0, 21, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE 2/5", COLOR.YELLOW)
				}
				menuCursor<26 -> {
					drawMenu(
						engine, receiver, 0, COLOR.COBALT, 17,
						"OUTLINE" to OUTLINE_TYPE_NAMES[outlineType[pid]],
						"SHOW CHAIN" to CHAIN_DISPLAY_NAMES[chainDisplayType[pid]],
						"FALL ANIM" to if(cascadeSlow[pid]) "FEVER" else "CLASSIC"
					)
					drawMenu(engine, receiver, COLOR.CYAN, "BIG" to big[pid])
					if(big[pid]) menuColor = COLOR.WHITE
					drawMenu(
						engine, receiver, "HARD OJAMA" to ojamaHard[pid],
						"X COLUMN" to if(dangerColumnDouble[pid]) "3 AND 4" else "3 ONLY",
						"X SHOW" to dangerColumnShowX[pid],
						"ZENKESHI" to ZENKESHI_TYPE_NAMES[zenKeshiType[pid]]
					)
					if(zenKeshiType[pid]==ZENKESHI_MODE_OFF) menuColor = COLOR.WHITE
					drawMenu(
						engine, receiver, "ZK-BONUS" to
							if(zenKeshiType[pid]==ZENKESHI_MODE_FEVER) "${zenKeshiChain[pid]} CHAIN" else "${zenKeshiOjama[pid]} OJAMA"
					)
					receiver.drawMenuFont(engine, 0, 21, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE 3/5", COLOR.YELLOW)
				}
				menuCursor<36 -> {
					drawMenu(
						engine, receiver, 0, if(big[pid]) COLOR.WHITE else COLOR.PURPLE, 26, "FEVER" to if(feverThreshold[pid]==0)
							"NONE" else "${feverThreshold[pid]} PTS"
					)
					if(feverThreshold[pid]==0&&zenKeshiType[pid]!=ZENKESHI_MODE_FEVER)
						menuColor = COLOR.WHITE
					drawMenu(engine, receiver, "F-MAP SET" to FEVER_MAPS[feverMapSet[pid]].uppercase())
					if(feverThreshold[pid]==0) menuColor = COLOR.WHITE
					drawMenu(
						engine, receiver, "F-MIN TIME" to "${feverTimeMin[pid]} SEC",
						"F-MAX TIME" to "${feverTimeMax[pid]} SEC",
						"F-DISPLAY" to if(feverShowMeter[pid]) "METER" else "COUNT",
						"F-ADDPOINT" to FEVER_POINT_CRITERIA_NAMES[feverPointCriteria[pid]],
						"F-ADDTIME" to FEVER_TIME_CRITERIA_NAMES[feverTimeCriteria[pid]],
						"F-POWER" to "${feverPower[pid]*10}%", "F-1STCHAIN" to "${feverChainStart[pid]}",
						"SIDE METER" to if(ojamaMeter[pid]||feverThreshold[pid]==0) "OJAMA" else "FEVER"
					)

					receiver.drawMenuFont(engine, 0, 21, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE 4/5", COLOR.YELLOW)
				}
				menuCursor<44
				-> {
					drawMenu(
						engine,
						receiver,
						0,
						COLOR.PINK,
						36,
						"USE MAP" to useMap[pid],
						"MAP SET" to mapSet[pid],
						"MAP NO." to if(mapNumber[pid]<0) "RANDOM" else "${mapNumber[pid]}/${mapMaxNo[pid]-1}"
					)
					drawMenu(engine, receiver, COLOR.COBALT, "BGM" to BGM.values[bgmId])
					drawMenu(engine, receiver, COLOR.YELLOW, "SE" to enableSE[pid])
					drawMenu(engine, receiver, COLOR.COBALT, "BIG DISP" to bigDisplay)
					drawMenu(engine, receiver, COLOR.GREEN, "LOAD" to presetNumber[pid], "SAVE" to presetNumber[pid])

					receiver.drawMenuFont(engine, 0, 21, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE 5/5", COLOR.YELLOW)
				}
				else -> {
					receiver.drawMenuFont(engine, 0, 13, "MAP PREVIEW", COLOR.YELLOW)
					receiver.drawMenuFont(engine, 0, 14, "A:DISPLAY", COLOR.GREEN)
					drawMenu(
						engine,
						receiver,
						15,
						COLOR.BLUE,
						44,
						"F-MAP SET" to FEVER_MAPS[feverMapSet[pid]].uppercase(),
						"SUBSET" to feverMapSubsets[pid][previewSubset[pid]].uppercase(),
						"CHAIN" to previewChain[pid]
					)
				}
			}
		} else
			receiver.drawMenuFont(engine, 3, 10, "WAIT", COLOR.YELLOW)
	}

	/* Called for initialization during Ready (before initialization) */
	override fun readyInit(engine:GameEngine):Boolean {
		val playerID = engine.playerID
		when {
			big[playerID] -> {
				feverThreshold[playerID] = 0
				ojamaMeter[playerID] = true
			}
			feverThreshold[playerID]==0 -> ojamaMeter[playerID] = true
			else -> {
				feverTime[playerID] = feverTimeMin[playerID]*60
				feverChain[playerID] = feverChainStart[playerID]
			}
		}
		super.readyInit(engine)
		return false
	}

	/* When the current piece is in action */
	override fun renderMove(engine:GameEngine) {
		drawX(engine)
	}

	/* Render score */
	override fun renderLast(engine:GameEngine) {
		val fldPosX = receiver.fieldX(engine)
		val fldPosY = receiver.fieldY(engine)
		val pid = engine.playerID
		val playerColor = EventReceiver.getPlayerColor(pid)

		// Timer
		if(pid==0) receiver.drawDirectFont(224, 8, engine.statistics.time.toTimeStr)

		// Ojama Counter
		var fontColor = COLOR.WHITE
		if(ojama[pid]>=1) fontColor = COLOR.YELLOW
		if(ojama[pid]>=6) fontColor = COLOR.ORANGE
		if(ojama[pid]>=12) fontColor = COLOR.RED

		var strOjama = "${ojama[pid]}"
		if(ojamaAdd[pid]>0&&!(inFever[pid]&&ojamaAddToFever[pid]))
			strOjama = "$strOjama(+${ojamaAdd[pid]})"

		if(strOjama!="0")
			receiver.drawDirectFont(fldPosX+4, fldPosY+if(inFever[pid]) 16 else 32, strOjama, fontColor)

		// Fever Ojama Counter
		fontColor = COLOR.WHITE
		if(ojamaFever[pid]>=1) fontColor = COLOR.YELLOW
		if(ojamaFever[pid]>=6) fontColor = COLOR.ORANGE
		if(ojamaFever[pid]>=12) fontColor = COLOR.RED

		var ojamaFeverStr = "${ojamaFever[pid]}"
		if(ojamaAdd[pid]>0&&inFever[pid]&&ojamaAddToFever[pid])
			ojamaFeverStr = "$ojamaFeverStr(+${ojamaAdd[pid]})"

		if(ojamaFeverStr!="0")
			receiver.drawDirectFont(fldPosX+4, fldPosY+if(inFever[pid]) 32 else 16, ojamaFeverStr, fontColor)

		// Score
		var strScoreMultiplier = ""
		if(lastscores[pid]!=0&&lastmultiplier[pid]!=0&&scgettime[pid]>0)
			strScoreMultiplier = "(${lastscores[pid]}e${lastmultiplier[pid]})"

		if(engine.displaySize==1) {
			receiver.drawDirectFont(fldPosX+4, fldPosY+440, "%12d".format(score[pid]), playerColor)
			receiver.drawDirectFont(fldPosX+4, fldPosY+456, "%12s".format(strScoreMultiplier), playerColor)
		} else if(engine.gameStarted) {
			receiver.drawDirectFont(fldPosX-28, fldPosY+248, "%8d".format(score[pid]), playerColor)
			receiver.drawDirectFont(fldPosX-28, fldPosY+264, "%8s".format(strScoreMultiplier), playerColor)
		}

		// Fever
		if(feverThreshold[pid]>0) {
			// Timer
			if(engine.displaySize==1) {
				receiver.drawDirectFont(fldPosX+224, fldPosY+200, "REST", playerColor, .5f)
				receiver.drawDirectFont(fldPosX+216, fldPosY+216, "%2d".format(feverTime[pid]/60))
				receiver.drawDirectFont(fldPosX+248, fldPosY+224, ".%d".format(feverTime[pid]%60/6), scale = .5f)

				if(feverTimeLimitAddDisplay[pid]>0)
					receiver.drawDirectFont(
						fldPosX+216, fldPosY+240, "+%d SEC.".format(feverTimeLimitAdd[pid]/60),
						COLOR.YELLOW, .5f
					)
			} else if(engine.gameStarted) {
				receiver.drawDirectFont(fldPosX+128, fldPosY+184, "REST", playerColor, .5f)
				receiver.drawDirectFont(fldPosX+120, fldPosY+200, "%2d".format(feverTime[pid]/60))
				receiver.drawDirectFont(fldPosX+152, fldPosY+208, ".%d".format(feverTime[pid]%60/6), scale = .5f)

				if(feverTimeLimitAddDisplay[pid]>0)
					receiver.drawDirectFont(
						fldPosX+120, fldPosY+216, "+%d SEC.".format(feverTimeLimitAdd[pid]/60),
						COLOR.YELLOW, .5f
					)
			}

			// Points
			if(feverShowMeter[pid]&&engine.displaySize==1) {
				if(inFever[pid]) {
					var color = (engine.statistics.time shr 2)%FEVER_METER_COLORS.size
					for(i in 0..<feverThreshold[pid]) {
						if(color==0) color = FEVER_METER_COLORS.size
						color--
						receiver.drawDirectFont(fldPosX+232, fldPosY+424-i*16, "\u0084", FEVER_METER_COLORS[color])
					}
				} else {
					for(i in feverPoints[pid]..<feverThreshold[pid])
						receiver.drawDirectFont(fldPosX+232, fldPosY+424-i*16, "\u0083")
					for(i in 0..<feverPoints[pid]) {
						val color = feverThreshold[pid]-1-i
						receiver.drawDirectFont(fldPosX+232, fldPosY+424-i*16, "\u0084", FEVER_METER_COLORS[color])
					}
				}
			} else if(engine.displaySize==1) {
				receiver.drawDirectFont(fldPosX+220, fldPosY+240, "FEVER", playerColor, .5f)
				receiver.drawDirectFont(fldPosX+228, fldPosY+256, "${feverPoints[pid]}/${feverThreshold[pid]}", scale = .5f)
			} else if(engine.gameStarted) {
				receiver.drawDirectFont(fldPosX+124, fldPosY+232, "FEVER", playerColor, .5f)
				receiver.drawDirectFont(fldPosX+132, fldPosY+240, "${feverPoints[pid]}/${feverThreshold[pid]}", scale = .5f)
			}
		}

		if(engine.stat!=GameEngine.Status.MOVE&&engine.stat!=GameEngine.Status.RESULT&&engine.gameStarted)
			drawX(engine)

		if(ojamaHard[pid]>0) drawHardOjama(engine)

		super.renderLast(engine)
	}

	override fun drawX(engine:GameEngine) {
		val playerID = engine.playerID
		if(inFever[playerID]) {
			val strFeverTimer = "%02d".format((feverTime[playerID]+59)/60)

			for(i in 0..1)
				if(engine.field.getBlockEmpty(2+i, 0))
					if(engine.displaySize==1)
						receiver.drawMenuFont(
							engine, 4+i*2, 0, "${strFeverTimer[i]}", if(feverTime[playerID]<360) COLOR.RED else COLOR.WHITE,
							2f
						)
					else
						receiver.drawMenuFont(
							engine, 2+i, 0, "${strFeverTimer[i]}", if(feverTime[playerID]<360) COLOR.RED else COLOR.WHITE
						)
		} else if(dangerColumnShowX[playerID]) drawX(engine)
	}

	override fun calcChainNewPower(engine:GameEngine, chain:Int):Int {
		val powers = if(inFever[engine.playerID]) FEVER_POWERS else CHAIN_POWERS
		return if(chain>powers.size)
			powers[powers.size-1]
		else
			powers[chain-1]
	}

	override fun onClear(engine:GameEngine) {
		val enemyID = if(engine.playerID==0) 1 else 0

		if(engine.chain==1) ojamaAddToFever[enemyID] = inFever[enemyID]
	}

	override fun addOjama(engine:GameEngine, pts:Int):Int {
		val pid = engine.playerID
		val enemyID = if(pid==0) 1 else 0

		var pow = 0
		if(zenKeshi[pid]&&zenKeshiType[pid]==ZENKESHI_MODE_ON) pow += zenKeshiOjama[pid]
		//Add ojama
		var rate = ojamaRate[pid]
		if(hurryUpSeconds[pid]>0&&engine.statistics.time>hurryUpSeconds[pid])
			rate = rate shr engine.statistics.time/(hurryUpSeconds[pid]*60)
		if(rate<=0) rate = 1
		pow += if(inFever[pid])
			(pts*feverPower[pid]+10*rate-1)/(10*rate)
		else
			(pts+rate-1)/rate
		ojamaSent[pid] += pow

		if(feverThreshold[pid]>0&&feverTimeCriteria[pid]==FEVER_TIME_CRITERIA_ATTACK&&!inFever[pid]) {
			feverTime[pid] = minOf(feverTime[pid]+60, feverTimeMax[pid]*60)
			feverTimeLimitAdd[pid] = 60
			feverTimeLimitAddDisplay[pid] = 60
		}
		var ojamaSend = pow
		var countered = false
		if(ojamaCounterMode[pid]!=OJAMA_COUNTER_OFF) {
			//Counter ojama
			if(inFever[pid]) {
				if(ojamaFever[pid]>0&&ojamaSend>0) {
					val delta = minOf(ojamaFever[pid], ojamaSend)
					ojamaFever[pid] -= delta
					ojamaSend -= delta
					countered = true
				}
				if(ojamaAdd[pid]>0&&ojamaSend>0) {
					val delta = minOf(ojamaAdd[pid], ojamaSend)
					ojamaAdd[pid] -= delta
					ojamaSend -= delta
					countered = true
				}
			}
			if(ojama[pid]>0&&ojamaSend>0) {
				val delta = minOf(ojama[pid], ojamaSend)
				ojama[pid] -= delta
				ojamaSend -= delta
				countered = true
			}
			if(ojamaAdd[pid]>0&&ojamaSend>0) {
				val delta = minOf(ojamaAdd[pid], ojamaSend)
				ojamaAdd[pid] -= delta
				ojamaSend -= delta
				countered = true
			}
		}
		ojamaAdd[enemyID] += maxOf(0, ojamaSend)
		if(countered&&feverPointCriteria[pid]!=FEVER_POINT_CRITERIA_CLEAR||engine.field.garbageCleared>0&&feverPointCriteria[pid]!=FEVER_POINT_CRITERIA_COUNTER) {
			if(feverThreshold[pid]>0&&feverThreshold[pid]>feverPoints[pid]) feverPoints[pid]++
			if(feverThreshold[enemyID]>0&&feverTimeCriteria[enemyID]==FEVER_TIME_CRITERIA_COUNTER&&!inFever[enemyID]) {
				feverTime[enemyID] = minOf(feverTime[enemyID]+60, feverTimeMax[enemyID]*60)
				feverTimeLimitAdd[enemyID] = 60
				feverTimeLimitAddDisplay[enemyID] = 60
			}
		}
		return pow
	}

	override fun lineClearEnd(engine:GameEngine):Boolean {
		val pid = engine.playerID
		val enemyID = if(pid==0) 1 else 0
		if(ojamaAdd[enemyID]>0) {
			if(ojamaAddToFever[enemyID]&&inFever[enemyID])
				ojamaFever[enemyID] += ojamaAdd[enemyID]
			else
				ojama[enemyID] += ojamaAdd[enemyID]
			ojamaAdd[enemyID] = 0
		}
		val feverChainNow = feverChain[pid]
		if(zenKeshi[pid]&&zenKeshiType[pid]==ZENKESHI_MODE_FEVER) {
			if(feverTime[pid]>0) {
				feverTime[pid] = minOf(feverTime[pid]+300, feverTimeMax[pid]*60)
				feverTimeLimitAdd[pid] = 300
				feverTimeLimitAddDisplay[pid] = 60
			}

			if(inFever[pid]||feverPoints[pid]>=feverThreshold[pid]) {
				feverChain[pid] += 2
				if(feverChain[pid]>feverChainMax[pid]) feverChain[pid] = feverChainMax[pid]
			} else
				loadFeverMap(engine, zenKeshiChain[pid])
		}
		if(zenKeshi[pid]&&zenKeshiType[pid]!=ZENKESHI_MODE_ON) {
			zenKeshi[pid] = false
			zenKeshiDisplay[pid] = 120
		}
		//Reset Fever board if necessary
		if(inFever[pid]&&cleared[pid]) {
			feverChain[pid] += maxOf(engine.chain+1-feverChainNow, -2)
			if(feverChain[pid]<feverChainMin[pid]) feverChain[pid] = feverChainMin[pid]
			if(feverChain[pid]>feverChainMax[pid]) feverChain[pid] = feverChainMax[pid]
			if(feverChain[pid]>feverChainNow)
				engine.playSE("cool")
			else if(feverChain[pid]<feverChainNow) engine.playSE("regret")
			if(feverTime[pid]>0) {
				if(engine.chain>2) {
					feverTime[pid] += (engine.chain-2)*30
					feverTimeLimitAdd[pid] = (engine.chain-2)*30
					feverTimeLimitAddDisplay[pid] = 60
				}
				loadFeverMap(engine, feverChain[pid])
			}
		}
		//Check to end Fever Mode
		if(inFever[pid]&&feverTime[pid]==0) {
			engine.playSE("levelup")
			inFever[pid] = false
			feverTime[pid] = feverTimeMin[pid]*60
			feverPoints[pid] = 0

			engine.field.replace(feverBackupField[pid])
			if(ojamaMeter[pid])
				engine.meterValue = ojama[pid]*1f/engine.field.width
			ojama[pid] += ojamaFever[pid]
			ojamaFever[pid] = 0
			ojamaAddToFever[pid] = false
		}
		//Drop garbage if needed.
		val ojamaNow = if(inFever[pid]) ojamaFever[pid] else ojama[pid]
		if(ojamaNow>0&&!ojamaDrop[pid]&&(!cleared[pid]||!inFever[pid]&&ojamaCounterMode[pid]!=OJAMA_COUNTER_FEVER)) {
			ojamaDrop[pid] = true
			val drop = minOf(ojamaNow, maxAttack[pid])
			if(inFever[pid]) ojamaFever[pid] -= drop
			else ojama[pid] -= drop
			engine.field.garbageDrop(engine, drop, false, ojamaHard[pid])
			engine.field.setAllSkin(engine.blkSkin)
			return true
		}
		//Check for game over
		gameOverCheck(engine)
		//Check to start Fever Mode
		if(!inFever[pid]&&feverPoints[pid]>=feverThreshold[pid]&&feverThreshold[pid]>0) {
			engine.playSE("levelup")
			inFever[pid] = true
			feverBackupField[pid].replace(engine.field)
			engine.field.reset()
			loadFeverMap(engine, feverChain[pid])
			if(!ojamaMeter[pid]) engine.meterValue = 0f
		}
		return false
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)

		// Debug cheat :p
		val pid = engine.playerID
		if(engine.ctrl.isPush(Controller.BUTTON_F)&&xyzzy==573)
			if(feverPoints[pid]<feverThreshold[pid]) feverPoints[pid]++

		if(feverTimeLimitAddDisplay[pid]>0) feverTimeLimitAddDisplay[pid]--

		if(inFever[pid]&&feverTime[pid]>0&&engine.timerActive) {
			feverTime[pid]--
			if(feverTime[pid] in 1..360&&feverTime[pid]%60==0)
				engine.playSE("countdown")
			else if(feverTime[pid]==0) engine.playSE("levelstop")
		}
		if(ojamaMeter[pid]||feverThreshold[pid]==0)
			updateOjamaMeter(engine)
		else if(!inFever[pid]) {
			engine.meterValue = 1f*feverPoints[pid]/feverThreshold[pid]
			when {
				feverPoints[pid]==feverThreshold[pid]-1 -> engine.meterColor = GameEngine.METER_COLOR_ORANGE
				feverPoints[pid]<feverThreshold[pid]-1 -> engine.meterColor = GameEngine.METER_COLOR_YELLOW
				feverPoints[pid]==feverThreshold[pid] -> engine.meterColor = GameEngine.METER_COLOR_RED
			}
		} else {
			engine.meterValue = feverTime[pid]*1f/(feverTimeMax[pid]*60)
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			when {
				feverTime[pid]<=feverTimeMin[pid]*15 -> engine.meterColor = GameEngine.METER_COLOR_RED
				feverTime[pid]<=feverTimeMin[pid]*30 -> engine.meterColor = GameEngine.METER_COLOR_ORANGE
				feverTime[pid]<=feverTimeMin[pid]*60 -> engine.meterColor = GameEngine.METER_COLOR_YELLOW
			}
		}
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveOtherSetting(engine, owner.replayProp)
		val pid = engine.playerID
		savePreset(engine, owner.replayProp, -1-pid, "")
		if(xyzzy==573) owner.replayProp.setProperty("avalanchevs.debugcheatenable", true)

		if(useMap[pid]) fldBackup[pid]?.let {saveMap(it, owner.replayProp, pid)}

		owner.replayProp.setProperty("avalanchevs.version", version)
		return false
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0

		/** Chain multipliers in Fever */
		private val FEVER_POWERS =
			listOf(
				4, 10, 18, 21, 29, 46, 76, 113, 150, 223, 259, 266, 313, 364, 398, 432, 468, 504, 540, 576, 612, 648, 684,
				720 //Arle
			)

		/** Names of fever point criteria settings */
		private val FEVER_POINT_CRITERIA_NAMES = listOf("COUNTER", "CLEAR", "BOTH")

		/** Constants for fever point criteria settings */
		private const val FEVER_POINT_CRITERIA_COUNTER = 0
		private const val FEVER_POINT_CRITERIA_CLEAR = 1/* ,FEVER_POINT_CRITERIA_BOTH = 2 */

		/** Names of fever time criteria settings */
		private val FEVER_TIME_CRITERIA_NAMES = listOf("COUNTER", "ATTACK")

		/** Constants for fever time criteria settings */
		private const val FEVER_TIME_CRITERIA_COUNTER = 0
		private const val FEVER_TIME_CRITERIA_ATTACK = 1

		/** Fever meter colors */
		private val FEVER_METER_COLORS =
			listOf(
				COLOR.RED, COLOR.ORANGE, COLOR.YELLOW, COLOR.GREEN, COLOR.CYAN, COLOR.BLUE, COLOR.COBALT, COLOR.PURPLE,
				COLOR.PINK
			)
	}
}
