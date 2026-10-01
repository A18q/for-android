# Discord Android UI Reference — Friends List & DM Chat

## Research Summary

**Screens covered:**
1. Friends List Screen (`FriendsScreen`) — Relationships directory, status buckets, and Add Friends flow
2. Direct Message (DM) Chat Screen (`ChannelScreen`) — Top app bar, message stream, Reddit-style branching reply spine, and message composer dock

**Sources consulted:**
- Live forensic captures of Discord Android v346.13 (`SVID_20260926_113537_1.mp4`, `SVID_20260926_114424_1.mp4`, `SVID_20260926_114528_1.mp4`, `SVID_20260926_124248_1.mp4`)
- Forensic documentation: `/root/stoat/STOAT_MODERN_LAYOUT_SPEC.md`, `/root/stoat/MODERN_LAYOUT_AUDIT.md`
- Discord official blog / changelog (discord.com) — 2025–2026 Mobile Visual Refresh & "You Bar" architecture
- Mockup reference captures: `/root/stoat/mockup_references/channel_view_chat_stream.png`, `invite_friends_bottom_sheet.png`

**Approximate date of design:** Late 2026 (Discord Android v346.13 / React Native .86 refresh)

**Key Modern Discord Design Language Invariants (Post-2023 Redesign):**
- **Shape Philosophy**: People are represented by **circles** (avatars, presence rings); UI containers and controls are represented by **squircles** (`RoundedCornerShape(8.dp)` to `RoundedCornerShape(16.dp)`).
- **Surface Elevation**: Layered charcoal hierarchy:
  - `bg_tertiary` (`#1E1F22`): Drawer, rail, root container background
  - `bg_secondary` (`#2B2D31`): Cards, input bars, bottom sheets
  - `bg_primary` (`#313338`): Active chat viewport, message area
  - `brand` (`#5865F2`): Accent, active states, send button
  - `text_header` (`#F2F3F5`): Primary titles and bold sender names
  - `text_normal` (`#DBDEE1`): Body chat messages, descriptions
  - `text_muted` (`#949BA4`): Timestamps, subtitles, unread/offline counters
  - `unread_badge` (`#ED4245`): Notification count dots, destructive action icons
- **Ergonomics**: Tighter corner radii on mobile controls; decluttered message composer with unified attachment and emoji actions.
- **Monetization Fat Purge**: Hard ban on Nitro gift icons (`🎁`), avatar decorations, profile effects, Orbs balances, and subscription upsell cards.

---

## Screen 1: Friends List Screen (`FriendsScreen`)

> Accessed via the "Add Friends" pill or message request icons in DM Hub, or directly from the navigation drawer.

### Layout (top → bottom, left → right)

#### 1. Top App Bar (Height: 48dp–56dp, bg: `#1E1F22`)
- **Navigation Icon (Left)**:
  - In drawer mode: Hamburger menu icon button (`ic_menu_24dp`), 36dp touch target, tint `#F2F3F5`.
  - In nested stack mode: Back arrow icon button (`ic_arrow_back_24dp`), 36dp touch target, tint `#F2F3F5`.
- **Title (Center-left)**:
  - Text: **"Friends"** — bold, 20sp, color `#F2F3F5`, single-line with ellipsis.
- **Actions (Right)**:
  - **"⋮" Overflow Menu button**: IconButton (tint `#F2F3F5`) opening dropdown menu with bulk management actions:
    - `"Deny all incoming"` (clears all incoming friend requests in one tap).

#### 2. Categorized Friends Directory (`LazyColumn` with sticky headers)
Items are grouped into distinct collapsible status buckets with countable headers:

