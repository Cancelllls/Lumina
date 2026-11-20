-dontobfuscate
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,Exceptions,*Annotation*

# Strip verbose/debug logging from release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Room persistence & schema reflection
-keep class com.cancellls.lumina.data.db.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Manifest-registered components
-keep class com.cancellls.lumina.LuminaApplication { *; }
-keep class com.cancellls.lumina.MainActivity { *; }
-keep class com.cancellls.lumina.alarm.** { *; }
-keep class com.cancellls.lumina.widget.** { *; }

-dontwarn org.json.**
