# Discord Android — Modals & Context Menus Reference

## Research Summary

**Screens covered:**
1. **Message Long-Press Context Bottom Sheet** (`MessageContextSheet.kt`)
2. **In-Server Member Profile Popout & Moderation Sheet** (`UserInfoSheet.kt`, `MemberContextSheet.kt`, `UserButtons.kt`)
3. **Server Context Bottom Sheet** (`ServerContextSheet.kt`)
4. **Channel Context Bottom Sheet** (`ChannelContextSheet.kt`)

**Sources consulted:**
- Forensic baseline captures: `STOAT_MODERN_LAYOUT_SPEC.md` (Sections 4, 6, 7), `MODERN_LAYOUT_AUDIT.md` (Sections 1, 5; on-device screen recordings `SVID_20260926_113537_1.mp4`, `SVID_20260926_114528_1.mp4`, `SVID_20260926_124248_1.mp4`)
- Discord Android v346+ live UI forensics and support documentation (2025–2026)
- Local mockup references: `/root/stoat/mockup_references/member_card_profile_sheet.png`, `member_moderation_actions.png`, `channel_permissions_modal.png`

**Approximate date of design:** 2025 – Late 2026 (Modern squircle design system, elevated card-stacked bottom sheets)

**Reference quality:** HIGH — Textual, structural, token, and forensic video documentation verified.

---

## Screen 1: Message Long-Press Context Sheet

Triggered by long-pressing any chat message bubble in a text channel or direct message.

### Visual Tokens
- Sheet Canvas Background (`bg_tertiary`): `#1E1F22`
- Card Item / Reaction Bubble Background (`bg_secondary`): `#2B2D31`
- Reaction Pill Border: `#35373C` (unselected) / `#5865F2` (selected)
- Text Header / Primary: `#F2F3F5` / `#DBDEE1`
- Muted Icon / Text: `#949BA4`
- Destructive Red: `#F23F43`
- Corner Radius: Sheet top corners `RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)`, reaction pills `RoundedCornerShape(16.dp)`, action rows `RoundedCornerShape(8.dp)`

### Layout (top → bottom, left → right)

#### 1. Drag Handle
- Centered pill: 36dp width × 4dp height, `#4E5058`, 8dp top padding

#### 2. Quick-Reactions Horizontal Pill Bar
Horizontal scrollable row of common / frequently used emoji reactions:
- Container: Horizontal `Row`, 16dp horizontal padding, 8dp item spacing
- Quick reaction pills (5 items):
  - `✅` Checkmark
  - `💀` Skull
  - `❤️` Heart
  - `🔥` Fire
  - `😂` Laughing
  - Dimensions: 44dp × 44dp squircle pill, bg `#2B2D31`, 1dp border `#35373C`, centered 24dp emoji
- **Add Reaction Button**: 44dp × 44dp squircle pill, bg `#2B2D31`, `+` smiley face icon (`ic_add_reaction_24dp`), opens full emoji picker bottom sheet

#### 3. Message Preview Snippet (Mini Quote Header)
- 1dp subtle divider `#35373C`
- Mini message quote preview:
  - 24dp circular author avatar
  - Author display name (12sp bold `#F2F3F5`)
  - Ellipsized message text snippet (12sp `#949BA4`, max 1 line)

