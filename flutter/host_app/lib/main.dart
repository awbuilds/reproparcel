import 'package:flutter/material.dart';
import 'package:reproparcel_flutter/reproparcel.dart';
import 'package:reproparcel_flutter/reproparcel_channel.dart';

void main() => runApp(const ReproParcelHostApp());

class ReproParcelHostApp extends StatefulWidget {
  const ReproParcelHostApp({super.key});

  @override
  State<ReproParcelHostApp> createState() => _ReproParcelHostAppState();
}

class _ReproParcelHostAppState extends State<ReproParcelHostApp> {
  final SessionStateMachine _machine = SessionStateMachine()
    ..transition(SessionEvent.enable);
  String _status = 'Initializing…';

  Future<void> _initialize() async {
    final ok = await ReproParcelFlutter.initialize({
      'enabled': false,
      'packageName': 'com.example.reproparcel_host',
      'versionName': '1.0.0',
      'versionCode': 1,
      'variant': 'debug',
      'capturePolicy': {
        'policyVersion': 'flutter-host-policy-v1',
        'screenshotEnabled': true,
        'semanticTextEnabled': false,
        'eligibleSurfaces': ['com.example.reproparcel_host.MainActivity'],
      },
    });
    setState(() => _status = ok ? 'Initialized disabled' : 'Initialization rejected');
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('ReproParcel Flutter host')),
        body: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Synthetic integration host. No real report data.'),
              const SizedBox(height: 12),
              Text('Limit: ${ReproParcelLimits.maxParcelEntries} parcel entries'),
              Text('Session: ${_machine.state.name}'),
              Text(
                Redactor.sanitize(SafeAttribute.routeTemplate, '/orders/{id}') ??
                    'route rejected',
              ),
              const SizedBox(height: 12),
              Text(_status),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                children: [
                  ElevatedButton(
                    onPressed: _initialize,
                    child: const Text('Initialize'),
                  ),
                  ElevatedButton(
                    onPressed: () async {
                      final ok = await ReproParcelFlutter.setEnabled(true);
                      setState(() => _status = ok ? 'Enabled' : 'Enable failed');
                    },
                    child: const Text('Enable'),
                  ),
                  ElevatedButton(
                    onPressed: () async {
                      final ok = await ReproParcelFlutter.recordBreadcrumb({
                        'category': 'navigation',
                        'actionId': 'open_home',
                        'screenId': 'home',
                      });
                      setState(() => _status = ok ? 'Breadcrumb recorded' : 'Breadcrumb rejected');
                    },
                    child: const Text('Breadcrumb'),
                  ),
                  ElevatedButton(
                    onPressed: () async {
                      final result = await ReproParcelFlutter.showReporter(
                        'com.example.reproparcel_host.MainActivity',
                      );
                      setState(() => _status =
                          result == 'success' ? 'Reporter launch requested' : 'Reporter: $result');
                    },
                    child: const Text('Open reporter'),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
