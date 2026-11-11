// SPDX-License-Identifier: Apache-2.0
library reproparcel_flutter;

class ReproParcelLimits {
  static const int maxEvents = 50;
  static const int maxLogs = 200;
  static const int maxLogBytes = 256 * 1024;
  static const int maxSemanticNodes = 500;
  static const int maxSemanticDepth = 32;
  static const int maxSemanticBytes = 512 * 1024;
  static const int maxImageLongEdge = 1600;
  static const int maxDecodedImageBytes = 12 * 1024 * 1024;
  static const int maxConcurrentDecodedImageBytes = 24 * 1024 * 1024;
  static const int maxParcelCompressedBytes = 5 * 1024 * 1024;
  static const int maxParcelUncompressedBytes = 10 * 1024 * 1024;
  static const int maxParcelEntries = 16;
  static const int maxCompressionRatio = 100;
  static const int maxJsonBytes = 2 * 1024 * 1024;
  static const int maxJsonDepth = 32;
  static const int maxJsonNodes = 10000;
  static const int maxDrafts = 10;
  static const int maxDraftBytes = 25 * 1024 * 1024;
}

class ImageDimensions {
  const ImageDimensions(this.width, this.height);
  final int width;
  final int height;
  int get longestEdge => width > height ? width : height;
  int get decodedBytes => width * height * 4;

  @override
  bool operator ==(Object other) =>
      other is ImageDimensions && other.width == width && other.height == height;

  @override
  int get hashCode => Object.hash(width, height);
}

class ImageBudget {
  static bool isWithinDecodedBudget(int width, int height) {
    if (width <= 0 || height <= 0) return false;
    final dimensions = ImageDimensions(width, height);
    return dimensions.longestEdge <= ReproParcelLimits.maxImageLongEdge * 4 &&
        dimensions.decodedBytes <= ReproParcelLimits.maxDecodedImageBytes;
  }

  static ImageDimensions scaledToLongestEdge(int width, int height) {
    final longest = width > height ? width : height;
    if (longest <= ReproParcelLimits.maxImageLongEdge) {
      return ImageDimensions(width, height);
    }
    final scale = ReproParcelLimits.maxImageLongEdge / longest;
    return ImageDimensions(
      (width * scale).clamp(1, ReproParcelLimits.maxImageLongEdge).toInt(),
      (height * scale).clamp(1, ReproParcelLimits.maxImageLongEdge).toInt(),
    );
  }
}

enum SafeAttribute {
  screenId('screen_id'),
  actionId('action_id'),
  routeTemplate('route_template'),
  httpMethod('http_method'),
  statusCode('status_code'),
  durationMs('duration_ms'),
  result('result'),
  category('category'),
  component('component'),
  errorCode('error_code'),
  orientation('orientation'),
  density('density'),
  fontScale('font_scale'),
  apiLevel('api_level'),
  manufacturer('manufacturer'),
  model('model'),
  appVersion('app_version'),
  variant('variant'),
  gitCommit('git_commit');

  const SafeAttribute(this.wireName);
  final String wireName;

  static SafeAttribute? fromWireName(String name) {
    for (final attribute in values) {
      if (attribute.wireName == name) return attribute;
    }
    return null;
  }
}

class Redactor {
  static final RegExp _email = RegExp(r'[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}');
  static final RegExp _bearer = RegExp(r'\b(bearer|basic)\s+[A-Za-z0-9._~+/=-]+', caseSensitive: false);
  static final RegExp _url = RegExp(r'(https?|ftp)://|\?|#', caseSensitive: false);
  static final RegExp _longDigits = RegExp(r'\d{7,}');
  static final RegExp _route = RegExp(r'^/[A-Za-z0-9_./{}:-]{0,120}$');

