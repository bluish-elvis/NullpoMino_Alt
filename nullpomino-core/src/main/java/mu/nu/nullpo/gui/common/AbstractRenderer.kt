package mu.nu.nullpo.gui.common

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.FragAnim.ANIM
import mu.nu.nullpo.gui.common.PopupCombo.CHAIN
import java.util.ArrayList

abstract class AbstractRenderer:EventReceiver() {

	internal open var resources:ResourceHolder? = null

	/** 演出オブジェクト */
	protected val effectlist:ArrayList<EffectObject> = ArrayList(10*4)

	/** Line clearエフェクト表示 */
	protected var showlineeffect:Boolean = false

	/** 重い演出を使う */
	protected var heavyeffect:Boolean = false

	/** fieldBackgroundの明るさ */
	protected var fieldbgbright:Float = .5f

	/** Show field BG grid */
	protected var showfieldbggrid:Boolean = false

	/** NEXT欄を暗くする */
	protected var darknextarea:Boolean = false

	/** ghost ピースの上にNEXT表示 */
	protected var nextshadow:Boolean = false

	/** Line clear effect speed */
	protected var lineeffectspeed:Int = 1

	/** 回転軸を表示する */
	protected var showCenter:Boolean = false

	/** 操作ブロック降下を滑らかにする */
	protected var smoothfall:Boolean = false

	/** 高速落下時の軌道を表示する */
	protected var showLocus:Boolean = false

	override fun drawFont(x:Int, y:Int, str:String, font:FONT, color:COLOR, scale:Float, alpha:Float) {
		if(font==FONT.TTF) printTTFSpecific(x, y, str, color, alpha)
		else printFontSpecific(x, y, str, font, color, scale, alpha)
	}

	var rainbow = 0

	/** Draw a block
	 * @param x X pos
	 * @param y Y pos
	 * @param color Color
	 * @param skin Skin
	 * @param bone true to use bone block ([][][][])
	 * @param darkness Darkness or brightness
	 * @param alpha Alpha
	 * @param scale Size (.5f, 1f, 2f)
	 * @param attr Attribute
	 */
	/* 1マスBlockを描画 */
	override fun drawBlock(x:Float, y:Float, color:Int, skin:Int, bone:Boolean, darkness:Float, alpha:Float, scale:Float,
		attr:Int, outline:Float) {
		var sk = skin

		if(!doesGraphicsExist()) return

		if(color<=Block.BLOCK_COLOR_INVALID) return
		if(sk>=resources?.imgBlockListSize ?: 0) sk = 0

		val isSpecialBlocks = color>=Block.BLOCK_COLOR_COUNT
		val isSticky = resources?.getBlockIsSticky(sk) ?: false

		var sx = color
		if(bone) sx += 9
		var sy = 0
		if(isSpecialBlocks) sx = (color-Block.BLOCK_COLOR_COUNT+18)

		if(isSticky)
			if(isSpecialBlocks) {
				sx = (color-Block.BLOCK_COLOR_COUNT)
				sy = 18
			} else {
				sx = 0
				if(attr and Block.ATTRIBUTE.CONNECT_UP.bit!=0) sx = sx or 0x1
				if(attr and Block.ATTRIBUTE.CONNECT_DOWN.bit!=0) sx = sx or 0x2
				if(attr and Block.ATTRIBUTE.CONNECT_LEFT.bit!=0) sx = sx or 0x4
				if(attr and Block.ATTRIBUTE.CONNECT_RIGHT.bit!=0) sx = sx or 0x8
				sy = color
				if(bone) sy += 9
			}
		val ls = BS*scale
		drawBlockSpecific(x, y, sx, sy, sk, ls, darkness, alpha)
		if(outline>0) {
			if(attr and Block.ATTRIBUTE.CONNECT_UP.bit==0)
				drawLineSpecific(x, y, x+ls, y, w = outline)
			if(attr and Block.ATTRIBUTE.CONNECT_DOWN.bit==0)
				drawLineSpecific(x, y+ls, x+ls, y+ls, w = outline)
			if(attr and Block.ATTRIBUTE.CONNECT_LEFT.bit==0)
				drawLineSpecific(x, y, x, y+ls, w = outline)
			if(attr and Block.ATTRIBUTE.CONNECT_RIGHT.bit==0)
				drawLineSpecific(x+ls, y, x+ls, y+ls, w = outline)
		}
	}

