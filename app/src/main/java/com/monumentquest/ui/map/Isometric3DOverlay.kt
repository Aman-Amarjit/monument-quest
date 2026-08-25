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
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Renders real OSM geometry (buildings, roads, parks, water) with high-contrast
 * dark theme styling (glowing amber roads, emerald parks, vibrant cyan water,
 * slate buildings) so map features are immediately clear.
 */
class Isometric3DOverlay : Overlay() {

    var geometry: TacticalGeometry? = null
    var isOverlayEnabled: Boolean = true
    var is3dExtrusionEnabled: Boolean = false

    private val minZoomForExtrusion = 12.0

    // ── High-Contrast Paints ──────────────────────────────────────────────────
    private val roofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.parseColor("#334155") }
    private val roofStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.parseColor("#64748B") }
    private val southWallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.parseColor("#1E293B") }
    private val southWallStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.parseColor("#475569") }
    private val eastWallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.parseColor("#172033") }
    private val eastWallStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.parseColor("#334155") }
    private val flatBuildingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.parseColor("#1E293B") }
    private val flatBuildingStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.parseColor("#475569") }

    // Vibrant Glowing Roads
    private val roadCasingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND; color = Color.parseColor("#B45309") }
    private val roadFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND; color = Color.parseColor("#F59E0B") }
    private val minorRoadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#FBBF24") }

    // Parks & Water
    private val parkFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.parseColor("#14532D") }
    private val parkStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.parseColor("#22C55E") }
    private val waterFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.parseColor("#0369A1") }
    private val waterStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.parseColor("#38BDF8") }

    private val scratchPoint = Point()
    private val path = Path()
    private val scratchPts = ArrayList<PointF>(24)

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || !isOverlayEnabled) return
        val geo  = geometry ?: return
        val proj = mapView.projection
        val zoom = mapView.zoomLevelDouble

        val zoomScale = 2.0.pow(zoom - 15.0).coerceIn(0.3, 6.0).toFloat()
        val majorFillW   = 8f  * zoomScale
        val majorCasingW = 12f * zoomScale
        val minorW       = 4.5f * zoomScale
        val edgeW = (1.2f * zoomScale).coerceIn(0.8f, 3f)

        roofStroke.strokeWidth         = edgeW
        southWallStroke.strokeWidth    = edgeW
        eastWallStroke.strokeWidth     = edgeW
        flatBuildingStroke.strokeWidth = edgeW
        parkStroke.strokeWidth         = edgeW
        waterStroke.strokeWidth        = edgeW

        val doExtrude = is3dExtrusionEnabled && zoom >= minZoomForExtrusion

        // Render features in painter order
        geo.water.forEach { drawArea(canvas, proj, it.outline, waterFillPaint, waterStroke) }
        geo.parks.forEach { drawArea(canvas, proj, it.outline, parkFillPaint,  parkStroke ) }
        for (r in geo.roads) if (!r.isMajor) drawRoad(canvas, proj, r, minorW,       minorW      )
        for (r in geo.roads) if ( r.isMajor) drawRoad(canvas, proj, r, majorCasingW, majorFillW  )
        geo.buildings
            .sortedBy { footprintDepth(proj, it.outline) }
            .forEach  { drawBuilding(canvas, proj, it, doExtrude, zoomScale) }
    }

    private fun toScreen(proj: Projection, gp: GeoPoint): PointF {
        proj.toPixels(gp, scratchPoint)
        return PointF(scratchPoint.x.toFloat(), scratchPoint.y.toFloat())
    }

    private fun footprintDepth(proj: Projection, outline: List<GeoPoint>): Float {
        var maxY = Float.NEGATIVE_INFINITY
        for (gp in outline) { val p = toScreen(proj, gp); if (p.y > maxY) maxY = p.y }
        return maxY
    }

    private fun drawBuilding(
        canvas: Canvas, proj: Projection,
        b: BuildingFootprint, extrude: Boolean, zoomScale: Float
    ) {
        if (b.outline.size < 3) return
        scratchPts.clear()
        for (gp in b.outline) scratchPts.add(toScreen(proj, gp))
        if (!extrude) { drawPolygon(canvas, scratchPts, flatBuildingPaint, flatBuildingStroke); return }

        val heightPerLevel = 3.5f * zoomScale
        val buildingHeight = (b.levels.coerceIn(1, 40) * heightPerLevel).coerceIn(6f, 100f)

        val dx = 0f
        val dy = -buildingHeight

        val n = scratchPts.size
        var cx = 0f; var cy = 0f
        for (p in scratchPts) { cx += p.x; cy += p.y }
        cx /= n; cy /= n

        for (i in 0 until n) {
            val a = scratchPts[i]
            val b2 = scratchPts[(i + 1) % n]

            val edgeX = b2.x - a.x
            val midX = (a.x + b2.x) / 2f
            val midY = (a.y + b2.y) / 2f

            val isFrontFacing = edgeX > -0.1f || (midY > cy && edgeX >= 0f)
            if (!isFrontFacing && (midY < cy && edgeX <= 0f)) continue

            val isEast = midX > cx
            val ra = PointF(a.x + dx, a.y + dy)
            val rb = PointF(b2.x + dx, b2.y + dy)

            path.reset()
            path.moveTo(a.x, a.y)
            path.lineTo(b2.x, b2.y)
            path.lineTo(rb.x, rb.y)
            path.lineTo(ra.x, ra.y)
            path.close()

            canvas.drawPath(path, if (isEast) eastWallPaint else southWallPaint)
            canvas.drawPath(path, if (isEast) eastWallStroke else southWallStroke)
        }

        val roof = ArrayList<PointF>(n)
        for (p in scratchPts) roof.add(PointF(p.x + dx, p.y + dy))
        drawPolygon(canvas, roof, roofPaint, roofStroke)
    }

    private fun drawRoad(canvas: Canvas, proj: Projection, r: RoadSegment, casingW: Float, fillW: Float) {
        if (r.points.size < 2) return
        path.reset()
        r.points.forEachIndexed { i, gp -> val p = toScreen(proj, gp)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
        if (r.isMajor) {
            roadCasingPaint.strokeWidth = casingW; roadFillPaint.strokeWidth = fillW
            canvas.drawPath(path, roadCasingPaint); canvas.drawPath(path, roadFillPaint)
        } else {
            minorRoadPaint.strokeWidth = fillW; canvas.drawPath(path, minorRoadPaint)
        }
    }

    private fun drawArea(canvas: Canvas, proj: Projection, outline: List<GeoPoint>, fill: Paint, stroke: Paint?) {
        if (outline.size < 3) return
        val pts = ArrayList<PointF>(outline.size)
        for (gp in outline) pts.add(toScreen(proj, gp))
        drawPolygon(canvas, pts, fill, stroke)
    }

    private fun drawPolygon(canvas: Canvas, pts: List<PointF>, fill: Paint, stroke: Paint?) {
        if (pts.size < 3) return
        path.reset(); path.moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) path.lineTo(pts[i].x, pts[i].y)
        path.close(); canvas.drawPath(path, fill); stroke?.let { canvas.drawPath(path, it) }
    }
}
