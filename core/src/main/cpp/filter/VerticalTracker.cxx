#include "VerticalTracker.hxx"
#include <cmath>

namespace skitrace {
    constexpr double SIGMA_ACCEL_NOISE = 0.5;
    constexpr double SIGMA_ACCEL_BIAS_WALK = 0.001;
    constexpr double SIGMA_BARO_BIAS_WALK = 0.01;

    constexpr double SIGMA_BARO_MEAS = 1.5;
    constexpr double DEFAULT_SIGMA_GPS = 15.0;

    VerticalTracker::VerticalTracker() {
        x_.setZero();
        P_.setIdentity();
        Q_.setZero();

        P_(0,0) = 100.0;
        P_(1,1) = 10.0;
        P_(2,2) = 0.1;
        P_(3,3) = 500.0;
    }

    void VerticalTracker::Initialize(double initialAltitude, bool isBarometric) {
        x_.setZero();
        x_(0) = initialAltitude;

        initialized_ = true;
    }

    void VerticalTracker::Predict(double accelZ, double dt) {
        if (!initialized_ || dt <= 0) return;

        Eigen::Matrix4d F = Eigen::Matrix4d::Identity();
        F(0, 1) = dt;
        F(0, 2) = -0.5 * dt * dt;
        F(1, 2) = -dt;

        Eigen::Vector4d B;
        B << 0.5 * dt * dt,
             dt,
             0.0,
             0.0;

        x_ = F * x_ + B * accelZ;

        const double dt2 = dt * dt;
        const double dt3 = dt2 * dt;
        const double dt4 = dt2 * dt2;

        constexpr double var_a = SIGMA_ACCEL_NOISE * SIGMA_ACCEL_NOISE;
        constexpr double var_ba = SIGMA_ACCEL_BIAS_WALK * SIGMA_ACCEL_BIAS_WALK;
        constexpr double var_bb = SIGMA_BARO_BIAS_WALK * SIGMA_BARO_BIAS_WALK;

        Q_.setZero();

        Q_(0, 0) = 0.25 * dt4 * var_a;
        Q_(0, 1) = 0.5 * dt3 * var_a;
        Q_(1, 0) = 0.5 * dt3 * var_a;
        Q_(1, 1) = dt2 * var_a;

        Q_(2, 2) = var_ba * dt;

        Q_(3, 3) = var_bb * dt;

        // P = F*P*F' + Q
        P_ = F * P_ * F.transpose() + Q_;
    }

    void VerticalTracker::UpdateBarometer(const double pressureAlt) {
        if (!initialized_) {
            Initialize(pressureAlt, true);
            return;
        }

        Eigen::RowVector4d H;
        H << 1.0, 0.0, 0.0, 1.0;

        const double R = SIGMA_BARO_MEAS * SIGMA_BARO_MEAS;

        // Innovation
        double y = pressureAlt - (x_(0) + x_(3));

        // S = H*P*H' + R
        double S = (H * P_ * H.transpose())(0, 0) + R;

        // Kalman Gain: K = P*H' * S^-1
        Eigen::Vector4d K = P_ * H.transpose() / S;

        // Update State
        x_ = x_ + K * y;

        // Update Covariance: P = (I - K*H) * P
        Eigen::Matrix4d I = Eigen::Matrix4d::Identity();
        P_ = (I - K * H) * P_;
    }

    void VerticalTracker::UpdateGPS(double gpsAlt, double verticalAccuracy) {
        if (!initialized_) {
            Initialize(gpsAlt, false);
            return;
        }

        Eigen::RowVector4d H;
        H << 1.0, 0.0, 0.0, 0.0;

        double sigma = (verticalAccuracy < 3.0) ? 3.0 : verticalAccuracy;
        const double R = sigma * sigma;

        double y = gpsAlt - x_(0);

        const double S = (H * P_ * H.transpose())(0, 0) + R;
        Eigen::Vector4d K = P_ * H.transpose() / S;

        x_ = x_ + K * y;

        const Eigen::Matrix4d I = Eigen::Matrix4d::Identity();
        P_ = (I - K * H) * P_;
    }

    double VerticalTracker::GetAltitude() const {
        return x_(0);
    }

    double VerticalTracker::GetVelocity() const {
        return x_(1);
    }
}