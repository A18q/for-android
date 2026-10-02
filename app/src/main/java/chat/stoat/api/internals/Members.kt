package chat.stoat.api.internals

import chat.stoat.core.model.schemas.Member

import java.util.concurrent.ConcurrentHashMap

class Members {
    // memberCache (mapping of serverId to userId to member)
    private val memberCache = ConcurrentHashMap<String, ConcurrentHashMap<String, Member>>()

    fun getMember(serverId: String, userId: String): Member? {
        return memberCache[serverId]?.get(userId)
    }

    fun hasMember(serverId: String, userId: String): Boolean {
        return memberCache[serverId]?.containsKey(userId) ?: false
    }

    fun setMember(serverId: String, member: Member) {
        val serverMap = memberCache.getOrPut(serverId) { ConcurrentHashMap() }
        member.id?.user?.let { serverMap[it] = member }
    }

    fun removeMember(serverId: String, userId: String) {
        memberCache[serverId]?.remove(userId)
    }

    fun removeServer(serverId: String) {
        memberCache.remove(serverId)
    }

    fun clear() {
        memberCache.clear()
    }

    /**
     * Returns a Map of userId to server-nickname for the given serverId.
     */
    fun markdownMemberMapFor(serverId: String): Map<String, String> {
        val serverMembers = memberCache[serverId] ?: return emptyMap()
        val result = HashMap<String, String>(serverMembers.size)
        for ((userId, member) in serverMembers) {
            val nick = member.nickname
            if (nick != null) {
                result[userId] = nick
            }
        }
        return result
    }

    fun filterNamesFor(serverId: String, query: String): List<Member> {
        return memberCache[serverId]?.values?.filter { member ->
            member.nickname?.contains(query, ignoreCase = true) ?: false
        } ?: emptyList()
    }
}
