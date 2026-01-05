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

if (ANDROID)
    target_link_options(skitrace-core-native PRIVATE "-Wl,-z,max-page-size=16384")
    find_library(
            log-lib
            log
    )
    target_link_libraries(
            skitrace-core-native
            ${log-lib}
    )
endif()




target_include_directories(skitrace-core-native PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(
        skitrace-core-native
        Eigen3::Eigen
)