cmake_minimum_required(VERSION 3.22.1)

project(SkiTrace)

if(NOT ANDROID)
    message(WARNING "Android build only supported. Rewrite build steps to work on other environment")
endif()

add_library(
        skitrace_core
        SHARED
        export.cxx
)

find_library(
        log-lib
        log
)

target_link_libraries(
        skitrace_core
        ${log-lib}
)