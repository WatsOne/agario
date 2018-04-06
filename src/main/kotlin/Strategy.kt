//import mu.KLogging
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.sqrt

class Strategy {
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

                //считаем дистанции до врагов, которые могут сожрать
                val enemyDist = Utils.getDistToEnemies(data.me, data.enemy)

                //моделирем 10 тиков движения по векторам скоростей
                val testFragments = enemyDist.keys.map { getById(it, data.me) }.map { TestPlayer(it) }
                val testEnemies = enemyDist.values.flatten()
                        .map { getById(it.first, data.enemy) }
                        .map { TestPlayer(it, enemySpeedVectors[it.id]?.first ?: 0f, enemySpeedVectors[it.id]?.second ?: 0f) }

                repeat(10, {
                    testFragments.forEach {
                        Utils.applyDirect(it.x + it.sx *2 , it.y + it.sy * 2, it, world)
                        Utils.move(it, world)
                    }
                    testEnemies.forEach {
                        Utils.applyDirect(it.x + it.sx *2 , it.y + it.sy * 2, it, world)
                        Utils.move(it, world)
                    }
                })
                val newDist = Utils.getDistToEnemiesTest(testFragments, testEnemies)

                //считаем разницу дистанций и выбираем максимальную (при наличии)
                var maxDeltaDist = 0f
                var dangerPair: Pair<String, String>? = null

                enemyDist.forEach { d ->
                    d.value.forEach {
                        val deltaDist = it.second - (newDist[d.key + it.first] ?: it.second)
                        if (deltaDist > maxDeltaDist) {
                            maxDeltaDist = deltaDist
                            dangerPair = Pair(d.key, it.first)
                        }
                    }
                }

                //если такая имеется то начинаем убегать
                if (maxDeltaDist > 0) {
                    val enemies = enemySpeedVectors.filter { it.value != null }.map { e ->
                        TestPlayer(data.enemy.filter { it.id == e.key}[0], e.value?.first ?: 0f, e.value?.second ?: 0f)
                    }
                    println(doRun(getById(dangerPair?.first ?: "", data.me), enemies, world))
                    continue
                }

                //иначе ищем ближайшую еду
                val nearestPair = Utils.getNearestMeFoodPair(data.me, data.enemy)
                if (nearestPair != null) {
                    val food = nearestPair.second
                    val player = nearestPair.first

                    val nearestFoodSpeedVector = enemySpeedVectors[food.id]

                    if (nearestFoodSpeedVector == null) {
                        if (canSplitStrike(player, food, 0f, 0f, data, world)) {
                            println(JSONObject(mapOf("X" to food.x, "Y" to food.y, "Split" to true)))
                        } else {
                            println(JSONObject(mapOf("X" to food.x, "Y" to food.y)))
                        }
                        continue
                    }
                    else {
                        //охотимся
                        if (canSplitStrike(player, food, nearestFoodSpeedVector.first, nearestFoodSpeedVector.second, data, world)) {
                            println(JSONObject(mapOf("X" to food.x, "Y" to food.y, "Split" to true)))
                            continue
                        }

                        val overtakePosition = overtakeEnemy(player, food, nearestFoodSpeedVector.first, nearestFoodSpeedVector.second, world)
                        println(overtakePosition)
                        continue
                    }
                }
            }

            if (data.food.isEmpty()) {
                idlePoint = getIdlePoint(data, world, idlePoint)
                println(JSONObject(mapOf("X" to idlePoint.first, "Y" to idlePoint.second)))
            } else {
                idlePoint = null
                println(doEat(data, world))
            }

