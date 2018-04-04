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

                val nearestPair = Utils.getNearestMeEnemyPair(data.me, data.enemy)

                val enemy = nearestPair.second
                val player = nearestPair.first

                val nearestEnemySpeedVector = enemySpeedVectors[enemy.id]
                if (nearestEnemySpeedVector != null) {

                    if (Utils.canEatPotential(enemy, player)) {
//                        logger.trace { "$tick: RUN!" }
                        val start = System.currentTimeMillis()
                        val enemies = enemySpeedVectors.filter { it.value != null }.map { e ->
                            TestPlayer(data.enemy.filter { it.id == e.key}[0], e.value?.first ?: 0f, e.value?.second ?: 0f)
                        }
                        val res = doRun(player, enemies, world)
//                        logger.trace { "RUN: ${System.currentTimeMillis() - start} ms." }
                        println(res)
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

    private fun getIdlePoint(data: Data, world: World, idlePoint: Pair<Float, Float>?): Pair<Float, Float> {
        val player = getLeaderFragment(data.me)

        return if (idlePoint == null) {
            getNewIdleRotatePoint(player, world)
        } else {
            val dist = Utils.dist(player.x, player.y, idlePoint.first, idlePoint.second)
            if (dist < player.r + 1) {
                getNewIdleRotatePoint(player, world)
            } else {
                idlePoint
            }
        }
    }

    private fun getNewIdleRotatePoint(player: Me, world: World): Pair<Float, Float> {
        val currentAngle = Utils.getAngle(player.sx, player.sy)
        return Utils.rotate(player.x, player.y, player.r, player.r + 30f, currentAngle + PI.toFloat() / 20, world)
    }

    private fun doRun(player: Me, enemies: List<TestPlayer>, world: World): JSONObject {
        val distance = mutableMapOf<Pair<Float, Float>, Float>()

        Utils.rotatingPoints(player, world).forEach { d ->
            val playerTest = TestPlayer(player)
            var penaltyPoints = 0
            val testEnemies = enemies.map { TestPlayer(it) }
            repeat(40, {
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

            while (Utils.dist(testPlayer.x, testPlayer.y, d.first, d.second) > testPlayer.r && eaten > 0 && tick <= 70) {

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
}