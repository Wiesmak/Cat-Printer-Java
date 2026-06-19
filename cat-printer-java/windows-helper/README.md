# cat-printer-ble-helper (Windows)

A small C# console app that wraps `Windows.Devices.Bluetooth` (WinRT) and
exposes a line-delimited JSON protocol over stdin/stdout. Driven from Java by
`com.catprinter.ble.windows.WindowsTransport`.

## Why it exists

The JVM has no working BLE GATT library on Windows. WinRT is the official
Windows 10/11 BLE API. This helper is the minimum native shim that lets
`cat-printer-java` actually talk to a printer on Windows.

## Rebuilding

You only need to do this if you change `Program.cs`. The committed
`dist/cat-printer-ble-helper.exe` is what the Java library bundles.

Requirements: Windows 10/11 build machine with **.NET 8 SDK**.

```cmd
cd cat-printer-java\windows-helper
dotnet publish -c Release -r win-x64 --self-contained ^
    -p:PublishSingleFile=true ^
    -p:IncludeNativeLibrariesForSelfExtract=true ^
    -o dist
```

The result is `dist\cat-printer-ble-helper.exe` (~30 MB self-contained
single-file). Commit it. The Gradle build copies it into the published jar.

If size matters, switch to native AOT by adding `<PublishAot>true</PublishAot>`
to `CatPrinterBleHelper.csproj` (output drops to ~3 MB). Same code; AOT just
takes longer to build.

## Protocol

One JSON object per line. `stdin` carries commands, `stdout` carries replies
(matched by `id`) and notification events (no `id`). `stderr` is free-form
diagnostic logging.

### Commands

| op | fields |
|---|---|
| `scan` | `id`, `timeout_ms` |
| `connect` | `id`, `address` (e.g. `"AA:BB:CC:DD:EE:FF"`) |
| `subscribe` | `id`, `characteristic` (UUID lowercase) |
| `write` | `id`, `characteristic`, `data_b64` |
| `disconnect` | `id` |
| `exit` | (no id; helper terminates) |

### Replies

```json
{"id": 1, "result": "ok", "devices": [{"name": "SC03h", "address": "AA:BB:..."}]}
{"id": 2, "result": "ok"}
{"id": 3, "result": "error", "error": "characteristic-not-found: ..."}
```

### Events

```json
{"event": "notify", "characteristic": "0000ae02-...", "data_b64": "UXi..."}
```

## Permissions

Windows 10 1809+ may prompt for Bluetooth permission the first time a desktop
process accesses BLE. Approve once; subsequent runs do not prompt.

## License

Same as the parent project (GPL-3.0-or-later).
