package dev.nohus.rift.network.requests

sealed class RateLimitGroup(val name: String) {
    data object Status : RateLimitGroup("status")
}
