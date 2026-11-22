# KnightMarket (Android, Java + XML)

Native Android version of KnightMarket, the UCF student marketplace, for CEN 4360.

## Getting started
1. In Android Studio: **File > New > Project from Version Control**, paste this repo's URL.
2. Let Gradle sync, then run the `app` configuration on an emulator.

## Branches
- `main` always builds. Don't commit to it directly.
- Make your own branch for your screens (for example `jair-screens`), push it, and open a pull request into `main`.
- Pull `main` into your branch often. `colors.xml`, `themes.xml` and `strings.xml` are shared, so that's where conflicts happen.

## Backend
The app uses the KnightMarket API, the same server and database as the web app (knightmarket.gaspardantas.com):
`https://knightmarket.onrender.com/api` (health check: `/health`). It's on Render's free tier, so the first request
after a quiet spell can take up to a minute.

- Every response is `{ success, message, data }`. Errors are `{ success: false, message, errors? }`.
- Logged-in calls send the token from `Session.token(context)` as `Authorization: Bearer <token>`.
- Demo accounts (already verified): `clarkkent@ucf.edu`, `brucewayne@ucf.edu`, `dianaprince@ucf.edu`,
  `peterparker@ucf.edu`, `natasharomanoff@ucf.edu`. Ask Gaspar for the shared password.

### How the code talks to it
- `Api.java`: one static method per endpoint. Calls run in the background and the `Callback` comes back on the main
  thread with `data` (a `JSONObject`) or an error message ready to show.
- `Session.java`: the logged-in user's token, name and email. `ImageLoader.java`: listing photos from Cloudinary.
- To add an endpoint: copy a method in `Api.java`, change the method and path, and call it from your screen:
  ```java
  Api.myListings(Session.token(this), new Api.Callback() {
      @Override public void onSuccess(JSONObject data, String message) { /* data.optJSONArray("listings") */ }
      @Override public void onError(int status, String message) { Toast.makeText(...).show(); }
  });
  ```

### Done
Landing, Log in (+ Forgot password), Create account, Verify email (link), Profile photo (skip),
Shopping (live feed, categories, search), **Listing detail** (photos, save, directions, owner's Change Status),
**Filters** (price, condition, sort), **Saved**, **My Listings** (change status, delete),
**Profile** (edit name, Contact Admin, log out). Shared helpers: `BottomNav`, `ListingStatus`, `ListingFilters`,
`SystemBars` (keeps screens clear of the status bar on Android 15+).

## Still to build
These four match features of the KnightMarket Flutter and web apps. Everything they need is already on the server.

### 1. Create and edit a listing (Sell tab, + in My Listings, Edit)
- Screen: `activity_create_listing.xml` (layout only). Edit can reuse it, filled in from `GET /listings/:id`.
- Create: `POST /listings`, edit: `PUT /listings/:id`. Both are **multipart/form-data**:
  `title` (max 100), `description`, `price` (≥ 0), `category`, `condition`, `meetupArea` (required, e.g. "Student Union"),
  and up to 10 files in the field `images` (the apps use 3).
  - Categories: `Textbooks`, `Electronics`, `Furniture`, `Clothing`, `Dorm & Home`, `Tickets & Events`, `Vehicles`, `Other`.
  - Conditions: `New`, `Like New`, `Good`, `Fair`, `Poor`.
- Photos: pick them with the Photo Picker (`ActivityResultContracts.PickMultipleVisualMedia`). `Api.java` only sends JSON,
  so add a multipart request (or use OkHttp's `MultipartBody`).
- Remove one photo while editing: `DELETE /listings/:id/image`.
- Then: open the new listing with `ListingDetailActivity.open(this, id)`, and replace the "Coming Soon" toasts in
  `BottomNav` (Sell) and `MyListingsActivity` (+).

### 2. Messages and chat (Chats tab, "Message Seller" on a listing)
- Screens: `activity_messages.xml` and `activity_chat_thread.xml` (layout only).
- `GET /threads`: your conversations. `GET /threads/unread-count`: the badge on the Chats tab.
- `POST /threads` with `{ listingId }`: starts (or reopens) the chat with that listing's seller, for **Message Seller**
  in `ListingDetailActivity`.
- `GET /threads/:id/messages` and `POST /threads/:id/messages` with `{ body }`.
- `PATCH /threads/:id/block`, `PATCH /threads/:id/delete`.
- Live updates: the server uses Socket.IO (events `newMessage`, `unreadCountUpdate`, `meetupUpdate`; join a thread with
  `joinThread`). Simplest option: refresh the open chat every few seconds. Full option: the `io.socket:socket.io-client`
  library with the token.
- Contact Admin (Profile) already creates the admin chat, so it will show up in this list.

### 3. Arrange a meetup and rate (inside a chat)
- Screen: `activity_arrange_meetup.xml` (layout only), opened from the chat's ⋮ menu.
- Propose: `PATCH /threads/:id/meetup` with `{ address, lat, lng, scheduledAt }` (the spinner's campus spots need
  their coordinates).
- Answer: `PATCH /threads/:id/meetup/respond` with `{ action: "confirm" }` or `{ action: "cancel" }`.
- After the exchange: `PATCH /threads/:id/meetup/complete`, then each side rates with `POST /threads/:id/rate`
  `{ score: 1-5, feedback }`. Ratings show on Profile and Listing detail already.
- A user's ratings: `GET /ratings/user/:id`.

### 4. Map
- Screen: `activity_map.xml` (layout only).
- `GET /listings/map` (optional `category`) returns `data.pins` with `_id`, `title`, `price`, `coordinates`
  (`[lng, lat]`), `images`, `category`. Tap a pin: `ListingDetailActivity.open(this, pin._id)`.
- Map library: osmdroid (OpenStreetMap, no API key, like the Flutter app) or Google Maps (needs a key).
