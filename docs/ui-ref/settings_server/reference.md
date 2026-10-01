# Discord Android — Server Settings Screen Reference

## Research Summary

**Screens covered:**
1. **Server Settings Home / Overview Hub** (reached via server context menu or top of channel list)
2. **Server Roles Configuration** (`settings/server/{serverId}/roles` & role editor)
3. **Server Channels & Category Management** (`settings/server/{serverId}/channels`)
4. **Custom Emojis Management** (`settings/server/{serverId}/emojis`)
5. **Member Moderation & Directory** (`settings/server/{serverId}/members`)
6. **Invites & Bans Management** (`settings/server/{serverId}/invites`, `settings/server/{serverId}/bans`)

**Sources consulted:**
- Forensic baseline captures: `STOAT_MODERN_LAYOUT_SPEC.md` (Sections 6 & 7), `MODERN_LAYOUT_AUDIT.md` (Section 5, device capture `SVID_20260926_124248_1.mp4`)
- Discord Android v346+ live UI forensics and support documentation (2025–2026)
- Local mockup references: `/root/stoat/mockup_references/server_settings_overview.png`, `server_roles_config.png`, `channel_category_settings.png`, `channel_permissions_modal.png`

**Approximate date of design:** 2025 – Late 2026 (Modern squircle design system, card-stacked settings surfaces)

**Reference quality:** HIGH — Textual, structural, token, and forensic video documentation verified.

---

## Screen 1: Server Settings Home (Main Menu)

This screen opens when a server administrator taps the **Server Settings** option from the server context sheet (`ServerContextSheet`).

### Visual Tokens
- Surface Background (`bg_primary`): `#313338` (or `#000000` in Onyx mode)
- Card Containers (`bg_secondary`): `#2B2D31`
- Inset Surfaces (`bg_tertiary`): `#1E1F22`
- Accent Brand: `#5865F2` (Blurple)
- Danger Accent: `#F23F43` (Red)
- Header Text: `#F2F3F5` (Bold 20sp for top bar, 11sp bold uppercase for category labels)
- Normal Text: `#DBDEE1`
- Muted Text: `#949BA4`
- Corner Radiuses: Outer grouped cards `12dp` or `16dp`, inner action pills `8dp`

---

### Layout (top → bottom, left → right)

#### 1. Top App Bar (bg: `#1E1F22`)
- **Left**: `←` Back navigation icon button (24dp hit target 48dp)
- **Title**: **"Server Settings"** (20sp bold `#F2F3F5`)
- **Right**: None (or overflow menu)

#### 2. Server Identity Header Card
- Top card block (`RoundedCornerShape(16dp)`, bg `#2B2D31`, horizontal margin 16dp)
- **Server Icon**: 64dp squircle icon (`RoundedCornerShape(18dp)`) with subtle 1dp border
- **Server Name**: 18sp bold text `#F2F3F5`
- **Server Stats / Subtitle**: "Created on [date] • [N] Members" in 12sp `#949BA4`

#### 3. Section: "OVERVIEW & STRUCTURE" (Header: 11sp bold uppercase `#949BA4`)
Card group (`RoundedCornerShape(12dp)`, bg `#2B2D31`):
- **Overview**:
  - Leading icon: Info icon (24dp `#949BA4`)
  - Headline: "Overview" (15sp `#DBDEE1`)
  - Trailing: Chevron `>`
  - [Subpage]: Server name input, icon upload/crop, system messages channel selector, default notification settings
- **Channels**:
  - Leading icon: Grid / Channel hash icon (24dp `#949BA4`)
  - Headline: "Channels" (15sp `#DBDEE1`)
  - Trailing: Chevron `>`
  - [Subpage]: List of channel categories, text channels, voice channels; create channel; drag-reorder

#### 4. Section: "CUSTOMISATION" (Header: 11sp bold uppercase `#949BA4`)
Card group (`RoundedCornerShape(12dp)`, bg `#2B2D31`):
- **Emoji**:
  - Leading icon: Mood / Smile icon (24dp `#949BA4`)
  - Headline: "Emoji" (15sp `#DBDEE1`)
  - Trailing: Chevron `>`
  - [Subpage]: Upload custom emojis, view emoji grid, delete emojis (all emojis 100% unlocked in Stoat)

