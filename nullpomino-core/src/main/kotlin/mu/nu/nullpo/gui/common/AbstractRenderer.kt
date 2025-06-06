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

package mu.nu.nullpo.gui.common

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR.*
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.common.BaseFont.FONT
import mu.nu.nullpo.gui.common.bg.AbstractBG
import mu.nu.nullpo.gui.common.bg.SpinBG
import mu.nu.nullpo.gui.common.bg.dtet.*
import mu.nu.nullpo.gui.common.bg.tech.Galaxy
import mu.nu.nullpo.gui.common.bg.tech.Snow
import mu.nu.nullpo.gui.common.bg.tech.Space
import mu.nu.nullpo.gui.common.fx.*
import mu.nu.nullpo.gui.common.fx.FragAnim.ANIM
import mu.nu.nullpo.gui.common.fx.particles.BlockParticle
import mu.nu.nullpo.gui.common.fx.particles.Fireworks
import mu.nu.nullpo.gui.common.fx.particles.LandingParticles
import mu.nu.nullpo.util.GeneralUtil.toInt
import kotlin.math.absoluteValue
import kotlin.random.Random

abstract class AbstractRenderer:EventReceiver() {
	internal abstract val resources:ResourceHolder
	open val bgType:List<AbstractBG<*>> by lazy {
		resources.imgPlayBG.map {i ->
			SpinBG(i, when(Random.nextInt(13)) {
				in 0..1 -> BGADNightClock(resources.imgPlayBGA.first {it.name.endsWith("_n")})
				in 2..3 -> BGAHBeams(resources.imgPlayBGA.first {it.name.endsWith("_b")})
				in 4..5 -> BGAMRush(resources.imgPlayBGA.first {it.name.endsWith("_r")})
				in 6..7 -> Galaxy()
				in 8..9 -> Space()
				in 10..11 -> Snow()
				else -> null
			})
		}
	}

	open val bgaType:List<AbstractBG<*>> by lazy {
		resources.imgPlayBGA.map {
			when {
				it.name.endsWith("_o") -> BGAAOcean(it)
				it.name.endsWith("_c") -> BGABCircleLoop(it)
				it.name.endsWith("_f") -> BGACFall(it)
				it.name.endsWith("_n") -> BGADNightClock(it)
				it.name.endsWith("_d") -> BGAEDeep(it)
				it.name.endsWith("_k") -> BGAFKaleidSq(it)
				it.name.endsWith("_t") -> BGAGTexture(it)
				it.name.endsWith("_b") -> BGAHBeams(it)
				it.name.endsWith("_m") -> BGAIMist(it)
				it.name.endsWith("_p") -> BGAJPrism(it)
				it.name.endsWith("_x") -> BGALExTrans(it)
				it.name.endsWith("_r") -> BGAMRush(it)
				else -> BGAKVWave(it)
			}
		}
	}

	protected val showLineEffect get() = heavyEffect>0
	protected val animBG get() = heavyEffect>1
	protected val particle get() = heavyEffect>2

	/** Background display */
	protected val showBG get() = conf.showBG
	/** Show Meter on right field */
	protected val showMeter get() = conf.showMeter
	/** Show ARE meter */
	protected val showSpeed get() = conf.showSpeed

	/** Show ghost piece as Outline */
	protected val outlineGhost get() = conf.outlineGhost

	/** fieldBackgroundの明るさ */
	protected val fieldBgBright get() = conf.fieldBgBright/255f

	/** 縁線を太くする */
	protected val edgeBold get() = conf.edgeBold

	/** Show field BG grid */
	protected val showFieldBgGrid get() = conf.showFieldBgGrid

	/** NEXT欄を暗くする */
	protected val darkNextArea get() = conf.darkNextArea

	/** ghost ピースの上にNEXT表示 */
	protected val nextShadow get() = conf.nextShadow

	/** Line clear effect speed */
	protected val lineEffectSpeed get() = conf.lineEffectSpeed

	/** 回転軸を表示する */
	protected val showCenter get() = conf.showCenter

	/** 操作ブロック降下を滑らかにする */
	protected val smoothFall get() = conf.smoothFall

	/** 高速落下時の軌道を表示する */
	protected val showLocus get() = conf.showLocus

	override fun setBGSpd(owner:GameManager, spd:Number, id:Int?) {
		val bgMax = resources.bgMax
		val bgaMax = resources.bgaMax
		val sp = spd.toFloat()
		if(!owner.menuOnly&&showBG&&animBG)
			if(id==null) {
				bgType.forEach {it.speed = sp}
				bgaType.forEach {it.speed = sp}
			} else if(id in 0..<bgMax)
				bgType[id].speed = sp
			else if(id<0&&id.absoluteValue in 1..bgaMax+1)
				bgaType[id.absoluteValue-1].speed = sp
	}

	override fun drawFont(x:Float, y:Float, str:String, font:FONT, color:COLOR, scale:Float, alpha:Float) {
		if(font==FONT.TTF) printTTFSpecific(x, y, str, color, scale, alpha)
		else printFontSpecific(x, y, str, font, color, scale, alpha)
	}

	/** Draw a block
	 * @param x X pos
	 * @param y Y pos
	 * @param color Color number for render
	 * @param skin Skin
	 * @param bone true to use bone block ([][][][])
	 * @param darkness Darkness or brightness
	 * @param alpha Alpha
	 * @param scale Size (.5f, 1f, 2f)
	 * @param attr Attribute
	 */
	/* 1マスBlockを描画 */
	override fun drawBlock(
		x:Float, y:Float, color:Int, skin:Int, bone:Boolean, darkness:Float, alpha:Float, scale:Float,
		attr:Int
	) {
		val sk = skin%resources.imgBlockListSize
		if(color<0) return

		val isSpecialBlocks = color>=Block.COLOR.COUNT
		val isSticky = resources.getBlockIsSticky(sk)

		val sx = if(isSticky) if(isSpecialBlocks)
			color-Block.COLOR.COUNT
		else
			(attr and Block.ATTRIBUTE.CONNECT_UP.bit>0).toInt()+
				(attr and Block.ATTRIBUTE.CONNECT_DOWN.bit>0).toInt()*2+
				(attr and Block.ATTRIBUTE.CONNECT_LEFT.bit>0).toInt()*4+
				(attr and Block.ATTRIBUTE.CONNECT_RIGHT.bit>0).toInt()*8
		else color+(bone).toInt()*9-(Block.COLOR.COUNT+18)*isSpecialBlocks.toInt()
		val sy = if(isSticky) if(isSpecialBlocks) 18 else color+(bone).toInt()*9 else 0

		drawBlockSpecific(x, y, sx, sy, sk, BS*scale, darkness, alpha)
	}

	/* 勲章を描画 */
	override fun drawBadges(x:Float, y:Float, width:Int, nums:Int, scale:Float) {
		var n = nums
		var nx = x
		var ny = y
		var z:Int
		val b = FontBadge.b
		var mh = 0
		while(n>0) {
			z = 0
			while(z<b.size-1&&n>=b[z+1]) z++
			val w = FontBadge(z).w*scale
			val h = FontBadge(z).h
			if(h>mh) mh = h
			drawBadgesSpecific(nx, ny, z, scale)
			n -= b[z]
			nx += w
			if(nx>x+width) {
				nx = x
				ny += mh*scale
				mh = 0
			}
		}
	}

	class FontBadge(type:Int) {
		val type = type.coerceIn(0, b.size-1)
		val sx = listOf(0, 10, 20, 30, 0, 10, 20, 30, 0, 20, 40, 0)[type]
		val sy = listOf(0, 0, 0, 0, 14, 14, 14, 14, 24, 24, 0, 44)[type]
		val w = listOf(10, 10, 10, 10, 10, 10, 10, 10, 20, 20, 32, 64)[type]
		val h = listOf(10, 10, 14, 14, 14, 14, 15, 15, 15, 15, 32, 48)[type]

		companion object {
			val b = listOf(1, 5, 10, 50, 100, 500, 1000, 5000, 10000, 50000, 100000, 500000)
		}
	}

