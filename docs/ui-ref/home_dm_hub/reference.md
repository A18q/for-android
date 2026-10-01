# Discord Android — Home / DM Hub Screen Reference

## Research Summary

**Source quality:** PARTIAL — No direct screenshot obtained. Descriptions derived from:
- Web search results (2025–2026 Discord mobile design documentation)
- Discord blog and support article descriptions of "You Bar" and mobile visual refresh
- Cross-referenced with Stoat codebase which already implements a close Discord-parity version

**Approximate date of design:** Mid-2026 (You Bar era, squircle language)

**Key design language signals from search results:**
- "You Bar" is a persistent card-like nav surface at the bottom of the app
- People represented by **circles**; UI elements use **squircles** (rounded-corner rectangles)
- Four themes: Light, Ash (OG dark), Dark, Onyx (true AMOLED black)
- Bg colors: bg_tertiary `#1E1F22`, bg_secondary `#2B2D31`, brand `#5865F2`, text_normal `#DBDEE1`, text_muted `#949BA4`
- "Decluttered" chat bar, more padding/breathing room
- Bottom nav with Home/Servers/DMs structure (old "Home/Friends/Search/Mentions" is OUTDATED)

---

## Screen: Home / DM Hub (Direct Messages Tab)

This is the screen shown when the user taps the "Direct Messages" tab in the bottom navigation — the central DM list view.

### Layout (top → bottom, left → right)

#### 1. Top App Bar (bg: `#1E1F22`)
- **Left**: Hamburger/menu icon button (drawer toggle) — only present in drawer-mode layouts
- **Center-left**: Title text: **"Messages"** — bold, 24sp, `#F2F3F5`
- **No right-side overflow menu** in this header

#### 2. Action Button Row (below title, horizontal)
- **Search button** — 38dp squircle (`RoundedCornerShape(12dp)`), bg `#2B2D31`, search icon; turns blurple `#5865F2` when active
- **Message Requests / Mail button** — 38dp squircle, bg `#2B2D31`, mail icon; navigates to Friends screen
- **"Add Friends" pill** — 38dp height, squircle bg `#2B2D31`, person-add icon + "Add Friends" label text at 14sp SemiBold
- **Spacer** (flex weight)
- **New DM "+" button** — 38dp squircle, bg `#5865F2` (blurple), white + icon; navigates to Friends

#### 3. Expandable Search Bar (animated, hidden by default)
- Full-width `OutlinedTextField`, height 50dp
- `RoundedCornerShape(10dp)`, bg `#2B2D31`
- Focused border: `#5865F2`; unfocused: `Color.Transparent`
- Placeholder: "Search direct messages..." in `#949BA4`
- Leading icon: search icon (muted); trailing: clear icon (muted, appears when text present)
- Filters DM list in real-time

#### 4. "Active Now" Horizontal Story Row (conditional — only when online DM partners exist, search is blank)
- Horizontal `LazyRow`, 16dp horizontal padding, 10dp spacing between items
- Each item: **`ActiveNowFriendCard`** — 86dp squircle card (`RoundedCornerShape(20dp)`) bg `#2B2D31`
  - Inside: 52dp circular user avatar centered
  - 16dp status dot badge at bottom-right, with 2dp ring in card bg color
  - Status colors: Online `#23A55A`, Idle `#F0B232`, DND `#F23F43`, Offline `#80848E`
  - Tap → opens DM channel

#### 5. DM List (LazyColumn, fills remaining space)
Each row = `DirectMessageItemRow`:
- **Row height**: 72dp, `RoundedCornerShape(12dp)` clip, `combinedClickable` (tap + long-press)
- **Left**: 48dp circular `UserAvatar` with 14dp presence dot badge
- **Center column** (weight 1):
  - Name: 15sp, SemiBold (Bold if unread), `#F2F3F5`
  - Message preview: 13sp, Normal (SemiBold if unread), `#949BA4` (or `#DBDEE1` if unread)
  - Preview shows "You: [content]" for self-sent, "📷 Attachment" for media, blank if empty
- **Right column** (end-aligned):
  - Relative timestamp: 12sp (Bold+`#F2F3F5` if unread, else muted)
  - Unread dot: 10dp circle, `#ED4245` (Discord red badge color) — appears only if unread

**Empty state**: centered icon + "No direct messages yet" or "No conversations match your search"

#### 6. Long-press Context Sheet (modal bottom sheet)
- `ModalBottomSheet` with `ChannelContextSheet` composable
- bg `#1E1F22`

#### 7. Bottom Spacer
- 32dp bottom spacer to account for system navigation + user capsule

---

## Navigation Context (Drawer)

The DM Hub is reached via the **left drawer** (`ChannelSideDrawer`). In drawer-mode:

### Left Rail (64dp wide, bg `#1E1F22`)
- **Sticky header**: Self user avatar (48dp circle), tapping navigates to DM overview
  - Status presence dot 16dp at bottom-right
- **Unread DM avatars**: DM channels with unread messages appear as avatar icons in the rail above servers
  - Groups show `GroupIcon`
  - DMs show `UserAvatar` with presence
- **Horizontal divider** separating DM unreads from server list
- **Server icons** (scrollable `LazyColumn`):
  - Each server: `ServerRailIcon` — 48dp squircle icon (corner radius animates 24dp→16dp when selected)
  - Left-side pill indicator: 8dp wide, height animates (36dp = selected, 8dp = unread, 0dp = none)
  - Voice badge: 16dp circle at bottom-right if voice activity exists
  - Folder support: `FolderRailHeader` with collapsible folder preview
  - Drag-to-reorder with haptics
- **"+" Add Server** button — 48dp circle
- **Explore/Discover** button — 48dp circle
- **Settings** button (conditional) — 48dp circle

### Right Panel (channel list area)
#### Server Banner Header
- Animates height: 192dp (scrolled to top) → 128dp (scrolled down) → 76dp (no banner)
- Banner image with dark gradient overlay
- Server name text + official/verified badge icons
- "⋮" overflow button for server context sheet

#### DM Channel List (when no server selected)
- "Direct Messages" row (navigation item, icon + text)
- "Friends" row (with unread badge if incoming requests)
- "Saved Messages / Notes" row (if exists)
- Horizontal divider
- Individual DM/Group channel items (`DMOrGroupItem`)

#### User Capsule (bottom of right panel, above nav bars)
`StoatUserCapsule` — floating pill, `RoundedCornerShape(28dp)`, bg `#1E1F22`, border `#2B2D31`, elevation 12dp:
- 38dp avatar with 14dp status dot
- Display name (14sp Bold) + presence/status text (12sp, `#949BA4`)
- "∨" toggle indicator next to name
- 32dp circle bell button (bg `#2B2D31`) → notification overview
- 32dp circle settings gear button (bg `#2B2D31`) → settings
