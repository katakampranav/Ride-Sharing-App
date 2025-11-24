import 'package:flutter/material.dart';

class MapWidget extends StatelessWidget {
  final String fromLocation;
  final String toLocation;
  final String riderName;
  final String riderPhoto;
  final bool showRouteAnimation;

  const MapWidget({
    super.key,
    required this.fromLocation,
    required this.toLocation,
    required this.riderName,
    required this.riderPhoto,
    this.showRouteAnimation = true,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 300,
      decoration: BoxDecoration(
        color: Colors.grey.shade900,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Stack(
        children: [
          // Base Map Background
          _buildMapBackground(),
          
          // Route Line
          if (showRouteAnimation) _buildAnimatedRoute(),
          
          // Start Marker (Driver)
          _buildStartMarker(),
          
          // End Marker (Rider)
          _buildEndMarker(),
          
          // Car Icon (Moving)
          if (showRouteAnimation) _buildMovingCar(),
          
          // Map Controls
          _buildMapControls(),
          
          // Route Info Overlay
          _buildRouteInfoOverlay(),
        ],
      ),
    );
  }

  Widget _buildMapBackground() {
    return Container(
      decoration: BoxDecoration(
        color: Colors.grey.shade900,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Opacity(
        opacity: 0.8,
        child: Image.network(
          'https://images.unsplash.com/photo-1541745537411-b8046dc6d66c?w=800&auto=format&fit=crop&q=60&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8NHx8bWFwfGVufDB8fDB8fHww',
          fit: BoxFit.cover,
          width: double.infinity,
          height: double.infinity,
        ),
      ),
    );
  }

  Widget _buildAnimatedRoute() {
    return Positioned(
      left: 50,
      top: 120,
      child: Container(
        width: 200,
        height: 4,
        decoration: BoxDecoration(
          gradient: LinearGradient(
            colors: [
              Colors.blue.shade400,
              Colors.green.shade400,
            ],
          ),
          borderRadius: BorderRadius.circular(2),
        ),
      ),
    );
  }

  Widget _buildStartMarker() {
    return const Positioned(
      left: 40,
      top: 110,
      child: Column(
        children: [
          Icon(Icons.location_pin, size: 30, color: Colors.red),
          SizedBox(height: 4),
          Text(
            'You',
            style: TextStyle(
              color: Colors.white,
              fontSize: 10,
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEndMarker() {
    return Positioned(
      right: 40,
      bottom: 80,
      child: Column(
        children: [
          Stack(
            children: [
              const Icon(Icons.location_pin, size: 30, color: Colors.green),
              Positioned(
                top: 4,
                left: 7,
                child: CircleAvatar(
                  radius: 6,
                  backgroundImage: NetworkImage(riderPhoto),
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            riderName.split(' ')[0],
            style: const TextStyle(
              color: Colors.white,
              fontSize: 10,
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMovingCar() {
    return Positioned(
      left: 120,
      top: 105,
      child: TweenAnimationBuilder(
        duration: const Duration(seconds: 3),
        tween: Tween<double>(begin: 0.0, end: 1.0),
        builder: (context, value, child) {
          return Transform.translate(
            offset: Offset(value * 160, 0),
            child: Container(
              padding: const EdgeInsets.all(6),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(20),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.3),
                    blurRadius: 8,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: const Icon(
                Icons.directions_car,
                color: Colors.blue,
                size: 24,
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildMapControls() {
    return Positioned(
      top: 16,
      right: 16,
      child: Column(
        children: [
          _buildMapControlButton(Icons.zoom_in, () {}),
          const SizedBox(height: 8),
          _buildMapControlButton(Icons.zoom_out, () {}),
          const SizedBox(height: 8),
          _buildMapControlButton(Icons.my_location, () {}),
        ],
      ),
    );
  }

  Widget _buildMapControlButton(IconData icon, VoidCallback onPressed) {
    return Container(
      width: 40,
      height: 40,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.2),
            blurRadius: 4,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: IconButton(
        icon: Icon(icon, size: 20),
        onPressed: onPressed,
        padding: EdgeInsets.zero,
      ),
    );
  }

  Widget _buildRouteInfoOverlay() {
    return Positioned(
      bottom: 16,
      left: 16,
      right: 16,
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(8),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.2),
              blurRadius: 8,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Row(
          children: [
            const Icon(Icons.route, size: 16, color: Colors.blue),
            const SizedBox(width: 8),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'To: $toLocation',
                    style: const TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  Text(
                    'Pickup: $fromLocation',
                    style: TextStyle(
                      fontSize: 10,
                      color: Colors.grey.shade600,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: Colors.green.shade50,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Row(
                children: [
                  const Icon(Icons.timer, size: 12, color: Colors.green),
                  const SizedBox(width: 4),
                  Text(
                    '3 min',
                    style: TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.w600,
                      color: Colors.green.shade700,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}