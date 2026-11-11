import 'package:flutter/material.dart';
import 'package:reproparcel_flutter/reproparcel.dart';

void main() => runApp(const ReproParcelHostApp());

class ReproParcelHostApp extends StatelessWidget {
  const ReproParcelHostApp({super.key});

  @override
  Widget build(BuildContext context) {
    final machine = SessionStateMachine()..transition(SessionEvent.enable);
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
              Text('Session: ${machine.state.name}'),
              const SizedBox(height: 12),
              Text(
                Redactor.sanitize(SafeAttribute.routeTemplate, '/orders/{id}') ??
                    'route rejected',
              ),
            ],
          ),
        ),
      ),
    );
  }
}
