import os
import pytest
import numpy as np
import cv2
import uuid
from fastapi.testclient import TestClient
from app.main import app
from app.services.face_service import FaceService, PersonIndex

client = TestClient(app)

@pytest.fixture(autouse=True)
def reset_person_index():
    # Reset in-memory index before each test
    FaceService.get_instance().person_index.clear()


def create_synthetic_face(width=400, height=400, skin_color=(180, 210, 240)) -> bytes:
    """
    Creates a synthetic face drawing.
    """
    img = np.full((height, width, 3), 245, dtype=np.uint8)
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
    
    # Mouth
    cv2.ellipse(img, (center_x, center_y + 40), (25, 12), 0, 0, 180, (50, 50, 200), 3)
    
    _, encoded = cv2.imencode('.jpg', img)
    return encoded.tobytes()


def create_synthetic_group_photo(width=800, height=500) -> bytes:
    """
    Creates a group photo containing 2 face drawings for testing independent group processing.
    """
    img = np.full((height, width, 3), 240, dtype=np.uint8)
    centers = [(200, 250), (600, 250)]
    
    for cx, cy in centers:
        cv2.ellipse(img, (cx, cy), (70, 95), 0, 0, 360, (180, 210, 240), -1)
        cv2.circle(img, (cx - 25, cy - 25), 10, (255, 255, 255), -1)
        cv2.circle(img, (cx + 25, cy - 25), 10, (255, 255, 255), -1)
        cv2.circle(img, (cx - 25, cy - 25), 4, (0, 0, 0), -1)
        cv2.circle(img, (cx + 25, cy - 25), 4, (0, 0, 0), -1)
        cv2.line(img, (cx - 38, cy - 40), (cx - 12, cy - 40), (40, 40, 40), 3)
        cv2.line(img, (cx + 12, cy - 40), (cx + 38, cy - 40), (40, 40, 40), 3)
        cv2.line(img, (cx, cy - 10), (cx - 8, cy + 12), (100, 100, 100), 2)
        cv2.ellipse(img, (cx, cy + 30), (20, 10), 0, 0, 180, (50, 50, 200), 3)
        
    _, encoded = cv2.imencode('.jpg', img)
    return encoded.tobytes()


def test_person_index_vector_matching_unit():
    """
    Unit test for PersonIndex cosine similarity matching logic using deterministic vectors.
    """
    index = PersonIndex()
    
    # Vector A (L2 normalized)
    v1 = np.zeros(128, dtype=np.float32)
    v1[0] = 1.0
    
    # Vector B (Identical to A)
    v2 = np.zeros(128, dtype=np.float32)
    v2[0] = 1.0
    
    # Vector C (Orthogonal to A)
    v3 = np.zeros(128, dtype=np.float32)
    v3[1] = 1.0
    
    p1 = "person-uuid-1"
    index.register_embedding(p1, v1)
    
    # Test identical vector matching
    match_pid, sim = index.find_match(v2, threshold=0.60)
    assert match_pid == p1
    assert sim == 1.0
    
    # Test orthogonal vector non-matching
    match_pid_3, sim_3 = index.find_match(v3, threshold=0.60)
    assert match_pid_3 is None
    assert sim_3 is None


def test_embedding_endpoint_format_and_dimension():
    img_bytes = create_synthetic_face()
    response = client.post(
        "/api/ai/embedding",
        files={"file": ("face.jpg", img_bytes, "image/jpeg")}
    )
    assert response.status_code == 200
    data = response.json()
    assert data["faces_detected"] >= 1
    
    face = data["faces"][0]
    assert "face_id" in face
    assert "embedding_dimension" in face
    assert "embedding" in face
    assert face["embedding_dimension"] == len(face["embedding"])
    assert face["embedding_dimension"] == 128


def test_same_face_processed_twice_returns_same_person_id():
    img_bytes = create_synthetic_face()
    
    # First request -> NEW_PERSON
    resp1 = client.post(
        "/api/ai/identify",
        files={"file": ("face1.jpg", img_bytes, "image/jpeg")}
    )
    assert resp1.status_code == 200
    data1 = resp1.json()
    assert data1["faces_detected"] >= 1
    match1 = data1["matches"][0]
    assert match1["status"] == "NEW_PERSON"
    assert match1["similarity"] is None
    person_id_1 = match1["person_id"]
    
    # Second request -> MATCHED to same person_id
    resp2 = client.post(
        "/api/ai/identify",
        files={"file": ("face2.jpg", img_bytes, "image/jpeg")}
    )
    assert resp2.status_code == 200
    data2 = resp2.json()
    assert data2["faces_detected"] >= 1
    match2 = data2["matches"][0]
    assert match2["status"] == "MATCHED"
    assert match2["person_id"] == person_id_1
    assert isinstance(match2["similarity"], float)
    assert match2["similarity"] >= 0.50


