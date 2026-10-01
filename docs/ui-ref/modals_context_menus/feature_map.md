# Feature Map: Modals & Context Menus

## Discord Layout (top → bottom, left → right)

### 1. Message Long-Press Context Bottom Sheet
- Drag handle (36dp pill `#4E5058`)
- **Quick-Reactions Horizontal Row**:
  - `✅` Checkmark pill
  - `💀` Skull pill
  - `❤️` Heart pill
  - `🔥` Fire pill
  - `😂` Laughing pill
  - `➕` Add Reaction squircle button (opens full reaction picker)
- **Message Quote Snippet**: Author avatar + Author name + ellipsized message text
- **Action Rows**:
  - Reply (`ic_reply_24dp`)
  - Edit Message (`ic_edit_24dp`, author only)
  - Copy Text (`ic_content_copy_24dp`)
  - Copy Message Link (`ic_link_24dp`)
  - Forward (`ic_forward_24dp`)
  - Pin Message / Unpin Message (`ic_pin_24dp`)
  - Mark Unread (`ic_mark_chat_unread_24dp`)
  - Copy ID (`ic_identifier_copy_24dp`, developer mode)
  - Report Message (`ic_report_24dp`, danger red, non-author)
  - Delete Message (`ic_delete_24dp`, danger red, author/moderator)

### 2. In-Server User Profile Card & Member Sheet
- **Header Banner**: 135dp height full-width user banner / gradient (`#5865F2` → `#232428`)
- **Overlapping Avatar**: 96dp circular avatar with 6dp canvas cut-out border and 26dp live status badge
- **Badge Capsule**: Developer, Supporter, Moderator badges on `#1E1F22` pill
- **Identity Block**: Display Name (24sp bold `#F2F3F5`), Handle/Pronouns (`@user • pronouns`), Custom Status bubble (`#232428`)
- **Primary Actions**:
  - Self: "Edit Profile" (Blurple `#5865F2` button)
  - Other: Message icon button (`#35373C`) + "Add Friend" / "Friends" pill + `⋮` overflow
- **Stacked Elevated Cards** (`#232428`, `RoundedCornerShape(8dp)`):
  - Card 1: ABOUT ME (Bio with markdown rendering)
  - Card 2: MEMBER SINCE (Server join date + Stoat registration date)
  - Card 3: ROLES — {count} (`DiscordRolePill` items with circular role color dot + role name)
  - Card 4: MODERATOR ACTIONS (Manage Nickname/Roles, Timeout, Kick, Ban)

### 3. Server Context Bottom Sheet
- Drag handle
- **Server Overview Card Header**: 48dp squircle server icon + Server Name + Member count
- **Primary Actions**:
  - Invite People (Special highlighted button)
  - Notification Settings
  - Create Channel (Text vs Voice toggle dialog)
- **Utility Actions**:
  - Mark as Read
  - Copy Server ID
  - Add to Folder / Remove from Folder
  - Server Identity (Edit Nickname)
  - Server Settings (Gear icon)
- **Danger Zone**:
  - Non-Owner: Report Server + Leave Server (with "Leave Silently" checkbox)
  - Owner: Delete Server (danger red, irreversible)

---

## Mapping Table

