package ru.alexkulikov

import mu.KLogging
import org.json.JSONObject

class Strategy {
    companion object: KLogging()
    var tick = 1

    fun go() {
        val config = JSONObject(readLine())
        val world = World(config)
        val data = Data()

        while (true) {
            val tickData = JSONObject(readLine())
            data.parse(tickData)

            val move = onTick(data, world)
            println(move)
            tick++
        }
    }

    fun onTick(data: Data, world: World): JSONObject {
        val x = data.food.getOrNull(0)?.x ?: world.width / 2
        val y = data.food.getOrNull(0)?.y ?: world.height / 2

        return JSONObject(mapOf("X" to x, "Y" to y))
    }
}