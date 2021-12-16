/*
 * Copyright (c) 2010-2021, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
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
package mu.nu.nullpo.gui.net

import org.apache.logging.log4j.LogManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.LinkedList
import java.util.regex.Pattern

/** 新Versionチェッカー */
class UpdateChecker:Runnable {

	/* 更新 check スレッドの処理 */
	override fun run() {
		// 開始
		status = STATUS_LOADING
		listeners.forEach {it.onUpdateCheckerStart()}

		// 更新 check
		status = if(checkUpdate()) STATUS_COMPLETE else STATUS_ERROR

		// 終了
		listeners.forEach {it.onUpdateCheckerEnd(status)}
	}

	companion object {
		/** Log */
		internal val log = LogManager.getLogger()

		/** default のXMLのURL */
		/* TODO: Find an actual place to put the NullpoUpdate.xml file, possible
* on github pages. For now, just use the v7.5.0 file as a classpath
* resource. */
		val DEFAULT_XML_URL = UpdateChecker::class.java.getResource("NullpoUpdate.xml")?.path ?: ""

		/** 状態の定数 */
		const val STATUS_INACTIVE = 0
		const val STATUS_LOADING = 1
		const val STATUS_ERROR = 2
		const val STATUS_COMPLETE = 3

		/** Current 状態 */
		/** Current 状態を取得
		 * @return Current 状態
		 */
		@Volatile
		var status = 0
			private set

		/** event リスナー */
		private var listeners:LinkedList<UpdateCheckerListener> = LinkedList()

		/** アップデート情報が書かれたXMLのURL */
		/** XMLのURLを取得
		 * @return XMLのURL
		 */
		var strURLofXML = ""
			private set

		/** 最新版のVersion number */
		/** 最新版のVersion number(未整形)を取得(7_0_0_0など)
		 * @return 最新版のVersion number(未整形)
		 */
		var strLatestVersion = ""
			private set

		/** リリース日 */
		/** 最新版がリリースされた日を取得
		 * @return 最新版がリリースされた日
		 */
		var strReleaseDate = ""
			private set

		/** ダウンロードURL */
		/** 最新版のダウンロード先URLを取得
		 * @return 最新版のダウンロード先URL
		 */
		var strDownloadURL = ""
			private set

		/** Installer for Windows URL */
		/** Get the URL of Installer (*.exe) for Windows
		 * @return URL of Installer (*.exe) for Windows
		 */
		var strWindowsInstallerURL = ""
			private set

		/** 更新 check 用スレッド */
		private var thread:Thread? = null

		/** XMLをダウンロードしてVersion numberなどを取得
		 * @return true if successful
		 */
		private fun checkUpdate():Boolean {
			try {
				val url = URL(strURLofXML)
				val httpCon = url.openConnection()
				val httpIn = BufferedReader(InputStreamReader(httpCon.getInputStream()))

				httpIn.readLines().forEach {
					var pat = Pattern.compile("<Version>.*</Version>")
					var matcher = pat.matcher(it)
					if(matcher.find()) {
						var tempStr = matcher.group()
						tempStr = tempStr.replace("<Version>", "")
						tempStr = tempStr.replace("</Version>", "")
						strLatestVersion = tempStr
						log.debug("Latest Version:$strLatestVersion")
					}

					pat = Pattern.compile("<Date>.*</Date>")
					matcher = pat.matcher(it)
					if(matcher.find()) {
						var tempStr = matcher.group()
						tempStr = tempStr.replace("<Date>", "")
						tempStr = tempStr.replace("</Date>", "")
						strReleaseDate = tempStr
						log.debug("Release Date:$strReleaseDate")
					}

					pat = Pattern.compile("<DownloadURL>.*</DownloadURL>")
					matcher = pat.matcher(it)
					if(matcher.find()) {
						var tempStr = matcher.group()
						tempStr = tempStr.replace("<DownloadURL>", "")
						tempStr = tempStr.replace("</DownloadURL>", "")
						strDownloadURL = tempStr
						log.debug("Download URL:$strDownloadURL")
					}

					pat = Pattern.compile("<WindowsInstallerURL>.*</WindowsInstallerURL>")
					matcher = pat.matcher(it)
					if(matcher.find()) {
						var tempStr = matcher.group()
						tempStr = tempStr.replace("<WindowsInstallerURL>", "")
						tempStr = tempStr.replace("</WindowsInstallerURL>", "")
						strWindowsInstallerURL = tempStr
						log.debug("Windows Installer URL:$strWindowsInstallerURL")
					}
				}

				httpIn.close()
			} catch(e:Exception) {
				log.error("Failed to get latest version data", e)
				return false
			}

			return true
		}

		/** 最新版のメジャーVersionを取得
		 * @return 最新版のメジャーVersion(float型)
		 */
		val latestMajorVersionAsFloat:Float
			get() {
				var resultVersion = 0f
				if(strLatestVersion.isNotEmpty()) {
					val strDot = if(strLatestVersion.contains("_")) "_" else "."
					val strSplit = strLatestVersion.split(strDot.toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()

					if(strSplit.size>=2) {
						val strTemp = "${strSplit[0]}.${strSplit[1]}"
						try {
							resultVersion = strTemp.toFloat()
						} catch(e:NumberFormatException) {
						}

					}
				}
				return resultVersion
			}

		/** 最新版のマイナーVersionを取得
		 * @return 最新版のマイナーVersion(int型)
		 */
		val latestMinorVersionAsInt:Int
			get() {
				var resultVersion = 0
				if(strLatestVersion.isNotEmpty()) {
					val strDot = if(strLatestVersion.contains("_")) "_" else "."
					val strSplit = strLatestVersion.split(strDot.toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()

					if(strSplit.isNotEmpty()) {
						val strTemp = strSplit[strSplit.size-1]
						try {
							resultVersion = strTemp.toInt()
						} catch(_:NumberFormatException) {
						}

					}
				}
				return resultVersion
			}

		/** 最新版のVersion numberのString型表現を取得
		 * @return 最新版のVersion numberのString型表現("7.0.0"など)
		 */
		val latestVersionFullString:String
			get() = "$latestMajorVersionAsFloat.$latestMinorVersionAsInt"

		/** Current versionよりも最新版のVersionの方が新しいか判定
		 * @param nowMajor Current メジャーVersion
		 * @param nowMinor Current マイナーVersion
		 * @return 最新版の方が新しいとtrue
		 */
		fun isNewVersionAvailable(nowMajor:Float, nowMinor:Int):Boolean {
			if(!isCompleted) return false

			val latestMajor = latestMajorVersionAsFloat
			val latestMinor = latestMinorVersionAsInt

			return if(latestMajor>nowMajor) true else latestMajor==nowMajor&&latestMinor>nowMinor

		}

		/** Version check
		 * @param strURL 最新版の情報が入ったXMLファイルのURL(nullまたは空文字列にすると default 値を使う)
		 */
		fun startCheckForUpdates(strURL:String?) {
			strURLofXML = if(strURL==null||strURL.isEmpty()) DEFAULT_XML_URL else strURL
			thread = Thread(UpdateChecker()).apply {
				isDaemon = true
				start()
			}
		}

		/** @return スレッドが動作中(読み込み中)ならtrue
		 */
		val isRunning:Boolean
			get() = status==STATUS_LOADING

		/** @return 読み込み完了したらtrue
		 */
		val isCompleted:Boolean
			get() = status==STATUS_COMPLETE

		/** event リスナーを追加(もう追加されていると何も起こりません)
		 * @param l 追加する event リスナー
		 */
		fun addListener(l:UpdateCheckerListener) {
			if(listeners.contains(l)) return
			listeners.add(l)
		}

		/** event リスナーを削除
		 * @param l 削除する event リスナー
		 * @return 削除されたらtrue, 最初から登録されていなかったらfalse
		 */
		fun removeListener(l:UpdateCheckerListener):Boolean = listeners.remove(l)
	}
}
