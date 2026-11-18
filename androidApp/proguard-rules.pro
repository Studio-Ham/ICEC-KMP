# ICEC release R8 rules. Keep this file minimal.
#
# ML Kit discovers its components through Firebase ComponentDiscovery. It reads
# registrar class names (FaceRegistrar, CommonComponentRegistrar,
# VisionCommonRegistrar) from manifest metadata and calls their no-arg
# constructors by reflection. R8 full mode (AGP 8 default) removes those
# constructors because the consumer rules do not keep them, and release builds
# crash at launch with a NullPointerException.
# https://developer.android.com/build/releases/past-releases/agp-8-0-0-release-notes#r8-full-mode
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
#
# No other app-specific keep rules are needed:
# - Metro generates the dependency graph at compile time. It uses no reflection.
# - Compose Multiplatform resources live in assets (composeResources/), which
#   resource shrinking does not remove. The library ships its own consumer rules.
#
# Add a rule here only with a crash or missing-class report from a release build.
