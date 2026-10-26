import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.sqldelight)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedLogic"
            isStatic = true
        }
    }
    
    androidLibrary {
       namespace = "com.monebie.app.sharedLogic"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_17
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }
    
    sourceSets {
        commonMain.dependencies {
            // put your Multiplatform dependencies here
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.sqldelight.coroutines.extensions)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }

        // JDBC driver, host-side only: lets the schema migrations be exercised
        // in a plain JVM test rather than only on a device.
        getByName("androidHostTest").dependencies {
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.kotlinx.coroutines.test)
        }
        
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

sqldelight {
    databases {
        create("MoneBieDatabase") {
            packageName.set("com.monebie.app.db")
            dialect(libs.sqldelight.sqlite.dialect)
            srcDirs.setFrom("src/commonMain/sq")
        }
    }
}