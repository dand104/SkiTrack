#include <jni.h>
#include "processor/TrackProcessor.hxx"

using namespace skitrace;

extern "C" {

    JNIEXPORT jlong JNICALL
    Java_org_skitrace_skitrace_core_TrackProcessor_createNativeProcessor(JNIEnv *env, jobject thiz) {
        return reinterpret_cast<jlong>(new TrackProcessor());
    }

    JNIEXPORT void JNICALL
    Java_org_skitrace_skitrace_core_TrackProcessor_destroyNativeProcessor(JNIEnv *env, jobject thiz, jlong ptr) {
        const auto *processor = reinterpret_cast<TrackProcessor *>(ptr);
        delete processor;
    }

    JNIEXPORT void JNICALL
    Java_org_skitrace_skitrace_core_TrackProcessor_resetNative(JNIEnv *env, jobject thiz, jlong ptr) {
        auto *processor = reinterpret_cast<TrackProcessor *>(ptr);
        if (processor) processor->Reset();
    }

    JNIEXPORT void JNICALL
    Java_org_skitrace_skitrace_core_TrackProcessor_addPointNative(
            JNIEnv *env, jobject thiz, jlong ptr,
            jdouble lat, jdouble lon, jdouble alt, jlong timestamp,
            jobject outputBuf) {

        auto *processor = reinterpret_cast<TrackProcessor *>(ptr);
        if (!processor) return;

        auto *outData = static_cast<double *>(env->GetDirectBufferAddress(outputBuf));
        if (!outData) return;

        const GeoPoint p = processor->AddPoint(lat, lon, alt, timestamp);

        outData[0] = p.latitude;
        outData[1] = p.longitude;
        outData[2] = p.altitude;
    }

    JNIEXPORT void JNICALL
    Java_org_skitrace_skitrace_core_TrackProcessor_getStatisticsNative(
            JNIEnv *env, jobject thiz, jlong ptr, jobject outputBuf) {

        const auto *processor = reinterpret_cast<TrackProcessor *>(ptr);
        if (!processor) return;

        auto *outData = static_cast<double *>(env->GetDirectBufferAddress(outputBuf));
        if (!outData) return;

        const TrackStatistics s = processor->GetStatistics();

        outData[0] = s.totalDistance;
        outData[1] = s.maxSpeed;
        outData[2] = s.avgSpeed;
        outData[3] = s.verticalDrop;
        outData[4] = s.verticalAscent;
        outData[5] = s.currentAltitude;
        outData[6] = s.currentSpeed;
        outData[7] = static_cast<double>(s.durationMs);
        outData[8] = static_cast<double>(s.currentState);
    }

    JNIEXPORT void JNICALL
    Java_org_skitrace_skitrace_core_TrackProcessor_updateSensorsNative(
            JNIEnv *env, jobject thiz, jlong ptr,
            jint type, jfloat v0, jfloat v1, jfloat v2, jfloat v3, jlong timestamp) {

        auto *processor = reinterpret_cast<TrackProcessor *>(ptr);
        if (processor) {
            processor->UpdateSensors(type, v0, v1, v2, v3, timestamp);
        }
    }
}