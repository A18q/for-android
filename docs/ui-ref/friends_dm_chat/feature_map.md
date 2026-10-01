<Feature Map: Friends List & DM Chat>
## Discord Layout (top → bottom, left → right)

### 1. Friends List Screen (`FriendsScreen`)
1. **Top App Bar**:
   - Navigation drawer hamburger (`ic_menu_24dp`) or Back arrow (`ic_arrow_back_24dp`)
   - Title: `"Friends"` (20sp bold, `#F2F3F5`)
   - Action: Overflow menu (`ic_more_vert_24dp`) → `"Deny all incoming"` friend requests
2. **Friends Directory (`LazyColumn` with sticky headers)**:
   - **Incoming Requests Header**: `"INCOMING FRIEND REQUESTS"` + numeric count badge pill
     - Request row: Avatar (40dp circle), display name, username handle, Accept button (`✓`, `#23A55A`), Decline button (`✕`)
   - **Outgoing Requests Header**: `"OUTGOING FRIEND REQUESTS"` + numeric count badge pill
     - Request row: Avatar, display name, handle, Cancel button (`✕`)
   - **Online Friends Header**: `"ONLINE — [count]"`
     - Friend row: 40dp circular avatar + live presence ring (`#23A55A`, `#F0B232`, `#F23F43`), display name (15sp bold), custom status subtitle (12sp `#949BA4`), right quick-action pair (1-tap `💬` Message + `📞` Call)
   - **All / Offline Friends Header**: `"ALL FRIENDS — [count]"`
     - Friend row: Avatar + offline status ring (`#80848E`), display name, offline status text, 1-tap `💬` Message + `📞` Call buttons
   - **Blocked Users Header**: `"BLOCKED — [count]"`
     - Blocked row: Avatar, username, `"Unblock"` action button
3. **Floating Action Button Dock (`FloatingActionButtonMenu`)**:
   - Primary toggle FAB (`#5865F2` blurple pill/circle, animated `+` to `✕`)
   - Speed dial options: `"New Group"` (`ic_group_add_24dp`), `"Scan QR"` (`ic_qr_code_scanner_24dp`), `"Add by Tag"` (`ic_tag_24dp`)
4. **Add Friends Bottom Sheets**:
   - **Add by Tag Sheet**: Nametag illustration graphic, title `"Add by Username"`, description, username `TextField`, `#` tag `TextField`, `"Paste from Clipboard"` button, `"Send Friend Request"` blurple button
   - **Scan QR Code Sheet**: Camera viewfinder scanner, QR user result sheet with avatar, display name, handle, `"Add Friend"`, and `"Cancel"` buttons

---

### 2. Direct Message (DM) Chat Screen (`ChannelScreen`)
1. **Top App Bar** (Height 48dp, `#313338` background, 1dp bottom border):
   - Navigation icon (hamburger `ic_menu_24dp` or back `ic_arrow_back_24dp`)
   - Partner identity header (interactive tap → `ChannelInfoSheet`):
     - 24dp circular avatar with 10dp live presence dot
     - Display name (20sp bold `#F2F3F5`)
     - Presence status text / badge
     - Right chevron indicator (`ic_keyboard_arrow_right_24dp`, 16dp)
   - Actions:
     - Voice Call button (`ic_call_24dp__fill`, `#DBDEE1`)
     - Video Call button (`ic_videocam_24dp`, `#DBDEE1`)
     - Search button (`ic_search_24dp`, `#DBDEE1`)
2. **Active Voice Call Banner** (conditional):
   - Green `#23A55A` banner when voice call is active
3. **Chat History Viewport (`LazyColumn` reversed layout)**:
   - **Start of Conversation Header** (`ConversationStartHeader`): 80dp circular avatar, 22dp presence dot, 28sp bold display name, `@username` handle, signature welcome text, `"Wave to [Name] 👋"` card button
   - **Date Dividers** (`DateDivider`): Centered `#2B2D31` capsule with date text (11sp bold `#949BA4`)
   - **Message Items** (`RegularMessage` / `Message`):
     - **Reddit-Style Branching Reply Spine** (`InReplyTo`): Curved 2dp stroke line (`#4E5058`), 16dp circular parent avatar, author name, `@` mention indicator, quote snippet preview; tap smooth-scrolls and highlights parent message
     - **Main Message Bubble**: 40dp circular avatar, author name with role color, badges, relative/absolute timestamp (12sp `#949BA4`), edited icon, markdown body text, Gigamoji font scaling (5x single, 2x multi)
     - **Attachment Cards**: Image/video thumbnails with 8dp rounded corners; document card (280dp, `#2B2D31`, filename, size, download icon)
     - **Reactions FlowRow**: Reaction pills with emoji + count, active tint for self-reactions, `+` add reaction button
   - **Scroll to Bottom FAB** (`ScrollDownFAB`): Floating action button with down arrow, unread badge counter pill (`"NEW"`)
