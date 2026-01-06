#ifndef SKITRACE_VERTICAL_TRACKER_HXX
#define SKITRACE_VERTICAL_TRACKER_HXX

#include <Eigen/Dense>

namespace skitrace {

    class VerticalTracker {
    public:
        VerticalTracker();

        void Initialize(double initialAltitude, bool isBarometric);
        bool IsInitialized() const;

        void Predict(double accelZ, double dtSec);

        void UpdateBarometer(double pressureAlt);

        void UpdateGPS(double gpsAlt, double verticalAccuracy);

        double GetAltitude() const;
        double GetVelocity() const;

    private:
        bool initialized_ = false;

        // State vector: [Altitude, Velocity, AccelBias, BaroBias]
        Eigen::Vector4d x_;

        // Covariance matrix
        Eigen::Matrix4d P_;

        // Process noise covariance
        Eigen::Matrix4d Q_;
    };

}

#endif //SKITRACE_VERTICAL_TRACKER_HXX