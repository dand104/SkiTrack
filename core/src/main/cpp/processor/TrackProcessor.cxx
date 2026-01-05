#include "processor/TrackProcessor.hxx"
#include <algorithm>

namespace skitrace {

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

        latFilter_.reset();
        lonFilter_.reset();

        lastSensorTime_ = 0;
        verticalTracker_ = std::make_unique<VerticalTracker>();
    }
    void TrackProcessor::UpdateSensors(const double accelZ, const double pressureHPa, const long long timestamp) {
        if (lastSensorTime_ == 0) {
            lastSensorTime_ = timestamp;
            const double alt = GeoUtils::PressureToAltitude(pressureHPa);
            verticalTracker_->UpdateBarometer(alt);
            return;
        }

        const long long dtMs = timestamp - lastSensorTime_;
        if (dtMs > 0) {
            const double dtSec = dtMs / 1000.0;
            verticalTracker_->Predict(accelZ, dtSec);
        }

        if (pressureHPa > 0) {
            const double baroAlt = GeoUtils::PressureToAltitude(pressureHPa);
            verticalTracker_->UpdateBarometer(baroAlt);
        }

        lastSensorTime_ = timestamp;
    }
    GeoPoint TrackProcessor::AddPoint(double lat, double lon, const double alt, const long long timestamp) {
        if (isFirstPoint_) {
            isFirstPoint_ = false;
            startTime_ = timestamp;
            lastTime_ = timestamp;
            lastSensorTime_ = timestamp;

            latFilter_ = std::make_unique<KalmanFilter1D>(PROCESS_NOISE, MEASUREMENT_NOISE, ESTIMATION_ERROR, lat);
            lonFilter_ = std::make_unique<KalmanFilter1D>(PROCESS_NOISE, MEASUREMENT_NOISE, ESTIMATION_ERROR, lon);
            verticalTracker_->Initialize(alt);

            lastFilteredPoint_ = {lat, lon, alt, timestamp};
            return lastFilteredPoint_;
        }

        latFilter_->Update(lat);
        lonFilter_->Update(lon);
        verticalTracker_->UpdateGPS(alt);

        const GeoPoint newFilteredPoint = {
                latFilter_->GetState(),
                lonFilter_->GetState(),
                verticalTracker_->GetAltitude(),
                timestamp
        };


        const double dist = GeoUtils::HaversineDistance(lastFilteredPoint_, newFilteredPoint);
        const double altDiff = newFilteredPoint.altitude - lastFilteredPoint_.altitude;
        const long long timeDiff = timestamp - lastFilteredPoint_.timestamp;

        if (timeDiff > 0) {
            const double speed = GeoUtils::CalculateSpeed(dist, timeDiff);

            if (speed < 45.0) {
                totalDistance_ += dist;
                currentSpeed_ = speed;
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
        }

        return newFilteredPoint;
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
            duration
        };
    }

}