# Feature Map: Server Sidebar & Channels

## Discord Layout (top → bottom, left → right)

### 1. Server Rail (Left Column — 64dp/68dp)
1. **System Status Bar Spacer** — Top window inset padding (`WindowInsets.statusBars`).
2. **Self Avatar (Sticky Rail Header)** — 48dp circular avatar with 16dp presence badge anchored bottom-right. Tap navigates to DM overview; long-press opens account switch / profile options.
3. **Unread Direct Message Avatars (Dynamic Pin Stack)** — Vertical list of 48dp circular avatars with presence dots for direct chats/groups containing unread messages.
4. **Rail Horizontal Divider** — Fixed 1dp divider (`#35373C`) separating DM section from server list.
5. **Server List (`LazyColumn`)**:
   - **Active Server Pill Indicator** — Left-anchored white bar (`#FFFFFF`, 8dp width, -4dp offset, spring animated: 0dp default, 8dp unread, 36dp active selection).
   - **Single Server Icon** — 48dp squircle with animated corner radius (24dp circle transitioning to 16dp rounded squircle on active selection).
   - **Voice / Screen Share Indicator Cutout** — 16dp circular bottom-end cutout with 12dp speaker icon (`ic_volume_up_24dp`) or screenshare icon (`ic_screen_share_24dp`) if active voice participants exist.
   - **Folder Header Icon** — 48dp squircle displaying a 2×2 thumbnail grid of member server icons when collapsed; open-folder icon (`ic_folder_open_24dp`) when expanded; tinted background.
   - **Folder Track / Background** — Indented continuous rounded-rect tinted background track enclosing expanded folder members drawn via canvas `drawRoundRect`.
   - **Server Drag & Drop Target** — Ghost drag overlay, spring animations, reordering before/after siblings, and drop-on-server to create a new folder.
6. **Add Server Action Button** — 48dp circular button with `+` icon (`ic_add_24dp`), background `#2B2D31`.
7. **Discover Servers Action Button** — 48dp circular button with compass/explore icon (`ic_explore_24dp`), background `#2B2D31`.
8. **Settings Action Button** — 48dp circular button with gear icon (`ic_settings_24dp`), conditional developer/admin trigger.
9. **System Navigation Bar Spacer** — Bottom window inset padding (`WindowInsets.navigationBars`).

---

### 2. Channel Drawer (Right Panel of Left Swipe — ~242dp)

#### A. Header Area (Fixed Top)
10. **Server Banner / Fallback Block** — 192dp animated height when scrolled to top, collapsing to 128dp on scroll; fallback to 76dp solid block when no banner image exists. Top status bar inset accounted for.
11. **Banner Gradient Overlay** — Top-to-bottom dark gradient scrim (60% black to transparent) ensuring text legibility.
12. **Server Flags / Badges** — Official badge (`ic_workspace_premium_24dp__fill`, 24dp) and Verified badge (`ic_verified_24dp__fill`, 24dp) preceding the server title.
13. **Server Title / Name** — 16sp `titleMedium`, bold, single-line with ellipsis truncation. In DM mode, displays "Direct Messages".
14. **Server Context Menu Button (⋮)** — 3-dots overflow icon button on trailing edge of header; triggers `ServerContextSheet`.

#### B. Direct Messages Mode Body (`currentServer == null`)
15. **"Direct Messages" Overview Row** — 40dp row, `ic_chat_24dp` icon, 14sp medium label, highlighted container (`#35373C`) when active.
16. **Friends Navigation Row** — 40dp row, `ic_group_24dp` icon, "Friends" label, badge indicator for incoming friend requests.
17. **Saved Messages / Notes Row** — 40dp row, notebook icon, "Notes" label (Stoat feature).
18. **Channel Section Divider** — 1dp horizontal divider (`#35373C`).
19. **Chronological DM / Group List (`LazyColumn`)**:
    - **Row Item** — 40dp compact height, 4dp rounded corners, `combinedClickable` (tap navigates, long-press opens channel context sheet).
    - **Avatar** — 36dp circular avatar with 12dp live status dot (for DMs) or 36dp `GroupIcon` (for group chats).
    - **Display Name** — 15sp SemiBold, `#F2F3F5` (active) or `#949BA4` (inactive).
    - **Last Message Snippet** — 12sp muted `#949BA4`, single line, ellipsis truncation.
    - **Unread Indicator** — 8dp red circle (`#ED4245`) trailing badge.

