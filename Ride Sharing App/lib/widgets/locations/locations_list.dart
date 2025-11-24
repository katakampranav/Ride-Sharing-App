import 'package:flutter/material.dart';
import 'package:my_app/widgets/locations/empty_locations.dart';
import 'package:my_app/widgets/locations/location_item.dart';

class LocationsList extends StatelessWidget {
  final List<Map<String, String>> locations;
  final Function(int index) onDeleteLocation;

  const LocationsList({
    super.key,
    required this.locations,
    required this.onDeleteLocation,
  });

  @override
  Widget build(BuildContext context) {
    if (locations.isEmpty) {
      return const EmptyLocations();
    }

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: locations.length,
      itemBuilder: (context, index) {
        final location = locations[index];
        return LocationItem(
          title: location['title']!,
          address: location['address']!,
          type: location['type']!,
          onDelete: () => onDeleteLocation(index),
        );
      },
    );
  }
}