	/* 勲章を描画 */
	override fun drawBadges(x:Int, y:Int, width:Int, nums:Int, scale:Float) {
		var n = nums
		var nx = x.toFloat()
		var ny = y.toFloat()
		var z:Int
		val b = FontBadge.b
		var mh = 0
		while(n>0) {
			z = 0
			while(z<b.size-1&&n>=b[z+1]) z++
			val w = FontBadge(z).w*scale
			val h = FontBadge(z).h
			if(nx+w>width) {
				nx = x.toFloat()
				ny += mh*scale
				mh = 0
			}
			if(h>mh) mh = h
			drawBadgesSpecific(nx, ny, z, scale)
			n -= b[z]
			nx += w
		}
	}

	class FontBadge(type:Int) {
		val type = maxOf(0, minOf(b.size-1, type))
		val sx = intArrayOf(0, 10, 20, 30, 0, 10, 20, 30, 0, 20, 40, 0)[type]
		val sy = intArrayOf(0, 0, 0, 0, 14, 14, 14, 14, 24, 24, 0, 44)[type]
		val w = intArrayOf(10, 10, 10, 10, 10, 10, 10, 10, 20, 20, 32, 64)[type]
		val h = intArrayOf(10, 10, 14, 14, 14, 14, 15, 15, 15, 15, 32, 48)[type]

		companion object {
			val b = intArrayOf(1, 5, 10, 50, 100, 500, 1000, 5000, 10000, 50000, 100000, 500000)
		}
	}

	/** Currently working onBlockDraw a piece
	 * (Y-coordinateThe0MoreBlockDisplay only)
	 * @param x X-coordinate of base field
	 * @param y Y-coordinate of base field
	 * @param engine GameEngineInstance of
	 * @param scale Display magnification
	 */
	protected fun drawCurrentPiece(x:Int, y:Int, engine:GameEngine, scale:Float) {
		val blksize = (getBlockSize(engine)*scale).toInt()
		val bx = engine.nowPieceX
		val by = engine.nowPieceY
		var g = engine.fpf

		val isRetro = engine.framecolor in GameEngine.FRAME_SKIN_SG..GameEngine.FRAME_SKIN_GB
		val ys = if(!smoothfall||by>=engine.nowPieceBottomY||isRetro) 0 else engine.gcount*blksize/engine.speed.denominator%blksize
		//if(engine.harddropFall>0)g+=engine.harddropFall;
		if(!showLocus||isRetro) g = 0

		engine.nowPieceObject?.let {
			for(z in 0..g) {
				//if(g>0)
				var i = 0
				var x2:Int
				var y2:Int
				while(i<it.maxBlock) {
					x2 = it.dataX[it.direction][i]
					y2 = it.dataY[it.direction][i]
					while(i<it.maxBlock-1) {
						if(x2!=it.dataX[it.direction][i+1]) break
						i++
						if(y2>it.dataY[it.direction][i]) y2 = it.dataY[it.direction][i]
					}
					val b = it.block[i]
					drawBlock(x+((x2+bx).toFloat()*16f*scale), y+((y2+by-z).toFloat()*16f*scale), b,
						scale*if(engine.big) 2 else 1, -.1f, .4f)
					i++
				}
				if(z==0) drawPiece(x+bx*blksize, y+by*blksize+ys, it, scale*if(engine.big) 2 else 1,-.25f,
					ow = if(engine.statc[0]%2==0) 2f else 0f)
			}
		}
	}

