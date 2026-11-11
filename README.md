# Payment Alerts (Fabric, Minecraft 1.21.11)

Client-side mod. When a server message like `cobhaa paid you $10M.` arrives, it plays an animated alert
based on the amount (all drawn on the HUD, no resource pack needed):

| Amount | Effect |
|---|---|
| $10M | Skin-tab toast, head does a crouch dance (4 styles) |
| $50M | Money rain (bills / gold bars / gems / mixed) + gold tab |
| $100M | Fireworks (classic / gold / rainbow / hearts) + name card |
| $250M | Lightning storm (blue / purple / red / gold) |
| $500M | Slot-machine jackpot (6 winning symbols) + coins + fireworks |
| $1B | Nuke (classic / radioactive / plasma / hellfire) |
| $5B | Orbital laser (cyan / red death ray / purple / gold / green) |
| $10B | Black hole -> new universe (4 colours) |
| $100B | God tier (gold / diamond / emerald / netherite) |

Highest tier whose minimum is reached is used. Big effects queue one after another; toasts stack.

## Build the .jar with GitHub
1. Create a new GitHub repo and upload everything in this folder (keep the `.github` folder).
2. Open the **Actions** tab -> the `build` workflow runs on push (or press *Run workflow*).
3. When it finishes, open the run and download the **payment-alerts-jar** artifact. Inside is `payment-alerts-1.0.0.jar`.
4. Put it in `.minecraft/mods` with Fabric Loader 0.18+ and **Fabric API** for 1.21.11.

## Commands (client side)
- `/payalert demo` – queue every tier
- `/payalert test <amount> [variation 1-6] [player]` – e.g. `/payalert test 500M 3`
- `/payalert toggle`, `/payalert clear`

## Config: `config/paymentalerts.json`
- `pattern` – regex; group 1 = player, group 2 = number, group 3 = K/M/B/T. Change it if your server words the message differently.
- `listenToChat` – false by default so players can't fake alerts by typing in chat.
- `sounds`, `enabled`.
