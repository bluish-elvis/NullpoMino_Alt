package mu.nu.nullpo.gui.common

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine

abstract class AbstractRenderer:EventReceiver() {

	internal var resources:ResourceHolder? = null

	override fun drawMenu(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, font:FONT, color:COLOR, scale:Float) {
		var x2 = if(scale==.5f) x*8 else x*16
		var y2 = if(scale==.5f) y*8 else y*16
		if(!engine.owner.menuOnly) {
			x2 += getFieldDisplayPositionX(engine, playerID)+4
			y2 += if(engine.displaysize==-1) getFieldDisplayPositionY(engine, playerID)+4
			else getFieldDisplayPositionY(engine, playerID)+52
		}
		when(font) {
			FONT.TTF -> printTTFSpecific(x2, y2, str, color)
			else -> printFontSpecific(x2, y2, str, color, scale)
		}
	}

	fun drawTTFMenuFont(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR) {
		var x2 = x*16
		var y2 = y*16
		if(!engine.owner.menuOnly) {
			x2 += getFieldDisplayPositionX(engine, playerID)+4
			y2 += if(engine.displaysize==-1)
				getFieldDisplayPositionY(engine, playerID)+4
			else getFieldDisplayPositionY(engine, playerID)+52
		}
		printTTFSpecific(x2, y2, str, color)
	}

	override fun drawScore(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, font:FONT, color:COLOR, scale:Float) {
		if(engine.owner.menuOnly) return
		val size = if(scale==.5f) 8 else 16
		when(font) {
			FONT.TTF -> printTTFSpecific(
				getScoreDisplayPositionX(engine, playerID)+x*16, getScoreDisplayPositionY(engine, playerID)+y*16, str, color)
			else -> printFontSpecific(
				getScoreDisplayPositionX(engine, playerID)+x*size, getScoreDisplayPositionY(engine, playerID)+y*size, str, color, scale)
		}

	}

	fun drawTTFScoreFont(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR) {
		if(engine.owner.menuOnly) return
		printTTFSpecific(
			getScoreDisplayPositionX(engine, playerID)+x*16, getScoreDisplayPositionY(engine, playerID)+y*16, str, color)
	}

	override fun drawDirect(x:Int, y:Int, str:String, font:FONT, color:COLOR, scale:Float) {
		printFontSpecific(x, y, str, color, scale)
	}

	override fun drawDirectTTF(x:Int, y:Int, str:String, color:COLOR) {
		printTTFSpecific(x, y, str, color)
	}

	@JvmOverloads
	protected fun drawBlock(x:Int, y:Int, color:Int, skin:Int, bone:Boolean, darkness:Float, alpha:Float, scale:Float, attr:Int = 0) {
		var skin = skin

		if(!doesGraphicsExist()) return

		if(color<=Block.BLOCK_COLOR_INVALID) return
		if(skin>=resources!!.imgBlockListSize) skin = 0

		val isSpecialBlocks = color>=Block.BLOCK_COLOR_COUNT
		val isSticky = resources!!.getBlockIsSticky(skin)

		val size = (16*scale).toInt()
		val img:AbstractImage
		img = when {
			scale<=.5f -> resources!!.getImgSmallBlock(skin)
			scale>=2f -> resources!!.getImgBigBlock(skin)
			else -> resources!!.getImgNormalBlock(skin)
		}

		var sx = color*size
		if(bone) sx += 9*size
		var sy = 0
		if(isSpecialBlocks) sx = (color-Block.BLOCK_COLOR_COUNT+18)*size

		if(isSticky)
			if(isSpecialBlocks) {
				sx = (color-Block.BLOCK_COLOR_COUNT)*size
				sy = 18*size
			} else {
				sx = 0
				if(attr and Block.BLOCK_ATTRIBUTE_CONNECT_UP!=0) sx = sx or 0x1
				if(attr and Block.BLOCK_ATTRIBUTE_CONNECT_DOWN!=0) sx = sx or 0x2
				if(attr and Block.BLOCK_ATTRIBUTE_CONNECT_LEFT!=0) sx = sx or 0x4
				if(attr and Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT!=0) sx = sx or 0x8
				sx *= size
				sy = color*size
				if(bone) sy += 9*size
			}

		val imageWidth = img.width
		if(sx>=imageWidth&&imageWidth!=-1) sx = 0
		val imageHeight = img.height
		if(sy>=imageHeight&&imageHeight!=-1) sy = 0

		drawBlockSpecific(x, y, sx, sy, darkness, alpha, img)
	}

