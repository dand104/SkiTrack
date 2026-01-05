# dependencies script
include(FetchContent)
message(STATUS "Loading external dependencies...")
# Eigen3
FetchContent_Declare(
        Eigen3
        GIT_REPOSITORY https://gitlab.com/libeigen/eigen.git
        GIT_TAG 5.0.1
        GIT_SHALLOW TRUE
        GIT_PROGRESS TRUE
)
set(EIGEN_BUILD_DOC OFF CACHE BOOL "Disable Eigen docs" FORCE)
set(EIGEN_BUILD_DEMOS OFF)
set(EIGEN_BUILD_BTL OFF)
set(BUILD_TESTING OFF CACHE BOOL "Disable Eigen tests" FORCE)
set(EIGEN_BUILD_PKGCONFIG OFF CACHE BOOL "Disable pkg-config" FORCE)
set(EIGEN_TEST_NOQT ON CACHE BOOL "Disable Qt support in tests" FORCE)


FetchContent_MakeAvailable(Eigen3)