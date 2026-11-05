# LifeOS — Your Everyday Hub

LifeOS is a mobile-first, installable Progressive Web App (PWA) starter for everyday productivity. It is configured for this GitHub Pages project site: `https://mahdimokhtari39161-svg.github.io/lifeos-app/`.

## Included in this first build

- Persian, English, and Arabic interface with RTL/LTR layout switching
- Responsive mobile dashboard and bottom navigation
- Task list with completion and filters
- Notes with local create/delete
- Income and expense tracking with selectable currency
- Habit tracker with daily completion
- Calculator, unit converter, and percentage calculator
- Dark/light theme
- Local JSON backup export and restore
- PWA manifest, app icons, and a service worker for offline app-shell caching

## Publish with GitHub Pages

1. Use the existing repository `mahdimokhtari39161-svg/lifeos-app`. Keep your other projects untouched.
2. Upload the contents of the extracted project folder to the repository root. Make sure `index.html`, `app.js`, `styles.css`, `manifest.webmanifest`, and `sw.js` are at the root (not inside another nested folder).
3. Commit the files to the `main` branch.
4. Open **Settings → Pages**.
5. Under **Build and deployment**, choose **Deploy from a branch**.
6. Select branch **main** and folder **/(root)**, then click **Save**.
7. Wait for the Pages deployment to complete. The site address will be `https://mahdimokhtari39161-svg.github.io/lifeos-app/`.

For the current GitHub Pages instructions, see: https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site

## Install on a phone

- **Android / Chrome:** open the published URL, then use the browser menu and choose **Install app** or **Add to Home screen**.
- **iPhone / Safari:** open the URL, tap **Share**, then **Add to Home Screen**.
- The app shell is cached for offline opening after the first successful visit. Features that require a live internet connection are not part of this prototype yet.

## Important limitations of this prototype

- User-created tasks, notes, expenses, and habits are stored in that browser on that device. They are not synchronized to another device and may be lost if browser storage is cleared. Export a backup regularly.
- There is no real AI backend, login system, cloud database, payment processing, or subscription enforcement yet. Pro is a preview screen only.
- Do not store passwords, payment card details, identity documents, or other highly sensitive information in this prototype.
- Do not place API keys, passwords, or private credentials in this repository or in browser-side JavaScript. A future AI/payment/cloud implementation must use a secure server-side backend.

## Development

No build tools or package installation are required. Edit the HTML, CSS, and JavaScript directly, then commit to `main`. GitHub Pages republishes the static files.