1. **Incoming Friend Requests Section** (conditional — visible only when `incomingRequests.isNotEmpty()`):
   - **Sticky Header**: `CountableListHeader` — uppercase title `"INCOMING FRIEND REQUESTS"`, 12sp bold, text color `#949BA4`, trailing numeric badge counter pill (`#2B2D31` bg, `#F2F3F5` text).
   - **Friend Request Item Row** (Height: ~60dp, background `#2B2D31` card surface, rounded corners):
     - **Left**: 40dp circular user avatar with status badge cutout.
     - **Middle**: Display name (15sp bold, `#F2F3F5`) + username (`@handle#discriminator`, 12sp `#949BA4`).
     - **Right Quick Action Pair**:
       - Accept button: 32dp circular button, bg `#23A55A` (green), white checkmark icon (`✓`).
       - Decline button: 32dp circular button, bg `#35373C`, muted close/cross icon (`✕`).

2. **Outgoing Friend Requests Section** (conditional — visible only when `outgoingRequests.isNotEmpty()`):
   - **Sticky Header**: `CountableListHeader` — uppercase title `"OUTGOING FRIEND REQUESTS"`, numeric count pill.
   - **Item Row**: Avatar, display name, username, and trailing cancel button (`✕`).

3. **Online Friends Section** (conditional — visible when online friends exist):
   - **Sticky Header**: `CountableListHeader` — `"ONLINE — [count]"`, 12sp bold, `#949BA4`.
   - **Friend Row** (Height: 56dp, rounded card or padded row):
     - **Left**: 40dp circular avatar with live presence ring:
       - 🟢 Online: `#23A55A`
       - 🟡 Idle: `#F0B232`
       - 🔴 DND: `#F23F43`
     - **Center Column**:
       - Display name: 15sp SemiBold, `#F2F3F5`, role-colored if assigned.
       - Custom status line: 12sp muted `#949BA4`, single-line ellipsis (e.g. "Listening to Spotify", "Playing VS Code").
     - **Right Quick Action Pair** (1-Tap Engagement):
       - **Direct Message button**: 36dp squircle/circle hitbox, tint `#949BA4`, icon `ic_chat_24dp` → immediately navigates to DM channel with friend.
       - **Voice Call button**: 36dp squircle/circle hitbox, tint `#949BA4`, icon `ic_call_24dp` → initiates direct voice call.

4. **All Friends / Offline Friends Section**:
   - **Sticky Header**: `CountableListHeader` — `"ALL FRIENDS — [count]"`.
   - **Friend Row**: Avatar with offline status ring (`#80848E`), display name, offline status text ("Offline" or last seen), and dual quick action buttons (`💬` Message, `📞` Call).

5. **Blocked Users Section** (conditional — visible when blocked users exist):
   - **Sticky Header**: `CountableListHeader` — `"BLOCKED — [count]"`.
   - **Row**: Avatar with strike-through or block badge, username, and trailing "Unblock" action button.

#### 3. Floating Action Button & Menu (Bottom-Right Dock)
- **Primary Toggle FAB**: 56dp pill/circle, blurple `#5865F2` container, white `+` icon transitioning to `✕` when expanded.
- **Speed Dial Menu Items** (slide/expand up vertically):
  - **"Add by Username / Tag"**: Icon `ic_tag_24dp` + label `"Add by Username"`.
  - **"Scan QR Code"**: Icon `ic_qr_code_scanner_24dp` + label `"Scan QR Code"`.
  - **"Create Group"**: Icon `ic_group_add_24dp` + label `"New Group"`.

#### 4. Add Friends Bottom Sheets
1. **Add by Tag Sheet** (`ModalBottomSheet`, bg `#1E1F22`):
   - Top Nametag vector graphic (200dp wide, centered).
   - Title: `"Add by Username"` (18sp bold `#F2F3F5`).
   - Description text explaining format (`username#0000` or global handle).
   - Input row:
     - Username `TextField` (flex weight 1, bg `#2B2D31`, text `#F2F3F5`).
     - Divider `#`.
     - 4-digit Tag `TextField` (80dp width, numeric keyboard).
   - Action buttons:
     - `"Paste from Clipboard"` `TextButton`.
     - `"Send Friend Request"` primary blurple `Button` (`#5865F2`).
