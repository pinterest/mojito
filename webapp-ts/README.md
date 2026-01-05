# Project Name

This is the project for modernizing the existing UI. The plan is to migrate the features over to this application over time.

### Prerequisites

1. Build the project once using `mvn`. This will download the necessary node files for you if you are not already using NPM
2. Run `source use_local_npm.sh` or ensure you have Node installed (version 22) globally

## Installation

```bash
npm install
```

## Usage

```bash
npm run dev
```

### Build / Deployment Details

Build details as defined in [webapp's pom.xml](../webapp/pom.xml) under the `frontend-ts` profile
1. Install Node and all NPM packages
2. Build application. Artifacts generated in `./dist`
3. Copy index.html to [template directory](../webapp/src/main/resources/templates/). This is done so that it can be found by the Mustache processor, to inject the necessary CSRF token and Application properties into the JS
4. The remaining generated assets (JS and CSS) are copied to the [static assets](../webapp/src/main/resources/templates/)
5. Specific routes configured in [ReactAppController](../webapp/src/main/java/com/box/l10n/mojito/react/ReactAppController.java) will redirect to the index.html artifact of this build


### Development

#### Assets

Use **inline assets** if assets are necessary:

```js
import reactLogo from './assets/react.svg?inline'
```

This bloats the JS slightly but ultimately simplifies the build process. We do not need to hardcode assets to be copied across nor do we need to rewrite import paths. Switching to static assets can be done once all the features of the webapp are migrated across
