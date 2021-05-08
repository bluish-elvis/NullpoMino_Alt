package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.event.EventReceiver
import org.apache.log4j.Logger
import org.newdawn.slick.Graphics
import zeroxfc.nullpo.custom.libs.RenderCommand
import zeroxfc.nullpo.custom.libs.RenderCommand.RenderType.*

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
		private val log = Logger.getLogger(BufferedPrimitiveDrawingHook::class.java)
	}
}