	/** fieldのBlockを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 */
	protected fun drawField(x:Int, y:Int, engine:GameEngine, size:Int) {
		if(!doesGraphicsExist()) return

		var blksize = getBlockSize(engine)
		var scale = 1f
		if(size==-1) {
			blksize /= 2
			scale /= 2
		} else if(size==1) {
			blksize *= 2
			scale *= 2
		}

		val field = engine.field ?: return
		val width = field.width
		val height = field.height
		var viewHeight = field.height

		if(engine.heboHiddenEnable&&engine.gameActive) viewHeight -= engine.heboHiddenYNow

		val outlineType = if(engine.owBlockOutlineType==-1) engine.blockOutlineType else engine.owBlockOutlineType

		for(i in -field.hiddenHeight until viewHeight)
			for(j in 0 until width) {
				val x2 = (x+j*blksize).toFloat()
				val y2 = (y+i*blksize).toFloat()

				field.getBlock(j, i)?.also {blk ->
					if(blk.color!=null) {
						if(blk.getAttribute(Block.ATTRIBUTE.WALL))
							drawBlock(x2, y2, 0, blk.skin, blk.getAttribute(Block.ATTRIBUTE.BONE), blk.darkness, blk.alpha, scale, blk.aint)
						else if(engine.owner.replayMode&&engine.owner.replayShowInvisible)
							drawBlockForceVisible(x2, y2, blk, scale)
						else if(blk.getAttribute(Block.ATTRIBUTE.VISIBLE)) drawBlock(x2, y2, blk, scale)

						if(blk.getAttribute(Block.ATTRIBUTE.OUTLINE)&&!blk.getAttribute(Block.ATTRIBUTE.BONE)) {
							val ls = blksize-1
							when(outlineType) {
								GameEngine.BLOCK_OUTLINE_NORMAL -> {
									if(field.getBlockColor(j, i-1)==Block.BLOCK_COLOR_NONE) drawLineSpecific(x2, y2, (x2+ls), y2)
									if(field.getBlockColor(j, i+1)==Block.BLOCK_COLOR_NONE) drawLineSpecific(x2, (y2+ls), (x2+ls), (y2+ls))
									if(field.getBlockColor(j-1, i)==Block.BLOCK_COLOR_NONE) drawLineSpecific(x2, y2, x2, (y2+ls))
									if(field.getBlockColor(j+1, i)==Block.BLOCK_COLOR_NONE) drawLineSpecific((x2+ls), y2, (x2+ls), (y2+ls))
								}
								GameEngine.BLOCK_OUTLINE_CONNECT -> {
									if(!blk.getAttribute(Block.ATTRIBUTE.CONNECT_UP)) drawLineSpecific(x2, y2, (x2+ls), y2)
									if(!blk.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) drawLineSpecific(x2, (y2+ls), (x2+ls), (y2+ls))
									if(!blk.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) drawLineSpecific(x2, y2, x2, (y2+ls))
									if(!blk.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) drawLineSpecific((x2+ls), y2, (x2+ls), (y2+ls))
								}
								GameEngine.BLOCK_OUTLINE_SAMECOLOR -> {
									val color = getColorByID(blk.color ?: Block.COLOR.WHITE)
									if(field.getBlockColor(j, i-1)!=blk.cint) drawLineSpecific(x2, y2, (x2+ls), y2, color)
									if(field.getBlockColor(j, i+1)!=blk.cint) drawLineSpecific(x2, (y2+ls), (x2+ls), (y2+ls), color)
									if(field.getBlockColor(j-1, i)!=blk.cint) drawLineSpecific(x2, y2, x2, (y2+ls), color)
									if(field.getBlockColor(j+1, i)!=blk.cint) drawLineSpecific((x2+ls), y2, (x2+ls), (y2+ls), color)
								}
							}
						}

					}
				}

			}
		drawFieldSpecific(x, y, width, viewHeight, blksize, scale, outlineType)

		// BunglerHIDDEN
		field.let {
			if(engine.heboHiddenEnable&&engine.gameActive) {
				var maxY = engine.heboHiddenYNow
				if(maxY>height) maxY = height
				for(i in 0 until maxY)
					for(j in 0 until width)
						drawBlock(x+j*blksize, y+(height-1-i)*blksize, Block.BLOCK_COLOR_GRAY, 0, false, 0.0f, 1f, scale)
			}
		}
	}

