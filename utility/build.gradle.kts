plugins {
	id("java")
	id("maven-publish")
	id("com.github.joschi.licenser")
}

val nettyAll: String = "4.2.3.Final"

repositories {
	mavenCentral()
	maven {
		url = uri("https://maven.luis-st.net/libraries/")
	}
}

dependencies {
	// Own libraries
	implementation("net.luis:LUtils:${rootProject.extra["lUtils"]}")
	// Google
	implementation("com.google.guava:guava:${rootProject.extra["googleGuava"]}") {  // Utility
		exclude(group = "org.checkerframework")
		exclude(group = "com.google.code.findbugs")
		exclude(group = "com.google.errorprone")
		exclude(group = "com.google.j2objc")
		exclude(group = "com.google.guava", module = "failureaccess")
		exclude(group = "com.google.guava", module = "listenablefuture")
	}
	// Apache
	implementation("org.apache.logging.log4j:log4j-api:${rootProject.extra["log4jAPI"]}") // Logging
	implementation("org.apache.logging.log4j:log4j-core:${rootProject.extra["log4jCore"]}") // Logging
	implementation("org.apache.commons:commons-lang3:${rootProject.extra["apacheLang"]}") // Utility
	// Netty
	implementation("io.netty:netty-all:$nettyAll") // Networking
	// Other
	implementation("org.jetbrains:annotations:${rootProject.extra["jetBrainsAnnotations"]}") // Annotations
	// Test
	testImplementation("org.junit.jupiter:junit-jupiter:${rootProject.extra["junitJupiter"]}")
}

tasks.test {
	useJUnitPlatform()
}

tasks.named<JavaCompile>("compileJava") {
	dependsOn("updateLicenses")
}

license {
	header = rootProject.file("header.txt")
	include("**/*.java")
	exclude("**/Main.java")
}

tasks.register<JavaExec>("run") {
	group = "runs"
	mainClass.set("net.luis.Main")
	classpath = sourceSets["main"].runtimeClasspath
	enableAssertions = true
	standardInput = System.`in`
	args = listOf()
}
