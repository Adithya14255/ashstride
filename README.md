# Ashstride - building and installing

## Get the APK (no Android Studio needed)
1. Create a free GitHub account and a new **private** repository.
2. Upload everything in this folder to it (the hidden `.github` folder must come along). Easiest: GitHub Desktop, or `git init && git add . && git commit -m init && git push`.
3. Open the repo's **Actions** tab, pick "Build APK", press **Run workflow** (it also runs on every push).
4. When it finishes (about 5-8 minutes), open the run and download the **ashstride-apk** artifact. Unzip it to get `app-debug.apk`.
5. Send the APK to your OnePlus 12 and open it. Allow "Install unknown apps" for the app you opened it from (Files, Chrome, etc.) when asked. Tap through any Play Protect prompt.

Every build uses the same signing key, so new APKs install over the old one and keep your data.

## Or build in Android Studio
Open this folder, let it sync (JDK 17), run the `app` configuration.

## If the build fails
Open the failed run, copy the red error lines, and paste them into the next session.
