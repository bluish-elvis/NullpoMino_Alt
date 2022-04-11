/*
 * Copyright (c) 2010-2021, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NullNoname nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
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

package mu.nu.nullpo.gui.slick

import org.newdawn.slick.Color
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image

internal fun Graphics.drawRect(x1:Int, y1:Int, width:Int, height:Int) =
	this.drawRect(x1.toFloat(), y1.toFloat(), width.toFloat(), height.toFloat())

internal fun Graphics.fillRect(x1:Int, y1:Int, width:Int, height:Int) =
	this.fillRect(x1.toFloat(), y1.toFloat(), width.toFloat(), height.toFloat())

internal fun Graphics.drawLine(x1:Int, y1:Int, x2:Int, y2:Int) =
	this.drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())

internal fun Graphics.drawImage(image:Image, x:Int, y:Int, x2:Int, y2:Int, srcx:Int, srcy:Int, srcx2:Int, srcy2:Int) =
	this.drawImage(image, x.toFloat(), y.toFloat(), x2.toFloat(), y2.toFloat(), srcx.toFloat(), srcy.toFloat(), srcx2.toFloat(),
		srcy2.toFloat())

internal fun Graphics.drawImage(image:Image, x:Int, y:Int, x2:Int, y2:Int, srcx:Int, srcy:Int, srcx2:Int, srcy2:Int,
	filter:Color) =
	this.drawImage(image, x.toFloat(), y.toFloat(), x2.toFloat(), y2.toFloat(), srcx.toFloat(), srcy.toFloat(), srcx2.toFloat(),
		srcy2.toFloat(), filter)

internal fun Graphics.drawImage(img:ResourceImageSlick, x:Float, y:Float, filter:Color = Color.white) =
	drawImage(img.res, x, y, filter)

internal fun Graphics.drawImage(img:ResourceImageSlick, x:Float, y:Float, x2:Float, y2:Float,
	srcX:Float, srcY:Float, srcX2:Float, srcY2:Float, filter:Color = Color.white) =
	drawImage(img.res, x, y, x2, y2, srcX, srcY, srcX2, srcY2, filter)

internal fun Graphics.drawImage(img:ResourceImageSlick, x:Int, y:Int, filter:Color = Color.white) =
	drawImage(img.res, x.toFloat(), y.toFloat(), filter)

internal fun Graphics.drawImage(img:ResourceImageSlick, x:Float, y:Float, x2:Float, y2:Float,
	srcX:Int, srcY:Int, srcX2:Int, srcY2:Int, filter:Color = Color.white) =
	drawImage(img.res, x, y, x2, y2, srcX.toFloat(), srcY.toFloat(), srcX2.toFloat(), srcY2.toFloat(), filter)

internal fun Graphics.drawImage(img:ResourceImageSlick, x:Int, y:Int, x2:Int, y2:Int,
	srcX:Int, srcY:Int, srcX2:Int, srcY2:Int, filter:Color = Color.white) =
	drawImage(img.res, x, y, x2, y2, srcX, srcY, srcX2, srcY2, filter)