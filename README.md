# Circle to Ask / CircleToGPT (v0.2)

Android (Galaxy Tab / S Pen) companion app: floating overlay button, Samsung Side button double press via system settings, optional accessibility button, and user-approved screenshot selection.

## APK build

GitHub Actions `Build CircleToGPT APK` builds `app/build/outputs/apk/debug/app-debug.apk` and stores it under `CircleToGPT-debug-apk` as a zipped artifact.

## Samsung Side button double-press (no Accessibility permission required)

1. Open Circle to Ask and enable **측면 버튼/앱 아이콘 바로 실행**.
2. Open Samsung **Settings → Advanced features (유용한 기능) → Side button/Side key (측면 버튼) → Double press (두 번 누르기) → Open app (앱 열기)**.
3. Select **Circle to Ask**.
4. Now, pressing the side button twice starts the Android screenshot permission prompt immediately. Approve capture, circle any region with S Pen, and send to ChatGPT.

The system controls the side button, **not** our Accessibility Service. There is no power-button interception or bypass of OS protections. Samsung menu and options vary by model and One UI version. If Side button remapping is not offered, use the accessibility button or floating button instead.

## Optional Accessibility shortcut

Settings → Accessibility → Installed apps → Circle to Ask 바로가기 → enable and configure the accessibility button shortcut. The service has `canRetrieveWindowContent=false`, does not process window events, does not log keys or inspect the screen, and launches the same opt-in screenshot consent flow only when invoked. It is optional and never required for Side button shortcuts.

## Opening app settings

Long-press the Circle to Ask home-screen icon → App settings shortcut, or turn off quick start before leaving the app. Home-screen app icon launches straight into capture while quick start is enabled.

## Floating button

Permit "Display over other apps" → enable floating button → tap it from another app → approve capture → circle content → send to ChatGPT.

**Privacy & limitations:** Screen capture always requires Android user consent on modern Android. Secure/protected windows cannot be captured. The captured image is stored temporarily in app cache and only shared when you choose. App does not request internet access. ChatGPT Android sharing may not prefill question text. Not an official Google Circle to Search / ChatGPT integration.

**APK update note:** GitHub-hosted debug builds may use different signing keys on each workflow run. If Android reports an incompatible-signature install error, uninstall the previous debug version before installing the new one; app settings will reset. This is an experimental build and is not yet tested on a physical Galaxy Tab.
