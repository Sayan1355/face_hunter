import os
import uuid
import numpy as np
import cv2
from typing import Tuple, Dict, Any

STORAGE_FACES_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../../storage/faces"))

def ensure_storage_dir():
    os.makedirs(STORAGE_FACES_DIR, exist_ok=True)

def safe_decode_image(image_bytes: bytes) -> np.ndarray | None:
    """
    Safely decode raw image bytes into an OpenCV BGR numpy array.
    Returns None if decoding fails or input is not a valid image.
    """
    if not image_bytes or len(image_bytes) < 12:
        return None
    try:
        nparr = np.frombuffer(image_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if img is None or img.size == 0 or len(img.shape) < 2:
            return None
        return img
    except Exception:
        return None

def crop_and_save_face(
    img: np.ndarray,
    bbox: Tuple[int, int, int, int],
    margin_ratio: float = 0.15
) -> Tuple[str, Dict[str, int], str]:
    """
    Crop face region from img given bbox (x1, y1, x2, y2) with a small margin,
    clamp to image bounds, save to storage/faces/<uuid>.jpg, and return
    (face_id, bbox_dict, relative_path).
    bbox_dict format: {"x": x, "y": y, "width": w, "height": h}
    """
    ensure_storage_dir()
    
    img_h, img_w = img.shape[0], img.shape[1]
    x1, y1, x2, y2 = [int(v) for v in bbox]
    
    w = max(1, x2 - x1)
    h = max(1, y2 - y1)
    
    # Calculate margin around face
    margin_w = int(w * margin_ratio)
    margin_h = int(h * margin_ratio)
    
    crop_x1 = max(0, x1 - margin_w)
    crop_y1 = max(0, y1 - margin_h)
    crop_x2 = min(img_w, x2 + margin_w)
    crop_y2 = min(img_h, y2 + margin_h)
    
    cropped_face = img[crop_y1:crop_y2, crop_x1:crop_x2]
    
    face_id = str(uuid.uuid4())
    filename = f"{face_id}.jpg"
    full_path = os.path.join(STORAGE_FACES_DIR, filename)
    
    # Relative path for API response
    relative_path = os.path.join("storage/faces", filename)
    
    cv2.imwrite(full_path, cropped_face)
    
    bbox_dict = {
        "x": max(0, x1),
        "y": max(0, y1),
        "width": w,
        "height": h
    }
    
    return face_id, bbox_dict, relative_path
