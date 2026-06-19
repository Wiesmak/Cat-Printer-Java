// Cat-Printer BLE helper for Windows. Wraps Windows.Devices.Bluetooth (WinRT)
// and exposes a tiny line-delimited JSON protocol over stdin/stdout. Driven
// from Java via WindowsTransport.
//
// Protocol (one JSON object per line):
//   stdin commands:
//     {"op":"scan","id":N,"timeout_ms":4000}
//     {"op":"connect","id":N,"address":"AA:BB:CC:DD:EE:FF"}
//     {"op":"subscribe","id":N,"characteristic":"0000ae02-..."}
//     {"op":"write","id":N,"characteristic":"0000ae01-...","data_b64":"..."}
//     {"op":"disconnect","id":N}
//     {"op":"exit"}
//   stdout replies:
//     {"id":N,"result":"ok",...}
//     {"id":N,"result":"error","error":"..."}
//   stdout events (no id):
//     {"event":"notify","characteristic":"...","data_b64":"..."}

using System.Collections.Concurrent;
using System.Text.Json;
using System.Text.Json.Nodes;
using Windows.Devices.Bluetooth;
using Windows.Devices.Bluetooth.Advertisement;
using Windows.Devices.Bluetooth.GenericAttributeProfile;
using Windows.Storage.Streams;

namespace CatPrinter.BleHelper;

internal static class Program
{
    private static readonly object StdoutLock = new();
    private static BluetoothLEDevice? _device;
    private static readonly Dictionary<string, GattCharacteristic> _characteristics = new();
    private static readonly HashSet<string> _subscribed = new();

    public static async Task<int> Main()
    {
        Console.OutputEncoding = System.Text.Encoding.UTF8;
        string? line;
        while ((line = Console.In.ReadLine()) != null)
        {
            JsonNode? node;
            try
            {
                node = JsonNode.Parse(line);
            }
            catch (Exception e)
            {
                WriteError(null, "bad-json: " + e.Message);
                continue;
            }
            if (node is not JsonObject obj) { WriteError(null, "expected-json-object"); continue; }
            string op = obj["op"]?.GetValue<string>() ?? "";
            int? id = obj["id"]?.GetValue<int>();
            try
            {
                switch (op)
                {
                    case "scan": await HandleScan(id, obj); break;
                    case "connect": await HandleConnect(id, obj); break;
                    case "subscribe": await HandleSubscribe(id, obj); break;
                    case "write": await HandleWrite(id, obj); break;
                    case "disconnect": HandleDisconnect(id); break;
                    case "exit": return 0;
                    default: WriteError(id, "unknown-op: " + op); break;
                }
            }
            catch (Exception e)
            {
                WriteError(id, e.GetType().Name + ": " + e.Message);
            }
        }
        return 0;
    }

    private static async Task HandleScan(int? id, JsonObject obj)
    {
        int timeoutMs = obj["timeout_ms"]?.GetValue<int>() ?? 4000;
        var devices = new ConcurrentDictionary<ulong, (string? name, ulong addr)>();
        var watcher = new BluetoothLEAdvertisementWatcher
        {
            ScanningMode = BluetoothLEScanningMode.Active
        };
        watcher.Received += (s, args) =>
        {
            string? name = args.Advertisement.LocalName;
            if (string.IsNullOrEmpty(name))
            {
                // Some advertisements have the name only in scan response; use what we have.
            }
            devices.AddOrUpdate(args.BluetoothAddress,
                _ => (name, args.BluetoothAddress),
                (_, prev) => string.IsNullOrEmpty(prev.name) ? (name, prev.addr) : prev);
        };
        watcher.Start();
        await Task.Delay(timeoutMs);
        watcher.Stop();

        var arr = new JsonArray();
        foreach (var entry in devices.Values)
        {
            arr.Add(new JsonObject
            {
                ["name"] = entry.name ?? "",
                ["address"] = FormatAddress(entry.addr)
            });
        }
        WriteReply(id, new JsonObject { ["result"] = "ok", ["devices"] = arr });
    }

    private static async Task HandleConnect(int? id, JsonObject obj)
    {
        string addrStr = obj["address"]?.GetValue<string>() ?? throw new ArgumentException("address required");
        ulong addr = ParseAddress(addrStr);
        Disconnect();
        _device = await BluetoothLEDevice.FromBluetoothAddressAsync(addr);
        if (_device == null)
        {
            WriteError(id, "device-not-found");
            return;
        }
        // Force a service enumeration so writes/subscribes succeed.
        var servicesResult = await _device.GetGattServicesAsync(BluetoothCacheMode.Uncached);
        if (servicesResult.Status != GattCommunicationStatus.Success)
        {
            WriteError(id, "gatt-services-failed: " + servicesResult.Status);
            return;
        }
        _characteristics.Clear();
        foreach (var svc in servicesResult.Services)
        {
            var charsResult = await svc.GetCharacteristicsAsync(BluetoothCacheMode.Uncached);
            if (charsResult.Status != GattCommunicationStatus.Success) continue;
            foreach (var ch in charsResult.Characteristics)
            {
                _characteristics[ch.Uuid.ToString().ToLowerInvariant()] = ch;
            }
        }
        WriteReply(id, new JsonObject { ["result"] = "ok" });
    }

