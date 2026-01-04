#include "VerticalTracker.hxx"
#include <cmath>

namespace skitrace {

    constexpr double SIGMA_ACCEL = 0.5;
    constexpr double SIGMA_BARO = 1.0;
    constexpr double SIGMA_GPS = 15.0;
    constexpr double SIGMA_PROCESS = 0.01;

    VerticalTracker::VerticalTracker() {
        x_.setZero();
        P_.setIdentity();

        P_(0,0) = 10.0;
        P_(1,1) = 1.0;
        P_(2,2) = 0.1;

        // z = H * x -> Alt = 1*Alt + 0*Vel + 0*Bias
        H_ << 1, 0, 0;
    }

    void VerticalTracker::Initialize(double initialAltitude) {
        x_(0) = initialAltitude;
        x_(1) = 0.0;
        x_(2) = 0.0; // Bias = 0
        initialized_ = true;
    }

    void VerticalTracker::Predict(double accelZ, double dt) {
        if (!initialized_ || dt <= 0) return;

        // h_new = h + v*dt + 0.5*(a-b)*dt^2
        // v_new = v + (a-b)*dt
        // b_new = b
        Eigen::Matrix3d F;
        F << 1, dt, -0.5 * dt * dt,
             0, 1,  -dt,
             0, 0,  1;

        // B (Control Input)
        Eigen::Vector3d B;
        B << 0.5 * dt * dt,
             dt,
             0;

        // Extrapolate State
        // x = F*x + B*u
        x_ = F * x_ + B * accelZ;

        // Extrapolate Uncertainty (Process Noise Q)
        Q_.setIdentity();
        Q_ = Q_ * (SIGMA_PROCESS * dt);
        Q_(1,1) += SIGMA_ACCEL * dt;

        P_ = F * P_ * F.transpose() + Q_;
    }

    void VerticalTracker::UpdateBarometer(double pressureAlt) {
        if (!initialized_) {
            Initialize(pressureAlt);
            return;
        }

        double R = SIGMA_BARO * SIGMA_BARO;

        // Kalman Gain
        // S = H*P*H' + R
        double S = (H_ * P_ * H_.transpose())(0, 0) + R;
        Eigen::Vector3d K = P_ * H_.transpose() / S;

        double y = pressureAlt - (H_ * x_)(0, 0);

        x_ = x_ + K * y;

        // Update Covariance
        // P = (I - K*H) * P
        Eigen::Matrix3d I = Eigen::Matrix3d::Identity();
        P_ = (I - K * H_) * P_;
    }

    void VerticalTracker::UpdateGPS(double gpsAlt) {
        if (!initialized_) {
            Initialize(gpsAlt);
            return;
        }

        double R = SIGMA_GPS * SIGMA_GPS;

        double S = (H_ * P_ * H_.transpose())(0, 0) + R;
        Eigen::Vector3d K = P_ * H_.transpose() / S;

        double y = gpsAlt - (H_ * x_)(0, 0);
        x_ = x_ + K * y;

        Eigen::Matrix3d I = Eigen::Matrix3d::Identity();
        P_ = (I - K * H_) * P_;
    }

    double VerticalTracker::GetAltitude() const {
        return x_(0);
    }

    double VerticalTracker::GetVelocity() const {
        return x_(1);
    }
}