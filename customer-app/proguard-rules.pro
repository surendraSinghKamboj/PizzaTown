# ============================================================
# PizzaTown Customer - Firebase Firestore / Kotlin DTO rules
# ============================================================

# Keep all Firestore DTO/model classes and their constructors.
-keep class com.pizzatown.customer.data.model.** { *; }

# Keep Firestore serialization annotations/metadata.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep DocumentId / Firestore annotated members.
-keepclassmembers class ** {
    @com.google.firebase.firestore.* <fields>;
}

# Keep Kotlin data-class constructors used by Firestore reflection.
-keepclassmembers class com.pizzatown.customer.data.model.** {
    <init>(...);
}

# Keep Firebase model annotations.
-keep @interface com.google.firebase.firestore.DocumentId
-keep @interface com.google.firebase.firestore.PropertyName
-keep @interface com.google.firebase.firestore.Exclude
-keep @interface com.google.firebase.firestore.IgnoreExtraProperties
-keep @interface com.google.firebase.firestore.ServerTimestamp

# Keep Firebase/Firestore DTOs from being renamed.
-keepnames class com.pizzatown.customer.data.model.**
