# Adaptive Food Tracking System
An Android Application to track healthy and unhealthy food items based on the user physical activities and goals.

# Problem
There are numerous challenges in making healthy food choices while on a weight management plan. Existing tools are static and not adaptive to monitor weight effectively. These tools offer one-size-fits-all advice, failing to account for individual users' unique needs, preferences, and activity levels. 

# Solution
Integrated OpenAI API with a bespoke image recognition model to deliver personalized dietary insights. Processes users’ requests by parsing prompts to the OpenAI API. Also, it calculates the predicted weight based on calories burnt and gained and the steps taken by the user. The application also calculates the BMI and takes the steps directly from the sensors present in the Android Device. 

# FTS Model 
<ol>
  <li> A machine learning model to detect food items created using Python, TensorFlow and a dataset taken from Kaggle. </li>
  <li> Training and Validation Traning to the Model. </li>
  <li> Convert images into numbers.</li>
  <li> Check the similarity. </li>
  <li> Output the name and the consecutive information to the application. </li>
 </ol>

 I have deployed the Model using Tensorflow Lite.
 
 # Tech-Stack
 Java, Python, Tensorflow, Android Studio, Google Collab.

