package chat.stoat.screens.settings

import android.app.Application
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import chat.stoat.api.routes.microservices.autumn.uploadToAutumn
import chat.stoat.api.routes.user.fetchUserProfile
import chat.stoat.api.routes.user.patchSelf
import chat.stoat.composables.generic.InlineMediaPicker
import chat.stoat.composables.screens.settings.RawUserOverview
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.Profile
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.io.File

class ProfileSettingsScreenViewModel(val context: Application) :
    ViewModel() {
    var isLoading by mutableStateOf(true)
    var pfpModel by mutableStateOf<Any?>(null)
    var currentProfile by mutableStateOf<Profile?>(null)
    var pendingProfile by mutableStateOf<Profile?>(null)
    var backgroundModel by mutableStateOf<Any?>(null)
    var uploadProgress by mutableFloatStateOf(0f)
    var uploadError by mutableStateOf<String?>(null)
    var bioError by mutableStateOf<String?>(null)
    var currentDisplayName by mutableStateOf<String?>(null)
    var pendingDisplayName by mutableStateOf("")
    var displayNameError by mutableStateOf<String?>(null)
    var currentPronouns by mutableStateOf<String?>(null)
    var pendingPronouns by mutableStateOf("")
    var pronounsError by mutableStateOf<String?>(null)

    init {
        StoatAPI.selfId?.let { self ->
            StoatAPI.userCache[self]?.let { user ->
                user.avatar?.id?.let {
                    pfpModel = "$STOAT_FILES/avatars/${it}"
                }
                currentDisplayName = user.displayName
                pendingDisplayName = user.displayName.orEmpty()
                currentPronouns = user.pronouns
                pendingPronouns = user.pronouns.orEmpty()
            }
            viewModelScope.launch {
                try {
                    val profile = fetchUserProfile(self)
                    currentProfile = profile
                    profile.background?.let { bg ->
                        val bgId = bg.id
                        if (!bgId.isNullOrBlank()) {
                            backgroundModel = if (!bg.filename.isNullOrBlank()) {
                                "$STOAT_FILES/backgrounds/$bgId/${bg.filename}"
                            } else {
                                "$STOAT_FILES/backgrounds/$bgId"
                            }
                        }
                    }
                    pendingProfile = profile.copy()
                } catch (e: Exception) {
                    currentProfile = Profile()
                    pendingProfile = Profile()
                } finally {
                    isLoading = false
                }
            }
        }

    }

    private suspend fun prepareUploadFile(uri: Uri, prefix: String): Pair<File, Pair<String, String>> =
        withContext(Dispatchers.IO) {
            val mimeType = context.contentResolver.getType(uri) ?: "image/png"
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                ?: when (mimeType) {
                    "image/png" -> "png"
                    "image/jpeg", "image/jpg" -> "jpg"
                    "image/webp" -> "webp"
                    "image/gif" -> "gif"
                    else -> "png"
                }

            var resolvedName: String? = null
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            resolvedName = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Exception) {}

            val sanitizedName = resolvedName?.substringAfterLast('/')?.substringAfterLast('\\')
            val fileName = if (!sanitizedName.isNullOrBlank() && sanitizedName.contains('.')) {
                sanitizedName
            } else {
                "${prefix}_${System.currentTimeMillis()}.$extension"
            }

            val tempFile = File.createTempFile("stoat-$prefix-", ".$extension", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalArgumentException("Could not read image file")

            Pair(tempFile, Pair(fileName, mimeType))
        }

    fun saveNewPfp(targetUri: Uri? = null) {
        uploadError = null

        val uri = targetUri ?: when (val model = pfpModel) {
            is Uri -> model
            is String -> Uri.parse(model)
            else -> return
        }

        viewModelScope.launch {
            var tempFile: File? = null
            try {
                val (file, meta) = prepareUploadFile(uri, "avatar")
                tempFile = file
                val (fileName, mimeType) = meta

                val parsedContentType = try {
                    ContentType.parse(mimeType)
                } catch (_: Exception) {
                    ContentType.Image.PNG
                }

                val id = uploadToAutumn(
                    file = file,
                    name = fileName,
                    tag = "avatars",
                    contentType = parsedContentType,
                    onProgress = { soFar, outOf ->
                        uploadProgress = if (outOf > 0) soFar.toFloat() / outOf.toFloat() else 0f
                    }
                )

                patchSelf(avatar = id)

                pfpModel = StoatAPI.userCache[StoatAPI.selfId]?.avatar?.id?.let {
                    "$STOAT_FILES/avatars/${it}"
                }
            } catch (e: Exception) {
                uploadError = e.message ?: "Failed to upload avatar"
            } finally {
                uploadProgress = 0f
                tempFile?.let { f ->
                    withContext(Dispatchers.IO) { runCatching { f.delete() } }
                }
            }
        }
    }

    fun saveNewBackground(targetUri: Uri? = null) {
        uploadError = null

        val uri = targetUri ?: when (val model = backgroundModel) {
            is Uri -> model
            is String -> Uri.parse(model)
            else -> return
        }

        viewModelScope.launch {
            var tempFile: File? = null
            try {
                val (file, meta) = prepareUploadFile(uri, "profile_bg")
                tempFile = file
                val (fileName, mimeType) = meta

                val parsedContentType = try {
                    ContentType.parse(mimeType)
                } catch (_: Exception) {
                    ContentType.Image.PNG
                }

                val id = uploadToAutumn(
                    file = file,
                    name = fileName,
                    tag = "backgrounds",
                    contentType = parsedContentType,
                    onProgress = { soFar, outOf ->
                        uploadProgress = if (outOf > 0) soFar.toFloat() / outOf.toFloat() else 0f
                    }
                )

                patchSelf(
                    background = id,
                    bio = pendingProfile?.content?.takeIf { it.isNotBlank() }
                )

                StoatAPI.selfId?.let { selfId ->
                    val profile = fetchUserProfile(selfId)
                    currentProfile = profile
                    pendingProfile = profile

                    StoatAPI.userCache[selfId]?.let { u ->
                        StoatAPI.userCache[selfId] = u.copy(profile = profile)
                    }

                    backgroundModel = profile.background?.let { bg ->
                        val bgId = bg.id
                        if (!bgId.isNullOrBlank()) {
                            if (!bg.filename.isNullOrBlank()) {
                                "$STOAT_FILES/backgrounds/$bgId/${bg.filename}"
                            } else {
                                "$STOAT_FILES/backgrounds/$bgId"
                            }
                        } else null
                    }
                }
            } catch (e: Exception) {
                uploadError = e.message ?: "Failed to upload background"
            } finally {
                uploadProgress = 0f
                tempFile?.let { f ->
                    withContext(Dispatchers.IO) { runCatching { f.delete() } }
                }
            }
        }
    }

    fun removePfp() {
        viewModelScope.launch {
            try {
                patchSelf(remove = listOf("Avatar"))
                pfpModel = null
                StoatAPI.selfId?.let { selfId ->
                    StoatAPI.userCache[selfId]?.let { u ->
                        StoatAPI.userCache[selfId] = u.copy(avatar = null)
                    }
                }
            } catch (e: Exception) {
                uploadError = e.message
            }
        }
    }

    fun removeBackground() {
        viewModelScope.launch {
            try {
                patchSelf(remove = listOf("ProfileBackground"))
                backgroundModel = null
                StoatAPI.selfId?.let { selfId ->
                    val profile = fetchUserProfile(selfId)
                    currentProfile = profile
                    pendingProfile = profile
                    StoatAPI.userCache[selfId]?.let { u ->
                        StoatAPI.userCache[selfId] = u.copy(profile = profile)
                    }
                }
            } catch (e: Exception) {
                uploadError = e.message
            }
        }
    }

    fun saveBio() {
        bioError = null
        viewModelScope.launch {
            try {
                patchSelf(
                    bio = pendingProfile?.content,
                    background = currentProfile?.background?.id
                )

                StoatAPI.selfId?.let { selfId ->
                    val profile = fetchUserProfile(selfId)
                    currentProfile = profile
                    pendingProfile = profile
                    StoatAPI.userCache[selfId]?.let { u ->
                        StoatAPI.userCache[selfId] = u.copy(profile = profile)
                    }
                }
            } catch (e: Exception) {
                bioError = e.message
            }
        }
    }



    fun savePronouns() {
        pronounsError = null
        val normalizedPronouns = pendingPronouns.trim()

        viewModelScope.launch {
            try {
                if (normalizedPronouns.isEmpty()) {
                    patchSelf(remove = listOf("Pronouns"))
                } else {
                    patchSelf(pronouns = normalizedPronouns)
                }

                currentPronouns = normalizedPronouns.ifEmpty { null }
                pendingPronouns = normalizedPronouns
            } catch (e: Exception) {
                pronounsError = e.message
            }
        }
    }

    fun saveDisplayName() {
        displayNameError = null
        val normalizedDisplayName = pendingDisplayName.trim()

        viewModelScope.launch {
            try {
                if (normalizedDisplayName.isEmpty()) {
                    patchSelf(remove = listOf("DisplayName"))
                } else {
                    patchSelf(displayName = normalizedDisplayName)
                }

                currentDisplayName = normalizedDisplayName.ifEmpty { null }
                pendingDisplayName = normalizedDisplayName
            } catch (e: Exception) {
                displayNameError = e.message
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    navController: NavController,
    viewModel: ProfileSettingsScreenViewModel = koinViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.settings_profile),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_24dp),
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
            )
        },
    ) { pv ->
        Box(
            Modifier
                .padding(pv)
                .imePadding()
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (viewModel.isLoading) {
                            Modifier
                        } else {
                            Modifier.verticalScroll(scrollState)
                        }
                    ),
                verticalArrangement = if (viewModel.isLoading) {
                    Arrangement.Center
                } else {
                    Arrangement.Top
                },
                horizontalAlignment = if (viewModel.isLoading) {
                    Alignment.CenterHorizontally
                } else {
                    Alignment.Start
                }
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                    )
                } else {
                    StoatAPI.userCache[StoatAPI.selfId]?.let {
                        RawUserOverview(
                            it,
                            viewModel.pendingProfile,
                            viewModel.pfpModel?.toString(),
                            viewModel.backgroundModel?.toString()
                        )
                    }

                    AnimatedVisibility(visible = viewModel.uploadProgress > 0f) {
                        LinearProgressIndicator(
                            progress = { viewModel.uploadProgress },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 0.dp)
                        )
                    }

                    AnimatedVisibility(visible = viewModel.uploadError != null) {
                        Text(
                            text = viewModel.uploadError ?: "",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 0.dp)
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 0.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_profile_profile_picture),
                                style = MaterialTheme.typography.labelLarge
                            )

                            Spacer(Modifier.height(10.dp))

                            InlineMediaPicker(
                                currentModel = viewModel.pfpModel,
                                circular = true,
                                useAvatarCircularity = true,
                                onPick = { uri ->
                                    viewModel.pfpModel = uri
                                    viewModel.saveNewPfp(uri)
                                },
                                canRemove = true,
                                onRemove = {
                                    viewModel.removePfp()
                                }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_profile_custom_background),
                                style = MaterialTheme.typography.labelLarge,
                            )

                            Spacer(Modifier.height(10.dp))

                            InlineMediaPicker(
                                currentModel = viewModel.backgroundModel,
                                onPick = { uri ->
                                    viewModel.backgroundModel = uri
                                    viewModel.saveNewBackground(uri)
                                },
                                canRemove = true,
                                onRemove = {
                                    viewModel.removeBackground()
                                }
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 20.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.pendingDisplayName,
                            onValueChange = { value ->
                                if (value.length <= 32) {
                                    viewModel.pendingDisplayName = value
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.settings_profile_display_name),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            isError = viewModel.displayNameError != null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        AnimatedVisibility(visible = viewModel.displayNameError != null) {
                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = viewModel.displayNameError ?: "",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 0.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        TextButton(
                            onClick = viewModel::saveDisplayName,
                            enabled = viewModel.pendingDisplayName.trim().ifEmpty { null } !=
                                    viewModel.currentDisplayName,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_24dp),
                                contentDescription = null
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = stringResource(id = R.string.settings_profile_save),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = viewModel.pendingPronouns,
                            onValueChange = { value ->
                                if (value.length <= 24) {
                                    viewModel.pendingPronouns = value
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(id = R.string.settings_profile_pronouns),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            isError = viewModel.pronounsError != null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        AnimatedVisibility(visible = viewModel.pronounsError != null) {
                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = viewModel.pronounsError ?: "",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 0.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                viewModel.savePronouns()
                            },
                            enabled = viewModel.pendingPronouns.trim().ifEmpty { null } !=
                                    viewModel.currentPronouns,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_24dp),
                                contentDescription = null
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = stringResource(id = R.string.settings_profile_save),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = viewModel.pendingProfile?.content ?: "",
                            onValueChange = { value ->
                                viewModel.pendingProfile?.let {
                                    viewModel.pendingProfile = it.copy(content = value)
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(id = R.string.user_info_sheet_category_bio),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        AnimatedVisibility(visible = viewModel.bioError != null) {
                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = viewModel.bioError ?: "",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 0.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                viewModel.saveBio()
                            },
                            enabled = viewModel.pendingProfile?.content != viewModel.currentProfile?.content,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_24dp),
                                contentDescription = null
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = stringResource(id = R.string.settings_profile_save),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