	/** NEXTを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 */
	protected fun drawNext(x:Int, y:Int, engine:GameEngine) {
		val fldWidth = engine.fieldWidth+1
		val fldBlkSize = getBlockSize(engine)
		if(engine.isNextVisible)
			when(nextDisplayType) {
				2 -> if(engine.ruleopt.nextDisplay>=1) {
					val x2 = x+8+fldWidth*fldBlkSize
					//FontNormal.printFont(x2+16,y+40,NullpoMinoSlick.getUIText("InGame_Next"),COLOR_ORANGE,.5f);
					drawFont(x2+16, y+40, "NEXT", FONT.NANO, COLOR.ORANGE)
					for(i in 0 until engine.ruleopt.nextDisplay) {
						engine.getNextObject(engine.nextPieceCount+i)?.let {piece ->
							val centerX = (64-(piece.width+1)*16)/2-piece.minimumBlockX*16
							val centerY = (64-(piece.height+1)*16)/2-piece.minimumBlockY*16
							drawPiece(x2+centerX, y+48+i*64+centerY, piece, 1f)
						}
					}
				}
				1 -> if(engine.ruleopt.nextDisplay>=1) {
					val x2 = x+8+fldWidth*fldBlkSize
					//FontNormal.printFont(x2,y+40,NullpoMinoSlick.getUIText("InGame_Next"),COLOR_ORANGE,.5f);
					drawFont(x2, y+40, "NEXT", FONT.NANO, COLOR.ORANGE)
					for(i in 0 until engine.ruleopt.nextDisplay) {
						engine.getNextObject(engine.nextPieceCount+i)?.let {
							val centerX = (32-(it.width+1)*8)/2-it.minimumBlockX*8
							val centerY = (32-(it.height+1)*8)/2-it.minimumBlockY*8
							drawPiece(x2+centerX, y+48+i*32+centerY, it, .75f)
						}
					}
				}
				else -> {
					// NEXT1
					if(engine.ruleopt.nextDisplay>=1) {
						//FontNormal.printFont(x+60,y,NullpoMinoSlick.getUIText("InGame_Next"),COLOR_ORANGE,.5f);
						drawFont(x+60, y, "NEXT", FONT.NANO, COLOR.ORANGE)
						engine.getNextObject(engine.nextPieceCount)?.let {
							//int x2 = x + 4 + ((-1 + (engine.field.getWidth() - piece.getWidth() + 1) / 2) * 16);
							val x2 = x+4+engine.getSpawnPosX(engine.field, it)*fldBlkSize //Rules with spawn x modified were misaligned.
							val y2 = y+48-(it.maximumBlockY+1)*16
							drawPiece(x2, y2, it)
						}
					}

					// NEXT2・3
					for(i in 0 until minOf(2, engine.ruleopt.nextDisplay-1))
						engine.getNextObject(engine.nextPieceCount+i+1)?.let {
							drawPiece(x+124+i*40, y+48-(it.maximumBlockY+1)*8, it, .5f)
						}

					// NEXT4～
					for(i in 0 until engine.ruleopt.nextDisplay-3) engine.getNextObject(engine.nextPieceCount+i+3)?.let {
						if(showmeter) drawPiece(x+176, y+i*40+88-(it.maximumBlockY+1)*8, it, .5f)
						else drawPiece(x+168, y+i*40+88-(it.maximumBlockY+1)*8, it, .5f)
					}
				}
			}

		if(engine.isHoldVisible) {
			// HOLD
			val holdRemain = engine.ruleopt.holdLimit-engine.holdUsedCount
			val x2 = if(sidenext) x-32 else x+if(nextDisplayType==2) -48 else 0
			val y2 = if(sidenext) y+40 else y+if(nextDisplayType==0) 16 else 0

			if(engine.ruleopt.holdEnable&&(engine.ruleopt.holdLimit<0||holdRemain>0)) {
				var str = "SWAP"
				var tempColor = if(engine.holdDisable) COLOR.WHITE else COLOR.GREEN
				if(engine.ruleopt.holdLimit>=0) {
					str += "\ne$holdRemain"
					if(!engine.holdDisable&&holdRemain>0&&holdRemain<=10)
						tempColor = if(holdRemain<=5) COLOR.RED else COLOR.YELLOW
				}
				drawFont(x2, y2, str, FONT.NANO, tempColor)

				engine.holdPieceObject?.let {
					val dark = if(engine.holdDisable) .3f else 0f
					it.resetOffsetArray()
					it.setDarkness(0f)

					when(nextDisplayType) {
						2 -> {
							val centerX = (64-(it.width+1)*16)/2-it.minimumBlockX*16
							val centerY = (64-(it.height+1)*16)/2-it.minimumBlockY*16
							drawPiece(x-64+centerX, y+48+centerY, it, 1f, dark, ow = 1f)
						}
						1 -> {
							val centerX = (32-(it.width+1)*8)/2-it.minimumBlockX*8
							val centerY = (32-(it.height+1)*8)/2-it.minimumBlockY*8
							drawPiece(x2+centerX, y+48+centerY, it, .5f, dark, ow = 1f)
						}
						else -> drawPiece(x2, y+48-(it.maximumBlockY+1)*8, it, .5f, dark, ow = 1f)
					}
				}
			}
		}
	}

	/** Currently working onBlockOf Peaceghost Draw a
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineInstance of
	 * @param scale Display magnification
	 */
	protected abstract fun drawGhostPiece(x:Float, y:Float, engine:GameEngine, scale:Float)

	protected abstract fun drawHintPiece(x:Int, y:Int, engine:GameEngine, scale:Float)

