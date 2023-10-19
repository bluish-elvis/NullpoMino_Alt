/*
 * Copyright (c) 2010-2023, NullNoname
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
package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.gui.common.BaseFontTTF
import org.newdawn.slick.Music
import org.newdawn.slick.UnicodeFont
import org.newdawn.slick.font.effects.ColorEffect
import org.newdawn.slick.font.effects.ShadowEffect
import org.newdawn.slick.openal.SoundStore
import java.awt.Color
import java.io.File
import mu.nu.nullpo.gui.slick.NullpoMinoSlick.Companion.propConfig as pCo
import mu.nu.nullpo.gui.slick.NullpoMinoSlick.Companion.propMusic as pMu

/** 画像や音声の管理をするクラス */
object ResourceHolder:mu.nu.nullpo.gui.common.ResourceHolder() {
	override val skinDir:String by lazy {NullpoMinoSlick.propGlobal.custom.skinDir}

//	override val bgMax get() = imgPlayBG.size

	/** Block images */
	override val imgNormalBlockList by lazy {
		try {
			super.imgNormalBlockList.map {ResourceImageSlick(it)}
		} catch(e:Exception) {
			log.error(e)
			emptyList()
		}
	}
	override val imgSmallBlockList by lazy {super.imgSmallBlockList.map {ResourceImageSlick(it)}}
	override val imgBigBlockList by lazy {super.imgBigBlockList.map {ResourceImageSlick(it)}}

	///** Block sticky flag */
//	override var blockStickyFlagList:LinkedList<Boolean> = LinkedList()

	/** Decoration Spriets : Badges and Medals */
	override val imgBadges = ResourceImageSlick(super.imgBadges)

	override val imgFont = super.imgFont.map {ResourceImageSlick(it)}
	override val imgFontNano = ResourceImageSlick(super.imgFontNano)
	override val imgNum = super.imgNum.map {ResourceImageSlick(it)}
	override val imgGrade = super.imgGrade.map {ResourceImageSlick(it)}
	override val imgFontMedal = ResourceImageSlick(super.imgFontMedal)

	override val imgCursor = ResourceImageSlick(super.imgCursor)
	//public static Image imgSprite;

	/** Field frame */
	override val imgFrame = super.imgFrame.map {ResourceImageSlick(it)}

	override val imgFrameOld = super.imgFrameOld.map {ResourceImageSlick(it)}

	/** Field background */
	override val imgFieldBG = super.imgFieldBG.map {ResourceImageSlick(it)}
	//public static Image imgFieldbg;
	override val imgFrags = super.imgFrags.map {ResourceImageSlick(it)}
	/** Beam animation during line clears */
	override val imgLine = super.imgLine.map {ResourceImageSlick(it)}

	/** Block spatter animation during line clears */
	override val imgBreak = super.imgBreak.map {it -> it.map {ResourceImageSlick(it)}}

	/** Effects for clearing gem blocks */
	override val imgPErase = super.imgPErase.map {ResourceImageSlick(it)}

	/** Effects for Fireworks */
	override val imgHanabi = super.imgHanabi.map {ResourceImageSlick(it)}

	/** Title Background */
	override var imgTitleBG = ResourceImageSlick(super.imgTitleBG, true)

	/** Title Logo */
	override val imgLogo = ResourceImageSlick(super.imgLogo, true)
	override val imgLogoSmall = ResourceImageSlick(super.imgLogoSmall, true)

	/** Menu Background */
	override val imgMenuBG:List<ResourceImageSlick> by lazy {super.imgMenuBG.map {ResourceImageSlick(it, true)}}

	/** プレイ中のBackground */
	override val imgPlayBG by lazy {super.imgPlayBG.map {ResourceImageSlick(it, true)}}
	override val imgPlayBGA by lazy {super.imgPlayBGA.map {ResourceImageSlick(it, true)}}
	/** TTF font */
	internal var ttfFont:UnicodeFont? = null

	/** Sound effects */
	internal var soundManager:SoundManager = SoundManager()

	/** BGM */
	private var bgm:List<MutableList<Pair<Music?, Boolean>>> = emptyList()

	/** Current BGM number */
	private var bgmint:Pair<Int, Int> = 0 to 0
	var bgmPlaying:BGM? = null; private set

	/** 画像や音声を読み込み */
	fun load() {
		try {
			loadImg(pCo.visual.showBG, pCo.visual.heavyEffect>0)
		} catch(e:Throwable) {
			log.error("Resource load failed", e)
		}
		// Font
		ttfFont = try {
			UnicodeFont("$skinDir/font/font.ttf", BaseFontTTF.FONT_SIZE, false, false).apply {
				effects.add(ShadowEffect(Color.BLACK, 1, 1, 1f))
				effects.add(ColorEffect(Color.WHITE))
			}
		} catch(e:Throwable) {
			log.error("TTF Font load failed", e)
			null
		}

		// Sound effects
		if(pCo.audio.se) {
			try {
				SoundStore.get().init()
			} catch(e:Throwable) {
				log.warn("Sound init failed", e)
			}

			log.info("Loading Sound Effect")
			soundSet.forEach {loadSE(it)}
			jingles.forEach {loadSE(it)}
		}

		// 音楽
		bgm = BGM.all.map {MutableList(it.size) {null to false}}
		bgmPlaying = null

		if(pCo.audio.bgmPreload) BGM.all.forEach {list ->
			list.forEach {loadBGM(it, false)}
		}
	}

