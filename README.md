# WalkUnlock

### Live a more active life

Lots of people (me included) spend too long browsing social media and not enough time outside.
WalkUnlock aims to combat that by requireing a step "cost" to using applications.

This is both an incentive to **walk more** and to spend **less time mindlessly scrolling** though social media.

## Features

WalkUnlock consists of a step counter that deducts steps every minute an app is open in the foreground based on the number of steps the user configures it to cost.
An app can be configured to not cost any steps although any app not on the list will also have no step cost.

Light and Dark theme are supported.

## Installation
### Android

Download the .apk file from the [GitHub Releases](https://github.com/NathanPrazeres/WalkUnlock/releases/latest) tab and install it like any other Android application.

To enable the ```ForegroundAppService``` (which is required for seeing if a locked app is open), Accessibility permission is required. Since WalkUnlock is installed through an APK, the option will probably be greyed out.
To enable the ability to grant the permission, please go to **Settings > Apps > WalkUnlock >** *3 dots menu* and tap **Allow restricted settings**.

Go back to WalkUnlock and the pop-up asking for Accessibility Permission should show up again. Follow it as normal.
 
## Development

This application was developed for the Mobile App Development 2024/25 Spring Semester course at AGH University in Kraków, Poland

---

###### Known issues:

- Sometimes launching an app will count as 2 instances causing the locked app screen to show up twice in a row.
- Settings menu is a bit bare bones.
