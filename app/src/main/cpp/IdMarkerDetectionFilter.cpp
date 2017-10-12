
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/aruco.hpp>

#include "IdMarkerDetectionFilter.hpp"

using namespace mymensor;

IdMarkerDetectionFilter::IdMarkerDetectionFilter(int qtyVps, float realSize) {
    qtVp = qtyVps;
    markerLength = realSize;
    dictionary = cv::aruco::getPredefinedDictionary(
            cv::aruco::PREDEFINED_DICTIONARY_NAME(cv::aruco::DICT_ARUCO_ORIGINAL));
    mCandidateSceneCorners.create(4, 1, CV_32FC2);
    // Assume no distortion.
    mDistCoeffs.zeros(4, 1, CV_64F);
    mTracking = false;
}

float *IdMarkerDetectionFilter::getPose() {
    if (mTracking) {
        mLastValidPose[0] = mPose[0];
        mLastValidPose[1] = mPose[1];
        mLastValidPose[2] = mPose[2];
        mLastValidPose[3] = mPose[3];
        mLastValidPose[4] = mPose[4];
        mLastValidPose[5] = mPose[5];
        mLastValidPose[6] = mPose[6];
        return mPose;
    } else {
        if (lostTrackingCounter < 5) {
            return mLastValidPose;
        } else {
            return NULL;
        }
    }
}


std::vector<cv::Point3d> Generate3DPoints(cv::Mat markerCorners, float markerLength) {
    std::vector<cv::Point3d> points;
    points.push_back(cv::Point3d(-markerLength / 2.f, markerLength / 2.f, 0));
    points.push_back(cv::Point3d(markerLength / 2.f, markerLength / 2.f, 0));
    points.push_back(cv::Point3d(markerLength / 2.f, -markerLength / 2.f, 0));
    points.push_back(cv::Point3d(-markerLength / 2.f, -markerLength / 2.f, 0));
    points.push_back(cv::Point3d(0, markerLength / 2.f, 0));
    points.push_back(cv::Point3d(0, markerLength * 0.7, 0));
    points.push_back(cv::Point3d(-markerLength * 0.1, markerLength * 0.6, 0));
    points.push_back(cv::Point3d(markerLength * 0.1, markerLength * 0.6, 0));
    return points;
}

double pi() {
    return std::atan(1) * 4;
}


