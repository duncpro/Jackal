plugins {
    java
}

val lombokVersion by extra { "1.18.20" }

group = "com.duncpro"
version = "1.0-SNAPSHOT"

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
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.jetbrains:annotations:21.0.1")

    implementation(platform("software.amazon.awssdk:bom:2.15.0"))
    implementation("software.amazon.awssdk:rdsdata")
    implementation("org.slf4j:slf4j-api:1.7.31")

    compileOnly("org.projectlombok:lombok:${lombokVersion}")
    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")
    testCompileOnly("org.projectlombok:lombok:${lombokVersion}")
    intTestCompileOnly("org.projectlombok:lombok:${lombokVersion}")
    testAnnotationProcessor("org.projectlombok:lombok:${lombokVersion}")
    intTestAnnotationProcessor("org.projectlombok:lombok:${lombokVersion}")

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
}

tasks.check { dependsOn(integrationTest) }