//            println(JSONObject(mapOf("X" to world.width / 2, "Y" to world.height / 2)))
            tick++
        }
    }

    private fun canSplitStrike(player: Me, enemy: Enemy, eSx: Float, eSy: Float, data: Data, world: World): Boolean {
        if (Utils.canSplit(player, data.me.size, world)) {
            val splitPlayer = Utils.split(player)
            if (Utils.canEatPotentialForHunting(splitPlayer, enemy)) {
                return canSplitOvertakeEnemy(splitPlayer, enemy, eSx, eSy, world)
            }
        }

        return false
    }

    private fun canSplitOvertakeEnemy(split: TestPlayer, enemy: Enemy, eSx: Float, eSy: Float, world: World): Boolean {
        val testEnemy = TestPlayer(enemy, eSx, eSy)
        val dist = Utils.dist(split, testEnemy)
        while (split.isFast) {
            Utils.applyDirect(testEnemy.x, testEnemy.y, split, world)
            Utils.applyDirect(testEnemy.x + eSx * 2, testEnemy.y + eSy * 2, testEnemy, world)
            Utils.move(split, world)
            Utils.move(testEnemy, world)

            if (Utils.canEat(split, testEnemy)) {
                return true
            }
        }

        val predictDist = Utils.dist(split, testEnemy)
        return predictDist < dist
    }

    private fun overtakeEnemy(player: Me, enemy: Enemy, eSx: Float, eSy: Float, world: World): JSONObject {
        val dist = mutableMapOf<Pair<Float, Float>, Float>()

        Utils.rotatingPoints(player, world).forEach { d ->
            val testPlayer = TestPlayer(player)
            val testEnemy = TestPlayer(enemy, eSx, eSy)

            var canEat = false
            for (i in 1..15) {
                Utils.applyDirect(d.first, d.second, testPlayer, world)
                Utils.applyDirect(testEnemy.x + testPlayer.sx * 2, testEnemy.y + testPlayer.sy * 2, testEnemy, world)
                Utils.move(testPlayer, world)
                Utils.move(testEnemy, world)

                if (Utils.canEat(testPlayer, testEnemy)) {
                    canEat = true
                }
            }

            dist[Pair(d.first, d.second)] = if (canEat) -100f else Utils.dist(testPlayer, testEnemy)
        }

        val minDist = dist.minBy { it.value }
        return JSONObject(mapOf("X" to minDist?.key?.first, "Y" to minDist?.key?.second))
    }

    private fun getIdlePoint(data: Data, world: World, idlePoint: Pair<Float, Float>?): Pair<Float, Float> {
        val player = getLeaderFragment(data.me)

        return if (idlePoint == null) {
            getNewIdleRotatePoint(player, world, PI.toFloat() / 20)
        } else {
            val dist = Utils.dist(player.x, player.y, idlePoint.first, idlePoint.second)
            if (dist < player.r) {
                val newIdle = getNewIdleRotatePoint(player, world, PI.toFloat() / 25)
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
        return Utils.rotate(player.x, player.y, player.r, player.r + 40f, currentAngle + angle, world)
    }

    private fun doRun(player: Me, enemies: List<TestPlayer>, world: World): JSONObject {
        val distance = mutableMapOf<Pair<Float, Float>, Float>()

        Utils.rotatingPoints(player, world).forEach { d ->
            val playerTest = TestPlayer(player)
            var penaltyPoints = 0
            val testEnemies = enemies.map { TestPlayer(it) }
            repeat(15, {
                Utils.applyDirect(d.first, d.second, playerTest, world)
                testEnemies.forEach { Utils.applyDirect(player.x, playerTest.y, it, world) }
                Utils.move(playerTest, world)
                testEnemies.forEach { Utils.move(it, world) }

                testEnemies.forEach {
                    if (Utils.canEat(it, playerTest)) {
                        penaltyPoints -= 100
                    }

                    //TODO рассеивающую функцию бы сюда
                    penaltyPoints -= if (Utils.dist(it, playerTest) < playerTest.r + it.r) 50 else 0
                }
            })

            val dist = testEnemies.map { Utils.dist(it, playerTest) }.min() ?: 0f
            distance[d] = dist + penaltyPoints
        }

        val maxPoint = distance.maxBy { it.value }
        if (maxPoint?.value ?: 0f < 0) {
            return JSONObject(mapOf("X" to maxPoint?.key?.first, "Y" to maxPoint?.key?.second, "Split" to true))
        }

        return JSONObject(mapOf("X" to maxPoint?.key?.first, "Y" to maxPoint?.key?.second))
    }

    private fun doEat(data: Data, world: World): JSONObject {

        val start = System.currentTimeMillis()
        val total = mutableMapOf<Pair<Float, Float>, Float>()
        var oper = 0

        val me = getLeaderFragment(data.me)
        val testFoods = data.food.map { TestFood(it) }

        Utils.rotatingPoints(me, world).forEach { d ->
            val testPlayer = TestPlayer(me)
            testFoods.forEach { it.eaten = false }

            var eaten = testFoods.size
            var eat = 0

            var ppt = 0f
            var tick = 0

            while (Utils.dist(testPlayer.x, testPlayer.y, d.first, d.second) > testPlayer.r && eaten > 0 && tick <= 40) {

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
        return JSONObject(mapOf("X" to max.first, "Y" to max.second))
    }

    private fun getMaxScore(scoreMap: Map<Pair<Float, Float>, Float>): Pair<Float, Float> =
            scoreMap.maxBy { it.value }?.key ?: Pair(0f, 0f)

    private fun getLeaderFragment(me: List<Me>): Me {
        return me.minBy { it.id.toFloat() } ?: me[0]
    }

    private fun getById(id: String, me: List<Me>): Me {
        return me.find { it.id == id } ?: me[0]
    }

    private fun getById(id: String, enemies: List<Enemy>): Enemy {
        return enemies.find { it.id == id } ?: enemies[0]
    }
}