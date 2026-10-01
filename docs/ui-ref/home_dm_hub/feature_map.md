<Feature Map: Home / DM Hub>
## Discord Layout (top → bottom, left → right)
1. **Top App Bar**:
   - Navigation drawer hamburger button (`ic_menu_24dp`, 36dp touch target)
   - Screen Title: `"Messages"` (24sp Bold, `#F2F3F5`)
2. **Action Button Row** (horizontal bar below title):
   - Search toggle button (38dp squircle, `#2B2D31` surface, search icon, turns blurple `#5865F2` when expanded)
   - Message Requests / Inbox Mail button (38dp squircle, `#2B2D31` surface, mail icon, navigates to Friends/Requests)
   - "Add Friends" pill button (38dp height squircle, `#2B2D31` surface, person add icon + `"Add Friends"` label)
   - New DM "+" button (38dp squircle, `#5865F2` blurple surface, white plus icon)
3. **Expandable Search Bar** (animated collapsible field):
   - 50dp height `OutlinedTextField`
   - Background `#2B2D31`, focused border `#5865F2`, unfocused border `Transparent`
   - Leading search icon, trailing clear `✕` button when text is present
   - Placeholder: `"Search direct messages..."` in `#949BA4`
4. **"Active Now" / Favorites Story Carousel** (horizontal scroll):
   - `LazyRow` with 16dp horizontal padding and 10dp item spacing
   - `ActiveNowFriendCard`: 86dp squircle (`RoundedCornerShape(20dp)`), `#2B2D31` background
   - 52dp circular `UserAvatar` centered with 16dp presence dot badge and 2dp ring cutout
   - Live presence colors: Online (`#23A55A`), Idle (`#F0B232`), DND (`#F23F43`), Offline (`#80848E`)
5. **Direct Messages Conversation List** (`LazyColumn`):
   - `DirectMessageItemRow` (72dp height, `RoundedCornerShape(12dp)`)
   - 48dp circular `UserAvatar` with 14dp live presence dot
   - Center column:
     - Partner Display Name (15sp, bold if unread, semi-bold if read, `#F2F3F5`)
     - Message Preview snippet (13sp, `#DBDEE1` if unread, `#949BA4` if read, includes `"You: "` self prefix or `"📷 Attachment"`)
   - Right column:
     - Relative timestamp (12sp, bold `#F2F3F5` if unread, `#949BA4` if read)
     - Unread dot indicator (10dp circle, Discord red `#ED4245`)
   - Empty state view: Centered chat icon (48dp, `#949BA4`) + `"No direct messages yet"` / `"No conversations match your search"`
6. **Long-Press DM Context Menu** (`ModalBottomSheet`):
   - Surface background `#1E1F22`, options to close DM, mark read/unread, mute, view profile
7. **Floating User Capsule** (anchored at bottom of drawer):
   - 50dp height pill card, `#17181B` surface, `#2B2D31` border, 34dp avatar + status dot, display name + status, notification bell

