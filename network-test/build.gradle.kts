import net.luis.lm.LineEnding
import java.time.Year
import java.util.UUID

val lUtils: String by project
val googleGuava: String by project
val log4jAPI: String by project
val log4jCore: String by project
val jackson: String by project
val apacheLang: String by project
val hikaricp: String by project
val postgresql: String by project
val jspecify: String by project
val jetBrainsAnnotations: String by project
val junitJupiter: String by project
val junitPlatformLauncher: String by project

plugins {
	id("java")
	id("maven-publish")
	id("net.luis.lm")
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
	runtimeOnly("com.fasterxml.jackson.core:jackson-databind:${jackson}") // JSON logging configuration support
	implementation("org.apache.commons:commons-lang3:${apacheLang}") // Utility
	// Database
	implementation("com.zaxxer:HikariCP:${hikaricp}") // Connection Pool
	runtimeOnly("org.postgresql:postgresql:${postgresql}") // PostgreSQL Driver
	// Other
	implementation("org.jspecify:jspecify:${jspecify}") // Nullability
	implementation("org.jetbrains:annotations:${jetBrainsAnnotations}") // Annotations
	// Test
	testImplementation("org.junit.jupiter:junit-jupiter:${junitJupiter}")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher:${junitPlatformLauncher}")
}

licenseManager {
	header = "header.txt"
	lineEnding = LineEnding.LF
	spacingAfterHeader = 1
	
	variable("year", Year.now())
	variable("author", "Luis Staudt")
	variable("project", rootProject.name)
	
	sourceSets = listOf("main", "test")
	
	include("**/*.java")
	exclude("**/Main.java")
}

tasks.named<JavaCompile>("compileJava") {
	dependsOn(tasks.named("updateLicenses"))
}

tasks.register<JavaExec>("runServer") {
	group = "runs"
	mainClass.set("net.luis.Main")
	classpath = sourceSets["main"].runtimeClasspath
	enableAssertions = true
	standardInput = System.`in`
	args = listOf(
		"--server",
		"-Dlog4j2.configurationFile=${sourceSets["main"].output.resourcesDir}/log4j2.json",
	)
}

tasks.register<JavaExec>("runClient") {
	group = "runs"
	mainClass.set("net.luis.Main")
	classpath = sourceSets["main"].runtimeClasspath
	enableAssertions = true
	standardInput = System.`in`
	args = listOf(
		"--client",
		UUID.fromString("00000000-0000-0000-0000-000000000000").toString(),
		"-Dlog4j2.configurationFile=${sourceSets["main"].output.resourcesDir}/log4j2.json"
	)
}

tasks.register<JavaExec>("spawnClient") {
	group = "runs"
	mainClass.set("net.luis.Main")
	classpath = sourceSets["main"].runtimeClasspath
	enableAssertions = true
	standardInput = System.`in`
	args = listOf(
		"--client",
		UUID.randomUUID().toString(),
		"-Dlog4j2.configurationFile=${sourceSets["main"].output.resourcesDir}/log4j2.json",
	)
}