#### C. Server Channels Mode Body (`currentServer != null`)
20. **Collapsible Category Header** — Uppercase label (11sp bold, letter spacing 0.6sp, `#949BA4`), with rotating chevron icon (`▶` 0° collapsed, 90° expanded), click to toggle category visibility.
21. **Channel Item Row**:
    - **Container Shape** — Rounded pill / CircleShape clip with 4dp vertical spacing.
    - **Channel Type Icon** — 16dp icon: text channel (`#`), voice speaker, announcement megaphone, forum board, or geogate age-restricted lock.
    - **Channel Name** — `bodyMedium` (14sp), single-line ellipsis truncation.
    - **Selection State** — `secondaryContainer` (`#35373C`) when active.
    - **Muted State** — 50% opacity (`Modifier.alpha(0.5f)`).
    - **Unread Indicator** — 8dp brand circle (`#5865F2` / primary) trailing dot, visible when unread and not current.
    - **Voice Capacity Counter** — Trailing monospace counter (`FragmentMono`, e.g. `2/10`) for voice channels with member limits.
22. **Voice Channel Participant Preview (Inline Tree)**:
    - **Inline Container** — Indented column under active voice channel (56dp start padding).
    - **Participant Row** — 20dp circular avatar, 12sp display name, screen share icon (`ic_screen_share_24dp`, 16dp) if active.
    - **Overflow Indicator** — Shows first 5 participants; displays "+ N more" text for remainder.
23. **Navigation Bar Spacer** — Bottom window inset padding.

#### D. Floating Bottom User Capsule
24. **Capsule Outer Container** — 50dp height, `RoundedCornerShape(28dp)`, background `#1E1F22` (`StoatCapsuleBg`), 1dp border `#2B2D31`, 12dp elevation shadow, anchored `bottom: 10dp, left: 8dp, right: 8dp`.
25. **User Avatar & Live Presence Badge** — 38dp circular avatar with 14dp status dot having a 2dp border matching capsule surface:
    - Online: `#23A55A`
    - Idle: `#F0B232`
    - Do Not Disturb: `#F23F43`
    - Offline / Invisible: `#80848E`
26. **User Identity Column** — 14sp bold username (`#F2F3F5`) with `∨` caret dropdown indicator; 12sp muted presence / custom status text (`#949BA4`).
27. **Quick Notification Bell Button** — 32dp circular pill, background `#2B2D31`, 18dp bell icon; opens in-app mentions/notifications overview.
28. **Quick Settings Gear Button** — 32dp circular pill, background `#2B2D31`, 18dp gear icon; opens Settings navigation.

---

## Mapping Table