	/** Draw shadow nexts
	 * @param x X coord
	 * @param y Y coord
	 * @param engine GameEngine
	 * @param scale Display size of piece
	 */
	protected fun drawShadowNexts(x:Int, y:Int, engine:GameEngine, scale:Float) {
		val blksize = (16*scale).toInt()

		engine.nowPieceObject?.let {piece ->
			val shadowX = engine.nowPieceX
			val shadowY = engine.nowPieceBottomY+piece.minimumBlockY

			for(i in 0 until engine.ruleopt.nextDisplay-1) {
				if(i>=3) break

				engine.getNextObject(engine.nextPieceCount+i)?.let {next ->
					val size = if(piece.big||engine.displaysize==1) 2 else 1
					val shadowCenter = blksize*piece.minimumBlockX+blksize*(piece.width+size)/2
					val nextCenter = blksize/2*next.minimumBlockX+blksize/2*(next.width+1)/2
					val vPos = blksize*shadowY-(i+1)*24-8

					if(vPos>=-blksize/2)
						drawPiece(x+blksize*shadowX+shadowCenter-nextCenter, y+vPos, next, .5f*scale, .25f, .75f)
				}
			}
		}
	}

	/* Ready画面の描画処理 */
	override fun renderReady(engine:GameEngine, playerID:Int) {
		if(!engine.allowTextRenderByReceiver) return
		//if(engine.isVisible == false) return;

		if(engine.statc[0]>0) {
			val offsetX = fieldX(engine, playerID)
			val offsetY = fieldY(engine, playerID)

			if(engine.statc[0]>0)
				if(engine.displaysize!=-1) {
					if(engine.statc[0]>=engine.readyStart&&engine.statc[0]<engine.readyEnd)
						drawDirectFont(offsetX+4, offsetY+196, "READY", COLOR.WHITE, 2f)
					else if(engine.statc[0]>=engine.goStart&&engine.statc[0]<engine.goEnd)
						drawDirectFont(offsetX+36, offsetY+196, "GO!", COLOR.WHITE, 2f)
				} else if(engine.statc[0]>=engine.readyStart&&engine.statc[0]<engine.readyEnd)
					drawDirectFont(offsetX+20, offsetY+80, "READY", COLOR.WHITE)
				else if(engine.statc[0]>=engine.goStart&&engine.statc[0]<engine.goEnd)
					drawDirectFont(offsetX+32, offsetY+30, "GO!", COLOR.WHITE)
		}
	}

	/* Blockピース移動時の処理 */
	override fun renderMove(engine:GameEngine, playerID:Int) {
		if(!engine.isVisible) return

		val offsetX = fieldX(engine, playerID)
		val offsetY = fieldY(engine, playerID)

		if(engine.statc[0]>1||engine.ruleopt.moveFirstFrame)
			when(engine.displaysize) {
				1 -> {
					if(nextshadow) drawShadowNexts(offsetX+4, offsetY+52, engine, 2f)
					if(engine.ghost&&engine.ruleopt.ghost) drawGhostPiece(offsetX+4f, offsetY+52f, engine, 2f)
					if(engine.ai!=null&&engine.aiShowHint&&engine.aiHintReady) drawHintPiece(offsetX+4, offsetY+52, engine, 2f)
					drawCurrentPiece(offsetX+4, offsetY+52, engine, 2f)
				}
				0 -> {
					if(nextshadow) drawShadowNexts(offsetX+4, offsetY+52, engine, 1f)
					if(engine.ghost&&engine.ruleopt.ghost) drawGhostPiece(offsetX+4f, offsetY+52f, engine, 1f)
					if(engine.ai!=null&&engine.aiShowHint&&engine.aiHintReady) drawHintPiece(offsetX+4, offsetY+52, engine, 1f)
					drawCurrentPiece(offsetX+4, offsetY+52, engine, 1f)
				}
				else -> {
					if(engine.ghost&&engine.ruleopt.ghost) drawGhostPiece(offsetX+4f, offsetY+4f, engine, .5f)
					if(engine.ai!=null&&engine.aiShowHint&&engine.aiHintReady) drawHintPiece(offsetX+4, offsetY+4, engine, .5f)
					drawCurrentPiece(offsetX+4, offsetY+4, engine, .5f)
				}
			}
	}

	override fun renderLockFlash(engine:GameEngine, playerID:Int) {
		if(engine.fpf>0) renderMove(engine, playerID)
	}

	override fun renderLineClear(engine:GameEngine, playerID:Int) {
		if(engine.fpf>0) renderMove(engine, playerID)
	}

	override fun renderARE(engine:GameEngine, playerID:Int) {
		if(engine.fpf>0) renderMove(engine, playerID)
	}

	override fun lineClear(engine:GameEngine, playerID:Int, y:Int) {
		val s = getBlockSize(engine)
		effectlist.add(BeamH(fieldX(engine, playerID)+4, fieldY(engine, playerID)+52+y*s,
			getBlockSize(engine)*engine.fieldWidth, s))
	}

