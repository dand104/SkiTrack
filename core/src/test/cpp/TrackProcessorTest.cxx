#include <random>
#include <gtest/gtest.h>
#include "processor/TrackProcessor.hxx"
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

    void SimulateTrack(int count, double startLat, double dLat, double startAlt, double dAlt, long long startTs, long long dTs) {
        double currentLat = startLat;
        double currentAlt = startAlt;
        long long currentTs = startTs;

        for(int i=0; i<count; ++i) {
            processor.UpdateSensors(
                TYPE_LINEAR_ACCELERATION,
                0.0f, 0.0f, 0.0f, 0.0f,
                currentTs
            );

            processor.AddPoint(currentLat, 0.0, currentAlt, 5.0, currentTs);

            currentLat += dLat;
            currentAlt += dAlt;
            currentTs += dTs;
        }
    }
    void TickSensors(long long currentTs, long long durationNs) {
        long long step = 20000000LL;
        for (long long t = currentTs; t < currentTs + durationNs; t += step) {
            processor.UpdateSensors(TYPE_LINEAR_ACCELERATION, 0, 0, 0, 0, t);
        }
    }
};

TEST_F(TrackProcessorTest, InitialState_IsIdle) {
    TrackStatistics stats = processor.fetchTrackData();
    EXPECT_EQ(stats.currentState, TrackState::IDLE);
    EXPECT_DOUBLE_EQ(stats.totalDistance, 0.0);
}
TEST_F(TrackProcessorTest, Scenario_GPSDrift_StayIdle) {
    long long ts = 1000000000LL;
    double baseLat = 45.0;
    double baseLon = 7.0;
    double baseAlt = 2000.0;

    std::default_random_engine generator;
    std::normal_distribution<double> distribution(0.0, 0.00005); // ~5 метров шум

    for(int i=0; i<100; ++i) {
        TickSensors(ts, SEC_TO_NS);

        double noiseLat = distribution(generator);
        double noiseLon = distribution(generator);

        processor.AddPoint(baseLat + noiseLat, baseLon + noiseLon, baseAlt, 10.0, ts);
        ts += SEC_TO_NS;
    }

    TrackStatistics stats = processor.fetchTrackData();

    EXPECT_EQ(stats.currentState, TrackState::IDLE);
    EXPECT_LT(stats.avgSpeed, 0.5);
    EXPECT_LT(stats.totalDistance, 200.0);
}

TEST_F(TrackProcessorTest, DetectSkiing_DownhillFast) {
    long long ts = 1000000000LL; // ns
    
    // Init filter
    SimulateTrack(5, 50.0, 0.0001, 2000.0, -2.0, ts, 1000000000LL);
    
    // State swithc test
    SimulateTrack(10, 50.0005, 0.0001, 1990.0, -2.0, ts + 5000000000LL, 1000000000LL);
    
    TrackStatistics stats = processor.fetchTrackData();
    EXPECT_EQ(stats.currentState, TrackState::SKIING);
    EXPECT_GT(stats.currentSpeed, 5.0);
    EXPECT_GT(stats.verticalDrop, 10.0);
}

TEST_F(TrackProcessorTest, DetectLift_UphillSlow) {
    long long ts = 1000000000LL;
    
    SimulateTrack(5, 50.0, 0.00003, 1000.0, 3.0, ts, 1000000000LL);

    SimulateTrack(20, 50.00015, 0.00003, 1015.0, 3.0, ts + 5000000000LL, 1000000000LL);

    TrackStatistics stats = processor.fetchTrackData();
    EXPECT_EQ(stats.currentState, TrackState::LIFT);
    EXPECT_GT(stats.verticalAscent, 10.0);
}


TEST_F(TrackProcessorTest, DetectIdle_Stationary) {
    long long ts = 1000000000LL;

    // IDLE -> IDLE
    double lat = 50.0;
    for(int i=0; i<20; ++i) {
        double noise = (i % 2 == 0 ? 0.000001 : -0.000001);
        processor.AddPoint(lat + noise, 0.0, 1500.0, 5.0, ts);
        ts += 1000000000LL;
    }
    
    TrackStatistics stats = processor.fetchTrackData();
    EXPECT_EQ(stats.currentState, TrackState::IDLE);
    EXPECT_LT(stats.currentSpeed, 1.0);
}

