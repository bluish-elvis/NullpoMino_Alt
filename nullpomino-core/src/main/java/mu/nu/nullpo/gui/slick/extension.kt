package mu.nu.nullpo.gui.slick

import org.newdawn.slick.*



internal fun Graphics.drawRect(x1:Int, y1:Int, width:Int, height:Int) =
	this.drawRect(x1.toFloat(), y1.toFloat(), width.toFloat(), height.toFloat())

internal fun Graphics.fillRect(x1:Int, y1:Int, width:Int, height:Int) =
	this.fillRect(x1.toFloat(), y1.toFloat(), width.toFloat(), height.toFloat())

internal fun Graphics.drawLine(x1:Int, y1:Int, x2:Int, y2:Int) =
	this.drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())

internal fun Graphics.drawImage(image:Image, x:Int, y:Int, x2:Int, y2:Int, srcx:Int, srcy:Int, srcx2:Int, srcy2:Int) =
	this.drawImage(image, x.toFloat(), y.toFloat(), x2.toFloat(), y2.toFloat(), srcx.toFloat(), srcy.toFloat(), srcx2.toFloat(), srcy2.toFloat())

internal fun Graphics.drawImage(image:Image, x:Int, y:Int, x2:Int, y2:Int, srcx:Int, srcy:Int, srcx2:Int, srcy2:Int,
	filter:Color) =
	this.drawImage(image, x.toFloat(), y.toFloat(), x2.toFloat(), y2.toFloat(), srcx.toFloat(), srcy.toFloat(), srcx2.toFloat(), srcy2.toFloat(), filter)

fun Image.draw(x:Int, y:Int, x2:Int, y2:Int, srcx:Int, srcy:Int, srcx2:Int, srcy2:Int) =
	this.draw(x.toFloat(), y.toFloat(), x2.toFloat(), y2.toFloat(), srcx.toFloat(), srcy.toFloat(), srcx2.toFloat(), srcy2.toFloat())
