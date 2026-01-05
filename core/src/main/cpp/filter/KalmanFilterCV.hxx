#ifndef SKITRACE_KALMAN_FILTER_CV_HXX
#define SKITRACE_KALMAN_FILTER_CV_HXX

#include <Eigen/Dense>

namespace skitrace {

    /**
     * Constant Velocity Kalman Filter (1 Dimension mapped to Pos + Vel)
     * State: [Position, Velocity]
     */
    class KalmanFilterCV {
        public:
            KalmanFilterCV(const double initialPos, const double processNoise) {
                x_ << initialPos, 0.0;

                P_ << 10.0, 0.0,
                      0.0, 10.0;

                processNoise_ = processNoise;

                H_ << 1.0, 0.0;
            }

            void Predict(const double dt) {
                if (dt <= 0) return;

                Eigen::Matrix2d F;
                F << 1.0, dt,
                     0.0, 1.0;

                x_ = F * x_;

                Eigen::Matrix2d Q;
                const double dt2 = dt * dt;
                double n = processNoise_;
                Q << 0.25*dt2*dt2*n, 0.5*dt*dt2*n,
                     0.5*dt*dt2*n,    dt2*n;

                P_ = F * P_ * F.transpose() + Q;
            }

            void Update(const double measurement, const double noiseVariance) {
                double r = noiseVariance;

                // S = H*P*H' + R
                const double s = P_(0,0) + r;

                // K = P*H' * S^-1
                Eigen::Vector2d k;
                k << P_(0,0) / s,
                     P_(1,0) / s;

                const double y = measurement - x_(0);

                x_ = x_ + k * y;

                // P = (I - K*H) * P
                Eigen::Matrix2d I = Eigen::Matrix2d::Identity();
                Eigen::Matrix2d KH;
                KH << k(0), 0.0,
                      k(1), 0.0;

                P_ = (I - KH) * P_;
            }

            double GetPosition() const { return x_(0); }
            double GetVelocity() const { return x_(1); }

        private:
            Eigen::Vector2d x_;
            Eigen::Matrix2d P_;
            double processNoise_;
            Eigen::RowVector2d H_;
    };
}

#endif