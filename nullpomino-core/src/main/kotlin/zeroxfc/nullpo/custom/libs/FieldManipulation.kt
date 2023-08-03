/*
 Copyright (c) 2023,
 This library class was created by 0xFC963F18DC21 / Shots243
 It is part of an extension library for the game NullpoMino (copyright 2010-2023)

 Kotlin converted and modified by Venom=Nhelv

 Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.

 THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

 Original Repository: https://github.com/Shots243/ModePile

 When using this library in a mode / library pack of your own, the following
 conditions must be satisfied:
     - This license must remain visible at the top of the document, unmodified.
     - You are allowed to use this library for any modding purpose.
         - If this is the case, the Library Creator must be credited somewhere.
             - Source comments only are fine, but in a README is recommended.
     - Modification of this library is allowed, but only in the condition that a
       pull request is made to merge the changes to the repository.

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
package zeroxfc.nullpo.custom.libs

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import java.util.Random

/**
 * Deletes blocks with any of the colors in the given array.
 *
 * @param colors Colors to erase
 * @return int[]; Amount of blocks of each color erased
 */
fun Field.delColors(colors:Array<Block.COLOR>, dir:Boolean = true) = colors.map {delColor(it, dir)}
/**
 * Deletes all blocks of a certain color on a field.
 *
 * @param color Color to erase
 * @param dir if false, block marked will be erased but not now
 * @return int; Amount of blocks erased
 */
fun Field.delColor(color:Block.COLOR, dir:Boolean = true):Int =
	findBlocks {it.color==color}.entries.sumOf {(y, row) ->
		row.entries.mapNotNull {(x, b) ->
			if(dir) delBlock(x, y) else b.apply {setAttribute(true, Block.ATTRIBUTE.ERASE)}
		}.size
	}

/**
 * Erases a mino in every filled line on a field. Displays the blockbreak effect.
 *
 * @param receiver EventReceiver instance to display blockbreak effect on.
 * @param engine   Current GameEngine
 * @param random   Random instance to use
 */
fun Field.shotgunField(random:Random, receiver:EventReceiver? = null, engine:GameEngine? = null) {
	(hiddenHeight*-1..<height).associateWith {y ->
		getRow(y).mapIndexedNotNull {x, b -> b?.let {x to b}}.let {mapOf(it[random.nextInt(it.size)])}
	}.filter {it.value.isNotEmpty()}.let {all ->
		delBlocks(all).let {engine?.let {e -> receiver?.blockBreak(e, it)}}
	}
}
/**
 *
 * Randomises the column order in a field.
 * Requires a random seed. For consistency, try `(engine.randSeed + engine.statistics.time)`.
 *
 * @param seed  Random seed
 */
fun Field.shuffleColumns(seed:Long) = shuffleColumns(Random(seed))

/**
 *
 * Randomises the column order in a field.
 * Requires a pre-instantiated `ArrayRandomizer` instance.
 *
 * @param randomizer Randomizer instance
 */
fun Field.shuffleColumns(randomizer:Random) {
	val columns = List(width) {it}.shuffled(randomizer)
	val nf = Field(this)
	for(i in columns.indices) for(y in -1*nf.hiddenHeight..<nf.height) nf.getBlock(i, y)?.replace(getBlock(columns[i], y))
	replace(nf)
}
/**
 *
 * Randomises the row order in a field.
 * Requires a random seed. For consistency, try `(engine.randSeed + engine.statistics.time)`.
 *
 * @param seed       Random seed
 * @param highestRow Highest row randomised (0 = top visible), recommended / default is >= 2 (inclusive)
 * @param lowestRow  Lowest row randomised (inclusive)
 */
fun Field.shuffleRows(seed:Long, highestRow:Int = 2, lowestRow:Int = height-1) =
	shuffleRows(Random(seed), highestRow, lowestRow)
/**
 *
 * Randomises the row order in a field.
 * Requires a pre-instantiated `ArrayRandomizer` instance.
 *
 * @param randomizer Randomizer instance
 * @param highestRow      Highest row randomised (0 = top visible), recommended / default is >= 2 (inclusive)
 * @param lowestRow       Lowest row randomised (inclusive)
 */
