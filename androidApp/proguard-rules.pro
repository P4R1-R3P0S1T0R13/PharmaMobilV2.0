# Reglas de reduccion/ofuscacion para la build de release (Sesion 14, paso 5).
# Si al probar la build firmada aparece un error que no existe en debug, la causa
# suele ser una clase que R8 elimino o renombro: se agrega aqui la regla -keep.

# Metadatos que necesitan kotlinx.serialization y Ktor en tiempo de ejecucion.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations

# kotlinx.serialization: conservar los serializadores generados de los DTO del proyecto.
-keepclassmembers @kotlinx.serialization.Serializable class pe.edu.upeu.pharmamobil.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class pe.edu.upeu.pharmamobil.**$$serializer { *; }
-keepclassmembers class pe.edu.upeu.pharmamobil.** {
    *** Companion;
}
-keepclasseswithmembers class pe.edu.upeu.pharmamobil.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# SQLDelight: el driver de Android crea la base por reflexion del esquema generado.
-keep class pe.edu.upeu.pharmamobil.db.** { *; }

# Ktor y sus dependencias opcionales que no se usan en Android.
-dontwarn java.lang.management.**
-dontwarn org.slf4j.**
-dontwarn io.ktor.utils.io.jvm.nio.**

# Pilas de Crashlytics legibles: se conserva el numero de linea y se oculta el archivo fuente.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
