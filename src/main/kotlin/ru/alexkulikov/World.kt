package ru.alexkulikov

import org.json.JSONObject

class World(config: JSONObject) {
    val width = config.getInt("GAME_WIDTH")
    val height = config.getInt("GAME_HEIGHT")
    val ticks = config.getInt("GAME_TICKS")
    val foodMass = config.getFloat("FOOD_MASS")
    val maxFragment = config.getInt("MAX_FRAGS_CNT")
    val ticksToFusion = config.getInt("TICKS_TIL_FUSION")
    val virusRadius = config.getFloat("VIRUS_RADIUS")
    val virusSplitMass = config.getFloat("VIRUS_SPLIT_MASS")
    val viscosity = config.getFloat("VISCOSITY")
    val inertion = config.getFloat("INERTION_FACTOR")
    val speed = config.getFloat("SPEED_FACTOR")
}