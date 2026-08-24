# MonumentQuest baseline R8 rules
# Keep model constructors used by Gson/Retrofit and generated Hilt entry points.
-keep class com.monumentquest.data.model.** { <fields>; }
-keepclassmembers class com.monumentquest.data.model.** { <fields>; }
-keep class dagger.hilt.** { *; }
-dontwarn org.osmdroid.**
