/* Copyright (c) 2010, NullNoname
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of NullNoname nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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
 * POSSIBILITY OF SUCH DAMAGE. */
package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.component.BGMStatus.BGM
import org.apache.log4j.Logger
import org.newdawn.slick.*
import org.newdawn.slick.font.effects.ColorEffect
import org.newdawn.slick.font.effects.ShadowEffect
import org.newdawn.slick.openal.SoundStore
import java.awt.Color
import java.io.File
import java.io.IOException
import java.util.*

/** 画像や音声の管理をするクラス */
object ResourceHolder {
	/** Log */
	internal val log = Logger.getLogger(ResourceHolder::class.java)

	/** Number of images for block spatter animation during line clears */
	internal const val BLOCK_BREAK_MAX = 8

	/** Number of image splits for block spatter animation during line clears */
	internal const val BLOCK_BREAK_SEGMENTS = 2

	/** Number of gem block clear effects */
	internal const val PERASE_MAX = 7
	internal const val HANABI_MAX = 7

	/** Block images */
	internal var imgNormalBlockList:LinkedList<Image> = LinkedList()
	internal var imgSmallBlockList:LinkedList<Image> = LinkedList()
	internal var imgBigBlockList:LinkedList<Image> = LinkedList()

	/** Block sticky flag */
	internal var blockStickyFlagList:LinkedList<Boolean> = LinkedList()

	/** Decoration Spriets : Badges and Medals */
	internal lateinit var imgBadges:Image
	/** Regular font */
	internal lateinit var imgFont:Image
	internal lateinit var imgFontBig:Image
	internal lateinit var imgFontSmall:Image
	internal lateinit var imgFontNano:Image
	/** Number font */
	internal lateinit var imgNumBig:Image
	internal lateinit var imgNum:Image
	/** Grade font */
	internal lateinit var imgGradeBig:Image
	internal lateinit var imgGrade:Image
	internal lateinit var imgFontMedal:Image
	/** 小物画像 */
	internal lateinit var imgCursor:Image
	//public static Image imgSprite;

	/** Field frame */
	internal var imgFrame:Array<Image> = emptyArray()
	internal var imgFrameOld:Array<Image> = emptyArray()

	/** Field background */
	internal lateinit var imgFieldbg2:Image
	internal lateinit var imgFieldbg2Small:Image
	internal lateinit var imgFieldbg2Big:Image
	//public static Image imgFieldbg;

	/** Beam animation during line clears */
	internal var imgLine:Array<Image> = emptyArray()
	/** Block spatter animation during line clears */
	internal var imgBreak:Array<Array<Image>> = emptyArray()
	/** Effects for clearing gem blocks */
	internal var imgPErase:Array<Image> = emptyArray()
	/** Effects for Fireworks */
	internal var imgHanabi:Array<Image> = emptyArray()

	/** Title Background */
	internal lateinit var imgTitleBG:Image
	/** Title Logo */
	internal lateinit var imgLogo:Image
	internal lateinit var imgLogoSmall:Image

	/** Menu Background */
	internal var imgMenuBG:Array<Image> = emptyArray()
	/** プレイ中のBackground */
	internal var imgPlayBG:Array<Image> = emptyArray()

	/** TTF font */
	internal var ttfFont:UnicodeFont? = null

	/** Sound effects */
	var soundManager:SoundManager = SoundManager()

	/** BGM */
	var bgm:Array<Array<Music?>> = emptyArray()

	/** Current BGM number */
	private var bgmint:Pair<Int, Int> = Pair(0, 0)
	var bgmPlaying:BGM? = null;private set
	internal val bGmax:Int get() = imgPlayBG.size

