cmake_minimum_required(VERSION 3.22.1)

project(SkiTrace)

if(NOT ANDROID)
    message(WARNING "Android build only supported. Rewrite build steps to work on other environment")
endif()

include(dependencies.cmake)

add_subdirectory(src/main/cpp)