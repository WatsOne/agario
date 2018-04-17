//import mu.KLogging
import org.json.JSONObject
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

        var prevFood = listOf<String>()
        var prevFoodPos: Pair<Float, Float>? = null

        while (true) {
            val tickData = JSONObject(readLine())
            data.parse(tickData, world)

            if (data.me.isEmpty()) return

            //очищаем вектора скоростей у противков пропавших из виду
            enemySpeedVectors.filter { !data.enemy.map { e -> e.id }.contains(it.key) }.forEach { enemySpeedVectors[it.key] = null }
            prevEnemyPositions.filter { !data.enemy.map { e -> e.id }.contains(it.key) }.forEach { prevEnemyPositions[it.key] = null }

            if (!data.enemy.isEmpty()) {
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

                if (victimPairs.isNotEmpty() || hunterPairs.isNotEmpty()) {

                    val simResult = if (Utils.canSplit(data.me, world)) {
                        val splitPoints = splitSimulation(enemySpeedVectors, world, data)
                        enemySimulation(victimPairs, hunterPairs, enemySpeedVectors, world, data, true, splitPoints)
                    } else {
                        enemySimulation(victimPairs, hunterPairs, enemySpeedVectors, world, data, false, 0f)
                    }

                    println(simResult)

                    prevFoodPos = null
                    prevFood = listOf()

                    tick++
                    continue
                }
            }

            val split = (tick % 50 == 0)
            if (data.food.isEmpty()) {
                idlePoint = getIdlePoint(data, world, idlePoint)

                prevFoodPos = null
                prevFood = listOf()

                println(JSONObject(mapOf("X" to idlePoint.first, "Y" to idlePoint.second)))
            } else {
                if (data.food.map { it.x.toString() + it.y }.intersect(prevFood).isNotEmpty()) {

                    println(JSONObject(mapOf("X" to prevFoodPos?.first, "Y" to prevFoodPos?.second, "Split" to split)))
                } else {

                    prevFoodPos = null
                    prevFood = listOf()

                    val doEatPosition = doEat(data, world)
                    if (doEatPosition.third == null) {
                        idlePoint = getIdlePoint(data, world, idlePoint)

                        println(JSONObject(mapOf("X" to idlePoint.first, "Y" to idlePoint.second, "Split" to split)))
                    } else {
                        idlePoint = null
                        if (!split) {
                            prevFoodPos = Pair(doEatPosition.first, doEatPosition.second)
                            prevFood = doEatPosition.third!!
                            println(JSONObject(mapOf("X" to doEatPosition.first, "Y" to doEatPosition.second)))
                        } else {
                            println(JSONObject(mapOf("X" to doEatPosition.first, "Y" to doEatPosition.second, "Split" to true)))
                        }
                    }
                }
            }

//            println(JSONObject(mapOf("X" to world.width / 2, "Y" to world.height / 2)))
            tick++
        }
    }

    private fun getIdlePoint(data: Data, world: World, idlePoint: Pair<Float, Float>?): Pair<Float, Float> {
        val player = getLeaderFragment(data.me)

        return if (idlePoint == null) {
            getNewIdleRotatePoint(player, world)
        } else {
            val dist = Utils.dist(player.x, player.y, idlePoint.first, idlePoint.second)
            if (dist < player.r * 3) {
                getNewIdleRotatePoint(player, world)
            } else {
                idlePoint
            }
        }
    }

    private fun getNewIdleRotatePoint(player: Me, world: World): Pair<Float, Float> {
        val nextPoint = Utils.rotatingPointsForSimulation(player, world, 3)[1]
        return Pair(nextPoint.first, nextPoint.second)
    }

    private fun doEat(data: Data, world: World): Triple<Float, Float, List<String>?> {

        val total = mutableMapOf<Pair<Float, Float>, Float>()
        var oper = 0

        val testFoods = data.food.map { TestFood(it) }
        val foodMap = mutableMapOf<Pair<Float, Float>, List<String>>()

        Utils.rotatingPointsForSimulation(data.me[0], world, 15).forEach { d ->
            val testPlayers = data.me.map { TestPlayer(it) }
            testFoods.forEach { it.eaten = false }

            var eat = 0
            var ppt = 0f
            var tick = 0

            val foodList = mutableListOf<String>()
            repeat(20, {

                testPlayers.forEach { Utils.applyDirect(d.first, d.second, it, world) }
                for (i in 0 until testPlayers.size ) {
                    for (j in i + 1 until testPlayers.size) {
                        Utils.calculateCollision(testPlayers[i], testPlayers[j])
                    }
                }
                testPlayers.forEach { Utils.move(it, world) }

                testPlayers.forEach { p ->
                    testFoods.forEach { f ->
                        if (!f.eaten && Utils.canEat(p, f)) {
                            eat++
                            f.eaten = true
                            p.m += world.foodMass
                            p.r = 2 * sqrt(p.m)
                            ppt = eat / tick.toFloat()

                            foodList.add(f.x.toString() + f.y.toString())
                        }
                    }
                }

                tick++
                oper++
            })
            total[d] = ppt
            foodMap[d] = foodList
        }


        val maxPoint = total.maxBy { it.value }
        val maxValue = maxPoint?.value ?: 0f

        return if (maxValue == 0f) {
            Triple(0f, 0f, null)
        } else {
            Triple(maxPoint?.key?.first ?: 0f, maxPoint?.key?.second ?: 0f, foodMap[Pair(maxPoint?.key?.first ?: 0f, maxPoint?.key?.second ?: 0f)])
        }
    }

    private fun getLeaderFragment(me: List<Me>): Me {
        return me.minBy { it.id.toFloat() } ?: me[0]
    }

    private fun enemySimulation(victims: List<Pair<String, String>>, huntersP: List<Pair<String, String>>, enemyVectors: Map<String, Pair<Float, Float>?>,  world: World, data: Data, canSplit: Boolean, splitPoints: Float):  JSONObject {
        val fragmentMap = data.me.associateBy({ it.id }, { it })
        var enemyMap = data.enemy.associateBy({ it.id }, { it })
        var hunters = huntersP
        val enemies = data.enemy

        if (hunters.isNotEmpty()) {
            val targetFragments = hunters.map { it.second }.distinct()
            val w = world.width
            val h = world.height

            val fakeHunters = mutableListOf<Enemy>()

            targetFragments.mapNotNull { fragmentMap[it] }.forEach {
                val newMass = it.m * MASS_EAT_FACTOR + 1
                val newR = 2 * sqrt(newMass)

                //слева и справа
                fakeHunters.add(Enemy("f00" + it.id, 0f, it.y, newR, newMass))
                fakeHunters.add(Enemy("f01" + it.id, w.toFloat(), it.y, newR, newMass))
                //снизу и сверху
                fakeHunters.add(Enemy("f02" + it.id, it.x, 0f, newR, newMass))
                fakeHunters.add(Enemy("f03" + it.id, it.x, h.toFloat(), newR, newMass))
            }

            enemies.addAll(fakeHunters)
            hunters = Utils.getPotentialHuntersTest(data.me, enemies)
            enemyMap = enemies.associateBy({ it.id }, { it })
        }

        val victimsCount = victims.groupingBy { it.second }.eachCount()
        val huntersCount = hunters.groupingBy { it.second }.eachCount()

        val victimDist = mutableMapOf<Pair<String, String>, Float>()
        val hunterDist = mutableMapOf<Pair<String, String>, Float>()

        victims.forEach { victimDist[it] = Utils.dist(fragmentMap[it.first]!!, enemyMap[it.second]!!) }
        hunters.forEach { hunterDist[it] = Utils.dist(enemyMap[it.first]!!, fragmentMap[it.second]!!) }

        val visionFactor = if (data.me.size == 1) 1f else sqrt(data.me.size.toFloat())
        val points = mutableMapOf<Pair<Float, Float>, Float>()

        Utils.rotatingPointsForSimulation(data.me[0], world, 60).plus(Utils.getCentorid(data.me)).forEach { d ->
            val testFragments = data.me.map { TestPlayer(it) }.toMutableList()
            val testEnemies = enemies.map { TestPlayer(it, enemyVectors[it.id]?.first ?: 0f, enemyVectors[it.id]?.second ?: 0f) }

            val testFragmentsMap = testFragments.associateBy({it.id}, {it})
            val testEnemiesMap = testEnemies.associateBy({it.id}, {it})

            val victimNewDist = mutableMapOf<Pair<String, String>, Float>()
            val hunterNewDist = mutableMapOf<Pair<String, String>, Float>()

            val testEnemiesForMoving = testEnemies.filter { it.id == null || !it.id.startsWith("f") }.toMutableList()
            val notActualHunters = mutableListOf<Pair<String, String>>()
            repeat(5, {
                testEnemiesForMoving.forEach { Utils.applyDirect(it.x + it.sx, it.y + it.sy, it, world) }
                testFragments.forEach { Utils.applyDirect(d.first, d.second, it, world) }

                for (i in 0 until testEnemiesForMoving.size ) {
                    for (j in i + 1 until testEnemiesForMoving.size) {
                        Utils.calculateCollision(testEnemiesForMoving[i], testEnemiesForMoving[j])
                    }
                }

                for (i in 0 until testFragments.size ) {
                    for (j in i + 1 until testFragments.size) {
                        Utils.calculateCollision(testFragments[i], testFragments[j])
                    }
                }

                testFragments.forEach { Utils.move(it, world) }
                testEnemiesForMoving.forEach { Utils.move(it, world) }

                if (tick % SHRINK_EVERY_TICK == 0) {
                    testFragments.filter { Utils.canShrink(it) }.forEach { Utils.shrink(it) }
                    testEnemiesForMoving.filter { Utils.canShrink(it) }.forEach { Utils.shrink(it) }
                }

                val fragmentsToRemove = mutableListOf<TestPlayer>()
                testFragments.forEach { f ->
                    val nearestEnemy = testEnemiesForMoving.filter { Utils.canEat(it, f) }.minBy { Utils.dist(it, f) }
                    if (nearestEnemy != null) {

                        hunterNewDist[Pair(nearestEnemy.id!!, f.id!!)] = 0f

                        val otherPairs = hunters.filter { it.second == f.id && !(it.second == f.id && it.first == nearestEnemy.id) }
                        otherPairs.forEach {
                            hunterNewDist[it] = Utils.dist(testEnemiesMap[it.first]!!, f)
                        }

                        fragmentsToRemove.add(f)
                    }
                }
                fragmentsToRemove.forEach { testFragments.remove(it) }

                val huntersToRemove = mutableListOf<TestPlayer>()
                testEnemiesForMoving.forEach { e ->
                    val nearestFragment = testFragments.filter { Utils.canEat(it, e) }.minBy { Utils.dist(it, e) }
                    if (nearestFragment != null) {
                        victimNewDist[Pair(nearestFragment.id!!, e.id!!)] = 0f

                        val otherPairs = victims.filter { it.second == e.id && !(it.second == e.id && it.first == nearestFragment.id) }
                        otherPairs.forEach {
                            victimNewDist[it] = Utils.dist(testFragmentsMap[it.first]!!, e)
                        }

                        huntersToRemove.add(e)
                    }
                }
                huntersToRemove.forEach { testEnemiesForMoving.remove(it) }

                var moreFuse = true
                val fuseToRemove = mutableListOf<TestPlayer>()

                while (moreFuse) {
                    moreFuse = false
                    for (i in 0 until testFragments.size ) {
                        for (j in i + 1 until testFragments.size) {
                            if (testFragments[i].isActual && testFragments[j].isActual) {
                                if (Utils.canFuse(testFragments[i], testFragments[j])) {
                                    Utils.fusion(testFragments[i], testFragments[j])
                                    testFragments[j].isActual = false
                                    fuseToRemove.add(testFragments[j])
                                    moreFuse = true
                                }
                            }
                        }
                    }

                    if (moreFuse) {
                        testFragments.filter { it.isActual }.forEach { Utils.updateByMass(it, world) }
                    }
                }
                if (fuseToRemove.isNotEmpty()) {
                    fuseToRemove.forEach { testFragments.remove(it) }
                    val newPotHunters = Utils.getPotentialHuntersTestTest(testFragments, testEnemiesForMoving)
                    hunters.forEach {
                        if (!newPotHunters.contains(it)) {
                            notActualHunters.add(it)
                        }
                    }
                }

                testFragments.filter { it.needUpdateMass }.forEach {
                    Utils.updateByMass(it, world)
                    it.needUpdateMass = false
                }
                testEnemiesForMoving.filter { it.needUpdateMass }.forEach {
                    Utils.updateByMass(it, world)
                    it.needUpdateMass = false
                }
            })

            victimDist.forEach { victimNewDist.putIfAbsent(it.key, Utils.dist(testFragmentsMap[it.key.first]!!, testEnemiesMap[it.key.second]!!)) }
            hunterDist.forEach { hunterNewDist.putIfAbsent(it.key, Utils.dist(testEnemiesMap[it.key.first]!!, testFragmentsMap[it.key.second]!!)) }

            val victimPoints = mutableMapOf<String, Float>()
            val excludeMap = mutableMapOf<String, Int>()
            victims.forEach {
                val allDist = fragmentMap[it.first]!!.r * 4 * visionFactor + 10
                val firstBound = max((allDist - victimNewDist[it]!!), 0f)
                val secondBound = max((allDist - victimDist[it]!!), 0f)

                val prev = victimPoints[it.second] ?: 0f
                val score = (firstBound*firstBound - secondBound*secondBound)/allDist*(100/allDist)
                if (score == 0f) {
                    val prevExclude = excludeMap[it.second] ?: 0
                    excludeMap[it.second] = prevExclude + 1
                }
                victimPoints[it.second] = prev + score*testEnemiesMap[it.second]!!.m
            }
            victimPoints.forEach {
                val count = victimsCount[it.key]!! - (excludeMap[it.key] ?: 0)
                victimPoints[it.key] = if (count == 0) 0f else victimPoints[it.key]!! / count
            }

            val hunterPoints = mutableMapOf<String, Float>()
            excludeMap.clear()
            hunters.filter { !notActualHunters.contains(it) }.forEach {
                val allDist = enemyMap[it.first]!!.r * 4 + 10
                val firstBound = max((allDist - hunterDist[it]!!), 0f)
                val secondBound = max((allDist - hunterNewDist[it]!!), 0f)

                val prev = hunterPoints[it.second] ?: 0f
                val score = (firstBound*firstBound - secondBound*secondBound)/allDist*(100/allDist)
                if (score == 0f) {
                    val prevExclude = excludeMap[it.second] ?: 0
                    excludeMap[it.second] = prevExclude + 1
                }
                hunterPoints[it.second] = prev + score*testFragmentsMap[it.second]!!.m
            }
            hunterPoints.forEach {
                val count = huntersCount[it.key]!! - (excludeMap[it.key] ?: 0)
                hunterPoints[it.key] = if (count == 0) 0f else hunterPoints[it.key]!! / count
            }

            val allVictimPoints = victimPoints.values.sum()
            val allHunterPoints = hunterPoints.values.sum()

            points[d] = allVictimPoints + allHunterPoints
        }

        val maxPoints = points.maxBy { it.value }!!

        return JSONObject(mapOf("X" to maxPoints.key.first, "Y" to maxPoints.key.second, "Split" to (canSplit && (splitPoints > maxPoints.value))))
    }

    private fun splitSimulation(enemyVectors: Map<String, Pair<Float, Float>?>, world: World, data: Data): Float {

        //команда сплита стартует после движений, симулируем 1 тик
        val testEnemies = data.enemy.map { TestPlayer(it, enemyVectors[it.id]?.first ?: 0f, enemyVectors[it.id]?.second ?: 0f) }.toMutableList()
        val testFragmentsInit = data.me.map { TestPlayer(it) }

        testEnemies.forEach { Utils.applyDirect(it.x + it.sx, it.y + it.sy, it, world) }
        testFragmentsInit.forEach { Utils.applyDirect(it.x + it.sx, it.y + it.sy, it, world) }

        for (i in 0 until testEnemies.size ) {
            for (j in i + 1 until testEnemies.size) {
                Utils.calculateCollision(testEnemies[i], testEnemies[j])
            }
        }

        for (i in 0 until testFragmentsInit.size ) {
            for (j in i + 1 until testFragmentsInit.size) {
                Utils.calculateCollision(testFragmentsInit[i], testFragmentsInit[j])
            }
        }

        testFragmentsInit.forEach { Utils.move(it, world) }
        testEnemies.forEach { Utils.move(it, world) }

        //фиксируем охотников
        val originalHunters = Utils.getPotentialHuntersTestTest(testFragmentsInit, testEnemies)

        //и вот теперь сплит
        val massOrderedFragments = testFragmentsInit.sortedByDescending { it.m }
        var maxPotentialFragment = world.maxFragment - data.me.size

        val testFragments = mutableListOf<TestPlayer>()
        massOrderedFragments.forEach {
            if (it.m > MIN_SPLIT_MASS && maxPotentialFragment <= world.maxFragment) {
                testFragments.addAll(Utils.split(it))
                maxPotentialFragment++
            } else {
                testFragments.add(TestPlayer(it))
            }
        }

        val visionFactor = if (testFragments.size == 1) 1f else sqrt(testFragments.size.toFloat())

        val victims = Utils.getPotentialVictims(testFragments, data.enemy)
        val hunters = Utils.getPotentialHunters(testFragments, data.enemy)

        val victimsCount = victims.groupingBy { it.second }.eachCount()
        val huntersCount = hunters.groupingBy { it.second }.eachCount()

        val fragmentMap = testFragments.associateBy({ it.id }, { it })
        val enemyMap = data.enemy.associateBy({ it.id }, { it })

        val victimDist = mutableMapOf<Pair<String, String>, Float>()
        val hunterDist = mutableMapOf<Pair<String, String>, Float>()

        victims.forEach { victimDist[it] = Utils.dist(fragmentMap[it.first]!!, enemyMap[it.second]!!) }

        originalHunters.forEach { hunterDist[it] = Utils.dist(enemyMap[it.first]!!, fragmentMap[it.second]!!) }
        hunters.filter { !originalHunters.contains(it) }.forEach { hunterDist[it] = enemyMap[it.first]!!.r * 4 + 10 }

        val huntersTarget = if (hunters.isEmpty()) {
            null
        } else {
            hunterDist.minBy { it.value }?.key?.second
        }

        val testEnemiesMap = testEnemies.associateBy({it.id}, {it})

        val victimNewDist = mutableMapOf<Pair<String, String>, Float>()
        val hunterNewDist = mutableMapOf<Pair<String, String>, Float>()

        repeat(5, {
            if (huntersTarget == null) {
                testEnemies.forEach { Utils.applyDirect(it.x + it.sx, it.y + it.sy, it, world) }
            } else {
                val targetFragment = fragmentMap[huntersTarget] ?: testFragments[0]
                testEnemies.forEach { Utils.applyDirect(targetFragment.x, targetFragment.y, it, world) }
            }
            testFragments.forEach { Utils.applyDirect(it.x + it.sx, it.y + it.sy, it, world) }

            for (i in 0 until testEnemies.size ) {
                for (j in i + 1 until testEnemies.size) {
                    Utils.calculateCollision(testEnemies[i], testEnemies[j])
                }
            }

            for (i in 0 until testFragments.size ) {
                for (j in i + 1 until testFragments.size) {
                    Utils.calculateCollision(testFragments[i], testFragments[j])
                }
            }

            testFragments.forEach { Utils.move(it, world) }
            testEnemies.forEach { Utils.move(it, world) }

            val fragmentsToRemove = mutableListOf<TestPlayer>()
            testFragments.forEach { f ->
                val nearestEnemy = testEnemies.filter { Utils.canEat(it, f) }.minBy { Utils.dist(it, f) }
                if (nearestEnemy != null) {

                    hunterNewDist[Pair(nearestEnemy.id!!, f.id!!)] = 0f

                    val otherPairs = hunters.filter { it.second == f.id && !(it.second == f.id && it.first == nearestEnemy.id) }
                    otherPairs.forEach {
                        hunterNewDist[it] = Utils.dist(testEnemiesMap[it.first]!!, f)
                    }

                    fragmentsToRemove.add(f)
                }
            }
            fragmentsToRemove.forEach { testFragments.remove(it) }

            val huntersToRemove = mutableListOf<TestPlayer>()
            testEnemies.forEach { e ->
                val nearestFragment = testFragments.filter { Utils.canEat(it, e) }.minBy { Utils.dist(it, e) }
                if (nearestFragment != null) {
                    victimNewDist[Pair(nearestFragment.id!!, e.id!!)] = 0f

                    val otherPairs = victims.filter { it.second == e.id && !(it.second == e.id && it.first == nearestFragment.id) }
                    otherPairs.forEach {
                        victimNewDist[it] = Utils.dist(fragmentMap[it.first]!!, e)
                    }

                    huntersToRemove.add(e)
                }
            }
            huntersToRemove.forEach { testEnemies.remove(it) }
        })

        victimDist.forEach { victimNewDist[it.key] = Utils.dist(fragmentMap[it.key.first]!!, testEnemiesMap[it.key.second]!!) }
        hunterDist.forEach { hunterNewDist[it.key] = Utils.dist(testEnemiesMap[it.key.first]!!, fragmentMap[it.key.second]!!) }

        val victimPoints = mutableMapOf<String, Float>()
        val excludeMap = mutableMapOf<String, Int>()
        victims.forEach {
            val allDist = fragmentMap[it.first]!!.r * 4 * visionFactor + 10
            val firstBound = max((allDist - victimNewDist[it]!!), 0f)
            val secondBound = max((allDist - victimDist[it]!!), 0f)

            val prev = victimPoints[it.second] ?: 0f
            val score = (firstBound*firstBound - secondBound*secondBound)/allDist*(100/allDist)
            if (score == 0f) {
                val prevExclude = excludeMap[it.second] ?: 0
                excludeMap[it.second] = prevExclude + 1
            }
            victimPoints[it.second] = prev + score*testEnemiesMap[it.second]!!.m
        }
        victimPoints.forEach {
            val count = victimsCount[it.key]!! - (excludeMap[it.key] ?: 0)
            victimPoints[it.key] = if (count == 0) 0f else victimPoints[it.key]!! / count
        }

        val hunterPoints = mutableMapOf<String, Float>()
        excludeMap.clear()
        hunters.forEach {
            val allDist = enemyMap[it.first]!!.r * 4 + 10
            val firstBound = max((allDist - hunterDist[it]!!), 0f)
            val secondBound = max((allDist - hunterNewDist[it]!!), 0f)

            val prev = hunterPoints[it.second] ?: 0f
            val score = (firstBound*firstBound - secondBound*secondBound)/allDist*(100/allDist)
            if (score == 0f) {
                val prevExclude = excludeMap[it.second] ?: 0
                excludeMap[it.second] = prevExclude + 1
            }
            hunterPoints[it.second] = prev + score*fragmentMap[it.second]!!.m
        }
        hunterPoints.forEach {
            val count = huntersCount[it.key]!! - (excludeMap[it.key] ?: 0)
            hunterPoints[it.key] = if (count == 0) 0f else hunterPoints[it.key]!! / count
        }

        val allVictimPoints = victimPoints.values.sum()
        val allHunterPoints = hunterPoints.values.sum()

        return allVictimPoints + allHunterPoints
    }
}