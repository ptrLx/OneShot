.DEFAULT_GOAL := help
GRADLE := ./gradlew --no-daemon
REPO ?= ptrLxAI/OneShot
BRANCH ?= $(shell git rev-parse --abbrev-ref HEAD)
APK_DIR := build/ci-apks

.PHONY: help
help:  ## Display this help text
	$(info OneShot)
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make \033[36m<target>\033[0m\n"} /^[a-zA-Z0-9_-]+:.*?##/ { printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

##@ Local build (needs a JDK and the Android SDK)

.PHONY: build test release-unsigned clean
build:  ## Build the debug APK
	$(GRADLE) assembleDebug

test:  ## Run unit tests
	$(GRADLE) testDebugUnitTest

release-unsigned:  ## Build the unsigned release APK (what F-Droid builds)
	$(GRADLE) assembleRelease

clean:  ## Remove build outputs
	$(GRADLE) clean
	rm -rf $(APK_DIR)

##@ Checks (no Android SDK needed)

.PHONY: check-fdroid check-version
check-fdroid:  ## Check what the F-Droid recipe relies on (versions, changelog, deps)
	scripts/ci/check-fdroid-metadata.sh

check-version:  ## Check versionCode and fastlane changelog against versionName (from the first release-please release on)
	scripts/release/sync_version.py --check

##@ CI as build machine (needs the gh CLI)

.PHONY: ci-run ci-status ci-apk
ci-run:  ## Trigger the CI workflow for the current branch
	gh workflow run ci.yml -R $(REPO) --ref $(BRANCH)

ci-status:  ## Show the latest CI runs of the current branch
	gh run list -R $(REPO) --branch $(BRANCH) --limit 5

ci-apk:  ## Download the APKs of the latest successful CI run of the current branch into build/ci-apks
	rm -rf $(APK_DIR)
	gh run download -R $(REPO) -n apks -D $(APK_DIR) \
	  $$(gh run list -R $(REPO) --workflow ci.yml --branch $(BRANCH) --status success --limit 1 --json databaseId --jq '.[0].databaseId')
	@find $(APK_DIR) -name '*.apk'

##@ Releases (needs the gh CLI and the `release` environment)

REF ?= master
IMAGE ?= debian:trixie
JDK ?= 21

.PHONY: release-rehearsal release-rehearsal-apk
release-rehearsal:  ## Build and sign REF with the release key without publishing (approve the run in the Actions tab)
	gh workflow run sign-release.yml -R $(REPO) --ref master -f ref=$(REF) -f image=$(IMAGE) -f jdk=$(JDK)

release-rehearsal-apk:  ## Download the signed OneShot.apk of the latest successful rehearsal into build/release
	rm -rf build/release
	gh run download -R $(REPO) -n OneShot-release -D build/release \
	  $$(gh run list -R $(REPO) --workflow sign-release.yml --status success --limit 1 --json databaseId --jq '.[0].databaseId')
	@ls -l build/release/OneShot.apk

##@ Branding (needs Docker)

.PHONY: logo
logo:  ## Regenerate logo v2, launcher icons and the store icon (node:22-alpine + rsvg-convert)
	docker run --rm -v "$(CURDIR)":/src -w /src node:22-alpine \
	  sh -c 'scripts/logo/render.sh && chown -R $(shell id -u):$(shell id -g) logo app/src/main/res fastlane/metadata/android/en-US/images'
