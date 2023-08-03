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

import com.davekoelle.AlphaNumComparator
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.gui.common.ResourceImage.ResourceImageStr
import mu.nu.nullpo.util.GeneralUtil.flattenList
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import java.io.FileFilter

abstract class ResourceHolder {
	/** log */
	val log:Logger = LogManager.getLogger()

	private val logConf = "config/etc/log.xml"

	init {
		try {
			val originLog = File(ResourceHolder::class.java.getResource("/log4j2.xml")?.toString() ?: "")
			val f = File(logConf)
			if(!f.parentFile.exists()) f.parentFile.mkdirs()
			else if(f.isDirectory) f.deleteRecursively()
			if(!f.exists()) originLog.copyTo(f)
		} catch(e:Exception) {
			log.error("Logfile init", e)
		}
	}

	fun getBlockIsSticky(skin:Int):Boolean = skin in 0..<imgBlockListSize&&blockStickyFlagList[skin]

	protected val filterImg = FileFilter {it.isImage}
	protected val nameSort:Comparator<File> = Comparator {x, y ->
		AlphaNumComparator.instance.compare(x.nameWithoutExtension, y.nameWithoutExtension)
	}
	protected val File.isImage get() = isFile&&canRead()&&extension=="png"
	open val skinDir = "res"

	/** Number of image splits for block spatter animation during line clears */
	internal val blockBreakSegments:Int = 2

	/** Number of images for block spatter animation during line clears */
	internal val blockBreakMax = Block.COLOR.COLOR_NUM+1

	/** Number of gem block clear effects */
	internal val pEraseMax = Block.COLOR.COLOR_NUM
	internal val hanabiMax = Block.COLOR.COLOR_NUM

	/** Block images */
	internal open val imgNormalBlockList:List<ResourceImage<*>> by lazy {
		try {
			File("$skinDir/graphics/blockskin/normal/").listFiles(filterImg)?.sortedWith(nameSort)?.map {
				ResourceImageStr("blockskin/normal/"+it.nameWithoutExtension)
//loadImage("blockskin/normal/n$i")
			} ?: emptyList()
		} catch(e:Exception) {
			//log.error(e)
			emptyList()
		}
	}

	val imgBlockListSize:Int get() = imgNormalBlockList.size
	/** Block sticky flag */
	val blockStickyFlagList:List<Boolean> get() = imgNormalBlockList.map {it.width>=400&&it.height>=304}

	internal open val imgSmallBlockList:List<ResourceImage<*>> by lazy {
		File("$skinDir/graphics/blockskin/small/").listFiles(filterImg)?.sortedWith(nameSort)?.map {
			ResourceImageStr("blockskin/small/"+it.nameWithoutExtension)
			//loadImage("blockskin/small/s$i")
		} ?: emptyList()
	}
	internal open val imgBigBlockList:List<ResourceImage<*>> by lazy {
		File("$skinDir/graphics/blockskin/big/").listFiles(filterImg)?.sortedWith(nameSort)?.map {
			ResourceImageStr("blockskin/big/"+it.nameWithoutExtension)
			//loadImage("blockskin/big/b$i")
		} ?: emptyList()
	}

	internal open val imgItemBlock:List<ResourceImage<*>> = listOf("s", "n", "b").map {ResourceImageStr("blockskin/item$it")}
	/** Decoration Spriets : Badges and Medals */
	internal open val imgBadges:ResourceImage<*> = ResourceImageStr("badge")

	/** Regular fonts */
	internal open val imgFont:List<ResourceImage<*>> = listOf("_small", "", "_big").map {ResourceImageStr("font$it")}
	internal open val imgFontNano:ResourceImage<*> = ResourceImageStr("font_nano")
	/** Number fonts */
	internal open val imgNum:List<ResourceImage<*>> = listOf("small", "big").map {ResourceImageStr("number_$it")}
	/** Grade fonts */
	internal open val imgGrade:List<ResourceImage<*>> = listOf("small", "big").map {ResourceImageStr("grade_$it")}
	/** Medal fonts */
	internal open val imgFontMedal:ResourceImage<*> = ResourceImageStr("font_medal")

	/** 小物画像 */
	internal open val imgCursor:ResourceImage<*> = ResourceImageStr("effects/target")
	//public static Image imgSprite;

	/** Field frame */
	internal open val imgFrame:List<ResourceImage<*>> by lazy {
		File("$skinDir/graphics/frames/").listFiles(FileFilter {it.isImage&&it.nameWithoutExtension.all {i -> i.isDigit()}})
			?.sortedWith(nameSort)?.map {
				ResourceImageStr("frames/"+it.nameWithoutExtension)
			} ?: emptyList()
	}

	/** Field frame for retro mode */
	internal open val imgFrameOld:List<ResourceImage<*>> =
		listOf("gb", "sa", "hebo", "grade").map {ResourceImageStr("frames/$it")}

	/** Field background */
	internal open val imgFieldBG:List<ResourceImage<*>> =
		listOf("_small", "", "_big").map {ResourceImageStr("fieldbg2$it")}
	//public static Image imgFieldbg;

	/**Particles sprite*/
	internal open val imgFrags:List<ResourceImage<*>> =
		listOf("brk_halo", "brk_tail", "fw_part").map {ResourceImageStr("effects/frag_$it")}

