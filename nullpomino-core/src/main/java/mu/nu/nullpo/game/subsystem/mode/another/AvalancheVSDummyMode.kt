/* Copyright (c) 2010, NullNoname
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of NullNoname nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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
 * POSSIBILITY OF SUCH DAMAGE. */
package mu.nu.nullpo.game.subsystem.mode.another

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.subsystem.mode.AbstractMode
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import java.util.*

/** AVALANCHE VS DUMMY Mode */
abstract class AvalancheVSDummyMode:AbstractMode() {

	/** Rule settings for countering ojama not yet dropped */
	protected var ojamaCounterMode:IntArray = IntArray(MAX_PLAYERS)

	/** Has accumulatedojama blockOfcount */
	protected var ojama:IntArray = IntArray(MAX_PLAYERS)

	/** Had sentojama blockOfcount */
	protected var ojamaSent:IntArray = IntArray(MAX_PLAYERS)

	/** Time to display the most recent increase in score */
	protected var scgettime:IntArray = IntArray(MAX_PLAYERS)

	/** UseBGM */
	protected var bgmno:Int = 0

	/** Big */
	protected var big:BooleanArray = BooleanArray(MAX_PLAYERS)

	/** Sound effectsON/OFF */
	protected var enableSE:BooleanArray = BooleanArray(MAX_PLAYERS)

	/** MapUse flag */
	protected var useMap:BooleanArray = BooleanArray(MAX_PLAYERS)

	/** UseMapSet number */
	protected var mapSet:IntArray = IntArray(MAX_PLAYERS)

	/** Map number(-1Random in) */
	protected var mapNumber:IntArray = IntArray(MAX_PLAYERS)

	/** Last preset number used */
	protected var presetNumber:IntArray = IntArray(MAX_PLAYERS)

	/** Winner */
	protected var winnerID:Int = 0

	/** MapSets ofProperty file */
	protected var propMap:Array<CustomProperties?> = emptyArray()

	/** MaximumMap number */
	protected var mapMaxNo:IntArray = IntArray(MAX_PLAYERS)

	/** For backupfield (MapUsed to save the replay) */
	protected var fldBackup:Array<Field?> = emptyArray()

	/** MapRan for selectioncount */
	protected var randMap:Random = Random()

	/** Flag for all clear */
	protected var zenKeshi:BooleanArray = BooleanArray(MAX_PLAYERS)

	/** Amount of points earned from most recent clear */
	protected var lastscore:IntArray = IntArray(MAX_PLAYERS)
	protected var lastmultiplier:IntArray = IntArray(MAX_PLAYERS)

	/** Amount of ojama added in current chain */
	protected var ojamaAdd:IntArray = IntArray(MAX_PLAYERS)

	/** Score */
	protected var score:IntArray = IntArray(MAX_PLAYERS)

	/** Max amount of ojama dropped at once */
	protected var maxAttack:IntArray = IntArray(MAX_PLAYERS)

	/** Number of colors to use */
	protected var numColors:IntArray = IntArray(MAX_PLAYERS)

	/** Minimum chain count needed to send ojama */
	protected var rensaShibari:IntArray = IntArray(MAX_PLAYERS)

	/** Denominator for score-to-ojama conversion */
	protected var ojamaRate:IntArray = IntArray(MAX_PLAYERS)

	/** Settings for hard ojama blocks */
	protected var ojamaHard:IntArray = IntArray(MAX_PLAYERS)

	/** HurryupSeconds before the startcount(0InHurryupNo) */
	protected var hurryupSeconds:IntArray = IntArray(MAX_PLAYERS)

	/** Set to true when last drop resulted in a clear */
	protected var cleared:BooleanArray = BooleanArray(MAX_PLAYERS)

	/** Set to true when dropping ojama blocks */
	protected var ojamaDrop:BooleanArray = BooleanArray(MAX_PLAYERS)

	/** Time to display "ZENKESHI!" */
	protected var zenKeshiDisplay:IntArray = IntArray(MAX_PLAYERS)

	/** Zenkeshi reward type */
	protected var zenKeshiType:IntArray = IntArray(MAX_PLAYERS)

	/** Selected fever values set file */
	protected var feverMapSet:IntArray = IntArray(MAX_PLAYERS)

	/** Selected fever values set file's subset list */
	protected var feverMapSubsets:Array<Array<String>> = emptyArray()

	/** Fever values CustomProperties */
	protected var propFeverMap:Array<CustomProperties?> = emptyArray()

	/** Chain level boundaries for Fever Mode */
	protected var feverChainMin:IntArray = IntArray(MAX_PLAYERS)
	protected var feverChainMax:IntArray = IntArray(MAX_PLAYERS)

	/** Selected outline type */
	protected var outlineType:IntArray = IntArray(MAX_PLAYERS)

	/** If true, both columns 3 and 4 are danger columns */
	protected var dangerColumnDouble:BooleanArray = BooleanArray(MAX_PLAYERS)

