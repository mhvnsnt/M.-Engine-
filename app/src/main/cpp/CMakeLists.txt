cmake_minimum_required(VERSION 3.22.1)
project("mengine_llama")

add_library(mengine_llama SHARED llama-jni.cpp)

find_library(log-lib log)
target_link_libraries(mengine_llama ${log-lib})
