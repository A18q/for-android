package chat.stoat.ui.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import chat.stoat.api.settings.UserInterfaceFont
import chat.stoat.core.model.data.OverridableColourScheme

val LightColorScheme = lightColorScheme(
    primary = Colour.PrimaryLight,
    onPrimary = Colour.OnPrimaryLight,
    primaryContainer = Colour.PrimaryContainerLight,
    onPrimaryContainer = Colour.OnPrimaryContainerLight,
    inversePrimary = Colour.InversePrimaryLight,
    secondary = Colour.SecondaryLight,
    onSecondary = Colour.OnSecondaryLight,
    secondaryContainer = Colour.SecondaryContainerLight,
    onSecondaryContainer = Colour.OnSecondaryContainerLight,
    tertiary = Colour.TertiaryLight,
    onTertiary = Colour.OnTertiaryLight,
    tertiaryContainer = Colour.TertiaryContainerLight,
    onTertiaryContainer = Colour.OnTertiaryContainerLight,
    background = Colour.BackgroundLight,
    onBackground = Colour.OnBackgroundLight,
    surface = Colour.SurfaceLight,
    onSurface = Colour.OnSurfaceLight,
    surfaceVariant = Colour.SurfaceVariantLight,
    onSurfaceVariant = Colour.OnSurfaceVariantLight,
    surfaceTint = Colour.SurfaceTintLight,
    inverseSurface = Colour.InverseSurfaceLight,
    inverseOnSurface = Colour.InverseOnSurfaceLight,
    error = Colour.ErrorLight,
    onError = Colour.OnErrorLight,
    errorContainer = Colour.ErrorContainerLight,
    onErrorContainer = Colour.OnErrorContainerLight,
    outline = Colour.OutlineLight,
    outlineVariant = Colour.OutlineVariantLight,
    scrim = Colour.ScrimLight,
    surfaceBright = Colour.SurfaceBrightLight,
    surfaceContainer = Colour.SurfaceContainerLight,
    surfaceContainerHigh = Colour.SurfaceContainerHighLight,
    surfaceContainerHighest = Colour.SurfaceContainerHighestLight,
    surfaceContainerLow = Colour.SurfaceContainerLowLight,
    surfaceContainerLowest = Colour.SurfaceContainerLowestLight,
    surfaceDim = Colour.SurfaceDimLight,
    primaryFixed = Colour.PrimaryFixed,
    primaryFixedDim = Colour.PrimaryFixedDim,
    onPrimaryFixed = Colour.OnPrimaryFixed,
    onPrimaryFixedVariant = Colour.OnPrimaryFixedVariant,
    secondaryFixed = Colour.SecondaryFixed,
    secondaryFixedDim = Colour.SecondaryFixedDim,
    onSecondaryFixed = Colour.OnSecondaryFixed,
    onSecondaryFixedVariant = Colour.OnSecondaryFixedVariant,
    tertiaryFixed = Colour.TertiaryFixed,
    tertiaryFixedDim = Colour.TertiaryFixedDim,
    onTertiaryFixed = Colour.OnTertiaryFixed,
    onTertiaryFixedVariant = Colour.OnTertiaryFixedVariant,
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF5865F2), // Discord Blurple
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF4752C4),
    onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF5865F2),
    secondary = Color(0xFF232428),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF2B2D31),
    onSecondaryContainer = Color(0xFFDBDEE1),
    tertiary = Color(0xFF5865F2),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF232428),
    onTertiaryContainer = Color(0xFFDBDEE1),
    background = Color(0xFF313338), // Discord Modern Center Chat Primary
    onBackground = Color(0xFFDBDEE1), // Discord Text Normal
    surface = Color(0xFF2B2D31), // Discord Drawer Secondary Body
    onSurface = Color(0xFFF2F3F5), // Discord Text Header / Bold
    surfaceVariant = Color(0xFF232428), // Discord Card Surface
    onSurfaceVariant = Color(0xFF949BA4), // Discord Text Muted
    surfaceTint = Color(0xFF5865F2),
    inverseSurface = Color(0xFFF2F3F5),
    inverseOnSurface = Color(0xFF1E1F22),
    error = Color(0xFFED4245),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFED4245),
    onErrorContainer = Color(0xFFFFFFFF),
    outline = Color(0xFF3F4147),
    outlineVariant = Color(0xFF2B2D31),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF383A40),
    surfaceContainer = Color(0xFF2B2D31), // Discord Secondary Drawer Body
    surfaceContainerHigh = Color(0xFF383A40),
    surfaceContainerHighest = Color(0xFF404249),
    surfaceContainerLow = Color(0xFF1E1F22), // Discord Persistent Rail
    surfaceContainerLowest = Color(0xFF17181B), // Discord Floating User Capsule / Modal Base
    surfaceDim = Color(0xFF1E1F22),
    primaryFixed = Colour.PrimaryFixed,
    primaryFixedDim = Colour.PrimaryFixedDim,
    onPrimaryFixed = Colour.OnPrimaryFixed,
    onPrimaryFixedVariant = Colour.OnPrimaryFixedVariant,
    secondaryFixed = Colour.SecondaryFixed,
    secondaryFixedDim = Colour.SecondaryFixedDim,
    onSecondaryFixed = Colour.OnSecondaryFixed,
    onSecondaryFixedVariant = Colour.OnSecondaryFixedVariant,
    tertiaryFixed = Colour.TertiaryFixed,
    tertiaryFixedDim = Colour.TertiaryFixedDim,
    onTertiaryFixed = Colour.OnTertiaryFixed,
    onTertiaryFixedVariant = Colour.OnTertiaryFixedVariant,
)