4. **Chat Input Dock / Message Composer (`MessageField`)**:
   - **Typing Indicator**: `"[User] is typing..."` with animated pulsing dots and slowmode countdown
   - **Replying Indicator Bar** (`ReplyManager`): `"Replying to [Name]"`, parent snippet, `@` mention toggle (`@ ON` / `@ OFF`), `✕` dismiss button
   - **Draft Attachments Strip** (`AttachmentManager`): Thumbnails of pending files with remove and spoiler toggles
   - **Autocomplete Suggestion Chips**: Horizontal bar for `@user`, `#channel`, and `:emoji:` autocomplete
   - **Input Bar Surface**: `#2B2D31` squircle/pill shape (`RoundedCornerShape(24dp)`)
   - **Left Attachment Button (`+`)**: 32dp circle, opens gallery/camera/files drawer
   - **[CRITICAL MONETIZATION PURGE]**: Nitro Gift button (`🎁`) next to `+` is **COMPLETELY DELETED**, closing the gap
   - **Auto-expanding Text Input**: Multi-line `BasicTextField`, placeholder `"Message @[Name]..."`
   - **Right Emoji Picker Trigger (`😄`)**: Toggles zero-bloat emoji/GIF/sticker sheet (all custom server emojis unlocked, zero Nitro padlocks)
   - **Right Action Button**: Send button (`➤`, `#5865F2` blurple pill) when text/attachment present, or push-to-talk Voice Recording Mic (`ic_mic_24dp`) when empty

