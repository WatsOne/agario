//import mu.KLogging
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sqrt

class Strategy2 {
    //    companion object: KLogging()
    var tick = 1

    fun go() {
        val config = JSONObject(readLine())
        val world = World(config)
        val data = Data()

        var idlePoint: Pair<Float, Float>? = null
        val prevEnemyPositions = mutableMapOf<String, Pair<Float, Float>?>()
        val enemySpeedVectors = mutableMapOf<String, Pair<Float, Float>?>()

        while (true) {
            val tickData = JSONObject(readLine())
            data.parse(tickData, world)

            if (data.me.isEmpty()) return

            //очищаем вектора скоростей у противков пропавших из виду
            enemySpeedVectors.filter { !data.enemy.map { e -> e.id }.contains(it.key) }.forEach { enemySpeedVectors[it.key] = null }
            prevEnemyPositions.filter { !data.enemy.map { e -> e.id }.contains(it.key) }.forEach { prevEnemyPositions[it.key] = null }

            if (!data.enemy.isEmpty()) {
                val enemyMap = data.enemy.associateBy({ it.id }, { it })

                //обновляем вектора скоростей врагов
                data.enemy.forEach {
                    val prevEnemyPos = prevEnemyPositions[it.id]
                    if (prevEnemyPos == null) {
                        prevEnemyPositions[it.id] = Pair(it.x, it.y)
                    } else {
                        enemySpeedVectors[it.id] = Pair(it.x - prevEnemyPos.first, it.y - prevEnemyPos.second)
                        prevEnemyPositions[it.id] = Pair(it.x, it.y)
                    }
                }

                val victimPairs = Utils.getPotentialVictims(data)
                val hunterPairs = Utils.getPotentialHunters(data)

                if (victimPairs.isNotEmpty() && hunterPairs.isNotEmpty()) {
                    val simPoint = enemySimulation(victimPairs, hunterPairs, enemySpeedVectors, world, data)
                    println(JSONObject(mapOf("X" to simPoint.first, "Y" to simPoint.second)))
                    continue
                }
            }

            if (data.food.isEmpty()) {
                idlePoint = getIdlePoint(data, world, idlePoint)
                println(JSONObject(mapOf("X" to idlePoint.first, "Y" to idlePoint.second)))
            } else {
                idlePoint = null
                val doEatPosition = doEat(data, world)
                println(JSONObject(mapOf("X" to doEatPosition.first, "Y" to doEatPosition.second)))
            }

//            println(JSONObject(mapOf("X" to world.width / 2, "Y" to world.height / 2)))
            tick++
        }
    }

    private fun getIdlePoint(data: Data, world: World, idlePoint: Pair<Float, Float>?): Pair<Float, Float> {
        val player = getLeaderFragment(data.me)

        return if (idlePoint == null) {
            getNewIdleRotatePoint(player, world, PI.toFloat() / 5)
        } else {
            val dist = Utils.dist(player.x, player.y, idlePoint.first, idlePoint.second)
            if (dist < player.r) {
                val newIdle = getNewIdleRotatePoint(player, world, PI.toFloat() / 5)
                if (idlePoint.first == newIdle.first && idlePoint.second == newIdle.second) {
                    getNewIdleRotatePoint(player, world, PI.toFloat())
                } else {
                    newIdle
                }
            } else {
                idlePoint
            }
        }
    }

    private fun getNewIdleRotatePoint(player: Me, world: World, angle: Float): Pair<Float, Float> {
        val currentAngle = Utils.getAngle(player.sx, player.sy)
        return Utils.rotate(player.x, player.y, player.r, 1000f, currentAngle + angle, world, true)
    }

    private fun doEat(data: Data, world: World): Pair<Float, Float> {

        val start = System.currentTimeMillis()
        val total = mutableMapOf<Pair<Float, Float>, Float>()
        var oper = 0

        val me = getLeaderFragment(data.me)
        val testFoods = data.food.map { TestFood(it) }

        Utils.rotatingPoints(me, 1000f, world, true).forEach { d ->
            val testPlayer = TestPlayer(me)
            testFoods.forEach { it.eaten = false }

            var eaten = testFoods.size
            var eat = 0

            var ppt = 0f
            var tick = 0

            while (Utils.dist(testPlayer.x, testPlayer.y, d.first, d.second) > testPlayer.r && eaten > 0 && tick <= 30) {

                Utils.applyDirect(d.first, d.second, testPlayer, world)
                Utils.move(testPlayer, world)

                testFoods.forEach { f ->
                    if (!f.eaten && Utils.canEat(testPlayer, f)) {
                        eat++
                        eaten--
                        f.eaten = true
                        testPlayer.m += world.foodMass
                        testPlayer.r = 2 * sqrt(testPlayer.m)
                        ppt = eat / tick.toFloat()
                    }
                }

                tick++
                oper++
            }
            total[d] = ppt
        }

//        logger.trace { "$tick calc: ${System.currentTimeMillis() - start} ms; oper: $oper. Radius: ${data.me[0].r}" }

        val max = getMaxScore(total)
        return Pair(max.first, max.second)
    }