	/** If true, red X's appear at tops of danger columns */
	protected var dangerColumnShowX:BooleanArray = BooleanArray(MAX_PLAYERS)

	/** Time to display last chain */
	protected var chainDisplay:IntArray = IntArray(MAX_PLAYERS)

	/** Type of chain display */
	protected var chainDisplayType:IntArray = IntArray(MAX_PLAYERS)

	/** True to use new (Fever) chain powers */
	protected var newChainPower:BooleanArray = BooleanArray(MAX_PLAYERS)

	/** True to use slower falling animations, false to use faster */
	protected var cascadeSlow:BooleanArray = BooleanArray(MAX_PLAYERS)

	/** True to use big field display */
	protected var bigDisplay:Boolean = false

	/* Mode name */
	override val name:String
		get() = "AVALANCHE VS DUMMY"

	override val isVSMode:Boolean
		get() = true

	/* Number of players */
	override val players:Int
		get() = MAX_PLAYERS

	/* Game style */
	override val gameStyle:Int
		get() = GameEngine.GAMESTYLE_AVALANCHE

	/* Mode initialization */
	@Suppress("RemoveExplicitTypeArguments")
	override fun modeInit(manager:GameManager) {
		owner = manager
		receiver = owner.receiver

		ojamaCounterMode = IntArray(MAX_PLAYERS)
		ojama = IntArray(MAX_PLAYERS)
		ojamaSent = IntArray(MAX_PLAYERS)

		scgettime = IntArray(MAX_PLAYERS)
		bgmno = 0
		big = BooleanArray(MAX_PLAYERS)
		enableSE = BooleanArray(MAX_PLAYERS)
		hurryupSeconds = IntArray(MAX_PLAYERS)
		useMap = BooleanArray(MAX_PLAYERS)
		mapSet = IntArray(MAX_PLAYERS)
		mapNumber = IntArray(MAX_PLAYERS)
		presetNumber = IntArray(MAX_PLAYERS)
		propMap = arrayOfNulls(MAX_PLAYERS)
		mapMaxNo = IntArray(MAX_PLAYERS)
		fldBackup = arrayOfNulls(MAX_PLAYERS)
		randMap = Random()

		zenKeshi = BooleanArray(MAX_PLAYERS)
		lastscore = IntArray(MAX_PLAYERS)
		lastmultiplier = IntArray(MAX_PLAYERS)
		ojamaAdd = IntArray(MAX_PLAYERS)
		score = IntArray(MAX_PLAYERS)
		numColors = IntArray(MAX_PLAYERS)
		maxAttack = IntArray(MAX_PLAYERS)
		rensaShibari = IntArray(MAX_PLAYERS)
		ojamaRate = IntArray(MAX_PLAYERS)
		ojamaHard = IntArray(MAX_PLAYERS)

		cleared = BooleanArray(MAX_PLAYERS)
		ojamaDrop = BooleanArray(MAX_PLAYERS)
		zenKeshiDisplay = IntArray(MAX_PLAYERS)
		zenKeshiType = IntArray(MAX_PLAYERS)
		outlineType = IntArray(MAX_PLAYERS)
		dangerColumnDouble = BooleanArray(MAX_PLAYERS)
		dangerColumnShowX = BooleanArray(MAX_PLAYERS)
		chainDisplay = IntArray(MAX_PLAYERS)
		chainDisplayType = IntArray(MAX_PLAYERS)
		newChainPower = BooleanArray(MAX_PLAYERS)
		cascadeSlow = BooleanArray(MAX_PLAYERS)

		feverMapSet = IntArray(MAX_PLAYERS)
		propFeverMap = Array(MAX_PLAYERS) {CustomProperties()}
		feverMapSubsets = Array(MAX_PLAYERS) {emptyArray<String>()}
		feverChainMin = IntArray(MAX_PLAYERS)
		feverChainMax = IntArray(MAX_PLAYERS)

		winnerID = -1
	}