    private static async Task HandleSubscribe(int? id, JsonObject obj)
    {
        string uuid = (obj["characteristic"]?.GetValue<string>() ?? "").ToLowerInvariant();
        if (!_characteristics.TryGetValue(uuid, out var ch))
        {
            WriteError(id, "characteristic-not-found: " + uuid);
            return;
        }
        if (_subscribed.Contains(uuid))
        {
            WriteReply(id, new JsonObject { ["result"] = "ok" });
            return;
        }
        var status = await ch.WriteClientCharacteristicConfigurationDescriptorAsync(
            GattClientCharacteristicConfigurationDescriptorValue.Notify);
        if (status != GattCommunicationStatus.Success)
        {
            WriteError(id, "subscribe-failed: " + status);
            return;
        }
        ch.ValueChanged += (sender, args) =>
        {
            byte[] data = new byte[args.CharacteristicValue.Length];
            DataReader.FromBuffer(args.CharacteristicValue).ReadBytes(data);
            WriteEvent(new JsonObject
            {
                ["event"] = "notify",
                ["characteristic"] = sender.Uuid.ToString().ToLowerInvariant(),
                ["data_b64"] = Convert.ToBase64String(data)
            });
        };
        _subscribed.Add(uuid);
        WriteReply(id, new JsonObject { ["result"] = "ok" });
    }

    private static async Task HandleWrite(int? id, JsonObject obj)
    {
        string uuid = (obj["characteristic"]?.GetValue<string>() ?? "").ToLowerInvariant();
        string b64 = obj["data_b64"]?.GetValue<string>() ?? "";
        if (!_characteristics.TryGetValue(uuid, out var ch))
        {
            WriteError(id, "characteristic-not-found: " + uuid);
            return;
        }
        byte[] data = Convert.FromBase64String(b64);
        var writer = new DataWriter();
        writer.WriteBytes(data);
        var option = ch.CharacteristicProperties.HasFlag(GattCharacteristicProperties.WriteWithoutResponse)
            ? GattWriteOption.WriteWithoutResponse
            : GattWriteOption.WriteWithResponse;
        var status = await ch.WriteValueAsync(writer.DetachBuffer(), option);
        if (status != GattCommunicationStatus.Success)
        {
            WriteError(id, "write-failed: " + status);
            return;
        }
        WriteReply(id, new JsonObject { ["result"] = "ok" });
    }

    private static void HandleDisconnect(int? id)
    {
        Disconnect();
        WriteReply(id, new JsonObject { ["result"] = "ok" });
    }

    private static void Disconnect()
    {
        _characteristics.Clear();
        _subscribed.Clear();
        _device?.Dispose();
        _device = null;
    }

    private static void WriteReply(int? id, JsonObject body)
    {
        if (id.HasValue) body["id"] = id.Value;
        WriteLine(body);
    }

    private static void WriteError(int? id, string error)
    {
        var obj = new JsonObject { ["result"] = "error", ["error"] = error };
        if (id.HasValue) obj["id"] = id.Value;
        WriteLine(obj);
    }

    private static void WriteEvent(JsonObject body)
    {
        WriteLine(body);
    }

    private static void WriteLine(JsonObject body)
    {
        string s = body.ToJsonString(new JsonSerializerOptions { WriteIndented = false });
        lock (StdoutLock)
        {
            Console.Out.WriteLine(s);
            Console.Out.Flush();
        }
    }

    private static string FormatAddress(ulong addr)
    {
        return string.Format("{0:X2}:{1:X2}:{2:X2}:{3:X2}:{4:X2}:{5:X2}",
            (addr >> 40) & 0xff, (addr >> 32) & 0xff, (addr >> 24) & 0xff,
            (addr >> 16) & 0xff, (addr >> 8) & 0xff, addr & 0xff);
    }

    private static ulong ParseAddress(string s)
    {
        string hex = s.Replace(":", "").Replace("-", "");
        if (hex.Length != 12) throw new ArgumentException("bad-address: " + s);
        return Convert.ToUInt64(hex, 16);
    }
}