	protected abstract fun drawBG(engine:GameEngine)
	protected abstract fun drawFrameSpecific(x:Float, y:Float, engine:GameEngine)
	private fun drawFrame(lX:Float, tY:Float, engine:GameEngine, inside:()->Unit) {
		// Upと下
		val s = engine.blockSize
		val fieldW = engine.field.width// ?: Field.DEFAULT_WIDTH
		val fieldH = engine.field.height//?: Field.DEFAULT_HEIGHT
		val fW = fieldW*s
		val fH = fieldH*s
		val cX = lX+fW/2f
		val rX = lX+fW
		val cY = tY+fH/2f
		val bY = tY+fH
		// NEXT area background
		if(showBG&&darkNextArea) {
			when(nextDisplayType) {
				0 -> {
					val w = fW+15f
					fillRectSpecific(lX+20, tY, w-40f, nextHeight*-1f, 0)
					for(i in 20 downTo 0)
						drawRectSpecific(lX+20-i, tY, w-40f+i*2, nextHeight*-1f-i, 0, 1-i/20f, 1f)
				}
				1 -> {//side small
					val x2 = lX+s+fieldW*s
					val maxNext = if(engine.isNextVisible) engine.ruleOpt.nextDisplay else 0

					// HOLD area
					if(engine.ruleOpt.holdEnable&&engine.isHoldVisible) {
						fillRectSpecific(lX-32, tY, 32f, 64f, 0)
						for(i in 8 downTo 0)
							drawRectSpecific(lX-32-i, tY-i, 33f+i, 64f+i*2, 0, 1-i/8f, 1f)
					}

					// NEXT area
					if(maxNext>0) {
						fillRectSpecific(x2, tY+8, 32f, 32f*maxNext-16, 0)
						for(i in 8 downTo 0)
							drawRectSpecific(x2-1, tY+8-i, 33f+i, 32f*maxNext-16+i*20, 0, 1-i/8f, 1f)
					}
				}
				2 -> {//side big
					val x2 = lX+s+fW
					val maxNext = if(engine.isNextVisible) engine.ruleOpt.nextDisplay else 0

					// HOLD area
					if(engine.ruleOpt.holdEnable&&engine.isHoldVisible) {
						fillRectSpecific(lX-64, tY, 64f, nextHeight*1f, 0)
						for(i in 8 downTo 0)
							drawRectSpecific(lX-64-i, tY-i, 65f+i, nextHeight+i*2f, 0, i/8f, 1f)
					}
					// NEXT area
					if(maxNext>0) {
						drawRect(x2, tY+8, 64f, 64f*maxNext-16, 0)
						for(i in 8 downTo 0)
							drawRectSpecific(x2-1, tY+8-i, 65f+i, 64f*maxNext-16+i*2, 0, i/8f, 1f)
					}
				}
			}
		}
// Field Background
		if(fieldBgBright>0)
			if(fieldW<=10&&fieldH<=20) {
				var img = resources.imgFieldBG[1]
				if(engine.displaySize==-1) img = resources.imgFieldBG[0]
				if(engine.displaySize==1) img = resources.imgFieldBG[2]
				img.draw(lX, tY, rX, bY, 0f, 0f, 0f+fW, 0f+fH, fieldBgBright)
			} else if(showBG) drawRect(lX, tY, 0f+fW, 0f+fH, 0, fieldBgBright)

		inside()
		drawFrameSpecific(lX, tY, engine)

		when(engine.frameSkin) {
			GameEngine.FRAME_SKIN_GB -> {
				drawRect(lX.toInt(), tY.toInt(), fW, fH, 0x888888)
				val fi = resources.imgFrameOld[0]
				for(i in 0..fieldH) {
					val zx = lX.toInt()-s
					val zy = tY.toInt()+i*s
					fi.draw(zx, zy, zx+s, zy+s, 0, 0, 16, 16)
					fi.draw(zx+fW+s, zy, zx+fW+s*2, zy+s, 0, 0, 16, 16)
					if(i==fieldH)
						for(z in 1..fieldW)
							fi.draw((zx+z*s), zy, zx+(z+1)*s, zy+s, 0, 0, 16, 16)
				}
			}
			GameEngine.FRAME_SKIN_SG -> {
				drawRect(lX.toInt(), tY.toInt(), fW, fH, 0)

				val fi = resources.imgFrameOld[1]
				val mW = fW+s*2
				fi.draw(lX-s, tY-s, lX-s+mW/2, tY-s+(fieldH+2)*s, 0, 0, 96, 352)
				fi.draw(lX-s+mW, tY-s, lX-s+mW/2, tY-s+(fieldH+2)*s, 0, 0, 96, 352)
			}
			GameEngine.FRAME_SKIN_HEBO -> {
				val fi = resources.imgFrameOld[2]

				//Top

				fi.draw(lX-s, tY-s, lX+s, tY, 0, 0, 32, 16)
				fi.draw(rX+s, tY-s, rX-s, tY, 0, 0, 32, 16)

				fi.draw(lX+s, tY-s, rX-s, tY, 32, 0, 48, 16)

				//Side Wall
				fi.draw(lX-s, tY, lX, bY, 0, 15, 16, 16)
				fi.draw(rX+s, tY, rX, bY, 0, 15, 16, 16)

				//Bottom
				fi.draw(lX-s, bY, lX, bY+s*3, 0, 16, 16, 48)
				fi.draw(lX, bY, rX, bY+s*3, 14, 16, 15, 48)
				fi.draw(rX+s, bY, rX, bY+s*3, 0, 16, 16, 48)
			}
			/*else -> if(engine.framecolor>0) {
				val mW = maxWidth+s
				val fi = resources.imgFrame[engine.framecolor]
				val oX = 0
			}*/
		}

		if(showMeter) {
			// 右Meter
			val mH = fH+s
			val mainH = mH*engine.meterValue.coerceIn(0f, 1f)
			val subH = mH*engine.meterValueSub.coerceIn(0f, 1f)
			val mW = maxOf(2f, s/4f)
			val zx = rX+s/2f-mW/2f
			val zy = tY-s/2f
			drawRect(zx, zy, mW, mH.toFloat(), 0)

			if(engine.meterValueSub>engine.meterValue) {
				drawRect(
					zx+mW/2, zy+mH-subH, mW/2, subH,
					getMeterColorHex(engine.meterColorSub, engine.meterValueSub)
				)
			}
			if(engine.meterValue>0) {
				drawRect(
					zx, zy+mH-mainH, mW, mainH,
					getMeterColorHex(engine.meterColor, engine.meterValue)
				)
			}
			if(engine.meterValueSub>0&&engine.meterValueSub<=engine.meterValue) {
				val ov = minOf(mainH, subH)
				drawRect(
					zx+mW/2f, zy+mH-ov, mW, ov,
					getMeterColorHex(engine.meterColorSub, engine.meterValueSub), .5f
				)
			}
		}
		if(showSpeed) {
			//下Meter 残りlockdelayとARE
			val smX = lX//+s/2f
			val smY = bY+s/2f+1
			val mW = fW+0f//-s/2f
			val mH = maxOf(2f, s/4f)
			val smY1 = bY+s/2f-mH
			val smY2 = smY1+mH/2

			val moveLimit = engine.ruleOpt.lockResetMoveLimit
			val spinLimit = if(engine.ruleOpt.lockResetLimitShareCount) moveLimit else engine.ruleOpt.lockResetSpinLimit
			val moveCount = if(engine.ruleOpt.lockResetLimitShareCount)
				engine.extendedMoveCount+engine.extendedSpinCount else engine.extendedMoveCount
			val spinCount = if(engine.ruleOpt.lockResetLimitShareCount) moveCount else engine.extendedSpinCount

			moveLimit.let {l ->
				if(l>0) repeat(l) {
					fillRectSpecific(smX+(mW+1)*it/l, smY1, mW/l-1, mH/2, 0)
				}
			}
			spinLimit.let {l ->
				if(l>0) repeat(l) {
					fillRectSpecific(smX+(mW+1)*it/l, smY2, mW/l-1, mH/2, 0)
				}
			}


			fillRectSpecific(smX, smY, mW, mH, 0)
			when(engine.stat) {
				GameEngine.Status.MOVE -> {
					if(engine.lockDelay>0) {
						val value = maxOf(0f, mW-engine.lockDelayNow*mW/engine.lockDelay)
						fillRectSpecific(smX+(mW-value)/2f, smY, value, mH, if(engine.lockDelayNow>0) 0xFFFF00 else 0x00FF00)
					} else fillRectSpecific(smX, smY, mW, mH, 0xFF0000)
					moveLimit.let {l ->
						if(l>0) repeat(moveLimit-moveCount) {
							fillRectSpecific(smX+(mW+1)*(it+moveCount/2)/l, smY1, mW/l-1, mH/2, 0x00FF80)
						}
					}
					spinLimit.let {l ->
						if(l>0) repeat(moveLimit-spinCount) {
							fillRectSpecific(smX+(mW+1)*(it+spinCount/2)/l, smY2, mW/l-1, mH/2, 0x0080FF)
						}
					}
				}
				GameEngine.Status.LOCKFLASH -> {
					fillRectSpecific(smX, smY, mW, mH, 0xFFFFFF)
					if(engine.ruleOpt.lockFlash>0) {
						val value = engine.statc[0]*mW/engine.ruleOpt.lockFlash
						fillRectSpecific(smX+(mW-value)/2f, smY, value, mH, 0x808080)
					}
				}
				GameEngine.Status.LINECLEAR -> if(engine.lineDelay>0) {
					val value = mW-engine.statc[0]*mW/engine.lineDelay
					fillRectSpecific(smX+(mW-value)/2f, smY, value, mH, 0x00FFFF)
				}
				GameEngine.Status.ARE -> if(engine.statc[1]>0) {
					fillRectSpecific(
						smX, smY, mW, mH,
						if(engine.ruleOpt.areCancelMove||engine.ruleOpt.areCancelSpin||engine.ruleOpt.areCancelHold) 0xFF8000 else 0xFFFF00
					)
					val value = maxOf(0f, mW-engine.statc[0]*mW/engine.statc[1])
					fillRectSpecific(smX+(mW-value)/2f, smY, value, mH, 0)
				}
				else -> {
				}
			}
			val zy = fieldH+1.1f
			val spd = engine.speed
//			engine.playerID
			val g = spd.gravity
			val d = spd.denominator
			drawMenuNum(engine, 0, zy, if(g<0||d<0) fieldH*1f else g*1f/d, 7 to 4)
			drawMenuNano(engine, 1.5f, zy, "GRAVITY", scale = .5f)
			drawMenuSpeed(engine, 1, zy, g, d, 5)

			for(i in 0..1) {
				val show = if(i==0) "ARE" to spd.are else "LINE" to spd.areLine
				drawMenuNum(engine, 6+i*3, zy, String.format(if(i==0) "%2d/" else "%2d", show.second))
				drawMenuNano(engine, 5+i*2.5f, zy+.5f, show.first, scale = .5f)
			}
			for(i in 0..2) {
				val show = when(i) {
					0 -> "LINE" to spd.lineDelay; 1 -> "LOCK" to spd.lockDelay; else -> "DAS" to spd.das
				}
				drawMenuNum(engine, 8-i*3, zy+1, String.format(if(i==1) "%2d+" else "%2d", show.second))
				drawMenuNano(engine, 6.5f-i*3, zy+1f, show.first, scale = .5f)
			}
			drawMenuNano(engine, 0f, zy+1.5f, "DELAYS", scale = .5f)
		}

		for(i in 0..engine.lives)
			drawDia(
				lX-.5f*s, bY-(1+i+.5f)*s, s*2f/3, s*2f/3,
				engine.statc[0]/(14f-engine.speed.rank*10f), alpha = .75f,
				outlineColor = getColorByID(COLOR.all.let {it[(i+1)%(it.size-1)]}), outlineW = 2f
			)
	}

