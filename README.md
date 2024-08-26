# Adaptive Food Tracking System
An Android Application to track healthy and unhealthy food items based on the user physical activities and goals.

# Problem
There are numerous challenges in making healthy food choices while on a weight management plan. Existing tools are static and not adaptive to monitor weight effectively. These tools offer one-size-fits-all advice, failing to account for the unique needs, preferences, and activity levels of individual users. 

# Solution
Integrated OpenAI API with a bespoke image recognition model to deliver personalized dietary insights. Processes users’ request by parsing prompts to the OpenAI API. Also, calculates the predicted weight based on calories burnt and gained and the steps take by the user. The application also calculates the BMI and takes the steps directly from the sensors present in the Android Device. 

# FTS Model 
<ol>
  <li> A machine learning model to detect food items created using python, tensorflow and dataset taken from Kaggle. </li>
  <li> Training and Validation Traning to the Model. </li>
  <li> Encode detected faces.</li>
  <li> Group the faces of one person in a cluster </li>
  <li> Detect the face in the given image and encode it. </li>
  <li> Predict in which cluster will the query image belong to.</li>
  <li> Retrieve all the frames in the predicted cluster </li>
 </ol>
 
 We have deployed the ML-Pipeline on a Web application using Flask.
 
 # Tech-Stack
 <ul>
  <li> Front-end : HTML, CSS, Bootstrap, JavaScript </li>
<li>Back-end: Flask </li>
<li>Machine Learning Libraries:  Numpy, Pandas, Sci-Kit Learn, Face-recognition, OpenCV </li>

## Team Members
<ul>
  <li>  [Shambhavi Aggarwal](https://github.com/agg-shambhavi) </li>
  <li>  [Bhargav Akhani](https://github.com/bhargav2427)</li>
  <li>  [Dharven Doshi](https://github.com/dharven) </li>
</ul>
