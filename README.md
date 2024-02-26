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
[^5]: Available only via the context menu of the **Quick Actions** button.

## Guide for DeepL

> [!NOTE]
> **API Key** is _REQUIRED_ for **DeepL** backend!

- You have to register a [**DeepL**](https://www.deepl.com/signup) account, choose a *free* or *paid* [**DeepL API**](https://www.deepl.com/ru/pro-api) plan and copy your [**API Key**](https://www.deepl.com/pro-account/summary).
	- If you can't register or verify for any reason – ask someone to help you!
- Open the config and paste the key into **API Key** field, type in your destination **Language** and choose whatever you want to be translated in the **Translation** section!
	- [**List**](src/main/resources/languages.json) of <span title="Last updated on February 6 Year 2024"><ins>offline languages</ins></span>!
	- Be warned that any additional stuff will use your **API Quota**, so choose wisely.
- **H2 SQL** database is located at `.runelite/cache/polywoof.mv.db`.

## Guide for Google

> [!NOTE]
> **API Key** is _REQUIRED_ for **Google** backend!
- You have to create [**Google Cloud**](https://console.cloud.google.com/freetrial) account, [**Cloud Billing**](https://console.cloud.google.com/billing) account and link it to your default project.
- Enable [**Translation API**](https://console.cloud.google.com/flows/enableapi?apiid=translate.googleapis.com) and create [**API Key**](https://console.cloud.google.com/apis/credentials/key), copy it.
	- **Google** will charge you if **API** exceeds over **500,000** characters monthly, to prevent that visit [**Translation API**](https://console.cloud.google.com/apis/api/translate.googleapis.com/quotas) and set _«v2 and v3 general model characters per day»_ to **16,000** _(500,000 ÷ 31)_.
- Open the config and paste the key into the **API Key** field, and you're done!


## Frequently Asked Questions

- _How much money does it cost to get the **API Key**? Any way to make it **Free**?_
	- **DeepL API Free** and **Google Cloud** registration is totally **Free**, but a credit card is required for their verification process.
- _Not working at all, nothing appears, nothing works._
	- You haven't set the required **API Key** in the config, or got the wrong one.
- _Can't see any words, squares appear but not text._
	- Your **Language** requires a different charset, try to change the **Font** or leave the field empty.
- _**DeepL** or **Google** not available in my country, what should I do?_
	- I've pointed out in the **Guide**, you should ask your friends for help.
- _What happens when **API Quota** is all used?_
	- Nothing horrible if you limit your **Quota**, but you have to wait till the ending of the month.
- _I've put everything in the config, but it's translating to **English**._
	- You've selected the wrong backend in the config, **Generic** backend for debugging purposes only.
- _Can I change where the text boxes appear?_
	- Just like any other elements, hold **Alt** key and drag.
- _Can you add an **Interface** translation? Do you have a plan to make an **Unofficial** translation?_
	- This plugin is designed to be a quick-and-easy **Framework** and **Helper** for automatic translations that still require basic English knowledge, not a translation project.
- _Please, add a different translation backend, like **ChatGPT**._
	- I've tried my best to make the code expandable, feel free to **Contribute**.
- _Some books, text or scrolls are not translating, can you enable them?_
	- The **UI** in this game is very unpredictable, you can translate them manually via `Select Text` context menu option.
- _How to **Export** or **Transfer** my saved translations? Can I share them with my friends?_
	- Absolutely, you have to copy the mentioned **H2 SQL** file to the same directory on another computer.
- _Why does it look so ugly in the **Preview**? Can I change that?_
	- This is an old screenshot for nostalgic purposes, a lot of tweaks available in the config.

> [!IMPORTANT]
> If you've encountered any **Bugs** or need **Help** – please, join our **[Discord](https://furfy.github.io/invite)** server!

[![](https://user-images.githubusercontent.com/13049652/161437194-fca3d9c0-7226-40ed-9403-b4c01393f1af.png)](../..)

[![DigitalOcean Referral Badge](https://web-platforms.sfo2.digitaloceanspaces.com/WWW/Badge%203.svg)](https://www.digitalocean.com/?refcode=71af1247dfc7&utm_campaign=Referral_Invite&utm_medium=Referral_Program&utm_source=badge)

*Люблю Дэйвика.. 💕*<br>
*Благодарности Эвилу за помощь! ✨*<br>