	/**Item Spritesheets*/
	internal open val imgItemAnims:List<ResourceImage<*>> =
		listOf("mirr", "roll", "big", "xray", "col", "dark", "morph", "nega", "shot", "excg", "hard", "reve").map {
			ResourceImageStr("effects/frag_$it")
		}
	/** Beam Spritesheets for line clears */
	internal open val imgLine:List<ResourceImage<*>> = listOf("h", "v").map {ResourceImageStr("effects/del_$it")}
	/** Block spatter Spritesheets for line clears */
	internal open val imgBreak:List<List<ResourceImage<*>>> = List(blockBreakMax) {i ->
		List(blockBreakSegments) {
			ResourceImageStr("effects/break${i}_$it")
		}
	}

	/** Effects for clearing gem blocks */
	internal open val imgPErase:List<ResourceImage<*>> = List(pEraseMax) {ResourceImageStr("effects/perase$it")}

	/** Effects for Fireworks */
	internal open val imgHanabi:List<ResourceImage<*>> = List(hanabiMax) {ResourceImageStr("effects/hanabi$it")}

	/** Title Background */
	internal open val imgTitleBG:ResourceImage<*> = ResourceImageStr("title")

	/** Title Logo */
	internal open val imgLogo:ResourceImage<*> = ResourceImageStr("logo")
	internal open val imgLogoSmall:ResourceImage<*> = ResourceImageStr("logo_small")

	/** Menu Background */
	internal open val imgMenuBG:List<ResourceImage<*>> = listOf("menu", "menu_in").map {ResourceImageStr(it)}

	/** プレイ中のBackground */
	internal open val imgPlayBG:List<ResourceImage<*>> by lazy {
		File("$skinDir/graphics/back/").listFiles(filterImg)?.sortedWith(nameSort)?.map {
			ResourceImageStr("back/"+it.nameWithoutExtension)
		} ?: emptyList()
	}
	/** プレイ中のBackground Animation */
	internal open val imgPlayBGA:List<ResourceImage<*>> by lazy {
		File("$skinDir/graphics/back_vis/").listFiles(filterImg)?.sortedWith(nameSort)?.map {
			ResourceImageStr("back_vis/"+it.nameWithoutExtension)
		} ?: emptyList()
	}
	/** BackgroundOfcount */
	val bgMax get() = imgPlayBG.size
	val bgaMax get() = imgPlayBGA.size
	/** BGA Count except rush*/
	val bgaMaxUniq get() = imgPlayBGA.count {!it.name.contains("rush")}

	fun loadImg(back:Boolean, frags:Boolean) {
		log.info("Loading Image from $skinDir")

		/*val nestList:List<Any> = listOf(
			listOf(1), 2, listOf(listOf(3, 4), 5), listOf(listOf(listOf<Int>())), listOf(listOf(listOf(6))), 7,	8,listOf<Int>()
		)
		log.debug(flattenList<Int>(nestList))*/

		// Blocks
		log.debug("${imgNormalBlockList.size} block skins found")
		listOf(imgNormalBlockList, imgSmallBlockList, imgBigBlockList).flatten()
			.forEach {it.load()}

		flattenList<ResourceImage<*>>(
			listOf(
				imgBadges, imgFont, imgFontNano, imgNum, imgGrade, imgFontMedal, imgCursor,
				imgFrame, imgFrameOld, imgFieldBG, imgLine, imgTitleBG, imgLogo, imgLogoSmall,
				imgMenuBG/*, imgPlayBG, imgPlayBGA*/
			)
		).forEach {it.load()}

		if(back) loadBackgroundImages()
		if(frags) loadLineClearEffectImages()
	}
	/** Load background images. */
	internal fun loadBackgroundImages() {
		log.debug("${imgPlayBG.size} backgrounds found")
		imgPlayBG.forEach {it.load()}
		log.debug("${imgPlayBGA.size} SP-backgrounds found")
		imgPlayBGA.forEach {it.load()}
	}

	/** Load line clear effect images. */
	internal fun loadLineClearEffectImages() {
		flattenList<ResourceImage<*>>(listOf(imgBreak, imgPErase, imgHanabi, imgFrags)).forEach {it.load()}
	}

	internal val jingles = setOf("gamelost", "gamewon", *setOf((0..2).map {"excellent$it"}).flatten().toTypedArray())

	internal val soundSet = setOf(
		"cursor", "change", "decide", "cancel", "pause",
		"hold", "initialhold", "holdfail", "move", "movefail",
		"rotate", "wallkick", "initialrotate", "rotfail",
		"harddrop", "softdrop", "step", "lock",
		"erase", "linefall", "linefall0", "linefall1", "cheer", "twist", "twister",
		"combo", "combo_pow", "b2b_start", "b2b_combo", "b2b_end",

		"danger", "dead", "dead_last", "shutter",
		"levelstop", "levelup", "levelup_section",
		"endingstart", "excellent",
		"bravo", "cool", "regret",

		"countdown", "hurryup", "timeout",
		"stageclear", "stagefail", "matchend",
		"gem", "bomb", "square_s", "square_g"
	)+((0..1).flatMap {setOf("start$it", "crowd$it")}+
		(0..2).flatMap {setOf("decide$it", "garbage$it", "erase$it", "firecracker$it")}+
		(0..4).map {"grade$it"}+(0..5).map {"applause$it"}+
		Piece.Shape.names.map {"piece_${it.lowercase()}"}+
		(1..3).map {"medal$it"}+(1..4).map {"line$it"}).toSet()

}
