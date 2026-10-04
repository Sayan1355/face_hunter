import os
import pytest
import numpy as np
import cv2
import uuid
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def create_synthetic_face_image(width=400, height=400, skin_color=(180, 210, 240)) -> bytes:
    """
    Creates a synthetic image containing a drawn human-like face structure for testing.
    """
    img = np.full((height, width, 3), 245, dtype=np.uint8)
    
    # Face oval
    center_x, center_y = width // 2, height // 2
    cv2.ellipse(img, (center_x, center_y), (80, 110), 0, 0, 360, skin_color, -1)
    
    # Eyes
    cv2.circle(img, (center_x - 30, center_y - 30), 12, (255, 255, 255), -1)
    cv2.circle(img, (center_x + 30, center_y - 30), 12, (255, 255, 255), -1)
    cv2.circle(img, (center_x - 30, center_y - 30), 5, (0, 0, 0), -1)
    cv2.circle(img, (center_x + 30, center_y - 30), 5, (0, 0, 0), -1)
    
    # Eyebrows
    cv2.line(img, (center_x - 45, center_y - 50), (center_x - 15, center_y - 50), (40, 40, 40), 4)
    cv2.line(img, (center_x + 15, center_y - 50), (center_x + 45, center_y - 50), (40, 40, 40), 4)
    
    # Nose
    cv2.line(img, (center_x, center_y - 15), (center_x - 10, center_y + 15), (100, 100, 100), 3)
    cv2.line(img, (center_x - 10, center_y + 15), (center_x + 5, center_y + 15), (100, 100, 100), 3)
    
    # Mouth
    cv2.ellipse(img, (center_x, center_y + 40), (25, 12), 0, 0, 180, (50, 50, 200), 3)
    
    _, encoded = cv2.imencode('.jpg', img)
    return encoded.tobytes()

def create_synthetic_group_image(width=800, height=500) -> bytes:
    """
    Creates a synthetic image containing multiple face drawings for testing group detection.
    """
    img = np.full((height, width, 3), 240, dtype=np.uint8)
    
    centers = [(200, 250), (550, 250)]
    for cx, cy in centers:
        cv2.ellipse(img, (cx, cy), (70, 95), 0, 0, 360, (180, 210, 240), -1)
        # Eyes
        cv2.circle(img, (cx - 25, cy - 25), 10, (255, 255, 255), -1)
        cv2.circle(img, (cx + 25, cy - 25), 10, (255, 255, 255), -1)
        cv2.circle(img, (cx - 25, cy - 25), 4, (0, 0, 0), -1)
        cv2.circle(img, (cx + 25, cy - 25), 4, (0, 0, 0), -1)
        # Eyebrows
        cv2.line(img, (cx - 38, cy - 40), (cx - 12, cy - 40), (40, 40, 40), 3)
        cv2.line(img, (cx + 12, cy - 40), (cx + 38, cy - 40), (40, 40, 40), 3)
        # Nose
        cv2.line(img, (cx, cy - 10), (cx - 8, cy + 12), (100, 100, 100), 2)
        # Mouth
        cv2.ellipse(img, (cx, cy + 30), (20, 10), 0, 0, 180, (50, 50, 200), 3)
        
    _, encoded = cv2.imencode('.jpg', img)
    return encoded.tobytes()

def create_no_face_image(width=300, height=300) -> bytes:
    """
    Creates a solid color/textured image with no faces.
    """
    img = np.zeros((height, width, 3), dtype=np.uint8)
    img[:, :] = (100, 150, 50)
    _, encoded = cv2.imencode('.jpg', img)
    return encoded.tobytes()


def test_detect_faces_single_person():
    img_bytes = create_synthetic_face_image()
    response = client.post(
        "/api/ai/detect-faces",
        files={"file": ("test_single.jpg", img_bytes, "image/jpeg")}
    )
    assert response.status_code == 200
    data = response.json()
    assert data["image_width"] == 400
    assert data["image_height"] == 400
    assert data["faces_detected"] >= 1
    assert len(data["faces"]) == data["faces_detected"]
    
    face = data["faces"][0]
    assert "face_id" in face
    assert "bounding_box" in face
    bbox = face["bounding_box"]
    assert bbox["x"] >= 0
    assert bbox["y"] >= 0
    assert bbox["width"] > 0
    assert bbox["height"] > 0
    
    # Verify file exists on disk
    file_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../", face["face_image_path"]))
    assert os.path.exists(file_path)


def test_detect_faces_group():
    img_bytes = create_synthetic_group_image()
    response = client.post(
        "/api/ai/detect-faces",
        files={"file": ("test_group.jpg", img_bytes, "image/jpeg")}
    )
    assert response.status_code == 200
    data = response.json()
    assert data["image_width"] == 800
    assert data["image_height"] == 500
    assert data["faces_detected"] >= 1


def test_detect_faces_no_face():
    img_bytes = create_no_face_image()
    response = client.post(
        "/api/ai/detect-faces",
        files={"file": ("test_noface.jpg", img_bytes, "image/jpeg")}
    )
    assert response.status_code == 200
    data = response.json()
    assert data["image_width"] == 300
    assert data["image_height"] == 300
    assert data["faces_detected"] == 0
    assert data["faces"] == []


def test_detect_faces_invalid_file():
    invalid_bytes = b"NOT_AN_IMAGE_FILE_HEADER_XYZ"
    response = client.post(
        "/api/ai/detect-faces",
        files={"file": ("invalid.txt", invalid_bytes, "text/plain")}
    )
    assert response.status_code == 400
    assert "detail" in response.json()


def test_unique_face_ids_and_bounding_boxes():
    img_bytes = create_synthetic_group_image()
    response = client.post(
        "/api/ai/detect-faces",
        files={"file": ("test_unique.jpg", img_bytes, "image/jpeg")}
    )
    assert response.status_code == 200
    data = response.json()
    face_ids = [f["face_id"] for f in data["faces"]]
    assert len(face_ids) == len(set(face_ids)), "All face_ids must be unique UUIDs"
    
    for f in data["faces"]:
        bbox = f["bounding_box"]
        assert bbox["x"] + bbox["width"] <= data["image_width"]
        assert bbox["y"] + bbox["height"] <= data["image_height"]