	/** BlockUsing an instance of the classBlockDraw a
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk BlockInstance of a class
	 */
	protected fun drawBlock(x:Int, y:Int, blk:Block) {
		drawBlock(x, y, blk.drawColor, blk.skin, blk.getAttribute(Block.ATTRIBUTE.BONE), blk.darkness, blk.alpha, 1f, blk.aint)
	}

	/** BlockUsing an instance of the classBlockDraw a
	 * (You can specify the magnification)
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk BlockInstance of a class
	 * @param scale Enlargement factor
	 */
	protected fun drawBlock(x:Int, y:Int, blk:Block, scale:Float) {
		drawBlock(x, y, blk.drawColor, blk.skin, blk.getAttribute(Block.ATTRIBUTE.BONE), blk.darkness, blk.alpha, scale, blk.aint)
	}

	/** BlockUsing an instance of the classBlockDraw a
	 * (You can specify the magnification and dark)
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk BlockInstance of a class
	 * @param scale Enlargement factor
	 * @param darkness Lightness or darkness
	 */
	protected fun drawBlock(x:Int, y:Int, blk:Block, scale:Float, darkness:Float) {
		drawBlock(x, y, blk.drawColor, blk.skin, blk.getAttribute(Block.ATTRIBUTE.BONE), darkness, blk.alpha, scale, blk.aint)
	}

	protected fun drawBlockForceVisible(x:Int, y:Int, blk:Block, scale:Float) {
		drawBlock(x, y, blk.drawColor, blk.skin, blk.getAttribute(Block.ATTRIBUTE.BONE), blk.darkness,
			.5f*blk.alpha+.5f, scale, blk.aint)
	}

	/** BlockDraw a piece (You can specify the brightness or darkness)
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param piece Peace to draw
	 * @param scale Enlargement factor
	 * @param darkness Lightness or darkness
	 */
	@JvmOverloads
	protected fun drawPiece(x:Int, y:Int, piece:Piece, scale:Float = 1f, darkness:Float = 0f) {
		for(i in 0 until piece.maxBlock) {
			val x2 = x+(piece.dataX[piece.direction][i].toFloat()*16f*scale).toInt()
			val y2 = y+(piece.dataY[piece.direction][i].toFloat()*16f*scale).toInt()

			val blkTemp = Block(piece.block[i])
			blkTemp.darkness = darkness

			drawBlock(x2, y2, blkTemp, scale)
		}
	}

	/** Currently working onBlockDraw a piece
	 * (Y-coordinateThe0MoreBlockDisplay only)
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineInstance of
	 * @param scale Display magnification
	 */
	protected fun drawCurrentPiece(x:Int, y:Int, engine:GameEngine, scale:Float) {
		val piece = engine.nowPieceObject
		val blksize = (16*scale).toInt()

		piece?.let {
			for(i in 0 until it.maxBlock)
				if(!it.big) {
					val x2 = engine.nowPieceX+it.dataX[it.direction][i]
					val y2 = engine.nowPieceY+it.dataY[it.direction][i]

					if(y2>=0) {
						var blkTemp = it.block[i]
						if(engine.nowPieceColorOverride>=0) {
							blkTemp = Block(it.block[i])
							blkTemp.cint = engine.nowPieceColorOverride
						}
						drawBlock(x+x2*blksize, y+y2*blksize, blkTemp, scale)
					}
				} else {
					val x2 = engine.nowPieceX+it.dataX[it.direction][i]*2
					val y2 = engine.nowPieceY+it.dataY[it.direction][i]*2

					var blkTemp = it.block[i]
					if(engine.nowPieceColorOverride>=0) {
						blkTemp = Block(it.block[i])
						blkTemp.cint = engine.nowPieceColorOverride
					}
					drawBlock(x+x2*blksize, y+y2*blksize, blkTemp, scale*2f)
				}
		}
	}

