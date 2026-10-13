package universe.xanny.app.render

import kotlin.math.hypot
import kotlin.math.ln

/** Pure preview touch math; independent of Android UI and GL. */
object CameraGestureMath {
    fun pointerSpacing(x0: Float, y0: Float, x1: Float, y1: Float): Float {
        require(listOf(x0,y0,x1,y1).all { it.isFinite() })
        return hypot(x1 - x0, y1 - y0)
    }
    fun zoomDelta(previousSpacing: Float, currentSpacing: Float, currentDistance: Float): Float {
        require(previousSpacing.isFinite() && currentSpacing.isFinite() && currentDistance.isFinite())
        if (previousSpacing <= 0f || currentSpacing <= 0f) return 0f
        return (-ln(currentSpacing / previousSpacing) * currentDistance).coerceIn(-3f,3f)
    }
}