#### 4. Action Button Stack (Vertical Sheet Buttons)
Grouped card rows (`RoundedCornerShape(8.dp)`, bg `#232428` or transparent with touch ripple, 48dp row height):
- **Reply**: Left reply arrow icon (`ic_reply_24dp`, 20dp `#949BA4`), headline "Reply" (15sp `#DBDEE1`). Tapping dismisses sheet and docks reply banner above composer with Reddit-style spine.
- **Edit Message** (conditional: author only): Left pencil icon (`ic_edit_24dp`), headline "Edit Message" (15sp). Opens inline composer editor.
- **Copy Text**: Left copy icon (`ic_content_copy_24dp`), headline "Copy Text" (15sp). Copies message body to clipboard.
- **Copy Message Link**: Left link icon (`ic_link_24dp`), headline "Copy Message Link" (15sp). Copies permalink to clipboard.
- **Forward**: Left forward arrow icon (`ic_forward_24dp`), headline "Forward" (15sp).
- **Pin Message** / **Unpin Message**: Left pin icon (`ic_pin_24dp`), headline "Pin Message" (15sp).
- **Mark Unread**: Left envelope icon (`ic_mark_chat_unread_24dp`), headline "Mark Unread" (15sp).
- **Copy Message ID** (Developer mode): Left ID tag icon (`ic_identifier_copy_24dp`), headline "Copy ID" (15sp).
- **Report Message** (conditional: non-author): Left flag icon (`ic_report_24dp`, danger red `#F23F43`), headline "Report Message" (15sp bold `#F23F43`).
- **Delete Message** (conditional: author or ManageMessages perm): Left trash icon (`ic_delete_24dp`, danger red `#F23F43`), headline "Delete Message" (15sp bold `#F23F43`). Opens confirmation dialog.

---

## Screen 2: In-Server User Profile & Member Sheet

Triggered by tapping any user's avatar or username in chat, the member list, or direct messages.

### Visual Tokens (Forensic Baseline & Video 4)
- Sheet Canvas: `#111214` (DiscordDarkCanvas)
- Card Surfaces: `#232428` (DiscordCardSurface)
- Inset Pill Surfaces: `#1E1F22` (DiscordInsetSurface)
- Divider Stroke: `#2B2D31` (DiscordDivider)
- Header Text: `#F2F3F5`
- Subtitle / Muted Text: `#949BA4`
- Blurple Accent: `#5865F2`

### Layout (top → bottom, left → right)

#### 1. Header Banner & Overlapping Avatar Staging
- **Banner Area**: 135dp height, full-width custom user image or vibrant gradient banner (`#5865F2` alpha to `#232428`)
- **Overlapping Avatar**: 96dp circular avatar anchored at bottom-left of banner, overlapping by 40dp into content area; surrounded by a thick 6dp cut-out border in canvas color `#111214`; live status dot badge (26dp) at bottom-right (`#23A55A` Online, `#F0B232` Idle, `#F23F43` DND, `#80848E` Offline)
- **Badge Capsule (Right-Anchored on Banner)**: Rounded capsule (`RoundedCornerShape(10dp)`, bg `#1E1F22`, border 1dp `#2B2D31`) displaying user badges (Developer, Supporter, Moderator, Founder, Early Adopter)

#### 2. User Identity Block
- **Display Name**: 24sp bold `#F2F3F5` (or server nickname if set)
- **Handle & Pronouns Line**: `@username#0000 • pronouns` (14sp medium `#949BA4`)
- **Custom Status Bubble**: Squircle pill card (`RoundedCornerShape(12dp)`, bg `#232428`, padding 12dp horizontal, 8dp vertical): emoji + custom status text (13sp medium `#F2F3F5`)

#### 3. Primary Action Row
- **Self User**: Full-width "Edit Profile" button (42dp height, `#5865F2` Blurple, white text 14sp SemiBold, pencil icon)
- **Other User**:
  - **Send Message Button**: 38dp × 38dp squircle icon button (bg `#35373C`, chat bubble icon `#DBDEE1`)
  - **Relationship Button**:
    - Add Friend: Blurple button (`#5865F2`, person-add icon + "Add Friend")
    - Friend: Pill indicator (`#2B2D31`, checkmark `#23A55A` + "Friends")
    - Incoming Request: "Accept" (Blurple) + "Decline" (Red)
    - Outgoing Request: "Cancel Request"
    - Blocked: "Unblock"
  - **More Options `⋮` Button**: Opens menu with Copy ID, Report User, and Moderation Actions (Timeout, Kick, Ban)

