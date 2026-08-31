# ============================================================
# PizzaTown Admin - Release / R8 safety rules
# ============================================================

# Keep Firestore DTO/model classes used through reflection.
-keep class com.pizzatown.admin.data.model.** { *; }

# Preserve Firestore serialization metadata.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Preserve Firestore annotated fields.
-keepclassmembers class ** {
    @com.google.firebase.firestore.* <fields>;
}

# Preserve DTO constructors used by Firestore reflection.
-keepclassmembers class com.pizzatown.admin.data.model.** {
    <init>(...);
}

# Preserve Firebase Firestore annotations.
-keep @interface com.google.firebase.firestore.DocumentId
-keep @interface com.google.firebase.firestore.PropertyName
-keep @interface com.google.firebase.firestore.Exclude
-keep @interface com.google.firebase.firestore.IgnoreExtraProperties
-keep @interface com.google.firebase.firestore.ServerTimestamp

# Preserve names of Firestore DTOs.
-keepnames class com.pizzatown.admin.data.model.**

# Preserve Hilt generated components/injection metadata.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Preserve Lottie raw animation handling.
-keep class com.airbnb.lottie.** { *; }

# Preserve generated Firebase/Hilt metadata.
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
