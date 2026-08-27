cmake_minimum_required(VERSION 3.22.1)

project("mldsa_jni")

add_library(mldsa_jni SHARED
    mldsa_jni.c
    mldsa-native/mldsa/mldsa_native.c
)

target_include_directories(mldsa_jni PRIVATE
    mldsa-native/mldsa
)

target_compile_definitions(mldsa_jni PRIVATE
    MLD_CONFIG_PARAMETER_SET=44
    MLD_BUILD_INTERNAL
)

find_library(log-lib log)

target_link_libraries(mldsa_jni
    ${log-lib}
)
