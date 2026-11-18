# ICEC release R8 rules. Keep this file minimal.
#
# No app-specific keep rules are needed:
# - ML Kit (com.google.mlkit:face-detection) ships consumer ProGuard rules in its AARs.
# - Metro generates the dependency graph at compile time. It uses no reflection.
# - Compose Multiplatform resources live in assets (composeResources/), which
#   resource shrinking does not remove. The library ships its own consumer rules.
#
# Add a rule here only with a crash or missing-class report from a release build.
