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
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.LevelMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

class MarathonActual:AbstractMode() {
	private var lastb2b = false
	private var lastcombo = 0
	private var lastpiece = 0
	private var bgmLv = 0

	private val itemLevel = LevelMenuItem("startlevel", "LEVEL", COLOR.RED, 0, 0..19, false, true)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	private var version = 0
	private var rankingRank = 0
	private val rankingPPS
		get() = List(minOf(rankingPieces.size, rankingTime.size)) {rankingPieces[it]*60f/rankingTime[it]}
	private val rankingPieces = MutableList(RANKING_MAX) {0}
	private val rankingTime = MutableList(RANKING_MAX) {0}
	override val rankMap
		get() = rankMapOf("pieces" to rankingPieces, "time" to rankingTime)
	private var totalLength = 0
	override val name:String = "Actual Marathon"
	override val menu = MenuList("actualmarathon", itemLevel, itemBig)

	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastscore = 0
		lastb2b = false
		lastcombo = 0
		lastpiece = 0
		bgmLv = 0
		totalLength = 0
		rankingRank = -1
		if(!owner.replayMode) {
			version = 1
		}
		engine.owner.bgMan.bg = 0
		engine.frameColor = 0
	}

	fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = 1
		engine.speed.denominator = 1
	}

	override fun onSetting(engine:GameEngine):Boolean {
		if(!engine.owner.replayMode) {
			val change:Int = this.updateMenu(engine)
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
		engine.statistics.level = startLevel
		engine.statistics.levelDispAdd = 1
		engine.b2bEnable = true
		engine.comboType = 1
		engine.big = big
		engine.twistAllowKick = true

		engine.twistEnable = true
		engine.useAllSpinBonus = true

		engine.twistEnableEZ = true
		totalLength = 0
		setSpeed(engine)
	}

	override fun renderLast(engine:GameEngine) {
		if(!owner.menuOnly) {
			receiver.drawScoreFont(engine, 0, 0, name, 4f)
			var topY:Int
			if(engine.gameActive) {
				receiver.drawScoreFont(engine, 0, 3, "SCORE", 1f)
				val strScore:String = if(lastscore!=0&&scDisp<120)
					"${engine.statistics.score}(+$lastscore)" else "${engine.statistics.score}(+$lastscore)"
				receiver.drawScoreFont(engine, 0, 4, strScore)
				receiver.drawScoreFont(engine, 0, 6, "LINE", 1f)
				receiver.drawScoreFont(engine, 0, 7, engine.statistics.lines.toString())
				receiver.drawScoreFont(engine, 0, 9, "LEVEL", 1f)
				receiver.drawScoreFont(engine, 0, 10, (engine.statistics.level+1).toString())
				topY = totalLength
				if(engine.stat==GameEngine.Status.MOVE&&engine.nowPieceObject!=null) {
					topY += engine.nowPieceY-engine.getSpawnPosY(engine.nowPieceObject)
				}
				receiver.drawScoreFont(engine, 0, 12, "DISTANCE LEFT", 1f)
				receiver.drawScoreFont(engine, 0, 13, ('ꓓ'.code-topY).toString())
				receiver.drawScoreFont(engine, 0, 15, "TIME", 1f)
				receiver.drawScoreFont(engine, 0, 16, engine.statistics.time.toTimeStr)
			} else if(!owner.replayMode&&!big&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) 0.5f else 1.0f
				topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 3, topY-1, "TIME     PIECE PPS", COLOR.BLUE, scale = scale)
				for(i in 0..9) {
					receiver.drawScoreGrade(engine, 0, topY+i, String.format("%2d", i+1), COLOR.YELLOW, scale = scale)
					receiver.drawScoreFont(engine, 3, topY+i, rankingTime[i].toTimeStr, rankingRank==i, scale)
					receiver.drawScoreFont(engine, 12, topY+i, "${rankingPieces[i]}", rankingRank==i, scale)
					receiver.drawScoreFont(engine, 18, topY+i, String.format("%.5g", rankingPPS[i]), rankingRank==i, scale)
				}
			}
		}
	}

	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		val dist = engine.nowPieceY-engine.getSpawnPosY(engine.nowPieceObject)
		totalLength += dist
		engine.statistics.scoreBonus += dist
		val pts = calcPower(engine, ev, true)
		if(pts>0) {
			lastscore = pts
			lastpiece = engine.nowPieceObject!!.id
			if(ev.lines>=1) engine.statistics.scoreLine += pts
			else engine.statistics.scoreBonus += pts
		}
		if(TABLE_BGM_CHANGE[bgmLv]!=-1) {
			if(totalLength>=TABLE_BGM_CHANGE[bgmLv]-50) {
				owner.musMan.fadeSW = true
			}
			if(totalLength>=TABLE_BGM_CHANGE[bgmLv]&&totalLength<MARATHON_LENGTH) {
				++bgmLv
				owner.musMan.bgm = BGMStatus.BGM.Generic(bgmLv)
				owner.musMan.fadeSW = false
			}
		}
		engine.meterValue = totalLength%LEVEL_LENGTH*1f/LEVEL_LENGTH
		engine.meterColor = GameEngine.METER_COLOR_LEVEL
		if(totalLength>=MARATHON_LENGTH) {
			engine.ending = 1
			engine.gameEnded()
			engine.stat = GameEngine.Status.EXCELLENT
		} else if(totalLength>=(engine.statistics.level+1)*LEVEL_LENGTH) {
			++engine.statistics.level
			owner.bgMan.nextBg = engine.statistics.level
			setSpeed(engine)
			engine.playSE("levelup")
		}
		return dist+pts
	}

	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreSD += fall*2
	}

	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreHD += fall*5
	}

	override fun renderResult(engine:GameEngine) {
		this.drawResultStats(engine, receiver, 0, COLOR.BLUE, Statistic.TIME, Statistic.PIECE, Statistic.PPM)
		this.drawResultRank(engine, receiver, 12, COLOR.BLUE, rankingRank)
	}

	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.pps, engine.statistics.totalPieceLocked, engine.statistics.time)
			return (rankingRank!=-1)
		}
		return false
	}

	private fun updateRanking(pps:Float, piece:Int, time:Int) {
		rankingRank = checkRanking(pps, piece, time)
		if(rankingRank!=-1) {
			for(i in 9 downTo rankingRank+1) {
				rankingPieces[i] = rankingPieces[i-1]
				rankingTime[i] = rankingTime[i-1]
			}
			rankingPieces[rankingRank] = piece
			rankingTime[rankingRank] = time
		}
	}

	private fun checkRanking(pps:Float, piece:Int, time:Int):Int {
		for(i in 0..9) if(time<rankingTime[i]||rankingTime[i]<0) return i
		else if(time==rankingTime[i]&&(piece<rankingPieces[i]||rankingPieces[i]==0)) return i
		else if(time==rankingTime[i]&&piece==rankingPieces[i]&&pps>rankingPPS[i]) return i
		return -1
	}

	companion object {
		private const val RANKING_MAX = 10
		private const val MARATHON_LENGTH = 42195
		private const val BGM_CHANGE_AT = 8439
		private val TABLE_BGM_CHANGE = intArrayOf(BGM_CHANGE_AT, BGM_CHANGE_AT*2, BGM_CHANGE_AT*3, BGM_CHANGE_AT*4, -1)
		private const val LEVEL_COUNT = 15
		private const val LEVEL_LENGTH = 2813
		private const val CURRENT_VERSION = 1
	}
}
