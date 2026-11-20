pluginManagement {
	plugins {
		id("net.luis.lm") version "1.2.1"
	}
	
	repositories {
		gradlePluginPortal()
		mavenCentral()
		maven {
			url = uri("https://artifact.luis-st.net/maven/plugins/")
		}
	}
}

rootProject.name = "Fun-Java-Projects"
include("codec-serialization")
include("grammar")
include("utility")
include("code-generator")
include("network-test")
include("memory-db")
