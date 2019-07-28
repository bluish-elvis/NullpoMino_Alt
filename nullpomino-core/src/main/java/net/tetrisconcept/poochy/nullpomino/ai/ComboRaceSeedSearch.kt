package net.tetrisconcept.poochy.nullpomino.ai

import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.ai.DummyAI
import mu.nu.nullpo.game.subsystem.wallkick.StandardWallkick
import net.omegaboshi.nullpomino.game.subsystem.randomizer.BagNoSZORandomizer
import org.apache.log4j.Logger

open class ComboRaceSeedSearch:DummyAI() {

	private class Transition
	constructor(val x:Int, val rt:Int, val rtSub:Int = 0, val newField:Int, val next:Transition) {
		constructor(x:Int,rt:Int,newField:Int,next:Transition):this(x,rt,0,newField,next)
	}

	companion object {
		/** Log (Apache log4j) */
		internal var log = Logger.getLogger(ComboRaceSeedSearch::class.java)

		/**
		 * List of field state codes which are possible to sustain a stable
		 * combo
		 */
		private val FIELDS =
			intArrayOf(0x7, 0xB, 0xD, 0xE, 0x13, 0x15, 0x16, 0x19, 0x1A, 0x1C, 0x23, 0x29, 0x31, 0x32, 0x49, 0x4C, 0x61, 0x68, 0x83, 0x85, 0x86, 0x89, 0x8A, 0x8C, 0xC4, 0xC8, 0x111, 0x888)

		/** Number of pieces to think ahead */
		private const val MAX_THINK_DEPTH = 6
		/** Length of piece queue */
		private const val QUEUE_SIZE = 1400

		private val stateScores =
			intArrayOf(6, 7, 7, 6, 8, 3, 2, 9, 3, 4, 3, 1, 8, 4, 1, 3, 1, 1, 4, 3, 9, 2, 3, 8, 4, 8, 3, 3)

		private val pieceScores = intArrayOf(28, 18, 10, 9, 18, 18, 9)
		private var moves:Array<Array<Transition>> = emptyArray()
		private var nextQueueIDs:IntArray = IntArray(MAX_THINK_DEPTH)
		private var queue:IntArray = IntArray(QUEUE_SIZE)
		private var bestHold:Boolean = false
		private var bestPts:Int = 0
		private var bestNext:Int = 0

		@JvmStatic fun main(args:Array<String>) {
			//long start = System.currentTimeMillis();
			createTables()
			var bestSeed = 0L
			var bestResult = 0
			var result:Int
			var pos:Int
			var fld:Int
			var holdID:Int
			nextQueueIDs = IntArray(MAX_THINK_DEPTH)
			val nextPieceEnable = BooleanArray(Piece.PIECE_COUNT)
			for(i in 0 until Piece.PIECE_STANDARD_COUNT)
				nextPieceEnable[i] = true
			var rand = BagNoSZORandomizer()
			rand.setPieceEnable(nextPieceEnable)

			for(seed in 0L until java.lang.Long.MAX_VALUE) {
				fld = 0x13
				rand = BagNoSZORandomizer()
				rand.setState(nextPieceEnable, seed)
				queue = IntArray(QUEUE_SIZE) {rand.next()}
				result = 0
				pos = 0
				holdID = -1
				while(true) {
					thinkBestPosition(fld, pos, holdID)
					if(bestNext==-1) break
					fld = bestNext
					if(bestHold)
						if(holdID==-1) {
							holdID = queue[pos%queue.size]
							pos++
						} else holdID = queue[pos%queue.size]
					pos++
					result++
					if(result>8L*QUEUE_SIZE.toLong()*FIELDS.size.toLong()) {
						println("Endless loop found! Seed = "+seed.toString(16))
						//long end = System.currentTimeMillis();
						//System.out.println("Runtime: " + (end - start) + "ms");
						break
					}
				}
				if(result>bestResult) {
					bestSeed = seed
					bestResult = result
					println("New best result: seed = "+bestSeed.toString(16)
						+", result = "+bestResult)
				}
			}
		}

		/** Search for the best choice */
		private fun thinkBestPosition(state:Int, nextIndex:Int, holdID:Int) {
			var nextIndex = nextIndex
			if(state<0) return

			bestPts = Integer.MIN_VALUE
			bestNext = -1

			val nowID = queue[nextIndex%queue.size]
			nextIndex++
			for(i in nextQueueIDs.indices)
				nextQueueIDs[i] = queue[(nextIndex+i)%queue.size]

			var t:Transition? = moves[state][nowID]

			while(t!=null) {
				val pts = thinkMain(t.newField, holdID, 0)

				if(pts>bestPts) {
					bestPts = pts
					bestNext = t.newField
					bestHold = false
				}

				t = t.next
			}
			if(holdID!=nowID) {
				t = moves[state][if(holdID==-1) nextQueueIDs[0] else holdID]

				while(t!=null) {
					val pts = thinkMain(t.newField, nowID, if(holdID==-1) 1 else 0)

					if(pts>bestPts) {
						bestPts = pts
						bestNext = t.newField
						bestHold = true
					}

					t = t.next
				}
			}

			//System.out.println("X:" + bestX + " Y:" + bestY + " R:" + bestRt + " H:" + bestHold + " Pts:" + bestPts);
		}

		/**
		 * Think routine
		 * @param state Think state
		 * @param holdID Hold piece ID
		 * @param depth Search depth
		 * @return Evaluation score
		 */
		private fun thinkMain(state:Int, holdID:Int, depth:Int):Int {
			if(state==-1) return 0
			if(depth==nextQueueIDs.size) {
				var result = stateScores[state]*100
				if(holdID==Piece.PIECE_I)
					result += 1000
				else if(holdID>=0&&holdID<pieceScores.size) result += pieceScores[holdID]*100/28
				return result
			}

			var bestPts = 0
			var t:Transition? = moves[state][nextQueueIDs[depth]]

			while(t!=null) {
				bestPts = maxOf(bestPts, thinkMain(t.newField, holdID, depth+1)+1000)
				t = t.next
			}

			if(holdID==-1)
				bestPts = maxOf(bestPts, thinkMain(state, nextQueueIDs[depth], depth+1))
			else {
				t = moves[state][holdID]
				while(t!=null) {
					bestPts = maxOf(bestPts, thinkMain(t.newField, nextQueueIDs[depth], depth+1)+1000)
					t = t.next
				}
			}

			return bestPts
		}

		fun checkOffset(p:Piece, engine:GameEngine):Piece {
			val result = Piece(p)
			result.big = engine.big
			if(!p.offsetApplied) result.applyOffsetArray(engine.ruleopt.pieceOffsetX[p.id], engine.ruleopt.pieceOffsetY[p.id])
			return result
		}

		/** Constructs the moves table if necessary. */
		private fun createTables() {
			if(moves.isNotEmpty()) return

			val wallkick = StandardWallkick()

			moves = Array(FIELDS.size) {emptyArray<Transition>()}

			val fldEmpty = Field(4, Field.DEFAULT_HEIGHT, Field.DEFAULT_HIDDEN_HEIGHT)
			val fldBackup = Field(fldEmpty)
			val fldTemp = Field(fldEmpty)

			val pieces = Array(7) {
				Piece(it).apply {setColor(1)}
			}

			var count = 0

			for(i in FIELDS.indices) {
				fldBackup.copy(fldEmpty)
				var code = FIELDS[i]

				for(y in Field.DEFAULT_HEIGHT-1 downTo Field.DEFAULT_HEIGHT-4+1)
					for(x in 3 downTo 0) {
						if(code.and(0b1)==0b1) fldBackup.setBlockColor(x, y, 1)
						code = code.shr(0b1)
					}

				for(p in 0..6) pieces[p].let {piece ->
					val tempX = -1+(fldBackup.width-piece.width+1)/2
					for(rt in 0 until Piece.DIRECTION_COUNT) {
						val minX = piece.getMostMovableLeft(tempX, 0, rt, fldBackup)
						val maxX = piece.getMostMovableRight(tempX, 0, rt, fldBackup)

						for(x in minX..maxX) {
							val y = piece.getBottom(x, 0, rt, fldBackup)
							if(p==Piece.PIECE_L||p==Piece.PIECE_T||p==Piece.PIECE_J||rt<2) {
								fldTemp.copy(fldBackup)
								piece.placeToField(x, y, rt, fldTemp)
								if(fldTemp.checkLine()==1) {
									fldTemp.clearLine()
									fldTemp.downFloatingBlocks()
									val index = fieldToIndex(fldTemp, 0)
									if(index>=0) {
										moves[i][p] = Transition(x, rt, index, moves[i][p])
										count++
									}
								}
								if(p==Piece.PIECE_O) continue
							}

							// Left rotation
							var rot = piece.getRotateDirection(-1, rt)
							var newX = x
							var newY = y
							fldTemp.copy(fldBackup)

							if(piece.checkCollision(x, y, rot, fldTemp)) {

								wallkick.executeWallkick(x, y, -1, rt, rot, true, piece, fldTemp, null!!)?.let {kick ->
									newX = x+kick.offsetX
									newY = piece.getBottom(newX, y+kick.offsetY, rot, fldTemp)
								}
							}
							if(!piece.checkCollision(newX, newY, rot, fldTemp)&&newY>piece.getBottom(newX, 0, rot, fldTemp)) {
								piece.placeToField(newX, newY, rot, fldTemp)
								if(fldTemp.checkLine()==1) {
									fldTemp.clearLine()
									fldTemp.downFloatingBlocks()
									val index = fieldToIndex(fldTemp, 0)
									if(index>=0) {
										moves[i][p] = Transition(x, rt, -1, index, moves[i][p])
										count++
									}
								}
							}

							// Right rotation
							rot = piece.getRotateDirection(1, rt)
							newX = x
							newY = y
							fldTemp.copy(fldBackup)

							if(piece.checkCollision(x, y, rot, fldTemp)) {
								wallkick.executeWallkick(x, y, 1, rt, rot, true, piece, fldTemp, null!!)?.let {kick ->
									newX = x+kick.offsetX
									newY = piece.getBottom(newX, y+kick.offsetY, rot, fldTemp)
								}
							}
							if(!piece.checkCollision(newX, newY, rot, fldTemp)&&newY>piece.getBottom(newX, 0, rot, fldTemp)) {
								piece.placeToField(newX, newY, rot, fldTemp)
								if(fldTemp.checkLine()==1) {
									fldTemp.clearLine()
									fldTemp.downFloatingBlocks()
									val index = fieldToIndex(fldTemp, 0)
									if(index>=0) {
										moves[i][p] = Transition(x, rt, 1, index, moves[i][p])
										count++
									}
								}
							}
						}

						if(piece.id==Piece.PIECE_O) break
					}
				}
			}
			//log.debug("Transition table created. Total entries: " + count);
			//TODO: PageRank scores for each state
		}

		/**
		 * Converts field to field state int code
		 * @param field Field object
		 * @param valleyX Leftmost x-coordinate of 4-block-wide valley to combo in
		 * @return Field state int code.
		 */
		private fun fieldToCode(field:Field, valleyX:Int = 3):Int {
			val height = field.height
			var result = 0
			for(y in height-3 until height)
				for(x in 0..3) {
					result = result.shl(1)
					if(!field.getBlockEmptyF(x+valleyX, y)) result++
				}
			return result
		}

		/**
		 * Converts field state int code to FIELDS array index
		 * @param field Field state int code
		 * @return State index if found; -1 if not found.
		 */
		private fun fieldToIndex(field:Int):Int {
			var min = 0
			var max = FIELDS.size-1
			var mid:Int
			while(min<=max) {
				mid = min+max shr 1
				when {
					FIELDS[mid]>field -> max = mid-1
					FIELDS[mid]<field -> min = mid+1
					else -> return mid
				}
			}
			return -1
		}

		/**
		 * Converts field object to FIELDS array index
		 * @param field Field object
		 * @param valleyX Leftmost x-coordinate of 4-block-wide valley to combo in
		 * @return State index if found; -1 if not found.
		 */
		private fun fieldToIndex(field:Field, valleyX:Int):Int = fieldToIndex(fieldToCode(field, valleyX))

		fun fieldToIndex(field:Field):Int = fieldToIndex(fieldToCode(field))

	}
}
