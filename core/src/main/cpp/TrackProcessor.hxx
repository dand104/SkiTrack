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

   struct InstantTrackData {
        GeoPoint point;
        double currentSpeed;
        TrackState currentState;
    };

    class TrackProcessor {
        public:
            TrackProcessor();
            ~TrackProcessor() = default;

            InstantTrackData ProcessPoint(double lat, double lon, double alt, double accuracy, long long timestamp);
            void UpdateSensors(int sensorType, float v0, float v1, float v2, float v3, long long timestamp);
            void UpdateActivity(int type, int confidence);

            void Reset();

        private:
            void UpdateState(double speedMs, double verticalVel);

            static inline double ClampMin(double v, double mn);
            static double ComputeRScaleFromInnovation(double innovationMeters, double sigmaMeters);

            bool isFirstPoint_{};
            GeoPoint lastFilteredPoint_{};
            TrackState currentState_ = TrackState::IDLE;
            double currentSpeed_{};

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