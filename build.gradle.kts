val lUtils: String by project
val googleGuava: String by project
val log4jAPI: String by project
val log4jCore: String by project
val apacheLang: String by project
val jetBrainsAnnotations: String by project
val junitJupiter: String by project

plugins {
	id("java")
	id("maven-publish")
	id("com.github.joschi.licenser") version "0.6.1"
}

allprojects {
	extra["lUtils"] = lUtils
	extra["googleGuava"] = googleGuava
	extra["log4jAPI"] = log4jAPI
	extra["log4jCore"] = log4jCore
	extra["apacheLang"] = apacheLang
	extra["jetBrainsAnnotations"] = jetBrainsAnnotations
	extra["junitJupiter"] = junitJupiter
}

subprojects {
	apply(plugin = "java")
	apply(plugin = "maven-publish")
	apply(plugin = "com.github.joschi.licenser")
}

repositories {
	mavenCentral()
	maven {
		url = uri("https://maven.luis-st.net/libraries/")
	}
}

dependencies {
	// Own libraries
	implementation("net.luis:LUtils:${lUtils}")
	// Google
	implementation("com.google.guava:guava:${googleGuava}") {  // Utility
		exclude(group = "org.checkerframework")
		exclude(group = "com.google.code.findbugs")
		exclude(group = "com.google.errorprone")
		exclude(group = "com.google.j2objc")
		exclude(group = "com.google.guava", module = "failureaccess")
		exclude(group = "com.google.guava", module = "listenablefuture")
	}
	// Apache
	implementation("org.apache.logging.log4j:log4j-api:${log4jAPI}") // Logging
	implementation("org.apache.logging.log4j:log4j-core:${log4jCore}") // Logging
	implementation("org.apache.commons:commons-lang3:${apacheLang}") // Utility
	// Other
	implementation("org.jetbrains:annotations:${jetBrainsAnnotations}") // Annotations
	// Test
	testImplementation("org.junit.jupiter:junit-jupiter:${junitJupiter}")
}

tasks.named<JavaCompile>("compileJava") {
	dependsOn("updateLicenses")
}

license {
	header = file("header.txt")
	include("**/*.java")
	exclude("**/Main.java")
}
