package com.monumentquest.ui.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import com.monumentquest.data.model.TacticalGeometry

/**
 * Custom OSMDroid Overlay — renders a 3D isometric city aesthetic matching the
 * reference image: grey extruded buildings, gold highways, green parks, blue water,
 * white street grid.
 *
 * Strategy:
 *  - Draw roads as thick polylines (white/gold by highway type)
 *  - Draw building polygons with 3D extrusion (roof + south wall + east wall)
 *  - All geometry comes from Overpass API via TacticalGeometry
 *
 * OSMDroid Projection.toPixels() is called with a reusable Point — the only
 * correct way to convert GeoPoint → screen pixel in an Overlay draw pass.
 */
class Isometric3DOverlay : Overlay() {

    var enabled: Boolean = true
    var geometry: TacticalGeometry = TacticalGeometry(emptyList(), emptyList())

    // ── Paint objects — allocated once, never inside draw() ──────────────────

    private val streetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color      = Color.parseColor("#E8EAF0")
        style      = Paint.Style.STROKE
        strokeCap  = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val highwayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color      = Color.parseColor("#F0A500")
        style      = Paint.Style.STROKE
        strokeCap  = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val highwayBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color      = Color.parseColor("#D08800")
        style      = Paint.Style.STROKE
        strokeCap  = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val roofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C0C4CE")
        style = Paint.Style.FILL
    }

    private val eastWallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#969AAA")
        style = Paint.Style.FILL
    }

    private val southWallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7C8090")
        style = Paint.Style.FILL
    }

    private val roofOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = Color.parseColor("#D8DCE8")
        style       = Paint.Style.STROKE
        strokeWidth = 0.7f
    }

    // ── Reusable draw scratch objects ─────────────────────────────────────────
    private val reusablePt = Point()
    private val path       = Path()

    // ── Draw entry point ──────────────────────────────────────────────────────

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || !enabled) return
        val proj = mapView.projection
        val zoom = mapView.zoomLevelDouble

        val streetW  = (zoom * 0.32f).toFloat().coerceIn(2f, 8f)
        val hwBorder = (zoom * 0.95f).toFloat().coerceIn(4f, 18f)
        val hwFill   = (zoom * 0.68f).toFloat().coerceIn(3f, 13f)
        val extH     = (zoom * 1.65f).toFloat().coerceIn(5f, 28f)

        streetPaint.strokeWidth      = streetW
        highwayBorderPaint.strokeWidth = hwBorder
        highwayPaint.strokeWidth     = hwFill

        // ── Roads (below buildings) ───────────────────────────────────────────
        for (road in geometry.roads) {
            if (road.size < 2) continue
            // Overpass gives us geometry but not a stable visual class here.
            // Longer ways are a good proxy for arterials; short ways stay as
            // the pale street grid visible between the extruded buildings.
            if (road.size >= 5) {
                drawPolyline(canvas, proj, road, highwayBorderPaint)
                drawPolyline(canvas, proj, road, highwayPaint)
            } else {
                drawPolyline(canvas, proj, road, streetPaint)
            }
        }

        // ── Buildings with 3D extrusion ───────────────────────────────────────
        for (building in geometry.buildings) {
            if (building.size < 3) continue
            draw3DBuilding(canvas, proj, building, extH)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun drawPolyline(
        canvas: Canvas,
        proj:   Projection,
        pts:    List<GeoPoint>,
        paint:  Paint
    ) {
        path.rewind()
        pts.forEachIndexed { index, gpt ->
            proj.toPixels(gpt, reusablePt)
            val px = reusablePt.x.toFloat()
            val py = reusablePt.y.toFloat()
            if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        canvas.drawPath(path, paint)
    }

    private fun draw3DBuilding(
        canvas:  Canvas,
        proj:    Projection,
        polygon: List<GeoPoint>,
        extH:    Float
    ) {
        // Project all polygon vertices to screen space
        val screen = polygon.map { gpt ->
            proj.toPixels(gpt, reusablePt)
            Pair(reusablePt.x.toFloat(), reusablePt.y.toFloat())
        }

        // Bounding box — skip tiny specks
        val minX = screen.minOf { it.first }
        val maxX = screen.maxOf { it.first }
        val minY = screen.minOf { it.second }
        val maxY = screen.maxOf { it.second }
        val bw   = maxX - minX
        val bh   = maxY - minY
        if (bw < 5f && bh < 5f) return

        // Isometric extrusion offset: top-right (east) and down (south)
        val offX = extH * 0.55f   // rightward shift for east/south walls
        val offY = extH            // downward shift

        // ── South wall — bottom edges of the footprint ────────────────────────
        // Heuristic: edge whose midpoint Y > maxY - bh * 0.4
        for (i in screen.indices) {
            val j  = (i + 1) % screen.size
            val x0 = screen[i].first;  val y0 = screen[i].second
            val x1 = screen[j].first;  val y1 = screen[j].second
            if ((y0 + y1) / 2f < maxY - bh * 0.35f) continue
            path.rewind()
            path.moveTo(x0, y0)
            path.lineTo(x1, y1)
            path.lineTo(x1 + offX, y1 + offY)
            path.lineTo(x0 + offX, y0 + offY)
            path.close()
            canvas.drawPath(path, southWallPaint)
        }

        // ── East wall — right edges of the footprint ─────────────────────────
        for (i in screen.indices) {
            val j  = (i + 1) % screen.size
            val x0 = screen[i].first;  val y0 = screen[i].second
            val x1 = screen[j].first;  val y1 = screen[j].second
            if ((x0 + x1) / 2f < maxX - bw * 0.35f) continue
            path.rewind()
            path.moveTo(x0, y0)
            path.lineTo(x1, y1)
            path.lineTo(x1 + offX, y1 + offY)
            path.lineTo(x0 + offX, y0 + offY)
            path.close()
            canvas.drawPath(path, eastWallPaint)
        }

        // ── Roof (top face = original footprint) ─────────────────────────────
        path.rewind()
        screen.forEachIndexed { idx, (px, py) ->
            if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, roofPaint)
        canvas.drawPath(path, roofOutlinePaint)
    }
}