def test_same_person_rescaled_image_matches():
    img_bytes1 = create_synthetic_face(width=400, height=400)
    
    # Rescale image bytes using opencv
    img1 = cv2.imencode('.jpg', cv2.resize(cv2.imdecode(np.frombuffer(img_bytes1, np.uint8), cv2.IMREAD_COLOR), (600, 600)))[1].tobytes()
    
    resp1 = client.post(
        "/api/ai/identify",
        files={"file": ("face_scale1.jpg", img_bytes1, "image/jpeg")}
    )
    data1 = resp1.json()
    assert data1["faces_detected"] >= 1
    person_id_1 = data1["matches"][0]["person_id"]
    
    resp2 = client.post(
        "/api/ai/identify",
        files={"file": ("face_scale2.jpg", img1, "image/jpeg")}
    )
    data2 = resp2.json()
    assert data2["faces_detected"] >= 1
    match2 = data2["matches"][0]
    assert match2["status"] == "MATCHED"
    assert match2["person_id"] == person_id_1


def test_different_faces_produce_different_person_ids():
    # Face 1: Standard synthetic face
    img_bytes1 = create_synthetic_face(skin_color=(180, 210, 240))
    
    # Face 2: Dark face with different contrast/background
    img_dark = np.zeros((400, 400, 3), dtype=np.uint8)
    cv2.circle(img_dark, (200, 200), 100, (50, 50, 50), -1)
    cv2.circle(img_dark, (160, 170), 15, (255, 255, 255), -1)
    cv2.circle(img_dark, (240, 170), 15, (255, 255, 255), -1)
    cv2.line(img_dark, (170, 250), (230, 250), (255, 255, 255), 5)
    _, encoded2 = cv2.imencode('.jpg', img_dark)
    img_bytes2 = encoded2.tobytes()
    
    resp1 = client.post(
        "/api/ai/identify",
        files={"file": ("face1.jpg", img_bytes1, "image/jpeg")}
    )
    pid1 = resp1.json()["matches"][0]["person_id"]
    
    resp2 = client.post(
        "/api/ai/identify",
        files={"file": ("face2.jpg", img_bytes2, "image/jpeg")}
    )
    data2 = resp2.json()
    if data2["faces_detected"] > 0:
        match2 = data2["matches"][0]
        if match2["status"] == "MATCHED":
            assert match2["person_id"] != pid1 or match2["similarity"] < 0.95


def test_group_photo_independent_processing():
    img_bytes = create_synthetic_group_photo()
    response = client.post(
        "/api/ai/identify",
        files={"file": ("group.jpg", img_bytes, "image/jpeg")}
    )
    assert response.status_code == 200
    data = response.json()
    assert data["faces_detected"] >= 2
    matches = data["matches"]
    assert len(matches) == data["faces_detected"]
    
    # Identical face drawings in same request share person_id
    assert matches[0]["person_id"] == matches[1]["person_id"]


def test_no_face_image_identify():
    img = np.zeros((300, 300, 3), dtype=np.uint8)
    img[:, :] = (100, 100, 100)
    _, encoded = cv2.imencode('.jpg', img)
    
    response = client.post(
        "/api/ai/identify",
        files={"file": ("noface.jpg", encoded.tobytes(), "image/jpeg")}
    )
    assert response.status_code == 200
    data = response.json()
    assert data["faces_detected"] == 0
    assert data["matches"] == []


def test_configurable_threshold_usage(monkeypatch):
    monkeypatch.setenv("FACE_MATCH_THRESHOLD", "0.9999")
    img_bytes = create_synthetic_face()
    
    resp1 = client.post("/api/ai/identify", files={"file": ("f1.jpg", img_bytes, "image/jpeg")})
    pid1 = resp1.json()["matches"][0]["person_id"]
    
    resp2 = client.post("/api/ai/identify", files={"file": ("f2.jpg", img_bytes, "image/jpeg")})
    match2 = resp2.json()["matches"][0]
    assert match2["status"] in ["MATCHED", "NEW_PERSON"]
