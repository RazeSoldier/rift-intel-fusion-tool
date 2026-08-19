package dev.nohus.rift.jabber

sealed interface JabberInputModel {
    data object None : JabberInputModel

    data class DirectMessage(val user: String) : JabberInputModel

    data class Channel(val channel: String) : JabberInputModel
}
