import 'package:flutter_test/flutter_test.dart';
import 'package:telemedicine_lab5_flutter/src/app.dart';

void main() {
  testWidgets('home screen renders', (tester) async {
    await tester.pumpWidget(const TelemedicineApp());
    expect(find.text('Telemedicine - WebRTC'), findsOneWidget);
  });
}
