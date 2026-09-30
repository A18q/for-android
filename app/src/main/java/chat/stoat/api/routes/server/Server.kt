package chat.stoat.api.routes.server

import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatAPIError
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import chat.stoat.core.model.schemas.Member
import chat.stoat.core.model.schemas.ServerWithChannelObjects
import chat.stoat.core.model.schemas.User
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

@Serializable
data class FetchMembersResponse(
    val members: List<Member>,
    val users: List<User>
)

suspend fun ackServer(serverId: String) {
    StoatHttp.put("/servers/$serverId/ack".api())
}

suspend fun fetchMembers(
    serverId: String,
    includeOffline: Boolean = false,
    pure: Boolean = false
): FetchMembersResponse {
    val response = StoatHttp.get("/servers/$serverId/members".api()) {
        parameter("exclude_offline", !includeOffline)
    }

    val responseContent = response.bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), responseContent)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val membersResponse =
        StoatJson.decodeFromString(FetchMembersResponse.serializer(), responseContent)

    if (pure) {
        return membersResponse
    }

    membersResponse.members.forEach { member ->
        if (!StoatAPI.members.hasMember(serverId, member.id!!.user)) {
            StoatAPI.members.setMember(serverId, member)
        }
    }

    membersResponse.users.forEach { user ->
        user.id?.let { StoatAPI.userCache.putIfAbsent(it, user) }
    }

    return membersResponse
}

suspend fun fetchMember(serverId: String, userId: String, pure: Boolean = false): Member {
    val response = StoatHttp.get("/servers/$serverId/members/$userId".api())

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response.bodyAsText())
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val member = StoatJson.decodeFromString(Member.serializer(), response.bodyAsText())

    if (!pure) {
        member.id?.let {
            if (!StoatAPI.members.hasMember(serverId, it.user)) {
                StoatAPI.members.setMember(serverId, member)
            }
        }
    }

    return member
}

suspend fun leaveOrDeleteServer(serverId: String, leaveSilently: Boolean = false) {
    StoatHttp.delete("/servers/$serverId".api()) {
        parameter("leave_silently", leaveSilently)
    }
}

@Serializable
data class ServerCreationBody(
    val name: String,
    val description: String? = null,
    val nsfw: Boolean = false
)

suspend fun createServer(
    name: String,
    description: String = "",
    nsfw: Boolean = false
): ServerWithChannelObjects {
    val body = ServerCreationBody(name, description, nsfw)

    val response = StoatHttp.post("/servers/create".api()) {
        setBody(StoatJson.encodeToString(ServerCreationBody.serializer(), body))
    }

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response.bodyAsText())
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    return StoatJson.decodeFromString(ServerWithChannelObjects.serializer(), response.bodyAsText())
}

@Serializable
data class CreateChannelBody(
    val type: String = "Text",
    val name: String,
    val description: String? = null
)

suspend fun createChannelInServer(
    serverId: String,
    name: String,
    type: String = "Text",
    description: String? = null
): chat.stoat.core.model.schemas.Channel? {
    val body = CreateChannelBody(type = type, name = name, description = description)
    val response = StoatHttp.post("/servers/$serverId/channels".api()) {
        contentType(ContentType.Application.Json)
        setBody(StoatJson.encodeToString(CreateChannelBody.serializer(), body))
    }
    val content = response.bodyAsText()
    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), content)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
    val channel = StoatJson.decodeFromString(chat.stoat.core.model.schemas.Channel.serializer(), content)
    val channelId = channel.id ?: return channel
    StoatAPI.channelCache[channelId] = channel
    val server = StoatAPI.serverCache[serverId]
    if (server != null) {
        val newChannels = (server.channels ?: emptyList()) + channelId
        StoatAPI.serverCache[serverId] = server.copy(channels = newChannels)
    }
    return channel
}

