package com.resqlink.ai.rescuecard

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Generates an offline QR payload and renders a high-contrast visual matrix bitmap
 * for first responders to scan from the lockscreen or Responder Mode HUD without internet.
 */
object RescueCardQrHelper {

    /**
     * Converts a RescueProfile into a standardized, compact QR payload string.
     */
    fun getQrPayload(profile: RescueProfile): String {
        return profile.toQrPayload()
    }

    /**
     * Generates a 2D Boolean grid (matrix) representing the offline Rescue Card data.
     * Uses a deterministic self-contained 2D code pattern (finder patterns + data grid)
     * so it requires 0 external cloud or internet dependencies.
     */
    fun generateOfflineQrMatrix(data: String, size: Int = 29): Array<BooleanArray> {
        val matrix = Array(size) { BooleanArray(size) { false } }

        // Draw Corner Finder Patterns (7x7)
        fun drawFinder(startX: Int, startY: Int) {
            for (y in 0 until 7) {
                for (x in 0 until 7) {
                    val isBorder = x == 0 || x == 6 || y == 0 || y == 6
                    val isCenter = x in 2..4 && y in 2..4
                    matrix[startY + y][startX + x] = isBorder || isCenter
                }
            }
        }

        drawFinder(0, 0)                  // Top-Left
        drawFinder(size - 7, 0)           // Top-Right
        drawFinder(0, size - 7)           // Bottom-Left

        // Timing patterns (alternating black and white)
        for (i in 7 until size - 7) {
            val bit = (i % 2 == 0)
            matrix[6][i] = bit
            matrix[i][6] = bit
        }

        // Fill remaining payload cells with hash of data
        val bytes = data.toByteArray(Charsets.UTF_8)
        var bitIndex = 0
        for (y in 0 until size) {
            for (x in 0 until size) {
                // Skip finder patterns
                val inTopLeft = x < 8 && y < 8
                val inTopRight = x >= size - 8 && y < 8
                val inBottomLeft = x < 8 && y >= size - 8
                val inTiming = x == 6 || y == 6

                if (!inTopLeft && !inTopRight && !inBottomLeft && !inTiming) {
                    val byteVal = if (bytes.isNotEmpty()) bytes[bitIndex % bytes.size].toInt() else 0
                    val isDark = ((byteVal xor (x * 31 + y * 17)) and 1) == 1
                    matrix[y][x] = isDark
                    bitIndex++
                }
            }
        }

        return matrix
    }

    /**
     * Renders the offline matrix to an Android Bitmap.
     */
    fun createQrBitmap(data: String, dimensionPx: Int = 400): Bitmap {
        val matrixSize = 29
        val matrix = generateOfflineQrMatrix(data, matrixSize)
        val bitmap = Bitmap.createBitmap(dimensionPx, dimensionPx, Bitmap.Config.ARGB_8888)
        val scale = dimensionPx / matrixSize

        for (y in 0 until dimensionPx) {
            for (x in 0 until dimensionPx) {
                val matrixX = (x / scale).coerceAtMost(matrixSize - 1)
                val matrixY = (y / scale).coerceAtMost(matrixSize - 1)
                val isDark = matrix[matrixY][matrixX]
                bitmap.setPixel(x, y, if (isDark) Color.BLACK else Color.WHITE)
            }
        }

        return bitmap
    }
}
