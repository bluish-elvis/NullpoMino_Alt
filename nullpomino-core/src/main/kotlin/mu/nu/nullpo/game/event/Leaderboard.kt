/*
 Copyright (c) 2024, NullNoname
 All rights reserved.

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis
 THIS IS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
     * Neither the name of NullNoname nor the names of its
       contributors may be used to endorse or promote products derived from
       this software without specific prior written permission.

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

package mu.nu.nullpo.game.event

import kotlinx.serialization.KSerializer
import mu.nu.nullpo.util.GeneralUtil
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipException

class Leaderboard<T:Comparable<T>> private constructor(val list:MutableList<T>, private val max:Int, private val serializer:KSerializer<List<T>>) {

	constructor(max:Int, serializer:KSerializer<List<T>>, init:(Int)->T):this(MutableList<T>(max, init), max, serializer)
	constructor(max:Int, serializer:KSerializer<List<T>>):this(mutableListOf<T>(), max, serializer)

	val best get() = list.firstOrNull()
	val size get() = list.size
	fun add(e:T) = list.sorted().indexOfFirst {e>=it}.let {pos ->
		if(pos<0&&list.size<max) {
			list.add(e)
			list.size-1
		} else {
			list.add(pos, e)
			while(list.size>max) list.removeLastOrNull()
			pos
		}
	}

	operator fun get(index:Int) = list[index]
	operator fun get(range:IntRange) = list.subList(range.first, range.last)
	operator fun set(index:Int, e:T) = list.set(index, e)
	operator fun iterator() = list.iterator()

	operator fun plusAssign(e:T) {;add(e)
	}

	operator fun minusAssign(e:T) {;list.remove(e)
	}

	fun fill(data:T) {
		while(list.size<max) list.add(data)
		list.fill(data)
	}

	fun clear() = list.clear()
	inline fun forEach(action:(T)->Unit) = list.forEach(action)
	inline fun forEachIndexed(action:(Int, T)->Unit) = list.forEachIndexed(action)
	inline fun <R> map(act:(T)->R) = list.map(act)
	inline fun <R> mapIndexed(act:(Int, T)->R) = list.mapIndexed(act)
	inline fun <R:Any> mapIndexedNotNull(act:(index:Int, T)->R?) = list.mapIndexedNotNull(act)

	var filename = ""; private set
	/** Load from [file].
	 * @return This Properties data you specified, or null if the file doesn't exist.
	 */
	fun load(file:String = filename):List<T>? {
		fun load(f:InputStream) {
			list.clear()
			list.addAll(GeneralUtil.Json.decodeFromString(serializer, f.bufferedReader().use {it.readText()}))
			filename = file
		}
		try {
			GZIPInputStream(FileInputStream(file)).use {load(it)}
		} catch(e:ZipException) {
			FileInputStream(file).use {load(it)}
		} catch(e:FileNotFoundException) {
			log.debug("Not found Leaderboard file: $file")
			return null
		} catch(e:IOException) {
			log.debug("Failed to load Leaderboard file from $file", e)
			return null
		}

		return list
	}

	/** Save to [file].
	 * @return true if success
	 */
	fun save(file:String = filename):Boolean {
		try {
			val repFolder = File(file).parentFile
			if(!repFolder.exists()) if(repFolder.mkdirs()) log.info("Created folder: ${repFolder.name}")
			else log.error("Couldn't create folder at ${repFolder.name}")
			val s = GeneralUtil.Json.encodeToString(serializer, list)
//			log.debug(s)
			GZIPOutputStream(FileOutputStream(file, false)).bufferedWriter().use {it.write(s)}
			log.debug("Saving Leaderboard to $file")
		} catch(e:Exception) {
			log.warn("Failed to save Leaderboard file to $file", e)
			return false
		}
		return true
	}

	companion object {
		private val log = LogManager.getLogger()
		private val code = StandardCharsets.UTF_8
	}
}
