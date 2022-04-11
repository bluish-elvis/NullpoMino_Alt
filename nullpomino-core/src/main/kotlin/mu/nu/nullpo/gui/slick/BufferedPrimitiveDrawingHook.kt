/*
 * This library class was created by 0xFC963F18DC21 / Shots243
 * It is part of an extension library for the game NullpoMino (copyright 2021)
 *
 * Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 * Herewith shall the term "Game Creator" be given to the original creator of NullpoMino.
 *
 * THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 *
 * Repository: https://github.com/Shots243/ModePile
 *
 * When using this library in a mode / library pack of your own, the following
 * conditions must be satisfied:
 *     - This license must remain visible at the top of the document, unmodified.
 *     - You are allowed to use this library for any modding purpose.
 *         - If this is the case, the Library Creator must be credited somewhere.
 *             - Source comments only are fine, but in a README is recommended.
 *     - Modification of this library is allowed, but only in the condition that a
 *       pull request is made to merge the changes to the repository.
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

/*
 * Copyright (c) 2010-2021, NullNoname
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

import mu.nu.nullpo.game.event.EventReceiver
import org.apache.logging.log4j.LogManager
import org.newdawn.slick.Graphics
import zeroxfc.nullpo.custom.libs.RenderCommand
import zeroxfc.nullpo.custom.libs.RenderCommand.RenderType.Arc
import zeroxfc.nullpo.custom.libs.RenderCommand.RenderType.Oval
import zeroxfc.nullpo.custom.libs.RenderCommand.RenderType.Rectangle

class BufferedPrimitiveDrawingHook {
	/**
	 * Drawing command queue.
	 */
	private val commandBuffer:ArrayList<RenderCommand> = ArrayList()
	/**
	 * These can be placed in the non-render methods.
	 */
	fun drawRectangle(x:Int, y:Int, sizeX:Int, sizeY:Int, red:Int, green:Int, blue:Int, alpha:Int, fill:Boolean) {
		commandBuffer.add(
			RenderCommand(
				Rectangle, arrayOf(x, y, sizeX, sizeY, red, green, blue, alpha, fill))
		)
	}
	/**
	 * These can be placed in the non-render methods.
	 */
	fun drawArc(x:Int, y:Int, sizeX:Int, sizeY:Int, angleStart:Int, angleSize:Int, red:Int, green:Int, blue:Int, alpha:Int,
		fill:Boolean) {
		commandBuffer.add(
			RenderCommand(
				Arc, arrayOf(x, y, sizeX, sizeY, angleStart, angleSize, red, green, blue, alpha, fill))
		)
	}
	/**
	 * These can be placed in the non-render methods.
	 */
	fun drawOval(x:Int, y:Int, sizeX:Int, sizeY:Int, red:Int, green:Int, blue:Int, alpha:Int, fill:Boolean) {
		commandBuffer.add(
			RenderCommand(
				Oval, arrayOf(x, y, sizeX, sizeY, red, green, blue, alpha, fill))
		)
	}
	/**
	 * Draws all the commands to the renderer. Recommended to use in `renderLast`.
	 *
	 * @param receiver Renderer to use
	 */
	fun renderAll(receiver:EventReceiver?) {
		if(commandBuffer.size<=0) return
		try {
			val graphics:Graphics = ResourceHolderCustomAssetExtension.getGraphicsSlick(receiver as RendererSlick) ?: return
			for(cmd in commandBuffer) {
				try {
					when(cmd.renderType) {
						Rectangle -> PrimitiveDrawingSlick.drawRectangle(
							graphics,
							cmd.args[0] as Int,
							cmd.args[1] as Int,
							cmd.args[2] as Int,
							cmd.args[3] as Int,
							cmd.args[4] as Int,
							cmd.args[5] as Int,
							cmd.args[6] as Int,
							cmd.args[7] as Int,
							cmd.args[8] as Boolean
						)
						Arc -> PrimitiveDrawingSlick.drawArc(
							graphics,
							cmd.args[0] as Int,
							cmd.args[1] as Int,
							cmd.args[2] as Int,
							cmd.args[3] as Int,
							cmd.args[4] as Int,
							cmd.args[5] as Int,
							cmd.args[6] as Int,
							cmd.args[7] as Int,
							cmd.args[8] as Int,
							cmd.args[9] as Int,
							cmd.args[10] as Boolean
						)
						Oval -> PrimitiveDrawingSlick.drawOval(
							graphics,
							cmd.args[0] as Int,
							cmd.args[1] as Int,
							cmd.args[2] as Int,
							cmd.args[3] as Int,
							cmd.args[4] as Int,
							cmd.args[5] as Int,
							cmd.args[6] as Int,
							cmd.args[7] as Int,
							cmd.args[8] as Boolean
						)
						else -> log.error("Invalid render type.")
					}
				} catch(e:Exception) {
					log.error("Error encountered.", e)
					break
				}
			}
			commandBuffer.clear()

		} catch(e:Exception) {
			log.error("Something went wrong.", e)
		}
	}

	companion object {
		/**
		 * Error logger
		 */
		private val log = LogManager.getLogger()
	}
}