	/*
		/** 画像読み込み
		 * @param filename Filename
		 * @return 画像 data
		 */
		fun loadImage(filename:String):Image {
			log.debug("Loading image from $filename")
			var bufI = Image(256, 256)
			try {
				bufI = Image("$skinDir/graphics/$filename.png")
			} catch(e:Exception) {
				if(e !is UnsupportedOperationException&&(e is IOException||e is SlickException))
					log.error("Failed to load image from $filename", e)
			}

			return bufI
		}*/

	private fun loadSE(name:String) {
		//log.info("LoadSE $name")
		val fn = "${NullpoMinoSlick.propGlobal.custom.skinDir}/se/$name"
		val wav = File("$fn.wav").canRead()&&soundManager.load(name, "$fn.wav")
		if(!wav&&File("$fn.ogg").canRead()) soundManager.load(name, "$fn.ogg")
	}

	/** 指定した numberのBGMをメモリ上に読み込み
	 * @param bgm enum [mu.nu.nullpo.game.component.BGMStatus.BGM]
	 * @param showErr 例外が発生したときにコンソールに表示する
	 */
	private fun loadBGM(bgm:BGM, showErr:Boolean = false) {
		if(!pCo.audio.bgm) return
		val name = bgm.name
		val n = bgm.longName
		bgm.id
		this.bgm[bgm.id].forEachIndexed {idx, (first) ->
			val sub = bgm.subName
			if(first==null) try {
				val filename = pMu.getProperty("music.filename.$name.$idx", null)
				if(filename.isNullOrEmpty()) {
					log.info("BGM $n:#$idx $sub not available")
					return@forEachIndexed
				}

				val streaming = pCo.audio.bgmStreaming
				if(File(filename).canRead()) {
					this.bgm[bgm.id][idx] = Music(filename, streaming) to pMu.getProperty("music.noloop.${bgm.name}.${bgm.idx}", false)
					//log.info("Loaded BGM $n:#$idx $sub")
				}
			} catch(e:Throwable) {
				if(showErr) log.error("BGM $n:#$idx $sub load failed", e)
				else log.warn("BGM $n:#$idx $sub load failed")
			}
			//else log.info("BGM $n:#$idx $sub is already load")
		}
	}

	/** 指定した numberのBGMを再生
	 * @param m enums of BGM [mu.nu.nullpo.game.component.BGMStatus.BGM]
	 */
	internal fun bgmStart(m:BGM) {
		if(!pCo.audio.bgm) return
		bgmStop()
		val x = minOf(m.id, bgm.size-1)
		val y = minOf(m.idx, bgm[x].size-1)
		NullpoMinoSlick.appGameContainer.musicVolume = (pCo.audio.bgmVolume/128f).also {
			if(it<=0) return bgmStop()
		}

		if(m!=BGM.Silent&&m!=bgmPlaying) {
			bgm[x][y].first?.also {
				try {
					val z = BGM.values.indexOf(m)
					if(bgm[x][y].second) it.play() else it.loop()
					log.info("Play BGM #$z = $x:$y ${m.longName}")
				} catch(e:Throwable) {
					log.error("Failed to play BGM $x:$y ${m.longName}", e)
				}
			} ?: loadBGM(m, true)

			bgmPlaying = m
			bgmint = x to y
		}
	}

	/** Current BGMを一時停止 */
	internal fun bgmPause() {
		if(bgmPlaying!=null) bgm[bgmint.first][bgmint.second].first?.pause()
	}

	/** 一時停止中のBGMを再開 */
	internal fun bgmResume() {
		if(bgmPlaying!=null) bgm[bgmint.first][bgmint.second].first?.resume()
	}

	/** BGM再生中かどうか
	 * @return 再生中ならtrue
	 */
	internal val bgmIsPlaying:Boolean get() = bgmPlaying!=null&&(bgm[bgmint.first][bgmint.second].first?.playing() ?: false)

	internal val bgmIsLooping:Boolean get() = bgmPlaying!=null&&bgm[bgmint.first][bgmint.second].second

	/** BGMを停止 */
	internal fun bgmStop() {
		bgm.flatMap {m -> m.mapNotNull {it.first}}.filter {it.playing()}.forEach {it.stop()}
		bgmPlaying = null
	}

	/** 全てのBGMをメモリから解放 */
	internal fun bgmUnloadAll() {
		bgm.forEachIndexed {x, a ->
			a.mapNotNull {it.first}.forEachIndexed {y, b ->
				b.stop()
				if(!pCo.audio.bgmPreload) bgm[x][y] = null to false
			}
		}
	}

}
