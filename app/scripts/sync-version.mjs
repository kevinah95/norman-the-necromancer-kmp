#!/usr/bin/env node

import { readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

const [rawVersion] = process.argv.slice(2);

if (!rawVersion) {
  console.error("Usage: node sync-version.mjs <version>");
  process.exit(1);
}

// Strip leading 'v' if provided and validate SemVer format
const version = rawVersion.replace(/^v/, "");
const match = version.match(/^(\d+)\.(\d+)\.(\d+)$/);

if (!match) {
  console.error(`Invalid version "${rawVersion}". Expected format: X.Y.Z (e.g. 1.0.0)`);
  process.exit(1);
}

const [, major, minor, patch] = match.map(Number);

// Standard monotonic versionCode for Android & iOS:
// Major (up to 210), Minor (up to 99), Patch (up to 99)
const versionCode = major * 10_000 + minor * 100 + patch;

const appDir = fileURLToPath(new URL("..", import.meta.url));

function updateProperties(filePath, updates) {
  let content = readFileSync(filePath, "utf8");

  for (const [key, value] of Object.entries(updates)) {
    const escapedKey = key.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    const pattern = new RegExp(`^${escapedKey}=.*$`, "m");

    if (pattern.test(content)) {
      content = content.replace(pattern, `${key}=${value}`);
    } else {
      content = `${content.trimEnd()}\n${key}=${value}\n`;
    }
  }

  writeFileSync(filePath, content);
}

// 1. Update Gradle properties (used by Android & Desktop)
updateProperties(resolve(appDir, "gradle.properties"), {
  "app.versionName": version,
  "app.versionCode": String(versionCode),
});

// 2. Update iOS build configuration
updateProperties(resolve(appDir, "iosApp/Configuration/Config.xcconfig"), {
  MARKETING_VERSION: version,
  CURRENT_PROJECT_VERSION: String(versionCode),
});

// 3. Update Kotlin GameVersion.kt (accessible across all platforms)
const gameVersionPath = resolve(
  appDir,
  "shared/src/commonMain/kotlin/io/github/kevinah95/norman_the_necromancer/GameVersion.kt",
);
const gameVersionContent = `package io.github.kevinah95.norman_the_necromancer

object GameVersion {
    const val VERSION_NAME: String = "${version}"
    const val VERSION_CODE: Int = ${versionCode}
}
`;
writeFileSync(gameVersionPath, gameVersionContent);

console.log(`Synced version to ${version} (versionCode: ${versionCode})`);
