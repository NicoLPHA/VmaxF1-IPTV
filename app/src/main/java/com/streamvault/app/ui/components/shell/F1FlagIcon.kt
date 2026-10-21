package com.streamvault.app.ui.components.shell

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val F1FlagIcon: ImageVector
    get() {
        if (_f1FlagIcon != null) return _f1FlagIcon!!
        _f1FlagIcon = ImageVector.Builder(
            name = "F1Flag",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                // Flag pole
                moveTo(4f, 2f)
                horizontalLineTo(6f)
                verticalLineTo(22f)
                horizontalLineTo(4f)
                close()
                // Flag body top-left block
                moveTo(6f, 2f)
                horizontalLineTo(13f)
                verticalLineTo(8f)
                horizontalLineTo(6f)
                close()
                // Flag body top-right block
                moveTo(13f, 2f)
                horizontalLineTo(20f)
                verticalLineTo(8f)
                horizontalLineTo(13f)
                close()
                // Flag body bottom-left block (offset for checkered look)
                moveTo(6f, 8f)
                horizontalLineTo(13f)
                verticalLineTo(14f)
                horizontalLineTo(6f)
                close()
                // Flag body bottom-right block
                moveTo(13f, 8f)
                horizontalLineTo(20f)
                verticalLineTo(14f)
                horizontalLineTo(13f)
                close()
            }
        }.build()
        return _f1FlagIcon!!
    }

private var _f1FlagIcon: ImageVector? = null
