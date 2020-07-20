<img src="./app/play_icon.png" alt="InstaGrabber" align="right" width="25%"/>

## InstaGrabber

InstaGrabber is an app that allows ~~stealing~~ downloading Instagram posts, stories, and DM. It can be used as a drop-in replacement for read functionalities of the Instagram app.

This app is originally made by [@AwaisKing](https://github.com/AwaisKing) who posted on [GitLab](https://gitlab.com/AwaisKing/instagrabber). I decided to continue the app cuz why not, ~~even though it might not be that *cash money*.~~ (Also I need to learn Java.)

### Download

Download [here](https://github.com/austinhuang0131/instagrabber/releases). Under each release is an `app-release.apk`. Just install that. Unfortunately, I have to sign it with my own key, which means you have to uninstall the old app and log in again. Sorry.

### How to log in

By logging in, you can access posts/stories from private accounts, as well as your direct messages.

The relevant source code is [here](https://github.com/austinhuang0131/instagrabber/blob/master/app/src/main/java/awais/instagrabber/activities/Login.java) should you be concerned.

1. Click the 3 dots on the bottom right corner.
2. Click "Settings".
3. Scroll down, click the green button called "Login".
4. (Recommended, not required) Enable desktop mode if you have 2FA.
5. Login as usual. Use "Refresh" when needed.
6. When you see your feed page, click "Get cookies".
7. Test it by checking your direct messages. If it loads, it works!

### Legal

* We do not collect any data, other than crash log, which is only stored locally. No funky stuff.
* You can voluntarily provide us with the crash log (3 dots => Settings => "Send logs"). In that case, it is your sole responsibility to remove any sensitive information. As well, you agree to the privacy policy of the platform that you send it on.
* **You are solely responsible for the integrity of your account.** While much effort is made to imitate the actual Instagram app, no one can make any guarantee that you can't get banned over this (blame Zucc).
