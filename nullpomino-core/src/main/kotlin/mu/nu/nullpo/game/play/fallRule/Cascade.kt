/*
 Copyright (c) 2026, NullNoname
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

package mu.nu.nullpo.game.play.fallRule

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.fallRule.Cascade.connectedNeighbors

data object Cascade:LineGravity {

	private fun connectedNeighbors(x:Int, y:Int, b:Block):Sequence<Pair<Int, Int>> = sequence {
		if(b.getAttribute(Block.ATTRIBUTE.CONNECT_UP)) yield(x to y-1)
		if(b.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) yield(x to y+1)
		if(b.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) yield(x-1 to y)
		if(b.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) yield(x+1 to y)
	}

	fun isCascadeTarget(b:Block) =
		!b.isEmpty&&!b.getAttribute(Block.ATTRIBUTE.ANTIGRAVITY)&&!b.getAttribute(Block.ATTRIBUTE.WALL)
			&&!b.getAttribute(Block.ATTRIBUTE.CASCADE_FALL)

	private data class Cluster(val cells:Set<Pair<Int, Int>>)

	/**
	 * [mu.nu.nullpo.game.component.Field]上のカスケード対象ブロックを、連結クラスタごとに収集する。
	 *
	 * 連結判定は単純な上下左右ではなく、各ブロックの CONNECT_* 属性に基づいて [connectedNeighbors] で決定する。
	 *
	 * @return first: [Cluster] 一覧 / second: 座標 `(x, y)` からクラスタ index を引く逆引きマップ
	 *
	 * 逆引きマップは探索中に同時構築され、後続の可動判定などで
	 * 「下のブロックが同一クラスタか別クラスタか」を高速に判定するために使う。
	 */
	private fun Field.collectClusters():Pair<List<Cluster>, Map<Pair<Int, Int>, Int>> {
		val visited = mutableSetOf<Pair<Int, Int>>()
		val clusters = mutableListOf<Cluster>()
		val cellToCluster = mutableMapOf<Pair<Int, Int>, Int>()

		for(y in allSpaceRows.reversed()) for(x in 0..<width) {
			val start = getBlock(x, y)?:continue
			if(!isCascadeTarget(start)) continue

			val startPos = x to y
			if(startPos in visited) continue

			val idx = clusters.size
			val cells = mutableSetOf<Pair<Int, Int>>()
			val stack = ArrayDeque<Pair<Int, Int>>()
			stack += startPos

			while(stack.isNotEmpty()) {
				val p = stack.removeLast()
				if(!visited.add(p)) continue

				val (cx, cy) = p
				val blk = getBlock(cx, cy)?:continue
				if(!isCascadeTarget(blk)) continue

				cells += p
				cellToCluster[p] = idx

				connectedNeighbors(cx, cy, blk).forEach {np ->
					val nb = getBlock(np.first, np.second)
					if(nb!=null&&isCascadeTarget(nb)&&np !in visited) stack += np
				}
			}

			if(cells.isNotEmpty()) clusters += Cluster(cells)
		}

		return clusters to cellToCluster
	}

	/** 落下条件を満たすクラスタを反復判定し、[clusters] のうち落下できるもののindexを返す。 */
	private fun Field.calcMovable(clusters:List<Cluster>, cellToCluster:Map<Pair<Int, Int>, Int>):Set<Int> {
		val movable = clusters.indices.toMutableSet()
		var changed:Boolean
		do {
			changed = false
			val it = movable.iterator()
			while(it.hasNext()) {
				val idx = it.next()
				val ownCells = clusters[idx].cells
				val canFall = ownCells.all {(x, y) ->
					val ny = y+1
					if(getCoordAttribute(x, ny)==Field.Coord.WALL) return@all false
					val belowPos = x to ny
					if(belowPos in ownCells) return@all true
					val below = getBlock(x, ny)
					if(below==null||below.isEmpty) return@all true
					val other = cellToCluster[belowPos]?:return@all false
					other in movable
				}

				if(!canFall) {
					it.remove()
					changed = true
				}
			}
		} while(changed)
		return movable
	}

	/**
	 * Drop connected block clusters by one cell without breaking their shape.
	 * Connectivity is defined by CONNECT_* attributes on each block.
	 *
	 * @return Number of blocks that can still fall after this step.
	 */
	private fun Field.cascadeStep():Int {
		val (clustersBefore, cellToClusterBefore) = collectClusters()
		if(clustersBefore.isEmpty()) return 0

		val movableBefore = calcMovable(clustersBefore, cellToClusterBefore)
		if(movableBefore.isEmpty()) return 0

		val movedCells = mutableSetOf<Pair<Int, Int>>()
		movableBefore.forEach {idx ->
			clustersBefore[idx].cells.sortedByDescending {it.second}.forEach {(x, y) ->
				getBlock(x, y)?.let {b ->
					b.setAttribute(false, Block.ATTRIBUTE.CASCADE_FALL)
					setBlock(x, y+1, b)
					delBlock(x, y)
					if(getLineFlag(y+1)) setLineFlag(y+1, false)
					movedCells += (x to y+1)
				}
			}
		}

		val (clustersAfter, cellToClusterAfter) = collectClusters()
		val movableAfter = calcMovable(clustersAfter, cellToClusterAfter)

		// 1マス降下後に停止したクラスタへ CASCADE_FALL を付与
		clustersAfter.forEachIndexed {idx, cluster ->
			if(idx in movableAfter) return@forEachIndexed
			if(cluster.cells.none {it in movedCells}) return@forEachIndexed
			cluster.cells.forEach {(x, y) ->
				getBlock(x, y)?.setAttribute(true, Block.ATTRIBUTE.CASCADE_FALL)
			}
		}

		return movableAfter.sumOf {clustersAfter[it].cells.size}
	}

	fun Field.canCascade():Boolean = cascadeTime().first>0
	/** @return pair first: Number of blocks can fall, second: Max Height of blocks that will */
	fun Field.cascadeTime():Pair<Int, Int> {
		val (clusters, cellToCluster) = collectClusters()
		if(clusters.isEmpty()) return 0 to 0

		val movable = calcMovable(clusters, cellToCluster)
		if(movable.isEmpty()) return 0 to 0

		val movablePositions = movable.flatMap {clusters[it].cells}.toSet()
		val movableCells:List<Int> = movable.map {
			clusters[it].cells.minOf {(x, y) ->
				var d = 0
				var ny = y+1
				while(getCoordAttribute(x, ny)!=Field.Coord.WALL) {
					val pos = x to ny
					if(pos in movablePositions) {
						d++
						ny++
						continue
					}
					val b = getBlock(x, ny)
					if(b==null||b.isEmpty) {
						d++
						ny++
					} else break
				}
				d
			}
		}

		return movable.sumOf {clusters[it].cells.size} to (movableCells.maxOrNull()?:0)
	}

	override fun check(field:Field):Pair<Int, Int> = field.cascadeTime()
	override fun fallInstant(field:Field) = field.run {
		val check = cascadeTime()
		do {
			val it = cascadeTime()
			repeat(it.second) {cascadeStep()}
		} while(it.first>0)
		setAllAttribute(false, Block.ATTRIBUTE.CASCADE_FALL)
		check.second
	}

	override fun fallSingle(field:Field) = field.run {
		cascadeStep()
	}

	override fun statLineClear(engine:GameEngine):Boolean = engine.run {
		if(statc[0]<lineDelay) return false
		val (fc1, fc2) = check(field)
		statc[7] = fc1
		statc[8] = fc2
		if(fc1>0) {
			statc[0] = lineDelay
			if(statc[6]<cascadeDelay) {
				statc[6]++
				field.filterAttributeBlocks(Block.ATTRIBUTE.CASCADE_FALL).forEach {(b) ->
					b.offsetY = statc[6]/(cascadeDelay+1f)
				}
				return false
			} else {
				statc[9] = fallSingle(field)
				statc[6] -= cascadeDelay
				if(!isRetroSkin) playSE("softdrop")
				if(statc[9]<=0) {
					statc[6] = 0
					playSE("step")
					field.setAllAttribute(false, Block.ATTRIBUTE.CASCADE_FALL)
					return false
				}
				return false
			}
		} else if(statc[6]<cascadeClearDelay) {
			statc[6]++
			return false
		} else if(clearMode.check(field).size>0) {
			chain++
			if(chain>statistics.maxChain) statistics.maxChain = chain
			resetStatc()
			statc[0]--
			return false
		}
		return fc1==0
	}

	override fun lineClearEnd(engine:GameEngine) {
	}
}