suspend fun editServer(
    serverId: String,
    name: String? = null,
    description: String? = null,
    icon: String? = null,
    banner: String? = null
): chat.stoat.core.model.schemas.Server? {
    val body = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
    if (name != null) body["name"] = StoatJson.encodeToJsonElement(String.serializer(), name)
    if (description != null) body["description"] = StoatJson.encodeToJsonElement(String.serializer(), description)
    if (icon != null) body["icon"] = StoatJson.encodeToJsonElement(String.serializer(), icon)
    if (banner != null) body["banner"] = StoatJson.encodeToJsonElement(String.serializer(), banner)

    val responseText = StoatHttp.patch("/servers/$serverId".api()) {
        contentType(ContentType.Application.Json)
        setBody(
            StoatJson.encodeToString(
                MapSerializer(String.serializer(), kotlinx.serialization.json.JsonElement.serializer()),
                body
            )
        )
    }.bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), responseText)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val server = StoatJson.decodeFromString(chat.stoat.core.model.schemas.Server.serializer(), responseText)
    StoatAPI.serverCache[serverId] = server
    return server
}

@Serializable
data class CreateRoleBody(
    val name: String,
    val rank: Double? = null
)

@Serializable
data class NewRoleResponse(
    val id: String,
    val role: chat.stoat.core.model.schemas.Role
)

suspend fun createRole(
    serverId: String,
    name: String,
    rank: Double? = null
): NewRoleResponse {
    val body = CreateRoleBody(name = name, rank = rank)
    val responseText = StoatHttp.post("/servers/$serverId/roles".api()) {
        contentType(ContentType.Application.Json)
        setBody(StoatJson.encodeToString(CreateRoleBody.serializer(), body))
    }.bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), responseText)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val newRoleResponse = StoatJson.decodeFromString(NewRoleResponse.serializer(), responseText)
    val server = StoatAPI.serverCache[serverId]
    if (server != null) {
        val newRoles = (server.roles ?: emptyMap()) + (newRoleResponse.id to newRoleResponse.role)
        StoatAPI.serverCache[serverId] = server.copy(roles = newRoles)
    }
    return newRoleResponse
}

suspend fun editRole(
    serverId: String,
    roleId: String,
    name: String? = null,
    colour: String? = null,
    hoist: Boolean? = null,
    rank: Double? = null,
    permissions: chat.stoat.core.model.schemas.PermissionDescription? = null,
    remove: List<String>? = null
): chat.stoat.core.model.schemas.Role {
    val body = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
    if (name != null) body["name"] = StoatJson.encodeToJsonElement(String.serializer(), name)
    if (colour != null) body["colour"] = StoatJson.encodeToJsonElement(String.serializer(), colour)
    if (hoist != null) body["hoist"] = StoatJson.encodeToJsonElement(Boolean.serializer(), hoist)
    if (rank != null) body["rank"] = StoatJson.encodeToJsonElement(Double.serializer(), rank)
    if (permissions != null) body["permissions"] = StoatJson.encodeToJsonElement(chat.stoat.core.model.schemas.PermissionDescription.serializer(), permissions)
    if (remove != null) body["remove"] = StoatJson.encodeToJsonElement(kotlinx.serialization.builtins.ListSerializer(String.serializer()), remove)

    val responseText = StoatHttp.patch("/servers/$serverId/roles/$roleId".api()) {
        contentType(ContentType.Application.Json)
        setBody(
            StoatJson.encodeToString(
                MapSerializer(String.serializer(), kotlinx.serialization.json.JsonElement.serializer()),
                body
            )
        )
    }.bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), responseText)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val role = StoatJson.decodeFromString(chat.stoat.core.model.schemas.Role.serializer(), responseText)
    val server = StoatAPI.serverCache[serverId]
    if (server != null) {
        val newRoles = (server.roles ?: emptyMap()) + (roleId to role)
        StoatAPI.serverCache[serverId] = server.copy(roles = newRoles)
    }
    return role
}

suspend fun deleteRole(
    serverId: String,
    roleId: String
) {
    StoatHttp.delete("/servers/$serverId/roles/$roleId".api())
    val server = StoatAPI.serverCache[serverId]
    if (server != null) {
        val newRoles = (server.roles ?: emptyMap()).toMutableMap()
        newRoles.remove(roleId)
        StoatAPI.serverCache[serverId] = server.copy(roles = newRoles)
    }
}