# Introduction
This project is used to test OpenCV in Java. 

## Dependencies
The project relies on Java bindings to OpenCV published by OpenPnP
- https://github.com/openpnp/opencv/
  
In addition, this project uses models from opencv_zoo
- https://github.com/opencv/opencv_zoo

And of course, JavaXT ;-)
- https://www.javaxt.com

## Maven Quickstart
Clone and build this repo
```
git clone https://github.com/pborissow/OpenCV.git
cd OpenCV
mvn install
```

Download models
```
git clone https://github.com/opencv/opencv_zoo && cd opencv_zoo
git lfs install
git lfs pull
```

Run face detection
```
java -jar opencv.jar -model /path/to/face_detection_yunet_2023mar.onnx -input /path/to/image.jpg
```