void drawDetectedMarkersMyM(cv::InputOutputArray _image, cv::InputArrayOfArrays _corners,
                            cv::Mat &cameraMatrix, float markerLength, cv::InputArray _rvec,
                            cv::InputArray _tvec,
                            int rotx, int roty, int rotz, int translx, int transly, int translz) {

    cv::Scalar targetColor, hudColor;
    targetColor = cv::Scalar(0, 175, 239);
    hudColor = cv::Scalar(168, 207, 69);

    // The Euler angles of the original target.
    cv::Mat mRVecOrig(3, 1, cv::DataType<double>::type); // Rotation vector
    mRVecOrig.at<double>(0) = rotx * pi() / 180;
    mRVecOrig.at<double>(1) = roty * pi() / 180;
    mRVecOrig.at<double>(2) = rotz * pi() / 180;

    // The XYZ coordinates of the original target.
    cv::Mat mTVecOrig(3, 1, cv::DataType<double>::type); // Translation vector
    mTVecOrig.at<double>(0) = translx;
    mTVecOrig.at<double>(1) = transly;
    mTVecOrig.at<double>(2) = translz;

    // Create zero distortion
    cv::Mat distCoeffs(5, 1, cv::DataType<double>::type);   // Distortion vector
    distCoeffs.at<double>(0) = 0.0;
    distCoeffs.at<double>(1) = 0.0;
    distCoeffs.at<double>(2) = 0.0;
    distCoeffs.at<double>(3) = 0.0;
    distCoeffs.at<double>(4) = 0.0;

    std::vector<cv::Point2d> targetHudPoints;

    int nMarkers = (int) _corners.total();
    for (int i = 0; i < nMarkers; i++) {
        cv::Mat currentMarker = _corners.getMat(i);
        CV_Assert(currentMarker.total() == 4 && currentMarker.type() == CV_32FC2);

        std::vector<cv::Point3d> objectPoints = Generate3DPoints(currentMarker, markerLength);

        cv::projectPoints(objectPoints, mRVecOrig, mTVecOrig, cameraMatrix, distCoeffs,
                          targetHudPoints);

        // draw Target
        line(_image, currentMarker.ptr<cv::Point2f>(0)[0], currentMarker.ptr<cv::Point2f>(0)[1],
             targetColor, 8);
        line(_image, currentMarker.ptr<cv::Point2f>(0)[1], currentMarker.ptr<cv::Point2f>(0)[2],
             targetColor, 8);
        line(_image, currentMarker.ptr<cv::Point2f>(0)[2], currentMarker.ptr<cv::Point2f>(0)[3],
             targetColor, 8);
        line(_image, currentMarker.ptr<cv::Point2f>(0)[3], currentMarker.ptr<cv::Point2f>(0)[0],
             targetColor, 8);

        // project Target Arrow points
        std::vector<cv::Point3f> targetArrowPoints;
        targetArrowPoints.push_back(cv::Point3f(0, markerLength / 2.f, 0));
        targetArrowPoints.push_back(cv::Point3f(0, markerLength * 0.7f, 0));
        targetArrowPoints.push_back(cv::Point3f(-markerLength * 0.1f, markerLength * 0.6f, 0));
        targetArrowPoints.push_back(cv::Point3f(markerLength * 0.1f, markerLength * 0.6f, 0));
        std::vector<cv::Point2f> imagePoints;
        cv::projectPoints(targetArrowPoints, _rvec, _tvec, cameraMatrix, distCoeffs, imagePoints);

        // draw Target Arrow lines
        line(_image, imagePoints[0], imagePoints[1], targetColor, 8);
        line(_image, imagePoints[1], imagePoints[2], targetColor, 8);
        line(_image, imagePoints[1], imagePoints[3], targetColor, 8);

        // draw Hud and Hud Arrow

        //LOGD("targetHudPoints[0]: x:%f  y:%f", targetHudPoints[0].x, targetHudPoints[0].y);
        line(_image, targetHudPoints[0], targetHudPoints[1], hudColor, 8);
        //LOGD("targetHudPoints[1]: x:%f  y:%f", targetHudPoints[1].x, targetHudPoints[1].y);
        line(_image, targetHudPoints[1], targetHudPoints[2], hudColor, 8);
        //LOGD("targetHudPoints[2]: x:%f  y:%f", targetHudPoints[2].x, targetHudPoints[2].y);
        line(_image, targetHudPoints[2], targetHudPoints[3], hudColor, 8);
        //LOGD("targetHudPoints[3]: x:%f  y:%f", targetHudPoints[3].x, targetHudPoints[3].y);
        line(_image, targetHudPoints[3], targetHudPoints[0], hudColor, 8);
        line(_image, targetHudPoints[4], targetHudPoints[5], hudColor, 8);
        line(_image, targetHudPoints[5], targetHudPoints[6], hudColor, 8);
        line(_image, targetHudPoints[5], targetHudPoints[7], hudColor, 8);
    }
}


void
IdMarkerDetectionFilter::apply(cv::Mat &src, int isHudOn, cv::Mat &cameraMatrix, int rotx, int roty,
                               int rotz, int translx, int transly, int translz) {

    // Convert the scene from RGBA to RGB (ArUco requirement).
    cv::cvtColor(src, src, cv::COLOR_RGBA2RGB);

    std::vector<int> ids;

    cv::aruco::detectMarkers(src, dictionary, corners, ids);

    if (ids.size() > 0) {
        mTracking = true;
        if (ids.size() == 1) {
            cv::aruco::estimatePoseSingleMarkers(corners, markerLength, cameraMatrix, mDistCoeffs,
                                                 mRVec, mTVec);
            mPose[0] = (float) mTVec[0](0);// X Translation
            mPose[1] = (float) mTVec[0](1);// Y Translation
            mPose[2] = (float) mTVec[0](2);// Z Translation
            mPose[3] = (float) mRVec[0](0);// X Rotation
            mPose[4] = (float) mRVec[0](1);// Y Rotation
            mPose[5] = (float) mRVec[0](2);// Z Rotation
            mPose[6] = (float) ((ids[0]) / 10); // marker currently being tracked
            lostTrackingCounter = 0;
            //LOGD("POSE: Id#%f x=%f y=%f z=%f rx=%f ry=%f rz=%f",mPose[6], mPose[0],mPose[1],mPose[2],mPose[3]*180/3.141592,mPose[4]*180/3.141592,mPose[5]*180/3.141592);
            if (isHudOn == 1) {
                drawDetectedMarkersMyM(src, corners, cameraMatrix, markerLength, mRVec[0],
                                       mTVec[0], rotx, roty, rotz, translx, transly, translz);
                cv::aruco::drawAxis(src, cameraMatrix, mDistCoeffs, mRVec[0], mTVec[0],
                                    markerLength * 0.5f);

            }
        }
    } else {
        mTracking = false;
        lostTrackingCounter++;
    }

}