/*
 Copyright (c) 2019-2024,
 This library class was created by 0xFC963F18DC21 / Shots243
 It is part of an extension library for the game NullpoMino (copyright 2010-2024)

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis

 Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.

 THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 THIS KOTLIN VERSION WAS NOT MADE IN ASSOCIATION WITH THE LIBRARY CREATOR.

 Original Repository: https://github.com/Shots243/ModePile

 When using this library in a mode / library pack of your own, the following
 conditions must be satisfied:
     - This license must remain visible at the top of the document, unmodified.
     - You are allowed to use this library for any modding purpose.
         - If this is the case, the Library Creator must be credited somewhere.
             - Source comments only are fine, but in a README is recommended.
     - Modification of this library is allowed, but only in the condition that a
       pull request is made to merge the changes to the repository.

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
package zeroxfc.nullpo.custom.libs.backgroundtypes

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.gui.slick.RendererSlick
import mu.nu.nullpo.gui.slick.ResourceHolder
import mu.nu.nullpo.gui.slick.ResourceImageSlick
import org.apache.logging.log4j.LogManager
import org.newdawn.slick.Color
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import org.newdawn.slick.Music

/**
Creates a new custom resource holder.

@param initialCapacity Start capacity of the internal hashmaps.
 */
class ResourceHolderCustomAssetExtension @JvmOverloads constructor(initialCapacity:Int = 8) {
	private var slickImages // Slick Images
		:HashMap<String, Image> = HashMap(initialCapacity)
	private var slickMusic // Slick Music
		:HashMap<String, Music> = HashMap(initialCapacity)
	/**
	 * Adds an image to the custom image library
	 *
	 * @param filePath Path of image file
	 * @param name     Identifier name
	 */
	fun loadImage(filePath:String, name:String) {
		slickImages[name] = ResourceImageSlick(filePath).apply {load()}.res
	}

	/**
	 * Copies an image from the HashMap key to another key.
	 *
	 * @param source Source key
	 * @param dest   Destination key
	 */
	fun copyImage(source:String, dest:String) {
		slickImages[source]?.let {slickImages.replace(dest, it)}
	}
	/**
	 * Gets the pixel dimensions of the named image.
	 *
	 * @param name Image name in holder dictionary.
	 * @return int[] { width, height } (both in pixels).
	 */
	fun getImageDimensions(name:String):List<Int> = listOf(slickImages[name]?.width ?: 0, slickImages[name]?.height ?: 0)

	/**
	 * Puts image in the holder at name.
	 *
	 * @param name Image name
	 */
	fun putImageAt(image:Image?, name:String) {
		image ?: return
		try {
			slickImages[name] = image
		} catch(e:Exception) {
			log.error("Unable to insert image $image at $name")
		}
	}
	/**
	 * Puts image in the holder at name.
	 *
	 * @param name Image name
	 */
	fun putImageAt(image:ResourceImageSlick?, name:String) {
		image ?: return
		try {
			putImageAt(image.res, name)
		} catch(e:Exception) {
			log.error("Unable to insert image $image at $name")
		}
	}
	/**
	 * Gets object that is an image instance.
	 * `A CAST IS STRICTLY NECESARRY!`
	 *
	 * @param name Image name
	 * @return Image at name
	 */
	fun getImageAt(name:String):Image? {
		return try {
			slickImages[name]
		} catch(e:Exception) {
			null
		}
	}
	/**
	 * Replaces an image.
	 *
	 * @param name  Key of image to replace
	 * @param image Image object to replace with.
	 */
	fun setImageAt(name:String, image:Image) {
		try {
			slickImages.replace(name, image)
			log.info("Image $name replaced.")
		} catch(e:Exception) {
			log.error("Image does not exist or invalid cast attempted.")
		}
	}
	/**
	 * Sets rotation center for an image when using Slick renderer.
	 *
	 * @param name Image name
	 * @param x    X-coordinate relative to image's top-left corner
	 * @param y    Y-coordinate relative to image's top-left corner
	 */
	fun setRotationCenter(name:String, x:Float, y:Float) {
		slickImages[name]?.setCenterOfRotation(x, y)
	}
	/**
	 * Sets rotation for an image when using Slick renderer.
	 *
	 * @param name Image name
	 * @param a    Angle, degrees.
	 */
	fun setRotation(name:String, a:Float) {
		slickImages[name]?.rotation = a
	}
	/**
	 * Draws image to game.
	 *
	 * @param name     Identifier of image.
	 * @param x        X position
	 * @param y        Y position
	 * @param srcX     Source X position
	 * @param srcY     Source Y position
	 * @param srcSizeX Source X size
	 * @param srcSizeY Source Y size
	 * @param color    Color component
	 * @param scale    Image scale
	 */
	fun drawImage(name:String, x:Float, y:Float, srcX:Float, srcY:Float, srcSizeX:Float, srcSizeY:Float,
		color:Color = Color(255, 255, 255, 255), scale:Float = 1f) {
		slickImages[name]?.let {toDraw ->
			val fx = x+(srcSizeX*scale)
			val fy = y+(srcSizeY*scale)
			toDraw.draw(x, y, fx, fy, srcX, srcY, (srcX+srcSizeX), (srcY+srcSizeY), color)
		}
	}

