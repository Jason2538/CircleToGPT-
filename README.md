# Circle to Ask / CircleToGPT

Android (Galaxy Tab / S Pen) companion app that offers a floating overlay button, user-approved screen capture, freeform region selection and sharing a screenshot with ChatGPT. This is **not** an official ChatGPT system integration.

## Build APK

The **Actions → Build CircleToGPT APK** workflow automatically builds a debug-signed installable APK for each push to main. Open a successful run, then download the `CircleToGPT-debug-apk` artifact ZIP, extract `app-debug.apk`, and install it on your Android tablet.

## How to use

Install app → grant "Display over other apps" → turn on floating button → open another app → tap floating button → approve screen capture → circle content → enter a question → send to ChatGPT.

User consent is needed for each capture on Android 14+. Secure/protected content can't be captured. The Android image sharing target can vary by ChatGPT version; the question text is not guaranteed to be prefilled. Floating overlay and background activity behavior can vary across Android/One UI versions.

This app does not require network permissions, Accessibility access, an API key, or a private server. It is an experimental build, **not yet tested on a physical Galaxy Tab**.
