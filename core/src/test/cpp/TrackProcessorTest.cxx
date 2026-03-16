#include <random>
#include <gtest/gtest.h>
#include "TrackProcessor.hxx"
#include <vector>

using namespace skitrace;

static constexpr int TYPE_LINEAR_ACCELERATION = 10;
constexpr long long SEC_TO_NS = 1000000000LL;

class TrackProcessorTest : public ::testing::Test {
protected:
    TrackProcessor processor;

    void SetUp() override {
        processor.Reset();
    }

    InstantTrackData SimulateTrack(int count, double startLat, double dLat, double startAlt, double dAlt, long long startTs, long long dTs) {
        double currentLat = startLat;
        double currentAlt = startAlt;
        long long currentTs = startTs;
        InstantTrackData lastData;

        for(int i = 0; i < count; ++i) {
            processor.UpdateSensors(
                TYPE_LINEAR_ACCELERATION,
                0.0f, 0.0f, 0.0f, 0.0f,
                currentTs
            );
            lastData = processor.ProcessPoint(currentLat, 0.0, currentAlt, 5.0, currentTs);

            currentLat += dLat;
            currentAlt += dAlt;
            currentTs += dTs;
        }
        return lastData;
    }

    void TickSensors(long long currentTs, long long durationNs) {
        long long step = 20000000LL; // 20ms
        for (long long t = currentTs; t < currentTs + durationNs; t += step) {
            processor.UpdateSensors(TYPE_LINEAR_ACCELERATION, 0, 0, 0, 0, t);
        }
    }
};

TEST_F(TrackProcessorTest, InitialState_IsIdle) {
    auto data = processor.ProcessPoint(45.0, 7.0, 1500.0, 5.0, 1000000000LL);

    EXPECT_EQ(data.currentState, TrackState::IDLE);
    EXPECT_DOUBLE_EQ(data.currentSpeed, 0.0);
    EXPECT_NEAR(data.point.latitude, 45.0, 0.0001);
}

TEST_F(TrackProcessorTest, Scenario_GPSDrift_StayIdle) {
    long long ts = 1000000000LL;
    double baseLat = 45.0;
    double baseAlt = 2000.0;

    std::default_random_engine generator;
    std::normal_distribution<double> distribution(0.0, 0.00002);

    InstantTrackData lastData;
    for(int i = 0; i < 50; ++i) {
        TickSensors(ts, SEC_TO_NS);
        double noiseLat = distribution(generator);
        lastData = processor.ProcessPoint(baseLat + noiseLat, 7.0, baseAlt, 10.0, ts);
        ts += SEC_TO_NS;
    }

    EXPECT_EQ(lastData.currentState, TrackState::IDLE);
    EXPECT_LT(lastData.currentSpeed, 1.0);
}

TEST_F(TrackProcessorTest, DetectSkiing_DownhillFast) {
    long long ts = 1000000000LL;

    // Fast
    auto data = SimulateTrack(15, 45.0, 0.0002, 2000.0, -3.0, ts, SEC_TO_NS);

    EXPECT_EQ(data.currentState, TrackState::SKIING);
    EXPECT_GT(data.currentSpeed, 10.0);
}

TEST_F(TrackProcessorTest, DetectLift_UphillSlow) {
    long long ts = 1000000000LL;

    // Lift
    auto data = SimulateTrack(20, 45.0, 0.00003, 1000.0, 2.0, ts, SEC_TO_NS);

    EXPECT_EQ(data.currentState, TrackState::LIFT);
}

TEST_F(TrackProcessorTest, Scenario_GPSGlitch_Rejection) {
    long long ts = 1000000000LL;
    double lat = 45.0;

    // Stable run
    SimulateTrack(10, lat, 0.0001, 2000.0, 0.0, ts, SEC_TO_NS);
    ts += 10 * SEC_TO_NS;
    lat += 10 * 0.0001;

    // gps 500m collision
    auto glitchData = processor.ProcessPoint(lat + 0.005, 7.0, 2000.0, 5.0, ts);

    EXPECT_NEAR(glitchData.point.latitude, lat, 0.001);
}

TEST_F(TrackProcessorTest, Scenario_TimeGap_ResetsFilter) {
    long long ts = 1000000000LL;

    // 1st point
    processor.ProcessPoint(45.0, 7.0, 2000.0, 5.0, ts);

    // app stoped working in background for 10 mins
    ts += 600 * SEC_TO_NS;

    auto data = processor.ProcessPoint(46.0, 8.0, 2000.0, 5.0, ts);

    EXPECT_NEAR(data.point.latitude, 46.0, 0.01);
}
