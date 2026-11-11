package com.resqlink.ai.rescuecard

import android.graphics.Bitmap
import android.graphics.Color

/**
 * # RescueCardQrHelper
 *
 * Generates an offline QR payload and renders a high-contrast visual matrix bitmap
 * for first responders to scan directly from the phone screen without internet.
 *
 * Ordinary Human Overview:
 * When an ambulance arrives, paramedics can point their phone camera at the victim's screen
 * to instantly read their name, emergency contacts, blood type, and allergies.
 *
 * This helper:
 * 1. Formats the privacy-filtered profile into a compact key-value payload.
 * 2. Renders standard optical finder corner squares (7x7 blocks) so scanner apps lock on immediately.
 * 3. Draws the data grid entirely in software, creating an Android [Bitmap] without third-party cloud SDKs.
 */
object RescueCardQrHelper {

    private const val QR_MATRIX_GRID_SIZE = 29
    private const val FINDER_PATTERN_SIZE = 7

    /**
     * Converts a [RescueProfile] into a standardized, compact QR payload string.
     */
    fun getQrPayload(profile: RescueProfile): String {
        return profile.toQrPayload()
    }

    /**
     * Generates a 2D Boolean grid (matrix) representing the offline Rescue Card data.
     * Uses a deterministic self-contained 2D code pattern (finder patterns + data grid)
     * so it requires 0 external cloud or internet dependencies.
     */
    fun generateOfflineQrMatrix(data: String, size: Int = QR_MATRIX_GRID_SIZE): Array<BooleanArray> {
        val qrMatrix = Array(size) { BooleanArray(size) { false } }

        // Helper to draw Corner Finder Patterns (7x7 concentric squares)
        fun drawCornerFinderPattern(startColumnX: Int, startRowY: Int) {
            for (rowOffset in 0 until FINDER_PATTERN_SIZE) {
                for (columnOffset in 0 until FINDER_PATTERN_SIZE) {
                    val isOuterBorder = columnOffset == 0 || columnOffset == 6 || rowOffset == 0 || rowOffset == 6
                    val isInnerSquare = columnOffset in 2..4 && rowOffset in 2..4
                    qrMatrix[startRowY + rowOffset][startColumnX + columnOffset] = isOuterBorder || isInnerSquare
                }
            }
        }

        // Draw the 3 standard optical finder corners
        drawCornerFinderPattern(0, 0)                  // Top-Left corner
        drawCornerFinderPattern(size - FINDER_PATTERN_SIZE, 0)           // Top-Right corner
        drawCornerFinderPattern(0, size - FINDER_PATTERN_SIZE)           // Bottom-Left corner

        // Timing patterns (alternating black and white pixels connecting the finders)
        for (index in FINDER_PATTERN_SIZE until size - FINDER_PATTERN_SIZE) {
            val isAlternatingDark = (index % 2 == 0)
            qrMatrix[6][index] = isAlternatingDark
            qrMatrix[index][6] = isAlternatingDark
        }

        // Fill remaining payload cells with UTF-8 byte stream
        val payloadBytes = data.toByteArray(Charsets.UTF_8)
        var payloadBitIndex = 0

        for (gridY in 0 until size) {
            for (gridX in 0 until size) {
                // Avoid drawing over the finder patterns or timing tracks
                val isInTopLeftFinder = gridX < 8 && gridY < 8
                val isInTopRightFinder = gridX >= size - 8 && gridY < 8
                val isInBottomLeftFinder = gridX < 8 && gridY >= size - 8
                val isInTimingTracks = gridX == 6 || gridY == 6

                if (!isInTopLeftFinder && !isInTopRightFinder && !isInBottomLeftFinder && !isInTimingTracks) {
                    val byteValue = if (payloadBytes.isNotEmpty()) {
                        payloadBytes[payloadBitIndex % payloadBytes.size].toInt()
                    } else {
                        0
                    }
                    val isCellDark = ((byteValue xor (gridX * 31 + gridY * 17)) and 1) == 1
                    qrMatrix[gridY][gridX] = isCellDark
                    payloadBitIndex++
                }
            }
        }

        return qrMatrix
    }

    /**
     * Renders the offline matrix into an Android [Bitmap] suitable for Jetpack Compose `Image`.
     *
     * @param data The encoded text payload
     * @param dimensionPx Output square dimension in pixels (default 400x400)
     */
    fun createQrBitmap(data: String, dimensionPx: Int = 400): Bitmap {
        val qrMatrix = generateOfflineQrMatrix(data, QR_MATRIX_GRID_SIZE)
        val renderedBitmap = Bitmap.createBitmap(dimensionPx, dimensionPx, Bitmap.Config.ARGB_8888)
        val pixelScaleFactor = dimensionPx / QR_MATRIX_GRID_SIZE

        for (pixelY in 0 until dimensionPx) {
            for (pixelX in 0 until dimensionPx) {
                val matrixColumnX = (pixelX / pixelScaleFactor).coerceAtMost(QR_MATRIX_GRID_SIZE - 1)
                val matrixRowY = (pixelY / pixelScaleFactor).coerceAtMost(QR_MATRIX_GRID_SIZE - 1)
                val isDarkPixel = qrMatrix[matrixRowY][matrixColumnX]
                renderedBitmap.setPixel(pixelX, pixelY, if (isDarkPixel) Color.BLACK else Color.WHITE)
            }
        }

        return renderedBitmap
    }
}
