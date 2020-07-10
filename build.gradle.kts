plugins {
    java
    kotlin("jvm") version "1.3.72"
    application
}

group = "org.cybergstudios"
version = "1.0-SNAPSHOT"

application {
    mainClassName = "com.cybergstudios.genetics.MainKt"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}