	/**
	 * Draws image to game.
	 *
	 * @param name     Identifier of image.
	 * @param x        X position
	 * @param y        Y position
	 * @param srcX     Source X position
	 * @param srcY     Source Y position
	 * @param srcSizeX Source X size
	 * @param srcSizeY Source Y size
	 * @param red      Red component
	 * @param green    Green component
	 * @param blue     Blue component
	 * @param alpha    Alpha component
	 * @param scale    Image scale
	 */
	fun drawImage(name:String, x:Int, y:Int, srcX:Int, srcY:Int, srcSizeX:Int, srcSizeY:Int, red:Int,
		green:Int = 255, blue:Int = 255, alpha:Int = 255, scale:Float = 1f) {
		drawImage(
			name, x.toFloat(), y.toFloat(), srcX.toFloat(), srcY.toFloat(), srcSizeX.toFloat(), srcSizeY.toFloat(),
			Color(red, green, blue, alpha), scale
		)
	}
	/**
	 * Draws image to game.
	 *
	 * @param name     Identifier of image.
	 * @param x        X position
	 * @param y        Y position
	 * @param sx       X size
	 * @param sy       Y size
	 * @param srcX     Source X position
	 * @param srcY     Source Y position
	 * @param srcSizeX Source X size
	 * @param srcSizeY Source Y size
	 * @param red      Red component
	 * @param green    Green component
	 * @param blue     Blue component
	 * @param alpha    Alpha component
	 */
	fun drawImage(name:String, x:Int, y:Int, sx:Int, sy:Int, srcX:Int, srcY:Int, srcSizeX:Int, srcSizeY:Int,
		red:Int = 255, green:Int = 255, blue:Int = 255, alpha:Int = 255) {
		if(sx<=0||sy<=0) return
		slickImages[name]?.let {toDraw ->
			val fx = x+sx
			val fy = y+sy
			val filter = Color(red, green, blue, alpha)
			toDraw.draw(
				x.toFloat(), y.toFloat(), fx.toFloat(), fy.toFloat(), srcX.toFloat(), srcY.toFloat(),
				(srcX+srcSizeX).toFloat(), (srcY+srcSizeY).toFloat(), filter
			)
		}
	}
	/**
	 * Draws whole image to game with no tint.
	 *
	 * @param name   Identifier of image.
	 * @param x      X position
	 * @param y      Y position
	 */
	fun drawImage(name:String, x:Int, y:Int) {
		slickImages[name]?.let {toDraw ->
			val fx = toDraw.width
			val fy = toDraw.height
			drawImage(name, x, y, 0, 0, fx, fy, 255, 255, 255, 255, 1f)
		}
	}
	/**
	 * Draws image to game with string color definition.
	 * 8 character RRGGBBAA hex code or 6 character RRGGBB hex code.
	 *
	 * @param name     Identifier of image.
	 * @param x        X position
	 * @param y        Y position
	 * @param srcX     Source X position
	 * @param srcY     Source Y position
	 * @param srcSizeX Source X size
	 * @param srcSizeY Source Y size
	 * @param color    Hex. code string of color.
	 * @param scale    Image scale
	 */
	fun drawImage(name:String, x:Int, y:Int, srcX:Int, srcY:Int, srcSizeX:Int, srcSizeY:Int, color:String, scale:Float) {
		val lc:String = color.replace("#", "").lowercase()
		var alpha = "FF"
		if(lc.length!=8&&lc.length!=6) {
			return
		}
		val red:String = lc.take(2)
		val green:String = lc.substring(2, 4)
		val blue:String = lc.substring(4, 6)
		if(lc.length==8) alpha = lc.substring(6, 8)
		val r = red.toInt(16)
		val g = green.toInt(16)
		val b = blue.toInt(16)
		val a = alpha.toInt(16)
		drawImage(name, x, y, srcX, srcY, srcSizeX, srcSizeY, r, g, b, a, scale)
	}
	/*
		/**
		 * Appends a new BGM number into the BGM list. On Swing, this has no effect.
		 *
		 * @param filename File path of music file to import.
		 * @param showerr  Show in log?
		 */
		fun loadNewBGMAppend(filename:String?, noLoop:Boolean, showerr:Boolean) {
			if(!NullpoMinoSlick.propConfig.audio.bgm) return
			val no = BGMStatus.BGM.all.size+1
			val newArr = arrayOfNulls<Music>(no)
			var i = 0
			while(i<ResourceHolder.bgm.size) {
				newArr[i] = ResourceHolder.bgm
				i++
			}
			if(newArr[no-1]==null) {
				if(showerr) log.info("Loading BGM at $filename")
				try {
					// String filename = NullpoMinoSlick.propMusic.getProperty("music.filename." + no, null);
					if(filename==null||filename.isEmpty()) {
						if(showerr) log.info("BGM at $filename not available")
						return
					}
					NullpoMinoSlick.propMusic.setProperty("music.noloop.$no", noLoop)
					// boolean streaming = NullpoMinoSlick.propConfig.audio.bgmStreaming;
					newArr[no-1] = Music(filename, true)
					ResourceHolder.bgm = arrayOf(newArr.clone())
					if(!showerr) log.info("Loaded BGM at $filename")
				} catch(e:Throwable) {
					if(showerr) log.error("BGM at $filename load failed", e) else log.warn("BGM at $filename load failed")
				}
			}
			return
		}

