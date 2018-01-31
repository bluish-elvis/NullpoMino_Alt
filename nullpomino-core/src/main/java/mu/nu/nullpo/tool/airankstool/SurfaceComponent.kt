package mu.nu.nullpo.tool.airankstool

import javax.swing.*
import java.awt.*

class SurfaceComponent(private val maxJump:Int, private val stackWidth:Int, surface:Int):JComponent() {

	private val surfaceDecoded:IntArray = IntArray(stackWidth-1)
	private var minHeight:Int = 0
	private var maxHeight:Int = 0
	private val componentHeight:Int
	private var baseSizeX:Int = 0
	private var baseSizeY:Int = 0

	init {

		baseSizeX = 10
		baseSizeY = 10
		componentHeight = maxJump*(stackWidth-1)
		preferredSize = preferredSize
		setSurface(surface)

	}

	override fun getPreferredSize():Dimension = Dimension(baseSizeX*(stackWidth+1)+2*baseSizeX, componentHeight*baseSizeY+2*baseSizeY)

	fun setSurface(surface:Int) {
		var height = 0
		minHeight = 0
		maxHeight = 0
		var workSurface = surface
		for(i in 0 until stackWidth-1) {
			surfaceDecoded[i] = workSurface%(2*maxJump+1)-maxJump
			height += surfaceDecoded[i]
			if(height>maxHeight) maxHeight = height
			if(height<minHeight) minHeight = height
			workSurface /= 2*maxJump+1
		}
		repaint()
	}

	override fun paintComponent(g:Graphics) {
		super.paintComponent(g)
		val bounds = g.clipBounds
		g.color = Color.BLACK
		baseSizeX = bounds.width/(stackWidth+3)
		baseSizeY = bounds.height/(componentHeight+2)
		g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)
		g.color = Color.WHITE
		var posX = bounds.x+baseSizeX
		var posY = bounds.y+baseSizeY+maxHeight*baseSizeY
		for(x in 0 until stackWidth-1) {
			g.drawLine(posX, posY, posX+baseSizeX, posY)
			posX += baseSizeX
			g.drawLine(posX, posY, posX, posY-surfaceDecoded[x]*baseSizeY)
			posY -= surfaceDecoded[x]*baseSizeY
		}
		g.drawLine(posX, posY, posX+baseSizeX, posY)

	}

	companion object {

		private const val serialVersionUID = 1L
	}

}