| Discord Element | Status | Stoat File/Composable | Notes |
|---|---|---|---|
| **System Status Bar Spacer** | MAPPED | `ChannelSideDrawer.kt:393-401` | Padded via `WindowInsets.statusBars`. |
| **Self Avatar Sticky Rail Header** | MAPPED | `ChannelSideDrawer.kt:403-425` | `UserAvatar` (48dp, 16dp presence). Navigates to DM overview on click. |
| **Unread DM Rail Avatars** | MAPPED | `ChannelSideDrawer.kt:428-486` | Dynamic items from `DirectMessages.unreadDMs()`, 48dp avatars with presence dots. |
| **Rail Horizontal Divider** | MAPPED | `ChannelSideDrawer.kt:488-495` | `HorizontalDivider` with 8dp horizontal padding. |
| **Active Server Pill Indicator** | MAPPED | `ServerRail.kt:77-136` (`RailIndicatorBox`) | Animated width 8dp, height 0dp/8dp/36dp with spring physics. |
| **Server Icon (Squircle / Selection)** | MAPPED | `ServerRail.kt:202-227` (`ServerIconImage`), `ServerRail.kt:229-305` (`ServerRailIcon`) | 48dp squircle, spring-animated corner radius 24dp → 16dp. |
| **Voice / Screen Share Badge on Server Icon** | MAPPED | `ServerRail.kt:247-303` | 16dp bottom-end circle cutout with `ic_volume_up_24dp` or `ic_screen_share_24dp`. |
| **Folder Header (Collapsed 2×2 / Expanded)** | MAPPED | `ServerRail.kt:307-343` (`FolderRailHeader`), `ServerRail.kt:345-409` (`FolderIcon`, `FolderPreview`) | 48dp squircle, 2×2 thumbnail grid or open folder icon. |
| **Folder Members Background Track** | MAPPED | `ServerRail.kt:162-200` (`folderGroupBackgrounds`) | Continuous rounded-rect background track drawn via `drawBehind`. |
| **Server Rail Drag & Drop Reorder / Fold** | MAPPED | `ChannelSideDrawer.kt:355-381`, `ServerRailDrag.kt` | `railDragGestures` with `RailIntent.Fold` and `RailIntent.Move`. |
| **Add Server Button** | MAPPED | `ChannelSideDrawer.kt:537-554` | 48dp circular button triggering `onShowAddServerSheet`. |
| **Discover Servers Button** | MAPPED | `ChannelSideDrawer.kt:556-573` | 48dp circular button navigating to `topNav.navigate("discover")`. |
| **Settings Gear Button (Rail)** | STOAT-ONLY | `ChannelSideDrawer.kt:575-597` | Conditional admin/debug trigger kept in Stoat rail. |
| **Server Banner & Collapsing Header** | MAPPED | `ChannelSideDrawer.kt:614-655` | 192dp/128dp/76dp animated banner height with linear gradient scrim. |
| **Official & Verified Server Flags** | MAPPED | `ChannelSideDrawer.kt:672-697` | 24dp icons (`ic_workspace_premium_24dp__fill`, `ic_verified_24dp__fill`). |
| **Server Name / Title** | MAPPED | `ChannelSideDrawer.kt:699-707` | `titleMedium` single line with ellipsis truncation. |
| **Server Context Menu Button (⋮)** | MAPPED | `ChannelSideDrawer.kt:710-720` | `IconButton` triggering `onShowServerContextSheet`. |
| **Direct Messages Overview Row** | MAPPED | `ChannelSideDrawer.kt:780-812` | 40dp row, `ic_chat_24dp`, highlighted `#35373C` when active. |
| **Friends Navigation Row** | MAPPED | `ChannelSideDrawer.kt:814-833` | Navigates to `ChatRouterDestination.Friends` with incoming friend request dot. |
| **Saved Messages / Notes Channel** | STOAT-ONLY | `ChannelSideDrawer.kt:835-859` | Dedicated notes storage channel in Stoat; kept as-is. |
| **Chronological DM / Group Rows** | MAPPED | `ChannelSideDrawer.kt:870-912`, `ChannelSideDrawer.kt:1315-1410` (`DMOrGroupItem`) | 40dp row, 36dp avatar, 15sp name, 12sp snippet, 8dp red unread dot. |
| **Collapsible Category Headers** | MAPPED | `ChannelSideDrawer.kt:1011-1014`, `ChannelSideDrawer.kt:1275-1311` (`CategoryItem`) | Uppercase 11sp bold, `#949BA4`, rotating chevron (0° → 90°). |
| **Channel Item Row** | MAPPED | `ChannelSideDrawer.kt:980-1009`, `ChannelSideDrawer.kt:1034-1166` (`ChannelItem`) | Text/voice/announcement icon, 14sp name, unread dot 8dp `#5865F2`, capacity counter. |
| **Geogate Age-Restricted Channel Icon** | STOAT-ONLY | `ChannelSideDrawer.kt:1100-1106` | Special lock icon `ic_grid_3x3_off_24dp` for restricted geo; kept as-is. |
| **Voice Participant Inline Preview Tree** | MAPPED | `ChannelSideDrawer.kt:1155-1164`, `ChannelSideDrawer.kt:1170-1232` (`VoiceChannelParticipantPreview`) | Up to 5 participant rows (20dp avatar + name + screen share icon) + "+ N more". |
| **Voice Participant Row** | MAPPED | `ChannelSideDrawer.kt:1235-1272` (`VoiceChannelParticipantRow`) | 20dp `UserAvatar`, 12sp display name, screen share icon. |
| **Floating User Capsule Container** | MAPPED | `ChannelSideDrawer.kt:748-753`, `StoatUserCapsule.kt:55-194` (`StoatUserCapsule`) | 50dp pill card, `#1E1F22`, 1dp border `#2B2D31`, 12dp elevation shadow. |
| **Capsule Avatar & Live Presence Ring** | MAPPED | `StoatUserCapsule.kt:96-117` | 38dp avatar with 14dp status dot, 2dp border matching capsule bg. |
| **Capsule Username & Presence Text** | MAPPED | `StoatUserCapsule.kt:121-154` | 14sp bold text + `∨` caret, 12sp muted presence text. |
| **Capsule Quick Bell (Mentions/Overview)** | MAPPED | `StoatUserCapsule.kt:158-174` | 32dp circular pill, background `#2B2D31`, bell icon. |
| **Capsule Quick Settings Gear** | MAPPED | `StoatUserCapsule.kt:176-192` | 32dp circular pill, background `#2B2D31`, gear icon. |
| **Server Boost Progress Bar & Level Tier Pill** | DISCORD-ONLY (MONETIZATION) | None (BANNED) | **DELETE SLOT / CLOSE GAP**. Discord places a Boost Level bar and crystal badge above channel list. Stoat eliminates this entirely. |
| **Nitro Gift Button in Channel Header** | DISCORD-ONLY (MONETIZATION) | None (BANNED) | **DELETE SLOT / CLOSE GAP**. Zero paywall upsells or gift icons. |
| **Server Store / Subscription Channels** | DISCORD-ONLY (MONETIZATION) | None (BANNED) | **DELETE SLOT / CLOSE GAP**. No monetization or commerce channels. |
| **Quest Banners / Gamification Promos** | DISCORD-ONLY (MONETIZATION) | None (BANNED) | **DELETE SLOT / CLOSE GAP**. Zero synthetic promo widgets. |

