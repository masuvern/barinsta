## WARNING

* All forks must respect [GPLv3](https://www.gnu.org/licenses/gpl-3.0.html). Please report violations in Issues or [confidentially](https://austinhuang.me/#hey-you-look-cool).
* Although publishing a fork is allowed by license, it is strongly discouraged to do so as it divides the effort and creates confusion. It is, therefore, recommended to send a pull request back to us, so that the larger community can enjoy your improvement. (This does not apply if you're adapting this app for a different platform other than Instagram.)

## Contributing

Thank you for your interest in InstaGrabber!

Our vision of InstaGrabber is an open source true alternative of the official Instagram app. It is Austin's pursuit of a libre life that lead him to this app during its abandonment, and it was one unresolved bug that made him have the enthusiasm in implementing numerous features for this app, despite having 0 knowledge of Java beforehand.

As we grow in popularity, it becomes apparent that we are short on hands. Every contribution counts!

## I want to help coding it!

Great!

Generally, we want to imitate features in the actual Instagram app. There are many Instagram private API repositories on GitHub for you to refer to. Note that you should minimize POST: If a job should be done with GET, then there has to be a GET endpoint. (Indeed, sometimes you need multiple repositories for reference.)

As long as you have tested your version (please indicate device and API version) and make sure it works, then you can submit a PR! Large UI changes have to be voted on by the community, so it would be helpful to upload some screenshots.

Check errors are for reference only. Try to minimize them, but usually they don't make a big difference.

**NEVER touch the l10n-master branch.** It's automatically managed by Crowdin.

### Structure

It is preferred that you read the scripts yourself, as my understanding (and presentation) may be basic.

* `awais.instagrabber`
  * `activities`: Scripts directly binding to each view. Assigns `adapters`.
  * `adapters`: Scripts used to present a list of `models` from `asyncs` into `activities`.
    * Those inside `viewholder` are for frontend, while others are for backend.
  * `asyncs`: Scripts used to communicate with Instagram. Returns `models` which is sent to `adapters`.
    * `asyncs.i`: Scripts that fetch data from `i.instagram.com`, except DM.
    * `asyncs.direct_messages`: As the name suggests, communication scripts used for DM.
  * `customviews`: Custom frontend components for this app.
  * `dialogs`: Scripts directly binding to dialogs (i.e. those that are nested in the main view).
  * `fragments.directmessages`: Scripts directly binding to each fragment ("small views") within `DirectMessageActivity` in `activities`.
  * `interfaces`: Custom backend components for this app.
  * `models`: Data structure for Instagram API responses from `asyncs`.
  * `utils`: Various tools.
  * `MainHelper.java` is basically an extension of `activities.main`.
* `awaisomereport`: Crash reporter. Shouldn not require too much maintenance.
* `thoughtbot.expandableadapter`: These are for the follower comparison view, which allows grouping users.

### I can't code Java, but I want to!

Fun fact: Austin took over this project and learned Java on the fly (I'm not joking, I only do JavaScript before taking this over).

Even though Java is quite annoying, it is still possible to learn it by trying to understand what these code do (Easier if you have coding experience in other languages).

If you have questions, don't be afraid to ask for help from any current maintainer!

## I found a bug!

**Please read [FAQ](https://instagrabber.austinhuang.me/faq) first.**

Bugs are inevitable during active development, as nobody can cover all the possible test cases. 

You can either email your crash dump to `instagrabber@austinhuang.me` (The crash reporter will fill in this address for you) or create a GitHub issue. If you're on GitHub, please follow the template. If you're reporting by email, your email address will be published in the GitHub issue. You can contact me [privately](https://austinhuang.me/#hey-you-look-cool) or [through support channels](https://instagrabber.austinhuang.me/#what-can-i-do) to remove it.

Generally, reporting bugs directly in support channels is not recommended, as they can be difficult to find.

### I want to help... in some other way.

You can...

* translate it [![badge](https://badges.crowdin.net/instagrabber/localized.svg)](https://crowdin.com/project/instagrabber)
* promote it (reddit [r/AndroidApps](https://www.reddit.com/r/androidapps/comments/i30tpp/instagrabber_an_open_source_instagram_client/), YouTube [HowToMen](https://www.youtube.com/watch?v=1Um2ZJG_mB4), [Ekşi Sözlük](https://eksisozluk.com/instagrabber--6643143))
* star it [![stars](https://img.shields.io/github/stars/austinhuang0131/instagrabber.svg?style=social&label=Star)](https://github.com/austinhuang0131/instagrabber/stargazers)

Happy contributing!
