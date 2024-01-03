/*
Copyright (c) 2018-2024, NullNoname
Kotlin converted and modified by Venom=Nhelv
All rights reserved.

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

package edu.cuhk.cse.fyp.tetrisai.lspi

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Container
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Image
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.event.KeyListener
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelListener
import java.awt.geom.Arc2D
import java.awt.geom.Ellipse2D
import java.awt.geom.GeneralPath
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sqrt

class TLabel(w:Int, h:Int) {
	var draw:JLabel
	var width = SIZE
	private var height = SIZE
	// boundary of drawing canvas, 0% border
	var border = 0.00
	var xMin = 0.0
	var yMin = 0.0
	var xMax = 0.0
	var yMax = 0.0
	// default font
	private val defaultFont = Font("Serif", Font.PLAIN, 16)
	// double buffered graphics
	private val offscreenImage:BufferedImage
	private val onscreenImage:BufferedImage
	protected var offscreen:Graphics2D
	protected var onscreen:Graphics2D
	// change the user coordinate system
	fun setXscale() {
		setXscale(DEFAULT_XMIN, DEFAULT_XMAX)
	}

	fun setYscale() {
		setYscale(DEFAULT_YMIN, DEFAULT_YMAX)
	}

	fun setXscale(min:Double, max:Double) {
		val size = max-min
		xMin = min-border*size
		xMax = max+border*size
	}

	fun setYscale(min:Double, max:Double) {
		val size = max-min
		yMin = min-border*size
		yMax = max+border*size
	}
	// helper functions that scale from user coordinates to screen coordinates and back
	protected fun scaleX(x:Double):Double = width*(x-xMin)/(xMax-xMin)

	protected fun scaleY(y:Double):Double = height*(yMax-y)/(yMax-yMin)

	fun factorX(w:Double):Double = w*width/abs(xMax-xMin)

	fun factorY(h:Double):Double = h*height/abs(yMax-yMin)

	fun userX(x:Double):Double = xMin+x*(xMax-xMin)/width

	fun userY(y:Double):Double = yMax-y*(yMax-yMin)/height
	//create a frame, insert self in frame, then show self
	fun showInFrame() {
		val j = JFrame()
		j.title = "Configuration"
		j.contentPane = draw
		j.isVisible = true
		j.pack()
		show()
	}
	// clear the screen with given color
	@JvmOverloads fun clear(color:Color? = DEFAULT_CLEAR_COLOR) {
		offscreen.color = color
		offscreen.fillRect(0, 0, width, height)
		offscreen.color = penColor
	}

	fun clear(x1:Double, x2:Double, y1:Double, y2:Double) {
		clear(DEFAULT_CLEAR_COLOR, x1, x2, y1, y2)
	}

	fun clear(color:Color?, x1:Double, x2:Double, y1:Double, y2:Double) {
		val ix1:Int = scaleX(x1).toInt()
		val ix2:Int = scaleX(x2).toInt()
		val iy1:Int = scaleY(y1).toInt()
		val iy2:Int = scaleY(y2).toInt()
		offscreen.color = color
		offscreen.fillRect(ix1, iy1, ix2, iy2)
		offscreen.color = penColor
		//show();
	}
	// set the pen size
	fun setPenRadius() {
		setPenRadius(DEFAULT_PEN_RADIUS)
	}

	fun setPenRadius(r:Double) {
		penRadius = r*SIZE
		val stroke = BasicStroke(penRadius.toFloat())
		offscreen.stroke = stroke
	}
	// set the pen color
	fun setPenColor() {
		setPenColor(DEFAULT_PEN_COLOR)
	}

	fun setPenColor(color:Color?) {
		penColor = color
		offscreen.color = penColor
	}
	// write the given string in the current font
	fun setFont() {
		setFont(defaultFont)
	}

	fun setFont(f:Font) {
		val toolkit = Toolkit.getDefaultToolkit()
		val x = toolkit.screenSize.getWidth()
		val y = toolkit.screenSize.getHeight()
		val xscale = x/1400.0
		val yscale = y/1050.0
		val scale = sqrt((xscale*xscale+yscale*yscale)/2)
		font = f.deriveFont((f.size*scale).toFloat())
	}

	init {
		width = w
		height = h
		offscreenImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
		onscreenImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
		offscreen = offscreenImage.createGraphics()
		onscreen = onscreenImage.createGraphics()
		setXscale()
		setYscale()
		offscreen.color = DEFAULT_CLEAR_COLOR
		offscreen.fillRect(0, 0, width, height)
		setPenColor()
		setPenRadius()
		setFont()
		clear()

		// add antialiasing
		val hints = RenderingHints(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON
		)
		hints[RenderingHints.KEY_RENDERING] = RenderingHints.VALUE_RENDER_QUALITY
		offscreen.addRenderingHints(hints)

		// frame stuff
		val icon = ImageIcon(onscreenImage)
		draw = JLabel(icon)
	}

	fun add(frame:Container, spot:String?) {
		frame.add(draw, spot)
	}

	fun addML(frame:MouseListener?) {
		draw.addMouseListener(frame)
	}

	fun addMML(frame:MouseMotionListener?) {
		draw.addMouseMotionListener(frame)
	}

	fun addKL(frame:KeyListener?) {
		draw.addKeyListener(frame)
	}

	fun addMWL(frame:MouseWheelListener?) {
		draw.addMouseWheelListener(frame)
	}

	fun remML(frame:MouseListener?) {
		draw.removeMouseListener(frame)
	}

	fun remMML(frame:MouseMotionListener?) {
		draw.removeMouseMotionListener(frame)
	}

	fun remKL(frame:KeyListener?) {
		draw.removeKeyListener(frame)
	}

	fun remMWL(frame:MouseWheelListener?) {
		draw.removeMouseWheelListener(frame)
	}
	// draw a line from (x0, y0) to (x1, y1)
	fun line(x0:Double, y0:Double, x1:Double, y1:Double) {
//		System.out.println("drawing a line from " + new Point(x0, y0).toString()+ " to " + new Point(x1,y1).toString());
		offscreen.draw(Line2D.Double(scaleX(x0), scaleY(y0), scaleX(x1), scaleY(y1)))
	}
	// draw one pixel at (x, y)
	fun pixel(x:Double, y:Double) {
		offscreen.fillRect(scaleX(x).roundToLong().toInt(), scaleY(y).roundToLong().toInt(), 1, 1)
	}
	// draw one pixel at (x, y)
	fun pixelP(x:Double, y:Double) {
		offscreen.fillRect(x.roundToLong().toInt(), y.roundToLong().toInt(), 1, 1)
	}
	// draw one pixel at (x, y)
	fun pixelP(x:Double, y:Double, c:Color?) {
		setPenColor(c)
		offscreen.fillRect(x.roundToLong().toInt(), y.roundToLong().toInt(), 1, 1)
	}
	// draw point at (x, y)
	fun point(x:Double, y:Double) {
		val xs = scaleX(x)
		val ys = scaleY(y)
		val r = penRadius
		// double WS = factorX(2*r);
		// double hs = factorY(2*r);
		// if (WS <= 1 && hs <= 1) pixel(x, y);
		if(r<=1) pixel(x, y) else offscreen.fill(Ellipse2D.Double(xs-r/2, ys-r/2, r, r))
	}

	fun arc(x:Double, y:Double, r:Double, startAngle:Double, arcRange:Double) {
		val xs = scaleX(x)
		val ys = scaleY(y)
		val ws = factorX(2*r)
		val hs = factorY(2*r)
		if(ws<=1&&hs<=1) pixel(x, y) else offscreen.draw(Arc2D.Double(xs-ws/2, ys-hs/2, ws, hs, startAngle, arcRange, Arc2D.OPEN))
	}
	// draw circle of radius r, centered on (x, y); degenerate to pixel if small
	fun circle(x:Double, y:Double, r:Double) {
		val xs = scaleX(x)
		val ys = scaleY(y)
		val ws = factorX(2*r)
		val hs = factorY(2*r)
		if(ws<=1&&hs<=1) pixel(x, y) else offscreen.draw(Ellipse2D.Double(xs-ws/2, ys-hs/2, ws, hs))
	}

	fun circleP(x:Double, y:Double, r:Double, col:Color?) {
		setPenColor(col)
		circleP(x, y, r)
	}

	fun circleP(x:Double, y:Double, r:Double) {
		val ws = 2*r
		val hs = 2*r
		val xs = scaleX(x)
		val ys = scaleY(y)
		if(ws<=1&&hs<=1) pixel(x, y) else offscreen.draw(Ellipse2D.Double(xs-ws/2, ys-hs/2, ws, hs))
	}

	fun circle(x:Double, y:Double, r:Double, color:Color?) {
		setPenColor(color)
		circle(x, y, r)
	}
	// draw filled circle of radius r, centered on (x, y); degenerate to pixel if small
	fun filledCircle(x:Double, y:Double, r:Double) {
		val xs = scaleX(x)
		val ys = scaleY(y)
		val ws = factorX(2*r)
		val hs = factorY(2*r)
		if(ws<=1&&hs<=1) pixel(x, y) else offscreen.fill(Ellipse2D.Double(xs-ws/2, ys-hs/2, ws, hs))
	}

	fun filledCircleP(x:Double, y:Double, r:Double) {
		val ws = 2*r
		val hs = 2*r
		if(ws<=1&&hs<=1) pixel(x, y) else offscreen.fill(Ellipse2D.Double(x-ws/2, y-hs/2, ws, hs))
	}
	// draw squared of side length 2r, centered on (x, y); degenerate to pixel if small
	fun square(x:Double, y:Double, r:Double) {
		// screen coordinates
		val xs = scaleX(x)
		val ys = scaleY(y)
		val ws = factorX(2*r)
		val hs = factorY(2*r)
		if(ws<=1&&hs<=1) pixel(x, y) else offscreen.draw(Rectangle2D.Double(xs-ws/2, ys-hs/2, ws, hs))
	}
	// draw squared of side length 2r, centered on (x, y); degenerate to pixel if small
	fun filledSquare(x:Double, y:Double, r:Double) {
		// screen coordinates
		val xs = scaleX(x)
		val ys = scaleY(y)
		val ws = factorX(2*r)
		val hs = factorY(2*r)
		if(ws<=1&&hs<=1) pixel(x, y) else offscreen.fill(Rectangle2D.Double(xs-ws/2, ys-hs/2, ws, hs))
	}
	//Draw an arrow of the appropriate scale, color, position
	fun arrow(x:Double, y:Double, w:Double, h:Double, scale:Double, color:Color?) {
		rectangle(x, y, w, h, color)
		val xArray = doubleArrayOf(x+w/2-w/10+scale*1/10*sqrt(h*h*25+w*w)*sqrt(3.0)/3, x+w/2-w/10, x+w/2-w/10)
		val yArray =
			doubleArrayOf(y, y+scale*1/10*sqrt(h*h*25+w*w)*sqrt(2.0)/2, y-scale*1/10*sqrt(h*h*25+w*w)*sqrt(2.0)/2)
		setPenColor(color)
		filledPolygon(xArray, yArray)
		setPenColor()
	}
	// draw a polygon with the given (x[i], y[i]) coordinates
	fun polygon(x:DoubleArray, y:DoubleArray) {
		val n = x.size
		val path = GeneralPath()
		path.moveTo(scaleX(x[0]).toFloat(), scaleY(y[0]).toFloat())
		for(i in 0..<n) path.lineTo(scaleX(x[i]).toFloat(), scaleY(y[i]).toFloat())
		path.closePath()
		offscreen.draw(path)
	}
	//	draw a polygon with the given (x[i], y[i]) coordinates
	fun polygonP(x:DoubleArray, y:DoubleArray) {
		val n = x.size
		val path = GeneralPath()
		path.moveTo(x[0].toFloat(), y[0].toFloat())
		for(i in 0..<n) path.lineTo(x[i].toFloat(), y[i].toFloat())
		path.closePath()
		offscreen.draw(path)
	}
	// draw a filled polygon with the given (x[i], y[i]) coordinates
	fun filledPolygon(x:DoubleArray, y:DoubleArray) {
		val n = x.size
		val path = GeneralPath()
		path.moveTo(scaleX(x[0]).toFloat(), scaleY(y[0]).toFloat())
		for(i in 0..<n) path.lineTo(scaleX(x[i]).toFloat(), scaleY(y[i]).toFloat())
		path.closePath()
		offscreen.fill(path)
	}
	//	draw a filled polygon with the given (x[i], y[i]) coordinates
	fun filledPolygonP(x:DoubleArray, y:DoubleArray) {
		val n = x.size
		val path = GeneralPath()
		path.moveTo(x[0].toFloat(), y[0].toFloat())
		for(i in 0..<n) path.lineTo(x[i].toFloat(), y[i].toFloat())
		path.closePath()
		offscreen.fill(path)
	}
	//Draw rectangle at the given coordinates
	fun rectangle(x:Double, y:Double, w:Double, h:Double) {
		val xArray = doubleArrayOf(x-w/2, x-w/2, x+w/2, x+w/2)
		val yArray = doubleArrayOf(y-h/2, y+h/2, y+h/2, y-h/2)
		polygon(xArray, yArray)
	}

	fun rectangleLL(x:Double, y:Double, w:Double, h:Double) {
		val xArray = doubleArrayOf(x, x, x+w, x+w)
		val yArray = doubleArrayOf(y, y+h, y+h, y)
		polygon(xArray, yArray)
	}

	fun rectangleP(x:Double, y:Double, w:Double, h:Double) {
		val xArray = doubleArrayOf(x, x, x+w, x+w)
		val yArray = doubleArrayOf(y, y+h, y+h, y)
		polygonP(xArray, yArray)
	}

	fun rectangle(x:Double, y:Double, w:Double, h:Double, c:Color?) {
		val xArray = doubleArrayOf(x-w/2, x-w/2, x+w/2, x+w/2)
		val yArray = doubleArrayOf(y-h/2, y+h/2, y+h/2, y-h/2)
		setPenColor(c)
		filledPolygon(xArray, yArray)
		setPenColor(DEFAULT_PEN_COLOR)
	}

	fun rectangleC(x:Double, y:Double, w:Double, h:Double, c:Color?) {
		val xArray = doubleArrayOf(x, x, x+w, x+w)
		val yArray = doubleArrayOf(y, y+h, y+h, y)
		setPenColor(c)
		filledPolygon(xArray, yArray)
		setPenColor(DEFAULT_PEN_COLOR)
	}

	fun filledRectangleP(x:Double, y:Double, w:Double, h:Double, c:Color?) {
		val xArray = doubleArrayOf(x, x, x+w, x+w)
		val yArray = doubleArrayOf(y, y+h, y+h, y)
		setPenColor(c)
		filledPolygonP(xArray, yArray)
		setPenColor(DEFAULT_PEN_COLOR)
	}

	fun filledRectangleLL(x:Double, y:Double, w:Double, h:Double, c:Color?) {
		val xArray = doubleArrayOf(x, x, x+w, x+w)
		val yArray = doubleArrayOf(y, y+h, y+h, y)
		setPenColor(c)
		filledPolygon(xArray, yArray)
		setPenColor(DEFAULT_PEN_COLOR)
	}

	fun rectangle(x:Double, y:Double, w:Double, h:Double, c:Color?, border:Boolean, borderColor:Color?) {
		val xArray = doubleArrayOf(x-w/2, x-w/2, x+w/2, x+w/2)
		val yArray = doubleArrayOf(y-h/2, y+h/2, y+h/2, y-h/2)
		if(c!=null) {
			setPenColor(c)
			filledPolygon(xArray, yArray)
		}
		setPenColor(borderColor)
		if(border) polygon(xArray, yArray)
		setPenColor()
	}
	// draw picture (gif, jpg, or png) upperLeft on (x, y), rescaled to w-by-h
	fun image(x:Double, y:Double, image:Image?, w:Double, h:Double) {
		val xs = scaleX(x)
		val ys = scaleY(y)
		val ws = factorX(w)
		val hs = factorY(h)
		if(ws<=1&&hs<=1) pixel(x, y) else {
			offscreen.drawImage(
				image, xs.roundToLong().toInt(), ys.roundToLong().toInt(), ws.roundToLong().toInt(), hs.roundToLong().toInt(), null
			)
		}
	}
	// draw picture (gif, jpg, or png) upperLeft on (x, y), rescaled to w-by-h
	fun imageP(x:Double, y:Double, image:Image?) {
		//if (WS <= 1 && hs <= 1) pixel(x, y);
		offscreen.drawImage(image, x.roundToLong().toInt(), y.roundToLong().toInt(), null)
	}
	//Invert an image
	fun invert(image:Image):BufferedImage {
		val b1 = BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB)
		val bg = b1.graphics
		bg.drawImage(image, 0, 0, null)
		bg.dispose()
		val b2 = BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB)
		val db1 = b1.raster.dataBuffer
		val db2 = b2.raster.dataBuffer
		run {
			var i = db1.size-1
			var j = 0
			while(i>=0) {
				db2.setElem(j, db1.getElem(i))
				--i
				j++
			}
		}
		var i = db1.size-1
		var j = 0
		while(i>=0) {
			db1.setElem(i, db1.getElem(i))
			--i
			j++
		}
		return b2
	}
	// write the given text string in the current font, center on (x, y)
	fun text(x:Double, y:Double, s:String?) {
		offscreen.font = font
		val metrics = offscreen.fontMetrics
		val xs = scaleX(x)
		val ys = scaleY(y)
		val ws = metrics.stringWidth(s)
		val hs = metrics.descent
		offscreen.drawString(s, (xs-ws/2.0).toFloat(), (ys+hs).toFloat())
	}

	fun textTop(x:Double, y:Double, s:String?) {
		offscreen.font = font
		val metrics = offscreen.fontMetrics
		val xs = scaleX(x)
		val ys = scaleY(y)
		val ws = metrics.stringWidth(s)
		val hs = metrics.descent
		offscreen.drawString(s, (xs-ws/2.0).toFloat(), (ys+hs*3).toFloat())
	}

	fun textLeft(x:Double, y:Double, s:String?, c:Color?) {
		setPenColor(c)
		offscreen.font = font
		val metrics = offscreen.fontMetrics
		val xs = scaleX(x)
		val ys = scaleY(y)
		//int WS = metrics.stringWidth(s);
		val hs = metrics.descent
		offscreen.drawString(s, xs.toFloat(), (ys+hs).toFloat())
		setPenColor()
	}
	//Draw text at the appropriate point and color
	fun text(x:Double, y:Double, s:String?, c:Color?) {
		setPenColor(c)
		offscreen.font = font
		val metrics = offscreen.fontMetrics
		val xs = scaleX(x)
		val ys = scaleY(y)
		val ws = metrics.stringWidth(s)
		val hs = metrics.descent
		offscreen.drawString(s, (xs-ws/2.0).toFloat(), (ys+hs).toFloat())
		setPenColor()
	}
	//	write the given text string in the current font, center on (x, y) sized to w, h
	fun text(x:Double, y:Double, s:String?, w:Double, h:Double) {
		offscreen.font = font
		//FontMetrics metrics = offscreen.getFontMetrics();
		val xs = scaleX(x)
		val ys = scaleY(y)
		val ws = factorX(w)
		val hs = factorY(h)
		offscreen.drawString(s, (xs-ws/2.0).toFloat(), (ys+hs).toFloat())
	}

	fun absText(s:String?, x:Int, y:Int) {
		offscreen.drawString(s, x.toFloat(), y.toFloat())
	}

	fun textinvert(x:Double, y:Double, s:String?) {
		offscreen.font = font
		val metrics = offscreen.fontMetrics
		val xs = scaleX(x)
		val ys = scaleY(y)
		val ws = metrics.stringWidth(s)
		val hs = metrics.descent
		val bimage = BufferedImage(ws, hs, BufferedImage.TYPE_INT_ARGB)
		val bimagegraphics = bimage.createGraphics()
		bimagegraphics.drawString(s, (xs-ws/2.0).toFloat(), (ys+hs).toFloat())
		val bimage2 = invert(bimage)
		offscreen.drawImage(bimage2, (xs-ws/2.0).roundToLong().toInt(), (ys+hs).roundToLong().toInt(), null)
	}
	// view on-screen, creating new frame if necessary
	fun show() {
		onscreen.drawImage(offscreenImage, 0, 0, null)
		try {
			draw.repaint()
			//frame.paint(frame.getGraphics());
		} catch(e:NullPointerException) {
			println("Null Pointer Exception in showatonce")
		}
	}

	companion object {
		// pre-defined colors
		val BLACK:Color = Color.BLACK
		val BLUE:Color = Color.BLUE
		val CYAN:Color = Color.CYAN
		val DARK_GRAY:Color = Color.DARK_GRAY
		val GRAY:Color = Color.GRAY
		val GREEN:Color = Color.GREEN
		val LIGHT_GRAY:Color = Color.LIGHT_GRAY
		val MAGENTA:Color = Color.MAGENTA
		val ORANGE:Color = Color.ORANGE
		val PINK:Color = Color.PINK
		val RED:Color = Color.RED
		val WHITE:Color = Color.WHITE
		val YELLOW:Color = Color.YELLOW
		val NICEGREEN = Color(0, 153, 0)
		// default colors
		val DEFAULT_PEN_COLOR:Color = BLACK
		val DEFAULT_CLEAR_COLOR:Color = WHITE
		// current pen color
		private var penColor:Color? = null
		// default canvas size is SIZE-by-SIZE
		const val SIZE = 512
		// default pen radius
		private const val DEFAULT_PEN_RADIUS = 0.002
		// current pen radius
		private var penRadius = 0.0
		private const val DEFAULT_XMIN = 0.0
		private const val DEFAULT_XMAX = 1.0
		private const val DEFAULT_YMIN = 0.0
		private const val DEFAULT_YMAX = 1.0
		// current font
		private var font:Font? = null
	}
}