## Mapping Table
| Discord Element | Status | Stoat File/Composable | Notes |
|---|---|---|---|
| **Friends: Top App Bar** | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/FriendsScreen.kt` | Lines 572–638: `TopAppBar` with menu toggle, title, and overflow menu |
| **Friends: Overflow Menu ("Deny all incoming")** | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/FriendsScreen.kt` | Lines 604–635: Dropdown menu with bulk rejection of pending requests |
| **Friends: Section Sticky Headers** | MAPPED | `app/src/main/java/chat/stoat/composables/generic/CountableListHeader.kt` (`CountableListHeader`) | Used for Incoming, Outgoing, Online, Offline, and Blocked buckets |
| **Friends: Friend Request Item Row** | MAPPED | `app/src/main/java/chat/stoat/composables/chat/MemberListItem.kt` (`MemberListItem`) | Displays avatar, display name, handle; accept/deny actions wired in `FriendsScreen.kt` |
| **Friends: Online/Offline Friend Item Row** | MAPPED | `app/src/main/java/chat/stoat/composables/chat/MemberListItem.kt` (`MemberListItem`) | Displays 40dp circular avatar, presence dot, name, status text |
| **Friends: 1-Tap Quick Actions (Message + Call)** | MAPPED | `app/src/main/java/chat/stoat/composables/chat/MemberListItem.kt` (`trailingContent`) | Modern Discord feature: `trailingContent` slot in `MemberListItem` can house direct call + message buttons |
| **Friends: Speed Dial FAB Menu** | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/FriendsScreen.kt` | Lines 813–882: `FloatingActionButtonMenu` with Group Add, QR Scan, Tag Add |
| **Friends: Add by Tag Sheet** | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/FriendsScreen.kt` | Lines 180–370: `ModalBottomSheet` with Nametag illustration, username/tag inputs |
| **Friends: Scan QR Code Flow** | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/FriendsScreen.kt` | Lines 372–569: Quickie QR scanner integration with result sheet |
| **Friends: Contact Sync Telemetry Prompts** | DISCORD-ONLY (MONETIZATION) | None | **HARD PURGE**: No phonebook scraping, no telemetry nag cards |
| **Chat: Top App Bar** | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/channel/ChannelScreen.kt` | Lines 757–913: `TopAppBar` with avatar, name, presence badge, chevron |
| **Chat: Voice Call Topbar Button** | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/channel/ChannelScreen.kt` | Lines 876–897: `ic_call_24dp__fill` opens voice overlay |
| **Chat: Video Call Topbar Button** | DISCORD-ONLY (NO NITRO/MONETIZATION) | None | **SKIP**: Video call action not separate from voice overlay; no dead button |
| **Chat: Channel Search Topbar Button** | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/channel/ChannelScreen.kt` | Lines 898–909: `ic_search_24dp` opens channel search view |
| **Chat: Voice Call Active Banner** | MAPPED | `app/src/main/java/chat/stoat/composables/voice/VoiceCallBanner.kt` (`VoiceCallBanner`) | Line 912: In-call header banner |
| **Chat: Start of Conversation Header** | MAPPED | `app/src/main/java/chat/stoat/composables/screens/chat/ConversationStartHeader.kt` (`ConversationStartHeader`) | Lines 1154–1161: 80dp avatar, 28sp name, handle, welcome text, "Wave 👋" button |
| **Chat: Date Dividers** | MAPPED | `app/src/main/java/chat/stoat/composables/chat/DateDivider.kt` (`DateDivider`) | Line 1122: Centered date capsule |
| **Chat: Reddit-Style Branching Reply Spine** | MAPPED | `app/src/main/java/chat/stoat/composables/chat/InReplyTo.kt` (`InReplyTo`) | 2dp stroke `#4E5058` curve, 16dp avatar, author, snippet; tap jumps & highlights |
| **Chat: Message Item & Avatars** | MAPPED | `app/src/main/java/chat/stoat/composables/chat/Message.kt` (`Message`) | 40dp circular avatar, author name, timestamp, role color, markdown prose |
| **Chat: Media & File Attachments** | MAPPED | `app/src/main/java/chat/stoat/composables/chat/Message.kt` (`MessageAttachment`) | Rounded thumbnails, document card with download action |
| **Chat: Reactions Bar** | MAPPED | `app/src/main/java/chat/stoat/composables/chat/Message.kt` (`Reaction`) | Lines 657–723: Horizontal `FlowRow` of reaction pills + add reaction button |
| **Chat: Scroll to Bottom FAB** | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/channel/ChannelScreen.kt` | Lines 1312–1364: FAB with down arrow + unread badge count pill |
| **Chat: Typing Indicator** | MAPPED | `app/src/main/java/chat/stoat/composables/screens/chat/TypingIndicator.kt` (`TypingIndicator`) | Lines 1384–1393: Pulsing dots, user names, slowmode countdown |
| **Chat: Replying Bar Dock** | MAPPED | `app/src/main/java/chat/stoat/composables/screens/chat/ReplyManager.kt` (`ReplyManager`) | Lines 1552–1570: Parent snippet, avatar, mention toggle, dismiss button |
| **Chat: Draft Attachments Strip** | MAPPED | `app/src/main/java/chat/stoat/composables/screens/chat/AttachmentManager.kt` (`AttachmentManager`) | Lines 1582–1612: Horizontal file list with remove/spoiler options |
| **Chat: Autocomplete Suggestions** | MAPPED | `app/src/main/java/chat/stoat/composables/chat/MessageField.kt` (`MessageField`) | Lines 583–809: Suggestion chips for users, channels, roles, emojis |
| **Chat: Composer Container Surface** | MAPPED | `app/src/main/java/chat/stoat/composables/chat/MessageField.kt` (`MessageField`) | Lines 571–580: `RoundedCornerShape(28dp)` pill container, `#2B2D31` |
| **Chat: Attachment Picker Button (`+`)** | MAPPED | `app/src/main/java/chat/stoat/composables/chat/MessageField.kt` (`MessageField`) | Lines 821–839: `ic_add_24dp` button opening attachment sheet |
| **Chat: Nitro Gift Button (`🎁`)** | DISCORD-ONLY (MONETIZATION) | None | **HARD PURGE**: Deleted; text field directly borders `+` button, zero gap |
| **Chat: Auto-Expanding Text Input** | MAPPED | `app/src/main/java/chat/stoat/composables/chat/MessageField.kt` (`MessageField`) | Lines 841–933: `BasicTextField` with multi-line expand and placeholder |
| **Chat: Emoji Picker Trigger (`😄`)** | MAPPED | `app/src/main/java/chat/stoat/composables/chat/MessageField.kt` (`MessageField`) | Lines 934–948: `ic_mood_24dp` opening emoji picker (zero-bloat, unlocked) |
| **Chat: Voice Message Recording Mic** | MAPPED | `app/src/main/java/chat/stoat/composables/chat/MessageField.kt` (`MessageField`) | Lines 950–1045: `ic_mic_24dp` push-to-talk voice recording |
| **Chat: Blurple Send Button (`➤`)** | MAPPED | `app/src/main/java/chat/stoat/composables/chat/MessageField.kt` (`MessageField`) | Lines 1056–1082: Animates in when content present, `#5865F2` pill |

