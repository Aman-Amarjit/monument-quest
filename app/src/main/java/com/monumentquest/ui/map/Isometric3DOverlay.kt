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
 * Paints [TacticalGeometry] as a stylised "extruded city" — grey building
 * blocks, gold arterial roads with a light casing, flat green parks and
 * blue water — to match the low-poly isometric look-book reference.
 *
 * OSMDroid has no real camera pitch, so the "3D" here is the classic 2.5D
 * trick: each building's roof polygon is its footprint shifted by a fixed
 * screen-space vector scaled by floor count, with wall quads filled in
 * between roof and footprint. Cheap, no GL dependency, reads as isometric
 * at a glance — especially once buildings sit close together like a real
 * city block.
 *
 * This is already wired up in MapScreen.kt:
 *   isometricOverlay.geometry = tacticalGeometry
 *   isometricOverlay.isOverlayEnabled = isAerialView
 *   map.overlays.add(minOf(1, map.overlays.size), isometricOverlay)
 */
class Isometric3DOverlay : Overlay() {

    var geometry: TacticalGeometry? = null
    var isOverlayEnabled: Boolean = true

    // Direction + scale of the fake extrusion, in screen px per floor.
    // Mostly vertical with a slight lean so roofs read as "lifted" rather
    // than just smeared upward. Tune these to taste.
    var extrudeDxPerLevel = -1.1f
    var extrudeDyPerLevel = -4.6f
    var maxLevelsForHeight = 9
    var minZoomForExtrusion = 15.3

    // ── Paint palette, kept as fields so draw() never allocates a Paint ──
    private val roofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#9AA3B4")
    }
    private val roofStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.4f
        color = Color.parseColor("#66FFFFFF")
    }
    private val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#727B8E")
    }
    private val flatBuildingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#A8B0C0")
    }

    private val roadCasingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#F5F3EE")
    }
    private val roadFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#F0A93B")
    }

    private val parkFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#9AD08A")
    }
    private val parkStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        color = Color.parseColor("#7CB86C")
    }

    private val waterFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#79C3EA")
    }
    private val waterStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        color = Color.parseColor("#57A9D6")
    }

    // Scratch objects reused every frame so draw() stays allocation-light.
    private val scratchPoint = Point()
    private val path = Path()
    private val scratchScreenPts = ArrayList<PointF>(16)

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || !isOverlayEnabled) return
        val geo = geometry ?: return
        val projection = mapView.projection

        // Ground layer first: water, then parks, then major roads.
        // Buildings always paint last so they sit above everything else.
        geo.water.forEach { drawArea(canvas, projection, it.outline, waterFillPaint, waterStroke) }
        geo.parks.forEach { drawArea(canvas, projection, it.outline, parkFillPaint, parkStroke) }
        for (road in geo.roads) if (road.isMajor) drawRoad(canvas, projection, road)

        val zoom = mapView.zoomLevelDouble
        val extrude = zoom >= minZoomForExtrusion
        val heightScale = (((zoom - 14.0) / 4.0).coerceIn(0.15, 1.4)).toFloat()

        geo.buildings
            .sortedBy { footprintDepth(projection, it.outline) }
            .forEach { drawBuilding(canvas, projection, it, extrude, heightScale) }
    }

    private fun toScreen(projection: Projection, gp: GeoPoint): PointF {
        projection.toPixels(gp, scratchPoint)
        return PointF(scratchPoint.x.toFloat(), scratchPoint.y.toFloat())
    }

    private fun footprintDepth(projection: Projection, outline: List<GeoPoint>): Float {
        // Painter's algorithm: buildings lower on screen are "nearer" the
        // viewer and must be drawn after (on top of) ones higher up.
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
            // Zoomed out too far for extrusion to read cleanly — flat
            // silhouette still gives the "city block" texture.
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

        // Only paint walls on the side of the building that faces the
        // viewer once the roof lifts away — edges whose midpoint sits
        // opposite the extrusion direction relative to the centroid.
        // This is winding-order independent, unlike a pure edge-normal test,
        // so it works regardless of how the source OSM way was wound.
        for (i in 0 until n) {
            val a = scratchScreenPts[i]
            val b = scratchScreenPts[(i + 1) % n]
            val midX = (a.x + b.x) / 2f
            val midY = (a.y + b.y) / 2f
            val outX = midX - cx
            val outY = midY - cy
            val facingDot = outX * -dx + outY * -dy
            if (facingDot <= 0f) continue

            val ra = PointF(a.x + dx, a.y + dy)
            val rb = PointF(b.x + dx, b.y + dy)
            path.reset()
            path.moveTo(a.x, a.y)
            path.lineTo(b.x, b.y)
            path.lineTo(rb.x, rb.y)
            path.lineTo(ra.x, ra.y)
            path.close()
            canvas.drawPath(path, wallPaint)
        }

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
        val fillWidth = 15f
        roadCasingPaint.strokeWidth = fillWidth + 7f
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
