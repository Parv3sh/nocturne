package dev.nocturne.ambient.ui.theme

import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Fire OS ships Amazon Ember as system fonts. Loading them directly avoids the
// variable-font crash on Android 7.1 and matches the platform's TV typography.
private val displayTypeface: Typeface = runCatching {
    Typeface.createFromFile("/system/fonts/Amazon-Ember-Thin.ttf")
}.getOrElse {
    Typeface.create("sans-serif-light", Typeface.NORMAL)
}

private val bodyTypeface: Typeface = runCatching {
    Typeface.createFromFile("/system/fonts/Amazon-Ember-Regular.ttf")
}.getOrElse {
    Typeface.create("sans-serif", Typeface.NORMAL)
}

private val boldTypeface: Typeface = runCatching {
    Typeface.createFromFile("/system/fonts/Amazon-Ember-Bold.ttf")
}.getOrElse {
    Typeface.create("sans-serif", Typeface.BOLD)
}

val Sora = FontFamily(displayTypeface)
val Manrope = FontFamily(bodyTypeface)
val EmberBold = FontFamily(boldTypeface)

val HeroType = TextStyle(
    fontFamily = Sora,
    fontWeight = FontWeight.ExtraLight,
    fontSize = 220.sp,
    letterSpacing = (-8).sp,
    lineHeight = 220.sp
)

val DateType = TextStyle(
    fontFamily = Manrope,
    fontWeight = FontWeight.Normal,
    fontSize = 22.sp,
    letterSpacing = 0.2.sp
)

val EyebrowType = TextStyle(
    fontFamily = Manrope,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 2.4.sp
)

val LabelType = TextStyle(
    fontFamily = Manrope,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    letterSpacing = 0.8.sp
)

val ClockSuffixType = TextStyle(
    fontFamily = EmberBold,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    letterSpacing = 1.8.sp
)

val BodyType = TextStyle(
    fontFamily = Manrope,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    letterSpacing = 0.1.sp
)

data class Palette(
    val background: Color,
    val backgroundDeep: Color,
    val surface: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val accent: Color,
    val accentSoft: Color,
    val hairline: Color
)

private data class PaletteKey(val hour: Float, val palette: Palette)

private val night = Palette(
    background = Color(0xFF060709),
    backgroundDeep = Color(0xFF030405),
    surface = Color(0xFF101216),
    ink = Color(0xFFECE5D9),
    inkSoft = Color(0xFFAAA69E),
    inkFaint = Color(0xFF67665F),
    accent = Color(0xFF9FAFD0),
    accentSoft = Color(0xFF6E7B99),
    hairline = Color(0xFF26282C)
)

private val dawn = Palette(
    background = Color(0xFF0A0807),
    backgroundDeep = Color(0xFF050404),
    surface = Color(0xFF17120F),
    ink = Color(0xFFF1E7D9),
    inkSoft = Color(0xFFB4A696),
    inkFaint = Color(0xFF74695E),
    accent = Color(0xFFD9AE80),
    accentSoft = Color(0xFF9C7956),
    hairline = Color(0xFF2D2823)
)

private val day = Palette(
    background = Color(0xFF08090A),
    backgroundDeep = Color(0xFF040506),
    surface = Color(0xFF131518),
    ink = Color(0xFFEDE8DE),
    inkSoft = Color(0xFFABA9A1),
    inkFaint = Color(0xFF696A66),
    accent = Color(0xFFBBC7C6),
    accentSoft = Color(0xFF7E8E8C),
    hairline = Color(0xFF26292B)
)

private val dusk = Palette(
    background = Color(0xFF0A0708),
    backgroundDeep = Color(0xFF050304),
    surface = Color(0xFF181113),
    ink = Color(0xFFF2E5DD),
    inkSoft = Color(0xFFB7A29A),
    inkFaint = Color(0xFF75635D),
    accent = Color(0xFFD8A08B),
    accentSoft = Color(0xFF9D705F),
    hairline = Color(0xFF2E2425)
)

private val evening = Palette(
    background = Color(0xFF07070A),
    backgroundDeep = Color(0xFF030305),
    surface = Color(0xFF111116),
    ink = Color(0xFFEDE6DD),
    inkSoft = Color(0xFFA9A3A0),
    inkFaint = Color(0xFF67625F),
    accent = Color(0xFFB9A7C2),
    accentSoft = Color(0xFF81718A),
    hairline = Color(0xFF26252B)
)

private val keys = listOf(
    PaletteKey(0f, night),
    PaletteKey(4.5f, night),
    PaletteKey(6f, dawn),
    PaletteKey(8f, day),
    PaletteKey(16f, day),
    PaletteKey(18.5f, dusk),
    PaletteKey(20.5f, evening),
    PaletteKey(24f, night)
)

fun paletteFor(hour: Int, minute: Int = 0): Palette {
    val t = (hour + minute / 60f).coerceIn(0f, 23.999f)
    val upperIndex = keys.indexOfFirst { it.hour >= t }.coerceAtLeast(1)
    val lower = keys[upperIndex - 1]
    val upper = keys[upperIndex]
    val span = (upper.hour - lower.hour).coerceAtLeast(0.001f)
    val f = ((t - lower.hour) / span).coerceIn(0f, 1f)
    return Palette(
        background = lerp(lower.palette.background, upper.palette.background, f),
        backgroundDeep = lerp(lower.palette.backgroundDeep, upper.palette.backgroundDeep, f),
        surface = lerp(lower.palette.surface, upper.palette.surface, f),
        ink = lerp(lower.palette.ink, upper.palette.ink, f),
        inkSoft = lerp(lower.palette.inkSoft, upper.palette.inkSoft, f),
        inkFaint = lerp(lower.palette.inkFaint, upper.palette.inkFaint, f),
        accent = lerp(lower.palette.accent, upper.palette.accent, f),
        accentSoft = lerp(lower.palette.accentSoft, upper.palette.accentSoft, f),
        hairline = lerp(lower.palette.hairline, upper.palette.hairline, f)
    )
}