## Changes Required (Restyle Only)

### Friends Screen Restyle:
1. **Background & Surface Hierarchy**:
   - `FriendsScreen` Scaffold currently inherits M3 default background; restyle container to `DiscordDarkBg = Color(0xFF1E1F22)` (`var(--bg-tertiary)`).
   - TopAppBar restyle: background `#1E1F22`, title `#F2F3F5` (20sp bold).
   - `MemberListItem` container color: restyle from M3 `surfaceContainer` to `#2B2D31` (`var(--bg-secondary)`) with `RoundedCornerShape(8dp)` or transparent list background with subtle `#35373C` dividers.
2. **Typography & Muted Text**:
   - Display names: `15.sp`, `FontWeight.SemiBold`, color `#F2F3F5`.
   - Subtitle / Custom Status text: `12.sp`, color `DiscordTextMuted = Color(0xFF949BA4)`.
3. **1-Tap Quick Action Pair**:
   - In `FriendsScreen.kt`, populate `MemberListItem.trailingContent` with modern Discord 1-tap action buttons:
     - Direct Message icon (`ic_chat_24dp`, 20dp, tint `#949BA4`) in a 36dp squircle hitbox.
     - Direct Call icon (`ic_call_24dp`, 20dp, tint `#949BA4`) in a 36dp squircle hitbox.
   - For incoming friend requests: ensure Accept (`✓`, `#23A55A`) and Decline (`✕`, `#ED4245` or `#35373C`) buttons are styled with 32dp circular hitboxes.

### DM Chat Screen Restyle:
1. **Chat Viewport & Background**:
   - Scaffold background: `Color(0xFF313338)` (`var(--bg-primary)`).
   - TopAppBar: `Color(0xFF313338)` with subtle bottom border `rgba(0,0,0,0.25)`.
2. **Branching Reply Spine Invariant**:
   - `InReplyTo.kt` path curvature: Ensure spine stroke is strictly 2dp solid `#4E5058` with 8dp corner radius (`M 8 12 L 8 4 Q 8 1 14 1 L 24 1`).
   - Quote snippet text color: `#B5BAC1` (12sp normal).
   - Author name: 12sp bold, role-colored.
3. **Message Composer Dock**:
   - Composer background: `#2B2D31` (`var(--bg-secondary)`), shape `RoundedCornerShape(24dp)`.
   - Text field hint color: `#949BA4`.
   - Send button: `RoundedCornerShape(16dp)` squircle/pill in brand blurple `#5865F2`, white send arrow.
   - Remove any remaining default Material3 outline borders; use Discord's flat card elevation.

## Slots to Delete (Monetization)
1. **Nitro Gift Button (`🎁`) in Composer Dock**:
   - Modern Discord includes a Nitro Gift button adjacent to the `+` attachment button.
   - **Stoat Rule**: Permanently excised. Zero gift slot, zero gap. The text field directly abuts the `+` attachment button.
2. **Nitro Emoji & Sticker Padlocks**:
   - Discord locks external server emojis behind Nitro upsells.
   - **Stoat Rule**: All server emojis in Stoat are client-side unlocked across all channels and DMs. Zero padlock badges, zero upsell bottom sheets.
3. **Contact Sync Telemetry Nags**:
   - Modern Discord displays aggressive banners prompting users to upload their address book for "friend suggestions".
   - **Stoat Rule**: Purged. Stoat uses direct handles (`username#tag` / global handle) and local QR codes only.
4. **Profile Effects & Avatar Frames**:
   - WebGL profile decorations and animated profile effect purchases are eliminated. Keep clean circular user avatars.
5. **Super Reactions & Nitro Badges**:
   - Paywalled reaction animations and Nitro badges are purged from message context menus and author profiles.
