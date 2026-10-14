# PollinationsMC

[![Made with pollinations.ai](https://raw.githubusercontent.com/pollinations/pollinations/main/packages/ui/src/brand/badge-made-with.svg)](https://pollinations.ai/?ref=badge)


**Pollinations plugin for Minecraft servers** — built for [Pollinations QUEST #17246](https://github.com/pollinations/pollinations/issues/17246) (10 Pollen reward)

Bring [Pollinations AI](https://pollinations.ai) to your Paper server: players chat with AI models and generate AI images that render onto in-game map items, using any model from the live [Pollinations catalog](https://gen.pollinations.ai/v1/models). The server owner's API key pays with their own Pollen.

## Features

- **`/poll text <prompt>`** — ask any Pollinations text model (GPT, Claude, Gemini, Llama...) right in chat
- **`/poll image <prompt>`** — generates an AI image and hands the player a filled map showing it (128x128, correct Minecraft map palette)
- **`/poll help`** — command list
- Async HTTP — the server thread never blocks on API calls
- Configurable models + API key in `config.yml`
- Works on **Paper 1.21+** (Spigot-compatible API usage)
- Zero external libraries — plain Java 21 + Bukkit API

## Install

1. Download `PollinationsMC.jar` from [Actions artifacts](../../actions/workflows/build.yml) (latest run → PollinationsMC-plugin)
2. Drop it into your server's `plugins/` folder
3. Start the server once, then edit `plugins/PollinationsMC/config.yml`:
   ```yaml
   api-key: "sk_..."   # get one at https://enter.pollinations.ai/keys
   text-model: "openai/gpt-5.4-nano"
   image-model: "black-forest-labs/flux.1-schnell"
   ```
4. Restart. Done. `/poll text hello` to test.

## Usage

```
/poll text what is the best diamond mining strategy
/poll image a dragon made of lava flying over a jungle
```

The image arrives as a **map item** named after your prompt — put it in an item frame to display AI art on your wall.

## Configuration (`plugins/PollinationsMC/config.yml`)

| Key | Default | Description |
|-----|---------|-------------|
| `api-key` | *(empty)* | Your Pollinations secret key. Required. |
| `text-model` | `openai/gpt-5.4-nano` | Any text model from the [catalog](https://gen.pollinations.ai/text/models) |
| `image-model` | `black-forest-labs/flux.1-schnell` | Any image model from the [catalog](https://gen.pollinations.ai/image/models) |
| `base-url` | `https://gen.pollinations.ai` | Pollinations API base URL |

## Build from source

```bash
mvn package
# -> target/PollinationsMC.jar
```

Requires Java 21+.

## Cost

Each generation spends [Pollen](https://enter.pollinations.ai) from the server owner's balance at the model's listed price. `gpt-5.4-nano` and `flux.1-schnell` are the cheapest good defaults; users with Quest Pollen can run them free.

## License

MIT — Built for Pollinations QUEST #17246 by [Sugamdeol](https://github.com/Sugamdeol)