	/** 画像や音声を読み込み */
	fun load() {
		val skindir = NullpoMinoSlick.propConfig.getProperty("custom.skin.directory", "res")

		log.info("Loading Image")

		// Blocks
		var numBlocks = 0
		while(File("$skindir/graphics/blockskin/normal/n$numBlocks.png").canRead())
			numBlocks++

		log.debug("$numBlocks block skins found")

		for(i in 0 until numBlocks) {
			loadImage("$skindir/graphics/blockskin/normal/n$i.png").let {
				imgNormalBlockList.add(it)
				blockStickyFlagList.add((it.width>=400&&it.height>=304))
			}
			imgSmallBlockList.add(loadImage("$skindir/graphics/blockskin/small/s$i.png"))
			imgBigBlockList.add(loadImage("$skindir/graphics/blockskin/big/b$i.png"))
		}
		var numFrames = 0
		while(File("$skindir/graphics/frames/$numFrames.png").canRead())
			numFrames++

		log.debug("$numBlocks frame skins found")


		imgFrame = Array(numFrames) {loadImage("$skindir/graphics/frames/$it.png")}
		imgFrameOld = arrayOf(
			loadImage("$skindir/graphics/frames/gb.png"),
			loadImage("$skindir/graphics/frames/sa.png"),
			loadImage("$skindir/graphics/frames/hebo.png"))

		// Other images
		imgFont = loadImage("$skindir/graphics/font.png")
		imgFontNano = loadImage("$skindir/graphics/font_nano.png")
		imgFontSmall = loadImage("$skindir/graphics/font_small.png")
		imgFontBig = loadImage("$skindir/graphics/font_big.png")
		imgFontMedal = loadImage("$skindir/graphics/font_medal.png")

		imgNumBig = loadImage("$skindir/graphics/number_big.png")
		imgNum = loadImage("$skindir/graphics/number_small.png")
		imgGradeBig = loadImage("$skindir/graphics/grade_big.png")
		imgGrade = loadImage("$skindir/graphics/grade_small.png")

		imgBadges = loadImage("$skindir/graphics/badge.png")

		imgCursor = loadImage("$skindir/graphics/effects/target.png")


		imgLogo = loadImage("$skindir/graphics/logo.png")
		imgLogoSmall = loadImage("$skindir/graphics/logo_small.png")

		imgTitleBG = loadImage("$skindir/graphics/title.png")
		imgMenuBG = arrayOf(
			loadImage("$skindir/graphics/menu.png"),
			loadImage("$skindir/graphics/menu_in.png"))

		imgFieldbg2 = loadImage("$skindir/graphics/fieldbg2.png")
		imgFieldbg2Small = loadImage("$skindir/graphics/fieldbg2_small.png")
		imgFieldbg2Big = loadImage("$skindir/graphics/fieldbg2_big.png")

		if(NullpoMinoSlick.propConfig.getProperty("option.showbg", true)) loadBackgroundImages()
		if(NullpoMinoSlick.propConfig.getProperty("option.showlineeffect", true)) loadLineClearEffectImages()
		imgLine = arrayOf(
			loadImage("$skindir/graphics/effects/del_h.png"),
			loadImage("$skindir/graphics/effects/del_v.png"))

		// Font
		ttfFont = try {
			UnicodeFont("$skindir/font/font.ttf", 16, false, false).apply {
				effects.add(ShadowEffect(Color.BLACK, 1, 1, 1f))
				effects.add(ColorEffect(Color.WHITE))
			}
		} catch(e:Throwable) {
			log.error("TTF Font load failed", e)
			null
		}

		// Sound effects
		if(NullpoMinoSlick.propConfig.getProperty("option.se", true)) {
			try {
				SoundStore.get().init()
			} catch(e:Throwable) {
				log.warn("Sound init failed", e)
			}

			log.info("Loading Sound Effect")
			loadSE("cursor")
			loadSE("change")
			loadSE("decide")
			for(i in 0..2) {
				loadSE("decide$i")
				loadSE("firecracker$i")
			}

			for(i in 0..10)
				loadSE("piece$i")

			loadSE("hold")
			loadSE("initialhold")
			loadSE("holdfail")
			loadSE("move")
			loadSE("movefail")
			loadSE("rotate")
			loadSE("wallkick")
			loadSE("initialrotate")
			loadSE("rotfail")
			loadSE("harddrop")
			loadSE("softdrop")
			loadSE("step")
			loadSE("lock")
			loadSE("erase")
			loadSE("eraser")
			for(i in 0..3)
				loadSE("line${i+1}")
			loadSE("linefall")
			for(i in 0..4)
				loadSE("applause$i")
			loadSE("twist")
			loadSE("tspin")
			loadSE("combo")
			loadSE("b2b_start")
			loadSE("b2b_combo")
			loadSE("b2b_end")

			loadSE("danger")
			loadSE("dead")
			loadSE("shutter")
			loadSE("gameover")
			loadSE("end")

			for(i in 0..4)
				loadSE("grade$i")
			loadSE("gradeup")
			loadSE("levelstop")
			loadSE("levelup")
			loadSE("levelup_section")

			loadSE("endingstart")
			loadSE("excellent")
			loadSE("bravo")
			loadSE("cool")
			loadSE("regret")
			for(i in 1..3)
				loadSE("medal$i")

			loadSE("ready")
			loadSE("go")
			loadSE("pause")
			loadSE("countdown")
			loadSE("hurryup")
			loadSE("timeout")
			loadSE("stageclear")
			loadSE("stagefail")
			loadSE("matchend")

			loadSE("garbage")
			loadSE("gem")
			loadSE("bomb")
			loadSE("square_s")
			loadSE("square_g")

		}

		// 音楽
		bgm = BGM.all.map {arrayOfNulls<Music?>(it.size)}.toTypedArray()
		bgmPlaying = null

		if(NullpoMinoSlick.propConfig.getProperty("option.bgmpreload", false))
			BGM.all.forEach{list -> list.forEach{loadBGM(it, false)}}
	}

	/** Load background images. */
	internal fun loadBackgroundImages() {
		if(imgPlayBG.isNullOrEmpty()) {
			val skindir = NullpoMinoSlick.propConfig.getProperty("custom.skin.directory", "res")+"/graphics/back/back"
			var numBGs = 0
			while(File("$skindir$numBGs.png").canRead())
				numBGs++

			if(numBGs>0) log.debug("$numBGs backgrounds found")
			else log.warn("no backgrounds found")
			imgPlayBG = Array(numBGs) {loadImage("$skindir$it.png")}
		}
	}

