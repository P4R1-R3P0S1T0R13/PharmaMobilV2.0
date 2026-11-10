import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

// Firebase (Sesion 13): los complementos se aplican solo si existe google-services.json
// en esta carpeta. Sin el archivo la app compila igual y la telemetria va al log.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

// Sesion 14: URL de PharmaSoft por build. El emulador usa 10.0.2.2; un telefono real necesita
// la IP de la PC en la red local. Se define en local.properties (que no se sube a Git):
//   pharmasoft.url=http://192.168.1.40:8080/
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val urlPharmaSoft: String = propiedadesLocales.getProperty("pharmasoft.url") ?: "http://10.0.2.2:8080/"

// Sesion 14: credenciales de firma en keystore.properties (raiz del proyecto, fuera de Git).
// Si el archivo no existe (quien clona el repo), el proyecto abre y compila igual en debug;
// solo la build de entrega queda sin firmar.
val firma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val hayFirma = firma.getProperty("storeFile") != null

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "pe.edu.upeu.pharmamobil"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "pe.edu.upeu.pharmamobil"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "PHARMASOFT_URL", "\"$urlPharmaSoft\"")
    }
    signingConfigs {
        if (hayFirma) {
            create("release") {
                storeFile = file(firma.getProperty("storeFile"))
                storePassword = firma.getProperty("storePassword")
                keyAlias = firma.getProperty("keyAlias")
                keyPassword = firma.getProperty("keyPassword")
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            if (hayFirma) signingConfig = signingConfigs.getByName("release")
            // Paso 5: reduccion y ofuscacion activas; las reglas estan en proguard-rules.pro.
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        // Sesion 14: BuildConfig.DEBUG y BuildConfig.VERSION_NAME llegan al codigo comun como InfoApp.
        buildConfig = true
    }
}
// Paso 4: genera el paquete (.aab) y el instalable (.apk) firmados y los copia con la
// nomenclatura de entrega a entregables/builds/ (carpeta ignorada por Git).
//   ./gradlew :androidApp:entregables
tasks.register<Copy>("entregables") {
    group = "entrega"
    description = "Genera el AAB y el APK de release y los copia con la nomenclatura de entrega."
    dependsOn("bundleRelease", "assembleRelease")
    from(layout.buildDirectory.file("outputs/bundle/release/androidApp-release.aab")) {
        rename { "Pari_ProyectoFinal_Android.aab" }
    }
    from(layout.buildDirectory.file("outputs/apk/release/androidApp-release.apk")) {
        rename { "Pari_ProyectoFinal_Android.apk" }
    }
    into(rootProject.layout.projectDirectory.dir("entregables/builds"))
}