	/** Read speed presets
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 * @param preset Preset number
	 */
	protected fun loadPreset(engine:GameEngine, prop:CustomProperties, preset:Int, name:String) {
		engine.speed.gravity = prop.getProperty("avalanchevs$name.gravity.$preset", 4)
		engine.speed.denominator = prop.getProperty("avalanchevs$name.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("avalanchevs$name.are.$preset", 30)
		engine.speed.areLine = prop.getProperty("avalanchevs$name.areLine.$preset", 30)
		engine.speed.lineDelay = prop.getProperty("avalanchevs$name.lineDelay.$preset", 10)
		engine.speed.lockDelay = prop.getProperty("avalanchevs$name.lockDelay.$preset", 60)
		engine.speed.das = prop.getProperty("avalanchevs$name.das.$preset", 14)
		engine.cascadeDelay = prop.getProperty("avalanchevs$name.fallDelay.$preset", 1)
		engine.cascadeClearDelay = prop.getProperty("avalanchevs$name.clearDelay.$preset", 10)
	}

	/** Save speed presets
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 * @param preset Preset number
	 */
	protected fun savePreset(engine:GameEngine, prop:CustomProperties, preset:Int, name:String) {
		prop.setProperty("avalanchevs$name.gravity.$preset", engine.speed.gravity)
		prop.setProperty("avalanchevs$name.denominator.$preset", engine.speed.denominator)
		prop.setProperty("avalanchevs$name.are.$preset", engine.speed.are)
		prop.setProperty("avalanchevs$name.areLine.$preset", engine.speed.areLine)
		prop.setProperty("avalanchevs$name.lineDelay.$preset", engine.speed.lineDelay)
		prop.setProperty("avalanchevs$name.lockDelay.$preset", engine.speed.lockDelay)
		prop.setProperty("avalanchevs$name.das.$preset", engine.speed.das)
		prop.setProperty("avalanchevs$name.fallDelay.$preset", engine.cascadeDelay)
		prop.setProperty("avalanchevs$name.clearDelay.$preset", engine.cascadeClearDelay)
	}

	/** Load settings not related to speeds
	 * Note: Subclasses need to load ojamaRate and ojamaHard, since default
	 * values vary.
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 */
	protected fun loadOtherSetting(engine:GameEngine, prop:CustomProperties, name:String) {
		val playerID = engine.playerID
		bgmno = prop.getProperty("avalanchevs$name.bgmno", 0)
		ojamaCounterMode[playerID] = prop.getProperty("avalanchevs$name.ojamaCounterMode", OJAMA_COUNTER_ON)
		big[playerID] = prop.getProperty("avalanchevs$name.big.p$playerID", false)
		enableSE[playerID] = prop.getProperty("avalanchevs$name.enableSE.p$playerID", true)
		hurryupSeconds[playerID] = prop.getProperty("avalanchevs$name.hurryupSeconds.p$playerID", 192)
		useMap[playerID] = prop.getProperty("avalanchevs$name.useMap.p$playerID", false)
		mapSet[playerID] = prop.getProperty("avalanchevs$name.mapSet.p$playerID", 0)
		mapNumber[playerID] = prop.getProperty("avalanchevs$name.mapNumber.p$playerID", -1)
		feverMapSet[playerID] = prop.getProperty("avalanchevs$name.feverMapSet.p$playerID", 0)
		presetNumber[playerID] = prop.getProperty("avalanchevs$name.presetNumber.p$playerID", 0)
		maxAttack[playerID] = prop.getProperty("avalanchevs$name.maxAttack.p$playerID", 30)
		numColors[playerID] = prop.getProperty("avalanchevs$name.numColors.p$playerID", 5)
		rensaShibari[playerID] = prop.getProperty("avalanchevs$name.rensaShibari.p$playerID", 1)
		zenKeshiType[playerID] = prop.getProperty("avalanchevs$name.zenKeshiType.p$playerID", 1)
		outlineType[playerID] = prop.getProperty("avalanchevs$name.outlineType.p$playerID", 1)
		dangerColumnDouble[playerID] = prop.getProperty("avalanchevs$name.dangerColumnDouble.p$playerID", false)
		dangerColumnShowX[playerID] = prop.getProperty("avalanchevs$name.dangerColumnShowX.p$playerID", false)
		chainDisplayType[playerID] = prop.getProperty("avalanchevs$name.chainDisplayType.p$playerID", 1)
		newChainPower[playerID] = prop.getProperty("avalanchevs$name.newChainPower.p$playerID", false)
		cascadeSlow[playerID] = prop.getProperty("avalanchevs$name.cascadeSlow.p$playerID", false)
		bigDisplay = prop.getProperty("avalanchevs$name.bigDisplay", false)
		engine.colorClearSize = prop.getProperty("avalanchevs$name.clearSize.p$playerID", 4)
		if(feverMapSet[playerID]>=0&&feverMapSet[playerID]<FEVER_MAPS.size)
			loadMapSetFever(engine, playerID, feverMapSet[playerID], true)
	}

	/** Save settings not related to speeds
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 */
	protected fun saveOtherSetting(engine:GameEngine, prop:CustomProperties, name:String) {
		val playerID = engine.playerID
		prop.setProperty("avalanchevs$name.bgmno", bgmno)
		prop.setProperty("avalanchevs$name.ojamaCounterMode", ojamaCounterMode[playerID])
		prop.setProperty("avalanchevs$name.big.p$playerID", big[playerID])
		prop.setProperty("avalanchevs$name.enableSE.p$playerID", enableSE[playerID])
		prop.setProperty("avalanchevs$name.hurryupSeconds.p$playerID", hurryupSeconds[playerID])
		prop.setProperty("avalanchevs$name.useMap.p$playerID", useMap[playerID])
		prop.setProperty("avalanchevs$name.mapSet.p$playerID", mapSet[playerID])
		prop.setProperty("avalanchevs$name.mapNumber.p$playerID", mapNumber[playerID])
		prop.setProperty("avalanchevs$name.feverMapSet.p$playerID", feverMapSet[playerID])
		prop.setProperty("avalanchevs$name.presetNumber.p$playerID", presetNumber[playerID])
		prop.setProperty("avalanchevs$name.maxAttack.p$playerID", maxAttack[playerID])
		prop.setProperty("avalanchevs$name.numColors.p$playerID", numColors[playerID])
		prop.setProperty("avalanchevs$name.rensaShibari.p$playerID", rensaShibari[playerID])
		prop.setProperty("avalanchevs$name.ojamaRate.p$playerID", ojamaRate[playerID])
		prop.setProperty("avalanchevs$name.ojamaHard.p$playerID", ojamaHard[playerID])
		prop.setProperty("avalanchevs$name.zenKeshiType.p$playerID", zenKeshiType[playerID])
		prop.setProperty("avalanchevs$name.outlineType.p$playerID", outlineType[playerID])
		prop.setProperty("avalanchevs$name.dangerColumnDouble.p$playerID", dangerColumnDouble[playerID])
		prop.setProperty("avalanchevs$name.dangerColumnShowX.p$playerID", dangerColumnShowX[playerID])
		prop.setProperty("avalanchevs$name.chainDisplayType.p$playerID", chainDisplayType[playerID])
		prop.setProperty("avalanchevs$name.newChainPower.p$playerID", newChainPower[playerID])
		prop.setProperty("avalanchevs$name.cascadeSlow.p$playerID", cascadeSlow[playerID])
		prop.setProperty("avalanchevs$name.bigDisplay", bigDisplay)
		prop.setProperty("avalanchevs$name.clearSize.p$playerID", engine.colorClearSize)
	}

	/** MapRead
	 * @param field field
	 * @param prop Property file to read from
	 */
	protected fun loadMap(field:Field?, prop:CustomProperties?, id:Int) {
		field?.run {
			reset()
			//field.readProperty(prop, id);
			stringToField(prop?.getProperty("values.$id", "")?:"")
			setAllAttribute(true, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
			setAllAttribute(false, Block.ATTRIBUTE.SELFPLACED)
		}
	}

	/** MapSave
	 * @param field field
	 * @param prop Property file to save to
	 * @param id AnyID
	 */
	protected fun saveMap(field:Field, prop:CustomProperties, id:Int) {
		//field.writeProperty(prop, id);
		prop.setProperty("values.$id", field.fieldToString())
	}

	/** For previewMapRead
	 * @param engine GameEngine
	 * @param playerID Player number
	 * @param id MapID
	 * @param forceReload trueWhen youMapForce Reload the file
	 */
	protected fun loadMapPreview(engine:GameEngine, playerID:Int, id:Int, forceReload:Boolean) {
		if(propMap[playerID].isNullOrEmpty()||forceReload) {
			mapMaxNo[playerID] = 0
			propMap[playerID] = receiver.loadProperties("config/values/avalanche/${mapSet[playerID]}.values")
		}

		if(propMap[playerID].isNullOrEmpty()&&engine.field!=null)
			engine.field!!.reset()
		else propMap[playerID]?.let {
			mapMaxNo[playerID] = it.getProperty("values.maxMapNumber", 0)
			engine.createFieldIfNeeded()
			loadMap(engine.field, it, id)
			engine.field!!.setAllSkin(engine.skin)
		}
	}

	protected fun loadMapSetFever(engine:GameEngine, playerID:Int, id:Int, forceReload:Boolean) {
		if(propFeverMap[playerID].isNullOrEmpty()||forceReload) {
			propFeverMap[playerID] = receiver.loadProperties("config/values/avalanche/${FEVER_MAPS[id]}.values")
			feverChainMin[playerID] = propFeverMap[playerID]?.getProperty("minChain", 3)?:3
			feverChainMax[playerID] = propFeverMap[playerID]?.getProperty("maxChain", 15)?:15
			val subsets = propFeverMap[playerID]?.getProperty("sets")?:""
			feverMapSubsets[playerID] = subsets.split(",".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()
		}
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		if(playerID==1) {
			engine.randSeed = owner.engine[0].randSeed
			engine.random = Random(owner.engine[0].randSeed)
		}

		engine.framecolor = PLAYER_COLOR_FRAME[playerID]
		engine.clearMode = GameEngine.ClearType.COLOR
		engine.garbageColorClear = true
		engine.lineGravityType = GameEngine.LineGravity.CASCADE
		for(i in 0 until Piece.PIECE_COUNT)
			engine.nextPieceEnable[i] = PIECE_ENABLE[i]==1
		engine.blockColors = BLOCK_COLORS
		engine.randomBlockColor = true
		engine.connectBlocks = false
		engine.dominoQuickTurn = true

		ojama[playerID] = 0
		ojamaAdd[playerID] = 0
		ojamaSent[playerID] = 0
		score[playerID] = 0
		zenKeshi[playerID] = false
		scgettime[playerID] = 0
		cleared[playerID] = false
		ojamaDrop[playerID] = false
		zenKeshiDisplay[playerID] = 0
		chainDisplay[playerID] = 0
	}

	/* Called for initialization during Ready (before initialization) */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean = if(engine.statc[0]==0) readyInit(engine, playerID) else false

	open fun readyInit(engine:GameEngine, playerID:Int):Boolean {
		engine.numColors = numColors[playerID]
		engine.lineGravityType =
			if(cascadeSlow[playerID]) GameEngine.LineGravity.CASCADE_SLOW else GameEngine.LineGravity.CASCADE
		engine.displaysize = if(bigDisplay) 1 else 0
		engine.sticky = 2

		if(outlineType[playerID]==0) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
		if(outlineType[playerID]==1) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_SAMECOLOR
		if(outlineType[playerID]==2) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE

		if(big[playerID]) {
			engine.fieldHeight = 6
			engine.fieldWidth = 3
			engine.field = null
			engine.colorClearSize = 3
			engine.displaysize = 1
			engine.createFieldIfNeeded()
			zenKeshiType[playerID] = ZENKESHI_MODE_OFF
			ojamaHard[playerID] = 0
		} else if(feverMapSet[playerID]>=0&&feverMapSet[playerID]<FEVER_MAPS.size)
			loadMapSetFever(engine, playerID, feverMapSet[playerID], true)
		// MapFor storing backup Replay read
		if(useMap[playerID]) {
			if(owner.replayMode) {
				engine.createFieldIfNeeded()
				loadMap(engine.field, owner.replayProp, playerID)
				engine.field!!.setAllSkin(engine.skin)
			} else {
				if(propMap[playerID].isNullOrEmpty())
					propMap[playerID] = receiver.loadProperties("config/values/avalanche/${mapSet[playerID]}.values")

				propMap[playerID]?.let {
					engine.createFieldIfNeeded()

					if(mapNumber[playerID]<0) {
						if(playerID==1&&useMap[0]&&mapNumber[0]<0)
							engine.field!!.copy(owner.engine[0].field)
						else {
							val no = if(mapMaxNo[playerID]<1) 0 else randMap.nextInt(mapMaxNo[playerID])
							loadMap(engine.field, it, no)
						}
					} else
						loadMap(engine.field, it, mapNumber[playerID])

					engine.field!!.setAllSkin(engine.skin)
					fldBackup[playerID] = Field(engine.field)
				}
			}
		} else engine.field?.reset()
		return false
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.b2bEnable = false
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		engine.enableSE = enableSE[playerID]
		if(playerID==1) owner.bgmStatus.bgm = BGM.values[bgmno]
		engine.ignoreHidden = true

		engine.tspinAllowKick = false
		engine.tspinEnable = false
		engine.useAllSpinBonus = false
	}

	/* Called when hard drop used */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreHD += fall
	}

	/* Called when soft drop used */
	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreSD += fall
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, avalanche:Int) {
		if(avalanche>0) {
			cleared[playerID] = true

			chainDisplay[playerID] = 60
			engine.playSE("combo${minOf(engine.chain, 20)}")
			onClear(engine, playerID)

			val pts = calcPts(engine, playerID, avalanche)

			var multiplier = engine.field!!.colorClearExtraCount
			if(big[playerID]) multiplier = multiplier shr 2
			if(engine.field!!.colorsCleared>1) multiplier += (engine.field!!.colorsCleared-1)*2

			multiplier += calcChainMultiplier(engine, playerID, engine.chain)

			if(multiplier>999) multiplier = 999
			if(multiplier<1) multiplier = 1

			lastscore[playerID] = pts
			lastmultiplier[playerID] = multiplier
			scgettime[playerID] = 25
			val ptsTotal = pts*multiplier
			score[playerID] += ptsTotal

			if(engine.chain>=rensaShibari[playerID]) addOjama(engine, playerID, ptsTotal)

			if(engine.field!!.isEmpty) {
				zenKeshi[playerID] = true
				engine.statistics.scoreBonus += 2100
				score[playerID] += 2100
			} else
				zenKeshi[playerID] = false
		} else if(!engine.field!!.canCascade()) cleared[playerID] = false
	}

	protected fun calcPts(engine:GameEngine, playerID:Int, avalanche:Int):Int = avalanche*10

	protected fun calcChainMultiplier(engine:GameEngine, playerID:Int, chain:Int):Int = if(newChainPower[playerID])
		calcChainNewPower(engine, playerID, chain)
	else
		calcChainClassicPower(engine, playerID, chain)

	protected open fun calcChainNewPower(engine:GameEngine, playerID:Int, chain:Int):Int {
		return if(chain>CHAIN_POWERS.size)
			CHAIN_POWERS[CHAIN_POWERS.size-1]
		else
			CHAIN_POWERS[chain-1]
	}

	protected fun calcChainClassicPower(engine:GameEngine, playerID:Int, chain:Int):Int = when {
		chain==2 -> 8
		chain==3 -> 16
		chain>=4 -> 32*(chain-3)
		else -> 0
	}

	protected open fun onClear(engine:GameEngine, playerID:Int) {}

	protected open fun addOjama(engine:GameEngine, playerID:Int, pts:Int) {
		var enemyID = 0
		if(playerID==0) enemyID = 1

		var ojamaNew = 0
		if(zenKeshi[playerID]&&zenKeshiType[playerID]==ZENKESHI_MODE_ON) ojamaNew += 30
		//Add ojama
		var rate = ojamaRate[playerID]
		if(hurryupSeconds[playerID]>0&&engine.statistics.time>hurryupSeconds[playerID])
			rate = rate shr engine.statistics.time/(hurryupSeconds[playerID]*60)
		if(rate<=0) rate = 1
		ojamaNew += ptsToOjama(engine, playerID, pts, rate)
		ojamaSent[playerID] += ojamaNew

		if(ojamaCounterMode[playerID]!=OJAMA_COUNTER_OFF) {
			//Counter ojama
			if(ojama[playerID]>0&&ojamaNew>0) {
				val delta = minOf(ojama[playerID], ojamaNew)
				ojama[playerID] -= delta
				ojamaNew -= delta
			}
			if(ojamaAdd[playerID]>0&&ojamaNew>0) {
				val delta = minOf(ojamaAdd[playerID], ojamaNew)
				ojamaAdd[playerID] -= delta
				ojamaNew -= delta
			}
		}
		if(ojamaNew>0) ojamaAdd[enemyID] += ojamaNew
	}

	protected open fun ptsToOjama(engine:GameEngine, playerID:Int, pts:Int, rate:Int):Int = (pts+rate-1)/rate

	abstract override fun lineClearEnd(engine:GameEngine, playerID:Int):Boolean

	/** Check for game over */
	protected fun gameOverCheck(engine:GameEngine, playerID:Int) {
		if(engine.field==null) return
		if(big[playerID]) {
			if(!engine.field!!.getBlockEmpty(1, 0)) engine.stat = GameEngine.Status.GAMEOVER
		} else if(!engine.field!!.getBlockEmpty(2, 0)||dangerColumnDouble[playerID]&&!engine.field!!.getBlockEmpty(3, 0))
			engine.stat = GameEngine.Status.GAMEOVER
	}

	protected fun loadFeverMap(engine:GameEngine, playerID:Int, chain:Int) {
		loadFeverMap(engine, playerID, engine.random, chain, engine.random.nextInt(feverMapSubsets[playerID].size))
	}

	protected fun loadFeverMap(engine:GameEngine, playerID:Int, rand:Random?, chain:Int, subset:Int) {
		engine.createFieldIfNeeded()
		engine.field?.run {
			reset()
			stringToField(propFeverMap[playerID]?.getProperty(
				"${feverMapSubsets[playerID][subset]}.${numColors[playerID]}colors.${chain}chain") ?: "")
			setBlockLinkByColor()
			setAllAttribute(false, Block.ATTRIBUTE.GARBAGE, Block.ATTRIBUTE.ANTIGRAVITY)
			setAllSkin(engine.skin)
			shuffleColors(BLOCK_COLORS, numColors[playerID], Random(rand!!.nextLong()))
		}
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		if(scgettime[playerID]>0) scgettime[playerID]--
		if(zenKeshiDisplay[playerID]>0) zenKeshiDisplay[playerID]--
		if(chainDisplay[playerID]>0) chainDisplay[playerID]--

		// Settlement
		if(playerID==1&&owner.engine[0].gameActive) {
			val p1Lose = owner.engine[0].stat==GameEngine.Status.GAMEOVER
			val p2Lose = owner.engine[1].stat==GameEngine.Status.GAMEOVER
			if(p1Lose&&p2Lose) {
				// Draw
				winnerID = -1
				owner.engine[0].stat = GameEngine.Status.GAMEOVER
				owner.engine[1].stat = GameEngine.Status.GAMEOVER
			} else if(p2Lose&&!p1Lose) {
				// 1P win
				winnerID = 0
				owner.engine[0].stat = GameEngine.Status.EXCELLENT
				owner.engine[1].stat = GameEngine.Status.GAMEOVER
			} else if(p1Lose&&!p2Lose) {
				// 2P win
				winnerID = 1
				owner.engine[0].stat = GameEngine.Status.GAMEOVER
				owner.engine[1].stat = GameEngine.Status.EXCELLENT
			}
			if(p1Lose||p2Lose) {
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.engine[0].resetStatc()
				owner.engine[1].resetStatc()
				owner.engine[0].statc[1] = 1
				owner.engine[1].statc[1] = 1
				owner.bgmStatus.bgm = BGM.SILENT
			}
		}
	}

	override fun pieceLocked(engine:GameEngine, playerID:Int, clear:Int) {
		cleared[playerID] = false
		ojamaDrop[playerID] = false
	}

	protected open fun updateOjamaMeter(engine:GameEngine, playerID:Int) {
		var width = 6
		if(engine.field!=null) width = engine.field!!.width
		val blockHeight = receiver.getBlockGraphicsHeight(engine)
		// Rising auctionMeter
		val value = ojama[playerID]*blockHeight/width
		when {
			ojama[playerID]>=5*width -> engine.meterColor = GameEngine.METER_COLOR_RED
			ojama[playerID]>=width -> engine.meterColor = GameEngine.METER_COLOR_ORANGE
			ojama[playerID]>=1 -> engine.meterColor = GameEngine.METER_COLOR_YELLOW
			else -> engine.meterColor = GameEngine.METER_COLOR_GREEN
		}
		if(value>engine.meterValue)
			engine.meterValue++
		else if(value<engine.meterValue) engine.meterValue--
	}

	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(!owner.engine[playerID].gameActive) return

		var textHeight = 13
		if(engine.field!=null) {
			textHeight = engine.field!!.height
			textHeight += 3
		}
		if(engine.displaysize==1) textHeight = 11

		val baseX = if(engine.displaysize==1) 1 else -2

		if(engine.chain>0&&chainDisplay[playerID]>0
			&&chainDisplayType[playerID]!=CHAIN_DISPLAY_NONE)
			receiver.drawMenuFont(engine, playerID, baseX+if(engine.chain>9)
				0
			else
				1, textHeight, "${engine.chain} CHAIN!", getChainColor(engine, playerID))
		if(zenKeshi[playerID]||zenKeshiDisplay[playerID]>0)
			receiver.drawMenuFont(engine, playerID, baseX+1, textHeight+1, "ZENKESHI!", COLOR.YELLOW)
	}