    private fun getMaxScore(scoreMap: Map<Pair<Float, Float>, Float>): Pair<Float, Float> =
            scoreMap.maxBy { it.value }?.key ?: Pair(0f, 0f)

    private fun getLeaderFragment(me: List<Me>): Me {
        return me.minBy { it.id.toFloat() } ?: me[0]
    }

    private fun enemySimulation(victims: List<Pair<String, String>>, hunters: List<Pair<String, String>>, enemyVectors: Map<String, Pair<Float, Float>?>,  world: World, data: Data): Pair<Float, Float> {

        val victimsCount = victims.groupingBy { it.second }.eachCount()
        val huntersCount = hunters.groupingBy { it.second }.eachCount()

        val fragmentMap = data.me.associateBy({ it.id }, { it })
        val enemyMap = data.enemy.associateBy({ it.id }, { it })

        val victimDist = mutableMapOf<Pair<String, String>, Float>()
        val hunterDist = mutableMapOf<Pair<String, String>, Float>()

        victims.forEach { victimDist[it] = Utils.dist(fragmentMap[it.first]!!, enemyMap[it.second]!!) }
        hunters.forEach { hunterDist[it] = Utils.dist(enemyMap[it.first]!!, fragmentMap[it.second]!!) }

        val huntersTarget = if (hunters.isEmpty()) {
            null
        } else {
            hunterDist.minBy { it.value }?.key?.second
        }

        val points = mutableMapOf<Pair<Float, Float>, Float>()

        Utils.rotatingPointsForSimulation(data.me[0], world, 60).forEach { d ->
            val testFragments = data.me.map { TestPlayer(it) }
            val testEnemies = data.enemy.map { TestPlayer(it, enemyVectors[it.id]?.second ?: 0f, enemyVectors[it.id]?.second ?: 0f) }

            val testFragmentsMap = testFragments.associateBy({it.id}, {it})
            val testEnemiesMap = testEnemies.associateBy({it.id}, {it})

            repeat(5, {
                if (huntersTarget == null) {
                    testEnemies.forEach { Utils.applyDirect(it.x + it.sx, it.y + it.sy, it, world) }
                } else {
                    val targetFragment = testFragmentsMap[huntersTarget] ?: testFragments[0]
                    testEnemies.forEach { Utils.applyDirect(targetFragment.x, targetFragment.y, it, world) }
                }
                testFragments.forEach { Utils.applyDirect(d.first, d.second, it, world) }

                for (i in 0 until testFragments.size ) {
                    for (j in i + 1 until testFragments.size) {
                        Utils.calculateCollision(testFragments[i], testFragments[j])
                    }
                }

                testFragments.forEach { Utils.move(it, world) }
                testEnemies.forEach { Utils.move(it, world) }
            })

            val victimNewDist = mutableMapOf<Pair<String, String>, Float>()
            val hunterNewDist = mutableMapOf<Pair<String, String>, Float>()

            victimDist.forEach { victimNewDist[it.key] = Utils.dist(testFragmentsMap[it.key.first]!!, testEnemiesMap[it.key.second]!!) }
            hunterDist.forEach { hunterNewDist[it.key] = Utils.dist(testEnemiesMap[it.key.first]!!, testFragmentsMap[it.key.second]!!) }

            val victimPoints = mutableMapOf<String, Float>()
            victims.forEach {
                val allDist = fragmentMap[it.first]!!.r * 4
                val firstBound = max((allDist - victimNewDist[it]!!), 0f)
                val secondBound = max((allDist - victimDist[it]!!), 0f)

                val prev = victimPoints[it.second] ?: 0f
                val score = (firstBound*firstBound - secondBound*secondBound)/allDist
                victimPoints[it.second] = prev + max(score, 1f)*testEnemiesMap[it.second]!!.m
            }
            victimPoints.forEach { victimPoints[it.key] = victimPoints[it.key]!! / victimsCount[it.key]!! }

            val hunterPoints = mutableMapOf<String, Float>()
            hunters.forEach {
                val allDist = enemyMap[it.first]!!.r * 4
                val firstBound = max((allDist - hunterDist[it]!!), 0f)
                val secondBound = max((allDist - hunterNewDist[it]!!), 0f)

                val prev = hunterPoints[it.second] ?: 0f
                val score = (firstBound*firstBound - secondBound*secondBound)/allDist
                hunterPoints[it.second] = prev + max(score, 1f)*testFragmentsMap[it.second]!!.m
            }
            hunterPoints.forEach { hunterPoints[it.key] = hunterPoints[it.key]!! / huntersCount[it.key]!! }

            val allVictimPoints = victimPoints.values.sum()
            val allHunterPoints = hunterPoints.values.sum()

            points[d] = allVictimPoints - allHunterPoints
        }

        return points.maxBy { it.value }!!.key
    }
}