		/**
		 * Removes a loaded BGM file from the end of the BGM array. On Swing, this has no effect.
		 *
		 * @param showerr Show in log?
		 */
		fun removeBGMFromEnd(showerr:Boolean) {
			if(!NullpoMinoSlick.propConfig.audio.bgm) return
			val no:Int = ResourceHolder.bgm.size-1
			val newBGM = arrayOfNulls<Music>(no)
			var i = 0
			while(i<newBGM.size) {
				newBGM[i] = ResourceHolder.bgm.flatten()[i]
				i++
			}
			ResourceHolder.bgm = arrayOf(newBGM.clone())
			if(showerr) log.info("Removed BGM at $no")
			return
		}

		/**
		 * Starts the play of an internal BGM. Use the relative index of the added music. (0 = first added track.)
		 * There is a limitation that the song can't be paused.
		 *
		 * @param name   Track name.
		 * @param noLoop Set true if it shouldn't loop.
		 */
		fun playCustomBGM(name:String, noLoop:Boolean) {
			if(!NullpoMinoSlick.propConfig.audio.bgm) return
			stopCustomBGM()
			val bgmVolume = NullpoMinoSlick.propConfig.audio.bgmVolume
			NullpoMinoSlick.appGameContainer.musicVolume = bgmVolume/128.toFloat()
			try {
				if(noLoop) slickMusic[name]!!.play() else slickMusic[name]!!.play()
			} catch(e:Throwable) {
				log.error("Failed to play music $name", e)
			}
			return
		}