2. **Scan QR Code Sheet**:
   - Camera viewfinder scanner overlay.
   - Result sheet displaying scanned user's 128dp avatar, display name, handle, `"Add Friend"` button, and `"Cancel"` button.

---

## Screen 2: Direct Message (DM) Chat Screen (`ChannelScreen`)

> The primary conversation view for 1:1 and Group Direct Messages.

### Layout (top → bottom, left → right)

#### 1. Top App Bar (Height: 48dp, bg: `#313338`, bottom border: `rgba(0,0,0,0.25)`)
- **Navigation (Left)**:
  - Hamburger menu icon button (`ic_menu_24dp`) if drawer mode active.
  - Back arrow icon button (`ic_arrow_back_24dp`) if navigating from stack.
- **Channel Identity Header (Center-left, interactive tap → opens ChannelInfoSheet)**:
  - 24dp circular partner avatar with 10dp live presence dot badge at bottom-right (for 1:1 DMs), or group icon (for Group DMs).
  - Partner Display Name: 20sp bold, `#F2F3F5`, single-line ellipsis.
  - Presence status badge / icon (Online, Idle, DND).
  - Subtle chevron indicator (`ic_keyboard_arrow_right_24dp`, 16dp, alpha 0.5).
- **Actions (Right)**:
  - **Start Voice Call button**: 36dp hitbox, icon `ic_call_24dp__fill`, tint `#DBDEE1` (triggers voice connection overlay).
  - **Start Video Call button**: 36dp hitbox, icon `ic_videocam_24dp`, tint `#DBDEE1` (triggers video call flow).
  - **In-Channel Search button**: 36dp hitbox, icon `ic_search_24dp`, tint `#DBDEE1` (opens Omni-filter DM search).
- **Voice Call Active Banner** (conditional):
  - Green `#23A55A` banner when voice call is connected with participant avatars, mute, and disconnect controls.

#### 2. Chat Viewport (`LazyColumn` with reversed layout, bg: `#313338`)

##### A. Start of Conversation Header (`ConversationStartHeader`)
Positioned at the oldest boundary of the chat history (scrolled to top):
- **User Avatar**: 80dp circular avatar centered on left with 22dp live presence dot.
- **Display Name**: 28sp bold, color `#F2F3F5`, line height 32sp.
- **Username Handle**: 15sp `#949BA4` (e.g. `@wladyslaw`).
- **Signature Welcome Message**:
  - `"This is the beginning of your direct message history with [DisplayName]."` (14sp `#DBDEE1`).
- **Wave CTA Button**:
  - Squircle card button (`#2B2D31` surface, 8dp radius), label `"Wave to [DisplayName] 👋"`, `#F2F3F5` text, sends `"👋"` emoji into the composer on tap.

##### B. Date Dividers (`DateDivider`)
- Centered horizontal rule with date capsule (`#2B2D31` background, 11sp bold text `#949BA4`, e.g., `"September 26, 2026"`).

##### C. Message Items (`RegularMessage` / `Message`)
Each message item consists of:
1. **Reddit-Style Branching Reply Spine** (when replying to a message — Section 9 Invariant):
   - **Spine Path**: Curved 2dp solid stroke (`#4E5058`), path `M 8 12 L 8 4 Q 8 1 14 1 L 24 1`.
   - **Inline Parent Preview**:
     - 16dp circular avatar of replied-to author.
     - Author name: 12sp bold, role-colored or `#DBDEE1`.
     - Mention indicator: `@` prefix if ping enabled.
     - Quote snippet: 12sp muted `#B5BAC1`, single-line ellipsis.
     - **Interaction**: Tapping anywhere on the spine or parent preview smooth-scrolls and animates a temporary background highlight on the parent message.
