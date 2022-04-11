//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package wtf.oshisaure.nullpomodshit.modes

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.AbstractMode
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.game.subsystem.mode.menu.StringsMenuItem
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import kotlin.math.pow

class ScoreAttackRun:AbstractMode() {
	private var lasttype = 0
	private var lastpiece = 0
	private var score:Long = 0
	/** Score Rate Percentage*/
	private var scoreRate:Int = 0
	private var currenttime = 0
	private var ren = 0
	private var allclears = 0
	private var lastb2b = false
	private var incombo = false
	private var quads = 0
	private var bgmlv = 0
	private var rankingScore:Array<LongArray> = Array(RANKING_TYPES) {LongArray(RANKING_MAX)}
	private var rankingLevel:Array<IntArray> = Array(RANKING_TYPES) {IntArray(RANKING_MAX)}
	private var rankingQuads:Array<IntArray> = Array(RANKING_TYPES) {IntArray(RANKING_MAX)}
	private var rankingRank = 0
	private var version = 0
	private var lastscoreL:Long = 0
	private var ingame = false
	private var maxchain = 0
	private var maxmult:Int = 0
	private var scgettime = 0

	private val itemMode = StringsMenuItem(
		"gametype", "GAME MODE", COLOR.BLUE, 0,
		Gametype.values().map {it.label}.toTypedArray()
	)
	private var gametype:Int by DelegateMenuItem(itemMode)
	override val name:String get() = "ARCADE SCORE ATTACK"
	override val menu = MenuList("arcadescoreattack", itemMode)

	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX) for(j in 0 until RANKING_TYPES) {
			rankingScore[j][i] = prop.getProperty("$j.score.$i", 0L)
			rankingLevel[j][i] = prop.getProperty("$j.level.$i", 0)
			rankingQuads[j][i] = prop.getProperty("$j.quads$i", 0)
		}
	}

	override fun saveRanking() {
		return super.saveRanking(
			(0 until RANKING_TYPES).flatMap {t ->
				(0 until RANKING_MAX).flatMap {i ->
					listOf(
						"$t.score.$i" to rankingScore[t][i],
						"$t.level.$i" to rankingLevel[t][i],
						"$t.quads.$i" to rankingQuads[t][i],
					)
				}
			}
		)
	}

	private fun formatScore(sc:Long):String {
		return if(sc>=scoreCutoffValue) {
			formatScoreScientific(sc)
		} else {
			val bigstr = "$sc"
			val len = bigstr.length
			val builder = StringBuilder()
			for(i in 1..len) {
				builder.append(bigstr[len-i])
				if(i%3==0&&i!=len) {
					builder.append(",")
				}
			}
			builder.reverse().toString()
		}
	}

	private fun formatScoreScientific(sc:Long):String {
		var bigstr:String
		bigstr = "$sc"
		while(bigstr.length%3!=0) {
			bigstr = " $bigstr"
		}
		val exponent = bigstr.length-3
		val expstr = "E+"+formatScore(exponent.toLong())
		var render = ""
		for(i in 0..12) {
			render += bigstr[i]
			if(i%3==2) {
				render += if(i==2) "." else " "
			}
		}
		return render.take(sciNotationLength-expstr.length)+expstr
	}

	override fun playerInit(engine:GameEngine, playerID:Int) {

		score = 0
		scoreRate = 1
		maxmult = 1
		currenttime = 7200
		lastscoreL = 0
		quads = 0
		scgettime = 0
		lastb2b = false
		incombo = false
		ingame = false
		ren = 0
		allclears = 0
		maxchain = 0
		bgmlv = 0
		rankingRank = -1
		rankingScore = Array(RANKING_TYPES) {LongArray(RANKING_MAX)}
		rankingLevel = Array(RANKING_TYPES) {IntArray(RANKING_MAX)}
		rankingQuads = Array(RANKING_TYPES) {IntArray(RANKING_MAX)}
		if(!owner.replayMode) {
			version = 1
		}
		engine.owner.backgroundStatus.bg = 0
		engine.framecolor = 7
	}

	fun setSpeed(engine:GameEngine) {
		when(gametype) {
			2, 3, 5 -> {
				engine.speed.gravity = -1
				engine.speed.denominator = 1
			}
			4 -> {
				var lv = engine.statistics.level
				if(lv<0) {
					lv = 0
				}
				if(lv>=tableGravity.size) {
					lv = tableGravity.size-1
				}
				engine.speed.gravity = tableGravity[lv]
				engine.speed.denominator = tableDenominator[lv]
			}
			else -> {
				var lv = engine.statistics.level
				if(lv<0) {
					lv = 0
				}
				if(lv>=tableGravity.size) {
					lv = tableGravity.size-1
				}
				engine.speed.gravity = tableGravity[lv]
				engine.speed.denominator = tableDenominator[lv]
			}
		}
	}

	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		if(!engine.owner.replayMode) {
//			val change = updateMenu(engine)
			if(engine.ctrl.isPush(4)&&engine.statc[3]>=5) {
				engine.playSE("decide")
				return false
			}
			if(engine.ctrl.isPush(5)) engine.quitflag = true
			engine.statc[3]++
		} else {
			engine.statc[3]++
			engine.statc[2] = -1
			if(engine.statc[3]>=60) return false
		}
		return true
	}

	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.twistEnable = gametype==1||gametype==3
		engine.useAllSpinBonus = true
		engine.big = gametype==4||gametype==5
		engine.twistAllowKick = true
		engine.twistEnableEZ = false
		engine.statistics.level = 0
		engine.statistics.levelDispAdd = 1
		score = 0
		currenttime = starttime
		lastscoreL = 0
		scgettime = 0
		quads = 0
		ren = 0
		maxchain = 0
		lastb2b = false
		incombo = false
		scoreRate = 1
		maxmult = 1
		ingame = true
		setSpeed(engine)
	}

	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(!owner.menuOnly) {
			receiver.drawScoreFont(engine, playerID, 0, 0, "ARCADE SCORE ATTACK", 8f)
			if(gametype!=0) receiver.drawScoreFont(engine, playerID, 2, 1, "(${GAMETYPE_LABELS[gametype]} MODE)", 9f)

			var colour:Int
			val event:Int = if(receiver.nextDisplayType==2) 6 else 4
			if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
				if(!owner.replayMode&&engine.ai==null) {
					val scale = if(receiver.nextDisplayType==2) 0.5f else 1.0f
					receiver.drawScoreFont(engine, playerID, 1, event-1, "LV QS SCORE", COLOR.PURPLE, scale)
					colour = 0
					while(colour<10) {
						receiver.drawScoreGrade(engine, playerID, -2, event+colour, String.format("%2d", colour+1), COLOR.YELLOW, scale)
						receiver.drawScoreFont(
							engine, playerID, 1, event+colour, "${rankingLevel[gametype][colour]+1}",
							colour==rankingRank, scale
						)
						receiver.drawScoreFont(
							engine, playerID, 4, event+colour, "${rankingQuads[gametype][colour]}",
							colour==rankingRank, scale
						)
						receiver.drawScoreFont(
							engine, playerID, 7, event+colour,
							formatScore(rankingScore[gametype][colour]), colour==rankingRank, scale
						)
						++colour
					}
				}
			} else {
				receiver.drawScoreFont(engine, playerID, 0, 3, "SCORE", 8f)
				receiver.drawScoreFont(engine, playerID, 0, 4, formatScore(score), 5f)
				if(lastscoreL!=0L)
					receiver.drawScoreFont(engine, playerID, 0, 5, "+"+formatScore(lastscoreL))

				receiver.drawScoreFont(engine, playerID, 0, 7, "LINES", 8f)
				if(engine.statistics.level>=19) {
					receiver.drawScoreFont(engine, playerID, 0, 8, engine.statistics.lines.toString())
				} else {
					receiver.drawScoreFont(
						engine,
						playerID,
						0,
						8,
						engine.statistics.lines.toString()+"/"+tableLevelChange[engine.statistics.level+1]
					)
				}
				receiver.drawScoreFont(engine, playerID, 0, 10, "LEVEL", 8f)
				receiver.drawScoreFont(engine, playerID, 0, 11, (engine.statistics.level+1).toString())
				receiver.drawScoreFont(engine, playerID, 10, 10, "QUADS", 8f)
				receiver.drawScoreFont(engine, playerID, 10, 11, "$quads", 6f)
				if(gametype!=4&&gametype!=5) {
					receiver.drawScoreFont(engine, playerID, 0, 13, "CHAIN", 8f)
					receiver.drawScoreFont(engine, playerID, 0, 14, "$ren", COLOR.RED)
				} else {
					receiver.drawScoreFont(engine, playerID, 0, 13, "ALL CLEARS", 8f)
					receiver.drawScoreFont(engine, playerID, 0, 14, "$allclears", COLOR.RED)
				}
				receiver.drawScoreFont(engine, playerID, 10, 13, "MULTIPLIER", 8f)
				receiver.drawScoreFont(engine, playerID, 10, 14, "${BaseFont.CROSS}$scoreRate%", 5f)
				receiver.drawScoreFont(engine, playerID, 0, 17, "TIME LEFT", 2f)
				receiver.drawScoreFont(engine, playerID, 0, 18, currenttime.toTimeStr)
				receiver.drawScoreFont(engine, playerID, 10, 17, "TOTAL TIME", 9f)
				receiver.drawScoreFont(engine, playerID, 10, 18, engine.statistics.time.toTimeStr)
			}
		}
	}

	override fun onLast(engine:GameEngine, playerID:Int) {
		super.onLast(engine, playerID)
		if(ingame&&engine.statistics.time>0) {
			scgettime += scoreRate
			while(scgettime>=100) {
				score++
				scgettime -= 100
			}
			--currenttime
			if(currenttime<=0) {
				ingame = false
				engine.playSE("levelstop")
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			} else if(currenttime<=600&&currenttime%60==0) {
				engine.playSE("countdown")
			}
		}
	}

	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		ingame = false
		return false
	}

	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		lasttype = minOf(lines, 4)
		lastpiece = engine.nowPieceObject!!.id
		var pts = calcScoreBase(engine, lines)
		var maxlines:Int
		if(lines!=0) {
			incombo = true
			maxlines = 2
			while(maxlines<=lines) {
				pts *= maxlines
				++maxlines
			}
//			engine.playSE("b2b_"+if(ren==0) "start" else "continue")
			++ren
			lastb2b = lines>=4||engine.twist
			quads += if(lines>=4) 1 else 0
		} else {
			pts = 0
			incombo = false
		}
		if(engine.twist) lasttype += 5
		if(lines>=1&&engine.field.isEmpty) {
			engine.playSE("bravo")
			++allclears
			lasttype += 10
		}
		pts = pts*scoreRate/100
		if(!lastb2b&&!incombo) {
			if(ren!=0&&gametype!=4&&gametype!=5) {
				engine.playSE("b2b_end")
			}
			ren = 0
		}
		engine.statistics.scoreLine += pts
		score += pts

		if(tableBGMChange[bgmlv]!=-1) {
			if(engine.statistics.lines>=tableBGMChange[bgmlv]-5) {
				owner.bgmStatus.fadesw = true
			}
			if(engine.statistics.lines>=tableBGMChange[bgmlv]) {
				++bgmlv
				owner.bgmStatus.bgm = BGMStatus.BGM.Generic(bgmlv)
				owner.bgmStatus.fadesw = false
			}
		}
		if(engine.statistics.lines>=tableLevelChange[engine.statistics.level+1]&&engine.statistics.level<19) {
			++engine.statistics.level
			currenttime += timebonus
			owner.backgroundStatus.fadesw = true
			owner.backgroundStatus.fadecount = 0
			owner.backgroundStatus.fadebg = engine.statistics.level
			setSpeed(engine)
			engine.playSE("levelup")
		}
		if(engine.statistics.level<19) {
			maxlines = tableLevelChange[engine.statistics.level+1]
			val minlines = tableLevelChange[engine.statistics.level]
			val linerange = maxlines-minlines
			val curlines = engine.statistics.lines-minlines
			val restlines = linerange-curlines
			engine.meterValue = curlines*receiver.getMeterMax(engine)/(linerange-1)
			engine.meterColor = 3
			if(restlines<=10) engine.meterColor = 2
			if(restlines<=4) engine.meterColor = 1
			if(restlines<=2) engine.meterColor = 0
		} else {
			engine.meterValue = currenttime*receiver.getMeterMax(engine)/'꿈'.code
			engine.meterColor = if(currenttime<=10) 0
			else if(currenttime<=30) 1
			else if(currenttime<=60) 2
			else 3
		}
		scoreRate = (10+engine.statistics.level)*(20+if(gametype!=4&&gametype!=5) ren else allclears)*(10+quads)/20
		if(ren>maxchain) maxchain = ren
		if(scoreRate>maxmult) maxmult = scoreRate
		return pts
	}

	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 0, "FINAL SCORE", COLOR.PURPLE)
		receiver.drawMenuFont(engine, playerID, 0, 1, formatScore(score), COLOR.YELLOW)
		receiver.drawMenuFont(engine, playerID, 0, 2, "QUADS", COLOR.PURPLE)
		receiver.drawMenuFont(engine, playerID, 0, 3, "$quads", COLOR.CYAN)
		receiver.drawMenuFont(engine, playerID, 0, 4, "MAX CHAIN", COLOR.PURPLE)
		receiver.drawMenuFont(engine, playerID, 0, 5, "$maxchain", COLOR.RED)
		receiver.drawMenuFont(engine, playerID, 0, 6, "MAX MULTI.", COLOR.PURPLE)
		receiver.drawMenuFont(engine, playerID, 0, 7, "${BaseFont.CROSS}$maxmult", COLOR.YELLOW)
		drawResultStats(engine, playerID, receiver, 8, COLOR.PURPLE, Statistic.LINES, Statistic.LEVEL, Statistic.LPM, Statistic.PPS)
	}

	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties):Boolean {
		if(!owner.replayMode&&engine.ai==null) {
			updateRanking(score, engine.statistics.level, quads, gametype)
			return (rankingRank!=-1)
		}
		return false
	}

	private fun updateRanking(sc:Long, lv:Int, tet:Int, type:Int) {
		rankingRank = checkRanking(sc, lv, tet, type)
		if(rankingRank!=-1) {
			for(i in 9 downTo rankingRank+1) {
				rankingScore[type][i] = rankingScore[type][i-1]
				rankingLevel[type][i] = rankingLevel[type][i-1]
				rankingQuads[type][i] = rankingQuads[type][i-1]
			}
			rankingScore[type][rankingRank] = sc
			rankingLevel[type][rankingRank] = lv
			rankingQuads[type][rankingRank] = tet
		}
	}

	private fun checkRanking(sc:Long, lv:Int, tet:Int, type:Int):Int {
		for(i in 0..9) if(sc>rankingScore[type][i]) return i
		else if(sc==rankingScore[type][i]&&tet>rankingQuads[type][i]) return i
		else if(sc==rankingScore[type][i]&&tet==rankingQuads[type][i]&&lv>rankingLevel[type][i]) return i
		return -1
	}

	companion object {
		private const val CURRENT_VERSION = 1
		private val tableGravity = intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 4, 5, 3, 4, 5, 7, 10, -1)
		private val tableDenominator = intArrayOf(60, 30, 20, 15, 12, 10, 6, 4, 3, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1)
		private val tableBGMChange = intArrayOf(25, 75, 160, 250, -1)
		private val tableLevelChange =
			intArrayOf(0, 5, 10, 15, 20, 25, 35, 45, 55, 65, 75, 90, 115, 130, 145, 160, 180, 200, 220, 250, -1)
		private const val RANKING_MAX = 13

		private enum class Gametype(label:String? = null) {
			Regular, Spin, MaxSpeed, SpinMax("Spin MaxSpeed"), Mega, MegaMax("Mega MaxSpeed");

			val label:String = label ?: name
		}

		private val GAMETYPE_LABELS:Array<String> = Gametype.values().map {it.label}.toTypedArray()
		private val RANKING_TYPES = GAMETYPE_LABELS.size
		private const val GAMETYPE_REGULAR = 0
		private const val GAMETYPE_SPIN = 1
		private const val GAMETYPE_MAXSPEED = 2
		private const val GAMETYPE_MAXSPEED_SPIN = 3
		private const val GAMETYPE_MEGA = 4
		private const val GAMETYPE_MAXSPEED_MEGA = 5
		private const val starttime = 9000
		private const val timebonus = 1800
		private const val maxNumberLength = 13
		private val scoreCutoffValue:Long = (10.0).pow(maxNumberLength).toLong()
		private const val sciNotationLength = 17
	}
}
