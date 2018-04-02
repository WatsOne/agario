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
        val prevEnemyPositions = mutableMapOf<String, Pair<Float, Float>>()

        while (true) {
            val tickData = JSONObject(readLine())
            data.parse(tickData, world)

            if (data.me.isEmpty()) return

            if (!data.enemy.isEmpty()) {
                val nearestPair = Utils.getNearestMeEnemyPair(data.me, data.enemy)

                val enemy = nearestPair.second
                val player = nearestPair.first

                val prevEnemyPos = prevEnemyPositions[enemy.id]

                if (prevEnemyPos == null) {
                    prevEnemyPositions[enemy.id] = Pair(enemy.x, enemy.y)
                } else {
                    val enemySx = enemy.x - prevEnemyPos.first
                    val enemySy = enemy.y - prevEnemyPos.second

                    prevEnemyPositions[enemy.id] = Pair(enemy.x, enemy.y)

                    if (Utils.canEatPotential(enemy, player)) {
//                        logger.trace { "$tick: RUN!" }
                        println(doRun(player, enemy, enemySx, enemySy, world))
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
        val player = data.me[0]

        return if (idlePoint == null) {
            getNewIdleRotatePoint(player, world)
        } else {
            val dist = Utils.dist(data.me[0].x, data.me[0].y, idlePoint.first, idlePoint.second)
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

    private fun doRun(player: Me, enemy: Enemy, enemySx: Float, enemySy: Float, world: World): JSONObject {
        val distance = mutableMapOf<Pair<Float, Float>, Float>()
        Utils.rotatingPoints(player, world).forEach { d ->
            val playerTest = TestPlayer(player)
            val enemyTest = TestPlayer(enemy, enemySx, enemySy)
            var penaltyPoints = 0
            for (i in 1..30) {
                Utils.applyDirect(d.first, d.second, playerTest, world)
                Utils.applyDirect(playerTest.x, playerTest.y, enemyTest, world)
                Utils.move(playerTest, world)
                Utils.move(enemyTest, world)
                if (Utils.canEat(enemyTest, playerTest)) {
                    penaltyPoints -= 100
                }
            }
            distance[d] = Utils.dist(playerTest, enemyTest) + penaltyPoints
        }

        val minDist = distance.maxBy { it.value }
        val xMax = minDist?.key?.first ?: 0f
        val yMax = minDist?.key?.second ?: 0f

        return JSONObject(mapOf("X" to xMax, "Y" to yMax))
    }

    private fun doEat(data: Data, world: World): JSONObject {

        val start = System.currentTimeMillis()
        val total = mutableMapOf<Pair<Float, Float>, Float>()
        var oper = 0

        val me = data.me[0]

        Utils.rotatingPoints(me, world).forEach { d ->
            val testPlayer = TestPlayer(me)
            val testFoods = data.food.map { TestFood(it) }
            var eat = 0

            var ppt = 0f
            var tick = 0

            while (Utils.dist(testPlayer.x, testPlayer.y, d.first, d.second) > testPlayer.r) {
                Utils.applyDirect(d.first, d.second, testPlayer, world)
                testFoods.forEach { f ->
                    if (!f.eaten && Utils.canEat(testPlayer, f)) {
                        eat++
                        f.eaten = true
                        testPlayer.m += world.foodMass
                        testPlayer.r = 2 * sqrt(testPlayer.m)
                        ppt = eat / tick.toFloat()
                    }
                }
                Utils.move(testPlayer, world)
                tick++
                oper++
            }
            total[d] = ppt
        }

        val maxTotal = total.maxBy { it.value }

        val xMax = maxTotal?.key?.first ?: 0f
        val yMax = maxTotal?.key?.second ?: 0f


//        logger.trace { "$tick MAX: $maxTotal calc: ${System.currentTimeMillis() - start} ms; oper: $oper. Radius: ${data.me[0].r}" }

        return JSONObject(mapOf("X" to xMax, "Y" to yMax))
    }
}