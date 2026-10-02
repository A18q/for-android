package chat.stoat.sheets

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.hasPermission
import chat.stoat.api.routes.server.patchServer
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.generic.SheetButton
import chat.stoat.internals.Platform
import chat.stoat.internals.extensions.rememberServerPermissions
import chat.stoat.internals.server.UncategorisedChannelSectionId
import chat.stoat.internals.server.serverChannelSections
import chat.stoat.internals.server.toServerCategories
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TokenBlurple = Color(0xFF5865F2)
private val TokenDanger = Color(0xFFED4245)
private val TokenMuted = Color(0xFF949BA4)
private val TokenHeader = Color(0xFFF2F3F5)
private val SectionBg = Color(0xFF2B2D31)

@Composable
fun CategoryContextSheet(
    serverId: String,
    categoryId: String,
    onHideSheet: suspend () -> Unit,
    isCollapsed: Boolean = false,
    onToggleCollapse: (() -> Unit)? = null,
    onOpenCreateChannel: (() -> Unit)? = null,
) {
    val server = StoatAPI.serverCache[serverId]
    if (server == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val category = server.categories?.firstOrNull { it.id == categoryId }
    val isOwner = server.owner == StoatAPI.selfId
    val permissions by rememberServerPermissions(serverId)
    val canManage = isOwner || (permissions != null && (permissions!!.hasPermission(PermissionBit.ManageChannel) || permissions!!.hasPermission(PermissionBit.ManageServer)))

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf(category?.title.orEmpty()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = SectionBg,
            title = {
                Text("Rename Category", color = TokenHeader, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("Category Name", color = TokenMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TokenHeader,
                        unfocusedTextColor = TokenHeader,
                        focusedBorderColor = TokenBlurple,
                        unfocusedBorderColor = TokenMuted,
                        cursorColor = TokenBlurple
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameValue.isNotBlank()) {
                            coroutineScope.launch {
                                val sections = serverChannelSections(server).map { section ->
                                    if (section.id == categoryId) section.copy(title = renameValue.trim()) else section
                                }
                                patchServer(serverId, categories = sections.toServerCategories())
                                showRenameDialog = false
                                onHideSheet()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TokenBlurple),
                    enabled = renameValue.isNotBlank()
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = TokenMuted)
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = SectionBg,
            title = {
                Text("Delete Category?", color = TokenHeader, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Channels inside this category will not be deleted and will move to uncategorized.",
                    color = TokenMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val sections = serverChannelSections(server)
                            val removed = sections.firstOrNull { it.id == categoryId }
                            if (removed != null) {
                                val updated = sections
                                    .filterNot { it.id == categoryId }
                                    .map { section ->
                                        if (section.id == UncategorisedChannelSectionId) {
                                            section.copy(channelIds = section.channelIds + removed.channelIds)
                                        } else {
                                            section
                                        }
                                    }
                                patchServer(serverId, categories = updated.toServerCategories())
                            }
                            showDeleteConfirm = false
                            onHideSheet()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TokenDanger)
                ) {
                    Text("Delete Category", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TokenMuted)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        SheetButton(
            headlineContent = {
                Text(if (isCollapsed) "Expand Category" else "Collapse Category")
            },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_keyboard_arrow_right_24dp),
                    contentDescription = null
                )
            },
            onClick = {
                onToggleCollapse?.invoke()
                coroutineScope.launch { onHideSheet() }
            }
        )

        SheetButton(
            headlineContent = {
                Text("Copy Category ID")
            },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_identifier_copy_24dp),
                    contentDescription = null
                )
            },
            onClick = {
                clipboardManager.setText(AnnotatedString(categoryId))
                if (Platform.needsShowClipboardNotification()) {
                    Toast.makeText(context, "Category ID copied", Toast.LENGTH_SHORT).show()
                }
                coroutineScope.launch { onHideSheet() }
            }
        )

        if (canManage && categoryId != UncategorisedChannelSectionId) {
            HorizontalDivider(
                color = Color(0xFF3A3C42),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            SheetButton(
                headlineContent = {
                    Text("Create Channel")
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_24dp),
                        contentDescription = null
                    )
                },
                onClick = {
                    coroutineScope.launch {
                        onHideSheet()
                        onOpenCreateChannel?.invoke()
                    }
                }
            )

            SheetButton(
                headlineContent = {
                    Text("Rename Category")
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_24dp),
                        contentDescription = null
                    )
                },
                onClick = {
                    renameValue = category?.title.orEmpty()
                    showRenameDialog = true
                }
            )

            SheetButton(
                headlineContent = {
                    Text("Delete Category", color = TokenDanger)
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_24dp),
                        contentDescription = null,
                        tint = TokenDanger
                    )
                },
                onClick = {
                    showDeleteConfirm = true
                }
            )
        }
    }
}