	/** fieldOfBlockDraw a
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineInstance of
	 */
	protected fun drawField(x:Int, y:Int, engine:GameEngine, size:Int) {
		if(!doesGraphicsExist()) return

		var blksize = 16
		var scale = 1f
		if(size==-1) {
			blksize = 8
			scale = .5f
		} else if(size==1) {
			blksize = 32
			scale = 2f
		}

		val field = engine.field
		var width = 10
		var height = 20
		var viewHeight = 20
		var outlineType = 0

		field?.let {
			width = it.width
			height = it.height
			viewHeight = height
		}
		if(engine.heboHiddenEnable&&engine.gameActive) viewHeight -= engine.heboHiddenYNow

		outlineType = if(engine.owBlockOutlineType==-1) engine.blockOutlineType else engine.owBlockOutlineType

		drawFieldSpecific(x, y, width, viewHeight, blksize, scale, outlineType)

		// BunglerHIDDEN
		field?.let {
			if(engine.heboHiddenEnable&&engine.gameActive) {
				var maxY = engine.heboHiddenYNow
				if(maxY>height) maxY = height
				for(i in 0 until maxY)
					for(j in 0 until width)
						drawBlock(x+j*blksize, y+(height-1-i)*blksize, Block.BLOCK_COLOR_GRAY, 0, false, 0.0f, 1f, scale)
			}
		}
	}

	/** Currently working onBlockOf Peaceghost Draw a
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineInstance of
	 * @param scale Display magnification
	 */
	protected abstract fun drawGhostPiece(x:Int, y:Int, engine:GameEngine, scale:Float)

	protected abstract fun drawHintPiece(x:Int, y:Int, engine:GameEngine, scale:Float)

	/** Draw shadow nexts
	 * @param x X coord
	 * @param y Y coord
	 * @param engine GameEngine
	 * @param scale Display size of piece
	 */
	protected fun drawShadowNexts(x:Int, y:Int, engine:GameEngine, scale:Float) {
		val piece = engine.nowPieceObject
		val blksize = (16*scale).toInt()

		if(piece!=null) {
			val shadowX = engine.nowPieceX
			val shadowY = engine.nowPieceBottomY+piece.minimumBlockY

			for(i in 0 until engine.ruleopt.nextDisplay-1) {
				if(i>=3) break

				engine.getNextObject(engine.nextPieceCount+i)?.let {
					val size = if(piece.big||engine.displaysize==1) 2 else 1
					val shadowCenter = blksize*piece.minimumBlockX+blksize*(piece.width+size)/2
					val nextCenter = blksize/2*it.minimumBlockX+blksize/2*(it.width+1)/2
					val vPos = blksize*shadowY-(i+1)*24-8

					if(vPos>=-blksize/2) drawPiece(x+blksize*shadowX+shadowCenter-nextCenter, y+vPos, it, .5f*scale, .1f)
				}
			}
		}
	}

	protected abstract fun printFontSpecific(x:Int, y:Int, str:String, color:COLOR, scale:Float)

	protected abstract fun printTTFSpecific(x:Int, y:Int, str:String, color:COLOR)

	protected abstract fun doesGraphicsExist():Boolean

	protected abstract fun drawBlockSpecific(x:Int, y:Int, sx:Int, sy:Int, darkness:Float, alpha:Float, img:AbstractImage)

	protected abstract fun drawOutlineSpecific(i:Int, j:Int, x:Int, y:Int, blksize:Int, blk:Block, outlineType:Int)

	protected abstract fun drawFieldSpecific(x:Int, y:Int, width:Int, viewHeight:Int, blksize:Int, scale:Float, outlineType:Int)
}