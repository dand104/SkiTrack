#include "TrackProcessor.hxx"
#include <algorithm>
#include <cmath>

namespace skitrace {

    static constexpr int TYPE_PRESSURE = 6;
    static constexpr int TYPE_LINEAR_ACCELERATION = 10;
    static constexpr int TYPE_ROTATION_VECTOR = 11;
    static constexpr int TYPE_GAME_ROTATION_VECTOR = 15;

    static constexpr int CONSISTENCY_THRESHOLD = 5;
    static constexpr double MIN_MOVE_SPEED = 0.8;
    static constexpr double LIFT_ASCENT_RATE = 0.8;
    static constexpr double SKI_DESCENT_RATE = -0.8;
    static constexpr double METERS_TO_DEGREES = 1.0 / 111132.0;

    static constexpr double NS_TO_SEC = 1.0e-9;

    static constexpr int AR_IN_VEHICLE = 0;
    static constexpr int AR_STILL = 3;

    static constexpr double OUTLIER_START_SIGMA = 3.0;
    static constexpr double OUTLIER_HARD_SIGMA  = 10.0;
    static constexpr double OUTLIER_R_SCALE_MAX = 1.0e6;

    inline double TrackProcessor::ClampMin(double v, double mn) { return (v < mn) ? mn : v; }

    double TrackProcessor::ComputeRScaleFromInnovation(double innovationMeters, double sigmaMeters) {
        sigmaMeters = ClampMin(sigmaMeters, 1.0);
        const double k = std::abs(innovationMeters) / sigmaMeters;

        if (k <= OUTLIER_START_SIGMA) return 1.0;
        if (k >= OUTLIER_HARD_SIGMA)  return OUTLIER_R_SCALE_MAX;

        const double t = (k / OUTLIER_START_SIGMA);
        double scale = t * t;
        if (scale > OUTLIER_R_SCALE_MAX) scale = OUTLIER_R_SCALE_MAX;
        return scale;
    }

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
        currentSpeed_ = 0.0;

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
            if (verticalTracker_->IsInitialized()) {
                const double alt = GeoUtils::PressureToAltitude(v0);
                verticalTracker_->UpdateBarometer(alt);
            }
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

            if (!verticalTracker_->IsInitialized()) {
                lastSensorTime_ = timestamp;
                return;
            }

            long long dtNs = timestamp - lastSensorTime_;
            double dtSec = dtNs * NS_TO_SEC;
            if (dtSec > 0.5) dtSec = 0.5;
            if (dtSec <= 0) return;

            lastSensorTime_ = timestamp;

            double verticalAccel = 0.0;

            if (hasRotation_) {
                // Accel_World = Q * Accel_Device * Q_inverse
                Eigen::Vector3d deviceAccel(v0, v1, v2);
                Eigen::Vector3d worldAccel = currentRotation_ * deviceAccel;

                verticalAccel = worldAccel.z();

                if (std::abs(verticalAccel) < 0.05) {
                    verticalAccel = 0.0;
                }
            }
            verticalTracker_->Predict(verticalAccel, dtSec);
        }
    }

    void TrackProcessor::UpdateState(const double speedMs, const double verticalVel) {
        const bool strongArSignal = lastActivityConfidence_ > 70;

        bool looksLift = verticalVel > LIFT_ASCENT_RATE;
        const bool looksSki  = (verticalVel < SKI_DESCENT_RATE) || (speedMs > 4.0);
        bool looksIdle = speedMs < MIN_MOVE_SPEED;

        if (strongArSignal) {
            if (lastActivityType_ == AR_IN_VEHICLE) {
                if (verticalVel > 0.2) looksLift = true;
            }
            if (lastActivityType_ == AR_STILL) {
                looksIdle = true;
            }
        }

        TrackState desired = currentState_;

        if (looksLift && !looksIdle)      desired = TrackState::LIFT;
        else if (looksSki && !looksIdle)  desired = TrackState::SKIING;
        else if (looksIdle)              desired = TrackState::IDLE;

        auto dec = [](int& c) { if (c > 0) --c; };

        if (desired == TrackState::LIFT) {
            liftConsistentCount_++;
            dec(skiConsistentCount_);
            dec(idleConsistentCount_);
            if (strongArSignal && lastActivityType_ == AR_IN_VEHICLE) liftConsistentCount_++;
        }
        else if (desired == TrackState::SKIING) {
            skiConsistentCount_++;
            dec(liftConsistentCount_);
            dec(idleConsistentCount_);
        }
        else if (desired == TrackState::IDLE) {
            idleConsistentCount_++;
            dec(liftConsistentCount_);
            dec(skiConsistentCount_);
            if (strongArSignal && lastActivityType_ == AR_STILL) idleConsistentCount_++;
        }
        else {
            dec(liftConsistentCount_);
            dec(skiConsistentCount_);
            dec(idleConsistentCount_);
        }

        if (liftConsistentCount_ > CONSISTENCY_THRESHOLD) {
            currentState_ = TrackState::LIFT;
        }
        else if (skiConsistentCount_ > CONSISTENCY_THRESHOLD) {
            currentState_ = TrackState::SKIING;
        }
        else if (idleConsistentCount_ > CONSISTENCY_THRESHOLD) {
            currentState_ = TrackState::IDLE;
        }
    }

    InstantTrackData TrackProcessor::ProcessPoint(double lat, double lon, double alt,
                                                double accuracy, long long timestampNs) {

        const double sigma_m = (accuracy < 2.0 ? 2.0 : accuracy);
        const double sigma_deg = sigma_m * METERS_TO_DEGREES;

        if (isFirstPoint_) {
            isFirstPoint_ = false;

            constexpr double baseProcessNoise = 8.0e-11;
            latFilter_ = std::make_unique<KalmanFilterCV>(lat, baseProcessNoise);
            lonFilter_ = std::make_unique<KalmanFilterCV>(lon, baseProcessNoise);
            verticalTracker_->Initialize(alt, false);

            lastFilteredPoint_ = {lat, lon, alt, timestampNs};
            currentSpeed_ = 0.0;
            currentState_ = TrackState::IDLE;

            return {lastFilteredPoint_, currentSpeed_, currentState_};
        }

        const long long dtNs = timestampNs - lastFilteredPoint_.timestamp;
        const double dtSec = dtNs * NS_TO_SEC;

        if (dtSec > 0.0001) {
            double cosLat = std::cos(lat * M_PI / 180.0);
            if (cosLat < 0.01) cosLat = 0.01;
            lonFilter_->SetProcessNoise(8.0e-11 / (cosLat * cosLat));

            latFilter_->Predict(dtSec);
            lonFilter_->Predict(dtSec);
            double predLat = latFilter_->GetPosition();
            double predLon = lonFilter_->GetPosition();
            double innovation_m = GeoUtils::HaversineDistance({predLat, predLon}, {lat, lon});
            double rScale = ComputeRScaleFromInnovation(innovation_m, sigma_m);

            latFilter_->Update(lat, (sigma_deg * sigma_deg) * rScale);
            lonFilter_->Update(lon, (sigma_deg / cosLat) * (sigma_deg / cosLat) * rScale);

            verticalTracker_->UpdateGPS(alt, sigma_m * 1.5);

            GeoPoint newPoint = {
                latFilter_->GetPosition(),
                lonFilter_->GetPosition(),
                verticalTracker_->GetAltitude(),
                timestampNs
            };

            double distMoved = GeoUtils::HaversineDistance(lastFilteredPoint_, newPoint);
            currentSpeed_ = distMoved / dtSec;

            double verticalVel = verticalTracker_->GetVelocity();
            UpdateState(currentSpeed_, verticalVel);

            lastFilteredPoint_ = newPoint;
        }

        return {
            lastFilteredPoint_,
            currentSpeed_,
            currentState_
        };
    }

}