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
package mu.nu.nullpo.game.component

/** 音楽の再生状況を管理するクラス */
//@kotlinx.serialization.Serializable
class BGMStatus {
	/** Current BGM */
	var bgm:BGM = BGM.Silent
	var track = 0

	/** 音量 (1f=100%, .5f=50%) */
	var volume:Float = 1f; private set

	/** BGM fadeoutスイッチ */
	var fadeSW:Boolean
		get() = fadeSpd>0f
		set(sw) {
			fadeSpd = if(sw) .01f else 0f
		}

	/** BGM fadeout速度 */
	var fadeSpd = 0f

	/** Constructor */
	constructor() {
		reset()
	}

	/** Copy constructor
	 * @param b Copy source
	 */
	constructor(b:BGMStatus) {
		replace(b)
	}

	/** Reset to defaults */
	fun reset() {
		bgm = BGM.Silent
		volume = 1f
		fadeSW = false
	}

	/** 設定を[b]からコピー */
	fun replace(b:BGMStatus) {
		bgm = b.bgm
		volume = b.volume
		fadeSW = b.fadeSW
	}

	/** BGM fade状態と音量の更新 */
	fun fadeUpdate() {
		if(fadeSW) {
			if(volume>0f) volume -= fadeSpd
			else if(volume<0f) volume = 0f
		} else if(volume!=1f) volume = 1f
	}
}
