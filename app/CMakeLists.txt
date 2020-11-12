cmake_minimum_required(VERSION 3.4.1)

add_library(native-lib SHARED src/main/cpp/native-lib.cpp )

find_library(log-lib
             log )

target_link_libraries( # Specifies the target library.
                       native-lib

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )