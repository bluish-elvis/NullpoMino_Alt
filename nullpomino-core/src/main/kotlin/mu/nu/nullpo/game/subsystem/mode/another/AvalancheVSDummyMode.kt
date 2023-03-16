/*
 * Copyright (c) 2010-2023, NullNoname
 * Kotlin converted and modified by Venom=Nhelv.
 * THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
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

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.play.GameStyle
import mu.nu.nullpo.game.subsystem.mode.AbstractMode
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import kotlin.random.Random

/** AVALANCHE VS DUMMY Mode */
abstract class AvalancheVSDummyMode:AbstractMode() {
	/** Rule settings for countering ojama not yet dropped */
	protected var ojamaCounterMode = IntArray(MAX_PLAYERS)

	/** Has accumulatedojama blockOfcount */
	protected var ojama = IntArray(MAX_PLAYERS)

	/** Had sentojama blockOfcount */
	protected var ojamaSent = IntArray(MAX_PLAYERS)

	/** Time to display the most recent increase in score */
	protected var scgettime = IntArray(MAX_PLAYERS)

	/** UseBGM */
	protected var bgmId = 0

	/** Big */
	protected var big = BooleanArray(MAX_PLAYERS)

	/** Sound effectsON/OFF */
	protected var enableSE = BooleanArray(MAX_PLAYERS)

	/** MapUse flag */
	protected var useMap = BooleanArray(MAX_PLAYERS)

	/** UseMapSet number */
	protected var mapSet = IntArray(MAX_PLAYERS)

	/** Map number(-1Random in) */
	protected var mapNumber = IntArray(MAX_PLAYERS)

	/** Last preset number used */
	protected var presetNumber = IntArray(MAX_PLAYERS)

	/** Winner */
	protected var winnerID = 0

	/** MapSets ofProperty file */
	protected var propMap:Array<CustomProperties?> = emptyArray()

	/** MaximumMap number */
	protected var mapMaxNo = IntArray(MAX_PLAYERS)

	/** For backupfield (MapUsed to save the replay) */
	protected var fldBackup:Array<Field?> = emptyArray()

	/** MapRan for selectioncount */
	protected var randMap:Random = Random.Default

	/** Flag for all clear */
	protected var zenKeshi = BooleanArray(MAX_PLAYERS)

	/** Amount of points earned from most recent clear */
	protected var lastscores = IntArray(MAX_PLAYERS)
	protected var lastmultiplier = IntArray(MAX_PLAYERS)

	/** Amount of ojama added in current chain */
	protected var ojamaAdd = IntArray(MAX_PLAYERS)

	/** Score */
	protected var score = IntArray(MAX_PLAYERS)

	/** Max amount of ojama dropped at once */
	protected var maxAttack = IntArray(MAX_PLAYERS)

	/** Number of colors to use */
	protected var numColors = IntArray(MAX_PLAYERS)

	/** Minimum chain count needed to send ojama */
	protected var rensaShibari = IntArray(MAX_PLAYERS)

	/** Denominator for score-to-ojama conversion */
	protected var ojamaRate = IntArray(MAX_PLAYERS)

	/** Settings for hard ojama blocks */
	protected var ojamaHard = IntArray(MAX_PLAYERS)

	/** HurryupSeconds before the startcount(0InHurryupNo) */
	protected var hurryUpSeconds = IntArray(MAX_PLAYERS)

	/** Set to true when last drop resulted in a clear */
	protected var cleared = BooleanArray(MAX_PLAYERS)

	/** Set to true when dropping ojama blocks */
	protected var ojamaDrop = BooleanArray(MAX_PLAYERS)

	/** Time to display "ZENKESHI!" */
	protected var zenKeshiDisplay = IntArray(MAX_PLAYERS)

	/** Zenkeshi reward type */
	protected var zenKeshiType = IntArray(MAX_PLAYERS)

	/** Selected fever values set file */
	protected var feverMapSet = IntArray(MAX_PLAYERS)

	/** Selected fever values set file's subset list */
	protected val feverMapSubsets = MutableList(MAX_PLAYERS) {List(0) {""}}

