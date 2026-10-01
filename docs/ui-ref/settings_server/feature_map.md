# Feature Map: Server Settings

## Discord Layout (top → bottom, left → right)

1. **Top App Bar**:
   - `←` Back navigation icon button
   - Title: "Server Settings"
2. **Server Identity Header Card**:
   - 64dp Squircle Server Icon (`RoundedCornerShape(18dp)`)
   - Server Name (18sp bold `#F2F3F5`)
   - Server Stats / Subtitle: "Created on [date] • [N] Members"
3. **Category: OVERVIEW & STRUCTURE**:
   - Section header: "OVERVIEW & STRUCTURE" (11sp bold, uppercase `#949BA4`)
   - **Overview** row: Info icon + "Overview" + chevron `>`
     - [Subpage] Server Name, Server Icon upload, System Messages Channel selector
   - **Channels** row: Grid icon + "Channels" + chevron `>`
     - [Subpage] Category blocks with text/voice channel list
     - [Subpage] Channel drag-to-reorder
     - [Subpage] Create Channel action
4. **Category: CUSTOMISATION**:
   - Section header: "CUSTOMISATION" (11sp bold, uppercase `#949BA4`)
   - **Emoji** row: Mood icon + "Emoji" + chevron `>`
     - [Subpage] Custom emoji list, upload new emoji, delete emoji
5. **[DISCORD MONETIZATION SECTION]**:
   - Server Boost Status, Boost Level Tier Meters, Server Subscriptions, Store Channels
6. **Category: USER MANAGEMENT**:
   - Section header: "USER MANAGEMENT" (11sp bold, uppercase `#949BA4`)
   - **Roles** row: Flag/Shield icon + "Roles" + count pill + chevron `>`
     - [Subpage] Draggable role hierarchy list with role color dots
     - [Subpage] Role create FAB + dialog
     - [Subpage] Role permissions editor
   - **Members** row: People icon + "Members" + chevron `>`
     - [Subpage] Member directory with moderation actions (Nickname, Roles, Timeout, Kick, Ban)
   - **Instant Invites** row: Link icon + "Instant Invites" + chevron `>`
     - [Subpage] Active invite list, usage count, revoke buttons
   - **Bans** row: Gavel icon + "Bans" + chevron `>`
     - [Subpage] Banned users directory + revoke ban action
7. **Category: DANGER ZONE**:
   - **Delete Server** row: Trash icon + "Delete Server" (danger red `#F23F43`) + alert confirmation dialog

---

## Mapping Table

