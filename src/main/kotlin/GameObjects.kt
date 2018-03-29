import org.json.JSONObject
import java.math.BigDecimal

data class Me(val id: String, val x: Float, val y: Float, val r: Float, val m: Float, val sx: Float, val sy: Float)
data class Food(val x: Float, val y: Float)
data class Ejection(val x: Float, val y: Float)
data class Virus(val id: String, val x: Float, val y: Float, val m: Float)
data class Enemy(val id: String, val x: Float, val y: Float, val r: Float, val m: Float)

fun JSONObject.getFloat(value: String) = BigDecimal.valueOf(this.getDouble(value)).toFloat()