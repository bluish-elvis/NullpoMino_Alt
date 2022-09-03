/*
 * Copyright (c) 2021-2022, NullNoname
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

import mu.nu.nullpo.game.event.EventReceiver.COLOR

interface BaseFont {
	companion object {
		const val SOLID = "\u0080"
		const val ATMARK = "\u0081"
		const val CURSOR = "\u0082"
		const val CIRCLE_L = "\u0083"
		const val CIRCLE_S = "\u0084"
		const val CROSS = "\u0085"
		const val SQUARE_L = "\u0086"
		const val SQUARE_S = "\u0087"
		const val DIA_L = "\u0088"
		const val DIA_S = "\u0089"
		const val UP_L = "\u0090"
		const val UP_S = "\u0091"
		const val HYPHEN = "\u0092"
		const val DOWN_L = "\u0093"
		const val DOWN_S = "\u0094"
		/** Backquote at end*/
		const val DQ_END = "\u0095"
		const val NAME_REV = "\u0096"
		const val NAME_END = "\u0097"
	}

	val rainbowCount:Int
	fun getImg(i:Int):ResourceImage<*>
	fun processTxt(x:Float, y:Float, str:String, color:COLOR, scale:Float, alpha:Float, rainbow:Int,
		draw:(i:Int, dx:Float, dy:Float, scale:Float, sx:Int, sy:Int, sw:Int, sh:Int, a:Float)->Unit)
	/** Draws the string
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String
	 * @param color Letter cint
	 * @param scale Enlargement factor
	 */
	fun printFont(x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f,
		rainbow:Int = rainbowCount)
	/** flagThefalseIf it&#39;s the casefontColorTrue cint, trueIf it&#39;s the
	 * casefontColorTrue colorDraws the string in
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String
	 * @param flag Conditional expression
	 * @param fontColorFalse flagThefalseText cint in the case of
	 * @param fontColorTrue flagThetrueText cint in the case of
	 * @param scale Enlargement factor
	 */
	fun printFont(x:Int, y:Int, str:String, flag:Boolean, fontColorFalse:COLOR = COLOR.WHITE,
		fontColorTrue:COLOR = COLOR.RAINBOW, scale:Float = 1f, alpha:Float = 1f,
		rainbow:Int = rainbowCount) =
		printFont(x, y, str, if(flag) fontColorTrue else fontColorFalse, scale, alpha, rainbow)
	/** Draws the string (16x16Grid units)
	 * @param fontX X-coordinate
	 * @param fontY Y-coordinate
	 * @param fontStr String
	 * @param fontColor Letter cint
	 */
	fun printFontGrid(fontX:Int, fontY:Int, fontStr:String, fontColor:COLOR = COLOR.WHITE, scale:Float = 1f,
		alpha:Float = 1f, rainbow:Int = rainbowCount) =
		printFont(fontX*16, fontY*16, fontStr, fontColor, scale, alpha, rainbow)
	/** flagThefalseIf it&#39;s the casefontColorTrue cint, trueIf it&#39;s the
	 * casefontColorTrue colorDraws the string in (16x16Grid units)
	 * @param fontX X-coordinate
	 * @param fontY Y-coordinate
	 * @param fontStr String
	 * @param flag Conditional expression
	 * @param fontColorFalse flagThefalseText cint in the case of
	 * @param fontColorTrue flagThetrueText cint in the case of
	 */
	fun printFontGrid(fontX:Int, fontY:Int, fontStr:String, flag:Boolean,
		fontColorFalse:COLOR = COLOR.WHITE, fontColorTrue:COLOR = COLOR.RAINBOW, scale:Float = 1f,
		alpha:Float = 1f, rainbow:Int = rainbowCount) =
		printFont(fontX*16, fontY*16, fontStr, flag, fontColorFalse, fontColorTrue, scale, alpha, rainbow)

}