2. **Main Message Container**:
   - **Avatar (Left)**: 40dp circular avatar (or placeholder space if consecutive tail message from same user).
   - **Header Line (Top)**:
     - Author name: bold 14sp, colored with user role/custom color.
     - Role icons / Bot badges / Verified tags.
     - Timestamp: 12sp muted `#949BA4`, relative format ("Today at 12:43 PM", "Yesterday", "MM/dd/yy").
     - Edited indicator: `ic_edit_24dp` icon + `"edited"` text if modified.
   - **Message Body**:
     - Markdown formatted text with custom font scaling for emojis (Gigamoji).
     - Inline mentions highlighted with translucent blurple pill (`#5865F2` alpha 0.2).
   - **Media & File Attachment Cards**:
     - Image / Video thumbnails: rounded corners (`8dp`), max width 280dp, tap to open fullscreen gallery.
     - File cards: 280dp card, `#2B2D31` background, 1dp border `#35373C`, file icon (`📎`), bold filename (12sp `#F2F3F5`), size string (10sp `#949BA4`), download icon (`⬇️`).
   - **Reactions Bar**:
     - Horizontal `FlowRow` of reaction pills (`#2B2D31` surface, 8dp radius, emoji + count, tinted `#5865F2` border if reacted by current user).
     - Quick Add Reaction button (`+` smiley icon).

##### D. Scroll to Bottom FAB (`ScrollDownFAB`)
- Small Floating Action Button (`#2B2D31` container, down arrow icon `ic_south_24dp`).
- Red badge counter pill (`#ED4245` container, bold `"NEW"` text) when unread messages arrive while scrolled up.

#### 3. Chat Input Dock / Message Composer (`MessageField`)
Pinned to the bottom above navigation bars, elevated with 28dp blur / surface container:

1. **Typing Indicator Row**:
   - Height: 20dp, animated pulsing dots + text: `"[User] is typing..."` (11sp `#949BA4`).
   - Slowmode countdown timer display if channel slowmode is active.

2. **Replying Indicator Bar** (`ReplyManager` — conditional, appears when reply is armed):
   - Background: `#2B2D31`, 1dp top divider `#35373C`.
   - Left: Close/cancel button (`ic_close_24dp`, 16dp).
   - Inline replied-to author 16dp avatar + name (`12sp bold`).
   - Mention toggle button: `"@ ON"` (blurple `#5865F2`) / `"@ OFF"` (muted `#949BA4`).
   - Content snippet preview.

3. **Composer Bar**:
   - Container shape: `RoundedCornerShape(24dp)` squircle/pill.
   - Surface color: `#2B2D31` (`var(--bg-secondary)`), subtle elevation shadow.
   - **Left Attachment Button (`+`)**:
     - 32dp circle hitbox, white `+` icon (`ic_add_24dp`, 22dp, tint `#949BA4`).
     - Tapping toggles inline attachment drawer (Gallery / Camera / Files).
   - **[CRITICAL MONETIZATION PURGE]**:
     - The modern Discord Android app places a Nitro Gift box icon (`🎁`) next to the `+` button.
     - **Stoat Rule**: The gift button is **100% DELETED**, with zero stub and zero layout gap. The text field expands directly from the `+` button.
   - **Text Input Area**:
     - Auto-expanding `BasicTextField` (max height 128dp, scrollable).
     - Placeholder text: `"Message @[DisplayName]..."` in `#949BA4`.
     - Cursor: blurple `#5865F2`.
     - Autocomplete suggestion popup bar: pops up above input when typing `@`, `#`, or `:` for instant inline mention/emoji completion.
   - **Right Emoji Picker Trigger**:
     - 32dp circle hitbox, smiling face icon (`ic_mood_24dp`, 22dp, tint `#949BA4`).
     - Tapping toggles zero-bloat emoji/sticker bottom sheet (all custom server emojis unlocked, zero Nitro padlocks).
   - **Far Right Action Button**:
     - **When input is empty and no attachments**: Microphone icon button (`ic_mic_24dp`) for push-to-talk voice messages.
     - **When input has text or attachments**: Blurple Send button pill (`ic_send_24dp`, `#5865F2` container, white icon, 40dp x 32dp) animates smoothly into view.
