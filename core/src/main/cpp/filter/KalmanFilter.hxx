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
            KalmanFilter1D(const double q, const double r, const double p, const double initial_value)
                : q_(q), r_(r), p_(p), x_(initial_value) {}

             /**
             * @param measurement Измеренное значение
             * @param r_noise Шум измерения (дисперсия, обычно accuracy^2)
             */
            void Update(const double measurement, const double r_noise) {
                p_ = p_ + q_;
                k_ = p_ / (p_ + r_noise);
                x_ = x_ + k_ * (measurement - x_);
                p_ = (1.0 - k_) * p_;
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