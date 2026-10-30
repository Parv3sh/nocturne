# No reflection-based serialization; keep the dream service entry point.
-keep class dev.nocturne.ambient.dream.NocturneDreamService { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
