plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
    signing
}


android {
    namespace = "dev.anygeneric.blazeftc"
    compileSdk {
        version = release(34)
    }

    defaultConfig {
        minSdk = 24
        targetSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    lint {
        // Disables the expired target SDK warning
        disable.add("ExpiredTargetSdkVersion")
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
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

dependencies {
    //implementation(libs.appcompat)
    implementation(libs.material)
    //implementation(libs.core.ktx)
    implementation("androidx.appcompat:appcompat:1.2.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    compileOnly("org.firstinspires.ftc:RobotCore:[11.0.0,)")
    compileOnly("org.firstinspires.ftc:Hardware:[11.0.0,)")
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

publishing {
    publications {
        create<MavenPublication>("mavenLibrary") {
            groupId = "dev.anygeneric"
            artifactId = "blazeftc"
            version = "0.1.30"
            description = "https://github.com/owenpmckenna/blaze_ftc"


            afterEvaluate {
                from(components["release"])
                /*val variant = android.libraryVariants.find { it.name == "release" }
                if (variant != null) {
                    artifact(variant.outputs.first().outputFile) {
                        extension = "aar"
                    }
                }*/
            }
            pom {
                name = "BlazeFTC"
                description = "BlazeFTC allows First Tech Challenge participants to write opmodes in Rust."
                url = "https://github.com/owenpmckenna/blaze_ftc"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "owenpmckenna"
                        name = "Owen Mckenna"
                        email = "noreply@example.com"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/owenpmckenna/blazeftc.git"
                    //developerConnection = "scm:git:ssh://example.com/my-library.git"
                    url = "https://github.com/owenpmckenna/blazeftc"
                }
            }
        }
    }

    repositories {
        mavenLocal()
        /*maven {
            name = "sonatype"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")

            credentials {
                username = findProperty("sonatypeUsername") as String
                password = findProperty("sonatypePassword") as String
            }
        }*/
    }
}