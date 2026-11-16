# R8 rules applied to any app that depends on :core-bridge.

# JNA looks up classes, fields and methods by name through reflection and JNI.
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
# JNA references desktop-only classes that do not exist on Android.
-dontwarn java.awt.**

# UniFFI bindings: JNA maps Structure fields and Library methods by name,
# so the generated classes must keep their names and members.
-keep class io.github.aizenprivai.openop.core.ffi.** { *; }
