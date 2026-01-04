#ifndef SKITRACE_KALMAN_FILTER_HXX
#define SKITRACE_KALMAN_FILTER_HXX

namespace skitrace {
    class KalmanFilter1D {
        public:
            /**
             * @param q Process noise covariance
             * @param r Measurement noise covariance
             * @param p Estimated error covariance
             * @param initial_value
             */
            KalmanFilter1D(double q, double r, double p, double initial_value)
                : q_(q), r_(r), p_(p), x_(initial_value) {}

            void Update(double measurement) {
                p_ = p_ + q_;

                // Measurement update
                k_ = p_ / (p_ + r_);
                x_ = x_ + k_ * (measurement - x_);
                p_ = (1 - k_) * p_;
            }

            double GetState() const {
                return x_;
            }

        private:
            double q_; // Process noise
            double r_; // Measurement noise
            double p_; // Estimation error
            double k_ = 0.0; // Kalman gain
            double x_; // State estimate
    };

}

#endif //SKITRACE_KALMAN_FILTER_HXX