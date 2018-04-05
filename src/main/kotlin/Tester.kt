import org.json.JSONObject

fun main(args: Array<String>) {
    val me = TestPlayer(451.14120713157064f, 549.00902215424264f, 52.169423869964511f, 680.41219673100568f, -0.08044729192856355f, -1.6794237557392491f)
    val enemy = TestPlayer(266.20391653911815f, 412.42636234069289f, 16.187176817280985f, 65.506173328479747f, 0f, 0f)

    println(Utils.dist(me, enemy))
    val split = TestPlayer(me.x, me.y, me.r, me.m / 2, me.sx, me.sy, SPLIT_START_SPEED, Utils.getAngle(me.sx, me.sy), true)
    val json = JSONObject("{\"FOOD_MASS\":1.025750299989443,\"GAME_HEIGHT\":990,\"GAME_TICKS\":7500,\"GAME_WIDTH\":990,\"INERTION_FACTOR\":4.4668000220244419,\"MAX_FRAGS_CNT\":15,\"SPEED_FACTOR\":87.201072192801604,\"TICKS_TIL_FUSION\":258,\"VIRUS_RADIUS\":29.852991505276226,\"VIRUS_SPLIT_MASS\":73.3859755727796,\"VISCOSITY\":0.42297772114304855}\n")
    val world = World(json)

    var tick = 0
    while (split.isFast) {
        Utils.applyDirect(enemy.x, enemy.y, split, world)
        Utils.applyDirect(enemy.x, enemy.y, enemy, world)
        Utils.move(split, world)
        Utils.move(enemy, world)
        tick++
    }

    println(Utils.dist(split, enemy))
    println(tick)
}