	/** Currently working onBlockDraw a piece
	 * (Y-coordinateThe0MoreBlockDisplay only)
	 * @param x X-coordinate of base field
	 * @param y Y-coordinate of base field
	 * @param engine GameEngineInstance of
	 * @param scale Display magnification
	 */
	protected fun drawCurrentPiece(x:Float, y:Float, engine:GameEngine, scale:Float) {
		val blkSize = (engine.blockSize*scale)
		val pX = engine.nowPieceX
		val pY = engine.nowPieceY
		var g = engine.fpf

		val isRetro = engine.isRetroSkin
		val ys =
			if(!smoothFall||pY>=engine.nowPieceBottomY||isRetro) 0f else engine.gCount*blkSize/engine.speed.denominator%blkSize
		//if(engine.harddropFall>0)g+=engine.harddropFall;
		if(!showLocus||isRetro) g = 0

		val dX = x+pX*blkSize
		val dY = y+pY*blkSize+ys
		engine.nowPieceObject?.let {p ->
			p.dataX[p.direction].zip(p.dataY[p.direction].withIndex()).groupBy({it.first}, {it.second})
				.forEach {(x2, c) ->
					val (i, y2) = c.minBy {it.value}
					val b = Block(p.block[i]).apply {darkness = 0f}
					for(z in 0..g) drawBlock(
						x+((x2+pX)*blkSize), y+((y2+pY-z)*blkSize), b,
						-.1f, .4f, scale*if(engine.big) 2 else 1
					)
				}
			drawPiece(dX, dY, p, scale*if(engine.big) 2 else 1, -.25f)
			val outline = if(engine.statc[0]%2==0||!engine.holdDisable) if(edgeBold) 2f else 1f else 0f
			if(outline>0) p.block.forEachIndexed {i, blk ->
				val oColor = if(engine.statc[0]%2==1) 0xFFFFFF else
					getColorByID(
						engine.holdPieceObject?.block[i]?.run {if(getAttribute(Block.ATTRIBUTE.BONE)) Block.COLOR.WHITE else color}
							?:Block.COLOR.BLACK)
				val bX = pX+p.dataX[p.direction][i]
				val bY = pY+p.dataY[p.direction][i]
				val x = x+bX*blkSize
				val y = y+bY*blkSize+ys
				if(!blk.getAttribute(Block.ATTRIBUTE.CONNECT_UP)&&engine.field.getBlockEmpty(bX, bY-1, false))
					drawLineSpecific(x, y, x+blkSize, y, oColor, w = outline)
				if(!blk.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)&&engine.field.getBlockEmpty(bX, bY+1, false))
					drawLineSpecific(x, y+blkSize, x+blkSize, y+blkSize, oColor, w = outline)
				if(!blk.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)&&engine.field.getBlockEmpty(bX-1, bY, false))
					drawLineSpecific(x, y, x, y+blkSize, oColor, w = outline)
				if(!blk.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)&&engine.field.getBlockEmpty(bX+1, bY, false))
					drawLineSpecific(x+blkSize, y, x+blkSize, y+blkSize, oColor, w = outline)
			}
			if(engine.nowPieceSteps<10) drawDirectNano(
				x+(pX+p.spinCX-.1f)*blkSize, dY,
				"${engine.nowPieceSteps}/${p.finesseLimit(engine.nowPieceX)}", WHITE, .5f
			)
			if(showCenter) drawDia(
				x+(pX+p.spinCX+.5f)*blkSize, y+(pY+p.spinCY+.5f)*blkSize+ys, blkSize*2/3, blkSize*2/3,
				engine.statc[0]/(14f-engine.speed.rank*10f), alpha = .75f,
				outlineColor = getColorByID(p.block[engine.statc[0]%p.block.size].run {
					if(getAttribute(Block.ATTRIBUTE.BONE)) Block.COLOR.WHITE else color
				}), outlineW = 2f
			)
		}
	}

	/** fieldのBlockを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 */
	protected fun drawField(x:Float, y:Float, engine:GameEngine, size:Int, scale:Float = 1f) {
		class Connected(val u:Boolean, val d:Boolean, val l:Boolean, val r:Boolean)
		val (blkSize, zoom) = (engine.blockSize to scale).let {
			when {
				size<=-1 -> it.first/2 to it.second/2
				size>=1 -> it.first*2 to it.second*2
				else -> it
			}
		}

		val field = engine.field
		val width = field.width
		val height = field.height
		var viewHeight = field.height

		if(engine.heboHiddenEnable&&engine.gameActive) viewHeight -= engine.heboHiddenYNow

		val outlineType = if(engine.owBlockOutlineType==-1) engine.blockOutlineType else engine.owBlockOutlineType

		for(i in -field.hiddenHeight..<viewHeight)
			for(j in 0..<width) {
				val x2 = x+j*blkSize
				val y2 = y+i*blkSize

				field.getBlock(j, i)?.also {
					if(it.getAttribute(Block.ATTRIBUTE.WALL))
						drawBlock(x2, y2+it.offsetY*blkSize, 0, it.skin, it.getAttribute(Block.ATTRIBUTE.BONE),
							it.darkness, if(it.getAttribute(Block.ATTRIBUTE.ERASE)) it.alpha/2 else it.alpha, zoom, it.aint)
					else if(it.color!=null) {
						if(engine.owner.replayMode&&engine.owner.replayShowInvisible)
							drawBlockForceVisible(x2, y2, it, scale)
						else if(it.getAttribute(Block.ATTRIBUTE.VISIBLE)) drawBlock(x2, y2, it, scale = scale)

						if(it.getAttribute(Block.ATTRIBUTE.OUTLINE)&&!it.getAttribute(Block.ATTRIBUTE.BONE)) {
							val ls = blkSize-1
							val w = if(edgeBold) 2f else 1f

							val color = getColorByID(
								if(outlineType==GameEngine.BLOCK_OUTLINE_SAMECOLOR&&it.color!=null) it.color
								else Block.COLOR.WHITE)
							val side = when(outlineType) {
								GameEngine.BLOCK_OUTLINE_NORMAL -> Connected(
									field.getBlockEmpty(j, i-1, false),
									field.getBlockEmpty(j, i+1, false),
									field.getBlockEmpty(j-1, i, false),
									field.getBlockEmpty(j+1, i, false))
								GameEngine.BLOCK_OUTLINE_CONNECT -> Connected(
									field.getCoordVaild(j, i-1)&&!it.getAttribute(Block.ATTRIBUTE.CONNECT_UP),
									field.getCoordVaild(j, i+1)&&!it.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN),
									field.getCoordVaild(j-1, i)&&!it.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT),
									field.getCoordVaild(j+1, i)&&!it.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT))
								GameEngine.BLOCK_OUTLINE_SAMECOLOR -> Connected(
									field.getCoordVaild(j, i-1)&&field.getBlockColor(j, i-1)!=it.color,
									field.getCoordVaild(j, i+1)&&field.getBlockColor(j, i+1)!=it.color,
									field.getCoordVaild(j-1, i)&&field.getBlockColor(j-1, i)!=it.color,
									field.getCoordVaild(j+1, i)&&field.getBlockColor(j+1, i)!=it.color)
								else -> Connected(false, false, false, false)
							}
							if(side.u) drawLineSpecific(x2-w/2*side.l.toInt(), y2-w/2, x2+ls+w/2*side.r.toInt(), y2-w/2, color, w = w)
							if(side.d) drawLineSpecific(x2-w/2*side.l.toInt(), y2+ls+w/2, x2+ls+w/2*side.r.toInt(), y2+ls+w/2, color,
								w = w)
							if(side.l) drawLineSpecific(x2-w/2, y2-w/2*side.u.toInt(), x2-w/2, y2+ls+w/2*side.d.toInt(), color, w = w)
							if(side.r) drawLineSpecific(x2+ls+w/2, y2-w/2*side.u.toInt(), x2+ls+w/2, y2+ls+w/2*side.d.toInt(), color,
								w = w)
						}
					}
				}
			}
		drawFieldSpecific(x, y, width, viewHeight, blkSize, zoom, outlineType)

		// BunglerHIDDEN
		field.let {
			if(engine.heboHiddenEnable&&engine.gameActive) {
				var maxY = engine.heboHiddenYNow
				if(maxY>height) maxY = height
				for(i in 0..<maxY)
					for(j in 0..<width)
						drawBlock(x+j*blkSize, y+(height-1-i)*blkSize, (x+y).toInt()%2, 0, scale = zoom)
			}
		}
	}

	/** NEXTを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 */
	private fun drawNext(x:Float, y:Float, engine:GameEngine) {
		val fldWidth = engine.field.width+1
		val fbs = engine.blockSize.toFloat()
		val cps = fldWidth*fbs
		val cX = x+cps/2
		val rX = x+cps
		if(engine.isNextVisible&&engine.ruleOpt.nextDisplay>=1) {
			val pid = engine.nextPieceCount
			drawFont(x, y-fbs, "${engine.nextPieceArraySize}", FONT.NANO, ORANGE, .5f)
			printFontSpecific(x, y-fbs/2, "${engine.statistics.randSeed}", FONT.NANO, WHITE, .5f, .5f)
			engine.getNextObject(pid)?.let {
				//int x2 = x + 4 + ((-1 + (engine.field.getWidth() - piece.getWidth() + 1) / 2) * 16);
				val x2 = x+engine.getSpawnPosX(it, engine.field)*fbs //Rules with spawn x modified were misaligned.
				val y2 = y+engine.getSpawnPosY(it, engine.field)*fbs
				val ym = y+(engine.getSpawnPosY(it, null)-engine.ruleOpt.pieceEnterMaxDistanceY)*fbs
				drawPieceOutline(x2, ym, it, fbs, .5f)
				if(engine.ruleOpt.fieldCeiling||!engine.ruleOpt.pieceEnterAboveField)
					drawPieceOutline(x2, y2, it, fbs, .5f)
				else drawPiece(x2, y2, it)
				drawFont(cX-45, y-26, "%3d".format(pid), FONT.NANO, if(pid%7==0) YELLOW else WHITE, .75f)
			}
			if(engine.ruleOpt.nextDisplay>1) when(nextDisplayType) {
				2 -> {
					drawFont(rX, y-fbs/2, "QUEUE", FONT.NANO, ORANGE)
					for(i in 0..<minOf(engine.ruleOpt.nextDisplay, 14)) {
						engine.getNextObject(pid+i)?.let {
							val centerX = ((4-it.width-1)*fbs)/2-it.minimumBlockX*fbs
							val centerY = ((4-it.height-1)*fbs)/2-it.minimumBlockY*fbs
							val pY = y+i*3*fbs
							drawPiece(rX+centerX, pY+centerY, it, 1f)
							(pid+i+1).let {n -> if(n%7==0) drawFont(rX, pY, "$n", FONT.NANO, YELLOW, .5f)}
						}
					}
				}
				1 -> {
					drawFont(rX, y, "QUEUE", FONT.NANO, ORANGE)
					for(i in 0..<minOf(engine.ruleOpt.nextDisplay, 14)) {
						engine.getNextObject(pid+i)?.let {
							val centerX = ((4-it.width-1)*fbs)/4-it.minimumBlockX*fbs/2
							val centerY = ((4-it.height-1)*fbs)/4-it.minimumBlockY*fbs/2
							val pY = y+i*2*fbs
							drawPiece(rX+centerX, pY+centerY, it, .5f)
							(pid+i+1).let {n -> if(n%7==0) drawFont(rX, pY, "$n", FONT.NANO, YELLOW, .5f)}
						}
					}
				}
				else -> {
					drawFont(cX-16, y-nextHeight, "NEXT", FONT.NANO, ORANGE)
					// NEXT1~4
					for(i in (0..<minOf(3, engine.ruleOpt.nextDisplay-1)).reversed())
						engine.getNextObject(pid+i+1)?.let {
							val pX = rX+(-2+i)*fbs*2
							val cY = (i-2)*fbs/2
							drawPiece(pX+((4-it.width-1)*fbs)/4-it.minimumBlockX*fbs/2, y+cY-(it.maximumBlockY+1)*fbs/2, it, .75f)
							(pid+i+1).let {n -> if(n%7==0) drawFont(pX, y+cY-6, "$n", FONT.NANO, YELLOW, .75f)}
						}
					if(engine.ruleOpt.nextDisplay>=5) {
						// NEXT5~
						for(i in (0..<engine.ruleOpt.nextDisplay-3).reversed())
							engine.getNextObject(pid+i+4)?.let {
								val pY = (1+i)*24
								drawPiece(rX+((4-it.width-1)*fbs)/4-it.minimumBlockX*fbs/2, y+pY-(it.maximumBlockY+1)*8, it, .5f)
								(pid+i+4).let {n -> if(n%7==0) drawFont(rX, y+pY-6, "$n", FONT.NANO, YELLOW, .5f)}
							}
					}
				}
			}
		}

		if(engine.isHoldVisible&&engine.ruleOpt.holdEnable) {
			// HOLD
			val holdRemain = engine.ruleOpt.holdLimit-engine.holdUsedCount
			val x2 = if(sideNext) x-fbs*3 else x+fbs
			val y2 = if(sideNext) y else y-fbs*3
			if(engine.ruleOpt.holdLimit<0||holdRemain>0) {
				var str = "SWAP"
				var tempColor = if(engine.holdDisable) WHITE else GREEN
				if(engine.ruleOpt.holdLimit>=0) {
					str += "\ne$holdRemain"
					if(!engine.holdDisable&&holdRemain>0&&holdRemain<=10)
						tempColor = if(holdRemain<=5) RED else YELLOW
				}
				drawFont(x2, y2, str, FONT.NANO, tempColor, .75f)

				engine.holdPieceObject?.let {
					val dark = if(engine.holdDisable) .3f else 0f
					it.resetOffsetArray()
					it.setDarkness(0f)

					when(nextDisplayType) {
						2 -> {
							val centerX = ((4-it.width-1)*fbs)/2-it.minimumBlockX*fbs
							val centerY = ((4-it.height-1)*fbs)/2-it.minimumBlockY*fbs
							drawPiece(x-64+centerX, y+centerY, it, 1f, dark, outline = 1f)
						}
						1 -> {
							val centerX = ((4-it.width-1)*fbs)/4-it.minimumBlockX*fbs/2
							val centerY = ((4-it.height-1)*fbs)/4-it.minimumBlockY*fbs/2
							drawPiece(x2+centerX, y+centerY, it, .5f, dark, outline = 1f)
						}
						else -> drawPiece(x2, y-fbs-(it.maximumBlockY+1)*8, it, .5f, dark, outline = 1f)
					}
				}
			}
		}
	}

	/** 現在操作中のBlockピースのghost を描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 * @param scale 表示倍率
	 */
	protected open fun drawGhostPiece(x:Float, y:Float, engine:GameEngine, scale:Float) {
		val blksize = (BS*scale)
		val px = (engine.nowPieceX*blksize+x)
		val py = (engine.nowPieceBottomY*blksize+y)
		engine.nowPieceObject?.let {
			drawPiece(px, py, it, scale, .3f, .7f)
			if(outlineGhost) drawPieceOutline(px, py, it, blksize, 1f, true)
		}
	}

	protected open fun drawHintPiece(x:Float, y:Float, engine:GameEngine, scale:Float) {
		val ai = engine.ai?:return
		engine.aiHintPiece?.let {
			val blkSize = BS*scale
			val px = (ai.bestX*blkSize+x)
			val py = (ai.bestY*blkSize+y)
			it.direction = ai.bestRt
			drawPieceOutline(px, py, it, blkSize)
		}
	}

	/** Draw shadow nexts
	 * @param x X coord
	 * @param y Y coord
	 * @param engine GameEngine
	 */
	protected fun drawShadowNexts(x:Float, y:Float, engine:GameEngine) {
		val blkSize = engine.blockSize.toFloat()
		engine.getNextObject(engine.nextPieceCount)?.let {next ->
			val sx = engine.getSpawnPosX(next)
			val sy = next.getBottom(sx, engine.getSpawnPosY(next), engine.field)
			drawPieceOutline(x+(blkSize*sx).toInt(), y+(blkSize*sy).toInt(), next, blkSize, .5f, true)
		}
		engine.holdPieceObject?.let {hold ->
			val sx = engine.getSpawnPosX(hold)
			val sy = hold.getBottom(sx, engine.getSpawnPosY(hold), engine.field)
			drawPieceOutline(x+(blkSize*sx).toInt(), y+(blkSize*sy).toInt(), hold, blkSize, .5f/3)
		}
		engine.nowPieceObject?.let {piece ->
			val shadowX = engine.nowPieceX
			val shadowY = engine.nowPieceBottomY+piece.minimumBlockY
			val size = if(piece.big||engine.displaySize==1) 2 else 1
			val shadowCenter = blkSize*(piece.minimumBlockX*2+(piece.width+size))/2

			for(i in 0..<engine.ruleOpt.nextDisplay-1) {
				if(i>=3) break

				engine.getNextObject(engine.nextPieceCount+i)?.let {next ->
					val vPos = (blkSize*shadowY).toInt()-(i+1)*24-8
					val nextCenter = blkSize*(next.minimumBlockX*2+(next.width+1))/4

					if(vPos>=-blkSize/2)
						drawPiece(x+(blkSize*shadowX+shadowCenter-nextCenter).toInt(), y+vPos, next, blkSize/2f/BS, .25f, .75f)
				}
			}
			engine.holdPieceObject?.let {hold ->
				val nextCenter = blkSize*(hold.minimumBlockX*2+(hold.width+1))/4

				drawPiece(x+(blkSize*shadowX-nextCenter).toInt(), y+(blkSize*shadowY).toInt(), hold, blkSize/2f/BS, .25f, .25f)
			}
		}
	}

	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		bgType.forEach {it.reset()}
		bgaType.forEach {it.reset()}
