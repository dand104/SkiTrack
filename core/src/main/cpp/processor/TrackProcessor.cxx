#include "processor/TrackProcessor.hxx"
#include <algorithm>
#include <cmath>

namespace skitrace {

    static constexpr int TYPE_ACCELEROMETER = 1;
    static constexpr int TYPE_PRESSURE = 6;
    static constexpr int TYPE_LINEAR_ACCELERATION = 10;
    static constexpr int TYPE_ROTATION_VECTOR = 11;
    static constexpr int TYPE_GAME_ROTATION_VECTOR = 15;

    static constexpr int CONSISTENCY_THRESHOLD = 5;
    static constexpr double MIN_MOVE_SPEED = 0.8;
    static constexpr double LIFT_ASCENT_RATE = 0.5;
    static constexpr double SKI_DESCENT_RATE = -0.8;
    static constexpr double METERS_TO_DEGREES = 1.0 / 111132.0;

    static constexpr int AR_IN_VEHICLE = 0;
    static constexpr int AR_ON_BICYCLE = 1;
    static constexpr int AR_ON_FOOT = 2;
    static constexpr int AR_STILL = 3;
    static constexpr int AR_UNKNOWN = 4;
    static constexpr int AR_TILTING = 5;
    static constexpr int AR_WALKING = 7;
    static constexpr int AR_RUNNING = 8;

    void TrackProcessor::UpdateActivity(const int type, const int confidence) {
        lastActivityType_ = type;
        lastActivityConfidence_ = confidence;
    }

    TrackProcessor::TrackProcessor() {
        verticalTracker_ = std::make_unique<VerticalTracker>();
        Reset();
    }

    void TrackProcessor::Reset() {
        isFirstPoint_ = true;
        totalDistance_ = 0.0;
        maxSpeed_ = 0.0;
        verticalDrop_ = 0.0;
        verticalAscent_ = 0.0;
        currentSpeed_ = 0.0;
        startTime_ = 0;
        lastTime_ = 0;

        currentState_ = TrackState::IDLE;
        liftConsistentCount_ = 0;
        skiConsistentCount_ = 0;
        idleConsistentCount_ = 0;

        latFilter_.reset();
        lonFilter_.reset();

        lastSensorTime_ = 0;
        verticalTracker_ = std::make_unique<VerticalTracker>();

        currentRotation_ = Eigen::Quaterniond::Identity();
        hasRotation_ = false;
    }

    void TrackProcessor::UpdateSensors(const int sensorType, const float v0, const float v1, const float v2, const float v3,
                                        const long long timestamp) {

        if (sensorType == TYPE_PRESSURE) {
            // v0 = pressure in hPa
            const double alt = GeoUtils::PressureToAltitude(v0);
            verticalTracker_->UpdateBarometer(alt);
        }
        else if (sensorType == TYPE_ROTATION_VECTOR || sensorType == TYPE_GAME_ROTATION_VECTOR) {
            double x = v0;
            double y = v1;
            double z = v2;
            double w = v3;

            if (sensorType == TYPE_ROTATION_VECTOR && w == 0.0 && (x*x+y*y+z*z) < 1.001) {
                const double magSq = x*x + y*y + z*z;
                if (magSq < 1.0) {
                    w = std::sqrt(1.0 - magSq);
                } else {
                    w = 0;
                }
            }

            currentRotation_ = Eigen::Quaterniond(w, x, y, z);
            currentRotation_.normalize();
            hasRotation_ = true;
        }
        else if (sensorType == TYPE_LINEAR_ACCELERATION) {
            if (lastSensorTime_ == 0) {
                lastSensorTime_ = timestamp;
                return;
            }

            long long dtMs = timestamp - lastSensorTime_;
            if (dtMs <= 0) return;
            if (dtMs > 500) dtMs = 500;
            const double dtSec = dtMs / 1000.0;
            lastSensorTime_ = timestamp;

            double verticalAccel = 0.0;

            if (hasRotation_) {
                // Accel_World = Q * Accel_Device * Q_inverse
                Eigen::Vector3d deviceAccel(v0, v1, v2);
                Eigen::Vector3d worldAccel = currentRotation_ * deviceAccel;

                verticalAccel = worldAccel.z();
            } else {
                // Without rotation, we can't trust Z. Let's use Z as is but it's error-prone.
                verticalAccel = v2;
            }

            verticalTracker_->Predict(verticalAccel, dtSec);
        }
    }

