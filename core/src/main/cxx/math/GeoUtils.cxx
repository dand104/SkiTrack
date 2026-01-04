#include "GeoUtils.hxx"

namespace skitrace {
    constexpr double EARTH_RADIUS_METERS = 6371000.0;
    constexpr double DEG_TO_RAD = M_PI / 180.0;

    double GeoUtils::HaversineDistance(const GeoPoint& p1, const GeoPoint& p2) {
        const double dLat = (p2.latitude - p1.latitude) * DEG_TO_RAD;
        const double dLon = (p2.longitude - p1.longitude) * DEG_TO_RAD;

        const double lat1 = p1.latitude * DEG_TO_RAD;
        const double lat2 = p2.latitude * DEG_TO_RAD;

        const double a = std::sin(dLat / 2) * std::sin(dLat / 2) +
                   std::sin(dLon / 2) * std::sin(dLon / 2) * std::cos(lat1) * std::cos(lat2);
        const double c = 2 * std::atan2(std::sqrt(a), std::sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    double GeoUtils::CalculateSpeed(const double distanceMeters, const long long timeDeltaMs) {
        if (timeDeltaMs <= 0) return 0.0;
        return distanceMeters / (static_cast<double>(timeDeltaMs) / 1000.0);
    }
    double GeoUtils::PressureToAltitude(const double pressureHPa, const double seaLevelPressureHPa) {
        if (pressureHPa <= 0) return 0.0;
        return 44330.0 * (1.0 - std::pow(pressureHPa / seaLevelPressureHPa, 1.0 / 5.255));
    }
}