		/**
		 * Remove internal custom BGM by name
		 *
		 * @param name BGM name
		 */
		fun removeCustomInternalBGM(name:String?) = slickMusic.remove(name)

		/**
		 * Gets number of loaded, appended BGM files.
		 *
		 * @return Number of loaded, appended BGM files.
		 */
		fun getAmountLoadedBGM():Int = ResourceHolder.bgm.size-BGMStatus.BGM.count

		/**
		 * Gets number of loaded BGM files in holder.
		 *
		 * @return Number of loaded BGM files in holder.
		 */
		fun getAmountDiscreteLoadedBGM():Int = slickMusic.size

		/**
		 * Stops all custom BGM.
		 */
		fun stopCustomBGM() {
			if(!NullpoMinoSlick.propConfig.audio.bgm) return
			slickMusic.forEach {t, u ->
				u.pause()
				u.stop()
			}
			return
		}
		/**
		 * Stops custom BGM.
		 */
		fun stopBGM() {
			if(!NullpoMinoSlick.propConfig.audio.bgm) return
			for(s in ResourceHolder.bgm) {
				s?.pause()
				s?.stop()			}
			return
		}

		/**
		 * Stops all BGM.
		 *
		 * @param owner Current GameManager
		 */
		fun stopDefaultBGM(owner:GameManager) {
			if(owner.bgmStatus.bgm!=BGMStatus.BGM.Silent) bgmPrevious = owner.bgmStatus.bgm
			owner.bgmStatus.bgm = BGMStatus.BGM.Silent
		}

		/**
		 * Restarts previously playing default BGM.
		 *
		 * @param owner Current GameManager
		 */
		fun restartDefaultBGM(owner:GameManager) {
			if(owner.bgmStatus.bgm==BGMStatus.BGM.Silent) {
				owner.bgmStatus.bgm = bgmPrevious
				owner.bgmStatus.fadeSW = false
			}
		}*/

	companion object {
		private val log = LogManager.getLogger()
		private var bgmPrevious:BGMStatus.BGM = BGMStatus.BGM.Silent// Thread-safe code used for when more threads are being used.
		// Warning: slower.
		/**
		 * Gets the current instance's main class name.
		 *
		 * @return Main class name.
		 */
		var mainClassName = ""
			get() {
				if(field.isEmpty()||field=="Unknown") {
					// Thread-safe code used for when more threads are being used.
					// Warning: slower.
					val allStackTraces:Collection<Array<StackTraceElement>> = Thread.getAllStackTraces().values
					for(traceElements in allStackTraces) {
						for(element in traceElements) {
							val name = element.className
							if(name.contains("NullpoMinoSlick")||name.contains("NullpoMinoSwing")||name.contains("NullpoMinoSDL")) {
								field = name
								break
							}
						}
						if(field.isNotEmpty()) break
					}
					if(field.isEmpty()) field = "Unknown"
				}
				return field
			}
			private set
		/**
		 * Gets the number of currently loaded block-skins inside the game.
		 *
		 * @return Number of block skins.
		 */
		val numberLoadedBlockSkins:Int
			get() = ResourceHolder.imgNormalBlockList.size

		fun getGraphicsSlick(renderer:RendererSlick):Graphics? {
			return try {
				renderer.graphics
			} catch(e:Exception) {
				log.error("Failed to extract graphics from Slick renderer.")
				null
			}
		}
		/*
				fun setGraphicsSlick(renderer:RendererSlick?, grp:Graphics?) {
					val local:Class<RendererSlick> = RendererSlick::class.java
					val localField:Field
					try {
						localField = local.getDeclaredField("graphics")
						localField.isAccessible = true
						localField[renderer] = grp
					} catch(e:Exception) {
						log.error("Failed to extract graphics from Slick renderer.")
					}
				}*/
	}
}
