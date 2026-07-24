package com.ericdevwang.jsonassetsminify

import java.nio.file.FileSystems
import java.nio.file.PathMatcher

internal fun matchesGlobPattern(path: String, pattern: String): Boolean =
    try {
        val matcher: PathMatcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")
        matcher.matches(FileSystems.getDefault().getPath(path))
    } catch (_: Exception) {
        path == pattern
    }
