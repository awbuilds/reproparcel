// SPDX-License-Identifier: AGPL-3.0-only
import { NativeModules, Platform } from "react-native";

export type CapturePolicy = {
  policyVersion: string;
  screenshotEnabled: boolean;
  semanticTextEnabled: boolean;
  eligibleSurfaces: string[];
};

export type ReproParcelConfig = {
  enabled: boolean;
  packageName: string;
  versionName: string;
  versionCode: number;
  variant: string;
  gitCommit?: string | null;
  capturePolicy?: CapturePolicy;
};

export type Breadcrumb = {
  category: string;
  actionId?: string;
  screenId?: string;
  attributes?: Record<string, string>;
  outcome?: string;
};

type NativeReproParcel = {
  initialize(configJson: string): Promise<boolean>;
  setEnabled(enabled: boolean): Promise<boolean>;
  showReporter(surfaceId: string): Promise<ReporterResult>;
  recordBreadcrumb(json: string): Promise<boolean>;
  recordLog(json: string): Promise<boolean>;
  deleteLocalReports(): Promise<boolean>;
};

function native(): NativeReproParcel {
  if (Platform.OS !== "android") throw new Error("ReproParcel React Native reference supports Android only");
  const module = NativeModules.ReproParcel as NativeReproParcel | undefined;
  if (!module) throw new Error("ReproParcel native module is not linked");
  return module;
}

export function initialize(config: ReproParcelConfig): Promise<boolean> {
  return native().initialize(JSON.stringify(config));
}

export function setEnabled(enabled: boolean): Promise<boolean> {
  return native().setEnabled(enabled);
}

export type ReporterResult =
  | "success"
  | "not_initialized"
  | "disabled"
  | "host_unavailable"
  | "wrong_thread"
  | "reporter_busy"
  | "unsupported"
  | "failure";

export function showReporter(surfaceId: string): Promise<ReporterResult> {
  return native().showReporter(surfaceId) as Promise<ReporterResult>;
}

export function recordBreadcrumb(breadcrumb: Breadcrumb): Promise<boolean> {
  return native().recordBreadcrumb(JSON.stringify(breadcrumb));
}

export function recordLog(event: { level: string; code: string; attributes?: Record<string, string> }): Promise<boolean> {
  return native().recordLog(JSON.stringify(event));
}

export function deleteLocalReports(): Promise<boolean> {
  return native().deleteLocalReports();
}
