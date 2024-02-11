# Polywoof for RuneLite\![<img align="right" height="192" src="https://user-images.githubusercontent.com/13049652/172053653-043b4dce-1bfb-46a5-82a6-d56fae313b9f.png">](icon.png)

[![](https://img.shields.io/endpoint?url=https://i.pluginhub.info/shields/rank/plugin/polywoof)](https://runelite.net/plugin-hub/show/polywoof)
[![](https://img.shields.io/endpoint?url=https://i.pluginhub.info/shields/installs/plugin/polywoof)](https://runelite.net/plugin-hub/show/polywoof)
[![](https://img.shields.io/discord/321345656184635402?label=Discord)](https://discord.gg/QbuVGMErrX)
[![](https://img.shields.io/github/stars/furfy/polywoof?style=social)](../..)

## Guide for DeepL

> [!NOTE]
> **DeepL API Key** is **REQUIRED** for this to work.

- You have to register **DeepL Account** and subscribe for *free* or *paid* **Developer's API**, then you have to copy your **API Key** from this [page](https://www.deepl.com/pro-account/summary).
	- If you can't register or verify for any reason – ask someone to help you out!
- Open plugin config and paste it into the **API Key** field, type in your destination **Language** and choose whatever you want to be translated in the **Translation** section!
	- [List](src/main/resources/languages.json) of <span title="Last updated on February 6 Year 2024"><ins>available languages</ins></span>!
	- Be warned that any additional stuff will use your **API Quota**, so choose wisely.
- **H2 SQL** database is located under `.runelite/cache/polywoof.mv.db` with saved translations.

> [!WARNING]
> Make sure to choose the right plan, **__DeepL API Free__** or **API __Pro__**, not **~~DeepL Starter~~**, **~~Advanced~~** or **~~Ultimate~~**!

## Frequently Asked Questions

- _Not working at all, nothing appears, nothing works._
  - You didn't put the required **API Key** in the config, or got the wrong one.
- _Can't see any words, squares appear but not text._
  - Your **Language** probably requires different charset, **change the font**.
- _**DeepL** is not working in my country, what should I do?_
  - I pointed out in the **Guide**, you should ask your friends for help.
- _What happens when **API Quota** is all used?_
	- Nothing horrible, but you have to wait till the ending of the month.
- _I've put everything in the config, but it's still translating to **English**._
  - You selected the wrong backend in the config, **Generic** backend is for debugging purposes only.
- _Can you add **Interface** translation? Do you have a plan to make an **unofficial translation**?_
  - This plugin is designed to be quick-and-easy **Framework** and **Helper** for automatic translation that still requires basic English knowledge, not a translation project.
- _Please, add different translation backend, like **Google** or **ChatGPT**._
	- I've tried my best to make the code expandable, feel free to **contribute**.
- _Some books, text or scrolls are not translating, can you enable them?_
	- The **UI** in this game is very unpredictable, you can enable **Generic** backend and select the text with the **Select Widget** menu option and send the screenshot to me.
- _How to export or transfer my saved translations? Can I share it with my friends?_
	- Absolutely, you have to copy mentioned **H2 SQL** file to the **same directory** on another computer.
- _Why it looks so ugly on the preview? Can I change that?_
  - This is an old screenshot for nostalgic purposes, there are a lot of **tweaks** available in the config.

> [!IMPORTANT]
> If you encountered any **bugs** or need **help** – please, join our **[Discord](https://furfy.github.io/invite)** server!

[![](https://user-images.githubusercontent.com/13049652/161437194-fca3d9c0-7226-40ed-9403-b4c01393f1af.png)](../..)

[![DigitalOcean Referral Badge](https://web-platforms.sfo2.digitaloceanspaces.com/WWW/Badge%203.svg)](https://www.digitalocean.com/?refcode=71af1247dfc7&utm_campaign=Referral_Invite&utm_medium=Referral_Program&utm_source=badge)

*Люблю Дэйвика.. 💕*
