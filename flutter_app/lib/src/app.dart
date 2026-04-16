import 'package:flutter/material.dart';
import 'screens/home_screen.dart';

class TelemedicineApp extends StatelessWidget {
  const TelemedicineApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Telemedicine Lab5',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
      ),
      home: const HomeScreen(),
    );
  }
}
