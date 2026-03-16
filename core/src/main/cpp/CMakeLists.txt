add_library(
        skitrace-core-modules
        OBJECT
        # Math module
        math/GeoUtils.cxx
        # Processor module
        TrackProcessor.cxx
        # Filters
        filter/VerticalTracker.cxx
)

target_include_directories(skitrace-core-modules PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
)
if (ANDROID)
    add_library(
            skitrace-core-native
            SHARED
            JNIExport.cxx
            $<TARGET_OBJECTS:skitrace-core-modules>
    )
    target_link_options(skitrace-core-native PRIVATE "-Wl,-z,max-page-size=16384")
    find_library(
            log-lib
            log
    )
    target_link_libraries(
            skitrace-core-native
            ${log-lib}
            skitrace-core-modules
    )
else()
    add_library(
            skitrace-core-native
            STATIC
            $<TARGET_OBJECTS:skitrace-core-modules> )
    target_link_libraries(
            skitrace-core-native
            skitrace-core-modules
    )
endif()

if(WIN32)
    target_compile_definitions(skitrace-core-modules PRIVATE _USE_MATH_DEFINES)
endif()

target_link_libraries(
        skitrace-core-modules PUBLIC
        Eigen3::Eigen
)