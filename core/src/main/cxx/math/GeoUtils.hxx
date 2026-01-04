#ifndef SKITRACE_GEO_UTILS_HXX
#define SKITRACE_GEO_UTILS_HXX

#include <cmath>

namespace skitrace {

    struct GeoPoint {
        double latitude;
        double longitude;
        double altitude;
        long long timestamp;
    };

    class GeoUtils {
        public:
            static double HaversineDistance(const GeoPoint& p1, const GeoPoint& p2);
            static double PressureToAltitude(double pressureHPa, double seaLevelPressureHPa = 1013.25);
            static double CalculateSpeed(double distanceMeters, long long timeDeltaMs);
    };

}

#endif //SKITRACE_GEO_UTILS_HXX