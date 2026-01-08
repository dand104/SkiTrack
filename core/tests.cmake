# Build tests
option(BUILD_TESTING "Build tests" OFF)
if(BUILD_TESTING)
    message(STATUS "Adding tests...")

    # Setup
    if (NOT ANDROID)
        # Setup GoogleTest
        FetchContent_Declare(
                googletest
                URL https://github.com/google/googletest/archive/refs/heads/main.zip
        )
        FetchContent_MakeAvailable(googletest)
        enable_testing()
    else ()
        find_package(googletest REQUIRED CONFIG)
        find_package(junit-gtest REQUIRED CONFIG)
    endif ()

    # Build
    if(ANDROID)
        add_library(skitrace_tests SHARED $<TARGET_OBJECTS:skitrace-core-modules>)
        target_link_libraries(skitrace_tests PRIVATE
                skitrace-core-modules
                googletest::gtest
                junit-gtest::junit-gtest
        )
    else()
        add_executable(skitrace_tests  $<TARGET_OBJECTS:skitrace-core-modules> src/test/cpp/Test_main.cxx)
        target_link_libraries(
                skitrace_tests PRIVATE
                skitrace-core-modules
                GTest::gtest_main
        )
    endif()

    # Source tests
    target_sources(skitrace_tests  PRIVATE
            src/test/cpp/GeoUtilsTest.cxx
            src/test/cpp/TrackProcessorTest.cxx
    )

    # Publish
    if(NOT ANDROID)
        add_test(NAME CoreTests COMMAND skitrace_tests)
    endif()
endif()