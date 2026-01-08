#include <gtest/gtest.h>
#include "processor/TrackProcessor.hxx"
#include <vector>

using namespace skitrace;

static constexpr int TYPE_LINEAR_ACCELERATION = 10;

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
};

TEST_F(TrackProcessorTest, InitialState_IsIdle) {
    TrackStatistics stats = processor.GetStatistics();
    EXPECT_EQ(stats.currentState, TrackState::IDLE);
    EXPECT_DOUBLE_EQ(stats.totalDistance, 0.0);
}

TEST_F(TrackProcessorTest, DetectSkiing_DownhillFast) {
    long long ts = 1000000000LL; // ns
    
    // Init filter
    SimulateTrack(5, 50.0, 0.0001, 2000.0, -2.0, ts, 1000000000LL);
    
    // State swithc test
    SimulateTrack(10, 50.0005, 0.0001, 1990.0, -2.0, ts + 5000000000LL, 1000000000LL);
    
    TrackStatistics stats = processor.GetStatistics();
    EXPECT_EQ(stats.currentState, TrackState::SKIING);
    EXPECT_GT(stats.currentSpeed, 5.0);
    EXPECT_GT(stats.verticalDrop, 10.0);
}

TEST_F(TrackProcessorTest, DetectLift_UphillSlow) {
    long long ts = 1000000000LL;
    
    SimulateTrack(5, 50.0, 0.00003, 1000.0, 3.0, ts, 1000000000LL);

    SimulateTrack(20, 50.00015, 0.00003, 1015.0, 3.0, ts + 5000000000LL, 1000000000LL);

    TrackStatistics stats = processor.GetStatistics();
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
    
    TrackStatistics stats = processor.GetStatistics();
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
    
    TrackStatistics stats = processor.GetStatistics();
    EXPECT_EQ(stats.currentState, TrackState::SKIING);
    EXPECT_GT(stats.totalDistance, 100.0); 
    EXPECT_GT(stats.maxSpeed, 0.0);
}

TEST_F(TrackProcessorTest, KalmanFilter_Smoothing) {
    long long ts = 0;
    
    GeoPoint p1 = processor.AddPoint(50.0, 50.0, 1000.0, 5.0, ts);
    
    ts += 1000000000LL;
    GeoPoint p2 = processor.AddPoint(50.0001, 50.0, 1000.0, 5.0, ts);
    

    ts += 1000000000LL;
    double outlierLat = 50.01;
    GeoPoint p3 = processor.AddPoint(outlierLat, 50.0, 1000.0, 20.0, ts);
    
    EXPECT_LT(p3.latitude, outlierLat);
    EXPECT_GT(p3.latitude, 50.0002);
}
