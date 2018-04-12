import org.json.JSONObject

class World(config: JSONObject) {
    val width = config.getInt("GAME_WIDTH")
    val height = config.getInt("GAME_HEIGHT")
    val ticks = config.getInt("GAME_TICKS")
    val foodMass = config.getDouble("FOOD_MASS").toFloat()
    val maxFragment = config.getInt("MAX_FRAGS_CNT")
    val ticksToFusion = config.getInt("TICKS_TIL_FUSION")
    val virusRadius = config.getDouble("VIRUS_RADIUS").toFloat()
    val virusSplitMass = config.getDouble("VIRUS_SPLIT_MASS").toFloat()
    val viscosity = config.getDouble("VISCOSITY").toFloat()
    val inertion = config.getDouble("INERTION_FACTOR").toFloat()
    val speed = config.getDouble("SPEED_FACTOR").toFloat()
}