package maha.garage.domain.engine

object NotificationPolicy {
    fun shouldSend(now: Long, lastSent: Long, signature: String, previous: String?, cooldownDays: Int): Boolean {
        if (signature.isEmpty()) return false
        return lastSent == 0L || signature != previous || now < lastSent || now - lastSent >= cooldownDays * 86_400_000L
    }
}
