/*
 * Copyright (c) 2022-2023,
 * This library class was created by 0xFC963F18DC21 / Shots243
 * It is part of an extension library for the game NullpoMino (copyright 2022-2023)
 *
 * Kotlin converted and modified by Venom=Nhelv
 *
 * Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 * Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.
 *
 * THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 *
 * Original Repository: https://github.com/Shots243/ModePile
 *
 * When using this library in a mode / library pack of your own, the following
 * conditions must be satisfied:
 *     - This license must remain visible at the top of the document, unmodified.
 *     - You are allowed to use this library for any modding purpose.
 *         - If this is the case, the Library Creator must be credited somewhere.
 *             - Source comments only are fine, but in a README is recommended.
 *     - Modification of this library is allowed, but only in the condition that a
 *       pull request is made to merge the changes to the repository.
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

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package wtf.oshisaure.nullpomodshit.modes

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
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
	private var score:Long = 0
	/** Score Rate Percentage*/
	private var scoreRate:Int = 0
	private var currenttime = 0
	private var ren = 0
	private var allclears = 0
	private var lastb2b = false
	private var incombo = false
	private var quads = 0
	private var bgmLv = 0
	private val rankingScore = List(RANKING_TYPES) {MutableList(RANKING_MAX) {0L}}
	private val rankingLevel = List(RANKING_TYPES) {MutableList(RANKING_MAX) {0}}
	private val rankingQuads = List(RANKING_TYPES) {MutableList(RANKING_MAX) {0}}
	private var rankingRank = 0
	private var version = 0
	private var lastscoreL:Long = 0
	private var ingame = false
	private var maxchain = 0
	private var maxmult:Int = 0
	private var scgettime = 0
	override val rankMap
		get() = rankMapOf(rankingScore.mapIndexed {a, x -> "$a.score" to x}+
			rankingLevel.mapIndexed {a, x -> "$a.level" to x}+
			rankingQuads.mapIndexed {a, x -> "$a.quads" to x})

	private val itemMode = StringsMenuItem(
		"gametype", "GAME MODE", COLOR.BLUE, 0, Gametype.all.map {it.label}
	)
	private var gametype:Int by DelegateMenuItem(itemMode)
	override val name:String get() = "Arcade Score Attack"
	override val menu = MenuList("arcadescoreattack", itemMode)

	override fun loadRanking(prop:CustomProperties) {
		for(i in 0 until RANKING_MAX) for(j in 0 until RANKING_TYPES) {
			rankingScore[j][i] = prop.getProperty("$j.score.$i", 0L)
			rankingLevel[j][i] = prop.getProperty("$j.level.$i", 0)
			rankingQuads[j][i] = prop.getProperty("$j.quads$i", 0)
		}
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

	override fun playerInit(engine:GameEngine) {
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
		bgmLv = 0
		rankingRank = -1
		rankingScore.forEach {it.fill(0)}
		rankingLevel.forEach {it.fill(0)}
		rankingQuads.forEach {it.fill(0)}
		if(!owner.replayMode) {
			version = 1
		}
		engine.owner.bgMan.bg = 0
		engine.frameColor = 7
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

	override fun onSetting(engine:GameEngine):Boolean {
		if(!engine.owner.replayMode) {
//			val change = updateMenu(engine)
			if(engine.ctrl.isPush(4)&&engine.statc[3]>=5) {
				engine.playSE("decide")
				return false
			}
			if(engine.ctrl.isPush(5)) engine.quitFlag = true
			engine.statc[3]++
		} else {
			engine.statc[3]++
			engine.statc[2] = -1
			if(engine.statc[3]>=60) return false
		}
		return true
	}

	override fun startGame(engine:GameEngine) {
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

	override fun renderLast(engine:GameEngine) {
		if(!owner.menuOnly) {
			receiver.drawScoreFont(engine, 0, 0, name, 8f)
			if(gametype!=0) receiver.drawScoreFont(engine, 2, 1, "(${GAMETYPE_LABELS[gametype]} MODE)", 9f)

			var colour:Int
			val event:Int = if(receiver.nextDisplayType==2) 6 else 4
			if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
				if(!owner.replayMode&&engine.ai==null) {
					val scale = if(receiver.nextDisplayType==2) 0.5f else 1.0f
					receiver.drawScoreFont(engine, 1, event-1, "LV QS SCORE", COLOR.PURPLE, scale)
					colour = 0
					while(colour<10) {
						receiver.drawScoreGrade(engine, -2, event+colour, String.format("%2d", colour+1), COLOR.YELLOW, scale)
						receiver.drawScoreFont(
							engine, 1, event+colour, "${rankingLevel[gametype][colour]+1}", colour==rankingRank,
							scale
						)
						receiver.drawScoreFont(
							engine, 4, event+colour, "${rankingQuads[gametype][colour]}", colour==rankingRank,
							scale
						)
						receiver.drawScoreFont(
							engine, 7, event+colour, formatScore(rankingScore[gametype][colour]),
							colour==rankingRank, scale
						)
						++colour
					}
				}
			} else {
				receiver.drawScoreFont(engine, 0, 3, "SCORE", 8f)
				receiver.drawScoreFont(engine, 0, 4, formatScore(score), 5f)
				if(lastscoreL!=0L)
					receiver.drawScoreFont(engine, 0, 5, "+"+formatScore(lastscoreL))

				receiver.drawScoreFont(engine, 0, 7, "LINES", 8f)
				if(engine.statistics.level>=19) {
					receiver.drawScoreFont(engine, 0, 8, engine.statistics.lines.toString())
				} else {
					receiver.drawScoreFont(
						engine,
						0,
						8,
						engine.statistics.lines.toString()+"/"+tableLevelChange[engine.statistics.level+1]
					)
				}
				receiver.drawScoreFont(engine, 0, 10, "LEVEL", 8f)
				receiver.drawScoreFont(engine, 0, 11, (engine.statistics.level+1).toString())
				receiver.drawScoreFont(engine, 10, 10, "QUADS", 8f)
				receiver.drawScoreFont(engine, 10, 11, "$quads", 6f)
				if(gametype!=4&&gametype!=5) {
					receiver.drawScoreFont(engine, 0, 13, "CHAIN", 8f)
					receiver.drawScoreFont(engine, 0, 14, "$ren", COLOR.RED)
				} else {
					receiver.drawScoreFont(engine, 0, 13, "ALL CLEARS", 8f)
					receiver.drawScoreFont(engine, 0, 14, "$allclears", COLOR.RED)
				}
				receiver.drawScoreFont(engine, 10, 13, "MULTIPLIER", 8f)
				receiver.drawScoreFont(engine, 10, 14, "${BaseFont.CROSS}$scoreRate%", 5f)
				receiver.drawScoreFont(engine, 0, 17, "TIME LEFT", 2f)
				receiver.drawScoreFont(engine, 0, 18, currenttime.toTimeStr)
				receiver.drawScoreFont(engine, 10, 17, "TOTAL TIME", 9f)
				receiver.drawScoreFont(engine, 10, 18, engine.statistics.time.toTimeStr)
			}
		}
	}

	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		if(ingame&&engine.statistics.time>0) {
			scgettime += scoreRate
			while(scgettime>=100) {
				engine.statistics.scoreBonus++
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

	override fun onGameOver(engine:GameEngine):Boolean {
		ingame = false
		return false
	}

	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		val li = ev.lines
		var pts = calcScoreBase(engine, ev)
		var maxlines:Int
		if(li!=0) {
			incombo = true
			maxlines = 2
			while(maxlines<=li) {
				pts *= maxlines
				++maxlines
			}
//			engine.playSE("b2b_"+if(ren==0) "start" else "continue")
			++ren
			lastb2b = li>=4||ev.twist
			quads += if(li>=4) 1 else 0
		} else {
			pts = 0
			incombo = false
		}
		if(li>=1&&engine.field.isEmpty) {
			engine.playSE("bravo")
			++allclears
		}
		if(!lastb2b&&!incombo) {
			if(ren!=0&&gametype!=4&&gametype!=5) {
				engine.playSE("b2b_end")
			}
			ren = 0
		}
		pts *= pts*scoreRate/100
		engine.statistics.scoreLine += pts
		score += pts

		if(tableBGMChange[bgmLv]!=-1) {
			if(engine.statistics.lines>=tableBGMChange[bgmLv]-5) {
				owner.musMan.fadeSW = true
			}
			if(engine.statistics.lines>=tableBGMChange[bgmLv]) {
				++bgmLv
				owner.musMan.bgm = BGMStatus.BGM.Generic(bgmLv)
				owner.musMan.fadeSW = false
			}
		}
		if(engine.statistics.lines>=tableLevelChange[engine.statistics.level+1]&&engine.statistics.level<19) {
			++engine.statistics.level
			currenttime += timebonus
			owner.bgMan.nextBg = engine.statistics.level
			setSpeed(engine)
			engine.playSE("levelup")
		}
		if(engine.statistics.level<19) {
			maxlines = tableLevelChange[engine.statistics.level+1]
			val minlines = tableLevelChange[engine.statistics.level]
			val linerange = maxlines-minlines
			val curlines = engine.statistics.lines-minlines
			val restlines = linerange-curlines
			engine.meterValue = curlines*1f/(linerange-1)
			engine.meterColor = 3
			if(restlines<=10) engine.meterColor = 2
			if(restlines<=4) engine.meterColor = 1
			if(restlines<=2) engine.meterColor = 0
		} else {
			engine.meterValue = currenttime*1f/'꿈'.code
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

	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 0, "FINAL SCORE", COLOR.PURPLE)
		receiver.drawMenuFont(engine, 0, 1, formatScore(score), COLOR.YELLOW)
		receiver.drawMenuFont(engine, 0, 2, "QUADS", COLOR.PURPLE)
		receiver.drawMenuFont(engine, 0, 3, "$quads", COLOR.CYAN)
		receiver.drawMenuFont(engine, 0, 4, "MAX CHAIN", COLOR.PURPLE)
		receiver.drawMenuFont(engine, 0, 5, "$maxchain", COLOR.RED)
		receiver.drawMenuFont(engine, 0, 6, "MAX MULTI.", COLOR.PURPLE)
		receiver.drawMenuFont(engine, 0, 7, "${BaseFont.CROSS}$maxmult", COLOR.YELLOW)
		drawResultStats(engine, receiver, 8, COLOR.PURPLE, Statistic.LINES, Statistic.LEVEL, Statistic.LPM, Statistic.PPS)
	}

	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
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

			companion object {
				val all = values()
			}
		}

		private val GAMETYPE_LABELS = Gametype.all.map {it.label}
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
