// SPDX-License-Identifier: AGPL-3.0-only
import 'package:flutter/services.dart';

/// MethodChannel bridge to the Android Flutter plugin in `android/`. The host
/// integration test in `flutter/host_app/integration_test` verifies initialize,
/// enable, breadcrumb, delete and reporter launch.
class ReproParcelFlutter {
  static const MethodChannel _channel = MethodChannel('reproparcel');

  static Future<bool> initialize(Map<String, Object?> config) async {
    return await _channel.invokeMethod<bool>('initialize', config) ?? false;
  }

  static Future<bool> setEnabled(bool enabled) async {
    return await _channel.invokeMethod<bool>('setEnabled', enabled) ?? false;
  }

  /// Resolves to `success` or a typed result code (`disabled`,
  /// `not_initialized`, `host_unavailable`, `failure`, ...).
  static Future<String> showReporter(String surfaceId) async {
    return await _channel.invokeMethod<String>('showReporter', surfaceId) ?? 'failure';
  }

  static Future<bool> recordBreadcrumb(Map<String, Object?> breadcrumb) async {
    return await _channel.invokeMethod<bool>('recordBreadcrumb', breadcrumb) ?? false;
  }

  static Future<bool> deleteLocalReports() async {
    return await _channel.invokeMethod<bool>('deleteLocalReports') ?? false;
  }
}