    void TrackProcessor::UpdateState(const double speedMs, const double verticalVel) {
        // TODO: improve state machine

        const bool strongArSignal = lastActivityConfidence_ > 70;
        bool looksLikeLift = verticalVel > LIFT_ASCENT_RATE;
        if (strongArSignal && lastActivityType_ == AR_IN_VEHICLE) {
            if (verticalVel > 0.2) looksLikeLift = true;
        }

        bool looksLikeSki = (verticalVel < SKI_DESCENT_RATE || speedMs > 4.0);
        bool looksLikeIdle = speedMs < MIN_MOVE_SPEED;

        if (strongArSignal && lastActivityType_ == AR_STILL) {
            looksLikeIdle = true;
        }

        if (verticalVel > LIFT_ASCENT_RATE) {
            liftConsistentCount_++;
            skiConsistentCount_ = 0;
            idleConsistentCount_ = 0;
        }
        else if (verticalVel < SKI_DESCENT_RATE || speedMs > 4.0) {
            skiConsistentCount_++;
            liftConsistentCount_ = 0;
            idleConsistentCount_ = 0;
        }
        else if (speedMs < MIN_MOVE_SPEED) {
            idleConsistentCount_++;
            liftConsistentCount_ = 0;
            skiConsistentCount_ = 0;
        }
        else {
             if(liftConsistentCount_ > 0) liftConsistentCount_--;
             if(skiConsistentCount_ > 0) skiConsistentCount_--;
             if(idleConsistentCount_ > 0) idleConsistentCount_--;
        }

        if (looksLikeLift) {
            liftConsistentCount_ += (strongArSignal && lastActivityType_ == AR_IN_VEHICLE) ? 2 : 1;
            skiConsistentCount_ = 0;
            idleConsistentCount_ = 0;
        }
        else if (looksLikeSki) {
            skiConsistentCount_++;
            liftConsistentCount_ = 0;
            idleConsistentCount_ = 0;
        }
        else if (looksLikeIdle) {
            idleConsistentCount_ += (strongArSignal && lastActivityType_ == AR_STILL) ? 2 : 1;
            liftConsistentCount_ = 0;
            skiConsistentCount_ = 0;
        }

        if (liftConsistentCount_ > CONSISTENCY_THRESHOLD && currentState_ != TrackState::LIFT) {
            currentState_ = TrackState::LIFT;
        }
        else if (skiConsistentCount_ > CONSISTENCY_THRESHOLD && currentState_ != TrackState::SKIING) {
            currentState_ = TrackState::SKIING;
        }
        else if (idleConsistentCount_ > CONSISTENCY_THRESHOLD && currentState_ != TrackState::IDLE) {
            currentState_ = TrackState::IDLE;
        }
    }

    GeoPoint TrackProcessor::AddPoint(double lat, double lon, const double alt,
                                    const double accuracy,
                                    const long long timestamp) {
        const double sigma_deg = (accuracy < 2.0 ? 2.0 : accuracy) * METERS_TO_DEGREES;
        const double r_variance_deg = sigma_deg * sigma_deg;

        if (isFirstPoint_) {
            isFirstPoint_ = false;
            startTime_ = timestamp;
            lastTime_ = timestamp;
            lastSensorTime_ = timestamp;

            latFilter_ = std::make_unique<KalmanFilterCV>(lat, 1e-11);
            lonFilter_ = std::make_unique<KalmanFilterCV>(lon, 1e-11);
            verticalTracker_->Initialize(alt);

            lastFilteredPoint_ = {lat, lon, alt, timestamp};
            return lastFilteredPoint_;
        }

        const long long dtMs = timestamp - lastTime_;
        const double dtSec = dtMs / 1000.0;

        if (dtSec > 0) {
            latFilter_->Predict(dtSec);
            lonFilter_->Predict(dtSec);

            latFilter_->Update(lat, r_variance_deg);
            lonFilter_->Update(lon, r_variance_deg);

            verticalTracker_->UpdateGPS(alt);

            const GeoPoint newFilteredPoint = {
                    latFilter_->GetPosition(),
                    lonFilter_->GetPosition(),
                    verticalTracker_->GetAltitude(),
                    timestamp
            };

            const double dist = GeoUtils::HaversineDistance(lastFilteredPoint_, newFilteredPoint);
            const double altDiff = newFilteredPoint.altitude - lastFilteredPoint_.altitude;

            const double speed = GeoUtils::CalculateSpeed(dist, dtMs);
            const double verticalVel = verticalTracker_->GetVelocity();

            UpdateState(speed, verticalVel);
            if (speed < 45.0) {
                currentSpeed_ = speed;

                if (currentState_ != TrackState::IDLE) {
                    totalDistance_ += dist;
                }

                if (speed > maxSpeed_) {
                    maxSpeed_ = speed;
                }

                if (std::abs(altDiff) > 0.1) {
                    if (altDiff < 0) {
                        verticalDrop_ += std::abs(altDiff);
                    } else {
                        verticalAscent_ += altDiff;
                    }
                }

                lastFilteredPoint_ = newFilteredPoint;
                lastTime_ = timestamp;
            }

            return newFilteredPoint;
        }

        return lastFilteredPoint_;
    }

    TrackStatistics TrackProcessor::GetStatistics() const {
        double avgSpeed = 0.0;
        const long long duration = lastTime_ - startTime_;
        if (duration > 0) {
            avgSpeed = GeoUtils::CalculateSpeed(totalDistance_, duration);
        }

        return TrackStatistics{
            totalDistance_,
            maxSpeed_,
            avgSpeed,
            verticalDrop_,
            verticalAscent_,
            verticalTracker_->GetAltitude(),
            currentSpeed_,
            duration,
            currentState_
        };
    }

}