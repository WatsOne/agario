import org.json.JSONObject
import java.math.BigDecimal

open class Circle(val x: Float, val y: Float, val r: Float)

class Me(val id: String, x: Float, y: Float, r: Float, val m: Float, val sx: Float, val sy: Float) : Circle(x, y, r)
class Food(x: Float, y: Float) : Circle(x, y, FOOD_RADIUS)
class Ejection(x: Float, y: Float) : Circle(x, y, EJECT_RADIUS)
class Virus(val id: String, x: Float, y: Float, val m: Float, r: Float) : Circle (x, y, r)
class Enemy(val id: String, x: Float, y: Float, r: Float, val m: Float) : Circle(x, y, r)

fun JSONObject.getFloat(value: String) = BigDecimal.valueOf(this.getDouble(value)).toFloat()