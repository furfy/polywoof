# Polywoof for RuneLite\![<img align="right" height="192" src="https://user-images.githubusercontent.com/13049652/172053653-043b4dce-1bfb-46a5-82a6-d56fae313b9f.png">](icon.png)

[![](https://img.shields.io/endpoint?url=https://i.pluginhub.info/shields/rank/plugin/polywoof)](https://runelite.net/plugin-hub/show/polywoof)
[![](https://img.shields.io/endpoint?url=https://i.pluginhub.info/shields/installs/plugin/polywoof)](https://runelite.net/plugin-hub/show/polywoof)
[![](https://img.shields.io/discord/321345656184635402?label=Discord)](https://discord.gg/QbuVGMErrX)
[![](https://img.shields.io/github/stars/furfy/polywoof?style=social)](../..)

Translation **Framework** for **RuneLite** that can translate *ACTUALLY* everything[^1]!
**Original Text** and **UI** are untouched to prevent confusion of misleading translations.
Support for any type of language characters[^2], including **Japanese**, **Chinese** and many others.
Embedded **H2 SQL** database[^3] with an organized schema for the supported languages and offline translations.

- [x] Translation for **NPC**, **Player** and other dialogs.
- [x] Translation for **Game Messages** and **Examines**.
- [x] Translation for **NPC** and **Item**[^4] names.
- [x] Translation for **Dialog Options**.
- [x] Translation for **Overhead Text**.
- [x] Translation[^5] for **Scrolls**, **Books**, **Player Messages** and any other text on the screen.

[^1]: Not interaction **Menus**, not **Stats**, not other basic **UI** elements.
[^2]: You have to provide your own font via the config.
[^3]: Translation via `Select Text` won't be saved into the database, only temporarily.
[^4]: Only when you click **Examine**.
[^5]: Available only via context menu of the **Quick Actions** button.

## Guide for DeepL

> [!NOTE]
> **DeepL API Key** is _REQUIRED_ for this to work.

- You have to register **DeepL Account** and subscribe for *free* or *paid* **Developer's API**, copy your **API Key** from this [page](https://www.deepl.com/pro-account/summary).
	- If you can't register or verify for any reason – ask someone to help you!
- Open the config and paste the key into the **API Key** field, type in your destination **Language** and choose whatever you want to be translated in the **Translation** section!
	- [List](src/main/resources/languages.json) of <span title="Last updated on February 6 Year 2024"><ins>supported languages</ins></span>!
	- Be warned that any additional stuff will use your **API Quota**, so choose wisely.
- **H2 SQL** database is located at `.runelite/cache/polywoof.mv.db` path.

> [!WARNING]
> Make sure to choose the right plan, **__DeepL API Free__** or **API __Pro__**, not ~~_DeepL Starter_~~, ~~_Advanced_~~ or ~~_Ultimate_~~!

## Frequently Asked Questions

- _Not working at all, nothing appears, nothing works._
	- You haven't set the required **API Key** in the config, or got the wrong one.
- _Can't see any words, squares appear but not text._
	- Your **Language** probably requires different charset, **change the font**.
- _**DeepL** not available in my country, what should I do?_
	- I've pointed out in the **Guide**, you should ask your friends for help.
- _What happens when **API Quota** is all used?_
	- Nothing horrible, but you have to wait till the ending of the month.
- _I've put everything in the config, but it's translating to **English**._
	- You've selected the wrong backend in the config, **Generic** backend for debugging purposes only.
- _Can I change where the text boxes appear?_
	- Just like any other elements, hold **Alt** key and drag.
- _Can you add an **Interface** translation? Do you have a plan to make an **Unofficial** translation?_
	- This plugin is designed to be quick-and-easy **Framework** and **Helper** for automatic translations that still require basic English knowledge, not a translation project.
- _Please, add different translation backend, like **Google** or **ChatGPT**._
	- They are not available for me due to sanctions, but I've tried my best to make the code expandable, feel free to **Contribute**.
- _Some books, text or scrolls are not translating, can you enable them?_
	- The **UI** in this game is very unpredictable, you can translate them manually via `Select Text` context menu option.
- _How to **Export** or **Transfer** my saved translations? Can I share them with my friends?_
	- Absolutely, you have to copy mentioned **H2 SQL** file to the same directory on another computer.
- _Why it looks so ugly on the **Preview**? Can I change that?_
	- This is an old screenshot for nostalgic purposes, there are a lot of tweaks available in the config.

> [!IMPORTANT]
> If you've encountered any **bugs** or need **help** – please, join our **[Discord](https://furfy.github.io/invite)** server!

[![](https://user-images.githubusercontent.com/13049652/161437194-fca3d9c0-7226-40ed-9403-b4c01393f1af.png)](../..)

[![DigitalOcean Referral Badge](https://web-platforms.sfo2.digitaloceanspaces.com/WWW/Badge%203.svg)](https://www.digitalocean.com/?refcode=71af1247dfc7&utm_campaign=Referral_Invite&utm_medium=Referral_Program&utm_source=badge)

*Люблю Дэйвика.. 💕*
