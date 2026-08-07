#!/bin/bash
sed -i '/implementation(libs.androidx.core.ktx)/a \    implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")' app/build.gradle.kts
gradle :app:dependencies | grep jgit