	/* Blockを消す演出を出すときの処理 */
	override fun blockBreak(engine:GameEngine, x:Int, y:Int, blk:Block) {
		resources ?: return
		if(showlineeffect&&engine.displaysize!=-1) {
			val color = blk.drawColor
			val sx = fieldX(engine)+4+x*getBlockSize(engine)
			val sy = fieldY(engine)+52+y*getBlockSize(engine)
			// 通常Block
			if(color>=Block.BLOCK_COLOR_GRAY&&color<=Block.BLOCK_COLOR_PURPLE
				&&!blk.getAttribute(Block.ATTRIBUTE.BONE))
				effectlist.add(FragAnim(if(blk.getAttribute(Block.ATTRIBUTE.LAST_COMMIT)) ANIM.SPARK else ANIM.BLOCK,
					sx, sy, (color-Block.BLOCK_COLOR_GRAY)%resources!!.BLOCK_BREAK_MAX, lineeffectspeed))
			else if(blk.isGemBlock)
				effectlist.add(
					FragAnim(ANIM.GEM, sx, sy, (color-Block.BLOCK_COLOR_GEM_RED)%resources!!.PERASE_MAX, lineeffectspeed))// 宝石Block
			//blockParticles.addBlock(engine, receiver, playerID, blk, j, i, 10, 90, li>=4, localRandom)
			//blockParticles.addBlock(engine, receiver, playerID, blk, j, i, engine.field.width, cY, li, 120)
		}
	}

	/* ラインを消す演出の処理 */
	override fun calcScore(engine:GameEngine, event:GameEngine.ScoreEvent?) {
		event ?: return
		val w = engine.fieldWidth*getBlockSize(engine)/2
		val sx = fieldX(engine)+4+w

		val sy = fieldY(engine)+52+getBlockSize(engine)/2*
			when {
				event.lines==0 -> engine.nowPieceBottomY*2
				event.split -> (engine.field?.lastLinesTop ?: engine.field?.lastLinesBottom ?: engine.fieldHeight)*2
				else -> ((engine.field?.lastLinesTop ?: 0)+(engine.field?.lastLinesBottom ?: engine.fieldHeight))
			}
		effectlist.add(PopupAward(sx, sy, event, if(event.lines==0) engine.speed.are else engine.speed.lineDelay, w*2))
	}

	override fun addScore(x:Int, y:Int, pts:Int, color:COLOR) {
		effectlist.add(PopupPoint(x, y, pts, color.ordinal))
	}

	override fun addCombo(x:Int, y:Int, pts:Int, type:CHAIN, ex:Int) {
		if(pts>0) effectlist.add(PopupCombo(x, y, pts, type, ex))
	}

	override fun shootFireworks(engine:GameEngine, x:Int, y:Int, color:COLOR) {
		effectlist.add(FragAnim(ANIM.HANABI, x, y, color.ordinal))
		super.shootFireworks(engine, x, y, color)
	}

	override fun bravo(engine:GameEngine) {
		effectlist.add(PopupBravo(fieldX(engine), fieldY(engine)))
		super.bravo(engine)
	}

	/* EXCELLENT画面の描画処理 */
	override fun renderExcellent(engine:GameEngine, playerID:Int) {
		if(!engine.allowTextRenderByReceiver) return
		if(!engine.isVisible) return

		val offsetX = fieldX(engine, playerID)
		val offsetY = fieldY(engine, playerID)

		if(engine.displaysize!=-1) {
			if(engine.owner.players<=1)
				drawDirectFont(offsetX+4, offsetY+204, "EXCELLENT!", COLOR.ORANGE, 1f)
			else drawDirectFont(offsetX+36, offsetY+204, "You WIN!", COLOR.ORANGE, 1f)
		} else if(engine.owner.players<=1)
			drawDirectFont(offsetX+4, offsetY+80, "EXCELLENT!", COLOR.ORANGE, .5f)
		else drawDirectFont(offsetX+20, offsetY+80, "You WIN!", COLOR.ORANGE, .5f)
	}

