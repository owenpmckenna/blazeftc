import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.proto

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
    publishing
    signing
    id("com.google.protobuf") version "0.9.1"
}

android {
    namespace = "dev.anygeneric.overplay_backend"
    compileSdk {
        version = release(34)
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    sourceSets {
        getByName("main")
            .proto {
                srcDir("src/main/protobuf")
            }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

dependencies {
    //implementation(libs.core.ktx)
    //implementation(libs.appcompat)
    //implementation(libs.material)
    implementation("androidx.appcompat:appcompat:1.2.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    compileOnly("org.firstinspires.ftc:RobotCore:[11.0.0,)")
    compileOnly("org.firstinspires.ftc:Hardware:[11.0.0,)")
    implementation("com.google.protobuf:protobuf-javalite:3.21.7")
}
protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.21.7"
    }
    plugins {
        // Optional: an artifact spec for a protoc plugin, with "grpc" as
        // the identifier, which can be referred to in the "plugins"
        // container of the "generateProtoTasks" closure.
        //id("com.google.protobuf:protoc-gen-javalite:3.0.0")
        //javalite {
        // The codegen for lite comes as a separate artifact
        //    artifact = 'com.google.protobuf:protoc-gen-javalite:3.0.0'
        //}

        generateProtoTasks {
            all().forEach {
                it.builtins {
                    create("java") {
                        option("lite")
                    }
                }
            }
        }
    }
}