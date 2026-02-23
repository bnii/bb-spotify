# bb-spotify

Babashka CLI for the Spotify Web API. Single script, no external dependencies.

## Files

| File                       | Purpose                                    |
|----------------------------|--------------------------------------------|
| `spotify`                  | Main script (symlinked to `~/.local/bin/`) |
| `spotify_test.bb`          | Unit tests (`bb spotify_test.bb`)          |
| `completions/spotify.bash` | Bash tab-completion                        |

## Auth

```bash
spotify auth <client-id>   # Save client ID + authenticate via browser OAuth (PKCE)
spotify auth               # Authenticate using SPOTIFY_CLIENT_ID env var or saved config
```

Client ID lookup order: `SPOTIFY_CLIENT_ID` env var > `~/.config/bb-spotify/config.edn`.
Tokens: `~/.config/bb-spotify/tokens.edn` (auto-refresh).

## Commands

```bash
# Search
spotify search <query>               # Up to 10 tracks. Columns: #, Track, Artist, URI

# Playlists
spotify playlists                     # List all (Name, Tracks, ID)
spotify playlist create <name>        # Create private playlist
spotify playlist show <id>            # List tracks
spotify playlist add <id> <uri>...    # Add tracks (batch)
spotify playlist remove <id> <uri>... # Remove tracks

# Devices
spotify devices                       # List devices. Active marked *
spotify device <name-or-id>           # Transfer playback
spotify device <name-or-id> play      # Transfer + start playing

# Playback
spotify play                          # Resume
spotify play <uri>                    # Play track or playlist URI
spotify play <name>                   # Play playlist by name (fuzzy match)
spotify pause
spotify resume
spotify next
spotify prev
spotify queue                         # Show now playing + upcoming
spotify queue <uri>                   # Add track to queue
spotify now                           # Current track info + progress
```

Device name matching: case-insensitive substring (e.g. `spotify device dot`).
Playlist name matching: exact then substring, case-insensitive.

## URI Format

- Track: `spotify:track:<id>`
- Playlist: `spotify:playlist:<id>`

## Errors

| Error                    | Fix                                                      |
|--------------------------|----------------------------------------------------------|
| `Not authenticated`      | `spotify auth <client-id>` or set `SPOTIFY_CLIENT_ID`   |
| `No active device found` | `spotify devices` then `spotify device <name> play`      |
| `Restriction violated`   | Action not valid in current state                        |
| `Token refresh failed`   | Re-run `spotify auth`                                    |

## Notes

- All commands exit 0 on success, 1 on error
- `spotify play` with no args is equivalent to `spotify resume`
- Search hardcodes limit=10 results
