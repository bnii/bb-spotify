# bb-spotify

A single-file Babashka CLI for Spotify. No daemon, no external deps — just a script calling the Spotify Web API with PKCE auth.

## Prerequisites

- [Babashka](https://github.com/babashka/babashka)
- A Spotify account

## Setup

### 1. Create a Spotify App

1. Go to [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Click **Create App**
3. In **Settings** > **Redirect URIs**, add: `http://127.0.0.1:12983/callback`
4. Copy the **Client ID**

### 2. Authenticate

```bash
spotify auth <your-client-id>
```

This saves your client ID and opens the browser for Spotify login. Tokens are stored in `~/.config/bb-spotify/` and auto-refresh.

Alternatively, set `SPOTIFY_CLIENT_ID` as an env var and run `spotify auth` without arguments.

## Usage

```bash
# Search
spotify search "All Blues Miles Davis"

# Playlists
spotify playlists
spotify playlist create "Jazz Blues Essentials"
spotify playlist show <id>
spotify playlist add <id> <uri> [uri...]
spotify playlist remove <id> <uri> [uri...]

# Devices
spotify devices                       # List available devices
spotify device <name-or-id> [play]    # Transfer playback (name is fuzzy-matched)

# Playback
spotify play <uri>                    # Track or playlist URI
spotify play <playlist-name>          # Play playlist by name (fuzzy match)
spotify pause
spotify resume
spotify next / prev
spotify queue                         # Show queue
spotify queue <uri>                   # Add to queue
spotify now                           # What's playing
```

Run `spotify --help` for the full reference.

## Bash Completion

```bash
source /path/to/completions/spotify.bash
# or copy to:
# ~/.local/share/bash-completion/completions/spotify
```

Completes commands, subcommands, device names, and playlist names.

## Token Storage

Tokens in `~/.config/bb-spotify/tokens.edn`, client ID in `config.edn`. Tokens auto-refresh before expiry.
