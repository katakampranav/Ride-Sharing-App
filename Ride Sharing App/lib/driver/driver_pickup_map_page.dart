import 'package:flutter/material.dart';

class DriverPickupMapPage extends StatefulWidget {
  final String riderName;
  final String riderPhoto;
  final String fromLocation;
  final String toLocation;
  final VoidCallback onPickupComplete;

  const DriverPickupMapPage({
    super.key,
    required this.riderName,
    required this.riderPhoto,
    required this.fromLocation,
    required this.toLocation,
    required this.onPickupComplete,
  });

  @override
  State<DriverPickupMapPage> createState() => _DriverPickupMapPageState();
}

class _DriverPickupMapPageState extends State<DriverPickupMapPage> {
  @override
  void initState() {
    super.initState();
    _startPickupTimer();
  }

  void _startPickupTimer() {
    Future.delayed(const Duration(seconds: 3), () {
      widget.onPickupComplete();
      Navigator.of(context).pop();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        elevation: 0,
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text(
          'Picking up ${widget.riderName}',
          style: const TextStyle(color: Colors.black87, fontWeight: FontWeight.w600),
        ),
      ),
      body: Column(
        children: [
          // 🚗 Map Section with More Visible Background Image
          Expanded(
            child: Container(
              decoration: const BoxDecoration(
                image: DecorationImage(
                  image: NetworkImage('https://i.sstatic.net/sENel.png'),
                  fit: BoxFit.cover,
                  opacity: 0.45, // increased visibility
                ),
              ),
              child: Stack(
                children: [
                  // Gradient overlay for text contrast
                  Container(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        colors: [
                          Colors.white.withOpacity(0.4),
                          Colors.white.withOpacity(0.15),
                          Colors.white.withOpacity(0.05),
                        ],
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                      ),
                    ),
                  ),

                  // Center content (text + icon)
                  Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(
                          Icons.navigation_rounded,
                          size: 85,
                          color: Colors.blueAccent,
                        ),
                        const SizedBox(height: 18),
                        Text(
                          'Navigating to ${widget.fromLocation}',
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            fontSize: 21,
                            fontWeight: FontWeight.w700,
                            color: Colors.black87,
                            shadows: [
                              Shadow(
                                color: Colors.white,
                                blurRadius: 4,
                              )
                            ],
                          ),
                        ),
                        const SizedBox(height: 8),
                        const Text(
                          'Picking up rider...',
                          style: TextStyle(
                            fontSize: 16,
                            color: Colors.black54,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ),

                  // Progress indicator
                  Positioned(
                    top: 20,
                    left: 0,
                    right: 0,
                    child: LinearProgressIndicator(
                      backgroundColor: Colors.grey.shade300,
                      valueColor: AlwaysStoppedAnimation<Color>(
                        Theme.of(context).primaryColor,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),

          // 🧍‍♂️ Rider Info Card
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(16),
                topRight: Radius.circular(16),
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.grey.withOpacity(0.15),
                  blurRadius: 6,
                  offset: const Offset(0, -3),
                ),
              ],
            ),
            child: Row(
              children: [
                CircleAvatar(
                  radius: 28,
                  backgroundImage: NetworkImage(widget.riderPhoto),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        widget.riderName,
                        style: const TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        widget.fromLocation,
                        style: const TextStyle(
                          fontSize: 14,
                          color: Colors.grey,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  decoration: BoxDecoration(
                    color: Colors.orange.shade100,
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: const Row(
                    children: [
                      Icon(Icons.timer, size: 14, color: Colors.orange),
                      SizedBox(width: 4),
                      Text(
                        '3s',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: Colors.orange,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
