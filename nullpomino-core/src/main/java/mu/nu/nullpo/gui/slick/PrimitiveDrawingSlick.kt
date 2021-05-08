package mu.nu.nullpo.gui.slick

import org.newdawn.slick.Graphics

object PrimitiveDrawingSlick {
	/**
	 * Draws a rectangle using a set of coordinates and its dimensions. No reflection.
	 *
	 * @param graphics Graphics object to draw on
	 * @param x              X-coordinate of the top-left corner
	 * @param y              Y-coordinate of the top-left corner
	 * @param sizeX          X-size of the rectangle
	 * @param sizeY          Y-size of the rectangle
	 * @param red            Red component of color
	 * @param green          Green component of color
	 * @param blue           Blue component of color
	 * @param alpha          Alpha component of color
	 * @param fill           Fill rectangle?
	 */
	fun drawRectangle(graphics:Graphics, x:Int, y:Int, sizeX:Int, sizeY:Int,
		red:Int, green:Int, blue:Int, alpha:Int, fill:Boolean) {
		graphics.color = org.newdawn.slick.Color(red, green, blue, alpha)
		if(fill) graphics.fillRect(x.toFloat(), y.toFloat(), sizeX.toFloat(),
			sizeY.toFloat()) else graphics.drawRect(
			x.toFloat(), y.toFloat(), sizeX.toFloat(), sizeY.toFloat())
		graphics.color = org.newdawn.slick.Color.white
	}
	/**
	 * Draws an arc using a set of coordinates and its dimensions. No reflection.<br></br>
	 * **Warning: SDL cannot use this feature.**
	 *
	 * @param graphics Graphics object to draw on
	 * @param x              X-coordinate of the centre
	 * @param y              Y-coordinate of the centre
	 * @param sizeX          X-size of the arc
	 * @param sizeY          Y-size of the arc
	 * @param angleStart     Start angle of arc in circle (0 degrees = top)
	 * @param angleSize      Total angular size of arc (360 degrees = full turn)
	 * @param red            Red component of color
	 * @param green          Green component of color
	 * @param blue           Blue component of color
	 * @param alpha          Alpha component of color
	 * @param fill           Fill arc?
	 */
	fun drawArc(graphics:Graphics, x:Int, y:Int, sizeX:Int, sizeY:Int, angleStart:Int, angleSize:Int,
		red:Int, green:Int, blue:Int, alpha:Int, fill:Boolean) {
		graphics.color = org.newdawn.slick.Color(red, green, blue, alpha)
		if(fill) graphics.fillArc(x.toFloat(), y.toFloat(), sizeX.toFloat(), sizeY.toFloat(), angleStart.toFloat(),
			angleSize.toFloat()) else graphics.drawArc(x.toFloat(), y.toFloat(), sizeX.toFloat(), sizeY.toFloat(),
			angleStart.toFloat(), angleSize.toFloat())
		graphics.color = org.newdawn.slick.Color.white
	}
	/**
	 * Draws an oval using a set of coordinates and its dimensions. No reflection.<br></br>
	 * **Warning: SDL cannot use this feature.**
	 *
	 * @param graphics Graphics object to draw on
	 * @param x              X-coordinate of the top-left corner
	 * @param y              Y-coordinate of the top-left corner
	 * @param sizeX          X-size of the oval
	 * @param sizeY          Y-size of the oval
	 * @param red            Red component of color
	 * @param green          Green component of color
	 * @param blue           Blue component of color
	 * @param alpha          Alpha component of color
	 * @param fill           Fill oval?
	 */
	fun drawOval(graphics:Graphics, x:Int, y:Int, sizeX:Int, sizeY:Int, red:Int, green:Int, blue:Int, alpha:Int, fill:Boolean) {
		graphics.color = org.newdawn.slick.Color(red, green, blue, alpha)
		if(fill) graphics.fillOval(x.toFloat(), y.toFloat(), sizeX.toFloat(),
			sizeY.toFloat()) else graphics.drawOval(
			x.toFloat(), y.toFloat(), sizeX.toFloat(), sizeY.toFloat())
		graphics.color = org.newdawn.slick.Color.white
	}
}