val AmoledColorScheme = DarkColorScheme.copy(
    background = Color(0xff000000),
    onBackground = Color(0xffffffff),
    surfaceVariant = Color(0xff131313),
    onSurfaceVariant = Color(0xffffffff),
    surface = Color(0xff000000),
    onSurface = Color(0xffffffff),
    surfaceContainerLowest = Color(0xff000000),
    surfaceContainerLow = Color(0xff000000),
    surfaceContainer = Color(0xff000000),
    surfaceContainerHigh = Color(0xff000000),
    surfaceContainerHighest = Color(0xff000000),
)

enum class Theme {
    None,
    Default,
    Light,
    M3Dynamic,
    Amoled
}

@Composable
fun getColorScheme(
    requestedTheme: Theme,
    colourOverrides: OverridableColourScheme? = null
): ColorScheme {
    val context = LocalContext.current

    val systemInDarkTheme = isSystemInDarkTheme()
    val m3Supported = systemSupportsDynamicColors()

    val colorScheme = when {
        m3Supported && requestedTheme == Theme.M3Dynamic && systemInDarkTheme -> dynamicDarkColorScheme(
            context
        )

        m3Supported && requestedTheme == Theme.M3Dynamic && !systemInDarkTheme -> dynamicLightColorScheme(
            context
        )

        requestedTheme == Theme.Default -> DarkColorScheme
        requestedTheme == Theme.Light -> LightColorScheme
        requestedTheme == Theme.Amoled -> AmoledColorScheme
        requestedTheme == Theme.None && systemInDarkTheme -> DarkColorScheme
        requestedTheme == Theme.None && !systemInDarkTheme -> LightColorScheme
        else -> DarkColorScheme
    }.copy()

    val colorSchemeIsDark = when {
        m3Supported && requestedTheme == Theme.M3Dynamic -> isSystemInDarkTheme()
        requestedTheme == Theme.Default -> true
        requestedTheme == Theme.Light -> false
        requestedTheme == Theme.Amoled -> true
        requestedTheme == Theme.None && systemInDarkTheme -> true
        requestedTheme == Theme.None && !systemInDarkTheme -> false
        else -> true
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars =
                !colorSchemeIsDark
        }
    }

    if (colourOverrides == null) return colorScheme
    return colourOverrides.applyTo(colorScheme)
}

@SuppressLint("NewApi")
@Composable
fun StoatTheme(
    requestedTheme: Theme,
    requestedUserInterfaceFont: UserInterfaceFont,
    colourOverrides: OverridableColourScheme? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = getColorScheme(requestedTheme, colourOverrides)
    val typography = when (requestedUserInterfaceFont) {
        UserInterfaceFont.Default -> StoatTypography
        UserInterfaceFont.GoogleSansFlex -> GoogleTypography
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

fun systemSupportsDynamicColors(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

fun getDefaultTheme(): Theme {
    return when {
        systemSupportsDynamicColors() -> Theme.M3Dynamic
        else -> Theme.Default
    }
}

fun isThemeDark(theme: Theme, systemIsDark: Boolean): Boolean {
    return when (theme) {
        Theme.Default, Theme.Amoled -> true
        Theme.Light -> false
        Theme.M3Dynamic, Theme.None -> systemIsDark
    }
}

@Composable
fun isThemeDark(theme: Theme) = isThemeDark(theme, isSystemInDarkTheme())
