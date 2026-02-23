# Bash completion for bb-spotify
#
# Install (pick one):
#   source /path/to/completions/spotify.bash        # in ~/.bashrc
#   cp spotify.bash ~/.local/share/bash-completion/completions/spotify

_spotify() {
    COMPREPLY=()
    while IFS= read -r line; do
        [[ -n "$line" ]] && COMPREPLY+=("$line")
    done < <(spotify --complete "${COMP_CWORD}" "${COMP_WORDS[@]}" 2>/dev/null)
}
complete -o filenames -F _spotify spotify