fun Field.shuffleRows(randomizer:Random, highestRow:Int = 2, lowestRow:Int = height-1) {
	if(highestRow<hiddenHeight*-1||highestRow>=hiddenHeight) return
	val oldRows = List(lowestRow-highestRow+1) {highestRow+it}
	val rows = oldRows.shuffled(randomizer)
	val nf = Field(this)
	for(i in rows.indices) for(x in 0..<nf.width) nf.getBlock(x, oldRows[i])?.replace(getBlock(x, rows[i]))
	replace(nf)
}
/**
 *
 * Compares two fields and calculates the percentage maps between them. Useful for "build shape modes".
 *
 *
 *
 * NOTE: BOTH FIELDS MUST HAVE THE SAME DIMENSIONS IF USING `exact` MATCHING MODE!
 *
 * NOTE: If using non-`exact` matching, `Field b` is used as the comparator where the percentage reflects how much of `a` is the same as `b`.
 *
 * @param b           A field to compare field with.
 * @param exact       Exact matching (also takes into account absolute block positions)?
 * @param colorMatch Exact color matching (halves match value of non-color matches)?
 * @return A double that denotes the proportion of match between the fields (0 <= value <= 1).
 */
@JvmOverloads fun Field.compare(b:Field, exact:Boolean = false, colorMatch:Boolean = false):Double {
	val a = this
	return if(exact) {
		/*
					 * This path attempts to match fields exactly.
					 * This means both fields must have the same dimensions.
					 */
		if(a.width!=b.width) return 0.0
		if(a.hiddenHeight+a.hiddenHeight!=b.hiddenHeight+b.hiddenHeight) return 0.0
		val total = 2*b.howManyBlocks
		var current = 0
		for(y in -1*a.hiddenHeight..<a.height) {
			for(x in 0..<a.width) {
				val blkA = a.getBlock(x, y)
				val blkB = b.getBlock(x, y)
				if(blkA!=null&&blkB!=null) {
					if(colorMatch) {
						if(!blkA.isEmpty||!blkB.isEmpty) {
							if(!blkA.isEmpty&&blkB.isEmpty) current -= 6
							else if(!blkA.isEmpty&&!blkB.isEmpty) current += (if(blkA.color===blkB.color) 2 else 1)
						}
					} else if(!blkA.isEmpty||!blkB.isEmpty) {
						if(!blkA.isEmpty&&blkB.isEmpty) current -= 6 else if(!blkA.isEmpty&&!blkB.isEmpty) current += 2
					}
				}
			}
		}
		maxOf(0.0, current.toDouble()/total.toDouble())
	} else {
		val finalResult = 0.0
		var areaA = 0
		var areaB = 0

		// Stage 1: area filled
		for(y in -1*a.hiddenHeight..<a.height) {
			for(x in 0..<a.width) {
				val blk = a.getBlock(x, y)
				if(blk?.color!=null) areaA++
			}
		}
		for(y in -1*b.hiddenHeight..<b.height) {
			for(x in 0..<b.width) {
				val blk = b.getBlock(x, y)
				if(blk!!.color!=null) areaB++
			}
		}

		// Stage 2: blocks
		val resA = a.opposingCornerCoords
		val topLeftA = resA[0]
		// bottomRightA = resA[1];
		val resB = b.opposingCornerCoords
		val topLeftB = resB[0]
		// bottomRightB = resB[1];
		val bboxSizeA = a.opposingCornerBoxSize
		val bboxSizeB = b.opposingCornerBoxSize
		// log.debug("%d %d | %d %d".format(bboxSizeA[0], bboxSizeA[1], bboxSizeB[0], bboxSizeB[1]));
		val aA = bboxSizeA[0]*bboxSizeA[1]
		val aB = bboxSizeB[0]*bboxSizeB[1]
		var total = 0
		// int excess = 0;
		return if(bboxSizeA[0]==bboxSizeB[0]&&bboxSizeA[1]==bboxSizeB[1]) {
			for(y in 0..<bboxSizeB[1]) {
				for(x in 0..<bboxSizeB[0]) {
					val blkA = a.getBlock(topLeftA[0]+x, topLeftA[1]+y)
					val blkB = b.getBlock(topLeftB[0]+x, topLeftB[1]+y)
					if(blkA!=null&&blkB!=null) {
						if(colorMatch) {
							if(blkA.isEmpty&&blkB.isEmpty) {
								total += 2
							} else {
								if(!blkA.isEmpty&&blkB.isEmpty) {
									total -= 6
								} else if(!blkA.isEmpty&&!blkB.isEmpty) {
									total += if(blkA.color===blkB.color) 2 else 1
								}
							}
						} else {
							if(blkA.isEmpty&&blkB.isEmpty) {
								total += 2
								// log.debug("(" + x + ", " + y + ") " + "EMPTY MATCH");
							} else {
								if(!blkA.isEmpty&&blkB.isEmpty) {
									total -= 6
									// log.debug("(" + x + ", " + y + ") " + "EXCESS IN A");
								} else if(!blkA.isEmpty&&!blkB.isEmpty) {
									total += 2
									// log.debug("(" + x + ", " + y + ") " + "FULL MATCH");
								} //  else {
								//total -= 1;
								// log.debug("(" + x + ", " + y + ") " + "UNFILLED");
								// }
							}
						}
					}
				}
			}
			var res3 = total/(2.0*aB)
			if(res3<0) res3 = 0.0

			// log.debug("TOTAL: %d, MAX: %d, PERCENT: %.2f".format(total, 2 * aB, res3 * 100));
			res3
		} else {
			// Use the lowest common multiple + nearest neighbour scaling check method.
			val lcmWidth = lcm(bboxSizeA[0], bboxSizeB[0])
			val lcmHeight = lcm(bboxSizeA[1], bboxSizeB[1])
			val multiplierWidthA = lcmWidth/bboxSizeA[0]
			val multiplierWidthB = lcmWidth/bboxSizeB[0]
			val multiplierHeightA = lcmHeight/bboxSizeA[1]
			val multiplierHeightB = lcmHeight/bboxSizeB[1]
			val maxArea = lcmHeight*lcmWidth*2
			var closenessAverageH = bboxSizeA[0].toDouble()/bboxSizeB[0]
			if(closenessAverageH>1) closenessAverageH = 1-(closenessAverageH-1)
			if(closenessAverageH<0) closenessAverageH = 0.0
			var closenessAverageV = bboxSizeA[1].toDouble()/bboxSizeB[1]
			if(closenessAverageV>1) closenessAverageV = 1-(closenessAverageV-1)
			if(closenessAverageV<0) closenessAverageV = 0.0
			val closenessAverage = (closenessAverageH+closenessAverageV)/2

			//StringBuilder matchArr = new StringBuilder("MATCH ARRAY:\n");
			for(y in 0..<lcmHeight) {
				for(x in 0..<lcmWidth) {
					val v1:Int = a.getBlock(topLeftA[0]+x/multiplierWidthA, topLeftA[1]+y/multiplierHeightA)!!.drawColor
					val v2:Int = b.getBlock(topLeftB[0]+x/multiplierWidthB, topLeftB[1]+y/multiplierHeightB)!!.drawColor
					if(colorMatch) {
						if(v1<=0&&v2<=0) {
							total += 2
							//matchArr.append(" 2");
						} else {
							if(v1>0&&v2<=0) {
								total -= 6
								//matchArr.append("-8");
							} else if(v1>0) {
								total += if(v1==v2) {
									2
									//matchArr.append(" 2");
								} else {
									1
									//matchArr.append(" 1");
								}
							} // else {
							// total -= 1;
							//matchArr.append("-4");
							// }
						}
					} else {
						if(v1<=0&&v2<=0) {
							total += 2
							//matchArr.append(" 2");
						} else {
							if(v1>0&&v2<=0) {
								total -= 6
								//matchArr.append("-8");
							} else if(v1>0) {
								total += 2
								//matchArr.append(" 2");
							} // else {
							// total -= 1;
							//matchArr.append("-4");
							// }
						}
					}
				}
				//matchArr.append("\n");
			}

//					StringBuilder j = new StringBuilder("FIELD A:\n");
//					for (int y = 0; y < lcmHeight; y++) {
//						for (int x = 0; x < lcmWidth; x++) {
//							j.append(a.getBlock(topLeftA[0] + (x / multiplierWidthA), topLeftA[1] + (y / multiplierHeightA)).color);
//						}
//						j.append("\n");
//					}
//
//					StringBuilder k = new StringBuilder("FIELD B:\n");
//					for (int y = 0; y < lcmHeight; y++) {
//						for (int x = 0; x < lcmWidth; x++) {
//							k.append(b.getBlock(topLeftB[0] + (x / multiplierWidthB), topLeftB[1] + (y / multiplierHeightB)).color);
//						}
//						k.append("\n");
//					}
//
//					log.debug(j.toString());
//					log.debug(k.toString());
//					log.debug(matchArr.toString());
			var res3 = total.toDouble()/maxArea.toDouble()*closenessAverage
			if(res3<0) res3 = 0.0

			// log.debug("TOTAL: %d, MAX: %d, CLOSENESS: %.2f, PERCENT: %.2f".format(total, maxArea, closenessAverage, res3 * 100));
			res3
		}
		finalResult
	}
}
/*
	 * NOTE: Here's hoping this doesn't slow the game down by much.
	 *
	 * Euler's method?
	 */
