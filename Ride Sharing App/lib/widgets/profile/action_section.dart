import 'package:flutter/material.dart';
import 'package:my_app/widgets/profile/action_item.dart';
import 'package:my_app/widgets/auth/auth_card.dart';

class ActionSection extends StatelessWidget {
  final VoidCallback onEditProfile;
  final VoidCallback onSavedLocations;
  final VoidCallback onVehicleDetails;
  final VoidCallback onNotifications;
  final VoidCallback onLogout;
  final bool showSavedLocations;
  final bool showVehicleDetails;

  const ActionSection({
    super.key,
    required this.onEditProfile,
    required this.onSavedLocations,
    required this.onVehicleDetails,
    required this.onNotifications,
    required this.onLogout,
    this.showSavedLocations = true,
    this.showVehicleDetails = true,
  });

  // Convenience constructor for Rider profile (shows saved locations, hides vehicle details)
  factory ActionSection.rider({
    required VoidCallback onEditProfile,
    required VoidCallback onSavedLocations,
    required VoidCallback onNotifications,
    required VoidCallback onLogout,
  }) {
    return ActionSection(
      onEditProfile: onEditProfile,
      onSavedLocations: onSavedLocations,
      onVehicleDetails: () {}, // Empty callback since it won't be shown
      onNotifications: onNotifications,
      onLogout: onLogout,
      showSavedLocations: true,
      showVehicleDetails: false,
    );
  }

  // Convenience constructor for Driver profile (shows vehicle details, hides saved locations)
  factory ActionSection.driver({
    required VoidCallback onEditProfile,
    required VoidCallback onVehicleDetails,
    required VoidCallback onNotifications,
    required VoidCallback onLogout,
  }) {
    return ActionSection(
      onEditProfile: onEditProfile,
      onSavedLocations: () {}, // Empty callback since it won't be shown
      onVehicleDetails: onVehicleDetails,
      onNotifications: onNotifications,
      onLogout: onLogout,
      showSavedLocations: false,
      showVehicleDetails: true,
    );
  }

  @override
  Widget build(BuildContext context) {
    final List<Widget> actionItems = [];

    // Always show Edit Profile
    actionItems.addAll([
      ActionItem(
        icon: Icons.edit,
        title: 'Edit Profile',
        onTap: onEditProfile,
      ),
      const SizedBox(height: 12),
    ]);

    // Conditionally show Saved Locations
    if (showSavedLocations) {
      actionItems.addAll([
        ActionItem(
          icon: Icons.location_city,
          title: 'Saved Locations',
          onTap: onSavedLocations,
        ),
        const SizedBox(height: 12),
      ]);
    }

    // Conditionally show Vehicle Details
    if (showVehicleDetails) {
      actionItems.addAll([
        ActionItem(
          icon: Icons.drive_eta,
          title: 'Vehicle Details',
          onTap: onVehicleDetails,
        ),
        const SizedBox(height: 12),
      ]);
    }

    // Always show Notifications and Logout
    actionItems.addAll([
      ActionItem(
        icon: Icons.notifications,
        title: 'Notifications',
        onTap: onNotifications,
      ),
      const SizedBox(height: 12),
      ActionItem(
        icon: Icons.logout,
        title: 'Log Out',
        onTap: onLogout,
        isLogout: true,
      ),
    ]);

    return AuthCard(
      children: actionItems,
    );
  }
}