	protected open fun getChainColor(engine:GameEngine, playerID:Int):COLOR {
		return if(chainDisplayType[playerID]==CHAIN_DISPLAY_PLAYER)
			if(playerID==0) COLOR.RED else COLOR.BLUE
		else if(chainDisplayType[playerID]==CHAIN_DISPLAY_SIZE)
			if(engine.chain>=rensaShibari[playerID]) COLOR.GREEN else COLOR.RED
		else COLOR.YELLOW
	}

	protected fun drawX(engine:GameEngine, playerID:Int) {
		if(!dangerColumnShowX[playerID]) return

		val baseX = if(big[playerID]) 1 else 2

		for(i in 0 until if(dangerColumnDouble[playerID]&&!big[playerID]) 2 else 1)
			if(engine.field==null||engine.field!!.getBlockEmpty(baseX+i, 0))
				when {
					big[playerID] -> receiver.drawMenuFont(engine, playerID, 2, 0, "e", COLOR.RED, 2f)
					engine.displaysize==1 -> receiver.drawMenuFont(engine, playerID, 4+i*2, 0, "e", COLOR.RED, 2f)
					else -> receiver.drawMenuFont(engine, playerID, 2+i, 0, "e", COLOR.RED)
				}
	}

	protected fun drawHardOjama(engine:GameEngine, playerID:Int) {
		if(engine.field!=null)
			for(x in 0 until engine.field!!.width)
				for(y in 0 until engine.field!!.height) {
					val hard = engine.field!!.getBlock(x, y)!!.hard
					if(hard>0)
						if(engine.displaysize==1) receiver.drawMenuFont(engine, playerID, x*2, y*2, "$hard", COLOR.YELLOW, 2f)
						else receiver.drawMenuFont(engine, playerID, x, y, "$hard", COLOR.YELLOW)
				}
	}

