#ifndef SKITRACE_VERTICAL_TRACKER_HXX
#define SKITRACE_VERTICAL_TRACKER_HXX

#include <Eigen/Dense>

namespace skitrace {

    class VerticalTracker {
        public:
            VerticalTracker();

            void Initialize(double initialAltitude);

            void Predict(double accelZ, double dtSec);

            void UpdateBarometer(double pressureAlt);

            void UpdateGPS(double gpsAlt);

            double GetAltitude() const;
            double GetVelocity() const;

        private:
            bool initialized_ = false;

            // State vector: [Altitude, Velocity, AccelBias]
            Eigen::Vector3d x_;

            // Covariance matrix
            Eigen::Matrix3d P_;

            // Process noise covariance
            Eigen::Matrix3d Q_;

            // Measurement matrices
            Eigen::RowVector3d H_;
    };

}

#endif //SKITRACE_VERTICAL_TRACKER_HXX