---

## Changes Required (Restyle Only)

1. **Drawer Container Background**:
   - `ChannelSideDrawer.kt:610`: Currently uses `Modifier.background(MaterialTheme.colorScheme.surfaceContainer)`.
   - Restyle to explicit dark secondary container `#2B2D31` (`bg_secondary`) to ensure uniform Discord dark-charcoal elevation regardless of Material3 dynamic scheme.
2. **Server Rail Styling**:
   - Rail background is `#1E1F22` (`bg_tertiary`) (`ChannelSideDrawer.kt:341`, `ServerRail.kt`). Keep aligned with Discord's dark palette.
   - Rail width: 64dp (`ChannelSideDrawer.kt:339`). Ensure adequate hit targets and smooth 68dp outer spacing per spec.
   - Unselected server icon corner radius: 24dp circular default transitioning to 16dp squircle (`ServerRail.kt:240`). Matches Discord 2026 squircle spec.
   - Indicator pill: 8dp width, offset (-4)dp, animated height 0dp/8dp/36dp (`ServerRail.kt:127-134`). Matches Discord pill behavior.
3. **Category Header Typography**:
   - `CategoryItem`: 11sp bold, uppercase, letter spacing 0.6sp, color `#949BA4` (`ChannelSideDrawer.kt:1301-1310`). Matches modern Discord specs.
4. **Channel Item Selection & Unread Polish**:
   - Selected channel background: `MaterialTheme.colorScheme.secondaryContainer` (`#35373C`).
   - Unread indicator: 8dp circle, brand blurple `#5865F2` (`ChannelSideDrawer.kt:1133-1139`).
   - Monospace capacity badge: `FragmentMono` font with `participantCount/maxUsers` format (`ChannelSideDrawer.kt:1145-1152`).
5. **Floating Bottom User Capsule**:
   - Docked above navigation bar with `WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 6.dp` padding (`ChannelSideDrawer.kt:752`).
   - Elevation shadow: 12dp, surface `#1E1F22`, border 1dp `#2B2D31`, pill shape 28dp (`StoatUserCapsule.kt:52, 88-92`).

---

## Slots to Delete (Monetization)

1. **Server Boost Progress Bar & Tier Level Badges**:
   - In modern Discord, a persistent booster bar (`Level 1 / 2 / 3` with diamond icon and "Boost Server" CTA) is slotted between the server header and the first category.
   - **Stoat Rule**: Slot is deleted permanently. No placeholder, no stub button, no gap. The channel list directly begins immediately beneath the server header.
2. **Nitro Gifting & Subscription Upsells**:
   - Discord embeds Nitro gift buttons in channel headers and context sheets.
   - **Stoat Rule**: Permanently banned. No gift icons, no trial promotions.
3. **Server Store Channels**:
   - Discord supports monetized "Store" channel types for selling server digital items.
   - **Stoat Rule**: Ignored and purged. Stoat only supports communication channels (Text, Voice, Announcements, Forums).
4. **Promotional Quest / Gamification Widgets**:
   - Discord mobile frequently renders promotional banners for streaming quests or synthetic rewards.
   - **Stoat Rule**: Purged. Zero gamification clutter.
