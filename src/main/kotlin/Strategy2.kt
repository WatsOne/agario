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
        val potentialFood = mutableListOf<Pair<Float, Float>>()

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

//                val victimPairs = Utils.getPotentialVictims(data)
//                val hunterPairs = Utils.getPotentialHunters(data)

//                if (victimPairs.isNotEmpty() || hunterPairs.isNotEmpty()) {

//                    val simResult = if (Utils.canSplit(data.me, world)) {
//                        val splitPoints = splitSimulation(enemySpeedVectors, world, data)
//                        enemySimulation(victimPairs, hunterPairs, enemySpeedVectors, world, data, true, splitPoints)
//                    } else {
//                        enemySimulation(victimPairs, hunterPairs, enemySpeedVectors, world, data, false, 0f)
//                    }

                    println(simulation(data, enemySpeedVectors, world))

                    prevFoodPos = null
                    prevFood = listOf()
                    potentialFood.clear()

                    tick++
                    continue
//                }
            }

            val split = (tick % 50 == 0)
            if (data.food.isEmpty()) {
                idlePoint = potentialFood.getOrElse(0, { getIdlePoint(data, world, idlePoint) })

                potentialFood.clear()
                prevFoodPos = null
                prevFood = listOf()

                println(JSONObject(mapOf("X" to idlePoint.first, "Y" to idlePoint.second)))
            } else {
                if (data.food.map { it.x.toString() + "|" + it.y }.intersect(prevFood).isNotEmpty()) {

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
                            potentialFood.addAll(data.food.map { it.x.toString() + "|" + it.y }.subtract(prevFood).map { Pair(it.split("|")[0].toFloat(), it.split("|")[1].toFloat()) })
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
        val nextPoint = Utils.rotatingPointsForSimulation(player, world, 9)[1]
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

                            foodList.add(f.x.toString() + "|" + f.y.toString())
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

    private fun simulation(data: Data, enemyVectors: Map<String, Pair<Float, Float>?>, world: World): JSONObject {

        val testFragmentsInitial = data.me.map { TestPlayer(it) }
        val testEnemiesInitial = data.enemy.map { TestPlayer(it, enemyVectors[it.id]?.first ?: 0f, enemyVectors[it.id]?.second ?: 0f) }.toMutableList()

        val points = mutableMapOf<Pair<Float, Float>, Float>()
        val simPoints = mutableMapOf<Pair<Float, Float>, Float>()
        val initialSimulationPoints = splitSimulation(testFragmentsInitial.map { it.copy() }.toMutableList(), testEnemiesInitial.map { it.copy() }.toMutableList(), world)

        Utils.rotatingPointsForSimulation(data.me[0], world, 20).forEach { d ->
            val testFragments = testFragmentsInitial.map { it.copy() }.toMutableList()
            val testEnemies = testEnemiesInitial.map { it.copy() }.toMutableList()
            val stepSimPoints = mutableListOf<Float>()

            var eatScore = 0f

            for (i in 1..10) {
                eatScore += stepSimulation(testFragments, testEnemies, d, world)
                if (i % 2 == 0) {
                    stepSimPoints.add(eatScore + splitSimulation(testFragments.map { it.copy() }.toMutableList(), testEnemies.map { it.copy() }.toMutableList(), world))
                }
            }

            points[d] = getScore(testFragments, testEnemies) + eatScore
            simPoints[d] = stepSimPoints.max() ?: Float.NEGATIVE_INFINITY
        }

        val maxPoints = points.maxBy { it.value }!!
        val maxSimPoint = simPoints.maxBy { it.value }!!

//        logger.trace { "$tick: max: $maxPoints; split: $maxSimPoint; init: $initialSimulationPoints" }

        return if (initialSimulationPoints > maxPoints.value && initialSimulationPoints > maxSimPoint.value) {
            JSONObject(mapOf("X" to maxPoints.key.first, "Y" to maxPoints.key.second, "Split" to true))
        } else if (maxSimPoint.value > maxPoints.value) {
            JSONObject(mapOf("X" to maxSimPoint.key.first, "Y" to maxSimPoint.key.second))
        } else {
            JSONObject(mapOf("X" to maxPoints.key.first, "Y" to maxPoints.key.second))
        }
    }

    private fun splitSimulation(testFragments: MutableList<TestPlayer>, testEnemies: MutableList<TestPlayer>, world: World): Float {
        if (!Utils.canSplit2(testFragments, world)) {
            return Float.NEGATIVE_INFINITY
        }

        var eatScore = 0f
        eatScore += stepSimulation(testFragments, testEnemies, null, world)

        var maxPotentialFragment = world.maxFragment - testFragments.size
        val massOrderedFragments = testFragments.sortedByDescending { it.m }

        val splittedFragment = mutableListOf<TestPlayer>()
        massOrderedFragments.forEach {
            if (it.m > MIN_SPLIT_MASS && maxPotentialFragment <= world.maxFragment) {
                splittedFragment.addAll(Utils.split(it))
                maxPotentialFragment++
            } else {
                splittedFragment.add(it)
            }
        }

        repeat(4, {
            eatScore += stepSimulation(splittedFragment, testEnemies, null, world)
        })

        return getScore(splittedFragment, testEnemies) + eatScore
    }

    private fun stepSimulation(testFragments: MutableList<TestPlayer>, testEnemies: MutableList<TestPlayer>, dir: Pair<Float, Float>?, world: World): Float {
        var eatScore = 0f

        testEnemies.forEach { Utils.applyDirect(it.x + it.sx, it.y + it.sy, it, world) }
        testFragments.forEach { Utils.applyDirect(dir?.first ?: (it.x + it.sx), dir?.second ?: (it.y + it.sy), it, world) }

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

        if (tick % SHRINK_EVERY_TICK == 0) {
            testFragments.filter { Utils.canShrink(it) }.forEach { Utils.shrink(it) }
            testEnemies.filter { Utils.canShrink(it) }.forEach { Utils.shrink(it) }
        }

        val fragmentsToRemove = mutableListOf<TestPlayer>()
        testFragments.forEach { f ->
            val nearestEnemy = testEnemies.filter { Utils.canEat(it, f) }.minBy { Utils.dist(it, f) }
            if (nearestEnemy != null) {

                f.x = nearestEnemy.x
                f.y = nearestEnemy.y

                nearestEnemy.m += f.m
                nearestEnemy.needUpdateMass = true

                eatScore += getScoreForEatFragment(f, testEnemies)

                fragmentsToRemove.add(f)
            }
        }
        fragmentsToRemove.forEach { testFragments.remove(it) }

        val huntersToRemove = mutableListOf<TestPlayer>()
        testEnemies.forEach { e ->
            val nearestFragment = testFragments.filter { Utils.canEat(it, e) }.minBy { Utils.dist(it, e) }
            if (nearestFragment != null) {

                e.x = nearestFragment.x
                e.y = nearestFragment.y

                nearestFragment.m += e.m
                nearestFragment.needUpdateMass = true

                eatScore += getScoreForEatEnemy(e, testFragments)

                huntersToRemove.add(e)
            }
        }
        huntersToRemove.forEach { testEnemies.remove(it) }

        var moreFuse = true

        while (moreFuse) {
            moreFuse = false
            for (i in 0 until testFragments.size ) {
                for (j in i + 1 until testFragments.size) {
                    if (testFragments[i].isActual && testFragments[j].isActual) {
                        if (Utils.canFuse(testFragments[i], testFragments[j])) {
                            Utils.fusion(testFragments[i], testFragments[j])
                            testFragments[j].isActual = false
                            moreFuse = true
                        }
                    }
                }
            }

            if (moreFuse) {
                testFragments.filter { it.isActual }.forEach { Utils.updateByMass(it, world) }
            }
        }

        testEnemies.filter { !it.isActual }.forEach { testEnemies.remove(it) }

        testFragments.filter { it.needUpdateMass }.forEach {
            Utils.updateByMass(it, world)
            it.needUpdateMass = false
        }
        testEnemies.filter { it.needUpdateMass }.forEach {
            Utils.updateByMass(it, world)
            it.needUpdateMass = false
        }

        return eatScore
    }

    private fun getScore(fragments: List<TestPlayer>, enemies: List<TestPlayer>): Float {

        val victims = mutableListOf<Pair<TestPlayer, TestPlayer>>()
        val hunters = mutableListOf<Pair<TestPlayer, TestPlayer>>()

        fragments.forEach { f ->
            enemies.forEach {
                if (Utils.canEatPotential(f, it)) {
                    victims.add(Pair(f, it))
                } else {
                    hunters.add(Pair(it, f))
                }
            }
        }

        val victimsCount = victims.groupingBy { it.second.id }.eachCount()
        val huntersCount = hunters.groupingBy { it.second.id }.eachCount()

        val visionFactor = if (fragments.size == 1) 1f else sqrt(fragments.size.toFloat())
        var victimsPoint = 0f
        victims.forEach {
            val allDist = it.first.r * 4 * visionFactor + 10
            val dist = max(allDist - Utils.dist(it.first, it.second), 0f)
            victimsPoint += ((dist*dist) / allDist) / (victimsCount[it.second.id]?.toFloat() ?: 1f) * it.second.m
        }

        var huntersPoint = 0f
        hunters.forEach {
            val allDist = if (it.first.id?.startsWith("f") == true) {
                it.first.r
            } else {
                it.first.r * 4 + 10
            }
            val dist = max(allDist - Utils.dist(it.first, it.second), 0f)
            huntersPoint += ((dist*dist) / allDist) / (huntersCount[it.second.id]?.toFloat() ?: 1f) * it.second.m
        }

        if (fragments.size == 1) {
            huntersPoint *= 2
        }

        return victimsPoint - huntersPoint
    }

    private fun getScoreForEatFragment(fragment: TestPlayer, enemies: List<TestPlayer>): Float {

        val hunters = mutableListOf<Pair<TestPlayer, TestPlayer>>()

        enemies.forEach {
            if (Utils.canEatPotential(it, fragment)) {
                hunters.add(Pair(it, fragment))
            }
        }

        val huntersCount = hunters.groupingBy { it.second.id }.eachCount()

        var huntersPoint = 0f
        hunters.forEach {
            val allDist = it.first.r * 4 + 10
            val dist = max(allDist - Utils.dist(it.first, it.second), 0f)
            huntersPoint += ((dist*dist) / allDist) / (huntersCount[it.second.id]?.toFloat() ?: 1f) * it.second.m
        }

        return -1 * huntersPoint
    }

    private fun getScoreForEatEnemy(enemy: TestPlayer, fragments: List<TestPlayer>): Float {

        val victims = mutableListOf<Pair<TestPlayer, TestPlayer>>()

        fragments.forEach {
            if (Utils.canEatPotential(it, enemy)) {
                victims.add(Pair(it, enemy))
            }
        }

        val victimsCount = victims.groupingBy { it.second.id }.eachCount()

        val visionFactor = if (fragments.size == 1) 1f else sqrt(fragments.size.toFloat())
        var victimsPoint = 0f
        victims.forEach {
            val allDist = it.first.r * 4 * visionFactor + 10
            val dist = max(allDist - Utils.dist(it.first, it.second), 0f)
            victimsPoint += ((dist*dist) / allDist) / (victimsCount[it.second.id]?.toFloat() ?: 1f) * it.second.m
        }

        return victimsPoint
    }
}