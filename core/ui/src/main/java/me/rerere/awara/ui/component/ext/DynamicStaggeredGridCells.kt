// Requirements: one width-based grid calculation for feed/search.
// Verify: narrow windows and all layout-affecting constructor values.
package me.rerere.awara.ui.component.ext

import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
class DynamicStaggeredGridCells(
    val minSize: Dp = 150.dp,
    private val min: Int = 1,
    private val max: Int = 6,
) : StaggeredGridCells {
    init { require(minSize.value > 0 && min > 0 && max >= min) }
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): IntArray {
        if (availableSize <= 0) return intArrayOf(0)
        val fit = ((availableSize + spacing) / (minSize.roundToPx() + spacing).coerceAtLeast(1)).coerceAtLeast(1)
        val count = fit.coerceIn(1, max)
        val pixels = (availableSize - spacing * (count - 1)).coerceAtLeast(0)
        return IntArray(count) { pixels / count + if (it < pixels % count) 1 else 0 }
    }
    override fun equals(other: Any?) = other is DynamicStaggeredGridCells && minSize == other.minSize && min == other.min && max == other.max
    override fun hashCode() = 31 * (31 * minSize.hashCode() + min) + max
}