	protected fun drawScores(engine:GameEngine, playerID:Int, x:Int, y:Int, headerColor:COLOR) {
		var y = y
		receiver.drawScoreFont(engine, playerID, x, y, "SCORE", headerColor)
		y++
		receiver.drawScoreFont(engine, playerID, x, y, "1P: ", COLOR.RED)
		if(scgettime[0]>0&&lastscore[0]>0&&lastmultiplier[0]>0)
			receiver.drawScoreFont(engine, playerID, x+4, y, "+${lastscore[0]}e${lastmultiplier[0]}", COLOR.RED)
		else receiver.drawScoreFont(engine, playerID, x+4, y, "${score[0]}", COLOR.RED)
		y++
		receiver.drawScoreFont(engine, playerID, x, y, "2P: ", COLOR.BLUE)
		if(scgettime[1]>0&&lastscore[1]>0&&lastmultiplier[1]>0)
			receiver.drawScoreFont(engine, playerID, x+4, y, "+${lastscore[1]}e${lastmultiplier[1]}", COLOR.BLUE)
		else receiver.drawScoreFont(engine, playerID, x+4, y, "${score[1]}", COLOR.BLUE)
	}

	protected fun drawOjama(engine:GameEngine, playerID:Int, x:Int, y:Int, headerColor:COLOR) {
		receiver.drawScoreFont(engine, playerID, x, y, "OJAMA", headerColor)
		val ojamaStr1P = "${ojama[0]}${if(ojamaAdd[0]>0) "(+${ojamaAdd[0]})" else ""}"
		val ojamaStr2P = "${ojama[1]}${if(ojamaAdd[1]>0) "(+${ojamaAdd[1]})" else ""}"
		receiver.drawScoreFont(engine, playerID, x, y+1, "1P:", COLOR.RED)
		receiver.drawScoreFont(engine, playerID, x+4, y+1, ojamaStr1P, ojama[0]>0)
		receiver.drawScoreFont(engine, playerID, x, y+2, "2P:", COLOR.BLUE)
		receiver.drawScoreFont(engine, playerID, x+4, y+2, ojamaStr2P, ojama[1]>0)
	}