## Mapping Table
| Discord Element | Status | Stoat File/Composable | Notes |
|---|---|---|---|
| Top Bar Drawer Hamburger Icon | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/OverviewScreen.kt` (`OverviewScreen`) | Line 178: `IconButton` toggling navigation drawer when `useDrawer` is true |
| Title `"Messages"` | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/OverviewScreen.kt` (`OverviewScreen`) | Line 191: 24sp Bold `#F2F3F5` header |
| Action Row Search Toggle | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/OverviewScreen.kt` (`OverviewScreen`) | Lines 207–221: 38dp squircle, toggles `isSearchExpanded`, turns `#5865F2` |
| Action Row Message Requests Button | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/OverviewScreen.kt` (`OverviewScreen`) | Lines 224–239: 38dp squircle, navigates to `ChatRouterDestination.Friends` |
| Action Row "Add Friends" Pill | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/OverviewScreen.kt` (`OverviewScreen`) | Lines 241–263: 38dp height squircle pill, navigates to Friends |
| Action Row New DM "+" Button | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/OverviewScreen.kt` (`OverviewScreen`) | Lines 268–282: 38dp squircle in `#5865F2`, navigates to Friends |
| Expandable Real-Time Search Bar | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/OverviewScreen.kt` (`OverviewScreen`) | Lines 286–342: Animated `OutlinedTextField` filtering `filteredDMs` in real time |
| "Active Now" Carousel | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/OverviewScreen.kt` (`ActiveNowFriendCard`) | Lines 353–382, 460–503: `LazyRow` of 86dp squircle story cards |
| Direct Message Item Row | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/OverviewScreen.kt` (`DirectMessageItemRow`) | Lines 506–626: 72dp row with circular avatar, presence dot, preview, timestamp, unread dot |
| User Circular Avatar & Presence | MAPPED | `app/src/main/java/chat/stoat/composables/generic/UserAvatar.kt` (`UserAvatar`) | 48dp circular avatar with 14dp live presence ring matching status |
| Unread Notification Dot | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/OverviewScreen.kt` (`DirectMessageItemRow`) | Lines 614–621: 10dp red circle `#ED4245` |
| Relative Timestamp Formatter | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/OverviewScreen.kt` (`formatRelativeTime`) | Lines 89–112: Formats ULID timestamp to `now`, `m`, `h`, `Yesterday`, `d`, date |
| Empty DM Hub State | MAPPED | `app/src/main/java/chat/stoat/screens/chat/views/OverviewScreen.kt` (`OverviewScreen`) | Lines 391–413: Icon `ic_chat_24dp` + muted state label |
| DM Long-Press Context Sheet | MAPPED | `app/src/main/java/chat/stoat/sheets/ChannelContextSheet.kt` (`ChannelContextSheet`) | Lines 441–456: `ModalBottomSheet` displaying DM management actions |
| Floating User Capsule | MAPPED | `app/src/main/java/chat/stoat/composables/navigation/StoatUserCapsule.kt` (`StoatUserCapsule`) | Pinned to drawer bottom with 34dp avatar, status dot, display name, bell, settings |
| Nitro Banner Upsells | DISCORD-ONLY (MONETIZATION) | None | **HARD PURGE**: Delete all promotional banners, close gap |
| Avatar Decorations & Profile Effects | DISCORD-ONLY (MONETIZATION) | None | **HARD PURGE**: Stripped; keep clean bespoke circular avatars |
| Nitro Gift Shortcuts in DM Hub | DISCORD-ONLY (MONETIZATION) | None | **HARD PURGE**: Completely absent; no dead buttons |
| "Shop" & "Quests" Tabs / Promos | DISCORD-ONLY (MONETIZATION) | None | **HARD PURGE**: Excised from navigation hierarchy |

## Changes Required (Restyle Only)
1. **Background & Scaffold Palette**:
   - Ensure the root container and Scaffold use `DiscordDarkBg = Color(0xFF1E1F22)` (`var(--bg-tertiary)`) in drawer mode, or `Color(0xFF313338)` (`var(--bg-primary)`) when presented full-screen.
   - Surface cards (`DirectMessageItemRow`, `ActiveNowFriendCard`, Search bar) strictly use `DiscordCardBg = Color(0xFF2B2D31)` (`var(--bg-secondary)`).
2. **Typography & Hierarchy**:
   - Title: `24.sp`, `FontWeight.Bold`, color `DiscordHeader = Color(0xFFF2F3F5)`.
   - Contact names: `15.sp`, `FontWeight.SemiBold` (read) / `FontWeight.Bold` (unread), color `#F2F3F5`.
   - Message previews: `13.sp`, `FontWeight.Normal` (read) / `FontWeight.SemiBold` (unread), color `DiscordTextMuted = Color(0xFF949BA4)` (read) / `DiscordTextNormal = Color(0xFFDBDEE1)` (unread).
   - Relative timestamp: `12.sp`, color `#949BA4` (read) / `#F2F3F5` (unread).
3. **Corner Radii & Shapes**:
   - Follow the 2026 shape rule: people = **circles**, UI elements = **squircles**.
   - `ActionButtonShape`: `RoundedCornerShape(12.dp)` for action buttons and DM item click surfaces.
   - `FriendCardShape`: `RoundedCornerShape(20.dp)` for story cards.
   - Search bar: `RoundedCornerShape(10.dp)`.
   - Avatar: Always strict `CircleShape`.
4. **Spacing & Paddings**:
   - Top action row: horizontal padding `16.dp`, vertical spacing `10.dp`, button gap `8.dp`.
   - DM rows: horizontal padding `14.dp`, vertical padding `6.dp`, height `72.dp`.
   - Bottom clearance: `32.dp` bottom spacer to account for system gesture bar and user capsule floating elevation.

## Slots to Delete (Monetization)
1. **Nitro Promotion Cards & Banners**: Any upsell cards promoting Discord Nitro, Super Reactions, custom app icons, or subscription discounts must have their slots deleted. No empty placeholder boxes.
2. **Avatar Decorations & Profile Frame Shaders**: Modern Discord overlays expensive WebGL profile decoration frames over DM avatars. Purged: keep pure native circular avatars with zero overhead.
3. **Nitro Gift Shortcut in DMs**: No gift buttons, no gift coin icons in the DM hub or message rows.
4. **Shop / Quests Entries**: Any gamified orb, quest, or avatar shop entry points are strictly forbidden and excised.