	/** Fever values CustomProperties */
	protected val propFeverMap = MutableList<CustomProperties?>(MAX_PLAYERS) {null}

	/** Chain level boundaries for Fever Mode */
	protected var feverChainMin = IntArray(MAX_PLAYERS)
	protected var feverChainMax = IntArray(MAX_PLAYERS)

	/** Selected outline type */
	protected var outlineType = IntArray(MAX_PLAYERS)

	/** If true, both columns 3 and 4 are danger columns */
	protected var dangerColumnDouble = BooleanArray(MAX_PLAYERS)

	/** If true, red X's appear at tops of danger columns */
	protected var dangerColumnShowX = BooleanArray(MAX_PLAYERS)

	/** Time to display last chain */
	protected var chainDisplay = IntArray(MAX_PLAYERS)

	/** Type of chain display */
	protected var chainDisplayType = IntArray(MAX_PLAYERS)

	/** True to use new (Fever) chain powers */
	protected var newChainPower = BooleanArray(MAX_PLAYERS)

	/** True to use slower falling animations, false to use faster */
	protected var cascadeSlow = BooleanArray(MAX_PLAYERS)

	/** True to use big field display */
	protected var bigDisplay = false

	/* Mode name */
	override val name = "AVALANCHE VS DUMMY"

	override val isVSMode:Boolean
		get() = true

	/* Number of players */
	override val players:Int
		get() = MAX_PLAYERS

