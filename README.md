# bb-spotify

A single-file Babashka CLI for Spotify. No daemon, no external deps — just a script calling the Spotify Web API with PKCE auth.

## Prerequisites

- [Babashka](https://github.com/babashka/babashka) installed
- Spotify Premium account (Dev Mode requirement)

## Setup

### 1. Create a Spotify App

1. Go to [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Click **Create App**
3. In **Settings** > **Redirect URIs**, add: `http://127.0.0.1:8080/callback`
4. Copy the **Client ID**

### 2. Set environment variable

Add to your shell profile (`~/.bashrc`, `~/.zshrc`, etc.):

```bash
export SPOTIFY_CLIENT_ID="your_client_id_here"
```

### 3. Authenticate

```bash
spotify auth
```

This opens your browser for Spotify login and saves tokens to `~/.config/bb-spotify/tokens.edn`.

## Commands

### Search

```bash
spotify search "All Blues Miles Davis"
```

### Playlists

```bash
spotify playlists                              # List your playlists
spotify playlist create "Jazz Blues Essentials" # Create a playlist
spotify playlist show <id>                     # Show playlist contents
spotify playlist add <id> <uri> [uri...]       # Add tracks
spotify playlist remove <id> <uri> [uri...]    # Remove tracks
```

### Playback

```bash
spotify play <uri>       # Play a track or playlist URI
spotify pause            # Pause
spotify resume           # Resume
spotify next             # Next track
spotify prev             # Previous track
spotify queue <uri>      # Add to queue
spotify now              # Show what's playing
```

## Token storage

Tokens are stored in `~/.config/bb-spotify/tokens.edn` and auto-refresh before expiry.
