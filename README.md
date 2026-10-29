# Circle to Ask (v0.3)

Circle to Ask is an experimental Android/S Pen screenshot-selection companion app. Its shortcuts include a Quick Settings (notification shade) tile, a Samsung Side Button double-press app launch, an optional accessibility button, and the floating bubble.

## Quick Settings tile

- Open Circle to Ask and allow **Display over other apps**.
- Open **Quick Settings** by swiping down twice, then tap **Edit** (pencil / plus / edit buttons). Drag **Circle to Ask** into active tiles.
- On Android 13+, you can instead tap **빠른 설정 버튼 추가하기** in the app and accept the system add-tile prompt.
- Tap the Circle to Ask tile from any ordinary app. It launches **only the translucent Android capture-permission host**, not the app's settings screen. After you approve the Android screen capture, a full-screen **overlay selection surface** appears above the original app, where you can circle the area with the S Pen and share it to ChatGPT.
- The system must still ask for screen-capture consent for each session. This permission cannot be silently bypassed. Tile use while locked prompts unlock.

## Side key

Open the app, enable quick start, then assign the app through Samsung Settings → Advanced features → Side button → Double press → Open app → Circle to Ask. Menu labels may vary. The quick-launch activity uses a transparent launch theme to reduce app transitions.

## Accessibility shortcut

Settings → Accessibility → Installed apps → Circle to Ask shortcut. This optional accessibility service does not retrieve window contents or monitor interaction events. 

## Developer / APK

GitHub Actions compiles the Android Java project via Gradle; download `CircleToGPT-debug-apk` from the successful run's artifacts and extract the APK. Android app version v0.3 / versionCode 3.

## Privacy and limitations

Screenshot content is shared only when the user taps "ChatGPT에 보내기". Local screenshot cache is transient. No network permission is requested. Android's protected/secure content cannot be captured. Android overlays and background launching behavior may differ across One UI versions; the real Galaxy Tab experience has not yet been verified. This isn't an official Google Circle to Search integration. Text in Android's image-sharing intent may not automatically populate the ChatGPT composer.

Debug APKs built on different GitHub Actions machines may use different signing keys. Uninstall the older debug APK before installing this version if Android reports an incompatible signature.
