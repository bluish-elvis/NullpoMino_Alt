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

package mu.nu.nullpo.tool.airankstool

import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JComponent

class SurfaceComponent(private val maxJump:Int, private val stackWidth:Int, surface:Int):JComponent() {
	private val surfaceDecoded = IntArray(stackWidth-1)
	private var minHeight = 0
	private var maxHeight = 0
	private val componentHeight:Int
	private var baseSizeX = 0
	private var baseSizeY = 0

	init {
		baseSizeX = 10
		baseSizeY = 10
		componentHeight = maxJump*(stackWidth-1)
		preferredSize = preferredSize
		setSurface(surface)
	}

	override fun getPreferredSize():Dimension =
		Dimension(baseSizeX*(stackWidth+1)+2*baseSizeX, componentHeight*baseSizeY+2*baseSizeY)

	fun setSurface(surface:Int) {
		var height = 0
		minHeight = 0
		maxHeight = 0
		var workSurface = surface
		for(i in 0..<stackWidth-1) {
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
		for(x in 0..<stackWidth-1) {
			g.drawLine(posX, posY, posX+baseSizeX, posY)
			posX += baseSizeX
			g.drawLine(posX, posY, posX, posY-surfaceDecoded[x]*baseSizeY)
			posY -= surfaceDecoded[x]*baseSizeY
		}
		g.drawLine(posX, posY, posX+baseSizeX, posY)
	}

}
