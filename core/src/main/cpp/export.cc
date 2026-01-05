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
            jdouble lat, jdouble lon, jdouble alt,
            jdouble accuracy, jlong timestamp,
            jobject outputBuf) {

        auto *processor = reinterpret_cast<TrackProcessor *>(ptr);
        if (!processor) return;

        auto *outData = static_cast<double *>(env->GetDirectBufferAddress(outputBuf));
        if (!outData) return;

        const GeoPoint p = processor->AddPoint(lat, lon, alt, accuracy, timestamp);

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
    Java_org_skitrace_skitrace_core_TrackProcessor_updateSensorsBatchNative(
            JNIEnv *env, jobject thiz, jlong ptr,
            jintArray types, jfloatArray v0s, jfloatArray v1s, jfloatArray v2s, jfloatArray v3s,
            jlongArray timestamps, jint count) {

        auto *processor = reinterpret_cast<TrackProcessor *>(ptr);
        if (!processor || count <= 0) return;

        jint* c_types = static_cast<jint*>(env->GetPrimitiveArrayCritical(types, nullptr));
        jfloat* c_v0 = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(v0s, nullptr));
        jfloat* c_v1 = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(v1s, nullptr));
        jfloat* c_v2 = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(v2s, nullptr));
        jfloat* c_v3 = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(v3s, nullptr));
        jlong* c_time = static_cast<jlong*>(env->GetPrimitiveArrayCritical(timestamps, nullptr));

        for (int i = 0; i < count; ++i) {
            processor->UpdateSensors(c_types[i], c_v0[i], c_v1[i], c_v2[i], c_v3[i], c_time[i]);
        }

        env->ReleasePrimitiveArrayCritical(types, c_types, 0);
        env->ReleasePrimitiveArrayCritical(v0s, c_v0, 0);
        env->ReleasePrimitiveArrayCritical(v1s, c_v1, 0);
        env->ReleasePrimitiveArrayCritical(v2s, c_v2, 0);
        env->ReleasePrimitiveArrayCritical(v3s, c_v3, 0);
        env->ReleasePrimitiveArrayCritical(timestamps, c_time, 0);
    }

    JNIEXPORT void JNICALL
    Java_org_skitrace_skitrace_core_TrackProcessor_updateActivityNative(
            JNIEnv *env, jobject thiz, jlong ptr, jint type, jint confidence) {
        auto *processor = reinterpret_cast<TrackProcessor *>(ptr);
        if (processor) processor->UpdateActivity(type, confidence);
    }
}