	/* Game style */
	override val gameStyle = GameStyle.AVALANCHE
	override val gameIntensity = 2
	/* Mode initialization */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)

		ojamaCounterMode = IntArray(MAX_PLAYERS)
		ojama = IntArray(MAX_PLAYERS)
		ojamaSent = IntArray(MAX_PLAYERS)

		scgettime = IntArray(MAX_PLAYERS)
		bgmId = 0
		big = BooleanArray(MAX_PLAYERS)
		enableSE = BooleanArray(MAX_PLAYERS)
		hurryUpSeconds = IntArray(MAX_PLAYERS)
		useMap = BooleanArray(MAX_PLAYERS)
		mapSet = IntArray(MAX_PLAYERS)
		mapNumber = IntArray(MAX_PLAYERS)
		presetNumber = IntArray(MAX_PLAYERS)
		propMap = arrayOfNulls(MAX_PLAYERS)
		mapMaxNo = IntArray(MAX_PLAYERS)
		fldBackup = arrayOfNulls(MAX_PLAYERS)
		randMap = Random.Default

		zenKeshi = BooleanArray(MAX_PLAYERS)
		lastscores = IntArray(MAX_PLAYERS)
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
		propFeverMap.fill(null)
		feverMapSubsets.fill(emptyList())
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

	/** Load settings from [prop] not related to speeds
	 * Note: Subclasses need to load ojamaRate and ojamaHard, since default
	 * values vary.
	 * @param engine GameEngine
	 */
	protected fun loadOtherSetting(engine:GameEngine, prop:CustomProperties, name:String) {
		val playerID = engine.playerID
		bgmId = prop.getProperty("avalanchevs$name.bgmno", 0)
		ojamaCounterMode[playerID] = prop.getProperty("avalanchevs$name.ojamaCounterMode", OJAMA_COUNTER_ON)
		big[playerID] = prop.getProperty("avalanchevs$name.big.p$playerID", false)
		enableSE[playerID] = prop.getProperty("avalanchevs$name.enableSE.p$playerID", true)
		hurryUpSeconds[playerID] = prop.getProperty("avalanchevs$name.hurryupSeconds.p$playerID", 192)
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
			loadMapSetFever(engine, feverMapSet[playerID], true)
	}

	/** Save settings from [engine] into [prop] not related to speeds */
	protected fun saveOtherSetting(engine:GameEngine, prop:CustomProperties, name:String) {
		val playerID = engine.playerID
		prop.setProperty("avalanchevs$name.bgmno", bgmId)
		prop.setProperty("avalanchevs$name.ojamaCounterMode", ojamaCounterMode[playerID])
		prop.setProperty("avalanchevs$name.big.p$playerID", big[playerID])
		prop.setProperty("avalanchevs$name.enableSE.p$playerID", enableSE[playerID])
		prop.setProperty("avalanchevs$name.hurryupSeconds.p$playerID", hurryUpSeconds[playerID])
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

	/** MapRead into #[id]:[field] from [prop] */
	protected fun loadMap(field:Field?, prop:CustomProperties?, id:Int) {
		field?.run {
			reset()
			//field.readProperty(prop, id);
			stringToField(prop?.getProperty("values.$id", "") ?: "")
			setAllAttribute(true, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
			setAllAttribute(false, Block.ATTRIBUTE.SELF_PLACED)
		}
	}

	/** MapSave from #[id]:[field] into [prop] */
	protected fun saveMap(field:Field, prop:CustomProperties, id:Int) {
		//field.writeProperty(prop, id);
		prop.setProperty("values.$id", field.fieldToString())
	}

	/** For previewMapRead
	 * @param engine GameEngine
	 * @param id MapID
	 * @param forceReload trueWhen youMapForce Reload the file
	 */
	protected fun loadMapPreview(engine:GameEngine, id:Int, forceReload:Boolean) {
		val playerID = engine.playerID
		if(propMap[playerID].isNullOrEmpty()||forceReload) {
			mapMaxNo[playerID] = 0
			propMap[playerID] = receiver.loadProperties("config/map/avalanche/${mapSet[playerID]}.map")
		}

		if(propMap[playerID].isNullOrEmpty())
			engine.field.reset()
		else propMap[playerID]?.let {
			mapMaxNo[playerID] = it.getProperty("values.maxMapNumber", 0)
			engine.createFieldIfNeeded()
			loadMap(engine.field, it, id)
			engine.field.setAllSkin(engine.skin)
		}
	}

	protected fun loadMapSetFever(engine:GameEngine, map:Int, forceReload:Boolean) {
		val playerID = engine.playerID
		if(propFeverMap[playerID].isNullOrEmpty()||forceReload) {
			propFeverMap[playerID] = receiver.loadProperties("config/map/avalanche/${FEVER_MAPS[map]}.map")
			feverChainMin[playerID] = propFeverMap[playerID]?.getProperty("minChain", 3) ?: 3
			feverChainMax[playerID] = propFeverMap[playerID]?.getProperty("maxChain", 15) ?: 15
			val subsets = propFeverMap[playerID]?.getProperty("sets") ?: ""
			feverMapSubsets[playerID] = subsets.split(Regex(",")).dropLastWhile {it.isEmpty()}
		}
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		val playerID = engine.playerID
		if(playerID==1) {
			engine.randSeed = owner.engine[0].randSeed
			engine.random = Random(owner.engine[0].randSeed)
		}

		engine.frameColor = PLAYER_COLOR_FRAME[playerID]
		engine.clearMode = GameEngine.ClearType.COLOR
		engine.garbageColorClear = true
		engine.lineGravityType = GameEngine.LineGravity.CASCADE
		engine.nextPieceEnable = PIECE_ENABLE.map {it==1}
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
	override fun onReady(engine:GameEngine):Boolean =
		if(engine.statc[0]==0) readyInit(engine) else false

	open fun readyInit(engine:GameEngine):Boolean {
		val playerID = engine.playerID
		engine.numColors = numColors[playerID]
		engine.lineGravityType =
			if(cascadeSlow[playerID]) GameEngine.LineGravity.CASCADE_SLOW else GameEngine.LineGravity.CASCADE
		engine.displaySize = if(bigDisplay) 1 else 0
		engine.sticky = 2

		if(outlineType[playerID]==0) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
		if(outlineType[playerID]==1) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_SAMECOLOR
		if(outlineType[playerID]==2) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE

		if(big[playerID]) {
			engine.fieldHeight = 6
			engine.fieldWidth = 3
			engine.field.reset()
			engine.colorClearSize = 3
			engine.displaySize = 1
			engine.createFieldIfNeeded()
			zenKeshiType[playerID] = ZENKESHI_MODE_OFF
			ojamaHard[playerID] = 0
		} else if(feverMapSet[playerID]>=0&&feverMapSet[playerID]<FEVER_MAPS.size)
			loadMapSetFever(engine, feverMapSet[playerID], true)
		// MapFor storing backup Replay read
		if(useMap[playerID]) {
			if(owner.replayMode) {
				engine.createFieldIfNeeded()
				loadMap(engine.field, owner.replayProp, playerID)
				engine.field.setAllSkin(engine.skin)
			} else {
				if(propMap[playerID].isNullOrEmpty())
					propMap[playerID] = receiver.loadProperties("config/map/avalanche/${mapSet[playerID]}.map")

				propMap[playerID]?.let {
					engine.createFieldIfNeeded()

					if(mapNumber[playerID]<0) {
						if(playerID==1&&useMap[0]&&mapNumber[0]<0)
							engine.field.replace(owner.engine[0].field)
						else {
							val no = if(mapMaxNo[playerID]<1) 0 else randMap.nextInt(mapMaxNo[playerID])
							loadMap(engine.field, it, no)
						}
					} else
						loadMap(engine.field, it, mapNumber[playerID])

					engine.field.setAllSkin(engine.skin)
					fldBackup[playerID] = Field(engine.field)
				}
			}
		} else engine.field.reset()
		return false
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		engine.b2bEnable = false
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		engine.enableSE = enableSE[engine.playerID]
		if(engine.playerID==1) owner.musMan.bgm = BGM.values[bgmId]
		engine.ignoreHidden = true

		engine.twistAllowKick = false
		engine.twistEnable = false
		engine.useAllSpinBonus = false
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
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		val pid = engine.playerID
		val avalanche = ev.lines
		if(avalanche>0) {
			cleared[pid] = true

			chainDisplay[pid] = 60
			engine.playSE("combo${minOf(engine.chain, 20)}")
			onClear(engine)

			val pts = calcPts(engine, pid, avalanche)

			var multiplier = engine.field.colorClearExtraCount
			if(big[pid]) multiplier = multiplier shr 2
			if(engine.field.colorsCleared>1) multiplier += (engine.field.colorsCleared-1)*2

			multiplier += calcChainMultiplier(engine, pid, engine.chain)

			if(multiplier>999) multiplier = 999
			if(multiplier<1) multiplier = 1

			lastscores[pid] = pts
			lastmultiplier[pid] = multiplier
			scgettime[pid] = 25
			val ptsTotal = pts*multiplier
			score[pid] += ptsTotal

			val pow = if(engine.chain>=rensaShibari[pid]) addOjama(engine, ptsTotal) else 0

			if(engine.field.isEmpty) {
				zenKeshi[pid] = true
				engine.statistics.scoreBonus += 2100
				score[pid] += 2100
			} else
				zenKeshi[pid] = false
			return pow
		} else if(!engine.field.canCascade()) cleared[pid] = false
		return 0
	}

	protected fun calcPts(engine:GameEngine, playerID:Int, avalanche:Int):Int = avalanche*10

	protected fun calcChainMultiplier(engine:GameEngine, playerID:Int, chain:Int):Int = if(newChainPower[playerID])
		calcChainNewPower(engine, chain)
	else
		calcChainClassicPower(engine, playerID, chain)

	protected open fun calcChainNewPower(engine:GameEngine, chain:Int):Int {
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

	protected open fun onClear(engine:GameEngine) {}

	protected open fun addOjama(engine:GameEngine, pts:Int):Int {
		val pid = engine.playerID
		val enemyID = if(pid==0) 1 else 0

		var pow = 0
		if(zenKeshi[pid]&&zenKeshiType[pid]==ZENKESHI_MODE_ON) pow += 30
		//Add ojama
		var rate = maxOf(1, ojamaRate[pid])
		if(hurryUpSeconds[pid]>0&&engine.statistics.time>hurryUpSeconds[pid])
			rate = rate shr engine.statistics.time/(hurryUpSeconds[pid]*60)
		pow += ptsToOjama(engine, pts, rate)
		ojamaSent[pid] += pow
		var send = pow
		if(ojamaCounterMode[pid]!=OJAMA_COUNTER_OFF) {
			//Counter ojama
			if(ojama[pid]>0&&send>0) {
				val delta = minOf(ojama[pid], send)
				ojama[pid] -= delta
				send -= delta
			}
			if(ojamaAdd[pid]>0&&send>0) {
				val delta = minOf(ojamaAdd[pid], send)
				ojamaAdd[pid] -= delta
				send -= delta
			}
		}
		if(pow>0) ojamaAdd[enemyID] += send
		return pow
	}

	protected open fun ptsToOjama(engine:GameEngine, pts:Int, rate:Int):Int = (pts+rate-1)/rate

	abstract override fun lineClearEnd(engine:GameEngine):Boolean

	/** Check for game over */
	protected fun gameOverCheck(engine:GameEngine) {
		if(engine.field.isEmpty) return
		if(big[engine.playerID]) {
			if(!engine.field.getBlockEmpty(1, 0)) engine.stat = GameEngine.Status.GAMEOVER
		} else if(!engine.field.getBlockEmpty(2, 0)||dangerColumnDouble[engine.playerID]&&!engine.field.getBlockEmpty(3, 0))
			engine.stat = GameEngine.Status.GAMEOVER
	}

	protected fun loadFeverMap(engine:GameEngine, chain:Int) {
		loadFeverMap(engine, engine.random, chain, engine.random.nextInt(feverMapSubsets[engine.playerID].size))
	}

	protected fun loadFeverMap(engine:GameEngine, rand:Random?, chain:Int, subset:Int) {
		val pid = engine.playerID
		engine.createFieldIfNeeded()
		engine.field.run {
			reset()
			stringToField(
				propFeverMap[pid]?.getProperty(
					"${feverMapSubsets[pid][subset]}.${numColors[pid]}colors.${chain}chain"
				) ?: ""
			)
			setBlockLinkByColor()
			setAllAttribute(false, Block.ATTRIBUTE.GARBAGE, Block.ATTRIBUTE.ANTIGRAVITY)
			setAllSkin(engine.skin)
			shuffleColors(BLOCK_COLORS, numColors[pid], Random(rand!!.nextLong()))
		}
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		val pid = engine.playerID
		if(scgettime[pid]>0) scgettime[pid]--
		if(zenKeshiDisplay[pid]>0) zenKeshiDisplay[pid]--
		if(chainDisplay[pid]>0) chainDisplay[pid]--

		// Settlement
		if(pid==1&&owner.engine[0].gameActive) {
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
				owner.musMan.bgm = BGM.Silent
			}
		}
	}

	override fun pieceLocked(engine:GameEngine, clear:Int) {
		cleared[engine.playerID] = false
		ojamaDrop[engine.playerID] = false
	}

	protected open fun updateOjamaMeter(engine:GameEngine) {
		var width = 6
		width = engine.field.width
		val blockHeight = engine.blockSize
		// Rising auctionMeter
		val pid = engine.playerID
		val value = ojama[pid]*blockHeight/width
		when {
			ojama[pid]>=5*width -> engine.meterColor = GameEngine.METER_COLOR_RED
			ojama[pid]>=width -> engine.meterColor = GameEngine.METER_COLOR_ORANGE
			ojama[pid]>=1 -> engine.meterColor = GameEngine.METER_COLOR_YELLOW
			else -> engine.meterColor = GameEngine.METER_COLOR_GREEN
		}
		if(value>engine.meterValue)
			engine.meterValue++
		else if(value<engine.meterValue) engine.meterValue--
	}

	override fun renderLast(engine:GameEngine) {
		val pid = engine.playerID
		if(!owner.engine[pid].gameActive) return
		val textHeight = if(engine.displaySize==1) 11 else engine.field.height+1
		val baseX = if(engine.displaySize==1) 1 else -2
		if(engine.chain>0&&chainDisplay[pid]>0&&chainDisplayType[pid]!=CHAIN_DISPLAY_NONE)
			receiver.drawMenuFont(
				engine, baseX+if(engine.chain>9) 0 else 1, textHeight, "${engine.chain} CHAIN!", getChainColor(engine)
			)
		if(zenKeshi[pid]||zenKeshiDisplay[pid]>0)
			receiver.drawMenuFont(engine, baseX+1, textHeight+1, "ZENKESHI!", COLOR.YELLOW)
	}

	protected open fun getChainColor(engine:GameEngine):COLOR = engine.playerID.let {pid ->
		when {
			chainDisplayType[pid]==CHAIN_DISPLAY_PLAYER -> EventReceiver.getPlayerColor(pid)
			chainDisplayType[pid]==CHAIN_DISPLAY_SIZE -> if(engine.chain>=rensaShibari[pid]) COLOR.GREEN else COLOR.RED
			else -> COLOR.YELLOW
		}
	}

	protected open fun drawX(engine:GameEngine) {
		val playerID = engine.playerID
		if(!dangerColumnShowX[playerID]) return

		val baseX = if(big[playerID]) 1 else 2

		for(i in 0 until if(dangerColumnDouble[playerID]&&!big[playerID]) 2 else 1)
			if(engine.field.getBlockEmpty(baseX+i, 0))
				when {
					big[playerID] -> receiver.drawMenuFont(engine, 2, 0, "\u0085", COLOR.RED, 2f)
					engine.displaySize==1 -> receiver.drawMenuFont(engine, 4+i*2, 0, "\u0085", COLOR.RED, 2f)
					else -> receiver.drawMenuFont(engine, 2+i, 0, "\u0085", COLOR.RED)
				}
	}

	protected fun drawHardOjama(engine:GameEngine) {
		for(x in 0 until engine.field.width)
			for(y in 0 until engine.field.height) {
				val hard = engine.field.getBlock(x, y)!!.hard
				if(hard>0)
					if(engine.displaySize==1) receiver.drawMenuFont(engine, x*2, y*2, "$hard", COLOR.YELLOW, 2f)
					else receiver.drawMenuFont(engine, x, y, "$hard", COLOR.YELLOW)
			}
	}

	protected fun drawScores(engine:GameEngine, x:Int, y:Int, headerColor:COLOR) {
		var y = y
		receiver.drawScoreFont(engine, x, y, "Score", headerColor)
		y++
		receiver.drawScoreFont(engine, x, y, "1P: ", COLOR.RED)
		if(scgettime[0]>0&&lastscores[0]>0&&lastmultiplier[0]>0)
			receiver.drawScoreFont(engine, x+4, y, "+${lastscores[0]}e${lastmultiplier[0]}", COLOR.RED)
		else receiver.drawScoreFont(engine, x+4, y, "${score[0]}", COLOR.RED)
		y++
		receiver.drawScoreFont(engine, x, y, "2P: ", COLOR.BLUE)
		if(scgettime[1]>0&&lastscores[1]>0&&lastmultiplier[1]>0)
			receiver.drawScoreFont(engine, x+4, y, "+${lastscores[1]}e${lastmultiplier[1]}", COLOR.BLUE)
		else receiver.drawScoreFont(engine, x+4, y, "${score[1]}", COLOR.BLUE)
	}

	protected fun drawOjama(engine:GameEngine, x:Int, y:Int, headerColor:COLOR) {
		receiver.drawScoreFont(engine, x, y, "OJAMA", headerColor)
		val ojamaStr1P = "${ojama[0]}${if(ojamaAdd[0]>0) "(+${ojamaAdd[0]})" else ""}"
		val ojamaStr2P = "${ojama[1]}${if(ojamaAdd[1]>0) "(+${ojamaAdd[1]})" else ""}"
		receiver.drawScoreFont(engine, x, y+1, "1P:", COLOR.RED)
		receiver.drawScoreFont(engine, x+4, y+1, ojamaStr1P, ojama[0]>0)
		receiver.drawScoreFont(engine, x, y+2, "2P:", COLOR.BLUE)
		receiver.drawScoreFont(engine, x+4, y+2, ojamaStr2P, ojama[1]>0)
	}

	protected fun drawAttack(engine:GameEngine, x:Int, y:Int, headerColor:COLOR) {
		receiver.drawScoreFont(engine, x, y, "ATTACK", headerColor)
		receiver.drawScoreFont(engine, x, y+1, "1P: ${ojamaSent[0]}", COLOR.RED)
		receiver.drawScoreFont(engine, x, y+2, "2P: ${ojamaSent[1]}", COLOR.BLUE)
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 1, "RESULT", COLOR.ORANGE)
		when(winnerID) {
			-1 -> receiver.drawMenuFont(engine, 6, 2, "DRAW", COLOR.GREEN)
			engine.playerID -> receiver.drawMenuFont(engine, 6, 2, "WIN!", COLOR.YELLOW)
			else -> receiver.drawMenuFont(engine, 6, 2, "LOSE", COLOR.WHITE)
		}

		val apm = ojamaSent[engine.playerID]*3600f/engine.statistics.time
		drawResult(
			engine, receiver, 3, COLOR.ORANGE,
			"ATTACK", "%10d".format(ojamaSent[engine.playerID]),
			"CLEARED", "%10d".format(engine.statistics.lines),
			"MAX CHAIN", "%10d".format(engine.statistics.maxChain),
			"PIECE", "%10d".format(engine.statistics.totalPieceLocked),
			"ATTACK/MIN", "%10g".format(apm),
			"PIECE/SEC", "%10g".format(engine.statistics.pps),
			"Time", "%10s".format(owner.engine[0].statistics.time.toTimeStr)
		)
	}

	companion object {
		/** Enabled piece types */
		val PIECE_ENABLE = listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0)

		/** Block colors */
		val BLOCK_COLORS =
			listOf(
				Block.COLOR.RED, Block.COLOR.GREEN, Block.COLOR.BLUE, Block.COLOR.YELLOW,
				Block.COLOR.PURPLE
			)

		/** Fever values files list */
		val FEVER_MAPS = listOf("Fever", "15th", "15thDS", "7", "Compendium")

		/** Chain multipliers */
		val CHAIN_POWERS = listOf(
			4, 12, 24, 33, 50, 101, 169, 254, 341, 428, 538, 648, 763, 876, 990, 999 //Arle
		)

		/** Number of players */
		const val MAX_PLAYERS = 2

		/** Ojama counter setting constants */
		const val OJAMA_COUNTER_OFF = 0
		const val OJAMA_COUNTER_ON = 1
		const val OJAMA_COUNTER_FEVER = 2

		/** Names of ojama counter settings */
		val OJAMA_COUNTER_STRING = listOf("OFF", "ON", "FEVER")

		/** Zenkeshi setting constants */
		const val ZENKESHI_MODE_OFF = 0
		const val ZENKESHI_MODE_ON = 1
		const val ZENKESHI_MODE_FEVER = 2

		/** Names of zenkeshi settings */
		val ZENKESHI_TYPE_NAMES = listOf("OFF", "ON", "FEVER")

		/** Names of outline settings */
		val OUTLINE_TYPE_NAMES = listOf("NORMAL", "COLOR", "NONE")

		/** Names of chain display settings */
		val CHAIN_DISPLAY_NAMES = listOf("OFF", "YELLOW", "PLAYER", "SIZE")

		/** Constants for chain display settings */
		const val CHAIN_DISPLAY_NONE = 0
		const val CHAIN_DISPLAY_YELLOW = 1
		const val CHAIN_DISPLAY_PLAYER = 2
		const val CHAIN_DISPLAY_SIZE = 3

		/** Each player's frame cint */
		val PLAYER_COLOR_FRAME = listOf(GameEngine.FRAME_COLOR_RED, GameEngine.FRAME_COLOR_BLUE)
	}
}