	/* game over画面の描画処理 */
	override fun renderGameOver(engine:GameEngine, playerID:Int) {
		if(!engine.allowTextRenderByReceiver||!engine.isVisible) return
		val offsetX = fieldX(engine, playerID)
		val offsetY = fieldY(engine, playerID)
		if(engine.lives>0&&engine.gameActive) {
			drawDirectFont(offsetX+4, offsetY+204, "LEFT", COLOR.WHITE, 1f)
			drawDirectFont(offsetX+132, offsetY+196, ((engine.lives-1)%10).toString(), COLOR.WHITE, 2f)
		} else if(engine.statc[0]>=engine.statc[1])
			when {
				engine.displaysize!=-1 ->
					when {
						engine.owner.players<2 -> if(engine.ending==0)
							drawDirectFont(offsetX+12, offsetY+204, "GAME OVER", COLOR.WHITE, 1f)
						else drawDirectFont(offsetX+28, offsetY+204, "THE END", COLOR.WHITE, 1f)
						engine.owner.winner==-2 -> drawDirectFont(offsetX+52, offsetY+204, "DRAW", COLOR.GREEN, 1f)
						engine.owner.players<3 -> drawDirectFont(offsetX+20, offsetY+80, "You Lost", COLOR.WHITE, 1f)
					}
				engine.owner.players<2 -> if(engine.ending==0)
					drawDirectFont(offsetX+4, offsetY+204, "GAME OVER", COLOR.WHITE, 1f)
				else drawDirectFont(offsetX+20, offsetY+204, "THE END", COLOR.WHITE, 1f)
				engine.owner.winner==-2 -> drawDirectFont(offsetX+28, offsetY+80, "DRAW", COLOR.GREEN, .5f)
				engine.owner.players<3 -> drawDirectFont(offsetX+12, offsetY+80, "You Lost", COLOR.WHITE, .5f)
			}
	}

	/* 各 frame の最後に行われる処理 */
	override fun onLast(engine:GameEngine, playerID:Int) {
		if(playerID==engine.owner.players-1) effectUpdate()
	}

