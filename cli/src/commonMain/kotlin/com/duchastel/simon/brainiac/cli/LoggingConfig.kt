package com.duchastel.simon.brainiac.cli

/**
 * Configure logging levels for the application.
 * By default, Koog logging is set to WARN to reduce verbosity.
 * When enableLogging is true, all logging is set to INFO.
 */
fun configureLogging(enableLogging: Boolean) {
    if (enableLogging) {
        // Enable verbose logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO")
    } else {
        // Suppress verbose Koog logging by default
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN")
        System.setProperty("org.slf4j.simpleLogger.log.ai.koog", "WARN")
    }
}
