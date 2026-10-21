# Reverse Engineered Private Arsenal

A Reverse Engineering Hub industry that deconstructs weapons, fighters and ships and stocks them
in an integrated Private Arsenal submarket. Forked from Dark QOL with Darkelle24's permission, now
Andrzej's own mod. These instructions apply to this repository, including when it is opened
without the parent mod workspace. The workspace `AGENTS.md` one folder up governs releases,
packaging, Git identity and communication.

## Notion

Tasks and history: the **Reverse Engineered Private Arsenal board** in Andrzej's Notion, under
**Starsector mods** (`collection://8654862e-ca5d-423d-9345-141c703ca5de`). Working rules and game
API notes: **Claude Information** (`collection://7bf9b1d6-b9e5-4b62-87f1-757bbaa51919`), starting
with "Start here: working on Starsector mods through Notion". Read the board before working, give
every task a ticket (ticket ids start with REPA) and keep it current. The mod is also bundled in
Coalesced Little Mods (`src/privatearsenal` there): bring every change over there too. The Notion
tools are deferred: load them with ToolSearch first.

Update the board during the work, not only at release time. Before changing files, put the
message on its ticket: a new request gets a new ticket (In progress), and a problem Andrzej
reports on Needs Human Test work moves that ticket to Changes Requested with the notes quoted.
Before ending the turn, update the ticket's body (What was done, How, Commits, Test list) and
its Status, Version and End date. Read the board with `notion-query-data-sources` mode `view`
on `view://3fd20ca7-5d20-8155-92f8-000cc410377a` or `notion-fetch` a ticket. SQL and rows mode share a
small quota that runs out. The workspace `AGENTS.md` has the full routine.

## Essentials

- id `re_private_arsenal`, package `privatearsenal`, ID prefix `RePA_`, LunaLib field ids
  `repa_re_*`. Java 8 bytecode, LunaLib at runtime.
- Build: `gradle jar` with `C:\Programming\Languages\Java\gradle-9.4.1\bin\gradle.bat` (no
  wrapper). Compile jars are vendored in `lib/`. Package: `gradle releaseZip` stages
  `ReverseEngineeredPrivateArsenal/` and writes `PrivateArsenal.zip` (runtime files only, `utils/`
  stays out).
- The old v1.2.4 tag points at commit 2ebab39, not the 1.2.4 code. Leave it.
- Release history in `CHANGELOG.md`.

## Update metadata

Keep `mod_info.json`, `re_private_arsenal.version` and `data/config/version/version_files.csv`
consistent. Forum topic 35627.

- masterVersionFile:
  `https://raw.githubusercontent.com/andrzejsokolowski/starsector-private-arsenal/main/re_private_arsenal.version`
- directDownloadURL, asset `PrivateArsenal.zip`:
  `https://github.com/andrzejsokolowski/starsector-private-arsenal/releases/latest/download/PrivateArsenal.zip`