#### 5. [DISCORD MONETIZATION SECTION — DELETED IN STOAT]
*In official Discord: "Server Boost Status", "Server Subscriptions", "Welcome Screen / Community Enablement Upsells", "Nitro Tier Progress Bar".*
*In Stoat: This section is completely eradicated. No boost badges, no tier meters, no paywalls.*

#### 6. Section: "USER MANAGEMENT" (Header: 11sp bold uppercase `#949BA4`)
Card group (`RoundedCornerShape(12dp)`, bg `#2B2D31`):
- **Roles**:
  - Leading icon: Flag / Shield icon (24dp `#949BA4`)
  - Headline: "Roles" (15sp `#DBDEE1`)
  - Trailing: Role count pill (`#1E1F22`, e.g., "7") + Chevron `>`
- **Members**:
  - Leading icon: Group / People icon (24dp `#949BA4`)
  - Headline: "Members" (15sp `#DBDEE1`)
  - Trailing: Chevron `>`
  - [Subpage]: Member directory, role assignment, kick/ban/timeout actions
- **Invites**:
  - Leading icon: Link icon (24dp `#949BA4`)
  - Headline: "Instant Invites" (15sp `#DBDEE1`)
  - Trailing: Chevron `>`
  - [Subpage]: Active invite links, expiry countdown, uses count, revoke buttons
- **Bans**:
  - Leading icon: Gavel icon (24dp `#949BA4`)
  - Headline: "Bans" (15sp `#DBDEE1`)
  - Trailing: Chevron `>`
  - [Subpage]: Banned users list with unban button

#### 7. Section: "DANGER ZONE"
Single card or standalone row (`RoundedCornerShape(12dp)`, bg `#2B2D31`, margin 16dp):
- **Delete Server**:
  - Leading icon: Trash icon (24dp, `#F23F43` danger red)
  - Headline: "Delete Server" (15sp bold, `#F23F43`)
  - Tap opens: Destructive Confirmation Alert Dialog with server name verification and "Delete" button (`#F23F43`)

---

## Screen 2: Server Roles Configuration (`settings/server/{serverId}/roles`)

### Layout (top → bottom)
1. **Top App Bar**:
   - `←` Back arrow
   - Title: **"Roles"**
   - Floating Action Button (FAB): `+` button in bottom-right (`#5865F2`) for creating new roles
2. **Role Count Eyebrow**:
   - "ROLES — 7" in 11sp bold `#949BA4`
3. **Draggable Roles List** (`LazyColumn` with drag-and-drop reordering):
   - Each role row:
     - **Drag handle**: 6-dot or 2-bar drag icon (`#949BA4`) with haptic tick on drag
     - **Role Color Indicator**: 12dp circular dot with assigned hex color (e.g. `#57F287`, `#9B59B6`, `#F1C40F`)
     - **Role Name**: 15sp medium `#DBDEE1`
     - **Hierarchy Lock**: Padlock icon if role rank is higher than user's own highest role
     - **Trailing action**: Chevron `>` navigating to Role Editor
4. **Create Role Dialog** (modal alert):
   - Title: "Create Role"
   - Text input: Role name (1..32 characters)
   - Buttons: Cancel / Create (Blurple `#5865F2`)

---

## Screen 3: Server Channels Management (`settings/server/{serverId}/channels`)

### Layout (top → bottom)
1. **Top App Bar**:
   - `←` Back arrow
   - Title: **"Channels"**
   - Floating Action Button (FAB): `+` button for creating new channel
2. **Category Blocks & Channel Hierarchy**:
   - **Category Header**: Uppercase category name (e.g., "TEXT CHANNELS", "VOICE CHANNELS") with collapse chevron and category settings gear
   - **Channel Items**:
     - Type Icon: `#` (Text) or `🔊` (Voice) or `📢` (Announcement) in `#949BA4`
     - Channel Name: 14sp medium `#DBDEE1`
     - Drag handle: For reordering channels within and across categories
     - Trailing gear: Navigates to Channel Settings (Name, Topic, Slowmode, Permissions)
3. **Create Channel Sheet / Dialog**:
   - Channel Type selector: Segmented toggle between **Text Channel** and **Voice Channel**
   - Channel Name input: with prefix `#` or `🔊`
   - Category selector dropdown
   - Create Channel button (`#5865F2`)