//		manager.bgMan.fadeEnabled=heavyEffect>0
	}

	/* Ready画面の描画処理 */
	override fun renderReady(engine:GameEngine) {
		if(!engine.allowTextRenderByReceiver) return
		//if(engine.isVisible == false) return;

		if(engine.statc[0]>0) {
			val cy = (engine.fieldHeight-2)/2
			if(engine.statc[0] in engine.readyStart..<engine.readyEnd)
				drawMenuFont(engine, 0, cy, "READY", WHITE, 2f)
			else if(engine.statc[0] in engine.goStart..<engine.goEnd)
				drawMenuFont(engine, 2, cy, "GO!", WHITE, 2f)

			drawMenuNano(engine, 0f, cy+2.25f, "TODAY SEEDS:", WHITE)
			drawMenuNano(engine, 0f, cy+3f, "${engine.statistics.randSeed}", WHITE, 0.75f)
		}
	}

	/* Blockピース移動時の処理 */
	override fun renderMove(engine:GameEngine) {
		if(!engine.isVisible) return

		val offsetX = engine.fX
		val offsetY = engine.fY

		if(engine.statc[0]>1||engine.ruleOpt.moveFirstFrame) {
			val scale = getScaleF(engine.displaySize)
			if(engine.ai!=null&&engine.aiShowHint&&engine.aiHintReady) drawHintPiece(offsetX, offsetY, engine, scale)
			if(engine.ghost&&engine.ruleOpt.ghost) drawGhostPiece(offsetX*1f, offsetY*1f, engine, scale)
			drawCurrentPiece(offsetX, offsetY, engine, scale)
		}
	}

	override fun renderLockFlash(engine:GameEngine) {
		/*if(engine.fpf>0)*/ renderMove(engine)
	}

	override fun renderLineClear(engine:GameEngine) {
		if(engine.fpf>0) renderMove(engine)
	}

	override fun renderARE(engine:GameEngine) {
		if(engine.fpf>0) renderMove(engine)
	}

	private fun locusParticle(engine:GameEngine, fall:Int, alpha:Int = 225, lifetime:IntRange, vel:Float = alpha*0.0072f) {
		if(particle&&!engine.isRetroSkin) engine.nowPieceObject?.let {p ->
			p.dataX[p.direction].zip(p.dataY[p.direction].withIndex()).groupBy({it.first}, {it.second})
				.mapValues {it.value.filterNot {(i, _) -> p.block[i].getAttribute(Block.ATTRIBUTE.BONE)}}
				.filterNot {it.value.isEmpty()}.forEach {(x2, c) ->
					val (i, y2) = c.minBy {it.value}
					val col = Fireworks.colorBy(getBlockColor(p.block[i]))
					val x = engine.fX+(engine.nowPieceX+x2+.5f)*engine.blockSize
					for(z in 0..fall) {
						val y = engine.fY+(engine.nowPieceY+y2-z+.5f)*engine.blockSize
						efxFG.addAll(
							Fireworks(
								x, y, col[0], col[1], col[2], alpha, 32, -.1f, .95f, lifetime.first, lifetime.last,
								vel, 0..0
							).particles
						)
					}
				}
		}
	}

	override fun afterPieceFall(engine:GameEngine, fpf:Int) {
		locusParticle(engine, fpf, 128, 8..16)
		super.afterPieceFall(engine, fpf)
	}

	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		locusParticle(engine, fall, 160, 16..26)
		super.afterSoftDropFall(engine, fall)
	}

	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		locusParticle(engine, fall, 200, 24..36)
		super.afterHardDropFall(engine, fall)
	}

	override fun pieceFlicked(engine:GameEngine, pX:Int, pY:Int, p:Piece, slide:Boolean) {
		if(particle&&!engine.isRetroSkin) efxFG.addAll(
			p.dataX[p.direction].zip(p.dataY[p.direction].withIndex()).groupBy({it.first}, {it.second})
				.filterNot {it.value.isEmpty()}.filter {(x2, c) ->
					c.maxBy {it.value}.value.let {y2 ->
						!engine.field.getBlockEmpty(pX+x2, pY+y2+1, pY+y2<engine.field.height-1)
					}
				}
				.flatMap {(x2, c) ->
					val (i, y2) = c.maxBy {it.value}
					val col = Fireworks.colorBy(getBlockColor(p.block[i]))
					val px = engine.fX+(pX+x2)*engine.blockSize
					val prx = engine.fX+(pX+x2+1)*engine.blockSize
					val py = engine.fY+(pY+y2+1)*engine.blockSize
					LandingParticles(
						3, px, prx, py, 0f, col[0], col[1], col[2],
						235, 45, .44f, if(slide) .4f else 1f
					).let {it.particles+it}
				}
		)
	}

	override fun pieceLocked(engine:GameEngine, pX:Int, pY:Int, p:Piece, lines:Int, finesse:Boolean) {
		if(particle&&!engine.isRetroSkin) efxFG.addAll(
			p.dataX[p.direction].zip(p.dataY[p.direction].withIndex()).groupBy({it.first}, {it.second})
				.mapValues {it.value.filterNot {(i, _) -> p.block[i].getAttribute(Block.ATTRIBUTE.BONE)}}
				.filterNot {it.value.isEmpty()}.flatMap {(x2, c) ->
					val x = engine.fX+(pX+x2+.5f)*engine.blockSize
					val fw = c.flatMap {(i, y2) ->
						val col = Fireworks.colorBy(getBlockColor(p.block[i]))
						val y = engine.fY+(pY+y2+.5f)*engine.blockSize
						Fireworks(
							x, y, col[0], col[1], col[2], 255, col[if(finesse) 3 else 4], -.05f, .95f,
							27, 45, 4.1f, 2..4
						).let {if(finesse) it.particles+it else listOf(it)}
					}
					val (i, y2) = c.maxBy {it.value}
					val col = Fireworks.colorBy(getBlockColor(p.block[i]))
					val px = engine.fX+(pX+x2)*engine.blockSize
					val prx = engine.fX+(pX+x2+1)*engine.blockSize
					val py = engine.fY+(pY+y2+1)*engine.blockSize
					val lp = LandingParticles(
						3, px, prx, py, 0f, col[0], col[1], col[2],
						235, 45, .44f, .66f
					).particles
					fw+lp
				}
		)
	}

	override fun lineClear(engine:GameEngine, y:Collection<Int>) {
		val s = engine.blockSize
		efxFG.addAll(y.map {Beam(engine.fX, engine.fY+it*s, s*engine.fieldWidth, s, if(particle) .5f else 1f)})
	}

	override fun blockBreak(engine:GameEngine, blk:Map<Int, Map<Int, Block>>) {
		if(showLineEffect&&engine.displaySize!=-1&&engine.blkSkin!=GameEngine.FRAME_SKIN_GB) {
			val s = engine.blockSize
			efxFG.addAll(blk.flatMap {(y, row) ->
				row.flatMap a@{(x, blk) ->
					val sx = engine.fX+x*s
					val sy = engine.fY+y*s
					if(particle&&engine.blkSkin!=GameEngine.FRAME_SKIN_GB) {
						if(blk.getAttribute(Block.ATTRIBUTE.BONE)) Fireworks.colorBy(GREEN).let {col ->
							LandingParticles(
								5, sx, sx+s, sy+s/2f, s*.5f, col[0], col[1], col[2],
								235, 25, .44f, .66f
							).particles
						} else Fireworks.colorBy(getBlockColor(blk)).let {col ->
							Fireworks(
								sx+s/2f, sy+s/2f, col[0], col[1], col[2], 255, 32, .1f, .95f, 25, 45,
								4.5f, 4..10
							).let {it.particles+it}
						}
					} else {
						val color = blk.drawColor
						val r = resources
						listOf(
							if(blk.isGemBlock) // 宝石Block
								FragAnim(ANIM.GEM, sx, sy, (color-Block.COLOR_GEM_RED)%r.pEraseMax, lineEffectSpeed)
							else // 通常Block
								FragAnim(
									if(blk.getAttribute(Block.ATTRIBUTE.LAST_COMMIT)) ANIM.SPARK else ANIM.BLOCK,
									sx, sy, maxOf(0, color-Block.COLOR_WHITE)%r.blockBreakMax, lineEffectSpeed
								)
						)
					}
				}
			})
//			if(engine.skin==)
			val btype =
				engine.blkSkin in GameEngine.FRAME_SKIN_HEBO..GameEngine.FRAME_SKIN_SG||(engine.owner.mode?.gameIntensity?:0)<0
			efxBG.addAll(
				BlockParticle.Mapper(
					engine, this, blk, if(btype) BlockParticle.Type.TGM else BlockParticle.Type.DTET,
					blk.size>=4, if(btype) 4f else 3.2f+blk.size
				).particles
			)
		}
	}

	/* ラインを消す演出の処理 */
	override fun calcScore(engine:GameEngine, event:ScoreEvent?) {
		event?:return
		val w = engine.fieldWidth*engine.blockSize/2
		val sx = engine.fX+w

		val dir = maxOf(-w*2f, minOf(w*2f, (320-sx)*w/128))
		val sy = engine.fY+engine.blockSize/2*
			when {
				event.lines==0 -> engine.nowPieceBottomY*2
				event.split -> engine.field.lastLinesTop*2
				else -> (engine.field.lastLinesTop+engine.field.lastLinesBottom)
			}
		efxFG.add(PopupAward(sx, sy, event, if(event.lines==0) engine.speed.are else engine.speed.lineDelay, dir))
	}

	override fun addScore(x:Int, y:Int, pts:Int, color:COLOR) {
		if(pts!=0) efxFG.add(PopupPoint(x, y, pts, color.ordinal))
	}

	override fun addCombo(x:Int, y:Int, pts:Int, type:PopupCombo.CHAIN, ex:Int) {
		if(pts>0) efxFG.add(PopupCombo(x, y, pts, type, ex))
	}

	override fun shootFireworks(engine:GameEngine, x:Float, y:Float, color:COLOR) {
		if(particle) {
			val col = Fireworks.colorBy(color)
			efxFG.addAll(Fireworks(x, y, col[0], col[1], col[2], 255, 128).let {it.particles+it})
		} else efxFG.add(FragAnim(ANIM.HANABI, x, y, color.ordinal))
		super.shootFireworks(engine, x, y, color)
	}

	override fun bravo(engine:GameEngine) {
		val s = engine.blockSize
		efxFG.add(PopupBravo(engine.fX, engine.fY, s*engine.fieldWidth, s*engine.fieldHeight))
		super.bravo(engine)
	}

	/* EXCELLENT画面の描画処理 */
	override fun renderExcellent(engine:GameEngine) {
		if(!engine.allowTextRenderByReceiver) return
		if(!engine.isVisible) return

		val cY = (engine.fieldHeight-1) // 19 : cY/3f = 6.3
		if(engine.owner.players<=1) {
			drawMenuFont(engine, 0f, cY/3f, "EXCELLENT!", RAINBOW, 1f)
			engine.owner.mode?.name?.let {
				drawMenuNano(engine, -30f, cY/3f+1, it, RAINBOW, .5f)
				drawMenuNano(engine, -30f, cY/3f+1.5f, "MODE COMPLETED", RAINBOW, .5f)
			}
		} else drawMenuFont(engine, -3f, cY/2f, "You WIN!", ORANGE, 1f)
	}

	/* game over画面の描画処理 */
	override fun renderGameOver(engine:GameEngine) {
		if(!engine.allowTextRenderByReceiver||!engine.isVisible) return
		val offsetX = engine.fX
		val offsetY = engine.fY
		if(engine.lives>0&&engine.gameActive) {
			drawDirectFont(offsetX+4, offsetY+156, "LEFT", WHITE, 1f)
			drawDirectFont(offsetX+128, offsetY+148, ((engine.lives-1)%10).toString(), WHITE, 2f)
		} else if(engine.statc[0]>=engine.statc[1])
			when {
				engine.owner.players<2 -> if(engine.ending==0)
					drawMenuFont(engine, -3f, engine.fieldHeight/2f, "GAME OVER", RED, 1f)
				else drawMenuFont(engine, -3f, engine.fieldHeight/2f, "THE END", WHITE, 1f)
				engine.owner.winner==-2 -> drawMenuFont(engine, -3f, engine.fieldHeight/2f, "DRAW", PURPLE, 1f)
				engine.owner.players<3 -> drawMenuFont(engine, -3f, engine.fieldHeight/2f, "You Lost", RED, 1f)
			}
	}

	/* Render results screen処理 */
	override fun renderResult(engine:GameEngine) {
		if(!engine.allowTextRenderByReceiver) return
		if(!engine.isVisible) return

		drawMenuFont(
			engine, .75f, 0f+engine.fieldHeight, "RETRY",
			if(engine.statc[0]==0) RAINBOW else WHITE, 1f
		)
		drawMenuFont(
			engine, 6.75f, 0f+engine.fieldHeight, "END",
			if(engine.statc[0]==1) RAINBOW else WHITE, 1f
		)
	}

	/* fieldエディット画面の描画処理 */
	override fun renderFieldEdit(engine:GameEngine) {
		val bs = engine.blockSize
		val x = engine.fX+engine.mapEditX*bs
		val y = engine.fY+engine.mapEditY*bs
		val bright = if(engine.mapEditFrames%6>=3) -.5f else -.2f
		drawBlock(x, y, engine.mapEditColor, engine.blkSkin, darkness = bright)
		val z = (engine.mapEditFrames/4)%8
		resources.imgCursor.draw(
			x-bs/2, y-bs/2, x-bs/2+32, y-bs/2+32,
			32f*(z%4), 32f*(z/4), 32f*(1+(z%4)), 32f*(1+z/4)
		)
	}
	/* 各 frame 最初の描画処理 */
	override fun renderFirst(engine:GameEngine, inside:()->Unit) {
		if(engine.playerID==0)
			drawBG(engine)

		// NEXTなど
		if(!engine.owner.menuOnly&&engine.isVisible) {
			val offsetX = engine.fX
			val offsetY = engine.fY

			drawFrame(offsetX, offsetY, engine, inside)
			engine.statc.forEachIndexed {i, it ->
				printFontSpecific(offsetX-20, offsetY+i*8, "%3d".format(it), FONT.NANO, WHITE, .5f, .5f)
			}
			efxBG.forEachIndexed {i, it -> it.draw(i, this)}
			if(engine.displaySize!=-1) drawNext(offsetX+engine.fieldXOffset, offsetY, engine)
			drawField(offsetX, offsetY, engine, engine.displaySize)

			if(nextShadow&&engine.ghost&&engine.ruleOpt.ghost&&engine.gameActive)
				drawShadowNexts(offsetX, offsetY, engine)

		} else
			efxBG.forEachIndexed {i, it -> it.draw(i, this)}
	}
	/* 各 frame の最後に行われる処理 */
	override fun onLast(engine:GameEngine) {
		if(engine.playerID==engine.owner.players-1) effectUpdate()
	}

	/** Update effects */
	private fun effectUpdate() {
		efxBG.removeAll {it.update(this)}
		efxFG.removeAll {it.update(this)}
	}
	/* 各 frame の最後に行われる描画処理 */
	override fun renderLast(engine:GameEngine) {
		if(engine.playerID==engine.owner.players-1)
			efxFG.forEachIndexed {i, it -> it.draw(i, this)}
	}

	open fun drawBlendAdd(unit:()->Unit) = unit()

	protected abstract fun printFontSpecific(x:Float, y:Float, str:String, font:FONT, color:COLOR, scale:Float,
		alpha:Float)

	protected fun printFontSpecific(x:Number, y:Number, str:String, font:FONT, color:COLOR, scale:Float,
		alpha:Float) =
		printFontSpecific(x.toFloat(), y.toFloat(), str, font, color, scale, alpha)

	protected abstract fun printTTFSpecific(x:Float, y:Float, str:String, color:COLOR, scale:Float, alpha:Float)
	protected fun printTTFSpecific(x:Number, y:Number, str:String, color:COLOR, size:Int, alpha:Float) =
		printTTFSpecific(x.toFloat(), y.toFloat(), str, color, size.toFloat()/BaseFontTTF.FONT_SIZE, alpha)

	/***
	 * @param sx BlockSkin grid-X
	 * @param sy BlockSkin grid-Y
	 * @param sk BlockSkin number
	 */
	protected abstract fun drawBlockSpecific(x:Float, y:Float, sx:Int, sy:Int, sk:Int, size:Float, darkness:Float,
		alpha:Float)

	abstract fun drawLineSpecific(x:Float, y:Float, sx:Float, sy:Float, color:Int = 0xFFFFFF, alpha:Float = 1f, w:Float = 1f)

	fun drawLineSpecific(x:Number, y:Number, sx:Number, sy:Number, color:Int = 0xFFFFFF, alpha:Float = 1f, w:Int = 1) =
		drawLineSpecific(x.toFloat(), y.toFloat(), sx.toFloat(), sy.toFloat(), color, alpha, w.toFloat())

	/** draw and Fill Rectangle */
	fun drawRect(
		x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f,
		outlineW:Float = 0f, outlineColor:Int = 0x000000
	) {
		fillRectSpecific(x-outlineW/2f, y-outlineW/2f, w+outlineW/2f, h+outlineW/2f, color, alpha)
		if(outlineW>0)
			drawRectSpecific(x-outlineW/2f, y-outlineW/2f, w+outlineW/2f, h+outlineW/2f, outlineColor, alpha, outlineW)
	}

	fun drawRect(
		x:Number, y:Number, w:Number, h:Number, color:Int = 0xFFFFFF, alpha:Float = 1f, outlineW:Int = 0,
		outlineColor:Int = 0x000000
	) =
		drawRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha, outlineW.toFloat(), outlineColor)

	/** Draw Rectangle Outline*/
	abstract fun drawRectSpecific(
		x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f, bold:Float = 1f
	)

	fun drawRectSpecific(x:Number, y:Number, w:Number, h:Number, color:Int = 0xFFFFFF, alpha:Float = 1f, bold:Int = 0) =
		drawRectSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha, bold.toFloat())
	/** Fiil Rectangle Solid*/
	abstract fun fillRectSpecific(x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f)

	fun fillRectSpecific(x:Number, y:Number, w:Number, h:Number, color:Int = 0xFFFFFF, alpha:Float = 1f) =
		fillRectSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha)

	/** draw and Fill Rectangle */
	fun drawDia(
		x:Float, y:Float, w:Float, h:Float, angle:Float, color:Int = 0xFFFFFF, alpha:Float = 1f,
		outlineW:Float = 0f, outlineColor:Int = 0x000000
	) {
		fillDiaSpecific(x, y, w, h, angle, color, alpha)
		if(outlineW>0)
			drawDiaSpecific(x, y, w, h, angle, outlineColor, alpha, outlineW)
	}

	fun drawDia(
		x:Number, y:Number, w:Number, h:Number, angle:Float, color:Int = 0xFFFFFF, alpha:Float = 1f, outlineW:Int = 0,
		outlineColor:Int = 0x000000
	) =
		drawDia(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), angle, color, alpha, outlineW.toFloat(), outlineColor)

	/** Draw Rectangle Outline*/
	protected abstract fun drawDiaSpecific(
		x:Float, y:Float, w:Float, h:Float, angle:Float, color:Int = 0xFFFFFF, alpha:Float = 1f, bold:Float = 1f
	)

	private fun drawDiaSpecific(x:Number, y:Number, w:Number, h:Number, angle:Float, color:Int = 0xFFFFFF, alpha:Float = 1f,
		bold:Int = 0) =
		drawDiaSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), angle, color, alpha, bold.toFloat())
	/** Fiil Diaangle Solid*/
	protected abstract fun fillDiaSpecific(x:Float, y:Float, w:Float, h:Float, angle:Float, color:Int = 0xFFFFFF,
		alpha:Float = 1f)

	private fun fillDiaSpecific(x:Number, y:Number, w:Number, h:Number, angle:Float, color:Int = 0xFFFFFF,
		alpha:Float = 1f) =
		fillDiaSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), angle, color, alpha)

	/** draw and Fill Oval */
	fun drawOval(
		x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f,
		outlineW:Float = 0f, outlineColor:Int = 0x000000
	) {
		fillOvalSpecific(x-outlineW/2f, y-outlineW/2f, w+outlineW/2f, h+outlineW/2f, color, alpha)
		if(outlineW>0)
			drawOvalSpecific(x-outlineW/2f, y-outlineW/2f, w+outlineW/2f, h+outlineW/2f, outlineColor, alpha, outlineW)
	}

	fun drawOval(
		x:Number, y:Number, w:Number, h:Number, color:Int = 0xFFFFFF, alpha:Float = 1f, outlineW:Int = 0,
		outlineColor:Int = 0x000000
	) =
		drawOval(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha, outlineW.toFloat(), outlineColor)

	/** Draw Oval Outline*/
	protected abstract fun drawOvalSpecific(
		x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f,
		bold:Float = 1f
	)

	private fun drawOvalSpecific(x:Number, y:Number, w:Number, h:Number, color:Int = 0xFFFFFF, alpha:Float = 1f,
		bold:Int = 0) =
		drawOvalSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha, bold.toFloat())
	/** Fill Oval Solid*/
	protected abstract fun fillOvalSpecific(x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f)

	private fun fillOvalSpecific(x:Number, y:Number, w:Number, h:Number, color:Int = 0xFFFFFF, alpha:Float = 1f) =
		fillOvalSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha)

	/**
	 * draw outline Piece
	 */
	protected open fun drawPieceOutline(x:Float, y:Float, piece:Piece, blksize:Float, alpha:Float = 1f,
		whiteLine:Boolean = false) {
		piece.let {
			val bigmul = blksize*(1+it.big.toInt())
			it.block.forEachIndexed {i, blk ->
				val x2 = x+(it.dataX[it.direction][i]*bigmul).toInt()
				val y2 = y+(it.dataY[it.direction][i]*bigmul).toInt()
				drawBlockOutline(x2, y2, blk, bigmul.toInt(), alpha, whiteLine)
			}
		}
	}

	protected open fun drawBlockOutline(x:Float, y:Float, blk:Block, blksize:Int, alpha:Float = 1f,
		whiteLine:Boolean = false) {
		blk.let {
			val x3 = (x)
			val y3 = (y)
			val ls = blksize-1

			val color =
				getColorByID(if(it.getAttribute(Block.ATTRIBUTE.BONE)) Block.COLOR.WHITE else it.color?:Block.COLOR.WHITE)
			val lC = if(whiteLine) 0xFFFFFF else color
			fillRectSpecific(x3, y3, blksize.toFloat(), blksize.toFloat(), color, alpha*.5f)

			if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_UP)) {
				drawLineSpecific(x3, y3, (x3+ls), y3, lC, alpha*.5f)
				drawLineSpecific(x3, (y3+1), (x3+ls), (y3+1), lC, alpha*.5f)
			}
			if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) {
				drawLineSpecific(x3, (y3+ls), (x3+ls), (y3+ls), lC, alpha*.5f)
				drawLineSpecific(x3, (y3-1+ls), (x3+ls), (y3-1+ls), lC, alpha*.5f)
			}
			if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) {
				drawLineSpecific(x3, y3, x3, (y3+ls), lC, alpha*.5f)
				drawLineSpecific((x3+1), y3, (x3+1), (y3+ls), lC, alpha*.5f)
			}
			if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) {
				drawLineSpecific((x3+ls), y3, (x3+ls), (y3+ls), lC, alpha*.5f)
				drawLineSpecific((x3-1+ls), y3, (x3-1+ls), (y3+ls), lC, alpha*.5f)
			}
			if(it.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_UP))
				fillRectSpecific(x3, y3, 2f, 2f, lC, alpha*.5f)
			if(it.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_DOWN))
				fillRectSpecific(x3, (y3+blksize-2), 2f, 2f, lC, alpha*.5f)
			if(it.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_UP))
				fillRectSpecific(x3+blksize-2, y3, 2f, 2f, lC, alpha*.5f)
			if(it.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_DOWN))
				fillRectSpecific(x3+blksize-2, y3+blksize-2, 2f, 2f, lC, alpha*.5f)
		}
	}

	protected open fun drawBadgesSpecific(x:Float, y:Float, type:Int, scale:Float) {
		val b = FontBadge(type)
		resources.imgBadges.draw(
			x, y, x+b.w*scale, y+b.h*scale,
			b.sx.toFloat(), b.sy.toFloat(), (b.sx+b.w).toFloat(), (b.sy+b.h).toFloat()
		)
	}

	protected open fun drawFieldSpecific(
		x:Float, y:Float, width:Int, viewHeight:Int, blksize:Int, scale:Float, outlineType:Int
	) {
	}

	override fun drawSpeedMeter(x:Float, y:Float, sp:Float, len:Float) {
		val s = if(sp<0) 1f else sp
		val w = maxOf(1f, len-1)*BS
		drawRectSpecific((x-1), (y-1), w+1, 3f, 0)
		fillRectSpecific(
			x, y, w, 2f, getColorByID(
				when {
					s<0.25f -> CYAN
					s<0.5f -> GREEN
					s<0.75f -> YELLOW
					s<1f -> ORANGE
					else -> RED
				}
			)
		)
		if(s<.25f) fillRectSpecific(x, y, s*w*4, 2f, 0xFF00)
		if(s<.5f) fillRectSpecific(x, y, s*w*2, 2f, 0xFFFF00)
		if(s<0.75f) fillRectSpecific(x, y, s*w/.75f, 2f, 0xFF8000)

		if(s<1f) fillRectSpecific(x, y, s*w, 2f, 0xFF0000)
		else fillRectSpecific(x, y, minOf(1f, s-1)*w, 2f, 0xFFFFFF)
	}

	fun drawBlackBG(alpha:Float = 1f) = fillRectSpecific(0, 0, 640, 480, 0, alpha)

	/** Block colorIDに応じてColor Hexを作成
	 * @param color Block colorID
	 * @return color Hex
	 */
	protected fun getColorByID(color:COLOR?):Int = when(color) {
		WHITE -> 0xFFFFFF
		RED -> 0xFF0000
		ORANGE -> 0xFF8000
		YELLOW -> 0xFFFF00
		GREEN -> 0x00FF00
		CYAN -> 0x00FFFF
		BLUE -> 0x0040FF
		COBALT -> 0x4000FF
		PURPLE -> 0xAA00FF
		PINK -> 0xFF00AA
		RAINBOW -> getColorByID(getRainbowColor(rainbowCount))
		null -> 0
	}
	/** Block colorIDに応じてColor Hexを作成
	 * @param color Block colorID
	 * @return color Hex
	 */
	protected fun getColorByID(color:Block.COLOR?):Int = getColorByID(getBlockColor(color))

	companion object {
		fun getMeterColorHex(meterColor:Int, value:Float):Int =
			when(meterColor) {
				//			var r = meterColor/65536
				//			var g = meterColor/256%256
				//			var b = meterColor%256
				GameEngine.METER_COLOR_LEVEL ->
					(0xFF*(value*3-1).coerceIn(0f, 1f)).toInt()*0x10000+
						(0xFF*(3-value*3f).coerceIn(0f, 1f)).toInt()*0x100+
						(0xFF*(1-value*3).coerceIn(0f, 1f)).toInt()
				GameEngine.METER_COLOR_LIMIT -> //red<yellow<green<cyan
					(0xFF*(2-value*3).coerceIn(0f, 1f)).toInt()*0x10000+
						(0xFF*(value*3).coerceIn(0f, 1f)).toInt()*0x100+
						(0xFF*(value*3f-2f).coerceIn(0f, 1f)).toInt()
				else -> meterColor
			}
	}
}
