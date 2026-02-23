# AGENTS.md — bb-spotify

Babashka CLI for the Spotify Web API. Single script, no external dependencies.

## Binary

`spotify` in the repo root (symlinked to `~/.local/bin/spotify`).

## Commands

### Setup

```bash
spotify auth <client-id>   # Save client ID + authenticate via browser OAuth (PKCE)
```

Client ID is persisted to `~/.config/bb-spotify/config.edn`. Tokens to `~/.config/bb-spotify/tokens.edn`. Auth is interactive (opens browser) — only needed once, tokens auto-refresh.

### Search

```bash
spotify search <query>     # Returns up to 10 tracks
```

Output columns: `#`, `Track`, `Artist`, `URI`. Use the URI in other commands.

### Playlists

```bash
spotify playlists                        # List all user playlists (Name, Tracks, ID)
spotify playlist create <name>           # Create private playlist → returns ID and URI
spotify playlist show <id>               # List tracks in playlist (#, Track, Artist)
spotify playlist add <id> <uri> [uri...] # Add tracks by URI (batch supported)
spotify playlist remove <id> <uri>...    # Remove tracks by URI
```

### Devices

```bash
spotify devices                      # List available devices (name, type, ID). Active marked *
spotify device <name-or-id>          # Transfer playback to device (keeps play state)
spotify device <name-or-id> play     # Transfer and force start playing
```

Device accepts an exact ID or a case-insensitive substring of the device name (e.g. `spotify device dot play`).

### Playback

Requires an active Spotify device. Use `spotify devices` + `spotify device` to select one.

```bash
spotify play [uri]         # No args = resume. Accepts track or playlist URIs.
spotify pause              # Pause playback
spotify resume             # Resume playback
spotify next               # Skip to next track
spotify prev               # Previous track
spotify queue <uri>        # Add track to playback queue
spotify now                # Show currently playing (track, artist, album, URI, progress)
```

## URI Format

- Track: `spotify:track:<id>` — from `spotify search` output
- Playlist: `spotify:playlist:<id>` — from `spotify playlists` or `spotify playlist create`

## Common Workflows

```bash
# Find and play a song
spotify search "All Blues Miles Davis"
spotify play spotify:track:<id-from-search>

# Build a playlist
spotify playlist create "My Playlist"
spotify search "Blue Monk Thelonious Monk"
spotify playlist add <playlist-id> spotify:track:<id1> spotify:track:<id2>
spotify play spotify:playlist:<playlist-id>

# Batch add — pass multiple URIs in one call
spotify playlist add <id> spotify:track:aaa spotify:track:bbb spotify:track:ccc
```

## Error Handling

| Error                        | Meaning                                                  | Fix                                      |
|------------------------------|----------------------------------------------------------|------------------------------------------|
| `Not authenticated`          | No tokens found                                          | Run `spotify auth <client-id>`           |
| `No active device found`     | Spotify app not open or hasn't played anything yet        | `spotify devices` then `spotify device <name> play` |
| `Restriction violated`       | Action invalid in current state (e.g. prev with no queue) | Expected; handle gracefully              |
| `Token refresh failed`       | Refresh token expired or revoked                         | Re-run `spotify auth`                    |

## Notes

- Search returns max 10 results per query (Spotify API limit)
- `spotify play` with no URI is equivalent to `spotify resume`
- Playlist IDs and track URIs are stable — safe to store and reuse
- All commands exit 0 on success, 1 on error with a message to stdout