TEST_F(TrackProcessorTest, Statistics_DistanceAccumulation) {
    long long ts = 0;
    
    // to SKIING ( > 5 points)
    double dLat = 0.0001; // ~11 meters
    for(int i=0; i<20; ++i) {
        processor.AddPoint(50.0 + (i * dLat), 0.0, 2000.0 - (i*1.0), 2.0, ts);
        ts += 1000000000LL; // 1 sec
    }
    
    TrackStatistics stats = processor.fetchTrackData();
    EXPECT_EQ(stats.currentState, TrackState::SKIING);
    EXPECT_GT(stats.totalDistance, 100.0); 
    EXPECT_GT(stats.maxSpeed, 0.0);
}

TEST_F(TrackProcessorTest, Scenario_GPSGlitch_Rejection) {
    long long ts = 0;
    double lat = 45.0;

    for(int i=0; i<10; ++i) {
        TickSensors(ts, SEC_TO_NS);
        processor.AddPoint(lat, 7.0, 2000.0, 5.0, ts);
        lat += 0.0001; // ~11 m/s
        ts += SEC_TO_NS;
    }

    TickSensors(ts, SEC_TO_NS);
    processor.AddPoint(lat + 0.01, 7.0, 2000.0, 20.0, ts);
    ts += SEC_TO_NS;

    for(int i=0; i<10; ++i) {
        TickSensors(ts, SEC_TO_NS);
        processor.AddPoint(lat, 7.0, 2000.0, 5.0, ts);
        lat += 0.0001;
        ts += SEC_TO_NS;
    }

    TrackStatistics stats = processor.fetchTrackData();

    EXPECT_LT(stats.maxSpeed, 50.0);
}

TEST_F(TrackProcessorTest, Scenario_FullDay_Cycle) {
    long long ts = 1000000000LL;
    double lat = 45.0;
    double alt = 1500.0;

    // 1: LIFT
    for(int i=0; i<30; ++i) {
        TickSensors(ts, SEC_TO_NS);
        processor.AddPoint(lat, 7.0, alt, 5.0, ts);
        lat += 0.00005; // ~5 m/s
        alt += 3.0;
        ts += SEC_TO_NS;
    }

    const TrackStatistics statsPhase1 = processor.fetchTrackData();
    EXPECT_EQ(statsPhase1.currentState, TrackState::LIFT);
    EXPECT_NEAR(statsPhase1.verticalAscent, 90.0, 10.0);
    EXPECT_NEAR(statsPhase1.verticalDrop, 0.0, 1.0);

    // 2: IDLE
    for(int i=0; i<40; ++i) {
        TickSensors(ts, SEC_TO_NS);
        processor.AddPoint(lat, 7.0, alt, 5.0, ts);
        ts += SEC_TO_NS;
    }

    TrackStatistics statsPhase2 = processor.fetchTrackData();
    EXPECT_EQ(statsPhase2.currentState, TrackState::IDLE);

    // 3: SKIING (fast)
    for(int i=0; i<40; ++i) {
        TickSensors(ts, SEC_TO_NS);
        processor.AddPoint(lat, 7.0, alt, 5.0, ts);
        lat -= 0.0002; // ~22 m/s
        alt -= 5.0;
        ts += SEC_TO_NS;
    }

    TrackStatistics statsPhase3 = processor.fetchTrackData();
    EXPECT_EQ(statsPhase3.currentState, TrackState::SKIING);

    EXPECT_NEAR(statsPhase3.verticalAscent, 90.0, 10.0);
    EXPECT_NEAR(statsPhase3.verticalDrop, 200.0, 20.0);
    EXPECT_GT(statsPhase3.maxSpeed, 10.0);
}

TEST_F(TrackProcessorTest, Scenario_TimeGap_ResetsFilter) {
    long long ts = 0;

    processor.AddPoint(45.0, 7.0, 2000.0, 5.0, ts);
    ts += 300 * SEC_TO_NS;
    GeoPoint p2 = processor.AddPoint(45.1, 7.1, 2000.0, 5.0, ts);

    EXPECT_NEAR(p2.latitude, 45.1, 0.001);
    EXPECT_NEAR(p2.longitude, 7.1, 0.001);
}