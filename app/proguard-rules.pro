# ========== App models (Gson + Room) ==========
-keep class com.notebook.notes.checklist.model.** { *; }

-renamesourcefileattribute SourceFile
-keepattributes SourceFile, LineNumberTable, Signature, RuntimeVisibleAnnotations, AnnotationDefault, InnerClasses, EnclosingMethod

-keep public class * extends java.lang.Exception

# Flow / prefs enums (name-based parsing from Remote Config / SharedPreferences)
-keepclassmembers enum com.notebook.notes.checklist.flow.FlowStep { *; }
-keepclassmembers enum com.notebook.notes.checklist.flow.SplashPermissionMode { *; }
-keepclassmembers enum com.notebook.notes.checklist.flow.PermissionType { *; }
-keepclassmembers enum com.notebook.notes.checklist.sort.NoteSortOrder { *; }

# WorkManager (reflection instantiation)
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Parcelable (e.g. AlarmModel in CallFunction.Model)
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Room (backup; AAR usually ships rules)
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Coil (image loading)
-keep class coil.** { *; }
-dontwarn coil.**

# Lottie
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# AndroidSVG
-keep class com.caverock.androidsvg.** { *; }
-dontwarn com.caverock.androidsvg.**

# Ads (AdMob, Facebook Audience Network, mediation, shimmer)
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.mediation.** { *; }
-keep class com.facebook.ads.** { *; }
-keep class com.facebook.shimmer.** { *; }
-dontwarn com.facebook.ads.**
-dontwarn com.google.ads.**

# Jersey Base64 (ads AES decryption)
-keep class com.sun.jersey.core.util.Base64 { *; }
-dontwarn com.sun.jersey.**

# Manifest / dynamically registered components
-keep class com.notebook.notes.checklist.reminder.ReminderAlarmReceiver { *; }
-keep class com.notebook.notes.checklist.reminder.BootCompletedReceiver { *; }
-keep class com.notebook.notes.checklist.Advertisement.MyFirebaseMessagingService { *; }
-keep class com.notebook.notes.checklist.Advertisement.CallendClass.PhoneCallStateService { *; }
-keep class com.notebook.notes.checklist.Advertisement.Overlay.DailyNotificationReceiver { *; }
-keep class com.notebook.notes.checklist.CallFunction.Notification.RemindReceiver { *; }

-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn javax.swing.tree.TreeNode
-dontwarn com.google.common.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.facebook.infer.annotation.Nullsafe$Mode
-dontwarn com.facebook.infer.annotation.Nullsafe
-dontwarn javax.activation.DataSource
-dontwarn javax.mail.internet.MimeMultipart

# Gson https://github.com/google/gson/blob/main/gson/src/main/resources/META-INF/proguard/gson.pro
-if class com.google.gson.reflect.TypeToken
-keep,allowobfuscation class com.google.gson.reflect.TypeToken

-keep,allowobfuscation class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowoptimization @com.google.gson.annotations.JsonAdapter class *

-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.Expose <fields>;
  @com.google.gson.annotations.JsonAdapter <fields>;
  @com.google.gson.annotations.Since <fields>;
  @com.google.gson.annotations.Until <fields>;
}

-keepclassmembers class * extends com.google.gson.TypeAdapter {
  <init>();
}
-keepclassmembers class * implements com.google.gson.TypeAdapterFactory {
  <init>();
}
-keepclassmembers class * implements com.google.gson.JsonSerializer {
  <init>();
}
-keepclassmembers class * implements com.google.gson.JsonDeserializer {
  <init>();
}

-if class *
-keepclasseswithmembers,allowobfuscation class <1> {
  @com.google.gson.annotations.SerializedName <fields>;
}
-if class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers,allowobfuscation,allowoptimization class <1> {
  <init>();
}

-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