	protected fun drawAttack(engine:GameEngine, playerID:Int, x:Int, y:Int, headerColor:COLOR) {
		receiver.drawScoreFont(engine, playerID, x, y, "ATTACK", headerColor)
		receiver.drawScoreFont(engine, playerID, x, y+1, "1P: ${ojamaSent[0]}", COLOR.RED)
		receiver.drawScoreFont(engine, playerID, x, y+2, "2P: ${ojamaSent[1]}", COLOR.BLUE)
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 1, "RESULT", COLOR.ORANGE)
		when(winnerID) {
			-1 -> receiver.drawMenuFont(engine, playerID, 6, 2, "DRAW", COLOR.GREEN)
			playerID -> receiver.drawMenuFont(engine, playerID, 6, 2, "WIN!", COLOR.YELLOW)
			else -> receiver.drawMenuFont(engine, playerID, 6, 2, "LOSE", COLOR.WHITE)
		}

		val apm = (ojamaSent[playerID]*3600).toFloat()/engine.statistics.time.toFloat()
		drawResult(engine, playerID, receiver, 3, COLOR.ORANGE,
			"ATTACK", String.format("%10d", ojamaSent[playerID]),
			"CLEARED", String.format("%10d", engine.statistics.lines),
			"MAX CHAIN", String.format("%10d", engine.statistics.maxChain),
			"PIECE", String.format("%10d", engine.statistics.totalPieceLocked),
			"ATTACK/MIN", String.format("%10g", apm),
			"PIECE/SEC", String.format("%10g", engine.statistics.pps),
			"TIME", String.format("%10s", GeneralUtil.getTime(owner.engine[0].statistics.time)))
	}

	companion object {
		/** Enabled piece types */
		val PIECE_ENABLE = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0)

		/** Block colors */
		val BLOCK_COLORS =
			intArrayOf(Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_GREEN, Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_YELLOW, Block.BLOCK_COLOR_PURPLE)

		/** Fever values files list */
		val FEVER_MAPS = arrayOf("Fever", "15th", "15thDS", "7", "Compendium")

		/** Chain multipliers */
		val CHAIN_POWERS = intArrayOf(4, 12, 24, 33, 50, 101, 169, 254, 341, 428, 538, 648, 763, 876, 990, 999 //Arle
		)

		/** Number of players */
		const val MAX_PLAYERS = 2

		/** Ojama counter setting constants */
		const val OJAMA_COUNTER_OFF = 0
		const val OJAMA_COUNTER_ON = 1
		const val OJAMA_COUNTER_FEVER = 2

		/** Names of ojama counter settings */
		val OJAMA_COUNTER_STRING = arrayOf("OFF", "ON", "FEVER")

		/** Zenkeshi setting constants */
		const val ZENKESHI_MODE_OFF = 0
		const val ZENKESHI_MODE_ON = 1
		const val ZENKESHI_MODE_FEVER = 2

		/** Names of zenkeshi settings */
		val ZENKESHI_TYPE_NAMES = arrayOf("OFF", "ON", "FEVER")

		/** Names of outline settings */
		val OUTLINE_TYPE_NAMES = arrayOf("NORMAL", "COLOR", "NONE")

		/** Names of chain display settings */
		val CHAIN_DISPLAY_NAMES = arrayOf("OFF", "YELLOW", "PLAYER", "SIZE")

		/** Constants for chain display settings */
		const val CHAIN_DISPLAY_NONE = 0
		const val CHAIN_DISPLAY_YELLOW = 1
		const val CHAIN_DISPLAY_PLAYER = 2
		const val CHAIN_DISPLAY_SIZE = 3

		/** Each player's frame cint */
		val PLAYER_COLOR_FRAME = intArrayOf(GameEngine.FRAME_COLOR_RED, GameEngine.FRAME_COLOR_BLUE)
	}
}
