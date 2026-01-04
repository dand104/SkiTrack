add_library(
        skitrace-core-native
        SHARED
        # JNI Exports
        export.cc
        # Math module
        math/GeoUtils.cxx
        # Processor module
        processor/TrackProcessor.cxx
        # Filters
        filter/VerticalTracker.cxx
)

find_library(
        log-lib
        log
)

target_include_directories(skitrace-core-native PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(
        skitrace-core-native
        ${log-lib}
        Eigen3::Eigen
)