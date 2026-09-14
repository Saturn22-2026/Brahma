cmake_minimum_required(VERSION 3.22.1)

project("mldsa_jni")

# Include paths for mldsa-native
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../../../../../../mldsa-native/mldsa)

add_library(mldsa_jni SHARED
    mldsa_jni.c
    ../../../../../../mldsa-native/mldsa/mldsa_native.c
    ../../../../../../mldsa-native/mldsa/fips202.c
    ../../../../../../mldsa-native/mldsa/ntt.c
    ../../../../../../mldsa-native/mldsa/packing.c
    ../../../../../../mldsa-native/mldsa/poly.c
    ../../../../../../mldsa-native/mldsa/polyvec.c
    ../../../../../../mldsa-native/mldsa/reduce.c
    ../../../../../../mldsa-native/mldsa/rounding.c
    ../../../../../../mldsa-native/mldsa/sign.c
    ../../../../../../mldsa-native/mldsa/symmetric-shake.c
)

find_library(log-lib log)

target_link_libraries(mldsa_jni ${log-lib})
