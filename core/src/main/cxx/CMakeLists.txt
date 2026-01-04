if(NOT ANDROID)
    message(WARNING "Android build only supported. Rewrite build steps to work on other environment")
endif()

add_library(
        skitrace-lib-dl
        SHARED
        export.cc
)

find_library(
        log-lib
        log
)

target_link_libraries(
        skitrace-lib-dl
        SkiTraceNativeCore
        ${log-lib}
)