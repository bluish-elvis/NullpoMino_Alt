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
package mu.nu.nullpo.game.component

/** BackgroundImage state */
@kotlinx.serialization.Serializable class BackgroundStatus {

	/** Background number */
	var bg = 0
//TODO bg:BG_RenderClass

	/** Background fade flag */
	var fadesw = false

	/** Background fade state (false for fade-out, true for fade-in) */
	var fadestat = false

	/** Background fade usage counter */
	var fadecount = 0

	/** Background after fade */
	var fadebg = 0

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
		fadesw = false
		fadestat = false
		fadecount = 0
		fadebg = 0
	}

	/** copy settings from [b] */
	fun replace(b:BackgroundStatus) {
		bg = b.bg
		fadesw = b.fadesw
		fadestat = b.fadestat
		fadecount = b.fadecount
		fadebg = b.fadebg
	}

	/** Update background fade state */
	fun fadeUpdate() {
		if(fadesw)
			if(fadecount<100)
				fadecount += 10
			else if(!fadestat) {
				bg = fadebg
				fadestat = true
				fadecount = 0
			} else {
				fadesw = false
				fadestat = false
				fadecount = 0
			}
	}

	companion object {
	}
}
