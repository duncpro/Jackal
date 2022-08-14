import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    jacoco
    `maven-publish`
    kotlin("jvm") version "1.6.21"
}

group = "com.duncpro"
version = "1.0-SNAPSHOT-24"

jacoco {
    toolVersion = "0.8.7"
}

val intTest: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

val intTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

val intTestRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations.runtimeOnly.get())
}

val intTestCompileOnly: Configuration by configurations.getting {}
val intTestAnnotationProcessor: Configuration by configurations.getting {}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.jetbrains:annotations:21.0.1")

    implementation(platform("software.amazon.awssdk:bom:2.15.0"))
    implementation("software.amazon.awssdk:rdsdata")
    implementation("org.slf4j:slf4j-api:1.7.31")

    testImplementation("junit:junit:4.13")

    intTestImplementation("org.apache.commons:commons-dbcp2:2.8.0")
    intTestImplementation("junit:junit:4.13")
    intTestImplementation("com.h2database:h2:1.4.200")
    intTestImplementation("org.slf4j:slf4j-simple:1.7.31")
}

tasks.getByName<Test>("test") {
    useJUnit()
}

val integrationTest = task<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["intTest"].output.classesDirs
    classpath = sourceSets["intTest"].runtimeClasspath

    shouldRunAfter("test")
    onlyIf { !project.hasProperty("skipIntegrationTests") }
}

val jacocoTestReport by tasks.getting(JacocoReport::class) {
    classDirectories.setFrom(sourceSets.main.get().output)
    sourceDirectories.setFrom(sourceSets.main.get().allSource.srcDirs)
    executionData.setFrom(fileTree(project.rootDir.absolutePath).include("**/build/jacoco/*.exec"))
    reports {
        xml.isEnabled = true
        html.isEnabled = false
    }
    sourceSets {
        add(main.get())
    }
}


tasks.check {
    dependsOn(integrationTest)
    finalizedBy(jacocoTestReport)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("https://duncpro-personal-618824625980.d.codeartifact.us-east-1.amazonaws.com/maven/duncpro-personal/")
            credentials {
                username = "aws"
                password = System.getenv("CODEARTIFACT_AUTH_TOKEN")
            }
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}