| Discord Element | Status | Stoat File/Composable | Notes |
|---|---|---|---|
| **Message Sheet: Drag Handle** | MAPPED | `chat.stoat.sheets.MessageContextSheet.kt` | Top center indicator |
| **Message Sheet: Quick Reactions Bar** | MAPPED | `chat.stoat.sheets.MessageContextSheet.kt` & `ReactSheet.kt` | Top horizontal emoji pill row (`✅`, `💀`, `❤️`, `🔥`, `😂`, `+`) |
| **Message Sheet: Message Snippet** | MAPPED | `chat.stoat.sheets.MessageContextSheet.kt` (`Message(previewMessage)`) | Mini preview of selected message |
| **Message Sheet: Reply** | MAPPED | `chat.stoat.sheets.MessageContextSheet.kt` (`UiCallbacks.replyToMessage`) | Dispatches reply callback to chat composer spine |
| **Message Sheet: Edit Message** | MAPPED | `chat.stoat.sheets.MessageContextSheet.kt` (`UiCallbacks.editMessage`) | Author-only edit trigger |
| **Message Sheet: Copy Text** | MAPPED | `chat.stoat.sheets.MessageContextSheet.kt` (`clipboardManager.setText`) | Copies message content |
| **Message Sheet: Copy Link** | MAPPED | `chat.stoat.sheets.MessageContextSheet.kt` | Formats URL with server/channel/message IDs |
| **Message Sheet: Share** | MAPPED | `chat.stoat.sheets.MessageContextSheet.kt` (`showShareSheet`) | Modal share sheet |
| **Message Sheet: Delete Message** | MAPPED | `chat.stoat.sheets.MessageContextSheet.kt` (`showDeleteMessageConfirmation`) | Destructive confirmation alert dialog |
| **Message Sheet: Report Message** | MAPPED | `chat.stoat.sheets.MessageContextSheet.kt` (`onReportMessage`) | Report flag action |
| **Nitro Gifting from Profile** | DISCORD-ONLY (MONETIZATION) | None | **HARD BAN**: Delete slot, close vertical gap completely |
| **Avatar Decorations / Profile Effects** | DISCORD-ONLY (MONETIZATION) | None | **HARD BAN**: Delete slot, close vertical gap completely |
| **Nitro Badges / Nitro Boost Badges** | DISCORD-ONLY (MONETIZATION) | None | **HARD BAN**: Delete slot, close vertical gap completely |
| **User Profile: Banner & Overlapping Avatar** | MAPPED | `chat.stoat.sheets.UserInfoSheet.kt` | 135dp banner, 96dp avatar with 6dp cut-out border |
| **User Profile: Badge Capsule** | MAPPED | `chat.stoat.sheets.UserInfoSheet.kt` (`DiscordBadgeCapsule`) | Authentic badge pill on `#1E1F22` inset |
| **User Profile: Custom Status Bubble** | MAPPED | `chat.stoat.sheets.UserInfoSheet.kt` | Squircle pill card `#232428` |
| **User Profile: Action Row** | MAPPED | `chat.stoat.composables.screens.settings.UserButtons.kt` | Message icon button + Friend / Relationship button + `⋮` |
| **User Profile: About Me Card** | MAPPED | `chat.stoat.sheets.UserInfoSheet.kt` (`ChatMarkdown`) | Card 1 with markdown bio |
| **User Profile: Member Since Card** | MAPPED | `chat.stoat.sheets.UserInfoSheet.kt` | Card 2 with server join & Stoat creation dates |
| **User Profile: Roles Card & Pills** | MAPPED | `chat.stoat.sheets.UserInfoSheet.kt` (`DiscordRolePill`) | Card 3 with rank-sorted role pills (color dot + text) |
| **User Profile: Moderation Actions** | MAPPED | `chat.stoat.composables.screens.settings.UserButtons.kt` & `chat.stoat.dialogs.MemberModerationDialog` | Manage Nickname, Timeout, Kick, Ban |
| **Server Sheet: Overview Header** | MAPPED | `chat.stoat.sheets.ServerContextSheet.kt` (`ServerOverview`) | 48dp squircle icon, title, description |
| **Server Sheet: Invite People** | MAPPED | `chat.stoat.sheets.ServerContextSheet.kt` (`SheetButton(special = true)`) | Highlighted special action |
| **Server Sheet: Notification Settings** | MAPPED | `chat.stoat.sheets.ServerContextSheet.kt` (`SheetButton`) | Opens notifications configuration |
| **Server Sheet: Create Channel** | MAPPED | `chat.stoat.sheets.ServerContextSheet.kt` (`showCreateChannelDialog`) | Dialog with Text vs Voice toggle |
| **Server Sheet: Mark As Read** | MAPPED | `chat.stoat.sheets.ServerContextSheet.kt` (`StoatAPI.unreads.markServerAsRead`) | Unread state clearing |
| **Server Sheet: Copy Server ID** | MAPPED | `chat.stoat.sheets.ServerContextSheet.kt` | Copies ID to clipboard |
| **Server Sheet: Add / Remove Folder** | MAPPED | `chat.stoat.sheets.ServerContextSheet.kt` (`ServerFolders`) | Server folder organization |
| **Server Sheet: Server Identity** | MAPPED | `chat.stoat.sheets.ServerContextSheet.kt` | Navigates to `settings/server/$serverId/identity` |
| **Server Sheet: Server Settings** | MAPPED | `chat.stoat.sheets.ServerContextSheet.kt` | Navigates to `settings/server/$serverId` |
| **Server Sheet: Leave Server (Silently)** | MAPPED | `chat.stoat.sheets.ServerContextSheet.kt` (`showLeaveConfirmation`) | "Leave Silently" checkbox dialog |
| **Server Sheet: Delete Server** | MAPPED | `chat.stoat.sheets.ServerContextSheet.kt` (`leaveOrDeleteServer`) | Danger red styling |

---

## Changes Required (Restyle Only)

1. **Color Token Alignment**:
   - Sheet canvas: Match `DiscordDarkCanvas` (`#111214`) for profile popout, `#1E1F22` for message and server sheets.
   - Inner cards: `#232428` (`DiscordCardSurface`).
   - Inset pills / chips: `#1E1F22` (`DiscordInsetSurface`).
   - Dividers: `#2B2D31` or `#35373C`.
   - Text header: `#F2F3F5`.
   - Text muted: `#949BA4`.
   - Brand accent: `#5865F2` (Blurple).
2. **Message Long-Press Polish**:
   - Ensure horizontal quick-reactions bar is pinned directly above the message preview snippet in `MessageContextSheet`.
   - Reaction pill shape: 44dp × 44dp squircles (`RoundedCornerShape(12dp)`) with 1dp border `#35373C`.
3. **User Profile Popout Polish**:
   - Already adheres closely to Discord desktop/mobile 1:1 tokens in `UserInfoSheet.kt`.
   - Ensure moderator actions (Timeout, Kick, Ban) display as clean 2×2 quick-action tiles inside an elevated card rather than buried exclusively in the `⋮` overflow menu.
4. **Server Context Sheet Polish**:
   - Maintain `ServerOverview` card header with 48dp squircle icon.
   - Keep "Invite People" visually highlighted with special container tint `#5865F2`.

---

## Slots to Delete (Monetization)

- ❌ **"Gift Nitro" button on User Profile**: Purged. Never display a gift icon or Nitro gift button on another user's profile.
- ❌ **Avatar Decorations / Profile Effects**: Purged. No cosmetic particle effects or decorative avatar rings.
- ❌ **Profile Nameplate Shop**: Purged.
- ❌ **Server Boost Banner in Server Sheet**: Purged. No "Boost this server" upsell row.
- **Vertical Spacing Rule**: In `UserInfoSheet`, the stacked cards ("ABOUT ME", "MEMBER SINCE", "ROLES") sit adjacent to each other with 8dp spacing; zero gap or placeholder for Nitro subscriptions.