| Discord Element | Status | Stoat File/Composable | Notes |
|---|---|---|---|
| Top App Bar (`←` Back, "Server Settings") | MAPPED | `chat.stoat.settings.dsl.SettingsPage` in `ServerSettingsHome.kt` | Restyle header bar to Discord `#1E1F22` background |
| Server Identity Header Card | MAPPED | `chat.stoat.composables.screens.settings.ServerOverview` | Reuse `ServerOverview` composable at top of `ServerSettingsHome` |
| "Overview" Row | MAPPED | `chat.stoat.screens.settings.server.ServerSettingsHome.kt` (`ServerSettingsOption.Overview`) | Navigates to `settings/server/$serverId/overview` (`ServerSettingsOverview.kt`) |
| Overview: Server Name / Icon Upload | MAPPED | `chat.stoat.screens.settings.server.ServerSettingsOverview.kt` | Icon upload via Autumn, server name text field |
| "Channels" Row | MAPPED | `chat.stoat.screens.settings.server.ServerSettingsHome.kt` (`ServerSettingsOption.Channels`) | Navigates to `settings/server/$serverId/channels` (`ServerSettingsChannels.kt`) |
| Channels: Hierarchy & Categories | MAPPED | `chat.stoat.screens.settings.server.ServerSettingsChannels.kt` (`serverChannelSections`) | Organizes channels by category sections and uncategorized |
| Channels: Drag-to-Reorder | MAPPED | `chat.stoat.screens.settings.server.ServerSettingsChannels.kt` (`ReorderableItem`) | Uses `sh.calvin.reorderable` with haptic feedback |
| Channels: Create Channel Dialog | MAPPED | `chat.stoat.screens.settings.server.ServerSettingsChannels.kt` & `ServerContextSheet.kt` | Segmented type toggle (Text vs Voice) + name input |
| "Emoji" Row | MAPPED | `chat.stoat.screens.settings.server.ServerSettingsHome.kt` (`ServerSettingsOption.Emojis`) | Navigates to `settings/server/$serverId/emojis` (`ServerSettingsEmojis.kt`) |
| Emoji: Custom Emojis (Unlocked) | MAPPED | `chat.stoat.screens.settings.server.ServerSettingsEmojis.kt` | **Stoat Rule**: Emojis are 100% free across all servers, zero Nitro padlocks |
| Server Boost Status / Perks | DISCORD-ONLY (MONETIZATION) | None | **HARD BAN**: Delete slot, close vertical gap completely |
| Server Subscriptions / Shop | DISCORD-ONLY (MONETIZATION) | None | **HARD BAN**: Delete slot, close vertical gap completely |
| "Roles" Row | MAPPED | `chat.stoat.screens.settings.server.ServerSettingsHome.kt` (`ServerSettingsOption.Roles`) | Navigates to `settings/server/$serverId/roles` (`ServerSettingsRoles.kt`) |
| Roles: Drag-to-Reorder List | MAPPED | `chat.stoat.screens.settings.server.ServerSettingsRoles.kt` (`RoleListRow`, `ReorderableItem`) | Respects role hierarchy ranks (`canManageServerRole`, `resolveOwnTopRoleRank`) |
| Roles: Role Color Indicator | MAPPED | `chat.stoat.composables.server.RoleColourIndicator` | 12dp circular dot displaying role color |
| Roles: Create Role Action | MAPPED | `chat.stoat.screens.settings.server.ServerSettingsRoles.kt` (`FloatingActionButton`, `showCreateDialog`) | FAB opens Create Role dialog |
| Roles: Role Permissions Editor | MAPPED | `chat.stoat.screens.settings.server.ServerSettingsRoleEditor.kt` | Fine-grained permission checkboxes |
| "Members" Row | MAPPED | `chat.stoat.screens.settings.server.ServerSettingsHome.kt` (`ServerSettingsOption.Members`) | Protected by `MEMBERS_PANEL_ACCESSIBLE` flag |
| "Instant Invites" Row | MAPPED | `chat.stoat.screens.settings.server.ServerSettingsHome.kt` (`ServerSettingsOption.Invites`) | Navigates to `settings/server/$serverId/invites` (`ServerSettingsInvites.kt`) |
| "Bans" Row | MAPPED | `chat.stoat.screens.settings.server.ServerSettingsHome.kt` (`ServerSettingsOption.Bans`) | Navigates to `settings/server/$serverId/bans` (`ServerSettingsBans.kt`) |
| "Delete Server" Row | MAPPED | `chat.stoat.screens.settings.server.ServerSettingsHome.kt` (`ServerSettingsOption.DeleteServer`) | Danger red styling with confirmation alert dialog (`leaveOrDeleteServer`) |

---

## Changes Required (Restyle Only)

1. **Color Token Alignment**:
   - Screen background: `#313338` (Dark) / `#000000` (AMOLED).
   - Group card container: `#2B2D31` (`bg_secondary`) with 1dp border `#35373C`.
   - Section headers: 11sp bold uppercase in `#949BA4`.
   - Row text: 15sp `#DBDEE1`.
   - Danger actions: `#F23F43` (Red).
2. **Card Stack Layout**:
   - Wrap options in `ServerSettingsSection` into cohesive squircle cards (`RoundedCornerShape(12dp)`) with 16dp horizontal padding.
   - Remove individual item rounded corners in favor of unified grouped card styling with 1dp inner dividers `#35373C`.
3. **Role Configuration Polish**:
   - Role rows: Left drag handle (24dp `#949BA4`), role color circle (12dp), role name (15sp bold `#DBDEE1`), lock icon for higher hierarchy roles, chevron `>` on right.
   - Role count eyebrow: "ROLES — {count}" in 11sp bold `#949BA4`.
4. **Channel Settings Polish**:
   - Channel items: `#` text channel icon or `🔊` voice channel icon in `#949BA4`, channel name in `#DBDEE1`, drag handle on right for reordering.

---

## Slots to Delete (Monetization)

- ❌ **Server Boost Status & Boost Level Progress**: Purged. Zero tier progress bars (Tier 1/2/3), zero boost counts.
- ❌ **Server Subscriptions / Paywalled Roles**: Purged.
- ❌ **Monetized Server Emojis Padlocks**: Purged. All server emojis are fully unlocked and usable without restriction.
- ❌ **Commercial Community Upsell Modals**: Purged.
- **Vertical Spacing Rule**: User Management immediately follows Customisation without any gap or empty placeholder where Boosts would be.
