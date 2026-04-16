import 'package:flutter/material.dart';

import 'call_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _roomController = TextEditingController(text: 'consult-room-1');
  final _serverController = TextEditingController(text: 'http://10.0.2.2:3000');
  String _role = 'doctor';

  @override
  void dispose() {
    _roomController.dispose();
    _serverController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Telemedicine - WebRTC')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            DropdownButtonFormField<String>(
              value: _role,
              decoration: const InputDecoration(labelText: 'Role'),
              items: const [
                DropdownMenuItem(value: 'doctor', child: Text('Doctor')),
                DropdownMenuItem(value: 'patient', child: Text('Patient')),
              ],
              onChanged: (value) {
                if (value != null) {
                  setState(() => _role = value);
                }
              },
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _roomController,
              decoration: const InputDecoration(
                labelText: 'Room ID',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _serverController,
              decoration: const InputDecoration(
                labelText: 'Signaling server URL',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => CallScreen(
                      role: _role,
                      roomId: _roomController.text.trim(),
                      serverUrl: _serverController.text.trim(),
                    ),
                  ),
                );
              },
              child: const Text('Start consultation'),
            ),
          ],
        ),
      ),
    );
  }
}