// Recursive method to return gcd of a and b
fun gcd(a:Int, b:Int):Int = if(a==0) b else gcd(b%a, a)
// Method to return LCM of two numbers
fun lcm(a:Int, b:Int):Int = a*b/gcd(a, b)
/**
 * Gets the number of empty blocks inside the field.
 *
 * @return Number of empty spaces inside (including in hidden height and clear queued line)
 */
val Field.getNumberOfEmptySpaces:Int
	get() = (hiddenHeight*-1..<heightWithoutHurryupFloor).sumOf {
		getRow(it).count {b -> b?.isEmpty ?: true||getLineFlag(it)}
	}
/**
 * Gets the coordinates of the top-left and the bottom-right of the smallest bounding square that covers all blocks in the field.
 *
 * @return int[2][2] result: result[0] = top left, result[1] = bottom right. result[i][0] = x, result[i][1] = y.
 */
val Field.opposingCornerCoords
	get() = listOf(listOf(getLeftMostColumn, highestBlockY), listOf(getRightMostColumn, getBottomMostRow))
/**
 * Gets the size of the smallest bounding box that covers all blocks in the field.
 *
 * @return int[] results: results[0] = x, results[1] = y.
 */
val Field.opposingCornerBoxSize
	get() = opposingCornerCoords.let {bbox ->
		val i = bbox[1][0]-bbox[0][0]+1
		val j = bbox[1][1]-bbox[0][1]+1
		if(i<=0||j<=0) emptyList()
		else listOf(i, j)
	}
/**
 * Gets the x coordinate of the left-most filled column a field.
 *
 * @return int; x coordinate
 */
val Field.getLeftMostColumn:Int
	get() = (0..<width).firstOrNull {x ->
		(-1*hiddenHeight..<height).any {y -> !getBlockEmpty(x, y)}
	} ?: (width-1)
/**
 * Gets the x coordinate of the right-most filled column a field.
 *
 * @return int; x coordinate
 */
val Field.getRightMostColumn:Int
	get() = (width-1 downTo 0).firstOrNull {x ->
		(-1*hiddenHeight..<height).any {y -> !getBlockEmpty(x, y)}
	} ?: 0
/**
 * Gets the y coordinate of the bottom-most filled row in a field.
 *
 * @return int; y coordinate
 */
val Field.getBottomMostRow:Int
	get() = (height-1 downTo -1*hiddenHeight).firstOrNull {y ->
		(0..<width).any {x -> !getBlockEmpty(x, y)}
	} ?: (hiddenHeight*-1)
