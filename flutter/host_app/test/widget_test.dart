// SPDX-License-Identifier: AGPL-3.0-only
import 'package:flutter_test/flutter_test.dart';
import 'package:reproparcel_host/main.dart';

void main() {
  testWidgets('host renders synthetic contracts and explicit controls', (tester) async {
    await tester.pumpWidget(const ReproParcelHostApp());
    expect(find.text('ReproParcel Flutter host'), findsOneWidget);
    expect(find.text('Synthetic integration host. No real report data.'), findsOneWidget);
    expect(find.text('Limit: 16 parcel entries'), findsOneWidget);
    expect(find.text('Initialize'), findsOneWidget);
    expect(find.text('Enable'), findsOneWidget);
    expect(find.text('Breadcrumb'), findsOneWidget);
    expect(find.text('Open reporter'), findsOneWidget);
  });
}
