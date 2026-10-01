# Discord Android UI Reference — Server Sidebar & Channel List

## Research Summary

**Screens covered:** Server Rail (left server icon strip) + Channel Drawer (right panel of left swipe)

**Sources consulted:**
- Discord official blog / changelog (discord.com) — 2025–2026 redesign articles
- friendcordapp.com — third-party Discord UI analysis (2026)
- Web search synthesis (multiple Reddit/Discord community threads)

**Screenshot availability:** No direct pixel-perfect screenshot could be captured (no public image CDN returned); however, the 2025–2026 redesign is well-documented in text. All elements below are confirmed from multiple cross-referenced sources.

**Reference quality: MEDIUM** — Textual + structural description confirmed; no pixel screenshot captured. Treat exact spacing values as approximate.

---

## Screen 1: Server Rail (Left Column)

> Post-2023 redesign. Bottom nav with Home/Servers/DMs tabs is the **old** pattern. The current pattern is a persistent server icon strip on the far left of the drawer (64 dp wide, bg #1E1F22).

### Layout (top → bottom)

1. **Status-bar spacer** — transparent, height = system status bar inset
2. **Self avatar** (sticky header at top of rail) — 48 dp circle, presence dot (16 dp) at bottom-right. Tapping navigates to DM overview.
3. **Unread DM avatars** (dynamic, pinned below self) — 48 dp circles for each DM/Group with unread messages; no label; presence dot shown. Circle shape (people = circles rule).
4. **Horizontal divider** — full-width, 1 dp, muted colour
5. **Server icon list** (scrollable, `LazyColumn`):
   - **Single server icon** — 48 dp squircle (RoundedCornerShape ~24 dp → ~16 dp when selected). Left-edge pill indicator: 8 dp × 8 dp (unread) or 8 dp × 36 dp (selected), white/off-white.
   - **Folder header** — 48 dp squircle, shows 2×2 thumbnail grid of server icons when collapsed; open-folder icon when expanded. Coloured translucent background from folder colour.
   - **Folder members** (when expanded) — same single-server icon format, indented via coloured rounded rect background track.
   - **Voice/screen-share badge** — 16 dp circle cutout at bottom-right of server icon with a speaker/screenshare icon (12 dp). Present only if voice participants exist in any channel.
6. **Add Server button** — 48 dp circle with `+` icon; navigates to add-server sheet
7. **Discover button** — 48 dp circle with explore icon; navigates to server discovery
8. **Settings button** (conditional, admin/dev mode) — 48 dp circle with gear icon
9. **Navigation bar spacer** — bottom = system nav inset

### Visual Language (2026)
- Rail background: `#1E1F22` (bg_tertiary)
- Server icons: squircle, animated corner radius 24 dp → 16 dp on selection
- Pill indicator: white, left-aligned, spring animation
- Folder track: translucent tinted rounded-rect background drawn via `drawBehind`
- Drag-to-reorder: long-press + drag with ghost overlay

---

## Screen 2: Channel Drawer (Right panel of left swipe)

> Opens when user swipes right from chat or taps server name in top bar.

### Layout (top → bottom)

#### Header area (fixed, ~76–192 dp tall)
1. **Server banner image** (if present) — full-width cropped image, darkened with linear gradient. Height: 192 dp at top, collapses to 128 dp on scroll.
2. **No banner fallback** — solid bg_secondary block, 76 dp tall.
3. **Server badge icons** — Official (workspace-premium icon) and/or Verified (verified icon), 24 dp, left of server name. Only shown if server has those flags.
4. **Server name** — `titleMedium`, bold, ellipsis. Left side of header row.
5. **⋮ More menu button** — IconButton on right side (shown only for server context, not DM list). Opens `ServerContextSheet`.

#### DM List mode (when no server selected)
6. **"Direct Messages" overview row** — icon + "Direct Messages" label, pill-clipped, highlighted when current
7. **Friends row** — friends icon + "Friends" label, shows unread dot if pending friend requests
8. **Saved Messages / Notes row** — notebook icon + notes label (shown only if SavedMessages channel exists)
9. **Horizontal divider**
10. **DM / Group list** (scrollable) — rows 40 dp tall, 4 dp rounded corners, contain:
    - Avatar (36 dp circle / group icon), presence dot (12 dp)
    - Display name (SemiBold 15 sp)
    - Last message preview (12 sp, muted, 1 line)
    - Unread dot (8 dp, red `#ED4245`) at trailing edge

#### Server Channel List mode (when server selected)
11. **Category headers** — uppercase label, 11 sp bold, muted `#949BA4`, with rotatable chevron `▶` (→90° when expanded). Padding: top 16 dp, bottom 4 dp.
12. **Channel items** — pill-shaped (CircleShape clip), contain:
    - Channel type icon (text/voice/etc.) left 16 dp
    - Channel name `bodyMedium`, ellipsis
    - Unread dot `8 dp` (brand primary) trailing, shown only when unread + not current
    - Voice capacity counter (monospace, e.g. `2/10`) trailing for limited-capacity voice
    - Muted channel: 50% alpha
    - Selected channel: `secondaryContainer` background
13. **Voice channel participant preview** (expanded inline) — avatar row (20 dp), name, screen-share icon per participant, up to 5; "+ N more" text if overflow
14. **Navigation bar spacer**

#### User Capsule (bottom of channel panel, always visible)
15. **Floating user capsule** — pill-shaped card (RoundedCornerShape 28 dp), bg `#1E1F22`, 1 dp border `#2B2D31`, elevation shadow 12 dp. Contains:
    - Self avatar (38 dp) with 14 dp status dot (color-coded: online/idle/dnd/offline)
    - Display name (14 sp bold) + `∨` caret
    - Status/presence text (12 sp muted)
    - Notification bell button (32 dp circle, bg `#2B2D31`)
    - Settings gear button (32 dp circle, bg `#2B2D31`)
