//
// Created by ja_ei on 17-Mar-20.
//

#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/core/mat.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>

using namespace std;
using namespace cv;

//#ifdef __cplusplus
//extern "C" {
//#endif
extern "C" JNIEXPORT void JNICALL
Java_com_example_loomoapp_OpenCV_OpenCVMain_nativeOrb(JNIEnv *env, jobject instance, jlong matAddr,
                                                      jlong dstAddr) {
    Mat &mat = *(Mat *) matAddr;
    vector<KeyPoint> &kp = *(vector<KeyPoint> *) dstAddr;
//    vector<KeyPoint> kp;
//    Mat &a = *(Mat *)noArray();

//    auto a = noArray();
    Ptr<ORB> detector = ORB::create();
    detector->detect(mat, kp);
}

//#ifdef __cplusplus
//}
//#endif