#### 4. Stacked Content Cards (12dp padding, 8dp vertical spacing, bg `#232428`, `RoundedCornerShape(8dp)`)
- **Card 1: ABOUT ME (Bio)**:
  - Header: "ABOUT ME" (11sp bold `#949BA4`, letter-spacing 0.8sp)
  - Body: Markdown rendered user bio (`ChatMarkdown`)
- **Card 2: MEMBER SINCE**:
  - Header: "MEMBER SINCE" (11sp bold `#949BA4`)
  - Server Join Date row: Explore icon (`ic_explore_24dp`), Server Name, relative date (e.g. "Sep 12, 2024")
  - Stoat Account Creation Date row: Stoat chat icon, "Stoat", relative date (e.g. "2 years ago")
- **Card 3: ROLES — {count}**:
  - Header: "ROLES — 6" (11sp bold `#949BA4`)
  - Wrapping flow row (`FlowRow`) of hierarchical `DiscordRolePill` items:
    - Inset surface (`#1E1F22`, `RoundedCornerShape(6dp)`, padding 8dp × 6dp)
    - 11dp circular role color dot (e.g. `#57F287` Friend, `#9B59B6` Admin, `#F1C40F` VIP)
    - 12sp medium role name text `#DBDEE1`
- **Card 4: MODERATOR ACTIONS (In-Server Profile)**:
  - 2×2 action grid:
    - `⚙️ Manage`: Opens server member nickname and role assignment sheet
    - `⏱️ Timeout`: Quick duration selector (60s, 5m, 10m, 1h, 1d, 1w)
    - `👢 Kick`: Danger red kick action with reason prompt
    - `🚫 Ban`: Maximum severity red ban action with message delete purge window

---

## Screen 3: Server Context Sheet

Triggered by long-pressing a server icon in the left rail or tapping `⋮` next to the server name in the channel drawer.

### Layout (top → bottom)
1. **Drag Handle**: 36dp × 4dp pill, `#4E5058`
2. **Server Overview Card Header**:
   - 48dp squircle server icon + Server name (18sp bold `#F2F3F5`)
   - Server description / member count
   - 1dp horizontal divider `#3A3C42`
3. **Primary Action Group**:
   - **Invite People**: Highlighted blurple action button (`ic_group_add_24dp`, headline "Invite People", special styling)
   - **Notification Settings**: Bell icon (`ic_notifications_24dp`), headline "Notification Settings"
   - **Create Channel**: Plus icon (`ic_add_24dp`), headline "Create Channel". Opens Create Channel dialog (Text vs Voice toggle, channel name, category)
4. **Utility Action Group** (separated by 1dp divider `#3A3C42`):
   - **Mark as Read**: Checkmark icon (`ic_mark_chat_read_24dp`), headline "Mark as Read"
   - **Copy Server ID**: Copy icon (`ic_identifier_copy_24dp`), headline "Copy Server ID"
   - **Add to Folder / Remove from Folder**: Folder icon (`ic_folder_24dp`)
   - **Server Identity / Nickname**: ID card icon (`ic_id_card_24dp`), headline "Server Identity"
   - **Server Settings**: Gear icon (`ic_settings_24dp`), headline "Server Settings" (navigates to `ServerSettingsHome`)
5. **Danger Zone**:
   - **Non-Owner**: "Report Server" (Flag icon, red) + "Leave Server" (Door icon, red, with "Leave Silently" checkbox)
   - **Owner**: "Delete Server" (Trash icon, red, confirmation body "This action is irreversible")

---

## Screen 4: Channel Context Sheet

Triggered by long-pressing a channel in the channel drawer list.

### Layout (top → bottom)
1. **Drag Handle**
2. **Channel Header**: `#` or `🔊` icon + Channel Name (16sp bold `#F2F3F5`)
3. **Action Rows**:
   - **Mark as Read**: Envelope/Checkmark icon
   - **Channel Settings / Edit Channel** (conditional: permissions): Gear icon
   - **Copy Channel Link**: Link icon
   - **Copy Channel ID**: Tag icon
   - **Delete Channel** (conditional: permissions): Trash icon (danger red `#F23F43`)
