#include <jni.h>
#include <string>
//#include <opencv2/core/core.hpp>
//#include <opencv2/features2d/features2d.hpp>
//
//using namespace std;
//using namespace cv;

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_loomoapp_MainActivity_stringFromJNI(JNIEnv *env, jobject) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());


//void JNICALL
//Java_com_example_loomoapp_OpenCV_OpenCVMain_nativeOrb(JNIEnv *env, jobject instance, jlong matAddr,
//                                                      jlong dstAddr) {
//    Mat &mat = *(Mat *) matAddr;
//    vector<KeyPoint> &kp = *(vector<KeyPoint> *) dstAddr;
////    vector<KeyPoint> kp;
////    Mat &a = *(Mat *)noArray();
//
////    auto a = noArray();
//    Ptr<ORB> detector = ORB::create();
//    detector->detect(mat, kp);
//}

}
