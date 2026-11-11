// SPDX-License-Identifier: AGPL-3.0-only
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

export type ReporterResult =
  | "success"
  | "not_initialized"
  | "disabled"
  | "host_unavailable"
  | "wrong_thread"
  | "reporter_busy"
  | "unsupported"
  | "failure";

export function initialize(config: ReproParcelConfig): Promise<boolean>;
export function setEnabled(enabled: boolean): Promise<boolean>;
export function showReporter(surfaceId: string): Promise<ReporterResult>;
export function recordBreadcrumb(breadcrumb: Breadcrumb): Promise<boolean>;
export function recordLog(event: {
  level: string;
  code: string;
  attributes?: Record<string, string>;
}): Promise<boolean>;
export function deleteLocalReports(): Promise<boolean>;
