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
package mu.nu.nullpo.util

import mu.nu.nullpo.game.subsystem.mode.GameMode
import org.apache.logging.log4j.LogManager
import java.io.BufferedReader
import java.io.IOException

/** Mode 管理クラス */
class ModeManager {

	/** Mode の動的配列 */
	val list:MutableList<GameMode> = mutableListOf()

	/** Mode のcountを取得(通常+ネットプレイ全部)
	 * @return Modeのcount(通常 + ネットプレイ全部)
	 */
	val size:Int
		get() = list.size

	/** 読み込まれている全てのMode nameを取得
	 * @return Mode nameの配列
	 */
	val allModeNames:List<String>
		get() = List(size) {getName(it)}

	/** Constructor */
	constructor()

	/** Copy constructor
	 * @param m Copy source
	 */
	constructor(m:ModeManager) {
		list.addAll(m.list)
	}

	/** Mode のcountを取得
	 * @param netplay falseなら通常Mode だけ, When true,ネットプレイ用Mode だけcount
	 * @return Modeのcount
	 */
	fun getNumberOfModes(netplay:Boolean):Int =
		list.count {it.isOnlineMode==netplay}

	/** 読み込まれているMode nameを取得
	 * @param netplay falseなら通常Mode だけ, When true,ネットプレイ用Mode だけ取得
	 * @return Mode nameの配列
	 */
	fun getModeNames(netplay:Boolean):List<String> =
		list.filter {it.isOnlineMode==netplay}.map {it.name}

	/** Mode nameを取得
	 * @param id ModeID
	 * @return Mode name (idが不正なら「*INVALID MODE*」）
	 */
	fun getName(id:Int):String = try {
		list[id].name
	} catch(e:Exception) {
		"*INVALID MODE*"
	}

	/** Mode Identifier Nameを取得
	 * @param id Mode No.
	 * @return Mode name (idが不正なら「*INVALID MODE*」）
	 */
	fun getID(id:Int):String = try {
		list[id].id
	} catch(e:Exception) {
		"*INVALID MODE*"
	}

	/** Mode name/Mode IDからID numberを取得
	 * @param name Mode name
	 * @return ModeNumber (見つからない場合は-1）
	 */
	fun getNum(name:String?):Int {
		if(name==null) return -1

		for(i in list.indices)
			if(name.compareTo(list[i].name)==0||name.compareTo(list[i].id)==0)
				return i

		return -1
	}

	/** Mode オブジェクトを取得
	 * @param id Mode No.
	 * @return Modeオブジェクト (idが不正ならnull）
	 */
	operator fun get(id:Int):GameMode? = try {
		list[id]
	} catch(e:Exception) {
		null
	}
	@Deprecated("operator get", ReplaceWith("get(id)"))
	fun getMode(id:Int):GameMode? = get(id)

	/** Mode オブジェクトを取得
	 * @param name Mode name
	 * @return Modeオブジェクト (見つからないならnull）
	 */
	operator fun get(name:String):GameMode? = try {
		list[getNum(name)]
	} catch(e:Exception) {
		null
	}

	@Deprecated("operator get", ReplaceWith("get(id)"))
	fun getMode(name:String):GameMode? = get(name)

	/** Property fileに書かれた一覧からゲームMode を読み込み
	 * @param prop Property file
	 */
	fun loadGameModes(prop:CustomProperties) {
		var count = 0

		while(true) {
			// クラス名を読み込み
			val name = prop.getProperty("$count", null) ?: return

			val modeClass:Class<*>
			val modeObject:GameMode

			try {
				modeClass = Class.forName(name)
				modeObject = modeClass.getDeclaredConstructor().newInstance() as GameMode
				list.add(modeObject)
			} catch(e:ClassNotFoundException) {
				log.warn("Mode class $name not found", e)
			} catch(e:Exception) {
				log.warn("Mode class $name load failed", e)
			}

			count++
		}
	}

	/** テキストファイルに書かれた一覧からゲームMode を読み込み
	 * @param bf テキストファイルを読み込んだBufferedReader
	 */
	fun loadGameModes(bf:BufferedReader) {
		while(true) {
			// クラス名を読み込み
			val name:String?
			try {
				name = bf.readLine()
			} catch(e:IOException) {
				log.warn("IOException on readLine()", e)
				return
			}

			if(name==null) return
			if(name.isEmpty()) return

			if(!name.startsWith("#")) {
				val modeClass:Class<*>
				val modeObject:GameMode
				try {
					modeClass = Class.forName(name)
					modeObject = modeClass.getDeclaredConstructor().newInstance() as GameMode
					list.add(modeObject)
				} catch(e:ClassNotFoundException) {
					log.warn("Mode class $name not found", e)
				} catch(e:Exception) {
					log.warn("Mode class $name load failed", e)
				}

			}
		}
	}

	companion object {
		/** Log */
		private val log = LogManager.getLogger()
	}
}
