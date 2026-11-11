// SPDX-License-Identifier: Apache-2.0
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:reproparcel_flutter/reproparcel_channel.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('native plugin initializes, enables and records safe evidence', (tester) async {
    final initialized = await ReproParcelFlutter.initialize({
      'enabled': false,
      'packageName': 'com.example.reproparcel_host',
      'versionName': '1.0.0',
      'versionCode': 1,
      'variant': 'debug',
      'capturePolicy': {
        'policyVersion': 'flutter-host-policy-v1',
        'screenshotEnabled': false,
        'semanticTextEnabled': false,
        'eligibleSurfaces': ['com.example.reproparcel_host.MainActivity'],
      },
    });
    expect(initialized, isTrue);

    expect(await ReproParcelFlutter.setEnabled(true), isTrue);
    expect(
      await ReproParcelFlutter.recordBreadcrumb({
        'category': 'navigation',
        'actionId': 'open_home',
        'screenId': 'home',
        'attributes': {'screen_id': 'home'},
      }),
      isTrue,
    );
    expect(await ReproParcelFlutter.deleteLocalReports(), isTrue);

    final reporter = await ReproParcelFlutter.showReporter(
      'com.example.reproparcel_host.MainActivity',
    );
    expect(reporter, 'success');
  });
}