  static String? sanitize(SafeAttribute attribute, String value) {
    final trimmed = value.trim();
    if (trimmed.isEmpty || trimmed.length > 256) return null;
    if (_email.hasMatch(trimmed) || _bearer.hasMatch(trimmed) || _longDigits.hasMatch(trimmed)) {
      return null;
    }
    if (trimmed.contains('..')) return null;
    switch (attribute) {
      case SafeAttribute.routeTemplate:
        if (!_route.hasMatch(trimmed) || _url.hasMatch(trimmed)) return null;
        return trimmed;
      case SafeAttribute.httpMethod:
        const methods = {'GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'};
        final method = trimmed.toUpperCase();
        return methods.contains(method) ? method : null;
      case SafeAttribute.statusCode:
      case SafeAttribute.durationMs:
      case SafeAttribute.density:
      case SafeAttribute.apiLevel:
        return int.tryParse(trimmed) == null ? null : trimmed;
      case SafeAttribute.fontScale:
        final parsed = double.tryParse(trimmed);
        return parsed != null && parsed >= 0.5 && parsed <= 4.0 ? trimmed : null;
      case SafeAttribute.orientation:
        return const {'portrait', 'landscape', 'unknown'}.contains(trimmed) ? trimmed : null;
      case SafeAttribute.variant:
        return RegExp(r'^[A-Za-z0-9_.-]{1,32}$').hasMatch(trimmed) ? trimmed : null;
      case SafeAttribute.gitCommit:
        return RegExp(r'^[0-9a-f]{7,64}$').hasMatch(trimmed) ? trimmed : null;
      default:
        return _url.hasMatch(trimmed) || trimmed.length > 120 ? null : trimmed;
    }
  }

  static Map<String, String> sanitizeAttributes(Map<String, String> raw) {
    final result = <String, String>{};
    raw.forEach((key, value) {
      final attribute = SafeAttribute.fromWireName(key);
      if (attribute == null) return;
      final safe = sanitize(attribute, value);
      if (safe != null) result[key] = safe;
    });
    return result;
  }
}

enum SessionState {
  disabled,
  ready,
  capturing,
  editing,
  reviewing,
  finalizing,
  exportable,
  completed,
  cancelled,
  exportFailed,
  shareLaunched,
}

enum SessionEvent {
  enable,
  disable,
  startCapture,
  captureSucceeded,
  captureOmitted,
  review,
  finalize,
  finalizeSucceeded,
  finalizeFailed,
  saveSucceeded,
  saveCancelled,
  shareLaunched,
  exportFailed,
  hostDestroyed,
  reset,
}

class SessionStateMachine {
  SessionStateMachine([this.state = SessionState.disabled]);
  SessionState state;

  bool transition(SessionEvent event) {
    if (event == SessionEvent.disable) {
      state = SessionState.disabled;
      return true;
    }
    if (event == SessionEvent.hostDestroyed) {
      const active = {
        SessionState.capturing,
        SessionState.editing,
        SessionState.reviewing,
        SessionState.finalizing,
        SessionState.exportable,
      };
      state = active.contains(state) ? SessionState.cancelled : SessionState.disabled;
      return true;
    }
    SessionState? next;
    switch (state) {
      case SessionState.disabled:
        if (event == SessionEvent.enable) next = SessionState.ready;
        break;
      case SessionState.ready:
        if (event == SessionEvent.startCapture) next = SessionState.capturing;
        break;
      case SessionState.capturing:
        if (event == SessionEvent.captureSucceeded || event == SessionEvent.captureOmitted) {
          next = SessionState.editing;
        }
        break;
      case SessionState.editing:
        if (event == SessionEvent.review) next = SessionState.reviewing;
        break;
      case SessionState.reviewing:
        if (event == SessionEvent.finalize) next = SessionState.finalizing;
        break;
      case SessionState.finalizing:
        if (event == SessionEvent.finalizeSucceeded) next = SessionState.exportable;
        if (event == SessionEvent.finalizeFailed) next = SessionState.exportFailed;
        break;
      case SessionState.exportable:
        if (event == SessionEvent.saveSucceeded) next = SessionState.completed;
        if (event == SessionEvent.saveCancelled) next = SessionState.cancelled;
        if (event == SessionEvent.shareLaunched) next = SessionState.shareLaunched;
        if (event == SessionEvent.exportFailed) next = SessionState.exportFailed;
        break;
      case SessionState.completed:
      case SessionState.cancelled:
      case SessionState.exportFailed:
      case SessionState.shareLaunched:
        if (event == SessionEvent.reset) next = SessionState.ready;
        break;
    }
    if (next == null) return false;
    state = next;
    return true;
  }
}

class Report {
  const Report({
    this.title = '',
    required this.description,
    this.expectedBehavior = '',
    this.actualBehavior = '',
    this.reproSteps,
    this.screenId,
  });

  final String title;
  final String description;
  final String expectedBehavior;
  final String actualBehavior;
  final String? reproSteps;
  final String? screenId;

  Map<String, Object?> toJson() => {
        'title': title,
        'description': description,
        'expectedBehavior': expectedBehavior,
        'actualBehavior': actualBehavior,
        'reproSteps': reproSteps,
        'screenId': screenId,
      };
}
