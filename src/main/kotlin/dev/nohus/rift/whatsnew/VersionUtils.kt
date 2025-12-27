package dev.nohus.rift.whatsnew

import org.semver4j.Semver

object VersionUtils {

    private const val SEGMENTS = 3

    fun isNewer(base: String, test: String): Boolean {
        val baseVersion = Semver.parse(base)
        val testVersion = Semver.parse(test)
        return isNewer(baseVersion!!, testVersion!!)
    }

    fun isNewer(base: Semver, test: Semver): Boolean {
        return test.isGreaterThan(base)
    }
}
