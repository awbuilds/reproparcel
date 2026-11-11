// SPDX-License-Identifier: AGPL-3.0-only
const { NativeModules, Platform } = require("react-native");

function native() {
  if (Platform.OS !== "android") throw new Error("ReproParcel React Native reference supports Android only");
  const module = NativeModules.ReproParcel;
  if (!module) throw new Error("ReproParcel native module is not linked");
  return module;
}

function initialize(config) {
  return native().initialize(JSON.stringify(config));
}

function setEnabled(enabled) {
  return native().setEnabled(enabled);
}

function showReporter(surfaceId) {
  // Resolves to "success" or a typed bridge result code.
  return native().showReporter(surfaceId);
}

function recordBreadcrumb(breadcrumb) {
  return native().recordBreadcrumb(JSON.stringify(breadcrumb));
}

function recordLog(event) {
  return native().recordLog(JSON.stringify(event));
}

function deleteLocalReports() {
  return native().deleteLocalReports();
}

module.exports = {
  initialize,
  setEnabled,
  showReporter,
  recordBreadcrumb,
  recordLog,
  deleteLocalReports,
};
