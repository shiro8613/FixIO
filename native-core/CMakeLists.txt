cmake_minimum_required(VERSION 3.18)
project(native_core C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)
set(OPENSSL_USE_STATIC_LIBS ON)

set(CMAKE_POSITION_INDEPENDENT_CODE ON)

find_package(JNI REQUIRED)
find_package(OpenSSL REQUIRED)

include(FetchContent)
FetchContent_Declare(
    libdeflate
    GIT_REPOSITORY https://github.com/ebiggers/libdeflate.git
    GIT_TAG        v1.20
)
set(LIBDEFLATE_BUILD_GZIP OFF CACHE BOOL "" FORCE)
set(LIBDEFLATE_BUILD_SHARED_LIB OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(libdeflate)


add_library(native_core SHARED
    src/common/jni_init.c
    src/compression/compressor.c
    src/crypto/crypto_aes.c
    src/io/gzip_file.c
    src/common/deflate_utils.c
    src/compute/compute_dispatch.c
    src/compute/compute_scalar.c
    src/compute/compute_avx2.c
    src/compute/compute_avx512.c
)

target_include_directories(native_core PRIVATE
    ${JNI_INCLUDE_DIRS}
    ${libdeflate_SOURCE_DIR}
    ${OPENSSL_INCLUDE_DIR}
    src/common
    src/compute
   
)

target_link_libraries(native_core PRIVATE
    libdeflate_static
    OpenSSL::Crypto
)

set_target_properties(native_core PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../native-api/src/main/resources/natives"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../native-api/src/main/resources/natives"
    LIBRARY_OUTPUT_DIRECTORY_DEBUG "${CMAKE_SOURCE_DIR}/../native-api/src/main/resources/natives"
    RUNTIME_OUTPUT_DIRECTORY_DEBUG "${CMAKE_SOURCE_DIR}/../native-api/src/main/resources/natives"
    LIBRARY_OUTPUT_DIRECTORY_RELEASE "${CMAKE_SOURCE_DIR}/../native-api/src/main/resources/natives"
    RUNTIME_OUTPUT_DIRECTORY_RELEASE "${CMAKE_SOURCE_DIR}/../native-api/src/main/resources/natives"
)

target_compile_options(native_core PRIVATE -O3)

if(WIN32)
    target_link_options(native_core PRIVATE -Wl,-export-all-symbols)

else()
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC")
endif()

if (MSVC)
    set_source_files_properties(src/compute/compute_avx2.c PROPERTIES COMPILE_FLAGS "/arch:AVX2")
    set_source_files_properties(src/compute/compute_avx512.c PROPERTIES COMPILE_FLAGS "/arch:AVX512")
elseif (CMAKE_C_COMPILER_ID MATCHES "Clang")
    if (WIN32)
        set_source_files_properties(src/compute/compute_avx2.c PROPERTIES COMPILE_FLAGS "-mavx2")
        set_source_files_properties(src/compute/compute_avx512.c PROPERTIES COMPILE_FLAGS "-mavx512f -mavx512vl")
    else()
        set_source_files_properties(src/compute/compute_avx2.c PROPERTIES COMPILE_FLAGS "-mavx2")
        set_source_files_properties(src/compute/compute_avx512.c PROPERTIES COMPILE_FLAGS "-mavx512f -mavx512vl")
    endif()
else()
    set_source_files_properties(src/compute/compute_avx2.c PROPERTIES COMPILE_FLAGS "-mavx2")
    set_source_files_properties(src/compute/compute_avx512.c PROPERTIES COMPILE_FLAGS "-mavx512f -mavx512vl")
endif()