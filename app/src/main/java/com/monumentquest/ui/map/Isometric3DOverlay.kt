package com.monumentquest.ui.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.PointF
import com.monumentquest.data.model.AreaFeature
import com.monumentquest.data.model.BuildingFootprint
import com.monumentquest.data.model.RoadSegment
import com.monumentquest.data.model.TacticalGeometry
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

/**
 * Paints [TacticalGeometry] as a stylised 3D isometric city matching the reference art:
 * slate-grey extruded buildings, raised golden highways, green lawns, and sky blue canals.
 */
class Isometric3DOverlay : Overlay() {

    var geometry: TacticalGeometry? = null
    var isOverlayEnabled: Boolean = true

    // Direction + scale of 3D extrusion per floor level
    var extrudeDxPerLevel = -2.2f
    var extrudeDyPerLevel = -5.8f
    var maxLevelsForHeight = 16
    var minZoomForExtrusion = 10.0

    // ── Paint palette matching the reference 3D isometric aesthetic ──
    private val roofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#8C97A8")
    }
    private val roofStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
        color = Color.parseColor("#E2E7F2")
    }
    private val southWallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#6D788A")
    }
    private val eastWallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#556072")
    }
    private val flatBuildingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#A3ACBC")
    }

    private val roadCasingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#D48806")
    }
    private val roadFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#F5A623")
    }

    private val parkFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#A2DC77")
    }
    private val parkStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.parseColor("#82C455")
    }

    private val waterFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#55B5E6")
    }
    private val waterStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.parseColor("#3F9DCE")
    }

    // Scratch objects reused every frame
    private val scratchPoint = Point()
    private val path = Path()
    private val scratchScreenPts = ArrayList<PointF>(16)

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || !isOverlayEnabled) return
        val geo = geometry ?: return
        val projection = mapView.projection

        // 1. Water features (canals/riverbanks)
        geo.water.forEach { drawArea(canvas, projection, it.outline, waterFillPaint, waterStroke) }

        // 2. Parks / Grass lawns
        geo.parks.forEach { drawArea(canvas, projection, it.outline, parkFillPaint, parkStroke) }

        // 3. Roads / Golden Overpasses
        for (road in geo.roads) if (road.isMajor) drawRoad(canvas, projection, road)

        // 4. Extruded 3D Buildings (Painter's algorithm: depth sorted)
        val zoom = mapView.zoomLevelDouble
        val extrude = zoom >= minZoomForExtrusion
        val heightScale = (((zoom - 10.0) / 6.0).coerceIn(0.4, 1.8)).toFloat()

        geo.buildings
            .sortedBy { footprintDepth(projection, it.outline) }
            .forEach { drawBuilding(canvas, projection, it, extrude, heightScale) }
    }

    private fun toScreen(projection: Projection, gp: GeoPoint): PointF {
        projection.toPixels(gp, scratchPoint)
        return PointF(scratchPoint.x.toFloat(), scratchPoint.y.toFloat())
    }

    private fun footprintDepth(projection: Projection, outline: List<GeoPoint>): Float {
        var maxY = Float.NEGATIVE_INFINITY
        for (gp in outline) {
            val p = toScreen(projection, gp)
            if (p.y > maxY) maxY = p.y
        }
        return maxY
    }

    private fun drawBuilding(
        canvas: Canvas,
        projection: Projection,
        building: BuildingFootprint,
        extrude: Boolean,
        heightScale: Float
    ) {
        if (building.outline.size < 3) return
        scratchScreenPts.clear()
        for (gp in building.outline) scratchScreenPts.add(toScreen(projection, gp))

        if (!extrude) {
            drawPolygon(canvas, scratchScreenPts, flatBuildingPaint, null)
            return
        }

        val levels = building.levels.coerceIn(1, maxLevelsForHeight)
        val dx = extrudeDxPerLevel * levels * heightScale
        val dy = extrudeDyPerLevel * levels * heightScale

        val n = scratchScreenPts.size
        var cx = 0f
        var cy = 0f
        for (p in scratchScreenPts) { cx += p.x; cy += p.y }
        cx /= n; cy /= n

        // Render extruded 3D walls facing camera
        for (i in 0 until n) {
            val a = scratchScreenPts[i]
            val b = scratchScreenPts[(i + 1) % n]
            val midX = (a.x + b.x) / 2f
            val midY = (a.y + b.y) / 2f
            val outX = midX - cx
            val outY = midY - cy
            val facingDot = outX * -dx + outY * -dy
            if (facingDot <= 0f) continue

            val wallColor = if (outX > 0) eastWallPaint else southWallPaint
            val ra = PointF(a.x + dx, a.y + dy)
            val rb = PointF(b.x + dx, b.y + dy)
            path.reset()
            path.moveTo(a.x, a.y)
            path.lineTo(b.x, b.y)
            path.lineTo(rb.x, rb.y)
            path.lineTo(ra.x, ra.y)
            path.close()
            canvas.drawPath(path, wallColor)
        }

        // Render elevated 3D Roof
        val roofPts = ArrayList<PointF>(n)
        for (p in scratchScreenPts) roofPts.add(PointF(p.x + dx, p.y + dy))
        drawPolygon(canvas, roofPts, roofPaint, roofStroke)
    }

    private fun drawRoad(canvas: Canvas, projection: Projection, road: RoadSegment) {
        if (road.points.size < 2) return
        path.reset()
        road.points.forEachIndexed { i, gp ->
            val p = toScreen(projection, gp)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        val fillWidth = 16f
        roadCasingPaint.strokeWidth = fillWidth + 8f
        roadFillPaint.strokeWidth = fillWidth
        canvas.drawPath(path, roadCasingPaint)
        canvas.drawPath(path, roadFillPaint)
    }

    private fun drawArea(
        canvas: Canvas,
        projection: Projection,
        outline: List<GeoPoint>,
        fill: Paint,
        stroke: Paint?
    ) {
        if (outline.size < 3) return
        val pts = ArrayList<PointF>(outline.size)
        for (gp in outline) pts.add(toScreen(projection, gp))
        drawPolygon(canvas, pts, fill, stroke)
    }

    private fun drawPolygon(canvas: Canvas, pts: List<PointF>, fill: Paint, stroke: Paint?) {
        if (pts.size < 3) return
        path.reset()
        path.moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) path.lineTo(pts[i].x, pts[i].y)
        path.close()
        canvas.drawPath(path, fill)
        stroke?.let { canvas.drawPath(path, it) }
    }
}
