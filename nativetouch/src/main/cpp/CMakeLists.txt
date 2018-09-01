cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=c99 -Wall")

add_library(nativetouche7 SHARED
            nativetouche7.c
            ${SRC_SOURCE}
            )

target_link_libraries(nativetouche7
                      android
                      log
                      )