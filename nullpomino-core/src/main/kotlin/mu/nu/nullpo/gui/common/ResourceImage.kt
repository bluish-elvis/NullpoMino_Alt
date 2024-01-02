/*
 * Copyright (c) 2010-2022, NullNoname
 * Kotlin converted and modified by Venom=Nhelv.
 * THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
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

package mu.nu.nullpo.gui.common

/** 画像オブジェ共通部 */
interface ResourceImage<T:Any?> {
	var res:T
	/** 画像 Filename */
	val name:String

	/** 画像読み込み*/
	fun load()
	fun draw(x:Float, y:Float, x2:Float, y2:Float, srcX:Float, srcY:Float, srcX2:Float, srcY2:Float, alpha:Float = 1f,
		color:Triple<Float, Float, Float> = Triple(1f, 1f, 1f))

	fun draw() = draw(0, 0, width, height, 0, 0, width, height)
	fun draw(x:Float, y:Float, srcx:Float, srcy:Float, srcx2:Float, srcy2:Float) =
		draw(x, y, x+(srcx2-srcx), y+(srcy2-srcy), srcx, srcy, srcx2, srcy2)

	fun draw(x:Float, y:Float, srcx:Float, srcy:Float, srcx2:Float, srcy2:Float, alpha:Float = 1f,
		color:Triple<Float, Float, Float> = Triple(1f, 1f, 1f)) =
		draw(x, y, x+width, y+height, srcx, srcy, srcx2, srcy2, alpha, color)

	fun draw(x:Float, y:Float, x2:Float, y2:Float, alpha:Float = 1f, color:Triple<Float, Float, Float> = Triple(1f, 1f, 1f)) =
		draw(x, y, x2, y2, 0, 0, width, height, alpha, color)

	fun draw(x:Float, y:Float, x2:Float, y2:Float, srcX:Int, srcY:Int, srcX2:Int, srcY2:Int, alpha:Float = 1f,
		color:Triple<Float, Float, Float> = Triple(1f, 1f, 1f)) =
		draw(x, y, x2, y2, srcX.toFloat(), srcY.toFloat(), srcX2.toFloat(), srcY2.toFloat(), alpha, color)

	fun draw(x:Int, y:Int, x2:Int, y2:Int, srcX:Int, srcY:Int, srcX2:Int, srcY2:Int, alpha:Float = 1f) = draw(
		x.toFloat(), y.toFloat(), x2.toFloat(), y2.toFloat(),
		srcX.toFloat(), srcY.toFloat(), srcX2.toFloat(), srcY2.toFloat(), alpha
	)

	val width:Int
	val height:Int

	class ResourceImageStr(override val name:String):ResourceImage<String> {
		override var res:String = "graphics/$name.png"
		override fun draw(x:Float, y:Float, x2:Float, y2:Float, srcX:Float, srcY:Float, srcX2:Float, srcY2:Float, alpha:Float, color:Triple<Float, Float, Float>) {
		}

		override fun load() {}
		override val width = 0
		override val height = 0
		override fun toString() = name
	}

	object ResourceImageBlank:ResourceImage<Nothing?> {
		override var res = null
		/** 画像 Filename */
		override val name = ""

		override fun draw(x:Float, y:Float, x2:Float, y2:Float, srcX:Float, srcY:Float, srcX2:Float, srcY2:Float, alpha:Float, color:Triple<Float, Float, Float>) {
		}

		override fun load() {}
		override val width = 0
		override val height = 0
		override fun toString() = ""
	}

}
