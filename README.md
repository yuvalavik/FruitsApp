🍎 Fruit Classification App

Overview

This app allows users to capture images of fruits, send them to a server for processing, and receive real-time feedback on the fruit type and its quality (e.g., fresh or rotten). Designed to assist farmers and consumers, the app aims to provide a simple and efficient solution for identifying fruit conditions with the help of machine learning.

Features

📷 Capture Images: Take pictures of fruits directly through the app.
✏️ Draw a Rectangle: Highlight the fruit in the image for accurate classification.
🤖 ML-Powered Classification: Uses a custom-trained model to classify the fruit type and determine its wellness.
🔊 Voice Feedback: Hear the classification results aloud.
📨 Feedback System: Users can report incorrect classifications and provide the correct labels. If the model is updated based on feedback, users are notified via email.
🔄 Seamless Workflow: Navigate easily between capturing, classifying, and providing feedback.
Tech Stack

Client
Android Studio: For the mobile app interface and camera functionality.
Server
Python: Handles image processing and runs the machine learning model.
Flask: Provides RESTful API endpoints for communication with the app.
Machine Learning
VGG16-Based Model: A pre-trained convolutional neural network fine-tuned for fruit classification.
Dataset: Includes 14 fruit classes with images labeled as "good" or "bad."
Database
MongoDB: Stores user feedback and email addresses for updates.
