#include <gtest/gtest.h>
#include "math/GeoUtils.hxx"

using namespace skitrace;

TEST(GeoUtilsTest, HaversineDistance_ZeroDistance) {
    GeoPoint p1 = {0.0, 0.0, 0.0, 0};
    GeoPoint p2 = {0.0, 0.0, 100.0, 0};
    EXPECT_DOUBLE_EQ(GeoUtils::HaversineDistance(p1, p2), 0.0);
}

TEST(GeoUtilsTest, HaversineDistance_OneDegreeLatitude) {
    GeoPoint p1 = {0.0, 0.0, 0.0, 0};
    GeoPoint p2 = {1.0, 0.0, 0.0, 0};
    
    double dist = GeoUtils::HaversineDistance(p1, p2);
    EXPECT_NEAR(dist, 111195.0, 100.0);
}

TEST(GeoUtilsTest, CalculateSpeed_Simple) {
    double dist = 100.0;
    long long timeMs = 10000;
    EXPECT_DOUBLE_EQ(GeoUtils::CalculateSpeed(dist, timeMs), 10.0);
}

TEST(GeoUtilsTest, CalculateSpeed_ZeroTime) {
    EXPECT_DOUBLE_EQ(GeoUtils::CalculateSpeed(100.0, 0), 0.0);
}

TEST(GeoUtilsTest, PressureToAltitude_Standard) {
    double alt = GeoUtils::PressureToAltitude(1013.25, 1013.25);
    EXPECT_NEAR(alt, 0.0, 0.1);
}

TEST(GeoUtilsTest, PressureToAltitude_High) {
    double alt = GeoUtils::PressureToAltitude(500.0, 1013.25);
    EXPECT_GT(alt, 5000.0);
    EXPECT_LT(alt, 6000.0);
}
