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

package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.gui.common.ResourceImage
import mu.nu.nullpo.gui.common.bg.AbstractBG
import org.newdawn.slick.Color
import org.newdawn.slick.Image
import org.newdawn.slick.SlickException
import java.io.IOException

class ResourceImageSlick(override val name:String, private val antiAlias:Boolean = false):ResourceImage<Image> {

	constructor(it:ResourceImage<*>, antiAlias:Boolean = false):this(it.name, antiAlias)
	constructor(it:AbstractBG<*>, antiAlias:Boolean = false):this(it.img, antiAlias)
	constructor(i:Image, antiAlias:Boolean = false):this(i.name ?: "", antiAlias) {
		res = i
	}

	constructor(it:ResourceImageSlick):this(it.name) {
		res = it.copy()
	}

	override var res = Image(1, 1)

	override val width get() = res.width
	override val height get() = res.height
	val textureWidth get() = res.textureWidth
	val textureHeight get() = res.textureHeight

	fun copy():Image = res.copy()
	override fun load() {
		if(name.isEmpty()) return
		res = try {
			ResourceHolder.log.debug("load image from $name.png")
			Image("${ResourceHolder.skinDir}/graphics/$name.png").apply {
				filter = if(antiAlias) Image.FILTER_LINEAR else Image.FILTER_NEAREST
			}
		} catch(e:Exception) {
			if(e !is UnsupportedOperationException&&(e is IOException||e is SlickException))
				ResourceHolder.log.error("Failed to load image from $name", e)
			res
		}
	}

	override fun draw(x:Float, y:Float, x2:Float, y2:Float, srcX:Float, srcY:Float, srcX2:Float, srcY2:Float,
		alpha:Float, color:Triple<Float, Float, Float>) =
		res.draw(x, y, x2, y2, srcX, srcY, srcX2, srcY2, Color(color.first, color.second, color.third, alpha))

	/*
		fun draw(x:Float, y:Float) = res.draw(x, y)
		fun draw(x:Float, y:Float, filter:Color) = res.draw(x, y, filter)
		fun draw(x:Float, y:Float, width:Float, height:Float) = res.draw(x, y, width, height)
		fun draw(x:Float, y:Float, width:Float, height:Float, filter:Color?) = res.draw(x, y, width, height, filter)
	*/

	override fun toString():String = name

}