	/* 各 frame の最後に行われる描画処理 */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(playerID==engine.owner.players-1) effectRender()
	}

	/* Render results screen処理 */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		if(!engine.allowTextRenderByReceiver) return
		if(!engine.isVisible) return

		var tempColor:COLOR = if(engine.statc[0]==0) COLOR.RED else COLOR.WHITE

		drawDirectFont(fieldX(engine, playerID)+12,
			fieldY(engine, playerID)+340, "RETRY", tempColor, 1f)

		tempColor = if(engine.statc[0]==1) COLOR.RED else COLOR.WHITE
		drawDirectFont(fieldX(engine, playerID)+108,
			fieldY(engine, playerID)+340, "END", tempColor, 1f)
	}

	/* fieldエディット画面の描画処理 */
	override fun renderFieldEdit(engine:GameEngine, playerID:Int) {
		val x = fieldX(engine, playerID)+4f+engine.fldeditX*getBlockSize(engine)
		val y = fieldY(engine, playerID)+52f+engine.fldeditY*getBlockSize(engine)
		val bright = if(engine.fldeditFrames%60>=30) -.5f else -.2f
		drawBlock(x, y, engine.fldeditColor, engine.skin, false, bright, 1f, 1f)
	}

	/** Update effects */
	private fun effectUpdate() {
		effectlist.forEach {
			it.update()
		}
		effectlist.removeIf {it.isExpired}
	}

	fun drawAward(x:Int, y:Int, ev:GameEngine.ScoreEvent, anim:Int) {
		val strPieceName = ev.piece?.id?.let {Piece.Shape.names[it]} ?: ""

		when {
			ev.lines==1 -> drawDirectFont(x-48, y, "SINGLE", color = if(ev.twist==null) COLOR.COBALT else COLOR.BLUE)
			ev.lines==2 -> {
				if(!ev.split)
					drawDirectFont(x-48, y, "DOUBLE", color = if(ev.twist==null) COLOR.BLUE else COLOR.CYAN)
				else drawDirectFont(x-80, y, "SPLIT TWIN", color = COLOR.PURPLE)
			}
			ev.lines==3 -> {
				if(!ev.split)
					drawDirectFont(x-48, y, "TRIPLE", color = COLOR.GREEN)
				else drawDirectFont(x-80, y, "1.2.TRIPLE", color = COLOR.CYAN)
			}
			ev.lines>=4 -> drawDirectFont(x-72, y, "QUADRUPLE", color = getRainbowColor(anim))
		}
		if(ev.twist!=null) when {
			ev.twist.isMini -> {
				drawDirectFont(x-80, y-16, "MINI", color = if(ev.b2b) COLOR.CYAN else COLOR.BLUE)
				ev.piece?.let {drawPiece(x-32, y, it, 0.5f)}
				drawDirectFont(x-16, y, "$strPieceName-TWIST", color = if(ev.b2b) COLOR.PINK else COLOR.PURPLE)
			}
			ev.twist==GameEngine.Twister.IMMOBILE_EZ -> {
				ev.piece?.let {drawPiece(x-16, y, it, 0.5f)}
				drawDirectFont(x-54, y-8, "EZ", color = COLOR.ORANGE)
				drawDirectFont(x+54, y-8, "TRICK", color = COLOR.ORANGE)
			}
			else -> {
				ev.piece?.let {drawPiece(x-64, y, it, 0.5f)}
				drawDirectFont(x-32, y-8, "-TWISTER",
					color = if(ev.lines==3) getRainbowColor(anim) else if(ev.b2b) COLOR.PINK else COLOR.PURPLE)
			}
		}
	}

	fun drawCombo(x:Int, y:Int, pts:Int, type:CHAIN) {
		when(type) {
			CHAIN.B2B -> {
				drawFont(x-18, y-15, "BACK 2 BACK", FONT.NANO, COLOR.RED)
				drawDirectNum(x-18, y, String.format("%2d", pts), color = COLOR.YELLOW, scale = 1.5f)
				drawFont(x-18, y+20, "CHAIN!", FONT.NANO, COLOR.ORANGE, .75f)
			}
			CHAIN.COMBO -> {
				drawDirectNum(x-18, y-0, String.format("%2d", pts), color = COLOR.CYAN, scale = 1.5f)
				drawFont(x+18, y+8, "REN", FONT.NANO, COLOR.BLUE, .5f)
				drawFont(x-18, y+20, "COMBO!", FONT.NANO, color = COLOR.BLUE, scale = .75f)
			}
		}
	}

	protected abstract fun printFontSpecific(x:Int, y:Int, str:String,
		font:FONT, color:COLOR, scale:Float, alpha:Float)

	protected abstract fun printTTFSpecific(x:Int, y:Int, str:String, color:COLOR, alpha:Float)

	protected abstract fun doesGraphicsExist():Boolean

	protected abstract fun drawBlockSpecific(x:Float, y:Float, sx:Int, sy:Int, sk:Int,
		size:Float, darkness:Float, alpha:Float)

	protected abstract fun drawLineSpecific(x:Float, y:Float, sx:Float, sy:Float, color:Int = 0xFFFFFF, alpha:Float = 1f,
		w:Float = 1f)

	protected abstract fun drawOutlineSpecific(i:Int, j:Int, x:Int, y:Int, blksize:Int, blk:Block, outlineType:Int)

	protected abstract fun drawBadgesSpecific(x:Float, y:Float, type:Int, scale:Float)
	protected abstract fun drawFieldSpecific(x:Int, y:Int, width:Int, viewHeight:Int, blksize:Int, scale:Float,
		outlineType:Int)

	protected abstract fun effectRender()

	companion object {

		/** Block colorIDに応じてColor Hexを作成
		 * @param color Block colorID
		 * @return color Hex
		 */
		fun getColorByID(color:Block.COLOR):Int = when(color) {
			Block.COLOR.WHITE -> 0xFFFFFF
			Block.COLOR.RED -> 0xFF0000
			Block.COLOR.ORANGE -> 0xFF8000
			Block.COLOR.YELLOW -> 0xFFFF00
			Block.COLOR.GREEN -> 0x00FF00
			Block.COLOR.CYAN -> 0x00FFFF
			Block.COLOR.BLUE -> 0x0040FF
			Block.COLOR.PURPLE -> 0xAA00FF
			else -> 0
		}

		fun getMeterColorAsColor(meterColor:Int, value:Int, max:Int):Int {
			var r = 0
			var g = 0
			var b = 0
			when(meterColor) {
				GameEngine.METER_COLOR_LEVEL -> {
					r = (maxOf(0f, minOf((value*3f-max)/max, 1f))*255).toInt()
					g = (maxOf(0f, minOf((max-value)*3f/max, 1f))*255).toInt()
					b = (maxOf(0f, minOf((max-value*3f)/max, 1f))*255).toInt()
				}
				GameEngine.METER_COLOR_LIMIT -> {//red<yellow<green<cyan
					r = (maxOf(0f, minOf((max*2f-value*3f)/max, 1f))*255).toInt()
					g = (minOf(value*3f/max, 1f)*255).toInt()
					b = (maxOf(0f, minOf((value*3f-max*2f)/max, 1f))*255).toInt()
				}
				else -> return meterColor
			}
			return r*0xFF0000+g*0xFF00+b*0xFF
		}
	}
}