plugins {
    alias(libs.plugins.kotlin.jvm)
}

// A plain Kotlin module on purpose: with no Android SDK on the classpath, a Context, a Color or a
// Composable inside the domain is a compile error rather than something to catch in review.
kotlin {
    jvmToolchain(17)
}

dependencies {
    // paging-common is the pure Kotlin half of Paging: PagingSource, Pager and PagingConfig, with
    // no Android in it. The Android parts live in the modules that draw the list.
    implementation(libs.androidx.paging.common)
    implementation(libs.javax.inject)

    testImplementation(libs.bundles.unit.testing)
}
