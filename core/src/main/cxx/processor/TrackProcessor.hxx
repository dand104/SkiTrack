#ifndef SKITRACE_TRACK_PROCESSOR_HXX
#define SKITRACE_TRACK_PROCESSOR_HXX

#include "math/GeoUtils.hxx"
#include "filter/KalmanFilter.hxx"
#include "filter/VerticalTracker.hxx"
#include <memory>
#include <vector>

namespace skitrace {

    struct TrackStatistics {
        double totalDistance; // meters
        double maxSpeed;      // m/s
        double avgSpeed;      // m/s
        double verticalDrop;  // meters (down)
        double verticalAscent;// meters (up)
        double currentAltitude;
        double currentSpeed;
        long long durationMs;
    };

    class TrackProcessor {
        public:
            TrackProcessor();
            ~TrackProcessor() = default;

            GeoPoint AddPoint(double lat, double lon, double alt, long long timestamp);

            [[nodiscard]] TrackStatistics GetStatistics() const;
            void UpdateSensors(double accelZ, double pressureHPa, long long timestamp);

            void Reset();

        private:
            bool isFirstPoint_{};
            GeoPoint lastFilteredPoint_{};

            double totalDistance_{};
            double maxSpeed_{};
            double verticalDrop_{};
            double verticalAscent_{};
            long long startTime_{};
            long long lastTime_{};
            double currentSpeed_{};

            std::unique_ptr<KalmanFilter1D> latFilter_;
            std::unique_ptr<KalmanFilter1D> lonFilter_;

            std::unique_ptr<VerticalTracker> verticalTracker_;
            long long lastSensorTime_ = 0;

            const double PROCESS_NOISE = 3.0; // Q
            const double MEASUREMENT_NOISE = 10.0; // R
            const double ESTIMATION_ERROR = 1.0; // P
    };

}

#endif //SKITRACE_TRACK_PROCESSOR_HXX