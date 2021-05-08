package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.slick.img.FontNormal
import org.newdawn.slick.*
import org.newdawn.slick.state.StateBasedGame

/** Dummy class for menus with a scroll bar */
abstract class DummyMenuScrollState:DummyMenuChooseState() {

	/** ID number of file at top of currently displayed section */
	private var minentry:Int = 0

	/** Maximum number of entries to display at a time */
	protected var pageHeight:Int = 0

	/** List of entries */
	protected var list:Array<String> = emptyArray()
	override val numChoice:Int get() = list.size

	protected var emptyError:String = ""

	/** Y-coordinates of dark sections of scroll bar */
	private var pUpMinY:Int = 0
	private var pUpMaxY:Int = 0
	private var pDownMinY:Int = 0
	private var pDownMaxY:Int = 0
	private val sbHeight get() = 16*(pageHeight-1)-(LINE_WIDTH shl 1)

	/* Draw the screen */
	override fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {
		// Background
		g.drawImage(ResourceHolder.imgMenuBG[0], 0f, 0f)

		// Menu
		when {
			list.isEmpty() -> FontNormal.printFontGrid(2, 10, emptyError, COLOR.RED)
			else -> {
				if(cursor>=list.size) cursor = 0
				if(cursor<minentry) minentry = cursor
				var maxentry = minentry+pageHeight-1
				if(cursor>=maxentry) {
					maxentry = cursor
					minentry = maxentry-pageHeight+1
				}
				drawMenuList(g)
				onRenderSuccess(container, game, g)
			}
		}
		super.renderImpl(container, game, g)
	}

	protected open fun onRenderSuccess(container:GameContainer, game:StateBasedGame, graphics:Graphics) {}

	public override fun updateMouseInput(input:Input):Boolean {
		// Mouse
		MouseInput.update(input)
		val clicked = MouseInput.isMouseClicked
		val x = MouseInput.mouseX shr 4
		val y = MouseInput.mouseY shr 4
		if(x>=SB_TEXT_X-1&&(clicked||MouseInput.isMenuRepeatLeft)) {
			var maxentry = minentry+pageHeight-1
			when {
				y<=2&&minentry>0 -> {
					ResourceHolder.soundManager.play("cursor")
					//Scroll up
					minentry--
					maxentry--
				}
				y>=2+pageHeight&&maxentry<list.size -> {
					ResourceHolder.soundManager.play("cursor")
					//Down arrow
					minentry++
				}
				numChoice>pageHeight ->
					maxOf(0, (MouseInput.mouseY-32)*(numChoice+1-pageHeight)/sbHeight).let {
						if(it!=minentry) {
							ResourceHolder.soundManager.play("cursor")
							minentry = it
						}
					}
			}
			if(cursor>=maxentry) cursor = maxentry-1
			if(cursor<minentry) cursor = minentry
		} else if(clicked&&y in 3 until 2+pageHeight) {
			val newCursor = y-3+minentry
			when {
				newCursor==cursor -> return true
				newCursor>=list.size -> return false
				else -> {
					ResourceHolder.soundManager.play("cursor")
					cursor = newCursor
					flashY = newCursor-minentry+minChoiceY
					flashT = 0
				}
			}
		}
		return false
	}

	private fun drawMenuList(graphics:Graphics) {
		val maxentry = minOf(minentry+pageHeight-1, list.size)

		for((y, i) in (minentry until maxentry).withIndex()) {
			FontNormal.printFontGrid(2, 3+y, list[i], cursor==i)
			if(cursor==i) FontNormal.printFontGrid(1, 3+y, "\u0082", COLOR.RAINBOW)
		}

		//Draw scroll bar
		FontNormal.printFontGrid(SB_TEXT_X, 2, "\u008b", SB_TEXT_COLOR)
		FontNormal.printFontGrid(SB_TEXT_X, 2+pageHeight, "\u008e", SB_TEXT_COLOR)
		//Draw shadow
		graphics.color = SB_SHADOW_COLOR
		graphics.fillRect((SB_MIN_X+SB_WIDTH).toFloat(), (SB_MIN_Y+LINE_WIDTH).toFloat(), LINE_WIDTH.toFloat(), sbHeight.toFloat())
		graphics.fillRect((SB_MIN_X+LINE_WIDTH).toFloat(), (SB_MIN_Y+sbHeight).toFloat(), SB_WIDTH.toFloat(), LINE_WIDTH.toFloat())
		//Draw border
		graphics.color = SB_BORDER_COLOR
		graphics.fillRect(SB_MIN_X.toFloat(), SB_MIN_Y.toFloat(), SB_WIDTH.toFloat(), sbHeight.toFloat())
		//Draw inside
		val insideHeight = sbHeight-(LINE_WIDTH shl 1)
		val insideWidth = SB_WIDTH-(LINE_WIDTH shl 1)
		var fillMinY = insideHeight*minentry/list.size
		var fillHeight = ((maxentry-minentry)*insideHeight+list.size)/list.size
		if(fillHeight<LINE_WIDTH) {
			fillHeight = LINE_WIDTH
			fillMinY = (insideHeight-fillHeight)*minentry/(list.size-pageHeight)
		}
		graphics.color = SB_BACK_COLOR
		graphics.fillRect((SB_MIN_X+LINE_WIDTH).toFloat(), (SB_MIN_Y+LINE_WIDTH).toFloat(), insideWidth.toFloat(), insideHeight.toFloat())
		graphics.color = SB_FILL_COLOR
		graphics.fillRect((SB_MIN_X+LINE_WIDTH).toFloat(), (SB_MIN_Y+LINE_WIDTH+fillMinY).toFloat(), insideWidth.toFloat(), fillHeight.toFloat())
		graphics.color = Color.white

		//Update coordinates
		pUpMinY = SB_MIN_Y+LINE_WIDTH
		pUpMaxY = pUpMinY+fillMinY
		pDownMinY = pUpMaxY+fillHeight
		pDownMaxY = SB_MIN_Y+LINE_WIDTH+insideHeight
	}

	override fun onChange(container:GameContainer, game:StateBasedGame, delta:Int, change:Int) {
		ResourceHolder.soundManager.play("cursor")
		if(change==1) pageDown()
		else if(change==-1) pageUp()
		flashY = cursor-minentry
		flashT = 0
	}

	private fun pageDown() {
		val max = numChoice-pageHeight
		if(minentry>=max) cursor = numChoice
		else {
			cursor += pageHeight
			minentry += pageHeight
			if(minentry>max) {
				cursor -= minentry-max
				minentry = max
			}
		}
	}

	private fun pageUp() {
		if(minentry==0) cursor = 0
		else {
			cursor -= pageHeight
			minentry -= pageHeight
			if(minentry<0) {
				cursor -= minentry
				minentry = 0
			}
		}
	}

	companion object {
		/** Scroll bar attributes */
		protected const val SB_TEXT_X = 38
		protected val SB_TEXT_COLOR = COLOR.BLUE
		protected const val SB_MIN_X = SB_TEXT_X shl 4
		protected const val SB_MIN_Y = 49
		protected const val LINE_WIDTH = 2
		protected const val SB_WIDTH = 14

		/** Scroll bar colors */
		protected val SB_SHADOW_COLOR = Color(12, 78, 156)
		protected val SB_BORDER_COLOR = Color(52, 150, 252)
		protected val SB_FILL_COLOR:Color = Color.white
		protected val SB_BACK_COLOR:Color = Color.black
	}
}
