// SPDX-License-Identifier: Apache-2.0
import 'package:reproparcel_flutter/reproparcel.dart';
import 'package:test/test.dart';

void main() {
  test('image budget scales and rejects oversized images', () {
    expect(
      ImageBudget.scaledToLongestEdge(3200, 1600),
      const ImageDimensions(1600, 800),
    );
    expect(ImageBudget.isWithinDecodedBudget(4000, 4000), isFalse);
    expect(ImageBudget.isWithinDecodedBudget(1600, 1600), isTrue);
  });

  test('redactor keeps allowlisted safe values and drops secrets', () {
    expect(Redactor.sanitize(SafeAttribute.screenId, 'checkout'), 'checkout');
    expect(Redactor.sanitize(SafeAttribute.routeTemplate, '/orders/{id}'), '/orders/{id}');
    expect(Redactor.sanitize(SafeAttribute.screenId, 'user@example.com'), isNull);
    expect(Redactor.sanitize(SafeAttribute.routeTemplate, '/orders?id=secret'), isNull);
    expect(Redactor.sanitize(SafeAttribute.httpMethod, 'TRACE'), isNull);
    expect(
      Redactor.sanitizeAttributes({'screen_id': 'home', 'password': 'hunter2'}),
      {'screen_id': 'home'},
    );
  });

  test('session state machine rejects duplicate export', () {
    final machine = SessionStateMachine();
    expect(machine.transition(SessionEvent.enable), isTrue);
    expect(machine.transition(SessionEvent.startCapture), isTrue);
    expect(machine.transition(SessionEvent.captureSucceeded), isTrue);
    expect(machine.transition(SessionEvent.review), isTrue);
    expect(machine.transition(SessionEvent.finalize), isTrue);
    expect(machine.transition(SessionEvent.finalizeSucceeded), isTrue);
    expect(machine.state, SessionState.exportable);
    expect(machine.transition(SessionEvent.saveSucceeded), isTrue);
    expect(machine.state, SessionState.completed);
    expect(machine.transition(SessionEvent.saveSucceeded), isFalse);
  });

  test('report serializes synthetic content', () {
    const report = Report(
      description: 'Synthetic',
      expectedBehavior: 'Expected',
      actualBehavior: 'Actual',
    );
    expect(report.toJson()['description'], 'Synthetic');
  });
}
