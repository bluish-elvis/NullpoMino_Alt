/*
 Copyright (c) 2010-2024, NullNoname
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
package mu.nu.nullpo.gui.slick

import org.apache.logging.log4j.LogManager
import org.newdawn.slick.Sound

/** Sound effectsマネージャ */
internal class SoundManager
/** Constructor
@param maxClips 登録できるWAVE file のMaximumcount
 */(
	/** 登録できるWAVE file のMaximumcount */
	private val maxClips:Int = 128) {
	/** WAVE file data (Name-> data本体) */
	private val clipMap:MutableMap<String, Sound> = mutableMapOf()
	/** 登録されたWAVE file count */
	private val counter get() = clipMap.size

	/** Load WAVE file
	 * @param name 登録名
	 * @param filename Filename (String）
	 * @return true if successful, false if failed
	 */
	fun load(name:String, filename:String):Boolean {
		if(counter>=maxClips) {
			log.error("No more wav files can be loaded ($maxClips)")
			return false
		}
		try {
			clipMap[name] = Sound(filename)
		} catch(e:Throwable) {
			log.error("Failed to load wav file", e)
			return false
		}

		return true
	}

	/** 再生
	 * @param name 登録名
	 */
	@JvmOverloads
	fun play(name:String, loop:Boolean = false, pitch:Float = 1f, vol:Float = 1f) =
		clipMap[name]?.run {
			if(!loop) play(pitch, vol)
			else if(!playing()) loop(pitch, vol)
		}

	/** 停止
	 * @param name 登録名
	 */
	fun stop(name:String) = clipMap[name]?.run {
		if(playing()) stop()
	}

	companion object {
		/** Log */
		internal val log = LogManager.getLogger()
	}
}
