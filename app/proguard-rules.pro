# AssistiveTouch ProGuard / R8 Rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Keep data models
-keep class com.example.assistivetouch.model.** { *; }
-keep class com.example.assistivetouch.prefs.** { *; }

# Keep custom Views used in XML layouts
-keep class com.example.assistivetouch.ui.view.** { *; }

# Keep services and activities
-keep class com.example.assistivetouch.service.** { *; }
-keep class com.example.assistivetouch.ui.** { *; }
