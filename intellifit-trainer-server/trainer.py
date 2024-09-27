import cv2
import numpy as np
import time
from exercises import *

# Capture the video or webcam feed
cap = cv2.VideoCapture('PoseVideos/pushup_1.mp4')

# Pose detector object
detector = PoseDetector()

# Initialize conditions and variables
is_legs = True  # Example to handle legs, modify as needed
is_arms = False  # Set True if working with arms
is_ankles = False  # Set True if working with ankles
is_leftLimb = True  # Left limb tracking
is_rightLimb = True  # Right limb tracking

# Min and max degrees for angle interpolation
min_degree = 90
max_degree = 160

# Initialize counters and direction
counter_left = 0
counter_right = 0
dir_left = 0
dir_right = 0

while True:
    # Read the video frames
    success, img = cap.read()
    if not success:
        break

    # Find pose landmarks and positions
    img = detector.findPose(img, False)
    lmList = detector.findPosition(img, False)

    if len(lmList) != 0:
        # Detect angles for legs, arms, or ankles
        if is_legs:
            angle_left = detector.findAngle(img, 23, 25, 27, is_leftLimb)
            angle_right = detector.findAngle(img, 24, 26, 28, is_rightLimb)

        elif is_arms:
            angle_left = detector.findAngle(img, 11, 13, 15, is_leftLimb)
            angle_right = detector.findAngle(img, 12, 14, 16, is_rightLimb)

        elif is_ankles:
            angle_left = detector.findAngle(img, 17, 11, 27, is_leftLimb)
            angle_right = detector.findAngle(img, 18, 12, 28, is_rightLimb)

        # Interpolation for percentage display
        per_right = np.interp(angle_right, (min_degree, max_degree), (100, 0))
        per_left = np.interp(angle_left, (min_degree, max_degree), (100, 0))

        # Drawing bars for visual feedback (left and right limbs)
        h, w, c = img.shape
        bar_color_left = (0, 255, 0)
        bar_color_right = (0, 255, 0)

        bar_rect_start_x_left = int(w * 0.90)
        bar_rect_start_y_left = int(h * 0.20)
        bar_rect_start_x_right = int(w * 0.05)
        bar_rect_start_y_right = int(h * 0.20)
        
        bar_rect_end_x_left = int(w * 0.95)
        bar_rect_end_y_left = int(h * 0.90)
        bar_rect_end_x_right = int(w * 0.10)
        bar_rect_end_y_right = int(h * 0.90)
        
        bar_text_x_left = int(w * 0.84)
        bar_text_y_left = int(h * 0.15)
        bar_text_x_right = int(w * 0.03)
        bar_text_y_right = int(h * 0.15)

        bar_left = bar_rect_end_y_left - (bar_rect_end_y_left - bar_rect_start_y_left) * per_left / 100
        bar_right = bar_rect_end_y_right - (bar_rect_end_y_right - bar_rect_start_y_right) * per_right / 100

        # Visual counter square size and placement
        sqr_size = int(min(w, h) * 0.20)
        sqr_padding = int(min(w, h) * 0.05)
        sqr_rect_start_x = (w - sqr_size) // 2 if (is_leftLimb and is_rightLimb) else sqr_padding if is_leftLimb else w - sqr_size - sqr_padding
        sqr_rect_start_y = h - sqr_size - sqr_padding
        sqr_rect_start = (sqr_rect_start_x, sqr_rect_start_y)
        sqr_rect_end = (sqr_rect_start_x + sqr_size, sqr_rect_start_y + sqr_size)

        # Update counters and bar colors based on movement direction
        if is_rightLimb:
            if per_right == 100:
                bar_color_right = (0, 255, 0)
                if dir_right == 0:
                    counter_right += 0.5
                    dir_right = 1
            elif per_right == 0:
                bar_color_right = (0, 0, 255)
                if dir_right == 1:
                    counter_right += 0.5
                    dir_right = 0
            else:
                bar_color_right = (0, 255, 255)

        if is_leftLimb:
            if per_left == 100:
                bar_color_left = (0, 255, 0)
                if dir_left == 0:
                    counter_left += 0.5
                    dir_left = 1
            elif per_left == 0:
                bar_color_left = (0, 0, 255)
                if dir_left == 1:
                    counter_left += 0.5
                    dir_left = 0
            else:
                bar_color_left = (0, 255, 255)

        # Draw left and right limb bars on the image
        if is_leftLimb:
            cv2.rectangle(img, (bar_rect_start_x_left, bar_rect_start_y_left),
                          (bar_rect_end_x_left, bar_rect_end_y_left), bar_color_left, 5)
            cv2.rectangle(img, (bar_rect_start_x_left, int(bar_left)),
                          (bar_rect_end_x_left, bar_rect_end_y_left), bar_color_left, cv2.FILLED)
            cv2.putText(img, f'{int(per_left)}%', (bar_text_x_left, bar_text_y_left),
                        cv2.FONT_HERSHEY_PLAIN, 2, (0, 0, 255), 2)

        if is_rightLimb:
            cv2.rectangle(img, (bar_rect_start_x_right, bar_rect_start_y_right),
                          (bar_rect_end_x_right, bar_rect_end_y_right), bar_color_right, 5)
            cv2.rectangle(img, (bar_rect_start_x_right, int(bar_right)),
                          (bar_rect_end_x_right, bar_rect_end_y_right), bar_color_right, cv2.FILLED)
            cv2.putText(img, f'{int(per_right)}%', (bar_text_x_right, bar_text_y_right),
                        cv2.FONT_HERSHEY_PLAIN, 2, (0, 0, 255), 2)

        # Display repetition count in the square box
        cv2.rectangle(img, sqr_rect_start, sqr_rect_end, (0, 255, 0), cv2.FILLED)
        cv2.rectangle(img, sqr_rect_start, sqr_rect_end, (0, 0, 255), 3)
        counter_text = str(int(counter_left + counter_right)) if is_leftLimb and is_rightLimb else str(int(counter_left)) if is_leftLimb else str(int(counter_right))
        text_x = sqr_rect_start_x + (sqr_size - cv2.getTextSize(counter_text, cv2.FONT_HERSHEY_PLAIN, 3, 2)[0][0]) // 2
        text_y = sqr_rect_start_y + (sqr_size + cv2.getTextSize(counter_text, cv2.FONT_HERSHEY_PLAIN, 3, 2)[0][1]) // 2
        cv2.putText(img, counter_text, (text_x, text_y), cv2.FONT_HERSHEY_PLAIN, 3, (0, 0, 255), 2)

    # Display the frame
    cv2.imshow("Image", img)

    # Break on 'q' key press
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()
