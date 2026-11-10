// SPDX-License-Identifier: AGPL-3.0-only
import 'package:flutter/services.dart';

/// Reference MethodChannel bridge. The native Flutter plugin implementation is
/// not built in this repository; verify it in a real Flutter host before any
/// support claim.
class ReproParcelFlutter {
  static const MethodChannel _channel = MethodChannel('reproparcel');

  static Future<bool> initialize(Map<String, Object?> config) async {
    return await _channel.invokeMethod<bool>('initialize', config) ?? false;
  }

  static Future<bool> setEnabled(bool enabled) async {
    return await _channel.invokeMethod<bool>('setEnabled', enabled) ?? false;
  }

  static Future<bool> showReporter(String surfaceId) async {
    return await _channel.invokeMethod<bool>('showReporter', surfaceId) ?? false;
  }

  static Future<bool> recordBreadcrumb(Map<String, Object?> breadcrumb) async {
    return await _channel.invokeMethod<bool>('recordBreadcrumb', breadcrumb) ?? false;
  }

  static Future<bool> deleteLocalReports() async {
    return await _channel.invokeMethod<bool>('deleteLocalReports') ?? false;
  }
}
