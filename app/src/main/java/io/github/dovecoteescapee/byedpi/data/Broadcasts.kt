package io.github.dovecoteescapee.byedpi.data

const val STARTED_BROADCAST = "io.github.romanvht.byedpi.STARTED"
const val STOPPED_BROADCAST = "io.github.romanvht.byedpi.STOPPED"
const val FAILED_BROADCAST = "io.github.romanvht.byedpi.FAILED"

const val SENDER = "sender"

enum class Sender(val senderName: String) {
    Proxy("Proxy"),
    VPN("VPN")
}