#ifndef SKITRACE_TRACK_PROCESSOR_HXX
#define SKITRACE_TRACK_PROCESSOR_HXX

#include "math/GeoUtils.hxx"
#include "filter/KalmanFilterCV.hxx"
#include <Eigen/Geometry>
#include "filter/VerticalTracker.hxx"
#include <memory>
#include <vector>

namespace skitrace {

    enum class TrackState {
        IDLE = 0,
        SKIING = 1,
        LIFT = 2
    };

    struct TrackStatistics {
        double totalDistance;
        double maxSpeed;      
        double avgSpeed;
        double verticalDrop;
        double verticalAscent;
        double currentAltitude;
        double currentSpeed;
        long long totalDurationMs;
        long long skiingDurationMs;
        long long liftDurationMs;
        TrackState currentState;
    };

    class TrackProcessor {
        public:
            TrackProcessor();
            ~TrackProcessor() = default;

            GeoPoint AddPoint(double lat, double lon, double alt, double accuracy, long long timestamp);

            [[nodiscard]] TrackStatistics fetchTrackData() const;
            void UpdateSensors(int sensorType, float v0, float v1, float v2, float v3, long long timestamp);
            void UpdateActivity(int type, int confidence);

            void Reset();

        private:
            void UpdateState(double speedMs, double verticalVel);
            bool isFirstPoint_{};
            GeoPoint lastFilteredPoint_{};

            static inline double ClampMin(double v, double mn);
            static double ComputeRScaleFromInnovation(double innovationMeters, double sigmaMeters);

            double totalDistance_{};
            double maxSpeed_{};
            double verticalDrop_{};
            double verticalAscent_{};
            long long startTime_{};
            long long skiingDurationNs_{};
            long long liftDurationNs_{};
            long long lastTime_{};
            double currentSpeed_{};
            TrackState currentState_ = TrackState::IDLE;

            int liftConsistentCount_ = 0;
            int skiConsistentCount_ = 0;
            int idleConsistentCount_ = 0;

            std::unique_ptr<KalmanFilterCV> latFilter_;
            std::unique_ptr<KalmanFilterCV> lonFilter_;
            std::unique_ptr<VerticalTracker> verticalTracker_;
            long long lastSensorTime_ = 0;

            Eigen::Quaterniond currentRotation_ = Eigen::Quaterniond::Identity();
            bool hasRotation_ = false;

            int lastActivityType_ = -1;
            int lastActivityConfidence_ = 0;

            const double PROCESS_NOISE = 3.0;
            const double MEASUREMENT_NOISE = 10.0;
            const double ESTIMATION_ERROR = 1.0;
    };

}

#endif //SKITRACE_TRACK_PROCESSOR_HXX