	/** Load line clear effect images. */
	internal fun loadLineClearEffectImages() {
		val skindir = NullpoMinoSlick.propConfig.getProperty("custom.skin.directory", "res")

		if(imgBreak.isNullOrEmpty()) imgBreak = Array(BLOCK_BREAK_MAX) {i ->
			Array(BLOCK_BREAK_SEGMENTS) {
				loadImage("$skindir/graphics/effects/break${i}_$it.png")
			}
		}

		if(imgPErase.isNullOrEmpty()) imgPErase = Array(PERASE_MAX) {
			loadImage("$skindir/graphics/effects/perase$it.png")
		}
		if(imgHanabi.isNullOrEmpty()) imgHanabi = Array(HANABI_MAX) {
			loadImage("$skindir/graphics/effects/hanabi$it.png")
		}
	}

	/** 画像読み込み
	 * @param filename Filename
	 * @return 画像 data
	 */
	private fun loadImage(filename:String):Image {
		log.debug("Loading image from $filename")

		var img = Image(256, 256)
		try {
			img = Image(filename)
		} catch(e:Exception) {
			if(e !is UnsupportedOperationException&&(e is IOException||e is SlickException))
				log.error("Failed to load image from $filename", e)
		}

		return img
	}

	private fun loadSE(name:String) {

		val fn = "${NullpoMinoSlick.propConfig.getProperty("custom.skin.directory", "res")}/se/$name"
		if(File("$fn.wav").canRead())
			soundManager.load(name, "$fn.wav")
		else if(File("$fn.ogg").canRead()) soundManager.load(name, "$fn.ogg")
	}

	/** 指定した numberのBGMをメモリ上に読み込み
	 * @param bgm BGM enum
	 * @param showErr 例外が発生したときにコンソールに表示する
	 */
	private fun loadBGM(bgm:BGM, showErr:Boolean) {
		if(!NullpoMinoSlick.propConfig.getProperty("option.bgm", false)) return
		val name = bgm.name
		val n = bgm.longName
		bgm.id
		this.bgm[bgm.id].forEachIndexed {idx, b ->
			val sub = bgm.subName
			if(b==null) try {
				val filename = NullpoMinoSlick.propMusic.getProperty("music.filename.$name.$idx", null)
				if(filename.isNullOrEmpty()) {
					log.info("BGM $n:#$idx $sub not available")
					return
				}

				val streaming = NullpoMinoSlick.propConfig.getProperty("option.bgmstreaming", true)
				if(File(filename).canRead()) {
					this.bgm[bgm.id][idx] = Music(filename, streaming)
					log.info("Loaded BGM $n:#$idx $sub")
				}
			} catch(e:Throwable) {
				if(showErr) log.error("BGM $n:#$idx $sub load failed", e)
				else log.warn("BGM $n:#$idx $sub load failed")
			}
			else log.info("BGM $n:#$idx $sub is already load")
		}
	}

	/** 指定した numberのBGMを再生
	 * @param M enums of BGM [mu.nu.nullpo.game.component.BGMStatus.BGM]
	 */
	internal fun bgmStart(M:BGM) {
		if(!NullpoMinoSlick.propConfig.getProperty("option.bgm", false)) return
		bgmStop()
		val x = M.id
		val y = M.idx
		val bgmvolume = NullpoMinoSlick.propConfig.getProperty("option.bgmvolume", 128)
		NullpoMinoSlick.appGameContainer.musicVolume = bgmvolume/256.toFloat()

		if(M!=BGM.SILENT && M!=bgmPlaying) {
			bgm[x][y]?.also {
				try {
					if(NullpoMinoSlick.propMusic.getProperty("music.noloop.${M.name}", false))
						it.play()
					else it.loop()
					log.info("Play BGM $x:$y ${M.longName}")
				} catch(e:Throwable) {
					log.error("Failed to play BGM $x:$y ${M.longName}", e)
				}
			} ?: loadBGM(M, true)

			bgmPlaying = M
			bgmint = Pair(x, y)
		}
	}

	/** Current BGMを一時停止 */
	fun bgmPause() {
		if(bgmPlaying!=null) bgm[bgmint.first][bgmint.second]?.pause()
	}

	/** 一時停止中のBGMを再開 */
	internal fun bgmResume() {
		if(bgmPlaying!=null) bgm[bgmint.first][bgmint.second]?.resume()
	}

	/** BGM再生中かどうか
	 * @return 再生中ならtrue
	 */
	internal fun bgmIsPlaying():Boolean = bgmPlaying!=null&&(bgm[bgmint.first][bgmint.second]?.playing() ?: false)


	/** BGMを停止 */
	internal fun bgmStop() {
		bgm.forEach {
			it.forEach {m ->
				m?.pause()
				m?.stop()
			}
			bgmPlaying = null
		}
	}

	/** 全てのBGMをメモリから解放 */
	internal fun bgmUnloadAll() {
		bgm.forEachIndexed {x, a ->
			a.forEachIndexed {y, b ->
				b?.stop()
				if(!NullpoMinoSlick.propConfig.getProperty("option.bgmpreload", false)) bgm[x][y] = null
			}
		}

	}

}
