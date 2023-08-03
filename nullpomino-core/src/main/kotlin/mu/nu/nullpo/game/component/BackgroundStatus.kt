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
package mu.nu.nullpo.game.component

/** BackgroundImage state */
@kotlinx.serialization.Serializable class BackgroundStatus {
	/** Background number */
	var bg = 0
//TODO bg:BG_RenderClass

	var fadeEnabled = true
	/** Background fade flag */
	var fadeSW = false; private set

	/** Background fade state (false for fade-out, true for fade-in) */
	var fadeStat = false; private set

	/** Background fade usage counter */
	var fadeCount = 0; private set

	/** Background after fade */
	var nextBg = 0
		set(value) {
			field = value
			if(!fadeEnabled) bg = value
			else
				if(value!=bg) {
					fadeSW = true
					fadeStat = false
					fadeCount = 0
				}

		}

	/** Default constructor */
	constructor() {
		reset()
	}

	/** Copy constructor settings from [b] */
	constructor(b:BackgroundStatus) {
		replace(b)
	}

	/** Reset to defaults */
	fun reset() {
		bg = 0
		fadeSW = false
		fadeStat = false
		fadeCount = 0
		nextBg = 0
	}

	/** copy settings from [b] */
	fun replace(b:BackgroundStatus) {
		bg = b.bg
		fadeSW = b.fadeSW
		fadeStat = b.fadeStat
		fadeCount = b.fadeCount
		nextBg = b.nextBg
	}

	/** Update background fade state */
	fun fadeUpdate() {
		if(fadeSW)
			if(fadeCount<100) fadeCount += 2
			else if(!fadeStat) {
				bg = nextBg
				fadeStat = true
				fadeCount = 0
			} else fadeFinish()
	}

	fun fadeFinish() {
		bg = nextBg
		fadeSW = false
		fadeStat = false
		fadeCount = 0
	}

}
